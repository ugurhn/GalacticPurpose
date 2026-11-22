package com.galacticpurpose.item;

import com.galacticpurpose.wormhole.WormholeAssemblerCalculator;
import com.galacticpurpose.wormhole.WormholeMultiblockValidator;
import com.galacticpurpose.wormhole.WormholeTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Automated Wormhole Structure Assembler Wrench:
 * - Shift+Right-click anywhere (or Right-click air) toggles target tier: Personal (7x5x7) / Colossal (45x41x45).
 * - Right-click a Wormhole Core repeatedly to pull required blocks from your inventory, assemble the next
 *   step in place, flag any obstructing blocks with exact coordinates, and summarize remaining materials in chat.
 */
public class StructureAssemblerItem extends Item {
	private static final String TAG_TIER = "TargetTier";
	private static BiConsumer<BlockPos, WormholeTier> clientHologramStarter = (pos, tier) -> {};

	public StructureAssemblerItem(Properties properties) {
		super(properties);
	}

	public static void setClientHologramStarter(BiConsumer<BlockPos, WormholeTier> starter) {
		clientHologramStarter = starter;
	}

	public static void showClientPreview(BlockPos corePos, ItemStack stack) {
		clientHologramStarter.accept(corePos, getTargetTier(stack));
	}

	public static WormholeTier getTargetTier(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return WormholeTier.PERSONAL;
		}
		CompoundTag tag = data.copyTag();
		return tag.contains(TAG_TIER) ? WormholeTier.byOrdinal(tag.getInt(TAG_TIER)) : WormholeTier.PERSONAL;
	}

	public static WormholeTier toggleTargetTier(ItemStack stack, Player player) {
		WormholeTier next = getTargetTier(stack) == WormholeTier.PERSONAL
			? WormholeTier.COLOSSAL
			: WormholeTier.PERSONAL;
		CompoundTag tag = new CompoundTag();
		tag.putInt(TAG_TIER, next.ordinal());
		CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
		if (player != null) {
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.assembler_mode",
					Component.translatable(next.translationKey()),
					WormholeMultiblockValidator.getRequiredBlueprint(next).size()
				).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				true
			);
			player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.25f);
		}
		return next;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide) {
			toggleTargetTier(stack, player);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null && player.isShiftKeyDown()) {
			if (!context.getLevel().isClientSide) {
				toggleTargetTier(context.getItemInHand(), player);
			}
			return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
		}
		return InteractionResult.PASS;
	}

	public static boolean assembleOnCore(Level level, BlockPos corePos, Player player, ItemStack stack) {
		if (player != null && player.isShiftKeyDown()) {
			toggleTargetTier(stack, player);
			return false;
		}
		WormholeTier tier = getTargetTier(stack);
		WormholeAssemblerCalculator.ScanResult preScan = WormholeAssemblerCalculator.scan(level, corePos, tier);

		// Auto-orient any already-placed stabilizer pylons to face inward without consuming items
		for (WormholeMultiblockValidator.StructureBlockSpec spec : WormholeMultiblockValidator.getRequiredBlueprint(tier)) {
			BlockPos p = corePos.offset(spec.offset());
			BlockState cur = level.getBlockState(p);
			if (cur.is(spec.targetState().getBlock()) && !cur.equals(spec.targetState())) {
				level.setBlock(p, spec.targetState(), Block.UPDATE_ALL);
			}
		}

		if (preScan.isComplete()) {
			notifyComplete(level, corePos, player, tier);
			return true;
		}

		int maxBatch = WormholeAssemblerCalculator.batchSizeFor(tier);
		int placed = 0;
		Map<Block, Integer> missingInInv = new LinkedHashMap<>();

		for (WormholeMultiblockValidator.StructureBlockSpec spec : preScan.buildableSlots()) {
			if (placed >= maxBatch) {
				break;
			}
			Block block = spec.targetState().getBlock();
			boolean creative = player != null && player.isCreative();
			if (creative || consumeBlockItem(player, block)) {
				BlockPos targetPos = corePos.offset(spec.offset());
				level.setBlock(targetPos, spec.targetState(), Block.UPDATE_ALL);
				placed++;
				if (level instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(
						ParticleTypes.ELECTRIC_SPARK,
						targetPos.getX() + 0.5,
						targetPos.getY() + 0.5,
						targetPos.getZ() + 0.5,
						8,
						0.35,
						0.35,
						0.35,
						0.05
					);
				}
				level.playSound(
					null,
					targetPos,
					spec.targetState().getSoundType().getPlaceSound(),
					SoundSource.BLOCKS,
					0.85f,
					1.12f
				);
			} else {
				missingInInv.merge(block, 1, Integer::sum);
			}
		}

		WormholeAssemblerCalculator.ScanResult afterScan = WormholeAssemblerCalculator.scan(level, corePos, tier);
		if (afterScan.isComplete()) {
			notifyComplete(level, corePos, player, tier);
			return true;
		}

		if (player != null) {
			sendStatusReport(player, afterScan, placed, !missingInInv.isEmpty() && placed == 0);
		}
		return placed > 0;
	}

	private static void notifyComplete(Level level, BlockPos corePos, Player player, WormholeTier tier) {
		level.playSound(null, corePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.3f, 1.25f);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(
				ParticleTypes.END_ROD,
				corePos.getX() + 0.5,
				corePos.getY() + 0.5,
				corePos.getZ() + 0.5,
				24,
				1.2,
				1.2,
				1.2,
				0.08
			);
		}
		if (player != null) {
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.assembler_complete",
					Component.translatable(tier.translationKey())
				).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
				false
			);
		}
	}

	private static void sendStatusReport(
		Player player,
		WormholeAssemblerCalculator.ScanResult scan,
		int placedNow,
		boolean outOfInventoryItems
	) {
		player.displayClientMessage(
			Component.translatable(
				"message.galacticpurpose.assembler_progress",
				Component.translatable(scan.tier().translationKey()),
				scan.alreadySatisfied(),
				scan.totalRequired(),
				placedNow
			).withStyle(placedNow > 0 ? ChatFormatting.AQUA : ChatFormatting.YELLOW),
			false
		);

		if (!scan.obstructions().isEmpty()) {
			List<WormholeAssemblerCalculator.Obstruction> obs = scan.obstructions();
			WormholeAssemblerCalculator.Obstruction first = obs.get(0);
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.assembler_obstruction",
					obs.size(),
					first.existingBlock().getName(),
					first.worldPos().getX(),
					first.worldPos().getY(),
					first.worldPos().getZ()
				).withStyle(ChatFormatting.RED),
				false
			);
		}

		if (!scan.remainingMaterialCounts().isEmpty()) {
			MutableComponent summary = Component.translatable("message.galacticpurpose.assembler_needed_prefix")
				.withStyle(outOfInventoryItems ? ChatFormatting.GOLD : ChatFormatting.GRAY);
			boolean firstEntry = true;
			for (Map.Entry<Block, Integer> entry : scan.remainingMaterialCounts().entrySet()) {
				Block block = entry.getKey();
				int need = entry.getValue();
				int have = player.isCreative() ? need : countBlockItem(player, block);
				if (!firstEntry) {
					summary.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
				}
				firstEntry = false;
				ChatFormatting col = have >= need
					? ChatFormatting.GREEN
					: (have > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED);
				summary.append(Component.literal(need + "x ").withStyle(col))
					.append(block.getName().copy().withStyle(col))
					.append(Component.literal(player.isCreative() ? "" : " (" + have + ")").withStyle(ChatFormatting.DARK_GRAY));
			}
			player.displayClientMessage(summary, false);
		}
	}

	public static int countBlockItem(Player player, Block block) {
		if (player == null) {
			return 0;
		}
		Item targetItem = block.asItem();
		int total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack slot = player.getInventory().getItem(i);
			if (slot.is(targetItem)) {
				total += slot.getCount();
			}
		}
		return total;
	}

	public static boolean consumeBlockItem(Player player, Block block) {
		if (player == null) {
			return false;
		}
		Item targetItem = block.asItem();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack slot = player.getInventory().getItem(i);
			if (slot.is(targetItem) && slot.getCount() > 0) {
				slot.shrink(1);
				return true;
			}
		}
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		WormholeTier tier = getTargetTier(stack);
		tooltip.add(Component.translatable(
			"tooltip.galacticpurpose.structure_assembler_mode",
			Component.translatable(tier.translationKey())
		).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.galacticpurpose.structure_assembler_hint_1")
			.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.galacticpurpose.structure_assembler_hint_2")
			.withStyle(ChatFormatting.DARK_GRAY));
	}
}

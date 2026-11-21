package com.galacticpurpose.item;

import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.wormhole.WormholeTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Interactive visual Engineering Codex explaining all 5 stages, recipes, armors, energy/mod compatibility,
 * and projecting live 3D multiblock holograms directly into the world!
 */
public class MissionCodexItem extends Item {
	private static Runnable clientScreenOpener = () -> {};
	private static BiConsumer<BlockPos, WormholeTier> clientHologramToggler = (pos, tier) -> {};

	public MissionCodexItem(Properties properties) {
		super(properties);
	}

	public static void setClientHooks(Runnable screenOpener, BiConsumer<BlockPos, WormholeTier> hologramToggler) {
		clientScreenOpener = screenOpener;
		clientHologramToggler = hologramToggler;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) {
			clientScreenOpener.run();
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();
		if (player != null && player.isShiftKeyDown()
			&& level.getBlockEntity(pos) instanceof WormholeCoreBlockEntity core) {
			if (level.isClientSide) {
				clientHologramToggler.accept(pos, core.activeTier());
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.galacticpurpose.mission_codex_1").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.galacticpurpose.mission_codex_2").withStyle(ChatFormatting.GRAY));
	}
}

package com.galacticpurpose.item;

import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.registry.ModBlocks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Schematic Blueprint for the Horizon Lander. Blank blueprints fabricate a new ship on a
 * Ship Assembly Pad; Packed blueprints carry a Survival-recovered lander's Fuel & Flux charge
 * and can be redeployed cleanly onto any pad or solid flat terrain.
 */
public class StarshipBlueprintItem extends Item {
	public static final String TAG_PACKED = "PackedShip";
	public static final String TAG_FUEL = "PackedFuel";
	public static final String TAG_ENERGY = "PackedEnergy";

	public StarshipBlueprintItem(Properties properties) {
		super(properties);
	}

	public static boolean isPacked(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBoolean(TAG_PACKED);
	}

	public static int readPackedFuel(ItemStack stack, int fallback) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return fallback;
		}
		CompoundTag tag = data.copyTag();
		return tag.getBoolean(TAG_PACKED) ? tag.getInt(TAG_FUEL) : fallback;
	}

	public static long readPackedEnergy(ItemStack stack, long fallback) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return fallback;
		}
		CompoundTag tag = data.copyTag();
		return tag.getBoolean(TAG_PACKED) ? tag.getLong(TAG_ENERGY) : fallback;
	}

	public static void writePackedState(ItemStack stack, int fuel, long energy) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(TAG_PACKED, true);
		tag.putInt(TAG_FUEL, Math.max(0, Math.min(InterstellarShipEntity.MAX_FUEL, fuel)));
		tag.putLong(TAG_ENERGY, Math.max(0L, Math.min(InterstellarShipEntity.MAX_ENERGY, energy)));
		CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		ItemStack stack = ctx.getItemInHand();
		if (!isPacked(stack)) {
			return InteractionResult.PASS;
		}
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (state.is(ModBlocks.SHIP_ASSEMBLY_PAD) || ctx.getClickedFace() != Direction.UP
				|| !state.isFaceSturdy(level, pos, Direction.UP)) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel) {
			AABB check = new AABB(pos.above()).inflate(4.0, 4.0, 4.0);
			if (!serverLevel.getEntitiesOfClass(InterstellarShipEntity.class, check).isEmpty()) {
				Player p = ctx.getPlayer();
				if (p != null) {
					p.displayClientMessage(
						Component.translatable("message.galacticpurpose.ship_pad_occupied").withStyle(ChatFormatting.YELLOW),
						false
					);
				}
				return InteractionResult.CONSUME;
			}
			int fuel = readPackedFuel(stack, 0);
			long energy = readPackedEnergy(stack, 0L);
			Player player = ctx.getPlayer();
			float fallbackYaw = player != null ? player.getYRot() : 0.0f;
			BlockPos core = InterstellarShipEntity.findNearestColossalWormhole(serverLevel, pos, 6, false);
			float yaw = core != null
				? InterstellarShipEntity.computeYawFacingTarget(pos.getX() + 0.5, pos.getZ() + 0.5, core.getX() + 0.5, core.getZ() + 0.5)
				: fallbackYaw;
			InterstellarShipEntity.spawnParked(
				serverLevel, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, yaw, fuel, energy, false
			);
			if (player == null || !player.isCreative()) {
				stack.shrink(1);
			}
			serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5, 30, 2.0, 1.0, 2.0, 0.12);
			serverLevel.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.4f, 0.78f);
			if (player != null) {
				player.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_unpacked", fuel)
						.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
					false
				);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (isPacked(stack)) {
			int fuel = readPackedFuel(stack, 0);
			long energy = readPackedEnergy(stack, 0L);
			tooltip.add(
				Component.translatable("tooltip.galacticpurpose.blueprint_packed", fuel, energy / 1000L)
					.withStyle(ChatFormatting.AQUA)
			);
			tooltip.add(
				Component.translatable("tooltip.galacticpurpose.blueprint_deploy_hint")
					.withStyle(ChatFormatting.GRAY)
			);
		} else {
			tooltip.add(
				Component.translatable("tooltip.galacticpurpose.blueprint_empty")
					.withStyle(ChatFormatting.GRAY)
			);
		}
	}
}

package com.galacticpurpose.block;

import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.item.StarshipBlueprintItem;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Magnetic Runway Assembly Pad: right-click with an Interstellar Starship Blueprint to fabricate
 * a fueled horizontal Horizon Lander automatically aligned straight down an adjacent runway track
 * or directly toward the nearest 45x41x45 Colossal Wormhole Core!
 */
public class ShipAssemblyPadBlock extends Block {
	public ShipAssemblyPadBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected ItemInteractionResult useItemOn(
		ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hit
	) {
		if (!stack.is(ModItems.STARSHIP_BLUEPRINT)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (level instanceof ServerLevel serverLevel) {
			AABB check = new AABB(pos.above()).inflate(4.0, 4.0, 4.0);
			if (!serverLevel.getEntitiesOfClass(InterstellarShipEntity.class, check).isEmpty()) {
				player.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_pad_occupied").withStyle(ChatFormatting.YELLOW),
					false
				);
				return ItemInteractionResult.CONSUME;
			}
			float yaw = resolveLaunchYaw(serverLevel, pos, player.getDirection());
			int initialFuel = StarshipBlueprintItem.readPackedFuel(stack, 2500);
			long initialEnergy = StarshipBlueprintItem.readPackedEnergy(stack, InterstellarShipEntity.MAX_ENERGY / 4L);
			InterstellarShipEntity ship = InterstellarShipEntity.spawnParked(
				serverLevel, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, yaw, initialFuel, initialEnergy, false
			);
			if (!player.isCreative()) {
				stack.shrink(1);
			}
			serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5, 45, 2.4, 1.2, 2.4, 0.14);
			serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0f, 1.15f);
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.ship_assembled", ship.fuelUnits())
					.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				false
			);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/**
	 * Priority alignment:
	 * 1) Nearest 45x41x45 Colossal Wormhole Core within 96 blocks
	 * 2) Adjacent straight Magnetic Runway Track (Starship Grate / Hull Plating)
	 * 3) Player's horizontal facing direction
	 */
	public static float resolveLaunchYaw(ServerLevel level, BlockPos padPos, Direction fallbackFacing) {
		BlockPos core = InterstellarShipEntity.findNearestColossalWormhole(level, padPos, 6, false);
		if (core != null) {
			return InterstellarShipEntity.computeYawFacingTarget(
				padPos.getX() + 0.5, padPos.getZ() + 0.5, core.getX() + 0.5, core.getZ() + 0.5
			);
		}
		Direction bestRunway = null;
		int bestLen = 2;
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			int len = 0;
			for (int step = 1; step <= 14; step++) {
				BlockState s = level.getBlockState(padPos.relative(dir, step));
				if (s.is(ModBlocks.STARSHIP_GRATE) || s.is(ModBlocks.STARSHIP_HULL_PLATING) || s.is(ModBlocks.STARSHIP_LIGHT_STRIP)) {
					len++;
				} else {
					break;
				}
			}
			if (len > bestLen) {
				bestLen = len;
				bestRunway = dir;
			}
		}
		Direction chosen = bestRunway != null ? bestRunway : fallbackFacing;
		return chosen.toYRot();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide) {
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.ship_pad_hint").withStyle(ChatFormatting.GRAY),
				false
			);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}

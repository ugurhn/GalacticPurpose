package com.galacticpurpose.block;

import com.galacticpurpose.ship.OrbitalTransitManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Front bridge Autopilot & Propulsive Auto-Descent Helm inside the walkable orbital_transit Starship Deck.
 */
public class StarshipHelmBlock extends Block {
	public StarshipHelmBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
			if (OrbitalTransitManager.isInOrbitalTransit(serverPlayer)) {
				OrbitalTransitManager.engageAutoLandingOrWormhole(serverPlayer);
			} else {
				serverPlayer.displayClientMessage(
					Component.translatable("message.galacticpurpose.helm_only_in_space").withStyle(ChatFormatting.AQUA),
					false
				);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}

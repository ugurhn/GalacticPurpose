package com.galacticpurpose.block;

import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.ExoplanetTravelHandler;
import com.galacticpurpose.wormhole.WormholeState;
import com.galacticpurpose.wormhole.WormholeTier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Swirling wormhole gateway spawned beneath an open Personal or Colossal Wormhole Core.
 */
public class WormholePortalBlock extends Block {
	public static final MapCodec<WormholePortalBlock> CODEC = simpleCodec(WormholePortalBlock::new);

	public WormholePortalBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
			return;
		}
		if (player.isOnPortalCooldown()) {
			return;
		}
		WormholeCoreBlockEntity core = findParentCore(level, pos);
		if (core != null && core.wormholeState() == WormholeState.OPEN) {
			ExoplanetRoute route = core.selectedRoute();
			if (route != null) {
				player.setPortalCooldown(80);
				ExoplanetTravelHandler.travel(player, route);
			}
		}
	}

	private static WormholeCoreBlockEntity findParentCore(Level level, BlockPos portalPos) {
		if (level.getBlockEntity(portalPos.above()) instanceof WormholeCoreBlockEntity personal) {
			return personal;
		}
		int drop = WormholeTier.COLOSSAL.portalDrop();
		int r = WormholeTier.COLOSSAL.portalHalfExtent();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (level.getBlockEntity(portalPos.offset(dx, drop, dz)) instanceof WormholeCoreBlockEntity colossal) {
					return colossal;
				}
			}
		}
		return null;
	}
}

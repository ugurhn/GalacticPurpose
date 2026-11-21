package com.galacticpurpose.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ground-level power anchor & remote control console beneath the floating Wormhole Core (3 or 13 blocks below).
 * Right-clicking opens the connected Wormhole Core GUI so players can operate or Quench Colossal reactors from the deck!
 */
public class WormholeEnergyInjectorBlock extends BaseEntityBlock {
	public static final MapCodec<WormholeEnergyInjectorBlock> CODEC = simpleCodec(WormholeEnergyInjectorBlock::new);

	public WormholeEnergyInjectorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WormholeEnergyInjectorBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof WormholeEnergyInjectorBlockEntity injector) {
			WormholeCoreBlockEntity core = injector.findConnectedCore();
			if (core != null) {
				player.openMenu(core);
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}

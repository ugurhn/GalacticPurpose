package com.galacticpurpose.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pressurized 27-slot Starship Cargo Locker block.
 */
public class StarshipCargoLockerBlock extends BaseEntityBlock {
	public static final MapCodec<StarshipCargoLockerBlock> CODEC = simpleCodec(StarshipCargoLockerBlock::new);

	public StarshipCargoLockerBlock(Properties properties) {
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
		return new StarshipCargoLockerBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof StarshipCargoLockerBlockEntity locker) {
			player.openMenu(locker);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StarshipCargoLockerBlockEntity locker) {
			// Do not drop transient orbital_transit deck locker items on rebuild
			if (!level.dimension().location().getPath().equals("orbital_transit")) {
				Containers.dropContents(level, pos, locker);
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

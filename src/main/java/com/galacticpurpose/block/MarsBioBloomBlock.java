package com.galacticpurpose.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bioluminescent mutated Mars plant harvested for exotic bio-essence biofuel.
 */
public class MarsBioBloomBlock extends BushBlock {
	public static final MapCodec<MarsBioBloomBlock> CODEC = simpleCodec(MarsBioBloomBlock::new);
	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

	public MarsBioBloomBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
		return floor.is(BlockTags.DIRT)
			|| floor.is(Blocks.RED_SAND)
			|| floor.is(Blocks.RED_SANDSTONE)
			|| floor.is(Blocks.TERRACOTTA);
	}
}

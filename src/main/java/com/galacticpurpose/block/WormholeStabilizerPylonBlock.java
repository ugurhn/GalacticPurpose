package com.galacticpurpose.block;

import com.galacticpurpose.wormhole.WormholeTier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Containment field pylon placed cardinally around a Wormhole Stabilizer Core.
 * Right-clicking any linked stabilizer pylon opens the connected Wormhole Core control console.
 */
public class WormholeStabilizerPylonBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<WormholeStabilizerPylonBlock> CODEC = simpleCodec(WormholeStabilizerPylonBlock::new);

	public WormholeStabilizerPylonBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (!level.isClientSide) {
			WormholeCoreBlockEntity core = findLinkedCore(level, pos, state.getValue(FACING));
			if (core != null) {
				player.openMenu(core);
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static WormholeCoreBlockEntity findLinkedCore(Level level, BlockPos pos, Direction inward) {
		// Check Personal distance (3) directly ahead
		if (level.getBlockEntity(pos.relative(inward, WormholeTier.PERSONAL.stabilizerDistance())) instanceof WormholeCoreBlockEntity personal) {
			return personal;
		}
		// Check Colossal 3x3 array distance (21) with [-1..1] height/tangent offset
		int d = WormholeTier.COLOSSAL.stabilizerDistance();
		Direction right = inward.getClockWise();
		BlockPos base = pos.relative(inward, d);
		for (int dy = -1; dy <= 1; dy++) {
			for (int t = -1; t <= 1; t++) {
				BlockPos candidate = base.above(dy).relative(right, t);
				if (level.getBlockEntity(candidate) instanceof WormholeCoreBlockEntity colossal) {
					return colossal;
				}
			}
		}
		return null;
	}
}

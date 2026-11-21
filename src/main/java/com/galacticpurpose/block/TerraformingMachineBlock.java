package com.galacticpurpose.block;

import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.terraforming.TerraformingMachineType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Shared block implementation for all Mars terraforming machines.
 */
public class TerraformingMachineBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public static final MapCodec<TerraformingMachineBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			propertiesCodec(),
			Codec.STRING.xmap(TerraformingMachineType::byId, TerraformingMachineType::id)
				.fieldOf("machine_type")
				.forGetter(TerraformingMachineBlock::machineType)
		).apply(instance, TerraformingMachineBlock::new)
	);

	private final TerraformingMachineType machineType;

	public TerraformingMachineBlock(Properties properties, TerraformingMachineType machineType) {
		super(properties);
		this.machineType = machineType;
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(LIT, false));
	}

	public TerraformingMachineType machineType() {
		return this.machineType;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(LIT, false);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TerraformingMachineBlockEntity(this.machineType, pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
		Level level,
		BlockState state,
		BlockEntityType<T> blockEntityType
	) {
		if (level.isClientSide) {
			return null;
		}
		return createTickerHelper(
			blockEntityType,
			ModBlockEntities.forMachine(this.machineType),
			TerraformingMachineBlockEntity::serverTick
		);
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof TerraformingMachineBlockEntity machine) {
			player.openMenu(machine);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TerraformingMachineBlockEntity machine) {
			Containers.dropContents(level, pos, machine);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

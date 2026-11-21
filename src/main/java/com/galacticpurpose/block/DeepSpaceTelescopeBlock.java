package com.galacticpurpose.block;

import com.galacticpurpose.climate.OverworldHazardTicker;
import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

/**
 * Deep Space Telescope built on Mars under open sky to scan and chart Exoplanet Coordinate Cards.
 */
public class DeepSpaceTelescopeBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final MapCodec<DeepSpaceTelescopeBlock> CODEC = simpleCodec(DeepSpaceTelescopeBlock::new);

	public DeepSpaceTelescopeBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
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
		return new DeepSpaceTelescopeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
		Level level,
		BlockState state,
		BlockEntityType<T> type
	) {
		if (level.isClientSide) {
			return null;
		}
		return createTickerHelper(type, ModBlockEntities.DEEP_SPACE_TELESCOPE, DeepSpaceTelescopeBlockEntity::serverTick);
	}

	@Override
	protected ItemInteractionResult useItemOn(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hit
	) {
		if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof DeepSpaceTelescopeBlockEntity telescope) {
			EnergyStorage itemEnergy = EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
			if (itemEnergy != null && itemEnergy.supportsExtraction()) {
				if (!level.isClientSide) {
					long moved = EnergyStorageUtil.move(itemEnergy, telescope.energyStorage(), 10_000L, null);
					player.displayClientMessage(
						Component.translatable(
							"message.galacticpurpose.telescope_charged",
							moved,
							telescope.energyStorage().getAmount(),
							telescope.energyStorage().getCapacity()
						).withStyle(ChatFormatting.AQUA),
						true
					);
				}
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (!(level instanceof ServerLevel serverLevel)
			|| !(level.getBlockEntity(pos) instanceof DeepSpaceTelescopeBlockEntity telescope)) {
			return InteractionResult.SUCCESS;
		}

		if (!SpaceCompatManager.get().isMars(serverLevel)) {
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.telescope_mars_only").withStyle(ChatFormatting.RED),
				false
			);
			return InteractionResult.CONSUME;
		}

		if (!OverworldHazardTicker.isExposedToSky(serverLevel, pos.above())) {
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.telescope_blocked_sky").withStyle(ChatFormatting.RED),
				false
			);
			return InteractionResult.CONSUME;
		}

		telescope.handleInteract(player);
		return InteractionResult.CONSUME;
	}
}

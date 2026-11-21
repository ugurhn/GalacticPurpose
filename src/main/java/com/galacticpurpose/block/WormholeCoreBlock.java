package com.galacticpurpose.block;

import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import com.galacticpurpose.wormhole.WormholeLinkRegistry;
import com.galacticpurpose.wormhole.WormholeMultiblockValidator;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Central suspended containment core of the Wormhole Stabilizer multiblock.
 */
public class WormholeCoreBlock extends BaseEntityBlock {
	// 0 = IDLE, 1 = CHARGING / SHUTTING_DOWN, 2 = OPEN, 3 = UNSTABLE
	public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
	public static final MapCodec<WormholeCoreBlock> CODEC = simpleCodec(WormholeCoreBlock::new);

	public WormholeCoreBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(STAGE);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WormholeCoreBlockEntity(pos, state);
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
		return createTickerHelper(type, ModBlockEntities.WORMHOLE_CORE, WormholeCoreBlockEntity::serverTick);
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
		if (stack.is(ModItems.QUANTUM_CORE)) {
			if (!level.isClientSide && level.getBlockEntity(pos) instanceof WormholeCoreBlockEntity core) {
				core.tryUpgradeWithQuantumCore(player, stack);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
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
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof WormholeCoreBlockEntity core) {
			WormholeMultiblockValidator.ValidationResult validation =
				WormholeMultiblockValidator.validate(level, pos);
			if (!validation.valid()) {
				BlockPos off = validation.missingOffset();
				player.displayClientMessage(
					Component.translatable(
						"message.galacticpurpose.wormhole_incomplete",
						Component.translatable(validation.expectedBlockKey()),
						off.getX(),
						off.getY(),
						off.getZ()
					).withStyle(ChatFormatting.YELLOW),
					false
				);
			}
			player.openMenu(core);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				WormholeLinkRegistry.get(serverLevel.getServer())
					.updateStatus(serverLevel.dimension().location(), pos, WormholeLinkCalculator.LinkStatus.DESTROYED);
			}
			if (level.getBlockEntity(pos) instanceof WormholeCoreBlockEntity core) {
				core.clearAllPortalBlocks(level, pos);
				Containers.dropContents(level, pos, core);
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}

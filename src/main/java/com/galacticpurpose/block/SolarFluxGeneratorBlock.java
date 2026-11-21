package com.galacticpurpose.block;

import com.galacticpurpose.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

/**
 * Survival-craftable Solar Flux Generator pushing standard Fabric Energy (E/RF) out all 6 faces
 * to Galactic Purpose, Galacticraft 5, Tech Reborn, Powah, AE2, or Modern Industrialization cables & machines.
 */
public class SolarFluxGeneratorBlock extends BaseEntityBlock {
	public static final MapCodec<SolarFluxGeneratorBlock> CODEC = simpleCodec(SolarFluxGeneratorBlock::new);

	public SolarFluxGeneratorBlock(Properties properties) {
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
		return new SolarFluxGeneratorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
		Level level, BlockState state, BlockEntityType<T> type
	) {
		if (level.isClientSide) {
			return null;
		}
		return createTickerHelper(type, ModBlockEntities.SOLAR_FLUX_GENERATOR, SolarFluxGeneratorBlockEntity::serverTick);
	}

	@Override
	protected ItemInteractionResult useItemOn(
		ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hit
	) {
		EnergyStorage itemEnergy = EnergyStorage.ITEM.find(stack, ContainerItemContext.forPlayerInteraction(player, hand));
		if (itemEnergy != null && itemEnergy.supportsInsertion()) {
			if (!level.isClientSide && level.getBlockEntity(pos) instanceof SolarFluxGeneratorBlockEntity gen) {
				long moved = EnergyStorageUtil.move(gen.energyStorage(), itemEnergy, 25_000L, null);
				player.displayClientMessage(
					Component.translatable(
						"message.galacticpurpose.solar_generator_charged_item",
						moved,
						itemEnergy.getAmount(),
						itemEnergy.getCapacity()
					).withStyle(ChatFormatting.AQUA),
					true
				);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
	) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof SolarFluxGeneratorBlockEntity gen) {
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.solar_generator_status",
					gen.lastOutputPerTick(),
					gen.energyStorage().getAmount(),
					gen.energyStorage().getCapacity()
				).withStyle(gen.lastOutputPerTick() > 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
				true
			);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}

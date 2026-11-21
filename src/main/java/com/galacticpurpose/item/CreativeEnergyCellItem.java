package com.galacticpurpose.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.List;

/**
 * Portable creative/testing energy cell providing continuous power inside machine battery slots.
 */
public class CreativeEnergyCellItem extends Item {
	public static final EnergyStorage INFINITE_SOURCE = new EnergyStorage() {
		@Override
		public boolean supportsInsertion() {
			return false;
		}

		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			return 0;
		}

		@Override
		public boolean supportsExtraction() {
			return true;
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			return Math.max(0L, maxAmount);
		}

		@Override
		public long getAmount() {
			return 1_000_000L;
		}

		@Override
		public long getCapacity() {
			return 1_000_000L;
		}
	};

	public CreativeEnergyCellItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.galacticpurpose.creative_energy_cell")
			.withStyle(ChatFormatting.AQUA));
	}
}

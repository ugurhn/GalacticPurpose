package com.galacticpurpose.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

/**
 * Survival-craftable rechargeable energy cell implementing universal Fabric Energy API (SimpleEnergyItem).
 * Compatible with Galacticraft 5, Tech Reborn, Powah, Modern Industrialization, AE2, Ad Astra, and all Galactic Purpose machines.
 */
public class LeadEnergyCellItem extends Item implements SimpleEnergyItem {
	public static final long CAPACITY = 250_000L;
	public static final long MAX_IO = 4_000L;

	public LeadEnergyCellItem(Properties properties) {
		super(properties);
	}

	@Override
	public long getEnergyCapacity(ItemStack stack) {
		return CAPACITY;
	}

	@Override
	public long getEnergyMaxInput(ItemStack stack) {
		return MAX_IO;
	}

	@Override
	public long getEnergyMaxOutput(ItemStack stack) {
		return MAX_IO;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return getStoredEnergy(stack) > 0L;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		double frac = Math.min(1.0, Math.max(0.0, getStoredEnergy(stack) / (double) CAPACITY));
		return Math.round((float) (frac * 13.0));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		float frac = Mth.clamp((float) (getStoredEnergy(stack) / (double) CAPACITY), 0.0f, 1.0f);
		return Mth.hsvToRgb(0.50f - (1.0f - frac) * 0.38f, 0.85f, 1.0f);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		long stored = getStoredEnergy(stack);
		tooltip.add(Component.translatable("tooltip.galacticpurpose.energy_stored", stored, CAPACITY)
			.withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.galacticpurpose.lead_energy_cell")
			.withStyle(ChatFormatting.GRAY));
	}
}

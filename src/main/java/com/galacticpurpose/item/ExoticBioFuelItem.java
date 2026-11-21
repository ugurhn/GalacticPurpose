package com.galacticpurpose.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Stabilized exotic biofuel distilled from Mars bioluminescent flora to ignite Wormhole Stabilizers.
 */
public class ExoticBioFuelItem extends Item {
	public static final int FUEL_VALUE = 250;

	public ExoticBioFuelItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.galacticpurpose.exotic_bio_fuel", FUEL_VALUE)
			.withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}

package com.galacticpurpose.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * High-energy propellant canister powering Interstellar Starship launches, orbital burns, and return transits.
 * Synthesizable from Exotic Bio-Fuel OR native resources harvested on Hephaestus-IV, Pelagia-9, Nyx-0, and Sanctuary.
 */
public class StarshipFuelCellItem extends Item {
	public static final int FUEL_VALUE = 1000;

	public StarshipFuelCellItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.galacticpurpose.starship_fuel_cell", FUEL_VALUE)
			.withStyle(ChatFormatting.GOLD));
	}
}

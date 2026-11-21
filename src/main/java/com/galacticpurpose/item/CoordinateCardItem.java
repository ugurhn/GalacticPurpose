package com.galacticpurpose.item;

import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Star-chart coordinate card inserted into a Wormhole Stabilizer to dial an exoplanet destination.
 */
public class CoordinateCardItem extends Item {
	private final ExoplanetRoute route;

	public CoordinateCardItem(ExoplanetRoute route, Properties properties) {
		super(properties);
		this.route = route;
	}

	public ExoplanetRoute route() {
		return this.route;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(this.route.translationKey()).withStyle(ChatFormatting.AQUA));
		if (this.route.requiresQuantumCore()) {
			tooltip.add(Component.translatable("tooltip.galacticpurpose.requires_quantum_core")
				.withStyle(ChatFormatting.GOLD));
		}
	}
}

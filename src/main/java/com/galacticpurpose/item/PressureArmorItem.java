package com.galacticpurpose.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;

/**
 * Reinforced deep-sea diving suit resisting crushing abyssal pressure on Pelagia-9 and aiding underwater exploration.
 */
public class PressureArmorItem extends SituationalArmorItem {
	public PressureArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
		super(
			material,
			type,
			properties,
			-0.05,
			"pressure_suit_weight_",
			"tooltip.galacticpurpose.pressure_suit",
			ChatFormatting.AQUA
		);
	}

	public static boolean hasFullSuit(LivingEntity entity) {
		return hasFullSetOf(entity, PressureArmorItem.class);
	}
}

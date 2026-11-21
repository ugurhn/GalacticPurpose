package com.galacticpurpose.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;

/**
 * Reflective thermal armor shielding extreme heat (Earth collapse, Hephaestus-IV) and cryogenic cold (Nyx-0).
 */
public class ThermalArmorItem extends SituationalArmorItem {
	public ThermalArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
		super(
			material,
			type,
			properties,
			-0.02,
			"thermal_suit_weight_",
			"tooltip.galacticpurpose.thermal_suit",
			ChatFormatting.GOLD
		);
	}

	public static boolean hasFullSuit(LivingEntity entity) {
		return hasFullSetOf(entity, ThermalArmorItem.class);
	}
}

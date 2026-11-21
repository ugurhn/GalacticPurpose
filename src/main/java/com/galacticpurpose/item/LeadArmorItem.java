package com.galacticpurpose.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;

/**
 * Heavy lead-lined radiation armor: blocks solar storm radiation at the cost of weight and low melee protection.
 */
public class LeadArmorItem extends SituationalArmorItem {
	public LeadArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
		super(
			material,
			type,
			properties,
			-0.04,
			"lead_suit_weight_",
			"tooltip.galacticpurpose.lead_suit",
			ChatFormatting.YELLOW
		);
	}

	public static boolean hasFullSuit(LivingEntity entity) {
		return hasFullSetOf(entity, LeadArmorItem.class);
	}
}

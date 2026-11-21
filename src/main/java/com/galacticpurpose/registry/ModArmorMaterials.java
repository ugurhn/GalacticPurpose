package com.galacticpurpose.registry;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers situational armor materials adhering to the Anti-God-Mode balance rule.
 */
public final class ModArmorMaterials {
	public static final Holder<ArmorMaterial> LEAD_SUIT = registerSuit(
		"lead_suit", 1, 2, 3, 1, 9, 0.0f, 0.1f, () -> Ingredient.of(ModItems.LEAD_INGOT)
	);
	public static final Holder<ArmorMaterial> THERMAL_SUIT = registerSuit(
		"thermal_suit", 2, 3, 4, 2, 12, 0.5f, 0.0f, () -> Ingredient.of(Items.GOLD_INGOT)
	);
	public static final Holder<ArmorMaterial> PRESSURE_SUIT = registerSuit(
		"pressure_suit", 2, 3, 4, 2, 10, 0.5f, 0.05f, () -> Ingredient.of(Items.COPPER_INGOT)
	);

	private ModArmorMaterials() {
	}

	private static Holder<ArmorMaterial> registerSuit(
		String id,
		int boots,
		int leggings,
		int chestplate,
		int helmet,
		int enchantability,
		float toughness,
		float knockbackResistance,
		Supplier<Ingredient> repairIngredient
	) {
		return Registry.registerForHolder(
			BuiltInRegistries.ARMOR_MATERIAL,
			GalacticPurpose.id(id),
			new ArmorMaterial(
				Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
					map.put(ArmorItem.Type.BOOTS, boots);
					map.put(ArmorItem.Type.LEGGINGS, leggings);
					map.put(ArmorItem.Type.CHESTPLATE, chestplate);
					map.put(ArmorItem.Type.HELMET, helmet);
					map.put(ArmorItem.Type.BODY, chestplate);
				}),
				enchantability,
				SoundEvents.ARMOR_EQUIP_IRON,
				repairIngredient,
				List.of(new ArmorMaterial.Layer(GalacticPurpose.id(id))),
				toughness,
				knockbackResistance
			)
		);
	}

	public static void register() {
		// Class initialization registers the material holder.
	}
}

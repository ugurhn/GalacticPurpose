package com.galacticpurpose.registry;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.item.CoordinateCardItem;
import com.galacticpurpose.item.CreativeEnergyCellItem;
import com.galacticpurpose.item.ExoticBioFuelItem;
import com.galacticpurpose.item.ExpeditionLogItem;
import com.galacticpurpose.item.LeadArmorItem;
import com.galacticpurpose.item.LeadEnergyCellItem;
import com.galacticpurpose.item.MissionCodexItem;
import com.galacticpurpose.item.PressureArmorItem;
import com.galacticpurpose.item.StarshipBlueprintItem;
import com.galacticpurpose.item.StarshipFlightDirectorItem;
import com.galacticpurpose.item.StarshipFuelCellItem;
import com.galacticpurpose.item.ThermalArmorItem;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import team.reborn.energy.api.EnergyStorage;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registers standalone items, situational armor pieces, and item API lookups.
 */
public final class ModItems {
	public static final Item MISSION_CODEX = register("mission_codex",
		new MissionCodexItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final Item LEAD_ENERGY_CELL = register("lead_energy_cell",
		new LeadEnergyCellItem(new Item.Properties().stacksTo(1)));
	public static final Item CREATIVE_ENERGY_CELL = register("creative_energy_cell",
		new CreativeEnergyCellItem(new Item.Properties().stacksTo(1)));
	public static final Item RAW_LEAD = register("raw_lead", new Item(new Item.Properties()));
	public static final Item LEAD_INGOT = register("lead_ingot", new Item(new Item.Properties()));
	public static final Item RAW_IGNIS = register("raw_ignis", new Item(new Item.Properties()));
	public static final Item IGNIS_INGOT = register("ignis_ingot", new Item(new Item.Properties()));
	public static final Item STARSHIP_FUEL_CELL = register("starship_fuel_cell",
		new StarshipFuelCellItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
	public static final Item STARSHIP_BLUEPRINT = register("starship_blueprint",
		new StarshipBlueprintItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	public static final Item FLIGHT_DIRECTOR = register("flight_director",
		new StarshipFlightDirectorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	public static final Item MARS_BIO_ESSENCE = register("mars_bio_essence", new Item(new Item.Properties()));
	public static final Item EXOTIC_BIO_FUEL = register("exotic_bio_fuel",
		new ExoticBioFuelItem(new Item.Properties()));
	public static final Item THERMIUM_CRYSTAL = register("thermium_crystal", new Item(new Item.Properties()));
	public static final Item FUSION_CORE = register("fusion_core",
		new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
	public static final Item BIO_STABILIZER = register("bio_stabilizer",
		new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
	public static final Item QUANTUM_CRYSTAL = register("quantum_crystal",
		new Item(new Item.Properties().rarity(Rarity.RARE)));
	public static final Item QUANTUM_CORE = register("quantum_core",
		new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

	public static final Item EXPEDITION_LOG_1 = register("expedition_log_1",
		new ExpeditionLogItem(1, new Item.Properties().stacksTo(1)));
	public static final Item EXPEDITION_LOG_2 = register("expedition_log_2",
		new ExpeditionLogItem(2, new Item.Properties().stacksTo(1)));
	public static final Item EXPEDITION_LOG_3 = register("expedition_log_3",
		new ExpeditionLogItem(3, new Item.Properties().stacksTo(1)));
	public static final Item EXPEDITION_LOG_4 = register("expedition_log_4",
		new ExpeditionLogItem(4, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

	private static final Map<ExoplanetRoute, Item> COORDINATE_CARDS = new EnumMap<>(ExoplanetRoute.class);

	static {
		for (ExoplanetRoute route : ExoplanetRoute.values()) {
			COORDINATE_CARDS.put(
				route,
				register("coordinate_card_" + route.id(), new CoordinateCardItem(route, new Item.Properties().stacksTo(1)))
			);
		}
	}

	private static final int LEAD_SUIT_DURABILITY_MULTIPLIER = 16;
	private static final int SPECIALIZED_SUIT_DURABILITY_MULTIPLIER = 22;

	public static final Item LEAD_HELMET = registerSuitPiece(
		"lead_helmet", ArmorItem.Type.HELMET, LEAD_SUIT_DURABILITY_MULTIPLIER,
		props -> new LeadArmorItem(ModArmorMaterials.LEAD_SUIT, ArmorItem.Type.HELMET, props)
	);
	public static final Item LEAD_CHESTPLATE = registerSuitPiece(
		"lead_chestplate", ArmorItem.Type.CHESTPLATE, LEAD_SUIT_DURABILITY_MULTIPLIER,
		props -> new LeadArmorItem(ModArmorMaterials.LEAD_SUIT, ArmorItem.Type.CHESTPLATE, props)
	);
	public static final Item LEAD_LEGGINGS = registerSuitPiece(
		"lead_leggings", ArmorItem.Type.LEGGINGS, LEAD_SUIT_DURABILITY_MULTIPLIER,
		props -> new LeadArmorItem(ModArmorMaterials.LEAD_SUIT, ArmorItem.Type.LEGGINGS, props)
	);
	public static final Item LEAD_BOOTS = registerSuitPiece(
		"lead_boots", ArmorItem.Type.BOOTS, LEAD_SUIT_DURABILITY_MULTIPLIER,
		props -> new LeadArmorItem(ModArmorMaterials.LEAD_SUIT, ArmorItem.Type.BOOTS, props)
	);

	public static final Item THERMAL_HELMET = registerSuitPiece(
		"thermal_helmet", ArmorItem.Type.HELMET, SPECIALIZED_SUIT_DURABILITY_MULTIPLIER,
		props -> new ThermalArmorItem(ModArmorMaterials.THERMAL_SUIT, ArmorItem.Type.HELMET, props)
	);
	public static final Item THERMAL_CHESTPLATE = registerSuitPiece(
		"thermal_chestplate", ArmorItem.Type.CHESTPLATE, SPECIALIZED_SUIT_DURABILITY_MULTIPLIER,
		props -> new ThermalArmorItem(ModArmorMaterials.THERMAL_SUIT, ArmorItem.Type.CHESTPLATE, props)
	);
	public static final Item THERMAL_LEGGINGS = registerSuitPiece(
		"thermal_leggings", ArmorItem.Type.LEGGINGS, SPECIALIZED_SUIT_DURABILITY_MULTIPLIER,
		props -> new ThermalArmorItem(ModArmorMaterials.THERMAL_SUIT, ArmorItem.Type.LEGGINGS, props)
	);
	public static final Item THERMAL_BOOTS = registerSuitPiece(
		"thermal_boots", ArmorItem.Type.BOOTS, SPECIALIZED_SUIT_DURABILITY_MULTIPLIER,
		props -> new ThermalArmorItem(ModArmorMaterials.THERMAL_SUIT, ArmorItem.Type.BOOTS, props)
	);

	public static final Item PRESSURE_HELMET = registerSuitPiece(
		"pressure_helmet", ArmorItem.Type.HELMET, SPECIALIZED_SUIT_DURABILITY_MULTIPLIER,
		props -> new PressureArmorItem(ModArmorMaterials.PRESSURE_SUIT, ArmorItem.Type.HELMET, props)
	);
	public static final Item PRESSURE_CHESTPLATE = registerSuitPiece(
		"pressure_chestplate", ArmorItem.Type.CHESTPLATE, SPECIALIZED_SUIT_DURABILITY_MULTIPLIER,
		props -> new PressureArmorItem(ModArmorMaterials.PRESSURE_SUIT, ArmorItem.Type.CHESTPLATE, props)
	);
	public static final Item PRESSURE_LEGGINGS = registerSuitPiece(
		"pressure_leggings", ArmorItem.Type.LEGGINGS, SPECIALIZED_SUIT_DURABILITY_MULTIPLIER,
		props -> new PressureArmorItem(ModArmorMaterials.PRESSURE_SUIT, ArmorItem.Type.LEGGINGS, props)
	);
	public static final Item PRESSURE_BOOTS = registerSuitPiece(
		"pressure_boots", ArmorItem.Type.BOOTS, SPECIALIZED_SUIT_DURABILITY_MULTIPLIER,
		props -> new PressureArmorItem(ModArmorMaterials.PRESSURE_SUIT, ArmorItem.Type.BOOTS, props)
	);

	private ModItems() {
	}

	private static Item registerSuitPiece(
		String id,
		ArmorItem.Type type,
		int durabilityMultiplier,
		Function<Item.Properties, Item> factory
	) {
		return register(id, factory.apply(new Item.Properties().durability(type.getDurability(durabilityMultiplier))));
	}

	private static Item register(String id, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, GalacticPurpose.id(id), item);
	}

	public static Item forCoordinateCard(ExoplanetRoute route) {
		return COORDINATE_CARDS.get(route);
	}

	public static void register() {
		EnergyStorage.ITEM.registerForItems(
			(stack, ctx) -> CreativeEnergyCellItem.INFINITE_SOURCE,
			CREATIVE_ENERGY_CELL
		);
	}
}

package com.galacticpurpose.registry;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Creative mode inventory tab containing all Galactic Purpose blocks and equipment.
 */
public final class ModItemGroups {
	public static final CreativeModeTab MAIN_TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		GalacticPurpose.id("main"),
		FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.galacticpurpose.main"))
			.icon(() -> new ItemStack(ModBlocks.THERMAL_HEATER))
			.displayItems((params, output) -> {
				output.accept(ModItems.MISSION_CODEX);
				output.accept(ModBlocks.SOLAR_FLUX_GENERATOR);
				output.accept(ModItems.LEAD_ENERGY_CELL);
				output.accept(ModBlocks.THERMAL_HEATER);
				output.accept(ModBlocks.PRESSURE_DRILL);
				output.accept(ModBlocks.FLORA_OXYGENATOR);
				output.accept(ModBlocks.ATMOSPHERE_CONDENSER);
				output.accept(ModBlocks.BIO_DISTILLER);
				output.accept(ModBlocks.DEEP_SPACE_TELESCOPE);
				output.accept(ModBlocks.WORMHOLE_CORE);
				output.accept(ModBlocks.WORMHOLE_STABILIZER_PYLON);
				output.accept(ModBlocks.WORMHOLE_INJECTOR);
				output.accept(ModBlocks.SOLAR_TELEMETRY_MONITOR);
				output.accept(ModBlocks.LEAD_ORE);
				output.accept(ModBlocks.DEEPSLATE_LEAD_ORE);
				output.accept(ModBlocks.THERMAL_SHARD_ORE);
				output.accept(ModBlocks.FUSION_RELAY_WRECKAGE);
				output.accept(ModBlocks.ABYSSAL_VENT);
				output.accept(ModBlocks.QUANTUM_CRYSTAL_ORE);
				output.accept(ModBlocks.CRYO_BEACON);
				output.accept(ModBlocks.LEAD_CONCRETE);
				output.accept(ModBlocks.LEADED_GLASS);
				output.accept(ModBlocks.MARS_BIO_BLOOM);
				output.accept(ModItems.RAW_LEAD);
				output.accept(ModItems.LEAD_INGOT);
				output.accept(ModItems.RAW_IGNIS);
				output.accept(ModItems.IGNIS_INGOT);
				output.accept(ModItems.STARSHIP_FUEL_CELL);
				output.accept(ModItems.STARSHIP_BLUEPRINT);
				output.accept(ModItems.FLIGHT_DIRECTOR);
				// Starship & Space Engineering Blocks
				output.accept(ModBlocks.SHIP_ASSEMBLY_PAD);
				output.accept(ModBlocks.STARSHIP_HELM);
				output.accept(ModBlocks.STARSHIP_CARGO_LOCKER);
				output.accept(ModBlocks.STARSHIP_HULL_PLATING);
				output.accept(ModBlocks.STARSHIP_RIBBED_PANEL);
				output.accept(ModBlocks.STARSHIP_GRATE);
				output.accept(ModBlocks.REINFORCED_VIEWPORT_GLASS);
				output.accept(ModBlocks.STARSHIP_THRUSTER_BLOCK);
				output.accept(ModBlocks.STARSHIP_LIGHT_STRIP);
				output.accept(ModBlocks.IGNIS_ALLOY_BLOCK);
				// Hephaestus-IV Block Set
				output.accept(ModBlocks.SCORCHED_BASALT);
				output.accept(ModBlocks.VOLCANIC_SLAGSTONE);
				output.accept(ModBlocks.IGNIS_ORE);
				output.accept(ModBlocks.PYROCLAST_BRICKS);
				output.accept(ModBlocks.CHISELED_PYROCLAST);
				output.accept(ModBlocks.PYROCLAST_PILLAR);
				output.accept(ModBlocks.MAGMA_CONDUIT_LAMP);
				output.accept(ModBlocks.EMBER_ASH);
				// Pelagia-9 Block Set
				output.accept(ModBlocks.ABYSSAL_SHALE);
				output.accept(ModBlocks.ABYSSAL_CORALSTONE);
				output.accept(ModBlocks.PELAGIC_BRICKS);
				output.accept(ModBlocks.CARVED_PELAGIC_TILE);
				output.accept(ModBlocks.PELAGIC_PILLAR);
				output.accept(ModBlocks.BIOLUMINESCENT_LAMP);
				output.accept(ModBlocks.LUMINESCENT_SEA_POD);
				output.accept(ModBlocks.PRESSURE_HULL_PLATING);
				// Nyx-0 Block Set
				output.accept(ModBlocks.CRYO_SLATE);
				output.accept(ModBlocks.FROST_OBSIDIAN);
				output.accept(ModBlocks.CRYO_BRICKS);
				output.accept(ModBlocks.CARVED_CRYO_TILE);
				output.accept(ModBlocks.CRYO_PILLAR);
				output.accept(ModBlocks.VOID_CRYSTAL_LAMP);
				output.accept(ModBlocks.FROZEN_QUANTUM_SPIRE);
				output.accept(ModBlocks.ZERO_K_PLATING);
				// Sanctuary Block Set
				output.accept(ModBlocks.SANCTUARY_LOAM);
				output.accept(ModBlocks.LUMINOUS_LIMESTONE);
				output.accept(ModBlocks.SANCTUARY_BRICKS);
				output.accept(ModBlocks.CARVED_SANCTUARY_STONE);
				output.accept(ModBlocks.SANCTUARY_PILLAR);
				output.accept(ModBlocks.AETHER_LANTERN);
				output.accept(ModBlocks.SANCTUARY_BLOSSOM);
				output.accept(ModBlocks.ARK_TITANIUM_PLATING);
				output.accept(ModItems.MARS_BIO_ESSENCE);
				output.accept(ModItems.EXOTIC_BIO_FUEL);
				output.accept(ModItems.THERMIUM_CRYSTAL);
				output.accept(ModItems.FUSION_CORE);
				output.accept(ModItems.BIO_STABILIZER);
				output.accept(ModItems.QUANTUM_CRYSTAL);
				output.accept(ModItems.QUANTUM_CORE);
				for (ExoplanetRoute route : ExoplanetRoute.values()) {
					output.accept(ModItems.forCoordinateCard(route));
				}
				output.accept(ModItems.EXPEDITION_LOG_1);
				output.accept(ModItems.EXPEDITION_LOG_2);
				output.accept(ModItems.EXPEDITION_LOG_3);
				output.accept(ModItems.EXPEDITION_LOG_4);
				output.accept(ModItems.LEAD_HELMET);
				output.accept(ModItems.LEAD_CHESTPLATE);
				output.accept(ModItems.LEAD_LEGGINGS);
				output.accept(ModItems.LEAD_BOOTS);
				output.accept(ModItems.THERMAL_HELMET);
				output.accept(ModItems.THERMAL_CHESTPLATE);
				output.accept(ModItems.THERMAL_LEGGINGS);
				output.accept(ModItems.THERMAL_BOOTS);
				output.accept(ModItems.PRESSURE_HELMET);
				output.accept(ModItems.PRESSURE_CHESTPLATE);
				output.accept(ModItems.PRESSURE_LEGGINGS);
				output.accept(ModItems.PRESSURE_BOOTS);
				output.accept(ModItems.CREATIVE_ENERGY_CELL);
			})
			.build()
	);

	private ModItemGroups() {
	}

	public static void register() {
		// Class initialization performs static registry bindings.
	}
}

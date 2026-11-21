package com.galacticpurpose;

import com.galacticpurpose.climate.OverworldHazardTicker;
import com.galacticpurpose.codex.MissionCodexGrantState;
import com.galacticpurpose.command.GalacticPurposeCommands;
import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.config.ModConfig;
import com.galacticpurpose.exoplanet.ExoplanetHazardTicker;
import com.galacticpurpose.network.ClientSoundEventPayload;
import com.galacticpurpose.network.ClimateSyncPayload;
import com.galacticpurpose.network.ExoplanetSyncPayload;
import com.galacticpurpose.network.MarsSyncPayload;
import com.galacticpurpose.network.OrbitalTransitSyncPayload;
import com.galacticpurpose.network.StarshipActionPayload;
import com.galacticpurpose.network.StarshipInputPayload;
import com.galacticpurpose.registry.ModArmorMaterials;
import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModEntities;
import com.galacticpurpose.registry.ModItemGroups;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.registry.ModMenus;
import com.galacticpurpose.ship.OrbitalTransitManager;
import com.galacticpurpose.terraforming.MarsTerraformingState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for Galactic Purpose.
 */
public class GalacticPurpose implements ModInitializer {
	public static final String MOD_ID = "galacticpurpose";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceKey<PlacedFeature> LEAD_ORE_PLACED_KEY =
		ResourceKey.create(Registries.PLACED_FEATURE, id("ore_lead"));

	@Override
	public void onInitialize() {
		ModConfig.loadOrCreate(FabricLoader.getInstance().getConfigDir());
		SpaceCompatManager.initialize();

		ModArmorMaterials.register();
		ModBlocks.register();
		ModItems.register();
		ModBlockEntities.register();
		ModEntities.register();
		ModMenus.register();
		ModItemGroups.register();

		BiomeModifications.addFeature(
			BiomeSelectors.foundInOverworld(),
			GenerationStep.Decoration.UNDERGROUND_ORES,
			LEAD_ORE_PLACED_KEY
		);

		PayloadTypeRegistry.playS2C().register(ClimateSyncPayload.TYPE, ClimateSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MarsSyncPayload.TYPE, MarsSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ExoplanetSyncPayload.TYPE, ExoplanetSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OrbitalTransitSyncPayload.TYPE, OrbitalTransitSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ClientSoundEventPayload.TYPE, ClientSoundEventPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(StarshipActionPayload.TYPE, StarshipActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(StarshipInputPayload.TYPE, StarshipInputPayload.CODEC);

		OverworldHazardTicker.register();
		MarsTerraformingState.registerEvents();
		ExoplanetHazardTicker.register();
		MissionCodexGrantState.register();
		OrbitalTransitManager.register();
		GalacticPurposeCommands.register();

		LOGGER.info("Galactic Purpose initialized (Galacticraft detected: {}, graceDays: {})",
			SpaceCompatManager.get().isGalacticraftLoaded(),
			ModConfig.get().graceDays());
	}

	/**
	 * Creates a mod-namespaced {@link ResourceLocation}.
	 */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}

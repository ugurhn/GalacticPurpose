package com.galacticpurpose.client;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.client.hud.ClientClimateState;
import com.galacticpurpose.client.hud.ClientExoplanetState;
import com.galacticpurpose.client.hud.ClientMarsState;
import com.galacticpurpose.client.hud.ClientOrbitalTransitState;
import com.galacticpurpose.client.hud.ClimateHudOverlay;
import com.galacticpurpose.client.hud.ExoplanetHudOverlay;
import com.galacticpurpose.client.hud.MarsHudOverlay;
import com.galacticpurpose.client.hud.StarshipCockpitHud;
import com.galacticpurpose.client.render.InterstellarShipRenderer;
import com.galacticpurpose.client.render.OrbitalShipPlumeRenderer;
import com.galacticpurpose.client.render.SingularityCollapseRenderer;
import com.galacticpurpose.client.render.StarshipSeatRenderer;
import com.galacticpurpose.client.render.WormholeCoreBlockEntityRenderer;
import com.galacticpurpose.client.render.WormholeHologramRenderer;
import com.galacticpurpose.client.screen.BioDistillerScreen;
import com.galacticpurpose.client.screen.MissionCodexScreen;
import com.galacticpurpose.client.screen.StarshipScreen;
import com.galacticpurpose.client.screen.TerraformingMachineScreen;
import com.galacticpurpose.client.screen.WormholeCoreScreen;
import com.galacticpurpose.client.sky.ErebusDimensionEffects;
import com.galacticpurpose.client.sky.ErebusSkyRenderer;
import com.galacticpurpose.client.sky.OrbitalTransitSkyRenderer;
import com.galacticpurpose.client.sound.ClientSoundController;
import com.galacticpurpose.item.MissionCodexItem;
import com.galacticpurpose.network.ClientSoundEventPayload;
import com.galacticpurpose.network.ClimateSyncPayload;
import com.galacticpurpose.network.ExoplanetSyncPayload;
import com.galacticpurpose.network.MarsSyncPayload;
import com.galacticpurpose.network.OrbitalTransitSyncPayload;
import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModEntities;
import com.galacticpurpose.registry.ModMenus;
import com.galacticpurpose.ship.OrbitalTransitManager;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side entrypoint responsible for HUD overlays, screens, block render layers, and sky tinting.
 */
public class GalacticPurposeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.TERRAFORMING_MACHINE, TerraformingMachineScreen::new);
		MenuScreens.register(ModMenus.BIO_DISTILLER, BioDistillerScreen::new);
		MenuScreens.register(ModMenus.WORMHOLE_CORE, WormholeCoreScreen::new);
		MenuScreens.register(ModMenus.STARSHIP, StarshipScreen::new);
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LEADED_GLASS, RenderType.translucent());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.REINFORCED_VIEWPORT_GLASS, RenderType.translucent());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WORMHOLE_PORTAL, RenderType.translucent());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MARS_BIO_BLOOM, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LUMINESCENT_SEA_POD, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FROZEN_QUANTUM_SPIRE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SANCTUARY_BLOSSOM, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STARSHIP_GRATE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WORMHOLE_STABILIZER_PYLON, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WORMHOLE_INJECTOR, RenderType.cutout());

		BlockEntityRendererRegistry.register(ModBlockEntities.WORMHOLE_CORE, WormholeCoreBlockEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.SINGULARITY_COLLAPSE, SingularityCollapseRenderer::new);
		EntityRendererRegistry.register(ModEntities.INTERSTELLAR_SHIP, InterstellarShipRenderer::new);
		EntityRendererRegistry.register(ModEntities.STARSHIP_SEAT, StarshipSeatRenderer::new);
		StarshipCockpitHud.registerKeyBindings();
		com.galacticpurpose.client.sound.NasaAudioBank.preloadAllAsync();
		MissionCodexItem.setClientHooks(MissionCodexScreen::open, WormholeHologramRenderer::toggleAt);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(WormholeHologramRenderer::render);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(OrbitalShipPlumeRenderer::render);
		ClientTickEvents.END_CLIENT_TICK.register(ClientSoundController::tick);
		ClientTickEvents.END_CLIENT_TICK.register(StarshipCockpitHud::tickInput);

		ClientPlayNetworking.registerGlobalReceiver(ClimateSyncPayload.TYPE, (payload, context) ->
			context.client().execute(() -> ClientClimateState.apply(payload)));
		ClientPlayNetworking.registerGlobalReceiver(MarsSyncPayload.TYPE, (payload, context) ->
			context.client().execute(() -> ClientMarsState.apply(payload)));
		ClientPlayNetworking.registerGlobalReceiver(ExoplanetSyncPayload.TYPE, (payload, context) ->
			context.client().execute(() -> ClientExoplanetState.apply(payload)));
		ClientPlayNetworking.registerGlobalReceiver(OrbitalTransitSyncPayload.TYPE, (payload, context) ->
			context.client().execute(() -> ClientOrbitalTransitState.apply(payload)));
		ClientPlayNetworking.registerGlobalReceiver(ClientSoundEventPayload.TYPE, (payload, context) ->
			context.client().execute(() -> ClientSoundController.handleSoundEvent(payload)));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientClimateState.reset();
			ClientMarsState.reset();
			ClientExoplanetState.reset();
			ClientOrbitalTransitState.reset();
			ClientSoundController.reset();
			WormholeHologramRenderer.clear();
		});

		HudRenderCallback.EVENT.register(ClimateHudOverlay::render);
		HudRenderCallback.EVENT.register(MarsHudOverlay::render);
		HudRenderCallback.EVENT.register(ExoplanetHudOverlay::render);
		HudRenderCallback.EVENT.register(StarshipCockpitHud::render);
		registerExoplanetRendering();
		DimensionRenderingRegistry.registerDimensionEffects(
			GalacticPurpose.id("orbital_transit"),
			new ErebusDimensionEffects(Float.NaN, new Vec3(0.01, 0.01, 0.02), true)
		);
		DimensionRenderingRegistry.registerSkyRenderer(
			OrbitalTransitManager.ORBITAL_TRANSIT_KEY,
			new OrbitalTransitSkyRenderer()
		);
		GalacticPurpose.LOGGER.info("Galactic Purpose client initialized.");
	}

	private static void registerExoplanetRendering() {
		// All 4 planets orbit the SAME golden-white Erebus Singularity (~1.7x Gargantua majestic scale!); differences come from orbital distance & planet atmosphere!
		registerPlanetVisual(
			ExoplanetRoute.HEPHAESTUS_IV, "erebus_forge",
			new Vec3(0.32, 0.12, 0.05), false,
			new ErebusSkyRenderer.SkyAtmosphere(0x160704, 0x4E1B0B, 0x060202, 0x1A0805, 1.04f, 0.90f, 0.78f, 2.20f, -24.0f)
		);
		registerPlanetVisual(
			ExoplanetRoute.PELAGIA_9, "erebus_abyss",
			new Vec3(0.06, 0.19, 0.34), false,
			new ErebusSkyRenderer.SkyAtmosphere(0x061024, 0x153B5E, 0x02040C, 0x071426, 0.94f, 0.98f, 1.08f, 2.75f, -32.0f)
		);
		registerPlanetVisual(
			ExoplanetRoute.NYX_0, "erebus_void",
			new Vec3(0.03, 0.03, 0.06), true,
			new ErebusSkyRenderer.SkyAtmosphere(0x020206, 0x0A0B18, 0x010103, 0x04040C, 1.00f, 1.00f, 1.02f, 1.18f, -15.0f)
		);
		registerPlanetVisual(
			ExoplanetRoute.SANCTUARY, "erebus_sanctuary",
			new Vec3(0.45, 0.72, 0.78), false,
			new ErebusSkyRenderer.SkyAtmosphere(0x12325C, 0x529CB0, 0x050C1C, 0x102438, 1.02f, 1.00f, 0.96f, 1.32f, -18.0f)
		);
	}

	private static void registerPlanetVisual(
		ExoplanetRoute route,
		String effectsId,
		Vec3 fogColor,
		boolean permanentNight,
		ErebusSkyRenderer.SkyAtmosphere atmosphere
	) {
		ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, route.dimensionId());
		DimensionRenderingRegistry.registerDimensionEffects(
			GalacticPurpose.id(effectsId),
			new ErebusDimensionEffects(Float.NaN, fogColor, permanentNight)
		);
		DimensionRenderingRegistry.registerSkyRenderer(dimKey, new ErebusSkyRenderer(atmosphere));
	}
}

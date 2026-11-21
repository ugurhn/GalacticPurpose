package com.galacticpurpose.mixin;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Ensures Galactic Purpose exoplanet dimensions are initialized on both pre-existing saves and GameTest servers.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Shadow @Final private Map<ResourceKey<Level>, ServerLevel> levels;
	@Shadow @Final private Executor executor;
	@Shadow @Final protected LevelStorageSource.LevelStorageAccess storageSource;
	@Shadow @Final protected WorldData worldData;

	@Shadow
	public abstract RegistryAccess.Frozen registryAccess();

	@Shadow
	public abstract ServerLevel overworld();

	@Inject(
		method = "createLevels",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/border/WorldBorder;applySettings(Lnet/minecraft/world/level/border/WorldBorder$Settings;)V"
		)
	)
	private void galacticpurpose$registerMissingExoplanets(ChunkProgressListener listener, CallbackInfo ci) {
		ServerLevel overworld = this.overworld();
		if (overworld == null) {
			return;
		}

		RegistryAccess.Frozen access = this.registryAccess();
		Registry<DimensionType> dimTypes = access.registryOrThrow(Registries.DIMENSION_TYPE);
		Registry<Biome> biomes = access.registryOrThrow(Registries.BIOME);
		Registry<NoiseGeneratorSettings> noiseSettings = access.registryOrThrow(Registries.NOISE_SETTINGS);

		ServerLevelData overworldData = this.worldData.overworldData();
		WorldBorder overworldBorder = overworld.getWorldBorder();
		long obfuscatedSeed = BiomeManager.obfuscateSeed(this.worldData.worldGenOptions().seed());
		boolean debugWorld = this.worldData.isDebugWorld();

		for (ExoplanetRoute route : ExoplanetRoute.values()) {
			galacticpurpose$tryInitDimension(
				route.dimensionId(), GalacticPurpose.id(route.biomeId()),
				dimTypes, biomes, noiseSettings, overworld, overworldData, overworldBorder,
				listener, debugWorld, obfuscatedSeed
			);
		}
		// Also register the deep-space walkable orbital_transit void dimension on all saves & GameTest servers!
		net.minecraft.resources.ResourceLocation transitId = GalacticPurpose.id("orbital_transit");
		galacticpurpose$tryInitDimension(
			transitId, transitId,
			dimTypes, biomes, noiseSettings, overworld, overworldData, overworldBorder,
			listener, debugWorld, obfuscatedSeed
		);
	}

	private void galacticpurpose$tryInitDimension(
		net.minecraft.resources.ResourceLocation dimId,
		net.minecraft.resources.ResourceLocation biomeId,
		Registry<DimensionType> dimTypes,
		Registry<Biome> biomes,
		Registry<NoiseGeneratorSettings> noiseSettings,
		ServerLevel overworld,
		ServerLevelData overworldData,
		WorldBorder overworldBorder,
		ChunkProgressListener listener,
		boolean debugWorld,
		long obfuscatedSeed
	) {
		ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimId);
		if (this.levels.containsKey(levelKey)) {
			return;
		}

		Holder<DimensionType> dimType = dimTypes.getHolder(ResourceKey.create(Registries.DIMENSION_TYPE, dimId)).orElse(null);
		Holder<Biome> biome = biomes.getHolder(ResourceKey.create(Registries.BIOME, biomeId)).orElse(null);
		Holder<NoiseGeneratorSettings> noise = noiseSettings.getHolder(ResourceKey.create(Registries.NOISE_SETTINGS, dimId)).orElse(null);
		if (dimType == null || biome == null || noise == null) {
			GalacticPurpose.LOGGER.warn("Skipping dimension {} initialization: missing datapack registry entries", dimId);
			return;
		}

		LevelStem stem = new LevelStem(dimType, new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), noise));
		DerivedLevelData derivedData = new DerivedLevelData(this.worldData, overworldData);
		ServerLevel customLevel = new ServerLevel(
			(MinecraftServer) (Object) this,
			this.executor,
			this.storageSource,
			derivedData,
			levelKey,
			stem,
			listener,
			debugWorld,
			obfuscatedSeed,
			ImmutableList.of(),
			false,
			overworld.getRandomSequences()
		);
		overworldBorder.addListener(new BorderChangeListener.DelegateBorderChangeListener(customLevel.getWorldBorder()));
		this.levels.put(levelKey, customLevel);
	}
}

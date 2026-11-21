package com.galacticpurpose.exoplanet;

import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.resources.ResourceLocation;

/**
 * Pure astrophysics simulation for exoplanet base gravity and dynamic Erebus black-hole orbital pull.
 */
public final class ExoplanetGravityCalculator {
	public static final long TICKS_PER_ORBITAL_DAY = 24000L;

	public record GravityReading(
		float baseG,
		float effectiveG,
		float blackHoleElevation,
		float orbitalAngleRadians
	) {
		public static final GravityReading STANDARD = new GravityReading(1.0f, 1.0f, 0.0f, 0.0f);
	}

	private ExoplanetGravityCalculator() {
	}

	public static GravityReading evaluate(ResourceLocation dimensionId, long dayTime) {
		ExoplanetRoute route = ExoplanetRoute.fromDimensionId(dimensionId);
		return route == null ? GravityReading.STANDARD : evaluate(route, dayTime);
	}

	public static GravityReading evaluate(ExoplanetRoute route, long dayTime) {
		if (route == null) {
			return GravityReading.STANDARD;
		}
		float baseG = baseGravityFor(route);
		float tidalAmp = tidalAmplitudeFor(route);

		double phase = (Math.floorMod(dayTime, TICKS_PER_ORBITAL_DAY) / (double) TICKS_PER_ORBITAL_DAY) * Math.PI * 2.0;
		float elevation = (float) Math.sin(phase); // +1 = overhead (zenith), -1 = opposite side (nadir)

		// Overhead black hole pulls upward (lighter felt gravity); opposite side reinforces downward pull (heavier).
		float rawEffective = baseG - (elevation * tidalAmp);
		float effectiveG = Math.max(0.20f, Math.min(2.20f, rawEffective));
		return new GravityReading(baseG, effectiveG, elevation, (float) phase);
	}

	public static float baseGravityFor(ExoplanetRoute route) {
		return switch (route) {
			case HEPHAESTUS_IV -> 1.25f;
			case PELAGIA_9 -> 1.30f;
			case NYX_0 -> 0.45f;
			case SANCTUARY -> 0.94f;
		};
	}

	public static float tidalAmplitudeFor(ExoplanetRoute route) {
		return switch (route) {
			case HEPHAESTUS_IV -> 0.22f;
			case PELAGIA_9 -> 0.28f;
			case NYX_0 -> 0.08f;
			case SANCTUARY -> 0.04f;
		};
	}
}

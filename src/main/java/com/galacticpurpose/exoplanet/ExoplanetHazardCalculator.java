package com.galacticpurpose.exoplanet;

import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.resources.ResourceLocation;

/**
 * Pure simulation rules for situational exoplanet environmental hazards and armor protections.
 */
public final class ExoplanetHazardCalculator {
	public static final int PELAGIA_CRUSH_CEILING_Y = 75;

	private ExoplanetHazardCalculator() {
	}

	public static ExoplanetHazardSnapshot evaluate(
		ResourceLocation dimensionId,
		int blockY,
		boolean exposedToSky,
		boolean underwater,
		boolean hasThermalSuit,
		boolean hasPressureSuit
	) {
		ExoplanetRoute route = ExoplanetRoute.fromDimensionId(dimensionId);
		if (route == null) {
			return null;
		}

		return switch (route) {
			case HEPHAESTUS_IV -> evaluateHephaestus(exposedToSky, hasThermalSuit);
			case PELAGIA_9 -> evaluatePelagia(blockY, underwater, hasPressureSuit);
			case NYX_0 -> evaluateNyx(exposedToSky, hasThermalSuit);
			case SANCTUARY -> new ExoplanetHazardSnapshot(
				ExoplanetRoute.SANCTUARY, 22, 1, false,
				0.0f, 0.0f, 0.0f,
				false, false,
				"hazard.galacticpurpose.sanctuary_safe"
			);
		};
	}

	private static ExoplanetHazardSnapshot evaluateHephaestus(boolean exposedToSky, boolean hasThermalSuit) {
		int temp = exposedToSky ? 265 : 65;
		if (!exposedToSky) {
			return new ExoplanetHazardSnapshot(
				ExoplanetRoute.HEPHAESTUS_IV, temp, 2, false,
				0.0f, 0.0f, 0.0f,
				hasThermalSuit, false,
				"hazard.galacticpurpose.sheltered"
			);
		}
		if (hasThermalSuit) {
			return new ExoplanetHazardSnapshot(
				ExoplanetRoute.HEPHAESTUS_IV, temp, 2, false,
				0.0f, 0.0f, 0.0f,
				true, false,
				"hazard.galacticpurpose.thermal_shielded"
			);
		}
		return new ExoplanetHazardSnapshot(
			ExoplanetRoute.HEPHAESTUS_IV, temp, 2, true,
			3.0f, 0.0f, 0.0f,
			false, false,
			"hazard.galacticpurpose.heat_exposed"
		);
	}

	private static ExoplanetHazardSnapshot evaluatePelagia(int blockY, boolean underwater, boolean hasPressureSuit) {
		boolean crushingDepth = underwater && blockY <= PELAGIA_CRUSH_CEILING_Y;
		int atm = crushingDepth ? 1 + (PELAGIA_CRUSH_CEILING_Y + 1 - blockY) / 3 : (underwater ? 2 : 1);
		int temp = underwater ? 3 : 12;

		if (!crushingDepth) {
			return new ExoplanetHazardSnapshot(
				ExoplanetRoute.PELAGIA_9, temp, atm, false,
				0.0f, 0.0f, 0.0f,
				false, hasPressureSuit,
				"hazard.galacticpurpose.safe"
			);
		}
		if (hasPressureSuit) {
			return new ExoplanetHazardSnapshot(
				ExoplanetRoute.PELAGIA_9, temp, atm, false,
				0.0f, 0.0f, 0.0f,
				false, true,
				"hazard.galacticpurpose.pressure_shielded"
			);
		}
		float damage = Math.min(8.0f, 1.5f + (PELAGIA_CRUSH_CEILING_Y + 1 - blockY) * 0.12f);
		return new ExoplanetHazardSnapshot(
			ExoplanetRoute.PELAGIA_9, temp, atm, true,
			0.0f, 0.0f, damage,
			false, false,
			"hazard.galacticpurpose.pressure_crush"
		);
	}

	private static ExoplanetHazardSnapshot evaluateNyx(boolean exposedToSky, boolean hasThermalSuit) {
		int temp = exposedToSky ? -195 : -45;
		if (!exposedToSky) {
			return new ExoplanetHazardSnapshot(
				ExoplanetRoute.NYX_0, temp, 0, false,
				0.0f, 0.0f, 0.0f,
				hasThermalSuit, false,
				"hazard.galacticpurpose.sheltered"
			);
		}
		if (hasThermalSuit) {
			return new ExoplanetHazardSnapshot(
				ExoplanetRoute.NYX_0, temp, 0, false,
				0.0f, 0.0f, 0.0f,
				true, false,
				"hazard.galacticpurpose.thermal_shielded"
			);
		}
		return new ExoplanetHazardSnapshot(
			ExoplanetRoute.NYX_0, temp, 0, true,
			0.0f, 3.5f, 0.0f,
			false, false,
			"hazard.galacticpurpose.cryo_exposed"
		);
	}
}

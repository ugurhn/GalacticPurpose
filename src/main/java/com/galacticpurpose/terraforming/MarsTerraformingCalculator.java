package com.galacticpurpose.terraforming;

/**
 * Pure deterministic calculator converting raw terraforming points into metrics and stage unlocks.
 */
public final class MarsTerraformingCalculator {
	public static final double MAX_POINTS = 10_000.0;

	private MarsTerraformingCalculator() {
	}

	public static double clampPoints(double value) {
		return Math.max(0.0, Math.min(MAX_POINTS, value));
	}

	public static MarsProgressSnapshot snapshot(double rawHeat, double rawPressure, double rawOxygen) {
		double heat = clampPoints(rawHeat);
		double pressure = clampPoints(rawPressure);
		double oxygen = clampPoints(rawOxygen);

		float heatPct = (float) (heat / MAX_POINTS * 100.0);
		float pressurePct = (float) (pressure / MAX_POINTS * 100.0);
		float oxygenPct = (float) (oxygen / MAX_POINTS * 100.0);

		double ti = (heat * 2.0) + (pressure * 2.0) + (oxygen * 3.0);
		float overallPct = (heatPct + pressurePct + oxygenPct) / 3.0f;

		float temperatureCelsius = -65.0f + (heatPct / 100.0f) * 90.0f;
		float pressureKpa = 0.6f + (pressurePct / 100.0f) * 100.7f;

		boolean waterReady = heatPct >= 35.0f && pressurePct >= 30.0f;
		boolean floraReady = heatPct >= 45.0f && pressurePct >= 40.0f && oxygenPct >= 25.0f;
		boolean meteorCalm = pressurePct >= 75.0f;
		boolean breathable = oxygenPct >= 99.95f;

		return new MarsProgressSnapshot(
			heat,
			pressure,
			oxygen,
			heatPct,
			pressurePct,
			oxygenPct,
			ti,
			overallPct,
			temperatureCelsius,
			pressureKpa,
			waterReady,
			floraReady,
			meteorCalm,
			breathable
		);
	}
}

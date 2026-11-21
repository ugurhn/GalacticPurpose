package com.galacticpurpose.terraforming;

/**
 * Computed telemetry snapshot of Mars terraforming progress and environmental thresholds.
 */
public record MarsProgressSnapshot(
	double heatPoints,
	double pressurePoints,
	double oxygenPoints,
	float heatPercent,
	float pressurePercent,
	float oxygenPercent,
	double terraformingIndex,
	float overallPercent,
	float temperatureCelsius,
	float pressureKpa,
	boolean liquidWaterReady,
	boolean floraReady,
	boolean meteorCalm,
	boolean breathable
) {
	public static MarsProgressSnapshot empty() {
		return MarsTerraformingCalculator.snapshot(0.0, 0.0, 0.0);
	}
}

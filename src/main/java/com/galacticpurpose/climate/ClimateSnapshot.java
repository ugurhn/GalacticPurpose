package com.galacticpurpose.climate;

/**
 * Immutable climate evaluation for a player's current day, time, and exposure state.
 */
public record ClimateSnapshot(
	CollapsePhase phase,
	float severity,
	boolean daytime,
	float ambientCelsius,
	float heatDamage,
	float coldDamage,
	boolean applyIgnition,
	boolean applyHypothermiaSlowness
) {
}

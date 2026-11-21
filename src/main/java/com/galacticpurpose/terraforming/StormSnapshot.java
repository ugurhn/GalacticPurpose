package com.galacticpurpose.terraforming;

/**
 * Immutable evaluation of the current Mars solar storm state and radiation hazard.
 */
public record StormSnapshot(
	SolarStormPhase phase,
	int secondsUntilImpact,
	int secondsRemaining,
	float radiationDamage
) {
	public static StormSnapshot dormant() {
		return new StormSnapshot(SolarStormPhase.DORMANT, 0, 0, 0.0f);
	}
}

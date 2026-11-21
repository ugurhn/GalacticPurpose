package com.galacticpurpose.wormhole;

/**
 * Result of a 1-second simulation step for the Wormhole Stabilizer core.
 */
public record WormholeStepResult(
	WormholeState nextState,
	float nextTempCelsius,
	float nextStabilityPercent,
	float nextChargePercent,
	int nextFuelUnits,
	int nextCoolantUnits,
	long energyConsumed,
	boolean meltdown
) {
}

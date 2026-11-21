package com.galacticpurpose.wormhole;

/**
 * Pure simulation step for Draconic-style Wormhole Stabilizer temperature, containment field, and fuel balance.
 */
public final class WormholeStabilityCalculator {
	public static final float AMBIENT_TEMP = 20.0f;
	public static final float COOLANT_TRIGGER_TEMP = 6_500.0f;
	public static final float DANGER_TEMP = 8_500.0f;
	public static final float POINT_OF_NO_RETURN_TEMP = 9_300.0f;
	public static final float POINT_OF_NO_RETURN_STABILITY = 15.0f;
	public static final float MELTDOWN_TEMP = 10_000.0f;
	public static final long ENERGY_PER_SECOND = 3_000L;
	public static final int FUEL_PER_SECOND_CHARGING = 4;
	public static final int FUEL_PER_SECOND_OPEN = 2;

	private WormholeStabilityCalculator() {
	}

	/**
	 * Once containment temperature exceeds 9,300°C, stability drops to <=15%, or structural pylons fail
	 * above 5,000°C, manual Quench / Kill Power can no longer stop runaway singularity collapse.
	 */
	public static boolean isPastPointOfNoReturn(
		WormholeState state,
		boolean structureValid,
		float tempCelsius,
		float stabilityPercent
	) {
		if (state == WormholeState.IDLE || state == WormholeState.SHUTTING_DOWN) {
			return false;
		}
		if (!structureValid && tempCelsius >= 5_000.0f) {
			return true;
		}
		return tempCelsius >= POINT_OF_NO_RETURN_TEMP
			|| (stabilityPercent <= POINT_OF_NO_RETURN_STABILITY && tempCelsius >= 5_000.0f);
	}

	public static WormholeStepResult step(
		WormholeState state,
		boolean structureValid,
		boolean hasTarget,
		float tempCelsius,
		float stabilityPercent,
		float chargePercent,
		int fuelUnits,
		int coolantUnits,
		long energyStored
	) {
		if (state == WormholeState.IDLE) {
			float cooled = Math.max(AMBIENT_TEMP, tempCelsius - 250.0f);
			float restored = Math.min(100.0f, stabilityPercent + 5.0f);
			return new WormholeStepResult(WormholeState.IDLE, cooled, restored, 0.0f, fuelUnits, coolantUnits, 0L, false);
		}

		if (state == WormholeState.SHUTTING_DOWN) {
			int usedCoolant = coolantUnits > 0 ? Math.min(coolantUnits, 5) : 0;
			float drop = usedCoolant > 0 ? 600.0f : 400.0f;
			float nextTemp = Math.max(AMBIENT_TEMP, tempCelsius - drop);
			long usedEnergy = Math.min(energyStored, 1_000L);
			float nextStability = Math.min(100.0f, stabilityPercent + (usedEnergy > 0 ? 5.0f : 1.0f));
			float nextCharge = Math.max(0.0f, chargePercent - 5.0f);
			WormholeState nextState = nextTemp <= 60.0f ? WormholeState.IDLE : WormholeState.SHUTTING_DOWN;
			return new WormholeStepResult(
				nextState,
				nextTemp,
				nextStability,
				nextCharge,
				fuelUnits,
				coolantUnits - usedCoolant,
				usedEnergy,
				false
			);
		}

		if (!structureValid && tempCelsius < 4_000.0f) {
			return new WormholeStepResult(
				WormholeState.IDLE,
				Math.max(AMBIENT_TEMP, tempCelsius - 250.0f),
				100.0f,
				0.0f,
				fuelUnits,
				coolantUnits,
				0L,
				false
			);
		}

		if ((fuelUnits <= 0 || !hasTarget) && state != WormholeState.UNSTABLE) {
			return new WormholeStepResult(
				WormholeState.SHUTTING_DOWN,
				tempCelsius,
				stabilityPercent,
				chargePercent,
				fuelUnits,
				coolantUnits,
				0L,
				false
			);
		}

		long energyUsed = Math.min(energyStored, ENERGY_PER_SECOND);
		float nextStability = stabilityPercent;
		if (!structureValid) {
			// Rapid destabilization when a stabilizer pylon is broken (~7s from 100% -> meltdown)
			nextStability -= 14.0f;
		} else if (energyUsed >= ENERGY_PER_SECOND) {
			nextStability = Math.min(100.0f, nextStability + 4.0f);
		} else {
			nextStability -= 16.0f;
		}
		nextStability = Math.max(0.0f, Math.min(100.0f, nextStability));

		int fuelCost = state == WormholeState.OPEN ? FUEL_PER_SECOND_OPEN : FUEL_PER_SECOND_CHARGING;
		int consumedFuel = Math.min(fuelUnits, fuelCost);
		int nextFuel = fuelUnits - consumedFuel;

		float heatGain = state == WormholeState.OPEN ? 60.0f : 220.0f;
		if (consumedFuel == 0) {
			heatGain = 0.0f;
		}
		float nextTemp = tempCelsius + heatGain - Math.max(20.0f, tempCelsius * 0.01f);

		int nextCoolant = coolantUnits;
		if (nextTemp >= COOLANT_TRIGGER_TEMP && nextCoolant > 0) {
			int coolantUsed = Math.min(nextCoolant, 15);
			nextCoolant -= coolantUsed;
			nextTemp -= (coolantUsed * 30.0f);
		}
		nextTemp = Math.max(AMBIENT_TEMP, Math.min(MELTDOWN_TEMP, nextTemp));

		boolean meltdown = nextTemp >= MELTDOWN_TEMP || (nextStability <= 0.0f && nextTemp >= 5_000.0f);
		if (meltdown) {
			return new WormholeStepResult(
				WormholeState.UNSTABLE,
				MELTDOWN_TEMP,
				0.0f,
				0.0f,
				nextFuel,
				nextCoolant,
				energyUsed,
				true
			);
		}

		boolean unstable = !structureValid || nextTemp >= DANGER_TEMP || nextStability < 35.0f;
		if (unstable) {
			return new WormholeStepResult(
				WormholeState.UNSTABLE,
				nextTemp,
				nextStability,
				chargePercent,
				nextFuel,
				nextCoolant,
				energyUsed,
				false
			);
		}

		if (state == WormholeState.OPEN) {
			return new WormholeStepResult(
				WormholeState.OPEN,
				nextTemp,
				nextStability,
				100.0f,
				nextFuel,
				nextCoolant,
				energyUsed,
				false
			);
		}

		float chargeDelta = (nextTemp >= 1_500.0f && nextStability >= 60.0f) ? 2.5f : 0.6f;
		float nextCharge = Math.min(100.0f, chargePercent + chargeDelta);
		WormholeState nextState = nextCharge >= 100.0f ? WormholeState.OPEN : WormholeState.CHARGING;

		return new WormholeStepResult(
			nextState,
			nextTemp,
			nextStability,
			nextCharge,
			nextFuel,
			nextCoolant,
			energyUsed,
			false
		);
	}
}

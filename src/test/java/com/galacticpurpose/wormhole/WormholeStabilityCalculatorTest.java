package com.galacticpurpose.wormhole;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WormholeStabilityCalculatorTest {

	@Test
	void chargingRequiresValidStructureFuelAndEnergy() {
		WormholeStepResult missingStructure = WormholeStabilityCalculator.step(
			WormholeState.CHARGING,
			false,
			true,
			20.0f,
			100.0f,
			0.0f,
			500,
			500,
			100_000L
		);
		assertEquals(WormholeState.IDLE, missingStructure.nextState());
		assertFalse(missingStructure.meltdown());

		WormholeStepResult charging = WormholeStabilityCalculator.step(
			WormholeState.CHARGING,
			true,
			true,
			2500.0f,
			90.0f,
			10.0f,
			500,
			500,
			100_000L
		);
		assertEquals(WormholeState.CHARGING, charging.nextState());
		assertTrue(charging.nextChargePercent() > 10.0f);
		assertTrue(charging.energyConsumed() > 0L);
	}

	@Test
	void coolantSuppressesOverheatingAndPreventsMeltdown() {
		WormholeStepResult cooled = WormholeStabilityCalculator.step(
			WormholeState.CHARGING,
			true,
			true,
			7200.0f,
			80.0f,
			60.0f,
			400,
			300,
			100_000L
		);
		assertTrue(cooled.nextCoolantUnits() < 300, "Expected coolant to be consumed above 6500C");
		assertFalse(cooled.meltdown());
	}

	@Test
	void runningHotWithoutCoolantOrEnergyCausesUnstableMeltdown() {
		WormholeStepResult noEnergyHot = WormholeStabilityCalculator.step(
			WormholeState.CHARGING,
			true,
			true,
			9950.0f,
			5.0f,
			50.0f,
			200,
			0,
			0L
		);
		assertTrue(noEnergyHot.meltdown() || noEnergyHot.nextState() == WormholeState.UNSTABLE);

		WormholeStepResult extremeHeat = WormholeStabilityCalculator.step(
			WormholeState.UNSTABLE,
			true,
			true,
			10_000.0f,
			0.0f,
			50.0f,
			200,
			0,
			0L
		);
		assertTrue(extremeHeat.meltdown());
	}

	@Test
	void reachingFullChargeOpensWormholeWithValidTarget() {
		WormholeStepResult opened = WormholeStabilityCalculator.step(
			WormholeState.CHARGING,
			true,
			true,
			4500.0f,
			90.0f,
			99.8f,
			200,
			200,
			100_000L
		);
		assertEquals(WormholeState.OPEN, opened.nextState());
		assertEquals(100.0f, opened.nextChargePercent(), 0.001f);
	}

	@Test
	void shuttingDownCoolsCoreSafelyTowardIdle() {
		WormholeStepResult cooling = WormholeStabilityCalculator.step(
			WormholeState.SHUTTING_DOWN,
			true,
			true,
			3000.0f,
			70.0f,
			40.0f,
			100,
			100,
			10_000L
		);
		assertTrue(cooling.nextTempCelsius() < 3000.0f);
		assertFalse(cooling.meltdown());

		WormholeStepResult idle = WormholeStabilityCalculator.step(
			WormholeState.SHUTTING_DOWN,
			true,
			true,
			25.0f,
			80.0f,
			0.0f,
			100,
			100,
			10_000L
		);
		assertEquals(WormholeState.IDLE, idle.nextState());
	}

	@Test
	void pointOfNoReturnLocksOutQuenchOnceCriticalThresholdPassed() {
		assertFalse(WormholeStabilityCalculator.isPastPointOfNoReturn(WormholeState.UNSTABLE, true, 8_650.0f, 46.0f));
		assertTrue(WormholeStabilityCalculator.isPastPointOfNoReturn(WormholeState.UNSTABLE, true, 9_350.0f, 40.0f));
		assertTrue(WormholeStabilityCalculator.isPastPointOfNoReturn(WormholeState.UNSTABLE, true, 7_000.0f, 12.0f));
		assertTrue(WormholeStabilityCalculator.isPastPointOfNoReturn(WormholeState.OPEN, false, 6_000.0f, 90.0f));
	}
}

package com.galacticpurpose.terraforming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarsTerraformingCalculatorTest {

	@Test
	void initialStateHasZeroProgressAndBaselineMarsConditions() {
		MarsProgressSnapshot snapshot = MarsTerraformingCalculator.snapshot(0.0, 0.0, 0.0);

		assertEquals(0.0f, snapshot.heatPercent(), 0.001f);
		assertEquals(0.0f, snapshot.pressurePercent(), 0.001f);
		assertEquals(0.0f, snapshot.oxygenPercent(), 0.001f);
		assertEquals(0.0, snapshot.terraformingIndex(), 0.001);
		assertEquals(-65.0f, snapshot.temperatureCelsius(), 0.01f);
		assertEquals(0.6f, snapshot.pressureKpa(), 0.01f);
		assertFalse(snapshot.liquidWaterReady());
		assertFalse(snapshot.floraReady());
		assertFalse(snapshot.breathable());
	}

	@Test
	void thresholdsUnlockWaterFloraMeteorCalmAndBreathabilityAtExpectedLevels() {
		MarsProgressSnapshot waterPhase = MarsTerraformingCalculator.snapshot(3500.0, 3000.0, 1000.0);
		assertTrue(waterPhase.liquidWaterReady());
		assertFalse(waterPhase.floraReady());
		assertFalse(waterPhase.breathable());

		MarsProgressSnapshot floraPhase = MarsTerraformingCalculator.snapshot(5000.0, 4500.0, 3000.0);
		assertTrue(floraPhase.liquidWaterReady());
		assertTrue(floraPhase.floraReady());
		assertFalse(floraPhase.breathable());

		MarsProgressSnapshot fullOxygen = MarsTerraformingCalculator.snapshot(8000.0, 8000.0, 10000.0);
		assertTrue(fullOxygen.meteorCalm());
		assertTrue(fullOxygen.breathable());
		assertEquals(100.0f, fullOxygen.oxygenPercent(), 0.001f);

		MarsProgressSnapshot edgeFull = MarsTerraformingCalculator.snapshot(10000.0, 10000.0, 9995.0);
		assertTrue(edgeFull.breathable(), "99.95%+ O2 rounding to 100.0% must remain breathable without flicker");
	}

	@Test
	void floraOxygenatorRequiresMinimumHeatAndPressureBeforeRunning() {
		MarsProgressSnapshot coldMars = MarsTerraformingCalculator.snapshot(500.0, 200.0, 0.0);
		MarsProgressSnapshot warmedMars = MarsTerraformingCalculator.snapshot(1500.0, 1000.0, 0.0);

		assertFalse(TerraformingMachineType.FLORA_OXYGENATOR.canOperate(coldMars));
		assertTrue(TerraformingMachineType.FLORA_OXYGENATOR.canOperate(warmedMars));
		assertTrue(TerraformingMachineType.THERMAL_HEATER.canOperate(coldMars));
		assertTrue(TerraformingMachineType.PRESSURE_DRILL.canOperate(coldMars));
	}

	@Test
	void pointsClampSafelyBetweenZeroAndMaximum() {
		assertEquals(0.0, MarsTerraformingCalculator.clampPoints(-250.0), 0.001);
		assertEquals(10000.0, MarsTerraformingCalculator.clampPoints(15000.0), 0.001);
	}
}

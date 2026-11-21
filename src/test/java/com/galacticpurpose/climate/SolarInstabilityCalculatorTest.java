package com.galacticpurpose.climate;

import com.galacticpurpose.config.ModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolarInstabilityCalculatorTest {

	@Test
	void gracePeriodRemainsCompletelyStableAndSafe() {
		ModConfig config = ModConfig.defaults();

		ClimateSnapshot noonSnapshot = SolarInstabilityCalculator.evaluate(
			19, 6000L, true, false, false, config
		);
		ClimateSnapshot midnightSnapshot = SolarInstabilityCalculator.evaluate(
			19, 18000L, true, false, false, config
		);

		assertEquals(CollapsePhase.STABLE, noonSnapshot.phase());
		assertEquals(0.0f, noonSnapshot.severity(), 0.0001f);
		assertEquals(0.0f, noonSnapshot.heatDamage(), 0.0001f);
		assertEquals(0.0f, midnightSnapshot.coldDamage(), 0.0001f);
		assertFalse(noonSnapshot.applyIgnition());
		assertFalse(midnightSnapshot.applyHypothermiaSlowness());
	}

	@Test
	void warningPhaseRaisesDayHeatAndDropsNightColdWithoutLethalDamage() {
		ModConfig config = ModConfig.defaults();

		ClimateSnapshot warningNoon = SolarInstabilityCalculator.evaluate(
			25, 6000L, true, false, false, config
		);
		ClimateSnapshot warningMidnight = SolarInstabilityCalculator.evaluate(
			25, 18000L, true, false, false, config
		);

		assertEquals(CollapsePhase.WARNING, warningNoon.phase());
		assertTrue(warningNoon.severity() > 0.0f && warningNoon.severity() < 1.0f);
		assertTrue(warningNoon.ambientCelsius() > 30.0f);
		assertTrue(warningMidnight.ambientCelsius() < 5.0f);
		assertEquals(0.0f, warningNoon.heatDamage(), 0.0001f);
		assertEquals(0.0f, warningMidnight.coldDamage(), 0.0001f);
	}

	@Test
	void criticalPhaseDealsDayHeatAndNightHypothermiaWhenExposed() {
		ModConfig config = ModConfig.defaults();

		ClimateSnapshot criticalNoon = SolarInstabilityCalculator.evaluate(
			40, 6000L, true, false, false, config
		);
		ClimateSnapshot criticalMidnight = SolarInstabilityCalculator.evaluate(
			40, 18000L, true, false, false, config
		);

		assertEquals(CollapsePhase.CRITICAL, criticalNoon.phase());
		assertTrue(criticalNoon.heatDamage() > 0.0f);
		assertEquals(0.0f, criticalNoon.coldDamage(), 0.0001f);

		assertTrue(criticalMidnight.coldDamage() > 0.0f);
		assertTrue(criticalMidnight.applyHypothermiaSlowness());
	}

	@Test
	void shelterProtectsPlayerEvenInTerminalPhase() {
		ModConfig config = ModConfig.defaults();

		ClimateSnapshot shelteredNoon = SolarInstabilityCalculator.evaluate(
			60, 6000L, false, false, false, config
		);
		ClimateSnapshot shelteredMidnight = SolarInstabilityCalculator.evaluate(
			60, 18000L, false, false, false, config
		);

		assertEquals(CollapsePhase.TERMINAL, shelteredNoon.phase());
		assertEquals(0.0f, shelteredNoon.heatDamage(), 0.0001f);
		assertEquals(0.0f, shelteredMidnight.coldDamage(), 0.0001f);
		assertFalse(shelteredNoon.applyIgnition());
		assertFalse(shelteredMidnight.applyHypothermiaSlowness());
	}

	@Test
	void thermalShieldedSuitNegatesAllSurfaceClimateDamageInTerminalPhase() {
		ModConfig config = ModConfig.defaults();

		ClimateSnapshot shieldedNoon = SolarInstabilityCalculator.evaluate(
			65, 6000L, true, false, true, config
		);
		ClimateSnapshot shieldedMidnight = SolarInstabilityCalculator.evaluate(
			65, 18000L, true, false, true, config
		);

		assertEquals(CollapsePhase.TERMINAL, shieldedNoon.phase());
		assertEquals(0.0f, shieldedNoon.heatDamage(), 0.0001f);
		assertEquals(0.0f, shieldedMidnight.coldDamage(), 0.0001f);
		assertFalse(shieldedNoon.applyIgnition());
		assertFalse(shieldedMidnight.applyHypothermiaSlowness());
	}

	@Test
	void terminalDaytimeIgnitesAndWaterOnlyMitigatesCriticalHeat() {
		ModConfig config = ModConfig.defaults();

		ClimateSnapshot criticalDry = SolarInstabilityCalculator.evaluate(
			40, 6000L, true, false, false, config
		);
		ClimateSnapshot criticalWet = SolarInstabilityCalculator.evaluate(
			40, 6000L, true, true, false, config
		);
		ClimateSnapshot terminalDry = SolarInstabilityCalculator.evaluate(
			55, 6000L, true, false, false, config
		);

		assertTrue(criticalWet.heatDamage() < criticalDry.heatDamage());
		assertTrue(terminalDry.applyIgnition());
		assertTrue(terminalDry.heatDamage() > criticalDry.heatDamage());
	}
}

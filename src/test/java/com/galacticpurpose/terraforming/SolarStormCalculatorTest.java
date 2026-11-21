package com.galacticpurpose.terraforming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolarStormCalculatorTest {

	@Test
	void calmBeforeRedGiantActivatesHasZeroStormAndZeroDecay() {
		StormSnapshot calm = SolarStormCalculator.evaluate(false, 0L, true, false, false);

		assertEquals(SolarStormPhase.DORMANT, calm.phase());
		assertEquals(0.0f, calm.radiationDamage(), 0.001f);
		assertEquals(0, calm.secondsUntilImpact());
	}

	@Test
	void redGiantStormWarnsBeforeImpactAndDamagesOnlyExposedUnshieldedPlayers() {
		// Cycle = 300s total: 0..194s COUNTDOWN, 195..254s WARNING (60s), 255..299s ACTIVE (45s)
		StormSnapshot countdown = SolarStormCalculator.evaluate(true, 100L * 20L, true, false, false);
		assertEquals(SolarStormPhase.COUNTDOWN, countdown.phase());
		assertEquals(155, countdown.secondsUntilImpact());
		assertEquals(0.0f, countdown.radiationDamage(), 0.001f);

		StormSnapshot warning = SolarStormCalculator.evaluate(true, 220L * 20L, true, false, false);
		assertEquals(SolarStormPhase.WARNING, warning.phase());
		assertEquals(35, warning.secondsUntilImpact());
		assertEquals(0.0f, warning.radiationDamage(), 0.001f);

		StormSnapshot activeExposed = SolarStormCalculator.evaluate(true, 270L * 20L, true, false, false);
		assertEquals(SolarStormPhase.ACTIVE, activeExposed.phase());
		assertTrue(activeExposed.radiationDamage() > 0.0f);

		StormSnapshot activeInLeadShelter = SolarStormCalculator.evaluate(true, 270L * 20L, false, false, false);
		assertEquals(0.0f, activeInLeadShelter.radiationDamage(), 0.001f);

		StormSnapshot activeWithLeadSuit = SolarStormCalculator.evaluate(true, 270L * 20L, true, true, false);
		assertEquals(0.0f, activeWithLeadSuit.radiationDamage(), 0.001f);

		StormSnapshot activeDeepUnderground = SolarStormCalculator.evaluate(true, 270L * 20L, true, false, true);
		assertEquals(0.0f, activeDeepUnderground.radiationDamage(), 0.001f);
	}
}

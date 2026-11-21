package com.galacticpurpose.ship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarshipLandingCalculatorTest {
	@Test
	void propulsiveRetroBurnSmoothlyDeceleratesToZeroAtTouchdown() {
		double high = StarshipLandingCalculator.computeDescentVelocity(100.0);
		double mid = StarshipLandingCalculator.computeDescentVelocity(32.0);
		double low = StarshipLandingCalculator.computeDescentVelocity(8.0);
		double landed = StarshipLandingCalculator.computeDescentVelocity(0.05);

		assertTrue(high < mid && mid < low && low < 0.0, "Velocity must smoothly brake downward toward zero");
		assertEquals(0.0, landed, 0.0001);
		assertFalse(StarshipLandingCalculator.shouldDeployLandingLegs(32.0));
		assertTrue(StarshipLandingCalculator.shouldDeployLandingLegs(15.0));
	}

	@Test
	void flatnessScorerPrefersFlatClearingsAndRejectsLava() {
		int flat = StarshipLandingCalculator.scoreTerrainFlatness(70, new int[]{70, 70, 70, 71, 70, 70, 70, 70}, false);
		int cliff = StarshipLandingCalculator.scoreTerrainFlatness(70, new int[]{70, 76, 64, 75, 68, 70, 72, 65}, false);
		int lava = StarshipLandingCalculator.scoreTerrainFlatness(70, new int[]{70, 70, 70, 70, 70, 70, 70, 70}, true);

		assertTrue(flat < cliff, "Flat clearing must outscore steep cliff");
		assertEquals(Integer.MAX_VALUE, lava, "Lava sites must be strictly rejected");
	}
}

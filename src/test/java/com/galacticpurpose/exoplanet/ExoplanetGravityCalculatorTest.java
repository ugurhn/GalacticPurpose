package com.galacticpurpose.exoplanet;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExoplanetGravityCalculatorTest {

	@Test
	void overheadBlackHoleReducesGravityAndOppositeBlackHoleIncreasesGravity() {
		// Time 6000 = zenith (sin = +1.0 -> black hole directly overhead pulling upward)
		ExoplanetGravityCalculator.GravityReading zenith =
			ExoplanetGravityCalculator.evaluate(ExoplanetRoute.PELAGIA_9, 6000L);
		// Time 18000 = nadir (sin = -1.0 -> black hole directly beneath planet reinforcing pull)
		ExoplanetGravityCalculator.GravityReading nadir =
			ExoplanetGravityCalculator.evaluate(ExoplanetRoute.PELAGIA_9, 18000L);

		assertTrue(zenith.blackHoleElevation() > 0.95f, "Black hole should be overhead at dayTime 6000");
		assertTrue(nadir.blackHoleElevation() < -0.95f, "Black hole should be opposite at dayTime 18000");
		assertTrue(zenith.effectiveG() < zenith.baseG(), "Overhead black hole must reduce felt gravity");
		assertTrue(nadir.effectiveG() > nadir.baseG(), "Opposite black hole must increase felt gravity");
		assertEquals(1.30f, zenith.baseG(), 0.001f);
	}

	@Test
	void nyxIsLowGravityAndSanctuaryMatchesChronicleSpecs() {
		ExoplanetGravityCalculator.GravityReading nyxHorizon =
			ExoplanetGravityCalculator.evaluate(ExoplanetRoute.NYX_0, 0L);
		ExoplanetGravityCalculator.GravityReading sanctuaryHorizon =
			ExoplanetGravityCalculator.evaluate(ExoplanetRoute.SANCTUARY, 0L);

		assertEquals(0.45f, nyxHorizon.effectiveG(), 0.01f);
		assertEquals(0.94f, sanctuaryHorizon.effectiveG(), 0.01f);
	}

	@Test
	void nonExoplanetReturnsStandardOneG() {
		ExoplanetGravityCalculator.GravityReading earth =
			ExoplanetGravityCalculator.evaluate(GalacticPurpose.id("overworld"), 6000L);
		assertEquals(1.0f, earth.effectiveG(), 0.001f);
	}
}

package com.galacticpurpose.exoplanet;

import com.galacticpurpose.GalacticPurpose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExoplanetHazardCalculatorTest {

	@Test
	void returnsNullForNonExoplanetDimension() {
		assertNull(ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("not_a_planet"), 64, true, false, false, false
		));
	}

	@Test
	void hephaestusBurnsExposedPlayerUnlessWearingThermalSuitOrSheltered() {
		ExoplanetHazardSnapshot exposed = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("hephaestus_iv"), 80, true, false, false, true
		);
		assertNotNull(exposed);
		assertTrue(exposed.hazardous());
		assertTrue(exposed.heatDamage() > 0.0f);
		assertEquals(0.0f, exposed.pressureDamage());

		ExoplanetHazardSnapshot thermal = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("hephaestus_iv"), 80, true, false, true, false
		);
		assertNotNull(thermal);
		assertFalse(thermal.hazardous());
		assertEquals(0.0f, thermal.heatDamage());

		ExoplanetHazardSnapshot underground = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("hephaestus_iv"), 40, false, false, false, false
		);
		assertNotNull(underground);
		assertFalse(underground.hazardous());
		assertEquals(0.0f, underground.heatDamage());
	}

	@Test
	void pelagiaCrushesDeepDiversUnlessWearingPressureSuit() {
		ExoplanetHazardSnapshot surface = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("pelagia_9"), 110, true, true, false, false
		);
		assertNotNull(surface);
		assertFalse(surface.hazardous());
		assertEquals(0.0f, surface.pressureDamage());

		ExoplanetHazardSnapshot deepCrushed = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("pelagia_9"), 40, false, true, true, false
		);
		assertNotNull(deepCrushed);
		assertTrue(deepCrushed.hazardous());
		assertTrue(deepCrushed.pressureDamage() > 0.0f);
		assertTrue(deepCrushed.pressureAtm() > 5);

		ExoplanetHazardSnapshot suited = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("pelagia_9"), 40, false, true, false, true
		);
		assertNotNull(suited);
		assertFalse(suited.hazardous());
		assertEquals(0.0f, suited.pressureDamage());
		assertTrue(suited.pressureProtected());
	}

	@Test
	void nyxFreezesExposedPlayerAndSanctuaryIsSafe() {
		ExoplanetHazardSnapshot nyxExposed = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("nyx_0"), 75, true, false, false, false
		);
		assertNotNull(nyxExposed);
		assertTrue(nyxExposed.hazardous());
		assertTrue(nyxExposed.coldDamage() > 0.0f);

		ExoplanetHazardSnapshot nyxThermal = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("nyx_0"), 75, true, false, true, false
		);
		assertNotNull(nyxThermal);
		assertFalse(nyxThermal.hazardous());
		assertEquals(0.0f, nyxThermal.coldDamage());

		ExoplanetHazardSnapshot sanctuary = ExoplanetHazardCalculator.evaluate(
			GalacticPurpose.id("sanctuary"), 80, true, false, false, false
		);
		assertNotNull(sanctuary);
		assertFalse(sanctuary.hazardous());
		assertEquals(22, sanctuary.ambientCelsius());
	}
}

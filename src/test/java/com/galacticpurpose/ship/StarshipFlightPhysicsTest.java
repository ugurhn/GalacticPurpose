package com.galacticpurpose.ship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StarshipFlightPhysicsTest {

	@Test
	void countdownSecondsMatchesTicksRemaining() {
		assertEquals(10, StarshipFlightPhysics.countdownSecondsRemaining(0));
		assertEquals(10, StarshipFlightPhysics.countdownSecondsRemaining(1));
		assertEquals(5, StarshipFlightPhysics.countdownSecondsRemaining(100));
		assertEquals(1, StarshipFlightPhysics.countdownSecondsRemaining(181));
		assertEquals(0, StarshipFlightPhysics.countdownSecondsRemaining(200));
	}

	@Test
	void runwayLaunchAcceleratesSmoothly() {
		double v0 = StarshipFlightPhysics.computeRunwayLaunchSpeed(0);
		double v20 = StarshipFlightPhysics.computeRunwayLaunchSpeed(20);
		double v60 = StarshipFlightPhysics.computeRunwayLaunchSpeed(60);
		assertTrue(v0 > 0.14);
		assertTrue(v20 > v0);
		assertTrue(v60 > v20);
	}

	@Test
	void ascentBuilds15MsBeforeSteepPitchUpAndLimitsHorizontalDrift() {
		StarshipFlightPhysics.AscentReading s0 = StarshipFlightPhysics.computeAscentProfile(0);
		StarshipFlightPhysics.AscentReading sPre15 = StarshipFlightPhysics.computeAscentProfile(58);  // ~15+ m/s threshold
		StarshipFlightPhysics.AscentReading sPitch = StarshipFlightPhysics.computeAscentProfile(162); // steep vertical capture
		StarshipFlightPhysics.AscentReading sMid = StarshipFlightPhysics.computeAscentProfile(450);
		StarshipFlightPhysics.AscentReading sEnd = StarshipFlightPhysics.computeAscentProfile(900);

		assertEquals(0.0f, s0.pitchDeg(), 0.01f);
		assertTrue(sPre15.forwardSpeed() * 20.0 >= 14.5, "Ship must accelerate near/past 15 m/s before rotating vertical");
		assertTrue(sPre15.pitchDeg() <= 8.0f, "Pitch must remain shallow (<= 8 deg) before 15 m/s lift-off");
		assertTrue(sPitch.pitchDeg() >= 86.0f, "Ship must pitch steep vertical (>= 86 deg) right after 15 m/s lift");
		assertTrue(sMid.pitchDeg() >= 88.5f && sEnd.pitchDeg() >= 89.0f);
		assertTrue(sEnd.forwardSpeed() >= 11.0, "High orbital boost must reach >= 11.0 b/t");
		assertEquals(120.0f, sEnd.simulatedAltitudeKm(), 0.2f);
		assertEquals(1.0f, sEnd.skyDarkness(), 0.01f);

		// Integrate full 900 ticks -> horizontal drift stays < 105 blocks so server chunk-gen never stalls
		double horiz = 0.0;
		double vert = 0.0;
		for (int t = 1; t <= StarshipFlightPhysics.ASCENT_DURATION_TICKS; t++) {
			StarshipFlightPhysics.AscentReading r = StarshipFlightPhysics.computeAscentProfile(t);
			double rad = Math.toRadians(r.pitchDeg());
			horiz += Math.cos(rad) * r.forwardSpeed();
			vert += Math.sin(rad) * r.forwardSpeed();
		}
		assertTrue(horiz < 105.0, "Total horizontal displacement must stay < 105 blocks, was " + horiz);
		assertTrue(vert > 2400.0, "Total vertical altitude gain must exceed 2400 blocks, was " + vert);
	}

	@Test
	void dynamicLandingSimulatesVectorBrakingRockingAndSettlesOverPad() {
		double dist = 78.0;
		double fwd = 1.15;
		double vert = -0.52;
		double alt = 46.0;
		float pitch = -18.0f;
		float vec = 0.0f;

		float maxGimbal = 0.0f;
		float maxFlare = -90.0f;
		boolean sawNoseReboundOrFlare = false;
		for (int t = 0; t < 240; t++) {
			StarshipFlightPhysics.DynamicLandingStep step =
					StarshipFlightPhysics.stepDynamicLanding(dist, fwd, vert, alt, pitch, vec);
			fwd = step.nextForwardSpeed();
			vert = step.nextVerticalSpeed();
			pitch = step.pitchDeg();
			vec = step.vectorAngleDeg();
			dist -= fwd;
			alt = Math.max(0.0, alt + vert);
			maxGimbal = Math.max(maxGimbal, vec);
			maxFlare = Math.max(maxFlare, pitch);
			if (dist < 18.0 && step.landingPhase() == 1 && vec > 94.0f) {
				sawNoseReboundOrFlare = true;
			}
			if (alt <= 0.16 && Math.abs(dist) <= 3.5 && Math.abs(fwd) <= 0.14) {
				break;
			}
		}

		assertTrue(maxGimbal > 96.0f, "Vectoring engines must pivot forward past 90 deg during horizontal brake, peak=" + maxGimbal);
		assertTrue(maxFlare > 10.0f, "Nose must flare above +10 deg while bleeding horizontal speed, peak=" + maxFlare);
		assertTrue(sawNoseReboundOrFlare);
		assertTrue(Math.abs(dist) < 4.5, "Craft must damp horizontal speed right onto target LZ, residual dist=" + dist);
		assertTrue(alt <= 1.0, "Craft must descend onto ground level smoothly, remaining alt=" + alt);
	}

	@Test
	void symmetric12000KmDistancesAndDistinctPlanetVsWormholeOpticsMatch() {
		assertEquals(1800, StarshipFlightPhysics.CRUISE_DURATION_TICKS);
		assertEquals(7_000.0, StarshipFlightPhysics.PLANET_RADIUS_KM, 0.01);
		assertEquals(0.030, StarshipFlightPhysics.WORMHOLE_RADIUS_KM, 0.001);

		// Symmetric 12,000 km corridor both ways!
		assertEquals(12_000, StarshipFlightPhysics.computePlanetDistanceKm(0.0f));
		assertEquals(120, StarshipFlightPhysics.computePlanetDistanceKm(1.0f));
		assertEquals(12_000.0f, StarshipFlightPhysics.computeWormholeDistanceKm(0.0f), 1.0f);
		assertEquals(0.05f, StarshipFlightPhysics.computeWormholeDistanceKm(1.0f), 0.02f);
		assertEquals(0.05f, StarshipFlightPhysics.computeOutboundWormholeDistanceKm(0.0f), 0.01f);
		assertEquals(12_000.0f, StarshipFlightPhysics.computeOutboundWormholeDistanceKm(1.0f), 1.0f);

		float farPlanetAng = StarshipFlightPhysics.computeAngularRadiusDeg(
				StarshipFlightPhysics.PLANET_RADIUS_KM, StarshipFlightPhysics.PLANET_RADIUS_KM + 12_000.0);
		float closePlanetAng = StarshipFlightPhysics.computeAngularRadiusDeg(
				StarshipFlightPhysics.PLANET_RADIUS_KM, StarshipFlightPhysics.PLANET_RADIUS_KM + 120.0);
		float wormholeMidCruiseAng = StarshipFlightPhysics.computeAngularRadiusDeg(
				StarshipFlightPhysics.WORMHOLE_RADIUS_KM, 3_000.0);
		assertTrue(farPlanetAng > 18.0f && closePlanetAng > 75.0f);
		assertTrue(wormholeMidCruiseAng < 0.05f, "At 3,000 km, a 60 m wormhole is vastly smaller angularly than a 7,000 km planet");

		// Planet keeps growing substantially past 1,000 km -> 120 km AND into entry plunge!
		float s0 = StarshipFlightPhysics.computePlanetVisualScale(0.0f, false, 0.0f);
		float s1000 = StarshipFlightPhysics.computePlanetVisualScale(0.85f, false, 0.0f);
		float s120 = StarshipFlightPhysics.computePlanetVisualScale(1.0f, false, 0.0f);
		float sPlunge = StarshipFlightPhysics.computePlanetVisualScale(1.0f, false, 1.0f);
		assertTrue(s1000 > s0 + 40.0f);
		assertTrue(s120 > s1000 + 20.0f, "Planet must continue growing visibly from 1000 km down to 120 km");
		assertTrue(sPlunge >= s120 + 10.0f && sPlunge < 120.0f, "Planet swells smoothly during [G] entry without hollow sphere clipping");

		// Return Wormhole starts pinpoint (< 0.25), grows cleanly across mid-cruise & 200 km, and swells cleanly at entry!
		float w0 = StarshipFlightPhysics.computeWormholeVisualScale(0.0f, true, 0.0f);
		float wMid = StarshipFlightPhysics.computeWormholeVisualScale(0.50f, true, 0.0f);
		float w206 = StarshipFlightPhysics.computeWormholeVisualScale(0.89f, true, 0.0f);
		float w100 = StarshipFlightPhysics.computeWormholeVisualScale(1.0f, true, 0.0f);
		float wIn = StarshipFlightPhysics.computeWormholeVisualScale(1.0f, true, 1.0f);
		assertTrue(w0 < 0.25f && wMid > w0 * 3.0f);
		assertTrue(w206 > 5.0f && w100 > 22.0f && wIn > 35.0f && wIn < 45.0f);
	}

	@Test
	void cubeOutwardWindingMatchesNormalsOnEveryFace() {
		float[][] faces = {
				{0, 1, 0,  0, 1, 1,  1, 1, 1,  0, 1, 0},
				{0, 0, 1,  0, 0, 0,  1, 0, 0,  0, -1, 0},
				{0, 0, 1,  1, 0, 1,  1, 1, 1,  0, 0, 1},
				{1, 0, 0,  0, 0, 0,  0, 1, 0,  0, 0, -1},
				{1, 0, 1,  1, 0, 0,  1, 1, 0,  1, 0, 0},
				{0, 0, 0,  0, 0, 1,  0, 1, 1,  -1, 0, 0}
		};
		for (int i = 0; i < faces.length; i++) {
			float[] f = faces[i];
			float dot = StarshipFlightPhysics.outwardWindingDot(
					f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11]);
			assertTrue(dot > 0.0f, "Face " + i + " should have positive outward normal winding, got " + dot);
		}
	}

	@Test
	void compassAndCruisePhasesMapAccurately() {
		assertEquals("N", StarshipFlightPhysics.computeCompassBearing(0.0, -30.0));
		assertEquals("E", StarshipFlightPhysics.computeCompassBearing(30.0, 0.0));
		assertEquals("S", StarshipFlightPhysics.computeCompassBearing(0.0, 30.0));
		assertEquals("W", StarshipFlightPhysics.computeCompassBearing(-30.0, 0.0));
		assertEquals(0, StarshipFlightPhysics.computeCruisePhase(0.10f));
		assertEquals(1, StarshipFlightPhysics.computeCruisePhase(0.45f));
		assertEquals(2, StarshipFlightPhysics.computeCruisePhase(0.80f));
		assertEquals(3, StarshipFlightPhysics.computeCruisePhase(1.00f));
	}
}

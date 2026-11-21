package com.galacticpurpose.wormhole;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WormholeLinkCalculatorTest {
	@Test
	void openOriginLinkAndSufficientFuelAllowsOrbitalLaunch() {
		assertEquals(
			WormholeLinkCalculator.LaunchDecision.READY,
			WormholeLinkCalculator.evaluateOrbitalLaunch(true, WormholeLinkCalculator.LinkStatus.OPEN, 1000)
		);
	}

	@Test
	void destroyedOrClosedOriginWormholeAbortsExoplanetLaunch() {
		assertEquals(
			WormholeLinkCalculator.LaunchDecision.ABORTED_WORMHOLE_OFFLINE,
			WormholeLinkCalculator.evaluateOrbitalLaunch(true, WormholeLinkCalculator.LinkStatus.DESTROYED, 1000)
		);
		assertEquals(
			WormholeLinkCalculator.LaunchDecision.ABORTED_WORMHOLE_OFFLINE,
			WormholeLinkCalculator.evaluateOrbitalLaunch(true, WormholeLinkCalculator.LinkStatus.CLOSED, 1000)
		);
	}

	@Test
	void unlinkedShipCannotLaunchFromExoplanet() {
		assertEquals(
			WormholeLinkCalculator.LaunchDecision.ABORTED_NO_LINK,
			WormholeLinkCalculator.evaluateOrbitalLaunch(true, WormholeLinkCalculator.LinkStatus.UNLINKED, 1000)
		);
	}

	@Test
	void insufficientFuelAbortsLaunchEvenWhenWormholeOpen() {
		assertEquals(
			WormholeLinkCalculator.LaunchDecision.ABORTED_NO_FUEL,
			WormholeLinkCalculator.evaluateOrbitalLaunch(true, WormholeLinkCalculator.LinkStatus.OPEN, 200)
		);
	}

	@Test
	void relocationFuelScalesProportionallyWithDistanceAndHasFloor() {
		assertEquals(0, WormholeLinkCalculator.computeRelocationFuelCost(0.0));
		assertEquals(WormholeLinkCalculator.MIN_RELOCATION_FUEL, WormholeLinkCalculator.computeRelocationFuelCost(10.0));
		assertEquals(125, WormholeLinkCalculator.computeRelocationFuelCost(100.0));
		assertEquals(625, WormholeLinkCalculator.computeRelocationFuelCost(500.0));
	}
}

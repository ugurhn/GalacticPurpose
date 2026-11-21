package com.galacticpurpose.wormhole;

/**
 * Pure deterministic launch-safety rules for Interstellar Starship orbital departures.
 * Enforces realistic Origin Wormhole telemetry verification so breaking/closing/exploding the
 * origin Wormhole back on Mars/Earth prevents unsafe launches into empty interstellar void.
 */
public final class WormholeLinkCalculator {
	public static final int REQUIRED_ORBITAL_LAUNCH_FUEL = 500;

	public enum LinkStatus {
		UNLINKED,
		OPEN,
		CLOSED,
		DESTROYED
	}

	public enum LaunchDecision {
		READY,
		ABORTED_NO_LINK,
		ABORTED_WORMHOLE_OFFLINE,
		ABORTED_NO_FUEL
	}

	private WormholeLinkCalculator() {
	}

	public static LaunchDecision evaluateOrbitalLaunch(
		boolean departingExoplanet,
		LinkStatus originLinkStatus,
		int fuelUnits
	) {
		if (departingExoplanet) {
			if (originLinkStatus == null || originLinkStatus == LinkStatus.UNLINKED) {
				return LaunchDecision.ABORTED_NO_LINK;
			}
			if (originLinkStatus != LinkStatus.OPEN) {
				return LaunchDecision.ABORTED_WORMHOLE_OFFLINE;
			}
		}
		if (fuelUnits < REQUIRED_ORBITAL_LAUNCH_FUEL) {
			return LaunchDecision.ABORTED_NO_FUEL;
		}
		return LaunchDecision.READY;
	}

	public static final int MIN_RELOCATION_FUEL = 25;
	public static final double MAX_RELOCATION_DISTANCE = 1_000.0;

	public static int computeRelocationFuelCost(double distanceBlocks) {
		if (!Double.isFinite(distanceBlocks) || distanceBlocks <= 0.0) {
			return 0;
		}
		return Math.max(MIN_RELOCATION_FUEL, (int) Math.ceil(distanceBlocks * 1.25));
	}
}

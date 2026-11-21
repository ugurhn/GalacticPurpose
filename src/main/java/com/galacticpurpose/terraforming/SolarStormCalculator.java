package com.galacticpurpose.terraforming;

/**
 * Deterministic calculator for Red Giant solar storm (CME) cycles and radiation shielding.
 */
public final class SolarStormCalculator {
	public static final int CYCLE_SECONDS = 300;
	public static final int ACTIVE_DURATION_SECONDS = 45;
	public static final int WARNING_DURATION_SECONDS = 60;
	public static final int IMPACT_START_SECOND = CYCLE_SECONDS - ACTIVE_DURATION_SECONDS; // 255
	public static final int WARNING_START_SECOND = IMPACT_START_SECOND - WARNING_DURATION_SECONDS; // 195
	public static final float BASE_RADIATION_DAMAGE = 3.5f;

	private SolarStormCalculator() {
	}

	public static StormSnapshot evaluate(
		boolean redGiantActive,
		long stormElapsedTicks,
		boolean exposedToRadiation,
		boolean hasLeadSuit,
		boolean deepUnderground
	) {
		if (!redGiantActive) {
			return StormSnapshot.dormant();
		}

		int cycleSec = (int) Math.floorMod(Math.max(0L, stormElapsedTicks) / 20L, CYCLE_SECONDS);

		if (cycleSec >= IMPACT_START_SECOND) {
			int remaining = CYCLE_SECONDS - cycleSec;
			boolean shielded = !exposedToRadiation || hasLeadSuit || deepUnderground;
			float damage = shielded ? 0.0f : BASE_RADIATION_DAMAGE;
			return new StormSnapshot(SolarStormPhase.ACTIVE, 0, remaining, damage);
		}

		int untilImpact = IMPACT_START_SECOND - cycleSec;
		SolarStormPhase phase = cycleSec >= WARNING_START_SECOND
			? SolarStormPhase.WARNING
			: SolarStormPhase.COUNTDOWN;

		return new StormSnapshot(phase, untilImpact, 0, 0.0f);
	}
}

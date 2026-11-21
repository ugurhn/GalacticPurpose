package com.galacticpurpose.client.hud;

import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.network.MarsSyncPayload;
import com.galacticpurpose.terraforming.MarsProgressSnapshot;
import com.galacticpurpose.terraforming.MarsTerraformingCalculator;
import com.galacticpurpose.terraforming.SolarStormPhase;

/**
 * Client-side cache of synced Mars terraforming, Red Giant, and Solar Storm telemetry.
 */
public final class ClientMarsState {
	private static boolean activeInDimension = false;
	private static MarsProgressSnapshot snapshot = MarsProgressSnapshot.empty();
	private static boolean redGiantActive = false;
	private static SolarStormPhase stormPhase = SolarStormPhase.DORMANT;
	private static int stormTimerSeconds = 0;

	private ClientMarsState() {
	}

	public static void apply(MarsSyncPayload payload) {
		activeInDimension = payload.activeInDimension();
		snapshot = MarsTerraformingCalculator.snapshot(
			payload.heatPoints(),
			payload.pressurePoints(),
			payload.oxygenPoints()
		);
		redGiantActive = payload.redGiantActive();
		stormPhase = SolarStormPhase.byOrdinal(payload.stormPhaseOrdinal());
		stormTimerSeconds = payload.stormTimerSeconds();
		SpaceCompatManager.setClientMarsBreathable(activeInDimension && snapshot.breathable());
	}

	public static void reset() {
		activeInDimension = false;
		snapshot = MarsProgressSnapshot.empty();
		redGiantActive = false;
		stormPhase = SolarStormPhase.DORMANT;
		stormTimerSeconds = 0;
		SpaceCompatManager.setClientMarsBreathable(false);
	}

	public static boolean isActiveInDimension() {
		return activeInDimension;
	}

	public static MarsProgressSnapshot snapshot() {
		return snapshot;
	}

	public static boolean isRedGiantActive() {
		return redGiantActive;
	}

	public static SolarStormPhase getStormPhase() {
		return stormPhase;
	}

	public static int getStormTimerSeconds() {
		return stormTimerSeconds;
	}
}

package com.galacticpurpose.client.hud;

import com.galacticpurpose.climate.CollapsePhase;
import com.galacticpurpose.network.ClimateSyncPayload;

/**
 * Client-side cache of the latest synced Overworld solar instability state.
 */
public final class ClientClimateState {
	private static boolean activeInDimension = false;
	private static CollapsePhase phase = CollapsePhase.STABLE;
	private static long day = 0L;
	private static float severity = 0.0f;
	private static float ambientCelsius = 20.0f;
	private static boolean exposedToSky = false;

	private ClientClimateState() {
	}

	public static void apply(ClimateSyncPayload payload) {
		activeInDimension = payload.activeInDimension();
		phase = CollapsePhase.byOrdinal(payload.phaseOrdinal());
		day = payload.day();
		severity = payload.severity();
		ambientCelsius = payload.ambientCelsius();
		exposedToSky = payload.exposedToSky();
	}

	public static void reset() {
		activeInDimension = false;
		phase = CollapsePhase.STABLE;
		day = 0L;
		severity = 0.0f;
		ambientCelsius = 20.0f;
		exposedToSky = false;
	}

	public static boolean isActiveInDimension() {
		return activeInDimension;
	}

	public static CollapsePhase getPhase() {
		return phase;
	}

	public static long getDay() {
		return day;
	}

	public static float getSeverity() {
		return severity;
	}

	public static float getAmbientCelsius() {
		return ambientCelsius;
	}

	public static boolean isExposedToSky() {
		return exposedToSky;
	}
}

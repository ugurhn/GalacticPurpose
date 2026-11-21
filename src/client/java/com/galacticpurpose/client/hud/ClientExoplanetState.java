package com.galacticpurpose.client.hud;

import com.galacticpurpose.network.ExoplanetSyncPayload;
import com.galacticpurpose.wormhole.ExoplanetRoute;

/**
 * Client-side synced cache of active Erebus exoplanet environment and tidal gravity telemetry.
 */
public final class ClientExoplanetState {
	private static boolean activeInDimension = false;
	private static ExoplanetRoute route = ExoplanetRoute.HEPHAESTUS_IV;
	private static int ambientCelsius = 20;
	private static int pressureAtm = 1;
	private static boolean hazardous = false;
	private static String statusKey = "hazard.galacticpurpose.safe";
	private static float effectiveG = 1.0f;
	private static float blackHoleElevation = 0.0f;

	private ClientExoplanetState() {
	}

	public static void apply(ExoplanetSyncPayload payload) {
		activeInDimension = payload.activeInDimension();
		route = ExoplanetRoute.byOrdinal(payload.routeOrdinal());
		ambientCelsius = payload.ambientCelsius();
		pressureAtm = payload.pressureAtm();
		hazardous = payload.hazardous();
		statusKey = payload.statusKey();
		effectiveG = payload.effectiveG();
		blackHoleElevation = payload.blackHoleElevation();
	}

	public static void reset() {
		activeInDimension = false;
		route = ExoplanetRoute.HEPHAESTUS_IV;
		ambientCelsius = 20;
		pressureAtm = 1;
		hazardous = false;
		statusKey = "hazard.galacticpurpose.safe";
		effectiveG = 1.0f;
		blackHoleElevation = 0.0f;
	}

	public static boolean isActiveInDimension() {
		return activeInDimension;
	}

	public static ExoplanetRoute route() {
		return route;
	}

	public static int ambientCelsius() {
		return ambientCelsius;
	}

	public static int pressureAtm() {
		return pressureAtm;
	}

	public static boolean isHazardous() {
		return hazardous;
	}

	public static String statusKey() {
		return statusKey;
	}

	public static float effectiveG() {
		return effectiveG;
	}

	public static float blackHoleElevation() {
		return blackHoleElevation;
	}
}

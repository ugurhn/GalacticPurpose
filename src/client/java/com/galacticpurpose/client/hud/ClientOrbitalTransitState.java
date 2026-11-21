package com.galacticpurpose.client.hud;

import com.galacticpurpose.network.OrbitalTransitSyncPayload;
import com.galacticpurpose.ship.StarshipFlightPhysics;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import net.minecraft.util.Mth;

/**
 * Client-side mirror of the active Orbital Spaceflight session with silky 144-FPS sub-tick interpolation
 * so approaching planets & the 3D Space Wormhole glide smoothly with zero step-by-step jerking!
 */
public final class ClientOrbitalTransitState {
	private static OrbitalTransitSyncPayload current = OrbitalTransitSyncPayload.inactive();
	private static long lastSyncNanos = 0L;
	private static long arrivalFadeOutStartMs = 0L;
	private static boolean arrivalFadeWarm = false;

	private ClientOrbitalTransitState() {
	}

	public static void apply(OrbitalTransitSyncPayload payload) {
		if (!payload.inTransit() && current.inTransit() && current.reentryActive()) {
			arrivalFadeOutStartMs = System.currentTimeMillis();
			arrivalFadeWarm = !current.returning();
		}
		current = payload;
		lastSyncNanos = System.nanoTime();
	}

	public static void reset() {
		current = OrbitalTransitSyncPayload.inactive();
		lastSyncNanos = 0L;
		arrivalFadeOutStartMs = 0L;
	}

	public static boolean inTransit() { return current.inTransit(); }
	public static boolean returning() { return current.returning(); }
	public static boolean reentryActive() { return current.reentryActive(); }
	public static int fuelUnits() { return current.fuelUnits(); }
	public static int elapsedTicks() { return current.elapsedTicks(); }
	public static int cruiseDurationTicks() {
		return Math.max(1, current.cruiseDurationTicks());
	}

	public static float entryProgress() {
		if (!current.inTransit() || !current.reentryActive()) {
			return 0.0f;
		}
		int dur = Math.max(1, current.reentryDurationTicks());
		float extraTicks = 0.0f;
		if (lastSyncNanos > 0L) {
			double elapsedSec = (System.nanoTime() - lastSyncNanos) * 1.0e-9;
			extraTicks = (float) Math.min(2.0, Math.max(0.0, elapsedSec * 20.0));
		}
		return Mth.clamp((current.reentryTicks() + extraTicks) / (float) dur, 0.0f, 1.0f);
	}

	public static float postArrivalFadeOutAlpha() {
		if (arrivalFadeOutStartMs <= 0L) {
			return 0.0f;
		}
		long dt = System.currentTimeMillis() - arrivalFadeOutStartMs;
		if (dt >= 1400L) {
			arrivalFadeOutStartMs = 0L;
			return 0.0f;
		}
		float rem = 1.0f - Mth.clamp(dt / 1400.0f, 0.0f, 1.0f);
		return rem * rem;
	}

	public static boolean arrivalFadeIsWarm() {
		return arrivalFadeWarm;
	}

	/**
	 * Smoothly interpolated [0..1] cruise progress advancing continuously between server syncs at monitor refresh rate.
	 */
	public static float approachProgress() {
		if (!current.inTransit()) {
			return 0.0f;
		}
		int dur = cruiseDurationTicks();
		float extraTicks = 0.0f;
		if (!current.reentryActive() && lastSyncNanos > 0L && current.elapsedTicks() < dur) {
			double elapsedSec = (System.nanoTime() - lastSyncNanos) * 1.0e-9;
			extraTicks = (float) Math.min(3.0, Math.max(0.0, elapsedSec * 20.0));
		}
		return Mth.clamp((current.elapsedTicks() + extraTicks) / (float) dur, 0.0f, 1.0f);
	}

	public static boolean arrived() {
		return current.inTransit() && current.elapsedTicks() >= cruiseDurationTicks();
	}

	public static int remainingSeconds() {
		if (arrived()) {
			return 0;
		}
		float prog = approachProgress();
		float totalSec = cruiseDurationTicks() / 20.0f;
		return Math.max(1, (int) Math.ceil((1.0f - prog) * totalSec));
	}

	public static int cruisePhase() {
		return StarshipFlightPhysics.computeCruisePhase(approachProgress());
	}

	public static ExoplanetRoute route() {
		ExoplanetRoute[] vals = ExoplanetRoute.values();
		int idx = current.routeOrdinal();
		return (idx >= 0 && idx < vals.length) ? vals[idx] : ExoplanetRoute.HEPHAESTUS_IV;
	}

	public static WormholeLinkCalculator.LinkStatus linkStatus() {
		WormholeLinkCalculator.LinkStatus[] vals = WormholeLinkCalculator.LinkStatus.values();
		int idx = current.linkStatusOrdinal();
		return (idx >= 0 && idx < vals.length) ? vals[idx] : WormholeLinkCalculator.LinkStatus.UNLINKED;
	}
}

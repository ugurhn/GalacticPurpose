package com.galacticpurpose.ship;

import net.minecraft.util.Mth;

/**
 * Pure orbital flight, aerodynamic ascent, PD-oscillating vector-thrust landing, and relativistic
 * 1/d optical formulas (7,000 km super-Earth vs 60 m Space Wormhole over a symmetric 12,000 km corridor).
 */
public final class StarshipFlightPhysics {
	public static final int COUNTDOWN_TICKS = 200;
	public static final int ASCENT_DURATION_TICKS = 900;
	public static final int CRUISE_DURATION_TICKS = 1800;
	public static final int REENTRY_DURATION_TICKS = 55;
	public static final double TRANSIT_CORRIDOR_KM = 12_000.0;
	public static final double PLANET_RADIUS_KM = 7_000.0;
	public static final double WORMHOLE_RADIUS_KM = 0.030; // 60 m throat diameter

	public record AscentReading(
			double forwardSpeed,
			float pitchDeg,
			float simulatedAltitudeKm,
			int stageIndex,
			float skyDarkness
	) {}

	public record LandingKinematics(
			float pitchDeg,
			float vectorAngleDeg,
			boolean skidsExtended,
			double targetHorizontalSpeed,
			double targetVerticalSpeed,
			int landingPhase
	) {}

	public record DynamicLandingStep(
			double nextForwardSpeed,
			double nextVerticalSpeed,
			float pitchDeg,
			float vectorAngleDeg,
			boolean skidsExtended,
			int landingPhase
	) {}

	private StarshipFlightPhysics() {
	}

	public static int countdownSecondsRemaining(int countdownTicks) {
		int clamped = Mth.clamp(countdownTicks, 0, COUNTDOWN_TICKS);
		return (COUNTDOWN_TICKS - clamped + 19) / 20;
	}

	public static double computeRunwayLaunchSpeed(int launchTicks) {
		float u = Mth.clamp(launchTicks / 70.0f, 0.0f, 1.0f);
		return 0.16 + 0.62 * Math.pow(u, 1.30);
	}

	/**
	 * High-acceleration spaceplane climb profile:
	 * - Phase 1 (0..8%, 0..3.6s): stays shallow (0 -> 7 deg) while accelerating past 15 m/s (0.75 b/t).
	 * - Phase 2 (8..22%, 3.6..9.9s): wings bite above 15 m/s and pitch sharply to ~88.6 deg.
	 * - Phase 3 (22..100%): near-vertical high-thrust orbital boost up to 11.5 b/t (230 m/s),
	 *   keeping total horizontal drift < 98 blocks across 45s to avoid chunk-generation lag spikes.
	 */
	public static AscentReading computeAscentProfile(int ascentTicks) {
		float u = Mth.clamp(ascentTicks / (float) ASCENT_DURATION_TICKS, 0.0f, 1.0f);

		double forwardSpeed;
		float pitchDeg;
		if (u < 0.07f) {
			float s = u / 0.07f;
			// Accelerate rapidly from 4.4 m/s to ~17.0 m/s horizontally before rotating up steeply
			forwardSpeed = 0.22 + 0.63 * Math.pow(s, 1.10);
			pitchDeg = 7.0f * (s * s);
		} else if (u < 0.16f) {
			float s = (u - 0.07f) / 0.09f;
			float smooth = s * s * (3.0f - 2.0f * s);
			pitchDeg = 7.0f + 82.4f * Mth.clamp(smooth, 0.0f, 1.0f);
			forwardSpeed = 0.85 + 0.65 * Math.pow(s, 1.25);
		} else {
			float s = (u - 0.16f) / 0.84f;
			pitchDeg = 89.4f + 0.50f * s;
			forwardSpeed = 1.50 + 10.00 * Math.pow(s, 1.50);
		}

		float altitudeKm = 120.0f * (float) Math.pow(u, 1.85);
		int stageIndex = u < 0.22f ? 1 : (u < 0.62f ? 2 : 3);
		float skyDarkness = Mth.clamp((u - 0.16f) / 0.56f, 0.0f, 1.0f);
		return new AscentReading(forwardSpeed, pitchDeg, altitudeKm, stageIndex, skyDarkness);
	}

	/**
	 * Velocity-coupled PD gimbal & damped-rocking hover stabilizer for realistic horizontal braking:
	 * - Forward speed pulls gimbal aft->downward past 90 deg (up to 116 deg) & flares nose up (+18 deg)
	 * - Overshoot/rebound gently tips nose slightly forward (-5 deg) & swings gimbal below 90 deg (74 deg)
	 *   over 2-3 damped pulses until horizontal error settles right above the landing site.
	 */
	public static DynamicLandingStep stepDynamicLanding(
			double distAlongTarget,
			double currentFwdSpeed,
			double currentVertSpeed,
			double altAboveGround,
			float prevPitchDeg,
			float prevVectorAngleDeg
	) {
		if (distAlongTarget > 26.0 && altAboveGround > 18.0) {
			double vFwd = Mth.lerp(0.12, currentFwdSpeed, 1.15);
			double desiredVy = altAboveGround > 30.0 ? -0.78 : -0.54;
			double vVert = Mth.lerp(0.14, currentVertSpeed, desiredVy);
			float pitch = Mth.lerp(0.14f, prevPitchDeg, -18.0f);
			float gimbal = Mth.lerp(0.14f, prevVectorAngleDeg, 0.0f);
			return new DynamicLandingStep(vFwd, vVert, pitch, gimbal, false, 0);
		}

		boolean closePad = Math.abs(distAlongTarget) <= 3.5 && altAboveGround <= 4.5 && Math.abs(currentFwdSpeed) <= 0.12;
		if (closePad) {
			float pitch = Mth.lerp(0.20f, prevPitchDeg, 0.0f);
			float gimbal = Mth.lerp(0.20f, prevVectorAngleDeg, 90.0f);
			double vFwd = currentFwdSpeed * 0.72 + distAlongTarget * 0.04;
			double vVert = -(0.075 + 0.065 * Mth.clamp(altAboveGround / 4.5, 0.0, 1.0));
			return new DynamicLandingStep(vFwd, vVert, pitch, gimbal, true, 2);
		}

		// PD controller on forward position/velocity: deliberately slightly underdamped for 2-3 subtle aero rocks!
		double targetSpeed = Mth.clamp(distAlongTarget * 0.078, -0.18, 0.72);
		double speedError = currentFwdSpeed - targetSpeed;
		float desiredGimbal = 90.0f + Mth.clamp((float) (speedError * 58.0), -20.0f, 26.0f);
		float gimbal = Mth.lerp(0.16f, prevVectorAngleDeg, desiredGimbal);

		double vectorHorizThrust = -Math.sin(Math.toRadians(gimbal - 90.0)) * 0.165;
		double nextFwd = Mth.clamp(currentFwdSpeed + vectorHorizThrust - currentFwdSpeed * 0.032, -0.32, 1.35);

		float desiredPitch = Mth.clamp((float) (speedError * 30.0 + Math.max(0.0, currentFwdSpeed) * 14.0), -6.5f, 19.0f);
		float pitch = Mth.lerp(0.15f, prevPitchDeg, desiredPitch);

		double desiredVy;
		if (altAboveGround <= 4.0 && Math.abs(distAlongTarget) > 4.5) {
			desiredVy = 0.0; // Hover cushion until horizontal braking completes
		} else {
			desiredVy = -Mth.lerp(Mth.clamp(altAboveGround / 28.0, 0.0, 1.0), 0.11, 0.56);
		}
		double nextVert = Mth.lerp(0.18, currentVertSpeed, desiredVy);
		boolean skids = altAboveGround <= 18.0 || Math.abs(distAlongTarget) <= 14.0;
		return new DynamicLandingStep(nextFwd, nextVert, pitch, gimbal, skids, 1);
	}

	public static LandingKinematics computeLandingKinematics(double altitudeAboveGround, double horizDist) {
		DynamicLandingStep s = stepDynamicLanding(
				horizDist,
				horizDist > 26.0 ? 1.15 : 0.45,
				-0.45,
				altitudeAboveGround,
				horizDist > 26.0 ? -18.0f : 12.0f,
				horizDist > 26.0 ? 0.0f : 90.0f
		);
		return new LandingKinematics(
				s.pitchDeg(),
				s.vectorAngleDeg(),
				s.skidsExtended(),
				Math.abs(s.nextForwardSpeed()),
				s.nextVerticalSpeed(),
				s.landingPhase()
		);
	}

	public static float computeDescentPitch(double altitudeAboveGround) {
		if (altitudeAboveGround > 18.0) {
			return -18.0f;
		}
		if (altitudeAboveGround > 4.5) {
			float flare = Mth.clamp(1.0f - (float) ((altitudeAboveGround - 4.5) / 13.5), 0.0f, 1.0f);
			return Mth.lerp(flare, -18.0f, 13.5f);
		}
		return 13.5f * Mth.clamp((float) (altitudeAboveGround / 4.5), 0.0f, 1.0f);
	}

	public static int computeCruisePhase(float progress) {
		float p = Mth.clamp(progress, 0.0f, 1.0f);
		if (p < 0.30f) {
			return 0;
		}
		if (p < 0.65f) {
			return 1;
		}
		if (p < 0.95f) {
			return 2;
		}
		return 3;
	}

	public static int computePlanetDistanceKm(float progress) {
		return computePlanetDistanceKm(progress, true);
	}

	public static int computePlanetDistanceKm(float progress, boolean outbound) {
		float p = Mth.clamp(progress, 0.0f, 1.0f);
		if (outbound) {
			double d = 120.0 + (TRANSIT_CORRIDOR_KM - 120.0) * Math.pow(1.0 - p, 1.35);
			return (int) Math.max(120L, Math.round(d));
		}
		double d = 120.0 + (TRANSIT_CORRIDOR_KM - 120.0) * Math.pow(p, 1.35);
		return (int) Math.max(120L, Math.round(d));
	}

	/**
	 * Return transit traverses the exact symmetric 12,000 km corridor back to the 60m Origin Wormhole.
	 */
	public static float computeWormholeDistanceKm(float progress) {
		float p = Mth.clamp(progress, 0.0f, 1.0f);
		return (float) (0.05 + (TRANSIT_CORRIDOR_KM - 0.05) * Math.pow(1.0 - p, 1.85));
	}

	/**
	 * Outbound retreat distance from the 60m Exit Wormhole behind the craft (0.05 km -> 12,000 km).
	 */
	public static float computeOutboundWormholeDistanceKm(float progress) {
		float p = Mth.clamp(progress, 0.0f, 1.0f);
		return (float) (0.05 + (TRANSIT_CORRIDOR_KM - 0.05) * Math.pow(p, 2.25));
	}

	/**
	 * True 1/d spherical angular radius in degrees for a sphere of given physical radius.
	 */
	public static float computeAngularRadiusDeg(double radiusKm, double centerDistanceKm) {
		double ratio = Math.min(0.992, Math.max(1.0e-5, radiusKm / Math.max(radiusKm * 1.004, centerDistanceKm)));
		return (float) Math.toDegrees(Math.asin(ratio));
	}

	public static float computePlanetVisualScale(float progress, boolean returning, float entryProgress) {
		float p = Mth.clamp(progress, 0.0f, 1.0f);
		float e = Mth.clamp(entryProgress, 0.0f, 1.0f);
		if (returning) {
			return Mth.lerp((float) Math.pow(p, 0.72), 20.0f, 11.0f);
		}
		float cruiseScale = 21.0f + 56.0f * (float) Math.pow(p, 1.35) + 19.0f * (float) Math.pow(p, 5.0);
		return cruiseScale + 12.0f * (float) Math.pow(e, 1.15);
	}

	public static float computeWormholeVisualScale(float progress, boolean returning, float entryProgress) {
		float p = Mth.clamp(progress, 0.0f, 1.0f);
		float e = Mth.clamp(entryProgress, 0.0f, 1.0f);
		if (!returning) {
			return Mth.clamp(20.0f / (1.0f + 88.0f * p * p), 0.16f, 20.0f);
		}
		float base = 0.16f
			+ 1.35f * (float) Math.pow(p, 1.35)
			+ 5.80f * (float) Math.pow(p, 3.80)
			+ 17.69f * (float) Math.pow(p, 9.50);
		return base + 14.0f * (float) Math.pow(e, 1.15);
	}

	public static String computeCompassBearing(double dx, double dz) {
		if (dx * dx + dz * dz < 1.0e-6) {
			return "N";
		}
		double deg = Math.toDegrees(Math.atan2(dx, -dz));
		if (deg < 0.0) {
			deg += 360.0;
		}
		int sector = (int) Math.floor((deg + 22.5) / 45.0) & 7;
		return switch (sector) {
			case 0 -> "N";
			case 1 -> "NE";
			case 2 -> "E";
			case 3 -> "SE";
			case 4 -> "S";
			case 5 -> "SW";
			case 6 -> "W";
			default -> "NW";
		};
	}

	public static float outwardWindingDot(
			float x0, float y0, float z0,
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float nx, float ny, float nz
	) {
		float ux = x1 - x0;
		float uy = y1 - y0;
		float uz = z1 - z0;
		float vx = x2 - x0;
		float vy = y2 - y0;
		float vz = z2 - z0;
		float cx = uy * vz - uz * vy;
		float cy = uz * vx - ux * vz;
		float cz = ux * vy - uy * vx;
		return cx * nx + cy * ny + cz * nz;
	}
}

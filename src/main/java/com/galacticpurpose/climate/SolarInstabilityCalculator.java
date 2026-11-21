package com.galacticpurpose.climate;

import com.galacticpurpose.config.ModConfig;

/**
 * Pure deterministic calculator for Overworld solar instability, temperatures, and surface hazards.
 */
public final class SolarInstabilityCalculator {
	private static final long TICKS_PER_DAY = 24000L;
	private static final long HALF_DAY_TICKS = 12000L;

	private SolarInstabilityCalculator() {
	}

	public static CollapsePhase determinePhase(long day, ModConfig config) {
		if (!config.enableOverworldCollapse() || day < config.graceDays()) {
			return CollapsePhase.STABLE;
		}
		if (day < config.warningEndDay()) {
			return CollapsePhase.WARNING;
		}
		if (day < config.terminalDay()) {
			return CollapsePhase.CRITICAL;
		}
		return CollapsePhase.TERMINAL;
	}

	public static float computeSeverity(long day, ModConfig config) {
		if (!config.enableOverworldCollapse() || day < config.graceDays()) {
			return 0.0f;
		}
		long transitionDays = Math.max(1L, (long) config.warningDays() + config.criticalDays());
		long elapsed = day - config.graceDays();
		return Math.min(1.0f, Math.max(0.0f, elapsed / (float) transitionDays));
	}

	public static ClimateSnapshot evaluate(
		long day,
		long timeOfDay,
		boolean exposedToSky,
		boolean inWater,
		boolean hasThermalShield,
		ModConfig config
	) {
		long normalizedTime = Math.floorMod(timeOfDay, TICKS_PER_DAY);
		boolean daytime = normalizedTime < HALF_DAY_TICKS;
		float sunFactor = daytime ? (float) Math.sin(Math.PI * (normalizedTime / (double) HALF_DAY_TICKS)) : 0.0f;
		float nightFactor = !daytime
			? (float) Math.sin(Math.PI * ((normalizedTime - HALF_DAY_TICKS) / (double) HALF_DAY_TICKS))
			: 0.0f;

		CollapsePhase phase = determinePhase(day, config);
		float severity = computeSeverity(day, config);

		float shelterDamping = exposedToSky ? 1.0f : 0.20f;
		float baseCelsius = 19.0f + (5.0f * sunFactor) - (5.0f * nightFactor);
		float swingCelsius = (46.0f * severity * sunFactor) - (72.0f * severity * nightFactor);
		float ambientCelsius = baseCelsius + (swingCelsius * shelterDamping);

		if (!exposedToSky || hasThermalShield || phase == CollapsePhase.STABLE || phase == CollapsePhase.WARNING) {
			return new ClimateSnapshot(phase, severity, daytime, ambientCelsius, 0.0f, 0.0f, false, false);
		}

		float heatDamage = 0.0f;
		float coldDamage = 0.0f;
		boolean applyIgnition = false;
		boolean applySlowness = false;

		if (phase == CollapsePhase.CRITICAL) {
			if (sunFactor > 0.15f) {
				float waterFactor = inWater ? 0.4f : 1.0f;
				heatDamage = config.baseDayHeatDamage() * sunFactor * severity * waterFactor;
			}
			if (nightFactor > 0.15f) {
				coldDamage = config.baseNightColdDamage() * nightFactor * severity;
				applySlowness = true;
			}
		} else if (phase == CollapsePhase.TERMINAL) {
			if (sunFactor > 0.10f) {
				heatDamage = config.baseDayHeatDamage() * 2.0f * sunFactor;
				applyIgnition = sunFactor > 0.35f;
			}
			if (nightFactor > 0.10f) {
				coldDamage = config.baseNightColdDamage() * 2.2f * nightFactor;
				applySlowness = true;
			}
		}

		return new ClimateSnapshot(
			phase,
			severity,
			daytime,
			ambientCelsius,
			heatDamage,
			coldDamage,
			applyIgnition,
			applySlowness
		);
	}
}

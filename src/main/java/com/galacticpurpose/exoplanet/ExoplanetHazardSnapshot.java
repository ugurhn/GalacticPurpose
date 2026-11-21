package com.galacticpurpose.exoplanet;

import com.galacticpurpose.wormhole.ExoplanetRoute;

/**
 * Immutable evaluation of environmental hazards on an Erebus exoplanet for one player.
 */
public record ExoplanetHazardSnapshot(
	ExoplanetRoute route,
	int ambientCelsius,
	int pressureAtm,
	boolean hazardous,
	float heatDamage,
	float coldDamage,
	float pressureDamage,
	boolean thermalProtected,
	boolean pressureProtected,
	String statusKey
) {
}

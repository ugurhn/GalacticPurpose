package com.galacticpurpose.terraforming;

/**
 * Stages of the Red Giant solar storm (CME) cycle on Mars.
 */
public enum SolarStormPhase {
	DORMANT("storm.galacticpurpose.dormant", 0x64748B),
	COUNTDOWN("storm.galacticpurpose.countdown", 0x38BDF8),
	WARNING("storm.galacticpurpose.warning", 0xFFAA00),
	ACTIVE("storm.galacticpurpose.active", 0xFF4444);

	private final String translationKey;
	private final int accentRgb;

	SolarStormPhase(String translationKey, int accentRgb) {
		this.translationKey = translationKey;
		this.accentRgb = accentRgb;
	}

	public String translationKey() {
		return this.translationKey;
	}

	public int accentRgb() {
		return this.accentRgb;
	}

	public static SolarStormPhase byOrdinal(int ordinal) {
		SolarStormPhase[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return DORMANT;
		}
		return values[ordinal];
	}
}

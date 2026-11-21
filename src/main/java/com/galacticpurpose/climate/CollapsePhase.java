package com.galacticpurpose.climate;

/**
 * Stages of the Overworld's progressive solar collapse.
 */
public enum CollapsePhase {
	STABLE("phase.galacticpurpose.stable", 0x55FF55),
	WARNING("phase.galacticpurpose.warning", 0xFFAA00),
	CRITICAL("phase.galacticpurpose.critical", 0xFF6B35),
	TERMINAL("phase.galacticpurpose.terminal", 0xFF4D4D);

	private final String translationKey;
	private final int accentRgb;

	CollapsePhase(String translationKey, int accentRgb) {
		this.translationKey = translationKey;
		this.accentRgb = accentRgb;
	}

	public String translationKey() {
		return this.translationKey;
	}

	public int accentRgb() {
		return this.accentRgb;
	}

	public static CollapsePhase byOrdinal(int ordinal) {
		CollapsePhase[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return STABLE;
		}
		return values[ordinal];
	}
}

package com.galacticpurpose.wormhole;

/**
 * Operational states of the Wormhole Stabilizer containment core.
 */
public enum WormholeState {
	IDLE("wormhole.galacticpurpose.state.idle", 0x94A3B8),
	CHARGING("wormhole.galacticpurpose.state.charging", 0x38BDF8),
	OPEN("wormhole.galacticpurpose.state.open", 0x22C55E),
	UNSTABLE("wormhole.galacticpurpose.state.unstable", 0xFF4444),
	SHUTTING_DOWN("wormhole.galacticpurpose.state.shutting_down", 0xF59E0B);

	private final String translationKey;
	private final int accentRgb;

	WormholeState(String translationKey, int accentRgb) {
		this.translationKey = translationKey;
		this.accentRgb = accentRgb;
	}

	public String translationKey() {
		return this.translationKey;
	}

	public int accentRgb() {
		return this.accentRgb;
	}

	public static WormholeState byOrdinal(int ordinal) {
		WormholeState[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return IDLE;
		}
		return values[ordinal];
	}
}

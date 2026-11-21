package com.galacticpurpose.wormhole;

/**
 * Supported Wormhole Stabilizer multiblock scales:
 * PERSONAL (7x5x7, 4 single stabilizers) for individual travel, and
 * COLOSSAL (45x41x45 Ship-Scale Observatory, 4x 3x3 = 36 linked stabilizers, 20.4-block diameter core) for ships & endgame travel.
 */
public enum WormholeTier {
	PERSONAL(3, 3, 1, 0, 1.25f, 56, "tier.galacticpurpose.personal"),
	COLOSSAL(21, 21, 13, 5, 10.20f, 112, "tier.galacticpurpose.colossal");

	private final int stabilizerDistance;
	private final int injectorDrop;
	private final int portalDrop;
	private final int portalHalfExtent;
	private final float coreRadius;
	private final int collapseBlastRadius;
	private final String translationKey;

	WormholeTier(
		int stabilizerDistance,
		int injectorDrop,
		int portalDrop,
		int portalHalfExtent,
		float coreRadius,
		int collapseBlastRadius,
		String translationKey
	) {
		this.stabilizerDistance = stabilizerDistance;
		this.injectorDrop = injectorDrop;
		this.portalDrop = portalDrop;
		this.portalHalfExtent = portalHalfExtent;
		this.coreRadius = coreRadius;
		this.collapseBlastRadius = collapseBlastRadius;
		this.translationKey = translationKey;
	}

	public int stabilizerDistance() {
		return this.stabilizerDistance;
	}

	public int injectorDrop() {
		return this.injectorDrop;
	}

	public int portalDrop() {
		return this.portalDrop;
	}

	public int portalHalfExtent() {
		return this.portalHalfExtent;
	}

	public float coreRadius() {
		return this.coreRadius;
	}

	public int collapseBlastRadius() {
		return this.collapseBlastRadius;
	}

	public String translationKey() {
		return this.translationKey;
	}

	public static WormholeTier byOrdinal(int ordinal) {
		WormholeTier[] vals = values();
		return (ordinal >= 0 && ordinal < vals.length) ? vals[ordinal] : PERSONAL;
	}
}

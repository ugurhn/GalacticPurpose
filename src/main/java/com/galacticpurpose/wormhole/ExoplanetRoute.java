package com.galacticpurpose.wormhole;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.resources.ResourceLocation;

/**
 * Target exoplanet routes discoverable by the Deep Space Telescope and dialed into the Wormhole Stabilizer.
 */
public enum ExoplanetRoute {
	HEPHAESTUS_IV("hephaestus_iv", "hephaestus_caldera", 0xFF6B2B, false),
	PELAGIA_9("pelagia_9", "pelagia_abyss", 0x0EA5E9, false),
	NYX_0("nyx_0", "nyx_cryo_void", 0xA855F7, false),
	SANCTUARY("sanctuary", "sanctuary_meadow", 0x22C55E, true);

	private final String id;
	private final String biomeId;
	private final int accentRgb;
	private final boolean requiresQuantumCore;
	private final ResourceLocation dimensionId;

	ExoplanetRoute(String id, String biomeId, int accentRgb, boolean requiresQuantumCore) {
		this.id = id;
		this.biomeId = biomeId;
		this.accentRgb = accentRgb;
		this.requiresQuantumCore = requiresQuantumCore;
		this.dimensionId = GalacticPurpose.id(id);
	}

	public String id() {
		return this.id;
	}

	public String biomeId() {
		return this.biomeId;
	}

	public int accentRgb() {
		return this.accentRgb;
	}

	public boolean requiresQuantumCore() {
		return this.requiresQuantumCore;
	}

	public ResourceLocation dimensionId() {
		return this.dimensionId;
	}

	public String translationKey() {
		return "route.galacticpurpose." + this.id;
	}

	public String landmarkNameKey() {
		return "landmark.galacticpurpose." + this.id + ".name";
	}

	public String landmarkHintKey() {
		return "landmark.galacticpurpose." + this.id + ".hint";
	}

	public static ExoplanetRoute byOrdinal(int ordinal) {
		ExoplanetRoute[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return HEPHAESTUS_IV;
		}
		return values[ordinal];
	}

	public static ExoplanetRoute fromDimensionId(ResourceLocation dimensionId) {
		if (dimensionId == null) {
			return null;
		}
		for (ExoplanetRoute route : values()) {
			if (route.dimensionId.equals(dimensionId)) {
				return route;
			}
		}
		return null;
	}
}

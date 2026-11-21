package com.galacticpurpose.config;

import com.galacticpurpose.GalacticPurpose;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent configuration for progression timings, dimension IDs, and environmental hazard tuning.
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile ModConfig instance = defaults();

	private boolean enableOverworldCollapse = true;
	private int graceDays = 20;
	private int warningDays = 15;
	private int criticalDays = 15;
	private int hazardCheckIntervalTicks = 20;
	private float baseDayHeatDamage = 2.0f;
	private float baseNightColdDamage = 1.5f;
	private String marsDimensionId = "galacticraft:mars";
	private boolean marsTerraformingTestMode = false;
	private boolean showHudInStablePhase = true;
	private boolean enableRedGiantCollapse = true;

	private ModConfig() {
	}

	public static ModConfig defaults() {
		return new ModConfig();
	}

	public static ModConfig get() {
		return instance;
	}

	public static ModConfig loadOrCreate(Path configDir) {
		Path file = configDir.resolve(GalacticPurpose.MOD_ID + ".json");
		ModConfig loaded = defaults();

		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				ModConfig parsed = GSON.fromJson(reader, ModConfig.class);
				if (parsed != null) {
					loaded = parsed;
				}
			} catch (Exception ex) {
				GalacticPurpose.LOGGER.warn("Failed to parse {}, falling back to defaults: {}", file, ex.getMessage());
			}
		}

		loaded.sanitize();
		save(file, loaded);
		instance = loaded;
		return loaded;
	}

	private static void save(Path file, ModConfig config) {
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ex) {
			GalacticPurpose.LOGGER.warn("Could not write config {}: {}", file, ex.getMessage());
		}
	}

	private void sanitize() {
		this.graceDays = Math.max(0, this.graceDays);
		this.warningDays = Math.max(1, this.warningDays);
		this.criticalDays = Math.max(1, this.criticalDays);
		this.hazardCheckIntervalTicks = Math.max(1, this.hazardCheckIntervalTicks);
		this.baseDayHeatDamage = Math.max(0.0f, this.baseDayHeatDamage);
		this.baseNightColdDamage = Math.max(0.0f, this.baseNightColdDamage);
		if (this.marsDimensionId == null || this.marsDimensionId.isBlank()) {
			this.marsDimensionId = "galacticraft:mars";
		}
	}

	public boolean enableOverworldCollapse() {
		return this.enableOverworldCollapse;
	}

	public int graceDays() {
		return this.graceDays;
	}

	public int warningDays() {
		return this.warningDays;
	}

	public int criticalDays() {
		return this.criticalDays;
	}

	public int warningEndDay() {
		return this.graceDays + this.warningDays;
	}

	public int terminalDay() {
		return this.graceDays + this.warningDays + this.criticalDays;
	}

	public int hazardCheckIntervalTicks() {
		return this.hazardCheckIntervalTicks;
	}

	public float baseDayHeatDamage() {
		return this.baseDayHeatDamage;
	}

	public float baseNightColdDamage() {
		return this.baseNightColdDamage;
	}

	public String marsDimensionId() {
		return this.marsDimensionId;
	}

	public boolean marsTerraformingTestMode() {
		return this.marsTerraformingTestMode;
	}

	public void setMarsTerraformingTestMode(boolean enabled) {
		this.marsTerraformingTestMode = enabled;
	}

	public boolean showHudInStablePhase() {
		return this.showHudInStablePhase;
	}

	public boolean enableRedGiantCollapse() {
		return this.enableRedGiantCollapse;
	}
}

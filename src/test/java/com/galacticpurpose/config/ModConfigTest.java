package com.galacticpurpose.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModConfigTest {

	@Test
	void loadOrCreateGeneratesDefaultFileAndSanitizesInvalidValues(@TempDir Path tempDir) throws Exception {
		ModConfig initial = ModConfig.loadOrCreate(tempDir);
		Path configFile = tempDir.resolve("galacticpurpose.json");

		assertTrue(Files.exists(configFile));
		assertEquals(20, initial.graceDays());
		assertEquals(50, initial.terminalDay());

		Files.writeString(configFile, """
			{
			  "enableOverworldCollapse": true,
			  "graceDays": -5,
			  "warningDays": 0,
			  "criticalDays": -2,
			  "hazardCheckIntervalTicks": 0,
			  "baseDayHeatDamage": -3.0,
			  "baseNightColdDamage": -1.0
			}
			""");

		ModConfig sanitized = ModConfig.loadOrCreate(tempDir);
		assertEquals(0, sanitized.graceDays());
		assertEquals(1, sanitized.warningDays());
		assertEquals(1, sanitized.criticalDays());
		assertEquals(1, sanitized.hazardCheckIntervalTicks());
		assertEquals(0.0f, sanitized.baseDayHeatDamage(), 0.0001f);
		assertEquals(0.0f, sanitized.baseNightColdDamage(), 0.0001f);
	}
}

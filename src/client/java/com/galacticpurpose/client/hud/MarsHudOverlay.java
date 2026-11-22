package com.galacticpurpose.client.hud;

import com.galacticpurpose.terraforming.MarsProgressSnapshot;
import com.galacticpurpose.terraforming.SolarStormPhase;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders live Mars Terraforming Index, milestone unlocks, Red Giant status, live shelter seal telemetry,
 * and animated CME solar storm screen pulses.
 */
public final class MarsHudOverlay {
	private MarsHudOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null || !ClientMarsState.isActiveInDimension()) {
			return;
		}

		renderSolarStormVignette(graphics, mc);

		MarsProgressSnapshot snap = ClientMarsState.snapshot();
		List<Component> lines = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();

		lines.add(Component.translatable(
			"hud.galacticpurpose.mars_header",
			Math.round(snap.terraformingIndex())
		));
		colors.add(ClientMarsState.isRedGiantActive() ? 0xFF8844 : 0x66D9FF);

		lines.add(Component.translatable(
			"hud.galacticpurpose.mars_metrics",
			formatTenthsFloor(snap.heatPercent()),
			formatTenthsFloor(snap.pressurePercent()),
			formatTenthsFloor(snap.oxygenPercent())
		));
		colors.add(0xE2E8F0);

		if (snap.breathable() || snap.floraReady() || snap.liquidWaterReady()) {
			String stageKey = snap.breathable()
				? "hud.galacticpurpose.stage_breathable"
				: (snap.floraReady() ? "hud.galacticpurpose.stage_flora" : "hud.galacticpurpose.stage_water");
			lines.add(Component.translatable(stageKey));
			colors.add(snap.breathable() ? 0x55FF88 : 0x66E3B4);
		}

		if (ClientMarsState.isRedGiantActive()) {
			SolarStormPhase storm = ClientMarsState.getStormPhase();
			int secs = ClientMarsState.getStormTimerSeconds();
			String timer = String.format("%02d:%02d", secs / 60, secs % 60);
			String key = storm == SolarStormPhase.ACTIVE
				? "hud.galacticpurpose.storm_active"
				: "hud.galacticpurpose.storm_timer";
			lines.add(Component.translatable(key, timer));
			colors.add(storm.accentRgb());

			float leak = ClientMarsState.getShelterLeakPercent();
			if (leak <= 0.05f) {
				lines.add(Component.translatable("hud.galacticpurpose.shelter_sealed"));
				colors.add(0x55FF88);
			} else if (leak < 99.5f) {
				lines.add(Component.translatable("hud.galacticpurpose.shelter_leaky", Math.max(1, Math.round(leak))));
				colors.add(0xFFAA33);
			} else {
				lines.add(Component.translatable("hud.galacticpurpose.shelter_exposed"));
				colors.add(0xFF5555);
			}
		}

		Font font = mc.font;
		int maxWidth = 0;
		for (Component line : lines) {
			maxWidth = Math.max(maxWidth, font.width(line));
		}
		int width = maxWidth + 12;
		int height = 6 + lines.size() * 11;
		int x = graphics.guiWidth() - width - 8;
		int y = 8;

		int borderColor = ClientMarsState.getStormPhase() == SolarStormPhase.ACTIVE
			? 0xFFFF3333
			: (ClientMarsState.isRedGiantActive() ? 0xFFFF7733 : 0xFF33B5E5);

		graphics.fill(x, y, x + width, y + height, 0xB80B0F17);
		graphics.renderOutline(x, y, width, height, borderColor);
		for (int i = 0; i < lines.size(); i++) {
			graphics.drawString(font, lines.get(i), x + 6, y + 4 + i * 11, colors.get(i), true);
		}
	}

	private static String formatTenthsFloor(float value) {
		double clamped = Math.max(0.0, Math.min(100.0, value));
		double floored = Math.floor(clamped * 10.0 + 1.0e-5) / 10.0;
		return String.format("%.1f", floored);
	}

	private static void renderSolarStormVignette(GuiGraphics graphics, Minecraft mc) {
		SolarStormPhase phase = ClientMarsState.getStormPhase();
		if (!ClientMarsState.isRedGiantActive() || (phase != SolarStormPhase.WARNING && phase != SolarStormPhase.ACTIVE)) {
			return;
		}
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		float t = (mc.level != null ? mc.level.getGameTime() : 0L) * 0.18f;
		float pulse = 0.5f + 0.5f * Mth.sin(t);
		float leak = Mth.clamp(ClientMarsState.getShelterLeakPercent() / 100.0f, 0.0f, 1.0f);

		if (phase == SolarStormPhase.WARNING) {
			int alpha = Math.round((12 + 16 * pulse) * (0.35f + 0.65f * leak));
			int col = (alpha << 24) | 0xFF8822;
			int band = 8;
			graphics.fill(0, 0, w, band, col);
			graphics.fill(0, h - band, w, h, col);
			graphics.fill(0, band, band, h - band, col);
			graphics.fill(w - band, band, w, h - band, col);
			return;
		}

		// ACTIVE CME Impact: intense blazing crimson-orange pulse scaled by exposure leakage!
		float strength = 0.25f + 0.75f * leak;
		int outerAlpha = Mth.clamp(Math.round((28 + 38 * pulse) * strength), 0, 140);
		int innerAlpha = Mth.clamp(Math.round((12 + 24 * pulse) * leak), 0, 80);
		int outerCol = (outerAlpha << 24) | 0xFF3B12;
		int innerCol = (innerAlpha << 24) | 0xFF6A1A;
		int band = 18;
		graphics.fill(0, 0, w, band, outerCol);
		graphics.fill(0, h - band, w, h, outerCol);
		graphics.fill(0, band, band, h - band, outerCol);
		graphics.fill(w - band, band, w, h - band, outerCol);
		if (innerAlpha > 2) {
			graphics.fill(0, 0, w, h, innerCol);
		}
	}
}

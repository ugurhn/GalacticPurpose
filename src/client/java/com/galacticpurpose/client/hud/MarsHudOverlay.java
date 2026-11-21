package com.galacticpurpose.client.hud;

import com.galacticpurpose.terraforming.MarsProgressSnapshot;
import com.galacticpurpose.terraforming.SolarStormPhase;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders live Mars Terraforming Index, milestone unlocks, Red Giant status, and CME storm timer.
 */
public final class MarsHudOverlay {
	private MarsHudOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null || !ClientMarsState.isActiveInDimension()) {
			return;
		}

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
			String.format("%.1f", snap.heatPercent()),
			String.format("%.1f", snap.pressurePercent()),
			String.format("%.1f", snap.oxygenPercent())
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
}

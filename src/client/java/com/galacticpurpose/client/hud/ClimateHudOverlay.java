package com.galacticpurpose.client.hud;

import com.galacticpurpose.climate.CollapsePhase;
import com.galacticpurpose.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Renders a compact environmental telemetry badge on the Overworld during approaching or active collapse.
 */
public final class ClimateHudOverlay {
	private ClimateHudOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null || !ClientClimateState.isActiveInDimension()) {
			return;
		}

		CollapsePhase phase = ClientClimateState.getPhase();
		long day = ClientClimateState.getDay();
		if (phase == CollapsePhase.STABLE && !ModConfig.get().showHudInStablePhase()) {
			return;
		}

		Component line1 = Component.translatable(
			"hud.galacticpurpose.climate_header",
			day,
			Component.translatable(phase.translationKey())
		);
		Component exposure = Component.translatable(
			ClientClimateState.isExposedToSky()
				? "hud.galacticpurpose.exposed"
				: "hud.galacticpurpose.sheltered"
		);
		Component line2 = Component.translatable(
			"hud.galacticpurpose.climate_detail",
			Math.round(ClientClimateState.getAmbientCelsius()),
			exposure
		);

		Font font = mc.font;
		int width = Math.max(font.width(line1), font.width(line2)) + 12;
		int x = 8;
		int y = 8;

		graphics.fill(x, y, x + width, y + 26, 0xB80B0F17);
		graphics.renderOutline(x, y, width, 26, 0xFF000000 | phase.accentRgb());
		graphics.drawString(font, line1, x + 6, y + 4, phase.accentRgb(), true);
		int detailColor = ClientClimateState.isExposedToSky() && phase.ordinal() >= CollapsePhase.CRITICAL.ordinal()
			? 0xFFD5D5
			: 0xE2E8F0;
		graphics.drawString(font, line2, x + 6, y + 15, detailColor, true);
	}
}

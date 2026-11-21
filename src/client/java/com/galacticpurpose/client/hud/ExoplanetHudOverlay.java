package com.galacticpurpose.client.hud;

import com.galacticpurpose.exoplanet.ExoplanetLandmarkBuilder;
import com.galacticpurpose.ship.StarshipFlightPhysics;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Renders exoplanet temperature, dynamic tidal gravity, Erebus elevation, suit status,
 * AND a live Landmark Signal Compass (name, distance, direction) in the top-left HUD!
 */
public final class ExoplanetHudOverlay {
	private ExoplanetHudOverlay() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null || !ClientExoplanetState.isActiveInDimension()) {
			return;
		}

		ExoplanetRoute route = ClientExoplanetState.route();
		boolean hazardous = ClientExoplanetState.isHazardous();
		int accent = hazardous ? 0xFF4D4D : route.accentRgb();

		float bhElev = ClientExoplanetState.blackHoleElevation();
		String bhIcon = bhElev > 0.25f ? "▲" : (bhElev < -0.25f ? "▼" : "•");

		boolean inShipCabin = mc.player.getVehicle() instanceof com.galacticpurpose.entity.InterstellarShipEntity;
		float shownG = inShipCabin ? 0.00f : ClientExoplanetState.effectiveG();
		String gSuffix = inShipCabin ? "ZERO-G" : bhIcon;
		Component line1 = Component.translatable(
			"hud.galacticpurpose.exoplanet_header",
			Component.translatable(route.translationKey()),
			String.format("%.2fg %s", shownG, gSuffix)
		);
		Component line2 = Component.translatable(
			"hud.galacticpurpose.exoplanet_detail",
			ClientExoplanetState.ambientCelsius(),
			ClientExoplanetState.pressureAtm(),
			Component.translatable(ClientExoplanetState.statusKey())
		);

		double dx = ExoplanetLandmarkBuilder.LANDMARK_X - mc.player.getX();
		double dz = ExoplanetLandmarkBuilder.LANDMARK_Z - mc.player.getZ();
		int dist = (int) Math.round(Math.hypot(dx, dz));
		String dir = StarshipFlightPhysics.computeCompassBearing(dx, dz);
		Component line3 = dist <= 14
			? Component.translatable("hud.galacticpurpose.landmark_at_site", Component.translatable(route.landmarkNameKey()))
			: Component.translatable("hud.galacticpurpose.landmark_signal", Component.translatable(route.landmarkNameKey()), dist, dir);

		Font font = mc.font;
		int width = Math.max(Math.max(font.width(line1), font.width(line2)), font.width(line3)) + 14;
		int x = 8;
		int y = 8;

		graphics.fill(x, y, x + width, y + 38, 0xC40B0F17);
		graphics.renderOutline(x, y, width, 38, 0xFF000000 | accent);
		graphics.drawString(font, line1, x + 6, y + 4, route.accentRgb(), true);
		graphics.drawString(font, line2, x + 6, y + 15, hazardous ? 0xFFD5D5 : 0xE2E8F0, true);
		graphics.drawString(font, line3, x + 6, y + 26, 0xFFFACC15, true);
	}
}

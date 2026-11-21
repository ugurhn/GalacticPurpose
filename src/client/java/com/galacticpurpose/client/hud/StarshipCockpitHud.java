package com.galacticpurpose.client.hud;

import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.entity.StarshipSeatEntity;
import com.galacticpurpose.network.StarshipActionPayload;
import com.galacticpurpose.ship.StarshipFlightPhysics;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Renders the Horizon Lander Flight Telemetry HUD, 10-second Launch Countdown Banner,
 * 95-second Guided Deep-Space Cruise Bridge HUD, AND a clear Right-Side Mission Flight Computer panel
 * explaining what is happening right now and what the pilot should do next!
 * Automatically switches camera to 3rd-person (TPS) while seated in the Horizon Lander and restores 1st-person on exit!
 */
public final class StarshipCockpitHud {
	private static KeyMapping keyShipAction;
	private static boolean prevKeySpace = false;
	private static boolean prevKeyG = false;
	private static boolean prevKeyI = false;
	private static long lastSpaceTapMs = 0L;
	private static boolean wasRidingShip = false;
	private static CameraType cameraBeforeBoarding = CameraType.FIRST_PERSON;

	private StarshipCockpitHud() {
	}

	public static void registerKeyBindings() {
		keyShipAction = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.galacticpurpose.ship_action",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			"key.categories.galacticpurpose"
		));
	}

	public static void tickInput(Minecraft mc) {
		if (mc.player == null) {
			wasRidingShip = false;
			prevKeySpace = false;
			prevKeyG = false;
			prevKeyI = false;
			return;
		}

		// Automatic 3rd-person camera switch on Horizon Lander boarding, restoring cleanly when dismounting or walking in cabin!
		boolean ridingShipNow = mc.player.getVehicle() instanceof InterstellarShipEntity;
		if (ridingShipNow && !wasRidingShip) {
			cameraBeforeBoarding = mc.options.getCameraType();
			if (cameraBeforeBoarding == CameraType.FIRST_PERSON) {
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
		} else if (!ridingShipNow && wasRidingShip) {
			mc.options.setCameraType(cameraBeforeBoarding);
		}
		wasRidingShip = ridingShipNow;

		if (mc.screen != null) {
			prevKeySpace = false;
			prevKeyG = false;
			prevKeyI = false;
			if (keyShipAction != null) {
				while (keyShipAction.consumeClick()) {}
			}
			return;
		}
		long win = mc.getWindow().getWindow();
		boolean keySpace = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_SPACE);
		boolean keyGDown = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_G);
		boolean keyI = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_I);

		boolean clickedG = false;
		if (keyShipAction != null) {
			while (keyShipAction.consumeClick()) {
				clickedG = true;
			}
		}
		if (keyGDown && !prevKeyG) {
			clickedG = true;
		}

		if (ClientOrbitalTransitState.inTransit()) {
			if (clickedG) {
				ClientPlayNetworking.send(new StarshipActionPayload(StarshipActionPayload.ACTION_ENGAGE_AUTO_LAND_OR_ENTER));
			}
		} else if (mc.player.getVehicle() instanceof InterstellarShipEntity ship) {
			if (keySpace && !prevKeySpace && ship.shipMode() == InterstellarShipEntity.MODE_PARKED) {
				long now = System.currentTimeMillis();
				if (now - lastSpaceTapMs <= 450L) {
					lastSpaceTapMs = 0L;
					ClientPlayNetworking.send(new StarshipActionPayload(StarshipActionPayload.ACTION_IGNITE_ORBITAL_LAUNCH));
				} else {
					lastSpaceTapMs = now;
				}
			}
			if (clickedG && ship.shipMode() == InterstellarShipEntity.MODE_PARKED) {
				lastSpaceTapMs = 0L;
				ClientPlayNetworking.send(new StarshipActionPayload(StarshipActionPayload.ACTION_IGNITE_ORBITAL_LAUNCH));
			}
			if (keyI && !prevKeyI) {
				ClientPlayNetworking.send(new StarshipActionPayload(StarshipActionPayload.ACTION_OPEN_CARGO));
			}
		}

		prevKeySpace = keySpace;
		prevKeyG = keyGDown;
		prevKeyI = keyI;
	}

	public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) {
			return;
		}
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();

		if (ClientOrbitalTransitState.inTransit()) {
			renderOrbitalBridgeHud(mc, g, sw);
			renderTransitFadeOverlay(g, sw, sh);
			return;
		}
		if (mc.player.getVehicle() instanceof InterstellarShipEntity ship) {
			renderCockpitHud(mc, g, ship, sw, sh);
		}
		renderTransitFadeOverlay(g, sw, sh);
	}

	private static void renderTransitFadeOverlay(GuiGraphics g, int sw, int sh) {
		if (ClientOrbitalTransitState.inTransit() && ClientOrbitalTransitState.reentryActive()) {
			float e = ClientOrbitalTransitState.entryProgress();
			boolean returning = ClientOrbitalTransitState.returning();
			float glowAlpha = Mth.clamp((e - 0.15f) / 0.55f, 0.0f, 1.0f) * 0.42f;
			if (glowAlpha > 0.01f) {
				int glowRgb = returning ? 0x38BDF8 : 0xF97316;
				int aGlow = Math.round(glowAlpha * 255.0f);
				g.fill(0, 0, sw, sh, (aGlow << 24) | glowRgb);
			}
			float darkAlpha = Mth.clamp((e - 0.50f) / 0.44f, 0.0f, 1.0f);
			if (darkAlpha > 0.01f) {
				int aDark = Math.round(darkAlpha * 255.0f);
				int darkRgb = returning ? 0x050916 : 0x090405;
				g.fill(0, 0, sw, sh, (aDark << 24) | darkRgb);
			}
			return;
		}
		float fadeOut = ClientOrbitalTransitState.postArrivalFadeOutAlpha();
		if (fadeOut > 0.01f) {
			int aDark = Math.round(fadeOut * 255.0f);
			int darkRgb = ClientOrbitalTransitState.arrivalFadeIsWarm() ? 0x090405 : 0x050916;
			g.fill(0, 0, sw, sh, (aDark << 24) | darkRgb);
		}
	}

	private static void renderOrbitalBridgeHud(Minecraft mc, GuiGraphics g, int sw) {
		boolean returning = ClientOrbitalTransitState.returning();
		boolean reentry = ClientOrbitalTransitState.reentryActive();
		float prog = ClientOrbitalTransitState.approachProgress();
		int phase = ClientOrbitalTransitState.cruisePhase();
		int remSec = ClientOrbitalTransitState.remainingSeconds();

		String dest = returning
			? Component.translatable("hud.galacticpurpose.origin_wormhole").getString()
			: Component.translatable(ClientOrbitalTransitState.route().translationKey()).getString();

		String distLabel;
		if (reentry) {
			float e = ClientOrbitalTransitState.entryProgress();
			if (returning) {
				float remWh = Math.max(0.0f, 0.05f * (1.0f - e));
				distLabel = String.format("%.3f km", remWh);
			} else {
				int remKm = Math.max(0, Math.round(120.0f * (1.0f - e)));
				distLabel = remKm + " km";
			}
		} else if (returning) {
			float whKm = StarshipFlightPhysics.computeWormholeDistanceKm(prog);
			distLabel = whKm >= 10.0f ? String.format("%,d km", Math.round(whKm)) : String.format("%.2f km", whKm);
		} else {
			distLabel = String.format("%,d km", StarshipFlightPhysics.computePlanetDistanceKm(prog, true));
		}

		boolean arrived = ClientOrbitalTransitState.arrived();
		boolean seatedInChair = mc.player != null && mc.player.getVehicle() instanceof StarshipSeatEntity;
		String phaseKey = returning
			? "hud.galacticpurpose.cruise_return_phase_" + phase
			: "hud.galacticpurpose.cruise_outbound_phase_" + phase;
		String phaseLabel = Component.translatable(phaseKey).getString();

		String line1 = phaseLabel + " -> " + dest + " · " + distLabel
			+ (remSec > 0 ? " (" + remSec + "s)" : (returning ? " (APERTURE)" : " (120 km ORBIT)"));
		String line2 = reentry
			? Component.translatable(returning
				? "hud.galacticpurpose.orbital_entering_banner"
				: "hud.galacticpurpose.orbital_reentry_banner").getString()
			: (arrived
				? Component.translatable(returning
					? "hud.galacticpurpose.orbital_bridge_hint_return"
					: "hud.galacticpurpose.orbital_bridge_hint_land").getString()
				: Component.translatable("hud.galacticpurpose.orbital_wait_hint", remSec).getString());

		int w = Math.max(310, Math.max(mc.font.width(line1), mc.font.width(line2)) + 24);
		int x = (sw - w) / 2;
		int y = 6;
		int h = 42;

		g.fill(x, y, x + w, y + h, 0xD2080E1A);
		g.renderOutline(x, y, w, h, reentry ? 0xFFF97316 : (arrived ? 0xFF22C55E : 0xFF38BDF8));
		g.drawCenteredString(mc.font, line1, sw / 2, y + 5, 0xFF7DD3FC);

		int barX = x + 12;
		int barY = y + 17;
		int barW = w - 24;
		g.fill(barX, barY, barX + barW, barY + 5, 0xFF172338);
		int fillW = Math.round(barW * Mth.clamp(prog, 0.0f, 1.0f));
		g.fill(barX, barY, barX + fillW, barY + 5, reentry ? 0xFFF97316 : (arrived ? 0xFF22C55E : 0xFF38BDF8));

		g.drawCenteredString(mc.font, line2, sw / 2, y + 27,
			reentry ? 0xFFF97316 : (arrived ? 0xFF4ADE80 : 0xFFFACC15));

		// Clear Right-Side Mission Flight Computer Guidance Card!
		String statusDescKey = reentry
			? (returning ? "hud.galacticpurpose.fc_transit_entering" : "hud.galacticpurpose.fc_transit_reentry")
			: (returning ? "hud.galacticpurpose.fc_transit_return_desc" : "hud.galacticpurpose.fc_transit_outbound_desc");
		String actionTxt = reentry
			? Component.translatable("hud.galacticpurpose.fc_action_hold").getString()
			: (arrived
				? Component.translatable(returning
					? "hud.galacticpurpose.fc_action_enter_wormhole"
					: "hud.galacticpurpose.fc_action_engage_descent").getString()
				: Component.translatable("hud.galacticpurpose.fc_action_cruise_locked", remSec).getString());
		renderFlightComputerCard(
			mc, g, sw,
			Component.translatable(statusDescKey).getString(),
			new String[]{
				Component.translatable("hud.galacticpurpose.fc_stat_target", dest).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_distance", distLabel).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_eta", remSec > 0 ? (remSec + "s") : "READY").getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_gravity_zero").getString(),
				Component.translatable(seatedInChair
					? "hud.galacticpurpose.fc_stat_chair_locked"
					: "hud.galacticpurpose.fc_stat_chair_hint").getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_fuel", ClientOrbitalTransitState.fuelUnits()).getString()
			},
			actionTxt,
			reentry ? 0xFFF97316 : (arrived ? 0xFF22C55E : 0xFF38BDF8)
		);
	}

	private static void renderCockpitHud(Minecraft mc, GuiGraphics g, InterstellarShipEntity ship, int sw, int sh) {
		int mode = ship.shipMode();

		if (mode == InterstellarShipEntity.MODE_ORBITAL_ASCENT) {
			StarshipFlightPhysics.AscentReading reading = StarshipFlightPhysics.computeAscentProfile(ship.syncedAscentTicks());
			int darkAlpha = Math.round(reading.skyDarkness() * 125.0f);
			if (darkAlpha > 2) {
				g.fill(0, 0, sw, sh / 2, (darkAlpha << 24) | 0x02040C);
			}
		}

		WormholeLinkCalculator.LinkStatus link = ship.syncedLinkStatus();
		boolean signalLost = link == WormholeLinkCalculator.LinkStatus.CLOSED
			|| link == WormholeLinkCalculator.LinkStatus.DESTROYED;

		String modeStr;
		String line2;
		float progressBar = -1.0f;
		int barColor = 0xFF38BDF8;
		String fcDesc;
		String fcAction;
		String[] fcStats;

		double spdMs = ship.getDeltaMovement().length() * 20.0;

		if (mode == InterstellarShipEntity.MODE_COUNTDOWN) {
			int sec = StarshipFlightPhysics.countdownSecondsRemaining(ship.syncedCountdownTicks());
			progressBar = Mth.clamp(ship.syncedCountdownTicks() / (float) StarshipFlightPhysics.COUNTDOWN_TICKS, 0.0f, 1.0f);
			barColor = sec <= 3 ? 0xFFF97316 : 0xFFFACC15;
			modeStr = Component.translatable("hud.galacticpurpose.mode_countdown", sec).getString();
			line2 = Component.translatable("hud.galacticpurpose.countdown_spool_hint").getString();
			renderCenterCountdownOverlay(mc, g, sw, sh, sec, progressBar);
			fcDesc = Component.translatable("hud.galacticpurpose.fc_ship_countdown_desc").getString();
			fcAction = Component.translatable("hud.galacticpurpose.fc_action_countdown", sec).getString();
			fcStats = new String[]{
				Component.translatable("hud.galacticpurpose.fc_stat_spool", Math.round(progressBar * 100.0f)).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_fuel", ship.fuelUnits()).getString()
			};
		} else if (mode == InterstellarShipEntity.MODE_RUNWAY_LAUNCH) {
			modeStr = Component.translatable("hud.galacticpurpose.mode_runway_launch").getString();
			line2 = Component.translatable("hud.galacticpurpose.runway_intercept_hint").getString();
			fcDesc = Component.translatable("hud.galacticpurpose.fc_ship_runway_desc").getString();
			fcAction = Component.translatable("hud.galacticpurpose.fc_action_hold").getString();
			fcStats = new String[]{
				Component.translatable("hud.galacticpurpose.fc_stat_speed", String.format("%.0f m/s", spdMs)).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_pitch", Math.round(ship.pitchDeg())).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_fuel", ship.fuelUnits()).getString()
			};
		} else if (mode == InterstellarShipEntity.MODE_ORBITAL_ASCENT) {
			StarshipFlightPhysics.AscentReading reading = StarshipFlightPhysics.computeAscentProfile(ship.syncedAscentTicks());
			progressBar = Mth.clamp(ship.syncedAscentTicks() / (float) StarshipFlightPhysics.ASCENT_DURATION_TICKS, 0.0f, 1.0f);
			barColor = reading.stageIndex() == 2 ? 0xFFA855F7 : 0xFFF97316;
			String stageTxt = Component.translatable("hud.galacticpurpose.ascent_stage_" + reading.stageIndex()).getString();
			int remSec = Math.max(0, (StarshipFlightPhysics.ASCENT_DURATION_TICKS - ship.syncedAscentTicks()) / 20);
			modeStr = stageTxt + String.format(" · %.1f / 120 km · +%d°", reading.simulatedAltitudeKm(), Math.round(ship.pitchDeg()));
			line2 = Component.translatable("hud.galacticpurpose.ascent_telemetry_hint", remSec).getString();
			fcDesc = Component.translatable("hud.galacticpurpose.fc_ship_ascent_desc").getString();
			fcAction = Component.translatable("hud.galacticpurpose.fc_action_ascent", remSec).getString();
			fcStats = new String[]{
				Component.translatable("hud.galacticpurpose.fc_stat_alt", String.format("%.1f km", reading.simulatedAltitudeKm())).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_speed", String.format("%.0f m/s", reading.forwardSpeed() * 20.0)).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_pitch", Math.round(ship.pitchDeg())).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_fuel", ship.fuelUnits()).getString()
			};
		} else if (mode == InterstellarShipEntity.MODE_AUTO_DESCENT) {
			modeStr = Component.translatable("hud.galacticpurpose.mode_auto_descent").getString() + " · " + Math.round(ship.groundAltitude()) + "m AGL";
			line2 = Component.translatable("hud.galacticpurpose.descent_telemetry_hint").getString();
			fcDesc = Component.translatable("hud.galacticpurpose.fc_ship_descent_desc").getString();
			fcAction = Component.translatable("hud.galacticpurpose.fc_action_hold").getString();
			fcStats = new String[]{
				Component.translatable("hud.galacticpurpose.fc_stat_agl", Math.round(ship.groundAltitude())).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_skids", ship.legDeploy() > 0.2f ? "DEPLOYING" : "STOWED").getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_fuel", ship.fuelUnits()).getString()
			};
		} else if (mode == InterstellarShipEntity.MODE_RELOCATING) {
			modeStr = Component.translatable("hud.galacticpurpose.mode_relocating").getString() + " · " + Math.round(ship.groundAltitude()) + "m AGL";
			line2 = Component.translatable("hud.galacticpurpose.relocating_telemetry_hint").getString();
			fcDesc = Component.translatable("hud.galacticpurpose.fc_ship_relocating_desc").getString();
			fcAction = Component.translatable("hud.galacticpurpose.fc_action_hold").getString();
			fcStats = new String[]{
				Component.translatable("hud.galacticpurpose.fc_stat_speed", String.format("%.0f m/s", spdMs)).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_agl", Math.round(ship.groundAltitude())).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_fuel", ship.fuelUnits()).getString()
			};
		} else {
			modeStr = Component.translatable("hud.galacticpurpose.mode_parked").getString()
				+ " · Y:" + Mth.floor(ship.getY()) + " · Fuel:" + ship.fuelUnits();
			boolean awaitingSecondTap = (System.currentTimeMillis() - lastSpaceTapMs) <= 450L;
			line2 = awaitingSecondTap
				? Component.translatable("hud.galacticpurpose.confirm_space_ignition").getString()
				: Component.translatable("hud.galacticpurpose.cockpit_controls").getString();
			fcDesc = Component.translatable("hud.galacticpurpose.fc_ship_parked_desc").getString();
			fcAction = Component.translatable("hud.galacticpurpose.fc_action_launch").getString();
			fcStats = new String[]{
				Component.translatable("hud.galacticpurpose.fc_stat_fuel", ship.fuelUnits()).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_link", link.name()).getString(),
				Component.translatable("hud.galacticpurpose.fc_stat_gravity_zero").getString()
			};
		}

		if (mode != InterstellarShipEntity.MODE_PARKED) {
			renderVerticalOrbitGauge(mc, g, ship, sw, sh);
		}

		String linkStr = switch (link) {
			case OPEN -> "LINK: OPEN";
			case CLOSED, DESTROYED -> "SIGNAL LOST!";
			case UNLINKED -> "LINK: LOCAL";
		};
		int linkColor = link == WormholeLinkCalculator.LinkStatus.OPEN ? 0xFF22C55E
			: (signalLost ? 0xFFEF4444 : 0xFF94A3B8);

		int topLineW = mc.font.width(modeStr) + mc.font.width(linkStr) + 28;
		int boxW = Math.max(272, Math.max(topLineW, mc.font.width(line2) + 20));
		int boxH = progressBar >= 0.0f ? 44 : 38;
		int x = (sw - boxW) / 2;
		int y = 8;

		g.fill(x, y, x + boxW, y + boxH, 0xD2080E1A);
		g.renderOutline(x, y, boxW, boxH, signalLost ? 0xFFEF4444 : 0xFF38BDF8);
		g.drawString(mc.font, modeStr, x + 8, y + 5, 0xFF7DD3FC, false);
		g.drawString(mc.font, linkStr, x + boxW - mc.font.width(linkStr) - 8, y + 5, linkColor, false);

		if (progressBar >= 0.0f) {
			int barX = x + 10;
			int barY = y + 17;
			int barW = boxW - 20;
			g.fill(barX, barY, barX + barW, barY + 5, 0xFF172338);
			g.fill(barX, barY, barX + Math.round(barW * progressBar), barY + 5, barColor);
			g.drawCenteredString(mc.font, line2, sw / 2, y + 27, 0xFFFACC15);
		} else {
			g.drawCenteredString(mc.font, line2, sw / 2, y + 21, 0xFFFACC15);
		}

		renderFlightComputerCard(mc, g, sw, fcDesc, fcStats, fcAction, signalLost ? 0xFFEF4444 : barColor);
	}

	/**
	 * Compact Right-Side Mission Flight Computer Panel giving clear plain-language status, live telemetry, and next action!
	 */
	private static void renderFlightComputerCard(
		Minecraft mc, GuiGraphics g, int sw,
		String statusDesc, String[] stats, String nextAction, int accentColor
	) {
		int cardW = 166;
		int x = sw - cardW - 8;
		int y = 56;
		int innerW = cardW - 12;

		List<FormattedCharSequence> descLines = mc.font.split(Component.literal(statusDesc), innerW);
		List<FormattedCharSequence> actLines = mc.font.split(Component.literal(nextAction), innerW);

		int descH = Math.min(descLines.size(), 4) * 9;
		int statsH = stats.length * 10;
		int actH = Math.min(actLines.size(), 3) * 9;
		int cardH = 18 + descH + 6 + statsH + 8 + actH + 8;

		g.fill(x, y, x + cardW, y + cardH, 0xC8070D18);
		g.renderOutline(x, y, cardW, cardH, accentColor);
		g.fill(x + 1, y + 1, x + cardW - 1, y + 14, 0xFF101C30);
		g.drawString(mc.font, Component.translatable("hud.galacticpurpose.fc_header"), x + 6, y + 3, accentColor, true);

		int cy = y + 18;
		for (int i = 0; i < Math.min(descLines.size(), 4); i++) {
			g.drawString(mc.font, descLines.get(i), x + 6, cy, 0xFFE2E8F0, false);
			cy += 9;
		}
		cy += 4;
		g.fill(x + 6, cy, x + cardW - 6, cy + 1, 0xFF1E3A5F);
		cy += 4;
		for (String st : stats) {
			g.drawString(mc.font, st, x + 6, cy, 0xFF7DD3FC, false);
			cy += 10;
		}
		cy += 3;
		int actBoxTop = cy;
		g.fill(x + 4, actBoxTop, x + cardW - 4, actBoxTop + actH + 6, 0x9913261E);
		g.fill(x + 4, actBoxTop, x + 6, actBoxTop + actH + 6, 0xFF22C55E);
		cy += 3;
		for (int i = 0; i < Math.min(actLines.size(), 3); i++) {
			g.drawString(mc.font, actLines.get(i), x + 9, cy, 0xFF4ADE80, false);
			cy += 9;
		}
	}

	private static void renderCenterCountdownOverlay(Minecraft mc, GuiGraphics g, int sw, int sh, int sec, float progress) {
		String bigText = sec > 0 ? ("T - " + sec) : "IGNITION!";
		int color = sec <= 3 ? 0xFFF97316 : 0xFFFACC15;
		int cy = sh / 2 - 42;
		g.pose().pushPose();
		g.pose().translate(sw / 2.0f, cy, 0.0f);
		g.pose().scale(2.4f, 2.4f, 1.0f);
		g.drawCenteredString(mc.font, bigText, 0, 0, color);
		g.pose().popPose();

		String sub = Component.translatable("hud.galacticpurpose.countdown_sub", Math.round(progress * 100.0f)).getString();
		g.drawCenteredString(mc.font, sub, sw / 2, cy + 24, 0xFFE2E8F0);
	}

	/**
	 * Galacticraft-inspired Vertical Ascent / Descent Trajectory Gauge:
	 * Shows the shaded planet orb below, atmospheric band, 120 km orbital wormhole gate at top,
	 * and a gliding Horizon Lander sprite rising/descending smoothly with live altitude & thruster fire.
	 */
	private static void renderVerticalOrbitGauge(Minecraft mc, GuiGraphics g, InterstellarShipEntity ship, int sw, int sh) {
		int gx = 10;
		int gh = 148;
		int gy = Math.max(54, (sh - gh) / 2);
		int gw = 48;

		g.fill(gx, gy, gx + gw, gy + gh, 0xCC070E1C);
		g.renderOutline(gx, gy, gw, gh, 0xFF38BDF8);

		int cx = gx + gw / 2;
		int topNodeY = gy + 15;
		int planetCenterY = gy + gh - 19;
		int trackTop = topNodeY + 9;
		int trackBottom = planetCenterY - 11;
		int trackH = trackBottom - trackTop;

		// Top 120 km Wormhole Orbit Gate badge
		g.fill(cx - 5, topNodeY - 5, cx + 6, topNodeY + 6, 0xFF0EA5E9);
		g.fill(cx - 3, topNodeY - 3, cx + 4, topNodeY + 4, 0xFFBAE6FD);
		g.fill(cx - 1, topNodeY - 1, cx + 2, topNodeY + 2, 0xFFFFFFFF);
		g.drawCenteredString(mc.font, "120km", cx, gy + 3, 0xFF7DD3FC);

		// Vertical trajectory track with atmospheric gradient (sky cyan bottom -> vacuum dark top)
		int atmoSplit = trackTop + (trackH * 55) / 100;
		g.fill(cx - 1, trackTop, cx + 2, atmoSplit, 0xFF1E293B);
		g.fill(cx - 1, atmoSplit, cx + 2, trackBottom, 0xFF0284C7);
		for (int i = 0; i <= 4; i++) {
			int ty = trackTop + (trackH * i) / 4;
			g.fill(cx - 5, ty, cx + 6, ty + 1, 0x8838BDF8);
		}

		// Bottom shaded mini planet orb (tinted by current world dimension)
		String dimPath = mc.level != null ? mc.level.dimension().location().getPath() : "overworld";
		int planetRim = 0xFF38BDF8;
		int planetBody = 0xFF15803D;
		int planetHighlight = 0xFF22C55E;
		if (dimPath.contains("hephaestus")) {
			planetRim = 0xFFF97316;
			planetBody = 0xFF991B1B;
			planetHighlight = 0xFFEA580C;
		} else if (dimPath.contains("pelagia")) {
			planetRim = 0xFF38BDF8;
			planetBody = 0xFF0369A1;
			planetHighlight = 0xFF0EA5E9;
		} else if (dimPath.contains("nyx")) {
			planetRim = 0xFFA855F7;
			planetBody = 0xFF312E81;
			planetHighlight = 0xFF6366F1;
		}
		g.fill(cx - 7, planetCenterY - 6, cx + 8, planetCenterY + 7, planetRim);
		g.fill(cx - 6, planetCenterY - 7, cx + 7, planetCenterY + 8, planetRim);
		g.fill(cx - 6, planetCenterY - 5, cx + 7, planetCenterY + 6, planetBody);
		g.fill(cx - 5, planetCenterY - 6, cx + 6, planetCenterY + 7, planetBody);
		g.fill(cx - 4, planetCenterY - 5, cx + 2, planetCenterY + 1, planetHighlight);

		// Compute normalized vertical rail position: 0 = surface, 1 = orbital exit / upper entry
		int mode = ship.shipMode();
		float progress;
		if (mode == InterstellarShipEntity.MODE_AUTO_DESCENT) {
			progress = Mth.clamp(ship.groundAltitude() / 56.0f, 0.0f, 1.0f);
		} else if (mode == InterstellarShipEntity.MODE_ORBITAL_ASCENT) {
			progress = Mth.clamp(ship.syncedAscentTicks() / (float) StarshipFlightPhysics.ASCENT_DURATION_TICKS, 0.0f, 1.0f);
		} else {
			progress = 0.02f;
		}
		int shipY = trackBottom - Math.round(progress * trackH);

		// Mini delta Horizon Lander sprite moving along the gauge + thruster flare
		g.fill(cx - 1, shipY - 4, cx + 2, shipY + 3, 0xFFF8FAFC);
		g.fill(cx - 3, shipY - 1, cx + 4, shipY + 3, 0xFFCBD5E1);
		g.fill(cx - 5, shipY + 1, cx + 6, shipY + 3, 0xFF38BDF8);
		g.fill(cx - 1, shipY - 3, cx + 2, shipY - 1, 0xFF0284C7);
		if (ship.thrustVisual() > 0.05f && (mc.gui.getGuiTicks() % 2 == 0 || true)) {
			int flameColor = mode == InterstellarShipEntity.MODE_AUTO_DESCENT ? 0xFFFACC15 : 0xFFF97316;
			g.fill(cx - 2, shipY + 3, cx + 3, shipY + 6, flameColor);
			g.fill(cx - 1, shipY + 6, cx + 2, shipY + 8, 0xFFFFFFFF);
		}

		String pctLabel = mode == InterstellarShipEntity.MODE_AUTO_DESCENT
			? Math.round(ship.groundAltitude()) + "m"
			: Math.round(progress * 100.0f) + "%";
		g.drawCenteredString(mc.font, pctLabel, cx, gy + gh - 10, 0xFFE2E8F0);
	}
}

package com.galacticpurpose.climate;

import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.config.ModConfig;
import com.galacticpurpose.network.ClimateSyncPayload;
import com.galacticpurpose.registry.ModDamageTypes;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/**
 * Applies periodic Overworld solar collapse hazards and synchronizes HUD telemetry.
 */
public final class OverworldHazardTicker {
	private static final long TICKS_PER_DAY = 24000L;

	private OverworldHazardTicker() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(OverworldHazardTicker::onWorldTick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			syncPlayer(handler.getPlayer()));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
			syncPlayer(player));
	}

	private static void onWorldTick(ServerLevel level) {
		if (!level.dimension().equals(Level.OVERWORLD)) {
			return;
		}
		ModConfig config = ModConfig.get();
		if (level.getGameTime() % config.hazardCheckIntervalTicks() != 0L) {
			return;
		}

		long dayTime = level.getDayTime();
		long day = Math.max(0L, dayTime / TICKS_PER_DAY);
		long timeOfDay = Math.floorMod(dayTime, TICKS_PER_DAY);

		CollapsePhase currentPhase = SolarInstabilityCalculator.determinePhase(day, config);
		announcePhaseChangeIfNeeded(level, currentPhase);

		for (ServerPlayer player : level.players()) {
			ClimateSnapshot snapshot = evaluateForPlayer(level, player, day, timeOfDay, config);
			applyHazardsIfVulnerable(level, player, snapshot);
			sendSnapshot(player, day, isExposedToSky(level, player), snapshot);
		}
	}

	public static void syncPlayer(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		if (!level.dimension().equals(Level.OVERWORLD)) {
			ServerPlayNetworking.send(player, ClimateSyncPayload.inactive());
			return;
		}
		ModConfig config = ModConfig.get();
		long dayTime = level.getDayTime();
		long day = Math.max(0L, dayTime / TICKS_PER_DAY);
		long timeOfDay = Math.floorMod(dayTime, TICKS_PER_DAY);
		ClimateSnapshot snapshot = evaluateForPlayer(level, player, day, timeOfDay, config);
		sendSnapshot(player, day, isExposedToSky(level, player), snapshot);
	}

	private static ClimateSnapshot evaluateForPlayer(
		ServerLevel level,
		ServerPlayer player,
		long day,
		long timeOfDay,
		ModConfig config
	) {
		boolean exposed = isExposedToSky(level, player);
		boolean inWater = player.isInWaterRainOrBubble();
		boolean thermalShield = SpaceCompatManager.get().hasThermalShield(player);
		return SolarInstabilityCalculator.evaluate(day, timeOfDay, exposed, inWater, thermalShield, config);
	}

	private static boolean isExposedToSky(ServerLevel level, ServerPlayer player) {
		BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
		return isExposedToSky(level, eyePos);
	}

	public static boolean isExposedToSky(Level level, BlockPos eyePos) {
		if (!level.canSeeSky(eyePos)) {
			return false;
		}
		int maxCheckY = Math.min(level.getMaxBuildHeight() - 1, eyePos.getY() + 24);
		for (int y = eyePos.getY() + 1; y <= maxCheckY; y++) {
			BlockPos check = new BlockPos(eyePos.getX(), y, eyePos.getZ());
			if (level.getBlockState(check).isSolidRender(level, check)) {
				return false;
			}
		}
		return true;
	}

	private static void applyHazardsIfVulnerable(
		ServerLevel level,
		ServerPlayer player,
		ClimateSnapshot snapshot
	) {
		if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
			return;
		}

		if (snapshot.heatDamage() > 0.0f) {
			player.hurt(level.damageSources().source(ModDamageTypes.SOLAR_RADIATION), snapshot.heatDamage());
			if (snapshot.applyIgnition()) {
				player.igniteForSeconds(3.0f);
			}
		}

		if (snapshot.coldDamage() > 0.0f) {
			int freezeTarget = Math.min(player.getTicksRequiredToFreeze() + 40, player.getTicksFrozen() + 60);
			player.setTicksFrozen(freezeTarget);
			player.hurt(level.damageSources().source(ModDamageTypes.HYPOTHERMIA), snapshot.coldDamage());
			if (snapshot.applyHypothermiaSlowness()) {
				int amplifier = snapshot.phase() == CollapsePhase.TERMINAL ? 1 : 0;
				player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, amplifier, true, false, true));
			}
		}
	}

	private static void sendSnapshot(
		ServerPlayer player,
		long day,
		boolean exposedToSky,
		ClimateSnapshot snapshot
	) {
		ServerPlayNetworking.send(player, new ClimateSyncPayload(
			true,
			snapshot.phase().ordinal(),
			day,
			snapshot.severity(),
			snapshot.ambientCelsius(),
			exposedToSky
		));
	}

	private static void announcePhaseChangeIfNeeded(ServerLevel overworld, CollapsePhase currentPhase) {
		EarthCollapseState state = EarthCollapseState.get(overworld);
		if (currentPhase.ordinal() <= state.getLastAnnouncedPhase().ordinal()) {
			return;
		}
		state.setLastAnnouncedPhase(currentPhase);

		String key = switch (currentPhase) {
			case WARNING -> "message.galacticpurpose.phase_warning";
			case CRITICAL -> "message.galacticpurpose.phase_critical";
			case TERMINAL -> "message.galacticpurpose.phase_terminal";
			default -> null;
		};
		if (key == null) {
			return;
		}

		MinecraftServer server = overworld.getServer();
		Component message = Component.translatable(key).withStyle(
			currentPhase == CollapsePhase.WARNING ? ChatFormatting.GOLD : ChatFormatting.RED,
			ChatFormatting.BOLD
		);
		server.getPlayerList().broadcastSystemMessage(message, false);
	}
}

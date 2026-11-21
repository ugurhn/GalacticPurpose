package com.galacticpurpose.exoplanet;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.climate.OverworldHazardTicker;
import com.galacticpurpose.config.ModConfig;
import com.galacticpurpose.item.PressureArmorItem;
import com.galacticpurpose.item.ThermalArmorItem;
import com.galacticpurpose.network.ExoplanetSyncPayload;
import com.galacticpurpose.registry.ModDamageTypes;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Evaluates exoplanet hazards, dynamic black-hole tidal gravity, Pressure Suit diving buffs, and atmospheric particles.
 */
public final class ExoplanetHazardTicker {
	public static final ResourceLocation GRAVITY_MODIFIER_ID = GalacticPurpose.id("exoplanet_gravity");
	public static final ResourceLocation SAFE_FALL_MODIFIER_ID = GalacticPurpose.id("exoplanet_safe_fall");
	public static final ResourceLocation FALL_DAMAGE_MODIFIER_ID = GalacticPurpose.id("exoplanet_fall_damage");
	private static final Set<String> DISCOVERED_LANDMARKS = new HashSet<>();

	private ExoplanetHazardTicker() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(ExoplanetHazardTicker::onWorldTick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			syncPlayer(handler.getPlayer()));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
			syncPlayer(player));
	}

	private static void onWorldTick(ServerLevel level) {
		if (level.getGameTime() % ModConfig.get().hazardCheckIntervalTicks() != 0L) {
			return;
		}

		ResourceLocation dimId = level.dimension().location();
		ExoplanetRoute route = ExoplanetRoute.fromDimensionId(dimId);

		for (ServerPlayer player : level.players()) {
			if (player.isUnderWater() && PressureArmorItem.hasFullSuit(player)) {
				applyPressureSuitDivingBuffs(player);
			}
			if (route == null) {
				if (dimId.equals(com.galacticpurpose.ship.OrbitalTransitManager.ORBITAL_TRANSIT_KEY.location())) {
					AttributeInstance gAttr = player.getAttribute(Attributes.GRAVITY);
					if (gAttr != null) {
						gAttr.removeModifier(GRAVITY_MODIFIER_ID);
						gAttr.addTransientModifier(new AttributeModifier(
							GRAVITY_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
						));
					}
					player.resetFallDistance();
				} else {
					clearExoplanetGravity(player);
				}
				continue;
			}

			ExoplanetGravityCalculator.GravityReading gravity =
				ExoplanetGravityCalculator.evaluate(route, level.getDayTime());
			applyExoplanetGravity(player, gravity.effectiveG());

			ExoplanetHazardSnapshot snapshot = evaluatePlayer(level, player, dimId);
			applyHazards(level, player, snapshot);
			spawnAtmosphereParticles(level, player, route);
			checkLandmarkDiscovery(player, route);
			sendSnapshot(player, snapshot, gravity);
		}
	}

	private static void checkLandmarkDiscovery(ServerPlayer player, ExoplanetRoute route) {
		double dx = player.getX() - ExoplanetLandmarkBuilder.LANDMARK_X;
		double dz = player.getZ() - ExoplanetLandmarkBuilder.LANDMARK_Z;
		if (dx * dx + dz * dz > 18.0 * 18.0) {
			return;
		}
		String key = player.getUUID() + ":" + route.id();
		if (!DISCOVERED_LANDMARKS.add(key)) {
			return;
		}
		Component title = Component.translatable(route.landmarkNameKey())
			.withStyle(style -> style.withColor(route.accentRgb()).withBold(true));
		Component subtitle = Component.translatable(route.landmarkHintKey())
			.withStyle(ChatFormatting.GOLD);
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 75, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.displayClientMessage(
			Component.translatable("message.galacticpurpose.landmark_discovered",
				Component.translatable(route.landmarkNameKey()),
				Component.translatable(route.landmarkHintKey())
			).withStyle(ChatFormatting.AQUA),
			false
		);
	}

	public static void syncPlayer(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		ResourceLocation dimId = level.dimension().location();
		ExoplanetRoute route = ExoplanetRoute.fromDimensionId(dimId);
		if (route == null) {
			if (dimId.equals(com.galacticpurpose.ship.OrbitalTransitManager.ORBITAL_TRANSIT_KEY.location())) {
				AttributeInstance gAttr = player.getAttribute(Attributes.GRAVITY);
				if (gAttr != null) {
					gAttr.removeModifier(GRAVITY_MODIFIER_ID);
					gAttr.addTransientModifier(new AttributeModifier(
						GRAVITY_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
					));
				}
				player.resetFallDistance();
			} else {
				clearExoplanetGravity(player);
			}
			ServerPlayNetworking.send(player, ExoplanetSyncPayload.inactive());
			return;
		}
		ExoplanetGravityCalculator.GravityReading gravity =
			ExoplanetGravityCalculator.evaluate(route, level.getDayTime());
		applyExoplanetGravity(player, gravity.effectiveG());
		ExoplanetHazardSnapshot snapshot = evaluatePlayer(level, player, dimId);
		sendSnapshot(player, snapshot, gravity);
	}

	public static void applyExoplanetGravity(Player player, float effectiveG) {
		if (player.getVehicle() instanceof com.galacticpurpose.entity.InterstellarShipEntity) {
			clearExoplanetGravity(player);
			return;
		}
		AttributeInstance gravityAttr = player.getAttribute(Attributes.GRAVITY);
		if (gravityAttr != null) {
			gravityAttr.removeModifier(GRAVITY_MODIFIER_ID);
			gravityAttr.addTransientModifier(new AttributeModifier(
				GRAVITY_MODIFIER_ID,
				effectiveG - 1.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE
			));
		}

		AttributeInstance safeFallAttr = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
		if (safeFallAttr != null) {
			safeFallAttr.removeModifier(SAFE_FALL_MODIFIER_ID);
			double safeFallDelta = (3.0 / Math.max(0.25, effectiveG)) - 3.0;
			safeFallAttr.addTransientModifier(new AttributeModifier(
				SAFE_FALL_MODIFIER_ID,
				safeFallDelta,
				AttributeModifier.Operation.ADD_VALUE
			));
		}

		AttributeInstance fallDmgAttr = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
		if (fallDmgAttr != null) {
			fallDmgAttr.removeModifier(FALL_DAMAGE_MODIFIER_ID);
			fallDmgAttr.addTransientModifier(new AttributeModifier(
				FALL_DAMAGE_MODIFIER_ID,
				effectiveG - 1.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE
			));
		}
	}

	public static void clearExoplanetGravity(Player player) {
		AttributeInstance gravityAttr = player.getAttribute(Attributes.GRAVITY);
		if (gravityAttr != null && gravityAttr.hasModifier(GRAVITY_MODIFIER_ID)) {
			gravityAttr.removeModifier(GRAVITY_MODIFIER_ID);
		}
		AttributeInstance safeFallAttr = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
		if (safeFallAttr != null && safeFallAttr.hasModifier(SAFE_FALL_MODIFIER_ID)) {
			safeFallAttr.removeModifier(SAFE_FALL_MODIFIER_ID);
		}
		AttributeInstance fallDmgAttr = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
		if (fallDmgAttr != null && fallDmgAttr.hasModifier(FALL_DAMAGE_MODIFIER_ID)) {
			fallDmgAttr.removeModifier(FALL_DAMAGE_MODIFIER_ID);
		}
	}

	private static ExoplanetHazardSnapshot evaluatePlayer(
		ServerLevel level,
		ServerPlayer player,
		ResourceLocation dimId
	) {
		boolean exposed = OverworldHazardTicker.isExposedToSky(level, player.blockPosition().above());
		boolean underwater = player.isUnderWater();
		boolean thermalSuit = ThermalArmorItem.hasFullSuit(player);
		boolean pressureSuit = PressureArmorItem.hasFullSuit(player);
		return ExoplanetHazardCalculator.evaluate(
			dimId,
			player.blockPosition().getY(),
			exposed,
			underwater,
			thermalSuit,
			pressureSuit
		);
	}

	private static void applyPressureSuitDivingBuffs(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 60, 0, true, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
	}

	private static void applyHazards(ServerLevel level, ServerPlayer player, ExoplanetHazardSnapshot snapshot) {
		if (snapshot == null || !snapshot.hazardous() || player.isCreative() || player.isSpectator() || !player.isAlive()) {
			return;
		}

		if (snapshot.heatDamage() > 0.0f) {
			player.hurt(level.damageSources().source(ModDamageTypes.EXTREME_HEAT), snapshot.heatDamage());
			player.igniteForSeconds(2.0f);
		}

		if (snapshot.pressureDamage() > 0.0f) {
			player.hurt(level.damageSources().source(ModDamageTypes.HYDROSTATIC_PRESSURE), snapshot.pressureDamage());
			player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 1, true, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false, true));
		}

		if (snapshot.coldDamage() > 0.0f) {
			int freezeTarget = Math.min(player.getTicksRequiredToFreeze() + 40, player.getTicksFrozen() + 65);
			player.setTicksFrozen(freezeTarget);
			player.hurt(level.damageSources().source(ModDamageTypes.CRYO_VOID), snapshot.coldDamage());
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false, true));
		}
	}

	private static void spawnAtmosphereParticles(ServerLevel level, ServerPlayer player, ExoplanetRoute route) {
		if (route == ExoplanetRoute.HEPHAESTUS_IV) {
			level.sendParticles(
				player, ParticleTypes.ASH, false,
				player.getX(), player.getY() + 3.0, player.getZ(),
				6, 8.0, 3.0, 8.0, 0.02
			);
			level.sendParticles(
				player, ParticleTypes.LAVA, false,
				player.getX(), player.getY() + 1.0, player.getZ(),
				1, 9.0, 2.0, 9.0, 0.01
			);
		} else if (route == ExoplanetRoute.NYX_0) {
			level.sendParticles(
				player, ParticleTypes.SNOWFLAKE, false,
				player.getX(), player.getY() + 3.5, player.getZ(),
				5, 8.0, 3.0, 8.0, 0.01
			);
		} else if (route == ExoplanetRoute.PELAGIA_9 && player.isUnderWater()) {
			level.sendParticles(
				player, ParticleTypes.BUBBLE, false,
				player.getX(), player.getY(), player.getZ(),
				4, 5.0, 3.0, 5.0, 0.03
			);
		} else if (route == ExoplanetRoute.SANCTUARY) {
			level.sendParticles(
				player, ParticleTypes.END_ROD, false,
				player.getX(), player.getY() + 3.0, player.getZ(),
				1, 9.0, 3.0, 9.0, 0.005
			);
		}
	}

	private static void sendSnapshot(
		ServerPlayer player,
		ExoplanetHazardSnapshot snapshot,
		ExoplanetGravityCalculator.GravityReading gravity
	) {
		if (snapshot == null) {
			ServerPlayNetworking.send(player, ExoplanetSyncPayload.inactive());
			return;
		}
		ServerPlayNetworking.send(player, new ExoplanetSyncPayload(
			true,
			snapshot.route().ordinal(),
			snapshot.ambientCelsius(),
			snapshot.pressureAtm(),
			snapshot.hazardous(),
			snapshot.statusKey(),
			gravity.effectiveG(),
			gravity.blackHoleElevation()
		));
	}
}

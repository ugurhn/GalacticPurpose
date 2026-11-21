package com.galacticpurpose.ship;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.block.StarshipCargoLockerBlockEntity;
import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.exoplanet.ExoplanetLandmarkBuilder;
import com.galacticpurpose.network.ClientSoundEventPayload;
import com.galacticpurpose.network.OrbitalTransitSyncPayload;
import com.galacticpurpose.network.StarshipActionPayload;
import com.galacticpurpose.network.StarshipInputPayload;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import com.galacticpurpose.wormhole.WormholeLinkRegistry;
import com.galacticpurpose.wormhole.WormholeReturnState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Coordinates the complete Interstellar Spaceflight loop:
 * Magnetic Runway Launch into 45x41x45 Colossal Wormhole -> Walkable Panoramic-Glass Cabin (60s Guided Orbital Cruise) ->
 * Atmospheric Re-Entry -> Automated Propulsive Natural-Terrain Descent ->
 * Exoplanet Landmark Exploration & Refueling -> Origin Wormhole Link Check ->
 * ~55s Planetary Atmospheric Climb -> 60s Return Space Cruise -> Origin Observatory Emergence!
 */
public final class OrbitalTransitManager {
	public static final ResourceKey<Level> ORBITAL_TRANSIT_KEY =
		ResourceKey.create(Registries.DIMENSION, GalacticPurpose.id("orbital_transit"));

	private static final int CRUISE_DURATION_TICKS = StarshipFlightPhysics.CRUISE_DURATION_TICKS;
	private static final int REENTRY_DURATION_TICKS = StarshipFlightPhysics.REENTRY_DURATION_TICKS;

	private OrbitalTransitManager() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(OrbitalTransitManager::tickServer);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncPlayer(handler.player));

		ServerPlayNetworking.registerGlobalReceiver(StarshipActionPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleAction(context.player(), payload.actionId())));

		// Retained no-op receiver for wire compatibility; Horizon Lander uses runway catapult & orbital autopilot
		ServerPlayNetworking.registerGlobalReceiver(StarshipInputPayload.TYPE, (payload, context) -> {});
	}

	public static boolean isInOrbitalTransit(ServerPlayer player) {
		return player.serverLevel().dimension().equals(ORBITAL_TRANSIT_KEY);
	}

	/**
	 * Launches the player & their Horizon Lander out of a 45x41x45 Colossal Wormhole
	 * straight into the 60-second deep-space walkable orbital cruise above the destination exoplanet!
	 */
	public static void beginOutboundTransit(
		ServerPlayer player,
		InterstellarShipEntity ship,
		ExoplanetRoute route,
		ResourceLocation originDimId,
		BlockPos originCorePos
	) {
		MinecraftServer server = player.server;
		ServerLevel transitLevel = server.getLevel(ORBITAL_TRANSIT_KEY);
		if (transitLevel == null) {
			ServerLevel destLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, route.dimensionId()));
			if (destLevel != null) {
				CompoundTag cargo = ship != null ? ship.exportCargoTag(server.registryAccess()) : new CompoundTag();
				int fuel = ship != null ? ship.fuelUnits() : 2000;
				long energy = ship != null ? ship.energyStored() : 250_000L;
				if (ship != null) {
					player.stopRiding();
					ship.discard();
				}
				ExoplanetLandmarkBuilder.ensureLandmarkBuilt(destLevel, route);
				StarshipLandingCalculator.LandingSite site = StarshipLandingCalculator.findNaturalLandingSite(destLevel, route);
				double startY = Math.min(destLevel.getMaxBuildHeight() - 12, site.y() + StarshipLandingCalculator.ENTRY_START_ALTITUDE);
				player.setPortalCooldown(100);
				player.teleportTo(destLevel, site.x() + 0.5, startY, site.z() + 0.5, 180.0f, 18.0f);
				InterstellarShipEntity landedShip = InterstellarShipEntity.spawnForAutoDescent(
					destLevel, site.x() + 0.5, startY, site.z() + 0.5, site.y(), site.hydroPontoon(),
					fuel, energy, cargo, server.registryAccess(), originDimId, originCorePos
				);
				player.startRiding(landedShip, true);
				ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_WORMHOLE_WARP, 0.72f, 1.0f));
			}
			return;
		}
		CompoundTag cargo = ship != null ? ship.exportCargoTag(server.registryAccess()) : new CompoundTag();
		int fuel = ship != null ? ship.fuelUnits() : 2000;
		long energy = ship != null ? ship.energyStored() : 250_000L;
		if (ship != null) {
			player.stopRiding();
			ship.discard();
		}

		if (originDimId != null && originCorePos != null) {
			WormholeLinkRegistry.get(server).updateStatus(originDimId, originCorePos, WormholeLinkCalculator.LinkStatus.OPEN);
		}

		OrbitalTransitState state = OrbitalTransitState.get(server);
		OrbitalTransitState.TransitSession session = state.createSession(
			player.getUUID(), false, route, originDimId, originCorePos, fuel, energy, cargo
		);
		session.startFuel = fuel;
		session.plannedFuelBurn = Math.min(fuel, 250);
		state.setDirty();
		OrbitalShipCabinBuilder.buildCabin(transitLevel, session.slotIndex, session.cargoTag, server.registryAccess());

		BlockPos spawn = OrbitalShipCabinBuilder.playerSpawnPos(session.slotIndex);
		player.setPortalCooldown(80);
		player.teleportTo(transitLevel, spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5, 0.0f, 0.0f);
		com.galacticpurpose.exoplanet.ExoplanetHazardTicker.syncPlayer(player);
		ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_WORMHOLE_WARP, 0.72f, 1.0f));
		player.displayClientMessage(
			Component.translatable("message.galacticpurpose.orbital_outbound_entered", Component.translatable(route.translationKey()))
				.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
			false
		);
		syncPlayer(player);
	}

	/**
	 * Transitions the player from their 45-second exoplanet orbital ascent into the 60-second return space cruise
	 * heading straight back toward their bound Origin Wormhole!
	 */
	public static void beginReturnTransit(ServerPlayer player, InterstellarShipEntity ship, ExoplanetRoute departedRoute) {
		MinecraftServer server = player.server;
		ServerLevel transitLevel = server.getLevel(ORBITAL_TRANSIT_KEY);
		CompoundTag cargo = ship.exportCargoTag(server.registryAccess());
		int ascentBurned = ship.ascentFuelBurned();
		int remainingCruiseBurn = Math.max(0, WormholeLinkCalculator.REQUIRED_ORBITAL_LAUNCH_FUEL - ascentBurned);
		int fuel = ship.fuelUnits();
		long energy = ship.energyStored();
		ResourceLocation originDim = ship.originDimId();
		BlockPos originPos = ship.originCorePos();

		player.stopRiding();
		ship.discard();

		if (transitLevel == null) {
			fuel = Math.max(0, fuel - remainingCruiseBurn);
			ServerLevel originLevel = originDim != null
				? server.getLevel(ResourceKey.create(Registries.DIMENSION, originDim))
				: null;
			if (originLevel == null) originLevel = server.overworld();
			int tx = originPos != null ? originPos.getX() + 26 : originLevel.getSharedSpawnPos().getX();
			int tz = originPos != null ? originPos.getZ() : originLevel.getSharedSpawnPos().getZ();
			originLevel.getChunk(tx >> 4, tz >> 4);
			int groundY = Math.max(originLevel.getMinBuildHeight() + 8,
				originLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz));
			double startY = Math.min(originLevel.getMaxBuildHeight() - 12, groundY + 72.0);
			player.setPortalCooldown(100);
			player.teleportTo(originLevel, tx + 0.5, startY, tz + 0.5, player.getYRot(), 12.0f);
			InterstellarShipEntity homeShip = InterstellarShipEntity.spawnForAutoDescent(
				originLevel, tx + 0.5, startY, tz + 0.5, groundY, false,
				fuel, energy, cargo, server.registryAccess(), originDim, originPos
			);
			player.startRiding(homeShip, true);
			ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_WORMHOLE_WARP, 0.72f, 1.0f));
			return;
		}

		OrbitalTransitState state = OrbitalTransitState.get(server);
		OrbitalTransitState.TransitSession session = state.createSession(
			player.getUUID(), true, departedRoute, originDim, originPos, fuel, energy, cargo
		);
		session.startFuel = fuel;
		session.plannedFuelBurn = Math.min(fuel, remainingCruiseBurn);
		state.setDirty();
		OrbitalShipCabinBuilder.buildCabin(transitLevel, session.slotIndex, session.cargoTag, server.registryAccess());

		BlockPos spawn = OrbitalShipCabinBuilder.playerSpawnPos(session.slotIndex);
		player.setPortalCooldown(80);
		player.teleportTo(transitLevel, spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5, 0.0f, 0.0f);
		com.galacticpurpose.exoplanet.ExoplanetHazardTicker.syncPlayer(player);
		ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_AUTOPILOT_ENGAGE, 0.68f, 1.05f));
		player.displayClientMessage(
			Component.translatable("message.galacticpurpose.orbital_return_entered")
				.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
			false
		);
		syncPlayer(player);
	}

	public static boolean canEngageSession(OrbitalTransitState.TransitSession session) {
		return session != null && !session.reentryActive && session.elapsedTicks >= CRUISE_DURATION_TICKS;
	}

	public static boolean canEngageAutoLanding(ServerPlayer player) {
		OrbitalTransitState state = OrbitalTransitState.get(player.server);
		return canEngageSession(state.getSession(player.getUUID()));
	}

	public static void engageAutoLandingOrWormhole(ServerPlayer player) {
		OrbitalTransitState state = OrbitalTransitState.get(player.server);
		OrbitalTransitState.TransitSession session = state.getSession(player.getUUID());
		if (session == null || session.reentryActive) {
			return;
		}
		if (session.elapsedTicks < CRUISE_DURATION_TICKS) {
			int secLeft = Math.max(1, (CRUISE_DURATION_TICKS - session.elapsedTicks + 19) / 20);
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.orbital_wait_arrival", secLeft)
					.withStyle(ChatFormatting.YELLOW),
				true
			);
			return;
		}
		session.reentryActive = true;
		session.reentryTicks = 0;
		state.setDirty();

		// Calm sci-fi bridge autopilot chime (ZERO sudden explosion jumpscare!)
		ServerPlayNetworking.send(player, new ClientSoundEventPayload(
			ClientSoundEventPayload.EVENT_AUTOPILOT_ENGAGE,
			0.68f,
			session.returning ? 1.08f : 0.96f
		));
		Component msg = session.returning
			? Component.translatable("message.galacticpurpose.orbital_entering_wormhole")
			: Component.translatable("message.galacticpurpose.orbital_engaging_landing", Component.translatable(session.route.translationKey()));
		player.displayClientMessage(msg.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		syncPlayer(player);
	}

	private static void handleAction(ServerPlayer player, int actionId) {
		if (actionId == StarshipActionPayload.ACTION_ENGAGE_AUTO_LAND_OR_ENTER && isInOrbitalTransit(player)) {
			engageAutoLandingOrWormhole(player);
			return;
		}
		if (player.getVehicle() instanceof InterstellarShipEntity ship) {
			switch (actionId) {
				case StarshipActionPayload.ACTION_IGNITE_ORBITAL_LAUNCH -> ship.tryStartLaunchSequence(player);
				case StarshipActionPayload.ACTION_OPEN_CARGO -> ship.openStarshipScreen(player);
			}
		}
	}

	private static void tickServer(MinecraftServer server) {
		ServerLevel transitLevel = server.getLevel(ORBITAL_TRANSIT_KEY);
		if (transitLevel == null) {
			return;
		}
		OrbitalTransitState state = OrbitalTransitState.get(server);
		for (ServerPlayer player : transitLevel.players().stream().toList()) {
			OrbitalTransitState.TransitSession session = state.getSession(player.getUUID());
			if (session == null) {
				continue;
			}
			session.elapsedTicks++;

			// Keep player safely inside their pressurized panoramic cabin bounds
			BlockPos origin = OrbitalShipCabinBuilder.cabinOrigin(session.slotIndex);
			if (player.getY() < origin.getY() - 2 || Math.abs(player.getX() - origin.getX()) > 12 || Math.abs(player.getZ()) > 14) {
				BlockPos safe = OrbitalShipCabinBuilder.playerSpawnPos(session.slotIndex);
				player.teleportTo(safe.getX() + 0.5, safe.getY() + 0.1, safe.getZ() + 0.5);
			}

			if (session.reentryActive) {
				session.reentryTicks++;
				float entryProg = Math.min(1.0f, session.reentryTicks / (float) REENTRY_DURATION_TICKS);
				session.fuelUnits = Math.max(0, session.startFuel - Math.round(session.plannedFuelBurn * (0.90f + 0.10f * entryProg)));
				if (session.reentryTicks >= REENTRY_DURATION_TICKS) {
					completeTransit(player, transitLevel, state, session);
					continue;
				}
			} else {
				float cruiseProg = Math.min(1.0f, session.elapsedTicks / (float) CRUISE_DURATION_TICKS);
				session.fuelUnits = Math.max(0, session.startFuel - Math.round(session.plannedFuelBurn * 0.90f * cruiseProg));
				// Guided 95-second narrative flight milestones
				if (session.elapsedTicks >= CRUISE_DURATION_TICKS - 140 && session.elapsedTicks <= CRUISE_DURATION_TICKS) {
					int step = session.elapsedTicks - (CRUISE_DURATION_TICKS - 140);
					if (step % 4 == 0) {
						warmSingleCorridorChunk(server, session, step / 4);
					}
				}
				if (session.elapsedTicks == 420) {
					Component phase2 = session.returning
						? Component.translatable("message.galacticpurpose.cruise_return_phase2")
						: Component.translatable("message.galacticpurpose.cruise_outbound_phase2", Component.translatable(session.route.translationKey()));
					player.displayClientMessage(phase2.copy().withStyle(ChatFormatting.AQUA), false);
				} else if (session.elapsedTicks == 1290) {
					Component phase3 = session.returning
						? Component.translatable("message.galacticpurpose.cruise_return_phase3")
						: Component.translatable("message.galacticpurpose.cruise_outbound_phase3", Component.translatable(session.route.translationKey()));
					player.displayClientMessage(phase3.copy().withStyle(ChatFormatting.GOLD), false);
				} else if (session.elapsedTicks == CRUISE_DURATION_TICKS) {
					syncPlayer(player);
					ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_AUTOPILOT_ENGAGE, 0.62f, 1.15f));
					Component ready = session.returning
						? Component.translatable("message.galacticpurpose.helm_hint_return")
						: Component.translatable("message.galacticpurpose.helm_hint_land");
					player.displayClientMessage(ready.copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
				} else if (session.elapsedTicks > CRUISE_DURATION_TICKS && session.elapsedTicks % 160 == 0) {
					Component hint = session.returning
						? Component.translatable("message.galacticpurpose.helm_hint_return")
						: Component.translatable("message.galacticpurpose.helm_hint_land");
					player.displayClientMessage(hint.copy().withStyle(ChatFormatting.YELLOW), true);
				}
			}

			// Sync every 2 ticks (or every tick during re-entry plunge) so client interpolates smoothly at 144 FPS!
			if (server.getTickCount() % 2 == 0 || session.reentryActive) {
				syncPlayer(player);
			}
		}
	}

	private static void completeTransit(
		ServerPlayer player,
		ServerLevel transitLevel,
		OrbitalTransitState state,
		OrbitalTransitState.TransitSession session
	) {
		MinecraftServer server = player.server;
		BlockPos lockerPos = OrbitalShipCabinBuilder.cargoLockerPos(session.slotIndex);
		BlockEntity be = transitLevel.getBlockEntity(lockerPos);
		if (be instanceof StarshipCargoLockerBlockEntity locker) {
			session.cargoTag = locker.exportCargoTag(server.registryAccess());
		}

		state.removeSession(player.getUUID());
		com.galacticpurpose.exoplanet.ExoplanetHazardTicker.clearExoplanetGravity(player);
		ServerPlayNetworking.send(player, OrbitalTransitSyncPayload.inactive());

		if (!session.returning) {
			ServerLevel destLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, session.route.dimensionId()));
			if (destLevel == null) {
				destLevel = server.overworld();
			}
			ExoplanetLandmarkBuilder.ensureLandmarkBuilt(destLevel, session.route);
			StarshipLandingCalculator.LandingSite site =
				StarshipLandingCalculator.findNaturalLandingSite(destLevel, session.route);

			double startY = Math.min(destLevel.getMaxBuildHeight() - 12, site.y() + 46.0);
			player.setPortalCooldown(100);
			player.teleportTo(destLevel, site.x() + 0.5, startY, site.z() + 78.5, 180.0f, 12.0f);

			InterstellarShipEntity ship = InterstellarShipEntity.spawnForAutoDescent(
				destLevel,
				site.x() + 0.5, startY, site.z() + 0.5,
				site.y(), site.hydroPontoon(),
				session.fuelUnits, session.energyStored,
				session.cargoTag, server.registryAccess(),
				session.originDimId, session.originCorePos
			);
			player.startRiding(ship, true);
			ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_AUTOPILOT_ENGAGE, 0.62f, 0.90f));

			if (session.route == ExoplanetRoute.SANCTUARY
				&& WormholeReturnState.get(destLevel).markSanctuaryArrived(player.getUUID())) {
				player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.EXPEDITION_LOG_4));
				player.displayClientMessage(
					Component.translatable("message.galacticpurpose.sanctuary_victory")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					false
				);
			}
		} else {
			ServerLevel originLevel = session.originDimId != null
				? server.getLevel(ResourceKey.create(Registries.DIMENSION, session.originDimId))
				: null;
			if (originLevel == null) {
				originLevel = server.overworld();
			}
			int tx;
			int tz;
			if (session.originCorePos != null) {
				tx = session.originCorePos.getX() + 28;
				tz = session.originCorePos.getZ();
			} else {
				BlockPos spawn = originLevel.getSharedSpawnPos();
				tx = spawn.getX();
				tz = spawn.getZ();
			}
			originLevel.getChunk(tx >> 4, tz >> 4);
			int groundY = Math.max(originLevel.getMinBuildHeight() + 8,
				originLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz));
			double startY = Math.min(originLevel.getMaxBuildHeight() - 12, groundY + 46.0);

			player.setPortalCooldown(100);
			player.teleportTo(originLevel, tx + 0.5, startY, tz + 78.5, 180.0f, 12.0f);

			InterstellarShipEntity ship = InterstellarShipEntity.spawnForAutoDescent(
				originLevel,
				tx + 0.5, startY, tz + 0.5,
				groundY, false,
				session.fuelUnits, session.energyStored,
				session.cargoTag, server.registryAccess(),
				session.originDimId, session.originCorePos
			);
			player.startRiding(ship, true);
			ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_WORMHOLE_WARP, 0.72f, 1.0f));
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.orbital_returned_home")
					.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
				false
			);
		}
	}

	private static void warmSingleCorridorChunk(MinecraftServer server, OrbitalTransitState.TransitSession session, int index) {
		if (index < 0 || index >= 35) {
			return;
		}
		int dx = (index % 5) - 2;
		int dz = (index / 5) - 1;
		if (!session.returning) {
			ServerLevel destLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, session.route.dimensionId()));
			if (destLevel == null) {
				destLevel = server.overworld();
			}
			if (!session.hasCachedTarget) {
				StarshipLandingCalculator.LandingSite site =
					StarshipLandingCalculator.findNaturalLandingSite(destLevel, session.route);
				session.cachedTargetX = site.x();
				session.cachedTargetZ = site.z();
				session.hasCachedTarget = true;
			}
			destLevel.getChunk((session.cachedTargetX >> 4) + dx, (session.cachedTargetZ >> 4) + dz);
		} else {
			ServerLevel originLevel = session.originDimId != null
				? server.getLevel(ResourceKey.create(Registries.DIMENSION, session.originDimId))
				: null;
			if (originLevel == null) {
				originLevel = server.overworld();
			}
			int tx = session.originCorePos != null ? session.originCorePos.getX() + 28 : originLevel.getSharedSpawnPos().getX();
			int tz = session.originCorePos != null ? session.originCorePos.getZ() : originLevel.getSharedSpawnPos().getZ();
			originLevel.getChunk((tx >> 4) + dx, (tz >> 4) + dz);
		}
	}

	public static void syncPlayer(ServerPlayer player) {
		if (!isInOrbitalTransit(player)) {
			ServerPlayNetworking.send(player, OrbitalTransitSyncPayload.inactive());
			return;
		}
		OrbitalTransitState.TransitSession session = OrbitalTransitState.get(player.server).getSession(player.getUUID());
		if (session == null) {
			ServerPlayNetworking.send(player, OrbitalTransitSyncPayload.inactive());
			return;
		}
		WormholeLinkCalculator.LinkStatus linkStatus = WormholeLinkRegistry.get(player.server)
			.queryLiveStatus(player.server, session.originDimId, session.originCorePos);
		ServerPlayNetworking.send(
			player,
			new OrbitalTransitSyncPayload(
				true,
				session.returning,
				session.route.ordinal(),
				Math.min(session.elapsedTicks, CRUISE_DURATION_TICKS),
				CRUISE_DURATION_TICKS,
				session.reentryActive,
				session.reentryTicks,
				REENTRY_DURATION_TICKS,
				linkStatus.ordinal(),
				session.fuelUnits
			)
		);
	}
}

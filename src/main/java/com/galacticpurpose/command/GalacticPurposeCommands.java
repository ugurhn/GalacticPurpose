package com.galacticpurpose.command;

import com.galacticpurpose.block.ShipAssemblyPadBlock;
import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.climate.CollapsePhase;
import com.galacticpurpose.climate.EarthCollapseState;
import com.galacticpurpose.climate.OverworldHazardTicker;
import com.galacticpurpose.climate.SolarInstabilityCalculator;
import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.config.ModConfig;
import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.entity.SingularityCollapseEntity;
import com.galacticpurpose.exoplanet.ExoplanetHazardTicker;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.ship.OrbitalTransitManager;
import com.galacticpurpose.terraforming.MarsProgressSnapshot;
import com.galacticpurpose.terraforming.MarsTerraformingState;
import com.galacticpurpose.terraforming.SolarStormPhase;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.ExoplanetTravelHandler;
import com.galacticpurpose.wormhole.WormholeMultiblockValidator;
import com.galacticpurpose.wormhole.WormholeTier;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * Administrative & testing commands for simulating solar collapse, Mars terraforming, and solar storms.
 */
public final class GalacticPurposeCommands {
	private static final long TICKS_PER_DAY = 24000L;

	private GalacticPurposeCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("galacticpurpose")
				.then(Commands.literal("codex").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					net.minecraft.world.item.ItemStack codex = new net.minecraft.world.item.ItemStack(ModItems.MISSION_CODEX);
					if (!player.getInventory().add(codex)) {
						player.drop(codex, false);
					}
					ctx.getSource().sendSuccess(() -> Component.translatable("message.galacticpurpose.codex_granted"), false);
					return 1;
				}))
				.then(Commands.literal("status").executes(ctx -> {
					ServerLevel overworld = ctx.getSource().getServer().overworld();
					long day = Math.max(0L, overworld.getDayTime() / TICKS_PER_DAY);
					ModConfig config = ModConfig.get();
					CollapsePhase phase = SolarInstabilityCalculator.determinePhase(day, config);
					int severityPct = Math.round(SolarInstabilityCalculator.computeSeverity(day, config) * 100.0f);

					ctx.getSource().sendSuccess(() -> Component.translatable(
						"command.galacticpurpose.status",
						day,
						Component.translatable(phase.translationKey()),
						severityPct,
						config.graceDays()
					), false);
					return 1;
				}))
				.then(Commands.literal("setday")
					.requires(source -> source.hasPermission(2))
					.then(Commands.argument("day", IntegerArgumentType.integer(0, 100000))
						.executes(ctx -> {
							int targetDay = IntegerArgumentType.getInteger(ctx, "day");
							ServerLevel overworld = ctx.getSource().getServer().overworld();
							long timeOfDay = Math.floorMod(overworld.getDayTime(), TICKS_PER_DAY);
							overworld.setDayTime((long) targetDay * TICKS_PER_DAY + timeOfDay);

							CollapsePhase phase = SolarInstabilityCalculator.determinePhase(targetDay, ModConfig.get());
							EarthCollapseState state = EarthCollapseState.get(overworld);
							if (phase.ordinal() < state.getLastAnnouncedPhase().ordinal()) {
								state.setLastAnnouncedPhase(phase);
							}

							for (ServerPlayer player : overworld.players()) {
								OverworldHazardTicker.syncPlayer(player);
							}

							ctx.getSource().sendSuccess(() -> Component.translatable(
								"command.galacticpurpose.setday",
								targetDay,
								Component.translatable(phase.translationKey())
							), true);
							return 1;
						})
					)
				)
				.then(Commands.literal("mars")
					.then(Commands.literal("status").executes(ctx -> {
						ServerLevel level = ctx.getSource().getLevel();
						if (!SpaceCompatManager.get().isMars(level)) {
							ctx.getSource().sendFailure(Component.translatable("command.galacticpurpose.mars_not_here"));
							return 0;
						}
						MarsProgressSnapshot snap = MarsTerraformingState.get(level).snapshot();
						ctx.getSource().sendSuccess(() -> Component.translatable(
							"command.galacticpurpose.mars_status",
							String.format("%.1f", snap.heatPercent()),
							String.format("%.1f", snap.pressurePercent()),
							String.format("%.1f", snap.oxygenPercent()),
							Math.round(snap.terraformingIndex())
						), false);
						return 1;
					}))
					.then(Commands.literal("set")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("heat", FloatArgumentType.floatArg(0.0f, 100.0f))
							.then(Commands.argument("pressure", FloatArgumentType.floatArg(0.0f, 100.0f))
								.then(Commands.argument("oxygen", FloatArgumentType.floatArg(0.0f, 100.0f))
									.executes(ctx -> {
										ServerLevel level = ctx.getSource().getLevel();
										if (!SpaceCompatManager.get().isMars(level)) {
											ctx.getSource().sendFailure(
												Component.translatable("command.galacticpurpose.mars_not_here")
											);
											return 0;
										}
										float heat = FloatArgumentType.getFloat(ctx, "heat");
										float pressure = FloatArgumentType.getFloat(ctx, "pressure");
										float oxygen = FloatArgumentType.getFloat(ctx, "oxygen");

										MarsTerraformingState state = MarsTerraformingState.get(level);
										state.setPercents(heat, pressure, oxygen);
										state.syncDimension(level);
										MarsProgressSnapshot snap = state.snapshot();

										ctx.getSource().sendSuccess(() -> Component.translatable(
											"command.galacticpurpose.mars_set",
											String.format("%.1f", snap.heatPercent()),
											String.format("%.1f", snap.pressurePercent()),
											String.format("%.1f", snap.oxygenPercent()),
											Math.round(snap.terraformingIndex())
										), true);
										return 1;
									})
								)
							)
						)
					)
					.then(Commands.literal("testmode")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("enabled", BoolArgumentType.bool())
							.executes(ctx -> {
								boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
								ModConfig.get().setMarsTerraformingTestMode(enabled);
								for (ServerLevel lvl : ctx.getSource().getServer().getAllLevels()) {
									for (ServerPlayer player : lvl.players()) {
										MarsTerraformingState.syncPlayer(player);
									}
								}
								ctx.getSource().sendSuccess(() -> Component.translatable(
									"command.galacticpurpose.mars_testmode",
									Boolean.toString(enabled)
								), true);
								return 1;
							})
						)
					)
					.then(Commands.literal("redgiant")
						.requires(source -> source.hasPermission(2))
						.then(Commands.argument("active", BoolArgumentType.bool())
							.executes(ctx -> {
								ServerLevel level = ctx.getSource().getLevel();
								if (!SpaceCompatManager.get().isMars(level)) {
									ctx.getSource().sendFailure(
										Component.translatable("command.galacticpurpose.mars_not_here")
									);
									return 0;
								}
								boolean active = BoolArgumentType.getBool(ctx, "active");
								MarsTerraformingState.get(level).activateRedGiant(level, active);
								return 1;
							})
						)
					)
					.then(Commands.literal("storm")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("countdown").executes(ctx -> setStormPhase(ctx.getSource(), SolarStormPhase.COUNTDOWN)))
						.then(Commands.literal("warning").executes(ctx -> setStormPhase(ctx.getSource(), SolarStormPhase.WARNING)))
						.then(Commands.literal("active").executes(ctx -> setStormPhase(ctx.getSource(), SolarStormPhase.ACTIVE)))
					)
				)
				.then(Commands.literal("goto")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("hephaestus_iv").executes(ctx -> visitPlanet(ctx.getSource(), ExoplanetRoute.HEPHAESTUS_IV)))
					.then(Commands.literal("pelagia_9").executes(ctx -> visitPlanet(ctx.getSource(), ExoplanetRoute.PELAGIA_9)))
					.then(Commands.literal("nyx_0").executes(ctx -> visitPlanet(ctx.getSource(), ExoplanetRoute.NYX_0)))
					.then(Commands.literal("sanctuary").executes(ctx -> visitPlanet(ctx.getSource(), ExoplanetRoute.SANCTUARY)))
				)
				.then(Commands.literal("return")
					.requires(source -> source.hasPermission(2))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						ExoplanetTravelHandler.returnToOrigin(player);
						return 1;
					})
				)
				.then(Commands.literal("orbit")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("zenith").executes(ctx -> setOrbitPhase(ctx.getSource(), 6000L, "zenith")))
					.then(Commands.literal("horizon").executes(ctx -> setOrbitPhase(ctx.getSource(), 0L, "horizon")))
					.then(Commands.literal("nadir").executes(ctx -> setOrbitPhase(ctx.getSource(), 18000L, "nadir")))
				)
				.then(Commands.literal("wormhole")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("build")
						.then(buildStableBranch(WormholeTier.COLOSSAL))
						.then(Commands.literal("unstable")
							.executes(ctx -> buildWormhole(ctx.getSource(), false, ExoplanetRoute.HEPHAESTUS_IV, WormholeTier.COLOSSAL))
						)
						.then(Commands.literal("colossal")
							.then(buildStableBranch(WormholeTier.COLOSSAL))
							.then(Commands.literal("unstable")
								.executes(ctx -> buildWormhole(ctx.getSource(), false, ExoplanetRoute.HEPHAESTUS_IV, WormholeTier.COLOSSAL))
							)
						)
						.then(Commands.literal("small")
							.then(buildStableBranch(WormholeTier.PERSONAL))
							.then(Commands.literal("unstable")
								.executes(ctx -> buildWormhole(ctx.getSource(), false, ExoplanetRoute.HEPHAESTUS_IV, WormholeTier.PERSONAL))
							)
						)
					)
					.then(Commands.literal("collapse")
						.executes(ctx -> triggerInstantCollapse(ctx.getSource(), WormholeTier.COLOSSAL))
						.then(Commands.literal("colossal").executes(ctx -> triggerInstantCollapse(ctx.getSource(), WormholeTier.COLOSSAL)))
						.then(Commands.literal("small").executes(ctx -> triggerInstantCollapse(ctx.getSource(), WormholeTier.PERSONAL)))
					)
				)
				.then(Commands.literal("ship")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("spawn").executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						ServerLevel level = player.serverLevel();
						Direction facing = player.getDirection();
						BlockPos pos = player.blockPosition().relative(facing, 6);
						float yaw = ShipAssemblyPadBlock.resolveLaunchYaw(level, pos, facing);
						InterstellarShipEntity.spawnParked(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 2500, false);
						ctx.getSource().sendSuccess(() -> Component.translatable("message.galacticpurpose.ship_assembled", 2500), true);
						return 1;
					}))
				)
				.then(Commands.literal("transit")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("hephaestus_iv").executes(ctx -> launchTestTransit(ctx.getSource(), ExoplanetRoute.HEPHAESTUS_IV)))
					.then(Commands.literal("pelagia_9").executes(ctx -> launchTestTransit(ctx.getSource(), ExoplanetRoute.PELAGIA_9)))
					.then(Commands.literal("nyx_0").executes(ctx -> launchTestTransit(ctx.getSource(), ExoplanetRoute.NYX_0)))
					.then(Commands.literal("sanctuary").executes(ctx -> launchTestTransit(ctx.getSource(), ExoplanetRoute.SANCTUARY)))
				)
			)
		);
	}

	private static int launchTestTransit(CommandSourceStack source, ExoplanetRoute route)
		throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = player.serverLevel();
		InterstellarShipEntity riding = player.getVehicle() instanceof InterstellarShipEntity s ? s : null;
		OrbitalTransitManager.beginOutboundTransit(
			player, riding, route, level.dimension().location(), player.blockPosition()
		);
		return 1;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildStableBranch(WormholeTier tier) {
		return Commands.literal("stable")
			.executes(ctx -> buildWormhole(ctx.getSource(), true, ExoplanetRoute.HEPHAESTUS_IV, tier))
			.then(Commands.literal("hephaestus_iv").executes(ctx -> buildWormhole(ctx.getSource(), true, ExoplanetRoute.HEPHAESTUS_IV, tier)))
			.then(Commands.literal("pelagia_9").executes(ctx -> buildWormhole(ctx.getSource(), true, ExoplanetRoute.PELAGIA_9, tier)))
			.then(Commands.literal("nyx_0").executes(ctx -> buildWormhole(ctx.getSource(), true, ExoplanetRoute.NYX_0, tier)))
			.then(Commands.literal("sanctuary").executes(ctx -> buildWormhole(ctx.getSource(), true, ExoplanetRoute.SANCTUARY, tier)));
	}

	private static int buildWormhole(CommandSourceStack source, boolean stable, ExoplanetRoute route, WormholeTier tier)
		throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = player.serverLevel();
		Direction facing = player.getDirection();
		int forwardOffset = tier == WormholeTier.COLOSSAL ? 42 : 7;
		int coreElevation = tier == WormholeTier.COLOSSAL ? WormholeTier.COLOSSAL.injectorDrop() : 3;
		BlockPos baseCenter = player.blockPosition().relative(facing, forwardOffset);
		BlockPos corePos = baseCenter.above(coreElevation);

		WormholeMultiblockValidator.buildStructure(level, corePos, tier);
		if (level.getBlockEntity(corePos) instanceof WormholeCoreBlockEntity core) {
			if (stable) {
				core.configureStablePreset(level, route, tier);
				if (tier == WormholeTier.COLOSSAL) {
					BlockPos padPos = WormholeMultiblockValidator.buildColossalLaunchRunway(level, corePos, facing);
					float yaw = InterstellarShipEntity.computeYawFacingTarget(
						padPos.getX() + 0.5, padPos.getZ() + 0.5, corePos.getX() + 0.5, corePos.getZ() + 0.5
					);
					InterstellarShipEntity.spawnParked(level, padPos.getX() + 0.5, padPos.getY() + 1.0, padPos.getZ() + 0.5, yaw, 2500, false);
					// Move testing player cleanly onto the unobstructed diagonal runway right beside the parked Horizon Lander!
					double dx = padPos.getX() - corePos.getX();
					double dz = padPos.getZ() - corePos.getZ();
					double len = Math.max(1.0, Math.hypot(dx, dz));
					double standX = padPos.getX() + 0.5 + (dx / len) * 8.0;
					double standZ = padPos.getZ() + 0.5 + (dz / len) * 8.0;
					player.teleportTo(level, standX, padPos.getY() + 1.2, standZ, yaw, -6.0f);
				}
				source.sendSuccess(() -> Component.translatable(
					"command.galacticpurpose.wormhole_built_stable",
					Component.translatable(tier.translationKey()),
					Component.translatable(route.translationKey()),
					corePos.toShortString()
				), true);
			} else {
				core.configureUnstablePreset(level, tier);
				source.sendSuccess(() -> Component.translatable(
					"command.galacticpurpose.wormhole_built_unstable",
					Component.translatable(tier.translationKey()),
					corePos.toShortString()
				), true);
			}
		}
		return 1;
	}

	private static int triggerInstantCollapse(CommandSourceStack source, WormholeTier tier)
		throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = player.serverLevel();
		Direction facing = player.getDirection();
		int dist = tier == WormholeTier.COLOSSAL ? 38 : 11;
		int elev = tier == WormholeTier.COLOSSAL ? 14 : 4;
		BlockPos spawnPos = player.blockPosition().relative(facing, dist).above(elev);
		SingularityCollapseEntity.spawnForTier(level, Vec3.atCenterOf(spawnPos), tier);
		source.sendSuccess(() -> Component.translatable(
			"command.galacticpurpose.wormhole_collapse_spawned",
			spawnPos.toShortString()
		), true);
		return 1;
	}

	/**
	 * Updates Overworld primary dayTime (which drives all derived dimensions in vanilla Minecraft)
	 * and immediately pushes time & tidal gravity packets to every connected player.
	 */
	public static void setServerOrbitalTime(MinecraftServer server, long timeOfDay) {
		ServerLevel overworld = server.overworld();
		long day = Math.max(0L, overworld.getDayTime() / TICKS_PER_DAY);
		long newDayTime = day * TICKS_PER_DAY + Math.floorMod(timeOfDay, TICKS_PER_DAY);
		overworld.setDayTime(newDayTime);
		boolean doDaylight = overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
		for (ServerLevel lvl : server.getAllLevels()) {
			ClientboundSetTimePacket timePacket = new ClientboundSetTimePacket(lvl.getGameTime(), newDayTime, doDaylight);
			for (ServerPlayer p : lvl.players()) {
				p.connection.send(timePacket);
				ExoplanetHazardTicker.syncPlayer(p);
			}
		}
	}

	private static int setOrbitPhase(CommandSourceStack source, long timeOfDay, String label) {
		setServerOrbitalTime(source.getServer(), timeOfDay);
		source.sendSuccess(() -> Component.translatable("command.galacticpurpose.orbit_set", label), true);
		return 1;
	}

	private static int visitPlanet(net.minecraft.commands.CommandSourceStack source, ExoplanetRoute route)
		throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ExoplanetTravelHandler.travel(player, route);
		return 1;
	}

	private static int setStormPhase(net.minecraft.commands.CommandSourceStack source, SolarStormPhase phase) {
		ServerLevel level = source.getLevel();
		if (!SpaceCompatManager.get().isMars(level)) {
			source.sendFailure(Component.translatable("command.galacticpurpose.mars_not_here"));
			return 0;
		}
		MarsTerraformingState.get(level).setStormPhaseForTesting(level, phase);
		source.sendSuccess(() -> Component.translatable(
			"command.galacticpurpose.mars_storm_set",
			Component.translatable(phase.translationKey())
		), true);
		return 1;
	}
}

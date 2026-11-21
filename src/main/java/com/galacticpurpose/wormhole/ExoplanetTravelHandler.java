package com.galacticpurpose.wormhole;

import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.exoplanet.ExoplanetLandmarkBuilder;
import com.galacticpurpose.network.ClientSoundEventPayload;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.ship.StarshipLandingCalculator;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Handles exoplanet arrival and return anchors without spawning artificial single-block return slabs:
 * direct/command arrival places a fueled Horizon Lander on natural terrain instead of a return beacon!
 */
public final class ExoplanetTravelHandler {
	private ExoplanetTravelHandler() {
	}

	public static void travel(ServerPlayer player, ExoplanetRoute route) {
		MinecraftServer server = player.server;
		ResourceLocation dimId = route.dimensionId();
		ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, dimId);
		ServerLevel targetLevel = server.getLevel(targetKey);

		if (targetLevel == null) {
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.wormhole_locked_preview",
					Component.translatable(route.translationKey())
				).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				false
			);
			return;
		}

		ServerLevel originLevel = player.serverLevel();
		WormholeReturnState returnState = WormholeReturnState.get(originLevel);
		if (ExoplanetRoute.fromDimensionId(originLevel.dimension().location()) == null) {
			returnState.setReturnAnchor(
				player.getUUID(),
				new WormholeReturnState.ReturnAnchor(
					originLevel.dimension().location(),
					player.getX(),
					player.getY(),
					player.getZ()
				)
			);
		}

		// Build signature planet architectural landmark & select untouched natural landing clearing (ZERO 5x5 slab or return beacon!)
		ExoplanetLandmarkBuilder.ensureLandmarkBuilt(targetLevel, route);
		StarshipLandingCalculator.LandingSite site = StarshipLandingCalculator.findNaturalLandingSite(targetLevel, route);

		player.setPortalCooldown(80);
		player.teleportTo(
			targetLevel,
			site.x() + 0.5,
			site.y() + 0.2,
			site.z() + 4.5,
			180.0f,
			0.0f
		);
		ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_WORMHOLE_WARP, 0.72f, 1.0f));

		// Ensure the explorer has a fueled Horizon Lander parked nearby on natural ground for authentic spacecraft return!
		AABB shipArea = new AABB(new BlockPos(site.x(), site.y(), site.z())).inflate(36.0);
		if (targetLevel.getEntitiesOfClass(InterstellarShipEntity.class, shipArea).isEmpty()) {
			BlockPos originCore = InterstellarShipEntity.findNearestColossalWormhole(originLevel, player.blockPosition(), 8, false);
			InterstellarShipEntity lander = InterstellarShipEntity.spawnParked(
				targetLevel, site.x() + 0.5, site.y(), site.z() + 0.5, 180.0f, 2500, site.hydroPontoon()
			);
			if (originCore != null) {
				lander.setOriginLink(originLevel.dimension().location(), originCore);
			}
		}

		player.displayClientMessage(
			Component.translatable(
				"message.galacticpurpose.wormhole_arrived",
				Component.translatable(route.translationKey())
			).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
			false
		);

		if (route == ExoplanetRoute.SANCTUARY && returnState.markSanctuaryArrived(player.getUUID())) {
			player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.EXPEDITION_LOG_4));
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.sanctuary_victory")
					.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				false
			);
		}
	}

	public static void returnToOrigin(ServerPlayer player) {
		MinecraftServer server = player.server;
		WormholeReturnState state = WormholeReturnState.get(player.serverLevel());
		WormholeReturnState.ReturnAnchor anchor = state.getReturnAnchor(player.getUUID());

		ServerLevel targetLevel = null;
		double x;
		double y;
		double z;

		if (anchor != null) {
			targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, anchor.dimensionId()));
		}
		if (targetLevel != null && anchor != null) {
			x = anchor.x();
			y = anchor.y();
			z = anchor.z();
		} else {
			targetLevel = server.overworld();
			BlockPos spawn = targetLevel.getSharedSpawnPos();
			x = spawn.getX() + 0.5;
			y = spawn.getY() + 1.0;
			z = spawn.getZ() + 0.5;
		}

		player.setPortalCooldown(80);
		player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
		ServerPlayNetworking.send(player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_WORMHOLE_WARP, 0.72f, 1.05f));
		player.displayClientMessage(
			Component.translatable("message.galacticpurpose.wormhole_returned")
				.withStyle(ChatFormatting.AQUA),
			false
		);
	}
}

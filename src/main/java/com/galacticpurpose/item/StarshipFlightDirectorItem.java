package com.galacticpurpose.item;

import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.network.ClientSoundEventPayload;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Portable Starship Flight Director:
 * 1. Right-click a parked Horizon Lander to bind its telemetry transponder UUID.
 * 2. Right-click any target destination block in the same dimension to initiate an automated
 *    point-to-point ferry launch (identical spool -> aerodynamic takeoff climb -> vector-thrust flare
 *    -> skid touchdown), consuming Fuel proportional to distance.
 */
public class StarshipFlightDirectorItem extends Item {
	public static final String TAG_SHIP_UUID = "BoundShipUuid";
	public static final String TAG_SHIP_FUEL = "BoundShipFuel";

	public StarshipFlightDirectorItem(Properties properties) {
		super(properties);
	}

	public static void bindShip(ItemStack stack, InterstellarShipEntity ship, ServerPlayer player) {
		if (ship.shipMode() != InterstellarShipEntity.MODE_PARKED) {
			if (player != null) {
				player.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_relocate_busy").withStyle(ChatFormatting.YELLOW),
					true
				);
			}
			return;
		}
		CompoundTag tag = new CompoundTag();
		tag.putUUID(TAG_SHIP_UUID, ship.getUUID());
		tag.putInt(TAG_SHIP_FUEL, ship.fuelUnits());
		CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
		if (player != null) {
			ServerPlayNetworking.send(
				player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_COUNTDOWN_PING, 0.65f, 1.35f)
			);
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.flight_director_bound",
					ship.fuelUnits(),
					(int) WormholeLinkCalculator.MAX_RELOCATION_DISTANCE
				).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				false
			);
		}
	}

	public static UUID getBoundShipUuid(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		CompoundTag tag = data.copyTag();
		return tag.hasUUID(TAG_SHIP_UUID) ? tag.getUUID(TAG_SHIP_UUID) : null;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		if (level.isClientSide) {
			return InteractionResult.sidedSuccess(true);
		}
		if (!(level instanceof ServerLevel serverLevel) || !(ctx.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.PASS;
		}

		ItemStack stack = ctx.getItemInHand();
		UUID shipId = getBoundShipUuid(stack);
		if (shipId == null) {
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.flight_director_unbound").withStyle(ChatFormatting.YELLOW),
				false
			);
			return InteractionResult.CONSUME;
		}

		Entity entity = serverLevel.getEntity(shipId);
		if (!(entity instanceof InterstellarShipEntity ship) || ship.isRemoved()) {
			ServerPlayNetworking.send(
				player, new ClientSoundEventPayload(ClientSoundEventPayload.EVENT_ABORT_ALARM, 0.65f, 0.95f)
			);
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.flight_director_ship_missing").withStyle(ChatFormatting.RED),
				false
			);
			return InteractionResult.CONSUME;
		}

		BlockPos clicked = ctx.getClickedPos();
		BlockPos landingBase = clicked;
		if (ctx.getClickedFace() == Direction.UP || !level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()) {
			BlockPos above = clicked.relative(ctx.getClickedFace());
			if (level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
				landingBase = ctx.getClickedFace() == Direction.UP ? clicked.above() : above;
			}
		}

		BlockState baseState = level.getBlockState(landingBase);
		double targetY = baseState.getCollisionShape(level, landingBase).isEmpty()
			? landingBase.getY()
			: landingBase.getY() + 1.0;

		if (ship.tryStartRelocationFlight(player, landingBase.getX() + 0.5, targetY, landingBase.getZ() + 0.5)) {
			CompoundTag updated = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			updated.putInt(TAG_SHIP_FUEL, ship.fuelUnits());
			CustomData.set(DataComponents.CUSTOM_DATA, stack, updated);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data != null && data.copyTag().hasUUID(TAG_SHIP_UUID)) {
			int fuel = data.copyTag().getInt(TAG_SHIP_FUEL);
			tooltip.add(
				Component.translatable("tooltip.galacticpurpose.flight_director_bound", fuel)
					.withStyle(ChatFormatting.AQUA)
			);
			tooltip.add(
				Component.translatable("tooltip.galacticpurpose.flight_director_use_target")
					.withStyle(ChatFormatting.GRAY)
			);
		} else {
			tooltip.add(
				Component.translatable("tooltip.galacticpurpose.flight_director_empty")
					.withStyle(ChatFormatting.GRAY)
			);
		}
	}
}

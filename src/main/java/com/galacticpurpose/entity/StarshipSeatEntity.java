package com.galacticpurpose.entity;

import com.galacticpurpose.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sittable Captain's Command Chair in front of the Bridge Helm inside orbital_transit
 * so players floating in Zero-G can lock into position cleanly for deep-space viewing & [G] descent.
 */
public class StarshipSeatEntity extends Entity {
	public StarshipSeatEntity(EntityType<? extends StarshipSeatEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public static StarshipSeatEntity spawnAt(Level level, double x, double y, double z, float yawDeg) {
		StarshipSeatEntity seat = new StarshipSeatEntity(ModEntities.STARSHIP_SEAT, level);
		seat.setPos(x, y, z);
		seat.setYRot(yawDeg);
		seat.yRotO = yawDeg;
		level.addFreshEntity(seat);
		return seat;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!this.level().isClientSide && !this.isVehicle()) {
			player.setYRot(this.getYRot());
			player.setDeltaMovement(Vec3.ZERO);
			player.startRiding(this, true);
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.seat_locked").withStyle(ChatFormatting.AQUA),
				true
			);
		}
		return InteractionResult.sidedSuccess(this.level().isClientSide);
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		return new Vec3(this.getX(), this.getY() + 0.42, this.getZ());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}
}

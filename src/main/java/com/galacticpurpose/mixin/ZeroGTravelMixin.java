package com.galacticpurpose.mixin;

import com.galacticpurpose.ship.OrbitalTransitManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * True Newtonian microgravity (0.00g) physics inside the orbital_transit Starship cabin:
 * - Zero gravity pull & conserved vacuum momentum (speed decays minimally ~0.996/tick).
 * - Minimal mid-air thrusters ("hard to steer in mid-float") — strong pushes happen when kicking off floor/walls.
 * - Elastic action-reaction bounce off skylight ceilings, glass walls, and floors (holding [Shift] engages magnetic boots).
 */
@Mixin(LivingEntity.class)
public abstract class ZeroGTravelMixin {
	@Shadow
	protected boolean jumping;

	@Unique
	private static boolean galacticpurpose$isZeroGCabinPlayer(LivingEntity self) {
		return self instanceof Player p
				&& p.level().dimension().equals(OrbitalTransitManager.ORBITAL_TRANSIT_KEY)
				&& !p.isPassenger()
				&& !p.getAbilities().flying
				&& !p.isSpectator()
				&& !p.isInWater();
	}

	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void galacticpurpose$zeroGJumpImpulse(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!galacticpurpose$isZeroGCabinPlayer(self)) {
			return;
		}
		Vec3 cur = self.getDeltaMovement();
		Vec3 look = self.getLookAngle();
		self.setDeltaMovement(cur.x + look.x * 0.04, 0.28, cur.z + look.z * 0.04);
		self.hasImpulse = true;
		ci.cancel();
	}

	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void galacticpurpose$zeroGNewtonianDrift(Vec3 travelVector, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!galacticpurpose$isZeroGCabinPlayer(self)) {
			return;
		}
		self.resetFallDistance();

		boolean onSurface = self.onGround() || self.horizontalCollision || self.verticalCollision;
		if (self.isShiftKeyDown() && onSurface) {
			// Magnetic boots lock astronaut smoothly onto deck floor or bulkheads
			self.setDeltaMovement(self.getDeltaMovement().scale(0.55));
		} else if (onSurface) {
			self.moveRelative(0.055f, travelVector);
			if (this.jumping && self.verticalCollisionBelow) {
				Vec3 cur = self.getDeltaMovement();
				self.setDeltaMovement(cur.x, Math.max(cur.y, 0.26), cur.z);
			}
		} else {
			// Minimal mid-air EVA push -> preserves drifted inertia without snappy air-walking!
			self.moveRelative(0.0045f, travelVector);
		}

		Vec3 preMove = self.getDeltaMovement();
		self.move(MoverType.SELF, preMove);
		Vec3 postMove = self.getDeltaMovement();

		double vx = postMove.x;
		double vy = postMove.y;
		double vz = postMove.z;

		// Action-reaction elastic rebound off ceiling skylight, floor deck, and viewports!
		if (self.verticalCollision && !self.verticalCollisionBelow && Math.abs(preMove.y) > 0.04) {
			vy = -Mth.clamp(Math.abs(preMove.y) * 0.78, 0.16, 0.52);
		} else if (self.verticalCollisionBelow && Math.abs(preMove.y) > 0.085 && !self.isShiftKeyDown()) {
			vy = Mth.clamp(Math.abs(preMove.y) * 0.68, 0.10, 0.45);
		}

		if (self.horizontalCollision) {
			if (Math.abs(preMove.x) > 0.032 && Math.abs(postMove.x) < Math.abs(preMove.x) * 0.35) {
				vx = -Mth.clamp(preMove.x * 0.74, -0.45, 0.45);
			}
			if (Math.abs(preMove.z) > 0.032 && Math.abs(postMove.z) < Math.abs(preMove.z) * 0.35) {
				vz = -Mth.clamp(preMove.z * 0.74, -0.45, 0.45);
			}
		}

		// Conserve inertial velocity cleanly across free-fall cabin space
		double damp = (self.onGround() && self.isShiftKeyDown()) ? 0.65 : 0.996;
		vx = Math.abs(vx * damp) < 1.0e-4 ? 0.0 : vx * damp;
		vy = Math.abs(vy * 0.996) < 1.0e-4 ? 0.0 : vy * 0.996;
		vz = Math.abs(vz * damp) < 1.0e-4 ? 0.0 : vz * damp;

		self.setDeltaMovement(vx, vy, vz);
		self.calculateEntityAnimation(false);
		ci.cancel();
	}
}

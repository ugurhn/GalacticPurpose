package com.galacticpurpose.client.mixin;

import com.galacticpurpose.entity.InterstellarShipEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Pulls the third-person camera clear of the 16x11 Horizon Lander hull so F5 exterior
 * flight views frame the wings, thruster plumes, and horizon cleanly without clipping inside.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private Entity entity;

	@ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
	private float galacticpurpose$expandShipCameraZoom(float maxZoom) {
		if (this.entity != null && this.entity.getVehicle() instanceof InterstellarShipEntity) {
			return Math.max(maxZoom, 17.5F);
		}
		return maxZoom;
	}
}

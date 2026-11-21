package com.galacticpurpose.client.render;

import com.galacticpurpose.ship.OrbitalShipCabinBuilder;
import com.galacticpurpose.ship.OrbitalTransitManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Renders roaring twin plasma thruster plumes trailing behind the walkable Starship Deck in orbital_transit
 * so pressing F5 (third-person) or looking back out the stern glass shows live glowing engines in deep space!
 */
public final class OrbitalShipPlumeRenderer {
	private OrbitalShipPlumeRenderer() {
	}

	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !mc.level.dimension().equals(OrbitalTransitManager.ORBITAL_TRANSIT_KEY)) {
			return;
		}
		MultiBufferSource buffers = context.consumers();
		PoseStack poseStack = context.matrixStack();
		if (buffers == null || poseStack == null) {
			return;
		}

		// Snap to the player's current multiplayer slot center (multiple of 256)
		int slotX = Math.round((float) mc.player.getX() / OrbitalShipCabinBuilder.SLOT_STRIDE) * OrbitalShipCabinBuilder.SLOT_STRIDE;
		Vec3 cam = context.camera().getPosition();
		float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
		float time = (mc.level.getGameTime() + partial) * 0.05f;
		float flicker = 0.90f + 0.10f * Mth.sin(time * 36.0f);

		poseStack.pushPose();
		poseStack.translate(slotX + 0.5 - cam.x, OrbitalShipCabinBuilder.CABIN_Y + 1.8 - cam.y, -10.5 - cam.z);

		for (float nx : new float[]{-4.5f, 4.5f}) {
			SphereMeshHelper.drawMultiLayerBeam(
				poseStack, buffers,
				nx, 0.0f, 0.0f,
				nx, 0.0f, -15.5f * flicker,
				1.35f, 56, 189, 248, 190, time * 2.5f
			);
			SphereMeshHelper.drawMultiLayerBeam(
				poseStack, buffers,
				nx, 0.0f, 0.0f,
				nx, 0.0f, -8.5f * flicker,
				0.65f, 224, 248, 255, 225, time * 4.0f
			);
		}
		poseStack.popPose();
	}
}

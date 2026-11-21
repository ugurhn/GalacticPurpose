package com.galacticpurpose.client.render;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.ship.StarshipFlightPhysics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 3D Horizon Lander renderer (1.9x scale = ~16x11 blocks):
 * - Single-buffer pass separation so every metallic hull, ribbed wing, viewport glass, centered
 *   thruster nozzle, and vectoring pod renders crisply without UV shear or black-magenta seams.
 * - First-person Pilot Cockpit mode strips obstructing dorsal walls above seat height and draws
 *   an illuminated bridge console, panoramic non-sheared rectangular canopy, and open fore-nose.
 * - Articulated gimbal thruster pods swivel smoothly downward (0 -> 92 deg) during landing flare.
 */
public class InterstellarShipRenderer extends EntityRenderer<InterstellarShipEntity> {
	private static final float SHIP_SCALE = 1.90f;

	private static final ResourceLocation TEX_HULL = GalacticPurpose.id("textures/block/starship_hull_plating.png");
	private static final ResourceLocation TEX_RIBBED = GalacticPurpose.id("textures/block/starship_ribbed_panel.png");
	private static final ResourceLocation TEX_HEATSHIELD = GalacticPurpose.id("textures/block/zero_k_plating.png");
	private static final ResourceLocation TEX_GLASS = GalacticPurpose.id("textures/block/reinforced_viewport_glass.png");
	private static final ResourceLocation TEX_THRUSTER_SIDE = GalacticPurpose.id("textures/block/starship_thruster_side.png");
	private static final ResourceLocation TEX_THRUSTER_END = GalacticPurpose.id("textures/block/starship_thruster_bottom.png");
	private static final ResourceLocation TEX_LIGHT = GalacticPurpose.id("textures/block/starship_light_strip.png");
	private static final ResourceLocation TEX_TITANIUM = GalacticPurpose.id("textures/block/ark_titanium_plating.png");
	private static final ResourceLocation TEX_HELM = GalacticPurpose.id("textures/block/starship_helm_front.png");

	public InterstellarShipRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 4.2f;
	}

	@Override
	public boolean shouldRender(InterstellarShipEntity entity, Frustum frustum, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void render(
			InterstellarShipEntity ship,
			float entityYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource buffers,
			int packedLight
	) {
		Minecraft mc = Minecraft.getInstance();
		boolean firstPersonPilot = mc.options.getCameraType().isFirstPerson()
				&& mc.player != null
				&& ship == mc.player.getVehicle();

		int fullBright = 0x00F000F0;
		float legDeploy = ship.legDeploy();
		float thrust = ship.thrustVisual();
		float yaw = Mth.rotLerp(partialTick, ship.yRotO, ship.getYRot());
		float pitchDeg = Mth.lerp(partialTick, ship.clientPitchO(), ship.pitchDeg());
		float rollDeg = Mth.lerp(partialTick, ship.clientRollO(), ship.rollDeg());
		float vectorDeg = ship.syncedVectorAngle();
		int mode = ship.shipMode();

		poseStack.pushPose();
		poseStack.scale(SHIP_SCALE, SHIP_SCALE, SHIP_SCALE);
		poseStack.translate(0.0f, 1.25f, 0.0f);
		poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(-pitchDeg));
		poseStack.mulPose(Axis.ZP.rotationDegrees(rollDeg));
		poseStack.translate(0.0f, -1.25f, 0.0f);

		// =========================================================================
		// PASS 1: White/Silver Composite Main Fuselage & Aerodynamic Lifting Body
		// =========================================================================
		{
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_HULL));
			PoseStack.Pose p = poseStack.last();

			// Main belly keel deck (always visible)
			drawTexturedBox(vc, p, -1.70f, 0.72f, -2.55f, 1.70f, 1.42f, 2.00f, packedLight, 232, 236, 242);

			// Upper hull: full sealed outer carapace in 3rd-person; enclosed rear cabin + canopy frame behind seat in 1st-person!
			if (!firstPersonPilot) {
				drawTexturedBox(vc, p, -1.62f, 1.42f, -2.25f, 1.62f, 1.96f, 1.55f, packedLight, 224, 228, 236);
				drawTexturedBox(vc, p, -0.82f, 1.96f, -1.75f, 0.82f, 2.18f, 1.35f, packedLight, 240, 244, 250);
			} else {
				// Enclosed pressurized rear bulkhead & overhead cockpit canopy ceiling (pilot eye sits clear at y=1.87, z=1.85!)
				drawTexturedBox(vc, p, -1.62f, 1.42f, -2.25f, 1.62f, 2.26f, 1.08f, packedLight, 165, 174, 188);
				drawTexturedBox(vc, p, -1.62f, 1.42f, 1.08f, -1.36f, 2.38f, 1.65f, packedLight, 188, 196, 210);
				drawTexturedBox(vc, p, 1.36f, 1.42f, 1.08f, 1.62f, 2.38f, 1.65f, packedLight, 188, 196, 210);
				drawTexturedBox(vc, p, -1.36f, 2.28f, 1.08f, 1.36f, 2.44f, 1.65f, packedLight, 198, 206, 220);
			}

			// Sleek aerodynamic nose bevel & side shoulders (frames true rectangular windshield cleanly!)
			drawWedge(vc, p,
					-1.65f, 0.72f, 2.00f, 1.65f, 1.42f,
					-1.26f, 0.88f, 3.85f, 1.26f, 1.38f,
					packedLight, 222, 226, 234);
			drawWedge(vc, p,
					-1.26f, 0.88f, 3.85f, 1.26f, 1.38f,
					-0.55f, 0.96f, 4.78f, 0.55f, 1.18f,
					packedLight, 208, 212, 220);

			// Canopy left/right armored cheek pillars (keeps 1st-person A-pillar framing crisp)
			drawTexturedBox(vc, p, -1.55f, 1.42f, 1.45f, -1.25f, 2.16f, 2.05f, packedLight, 205, 210, 218);
			drawTexturedBox(vc, p, 1.25f, 1.42f, 1.45f, 1.55f, 2.16f, 2.05f, packedLight, 205, 210, 218);

			// Wing leading-edge aerodynamic strakes
			drawWedge(vc, p,
					-2.35f, 0.78f, -2.15f, -1.70f, 1.26f,
					-1.68f, 0.90f, 1.85f, -1.25f, 1.20f,
					packedLight, 216, 220, 228);
			drawWedge(vc, p,
					1.70f, 0.78f, -2.15f, 2.35f, 1.26f,
					1.25f, 0.90f, 1.85f, 1.68f, 1.20f,
					packedLight, 216, 220, 228);

			// Dorsal tail spine (hidden only in 1st-person close region behind seat)
			if (!firstPersonPilot) {
				drawTexturedBox(vc, p, -0.32f, 1.98f, -2.42f, 0.32f, 2.30f, -1.25f, packedLight, 214, 218, 226);
			}
		}

		// =========================================================================
		// PASS 2: Ribbed Swept Delta Wings & Twin Outward Vertical Stabilizers
		// =========================================================================
		{
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_RIBBED));
			PoseStack.Pose p = poseStack.last();

			drawWedge(vc, p,
					-4.10f, 0.78f, -2.15f, -1.68f, 1.06f,
					-2.35f, 0.86f, 0.85f, -1.35f, 1.04f,
					packedLight, 218, 222, 230);
			drawWedge(vc, p,
					1.68f, 0.78f, -2.15f, 4.10f, 1.06f,
					1.35f, 0.86f, 0.85f, 2.35f, 1.04f,
					packedLight, 218, 222, 230);

			// Twin angled canted tail fins
			emitQuadOutward(vc, p,
					-3.55f, 1.04f, -2.15f, -3.55f, 1.04f, -0.95f,
					-3.85f, 2.30f, -1.65f, -3.85f, 2.30f, -2.35f,
					-0.95f, 0.25f, 0.0f, 2.0f, 1.5f, packedLight, 222, 226, 234);
			emitQuadOutward(vc, p,
					-3.42f, 1.04f, -0.95f, -3.42f, 1.04f, -2.15f,
					-3.72f, 2.30f, -2.35f, -3.72f, 2.30f, -1.65f,
					0.95f, -0.25f, 0.0f, 2.0f, 1.5f, packedLight, 200, 204, 214);
			emitQuadOutward(vc, p,
					3.55f, 1.04f, -0.95f, 3.55f, 1.04f, -2.15f,
					3.85f, 2.30f, -2.35f, 3.85f, 2.30f, -1.65f,
					0.95f, 0.25f, 0.0f, 2.0f, 1.5f, packedLight, 222, 226, 234);
			emitQuadOutward(vc, p,
					3.42f, 1.04f, -2.15f, 3.42f, 1.04f, -0.95f,
					3.72f, 2.30f, -1.65f, 3.72f, 2.30f, -2.35f,
					-0.95f, -0.25f, 0.0f, 2.0f, 1.5f, packedLight, 200, 204, 214);
		}

		// =========================================================================
		// PASS 3: Thermal Re-entry Heatshield Tiles & Nose Cap
		// =========================================================================
		{
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_HEATSHIELD));
			PoseStack.Pose p = poseStack.last();

			drawTexturedBox(vc, p, -1.62f, 0.62f, -2.45f, 1.62f, 0.72f, 1.95f, packedLight, 210, 215, 222);
			drawWedge(vc, p,
					-1.52f, 0.62f, 1.95f, 1.52f, 0.72f,
					-0.52f, 0.88f, 4.78f, 0.52f, 0.96f,
					packedLight, 205, 210, 218);
			// Dark charcoal armored nose cone tip
			drawTexturedBox(vc, p, -0.50f, 0.96f, 4.74f, 0.50f, 1.16f, 5.00f, packedLight, 70, 74, 82);
		}

		// =========================================================================
		// PASS 4: Canopy Windshield (Cutout Frame in 1st-Person so Wormholes Stay 100% Visible!)
		// =========================================================================
		if (!firstPersonPilot) {
			VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX_GLASS));
			PoseStack.Pose p = poseStack.last();

			float glassTopY = 2.16f;
			float glassTopZ = 1.55f;
			emitQuadOutwardFixedUv(vc, p,
					-1.24f, 1.44f, 3.16f,
					1.24f, 1.44f, 3.16f,
					1.24f, glassTopY, glassTopZ,
					-1.24f, glassTopY, glassTopZ,
					0.0f, 0.48f, 0.88f,
					0.02f, 0.02f, 2.98f, 0.98f,
					fullBright, 205, 240, 255, 215);

			// True planar right/left triangular canopy side glass (zero twisted UV quads!)
			emitQuadOutwardFixedUv(vc, p,
					1.24f, 1.44f, 3.16f,
					1.24f, 1.44f, glassTopZ,
					1.24f, glassTopY, glassTopZ,
					1.24f, glassTopY, glassTopZ,
					1.0f, 0.0f, 0.0f,
					0.02f, 0.02f, 0.98f, 0.98f,
					fullBright, 195, 232, 252, 205);
			emitQuadOutwardFixedUv(vc, p,
					-1.24f, 1.44f, glassTopZ,
					-1.24f, 1.44f, 3.16f,
					-1.24f, glassTopY, glassTopZ,
					-1.24f, glassTopY, glassTopZ,
					-1.0f, 0.0f, 0.0f,
					0.02f, 0.02f, 0.98f, 0.98f,
					fullBright, 195, 232, 252, 205);
		} else {
			// Cutout metallic cockpit bezel bars only — zero depth-writing translucent fill so 3D Wormhole Cores
			// and plasma jets remain 100% luminous and clear from inside the pilot seat!
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_TITANIUM));
			PoseStack.Pose p = poseStack.last();
			// Front canopy bottom sill & top header bar
			drawTexturedBox(vc, p, -1.28f, 1.40f, 3.12f, 1.28f, 1.47f, 3.22f, packedLight, 75, 84, 98);
			drawTexturedBox(vc, p, -1.28f, 2.34f, 1.50f, 1.28f, 2.42f, 1.60f, packedLight, 75, 84, 98);
			// Sloped left & right armored A-pillar canopy struts
			for (float side : new float[]{-1.24f, 1.18f}) {
				emitQuadOutward(vc, p,
						side, 1.44f, 3.16f, side + 0.06f, 1.44f, 3.16f,
						side + 0.06f, 2.38f, 1.54f, side, 2.38f, 1.54f,
						0.0f, 0.5f, 0.86f, 0.2f, 2.0f, packedLight, 88, 98, 112);
			}
		}

		// =========================================================================
		// PASS 4B: 1st-Person Interior Flight Deck & Illuminated Helm Console
		// =========================================================================
		if (firstPersonPilot) {
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_HELM));
			PoseStack.Pose p = poseStack.last();
			// Angled crisp bridge display panel directly below glass sill in cockpit
			emitQuadOutwardFixedUv(vc, p,
					-0.68f, 1.24f, 2.52f,
					0.68f, 1.24f, 2.52f,
					0.68f, 1.54f, 2.86f,
					-0.68f, 1.54f, 2.86f,
					0.0f, 0.65f, -0.76f,
					0.0f, 0.0f, 1.0f, 1.0f,
					fullBright, 255, 255, 255, 255);
		}

		// =========================================================================
		// PASS 5 & 6: Articulated Vectoring Aft Thruster Pods & Centered Nozzles
		// =========================================================================
		float[] thrusterXs = {-2.55f, -1.05f, 1.05f, 2.55f};
		poseStack.pushPose();
		poseStack.translate(0.0f, 0.92f, -2.35f);
		poseStack.mulPose(Axis.XP.rotationDegrees(-vectorDeg));
		poseStack.translate(0.0f, -0.92f, 2.35f);

		{
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_THRUSTER_SIDE));
			PoseStack.Pose p = poseStack.last();
			for (float tx : thrusterXs) {
				drawTexturedBox(vc, p, tx - 0.56f, 0.66f, -3.02f, tx + 0.56f, 1.24f, -2.02f, packedLight, 224, 228, 236);
			}
		}

		{
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_THRUSTER_END));
			PoseStack.Pose p = poseStack.last();
			int glow = (thrust > 0.05f) ? fullBright : packedLight;
			for (float tx : thrusterXs) {
				// Normalized 0.04..0.96 UVs -> circular thruster core is 100% centered on every pod!
				emitQuadOutwardFixedUv(vc, p,
						tx - 0.52f, 0.68f, -3.03f,
						tx + 0.52f, 0.68f, -3.03f,
						tx + 0.52f, 1.22f, -3.03f,
						tx - 0.52f, 1.22f, -3.03f,
						0.0f, 0.0f, -1.0f,
						0.04f, 0.04f, 0.96f, 0.96f,
						glow, 255, 255, 255, 255);
			}
		}
		poseStack.popPose();

		// =========================================================================
		// PASS 7: Cyan Aerodynamic Trim Lights, Engine Burn Strips & Docking Hatch
		// =========================================================================
		{
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_LIGHT));
			PoseStack.Pose p = poseStack.last();
			// Cockpit visor A-pillar & canopy header accent glow
			drawTexturedBox(vc, p, -1.26f, 1.42f, 3.16f, 1.26f, 1.48f, 3.22f, fullBright, 210, 245, 255);
			drawTexturedBox(vc, p, -2.65f, 0.98f, -0.65f, -2.55f, 1.07f, 0.45f, fullBright, 210, 245, 255);
			drawTexturedBox(vc, p, 2.55f, 0.98f, -0.65f, 2.65f, 1.07f, 0.45f, fullBright, 210, 245, 255);
			if (!firstPersonPilot) {
				drawTexturedBox(vc, p, -0.58f, 2.18f, -0.95f, 0.58f, 2.23f, 0.85f, fullBright, 185, 240, 255);
			}
		}

		// =========================================================================
		// PASS 8: Animated Deployable Landing Skids & Hydro-Pontoons
		// =========================================================================
		if (legDeploy > 0.04f) {
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_TITANIUM));
			PoseStack.Pose p = poseStack.last();
			float dropY = 0.65f * legDeploy;
			float skidY = 0.62f - dropY;

			float[][] legs = {
					{-1.25f, 2.05f, -1.25f, 2.25f},
					{1.25f, 2.05f, 1.25f, 2.25f},
					{-2.25f, -1.60f, -2.45f, -1.85f},
					{2.25f, -1.60f, 2.45f, -1.85f}
			};
			for (float[] leg : legs) {
				drawTexturedBox(vc, p,
						Math.min(leg[0], leg[2]) - 0.12f, skidY + 0.08f, Math.min(leg[1], leg[3]) - 0.12f,
						Math.max(leg[0], leg[2]) + 0.12f, 0.72f, Math.max(leg[1], leg[3]) + 0.12f,
						packedLight, 140, 150, 165);
				drawTexturedBox(vc, p,
						leg[2] - 0.34f, skidY - 0.04f, leg[3] - 0.52f,
						leg[2] + 0.34f, skidY + 0.08f, leg[3] + 0.52f,
						packedLight, 195, 205, 220);
			}

			if (ship.hydroPontoon()) {
				drawTexturedBox(vc, p, -2.65f, skidY - 0.08f, -2.20f, -2.05f, skidY + 0.16f, 2.35f, packedLight, 95, 195, 230);
				drawTexturedBox(vc, p, 2.05f, skidY - 0.08f, -2.20f, 2.65f, skidY + 0.16f, 2.35f, packedLight, 95, 195, 230);
			}
		}

		// =========================================================================
		// PASS 9: Lean Vectoring Plasma Plumes (reduced segments for silky FPS)
		// =========================================================================
		if (thrust > 0.02f) {
			VertexConsumer glowVc = buffers.getBuffer(RenderType.lightning());
			float flicker = 0.88f + 0.22f * Mth.sin((ship.tickCount + partialTick) * 1.45f);
			float plumeLen = (mode == InterstellarShipEntity.MODE_COUNTDOWN ? 1.8f : 4.2f) * thrust * flicker;
			int plumeAlpha = mode == InterstellarShipEntity.MODE_COUNTDOWN ? 165 : 225;

			poseStack.pushPose();
			poseStack.translate(0.0f, 0.92f, -2.35f);
			poseStack.mulPose(Axis.XP.rotationDegrees(-vectorDeg));
			poseStack.translate(0.0f, -0.92f, 2.35f);

			Matrix4f mat = poseStack.last().pose();
			for (float tx : thrusterXs) {
				drawLeanPlume(glowVc, mat, tx, 0.95f, -3.05f, plumeLen, 0.42f, 110, 220, 255, plumeAlpha);
				drawLeanPlume(glowVc, mat, tx, 0.95f, -3.05f, plumeLen * 0.58f, 0.22f, 255, 250, 235, 240);
			}
			poseStack.popPose();
		}

		poseStack.popPose();
		super.render(ship, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private static void drawLeanPlume(
			VertexConsumer vc, Matrix4f mat,
			float cx, float cy, float cz,
			float length, float baseR,
			int r, int g, int b, int a
	) {
		int segs = 8;
		for (int i = 0; i < segs; i++) {
			float a0 = (float) (2.0 * Math.PI * i / segs);
			float a1 = (float) (2.0 * Math.PI * (i + 1) / segs);
			float x0 = cx + Mth.cos(a0) * baseR;
			float y0 = cy + Mth.sin(a0) * baseR;
			float x1 = cx + Mth.cos(a1) * baseR;
			float y1 = cy + Mth.sin(a1) * baseR;
			float tipR = baseR * 0.14f;
			float tx0 = cx + Mth.cos(a0) * tipR;
			float ty0 = cy + Mth.sin(a0) * tipR;
			float tx1 = cx + Mth.cos(a1) * tipR;
			float ty1 = cy + Mth.sin(a1) * tipR;
			vc.addVertex(mat, x0, y0, cz).setColor(r, g, b, a);
			vc.addVertex(mat, x1, y1, cz).setColor(r, g, b, a);
			vc.addVertex(mat, tx1, ty1, cz - length).setColor(r, g, b, 20);
			vc.addVertex(mat, tx0, ty0, cz - length).setColor(r, g, b, 20);
		}
	}

	private static void drawTexturedBox(
			VertexConsumer vc, PoseStack.Pose pose,
			float x0, float y0, float z0, float x1, float y1, float z1,
			int light, int r, int g, int b
	) {
		float w = Math.max(0.25f, Math.abs(x1 - x0));
		float h = Math.max(0.25f, Math.abs(y1 - y0));
		float d = Math.max(0.25f, Math.abs(z1 - z0));

		emitQuadOutward(vc, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, w, d, light, r, g, b);
		emitQuadOutward(vc, pose, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, w, d, light,
				Math.round(r * 0.72f), Math.round(g * 0.72f), Math.round(b * 0.72f));
		emitQuadOutward(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, w, h, light,
				Math.round(r * 0.90f), Math.round(g * 0.90f), Math.round(b * 0.90f));
		emitQuadOutward(vc, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, w, h, light,
				Math.round(r * 0.84f), Math.round(g * 0.84f), Math.round(b * 0.84f));
		emitQuadOutward(vc, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, d, h, light,
				Math.round(r * 0.88f), Math.round(g * 0.88f), Math.round(b * 0.88f));
		emitQuadOutward(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, d, h, light,
				Math.round(r * 0.82f), Math.round(g * 0.82f), Math.round(b * 0.82f));
	}

	private static void drawWedge(
			VertexConsumer vc, PoseStack.Pose pose,
			float bx0, float by0, float z0, float bx1, float by1,
			float fx0, float fy0, float z1, float fx1, float fy1,
			int light, int r, int g, int b
	) {
		float wBack = Math.max(0.25f, Math.abs(bx1 - bx0));
		float hBack = Math.max(0.25f, Math.abs(by1 - by0));
		float wFront = Math.max(0.25f, Math.abs(fx1 - fx0));
		float hFront = Math.max(0.25f, Math.abs(fy1 - fy0));
		float len = Math.max(0.25f, Math.abs(z1 - z0));

		emitQuadOutward(vc, pose, bx0, by1, z0, fx0, fy1, z1, fx1, fy1, z1, bx1, by1, z0, 0, 1, 0.25f, wBack, len, light, r, g, b);
		emitQuadOutward(vc, pose, fx0, fy0, z1, bx0, by0, z0, bx1, by0, z0, fx1, fy0, z1, 0, -1, 0, wBack, len, light,
				Math.round(r * 0.72f), Math.round(g * 0.72f), Math.round(b * 0.72f));
		emitQuadOutward(vc, pose, fx0, fy0, z1, fx1, fy0, z1, fx1, fy1, z1, fx0, fy1, z1, 0, 0, 1, wFront, hFront, light,
				Math.round(r * 0.90f), Math.round(g * 0.90f), Math.round(b * 0.90f));
		emitQuadOutward(vc, pose, bx1, by0, z0, bx0, by0, z0, bx0, by1, z0, bx1, by1, z0, 0, 0, -1, wBack, hBack, light,
				Math.round(r * 0.84f), Math.round(g * 0.84f), Math.round(b * 0.84f));
		emitQuadOutward(vc, pose, fx1, fy0, z1, bx1, by0, z0, bx1, by1, z0, fx1, fy1, z1, 1, 0, 0.2f, len, hBack, light,
				Math.round(r * 0.90f), Math.round(g * 0.90f), Math.round(b * 0.90f));
		emitQuadOutward(vc, pose, bx0, by0, z0, fx0, fy0, z1, fx0, fy1, z1, bx0, by1, z0, -1, 0, 0.2f, len, hBack, light,
				Math.round(r * 0.82f), Math.round(g * 0.82f), Math.round(b * 0.82f));
	}

	private static void emitQuadOutward(
			VertexConsumer vc, PoseStack.Pose pose,
			float x0, float y0, float z0,
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3,
			float nx, float ny, float nz,
			float uMax, float vMax, int light,
			int r, int g, int b
	) {
		emitQuadOutwardFixedUv(vc, pose, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
				nx, ny, nz, 0.0f, 0.0f, uMax, vMax, light, r, g, b, 255);
	}

	private static void emitQuadOutwardFixedUv(
			VertexConsumer vc, PoseStack.Pose pose,
			float x0, float y0, float z0,
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3,
			float nx, float ny, float nz,
			float u0, float v0, float u1, float v1, int light,
			int r, int g, int b, int a
	) {
		float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
		float unx = len > 0.0001f ? nx / len : 0.0f;
		float uny = len > 0.0001f ? ny / len : 1.0f;
		float unz = len > 0.0001f ? nz / len : 0.0f;

		float dot = StarshipFlightPhysics.outwardWindingDot(x0, y0, z0, x1, y1, z1, x2, y2, z2, unx, uny, unz);
		if (dot >= 0.0f) {
			addV(vc, pose, x0, y0, z0, u0, v1, unx, uny, unz, light, r, g, b, a);
			addV(vc, pose, x1, y1, z1, u1, v1, unx, uny, unz, light, r, g, b, a);
			addV(vc, pose, x2, y2, z2, u1, v0, unx, uny, unz, light, r, g, b, a);
			addV(vc, pose, x3, y3, z3, u0, v0, unx, uny, unz, light, r, g, b, a);
		} else {
			addV(vc, pose, x3, y3, z3, u0, v0, unx, uny, unz, light, r, g, b, a);
			addV(vc, pose, x2, y2, z2, u1, v0, unx, uny, unz, light, r, g, b, a);
			addV(vc, pose, x1, y1, z1, u1, v1, unx, uny, unz, light, r, g, b, a);
			addV(vc, pose, x0, y0, z0, u0, v1, unx, uny, unz, light, r, g, b, a);
		}
	}

	private static void addV(
			VertexConsumer vc, PoseStack.Pose pose,
			float x, float y, float z, float u, float v,
			float nx, float ny, float nz, int light,
			int r, int g, int b, int a
	) {
		vc.addVertex(pose, x, y, z)
				.setColor(r, g, b, a)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
	}

	@Override
	public ResourceLocation getTextureLocation(InterstellarShipEntity entity) {
		return TEX_HULL;
	}
}

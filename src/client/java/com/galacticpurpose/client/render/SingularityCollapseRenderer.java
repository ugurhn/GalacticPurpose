package com.galacticpurpose.client.render;

import com.galacticpurpose.entity.SingularityCollapseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders the apocalyptic 10x growing Singularity Collapse & Draconic-scale 3D Shockwave:
 * - Phases 1-3: Colossal growing pitch-black event horizon (up to 76 blocks wide!), fracturing geodesic cage,
 *   soft-feathered accretion disk, AND 144-FPS world-devouring gravitational siphon streams sucking ground chunks straight in!
 * - Phase 4: Expanding Draconic-scale 3D multi-layered shockwave dome, Mach torus ring, and stratospheric polar gamma jets!
 */
public class SingularityCollapseRenderer extends EntityRenderer<SingularityCollapseEntity> {

	public SingularityCollapseRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(SingularityCollapseEntity entity, Frustum frustum, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void render(
		SingularityCollapseEntity entity,
		float entityYaw,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource buffers,
		int packedLight
	) {
		float age = entity.collapseAge() + partialTick;
		float time = age * 0.05f;

		poseStack.pushPose();
		poseStack.translate(0.0, 1.2, 0.0);

		if (age < SingularityCollapseEntity.DETONATION_TICK) {
			renderGrowingSingularity(entity, partialTick, age, time, poseStack, buffers);
		} else {
			renderDraconicShockwaveExplosion(entity, partialTick, time, poseStack, buffers);
		}

		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private static void renderGrowingSingularity(
		SingularityCollapseEntity entity,
		float partialTick,
		float age,
		float time,
		PoseStack poseStack,
		MultiBufferSource buffers
	) {
		float radius = entity.visualCoreRadius(partialTick);
		boolean imploding = age >= SingularityCollapseEntity.VORTEX_PHASE_END;
		boolean deadSilence = age >= SingularityCollapseEntity.DETONATION_TICK - 9;

		// 0. World-Devouring Gravitational Suction Funnel arching from the earth straight up into the growing black hole!
		if (age > 10 && !deadSilence) {
			float groundDrop = Math.min(118.0f, 12.0f + age * 0.36f);
			float groundSpan = Math.min(82.0f, Math.max(24.0f, entity.blastRadius() * 0.78f));
			SphereMeshHelper.drawGravitationalSuctionVortex(
				poseStack, buffers, radius, groundDrop, groundSpan, imploding, time
			);
		}

		float shakeAmp = imploding ? 0.35f : 0.10f;
		poseStack.translate(
			Mth.sin(time * 41.0f) * shakeAmp,
			Mth.cos(time * 47.0f) * shakeAmp,
			Mth.sin(time * 53.0f) * shakeAmp
		);

		if (deadSilence) {
			SphereMeshHelper.drawPlasmaCoreSphere(
				poseStack, buffers, Math.max(0.45f, radius * 2.5f),
				255, 255, 255, 255, 235, 180, 255, time * 12.0f
			);
			return;
		}

		// 1. Pure Pitch-Black Colossal Event Horizon (up to 76 blocks diameter!)
		SphereMeshHelper.drawOpaqueSphere(poseStack, buffers, radius * 0.90f, 0, 0, 0, 255);

		// 2. Blazing Photon Rim Shell
		SphereMeshHelper.drawPlasmaCoreSphere(
			poseStack, buffers, radius * 1.03f,
			255, imploding ? 235 : 130, imploding ? 195 : 32,
			255, 45, 16,
			imploding ? 195 : 95,
			time * 5.0f
		);

		// 3. Fracturing Geodesic Gravity-Field Lattice
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotation(time * (imploding ? 4.8f : 1.4f)));
		poseStack.mulPose(Axis.XP.rotation(time * 0.85f));
		float strut = Math.max(0.05f, radius * 0.018f);
		SphereMeshHelper.drawGeodesicCage(
			poseStack, buffers, radius * 1.15f, strut,
			255, imploding ? 225 : 75, imploding ? 140 : 36,
			imploding ? 210 : 135
		);
		poseStack.popPose();

		// 4. Primary Tilted Relativistic Accretion Disk (strictly outside sphere -> zero slicing!)
		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(14.0f));
		poseStack.mulPose(Axis.XP.rotationDegrees(8.0f));
		poseStack.mulPose(Axis.YP.rotation(-time * (imploding ? 6.0f : 1.9f)));
		SphereMeshHelper.drawSoftAccretionRingXZ(
			poseStack, buffers,
			radius * 1.26f, radius * 1.65f, radius * 2.55f,
			255, 225, 135, 195,
			235, 50, 16,
			time * 4.0f
		);
		SphereMeshHelper.drawSoftAccretionRingXZ(
			poseStack, buffers,
			radius * 1.60f, radius * 2.25f, radius * 3.25f,
			200, 58, 235, 125,
			80, 16, 130,
			-time * 2.6f
		);
		poseStack.popPose();

		// 5. Vertical Polar Relativistic Energy Jets
		if (age > SingularityCollapseEntity.RISE_PHASE_END) {
			float jetLen = radius * (imploding ? 4.2f : 2.5f);
			float jetW = radius * (imploding ? 0.24f : 0.12f);
			SphereMeshHelper.drawMultiLayerBeam(
				poseStack, buffers,
				0.0f, -jetLen, 0.0f, 0.0f, jetLen, 0.0f,
				jetW, 255, imploding ? 210 : 120, imploding ? 140 : 255, 150, time
			);
		}
	}

	private static void renderDraconicShockwaveExplosion(
		SingularityCollapseEntity entity,
		float partialTick,
		float time,
		PoseStack poseStack,
		MultiBufferSource buffers
	) {
		float progress = entity.shockwaveProgress(partialTick);
		float waveRadius = entity.shockwaveRadius(partialTick);
		if (progress >= 1.0f || waveRadius <= 0.1f) {
			return;
		}
		float fade = Mth.clamp(1.0f - progress * progress, 0.0f, 1.0f);
		float coreFade = Mth.clamp(1.0f - progress * 1.45f, 0.0f, 1.0f);

		if (coreFade > 0.01f) {
			SphereMeshHelper.drawPlasmaCoreSphere(
				poseStack, buffers, waveRadius * 0.56f,
				255, 255, 238, 255, 210, 105,
				Math.round(215 * coreFade), time * 6.0f
			);
		}

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotation(time * 0.5f));
		SphereMeshHelper.drawPlasmaCoreSphere(
			poseStack, buffers, waveRadius,
			255, 140, 28, 230, 38, 12,
			Math.round(160 * fade), time * 4.0f
		);
		poseStack.popPose();

		if (progress < 0.72f) {
			float latticeFade = Mth.clamp(1.0f - (progress / 0.72f), 0.0f, 1.0f);
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotation(-time * 0.75f));
			SphereMeshHelper.drawGeodesicCage(
				poseStack, buffers, waveRadius * 1.03f,
				Math.max(0.18f, waveRadius * 0.012f),
				255, 225, 130, Math.round(165 * latticeFade)
			);
			poseStack.popPose();
		}

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotation(time * 0.9f));
		SphereMeshHelper.drawSoftAccretionRingXZ(
			poseStack, buffers,
			waveRadius * 0.75f, waveRadius * 1.15f, waveRadius * 1.65f,
			255, 240, 200, Math.round(195 * fade),
			255, 80, 20,
			time * 5.0f
		);
		poseStack.popPose();

		if (coreFade > 0.01f) {
			float jetHeight = waveRadius * 2.0f + 35.0f;
			float jetWidth = Math.max(0.8f, waveRadius * 0.12f * coreFade);
			SphereMeshHelper.drawMultiLayerBeam(
				poseStack, buffers,
				0.0f, -jetHeight * 0.45f, 0.0f,
				0.0f, jetHeight, 0.0f,
				jetWidth, 255, 235, 175, Math.round(205 * coreFade), time
			);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(SingularityCollapseEntity entity) {
		return SphereMeshHelper.SOLID_WHITE_TEXTURE;
	}
}

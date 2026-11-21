package com.galacticpurpose.client.render;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.entity.StarshipSeatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Clean 3D Captain's Command Chair rendered right behind the orbital Bridge Helm console.
 */
public class StarshipSeatRenderer extends EntityRenderer<StarshipSeatEntity> {
	private static final ResourceLocation TEX_FRAME = GalacticPurpose.id("textures/block/ark_titanium_plating.png");
	private static final ResourceLocation TEX_ACCENT = GalacticPurpose.id("textures/block/starship_light_strip.png");

	public StarshipSeatRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(StarshipSeatEntity entity, Frustum frustum, double cx, double cy, double cz) {
		return true;
	}

	@Override
	public void render(
			StarshipSeatEntity seat,
			float entityYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource buffers,
			int packedLight
	) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(-seat.getYRot()));

		{
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_FRAME));
			PoseStack.Pose p = poseStack.last();
			// Pedestal base bolted to grate deck
			drawBox(vc, p, -0.22f, 0.00f, -0.22f, 0.22f, 0.34f, 0.22f, packedLight, 95, 105, 120);
			// Ergonomic seat cushion
			drawBox(vc, p, -0.40f, 0.34f, -0.36f, 0.40f, 0.50f, 0.38f, packedLight, 56, 66, 82);
			// High contoured backrest (at -Z behind pilot so +Z view stays completely clear!)
			drawBox(vc, p, -0.38f, 0.50f, -0.40f, 0.38f, 1.24f, -0.22f, packedLight, 68, 78, 96);
			// Padded headrest & armrests
			drawBox(vc, p, -0.24f, 1.24f, -0.38f, 0.24f, 1.48f, -0.20f, packedLight, 110, 120, 138);
			drawBox(vc, p, -0.46f, 0.50f, -0.32f, -0.34f, 0.72f, 0.32f, packedLight, 130, 140, 155);
			drawBox(vc, p, 0.34f, 0.50f, -0.32f, 0.46f, 0.72f, 0.32f, packedLight, 130, 140, 155);
		}

		{
			VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_ACCENT));
			PoseStack.Pose p = poseStack.last();
			drawBox(vc, p, -0.24f, 1.32f, -0.19f, 0.24f, 1.38f, -0.16f, 0x00F000F0, 190, 240, 255);
		}

		poseStack.popPose();
		super.render(seat, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private static void drawBox(
			VertexConsumer vc, PoseStack.Pose p,
			float x0, float y0, float z0, float x1, float y1, float z1,
			int light, int r, int g, int b
	) {
		addQuad(vc, p, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, light, r, g, b);
		addQuad(vc, p, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, light, r, g, b);
		addQuad(vc, p, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, light, r, g, b);
		addQuad(vc, p, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, light, r, g, b);
		addQuad(vc, p, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, light, r, g, b);
		addQuad(vc, p, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, light, r, g, b);
	}

	private static void addQuad(
			VertexConsumer vc, PoseStack.Pose p,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3,
			float nx, float ny, float nz, int light, int r, int g, int b
	) {
		vc.addVertex(p, x0, y0, z0).setColor(r, g, b, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
		vc.addVertex(p, x1, y1, z1).setColor(r, g, b, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
		vc.addVertex(p, x2, y2, z2).setColor(r, g, b, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
		vc.addVertex(p, x3, y3, z3).setColor(r, g, b, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
	}

	@Override
	public ResourceLocation getTextureLocation(StarshipSeatEntity entity) {
		return TEX_FRAME;
	}
}

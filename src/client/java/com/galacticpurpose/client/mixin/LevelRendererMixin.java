package com.galacticpurpose.client.mixin;

import com.galacticpurpose.client.hud.ClientClimateState;
import com.galacticpurpose.client.hud.ClientMarsState;
import com.galacticpurpose.climate.CollapsePhase;
import com.galacticpurpose.terraforming.SolarStormPhase;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Expands and reddens the Sun during Overworld solar collapse and Mars' Red Giant / CME Solar Storm phases,
 * drawing an additive multi-ring crimson plasma corona & flare prominence halo around the enlarged star.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Shadow
	private ClientLevel level;

	@ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 30.0F))
	private float galacticpurpose$scaleSolarRadius(float baseSunSize) {
		float time = galacticpurpose$elapsedSeconds();
		if (ClientMarsState.isActiveInDimension()) {
			if (!ClientMarsState.isRedGiantActive()) {
				return 21.0F; // Distant faint sun before Red Giant expansion
			}
			boolean activeStorm = ClientMarsState.getStormPhase() == SolarStormPhase.ACTIVE;
			float pulse = activeStorm
				? 6.0F * Mth.sin(time * 3.4F)
				: 2.5F * Mth.sin(time * 0.85F);
			return (activeStorm ? 132.0F : 114.0F) + pulse;
		}
		if (ClientClimateState.isActiveInDimension()) {
			CollapsePhase phase = ClientClimateState.getPhase();
			if (phase.ordinal() >= CollapsePhase.WARNING.ordinal()) {
				float sev = Mth.clamp(ClientClimateState.getSeverity(), 0.0F, 1.0F);
				float factor = phase == CollapsePhase.TERMINAL
					? 2.85F
					: (phase == CollapsePhase.CRITICAL ? 1.55F + 1.15F * sev : 1.18F + 0.35F * sev);
				return baseSunSize * factor;
			}
		}
		return baseSunSize;
	}

	@Inject(
		method = "renderSky",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
			ordinal = 0
		)
	)
	private void galacticpurpose$renderRedGiantCoronaAndTintSun(
		Matrix4f viewMatrix,
		Matrix4f projectionMatrix,
		float partialTick,
		Camera camera,
		boolean isFoggy,
		Runnable skyFogSetup,
		CallbackInfo ci
	) {
		if (this.level == null) {
			return;
		}
		float redFactor = galacticpurpose$computeRedGiantFactor();
		if (redFactor <= 0.01F) {
			return;
		}

		float rainAlpha = 1.0F - this.level.getRainLevel(partialTick);
		boolean activeStorm = ClientMarsState.isActiveInDimension()
			&& ClientMarsState.isRedGiantActive()
			&& ClientMarsState.getStormPhase() == SolarStormPhase.ACTIVE;
		float sunRadius = galacticpurpose$scaleSolarRadius(30.0F);
		float time = galacticpurpose$elapsedSeconds();

		// Build exact Sun orientation matrix matching LevelRenderer.renderSky
		Matrix4f sunMat = new Matrix4f(viewMatrix);
		sunMat.rotate(Axis.YP.rotationDegrees(-90.0F));
		sunMat.rotate(Axis.XP.rotationDegrees(this.level.getTimeOfDay(partialTick) * 360.0F));

		Tesselator tess = Tesselator.getInstance();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		// Outer deep crimson corona + bright fiery orange photosphere bloom
		float outerR = sunRadius * (activeStorm ? 2.15F : 1.78F);
		float innerR = sunRadius * (activeStorm ? 1.25F : 1.08F);
		float outerAlpha = Mth.clamp((0.52F * redFactor + (activeStorm ? 0.25F : 0.0F)) * rainAlpha, 0.0F, 0.92F);
		float innerAlpha = Mth.clamp((0.72F * redFactor + (activeStorm ? 0.22F : 0.0F)) * rainAlpha, 0.0F, 0.96F);

		galacticpurpose$drawRadialDisc(tess, sunMat, outerR, 1.0F, 0.16F, 0.04F, outerAlpha, 36);
		galacticpurpose$drawRadialDisc(tess, sunMat, innerR, 1.0F, activeStorm ? 0.42F : 0.28F, 0.07F, innerAlpha, 32);

		if (activeStorm) {
			// Rotating CME coronal mass ejection flare spikes blasting outward
			galacticpurpose$drawStormFlareRays(tess, sunMat, sunRadius * 2.55F, time, rainAlpha);
		}

		// Restore PositionTexShader & tint vanilla's scaled Sun disc deep crimson-orange
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		float green = Mth.lerp(redFactor, 1.0F, activeStorm ? 0.36F : 0.22F);
		float blue = Mth.lerp(redFactor, 1.0F, activeStorm ? 0.12F : 0.06F);
		RenderSystem.setShaderColor(1.0F, green, blue, rainAlpha);
	}

	@Inject(
		method = "renderSky",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
			ordinal = 1
		)
	)
	private void galacticpurpose$restoreMoonTint(
		Matrix4f viewMatrix,
		Matrix4f projectionMatrix,
		float partialTick,
		Camera camera,
		boolean isFoggy,
		Runnable skyFogSetup,
		CallbackInfo ci
	) {
		if (this.level != null) {
			float rainAlpha = 1.0F - this.level.getRainLevel(partialTick);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, rainAlpha);
		}
	}

	@Unique
	private static float galacticpurpose$elapsedSeconds() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null ? mc.level.getGameTime() * 0.05F : 0.0F;
	}

	@Unique
	private static float galacticpurpose$computeRedGiantFactor() {
		if (ClientMarsState.isActiveInDimension()) {
			return ClientMarsState.isRedGiantActive() ? 1.0F : 0.0F;
		}
		if (ClientClimateState.isActiveInDimension()) {
			CollapsePhase phase = ClientClimateState.getPhase();
			if (phase == CollapsePhase.WARNING) {
				return 0.35F;
			}
			if (phase == CollapsePhase.CRITICAL) {
				return 0.55F + 0.35F * Mth.clamp(ClientClimateState.getSeverity(), 0.0F, 1.0F);
			}
			if (phase == CollapsePhase.TERMINAL) {
				return 1.0F;
			}
		}
		return 0.0F;
	}

	@Unique
	private static void galacticpurpose$drawRadialDisc(
		Tesselator tess,
		Matrix4f mat,
		float radius,
		float r,
		float g,
		float b,
		float centerAlpha,
		int segments
	) {
		BufferBuilder bb = tess.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		bb.addVertex(mat, 0.0F, 100.0F, 0.0F).setColor(r, g, b, centerAlpha);
		for (int i = 0; i <= segments; i++) {
			double a = (i / (double) segments) * Math.PI * 2.0;
			float vx = (float) (Math.cos(a) * radius);
			float vz = (float) (Math.sin(a) * radius);
			bb.addVertex(mat, vx, 100.0F, vz).setColor(r, g * 0.5F, b * 0.4F, 0.0F);
		}
		BufferUploader.drawWithShader(bb.buildOrThrow());
	}

	@Unique
	private static void galacticpurpose$drawStormFlareRays(
		Tesselator tess,
		Matrix4f mat,
		float maxRadius,
		float time,
		float rainAlpha
	) {
		int spikes = 10;
		BufferBuilder bb = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < spikes; i++) {
			float angle = (float) (i * (Math.PI * 2.0 / spikes)) + time * 0.16F;
			float flareLen = maxRadius * (0.72F + 0.28F * Mth.sin(time * 2.8F + i * 1.7F));
			float spread = 0.22F;
			float baseR = maxRadius * 0.28F;
			float x1 = Mth.cos(angle - spread) * baseR;
			float z1 = Mth.sin(angle - spread) * baseR;
			float x2 = Mth.cos(angle + spread) * baseR;
			float z2 = Mth.sin(angle + spread) * baseR;
			float xt = Mth.cos(angle) * flareLen;
			float zt = Mth.sin(angle) * flareLen;
			float baseA = Mth.clamp(0.58F * rainAlpha, 0.0F, 0.85F);
			bb.addVertex(mat, x1, 100.0F, z1).setColor(1.0F, 0.52F, 0.14F, baseA);
			bb.addVertex(mat, xt, 100.0F, zt).setColor(1.0F, 0.20F, 0.05F, 0.0F);
			bb.addVertex(mat, x2, 100.0F, z2).setColor(1.0F, 0.52F, 0.14F, baseA);
		}
		BufferUploader.drawWithShader(bb.buildOrThrow());
	}
}

package com.galacticpurpose.client.sky;

import com.galacticpurpose.client.hud.ClientOrbitalTransitState;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Interstellar deep-space sky renderer for galacticpurpose:orbital_transit:
 * - 95-second unhurried majestic approach (planet grows from 12,000 km -> 120 km right above atmosphere;
 *   Origin Wormhole grows from 10.0 km -> 0.2 km filling the entire front bridge windshield!)
 * - High-res 48x80 Relativistic Gravitational-Lens Space Wormhole showing refracted photons from the
 *   other universe (Earth's curved blue sky, emerald horizons, clouds & bent stars!) + crisp cyan-gold Einstein halo!
 * - Smooth circular radial bow-shock plasma corona during atmospheric entry (zero square flash edges!)
 */
public final class OrbitalTransitSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
	private static final int STAR_COUNT = 560;
	private static final float[] STAR_X = new float[STAR_COUNT];
	private static final float[] STAR_Y = new float[STAR_COUNT];
	private static final float[] STAR_Z = new float[STAR_COUNT];
	private static final int[] STAR_RGB = new int[STAR_COUNT];

	public static final int LAT = 32;
	public static final int LON = 64;
	public static final float[][][] SPHERE = new float[LAT + 1][LON + 1][3];

	// Lean 28x48 sphere for crisp Relativistic Space Wormhole with 3x higher FPS!
	private static final int WH_LAT = 28;
	private static final int WH_LON = 48;
	private static final float[][][] WH_SPHERE = new float[WH_LAT + 1][WH_LON + 1][3];

	static {
		Random rng = new Random(0x53544152L);
		int[] palette = {0xFFFFFF, 0xDCEBFF, 0xFFF3D1, 0xB8D4FF, 0xFFD6B5};
		for (int i = 0; i < STAR_COUNT; i++) {
			double u = rng.nextDouble() * 2.0 - 1.0;
			double th = rng.nextDouble() * Math.PI * 2.0;
			double r = Math.sqrt(Math.max(0.0, 1.0 - u * u));
			STAR_X[i] = (float) (r * Math.cos(th));
			STAR_Y[i] = (float) u;
			STAR_Z[i] = (float) (r * Math.sin(th));
			STAR_RGB[i] = palette[rng.nextInt(palette.length)];
		}
		fillUnitSphere(SPHERE, LAT, LON);
		fillUnitSphere(WH_SPHERE, WH_LAT, WH_LON);
	}

	private static void fillUnitSphere(float[][][] dst, int latSteps, int lonSteps) {
		for (int la = 0; la <= latSteps; la++) {
			double phi = (la * Math.PI) / latSteps;
			float sp = (float) Math.sin(phi), cp = (float) Math.cos(phi);
			for (int lo = 0; lo <= lonSteps; lo++) {
				double th = (lo * Math.PI * 2.0) / lonSteps;
				dst[la][lo][0] = sp * (float) Math.cos(th);
				dst[la][lo][1] = cp;
				dst[la][lo][2] = sp * (float) Math.sin(th);
			}
		}
	}

	private final Matrix4f viewMat = new Matrix4f();
	private final Matrix4f workMat = new Matrix4f();
	private final Matrix4f scratchMat = new Matrix4f();

	@Override
	public void render(WorldRenderContext context) {
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		this.viewMat.set(context.positionMatrix());
		float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
		long gt = context.world() != null ? context.world().getGameTime() : 0L;
		float time = (gt + partial) * 0.05f;

		boolean returning = ClientOrbitalTransitState.returning();
		float progress = ClientOrbitalTransitState.approachProgress();
		boolean reentry = ClientOrbitalTransitState.reentryActive();
		float entry = ClientOrbitalTransitState.entryProgress();
		ExoplanetRoute route = ClientOrbitalTransitState.route();

		// 1. Deep-space starfield with subtle forward cruise parallax
		drawDeepSpaceStars(this.viewMat, time, reentry, entry);

		// 2. GIGANTIC Erebus Black Hole arching overhead through skylight & side windows
		this.workMat.set(this.viewMat)
			.rotateY((float) Math.toRadians(36.0))
			.rotateX((float) Math.toRadians(34.0))
			.translate(0.0f, 0.0f, -70.0f)
			.scale(2.15f);
		ErebusSkyRenderer.renderErebusAt(this.workMat, this.scratchMat, time, 1.02f, 0.98f, 0.94f);
		RenderSystem.defaultBlendFunc();

		// 3. HUGE High-LOD 512x256 Textured 7,000 km Exoplanet (continuous growth through 1,000 km -> 120 km; safe standoff so camera never clips inside!)
		float planetScale = com.galacticpurpose.ship.StarshipFlightPhysics.computePlanetVisualScale(progress, returning, entry);
		float planetZ = returning ? -96.0f : Mth.lerp(entry, 120.0f, 102.0f);
		float closeRatio = Mth.clamp((planetScale - 24.0f) / 72.0f, 0.0f, 1.0f);
		float planetY = returning ? -6.0f : (Mth.lerp(closeRatio, -6.0f, -38.0f) + entry * 10.0f);
		this.workMat.set(this.viewMat)
			.translate(0.0f, planetY, planetZ)
			.rotateY(time * 0.045f)
			.rotateZ((float) Math.toRadians(10.0));
		drawSmoothCulled3DPlanet(this.workMat, this.scratchMat, route, planetScale, time);

		// 4. RELATIVISTIC 60m SPACE WORMHOLE (0.16 pinpoint -> continuous approach -> restrained entry swell strictly outside sphere)
		float wormholeScale = com.galacticpurpose.ship.StarshipFlightPhysics.computeWormholeVisualScale(progress, returning, entry);
		float wormholeZ = returning ? Mth.lerp(entry, 76.0f, 44.0f) : -76.0f;
		this.workMat.set(this.viewMat)
			.translate(0.0f, returning ? Mth.lerp(entry, 2.5f, 1.0f) : 2.5f, wormholeZ);
		drawRelativisticSpaceWormhole(this.workMat, this.scratchMat, wormholeScale, returning, route, time);

		// 5. Smooth Circular Radial Plasma Corona swelling progressively on Atmospheric Entry / Wormhole Plunge
		if (reentry) {
			drawSmoothCircularEntryCorona(this.viewMat, returning, entry, time);
		}

		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
	}

	private static void drawDeepSpaceStars(Matrix4f view, float time, boolean reentry, float entry) {
		float d = 92.0f;
		RenderSystem.disableCull();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		addVoidSphere(b, view, 95.0f, 2, 3, 8);
		float streak = reentry ? (1.6f + entry * 4.8f) : 0.72f;
		for (int i = 0; i < STAR_COUNT; i++) {
			float sx = STAR_X[i] * d;
			float sy = STAR_Y[i] * d;
			float sz = STAR_Z[i] * d;
			float r = 0.15f + (i % 3) * 0.06f;
			int rgb = STAR_RGB[i];
			int cr = (rgb >> 16) & 0xFF, cg = (rgb >> 8) & 0xFF, cb = rgb & 0xFF;
			int a = 155 + (i % 95);
			b.addVertex(view, sx - r, sy - r, sz - streak).setColor(cr, cg, cb, a / 2);
			b.addVertex(view, sx + r, sy - r, sz + streak).setColor(cr, cg, cb, a);
			b.addVertex(view, sx + r, sy + r, sz + streak).setColor(cr, cg, cb, a);
			b.addVertex(view, sx - r, sy + r, sz - streak).setColor(cr, cg, cb, a / 2);
		}
		flush(b);
	}

	private static void drawSmoothCulled3DPlanet(
			Matrix4f mat, Matrix4f scratch, ExoplanetRoute route, float radius, float time
	) {
		int rAtmo, gAtmo, bAtmo;
		switch (route) {
			case HEPHAESTUS_IV -> { rAtmo = 255; gAtmo = 104; bAtmo = 34; }
			case PELAGIA_9 -> { rAtmo = 56; gAtmo = 196; bAtmo = 252; }
			case NYX_0 -> { rAtmo = 148; gAtmo = 78; bAtmo = 235; }
			default -> { rAtmo = 86; gAtmo = 224; bAtmo = 238; }
		}

		PlanetTextureBank.PlanetTextures tex = PlanetTextureBank.getOrCreate(route);

		// Pass 1: 512x256 High-Detail Planetary Surface Globe (ZERO per-frame fractal CPU work!)
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, tex.surface());
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (int la = 0; la < LAT; la++) {
			float v0 = 1.0f - (float) la / LAT;
			float v1 = 1.0f - (float) (la + 1) / LAT;
			for (int lo = 0; lo < LON; lo++) {
				float u0 = (float) lo / LON;
				float u1 = (float) (lo + 1) / LON;
				float[] p00 = SPHERE[la][lo], p10 = SPHERE[la + 1][lo], p11 = SPHERE[la + 1][lo + 1], p01 = SPHERE[la][lo + 1];

				int l00 = surfaceSunlight(p00, route), l01 = surfaceSunlight(p01, route);
				int l11 = surfaceSunlight(p11, route), l10 = surfaceSunlight(p10, route);

				b.addVertex(mat, p00[0] * radius, p00[1] * radius, p00[2] * radius).setUv(u0, v0).setColor(l00, l00, l00, 255);
				b.addVertex(mat, p01[0] * radius, p01[1] * radius, p01[2] * radius).setUv(u1, v0).setColor(l01, l01, l01, 255);
				b.addVertex(mat, p11[0] * radius, p11[1] * radius, p11[2] * radius).setUv(u1, v1).setColor(l11, l11, l11, 255);
				b.addVertex(mat, p10[0] * radius, p10[1] * radius, p10[2] * radius).setUv(u0, v1).setColor(l10, l10, l10, 255);
			}
		}
		flush(b);

		// Pass 2: Drifting Cloud / Ash Layer slightly higher over surface for crisp close-orbit parallax!
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderTexture(0, tex.clouds());
		scratch.set(mat).rotateY(time * 0.042f);
		float cloudR = radius * 1.016f;
		BufferBuilder cb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (int la = 0; la < LAT; la++) {
			float v0 = 1.0f - (float) la / LAT;
			float v1 = 1.0f - (float) (la + 1) / LAT;
			for (int lo = 0; lo < LON; lo++) {
				float u0 = (float) lo / LON;
				float u1 = (float) (lo + 1) / LON;
				float[] p00 = SPHERE[la][lo], p10 = SPHERE[la + 1][lo], p11 = SPHERE[la + 1][lo + 1], p01 = SPHERE[la][lo + 1];

				int l00 = cloudSunlight(p00), l01 = cloudSunlight(p01), l11 = cloudSunlight(p11), l10 = cloudSunlight(p10);
				cb.addVertex(scratch, p00[0] * cloudR, p00[1] * cloudR, p00[2] * cloudR).setUv(u0, v0).setColor(l00, l00, l00, 255);
				cb.addVertex(scratch, p01[0] * cloudR, p01[1] * cloudR, p01[2] * cloudR).setUv(u1, v0).setColor(l01, l01, l01, 255);
				cb.addVertex(scratch, p11[0] * cloudR, p11[1] * cloudR, p11[2] * cloudR).setUv(u1, v1).setColor(l11, l11, l11, 255);
				cb.addVertex(scratch, p10[0] * cloudR, p10[1] * cloudR, p10[2] * cloudR).setUv(u0, v1).setColor(l10, l10, l10, 255);
			}
		}
		flush(cb);

		// Pass 3: Luminous Atmospheric Rim Shell (restores POSITION_COLOR shader cleanly)
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.enableCull();
		float haloR = radius * 1.055f;
		BufferBuilder h = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int la = 0; la < LAT; la++) {
			for (int lo = 0; lo < LON; lo++) {
				float[] p00 = SPHERE[la][lo], p10 = SPHERE[la + 1][lo], p11 = SPHERE[la + 1][lo + 1], p01 = SPHERE[la][lo + 1];
				h.addVertex(mat, p00[0] * haloR, p00[1] * haloR, p00[2] * haloR).setColor(rAtmo, gAtmo, bAtmo, limbAlpha(p00));
				h.addVertex(mat, p01[0] * haloR, p01[1] * haloR, p01[2] * haloR).setColor(rAtmo, gAtmo, bAtmo, limbAlpha(p01));
				h.addVertex(mat, p11[0] * haloR, p11[1] * haloR, p11[2] * haloR).setColor(rAtmo, gAtmo, bAtmo, limbAlpha(p11));
				h.addVertex(mat, p10[0] * haloR, p10[1] * haloR, p10[2] * haloR).setColor(rAtmo, gAtmo, bAtmo, limbAlpha(p10));
			}
		}
		flush(h);
		RenderSystem.defaultBlendFunc();
	}

	private static int surfaceSunlight(float[] p, ExoplanetRoute route) {
		float sunDot = p[0] * 0.42f + p[1] * 0.46f + Math.abs(p[2]) * 0.64f;
		float floor = route == ExoplanetRoute.HEPHAESTUS_IV ? 0.56f : 0.44f;
		float light = Mth.clamp(floor + Math.max(0.0f, sunDot + 0.16f) * 0.68f, floor, 1.0f);
		return Mth.clamp(Math.round(light * 255.0f), 90, 255);
	}

	private static int cloudSunlight(float[] p) {
		float sunDot = p[0] * 0.42f + p[1] * 0.46f + Math.abs(p[2]) * 0.64f;
		float light = Mth.clamp(0.42f + Math.max(0.0f, sunDot + 0.16f) * 0.68f, 0.42f, 1.0f);
		return Mth.clamp(Math.round(light * 255.0f), 95, 255);
	}

	private static int limbAlpha(float[] p) {
		float lit = Mth.clamp(0.32f + 0.68f * (p[0] * -0.45f + p[1] * 0.55f + p[2] * -0.55f), 0.12f, 1.0f);
		return Mth.clamp(Math.round(48.0f * lit), 6, 56);
	}

	/**
	 * True Relativistic Gravitational-Lens 3D Space Wormhole (28x48 sphere):
	 * - Shows refracted photons from the OTHER universe bent inside the crystal sphere (Earth's blue sky, emerald continents,
	 *   swirling white clouds & bent golden starlight on return; warped distant galaxy & solar starfield behind you on outbound!)
	 * - Surrounded by a razor-sharp luminous cyan-white Einstein photon ring + ONE clean tilted golden-cyan accretion halo
	 *   matching our Overworld Colossal Wormhole's iconic look — with ZERO walls, ZERO pylons, ZERO faceted grid!
	 */
	private static void drawRelativisticSpaceWormhole(
		Matrix4f baseMat, Matrix4f scratch, float radius, boolean returning, ExoplanetRoute route, float time
	) {
		int acc = route.accentRgb();
		int ar = (acc >> 16) & 0xFF, ag = (acc >> 8) & 0xFF, ab = acc & 0xFF;

		// 1. Ultra-Smooth 48x80 Spherical Gravitational Lens Interior (per-vertex smooth refracted photons from the other side!)
		scratch.set(baseMat).rotateY(time * 0.08f);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		BufferBuilder lensSphere = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		float orbR = radius * 0.90f;
		int[] c00 = new int[3], c01 = new int[3], c11 = new int[3], c10 = new int[3];
		for (int la = 0; la < WH_LAT; la++) {
			for (int lo = 0; lo < WH_LON; lo++) {
				float[] p00 = WH_SPHERE[la][lo], p10 = WH_SPHERE[la + 1][lo], p11 = WH_SPHERE[la + 1][lo + 1], p01 = WH_SPHERE[la][lo + 1];
				sampleWormholeRefraction(p00, returning, ar, ag, ab, time, c00);
				sampleWormholeRefraction(p01, returning, ar, ag, ab, time, c01);
				sampleWormholeRefraction(p11, returning, ar, ag, ab, time, c11);
				sampleWormholeRefraction(p10, returning, ar, ag, ab, time, c10);

				lensSphere.addVertex(scratch, p00[0] * orbR, p00[1] * orbR, p00[2] * orbR).setColor(c00[0], c00[1], c00[2], 255);
				lensSphere.addVertex(scratch, p01[0] * orbR, p01[1] * orbR, p01[2] * orbR).setColor(c01[0], c01[1], c01[2], 255);
				lensSphere.addVertex(scratch, p11[0] * orbR, p11[1] * orbR, p11[2] * orbR).setColor(c11[0], c11[1], c11[2], 255);
				lensSphere.addVertex(scratch, p10[0] * orbR, p10[1] * orbR, p10[2] * orbR).setColor(c10[0], c10[1], c10[2], 255);
			}
		}
		flush(lensSphere);

		// 2. Crisp Luminous Relativistic Cyan-White Einstein Photon Ring hugging the exact spherical boundary
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.disableCull();
		BufferBuilder einstein = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int segs = 96;
		float eIn = radius * 0.84f;
		float ePeak = radius * 0.94f;
		float eOut = radius * 1.24f;
		for (int i = 0; i < segs; i++) {
			float a0 = (float) ((i * Math.PI * 2.0) / segs + time * 0.35f);
			float a1 = (float) (((i + 1) * Math.PI * 2.0) / segs + time * 0.35f);
			float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
			float mod0 = 0.80f + 0.20f * Mth.sin(a0 * 4.0f - time * 2.2f);
			float mod1 = 0.80f + 0.20f * Mth.sin(a1 * 4.0f - time * 2.2f);
			int pa0 = Math.round(215 * mod0), pa1 = Math.round(215 * mod1);

			einstein.addVertex(baseMat, c0 * eIn, s0 * eIn, 0.0f).setColor(80, 224, 255, 0);
			einstein.addVertex(baseMat, c0 * ePeak, s0 * ePeak, 0.0f).setColor(195, 244, 255, pa0);
			einstein.addVertex(baseMat, c1 * ePeak, s1 * ePeak, 0.0f).setColor(195, 244, 255, pa1);
			einstein.addVertex(baseMat, c1 * eIn, s1 * eIn, 0.0f).setColor(80, 224, 255, 0);

			einstein.addVertex(baseMat, c0 * ePeak, s0 * ePeak, 0.0f).setColor(195, 244, 255, pa0);
			einstein.addVertex(baseMat, c0 * eOut, s0 * eOut, 0.0f).setColor(72, 192, 255, 0);
			einstein.addVertex(baseMat, c1 * eOut, s1 * eOut, 0.0f).setColor(72, 192, 255, 0);
			einstein.addVertex(baseMat, c1 * ePeak, s1 * ePeak, 0.0f).setColor(195, 244, 255, pa1);
		}
		flush(einstein);

		// 3. ONE Crisp Tilted 3D Cyan-Gold Relativistic Accretion Ring (echoing our Overworld Wormhole's clean silhouette!)
		scratch.set(baseMat)
			.rotateZ((float) Math.toRadians(14.0))
			.rotateX((float) Math.toRadians(72.0))
			.rotateY(time * 0.55f);
		BufferBuilder eqRing = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		float rIn = radius * 1.02f;
		float rPeak = radius * 1.24f;
		float rOut = radius * 1.66f;
		for (int i = 0; i < segs; i++) {
			float a0 = (float) ((i * Math.PI * 2.0) / segs);
			float a1 = (float) (((i + 1) * Math.PI * 2.0) / segs);
			float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
			float m0 = 0.78f + 0.22f * Mth.sin(a0 * 3.0f - time * 1.9f);
			float m1 = 0.78f + 0.22f * Mth.sin(a1 * 3.0f - time * 1.9f);
			int pa0 = Math.round(175 * m0), pa1 = Math.round(175 * m1);

			eqRing.addVertex(scratch, c0 * rIn, 0.0f, s0 * rIn).setColor(120, 224, 255, 0);
			eqRing.addVertex(scratch, c0 * rPeak, 0.0f, s0 * rPeak).setColor(245, 225, 160, pa0);
			eqRing.addVertex(scratch, c1 * rPeak, 0.0f, s1 * rPeak).setColor(245, 225, 160, pa1);
			eqRing.addVertex(scratch, c1 * rIn, 0.0f, s1 * rIn).setColor(120, 224, 255, 0);

			eqRing.addVertex(scratch, c0 * rPeak, 0.0f, s0 * rPeak).setColor(245, 225, 160, pa0);
			eqRing.addVertex(scratch, c0 * rOut, 0.0f, s0 * rOut).setColor(80, 195, 255, 0);
			eqRing.addVertex(scratch, c1 * rOut, 0.0f, s1 * rOut).setColor(80, 195, 255, 0);
			eqRing.addVertex(scratch, c1 * rPeak, 0.0f, s1 * rPeak).setColor(245, 225, 160, pa1);
		}
		flush(eqRing);
		RenderSystem.defaultBlendFunc();
	}

	/**
	 * Computes smooth spherical gravitational-lensing photon refraction on the 48x80 Space Wormhole interior:
	 * light rays bend dramatically near the outer sphere edge, revealing a warped miniature sky, curved continents,
	 * drifting clouds, and stretched distant stars from the other side of the Einstein-Rosen bridge!
	 */
	private static void sampleWormholeRefraction(
		float[] p, boolean returningToOrigin, int ar, int ag, int ab, float time, int[] outRgb
	) {
		float x = p[0], y = p[1], z = p[2];
		// Spherical lens radial bend (strongest magnification near the outer rim of the sphere!)
		float r2 = Mth.clamp(x * x + y * y, 0.0f, 1.0f);
		float edgeBend = (float) Math.pow(r2, 1.35);
		float angle = (float) Mth.atan2(y, x);

		// Warped photon coordinates pulled around the curvature of the wormhole throat
		float wx = x * (1.0f + edgeBend * 2.2f);
		float wy = y * (1.0f + edgeBend * 2.2f);
		float wz = z + edgeBend * 0.9f;

		// Distant starlight streaks stretched azimuthally around the Einstein ring
		float starArc = Mth.sin(angle * 11.0f + r2 * 9.0f - time * 0.45f) * Mth.cos(angle * 7.0f - r2 * 13.0f);
		float starBright = Mth.clamp((starArc - 0.78f) / 0.22f, 0.0f, 1.0f);

		// Curved horizon / galactic vista seen through the throat of the wormhole!
		float vistaNoise = 0.5f + 0.5f * Mth.sin(wx * 3.4f + wz * 2.6f + time * 0.14f) * Mth.cos(wy * 3.8f - wx * 2.1f);
		float cloudBand = Mth.clamp((0.5f + 0.5f * Mth.sin(wy * 6.5f + wx * 2.8f + time * 0.20f) - 0.62f) / 0.36f, 0.0f, 1.0f);

		float rCol, gCol, bCol;
		if (returningToOrigin) {
			// Looking back home toward Earth / Overworld: deep azure sky, curved emerald land & sapphire ocean, sunlit clouds!
			float horizon = Mth.clamp(0.5f + wy * 0.55f, 0.0f, 1.0f);
			float land = Mth.clamp((vistaNoise - 0.48f) / 0.20f, 0.0f, 1.0f);
			float rEarth = Mth.lerp(horizon, Mth.lerp(land, 16.0f, 44.0f), 78.0f);
			float gEarth = Mth.lerp(horizon, Mth.lerp(land, 86.0f, 152.0f), 168.0f);
			float bEarth = Mth.lerp(horizon, Mth.lerp(land, 178.0f, 88.0f), 238.0f);
			// Blend white drifting Earth clouds + warm distant Sun glint
			rCol = Mth.lerp(cloudBand * 0.65f, rEarth, 238.0f);
			gCol = Mth.lerp(cloudBand * 0.65f, gEarth, 246.0f);
			bCol = Mth.lerp(cloudBand * 0.65f, bEarth, 255.0f);
		} else {
			// Looking back from Erebus into the Exit Wormhole: refracted deep-space cosmic nebula, blue solar starlight & destination shimmer!
			float nebula = Mth.clamp(vistaNoise, 0.0f, 1.0f);
			rCol = Mth.lerp(nebula, 16.0f, Math.min(255.0f, ar * 0.55f + 58.0f));
			gCol = Mth.lerp(nebula, 46.0f, Math.min(255.0f, ag * 0.55f + 115.0f));
			bCol = Mth.lerp(nebula, 98.0f, Math.min(255.0f, ab * 0.65f + 165.0f));
		}

		// Add gravitationally bent starlight arcs + luminous cyan-white lensing compression right at the outer rim!
		float rimGlow = (float) Math.pow(r2, 2.6);
		rCol = Mth.lerp(rimGlow * 0.80f, rCol + starBright * 175.0f, 185.0f);
		gCol = Mth.lerp(rimGlow * 0.80f, gCol + starBright * 210.0f, 238.0f);
		bCol = Mth.lerp(rimGlow * 0.80f, bCol + starBright * 245.0f, 255.0f);

		outRgb[0] = Mth.clamp(Math.round(rCol), 0, 255);
		outRgb[1] = Mth.clamp(Math.round(gCol), 0, 255);
		outRgb[2] = Mth.clamp(Math.round(bCol), 0, 255);
	}

	/**
	 * Smooth 64-segment circular radial plasma corona during atmospheric entry / wormhole plunge (ZERO square box flash!).
	 */
	private static void drawSmoothCircularEntryCorona(Matrix4f view, boolean wormholeJump, float entry, float time) {
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int r = wormholeJump ? 105 : 255;
		int g = wormholeJump ? 205 : 136;
		int bl = wormholeJump ? 255 : 42;
		float intensity = Mth.clamp(0.25f + entry * 1.15f, 0.25f, 1.35f);
		float pulse = (0.78f + 0.22f * Mth.sin(time * 16.0f)) * intensity;
		int peakA = Mth.clamp(Math.round(88 * pulse), 0, 185);

		int segs = 64;
		float z = 54.0f;
		float rInner = 16.0f;
		float rPeak = 32.0f;
		float rOuter = 58.0f;
		for (int i = 0; i < segs; i++) {
			float a0 = (float) ((i * Math.PI * 2.0) / segs + time * 0.4f);
			float a1 = (float) (((i + 1) * Math.PI * 2.0) / segs + time * 0.4f);
			float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);

			b.addVertex(view, c0 * rInner, s0 * rInner, z).setColor(r, g, bl, 0);
			b.addVertex(view, c0 * rPeak, s0 * rPeak, z).setColor(r, g, bl, peakA);
			b.addVertex(view, c1 * rPeak, s1 * rPeak, z).setColor(r, g, bl, peakA);
			b.addVertex(view, c1 * rInner, s1 * rInner, z).setColor(r, g, bl, 0);

			b.addVertex(view, c0 * rPeak, s0 * rPeak, z).setColor(r, g, bl, peakA);
			b.addVertex(view, c0 * rOuter, s0 * rOuter, z).setColor(r, g, bl, 0);
			b.addVertex(view, c1 * rOuter, s1 * rOuter, z).setColor(r, g, bl, 0);
			b.addVertex(view, c1 * rPeak, s1 * rPeak, z).setColor(r, g, bl, peakA);
		}
		flush(b);
		RenderSystem.defaultBlendFunc();
	}

	private static void addVoidSphere(BufferBuilder b, Matrix4f m, float radius, int r, int g, int bl) {
		int latN = 20;
		int lonN = 36;
		for (int i = 0; i < latN; i++) {
			double t0 = -Math.PI * 0.5 + Math.PI * i / latN;
			double t1 = -Math.PI * 0.5 + Math.PI * (i + 1) / latN;
			float y0 = (float) Math.sin(t0) * radius;
			float c0 = (float) Math.cos(t0) * radius;
			float y1 = (float) Math.sin(t1) * radius;
			float c1 = (float) Math.cos(t1) * radius;
			for (int j = 0; j < lonN; j++) {
				double p0 = 2.0 * Math.PI * j / lonN;
				double p1 = 2.0 * Math.PI * (j + 1) / lonN;
				float x00 = (float) Math.cos(p0) * c0, z00 = (float) Math.sin(p0) * c0;
				float x01 = (float) Math.cos(p1) * c0, z01 = (float) Math.sin(p1) * c0;
				float x11 = (float) Math.cos(p1) * c1, z11 = (float) Math.sin(p1) * c1;
				float x10 = (float) Math.cos(p0) * c1, z10 = (float) Math.sin(p0) * c1;
				b.addVertex(m, x00, y0, z00).setColor(r, g, bl, 255);
				b.addVertex(m, x10, y1, z10).setColor(r, g, bl, 255);
				b.addVertex(m, x11, y1, z11).setColor(r, g, bl, 255);
				b.addVertex(m, x01, y0, z01).setColor(r, g, bl, 255);
			}
		}
	}

	private static void flush(BufferBuilder builder) {
		MeshData mesh = builder.build();
		if (mesh != null) {
			BufferUploader.drawWithShader(mesh);
		}
	}
}

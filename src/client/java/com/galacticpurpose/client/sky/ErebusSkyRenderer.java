package com.galacticpurpose.client.sky;

import com.galacticpurpose.exoplanet.ExoplanetGravityCalculator;
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
 * Interstellar-grade zero-allocation GPU sky renderer for the Erebus Singularity black hole:
 * pure pitch-black event horizon, additive relativistic upper/lower gravitational lensing arches,
 * smooth soft-edge golden-white accretion disk with Keplerian swirl and Doppler beaming (zero hard cut lines),
 * unified across all exoplanets with planetary atmospheric filtering and orbital rotation.
 */
public final class ErebusSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
	public record SkyAtmosphere(
		int dayZenithRgb,
		int dayHorizonRgb,
		int nightZenithRgb,
		int nightHorizonRgb,
		float atmoFilterR,
		float atmoFilterG,
		float atmoFilterB,
		float singularityScale,
		float orbitalTiltDeg
	) {
	}

	private static final int SEGMENTS = 48;
	private static final float[] COS = new float[SEGMENTS + 1];
	private static final float[] SIN = new float[SEGMENTS + 1];
	private static final float[] ANGLE = new float[SEGMENTS + 1];
	private static final int STAR_COUNT = 280;
	private static final float[] STAR_DIR = new float[STAR_COUNT * 3];
	private static final float[] STAR_SIZE = new float[STAR_COUNT];
	private static final int[] STAR_RGB = new int[STAR_COUNT];

	static {
		for (int i = 0; i <= SEGMENTS; i++) {
			double a = (i * Math.PI * 2.0) / SEGMENTS;
			ANGLE[i] = (float) a;
			COS[i] = (float) Math.cos(a);
			SIN[i] = (float) Math.sin(a);
		}
		Random rand = new Random(0x45524542L);
		int[] starPalette = {0xFFFFFF, 0xDCEBFF, 0xFFF4D6, 0xC8DCFF, 0xFFE3C2};
		for (int i = 0; i < STAR_COUNT; i++) {
			double u = rand.nextDouble() * 2.0 - 1.0;
			double theta = rand.nextDouble() * Math.PI * 2.0;
			double r = Math.sqrt(Math.max(0.0, 1.0 - u * u));
			STAR_DIR[i * 3] = (float) (r * Math.cos(theta));
			STAR_DIR[i * 3 + 1] = (float) u;
			STAR_DIR[i * 3 + 2] = (float) (r * Math.sin(theta));
			STAR_SIZE[i] = 0.11f + rand.nextFloat() * 0.22f;
			STAR_RGB[i] = starPalette[rand.nextInt(starPalette.length)];
		}
	}

	private final SkyAtmosphere atmosphere;
	private final Matrix4f viewMatrix = new Matrix4f();
	private final Matrix4f starMatrix = new Matrix4f();
	private final Matrix4f bhMatrix = new Matrix4f();
	private final Matrix4f diskMatrix = new Matrix4f();

	public ErebusSkyRenderer(SkyAtmosphere atmosphere) {
		this.atmosphere = atmosphere;
	}

	@Override
	public void render(WorldRenderContext context) {
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		this.viewMatrix.set(context.positionMatrix());

		float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
		long dayTime = context.world() != null ? context.world().getDayTime() : 6000L;
		long gameTime = context.world() != null ? context.world().getGameTime() : 0L;
		float timeSeconds = (gameTime + partial) * 0.05f;

		double orbitFraction = (Math.floorMod(dayTime, ExoplanetGravityCalculator.TICKS_PER_ORBITAL_DAY) + partial)
			/ (double) ExoplanetGravityCalculator.TICKS_PER_ORBITAL_DAY;
		float orbitAngle = (float) (orbitFraction * Math.PI * 2.0);
		float elevation = Mth.sin(orbitAngle);
		float daylightFactor = Mth.clamp((elevation + 0.25f) / 1.25f, 0.0f, 1.0f);

		drawSkyDome(this.viewMatrix, daylightFactor);
		drawStars(this.viewMatrix, orbitAngle, daylightFactor);
		drawErebusSingularity(this.viewMatrix, orbitAngle, timeSeconds);

		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
	}

	private void drawSkyDome(Matrix4f mat, float daylight) {
		int zr = lerpChannel(this.atmosphere.nightZenithRgb(), this.atmosphere.dayZenithRgb(), 16, daylight);
		int zg = lerpChannel(this.atmosphere.nightZenithRgb(), this.atmosphere.dayZenithRgb(), 8, daylight);
		int zb = lerpChannel(this.atmosphere.nightZenithRgb(), this.atmosphere.dayZenithRgb(), 0, daylight);
		int hr = lerpChannel(this.atmosphere.nightHorizonRgb(), this.atmosphere.dayHorizonRgb(), 16, daylight);
		int hg = lerpChannel(this.atmosphere.nightHorizonRgb(), this.atmosphere.dayHorizonRgb(), 8, daylight);
		int hb = lerpChannel(this.atmosphere.nightHorizonRgb(), this.atmosphere.dayHorizonRgb(), 0, daylight);

		float d = 95.0f;
		BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		addQuad(builder, mat, -d, d, -d, zr, zg, zb, d, d, -d, zr, zg, zb, d, d, d, zr, zg, zb, -d, d, d, zr, zg, zb, 255);
		addQuad(builder, mat, -d, -d, d, hr, hg, hb, d, -d, d, hr, hg, hb, d, -d, -d, hr, hg, hb, -d, -d, -d, hr, hg, hb, 255);
		addQuad(builder, mat, -d, -d, -d, hr, hg, hb, d, -d, -d, hr, hg, hb, d, d, -d, zr, zg, zb, -d, d, -d, zr, zg, zb, 255);
		addQuad(builder, mat, d, -d, d, hr, hg, hb, -d, -d, d, hr, hg, hb, -d, d, d, zr, zg, zb, d, d, d, zr, zg, zb, 255);
		addQuad(builder, mat, -d, -d, d, hr, hg, hb, -d, -d, -d, hr, hg, hb, -d, d, -d, zr, zg, zb, -d, d, d, zr, zg, zb, 255);
		addQuad(builder, mat, d, -d, -d, hr, hg, hb, d, -d, d, hr, hg, hb, d, d, d, zr, zg, zb, d, d, -d, zr, zg, zb, 255);
		flush(builder);
	}

	private void drawStars(Matrix4f view, float orbitAngle, float daylight) {
		this.starMatrix.set(view)
			.rotateY((float) Math.toRadians(this.atmosphere.orbitalTiltDeg()))
			.rotateX(orbitAngle);

		float d = 88.0f;
		float visibility = 1.0f - daylight * 0.42f;
		BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < STAR_COUNT; i++) {
			float sx = STAR_DIR[i * 3];
			float sy = STAR_DIR[i * 3 + 1];
			float sz = STAR_DIR[i * 3 + 2];
			float cx = sx * d, cy = sy * d, cz = sz * d;
			float r = STAR_SIZE[i];
			int rgb = STAR_RGB[i];
			int cr = (rgb >> 16) & 0xFF, cg = (rgb >> 8) & 0xFF, cb = rgb & 0xFF;
			int a = Math.round((130 + (i % 110)) * visibility);
			builder.addVertex(this.starMatrix, cx - r, cy - r, cz).setColor(cr, cg, cb, a);
			builder.addVertex(this.starMatrix, cx + r, cy - r, cz).setColor(cr, cg, cb, a);
			builder.addVertex(this.starMatrix, cx + r, cy + r, cz).setColor(cr, cg, cb, a);
			builder.addVertex(this.starMatrix, cx - r, cy + r, cz).setColor(cr, cg, cb, a);
		}
		flush(builder);
	}

	private void drawErebusSingularity(Matrix4f view, float orbitAngle, float timeSeconds) {
		this.bhMatrix.set(view)
			.rotateY((float) Math.toRadians(this.atmosphere.orbitalTiltDeg()))
			.rotateX(orbitAngle)
			.translate(0.0f, 0.0f, -72.0f)
			.scale(this.atmosphere.singularityScale());

		renderErebusAt(
			this.bhMatrix,
			this.diskMatrix,
			timeSeconds,
			this.atmosphere.atmoFilterR(),
			this.atmosphere.atmoFilterG(),
			this.atmosphere.atmoFilterB()
		);
	}

	/**
	 * Reusable Gargantua-grade Erebus Singularity renderer (used on all 4 exoplanets AND in Orbital Transit deep space!).
	 */
	public static void renderErebusAt(
		Matrix4f bhMat,
		Matrix4f scratchDiskMat,
		float timeSeconds,
		float fr,
		float fg,
		float fb
	) {
		// Steep 83-degree edge-on Gargantua inclination -> graceful equatorial belt without masking half the shadow
		scratchDiskMat.set(bhMat)
			.rotateZ((float) Math.toRadians(8.0))
			.rotateX((float) Math.toRadians(83.0));

		// Switch to additive plasma light so overlapping relativistic glow layers brighten smoothly instead of darkening!
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

		// 1. Far half of accretion disk (behind event horizon)
		BufferBuilder diskBack = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		emitPlasmaDiskHalf(diskBack, scratchDiskMat, SEGMENTS / 2, SEGMENTS, timeSeconds, fr, fg, fb, 0.85f);
		flush(diskBack);

		// 2. Relativistic gravitational lensing upper & lower bent light arches
		BufferBuilder halo = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		emitLensingArches(halo, bhMat, timeSeconds, fr, fg, fb);
		flush(halo);

		// 3. Pure pitch-black Event Horizon shadow sphere (opaque alpha blend cleanly punches out background)
		RenderSystem.defaultBlendFunc();
		BufferBuilder core = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		emitDisc(core, bhMat, 7.65f, 0, 0, 0, 255);
		flush(core);

		// 4. Razor-thin blinding photon ring + smooth soft-edged near equatorial belt (additive glow -> zero cut edge!)
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		BufferBuilder photonRing = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		emitPhotonRing(photonRing, bhMat, timeSeconds, fr, fg, fb);
		flush(photonRing);

		BufferBuilder diskFront = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		emitPlasmaDiskHalf(diskFront, scratchDiskMat, 0, SEGMENTS / 2, timeSeconds, fr, fg, fb, 1.0f);
		flush(diskFront);
	}

	private static void emitPlasmaDiskHalf(
		BufferBuilder b,
		Matrix4f mat,
		int startSeg,
		int endSeg,
		float time,
		float fr,
		float fg,
		float fb,
		float intensityScale
	) {
		// 5 smooth abutting radial bands with SOFT inner fade-in (alpha=0 at inner edge -> zero razor slice line!)
		float[] radii = {7.70f, 8.75f, 11.4f, 16.0f, 23.0f, 32.0f};
		int[][] colors = {
			{255, 240, 200, 0},   // Soft inner feather so plasma glides seamlessly over the black shadow
			{255, 250, 228, 225}, // White-hot inner ISCO plasma stream
			{255, 198, 108, 175}, // Golden-orange main accretion stream
			{235, 122, 38,  105}, // Amber spiral arm
			{175, 58,  14,  42},  // Deep glowing outer ember
			{95,  22,  6,   0}    // Seamless fade to vacuum
		};

		for (int band = 0; band < 5; band++) {
			float r0 = radii[band];
			float r1 = radii[band + 1];
			int[] c0 = colors[band];
			int[] c1 = colors[band + 1];

			for (int i = startSeg; i < endSeg; i++) {
				int next = i + 1;
				float b0 = dopplerAndSwirl(ANGLE[i], time, band);
				float b1 = dopplerAndSwirl(ANGLE[next], time, band);
				float shift0 = -COS[i] * 0.14f;
				float shift1 = -COS[next] * 0.14f;

				emitDiskVertex(b, mat, COS[i] * r0, SIN[i] * r0, c0, b0 * intensityScale, shift0, fr, fg, fb);
				emitDiskVertex(b, mat, COS[i] * r1, SIN[i] * r1, c1, b0 * intensityScale, shift0, fr, fg, fb);
				emitDiskVertex(b, mat, COS[next] * r1, SIN[next] * r1, c1, b1 * intensityScale, shift1, fr, fg, fb);
				emitDiskVertex(b, mat, COS[next] * r0, SIN[next] * r0, c0, b1 * intensityScale, shift1, fr, fg, fb);
			}
		}
	}

	private static float dopplerAndSwirl(float angle, float time, int band) {
		// Relativistic Doppler beaming: approaching side (-cos > 0) glows brighter
		float doppler = 1.0f - (float) Math.cos(angle) * 0.28f;
		float speed = 1.7f - band * 0.25f;
		float swirl = 0.88f + 0.12f * Mth.sin(angle * 3.0f - time * speed) * Mth.cos(angle * 2.0f + time * (speed * 0.55f));
		return Mth.clamp(doppler * swirl, 0.52f, 1.32f);
	}

	private static void emitDiskVertex(
		BufferBuilder b, Matrix4f mat, float x, float y,
		int[] rgba, float bright, float dopplerShift, float fr, float fg, float fb
	) {
		int r = clamp255(rgba[0] * bright * fr * (1.0f - Math.max(0.0f, -dopplerShift) * 0.08f));
		int g = clamp255(rgba[1] * bright * fg * (1.0f + dopplerShift * 0.35f));
		int bl = clamp255(rgba[2] * bright * fb * (1.0f + Math.max(0.0f, dopplerShift) * 1.15f));
		int a = clamp255(rgba[3] * Math.min(1.08f, bright));
		b.addVertex(mat, x, y, 0.0f).setColor(r, g, bl, a);
	}

	/**
	 * Symmetrical Gargantua-style gravitational lensing arches bending the far side of the disk
	 * gracefully over both the top and bottom poles of the black hole shadow.
	 */
	private static void emitLensingArches(BufferBuilder b, Matrix4f mat, float time, float fr, float fg, float fb) {
		float pulse = 0.96f + 0.04f * Mth.sin(time * 0.65f);
		float[] radii = {7.60f, 8.70f, 10.6f, 13.8f};
		int[][] colors = {
			{255, 242, 206, 175},
			{255, 182, 86,  112},
			{215, 98,  26,  38},
			{120, 35,  8,   0}
		};
		for (int band = 0; band < 3; band++) {
			float r0 = radii[band], r1 = radii[band + 1];
			int[] c0 = colors[band], c1 = colors[band + 1];
			for (int i = 0; i < SEGMENTS; i++) {
				int next = i + 1;
				float pole0 = 0.45f + 0.55f * (SIN[i] * SIN[i]);
				float pole1 = 0.45f + 0.55f * (SIN[next] * SIN[next]);
				float arch0 = pole0 * (1.0f - COS[i] * 0.20f) * pulse;
				float arch1 = pole1 * (1.0f - COS[next] * 0.20f) * pulse;
				emitDiskVertex(b, mat, COS[i] * r0, SIN[i] * r0, c0, arch0, -COS[i] * 0.1f, fr, fg, fb);
				emitDiskVertex(b, mat, COS[i] * r1, SIN[i] * r1, c1, arch0, -COS[i] * 0.1f, fr, fg, fb);
				emitDiskVertex(b, mat, COS[next] * r1, SIN[next] * r1, c1, arch1, -COS[next] * 0.1f, fr, fg, fb);
				emitDiskVertex(b, mat, COS[next] * r0, SIN[next] * r0, c0, arch1, -COS[next] * 0.1f, fr, fg, fb);
			}
		}
	}

	private static void emitPhotonRing(BufferBuilder b, Matrix4f mat, float time, float fr, float fg, float fb) {
		float rIn = 7.62f;
		float rOut = 8.18f;
		float shimmer = 0.94f + 0.06f * Mth.sin(time * 2.1f);
		for (int i = 0; i < SEGMENTS; i++) {
			int next = i + 1;
			float d0 = (1.0f - COS[i] * 0.20f) * shimmer;
			float d1 = (1.0f - COS[next] * 0.20f) * shimmer;
			b.addVertex(mat, COS[i] * rIn, SIN[i] * rIn, 0.0f)
				.setColor(clamp255(255 * fr), clamp255(248 * fg), clamp255(228 * fb), clamp255(225 * d0));
			b.addVertex(mat, COS[i] * rOut, SIN[i] * rOut, 0.0f)
				.setColor(clamp255(255 * fr), clamp255(198 * fg), clamp255(112 * fb), 0);
			b.addVertex(mat, COS[next] * rOut, SIN[next] * rOut, 0.0f)
				.setColor(clamp255(255 * fr), clamp255(198 * fg), clamp255(112 * fb), 0);
			b.addVertex(mat, COS[next] * rIn, SIN[next] * rIn, 0.0f)
				.setColor(clamp255(255 * fr), clamp255(248 * fg), clamp255(228 * fb), clamp255(225 * d1));
		}
	}

	private static void emitDisc(BufferBuilder b, Matrix4f mat, float radius, int r, int g, int bl, int a) {
		for (int i = 0; i < SEGMENTS; i++) {
			b.addVertex(mat, 0.0f, 0.0f, 0.0f).setColor(r, g, bl, a);
			b.addVertex(mat, COS[i] * radius, SIN[i] * radius, 0.0f).setColor(r, g, bl, a);
			b.addVertex(mat, COS[i + 1] * radius, SIN[i + 1] * radius, 0.0f).setColor(r, g, bl, a);
			b.addVertex(mat, 0.0f, 0.0f, 0.0f).setColor(r, g, bl, a);
		}
	}

	private static void addQuad(
		BufferBuilder b, Matrix4f m,
		float x1, float y1, float z1, int r1, int g1, int b1,
		float x2, float y2, float z2, int r2, int g2, int b2,
		float x3, float y3, float z3, int r3, int g3, int b3,
		float x4, float y4, float z4, int r4, int g4, int b4,
		int alpha
	) {
		b.addVertex(m, x1, y1, z1).setColor(r1, g1, b1, alpha);
		b.addVertex(m, x2, y2, z2).setColor(r2, g2, b2, alpha);
		b.addVertex(m, x3, y3, z3).setColor(r3, g3, b3, alpha);
		b.addVertex(m, x4, y4, z4).setColor(r4, g4, b4, alpha);
	}

	private static int lerpChannel(int nightRgb, int dayRgb, int shift, float t) {
		int n = (nightRgb >> shift) & 0xFF;
		int d = (dayRgb >> shift) & 0xFF;
		return clamp255(n + (d - n) * t);
	}

	private static int clamp255(float v) {
		return Math.max(0, Math.min(255, Math.round(v)));
	}

	private static void flush(BufferBuilder builder) {
		MeshData mesh = builder.build();
		if (mesh != null) {
			BufferUploader.drawWithShader(mesh);
		}
	}
}

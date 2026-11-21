package com.galacticpurpose.client.sky;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Generates and caches high-detail 512x256 equirectangular planetary surface + semi-transparent
 * cloud textures once per ExoplanetRoute in GPU texture memory. Zero per-frame CPU fractal cost
 * and crystal-sharp planetary detail down to 120 km low orbit!
 */
public final class PlanetTextureBank {
	private static final int TEX_W = 512;
	private static final int TEX_H = 256;

	public record PlanetTextures(ResourceLocation surface, ResourceLocation clouds) {}

	private static final Map<ExoplanetRoute, PlanetTextures> CACHE = new EnumMap<>(ExoplanetRoute.class);

	private PlanetTextureBank() {
	}

	public static PlanetTextures getOrCreate(ExoplanetRoute route) {
		return CACHE.computeIfAbsent(route, PlanetTextureBank::buildAndRegister);
	}

	private static PlanetTextures buildAndRegister(ExoplanetRoute route) {
		NativeImage surfImg = new NativeImage(TEX_W, TEX_H, false);
		NativeImage cloudImg = new NativeImage(TEX_W, TEX_H, false);

		for (int py = 0; py < TEX_H; py++) {
			double v = (py + 0.5) / TEX_H;
			double lat = (v - 0.5) * Math.PI;
			float y = (float) Math.sin(lat);
			float cosLat = (float) Math.cos(lat);

			for (int px = 0; px < TEX_W; px++) {
				double u = (px + 0.5) / TEX_W;
				double lon = u * Math.PI * 2.0;
				float x = (float) (Math.cos(lon) * cosLat);
				float z = (float) (Math.sin(lon) * cosLat);

				float h = sampleHeightFbm(x, y, z, route);
				float hx = sampleHeightFbm(x + 0.014f, y, z, route);
				float hy = sampleHeightFbm(x, y + 0.014f, z, route);
				float bump = Mth.clamp((h - hx) * 10.5f + (h - hy) * 8.5f, -0.35f, 0.42f);

				int[] rgba = shadeSurfaceTexel(route, x, y, z, h, bump);
				surfImg.setPixelRGBA(px, py, abgr(rgba[0], rgba[1], rgba[2], 255));

				int[] cRgba = shadeCloudTexel(route, x, y, z);
				cloudImg.setPixelRGBA(px, py, abgr(cRgba[0], cRgba[1], cRgba[2], cRgba[3]));
			}
		}

		DynamicTexture surfDyn = new DynamicTexture(surfImg);
		surfDyn.setFilter(true, false);
		DynamicTexture cloudDyn = new DynamicTexture(cloudImg);
		cloudDyn.setFilter(true, false);

		ResourceLocation surfId = GalacticPurpose.id("dynamic/planet_" + route.id() + "_surface");
		ResourceLocation cloudId = GalacticPurpose.id("dynamic/planet_" + route.id() + "_clouds");
		Minecraft.getInstance().getTextureManager().register(surfId, surfDyn);
		Minecraft.getInstance().getTextureManager().register(cloudId, cloudDyn);
		return new PlanetTextures(surfId, cloudId);
	}

	private static int abgr(int r, int g, int b, int a) {
		return ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);
	}

	private static float sampleHeightFbm(float x, float y, float z, ExoplanetRoute route) {
		float seed = route.ordinal() * 2.37f;
		float o1 = (float) (Math.sin(x * 3.1 + seed) * Math.cos(z * 2.9 - y * 2.2));
		float o2 = (float) (Math.sin(x * 7.4 - y * 6.3 + seed) * Math.cos(z * 7.8 + x * 4.1)) * 0.50f;
		float o3 = (float) (Math.sin(z * 16.5 + y * 13.2 - x * 9.0) * Math.cos(x * 15.8 - z * 11.4)) * 0.25f;
		float o4 = (float) (Math.sin(x * 33.0 + z * 28.0 + y * 24.0)) * 0.13f;
		float o5 = (float) (Math.cos(y * 62.0 - x * 48.0 + z * 54.0)) * 0.065f;
		return Mth.clamp(0.5f + 0.34f * (o1 + o2 + o3 + o4 + o5), 0.0f, 1.0f);
	}

	private static int[] shadeSurfaceTexel(
			ExoplanetRoute route, float x, float y, float z, float h, float bump
	) {
		// Micro-relief bump modulation keeps close-orbit craters & ridges razor sharp
		float relief = Mth.clamp(0.92f + bump, 0.58f, 1.38f);
		float craterRing = (float) Math.abs(Math.sin(x * 19.0 + y * 23.0 + z * 17.0));
		float ridge = 1.0f - Math.abs(h * 2.0f - 1.0f);

		return switch (route) {
			case HEPHAESTUS_IV -> {
				// Fractured basalt plates + branching glowing magma river canyons & glowing volcanic calderas
				float fissureNoise = (float) Math.abs(
						Math.sin(x * 5.2 + z * 4.6 + h * 4.2) * Math.cos(y * 5.9 - x * 3.8 + h * 2.7));
				float microFissure = (float) Math.abs(Math.sin(x * 18.0 - z * 15.0 + y * 14.0));
				float magma = Mth.clamp((0.15f - fissureNoise) / 0.15f, 0.0f, 1.0f);
				float fineLava = Mth.clamp((0.065f - microFissure) / 0.065f, 0.0f, 1.0f) * 0.65f;
				float glow = Math.max(magma, fineLava);

				float basalt = (0.35f + 0.65f * h) * relief;
				float r = Mth.clamp(Mth.lerp(glow, 62.0f * basalt + ridge * 38.0f, 255.0f), 0.0f, 255.0f);
				float g = Mth.clamp(Mth.lerp(glow, 24.0f * basalt + ridge * 16.0f, 128.0f + glow * 112.0f), 0.0f, 255.0f);
				float b = Mth.clamp(Mth.lerp(glow, 18.0f * basalt, 26.0f + glow * 58.0f), 0.0f, 255.0f);
				yield new int[]{Math.round(r), Math.round(g), Math.round(b)};
			}
			case PELAGIA_9 -> {
				// Abyssal ocean trenches, glowing turquoise coral atoll shelves & sun glint ridges
				float atoll = Mth.clamp((h - 0.56f) / 0.24f, 0.0f, 1.0f);
				float foam = Mth.clamp((craterRing - 0.86f) / 0.14f, 0.0f, 1.0f) * atoll * 0.6f;
				float r = Mth.clamp((Mth.lerp(atoll, 12.0f, 32.0f) + foam * 180.0f) * relief, 0.0f, 255.0f);
				float g = Mth.clamp((Mth.lerp(atoll, 62.0f, 178.0f) + foam * 60.0f) * relief, 0.0f, 255.0f);
				float b = Mth.clamp((Mth.lerp(atoll, 148.0f, 232.0f) + foam * 22.0f) * relief, 0.0f, 255.0f);
				yield new int[]{Math.round(r), Math.round(g), Math.round(b)};
			}
			case NYX_0 -> {
				// Cryogenic slate plates, frost ridges & glowing violet-cyan quantum fissure veins
				float fissure = (float) Math.abs(Math.sin(x * 6.4 - y * 5.8 + h * 3.5) * Math.cos(z * 6.9 + x * 4.4));
				float glow = Mth.clamp((0.14f - fissure) / 0.14f, 0.0f, 1.0f);
				float frost = Mth.clamp((Math.abs(y) - 0.62f) / 0.30f + (h - 0.66f), 0.0f, 1.0f);
				float rBase = Mth.lerp(frost, 32.0f * relief, 165.0f * relief);
				float gBase = Mth.lerp(frost, 44.0f * relief, 205.0f * relief);
				float bBase = Mth.lerp(frost, 82.0f * relief, 245.0f * relief);
				yield new int[]{
					Mth.clamp(Math.round(Mth.lerp(glow, rBase, 198.0f)), 0, 255),
					Mth.clamp(Math.round(Mth.lerp(glow, gBase, 125.0f + glow * 90.0f)), 0, 255),
					Mth.clamp(Math.round(Mth.lerp(glow, bBase, 255.0f)), 0, 255)
				};
			}
			default -> {
				// Crisp archipelago continents, turquoise coastal reefs, forested lowlands, alpine snow peaks
				float land = Mth.clamp((h - 0.49f) / 0.09f, 0.0f, 1.0f);
				float highland = Mth.clamp((h - 0.68f) / 0.22f, 0.0f, 1.0f);
				float polarSnow = Mth.clamp((Math.abs(y) - 0.78f) / 0.16f, 0.0f, 1.0f);

				float rLand = Mth.lerp(highland, 46.0f, 168.0f);
				float gLand = Mth.lerp(highland, 148.0f, 152.0f);
				float bLand = Mth.lerp(highland, 68.0f, 128.0f);

				float r = Mth.lerp(land, 14.0f, rLand) * relief;
				float g = Mth.lerp(land, 92.0f, gLand) * relief;
				float b = Mth.lerp(land, 188.0f, bLand) * relief;

				float snow = Math.max(polarSnow, Mth.clamp((highland - 0.65f) * 2.4f, 0.0f, 1.0f));
				r = Mth.lerp(snow, r, 242.0f);
				g = Mth.lerp(snow, g, 248.0f);
				b = Mth.lerp(snow, b, 255.0f);
				yield new int[]{
					Mth.clamp(Math.round(r), 0, 255),
					Mth.clamp(Math.round(g), 0, 255),
					Mth.clamp(Math.round(b), 0, 255)
				};
			}
		};
	}

	private static int[] shadeCloudTexel(ExoplanetRoute route, float x, float y, float z) {
		float swirl = (float) (Math.sin(x * 4.5 + z * 5.2 + Math.sin(y * 8.0) * 1.4)
				* Math.cos(y * 7.5 - x * 3.8 + z * 3.1));
		float detail = (float) (Math.sin(x * 15.0 + y * 12.0 - z * 14.0) * 0.38f);
		float d = 0.5f + 0.5f * swirl + detail * 0.45f;
		float alpha = Mth.clamp((d - 0.56f) / 0.44f, 0.0f, 1.0f);
		int a = Math.round(alpha * (route == ExoplanetRoute.HEPHAESTUS_IV ? 125.0f : 175.0f));

		return switch (route) {
			case HEPHAESTUS_IV -> new int[]{245, 165, 110, a}; // Lit volcanic ash & amber cloud bands
			case PELAGIA_9 -> new int[]{225, 244, 255, a};     // Dense sapphire-white cyclone bands
			case NYX_0 -> new int[]{185, 200, 245, Math.round(a * 0.72f)};
			default -> new int[]{245, 250, 255, a};
		};
	}
}

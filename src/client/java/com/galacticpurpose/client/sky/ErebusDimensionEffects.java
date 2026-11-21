package com.galacticpurpose.client.sky;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Custom dimension fog & light effects for Erebus Singularity exoplanets.
 */
public class ErebusDimensionEffects extends DimensionSpecialEffects {
	private final Vec3 fogBaseColor;
	private final boolean permanentNightLight;

	public ErebusDimensionEffects(float cloudHeight, Vec3 fogBaseColor, boolean permanentNightLight) {
		super(cloudHeight, true, SkyType.NONE, false, false);
		this.fogBaseColor = fogBaseColor;
		this.permanentNightLight = permanentNightLight;
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
		float factor = this.permanentNightLight ? 1.0f : (0.55f + 0.45f * daylight);
		return this.fogBaseColor.scale(factor);
	}

	@Override
	public boolean isFoggyAt(int x, int z) {
		return false;
	}

	@Override
	public float[] getSunriseColor(float timeOfDay, float partialTick) {
		return null;
	}
}

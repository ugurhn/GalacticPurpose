package com.galacticpurpose.registry;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Custom environmental damage type keys registered via data packs.
 */
public final class ModDamageTypes {
	public static final ResourceKey<DamageType> SOLAR_RADIATION =
		ResourceKey.create(Registries.DAMAGE_TYPE, GalacticPurpose.id("solar_radiation"));
	public static final ResourceKey<DamageType> HYPOTHERMIA =
		ResourceKey.create(Registries.DAMAGE_TYPE, GalacticPurpose.id("hypothermia"));
	public static final ResourceKey<DamageType> SOLAR_STORM_RADIATION =
		ResourceKey.create(Registries.DAMAGE_TYPE, GalacticPurpose.id("solar_storm_radiation"));
	public static final ResourceKey<DamageType> EXTREME_HEAT =
		ResourceKey.create(Registries.DAMAGE_TYPE, GalacticPurpose.id("extreme_heat"));
	public static final ResourceKey<DamageType> HYDROSTATIC_PRESSURE =
		ResourceKey.create(Registries.DAMAGE_TYPE, GalacticPurpose.id("hydrostatic_pressure"));
	public static final ResourceKey<DamageType> CRYO_VOID =
		ResourceKey.create(Registries.DAMAGE_TYPE, GalacticPurpose.id("cryo_void"));

	private ModDamageTypes() {
	}
}

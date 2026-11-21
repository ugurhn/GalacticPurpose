package com.galacticpurpose.registry;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.entity.SingularityCollapseEntity;
import com.galacticpurpose.entity.StarshipSeatEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registers custom Galactic Purpose world entities.
 */
public final class ModEntities {
	public static final EntityType<SingularityCollapseEntity> SINGULARITY_COLLAPSE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		GalacticPurpose.id("singularity_collapse"),
		EntityType.Builder.<SingularityCollapseEntity>of(SingularityCollapseEntity::new, MobCategory.MISC)
			.sized(2.4f, 2.4f)
			.fireImmune()
			.clientTrackingRange(12)
			.updateInterval(2)
			.build()
	);
	public static final EntityType<InterstellarShipEntity> INTERSTELLAR_SHIP = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		GalacticPurpose.id("interstellar_ship"),
		EntityType.Builder.<InterstellarShipEntity>of(InterstellarShipEntity::new, MobCategory.MISC)
			.sized(6.4f, 4.4f)
			.fireImmune()
			.clientTrackingRange(20)
			.updateInterval(1)
			.build()
	);
	public static final EntityType<StarshipSeatEntity> STARSHIP_SEAT = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		GalacticPurpose.id("starship_seat"),
		EntityType.Builder.<StarshipSeatEntity>of(StarshipSeatEntity::new, MobCategory.MISC)
			.sized(0.85f, 1.10f)
			.fireImmune()
			.clientTrackingRange(8)
			.updateInterval(5)
			.build()
	);

	private ModEntities() {
	}

	public static void register() {
		// Static initialization performs entity type registration.
	}
}

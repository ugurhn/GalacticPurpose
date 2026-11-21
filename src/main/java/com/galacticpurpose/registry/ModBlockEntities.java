package com.galacticpurpose.registry;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.block.BioDistillerBlockEntity;
import com.galacticpurpose.block.DeepSpaceTelescopeBlockEntity;
import com.galacticpurpose.block.SolarFluxGeneratorBlockEntity;
import com.galacticpurpose.block.StarshipCargoLockerBlockEntity;
import com.galacticpurpose.block.TerraformingMachineBlockEntity;
import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.block.WormholeEnergyInjectorBlockEntity;
import com.galacticpurpose.terraforming.TerraformingMachineType;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registers block entities and their TeamReborn Energy storage lookups.
 */
public final class ModBlockEntities {
	private static final Map<TerraformingMachineType, BlockEntityType<TerraformingMachineBlockEntity>> MACHINES =
		new EnumMap<>(TerraformingMachineType.class);

	public static final BlockEntityType<BioDistillerBlockEntity> BIO_DISTILLER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		GalacticPurpose.id("bio_distiller"),
		BlockEntityType.Builder.of(BioDistillerBlockEntity::new, ModBlocks.BIO_DISTILLER).build(null)
	);
	public static final BlockEntityType<DeepSpaceTelescopeBlockEntity> DEEP_SPACE_TELESCOPE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		GalacticPurpose.id("deep_space_telescope"),
		BlockEntityType.Builder.of(DeepSpaceTelescopeBlockEntity::new, ModBlocks.DEEP_SPACE_TELESCOPE).build(null)
	);
	public static final BlockEntityType<WormholeCoreBlockEntity> WORMHOLE_CORE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		GalacticPurpose.id("wormhole_core"),
		BlockEntityType.Builder.of(WormholeCoreBlockEntity::new, ModBlocks.WORMHOLE_CORE).build(null)
	);
	public static final BlockEntityType<WormholeEnergyInjectorBlockEntity> WORMHOLE_INJECTOR = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		GalacticPurpose.id("wormhole_injector"),
		BlockEntityType.Builder.of(WormholeEnergyInjectorBlockEntity::new, ModBlocks.WORMHOLE_INJECTOR).build(null)
	);
	public static final BlockEntityType<SolarFluxGeneratorBlockEntity> SOLAR_FLUX_GENERATOR = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		GalacticPurpose.id("solar_flux_generator"),
		BlockEntityType.Builder.of(SolarFluxGeneratorBlockEntity::new, ModBlocks.SOLAR_FLUX_GENERATOR).build(null)
	);
	public static final BlockEntityType<StarshipCargoLockerBlockEntity> STARSHIP_CARGO_LOCKER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		GalacticPurpose.id("starship_cargo_locker"),
		BlockEntityType.Builder.of(StarshipCargoLockerBlockEntity::new, ModBlocks.STARSHIP_CARGO_LOCKER).build(null)
	);

	static {
		for (TerraformingMachineType type : TerraformingMachineType.values()) {
			BlockEntityType<TerraformingMachineBlockEntity> beType = Registry.register(
				BuiltInRegistries.BLOCK_ENTITY_TYPE,
				GalacticPurpose.id(type.id()),
				BlockEntityType.Builder.of(
					(pos, state) -> new TerraformingMachineBlockEntity(type, pos, state),
					ModBlocks.forMachine(type)
				).build(null)
			);
			MACHINES.put(type, beType);
		}
	}

	private ModBlockEntities() {
	}

	public static BlockEntityType<TerraformingMachineBlockEntity> forMachine(TerraformingMachineType type) {
		return MACHINES.get(type);
	}

	public static void register() {
		for (BlockEntityType<TerraformingMachineBlockEntity> beType : MACHINES.values()) {
			EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.energyStorage(), beType);
			ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, beType);
		}
		EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.energyStorage(), BIO_DISTILLER);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, BIO_DISTILLER);
		EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.energyStorage(), DEEP_SPACE_TELESCOPE);
		EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.energyStorage(), WORMHOLE_CORE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, WORMHOLE_CORE);
		EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.coreProxy(), WORMHOLE_INJECTOR);
		EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.energyStorage(), SOLAR_FLUX_GENERATOR);
	}
}

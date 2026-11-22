package com.galacticpurpose.registry;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.block.BioDistillerBlock;
import com.galacticpurpose.block.DeepSpaceTelescopeBlock;
import com.galacticpurpose.block.ExoplanetFloraBlock;
import com.galacticpurpose.block.MarsBioBloomBlock;
import com.galacticpurpose.block.ShipAssemblyPadBlock;
import com.galacticpurpose.block.SolarFluxGeneratorBlock;
import com.galacticpurpose.block.SolarTelemetryMonitorBlock;
import com.galacticpurpose.block.StarshipCargoLockerBlock;
import com.galacticpurpose.block.StarshipHelmBlock;
import com.galacticpurpose.block.TerraformingMachineBlock;
import com.galacticpurpose.block.WormholeCoreBlock;
import com.galacticpurpose.block.WormholeEnergyInjectorBlock;
import com.galacticpurpose.block.WormholePortalBlock;
import com.galacticpurpose.block.WormholeReturnBeaconBlock;
import com.galacticpurpose.block.WormholeStabilizerPylonBlock;
import com.galacticpurpose.terraforming.TerraformingMachineType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registers all blocks and matching block items for Galactic Purpose.
 */
public final class ModBlocks {
	private static final Map<TerraformingMachineType, Block> MACHINES =
		new EnumMap<>(TerraformingMachineType.class);

	public static final Block THERMAL_HEATER = registerMachine(TerraformingMachineType.THERMAL_HEATER);
	public static final Block PRESSURE_DRILL = registerMachine(TerraformingMachineType.PRESSURE_DRILL);
	public static final Block FLORA_OXYGENATOR = registerMachine(TerraformingMachineType.FLORA_OXYGENATOR);
	public static final Block ATMOSPHERE_CONDENSER = registerMachine(TerraformingMachineType.ATMOSPHERE_CONDENSER);

	public static final Block LEAD_ORE = registerWithItem("lead_ore", new Block(
		BlockBehaviour.Properties.of().strength(3.0f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
	));
	public static final Block DEEPSLATE_LEAD_ORE = registerWithItem("deepslate_lead_ore", new Block(
		BlockBehaviour.Properties.of().strength(4.5f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
	));
	public static final Block LEAD_CONCRETE = registerWithItem("lead_concrete", new Block(
		BlockBehaviour.Properties.of().strength(6.0f, 1200.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
	));
	public static final Block LEADED_GLASS = registerWithItem("leaded_glass", new TransparentBlock(
		BlockBehaviour.Properties.of()
			.strength(2.5f, 120.0f)
			.sound(SoundType.GLASS)
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
	));
	public static final Block LEADED_DOOR = registerWithItem("leaded_door",
		new com.galacticpurpose.block.LeadedDoorBlock(
			BlockBehaviour.Properties.of()
				.strength(6.0f, 1200.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.noOcclusion()
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block SOLAR_TELEMETRY_MONITOR = registerWithItem("solar_telemetry_monitor",
		new SolarTelemetryMonitorBlock(
			BlockBehaviour.Properties.of()
				.strength(3.0f, 12.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.lightLevel(state -> 6)
		)
	);
	public static final Block SOLAR_FLUX_GENERATOR = registerWithItem("solar_flux_generator",
		new SolarFluxGeneratorBlock(
			BlockBehaviour.Properties.of()
				.strength(3.5f, 8.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.lightLevel(state -> 4)
		)
	);
	public static final Block MARS_BIO_BLOOM = registerWithItem("mars_bio_bloom",
		new MarsBioBloomBlock(
			BlockBehaviour.Properties.of()
				.noCollission()
				.instabreak()
				.sound(SoundType.GRASS)
				.lightLevel(state -> 7)
				.offsetType(BlockBehaviour.OffsetType.XZ)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block BIO_DISTILLER = registerWithItem("bio_distiller",
		new BioDistillerBlock(
			BlockBehaviour.Properties.of()
				.strength(3.5f, 6.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.lightLevel(state -> state.getValue(BioDistillerBlock.LIT) ? 10 : 0)
		)
	);
	public static final Block DEEP_SPACE_TELESCOPE = registerWithItem("deep_space_telescope",
		new DeepSpaceTelescopeBlock(
			BlockBehaviour.Properties.of()
				.strength(3.5f, 6.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.noOcclusion()
				.lightLevel(state -> 4)
		)
	);
	public static final Block WORMHOLE_CORE = registerWithItem("wormhole_core",
		new WormholeCoreBlock(
			BlockBehaviour.Properties.of()
				.strength(5.0f, 1200.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.noOcclusion()
				.lightLevel(state -> switch (state.getValue(WormholeCoreBlock.STAGE)) {
					case 1 -> 10;
					case 2 -> 15;
					case 3 -> 12;
					default -> 4;
				})
		)
	);
	public static final Block WORMHOLE_STABILIZER_PYLON = registerWithItem("wormhole_stabilizer_pylon",
		new WormholeStabilizerPylonBlock(
			BlockBehaviour.Properties.of()
				.strength(4.5f, 1200.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.noOcclusion()
				.lightLevel(state -> 6)
		)
	);
	public static final Block WORMHOLE_INJECTOR = registerWithItem("wormhole_injector",
		new WormholeEnergyInjectorBlock(
			BlockBehaviour.Properties.of()
				.strength(4.5f, 1200.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.noOcclusion()
				.lightLevel(state -> 7)
		)
	);
	public static final Block WORMHOLE_PORTAL = registerBlockOnly("wormhole_portal",
		new WormholePortalBlock(
			BlockBehaviour.Properties.of()
				.noCollission()
				.noLootTable()
				.strength(-1.0f, 3600000.0f)
				.noOcclusion()
				.sound(SoundType.AMETHYST)
				.lightLevel(state -> 15)
				.pushReaction(PushReaction.BLOCK)
		)
	);
	public static final Block WORMHOLE_RETURN_BEACON = registerWithItem("wormhole_return_beacon",
		new WormholeReturnBeaconBlock(
			BlockBehaviour.Properties.of()
				.strength(-1.0f, 3600000.0f)
				.sound(SoundType.AMETHYST)
				.lightLevel(state -> 14)
				.pushReaction(PushReaction.BLOCK)
		)
	);
	public static final Block THERMAL_SHARD_ORE = registerWithItem("thermal_shard_ore",
		new Block(
			BlockBehaviour.Properties.of()
				.strength(3.5f, 6.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.BASALT)
				.lightLevel(state -> 6)
		)
	);
	public static final Block FUSION_RELAY_WRECKAGE = registerWithItem("fusion_relay_wreckage",
		new Block(
			BlockBehaviour.Properties.of()
				.strength(4.0f, 12.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.NETHERITE_BLOCK)
				.lightLevel(state -> 9)
		)
	);
	public static final Block ABYSSAL_VENT = registerWithItem("abyssal_vent",
		new Block(
			BlockBehaviour.Properties.of()
				.strength(3.0f, 6.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.CORAL_BLOCK)
				.lightLevel(state -> 13)
		)
	);
	public static final Block QUANTUM_CRYSTAL_ORE = registerWithItem("quantum_crystal_ore",
		new Block(
			BlockBehaviour.Properties.of()
				.strength(4.0f, 6.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.AMETHYST)
				.lightLevel(state -> 9)
		)
	);
	public static final Block CRYO_BEACON = registerWithItem("cryo_beacon",
		new Block(
			BlockBehaviour.Properties.of()
				.strength(3.5f, 12.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.GLASS)
				.lightLevel(state -> 11)
		)
	);

	// === HEPHAESTUS-IV (Volcanic Forge World) ===
	public static final Block SCORCHED_BASALT = registerWithItem("scorched_basalt", new Block(
		BlockBehaviour.Properties.of().strength(2.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.BASALT)
	));
	public static final Block VOLCANIC_SLAGSTONE = registerWithItem("volcanic_slagstone", new Block(
		BlockBehaviour.Properties.of().strength(2.8f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.BASALT)
	));
	public static final Block IGNIS_ORE = registerWithItem("ignis_ore", new Block(
		BlockBehaviour.Properties.of().strength(3.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.BASALT).lightLevel(s -> 7)
	));
	public static final Block PYROCLAST_BRICKS = registerWithItem("pyroclast_bricks", new Block(
		BlockBehaviour.Properties.of().strength(3.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
	));
	public static final Block CHISELED_PYROCLAST = registerWithItem("chiseled_pyroclast", new Block(
		BlockBehaviour.Properties.of().strength(3.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
	));
	public static final Block PYROCLAST_PILLAR = registerWithItem("pyroclast_pillar", new RotatedPillarBlock(
		BlockBehaviour.Properties.of().strength(3.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.BASALT)
	));
	public static final Block MAGMA_CONDUIT_LAMP = registerWithItem("magma_conduit_lamp", new Block(
		BlockBehaviour.Properties.of().strength(2.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.GLASS).lightLevel(s -> 15)
	));
	public static final Block EMBER_ASH = registerWithItem("ember_ash", new Block(
		BlockBehaviour.Properties.of().strength(0.8f, 1.0f).sound(SoundType.SAND).lightLevel(s -> 3)
	));

	// === PELAGIA-9 (Deep Abyss Ocean World) ===
	public static final Block ABYSSAL_SHALE = registerWithItem("abyssal_shale", new Block(
		BlockBehaviour.Properties.of().strength(2.6f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
	));
	public static final Block ABYSSAL_CORALSTONE = registerWithItem("abyssal_coralstone", new Block(
		BlockBehaviour.Properties.of().strength(2.4f, 5.0f).requiresCorrectToolForDrops().sound(SoundType.CORAL_BLOCK)
	));
	public static final Block PELAGIC_BRICKS = registerWithItem("pelagic_bricks", new Block(
		BlockBehaviour.Properties.of().strength(3.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
	));
	public static final Block CARVED_PELAGIC_TILE = registerWithItem("carved_pelagic_tile", new Block(
		BlockBehaviour.Properties.of().strength(3.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
	));
	public static final Block PELAGIC_PILLAR = registerWithItem("pelagic_pillar", new RotatedPillarBlock(
		BlockBehaviour.Properties.of().strength(3.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
	));
	public static final Block BIOLUMINESCENT_LAMP = registerWithItem("bioluminescent_lamp", new Block(
		BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.GLASS).lightLevel(s -> 15)
	));
	public static final Block LUMINESCENT_SEA_POD = registerWithItem("luminescent_sea_pod", new ExoplanetFloraBlock(
		BlockBehaviour.Properties.of().noCollission().strength(0.3f).sound(SoundType.CORAL_BLOCK).lightLevel(s -> 12).pushReaction(PushReaction.DESTROY)
	));
	public static final Block PRESSURE_HULL_PLATING = registerWithItem("pressure_hull_plating", new Block(
		BlockBehaviour.Properties.of().strength(5.0f, 18.0f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)
	));

	// === NYX-0 (Cryo Rogue Void World) ===
	public static final Block CRYO_SLATE = registerWithItem("cryo_slate", new Block(
		BlockBehaviour.Properties.of().strength(2.8f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
	));
	public static final Block FROST_OBSIDIAN = registerWithItem("frost_obsidian", new Block(
		BlockBehaviour.Properties.of().strength(6.0f, 60.0f).requiresCorrectToolForDrops().sound(SoundType.GLASS).lightLevel(s -> 4)
	));
	public static final Block CRYO_BRICKS = registerWithItem("cryo_bricks", new Block(
		BlockBehaviour.Properties.of().strength(3.2f, 9.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
	));
	public static final Block CARVED_CRYO_TILE = registerWithItem("carved_cryo_tile", new Block(
		BlockBehaviour.Properties.of().strength(3.2f, 9.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
	));
	public static final Block CRYO_PILLAR = registerWithItem("cryo_pillar", new RotatedPillarBlock(
		BlockBehaviour.Properties.of().strength(3.2f, 9.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
	));
	public static final Block VOID_CRYSTAL_LAMP = registerWithItem("void_crystal_lamp", new Block(
		BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.AMETHYST).lightLevel(s -> 15)
	));
	public static final Block FROZEN_QUANTUM_SPIRE = registerWithItem("frozen_quantum_spire", new ExoplanetFloraBlock(
		BlockBehaviour.Properties.of().noCollission().strength(0.6f).sound(SoundType.AMETHYST).lightLevel(s -> 11).pushReaction(PushReaction.DESTROY)
	));
	public static final Block ZERO_K_PLATING = registerWithItem("zero_k_plating", new Block(
		BlockBehaviour.Properties.of().strength(4.5f, 16.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)
	));

	// === SANCTUARY (New Home Final World) ===
	public static final Block SANCTUARY_LOAM = registerWithItem("sanctuary_loam", new Block(
		BlockBehaviour.Properties.of().strength(0.7f, 1.0f).sound(SoundType.GRASS)
	));
	public static final Block LUMINOUS_LIMESTONE = registerWithItem("luminous_limestone", new Block(
		BlockBehaviour.Properties.of().strength(2.2f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.CALCITE)
	));
	public static final Block SANCTUARY_BRICKS = registerWithItem("sanctuary_bricks", new Block(
		BlockBehaviour.Properties.of().strength(2.8f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
	));
	public static final Block CARVED_SANCTUARY_STONE = registerWithItem("carved_sanctuary_stone", new Block(
		BlockBehaviour.Properties.of().strength(2.8f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
	));
	public static final Block SANCTUARY_PILLAR = registerWithItem("sanctuary_pillar", new RotatedPillarBlock(
		BlockBehaviour.Properties.of().strength(2.8f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)
	));
	public static final Block AETHER_LANTERN = registerWithItem("aether_lantern", new Block(
		BlockBehaviour.Properties.of().strength(2.0f, 6.0f).sound(SoundType.GLASS).lightLevel(s -> 15)
	));
	public static final Block SANCTUARY_BLOSSOM = registerWithItem("sanctuary_blossom", new ExoplanetFloraBlock(
		BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS).lightLevel(s -> 10).pushReaction(PushReaction.DESTROY)
	));
	public static final Block ARK_TITANIUM_PLATING = registerWithItem("ark_titanium_plating", new Block(
		BlockBehaviour.Properties.of().strength(4.5f, 16.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)
	));

	// === INTERSTELLAR STARSHIP & SPACE ENGINEERING SET ===
	public static final Block STARSHIP_HULL_PLATING = registerWithItem("starship_hull_plating", new Block(
		BlockBehaviour.Properties.of().strength(5.0f, 24.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)
	));
	public static final Block STARSHIP_RIBBED_PANEL = registerWithItem("starship_ribbed_panel", new RotatedPillarBlock(
		BlockBehaviour.Properties.of().strength(4.5f, 20.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)
	));
	public static final Block STARSHIP_GRATE = registerWithItem("starship_grate", new TransparentBlock(
		BlockBehaviour.Properties.of().strength(3.5f, 14.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)
			.noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)
	));
	public static final Block REINFORCED_VIEWPORT_GLASS = registerWithItem("reinforced_viewport_glass", new TransparentBlock(
		BlockBehaviour.Properties.of().strength(3.0f, 80.0f).sound(SoundType.GLASS)
			.noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)
	));
	public static final Block SHIP_ASSEMBLY_PAD = registerWithItem("ship_assembly_pad", new ShipAssemblyPadBlock(
		BlockBehaviour.Properties.of().strength(4.5f, 24.0f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 8)
	));
	public static final Block STARSHIP_HELM = registerWithItem("starship_helm", new StarshipHelmBlock(
		BlockBehaviour.Properties.of().strength(4.0f, 24.0f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 10)
	));
	public static final Block STARSHIP_CARGO_LOCKER = registerWithItem("starship_cargo_locker", new StarshipCargoLockerBlock(
		BlockBehaviour.Properties.of().strength(4.0f, 20.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)
	));
	public static final Block STARSHIP_THRUSTER_BLOCK = registerWithItem("starship_thruster_block", new Block(
		BlockBehaviour.Properties.of().strength(5.0f, 24.0f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 12)
	));
	public static final Block STARSHIP_LIGHT_STRIP = registerWithItem("starship_light_strip", new Block(
		BlockBehaviour.Properties.of().strength(2.0f, 8.0f).sound(SoundType.GLASS).lightLevel(s -> 15)
	));
	public static final Block IGNIS_ALLOY_BLOCK = registerWithItem("ignis_alloy_block", new Block(
		BlockBehaviour.Properties.of().strength(5.0f, 18.0f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 7)
	));

	private ModBlocks() {
	}

	private static Block registerMachine(TerraformingMachineType type) {
		Block block = new TerraformingMachineBlock(
			BlockBehaviour.Properties.of()
				.strength(3.5f, 6.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.lightLevel(state -> state.getValue(TerraformingMachineBlock.LIT) ? 10 : 0),
			type
		);
		return registerWithItem(type.id(), block);
	}

	private static Block registerBlockOnly(String id, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, GalacticPurpose.id(id), block);
	}

	private static Block registerWithItem(String id, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, GalacticPurpose.id(id), block);
		Registry.register(BuiltInRegistries.ITEM, GalacticPurpose.id(id), new BlockItem(block, new Item.Properties()));
		if (block instanceof TerraformingMachineBlock machine) {
			MACHINES.put(machine.machineType(), block);
		}
		return block;
	}

	public static Block forMachine(TerraformingMachineType type) {
		return MACHINES.get(type);
	}

	public static void register() {
		// Class initialization performs static registry bindings.
	}
}

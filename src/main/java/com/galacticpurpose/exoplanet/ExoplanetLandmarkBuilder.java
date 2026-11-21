package com.galacticpurpose.exoplanet;

import com.galacticpurpose.block.ExoplanetFloraBlock;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Generates grand, signature architectural ruins & research landmarks on each Erebus exoplanet
 * built out of that world's native custom blocks, housing mission relics, expedition logs, and emergency fuel caches.
 */
public final class ExoplanetLandmarkBuilder {
	public static final int LANDMARK_X = 0;
	public static final int LANDMARK_Z = -26;

	private ExoplanetLandmarkBuilder() {
	}

	public static BlockPos ensureLandmarkBuilt(ServerLevel level, ExoplanetRoute route) {
		level.getChunk(LANDMARK_X >> 4, LANDMARK_Z >> 4);
		return switch (route) {
			case HEPHAESTUS_IV -> buildFusionForgeTower(level, LANDMARK_X, LANDMARK_Z);
			case PELAGIA_9 -> buildAbyssalResearchDome(level, LANDMARK_X, LANDMARK_Z);
			case NYX_0 -> buildQuantumObservatoryRuin(level, LANDMARK_X, LANDMARK_Z);
			case SANCTUARY -> buildSanctuaryColonyMonument(level, LANDMARK_X, LANDMARK_Z);
		};
	}

	/**
	 * Hephaestus-IV: Multi-story Ancient Fusion Forge Tower built from Pyroclast Bricks, Pillars,
	 * Magma Conduit Lamps, and Ignis Ore veins, holding the Ancient Fusion Relay & emergency fuel chest.
	 */
	private static BlockPos buildFusionForgeTower(ServerLevel level, int lx, int lz) {
		int y = Math.max(66, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz));
		BlockPos base = new BlockPos(lx, y, lz);
		if (level.getBlockState(base.above()).is(ModBlocks.FUSION_RELAY_WRECKAGE)
			|| level.getBlockState(base.offset(-5, 0, -5)).is(ModBlocks.PYROCLAST_BRICKS)) {
			return base;
		}

		BlockState bricks = ModBlocks.PYROCLAST_BRICKS.defaultBlockState();
		BlockState carved = ModBlocks.CHISELED_PYROCLAST.defaultBlockState();
		BlockState pillar = ModBlocks.PYROCLAST_PILLAR.defaultBlockState();
		BlockState lamp = ModBlocks.MAGMA_CONDUIT_LAMP.defaultBlockState();
		BlockState grate = ModBlocks.STARSHIP_GRATE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		// Heavy foundation + clear interior
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				for (int py = -3; py <= -1; py++) {
					level.setBlock(base.offset(dx, py, dz), bricks, Block.UPDATE_ALL);
				}
				boolean edge = Math.abs(dx) == 5 || Math.abs(dz) == 5;
				level.setBlock(base.offset(dx, 0, dz), edge ? carved : bricks, Block.UPDATE_ALL);
				for (int dy = 1; dy <= 10; dy++) {
					level.setBlock(base.offset(dx, dy, dz), air, Block.UPDATE_ALL);
				}
			}
		}

		// 4 corner forge towers & upper catwalk ring
		for (int sx : new int[]{-5, 5}) {
			for (int sz : new int[]{-5, 5}) {
				for (int dy = 1; dy <= 9; dy++) {
					level.setBlock(base.offset(sx, dy, sz), dy == 5 ? lamp : pillar, Block.UPDATE_ALL);
				}
				level.setBlock(base.offset(sx, 10, sz), carved, Block.UPDATE_ALL);
			}
		}
		for (int d = -4; d <= 4; d++) {
			if (Math.abs(d) > 1) {
				level.setBlock(base.offset(d, 1, -5), bricks, Block.UPDATE_ALL);
				level.setBlock(base.offset(d, 2, -5), bricks, Block.UPDATE_ALL);
				level.setBlock(base.offset(d, 1, 5), bricks, Block.UPDATE_ALL);
				level.setBlock(base.offset(-5, 1, d), bricks, Block.UPDATE_ALL);
				level.setBlock(base.offset(5, 1, d), bricks, Block.UPDATE_ALL);
			}
			// Elevated industrial catwalk at y+5
			level.setBlock(base.offset(d, 5, -4), grate, Block.UPDATE_ALL);
			level.setBlock(base.offset(d, 5, 4), grate, Block.UPDATE_ALL);
			level.setBlock(base.offset(-4, 5, d), grate, Block.UPDATE_ALL);
			level.setBlock(base.offset(4, 5, d), grate, Block.UPDATE_ALL);
			// Ruined crown arches at y+9
			if (Math.abs(d) >= 2) {
				level.setBlock(base.offset(d, 9, -5), bricks, Block.UPDATE_ALL);
				level.setBlock(base.offset(d, 9, 5), bricks, Block.UPDATE_ALL);
			}
		}

		// Glowing center reactor dais + Ancient Fusion Relay + Ignis Ore + Emergency Supply Chest
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlock(base.offset(dx, 0, dz), (dx == 0 && dz == 0) ? lamp : carved, Block.UPDATE_ALL);
			}
		}
		level.setBlock(base.above(), ModBlocks.FUSION_RELAY_WRECKAGE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base.offset(-3, 0, -3), ModBlocks.IGNIS_ORE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base.offset(3, 0, -3), ModBlocks.THERMAL_SHARD_ORE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base.offset(3, 1, 3), ModBlocks.IGNIS_ALLOY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);

		placeSupplyChest(level, base.offset(-3, 1, 3), ModItems.EXPEDITION_LOG_1);
		return base;
	}

	/**
	 * Pelagia-9: Underwater Sunken Abyssal Research Dome framed in Pressure Hull Plating &
	 * Reinforced Viewport Glass, lit by Bioluminescent Lamps and Sea Pods around the Abyssal Bio-Vent.
	 */
	private static BlockPos buildAbyssalResearchDome(ServerLevel level, int lx, int lz) {
		int floorY = Math.max(16, Math.min(62, level.getHeight(Heightmap.Types.OCEAN_FLOOR, lx, lz)));
		BlockPos base = new BlockPos(lx, floorY, lz);
		if (level.getBlockState(base.above(2)).is(ModBlocks.ABYSSAL_VENT)
			|| level.getBlockState(base.offset(-4, 0, -4)).is(ModBlocks.PRESSURE_HULL_PLATING)) {
			return base;
		}

		BlockState hull = ModBlocks.PRESSURE_HULL_PLATING.defaultBlockState();
		BlockState bricks = ModBlocks.PELAGIC_BRICKS.defaultBlockState();
		BlockState carved = ModBlocks.CARVED_PELAGIC_TILE.defaultBlockState();
		BlockState pillar = ModBlocks.PELAGIC_PILLAR.defaultBlockState();
		BlockState glass = ModBlocks.REINFORCED_VIEWPORT_GLASS.defaultBlockState();
		BlockState lamp = ModBlocks.BIOLUMINESCENT_LAMP.defaultBlockState();
		BlockState seaPod = ModBlocks.LUMINESCENT_SEA_POD.defaultBlockState().setValue(ExoplanetFloraBlock.WATERLOGGED, true);
		BlockState water = Blocks.WATER.defaultBlockState();

		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				int r2 = dx * dx + dz * dz;
				if (r2 > 28) continue;
				level.setBlock(base.offset(dx, -1, dz), bricks, Block.UPDATE_ALL);
				level.setBlock(base.offset(dx, 0, dz), r2 >= 18 ? hull : carved, Block.UPDATE_ALL);
				for (int dy = 1; dy <= 6; dy++) {
					double d3 = Math.sqrt(r2 + dy * dy);
					if (d3 >= 4.4 && d3 <= 5.45) {
						boolean doorway = (dy <= 2 && Math.abs(dx) <= 1 && dz > 3);
						level.setBlock(base.offset(dx, dy, dz), doorway ? water : (dy == 1 ? hull : glass), Block.UPDATE_ALL);
					} else if (d3 < 4.4) {
						level.setBlock(base.offset(dx, dy, dz), water, Block.UPDATE_ALL);
					}
				}
			}
		}

		// 4 glowing oceanic support columns + central Bio-Vent pedestal + glowing Sea Pods
		for (int sx : new int[]{-3, 3}) {
			for (int sz : new int[]{-3, 3}) {
				level.setBlock(base.offset(sx, 1, sz), pillar, Block.UPDATE_ALL);
				level.setBlock(base.offset(sx, 2, sz), lamp, Block.UPDATE_ALL);
				level.setBlock(base.offset(sx, 3, sz), pillar, Block.UPDATE_ALL);
			}
		}
		level.setBlock(base.above(), bricks, Block.UPDATE_ALL);
		level.setBlock(base.above(2), ModBlocks.ABYSSAL_VENT.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base.offset(-2, 1, 0), seaPod, Block.UPDATE_ALL);
		level.setBlock(base.offset(2, 1, 0), seaPod, Block.UPDATE_ALL);
		level.setBlock(base.offset(0, 1, -2), seaPod, Block.UPDATE_ALL);

		placeSupplyChest(level, base.offset(0, 1, 2), ModItems.EXPEDITION_LOG_2);
		return base;
	}

	/**
	 * Nyx-0: Frozen Quantum Observatory Ruin built of Cryo Bricks, Cryo Pillars, Zero-K Plating &
	 * Void Crystal Lamps surrounding the Cryo Beacon & Quantum Crystal cluster.
	 */
	private static BlockPos buildQuantumObservatoryRuin(ServerLevel level, int lx, int lz) {
		int y = Math.max(60, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz));
		BlockPos base = new BlockPos(lx, y, lz);
		if (level.getBlockState(base.above(2)).is(ModBlocks.CRYO_BEACON)
			|| level.getBlockState(base.offset(-5, 0, -5)).is(ModBlocks.CRYO_BRICKS)) {
			return base;
		}

		BlockState bricks = ModBlocks.CRYO_BRICKS.defaultBlockState();
		BlockState carved = ModBlocks.CARVED_CRYO_TILE.defaultBlockState();
		BlockState pillar = ModBlocks.CRYO_PILLAR.defaultBlockState();
		BlockState plating = ModBlocks.ZERO_K_PLATING.defaultBlockState();
		BlockState lamp = ModBlocks.VOID_CRYSTAL_LAMP.defaultBlockState();
		BlockState spire = ModBlocks.FROZEN_QUANTUM_SPIRE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				for (int py = -3; py <= -1; py++) {
					level.setBlock(base.offset(dx, py, dz), bricks, Block.UPDATE_ALL);
				}
				boolean ring = Math.abs(dx) == 5 || Math.abs(dz) == 5;
				level.setBlock(base.offset(dx, 0, dz), ring ? plating : carved, Block.UPDATE_ALL);
				for (int dy = 1; dy <= 9; dy++) {
					level.setBlock(base.offset(dx, dy, dz), air, Block.UPDATE_ALL);
				}
			}
		}

		// Crystalline monolith spires & vaulted broken observatory ribs
		for (int sx : new int[]{-5, 5}) {
			for (int sz : new int[]{-5, 5}) {
				for (int dy = 1; dy <= 7; dy++) {
					level.setBlock(base.offset(sx, dy, sz), dy == 4 ? lamp : pillar, Block.UPDATE_ALL);
				}
				level.setBlock(base.offset(sx, 8, sz), spire, Block.UPDATE_ALL);
			}
		}
		for (int d = -4; d <= 4; d++) {
			if (Math.abs(d) >= 2) {
				level.setBlock(base.offset(d, 7, -5), bricks, Block.UPDATE_ALL);
				level.setBlock(base.offset(d, 7, 5), bricks, Block.UPDATE_ALL);
			}
		}

		level.setBlock(base.above(), plating, Block.UPDATE_ALL);
		level.setBlock(base.above(2), ModBlocks.CRYO_BEACON.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base.offset(2, 1, 0), ModBlocks.QUANTUM_CRYSTAL_ORE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base.offset(-2, 1, 0), ModBlocks.QUANTUM_CRYSTAL_ORE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base.offset(0, 1, -2), ModBlocks.QUANTUM_CRYSTAL_ORE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(base.offset(2, 2, 0), spire, Block.UPDATE_ALL);
		level.setBlock(base.offset(-2, 2, 0), spire, Block.UPDATE_ALL);

		placeSupplyChest(level, base.offset(0, 1, 3), ModItems.EXPEDITION_LOG_3);
		return base;
	}

	/**
	 * Sanctuary: Sunlit New Home Colony Monument crafted in Sanctuary Bricks, Ark Titanium Plating,
	 * Aether Lanterns, and Sanctuary Blossoms.
	 */
	private static BlockPos buildSanctuaryColonyMonument(ServerLevel level, int lx, int lz) {
		int y = Math.max(66, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz));
		BlockPos base = new BlockPos(lx, y, lz);
		if (level.getBlockState(base.offset(-4, 0, -4)).is(ModBlocks.SANCTUARY_BRICKS)) {
			return base;
		}

		BlockState bricks = ModBlocks.SANCTUARY_BRICKS.defaultBlockState();
		BlockState carved = ModBlocks.CARVED_SANCTUARY_STONE.defaultBlockState();
		BlockState pillar = ModBlocks.SANCTUARY_PILLAR.defaultBlockState();
		BlockState titanium = ModBlocks.ARK_TITANIUM_PLATING.defaultBlockState();
		BlockState lantern = ModBlocks.AETHER_LANTERN.defaultBlockState();
		BlockState blossom = ModBlocks.SANCTUARY_BLOSSOM.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				for (int py = -2; py <= -1; py++) {
					level.setBlock(base.offset(dx, py, dz), bricks, Block.UPDATE_ALL);
				}
				boolean border = Math.abs(dx) == 4 || Math.abs(dz) == 4;
				level.setBlock(base.offset(dx, 0, dz), border ? titanium : carved, Block.UPDATE_ALL);
				for (int dy = 1; dy <= 7; dy++) {
					level.setBlock(base.offset(dx, dy, dz), air, Block.UPDATE_ALL);
				}
			}
		}

		for (int sx : new int[]{-4, 4}) {
			for (int sz : new int[]{-4, 4}) {
				for (int dy = 1; dy <= 5; dy++) {
					level.setBlock(base.offset(sx, dy, sz), dy == 5 ? lantern : pillar, Block.UPDATE_ALL);
				}
			}
		}
		// Central ark obelisk
		level.setBlock(base.above(1), titanium, Block.UPDATE_ALL);
		level.setBlock(base.above(2), carved, Block.UPDATE_ALL);
		level.setBlock(base.above(3), lantern, Block.UPDATE_ALL);
		level.setBlock(base.offset(-2, 1, 0), blossom, Block.UPDATE_ALL);
		level.setBlock(base.offset(2, 1, 0), blossom, Block.UPDATE_ALL);
		level.setBlock(base.offset(0, 1, -2), blossom, Block.UPDATE_ALL);

		placeSupplyChest(level, base.offset(0, 1, 2), ModItems.EXPEDITION_LOG_4);
		return base;
	}

	private static void placeSupplyChest(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item logItem) {
		level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof ChestBlockEntity chest) {
			chest.setItem(0, new ItemStack(logItem));
			chest.setItem(1, new ItemStack(ModItems.STARSHIP_FUEL_CELL, 2));
			chest.setItem(2, new ItemStack(ModItems.LEAD_ENERGY_CELL, 1));
			chest.setItem(3, new ItemStack(ModItems.EXOTIC_BIO_FUEL, 2));
			chest.setChanged();
		}
	}
}

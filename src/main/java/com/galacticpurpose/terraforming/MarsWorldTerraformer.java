package com.galacticpurpose.terraforming;

import com.galacticpurpose.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Budgeted TPS-safe world transformation applying ice melting and flora spreading on Mars.
 */
public final class MarsWorldTerraformer {
	private static final int MAX_BLOCK_OPS_PER_TICK = 8;
	private static final int HORIZONTAL_RADIUS = 28;

	private MarsWorldTerraformer() {
	}

	public static void tickBudgeted(ServerLevel level, MarsProgressSnapshot snapshot) {
		if (!snapshot.liquidWaterReady() && !snapshot.floraReady()) {
			return;
		}
		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) {
			return;
		}

		RandomSource random = level.random;
		for (int attempt = 0; attempt < MAX_BLOCK_OPS_PER_TICK; attempt++) {
			ServerPlayer anchor = players.get(random.nextInt(players.size()));
			int x = anchor.getBlockX() + random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
			int z = anchor.getBlockZ() + random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
			int y = Math.max(level.getMinBuildHeight() + 1,
				Math.min(level.getMaxBuildHeight() - 2, anchor.getBlockY() + random.nextInt(17) - 8));
			BlockPos sample = new BlockPos(x, y, z);
			if (!level.isLoaded(sample)) {
				continue;
			}
			tryTransformAt(level, sample, snapshot, random);
		}
	}

	public static boolean tryTransformAt(
		ServerLevel level,
		BlockPos samplePos,
		MarsProgressSnapshot snapshot,
		RandomSource random
	) {
		if (snapshot.liquidWaterReady()) {
			BlockState state = level.getBlockState(samplePos);
			if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
				level.setBlock(samplePos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
				return true;
			}
		}

		if (snapshot.floraReady()) {
			BlockPos topEmpty = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, samplePos);
			BlockPos groundPos = topEmpty.below();
			BlockState groundState = level.getBlockState(groundPos);
			if (isMarsConvertibleSurface(groundState) && level.isEmptyBlock(topEmpty)) {
				level.setBlock(groundPos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
				placeMarsFloraIfLucky(level, topEmpty, random);
				return true;
			}
			if (groundState.is(Blocks.GRASS_BLOCK) && level.isEmptyBlock(topEmpty) && random.nextInt(8) == 0) {
				placeMarsFloraIfLucky(level, topEmpty, random);
				return true;
			}
		}
		return false;
	}

	private static boolean isMarsConvertibleSurface(BlockState state) {
		if (state.is(Blocks.RED_SAND)
			|| state.is(Blocks.RED_SANDSTONE)
			|| state.is(Blocks.TERRACOTTA)
			|| state.is(Blocks.ORANGE_TERRACOTTA)
			|| state.is(Blocks.RED_TERRACOTTA)) {
			return true;
		}
		String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		return (path.contains("mars_surface")
			|| path.contains("mars_subsurface")
			|| path.equals("mars_sand")
			|| path.equals("mars_stone")) && !path.contains("ore");
	}

	private static void placeMarsFloraIfLucky(ServerLevel level, BlockPos pos, RandomSource random) {
		int roll = random.nextInt(12);
		if (roll == 0) {
			level.setBlock(pos, ModBlocks.MARS_BIO_BLOOM.defaultBlockState(), Block.UPDATE_ALL);
		} else if (roll <= 3) {
			level.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
		} else if (roll == 4) {
			level.setBlock(pos, Blocks.FERN.defaultBlockState(), Block.UPDATE_ALL);
		}
	}
}

package com.galacticpurpose.terraforming;

import com.galacticpurpose.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fast O(1) radiation shielding detector checking for Lead-Concrete/Leaded Glass bunkers or deep rock cover.
 */
public final class RadiationShelterChecker {
	private static final int ROOF_SEARCH_RADIUS = 8;
	private static final int WALL_SEARCH_RADIUS = 6;
	private static final int DEEP_COVER_SOLID_BLOCKS = 16;

	private RadiationShelterChecker() {
	}

	public static boolean isLeadShieldBlock(BlockState state) {
		return state.is(ModBlocks.LEAD_CONCRETE)
			|| state.is(ModBlocks.LEADED_GLASS)
			|| state.is(ModBlocks.SOLAR_TELEMETRY_MONITOR);
	}

	public static boolean isPositionShielded(ServerLevel level, BlockPos eyePos) {
		return hasLeadEnclosure(level, eyePos) || isDeepUnderground(level, eyePos);
	}

	private static boolean hasLeadEnclosure(ServerLevel level, BlockPos eyePos) {
		boolean leadRoofFound = false;
		for (int dy = 1; dy <= ROOF_SEARCH_RADIUS; dy++) {
			BlockState above = level.getBlockState(eyePos.above(dy));
			if (isLeadShieldBlock(above)) {
				leadRoofFound = true;
				break;
			}
			if (above.isSolidRender(level, eyePos.above(dy))) {
				// Solid non-lead roof blocks direct line of sight to lead roof above.
				break;
			}
		}
		if (!leadRoofFound) {
			return false;
		}

		for (Direction dir : Direction.Plane.HORIZONTAL) {
			boolean wallClosed = false;
			for (int dist = 1; dist <= WALL_SEARCH_RADIUS; dist++) {
				BlockPos checkPos = eyePos.relative(dir, dist);
				BlockState wallState = level.getBlockState(checkPos);
				if (isLeadShieldBlock(wallState) || wallState.isSolidRender(level, checkPos)) {
					wallClosed = true;
					break;
				}
			}
			if (!wallClosed) {
				return false;
			}
		}
		return true;
	}

	public static boolean isDeepUnderground(ServerLevel level, BlockPos eyePos) {
		int solidAbove = 0;
		int maxY = Math.min(level.getMaxBuildHeight() - 1, eyePos.getY() + 32);
		for (int y = eyePos.getY() + 1; y <= maxY; y++) {
			BlockPos pos = new BlockPos(eyePos.getX(), y, eyePos.getZ());
			BlockState state = level.getBlockState(pos);
			if (state.isSolidRender(level, pos)) {
				solidAbove++;
				if (solidAbove >= DEEP_COVER_SOLID_BLOCKS) {
					return true;
				}
			}
		}
		return false;
	}
}

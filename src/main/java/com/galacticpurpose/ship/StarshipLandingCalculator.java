package com.galacticpurpose.ship;

import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * Automated propulsive natural-terrain landing guidance & retro-burn kinematics for the Horizon Lander.
 * Scans untouched alien terrain for the flattest safe non-lava clearing (zero artificial stone launchpads!),
 * and deploys buoyant hydro-pontoons directly onto ocean-world Pelagia-9.
 */
public final class StarshipLandingCalculator {
	public static final double ENTRY_START_ALTITUDE = 132.0;
	public static final double RETRO_BURN_ALTITUDE = 58.0;
	public static final double LEGS_DEPLOY_ALTITUDE = 20.0;

	public record LandingSite(int x, int y, int z, boolean hydroPontoon) {
	}

	private StarshipLandingCalculator() {
	}

	/**
	 * Smooth propulsive retro-burn vertical descent speed (blocks/tick, negative downward).
	 */
	public static double computeDescentVelocity(double altitudeAboveGround) {
		if (altitudeAboveGround <= 0.12) {
			return 0.0;
		}
		if (altitudeAboveGround > RETRO_BURN_ALTITUDE) {
			return -1.85;
		}
		if (altitudeAboveGround > LEGS_DEPLOY_ALTITUDE) {
			double t = (altitudeAboveGround - LEGS_DEPLOY_ALTITUDE) / (RETRO_BURN_ALTITUDE - LEGS_DEPLOY_ALTITUDE);
			return -(0.36 + (1.85 - 0.36) * t * t);
		}
		double t = altitudeAboveGround / LEGS_DEPLOY_ALTITUDE;
		return -(0.08 + (0.36 - 0.08) * t);
	}

	public static boolean shouldDeployLandingLegs(double altitudeAboveGround) {
		return altitudeAboveGround <= LEGS_DEPLOY_ALTITUDE;
	}

	public static int scoreTerrainFlatness(int centerY, int[] sampleHeights, boolean hasHazardFluid) {
		if (hasHazardFluid) {
			return Integer.MAX_VALUE;
		}
		int penalty = 0;
		for (int h : sampleHeights) {
			int diff = Math.abs(h - centerY);
			penalty += diff * diff;
		}
		return penalty;
	}

	/**
	 * Scans a 36x36 natural zone around (0, 6) (clear of landmarks at Z=-26) to pick the flattest untouched clearing.
	 */
	public static LandingSite findNaturalLandingSite(ServerLevel level, ExoplanetRoute route) {
		if (route == ExoplanetRoute.PELAGIA_9) {
			level.getChunk(0, 0);
			return new LandingSite(0, 110, 4, true);
		}

		LandingSite best = null;
		int bestScore = Integer.MAX_VALUE;

		for (int cx = -16; cx <= 16; cx += 4) {
			for (int cz = -8; cz <= 20; cz += 4) {
				level.getChunk(cx >> 4, cz >> 4);
				int centerY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
				if (centerY < level.getMinBuildHeight() + 10) {
					continue;
				}
				BlockState below = level.getBlockState(new BlockPos(cx, centerY - 1, cz));
				boolean hazard = below.getFluidState().is(Fluids.LAVA)
					|| below.getFluidState().is(Fluids.FLOWING_LAVA)
					|| !below.blocksMotion();

				int[] samples = new int[8];
				int idx = 0;
				for (int ox : new int[]{-2, 0, 2}) {
					for (int oz : new int[]{-2, 0, 2}) {
						if (ox == 0 && oz == 0) continue;
						int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + ox, cz + oz);
						BlockState sBelow = level.getBlockState(new BlockPos(cx + ox, sy - 1, cz + oz));
						if (sBelow.getFluidState().is(Fluids.LAVA) || sBelow.getFluidState().is(Fluids.FLOWING_LAVA)) {
							hazard = true;
						}
						samples[idx++] = sy;
					}
				}
				int score = scoreTerrainFlatness(centerY, samples, hazard);
				if (score < bestScore) {
					bestScore = score;
					best = new LandingSite(cx, centerY, cz, false);
				}
			}
		}

		if (best != null) {
			return best;
		}
		int fallbackY = Math.max(68, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 4));
		return new LandingSite(0, fallbackY, 4, false);
	}
}

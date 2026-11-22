package com.galacticpurpose.terraforming;

import com.galacticpurpose.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Volumetric flood-fill + 66-ray spherical aperture radiation shielding scanner.
 * Detects sealed Lead-Concrete / Leaded-Glass / closed Leaded-Door bunkers, deep rock caves,
 * AND quantifies partial radiation leakage whenever walls, corners, roofs, or open doors leave gaps.
 */
public final class RadiationShelterChecker {
	private static final int ROOM_RADIUS_XZ = 7;
	private static final int ROOM_MAX_UP = 8;
	private static final int ROOM_MAX_DOWN = 4;
	private static final int MAX_FLOOD_CELLS = 650;
	private static final int DEEP_COVER_SOLID_BLOCKS = 16;

	private static final int RAY_COUNT = 66;
	private static final double[][] RAY_DIRS = buildFibonacciDirections(RAY_COUNT);
	private static final double RAY_MAX_DIST = 9.5;
	private static final double RAY_STEP = 0.25;

	public enum ShelterStatus {
		SEALED,
		LEAKY,
		EXPOSED
	}

	public record ShelterAssessment(
		double protectionPercent,
		double leakagePercent,
		boolean enclosed,
		boolean deepUnderground,
		int escapingRays,
		int totalRays,
		BlockPos sampleBreachPos
	) {
		public boolean isFullySealed() {
			return this.leakagePercent <= 0.01;
		}

		public ShelterStatus status() {
			if (this.leakagePercent <= 0.01) {
				return ShelterStatus.SEALED;
			}
			if (this.leakagePercent >= 99.99) {
				return ShelterStatus.EXPOSED;
			}
			return ShelterStatus.LEAKY;
		}
	}

	private RadiationShelterChecker() {
	}

	public static boolean isLeadShieldBlock(BlockState state) {
		if (state.is(ModBlocks.LEADED_DOOR)) {
			return !state.getValue(DoorBlock.OPEN);
		}
		return state.is(ModBlocks.LEAD_CONCRETE)
			|| state.is(ModBlocks.LEADED_GLASS)
			|| state.is(ModBlocks.SOLAR_TELEMETRY_MONITOR);
	}

	public static boolean isPositionShielded(ServerLevel level, BlockPos eyePos) {
		return evaluateShelter(level, eyePos).isFullySealed();
	}

	public static ShelterAssessment evaluateShelter(ServerLevel level, BlockPos eyePos) {
		boolean deep = isDeepUnderground(level, eyePos);
		if (deep && !hasNearbyOpenSkyShaft(level, eyePos)) {
			return new ShelterAssessment(100.0, 0.0, true, true, 0, RAY_COUNT, null);
		}

		// 1. Bounded 3D volumetric containment check from player eye position
		Queue<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		queue.add(eyePos);
		visited.add(eyePos);

		int floodEscapes = 0;
		int skyExposedCells = 0;
		int leadBoundaryHits = 0;
		int nonLeadUpperBoundaryHits = 0;
		BlockPos sampleBreach = null;

		while (!queue.isEmpty()) {
			BlockPos cur = queue.poll();
			boolean cellExposed = isExposedColumnToSky(level, cur);
			if (cellExposed) {
				skyExposedCells++;
				if (sampleBreach == null) {
					sampleBreach = cur;
				}
			}

			for (Direction dir : Direction.values()) {
				BlockPos next = cur.relative(dir);
				if (visited.contains(next)) {
					continue;
				}
				int dx = Math.abs(next.getX() - eyePos.getX());
				int dz = Math.abs(next.getZ() - eyePos.getZ());
				int dy = next.getY() - eyePos.getY();
				if (dx > ROOM_RADIUS_XZ || dz > ROOM_RADIUS_XZ || dy > ROOM_MAX_UP || dy < -ROOM_MAX_DOWN) {
					floodEscapes++;
					if (sampleBreach == null) {
						sampleBreach = cur;
					}
					continue;
				}

				BlockState state = level.getBlockState(next);
				if (isLeadShieldBlock(state)) {
					leadBoundaryHits++;
					continue;
				}
				if (isOpaqueBarrier(level, next, state)) {
					if (dir != Direction.DOWN) {
						nonLeadUpperBoundaryHits++;
					}
					continue;
				}

				if (visited.size() >= MAX_FLOOD_CELLS) {
					floodEscapes++;
					if (sampleBreach == null) {
						sampleBreach = next;
					}
					continue;
				}
				visited.add(next);
				// Do not waste expansion walking out across open sky once a breach is detected
				if (!cellExposed) {
					queue.add(next);
				}
			}
		}

		// 2. 66-direction spherical raycast catching wall/roof/corner gaps & measuring true solid-angle exposure
		int escapingRays = 0;
		int nonLeadRays = 0;
		int leadRays = 0;
		double cx = eyePos.getX() + 0.5;
		double cy = eyePos.getY() + 0.5;
		double cz = eyePos.getZ() + 0.5;

		for (double[] dir : RAY_DIRS) {
			boolean hitShield = false;
			boolean hitNonLead = false;
			boolean leakedToSky = false;
			for (double t = 0.45; t <= RAY_MAX_DIST; t += RAY_STEP) {
				BlockPos p = BlockPos.containing(cx + dir[0] * t, cy + dir[1] * t, cz + dir[2] * t);
				if (p.equals(eyePos)) {
					continue;
				}
				BlockState st = level.getBlockState(p);
				if (isLeadShieldBlock(st)) {
					hitShield = true;
					break;
				}
				if (isOpaqueBarrier(level, p, st)) {
					hitNonLead = true;
					break;
				}
				// Stepping into open sky air BEFORE hitting a shield/barrier means this ray exited a hole/breach!
				if (isExposedColumnToSky(level, p)) {
					leakedToSky = true;
					if (sampleBreach == null) {
						sampleBreach = p;
					}
					break;
				}
			}
			if (hitShield) {
				leadRays++;
			} else if (leakedToSky) {
				escapingRays++;
			} else if (hitNonLead) {
				nonLeadRays++;
			} else {
				escapingRays++;
				if (sampleBreach == null) {
					sampleBreach = BlockPos.containing(cx + dir[0] * 2.5, cy + dir[1] * 2.5, cz + dir[2] * 2.5);
				}
			}
		}

		boolean fullyContained = floodEscapes == 0 && skyExposedCells == 0 && escapingRays == 0;
		if (fullyContained && nonLeadUpperBoundaryHits == 0 && leadBoundaryHits > 0) {
			return new ShelterAssessment(100.0, 0.0, true, false, 0, RAY_COUNT, null);
		}

		// Quantify leakage risk smoothly with aperture & breach count (0..100%)
		double rayEscapeLeak = (escapingRays / (double) RAY_COUNT) * 100.0;
		double nonLeadSurfaceLeak = (nonLeadRays / (double) RAY_COUNT) * 35.0;
		double breachFactor = Math.min(35.0, skyExposedCells * 3.2 + Math.min(20, floodEscapes) * 0.5);
		double leak = rayEscapeLeak + nonLeadSurfaceLeak + breachFactor;

		if ((floodEscapes > 0 || skyExposedCells > 0 || escapingRays > 0) && leak < 15.0) {
			leak = 15.0;
		}
		if (skyExposedCells > 0 && escapingRays >= RAY_COUNT * 0.60) {
			leak = 100.0;
		}
		leak = Math.max(0.0, Math.min(100.0, leak));
		double protection = 100.0 - leak;

		return new ShelterAssessment(
			protection,
			leak,
			floodEscapes == 0 && skyExposedCells == 0,
			false,
			escapingRays,
			RAY_COUNT,
			sampleBreach
		);
	}

	private static boolean isOpaqueBarrier(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.is(ModBlocks.LEADED_DOOR)) {
			return false; // Open door lets radiation stream freely through
		}
		return state.isSolidRender(level, pos) || state.isCollisionShapeFullBlock(level, pos);
	}

	private static boolean isExposedColumnToSky(ServerLevel level, BlockPos pos) {
		int maxY = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + 24);
		for (int y = pos.getY() + 1; y <= maxY; y++) {
			BlockPos above = new BlockPos(pos.getX(), y, pos.getZ());
			BlockState st = level.getBlockState(above);
			if (isLeadShieldBlock(st) || st.isSolidRender(level, above)) {
				return false;
			}
		}
		return level.canSeeSky(pos);
	}

	private static boolean hasNearbyOpenSkyShaft(ServerLevel level, BlockPos origin) {
		Queue<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty()) {
			BlockPos cur = queue.poll();
			if (isExposedColumnToSky(level, cur)) {
				return true;
			}
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos next = cur.relative(dir);
				if (seen.contains(next)) {
					continue;
				}
				if (Math.abs(next.getX() - origin.getX()) > 4 || Math.abs(next.getZ() - origin.getZ()) > 4) {
					continue;
				}
				BlockState st = level.getBlockState(next);
				if (!isLeadShieldBlock(st) && !isOpaqueBarrier(level, next, st) && seen.size() < 96) {
					seen.add(next);
					queue.add(next);
				}
			}
		}
		return false;
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

	private static double[][] buildFibonacciDirections(int count) {
		double[][] dirs = new double[count][3];
		double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
		for (int i = 0; i < count; i++) {
			double y = 1.0 - (i / (double) (count - 1)) * 2.0;
			double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
			double theta = goldenAngle * i;
			dirs[i][0] = Math.cos(theta) * r;
			dirs[i][1] = y;
			dirs[i][2] = Math.sin(theta) * r;
		}
		return dirs;
	}
}

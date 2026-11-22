package com.galacticpurpose.wormhole;

import com.galacticpurpose.block.WormholeStabilizerPylonBlock;
import com.galacticpurpose.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates and builds both Personal (7x5x7, 4 stabilizers) and Colossal Ship-Scale Observatory
 * (45x41x45 octagonal open-sky accelerator monument with 4 curved monolith spires and 4x 3x3 = 36 stabilizers).
 */
public final class WormholeMultiblockValidator {
	public record ValidationResult(boolean valid, WormholeTier tier, BlockPos missingOffset, String expectedBlockKey) {
		public static ValidationResult ok(WormholeTier tier) {
			return new ValidationResult(true, tier, BlockPos.ZERO, "");
		}
	}

	public record StructureBlockSpec(BlockPos offset, BlockState targetState) {
	}

	public static final BlockPos INJECTOR_OFFSET = new BlockPos(0, -3, 0);
	public static final BlockPos COLOSSAL_INJECTOR_OFFSET = new BlockPos(0, -WormholeTier.COLOSSAL.injectorDrop(), 0);
	public static final BlockPos PORTAL_OFFSET = new BlockPos(0, -1, 0);
	public static final BlockPos LOWER_CLEAR_OFFSET = new BlockPos(0, -2, 0);

	public static final BlockPos[] PYLON_OFFSETS = {
		new BlockPos(3, 0, 0),
		new BlockPos(-3, 0, 0),
		new BlockPos(0, 0, 3),
		new BlockPos(0, 0, -3)
	};

	public static final BlockPos[] CONCRETE_OFFSETS = {
		new BlockPos(3, -1, 0), new BlockPos(3, -2, 0), new BlockPos(3, -3, 0),
		new BlockPos(-3, -1, 0), new BlockPos(-3, -2, 0), new BlockPos(-3, -3, 0),
		new BlockPos(0, -1, 3), new BlockPos(0, -2, 3), new BlockPos(0, -3, 3),
		new BlockPos(0, -1, -3), new BlockPos(0, -2, -3), new BlockPos(0, -3, -3),
		new BlockPos(3, -3, 3),
		new BlockPos(3, -3, -3),
		new BlockPos(-3, -3, 3),
		new BlockPos(-3, -3, -3)
	};

	private WormholeMultiblockValidator() {
	}

	public static ValidationResult validate(Level level, BlockPos corePos) {
		int d = WormholeTier.COLOSSAL.stabilizerDistance();
		boolean looksColossal = level.getBlockState(corePos.offset(COLOSSAL_INJECTOR_OFFSET)).is(ModBlocks.WORMHOLE_INJECTOR)
			|| level.getBlockState(corePos.offset(d, 0, 0)).is(ModBlocks.WORMHOLE_STABILIZER_PYLON)
			|| level.getBlockState(corePos.offset(-d, 0, 0)).is(ModBlocks.WORMHOLE_STABILIZER_PYLON)
			|| level.getBlockState(corePos.offset(0, 0, d)).is(ModBlocks.WORMHOLE_STABILIZER_PYLON)
			|| level.getBlockState(corePos.offset(0, 0, -d)).is(ModBlocks.WORMHOLE_STABILIZER_PYLON);
		if (looksColossal) {
			ValidationResult colossal = validateColossal(level, corePos);
			if (colossal.valid()) {
				return colossal;
			}
			ValidationResult personal = validatePersonal(level, corePos);
			return personal.valid() ? personal : colossal;
		}
		return validatePersonal(level, corePos);
	}

	public static ValidationResult validatePersonal(Level level, BlockPos corePos) {
		if (!level.getBlockState(corePos.offset(INJECTOR_OFFSET)).is(ModBlocks.WORMHOLE_INJECTOR)) {
			return new ValidationResult(false, WormholeTier.PERSONAL, INJECTOR_OFFSET, "block.galacticpurpose.wormhole_injector");
		}

		for (BlockPos offset : PYLON_OFFSETS) {
			if (!level.getBlockState(corePos.offset(offset)).is(ModBlocks.WORMHOLE_STABILIZER_PYLON)) {
				return new ValidationResult(false, WormholeTier.PERSONAL, offset, "block.galacticpurpose.wormhole_stabilizer_pylon");
			}
		}

		for (BlockPos offset : CONCRETE_OFFSETS) {
			if (!level.getBlockState(corePos.offset(offset)).is(ModBlocks.LEAD_CONCRETE)) {
				return new ValidationResult(false, WormholeTier.PERSONAL, offset, "block.galacticpurpose.lead_concrete");
			}
		}

		BlockState portalSlot = level.getBlockState(corePos.offset(PORTAL_OFFSET));
		if (!portalSlot.isAir() && !portalSlot.is(ModBlocks.WORMHOLE_PORTAL)) {
			return new ValidationResult(false, WormholeTier.PERSONAL, PORTAL_OFFSET, "block.minecraft.air");
		}

		BlockState lowerSlot = level.getBlockState(corePos.offset(LOWER_CLEAR_OFFSET));
		if (!lowerSlot.isAir()) {
			return new ValidationResult(false, WormholeTier.PERSONAL, LOWER_CLEAR_OFFSET, "block.minecraft.air");
		}

		return ValidationResult.ok(WormholeTier.PERSONAL);
	}

	public static ValidationResult validateColossal(Level level, BlockPos corePos) {
		int d = WormholeTier.COLOSSAL.stabilizerDistance(); // 21
		int drop = WormholeTier.COLOSSAL.injectorDrop();    // 21
		if (!level.getBlockState(corePos.offset(COLOSSAL_INJECTOR_OFFSET)).is(ModBlocks.WORMHOLE_INJECTOR)) {
			return new ValidationResult(false, WormholeTier.COLOSSAL, COLOSSAL_INJECTOR_OFFSET, "block.galacticpurpose.wormhole_injector");
		}

		// Four 3x3 (9-block) Stabilizer Pylon multiblock arrays at distance 21 on ±X and ±Z (36 pylons total)
		for (int side : new int[]{d, -d}) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int t = -1; t <= 1; t++) {
					BlockPos xOff = new BlockPos(side, dy, t);
					if (!level.getBlockState(corePos.offset(xOff)).is(ModBlocks.WORMHOLE_STABILIZER_PYLON)) {
						return new ValidationResult(false, WormholeTier.COLOSSAL, xOff, "block.galacticpurpose.wormhole_stabilizer_pylon");
					}
					BlockPos zOff = new BlockPos(t, dy, side);
					if (!level.getBlockState(corePos.offset(zOff)).is(ModBlocks.WORMHOLE_STABILIZER_PYLON)) {
						return new ValidationResult(false, WormholeTier.COLOSSAL, zOff, "block.galacticpurpose.wormhole_stabilizer_pylon");
					}
				}
			}
		}

		// Four accelerator monolith spines directly supporting the 3x3 arrays down to the octagonal deck y=-21
		for (int dy = -2; dy >= -drop; dy--) {
			for (BlockPos off : new BlockPos[]{
				new BlockPos(d, dy, 0), new BlockPos(-d, dy, 0),
				new BlockPos(0, dy, d), new BlockPos(0, dy, -d)
			}) {
				if (!level.getBlockState(corePos.offset(off)).is(ModBlocks.LEAD_CONCRETE)) {
					return new ValidationResult(false, WormholeTier.COLOSSAL, off, "block.galacticpurpose.lead_concrete");
				}
			}
		}

		// Four octagonal diagonal anchor pylons on the deck at (±15, -21, ±15)
		for (BlockPos off : new BlockPos[]{
			new BlockPos(15, -drop, 15), new BlockPos(15, -drop, -15),
			new BlockPos(-15, -drop, 15), new BlockPos(-15, -drop, -15)
		}) {
			if (!level.getBlockState(corePos.offset(off)).is(ModBlocks.LEAD_CONCRETE)) {
				return new ValidationResult(false, WormholeTier.COLOSSAL, off, "block.galacticpurpose.lead_concrete");
			}
		}

		BlockState below = level.getBlockState(corePos.below());
		if (!below.isAir() && !below.is(ModBlocks.WORMHOLE_PORTAL)) {
			return new ValidationResult(false, WormholeTier.COLOSSAL, PORTAL_OFFSET, "block.minecraft.air");
		}

		return ValidationResult.ok(WormholeTier.COLOSSAL);
	}

	/**
	 * Checks whether a specific cardinal stabilizer side is intact right now (used by renderer to kill beams on broken sides!).
	 */
	public static List<StructureBlockSpec> getRequiredBlueprint(WormholeTier tier) {
		List<StructureBlockSpec> specs = new ArrayList<>();
		BlockState concrete = ModBlocks.LEAD_CONCRETE.defaultBlockState();
		BlockState injector = ModBlocks.WORMHOLE_INJECTOR.defaultBlockState();
		if (tier == WormholeTier.PERSONAL) {
			specs.add(new StructureBlockSpec(INJECTOR_OFFSET, injector));
			for (BlockPos off : CONCRETE_OFFSETS) {
				specs.add(new StructureBlockSpec(off, concrete));
			}
			specs.add(new StructureBlockSpec(new BlockPos(3, 0, 0),
				ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState().setValue(WormholeStabilizerPylonBlock.FACING, Direction.WEST)));
			specs.add(new StructureBlockSpec(new BlockPos(-3, 0, 0),
				ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState().setValue(WormholeStabilizerPylonBlock.FACING, Direction.EAST)));
			specs.add(new StructureBlockSpec(new BlockPos(0, 0, 3),
				ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState().setValue(WormholeStabilizerPylonBlock.FACING, Direction.NORTH)));
			specs.add(new StructureBlockSpec(new BlockPos(0, 0, -3),
				ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState().setValue(WormholeStabilizerPylonBlock.FACING, Direction.SOUTH)));
			return List.copyOf(specs);
		}

		int d = WormholeTier.COLOSSAL.stabilizerDistance();
		int drop = WormholeTier.COLOSSAL.injectorDrop();
		specs.add(new StructureBlockSpec(COLOSSAL_INJECTOR_OFFSET, injector));

		for (BlockPos off : new BlockPos[]{
			new BlockPos(15, -drop, 15), new BlockPos(15, -drop, -15),
			new BlockPos(-15, -drop, 15), new BlockPos(-15, -drop, -15)
		}) {
			specs.add(new StructureBlockSpec(off, concrete));
		}

		for (int dy = -drop; dy <= -2; dy++) {
			specs.add(new StructureBlockSpec(new BlockPos(d, dy, 0), concrete));
			specs.add(new StructureBlockSpec(new BlockPos(-d, dy, 0), concrete));
			specs.add(new StructureBlockSpec(new BlockPos(0, dy, d), concrete));
			specs.add(new StructureBlockSpec(new BlockPos(0, dy, -d), concrete));
		}

		for (int side : new int[]{1, -1}) {
			int posMain = side * d;
			BlockState xPylon = ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState()
				.setValue(WormholeStabilizerPylonBlock.FACING, side > 0 ? Direction.WEST : Direction.EAST);
			BlockState zPylon = ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState()
				.setValue(WormholeStabilizerPylonBlock.FACING, side > 0 ? Direction.NORTH : Direction.SOUTH);
			for (int dy = -1; dy <= 1; dy++) {
				for (int t = -1; t <= 1; t++) {
					specs.add(new StructureBlockSpec(new BlockPos(posMain, dy, t), xPylon));
					specs.add(new StructureBlockSpec(new BlockPos(t, dy, posMain), zPylon));
				}
			}
		}
		return List.copyOf(specs);
	}

	public static List<BlockPos> getRequiredClearOffsets(WormholeTier tier) {
		if (tier == WormholeTier.PERSONAL) {
			return List.of(PORTAL_OFFSET, LOWER_CLEAR_OFFSET);
		}
		return List.of(PORTAL_OFFSET);
	}

	public static boolean isStabilizerSideIntact(Level level, BlockPos corePos, WormholeTier tier, Direction side) {
		int d = tier.stabilizerDistance();
		if (tier == WormholeTier.PERSONAL) {
			return level.getBlockState(corePos.relative(side, d)).is(ModBlocks.WORMHOLE_STABILIZER_PYLON);
		}
		// Colossal 3x3 array: require all 9 pylons on this wall so breaking even 1 visibly kills that wall's beam!
		boolean alongX = side.getAxis() == Direction.Axis.X;
		int sign = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
		for (int dy = -1; dy <= 1; dy++) {
			for (int t = -1; t <= 1; t++) {
				BlockPos p = alongX ? corePos.offset(sign * d, dy, t) : corePos.offset(t, dy, sign * d);
				if (!level.getBlockState(p).is(ModBlocks.WORMHOLE_STABILIZER_PYLON)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isInjectorIntact(Level level, BlockPos corePos, WormholeTier tier) {
		return level.getBlockState(corePos.below(tier.injectorDrop())).is(ModBlocks.WORMHOLE_INJECTOR);
	}

	public static void buildStructure(Level level, BlockPos corePos) {
		buildStructure(level, corePos, WormholeTier.PERSONAL);
	}

	public static void buildStructure(Level level, BlockPos corePos, WormholeTier tier) {
		if (tier == WormholeTier.COLOSSAL) {
			buildColossalObservatory(level, corePos);
		} else {
			buildPersonalStructure(level, corePos);
		}
	}

	private static void buildPersonalStructure(Level level, BlockPos corePos) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = -2; dy <= 1; dy++) {
					level.setBlock(corePos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
				boolean isRim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
				if (isRim) {
					level.setBlock(corePos.offset(dx, -3, dz), ModBlocks.LEAD_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
				} else if (dx != 0 || dz != 0) {
					level.setBlock(corePos.offset(dx, -3, dz), ModBlocks.LEADED_GLASS.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}

		for (BlockPos offset : CONCRETE_OFFSETS) {
			level.setBlock(corePos.offset(offset), ModBlocks.LEAD_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
		}

		level.setBlock(corePos.offset(3, 0, 0), ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState()
			.setValue(WormholeStabilizerPylonBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
		level.setBlock(corePos.offset(-3, 0, 0), ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState()
			.setValue(WormholeStabilizerPylonBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
		level.setBlock(corePos.offset(0, 0, 3), ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState()
			.setValue(WormholeStabilizerPylonBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
		level.setBlock(corePos.offset(0, 0, -3), ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState()
			.setValue(WormholeStabilizerPylonBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);

		level.setBlock(corePos.offset(INJECTOR_OFFSET), ModBlocks.WORMHOLE_INJECTOR.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(corePos, ModBlocks.WORMHOLE_CORE.defaultBlockState(), Block.UPDATE_ALL);
	}

	/**
	 * Constructs Galactic Purpose's signature 45x41x45 Open-Sky Octagonal Wormhole Observatory:
	 * chamfered octagonal orbital deck with concentric glass conduits, 4 towering curved astrophysical accelerator arms
	 * cradling four 3x3 (9-block) hollow stabilizer emitter matrices, and cantilevered inward magnetic arc spires
	 * leaving the sky wide open above for spaceships!
	 */
	private static void buildColossalObservatory(Level level, BlockPos corePos) {
		int d = WormholeTier.COLOSSAL.stabilizerDistance(); // 21
		int drop = WormholeTier.COLOSSAL.injectorDrop();    // 21
		BlockState concrete = ModBlocks.LEAD_CONCRETE.defaultBlockState();
		BlockState glass = ModBlocks.LEADED_GLASS.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		// 1. Clear open-sky octagonal chamber & lay tiered 45-block chamfered octagonal deck at y = -21
		for (int dx = -d - 1; dx <= d + 1; dx++) {
			for (int dz = -d - 1; dz <= d + 1; dz++) {
				int ax = Math.abs(dx);
				int az = Math.abs(dz);
				// True octagon footprint (max(|x|,|z|) <= 22 AND |x|+|z| <= 31)
				boolean inOctagon = Math.max(ax, az) <= d + 1 && (ax + az) <= 31;
				if (!inOctagon) {
					continue;
				}
				// Clear entire open-sky chamber from deck+1 up to +18
				for (int dy = -drop + 1; dy <= 18; dy++) {
					BlockPos p = corePos.offset(dx, dy, dz);
					if (!level.getBlockState(p).isAir()) {
						level.setBlock(p, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
					}
				}
				int rSq = ax * ax + az * az;
				boolean outerChamfer = Math.max(ax, az) >= d || (ax + az) >= 29;
				boolean radialSpoke = ax <= 1 || az <= 1 || Math.abs(ax - az) <= 1;
				boolean midConduitRing = rSq >= 115 && rSq <= 165;
				boolean centralPad = rSq <= 26;

				if (dx == 0 && dz == 0) {
					level.setBlock(corePos.offset(dx, -drop, dz), ModBlocks.WORMHOLE_INJECTOR.defaultBlockState(), Block.UPDATE_ALL);
				} else if (outerChamfer || centralPad || radialSpoke) {
					level.setBlock(corePos.offset(dx, -drop, dz), concrete, Block.UPDATE_CLIENTS);
					// Raised octagonal perimeter curb at y = -20
					if (outerChamfer && (Math.max(ax, az) == d + 1 || (ax + az) == 31)) {
						level.setBlock(corePos.offset(dx, -drop + 1, dz), concrete, Block.UPDATE_CLIENTS);
					}
				} else if (midConduitRing) {
					level.setBlock(corePos.offset(dx, -drop, dz), glass, Block.UPDATE_CLIENTS);
				} else {
					level.setBlock(corePos.offset(dx, -drop, dz), concrete, Block.UPDATE_CLIENTS);
				}
			}
		}

		// 2. Build 4 Curved Astrophysical Accelerator Towers on ±X and ±Z (tapering + cantilevered magnetic arc spires!)
		for (int side : new int[]{1, -1}) {
			int posMain = side * d;
			Direction xFace = side > 0 ? Direction.WEST : Direction.EAST;
			Direction zFace = side > 0 ? Direction.NORTH : Direction.SOUTH;

			// Base heavy splayed buttress (y = -21..-12) & slender main spire shaft (y = -11..+8)
			for (int dy = -drop; dy <= 8; dy++) {
				int halfW = dy <= -15 ? 3 : 2;
				for (int t = -halfW; t <= halfW; t++) {
					boolean insideEmitter = Math.abs(t) <= 1 && Math.abs(dy) <= 1;
					if (!insideEmitter) {
						level.setBlock(corePos.offset(posMain, dy, t), concrete, Block.UPDATE_CLIENTS);
						level.setBlock(corePos.offset(t, dy, posMain), concrete, Block.UPDATE_CLIENTS);
					}
				}
				// Swept outer dorsal fin behind each monolith tower
				int finDepth = dy <= -12 ? 3 : (dy <= 2 ? 2 : 1);
				for (int step = 1; step <= finDepth; step++) {
					int pOut = side * (d + step);
					level.setBlock(corePos.offset(pOut, dy, 0), concrete, Block.UPDATE_CLIENTS);
					level.setBlock(corePos.offset(0, dy, pOut), concrete, Block.UPDATE_CLIENTS);
					if (dy <= -10 && step == 1) {
						level.setBlock(corePos.offset(pOut, dy, -1), concrete, Block.UPDATE_CLIENTS);
						level.setBlock(corePos.offset(pOut, dy, 1), concrete, Block.UPDATE_CLIENTS);
						level.setBlock(corePos.offset(-1, dy, pOut), concrete, Block.UPDATE_CLIENTS);
						level.setBlock(corePos.offset(1, dy, pOut), concrete, Block.UPDATE_CLIENTS);
					}
				}
			}

			// Dramatic cantilevered inward-curving magnetic arc crown spires (y = +9..+17, curving inward toward the sky!)
			for (int step = 0; step <= 8; step++) {
				int dy = 9 + step;
				int inward = (step * step) / 10; // sweeps gracefully inward by 6 blocks while leaving sky open!
				int pArc = side * (d - inward);
				for (int t = -1; t <= 1; t++) {
					if (step <= 5 || t == 0) {
						level.setBlock(corePos.offset(pArc, dy, t), concrete, Block.UPDATE_CLIENTS);
						level.setBlock(corePos.offset(t, dy, pArc), concrete, Block.UPDATE_CLIENTS);
					}
				}
				// Glowing glass energy strip along inner curve of the arc spire
				if (step >= 2 && step <= 7) {
					level.setBlock(corePos.offset(side * (d - inward - 1), dy, 0), glass, Block.UPDATE_CLIENTS);
					level.setBlock(corePos.offset(0, dy, side * (d - inward - 1)), glass, Block.UPDATE_CLIENTS);
				}
			}

			// 3x3 (9-block) hollow Stabilizer Pylon emitter matrices facing inward at distance ±21
			BlockState xPylon = ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState()
				.setValue(WormholeStabilizerPylonBlock.FACING, xFace);
			BlockState zPylon = ModBlocks.WORMHOLE_STABILIZER_PYLON.defaultBlockState()
				.setValue(WormholeStabilizerPylonBlock.FACING, zFace);
			for (int dy = -1; dy <= 1; dy++) {
				for (int t = -1; t <= 1; t++) {
					level.setBlock(corePos.offset(posMain, dy, t), xPylon, Block.UPDATE_ALL);
					level.setBlock(corePos.offset(t, dy, posMain), zPylon, Block.UPDATE_ALL);
				}
			}
		}

		// 3. Four Octagonal Diagonal Sensor Obelisks on the outer chamfered corners (shorter architectural accents!)
		for (int sx : new int[]{15, -15}) {
			for (int sz : new int[]{15, -15}) {
				for (int dy = -drop; dy <= -12; dy++) {
					level.setBlock(corePos.offset(sx, dy, sz), concrete, Block.UPDATE_CLIENTS);
				}
				level.setBlock(corePos.offset(sx, -11, sz), glass, Block.UPDATE_CLIENTS);
			}
		}

		// 4. Place suspended central Wormhole Core
		level.setBlock(corePos, ModBlocks.WORMHOLE_CORE.defaultBlockState(), Block.UPDATE_ALL);
	}

	/**
	 * Constructs a grand 13-wide x 28-long UNOBSTRUCTED Diagonal Magnetic Launch Runway approaching through
	 * the wide-open 45-degree corridor between two cardinal Stabilizer Towers (zero tower walls blocking launch!),
	 * keeping the deck anchor block intact so validateColossal() stays 100% valid!
	 * Returns the BlockPos of the Ship Assembly Pad at the far launch end of the runway.
	 */
	public static BlockPos buildColossalLaunchRunway(Level level, BlockPos corePos, Direction approachDir) {
		// Pick the open 45-degree diagonal behind approachDir so the ship launches straight between 2 towers!
		Direction backDir = approachDir.getOpposite();
		Direction rightDir = approachDir.getClockWise();
		int sx = (backDir.getStepX() + rightDir.getStepX()) >= 0 ? 1 : -1;
		int sz = (backDir.getStepZ() + rightDir.getStepZ()) >= 0 ? 1 : -1;

		int deckY = corePos.getY() - WormholeTier.COLOSSAL.injectorDrop();
		BlockState hull = ModBlocks.STARSHIP_HULL_PLATING.defaultBlockState();
		BlockState ribbed = ModBlocks.STARSHIP_RIBBED_PANEL.defaultBlockState();
		BlockState grate = ModBlocks.STARSHIP_GRATE.defaultBlockState();
		BlockState light = ModBlocks.STARSHIP_LIGHT_STRIP.defaultBlockState();
		BlockState concrete = ModBlocks.LEAD_CONCRETE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		// 1. Clear the one low corner decorative obelisk ABOVE deckY on this launch diagonal (keeping y=deckY anchor valid!)
		for (int ox = -2; ox <= 2; ox++) {
			for (int oz = -2; oz <= 2; oz++) {
				for (int dy = 1; dy <= 14; dy++) {
					BlockPos p = corePos.offset(sx * 15 + ox, -WormholeTier.COLOSSAL.injectorDrop() + dy, sz * 15 + oz);
					if (!level.getBlockState(p).isAir()) {
						level.setBlock(p, air, Block.UPDATE_CLIENTS);
					}
				}
			}
		}

		// Unit diagonal basis: forward (toward core) = (-sx, -sz)/sqrt(2), back = (sx, sz)/sqrt(2), lateral = (-sz, sx)/sqrt(2)
		double invSqrt2 = 0.70710678;
		double bx = sx * invSqrt2;
		double bz = sz * invSqrt2;
		double lx = -sz * invSqrt2;
		double lz = sx * invSqrt2;

		// 2. Lay wide 13-block-wide x 28-block-long illuminated diagonal launch deck (dist 17 -> 45 from core!)
		for (int dist = 17; dist <= 45; dist++) {
			for (int lat = -6; lat <= 6; lat++) {
				int wx = corePos.getX() + (int) Math.round(bx * dist + lx * lat);
				int wz = corePos.getZ() + (int) Math.round(bz * dist + lz * lat);
				BlockPos deckPos = new BlockPos(wx, deckY, wz);

				// Keep the validated diagonal anchor block at (sx*15, deckY, sz*15) untouched as Lead Concrete
				boolean isAnchor = (wx == corePos.getX() + sx * 15 && wz == corePos.getZ() + sz * 15);
				if (!isAnchor) {
					BlockState surface;
					if (Math.abs(lat) == 6) {
						surface = (dist % 4 == 0) ? light : ribbed;
					} else if (Math.abs(lat) >= 4) {
						surface = hull;
					} else if (lat == 0 && (dist % 3 == 0)) {
						surface = light;
					} else {
						surface = grate;
					}
					level.setBlock(deckPos, surface, Block.UPDATE_CLIENTS);
				}

				// Heavy structural pillars beneath outer rails down to ground
				if (Math.abs(lat) == 6 && dist % 5 == 0) {
					for (int py = 1; py <= 10; py++) {
						BlockPos below = deckPos.below(py);
						if (level.getBlockState(below).isAir()) {
							level.setBlock(below, concrete, Block.UPDATE_CLIENTS);
						} else {
							break;
						}
					}
				}

				// Clear tall 12-block unobstructed flight corridor above the entire diagonal runway!
				for (int dy = 1; dy <= 12; dy++) {
					BlockPos clearPos = deckPos.above(dy);
					if (!level.getBlockState(clearPos).isAir()) {
						level.setBlock(clearPos, air, Block.UPDATE_CLIENTS);
					}
				}
			}
		}

		// 3. Place Ship Assembly Pad centered cleanly at dist = 36 aiming dead-center through the open diagonal at the Wormhole!
		int padX = corePos.getX() + (int) Math.round(bx * 36.0);
		int padZ = corePos.getZ() + (int) Math.round(bz * 36.0);
		BlockPos padPos = new BlockPos(padX, deckY, padZ);
		level.setBlock(padPos, ModBlocks.SHIP_ASSEMBLY_PAD.defaultBlockState(), Block.UPDATE_ALL);
		return padPos;
	}
}

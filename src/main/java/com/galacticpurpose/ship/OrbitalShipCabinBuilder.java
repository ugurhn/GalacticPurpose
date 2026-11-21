package com.galacticpurpose.ship;

import com.galacticpurpose.block.StarshipCargoLockerBlockEntity;
import com.galacticpurpose.entity.StarshipSeatEntity;
import com.galacticpurpose.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Builds the sleek walkable 11x7x21 Panoramic-Glass Interstellar Starship Deck inside
 * galacticpurpose:orbital_transit at (slot * 256, 120, 0) so the pilot can walk freely from
 * the front bridge windshield (planet / wormhole ahead) to the skylight & side windows (1.7x Erebus Black Hole)
 * to the rear observation glass (exit wormhole / departing planet) and access live synced Cargo!
 */
public final class OrbitalShipCabinBuilder {
	public static final int CABIN_Y = 120;
	public static final int SLOT_STRIDE = 256;

	private OrbitalShipCabinBuilder() {
	}

	public static BlockPos cabinOrigin(int slotIndex) {
		return new BlockPos(slotIndex * SLOT_STRIDE, CABIN_Y, 0);
	}

	public static BlockPos cargoLockerPos(int slotIndex) {
		return cabinOrigin(slotIndex).offset(-3, 1, 2);
	}

	public static BlockPos helmPos(int slotIndex) {
		return cabinOrigin(slotIndex).offset(0, 1, 7);
	}

	public static BlockPos playerSpawnPos(int slotIndex) {
		return cabinOrigin(slotIndex).offset(0, 1, 3);
	}

	public static void buildCabin(
		ServerLevel transitLevel,
		int slotIndex,
		CompoundTag cargoTag,
		HolderLookup.Provider registries
	) {
		BlockPos origin = cabinOrigin(slotIndex);
		transitLevel.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
		transitLevel.getChunk((origin.getX() + 16) >> 4, origin.getZ() >> 4);
		transitLevel.getChunk((origin.getX() - 16) >> 4, origin.getZ() >> 4);

		BlockState hull = ModBlocks.STARSHIP_HULL_PLATING.defaultBlockState();
		BlockState ribbed = ModBlocks.STARSHIP_RIBBED_PANEL.defaultBlockState();
		BlockState grate = ModBlocks.STARSHIP_GRATE.defaultBlockState();
		BlockState glass = ModBlocks.REINFORCED_VIEWPORT_GLASS.defaultBlockState();
		BlockState light = ModBlocks.STARSHIP_LIGHT_STRIP.defaultBlockState();
		BlockState thruster = ModBlocks.STARSHIP_THRUSTER_BLOCK.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		// Clear bounding volume & assemble tapered sleek hull (width narrows cleanly toward nose Z=+10)
		for (int dz = -10; dz <= 10; dz++) {
			int halfW = dz >= 8 ? 3 : (dz >= 6 ? 4 : 5);
			int roofH = dz >= 8 ? 5 : 6;
			for (int dx = -6; dx <= 6; dx++) {
				for (int dy = -1; dy <= 7; dy++) {
					BlockPos p = origin.offset(dx, dy, dz);
					if (Math.abs(dx) > halfW || dy < 0 || dy > roofH) {
						transitLevel.setBlock(p, air, Block.UPDATE_CLIENTS);
						continue;
					}

					boolean isFloor = (dy == 0);
					boolean isRoof = (dy == roofH);
					boolean isSideWall = (Math.abs(dx) == halfW);
					boolean isFrontNose = (dz == 10);
					boolean isRearStern = (dz == -10);

					if (isFloor) {
						boolean centerWalk = Math.abs(dx) <= 1 && Math.abs(dz) <= 8;
						boolean strip = Math.abs(dx) == 2 && (dz % 3 == 0);
						transitLevel.setBlock(p, strip ? light : (centerWalk ? grate : hull), Block.UPDATE_CLIENTS);
					} else if (isRoof) {
						// Panoramic central glass skylight spine looking directly up at the giant Erebus Black Hole!
						boolean skylight = Math.abs(dx) <= halfW - 2 && dz >= -8 && dz <= 8;
						transitLevel.setBlock(p, skylight ? glass : hull, Block.UPDATE_CLIENTS);
					} else if (isFrontNose) {
						// Sweeping floor-to-ceiling panoramic bridge windshield facing +Z!
						boolean frame = (dy == 1 && Math.abs(dx) == halfW) || (Math.abs(dx) == halfW && dy >= roofH - 1);
						transitLevel.setBlock(p, frame ? ribbed : glass, Block.UPDATE_CLIENTS);
					} else if (isRearStern) {
						// Wide sweeping rear observation viewport looking cleanly at receding Exit Wormhole / Planet!
						boolean window = Math.abs(dx) <= 3 && dy >= 1 && dy <= 5;
						boolean nacelle = Math.abs(dx) >= 4 && dy >= 1 && dy <= 2;
						transitLevel.setBlock(p, window ? glass : (nacelle ? thruster : ribbed), Block.UPDATE_CLIENTS);
					} else if (isSideWall) {
						// Continuous wide side observation windows with sleek structural ribs every 4 blocks
						boolean rib = (dz % 4 == 0) || dy == 1 || dy == roofH - 1;
						transitLevel.setBlock(p, rib ? ribbed : glass, Block.UPDATE_CLIENTS);
					} else {
						transitLevel.setBlock(p, air, Block.UPDATE_CLIENTS);
					}
				}
			}
		}

		// Sleek exterior swept delta wing stubs visible in F5 third-person!
		for (int side : new int[]{-1, 1}) {
			for (int dz = -9; dz <= -2; dz++) {
				int wingSpan = 1 + Math.max(0, (-2 - dz) / 2);
				for (int w = 1; w <= wingSpan; w++) {
					transitLevel.setBlock(origin.offset(side * (5 + w), 1, dz), hull, Block.UPDATE_CLIENTS);
				}
			}
		}

		// Bridge interior stations: Front Autopilot Helm, Synced 27-Slot Cargo Locker, Crafting Table & Lighting
		BlockPos helm = helmPos(slotIndex);
		transitLevel.setBlock(helm, ModBlocks.STARSHIP_HELM.defaultBlockState(), Block.UPDATE_ALL);
		transitLevel.setBlock(helm.offset(-1, 0, 0), ModBlocks.SOLAR_TELEMETRY_MONITOR.defaultBlockState(), Block.UPDATE_ALL);
		transitLevel.setBlock(helm.offset(1, 0, 0), ModBlocks.STARSHIP_LIGHT_STRIP.defaultBlockState(), Block.UPDATE_ALL);

		BlockPos lockerPos = cargoLockerPos(slotIndex);
		transitLevel.setBlock(lockerPos, ModBlocks.STARSHIP_CARGO_LOCKER.defaultBlockState(), Block.UPDATE_ALL);
		BlockEntity be = transitLevel.getBlockEntity(lockerPos);
		if (be instanceof StarshipCargoLockerBlockEntity locker) {
			locker.importCargoTag(cargoTag, registries);
		}

		// Right-side workbench & fuel reserve decor
		transitLevel.setBlock(origin.offset(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
		transitLevel.setBlock(origin.offset(3, 1, 0), Blocks.ANVIL.defaultBlockState(), Block.UPDATE_ALL);
		transitLevel.setBlock(origin.offset(-3, 1, 0), ModBlocks.BIO_DISTILLER.defaultBlockState(), Block.UPDATE_ALL);

		// Sittable Captain's Command Chair directly behind Bridge Helm (prevents Zero-G float while commanding!)
		AABB clearBox = new AABB(origin).inflate(14.0, 10.0, 14.0);
		for (StarshipSeatEntity oldSeat : transitLevel.getEntitiesOfClass(StarshipSeatEntity.class, clearBox)) {
			oldSeat.discard();
		}
		StarshipSeatEntity.spawnAt(transitLevel, origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 5.8, 0.0f);
	}
}

package com.galacticpurpose.block;

import com.galacticpurpose.climate.OverworldHazardTicker;
import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Generates clean solar energy (160-240 E/t under open daytime sky) and auto-pushes to all adjacent
 * EnergyStorage.SIDED machines & cables (universal Fabric Energy / RF ecosystem compatibility!).
 */
public class SolarFluxGeneratorBlockEntity extends BlockEntity {
	public static final long CAPACITY = 100_000L;
	public static final long MAX_EXTRACT = 4_000L;
	public static final long BASE_GENERATION = 160L;

	private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, 0L, MAX_EXTRACT) {
		@Override
		protected void onFinalCommit() {
			SolarFluxGeneratorBlockEntity.this.setChanged();
		}
	};

	private long lastOutputPerTick = 0L;

	public SolarFluxGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SOLAR_FLUX_GENERATOR, pos, state);
	}

	public EnergyStorage energyStorage() {
		return this.energyStorage;
	}

	public long lastOutputPerTick() {
		return this.lastOutputPerTick;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, SolarFluxGeneratorBlockEntity gen) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		boolean skyOpen = OverworldHazardTicker.isExposedToSky(serverLevel, pos.above());
		boolean daylight = serverLevel.isDay() && !serverLevel.isRaining();
		long generated = 0L;
		if (skyOpen && (daylight || SpaceCompatManager.get().isMars(serverLevel))) {
			// High solar intensity on Mars / collapsing sun boosts generation cleanly
			generated = SpaceCompatManager.get().isMars(serverLevel) ? 240L : BASE_GENERATION;
			if (gen.energyStorage.amount < CAPACITY) {
				gen.energyStorage.amount = Math.min(CAPACITY, gen.energyStorage.amount + generated);
				gen.setChanged();
			}
		}
		gen.lastOutputPerTick = generated;

		// Push stored energy out to any adjacent machine/cable implementing EnergyStorage.SIDED!
		if (gen.energyStorage.amount > 0L) {
			for (Direction dir : Direction.values()) {
				EnergyStorage neighbor = EnergyStorage.SIDED.find(serverLevel, pos.relative(dir), dir.getOpposite());
				if (neighbor != null && neighbor.supportsInsertion()) {
					EnergyStorageUtil.move(gen.energyStorage, neighbor, MAX_EXTRACT, null);
					if (gen.energyStorage.amount <= 0L) {
						break;
					}
				}
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putLong("Energy", this.energyStorage.amount);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.energyStorage.amount = Math.max(0L, Math.min(CAPACITY, tag.getLong("Energy")));
	}
}

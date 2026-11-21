package com.galacticpurpose.block;

import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.wormhole.WormholeTier;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;

/**
 * Energy proxy & remote terminal forwarding cable power and GUI interaction to the Wormhole Core
 * suspended either 3 blocks (Personal tier) or 13 blocks (Colossal Ship-Scale tier) above.
 */
public class WormholeEnergyInjectorBlockEntity extends BlockEntity {
	private final EnergyStorage coreProxy = new EnergyStorage() {
		private EnergyStorage findCoreStorage() {
			WormholeCoreBlockEntity core = findConnectedCore();
			return core != null ? core.energyStorage() : null;
		}

		@Override
		public boolean supportsInsertion() {
			EnergyStorage target = findCoreStorage();
			return target != null && target.supportsInsertion();
		}

		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			EnergyStorage target = findCoreStorage();
			return target != null ? target.insert(maxAmount, transaction) : 0L;
		}

		@Override
		public boolean supportsExtraction() {
			return false;
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			return 0L;
		}

		@Override
		public long getAmount() {
			EnergyStorage target = findCoreStorage();
			return target != null ? target.getAmount() : 0L;
		}

		@Override
		public long getCapacity() {
			EnergyStorage target = findCoreStorage();
			return target != null ? target.getCapacity() : 0L;
		}
	};

	public WormholeEnergyInjectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WORMHOLE_INJECTOR, pos, state);
	}

	public WormholeCoreBlockEntity findConnectedCore() {
		if (this.level == null) {
			return null;
		}
		if (this.level.getBlockEntity(this.worldPosition.above(WormholeTier.PERSONAL.injectorDrop())) instanceof WormholeCoreBlockEntity personal) {
			return personal;
		}
		if (this.level.getBlockEntity(this.worldPosition.above(WormholeTier.COLOSSAL.injectorDrop())) instanceof WormholeCoreBlockEntity colossal) {
			return colossal;
		}
		return null;
	}

	public EnergyStorage coreProxy() {
		return this.coreProxy;
	}
}

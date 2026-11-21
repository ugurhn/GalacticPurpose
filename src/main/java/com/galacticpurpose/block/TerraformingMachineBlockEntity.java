package com.galacticpurpose.block;

import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.menu.TerraformingMachineMenu;
import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.terraforming.MarsProgressSnapshot;
import com.galacticpurpose.terraforming.MarsTerraformingState;
import com.galacticpurpose.terraforming.TerraformingMachineType;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Shared block entity powering Mars terraforming machines using TeamReborn Energy.
 */
public class TerraformingMachineBlockEntity extends BlockEntity
	implements Container, ExtendedScreenHandlerFactory<BlockPos> {

	public enum MachineStatus {
		ACTIVE("gui.galacticpurpose.status.active", 0x55FF55),
		NOT_ON_MARS("gui.galacticpurpose.status.not_on_mars", 0xFFAA00),
		NO_ENERGY("gui.galacticpurpose.status.no_energy", 0xFF5555),
		MISSING_PREREQUISITE("gui.galacticpurpose.status.missing_prereq", 0xFF8833);

		private final String translationKey;
		private final int colorRgb;

		MachineStatus(String translationKey, int colorRgb) {
			this.translationKey = translationKey;
			this.colorRgb = colorRgb;
		}

		public String translationKey() {
			return this.translationKey;
		}

		public int colorRgb() {
			return this.colorRgb;
		}

		public static MachineStatus byOrdinal(int ordinal) {
			MachineStatus[] values = values();
			if (ordinal < 0 || ordinal >= values.length) {
				return NOT_ON_MARS;
			}
			return values[ordinal];
		}
	}

	private static final String TAG_ENERGY = "Energy";
	private static final String TAG_MACHINE_TYPE = "MachineType";

	private TerraformingMachineType machineType;
	private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private final SimpleEnergyStorage energyStorage;
	private MachineStatus status = MachineStatus.NOT_ON_MARS;

	private final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(int index) {
			MarsProgressSnapshot snap = getServerMarsSnapshot();
			return switch (index) {
				case 0 -> (int) Math.min(30_000, energyStorage.amount / 10L);
				case 1 -> (int) Math.min(30_000, energyStorage.capacity / 10L);
				case 2 -> status.ordinal();
				case 3 -> Math.round(snap.heatPercent() * 10.0f);
				case 4 -> Math.round(snap.pressurePercent() * 10.0f);
				case 5 -> Math.round(snap.oxygenPercent() * 10.0f);
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			// Server-authoritative ContainerData synced to client Menu.
		}

		@Override
		public int getCount() {
			return 6;
		}
	};

	public TerraformingMachineBlockEntity(TerraformingMachineType machineType, BlockPos pos, BlockState state) {
		super(ModBlockEntities.forMachine(machineType), pos, state);
		this.machineType = machineType;
		this.energyStorage = new SimpleEnergyStorage(
			machineType.energyCapacity(),
			machineType.maxInputPerTick(),
			0L
		) {
			@Override
			protected void onFinalCommit() {
				TerraformingMachineBlockEntity.this.setChanged();
			}
		};
	}

	public TerraformingMachineType machineType() {
		return this.machineType;
	}

	public EnergyStorage energyStorage() {
		return this.energyStorage;
	}

	private MarsProgressSnapshot getServerMarsSnapshot() {
		if (this.level instanceof ServerLevel serverLevel && SpaceCompatManager.get().isMars(serverLevel)) {
			return MarsTerraformingState.get(serverLevel).snapshot();
		}
		return MarsProgressSnapshot.empty();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TerraformingMachineBlockEntity machine) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		machine.pullBatterySlotEnergy();

		boolean lit = false;
		if (!SpaceCompatManager.get().isMars(serverLevel)) {
			machine.status = MachineStatus.NOT_ON_MARS;
		} else {
			MarsTerraformingState marsState = MarsTerraformingState.get(serverLevel);
			MarsProgressSnapshot snap = marsState.snapshot();

			if (!machine.machineType.canOperate(snap)) {
				machine.status = MachineStatus.MISSING_PREREQUISITE;
			} else if (machine.energyStorage.amount < machine.machineType.energyPerTick()) {
				machine.status = MachineStatus.NO_ENERGY;
			} else {
				machine.energyStorage.amount -= machine.machineType.energyPerTick();
				machine.status = MachineStatus.ACTIVE;
				lit = true;

				marsState.addProgress(
					machine.machineType.heatPerSecond() / 20.0,
					machine.machineType.pressurePerSecond() / 20.0,
					machine.machineType.oxygenPerSecond() / 20.0
				);
				marsState.syncDimensionIfThrottled(serverLevel);
				machine.setChanged();
			}
		}

		if (state.getValue(TerraformingMachineBlock.LIT) != lit) {
			level.setBlock(pos, state.setValue(TerraformingMachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
		}
	}

	private void pullBatterySlotEnergy() {
		ItemStack stack = this.items.get(0);
		if (stack.isEmpty() || this.energyStorage.amount >= this.energyStorage.capacity) {
			return;
		}

		ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(
			InventoryStorage.of(this, null).getSlot(0)
		);
		EnergyStorage itemEnergy = EnergyStorage.ITEM.find(stack, ctx);
		if (itemEnergy != null && itemEnergy.supportsExtraction()) {
			long moved = EnergyStorageUtil.move(
				itemEnergy,
				this.energyStorage,
				this.machineType.maxInputPerTick(),
				null
			);
			if (moved > 0) {
				this.setChanged();
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putLong(TAG_ENERGY, this.energyStorage.amount);
		tag.putString(TAG_MACHINE_TYPE, this.machineType.id());
		ContainerHelper.saveAllItems(tag, this.items, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains(TAG_MACHINE_TYPE)) {
			this.machineType = TerraformingMachineType.byId(tag.getString(TAG_MACHINE_TYPE));
		}
		this.energyStorage.amount = Math.max(0L, Math.min(this.energyStorage.capacity, tag.getLong(TAG_ENERGY)));
		ContainerHelper.loadAllItems(tag, this.items, registries);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return this.worldPosition;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.galacticpurpose." + this.machineType.id());
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new TerraformingMachineMenu(
			syncId,
			playerInventory,
			this,
			this.dataAccess,
			this.machineType,
			ContainerLevelAccess.create(this.level, this.worldPosition)
		);
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
	}

	@Override
	public boolean isEmpty() {
		return this.items.get(0).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 0 ? this.items.get(0) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
		if (!removed.isEmpty()) {
			this.setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(this.items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot != 0) {
			return;
		}
		this.items.set(0, stack);
		if (stack.getCount() > this.getMaxStackSize()) {
			stack.setCount(this.getMaxStackSize());
		}
		this.setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		this.items.clear();
		this.setChanged();
	}
}

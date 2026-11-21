package com.galacticpurpose.block;

import com.galacticpurpose.menu.BioDistillerMenu;
import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.registry.ModItems;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 * Block entity distilling 2x Mars Bio-Essence into 1x Exotic Bio-Fuel using TeamReborn Energy.
 */
public class BioDistillerBlockEntity extends BlockEntity
	implements Container, ExtendedScreenHandlerFactory<BlockPos> {

	public static final long ENERGY_CAPACITY = 20_000L;
	public static final long ENERGY_PER_TICK = 40L;
	public static final int PROCESS_TICKS = 100;
	public static final int ESSENCE_PER_CRAFT = 2;

	private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
	private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500L, 0L) {
		@Override
		protected void onFinalCommit() {
			BioDistillerBlockEntity.this.setChanged();
		}
	};
	private int progress = 0;

	private final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> (int) Math.min(30_000, energyStorage.amount / 10L);
				case 1 -> (int) Math.min(30_000, energyStorage.capacity / 10L);
				case 2 -> progress;
				case 3 -> PROCESS_TICKS;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return 4;
		}
	};

	public BioDistillerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BIO_DISTILLER, pos, state);
	}

	public EnergyStorage energyStorage() {
		return this.energyStorage;
	}

	public int progress() {
		return this.progress;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BioDistillerBlockEntity be) {
		be.pullBatterySlotEnergy();

		boolean active = false;
		if (be.canProcess()) {
			if (be.energyStorage.amount >= ENERGY_PER_TICK) {
				be.energyStorage.amount -= ENERGY_PER_TICK;
				be.progress++;
				active = true;
				if (be.progress >= PROCESS_TICKS) {
					be.finishDistillation();
					be.progress = 0;
				}
				be.setChanged();
			}
		} else if (be.progress > 0) {
			be.progress = 0;
			be.setChanged();
		}

		if (state.getValue(BioDistillerBlock.LIT) != active) {
			level.setBlock(pos, state.setValue(BioDistillerBlock.LIT, active), Block.UPDATE_CLIENTS);
		}
	}

	private void pullBatterySlotEnergy() {
		ItemStack battery = this.items.get(0);
		if (battery.isEmpty() || this.energyStorage.amount >= this.energyStorage.capacity) {
			return;
		}
		ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(this, null).getSlot(0));
		EnergyStorage itemEnergy = EnergyStorage.ITEM.find(battery, ctx);
		if (itemEnergy != null && itemEnergy.supportsExtraction()) {
			long moved = EnergyStorageUtil.move(itemEnergy, this.energyStorage, 500L, null);
			if (moved > 0) {
				this.setChanged();
			}
		}
	}

	private boolean canProcess() {
		ItemStack input = this.items.get(1);
		ItemStack output = this.items.get(2);
		if (!input.is(ModItems.MARS_BIO_ESSENCE) || input.getCount() < ESSENCE_PER_CRAFT) {
			return false;
		}
		if (output.isEmpty()) {
			return true;
		}
		return output.is(ModItems.EXOTIC_BIO_FUEL) && output.getCount() < output.getMaxStackSize();
	}

	private void finishDistillation() {
		this.items.get(1).shrink(ESSENCE_PER_CRAFT);
		ItemStack output = this.items.get(2);
		if (output.isEmpty()) {
			this.items.set(2, new ItemStack(ModItems.EXOTIC_BIO_FUEL));
		} else {
			output.grow(1);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putLong("Energy", this.energyStorage.amount);
		tag.putInt("Progress", this.progress);
		ContainerHelper.saveAllItems(tag, this.items, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.energyStorage.amount = Math.max(0L, Math.min(ENERGY_CAPACITY, tag.getLong("Energy")));
		this.progress = Math.max(0, tag.getInt("Progress"));
		ContainerHelper.loadAllItems(tag, this.items, registries);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return this.worldPosition;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.galacticpurpose.bio_distiller");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new BioDistillerMenu(
			syncId,
			playerInventory,
			this,
			this.dataAccess,
			ContainerLevelAccess.create(this.level, this.worldPosition)
		);
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : this.items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.items.get(slot);
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
		this.items.set(slot, stack);
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

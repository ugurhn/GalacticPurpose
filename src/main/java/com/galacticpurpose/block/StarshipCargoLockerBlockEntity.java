package com.galacticpurpose.block;

import com.galacticpurpose.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 27-slot pressurized Starship Cargo Locker used inside the walkable orbital_transit deck
 * and synced seamlessly with the InterstellarShipEntity's internal cargo hold.
 */
public class StarshipCargoLockerBlockEntity extends RandomizableContainerBlockEntity {
	private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

	public StarshipCargoLockerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STARSHIP_CARGO_LOCKER, pos, state);
	}

	@Override
	public int getContainerSize() {
		return 27;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.galacticpurpose.starship_cargo_locker");
	}

	@Override
	protected AbstractContainerMenu createMenu(int syncId, Inventory playerInv) {
		return ChestMenu.threeRows(syncId, playerInv, this);
	}

	public CompoundTag exportCargoTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		ContainerHelper.saveAllItems(tag, this.items, registries);
		return tag;
	}

	public void importCargoTag(CompoundTag tag, HolderLookup.Provider registries) {
		this.items = NonNullList.withSize(27, ItemStack.EMPTY);
		if (tag != null && !tag.isEmpty()) {
			ContainerHelper.loadAllItems(tag, this.items, registries);
		}
		this.setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, this.items, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.items = NonNullList.withSize(27, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, this.items, registries);
	}
}

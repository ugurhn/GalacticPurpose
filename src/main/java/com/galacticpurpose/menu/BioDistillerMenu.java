package com.galacticpurpose.menu;

import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.registry.ModMenus;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

/**
 * Container menu for the Exotic Bio-Fuel Distiller.
 */
public class BioDistillerMenu extends AbstractContainerMenu {
	private final Container container;
	private final ContainerData data;
	private final ContainerLevelAccess access;

	public BioDistillerMenu(int syncId, Inventory playerInventory, BlockPos pos) {
		this(
			syncId,
			playerInventory,
			new SimpleContainer(3),
			new SimpleContainerData(4),
			ContainerLevelAccess.create(playerInventory.player.level(), pos)
		);
	}

	public BioDistillerMenu(
		int syncId,
		Inventory playerInventory,
		Container container,
		ContainerData data,
		ContainerLevelAccess access
	) {
		super(ModMenus.BIO_DISTILLER, syncId);
		checkContainerSize(container, 3);
		checkContainerDataCount(data, 4);
		this.container = container;
		this.data = data;
		this.access = access;
		container.startOpen(playerInventory.player);

		// Slot 0: Battery
		this.addSlot(new Slot(container, 0, 12, 46) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
			}
		});
		// Slot 1: Mars Bio-Essence input
		this.addSlot(new Slot(container, 1, 56, 46) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModItems.MARS_BIO_ESSENCE);
			}
		});
		// Slot 2: Exotic Bio-Fuel output
		this.addSlot(new Slot(container, 2, 132, 46) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
		}
		this.addDataSlots(data);
	}

	public int energyStored() {
		return this.data.get(0) * 10;
	}

	public int energyCapacity() {
		return Math.max(10, this.data.get(1) * 10);
	}

	public int progress() {
		return this.data.get(2);
	}

	public int maxProgress() {
		return Math.max(1, this.data.get(3));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index < 3) {
			if (!this.moveItemStackTo(stack, 3, 39, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null) {
				if (!this.moveItemStackTo(stack, 0, 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (stack.is(ModItems.MARS_BIO_ESSENCE)) {
				if (!this.moveItemStackTo(stack, 1, 2, false)) {
					return ItemStack.EMPTY;
				}
			} else if (index < 30) {
				if (!this.moveItemStackTo(stack, 30, 39, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 3, 30, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, ModBlocks.BIO_DISTILLER);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.container.stopOpen(player);
	}
}

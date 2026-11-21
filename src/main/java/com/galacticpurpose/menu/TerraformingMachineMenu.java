package com.galacticpurpose.menu;

import com.galacticpurpose.block.TerraformingMachineBlockEntity;
import com.galacticpurpose.block.TerraformingMachineBlockEntity.MachineStatus;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModMenus;
import com.galacticpurpose.terraforming.TerraformingMachineType;
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
 * Container menu shared by all four Mars terraforming machines.
 */
public class TerraformingMachineMenu extends AbstractContainerMenu {
	private final Container machineInventory;
	private final ContainerData data;
	private final TerraformingMachineType machineType;
	private final ContainerLevelAccess access;

	public TerraformingMachineMenu(int syncId, Inventory playerInventory, BlockPos pos) {
		this(
			syncId,
			playerInventory,
			new SimpleContainer(1),
			new SimpleContainerData(6),
			resolveMachineType(playerInventory, pos),
			ContainerLevelAccess.create(playerInventory.player.level(), pos)
		);
	}

	public TerraformingMachineMenu(
		int syncId,
		Inventory playerInventory,
		Container machineInventory,
		ContainerData data,
		TerraformingMachineType machineType,
		ContainerLevelAccess access
	) {
		super(ModMenus.TERRAFORMING_MACHINE, syncId);
		checkContainerSize(machineInventory, 1);
		checkContainerDataCount(data, 6);
		this.machineInventory = machineInventory;
		this.data = data;
		this.machineType = machineType;
		this.access = access;

		machineInventory.startOpen(playerInventory.player);

		// Battery slot
		this.addSlot(new Slot(machineInventory, 0, 12, 46) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
			}
		});

		// Player inventory (3 rows)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
			}
		}

		// Hotbar
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
		}

		this.addDataSlots(data);
	}

	private static TerraformingMachineType resolveMachineType(Inventory playerInventory, BlockPos pos) {
		if (playerInventory.player.level().getBlockEntity(pos) instanceof TerraformingMachineBlockEntity be) {
			return be.machineType();
		}
		return TerraformingMachineType.THERMAL_HEATER;
	}

	public TerraformingMachineType machineType() {
		return this.machineType;
	}

	public int energyStored() {
		return this.data.get(0) * 10;
	}

	public int energyCapacity() {
		return Math.max(10, this.data.get(1) * 10);
	}

	public MachineStatus status() {
		return MachineStatus.byOrdinal(this.data.get(2));
	}

	public float heatPercent() {
		return this.data.get(3) / 10.0f;
	}

	public float pressurePercent() {
		return this.data.get(4) / 10.0f;
	}

	public float oxygenPercent() {
		return this.data.get(5) / 10.0f;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index == 0) {
			if (!this.moveItemStackTo(stack, 1, 37, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null) {
				if (!this.moveItemStackTo(stack, 0, 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (index < 28) {
				if (!this.moveItemStackTo(stack, 28, 37, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 1, 28, false)) {
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
		return stillValid(this.access, player, ModBlocks.forMachine(this.machineType));
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.machineInventory.stopOpen(player);
	}
}

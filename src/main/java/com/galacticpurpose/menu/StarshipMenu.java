package com.galacticpurpose.menu;

import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 27-slot Cargo Bay + Fuel Cell & Energy Cell intake + Orbital Launch/Free-Flight screen handler for InterstellarShipEntity.
 */
public class StarshipMenu extends AbstractContainerMenu {
	private final Container shipInv;
	private final InterstellarShipEntity ship;
	private final ContainerData data;

	public StarshipMenu(int syncId, Inventory playerInv, int shipEntityId) {
		this(syncId, playerInv, resolveShip(playerInv.player, shipEntityId));
	}

	private static InterstellarShipEntity resolveShip(Player player, int id) {
		Entity e = player.level().getEntity(id);
		return e instanceof InterstellarShipEntity s ? s : null;
	}

	public StarshipMenu(int syncId, Inventory playerInv, InterstellarShipEntity ship) {
		super(ModMenus.STARSHIP, syncId);
		this.ship = ship;
		this.shipInv = ship != null ? ship.inventory() : new SimpleContainer(29);
		this.data = ship != null ? new ContainerData() {
			@Override
			public int get(int idx) {
				return switch (idx) {
					case 0 -> ship.fuelUnits();
					case 1 -> ship.energyKilo();
					case 2 -> ship.syncedLinkStatus().ordinal();
					case 3 -> ship.shipMode();
					default -> 0;
				};
			}

			@Override
			public void set(int idx, int val) {
			}

			@Override
			public int getCount() {
				return 4;
			}
		} : new SimpleContainerData(4);
		this.addDataSlots(this.data);

		// 27 Ship Cargo slots (3 rows x 9) at y=46
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(this.shipInv, row * 9 + col, 8 + col * 18, 46 + row * 18));
			}
		}
		// Slot 27 = Fuel Cell intake (x=8, y=20), Slot 28 = Energy Cell intake (x=28, y=20)
		this.addSlot(new Slot(this.shipInv, 27, 8, 20));
		this.addSlot(new Slot(this.shipInv, 28, 28, 20));

		// Player inventory (y=114) & hotbar (y=172)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInv, col, 8 + col * 18, 172));
		}
	}

	public int fuelUnits() { return this.data.get(0); }
	public int energyKilo() { return this.data.get(1); }
	public int linkStatusOrdinal() { return this.data.get(2); }
	public int shipMode() { return this.data.get(3); }

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (this.ship == null || !(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
			return false;
		}
		if (buttonId == 0) {
			if (!sp.isPassengerOfSameVehicle(this.ship)) {
				sp.startRiding(this.ship, true);
			}
			this.ship.tryStartLaunchSequence(sp);
			return true;
		}
		if (buttonId == 1) {
			return this.ship.packUpIntoItem(sp);
		}
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			result = stack.copy();
			if (index < 29) {
				if (!this.moveItemStackTo(stack, 29, 65, true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 0, 29, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return result;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.ship == null || (this.ship.isAlive() && player.distanceToSqr(this.ship) <= 144.0);
	}
}

package com.galacticpurpose.menu;

import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.item.CoordinateCardItem;
import com.galacticpurpose.item.ExoticBioFuelItem;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModMenus;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.WormholeState;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
 * Container menu for the Wormhole Stabilizer Core with button actions and telemetry data.
 */
public class WormholeCoreMenu extends AbstractContainerMenu {
	private final Container container;
	private final ContainerData data;
	private final ContainerLevelAccess access;

	public WormholeCoreMenu(int syncId, Inventory playerInventory, BlockPos pos) {
		this(
			syncId,
			playerInventory,
			new SimpleContainer(4),
			new SimpleContainerData(11),
			ContainerLevelAccess.create(playerInventory.player.level(), pos)
		);
	}

	public WormholeCoreMenu(
		int syncId,
		Inventory playerInventory,
		Container container,
		ContainerData data,
		ContainerLevelAccess access
	) {
		super(ModMenus.WORMHOLE_CORE, syncId);
		checkContainerSize(container, 4);
		checkContainerDataCount(data, 11);
		this.container = container;
		this.data = data;
		this.access = access;
		container.startOpen(playerInventory.player);

		// Slot 0: Coordinate Card
		this.addSlot(new Slot(container, 0, 10, 22) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof CoordinateCardItem;
			}
		});
		// Slot 1: Exotic Bio-Fuel
		this.addSlot(new Slot(container, 1, 10, 44) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof ExoticBioFuelItem;
			}
		});
		// Slot 2: Coolant (Ice / Packed Ice / Blue Ice / Water Bucket)
		this.addSlot(new Slot(container, 2, 10, 66) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return WormholeCoreBlockEntity.getCoolantValue(stack) > 0;
			}
		});
		// Slot 3: Battery
		this.addSlot(new Slot(container, 3, 30, 66) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
			}
		});

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 120 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 178));
		}
		this.addDataSlots(data);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id < 0 || id > 2) {
			return false;
		}
		this.access.execute((level, pos) -> {
			if (player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof WormholeCoreBlockEntity core) {
				core.handleMenuButton(serverPlayer, id);
			}
		});
		return true;
	}

	public long energyStored() {
		return this.data.get(0) * 100L;
	}

	public long energyCapacity() {
		return Math.max(100L, this.data.get(1) * 100L);
	}

	public WormholeState wormholeState() {
		return WormholeState.byOrdinal(this.data.get(2));
	}

	public int tempCelsius() {
		return this.data.get(3);
	}

	public float stabilityPercent() {
		return this.data.get(4) / 10.0f;
	}

	public float chargePercent() {
		return this.data.get(5) / 10.0f;
	}

	public int fuelUnits() {
		return this.data.get(6);
	}

	public int coolantUnits() {
		return this.data.get(7);
	}

	public boolean isStructureValid() {
		return this.data.get(8) == 1;
	}

	public boolean isQuantumUpgraded() {
		return this.data.get(10) == 1;
	}

	public ExoplanetRoute targetRoute() {
		int ord = this.data.get(9);
		return ord >= 0 ? ExoplanetRoute.byOrdinal(ord) : null;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index < 4) {
			if (!this.moveItemStackTo(stack, 4, 40, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (stack.getItem() instanceof CoordinateCardItem) {
				if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
			} else if (stack.getItem() instanceof ExoticBioFuelItem) {
				if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
			} else if (WormholeCoreBlockEntity.getCoolantValue(stack) > 0) {
				if (!this.moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
			} else if (EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null) {
				if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
			} else if (index < 31) {
				if (!this.moveItemStackTo(stack, 31, 40, false)) return ItemStack.EMPTY;
			} else if (!this.moveItemStackTo(stack, 4, 31, false)) {
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
		return this.access.evaluate((level, pos) -> {
			if (!level.getBlockState(pos).is(ModBlocks.WORMHOLE_CORE)) {
				return false;
			}
			// 34-block reach so players can operate the 45x41x45 Colossal Observatory from the Floor Injector or Stabilizer walls
			return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 34.0 * 34.0;
		}, true);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.container.stopOpen(player);
	}
}

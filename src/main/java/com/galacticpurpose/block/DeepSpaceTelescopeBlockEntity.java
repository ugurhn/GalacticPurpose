package com.galacticpurpose.block;

import com.galacticpurpose.climate.OverworldHazardTicker;
import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Block entity performing timed deep-space coordinate scans when powered under open Martian sky.
 */
public class DeepSpaceTelescopeBlockEntity extends BlockEntity {
	public static final long ENERGY_CAPACITY = 20_000L;
	public static final long SCAN_COST_PER_TICK = 40L;
	public static final int SCAN_DURATION_TICKS = 80;

	private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 10_000L, 0L) {
		@Override
		protected void onFinalCommit() {
			DeepSpaceTelescopeBlockEntity.this.setChanged();
		}
	};

	private boolean scanning = false;
	private int scanTicks = 0;
	private int nextRouteIndex = 0;
	private ExoplanetRoute readyReward = null;

	public DeepSpaceTelescopeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DEEP_SPACE_TELESCOPE, pos, state);
	}

	public EnergyStorage energyStorage() {
		return this.energyStorage;
	}

	public boolean isScanning() {
		return this.scanning;
	}

	public int scanTicks() {
		return this.scanTicks;
	}

	public ExoplanetRoute readyReward() {
		return this.readyReward;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, DeepSpaceTelescopeBlockEntity be) {
		if (!(level instanceof ServerLevel serverLevel) || !be.scanning || be.readyReward != null) {
			return;
		}
		if (!SpaceCompatManager.get().isMars(serverLevel) || !OverworldHazardTicker.isExposedToSky(serverLevel, pos.above())) {
			be.scanning = false;
			be.setChanged();
			return;
		}
		if (be.energyStorage.amount < SCAN_COST_PER_TICK) {
			return;
		}
		be.energyStorage.amount -= SCAN_COST_PER_TICK;
		be.scanTicks++;
		if (be.scanTicks >= SCAN_DURATION_TICKS) {
			be.scanning = false;
			be.scanTicks = 0;
			be.readyReward = pickNextDiscoverableRoute(be.nextRouteIndex);
			be.nextRouteIndex = (be.nextRouteIndex + 1) % 3;
		}
		be.setChanged();
	}

	public void handleInteract(Player player) {
		if (this.readyReward != null) {
			ItemStack card = new ItemStack(ModItems.forCoordinateCard(this.readyReward));
			player.getInventory().placeItemBackInInventory(card);
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.telescope_discovered",
					Component.translatable(this.readyReward.translationKey())
				).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
				false
			);
			this.readyReward = null;
			this.setChanged();
			return;
		}

		if (this.scanning) {
			int pct = Math.min(100, Math.round((this.scanTicks / (float) SCAN_DURATION_TICKS) * 100.0f));
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.telescope_progress",
					pct,
					this.energyStorage.amount
				).withStyle(ChatFormatting.AQUA),
				true
			);
			return;
		}

		if (this.energyStorage.amount < SCAN_COST_PER_TICK * 10L) {
			player.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.telescope_no_energy",
					this.energyStorage.amount,
					SCAN_COST_PER_TICK * SCAN_DURATION_TICKS
				).withStyle(ChatFormatting.RED),
				false
			);
			return;
		}

		this.scanning = true;
		this.scanTicks = 0;
		this.setChanged();
		player.displayClientMessage(
			Component.translatable("message.galacticpurpose.telescope_started").withStyle(ChatFormatting.YELLOW),
			false
		);
	}

	private static ExoplanetRoute pickNextDiscoverableRoute(int index) {
		return switch (Math.floorMod(index, 3)) {
			case 0 -> ExoplanetRoute.HEPHAESTUS_IV;
			case 1 -> ExoplanetRoute.PELAGIA_9;
			default -> ExoplanetRoute.NYX_0;
		};
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putLong("Energy", this.energyStorage.amount);
		tag.putBoolean("Scanning", this.scanning);
		tag.putInt("ScanTicks", this.scanTicks);
		tag.putInt("NextRoute", this.nextRouteIndex);
		if (this.readyReward != null) {
			tag.putInt("ReadyReward", this.readyReward.ordinal());
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.energyStorage.amount = Math.max(0L, Math.min(ENERGY_CAPACITY, tag.getLong("Energy")));
		this.scanning = tag.getBoolean("Scanning");
		this.scanTicks = Math.max(0, tag.getInt("ScanTicks"));
		this.nextRouteIndex = Math.max(0, tag.getInt("NextRoute"));
		this.readyReward = tag.contains("ReadyReward") ? ExoplanetRoute.byOrdinal(tag.getInt("ReadyReward")) : null;
	}
}

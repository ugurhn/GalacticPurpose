package com.galacticpurpose.block;

import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.entity.SingularityCollapseEntity;
import com.galacticpurpose.item.CoordinateCardItem;
import com.galacticpurpose.item.ExoticBioFuelItem;
import com.galacticpurpose.menu.WormholeCoreMenu;
import com.galacticpurpose.registry.ModBlockEntities;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.ship.OrbitalTransitManager;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import com.galacticpurpose.wormhole.WormholeLinkRegistry;
import com.galacticpurpose.wormhole.WormholeMultiblockValidator;
import com.galacticpurpose.wormhole.WormholeStabilityCalculator;
import com.galacticpurpose.wormhole.WormholeState;
import com.galacticpurpose.wormhole.WormholeStepResult;
import com.galacticpurpose.wormhole.WormholeTier;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Containment Core controller balancing energy, exotic bio-fuel, coolant, and stability.
 */
public class WormholeCoreBlockEntity extends BlockEntity
	implements Container, ExtendedScreenHandlerFactory<BlockPos> {

	public static final long ENERGY_CAPACITY = 1_000_000L;
	public static final int MAX_FUEL_UNITS = 1_000;
	public static final int MAX_COOLANT_UNITS = 1_000;

	private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
	private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 20_000L, 0L) {
		@Override
		protected void onFinalCommit() {
			WormholeCoreBlockEntity.this.setChanged();
		}
	};

	private WormholeState wormholeState = WormholeState.IDLE;
	private float tempCelsius = WormholeStabilityCalculator.AMBIENT_TEMP;
	private float stabilityPercent = 100.0f;
	private float chargePercent = 0.0f;
	private int fuelUnits = 0;
	private int coolantUnits = 0;
	private boolean structureValid = false;
	private boolean quantumUpgraded = false;
	private WormholeTier activeTier = WormholeTier.PERSONAL;

	private transient WormholeState prevSyncedState;
	private transient boolean prevSyncedValid;
	private transient boolean prevSyncedQuantum;
	private transient ExoplanetRoute prevSyncedRoute;
	private transient float prevSyncedTemp = -999.0f;
	private transient float prevSyncedCharge = -999.0f;
	private transient int syncCooldown = 0;

	// Client-side smooth frame-interpolated visual telemetry & cached per-side stabilizer mask (avoids 36 getBlockState/frame!)
	public float clientVisualRadius = -1.0f;
	public float clientVisualTemp = -1.0f;
	public float clientVisualStability = -1.0f;
	public long clientLastRenderNanos = 0L;
	public int clientSideIntactMask = 0x1F;
	public long clientMaskGameTime = -100L;

	private final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(int index) {
			ExoplanetRoute route = selectedRoute();
			return switch (index) {
				case 0 -> (int) Math.min(30_000, energyStorage.amount / 100L);
				case 1 -> (int) Math.min(30_000, energyStorage.capacity / 100L);
				case 2 -> wormholeState.ordinal();
				case 3 -> Math.round(tempCelsius);
				case 4 -> Math.round(stabilityPercent * 10.0f);
				case 5 -> Math.round(chargePercent * 10.0f);
				case 6 -> fuelUnits;
				case 7 -> coolantUnits;
				case 8 -> structureValid ? 1 : 0;
				case 9 -> route != null ? route.ordinal() : -1;
				case 10 -> quantumUpgraded ? 1 : 0;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return 11;
		}
	};

	public WormholeCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WORMHOLE_CORE, pos, state);
	}

	public EnergyStorage energyStorage() {
		return this.energyStorage;
	}

	public WormholeState wormholeState() {
		return this.wormholeState;
	}

	public float tempCelsius() {
		return this.tempCelsius;
	}

	public float stabilityPercent() {
		return this.stabilityPercent;
	}

	public float chargePercent() {
		return this.chargePercent;
	}

	public int fuelUnits() {
		return this.fuelUnits;
	}

	public int coolantUnits() {
		return this.coolantUnits;
	}

	public boolean isStructureValid() {
		return this.structureValid;
	}

	public boolean isQuantumUpgraded() {
		return this.quantumUpgraded;
	}

	public WormholeTier activeTier() {
		return this.activeTier;
	}

	public boolean tryUpgradeWithQuantumCore(Player player, ItemStack stack) {
		if (this.quantumUpgraded) {
			if (player != null) {
				player.displayClientMessage(
					Component.translatable("message.galacticpurpose.wormhole_already_quantum")
						.withStyle(ChatFormatting.AQUA),
					true
				);
			}
			return false;
		}
		this.quantumUpgraded = true;
		if (player == null || !player.isCreative()) {
			stack.shrink(1);
		}
		this.setChanged();
		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			this.level.playSound(
				null,
				this.worldPosition,
				SoundEvents.BEACON_POWER_SELECT,
				SoundSource.BLOCKS,
				1.0f,
				1.2f
			);
		}
		if (player != null) {
			ItemStack sanctuaryCard = new ItemStack(ModItems.forCoordinateCard(ExoplanetRoute.SANCTUARY));
			if (player.getInventory().countItem(sanctuaryCard.getItem()) == 0) {
				player.getInventory().placeItemBackInInventory(sanctuaryCard);
			}
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.wormhole_quantum_upgraded")
					.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
				false
			);
		}
		return true;
	}

	public ExoplanetRoute selectedRoute() {
		ItemStack card = this.items.get(0);
		if (card.getItem() instanceof CoordinateCardItem coord) {
			return coord.route();
		}
		return null;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, WormholeCoreBlockEntity core) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		core.pullBatteryEnergy();
		core.intakeFuelAndCoolant();

		// Colossal ship-scale open core: Starships flying into the giant sphere launch into full Walkable Orbital Spaceflight!
		// On-foot players flying in (without Shift) continue using fast direct solo transit.
		if (core.wormholeState == WormholeState.OPEN
			&& core.activeTier == WormholeTier.COLOSSAL
			&& serverLevel.getGameTime() % 4L == 0L) {
			ExoplanetRoute route = core.selectedRoute();
			if (route != null) {
				Vec3 center = Vec3.atCenterOf(pos);
				AABB coreSphere = new AABB(center, center).inflate(core.activeTier.coreRadius() + 1.2);
				for (InterstellarShipEntity ship : serverLevel.getEntitiesOfClass(InterstellarShipEntity.class, coreSphere)) {
					if (ship.getFirstPassenger() instanceof ServerPlayer pilot
						&& !pilot.isOnPortalCooldown()
						&& ship.position().distanceTo(center) <= core.activeTier.coreRadius() + 2.0) {
						OrbitalTransitManager.beginOutboundTransit(
							pilot, ship, route, serverLevel.dimension().location(), pos
						);
						return;
					}
				}
				for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, coreSphere)) {
					if (!(player.getVehicle() instanceof InterstellarShipEntity)
						&& player.position().distanceTo(center) <= core.activeTier.coreRadius() + 0.5) {
						repelUnshieldedPlayer(serverLevel, pos, pos, player);
					}
				}
			}
		}

		if (serverLevel.getGameTime() % 20L != 0L) {
			return;
		}

		WormholeMultiblockValidator.ValidationResult validation = WormholeMultiblockValidator.validate(serverLevel, pos);
		core.structureValid = validation.valid();
		if (validation.valid()) {
			core.activeTier = validation.tier();
		}
		WormholeStepResult step = WormholeStabilityCalculator.step(
			core.wormholeState,
			core.structureValid,
			core.selectedRoute() != null,
			core.tempCelsius,
			core.stabilityPercent,
			core.chargePercent,
			core.fuelUnits,
			core.coolantUnits,
			core.energyStorage.amount
		);

		core.energyStorage.amount = Math.max(0L, core.energyStorage.amount - step.energyConsumed());
		core.wormholeState = step.nextState();
		core.tempCelsius = step.nextTempCelsius();
		core.stabilityPercent = step.nextStabilityPercent();
		core.chargePercent = step.nextChargePercent();
		core.fuelUnits = step.nextFuelUnits();
		core.coolantUnits = step.nextCoolantUnits();
		core.setChanged();

		if (step.meltdown()) {
			core.triggerMeltdown(serverLevel, pos);
			return;
		}

		WormholeLinkRegistry.get(serverLevel.getServer()).updateStatus(
			serverLevel.dimension().location(),
			pos,
			core.wormholeState == WormholeState.OPEN
				? WormholeLinkCalculator.LinkStatus.OPEN
				: WormholeLinkCalculator.LinkStatus.CLOSED
		);
		int stage = switch (core.wormholeState) {
			case IDLE -> 0;
			case CHARGING, SHUTTING_DOWN -> 1;
			case OPEN -> 2;
			case UNSTABLE -> 3;
		};
		BlockState updatedState = state.getValue(WormholeCoreBlock.STAGE) != stage
			? state.setValue(WormholeCoreBlock.STAGE, stage)
			: state;
		ExoplanetRoute curRoute = core.selectedRoute();
		boolean majorChange = updatedState != state
			|| core.wormholeState != core.prevSyncedState
			|| core.structureValid != core.prevSyncedValid
			|| core.quantumUpgraded != core.prevSyncedQuantum
			|| curRoute != core.prevSyncedRoute;
		boolean visualShift = Math.abs(core.tempCelsius - core.prevSyncedTemp) > 120.0f
			|| Math.abs(core.chargePercent - core.prevSyncedCharge) > 2.5f;

		if (updatedState != state) {
			serverLevel.setBlock(pos, updatedState, Block.UPDATE_CLIENTS);
		}
		if (majorChange || (visualShift && ++core.syncCooldown >= 2)) {
			core.syncCooldown = 0;
			core.prevSyncedState = core.wormholeState;
			core.prevSyncedValid = core.structureValid;
			core.prevSyncedQuantum = core.quantumUpgraded;
			core.prevSyncedRoute = curRoute;
			core.prevSyncedTemp = core.tempCelsius;
			core.prevSyncedCharge = core.chargePercent;
			if (majorChange) {
				core.syncPortalBlock(serverLevel, pos);
			}
			serverLevel.sendBlockUpdated(pos, updatedState, updatedState, Block.UPDATE_CLIENTS);
		}

		// Continuous humming is handled smoothly out to 38b/72b by client WormholeAmbienceSoundInstance;
		// server emits wide-range creepy Warden heartbeat & alarm pulses on UNSTABLE crisis!
		long gt = serverLevel.getGameTime();
		float alarmVol = core.activeTier == WormholeTier.COLOSSAL ? 4.2f : 2.2f;
		if (core.wormholeState == WormholeState.UNSTABLE) {
			serverLevel.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, alarmVol, 0.65f);
			if (gt % 40L == 0L) {
				serverLevel.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, alarmVol * 0.85f, 0.55f);
			}
		}
	}

	private void pullBatteryEnergy() {
		ItemStack battery = this.items.get(3);
		if (battery.isEmpty() || this.energyStorage.amount >= this.energyStorage.capacity) {
			return;
		}
		ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(this, null).getSlot(3));
		EnergyStorage itemEnergy = EnergyStorage.ITEM.find(battery, ctx);
		if (itemEnergy != null && itemEnergy.supportsExtraction()) {
			long moved = EnergyStorageUtil.move(itemEnergy, this.energyStorage, 20_000L, null);
			if (moved > 0) {
				this.setChanged();
			}
		}
	}

	private void intakeFuelAndCoolant() {
		ItemStack fuelStack = this.items.get(1);
		if (fuelStack.getItem() instanceof ExoticBioFuelItem && this.fuelUnits <= MAX_FUEL_UNITS - ExoticBioFuelItem.FUEL_VALUE) {
			fuelStack.shrink(1);
			this.fuelUnits += ExoticBioFuelItem.FUEL_VALUE;
			this.setChanged();
		}

		ItemStack coolantStack = this.items.get(2);
		int coolantValue = getCoolantValue(coolantStack);
		if (coolantValue > 0 && this.coolantUnits <= MAX_COOLANT_UNITS - coolantValue) {
			if (coolantStack.is(Items.WATER_BUCKET)) {
				this.items.set(2, new ItemStack(Items.BUCKET));
			} else {
				coolantStack.shrink(1);
			}
			this.coolantUnits += coolantValue;
			this.setChanged();
		}
	}

	public static int getCoolantValue(ItemStack stack) {
		if (stack.is(Items.BLUE_ICE)) return 350;
		if (stack.is(Items.PACKED_ICE)) return 150;
		if (stack.is(Items.ICE)) return 50;
		if (stack.is(Items.WATER_BUCKET)) return 100;
		return 0;
	}

	private void syncPortalBlock(ServerLevel level, BlockPos corePos) {
		int drop = this.activeTier.portalDrop();
		int half = this.activeTier.portalHalfExtent();
		if (this.wormholeState == WormholeState.OPEN) {
			for (int dx = -half; dx <= half; dx++) {
				for (int dz = -half; dz <= half; dz++) {
					if (half > 0 && dx * dx + dz * dz > half * half + 2) {
						continue;
					}
					BlockPos portalPos = corePos.offset(dx, -drop, dz);
					if (level.getBlockState(portalPos).isAir()) {
						level.setBlock(portalPos, ModBlocks.WORMHOLE_PORTAL.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
		} else {
			clearAllPortalBlocks(level, corePos);
		}
	}

	public void clearAllPortalBlocks(Level level, BlockPos corePos) {
		if (level.getBlockState(corePos.below()).is(ModBlocks.WORMHOLE_PORTAL)) {
			level.setBlock(corePos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		int drop = WormholeTier.COLOSSAL.portalDrop();
		int half = WormholeTier.COLOSSAL.portalHalfExtent();
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				BlockPos p = corePos.offset(dx, -drop, dz);
				if (level.getBlockState(p).is(ModBlocks.WORMHOLE_PORTAL)) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}
	}

	private void triggerMeltdown(ServerLevel level, BlockPos pos) {
		clearAllPortalBlocks(level, pos);
		WormholeLinkRegistry.get(level.getServer())
			.updateStatus(level.dimension().location(), pos, WormholeLinkCalculator.LinkStatus.DESTROYED);
		WormholeTier tier = this.activeTier;
		level.removeBlock(pos, false);
		SingularityCollapseEntity.spawnForTier(level, Vec3.atCenterOf(pos), tier);
	}

	public void configureStablePreset(ServerLevel level, ExoplanetRoute route) {
		configureStablePreset(level, route, this.activeTier);
	}

	public void configureStablePreset(ServerLevel level, ExoplanetRoute route, WormholeTier tier) {
		this.activeTier = tier;
		this.items.set(0, new ItemStack(ModItems.forCoordinateCard(route)));
		this.items.set(1, new ItemStack(ModItems.EXOTIC_BIO_FUEL, 16));
		this.items.set(2, new ItemStack(Items.BLUE_ICE, 16));
		this.items.set(3, new ItemStack(ModItems.CREATIVE_ENERGY_CELL));
		this.energyStorage.amount = 900_000L;
		this.fuelUnits = MAX_FUEL_UNITS;
		this.coolantUnits = MAX_COOLANT_UNITS;
		this.tempCelsius = 6_100.0f;
		this.stabilityPercent = 100.0f;
		this.chargePercent = 100.0f;
		this.structureValid = true;
		this.quantumUpgraded = route.requiresQuantumCore();
		this.wormholeState = WormholeState.OPEN;
		this.setChanged();

		BlockState next = getBlockState().setValue(WormholeCoreBlock.STAGE, 2);
		level.setBlock(this.worldPosition, next, Block.UPDATE_ALL);
		syncPortalBlock(level, this.worldPosition);
		WormholeLinkRegistry.get(level.getServer()).updateStatus(
			level.dimension().location(), this.worldPosition, WormholeLinkCalculator.LinkStatus.OPEN
		);
		level.sendBlockUpdated(this.worldPosition, next, next, Block.UPDATE_CLIENTS);
	}

	public void configureUnstablePreset(ServerLevel level) {
		configureUnstablePreset(level, this.activeTier);
	}

	public void configureUnstablePreset(ServerLevel level, WormholeTier tier) {
		this.activeTier = tier;
		this.items.set(0, new ItemStack(ModItems.forCoordinateCard(ExoplanetRoute.HEPHAESTUS_IV)));
		this.items.set(1, new ItemStack(ModItems.EXOTIC_BIO_FUEL, 16));
		this.items.set(2, ItemStack.EMPTY);
		this.items.set(3, ItemStack.EMPTY);
		// Starts UNSTABLE (~8,650°C): ~5 seconds left to Quench before passing 9,300°C Point of No Return, ~11s to full breach!
		this.energyStorage.amount = 45_000L;
		this.fuelUnits = 800;
		this.coolantUnits = 0;
		this.tempCelsius = 8_650.0f;
		this.stabilityPercent = 46.0f;
		this.chargePercent = 82.0f;
		this.structureValid = true;
		this.wormholeState = WormholeState.UNSTABLE;
		this.setChanged();

		BlockState next = getBlockState().setValue(WormholeCoreBlock.STAGE, 3);
		level.setBlock(this.worldPosition, next, Block.UPDATE_ALL);
		syncPortalBlock(level, this.worldPosition);
		WormholeLinkRegistry.get(level.getServer()).updateStatus(
			level.dimension().location(), this.worldPosition, WormholeLinkCalculator.LinkStatus.CLOSED
		);
		level.sendBlockUpdated(this.worldPosition, next, next, Block.UPDATE_CLIENTS);
	}

	public void handleMenuButton(Player player, int buttonId) {
		if (this.level == null) {
			return;
		}
		WormholeMultiblockValidator.ValidationResult check = WormholeMultiblockValidator.validate(this.level, this.worldPosition);
		this.structureValid = check.valid();
		if (check.valid()) {
			this.activeTier = check.tier();
		}
		if (buttonId == 0) {
			this.pullBatteryEnergy();
			this.intakeFuelAndCoolant();
			if (!this.structureValid) {
				if (player != null) {
					player.displayClientMessage(
						Component.translatable("message.galacticpurpose.wormhole_cannot_ignite_structure")
							.withStyle(ChatFormatting.RED),
						false
					);
				}
				return;
			}
			ExoplanetRoute route = selectedRoute();
			if (route == null) {
				if (player != null) {
					player.displayClientMessage(
						Component.translatable("message.galacticpurpose.wormhole_no_card").withStyle(ChatFormatting.RED),
						false
					);
				}
				return;
			}
			if (route.requiresQuantumCore() && !this.quantumUpgraded) {
				if (player != null) {
					player.displayClientMessage(
						Component.translatable("message.galacticpurpose.wormhole_needs_quantum_core")
							.withStyle(ChatFormatting.GOLD),
						false
					);
				}
				return;
			}
			if (this.fuelUnits <= 0) {
				if (player != null) {
					player.displayClientMessage(
						Component.translatable("message.galacticpurpose.wormhole_no_fuel").withStyle(ChatFormatting.RED),
						false
					);
				}
				return;
			}
			if (this.wormholeState == WormholeState.IDLE || this.wormholeState == WormholeState.SHUTTING_DOWN) {
				this.wormholeState = WormholeState.CHARGING;
				this.setChanged();
				this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			}
		} else if (buttonId == 1) {
			if (this.wormholeState != WormholeState.IDLE) {
				if (WormholeStabilityCalculator.isPastPointOfNoReturn(
					this.wormholeState, this.structureValid, this.tempCelsius, this.stabilityPercent)) {
					if (player != null) {
						player.displayClientMessage(
							Component.translatable("message.galacticpurpose.wormhole_point_of_no_return")
								.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
							false
						);
					}
					return;
				}
				this.wormholeState = WormholeState.SHUTTING_DOWN;
				this.setChanged();
				this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			}
		} else if (buttonId == 2) {
			if (this.wormholeState == WormholeState.OPEN && player != null) {
				if (player instanceof ServerPlayer sp) {
					sp.closeContainer();
				}
				if (this.activeTier != WormholeTier.COLOSSAL) {
					player.displayClientMessage(
						Component.translatable("message.galacticpurpose.wormhole_small_no_transit")
							.withStyle(ChatFormatting.GOLD),
						false
					);
				} else {
					player.displayClientMessage(
						Component.translatable("message.galacticpurpose.wormhole_enter_via_ship")
							.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
						false
					);
				}
			}
		}
	}

	public static void repelUnshieldedPlayer(ServerLevel level, BlockPos portalPos, BlockPos corePos, Player player) {
		if (player.isOnPortalCooldown()) {
			return;
		}
		player.setPortalCooldown(35);
		Vec3 center = Vec3.atCenterOf(portalPos);
		Vec3 away = player.position().subtract(center);
		if (away.lengthSqr() < 0.05) {
			away = new Vec3(0.0, 0.35, 1.0);
		}
		Vec3 knock = away.normalize().scale(1.15);
		player.push(knock.x, Math.max(0.28, knock.y * 0.45), knock.z);
		player.hurtMarked = true;
		if (!player.isCreative() && !player.isSpectator()) {
			player.hurt(level.damageSources().magic(), 6.0f);
		}
		level.playSound(null, corePos, net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 1.4f, 0.55f);
		player.displayClientMessage(
			Component.translatable("message.galacticpurpose.wormhole_requires_ship")
				.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
			true
		);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putLong("Energy", this.energyStorage.amount);
		tag.putInt("State", this.wormholeState.ordinal());
		tag.putFloat("Temp", this.tempCelsius);
		tag.putFloat("Stability", this.stabilityPercent);
		tag.putFloat("Charge", this.chargePercent);
		tag.putInt("Fuel", this.fuelUnits);
		tag.putInt("Coolant", this.coolantUnits);
		tag.putBoolean("QuantumUpgraded", this.quantumUpgraded);
		tag.putBoolean("StructureValid", this.structureValid);
		tag.putInt("Tier", this.activeTier.ordinal());
		ContainerHelper.saveAllItems(tag, this.items, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.energyStorage.amount = Math.max(0L, Math.min(ENERGY_CAPACITY, tag.getLong("Energy")));
		this.wormholeState = WormholeState.byOrdinal(tag.getInt("State"));
		this.tempCelsius = Math.max(WormholeStabilityCalculator.AMBIENT_TEMP, tag.getFloat("Temp"));
		this.stabilityPercent = Math.max(0.0f, Math.min(100.0f, tag.getFloat("Stability")));
		this.chargePercent = Math.max(0.0f, Math.min(100.0f, tag.getFloat("Charge")));
		this.fuelUnits = Math.max(0, Math.min(MAX_FUEL_UNITS, tag.getInt("Fuel")));
		this.coolantUnits = Math.max(0, Math.min(MAX_COOLANT_UNITS, tag.getInt("Coolant")));
		this.quantumUpgraded = tag.getBoolean("QuantumUpgraded");
		this.structureValid = tag.getBoolean("StructureValid");
		this.activeTier = WormholeTier.byOrdinal(tag.getInt("Tier"));
		this.items.clear();
		ContainerHelper.loadAllItems(tag, this.items, registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return this.worldPosition;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.galacticpurpose.wormhole_core");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new WormholeCoreMenu(
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
		if (this.isRemoved() || this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
			return false;
		}
		double maxDist = this.activeTier == WormholeTier.COLOSSAL ? 34.0 : 9.0;
		return player.distanceToSqr(
			this.worldPosition.getX() + 0.5,
			this.worldPosition.getY() + 0.5,
			this.worldPosition.getZ() + 0.5
		) <= maxDist * maxDist;
	}

	@Override
	public void clearContent() {
		this.items.clear();
		this.setChanged();
	}
}

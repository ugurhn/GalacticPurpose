package com.galacticpurpose.entity;

import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.exoplanet.ExoplanetLandmarkBuilder;
import com.galacticpurpose.item.LeadEnergyCellItem;
import com.galacticpurpose.item.StarshipFuelCellItem;
import com.galacticpurpose.menu.StarshipMenu;
import com.galacticpurpose.network.ClientSoundEventPayload;
import com.galacticpurpose.registry.ModEntities;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.ship.OrbitalTransitManager;
import com.galacticpurpose.ship.StarshipFlightPhysics;
import com.galacticpurpose.ship.StarshipLandingCalculator;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import com.galacticpurpose.wormhole.WormholeLinkRegistry;
import com.galacticpurpose.wormhole.WormholeState;
import com.galacticpurpose.wormhole.WormholeTier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.TicketType;
import java.util.Comparator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/**
 * Full-Scale Horizontal Delta-Wing Horizon Lander (~16 x 11 x 5 blocks matching the walkable orbital deck!):
 * - Launches from an unobstructed diagonal Magnetic Runway via [Space x2] (or [G]) -> 10s countdown -> catapult into Wormhole
 * - Realistic aerodynamic forward-vector takeoff & orbital climb:
 *     pitch follows smooth x^1.2 lift curve, forward speed accelerates along x^1.5, flies STRICTLY along nose vector
 *     with ZERO Y=320 ceiling stall!
 * - Smooth automated propulsive natural-terrain descent (zero blocky vanilla smoke/fire spam — uses 3D volumetric renderer FX!)
 * - Origin Wormhole live link verification & 27-slot persistent Cargo Bay
 */
public class InterstellarShipEntity extends Entity implements ExtendedScreenHandlerFactory<Integer> {
	public static final int MODE_PARKED = 0;
	public static final int MODE_COUNTDOWN = 1;
	public static final int MODE_RUNWAY_LAUNCH = 2;
	public static final int MODE_ORBITAL_ASCENT = 3;
	public static final int MODE_AUTO_DESCENT = 4;
	public static final int MODE_RELOCATING = 5;

	public static final int MAX_FUEL = 4000;
	public static final long MAX_ENERGY = 500_000L;
	private static final TicketType<ChunkPos> RELOCATE_TICKET = TicketType.create(
		"galacticpurpose_relocate",
		Comparator.comparingLong(ChunkPos::toLong),
		40
	);

	private static final EntityDataAccessor<Integer> DATA_MODE =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_FUEL =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ENERGY_K =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_LEG_DEPLOY =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> DATA_HYDRO_PONTOON =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_LINK_STATUS =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_THRUST =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_PITCH_DEG =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_COUNTDOWN_TICKS =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ASCENT_TICKS =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_GROUND_ALT =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_VECTOR_ANGLE =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_ROLL_DEG =
		SynchedEntityData.defineId(InterstellarShipEntity.class, EntityDataSerializers.FLOAT);

	private float clientPitchO = 0.0f;
	private float clientRollO = 0.0f;

	// 27 Cargo slots + Slot 27 = Fuel Cell Intake + Slot 28 = Energy Cell Intake
	private final SimpleContainer inventory = new SimpleContainer(29);
	private long energyStored = 250_000L;
	private ResourceLocation originDimId;
	private BlockPos originCorePos;
	private double targetLandingY = 70.0;
	private double targetLandingX = 0.0;
	private double targetLandingZ = 0.0;
	private boolean hasLandingTarget = false;
	private double landingDirX = 0.0;
	private double landingDirZ = -1.0;
	private double landingFwdSpeed = 1.15;
	private double landingVertSpeed = -0.52;
	private double launchOriginY = 70.0;
	private int countdownTicks = 0;
	private int launchTicks = 0;
	private int ascentTicks = 0;
	private boolean countdownForExoplanet = false;
	private BlockPos lockedWormholeCorePos = null;
	private double relocateTargetX = 0.0;
	private double relocateTargetY = 70.0;
	private double relocateTargetZ = 0.0;
	private double relocatePeakY = 95.0;
	private double relocateBrakeDist = 26.0;
	private double relocateFwdSpeed = 0.0;
	private double relocateVertSpeed = 0.0;
	private int relocateSubPhase = 0;
	private int relocateTicks = 0;
	private int relocateFuelTotal = 0;
	private int relocateFuelBurned = 0;
	private double relocateInitialDist = 1.0;
	private int relocateTurnSign = 1;
	private int ascentFuelBurned = 0;
	private double extVelX = 0.0;
	private double extVelY = 0.0;
	private double extVelZ = 0.0;
	private UUID relocateCommanderId = null;

	public InterstellarShipEntity(EntityType<? extends InterstellarShipEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
		this.setNoGravity(true);
	}

	public static InterstellarShipEntity spawnParked(
		ServerLevel level, double x, double y, double z, float yawDeg, int initialFuel, boolean hydroPontoon
	) {
		return spawnParked(level, x, y, z, yawDeg, initialFuel, 250_000L, hydroPontoon);
	}

	public static InterstellarShipEntity spawnParked(
		ServerLevel level, double x, double y, double z, float yawDeg, int initialFuel, long initialEnergy, boolean hydroPontoon
	) {
		InterstellarShipEntity ship = new InterstellarShipEntity(ModEntities.INTERSTELLAR_SHIP, level);
		ship.setPos(x, y, z);
		ship.setYRot(yawDeg);
		ship.yRotO = yawDeg;
		ship.entityData.set(DATA_MODE, MODE_PARKED);
		ship.entityData.set(DATA_FUEL, Mth.clamp(initialFuel, 0, MAX_FUEL));
		ship.energyStored = Mth.clamp(initialEnergy, 0L, MAX_ENERGY);
		ship.entityData.set(DATA_ENERGY_K, (int) (ship.energyStored / 1000L));
		ship.entityData.set(DATA_LEG_DEPLOY, 1.0f);
		ship.entityData.set(DATA_HYDRO_PONTOON, hydroPontoon);
		ship.entityData.set(DATA_PITCH_DEG, 0.0f);
		ship.entityData.set(DATA_VECTOR_ANGLE, 0.0f);
		ship.entityData.set(DATA_GROUND_ALT, 0.0f);
		ship.targetLandingY = y;
		level.addFreshEntity(ship);
		return ship;
	}

	public static InterstellarShipEntity spawnParked(
		ServerLevel level, double x, double y, double z, int initialFuel, boolean hydroPontoon
	) {
		return spawnParked(level, x, y, z, 0.0f, initialFuel, 250_000L, hydroPontoon);
	}

	public static InterstellarShipEntity spawnForAutoDescent(
		ServerLevel level,
		double x, double startY, double z,
		double landingY, boolean hydroPontoon,
		int fuelUnits, long energyStored,
		CompoundTag cargoTag, HolderLookup.Provider registries,
		ResourceLocation originDimId, BlockPos originCorePos
	) {
		// Landing corridor chunks are staggered beforehand in OrbitalTransitManager; ensure only target chunk here
		level.getChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4);

		double approachSpawnX = x;
		double approachSpawnZ = z + 78.0;
		double approachSpawnY = landingY + 46.0;
		float approachYaw = computeYawFacingTarget(approachSpawnX, approachSpawnZ, x, z);

		InterstellarShipEntity ship = new InterstellarShipEntity(ModEntities.INTERSTELLAR_SHIP, level);
		ship.setPos(approachSpawnX, approachSpawnY, approachSpawnZ);
		ship.setYRot(approachYaw);
		ship.yRotO = approachYaw;
		ship.entityData.set(DATA_MODE, MODE_AUTO_DESCENT);
		ship.entityData.set(DATA_FUEL, Mth.clamp(fuelUnits, 0, MAX_FUEL));
		ship.entityData.set(DATA_LEG_DEPLOY, 0.0f);
		ship.entityData.set(DATA_HYDRO_PONTOON, hydroPontoon);
		ship.entityData.set(DATA_PITCH_DEG, -18.0f);
		ship.entityData.set(DATA_VECTOR_ANGLE, 0.0f);
		ship.entityData.set(DATA_GROUND_ALT, (float) Math.max(0.0, approachSpawnY - landingY));
		ship.energyStored = Mth.clamp(energyStored, 0L, MAX_ENERGY);
		ship.entityData.set(DATA_ENERGY_K, (int) (ship.energyStored / 1000L));
		ship.targetLandingY = landingY;
		ship.targetLandingX = x;
		ship.targetLandingZ = z;
		ship.hasLandingTarget = true;
		ship.landingDirX = 0.0;
		ship.landingDirZ = -1.0;
		ship.landingFwdSpeed = 1.15;
		ship.landingVertSpeed = -0.52;
		ship.originDimId = originDimId;
		ship.originCorePos = originCorePos;
		ship.importCargoTag(cargoTag, registries);
		level.addFreshEntity(ship);
		return ship;
	}

	public static BlockPos findNearestColossalWormhole(ServerLevel level, BlockPos origin, int radiusChunks, boolean requireOpen) {
		int cx = origin.getX() >> 4;
		int cz = origin.getZ() >> 4;
		BlockPos best = null;
		double bestDistSq = 112.0 * 112.0;
		for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
			for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
				if (!level.hasChunk(cx + dx, cz + dz)) {
					continue;
				}
				LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof WormholeCoreBlockEntity core && core.activeTier() == WormholeTier.COLOSSAL) {
						if (requireOpen && core.wormholeState() != WormholeState.OPEN) {
							continue;
						}
						double dSq = core.getBlockPos().distSqr(origin);
						if (dSq < bestDistSq) {
							bestDistSq = dSq;
							best = core.getBlockPos().immutable();
						}
					}
				}
			}
		}
		return best;
	}

	public static float computeYawFacingTarget(double fromX, double fromZ, double toX, double toZ) {
		double dx = toX - fromX;
		double dz = toZ - fromZ;
		return (float) Math.toDegrees(Math.atan2(-dx, dz));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_MODE, MODE_PARKED);
		builder.define(DATA_FUEL, 2000);
		builder.define(DATA_ENERGY_K, 250);
		builder.define(DATA_LEG_DEPLOY, 1.0f);
		builder.define(DATA_HYDRO_PONTOON, false);
		builder.define(DATA_LINK_STATUS, WormholeLinkCalculator.LinkStatus.UNLINKED.ordinal());
		builder.define(DATA_THRUST, 0.0f);
		builder.define(DATA_PITCH_DEG, 0.0f);
		builder.define(DATA_COUNTDOWN_TICKS, 0);
		builder.define(DATA_ASCENT_TICKS, 0);
		builder.define(DATA_GROUND_ALT, 0.0f);
		builder.define(DATA_VECTOR_ANGLE, 0.0f);
		builder.define(DATA_ROLL_DEG, 0.0f);
	}

	public int shipMode() { return this.entityData.get(DATA_MODE); }
	public int fuelUnits() { return this.entityData.get(DATA_FUEL); }
	public long energyStored() { return this.energyStored; }
	public int energyKilo() { return this.entityData.get(DATA_ENERGY_K); }
	public float legDeploy() { return this.entityData.get(DATA_LEG_DEPLOY); }
	public boolean hydroPontoon() { return this.entityData.get(DATA_HYDRO_PONTOON); }
	public float thrustVisual() { return this.entityData.get(DATA_THRUST); }
	public float pitchDeg() { return this.entityData.get(DATA_PITCH_DEG); }
	public float rollDeg() { return this.entityData.get(DATA_ROLL_DEG); }
	public float clientPitchO() { return this.clientPitchO; }
	public float clientRollO() { return this.clientRollO; }
	public int ascentFuelBurned() { return this.ascentFuelBurned; }
	public int syncedCountdownTicks() { return this.entityData.get(DATA_COUNTDOWN_TICKS); }
	public int syncedAscentTicks() { return this.entityData.get(DATA_ASCENT_TICKS); }
	public float groundAltitude() { return this.entityData.get(DATA_GROUND_ALT); }
	public float syncedVectorAngle() { return this.entityData.get(DATA_VECTOR_ANGLE); }
	public ResourceLocation originDimId() { return this.originDimId; }
	public BlockPos originCorePos() { return this.originCorePos; }
	public SimpleContainer inventory() { return this.inventory; }

	public WormholeLinkCalculator.LinkStatus syncedLinkStatus() {
		WormholeLinkCalculator.LinkStatus[] vals = WormholeLinkCalculator.LinkStatus.values();
		int ord = this.entityData.get(DATA_LINK_STATUS);
		return (ord >= 0 && ord < vals.length) ? vals[ord] : WormholeLinkCalculator.LinkStatus.UNLINKED;
	}

	public void setOriginLink(ResourceLocation dimId, BlockPos corePos) {
		this.originDimId = dimId;
		this.originCorePos = corePos != null ? corePos.immutable() : null;
	}

	public void addFuel(int amount) {
		this.entityData.set(DATA_FUEL, Mth.clamp(fuelUnits() + amount, 0, MAX_FUEL));
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
			ItemStack held = player.getItemInHand(hand);
			if (held.getItem() instanceof com.galacticpurpose.item.StarshipFlightDirectorItem) {
				com.galacticpurpose.item.StarshipFlightDirectorItem.bindShip(held, this, serverPlayer);
				return InteractionResult.SUCCESS;
			}
			if (held.getItem() instanceof StarshipFuelCellItem && fuelUnits() < MAX_FUEL) {
				addFuel(StarshipFuelCellItem.FUEL_VALUE);
				if (!player.isCreative()) {
					held.shrink(1);
				}
				this.level().playSound(null, this.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.9f, 1.25f);
				serverPlayer.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_refueled", fuelUnits(), MAX_FUEL)
						.withStyle(ChatFormatting.GREEN),
					true
				);
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown()) {
				openStarshipScreen(serverPlayer);
				return InteractionResult.SUCCESS;
			}
			if (!this.isVehicle()) {
				player.startRiding(this);
				player.setYRot(this.getYRot());
				serverPlayer.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_boarded").withStyle(ChatFormatting.AQUA),
					true
				);
			}
		}
		return InteractionResult.sidedSuccess(this.level().isClientSide);
	}

	public void openStarshipScreen(ServerPlayer player) {
		player.openMenu(this);
	}

	public WormholeLinkCalculator.LaunchDecision tryStartLaunchSequence(ServerPlayer pilot) {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return WormholeLinkCalculator.LaunchDecision.ABORTED_NO_LINK;
		}
		int curMode = shipMode();
		if (curMode != MODE_PARKED) {
			return WormholeLinkCalculator.LaunchDecision.READY;
		}

		ExoplanetRoute currentPlanet = ExoplanetRoute.fromDimensionId(serverLevel.dimension().location());
		boolean onExoplanet = currentPlanet != null;

		if (!onExoplanet) {
			BlockPos openCore = findNearestColossalWormhole(serverLevel, this.blockPosition(), 7, true);
			if (openCore == null) {
				BlockPos anyCore = findNearestColossalWormhole(serverLevel, this.blockPosition(), 7, false);
				sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_ABORT_ALARM, 0.72f, 1.0f);
				pilot.displayClientMessage(
					Component.translatable(anyCore != null
						? "message.galacticpurpose.ship_wormhole_not_open"
						: "message.galacticpurpose.ship_no_wormhole_ahead"
					).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					false
				);
				return anyCore != null
					? WormholeLinkCalculator.LaunchDecision.ABORTED_WORMHOLE_OFFLINE
					: WormholeLinkCalculator.LaunchDecision.ABORTED_NO_LINK;
			}

			float targetYaw = computeYawFacingTarget(this.getX(), this.getZ(), openCore.getX() + 0.5, openCore.getZ() + 0.5);
			this.setYRot(targetYaw);
			this.yRotO = targetYaw;
			this.lockedWormholeCorePos = openCore;
			this.countdownForExoplanet = false;
			this.launchOriginY = this.getY();
			this.countdownTicks = 0;
			this.launchTicks = 0;
			this.entityData.set(DATA_COUNTDOWN_TICKS, 0);
			this.entityData.set(DATA_MODE, MODE_COUNTDOWN);
			this.entityData.set(DATA_THRUST, 0.30f);
			sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_COUNTDOWN_PING, 0.65f, 1.0f);
			pilot.displayClientMessage(
				Component.translatable("message.galacticpurpose.ship_runway_countdown_started")
					.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				false
			);
			return WormholeLinkCalculator.LaunchDecision.READY;
		}

		WormholeLinkCalculator.LinkStatus liveStatus = WormholeLinkRegistry.get(serverLevel.getServer())
			.queryLiveStatus(serverLevel.getServer(), this.originDimId, this.originCorePos);
		this.entityData.set(DATA_LINK_STATUS, liveStatus.ordinal());

		WormholeLinkCalculator.LaunchDecision decision =
			WormholeLinkCalculator.evaluateOrbitalLaunch(true, liveStatus, fuelUnits());

		if (decision == WormholeLinkCalculator.LaunchDecision.ABORTED_WORMHOLE_OFFLINE) {
			sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_ABORT_ALARM, 0.75f, 0.90f);
			pilot.displayClientMessage(
				Component.translatable("message.galacticpurpose.ship_abort_wormhole_offline")
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				false
			);
			return decision;
		}
		if (decision == WormholeLinkCalculator.LaunchDecision.ABORTED_NO_LINK) {
			sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_ABORT_ALARM, 0.72f, 0.95f);
			pilot.displayClientMessage(
				Component.translatable("message.galacticpurpose.ship_abort_no_link")
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				false
			);
			return decision;
		}
		if (decision == WormholeLinkCalculator.LaunchDecision.ABORTED_NO_FUEL) {
			sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_ABORT_ALARM, 0.72f, 1.05f);
			pilot.displayClientMessage(
				Component.translatable("message.galacticpurpose.ship_abort_no_fuel", WormholeLinkCalculator.REQUIRED_ORBITAL_LAUNCH_FUEL)
					.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				false
			);
			return decision;
		}

		this.countdownForExoplanet = true;
		this.launchOriginY = this.getY();
		this.countdownTicks = 0;
		this.ascentTicks = 0;
		this.entityData.set(DATA_COUNTDOWN_TICKS, 0);
		this.entityData.set(DATA_ASCENT_TICKS, 0);
		this.entityData.set(DATA_MODE, MODE_COUNTDOWN);
		this.entityData.set(DATA_THRUST, 0.30f);
		sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_COUNTDOWN_PING, 0.65f, 1.0f);
		pilot.displayClientMessage(
			Component.translatable("message.galacticpurpose.ship_launch_ignited")
				.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
			false
		);
		return decision;
	}

	public int relocateSubPhase() {
		return this.relocateSubPhase;
	}

	public boolean tryStartRelocationFlight(ServerPlayer commander, double targetX, double targetY, double targetZ) {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return false;
		}
		if (shipMode() != MODE_PARKED) {
			if (commander != null) {
				commander.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_relocate_busy").withStyle(ChatFormatting.YELLOW),
					true
				);
			}
			return false;
		}

		double dx = targetX - this.getX();
		double dy = targetY - this.getY();
		double dz = targetZ - this.getZ();
		double horizDist = Math.hypot(dx, dz);
		double totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (horizDist < 6.0) {
			if (commander != null) {
				sendSoundEvent(commander, ClientSoundEventPayload.EVENT_ABORT_ALARM, 0.65f, 1.0f);
				commander.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_relocate_too_close").withStyle(ChatFormatting.GOLD),
					false
				);
			}
			return false;
		}
		if (totalDist > WormholeLinkCalculator.MAX_RELOCATION_DISTANCE) {
			if (commander != null) {
				sendSoundEvent(commander, ClientSoundEventPayload.EVENT_ABORT_ALARM, 0.70f, 0.95f);
				commander.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_relocate_too_far", (int) WormholeLinkCalculator.MAX_RELOCATION_DISTANCE)
						.withStyle(ChatFormatting.RED),
					false
				);
			}
			return false;
		}

		int fuelCost = WormholeLinkCalculator.computeRelocationFuelCost(totalDist);
		if (fuelUnits() < fuelCost) {
			if (commander != null) {
				sendSoundEvent(commander, ClientSoundEventPayload.EVENT_ABORT_ALARM, 0.72f, 1.05f);
				commander.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_relocate_no_fuel", fuelCost, fuelUnits())
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					false
				);
			}
			return false;
		}

		double maxCorridorY = Math.max(this.getY(), targetY);
		for (int s = 1; s <= 6; s++) {
			double t = s / 7.0;
			int sx = Mth.floor(Mth.lerp(t, this.getX(), targetX));
			int sz = Mth.floor(Mth.lerp(t, this.getZ(), targetZ));
			serverLevel.getChunk(sx >> 4, sz >> 4);
			maxCorridorY = Math.max(maxCorridorY, serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz));
		}

		this.relocatePeakY = Math.max(
			Math.max(this.getY(), targetY) + Mth.clamp(horizDist * 0.28, 12.0, 44.0),
			maxCorridorY + 14.0
		);
		float initYaw = computeYawFacingTarget(this.getX(), this.getZ(), targetX, targetZ);
		float initYawErr = Mth.wrapDegrees(initYaw - this.getYRot());
		this.relocateTurnSign = initYawErr >= 0.0f ? 1 : -1;
		double minBrake = Math.abs(initYawErr) > 100.0f ? 18.0 : Math.min(18.0, horizDist * 0.52);
		this.relocateBrakeDist = Mth.clamp(horizDist * 0.44, minBrake, 38.0);
		this.relocateTargetX = targetX;
		this.relocateTargetY = targetY;
		this.relocateTargetZ = targetZ;
		this.targetLandingX = targetX;
		this.targetLandingY = targetY;
		this.targetLandingZ = targetZ;
		this.hasLandingTarget = true;
		this.launchOriginY = this.getY();
		this.relocateSubPhase = 0;
		this.relocateTicks = 0;
		this.relocateFuelTotal = fuelCost;
		this.relocateFuelBurned = 0;
		this.relocateInitialDist = Math.max(1.0, horizDist);
		this.relocateFwdSpeed = 0.0;
		this.relocateVertSpeed = 0.0;
		this.relocateCommanderId = commander != null ? commander.getUUID() : null;

		this.yRotO = this.getYRot();
		this.entityData.set(DATA_MODE, MODE_RELOCATING);
		this.entityData.set(DATA_THRUST, 0.25f);
		this.entityData.set(DATA_PITCH_DEG, 0.0f);
		this.entityData.set(DATA_ROLL_DEG, 0.0f);
		this.entityData.set(DATA_VECTOR_ANGLE, 0.0f);
		keepCorridorChunksEntityTicking(serverLevel, this.getX(), this.getZ());
		keepCorridorChunksEntityTicking(serverLevel, targetX, targetZ);
		float initYawRad = (float) Math.toRadians(this.getYRot());
		double turnClearX = this.getX() - Math.sin(initYawRad) * 28.0;
		double turnClearZ = this.getZ() + Math.cos(initYawRad) * 28.0;
		keepCorridorChunksEntityTicking(serverLevel, turnClearX, turnClearZ);

		if (commander != null) {
			sendSoundEvent(commander, ClientSoundEventPayload.EVENT_AUTOPILOT_ENGAGE, 0.68f, 1.08f);
			commander.displayClientMessage(
				Component.translatable(
					"message.galacticpurpose.ship_relocate_launched",
					Mth.floor(targetX), Mth.floor(targetY), Mth.floor(targetZ), fuelCost
				).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				false
			);
		}
		return true;
	}

	@Override
	public void tick() {
		this.clientPitchO = pitchDeg();
		this.clientRollO = rollDeg();
		super.tick();
		this.resetFallDistance();

		if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
			intakeFuelAndEnergySlots();
			if (serverLevel.getGameTime() % 20L == 0L) {
				WormholeLinkCalculator.LinkStatus st = WormholeLinkRegistry.get(serverLevel.getServer())
					.queryLiveStatus(serverLevel.getServer(), this.originDimId, this.originCorePos);
				this.entityData.set(DATA_LINK_STATUS, st.ordinal());
			}

			int mode = shipMode();
			if (mode == MODE_AUTO_DESCENT) {
				tickAutoDescent(serverLevel);
			} else if (mode == MODE_COUNTDOWN) {
				tickCountdown(serverLevel);
			} else if (mode == MODE_RUNWAY_LAUNCH) {
				tickRunwayLaunch(serverLevel);
			} else if (mode == MODE_ORBITAL_ASCENT) {
				tickOrbitalAscent(serverLevel);
			} else if (mode == MODE_RELOCATING) {
				tickRelocating(serverLevel);
			} else {
				stepParkedGravityAndBlast(serverLevel);
			}
		}
	}

	public void applyBlastImpulse(Vec3 impulse, float pitchKick, float rollKick) {
		if (this.level().isClientSide || this.level().dimension().equals(OrbitalTransitManager.ORBITAL_TRANSIT_KEY)) {
			return;
		}
		this.extVelX = Mth.clamp(this.extVelX + impulse.x, -1.8, 1.8);
		this.extVelY = Mth.clamp(this.extVelY + impulse.y, -1.8, 1.8);
		this.extVelZ = Mth.clamp(this.extVelZ + impulse.z, -1.8, 1.8);
		this.entityData.set(DATA_PITCH_DEG, Mth.clamp(pitchDeg() + pitchKick, -42.0f, 42.0f));
		this.entityData.set(DATA_ROLL_DEG, Mth.clamp(rollDeg() + rollKick, -55.0f, 55.0f));
		this.hurtMarked = true;
	}

	private double findParkedSupportFloorY(ServerLevel level, double baseX, double baseZ, double scanTopY, double scanBotY) {
		double[][] pts = {{0.0, 0.0}, {-1.85, -1.85}, {1.85, -1.85}, {-1.85, 1.85}, {1.85, 1.85}};
		double highest = Double.NEGATIVE_INFINITY;
		int yTop = Mth.floor(scanTopY + 0.15);
		int yBot = Math.max(level.getMinBuildHeight(), Mth.floor(scanBotY - 0.35));
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		for (double[] pt : pts) {
			int bx = Mth.floor(baseX + pt[0]);
			int bz = Mth.floor(baseZ + pt[1]);
			for (int by = yTop; by >= yBot; by--) {
				mpos.set(bx, by, bz);
				net.minecraft.world.level.block.state.BlockState st = level.getBlockState(mpos);
				if (!st.getFluidState().isEmpty()) {
					highest = Math.max(highest, by + 0.90);
					break;
				}
				var shape = st.getCollisionShape(level, mpos);
				if (!shape.isEmpty()) {
					highest = Math.max(highest, by + shape.max(net.minecraft.core.Direction.Axis.Y));
					break;
				}
			}
		}
		return highest;
	}

	private void stepParkedGravityAndBlast(ServerLevel level) {
		this.entityData.set(DATA_LEG_DEPLOY, Math.min(1.0f, legDeploy() + 0.08f));
		this.entityData.set(DATA_THRUST, 0.0f);
		this.entityData.set(DATA_GROUND_ALT, 0.0f);

		if (level.dimension().equals(OrbitalTransitManager.ORBITAL_TRANSIT_KEY)) {
			this.setDeltaMovement(Vec3.ZERO);
			this.entityData.set(DATA_PITCH_DEG, Mth.lerp(0.2f, pitchDeg(), 0.0f));
			this.entityData.set(DATA_ROLL_DEG, Mth.lerp(0.2f, rollDeg(), 0.0f));
			return;
		}

		double immediateFloor = findParkedSupportFloorY(level, this.getX(), this.getZ(), this.getY(), this.getY() - 0.12);
		boolean hasImpulse = Math.abs(this.extVelX) > 0.012 || Math.abs(this.extVelY) > 0.012 || Math.abs(this.extVelZ) > 0.012;
		boolean unsupported = immediateFloor < this.getY() - 0.16;

		if (!unsupported && !hasImpulse) {
			this.extVelX = 0.0;
			this.extVelY = 0.0;
			this.extVelZ = 0.0;
			this.setDeltaMovement(Vec3.ZERO);
			this.entityData.set(DATA_PITCH_DEG, Mth.lerp(0.2f, pitchDeg(), 0.0f));
			this.entityData.set(DATA_ROLL_DEG, Mth.lerp(0.2f, rollDeg(), 0.0f));
			return;
		}

		double gravMul = Mth.clamp(
			com.galacticpurpose.exoplanet.ExoplanetGravityCalculator.evaluate(level.dimension().location(), level.getDayTime()).effectiveG(),
			0.35, 1.85
		);
		this.extVelY = Math.max(-1.85, this.extVelY - 0.055 * gravMul);
		this.extVelX *= 0.95;
		this.extVelZ *= 0.95;

		double nx = this.getX() + this.extVelX;
		double ny = this.getY() + this.extVelY;
		double nz = this.getZ() + this.extVelZ;
		double sweptFloor = findParkedSupportFloorY(level, nx, nz, this.getY() + 0.15, ny);

		if (this.extVelY <= 0.0 && sweptFloor >= ny) {
			double impactSpeed = -this.extVelY;
			this.setPos(nx, sweptFloor, nz);
			this.extVelX *= 0.35;
			this.extVelZ *= 0.35;
			this.extVelY = 0.0;
			this.setDeltaMovement(this.extVelX, 0.0, this.extVelZ);
			this.entityData.set(DATA_PITCH_DEG, Mth.lerp(0.30f, pitchDeg(), 0.0f));
			this.entityData.set(DATA_ROLL_DEG, Mth.lerp(0.30f, rollDeg(), 0.0f));
			if (impactSpeed > 0.28) {
				level.playSound(null, this.blockPosition(), SoundEvents.NETHERITE_BLOCK_FALL, SoundSource.PLAYERS, 1.8f, 0.72f);
				level.playSound(null, this.blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 1.4f, 0.75f);
			}
		} else {
			this.setPos(nx, ny, nz);
			this.setDeltaMovement(this.extVelX, this.extVelY, this.extVelZ);
			this.entityData.set(DATA_PITCH_DEG, Mth.lerp(0.06f, pitchDeg(), Mth.clamp((float) (this.extVelY * 14.0), -18.0f, 14.0f)));
			this.entityData.set(DATA_ROLL_DEG, Mth.lerp(0.06f, rollDeg(), 0.0f));
		}
	}

	private void intakeFuelAndEnergySlots() {
		ItemStack fuelSlot = this.inventory.getItem(27);
		if (fuelSlot.getItem() instanceof StarshipFuelCellItem && fuelUnits() <= MAX_FUEL - StarshipFuelCellItem.FUEL_VALUE) {
			fuelSlot.shrink(1);
			addFuel(StarshipFuelCellItem.FUEL_VALUE);
		}
		ItemStack energySlot = this.inventory.getItem(28);
		if (!energySlot.isEmpty() && this.energyStored < MAX_ENERGY) {
			if (energySlot.is(ModItems.CREATIVE_ENERGY_CELL)) {
				this.energyStored = MAX_ENERGY;
				this.entityData.set(DATA_ENERGY_K, (int) (this.energyStored / 1000L));
			} else if (energySlot.getItem() instanceof LeadEnergyCellItem cell) {
				long avail = cell.getStoredEnergy(energySlot);
				long move = Math.min(avail, Math.min(10_000L, MAX_ENERGY - this.energyStored));
				if (move > 0L) {
					cell.setStoredEnergy(energySlot, avail - move);
					this.energyStored += move;
					this.entityData.set(DATA_ENERGY_K, (int) (this.energyStored / 1000L));
				}
			}
		}
	}

	private void tickCountdown(ServerLevel level) {
		this.countdownTicks++;
		this.entityData.set(DATA_COUNTDOWN_TICKS, this.countdownTicks);
		float spool = Mth.clamp(this.countdownTicks / (float) StarshipFlightPhysics.COUNTDOWN_TICKS, 0.0f, 1.0f);
		this.entityData.set(DATA_THRUST, 0.20f + spool * 0.65f);
		this.entityData.set(DATA_GROUND_ALT, 0.0f);
		this.setDeltaMovement(Vec3.ZERO);

		if (!(this.getFirstPassenger() instanceof ServerPlayer pilot)) {
			this.entityData.set(DATA_MODE, MODE_PARKED);
			this.entityData.set(DATA_THRUST, 0.0f);
			this.countdownTicks = 0;
			this.entityData.set(DATA_COUNTDOWN_TICKS, 0);
			return;
		}

		if (this.countdownTicks < StarshipFlightPhysics.COUNTDOWN_TICKS && this.countdownTicks % 20 == 0) {
			int secLeft = StarshipFlightPhysics.countdownSecondsRemaining(this.countdownTicks);
			float pitch = secLeft <= 3 ? 1.18f : 1.0f;
			sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_COUNTDOWN_PING, 0.64f, pitch);
			pilot.displayClientMessage(
				Component.translatable("message.galacticpurpose.ship_countdown_tick", secLeft)
					.withStyle(secLeft <= 3 ? ChatFormatting.GOLD : ChatFormatting.AQUA, ChatFormatting.BOLD),
				true
			);
		}

		if (this.countdownTicks >= StarshipFlightPhysics.COUNTDOWN_TICKS) {
			sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_IGNITION_GO, 0.72f, 1.0f);
			this.entityData.set(DATA_THRUST, 1.0f);
			if (this.countdownForExoplanet) {
				this.ascentTicks = 0;
				this.ascentFuelBurned = 0;
				this.entityData.set(DATA_ASCENT_TICKS, 0);
				this.entityData.set(DATA_MODE, MODE_ORBITAL_ASCENT);
				pilot.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_ascent_stage1")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					false
				);
			} else {
				this.launchTicks = 0;
				this.entityData.set(DATA_MODE, MODE_RUNWAY_LAUNCH);
				pilot.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_runway_liftoff")
						.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
					true
				);
			}
		}
	}

	private void tickRunwayLaunch(ServerLevel level) {
		this.launchTicks++;
		this.entityData.set(DATA_THRUST, 1.0f);
		this.entityData.set(DATA_LEG_DEPLOY, Math.max(0.0f, legDeploy() - 0.10f));
		this.entityData.set(DATA_GROUND_ALT, (float) Math.max(0.0, this.getY() - this.launchOriginY));

		if (!(this.getFirstPassenger() instanceof ServerPlayer pilot)) {
			this.entityData.set(DATA_MODE, MODE_PARKED);
			this.entityData.set(DATA_THRUST, 0.0f);
			return;
		}

		BlockPos targetCore = this.lockedWormholeCorePos;
		WormholeCoreBlockEntity activeCore = null;
		if (targetCore != null && level.getBlockEntity(targetCore) instanceof WormholeCoreBlockEntity be
			&& be.wormholeState() == WormholeState.OPEN && be.activeTier() == WormholeTier.COLOSSAL) {
			activeCore = be;
		} else {
			BlockPos refound = findNearestColossalWormhole(level, this.blockPosition(), 7, true);
			if (refound != null && level.getBlockEntity(refound) instanceof WormholeCoreBlockEntity be) {
				targetCore = refound;
				this.lockedWormholeCorePos = refound;
				activeCore = be;
			}
		}

		double speed = StarshipFlightPhysics.computeRunwayLaunchSpeed(this.launchTicks);
		Vec3 moveVec;
		if (activeCore != null && targetCore != null) {
			Vec3 center = Vec3.atCenterOf(targetCore);
			Vec3 toCore = center.subtract(this.position());
			double dist = toCore.length();
			if (dist <= activeCore.activeTier().coreRadius() + 4.5) {
				ExoplanetRoute route = activeCore.selectedRoute();
				if (route != null) {
					OrbitalTransitManager.beginOutboundTransit(
						pilot, this, route, level.dimension().location(), targetCore
					);
					return;
				}
			}

			// First 12 ticks accelerate flat along the unobstructed diagonal runway, then smoothly pitch nose up straight into the Wormhole sphere!
			double horizDist = Math.max(0.01, Math.hypot(toCore.x, toCore.z));
			float desiredYaw = computeYawFacingTarget(this.getX(), this.getZ(), center.x, center.z);
			this.setYRot(Mth.rotLerp(0.25f, this.getYRot(), desiredYaw));
			float climbBlend = Mth.clamp((this.launchTicks - 10) / 16.0f, 0.0f, 1.0f);
			double desiredDy = (toCore.y / Math.max(dist, 0.1)) * climbBlend;
			double horizScale = Math.sqrt(Math.max(0.05, 1.0 - desiredDy * desiredDy));
			Vec3 dir = new Vec3((toCore.x / horizDist) * horizScale, desiredDy, (toCore.z / horizDist) * horizScale).normalize();
			moveVec = dir.scale(speed);
			float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(dir.y, -0.5, 0.78)));
			this.entityData.set(DATA_PITCH_DEG, Mth.lerp(0.24f, pitchDeg(), pitch));
		} else {
			if (this.launchTicks > 30) {
				this.entityData.set(DATA_MODE, MODE_PARKED);
				this.entityData.set(DATA_THRUST, 0.0f);
				this.entityData.set(DATA_PITCH_DEG, 0.0f);
				this.setDeltaMovement(Vec3.ZERO);
				pilot.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_no_wormhole_ahead").withStyle(ChatFormatting.RED),
					false
				);
				return;
			}
			float yawRad = (float) Math.toRadians(this.getYRot());
			moveVec = new Vec3(-Math.sin(yawRad) * speed, 0.08, Math.cos(yawRad) * speed);
		}

		this.setPos(this.getX() + moveVec.x, this.getY() + moveVec.y, this.getZ() + moveVec.z);
		this.setDeltaMovement(moveVec);
	}

	/**
	 * True Aerodynamic Spaceplane Orbital Climb (45 seconds, 900 ticks):
	 * - Accelerates smoothly forward along nose yaw/pitch vector (x^1.5 speed, x^1.2 pitch curve)
	 * - Zero Y=320 ceiling clamp — climbs smoothly above Y=1000+ until the entire planet shrinks below!
	 */
	private void tickOrbitalAscent(ServerLevel level) {
		this.ascentTicks++;
		this.entityData.set(DATA_ASCENT_TICKS, this.ascentTicks);
		this.entityData.set(DATA_THRUST, 1.0f);
		this.entityData.set(DATA_LEG_DEPLOY, Math.max(0.0f, legDeploy() - 0.06f));
		this.entityData.set(DATA_ROLL_DEG, Mth.lerp(0.15f, rollDeg(), 0.0f));

		int desiredAscentBurn = Math.min(200, Math.round(200.0f * Mth.clamp(
			this.ascentTicks / (float) StarshipFlightPhysics.ASCENT_DURATION_TICKS, 0.0f, 1.0f)));
		if (desiredAscentBurn > this.ascentFuelBurned) {
			int stepBurn = Math.min(fuelUnits(), desiredAscentBurn - this.ascentFuelBurned);
			if (stepBurn > 0) {
				this.entityData.set(DATA_FUEL, fuelUnits() - stepBurn);
				this.ascentFuelBurned += stepBurn;
			}
		}

		StarshipFlightPhysics.AscentReading reading = StarshipFlightPhysics.computeAscentProfile(this.ascentTicks);
		float targetPitch = reading.pitchDeg();

		// Early terrain clearance safety: if rolling forward near a ridge during the first 8 seconds, smoothly pull pitch up!
		float yawRad = (float) Math.toRadians(this.getYRot());
		double fwdX = -Math.sin(yawRad);
		double fwdZ = Math.cos(yawRad);
		if (this.ascentTicks < 160) {
			int aheadX = Mth.floor(this.getX() + fwdX * 14.0);
			int aheadZ = Mth.floor(this.getZ() + fwdZ * 14.0);
			int ridgeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, aheadX, aheadZ);
			if (ridgeY + 6.0 > this.getY()) {
				targetPitch = Math.max(targetPitch, 22.0f);
			}
		}

		float smoothPitch = Mth.lerp(0.24f, pitchDeg(), targetPitch);
		this.entityData.set(DATA_PITCH_DEG, smoothPitch);
		this.entityData.set(DATA_VECTOR_ANGLE, 0.0f);

		// Move STRICTLY along the spacecraft's 3D nose-forward vector!
		double pitchRad = Math.toRadians(smoothPitch);
		double speed = reading.forwardSpeed();
		double vx = fwdX * Math.cos(pitchRad) * speed;
		double vy = Math.sin(pitchRad) * speed;
		double vz = fwdZ * Math.cos(pitchRad) * speed;

		// Gentle ground clearance lift during very first ~1.5 seconds of forward speed build-up past 15 m/s
		if (this.ascentTicks < 35) {
			vy = Math.max(vy, 0.06);
		}

		this.setPos(this.getX() + vx, this.getY() + vy, this.getZ() + vz);
		this.setDeltaMovement(vx, vy, vz);
		this.entityData.set(DATA_GROUND_ALT, (float) Math.max(0.0, this.getY() - this.launchOriginY));

		if (this.getFirstPassenger() instanceof ServerPlayer pilot) {
			if (this.ascentTicks == 200) {
				pilot.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_ascent_stage2")
						.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
					false
				);
			} else if (this.ascentTicks == 610) {
				pilot.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_ascent_stage3")
						.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
					false
				);
			}
		}

		if (this.ascentTicks >= StarshipFlightPhysics.ASCENT_DURATION_TICKS) {
			if (this.getFirstPassenger() instanceof ServerPlayer pilot) {
				ExoplanetRoute departed = ExoplanetRoute.fromDimensionId(level.dimension().location());
				if (departed != null) {
					OrbitalTransitManager.beginReturnTransit(pilot, this, departed);
				}
			}
		}
	}

	private static void keepCorridorChunksEntityTicking(ServerLevel level, double x, double z) {
		ChunkPos chunkPos = new ChunkPos(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
		level.getChunkSource().addRegionTicket(RELOCATE_TICKET, chunkPos, 4, chunkPos);
		level.getChunk(chunkPos.x, chunkPos.z);
	}

	private void consumeRelocateFuelProgress(int targetBurn) {
		int clamped = Math.min(this.relocateFuelTotal, targetBurn);
		if (clamped > this.relocateFuelBurned) {
			int step = Math.min(fuelUnits(), clamped - this.relocateFuelBurned);
			if (step > 0) {
				this.entityData.set(DATA_FUEL, fuelUnits() - step);
				this.relocateFuelBurned += step;
			}
		}
	}

	private void tickRelocating(ServerLevel level) {
		keepCorridorChunksEntityTicking(level, this.getX(), this.getZ());
		keepCorridorChunksEntityTicking(level, this.relocateTargetX, this.relocateTargetZ);
		this.relocateTicks++;

		if (this.relocateSubPhase == 0) {
			float yawRad = (float) Math.toRadians(this.getYRot());
			keepCorridorChunksEntityTicking(level, this.getX() - Math.sin(yawRad) * 24.0, this.getZ() + Math.cos(yawRad) * 24.0);
			float spool = Mth.clamp(this.relocateTicks / 60.0f, 0.0f, 1.0f);
			this.entityData.set(DATA_THRUST, 0.22f + spool * 0.35f);
			this.entityData.set(DATA_LEG_DEPLOY, 1.0f);
			this.entityData.set(DATA_PITCH_DEG, 0.0f);
			this.entityData.set(DATA_ROLL_DEG, 0.0f);
			this.entityData.set(DATA_VECTOR_ANGLE, 0.0f);
			this.entityData.set(DATA_GROUND_ALT, 0.0f);
			this.setDeltaMovement(Vec3.ZERO);
			consumeRelocateFuelProgress(Math.round(this.relocateFuelTotal * 0.08f * spool));

			if (this.relocateTicks >= 60) {
				this.relocateSubPhase = 1;
				this.relocateTicks = 0;
				ServerPlayer notify = resolveRelocateNotifyPlayer(level);
				if (notify != null) {
					sendSoundEvent(notify, ClientSoundEventPayload.EVENT_IGNITION_GO, 0.72f, 1.02f);
				}
			}
			return;
		}

		double dx = this.relocateTargetX - this.getX();
		double dz = this.relocateTargetZ - this.getZ();
		double horizDist = Math.hypot(dx, dz);

		// First 14 ticks: takeoff roll & rotation straight along parked heading; AFTER lift-off, perform smooth GTA-5 banking turn along a natural curved arc!
		float rawErr = horizDist > 2.0
			? Mth.wrapDegrees(computeYawFacingTarget(this.getX(), this.getZ(), this.relocateTargetX, this.relocateTargetZ) - this.getYRot())
			: 0.0f;
		float desiredRoll = 0.0f;
		if (this.relocateTicks > 14 && horizDist > 2.5) {
			float steerErr = Math.abs(rawErr) > 158.0f
				? (float) (this.relocateTurnSign * Math.abs(rawErr))
				: rawErr;
			float maxTurn = Mth.clamp(2.2f + Math.abs(steerErr) * 0.025f, 2.2f, 3.8f);
			float deltaYaw = Mth.clamp(steerErr, -maxTurn, maxTurn);
			this.setYRot(Mth.wrapDegrees(this.getYRot() + deltaYaw));
			desiredRoll = Mth.clamp(steerErr * 0.90f, -42.0f, 42.0f);
		}
		this.entityData.set(DATA_ROLL_DEG, Mth.lerp(0.18f, rollDeg(), desiredRoll));

		float yawRad = (float) Math.toRadians(this.getYRot());
		double fwdX = -Math.sin(yawRad);
		double fwdZ = Math.cos(yawRad);
		keepCorridorChunksEntityTicking(level, this.getX() + fwdX * 24.0, this.getZ() + fwdZ * 24.0);

		double takeOffRoll = Mth.clamp(this.relocateTicks / 18.0, 0.0, 1.0);
		double cruiseMax = Mth.clamp(Math.max(horizDist, Math.abs(rawErr) * 0.28) * 0.034, 0.56, 1.45);
		this.relocateFwdSpeed = this.relocateTicks < 18
			? Math.max(0.24, 0.24 + (Math.min(0.85, cruiseMax) - 0.24) * Math.pow(takeOffRoll, 1.10))
			: Mth.lerp(0.14, this.relocateFwdSpeed, cruiseMax);

		double altNeeded = this.relocatePeakY - this.getY();
		float targetPitch;
		if (this.relocateTicks < 14) {
			targetPitch = 6.0f * (float) (takeOffRoll * takeOffRoll);
		} else if (altNeeded > 2.0) {
			targetPitch = Mth.clamp((float) (altNeeded * 1.40), 10.0f, 32.0f);
		} else {
			targetPitch = Mth.clamp((float) (altNeeded * 1.15), -6.0f, 8.0f);
		}
		float smoothPitch = Mth.lerp(0.20f, pitchDeg(), targetPitch);
		this.entityData.set(DATA_PITCH_DEG, smoothPitch);
		boolean aligned = Math.abs(rawErr) <= 34.0f;
		float preFlareVector = (aligned && horizDist < this.relocateBrakeDist + 6.0)
			? Mth.lerp(0.25f, syncedVectorAngle(), 78.0f)
			: Mth.lerp(0.20f, syncedVectorAngle(), 0.0f);
		this.entityData.set(DATA_VECTOR_ANGLE, preFlareVector);
		this.entityData.set(DATA_THRUST, 1.0f);
		this.entityData.set(DATA_LEG_DEPLOY, Math.max(0.0f, legDeploy() - 0.08f));

		double pitchRad = Math.toRadians(smoothPitch);
		double horizSpeed = Math.cos(pitchRad) * this.relocateFwdSpeed;
		this.relocateVertSpeed = Math.sin(pitchRad) * this.relocateFwdSpeed;
		if (this.relocateTicks >= 8 && this.relocateTicks < 24) {
			this.relocateVertSpeed = Math.max(this.relocateVertSpeed, 0.14);
		}
		double vx = fwdX * horizSpeed;
		double vz = fwdZ * horizSpeed;
		this.setPos(this.getX() + vx, this.getY() + this.relocateVertSpeed, this.getZ() + vz);
		this.setDeltaMovement(vx, this.relocateVertSpeed, vz);
		this.entityData.set(DATA_GROUND_ALT, (float) Math.max(0.0, this.getY() - this.targetLandingY));

		double remainingDx = this.relocateTargetX - this.getX();
		double remainingDz = this.relocateTargetZ - this.getZ();
		double remainingHoriz = Math.hypot(remainingDx, remainingDz);
		double forwardDot = remainingDx * fwdX + remainingDz * fwdZ;

		float flightFrac = 1.0f - Mth.clamp((float) (remainingHoriz / Math.max(1.0, this.relocateInitialDist)), 0.0f, 1.0f);
		consumeRelocateFuelProgress(Math.round(this.relocateFuelTotal * (0.08f + 0.72f * flightFrac)));

		if ((aligned && this.relocateTicks >= 20 && remainingHoriz <= this.relocateBrakeDist)
			|| (aligned && this.relocateTicks >= 16 && forwardDot <= 1.0)
			|| this.relocateTicks >= 220) {
			level.getChunk(Mth.floor(this.relocateTargetX) >> 4, Mth.floor(this.relocateTargetZ) >> 4);
			double norm = Math.max(0.01, remainingHoriz);
			this.landingDirX = norm > 0.05 ? (remainingDx / norm) : fwdX;
			this.landingDirZ = norm > 0.05 ? (remainingDz / norm) : fwdZ;
			this.landingFwdSpeed = Math.min(horizSpeed, Math.max(0.38, remainingHoriz * 0.055));
			this.landingVertSpeed = Math.min(-0.18, this.relocateVertSpeed * 0.45);
			if (syncedVectorAngle() < 78.0f) {
				this.entityData.set(DATA_VECTOR_ANGLE, 82.0f);
			}
			this.targetLandingX = this.relocateTargetX;
			this.targetLandingY = this.relocateTargetY;
			this.targetLandingZ = this.relocateTargetZ;
			this.hasLandingTarget = true;
			boolean waterTarget = !level.getFluidState(BlockPos.containing(this.relocateTargetX, this.relocateTargetY - 0.5, this.relocateTargetZ)).isEmpty();
			this.entityData.set(DATA_HYDRO_PONTOON, waterTarget);
			this.entityData.set(DATA_MODE, MODE_AUTO_DESCENT);
		}
	}

	private ServerPlayer resolveRelocateNotifyPlayer(ServerLevel level) {
		if (this.getFirstPassenger() instanceof ServerPlayer pilot) {
			return pilot;
		}
		if (this.relocateCommanderId != null) {
			return level.getServer().getPlayerList().getPlayer(this.relocateCommanderId);
		}
		return null;
	}

	private void tickAutoDescent(ServerLevel level) {
		keepCorridorChunksEntityTicking(level, this.getX(), this.getZ());
		double alt = Math.max(0.0, this.getY() - this.targetLandingY);
		this.entityData.set(DATA_GROUND_ALT, (float) alt);

		double tx = this.hasLandingTarget ? this.targetLandingX : this.getX();
		double tz = this.hasLandingTarget ? this.targetLandingZ : this.getZ();
		double dx = tx - this.getX();
		double dz = tz - this.getZ();
		double signedDist = dx * this.landingDirX + dz * this.landingDirZ;
		double horizDist = Math.hypot(dx, dz);

		StarshipFlightPhysics.DynamicLandingStep step = StarshipFlightPhysics.stepDynamicLanding(
			signedDist,
			this.landingFwdSpeed,
			this.landingVertSpeed,
			alt,
			pitchDeg(),
			syncedVectorAngle()
		);
		this.landingFwdSpeed = step.nextForwardSpeed();
		this.landingVertSpeed = step.nextVerticalSpeed();

		this.entityData.set(DATA_PITCH_DEG, step.pitchDeg());
		this.entityData.set(DATA_ROLL_DEG, Mth.lerp(0.18f, rollDeg(), 0.0f));
		this.entityData.set(DATA_VECTOR_ANGLE, step.vectorAngleDeg());
		this.entityData.set(DATA_THRUST, step.landingPhase() == 0 ? 0.42f : (step.landingPhase() == 1 ? 0.96f : 0.56f));
		this.entityData.set(
			DATA_LEG_DEPLOY,
			step.skidsExtended() ? Math.min(1.0f, legDeploy() + 0.09f) : Math.max(0.0f, legDeploy() - 0.05f)
		);
		if (this.relocateFuelTotal > 0) {
			float altDropFrac = 1.0f - Mth.clamp((float) (alt / 22.0), 0.0f, 1.0f);
			consumeRelocateFuelProgress(Math.round(this.relocateFuelTotal * (0.80f + 0.20f * altDropFrac)));
		}

		// Touchdown condition: skids kissed ground and horizontal PD damping settled over LZ
		if (alt <= 0.16 && horizDist <= 3.5 && Math.abs(this.landingFwdSpeed) <= 0.14) {
			if (this.relocateFuelTotal > 0) {
				consumeRelocateFuelProgress(this.relocateFuelTotal);
				this.relocateFuelTotal = 0;
				this.relocateFuelBurned = 0;
			}
			this.setPos(tx, this.targetLandingY, tz);
			this.setDeltaMovement(Vec3.ZERO);
			this.hasLandingTarget = false;
			this.landingFwdSpeed = 0.0;
			this.landingVertSpeed = 0.0;
			this.entityData.set(DATA_MODE, MODE_PARKED);
			this.entityData.set(DATA_LEG_DEPLOY, 1.0f);
			this.entityData.set(DATA_THRUST, 0.0f);
			this.entityData.set(DATA_PITCH_DEG, 0.0f);
			this.entityData.set(DATA_ROLL_DEG, 0.0f);
			this.entityData.set(DATA_VECTOR_ANGLE, 0.0f);
			this.entityData.set(DATA_GROUND_ALT, 0.0f);
			if (this.getFirstPassenger() instanceof ServerPlayer pilot) {
				sendSoundEvent(pilot, ClientSoundEventPayload.EVENT_HYDRAULIC_TOUCHDOWN, 0.72f, 1.0f);
				ExoplanetRoute planet = ExoplanetRoute.fromDimensionId(level.dimension().location());
				if (planet != null) {
					int dist = (int) Math.round(Math.hypot(
						ExoplanetLandmarkBuilder.LANDMARK_X - this.getX(),
						ExoplanetLandmarkBuilder.LANDMARK_Z - this.getZ()
					));
					String dir = StarshipFlightPhysics.computeCompassBearing(
						ExoplanetLandmarkBuilder.LANDMARK_X - this.getX(),
						ExoplanetLandmarkBuilder.LANDMARK_Z - this.getZ()
					);
					pilot.displayClientMessage(
						Component.translatable(
							"message.galacticpurpose.ship_touchdown_exoplanet",
							Component.translatable(planet.landmarkNameKey()),
							dist, dir
						).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
						false
					);
				} else {
					pilot.displayClientMessage(
						Component.translatable("message.galacticpurpose.ship_touchdown")
							.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
						false
					);
				}
			}
			if (this.relocateCommanderId != null) {
				ServerPlayer commander = level.getServer().getPlayerList().getPlayer(this.relocateCommanderId);
				this.relocateCommanderId = null;
				if (commander != null && commander != this.getFirstPassenger()) {
					sendSoundEvent(commander, ClientSoundEventPayload.EVENT_HYDRAULIC_TOUCHDOWN, 0.72f, 1.0f);
					commander.displayClientMessage(
						Component.translatable(
							"message.galacticpurpose.ship_relocate_arrived",
							Mth.floor(tx), Mth.floor(this.targetLandingY), Mth.floor(tz)
						).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
						false
					);
				}
			}
			return;
		}

		// Apply true forward-axis speed plus subtle lateral cross-track centering without flipping heading yaw
		double latX = dx - signedDist * this.landingDirX;
		double latZ = dz - signedDist * this.landingDirZ;
		double vx = this.landingDirX * this.landingFwdSpeed + Mth.clamp(latX * 0.15, -0.25, 0.25);
		double vz = this.landingDirZ * this.landingFwdSpeed + Mth.clamp(latZ * 0.15, -0.25, 0.25);
		double vy = this.landingVertSpeed;

		double nextY = Math.max(this.targetLandingY, this.getY() + vy);
		this.setPos(this.getX() + vx, nextY, this.getZ() + vz);
		this.setDeltaMovement(vx, vy, vz);
	}

	private static void sendSoundEvent(ServerPlayer player, int eventId, float volume, float pitch) {
		ServerPlayNetworking.send(player, new ClientSoundEventPayload(eventId, volume, pitch));
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		// Rotate pilot cockpit seat strictly WITH the 1.9x Horizon Lander's pitch & yaw so Steve NEVER pokes through the hull!
		double scale = 1.90;
		double pivotY = 1.25 * scale;
		double localY = 1.32 * scale - pivotY;
		double localZ = 1.85 * scale;
		double pitchRad = Math.toRadians(pitchDeg());
		double rotY = localY * Math.cos(pitchRad) + localZ * Math.sin(pitchRad);
		double rotZ = -localY * Math.sin(pitchRad) + localZ * Math.cos(pitchRad);

		double yawRad = Math.toRadians(this.getYRot());
		double worldX = -Math.sin(yawRad) * rotZ;
		double worldZ = Math.cos(yawRad) * rotZ;
		return new Vec3(this.getX() + worldX, this.getY() + pivotY + rotY, this.getZ() + worldZ);
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source) || this.level().isClientSide) {
			return false;
		}
		if (source.getEntity() instanceof ServerPlayer sp && sp.isShiftKeyDown()) {
			if (sp.isCreative()) {
				this.discard();
				return true;
			}
			return packUpIntoItem(sp);
		}
		if (source.is(DamageTypeTags.IS_EXPLOSION) && shipMode() == MODE_PARKED) {
			Vec3 src = source.getSourcePosition();
			Vec3 away = src != null ? this.position().subtract(src) : new Vec3(0.0, 1.0, 0.0);
			Vec3 dir = new Vec3(away.x, Math.max(1.2, Math.abs(away.y) + 1.4), away.z).normalize();
			double power = Mth.clamp(amount * 0.08, 0.35, 1.45);
			applyBlastImpulse(
				dir.scale(power),
				(float) ((this.level().random.nextFloat() - 0.4f) * 18.0f),
				(float) ((this.level().random.nextFloat() - 0.5f) * 24.0f)
			);
			return true;
		}
		return false;
	}

	/**
	 * Packs a parked lander cleanly back into a Survival-portable Starship Blueprint item,
	 * dropping cargo contents safely and preserving remaining fuel and flux charge in NBT.
	 */
	public boolean packUpIntoItem(ServerPlayer player) {
		if (this.level().isClientSide || shipMode() != MODE_PARKED) {
			if (player != null) {
				player.displayClientMessage(
					Component.translatable("message.galacticpurpose.ship_pack_only_parked").withStyle(ChatFormatting.YELLOW),
					false
				);
			}
			return false;
		}
		this.ejectPassengers();
		net.minecraft.world.Containers.dropContents(this.level(), this.blockPosition(), this.inventory);
		ItemStack packedBlueprint = new ItemStack(com.galacticpurpose.registry.ModItems.STARSHIP_BLUEPRINT);
		com.galacticpurpose.item.StarshipBlueprintItem.writePackedState(packedBlueprint, fuelUnits(), this.energyStored);
		if (player != null) {
			player.closeContainer();
			player.getInventory().placeItemBackInInventory(packedBlueprint);
			player.displayClientMessage(
				Component.translatable("message.galacticpurpose.ship_packed", fuelUnits()).withStyle(ChatFormatting.AQUA),
				false
			);
		} else {
			this.spawnAtLocation(packedBlueprint);
		}
		this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 1.4f, 0.72f);
		this.discard();
		return true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 480.0 * 480.0;
	}

	public CompoundTag exportCargoTag(HolderLookup.Provider registries) {
		NonNullList<ItemStack> cargo = NonNullList.withSize(27, ItemStack.EMPTY);
		for (int i = 0; i < 27; i++) {
			cargo.set(i, this.inventory.getItem(i).copy());
		}
		CompoundTag tag = new CompoundTag();
		ContainerHelper.saveAllItems(tag, cargo, registries);
		return tag;
	}

	public void importCargoTag(CompoundTag tag, HolderLookup.Provider registries) {
		NonNullList<ItemStack> cargo = NonNullList.withSize(27, ItemStack.EMPTY);
		if (tag != null && !tag.isEmpty()) {
			ContainerHelper.loadAllItems(tag, cargo, registries);
		}
		for (int i = 0; i < 27; i++) {
			this.inventory.setItem(i, cargo.get(i));
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(DATA_MODE, tag.getInt("ShipMode"));
		this.entityData.set(DATA_FUEL, tag.getInt("Fuel"));
		this.energyStored = tag.getLong("Energy");
		this.entityData.set(DATA_ENERGY_K, (int) (this.energyStored / 1000L));
		this.entityData.set(DATA_LEG_DEPLOY, tag.getFloat("LegDeploy"));
		this.entityData.set(DATA_HYDRO_PONTOON, tag.getBoolean("HydroPontoon"));
		this.entityData.set(DATA_PITCH_DEG, tag.getFloat("PitchDeg"));
		this.entityData.set(DATA_ROLL_DEG, tag.getFloat("RollDeg"));
		this.entityData.set(DATA_VECTOR_ANGLE, tag.getFloat("VectorAngle"));
		this.relocateFuelTotal = tag.getInt("RelocateFuelTotal");
		this.relocateFuelBurned = tag.getInt("RelocateFuelBurned");
		this.relocateInitialDist = tag.contains("RelocateInitDist") ? tag.getDouble("RelocateInitDist") : 1.0;
		this.relocateTurnSign = tag.contains("RelocateTurnSign") ? tag.getInt("RelocateTurnSign") : 1;
		this.ascentFuelBurned = tag.getInt("AscentFuelBurned");
		this.extVelX = tag.getDouble("ExtVelX");
		this.extVelY = tag.getDouble("ExtVelY");
		this.extVelZ = tag.getDouble("ExtVelZ");
		this.targetLandingY = tag.getDouble("TargetLandingY");
		this.targetLandingX = tag.getDouble("TargetLandingX");
		this.targetLandingZ = tag.getDouble("TargetLandingZ");
		this.hasLandingTarget = tag.getBoolean("HasLandingTarget");
		this.launchOriginY = tag.getDouble("LaunchOriginY");
		this.countdownTicks = tag.getInt("CountdownTicks");
		this.launchTicks = tag.getInt("LaunchTicks");
		this.ascentTicks = tag.getInt("AscentTicks");
		this.countdownForExoplanet = tag.getBoolean("CountdownForExoplanet");
		this.relocateTargetX = tag.getDouble("RelocateTargetX");
		this.relocateTargetY = tag.getDouble("RelocateTargetY");
		this.relocateTargetZ = tag.getDouble("RelocateTargetZ");
		this.relocatePeakY = tag.getDouble("RelocatePeakY");
		this.relocateBrakeDist = tag.getDouble("RelocateBrakeDist");
		this.relocateFwdSpeed = tag.getDouble("RelocateFwdSpeed");
		this.relocateVertSpeed = tag.getDouble("RelocateVertSpeed");
		this.relocateSubPhase = tag.getInt("RelocateSubPhase");
		this.relocateTicks = tag.getInt("RelocateTicks");
		this.relocateCommanderId = tag.hasUUID("RelocateCommander") ? tag.getUUID("RelocateCommander") : null;
		this.entityData.set(DATA_COUNTDOWN_TICKS, this.countdownTicks);
		this.entityData.set(DATA_ASCENT_TICKS, this.ascentTicks);
		if (tag.contains("OriginDim")) {
			this.originDimId = ResourceLocation.tryParse(tag.getString("OriginDim"));
		}
		if (tag.contains("OriginX")) {
			this.originCorePos = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
		}
		if (tag.contains("LockedCoreX")) {
			this.lockedWormholeCorePos = new BlockPos(tag.getInt("LockedCoreX"), tag.getInt("LockedCoreY"), tag.getInt("LockedCoreZ"));
		}
		importCargoTag(tag.getCompound("Cargo"), this.registryAccess());
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("ShipMode", shipMode());
		tag.putInt("Fuel", fuelUnits());
		tag.putLong("Energy", this.energyStored);
		tag.putFloat("LegDeploy", legDeploy());
		tag.putBoolean("HydroPontoon", hydroPontoon());
		tag.putFloat("PitchDeg", pitchDeg());
		tag.putFloat("RollDeg", rollDeg());
		tag.putFloat("VectorAngle", syncedVectorAngle());
		tag.putInt("RelocateFuelTotal", this.relocateFuelTotal);
		tag.putInt("RelocateFuelBurned", this.relocateFuelBurned);
		tag.putDouble("RelocateInitDist", this.relocateInitialDist);
		tag.putInt("RelocateTurnSign", this.relocateTurnSign);
		tag.putInt("AscentFuelBurned", this.ascentFuelBurned);
		tag.putDouble("ExtVelX", this.extVelX);
		tag.putDouble("ExtVelY", this.extVelY);
		tag.putDouble("ExtVelZ", this.extVelZ);
		tag.putDouble("TargetLandingY", this.targetLandingY);
		tag.putDouble("TargetLandingX", this.targetLandingX);
		tag.putDouble("TargetLandingZ", this.targetLandingZ);
		tag.putBoolean("HasLandingTarget", this.hasLandingTarget);
		tag.putDouble("LaunchOriginY", this.launchOriginY);
		tag.putInt("CountdownTicks", this.countdownTicks);
		tag.putInt("LaunchTicks", this.launchTicks);
		tag.putInt("AscentTicks", this.ascentTicks);
		tag.putBoolean("CountdownForExoplanet", this.countdownForExoplanet);
		tag.putDouble("RelocateTargetX", this.relocateTargetX);
		tag.putDouble("RelocateTargetY", this.relocateTargetY);
		tag.putDouble("RelocateTargetZ", this.relocateTargetZ);
		tag.putDouble("RelocatePeakY", this.relocatePeakY);
		tag.putDouble("RelocateBrakeDist", this.relocateBrakeDist);
		tag.putDouble("RelocateFwdSpeed", this.relocateFwdSpeed);
		tag.putDouble("RelocateVertSpeed", this.relocateVertSpeed);
		tag.putInt("RelocateSubPhase", this.relocateSubPhase);
		tag.putInt("RelocateTicks", this.relocateTicks);
		if (this.relocateCommanderId != null) {
			tag.putUUID("RelocateCommander", this.relocateCommanderId);
		}
		if (this.originDimId != null) {
			tag.putString("OriginDim", this.originDimId.toString());
		}
		if (this.originCorePos != null) {
			tag.putInt("OriginX", this.originCorePos.getX());
			tag.putInt("OriginY", this.originCorePos.getY());
			tag.putInt("OriginZ", this.originCorePos.getZ());
		}
		if (this.lockedWormholeCorePos != null) {
			tag.putInt("LockedCoreX", this.lockedWormholeCorePos.getX());
			tag.putInt("LockedCoreY", this.lockedWormholeCorePos.getY());
			tag.putInt("LockedCoreZ", this.lockedWormholeCorePos.getZ());
		}
		tag.put("Cargo", exportCargoTag(this.registryAccess()));
	}

	@Override
	public Integer getScreenOpeningData(ServerPlayer player) {
		return this.getId();
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("entity.galacticpurpose.interstellar_ship");
	}

	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
		return new StarshipMenu(syncId, playerInv, this);
	}
}

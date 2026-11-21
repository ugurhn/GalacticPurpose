package com.galacticpurpose.terraforming;

import com.galacticpurpose.compat.SpaceCompatManager;
import com.galacticpurpose.config.ModConfig;
import com.galacticpurpose.item.LeadArmorItem;
import com.galacticpurpose.network.MarsSyncPayload;
import com.galacticpurpose.registry.ModDamageTypes;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent Mars dimension state storing Heat, Pressure, Oxygen, Red Giant phase, and Solar Storm cycle.
 */
public final class MarsTerraformingState extends SavedData {
	private static final String DATA_NAME = "galacticpurpose_mars_terraforming";
	private static final String TAG_HEAT = "HeatPoints";
	private static final String TAG_PRESSURE = "PressurePoints";
	private static final String TAG_OXYGEN = "OxygenPoints";
	private static final String TAG_RED_GIANT = "RedGiantActive";
	private static final String TAG_RED_GIANT_TRIGGERED = "RedGiantTriggered";
	private static final String TAG_STORM_TICKS = "StormCycleTicks";
	private static final String TAG_STORM_ANNOUNCED = "StormAnnouncedPhase";

	private static final ResourceLocation GC_FALLING_METEOR =
		ResourceLocation.fromNamespaceAndPath("galacticraft", "falling_meteor");

	public static final Factory<MarsTerraformingState> FACTORY = new Factory<>(
		MarsTerraformingState::new,
		MarsTerraformingState::load,
		null
	);

	private double heatPoints = 0.0;
	private double pressurePoints = 0.0;
	private double oxygenPoints = 0.0;
	private boolean redGiantActive = false;
	private boolean redGiantTriggeredOnce = false;
	private long stormCycleTicks = 0L;
	private SolarStormPhase lastAnnouncedStormPhase = SolarStormPhase.DORMANT;
	private long lastSyncGameTime = -100L;
	private boolean lastBreathableSent = false;

	public static void registerEvents() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			syncPlayer(handler.getPlayer()));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
			syncPlayer(player));
		ServerTickEvents.END_WORLD_TICK.register(MarsTerraformingState::onMarsWorldTick);
		ServerEntityEvents.ENTITY_LOAD.register((entity, serverLevel) -> {
			if (!SpaceCompatManager.get().isMars(serverLevel)) {
				return;
			}
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(GC_FALLING_METEOR)) {
				if (get(serverLevel).snapshot().meteorCalm()) {
					entity.discard();
				}
			}
		});
	}

	private static void onMarsWorldTick(ServerLevel level) {
		if (!SpaceCompatManager.get().isMars(level)) {
			return;
		}
		MarsTerraformingState state = get(level);
		MarsProgressSnapshot snap = state.snapshot();

		// 1. Update Galacticraft breathability when oxygen hits/drops below 100%
		if (snap.breathable() != state.lastBreathableSent) {
			state.lastBreathableSent = snap.breathable();
			SpaceCompatManager.get().setMarsBreathable(level, snap.breathable());
		}

		// 2. Budgeted ice melting & flora spreading
		MarsWorldTerraformer.tickBudgeted(level, snap);

		// 3. Automatic Red Giant trigger once Mars achieves breathability or mature Ti
		if (!state.redGiantTriggeredOnce
			&& ModConfig.get().enableRedGiantCollapse()
			&& (snap.breathable() || snap.terraformingIndex() >= 55_000.0)) {
			state.activateRedGiant(level, true);
		}

		// 4. Red Giant stripping & Solar Storm radiation loop (1 Hz)
		if (state.redGiantActive) {
			state.stormCycleTicks++;
			if (level.getGameTime() % 20L == 0L) {
				// Solar wind strips Mars atmosphere back toward space unless machines hold it up.
				state.addProgress(-1.5, -2.0, -2.5);
				state.evaluateAndApplySolarStorm(level);
				state.setDirty();
				state.syncDimension(level);
			}
		} else if (level.getGameTime() % 40L == 0L) {
			state.syncDimensionIfThrottled(level);
		}
	}

	private void evaluateAndApplySolarStorm(ServerLevel level) {
		StormSnapshot storm = SolarStormCalculator.evaluate(
			this.redGiantActive,
			this.stormCycleTicks,
			true,
			false,
			false
		);

		if (storm.phase() != this.lastAnnouncedStormPhase) {
			this.lastAnnouncedStormPhase = storm.phase();
			if (storm.phase() == SolarStormPhase.WARNING) {
				broadcastToDimension(level, Component.translatable(
					"message.galacticpurpose.storm_warning",
					storm.secondsUntilImpact()
				).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			} else if (storm.phase() == SolarStormPhase.ACTIVE) {
				broadcastToDimension(level, Component.translatable(
					"message.galacticpurpose.storm_active",
					storm.secondsRemaining()
				).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			} else if (storm.phase() == SolarStormPhase.COUNTDOWN) {
				broadcastToDimension(level, Component.translatable("message.galacticpurpose.storm_cleared")
					.withStyle(ChatFormatting.AQUA));
			}
		}

		if (storm.phase() != SolarStormPhase.ACTIVE) {
			return;
		}

		for (ServerPlayer player : level.players()) {
			if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
				continue;
			}
			BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
			boolean shielded = RadiationShelterChecker.isPositionShielded(level, eyePos);
			boolean leadSuit = LeadArmorItem.hasFullSuit(player);

			StormSnapshot playerStorm = SolarStormCalculator.evaluate(
				this.redGiantActive,
				this.stormCycleTicks,
				!shielded,
				leadSuit,
				false
			);
			if (playerStorm.radiationDamage() > 0.0f) {
				player.hurt(
					level.damageSources().source(ModDamageTypes.SOLAR_STORM_RADIATION),
					playerStorm.radiationDamage()
				);
			}
		}
	}

	private static void broadcastToDimension(ServerLevel level, Component message) {
		for (ServerPlayer player : level.players()) {
			player.displayClientMessage(message, false);
		}
	}

	public static MarsTerraformingState get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static MarsTerraformingState load(CompoundTag tag, HolderLookup.Provider registries) {
		MarsTerraformingState state = new MarsTerraformingState();
		state.heatPoints = MarsTerraformingCalculator.clampPoints(tag.getDouble(TAG_HEAT));
		state.pressurePoints = MarsTerraformingCalculator.clampPoints(tag.getDouble(TAG_PRESSURE));
		state.oxygenPoints = MarsTerraformingCalculator.clampPoints(tag.getDouble(TAG_OXYGEN));
		state.redGiantActive = tag.getBoolean(TAG_RED_GIANT);
		state.redGiantTriggeredOnce = tag.getBoolean(TAG_RED_GIANT_TRIGGERED);
		state.stormCycleTicks = Math.max(0L, tag.getLong(TAG_STORM_TICKS));
		state.lastAnnouncedStormPhase = SolarStormPhase.byOrdinal(tag.getInt(TAG_STORM_ANNOUNCED));
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putDouble(TAG_HEAT, this.heatPoints);
		tag.putDouble(TAG_PRESSURE, this.pressurePoints);
		tag.putDouble(TAG_OXYGEN, this.oxygenPoints);
		tag.putBoolean(TAG_RED_GIANT, this.redGiantActive);
		tag.putBoolean(TAG_RED_GIANT_TRIGGERED, this.redGiantTriggeredOnce);
		tag.putLong(TAG_STORM_TICKS, this.stormCycleTicks);
		tag.putInt(TAG_STORM_ANNOUNCED, this.lastAnnouncedStormPhase.ordinal());
		return tag;
	}

	public void addProgress(double deltaHeat, double deltaPressure, double deltaOxygen) {
		double nextHeat = MarsTerraformingCalculator.clampPoints(this.heatPoints + deltaHeat);
		double nextPressure = MarsTerraformingCalculator.clampPoints(this.pressurePoints + deltaPressure);
		double nextOxygen = MarsTerraformingCalculator.clampPoints(this.oxygenPoints + deltaOxygen);

		if (nextHeat != this.heatPoints || nextPressure != this.pressurePoints || nextOxygen != this.oxygenPoints) {
			this.heatPoints = nextHeat;
			this.pressurePoints = nextPressure;
			this.oxygenPoints = nextOxygen;
			this.setDirty();
		}
	}

	public void setPercents(float heatPct, float pressurePct, float oxygenPct) {
		this.heatPoints = MarsTerraformingCalculator.clampPoints((heatPct / 100.0) * MarsTerraformingCalculator.MAX_POINTS);
		this.pressurePoints = MarsTerraformingCalculator.clampPoints((pressurePct / 100.0) * MarsTerraformingCalculator.MAX_POINTS);
		this.oxygenPoints = MarsTerraformingCalculator.clampPoints((oxygenPct / 100.0) * MarsTerraformingCalculator.MAX_POINTS);
		this.setDirty();
	}

	public void activateRedGiant(ServerLevel level, boolean active) {
		this.redGiantActive = active;
		if (active) {
			this.redGiantTriggeredOnce = true;
			this.stormCycleTicks = 0L;
			this.lastAnnouncedStormPhase = SolarStormPhase.COUNTDOWN;
			broadcastToDimension(level, Component.translatable("message.galacticpurpose.red_giant_started")
				.withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
		} else {
			this.lastAnnouncedStormPhase = SolarStormPhase.DORMANT;
		}
		this.setDirty();
		this.syncDimension(level);
	}

	public void setStormPhaseForTesting(ServerLevel level, SolarStormPhase phase) {
		this.redGiantActive = true;
		this.redGiantTriggeredOnce = true;
		this.stormCycleTicks = switch (phase) {
			case WARNING -> SolarStormCalculator.WARNING_START_SECOND * 20L;
			case ACTIVE -> SolarStormCalculator.IMPACT_START_SECOND * 20L;
			default -> 0L;
		};
		this.lastAnnouncedStormPhase = SolarStormPhase.DORMANT;
		this.setDirty();
		this.syncDimension(level);
	}

	public boolean isRedGiantActive() {
		return this.redGiantActive;
	}

	public long getStormCycleTicks() {
		return this.stormCycleTicks;
	}

	public MarsProgressSnapshot snapshot() {
		return MarsTerraformingCalculator.snapshot(this.heatPoints, this.pressurePoints, this.oxygenPoints);
	}

	public void syncDimensionIfThrottled(ServerLevel level) {
		long now = level.getGameTime();
		if (now - this.lastSyncGameTime < 20L) {
			return;
		}
		this.lastSyncGameTime = now;
		syncDimension(level);
	}

	public void syncDimension(ServerLevel level) {
		MarsSyncPayload payload = toPayload();
		for (ServerPlayer player : level.players()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void syncPlayer(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		if (!SpaceCompatManager.get().isMars(level)) {
			ServerPlayNetworking.send(player, MarsSyncPayload.inactive());
			return;
		}
		ServerPlayNetworking.send(player, get(level).toPayload());
	}

	private MarsSyncPayload toPayload() {
		StormSnapshot storm = SolarStormCalculator.evaluate(
			this.redGiantActive,
			this.stormCycleTicks,
			true,
			false,
			false
		);
		int timer = storm.phase() == SolarStormPhase.ACTIVE
			? storm.secondsRemaining()
			: storm.secondsUntilImpact();
		return new MarsSyncPayload(
			true,
			(float) this.heatPoints,
			(float) this.pressurePoints,
			(float) this.oxygenPoints,
			this.redGiantActive,
			storm.phase().ordinal(),
			timer
		);
	}
}

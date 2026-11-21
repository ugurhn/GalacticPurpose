package com.galacticpurpose.ship;

import com.galacticpurpose.wormhole.ExoplanetRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Overworld-persisted session state for players currently walking inside their Starship
 * in galacticpurpose:orbital_transit (survives relogs and supports isolated multiplayer slots).
 */
public final class OrbitalTransitState extends SavedData {
	private static final String DATA_NAME = "galacticpurpose_orbital_transit";
	private static final SavedData.Factory<OrbitalTransitState> FACTORY =
		new SavedData.Factory<>(OrbitalTransitState::new, OrbitalTransitState::load, null);

	public static final class TransitSession {
		public final UUID playerId;
		public final int slotIndex;
		public final boolean returning;
		public final ExoplanetRoute route;
		public final ResourceLocation originDimId;
		public final BlockPos originCorePos;
		public int fuelUnits;
		public int startFuel;
		public int plannedFuelBurn;
		public long energyStored;
		public CompoundTag cargoTag;
		public int elapsedTicks;
		public boolean reentryActive;
		public int reentryTicks;
		public transient int cachedTargetX;
		public transient int cachedTargetZ;
		public transient boolean hasCachedTarget;

		public TransitSession(
			UUID playerId, int slotIndex, boolean returning, ExoplanetRoute route,
			ResourceLocation originDimId, BlockPos originCorePos,
			int fuelUnits, long energyStored, CompoundTag cargoTag
		) {
			this.playerId = playerId;
			this.slotIndex = slotIndex;
			this.returning = returning;
			this.route = route;
			this.originDimId = originDimId;
			this.originCorePos = originCorePos;
			this.fuelUnits = fuelUnits;
			this.startFuel = fuelUnits;
			this.plannedFuelBurn = 0;
			this.energyStored = energyStored;
			this.cargoTag = cargoTag != null ? cargoTag.copy() : new CompoundTag();
		}
	}

	private final Map<UUID, TransitSession> sessions = new HashMap<>();
	private int nextSlot = 0;

	public static OrbitalTransitState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public TransitSession createSession(
		UUID playerId, boolean returning, ExoplanetRoute route,
		ResourceLocation originDimId, BlockPos originCorePos,
		int fuelUnits, long energyStored, CompoundTag cargoTag
	) {
		TransitSession existing = this.sessions.get(playerId);
		int slot = existing != null ? existing.slotIndex : this.nextSlot++;
		TransitSession session = new TransitSession(
			playerId, slot, returning, route, originDimId, originCorePos, fuelUnits, energyStored, cargoTag
		);
		this.sessions.put(playerId, session);
		this.setDirty();
		return session;
	}

	public TransitSession getSession(UUID playerId) {
		return this.sessions.get(playerId);
	}

	public void removeSession(UUID playerId) {
		if (this.sessions.remove(playerId) != null) {
			this.setDirty();
		}
	}

	public Map<UUID, TransitSession> sessions() {
		return this.sessions;
	}

	private static OrbitalTransitState load(CompoundTag tag, HolderLookup.Provider registries) {
		OrbitalTransitState state = new OrbitalTransitState();
		state.nextSlot = tag.getInt("NextSlot");
		ListTag list = tag.getList("Sessions", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag s = list.getCompound(i);
			if (!s.hasUUID("Player")) continue;
			UUID pid = s.getUUID("Player");
			int slot = s.getInt("Slot");
			boolean ret = s.getBoolean("Returning");
			ExoplanetRoute[] routes = ExoplanetRoute.values();
			int rOrd = Math.max(0, Math.min(routes.length - 1, s.getInt("Route")));
			ResourceLocation originDim = ResourceLocation.tryParse(s.getString("OriginDim"));
			BlockPos originPos = s.contains("OriginX")
				? new BlockPos(s.getInt("OriginX"), s.getInt("OriginY"), s.getInt("OriginZ"))
				: null;
			TransitSession ts = new TransitSession(
				pid, slot, ret, routes[rOrd], originDim, originPos,
				s.getInt("Fuel"), s.getLong("Energy"), s.getCompound("Cargo")
			);
			ts.elapsedTicks = s.getInt("Elapsed");
			ts.startFuel = s.contains("StartFuel") ? s.getInt("StartFuel") : ts.fuelUnits;
			ts.plannedFuelBurn = s.getInt("PlannedBurn");
			ts.reentryActive = s.getBoolean("Reentry");
			ts.reentryTicks = s.getInt("ReentryTicks");
			state.sessions.put(pid, ts);
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putInt("NextSlot", this.nextSlot);
		ListTag list = new ListTag();
		for (TransitSession ts : this.sessions.values()) {
			CompoundTag s = new CompoundTag();
			s.putUUID("Player", ts.playerId);
			s.putInt("Slot", ts.slotIndex);
			s.putBoolean("Returning", ts.returning);
			s.putInt("Route", ts.route.ordinal());
			if (ts.originDimId != null) {
				s.putString("OriginDim", ts.originDimId.toString());
			}
			if (ts.originCorePos != null) {
				s.putInt("OriginX", ts.originCorePos.getX());
				s.putInt("OriginY", ts.originCorePos.getY());
				s.putInt("OriginZ", ts.originCorePos.getZ());
			}
			s.putInt("Fuel", ts.fuelUnits);
			s.putInt("StartFuel", ts.startFuel);
			s.putInt("PlannedBurn", ts.plannedFuelBurn);
			s.putLong("Energy", ts.energyStored);
			s.put("Cargo", ts.cargoTag != null ? ts.cargoTag : new CompoundTag());
			s.putInt("Elapsed", ts.elapsedTicks);
			s.putBoolean("Reentry", ts.reentryActive);
			s.putInt("ReentryTicks", ts.reentryTicks);
			list.add(s);
		}
		tag.put("Sessions", list);
		return tag;
	}
}

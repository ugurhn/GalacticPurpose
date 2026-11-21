package com.galacticpurpose.climate;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent Overworld progression data tracking announced solar collapse milestones.
 */
public final class EarthCollapseState extends SavedData {
	private static final String DATA_NAME = "galacticpurpose_earth_collapse";
	private static final String TAG_ANNOUNCED_PHASE = "AnnouncedPhase";

	public static final Factory<EarthCollapseState> FACTORY = new Factory<>(
		EarthCollapseState::new,
		EarthCollapseState::load,
		null
	);

	private CollapsePhase lastAnnouncedPhase = CollapsePhase.STABLE;

	public static EarthCollapseState get(ServerLevel overworld) {
		return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static EarthCollapseState load(CompoundTag tag, HolderLookup.Provider registries) {
		EarthCollapseState state = new EarthCollapseState();
		state.lastAnnouncedPhase = CollapsePhase.byOrdinal(tag.getInt(TAG_ANNOUNCED_PHASE));
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putInt(TAG_ANNOUNCED_PHASE, this.lastAnnouncedPhase.ordinal());
		return tag;
	}

	public CollapsePhase getLastAnnouncedPhase() {
		return this.lastAnnouncedPhase;
	}

	public void setLastAnnouncedPhase(CollapsePhase phase) {
		if (this.lastAnnouncedPhase != phase) {
			this.lastAnnouncedPhase = phase;
			this.setDirty();
		}
	}
}

package com.galacticpurpose.wormhole;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Overworld-stored return tether coordinates and first-visit completion markers for exoplanet travel.
 */
public final class WormholeReturnState extends SavedData {
	private static final String DATA_NAME = "galacticpurpose_wormhole_returns";

	public record ReturnAnchor(ResourceLocation dimensionId, double x, double y, double z) {
	}

	public static final Factory<WormholeReturnState> FACTORY = new Factory<>(
		WormholeReturnState::new,
		WormholeReturnState::load,
		null
	);

	private final Map<UUID, ReturnAnchor> returnAnchors = new HashMap<>();
	private final Set<UUID> sanctuaryArrivals = new HashSet<>();

	public static WormholeReturnState get(ServerLevel level) {
		ServerLevel overworld = level.getServer().overworld();
		return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static WormholeReturnState load(CompoundTag tag, HolderLookup.Provider registries) {
		WormholeReturnState state = new WormholeReturnState();
		ListTag anchors = tag.getList("Anchors", Tag.TAG_COMPOUND);
		for (int i = 0; i < anchors.size(); i++) {
			CompoundTag entry = anchors.getCompound(i);
			if (!entry.hasUUID("Player")) {
				continue;
			}
			ResourceLocation dimId = ResourceLocation.tryParse(entry.getString("Dimension"));
			if (dimId != null) {
				state.returnAnchors.put(
					entry.getUUID("Player"),
					new ReturnAnchor(dimId, entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z"))
				);
			}
		}
		ListTag sanctuaryList = tag.getList("SanctuaryArrivals", Tag.TAG_COMPOUND);
		for (int i = 0; i < sanctuaryList.size(); i++) {
			CompoundTag entry = sanctuaryList.getCompound(i);
			if (entry.hasUUID("Player")) {
				state.sanctuaryArrivals.add(entry.getUUID("Player"));
			}
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag anchors = new ListTag();
		for (Map.Entry<UUID, ReturnAnchor> entry : this.returnAnchors.entrySet()) {
			CompoundTag item = new CompoundTag();
			item.putUUID("Player", entry.getKey());
			ReturnAnchor anchor = entry.getValue();
			item.putString("Dimension", anchor.dimensionId().toString());
			item.putDouble("X", anchor.x());
			item.putDouble("Y", anchor.y());
			item.putDouble("Z", anchor.z());
			anchors.add(item);
		}
		tag.put("Anchors", anchors);

		ListTag sanctuaryList = new ListTag();
		for (UUID uuid : this.sanctuaryArrivals) {
			CompoundTag item = new CompoundTag();
			item.putUUID("Player", uuid);
			sanctuaryList.add(item);
		}
		tag.put("SanctuaryArrivals", sanctuaryList);
		return tag;
	}

	public void setReturnAnchor(UUID playerId, ReturnAnchor anchor) {
		this.returnAnchors.put(playerId, anchor);
		this.setDirty();
	}

	public ReturnAnchor getReturnAnchor(UUID playerId) {
		return this.returnAnchors.get(playerId);
	}

	public boolean markSanctuaryArrived(UUID playerId) {
		if (this.sanctuaryArrivals.add(playerId)) {
			this.setDirty();
			return true;
		}
		return false;
	}
}

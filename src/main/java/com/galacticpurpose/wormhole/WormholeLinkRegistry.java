package com.galacticpurpose.wormhole;

import com.galacticpurpose.block.WormholeCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Overworld-persisted registry tracking every Wormhole Core's live open/closed/destroyed state
 * so singleplayer chunk unloads preserve an open return link while multiplayer sabotage, shutdown,
 * coolant starvation, or meltdowns immediately lock out returning Starships with [SIGNAL LOST]!
 */
public final class WormholeLinkRegistry extends SavedData {
	private static final String DATA_NAME = "galacticpurpose_wormhole_links";
	private static final SavedData.Factory<WormholeLinkRegistry> FACTORY =
		new SavedData.Factory<>(WormholeLinkRegistry::new, WormholeLinkRegistry::load, null);

	private final Map<String, WormholeLinkCalculator.LinkStatus> links = new HashMap<>();

	public static WormholeLinkRegistry get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public static String keyOf(ResourceLocation dimId, BlockPos pos) {
		return dimId.toString() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	public void updateStatus(ResourceLocation dimId, BlockPos pos, WormholeLinkCalculator.LinkStatus status) {
		String key = keyOf(dimId, pos);
		if (this.links.get(key) != status) {
			this.links.put(key, status);
			this.setDirty();
		}
	}

	public WormholeLinkCalculator.LinkStatus queryLiveStatus(
		MinecraftServer server,
		ResourceLocation originDimId,
		BlockPos originPos
	) {
		if (originDimId == null || originPos == null) {
			return WormholeLinkCalculator.LinkStatus.UNLINKED;
		}
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, originDimId);
		ServerLevel originLevel = server.getLevel(key);
		if (originLevel != null && originLevel.isLoaded(originPos)) {
			BlockEntity be = originLevel.getBlockEntity(originPos);
			if (!(be instanceof WormholeCoreBlockEntity core) || core.isRemoved()) {
				updateStatus(originDimId, originPos, WormholeLinkCalculator.LinkStatus.DESTROYED);
				return WormholeLinkCalculator.LinkStatus.DESTROYED;
			}
			WormholeLinkCalculator.LinkStatus live = core.wormholeState() == WormholeState.OPEN
				? WormholeLinkCalculator.LinkStatus.OPEN
				: WormholeLinkCalculator.LinkStatus.CLOSED;
			updateStatus(originDimId, originPos, live);
			return live;
		}
		return this.links.getOrDefault(keyOf(originDimId, originPos), WormholeLinkCalculator.LinkStatus.CLOSED);
	}

	private static WormholeLinkRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
		WormholeLinkRegistry data = new WormholeLinkRegistry();
		ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			String key = entry.getString("Key");
			int ord = entry.getInt("Status");
			WormholeLinkCalculator.LinkStatus[] vals = WormholeLinkCalculator.LinkStatus.values();
			if (!key.isEmpty() && ord >= 0 && ord < vals.length) {
				data.links.put(key, vals[ord]);
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<String, WormholeLinkCalculator.LinkStatus> e : this.links.entrySet()) {
			CompoundTag entry = new CompoundTag();
			entry.putString("Key", e.getKey());
			entry.putInt("Status", e.getValue().ordinal());
			list.add(entry);
		}
		tag.put("Links", list);
		return tag;
	}
}

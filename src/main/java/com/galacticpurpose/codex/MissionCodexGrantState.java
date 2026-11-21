package com.galacticpurpose.codex;

import com.galacticpurpose.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Grants the Galactic Purpose Mission Codex once per player upon first joining a world.
 */
public final class MissionCodexGrantState extends SavedData {
	private static final String DATA_NAME = "galacticpurpose_codex_grants";
	private final Set<UUID> grantedPlayers = new HashSet<>();

	public MissionCodexGrantState() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			ServerLevel overworld = server.overworld();
			MissionCodexGrantState state = get(overworld);
			if (state.tryMarkGranted(player.getUUID())) {
				ItemStack codex = new ItemStack(ModItems.MISSION_CODEX);
				if (!player.getInventory().add(codex)) {
					player.drop(codex, false);
				}
				player.displayClientMessage(
					Component.translatable("message.galacticpurpose.codex_granted")
						.withStyle(ChatFormatting.AQUA),
					false
				);
			}
		});
	}

	public static MissionCodexGrantState get(ServerLevel overworld) {
		return overworld.getDataStorage().computeIfAbsent(
			new Factory<>(MissionCodexGrantState::new, MissionCodexGrantState::load, null),
			DATA_NAME
		);
	}

	public boolean tryMarkGranted(UUID playerId) {
		if (this.grantedPlayers.add(playerId)) {
			this.setDirty();
			return true;
		}
		return false;
	}

	private static MissionCodexGrantState load(CompoundTag tag, HolderLookup.Provider registries) {
		MissionCodexGrantState state = new MissionCodexGrantState();
		ListTag list = tag.getList("Players", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			try {
				state.grantedPlayers.add(UUID.fromString(list.getString(i)));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (UUID id : this.grantedPlayers) {
			list.add(StringTag.valueOf(id.toString()));
		}
		tag.put("Players", list);
		return tag;
	}
}

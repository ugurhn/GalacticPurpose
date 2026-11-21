package com.galacticpurpose.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Compatibility boundary isolating Galacticraft 5 API interactions from core progression systems.
 */
public interface SpaceCompat {
	/**
	 * @return {@code true} when Galacticraft (GC5 / compatible fork) is loaded in this environment.
	 */
	boolean isGalacticraftLoaded();

	/**
	 * @return {@code true} if the given level represents Mars.
	 */
	boolean isMars(Level level);

	/**
	 * Applies or revokes breathable atmosphere status on Mars when terraforming crosses the threshold.
	 */
	void setMarsBreathable(ServerLevel level, boolean breathable);

	/**
	 * @return {@code true} if the player is wearing a full situational Thermal-Shielded Suit.
	 */
	default boolean hasThermalShield(Player player) {
		return false;
	}
}

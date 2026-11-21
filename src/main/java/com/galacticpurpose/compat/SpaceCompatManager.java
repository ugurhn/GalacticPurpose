package com.galacticpurpose.compat;

import com.galacticpurpose.config.ModConfig;
import com.galacticpurpose.item.LeadArmorItem;
import com.galacticpurpose.item.ThermalArmorItem;
import com.galacticpurpose.terraforming.MarsTerraformingState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Resolves the active {@link SpaceCompat} bridge at startup without hard-crashing if GC5 is absent.
 */
public final class SpaceCompatManager {
	private static SpaceCompat active = new BaseSpaceCompat(false, null);
	private static volatile boolean clientMarsBreathable = false;

	private SpaceCompatManager() {
	}

	public static void initialize() {
		boolean gcLoaded = FabricLoader.getInstance().isModLoaded("galacticraft");
		MethodHandle setDefaultBreathableHandle = gcLoaded ? resolveSetDefaultBreathable() : null;
		active = new BaseSpaceCompat(gcLoaded, setDefaultBreathableHandle);
	}

	public static SpaceCompat get() {
		return active;
	}

	public static void setClientMarsBreathable(boolean breathable) {
		clientMarsBreathable = breathable;
	}

	public static boolean isLevelBreathableMars(Level level) {
		if (!active.isMars(level)) {
			return false;
		}
		if (level instanceof ServerLevel serverLevel) {
			return MarsTerraformingState.get(serverLevel).snapshot().breathable();
		}
		return clientMarsBreathable;
	}

	private static MethodHandle resolveSetDefaultBreathable() {
		try {
			Class<?> accessorClass = Class.forName("dev.galacticraft.api.accessor.LevelOxygenAccessor");
			return MethodHandles.publicLookup().findVirtual(
				accessorClass,
				"setDefaultBreathable",
				MethodType.methodType(void.class, boolean.class)
			);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static boolean matchesMarsDimension(Level level) {
		if (ModConfig.get().marsTerraformingTestMode()) {
			return true;
		}
		ResourceLocation dimId = level.dimension().location();
		ResourceLocation target = ResourceLocation.tryParse(ModConfig.get().marsDimensionId());
		if (target != null && dimId.equals(target)) {
			return true;
		}
		// Auto-detect Mars from any space mod (Galacticraft 5, Ad Astra, Cosmic Horizons, etc.)
		return "mars".equals(dimId.getPath());
	}

	private record BaseSpaceCompat(boolean galacticraftLoaded, MethodHandle setBreathableHandle) implements SpaceCompat {
		@Override
		public boolean isGalacticraftLoaded() {
			return this.galacticraftLoaded;
		}

		@Override
		public boolean isMars(Level level) {
			return matchesMarsDimension(level);
		}

		@Override
		public void setMarsBreathable(ServerLevel level, boolean breathable) {
			if (!this.galacticraftLoaded || this.setBreathableHandle == null) {
				return;
			}
			try {
				this.setBreathableHandle.invoke(level, breathable);
			} catch (Throwable ignored) {
				// LevelOxygenAccessor mixin fallback remains active via MarsOxygenLevelMixin.
			}
		}

		@Override
		public boolean hasThermalShield(Player player) {
			return ThermalArmorItem.hasFullSuit(player);
		}

		public boolean hasLeadSuit(Player player) {
			return LeadArmorItem.hasFullSuit(player);
		}
	}
}

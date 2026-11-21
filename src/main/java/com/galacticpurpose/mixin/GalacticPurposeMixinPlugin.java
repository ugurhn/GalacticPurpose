package com.galacticpurpose.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Loads Galacticraft-specific compatibility mixins only when Galacticraft 5 is present at runtime.
 */
public class GalacticPurposeMixinPlugin implements IMixinConfigPlugin {
	private boolean galacticraftLoaded;

	@Override
	public void onLoad(String mixinPackage) {
		this.galacticraftLoaded = FabricLoader.getInstance().isModLoaded("galacticraft");
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.endsWith("MarsOxygenLevelMixin")) {
			return this.galacticraftLoaded;
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}

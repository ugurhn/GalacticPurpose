package com.galacticpurpose.mixin;

import com.galacticpurpose.compat.SpaceCompatManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides Galacticraft 5's oxygen checks on Mars once terraformed oxygen reaches 100%.
 */
@Mixin(value = Level.class, priority = 1500)
public abstract class MarsOxygenLevelMixin {
	@Inject(method = "isBreathable(III)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private void galacticpurpose$allowBreathableMarsPos(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (SpaceCompatManager.isLevelBreathableMars((Level) (Object) this)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getDefaultBreathable()Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private void galacticpurpose$allowBreathableMarsDefault(CallbackInfoReturnable<Boolean> cir) {
		if (SpaceCompatManager.isLevelBreathableMars((Level) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}

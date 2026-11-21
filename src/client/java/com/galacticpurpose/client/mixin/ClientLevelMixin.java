package com.galacticpurpose.client.mixin;

import com.galacticpurpose.client.hud.ClientClimateState;
import com.galacticpurpose.client.hud.ClientMarsState;
import com.galacticpurpose.terraforming.SolarStormPhase;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dynamically tints the sky for both Overworld solar collapse and Mars terraforming / Red Giant phases.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void galacticpurpose$tintDynamicPlanetarySky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
		Vec3 base = cir.getReturnValue();

		if (ClientMarsState.isActiveInDimension()) {
			float progress = Math.min(1.0f, Math.max(0.0f, ClientMarsState.snapshot().overallPercent() / 100.0f));
			double luminance = Math.max(0.08, Math.min(1.0, (base.x + base.y + base.z) / 2.1));

			Vec3 barrenMars = new Vec3(0.72 * luminance, 0.28 * luminance, 0.16 * luminance);
			Vec3 terraformedBlue = new Vec3(0.42 * luminance, 0.72 * luminance, 0.98 * luminance);
			Vec3 sky = barrenMars.lerp(terraformedBlue, progress);

			if (ClientMarsState.isRedGiantActive()) {
				Vec3 redGiantTarget = ClientMarsState.getStormPhase() == SolarStormPhase.ACTIVE
					? new Vec3(0.95 * luminance, 0.28 * luminance, 0.35 * luminance)
					: new Vec3(0.86 * luminance, 0.30 * luminance, 0.10 * luminance);
				float redShift = ClientMarsState.getStormPhase() == SolarStormPhase.ACTIVE ? 0.80f : 0.55f;
				sky = sky.lerp(redGiantTarget, redShift);
			}
			cir.setReturnValue(sky);
			return;
		}

		if (!ClientClimateState.isActiveInDimension()) {
			return;
		}
		float severity = ClientClimateState.getSeverity();
		if (severity <= 0.01f) {
			return;
		}

		float blend = Math.min(0.65f, severity * 0.65f);
		double luminance = Math.max(0.05, Math.min(1.0, (base.x + base.y + base.z) / 2.2));
		Vec3 scorchedTarget = new Vec3(0.82 * luminance, 0.23 * luminance, 0.10 * luminance);
		cir.setReturnValue(base.lerp(scorchedTarget, blend));
	}
}

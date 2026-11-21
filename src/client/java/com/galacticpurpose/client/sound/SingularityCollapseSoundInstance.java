package com.galacticpurpose.client.sound;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.entity.SingularityCollapseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.concurrent.CompletableFuture;

/**
 * Escalating dark gravity warp & pitch-bending implosion siren tracking a runaway SingularityCollapseEntity.
 * Climbs relentlessly during ascent/vortex, screams upward during implosion, and drops into eerie dead silence
 * right before triggering the cataclysmic detonation boom!
 */
public final class SingularityCollapseSoundInstance extends AbstractTickableSoundInstance {
	private static final SoundEvent COLLAPSE_SIREN =
		SoundEvent.createVariableRangeEvent(GalacticPurpose.id("singularity_collapse_siren"));
	private final SingularityCollapseEntity entity;

	public SingularityCollapseSoundInstance(SingularityCollapseEntity entity) {
		super(COLLAPSE_SIREN, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
		this.entity = entity;
		this.x = entity.getX();
		this.y = entity.getY();
		this.z = entity.getZ();
		this.looping = true;
		this.delay = 0;
		this.volume = 0.65f;
		this.pitch = 0.55f;
		this.attenuation = Attenuation.NONE;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
		return CompletableFuture.completedFuture(new ProceduralAudioStreams.CollapseWarpStream());
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !this.entity.isAlive() || this.entity.isDetonating()) {
			this.stop();
			return;
		}

		this.x = this.entity.getX();
		this.y = this.entity.getY();
		this.z = this.entity.getZ();

		double dist = Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z));
		float distFactor = Mth.clamp((float) (1.0 - dist / 320.0), 0.12f, 1.0f);
		int age = this.entity.collapseAge();

		// Last 9 ticks (~0.45s) before detonation: eerie dead vacuum silence right before shockwave!
		if (age >= SingularityCollapseEntity.DETONATION_TICK - 9) {
			this.volume = 0.001f;
			return;
		}

		if (age < SingularityCollapseEntity.VORTEX_PHASE_END) {
			float t = age / (float) SingularityCollapseEntity.VORTEX_PHASE_END;
			this.pitch = 0.82f + t * 0.48f;
			this.volume = (0.78f + t * 0.52f) * distFactor;
		} else {
			// Implosion hyper-tension pitch rise!
			float imp = Mth.clamp(
				(age - SingularityCollapseEntity.VORTEX_PHASE_END) / (float) (SingularityCollapseEntity.DETONATION_TICK - 9 - SingularityCollapseEntity.VORTEX_PHASE_END),
				0.0f, 1.0f
			);
			this.pitch = Mth.clamp(1.30f + imp * imp * 0.65f, 0.5f, 2.0f);
			this.volume = (1.30f + imp * 0.25f) * distFactor;
		}
	}
}

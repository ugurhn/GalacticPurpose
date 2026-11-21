package com.galacticpurpose.client.sound;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.CompletableFuture;

/**
 * Global non-attenuated 6.5-second Draconic-scale seismic detonation shockwave audio burst.
 */
public final class SingularityDetonationSoundInstance extends AbstractSoundInstance {
	private static final ResourceLocation SOUND_ID = GalacticPurpose.id("singularity_detonation");

	public SingularityDetonationSoundInstance(double x, double y, double z, float volumeScale) {
		super(SOUND_ID, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
		this.x = x;
		this.y = y;
		this.z = z;
		this.looping = false;
		this.delay = 0;
		this.volume = Math.max(0.25f, Math.min(1.6f, volumeScale));
		this.pitch = 1.0f;
		this.attenuation = Attenuation.NONE;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
		return CompletableFuture.completedFuture(new ProceduralAudioStreams.CataclysmicDetonationStream());
	}
}

package com.galacticpurpose.client.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Reusable global non-attenuated one-shot custom 44.1 kHz PCM sound instance for countdown pings,
 * ignition blasts, relativistic wormhole warp transitions, hydraulic landings, and abort alarms.
 */
public final class OneShotProceduralSoundInstance extends AbstractSoundInstance {
	private final Supplier<AudioStream> streamFactory;

	public OneShotProceduralSoundInstance(
		ResourceLocation soundId,
		float volume,
		float pitch,
		Supplier<AudioStream> streamFactory
	) {
		super(soundId, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.streamFactory = streamFactory;
		this.looping = false;
		this.delay = 0;
		this.attenuation = Attenuation.NONE;
		this.relative = true;
		this.volume = Math.max(0.25f, Math.min(3.5f, volume));
		this.pitch = Math.max(0.5f, Math.min(2.0f, pitch));
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
		return CompletableFuture.completedFuture(this.streamFactory.get());
	}
}

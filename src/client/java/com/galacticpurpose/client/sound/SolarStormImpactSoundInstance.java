package com.galacticpurpose.client.sound;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.client.hud.ClientMarsState;
import com.galacticpurpose.terraforming.SolarStormPhase;
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
 * Looping CME solar wind & Geiger crackle active while a Red Giant Solar Storm strikes Mars,
 * attenuated naturally when sheltered inside a sealed Lead-Concrete bunker.
 */
public final class SolarStormImpactSoundInstance extends AbstractTickableSoundInstance {
	private static final SoundEvent IMPACT_LOOP =
		SoundEvent.createVariableRangeEvent(GalacticPurpose.id("solar_storm_impact"));

	public SolarStormImpactSoundInstance() {
		super(IMPACT_LOOP, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
		this.looping = true;
		this.delay = 0;
		this.relative = true;
		this.volume = 0.28f;
		this.pitch = 1.0f;
		this.attenuation = Attenuation.NONE;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
		return CompletableFuture.completedFuture(new ProceduralAudioStreams.SolarStormImpactLoopStream());
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null
			|| mc.player == null
			|| !ClientMarsState.isActiveInDimension()
			|| !ClientMarsState.isRedGiantActive()
			|| ClientMarsState.getStormPhase() != SolarStormPhase.ACTIVE) {
			this.stop();
			return;
		}
		float leakFrac = Mth.clamp(ClientMarsState.getShelterLeakPercent() / 100.0f, 0.0f, 1.0f);
		// Sealed bunkers hear muffled exterior solar roar (0.14), exposed/leaky hears crisp storm wind (up to 0.56)
		this.volume = Mth.lerp(leakFrac, 0.14f, 0.56f);
		this.pitch = Mth.lerp(leakFrac, 0.85f, 1.08f);
	}
}

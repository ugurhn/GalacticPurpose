package com.galacticpurpose.client.sound;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.wormhole.WormholeState;
import com.galacticpurpose.wormhole.WormholeTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Loud, high-presence custom 44.1 kHz PCM Wormhole Core ambient resonance:
 * smoothly and clearly audible out to 55 blocks (Personal 7x5x7) and 110 blocks (Colossal 45x41x45)
 * with a flat perceptual falloff curve so you hear its unmistakable gravitational hum from far away!
 */
public class WormholeAmbienceSoundInstance extends AbstractTickableSoundInstance {
	private static final SoundEvent SOUND_EVENT = SoundEvent.createVariableRangeEvent(GalacticPurpose.id("wormhole_ambience"));
	private final BlockPos corePos;

	public WormholeAmbienceSoundInstance(BlockPos corePos) {
		super(SOUND_EVENT, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
		this.corePos = corePos.immutable();
		this.looping = true;
		this.delay = 0;
		this.attenuation = Attenuation.NONE;
		this.relative = false;
		this.x = corePos.getX() + 0.5;
		this.y = corePos.getY() + 0.5;
		this.z = corePos.getZ() + 0.5;
		this.volume = 0.05f;
		this.pitch = 0.85f;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
		return CompletableFuture.completedFuture(new ProceduralAudioStreams.ReactorHumStream());
	}

	public static double maxAudibleRange(WormholeTier tier) {
		return tier == WormholeTier.COLOSSAL ? 110.0 : 55.0;
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		if (level == null || mc.player == null) {
			this.stop();
			return;
		}
		BlockEntity be = level.getBlockEntity(this.corePos);
		if (!(be instanceof WormholeCoreBlockEntity core) || core.isRemoved()) {
			this.stop();
			return;
		}
		WormholeState state = core.wormholeState();
		if (state == WormholeState.IDLE) {
			this.stop();
			return;
		}

		boolean colossal = core.activeTier() == WormholeTier.COLOSSAL;
		double maxRange = maxAudibleRange(core.activeTier());
		double dist = Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z));
		if (dist > maxRange + 12.0) {
			this.stop();
			return;
		}

		float norm = Mth.clamp(1.0f - (float) (dist / maxRange), 0.0f, 1.0f);
		// Flatter perceptual power curve (0.80) keeps sound loud & full even 30-70 blocks away!
		float falloff = (float) Math.pow(norm, 0.80);
		float tierGain = colossal ? 2.60f : 1.95f;
		float stateGain = switch (state) {
			case UNSTABLE -> 1.35f;
			case OPEN -> 1.15f;
			case CHARGING -> 0.85f + 0.30f * (core.chargePercent() / 100.0f);
			case SHUTTING_DOWN -> 0.72f;
			case IDLE -> 0.0f;
		};
		this.volume = Math.max(0.01f, falloff * tierGain * stateGain);

		long gt = level.getGameTime();
		float tempFrac = Mth.clamp(core.tempCelsius() / 10_000.0f, 0.0f, 1.0f);
		this.pitch = switch (state) {
			case CHARGING -> 0.72f + 0.35f * (core.chargePercent() / 100.0f);
			case OPEN -> (colossal ? 0.88f : 0.98f) + 0.025f * Mth.sin(gt * 0.06f);
			case UNSTABLE -> 1.05f + tempFrac * 0.55f + 0.08f * Mth.sin(gt * 0.42f);
			case SHUTTING_DOWN -> 0.65f + tempFrac * 0.30f;
			case IDLE -> 0.80f;
		};
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}

package com.galacticpurpose.client.sound;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.registry.ModBlocks;
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

import java.util.concurrent.CompletableFuture;

/**
 * Dynamic Draconic-style continuous reactor drone attached to an active Wormhole Core.
 * Smoothly shifts pitch and intensity based on core temperature, tier (Personal vs Colossal), and instability!
 */
public final class WormholeReactorSoundInstance extends AbstractTickableSoundInstance {
	private static final SoundEvent REACTOR_HUM =
		SoundEvent.createVariableRangeEvent(GalacticPurpose.id("wormhole_reactor_hum"));
	private final BlockPos corePos;

	public WormholeReactorSoundInstance(WormholeCoreBlockEntity core) {
		super(REACTOR_HUM, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
		this.corePos = core.getBlockPos();
		this.x = this.corePos.getX() + 0.5;
		this.y = this.corePos.getY() + 0.5;
		this.z = this.corePos.getZ() + 0.5;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.05f;
		this.pitch = 0.65f;
		this.attenuation = Attenuation.NONE;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
		return CompletableFuture.completedFuture(new ProceduralAudioStreams.ReactorHumStream());
	}

	public BlockPos corePos() {
		return this.corePos;
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			this.stop();
			return;
		}
		if (!mc.level.getBlockState(this.corePos).is(ModBlocks.WORMHOLE_CORE)
			|| !(mc.level.getBlockEntity(this.corePos) instanceof WormholeCoreBlockEntity core)
			|| core.isRemoved()) {
			this.stop();
			return;
		}

		WormholeState state = core.wormholeState();
		boolean colossal = core.activeTier() == WormholeTier.COLOSSAL;
		double maxHearingRange = colossal ? 92.0 : 30.0;
		double dist = Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z));
		if (dist > maxHearingRange || state == WormholeState.IDLE) {
			this.stop();
			return;
		}

		float distFactor = Mth.clamp((float) (1.0 - (dist / maxHearingRange)), 0.0f, 1.0f);
		distFactor = distFactor * distFactor;
		float tempRatio = Mth.clamp(core.tempCelsius() / 10_000.0f, 0.0f, 1.0f);
		float time = mc.level.getGameTime() * 0.05f;

		if (state == WormholeState.UNSTABLE) {
			// Dramatic tension escalation + eerie gravitational pitch bend as meltdown approaches
			float swell = 0.08f * Mth.sin(time * 3.8f);
			this.pitch = Mth.clamp(1.08f + tempRatio * 0.55f + swell, 0.5f, 1.95f);
			this.volume = Math.max(0.02f, distFactor * (colossal ? 1.25f : 0.95f));
		} else if (state == WormholeState.CHARGING || state == WormholeState.SHUTTING_DOWN) {
			float charge = Mth.clamp(core.chargePercent() / 100.0f, 0.0f, 1.0f);
			this.pitch = (colossal ? 0.72f : 0.82f) + charge * 0.25f;
			this.volume = Math.max(0.02f, distFactor * (0.45f + charge * 0.45f));
		} else {
			// OPEN: Majestic cosmic cathedral gravity chord (deeper & grander for Colossal Observatory!)
			this.pitch = (colossal ? 0.88f : 1.00f) + 0.015f * Mth.sin(time * 0.9f);
			this.volume = Math.max(0.02f, distFactor * (colossal ? 1.10f : 0.78f));
		}
	}
}

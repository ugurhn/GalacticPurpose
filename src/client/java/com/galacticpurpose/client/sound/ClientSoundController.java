package com.galacticpurpose.client.sound;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.block.WormholeCoreBlockEntity;
import com.galacticpurpose.entity.SingularityCollapseEntity;
import com.galacticpurpose.network.ClientSoundEventPayload;
import com.galacticpurpose.wormhole.WormholeState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates all client audio streams:
 * - Wide-range Wormhole Core cosmic resonance (55b Personal / 110b Colossal)
 * - Genuine NASA Public Domain OGG rocket launch / ascent / cabin / landing audio (ear-safe 0.22-0.88f)
 * - Calm sci-fi autopilot bridge chime, clean countdown pings, smooth relativistic warp glide, and soft hydraulics
 * - 8-second Singularity Detonation shockwave boom
 */
public final class ClientSoundController {
	private static final Set<Integer> DETONATED_ENTITY_IDS = new HashSet<>();
	private static final Map<BlockPos, WormholeAmbienceSoundInstance> ACTIVE_WORMHOLE_SOUNDS = new HashMap<>();
	private static StarshipEngineSoundInstance activeEngineSound;
	private static SolarStormImpactSoundInstance activeStormSound;
	private static int engineSwitchCooldownTicks = 0;
	private static Object lastWorldRef;

	private ClientSoundController() {
	}

	public static void handleSoundEvent(ClientSoundEventPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager() == null) {
			return;
		}
		// Strictly cap one-shot relative UI/ship sounds <= 0.92f so headphones are NEVER clipped!
		float safeVol = Mth.clamp(payload.volume(), 0.20f, 0.92f);
		OneShotProceduralSoundInstance inst = switch (payload.eventId()) {
			case ClientSoundEventPayload.EVENT_COUNTDOWN_PING -> new OneShotProceduralSoundInstance(
				GalacticPurpose.id("starship_countdown_ping"),
				safeVol,
				payload.pitch(),
				() -> new ProceduralAudioStreams.CountdownTelemetryStream(false)
			);
			case ClientSoundEventPayload.EVENT_IGNITION_GO -> new OneShotProceduralSoundInstance(
				GalacticPurpose.id("starship_countdown_ping"),
				safeVol,
				payload.pitch(),
				() -> new ProceduralAudioStreams.CountdownTelemetryStream(true)
			);
			case ClientSoundEventPayload.EVENT_AUTOPILOT_ENGAGE -> new OneShotProceduralSoundInstance(
				GalacticPurpose.id("starship_autopilot_chime"),
				safeVol,
				payload.pitch(),
				ProceduralAudioStreams.AutopilotEngageChimeStream::new
			);
			case ClientSoundEventPayload.EVENT_WORMHOLE_WARP -> new OneShotProceduralSoundInstance(
				GalacticPurpose.id("wormhole_warp_transit"),
				safeVol,
				payload.pitch(),
				ProceduralAudioStreams.WormholeWarpTransitStream::new
			);
			case ClientSoundEventPayload.EVENT_HYDRAULIC_TOUCHDOWN -> new OneShotProceduralSoundInstance(
				GalacticPurpose.id("starship_touchdown_hydraulics"),
				safeVol,
				payload.pitch(),
				ProceduralAudioStreams.HydraulicTouchdownStream::new
			);
			case ClientSoundEventPayload.EVENT_ABORT_ALARM -> new OneShotProceduralSoundInstance(
				GalacticPurpose.id("starship_abort_alarm"),
				safeVol,
				payload.pitch(),
				ProceduralAudioStreams.AbortAlarmStream::new
			);
			case ClientSoundEventPayload.EVENT_STORM_WARNING -> new OneShotProceduralSoundInstance(
				GalacticPurpose.id("solar_storm_alert"),
				safeVol,
				payload.pitch(),
				ProceduralAudioStreams.SolarStormAlertStream::new
			);
			case ClientSoundEventPayload.EVENT_STORM_STRIKE -> new OneShotProceduralSoundInstance(
				GalacticPurpose.id("solar_storm_impact"),
				safeVol,
				payload.pitch(),
				ProceduralAudioStreams.SolarStormStrikePulseStream::new
			);
			default -> null;
		};
		if (inst != null) {
			mc.getSoundManager().play(inst);
		}
	}

	public static void trackWormholeCore(WormholeCoreBlockEntity core) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || core.isRemoved() || core.wormholeState() == WormholeState.IDLE) {
			return;
		}
		BlockPos pos = core.getBlockPos();
		WormholeAmbienceSoundInstance existing = ACTIVE_WORMHOLE_SOUNDS.get(pos);
		if (existing != null && !existing.isStopped()) {
			return;
		}
		double maxRange = WormholeAmbienceSoundInstance.maxAudibleRange(core.activeTier());
		double distSq = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		if (distSq <= maxRange * maxRange) {
			// Keep at most ONE active streaming Wormhole Ambience source so OpenAL's 8-channel streaming pool never starves Engine audio!
			if (!ACTIVE_WORMHOLE_SOUNDS.isEmpty()) {
				BlockPos bestExisting = null;
				double bestDistSq = Double.MAX_VALUE;
				for (BlockPos existingPos : ACTIVE_WORMHOLE_SOUNDS.keySet()) {
					double ed = mc.player.distanceToSqr(existingPos.getX() + 0.5, existingPos.getY() + 0.5, existingPos.getZ() + 0.5);
					if (ed < bestDistSq) {
						bestDistSq = ed;
						bestExisting = existingPos;
					}
				}
				if (bestExisting != null && bestDistSq <= distSq * 1.15) {
					return;
				}
				for (WormholeAmbienceSoundInstance old : ACTIVE_WORMHOLE_SOUNDS.values()) {
					mc.getSoundManager().stop(old);
				}
				ACTIVE_WORMHOLE_SOUNDS.clear();
			}
			WormholeAmbienceSoundInstance inst = new WormholeAmbienceSoundInstance(pos);
			ACTIVE_WORMHOLE_SOUNDS.put(pos.immutable(), inst);
			mc.getSoundManager().play(inst);
		}
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			reset();
			lastWorldRef = null;
			return;
		}
		if (mc.level != lastWorldRef) {
			lastWorldRef = mc.level;
			if (activeEngineSound != null) {
				mc.getSoundManager().stop(activeEngineSound);
				activeEngineSound = null;
			}
		}

		Iterator<Map.Entry<BlockPos, WormholeAmbienceSoundInstance>> it = ACTIVE_WORMHOLE_SOUNDS.entrySet().iterator();
		while (it.hasNext()) {
			WormholeAmbienceSoundInstance inst = it.next().getValue();
			if (inst.isStopped() || !mc.getSoundManager().isActive(inst)) {
				it.remove();
			}
		}

		AABB box = mc.player.getBoundingBox().inflate(380.0);
		Set<Integer> seenIds = new HashSet<>();
		for (SingularityCollapseEntity collapse : mc.level.getEntitiesOfClass(SingularityCollapseEntity.class, box)) {
			int id = collapse.getId();
			seenIds.add(id);

			if (collapse.isDetonating() && DETONATED_ENTITY_IDS.add(id)) {
				double dist = Math.sqrt(mc.player.distanceToSqr(collapse.getX(), collapse.getY(), collapse.getZ()));
				float vol = Mth.clamp((float) (1.25 - dist / 240.0), 0.35f, 1.25f);
				mc.getSoundManager().play(
					new SingularityDetonationSoundInstance(collapse.getX(), collapse.getY(), collapse.getZ(), vol)
				);
			}
		}
		DETONATED_ENTITY_IDS.retainAll(seenIds);

		if (engineSwitchCooldownTicks > 0) {
			engineSwitchCooldownTicks--;
		}
		StarshipEngineSoundInstance.Profile want = StarshipEngineSoundInstance.desiredProfile(mc);
		if (want != null) {
			boolean profileMismatch = activeEngineSound != null && activeEngineSound.profile() != want;
			if (profileMismatch) {
				mc.getSoundManager().stop(activeEngineSound);
				activeEngineSound = null;
				engineSwitchCooldownTicks = 1;
				return;
			}
			boolean needNew = activeEngineSound == null
				|| activeEngineSound.isStopped()
				|| !mc.getSoundManager().isActive(activeEngineSound);
			if (needNew && engineSwitchCooldownTicks <= 0) {
				if (activeEngineSound != null) {
					mc.getSoundManager().stop(activeEngineSound);
				}
				activeEngineSound = new StarshipEngineSoundInstance(want);
				mc.getSoundManager().play(activeEngineSound);
				engineSwitchCooldownTicks = 2;
			}
		} else if (activeEngineSound != null) {
			mc.getSoundManager().stop(activeEngineSound);
			activeEngineSound = null;
			engineSwitchCooldownTicks = 0;
		}

		boolean stormActive = com.galacticpurpose.client.hud.ClientMarsState.isActiveInDimension()
			&& com.galacticpurpose.client.hud.ClientMarsState.isRedGiantActive()
			&& com.galacticpurpose.client.hud.ClientMarsState.getStormPhase() == com.galacticpurpose.terraforming.SolarStormPhase.ACTIVE;
		if (stormActive) {
			if (activeStormSound == null || activeStormSound.isStopped() || !mc.getSoundManager().isActive(activeStormSound)) {
				activeStormSound = new SolarStormImpactSoundInstance();
				mc.getSoundManager().play(activeStormSound);
			}
		} else if (activeStormSound != null) {
			mc.getSoundManager().stop(activeStormSound);
			activeStormSound = null;
		}
	}

	public static void reset() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.getSoundManager() != null) {
			for (WormholeAmbienceSoundInstance inst : ACTIVE_WORMHOLE_SOUNDS.values()) {
				mc.getSoundManager().stop(inst);
			}
			if (activeEngineSound != null) {
				mc.getSoundManager().stop(activeEngineSound);
			}
			if (activeStormSound != null) {
				mc.getSoundManager().stop(activeStormSound);
			}
		}
		activeEngineSound = null;
		activeStormSound = null;
		ACTIVE_WORMHOLE_SOUNDS.clear();
		DETONATED_ENTITY_IDS.clear();
	}
}

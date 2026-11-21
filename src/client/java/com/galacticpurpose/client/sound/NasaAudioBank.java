package com.galacticpurpose.client.sound;

import com.galacticpurpose.GalacticPurpose;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

/**
 * Decodes, trims silent intros from, normalizes, and seamlessly loop-crossfades real NASA
 * public-domain propulsion recordings into cached 16-bit PCM buffers so Starship ignition
 * and ascent audio slams in immediately at t=0.00s without dead lead-in silence.
 */
public final class NasaAudioBank {
	private static final String LAUNCH_OGG_PATH = "/assets/galacticpurpose/sounds/rocket_launch_heavy_nasa.ogg";
	private static final String ASCENT_OGG_PATH = "/assets/galacticpurpose/sounds/rocket_ascent_nasa.ogg";
	private static final Map<StarshipEngineSoundInstance.Profile, CachedTrack> CACHE =
			new EnumMap<>(StarshipEngineSoundInstance.Profile.class);

	private record CachedTrack(short[] pcm, int sampleRate) {}

	private NasaAudioBank() {
	}

	public static void preloadAllAsync() {
		Thread t = new Thread(() -> {
			for (StarshipEngineSoundInstance.Profile p : StarshipEngineSoundInstance.Profile.values()) {
				synchronized (NasaAudioBank.class) {
					CACHE.computeIfAbsent(p, NasaAudioBank::buildTrack);
				}
			}
		}, "GalacticPurpose-NASA-Audio-Preload");
		t.setDaemon(true);
		t.start();
	}

	public static synchronized AudioStream openLoopStream(StarshipEngineSoundInstance.Profile profile) {
		CachedTrack track = CACHE.computeIfAbsent(profile, NasaAudioBank::buildTrack);
		return new LoopingPcmStream(track.pcm(), track.sampleRate());
	}

	private static CachedTrack buildTrack(StarshipEngineSoundInstance.Profile profile) {
		return switch (profile) {
			case LAUNCH_HEAVY -> loadNormalizedLoop(LAUNCH_OGG_PATH, 0.048f, 0.28f, false);
			case ASCENT_THRUST -> loadNormalizedLoop(ASCENT_OGG_PATH, 0.032f, 0.28f, false);
			case CABIN_AND_DESCENT -> loadNormalizedLoop(ASCENT_OGG_PATH, 0.032f, 0.23f, true);
		};
	}

	private static CachedTrack loadNormalizedLoop(
			String classpathLocation,
			float trimThreshold,
			float targetRms,
			boolean lowPassForCabin
	) {
		try {
			DecodedPcm raw = decodeOggMono(classpathLocation);
			if (raw == null || raw.samples.length < 4096) {
				return fallbackTrack(lowPassForCabin ? 0.18f : 0.24f);
			}
			float[] f = new float[raw.samples.length];
			for (int i = 0; i < raw.samples.length; i++) {
				f[i] = raw.samples[i] / 32768.0f;
			}
			float[] trimmed = trimSilentIntroAndTail(f, raw.sampleRate, trimThreshold);
			if (lowPassForCabin) {
				trimmed = applyWarmLowPass(trimmed, raw.sampleRate, 950.0f);
			}
			float[] normalized = normalizeAndSoftLimit(trimmed, targetRms);
			short[] looped = crossfadeLoop(normalized, raw.sampleRate, 1.4f);
			return new CachedTrack(looped, raw.sampleRate);
		} catch (Throwable t) {
			GalacticPurpose.LOGGER.warn("Failed to prepare NASA audio track {}; using clean fallback", classpathLocation, t);
			return fallbackTrack(lowPassForCabin ? 0.18f : 0.24f);
		}
	}

	private record DecodedPcm(short[] samples, int sampleRate) {}

	private static DecodedPcm decodeOggMono(String classpathLocation) throws IOException {
		byte[] bytes;
		try (InputStream in = NasaAudioBank.class.getResourceAsStream(classpathLocation)) {
			if (in == null) {
				return null;
			}
			bytes = in.readAllBytes();
		}
		ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
		ShortBuffer decoded = null;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			encoded.put(bytes).flip();
			IntBuffer channelsBuf = stack.mallocInt(1);
			IntBuffer rateBuf = stack.mallocInt(1);
			decoded = STBVorbis.stb_vorbis_decode_memory(encoded, channelsBuf, rateBuf);
			if (decoded == null) {
				return null;
			}
			int channels = Math.max(1, channelsBuf.get(0));
			int sampleRate = Math.max(22050, rateBuf.get(0));
			int frames = decoded.limit() / channels;
			short[] mono = new short[frames];
			for (int i = 0; i < frames; i++) {
				int sum = 0;
				int base = i * channels;
				for (int c = 0; c < channels; c++) {
					sum += decoded.get(base + c);
				}
				mono[i] = (short) (sum / channels);
			}
			return new DecodedPcm(mono, sampleRate);
		} finally {
			MemoryUtil.memFree(encoded);
			if (decoded != null) {
				LibCStdlib.free(decoded);
			}
		}
	}

	private static float[] trimSilentIntroAndTail(float[] samples, int sampleRate, float rmsThreshold) {
		int window = Math.max(512, sampleRate / 12);
		int startIdx = 0;
		for (int i = 0; i + window <= samples.length; i += window / 4) {
			if (windowRms(samples, i, window) >= rmsThreshold) {
				// Step back 120ms for a clean natural attack transient without dead air.
				startIdx = Math.max(0, i - (int) (sampleRate * 0.12f));
				break;
			}
		}

		int endIdx = samples.length;
		for (int i = samples.length - window; i > startIdx + sampleRate; i -= window / 4) {
			if (windowRms(samples, i, window) >= rmsThreshold * 0.65f) {
				endIdx = Math.min(samples.length, i + window);
				break;
			}
		}

		int minLen = Math.min(samples.length - startIdx, sampleRate * 28);
		int len = Math.max(Math.min(endIdx - startIdx, sampleRate * 32), minLen);
		len = Math.min(len, samples.length - startIdx);
		if (len < sampleRate) {
			return samples;
		}
		float[] out = new float[len];
		System.arraycopy(samples, startIdx, out, 0, len);
		// Short 80ms fade-in so start sample never clicks when OpenAL begins playback.
		int attackSamples = Math.min(len / 4, (int) (sampleRate * 0.08f));
		for (int i = 0; i < attackSamples; i++) {
			out[i] *= (float) i / Math.max(1, attackSamples);
		}
		return out;
	}

	private static float windowRms(float[] s, int offset, int count) {
		double sumSq = 0.0;
		for (int i = 0; i < count; i++) {
			float v = s[offset + i];
			sumSq += v * v;
		}
		return (float) Math.sqrt(sumSq / count);
	}

	private static float[] applyWarmLowPass(float[] samples, int sampleRate, float cutoffHz) {
		float[] out = new float[samples.length];
		double rc = 1.0 / (2.0 * Math.PI * cutoffHz);
		double dt = 1.0 / sampleRate;
		float alpha = (float) (dt / (rc + dt));
		float y1 = 0.0f;
		float y2 = 0.0f;
		for (int i = 0; i < samples.length; i++) {
			y1 += alpha * (samples[i] - y1);
			y2 += alpha * (y1 - y2);
			out[i] = y2;
		}
		return out;
	}

	private static float[] normalizeAndSoftLimit(float[] samples, float targetRms) {
		double sumSq = 0.0;
		for (float v : samples) {
			sumSq += v * v;
		}
		float currentRms = (float) Math.sqrt(sumSq / Math.max(1, samples.length));
		float gain = currentRms > 1.0e-4f ? Math.min(8.5f, targetRms / currentRms) : 1.0f;
		float[] out = new float[samples.length];
		for (int i = 0; i < samples.length; i++) {
			double scaled = samples[i] * gain;
			// Soft tanh limiter avoids digital clipping harshness on turbine spikes.
			out[i] = (float) (Math.tanh(scaled * 1.15) * 0.88);
		}
		return out;
	}

	private static short[] crossfadeLoop(float[] samples, int sampleRate, float overlapSeconds) {
		int fade = Math.min(samples.length / 4, Math.max(1024, (int) (sampleRate * overlapSeconds)));
		int outLen = samples.length - fade;
		if (outLen <= fade) {
			short[] raw = new short[samples.length];
			for (int i = 0; i < samples.length; i++) {
				raw[i] = (short) Math.max(-32767, Math.min(32767, Math.round(samples[i] * 32767.0f)));
			}
			return raw;
		}
		short[] loop = new short[outLen];
		for (int i = 0; i < outLen; i++) {
			float sample;
			if (i < fade) {
				float t = (float) i / fade;
				float wIn = (float) Math.sin(t * Math.PI * 0.5);
				float wOut = (float) Math.cos(t * Math.PI * 0.5);
				sample = samples[i] * wIn + samples[outLen + i] * wOut;
			} else {
				sample = samples[i];
			}
			loop[i] = (short) Math.max(-32767, Math.min(32767, Math.round(sample * 32767.0f)));
		}
		return loop;
	}

	private static CachedTrack fallbackTrack(float amplitude) {
		int rate = 44100;
		int len = rate * 4;
		short[] pcm = new short[len];
		long seed = 0x5EEDBEEFL;
		float lp = 0.0f;
		for (int i = 0; i < len; i++) {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			float noise = ((seed >> 33) / (float) Integer.MAX_VALUE);
			lp += 0.035f * (noise - lp);
			float rumble = (float) Math.sin(2.0 * Math.PI * 55.0 * i / rate) * 0.35f + lp * 2.1f;
			pcm[i] = (short) Math.max(-32767, Math.min(32767, Math.round(rumble * amplitude * 32767.0f)));
		}
		return new CachedTrack(pcm, rate);
	}

	private static final class LoopingPcmStream implements AudioStream {
		private final short[] pcm;
		private final AudioFormat format;
		private int cursor;

		private LoopingPcmStream(short[] pcm, int sampleRate) {
			this.pcm = pcm;
			this.format = new AudioFormat((float) sampleRate, 16, 1, true, false);
		}

		@Override
		public AudioFormat getFormat() {
			return this.format;
		}

		@Override
		public ByteBuffer read(int size) {
			int samplesToRead = Math.max(1, size / 2);
			ByteBuffer buf = BufferUtils.createByteBuffer(samplesToRead * 2).order(ByteOrder.LITTLE_ENDIAN);
			for (int i = 0; i < samplesToRead; i++) {
				buf.putShort(this.pcm[this.cursor]);
				this.cursor++;
				if (this.cursor >= this.pcm.length) {
					this.cursor = 0;
				}
			}
			buf.flip();
			return buf;
		}

		@Override
		public void close() {
			// PCM buffers stay cached in RAM across flight phases.
		}
	}
}

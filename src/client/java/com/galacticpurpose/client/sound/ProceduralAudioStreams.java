package com.galacticpurpose.client.sound;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * Pure-Java 44.1 kHz 16-bit PCM ear-safe mastered cosmic & cockpit audio streams (MIT-clean):
 * Rocket liftoff/climb/descent/cabin engines now use genuine NASA Public Domain OGG recordings in sounds.json!
 * These streams handle cosmic wormhole ambience, soft aerospace cockpit chimes, smooth warp transitions, and hydraulics.
 */
public final class ProceduralAudioStreams {
	public static final float SAMPLE_RATE = 44100.0f;
	public static final AudioFormat MONO_16_PCM = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

	private ProceduralAudioStreams() {
	}

	private static final class CosmicReverb {
		private final double[][] lines;
		private final int[] pos;
		private final double[] lp;
		private final double feedback;

		CosmicReverb(double feedback) {
			int[] lengths = {3517, 5821, 8807, 12799};
			this.lines = new double[lengths.length][];
			this.pos = new int[lengths.length];
			this.lp = new double[lengths.length];
			this.feedback = feedback;
			for (int i = 0; i < lengths.length; i++) {
				this.lines[i] = new double[lengths[i]];
			}
		}

		double process(double input) {
			double sum = 0.0;
			for (int i = 0; i < this.lines.length; i++) {
				double[] buf = this.lines[i];
				int idx = this.pos[i];
				double delayed = buf[idx];
				this.lp[i] = this.lp[i] * 0.72 + delayed * 0.28;
				buf[idx] = input + this.lp[i] * this.feedback;
				this.pos[i] = (idx + 1) % buf.length;
				sum += this.lp[i];
			}
			return sum * 0.30;
		}
	}

	/**
	 * Rich, velvety cosmic Wormhole Core resonance audible smoothly across wide distances without distortion.
	 */
	public static final class ReactorHumStream implements AudioStream {
		private long sampleIndex = 0L;
		private final CosmicReverb reverb = new CosmicReverb(0.56);
		private double windLp = 0.0;
		private int rng = 0x5A3C91E7;
		private boolean closed = false;

		@Override
		public AudioFormat getFormat() {
			return MONO_16_PCM;
		}

		@Override
		public ByteBuffer read(int size) {
			if (this.closed) {
				return null;
			}
			int sampleCount = Math.max(256, size / 2);
			ByteBuffer buf = BufferUtils.createByteBuffer(sampleCount * 2);
			for (int i = 0; i < sampleCount; i++) {
				double t = this.sampleIndex / (double) SAMPLE_RATE;
				this.sampleIndex++;

				double fadeIn = Math.min(1.0, t / 1.2);
				double breath1 = 0.76 + 0.24 * Math.sin(2.0 * Math.PI * 0.065 * t);
				double breath2 = 0.68 + 0.32 * Math.sin(2.0 * Math.PI * 0.095 * t + 1.4);
				double breath3 = 0.58 + 0.42 * Math.sin(2.0 * Math.PI * 0.043 * t + 3.1);

				double d1 = Math.sin(2.0 * Math.PI * 36.71 * t) * 0.40;
				double d2 = Math.sin(2.0 * Math.PI * 73.42 * t) * 0.30 * breath1;
				double a2 = Math.sin(2.0 * Math.PI * 110.00 * t) * 0.20 * breath2;
				double d3 = Math.sin(2.0 * Math.PI * 146.83 * t + 0.25 * Math.sin(2.0 * Math.PI * 0.22 * t)) * 0.14 * breath1;
				double f3 = Math.sin(2.0 * Math.PI * 174.61 * t) * 0.09 * breath3;
				double a3 = Math.sin(2.0 * Math.PI * 220.25 * t) * 0.07 * breath2;

				double shimmer = (Math.sin(2.0 * Math.PI * 293.66 * t)
					+ Math.sin(2.0 * Math.PI * 294.20 * t)
					+ 0.50 * Math.sin(2.0 * Math.PI * 440.50 * t)) * 0.032 * breath3;

				double white = nextWhite();
				this.windLp = this.windLp * 0.985 + white * 0.015;
				double cosmicWind = this.windLp * 0.28 * breath2;

				double dry = d1 + d2 + a2 + d3 + f3 + a3 + shimmer + cosmicWind;
				double wet = this.reverb.process(d2 * 0.50 + a2 + d3 + f3 + a3 + shimmer * 1.6 + cosmicWind);
				double out = Math.tanh((dry * 0.65 + wet * 0.55) * 1.15) * 0.85 * fadeIn;
				short pcm = (short) Math.max(-32767, Math.min(32767, Math.round(out * 32767.0)));
				buf.putShort(pcm);
			}
			buf.flip();
			return buf;
		}

		private double nextWhite() {
			this.rng ^= (this.rng << 13);
			this.rng ^= (this.rng >>> 17);
			this.rng ^= (this.rng << 5);
			return (this.rng / (double) Integer.MAX_VALUE);
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}

	/**
	 * Clean, gentle aerospace cockpit countdown ping (T-10..T-1) or smooth T-0 GO confirmation chord (NO harsh blast!).
	 */
	public static final class CountdownTelemetryStream implements AudioStream {
		private final boolean ignitionGo;
		private final int totalSamples;
		private int cursor = 0;
		private boolean closed = false;

		public CountdownTelemetryStream(boolean ignitionGo) {
			this.ignitionGo = ignitionGo;
			this.totalSamples = (int) (SAMPLE_RATE * (ignitionGo ? 0.85f : 0.26f));
		}

		@Override
		public AudioFormat getFormat() {
			return MONO_16_PCM;
		}

		@Override
		public ByteBuffer read(int size) {
			if (this.closed || this.cursor >= this.totalSamples) {
				return null;
			}
			int remaining = this.totalSamples - this.cursor;
			int sampleCount = Math.min(remaining, Math.max(256, size / 2));
			ByteBuffer buf = BufferUtils.createByteBuffer(sampleCount * 2);

			for (int i = 0; i < sampleCount; i++) {
				double t = this.cursor / (double) SAMPLE_RATE;
				this.cursor++;

				double out;
				if (!this.ignitionGo) {
					double env = Math.min(1.0, t / 0.012) * Math.exp(-t * 14.0);
					double tone = Math.sin(2.0 * Math.PI * 820.0 * t) * 0.58
						+ Math.sin(2.0 * Math.PI * 1230.0 * t) * 0.16;
					out = tone * env * 0.72;
				} else {
					// Clean triumphant aerospace GO chord (zero loud explosion jumpscare!)
					double env = Math.min(1.0, t / 0.018) * Math.exp(-t * 3.6);
					double chord = Math.sin(2.0 * Math.PI * 587.33 * t) * 0.42
						+ Math.sin(2.0 * Math.PI * 880.00 * t) * 0.34
						+ Math.sin(2.0 * Math.PI * 1174.66 * t) * 0.18;
					out = chord * env * 0.75;
				}
				short pcm = (short) Math.max(-32767, Math.min(32767, Math.round(out * 32767.0)));
				buf.putShort(pcm);
			}
			buf.flip();
			return buf;
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}

	/**
	 * Calm, futuristic bridge autopilot engagement chime when pressing [G] or clicking Bridge Helm (zero scary explosion!).
	 */
	public static final class AutopilotEngageChimeStream implements AudioStream {
		private static final int TOTAL_SAMPLES = (int) (SAMPLE_RATE * 1.45f);
		private int cursor = 0;
		private final CosmicReverb reverb = new CosmicReverb(0.45);
		private boolean closed = false;

		@Override
		public AudioFormat getFormat() {
			return MONO_16_PCM;
		}

		@Override
		public ByteBuffer read(int size) {
			if (this.closed || this.cursor >= TOTAL_SAMPLES) {
				return null;
			}
			int remaining = TOTAL_SAMPLES - this.cursor;
			int sampleCount = Math.min(remaining, Math.max(256, size / 2));
			ByteBuffer buf = BufferUtils.createByteBuffer(sampleCount * 2);

			for (int i = 0; i < sampleCount; i++) {
				double t = this.cursor / (double) SAMPLE_RATE;
				this.cursor++;

				// Soft ascending two-note sci-fi bridge confirmation (D4 -> A4) + warm sub-pad
				double note1 = Math.sin(2.0 * Math.PI * 293.66 * t) * Math.min(1.0, t / 0.03) * Math.exp(-t * 4.5) * 0.38;
				double t2 = Math.max(0.0, t - 0.16);
				double note2 = t >= 0.16
					? (Math.sin(2.0 * Math.PI * 440.0 * t2) * 0.34 + Math.sin(2.0 * Math.PI * 659.25 * t2) * 0.16)
						* Math.min(1.0, t2 / 0.03) * Math.exp(-t2 * 2.8)
					: 0.0;
				double pad = Math.sin(2.0 * Math.PI * 110.0 * t) * Math.min(1.0, t / 0.15) * Math.exp(-t * 1.6) * 0.22;

				double dry = note1 + note2 + pad;
				double wet = this.reverb.process(dry);
				double tail = t > 1.15 ? Math.max(0.0, (1.45 - t) / 0.30) : 1.0;
				double out = (dry * 0.65 + wet * 0.45) * 0.72 * tail;
				short pcm = (short) Math.max(-32767, Math.min(32767, Math.round(out * 32767.0)));
				buf.putShort(pcm);
			}
			buf.flip();
			return buf;
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}

	/**
	 * Smooth, velvety 3.0-second relativistic space-time harmonic glide (zero ear-piercing loud explosion!).
	 */
	public static final class WormholeWarpTransitStream implements AudioStream {
		private static final int TOTAL_SAMPLES = (int) (SAMPLE_RATE * 3.0f);
		private int cursor = 0;
		private double bassPhase = 0.0;
		private double sweepPhase = 0.0;
		private double lp = 0.0;
		private final CosmicReverb reverb = new CosmicReverb(0.55);
		private int rng = 0x41B9D723;
		private boolean closed = false;

		@Override
		public AudioFormat getFormat() {
			return MONO_16_PCM;
		}

		@Override
		public ByteBuffer read(int size) {
			if (this.closed || this.cursor >= TOTAL_SAMPLES) {
				return null;
			}
			int remaining = TOTAL_SAMPLES - this.cursor;
			int sampleCount = Math.min(remaining, Math.max(256, size / 2));
			ByteBuffer buf = BufferUtils.createByteBuffer(sampleCount * 2);

			for (int i = 0; i < sampleCount; i++) {
				double t = this.cursor / (double) SAMPLE_RATE;
				this.cursor++;

				// Smooth warm bass glide (96 Hz -> 36 Hz, soft 180ms fade-in so zero jump-scare!)
				double bassFreq = 36.0 + 60.0 * Math.exp(-t * 2.1);
				this.bassPhase += (2.0 * Math.PI * bassFreq) / SAMPLE_RATE;
				double bassEnv = Math.min(1.0, t / 0.22) * Math.exp(-t * 0.85);
				double bassGlide = Math.sin(this.bassPhase) * bassEnv * 0.56;

				// Ethereal cosmic chord shimmer (D3 + A3 + D4)
				double sweepFreq = 196.0 + 180.0 * Math.exp(-Math.pow((t - 0.65) * 2.0, 2.0));
				this.sweepPhase += (2.0 * Math.PI * sweepFreq) / SAMPLE_RATE;
				double harmonicEnv = Math.min(1.0, t / 0.28) * Math.exp(-t * 0.92);
				double shimmer = (Math.sin(this.sweepPhase) * 0.24
					+ Math.sin(2.0 * Math.PI * 146.83 * t) * 0.20
					+ Math.sin(2.0 * Math.PI * 220.00 * t) * 0.14) * harmonicEnv;

				// Soft velvety space-wind rush (heavily low-passed -> zero harsh static!)
				double white = nextWhite();
				double cut = 0.015 + 0.055 * Math.exp(-Math.pow((t - 0.45) * 2.2, 2.0));
				this.lp += (white - this.lp) * cut;
				double whoosh = this.lp * 1.45 * Math.min(1.0, t / 0.20) * Math.exp(-t * 1.0);

				double tailFade = t > 2.3 ? Math.max(0.0, (3.0 - t) / 0.7) : 1.0;
				double dry = bassGlide + shimmer + whoosh;
				double wet = this.reverb.process(shimmer * 1.3 + whoosh * 0.6);
				double out = Math.tanh((dry * 0.68 + wet * 0.45) * 1.05) * 0.74 * tailFade;
				short pcm = (short) Math.max(-32767, Math.min(32767, Math.round(out * 32767.0)));
				buf.putShort(pcm);
			}
			buf.flip();
			return buf;
		}

		private double nextWhite() {
			this.rng ^= (this.rng << 13);
			this.rng ^= (this.rng >>> 17);
			this.rng ^= (this.rng << 5);
			return (this.rng / (double) Integer.MAX_VALUE);
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}

	/**
	 * Controlled, ear-safe mechanical landing-skid lock + gentle pneumatic release hiss on planetary touchdown.
	 */
	public static final class HydraulicTouchdownStream implements AudioStream {
		private static final int TOTAL_SAMPLES = (int) (SAMPLE_RATE * 1.35f);
		private int cursor = 0;
		private double thudPhase = 0.0;
		private double hp = 0.0;
		private double lp = 0.0;
		private int rng = 0x2B94F031;
		private boolean closed = false;

		@Override
		public AudioFormat getFormat() {
			return MONO_16_PCM;
		}

		@Override
		public ByteBuffer read(int size) {
			if (this.closed || this.cursor >= TOTAL_SAMPLES) {
				return null;
			}
			int remaining = TOTAL_SAMPLES - this.cursor;
			int sampleCount = Math.min(remaining, Math.max(256, size / 2));
			ByteBuffer buf = BufferUtils.createByteBuffer(sampleCount * 2);

			for (int i = 0; i < sampleCount; i++) {
				double t = this.cursor / (double) SAMPLE_RATE;
				this.cursor++;

				double thudFreq = 36.0 + 52.0 * Math.exp(-t * 14.0);
				this.thudPhase += (2.0 * Math.PI * thudFreq) / SAMPLE_RATE;
				double thud = Math.sin(this.thudPhase) * Math.min(1.0, t / 0.025) * Math.exp(-t * 8.5) * 0.58;

				double white = nextWhite();
				this.hp = this.hp * 0.86 + white * 0.14;
				double highNoise = white - this.hp;
				this.lp += (highNoise - this.lp) * 0.14;
				double hissEnv = t > 0.10 ? Math.min(1.0, (t - 0.10) / 0.12) * Math.exp(-(t - 0.10) * 2.8) : 0.0;
				double hiss = this.lp * 0.72 * hissEnv;

				double out = (thud + hiss) * 0.70;
				short pcm = (short) Math.max(-32767, Math.min(32767, Math.round(out * 32767.0)));
				buf.putShort(pcm);
			}
			buf.flip();
			return buf;
		}

		private double nextWhite() {
			this.rng ^= (this.rng << 13);
			this.rng ^= (this.rng >>> 17);
			this.rng ^= (this.rng << 5);
			return (this.rng / (double) Integer.MAX_VALUE);
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}

	/**
	 * Clear cockpit abort warning dual-tone alarm for [SIGNAL LOST] / [LOW FUEL].
	 */
	public static final class AbortAlarmStream implements AudioStream {
		private static final int TOTAL_SAMPLES = (int) (SAMPLE_RATE * 0.75f);
		private int cursor = 0;
		private boolean closed = false;

		@Override
		public AudioFormat getFormat() {
			return MONO_16_PCM;
		}

		@Override
		public ByteBuffer read(int size) {
			if (this.closed || this.cursor >= TOTAL_SAMPLES) {
				return null;
			}
			int remaining = TOTAL_SAMPLES - this.cursor;
			int sampleCount = Math.min(remaining, Math.max(256, size / 2));
			ByteBuffer buf = BufferUtils.createByteBuffer(sampleCount * 2);

			for (int i = 0; i < sampleCount; i++) {
				double t = this.cursor / (double) SAMPLE_RATE;
				this.cursor++;

				boolean pulse1 = t < 0.24;
				boolean pulse2 = t >= 0.34 && t < 0.68;
				double out = 0.0;
				if (pulse1 || pulse2) {
					double lt = pulse1 ? t : (t - 0.34);
					double freq = pulse1 ? 580.0 : 430.0;
					double env = Math.min(1.0, lt / 0.015) * Math.exp(-lt * 5.0);
					out = Math.sin(2.0 * Math.PI * freq * t) * env * 0.62;
				}
				short pcm = (short) Math.max(-32767, Math.min(32767, Math.round(out * 32767.0)));
				buf.putShort(pcm);
			}
			buf.flip();
			return buf;
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}

	public static final class CollapseWarpStream implements AudioStream {
		private long sampleIndex = 0L;
		private final double[] shepardPhases = new double[6];
		private final CosmicReverb reverb = new CosmicReverb(0.60);
		private double lpNoise = 0.0;
		private int rng = 0x19F4B823;
		private boolean closed = false;

		@Override
		public AudioFormat getFormat() {
			return MONO_16_PCM;
		}

		@Override
		public ByteBuffer read(int size) {
			if (this.closed) {
				return null;
			}
			int sampleCount = Math.max(256, size / 2);
			ByteBuffer buf = BufferUtils.createByteBuffer(sampleCount * 2);
			for (int i = 0; i < sampleCount; i++) {
				double t = this.sampleIndex / (double) SAMPLE_RATE;
				this.sampleIndex++;

				double fadeIn = Math.min(1.0, t / 0.8);
				double sweepCycle = (t * 0.095) % 1.0;
				double shepard = 0.0;
				for (int k = 0; k < 6; k++) {
					double u = ((k / 6.0) + sweepCycle) % 1.0;
					double freq = 55.0 * Math.pow(2.0, u * 5.2);
					double distFromCenter = (u - 0.5) * 3.1;
					double bellAmp = Math.exp(-distFromCenter * distFromCenter);
					this.shepardPhases[k] += (2.0 * Math.PI * freq) / SAMPLE_RATE;
					shepard += Math.sin(this.shepardPhases[k]) * bellAmp * 0.15;
				}

				double swell = 0.55 + 0.45 * Math.sin(2.0 * Math.PI * 0.36 * t);
				double subDread = Math.sin(2.0 * Math.PI * 36.71 * t) * 0.42
					+ Math.sin(2.0 * Math.PI * 51.91 * t) * 0.16 * swell
					+ Math.sin(2.0 * Math.PI * 77.78 * t) * 0.11 * swell;

				double white = nextWhite();
				this.lpNoise = this.lpNoise * 0.96 + white * 0.04;
				double vortexWind = this.lpNoise * 0.30 * swell;

				double dry = subDread + shepard + vortexWind;
				double wet = this.reverb.process(shepard * 1.4 + subDread * 0.4 + vortexWind);
				double out = Math.tanh((dry * 0.65 + wet * 0.58) * 1.25) * 0.88 * fadeIn;
				short pcm = (short) Math.max(-32767, Math.min(32767, Math.round(out * 32767.0)));
				buf.putShort(pcm);
			}
			buf.flip();
			return buf;
		}

		private double nextWhite() {
			this.rng ^= (this.rng << 13);
			this.rng ^= (this.rng >>> 17);
			this.rng ^= (this.rng << 5);
			return (this.rng / (double) Integer.MAX_VALUE);
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}

	public static final class CataclysmicDetonationStream implements AudioStream {
		private static final int TOTAL_SAMPLES = (int) (SAMPLE_RATE * 8.0f);
		private int cursor = 0;
		private double subPhase = 0.0;
		private double lp1 = 0.0;
		private double lp2 = 0.0;
		private final CosmicReverb reverb = new CosmicReverb(0.62);
		private int rng = 0x7E21C49B;
		private boolean closed = false;

		@Override
		public AudioFormat getFormat() {
			return MONO_16_PCM;
		}

		@Override
		public ByteBuffer read(int size) {
			if (this.closed || this.cursor >= TOTAL_SAMPLES) {
				return null;
			}
			int remaining = TOTAL_SAMPLES - this.cursor;
			int sampleCount = Math.min(remaining, Math.max(256, size / 2));
			ByteBuffer buf = BufferUtils.createByteBuffer(sampleCount * 2);

			for (int i = 0; i < sampleCount; i++) {
				double t = this.cursor / (double) SAMPLE_RATE;
				this.cursor++;

				double white = nextWhite();
				double crack = white * Math.exp(-t * 15.0) * 0.88;
				double subFreq = 22.0 + 96.0 * Math.exp(-t * 2.2);
				this.subPhase += (2.0 * Math.PI * subFreq) / SAMPLE_RATE;
				double subEnv = Math.min(1.0, t * 50.0) * Math.exp(-t * 0.56);
				double subBoom = Math.sin(this.subPhase) * subEnv * 0.92;

				double cutoff = 0.012 + 0.16 * Math.exp(-t * 0.95);
				this.lp1 += (white - this.lp1) * cutoff;
				this.lp2 += (this.lp1 - this.lp2) * (cutoff * 1.35);
				double roarEnv = Math.min(1.0, t * 9.0) * Math.exp(-t * 0.36);
				double roar = this.lp2 * 4.5 * roarEnv;

				double chordEnv = Math.min(1.0, t * 2.5) * Math.exp(-t * 0.42);
				double aftermath = (Math.sin(2.0 * Math.PI * 73.42 * t) * 0.15
					+ Math.sin(2.0 * Math.PI * 110.00 * t) * 0.09
					+ Math.sin(2.0 * Math.PI * 174.61 * t) * 0.05) * chordEnv;

				double tailFade = t > 7.0 ? Math.max(0.0, (8.0 - t)) : 1.0;
				double dry = crack + subBoom + roar + aftermath;
				double wet = this.reverb.process(crack * 0.6 + roar * 0.7 + aftermath * 1.5);
				double mixed = (dry * 0.72 + wet * 0.45) * tailFade;
				double saturated = Math.tanh(mixed * 1.35) * 0.92;
				short pcm = (short) Math.max(-32767, Math.min(32767, Math.round(saturated * 32767.0)));
				buf.putShort(pcm);
			}
			buf.flip();
			return buf;
		}

		private double nextWhite() {
			this.rng ^= (this.rng << 13);
			this.rng ^= (this.rng >>> 17);
			this.rng ^= (this.rng << 5);
			return (this.rng / (double) Integer.MAX_VALUE);
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}
}

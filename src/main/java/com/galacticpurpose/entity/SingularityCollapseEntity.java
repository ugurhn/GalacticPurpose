package com.galacticpurpose.entity;

import com.galacticpurpose.registry.ModEntities;
import com.galacticpurpose.wormhole.WormholeTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Uncontained Wormhole Singularity collapse & cataclysmic progressive shockwave detonation:
 * - Grows up to 10x (up to 76-block diameter apocalyptic black hole high in the sky!).
 * - Restores terrifying vanilla Minecraft collapse sounds (Wither Spawn, Warden Roar/Heartbeat, Beacon Deactivate, Sonic Charge/Boom, Dragon Death) + sub-bass detonation shockwave!
 * - Real-time synced (hurtMarked=true) direct gravitational funnel suction pulling ground blocks smoothly UP and STRAIGHT INTO the black hole!
 * - TPS-safe 144-block wide clean scorched caldera & colossal animated shockwave dome.
 */
public class SingularityCollapseEntity extends Entity {
	public static final int RISE_PHASE_END = 135;
	public static final int VORTEX_PHASE_END = 275;
	public static final int DETONATION_TICK = 320;
	public static final int CARVE_END_TICK = 475;
	public static final int FINISH_TICK = 495;

	// Up to 180 real airborne blocks sucked up simultaneously + 140 zero-lag 144-FPS client debris chunks!
	private static final int MAX_ACTIVE_FALLING_BLOCKS = 180;

	private static final EntityDataAccessor<Integer> DATA_AGE =
		SynchedEntityData.defineId(SingularityCollapseEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_BASE_RADIUS =
		SynchedEntityData.defineId(SingularityCollapseEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_BLAST_RADIUS =
		SynchedEntityData.defineId(SingularityCollapseEntity.class, EntityDataSerializers.INT);

	private int absorbedBlocks = 0;
	private double craterCenterY = Double.NaN;
	private float lastCarvedRadius = 0.0f;
	private double ripSpiralAngle = 0.0;
	private double ripSpiralRadius = 4.0;

	public SingularityCollapseEntity(EntityType<? extends SingularityCollapseEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public static SingularityCollapseEntity spawnAt(ServerLevel level, Vec3 center) {
		return spawnForTier(level, center, WormholeTier.PERSONAL);
	}

	public static SingularityCollapseEntity spawnForTier(ServerLevel level, Vec3 center, WormholeTier tier) {
		return spawnCustom(level, center, tier.coreRadius(), tier.collapseBlastRadius());
	}

	public static SingularityCollapseEntity spawnCustom(
		ServerLevel level,
		Vec3 center,
		float baseRadius,
		int blastRadius
	) {
		SingularityCollapseEntity entity = new SingularityCollapseEntity(ModEntities.SINGULARITY_COLLAPSE, level);
		entity.setPos(center.x, center.y, center.z);
		entity.entityData.set(DATA_BASE_RADIUS, Math.max(0.8f, baseRadius));
		entity.entityData.set(DATA_BLAST_RADIUS, Math.max(6, Math.min(120, blastRadius)));
		entity.craterCenterY = center.y;
		level.addFreshEntity(entity);
		return entity;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_AGE, 0);
		builder.define(DATA_BASE_RADIUS, 1.25f);
		builder.define(DATA_BLAST_RADIUS, 56);
	}

	public int collapseAge() {
		return this.entityData.get(DATA_AGE);
	}

	public float baseRadius() {
		return this.entityData.get(DATA_BASE_RADIUS);
	}

	public int blastRadius() {
		return this.entityData.get(DATA_BLAST_RADIUS);
	}

	public int absorbedBlocks() {
		return this.absorbedBlocks;
	}

	public boolean isDetonating() {
		return collapseAge() >= DETONATION_TICK;
	}

	/**
	 * Apocalyptic sky-eating growth:
	 * - Personal (1.25 -> 18.75 radius = 37.5-block wide black hole: 15x!)
	 * - Colossal (10.2 -> 59.16 radius = 118-block wide apocalyptic sky titan!)
	 * followed by sudden violent implosion crush down to a blinding white pinpoint!
	 */
	public float visualCoreRadius(float partialTick) {
		float age = collapseAge() + partialTick;
		float startR = baseRadius();
		float peakR = startR > 4.0f
			? Math.min(62.0f, startR * 5.8f)
			: Math.min(20.0f, startR * 15.0f);
		if (age < RISE_PHASE_END) {
			float t = Mth.clamp(age / RISE_PHASE_END, 0.0f, 1.0f);
			float smooth = t * t * (3.0f - 2.0f * t);
			return Mth.lerp(smooth, startR, peakR * 0.65f);
		}
		if (age < VORTEX_PHASE_END) {
			float t = Mth.clamp((age - RISE_PHASE_END) / (VORTEX_PHASE_END - RISE_PHASE_END), 0.0f, 1.0f);
			return Mth.lerp(t, peakR * 0.65f, peakR);
		}
		if (age < DETONATION_TICK) {
			float implode = Mth.clamp((age - VORTEX_PHASE_END) / (DETONATION_TICK - VORTEX_PHASE_END), 0.0f, 1.0f);
			float curve = implode * implode * implode;
			return Mth.lerp(curve, peakR, 0.35f);
		}
		return 0.0f;
	}

	public float shockwaveRadius(float partialTick) {
		float age = collapseAge() + partialTick;
		if (age < DETONATION_TICK) {
			return 0.0f;
		}
		float t = Mth.clamp((age - DETONATION_TICK) / (float) (FINISH_TICK - DETONATION_TICK), 0.0f, 1.0f);
		float eased = 1.0f - (float) Math.pow(1.0 - t, 2.25);
		return Mth.lerp(eased, 3.0f, blastRadius() * 2.35f);
	}

	public float shockwaveProgress(float partialTick) {
		float age = collapseAge() + partialTick;
		if (age < DETONATION_TICK) {
			return 0.0f;
		}
		return Mth.clamp((age - DETONATION_TICK) / (float) (FINISH_TICK - DETONATION_TICK), 0.0f, 1.0f);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) {
			return;
		}
		ServerLevel level = (ServerLevel) this.level();
		if (Double.isNaN(this.craterCenterY)) {
			this.craterCenterY = this.getY();
		}

		int age = collapseAge() + 1;
		this.entityData.set(DATA_AGE, age);

		if (age == 1) {
			alertNearbyPlayers(level);
			level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.50f);
		}

		boolean colossal = baseRadius() > 4.0f;
		double climbBoost = colossal ? 1.80 : 1.15;

		if (age <= RISE_PHASE_END) {
			// Phase 1: Fast upward launch decelerating smoothly into steady relentless climb high into the clouds!
			double vy = (0.17 + 0.48 * Math.exp(-age / 48.0)) * climbBoost;
			this.setPos(this.getX(), this.getY() + vy, this.getZ());
			if (age > 10) {
				peelSurfaceBlocksForSuction(level, colossal ? 8 : 5);
			}
			updateDirectBlackHoleSuction(level, false, age);
			if (age % 22 == 0) {
				level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.6f, 0.55f + age * 0.003f);
			}
			if (age == 70) {
				level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0f, 0.52f);
			}
			if (age % 22 == 0) {
				spawnVisualLightning(level, 36.0);
			}
			spawnPhaseParticles(level, 18, Math.min(34.0, visualCoreRadius(0.0f) * 0.70));
		} else if (age <= VORTEX_PHASE_END) {
			// Phase 2: Relentless sky climb + massive world-devouring direct gravitational suction funnel!
			this.setPos(this.getX(), this.getY() + 0.13 * climbBoost, this.getZ());
			peelSurfaceBlocksForSuction(level, colossal ? 16 : 11);
			updateDirectBlackHoleSuction(level, false, age);
			pullLivingEntitiesInward(level, 0.30);
			if (age % 22 == 0) {
				level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 4.2f, 0.55f);
				spawnVisualLightning(level, 54.0);
			}
			if (age == 210) {
				level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.2f, 0.48f);
			}
			spawnPhaseParticles(level, 26, Math.min(44.0, visualCoreRadius(0.0f) * 0.78));
		} else if (age < DETONATION_TICK) {
			// Phase 3: Violent implosion crush + final frantic terrain rip + brief eerie silence before blast!
			if (age < DETONATION_TICK - 10) {
				peelSurfaceBlocksForSuction(level, colossal ? 20 : 14);
			}
			updateDirectBlackHoleSuction(level, true, age);
			pullLivingEntitiesInward(level, 0.48);
			if (age == VORTEX_PHASE_END + 2) {
				level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.5f, 0.62f);
			}
			if (age < DETONATION_TICK - 9 && age % 7 == 0) {
				float pitch = Math.min(2.0f, 1.15f + (age - VORTEX_PHASE_END) * 0.022f);
				level.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4.0f, pitch);
			}
			if (age == VORTEX_PHASE_END + 10 || age == VORTEX_PHASE_END + 24) {
				spawnVisualLightning(level, 14.0);
			}
			if (age < DETONATION_TICK - 9) {
				spawnPhaseParticles(level, 28, Math.max(1.8, visualCoreRadius(0.0f) * 0.5));
			}
		} else if (age == DETONATION_TICK) {
			beginDetonation(level);
		} else if (age <= FINISH_TICK) {
			stepProgressiveShockwave(level, age);
			if (age >= FINISH_TICK) {
				this.discard();
			}
		} else {
			this.discard();
		}
	}

	private void alertNearbyPlayers(ServerLevel level) {
		Component msg = Component.translatable("message.galacticpurpose.singularity_escaped")
			.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) <= 340.0 * 340.0) {
				player.displayClientMessage(msg, false);
			}
		}
	}

	/**
	 * Plucks visible surface terrain blocks directly below and around the rising black hole,
	 * immediately launching them smoothly along the gravitational funnel straight toward the event horizon!
	 */
	private void peelSurfaceBlocksForSuction(ServerLevel level, int maxRipPerTick) {
		double maxPullRadius = Math.max(30.0, Math.min(72.0, blastRadius() * 0.70));
		AABB box = this.getBoundingBox().inflate(maxPullRadius + 64.0);
		List<FallingBlockEntity> activeFalling = level.getEntitiesOfClass(FallingBlockEntity.class, box);
		if (activeFalling.size() >= MAX_ACTIVE_FALLING_BLOCKS) {
			return;
		}

		int budget = Math.min(maxRipPerTick, MAX_ACTIVE_FALLING_BLOCKS - activeFalling.size());
		int ripped = 0;
		int cx = Mth.floor(this.getX());
		int cz = Mth.floor(this.getZ());
		int maxSurfaceY = Mth.floor(this.getY()) - 4;
		int minSurfaceY = Math.max(level.getMinBuildHeight() + 6, Mth.floor(this.craterCenterY) - 34);
		Vec3 coreCenter = this.position();

		for (int attempt = 0; attempt < 72 && ripped < budget; attempt++) {
			this.ripSpiralAngle += 0.68;
			this.ripSpiralRadius += 0.55;
			if (this.ripSpiralRadius > maxPullRadius) {
				this.ripSpiralRadius = 2.0 + level.random.nextDouble() * 5.0;
			}

			int tx = cx + Mth.floor(Math.cos(this.ripSpiralAngle) * this.ripSpiralRadius);
			int tz = cz + Mth.floor(Math.sin(this.ripSpiralAngle) * this.ripSpiralRadius);
			int topY = Math.min(maxSurfaceY, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz) - 1);

			BlockPos found = null;
			BlockState foundState = null;
			for (int y = topY; y >= Math.max(minSurfaceY, topY - 8); y--) {
				BlockPos candidate = new BlockPos(tx, y, tz);
				BlockState st = level.getBlockState(candidate);
				if (st.isAir() || !st.getFluidState().isEmpty() || st.hasBlockEntity()) {
					continue;
				}
				float speed = st.getDestroySpeed(level, candidate);
				if (speed < 0.0f || speed > 55.0f) {
					continue;
				}
				found = candidate;
				foundState = st;
				break;
			}
			if (found == null) {
				continue;
			}

			FallingBlockEntity falling = FallingBlockEntity.fall(level, found, foundState);
			falling.dropItem = false;
			falling.setNoGravity(true);
			falling.noPhysics = true;
			falling.time = 2;

			// Launch cleanly toward the black hole center with gentle spiral swirl + immediate client motion sync!
			Vec3 toCore = coreCenter.subtract(falling.position()).normalize();
			Vec3 swirl = new Vec3(-toCore.z, 0.0, toCore.x).scale(0.24);
			falling.setDeltaMovement(toCore.scale(0.85).add(swirl));
			falling.hurtMarked = true;
			ripped++;
		}
	}

	/**
	 * Direct funnel gravitational suction with hurtMarked=true velocity syncing so clients watch every
	 * ripped block stream smoothly UP and STRAIGHT INTO the black hole's maw (zero erratic 20-tick client desync!).
	 */
	private void updateDirectBlackHoleSuction(ServerLevel level, boolean imploding, int age) {
		double captureRadius = Math.max(70.0, blastRadius() + 55.0);
		AABB box = this.getBoundingBox().inflate(captureRadius);
		Vec3 center = this.position();
		double coreR = visualCoreRadius(0.0f);
		double swallowRadius = Math.max(3.2, coreR * 0.78);

		for (FallingBlockEntity falling : level.getEntitiesOfClass(FallingBlockEntity.class, box)) {
			Vec3 toCenter = center.subtract(falling.position());
			double dist = toCenter.length();

			if (dist <= swallowRadius) {
				this.absorbedBlocks++;
				level.sendParticles(
					ParticleTypes.PORTAL,
					falling.getX(), falling.getY(), falling.getZ(),
					3, 0.4, 0.4, 0.4, 0.12
				);
				falling.discard();
				continue;
			}

			falling.setNoGravity(true);
			falling.noPhysics = true;
			falling.time = 2; // never expire mid-flight

			Vec3 dir = toCenter.scale(1.0 / dist);
			// Accelerates strongly as block gets closer to the event horizon!
			double speed = imploding
				? 2.65
				: Mth.clamp(0.75 + (38.0 / (dist + 6.0)), 0.80, 2.25);
			// 82% direct suction straight into the core + 18% gentle vortex twist so blocks visibly pour into the maw!
			double hLen = Math.hypot(dir.x, dir.z);
			double swirlX = hLen > 0.05 ? (-dir.z / hLen) * speed * 0.24 : 0.0;
			double swirlZ = hLen > 0.05 ? (dir.x / hLen) * speed * 0.24 : 0.0;

			falling.setDeltaMovement(
				dir.x * speed + swirlX,
				dir.y * speed,
				dir.z * speed + swirlZ
			);
			// CRITICAL: Force ServerEntity to send ClientboundSetEntityMotionPacket so client never desyncs!
			if (age % 2 == 0 || imploding) {
				falling.hurtMarked = true;
			}
		}
	}

	private void pullLivingEntitiesInward(ServerLevel level, double strength) {
		double pullRadius = Math.max(40.0, blastRadius() * 0.95);
		AABB box = this.getBoundingBox().inflate(pullRadius);
		Vec3 center = this.position();
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
			if (living instanceof Player p && (p.isCreative() || p.isSpectator())) {
				continue;
			}
			Vec3 diff = center.subtract(living.position());
			double dist = Math.max(1.5, diff.length());
			if (dist > pullRadius) {
				continue;
			}
			Vec3 dir = diff.scale(1.0 / dist);
			double pullFactor = strength * 0.58 * (1.0 - (dist / pullRadius));
			living.push(dir.x * pullFactor, Math.max(0.03, dir.y * pullFactor), dir.z * pullFactor);
			living.hurtMarked = true;
		}
		for (InterstellarShipEntity ship : level.getEntitiesOfClass(InterstellarShipEntity.class, box)) {
			if (ship.shipMode() != InterstellarShipEntity.MODE_PARKED) {
				continue;
			}
			Vec3 diff = center.subtract(ship.position());
			double dist = Math.max(2.0, diff.length());
			if (dist > pullRadius) {
				continue;
			}
			Vec3 dir = diff.scale(1.0 / dist);
			double pullFactor = strength * 0.22 * (1.0 - (dist / pullRadius));
			float rollKick = (float) ((dir.x * -dir.z) * 4.5 * strength);
			ship.applyBlastImpulse(
				new Vec3(dir.x * pullFactor, Math.max(0.015, dir.y * pullFactor * 0.65), dir.z * pullFactor),
				(float) (strength * 3.5),
				rollKick
			);
		}
	}

	private void beginDetonation(ServerLevel level) {
		for (FallingBlockEntity falling : level.getEntitiesOfClass(
			FallingBlockEntity.class, this.getBoundingBox().inflate(blastRadius() + 45.0))) {
			falling.discard();
		}
		Vec3 detCenter = this.position();
		double shockRange = Math.max(36.0, blastRadius() * 1.45);
		for (InterstellarShipEntity ship : level.getEntitiesOfClass(
			InterstellarShipEntity.class, this.getBoundingBox().inflate(shockRange))) {
			Vec3 away = ship.position().subtract(detCenter);
			double dist = Math.max(2.0, away.length());
			if (dist > shockRange) {
				continue;
			}
			Vec3 radial = new Vec3(away.x, Math.max(4.0, Math.abs(away.y) + 6.0), away.z).normalize();
			double force = (1.0 - (dist / shockRange)) * 1.55 + 0.25;
			float pitchKick = (float) ((level.random.nextFloat() - 0.35f) * 26.0f * (force / 1.5));
			float rollKick = (float) ((level.random.nextFloat() - 0.5f) * 34.0f * (force / 1.5));
			ship.applyBlastImpulse(radial.scale(force), pitchKick, rollKick);
		}

		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		level.sendParticles(ParticleTypes.FLASH, x, y, z, 18, 4.0, 4.0, 4.0, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 32, 12.0, 12.0, 12.0, 0.25);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 180, 14.0, 14.0, 14.0, 0.55);

		level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0f, 0.45f);
		level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5.5f, 0.52f);
		level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 4.0f, 0.55f);
		this.lastCarvedRadius = 0.0f;
	}

	private void stepProgressiveShockwave(ServerLevel level, int age) {
		int maxR = blastRadius();
		int carveTicks = CARVE_END_TICK - DETONATION_TICK;
		int step = age - DETONATION_TICK;
		if (step < 1 || step > carveTicks) {
			return;
		}

		// Sedov-Taylor / equal-volume cube-root shell scheduling:
		// r(t) = maxR * t^(1/3) ensures CONSTANT carved block volume per tick even for a 224-block mega caldera!
		float progress = step / (float) carveTicks;
		float eased = (float) Math.cbrt(progress);
		float nextR = eased * maxR;
		float prevR = this.lastCarvedRadius;
		this.lastCarvedRadius = nextR;

		int cx = Mth.floor(this.getX());
		int cy = Mth.floor(this.craterCenterY);
		int cz = Mth.floor(this.getZ());

		double rMinSq = Math.max(0.0, (prevR - 0.7) * (prevR - 0.7));
		double rMaxSq = (nextR + 0.7) * (nextR + 0.7);
		double cleanVaporizeSq = (maxR * 0.945) * (maxR * 0.945);
		double maxRSq = maxR * (double) maxR;

		int rCeil = Mth.ceil(nextR + 1.0f);
		int minY = Math.max(level.getMinBuildHeight() + 5, cy - Math.min(rCeil, Math.round(maxR * 0.62f)));
		int maxY = Math.min(level.getMaxBuildHeight() - 2, cy + rCeil);

		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
		BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
		BlockState cryingObs = Blocks.CRYING_OBSIDIAN.defaultBlockState();
		BlockState lava = Blocks.LAVA.defaultBlockState();
		BlockState fire = Blocks.FIRE.defaultBlockState();
		RandomSource rand = level.random;

		for (int dx = -rCeil; dx <= rCeil; dx++) {
			int dx2 = dx * dx;
			for (int dz = -rCeil; dz <= rCeil; dz++) {
				int horizSq = dx2 + dz * dz;
				if (horizSq > rMaxSq) {
					continue;
				}
				double remMax = Math.max(0.0, Math.min(rMaxSq, maxRSq) - horizSq);
				int dyMax = Mth.ceil(Math.sqrt(remMax) / 1.24);
				int colTopY = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx + dx, cz + dz);
				int yStart = Math.max(minY, cy - dyMax);
				int yEnd = Math.min(maxY, Math.min(cy + dyMax, colTopY));

				for (int y = yStart; y <= yEnd; y++) {
					double dyScaled = (y - cy) * 1.24;
					double distSq = horizSq + dyScaled * dyScaled;
					if (distSq < rMinSq || distSq > rMaxSq || distSq > maxRSq) {
						continue;
					}
					mpos.set(cx + dx, y, cz + dz);
					BlockState current = level.getBlockState(mpos);
					if (current.isAir() || current.getDestroySpeed(level, mpos) < 0.0f) {
						continue;
					}

					if (distSq < cleanVaporizeSq || y > cy + 6) {
						level.setBlock(mpos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
					} else {
						int roll = rand.nextInt(100);
						if (roll < 36) {
							level.setBlock(mpos, blackstone, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
						} else if (roll < 48) {
							level.setBlock(mpos, magma, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
						} else if (roll < 53 && y <= cy - 6) {
							level.setBlock(mpos, lava, Block.UPDATE_CLIENTS);
						} else if (roll < 57) {
							level.setBlock(mpos, cryingObs, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
						} else if (roll < 74) {
							level.setBlock(mpos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
							if (rand.nextInt(9) == 0 && !level.getBlockState(mpos.below()).isAir()) {
								level.setBlock(mpos, fire, Block.UPDATE_CLIENTS);
							}
						}
					}
				}
			}
		}

		double visualWaveR = shockwaveRadius(0.0f);
		AABB waveBox = new AABB(
			this.getX() - visualWaveR, this.craterCenterY - visualWaveR * 0.7, this.getZ() - visualWaveR,
			this.getX() + visualWaveR, this.getY() + visualWaveR, this.getZ() + visualWaveR
		);
		Vec3 blastOrigin = new Vec3(this.getX(), this.craterCenterY + 3.0, this.getZ());
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, waveBox)) {
			if (living instanceof Player p && (p.isCreative() || p.isSpectator())) {
				continue;
			}
			double d = living.position().distanceTo(blastOrigin);
			if (d <= nextR + 8.0 && d >= prevR - 5.0) {
				float dmg = (float) Math.max(16.0, 140.0 * (1.0 - d / (maxR + 18.0)));
				living.hurt(level.damageSources().explosion(this, null), dmg);
				Vec3 push = living.position().subtract(blastOrigin).normalize().scale(3.6);
				living.push(push.x, Math.max(1.1, push.y + 1.0), push.z);
				living.hurtMarked = true;
			}
		}

		if (step % 5 == 0) {
			level.sendParticles(
				ParticleTypes.CAMPFIRE_COSY_SMOKE,
				this.getX(), this.craterCenterY + 5.0, this.getZ(),
				28, nextR * 0.70, 6.0, nextR * 0.70, 0.08
			);
		}
	}

	private void spawnVisualLightning(ServerLevel level, double spread) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			double ox = (level.random.nextDouble() - 0.5) * spread * 2.0;
			double oz = (level.random.nextDouble() - 0.5) * spread * 2.0;
			bolt.moveTo(this.getX() + ox, this.craterCenterY, this.getZ() + oz);
			level.addFreshEntity(bolt);
		}
	}

	private void spawnPhaseParticles(ServerLevel level, int count, double spread) {
		level.sendParticles(
			ParticleTypes.REVERSE_PORTAL,
			this.getX(), this.getY(), this.getZ(),
			count, spread, spread, spread, 0.10
		);
		level.sendParticles(
			ParticleTypes.FLAME,
			this.getX(), this.getY(), this.getZ(),
			Math.max(4, count / 3), spread * 0.65, spread * 0.65, spread * 0.65, 0.05
		);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 720.0 * 720.0;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(DATA_AGE, tag.getInt("CollapseAge"));
		if (tag.contains("BaseRadius")) {
			this.entityData.set(DATA_BASE_RADIUS, tag.getFloat("BaseRadius"));
		}
		if (tag.contains("BlastRadius")) {
			this.entityData.set(DATA_BLAST_RADIUS, tag.getInt("BlastRadius"));
		}
		this.absorbedBlocks = tag.getInt("AbsorbedBlocks");
		this.craterCenterY = tag.contains("CraterY") ? tag.getDouble("CraterY") : this.getY();
		this.lastCarvedRadius = tag.getFloat("LastCarvedR");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("CollapseAge", collapseAge());
		tag.putFloat("BaseRadius", baseRadius());
		tag.putInt("BlastRadius", blastRadius());
		tag.putInt("AbsorbedBlocks", this.absorbedBlocks);
		tag.putDouble("CraterY", Double.isNaN(this.craterCenterY) ? this.getY() : this.craterCenterY);
		tag.putFloat("LastCarvedR", this.lastCarvedRadius);
	}
}

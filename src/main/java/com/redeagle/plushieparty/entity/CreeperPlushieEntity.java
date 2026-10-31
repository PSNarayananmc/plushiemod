package com.redeagle.plushieparty.entity;

import com.redeagle.plushieparty.party.PartyManager;
import com.redeagle.plushieparty.registry.PlushieSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Creeper Plushie entity ({@code re_plushie_party:creeper_plushie}).
 *
 * <p>Faithful port of the Bedrock entity behaviour:</p>
 * <ul>
 *   <li>persistent, no AI, zero-size collision box</li>
 *   <li>invulnerable to all damage, immune to knockback, fire and status
 *       effects (a non-living Java entity is immune to all of these by
 *       definition)</li>
 *   <li>click "squish" animation state (variant 0/1 in Bedrock) that lasts
 *       0.5 s; the toy squeak sound plays at the same moment the Bedrock
 *       animation's sound effect would fire</li>
 *   <li>despawns when the block it sits in turns to air or water. The
 *       original addon intended this via an {@code inside_block_notifier},
 *       but its despawn event referenced a non-existent component group and
 *       never actually fired; this port implements the intended behaviour,
 *       which also cleans up orphaned entities whose hitbox block was
 *       removed by commands.</li>
 * </ul>
 */
public class CreeperPlushieEntity extends Entity {
	/** Squish animation length in ticks (Bedrock: 0.5 s timer). */
	public static final int SQUISH_DURATION_TICKS = 10;

	private static final EntityDataAccessor<Boolean> DATA_SQUISHING =
			SynchedEntityData.defineId(CreeperPlushieEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Long> DATA_SQUISH_START_GAME_TIME =
			SynchedEntityData.defineId(CreeperPlushieEntity.class, EntityDataSerializers.LONG);

	private static final String TAG_SQUISH_REMAINING = "PlushieParty:SquishRemainingTicks";

	public CreeperPlushieEntity(EntityType<? extends CreeperPlushieEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setInvulnerable(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_SQUISHING, false);
		builder.define(DATA_SQUISH_START_GAME_TIME, 0L);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			// Orphan cleanup: the hitbox block is the only thing keeping us alive.
			// (Faithful to the addon's intended inside_block_notifier -> despawn on air/water.)
			BlockPos pos = this.blockPosition();
			BlockState state = this.level().getBlockState(pos);
			boolean inAirOrWater = (state.isAir() || !state.getFluidState().isEmpty())
					&& !(state.getBlock() instanceof com.redeagle.plushieparty.block.CreeperPlushieHitboxBlock);
			if (inAirOrWater) {
				PartyManager.onEntityRemoved(this);
				this.discard();
				return;
			}
			// End of squish window (Bedrock: timer fires re_event:variant1 after 0.5 s).
			if (this.isSquishing() && this.squishTicksRemaining() <= 0) {
				this.getEntityData().set(DATA_SQUISHING, false);
			}
		}
	}

	/** Starts the 0.5 s click-squish (Bedrock: triggerEvent("re_event:variant0")). */
	public void squish() {
		if (!this.level().isClientSide()) {
			this.getEntityData().set(DATA_SQUISHING, true);
			this.getEntityData().set(DATA_SQUISH_START_GAME_TIME, this.level().getGameTime());
			this.playToySound();
		}
	}

	public boolean isSquishing() {
		return this.getEntityData().get(DATA_SQUISHING);
	}

	public long squishStartGameTime() {
		return this.getEntityData().get(DATA_SQUISH_START_GAME_TIME);
	}

	public int squishTicksRemaining() {
		return SQUISH_DURATION_TICKS - (int) (this.level().getGameTime() - this.squishStartGameTime());
	}

	private void playToySound() {
		// Bedrock sound: volume 0.8, pitch 1.0, max distance 7 (fixed on the SoundEvent), category neutral.
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), PlushieSounds.TOY_USE, SoundSource.NEUTRAL, 0.8F, 1.0F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Bedrock damage_sensor: cause "all", deals_damage "no".
		return false;
	}

	@Override
	public boolean hurtClient(DamageSource source) {
		return false;
	}

	@Override
	public boolean isPickable() {
		// Clicks go to the hitbox block, never to the entity.
		return false;
	}

	@Override
	public boolean canBeCollidedWith(Entity other) {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean ignoreExplosion(net.minecraft.world.level.Explosion explosion) {
		return true;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public void push(Entity entity) {
		// Bedrock knockback_resistance 1.0: nothing moves the plushie.
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected AABB makeBoundingBox(Vec3 position) {
		// Zero-size collision box (Bedrock collision_box 0x0).
		return new AABB(position, position);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		int remaining = input.getIntOr(TAG_SQUISH_REMAINING, 0);
		if (remaining > 0) {
			this.getEntityData().set(DATA_SQUISHING, true);
			this.getEntityData().set(DATA_SQUISH_START_GAME_TIME, this.level().getGameTime() - (SQUISH_DURATION_TICKS - remaining));
		} else {
			this.getEntityData().set(DATA_SQUISHING, false);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		if (this.isSquishing()) {
			output.putInt(TAG_SQUISH_REMAINING, Math.max(0, this.squishTicksRemaining()));
		}
	}

	@Override
	public boolean displayFireAnimation() {
		return false;
	}
}

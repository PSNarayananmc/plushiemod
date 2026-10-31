package com.redeaglestudios.plushieparty;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Creeper Plushie doll entity.
 *
 * Faithful port of the Bedrock entity re_plushie_party:creeper_plushie:
 * - persistent, no AI, zero collision box, immune to damage/knockback/fire
 * - squeezes on interaction (0.5s animation, variant 0 -> 1 with a 0.5s timer)
 * - squeezes once right after spawning (minecraft:entity_spawned -> re_event:variant0)
 * - despawns when its position becomes air or water (inside_block_notifier safety)
 */
public class CreeperPlushieEntity extends Mob {
    /** Ticks the squeeze animation lasts (Bedrock: 0.5s timer between variant0 and variant1). */
    public static final int SQUEEZE_TICKS = 10;

    /** Synced: remaining squeeze ticks (>0 while squeezing). */
    private static final EntityDataAccessor<Integer> SQUEEZE_TICKS_LEFT =
        SynchedEntityData.defineId(CreeperPlushieEntity.class, EntityDataSerializers.INT);

    /** Client-side: ageInTicks when the currently playing squeeze started (-1 = none). */
    public float clientSqueezeStartAge = -1.0f;
    /** Client-side: true while the squeeze data is active (used to detect a fresh squeeze). */
    public boolean clientSqueezeActive = false;

    public CreeperPlushieEntity(EntityType<? extends CreeperPlushieEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);                 // no AI at all (Bedrock has no goals)
        this.setNoGravity(true);            // never falls or gets pushed
        this.setInvulnerable(true);         // damage_sensor: deals_damage "no" (hurt() is final in 26.2)
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)            // minecraft:health 1
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0); // knockback_resistance 1.0
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SQUEEZE_TICKS_LEFT, 0);
    }

    // ------------------------------------------------------------- squeeze
    /** Starts the 0.5s squeeze + squeak (Bedrock: triggerEvent re_event:variant0). */
    public void squeeze() {
        if (level() instanceof ServerLevel server) {
            this.entityData.set(SQUEEZE_TICKS_LEFT, SQUEEZE_TICKS);
            // The squeaky sound is attached to the animation start in Bedrock (sound effect at t=0).
            server.playSound(null, this.blockPosition(),
                ModRegistry.TOY_USE, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
    }

    public int getSqueezeTicksLeft() {
        return this.entityData.get(SQUEEZE_TICKS_LEFT);
    }

    // --------------------------------------------------------------- ticks
    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            int left = getSqueezeTicksLeft();
            if (left > 0) {
                this.entityData.set(SQUEEZE_TICKS_LEFT, left - 1);
            }

            // Bedrock inside_block_notifier: despawn if we end up inside air or water.
            BlockState state = level().getBlockState(this.blockPosition());
            if (state.isAir() || state.is(Blocks.WATER)) {
                PartyManager.get().onPlushieRemoved(this);
                this.discard();
            }
        }
    }

    // ------------------------------------------------- immortality & persistence
    @Override
    public boolean isPushable() {
        return false;   // never pushed by players, fluids or pistons
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;   // minecraft:persistent
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // No movement what so ever; swallow knockback like knockback_resistance 1.0.
        this.setDeltaMovement(0, 0, 0);
    }
}

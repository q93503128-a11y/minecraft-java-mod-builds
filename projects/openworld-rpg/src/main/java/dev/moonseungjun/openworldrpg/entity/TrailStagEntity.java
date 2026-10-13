package dev.moonseungjun.openworldrpg.entity;

import dev.moonseungjun.openworldrpg.mount.TrailStagLiveBinding;
import dev.moonseungjun.openworldrpg.mount.TrailStagMountRules;
import dev.moonseungjun.openworldrpg.mount.TrailStagMountService;
import dev.moonseungjun.openworldrpg.mount.TrailStagResolveState;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Project-owned Trail Stag traversal mount.
 *
 * <p>The rider supplies directional intent through Minecraft's normal ridden-input transport, while
 * speed, turn rate, hop, Resolve and dismount authority are resolved on the server. The entity is a
 * transient summon: if its owner seam is absent after a world reload it removes itself rather than
 * becoming a stray persistent animal.</p>
 */
public final class TrailStagEntity extends PathfinderMob implements PlayerRideableJumping {
    public static final float REVIEW_WIDTH = 1.10F;
    public static final float REVIEW_HEIGHT = 1.65F;

    private static final double CRUISE_PER_TICK = TrailStagMountRules.CRUISE_BLOCKS_PER_SECOND / 20.0;
    private static final double ACCEL_PER_TICK = CRUISE_PER_TICK * 0.90 / 20.0;
    private static final double DECEL_PER_TICK = CRUISE_PER_TICK * 0.90 / 15.0;
    private static final double FIXED_HOP_VELOCITY = Math.sqrt(2.0 * 0.08 * TrailStagMountRules.JUMP_APEX_BLOCKS);

    private static final EntityDataAccessor<Float> RESOLVE_CURRENT =
            SynchedEntityData.defineId(TrailStagEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RESOLVE_MAXIMUM =
            SynchedEntityData.defineId(TrailStagEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> AUTHORED_AIRBORNE =
            SynchedEntityData.defineId(TrailStagEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerId;
    private double riddenSpeed;
    private int landingRecoveryTicks;
    private boolean wasGrounded = true;
    private int ownerGraceTicks = 2;
    private int cleanDespawnTicks = -1;

    public TrailStagEntity(EntityType<? extends TrailStagEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, CRUISE_PER_TICK)
                .add(Attributes.STEP_HEIGHT, TrailStagMountRules.MAX_AUTHORED_STEP_UP_BLOCKS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RESOLVE_CURRENT, 0.0F);
        builder.define(RESOLVE_MAXIMUM, 0.0F);
        builder.define(AUTHORED_AIRBORNE, false);
    }

    public void bindOwner(ServerPlayer owner) {
        ownerId = owner.getUUID();
        TrailStagResolveState fresh = TrailStagResolveState.fresh(owner.getMaxHealth());
        entityData.set(RESOLVE_MAXIMUM, (float) fresh.maximum());
        entityData.set(RESOLVE_CURRENT, (float) fresh.current());
        ownerGraceTicks = 2;
        cleanDespawnTicks = -1;
    }

    @Nullable
    public UUID ownerId() {
        return ownerId;
    }

    public boolean ownedBy(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    public float resolveCurrent() {
        return entityData.get(RESOLVE_CURRENT);
    }

    public float resolveMaximum() {
        return entityData.get(RESOLVE_MAXIMUM);
    }

    public boolean authoredAirborne() {
        return entityData.get(AUTHORED_AIRBORNE);
    }

    public void armCleanDespawn() {
        cleanDespawnTicks = TrailStagMountRules.CLEAN_RETURN_TICKS;
    }

    public int cleanDespawnTicksRemaining() {
        return cleanDespawnTicks;
    }

    public int tickCleanDespawnCountdown() {
        if (cleanDespawnTicks > 0) {
            cleanDespawnTicks--;
        }
        return cleanDespawnTicks;
    }

    /** Called from the final player-damage application boundary; input is already authoritative. */
    public float absorbResolvedDamage(ServerPlayer rider, float damage) {
        if (!ownedBy(rider) || damage <= 0.0F || !Float.isFinite(damage)) {
            return damage;
        }
        double maximum = Math.max(0.0001, resolveMaximum());
        double current = Math.min(maximum, Math.max(0.0, resolveCurrent()));
        TrailStagResolveState.DamageResult result =
                new TrailStagResolveState(maximum, current).apply(damage);
        entityData.set(RESOLVE_CURRENT, (float) result.state().current());
        if (result.brokeResolve() && level() instanceof ServerLevel serverLevel) {
            TrailStagMountService.recordResolveBreak(rider, serverLevel.getGameTime());
            TrailStagLiveBinding.forceResolveBreakDismount(rider, this);
        }
        return (float) result.overflowDamage();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        LivingEntity passenger = getControllingPassenger();
        if (passenger instanceof ServerPlayer rider && ownedBy(rider)) {
            // Route body hits through the rider's ordinary mitigation exactly once; the player
            // damage boundary then consumes the post-mitigation result into Resolve.
            return rider.hurtServer(level, source, damage);
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (ownerGraceTicks > 0) {
            ownerGraceTicks--;
        } else if (ownerId == null) {
            discard();
            return;
        }

        boolean grounded = onGround();
        if (!wasGrounded && grounded) {
            landingRecoveryTicks = TrailStagMountRules.JUMP_LANDING_RECOVERY_TICKS;
            entityData.set(AUTHORED_AIRBORNE, false);
        } else if (!grounded) {
            entityData.set(AUTHORED_AIRBORNE, true);
        }
        wasGrounded = grounded;
        if (landingRecoveryTicks > 0) {
            landingRecoveryTicks--;
        }

        if (!isVehicle()) {
            riddenSpeed = approach(riddenSpeed, 0.0, DECEL_PER_TICK);
            TrailStagLiveBinding.tickUnridden(this);
        }
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity first = getFirstPassenger();
        return first instanceof Player player && ownedBy(player) ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty()
                && passenger instanceof Player player
                && ownedBy(player);
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        if (!ownedBy(controller) || landingRecoveryTicks > 0) {
            return Vec3.ZERO;
        }
        float forward = Mth.clamp(controller.zza, -1.0F, 1.0F);
        double target = Math.abs(forward) > 0.001F ? CRUISE_PER_TICK : 0.0;
        riddenSpeed = approach(
                riddenSpeed,
                target,
                target > riddenSpeed ? ACCEL_PER_TICK : DECEL_PER_TICK
        );
        return new Vec3(0.0, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float) riddenSpeed;
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        getNavigation().stop();
        setTarget(null);

        double ratio = CRUISE_PER_TICK <= 0.0 ? 0.0 : Mth.clamp(riddenSpeed / CRUISE_PER_TICK, 0.0, 1.0);
        double yawPerSecond = ratio <= 0.50
                ? TrailStagMountRules.LOW_SPEED_YAW_DEGREES_PER_SECOND
                : Mth.lerp(
                        (float) ((ratio - 0.50) / 0.50),
                        (float) TrailStagMountRules.LOW_SPEED_YAW_DEGREES_PER_SECOND,
                        (float) TrailStagMountRules.FULL_SPEED_YAW_DEGREES_PER_SECOND
                );
        float maxTurn = (float) (yawPerSecond / 20.0);
        float delta = Mth.wrapDegrees(controller.getYRot() - getYRot());
        float nextYaw = getYRot() + Mth.clamp(delta, -maxTurn, maxTurn);
        setYRot(nextYaw);
        setYBodyRot(nextYaw);
        setYHeadRot(nextYaw);
        setXRot(0.0F);
    }

    @Override
    public boolean canJump() {
        return onGround() && landingRecoveryTicks == 0 && getControllingPassenger() != null;
    }

    @Override
    public void onPlayerJump(int jumpAmount) {
        if (!canJump()) {
            return;
        }
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(
                motion.x * TrailStagMountRules.JUMP_HORIZONTAL_VELOCITY_RETENTION,
                FIXED_HOP_VELOCITY,
                motion.z * TrailStagMountRules.JUMP_HORIZONTAL_VELOCITY_RETENTION
        );
        setOnGround(false);
        entityData.set(AUTHORED_AIRBORNE, true);
    }

    @Override
    public void handleStartJump(int jumpScale) {
        onPlayerJump(jumpScale);
    }

    @Override
    public void handleStopJump() {
        // Trail Stag uses a fixed hop. Holding/releasing jump cannot charge or alter it.
    }

    @Override
    public int getJumpCooldown() {
        return landingRecoveryTicks;
    }

    private static double approach(double current, double target, double step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }
}

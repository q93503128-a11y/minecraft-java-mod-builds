package kr.moonseungjun.anomalousclasses.entity;

import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.SwarmCasterService;
import kr.moonseungjun.anomalousclasses.state.SwarmSavedData.SwarmNodeRole;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;

/**
 * Real living target and projectile collider for one logical role. No chunk-save
 * persistence: player counts live in SwarmSavedData and rehydrate independently.
 */
public final class SwarmNodeEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> COUNT =
            SynchedEntityData.defineId(SwarmNodeEntity.class, EntityDataSerializers.INT);
    private UUID ownerId;
    private SwarmNodeRole role;

    public SwarmNodeEntity(EntityType<? extends SwarmNodeEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1024)
                .add(Attributes.MOVEMENT_SPEED, .34)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COUNT, 1);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    public void initialize(UUID owner, SwarmNodeRole role, int count) {
        this.ownerId = owner;
        this.role = role;
        setNoGravity(role != SwarmNodeRole.ATTACK);
        setCount(count);
    }

    public UUID ownerId() { return ownerId; }
    public SwarmNodeRole role() { return role; }
    public int count() { return entityData.get(COUNT); }

    public void setCount(int value) {
        entityData.set(COUNT, Math.max(0, value));
        refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        if (entityData == null) return super.getDefaultDimensions(pose);
        int value = entityData.get(COUNT);
        float width = Math.min(.9F, .25F + .05F * Math.max(0, value - 1));
        return EntityDimensions.scalable(width, Math.max(.28F, width * .64F));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (COUNT.equals(key)) refreshDimensions();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel) || role == null || ownerId == null
                || SwarmCasterService.friendlyHit(this, source)) return false;
        boolean hit = super.hurt(source, amount);
        if (hit) {
            SwarmCasterService.onNodeHit(this, amount);
            if (!isRemoved()) setHealth(getMaxHealth());
        }
        return hit;
    }

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    // The entity has no save representation; no source of duplicate units or loot.
    @Override
    public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
}

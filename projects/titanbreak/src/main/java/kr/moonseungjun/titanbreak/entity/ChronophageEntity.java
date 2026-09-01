package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.ChronophageRewardService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.ReflexDriveService;
import kr.moonseungjun.titanbreak.combat.ReflexFieldService;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ChronophageEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int TIME_ORGAN_0 = 1 << 0;
    public static final int TIME_ORGAN_1 = 1 << 1;
    public static final int TIME_ORGAN_2 = 1 << 2;
    public static final int PHASE_JOINT_0 = 1 << 3;
    public static final int PHASE_JOINT_1 = 1 << 4;
    public static final int PHASE_JOINT_2 = 1 << 5;
    public static final int PHASE_JOINT_3 = 1 << 6;
    public static final int CENTRAL_RING = 1 << 7;
    public static final int TIME_ORGAN_MASK = TIME_ORGAN_0 | TIME_ORGAN_1 | TIME_ORGAN_2;
    public static final int PHASE_JOINT_MASK = PHASE_JOINT_0 | PHASE_JOINT_1 | PHASE_JOINT_2 | PHASE_JOINT_3;
    public static final int ALL_PARTS_MASK = 0xFF;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 15_000.0D;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(ChronophageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(ChronophageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EFFECTIVE_TR =
            SynchedEntityData.defineId(ChronophageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FIELD_RADIUS =
            SynchedEntityData.defineId(ChronophageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FIELD_OVERRIDE =
            SynchedEntityData.defineId(ChronophageEntity.class, EntityDataSerializers.BOOLEAN);

    private static final PartSpec[] SPECS = {
            new PartSpec(PartKind.TIME_ORGAN, 0, TIME_ORGAN_0, -10.0D, 32.0D, -4.0D, 8.0F, 9.0F, 360.0F),
            new PartSpec(PartKind.TIME_ORGAN, 1, TIME_ORGAN_1, 10.0D, 32.0D, -4.0D, 8.0F, 9.0F, 360.0F),
            new PartSpec(PartKind.TIME_ORGAN, 2, TIME_ORGAN_2, 0.0D, 47.0D, 1.0D, 9.0F, 9.0F, 420.0F),
            new PartSpec(PartKind.PHASE_JOINT, 0, PHASE_JOINT_0, -15.0D, 18.0D, 0.0D, 7.0F, 7.0F, 240.0F),
            new PartSpec(PartKind.PHASE_JOINT, 1, PHASE_JOINT_1, 15.0D, 18.0D, 0.0D, 7.0F, 7.0F, 240.0F),
            new PartSpec(PartKind.PHASE_JOINT, 2, PHASE_JOINT_2, -15.0D, 43.0D, 0.0D, 7.0F, 7.0F, 240.0F),
            new PartSpec(PartKind.PHASE_JOINT, 3, PHASE_JOINT_3, 15.0D, 43.0D, 0.0D, 7.0F, 7.0F, 240.0F),
            new PartSpec(PartKind.CENTRAL_RING, 0, CENTRAL_RING, 0.0D, 38.0D, -8.0D, 14.0F, 14.0F, 850.0F)
    };

    private final ChronophagePart[] parts = new ChronophagePart[SPECS.length];
    private final ServerBossEvent bossBar;
    private final List<TemporalStrike> strikes = new ArrayList<>();
    private final Deque<Vec3> targetHistory = new ArrayDeque<>();
    private boolean partsInitialized;
    private boolean rewardsGranted;
    private int actionCooldown = 40;
    private int overrideCooldown = 45;
    private int phaseMoveCooldown = 34;
    private int reversalCooldown = 100;

    public ChronophageEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.chronophage"),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new ChronophagePart(this, spec, spec.width(), spec.height(), spec.health());
        }
        xpReward = 200;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
        builder.define(PHASE, 1);
        builder.define(EFFECTIVE_TR, 75);
        builder.define(FIELD_RADIUS, 64);
        builder.define(FIELD_OVERRIDE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new ChronophageCombatGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        return getEntityData().get(EFFECTIVE_TR);
    }

    public int phase() { return getEntityData().get(PHASE); }
    public int fieldRadius() { return getEntityData().get(FIELD_RADIUS); }
    public boolean fieldOverrideActive() { return getEntityData().get(FIELD_OVERRIDE); }
    public int brokenPartsMask() { return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK; }
    public boolean isPartBroken(int mask) { return (brokenPartsMask() & mask) != 0; }
    public int brokenTimeOrganCount() { return Integer.bitCount(brokenPartsMask() & TIME_ORGAN_MASK); }
    public int brokenPhaseJointCount() { return Integer.bitCount(brokenPartsMask() & PHASE_JOINT_MASK); }
    public boolean centralRingExposed() { return phase() == 3 && brokenTimeOrganCount() >= 2; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float effective = amount * (centralRingExposed() ? 0.035F : 0.012F);
        if (effective <= 0.0F) return false;
        float before = getHealth();
        setHealth(Math.max(1.0F, before - effective));
        return getHealth() < before;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        updatePartPositions();
        if (!level().isClientSide()) {
            bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / Math.max(1.0F, getMaxHealth()))));
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;

        LivingEntity target = getTarget();
        updateTargetHistory(target);
        updateTemporalState(target);
        tickStrikes(serverLevel);

        if (actionCooldown > 0) actionCooldown--;
        if (overrideCooldown > 0) overrideCooldown--;
        if (phaseMoveCooldown > 0) phaseMoveCooldown--;
        if (reversalCooldown > 0) reversalCooldown--;

        if (phase() >= 2 && target instanceof ServerPlayer player && ReflexDriveService.requested(player.getUUID())) {
            temporalOverwrite(player);
        } else {
            getEntityData().set(FIELD_OVERRIDE, false);
        }

        if (phase() == 3 && target != null && target.isAlive() && reversalCooldown <= 0) {
            reversalCooldown = 88 + brokenTimeOrganCount() * 18;
            schedulePastEchoes(target);
        }
    }

    private void updateTemporalState(LivingEntity target) {
        boolean detectedField = target instanceof ServerPlayer player && ReflexDriveService.requested(player.getUUID());
        int requestedRating = target instanceof ServerPlayer player ? ReflexDriveService.rating(player.getUUID()) : 0;
        int brokenOrgans = brokenTimeOrganCount();
        int nextPhase = brokenOrgans >= 2 || getHealth() <= getMaxHealth() * 0.32F ? 3 : detectedField ? 2 : 1;
        int baseRating = nextPhase == 1 ? 75 : (requestedRating >= 100 ? 110 : 95);
        int effectiveRating = Math.max(40, baseRating - brokenOrgans * 15);
        int radius = Math.max(32, (nextPhase == 1 ? 64 : 96) - brokenOrgans * 18);
        getEntityData().set(PHASE, nextPhase);
        getEntityData().set(EFFECTIVE_TR, effectiveRating);
        getEntityData().set(FIELD_RADIUS, radius);
    }

    private void temporalOverwrite(ServerPlayer player) {
        double radius = fieldRadius();
        if (distanceToSqr(player) > radius * radius) {
            getEntityData().set(FIELD_OVERRIDE, false);
            return;
        }
        if (overrideCooldown > 0) return;
        overrideCooldown = 52 + brokenTimeOrganCount() * 18;
        getEntityData().set(FIELD_OVERRIDE, true);
        ReflexFieldService.clear(player.getUUID());
        ReflexDriveService.setActive(player, false);
        player.hurtServer((ServerLevel) level(), damageSources().mobAttack(this),
                (float) CombatScale.toInternal(phase() == 3 ? 22.0D : 14.0D));
        Vec3 push = player.position().subtract(position());
        if (push.horizontalDistanceSqr() > 1.0E-6D) {
            push = new Vec3(push.x, 0.0D, push.z).normalize();
            player.push(push.x * 0.45D, 0.12D, push.z * 0.45D);
        }
    }

    private void updateTargetHistory(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            targetHistory.clear();
            return;
        }
        targetHistory.addLast(target.position());
        while (targetHistory.size() > 48) targetHistory.removeFirst();
    }

    private Vec3 pastPosition(int ticksAgo, LivingEntity fallback) {
        if (targetHistory.isEmpty()) return fallback.position();
        int index = Math.max(0, targetHistory.size() - 1 - ticksAgo);
        int i = 0;
        for (Vec3 pos : targetHistory) {
            if (i++ == index) return pos;
        }
        return fallback.position();
    }

    private boolean partPickable(PartSpec spec) {
        if (isPartBroken(spec.mask())) return false;
        if (spec.kind() == PartKind.CENTRAL_RING) return centralRingExposed();
        return true;
    }

    private boolean hurtPart(ChronophagePart part, ServerLevel level, DamageSource source, float amount) {
        PartSpec spec = part.spec;
        if (!partPickable(spec)) return false;
        float effective = amount;
        part.applyPartDamage(effective);
        float transfer = switch (spec.kind()) {
            case TIME_ORGAN -> effective * 0.50F;
            case PHASE_JOINT -> effective * 0.22F;
            case CENTRAL_RING -> effective;
        };
        setHealth(Math.max(1.0F, getHealth() - transfer));
        if (!part.broken()) return true;
        markBroken(spec.mask());
        if (spec.kind() == PartKind.CENTRAL_RING) return super.hurtServer(level, source, Float.MAX_VALUE);
        if (spec.kind() == PartKind.TIME_ORGAN) {
            overrideCooldown += 18;
            actionCooldown += 8;
        } else {
            phaseMoveCooldown += 22;
        }
        return true;
    }

    private void markBroken(int mask) {
        getEntityData().set(BROKEN_PARTS, brokenPartsMask() | mask);
    }

    private void timeCutSlash(ServerLevel level, LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        Vec3 origin = position().add(0.0D, 28.0D, 0.0D);
        Vec3 direction = target.getEyePosition().subtract(origin);
        if (direction.lengthSqr() < 1.0E-6D) return;
        direction = direction.normalize();
        double range = phase() == 3 ? 52.0D : 42.0D;
        double width = phase() == 3 ? 4.4D : 3.2D;
        double visibleDamage = phase() == 3 ? 62.0D : 48.0D;
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(range), Player::isAlive)) {
            Vec3 delta = player.getEyePosition().subtract(origin);
            double along = delta.dot(direction);
            if (along < 0.0D || along > range) continue;
            if (delta.subtract(direction.scale(along)).length() > width) continue;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(visibleDamage));
        }
    }

    private void scheduleDelayedExplosion(LivingEntity target) {
        Vec3 predicted = target.position().add(target.getDeltaMovement().scale(10.0D));
        strikes.add(new TemporalStrike(predicted, phase() == 3 ? 16 : 24,
                phase() == 3 ? 7.0D : 5.5D, phase() == 3 ? 54.0D : 42.0D));
    }

    private void schedulePastEchoes(LivingEntity target) {
        strikes.add(new TemporalStrike(pastPosition(8, target), 10, 4.5D, 38.0D));
        strikes.add(new TemporalStrike(pastPosition(16, target), 18, 5.0D, 44.0D));
        strikes.add(new TemporalStrike(pastPosition(24, target), 26, 5.5D, 50.0D));
    }

    private void tickStrikes(ServerLevel level) {
        for (int i = strikes.size() - 1; i >= 0; i--) {
            TemporalStrike strike = strikes.get(i);
            if (--strike.ticks > 0) continue;
            detonate(level, strike);
            strikes.remove(i);
        }
    }

    private void detonate(ServerLevel level, TemporalStrike strike) {
        AABB area = new AABB(strike.center, strike.center).inflate(strike.radius, 4.0D, strike.radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            Vec3 flat = new Vec3(player.getX() - strike.center.x, 0.0D, player.getZ() - strike.center.z);
            if (flat.lengthSqr() > strike.radius * strike.radius) continue;
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(strike.visibleDamage));
        }
    }

    private void phaseMove(ServerLevel level, LivingEntity target) {
        if (brokenPhaseJointCount() >= 4 || phaseMoveCooldown > 0) return;
        Vec3 away = position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
        away = new Vec3(away.x, 0.0D, away.z).normalize();
        Vec3 side = new Vec3(-away.z, 0.0D, away.x).scale(getRandom().nextBoolean() ? 1.0D : -1.0D);
        double distance = 10.0D + Math.max(0, 4 - brokenPhaseJointCount()) * 1.5D;
        Vec3 destination = target.position().add(away.scale(7.0D)).add(side.scale(distance));
        Vec3 delta = destination.subtract(position());
        if (level.noCollision(this, getBoundingBox().move(delta.x, delta.y, delta.z))) {
            setPos(destination.x, destination.y, destination.z);
            setDeltaMovement(Vec3.ZERO);
        }
        phaseMoveCooldown = 32 + brokenPhaseJointCount() * 14;
    }

    private void updatePartPositions() {
        Vec3[] previous = new Vec3[parts.length];
        for (int i = 0; i < parts.length; i++) previous[i] = parts[i].position();
        double yaw = Math.toRadians(-getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        for (int i = 0; i < parts.length; i++) {
            PartSpec spec = SPECS[i];
            double x = spec.x() * cos - spec.z() * sin;
            double z = spec.x() * sin + spec.z() * cos;
            parts[i].setPos(getX() + x, getY() + spec.y(), getZ() + z);
        }
        for (int i = 0; i < parts.length; i++) {
            ChronophagePart part = parts[i];
            Vec3 old = partsInitialized ? previous[i] : part.position();
            part.xo = old.x; part.yo = old.y; part.zo = old.z;
            part.xOld = old.x; part.yOld = old.y; part.zOld = old.z;
        }
        partsInitialized = true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (!partsInitialized) return getBoundingBox().inflate(34.0D, 76.0D, 34.0D);
        AABB bounds = getBoundingBox();
        for (ChronophagePart part : parts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds.inflate(6.0D);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        if (!rewardsGranted && level() instanceof ServerLevel level) {
            rewardsGranted = true;
            ChronophageRewardService.award(this, level, source);
        }
        super.die(source);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int saved = input.getIntOr("TitanbreakChronophageBrokenParts", 0) & ALL_PARTS_MASK;
        getEntityData().set(BROKEN_PARTS, saved);
        getEntityData().set(PHASE, input.getIntOr("TitanbreakChronophagePhase", 1));
        getEntityData().set(EFFECTIVE_TR, input.getIntOr("TitanbreakChronophageTR", 75));
        getEntityData().set(FIELD_RADIUS, input.getIntOr("TitanbreakChronophageFieldRadius", 64));
        getEntityData().set(FIELD_OVERRIDE, false);
        for (int i = 0; i < parts.length; i++) {
            float hp = input.getFloatOr("TitanbreakChronophagePartHealth" + i, SPECS[i].health());
            if ((saved & SPECS[i].mask()) != 0) hp = 0.0F;
            parts[i].setPartHealth(hp);
        }
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakChronophageBrokenParts", brokenPartsMask());
        output.putInt("TitanbreakChronophagePhase", phase());
        output.putInt("TitanbreakChronophageTR", temporalRating());
        output.putInt("TitanbreakChronophageFieldRadius", fieldRadius());
        for (int i = 0; i < parts.length; i++) output.putFloat("TitanbreakChronophagePartHealth" + i, parts[i].partHealth);
    }

    @Override public boolean isMultipartEntity() { return true; }
    @Override public PartEntity<?>[] getParts() { return parts; }
    @Override public boolean isPickable() { return false; }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        for (int i = 0; i < parts.length; i++) parts[i].setId(packet.getId() + i + 1);
        partsInitialized = false;
        updatePartPositions();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }

    private final class ChronophageCombatGoal extends Goal {
        @Override public boolean canUse() { LivingEntity target = getTarget(); return target != null && target.isAlive(); }
        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !(level() instanceof ServerLevel serverLevel)) return;
            double speed = phase() == 1 ? 0.62D : phase() == 2 ? 0.78D : 0.88D;
            speed *= 1.0D - brokenPhaseJointCount() * 0.08D;
            getNavigation().moveTo(target, Math.max(0.38D, speed));
            getLookControl().setLookAt(target, 72.0F, 50.0F);
            if (actionCooldown > 0) return;

            int choice = getRandom().nextInt(phase() == 3 ? 5 : 4);
            double distance = distanceTo(target);
            if (choice == 0 && distance <= 48.0D) {
                timeCutSlash(serverLevel, target);
                actionCooldown = phase() == 3 ? 34 : 46;
            } else if (choice == 1) {
                scheduleDelayedExplosion(target);
                actionCooldown = phase() == 3 ? 30 : 42;
            } else if (choice == 2 && brokenPhaseJointCount() < 4) {
                phaseMove(serverLevel, target);
                actionCooldown = 28 + brokenPhaseJointCount() * 5;
            } else {
                schedulePastEchoes(target);
                actionCooldown = phase() == 3 ? 36 : 54;
            }
        }
    }

    private enum PartKind { TIME_ORGAN, PHASE_JOINT, CENTRAL_RING }
    private record PartSpec(PartKind kind, int index, int mask, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class TemporalStrike {
        private final Vec3 center;
        private int ticks;
        private final double radius;
        private final double visibleDamage;
        private TemporalStrike(Vec3 center, int ticks, double radius, double visibleDamage) {
            this.center = center; this.ticks = ticks; this.radius = radius; this.visibleDamage = visibleDamage;
        }
    }

    private static final class ChronophagePart extends PartEntity<ChronophageEntity> {
        private final PartSpec spec;
        private final EntityDimensions dimensions;
        private float partHealth;

        private ChronophagePart(ChronophageEntity parent, PartSpec spec, float width, float height, float health) {
            super(parent);
            this.spec = spec;
            this.dimensions = EntityDimensions.scalable(width, height);
            this.partHealth = health;
            refreshDimensions();
        }

        private boolean broken() { return partHealth <= 0.0F; }
        private void setPartHealth(float health) { partHealth = Math.max(0.0F, health); }
        private void applyPartDamage(float amount) { setPartHealth(partHealth - Math.max(0.0F, amount)); }

        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
        @Override protected void readAdditionalSaveData(ValueInput input) {}
        @Override protected void addAdditionalSaveData(ValueOutput output) {}
        @Override public boolean isPickable() { return getParent().partPickable(spec); }
        @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            return !isInvulnerableToBase(source) && getParent().hurtPart(this, level, source, amount);
        }
        @Override public boolean is(Entity entity) { return this == entity || getParent() == entity; }
        @Override public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) { throw new UnsupportedOperationException(); }
        @Override public EntityDimensions getDimensions(Pose pose) { return dimensions; }
        @Override public boolean shouldBeSaved() { return false; }
    }
}

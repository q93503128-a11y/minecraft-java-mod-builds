package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import kr.moonseungjun.titanbreak.combat.WatcherRewardService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModEntities;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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

import java.util.ArrayList;
import java.util.List;

public final class HundredEyedWatcherEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int EYE_COUNT = 24;
    public static final int BRAIN_COUNT = 3;
    public static final int EYE_MASK = (1 << EYE_COUNT) - 1;
    public static final int BRAIN_0 = 1 << 24;
    public static final int BRAIN_1 = 1 << 25;
    public static final int BRAIN_2 = 1 << 26;
    public static final int CENTRAL_CORE = 1 << 27;
    public static final int ALL_PARTS_MASK = EYE_MASK | BRAIN_0 | BRAIN_1 | BRAIN_2 | CENTRAL_CORE;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 11_500.0D;

    // Balance thresholds: canonical specifies 20+ eyes and a later prediction-field phase, not exact counts.
    private static final int P2_EYES_BROKEN = 8;
    private static final int P3_EYES_BROKEN = 16;
    private static final int DECOY_ROTATE_TICKS = 90;
    private static final int FIELD_PULSE_TICKS = 72;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(HundredEyedWatcherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DECOY_EYE =
            SynchedEntityData.defineId(HundredEyedWatcherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PREDICTION_FIELD =
            SynchedEntityData.defineId(HundredEyedWatcherEntity.class, EntityDataSerializers.BOOLEAN);

    private static final PartSpec[] SPECS = buildSpecs();

    private final WatcherPart[] parts = new WatcherPart[SPECS.length];
    private final ServerBossEvent bossBar;
    private final List<PredictionStrike> strikes = new ArrayList<>();
    private boolean partsInitialized;
    private boolean rewardsGranted;
    private int actionCooldown = 45;
    private int droneCooldown = 120;
    private int fieldPulseCooldown = FIELD_PULSE_TICKS;
    private int decoyCooldown = DECOY_ROTATE_TICKS;
    private Vec3 lastTargetPos;
    private Vec3 lastMoveDirection = Vec3.ZERO;
    private double patternConfidence;

    public HundredEyedWatcherEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.hundred_eyed_watcher"),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new WatcherPart(this, spec, spec.width(), spec.height(), spec.health());
        }
        xpReward = 170;
    }

    private static PartSpec[] buildSpecs() {
        List<PartSpec> specs = new ArrayList<>();
        for (int i = 0; i < EYE_COUNT; i++) {
            int ring = i / 8;
            int spoke = i % 8;
            double angle = Math.toRadians(spoke * 45.0D + ring * 15.0D);
            double radius = 13.0D + ring * 4.0D;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = 20.0D + ring * 9.0D + ((spoke & 1) == 0 ? 2.0D : -2.0D);
            specs.add(new PartSpec(PartKind.EYE, i, 1 << i, x, y, z, 4.8F, 4.8F, 88.0F));
        }
        specs.add(new PartSpec(PartKind.BRAIN, 0, BRAIN_0, -9.0D, 44.0D, -3.0D, 8.0F, 8.0F, 330.0F));
        specs.add(new PartSpec(PartKind.BRAIN, 1, BRAIN_1, 0.0D, 49.0D, 4.0D, 8.0F, 8.0F, 330.0F));
        specs.add(new PartSpec(PartKind.BRAIN, 2, BRAIN_2, 9.0D, 44.0D, -3.0D, 8.0F, 8.0F, 330.0F));
        specs.add(new PartSpec(PartKind.CORE, 0, CENTRAL_CORE, 0.0D, 31.0D, -7.0D, 11.0F, 12.0F, 760.0F));
        return specs.toArray(PartSpec[]::new);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
        builder.define(DECOY_EYE, 0);
        builder.define(PREDICTION_FIELD, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new WatcherCombatGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        return 42;
    }

    public int brokenPartsMask() {
        return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK;
    }

    public boolean isPartBroken(int mask) {
        return (brokenPartsMask() & mask) != 0;
    }

    public int brokenEyeCount() {
        return Integer.bitCount(brokenPartsMask() & EYE_MASK);
    }

    public int brokenBrainCount() {
        return Integer.bitCount(brokenPartsMask() & (BRAIN_0 | BRAIN_1 | BRAIN_2));
    }

    public int phase() {
        int brokenEyes = brokenEyeCount();
        if (brokenEyes >= P3_EYES_BROKEN) return 3;
        if (brokenEyes >= P2_EYES_BROKEN) return 2;
        return 1;
    }

    public boolean centralCoreExposed() {
        return phase() == 3 && brokenBrainCount() >= BRAIN_COUNT;
    }

    public int decoyEyeIndex() {
        return Math.floorMod(getEntityData().get(DECOY_EYE), EYE_COUNT);
    }

    public boolean predictionFieldActive() {
        return getEntityData().get(PREDICTION_FIELD);
    }

    public double predictionQuality() {
        double eyeQuality = (EYE_COUNT - brokenEyeCount()) / (double) EYE_COUNT;
        double brainQuality = (BRAIN_COUNT - brokenBrainCount()) / (double) BRAIN_COUNT;
        return Mth.clamp(eyeQuality * 0.62D + brainQuality * 0.38D, 0.05D, 1.0D);
    }

    public double patternConfidence() {
        return Mth.clamp(patternConfidence, 0.0D, 1.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float effective = amount * (centralCoreExposed() ? 0.06F : 0.018F);
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
        updatePatternModel(serverLevel, target);
        tickPredictionStrikes(serverLevel);

        if (--decoyCooldown <= 0) {
            decoyCooldown = DECOY_ROTATE_TICKS;
            rotateDecoy();
        }
        boolean field = phase() == 3 && !centralCoreExposed();
        getEntityData().set(PREDICTION_FIELD, field);
        if (field && --fieldPulseCooldown <= 0 && target != null && target.isAlive()) {
            fieldPulseCooldown = FIELD_PULSE_TICKS;
            schedulePredictionField(target);
        }
        if (droneCooldown > 0) droneCooldown--;
    }

    private void updatePatternModel(ServerLevel level, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            patternConfidence *= 0.97D;
            lastTargetPos = null;
            lastMoveDirection = Vec3.ZERO;
            return;
        }
        Vec3 now = target.position();
        if (lastTargetPos == null) {
            lastTargetPos = now;
            return;
        }
        Vec3 movement = now.subtract(lastTargetPos);
        lastTargetPos = now;
        Vec3 horizontal = new Vec3(movement.x, 0.0D, movement.z);
        if (horizontal.lengthSqr() < 0.0025D) {
            patternConfidence *= 0.992D;
            return;
        }
        Vec3 direction = horizontal.normalize();
        if (lastMoveDirection.lengthSqr() > 0.5D) {
            double dot = direction.dot(lastMoveDirection);
            if (dot > 0.94D) patternConfidence += horizontal.length() > 0.32D ? 0.032D : 0.018D;
            else if (dot < 0.55D) patternConfidence -= 0.075D;
            else patternConfidence -= 0.018D;
        }
        lastMoveDirection = direction;

        if (target instanceof ServerPlayer player) {
            TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
            if (state.hasInstalled("optical_camo_skin")) patternConfidence -= 0.020D;
        }
        patternConfidence = Mth.clamp(patternConfidence, 0.0D, 1.0D);
    }

    private void rotateDecoy() {
        int candidate = getRandom().nextInt(EYE_COUNT);
        for (int i = 0; i < EYE_COUNT; i++) {
            int index = (candidate + i) % EYE_COUNT;
            if (!isPartBroken(1 << index)) {
                getEntityData().set(DECOY_EYE, index);
                return;
            }
        }
    }

    private boolean partPickable(PartSpec spec) {
        if (isPartBroken(spec.mask())) return false;
        if (spec.kind() == PartKind.EYE) return true;
        if (spec.kind() == PartKind.BRAIN) return phase() >= 2;
        return centralCoreExposed();
    }

    private boolean hurtPart(WatcherPart part, ServerLevel level, DamageSource source, float amount) {
        PartSpec spec = part.spec;
        if (!partPickable(spec)) return false;
        float effective = amount;
        if (spec.kind() == PartKind.EYE && phase() >= 2 && spec.index() == decoyEyeIndex()) {
            effective *= 0.28F;
        }
        part.applyPartDamage(effective);
        damageBodyFromPart(effective, spec.kind());
        if (!part.broken()) return true;

        markBroken(spec.mask());
        if (spec.kind() == PartKind.CORE) {
            return super.hurtServer(level, source, Float.MAX_VALUE);
        }
        if (spec.kind() == PartKind.EYE) rotateDecoy();
        return true;
    }

    private void markBroken(int mask) {
        getEntityData().set(BROKEN_PARTS, brokenPartsMask() | mask);
    }

    private void damageBodyFromPart(float amount, PartKind kind) {
        float multiplier = switch (kind) {
            case EYE -> 0.18F;
            case BRAIN -> 0.55F;
            case CORE -> 1.0F;
        };
        setHealth(Math.max(1.0F, getHealth() - Math.max(0.0F, amount * multiplier)));
    }

    private void schedulePredictiveBlast(LivingEntity target, int delay, double radius, double visibleDamage, double leadScale) {
        double quality = predictionQuality();
        double confidence = patternConfidence();
        double leadTicks = (5.0D + phase() * 2.5D + confidence * 10.0D) * quality * leadScale;
        Vec3 velocity = target.getDeltaMovement();
        Vec3 predicted = target.position().add(velocity.scale(leadTicks));
        double error = (1.0D - quality) * 8.0D + (1.0D - confidence) * 3.0D;
        if (target instanceof ServerPlayer player) {
            TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) level()).getServer()).state(player);
            if (state.hasInstalled("optical_camo_skin")) error += 5.0D;
        }
        if (error > 0.1D) {
            predicted = predicted.add((getRandom().nextDouble() - 0.5D) * error, 0.0D,
                    (getRandom().nextDouble() - 0.5D) * error);
        }
        strikes.add(new PredictionStrike(predicted, delay, radius, visibleDamage));
    }

    private void schedulePredictionField(LivingEntity target) {
        schedulePredictiveBlast(target, 24, 4.5D, 38.0D, 1.0D);
        schedulePredictiveBlast(target, 34, 5.0D, 42.0D, 1.25D);
        schedulePredictiveBlast(target, 44, 5.5D, 46.0D, 1.50D);
    }

    private void tickPredictionStrikes(ServerLevel level) {
        for (int i = strikes.size() - 1; i >= 0; i--) {
            PredictionStrike strike = strikes.get(i);
            if (--strike.ticks > 0) continue;
            detonatePrediction(level, strike);
            strikes.remove(i);
        }
    }

    private void detonatePrediction(ServerLevel level, PredictionStrike strike) {
        AABB area = new AABB(strike.center, strike.center).inflate(strike.radius, 4.0D, strike.radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            Vec3 flat = new Vec3(player.getX() - strike.center.x, 0.0D, player.getZ() - strike.center.z);
            if (flat.lengthSqr() > strike.radius * strike.radius) continue;
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(strike.visibleDamage));
            if (flat.lengthSqr() > 1.0E-6D) {
                Vec3 push = flat.normalize();
                player.push(push.x * 0.75D, 0.22D, push.z * 0.75D);
            }
        }
    }

    private void gazeLaser(ServerLevel level, LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        getLookControl().setLookAt(target, 72.0F, 48.0F);
        Vec3 origin = position().add(0.0D, 31.0D, 0.0D);
        Vec3 direction = target.getEyePosition().subtract(origin);
        if (direction.lengthSqr() < 1.0E-6D) return;
        direction = direction.normalize();
        double width = phase() == 3 ? 3.3D : phase() == 2 ? 2.8D : 2.3D;
        double damage = phase() == 3 ? 48.0D : 40.0D;
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(78.0D), Player::isAlive)) {
            Vec3 delta = player.getEyePosition().subtract(origin);
            double along = delta.dot(direction);
            if (along < 0.0D || along > 76.0D) continue;
            double perpendicular = delta.subtract(direction.scale(along)).length();
            if (perpendicular > width) continue;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(damage));
        }
    }

    private void lockOnReflection(ServerLevel level, LivingEntity target) {
        boolean assisted = false;
        if (target instanceof ServerPlayer player) {
            TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
            assisted = state.hasInstalled("target_assist") || state.hasInstalled("predictive_combat_core")
                    || state.hasInstalled("combat_autopilot") || state.hasInstalled("weakpoint_analysis_eye");
        }
        schedulePredictiveBlast(target, assisted ? 15 : 24, assisted ? 5.8D : 4.6D,
                assisted ? 52.0D : 40.0D, assisted ? 1.40D : 0.90D);
        if (assisted && phase() >= 2) {
            schedulePredictiveBlast(target, 28, 4.4D, 42.0D, 0.55D);
        }
    }

    private void deployTrackingEyes(ServerLevel level, LivingEntity target) {
        if (droneCooldown > 0) return;
        droneCooldown = phase() == 3 ? 120 : 180;
        spawnTracker(level, target, ModEntities.GLIDER.get(), -5.0D);
        spawnTracker(level, target, ModEntities.NEEDLER.get(), 5.0D);
        if (phase() == 3) spawnTracker(level, target, ModEntities.GLIDER.get(), 0.0D);
    }

    private void spawnTracker(ServerLevel level, LivingEntity target, EntityType<?> type, double side) {
        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (!(entity instanceof Mob mob)) return;
        Vec3 right = new Vec3(-getLookAngle().z, 0.0D, getLookAngle().x);
        Vec3 spawn = position().add(right.scale(side)).add(0.0D, 5.0D, 0.0D);
        mob.setPos(spawn.x, spawn.y, spawn.z);
        mob.setTarget(target);
        if (level.noCollision(mob)) level.addFreshEntity(mob);
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
            WatcherPart part = parts[i];
            Vec3 old = partsInitialized ? previous[i] : part.position();
            part.xo = old.x;
            part.yo = old.y;
            part.zo = old.z;
            part.xOld = old.x;
            part.yOld = old.y;
            part.zOld = old.z;
        }
        partsInitialized = true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (!partsInitialized) return getBoundingBox().inflate(30.0D, 68.0D, 30.0D);
        AABB bounds = getBoundingBox();
        for (WatcherPart part : parts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds.inflate(5.0D);
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
            WatcherRewardService.award(this, level, source);
        }
        super.die(source);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int saved = input.getIntOr("TitanbreakWatcherBrokenParts", 0) & ALL_PARTS_MASK;
        getEntityData().set(BROKEN_PARTS, saved);
        getEntityData().set(DECOY_EYE, input.getIntOr("TitanbreakWatcherDecoyEye", 0));
        getEntityData().set(PREDICTION_FIELD, false);
        patternConfidence = Mth.clamp(input.getDoubleOr("TitanbreakWatcherPatternConfidence", 0.0D), 0.0D, 1.0D);
        for (int i = 0; i < parts.length; i++) {
            float hp = input.getFloatOr("TitanbreakWatcherPartHealth" + i, SPECS[i].health());
            if ((saved & SPECS[i].mask()) != 0) hp = 0.0F;
            parts[i].setPartHealth(hp);
        }
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakWatcherBrokenParts", brokenPartsMask());
        output.putInt("TitanbreakWatcherDecoyEye", decoyEyeIndex());
        output.putDouble("TitanbreakWatcherPatternConfidence", patternConfidence());
        for (int i = 0; i < parts.length; i++) output.putFloat("TitanbreakWatcherPartHealth" + i, parts[i].partHealth);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

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

    private final class WatcherCombatGoal extends Goal {
        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !(level() instanceof ServerLevel serverLevel)) return;
            getNavigation().moveTo(target, phase() == 1 ? 0.58D : phase() == 2 ? 0.68D : 0.76D);
            getLookControl().setLookAt(target, 64.0F, 42.0F);
            if (actionCooldown > 0) {
                actionCooldown--;
                return;
            }
            int choices = phase() == 1 ? 3 : 5;
            int choice = getRandom().nextInt(choices);
            if (choice == 0) {
                gazeLaser(serverLevel, target);
                actionCooldown = phase() == 3 ? 32 : 42;
            } else if (choice == 1) {
                schedulePredictiveBlast(target, phase() == 3 ? 18 : 24, phase() == 3 ? 5.2D : 4.4D,
                        phase() == 3 ? 46.0D : 38.0D, 1.0D);
                actionCooldown = phase() == 3 ? 30 : 40;
            } else if (choice == 2) {
                deployTrackingEyes(serverLevel, target);
                actionCooldown = 48;
            } else if (choice == 3) {
                lockOnReflection(serverLevel, target);
                actionCooldown = 38;
            } else {
                schedulePredictionField(target);
                actionCooldown = 54;
            }
        }
    }

    private enum PartKind { EYE, BRAIN, CORE }

    private record PartSpec(PartKind kind, int index, int mask, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class PredictionStrike {
        private final Vec3 center;
        private int ticks;
        private final double radius;
        private final double visibleDamage;

        private PredictionStrike(Vec3 center, int ticks, double radius, double visibleDamage) {
            this.center = center;
            this.ticks = ticks;
            this.radius = radius;
            this.visibleDamage = visibleDamage;
        }
    }

    private static final class WatcherPart extends PartEntity<HundredEyedWatcherEntity> {
        private final PartSpec spec;
        private final EntityDimensions dimensions;
        private final float maxPartHealth;
        private float partHealth;

        private WatcherPart(HundredEyedWatcherEntity parent, PartSpec spec, float width, float height, float health) {
            super(parent);
            this.spec = spec;
            this.dimensions = EntityDimensions.scalable(width, height);
            this.maxPartHealth = health;
            this.partHealth = health;
            refreshDimensions();
        }

        private boolean broken() { return partHealth <= 0.0F; }
        private void setPartHealth(float health) { partHealth = Math.max(0.0F, Math.min(maxPartHealth, health)); }
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

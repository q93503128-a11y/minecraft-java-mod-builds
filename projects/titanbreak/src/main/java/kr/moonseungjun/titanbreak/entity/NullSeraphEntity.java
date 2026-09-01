package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.combat.AnalysisJammingService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.NullSeraphRewardService;
import kr.moonseungjun.titanbreak.combat.NullSuppressionService;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NullSeraphEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int SUPPRESSION_WING_0 = 1 << 0;
    public static final int SUPPRESSION_WING_1 = 1 << 1;
    public static final int SUPPRESSION_WING_2 = 1 << 2;
    public static final int SUPPRESSION_WING_3 = 1 << 3;
    public static final int NULL_CORE_LEFT = 1 << 4;
    public static final int NULL_CORE_RIGHT = 1 << 5;
    public static final int HEAD_RESONATOR = 1 << 6;
    public static final int SUPPRESSION_WING_MASK =
            SUPPRESSION_WING_0 | SUPPRESSION_WING_1 | SUPPRESSION_WING_2 | SUPPRESSION_WING_3;
    public static final int NULL_CORE_MASK = NULL_CORE_LEFT | NULL_CORE_RIGHT;
    public static final int ALL_PARTS_MASK = 0x7F;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 14_000.0D;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(NullSeraphEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(NullSeraphEntity.class, EntityDataSerializers.INT);

    private static final PartSpec[] SPECS = {
            new PartSpec(PartKind.SUPPRESSION_WING, 0, SUPPRESSION_WING_0, -17.0D, 24.0D, 2.0D, 10.0F, 21.0F, 420.0F),
            new PartSpec(PartKind.SUPPRESSION_WING, 1, SUPPRESSION_WING_1, 17.0D, 24.0D, 2.0D, 10.0F, 21.0F, 420.0F),
            new PartSpec(PartKind.SUPPRESSION_WING, 2, SUPPRESSION_WING_2, -13.0D, 37.0D, 1.0D, 9.0F, 18.0F, 420.0F),
            new PartSpec(PartKind.SUPPRESSION_WING, 3, SUPPRESSION_WING_3, 13.0D, 37.0D, 1.0D, 9.0F, 18.0F, 420.0F),
            new PartSpec(PartKind.NULL_CORE, 0, NULL_CORE_LEFT, -6.5D, 26.0D, -6.0D, 8.0F, 10.0F, 760.0F),
            new PartSpec(PartKind.NULL_CORE, 1, NULL_CORE_RIGHT, 6.5D, 26.0D, -6.0D, 8.0F, 10.0F, 760.0F),
            new PartSpec(PartKind.HEAD_RESONATOR, 0, HEAD_RESONATOR, 0.0D, 48.0D, -2.0D, 9.0F, 9.0F, 520.0F)
    };

    private final NullSeraphPart[] parts = new NullSeraphPart[SPECS.length];
    private final ServerBossEvent bossBar;
    private boolean partsInitialized;
    private boolean rewardsGranted;
    private int actionCooldown = 34;
    private int retargetCooldown = 10;

    public NullSeraphEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.null_seraph"),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new NullSeraphPart(this, spec, spec.width(), spec.height(), spec.health());
        }
        xpReward = 310;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
        builder.define(PHASE, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new NullSeraphCombatGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        return 70;
    }

    public int phase() { return getEntityData().get(PHASE); }
    public int brokenPartsMask() { return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK; }
    public boolean isPartBroken(int mask) { return (brokenPartsMask() & mask) != 0; }
    public int brokenSuppressionWingCount() { return Integer.bitCount(brokenPartsMask() & SUPPRESSION_WING_MASK); }
    public int brokenNullCoreCount() { return Integer.bitCount(brokenPartsMask() & NULL_CORE_MASK); }
    public boolean headResonatorBroken() { return isPartBroken(HEAD_RESONATOR); }
    public boolean suppressionFieldCollapsed() { return phase() == 3; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float effective = amount * (phase() == 3 ? 0.032F : 0.010F);
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

        updatePhase();
        if (actionCooldown > 0) actionCooldown--;
        if (--retargetCooldown <= 0) {
            retargetCooldown = phase() == 3 ? 16 : 28;
            retargetHighestAugmentationThreat(serverLevel);
        }
    }

    private void updatePhase() {
        int next = 1;
        int brokenWings = brokenSuppressionWingCount();
        if (brokenWings >= 2 || getHealth() <= getMaxHealth() * 0.72F) next = 2;
        if (brokenWings >= 4 || getHealth() <= getMaxHealth() * 0.32F) next = 3;
        getEntityData().set(PHASE, next);
    }

    private void retargetHighestAugmentationThreat(ServerLevel level) {
        List<ServerPlayer> candidates = level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(96.0D),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        ServerPlayer selected = null;
        double best = Double.NEGATIVE_INFINITY;
        for (ServerPlayer player : candidates) {
            double score = augmentationThreat(level, player) - Math.sqrt(distanceToSqr(player)) * 0.10D;
            if (score > best) {
                best = score;
                selected = player;
            }
        }
        if (selected != null) setTarget(selected);
    }

    private double augmentationThreat(ServerLevel level, ServerPlayer player) {
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        Set<String> unique = new LinkedHashSet<>(state.installedView().values());
        double score = unique.size() * 4.0D;
        for (String id : unique) {
            AugmentationCatalog.Definition definition = AugmentationCatalog.byId(id);
            if (definition == null) continue;
            score += definition.tier() * 8.0D
                    + Math.max(0, definition.powerLoad()) * 0.35D
                    + Math.max(0, definition.neuralLoad()) * 0.45D;
        }
        return score;
    }

    private int activeAugmentationCount(ServerLevel level, ServerPlayer player) {
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        return new LinkedHashSet<>(state.installedView().values()).size();
    }

    private void selectiveSuppression(ServerLevel level, ServerPlayer target) {
        if (phase() == 3) return;
        int brokenWings = brokenSuppressionWingCount();
        int baseDuration = phase() == 2 ? 170 : 125;
        int duration = Math.max(55, baseDuration - brokenWings * 22);
        int systems = phase() == 2 && brokenWings <= 1 ? 2 : 1;
        NullSuppressionService.apply(target, duration, systems);
        int jamTicks = Math.max(30, (phase() == 2 ? 105 : 72) - brokenWings * 12);
        if (headResonatorBroken()) jamTicks = Math.max(20, jamTicks / 2);
        AnalysisJammingService.apply(target, jamTicks);
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0D, target.getZ(),
                28, 1.2D, 1.8D, 1.2D, 0.08D);
    }

    private void powerCutWave(ServerLevel level) {
        double radius = phase() == 3 ? 24.0D : 38.0D;
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 20.0D, getZ(),
                52, radius * 0.35D, 7.0D, radius * 0.35D, 0.02D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(radius),
                ServerPlayer::isAlive)) {
            if (distanceToSqr(player) > radius * radius) continue;
            TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
            int installed = activeAugmentationCount(level, player);
            AugmentationResourceService.drainPower(player, state, 24.0D + installed * 2.5D);
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(phase() == 3 ? 28.0D : 20.0D));
        }
    }

    private void mentalResonance(ServerLevel level) {
        double radius = 46.0D;
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 34.0D, getZ(),
                64, 18.0D, 11.0D, 18.0D, 0.04D);
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(radius),
                ServerPlayer::isAlive)) {
            if (distanceToSqr(player) > radius * radius) continue;
            TitanPlayerData.State state = data.state(player);
            double loss = (phase() == 2 ? 9.0D : 5.0D) + activeAugmentationCount(level, player) * 0.45D;
            if (headResonatorBroken()) loss *= 0.40D;
            data.setSanity(player, state.sanity() - loss);
            AnalysisJammingService.apply(player, headResonatorBroken() ? 28 : 55);
        }
    }

    private void lanceAssault(ServerLevel level, LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        Vec3 delta = target.position().subtract(position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        if (horizontal.lengthSqr() > 1.0E-6D) {
            Vec3 direction = horizontal.normalize();
            setDeltaMovement(direction.scale(phase() == 3 ? 2.15D : 1.35D).add(0.0D, 0.12D, 0.0D));
            hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 28.0D, getZ(),
                22, 2.5D, 8.0D, 2.5D, 0.03D);
        if (distanceToSqr(target) <= 18.0D * 18.0D) {
            target.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(phase() == 3 ? 82.0D : 58.0D));
            Vec3 away = target.position().subtract(position());
            if (away.horizontalDistanceSqr() > 1.0E-6D) {
                Vec3 push = new Vec3(away.x, 0.0D, away.z).normalize();
                target.push(push.x * 0.85D, 0.22D, push.z * 0.85D);
            }
        }
    }

    private boolean partPickable(PartSpec spec) {
        if (isPartBroken(spec.mask())) return false;
        if (spec.kind() == PartKind.NULL_CORE) return phase() == 3;
        if (spec.kind() == PartKind.HEAD_RESONATOR) return phase() >= 2;
        return true;
    }

    private boolean hurtPart(NullSeraphPart part, ServerLevel level, DamageSource source, float amount) {
        PartSpec spec = part.spec;
        if (!partPickable(spec)) return false;
        float effective = Math.max(0.0F, amount);
        part.applyPartDamage(effective);
        float transfer = switch (spec.kind()) {
            case SUPPRESSION_WING -> effective * 0.30F;
            case NULL_CORE -> effective;
            case HEAD_RESONATOR -> effective * 0.38F;
        };
        setHealth(Math.max(1.0F, getHealth() - transfer));
        if (!part.broken()) return true;

        markBroken(spec.mask());
        if (spec.kind() == PartKind.NULL_CORE && brokenNullCoreCount() >= 2) {
            return super.hurtServer(level, source, Float.MAX_VALUE);
        }
        if (spec.kind() == PartKind.SUPPRESSION_WING) actionCooldown = Math.max(5, actionCooldown - 4);
        if (spec.kind() == PartKind.HEAD_RESONATOR) actionCooldown += 6;
        return true;
    }

    private void markBroken(int mask) {
        getEntityData().set(BROKEN_PARTS, brokenPartsMask() | mask);
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
            NullSeraphPart part = parts[i];
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
        if (!partsInitialized) return getBoundingBox().inflate(32.0D, 58.0D, 32.0D);
        AABB bounds = getBoundingBox();
        for (NullSeraphPart part : parts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds.inflate(7.0D);
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
            NullSeraphRewardService.award(this, level, source);
        }
        super.die(source);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int saved = input.getIntOr("TitanbreakNullSeraphBrokenParts", 0) & ALL_PARTS_MASK;
        getEntityData().set(BROKEN_PARTS, saved);
        getEntityData().set(PHASE, input.getIntOr("TitanbreakNullSeraphPhase", 1));
        for (int i = 0; i < parts.length; i++) {
            float hp = input.getFloatOr("TitanbreakNullSeraphPartHealth" + i, SPECS[i].health());
            if ((saved & SPECS[i].mask()) != 0) hp = 0.0F;
            parts[i].setPartHealth(hp);
        }
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakNullSeraphBrokenParts", brokenPartsMask());
        output.putInt("TitanbreakNullSeraphPhase", phase());
        for (int i = 0; i < parts.length; i++) {
            output.putFloat("TitanbreakNullSeraphPartHealth" + i, parts[i].partHealth);
        }
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

    private final class NullSeraphCombatGoal extends Goal {
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

            double distance = distanceTo(target);
            double speed = phase() == 3 ? 0.92D : phase() == 2 ? 0.62D : 0.50D;
            if (distance > (phase() == 3 ? 8.0D : 20.0D)) getNavigation().moveTo(target, speed);
            else getNavigation().stop();
            getLookControl().setLookAt(target, 95.0F, 70.0F);

            if (actionCooldown > 0) return;

            if (phase() == 3) {
                if (getRandom().nextInt(4) == 0) powerCutWave(serverLevel);
                else lanceAssault(serverLevel, target);
                actionCooldown = 14 + getRandom().nextInt(10);
                return;
            }

            int choice = getRandom().nextInt(phase() == 2 ? 5 : 4);
            if (choice <= 1 && target instanceof ServerPlayer player) {
                selectiveSuppression(serverLevel, player);
                actionCooldown = phase() == 2 ? 38 : 48;
            } else if (choice == 2) {
                powerCutWave(serverLevel);
                actionCooldown = phase() == 2 ? 34 : 44;
            } else if (phase() == 2 && choice == 3) {
                mentalResonance(serverLevel);
                actionCooldown = 42;
            } else {
                lanceAssault(serverLevel, target);
                actionCooldown = phase() == 2 ? 30 : 38;
            }
        }
    }

    private enum PartKind { SUPPRESSION_WING, NULL_CORE, HEAD_RESONATOR }

    private record PartSpec(PartKind kind, int index, int mask, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class NullSeraphPart extends PartEntity<NullSeraphEntity> {
        private final PartSpec spec;
        private final EntityDimensions dimensions;
        private float partHealth;

        private NullSeraphPart(NullSeraphEntity parent, PartSpec spec, float width, float height, float health) {
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
        @Override public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
            throw new UnsupportedOperationException();
        }
        @Override public EntityDimensions getDimensions(Pose pose) { return dimensions; }
        @Override public boolean shouldBeSaved() { return false; }
    }
}

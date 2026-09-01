package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.StormLeviathanRewardService;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
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

import java.util.ArrayList;
import java.util.List;

public final class StormLeviathanEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int WING_0 = 1 << 0;
    public static final int WING_1 = 1 << 1;
    public static final int WING_2 = 1 << 2;
    public static final int WING_3 = 1 << 3;
    public static final int ELECTRIC_SAC_0 = 1 << 4;
    public static final int ELECTRIC_SAC_1 = 1 << 5;
    public static final int ELECTRIC_SAC_2 = 1 << 6;
    public static final int ELECTRIC_SAC_3 = 1 << 7;
    public static final int ELECTRIC_SAC_4 = 1 << 8;
    public static final int ELECTRIC_SAC_5 = 1 << 9;
    public static final int HEAD_SENSOR = 1 << 10;
    public static final int STORM_ORGAN = 1 << 11;
    public static final int WING_MASK = WING_0 | WING_1 | WING_2 | WING_3;
    public static final int ELECTRIC_SAC_MASK = ELECTRIC_SAC_0 | ELECTRIC_SAC_1 | ELECTRIC_SAC_2
            | ELECTRIC_SAC_3 | ELECTRIC_SAC_4 | ELECTRIC_SAC_5;
    public static final int ALL_PARTS_MASK = 0xFFF;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 17_000.0D;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(StormLeviathanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(StormLeviathanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STORM_RADIUS =
            SynchedEntityData.defineId(StormLeviathanEntity.class, EntityDataSerializers.INT);

    private static final PartSpec[] SPECS = {
            new PartSpec(PartKind.WING, 0, WING_0, -38.0D, 13.0D, -17.0D, 22.0F, 7.0F, 420.0F),
            new PartSpec(PartKind.WING, 1, WING_1, 38.0D, 13.0D, -17.0D, 22.0F, 7.0F, 420.0F),
            new PartSpec(PartKind.WING, 2, WING_2, -32.0D, 11.0D, 18.0D, 20.0F, 7.0F, 420.0F),
            new PartSpec(PartKind.WING, 3, WING_3, 32.0D, 11.0D, 18.0D, 20.0F, 7.0F, 420.0F),
            new PartSpec(PartKind.ELECTRIC_SAC, 0, ELECTRIC_SAC_0, -10.0D, 12.0D, -28.0D, 8.0F, 8.0F, 300.0F),
            new PartSpec(PartKind.ELECTRIC_SAC, 1, ELECTRIC_SAC_1, 10.0D, 12.0D, -28.0D, 8.0F, 8.0F, 300.0F),
            new PartSpec(PartKind.ELECTRIC_SAC, 2, ELECTRIC_SAC_2, -11.0D, 10.0D, -2.0D, 8.0F, 8.0F, 300.0F),
            new PartSpec(PartKind.ELECTRIC_SAC, 3, ELECTRIC_SAC_3, 11.0D, 10.0D, -2.0D, 8.0F, 8.0F, 300.0F),
            new PartSpec(PartKind.ELECTRIC_SAC, 4, ELECTRIC_SAC_4, -9.0D, 9.0D, 25.0D, 7.0F, 7.0F, 300.0F),
            new PartSpec(PartKind.ELECTRIC_SAC, 5, ELECTRIC_SAC_5, 9.0D, 9.0D, 25.0D, 7.0F, 7.0F, 300.0F),
            new PartSpec(PartKind.HEAD_SENSOR, 0, HEAD_SENSOR, 0.0D, 14.0D, -48.0D, 12.0F, 10.0F, 450.0F),
            new PartSpec(PartKind.STORM_ORGAN, 0, STORM_ORGAN, 0.0D, 10.0D, 4.0D, 15.0F, 12.0F, 1100.0F)
    };

    private final StormLeviathanPart[] parts = new StormLeviathanPart[SPECS.length];
    private final ServerBossEvent bossBar;
    private final List<StormStrike> strikes = new ArrayList<>();
    private boolean partsInitialized;
    private boolean rewardsGranted;
    private boolean diveHit;
    private int actionCooldown = 54;
    private int diveTicks;
    private int fieldPulseCooldown = 72;

    public StormLeviathanEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.storm_leviathan"),
                BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new StormLeviathanPart(this, spec, spec.width(), spec.height(), spec.health());
        }
        setNoGravity(true);
        xpReward = 230;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
        builder.define(PHASE, 1);
        builder.define(STORM_RADIUS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new StormLeviathanCombatGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        return 55;
    }

    public int phase() { return getEntityData().get(PHASE); }
    public int stormRadius() { return getEntityData().get(STORM_RADIUS); }
    public int brokenPartsMask() { return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK; }
    public boolean isPartBroken(int mask) { return (brokenPartsMask() & mask) != 0; }
    public int brokenWingCount() { return Integer.bitCount(brokenPartsMask() & WING_MASK); }
    public int brokenElectricSacCount() { return Integer.bitCount(brokenPartsMask() & ELECTRIC_SAC_MASK); }
    public boolean headSensorBroken() { return isPartBroken(HEAD_SENSOR); }
    public boolean stormOrganExposed() { return phase() == 3 && brokenElectricSacCount() >= 4; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float effective = amount * (stormOrganExposed() ? 0.04F : 0.01F);
        if (effective <= 0.0F) return false;
        float before = getHealth();
        setHealth(Math.max(1.0F, before - effective));
        return getHealth() < before;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setNoGravity(true);
        updatePartPositions();
        if (!level().isClientSide()) {
            bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / Math.max(1.0F, getMaxHealth()))));
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;

        LivingEntity target = getTarget();
        updatePhase();
        tickStrikes(serverLevel);

        if (actionCooldown > 0) actionCooldown--;
        if (fieldPulseCooldown > 0) fieldPulseCooldown--;

        if (target == null || !target.isAlive()) {
            setDeltaMovement(getDeltaMovement().scale(0.92D).add(0.0D, 0.015D, 0.0D));
            return;
        }

        if (diveTicks > 0) tickDive(serverLevel, target);
        else tickFlight(target);

        if (phase() >= 2 && fieldPulseCooldown <= 0) {
            stormFieldPulse(serverLevel);
            fieldPulseCooldown = Math.max(34, (phase() == 3 ? 58 : 78) + brokenElectricSacCount() * 12);
        }
    }

    private void updatePhase() {
        int next = 1;
        if (getHealth() <= getMaxHealth() * 0.72F || brokenWingCount() >= 2) next = 2;
        if (getHealth() <= getMaxHealth() * 0.38F || brokenElectricSacCount() >= 4) next = 3;
        int radius = next == 1 ? 0 : Math.max(22, (next == 2 ? 72 : 96) - brokenElectricSacCount() * 8);
        getEntityData().set(PHASE, next);
        getEntityData().set(STORM_RADIUS, radius);
    }

    private void tickFlight(LivingEntity target) {
        double desiredHeight = phase() == 1 ? 34.0D : phase() == 2 ? 24.0D : 11.0D;
        double orbitRadius = phase() == 3 ? 22.0D : 34.0D;
        double angle = tickCount * (phase() == 3 ? 0.026D : 0.018D) + getId() * 0.21D;
        Vec3 desired = target.position().add(Math.cos(angle) * orbitRadius, desiredHeight, Math.sin(angle) * orbitRadius);
        Vec3 correction = desired.subtract(position());
        if (correction.lengthSqr() > 0.01D) {
            double acceleration = phase() == 3 ? 0.095D : 0.075D;
            setDeltaMovement(getDeltaMovement().scale(0.91D).add(correction.normalize().scale(acceleration)));
        }
        clampFlightSpeed(phase() == 3 ? 1.20D : 1.02D);
    }

    private void startDive() {
        if (brokenWingCount() >= 4) return;
        diveTicks = 30;
        diveHit = false;
    }

    private void tickDive(ServerLevel level, LivingEntity target) {
        diveTicks--;
        Vec3 aim = predictedTarget(target, 3.2D).subtract(position());
        if (aim.lengthSqr() > 1.0E-6D) {
            double thrust = Math.max(0.13D, 0.24D - brokenWingCount() * 0.025D);
            setDeltaMovement(getDeltaMovement().scale(0.78D).add(aim.normalize().scale(thrust)));
        }
        clampFlightSpeed(Math.max(0.82D, 1.65D - brokenWingCount() * 0.18D));
        if (!diveHit && distanceToSqr(target) <= 6.5D * 6.5D) {
            diveHit = true;
            target.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(78.0D));
            Vec3 push = target.position().subtract(position());
            if (push.lengthSqr() > 1.0E-6D) {
                push = push.normalize().scale(1.25D);
                target.push(push.x, 0.55D, push.z);
            }
        }
        if (diveTicks <= 0) {
            setDeltaMovement(getDeltaMovement().scale(0.55D).add(0.0D, 0.72D, 0.0D));
        }
    }

    private void clampFlightSpeed(double maxSpeed) {
        Vec3 motion = getDeltaMovement();
        if (motion.length() > maxSpeed) setDeltaMovement(motion.normalize().scale(maxSpeed));
        hurtMarked = true;
    }

    private Vec3 predictedTarget(LivingEntity target, double leadTicks) {
        Vec3 predicted = target.position().add(target.getDeltaMovement().scale(leadTicks));
        if (!headSensorBroken()) return predicted;
        double spread = 4.0D + getRandom().nextDouble() * 4.0D;
        double angle = getRandom().nextDouble() * Math.PI * 2.0D;
        return predicted.add(Math.cos(angle) * spread, 0.0D, Math.sin(angle) * spread);
    }

    private void chainLightning(ServerLevel level, LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        int chain = 0;
        if (target instanceof Player player && player.isAlive()) {
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(58.0D));
            chain = 1;
        }
        for (Player player : level.getEntitiesOfClass(Player.class, target.getBoundingBox().inflate(38.0D), Player::isAlive)) {
            if (player == target) continue;
            double visibleDamage = chain == 0 ? 58.0D : chain == 1 ? 42.0D : 30.0D;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(visibleDamage));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0D, player.getZ(),
                    18, 0.8D, 1.0D, 0.8D, 0.08D);
            if (++chain >= 3) break;
        }
    }

    private void windPressure(ServerLevel level) {
        swing(InteractionHand.MAIN_HAND);
        double radius = phase() == 3 ? 30.0D : 24.0D;
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 8.0D, getZ(), 64,
                radius * 0.45D, 5.0D, radius * 0.45D, 0.12D);
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), Player::isAlive)) {
            Vec3 push = player.position().subtract(position());
            if (push.lengthSqr() <= 1.0E-6D) continue;
            double distance = Math.max(1.0D, push.length());
            double strength = Math.max(0.25D, 1.35D - distance / radius);
            Vec3 force = push.normalize().scale(strength);
            player.push(force.x, 0.30D + strength * 0.18D, force.z);
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(20.0D));
        }
    }

    private void scheduleElectricOrbs(LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        for (int i = 0; i < 3; i++) {
            Vec3 center = predictedTarget(target, 5.0D + i * 2.5D);
            strikes.add(new StormStrike(center, 18 + i * 8, 4.5D + i * 0.7D, 38.0D + i * 6.0D));
        }
    }

    private void scheduleGuidedLightning(LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        for (int i = 0; i < 4; i++) {
            Vec3 center = predictedTarget(target, 4.0D + i * 2.0D);
            strikes.add(new StormStrike(center, 8 + i * 7, 3.8D, 46.0D));
        }
    }

    private void tickStrikes(ServerLevel level) {
        for (int i = strikes.size() - 1; i >= 0; i--) {
            StormStrike strike = strikes.get(i);
            if (strike.ticks % 3 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strike.center.x, strike.center.y + 0.8D, strike.center.z,
                        10, strike.radius * 0.55D, 0.20D, strike.radius * 0.55D, 0.03D);
            }
            if (--strike.ticks > 0) continue;
            detonate(level, strike);
            strikes.remove(i);
        }
    }

    private void detonate(ServerLevel level, StormStrike strike) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strike.center.x, strike.center.y + 1.0D, strike.center.z,
                42, strike.radius, 2.0D, strike.radius, 0.16D);
        AABB area = new AABB(strike.center, strike.center).inflate(strike.radius, 5.0D, strike.radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            Vec3 flat = new Vec3(player.getX() - strike.center.x, 0.0D, player.getZ() - strike.center.z);
            if (flat.lengthSqr() > strike.radius * strike.radius) continue;
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(strike.visibleDamage));
            player.push(0.0D, 0.28D, 0.0D);
        }
    }

    private void stormFieldPulse(ServerLevel level) {
        double radius = stormRadius();
        if (radius <= 0.0D) return;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 7.0D, getZ(), 36,
                radius * 0.35D, 8.0D, radius * 0.35D, 0.06D);
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), Player::isAlive)) {
            if (distanceToSqr(player) > radius * radius) continue;
            double damage = phase() == 3 ? 30.0D : 22.0D;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(damage));
            Vec3 lateral = player.position().subtract(position());
            if (lateral.horizontalDistanceSqr() > 1.0E-6D) {
                lateral = new Vec3(lateral.x, 0.0D, lateral.z).normalize().scale(0.32D);
                player.push(lateral.x, 0.10D, lateral.z);
            }
        }
    }

    private boolean partPickable(PartSpec spec) {
        if (isPartBroken(spec.mask())) return false;
        if (spec.kind() == PartKind.STORM_ORGAN) return stormOrganExposed();
        return true;
    }

    private boolean hurtPart(StormLeviathanPart part, ServerLevel level, DamageSource source, float amount) {
        PartSpec spec = part.spec;
        if (!partPickable(spec)) return false;
        float effective = Math.max(0.0F, amount);
        part.applyPartDamage(effective);
        float transfer = switch (spec.kind()) {
            case WING -> effective * 0.12F;
            case ELECTRIC_SAC -> effective * 0.34F;
            case HEAD_SENSOR -> effective * 0.20F;
            case STORM_ORGAN -> effective;
        };
        setHealth(Math.max(1.0F, getHealth() - transfer));
        if (!part.broken()) return true;

        markBroken(spec.mask());
        if (spec.kind() == PartKind.STORM_ORGAN) {
            return super.hurtServer(level, source, Float.MAX_VALUE);
        }
        if (spec.kind() == PartKind.ELECTRIC_SAC) {
            actionCooldown += 16;
            fieldPulseCooldown += 18;
        } else if (spec.kind() == PartKind.WING) {
            actionCooldown += 10;
        } else {
            actionCooldown += 20;
        }
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
            StormLeviathanPart part = parts[i];
            Vec3 old = partsInitialized ? previous[i] : part.position();
            part.xo = old.x; part.yo = old.y; part.zo = old.z;
            part.xOld = old.x; part.yOld = old.y; part.zOld = old.z;
        }
        partsInitialized = true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (!partsInitialized) return getBoundingBox().inflate(68.0D, 34.0D, 68.0D);
        AABB bounds = getBoundingBox();
        for (StormLeviathanPart part : parts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds.inflate(8.0D);
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
            StormLeviathanRewardService.award(this, level, source);
        }
        super.die(source);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int saved = input.getIntOr("TitanbreakStormLeviathanBrokenParts", 0) & ALL_PARTS_MASK;
        getEntityData().set(BROKEN_PARTS, saved);
        getEntityData().set(PHASE, input.getIntOr("TitanbreakStormLeviathanPhase", 1));
        getEntityData().set(STORM_RADIUS, input.getIntOr("TitanbreakStormLeviathanStormRadius", 0));
        for (int i = 0; i < parts.length; i++) {
            float hp = input.getFloatOr("TitanbreakStormLeviathanPartHealth" + i, SPECS[i].health());
            if ((saved & SPECS[i].mask()) != 0) hp = 0.0F;
            parts[i].setPartHealth(hp);
        }
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakStormLeviathanBrokenParts", brokenPartsMask());
        output.putInt("TitanbreakStormLeviathanPhase", phase());
        output.putInt("TitanbreakStormLeviathanStormRadius", stormRadius());
        for (int i = 0; i < parts.length; i++) {
            output.putFloat("TitanbreakStormLeviathanPartHealth" + i, parts[i].partHealth);
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

    private final class StormLeviathanCombatGoal extends Goal {
        @Override public boolean canUse() { LivingEntity target = getTarget(); return target != null && target.isAlive(); }
        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !(level() instanceof ServerLevel serverLevel)) return;
            getLookControl().setLookAt(target, 90.0F, 70.0F);
            if (actionCooldown > 0 || diveTicks > 0) return;

            int options = phase() == 1 ? 3 : phase() == 2 ? 4 : 5;
            int choice = getRandom().nextInt(options);
            if (choice == 0 && brokenWingCount() < 4) {
                startDive();
                actionCooldown = 64 + brokenWingCount() * 16;
            } else if (choice == 1) {
                chainLightning(serverLevel, target);
                actionCooldown = (phase() == 3 ? 42 : 56) + brokenElectricSacCount() * 10;
            } else if (choice == 2) {
                windPressure(serverLevel);
                actionCooldown = 48 + brokenElectricSacCount() * 8;
            } else if (choice == 3) {
                scheduleElectricOrbs(target);
                actionCooldown = 54 + brokenElectricSacCount() * 9;
            } else {
                scheduleGuidedLightning(target);
                actionCooldown = 44 + brokenElectricSacCount() * 10;
            }
        }
    }

    private enum PartKind { WING, ELECTRIC_SAC, HEAD_SENSOR, STORM_ORGAN }

    private record PartSpec(PartKind kind, int index, int mask, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class StormStrike {
        private final Vec3 center;
        private int ticks;
        private final double radius;
        private final double visibleDamage;

        private StormStrike(Vec3 center, int ticks, double radius, double visibleDamage) {
            this.center = center;
            this.ticks = ticks;
            this.radius = radius;
            this.visibleDamage = visibleDamage;
        }
    }

    private static final class StormLeviathanPart extends PartEntity<StormLeviathanEntity> {
        private final PartSpec spec;
        private final EntityDimensions dimensions;
        private float partHealth;

        private StormLeviathanPart(StormLeviathanEntity parent, PartSpec spec, float width, float height, float health) {
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

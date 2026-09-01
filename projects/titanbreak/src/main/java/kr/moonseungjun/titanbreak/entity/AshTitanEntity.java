package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.AshTitanRewardService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
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

public final class AshTitanEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int COOLING_PLATE_0 = 1 << 0;
    public static final int COOLING_PLATE_1 = 1 << 1;
    public static final int COOLING_PLATE_2 = 1 << 2;
    public static final int COOLING_PLATE_3 = 1 << 3;
    public static final int COOLING_PLATE_4 = 1 << 4;
    public static final int COOLING_PLATE_5 = 1 << 5;
    public static final int RADIATION_ARM_LEFT = 1 << 6;
    public static final int RADIATION_ARM_RIGHT = 1 << 7;
    public static final int RADIANT_HEART = 1 << 8;
    public static final int HEAD_SENSOR = 1 << 9;
    public static final int COOLING_PLATE_MASK = COOLING_PLATE_0 | COOLING_PLATE_1 | COOLING_PLATE_2
            | COOLING_PLATE_3 | COOLING_PLATE_4 | COOLING_PLATE_5;
    public static final int RADIATION_ARM_MASK = RADIATION_ARM_LEFT | RADIATION_ARM_RIGHT;
    public static final int ALL_PARTS_MASK = 0x3FF;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 20_000.0D;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(AshTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(AshTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEAT_LEVEL =
            SynchedEntityData.defineId(AshTitanEntity.class, EntityDataSerializers.INT);

    private static final PartSpec[] SPECS = {
            new PartSpec(PartKind.COOLING_PLATE, 0, COOLING_PLATE_0, -10.0D, 48.0D, 9.0D, 9.0F, 11.0F, 360.0F),
            new PartSpec(PartKind.COOLING_PLATE, 1, COOLING_PLATE_1, 10.0D, 48.0D, 9.0D, 9.0F, 11.0F, 360.0F),
            new PartSpec(PartKind.COOLING_PLATE, 2, COOLING_PLATE_2, -13.0D, 58.0D, 7.0D, 9.0F, 11.0F, 360.0F),
            new PartSpec(PartKind.COOLING_PLATE, 3, COOLING_PLATE_3, 13.0D, 58.0D, 7.0D, 9.0F, 11.0F, 360.0F),
            new PartSpec(PartKind.COOLING_PLATE, 4, COOLING_PLATE_4, -8.0D, 68.0D, 5.0D, 8.0F, 10.0F, 360.0F),
            new PartSpec(PartKind.COOLING_PLATE, 5, COOLING_PLATE_5, 8.0D, 68.0D, 5.0D, 8.0F, 10.0F, 360.0F),
            new PartSpec(PartKind.RADIATION_ARM, 0, RADIATION_ARM_LEFT, -28.0D, 42.0D, 0.0D, 12.0F, 28.0F, 560.0F),
            new PartSpec(PartKind.RADIATION_ARM, 1, RADIATION_ARM_RIGHT, 28.0D, 42.0D, 0.0D, 12.0F, 28.0F, 560.0F),
            new PartSpec(PartKind.RADIANT_HEART, 0, RADIANT_HEART, 0.0D, 49.0D, -12.0D, 16.0F, 17.0F, 1_450.0F),
            new PartSpec(PartKind.HEAD_SENSOR, 0, HEAD_SENSOR, 0.0D, 78.0D, -5.0D, 12.0F, 10.0F, 440.0F)
    };

    private final AshTitanPart[] parts = new AshTitanPart[SPECS.length];
    private final ServerBossEvent bossBar;
    private final List<HeatZone> heatZones = new ArrayList<>();
    private final List<MoltenStrike> moltenStrikes = new ArrayList<>();
    private final List<BeamSweep> beamSweeps = new ArrayList<>();
    private boolean partsInitialized;
    private boolean rewardsGranted;
    private int actionCooldown = 48;
    private int heatPulseCooldown = 18;

    public AshTitanEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.ash_titan"),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new AshTitanPart(this, spec, spec.width(), spec.height(), spec.health());
        }
        xpReward = 260;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
        builder.define(PHASE, 1);
        builder.define(HEAT_LEVEL, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new AshTitanCombatGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        return 20;
    }

    public int phase() { return getEntityData().get(PHASE); }
    public int heatLevel() { return getEntityData().get(HEAT_LEVEL); }
    public int brokenPartsMask() { return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK; }
    public boolean isPartBroken(int mask) { return (brokenPartsMask() & mask) != 0; }
    public int brokenCoolingPlateCount() { return Integer.bitCount(brokenPartsMask() & COOLING_PLATE_MASK); }
    public int brokenRadiationArmCount() { return Integer.bitCount(brokenPartsMask() & RADIATION_ARM_MASK); }
    public boolean headSensorBroken() { return isPartBroken(HEAD_SENSOR); }
    public boolean radiantHeartExposed() { return phase() == 3 && brokenCoolingPlateCount() >= 4; }

    private double overheatMultiplier() {
        return 1.0D + brokenCoolingPlateCount() * 0.115D + (phase() == 3 ? 0.16D : 0.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float effective = amount * (radiantHeartExposed() ? 0.045F : 0.010F);
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
        tickHeatZones(serverLevel);
        tickMoltenStrikes(serverLevel);
        tickBeamSweeps(serverLevel);

        if (actionCooldown > 0) actionCooldown--;
        if (heatPulseCooldown > 0) heatPulseCooldown--;

        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && heatPulseCooldown <= 0) {
            passiveHeatField(serverLevel);
            heatPulseCooldown = phase() == 3 ? 10 : phase() == 2 ? 14 : 18;
        }
    }

    private void updatePhase() {
        int broken = brokenCoolingPlateCount();
        int next = 1;
        if (broken >= 2 || getHealth() <= getMaxHealth() * 0.72F) next = 2;
        if (broken >= 4 || getHealth() <= getMaxHealth() * 0.34F) next = 3;
        getEntityData().set(PHASE, next);
        getEntityData().set(HEAT_LEVEL, Math.min(100, broken * 13 + (next - 1) * 16));
    }

    private Vec3 predictedTarget(LivingEntity target, double leadTicks) {
        Vec3 predicted = target.position().add(target.getDeltaMovement().scale(leadTicks));
        if (!headSensorBroken()) return predicted;
        double spread = 3.5D + getRandom().nextDouble() * 5.0D;
        double angle = getRandom().nextDouble() * Math.PI * 2.0D;
        return predicted.add(Math.cos(angle) * spread, 0.0D, Math.sin(angle) * spread);
    }

    private void passiveHeatField(ServerLevel level) {
        double radius = phase() == 1 ? 24.0D : phase() == 2 ? 29.0D : 35.0D;
        radius += brokenCoolingPlateCount() * 1.5D;
        double visibleDamage = (phase() == 3 ? 8.0D : phase() == 2 ? 6.0D : 4.0D) * overheatMultiplier();
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.0D, getZ(), 22,
                radius * 0.20D, 2.0D, radius * 0.20D, 0.02D);
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), Player::isAlive)) {
            if (distanceToSqr(player) > radius * radius) continue;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(visibleDamage));
        }
    }

    private void heatShockwave(ServerLevel level) {
        swing(InteractionHand.MAIN_HAND);
        double radius = phase() == 3 ? 31.0D : 25.0D;
        double visibleDamage = (phase() == 3 ? 58.0D : 44.0D) * overheatMultiplier();
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.0D, getZ(), 72,
                radius * 0.42D, 2.5D, radius * 0.42D, 0.08D);
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), Player::isAlive)) {
            Vec3 push = player.position().subtract(position());
            if (push.horizontalDistanceSqr() <= 1.0E-6D) continue;
            if (push.horizontalDistanceSqr() > radius * radius) continue;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(visibleDamage));
            Vec3 horizontal = new Vec3(push.x, 0.0D, push.z).normalize();
            player.push(horizontal.x * 1.15D, 0.35D, horizontal.z * 1.15D);
        }
    }

    private void meltGround(LivingEntity target) {
        Vec3 center = predictedTarget(target, 7.0D);
        int count = phase() == 3 ? 5 : 3;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i / count) + getRandom().nextDouble() * 0.4D;
            double distance = i == 0 ? 0.0D : 4.5D + getRandom().nextDouble() * 6.0D;
            Vec3 point = center.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            heatZones.add(new HeatZone(point, phase() == 3 ? 130 : 100,
                    phase() == 3 ? 6.5D : 5.2D, phase() == 3 ? 12.0D : 9.0D));
        }
    }

    private void overheatZone(LivingEntity target) {
        Vec3 center = predictedTarget(target, 4.0D);
        heatZones.add(new HeatZone(center, phase() == 3 ? 160 : 120,
                phase() == 3 ? 9.0D : 7.0D, phase() == 3 ? 14.0D : 10.0D));
    }

    private void launchMoltenProjectile(LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        Vec3 center = predictedTarget(target, headSensorBroken() ? 4.0D : 9.0D);
        moltenStrikes.add(new MoltenStrike(center, phase() == 3 ? 14 : 22,
                phase() == 3 ? 7.0D : 5.5D,
                (phase() == 3 ? 72.0D : 54.0D) * overheatMultiplier()));
    }

    private void startBeamSweep(LivingEntity target) {
        if (brokenRadiationArmCount() >= 2) {
            launchMoltenProjectile(target);
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        Vec3 delta = target.position().subtract(position());
        double centerYaw = Math.atan2(delta.z, delta.x);
        double sweep = phase() == 3 ? 1.30D : 0.92D;
        double startYaw = centerYaw - sweep * 0.5D;
        int ticks = phase() == 3 ? 28 : 24;
        int activeArms = Math.max(1, 2 - brokenRadiationArmCount());
        double range = phase() == 3 ? 58.0D : 48.0D;
        double width = activeArms == 2 ? 3.6D : 2.8D;
        double damage = (phase() == 3 ? 64.0D : 48.0D) * overheatMultiplier();
        beamSweeps.add(new BeamSweep(startYaw, sweep / ticks, ticks, range, width, damage));
    }

    private void tickBeamSweeps(ServerLevel level) {
        for (int i = beamSweeps.size() - 1; i >= 0; i--) {
            BeamSweep beam = beamSweeps.get(i);
            beam.age++;
            double yaw = beam.startYaw + beam.stepYaw * beam.age;
            Vec3 direction = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            double sampleDistance = 20.0D + (beam.age % 5) * 6.0D;
            Vec3 sample = position().add(direction.scale(sampleDistance));
            level.sendParticles(ParticleTypes.FLAME, sample.x, getY() + 3.0D, sample.z,
                    10, 1.2D, 1.2D, 1.2D, 0.02D);

            if ((beam.age & 3) == 0) {
                for (Player player : level.getEntitiesOfClass(Player.class,
                        getBoundingBox().inflate(beam.range), Player::isAlive)) {
                    Vec3 relative = player.position().subtract(position());
                    double along = relative.x * direction.x + relative.z * direction.z;
                    if (along < 0.0D || along > beam.range) continue;
                    double lateralX = relative.x - direction.x * along;
                    double lateralZ = relative.z - direction.z * along;
                    double lateral = Math.sqrt(lateralX * lateralX + lateralZ * lateralZ);
                    if (lateral > beam.width) continue;
                    player.hurtServer(level, damageSources().mobAttack(this),
                            (float) CombatScale.toInternal(beam.visibleDamage));
                }
            }

            if (beam.age >= beam.totalTicks) beamSweeps.remove(i);
        }
    }

    private void tickMoltenStrikes(ServerLevel level) {
        for (int i = moltenStrikes.size() - 1; i >= 0; i--) {
            MoltenStrike strike = moltenStrikes.get(i);
            if (--strike.ticks > 0) {
                if ((strike.ticks & 3) == 0) {
                    level.sendParticles(ParticleTypes.FLAME, strike.center.x, strike.center.y + 0.5D, strike.center.z,
                            12, strike.radius * 0.35D, 0.4D, strike.radius * 0.35D, 0.02D);
                }
                continue;
            }

            level.sendParticles(ParticleTypes.LAVA, strike.center.x, strike.center.y + 0.5D, strike.center.z,
                    34, strike.radius * 0.45D, 1.0D, strike.radius * 0.45D, 0.10D);
            AABB area = new AABB(strike.center, strike.center).inflate(strike.radius, 4.0D, strike.radius);
            for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
                if (player.position().distanceToSqr(strike.center) > strike.radius * strike.radius) continue;
                player.hurtServer(level, damageSources().mobAttack(this),
                        (float) CombatScale.toInternal(strike.visibleDamage));
            }
            heatZones.add(new HeatZone(strike.center, 90, strike.radius + 1.0D, 10.0D));
            moltenStrikes.remove(i);
        }
    }

    private void tickHeatZones(ServerLevel level) {
        for (int i = heatZones.size() - 1; i >= 0; i--) {
            HeatZone zone = heatZones.get(i);
            zone.ticks--;
            if (zone.ticks % 6 == 0) {
                level.sendParticles(ParticleTypes.FLAME, zone.center.x, zone.center.y + 0.35D, zone.center.z,
                        8, zone.radius * 0.40D, 0.25D, zone.radius * 0.40D, 0.01D);
            }
            if (zone.ticks % 8 == 0) {
                AABB area = new AABB(zone.center, zone.center).inflate(zone.radius, 3.0D, zone.radius);
                for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
                    Vec3 delta = player.position().subtract(zone.center);
                    if (delta.x * delta.x + delta.z * delta.z > zone.radius * zone.radius) continue;
                    player.hurtServer(level, damageSources().mobAttack(this),
                            (float) CombatScale.toInternal(zone.visibleDamage * overheatMultiplier()));
                }
            }
            if (zone.ticks <= 0) heatZones.remove(i);
        }
    }

    private boolean partPickable(PartSpec spec) {
        if (isPartBroken(spec.mask())) return false;
        if (spec.kind() == PartKind.RADIANT_HEART) return radiantHeartExposed();
        return true;
    }

    private boolean hurtPart(AshTitanPart part, ServerLevel level, DamageSource source, float amount) {
        PartSpec spec = part.spec;
        if (!partPickable(spec)) return false;
        float effective = Math.max(0.0F, amount);
        part.applyPartDamage(effective);
        float transfer = switch (spec.kind()) {
            case COOLING_PLATE -> effective * 0.34F;
            case RADIATION_ARM -> effective * 0.45F;
            case HEAD_SENSOR -> effective * 0.25F;
            case RADIANT_HEART -> effective;
        };
        setHealth(Math.max(1.0F, getHealth() - transfer));
        if (!part.broken()) return true;

        markBroken(spec.mask());
        if (spec.kind() == PartKind.RADIANT_HEART) {
            return super.hurtServer(level, source, Float.MAX_VALUE);
        }
        if (spec.kind() == PartKind.COOLING_PLATE) {
            actionCooldown = Math.max(4, actionCooldown - 8);
            heatPulseCooldown = Math.max(4, heatPulseCooldown - 3);
        } else if (spec.kind() == PartKind.RADIATION_ARM) {
            actionCooldown += 14;
        } else if (spec.kind() == PartKind.HEAD_SENSOR) {
            actionCooldown += 8;
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
            AshTitanPart part = parts[i];
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
        if (!partsInitialized) return getBoundingBox().inflate(44.0D, 108.0D, 44.0D);
        AABB bounds = getBoundingBox();
        for (AshTitanPart part : parts) bounds = bounds.minmax(part.getBoundingBox());
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
            AshTitanRewardService.award(this, level, source);
        }
        super.die(source);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int saved = input.getIntOr("TitanbreakAshTitanBrokenParts", 0) & ALL_PARTS_MASK;
        getEntityData().set(BROKEN_PARTS, saved);
        getEntityData().set(PHASE, input.getIntOr("TitanbreakAshTitanPhase", 1));
        getEntityData().set(HEAT_LEVEL, input.getIntOr("TitanbreakAshTitanHeat", 0));
        for (int i = 0; i < parts.length; i++) {
            float hp = input.getFloatOr("TitanbreakAshTitanPartHealth" + i, SPECS[i].health());
            if ((saved & SPECS[i].mask()) != 0) hp = 0.0F;
            parts[i].setPartHealth(hp);
        }
        heatZones.clear();
        moltenStrikes.clear();
        beamSweeps.clear();
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakAshTitanBrokenParts", brokenPartsMask());
        output.putInt("TitanbreakAshTitanPhase", phase());
        output.putInt("TitanbreakAshTitanHeat", heatLevel());
        for (int i = 0; i < parts.length; i++) {
            output.putFloat("TitanbreakAshTitanPartHealth" + i, parts[i].partHealth);
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

    private final class AshTitanCombatGoal extends Goal {
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
            double speed = phase() == 1 ? 0.42D : phase() == 2 ? 0.49D : 0.56D;
            if (distance > 18.0D) getNavigation().moveTo(target, speed);
            else getNavigation().stop();
            getLookControl().setLookAt(target, 68.0F, 44.0F);

            if (actionCooldown > 0) return;

            int options = phase() == 1 ? 3 : phase() == 2 ? 5 : 6;
            int choice = getRandom().nextInt(options);
            if (choice == 0 && distance <= 34.0D) {
                heatShockwave(serverLevel);
                actionCooldown = phase() == 3 ? 28 : 40;
            } else if (choice == 1) {
                meltGround(target);
                actionCooldown = phase() == 3 ? 24 : 38;
            } else if (choice == 2) {
                launchMoltenProjectile(target);
                actionCooldown = phase() == 3 ? 22 : 34;
            } else if (choice == 3 && phase() >= 2) {
                startBeamSweep(target);
                actionCooldown = phase() == 3 ? 30 : 46;
            } else if (choice == 4 && phase() >= 2) {
                overheatZone(target);
                actionCooldown = phase() == 3 ? 24 : 38;
            } else {
                startBeamSweep(target);
                overheatZone(target);
                actionCooldown = 34;
            }
        }
    }

    private enum PartKind { COOLING_PLATE, RADIATION_ARM, RADIANT_HEART, HEAD_SENSOR }

    private record PartSpec(PartKind kind, int index, int mask, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class HeatZone {
        private final Vec3 center;
        private int ticks;
        private final double radius;
        private final double visibleDamage;

        private HeatZone(Vec3 center, int ticks, double radius, double visibleDamage) {
            this.center = center;
            this.ticks = ticks;
            this.radius = radius;
            this.visibleDamage = visibleDamage;
        }
    }

    private static final class MoltenStrike {
        private final Vec3 center;
        private int ticks;
        private final double radius;
        private final double visibleDamage;

        private MoltenStrike(Vec3 center, int ticks, double radius, double visibleDamage) {
            this.center = center;
            this.ticks = ticks;
            this.radius = radius;
            this.visibleDamage = visibleDamage;
        }
    }

    private static final class BeamSweep {
        private final double startYaw;
        private final double stepYaw;
        private final int totalTicks;
        private int age;
        private final double range;
        private final double width;
        private final double visibleDamage;

        private BeamSweep(double startYaw, double stepYaw, int totalTicks,
                          double range, double width, double visibleDamage) {
            this.startYaw = startYaw;
            this.stepYaw = stepYaw;
            this.totalTicks = totalTicks;
            this.range = range;
            this.width = width;
            this.visibleDamage = visibleDamage;
        }
    }

    private static final class AshTitanPart extends PartEntity<AshTitanEntity> {
        private final PartSpec spec;
        private final EntityDimensions dimensions;
        private float partHealth;

        private AshTitanPart(AshTitanEntity parent, PartSpec spec, float width, float height, float health) {
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

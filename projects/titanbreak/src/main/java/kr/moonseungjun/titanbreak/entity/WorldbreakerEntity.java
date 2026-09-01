package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.BreachService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import kr.moonseungjun.titanbreak.combat.WorldbreakerRewardService;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.List;

public final class WorldbreakerEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int LEG_AXIS_0 = 1 << 0;
    public static final int LEG_AXIS_1 = 1 << 1;
    public static final int LEG_AXIS_2 = 1 << 2;
    public static final int LEG_AXIS_3 = 1 << 3;
    public static final int ARM_LEFT = 1 << 4;
    public static final int ARM_RIGHT = 1 << 5;
    public static final int OUTER_CORE_0 = 1 << 6;
    public static final int OUTER_CORE_1 = 1 << 7;
    public static final int OUTER_CORE_2 = 1 << 8;
    public static final int OUTER_CORE_3 = 1 << 9;
    public static final int OUTER_CORE_4 = 1 << 10;
    public static final int OUTER_CORE_5 = 1 << 11;
    public static final int TEMPORAL_AUX = 1 << 12;
    public static final int ENERGY_AUX = 1 << 13;
    public static final int CENTRAL_CORE = 1 << 14;

    public static final int LEG_AXIS_MASK = LEG_AXIS_0 | LEG_AXIS_1 | LEG_AXIS_2 | LEG_AXIS_3;
    public static final int ARM_MASK = ARM_LEFT | ARM_RIGHT;
    public static final int OUTER_CORE_MASK = OUTER_CORE_0 | OUTER_CORE_1 | OUTER_CORE_2
            | OUTER_CORE_3 | OUTER_CORE_4 | OUTER_CORE_5;
    public static final int AUX_MASK = TEMPORAL_AUX | ENERGY_AUX;
    public static final int ALL_PARTS_MASK = 0x7FFF;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 45_000.0D;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(WorldbreakerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(WorldbreakerEntity.class, EntityDataSerializers.INT);

    private static final PartSpec[] SPECS = {
            new PartSpec(PartKind.LEG_AXIS, 0, LEG_AXIS_0, -25.0D, 18.0D, -20.0D, 16.0F, 24.0F, 1_500.0F),
            new PartSpec(PartKind.LEG_AXIS, 1, LEG_AXIS_1, 25.0D, 18.0D, -20.0D, 16.0F, 24.0F, 1_500.0F),
            new PartSpec(PartKind.LEG_AXIS, 2, LEG_AXIS_2, -25.0D, 18.0D, 20.0D, 16.0F, 24.0F, 1_500.0F),
            new PartSpec(PartKind.LEG_AXIS, 3, LEG_AXIS_3, 25.0D, 18.0D, 20.0D, 16.0F, 24.0F, 1_500.0F),
            new PartSpec(PartKind.ARM, 0, ARM_LEFT, -52.0D, 82.0D, 0.0D, 18.0F, 42.0F, 1_800.0F),
            new PartSpec(PartKind.ARM, 1, ARM_RIGHT, 52.0D, 82.0D, 0.0D, 18.0F, 42.0F, 1_800.0F),
            new PartSpec(PartKind.OUTER_CORE, 0, OUTER_CORE_0, -22.0D, 68.0D, -18.0D, 11.0F, 12.0F, 1_000.0F),
            new PartSpec(PartKind.OUTER_CORE, 1, OUTER_CORE_1, 22.0D, 68.0D, -18.0D, 11.0F, 12.0F, 1_000.0F),
            new PartSpec(PartKind.OUTER_CORE, 2, OUTER_CORE_2, -25.0D, 91.0D, -16.0D, 11.0F, 12.0F, 1_000.0F),
            new PartSpec(PartKind.OUTER_CORE, 3, OUTER_CORE_3, 25.0D, 91.0D, -16.0D, 11.0F, 12.0F, 1_000.0F),
            new PartSpec(PartKind.OUTER_CORE, 4, OUTER_CORE_4, -18.0D, 114.0D, -13.0D, 10.0F, 11.0F, 1_000.0F),
            new PartSpec(PartKind.OUTER_CORE, 5, OUTER_CORE_5, 18.0D, 114.0D, -13.0D, 10.0F, 11.0F, 1_000.0F),
            new PartSpec(PartKind.TEMPORAL_AUX, 0, TEMPORAL_AUX, -13.0D, 132.0D, 4.0D, 13.0F, 16.0F, 2_200.0F),
            new PartSpec(PartKind.ENERGY_AUX, 0, ENERGY_AUX, 13.0D, 132.0D, 4.0D, 13.0F, 16.0F, 2_200.0F),
            new PartSpec(PartKind.CENTRAL_CORE, 0, CENTRAL_CORE, 0.0D, 96.0D, -23.0D, 18.0F, 20.0F, 6_000.0F)
    };

    private final WorldbreakerPart[] parts = new WorldbreakerPart[SPECS.length];
    private final ServerBossEvent bossBar;
    private final List<DebrisStrike> debrisStrikes = new ArrayList<>();
    private boolean partsInitialized;
    private boolean rewardsGranted;
    private boolean marchDestinationInitialized;
    private double marchTargetX;
    private double marchTargetZ;
    private int actionCooldown = 46;
    private int terrainCooldown;
    private int parasiteCooldown = 180;

    public WorldbreakerEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.worldbreaker"),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new WorldbreakerPart(this, spec, spec.width(), spec.height(), spec.health());
        }
        xpReward = 480;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
        builder.define(PHASE, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new WorldbreakerCombatGoal());
    }

    @Override
    public int temporalRating() {
        return 88;
    }

    public int phase() { return getEntityData().get(PHASE); }
    public int brokenPartsMask() { return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK; }
    public boolean isPartBroken(int mask) { return (brokenPartsMask() & mask) != 0; }
    public int brokenLegAxisCount() { return Integer.bitCount(brokenPartsMask() & LEG_AXIS_MASK); }
    public int brokenArmCount() { return Integer.bitCount(brokenPartsMask() & ARM_MASK); }
    public int brokenOuterCoreCount() { return Integer.bitCount(brokenPartsMask() & OUTER_CORE_MASK); }
    public int brokenAuxCount() { return Integer.bitCount(brokenPartsMask() & AUX_MASK); }
    public boolean centralCoreExposed() { return phase() >= 3; }

    public void setMarchDestination(double x, double z) {
        marchTargetX = x;
        marchTargetZ = z;
        marchDestinationInitialized = true;
    }

    private void ensureMarchDestination() {
        if (marchDestinationInitialized) return;
        double yaw = Math.toRadians(getYRot());
        setMarchDestination(getX() - Math.sin(yaw) * 1_800.0D, getZ() + Math.cos(yaw) * 1_800.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float effective = amount * (phase() >= 3 ? 0.0035F : 0.0008F);
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

        ensureMarchDestination();
        updatePhase();
        tickDebris(serverLevel);
        if (actionCooldown > 0) actionCooldown--;
        if (terrainCooldown > 0) terrainCooldown--;
        if (parasiteCooldown > 0) parasiteCooldown--;

        if (phase() == 1 && terrainCooldown <= 0) {
            terrainCooldown = 5;
            terrainPiercingStep(serverLevel);
        }
        if (phase() >= 2 && parasiteCooldown <= 0) {
            parasiteCooldown = phase() >= 4 ? 80 : phase() == 3 ? 120 : 180;
            releaseParasites(serverLevel);
        }
    }

    private void updatePhase() {
        int next;
        if (brokenLegAxisCount() < 4) next = 1;
        else if (brokenOuterCoreCount() < 6 || brokenAuxCount() < 2) next = 2;
        else next = coreHealthFraction() <= 0.25D ? 4 : 3;
        getEntityData().set(PHASE, next);
        if (next >= 2) getNavigation().stop();
    }

    private double coreHealthFraction() {
        for (int i = 0; i < SPECS.length; i++) {
            if (SPECS[i].kind() == PartKind.CENTRAL_CORE) {
                return parts[i].partHealth / Math.max(1.0F, SPECS[i].health());
            }
        }
        return 1.0D;
    }

    private void terrainPiercingStep(ServerLevel level) {
        Vec3 toDestination = new Vec3(marchTargetX - getX(), 0.0D, marchTargetZ - getZ());
        if (toDestination.lengthSqr() <= 4.0D) return;
        Vec3 direction = toDestination.normalize();
        int broken = 0;
        for (int forward = 3; forward <= 11 && broken < 28; forward += 2) {
            Vec3 center = position().add(direction.scale(forward));
            for (int y = 0; y <= 10 && broken < 28; y += 2) {
                for (int side = -4; side <= 4 && broken < 28; side += 2) {
                    Vec3 lateral = new Vec3(-direction.z * side, y, direction.x * side);
                    BlockPos pos = BlockPos.containing(center.add(lateral));
                    BlockState state = level.getBlockState(pos);
                    if (BreachService.requiredPower(level, pos, state) > 5) continue;
                    if (level.destroyBlock(pos, false, this)) broken++;
                }
            }
        }
        if (broken > 0) {
            level.sendParticles(ParticleTypes.POOF, getX() + direction.x * 7.0D, getY() + 4.0D,
                    getZ() + direction.z * 7.0D, Math.min(60, broken * 2), 5.0D, 4.0D, 5.0D, 0.08D);
        }
    }

    private Player nearestCombatant(ServerLevel level, double range) {
        return level.getNearestPlayer(this, range);
    }

    private void seismicImpact(ServerLevel level) {
        swing(InteractionHand.MAIN_HAND);
        double radius = phase() >= 4 ? 48.0D : phase() >= 3 ? 40.0D : 34.0D;
        double damage = phase() >= 4 ? 120.0D : phase() >= 3 ? 88.0D : phase() == 2 ? 68.0D : 54.0D;
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 2.0D, getZ(), 100,
                radius * 0.35D, 3.0D, radius * 0.35D, 0.14D);
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), Player::isAlive)) {
            Vec3 away = player.position().subtract(position());
            if (away.horizontalDistanceSqr() > radius * radius) continue;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(damage));
            if (away.horizontalDistanceSqr() > 1.0E-6D) {
                Vec3 push = new Vec3(away.x, 0.0D, away.z).normalize();
                player.push(push.x * 1.30D, 0.48D, push.z * 1.30D);
            }
        }
    }

    private void energyBeam(ServerLevel level, Player target) {
        if (target == null) return;
        swing(InteractionHand.MAIN_HAND);
        Vec3 start = position().add(0.0D, 96.0D, 0.0D);
        Vec3 end = target.getEyePosition();
        Vec3 delta = end.subtract(start);
        double length = Math.max(1.0D, delta.length());
        Vec3 direction = delta.scale(1.0D / length);
        double width = phase() >= 4 ? 5.5D : phase() >= 3 ? 4.5D : 3.5D;
        double damage = phase() >= 4 ? 150.0D : phase() >= 3 ? 112.0D : 82.0D;
        int samples = Math.max(1, Math.min(30, (int) (length / 4.0D)));
        for (int i = 1; i <= samples; i++) {
            Vec3 point = start.add(direction.scale(length * i / samples));
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 3,
                    0.35D, 0.35D, 0.35D, 0.01D);
        }
        AABB area = new AABB(start, end).inflate(width);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            if (distanceToSegmentSqr(player.getEyePosition(), start, end) > width * width) continue;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(damage));
        }
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr <= 1.0E-8D) return point.distanceToSqr(start);
        double t = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(segment) / lengthSqr));
        return point.distanceToSqr(start.add(segment.scale(t)));
    }

    private void debrisStorm(ServerLevel level, Player target) {
        if (target == null) return;
        int count = phase() >= 4 ? 9 : phase() >= 3 ? 7 : 5;
        for (int i = 0; i < count; i++) {
            double angle = getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = getRandom().nextDouble() * (phase() >= 3 ? 18.0D : 13.0D);
            Vec3 center = target.position().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            debrisStrikes.add(new DebrisStrike(center, 20 + getRandom().nextInt(25),
                    phase() >= 4 ? 6.0D : 4.8D, phase() >= 4 ? 96.0D : 68.0D));
        }
    }

    private void tickDebris(ServerLevel level) {
        for (int i = debrisStrikes.size() - 1; i >= 0; i--) {
            DebrisStrike strike = debrisStrikes.get(i);
            if (--strike.ticks > 0) {
                if ((strike.ticks & 3) == 0) {
                    level.sendParticles(ParticleTypes.POOF, strike.center.x, strike.center.y + 10.0D, strike.center.z,
                            5, 1.2D, 3.0D, 1.2D, 0.02D);
                }
                continue;
            }
            level.sendParticles(ParticleTypes.EXPLOSION, strike.center.x, strike.center.y + 0.8D, strike.center.z,
                    3, 1.0D, 0.8D, 1.0D, 0.01D);
            AABB area = new AABB(strike.center, strike.center).inflate(strike.radius, 4.0D, strike.radius);
            for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
                if (player.position().distanceToSqr(strike.center) > strike.radius * strike.radius) continue;
                player.hurtServer(level, damageSources().mobAttack(this),
                        (float) CombatScale.toInternal(strike.visibleDamage));
            }
            debrisStrikes.remove(i);
        }
    }

    private void releaseParasites(ServerLevel level) {
        int count = phase() >= 4 ? 5 : phase() == 3 ? 4 : 3;
        Player target = nearestCombatant(level, 96.0D);
        for (int i = 0; i < count; i++) {
            Mob parasite = ModEntities.BURSTLING.get().create(level, EntitySpawnReason.EVENT);
            if (parasite == null) continue;
            double angle = Math.PI * 2.0D * i / count;
            parasite.setPos(getX() + Math.cos(angle) * 12.0D, getY() + 6.0D,
                    getZ() + Math.sin(angle) * 12.0D);
            if (target instanceof LivingEntity living) parasite.setTarget(living);
            level.addFreshEntity(parasite);
        }
    }

    private boolean partPickable(PartSpec spec) {
        if (isPartBroken(spec.mask())) return false;
        return switch (spec.kind()) {
            case LEG_AXIS -> phase() == 1;
            case ARM, OUTER_CORE, TEMPORAL_AUX, ENERGY_AUX -> phase() == 2;
            case CENTRAL_CORE -> phase() >= 3;
        };
    }

    private boolean hurtPart(WorldbreakerPart part, ServerLevel level, DamageSource source, float amount) {
        PartSpec spec = part.spec;
        if (!partPickable(spec)) return false;
        float effective = Math.max(0.0F, amount);
        part.applyPartDamage(effective);
        float transfer = switch (spec.kind()) {
            case LEG_AXIS -> effective * 0.40F;
            case ARM -> effective * 0.52F;
            case OUTER_CORE -> effective * 0.72F;
            case TEMPORAL_AUX, ENERGY_AUX -> effective * 0.85F;
            case CENTRAL_CORE -> effective;
        };
        setHealth(Math.max(1.0F, getHealth() - transfer));

        if (spec.kind() == PartKind.CENTRAL_CORE && !part.broken() && part.partHealth <= spec.health() * 0.25F) {
            getEntityData().set(PHASE, 4);
            actionCooldown = Math.min(actionCooldown, 8);
        }
        if (!part.broken()) return true;

        markBroken(spec.mask());
        if (spec.kind() == PartKind.CENTRAL_CORE) {
            return super.hurtServer(level, source, Float.MAX_VALUE);
        }
        if (spec.kind() == PartKind.ARM) actionCooldown += 10;
        if (spec.kind() == PartKind.TEMPORAL_AUX) actionCooldown += 8;
        if (spec.kind() == PartKind.ENERGY_AUX) actionCooldown += 12;
        updatePhase();
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
            WorldbreakerPart part = parts[i];
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
        if (!partsInitialized) return getBoundingBox().inflate(72.0D, 170.0D, 72.0D);
        AABB bounds = getBoundingBox();
        for (WorldbreakerPart part : parts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds.inflate(12.0D);
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
            WorldbreakerRewardService.award(this, level, source);
        }
        super.die(source);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int saved = input.getIntOr("TitanbreakWorldbreakerBrokenParts", 0) & ALL_PARTS_MASK;
        getEntityData().set(BROKEN_PARTS, saved);
        getEntityData().set(PHASE, input.getIntOr("TitanbreakWorldbreakerPhase", 1));
        marchDestinationInitialized = input.getBooleanOr("TitanbreakWorldbreakerMarchSet", false);
        marchTargetX = input.getDoubleOr("TitanbreakWorldbreakerMarchX", getX());
        marchTargetZ = input.getDoubleOr("TitanbreakWorldbreakerMarchZ", getZ());
        for (int i = 0; i < parts.length; i++) {
            float hp = input.getFloatOr("TitanbreakWorldbreakerPartHealth" + i, SPECS[i].health());
            if ((saved & SPECS[i].mask()) != 0) hp = 0.0F;
            parts[i].setPartHealth(hp);
        }
        debrisStrikes.clear();
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakWorldbreakerBrokenParts", brokenPartsMask());
        output.putInt("TitanbreakWorldbreakerPhase", phase());
        output.putBoolean("TitanbreakWorldbreakerMarchSet", marchDestinationInitialized);
        output.putDouble("TitanbreakWorldbreakerMarchX", marchTargetX);
        output.putDouble("TitanbreakWorldbreakerMarchZ", marchTargetZ);
        for (int i = 0; i < parts.length; i++) {
            output.putFloat("TitanbreakWorldbreakerPartHealth" + i, parts[i].partHealth);
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

    private final class WorldbreakerCombatGoal extends Goal {
        @Override
        public boolean canUse() { return isAlive(); }

        @Override
        public boolean canContinueToUse() { return isAlive(); }

        @Override
        public void tick() {
            if (!(level() instanceof ServerLevel serverLevel)) return;
            ensureMarchDestination();
            Player target = nearestCombatant(serverLevel, 120.0D);

            if (phase() == 1) {
                getNavigation().moveTo(marchTargetX, getY(), marchTargetZ, 0.46D);
                Vec3 direction = new Vec3(marchTargetX - getX(), 0.0D, marchTargetZ - getZ());
                if (direction.lengthSqr() > 1.0E-6D) {
                    setYRot((float) (Math.toDegrees(Math.atan2(-direction.x, direction.z))));
                }
            } else {
                getNavigation().stop();
                if (target != null) getLookControl().setLookAt(target, 40.0F, 25.0F);
            }

            if (actionCooldown > 0) return;
            int p = phase();
            if (p == 1) {
                seismicImpact(serverLevel);
                actionCooldown = 52;
            } else if (p == 2) {
                int choice = getRandom().nextInt(4);
                if (choice == 0) seismicImpact(serverLevel);
                else if (choice == 1) energyBeam(serverLevel, target);
                else debrisStorm(serverLevel, target);
                actionCooldown = 36 + getRandom().nextInt(18);
            } else if (p == 3) {
                int choice = getRandom().nextInt(5);
                if (choice == 0) seismicImpact(serverLevel);
                else if (choice <= 2) energyBeam(serverLevel, target);
                else debrisStorm(serverLevel, target);
                actionCooldown = 24 + getRandom().nextInt(12);
            } else {
                int choice = getRandom().nextInt(4);
                if (choice == 0) seismicImpact(serverLevel);
                else if (choice <= 2) energyBeam(serverLevel, target);
                else debrisStorm(serverLevel, target);
                actionCooldown = 10 + getRandom().nextInt(8);
            }
        }
    }

    private enum PartKind { LEG_AXIS, ARM, OUTER_CORE, TEMPORAL_AUX, ENERGY_AUX, CENTRAL_CORE }

    private record PartSpec(PartKind kind, int index, int mask, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class DebrisStrike {
        private final Vec3 center;
        private int ticks;
        private final double radius;
        private final double visibleDamage;

        private DebrisStrike(Vec3 center, int ticks, double radius, double visibleDamage) {
            this.center = center;
            this.ticks = ticks;
            this.radius = radius;
            this.visibleDamage = visibleDamage;
        }
    }

    private static final class WorldbreakerPart extends PartEntity<WorldbreakerEntity> {
        private final PartSpec spec;
        private final EntityDimensions dimensions;
        private float partHealth;

        private WorldbreakerPart(WorldbreakerEntity parent, PartSpec spec, float width, float height, float health) {
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

package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
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

public final class BastionWalkerEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int PART_LEG_NW = 1 << 0;
    public static final int PART_LEG_NE = 1 << 1;
    public static final int PART_LEG_SW = 1 << 2;
    public static final int PART_LEG_SE = 1 << 3;
    public static final int PART_PLATE_NORTH_LOWER = 1 << 4;
    public static final int PART_PLATE_NORTH_UPPER = 1 << 5;
    public static final int PART_PLATE_EAST_LOWER = 1 << 6;
    public static final int PART_PLATE_EAST_UPPER = 1 << 7;
    public static final int PART_PLATE_SOUTH_LOWER = 1 << 8;
    public static final int PART_PLATE_SOUTH_UPPER = 1 << 9;
    public static final int PART_PLATE_WEST_LOWER = 1 << 10;
    public static final int PART_PLATE_WEST_UPPER = 1 << 11;
    public static final int PART_UPPER_NODE = 1 << 12;
    public static final int PART_POWER_CORE = 1 << 13;
    public static final int ALL_PARTS_MASK = (1 << 14) - 1;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 26_000.0D;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(BastionWalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ARMOR_CLOSED =
            SynchedEntityData.defineId(BastionWalkerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int[] ARMOR_PLATE_MASKS = {
            PART_PLATE_NORTH_LOWER, PART_PLATE_NORTH_UPPER,
            PART_PLATE_EAST_LOWER, PART_PLATE_EAST_UPPER,
            PART_PLATE_SOUTH_LOWER, PART_PLATE_SOUTH_UPPER,
            PART_PLATE_WEST_LOWER, PART_PLATE_WEST_UPPER
    };

    private static final PartSpec[] SPECS = {
            new PartSpec(PartSlot.LEG_NW, -18.0D, 17.0D, -18.0D, 11.0F, 28.0F, 360.0F),
            new PartSpec(PartSlot.LEG_NE, 18.0D, 17.0D, -18.0D, 11.0F, 28.0F, 360.0F),
            new PartSpec(PartSlot.LEG_SW, -18.0D, 17.0D, 18.0D, 11.0F, 28.0F, 360.0F),
            new PartSpec(PartSlot.LEG_SE, 18.0D, 17.0D, 18.0D, 11.0F, 28.0F, 360.0F),
            new PartSpec(PartSlot.PLATE_NORTH_LOWER, 0.0D, 55.0D, -27.0D, 22.0F, 19.0F, 420.0F),
            new PartSpec(PartSlot.PLATE_NORTH_UPPER, 0.0D, 90.0D, -25.0D, 21.0F, 19.0F, 420.0F),
            new PartSpec(PartSlot.PLATE_EAST_LOWER, 27.0D, 55.0D, 0.0D, 22.0F, 19.0F, 420.0F),
            new PartSpec(PartSlot.PLATE_EAST_UPPER, 25.0D, 90.0D, 0.0D, 21.0F, 19.0F, 420.0F),
            new PartSpec(PartSlot.PLATE_SOUTH_LOWER, 0.0D, 55.0D, 27.0D, 22.0F, 19.0F, 420.0F),
            new PartSpec(PartSlot.PLATE_SOUTH_UPPER, 0.0D, 90.0D, 25.0D, 21.0F, 19.0F, 420.0F),
            new PartSpec(PartSlot.PLATE_WEST_LOWER, -27.0D, 55.0D, 0.0D, 22.0F, 19.0F, 420.0F),
            new PartSpec(PartSlot.PLATE_WEST_UPPER, -25.0D, 90.0D, 0.0D, 21.0F, 19.0F, 420.0F),
            new PartSpec(PartSlot.UPPER_NODE, 0.0D, 129.0D, 0.0D, 20.0F, 17.0F, 680.0F),
            new PartSpec(PartSlot.POWER_CORE, 0.0D, 116.0D, -4.0D, 18.0F, 18.0F, 980.0F)
    };

    private final BastionPart[] parts = new BastionPart[SPECS.length];
    private final ServerBossEvent bossBar;
    private boolean partsInitialized;
    private int actionCooldown = 60;
    private int closureCooldown = 180;
    private int armorClosureTicks;
    private int turretImpactDelay;
    private Vec3 turretImpact;
    private int parasiteCooldown;

    public BastionWalkerEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.bastion_walker"),
                BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new BastionPart(this, spec.slot(), spec.width(), spec.height(), spec.health());
        }
        xpReward = 180;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
        builder.define(ARMOR_CLOSED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new BastionCombatGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        return 8;
    }

    public double canonicalVisibleHealth() {
        return CANONICAL_VISIBLE_MAX_HEALTH * Math.max(0.0D, getHealth()) / Math.max(1.0D, getMaxHealth());
    }

    public int brokenPartsMask() {
        return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK;
    }

    public boolean isPartBroken(int mask) {
        return (brokenPartsMask() & mask) != 0;
    }

    public boolean armorClosed() {
        return getEntityData().get(ARMOR_CLOSED);
    }

    public int brokenArmorPlateCount() {
        int count = 0;
        for (int mask : ARMOR_PLATE_MASKS) if (isPartBroken(mask)) count++;
        return count;
    }

    public boolean upperNodeExposed() {
        return brokenArmorPlateCount() >= 6;
    }

    public boolean coreExposed() {
        return isPartBroken(PART_UPPER_NODE);
    }

    public int phase() {
        if (coreExposed()) return 3;
        if (upperNodeExposed()) return 2;
        return 1;
    }

    public boolean routeGateOpen(int route, double relativeY) {
        if (armorClosed()) return false;
        if (relativeY < 52.0D) return true;
        int lower = switch (route & 3) {
            case 0 -> PART_PLATE_NORTH_LOWER;
            case 1 -> PART_PLATE_EAST_LOWER;
            case 2 -> PART_PLATE_SOUTH_LOWER;
            default -> PART_PLATE_WEST_LOWER;
        };
        if (!isPartBroken(lower)) return false;
        if (relativeY < 86.0D) return true;
        int upper = switch (route & 3) {
            case 0 -> PART_PLATE_NORTH_UPPER;
            case 1 -> PART_PLATE_EAST_UPPER;
            case 2 -> PART_PLATE_SOUTH_UPPER;
            default -> PART_PLATE_WEST_UPPER;
        };
        return isPartBroken(upper);
    }

    private boolean isBroken(PartSlot slot) {
        return isPartBroken(slot.mask());
    }

    private int brokenLegCount() {
        int count = 0;
        for (PartSlot slot : new PartSlot[]{PartSlot.LEG_NW, PartSlot.LEG_NE, PartSlot.LEG_SW, PartSlot.LEG_SE}) {
            if (isBroken(slot)) count++;
        }
        return count;
    }

    private void markBroken(PartSlot slot) {
        int old = brokenPartsMask();
        int next = old | slot.mask();
        if (old != next) getEntityData().set(BROKEN_PARTS, next);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float effective = amount * (coreExposed() ? 0.10F : 0.02F);
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

        int brokenLegs = brokenLegCount();
        if (brokenLegs > 0) {
            Vec3 motion = getDeltaMovement();
            double factor = Math.max(0.12D, 1.0D - brokenLegs * 0.20D);
            setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
        }

        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (armorClosureTicks > 0) {
            armorClosureTicks--;
            if (armorClosureTicks % 10 == 0) shakeHull(serverLevel, 17.0D, 0.65D);
            if (armorClosureTicks == 0) getEntityData().set(ARMOR_CLOSED, false);
        }

        if (turretImpactDelay > 0 && --turretImpactDelay == 0 && turretImpact != null) {
            turretStrike(serverLevel, turretImpact);
            turretImpact = null;
        }

        if (parasiteCooldown > 0) parasiteCooldown--;

        if (phase() == 3 && tickCount % 28 == 0) {
            shakeHull(serverLevel, 20.0D, 0.45D);
        }
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
            BastionPart part = parts[i];
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
        if (!partsInitialized) return getBoundingBox().inflate(48.0D, 175.0D, 48.0D);
        AABB bounds = getBoundingBox();
        for (BastionPart part : parts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds.inflate(8.0D);
    }

    private boolean partPickable(PartSlot slot) {
        if (slot == PartSlot.UPPER_NODE) return upperNodeExposed() && !isBroken(slot);
        if (slot == PartSlot.POWER_CORE) return coreExposed() && !isBroken(slot);
        return !isBroken(slot);
    }

    private boolean hurtPart(BastionPart part, ServerLevel level, DamageSource source, float amount) {
        if (!partPickable(part.slot)) return false;

        float effective = amount;
        if (armorClosed() && part.slot != PartSlot.LEG_NW && part.slot != PartSlot.LEG_NE
                && part.slot != PartSlot.LEG_SW && part.slot != PartSlot.LEG_SE) {
            effective *= 0.18F;
        }

        boolean wasBroken = part.broken();
        part.applyPartDamage(effective);
        if (!wasBroken && part.broken()) markBroken(part.slot);

        float healthTransfer = switch (part.slot) {
            case LEG_NW, LEG_NE, LEG_SW, LEG_SE -> effective * 0.70F;
            case PLATE_NORTH_LOWER, PLATE_NORTH_UPPER, PLATE_EAST_LOWER, PLATE_EAST_UPPER,
                    PLATE_SOUTH_LOWER, PLATE_SOUTH_UPPER, PLATE_WEST_LOWER, PLATE_WEST_UPPER -> effective;
            case UPPER_NODE -> effective * 0.90F;
            case POWER_CORE -> effective;
        };

        if (part.slot == PartSlot.POWER_CORE && part.broken()) {
            return super.hurtServer(level, source, Float.MAX_VALUE);
        }

        float before = getHealth();
        setHealth(Math.max(1.0F, before - Math.max(0.0F, healthTransfer)));
        return effective > 0.0F;
    }

    private void scheduleTurretShot(LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        turretImpact = target.position().add(target.getDeltaMovement().scale(14.0D));
        turretImpactDelay = phase() == 3 ? 11 : 17;
    }

    private void turretStrike(ServerLevel level, Vec3 impact) {
        double radius = phase() == 3 ? 6.0D : 4.5D;
        double visibleDamage = phase() == 3 ? 54.0D : 42.0D;
        AABB area = new AABB(impact, impact).inflate(radius, radius, radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            double distance = player.position().distanceTo(impact);
            if (distance > radius) continue;
            double scale = Math.max(0.35D, 1.0D - distance / radius);
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(visibleDamage * scale));
            player.push(0.0D, 0.25D + scale * 0.35D, 0.0D);
        }
    }

    private void legSweep(ServerLevel level) {
        swing(InteractionHand.MAIN_HAND);
        double radius = 38.0D;
        AABB area = getBoundingBox().inflate(radius, 36.0D, radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            double relativeY = player.getY() - getY();
            double horizontal = player.position().subtract(position()).horizontalDistance();
            if (relativeY > 36.0D || horizontal > radius) continue;
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(64.0D));
            Vec3 push = player.position().subtract(position());
            if (push.horizontalDistanceSqr() > 1.0E-6D) {
                push = new Vec3(push.x, 0.0D, push.z).normalize();
                player.push(push.x * 2.4D, 0.55D, push.z * 2.4D);
            }
        }
    }

    private void shakeHull(ServerLevel level, double visibleDamage, double force) {
        double radius = 43.0D;
        AABB area = getBoundingBox().inflate(radius, 150.0D, radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            double relativeY = player.getY() - getY();
            Vec3 offset = player.position().subtract(position());
            double horizontal = offset.horizontalDistance();
            if (relativeY < 8.0D || relativeY > 145.0D || horizontal < 16.0D || horizontal > radius) continue;

            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(visibleDamage));
            if (offset.horizontalDistanceSqr() > 1.0E-6D) {
                Vec3 outward = new Vec3(offset.x, 0.0D, offset.z).normalize();
                player.push(outward.x * force, 0.16D + force * 0.18D, outward.z * force);
            }
        }
    }

    private void startArmorClosure(ServerLevel level) {
        getEntityData().set(ARMOR_CLOSED, true);
        armorClosureTicks = 40;
        shakeHull(level, 14.0D, 0.90D);
    }

    private void releaseParasites(ServerLevel level, LivingEntity target) {
        if (parasiteCooldown > 0) return;
        parasiteCooldown = phase() == 3 ? 140 : 210;
        for (int i = 0; i < 3; i++) spawnParasite(level, target, ModEntities.SKITTER.get(), i, false);
        if (phase() >= 2) spawnParasite(level, target, ModEntities.GLIDER.get(), 3, true);
    }

    private void spawnParasite(ServerLevel level, LivingEntity target, EntityType<?> type, int index, boolean airborne) {
        double angle = (Math.PI * 2.0D * index / 4.0D) + getRandom().nextDouble() * 0.45D;
        double radius = 28.0D + getRandom().nextDouble() * 8.0D;
        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (!(entity instanceof Mob mob)) return;
        mob.setPos(getX() + Math.cos(angle) * radius, getY() + (airborne ? 12.0D : 2.0D),
                getZ() + Math.sin(angle) * radius);
        mob.setYRot(getRandom().nextFloat() * 360.0F);
        mob.setTarget(target);
        level.addFreshEntity(mob);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int saved = input.getIntOr("TitanbreakBastionBrokenParts", 0) & ALL_PARTS_MASK;
        int rebuilt = 0;
        for (int i = 0; i < parts.length; i++) {
            BastionPart part = parts[i];
            float hp = input.getFloatOr("TitanbreakBastionPartHealth" + i, SPECS[i].health());
            if ((saved & part.slot.mask()) != 0) hp = 0.0F;
            part.setPartHealth(hp);
            if (part.broken()) rebuilt |= part.slot.mask();
        }
        getEntityData().set(BROKEN_PARTS, rebuilt);
        getEntityData().set(ARMOR_CLOSED, false);
        armorClosureTicks = 0;
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakBastionBrokenParts", brokenPartsMask());
        for (int i = 0; i < parts.length; i++) {
            output.putFloat("TitanbreakBastionPartHealth" + i, parts[i].partHealth);
        }
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

    private final class BastionCombatGoal extends Goal {
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
            if (target == null || !(level() instanceof ServerLevel level)) return;

            if (actionCooldown > 0) actionCooldown--;
            if (closureCooldown > 0) closureCooldown--;

            int currentPhase = phase();
            double speed = currentPhase == 1 ? 0.44D : currentPhase == 2 ? 0.34D : 0.24D;
            speed *= Math.max(0.22D, 1.0D - brokenLegCount() * 0.18D);
            getNavigation().moveTo(target, speed);
            getLookControl().setLookAt(target, 24.0F, 16.0F);

            if (currentPhase <= 2 && closureCooldown <= 0 && !armorClosed()) {
                startArmorClosure(level);
                closureCooldown = 180 + getRandom().nextInt(90);
                actionCooldown = Math.max(actionCooldown, 45);
                return;
            }

            if (actionCooldown > 0 || armorClosed() || turretImpactDelay > 0) return;

            double distance = distanceTo(target);
            int choice = getRandom().nextInt(currentPhase == 3 ? 5 : 6);

            if (distance <= 36.0D && target.getY() - getY() < 38.0D && choice <= 1) {
                legSweep(level);
                actionCooldown = currentPhase == 3 ? 48 : 66;
            } else if (choice == 2 || distance > 50.0D) {
                scheduleTurretShot(target);
                actionCooldown = currentPhase == 3 ? 30 : 46;
            } else if (choice == 3) {
                shakeHull(level, currentPhase == 3 ? 32.0D : 24.0D, currentPhase == 3 ? 1.10D : 0.86D);
                actionCooldown = currentPhase == 3 ? 44 : 60;
            } else if (choice == 4 && parasiteCooldown <= 0) {
                releaseParasites(level, target);
                actionCooldown = 72;
            } else {
                scheduleTurretShot(target);
                actionCooldown = 42;
            }
        }
    }

    private enum PartSlot {
        LEG_NW(PART_LEG_NW), LEG_NE(PART_LEG_NE), LEG_SW(PART_LEG_SW), LEG_SE(PART_LEG_SE),
        PLATE_NORTH_LOWER(PART_PLATE_NORTH_LOWER), PLATE_NORTH_UPPER(PART_PLATE_NORTH_UPPER),
        PLATE_EAST_LOWER(PART_PLATE_EAST_LOWER), PLATE_EAST_UPPER(PART_PLATE_EAST_UPPER),
        PLATE_SOUTH_LOWER(PART_PLATE_SOUTH_LOWER), PLATE_SOUTH_UPPER(PART_PLATE_SOUTH_UPPER),
        PLATE_WEST_LOWER(PART_PLATE_WEST_LOWER), PLATE_WEST_UPPER(PART_PLATE_WEST_UPPER),
        UPPER_NODE(PART_UPPER_NODE), POWER_CORE(PART_POWER_CORE);

        private final int mask;
        PartSlot(int mask) { this.mask = mask; }
        int mask() { return mask; }
    }

    private record PartSpec(PartSlot slot, double x, double y, double z, float width, float height, float health) {}

    private static final class BastionPart extends PartEntity<BastionWalkerEntity> {
        private final PartSlot slot;
        private final EntityDimensions dimensions;
        private float partHealth;

        private BastionPart(BastionWalkerEntity parent, PartSlot slot, float width, float height, float health) {
            super(parent);
            this.slot = slot;
            this.dimensions = EntityDimensions.scalable(width, height);
            this.partHealth = health;
            refreshDimensions();
        }

        private boolean broken() {
            return partHealth <= 0.0F;
        }

        private void setPartHealth(float health) {
            partHealth = Math.max(0.0F, health);
        }

        private void applyPartDamage(float amount) {
            setPartHealth(partHealth - Math.max(0.0F, amount));
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {}

        @Override
        protected void readAdditionalSaveData(ValueInput input) {}

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {}

        @Override
        public boolean isPickable() {
            return getParent().partPickable(slot);
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            return !isInvulnerableToBase(source) && getParent().hurtPart(this, level, source, amount);
        }

        @Override
        public boolean is(Entity entity) {
            return this == entity || getParent() == entity;
        }

        @Override
        public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return dimensions;
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }
    }
}

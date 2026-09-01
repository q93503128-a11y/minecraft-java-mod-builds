package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.BreachService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.core.BlockPos;
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

public final class GravemarchColossusEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int PART_LEFT_ANKLE = 1 << 0;
    public static final int PART_RIGHT_ANKLE = 1 << 1;
    public static final int PART_LEFT_KNEE = 1 << 2;
    public static final int PART_RIGHT_KNEE = 1 << 3;
    public static final int PART_LEFT_ELBOW = 1 << 4;
    public static final int PART_RIGHT_ELBOW = 1 << 5;
    public static final int PART_CHEST_HEART = 1 << 6;
    public static final int PART_SKULL_ARMOR = 1 << 7;
    public static final int ALL_PARTS_MASK = 0xFF;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 18_000.0D;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(GravemarchColossusEntity.class, EntityDataSerializers.INT);

    private static final PartSpec[] SPECS = {
            new PartSpec(PartSlot.LEFT_ANKLE, -10.0D, 9.0D, 0.0D, 10.0F, 13.0F, 180.0F),
            new PartSpec(PartSlot.RIGHT_ANKLE, 10.0D, 9.0D, 0.0D, 10.0F, 13.0F, 180.0F),
            new PartSpec(PartSlot.LEFT_KNEE, -10.0D, 30.0D, 0.0D, 12.0F, 15.0F, 240.0F),
            new PartSpec(PartSlot.RIGHT_KNEE, 10.0D, 30.0D, 0.0D, 12.0F, 15.0F, 240.0F),
            new PartSpec(PartSlot.LEFT_ELBOW, -24.0D, 58.0D, -1.0D, 11.0F, 14.0F, 220.0F),
            new PartSpec(PartSlot.RIGHT_ELBOW, 24.0D, 58.0D, -1.0D, 11.0F, 14.0F, 220.0F),
            new PartSpec(PartSlot.CHEST_HEART, 0.0D, 69.0D, -7.0D, 17.0F, 18.0F, 420.0F),
            new PartSpec(PartSlot.SKULL_ARMOR, 0.0D, 91.0D, -2.0D, 18.0F, 16.0F, 500.0F)
    };

    private final GravemarchPart[] parts = new GravemarchPart[SPECS.length];
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.titanbreak.gravemarch_colossus"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private boolean partsInitialized;
    private int actionCooldown = 50;
    private int shockwaveBursts;
    private int shockwaveDelay;
    private int debrisImpactDelay;
    private Vec3 debrisImpact;

    public GravemarchColossusEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new GravemarchPart(this, spec.slot(), spec.width(), spec.height(), spec.health());
        }
        xpReward = 120;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new GravemarchCombatGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        return 10;
    }

    public double canonicalVisibleHealth() {
        return CANONICAL_VISIBLE_MAX_HEALTH * Math.max(0.0D, getHealth()) / Math.max(1.0D, getMaxHealth());
    }

    public int phase() {
        if (isBroken(PartSlot.CHEST_HEART) || getHealth() <= getMaxHealth() * 0.30F) return 3;
        if (brokenLegPartCount() > 0) return 2;
        return 1;
    }

    public int brokenPartsMask() {
        return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK;
    }

    public boolean isPartBroken(int mask) {
        return (brokenPartsMask() & mask) != 0;
    }

    public boolean chestExposed() {
        return brokenLegPartCount() >= 2;
    }

    private boolean isBroken(PartSlot slot) {
        return isPartBroken(slot.mask());
    }

    private int brokenLegPartCount() {
        int count = 0;
        for (PartSlot slot : new PartSlot[]{PartSlot.LEFT_ANKLE, PartSlot.RIGHT_ANKLE,
                PartSlot.LEFT_KNEE, PartSlot.RIGHT_KNEE}) {
            if (isBroken(slot)) count++;
        }
        return count;
    }

    private int brokenPartCount() {
        return Integer.bitCount(brokenPartsMask());
    }

    private void markBroken(PartSlot slot) {
        int current = brokenPartsMask();
        int updated = current | slot.mask();
        if (updated != current) getEntityData().set(BROKEN_PARTS, updated);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        updatePartPositions();

        if (!level().isClientSide()) {
            bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / Math.max(1.0F, getMaxHealth()))));
        }

        int legDamage = brokenLegPartCount();
        if (legDamage > 0) {
            Vec3 motion = getDeltaMovement();
            double factor = legDamage >= 3 ? 0.12D : legDamage == 2 ? 0.22D : 0.38D;
            setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
        }

        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (debrisImpactDelay > 0 && --debrisImpactDelay == 0 && debrisImpact != null) {
            impactArea(serverLevel, debrisImpact, 7.0D + rageRadiusBonus(), 58.0D * rageDamageMultiplier(), 1.05D);
            fractureTerrain(serverLevel, BlockPos.containing(debrisImpact), 3, 18);
            debrisImpact = null;
        }
        if (shockwaveDelay > 0 && --shockwaveDelay == 0 && shockwaveBursts > 0) {
            int sequence = 4 - shockwaveBursts;
            double radius = 14.0D + sequence * 5.0D + rageRadiusBonus();
            impactArea(serverLevel, position(), radius, (46.0D + sequence * 10.0D) * rageDamageMultiplier(), 1.30D);
            fractureTerrain(serverLevel, blockPosition(), 3, 16);
            shockwaveBursts--;
            if (shockwaveBursts > 0) shockwaveDelay = 10;
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

    private float rageDamageMultiplier() {
        return (float) (1.0D + brokenPartCount() * 0.08D);
    }

    private double rageRadiusBonus() {
        return brokenPartCount() * 0.65D;
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
            GravemarchPart part = parts[i];
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
        if (!partsInitialized) return getBoundingBox().inflate(30.0D, 105.0D, 30.0D);
        AABB bounds = getBoundingBox();
        for (GravemarchPart part : parts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds;
    }

    private boolean hurtPart(GravemarchPart part, ServerLevel level, DamageSource source, float amount) {
        if (part.broken() && part.slot != PartSlot.CHEST_HEART) return false;

        if (part.slot == PartSlot.CHEST_HEART && part.broken()) {
            return super.hurtServer(level, source, amount);
        }

        float partDamage = amount;
        if (part.slot == PartSlot.CHEST_HEART && !chestExposed()) partDamage *= 0.12F;
        part.applyPartDamage(partDamage);
        if (part.broken()) markBroken(part.slot);

        float transferred = switch (part.slot) {
            case LEFT_ANKLE, RIGHT_ANKLE, LEFT_KNEE, RIGHT_KNEE -> amount * 0.20F;
            case LEFT_ELBOW, RIGHT_ELBOW -> amount * 0.16F;
            case SKULL_ARMOR -> amount * 0.08F;
            case CHEST_HEART -> amount * (chestExposed() ? 1.10F : 0.04F);
        };
        return super.hurtServer(level, source, transferred);
    }

    private void groundSlam(ServerLevel level) {
        swing(InteractionHand.MAIN_HAND);
        double radius = 14.0D + rageRadiusBonus();
        impactArea(level, position(), radius, 68.0D * rageDamageMultiplier(), 1.20D);
        fractureTerrain(level, blockPosition(), 3, 20);
    }

    private void mountainPush(ServerLevel level, LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        Vec3 facing = target.position().subtract(position());
        if (facing.horizontalDistanceSqr() < 1.0E-6D) return;
        facing = new Vec3(facing.x, 0.0D, facing.z).normalize();
        double range = 27.0D + rageRadiusBonus();
        AABB area = getBoundingBox().inflate(range, 12.0D, range);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            Vec3 offset = player.position().subtract(position());
            double horizontal = offset.horizontalDistance();
            if (horizontal > range || horizontal < 1.0E-5D) continue;
            Vec3 direction = new Vec3(offset.x, 0.0D, offset.z).normalize();
            if (direction.dot(facing) < 0.25D) continue;
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(54.0D * rageDamageMultiplier()));
            player.push(direction.x * 2.2D, 0.45D, direction.z * 2.2D);
        }
        fractureTerrain(level, blockPosition().offset((int) Math.round(facing.x * 5.0D), 0,
                (int) Math.round(facing.z * 5.0D)), 3, 18);
    }

    private void grabThrow(ServerLevel level, LivingEntity target) {
        if (isBroken(PartSlot.LEFT_ELBOW) && isBroken(PartSlot.RIGHT_ELBOW)) {
            groundSlam(level);
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        target.hurtServer(level, damageSources().mobAttack(this),
                (float) CombatScale.toInternal(82.0D * rageDamageMultiplier()));
        Vec3 away = target.position().subtract(position());
        if (away.horizontalDistanceSqr() < 1.0E-6D) away = getLookAngle();
        away = new Vec3(away.x, 0.0D, away.z).normalize();
        target.setDeltaMovement(away.x * 2.4D, 1.35D, away.z * 2.4D);
    }

    private void scheduleDebrisThrow(LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        debrisImpact = target.position().add(target.getDeltaMovement().scale(18.0D));
        debrisImpactDelay = 26;
    }

    private void startOverloadShockwaves() {
        shockwaveBursts = 3;
        shockwaveDelay = 1;
    }

    private void impactArea(ServerLevel level, Vec3 center, double radius, double visibleDamage, double knockback) {
        AABB area = new AABB(center, center).inflate(radius, Math.max(8.0D, radius * 0.55D), radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            double distance = player.position().distanceTo(center);
            if (distance > radius) continue;
            double scale = Math.max(0.20D, 1.0D - distance / Math.max(1.0D, radius));
            player.hurtServer(level, damageSources().mobAttack(this),
                    (float) CombatScale.toInternal(visibleDamage * (0.45D + scale * 0.55D)));
            Vec3 push = player.position().subtract(center);
            if (push.horizontalDistanceSqr() > 1.0E-6D) {
                push = new Vec3(push.x, 0.0D, push.z).normalize();
                player.push(push.x * knockback * (0.6D + scale), 0.28D + scale * 0.35D,
                        push.z * knockback * (0.6D + scale));
            }
        }
    }

    private void fractureTerrain(ServerLevel level, BlockPos center, int breachPower, int maxBlocks) {
        int broken = 0;
        int radius = 4;
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 1, radius))) {
            if (broken >= Math.min(24, maxBlocks)) break;
            BlockPos pos = cursor.immutable();
            if (pos.distSqr(center) > radius * radius + 2.0D) continue;
            var state = level.getBlockState(pos);
            if (BreachService.requiredPower(level, pos, state) > breachPower) continue;
            if (level.destroyBlock(pos, false, this)) broken++;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int savedMask = input.getIntOr("TitanbreakGravemarchBrokenParts", 0) & ALL_PARTS_MASK;
        int rebuiltMask = 0;
        for (int i = 0; i < parts.length; i++) {
            GravemarchPart part = parts[i];
            float savedHealth = input.getFloatOr("TitanbreakGravemarchPartHealth" + i, SPECS[i].health());
            if ((savedMask & part.slot.mask()) != 0) savedHealth = 0.0F;
            part.setPartHealth(savedHealth);
            if (part.broken()) rebuiltMask |= part.slot.mask();
        }
        getEntityData().set(BROKEN_PARTS, rebuiltMask);
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakGravemarchBrokenParts", brokenPartsMask());
        for (int i = 0; i < parts.length; i++) output.putFloat("TitanbreakGravemarchPartHealth" + i, parts[i].partHealth);
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

    private final class GravemarchCombatGoal extends Goal {
        private int actionClock;

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
            int phase = phase();
            if (actionCooldown > 0) actionCooldown--;
            actionClock++;

            double moveSpeed = phase == 1 ? 0.72D : phase == 2 ? 0.40D : 0.34D;
            if (brokenLegPartCount() >= 2) moveSpeed *= 0.55D;
            getNavigation().moveTo(target, moveSpeed);
            getLookControl().setLookAt(target, 38.0F, 28.0F);

            if (phase == 3 && shockwaveBursts == 0 && shockwaveDelay == 0 && actionClock % 74 == 0) {
                startOverloadShockwaves();
                actionCooldown = Math.max(actionCooldown, 30);
                return;
            }
            if (actionCooldown > 0 || debrisImpactDelay > 0) return;

            double distance = distanceTo(target);
            int choice = getRandom().nextInt(phase == 1 ? 4 : 5);
            if (distance <= 10.0D && choice == 0) {
                grabThrow(serverLevel, target);
                actionCooldown = 52;
            } else if (distance <= 18.0D && choice <= 2) {
                groundSlam(serverLevel);
                actionCooldown = phase == 1 ? 62 : 48;
            } else if (distance <= 30.0D && choice == 3) {
                mountainPush(serverLevel, target);
                actionCooldown = 58;
            } else if (distance <= 52.0D) {
                scheduleDebrisThrow(target);
                actionCooldown = 64;
            }
        }
    }

    private enum PartSlot {
        LEFT_ANKLE(PART_LEFT_ANKLE), RIGHT_ANKLE(PART_RIGHT_ANKLE),
        LEFT_KNEE(PART_LEFT_KNEE), RIGHT_KNEE(PART_RIGHT_KNEE),
        LEFT_ELBOW(PART_LEFT_ELBOW), RIGHT_ELBOW(PART_RIGHT_ELBOW),
        CHEST_HEART(PART_CHEST_HEART), SKULL_ARMOR(PART_SKULL_ARMOR);

        private final int mask;

        PartSlot(int mask) {
            this.mask = mask;
        }

        int mask() {
            return mask;
        }
    }

    private record PartSpec(PartSlot slot, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class GravemarchPart extends PartEntity<GravemarchColossusEntity> {
        private final PartSlot slot;
        private final EntityDimensions dimensions;
        private float partHealth;

        private GravemarchPart(GravemarchColossusEntity parent, PartSlot slot, float width, float height, float health) {
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
            return !broken() || slot == PartSlot.CHEST_HEART;
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
        public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
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

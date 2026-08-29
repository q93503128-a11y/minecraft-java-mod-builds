package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public final class PursuerEntity extends Giant implements TemporalRated {
    private static final PartSpec[] SPECS = {
            new PartSpec(PartSlot.LEFT_EYE, -2.4D, 29.0D, -1.8D, 2.4F, 2.4F, 85.0F),
            new PartSpec(PartSlot.RIGHT_EYE, 2.4D, 29.0D, -1.8D, 2.4F, 2.4F, 85.0F),
            new PartSpec(PartSlot.LEFT_FORE_UPPER, -5.2D, 10.5D, -1.0D, 3.2F, 6.5F, 150.0F),
            new PartSpec(PartSlot.LEFT_FORE_LOWER, -5.2D, 3.5D, -1.0D, 3.0F, 6.2F, 150.0F),
            new PartSpec(PartSlot.RIGHT_FORE_UPPER, 5.2D, 10.5D, -1.0D, 3.2F, 6.5F, 150.0F),
            new PartSpec(PartSlot.RIGHT_FORE_LOWER, 5.2D, 3.5D, -1.0D, 3.0F, 6.2F, 150.0F),
            new PartSpec(PartSlot.SPINE_REACTION, 0.0D, 20.0D, 2.8D, 4.8F, 7.0F, 280.0F),
            new PartSpec(PartSlot.CHEST_CORE, 0.0D, 15.0D, -1.2D, 6.4F, 7.5F, 420.0F)
    };

    private final PursuerPart[] parts = new PursuerPart[SPECS.length];
    private boolean partsInitialized;

    public PursuerEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new PursuerPart(this, spec.slot(), spec.width(), spec.height(), spec.health());
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PursuitGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        if (isBroken(PartSlot.SPINE_REACTION)) return 48;
        return phase() >= 3 ? 88 : 78;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        updatePartPositions();
        int brokenJoints = brokenJointCount();
        if (brokenJoints > 0) {
            Vec3 motion = getDeltaMovement();
            double factor = brokenJoints >= 4 ? 0.22D : Math.max(0.42D, 1.0D - brokenJoints * 0.16D);
            setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
        }
    }

    public int phase() {
        double ratio = getHealth() / Math.max(1.0D, getMaxHealth());
        if (ratio <= 0.30D) return 3;
        if (ratio <= 0.65D) return 2;
        return 1;
    }

    private int brokenJointCount() {
        int count = 0;
        for (PartSlot slot : new PartSlot[]{PartSlot.LEFT_FORE_UPPER, PartSlot.LEFT_FORE_LOWER,
                PartSlot.RIGHT_FORE_UPPER, PartSlot.RIGHT_FORE_LOWER}) {
            if (isBroken(slot)) count++;
        }
        return count;
    }

    private boolean isBroken(PartSlot slot) {
        for (PursuerPart part : parts) if (part.slot == slot) return part.broken();
        return false;
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
            PursuerPart part = parts[i];
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

    private boolean hurtPart(PursuerPart part, ServerLevel level, DamageSource source, float amount) {
        if (part.broken()) return false;
        part.applyPartDamage(amount);
        float transferred = switch (part.slot) {
            case CHEST_CORE -> amount * 1.55F;
            case SPINE_REACTION -> amount * 0.70F;
            case LEFT_EYE, RIGHT_EYE -> amount * 0.60F;
            case LEFT_FORE_UPPER, LEFT_FORE_LOWER, RIGHT_FORE_UPPER, RIGHT_FORE_LOWER -> amount * 0.34F;
        };
        return super.hurtServer(level, source, transferred);
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

    private final class PursuitGoal extends Goal {
        private int chargeClock;
        private int attackCooldown;

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
            if (target == null) return;
            int phase = phase();
            if (attackCooldown > 0) attackCooldown--;
            chargeClock++;

            double leadTicks = phase == 1 ? 5.0D : phase == 2 ? 9.0D : 13.0D;
            Vec3 predicted = target.position().add(target.getDeltaMovement().scale(leadTicks));
            double speed = phase == 1 ? 1.0D : phase == 2 ? 1.28D : 1.52D;
            getNavigation().moveTo(predicted.x, predicted.y, predicted.z, speed);
            getLookControl().setLookAt(target, 55.0F, 55.0F);

            int chargePeriod = phase == 1 ? 52 : phase == 2 ? 36 : 24;
            if (chargeClock >= chargePeriod) {
                chargeClock = 0;
                Vec3 direction = predicted.subtract(position());
                if (direction.horizontalDistanceSqr() > 1.0E-5D) {
                    direction = direction.normalize();
                    double force = phase == 1 ? 1.35D : phase == 2 ? 1.85D : 2.35D;
                    setDeltaMovement(getDeltaMovement().add(direction.x * force, 0.12D, direction.z * force));
                }
            }

            if (distanceToSqr(target) <= 9.0D * 9.0D && attackCooldown <= 0 && level() instanceof ServerLevel serverLevel) {
                float visibleDamage = phase == 1 ? 46.0F : phase == 2 ? 58.0F : 72.0F;
                target.hurtServer(serverLevel, damageSources().mobAttack(PursuerEntity.this),
                        (float) CombatScale.toInternal(visibleDamage));
                attackCooldown = phase == 3 ? 15 : 22;
            }
        }
    }

    private enum PartSlot {
        LEFT_EYE,
        RIGHT_EYE,
        LEFT_FORE_UPPER,
        LEFT_FORE_LOWER,
        RIGHT_FORE_UPPER,
        RIGHT_FORE_LOWER,
        SPINE_REACTION,
        CHEST_CORE
    }

    private record PartSpec(PartSlot slot, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class PursuerPart extends PartEntity<PursuerEntity> {
        private final PartSlot slot;
        private final EntityDimensions dimensions;
        private float partHealth;

        private PursuerPart(PursuerEntity parent, PartSlot slot, float width, float height, float health) {
            super(parent);
            this.slot = slot;
            this.dimensions = EntityDimensions.scalable(width, height);
            this.partHealth = health;
            refreshDimensions();
        }

        private boolean broken() {
            return partHealth <= 0.0F;
        }

        private void applyPartDamage(float amount) {
            partHealth = Math.max(0.0F, partHealth - Math.max(0.0F, amount));
        }

        @Override
        protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

        @Override
        protected void readAdditionalSaveData(ValueInput input) {}

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {}

        @Override
        public boolean isPickable() {
            return !broken();
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

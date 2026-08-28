package kr.moonseungjun.titanbreak.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public final class HollowColossusEntity extends Giant {
    private static final PartSpec[] SPECS = {
            new PartSpec(PartSlot.HEAD, 0.0, 9.0, 0.0, 3.2F, 3.0F, 40.0F),
            new PartSpec(PartSlot.CORE, 0.0, 5.1, 0.0, 3.6F, 4.2F, 80.0F),
            new PartSpec(PartSlot.LEFT_ARM, -2.65, 4.8, 0.0, 1.7F, 4.8F, 50.0F),
            new PartSpec(PartSlot.RIGHT_ARM, 2.65, 4.8, 0.0, 1.7F, 4.8F, 50.0F),
            new PartSpec(PartSlot.LEFT_LEG, -0.95, 0.0, 0.0, 1.55F, 5.3F, 55.0F),
            new PartSpec(PartSlot.RIGHT_LEG, 0.95, 0.0, 0.0, 1.55F, 5.3F, 55.0F)
    };

    private final ColossusPart[] parts = new ColossusPart[SPECS.length];
    private boolean partsInitialized;

    public HollowColossusEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new ColossusPart(this, spec.slot(), spec.width(), spec.height(), spec.health());
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D, 40));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        updatePartPositions();

        boolean leftLegBroken = isBroken(PartSlot.LEFT_LEG);
        boolean rightLegBroken = isBroken(PartSlot.RIGHT_LEG);
        if (leftLegBroken || rightLegBroken) {
            Vec3 motion = getDeltaMovement();
            double factor = leftLegBroken && rightLegBroken ? 0.05 : 0.35;
            setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
            if (leftLegBroken && rightLegBroken) getNavigation().stop();
        }
    }

    private void updatePartPositions() {
        Vec3[] previousPositions = new Vec3[parts.length];
        for (int i = 0; i < parts.length; i++) {
            previousPositions[i] = parts[i].position();
        }

        double yaw = Math.toRadians(-getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        for (int i = 0; i < parts.length; i++) {
            PartSpec spec = SPECS[i];
            ColossusPart part = parts[i];
            double offsetX = spec.x() * cos - spec.z() * sin;
            double offsetZ = spec.x() * sin + spec.z() * cos;
            part.setPos(getX() + offsetX, getY() + spec.y(), getZ() + offsetZ);
        }

        for (int i = 0; i < parts.length; i++) {
            ColossusPart part = parts[i];
            Vec3 previous = partsInitialized ? previousPositions[i] : part.position();
            part.xo = previous.x;
            part.yo = previous.y;
            part.zo = previous.z;
            part.xOld = previous.x;
            part.yOld = previous.y;
            part.zOld = previous.z;
        }
        partsInitialized = true;
    }

    private boolean isBroken(PartSlot slot) {
        for (ColossusPart part : parts) {
            if (part.slot == slot) return part.broken();
        }
        return false;
    }

    private boolean hurtPart(ColossusPart part, ServerLevel level, DamageSource source, float amount) {
        if (part.broken()) return false;

        part.applyPartDamage(amount);
        float transferred = switch (part.slot) {
            case CORE -> amount * 1.25F;
            case HEAD -> amount * 0.75F;
            case LEFT_LEG, RIGHT_LEG -> amount * 0.35F;
            case LEFT_ARM, RIGHT_ARM -> amount * 0.30F;
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
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        for (int i = 0; i < parts.length; i++) {
            parts[i].setId(packet.getId() + i + 1);
        }
        partsInitialized = false;
        updatePartPositions();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    private enum PartSlot {
        HEAD,
        CORE,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG
    }

    private record PartSpec(PartSlot slot, double x, double y, double z,
                            float width, float height, float health) {}

    private static final class ColossusPart extends PartEntity<HollowColossusEntity> {
        private final PartSlot slot;
        private final EntityDimensions dimensions;
        private float partHealth;

        private ColossusPart(HollowColossusEntity parent, PartSlot slot,
                             float width, float height, float health) {
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

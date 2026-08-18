package kr.moonseungjun.villageguardians;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class VillageSkillEffectEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_KIND =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_OWNER =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DIR_X =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Y =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Z =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPEED =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SEED =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_EXTRA =
            SynchedEntityData.defineId(VillageSkillEffectEntity.class, EntityDataSerializers.STRING);

    public VillageSkillEffectEntity(EntityType<? extends VillageSkillEffectEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static VillageSkillEffectEntity spawn(
            ServerLevel level,
            Entity owner,
            String kind,
            Vec3 position,
            Vec3 direction,
            int duration,
            float speed,
            String extra) {
        VillageSkillEffectEntity entity =
                VillageSkillEffectEntities.SKILL_EFFECT.get().create(level, EntitySpawnReason.EVENT);
        if (entity == null) return null;
        Vec3 normalized = direction == null || direction.lengthSqr() < 1.0E-6
                ? new Vec3(0.0, 0.0, 1.0)
                : direction.normalize();
        entity.setKind(kind);
        entity.setOwnerEntityId(owner == null ? -1 : owner.getId());
        entity.setDuration(duration);
        entity.setDirection(normalized);
        entity.setSpeed(speed);
        entity.setSeed(level.getRandom().nextInt());
        entity.setExtra(extra == null ? "" : extra);
        entity.setPos(position);
        entity.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, "");
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_DURATION, 20);
        builder.define(DATA_DIR_X, 0.0f);
        builder.define(DATA_DIR_Y, 0.0f);
        builder.define(DATA_DIR_Z, 1.0f);
        builder.define(DATA_SPEED, 0.0f);
        builder.define(DATA_SEED, 0);
        builder.define(DATA_EXTRA, "");
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        int duration = Math.max(1, duration());
        if (tickCount > duration) {
            discard();
            return;
        }
        Entity owner = ownerEntity();
        if ("arcanist_tornado".equals(kind()) && owner != null && owner.isAlive()) {
            Vec3 look = owner.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
            if (horizontal.lengthSqr() > 1.0E-6) setDirection(horizontal.normalize());
        }
        if (followsOwner()) {
            if (owner == null || !owner.isAlive()) {
                discard();
                return;
            }
            if (tracksOwnerLook()) {
                Vec3 look = owner.getLookAngle();
                if (kind().startsWith("warden_")) look = new Vec3(look.x, 0.0, look.z);
                if (look.lengthSqr() > 1.0E-6) setDirection(look.normalize());
            }
            Vec3 target = switch (kind()) {
                case "ranger_energy_charge" -> owner.getEyePosition().add(direction().scale(2.35));
                case "ranger_focus" -> owner.getEyePosition().add(direction().scale(1.15));
                case "vanguard_slam_charge" -> owner.position().add(0.0, 0.2, 0.0);
                default -> owner.position();
            };
            setPos(target);
        } else if (speed() != 0.0f) {
            setPos(position().add(direction().scale(speed())));
        }
    }

    private boolean tracksOwnerLook() {
        return switch (kind()) {
            case "ranger_focus", "ranger_energy_charge", "warden_charge_cast",
                    "warden_fortress", "warden_aegis" -> true;
            default -> false;
        };
    }

    private boolean followsOwner() {
        if (kind().startsWith("elite_aura_")) return true;
        return switch (kind()) {
            case "vanguard_spin", "vanguard_rally", "vanguard_blade_charge",
                    "vanguard_slam_charge", "ranger_rapid", "ranger_focus",
                    "ranger_energy_charge", "luminar_heal_cast", "luminar_cleanse_cast",
                    "luminar_miracle_cast",
                    "warden_charge_cast", "warden_taunt", "warden_fortress", "warden_aegis" -> true;
            default -> false;
        };
    }

    public Entity ownerEntity() {
        return ownerEntityId() < 0 ? null : level().getEntity(ownerEntityId());
    }

    public String kind() {
        return entityData.get(DATA_KIND);
    }

    public void setKind(String value) {
        entityData.set(DATA_KIND, value == null ? "" : value);
    }

    public int ownerEntityId() {
        return entityData.get(DATA_OWNER);
    }

    public void setOwnerEntityId(int value) {
        entityData.set(DATA_OWNER, value);
    }

    public int duration() {
        return entityData.get(DATA_DURATION);
    }

    public void setDuration(int value) {
        entityData.set(DATA_DURATION, Math.max(1, value));
    }

    public Vec3 direction() {
        return new Vec3(entityData.get(DATA_DIR_X), entityData.get(DATA_DIR_Y), entityData.get(DATA_DIR_Z));
    }

    public void setDirection(Vec3 value) {
        entityData.set(DATA_DIR_X, (float) value.x);
        entityData.set(DATA_DIR_Y, (float) value.y);
        entityData.set(DATA_DIR_Z, (float) value.z);
    }

    public float speed() {
        return entityData.get(DATA_SPEED);
    }

    public void setSpeed(float value) {
        entityData.set(DATA_SPEED, value);
    }

    public int seed() {
        return entityData.get(DATA_SEED);
    }

    public void setSeed(int value) {
        entityData.set(DATA_SEED, value);
    }

    public String extra() {
        return entityData.get(DATA_EXTRA);
    }

    public void setExtra(String value) {
        entityData.set(DATA_EXTRA, value == null ? "" : value);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // Runtime-only visual actor. The entity type is no-save.
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // Runtime-only visual actor. The entity type is no-save.
    }
}

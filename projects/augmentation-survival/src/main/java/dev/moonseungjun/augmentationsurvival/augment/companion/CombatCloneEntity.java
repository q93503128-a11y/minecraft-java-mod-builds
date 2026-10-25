package dev.moonseungjun.augmentationsurvival.augment.companion;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Temporary server-AI combat body created by the Clone augment. */
public final class CombatCloneEntity extends PathfinderMob {
    private UUID ownerId;
    private int remainingTicks;

    public CombatCloneEntity(EntityType<? extends CombatCloneEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public void configure(ServerPlayer owner, float maxHealth, double attackDamage, int durationTicks) {
        ownerId = owner.getUUID();
        remainingTicks = durationTicks;
        AttributeInstance maxHealthAttribute = getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance attackAttribute = getAttribute(Attributes.ATTACK_DAMAGE);
        if (maxHealthAttribute != null) maxHealthAttribute.setBaseValue(Math.max(1.0D, maxHealth));
        if (attackAttribute != null) attackAttribute.setBaseValue(Math.max(0.0D, attackDamage));
        setHealth(getMaxHealth());
        setPersistenceRequired();
        setCanPickUpLoot(false);
    }

    public UUID ownerId() {
        return ownerId;
    }

    public boolean isCloneAlly(Entity other) {
        if (other == null || ownerId == null) return false;
        if (other.getUUID().equals(ownerId)) return true;
        if (other instanceof CombatCloneEntity clone && ownerId.equals(clone.ownerId)) return true;
        if (CompanionAugmentService.isOwnedBy(other, ownerId)) return true;
        if (other instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity owner = tamable.getOwner();
            return owner != null && ownerId.equals(owner.getUUID());
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (remainingTicks <= 0 || ownerId == null) {
            discard();
            return;
        }
        remainingTicks--;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.isDeadOrDying() || owner.level() != level) {
            discard();
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && !CompanionAugmentService.isHostileTarget(this, target)) {
            setTarget(null);
            target = null;
        }
        if (target == null || tickCount % 10 == 0) {
            LivingEntity nearest = findNearestHostile();
            if (nearest != null) setTarget(nearest);
        }
    }

    private LivingEntity findNearestHostile() {
        double range = Math.max(8.0D, getAttributeValue(Attributes.FOLLOW_RANGE));
        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(range),
                candidate -> CompanionAugmentService.isHostileTarget(this, candidate)
        );
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerId != null) output.putString("clone_owner", ownerId.toString());
        output.putInt("clone_remaining_ticks", remainingTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ownerId = null;
        input.getString("clone_owner").ifPresent(value -> {
            try {
                ownerId = UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                ownerId = null;
            }
        });
        remainingTicks = input.getInt("clone_remaining_ticks").orElse(0);
        this.xpReward = 0;
        setCanPickUpLoot(false);
    }
}

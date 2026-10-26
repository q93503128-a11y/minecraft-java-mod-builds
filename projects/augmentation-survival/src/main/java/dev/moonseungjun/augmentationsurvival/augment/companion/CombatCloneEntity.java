package dev.moonseungjun.augmentationsurvival.augment.companion;

import dev.moonseungjun.augmentationsurvival.augment.active.CloneActiveService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Temporary server-AI combat body created by the Clone augment. */
public final class CombatCloneEntity extends PathfinderMob {
    private static final List<EquipmentSlot> PRESENTATION_SLOTS = List.of(
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
    );
    private static final EntityDataAccessor<String> OWNER_ID = SynchedEntityData.defineId(
            CombatCloneEntity.class,
            EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<Integer> MODEL_PARTS = SynchedEntityData.defineId(
            CombatCloneEntity.class,
            EntityDataSerializers.INT
    );
    private static final int ALL_MODEL_PARTS = allModelPartsMask();

    private UUID ownerId;
    private int remainingTicks;
    private int overtimeExtensionRemainingTicks;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_ID, "");
        builder.define(MODEL_PARTS, ALL_MODEL_PARTS);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public void configure(
            ServerPlayer owner,
            float maxHealth,
            double attackDamage,
            int durationTicks,
            int overtimeExtensionBudgetTicks
    ) {
        ownerId = owner.getUUID();
        entityData.set(OWNER_ID, ownerId.toString());
        remainingTicks = Math.max(1, durationTicks);
        overtimeExtensionRemainingTicks = Math.max(0, overtimeExtensionBudgetTicks);
        AttributeInstance maxHealthAttribute = getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance attackAttribute = getAttribute(Attributes.ATTACK_DAMAGE);
        if (maxHealthAttribute != null) maxHealthAttribute.setBaseValue(Math.max(1.0D, maxHealth));
        if (attackAttribute != null) attackAttribute.setBaseValue(Math.max(0.0D, attackDamage));
        copyPresentationSnapshot(owner);
        setHealth(getMaxHealth());
        setPersistenceRequired();
        setCanPickUpLoot(false);
    }

    /** Extends this manual Clone instance while respecting its per-use Overtime budget. */
    public int extendDuration(int requestedTicks) {
        if (requestedTicks <= 0 || remainingTicks <= 0 || overtimeExtensionRemainingTicks <= 0) {
            return 0;
        }
        int applied = Math.min(requestedTicks, overtimeExtensionRemainingTicks);
        remainingTicks = Math.min(Integer.MAX_VALUE, remainingTicks + applied);
        overtimeExtensionRemainingTicks -= applied;
        return applied;
    }

    private void copyPresentationSnapshot(ServerPlayer owner) {
        for (EquipmentSlot slot : PRESENTATION_SLOTS) {
            ItemStack equipped = owner.getItemBySlot(slot);
            setItemSlot(slot, presentationCopy(equipped));
            setDropChance(slot, 0.0F);
        }
        setLeftHanded(owner.getMainArm() == HumanoidArm.LEFT);
        int modelParts = 0;
        for (PlayerModelPart part : PlayerModelPart.values()) {
            if (owner.isModelPartShown(part)) modelParts |= part.getMask();
        }
        entityData.set(MODEL_PARTS, modelParts);
    }

    /** Keeps the item model/glint but prevents copied equipment from changing the fixed combat snapshot. */
    private static ItemStack presentationCopy(ItemStack source) {
        if (source.isEmpty()) return ItemStack.EMPTY;
        boolean glint = source.hasFoil();
        ItemStack copy = source.copy();
        copy.remove(DataComponents.ATTRIBUTE_MODIFIERS);
        copy.remove(DataComponents.ENCHANTMENTS);
        if (glint) copy.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return copy;
    }

    public UUID ownerId() {
        String syncedOwnerId = entityData.get(OWNER_ID);
        if (!syncedOwnerId.isEmpty()) {
            try {
                return UUID.fromString(syncedOwnerId);
            } catch (IllegalArgumentException ignored) {
                // Fall back to the server-side field below.
            }
        }
        return ownerId;
    }

    public boolean modelPartShown(PlayerModelPart part) {
        return (entityData.get(MODEL_PARTS) & part.getMask()) != 0;
    }

    public boolean isCloneAlly(Entity other) {
        UUID resolvedOwner = ownerId();
        if (other == null || resolvedOwner == null) return false;
        if (other.getUUID().equals(resolvedOwner)) return true;
        if (other instanceof CombatCloneEntity clone && resolvedOwner.equals(clone.ownerId())) return true;
        if (CompanionAugmentService.isOwnedBy(other, resolvedOwner)) return true;
        if (other instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity owner = tamable.getOwner();
            return owner != null && resolvedOwner.equals(owner.getUUID());
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        UUID resolvedOwner = ownerId();
        if (remainingTicks <= 0 || resolvedOwner == null) {
            CloneActiveService.unregisterActiveClone(this);
            discard();
            return;
        }
        CloneActiveService.registerActiveClone(this);
        remainingTicks--;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(resolvedOwner);
        if (owner == null || owner.isDeadOrDying() || owner.level() != level) {
            CloneActiveService.unregisterActiveClone(this);
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
        UUID resolvedOwner = ownerId();
        if (resolvedOwner != null) output.putString("clone_owner", resolvedOwner.toString());
        output.putInt("clone_remaining_ticks", remainingTicks);
        output.putInt("clone_overtime_extension_remaining_ticks", overtimeExtensionRemainingTicks);
        output.putInt("clone_model_parts", entityData.get(MODEL_PARTS));
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
        entityData.set(OWNER_ID, ownerId == null ? "" : ownerId.toString());
        entityData.set(MODEL_PARTS, input.getInt("clone_model_parts").orElse(ALL_MODEL_PARTS));
        remainingTicks = input.getInt("clone_remaining_ticks").orElse(0);
        overtimeExtensionRemainingTicks = input.getInt("clone_overtime_extension_remaining_ticks").orElse(0);
        this.xpReward = 0;
        setCanPickUpLoot(false);
        for (EquipmentSlot slot : PRESENTATION_SLOTS) {
            setDropChance(slot, 0.0F);
        }
    }

    private static int allModelPartsMask() {
        int mask = 0;
        for (PlayerModelPart part : PlayerModelPart.values()) mask |= part.getMask();
        return mask;
    }
}

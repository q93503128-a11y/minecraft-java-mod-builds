package kr.moonseungjun.riftfrontier.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Physical server/client actor identity for the Region 01 boss.
 *
 * <p>This class deliberately does not invent final AI, dimensions, combat tuning, animation or presentation data.
 * The authoritative boss combat timeline already lives in the combat domain and will be attached when the production
 * encounter/spawn gate is opened. Until then this registered type is not referenced by Region 01 encounter content.</p>
 */
public final class Region01BossEntity extends LivingEntity {
    public Region01BossEntity(EntityType<? extends Region01BossEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // Region 01 boss equipment is not an authored gameplay axis; no vanilla equipment slots are exposed.
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}

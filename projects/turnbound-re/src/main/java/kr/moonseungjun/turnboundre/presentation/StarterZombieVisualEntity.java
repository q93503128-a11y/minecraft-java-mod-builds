package kr.moonseungjun.turnboundre.presentation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Presentation-only humanoid used by character preview and the virtual battle stage.
 * It has no gameplay AI, inventory, drops, progression state, or server-owned combat authority.
 */
public final class StarterZombieVisualEntity extends LivingEntity {
    public StarterZombieVisualEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // Presentation entity intentionally owns no equipment state.
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}

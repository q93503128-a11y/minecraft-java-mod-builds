package kr.moonseungjun.turnboundre.presentation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Presentation-only Blaze used by character preview and the virtual battle stage.
 * It owns no gameplay AI, drops, equipment, progression state, or combat authority.
 */
public final class BlazeVisualEntity extends Monster {
    public BlazeVisualEntity(EntityType<? extends Monster> type, Level level) {
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

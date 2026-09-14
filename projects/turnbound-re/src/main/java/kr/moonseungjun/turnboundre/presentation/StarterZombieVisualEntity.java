package kr.moonseungjun.turnboundre.presentation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Presentation-only humanoid used by character preview and the virtual battle stage.
 * It has no registered goals, inventory, drops, progression state, or server-owned combat authority.
 *
 * It extends Monster only so the existing presentation Pose contract can drive Mob#aggressive without creating
 * a second animation-control path. The entity is never spawned into normal gameplay by TURNBOUND: RE.
 */
public final class StarterZombieVisualEntity extends Monster {
    public StarterZombieVisualEntity(EntityType<? extends Monster> type, Level level) {
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

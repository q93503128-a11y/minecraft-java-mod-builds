package kr.moonseungjun.turnboundre.presentation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Presentation-only Spider used by Character Detail and the virtual battle stage.
 * Gameplay remains authored as minecraft:spider; this entity owns no AI, drops, combat authority or save state.
 */
public final class SpiderVisualEntity extends Monster implements PresentationPoseAware {
    private TurnboundPresentationPose presentationPose = TurnboundPresentationPose.NEUTRAL;

    public SpiderVisualEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public TurnboundPresentationPose presentationPose() {
        return presentationPose;
    }

    @Override
    public void setPresentationPose(TurnboundPresentationPose pose) {
        presentationPose = pose == null ? TurnboundPresentationPose.NEUTRAL : pose;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // Presentation entity intentionally owns no gameplay equipment state.
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}

package kr.moonseungjun.turnboundre.presentation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Presentation-only Creeper used by Character Detail and the virtual battle stage.
 * Gameplay remains authored as minecraft:creeper; this entity owns no AI, drops, combat authority or save state.
 */
public final class CreeperVisualEntity extends Monster implements PresentationPoseAware {
    private TurnboundPresentationPose presentationPose = TurnboundPresentationPose.NEUTRAL;
    private boolean volatileCharged;

    public CreeperVisualEntity(EntityType<? extends Monster> type, Level level) {
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

    public boolean volatileCharged() {
        return volatileCharged;
    }

    public void setVolatileCharged(boolean volatileCharged) {
        this.volatileCharged = volatileCharged;
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

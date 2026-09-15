package kr.moonseungjun.turnboundre.client.presentation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.blaze.BlazeModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * TURNBOUND Blaze presentation using Mojang's runtime Blaze model directly.
 * Replacement core/rod geometry is forbidden; TURNBOUND only adds an action-readable pose to the external base.
 */
public final class BlazeVisualModel extends BlazeModel {
    private final ModelPart head;

    public BlazeVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
    }

    @Override
    public void setupAnim(LivingEntityRenderState baseState) {
        super.setupAnim(baseState);
        if (!(baseState instanceof BlazeVisualRenderState state) || !state.aggressive) {
            return;
        }

        float pulse = Mth.sin(state.presentationTime * 0.24F);
        head.xRot += -0.10F + pulse * 0.018F;
        head.yRot += pulse * 0.035F;
    }
}

package kr.moonseungjun.turnboundre.client.presentation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.state.WitchRenderState;

/**
 * TURNBOUND Witch presentation using Mojang's runtime Witch model directly.
 * No replacement hat, satchel, robe silhouette or other TURNBOUND-authored geometry is allowed here.
 */
public final class WitchVisualModel extends WitchModel {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart arms;

    public WitchVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        arms = root.getChild("arms");
    }

    @Override
    public void setupAnim(WitchRenderState baseState) {
        super.setupAnim(baseState);
        if (!(baseState instanceof WitchVisualRenderState state) || !state.offensive) {
            return;
        }

        // Exact enemy-target Witch actions may add a compact throw commitment, but silhouette stays Mojang's model.
        body.xRot += 0.17F;
        body.zRot -= 0.025F;
        head.xRot += 0.12F;
        head.yRot += 0.10F;
        arms.xRot = -1.06F;
        arms.yRot = -0.16F;
        arms.zRot = 0.08F;
        arms.y = 2.35F;
        arms.z = -1.75F;
    }
}

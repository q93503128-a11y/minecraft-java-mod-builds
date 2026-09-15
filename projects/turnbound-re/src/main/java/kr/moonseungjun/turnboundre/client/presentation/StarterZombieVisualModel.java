package kr.moonseungjun.turnboundre.client.presentation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.zombie.ZombieModel;

/**
 * TURNBOUND starter Zombie presentation using Mojang's runtime Zombie model directly.
 * The previous reference-recreated segmented humanoid geometry is intentionally removed.
 */
public final class StarterZombieVisualModel extends ZombieModel<StarterZombieVisualRenderState> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public StarterZombieVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    @Override
    public void setupAnim(StarterZombieVisualRenderState state) {
        super.setupAnim(state);
        if (!state.aggressive) {
            return;
        }

        // Enemy-targeting cue only; body design remains Mojang's actual Zombie model.
        body.xRot -= 0.13F;
        head.xRot -= 0.055F;
        head.yRot -= 0.055F;
        rightArm.xRot = -1.23F;
        rightArm.yRot = 0.13F;
        rightArm.zRot = 0.16F;
        leftArm.xRot = -0.91F;
        leftArm.yRot = -0.08F;
        leftArm.zRot = -0.19F;
        rightLeg.xRot += 0.10F;
        leftLeg.xRot -= 0.08F;
    }
}

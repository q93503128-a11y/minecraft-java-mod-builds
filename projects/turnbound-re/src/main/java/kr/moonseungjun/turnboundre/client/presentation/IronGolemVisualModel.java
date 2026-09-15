package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;

/**
 * TURNBOUND Iron Golem presentation using Mojang's runtime Iron Golem model directly.
 * No widened chest, segmented forearms or other replacement geometry is authored by TURNBOUND.
 */
public final class IronGolemVisualModel extends IronGolemModel {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public IronGolemVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    @Override
    public void setupAnim(IronGolemRenderState baseState) {
        super.setupAnim(baseState);
        if (!(baseState instanceof IronGolemVisualRenderState state)) {
            return;
        }

        TurnboundPresentationPose pose = state.presentationPose == null
                ? TurnboundPresentationPose.NEUTRAL
                : state.presentationPose;
        switch (pose) {
            case OFFENSIVE -> {
                head.yRot += 0.10F;
                rightArm.xRot = -1.62F;
                rightArm.yRot = 0.24F;
                rightArm.zRot = 0.12F;
                leftArm.xRot = -0.48F;
                leftArm.yRot = -0.14F;
                leftArm.zRot = -0.16F;
                rightLeg.xRot += 0.10F;
                leftLeg.xRot -= 0.08F;
            }
            case DEFENSIVE -> {
                head.xRot += 0.06F;
                rightArm.xRot = -1.10F;
                rightArm.yRot = 0.22F;
                rightArm.zRot = 0.20F;
                leftArm.xRot = -1.10F;
                leftArm.yRot = -0.22F;
                leftArm.zRot = -0.20F;
                rightLeg.xRot += 0.11F;
                leftLeg.xRot += 0.11F;
            }
            case SLAM -> {
                head.xRot += 0.10F;
                rightArm.xRot = -2.02F;
                rightArm.yRot = 0.11F;
                rightArm.zRot = 0.18F;
                leftArm.xRot = -2.02F;
                leftArm.yRot = -0.11F;
                leftArm.zRot = -0.18F;
                rightLeg.xRot += 0.22F;
                leftLeg.xRot += 0.22F;
            }
            case EXECUTE -> {
                head.xRot -= 0.07F;
                head.yRot -= 0.10F;
                rightArm.xRot = -2.28F;
                rightArm.yRot = 0.22F;
                rightArm.zRot = 0.12F;
                leftArm.xRot = -1.42F;
                leftArm.yRot = -0.18F;
                leftArm.zRot = -0.20F;
                rightLeg.xRot += 0.06F;
                leftLeg.xRot -= 0.11F;
            }
            default -> {
                // Vanilla runtime pose remains authoritative for neutral/unsupported pose families.
            }
        }
    }
}

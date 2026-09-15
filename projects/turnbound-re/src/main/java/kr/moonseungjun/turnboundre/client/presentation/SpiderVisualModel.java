package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * TURNBOUND Spider presentation using Mojang's runtime Spider model directly.
 * No hand-authored replacement silhouette, extra limbs or reference-recreated geometry is allowed here.
 */
public final class SpiderVisualModel extends SpiderModel {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart abdomen;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightMiddleHindLeg;
    private final ModelPart leftMiddleHindLeg;
    private final ModelPart rightMiddleFrontLeg;
    private final ModelPart leftMiddleFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public SpiderVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body0");
        abdomen = root.getChild("body1");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightMiddleHindLeg = root.getChild("right_middle_hind_leg");
        leftMiddleHindLeg = root.getChild("left_middle_hind_leg");
        rightMiddleFrontLeg = root.getChild("right_middle_front_leg");
        leftMiddleFrontLeg = root.getChild("left_middle_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
    }

    @Override
    public void setupAnim(LivingEntityRenderState baseState) {
        super.setupAnim(baseState);
        if (!(baseState instanceof SpiderVisualRenderState state)) {
            return;
        }

        TurnboundPresentationPose pose = state.presentationPose == null
                ? TurnboundPresentationPose.NEUTRAL
                : state.presentationPose;
        switch (pose) {
            case OFFENSIVE -> {
                // Fang: preserve Mojang's spider design and add only an action-readable head/front-leg snap.
                head.xRot += 0.18F;
                body.xRot -= 0.10F;
                rightFrontLeg.yRot -= 0.24F;
                leftFrontLeg.yRot += 0.24F;
                rightFrontLeg.zRot -= 0.18F;
                leftFrontLeg.zRot += 0.18F;
            }
            case VENOM -> {
                // Venom Bite: a lower committed bite using the existing external model parts only.
                head.xRot += 0.30F;
                body.xRot += 0.08F;
                rightMiddleFrontLeg.zRot -= 0.10F;
                leftMiddleFrontLeg.zRot += 0.10F;
                rightFrontLeg.yRot -= 0.14F;
                leftFrontLeg.yRot += 0.14F;
            }
            case WEB -> {
                // Binding Web: abdomen lift/front anchor; projectile identity is handled by the cobweb stage cue.
                abdomen.xRot -= 0.30F;
                body.xRot += 0.08F;
                head.xRot -= 0.04F;
                rightFrontLeg.yRot -= 0.16F;
                leftFrontLeg.yRot += 0.16F;
                rightHindLeg.zRot -= 0.13F;
                leftHindLeg.zRot += 0.13F;
            }
            case POUNCE -> {
                // Burst: coil all eight Mojang spider legs without inventing a new silhouette.
                head.xRot += 0.20F;
                body.xRot += 0.18F;
                abdomen.xRot += 0.12F;
                rightHindLeg.zRot += 0.20F;
                rightMiddleHindLeg.zRot += 0.20F;
                rightMiddleFrontLeg.zRot += 0.20F;
                rightFrontLeg.zRot += 0.20F;
                leftHindLeg.zRot -= 0.20F;
                leftMiddleHindLeg.zRot -= 0.20F;
                leftMiddleFrontLeg.zRot -= 0.20F;
                leftFrontLeg.zRot -= 0.20F;
            }
            default -> {
                // Other pose families belong to other presentation entities.
            }
        }
    }
}

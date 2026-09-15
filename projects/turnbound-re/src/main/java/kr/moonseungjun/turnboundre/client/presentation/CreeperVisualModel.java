package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.util.Mth;

/**
 * TURNBOUND Creeper presentation using Mojang's runtime Creeper model directly.
 * No hand-authored replacement geometry or UV layout is allowed here; action cues are pose offsets on the external base model.
 */
public final class CreeperVisualModel extends CreeperModel {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public CreeperVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
    }

    @Override
    public void setupAnim(CreeperRenderState baseState) {
        super.setupAnim(baseState);
        if (!(baseState instanceof CreeperVisualRenderState state)) {
            return;
        }

        float fuse = Mth.sin(state.presentationTime * 0.42F);

        if (state.volatileCharged) {
            body.xRot += 0.045F + fuse * 0.018F;
            body.yRot += fuse * 0.018F;
            head.xRot += 0.05F + fuse * 0.025F;
            head.yRot += fuse * 0.025F;
            rightHindLeg.zRot -= 0.055F;
            leftHindLeg.zRot += 0.055F;
            rightFrontLeg.zRot -= 0.055F;
            leftFrontLeg.zRot += 0.055F;
        }

        TurnboundPresentationPose pose = state.presentationPose == null
                ? TurnboundPresentationPose.NEUTRAL
                : state.presentationPose;
        switch (pose) {
            case OFFENSIVE -> {
                body.xRot -= 0.18F;
                head.xRot -= 0.10F;
                head.yRot += 0.08F;
                rightFrontLeg.xRot += 0.22F;
                leftFrontLeg.xRot -= 0.16F;
                rightHindLeg.xRot -= 0.10F;
                leftHindLeg.xRot += 0.10F;
            }
            case CHARGE -> {
                body.xRot += 0.10F + fuse * 0.030F;
                body.yRot += fuse * 0.022F;
                head.xRot += 0.14F + fuse * 0.040F;
                head.yRot += fuse * 0.030F;
                rightHindLeg.xRot += 0.12F;
                leftHindLeg.xRot += 0.12F;
                rightFrontLeg.xRot -= 0.10F;
                leftFrontLeg.xRot -= 0.10F;
                rightHindLeg.zRot -= 0.11F;
                leftHindLeg.zRot += 0.11F;
                rightFrontLeg.zRot -= 0.11F;
                leftFrontLeg.zRot += 0.11F;
            }
            case BLAST -> {
                body.xRot += 0.20F + fuse * 0.022F;
                head.xRot += 0.23F + fuse * 0.025F;
                rightHindLeg.xRot += 0.18F;
                leftHindLeg.xRot += 0.18F;
                rightFrontLeg.xRot -= 0.15F;
                leftFrontLeg.xRot -= 0.15F;
                rightHindLeg.zRot -= 0.16F;
                leftHindLeg.zRot += 0.16F;
                rightFrontLeg.zRot -= 0.15F;
                leftFrontLeg.zRot += 0.15F;
            }
            case CATASTROPHE -> {
                body.xRot += 0.31F + fuse * 0.040F;
                body.yRot += fuse * 0.035F;
                head.xRot += 0.35F + fuse * 0.050F;
                head.yRot -= fuse * 0.040F;
                rightHindLeg.xRot += 0.25F;
                leftHindLeg.xRot += 0.25F;
                rightFrontLeg.xRot -= 0.22F;
                leftFrontLeg.xRot -= 0.22F;
                rightHindLeg.zRot -= 0.22F;
                leftHindLeg.zRot += 0.22F;
                rightFrontLeg.zRot -= 0.20F;
                leftFrontLeg.zRot += 0.20F;
            }
            default -> {
                // Other pose families belong to other presentation entities.
            }
        }
    }
}

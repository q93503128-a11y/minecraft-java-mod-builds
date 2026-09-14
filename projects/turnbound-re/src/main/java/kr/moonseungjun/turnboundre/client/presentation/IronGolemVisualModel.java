package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * TURNBOUND Iron Golem presentation model.
 *
 * Vanilla identity is preserved through the runtime Iron Golem texture, while the geometry exaggerates the
 * shoulder/chest mass and segmented forearms so VANGUARD/BREAKER weight reads at battle-stage scale. Distinct
 * presentation poses separate protection, fist pressure, ground slam and the Burst execution silhouette.
 */
public final class IronGolemVisualModel extends EntityModel<IronGolemVisualRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "iron_golem_visual"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightUpperArm;
    private final ModelPart rightForearm;
    private final ModelPart leftUpperArm;
    private final ModelPart leftForearm;
    private final ModelPart rightThigh;
    private final ModelPart rightShin;
    private final ModelPart leftThigh;
    private final ModelPart leftShin;

    public IronGolemVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightUpperArm = root.getChild("right_upper_arm");
        rightForearm = rightUpperArm.getChild("right_forearm");
        leftUpperArm = root.getChild("left_upper_arm");
        leftForearm = leftUpperArm.getChild("left_forearm");
        rightThigh = root.getChild("right_thigh");
        rightShin = rightThigh.getChild("right_shin");
        leftThigh = root.getChild("left_thigh");
        leftShin = leftThigh.getChild("left_shin");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -10.0F, -5.0F, 8.0F, 10.0F, 8.0F)
                        .texOffs(24, 0).addBox(-1.0F, -4.0F, -6.5F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -6.0F, -1.5F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-10.0F, -3.0F, -4.0F, 20.0F, 12.0F, 10.0F)
                        .texOffs(0, 64).addBox(-11.0F, -2.5F, -4.5F, 22.0F, 5.0F, 11.0F,
                                new CubeDeformation(0.15F))
                        .texOffs(0, 82).addBox(-7.0F, 8.0F, -3.0F, 14.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, -6.0F, 0.0F));

        PartDefinition rightArm = root.addOrReplaceChild("right_upper_arm",
                CubeListBuilder.create().texOffs(60, 21)
                        .addBox(-3.0F, -2.0F, -3.5F, 6.0F, 13.0F, 7.0F),
                PartPose.offset(-10.5F, -5.0F, 0.5F));
        rightArm.addOrReplaceChild("right_forearm",
                CubeListBuilder.create().texOffs(60, 42)
                        .addBox(-3.5F, 0.0F, -4.0F, 7.0F, 14.0F, 8.0F,
                                new CubeDeformation(0.12F)),
                PartPose.offset(0.0F, 9.5F, 0.0F));

        PartDefinition leftArm = root.addOrReplaceChild("left_upper_arm",
                CubeListBuilder.create().texOffs(60, 21).mirror()
                        .addBox(-3.0F, -2.0F, -3.5F, 6.0F, 13.0F, 7.0F),
                PartPose.offset(10.5F, -5.0F, 0.5F));
        leftArm.addOrReplaceChild("left_forearm",
                CubeListBuilder.create().texOffs(60, 42).mirror()
                        .addBox(-3.5F, 0.0F, -4.0F, 7.0F, 14.0F, 8.0F,
                                new CubeDeformation(0.12F)),
                PartPose.offset(0.0F, 9.5F, 0.0F));

        PartDefinition rightLeg = root.addOrReplaceChild("right_thigh",
                CubeListBuilder.create().texOffs(37, 0)
                        .addBox(-3.5F, 0.0F, -3.0F, 7.0F, 9.0F, 6.0F),
                PartPose.offset(-4.4F, 8.0F, 1.0F));
        rightLeg.addOrReplaceChild("right_shin",
                CubeListBuilder.create().texOffs(37, 16)
                        .addBox(-3.5F, 0.0F, -3.0F, 7.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_thigh",
                CubeListBuilder.create().texOffs(37, 0).mirror()
                        .addBox(-3.5F, 0.0F, -3.0F, 7.0F, 9.0F, 6.0F),
                PartPose.offset(4.4F, 8.0F, 1.0F));
        leftLeg.addOrReplaceChild("left_shin",
                CubeListBuilder.create().texOffs(37, 16).mirror()
                        .addBox(-3.5F, 0.0F, -3.0F, 7.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(IronGolemVisualRenderState state) {
        super.setupAnim(state);

        float time = state.presentationTime;
        float weight = Mth.sin(time * 0.045F);
        float secondary = Mth.sin(time * 0.028F + 1.35F);

        // Default: restrained, slow weight transfer. The wide chest and forearms carry most of the silhouette.
        body.xRot = 0.025F + weight * 0.008F;
        body.yRot = secondary * 0.012F;
        head.xRot = -0.015F + weight * 0.010F;
        head.yRot = secondary * 0.025F;
        rightUpperArm.xRot = 0.045F + weight * 0.018F;
        rightUpperArm.yRot = 0.0F;
        rightUpperArm.zRot = 0.035F;
        rightForearm.xRot = -0.035F;
        rightForearm.yRot = 0.0F;
        rightForearm.zRot = 0.0F;
        leftUpperArm.xRot = 0.035F - weight * 0.018F;
        leftUpperArm.yRot = 0.0F;
        leftUpperArm.zRot = -0.035F;
        leftForearm.xRot = -0.025F;
        leftForearm.yRot = 0.0F;
        leftForearm.zRot = 0.0F;
        rightThigh.xRot = secondary * 0.012F;
        rightThigh.zRot = 0.018F;
        rightShin.xRot = -secondary * 0.008F;
        leftThigh.xRot = -secondary * 0.012F;
        leftThigh.zRot = -0.018F;
        leftShin.xRot = secondary * 0.008F;

        TurnboundPresentationPose pose = state.presentationPose == null
                ? TurnboundPresentationPose.NEUTRAL
                : state.presentationPose;
        switch (pose) {
            case NEUTRAL -> {
                // Base pose already applied.
            }
            case OFFENSIVE -> {
                // Iron Fist: one enormous arm loads behind the shoulder while the other braces the torso.
                body.xRot = -0.075F;
                body.yRot = -0.16F;
                head.yRot = 0.10F;
                rightUpperArm.xRot = -1.62F;
                rightUpperArm.yRot = 0.24F;
                rightUpperArm.zRot = 0.12F;
                rightForearm.xRot = -0.34F;
                leftUpperArm.xRot = -0.48F;
                leftUpperArm.yRot = -0.14F;
                leftUpperArm.zRot = -0.16F;
                leftForearm.xRot = -0.20F;
                rightThigh.xRot = 0.10F;
                leftThigh.xRot = -0.08F;
            }
            case DEFENSIVE -> {
                // Guardian Plate: both forearms close the lane in front of an ally instead of reading as a punch.
                body.xRot = 0.10F;
                head.xRot = 0.06F;
                rightUpperArm.xRot = -1.10F;
                rightUpperArm.yRot = 0.22F;
                rightUpperArm.zRot = 0.20F;
                rightForearm.xRot = -0.48F;
                rightForearm.zRot = -0.08F;
                leftUpperArm.xRot = -1.10F;
                leftUpperArm.yRot = -0.22F;
                leftUpperArm.zRot = -0.20F;
                leftForearm.xRot = -0.48F;
                leftForearm.zRot = 0.08F;
                rightThigh.xRot = 0.11F;
                leftThigh.xRot = 0.11F;
            }
            case SLAM -> {
                // Ground Slam: broad two-arm compression keeps the hit direction visibly downward.
                body.xRot = 0.20F;
                head.xRot = 0.10F;
                rightUpperArm.xRot = -2.02F;
                rightUpperArm.yRot = 0.11F;
                rightUpperArm.zRot = 0.18F;
                rightForearm.xRot = -0.18F;
                leftUpperArm.xRot = -2.02F;
                leftUpperArm.yRot = -0.11F;
                leftUpperArm.zRot = -0.18F;
                leftForearm.xRot = -0.18F;
                rightThigh.xRot = 0.22F;
                leftThigh.xRot = 0.22F;
                rightShin.xRot = -0.16F;
                leftShin.xRot = -0.16F;
            }
            case EXECUTE -> {
                // Village Judgment: taller, asymmetric overhead silhouette reserved for the Burst.
                body.xRot = -0.12F;
                body.yRot = 0.12F;
                head.xRot = -0.07F;
                head.yRot = -0.10F;
                rightUpperArm.xRot = -2.28F;
                rightUpperArm.yRot = 0.22F;
                rightUpperArm.zRot = 0.12F;
                rightForearm.xRot = -0.36F;
                leftUpperArm.xRot = -1.42F;
                leftUpperArm.yRot = -0.18F;
                leftUpperArm.zRot = -0.20F;
                leftForearm.xRot = -0.58F;
                rightThigh.xRot = 0.06F;
                leftThigh.xRot = -0.11F;
            }
        }
    }
}

package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * First production roster model: a segmented, asymmetric zombie silhouette rather than a vanilla Zombie skin swap.
 * The articulation family is adapted from the CC0 Loy's Goodies humanoid template, while geometry/UV layout here is
 * rebuilt for TURNBOUND: RE's own 128x128 project texture.
 */
public final class StarterZombieVisualModel extends EntityModel<StarterZombieVisualRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "starter_zombie_visual"), "main");

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

    public StarterZombieVisualModel(ModelPart root) {
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
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(80, 0).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 3.0F, 9.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 10.0F, 4.0F)
                        .texOffs(40, 24).addBox(-4.5F, 0.5F, -2.5F, 9.0F, 5.0F, 5.0F)
                        .texOffs(0, 44).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 2.0F, 4.0F),
                PartPose.ZERO);

        PartDefinition rightUpperArmDef = root.addOrReplaceChild("right_upper_arm",
                CubeListBuilder.create()
                        .texOffs(40, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(72, 32).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(-5.0F, 2.5F, 0.0F, -0.38F, 0.08F, 0.12F));
        rightUpperArmDef.addOrReplaceChild("right_forearm",
                CubeListBuilder.create().texOffs(72, 56)
                        .addBox(-1.75F, 0.0F, -1.75F, 3.5F, 6.0F, 3.5F),
                PartPose.offsetAndRotation(0.0F, 4.8F, -0.2F, -0.58F, 0.0F, 0.04F));

        PartDefinition leftUpperArmDef = root.addOrReplaceChild("left_upper_arm",
                CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(96, 32).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(5.0F, 2.5F, 0.0F, -0.24F, -0.10F, -0.16F));
        leftUpperArmDef.addOrReplaceChild("left_forearm",
                CubeListBuilder.create().texOffs(40, 44)
                        .addBox(-1.75F, 0.0F, -1.75F, 3.5F, 6.0F, 3.5F),
                PartPose.offsetAndRotation(0.0F, 4.8F, 0.0F, -0.42F, 0.0F, -0.03F));

        PartDefinition rightThighDef = root.addOrReplaceChild("right_thigh",
                CubeListBuilder.create().texOffs(40, 88)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(-2.05F, 12.0F, 0.0F, 0.04F, 0.0F, 0.02F));
        rightThighDef.addOrReplaceChild("right_shin",
                CubeListBuilder.create().texOffs(80, 88)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, -0.04F, 0.0F, 0.0F));

        PartDefinition leftThighDef = root.addOrReplaceChild("left_thigh",
                CubeListBuilder.create().texOffs(72, 72)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(2.05F, 12.0F, 0.0F, -0.03F, 0.0F, -0.02F));
        leftThighDef.addOrReplaceChild("left_shin",
                CubeListBuilder.create().texOffs(0, 64)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.03F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(StarterZombieVisualRenderState state) {
        super.setupAnim(state);

        float breathe = Mth.sin(state.presentationTime * 0.085F);
        float secondary = Mth.sin(state.presentationTime * 0.052F + 1.4F);

        // Overview / waiting pose: restrained motion so the character feels alive without becoming UI noise.
        body.xRot += -0.035F + breathe * 0.012F;
        head.xRot += 0.035F + secondary * 0.012F;
        head.yRot += secondary * 0.018F;
        rightUpperArm.zRot += breathe * 0.022F;
        leftUpperArm.zRot -= breathe * 0.022F;
        rightThigh.xRot += secondary * 0.012F;
        leftThigh.xRot -= secondary * 0.012F;
        rightShin.xRot -= secondary * 0.008F;
        leftShin.xRot += secondary * 0.008F;

        if (!state.aggressive) return;

        // Enemy-targeting melee cue: broad asymmetric wind-up silhouette that reads at the small battle-stage size.
        float attackPulse = Mth.sin(state.presentationTime * 0.32F) * 0.045F;
        body.xRot = -0.13F;
        head.xRot = -0.055F;
        head.yRot = -0.055F;
        rightUpperArm.xRot = -1.23F + attackPulse;
        rightUpperArm.yRot = 0.13F;
        rightUpperArm.zRot = 0.16F;
        rightForearm.xRot = -0.72F - attackPulse;
        rightForearm.zRot = 0.06F;
        leftUpperArm.xRot = -0.91F - attackPulse * 0.6F;
        leftUpperArm.yRot = -0.08F;
        leftUpperArm.zRot = -0.19F;
        leftForearm.xRot = -0.57F;
        leftForearm.zRot = -0.05F;
        rightThigh.xRot = 0.10F;
        leftThigh.xRot = -0.08F;
        rightShin.xRot = -0.09F;
        leftShin.xRot = 0.07F;
    }
}

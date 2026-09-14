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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * First production roster model: a segmented, asymmetric zombie silhouette rather than a vanilla Zombie skin swap.
 * The articulation family is adapted from the CC0 Loy's Goodies humanoid template, while geometry/UV layout here is
 * rebuilt for TURNBOUND: RE's own 128x128 project texture.
 */
public final class StarterZombieVisualModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "starter_zombie_visual"), "main");

    public StarterZombieVisualModel(ModelPart root) {
        super(root);
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

        PartDefinition rightUpperArm = root.addOrReplaceChild("right_upper_arm",
                CubeListBuilder.create()
                        .texOffs(40, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(72, 32).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(-5.0F, 2.5F, 0.0F, -0.38F, 0.08F, 0.12F));
        rightUpperArm.addOrReplaceChild("right_forearm",
                CubeListBuilder.create().texOffs(72, 56)
                        .addBox(-1.75F, 0.0F, -1.75F, 3.5F, 6.0F, 3.5F),
                PartPose.offsetAndRotation(0.0F, 4.8F, -0.2F, -0.58F, 0.0F, 0.04F));

        PartDefinition leftUpperArm = root.addOrReplaceChild("left_upper_arm",
                CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(96, 32).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(5.0F, 2.5F, 0.0F, -0.24F, -0.10F, -0.16F));
        leftUpperArm.addOrReplaceChild("left_forearm",
                CubeListBuilder.create().texOffs(40, 44)
                        .addBox(-1.75F, 0.0F, -1.75F, 3.5F, 6.0F, 3.5F),
                PartPose.offsetAndRotation(0.0F, 4.8F, 0.0F, -0.42F, 0.0F, -0.03F));

        PartDefinition rightThigh = root.addOrReplaceChild("right_thigh",
                CubeListBuilder.create().texOffs(40, 88)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(-2.05F, 12.0F, 0.0F, 0.04F, 0.0F, 0.02F));
        rightThigh.addOrReplaceChild("right_shin",
                CubeListBuilder.create().texOffs(80, 88)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, -0.04F, 0.0F, 0.0F));

        PartDefinition leftThigh = root.addOrReplaceChild("left_thigh",
                CubeListBuilder.create().texOffs(72, 72)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(2.05F, 12.0F, 0.0F, -0.03F, 0.0F, -0.02F));
        leftThigh.addOrReplaceChild("left_shin",
                CubeListBuilder.create().texOffs(0, 64)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.03F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }
}

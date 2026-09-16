package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Broad-headed catfish silhouette authored for Fishing Game.
 * Whisker planes and head/body proportions are informed by the MIT-licensed Fishing Frenzy catfish
 * while retaining a 32x32 atlas compatible with the current catfish texture family.
 */
public final class CatfishEncounterFishModel extends EncounterFishModel {
    public CatfishEncounterFishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0f, -5.0f, -4.0f, 4.0f, 4.0f, 9.0f)
                        .texOffs(0, 14)
                        .addBox(-2.5f, -5.0f, -7.0f, 5.0f, 4.0f, 3.0f)
                        .texOffs(18, 0)
                        .addBox(0.0f, -7.0f, -2.0f, 0.0f, 2.0f, 5.0f)
                        .texOffs(18, 8)
                        .addBox(0.0f, -1.0f, 0.0f, 0.0f, 2.0f, 5.0f),
                PartPose.offset(0.0f, 24.0f, 0.0f)
        );

        PartDefinition whiskers = body.addOrReplaceChild(
                "whiskers",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, -3.0f, -7.0f)
        );
        whiskers.addOrReplaceChild(
                "left_upper",
                CubeListBuilder.create().texOffs(17, 15).addBox(0.0f, 0.0f, 0.0f, 5.0f, 0.0f, 1.0f),
                PartPose.offsetAndRotation(1.5f, -0.5f, 0.0f, 0.0f, 0.20f, -0.12f)
        );
        whiskers.addOrReplaceChild(
                "right_upper",
                CubeListBuilder.create().texOffs(17, 15).addBox(-5.0f, 0.0f, 0.0f, 5.0f, 0.0f, 1.0f),
                PartPose.offsetAndRotation(-1.5f, -0.5f, 0.0f, 0.0f, -0.20f, 0.12f)
        );
        whiskers.addOrReplaceChild(
                "left_lower",
                CubeListBuilder.create().texOffs(17, 17).addBox(0.0f, 0.0f, 0.0f, 4.0f, 0.0f, 1.0f),
                PartPose.offsetAndRotation(1.2f, 1.0f, 0.0f, 0.0f, 0.32f, 0.16f)
        );
        whiskers.addOrReplaceChild(
                "right_lower",
                CubeListBuilder.create().texOffs(17, 17).addBox(-4.0f, 0.0f, 0.0f, 4.0f, 0.0f, 1.0f),
                PartPose.offsetAndRotation(-1.2f, 1.0f, 0.0f, 0.0f, -0.32f, -0.16f)
        );

        PartDefinition fins = body.addOrReplaceChild(
                "fins",
                CubeListBuilder.create(),
                PartPose.ZERO
        );
        fins.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create().texOffs(0, 22).addBox(-4.0f, 0.0f, 0.0f, 4.0f, 0.0f, 3.0f),
                PartPose.offsetAndRotation(-2.0f, -3.0f, -1.0f, 0.0f, 0.28f, -0.30f)
        );
        fins.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create().texOffs(0, 22).addBox(0.0f, 0.0f, 0.0f, 4.0f, 0.0f, 3.0f),
                PartPose.offsetAndRotation(2.0f, -3.0f, -1.0f, 0.0f, -0.28f, 0.30f)
        );

        root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(14, 22)
                        .addBox(-1.5f, -3.5f, 0.0f, 3.0f, 3.0f, 5.0f)
                        .texOffs(0, 26)
                        .addBox(0.0f, -6.0f, 4.0f, 0.0f, 7.0f, 4.0f),
                PartPose.offset(0.0f, 24.0f, 5.0f)
        );

        return LayerDefinition.create(mesh, 32, 32);
    }
}

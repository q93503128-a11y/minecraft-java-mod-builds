package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometry adapted from Sea Life FatFishModel (MIT, Copyright (c) 2021 joshiejack).
 */
public final class FatEncounterFishModel extends EncounterFishModel {
    public FatEncounterFishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(22, 2)
                        .addBox(0.0f, -6.3f, 4.0f, 0.0f, 8.0f, 4.0f)
                        .texOffs(0, 4)
                        .addBox(0.0f, -4.3f, 0.0f, 0.0f, 4.0f, 4.0f),
                PartPose.offset(0.0f, 23.0f, 9.75f)
        );

        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0f, -8.0f, -3.0f, 4.0f, 8.0f, 14.0f)
                        .texOffs(0, 0)
                        .addBox(-2.0f, -5.2f, -5.0f, 4.0f, 4.0f, 2.0f)
                        .texOffs(0, 18)
                        .addBox(0.0f, -12.0f, 3.0f, 0.0f, 4.0f, 6.0f)
                        .texOffs(0, 0)
                        .addBox(0.0f, 0.0f, 3.0f, 0.0f, 2.0f, 6.0f),
                PartPose.offset(0.0f, 24.0f, 0.0f)
        );

        PartDefinition fins = root.addOrReplaceChild(
                "fins",
                CubeListBuilder.create(),
                PartPose.offset(-1.0f, 23.0f, -2.0f)
        );
        fins.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create().texOffs(0, 18).addBox(0.0f, -1.0f, 0.0f, 0.0f, 2.0f, 4.0f),
                PartPose.offsetAndRotation(-1.0f, -1.0f, 3.0f, -0.1745f, -0.6109f, 0.2618f)
        );
        fins.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create().texOffs(0, 8).addBox(0.0f, -1.0f, 0.0f, 0.0f, 2.0f, 4.0f),
                PartPose.offsetAndRotation(3.0f, -1.0f, 3.0f, -0.1745f, 0.6109f, -0.2618f)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}

package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometry adapted from Sea Life SmallFishModel (MIT, Copyright (c) 2021 joshiejack).
 */
public final class SmallEncounterFishModel extends EncounterFishModel {
    public SmallEncounterFishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(11, 1)
                        .addBox(0.0f, -1.4f, 0.0f, 0.0f, 3.0f, 3.0f),
                PartPose.offset(0.0f, 23.0f, 5.0f)
        );

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0f, -2.0f, -2.0f, 2.0f, 2.0f, 7.0f)
                        .texOffs(12, 12)
                        .addBox(-1.0f, -1.6f, -3.0f, 2.0f, 1.0f, 1.0f)
                        .texOffs(1, 11)
                        .addBox(0.0f, -4.0f, 1.0f, 0.0f, 2.0f, 3.0f)
                        .texOffs(11, 11)
                        .addBox(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 4.0f),
                PartPose.offset(0.0f, 24.0f, 0.0f)
        );

        PartDefinition fins = body.addOrReplaceChild(
                "fins",
                CubeListBuilder.create(),
                PartPose.offset(-1.0f, -1.0f, -2.0f)
        );
        fins.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, -0.5f, 0.0f, 0.0f, 1.0f, 2.0f),
                PartPose.offsetAndRotation(0.0f, 0.0f, 2.0f, -0.1745f, -0.6109f, 0.2618f)
        );
        fins.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, -0.5f, 0.0f, 0.0f, 1.0f, 2.0f),
                PartPose.offsetAndRotation(2.0f, 0.0f, 2.0f, -0.1745f, 0.6109f, -0.2618f)
        );

        return LayerDefinition.create(mesh, 32, 16);
    }
}

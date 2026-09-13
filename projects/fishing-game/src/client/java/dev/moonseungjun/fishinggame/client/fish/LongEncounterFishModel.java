package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometry adapted from Sea Life LongFishModel (MIT, Copyright (c) 2021 joshiejack).
 */
public final class LongEncounterFishModel extends EncounterFishModel {
    public LongEncounterFishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 24.0f, 0.0f)
        );
        body.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0f, -4.0f, -4.0f, 4.0f, 4.0f, 10.0f)
                        .texOffs(0, 0)
                        .addBox(-1.5f, -3.0f, -6.0f, 3.0f, 3.0f, 2.0f)
                        .texOffs(18, 4)
                        .addBox(-1.0f, -2.6f, -7.0f, 2.0f, 2.0f, 1.0f),
                PartPose.ZERO
        );

        PartDefinition fins = body.addOrReplaceChild(
                "fins",
                CubeListBuilder.create(),
                PartPose.offset(-1.0f, -1.0f, -2.0f)
        );
        fins.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create().texOffs(0, 3).addBox(0.0f, -1.5f, 0.0f, 0.0f, 2.0f, 4.0f),
                PartPose.offsetAndRotation(-1.0f, 0.0f, 3.0f, -0.1745f, -0.6109f, 0.2618f)
        );
        fins.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create().texOffs(0, 3).addBox(0.0f, -1.5f, 0.0f, 0.0f, 2.0f, 4.0f),
                PartPose.offsetAndRotation(3.0f, 0.0f, 3.0f, -0.1745f, 0.6109f, -0.2618f)
        );

        PartDefinition tail = root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(17, 14)
                        .addBox(-1.1947f, -1.0f, 6.9933f, 3.0f, 3.0f, 4.0f)
                        .texOffs(0, 16)
                        .addBox(-0.6947f, -1.25f, 12.9933f, 2.0f, 2.0f, 2.0f)
                        .texOffs(17, 14)
                        .addBox(-0.1947f, -1.75f, 14.9933f, 1.0f, 1.0f, 1.0f)
                        .texOffs(18, 0)
                        .addBox(-1.1947f, -0.5f, 10.9933f, 3.0f, 2.0f, 2.0f),
                PartPose.offset(-0.3f, 22.0f, 6.0f)
        );
        tail.addOrReplaceChild(
                "tailfin",
                CubeListBuilder.create().texOffs(7, 27).addBox(-3.1947f, -0.75f, -1.0067f, 7.0f, 0.0f, 5.0f),
                PartPose.offsetAndRotation(0.0f, 0.0f, 16.0f, 0.3491f, 0.0f, 0.0f)
        );
        tail.addOrReplaceChild(
                "backfin",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.3053f, 1.4128f, 13.9335f, 0.0f, 2.0f, 5.0f),
                PartPose.offsetAndRotation(0.0f, 0.0f, -3.0f, 0.1309f, 0.0f, 0.0f)
        );
        tail.addOrReplaceChild(
                "middle",
                CubeListBuilder.create()
                        .texOffs(17, 15)
                        .addBox(0.0f, -6.0f, 6.0f, 0.0f, 2.0f, 6.0f)
                        .texOffs(0, 23)
                        .addBox(0.0f, 0.0f, 9.0f, 0.0f, 3.0f, 4.0f)
                        .texOffs(0, 14)
                        .addBox(-2.0f, -4.0f, 5.0f, 4.0f, 4.0f, 9.0f),
                PartPose.offset(0.3f, 2.0f, -6.0f)
        );

        return LayerDefinition.create(mesh, 32, 32);
    }
}

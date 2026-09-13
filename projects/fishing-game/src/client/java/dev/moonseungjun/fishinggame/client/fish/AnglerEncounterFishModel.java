package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Geometry adapted from Sea Life AnglerfishModel (MIT, Copyright (c) 2021 joshiejack). */
public final class AnglerEncounterFishModel extends EncounterFishModel {
    public AnglerEncounterFishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(18, 13).addBox(0.0f, -7.3f, 3.0f, 0.0f, 6.0f, 6.0f)
                        .texOffs(0, 30).addBox(-2.0f, -6.0f, 0.0f, 4.0f, 3.0f, 3.0f),
                PartPose.offset(0.0f, 23.0f, 3.0f));

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0f, -9.0f, -8.0f, 10.0f, 9.0f, 10.0f)
                        .texOffs(0, 19).addBox(-4.0f, -9.0f, 2.0f, 8.0f, 7.0f, 1.0f)
                        .texOffs(30, 0).addBox(-3.0f, -8.0f, 3.0f, 6.0f, 5.0f, 1.0f)
                        .texOffs(16, 25).addBox(-3.0f, -8.45f, -10.0f, 6.0f, 6.0f, 2.0f)
                        .texOffs(0, 21).addBox(0.0f, -12.0f, -3.0f, 0.0f, 3.0f, 6.0f)
                        .texOffs(12, 25).addBox(0.0f, -2.0f, 2.0f, 0.0f, 2.0f, 2.0f),
                PartPose.offset(0.0f, 24.0f, 0.0f));

        body.addOrReplaceChild("light",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -6.0f, 1.0f, 2.0f, 8.0f, 2.0f),
                PartPose.offsetAndRotation(1.0f, -9.0f, -8.0f, 0.6109f, 0.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }
}

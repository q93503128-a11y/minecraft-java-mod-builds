package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Geometry adapted from Sea Life TallFishModel (MIT, Copyright (c) 2021 joshiejack). */
public final class TallEncounterFishModel extends EncounterFishModel {
    public TallEncounterFishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(11, 0).addBox(0.0f, 0.0f, 0.0f, 0.0f, 4.0f, 3.0f),
                PartPose.offset(0.0f, 20.0f, 4.0f));
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0f, -4.0f, -3.0f, 2.0f, 4.0f, 7.0f)
                        .texOffs(0, 0).addBox(-1.0f, -3.0f, -4.0f, 2.0f, 2.0f, 1.0f)
                        .texOffs(0, 8).addBox(0.0f, -5.0f, -2.0f, 0.0f, 1.0f, 4.0f)
                        .texOffs(0, 7).addBox(0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 4.0f),
                PartPose.offset(0.0f, 24.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 16);
    }
}

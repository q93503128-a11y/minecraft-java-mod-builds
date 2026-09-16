package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Streamlined trout/salmon/tuna silhouette authored for Fishing Game.
 * The torpedo profile is informed by the MIT-licensed Fishing Frenzy albacore proportions while
 * retaining the existing 64x32 Fishing Game texture contract.
 */
public final class PelagicEncounterFishModel extends EncounterFishModel {
    public PelagicEncounterFishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0f, -5.0f, -5.0f, 4.0f, 4.0f, 13.0f)
                        .texOffs(36, 0)
                        .addBox(-1.5f, -4.5f, -8.0f, 3.0f, 3.0f, 3.0f)
                        .texOffs(48, 0)
                        .addBox(-1.0f, -3.8f, -9.0f, 2.0f, 2.0f, 1.0f)
                        .texOffs(0, 19)
                        .addBox(0.0f, -8.0f, -1.0f, 0.0f, 3.0f, 5.0f)
                        .texOffs(10, 19)
                        .addBox(0.0f, -1.0f, 2.0f, 0.0f, 2.0f, 5.0f),
                PartPose.offset(0.0f, 24.0f, 0.0f)
        );

        PartDefinition fins = body.addOrReplaceChild(
                "fins",
                CubeListBuilder.create(),
                PartPose.ZERO
        );
        fins.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create().texOffs(22, 19).addBox(-4.0f, 0.0f, 0.0f, 4.0f, 0.0f, 3.0f),
                PartPose.offsetAndRotation(-2.0f, -3.0f, -2.0f, 0.0f, 0.22f, -0.28f)
        );
        fins.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create().texOffs(22, 19).addBox(0.0f, 0.0f, 0.0f, 4.0f, 0.0f, 3.0f),
                PartPose.offsetAndRotation(2.0f, -3.0f, -2.0f, 0.0f, -0.22f, 0.28f)
        );

        root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(36, 10)
                        .addBox(-1.0f, -3.5f, 0.0f, 2.0f, 2.0f, 4.0f)
                        .texOffs(0, 27)
                        .addBox(0.0f, -6.0f, 3.0f, 0.0f, 7.0f, 6.0f),
                PartPose.offset(0.0f, 24.0f, 8.0f)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}

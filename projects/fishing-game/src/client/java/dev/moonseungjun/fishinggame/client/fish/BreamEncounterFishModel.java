package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Laterally compressed sea-bream silhouette authored for Fishing Game.
 * The steep forehead/deep body reads separately from the generic fat-fish profile while preserving
 * the existing 64x32 species texture contract.
 */
public final class BreamEncounterFishModel extends EncounterFishModel {
    public BreamEncounterFishModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5f, -9.0f, -4.0f, 3.0f, 9.0f, 10.0f)
                        .texOffs(28, 0)
                        .addBox(-1.5f, -8.0f, -7.0f, 3.0f, 7.0f, 3.0f)
                        .texOffs(40, 0)
                        .addBox(-1.0f, -5.5f, -8.0f, 2.0f, 2.0f, 1.0f)
                        .texOffs(0, 21)
                        .addBox(0.0f, -13.0f, -2.0f, 0.0f, 4.0f, 6.0f)
                        .texOffs(12, 21)
                        .addBox(0.0f, 0.0f, -1.0f, 0.0f, 3.0f, 5.0f),
                PartPose.offset(0.0f, 24.0f, 0.0f)
        );

        PartDefinition fins = body.addOrReplaceChild(
                "fins",
                CubeListBuilder.create(),
                PartPose.ZERO
        );
        fins.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create().texOffs(24, 21).addBox(-4.0f, 0.0f, 0.0f, 4.0f, 0.0f, 4.0f),
                PartPose.offsetAndRotation(-1.5f, -4.0f, -1.0f, 0.0f, 0.20f, -0.40f)
        );
        fins.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create().texOffs(24, 21).addBox(0.0f, 0.0f, 0.0f, 4.0f, 0.0f, 4.0f),
                PartPose.offsetAndRotation(1.5f, -4.0f, -1.0f, 0.0f, -0.20f, 0.40f)
        );

        root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(42, 8)
                        .addBox(-1.0f, -4.5f, 0.0f, 2.0f, 4.0f, 3.0f)
                        .texOffs(38, 18)
                        .addBox(0.0f, -7.0f, 2.0f, 0.0f, 9.0f, 5.0f),
                PartPose.offset(0.0f, 24.0f, 6.0f)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}

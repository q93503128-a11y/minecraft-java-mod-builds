package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
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
 * Creeper presentation model that deliberately preserves Mojang's canonical Creeper silhouette and texture layout.
 * TURNBOUND differentiation comes from externally-referenced fuse/bracing motion, not a replacement creature design.
 */
public final class CreeperVisualModel extends EntityModel<CreeperVisualRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "creeper_visual"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public CreeperVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.0F, 18.0F, 4.0F));
        root.addOrReplaceChild("left_hind_leg", leg.mirror(), PartPose.offset(2.0F, 18.0F, 4.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.0F, 18.0F, -4.0F));
        root.addOrReplaceChild("left_front_leg", leg.mirror(), PartPose.offset(2.0F, 18.0F, -4.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(CreeperVisualRenderState state) {
        super.setupAnim(state);
        float time = state.presentationTime;
        float slow = Mth.sin(time * 0.035F);
        float fuse = Mth.sin(time * 0.42F);

        head.xRot = 0.01F;
        head.yRot = slow * 0.055F;
        head.zRot = 0.0F;
        body.xRot = 0.0F;
        body.yRot = 0.0F;
        body.zRot = slow * 0.014F;
        rightHindLeg.xRot = slow * 0.035F;
        leftHindLeg.xRot = -slow * 0.035F;
        rightFrontLeg.xRot = -slow * 0.035F;
        leftFrontLeg.xRot = slow * 0.035F;
        rightHindLeg.zRot = 0.0F;
        leftHindLeg.zRot = 0.0F;
        rightFrontLeg.zRot = 0.0F;
        leftFrontLeg.zRot = 0.0F;

        // The authoritative Volatile status remains visible after the short cast beat as a restrained fuse tension.
        if (state.volatileCharged) {
            body.xRot = 0.045F + fuse * 0.018F;
            body.yRot = fuse * 0.018F;
            head.xRot = 0.05F + fuse * 0.025F;
            head.yRot = fuse * 0.025F;
            rightHindLeg.zRot = -0.055F;
            leftHindLeg.zRot = 0.055F;
            rightFrontLeg.zRot = -0.055F;
            leftFrontLeg.zRot = 0.055F;
        }

        TurnboundPresentationPose pose = state.presentationPose == null
                ? TurnboundPresentationPose.NEUTRAL
                : state.presentationPose;
        switch (pose) {
            case OFFENSIVE -> {
                // Fuse Bash: a body-led shove. No fuse compression, so it cannot be mistaken for an explosion cast.
                body.xRot = -0.18F;
                head.xRot = -0.10F;
                head.yRot = 0.08F;
                rightFrontLeg.xRot = 0.22F;
                leftFrontLeg.xRot = -0.16F;
                rightHindLeg.xRot = -0.10F;
                leftHindLeg.xRot = 0.10F;
            }
            case CHARGE -> {
                // Volatile Charge: four feet brace while the familiar Creeper fuse tension tightens inward.
                body.xRot = 0.10F + fuse * 0.030F;
                body.yRot = fuse * 0.022F;
                head.xRot = 0.14F + fuse * 0.040F;
                head.yRot = fuse * 0.030F;
                rightHindLeg.xRot = 0.12F;
                leftHindLeg.xRot = 0.12F;
                rightFrontLeg.xRot = -0.10F;
                leftFrontLeg.xRot = -0.10F;
                rightHindLeg.zRot = -0.11F;
                leftHindLeg.zRot = 0.11F;
                rightFrontLeg.zRot = -0.11F;
                leftFrontLeg.zRot = 0.11F;
            }
            case BLAST -> {
                // Blast Wave: symmetric low brace before discharging the authoritative multi-target blast.
                body.xRot = 0.20F + fuse * 0.022F;
                head.xRot = 0.23F + fuse * 0.025F;
                rightHindLeg.xRot = 0.18F;
                leftHindLeg.xRot = 0.18F;
                rightFrontLeg.xRot = -0.15F;
                leftFrontLeg.xRot = -0.15F;
                rightHindLeg.zRot = -0.16F;
                leftHindLeg.zRot = 0.16F;
                rightFrontLeg.zRot = -0.15F;
                leftFrontLeg.zRot = 0.15F;
            }
            case CATASTROPHE -> {
                // Burst: deepest compression and fastest tension, visually reserved for Catastrophe.
                body.xRot = 0.31F + fuse * 0.040F;
                body.yRot = fuse * 0.035F;
                head.xRot = 0.35F + fuse * 0.050F;
                head.yRot = -fuse * 0.040F;
                rightHindLeg.xRot = 0.25F;
                leftHindLeg.xRot = 0.25F;
                rightFrontLeg.xRot = -0.22F;
                leftFrontLeg.xRot = -0.22F;
                rightHindLeg.zRot = -0.22F;
                leftHindLeg.zRot = 0.22F;
                rightFrontLeg.zRot = -0.20F;
                leftFrontLeg.zRot = 0.20F;
            }
            default -> {
                // Other pose families belong to other presentation entities.
            }
        }
    }
}

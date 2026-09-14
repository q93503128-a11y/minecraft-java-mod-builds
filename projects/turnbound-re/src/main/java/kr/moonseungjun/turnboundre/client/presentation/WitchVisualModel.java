package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * TURNBOUND Witch presentation model.
 *
 * The Minecraft Witch texture is reused, while silhouette and motion are authored for the representative
 * SUPPORT / CONTROLLER role: a lower asymmetric hat, side satchel and deliberate hunched alchemist idle.
 * Offensive ARCANE beats tighten the body and crossed arms into a forward throwing silhouette. Ally support
 * remains non-aggressive and is communicated by the battle-stage support transfer rather than reusing attack pose.
 */
public final class WitchVisualModel extends EntityModel<WitchVisualRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "witch_visual"), "main");

    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart body;
    private final ModelPart arms;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart satchel;

    public WitchVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        nose = head.getChild("nose");
        body = root.getChild("body");
        arms = root.getChild("arms");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        satchel = body.getChild("satchel");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = VillagerModel.createBodyModel();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");

        // Remove the generic villager head overlay and author a lower, asymmetric apothecary hat.
        head.clearChild("hat").clearRecursively();
        PartDefinition brim = head.addOrReplaceChild(
                "alchemy_hat",
                CubeListBuilder.create().texOffs(0, 64)
                        .addBox(-5.5F, -1.0F, -5.5F, 11.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, -0.035F, 0.0F, 0.025F));
        PartDefinition crown = brim.addOrReplaceChild(
                "crown",
                CubeListBuilder.create().texOffs(0, 76)
                        .addBox(-3.5F, -4.0F, -3.5F, 7.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(0.35F, -0.75F, 0.45F, -0.11F, 0.0F, 0.08F));
        crown.addOrReplaceChild(
                "tip",
                CubeListBuilder.create().texOffs(0, 87)
                        .addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.55F, -3.4F, 0.5F, -0.18F, 0.0F, 0.17F));

        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild(
                "satchel",
                CubeListBuilder.create().texOffs(44, 22)
                        .addBox(-1.5F, -2.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.15F)),
                PartPose.offsetAndRotation(4.2F, 6.7F, 1.5F, 0.08F, 0.0F, 0.10F));

        return LayerDefinition.create(mesh, 64, 128);
    }

    @Override
    public void setupAnim(WitchVisualRenderState state) {
        super.setupAnim(state);

        float time = state.presentationTime;
        float breathe = Mth.sin(time * 0.070F);
        float secondary = Mth.sin(time * 0.041F + 1.1F);

        head.yRot = secondary * 0.045F;
        nose.xRot = breathe * 0.045F;
        nose.zRot = secondary * 0.028F;
        rightLeg.xRot = 0.0F;
        leftLeg.xRot = 0.0F;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot = 0.0F;

        if (state.offensive) {
            // A compact, forward potion-throw silhouette: torso commits first, then arms and head follow.
            body.xRot = 0.17F;
            body.zRot = -0.025F;
            head.xRot = 0.12F + breathe * 0.018F;
            head.yRot += 0.10F;
            arms.xRot = -1.06F;
            arms.yRot = -0.16F;
            arms.zRot = 0.08F;
            arms.y = 2.35F;
            arms.z = -1.75F;
            satchel.zRot = 0.18F + breathe * 0.025F;
            satchel.xRot = -0.05F;
        } else {
            // Fresh-Animations-style principle: keep the vanilla identity but make the idle posture readable.
            body.xRot = 0.085F + breathe * 0.010F;
            body.zRot = secondary * 0.012F;
            head.xRot = 0.055F + breathe * 0.018F;
            arms.xRot = -0.83F + breathe * 0.025F;
            arms.yRot = secondary * 0.018F;
            arms.zRot = -secondary * 0.016F;
            arms.y = 3.0F + breathe * 0.10F;
            arms.z = -1.15F;
            satchel.zRot = 0.10F + secondary * 0.035F;
            satchel.xRot = breathe * 0.025F;
        }
    }
}

package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Original wooden hinged mannequin geometry with articulated limbs and silk anchors. */
public final class PuppetModel extends HierarchicalModel<PuppetEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "marionette"), "main");
    private final ModelPart root, head, leftArm, rightArm, leftLeg, rightLeg, strings;
    private final ModelPart blade, launcher, shield, chestBrace;

    public PuppetModel(ModelPart baked) {
        root = baked;
        head = baked.getChild("head");
        leftArm = baked.getChild("left_arm");
        rightArm = baked.getChild("right_arm");
        leftLeg = baked.getChild("left_leg");
        rightLeg = baked.getChild("right_leg");
        strings = baked.getChild("strings");
        blade = rightArm.getChild("blade");
        launcher = rightArm.getChild("launcher");
        shield = leftArm.getChild("shield");
        chestBrace = baked.getChild("body").getChild("brace");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -8.5F, -2.3F, 7, 9, 4.6F)
                        .texOffs(0, 14).addBox(-4, -9, -2.6F, 8, 1.3F, 5.2F),
                PartPose.offset(0, 12.5F, 0));
        // Heavy frame has substantial visible front plating instead of a hidden stat change.
        torso.addOrReplaceChild("brace", CubeListBuilder.create()
                        .texOffs(32, 42).addBox(-4.5F, -7.5F, -3.25F, 9F, 3.5F, 1F)
                        .texOffs(32, 47).addBox(-3.5F, -3F, -3.1F, 7F, 2F, 1F),
                PartPose.ZERO);
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(22, 0).addBox(-3.3F, -5.5F, -2.8F, 6.6F, 6.2F, 5.6F)
                        .texOffs(44, 0).addBox(-2.4F, -6.5F, -1.9F, 4.8F, 1F, 3.8F),
                PartPose.offset(0, 4.2F, 0));
        PartDefinition right = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(28, 18).addBox(-2F, -1F, -2F, 3F, 10F, 4F)
                        .texOffs(28, 32).addBox(-2.2F, 4.2F, -2.1F, 3.4F, 1F, 4.2F),
                PartPose.offset(-4.9F, 6.5F, 0));
        PartDefinition left = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(28, 18).mirror().addBox(-1F, -1F, -2F, 3F, 10F, 4F).mirror(false)
                        .texOffs(28, 32).addBox(-1.2F, 4.2F, -2.1F, 3.4F, 1F, 4.2F),
                PartPose.offset(4.9F, 6.5F, 0));
        // Three exclusive battle silhouettes share the same moving puppet skeleton.
        right.addOrReplaceChild("blade", CubeListBuilder.create()
                        .texOffs(44, 30).addBox(-1F, 1F, -1.7F, 2F, 11F, 2F)
                        .texOffs(45, 41).addBox(-3F, 0, -1.8F, 6F, 1.5F, 2F),
                PartPose.offset(-.5F, 6F, -.7F));
        right.addOrReplaceChild("launcher", CubeListBuilder.create()
                        .texOffs(42, 22).addBox(-2F, 3F, -10F, 4F, 3F, 10F)
                        .texOffs(49, 36).addBox(-1F, 3.5F, -12F, 2F, 2F, 2F),
                PartPose.offset(-.5F, 1F, -1F));
        left.addOrReplaceChild("shield", CubeListBuilder.create()
                        .texOffs(0, 50).addBox(-4.5F, 0, -4.5F, 9F, 12F, 1.5F)
                        .texOffs(22, 52).addBox(-3F, 1.5F, -4.9F, 6F, 2F, .8F),
                PartPose.offset(.5F, 2F, -.4F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 28).addBox(-2.1F, 0, -2F, 3.5F, 10F, 4F)
                        .texOffs(0, 42).addBox(-2.3F, 8.5F, -3.1F, 4F, 1.5F, 5F),
                PartPose.offset(-2.1F, 14.2F, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 28).addBox(-1.4F, 0, -2F, 3.5F, 10F, 4F)
                        .texOffs(0, 42).addBox(-1.7F, 8.5F, -3.1F, 4F, 1.5F, 5F),
                PartPose.offset(2.1F, 14.2F, 0));
        // Thin raised silk rods read as the four articulated suspension strings.
        root.addOrReplaceChild("strings", CubeListBuilder.create()
                        .texOffs(50, 18).addBox(-4.6F, -4.8F, 0.2F, .5F, 10F, .5F)
                        .texOffs(50, 18).addBox(4.1F, -4.8F, 0.2F, .5F, 10F, .5F)
                        .texOffs(52, 18).addBox(-2.4F, -5.8F, 0, .5F, 7F, .5F)
                        .texOffs(52, 18).addBox(1.9F, -5.8F, 0, .5F, 7F, .5F),
                PartPose.offset(0, 7F, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(PuppetEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float walk = Math.min(1.0F, limbSwingAmount);
        leftLeg.xRot = Mth.cos(limbSwing * .6662F) * 1.2F * walk;
        rightLeg.xRot = Mth.cos(limbSwing * .6662F + Mth.PI) * 1.2F * walk;
        rightArm.xRot = Mth.cos(limbSwing * .6662F + Mth.PI) * .75F * walk
                - entity.getAttackAnim(0.0F) * 1.3F;
        leftArm.xRot = Mth.cos(limbSwing * .6662F) * .75F * walk;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * .5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * .5F;
        strings.zRot = Mth.sin(ageInTicks * .09F) * .015F;
        rightArm.yRot = 0;
        leftArm.yRot = 0;
        blade.visible = entity.deviceIndex() == 0;
        launcher.visible = entity.deviceIndex() == 1;
        shield.visible = entity.deviceIndex() == 2;
        chestBrace.visible = entity.frameIndex() == 2;
        if (entity.deviceIndex() == 1) {
            rightArm.xRot = -.68F + Mth.sin(ageInTicks * .08F) * .025F;
            rightArm.yRot = -.12F;
        } else if (entity.deviceIndex() == 2) {
            leftArm.xRot = -.68F;
            leftArm.yRot = -.25F;
        } else {
            rightArm.yRot = 0;
            leftArm.yRot = 0;
        }
    }
}

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
 * Spider model derived from external vanilla-like references: wider head, visible pedipalps and two-segment legs.
 * No Fresh Animations asset is copied; the Minecraft Spider texture remains the runtime identity.
 */
public final class SpiderVisualModel extends EntityModel<SpiderVisualRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "spider_visual"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart abdomen;
    private final ModelPart rightPedipalp;
    private final ModelPart leftPedipalp;
    private final ModelPart[] upperLegs = new ModelPart[8];
    private final ModelPart[] lowerLegs = new ModelPart[8];

    public SpiderVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        abdomen = root.getChild("abdomen");
        rightPedipalp = head.getChild("right_pedipalp");
        leftPedipalp = head.getChild("left_pedipalp");
        String[] names = {"right_front", "right_mid_front", "right_mid_hind", "right_hind",
                "left_front", "left_mid_front", "left_mid_hind", "left_hind"};
        for (int i = 0; i < names.length; i++) {
            upperLegs[i] = root.getChild(names[i] + "_upper");
            lowerLegs[i] = upperLegs[i].getChild(names[i] + "_lower");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(32, 4).addBox(-4.5F, -4.0F, -8.0F, 9.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, 15.0F, -3.0F));
        head.addOrReplaceChild("right_pedipalp",
                CubeListBuilder.create().texOffs(18, 0).addBox(-2.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(-1.0F, 1.0F, -7.0F));
        head.addOrReplaceChild("left_pedipalp",
                CubeListBuilder.create().texOffs(18, 0).mirror().addBox(0.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(1.0F, 1.0F, -7.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 12).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 12.0F),
                PartPose.offset(0.0F, 15.0F, 9.0F));

        addLeg(root, "right_front", false, -2.5F);
        addLeg(root, "right_mid_front", false, 0.0F);
        addLeg(root, "right_mid_hind", false, 3.0F);
        addLeg(root, "right_hind", false, 5.5F);
        addLeg(root, "left_front", true, -2.5F);
        addLeg(root, "left_mid_front", true, 0.0F);
        addLeg(root, "left_mid_hind", true, 3.0F);
        addLeg(root, "left_hind", true, 5.5F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addLeg(PartDefinition root, String name, boolean left, float z) {
        float rootX = left ? 3.0F : -3.0F;
        float upperX = left ? 0.0F : -7.0F;
        float childX = left ? 7.0F : -7.0F;
        float lowerX = left ? 0.0F : -8.0F;
        CubeListBuilder upperCube = CubeListBuilder.create().texOffs(18, 0);
        CubeListBuilder lowerCube = CubeListBuilder.create().texOffs(18, 0);
        if (left) {
            upperCube = upperCube.mirror();
            lowerCube = lowerCube.mirror();
        }
        PartDefinition upper = root.addOrReplaceChild(name + "_upper",
                upperCube.addBox(upperX, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F),
                PartPose.offset(rootX, 15.0F, z));
        upper.addOrReplaceChild(name + "_lower",
                lowerCube.addBox(lowerX, -1.0F, -1.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offset(childX, 0.0F, 0.0F));
    }

    @Override
    public void setupAnim(SpiderVisualRenderState state) {
        super.setupAnim(state);
        float time = state.presentationTime;
        float crawl = Mth.sin(time * 0.12F);
        float quick = Mth.sin(time * 0.34F);

        head.xRot = 0.0F;
        head.yRot = Mth.sin(time * 0.045F) * 0.055F;
        head.zRot = 0.0F;
        body.xRot = 0.0F;
        body.yRot = 0.0F;
        body.zRot = crawl * 0.012F;
        abdomen.xRot = -0.025F + crawl * 0.012F;
        abdomen.yRot = 0.0F;
        abdomen.zRot = -crawl * 0.010F;
        rightPedipalp.xRot = 0.08F;
        leftPedipalp.xRot = 0.08F;
        rightPedipalp.yRot = -0.08F;
        leftPedipalp.yRot = 0.08F;
        resetLegs(crawl);

        TurnboundPresentationPose pose = state.presentationPose == null
                ? TurnboundPresentationPose.NEUTRAL
                : state.presentationPose;
        switch (pose) {
            case OFFENSIVE -> {
                // Fang: fast head-led snap with front legs reaching to pin the target line.
                head.xRot = 0.18F;
                body.xRot = -0.10F;
                rightPedipalp.xRot = 0.34F;
                leftPedipalp.xRot = 0.34F;
                upperLegs[0].yRot -= 0.24F;
                upperLegs[4].yRot += 0.24F;
                lowerLegs[0].zRot -= 0.18F;
                lowerLegs[4].zRot += 0.18F;
            }
            case VENOM -> {
                // Venom Bite: mouth parts open wider and the head stays low for a distinct committed bite.
                head.xRot = 0.30F + quick * 0.025F;
                body.xRot = 0.08F;
                rightPedipalp.xRot = 0.44F;
                leftPedipalp.xRot = 0.44F;
                rightPedipalp.yRot = -0.24F;
                leftPedipalp.yRot = 0.24F;
                upperLegs[1].zRot -= 0.10F;
                upperLegs[5].zRot += 0.10F;
            }
            case WEB -> {
                // Binding Web: abdomen lifts while the front half anchors, making the projectile read as silk rather than an arrow shot.
                abdomen.xRot = -0.30F;
                body.xRot = 0.08F;
                head.xRot = -0.04F;
                upperLegs[0].yRot -= 0.16F;
                upperLegs[4].yRot += 0.16F;
                upperLegs[3].zRot -= 0.13F;
                upperLegs[7].zRot += 0.13F;
            }
            case POUNCE -> {
                // Burst: all eight legs coil under the body before the stage motion carries the leap.
                head.xRot = 0.20F;
                body.xRot = 0.18F;
                abdomen.xRot = 0.12F;
                for (int i = 0; i < 4; i++) {
                    upperLegs[i].zRot += 0.20F;
                    upperLegs[i + 4].zRot -= 0.20F;
                    lowerLegs[i].zRot += 0.16F;
                    lowerLegs[i + 4].zRot -= 0.16F;
                }
            }
            default -> {
                // Other pose families belong to other presentation entities.
            }
        }
    }

    private void resetLegs(float crawl) {
        float[] rightY = {-0.72F, -0.28F, 0.28F, 0.72F};
        float[] rightZ = {-0.58F, -0.72F, -0.72F, -0.58F};
        for (int i = 0; i < 4; i++) {
            float step = crawl * (i % 2 == 0 ? 0.055F : -0.055F);
            upperLegs[i].xRot = 0.0F;
            upperLegs[i].yRot = rightY[i] + step;
            upperLegs[i].zRot = rightZ[i];
            lowerLegs[i].xRot = 0.0F;
            lowerLegs[i].yRot = 0.0F;
            lowerLegs[i].zRot = -0.26F;

            int left = i + 4;
            upperLegs[left].xRot = 0.0F;
            upperLegs[left].yRot = -rightY[i] - step;
            upperLegs[left].zRot = -rightZ[i];
            lowerLegs[left].xRot = 0.0F;
            lowerLegs[left].yRot = 0.0F;
            lowerLegs[left].zRot = 0.26F;
        }
    }
}

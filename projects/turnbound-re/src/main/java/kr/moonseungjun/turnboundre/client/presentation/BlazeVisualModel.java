package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
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
 * TURNBOUND Blaze presentation model.
 *
 * The vanilla Blaze texture is reused, but the geometry and animation are authored here so the character reads as
 * a controlled fire striker rather than a stock mob: three counter-moving rod tiers orbit a compact core at idle,
 * then compress into a forward furnace-cage silhouette during an authored offensive FIRE beat.
 */
public final class BlazeVisualModel extends EntityModel<BlazeVisualRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "blaze_visual"), "main");
    private static final int ROD_COUNT = 12;
    private static final float HALF_PI = (float) (Math.PI * 0.5D);

    private final ModelPart head;
    private final ModelPart[] rods = new ModelPart[ROD_COUNT];

    public BlazeVisualModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        for (int i = 0; i < ROD_COUNT; i++) {
            rods[i] = root.getChild("rod_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        for (int i = 0; i < ROD_COUNT; i++) {
            int tier = i / 4;
            float height = tier == 0 ? 9.0F : tier == 1 ? 8.0F : 7.0F;
            root.addOrReplaceChild("rod_" + i,
                    CubeListBuilder.create().texOffs(0, 16)
                            .addBox(-1.0F, -height * 0.5F, -1.0F, 2.0F, height, 2.0F),
                    PartPose.ZERO);
        }

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(BlazeVisualRenderState state) {
        super.setupAnim(state);

        float time = state.presentationTime;
        float breathe = Mth.sin(time * 0.075F);
        float secondary = Mth.sin(time * 0.043F + 1.2F);

        head.x = 0.0F;
        head.y = 8.0F + breathe * 0.22F;
        head.z = state.aggressive ? -0.55F : 0.0F;
        head.xRot = state.aggressive ? -0.10F : secondary * 0.018F;
        head.yRot = state.aggressive ? secondary * 0.035F : secondary * 0.055F;
        head.zRot = 0.0F;

        for (int i = 0; i < ROD_COUNT; i++) {
            ModelPart rod = rods[i];
            int tier = i / 4;
            int lane = i % 4;
            if (state.aggressive) {
                setupAttackRod(rod, tier, lane, time);
            } else {
                setupIdleRod(rod, tier, lane, time);
            }
        }
    }

    private static void setupIdleRod(ModelPart rod, int tier, int lane, float time) {
        float direction = tier == 1 ? -1.0F : 1.0F;
        float speed = 0.040F + tier * 0.009F;
        float phase = tier * 0.43F;
        float angle = direction * time * speed + lane * HALF_PI + phase;
        float radius = switch (tier) {
            case 0 -> 6.9F;
            case 1 -> 5.5F;
            default -> 4.3F;
        };
        float baseY = switch (tier) {
            case 0 -> 4.7F;
            case 1 -> 10.6F;
            default -> 16.4F;
        };

        rod.x = Mth.cos(angle) * radius;
        rod.y = baseY + Mth.sin(time * 0.082F + lane * 0.9F + tier) * 0.38F;
        rod.z = Mth.sin(angle) * radius;
        rod.xRot = Mth.sin(angle) * (0.11F + tier * 0.025F);
        rod.yRot = -angle * 0.22F;
        rod.zRot = Mth.cos(angle) * (0.16F - tier * 0.02F);
    }

    private static void setupAttackRod(ModelPart rod, int tier, int lane, float time) {
        float pulse = Mth.sin(time * 0.29F + tier * 0.7F) * 0.18F;
        float laneX = (lane - 1.5F) * 2.15F;
        float baseY = 4.5F + tier * 5.2F;

        // Four vertical lanes close around the core and push forward, producing a readable firing silhouette.
        rod.x = laneX * (0.88F - tier * 0.05F);
        rod.y = baseY + ((lane & 1) == 0 ? -0.30F : 0.30F) + pulse;
        rod.z = -4.1F + tier * 0.85F - Math.abs(lane - 1.5F) * 0.30F;
        rod.xRot = -0.30F + tier * 0.07F;
        rod.yRot = 0.0F;
        rod.zRot = (lane - 1.5F) * 0.085F;
    }
}

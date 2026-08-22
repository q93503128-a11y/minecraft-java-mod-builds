package kr.moonseungjun.frontiersettlement.client;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.List;

public final class RoadGhostRenderer {
    private static final ContextKey<GhostState> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "road_placement_ghost"));
    private static final int VALID_COLOR = 0xD04ED879;
    private static final int INVALID_COLOR = 0xD0E64B4B;

    private RoadGhostRenderer() {}

    public static void extract(ExtractLevelRenderStateEvent event) {
        if (!RoadPlacementClient.active() || RoadPlacementClient.start() == null) return;
        List<BlockPos> blocks = RoadPlacementClient.ghostBlocks();
        if (blocks.isEmpty()) return;
        event.getRenderState().setRenderData(DATA_KEY,
                new GhostState(blocks, RoadPlacementClient.ghostValid()));
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        GhostState state = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (state == null || state.blocks().isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        int color = state.valid() ? VALID_COLOR : INVALID_COLOR;
        float lineWidth = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;

        for (BlockPos pos : state.blocks()) {
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            event.getSubmitNodeCollector().submitShapeOutline(
                    poseStack, Shapes.block(), RenderTypes.lines(), color, lineWidth, false);
            poseStack.popPose();
        }
    }

    private record GhostState(List<BlockPos> blocks, boolean valid) {}
}

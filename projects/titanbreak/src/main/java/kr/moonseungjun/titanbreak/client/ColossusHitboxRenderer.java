package kr.moonseungjun.titanbreak.client;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.entity.HollowColossusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.List;

public final class ColossusHitboxRenderer {
    private static final ContextKey<List<AABB>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "colossus_part_hitboxes"));

    private ColossusHitboxRenderer() {}

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) return;

        List<AABB> boxes = new ArrayList<>();
        for (Entity entity : event.getLevel().entitiesForRendering()) {
            if (!(entity instanceof HollowColossusEntity colossus)) continue;
            for (PartEntity<?> part : colossus.getParts()) {
                if (part.isPickable()) boxes.add(part.getBoundingBox());
            }
        }
        if (!boxes.isEmpty()) event.getRenderState().setRenderData(DATA_KEY, boxes);
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<AABB> boxes = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (boxes == null || boxes.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float lineWidth = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;

        for (AABB worldBox : boxes) {
            AABB box = worldBox.move(-camera.x, -camera.y, -camera.z);
            event.getSubmitNodeCollector().submitShapeOutline(
                    poseStack,
                    Shapes.create(box),
                    RenderTypes.lines(),
                    0xFF00FFFF,
                    lineWidth,
                    false);
        }
    }
}

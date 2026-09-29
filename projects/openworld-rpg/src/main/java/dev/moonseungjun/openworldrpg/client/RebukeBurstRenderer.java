package dev.moonseungjun.openworldrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.moonseungjun.openworldrpg.combat.runtime.RebukeBurstShape;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Short-lived 3D presentation for Cleric Rebuke.
 *
 * <p>The outer light volume and inner magic ribs share the exact server-side shape constants. Kenney
 * textures are support material; the authored 3D fan geometry provides the attack silhouette.</p>
 */
public final class RebukeBurstRenderer {
    public static final String MODEL_ID =
            "openworld_rpg:spell_effect/rebuke_burst";

    private static final Identifier LIGHT_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "openworld_rpg",
                    "textures/spell/rebuke_light_kenney.png"
            );
    private static final Identifier MAGIC_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "openworld_rpg",
                    "textures/spell/rebuke_magic_kenney.png"
            );
    private static final RenderType LIGHT_LAYER =
            RenderTypes.entityTranslucent(LIGHT_TEXTURE);
    private static final RenderType MAGIC_LAYER =
            RenderTypes.entityTranslucent(MAGIC_TEXTURE);
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float DURATION_TICKS = 8.0F;

    private RebukeBurstRenderer() {
    }

    public static boolean renderIfOwned(
            Object renderState,
            PoseStack matrices,
            SubmitNodeCollector queue
    ) {
        try {
            Object rawEntity = renderState.getClass()
                    .getField("entity")
                    .get(renderState);
            if (!(rawEntity instanceof Entity entity)) {
                return false;
            }

            Object effect = entity.getClass()
                    .getMethod("getModelEffect")
                    .invoke(entity);
            if (effect == null) {
                return false;
            }
            Object modelId = effect.getClass()
                    .getField("model_id")
                    .get(effect);
            if (!MODEL_ID.equals(String.valueOf(modelId))) {
                return false;
            }

            float tickDelta = ((Number) renderState.getClass()
                    .getField("tickDelta")
                    .get(renderState)).floatValue();
            float age = entity.tickCount + tickDelta;
            float growth = easeOutCubic(
                    Math.min(1.0F, age / 2.25F)
            );
            float fade = Math.max(
                    0.0F,
                    1.0F - age / DURATION_TICKS
            );

            matrices.pushPose();
            matrices.mulPose(
                    Axis.YP.rotationDegrees(-entity.getYRot())
            );

            int outerAlpha = Math.max(
                    0,
                    Math.min(255, Math.round(150.0F * fade))
            );
            int innerAlpha = Math.max(
                    0,
                    Math.min(255, Math.round(210.0F * fade))
            );

            queue.submitCustomGeometry(
                    matrices,
                    LIGHT_LAYER,
                    (entry, vertices) -> emitOuterFan(
                            entry,
                            vertices,
                            growth,
                            outerAlpha
                    )
            );
            queue.submitCustomGeometry(
                    matrices,
                    MAGIC_LAYER,
                    (entry, vertices) -> emitInnerRibs(
                            entry,
                            vertices,
                            growth,
                            innerAlpha
                    )
            );
            matrices.popPose();
            return true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine Rebuke model-effect render contract is unavailable.",
                    exception
            );
        }
    }

    private static void emitOuterFan(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float growth,
            int alpha
    ) {
        float nearZ = 0.20F;
        float farZ = (float) (
                RebukeBurstShape.RANGE_BLOCKS * growth
        );
        float nearHalf = (float) (
                RebukeBurstShape.NEAR_HALF_WIDTH_BLOCKS
                        * (0.65 + 0.35 * growth)
        );
        float farHalf = (float) (
                RebukeBurstShape.NEAR_HALF_WIDTH_BLOCKS
                        + (RebukeBurstShape.FAR_HALF_WIDTH_BLOCKS
                                - RebukeBurstShape.NEAR_HALF_WIDTH_BLOCKS)
                                * growth
        );

        int color = argb(alpha, 0xFFF1B8);
        quad(
                entry,
                vertices,
                -nearHalf, 0.05F, nearZ,
                nearHalf, 0.05F, nearZ,
                farHalf, 0.05F, farZ,
                -farHalf, 0.05F, farZ,
                0.0F, 1.0F, 0.0F,
                color
        );
        quad(
                entry,
                vertices,
                -nearHalf, 0.46F, nearZ,
                nearHalf, 0.46F, nearZ,
                farHalf, 0.46F, farZ,
                -farHalf, 0.46F, farZ,
                0.0F, 1.0F, 0.0F,
                argb(Math.max(0, alpha - 25), 0xFFF6CB)
        );
        quad(
                entry,
                vertices,
                -nearHalf, 0.92F, nearZ,
                nearHalf, 0.92F, nearZ,
                farHalf, 0.92F, farZ,
                -farHalf, 0.92F, farZ,
                0.0F, 1.0F, 0.0F,
                argb(Math.max(0, alpha - 55), 0xFFFFFF)
        );

        quad(
                entry,
                vertices,
                -nearHalf, -0.10F, nearZ,
                -nearHalf, 0.78F, nearZ,
                -farHalf, 1.18F, farZ,
                -farHalf, -0.10F, farZ,
                -1.0F, 0.0F, 0.0F,
                argb(Math.max(0, alpha - 30), 0xFFECA8)
        );
        quad(
                entry,
                vertices,
                nearHalf, 0.78F, nearZ,
                nearHalf, -0.10F, nearZ,
                farHalf, -0.10F, farZ,
                farHalf, 1.18F, farZ,
                1.0F, 0.0F, 0.0F,
                argb(Math.max(0, alpha - 30), 0xFFECA8)
        );

        quad(
                entry,
                vertices,
                -farHalf, -0.08F, farZ,
                farHalf, -0.08F, farZ,
                farHalf, 1.16F, farZ,
                -farHalf, 1.16F, farZ,
                0.0F, 0.0F, 1.0F,
                argb(Math.max(0, alpha - 15), 0xFFFFDB)
        );
    }

    private static void emitInnerRibs(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float growth,
            int alpha
    ) {
        float nearZ = 0.22F;
        float farZ = (float) (
                RebukeBurstShape.RANGE_BLOCKS * growth
        );
        float farHalf = (float) (
                RebukeBurstShape.FAR_HALF_WIDTH_BLOCKS
                        * growth
        );

        emitRibbon(
                entry,
                vertices,
                0.0F,
                farZ,
                0.18F,
                0.20F,
                1.16F,
                argb(alpha, 0xFFFFFF)
        );
        emitRibbon(
                entry,
                vertices,
                -farHalf * 0.58F,
                farZ,
                0.11F,
                0.12F,
                0.92F,
                argb(Math.max(0, alpha - 35), 0xFFF0A8)
        );
        emitRibbon(
                entry,
                vertices,
                farHalf * 0.58F,
                farZ,
                0.11F,
                0.12F,
                0.92F,
                argb(Math.max(0, alpha - 35), 0xFFF0A8)
        );

        // A low center sheet makes the release origin readable without turning the skill into
        // particle fog.
        quad(
                entry,
                vertices,
                -0.22F, 0.18F, nearZ,
                0.22F, 0.18F, nearZ,
                0.52F, 0.18F, farZ,
                -0.52F, 0.18F, farZ,
                0.0F, 1.0F, 0.0F,
                argb(Math.max(0, alpha - 45), 0xFFFFD2)
        );
    }

    private static void emitRibbon(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float farX,
            float farZ,
            float nearHalfWidth,
            float minY,
            float maxY,
            int color
    ) {
        float nearZ = 0.24F;
        float farHalfWidth = nearHalfWidth * 1.7F;
        quad(
                entry,
                vertices,
                -nearHalfWidth, minY, nearZ,
                nearHalfWidth, minY, nearZ,
                farX + farHalfWidth, maxY, farZ,
                farX - farHalfWidth, maxY, farZ,
                0.0F, 0.0F, 1.0F,
                color
        );
    }

    private static void quad(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float nx,
            float ny,
            float nz,
            int color
    ) {
        vertex(entry, vertices, x0, y0, z0, 0.0F, 0.0F, nx, ny, nz, color);
        vertex(entry, vertices, x1, y1, z1, 1.0F, 0.0F, nx, ny, nz, color);
        vertex(entry, vertices, x2, y2, z2, 1.0F, 1.0F, nx, ny, nz, color);
        vertex(entry, vertices, x3, y3, z3, 0.0F, 1.0F, nx, ny, nz, color);
    }

    private static void vertex(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int color
    ) {
        vertices.addVertex(entry, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(entry, nx, ny, nz);
    }

    private static float easeOutCubic(float t) {
        float remaining = 1.0F - t;
        return 1.0F - remaining * remaining * remaining;
    }

    private static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}

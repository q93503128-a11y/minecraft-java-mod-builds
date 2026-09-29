package dev.moonseungjun.openworldrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.moonseungjun.openworldrpg.combat.runtime.SanctuaryRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.SanctuaryZoneShape;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Authored 3D presentation for Cleric Sanctuary.
 *
 * <p>The floor ward uses the exact authoritative seven-block radius. Kenney textures remain support
 * material; the project-authored ring, radial seals and vertical light ribs define the effect.</p>
 */
public final class SanctuaryWardRenderer {
    public static final String MODEL_ID =
            "openworld_rpg:spell_effect/sanctuary_ward";

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
    private static final int SEGMENTS = 48;

    private SanctuaryWardRenderer() {
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
            float fadeIn = Math.min(1.0F, age / 8.0F);
            float fadeOut = Math.min(
                    1.0F,
                    Math.max(
                            0.0F,
                            (SanctuaryRuntime.DURATION_TICKS - age) / 12.0F
                    )
            );
            float visibility = fadeIn * fadeOut;
            float breathing = 0.94F
                    + 0.06F * (float) Math.sin(age * 0.22F);

            int floorAlpha = alpha(155.0F * visibility);
            int sigilAlpha = alpha(205.0F * visibility);
            int pillarAlpha = alpha(125.0F * visibility);

            matrices.pushPose();
            queue.submitCustomGeometry(
                    matrices,
                    LIGHT_LAYER,
                    (entry, vertices) -> emitWardBoundary(
                            entry,
                            vertices,
                            breathing,
                            floorAlpha,
                            pillarAlpha
                    )
            );
            queue.submitCustomGeometry(
                    matrices,
                    MAGIC_LAYER,
                    (entry, vertices) -> emitInnerSeal(
                            entry,
                            vertices,
                            breathing,
                            sigilAlpha
                    )
            );
            matrices.popPose();
            return true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine Sanctuary model-effect render contract is unavailable.",
                    exception
            );
        }
    }

    private static void emitWardBoundary(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float breathing,
            int floorAlpha,
            int pillarAlpha
    ) {
        float radius = (float) SanctuaryZoneShape.RADIUS_BLOCKS;
        emitRing(
                entry,
                vertices,
                radius - 0.34F,
                radius,
                0.035F,
                argb(floorAlpha, 0xFFF0A8)
        );

        float ribRadius = radius - 0.28F;
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0 * i / 12.0;
            float cx = (float) Math.cos(angle) * ribRadius;
            float cz = (float) Math.sin(angle) * ribRadius;
            float tx = (float) -Math.sin(angle);
            float tz = (float) Math.cos(angle);
            float halfWidth = 0.24F * breathing;
            float topLean = 0.22F;

            doubleSidedQuad(
                    entry,
                    vertices,
                    cx - tx * halfWidth,
                    0.04F,
                    cz - tz * halfWidth,
                    cx + tx * halfWidth,
                    0.04F,
                    cz + tz * halfWidth,
                    cx + tx * halfWidth - cx / ribRadius * topLean,
                    3.25F,
                    cz + tz * halfWidth - cz / ribRadius * topLean,
                    cx - tx * halfWidth - cx / ribRadius * topLean,
                    3.25F,
                    cz - tz * halfWidth - cz / ribRadius * topLean,
                    argb(pillarAlpha, i % 2 == 0 ? 0xFFF8D6 : 0xFFE49B)
            );
        }

        // A restrained central vertical cross makes the ward readable from a distance without
        // filling the whole volume with particles.
        doubleSidedQuad(
                entry,
                vertices,
                -0.34F, 0.05F, 0.0F,
                0.34F, 0.05F, 0.0F,
                0.18F, 4.10F, 0.0F,
                -0.18F, 4.10F, 0.0F,
                argb(Math.max(0, pillarAlpha - 20), 0xFFFFFF)
        );
        doubleSidedQuad(
                entry,
                vertices,
                0.0F, 0.05F, -0.34F,
                0.0F, 0.05F, 0.34F,
                0.0F, 4.10F, 0.18F,
                0.0F, 4.10F, -0.18F,
                argb(Math.max(0, pillarAlpha - 20), 0xFFFFFF)
        );
    }

    private static void emitInnerSeal(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float breathing,
            int alpha
    ) {
        float outer = (float) SanctuaryZoneShape.RADIUS_BLOCKS * 0.72F;
        emitRing(
                entry,
                vertices,
                outer - 0.18F,
                outer + 0.18F,
                0.045F,
                argb(alpha, 0xFFFFFF)
        );

        float spokeLength = outer * 0.90F * breathing;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            float dx = (float) Math.cos(angle);
            float dz = (float) Math.sin(angle);
            float tx = -dz;
            float tz = dx;
            float halfWidth = 0.065F;
            quad(
                    entry,
                    vertices,
                    tx * halfWidth,
                    0.050F,
                    tz * halfWidth,
                    -tx * halfWidth,
                    0.050F,
                    -tz * halfWidth,
                    dx * spokeLength - tx * halfWidth,
                    0.050F,
                    dz * spokeLength - tz * halfWidth,
                    dx * spokeLength + tx * halfWidth,
                    0.050F,
                    dz * spokeLength + tz * halfWidth,
                    0.0F,
                    1.0F,
                    0.0F,
                    argb(Math.max(0, alpha - 35), 0xFFF0B8)
            );
        }
    }

    private static void emitRing(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float innerRadius,
            float outerRadius,
            float y,
            int color
    ) {
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / SEGMENTS;
            float ix0 = (float) Math.cos(a0) * innerRadius;
            float iz0 = (float) Math.sin(a0) * innerRadius;
            float ox0 = (float) Math.cos(a0) * outerRadius;
            float oz0 = (float) Math.sin(a0) * outerRadius;
            float ix1 = (float) Math.cos(a1) * innerRadius;
            float iz1 = (float) Math.sin(a1) * innerRadius;
            float ox1 = (float) Math.cos(a1) * outerRadius;
            float oz1 = (float) Math.sin(a1) * outerRadius;

            quad(
                    entry,
                    vertices,
                    ix0, y, iz0,
                    ox0, y, oz0,
                    ox1, y, oz1,
                    ix1, y, iz1,
                    0.0F, 1.0F, 0.0F,
                    color
            );
        }
    }

    private static void doubleSidedQuad(
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
            int color
    ) {
        quad(
                entry,
                vertices,
                x0, y0, z0,
                x1, y1, z1,
                x2, y2, z2,
                x3, y3, z3,
                0.0F, 0.0F, 1.0F,
                color
        );
        quad(
                entry,
                vertices,
                x3, y3, z3,
                x2, y2, z2,
                x1, y1, z1,
                x0, y0, z0,
                0.0F, 0.0F, -1.0F,
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

    private static int alpha(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}

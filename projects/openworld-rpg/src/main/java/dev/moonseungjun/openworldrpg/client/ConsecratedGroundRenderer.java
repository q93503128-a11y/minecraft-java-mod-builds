package dev.moonseungjun.openworldrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.moonseungjun.openworldrpg.combat.runtime.ConsecratedGroundRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ConsecratedGroundZoneShape;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Authored 3D presentation for Cleric Consecrated Ground.
 *
 * <p>The outer ring uses the exact authoritative five-block radius. Low vertical glyphs and a
 * separate inner seal keep the field readable without turning the ordinary active into a smaller
 * copy of Sanctuary.</p>
 */
public final class ConsecratedGroundRenderer {
    public static final String MODEL_ID =
            "openworld_rpg:spell_effect/consecrated_ground";

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
    private static final int SEGMENTS = 40;

    private ConsecratedGroundRenderer() {
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
            float fadeIn = Math.min(1.0F, age / 5.0F);
            float fadeOut = Math.min(
                    1.0F,
                    Math.max(
                            0.0F,
                            (ConsecratedGroundRuntime.DURATION_TICKS - age)
                                    / 8.0F
                    )
            );
            float visibility = fadeIn * fadeOut;
            float pulse = 0.96F
                    + 0.04F * (float) Math.sin(age * 0.30F);

            matrices.pushPose();
            queue.submitCustomGeometry(
                    matrices,
                    LIGHT_LAYER,
                    (entry, vertices) -> emitBoundary(
                            entry,
                            vertices,
                            pulse,
                            alpha(165.0F * visibility),
                            alpha(105.0F * visibility)
                    )
            );
            queue.submitCustomGeometry(
                    matrices,
                    MAGIC_LAYER,
                    (entry, vertices) -> emitSeal(
                            entry,
                            vertices,
                            pulse,
                            alpha(190.0F * visibility)
                    )
            );
            matrices.popPose();
            return true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine Consecrated Ground model-effect render contract is unavailable.",
                    exception
            );
        }
    }

    private static void emitBoundary(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float pulse,
            int ringAlpha,
            int glyphAlpha
    ) {
        float radius = (float) ConsecratedGroundZoneShape.RADIUS_BLOCKS;
        emitRing(
                entry,
                vertices,
                radius - 0.26F,
                radius,
                0.035F,
                argb(ringAlpha, 0xFFEAA0)
        );

        float glyphRadius = radius * 0.74F;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            float cx = (float) Math.cos(angle) * glyphRadius;
            float cz = (float) Math.sin(angle) * glyphRadius;
            float tx = (float) -Math.sin(angle);
            float tz = (float) Math.cos(angle);
            float halfWidth = 0.18F * pulse;
            float height = i % 2 == 0 ? 1.15F : 0.78F;

            doubleSidedQuad(
                    entry,
                    vertices,
                    cx - tx * halfWidth, 0.04F, cz - tz * halfWidth,
                    cx + tx * halfWidth, 0.04F, cz + tz * halfWidth,
                    cx + tx * halfWidth, height, cz + tz * halfWidth,
                    cx - tx * halfWidth, height, cz - tz * halfWidth,
                    argb(glyphAlpha, i % 2 == 0 ? 0xFFF4C1 : 0xFFE2A0)
            );
        }
    }

    private static void emitSeal(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float pulse,
            int sealAlpha
    ) {
        float inner = (float) ConsecratedGroundZoneShape.RADIUS_BLOCKS
                * 0.53F * pulse;
        emitRing(
                entry,
                vertices,
                inner - 0.16F,
                inner + 0.16F,
                0.045F,
                argb(sealAlpha, 0xFFFFFF)
        );

        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 0.5 * i;
            float dx = (float) Math.cos(angle);
            float dz = (float) Math.sin(angle);
            float tx = -dz;
            float tz = dx;
            float start = inner * 0.20F;
            float end = inner * 0.92F;
            float halfWidth = 0.075F;
            quad(
                    entry,
                    vertices,
                    dx * start + tx * halfWidth, 0.050F,
                    dz * start + tz * halfWidth,
                    dx * start - tx * halfWidth, 0.050F,
                    dz * start - tz * halfWidth,
                    dx * end - tx * halfWidth, 0.050F,
                    dz * end - tz * halfWidth,
                    dx * end + tx * halfWidth, 0.050F,
                    dz * end + tz * halfWidth,
                    0.0F, 1.0F, 0.0F,
                    argb(Math.max(0, sealAlpha - 30), 0xFFF0B5)
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

package dev.moonseungjun.openworldrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.moonseungjun.openworldrpg.combat.runtime.WarriorSkillShape;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Project-authored 3D silhouettes for Warrior root actions.
 *
 * <p>Kenney slash_01 is only the support material; range/radius geometry is authored here from the
 * same constants used by server hit acceptance.</p>
 */
public final class WarriorSkillRenderer {
    public static final String DRIVING_SLASH_MODEL =
            "openworld_rpg:spell_effect/warrior_driving_slash";
    public static final String IRON_COUNTER_MODEL =
            "openworld_rpg:spell_effect/warrior_iron_counter";
    public static final String CYCLONE_CUT_MODEL =
            "openworld_rpg:spell_effect/warrior_cyclone_cut";
    public static final String BREAKER_SLAM_MODEL =
            "openworld_rpg:spell_effect/warrior_breaker_slam";
    public static final String EARTHSHATTER_MODEL =
            "openworld_rpg:spell_effect/warrior_earthshatter";

    private static final Identifier SLASH_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "openworld_rpg",
                    "textures/spell/warrior_slash_kenney.png"
            );
    private static final RenderType LAYER =
            RenderTypes.entityTranslucent(SLASH_TEXTURE);
    private static final int FULL_BRIGHT = 0xF000F0;

    private WarriorSkillRenderer() {
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
            String modelId = String.valueOf(
                    effect.getClass()
                            .getField("model_id")
                            .get(effect)
            );
            if (!owned(modelId)) {
                return false;
            }

            float tickDelta = ((Number) renderState.getClass()
                    .getField("tickDelta")
                    .get(renderState)).floatValue();
            float age = entity.tickCount + tickDelta;
            float duration = duration(modelId);
            float growth = easeOutCubic(
                    Math.min(1.0F, age / 3.0F)
            );
            float fade = Math.max(
                    0.0F,
                    1.0F - age / duration
            );
            int alpha = Math.max(
                    0,
                    Math.min(255, Math.round(220.0F * fade))
            );

            matrices.pushPose();
            matrices.mulPose(
                    Axis.YP.rotationDegrees(-entity.getYRot())
            );
            if (CYCLONE_CUT_MODEL.equals(modelId)) {
                matrices.mulPose(
                        Axis.YP.rotationDegrees(age * 31.0F)
                );
            }
            queue.submitCustomGeometry(
                    matrices,
                    LAYER,
                    (entry, vertices) -> emit(
                            modelId,
                            entry,
                            vertices,
                            growth,
                            alpha
                    )
            );
            matrices.popPose();
            return true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine Warrior model-effect render contract is unavailable.",
                    exception
            );
        }
    }

    private static boolean owned(String modelId) {
        return DRIVING_SLASH_MODEL.equals(modelId)
                || IRON_COUNTER_MODEL.equals(modelId)
                || CYCLONE_CUT_MODEL.equals(modelId)
                || BREAKER_SLAM_MODEL.equals(modelId)
                || EARTHSHATTER_MODEL.equals(modelId);
    }

    private static float duration(String modelId) {
        if (IRON_COUNTER_MODEL.equals(modelId)) {
            return 10.0F;
        }
        if (CYCLONE_CUT_MODEL.equals(modelId)) {
            return 12.0F;
        }
        if (BREAKER_SLAM_MODEL.equals(modelId)) {
            return 14.0F;
        }
        if (EARTHSHATTER_MODEL.equals(modelId)) {
            return 20.0F;
        }
        return 9.0F;
    }

    private static void emit(
            String modelId,
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float growth,
            int alpha
    ) {
        if (DRIVING_SLASH_MODEL.equals(modelId)) {
            emitDrivingSlash(entry, vertices, growth, alpha);
        } else if (IRON_COUNTER_MODEL.equals(modelId)) {
            emitIronCounter(entry, vertices, growth, alpha);
        } else if (CYCLONE_CUT_MODEL.equals(modelId)) {
            emitCyclone(entry, vertices, growth, alpha);
        } else if (BREAKER_SLAM_MODEL.equals(modelId)) {
            emitBreaker(entry, vertices, growth, alpha);
        } else if (EARTHSHATTER_MODEL.equals(modelId)) {
            emitEarthshatter(entry, vertices, growth, alpha);
        }
    }

    private static void emitDrivingSlash(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float growth,
            int alpha
    ) {
        float far = (float) WarriorSkillShape
                .DRIVING_SLASH_RANGE * growth;
        float half = 1.65F * growth;
        doubleSided(
                entry,
                vertices,
                -0.18F, 0.15F, 0.18F,
                0.18F, 1.65F, 0.32F,
                half, 1.15F, far,
                -half, 0.02F, far,
                argb(alpha, 0xFFF0C8)
        );
        doubleSided(
                entry,
                vertices,
                -0.12F, 0.10F, 0.25F,
                0.12F, 1.28F, 0.34F,
                half * 0.72F, 0.88F, far * 0.94F,
                -half * 0.72F, 0.04F, far * 0.94F,
                argb(Math.max(0, alpha - 55), 0xFFFFFF)
        );
    }

    private static void emitIronCounter(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float growth,
            int alpha
    ) {
        float r = 1.15F * growth;
        for (int i = 0; i < 2; i++) {
            float sign = i == 0 ? 1.0F : -1.0F;
            doubleSided(
                    entry,
                    vertices,
                    -r, 0.05F, sign * 0.05F,
                    r, 1.90F, sign * 0.05F,
                    r * 0.76F, 2.05F, sign * 0.11F,
                    -r * 0.76F, 0.20F, sign * 0.11F,
                    argb(alpha, i == 0 ? 0xFFE2A0 : 0xFFFFFF)
            );
        }
    }

    private static void emitCyclone(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float growth,
            int alpha
    ) {
        float r = (float) WarriorSkillShape.CYCLONE_RADIUS
                * growth;
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8.0;
            double b = a + Math.PI / 5.0;
            float x0 = (float) Math.sin(a) * r * 0.42F;
            float z0 = (float) Math.cos(a) * r * 0.42F;
            float x1 = (float) Math.sin(b) * r;
            float z1 = (float) Math.cos(b) * r;
            float width = 0.34F;
            doubleSided(
                    entry,
                    vertices,
                    x0 - width, 0.18F, z0,
                    x0 + width, 1.45F, z0,
                    x1 + width, 1.06F, z1,
                    x1 - width, 0.05F, z1,
                    argb(
                            Math.max(0, alpha - i * 8),
                            i % 2 == 0 ? 0xFFF2C1 : 0xFFD997
                    )
            );
        }
    }

    private static void emitBreaker(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float growth,
            int alpha
    ) {
        float range = (float) WarriorSkillShape
                .BREAKER_SLAM_RANGE * growth;
        emitGroundSector(
                entry,
                vertices,
                range,
                WarriorSkillShape
                        .BREAKER_SLAM_HALF_ANGLE_DEGREES,
                alpha,
                0xFFD39A
        );
        float half = 2.2F * growth;
        doubleSided(
                entry,
                vertices,
                -half, 0.02F, range,
                half, 0.02F, range,
                half * 0.72F, 1.55F, range,
                -half * 0.72F, 1.55F, range,
                argb(Math.max(0, alpha - 25), 0xFFF4D2)
        );
    }

    private static void emitEarthshatter(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float growth,
            int alpha
    ) {
        float range = (float) WarriorSkillShape
                .EARTHSHATTER_RANGE * growth;
        emitGroundSector(
                entry,
                vertices,
                range,
                WarriorSkillShape
                        .EARTHSHATTER_HALF_ANGLE_DEGREES,
                alpha,
                0xFFC68D
        );

        for (int i = -3; i <= 3; i++) {
            double angle = Math.toRadians(i * 15.0);
            float sx = (float) Math.sin(angle) * 0.30F;
            float sz = (float) Math.cos(angle) * 0.30F;
            float ex = (float) Math.sin(angle) * range;
            float ez = (float) Math.cos(angle) * range;
            float width = 0.12F;
            doubleSided(
                    entry,
                    vertices,
                    sx - width, 0.04F, sz,
                    sx + width, 0.04F, sz,
                    ex + width, 0.20F + 0.08F * Math.abs(i), ez,
                    ex - width, 0.20F + 0.08F * Math.abs(i), ez,
                    argb(
                            Math.max(0, alpha - 25),
                            i == 0 ? 0xFFFFFF : 0xFFE0AD
                    )
            );
        }
    }

    private static void emitGroundSector(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float range,
            double halfAngleDegrees,
            int alpha,
            int rgb
    ) {
        int segments = 10;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.toRadians(
                    -halfAngleDegrees
                            + 2.0 * halfAngleDegrees * i / segments
            );
            double a1 = Math.toRadians(
                    -halfAngleDegrees
                            + 2.0 * halfAngleDegrees * (i + 1) / segments
            );
            float x0 = (float) Math.sin(a0) * range;
            float z0 = (float) Math.cos(a0) * range;
            float x1 = (float) Math.sin(a1) * range;
            float z1 = (float) Math.cos(a1) * range;
            quad(
                    entry,
                    vertices,
                    0.0F, 0.035F, 0.18F,
                    0.0F, 0.035F, 0.18F,
                    x1, 0.035F, z1,
                    x0, 0.035F, z0,
                    0.0F, 1.0F, 0.0F,
                    argb(Math.max(0, alpha - 55), rgb)
            );
        }
    }

    private static void doubleSided(
            PoseStack.Pose entry,
            VertexConsumer vertices,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int color
    ) {
        quad(
                entry, vertices,
                x0, y0, z0,
                x1, y1, z1,
                x2, y2, z2,
                x3, y3, z3,
                0.0F, 0.0F, 1.0F,
                color
        );
        quad(
                entry, vertices,
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
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz,
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
            float x, float y, float z,
            float u, float v,
            float nx, float ny, float nz,
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

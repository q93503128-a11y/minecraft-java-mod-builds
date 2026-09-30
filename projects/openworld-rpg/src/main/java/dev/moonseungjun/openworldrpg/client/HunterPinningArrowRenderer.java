package dev.moonseungjun.openworldrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** KayKit-backed physical arrow body for Hunter Pinning Shot. */
public final class HunterPinningArrowRenderer {
    public static final String MODEL_ID =
            "openworld_rpg:spell_projectile/hunter_pinning_arrow";
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "openworld_rpg",
                    "textures/spell/radiant_lance_kaykit.png"
            );
    private static final RenderType LAYER =
            RenderTypes.entityCutout(TEXTURE);
    private static final float MODEL_SCALE = 1.65F;
    private static final int TINT = 0xFFC8D99A;

    private HunterPinningArrowRenderer() {
    }

    public static boolean renderIfOwned(
            Object renderState,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState
    ) {
        try {
            Object projectile = renderState.getClass()
                    .getField("projectile")
                    .get(renderState);
            if (!(projectile instanceof Entity entity)) {
                return false;
            }
            Object composite = projectile.getClass()
                    .getMethod("renderModels")
                    .invoke(projectile);
            if (composite == null) {
                return false;
            }
            Object rawModels = composite.getClass()
                    .getField("models")
                    .get(composite);
            if (!(rawModels instanceof java.util.List<?> models)
                    || models.isEmpty()) {
                return false;
            }

            boolean owns = false;
            for (Object model : models) {
                Object fx = model.getClass()
                        .getField("fx")
                        .get(model);
                Object modelId = fx.getClass()
                        .getField("model_id")
                        .get(fx);
                if (MODEL_ID.equals(String.valueOf(modelId))) {
                    owns = true;
                    break;
                }
            }
            if (!owns) {
                return false;
            }

            if (entity.tickCount < 2
                    && cameraState.pos.distanceToSqr(
                            entity.position()
                    ) < 12.25) {
                return true;
            }

            Vec3 velocity = entity.getDeltaMovement();
            if (velocity.lengthSqr() > 1.0e-8) {
                velocity = velocity.normalize();
                double yaw = Math.toDegrees(
                        Math.atan2(
                                velocity.x,
                                velocity.z
                        )
                ) + 180.0;
                double pitch = Math.toDegrees(
                        Math.asin(velocity.y)
                );
                matrices.mulPose(
                        Axis.YP.rotationDegrees((float) yaw)
                );
                matrices.mulPose(
                        Axis.XP.rotationDegrees((float) pitch)
                );
                matrices.mulPose(
                        Axis.YP.rotationDegrees(90.0F)
                );
            }
            matrices.scale(
                    MODEL_SCALE,
                    MODEL_SCALE,
                    MODEL_SCALE
            );

            int light = ((Number) renderState.getClass()
                    .getField("lightCoords")
                    .get(renderState)).intValue();
            queue.submitCustomGeometry(
                    matrices,
                    LAYER,
                    (entry, vertices) ->
                            emit(entry, vertices, light)
            );
            return true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine Pinning Shot arrow render contract is unavailable.",
                    exception
            );
        }
    }

    private static void emit(
            PoseStack.Pose entry,
            com.mojang.blaze3d.vertex.VertexConsumer vertices,
            int light
    ) {
        int[] indices = RadiantLanceMeshData.INDICES;
        for (int triangle = 0;
                triangle < indices.length;
                triangle += 3) {
            int a = indices[triangle];
            int b = indices[triangle + 1];
            int c = indices[triangle + 2];
            emitVertex(entry, vertices, a, light);
            emitVertex(entry, vertices, b, light);
            emitVertex(entry, vertices, c, light);
            emitVertex(entry, vertices, c, light);
        }
    }

    private static void emitVertex(
            PoseStack.Pose entry,
            com.mojang.blaze3d.vertex.VertexConsumer vertices,
            int vertex,
            int light
    ) {
        int p = vertex * 3;
        int t = vertex * 2;
        vertices.addVertex(
                        entry,
                        RadiantLanceMeshData.POSITIONS[p],
                        RadiantLanceMeshData.POSITIONS[p + 1],
                        RadiantLanceMeshData.POSITIONS[p + 2]
                )
                .setColor(TINT)
                .setUv(
                        RadiantLanceMeshData.TEXCOORDS[t],
                        RadiantLanceMeshData.TEXCOORDS[t + 1]
                )
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(
                        entry,
                        RadiantLanceMeshData.NORMALS[p],
                        RadiantLanceMeshData.NORMALS[p + 1],
                        RadiantLanceMeshData.NORMALS[p + 2]
                );
    }
}

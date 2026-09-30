package kr.moonseungjun.campfiresessions.client.resident.mesh;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * GeckoLib bone implementation that preserves arbitrary triangle geometry.
 *
 * <p>Each source glTF primitive keeps its material index. The custom resident
 * renderer submits one render pass per material/texture while reusing the same
 * GeckoLib bone snapshots, so multi-material residents do not need cuboid
 * remeshing or a lossy texture merge.</p>
 */
public final class ResidentMeshBone extends GeoBone {
    private static final float MODEL_PIXELS_PER_BLOCK = 16f;

    private final ResidentMeshPrimitive[] primitives;

    public ResidentMeshBone(
            @Nullable GeoBone parent,
            String name,
            GeoBone[] children,
            GeoLocator[] locators,
            ResidentMeshPrimitive[] primitives
    ) {
        super(parent, name, children, locators, 0, 0, 0, 0, 0, 0);
        this.primitives = primitives.clone();
    }

    public ResidentMeshPrimitive[] primitives() {
        return this.primitives.clone();
    }

    @Override
    public <R extends GeoRenderState> void render(
            RenderPassInfo<R> renderPassInfo,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        if (this.frameSnapshot != null && this.frameSnapshot.isHidden()) {
            return;
        }

        for (ResidentMeshPrimitive primitive : this.primitives) {
            renderPrimitive(primitive, poseStack, vertexConsumer, packedLight, packedOverlay, renderColor);
        }
    }

    public <R extends GeoRenderState> void positionAndRenderMaterial(
            RenderPassInfo<R> renderPassInfo,
            int materialIndex,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        RenderUtil.prepMatrixForBoneAndUpdateListeners(poseStack, this, renderPassInfo);

        renderMaterial(renderPassInfo, materialIndex, poseStack, vertexConsumer, packedLight, packedOverlay, renderColor);

        if (this.frameSnapshot == null || !this.frameSnapshot.areChildrenHidden()) {
            for (GeoBone child : this.children) {
                if (child instanceof ResidentMeshBone meshChild) {
                    meshChild.positionAndRenderMaterial(
                            renderPassInfo,
                            materialIndex,
                            vertexConsumer,
                            packedLight,
                            packedOverlay,
                            renderColor
                    );
                }
            }
        }

        poseStack.popPose();
    }

    private <R extends GeoRenderState> void renderMaterial(
            RenderPassInfo<R> renderPassInfo,
            int materialIndex,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        if (this.frameSnapshot != null && this.frameSnapshot.isHidden()) {
            return;
        }

        for (ResidentMeshPrimitive primitive : this.primitives) {
            if (primitive.materialIndex() == materialIndex) {
                renderPrimitive(primitive, poseStack, vertexConsumer, packedLight, packedOverlay, renderColor);
            }
        }
    }

    private static void renderPrimitive(
            ResidentMeshPrimitive primitive,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalPose = poseStack.last().normal();

        for (ResidentMeshTriangle triangle : primitive.trianglesUnsafe()) {
            emitVertex(triangle.a(), pose, normalPose, vertexConsumer, packedLight, packedOverlay, renderColor);
            emitVertex(triangle.b(), pose, normalPose, vertexConsumer, packedLight, packedOverlay, renderColor);
            emitVertex(triangle.c(), pose, normalPose, vertexConsumer, packedLight, packedOverlay, renderColor);

            // Entity rendering is quad-grouped. Repeating C keeps the second
            // rasterized triangle degenerate while preserving source triangle ABC.
            emitVertex(triangle.c(), pose, normalPose, vertexConsumer, packedLight, packedOverlay, renderColor);
        }
    }

    private static void emitVertex(
            ResidentMeshVertex vertex,
            Matrix4f pose,
            Matrix3f normalPose,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        Vector4f position = pose.transform(new Vector4f(
                vertex.x() / MODEL_PIXELS_PER_BLOCK,
                vertex.y() / MODEL_PIXELS_PER_BLOCK,
                vertex.z() / MODEL_PIXELS_PER_BLOCK,
                1
        ));
        Vector3f normal = normalPose.transform(new Vector3f(
                vertex.normalX(),
                vertex.normalY(),
                vertex.normalZ()
        ));

        if (normal.lengthSquared() > 1.0e-8f) {
            normal.normalize();
        }

        vertexConsumer.addVertex(
                position.x(), position.y(), position.z(),
                renderColor,
                vertex.u(), vertex.v(),
                packedOverlay, packedLight,
                normal.x(), normal.y(), normal.z()
        );
    }
}

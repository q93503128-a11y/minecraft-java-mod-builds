package kr.moonseungjun.campfiresessions.client.resident.mesh;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
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
 * <p>GeckoLib 5's Bedrock geometry loader bakes ordinary definitions into
 * CuboidGeoBone and intentionally does not render Bedrock poly_mesh. Campfire
 * therefore keeps GeckoLib's hierarchy/render lifecycle while supplying the
 * Plumberry triangles directly from a converted runtime asset.</p>
 */
public final class ResidentMeshBone extends GeoBone {
    private static final float MODEL_PIXELS_PER_BLOCK = 16f;

    private final ResidentMeshTriangle[] triangles;

    public ResidentMeshBone(
            @Nullable GeoBone parent,
            String name,
            GeoBone[] children,
            GeoLocator[] locators,
            ResidentMeshTriangle[] triangles
    ) {
        super(parent, name, children, locators, 0, 0, 0, 0, 0, 0);
        this.triangles = triangles;
    }

    public ResidentMeshTriangle[] triangles() {
        return this.triangles;
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

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalPose = poseStack.last().normal();

        for (ResidentMeshTriangle triangle : this.triangles) {
            emitVertex(triangle.a(), pose, normalPose, vertexConsumer, packedLight, packedOverlay, renderColor);
            emitVertex(triangle.b(), pose, normalPose, vertexConsumer, packedLight, packedOverlay, renderColor);
            emitVertex(triangle.c(), pose, normalPose, vertexConsumer, packedLight, packedOverlay, renderColor);

            // Entity cutout rendering is quad-based. Repeating C makes the second
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

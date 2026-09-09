package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.moonseungjun.riftfrontier.combat.presentation.BossSkinnedMeshFrameSampler;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshFrame;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;

import java.util.Objects;

/**
 * Minecraft 26.2 submission adapter for one immutable, already-skinned boss frame.
 *
 * <p>This class is intentionally presentation-only. It owns no animation clock, hit timing, material selection,
 * texture lookup, or source-asset fallback. The caller supplies the {@link RenderType}; this keeps the currently
 * unapproved Dragon source Atlas material outside the production path while allowing an approved material to be
 * connected later without changing deformation semantics.</p>
 */
public final class SkinnedMeshCustomGeometryAdapter {
    private SkinnedMeshCustomGeometryAdapter() {
    }

    /** Snapshots the exact authoritative frame sample before Minecraft defers feature rendering. */
    public static PreparedGeometry prepare(BossSkinnedMeshFrameSampler.FrameSample sample) {
        Objects.requireNonNull(sample, "sample");
        return prepare(sample.frame());
    }

    /** Snapshots renderer data into defensive copies suitable for deferred submission. */
    public static PreparedGeometry prepare(SkinnedMeshFrame frame) {
        return new PreparedGeometry(Objects.requireNonNull(frame, "frame"));
    }

    /**
     * Submits an immutable triangle frame through Minecraft 26.2's feature renderer.
     *
     * @param packedColor ARGB color multiplier supplied by the eventual approved presentation profile
     */
    public static void submit(
        BossSkinnedMeshFrameSampler.FrameSample sample,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        RenderType renderType,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        Objects.requireNonNull(poseStack, "poseStack");
        Objects.requireNonNull(collector, "collector");
        Objects.requireNonNull(renderType, "renderType");

        PreparedGeometry geometry = prepare(sample);
        collector.submitCustomGeometry(
            poseStack,
            renderType,
            (pose, consumer) -> geometry.write(pose, consumer, packedLight, packedOverlay, packedColor)
        );
    }

    /** Immutable submission snapshot; source indices are preserved rather than converted to cubes or rigid bones. */
    public static final class PreparedGeometry {
        private final float[] positions;
        private final float[] normals;
        private final float[] uvs;
        private final int[] indices;

        private PreparedGeometry(SkinnedMeshFrame frame) {
            this.positions = frame.positions();
            this.normals = frame.normals();
            this.uvs = frame.uvs();
            this.indices = frame.indices();
        }

        private void write(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            int packedColor
        ) {
            for (int index : indices) {
                int xyz = index * 3;
                int uv = index * 2;
                consumer.addVertex(pose, positions[xyz], positions[xyz + 1], positions[xyz + 2])
                    .setColor(packedColor)
                    .setUv(uvs[uv], uvs[uv + 1])
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(pose, normals[xyz], normals[xyz + 1], normals[xyz + 2]);
            }
        }

        public int vertexCount() {
            return positions.length / 3;
        }

        public int triangleCount() {
            return indices.length / 3;
        }

        public float[] positions() {
            return positions.clone();
        }

        public float[] normals() {
            return normals.clone();
        }

        public float[] uvs() {
            return uvs.clone();
        }

        public int[] indices() {
            return indices.clone();
        }
    }
}

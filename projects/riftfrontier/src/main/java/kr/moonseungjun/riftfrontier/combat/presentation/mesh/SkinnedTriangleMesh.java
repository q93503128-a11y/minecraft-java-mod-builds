package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import java.util.Arrays;

/**
 * Immutable indexed triangle mesh with glTF-style four-influence linear-blend skinning.
 * This is a renderer-neutral runtime boundary: no Minecraft client classes are referenced here.
 */
public final class SkinnedTriangleMesh {
    private static final float WEIGHT_SUM_EPSILON = 0.0025f;

    private final float[] positions;
    private final float[] normals;
    private final float[] uvs;
    private final int[] indices;
    private final int[] joints;
    private final float[] weights;

    public SkinnedTriangleMesh(
            float[] positions,
            float[] normals,
            float[] uvs,
            int[] indices,
            int[] joints,
            float[] weights
    ) {
        this.positions = copy(positions, "positions");
        this.normals = copy(normals, "normals");
        this.uvs = copy(uvs, "uvs");
        this.indices = copy(indices, "indices");
        this.joints = copy(joints, "joints");
        this.weights = copy(weights, "weights");
        validate();
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

    public int[] joints() {
        return joints.clone();
    }

    public float[] weights() {
        return weights.clone();
    }

    float position(int componentIndex) {
        return positions[componentIndex];
    }

    float normal(int componentIndex) {
        return normals[componentIndex];
    }

    float uv(int componentIndex) {
        return uvs[componentIndex];
    }

    int index(int index) {
        return indices[index];
    }

    int joint(int influenceIndex) {
        return joints[influenceIndex];
    }

    float weight(int influenceIndex) {
        return weights[influenceIndex];
    }

    private void validate() {
        if (positions.length == 0 || positions.length % 3 != 0) {
            throw new IllegalArgumentException("positions must contain one or more xyz vertices");
        }
        int vertexCount = vertexCount();
        if (normals.length != positions.length) {
            throw new IllegalArgumentException("normals must contain one xyz normal per vertex");
        }
        if (uvs.length != vertexCount * 2) {
            throw new IllegalArgumentException("uvs must contain one uv pair per vertex");
        }
        if (joints.length != vertexCount * 4 || weights.length != vertexCount * 4) {
            throw new IllegalArgumentException("joints and weights must contain four influences per vertex");
        }
        if (indices.length == 0 || indices.length % 3 != 0) {
            throw new IllegalArgumentException("indices must contain one or more complete triangles");
        }

        requireFinite(positions, "positions");
        requireFinite(normals, "normals");
        requireFinite(uvs, "uvs");
        requireFinite(weights, "weights");

        for (int index : indices) {
            if (index < 0 || index >= vertexCount) {
                throw new IllegalArgumentException("triangle index outside vertex range: " + index);
            }
        }

        for (int vertex = 0; vertex < vertexCount; vertex++) {
            float sum = 0.0f;
            for (int lane = 0; lane < 4; lane++) {
                int influence = vertex * 4 + lane;
                int joint = joints[influence];
                float weight = weights[influence];
                if (joint < 0) {
                    throw new IllegalArgumentException("joint index must be non-negative");
                }
                if (weight < 0.0f) {
                    throw new IllegalArgumentException("skin weight must be non-negative");
                }
                sum += weight;
            }
            if (Math.abs(sum - 1.0f) > WEIGHT_SUM_EPSILON) {
                throw new IllegalArgumentException("skin weights must sum to 1 for vertex " + vertex + ": " + sum);
            }
        }
    }

    private static float[] copy(float[] values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return values.clone();
    }

    private static int[] copy(int[] values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return values.clone();
    }

    private static void requireFinite(float[] values, String name) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(name + " contains a non-finite component");
            }
        }
    }

    @Override
    public String toString() {
        return "SkinnedTriangleMesh[vertices=" + vertexCount() + ", triangles=" + triangleCount() + "]";
    }
}

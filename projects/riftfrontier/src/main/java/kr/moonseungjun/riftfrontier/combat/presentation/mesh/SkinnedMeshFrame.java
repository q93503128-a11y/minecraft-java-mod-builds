package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

/** Immutable renderer-facing snapshot produced after animation pose sampling and skinning. */
public final class SkinnedMeshFrame {
    private final float[] positions;
    private final float[] normals;
    private final float[] uvs;
    private final int[] indices;

    public SkinnedMeshFrame(float[] positions, float[] normals, float[] uvs, int[] indices) {
        if (positions == null || normals == null || uvs == null || indices == null) {
            throw new IllegalArgumentException("frame arrays must not be null");
        }
        if (positions.length == 0 || positions.length % 3 != 0 || normals.length != positions.length) {
            throw new IllegalArgumentException("frame must contain matching xyz position/normal streams");
        }
        int vertexCount = positions.length / 3;
        if (uvs.length != vertexCount * 2) {
            throw new IllegalArgumentException("frame must contain one uv pair per vertex");
        }
        if (indices.length == 0 || indices.length % 3 != 0) {
            throw new IllegalArgumentException("frame indices must contain complete triangles");
        }
        for (float value : positions) {
            requireFinite(value, "position");
        }
        for (float value : normals) {
            requireFinite(value, "normal");
        }
        for (float value : uvs) {
            requireFinite(value, "uv");
        }
        for (int index : indices) {
            if (index < 0 || index >= vertexCount) {
                throw new IllegalArgumentException("frame index outside vertex stream: " + index);
            }
        }
        this.positions = positions.clone();
        this.normals = normals.clone();
        this.uvs = uvs.clone();
        this.indices = indices.clone();
    }

    private static void requireFinite(float value, String stream) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("frame contains non-finite " + stream + " data");
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

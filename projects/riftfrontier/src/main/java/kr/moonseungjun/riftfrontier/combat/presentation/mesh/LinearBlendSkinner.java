package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import java.util.List;

/**
 * CPU reference implementation of four-influence linear-blend skinning.
 *
 * <p>The output keeps the source triangle topology and UV stream unchanged. Positions are blended as affine
 * points. Normals are blended by the upper 3x3 portion of the same skin matrices and normalized afterwards.
 * Production animation code is responsible for supplying the final per-joint skin matrices for the sampled pose.</p>
 */
public final class LinearBlendSkinner {
    private static final float NORMAL_EPSILON = 1.0e-12f;

    private LinearBlendSkinner() {
    }

    public static SkinnedMeshFrame skin(SkinnedTriangleMesh mesh, List<Affine3x4> skinMatrices) {
        if (mesh == null) {
            throw new IllegalArgumentException("mesh must not be null");
        }
        if (skinMatrices == null || skinMatrices.isEmpty()) {
            throw new IllegalArgumentException("skinMatrices must contain at least one joint matrix");
        }
        for (Affine3x4 matrix : skinMatrices) {
            if (matrix == null) {
                throw new IllegalArgumentException("skinMatrices must not contain null entries");
            }
        }

        float[] positions = new float[mesh.vertexCount() * 3];
        float[] normals = new float[mesh.vertexCount() * 3];

        for (int vertex = 0; vertex < mesh.vertexCount(); vertex++) {
            int p = vertex * 3;
            float x = mesh.position(p);
            float y = mesh.position(p + 1);
            float z = mesh.position(p + 2);
            float nx = mesh.normal(p);
            float ny = mesh.normal(p + 1);
            float nz = mesh.normal(p + 2);

            float outX = 0.0f;
            float outY = 0.0f;
            float outZ = 0.0f;
            float outNx = 0.0f;
            float outNy = 0.0f;
            float outNz = 0.0f;

            for (int lane = 0; lane < 4; lane++) {
                int influence = vertex * 4 + lane;
                float weight = mesh.weight(influence);
                if (weight == 0.0f) {
                    continue;
                }
                int joint = mesh.joint(influence);
                if (joint >= skinMatrices.size()) {
                    throw new IllegalArgumentException(
                            "joint index " + joint + " outside supplied matrix palette of " + skinMatrices.size()
                    );
                }
                Affine3x4 matrix = skinMatrices.get(joint);
                outX += matrix.transformPointX(x, y, z) * weight;
                outY += matrix.transformPointY(x, y, z) * weight;
                outZ += matrix.transformPointZ(x, y, z) * weight;
                outNx += matrix.transformVectorX(nx, ny, nz) * weight;
                outNy += matrix.transformVectorY(nx, ny, nz) * weight;
                outNz += matrix.transformVectorZ(nx, ny, nz) * weight;
            }

            positions[p] = outX;
            positions[p + 1] = outY;
            positions[p + 2] = outZ;

            float normalLengthSquared = outNx * outNx + outNy * outNy + outNz * outNz;
            if (normalLengthSquared <= NORMAL_EPSILON || !Float.isFinite(normalLengthSquared)) {
                throw new IllegalArgumentException("skinning produced a degenerate normal at vertex " + vertex);
            }
            float invLength = (float) (1.0 / Math.sqrt(normalLengthSquared));
            normals[p] = outNx * invLength;
            normals[p + 1] = outNy * invLength;
            normals[p + 2] = outNz * invLength;
        }

        return new SkinnedMeshFrame(positions, normals, mesh.uvs(), mesh.indices());
    }
}

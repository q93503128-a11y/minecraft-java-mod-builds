package kr.moonseungjun.campfiresessions.client.resident.mesh;

/**
 * Geometry batch using one converted material.
 */
public record ResidentMeshPrimitive(
        int materialIndex,
        ResidentMeshTriangle[] triangles
) {
    public ResidentMeshPrimitive {
        if (materialIndex < 0) {
            throw new IllegalArgumentException("materialIndex must be >= 0");
        }
        triangles = triangles.clone();
    }

    @Override
    public ResidentMeshTriangle[] triangles() {
        return this.triangles.clone();
    }

    ResidentMeshTriangle[] trianglesUnsafe() {
        return this.triangles;
    }
}

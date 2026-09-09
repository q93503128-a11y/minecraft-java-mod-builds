package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

/**
 * Immutable row-major 3x4 affine transform used by the project-owned skinned-mesh runtime.
 * The final row is implicitly {@code [0, 0, 0, 1]}.
 */
public record Affine3x4(
        float m00, float m01, float m02, float m03,
        float m10, float m11, float m12, float m13,
        float m20, float m21, float m22, float m23
) {
    public Affine3x4 {
        requireFinite(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
    }

    public static Affine3x4 identity() {
        return new Affine3x4(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f
        );
    }

    public static Affine3x4 translation(float x, float y, float z) {
        return new Affine3x4(
                1.0f, 0.0f, 0.0f, x,
                0.0f, 1.0f, 0.0f, y,
                0.0f, 0.0f, 1.0f, z
        );
    }

    public float transformPointX(float x, float y, float z) {
        return m00 * x + m01 * y + m02 * z + m03;
    }

    public float transformPointY(float x, float y, float z) {
        return m10 * x + m11 * y + m12 * z + m13;
    }

    public float transformPointZ(float x, float y, float z) {
        return m20 * x + m21 * y + m22 * z + m23;
    }

    public float transformVectorX(float x, float y, float z) {
        return m00 * x + m01 * y + m02 * z;
    }

    public float transformVectorY(float x, float y, float z) {
        return m10 * x + m11 * y + m12 * z;
    }

    public float transformVectorZ(float x, float y, float z) {
        return m20 * x + m21 * y + m22 * z;
    }

    private static void requireFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Affine transform contains a non-finite component");
            }
        }
    }
}

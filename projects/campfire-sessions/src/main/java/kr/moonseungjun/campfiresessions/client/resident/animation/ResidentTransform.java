package kr.moonseungjun.campfiresessions.client.resident.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Immutable local glTF node transform.
 */
public record ResidentTransform(
        float tx, float ty, float tz,
        float qx, float qy, float qz, float qw,
        float sx, float sy, float sz
) {
    public static ResidentTransform identity() {
        return new ResidentTransform(0, 0, 0, 0, 0, 0, 1, 1, 1, 1);
    }

    public Vector3f translation() {
        return new Vector3f(this.tx, this.ty, this.tz);
    }

    public Quaternionf rotation() {
        return new Quaternionf(this.qx, this.qy, this.qz, this.qw).normalize();
    }

    public Vector3f scale() {
        return new Vector3f(this.sx, this.sy, this.sz);
    }

    public ResidentTransform withTranslation(Vector3f value) {
        return new ResidentTransform(
                value.x, value.y, value.z,
                this.qx, this.qy, this.qz, this.qw,
                this.sx, this.sy, this.sz
        );
    }

    public ResidentTransform withRotation(Quaternionf value) {
        Quaternionf normalized = new Quaternionf(value).normalize();
        return new ResidentTransform(
                this.tx, this.ty, this.tz,
                normalized.x, normalized.y, normalized.z, normalized.w,
                this.sx, this.sy, this.sz
        );
    }

    public ResidentTransform withScale(Vector3f value) {
        return new ResidentTransform(
                this.tx, this.ty, this.tz,
                this.qx, this.qy, this.qz, this.qw,
                value.x, value.y, value.z
        );
    }
}

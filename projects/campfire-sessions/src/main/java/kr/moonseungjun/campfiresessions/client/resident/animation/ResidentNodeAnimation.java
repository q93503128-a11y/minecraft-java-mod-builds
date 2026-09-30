package kr.moonseungjun.campfiresessions.client.resident.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Transform channels targeting one glTF node.
 */
public record ResidentNodeAnimation(
        @Nullable ResidentVec3Track translation,
        @Nullable ResidentQuaternionTrack rotation,
        @Nullable ResidentVec3Track scale
) {
    public ResidentTransform sample(float time, ResidentTransform rest) {
        Vector3f sampledTranslation = this.translation == null ? rest.translation() : this.translation.sample(time);
        Quaternionf sampledRotation = this.rotation == null ? rest.rotation() : this.rotation.sample(time);
        Vector3f sampledScale = this.scale == null ? rest.scale() : this.scale.sample(time);

        return new ResidentTransform(
                sampledTranslation.x, sampledTranslation.y, sampledTranslation.z,
                sampledRotation.x, sampledRotation.y, sampledRotation.z, sampledRotation.w,
                sampledScale.x, sampledScale.y, sampledScale.z
        );
    }

    public float duration() {
        float duration = 0;
        if (this.translation != null) duration = Math.max(duration, this.translation.duration());
        if (this.rotation != null) duration = Math.max(duration, this.rotation.duration());
        if (this.scale != null) duration = Math.max(duration, this.scale.duration());
        return duration;
    }
}

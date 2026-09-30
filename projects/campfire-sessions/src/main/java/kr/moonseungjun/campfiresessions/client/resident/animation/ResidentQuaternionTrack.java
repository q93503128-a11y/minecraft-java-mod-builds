package kr.moonseungjun.campfiresessions.client.resident.animation;

import org.joml.Quaternionf;

import java.util.Arrays;

/**
 * glTF quaternion animation sampler.
 *
 * <p>LINEAR uses quaternion slerp. CUBICSPLINE follows glTF's component-wise
 * Hermite rule and normalizes the resulting quaternion.</p>
 */
public final class ResidentQuaternionTrack {
    private final float[] times;
    private final float[] values;
    private final ResidentInterpolation interpolation;

    public ResidentQuaternionTrack(float[] times, float[] values, ResidentInterpolation interpolation) {
        if (times.length == 0) {
            throw new IllegalArgumentException("track must contain at least one key");
        }

        int expected = times.length * (interpolation == ResidentInterpolation.CUBICSPLINE ? 12 : 4);
        if (values.length != expected) {
            throw new IllegalArgumentException("expected " + expected + " quaternion values, got " + values.length);
        }

        if (times[0] < 0) {
            throw new IllegalArgumentException("glTF key times must start at or after zero");
        }
        if (interpolation == ResidentInterpolation.CUBICSPLINE && times.length < 2) {
            throw new IllegalArgumentException("glTF CUBICSPLINE requires at least two keys");
        }

        this.times = times.clone();
        this.values = values.clone();
        this.interpolation = interpolation;

        for (int i = 1; i < this.times.length; i++) {
            if (this.times[i] <= this.times[i - 1]) {
                throw new IllegalArgumentException("glTF key times must be strictly increasing");
            }
        }
    }

    public float duration() {
        return this.times[this.times.length - 1];
    }

    public Quaternionf sample(float time) {
        if (this.times.length == 1 || time <= this.times[0]) {
            return valueAt(0);
        }
        if (time >= this.times[this.times.length - 1]) {
            return valueAt(this.times.length - 1);
        }

        int index = ResidentTrackMath.segment(this.times, time);
        if (this.interpolation == ResidentInterpolation.STEP) {
            return valueAt(index);
        }

        float alpha = ResidentTrackMath.alpha(this.times, index, time);
        if (this.interpolation == ResidentInterpolation.LINEAR) {
            return valueAt(index).slerp(valueAt(index + 1), alpha).normalize();
        }

        float dt = this.times[index + 1] - this.times[index];
        Quaternionf p0 = valueAt(index);
        Quaternionf p1 = valueAt(index + 1);
        Quaternionf out0 = outTangentAt(index);
        Quaternionf in1 = inTangentAt(index + 1);

        return new Quaternionf(
                ResidentTrackMath.hermite(p0.x, out0.x, p1.x, in1.x, alpha, dt),
                ResidentTrackMath.hermite(p0.y, out0.y, p1.y, in1.y, alpha, dt),
                ResidentTrackMath.hermite(p0.z, out0.z, p1.z, in1.z, alpha, dt),
                ResidentTrackMath.hermite(p0.w, out0.w, p1.w, in1.w, alpha, dt)
        ).normalize();
    }

    private Quaternionf valueAt(int key) {
        int offset = key * (this.interpolation == ResidentInterpolation.CUBICSPLINE ? 12 : 4);
        if (this.interpolation == ResidentInterpolation.CUBICSPLINE) {
            offset += 4;
        }
        return new Quaternionf(
                this.values[offset],
                this.values[offset + 1],
                this.values[offset + 2],
                this.values[offset + 3]
        ).normalize();
    }

    private Quaternionf inTangentAt(int key) {
        int offset = key * 12;
        return new Quaternionf(
                this.values[offset],
                this.values[offset + 1],
                this.values[offset + 2],
                this.values[offset + 3]
        );
    }

    private Quaternionf outTangentAt(int key) {
        int offset = key * 12 + 8;
        return new Quaternionf(
                this.values[offset],
                this.values[offset + 1],
                this.values[offset + 2],
                this.values[offset + 3]
        );
    }

    @Override
    public String toString() {
        return "ResidentQuaternionTrack{" +
                "times=" + Arrays.toString(this.times) +
                ", interpolation=" + this.interpolation +
                '}';
    }
}

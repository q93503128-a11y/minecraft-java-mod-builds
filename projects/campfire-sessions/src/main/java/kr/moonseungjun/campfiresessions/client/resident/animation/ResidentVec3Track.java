package kr.moonseungjun.campfiresessions.client.resident.animation;

import org.joml.Vector3f;

import java.util.Arrays;

/**
 * glTF VEC3 animation sampler for translation or scale.
 *
 * <p>For CUBICSPLINE the value array follows glTF's
 * in-tangent/value/out-tangent layout for every key.</p>
 */
public final class ResidentVec3Track {
    private final float[] times;
    private final float[] values;
    private final ResidentInterpolation interpolation;

    public ResidentVec3Track(float[] times, float[] values, ResidentInterpolation interpolation) {
        if (times.length == 0) {
            throw new IllegalArgumentException("track must contain at least one key");
        }

        int expected = times.length * (interpolation == ResidentInterpolation.CUBICSPLINE ? 9 : 3);
        if (values.length != expected) {
            throw new IllegalArgumentException("expected " + expected + " VEC3 values, got " + values.length);
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

    public Vector3f sample(float time) {
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
            return valueAt(index).lerp(valueAt(index + 1), alpha);
        }

        float dt = this.times[index + 1] - this.times[index];
        Vector3f p0 = valueAt(index);
        Vector3f p1 = valueAt(index + 1);
        Vector3f out0 = outTangentAt(index);
        Vector3f in1 = inTangentAt(index + 1);

        return new Vector3f(
                ResidentTrackMath.hermite(p0.x, out0.x, p1.x, in1.x, alpha, dt),
                ResidentTrackMath.hermite(p0.y, out0.y, p1.y, in1.y, alpha, dt),
                ResidentTrackMath.hermite(p0.z, out0.z, p1.z, in1.z, alpha, dt)
        );
    }

    private Vector3f valueAt(int key) {
        int offset = key * (this.interpolation == ResidentInterpolation.CUBICSPLINE ? 9 : 3);
        if (this.interpolation == ResidentInterpolation.CUBICSPLINE) {
            offset += 3;
        }
        return new Vector3f(this.values[offset], this.values[offset + 1], this.values[offset + 2]);
    }

    private Vector3f inTangentAt(int key) {
        int offset = key * 9;
        return new Vector3f(this.values[offset], this.values[offset + 1], this.values[offset + 2]);
    }

    private Vector3f outTangentAt(int key) {
        int offset = key * 9 + 6;
        return new Vector3f(this.values[offset], this.values[offset + 1], this.values[offset + 2]);
    }

    @Override
    public String toString() {
        return "ResidentVec3Track{" +
                "times=" + Arrays.toString(this.times) +
                ", interpolation=" + this.interpolation +
                '}';
    }
}

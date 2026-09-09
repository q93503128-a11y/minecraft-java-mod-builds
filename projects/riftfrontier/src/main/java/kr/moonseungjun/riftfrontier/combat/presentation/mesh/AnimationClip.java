package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import java.util.List;

/** Immutable renderer-neutral glTF animation clip targeting the imported skin palette. */
public record AnimationClip(String name, float durationSeconds, List<Channel> channels) {
    public AnimationClip {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("animation clip name must be non-blank");
        }
        if (!Float.isFinite(durationSeconds) || durationSeconds < 0.0f) {
            throw new IllegalArgumentException("animation duration must be finite and non-negative");
        }
        channels = List.copyOf(channels);
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("animation clip must contain at least one channel");
        }
    }

    public enum Path {
        TRANSLATION(3), ROTATION(4), SCALE(3);

        private final int components;

        Path(int components) {
            this.components = components;
        }

        public int components() {
            return components;
        }
    }

    public enum Interpolation {
        STEP, LINEAR, CUBICSPLINE
    }

    public record Channel(
            int joint,
            Path path,
            Interpolation interpolation,
            float[] keyTimes,
            float[] values
    ) {
        public Channel {
            if (joint < 0) {
                throw new IllegalArgumentException("animation joint must be non-negative");
            }
            if (path == null || interpolation == null || keyTimes == null || values == null) {
                throw new IllegalArgumentException("animation channel fields must not be null");
            }
            keyTimes = keyTimes.clone();
            values = values.clone();
            if (keyTimes.length == 0) {
                throw new IllegalArgumentException("animation channel must contain at least one key");
            }
            float previous = -Float.MAX_VALUE;
            for (float time : keyTimes) {
                if (!Float.isFinite(time) || time < 0.0f || time <= previous) {
                    throw new IllegalArgumentException("animation key times must be finite, non-negative and strictly increasing");
                }
                previous = time;
            }
            int multiplier = interpolation == Interpolation.CUBICSPLINE ? 3 : 1;
            int expected = keyTimes.length * path.components() * multiplier;
            if (values.length != expected) {
                throw new IllegalArgumentException("animation value count does not match key/path/interpolation contract");
            }
            for (float value : values) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("animation channel contains non-finite value");
                }
            }
        }

        @Override
        public float[] keyTimes() {
            return keyTimes.clone();
        }

        @Override
        public float[] values() {
            return values.clone();
        }

        float keyTime(int index) {
            return keyTimes[index];
        }

        float value(int index) {
            return values[index];
        }

        int keyCount() {
            return keyTimes.length;
        }
    }
}

package kr.moonseungjun.campfiresessions.client.resident.animation;

final class ResidentTrackMath {
    private ResidentTrackMath() {}

    static int segment(float[] times, float time) {
        if (times.length < 2 || time <= times[0]) {
            return 0;
        }

        for (int i = 0; i < times.length - 1; i++) {
            if (time < times[i + 1]) {
                return i;
            }
        }

        return times.length - 2;
    }

    static float alpha(float[] times, int index, float time) {
        float start = times[index];
        float end = times[index + 1];

        if (end <= start) {
            return 0;
        }

        return Math.max(0, Math.min(1, (time - start) / (end - start)));
    }

    static float hermite(float p0, float outTangent0, float p1, float inTangent1, float alpha, float deltaTime) {
        float t2 = alpha * alpha;
        float t3 = t2 * alpha;
        float h00 = 2 * t3 - 3 * t2 + 1;
        float h10 = t3 - 2 * t2 + alpha;
        float h01 = -2 * t3 + 3 * t2;
        float h11 = t3 - t2;

        return h00 * p0
                + h10 * deltaTime * outTangent0
                + h01 * p1
                + h11 * deltaTime * inTangent1;
    }
}

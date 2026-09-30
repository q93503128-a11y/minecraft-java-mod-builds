package kr.moonseungjun.campfiresessions.client.resident.animation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One named glTF animation clip mapped by Campfire bone/node name.
 */
public final class ResidentAnimationClip {
    private final String name;
    private final boolean loop;
    private final float duration;
    private final Map<String, ResidentNodeAnimation> nodes;

    public ResidentAnimationClip(String name, boolean loop, Map<String, ResidentNodeAnimation> nodes) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("animation name must not be blank");
        }

        this.name = name;
        this.loop = loop;
        this.nodes = Map.copyOf(nodes);

        float maxDuration = 0;
        for (ResidentNodeAnimation animation : this.nodes.values()) {
            maxDuration = Math.max(maxDuration, animation.duration());
        }
        this.duration = maxDuration;
    }

    public String name() {
        return this.name;
    }

    public boolean loop() {
        return this.loop;
    }

    public float duration() {
        return this.duration;
    }

    public ResidentTransform sample(String nodeName, float timeSeconds, ResidentTransform restTransform) {
        ResidentNodeAnimation animation = this.nodes.get(nodeName);
        if (animation == null) {
            return restTransform;
        }

        return animation.sample(normalizeTime(timeSeconds), restTransform);
    }

    public Map<String, ResidentTransform> samplePose(
            float timeSeconds,
            Map<String, ResidentTransform> restPose
    ) {
        float time = normalizeTime(timeSeconds);
        Map<String, ResidentTransform> result = new LinkedHashMap<>(restPose.size());

        for (Map.Entry<String, ResidentTransform> entry : restPose.entrySet()) {
            ResidentNodeAnimation animation = this.nodes.get(entry.getKey());
            result.put(
                    entry.getKey(),
                    animation == null ? entry.getValue() : animation.sample(time, entry.getValue())
            );
        }

        return Map.copyOf(result);
    }

    private float normalizeTime(float timeSeconds) {
        if (this.duration <= 0) {
            return 0;
        }
        if (!this.loop) {
            return Math.max(0, Math.min(this.duration, timeSeconds));
        }

        float wrapped = timeSeconds % this.duration;
        return wrapped < 0 ? wrapped + this.duration : wrapped;
    }
}

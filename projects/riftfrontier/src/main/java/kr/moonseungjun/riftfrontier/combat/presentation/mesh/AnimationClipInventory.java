package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, deterministic inventory of imported animation clips.
 *
 * <p>The inventory deliberately keeps source clip names separate from logical presentation keys. Production
 * bindings must first prove that the imported source inventory is the expected one, then explicitly bind logical
 * keys to verified source names.</p>
 */
public final class AnimationClipInventory {
    private final List<AnimationClip> clips;
    private final Map<String, AnimationClip> clipsByName;
    private final List<ClipMetrics> metrics;

    private AnimationClipInventory(List<AnimationClip> importedClips) {
        Objects.requireNonNull(importedClips, "importedClips");
        if (importedClips.isEmpty()) {
            throw new IllegalArgumentException("animation inventory must contain at least one clip");
        }

        List<AnimationClip> clipCopy = new ArrayList<>(importedClips.size());
        Map<String, AnimationClip> byName = new LinkedHashMap<>();
        List<ClipMetrics> metricCopy = new ArrayList<>(importedClips.size());
        for (AnimationClip clip : importedClips) {
            Objects.requireNonNull(clip, "animation clip");
            if (byName.putIfAbsent(clip.name(), clip) != null) {
                throw new IllegalArgumentException("duplicate imported animation clip name: " + clip.name());
            }
            clipCopy.add(clip);
            metricCopy.add(ClipMetrics.from(clip));
        }
        this.clips = List.copyOf(clipCopy);
        this.clipsByName = Map.copyOf(byName);
        this.metrics = List.copyOf(metricCopy);
    }

    public static AnimationClipInventory fromImported(List<AnimationClip> importedClips) {
        return new AnimationClipInventory(importedClips);
    }

    public AnimationClipInventory requireExactNames(List<String> expectedNames) {
        Objects.requireNonNull(expectedNames, "expectedNames");
        LinkedHashSet<String> expected = new LinkedHashSet<>();
        for (String name : expectedNames) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("expected source clip name must be non-blank");
            }
            if (!expected.add(name)) {
                throw new IllegalArgumentException("duplicate expected source clip name: " + name);
            }
        }
        if (expected.isEmpty()) {
            throw new IllegalArgumentException("expected source clip inventory must not be empty");
        }

        Set<String> actual = clipsByName.keySet();
        if (!actual.equals(expected)) {
            LinkedHashSet<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(actual);
            LinkedHashSet<String> unexpected = new LinkedHashSet<>(actual);
            unexpected.removeAll(expected);
            throw new IllegalArgumentException(
                "animation source inventory mismatch; missing=" + missing + ", unexpected=" + unexpected
            );
        }
        return this;
    }

    public AnimationClip requireClip(String sourceName) {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("source clip name must be non-blank");
        }
        AnimationClip clip = clipsByName.get(sourceName);
        if (clip == null) {
            throw new IllegalArgumentException("unknown imported source clip: " + sourceName);
        }
        return clip;
    }

    public List<AnimationClip> clips() {
        return clips;
    }

    public List<String> names() {
        return clips.stream().map(AnimationClip::name).toList();
    }

    public List<ClipMetrics> metrics() {
        return metrics;
    }

    public record ClipMetrics(
        String name,
        float durationSeconds,
        int channelCount,
        int totalKeyCount,
        int translationChannels,
        int rotationChannels,
        int scaleChannels,
        int stepChannels,
        int linearChannels,
        int cubicSplineChannels
    ) {
        private static ClipMetrics from(AnimationClip clip) {
            int keys = 0;
            int translation = 0;
            int rotation = 0;
            int scale = 0;
            int step = 0;
            int linear = 0;
            int cubic = 0;
            for (AnimationClip.Channel channel : clip.channels()) {
                keys += channel.keyTimes().length;
                switch (channel.path()) {
                    case TRANSLATION -> translation++;
                    case ROTATION -> rotation++;
                    case SCALE -> scale++;
                }
                switch (channel.interpolation()) {
                    case STEP -> step++;
                    case LINEAR -> linear++;
                    case CUBICSPLINE -> cubic++;
                }
            }
            return new ClipMetrics(
                clip.name(), clip.durationSeconds(), clip.channels().size(), keys,
                translation, rotation, scale, step, linear, cubic
            );
        }
    }
}

package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Data-authored logical presentation profile for one boss context.
 * Asset keys are stable logical identifiers; this layer does not assert that final model/VFX/audio assets exist yet.
 */
public record BossPresentationProfile(
    ContentId id,
    ContentId bossProfile,
    String variant,
    ContentId modelKey,
    Map<BindingKey, AssetBinding> bindings
) {
    private static final Pattern VARIANT = Pattern.compile("[a-z0-9_.-]+");

    public BossPresentationProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bossProfile, "bossProfile");
        variant = Objects.requireNonNull(variant, "variant").trim();
        Objects.requireNonNull(modelKey, "modelKey");
        if (!VARIANT.matcher(variant).matches()) {
            throw new IllegalArgumentException("variant must match [a-z0-9_.-]+: " + variant);
        }
        Map<BindingKey, AssetBinding> copy = new LinkedHashMap<>();
        Objects.requireNonNull(bindings, "bindings").forEach((key, value) -> {
            Objects.requireNonNull(key, "binding key");
            Objects.requireNonNull(value, "asset binding");
            if (copy.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("duplicate presentation binding: " + key.selector());
            }
        });
        bindings = Map.copyOf(copy);
    }

    public record Context(ContentId bossProfile, String variant) {
        public Context {
            Objects.requireNonNull(bossProfile, "bossProfile");
            variant = Objects.requireNonNull(variant, "variant").trim();
            if (!VARIANT.matcher(variant).matches()) throw new IllegalArgumentException("invalid variant: " + variant);
        }
    }

    public record BindingKey(String presentationCue, String delivery, AttackTimeline.Phase phase) {
        public BindingKey {
            presentationCue = requireToken(presentationCue, "presentationCue");
            delivery = requireToken(delivery, "delivery");
            Objects.requireNonNull(phase, "phase");
            if (phase == AttackTimeline.Phase.COMPLETE) {
                throw new IllegalArgumentException("COMPLETE cannot have a presentation binding");
            }
        }

        public String selector() {
            return presentationCue + "|" + delivery + "|" + phase.name();
        }
    }

    public record AssetBinding(ContentId animationKey, ContentId vfxKey, ContentId soundKey) {
        public AssetBinding {
            Objects.requireNonNull(animationKey, "animationKey");
            Objects.requireNonNull(vfxKey, "vfxKey");
            Objects.requireNonNull(soundKey, "soundKey");
        }
    }

    public Context context() {
        return new Context(bossProfile, variant);
    }

    private static String requireToken(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty() || normalized.indexOf('|') >= 0) {
            throw new IllegalArgumentException(label + " must be non-blank and may not contain '|'");
        }
        return normalized;
    }
}

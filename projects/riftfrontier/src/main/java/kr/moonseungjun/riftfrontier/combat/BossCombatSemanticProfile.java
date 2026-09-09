package kr.moonseungjun.riftfrontier.combat;

import com.google.gson.*;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.*;

/**
 * Non-numeric, data-authored boss combat semantics.
 *
 * <p>This profile scopes already-authored attack patterns to boss phases. It owns no
 * damage, tick timing, hit volume, weight, cooldown or visual asset path.</p>
 */
public record BossCombatSemanticProfile(
    ContentId bossProfile,
    Map<Integer, Set<ContentId>> phaseAttackPatterns
) {
    public BossCombatSemanticProfile {
        Objects.requireNonNull(bossProfile, "bossProfile");
        Objects.requireNonNull(phaseAttackPatterns, "phaseAttackPatterns");
        if (phaseAttackPatterns.isEmpty()) {
            throw new IllegalArgumentException("phaseAttackPatterns must not be empty");
        }
        Map<Integer, Set<ContentId>> copy = new TreeMap<>();
        phaseAttackPatterns.forEach((phase, attacks) -> {
            if (phase == null || phase <= 0) throw new IllegalArgumentException("phase keys must be positive");
            Objects.requireNonNull(attacks, "phase attack set");
            if (attacks.isEmpty()) throw new IllegalArgumentException("phase " + phase + " has no attacks");
            TreeSet<ContentId> sorted = new TreeSet<>();
            for (ContentId attack : attacks) sorted.add(Objects.requireNonNull(attack, "attack id"));
            if (copy.putIfAbsent(phase, Collections.unmodifiableSet(sorted)) != null) {
                throw new IllegalArgumentException("duplicate phase " + phase);
            }
        });
        phaseAttackPatterns = Collections.unmodifiableMap(copy);
    }

    public Set<ContentId> attacksForPhase(int phase) {
        Set<ContentId> attacks = phaseAttackPatterns.get(phase);
        if (attacks == null) throw new IllegalStateException("no authored attack pool for phase " + phase);
        return attacks;
    }

    /** Strict codec kept beside the semantic type so fixture/data loaders cannot silently default missing phases. */
    public static BossCombatSemanticProfile decode(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        ContentId boss = ContentId.parse(requireString(root, "boss_profile"));
        JsonElement phaseElement = root.get("phase_attack_patterns");
        if (phaseElement == null || !phaseElement.isJsonObject()) {
            throw new IllegalArgumentException("Missing or non-object field 'phase_attack_patterns'");
        }
        Map<Integer, Set<ContentId>> phases = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : phaseElement.getAsJsonObject().entrySet()) {
            int phase;
            try { phase = Integer.parseInt(entry.getKey()); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("phase key must be an integer: " + entry.getKey(), ex); }
            if (!entry.getValue().isJsonArray()) throw new IllegalArgumentException("phase " + phase + " must be an array");
            LinkedHashSet<ContentId> attacks = new LinkedHashSet<>();
            for (JsonElement value : entry.getValue().getAsJsonArray()) {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("phase " + phase + " attack ids must be strings");
                }
                ContentId id = ContentId.parse(value.getAsString());
                if (!attacks.add(id)) throw new IllegalArgumentException("phase " + phase + " contains duplicate attack " + id);
            }
            if (phases.putIfAbsent(phase, attacks) != null) throw new IllegalArgumentException("duplicate phase " + phase);
        }
        return new BossCombatSemanticProfile(boss, phases);
    }

    private static String requireString(JsonObject root, String key) {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing or non-string field '" + key + "'");
        }
        String text = value.getAsString().trim();
        if (text.isEmpty()) throw new IllegalArgumentException("Field '" + key + "' must not be blank");
        return text;
    }
}

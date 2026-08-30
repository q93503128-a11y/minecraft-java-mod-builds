package io.github.q93503128.turnbound.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.q93503128.turnbound.combat.BattleStats;
import io.github.q93503128.turnbound.combat.CombatantDefinition;
import io.github.q93503128.turnbound.combat.EffectType;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import io.github.q93503128.turnbound.combat.SkillEffect;
import io.github.q93503128.turnbound.combat.TargetRule;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** v0.4 canonical data loader. Per-character/enemy numeric branches are stored in bundled JSON definitions. */
public final class CanonicalData {
    public static final int SCHEMA_VERSION = 4;
    private static final Map<String, JsonObject> DEFINITIONS = loadDefinitions();

    private CanonicalData() {}

    public static Set<String> ids() { return Set.copyOf(DEFINITIONS.keySet()); }
    public static boolean contains(String id) { return DEFINITIONS.containsKey(id); }

    public static CombatantDefinition definition(String id) {
        JsonObject raw = raw(id);
        int stars = integer(raw, "nativeStars", 0);
        return definition(id, 1, stars, false);
    }

    public static CombatantDefinition definition(String id, int level, int currentStars, boolean awakened) {
        JsonObject raw = raw(id);
        JsonObject stats = raw.getAsJsonObject("stats");
        int nativeStars = integer(raw, "nativeStars", 0);
        String rank = string(raw, "rank", nativeStars > 0 ? "PLAYABLE" : "NORMAL");
        int safeLevel = Math.max(1, Math.min(60, level));
        int safeStars = nativeStars == 0 ? 0 : Math.max(nativeStars, Math.min(6, currentStars));

        double levelMultiplier = 1.0 + 0.045 * (safeLevel - 1);
        double starMultiplier = nativeStars == 0 ? 1.0 : starMultiplier(nativeStars, safeStars);
        int hp;
        int attack;
        int defense;
        int speed = integer(stats, "speed", 100);
        if (hasRule(raw, "BOSS")) {
            // Boss table in v0.4 is already the final stat line at its canonical encounter level.
            hp = integer(stats, "hp", 1);
            attack = integer(stats, "attack", 1);
            defense = integer(stats, "defense", 0);
        } else {
            double hpRank = "ELITE".equals(rank) ? 1.80 : 1.0;
            double atkRank = "ELITE".equals(rank) ? 1.15 : 1.0;
            double defRank = "ELITE".equals(rank) ? 1.10 : 1.0;
            hp = floor(integer(stats, "hp", 1) * levelMultiplier * starMultiplier * hpRank);
            attack = floor(integer(stats, "attack", 1) * levelMultiplier * starMultiplier * atkRank);
            defense = floor(integer(stats, "defense", 0) * levelMultiplier * starMultiplier * defRank);
        }

        List<String> rules = new ArrayList<>(strings(raw.getAsJsonArray("rules")));
        if (awakened && !rules.contains("AWAKENED")) rules.add("AWAKENED");
        Map<String, Double> params = numbers(raw.getAsJsonObject("params"));
        params = new LinkedHashMap<>(params);
        params.put("level", (double)safeLevel);
        params.put("currentStars", (double)safeStars);

        List<SkillDefinition> skills = new ArrayList<>();
        for (JsonElement item : raw.getAsJsonArray("skills")) skills.add(skill(item.getAsJsonObject()));
        String canonicalBasicId = string(raw, "basicSkillId", "");
        String runtimeBasicId = CharacterSkillRegistry.runtimeSkillId(canonicalBasicId);
        return new CombatantDefinition(
                string(raw, "id", id), string(raw, "name", id),
                new BattleStats(hp, attack, defense, speed), runtimeBasicId, skills,
                nativeStars, rules, params);
    }

    public static String rank(String id) { return string(raw(id), "rank", "NORMAL"); }
    public static JsonObject rawCopy(String id) { return raw(id).deepCopy(); }

    public static double levelMultiplier(int level) { return 1.0 + 0.045 * (Math.max(1, Math.min(60, level)) - 1); }

    public static double starMultiplier(int nativeStars, int currentStars) {
        double value = 1.0;
        for (int star = Math.max(1, nativeStars); star < Math.min(6, currentStars); star++) {
            value *= switch (star) {
                case 1 -> 1.06;
                case 2 -> 1.07;
                case 3 -> 1.08;
                case 4 -> 1.10;
                case 5 -> 1.12;
                default -> 1.0;
            };
        }
        return value;
    }

    public static int levelCap(int stars) {
        return switch (Math.max(1, Math.min(6, stars))) {
            case 1 -> 10; case 2 -> 20; case 3 -> 30; case 4 -> 40; case 5 -> 50; default -> 60;
        };
    }

    private static SkillDefinition skill(JsonObject raw) {
        List<SkillEffect> effects = new ArrayList<>();
        for (JsonElement item : raw.getAsJsonArray("effects")) {
            JsonObject e = item.getAsJsonObject();
            effects.add(new SkillEffect(
                    EffectType.valueOf(string(e, "type", "NOOP")),
                    number(e, "magnitude", 0.0), integer(e, "flatValue", 0), integer(e, "duration", 0), string(e, "key", "")));
        }
        String canonicalId = string(raw, "id", "");
        String runtimeId = CharacterSkillRegistry.runtimeSkillId(canonicalId);
        return new SkillDefinition(
                runtimeId, string(raw, "name", ""),
                TargetRule.valueOf(string(raw, "target", "SELF")), integer(raw, "cooldown", 0), effects,
                string(raw, "description", string(raw, "name", "스킬")),
                strings(raw.getAsJsonArray("rules")), numbers(raw.getAsJsonObject("params")));
    }

    private static Map<String, JsonObject> loadDefinitions() {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        loadInto(out, "/data/turnbound/characters/v04.json");
        loadInto(out, "/data/turnbound/enemies/v04.json");
        return Map.copyOf(out);
    }

    private static void loadInto(Map<String, JsonObject> out, String resource) {
        try (InputStream stream = CanonicalData.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND data resource " + resource);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = integer(root, "schemaVersion", -1);
            if (schema != SCHEMA_VERSION) throw new IllegalStateException("Unsupported TURNBOUND data schema " + schema + " at " + resource);
            for (JsonElement element : root.getAsJsonArray("definitions")) {
                JsonObject definition = element.getAsJsonObject();
                String id = string(definition, "id", "");
                if (id.isBlank() || out.put(id, definition) != null) throw new IllegalStateException("Duplicate/blank TURNBOUND definition " + id);
            }
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading " + resource, ex);
        }
    }

    private static JsonObject raw(String id) {
        JsonObject raw = DEFINITIONS.get(id);
        if (raw == null) throw new IllegalArgumentException("Unknown canonical combatant " + id);
        return raw;
    }

    private static boolean hasRule(JsonObject raw, String rule) { return strings(raw.getAsJsonArray("rules")).contains(rule); }
    private static int floor(double value) { return Math.max(1, (int)Math.floor(value)); }
    private static String string(JsonObject o, String key, String fallback) { return o != null && o.has(key) ? o.get(key).getAsString() : fallback; }
    private static int integer(JsonObject o, String key, int fallback) { return o != null && o.has(key) ? o.get(key).getAsInt() : fallback; }
    private static double number(JsonObject o, String key, double fallback) { return o != null && o.has(key) ? o.get(key).getAsDouble() : fallback; }
    private static List<String> strings(JsonArray array) {
        if (array == null) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement e : array) out.add(e.getAsString());
        return List.copyOf(out);
    }
    private static Map<String, Double> numbers(JsonObject object) {
        if (object == null) return Map.of();
        Map<String, Double> out = new LinkedHashMap<>();
        for (var e : object.entrySet()) if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) out.put(e.getKey(), e.getValue().getAsDouble());
        return Map.copyOf(out);
    }
}

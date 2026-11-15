package kr.moonseungjun.anomalousclasses.state;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

/**
 * Initial server-owned host adapters. These values are the existing v7 design
 * candidates, NOT final approved balance. Unimplemented boss/mod controls stay
 * explicitly unsupported rather than pretending to possess every LivingEntity.
 */
public final class ParasiteRules {
    /** One catalog entry is a declared, audited original-host control adapter. */
    public record HostSpec(long requiredBd, int intrusionTicks, double speedFactor,
                           String adapter, double innateJump, String expectedClass,
                           boolean wallClimb) {
        public HostSpec {
            if (requiredBd < 0 || intrusionTicks < 10 || intrusionTicks > 1200
                    || !Double.isFinite(speedFactor) || speedFactor < .2 || speedFactor > 3.0
                    || !("ground".equals(adapter) || "aquatic".equals(adapter))
                    || !Double.isFinite(innateJump) || innateJump < .42 || innateJump > .8
                    || expectedClass == null || expectedClass.length() > 220)
                throw new IllegalArgumentException("Invalid parasite host profile");
        }
        public HostSpec(long bd, int ticks, double speed, String adapter, double jump) {
            this(bd, ticks, speed, adapter, jump, "", false);
        }
        /** Preserve the original three-argument API used by server rules/tests. */
        public HostSpec(long bd, int ticks, double speed) {
            this(bd, ticks, speed, "ground", .42, "", false);
        }
        /** Pin non-vanilla hosts to the observed, exact Java class for that release. */
        public boolean matchesClass(String realClass) {
            return expectedClass.isEmpty() || expectedClass.equals(realClass);
        }
    }
    private static final Map<String, HostSpec> SUPPORTED = loadCatalog();
    private static final List<Map.Entry<String, HostSpec>> JUMP_TRAITS = SUPPORTED.entrySet().stream()
            .filter(entry -> entry.getValue().innateJump() > .42)
            .toList();
    private static final List<Map.Entry<String, HostSpec>> AQUATIC_TRAITS = SUPPORTED.entrySet().stream()
            .filter(entry -> "aquatic".equals(entry.getValue().adapter())).toList();
    private static final List<Map.Entry<String, HostSpec>> CLIMB_TRAITS = SUPPORTED.entrySet().stream()
            .filter(entry -> entry.getValue().wallClimb()).toList();

    /**
     * Bundled data catalog. Adding a modded host requires an exact EntityType ID,
     * audited Mob movement/ownership behavior, and a dedicated compatibility test.
     * Unknown modded entities and scripted bosses are NOT automatically eligible.
     */
    private static Map<String, HostSpec> loadCatalog() {
        String resource = "/data/anomalousclasses/parasite/hosts.json";
        try (InputStream stream = ParasiteRules.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing host catalog: " + resource);
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"anomalousclasses:parasite_hosts_v1".equals(root.get("schema").getAsString()))
                throw new IllegalStateException("Unsupported host catalog schema");
            Map<String, HostSpec> hosts = new HashMap<>();
            for (JsonElement entry : root.getAsJsonArray("hosts")) {
                JsonObject node = entry.getAsJsonObject();
                String id = node.get("id").getAsString();
                ResourceLocation key = ResourceLocation.tryParse(id);
                if (key == null || !id.equals(key.toString()))
                    throw new IllegalStateException("Invalid host EntityType: " + id);
                String expectedClass = node.has("expectedClass")
                        ? node.get("expectedClass").getAsString() : "";
                if (!"minecraft".equals(key.getNamespace()) && expectedClass.isBlank())
                    throw new IllegalStateException("Nonvanilla host must pin observed class: " + id);
                HostSpec spec = new HostSpec(node.get("requiredBd").getAsLong(),
                        node.get("intrusionTicks").getAsInt(),
                        node.get("speedFactor").getAsDouble(),
                        node.get("adapter").getAsString(), node.get("innateJump").getAsDouble(),
                        expectedClass, node.has("wallClimb") && node.get("wallClimb").getAsBoolean());
                if (hosts.putIfAbsent(id, spec) != null)
                    throw new IllegalStateException("Duplicate host EntityType: " + id);
            }
            if (hosts.isEmpty() || !hosts.containsKey("minecraft:pig"))
                throw new IllegalStateException("No starter host declared");
            return Map.copyOf(hosts);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to load parasite host profiles", exception);
        }
    }
    /** Concrete registered profiles only. No tag-based wildcard support for third-party bosses. */
    public static int supportedCount() { return SUPPORTED.size(); }
    private static final double[] FOCUS = {0.05, 0.10, 0.15, 0.20, 0.40};
    private ParasiteRules() {}

    public static HostSpec supported(ResourceLocation id) {
        return id == null ? null : SUPPORTED.get(id.toString());
    }
    /** Fail closed after a mod update changes the actual class behind an old ID. */
    public static boolean matchesHostClass(ResourceLocation id, String actualClass) {
        HostSpec spec = supported(id);
        return spec != null && actualClass != null && spec.matchesClass(actualClass);
    }
    /** Native adapted bodies swim; mastered aquatic species grant the same passive. */
    public static boolean hasAquaticAdaptation(String species, ParasiteSavedData.State progression) {
        HostSpec nativeSpec = SUPPORTED.get(species);
        if (nativeSpec != null && "aquatic".equals(nativeSpec.adapter())) return true;
        for (var entry : AQUATIC_TRAITS)
            if (progression.mastery(entry.getKey()) >= 100) return true;
        return false;
    }
    /** Environmental wall climbing is unconditional for the native species and mastery100. */
    public static boolean hasWallClimb(String species, ParasiteSavedData.State progression) {
        HostSpec nativeSpec = SUPPORTED.get(species);
        if (nativeSpec != null && nativeSpec.wallClimb()) return true;
        for (var entry : CLIMB_TRAITS)
            if (progression.mastery(entry.getKey()) >= 100) return true;
        return false;
    }
    public static long plannedRequirement(ResourceLocation id) {
        if (id == null) return -1;
        if ("minecraft:warden".equals(id.toString())) return 5000;
        HostSpec spec = supported(id);
        return spec == null ? -1 : spec.requiredBd();
    }
    public static int rank(PlayerClassProfile profile, String id) {
        if (profile == null || id == null) return 0;
        var tree = GraphSkillTreeCatalog.tree(ClassId.PARASITE_MORPHER);
        var skill = tree.skills().get(id);
        if (skill == null) return 0;
        var purchased = profile.progress(ClassId.PARASITE_MORPHER).purchasedNodes();
        int ranks = 0;
        for (var segment : skill.segments()) {
            if (purchased.contains(segment.majorNodeId())) ranks += segment.oldRankTo() - segment.oldRankFrom() + 1;
        }
        return ranks;
    }
    public static double focus(PlayerClassProfile profile) {
        if (profile == null) return 0;
        var item = profile.equippedFocus().orElse(null);
        return item != null && item.classId() == ClassId.PARASITE_MORPHER
                ? FOCUS[Math.max(0, Math.min(4, item.tier().index()))] : 0;
    }
    public static double healthMultiplier(long bd, int mastery, PlayerClassProfile profile) {
        return bounded(1 + 0.05 * Math.sqrt(Math.max(0, bd) / 100.0))
                * (1 + Math.min(100, Math.max(0, mastery)) * 0.0005)
                * (1 + rank(profile, "tissue_reinforcement") * 0.0035)
                * (1 + focus(profile));
    }
    public static double damageMultiplier(long bd, int mastery, PlayerClassProfile profile) {
        return bounded(1 + 0.06 * Math.sqrt(Math.max(0, bd) / 100.0))
                * (1 + Math.min(100, Math.max(0, mastery)) * 0.0005)
                * (1 + rank(profile, "attack_adaptation") * 0.0035)
                * (1 + focus(profile));
    }
    /** Slime's innate elasticity and its mastery-100 passive are always-on environmental traits. */
    public static boolean hasSlimeElasticity(String species, int slimeMastery) {
        return "minecraft:slime".equals(species) || slimeMastery >= 100;
    }

    /** The normal vanilla jump remains available for every other species. */
    public static double jumpImpulse(String species, int slimeMastery) {
        return "minecraft:slime".equals(species) ? 0.58
                : slimeMastery >= 100 ? 0.50 : 0.42;
    }

    /**
     * Ground-jump inheritance uses the same persisted species mastery as combat.
     * New ordinary species can declare their native jump in JSON; at mastery 100
     * this same movement becomes usable on every later host, without more slots.
     */
    public static double jumpImpulse(String species, ParasiteSavedData.State progression) {
        double jump = jumpImpulse(species, progression.mastery("minecraft:slime"));
        HostSpec nativeSpec = SUPPORTED.get(species);
        if (nativeSpec != null) jump = Math.max(jump, nativeSpec.innateJump());
        for (var entry : JUMP_TRAITS)
            if (progression.mastery(entry.getKey()) >= 100)
                jump = Math.max(jump, entry.getValue().innateJump());
        return jump;
    }

    /** Only transferred elasticity reduces fall damage; native slime immunity stays vanilla. */
    public static float fallDamageFactor(String species, int slimeMastery) {
        return hasSlimeElasticity(species, slimeMastery) ? 0.5F : 1.0F;
    }

    /** Native Guardian adapts immediately; other host bodies require complete mastery. */
    public static boolean hasGuardianAdaptation(boolean nativeGuardian, int guardianMastery) {
        return nativeGuardian || guardianMastery >= 100;
    }

    /** Enderman passive dodge is a conditional projectile response, not universal immunity. */
    public static boolean hasEnderDodge(boolean nativeEnderman, int endermanMastery) {
        return nativeEnderman || endermanMastery >= 100;
    }

    public static double movementMultiplier(PlayerClassProfile profile) {
        return 1 + rank(profile, "body_control") * 0.003;
    }
    public static int infiltrationTicks(HostSpec spec, long bd, PlayerClassProfile profile) {
        double ratio = spec.requiredBd() == 0 ? 1.0
                : Math.max(1.0, Math.min(4.0, (double) bd / spec.requiredBd()));
        double bdFactor = 1.0 - 0.10 * (ratio - 1.0);
        double passive = 1.0 - 0.005 * rank(profile, "infiltration_precision");
        return (int) Math.max(Math.ceil(spec.intrusionTicks() * .55),
                Math.ceil(spec.intrusionTicks() * bdFactor * passive));
    }
    public static int lockoutTicks(PlayerClassProfile profile) {
        return Math.max(900, 1200 - 10 * rank(profile, "survival_transfer"));
    }
    public static int maxEmergencyCharges(PlayerClassProfile profile) {
        return Math.min(3, rank(profile, "survival_transfer") / 10);
    }
    public static float humanMaximum(PlayerClassProfile profile) {
        return 8.0F + 0.08F * rank(profile, "human_mimicry");
    }
    private static double bounded(double value) {
        return Math.min(100.0, Math.max(1.0, Double.isFinite(value) ? value : 1.0));
    }
}

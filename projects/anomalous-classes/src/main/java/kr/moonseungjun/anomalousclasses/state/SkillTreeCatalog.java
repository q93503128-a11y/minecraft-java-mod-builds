package kr.moonseungjun.anomalousclasses.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Stable UI metadata paired by order and Korean name with the approved cost model. */
public final class SkillTreeCatalog {
    public record Node(String id, String expectedName, String groupKey, String iconId,
                       int column, int row, int[] breakpoints, int[] rankCosts) {
        public Node {
            breakpoints = breakpoints.clone();
            rankCosts = rankCosts.clone();
        }
        @Override public int[] breakpoints() { return breakpoints.clone(); }
        @Override public int[] rankCosts() { return rankCosts.clone(); }
        public int maxRank() { return rankCosts.length; }
        public String nameKey(ClassId id) { return "skill.anomalousclasses." + id.key() + "." + this.id + ".name"; }
        public String effectKey(ClassId id) { return "skill.anomalousclasses." + id.key() + "." + this.id + ".effect"; }
        public int nextBreakpoint(int currentRank) {
            for (int breakpoint : breakpoints) if (breakpoint > currentRank) return breakpoint;
            return -1;
        }
        public int costFromTo(int currentRank, int targetRank) {
            int total = 0;
            for (int rank = Math.max(0, currentRank); rank < Math.min(maxRank(), targetRank); rank++) total += rankCosts[rank];
            return total;
        }
    }

    private record NodeMeta(String id, String name, String group, String icon, int[] breaks) {}
    private static final EnumMap<ClassId, List<Node>> TREES = load();

    private SkillTreeCatalog() {}

    public static List<Node> nodes(ClassId id) { return TREES.getOrDefault(id, List.of()); }
    public static int totalRankCount(ClassId id) { return nodes(id).stream().mapToInt(Node::maxRank).sum(); }
    public static int totalCost(ClassId id) { return nodes(id).stream().mapToInt(node -> node.costFromTo(0, node.maxRank())).sum(); }
    public static int spent(ClassId id, int[] ranks) {
        int total = 0;
        List<Node> nodes = nodes(id);
        for (int index = 0; index < Math.min(nodes.size(), ranks.length); index++) {
            int rank = Math.min(nodes.get(index).maxRank(), Math.max(0, ranks[index]));
            total += nodes.get(index).costFromTo(0, rank);
        }
        return total;
    }

    private static EnumMap<ClassId, List<Node>> load() {
        EnumMap<ClassId, List<Node>> result = new EnumMap<>(ClassId.class);
        try (var stream = SkillTreeCatalog.class.getResourceAsStream("/data/anomalousclasses/skill_point_cost_model_v2.json")) {
            if (stream == null) throw new IllegalStateException("Missing approved skill-point cost model resource");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject classes = root.getAsJsonObject("classes");
            for (ClassId classId : ClassId.values()) {
                String costKey = costKey(classId);
                JsonArray costs = classes.getAsJsonObject(costKey).getAsJsonArray("nodes");
                List<NodeMeta> metadata = metadata(classId);
                if (costs.size() != metadata.size()) throw new IllegalStateException("Node count mismatch for " + costKey);
                List<Node> nodes = new ArrayList<>(metadata.size());
                int[] groupRows = new int[3];
                for (int index = 0; index < metadata.size(); index++) {
                    NodeMeta meta = metadata.get(index);
                    JsonObject json = costs.get(index).getAsJsonObject();
                    String sourceName = json.get("name").getAsString();
                    if (!meta.name().equals(sourceName)) throw new IllegalStateException("Node order/name mismatch: " + costKey + "/" + index);
                    JsonArray ranksJson = json.getAsJsonArray("rank_costs");
                    int maxRank = json.get("max_rank").getAsInt();
                    if (maxRank != ranksJson.size()) throw new IllegalStateException("Rank cost count mismatch for " + sourceName);
                    int[] rankCosts = new int[maxRank];
                    for (int rank = 0; rank < maxRank; rank++) rankCosts[rank] = ranksJson.get(rank).getAsInt();
                    int column = groupColumn(meta.group());
                    nodes.add(new Node(meta.id(), meta.name(), groupKey(classId, meta.group()), meta.icon(),
                            column, groupRows[column]++, meta.breaks(), rankCosts));
                }
                List<Node> frozen = List.copyOf(nodes);
                int sum = frozen.stream().mapToInt(node -> node.costFromTo(0, node.maxRank())).sum();
                if (sum != 900) throw new IllegalStateException("Skill tree must total 900 SP: " + costKey + "=" + sum);
                result.put(classId, frozen);
            }
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
        return result;
    }

    private static String costKey(ClassId id) {
        return switch (id) {
            case MENTAL_DOMINATOR -> "mental";
            case PUPPETEER -> "puppet";
            case PARASITE_MORPHER -> "parasite";
            case BARRIER_CASTER -> "barrier";
            case PACT_CASTER -> "pact";
            case SWARM_CASTER -> "swarm";
        };
    }

    private static String groupKey(ClassId id, String group) {
        return "skill_group.anomalousclasses." + id.key() + "." + group;
    }

    private static int groupColumn(String group) {
        return switch (group) { case "one" -> 0; case "two" -> 1; default -> 2; };
    }

    private static NodeMeta n(String id, String name, String group, String icon, int... breaks) {
        return new NodeMeta(id, name, group, icon, breaks);
    }

    private static List<NodeMeta> metadata(ClassId id) {
        return switch (id) {
            case MENTAL_DOMINATOR -> List.of(
                    n("deep_infiltration", "심층 침투", "one", "spyglass", 10,20,30),
                    n("resistance_piercing", "저항 관통", "one", "amethyst_shard", 10,20,30),
                    n("domination_stability", "지배 안정화", "one", "totem_of_undying", 10,20),
                    n("mental_bond", "정신 결속", "two", "string", 10,20,30,40),
                    n("multiple_bonds", "다중 결속", "two", "chain", 5,10,15,20,25,30),
                    n("distributed_attunement", "분산 동조", "two", "echo_shard", 10,20,30),
                    n("shock_echo", "충격 잔향", "three", "ender_eye", 10,20),
                    n("suggestion_depth", "암시 심화", "three", "sculk_sensor", 10,20,25),
                    n("overload_duration", "과부하 지속", "three", "soul_torch", 10,20,25),
                    n("containment_expansion", "봉쇄 확장", "three", "redstone", 10,20));
            case PUPPETEER -> List.of(
                    n("frame_refinement", "골격 정밀화", "one", "armor_stand", 10,20,30),
                    n("material_use", "소재 활용", "one", "iron_ingot", 10,20,25),
                    n("module_adaptation", "모듈 적응", "one", "redstone", 10,20,30),
                    n("connection_strength", "연결 강도", "two", "string", 10,20,30,40),
                    n("spare_threads", "여분의 실", "two", "chain", 10,20,30),
                    n("response_attunement", "반응 동조", "two", "target", 5,10,20,25),
                    n("weapon_tuning", "무장 조율", "three", "iron_sword", 10,20,30),
                    n("guarded_joints", "방호 관절", "three", "shield", 10,20,30),
                    n("mobility_axis", "기동 축", "three", "piston", 5,10,20,25),
                    n("emergency_repair", "긴급 복원", "three", "anvil", 10,20,25));
            case PARASITE_MORPHER -> List.of(
                    n("infiltration_precision", "침투 정밀화", "one", "fermented_spider_eye", 10,20,30),
                    n("infiltration_resistance", "침투 저항", "one", "honeycomb", 5,15,25),
                    n("biological_analysis", "생체 해석", "one", "knowledge_book", 10,20,30),
                    n("species_adaptation", "종별 융화", "one", "compass", 5,15,25),
                    n("tissue_reinforcement", "조직 강화", "two", "rotten_flesh", 10,20,35),
                    n("attack_adaptation", "공격 적응", "two", "iron_sword", 10,20,30),
                    n("body_control", "신체 운용", "two", "leather_boots", 5,15,25),
                    n("survival_transfer", "생존 전이", "three", "totem_of_undying", 10,20,30),
                    n("self_repair", "자가 수복", "three", "golden_apple", 10,20,30),
                    n("human_mimicry", "인간 의태", "three", "player_head", 5,15,25));
            case BARRIER_CASTER -> List.of(
                    n("spatial_aiming", "공간 조준", "one", "amethyst_shard"),
                    n("lasting_inscription", "지속 각인", "one", "clock"),
                    n("multiple_deployment", "다중 전개", "one", "redstone", 15,35),
                    n("compression_density", "압축 밀도", "two", "tnt"),
                    n("focused_pressure", "압박 집중", "two", "fire_charge"),
                    n("barrier_link", "결계 연계", "two", "chain"),
                    n("defense_density", "방호 밀도", "three", "shield"),
                    n("sealing_strength", "봉쇄 강도", "three", "iron_bars"),
                    n("quick_barrier", "즉응 방벽", "three", "iron_door"));
            case PACT_CASTER -> List.of(
                    n("flesh_inscription", "육신 각인", "one", "heart_of_the_sea"),
                    n("martial_attunement", "무력 동화", "one", "iron_sword"),
                    n("crisis_resistance", "위기 저항", "one", "shield", 20),
                    n("power_condensation", "권능 응축", "two", "nether_star"),
                    n("woven_arms", "무장 직조", "two", "blaze_rod", 10,20,30),
                    n("ritual_cycle", "술식 순환", "two", "clock"),
                    n("relationship_resonance", "관계 공명", "three", "emerald"),
                    n("deepening_internalization", "심화 내재화", "three", "gold_ingot"),
                    n("dual_attunement", "이중 조율", "three", "ender_eye", 10,20,30));
            case SWARM_CASTER -> List.of(
                    n("sharp_jaws", "날카로운 턱", "one", "bone"),
                    n("assault_tactics", "급습 전술", "one", "iron_sword", 20),
                    n("sacrifice_instinct", "희생 본능", "one", "gunpowder", 30),
                    n("hard_shell", "단단한 갑피", "two", "iron_chestplate"),
                    n("ballistic_sense", "탄도 감각", "two", "crossbow", 15),
                    n("escort_formation", "호위 진형", "two", "shield", 10),
                    n("natural_fission", "자연 분열", "three", "honeycomb"),
                    n("regrouping", "재결집", "three", "lead", 20),
                    n("pack_tracking", "무리 추적", "three", "spyglass"));
        };
    }
}

package kr.moonseungjun.anomalousclasses.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Runtime graph catalog loaded from the reviewed branch and layout JSON sources. */
public final class GraphSkillTreeCatalog {
    public static final int CURRENT_VERSION = 1;

    public enum Kind { NAVIGATION, MINOR, MAJOR }
    public enum PurchaseCheck { PURCHASABLE, UNKNOWN_NODE, NOT_PURCHASABLE, ALREADY_OWNED,
        NOT_CONNECTED, MISSING_REQUIREMENT, INSUFFICIENT_SP }

    public record Node(String id, Kind kind, int x, int y, int cost, String stat, String source,
                       int stage, int equivalentOldRank, boolean milestone, String requiresPreviousMajor,
                       List<String> requiresStageMinors, List<Integer> crossedMilestones, int purchaseIndex) {
        public Node {
            requiresStageMinors = List.copyOf(requiresStageMinors);
            crossedMilestones = List.copyOf(crossedMilestones);
        }
        public boolean purchasable() { return kind != Kind.NAVIGATION; }
        public boolean major() { return kind == Kind.MAJOR; }
        public String nameKey(ClassId classId) {
            if (kind == Kind.MINOR) return "skill_node.anomalousclasses." + classId.key() + ".stat." + stat;
            if (kind == Kind.MAJOR) return "skill.anomalousclasses." + classId.key() + "." + source + ".name";
            return "skill_group.anomalousclasses." + classId.key() + "." + navigationKey(id);
        }
        public String effectKey(ClassId classId) {
            return kind == Kind.MAJOR
                    ? "skill.anomalousclasses." + classId.key() + "." + source + ".effect"
                    : "skill_node.anomalousclasses." + classId.key() + "." + stat + ".effect";
        }
        public String iconPath(ClassId classId) {
            if (kind == Kind.MINOR) return "textures/gui/skilltree_v1/icons/common/" + stat + ".png";
            if (kind == Kind.MAJOR) return "textures/gui/skilltree_v1/icons/" + classId.key() + "/" + source + ".png";
            return null;
        }
    }

    public record Edge(String from, String to, String type) {
        public boolean connects(String a, String b) {
            return (from.equals(a) && to.equals(b)) || (from.equals(b) && to.equals(a));
        }
    }

    public record Segment(int stage, int oldRankFrom, int oldRankTo, int oldSegmentSp,
                          List<Integer> minorCosts, int majorCost, List<String> minorNodeIds, String majorNodeId) {
        public Segment {
            minorCosts = List.copyOf(minorCosts);
            minorNodeIds = List.copyOf(minorNodeIds);
        }
    }

    public record Skill(String legacyId, String name, String sourcePerRankEffect,
                        List<Integer> milestones, List<Segment> segments) {
        public Skill {
            milestones = List.copyOf(milestones);
            segments = List.copyOf(segments);
        }
    }

    public record Tree(List<Node> allNodes, List<Node> purchaseNodes, List<Edge> edges,
                       Map<String, Node> byId, Map<String, Skill> skills,
                       int minX, int maxX, int minY, int maxY, int totalCost) {
        public Tree {
            allNodes = List.copyOf(allNodes);
            purchaseNodes = List.copyOf(purchaseNodes);
            edges = List.copyOf(edges);
            byId = Collections.unmodifiableMap(new LinkedHashMap<>(byId));
            skills = Collections.unmodifiableMap(new LinkedHashMap<>(skills));
        }
        public Node node(String id) { return byId.get(id); }
        public Node purchaseNode(int index) {
            return index < 0 || index >= purchaseNodes.size() ? null : purchaseNodes.get(index);
        }
    }

    public record StatTotals(double maxHealth, double armor, double movementSpeedPercent, double attackDamage) {}

    private static final Map<String, Double> STANDARD_STATS = loadStatAmounts(false);
    private static final Map<String, Double> PARASITE_STATS = loadStatAmounts(true);
    private static final EnumMap<ClassId, Tree> TREES = loadTrees();

    private GraphSkillTreeCatalog() {}

    public static Tree tree(ClassId classId) { return TREES.get(classId); }

    public static int spent(ClassId classId, Set<String> purchased) {
        long spent = 0L;
        Tree tree = tree(classId);
        for (String id : purchased) {
            Node node = tree.node(id);
            if (node == null || !node.purchasable()) throw new IllegalStateException("Unknown purchased skill node: " + id);
            spent += node.cost();
        }
        if (spent > Integer.MAX_VALUE) throw new IllegalStateException("Skill-tree spend overflow");
        return (int) spent;
    }

    public static StatTotals statTotals(ClassId classId, Set<String> purchased) {
        Tree tree = tree(classId);
        Map<String, Double> amounts = amounts(classId);
        double health = 0, armor = 0, speed = 0, damage = 0;
        for (String id : purchased) {
            Node node = tree.node(id);
            if (node == null || node.kind() != Kind.MINOR) continue;
            double amount = amounts.getOrDefault(node.stat(), 0.0);
            switch (node.stat()) {
                case "max_health_hp" -> health += amount;
                case "armor" -> armor += amount;
                case "movement_speed_percent" -> speed += amount;
                case "attack_damage" -> damage += amount;
                default -> { }
            }
        }
        return new StatTotals(health, armor, speed, damage);
    }

    public static double statAmount(ClassId classId, String stat) {
        return amounts(classId).getOrDefault(stat, 0.0);
    }

    public static PurchaseCheck canPurchase(ClassId classId, Set<String> owned, int skillPoints, String nodeId) {
        Tree tree = tree(classId);
        Node node = tree.node(nodeId);
        if (node == null) return PurchaseCheck.UNKNOWN_NODE;
        if (!node.purchasable()) return PurchaseCheck.NOT_PURCHASABLE;
        if (owned.contains(node.id())) return PurchaseCheck.ALREADY_OWNED;
        if (node.requiresPreviousMajor() != null && !owned.contains(node.requiresPreviousMajor())) {
            return PurchaseCheck.MISSING_REQUIREMENT;
        }
        for (String required : node.requiresStageMinors()) {
            if (!owned.contains(required)) return PurchaseCheck.MISSING_REQUIREMENT;
        }
        if (!hasPurchasedPath(tree, node, owned)) return PurchaseCheck.NOT_CONNECTED;
        if (skillPoints < node.cost()) return PurchaseCheck.INSUFFICIENT_SP;
        return PurchaseCheck.PURCHASABLE;
    }

    public static int[] purchasedIndexes(ClassId classId, Set<String> owned) {
        Tree tree = tree(classId);
        int count = 0;
        for (Node node : tree.purchaseNodes()) if (owned.contains(node.id())) count++;
        int[] result = new int[count];
        int next = 0;
        for (Node node : tree.purchaseNodes()) if (owned.contains(node.id())) result[next++] = node.purchaseIndex();
        return result;
    }

    private static boolean hasPurchasedPath(Tree tree, Node node, Set<String> owned) {
        for (Edge edge : tree.edges()) {
            if (edge.type().equals("growth") && edge.to().equals(node.id())) {
                Node parent = tree.node(edge.from());
                if (parent != null && (!parent.purchasable() || owned.contains(parent.id()))) return true;
            }
            if (edge.type().equals("bridge") && (edge.from().equals(node.id()) || edge.to().equals(node.id()))) {
                String otherId = edge.from().equals(node.id()) ? edge.to() : edge.from();
                Node other = tree.node(otherId);
                if (other != null && other.purchasable() && owned.contains(otherId)) return true;
            }
        }
        return false;
    }

    private static EnumMap<ClassId, Tree> loadTrees() {
        EnumMap<ClassId, Tree> result = new EnumMap<>(ClassId.class);
        JsonObject branchRoot = readJson("/data/anomalousclasses/skill_tree_branch_redesign_v1.json");
        JsonObject layoutRoot = readJson("/data/anomalousclasses/skill_tree_graph_layout_v1.json");
        JsonObject branchClasses = branchRoot.getAsJsonObject("classes");
        JsonObject layoutClasses = layoutRoot.getAsJsonObject("classes");
        for (ClassId classId : ClassId.values()) {
            String key = dataKey(classId);
            JsonObject branch = branchClasses.getAsJsonObject(key);
            JsonObject layout = layoutClasses.getAsJsonObject(key);
            if (branch == null || layout == null) throw new IllegalStateException("Missing skill-tree class data: " + key);
            Map<String, Skill> skills = loadSkills(branch);
            List<JsonObject> nodeJson = objects(layout.getAsJsonArray("nodes"));
            Map<String, Node> byId = new LinkedHashMap<>();
            List<Node> all = new ArrayList<>(nodeJson.size());
            List<Node> purchase = new ArrayList<>();
            int purchaseIndex = 0;
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            for (JsonObject value : nodeJson) {
                String id = value.get("id").getAsString();
                Kind kind = Kind.valueOf(value.get("kind").getAsString().toUpperCase(java.util.Locale.ROOT));
                String source = nullableString(value, "source");
                int stage = integer(value, "stage", 0);
                int equivalentRank = integer(value, "equivalent_old_rank", 0);
                List<String> stageMinors = strings(value.getAsJsonArray("requires_stage_minors"));
                List<Integer> crossedMilestones = kind == Kind.MAJOR
                        ? crossedMilestones(skills.get(source), stage) : List.of();
                Node node = new Node(id, kind, value.get("x").getAsInt(), value.get("y").getAsInt(),
                        integer(value, "cost", 0), nullableString(value, "stat"), source, stage,
                        equivalentRank, !crossedMilestones.isEmpty(), nullableString(value, "requires_previous_major"),
                        stageMinors, crossedMilestones, kind == Kind.NAVIGATION ? -1 : purchaseIndex++);
                if (byId.putIfAbsent(id, node) != null) throw new IllegalStateException("Duplicate skill node: " + id);
                all.add(node);
                if (node.purchasable()) purchase.add(node);
                minX = Math.min(minX, node.x()); maxX = Math.max(maxX, node.x());
                minY = Math.min(minY, node.y()); maxY = Math.max(maxY, node.y());
            }
            List<Edge> edges = new ArrayList<>();
            for (JsonObject value : objects(layout.getAsJsonArray("edges"))) {
                String from = value.get("from").getAsString();
                String to = value.get("to").getAsString();
                String type = value.get("type").getAsString();
                if (!byId.containsKey(from) || !byId.containsKey(to)) throw new IllegalStateException("Invalid skill edge: " + from + " -> " + to);
                edges.add(new Edge(from, to, type));
            }
            validateClassGraph(key, branch, layout, skills, byId, purchase, edges);
            int totalCost = purchase.stream().mapToInt(Node::cost).sum();
            result.put(classId, new Tree(all, purchase, edges, byId, skills, minX, maxX, minY, maxY, totalCost));
        }
        int totalPurchaseNodes = result.values().stream().mapToInt(value -> value.purchaseNodes().size()).sum();
        if (totalPurchaseNodes != 581) throw new IllegalStateException("Expected 581 skill nodes, found " + totalPurchaseNodes);
        return result;
    }

    private static void validateClassGraph(String classKey, JsonObject branch, JsonObject layout,
                                           Map<String, Skill> skills, Map<String, Node> byId,
                                           List<Node> purchaseNodes, List<Edge> edges) {
        JsonObject expected = branch.getAsJsonObject("derived");
        long minorCount = purchaseNodes.stream().filter(node -> node.kind() == Kind.MINOR).count();
        long majorCount = purchaseNodes.stream().filter(Node::major).count();
        int totalCost = purchaseNodes.stream().mapToInt(Node::cost).sum();
        if (minorCount != expected.get("minor_nodes").getAsInt()
                || majorCount != expected.get("major_nodes").getAsInt()
                || totalCost != expected.get("sp_total").getAsInt() || totalCost != 900) {
            throw new IllegalStateException("Node-count or 900-SP mismatch for " + classKey);
        }
        JsonObject summary = layout.getAsJsonObject("summary");
        if (summary.get("purchase_count").getAsInt() != purchaseNodes.size()
                || summary.get("total_sp").getAsInt() != totalCost) {
            throw new IllegalStateException("Graph summary mismatch for " + classKey);
        }
        for (Node node : purchaseNodes) {
            if (node.kind() == Kind.MINOR && statAmountForValidation(classKey, node.stat()) <= 0) {
                throw new IllegalStateException("Unknown minor stat: " + classKey + "/" + node.id());
            }
            if (node.requiresPreviousMajor() != null) {
                Node previous = byId.get(node.requiresPreviousMajor());
                if (previous == null || !previous.major() || !node.source().equals(previous.source())) {
                    throw new IllegalStateException("Invalid previous-stage requirement: " + node.id());
                }
            }
            for (String required : node.requiresStageMinors()) {
                Node minor = byId.get(required);
                if (minor == null || minor.kind() != Kind.MINOR) throw new IllegalStateException("Invalid required minor: " + node.id());
            }
        }
        for (Skill skill : skills.values()) {
            for (Segment segment : skill.segments()) {
                Node major = byId.get(segment.majorNodeId());
                if (major == null || !major.major() || major.equivalentOldRank() != segment.oldRankTo()) {
                    throw new IllegalStateException("Missing or mismatched major stage: " + segment.majorNodeId());
                }
                long segmentCost = segment.majorCost();
                for (int index = 0; index < segment.minorNodeIds().size(); index++) {
                    Node minor = byId.get(segment.minorNodeIds().get(index));
                    if (minor == null || minor.kind() != Kind.MINOR || minor.cost() != segment.minorCosts().get(index)) {
                        throw new IllegalStateException("Mismatched minor stage: " + segment.minorNodeIds().get(index));
                    }
                    segmentCost += minor.cost();
                }
                if (segmentCost != segment.oldSegmentSp()) throw new IllegalStateException("Legacy segment cost changed: " + segment.majorNodeId());
            }
        }
    }

    private static Map<String, Skill> loadSkills(JsonObject classData) {
        Map<String, Skill> skills = new LinkedHashMap<>();
        for (JsonObject skillJson : objects(classData.getAsJsonArray("skills"))) {
            String id = skillJson.get("legacy_id").getAsString();
            List<Segment> segments = new ArrayList<>();
            for (JsonObject segment : objects(skillJson.getAsJsonArray("segments"))) {
                int stage = segment.get("stage").getAsInt();
                List<Integer> minorCosts = integers(segment.getAsJsonArray("minor_costs"));
                List<String> minorIds = new ArrayList<>(minorCosts.size());
                for (int index = 0; index < minorCosts.size(); index++) minorIds.add(id + ".s" + stage + ".m" + (index + 1));
                segments.add(new Segment(stage, segment.get("old_rank_from").getAsInt(), segment.get("old_rank_to").getAsInt(),
                        segment.get("old_segment_sp").getAsInt(), minorCosts, segment.get("major_cost").getAsInt(),
                        minorIds, id + ".s" + stage + ".major"));
            }
            skills.put(id, new Skill(id, skillJson.get("name").getAsString(),
                    skillJson.get("source_per_rank_effect").getAsString(),
                    integers(skillJson.getAsJsonArray("catalog_milestones")), segments));
        }
        return skills;
    }

    private static List<Integer> crossedMilestones(Skill skill, int stage) {
        if (skill == null) return List.of();
        Segment current = skill.segments().stream().filter(value -> value.stage() == stage).findFirst().orElse(null);
        if (current == null) throw new IllegalStateException("Missing skill stage " + skill.legacyId() + "/" + stage);
        int lowerBound = current.oldRankFrom();
        return skill.milestones().stream().filter(rank -> rank > lowerBound && rank <= current.oldRankTo()).toList();
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = GraphSkillTreeCatalog.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing skill-tree data resource " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Map<String, Double> loadStatAmounts(boolean parasite) {
        JsonObject root = readJson("/data/anomalousclasses/skill_tree_branch_redesign_v1.json");
        JsonObject source = root.getAsJsonObject("rules").getAsJsonObject(
                parasite ? "parasite_minor_stat_amounts" : "minor_stat_amounts");
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) result.put(entry.getKey(), entry.getValue().getAsDouble());
        return Map.copyOf(result);
    }

    private static Map<String, Double> amounts(ClassId classId) {
        return classId == ClassId.PARASITE_MORPHER ? PARASITE_STATS : STANDARD_STATS;
    }

    private static double statAmountForValidation(String classKey, String stat) {
        return (classKey.equals("parasite") ? PARASITE_STATS : STANDARD_STATS).getOrDefault(stat, 0.0);
    }

    private static List<JsonObject> objects(JsonArray array) {
        List<JsonObject> result = new ArrayList<>(array.size());
        for (JsonElement element : array) result.add(element.getAsJsonObject());
        return result;
    }

    private static List<String> strings(JsonArray array) {
        if (array == null) return List.of();
        List<String> result = new ArrayList<>(array.size());
        for (JsonElement element : array) result.add(element.getAsString());
        return result;
    }

    private static List<Integer> integers(JsonArray array) {
        if (array == null) return List.of();
        List<Integer> result = new ArrayList<>(array.size());
        for (JsonElement element : array) result.add(element.getAsInt());
        return result;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
    }

    private static String nullableString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private static String dataKey(ClassId classId) {
        return switch (classId) {
            case MENTAL_DOMINATOR -> "mental";
            case PUPPETEER -> "puppet";
            case PARASITE_MORPHER -> "parasite";
            case BARRIER_CASTER -> "barrier";
            case PACT_CASTER -> "pact";
            case SWARM_CASTER -> "swarm";
        };
    }

    private static String navigationKey(String id) {
        return switch (id) {
            case "gate.one" -> "one";
            case "gate.two" -> "two";
            case "gate.three" -> "three";
            default -> "one";
        };
    }
}

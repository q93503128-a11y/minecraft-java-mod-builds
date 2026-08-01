package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class VillageSkillTreeSystem {
    private static final Map<UUID, Integer> UNLOCKED_MASKS = new LinkedHashMap<>();
    private static VillageSkillTreeData savedData;

    private VillageSkillTreeSystem() {
    }

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageSkillTreeData.TYPE);
        UNLOCKED_MASKS.clear();
        UNLOCKED_MASKS.putAll(savedData.masks());
        persist();
    }

    public static int earnedPoints(ServerPlayer player) {
        int level = VillageCouncilState.levelOf(player.getUUID());
        return Math.max(0, (level - 1) / 3);
    }

    public static int spentPoints(ServerPlayer player) {
        int spent = 0;
        for (Node node : Node.values()) {
            if (has(player, node)) {
                spent++;
            }
        }
        return spent;
    }

    public static boolean hasValidAllocation(ServerPlayer player) {
        return spentPoints(player) <= earnedPoints(player);
    }

    public static int availablePoints(ServerPlayer player) {
        return Math.max(0, earnedPoints(player) - spentPoints(player));
    }

    public static synchronized boolean has(ServerPlayer player, Node node) {
        int mask = UNLOCKED_MASKS.getOrDefault(player.getUUID(), 0);
        return (mask & bit(node)) != 0;
    }

    public static synchronized String purchase(ServerPlayer player, String nodeId) {
        if (!hasValidAllocation(player)) {
            return "사용한 포인트가 획득 포인트보다 많아 안전 잠금되었습니다.";
        }
        Node node = Node.parse(nodeId).orElse(null);
        if (node == null) {
            return "알 수 없는 전술 노드입니다.";
        }
        if (has(player, node)) {
            return node.title() + "은(는) 이미 습득했습니다.";
        }
        if (node.prerequisite() != null && !has(player, node.prerequisite())) {
            return "먼저 " + node.prerequisite().title() + "을(를) 습득해야 합니다.";
        }
        if (availablePoints(player) <= 0) {
            return "사용 가능한 전술 포인트가 없습니다. 3레벨마다 1포인트를 얻습니다.";
        }
        int mask = UNLOCKED_MASKS.getOrDefault(player.getUUID(), 0);
        UNLOCKED_MASKS.put(player.getUUID(), mask | bit(node));
        persist();
        return node.title() + " 습득 완료 | 남은 포인트 " + availablePoints(player);
    }

    public static String nodeStatus(ServerPlayer player, Node node) {
        if (has(player, node)) {
            return "습득";
        }
        if (!hasValidAllocation(player)) {
            return "데이터 잠금";
        }
        if (node.prerequisite() != null && !has(player, node.prerequisite())) {
            return "잠김";
        }
        return availablePoints(player) > 0 ? "습득 가능" : "포인트 필요";
    }

    public static float outgoingDamageMultiplier(ServerPlayer player) {
        float bonus = 0.0f;
        if (has(player, Node.POWER_1)) bonus += 0.06f;
        if (has(player, Node.POWER_2)) bonus += 0.06f;
        if (has(player, Node.POWER_4)) bonus += 0.08f;
        return 1.0f + bonus;
    }

    public static float executionMultiplier(ServerPlayer player, float health, float maximum) {
        return has(player, Node.POWER_3) && maximum > 0.0f && health / maximum <= 0.30f
                ? 1.18f : 1.0f;
    }

    public static float projectileDamageMultiplier(ServerPlayer player) {
        float bonus = 0.0f;
        if (has(player, Node.RANGED_1)) bonus += 0.08f;
        if (has(player, Node.RANGED_4)) bonus += 0.10f;
        return 1.0f + bonus;
    }

    public static int projectileFireBonusTicks(ServerPlayer player) {
        return has(player, Node.RANGED_2) ? 70 : 0;
    }

    public static int extraRicochetTargets(ServerPlayer player) {
        int extra = 0;
        if (has(player, Node.RANGED_3)) extra++;
        if (has(player, Node.RANGED_5)) extra += 2;
        return extra;
    }

    public static float incomingDamageMultiplier(ServerPlayer player) {
        float reduction = 0.0f;
        if (has(player, Node.GUARD_1)) reduction += 0.05f;
        if (has(player, Node.GUARD_2)) reduction += 0.05f;
        if (has(player, Node.GUARD_4)) reduction += 0.06f;
        return Math.max(0.72f, 1.0f - reduction);
    }

    public static float lowHealthIncomingMultiplier(ServerPlayer player) {
        return has(player, Node.GUARD_3) && player.getHealth() <= player.getMaxHealth() * 0.35f
                ? 0.82f : 1.0f;
    }

    public static boolean emergencyBarrierUnlocked(ServerPlayer player) {
        return has(player, Node.GUARD_5);
    }

    public static float coinRewardMultiplier(ServerPlayer player) {
        float bonus = 0.0f;
        if (has(player, Node.SUPPORT_1)) bonus += 0.08f;
        if (has(player, Node.SUPPORT_4)) bonus += 0.10f;
        return 1.0f + bonus;
    }

    public static int cooldownReductionSeconds(ServerPlayer player) {
        int reduction = 0;
        if (has(player, Node.SUPPORT_2)) reduction += 2;
        if (has(player, Node.SUPPORT_4)) reduction += 2;
        return reduction;
    }

    public static boolean sharedSupplyChanceUnlocked(ServerPlayer player) {
        return has(player, Node.SUPPORT_3);
    }

    public static float killHealAmount(ServerPlayer player) {
        float amount = 0.0f;
        if (has(player, Node.POWER_5)) amount += 2.0f;
        if (has(player, Node.SUPPORT_5)) amount += 1.0f;
        return amount;
    }

    public static int branchRanks(ServerPlayer player, Branch branch) {
        int count = 0;
        for (Node node : Node.values()) {
            if (node.branch() == branch && has(player, node)) {
                count++;
            }
        }
        return count;
    }

    public static List<Node> nodes() {
        return List.of(Node.values());
    }

    private static int bit(Node node) {
        return 1 << node.ordinal();
    }

    private static void persist() {
        if (savedData != null) {
            savedData.replace(UNLOCKED_MASKS);
        }
    }

    public enum Branch {
        POWER("공격"),
        GUARD("방어"),
        SUPPORT("지원"),
        RANGED("사격");

        private final String displayName;

        Branch(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum Node {
        POWER_1("power_1", "예리한 공세", "모든 공격 피해 +6%", Branch.POWER, 1, null),
        POWER_2("power_2", "전투 가속", "모든 공격 피해 추가 +6%", Branch.POWER, 2, POWER_1),
        POWER_3("power_3", "처형 본능", "체력이 30% 이하인 적에게 피해 +18%", Branch.POWER, 3, POWER_2),
        POWER_4("power_4", "파쇄 집중", "모든 공격 피해 추가 +8%", Branch.POWER, 4, POWER_3),
        POWER_5("power_5", "피의 환류", "적 처치 시 체력 1칸 회복", Branch.POWER, 5, POWER_4),

        GUARD_1("guard_1", "강철 방어", "받는 피해 5% 감소", Branch.GUARD, 1, null),
        GUARD_2("guard_2", "충격 분산", "받는 피해 추가 5% 감소", Branch.GUARD, 2, GUARD_1),
        GUARD_3("guard_3", "최후의 방벽", "체력 35% 이하에서 받는 피해 추가 18% 감소", Branch.GUARD, 3, GUARD_2),
        GUARD_4("guard_4", "중갑 적응", "받는 피해 추가 6% 감소", Branch.GUARD, 4, GUARD_3),
        GUARD_5("guard_5", "응급 장막", "위기 상황에서 역할 기술의 보호 효과가 강화됨", Branch.GUARD, 5, GUARD_4),

        SUPPORT_1("support_1", "전리품 감정", "처치 주화 +8%", Branch.SUPPORT, 1, null),
        SUPPORT_2("support_2", "빠른 재정비", "장착 기술 재사용 대기시간 -2초", Branch.SUPPORT, 2, SUPPORT_1),
        SUPPORT_3("support_3", "공동 회수", "적 처치 시 낮은 확률로 공동 보급품 획득", Branch.SUPPORT, 3, SUPPORT_2),
        SUPPORT_4("support_4", "숙련된 지휘", "처치 주화 +10%, 기술 재사용 -2초", Branch.SUPPORT, 4, SUPPORT_3),
        SUPPORT_5("support_5", "전선 회복", "적 처치 시 체력 0.5칸 회복", Branch.SUPPORT, 5, SUPPORT_4),

        RANGED_1("ranged_1", "장거리 조준", "화살과 투사체 피해 +8%", Branch.RANGED, 1, null),
        RANGED_2("ranged_2", "발화 촉", "화살 적중 시 대상을 불태움", Branch.RANGED, 2, RANGED_1),
        RANGED_3("ranged_3", "도탄 각도", "도탄 사격 대상 +1", Branch.RANGED, 3, RANGED_2),
        RANGED_4("ranged_4", "관통 장력", "화살과 투사체 피해 추가 +10%", Branch.RANGED, 4, RANGED_3),
        RANGED_5("ranged_5", "분열 사격", "연쇄 사격 대상 +2", Branch.RANGED, 5, RANGED_4);

        private final String id;
        private final String title;
        private final String description;
        private final Branch branch;
        private final int tier;
        private final Node prerequisite;

        Node(
                String id,
                String title,
                String description,
                Branch branch,
                int tier,
                Node prerequisite) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.branch = branch;
            this.tier = tier;
            this.prerequisite = prerequisite;
        }

        public String id() { return id; }
        public String title() { return title; }
        public String description() { return description; }
        public Branch branch() { return branch; }
        public int tier() { return tier; }
        public Node prerequisite() { return prerequisite; }

        public static Optional<Node> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values())
                    .filter(node -> node.id.equals(normalized))
                    .findFirst();
        }
    }
}

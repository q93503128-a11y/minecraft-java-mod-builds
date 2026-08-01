package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class VillageSkillTreeSystem {
    private static final String TAG_PREFIX = "villageguardians_skill_";

    private VillageSkillTreeSystem() {
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

    public static int availablePoints(ServerPlayer player) {
        return Math.max(0, earnedPoints(player) - spentPoints(player));
    }

    public static boolean has(ServerPlayer player, Node node) {
        return player.getCommandTags().contains(TAG_PREFIX + node.id());
    }

    public static String purchase(ServerPlayer player, String nodeId) {
        Node node = Node.parse(nodeId).orElse(null);
        if (node == null) {
            return "알 수 없는 스킬 노드입니다.";
        }
        if (has(player, node)) {
            return node.title() + "은(는) 이미 습득했습니다.";
        }
        if (node.prerequisite() != null && !has(player, node.prerequisite())) {
            return "먼저 " + node.prerequisite().title() + "을(를) 습득해야 합니다.";
        }
        if (availablePoints(player) <= 0) {
            return "사용 가능한 스킬 포인트가 없습니다. 3레벨마다 1포인트를 얻습니다.";
        }
        player.addTag(TAG_PREFIX + node.id());
        return node.title() + " 습득 완료 | 남은 포인트 " + availablePoints(player);
    }

    public static String nodeStatus(ServerPlayer player, Node node) {
        if (has(player, node)) {
            return "습득";
        }
        if (node.prerequisite() != null && !has(player, node.prerequisite())) {
            return "잠김";
        }
        return availablePoints(player) > 0 ? "습득 가능" : "포인트 필요";
    }

    public static float outgoingDamageMultiplier(ServerPlayer player) {
        return 1.0f + branchRanks(player, Branch.POWER) * 0.08f;
    }

    public static float incomingDamageMultiplier(ServerPlayer player) {
        return Math.max(0.75f, 1.0f - branchRanks(player, Branch.GUARD) * 0.07f);
    }

    public static float coinRewardMultiplier(ServerPlayer player) {
        return 1.0f + branchRanks(player, Branch.SUPPORT) * 0.12f;
    }

    public static int cooldownReductionSeconds(ServerPlayer player) {
        return branchRanks(player, Branch.SUPPORT) * 2;
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

    public enum Branch {
        POWER("공격"),
        GUARD("방어"),
        SUPPORT("지원");

        private final String displayName;

        Branch(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum Node {
        POWER_1("power_1", "정밀 타격 I", "모든 공격 피해 +8%", Branch.POWER, 1, null),
        POWER_2("power_2", "정밀 타격 II", "모든 공격 피해 추가 +8%", Branch.POWER, 2, POWER_1),
        POWER_3("power_3", "정밀 타격 III", "모든 공격 피해 추가 +8%", Branch.POWER, 3, POWER_2),

        GUARD_1("guard_1", "강철 방어 I", "받는 피해 7% 감소", Branch.GUARD, 1, null),
        GUARD_2("guard_2", "강철 방어 II", "받는 피해 추가 7% 감소", Branch.GUARD, 2, GUARD_1),
        GUARD_3("guard_3", "강철 방어 III", "받는 피해 추가 7% 감소", Branch.GUARD, 3, GUARD_2),

        SUPPORT_1("support_1", "전리품 감정 I", "처치 주화 +12%, 스킬 쿨타임 -2초", Branch.SUPPORT, 1, null),
        SUPPORT_2("support_2", "전리품 감정 II", "처치 주화 추가 +12%, 쿨타임 추가 -2초", Branch.SUPPORT, 2, SUPPORT_1),
        SUPPORT_3("support_3", "전리품 감정 III", "처치 주화 추가 +12%, 쿨타임 추가 -2초", Branch.SUPPORT, 3, SUPPORT_2);

        private final String id;
        private final String title;
        private final String description;
        private final Branch branch;
        private final int tier;
        private final Node prerequisite;

        Node(String id, String title, String description, Branch branch, int tier, Node prerequisite) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.branch = branch;
            this.tier = tier;
            this.prerequisite = prerequisite;
        }

        public String id() {
            return id;
        }

        public String title() {
            return title;
        }

        public String description() {
            return description;
        }

        public Branch branch() {
            return branch;
        }

        public int tier() {
            return tier;
        }

        public Node prerequisite() {
            return prerequisite;
        }

        public static Optional<Node> parse(String value) {
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(node -> node.id.equals(normalized)).findFirst();
        }
    }
}

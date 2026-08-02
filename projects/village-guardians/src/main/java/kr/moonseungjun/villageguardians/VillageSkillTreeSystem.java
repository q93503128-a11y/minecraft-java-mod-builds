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
    private static final Map<UUID, Long> UNLOCKED_MASKS = new LinkedHashMap<>();
    private static final Map<UUID, Integer> SPENT_POINTS = new LinkedHashMap<>();
    private static VillageSkillTreeData savedData;

    private VillageSkillTreeSystem() {
    }

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageSkillTreeData.TYPE);
        UNLOCKED_MASKS.clear();
        UNLOCKED_MASKS.putAll(savedData.masks());
        SPENT_POINTS.clear();
        SPENT_POINTS.putAll(savedData.spentPoints());
        UNLOCKED_MASKS.forEach((uuid, mask) -> SPENT_POINTS.putIfAbsent(uuid, Long.bitCount(mask)));
        persist();
    }

    public static int earnedPoints(ServerPlayer player) {
        int level = VillageCouncilState.levelOf(player.getUUID());
        return Math.max(0, level - 1);
    }

    public static int spentPoints(ServerPlayer player) {
        return Math.max(0, SPENT_POINTS.getOrDefault(player.getUUID(), 0));
    }

    public static boolean hasValidAllocation(ServerPlayer player) {
        return spentPoints(player) <= earnedPoints(player);
    }

    public static int availablePoints(ServerPlayer player) {
        return Math.max(0, earnedPoints(player) - spentPoints(player));
    }

    public static synchronized boolean has(ServerPlayer player, Node node) {
        long mask = UNLOCKED_MASKS.getOrDefault(player.getUUID(), 0L);
        return (mask & bit(node)) != 0L;
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
        int cost = node.pointCost();
        if (availablePoints(player) < cost) {
            return "전술 포인트가 부족합니다. 필요 " + cost + "P, 현재 " + availablePoints(player)
                    + "P · 레벨이 오를 때마다 1P를 얻습니다.";
        }
        long mask = UNLOCKED_MASKS.getOrDefault(player.getUUID(), 0L);
        UNLOCKED_MASKS.put(player.getUUID(), mask | bit(node));
        SPENT_POINTS.put(player.getUUID(), spentPoints(player) + cost);
        persist();
        return node.title() + " 습득 완료 | 사용 " + cost + "P · 남은 포인트 "
                + availablePoints(player) + "P";
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
        return availablePoints(player) >= node.pointCost()
                ? "습득 가능" : node.pointCost() + "P 필요";
    }

    public static float outgoingDamageMultiplier(ServerPlayer player) {
        float bonus = 0.0f;
        if (has(player, Node.POWER_1)) bonus += 0.06f;
        if (has(player, Node.POWER_2)) bonus += 0.06f;
        if (has(player, Node.POWER_4)) bonus += 0.08f;
        if (has(player, Node.POWER_7)) bonus += 0.10f;
        if (has(player, Node.POWER_8)) bonus += 0.12f;
        return 1.0f + bonus;
    }

    public static float executionMultiplier(ServerPlayer player, float health, float maximum) {
        if (maximum <= 0.0f) return 1.0f;
        float ratio = health / maximum;
        if (has(player, Node.POWER_6) && ratio <= 0.40f) return 1.24f;
        return has(player, Node.POWER_3) && ratio <= 0.30f ? 1.18f : 1.0f;
    }

    public static int killMomentumSeconds(ServerPlayer player) {
        if (has(player, Node.POWER_7)) return 8;
        return has(player, Node.POWER_6) ? 5 : 0;
    }

    public static float projectileDamageMultiplier(ServerPlayer player) {
        float bonus = 0.0f;
        if (has(player, Node.RANGED_1)) bonus += 0.08f;
        if (has(player, Node.RANGED_4)) bonus += 0.10f;
        if (has(player, Node.RANGED_6)) bonus += 0.12f;
        if (has(player, Node.RANGED_8)) bonus += 0.14f;
        return 1.0f + bonus;
    }

    public static float projectileExecutionMultiplier(ServerPlayer player, float health, float maximum) {
        if (maximum <= 0.0f) return 1.0f;
        float ratio = health / maximum;
        if (has(player, Node.RANGED_8) && ratio <= 0.60f) return 1.30f;
        return has(player, Node.RANGED_7) && ratio <= 0.50f ? 1.22f : 1.0f;
    }

    public static int projectileFireBonusTicks(ServerPlayer player) {
        if (has(player, Node.RANGED_7)) return 140;
        return has(player, Node.RANGED_2) ? 70 : 0;
    }

    public static int extraRicochetTargets(ServerPlayer player) {
        int extra = 0;
        if (has(player, Node.RANGED_3)) extra++;
        if (has(player, Node.RANGED_5)) extra += 2;
        if (has(player, Node.RANGED_7)) extra += 2;
        if (has(player, Node.RANGED_8)) extra += 3;
        return extra;
    }

    public static float incomingDamageMultiplier(ServerPlayer player) {
        float reduction = 0.0f;
        if (has(player, Node.GUARD_1)) reduction += 0.05f;
        if (has(player, Node.GUARD_2)) reduction += 0.05f;
        if (has(player, Node.GUARD_4)) reduction += 0.06f;
        if (has(player, Node.GUARD_6)) reduction += 0.04f;
        if (has(player, Node.GUARD_8)) reduction += 0.05f;
        return Math.max(0.65f, 1.0f - reduction);
    }

    public static float lowHealthIncomingMultiplier(ServerPlayer player) {
        return has(player, Node.GUARD_3) && player.getHealth() <= player.getMaxHealth() * 0.35f
                ? 0.82f : 1.0f;
    }

    public static boolean emergencyBarrierUnlocked(ServerPlayer player) {
        return has(player, Node.GUARD_5);
    }

    public static boolean lowHealthRegenerationUnlocked(ServerPlayer player) {
        return has(player, Node.GUARD_6);
    }

    public static int emergencyBarrierCooldownSeconds(ServerPlayer player) {
        return has(player, Node.GUARD_7) ? 55 : 90;
    }

    public static int emergencyBarrierAbsorptionAmplifier(ServerPlayer player) {
        return has(player, Node.GUARD_7) ? 3 : 1;
    }

    public static float coinRewardMultiplier(ServerPlayer player) {
        float bonus = 0.0f;
        if (has(player, Node.SUPPORT_1)) bonus += 0.08f;
        if (has(player, Node.SUPPORT_4)) bonus += 0.10f;
        if (has(player, Node.SUPPORT_6)) bonus += 0.12f;
        if (has(player, Node.SUPPORT_8)) bonus += 0.15f;
        return 1.0f + bonus;
    }

    public static int cooldownReductionSeconds(ServerPlayer player) {
        int reduction = 0;
        if (has(player, Node.SUPPORT_2)) reduction += 2;
        if (has(player, Node.SUPPORT_4)) reduction += 2;
        if (has(player, Node.SUPPORT_6)) reduction += 2;
        if (has(player, Node.SUPPORT_7)) reduction += 1;
        if (has(player, Node.SUPPORT_8)) reduction += 2;
        return reduction;
    }

    public static boolean sharedSupplyChanceUnlocked(ServerPlayer player) {
        return has(player, Node.SUPPORT_3);
    }

    public static float sharedSupplyChance(ServerPlayer player) {
        if (!sharedSupplyChanceUnlocked(player)) return 0.0f;
        float chance = 0.12f;
        if (has(player, Node.SUPPORT_6)) chance += 0.05f;
        if (has(player, Node.SUPPORT_7)) chance += 0.03f;
        return chance;
    }

    public static float killHealAmount(ServerPlayer player) {
        float amount = 0.0f;
        if (has(player, Node.POWER_5)) amount += 2.0f;
        if (has(player, Node.SUPPORT_5)) amount += 1.0f;
        return amount;
    }

    public static float teamHealOnKillAmount(ServerPlayer player) {
        if (has(player, Node.SUPPORT_8)) return 3.0f;
        return has(player, Node.SUPPORT_7) ? 2.0f : 0.0f;
    }

    public static int passiveSpeedAmplifier(ServerPlayer player, boolean daytime) {
        if (daytime) return 2 + (has(player, Node.MOBILITY_6) ? 1 : 0);
        if (has(player, Node.MOBILITY_4)) return 1;
        return has(player, Node.MOBILITY_1) ? 0 : -1;
    }

    public static float movingDamageMultiplier(ServerPlayer player) {
        double horizontal = player.getDeltaMovement().x * player.getDeltaMovement().x
                + player.getDeltaMovement().z * player.getDeltaMovement().z;
        if (horizontal < 0.012) return 1.0f;
        float bonus = 0.0f;
        if (has(player, Node.MOBILITY_2)) bonus += 0.06f;
        if (has(player, Node.MOBILITY_5)) bonus += 0.08f;
        if (has(player, Node.MOBILITY_8)) bonus += 0.12f;
        return 1.0f + bonus;
    }

    public static int mobilityCooldownReductionSeconds(ServerPlayer player) {
        int result = 0;
        if (has(player, Node.MOBILITY_3)) result++;
        if (has(player, Node.MOBILITY_6)) result++;
        if (has(player, Node.MOBILITY_8)) result += 2;
        return result;
    }

    public static int killSpeedSeconds(ServerPlayer player) {
        if (has(player, Node.MOBILITY_8)) return 8;
        return has(player, Node.MOBILITY_7) ? 5 : 0;
    }

    public static float sprintIncomingMultiplier(ServerPlayer player) {
        return has(player, Node.MOBILITY_7) && player.isSprinting() ? 0.88f : 1.0f;
    }

    public static synchronized void resetForNewGame() {
        UNLOCKED_MASKS.clear();
        SPENT_POINTS.clear();
        persist();
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
        return Arrays.stream(Node.values())
                .sorted((first, second) -> {
                    int branch = Integer.compare(first.branch().ordinal(), second.branch().ordinal());
                    return branch != 0 ? branch : Integer.compare(first.tier(), second.tier());
                })
                .toList();
    }

    private static long bit(Node node) {
        return 1L << node.ordinal();
    }

    private static void persist() {
        if (savedData != null) savedData.replace(UNLOCKED_MASKS, SPENT_POINTS);
    }

    public enum Branch {
        POWER("공격"),
        GUARD("방어"),
        SUPPORT("지원"),
        RANGED("사격"),
        MOBILITY("기동");

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
        RANGED_5("ranged_5", "분열 사격", "연쇄 사격 대상 +2", Branch.RANGED, 5, RANGED_4),

        // Appended to preserve every existing saved-mask ordinal.
        POWER_6("power_6", "포식자의 감각", "체력 40% 이하의 적에게 피해 +24%, 처치 시 잠시 힘을 얻음", Branch.POWER, 6, POWER_5),
        POWER_7("power_7", "전쟁의 화신", "모든 공격 피해 +10%, 처치 연속 강화 지속시간 증가", Branch.POWER, 7, POWER_6),
        GUARD_6("guard_6", "재생 태세", "받는 피해 추가 4% 감소, 낮은 체력에서 재생 발동", Branch.GUARD, 6, GUARD_5),
        GUARD_7("guard_7", "불굴의 심장", "응급 장막의 보호량 증가, 재사용 대기시간 90초에서 55초로 감소", Branch.GUARD, 7, GUARD_6),
        SUPPORT_6("support_6", "전선 군수관", "처치 주화 +12%, 기술 재사용 -2초, 공동 보급 회수율 증가", Branch.SUPPORT, 6, SUPPORT_5),
        SUPPORT_7("support_7", "연대의 맹세", "처치 시 주변 아군 체력 1칸 회복, 기술 재사용 -1초", Branch.SUPPORT, 7, SUPPORT_6),
        RANGED_6("ranged_6", "초장력 시위", "화살과 투사체 피해 추가 +12%", Branch.RANGED, 6, RANGED_5),
        RANGED_7("ranged_7", "종결 사격", "체력 50% 이하 적 대상 투사체 피해 +22%, 연쇄 대상 +2, 발화 지속 증가", Branch.RANGED, 7, RANGED_6),
        POWER_8("power_8", "절대 공세", "모든 공격 피해 추가 +12%", Branch.POWER, 8, POWER_7),
        GUARD_8("guard_8", "요새의 심장", "받는 피해 추가 5% 감소", Branch.GUARD, 8, GUARD_7),
        SUPPORT_8("support_8", "총군수 지휘", "처치 주화 +15%, 기술 재사용 -2초, 주변 회복 강화", Branch.SUPPORT, 8, SUPPORT_7),
        RANGED_8("ranged_8", "천공 사격", "투사체 피해 +14%, 체력 60% 이하 대상 추가 피해와 연쇄 대상 +3", Branch.RANGED, 8, RANGED_7),
        MOBILITY_1("mobility_1", "경량 장비", "밤에도 이동 속도 I을 유지합니다.", Branch.MOBILITY, 1, null),
        MOBILITY_2("mobility_2", "유동 공세", "이동 중 공격 피해 +6%", Branch.MOBILITY, 2, MOBILITY_1),
        MOBILITY_3("mobility_3", "빠른 호흡", "직업 기술 재사용 대기시간 -1초", Branch.MOBILITY, 3, MOBILITY_2),
        MOBILITY_4("mobility_4", "전장 질주", "밤 이동 속도가 II로 증가합니다.", Branch.MOBILITY, 4, MOBILITY_3),
        MOBILITY_5("mobility_5", "관성 타격", "이동 중 공격 피해 추가 +8%", Branch.MOBILITY, 5, MOBILITY_4),
        MOBILITY_6("mobility_6", "신속한 전환", "낮 이동 속도 단계 +1, 직업 기술 재사용 -1초", Branch.MOBILITY, 6, MOBILITY_5),
        MOBILITY_7("mobility_7", "회피 기동", "질주 중 받는 피해 12% 감소, 처치 후 5초간 가속", Branch.MOBILITY, 7, MOBILITY_6),
        MOBILITY_8("mobility_8", "번개 보법", "이동 중 피해 +12%, 기술 재사용 -2초, 처치 가속 8초", Branch.MOBILITY, 8, MOBILITY_7);

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
        public int pointCost() { return Math.max(1, Math.min(3, (tier + 2) / 3)); }

        public static Optional<Node> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values())
                    .filter(node -> node.id.equals(normalized))
                    .findFirst();
        }
    }
}

package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VillageTowerSpecializationSystem {
    public static final int MAX_BRANCH_RANK = 3;

    private static final EnumMap<TowerKind, Branch> BRANCHES = new EnumMap<>(TowerKind.class);
    private static final EnumMap<TowerKind, Integer> RANKS = new EnumMap<>(TowerKind.class);
    private static final EnumMap<TowerKind, Integer> DISABLED_TICKS = new EnumMap<>(TowerKind.class);
    private static VillageTowerProgressData savedData;
    private static int disableCursor;

    private VillageTowerSpecializationSystem() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageTowerProgressData.TYPE);
        BRANCHES.clear();
        RANKS.clear();
        DISABLED_TICKS.clear();
        savedData.branches().forEach((key, value) -> {
            TowerKind kind = TowerKind.fromId(key);
            Branch branch = Branch.fromId(value);
            if (kind != null && branch != null && branch.kind() == kind) BRANCHES.put(kind, branch);
        });
        savedData.ranks().forEach((key, value) -> {
            TowerKind kind = TowerKind.fromId(key);
            if (kind != null) RANKS.put(kind, Math.max(0, Math.min(MAX_BRANCH_RANK, value)));
        });
        sanitize();
        disableCursor = 0;
        persist();
    }

    public static synchronized void resetTransientState() {
        DISABLED_TICKS.clear();
        disableCursor = 0;
    }

    public static synchronized void tick() {
        for (TowerKind kind : TowerKind.values()) {
            int remaining = DISABLED_TICKS.getOrDefault(kind, 0);
            if (remaining <= 1) DISABLED_TICKS.remove(kind);
            else DISABLED_TICKS.put(kind, remaining - 1);
        }
    }

    public static synchronized boolean installed(TowerKind kind) {
        return kind != null && VillageProgressionSystem.wallLevel() >= kind.requiredWallLevel();
    }

    public static synchronized boolean disabled(TowerKind kind) {
        return DISABLED_TICKS.getOrDefault(kind, 0) > 0;
    }

    public static synchronized int disabledSeconds(TowerKind kind) {
        return Math.max(0, (DISABLED_TICKS.getOrDefault(kind, 0) + 19) / 20);
    }

    public static synchronized Branch branch(TowerKind kind) {
        return BRANCHES.get(kind);
    }

    public static synchronized int rank(TowerKind kind) {
        return branch(kind) == null ? 0 : Math.max(1, RANKS.getOrDefault(kind, 1));
    }

    public static List<Branch> branchesFor(TowerKind kind) {
        return List.of(Branch.values()).stream().filter(branch -> branch.kind() == kind).toList();
    }

    public static synchronized String purchaseBranch(ServerPlayer player, TowerKind kind, Branch requested) {
        if (kind == null || requested == null || requested.kind() != kind) return "알 수 없는 방어탑 분기입니다.";
        if (!installed(kind)) return kind.displayName() + "이 아직 설치되지 않았습니다. 성벽 Lv."
                + kind.requiredWallLevel() + "이 필요합니다.";
        if (VillageRaidSystem.isRaidLocked()) return "습격 진행 중에는 포탑 구조를 개조할 수 없습니다.";
        Branch current = branch(kind);
        if (current == requested) return requested.displayName() + " 분기가 이미 적용되어 있습니다.";
        int cost = branchInstallCost(kind, current != null);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        BRANCHES.put(kind, requested);
        RANKS.put(kind, 1);
        persist();
        return kind.displayName() + "을(를) " + requested.displayName() + " I로 개조했습니다."
                + (current == null ? "" : " 기존 분기 단계는 초기화되었습니다.");
    }

    public static synchronized String upgradeBranch(ServerPlayer player, TowerKind kind) {
        if (kind == null || !installed(kind)) return "설치되지 않은 방어탑입니다.";
        Branch current = branch(kind);
        if (current == null) return "먼저 세 가지 전문 분기 중 하나를 선택해야 합니다.";
        if (VillageRaidSystem.isRaidLocked()) return "습격 진행 중에는 방어탑을 개조할 수 없습니다.";
        int currentRank = rank(kind);
        if (currentRank >= MAX_BRANCH_RANK) return current.displayName() + " 분기가 최고 단계입니다.";
        int cost = branchUpgradeCost(kind, currentRank);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        RANKS.put(kind, currentRank + 1);
        persist();
        return kind.displayName() + " · " + current.displayName() + " " + roman(currentRank + 1)
                + " 강화 완료";
    }

    public static synchronized void disableRandomInstalledTower(int ticks) {
        List<TowerKind> candidates = List.of(TowerKind.values()).stream()
                .filter(VillageTowerSpecializationSystem::installed)
                .toList();
        if (candidates.isEmpty()) return;
        TowerKind selected = candidates.get(Math.floorMod(disableCursor++, candidates.size()));
        DISABLED_TICKS.put(selected, Math.max(DISABLED_TICKS.getOrDefault(selected, 0), Math.max(1, ticks)));
    }

    public static synchronized String summary(TowerKind kind) {
        if (!installed(kind)) return "미설치 · 성벽 Lv." + kind.requiredWallLevel() + " 필요";
        Branch branch = branch(kind);
        String state = branch == null ? "기본형" : branch.displayName() + " " + roman(rank(kind));
        if (disabled(kind)) state += " · §c교란 " + disabledSeconds(kind) + "초";
        return state;
    }

    public static int branchInstallCost(TowerKind kind, boolean reconfigure) {
        int base = 180 + kind.requiredWallLevel() * 55;
        return reconfigure ? base + 140 : base;
    }

    public static int branchUpgradeCost(TowerKind kind, int currentRank) {
        return 240 + kind.requiredWallLevel() * 60 + Math.max(1, currentRank) * 210;
    }

    private static void sanitize() {
        BRANCHES.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().kind() != entry.getKey());
        for (TowerKind kind : TowerKind.values()) {
            if (!BRANCHES.containsKey(kind)) RANKS.remove(kind);
            else RANKS.put(kind, Math.max(1, Math.min(MAX_BRANCH_RANK, RANKS.getOrDefault(kind, 1))));
        }
    }

    private static synchronized void persist() {
        if (savedData == null) return;
        Map<String, String> branches = new LinkedHashMap<>();
        BRANCHES.forEach((kind, branch) -> branches.put(kind.id(), branch.id()));
        Map<String, Integer> ranks = new LinkedHashMap<>();
        RANKS.forEach((kind, rank) -> ranks.put(kind.id(), rank));
        savedData.replace(branches, ranks);
    }

    private static String roman(int value) {
        return switch (value) { case 1 -> "I"; case 2 -> "II"; default -> "III"; };
    }

    public enum TowerKind {
        BALLISTA("ballista", "노포탑", 1),
        FLAME("flame", "화염탑", 2),
        FROST("frost", "빙결탑", 3),
        ARCANE("arcane", "비전탑", 4);

        private final String id;
        private final String displayName;
        private final int requiredWallLevel;

        TowerKind(String id, String displayName, int requiredWallLevel) {
            this.id = id;
            this.displayName = displayName;
            this.requiredWallLevel = requiredWallLevel;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public int requiredWallLevel() { return requiredWallLevel; }

        public static TowerKind fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (TowerKind kind : values()) if (kind.id.equals(normalized)) return kind;
            return null;
        }
    }

    public enum Branch {
        BALLISTA_TITAN(TowerKind.BALLISTA, "titan", "거인살 화살", "보스·중장갑 단일 대상에게 큰 추가 피해"),
        BALLISTA_PIERCE(TowerKind.BALLISTA, "pierce", "관통 궤도", "전열을 꿰뚫어 일렬·근접 적을 연속 타격"),
        BALLISTA_SPLIT(TowerKind.BALLISTA, "split", "분열 화살", "세 갈래 화살로 다수의 빠른 적을 동시 공격"),

        FLAME_INFERNO(TowerKind.FLAME, "inferno", "용광 지대", "넓은 범위를 오래 불태워 물량 공세 차단"),
        FLAME_BLAST(TowerKind.FLAME, "blast", "폭렬 화염탄", "착탄 순간 강한 범위 폭발 피해"),
        FLAME_MELT(TowerKind.FLAME, "melt", "융해 불꽃", "적의 공격과 방어를 약화시키는 고열 화염"),

        FROST_DEEP(TowerKind.FROST, "deep", "심층 동결", "짧은 시간 거의 움직이지 못할 정도로 강한 둔화"),
        FROST_SHATTER(TowerKind.FROST, "shatter", "빙결 파쇄", "이미 느려진 적에게 큰 추가 피해"),
        FROST_BLIZZARD(TowerKind.FROST, "blizzard", "눈보라 장막", "넓은 범위와 많은 대상을 지속 제어"),

        ARCANE_CHAIN(TowerKind.ARCANE, "chain", "연쇄 비전", "가까운 적 사이를 옮겨 다니며 다중 타격"),
        ARCANE_NULL(TowerKind.ARCANE, "null", "무효화 장", "정예·주술 적을 약화하고 강화 효과를 억제"),
        ARCANE_OVERCHARGE(TowerKind.ARCANE, "overcharge", "마력 과충전", "공격 간격은 늘지만 한 번의 피해가 크게 증가");

        private final TowerKind kind;
        private final String id;
        private final String displayName;
        private final String description;

        Branch(TowerKind kind, String id, String displayName, String description) {
            this.kind = kind;
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public TowerKind kind() { return kind; }
        public String id() { return id; }
        public String displayName() { return displayName; }
        public String description() { return description; }

        public static Branch fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (Branch branch : values()) if (branch.id.equals(normalized)) return branch;
            return null;
        }

        public static Branch from(TowerKind kind, String id) {
            Branch branch = fromId(id);
            return branch != null && branch.kind == kind ? branch : null;
        }
    }
}

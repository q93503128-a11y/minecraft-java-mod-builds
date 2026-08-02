package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class VillageDefenseResearchSystem {
    public static final int MAX_LEVEL = 5;
    private static final EnumMap<Branch, Integer> LEVELS = new EnumMap<>(Branch.class);
    private static VillageDefenseResearchData savedData;

    private VillageDefenseResearchSystem() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageDefenseResearchData.TYPE);
        LEVELS.clear();
        savedData.levels().forEach((id, value) -> {
            Branch branch = Branch.fromId(id);
            if (branch != null) LEVELS.put(branch, Math.max(0, Math.min(MAX_LEVEL, value)));
        });
        persist();
    }

    public static synchronized int level(Branch branch) {
        return branch == null ? 0 : LEVELS.getOrDefault(branch, 0);
    }

    public static synchronized int upgradeCost(Branch branch) {
        int current = level(branch);
        return 180 + branch.ordinal() * 40 + current * 220;
    }

    public static synchronized String upgrade(ServerPlayer player, Branch branch) {
        if (branch == null) return "알 수 없는 연구 분야입니다.";
        int current = level(branch);
        if (current >= MAX_LEVEL) return branch.displayName() + " 연구가 최고 단계입니다.";
        int cost = upgradeCost(branch);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        LEVELS.put(branch, current + 1);
        persist();
        return branch.displayName() + " Lv." + (current + 1) + " 연구 완료";
    }

    public static float mercenaryDamageMultiplier() {
        return 1.0f + level(Branch.MERCENARY) * 0.12f;
    }

    public static int mercenaryCapacityBonus() {
        return Math.min(3, level(Branch.MERCENARY));
    }

    public static float towerDamageMultiplier() {
        return 1.0f + level(Branch.TOWER) * 0.10f;
    }

    public static float equipmentDropBonus() {
        return level(Branch.LOGISTICS) * 0.03f;
    }

    public static float lootValueMultiplier() {
        return 1.0f + level(Branch.LOGISTICS) * 0.10f;
    }

    private static synchronized void persist() {
        if (savedData == null) return;
        Map<String, Integer> values = new java.util.LinkedHashMap<>();
        LEVELS.forEach((branch, level) -> values.put(branch.id(), level));
        savedData.replace(values);
    }

    public enum Branch {
        MERCENARY("mercenary", "용병 교리"),
        TOWER("tower", "포탑 공학"),
        LOGISTICS("logistics", "전리품 군수학");

        private final String id;
        private final String displayName;

        Branch(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }

        public String description(int level) {
            return switch (this) {
                case MERCENARY -> "용병 정원 +" + Math.min(3, level) + " · 용병 피해 +" + (level * 12) + "%";
                case TOWER -> "모든 방어탑 피해 +" + (level * 10) + "%";
                case LOGISTICS -> "장비 드랍률과 전리품 판매가치 강화 · 현재 판매 +" + (level * 10) + "%";
            };
        }

        public static Branch fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (Branch branch : values()) if (branch.id.equals(normalized)) return branch;
            return null;
        }
    }
}

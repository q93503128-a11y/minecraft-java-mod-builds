package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class VillageDefenseResearchSystem {
    public static final int MAX_LEVEL = 10;
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
        int mastery = Math.max(0, current - 4);
        return 180 + branch.ordinal() * 40 + current * 220 + mastery * 180;
    }

    public static synchronized String upgrade(ServerPlayer player, Branch branch) {
        if (branch == null) return "알 수 없는 연구 분야입니다.";
        int current = level(branch);
        if (current >= MAX_LEVEL) return branch.displayName() + " 연구가 최고 단계입니다.";
        int cost = upgradeCost(branch);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        String before = branch.description(current);
        float previousTowerDurability = branch == Branch.TOWER ? towerDurabilityMultiplier() : 1.0f;
        LEVELS.put(branch, current + 1);
        persist();
        if (branch == Branch.TOWER && player.level() instanceof ServerLevel level) {
            VillagePlacedTurretSystem.applyResearchDurabilityUpgrade(level, previousTowerDurability);
        }
        String after = branch.description(current + 1);
        return branch.displayName() + " Lv." + (current + 1) + " 연구 완료"
                + "\n이전: " + before + "\n현재: " + after;
    }

    private static float curve(int level, float firstFive, float masteryFive) {
        int safe = Math.max(0, Math.min(MAX_LEVEL, level));
        int foundation = Math.min(5, safe);
        int mastery = Math.max(0, safe - 5);
        return foundation * firstFive + mastery * masteryFive;
    }

    public static float mercenaryDamageMultiplier() {
        return 1.0f + curve(level(Branch.MERCENARY), 0.12f, 0.05f);
    }

    public static float mercenaryHealingMultiplier() {
        return 1.0f + curve(level(Branch.MERCENARY), 0.04f, 0.025f);
    }

    public static int mercenaryTrainingProgressPerKill() {
        return 1 + level(Branch.MERCENARY) / 4;
    }

    private static int mercenaryCapacityAt(int researchLevel) {
        int safe = Math.max(0, Math.min(MAX_LEVEL, researchLevel));
        int foundation = Math.min(3, safe);
        int mastery = Math.max(0, safe - 4) / 2;
        return Math.min(5, foundation + mastery);
    }

    public static int mercenaryCapacityBonus() {
        return mercenaryCapacityAt(level(Branch.MERCENARY));
    }

    public static float towerDamageMultiplier() {
        return 1.0f + curve(level(Branch.TOWER), 0.10f, 0.04f);
    }

    public static float towerRangeMultiplier() {
        return 1.0f + curve(level(Branch.TOWER), 0.01f, 0.015f);
    }

    public static float towerDurabilityMultiplier() {
        return 1.0f + curve(level(Branch.TOWER), 0.025f, 0.035f);
    }

    public static float equipmentDropBonus() {
        return curve(level(Branch.LOGISTICS), 0.03f, 0.01f);
    }

    public static float lootValueMultiplier() {
        return 1.0f + curve(level(Branch.LOGISTICS), 0.10f, 0.04f);
    }

    public static float consumableCostMultiplier() {
        return Math.max(0.75f, 1.0f - level(Branch.LOGISTICS) * 0.025f);
    }

    public static float fieldRepairMultiplier() {
        return 1.0f + curve(level(Branch.LOGISTICS), 0.04f, 0.03f);
    }

    public static synchronized void resetForNewGame() {
        LEVELS.clear();
        persist();
    }

    private static synchronized void persist() {
        if (savedData == null) return;
        Map<String, Integer> values = new java.util.LinkedHashMap<>();
        LEVELS.forEach((branch, level) -> values.put(branch.id(), level));
        savedData.replace(values);
    }

    private static int percent(float multiplier) {
        return Math.max(0, Math.round((multiplier - 1.0f) * 100.0f));
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

        public String description(int value) {
            int safe = Math.max(0, Math.min(MAX_LEVEL, value));
            return switch (this) {
                case MERCENARY -> "정원 +" + mercenaryCapacityAt(safe)
                        + " · 피해 +" + percent(1.0f + curve(safe, 0.12f, 0.05f)) + "%"
                        + " · 치유 +" + percent(1.0f + curve(safe, 0.04f, 0.025f)) + "%"
                        + " · 처치 훈련 진척 ×" + (1 + safe / 4);
                case TOWER -> "피해 +" + percent(1.0f + curve(safe, 0.10f, 0.04f)) + "%"
                        + " · 사거리 +" + percent(1.0f + curve(safe, 0.01f, 0.015f)) + "%"
                        + " · 내구 +" + percent(1.0f + curve(safe, 0.025f, 0.035f)) + "%";
                case LOGISTICS -> "장비 드랍 보너스 +"
                        + Math.round(curve(safe, 0.03f, 0.01f) * 100.0f) + "%p"
                        + " · 판매 +" + percent(1.0f + curve(safe, 0.10f, 0.04f)) + "%"
                        + " · 전투 소모품 할인 " + Math.round((1.0f - Math.max(0.75f, 1.0f - safe * 0.025f)) * 100.0f) + "%";
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

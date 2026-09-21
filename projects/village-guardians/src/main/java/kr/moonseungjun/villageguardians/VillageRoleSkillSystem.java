package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class VillageRoleSkillSystem {
    private static final Map<String, Integer> TREE_MASKS = new LinkedHashMap<>();
    private static final Map<String, Integer> SKILL_MASKS = new LinkedHashMap<>();
    private static final Map<String, String> EQUIPPED_SKILLS = new LinkedHashMap<>();
    private static final Map<String, Long> READY_AT = new LinkedHashMap<>();
    private static VillageRoleProgressData savedData;

    private VillageRoleSkillSystem() {
    }

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageRoleProgressData.TYPE);
        TREE_MASKS.clear();
        TREE_MASKS.putAll(savedData.treeMasks());
        SKILL_MASKS.clear();
        SKILL_MASKS.putAll(savedData.skillMasks());
        EQUIPPED_SKILLS.clear();
        EQUIPPED_SKILLS.putAll(savedData.equippedSkills());
        READY_AT.clear();
        sanitizeLoadouts();
        persist();
    }

    public static synchronized void resetTransientState() {
        READY_AT.clear();
    }

    public static synchronized boolean hasNode(ServerPlayer player, VillageRole role, RoleNode node) {
        int mask = TREE_MASKS.getOrDefault(roleKey(player.getUUID(), role), 0);
        return (mask & bit(node.ordinal())) != 0;
    }

    public static synchronized int branchRank(ServerPlayer player, VillageRole role, RoleBranch branch) {
        int result = 0;
        for (RoleNode node : RoleNode.values()) {
            if (node.branch() == branch && hasNode(player, role, node)) {
                result++;
            }
        }
        return result;
    }

    public static float durationMultiplier(ServerPlayer player, VillageRole role) {
        int rank = branchRank(player, role, RoleBranch.DURATION);
        float bonus = Math.min(3, rank) * 0.16f + Math.max(0, rank - 3) * 0.11f;
        return 1.0f + bonus;
    }

    public static float powerMultiplier(ServerPlayer player, VillageRole role) {
        int rank = branchRank(player, role, RoleBranch.POWER);
        float bonus = Math.min(3, rank) * 0.14f
                + (rank >= 3 ? 0.08f : 0.0f)
                + Math.max(0, rank - 3) * 0.11f;
        return 1.0f + bonus;
    }

    public static int specialRank(ServerPlayer player, VillageRole role) {
        return branchRank(player, role, RoleBranch.SPECIAL);
    }

    public static int roleTreeCooldownReductionSeconds(ServerPlayer player, VillageRole role) {
        int duration = branchRank(player, role, RoleBranch.DURATION);
        int special = branchRank(player, role, RoleBranch.SPECIAL);
        return Math.max(0, duration - 3) + Math.max(0, special - 3);
    }

    public static List<RoleNode> nodes() {
        return Arrays.stream(RoleNode.values())
                .sorted((first, second) -> {
                    int branch = Integer.compare(first.branch().ordinal(), second.branch().ordinal());
                    return branch != 0 ? branch : Integer.compare(first.tier(), second.tier());
                })
                .toList();
    }

    public static synchronized String purchaseNode(
            ServerPlayer player,
            VillageRole role,
            String nodeId) {
        if (VillageCouncilState.roleOf(player.getUUID()).orElse(null) != role) {
            return "현재 배치된 직업의 성장 경로만 습득할 수 있습니다.";
        }
        RoleNode node = RoleNode.parse(nodeId).orElse(null);
        if (node == null) {
            return "알 수 없는 직업 성장 노드입니다.";
        }
        if (hasNode(player, role, node)) {
            return node.title(role) + "은(는) 이미 습득했습니다.";
        }
        if (node.prerequisite() != null && !hasNode(player, role, node.prerequisite())) {
            return "먼저 " + node.prerequisite().title(role) + "을(를) 습득해야 합니다.";
        }
        int level = VillageCouncilState.levelOf(player.getUUID());
        if (level < node.requiredLevel()) {
            return "레벨 " + node.requiredLevel() + "부터 습득할 수 있습니다. 현재 레벨 " + level;
        }
        if (!VillageProgressionSystem.spendCoins(player, node.coinCost())) {
            return "수호 주화가 부족합니다. 필요 " + node.coinCost()
                    + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        String key = roleKey(player.getUUID(), role);
        TREE_MASKS.put(key, TREE_MASKS.getOrDefault(key, 0) | bit(node.ordinal()));
        persist();
        return node.title(role) + " 습득 완료 | 남은 주화 " + VillageProgressionSystem.coins(player);
    }

    public static String nodeStatus(ServerPlayer player, VillageRole role, RoleNode node) {
        if (hasNode(player, role, node)) {
            return "습득";
        }
        if (node.prerequisite() != null && !hasNode(player, role, node.prerequisite())) {
            return "선행 필요";
        }
        int level = VillageCouncilState.levelOf(player.getUUID());
        if (level < node.requiredLevel()) {
            return "Lv." + node.requiredLevel() + " 필요";
        }
        if (VillageProgressionSystem.coins(player) < node.coinCost()) {
            return "주화 " + node.coinCost() + " 필요";
        }
        return "습득 가능";
    }

    public static synchronized boolean hasSkill(ServerPlayer player, ActiveSkill skill) {
        int mask = SKILL_MASKS.getOrDefault(roleKey(player.getUUID(), skill.role()), 0);
        return (mask & bit(skill.roleIndex())) != 0;
    }

    public static synchronized String unlockSkill(ServerPlayer player, String skillId) {
        if (!VillageLocationRules.isNearSkillHall(player)) {
            return "직업 기술 연구는 기술·마법 연구소 근처에서만 가능합니다.";
        }
        String blocked = VillageMaintenanceRules.blockReason("직업 기술 연구");
        if (blocked != null) return blocked;
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.SKILL_HALL)) {
            return "기술·마법 연구소가 파괴되어 직업 기술을 연구할 수 없습니다.";
        }
        ActiveSkill skill = ActiveSkill.parse(skillId).orElse(null);
        if (skill == null) {
            return "알 수 없는 직업 기술입니다.";
        }
        VillageRole currentRole = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (currentRole != skill.role()) {
            return "현재 직업의 기술만 습득할 수 있습니다.";
        }
        if (hasSkill(player, skill)) {
            return skill.displayName() + "은(는) 이미 습득했습니다.";
        }
        int level = VillageCouncilState.levelOf(player.getUUID());
        int promotionTier = VillageRolePromotionSystem.tier(level);
        if (promotionTier < skill.promotionTier()) {
            int requiredPromotionLevel = skill.promotionTier() >= 2
                    ? VillageRolePromotionSystem.SECOND_PROMOTION_LEVEL
                    : VillageRolePromotionSystem.FIRST_PROMOTION_LEVEL;
            return "레벨 " + requiredPromotionLevel + " 전직 후 습득할 수 있습니다. 현재 레벨 " + level;
        }
        if (level < skill.requiredLevel()) {
            return "레벨 " + skill.requiredLevel() + "부터 습득할 수 있습니다. 현재 레벨 " + level;
        }
        if (!VillageProgressionSystem.spendCoins(player, skill.coinCost())) {
            return "수호 주화가 부족합니다. 필요 " + skill.coinCost()
                    + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        String key = roleKey(player.getUUID(), skill.role());
        SKILL_MASKS.put(key, SKILL_MASKS.getOrDefault(key, 0) | bit(skill.roleIndex()));
        equipIntoFirstFreeSlot(player, skill);
        persist();
        return skill.displayName() + " 습득 완료 | 빈 슬롯이 있으면 자동 장착됩니다.";
    }

    public static synchronized String equipSkill(ServerPlayer player, String skillId, int slot) {
        ActiveSkill skill = ActiveSkill.parse(skillId).orElse(null);
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (skill == null || role == null || skill.role() != role) {
            return "현재 직업에 맞지 않는 기술입니다.";
        }
        if (!hasSkill(player, skill)) {
            return "먼저 해당 기술을 습득해야 합니다.";
        }
        int safeSlot = slot == 1 ? 1 : 0;
        int otherSlot = safeSlot == 0 ? 1 : 0;
        String otherKey = loadoutKey(player.getUUID(), role, otherSlot);
        if (skill.id().equals(EQUIPPED_SKILLS.get(otherKey))) {
            EQUIPPED_SKILLS.remove(otherKey);
        }
        EQUIPPED_SKILLS.put(loadoutKey(player.getUUID(), role, safeSlot), skill.id());
        persist();
        return skill.displayName() + "을(를) 기술 슬롯 " + (safeSlot + 1) + "에 장착했습니다.";
    }

    public static synchronized Optional<ActiveSkill> equippedSkill(ServerPlayer player, int slot) {
        if (VillageSkillTestSystem.isEnabled(player)) {
            return VillageSkillTestSystem.equippedSkill(player, slot);
        }
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) return Optional.empty();
        return ActiveSkill.parse(EQUIPPED_SKILLS.get(loadoutKey(player.getUUID(), role, slot == 1 ? 1 : 0)))
                .filter(skill -> skill.role() == role && hasSkill(player, skill));
    }

    public static String loadoutSummary(ServerPlayer player) {
        String first = equippedSkill(player, 0).map(ActiveSkill::displayName).orElse("비어 있음");
        String second = equippedSkill(player, 1).map(ActiveSkill::displayName).orElse("비어 있음");
        return "{SKILL1}: " + first + " | {SKILL2}: " + second;
    }

    public static synchronized int cooldownRemainingSeconds(ServerPlayer player, int slot) {
        if (player == null || VillageSkillTestSystem.isEnabled(player)) return 0;
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null) return 0;
        long remaining = READY_AT.getOrDefault(player.getUUID() + "|" + skill.id(), 0L)
                - System.currentTimeMillis();
        return remaining <= 0L ? 0 : (int) Math.max(1L, (remaining + 999L) / 1000L);
    }

    public static synchronized float cooldownProgress(ServerPlayer player, int slot) {
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null || VillageSkillTestSystem.isEnabled(player)) return 0.0f;
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) return 0.0f;
        int total = effectiveCooldownSeconds(player, role, skill);
        int remaining = cooldownRemainingSeconds(player, slot);
        return total <= 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, remaining / (float) total));
    }

    public static String hudSlotText(ServerPlayer player, int slot) {
        String key = slot == 0 ? "§b{SKILL1}" : "§d{SKILL2}";
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null) return key + " §8비어 있음";
        int remaining = cooldownRemainingSeconds(player, slot);
        if (remaining <= 0) return key + " §f" + skill.displayName() + " §a준비";
        float progress = cooldownProgress(player, slot);
        int cooled = Math.max(0, Math.min(5, Math.round((1.0f - progress) * 5.0f)));
        String bar = "§a" + "■".repeat(cooled) + "§8" + "□".repeat(5 - cooled);
        return key + " §f" + skill.displayName() + " §c" + remaining + "초 " + bar;
    }

    public static List<ActiveSkill> skillsFor(VillageRole role) {
        return Arrays.stream(ActiveSkill.values()).filter(skill -> skill.role() == role).toList();
    }

    public static String skillStatus(ServerPlayer player, ActiveSkill skill) {
        if (hasSkill(player, skill)) {
            int slot = equippedSlot(player, skill);
            return slot >= 0 ? "장착 " + (slot + 1) : "습득";
        }
        int level = VillageCouncilState.levelOf(player.getUUID());
        if (VillageRolePromotionSystem.tier(level) < skill.promotionTier()) {
            return skill.promotionTier() >= 2 ? "2차 전직 필요" : "1차 전직 필요";
        }
        if (level < skill.requiredLevel()) {
            return "Lv." + skill.requiredLevel() + " 필요";
        }
        if (VillageProgressionSystem.coins(player) < skill.coinCost()) {
            return "주화 " + skill.coinCost() + " 필요";
        }
        return "습득 가능";
    }

    public static int equippedSlot(ServerPlayer player, ActiveSkill skill) {
        for (int slot = 0; slot < 2; slot++) {
            if (equippedSkill(player, slot).orElse(null) == skill) {
                return slot;
            }
        }
        return -1;
    }

    public static String useEquippedSkill(ServerPlayer player, int slot) {
        if (player == null || !player.isAlive() || VillageRespawnSystem.isDowned(player)) {
            return "전투 불능 상태에서는 기술을 사용할 수 없습니다.";
        }
        boolean testing = VillageSkillTestSystem.isEnabled(player);
        VillageRole role = testing
                ? VillageSkillTestSystem.selectedRole(player)
                : VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            return "기술 연구소에서 직업을 먼저 배치해야 합니다.";
        }
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null) {
            return testing
                    ? "시험 기술 슬롯 " + (slot + 1) + "이 비어 있습니다. 시험 관리함에서 기술을 장착하세요."
                    : "기술 슬롯 " + (slot + 1) + "이 비어 있습니다. 직업 성장 화면에서 기술을 장착하세요.";
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return "현재 월드에서는 기술을 사용할 수 없습니다.";
        }
        if (!testing && (skill == ActiveSkill.WARDEN_FORMATION || skill == ActiveSkill.WARDEN_FIELD)
                && VillageRoleAbilitySystem.cancelHeldShield(player, skill)) {
            return skill.displayName() + " 해제";
        }

        long now = System.currentTimeMillis();
        String cooldownKey = player.getUUID() + "|" + skill.id();
        if (!testing) {
            long readyAt = READY_AT.getOrDefault(cooldownKey, 0L);
            if (readyAt > now) {
                return skill.displayName() + " 재사용까지 "
                        + Math.max(1L, (readyAt - now + 999L) / 1000L) + "초";
            }
        }

        float power = powerMultiplier(player, role)
                * VillageRolePromotionSystem.skillPowerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageProgressionSystem.skillHallPowerMultiplier()
                * VillageEquipmentShop.roleSkillMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player)
                * VillageConsumableSystem.skillMultiplier(player);
        float duration = durationMultiplier(player, role)
                * VillageProgressionSystem.skillHallDurationMultiplier()
                * VillageRelicSystem.skillDurationMultiplier(player);
        int special = specialRank(player, role);
        cast(level, player, skill, power, duration, special);

        if (testing) {
            return skill.displayName() + " 사용 완료 | 시험 모드 · 재사용 대기시간 없음";
        }
        int cooldown = effectiveCooldownSeconds(player, role, skill);
        READY_AT.put(cooldownKey, now + cooldown * 1000L);
        return skill.displayName() + " 사용 완료 | 재사용 " + cooldown + "초";
    }

    private static int effectiveCooldownSeconds(
            ServerPlayer player, VillageRole role, ActiveSkill skill) {
        int minimum = Math.max(2, Math.round(skill.baseCooldownSeconds() * 0.20f));
        int afterFlatReduction = skill.baseCooldownSeconds()
                - VillageProgressionSystem.skillCooldownReductionSeconds(player)
                - VillageSkillTreeSystem.cooldownReductionSeconds(player)
                - VillageSkillTreeSystem.mobilityCooldownReductionSeconds(player)
                - roleTreeCooldownReductionSeconds(player, role)
                - VillageRolePromotionSystem.cooldownReductionSeconds(player, role)
                - VillageEquipmentShop.cooldownReductionSeconds(player);
        int afterRelics = Math.round(Math.max(1, afterFlatReduction)
                * VillageRelicSystem.cooldownMultiplier(player));
        return Math.max(minimum, afterRelics);
    }

    public static synchronized void resetForNewGame() {
        TREE_MASKS.clear(); SKILL_MASKS.clear(); EQUIPPED_SKILLS.clear(); READY_AT.clear(); persist();
    }

    private static void cast(
            ServerLevel level,
            ServerPlayer player,
            ActiveSkill skill,
            float power,
            float durationMultiplier,
            int specialRank) {
        VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
    }

    private static void equipIntoFirstFreeSlot(ServerPlayer player, ActiveSkill skill) {
        for (int slot = 0; slot < 2; slot++) {
            String key = loadoutKey(player.getUUID(), skill.role(), slot);
            if (!EQUIPPED_SKILLS.containsKey(key) || EQUIPPED_SKILLS.get(key).isBlank()) {
                EQUIPPED_SKILLS.put(key, skill.id());
                return;
            }
        }
    }

    private static void sanitizeLoadouts() {
        EQUIPPED_SKILLS.entrySet().removeIf(entry -> {
            ActiveSkill skill = ActiveSkill.parse(entry.getValue()).orElse(null);
            if (skill == null) {
                return true;
            }
            String[] key = entry.getKey().split("\\|", -1);
            return key.length != 3 || VillageRole.parse(key[1]).orElse(null) != skill.role();
        });
    }

    private static String roleKey(UUID uuid, VillageRole role) {
        return uuid + "|" + role.id();
    }

    private static String loadoutKey(UUID uuid, VillageRole role, int slot) {
        return uuid + "|" + role.id() + "|" + (slot == 1 ? 1 : 0);
    }

    private static int bit(int index) {
        return 1 << Math.max(0, Math.min(30, index));
    }

    private static void persist() {
        if (savedData != null) {
            savedData.replace(TREE_MASKS, SKILL_MASKS, EQUIPPED_SKILLS);
        }
    }

    public enum RoleBranch {
        DURATION("지속"), POWER("위력"), SPECIAL("특수");

        private final String displayName;

        RoleBranch(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum RoleNode {
        DURATION_1("duration_1", RoleBranch.DURATION, 1, 4, 120, null),
        DURATION_2("duration_2", RoleBranch.DURATION, 2, 10, 280, DURATION_1),
        DURATION_3("duration_3", RoleBranch.DURATION, 3, 18, 520, DURATION_2),
        POWER_1("power_1", RoleBranch.POWER, 1, 4, 120, null),
        POWER_2("power_2", RoleBranch.POWER, 2, 10, 280, POWER_1),
        POWER_3("power_3", RoleBranch.POWER, 3, 18, 520, POWER_2),
        SPECIAL_1("special_1", RoleBranch.SPECIAL, 1, 5, 150, null),
        SPECIAL_2("special_2", RoleBranch.SPECIAL, 2, 12, 340, SPECIAL_1),
        SPECIAL_3("special_3", RoleBranch.SPECIAL, 3, 21, 620, SPECIAL_2),

        // Appended so existing role-tree masks keep their original ordinal meaning.
        DURATION_4("duration_4", RoleBranch.DURATION, 4, 24, 880, DURATION_3),
        DURATION_5("duration_5", RoleBranch.DURATION, 5, 29, 1280, DURATION_4),
        POWER_4("power_4", RoleBranch.POWER, 4, 24, 920, POWER_3),
        POWER_5("power_5", RoleBranch.POWER, 5, 29, 1340, POWER_4),
        SPECIAL_4("special_4", RoleBranch.SPECIAL, 4, 25, 980, SPECIAL_3),
        SPECIAL_5("special_5", RoleBranch.SPECIAL, 5, 30, 1450, SPECIAL_4);

        private final String id;
        private final RoleBranch branch;
        private final int tier;
        private final int requiredLevel;
        private final int coinCost;
        private final RoleNode prerequisite;

        RoleNode(
                String id,
                RoleBranch branch,
                int tier,
                int requiredLevel,
                int coinCost,
                RoleNode prerequisite) {
            this.id = id;
            this.branch = branch;
            this.tier = tier;
            this.requiredLevel = requiredLevel;
            this.coinCost = coinCost;
            this.prerequisite = prerequisite;
        }

        public String id() { return id; }
        public RoleBranch branch() { return branch; }
        public int tier() { return tier; }
        public int requiredLevel() { return requiredLevel; }
        public int coinCost() { return coinCost; }
        public RoleNode prerequisite() { return prerequisite; }

        public String title(VillageRole role) {
            return switch (branch) {
                case DURATION -> switch (role) {
                    case VANGUARD -> "끊기지 않는 공세 " + roman(tier);
                    case RANGER -> "집중 호흡 " + roman(tier);
                    case ARCANIST -> "마력 유지 " + roman(tier);
                    case LUMINAR -> "지속 기도 " + roman(tier);
                    case WARDEN -> "불굴 태세 " + roman(tier);
                };
                case POWER -> switch (role) {
                    case VANGUARD -> "파쇄 검격 " + roman(tier);
                    case RANGER -> "치명 조준 " + roman(tier);
                    case ARCANIST -> "고밀도 마력 " + roman(tier);
                    case LUMINAR -> "증폭 치유 " + roman(tier);
                    case WARDEN -> "반격 충격 " + roman(tier);
                };
                case SPECIAL -> switch (role) {
                    case VANGUARD -> "피의 흐름 " + roman(tier);
                    case RANGER -> "사냥꾼의 표식 " + roman(tier);
                    case ARCANIST -> "원소 공명 " + roman(tier);
                    case LUMINAR -> "성역의 보호 " + roman(tier);
                    case WARDEN -> "철벽의 맹세 " + roman(tier);
                };
            };
        }

        public String description(VillageRole role) {
            return switch (branch) {
                case DURATION -> switch (tier) {
                    case 1 -> "지속시간이 있는 " + role.displayName() + " 강화·제어 효과 +16%.";
                    case 2 -> "지속 효과 추가 +16% · 이 가지 누적 +32%.";
                    case 3 -> "지속 효과 추가 +16% · 이 가지 누적 +48%.";
                    case 4 -> "지속 효과 +11% · 누적 +59% · 직업 기술 재사용 대기시간 -1초.";
                    default -> "지속 효과 +11% · 누적 +70% · 이 가지의 재사용 감소 총 -2초.";
                };
                case POWER -> switch (tier) {
                    case 1 -> "모든 " + role.displayName() + " 기술의 피해·치유량 +14%.";
                    case 2 -> "기술 피해·치유량 추가 +14% · 이 가지 누적 +28%.";
                    case 3 -> "3단계 추가 증폭 포함 · 이 가지 누적 피해·치유량 +50%.";
                    case 4 -> "기술 피해·치유량 추가 +11% · 이 가지 누적 +61%.";
                    default -> "기술 피해·치유량 추가 +11% · 이 가지 누적 +72%.";
                };
                case SPECIAL -> specialDescription(role, tier);
            };
        }

        private static String specialDescription(VillageRole role, int tier) {
            String effect = switch (role) {
                case VANGUARD -> "흡혈률과 공격 기술의 범위·타격수·밀어내기·약화/마무리 보정";
                case RANGER -> "조준 보정, 도탄 수·범위, 강화 사격의 추가 화살과 마무리 효과";
                case ARCANIST -> "마법 범위·사거리와 원소별 약화·연쇄·폭발 효과";
                case LUMINAR -> "치유 기술의 재생·흡수 보호막·저항과 보호 강도";
                case WARDEN -> "도발 범위, 받는 피해 감소, 약화·둔화·보호막·밀어내기";
            };
            String cooldown = tier == 4
                    ? " · 이 노드로 직업 기술 재사용 대기시간 -1초"
                    : tier >= 5 ? " · 특수 가지의 재사용 감소 총 -2초" : "";
            return "특수 등급 " + tier + "/5 · " + effect + "를 단계에 맞게 강화합니다" + cooldown + ".";
        }

        public static Optional<RoleNode> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(node -> node.id.equals(normalized)).findFirst();
        }

        private static String roman(int value) {
            return switch (value) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                default -> "V";
            };
        }
    }

    public enum ActiveSkill {
        VANGUARD_WHIRLWIND("vanguard_whirlwind", VillageRole.VANGUARD, 0, "회전 검무", 2, 70, 18, "몸을 회전하며 여러 차례 주변 적을 베고 이동할 수 있습니다."),
        VANGUARD_BREAKER("vanguard_breaker", VillageRole.VANGUARD, 1, "전투 고양", 7, 190, 24, "함성을 질러 자신과 주변 아군의 공격력·이동 속도를 강화합니다."),
        VANGUARD_CRY("vanguard_cry", VillageRole.VANGUARD, 2, "검기 난무", 13, 380, 32, "자세를 잡고 여러 개의 실제 검기 투사체를 전방으로 연속 발사합니다."),
        VANGUARD_STORM("vanguard_storm", VillageRole.VANGUARD, 3, "천붕 강하", 21, 680, 42, "공중으로 도약한 뒤 지면을 내려찍어 넓은 범위에 피해와 강한 충격을 줍니다."),
        VANGUARD_FRONTLINE_REND("vanguard_frontline_rend", VillageRole.VANGUARD, 4, "전선 절개", 30, 1200, 22, "1차 전직 기술. 세 갈래 검기를 한꺼번에 뻗어 전열을 가르고 적을 밀어냅니다."),
        VANGUARD_BLOOD_SPIRAL("vanguard_blood_spiral", VillageRole.VANGUARD, 5, "피의 회오리", 30, 1450, 28, "1차 전직 기술. 주변 적을 베어 피해를 주고 적중 수에 따라 자신의 체력을 회복합니다."),
        VANGUARD_WAR_BANNER("vanguard_war_banner", VillageRole.VANGUARD, 6, "전투 깃발", 30, 1750, 36, "1차 전직 기술. 자신과 주변 수호자의 공격·방어·기동을 일정 시간 끌어올립니다."),
        VANGUARD_BREACH_STRIKE("vanguard_breach_strike", VillageRole.VANGUARD, 7, "파성 일격", 30, 2150, 34, "1차 전직 기술. 전방의 중장갑·공성 병력을 강타해 약화시키고 뒤로 밀어냅니다."),
        VANGUARD_SWORD_CHAIN("vanguard_sword_chain", VillageRole.VANGUARD, 8, "검성 연환", 60, 3300, 26, "2차 전직 기술. 다섯 갈래 검기를 부채꼴로 연속 전개해 넓은 전선을 쓸어냅니다."),
        VANGUARD_LIFE_SEVER("vanguard_life_sever", VillageRole.VANGUARD, 9, "생명 절취", 60, 3900, 34, "2차 전직 기술. 주변의 부상당한 적을 베어 강한 피해를 주고 타격한 생명력을 흡수합니다."),
        VANGUARD_ABSOLUTE_BREAK("vanguard_absolute_break", VillageRole.VANGUARD, 10, "절대 돌파", 60, 4700, 38, "2차 전직 기술. 전방으로 크게 돌진하며 경로의 적을 연속 타격하고 진형을 무너뜨립니다."),
        VANGUARD_HEAVEN_SEVER("vanguard_heaven_sever", VillageRole.VANGUARD, 11, "천단참", 60, 5700, 52, "2차 전직 기술. 조준 지점에 거대한 검격을 떨어뜨려 넓은 범위를 한 번에 파쇄합니다."),

        RANGER_VOLLEY("ranger_volley", VillageRole.RANGER, 0, "신속 삼연사", 2, 70, 16, "기술 사용 후 다음 활은 빠르게 자동 완충·발사되며, 다음 실제 활·석궁 발사 한 번이 세 갈래 화살로 강화됩니다."),
        RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "추적 도탄", 7, 190, 22, "다음 실제 활·석궁 발사가 표적을 추적하고 적중 후 가까운 적에게 연속 도탄합니다."),
        RANGER_RICOCHET("ranger_ricochet", VillageRole.RANGER, 2, "천공 화살비", 13, 380, 30, "다음 실제 활·석궁 발사 지점에 강한 화살비를 펼쳐 지속 광역 피해를 줍니다."),
        RANGER_FIRE_RAIN("ranger_fire_rain", VillageRole.RANGER, 3, "성멸 대궁", 21, 680, 40, "다음 실제 활·석궁 발사를 거대한 성멸 화살로 바꾸어 넓은 전방을 관통합니다."),
        RANGER_HAWK_MARK("ranger_hawk_mark", VillageRole.RANGER, 4, "매의 징표", 30, 1200, 18, "1차 전직 기술. 조준한 고위협 표적을 드러내고 약화시켜 집중 사격의 기점을 만듭니다."),
        RANGER_SPLIT_SHOT("ranger_split_shot", VillageRole.RANGER, 5, "분열 사격", 30, 1450, 22, "1차 전직 기술. 다섯 발의 에너지 화살을 부채꼴로 발사해 다수의 적을 동시에 압박합니다."),
        RANGER_AA_INTERCEPT("ranger_aa_intercept", VillageRole.RANGER, 6, "대공 요격", 30, 1750, 28, "1차 전직 기술. 넓은 공역의 비행 적을 우선 포착해 연속 요격하고 움직임을 제한합니다."),
        RANGER_DOWNPOUR("ranger_downpour", VillageRole.RANGER, 7, "폭우 사격", 30, 2150, 34, "1차 전직 기술. 조준 지점에 여러 차례 화살 폭우를 집중시켜 지역을 봉쇄합니다."),
        RANGER_STAR_TRACKER("ranger_star_tracker", VillageRole.RANGER, 8, "별추적 화살", 60, 3300, 20, "2차 전직 기술. 위협도가 높은 여러 표적을 자동 포착해 추적 화살을 동시에 날립니다."),
        RANGER_CONSTELLATION("ranger_constellation", VillageRole.RANGER, 9, "관통 성단", 60, 3900, 28, "2차 전직 기술. 굵은 관통 사격을 발사해 일렬의 적을 뚫고 강하게 밀어냅니다."),
        RANGER_SKY_LOCK("ranger_sky_lock", VillageRole.RANGER, 10, "천공 봉쇄", 60, 4700, 38, "2차 전직 기술. 전장의 공중 적을 광범위하게 억제하고 대공 피해를 집중합니다."),
        RANGER_METEOR_BOW("ranger_meteor_bow", VillageRole.RANGER, 11, "유성 대궁", 60, 5700, 50, "2차 전직 기술. 조준 지점에 초대형 사격을 꽂아 넓은 범위의 적을 폭발적으로 밀어냅니다."),

        ARCANIST_FIRE_ORB("arcanist_fire_orb", VillageRole.ARCANIST, 0, "홍염탄", 2, 70, 18, "실제 화염 구체를 전방으로 날려 충돌 지점에서 폭발시키고 적을 불태웁니다."),
        ARCANIST_FROST_RING("arcanist_frost_ring", VillageRole.ARCANIST, 1, "빙결 지대", 7, 190, 24, "조준 위치에 지속되는 냉기 지대를 만들어 적을 강하게 둔화하고 피해를 줍니다."),
        ARCANIST_CHAIN("arcanist_chain", VillageRole.ARCANIST, 2, "폭풍 회랑", 13, 380, 30, "전진하는 토네이도로 적을 끌어올리고 휩쓸어 진형을 무너뜨립니다."),
        ARCANIST_NOVA("arcanist_nova", VillageRole.ARCANIST, 3, "천뢰 폭격", 21, 680, 44, "넓은 목표 지점에 번개가 연속으로 떨어져 다수의 적에게 강한 광역 피해를 줍니다."),
        ARCANIST_LAVA_CORE("arcanist_lava_core", VillageRole.ARCANIST, 4, "용암핵", 30, 1200, 22, "1차 전직 기술. 커다란 고열 마력핵을 발사해 넓은 폭발과 장시간 화상을 일으킵니다."),
        ARCANIST_FROST_PRISON("arcanist_frost_prison", VillageRole.ARCANIST, 5, "빙결 감옥", 30, 1450, 28, "1차 전직 기술. 넓은 지역을 얼려 적의 진군을 크게 늦추고 지속 피해를 줍니다."),
        ARCANIST_LIGHTNING_CHAIN("arcanist_lightning_chain", VillageRole.ARCANIST, 6, "낙뢰 사슬", 30, 1750, 30, "1차 전직 기술. 표적 사이를 뛰는 번개로 여러 적을 연속 타격합니다."),
        ARCANIST_GRAVITY_STORM("arcanist_gravity_storm", VillageRole.ARCANIST, 7, "중력 폭풍", 30, 2150, 38, "1차 전직 기술. 거대한 폭풍장을 전진시켜 적을 중심으로 끌어당기고 묶습니다."),
        ARCANIST_SOLAR_CORE("arcanist_solar_core", VillageRole.ARCANIST, 8, "태양핵 폭발", 60, 3300, 30, "2차 전직 기술. 거대한 태양핵을 발사해 충돌 지점에 초대형 폭발을 일으킵니다."),
        ARCANIST_ABSOLUTE_ZERO("arcanist_absolute_zero", VillageRole.ARCANIST, 9, "절대영도", 60, 3900, 38, "2차 전직 기술. 광범위한 적의 이동과 공격 흐름을 장시간 얼려 세웁니다."),
        ARCANIST_HEAVEN_CHAIN("arcanist_heaven_chain", VillageRole.ARCANIST, 10, "천뢰 연쇄", 60, 4700, 42, "2차 전직 기술. 넓은 지역에 고밀도 낙뢰를 연속 유도해 전열을 붕괴시킵니다."),
        ARCANIST_SINGULARITY("arcanist_singularity", VillageRole.ARCANIST, 11, "붕괴 특이점", 60, 5700, 56, "2차 전직 기술. 거대한 중력장을 만들어 적을 끌어모으고 반복 피해를 줍니다."),

        LUMINAR_HEAL("luminar_heal", VillageRole.LUMINAR, 0, "응급 성광", 2, 70, 16, "현재 체력 비율이 가장 낮은 아군 한 명을 찾아 큰 폭으로 즉시 회복시킵니다."),
        LUMINAR_CLEANSE("luminar_cleanse", VillageRole.LUMINAR, 1, "전군 정화", 7, 190, 24, "같은 전장에 있는 모든 아군의 해로운 효과를 제거하고 소량 회복시킵니다."),
        LUMINAR_VEIL("luminar_veil", VillageRole.LUMINAR, 2, "치유 성역", 13, 380, 32, "주변에 지속되는 회복 지대를 설치해 범위 안 아군을 반복해서 치유합니다."),
        LUMINAR_SANCTUARY("luminar_sanctuary", VillageRole.LUMINAR, 3, "기적의 대성역", 21, 680, 46, "전장 전체 아군을 크게 치유하고 보호막을 부여하며 전투 불능 아군을 즉시 부활시킵니다."),
        LUMINAR_GUARDIAN_LIGHT("luminar_guardian_light", VillageRole.LUMINAR, 4, "수호의 빛", 30, 1200, 18, "1차 전직 기술. 가장 위급한 아군을 크게 치유하고 즉시 보호막을 씌웁니다."),
        LUMINAR_HOLY_PURGE("luminar_holy_purge", VillageRole.LUMINAR, 5, "성역 정화", 30, 1450, 24, "1차 전직 기술. 주변 아군을 정화하고 회복하며 짧은 피해 저항을 부여합니다."),
        LUMINAR_REVIVAL_WAVE("luminar_revival_wave", VillageRole.LUMINAR, 6, "회생 파동", 30, 1750, 30, "1차 전직 기술. 넓은 범위의 아군을 회복하고 재생·보호막으로 전선을 복구합니다."),
        LUMINAR_JUDGEMENT("luminar_judgement", VillageRole.LUMINAR, 7, "심판광", 30, 2150, 34, "1차 전직 기술. 주변 적을 성광으로 타격하는 동시에 가까운 아군을 회복합니다."),
        LUMINAR_HEAVENLY_BARRIER("luminar_heavenly_barrier", VillageRole.LUMINAR, 8, "천상의 방벽", 60, 3300, 32, "2차 전직 기술. 전장 아군에게 두꺼운 보호막과 피해 저항을 부여합니다."),
        LUMINAR_RETURNING_LIGHT("luminar_returning_light", VillageRole.LUMINAR, 9, "회귀의 빛", 60, 3900, 30, "2차 전직 기술. 가장 위급한 아군을 즉시 전투선으로 복귀시킬 정도로 크게 회복합니다."),
        LUMINAR_RESURRECTION_HYMN("luminar_resurrection_hymn", VillageRole.LUMINAR, 10, "부활 성가", 60, 4700, 58, "2차 전직 기술. 전투 불능 아군을 되살리고 살아 있는 아군에게 재생을 부여합니다."),
        LUMINAR_LAST_MIRACLE("luminar_last_miracle", VillageRole.LUMINAR, 11, "최후의 기적", 60, 5700, 64, "2차 전직 기술. 아군을 치유·보호하면서 주변 적에게도 강한 성광 피해를 가합니다."),

        WARDEN_TAUNT("warden_taunt", VillageRole.WARDEN, 0, "수호 돌진", 2, 70, 18, "방패를 앞세워 전방으로 돌진하고 접촉한 적에게 피해를 주며 강하게 밀어냅니다."),
        WARDEN_BASH("warden_bash", VillageRole.WARDEN, 1, "위압의 함성", 7, 190, 22, "넓은 범위의 적을 강제로 자신에게 돌리고 약화시킵니다."),
        WARDEN_FORMATION("warden_formation", VillageRole.WARDEN, 2, "거대 방패 태세", 13, 380, 32, "이동을 멈추고 사방에서 받는 피해를 크게 줄이며 주변 적을 밀어내고 도발합니다. 유지 중 다시 사용하면 즉시 방패를 내립니다."),
        WARDEN_FIELD("warden_field", VillageRole.WARDEN, 3, "대수호 진군", 21, 680, 46, "거대한 에너지 방패로 전방과 측면 압박을 버티며 적을 밀어내고 도발합니다. 유지 중 다시 사용하면 즉시 해제합니다."),
        WARDEN_GATE_IMPACT("warden_gate_impact", VillageRole.WARDEN, 4, "성문 충격", 30, 1200, 20, "1차 전직 기술. 전방을 방패로 찍어 적을 크게 밀어내고 강제로 자신에게 돌립니다."),
        WARDEN_FORCED_CHALLENGE("warden_forced_challenge", VillageRole.WARDEN, 5, "강제 도전", 30, 1450, 26, "1차 전직 기술. 넓은 범위의 적을 장시간 도발하고 공격 능력을 약화시킵니다."),
        WARDEN_GUARD_BARRIER("warden_guard_barrier", VillageRole.WARDEN, 6, "수호 결계", 30, 1750, 34, "1차 전직 기술. 자신과 주변 아군에게 피해 저항과 보호막을 부여합니다."),
        WARDEN_IRON_PULSE("warden_iron_pulse", VillageRole.WARDEN, 7, "철벽 파동", 30, 2150, 32, "1차 전직 기술. 사방으로 충격파를 내보내 적을 밀치고 도발해 전선을 다시 세웁니다."),
        WARDEN_UNBROKEN_WALL("warden_unbroken_wall", VillageRole.WARDEN, 8, "불락 방벽", 60, 3300, 34, "2차 전직 기술. 강력한 보호막과 저항을 얻고 주변 적의 공격을 자신에게 고정합니다."),
        WARDEN_FORTRESS_CHARGE("warden_fortress_charge", VillageRole.WARDEN, 9, "성채 돌진", 60, 3900, 30, "2차 전직 기술. 긴 거리를 돌진하며 경로의 적을 강하게 밀어내고 피해를 줍니다."),
        WARDEN_ABSOLUTE_FORMATION("warden_absolute_formation", VillageRole.WARDEN, 10, "절대 방진", 60, 4700, 46, "2차 전직 기술. 주변 아군 전체의 생존력을 크게 높이고 적을 자신에게 끌어옵니다."),
        WARDEN_FORTRESS_DESCENT("warden_fortress_descent", VillageRole.WARDEN, 11, "성채 강림", 60, 5700, 58, "2차 전직 기술. 지면에 거대한 수호 충격을 일으켜 적 진형을 날리고 아군을 보호합니다.");

        private final String id;
        private final VillageRole role;
        private final int roleIndex;
        private final String displayName;
        private final int requiredLevel;
        private final int coinCost;
        private final int baseCooldownSeconds;
        private final String description;

        ActiveSkill(
                String id,
                VillageRole role,
                int roleIndex,
                String displayName,
                int requiredLevel,
                int coinCost,
                int baseCooldownSeconds,
                String description) {
            this.id = id;
            this.role = role;
            this.roleIndex = roleIndex;
            this.displayName = displayName;
            this.requiredLevel = requiredLevel;
            this.coinCost = coinCost;
            this.baseCooldownSeconds = baseCooldownSeconds;
            this.description = description;
        }

        public String id() { return id; }
        public VillageRole role() { return role; }
        public int roleIndex() { return roleIndex; }
        public String displayName() { return displayName; }
        public int requiredLevel() { return requiredLevel; }
        public int coinCost() { return coinCost; }
        public int baseCooldownSeconds() { return baseCooldownSeconds; }
        public String description() { return description; }
        public int promotionTier() { return roleIndex >= 8 ? 2 : roleIndex >= 4 ? 1 : 0; }
        public int promotionSlot() { return roleIndex < 4 ? roleIndex : (roleIndex - 4) % 4; }

        public static Optional<ActiveSkill> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(skill -> skill.id.equals(normalized)).findFirst();
        }

        public static int maxRoleSkillCount() {
            return 12;
        }
    }
}

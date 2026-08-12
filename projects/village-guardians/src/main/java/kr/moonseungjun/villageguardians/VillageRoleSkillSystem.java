package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

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

    public static ScalingCoverage scalingCoverage(ActiveSkill skill) {
        if (skill == null) return new ScalingCoverage(false, false, false, "기술 없음");
        return switch (skill) {
            case VANGUARD_WHIRLWIND -> new ScalingCoverage(true, true, true,
                    "위력=틱 피해 · 지속=회전 시간 · 특수=범위/대상/밀치기");
            case VANGUARD_BREAKER -> new ScalingCoverage(true, true, true,
                    "위력=공격 증폭 · 지속=버프 시간 · 특수=효과 단계/아군 강화");
            case VANGUARD_CRY -> new ScalingCoverage(true, true, true,
                    "위력=검기 피해 · 지속=검기 횟수 · 특수=검기 크기/사거리");
            case VANGUARD_STORM -> new ScalingCoverage(true, true, true,
                    "위력=강하 피해 · 지속=균열 약화 시간 · 특수=범위/제어");
            case RANGER_VOLLEY -> new ScalingCoverage(true, true, true,
                    "위력=본체/추가 화살 피해 · 지속=준비 시간 · 특수=장전/추가 화살 수");
            case RANGER_PIERCE -> new ScalingCoverage(true, true, true,
                    "위력=추적/도탄 피해 · 지속=준비 시간 · 특수=도탄 반경/대상 수");
            case RANGER_RICOCHET -> new ScalingCoverage(true, true, true,
                    "위력=화살비 피해 · 지속=준비/장판 시간 · 특수=범위/화상");
            case RANGER_FIRE_RAIN -> new ScalingCoverage(true, true, true,
                    "위력=대궁 피해 · 지속=준비 시간 · 특수=크기/관통 범위");
            case ARCANIST_FIRE_ORB -> new ScalingCoverage(true, true, true,
                    "위력=폭발 피해 · 지속=비행 사거리 · 특수=폭발 범위/화상");
            case ARCANIST_FROST_RING -> new ScalingCoverage(true, true, true,
                    "위력=지속 피해 · 지속=장판 시간 · 특수=범위/사거리");
            case ARCANIST_CHAIN -> new ScalingCoverage(true, true, true,
                    "위력=회랑 피해 · 지속=토네이도 시간 · 특수=범위/제어");
            case ARCANIST_NOVA -> new ScalingCoverage(true, true, true,
                    "위력=낙뢰 피해 · 지속=폭격 시간 · 특수=범위/낙뢰 대상");
            case LUMINAR_HEAL -> new ScalingCoverage(true, true, true,
                    "위력=즉시 회복 · 지속=재생/보호막 시간 · 특수=보호막 강도");
            case LUMINAR_CLEANSE -> new ScalingCoverage(true, true, true,
                    "위력=회복량 · 지속=정화 후 보호 시간 · 특수=보호막/정화 범위");
            case LUMINAR_VEIL -> new ScalingCoverage(true, true, true,
                    "위력=틱 회복 · 지속=성역 시간 · 특수=범위/보호");
            case LUMINAR_SANCTUARY -> new ScalingCoverage(true, true, true,
                    "위력=전체 회복 · 지속=보호막/재생 시간 · 특수=보호막 강도/부활");
            case WARDEN_TAUNT -> new ScalingCoverage(true, true, true,
                    "위력=돌진 피해 · 지속=돌진 거리 · 특수=방패 폭/밀치기");
            case WARDEN_BASH -> new ScalingCoverage(true, true, true,
                    "위력=함성 피해 · 지속=도발/약화 시간 · 특수=범위/약화 단계");
            case WARDEN_FORMATION -> new ScalingCoverage(true, true, true,
                    "위력=보호막/접촉 피해 · 지속=태세 시간 · 특수=방패 범위/밀치기");
            case WARDEN_FIELD -> new ScalingCoverage(true, true, true,
                    "위력=보호막/진군 피해 · 지속=진군 시간 · 특수=방패 범위/저항");
        };
    }

    public static boolean allSkillBranchesConnected() {
        return Arrays.stream(ActiveSkill.values()).allMatch(skill -> scalingCoverage(skill).complete());
    }

    public record ScalingCoverage(boolean power, boolean duration, boolean special, String detail) {
        public boolean complete() { return power && duration && special; }
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
        boolean testing = VillageSkillTestSystem.isEnabled(player);
        VillageRole role = testing
                ? VillageSkillTestSystem.selectedRole(player)
                : VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            return "마을 회관에서 직업을 먼저 배치해야 합니다.";
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
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageEquipmentShop.roleSkillMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player);
        float duration = durationMultiplier(player, role);
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
        return Math.max(minimum,
                skill.baseCooldownSeconds()
                        - VillageProgressionSystem.skillCooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.cooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.mobilityCooldownReductionSeconds(player)
                        - roleTreeCooldownReductionSeconds(player, role)
                        - VillageEquipmentShop.cooldownReductionSeconds(player)
                        - VillageRelicSystem.cooldownReductionSeconds(player));
    }

    public static String useTestSkill(ServerPlayer player, String skillId) {
        if (!VillageSkillTestSystem.isEnabled(player)) return "먼저 기술 시험 모드를 활성화해야 합니다.";
        ActiveSkill skill = ActiveSkill.parse(skillId).orElse(null);
        VillageRole role = VillageSkillTestSystem.selectedRole(player);
        if (skill == null || skill.role() != role) return "현재 시험 직업의 기술만 시험할 수 있습니다.";
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 시험할 수 없습니다.";
        cast(level, player, skill, powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player),
                durationMultiplier(player, role), specialRank(player, role));
        return skill.displayName() + " 시험 시전 완료 · 비용과 재사용 대기시간 없음";
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
        switch (skill) {
            case VANGUARD_WHIRLWIND -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case VANGUARD_BREAKER -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case VANGUARD_CRY -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case VANGUARD_STORM -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case RANGER_VOLLEY -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case RANGER_PIERCE -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case RANGER_RICOCHET -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case RANGER_FIRE_RAIN -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case ARCANIST_FIRE_ORB -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case ARCANIST_FROST_RING -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case ARCANIST_CHAIN -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case ARCANIST_NOVA -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case LUMINAR_HEAL -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case LUMINAR_CLEANSE -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case LUMINAR_VEIL -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case LUMINAR_SANCTUARY -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case WARDEN_TAUNT -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case WARDEN_BASH -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case WARDEN_FORMATION -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case WARDEN_FIELD -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
        }
    }

    private static List<Mob> damageArea(
            ServerLevel level,
            ServerPlayer player,
            double radius,
            int limit,
            float damage,
            int specialRank,
            boolean lifeSteal) {
        List<Mob> targets = VillageSkillTestSystem.isEnabled(player)
                ? VillageSkillTestSystem.targetsNear(level, player, radius, limit + specialRank)
                : VillageRaidSystem.activeEnemiesNear(
                        level, player.position(), radius, limit + specialRank, null);
        int hits = 0;
        for (Mob target : targets) {
            if (!target.isAlive()) {
                continue;
            }
            float finalDamage = damage;
            if (specialRank >= 3 && target.getHealth() <= target.getMaxHealth() * 0.30f) {
                finalDamage *= 1.28f;
            }
            target.hurtServer(level, level.damageSources().magic(), finalDamage);
            if (specialRank >= 2) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50 + specialRank * 20, 0));
            }
            hits++;
        }
        if (lifeSteal && specialRank >= 1 && hits > 0) {
            player.heal(Math.min(8.0f, hits * (0.55f + specialRank * 0.20f)));
        }
        return targets;
    }

    private static void healAllies(
            ServerPlayer player,
            double radius,
            float heal,
            int duration,
            int specialRank,
            boolean barrier) {
        for (ServerPlayer ally : allies(player, radius)) {
            ally.heal(heal);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, Math.min(2, specialRank)));
            if (barrier || specialRank >= 2) {
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.min(4, 1 + specialRank)));
            }
        }
    }

    private static List<ServerPlayer> allies(ServerPlayer player, double radius) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return List.of(player);
        }
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(other -> other.level() == player.level() && other.distanceToSqr(player) <= squared)
                .toList();
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
                case DURATION -> tier <= 3
                        ? "모든 " + role.displayName() + " 기술의 강화·제어 지속시간이 단계당 16% 증가합니다."
                        : "고급 지속 단계입니다. 지속시간 +11%, IV·V 단계마다 기술 재사용 대기시간도 1초 감소합니다.";
                case POWER -> tier <= 3
                        ? "모든 " + role.displayName() + " 기술의 피해 또는 치유량이 증가하며 3단계에서 추가 증폭됩니다."
                        : "고급 위력 단계입니다. 기술 피해 또는 치유량이 단계당 추가 11% 증가합니다.";
                case SPECIAL -> (switch (role) {
                    case VANGUARD -> "기술 적중 시 흡혈, 약화, 낮은 체력 적 처형 보정을 순서대로 추가합니다.";
                    case RANGER -> "사격 기술의 대상 수, 약화, 낮은 체력 적 마무리 능력을 강화합니다.";
                    case ARCANIST -> "원소 기술의 대상 수와 약화 효과, 마무리 폭발력을 강화합니다.";
                    case LUMINAR -> "치유 기술에 재생과 흡수 보호막을 추가하고 보호 강도를 높입니다.";
                    case WARDEN -> "도발·방패 기술의 약화와 둔화, 아군 보호막을 강화합니다.";
                }) + (tier >= 4 ? " 고급 단계에서는 대상 수와 효과 강도가 더 오르고 재사용 대기시간이 단계당 1초 감소합니다." : "");
            };
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
        VANGUARD_WHIRLWIND("vanguard_whirlwind", VillageRole.VANGUARD, 0, "회전 검무", 2, 70, 18, "가렌의 회전 공격처럼 몸을 돌리며 여러 차례 주변 적을 베고 이동할 수 있습니다."),
        VANGUARD_BREAKER("vanguard_breaker", VillageRole.VANGUARD, 1, "전투 고양", 7, 190, 24, "검을 치켜들고 함성을 질러 자신과 주변 아군의 공격력·이동 속도를 강화합니다."),
        VANGUARD_CRY("vanguard_cry", VillageRole.VANGUARD, 2, "검기 난무", 13, 380, 32, "자세를 잡고 검을 연속으로 휘둘러 전방에 여러 개의 실제 검기 투사체를 날립니다."),
        VANGUARD_STORM("vanguard_storm", VillageRole.VANGUARD, 3, "천붕 강하", 21, 680, 42, "공중으로 도약한 뒤 지면을 내려찍어 바닥을 깨뜨리고 넓은 범위에 피해와 강한 충격을 줍니다."),

        RANGER_VOLLEY("ranger_volley", VillageRole.RANGER, 0, "신속 삼연사", 2, 70, 16, "기술 사용 후 다음 활은 빠르게 자동 완충·발사되며, 다음 실제 활·석궁 발사 한 번이 세 갈래 화살로 강화됩니다."),
        RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "추적 도탄", 7, 190, 22, "기술 사용 후 다음 실제 활·석궁 한 발이 전방 표적을 추적합니다. 표적이 사라지면 비행 경로 전방의 새 적을 재포착하고, 적중 후에는 가까운 적을 중복 없이 순차 도탄합니다."),
        RANGER_RICOCHET("ranger_ricochet", VillageRole.RANGER, 2, "천공 화살비", 13, 380, 30, "기술 사용 후 다음 실제 활·석궁 발사 시 조준한 바닥에 짧고 강한 화살비가 펼쳐져 지속 광역 피해를 줍니다."),
        RANGER_FIRE_RAIN("ranger_fire_rain", VillageRole.RANGER, 3, "성멸 대궁", 21, 680, 40, "기술 사용 후 다음 실제 활·석궁 발사를 밝은 초록색 초대형 성멸 화살로 바꾸어 넓은 전방을 관통합니다."),

        ARCANIST_FIRE_ORB("arcanist_fire_orb", VillageRole.ARCANIST, 0, "홍염탄", 2, 70, 18, "실제 화염 구체를 전방으로 날려 충돌 지점에서 폭발시키고 적을 불태웁니다."),
        ARCANIST_FROST_RING("arcanist_frost_ring", VillageRole.ARCANIST, 1, "빙결 지대", 7, 190, 24, "조준 위치에 지속되는 냉기 지대를 만들어 범위 안 적을 강하게 둔화하고 조금씩 피해를 줍니다."),
        ARCANIST_CHAIN("arcanist_chain", VillageRole.ARCANIST, 2, "폭풍 회랑", 13, 380, 30, "전진하는 토네이도를 만들어 적을 끌어올리고 휩쓸며 낮은 피해와 강한 군중 제어를 가합니다."),
        ARCANIST_NOVA("arcanist_nova", VillageRole.ARCANIST, 3, "천뢰 폭격", 21, 680, 44, "넓은 목표 지점에 번개가 연속으로 떨어져 다수의 적에게 강한 광역 피해를 줍니다."),

        LUMINAR_HEAL("luminar_heal", VillageRole.LUMINAR, 0, "응급 성광", 2, 70, 16, "현재 체력 비율이 가장 낮은 아군 한 명을 찾아 큰 폭으로 즉시 회복시킵니다."),
        LUMINAR_CLEANSE("luminar_cleanse", VillageRole.LUMINAR, 1, "전군 정화", 7, 190, 24, "같은 전장에 있는 모든 아군의 해로운 효과를 제거하고 소량 회복시킵니다."),
        LUMINAR_VEIL("luminar_veil", VillageRole.LUMINAR, 2, "치유 성역", 13, 380, 32, "주변에 오래 지속되는 회복 지대를 설치해 범위 안 아군을 반복해서 치유합니다."),
        LUMINAR_SANCTUARY("luminar_sanctuary", VillageRole.LUMINAR, 3, "기적의 대성역", 21, 680, 46, "전장 전체 아군을 크게 치유하고 보호막을 부여하며 전투 불능 아군을 즉시 부활시킵니다."),

        WARDEN_TAUNT("warden_taunt", VillageRole.WARDEN, 0, "수호 돌진", 2, 70, 18, "방패를 앞세워 전방으로 돌진하고 접촉한 적에게 피해를 주며 강하게 밀어냅니다."),
        WARDEN_BASH("warden_bash", VillageRole.WARDEN, 1, "위압의 함성", 7, 190, 22, "큰 소리를 질러 주변 적에게 약한 피해를 주고 잠시 자신을 공격하도록 도발합니다."),
        WARDEN_FORMATION("warden_formation", VillageRole.WARDEN, 2, "거대 방패 태세", 13, 380, 32, "잠시 이동할 수 없는 대신 거대한 보호막과 피해 저항을 얻고 가까운 적을 계속 밀어냅니다."),
        WARDEN_FIELD("warden_field", VillageRole.WARDEN, 3, "대수호 진군", 21, 680, 46, "전방에 거대한 반투명 에너지 방패를 전개하고 달리면 짧게 돌진하며 접촉한 적을 밀어냅니다.");

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

        public static Optional<ActiveSkill> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(skill -> skill.id.equals(normalized)).findFirst();
        }

        public static int maxRoleSkillCount() {
            return 4;
        }
    }
}

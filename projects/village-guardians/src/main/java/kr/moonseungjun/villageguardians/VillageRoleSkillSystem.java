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
        return "Z: " + first + " | X: " + second;
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
        String key = slot == 0 ? "§bZ" : "§dX";
        ActiveSkill skill = equippedSkill(player, slot).orElse(null);
        if (skill == null) return key + " §8비어 있음";
        int remaining = cooldownRemainingSeconds(player, slot);
        String state = remaining > 0 ? "§c" + remaining + "초" : "§a준비";
        return key + " §f" + skill.displayName() + " " + state;
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
                    ? "시험 슬롯 " + (slot == 0 ? "Z" : "X") + "이 비어 있습니다. 시험 관리함에서 기술을 장착하세요."
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
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player);
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
        return Math.max(7,
                skill.baseCooldownSeconds()
                        - VillageProgressionSystem.skillCooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.cooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.mobilityCooldownReductionSeconds(player)
                        - roleTreeCooldownReductionSeconds(player, role));
    }

    public static String useTestSkill(ServerPlayer player, String skillId) {
        if (!VillageSkillTestSystem.isEnabled(player)) return "먼저 기술 시험 모드를 활성화해야 합니다.";
        ActiveSkill skill = ActiveSkill.parse(skillId).orElse(null);
        VillageRole role = VillageSkillTestSystem.selectedRole(player);
        if (skill == null || skill.role() != role) return "현재 시험 직업의 기술만 시험할 수 있습니다.";
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 시험할 수 없습니다.";
        cast(level, player, skill, powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player),
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
        int playerLevel = VillageCouncilState.levelOf(player.getUUID());
        int buffTicks = Math.round((100 + playerLevel * 4) * durationMultiplier);
        switch (skill) {
            case VANGUARD_WHIRLWIND -> damageArea(level, player, 5.0, 8,
                    (6.0f + playerLevel * 0.35f) * power, specialRank, true);
            case VANGUARD_BREAKER -> {
                damageArea(level, player, 6.5, 6,
                        (9.0f + playerLevel * 0.48f) * power, specialRank, true);
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, buffTicks, Math.min(2, specialRank)));
            }
            case VANGUARD_CRY -> {
                for (ServerPlayer ally : allies(player, 12.0)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, buffTicks, specialRank >= 2 ? 1 : 0));
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, buffTicks, specialRank >= 3 ? 1 : 0));
                }
            }
            case VANGUARD_STORM -> damageArea(level, player, 8.0, 14,
                    (11.0f + playerLevel * 0.60f) * power, Math.max(1, specialRank), true);

            case RANGER_VOLLEY -> damageArea(level, player, 12.0, 5,
                    (5.5f + playerLevel * 0.32f) * power, specialRank, false);
            case RANGER_PIERCE -> damageArea(level, player, 15.0, 4,
                    (10.0f + playerLevel * 0.55f) * power, specialRank, false);
            case RANGER_RICOCHET -> damageArea(level, player, 13.0, 9,
                    (7.0f + playerLevel * 0.40f) * power, Math.max(1, specialRank), false);
            case RANGER_FIRE_RAIN -> {
                List<Mob> targets = damageArea(level, player, 14.0, 14,
                        (8.0f + playerLevel * 0.44f) * power, specialRank, false);
                for (Mob target : targets) {
                    target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100 + specialRank * 40));
                }
            }

            case ARCANIST_FIRE_ORB -> {
                List<Mob> targets = damageArea(level, player, 9.0, 7,
                        (8.5f + playerLevel * 0.50f) * power, specialRank, false);
                targets.forEach(target -> target.setRemainingFireTicks(90 + specialRank * 35));
            }
            case ARCANIST_FROST_RING -> {
                List<Mob> targets = damageArea(level, player, 7.0, 10,
                        (5.0f + playerLevel * 0.30f) * power, specialRank, false);
                targets.forEach(target -> target.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS, buffTicks, Math.min(3, 1 + specialRank))));
            }
            case ARCANIST_CHAIN -> damageArea(level, player, 12.0, 12,
                    (7.5f + playerLevel * 0.46f) * power, Math.max(1, specialRank), false);
            case ARCANIST_NOVA -> damageArea(level, player, 9.0, 16,
                    (12.0f + playerLevel * 0.65f) * power, Math.max(2, specialRank), false);

            case LUMINAR_HEAL -> healAllies(player, 10.0,
                    (7.0f + playerLevel * 0.55f) * power, buffTicks, specialRank, false);
            case LUMINAR_CLEANSE -> {
                for (ServerPlayer ally : allies(player, 11.0)) {
                    ally.removeEffect(MobEffects.POISON);
                    ally.removeEffect(MobEffects.WITHER);
                    ally.removeEffect(MobEffects.WEAKNESS);
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, buffTicks, specialRank >= 2 ? 1 : 0));
                }
            }
            case LUMINAR_VEIL -> healAllies(player, 12.0,
                    (4.0f + playerLevel * 0.30f) * power, buffTicks * 2, specialRank, true);
            case LUMINAR_SANCTUARY -> healAllies(player, 14.0,
                    (11.0f + playerLevel * 0.70f) * power, buffTicks * 2, Math.max(2, specialRank), true);

            case WARDEN_TAUNT -> {
                List<Mob> tauntTargets = VillageSkillTestSystem.isEnabled(player)
                        ? VillageSkillTestSystem.targetsNear(level, player, 9.0, 14)
                        : VillageRaidSystem.activeEnemiesNear(level, player.position(), 9.0, 14, null);
                for (Mob target : tauntTargets) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, buffTicks, Math.min(2, specialRank)));
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, buffTicks, Math.min(2, specialRank)));
                }
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, buffTicks, 1 + Math.min(1, specialRank)));
            }
            case WARDEN_BASH -> {
                List<Mob> targets = damageArea(level, player, 5.0, 8,
                        (6.5f + playerLevel * 0.35f) * power, specialRank, true);
                targets.forEach(target -> target.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS, 40 + specialRank * 20, 4)));
            }
            case WARDEN_FORMATION -> {
                for (ServerPlayer ally : allies(player, 10.0)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, buffTicks * 2, specialRank >= 2 ? 1 : 0));
                    ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, buffTicks * 2, Math.min(3, specialRank)));
                }
            }
            case WARDEN_FIELD -> {
                for (ServerPlayer ally : allies(player, 14.0)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, buffTicks * 2, 2));
                    ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, buffTicks * 2, 2 + Math.min(2, specialRank)));
                }
            }
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
        VANGUARD_WHIRLWIND("vanguard_whirlwind", VillageRole.VANGUARD, 0, "회전 참격", 2, 70, 18, "주변 적을 한 번에 베고 특수 경로에 따라 체력을 회복합니다."),
        VANGUARD_BREAKER("vanguard_breaker", VillageRole.VANGUARD, 1, "돌진 분쇄", 7, 190, 24, "전방 돌파를 표현한 강한 범위 일격과 이동 가속을 얻습니다."),
        VANGUARD_CRY("vanguard_cry", VillageRole.VANGUARD, 2, "전장의 포효", 13, 380, 32, "주변 아군의 공격력과 이동 속도를 강화합니다."),
        VANGUARD_STORM("vanguard_storm", VillageRole.VANGUARD, 3, "검기 폭풍", 21, 680, 42, "넓은 범위의 다수 적에게 강한 검기 피해를 가합니다."),

        RANGER_VOLLEY("ranger_volley", VillageRole.RANGER, 0, "연발 사격", 2, 70, 16, "주변 여러 적에게 빠른 원거리 피해를 분산합니다."),
        RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "관통 사격", 7, 190, 22, "적은 수의 정예 대상에게 높은 관통 피해를 줍니다."),
        RANGER_RICOCHET("ranger_ricochet", VillageRole.RANGER, 2, "도탄 연쇄", 13, 380, 30, "여러 적 사이를 튕기는 연쇄 사격을 가합니다."),
        RANGER_FIRE_RAIN("ranger_fire_rain", VillageRole.RANGER, 3, "화염 폭우", 21, 680, 40, "넓은 범위의 적을 불태우는 대규모 사격을 실행합니다."),

        ARCANIST_FIRE_ORB("arcanist_fire_orb", VillageRole.ARCANIST, 0, "화염 구체", 2, 70, 18, "주변 적에게 폭발 피해와 화염을 가합니다."),
        ARCANIST_FROST_RING("arcanist_frost_ring", VillageRole.ARCANIST, 1, "서리 고리", 7, 190, 24, "주변 적에게 냉기 피해와 강한 둔화를 부여합니다."),
        ARCANIST_CHAIN("arcanist_chain", VillageRole.ARCANIST, 2, "연쇄 번개", 13, 380, 30, "다수의 적에게 연쇄되는 비전 피해를 가합니다."),
        ARCANIST_NOVA("arcanist_nova", VillageRole.ARCANIST, 3, "비전 폭발", 21, 680, 44, "넓은 범위의 적을 한 번에 폭발시키는 궁극 기술입니다."),

        LUMINAR_HEAL("luminar_heal", VillageRole.LUMINAR, 0, "치유의 빛", 2, 70, 16, "주변 아군의 체력을 즉시 회복합니다."),
        LUMINAR_CLEANSE("luminar_cleanse", VillageRole.LUMINAR, 1, "정화 기도", 7, 190, 24, "주변 아군의 주요 해로운 효과를 제거하고 재생을 부여합니다."),
        LUMINAR_VEIL("luminar_veil", VillageRole.LUMINAR, 2, "재생 장막", 13, 380, 32, "아군을 치유하고 오래 지속되는 재생과 보호막을 부여합니다."),
        LUMINAR_SANCTUARY("luminar_sanctuary", VillageRole.LUMINAR, 3, "생명 성역", 21, 680, 46, "넓은 범위의 아군을 크게 치유하고 강한 보호막을 부여합니다."),

        WARDEN_TAUNT("warden_taunt", VillageRole.WARDEN, 0, "도발의 함성", 2, 70, 18, "주변 적을 약화·둔화하고 자신에게 저항을 부여합니다."),
        WARDEN_BASH("warden_bash", VillageRole.WARDEN, 1, "방패 충격", 7, 190, 22, "주변 적에게 피해와 매우 강한 짧은 둔화를 가합니다."),
        WARDEN_FORMATION("warden_formation", VillageRole.WARDEN, 2, "철벽 진형", 13, 380, 32, "주변 아군에게 저항과 흡수 보호막을 부여합니다."),
        WARDEN_FIELD("warden_field", VillageRole.WARDEN, 3, "수호 결계", 21, 680, 46, "넓은 범위의 아군에게 강한 저항과 보호막을 부여합니다.");

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

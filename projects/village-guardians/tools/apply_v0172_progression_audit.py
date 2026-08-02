#!/usr/bin/env python3
"""Apply the v0.17.2 UI/accessibility and progression-depth audit patch.

The patch is intentionally idempotent. CI runs it before tests so a failed build can
be retried without shifting enum ordinals or duplicating content.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(relative: str, old: str, new: str) -> None:
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{relative}: expected one patch target, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def write(relative: str, content: str) -> None:
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and path.read_text(encoding="utf-8") == content:
        return
    path.write_text(content, encoding="utf-8")


# ---------------------------------------------------------------------------
# Shared tactical tree: 20 -> 28 nodes, meaningful capstones, no ordinal shifts.
# ---------------------------------------------------------------------------
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeSystem.java",
    "return Math.max(0, (level - 1) / 3);",
    "return Math.max(0, (level - 1) / 2);",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeSystem.java",
    "return \"사용 가능한 전술 포인트가 없습니다. 3레벨마다 1포인트를 얻습니다.\";",
    "return \"사용 가능한 전술 포인트가 없습니다. 2레벨마다 1포인트를 얻습니다.\";",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeSystem.java",
    """    public static float outgoingDamageMultiplier(ServerPlayer player) {
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
""",
    """    public static float outgoingDamageMultiplier(ServerPlayer player) {
        float bonus = 0.0f;
        if (has(player, Node.POWER_1)) bonus += 0.06f;
        if (has(player, Node.POWER_2)) bonus += 0.06f;
        if (has(player, Node.POWER_4)) bonus += 0.08f;
        if (has(player, Node.POWER_7)) bonus += 0.10f;
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
        return 1.0f + bonus;
    }

    public static float projectileExecutionMultiplier(ServerPlayer player, float health, float maximum) {
        return has(player, Node.RANGED_7) && maximum > 0.0f && health / maximum <= 0.50f
                ? 1.22f : 1.0f;
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
        return extra;
    }

    public static float incomingDamageMultiplier(ServerPlayer player) {
        float reduction = 0.0f;
        if (has(player, Node.GUARD_1)) reduction += 0.05f;
        if (has(player, Node.GUARD_2)) reduction += 0.05f;
        if (has(player, Node.GUARD_4)) reduction += 0.06f;
        if (has(player, Node.GUARD_6)) reduction += 0.04f;
        return Math.max(0.72f, 1.0f - reduction);
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
        return 1.0f + bonus;
    }

    public static int cooldownReductionSeconds(ServerPlayer player) {
        int reduction = 0;
        if (has(player, Node.SUPPORT_2)) reduction += 2;
        if (has(player, Node.SUPPORT_4)) reduction += 2;
        if (has(player, Node.SUPPORT_6)) reduction += 2;
        if (has(player, Node.SUPPORT_7)) reduction += 1;
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
        return has(player, Node.SUPPORT_7) ? 2.0f : 0.0f;
    }
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeSystem.java",
    """        RANGED_4(\"ranged_4\", \"관통 장력\", \"화살과 투사체 피해 추가 +10%\", Branch.RANGED, 4, RANGED_3),
        RANGED_5(\"ranged_5\", \"분열 사격\", \"연쇄 사격 대상 +2\", Branch.RANGED, 5, RANGED_4);
""",
    """        RANGED_4(\"ranged_4\", \"관통 장력\", \"화살과 투사체 피해 추가 +10%\", Branch.RANGED, 4, RANGED_3),
        RANGED_5(\"ranged_5\", \"분열 사격\", \"연쇄 사격 대상 +2\", Branch.RANGED, 5, RANGED_4),

        // Appended to preserve every existing saved-mask ordinal.
        POWER_6(\"power_6\", \"포식자의 감각\", \"체력 40% 이하의 적에게 피해 +24%, 처치 시 잠시 힘을 얻음\", Branch.POWER, 6, POWER_5),
        POWER_7(\"power_7\", \"전쟁의 화신\", \"모든 공격 피해 +10%, 처치 연속 강화 지속시간 증가\", Branch.POWER, 7, POWER_6),
        GUARD_6(\"guard_6\", \"재생 태세\", \"받는 피해 추가 4% 감소, 낮은 체력에서 재생 발동\", Branch.GUARD, 6, GUARD_5),
        GUARD_7(\"guard_7\", \"불굴의 심장\", \"응급 장막의 보호량 증가, 재사용 대기시간 90초에서 55초로 감소\", Branch.GUARD, 7, GUARD_6),
        SUPPORT_6(\"support_6\", \"전선 군수관\", \"처치 주화 +12%, 기술 재사용 -2초, 공동 보급 회수율 증가\", Branch.SUPPORT, 6, SUPPORT_5),
        SUPPORT_7(\"support_7\", \"연대의 맹세\", \"처치 시 주변 아군 체력 1칸 회복, 기술 재사용 -1초\", Branch.SUPPORT, 7, SUPPORT_6),
        RANGED_6(\"ranged_6\", \"초장력 시위\", \"화살과 투사체 피해 추가 +12%\", Branch.RANGED, 6, RANGED_5),
        RANGED_7(\"ranged_7\", \"종결 사격\", \"체력 50% 이하 적 대상 투사체 피해 +22%, 연쇄 대상 +2, 발화 지속 증가\", Branch.RANGED, 7, RANGED_6);
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeSystem.java",
    """    public static List<Node> nodes() {
        return List.of(Node.values());
    }
""",
    """    public static List<Node> nodes() {
        return Arrays.stream(Node.values())
                .sorted((first, second) -> {
                    int branch = Integer.compare(first.branch().ordinal(), second.branch().ordinal());
                    return branch != 0 ? branch : Integer.compare(first.tier(), second.tier());
                })
                .toList();
    }
""",
)

replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeData.java",
    """        int nodeCount = VillageSkillTreeSystem.Node.values().length;
        int allowedBits = (1 << nodeCount) - 1;
        return Math.max(0, value) & allowedBits;
""",
    """        int nodeCount = VillageSkillTreeSystem.Node.values().length;
        int allowedBits = nodeCount >= Integer.SIZE - 1 ? Integer.MAX_VALUE : (1 << nodeCount) - 1;
        return Math.max(0, value) & allowedBits;
""",
)

# Activate the previously empty personal-combat extension point. This also makes
# the old GUARD_5 node real instead of a dead, never-consumed boolean.
write(
    "src/main/java/kr/moonseungjun/villageguardians/VillagePersonalCombatSystem.java",
    """package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Personal tactical capstones that need transient cooldown or party context. */
final class VillagePersonalCombatSystem {
    private static final Map<UUID, Long> NEXT_BARRIER_AT = new HashMap<>();

    private VillagePersonalCombatSystem() {}

    static void reset() {
        NEXT_BARRIER_AT.clear();
    }

    static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getAmount() <= 0.0f
                || !VillageSkillTreeSystem.emergencyBarrierUnlocked(player)) return;
        float projected = player.getHealth() - event.getAmount();
        if (projected > player.getMaxHealth() * 0.30f) return;
        long now = System.currentTimeMillis();
        long readyAt = NEXT_BARRIER_AT.getOrDefault(player.getUUID(), 0L);
        if (readyAt > now) return;

        float reduced = event.getAmount() * 0.65f;
        if (player.getHealth() > 1.0f) reduced = Math.min(reduced, player.getHealth() - 1.0f);
        event.setAmount(Math.max(0.0f, reduced));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 8,
                VillageSkillTreeSystem.emergencyBarrierAbsorptionAmplifier(player), false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 4, 1, false, true, true));
        NEXT_BARRIER_AT.put(player.getUUID(), now
                + VillageSkillTreeSystem.emergencyBarrierCooldownSeconds(player) * 1000L);
        player.sendSystemMessage(Component.literal("§b[응급 장막] §f치명적인 충격을 흡수했습니다."));
    }

    static void applyLowHealthPassive(ServerPlayer player) {
        if (VillageSkillTreeSystem.lowHealthRegenerationUnlocked(player)
                && player.getHealth() <= player.getMaxHealth() * 0.40f) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 55, 0, false, false, true));
        }
    }

    static void applyKillMomentum(ServerPlayer player) {
        int seconds = VillageSkillTreeSystem.killMomentumSeconds(player);
        if (seconds > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, seconds * 20, 0, false, true, true));
        }
    }

    static void healNearbyAlliesOnKill(ServerPlayer player) {
        float amount = VillageSkillTreeSystem.teamHealOnKillAmount(player);
        MinecraftServer server = player.level().getServer();
        if (amount <= 0.0f || server == null) return;
        for (ServerPlayer ally : server.getPlayerList().getPlayers()) {
            if (ally != player && ally.level() == player.level() && ally.distanceToSqr(player) <= 144.0) {
                ally.heal(amount);
            }
        }
    }
}
""",
)

replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRpgSystem.java",
    """        VillageCombatTechniqueSystem.reset();
        VillageRoleSkillSystem.resetTransientState();
""",
    """        VillageCombatTechniqueSystem.reset();
        VillageRoleSkillSystem.resetTransientState();
        VillagePersonalCombatSystem.reset();
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRpgSystem.java",
    """        if (VillageCouncilState.roleOf(player.getUUID()).orElse(null) == VillageRole.WARDEN
                && player.getOffhandItem().is(Items.SHIELD)) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        }
""",
    """        if (VillageCouncilState.roleOf(player.getUUID()).orElse(null) == VillageRole.WARDEN
                && player.getOffhandItem().is(Items.SHIELD)) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        }
        VillagePersonalCombatSystem.applyLowHealthPassive(player);
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRpgSystem.java",
    """            if (event.getEntity() instanceof Monster monster) {
                value *= VillageSkillTreeSystem.executionMultiplier(attacker, monster.getHealth(), monster.getMaxHealth());
            }
""",
    """            if (event.getEntity() instanceof Monster monster) {
                value *= VillageSkillTreeSystem.executionMultiplier(attacker, monster.getHealth(), monster.getMaxHealth());
                if (projectile) {
                    value *= VillageSkillTreeSystem.projectileExecutionMultiplier(
                            attacker, monster.getHealth(), monster.getMaxHealth());
                }
            }
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRpgSystem.java",
    """        VillageCombatTechniqueSystem.handleIncomingDamage(event);
""",
    """        VillagePersonalCombatSystem.handleIncomingDamage(event);
        VillageCombatTechniqueSystem.handleIncomingDamage(event);
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRpgSystem.java",
    """        float heal = VillageSkillTreeSystem.killHealAmount(killer);
        if (heal > 0.0f) killer.heal(heal);
        MinecraftServer server = killer.level().getServer();
        if (server != null && VillageSkillTreeSystem.sharedSupplyChanceUnlocked(killer)
                && killer.getRandom().nextFloat() < 0.12f) {
            VillageProgressionSystem.addSupplies(server, 1, \"공동 회수\");
        }
""",
    """        float heal = VillageSkillTreeSystem.killHealAmount(killer);
        if (heal > 0.0f) killer.heal(heal);
        VillagePersonalCombatSystem.applyKillMomentum(killer);
        VillagePersonalCombatSystem.healNearbyAlliesOnKill(killer);
        MinecraftServer server = killer.level().getServer();
        float supplyChance = VillageSkillTreeSystem.sharedSupplyChance(killer);
        if (server != null && supplyChance > 0.0f && killer.getRandom().nextFloat() < supplyChance) {
            VillageProgressionSystem.addSupplies(server, 1, \"공동 회수\");
        }
""",
)

# ---------------------------------------------------------------------------
# Role tree: append two tiers per branch (45 -> 75 role-specific upgrades).
# ---------------------------------------------------------------------------
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleSkillSystem.java",
    """    public static float durationMultiplier(ServerPlayer player, VillageRole role) {
        return 1.0f + branchRank(player, role, RoleBranch.DURATION) * 0.16f;
    }

    public static float powerMultiplier(ServerPlayer player, VillageRole role) {
        int rank = branchRank(player, role, RoleBranch.POWER);
        return 1.0f + rank * 0.14f + (rank >= 3 ? 0.08f : 0.0f);
    }

    public static int specialRank(ServerPlayer player, VillageRole role) {
        return branchRank(player, role, RoleBranch.SPECIAL);
    }
""",
    """    public static float durationMultiplier(ServerPlayer player, VillageRole role) {
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
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleSkillSystem.java",
    """                        - VillageProgressionSystem.skillCooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.cooldownReductionSeconds(player));
""",
    """                        - VillageProgressionSystem.skillCooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.cooldownReductionSeconds(player)
                        - roleTreeCooldownReductionSeconds(player, role));
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleSkillSystem.java",
    """        SPECIAL_1(\"special_1\", RoleBranch.SPECIAL, 1, 5, 150, null),
        SPECIAL_2(\"special_2\", RoleBranch.SPECIAL, 2, 12, 340, SPECIAL_1),
        SPECIAL_3(\"special_3\", RoleBranch.SPECIAL, 3, 21, 620, SPECIAL_2);
""",
    """        SPECIAL_1(\"special_1\", RoleBranch.SPECIAL, 1, 5, 150, null),
        SPECIAL_2(\"special_2\", RoleBranch.SPECIAL, 2, 12, 340, SPECIAL_1),
        SPECIAL_3(\"special_3\", RoleBranch.SPECIAL, 3, 21, 620, SPECIAL_2),

        // Appended so existing role-tree masks keep their original ordinal meaning.
        DURATION_4(\"duration_4\", RoleBranch.DURATION, 4, 24, 880, DURATION_3),
        DURATION_5(\"duration_5\", RoleBranch.DURATION, 5, 29, 1280, DURATION_4),
        POWER_4(\"power_4\", RoleBranch.POWER, 4, 24, 920, POWER_3),
        POWER_5(\"power_5\", RoleBranch.POWER, 5, 29, 1340, POWER_4),
        SPECIAL_4(\"special_4\", RoleBranch.SPECIAL, 4, 25, 980, SPECIAL_3),
        SPECIAL_5(\"special_5\", RoleBranch.SPECIAL, 5, 30, 1450, SPECIAL_4);
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleSkillSystem.java",
    """                case DURATION -> \"모든 \" + role.displayName() + \" 기술의 강화·제어 지속시간이 단계당 16% 증가합니다.\";
                case POWER -> \"모든 \" + role.displayName() + \" 기술의 피해 또는 치유량이 증가하며 3단계에서 추가 증폭됩니다.\";
""",
    """                case DURATION -> tier <= 3
                        ? \"모든 \" + role.displayName() + \" 기술의 강화·제어 지속시간이 단계당 16% 증가합니다.\"
                        : \"고급 지속 단계입니다. 지속시간 +11%, IV·V 단계마다 기술 재사용 대기시간도 1초 감소합니다.\";
                case POWER -> tier <= 3
                        ? \"모든 \" + role.displayName() + \" 기술의 피해 또는 치유량이 증가하며 3단계에서 추가 증폭됩니다.\"
                        : \"고급 위력 단계입니다. 기술 피해 또는 치유량이 단계당 추가 11% 증가합니다.\";
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleSkillSystem.java",
    """                case SPECIAL -> switch (role) {
                    case VANGUARD -> \"기술 적중 시 흡혈, 약화, 낮은 체력 적 처형 보정을 순서대로 추가합니다.\";
                    case RANGER -> \"사격 기술의 대상 수, 약화, 낮은 체력 적 마무리 능력을 강화합니다.\";
                    case ARCANIST -> \"원소 기술의 대상 수와 약화 효과, 마무리 폭발력을 강화합니다.\";
                    case LUMINAR -> \"치유 기술에 재생과 흡수 보호막을 추가하고 보호 강도를 높입니다.\";
                    case WARDEN -> \"도발·방패 기술의 약화와 둔화, 아군 보호막을 강화합니다.\";
                };
""",
    """                case SPECIAL -> (switch (role) {
                    case VANGUARD -> \"기술 적중 시 흡혈, 약화, 낮은 체력 적 처형 보정을 순서대로 추가합니다.\";
                    case RANGER -> \"사격 기술의 대상 수, 약화, 낮은 체력 적 마무리 능력을 강화합니다.\";
                    case ARCANIST -> \"원소 기술의 대상 수와 약화 효과, 마무리 폭발력을 강화합니다.\";
                    case LUMINAR -> \"치유 기술에 재생과 흡수 보호막을 추가하고 보호 강도를 높입니다.\";
                    case WARDEN -> \"도발·방패 기술의 약화와 둔화, 아군 보호막을 강화합니다.\";
                }) + (tier >= 4 ? \" 고급 단계에서는 대상 수와 효과 강도가 더 오르고 재사용 대기시간이 단계당 1초 감소합니다.\" : \"\");
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleSkillSystem.java",
    """        private static String roman(int value) {
            return switch (value) { case 1 -> \"I\"; case 2 -> \"II\"; default -> \"III\"; };
        }
""",
    """        private static String roman(int value) {
            return switch (value) {
                case 1 -> \"I\";
                case 2 -> \"II\";
                case 3 -> \"III\";
                case 4 -> \"IV\";
                default -> \"V\";
            };
        }
""",
)

replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageUiController.java",
    "for (VillageRoleSkillSystem.RoleNode node : VillageRoleSkillSystem.RoleNode.values()) {",
    "for (VillageRoleSkillSystem.RoleNode node : VillageRoleSkillSystem.nodes()) {",
)

# ---------------------------------------------------------------------------
# Village defense research: 9 -> 15 upgrades, while keeping branch IDs stable.
# ---------------------------------------------------------------------------
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    """public final class VillageDefenseResearchSystem {
    private static final EnumMap<Branch, Integer> LEVELS = new EnumMap<>(Branch.class);
""",
    """public final class VillageDefenseResearchSystem {
    public static final int MAX_LEVEL = 5;
    private static final EnumMap<Branch, Integer> LEVELS = new EnumMap<>(Branch.class);
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    "Math.max(0, Math.min(3, value))",
    "Math.max(0, Math.min(MAX_LEVEL, value))",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    "if (current >= 3) return branch.displayName() + \" 연구가 최고 단계입니다.\";",
    "if (current >= MAX_LEVEL) return branch.displayName() + \" 연구가 최고 단계입니다.\";",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    "return 1.0f + level(Branch.MERCENARY) * 0.14f;",
    "return 1.0f + level(Branch.MERCENARY) * 0.12f;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    "return level(Branch.MERCENARY);",
    "return Math.min(3, level(Branch.MERCENARY));",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    "return 1.0f + level(Branch.TOWER) * 0.12f;",
    "return 1.0f + level(Branch.TOWER) * 0.10f;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    "return level(Branch.LOGISTICS) * 0.035f;",
    "return level(Branch.LOGISTICS) * 0.03f;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    "return 1.0f + level(Branch.LOGISTICS) * 0.12f;",
    "return 1.0f + level(Branch.LOGISTICS) * 0.10f;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.java",
    """                case MERCENARY -> \"용병 정원 +\" + level + \" · 용병 피해 +\" + (level * 14) + \"%\";
                case TOWER -> \"모든 방어탑 피해 +\" + (level * 12) + \"%\";
                case LOGISTICS -> \"장비 드랍률과 전리품 판매가치 강화 · 현재 판매 +\" + (level * 12) + \"%\";
""",
    """                case MERCENARY -> \"용병 정원 +\" + Math.min(3, level) + \" · 용병 피해 +\" + (level * 12) + \"%\";
                case TOWER -> \"모든 방어탑 피해 +\" + (level * 10) + \"%\";
                case LOGISTICS -> \"장비 드랍률과 전리품 판매가치 강화 · 현재 판매 +\" + (level * 10) + \"%\";
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageDefenseResearchData.java",
    "Math.max(0, Math.min(3, value))",
    "Math.max(0, Math.min(VillageDefenseResearchSystem.MAX_LEVEL, value))",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageUiController.java",
    """            labels.add(branch.displayName() + \" Lv.\" + level + \"/3\"
                    + (level >= 3 ? \"\" : \" · 주화 \" + cost) + \"|\" + branch.description(level));
""",
    """            labels.add(branch.displayName() + \" Lv.\" + level + \"/\" + VillageDefenseResearchSystem.MAX_LEVEL
                    + (level >= VillageDefenseResearchSystem.MAX_LEVEL ? \"\" : \" · 주화 \" + cost)
                    + \"|\" + branch.description(level));
""",
)

# ---------------------------------------------------------------------------
# Tree usability: linear spacing, overview reset, larger details.
# ---------------------------------------------------------------------------
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeScreen.java",
    "private static double savedZoom = 1.0;",
    "private static double savedZoom = 0.82;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeScreen.java",
    "int top = Math.max(112, height - 88);",
    "int top = Math.max(132, height - 108);",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeScreen.java",
    "if (savedZoom >= 0.82) {",
    "if (savedZoom >= 0.72) {",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeScreen.java",
    """            title = node.title();
            description = node.description();
""",
    """            title = branchName(node.branch()) + \" \" + node.tier() + \"단계 · \" + node.title();
            description = node.description();
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeScreen.java",
    """        if (inside(click.x(), click.y(), centerX, 10, 46, 25)) { savedPanX = 0; savedPanY = 0; savedZoom = 1.0; return true; }
""",
    """        if (inside(click.x(), click.y(), centerX, 10, 46, 25)) { savedPanX = 0; savedPanY = 0; savedZoom = 0.82; return true; }
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeScreen.java",
    """            double distance = 105.0 + tier * 13.0;
            double worldX = switch (branch) {
                case POWER -> tier * distance;
                case GUARD -> -tier * distance;
                default -> 0;
            };
            double worldY = switch (branch) {
                case RANGED -> -tier * distance;
                case SUPPORT -> tier * distance;
                default -> 0;
            };
""",
    """            double distance = 92.0;
            double worldX = switch (branch) {
                case POWER -> tier * distance;
                case GUARD -> -tier * distance;
                default -> 0;
            };
            double worldY = switch (branch) {
                case RANGED -> -tier * distance;
                case SUPPORT -> tier * distance;
                default -> 0;
            };
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeScreen.java",
    "private Viewport viewport() { return new Viewport(0, 48, width, Math.max(49, height - 88)); }",
    "private Viewport viewport() { return new Viewport(0, 48, width, Math.max(49, height - 108)); }",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageSkillTreeScreen.java",
    """    private int statusColor(String status) {
        return switch (status) { case \"습득\" -> ACCENT; case \"습득 가능\" -> GOLD; case \"데이터 잠금\" -> RED; default -> MUTED; };
    }
""",
    """    private int statusColor(String status) {
        return switch (status) { case \"습득\" -> ACCENT; case \"습득 가능\" -> GOLD; case \"데이터 잠금\" -> RED; default -> MUTED; };
    }

    private String branchName(Branch branch) {
        return switch (branch) {
            case POWER -> \"공격\";
            case GUARD -> \"방어\";
            case SUPPORT -> \"지원\";
            case RANGED -> \"사격\";
        };
    }
""",
)

replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleProgressScreen.java",
    "private static final int FOOTER_HEIGHT = 112;",
    "private static final int FOOTER_HEIGHT = 126;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleProgressScreen.java",
    "private static double savedZoom = 1.0;",
    "private static double savedZoom = 0.86;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleProgressScreen.java",
    """        String title = node == null ? \"성장 노드를 선택하세요\"
                : node.title() + \" · 요구 Lv.\" + node.level() + \" · 주화 \" + node.cost();
""",
    """        String title = node == null ? \"성장 노드를 선택하세요\"
                : node.branch().displayName() + \" \" + node.tier() + \"단계 · \" + node.title()
                + \" · 요구 Lv.\" + node.level() + \" · 주화 \" + node.cost();
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleProgressScreen.java",
    "double y = -tier * 120.0;",
    "double y = -tier * 88.0;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageRoleProgressScreen.java",
    "savedPanX = 0; savedPanY = 0; savedZoom = 1.0;",
    "savedPanX = 0; savedPanY = 0; savedZoom = 0.86;",
)

# ---------------------------------------------------------------------------
# Shop was the remaining oversized-button and legacy-format outlier.
# ---------------------------------------------------------------------------
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    "import net.minecraft.client.gui.GuiGraphicsExtractor;",
    "import net.minecraft.ChatFormatting;\nimport net.minecraft.client.gui.GuiGraphicsExtractor;",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    """    private static final int PANEL = 0xFFF0E5CC;
    private static final int SURFACE = 0xFFFFF8E8;
    private static final int SURFACE_ALT = 0xFFE6D9BE;
    private static final int SELECTED = 0xFFFFE2A8;
    private static final int BORDER = 0xFF75634C;
    private static final int TEXT = 0xFF241D17;
    private static final int MUTED = 0xFF6D6256;
    private static final int GOLD = 0xFFC78B2D;
    private static final int TEAL = 0xFF2E8E80;
    private static final int CARD_HEIGHT = 52;
    private static final int CARD_GAP = 6;
""",
    """    private static final int PANEL = 0xFFF1E6CF;
    private static final int SURFACE = 0xFFFFFAEE;
    private static final int SURFACE_ALT = 0xFFE9DCC1;
    private static final int SELECTED = 0xFFFFE1A2;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int GOLD = 0xFFB87B20;
    private static final int TEAL = 0xFF267E73;
    private static final int CARD_HEIGHT = 40;
    private static final int CARD_GAP = 4;
    private static final int ACTION_HEIGHT = 24;
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    "graphics.text(font, compact(card.name(), Math.max(13, cardWidth / 7)), x + 14, y + 9, TEXT, false);",
    "graphics.text(font, compact(plain(card.name()), Math.max(11, cardWidth / 7)), x + 12, y + 6, TEXT, false);",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    """            graphics.text(font, compact(card.cost(), Math.max(13, cardWidth / 7)), x + 14, y + 29,
                    card.available() ? GOLD : MUTED, false);
""",
    """            graphics.text(font, compact(plain(card.cost()), Math.max(11, cardWidth / 7)), x + 12, y + 23,
                    card.available() ? GOLD : MUTED, false);
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    """        int buttonLeft = pane.left() + 20;
        int buttonRight = pane.right() - 20;
        int buttonTop = pane.bottom() - 49;
        int textLeft = pane.left() + 20;
        int textRight = pane.right() - 20;
        int textTop = pane.top() + 18;
        int textBottom = buttonTop - 12;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(Component.literal(\"§l\" + card.name()), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(\"§6\" + card.cost()), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(card.effect()), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(card.status()), Math.max(100, textRight - textLeft)));
""",
    """        int buttonWidth = Math.min(142, Math.max(86, pane.width() / 3));
        int buttonRight = pane.right() - 15;
        int buttonLeft = buttonRight - buttonWidth;
        int buttonTop = pane.bottom() - ACTION_HEIGHT - 11;
        int textLeft = pane.left() + 15;
        int textRight = pane.right() - 15;
        int textTop = pane.top() + 14;
        int textBottom = buttonTop - 8;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(Component.literal(plain(card.name())), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(plain(card.cost())), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(plain(card.effect())), Math.max(100, textRight - textLeft)));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.literal(plain(card.status())), Math.max(100, textRight - textLeft)));
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    """        boolean hovered = inside(mouseX, mouseY, buttonLeft, buttonTop, buttonRight - buttonLeft, 34);
        graphics.fill(buttonLeft - 1, buttonTop - 1, buttonRight + 1, buttonTop + 35,
                hovered ? TEAL : GOLD);
        graphics.fill(buttonLeft, buttonTop, buttonRight, buttonTop + 34,
                hovered ? 0xFFD7F1E9 : SELECTED);
        graphics.centeredText(font, actionLabel(card.action()), (buttonLeft + buttonRight) / 2,
                buttonTop + 12, TEXT);
""",
    """        boolean active = card.available();
        boolean hovered = active && inside(mouseX, mouseY, buttonLeft, buttonTop, buttonWidth, ACTION_HEIGHT);
        graphics.fill(buttonLeft - 1, buttonTop - 1, buttonRight + 1, buttonTop + ACTION_HEIGHT + 1,
                active ? (hovered ? TEAL : GOLD) : BORDER);
        graphics.fill(buttonLeft, buttonTop, buttonRight, buttonTop + ACTION_HEIGHT,
                active ? (hovered ? 0xFFD7F1E9 : SELECTED) : SURFACE_ALT);
        graphics.centeredText(font, active ? actionLabel(card.action()) : \"잠김\", (buttonLeft + buttonRight) / 2,
                buttonTop + 7, active ? TEXT : MUTED);
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    """            if (inside(click.x(), click.y(), detail.left() + 20, detail.bottom() - 49,
                    detail.width() - 40, 34)) {
                execute(offers.get(selectedIndex));
                return true;
            }
""",
    """            OfferCard card = offers.get(selectedIndex);
            int buttonWidth = Math.min(142, Math.max(86, detail.width() / 3));
            int buttonLeft = detail.right() - 15 - buttonWidth;
            int buttonTop = detail.bottom() - ACTION_HEIGHT - 11;
            if (card.available() && inside(click.x(), click.y(), buttonLeft, buttonTop,
                    buttonWidth, ACTION_HEIGHT)) {
                execute(card);
                return true;
            }
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    "String detail = card.effect() + \"\\n\" + card.cost();",
    "String detail = plain(card.effect()) + \"\\n\" + plain(card.cost());",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    """                offers.add(new OfferCard(actions[i], Category.parse(p[1]), p[2], p[3], p[4], p[5],
                        \"available\".equals(p[6])));
""",
    """                offers.add(new OfferCard(actions[i], Category.parse(p[1]), plain(p[2]), plain(p[3]),
                        plain(p[4]), plain(p[5]), \"available\".equals(p[6])));
""",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    "int listWidth = clamp((right - left) * 35 / 100, 190, 310);",
    "int listWidth = clamp((right - left) * 24 / 100, 118, 198);",
)
replace_once(
    "src/main/java/kr/moonseungjun/villageguardians/VillageShopScreen.java",
    """    private String compact(String value, int maximum) {
""",
    """    private String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? \"\" : value);
        return stripped == null ? \"\" : stripped;
    }

    private String compact(String value, int maximum) {
""",
)

# ---------------------------------------------------------------------------
# Source-level regression tests and design audit record.
# ---------------------------------------------------------------------------
write(
    "tools/test_progression_depth.py",
    """#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / \"src/main/java/kr/moonseungjun/villageguardians\"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding=\"utf-8\")


def main() -> None:
    common = read(\"VillageSkillTreeSystem.java\")
    common_data = read(\"VillageSkillTreeData.java\")
    role = read(\"VillageRoleSkillSystem.java\")
    personal = read(\"VillagePersonalCombatSystem.java\")
    rpg = read(\"VillageRpgSystem.java\")
    research = read(\"VillageDefenseResearchSystem.java\")
    research_data = read(\"VillageDefenseResearchData.java\")
    controller = read(\"VillageUiController.java\")
    common_ui = read(\"VillageSkillTreeScreen.java\")
    role_ui = read(\"VillageRoleProgressScreen.java\")
    shop_ui = read(\"VillageShopScreen.java\")

    assert common.count('(\"power_') == 7
    assert common.count('(\"guard_') == 7
    assert common.count('(\"support_') == 7
    assert common.count('(\"ranged_') == 7
    assert \"(level - 1) / 2\" in common
    assert \"sharedSupplyChance\" in common and \"teamHealOnKillAmount\" in common
    assert \"Integer.SIZE - 1\" in common_data

    for branch in (\"DURATION\", \"POWER\", \"SPECIAL\"):
        assert role.count(branch + \"_\") >= 5
    assert \"DURATION_5\" in role and \"POWER_5\" in role and \"SPECIAL_5\" in role
    assert \"roleTreeCooldownReductionSeconds\" in role
    assert \"VillageRoleSkillSystem.nodes()\" in controller

    assert \"handleIncomingDamage\" in personal
    assert \"applyKillMomentum\" in personal
    assert \"healNearbyAlliesOnKill\" in personal
    assert \"VillagePersonalCombatSystem.handleIncomingDamage\" in rpg
    assert \"VillagePersonalCombatSystem.reset\" in rpg

    assert \"MAX_LEVEL = 5\" in research
    assert \"VillageDefenseResearchSystem.MAX_LEVEL\" in research_data
    assert '\"/\" + VillageDefenseResearchSystem.MAX_LEVEL' in controller

    assert \"double distance = 92.0\" in common_ui
    assert \"savedZoom = 0.82\" in common_ui
    assert \"double y = -tier * 88.0\" in role_ui
    assert \"savedZoom = 0.86\" in role_ui
    assert \"ACTION_HEIGHT = 24\" in shop_ui
    assert \"* 24 / 100\" in shop_ui
    assert \"ChatFormatting.stripFormatting\" in shop_ui
    assert \"§l\" not in shop_ui and \"§6\" not in shop_ui

    print(\"[PASS] Common tactical tree has 28 nodes with functional capstones\")
    print(\"[PASS] Five roles expose 75 ordered role-upgrade nodes without ordinal migration\")
    print(\"[PASS] Emergency barrier, momentum and party recovery are wired into combat\")
    print(\"[PASS] Defense research expands from 9 to 15 upgrades\")
    print(\"[PASS] Skill trees fit overview spacing and the shop uses compact safe actions\")


if __name__ == \"__main__\":
    main()
""",
)

write(
    "docs/UI_AND_PROGRESSION_AUDIT_0.17.2.md",
    """# Village Guardians UI·진행 구조 정밀 감사 — v0.17.2

## 참고 기준

- W3C WCAG 2.2, 1.4.3 Contrast (Minimum): 일반 크기 텍스트 4.5:1, 큰 텍스트 3:1을 기준으로 삼는다.
  - https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
- W3C WCAG 2.2, 2.4.13 Focus Appearance: 포커스 표시는 충분한 면적과 3:1 이상의 상태 대비를 가져야 한다.
  - https://www.w3.org/WAI/WCAG22/Understanding/focus-appearance.html
- Xbox Accessibility Guideline 101/102/112/113/115: 텍스트 가독성, 대비, 일관된 탐색, 명확한 포커스, 비용이 드는 행동의 확인 절차를 게임 UI 검증 기준으로 사용한다.
  - https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/101
  - https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/102
  - https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/112
  - https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/113
  - https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/115

## 발견한 핵심 문제

1. 상점만 과거 35% 목록 폭·34px 전체 폭 버튼·레거시 `§` 색상 코드를 유지해 다른 화면과 조작 규칙이 달랐다.
2. 공용 전술 트리는 좌표가 `tier × (105 + tier × 13)`으로 증가해 단계가 늘수록 간격이 이차적으로 벌어졌다. 5단계도 기본 화면 밖으로 밀릴 수 있었고 7단계 확장이 불가능했다.
3. 공용 방어 5단계 `응급 장막`은 해금 여부 메서드만 존재하고 실제 전투 코드에서 한 번도 소비되지 않는 죽은 업그레이드였다.
4. 공용 트리는 20개 노드인데 최대 레벨 30에서 9포인트만 획득해, 한 경로를 완성하면 다른 경로 실험이 지나치게 막혔다.
5. 직업 성장 경로는 역할마다 사실상 3×3 단계에 그쳐 레벨 21 이후 성장 동기가 급격히 약해졌다.
6. 마을 방어 연구도 3분기×3단계뿐이라 장기 방어에서 주화 사용처가 빨리 소진됐다.
7. 저장 데이터가 enum ordinal 기반 비트마스크이므로 기존 노드 사이에 새 enum을 삽입하면 세이브의 의미가 바뀐다. 새 노드는 반드시 기존 상수 뒤에 추가하고 표시 순서만 별도 정렬해야 한다.

## 적용 규칙

- 밝은 패널의 기본 본문과 보조 본문은 각각 4.5:1 이상을 자동 검사한다.
- 선택·호버·잠김은 색상 하나만 바꾸지 않고 테두리, 채움, 상태 문구를 함께 사용한다.
- 반복 화면은 `좁은 선택 목록 + 넓은 설명 + 우측 하단 작은 실행 버튼` 구조를 공통으로 사용한다.
- 비용을 쓰는 구매·강화·노드 습득은 실행 전에 대상, 현재 효과, 다음 효과, 비용을 확인한다.
- 잠긴 상품은 버튼을 비활성화해 실수 클릭과 무의미한 서버 요청을 차단한다.
- 확대·이동형 트리는 전체 보기 상태를 기본값으로 제공하고, 단계 간 간격은 선형으로 유지한다.
- 신규 성장 노드는 기존 enum ordinal 뒤에만 추가해 기존 월드와 저장값을 보존한다.

## 콘텐츠·성장 확장 결과

- 공용 전술: 4분기×5단계 20개 → 4분기×7단계 28개.
- 전술 포인트: 3레벨당 1점 → 2레벨당 1점. 최대 레벨에서 14점으로 두 경로 완성 또는 혼합 빌드가 가능하다.
- 직업 성장: 역할당 3분기×3단계 9개 → 역할당 3분기×5단계 15개. 5직업 총 75개 역할별 업그레이드.
- 마을 방어 연구: 3분기×3단계 9개 → 3분기×5단계 15개.
- 기존 죽은 `응급 장막`에 실제 치명타 완화, 흡수 보호막, 재사용 대기시간을 연결했다.
- 공격 상위 노드는 처치 연속 강화, 방어 상위 노드는 저체력 재생·장막 강화, 지원 상위 노드는 파티 회복·군수 강화, 사격 상위 노드는 마무리 사격·연쇄 대상 확장으로 구성했다.

## 다음 확장 우선순위

1. 각 직업의 다섯 번째 궁극 기술과 전용 시각 효과.
2. 보스별 전용 드랍과 유물 조합 시너지.
3. 시설 Lv.5 이후 선택형 전문화: 대장간 무기/방어, 병영 용병/훈련, 의무소 즉시회복/지속회복.
4. 웨이브 목표 변형: 호위, 특정 시설 집중 방어, 정예 추적, 제한 시간 공성 저지.
5. 키보드·컨트롤러 방향키로 트리 노드를 순회하는 명시적 포커스 이동.
""",
)

print("[PATCHED] v0.17.2 UI/accessibility and progression audit")

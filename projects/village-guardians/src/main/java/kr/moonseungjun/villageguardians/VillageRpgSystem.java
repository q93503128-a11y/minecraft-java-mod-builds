package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VillageRpgSystem {
    private static final Map<UUID, Long> NEXT_SKILL_USE_MILLIS = new HashMap<>();

    private VillageRpgSystem() {
    }

    public static void resetTransientState() {
        NEXT_SKILL_USE_MILLIS.clear();
    }

    public static void refreshPassives(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshPlayerPassive(player);
        }
    }

    public static void refreshPlayerPassive(ServerPlayer player) {
        int level = VillageCouncilState.levelOf(player.getUUID());
        int bonusHealth = bonusHealthPoints(level);
        if (bonusHealth > 0) {
            int amplifier = Math.max(0, bonusHealth / 4 - 1);
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 80, amplifier));
        }
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof ServerPlayer)) {
            int level = VillageCouncilState.levelOf(attacker.getUUID());
            float multiplier = outgoingDamageMultiplier(level);
            if (VillageCouncilState.isInsideVillage(attacker)) {
                multiplier *= VillageProgressionSystem.armoryDamageMultiplier();
            }
            event.setAmount(event.getAmount() * multiplier);
        }

        if (event.getEntity() instanceof ServerPlayer defender) {
            int level = VillageCouncilState.levelOf(defender.getUUID());
            float multiplier = incomingDamageMultiplier(level);
            if (VillageCouncilState.isInsideVillage(defender)) {
                multiplier *= VillageProgressionSystem.wallDamageMultiplier();
            }
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    public static void handleDeath(LivingDeathEvent event) {
        VillageRaidSystem.onLivingDeath(event);
        if (!(event.getEntity() instanceof Monster defeated)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        int baseReward = Math.min(300, 20 + Math.round(defeated.getMaxHealth() * 1.5f));
        boolean villageDefense = VillageCouncilState.isInsideVillage(killer);
        int reward = villageDefense
                ? Math.round(baseReward * VillageCouncilState.VILLAGE_DEFENSE_XP_MULTIPLIER)
                : baseReward;
        VillageCouncilState.ExperienceResult result = VillageCouncilState.grantExperience(killer, reward);
        killer.sendSystemMessage(Component.literal("§d+" + result.awardedExperience() + " RPG XP"
                + (villageDefense ? " §6(마을 방어 보너스)" : "")));
        if (result.levelsGained() > 0) {
            refreshPlayerPassive(killer);
            killer.heal(killer.getMaxHealth());
        }
    }

    public static String useRoleSkill(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            return "먼저 /vg role <role>로 역할을 선택해야 합니다.";
        }

        long now = System.currentTimeMillis();
        long readyAt = NEXT_SKILL_USE_MILLIS.getOrDefault(player.getUUID(), 0L);
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now + 999L) / 1000L);
            return "스킬 재사용까지 " + seconds + "초 남았습니다.";
        }

        int level = VillageCouncilState.levelOf(player.getUUID());
        int tier = Math.min(2, (level - 1) / 10);
        int duration = 200 + level * 10 + VillageProgressionSystem.skillDurationBonusTicks();
        List<ServerPlayer> allies = nearbyAllies(player, 12.0);
        String skillName;

        switch (role) {
            case GUARD_CAPTAIN -> {
                skillName = "전장의 함성";
                for (ServerPlayer ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, tier));
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, Math.min(1, tier)));
                }
            }
            case BUILDER -> {
                skillName = "긴급 방벽";
                for (ServerPlayer ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration + 100, Math.min(2, tier + 1)));
                    ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration + 100, tier + 1));
                }
            }
            case QUARTERMASTER -> {
                skillName = "전투 보급";
                for (ServerPlayer ally : allies) {
                    ally.heal(4.0f + level * 0.6f);
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, Math.min(2, tier + 1)));
                    ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, tier));
                }
            }
            case SCOUT -> {
                skillName = "질풍 정찰";
                for (ServerPlayer ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, duration + 200, tier + 1));
                    ally.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration + 200, tier));
                    ally.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration + 400, 0));
                }
            }
            case STEWARD -> {
                skillName = "풍요의 기운";
                for (ServerPlayer ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.HASTE, duration + 200, tier + 1));
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, tier));
                }
            }
            case MEDIC -> {
                skillName = "구호의 빛";
                for (ServerPlayer ally : allies) {
                    ally.heal(8.0f + level);
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration + 100, Math.min(3, tier + 1)));
                    ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, tier + 1));
                }
            }
            default -> throw new IllegalStateException("Unhandled role: " + role);
        }

        int cooldownSeconds = Math.max(
                12,
                36 - level / 2 - VillageProgressionSystem.skillCooldownReductionSeconds());
        NEXT_SKILL_USE_MILLIS.put(player.getUUID(), now + cooldownSeconds * 1000L);
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§b[역할 스킬] §f" + player.getGameProfile().name() + " 님이 " + skillName
                            + "을 사용했습니다. 주변 아군 " + allies.size() + "명 적용."),
                    false);
        }
        return skillName + " 사용 완료. 재사용 대기시간 " + cooldownSeconds + "초";
    }

    public static float outgoingDamageMultiplier(int level) {
        int normalized = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        int milestones = (normalized - 1) / 5;
        return 1.0f + (normalized - 1) * 0.12f + milestones * 0.35f;
    }

    public static float incomingDamageMultiplier(int level) {
        int normalized = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        int milestones = (normalized - 1) / 5;
        return Math.max(0.28f, 1.0f - (normalized - 1) * 0.018f - milestones * 0.06f);
    }

    public static int bonusHealthPoints(int level) {
        int normalized = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return ((normalized - 1) / 3) * 4;
    }

    private static List<ServerPlayer> nearbyAllies(ServerPlayer player, double radius) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return List.of(player);
        }
        double radiusSquared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(other -> other.level() == player.level())
                .filter(other -> other.distanceToSqr(player) <= radiusSquared)
                .toList();
    }
}

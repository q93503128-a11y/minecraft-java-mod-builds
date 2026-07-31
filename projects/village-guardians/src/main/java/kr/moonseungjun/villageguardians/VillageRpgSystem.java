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
    private static final Map<UUID, Long> NEXT_SKILL_USE = new HashMap<>();
    private VillageRpgSystem() {}

    public static void resetTransientState() { NEXT_SKILL_USE.clear(); }

    public static void refreshPassives(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(VillageRpgSystem::refreshPlayerPassive);
    }

    public static void refreshPlayerPassive(ServerPlayer player) {
        int bonus = bonusHealthPoints(VillageCouncilState.levelOf(player.getUUID()));
        if (bonus > 0) player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 80, Math.max(0, bonus / 4 - 1)));
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof ServerPlayer)) {
            float value = outgoingDamageMultiplier(VillageCouncilState.levelOf(attacker.getUUID()));
            value *= VillageProgressionSystem.smithyDamageMultiplier(attacker);
            value *= VillageProgressionSystem.learnedSkillDamageMultiplier(attacker);
            event.setAmount(event.getAmount() * value);
        }
        if (event.getEntity() instanceof ServerPlayer defender) {
            float value = incomingDamageMultiplier(VillageCouncilState.levelOf(defender.getUUID()));
            if (VillageCouncilState.isInsideVillage(defender)) value *= VillageProgressionSystem.wallDamageMultiplier();
            event.setAmount(event.getAmount() * value);
        }
    }

    public static void handleDeath(LivingDeathEvent event) {
        VillageRaidSystem.onLivingDeath(event);
        if (!(event.getEntity() instanceof Monster defeated)
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        int base = Math.min(300, 20 + Math.round(defeated.getMaxHealth() * 1.5f));
        int reward = VillageCouncilState.isInsideVillage(killer)
                ? Math.round(base * VillageCouncilState.VILLAGE_DEFENSE_XP_MULTIPLIER) : base;
        VillageCouncilState.ExperienceResult result = VillageCouncilState.grantExperience(killer, reward);
        VillageProgressionSystem.addCoins(killer, Math.max(2, Math.round(defeated.getMaxHealth() / 6.0f)), "적 처치");
        killer.sendSystemMessage(Component.literal("§d+" + result.awardedExperience() + " RPG XP"));
        if (result.levelsGained() > 0) {
            refreshPlayerPassive(killer);
            killer.heal(killer.getMaxHealth());
        }
    }

    public static String useRoleSkill(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) return "먼저 역할을 선택해야 합니다.";
        int learned = VillageProgressionSystem.skillRank(player);
        if (learned <= 0) return "스킬 습득소에서 첫 전투 기술을 배워야 합니다.";
        long now = System.currentTimeMillis();
        long ready = NEXT_SKILL_USE.getOrDefault(player.getUUID(), 0L);
        if (ready > now) return "스킬 재사용까지 " + Math.max(1, (ready - now + 999) / 1000) + "초 남았습니다.";

        int level = VillageCouncilState.levelOf(player.getUUID());
        int tier = Math.min(3, (level - 1) / 10 + (learned - 1) / 2);
        int duration = 200 + level * 10 + VillageProgressionSystem.skillDurationBonusTicks(player);
        List<ServerPlayer> allies = allies(player, 12 + learned * 1.5);
        String name = role.displayName() + " 전술";
        for (ServerPlayer ally : allies) apply(role, ally, duration, tier, level, learned);
        int cooldown = Math.max(12, 36 - level / 2 - VillageProgressionSystem.skillCooldownReductionSeconds(player));
        NEXT_SKILL_USE.put(player.getUUID(), now + cooldown * 1000L);
        MinecraftServer server = player.level().getServer();
        if (server != null) server.getPlayerList().broadcastSystemMessage(
                Component.literal("§b[역할 스킬] §f" + player.getGameProfile().name() + " - " + name), false);
        return name + " 사용 완료. 재사용 " + cooldown + "초";
    }

    private static void apply(VillageRole role, ServerPlayer ally, int duration, int tier, int level, int learned) {
        switch (role) {
            case GUARD_CAPTAIN -> {
                ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, tier));
                ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, Math.min(2, tier)));
            }
            case BUILDER -> {
                ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration + 100, Math.min(3, tier + 1)));
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration + 100, Math.min(4, tier + 1)));
            }
            case QUARTERMASTER -> {
                ally.heal(4 + level * 0.6f + learned * 2);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, Math.min(3, tier + 1)));
            }
            case SCOUT -> {
                ally.addEffect(new MobEffectInstance(MobEffects.SPEED, duration + 200, Math.min(4, tier + 1)));
                ally.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration + 400, 0));
            }
            case STEWARD -> {
                ally.addEffect(new MobEffectInstance(MobEffects.HASTE, duration + 200, Math.min(4, tier + 1)));
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, Math.min(3, tier)));
            }
            case MEDIC -> {
                ally.heal(8 + level + learned * 2);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration + 100, Math.min(4, tier + 1)));
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.min(4, tier + 1)));
            }
        }
    }

    public static float outgoingDamageMultiplier(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return 1 + (value - 1) * 0.12f + ((value - 1) / 5) * 0.35f;
    }

    public static float incomingDamageMultiplier(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return Math.max(0.28f, 1 - (value - 1) * 0.018f - ((value - 1) / 5) * 0.06f);
    }

    public static int bonusHealthPoints(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return ((value - 1) / 3) * 4;
    }

    private static List<ServerPlayer> allies(ServerPlayer player, double radius) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return List.of(player);
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(other -> other.level() == player.level() && other.distanceToSqr(player) <= squared).toList();
    }
}

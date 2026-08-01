package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VillageRpgSystem {
    private static final Map<UUID, Long> NEXT_SKILL_USE = new HashMap<>();
    private static final Map<UUID, Long> RANGED_FOCUS_UNTIL = new HashMap<>();

    private VillageRpgSystem() {
    }

    public static void resetTransientState() {
        NEXT_SKILL_USE.clear();
        RANGED_FOCUS_UNTIL.clear();
        VillageCombatTechniqueSystem.reset();
    }

    public static void refreshPassives(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(VillageRpgSystem::refreshPlayerPassive);
    }

    public static void refreshPlayerPassive(ServerPlayer player) {
        int bonus = bonusHealthPoints(VillageCouncilState.levelOf(player.getUUID()));
        if (bonus > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 80, Math.max(0, bonus / 4 - 1)));
        }
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof ServerPlayer)) {
            float value = outgoingDamageMultiplier(VillageCouncilState.levelOf(attacker.getUUID()));
            value *= roleOutgoingMultiplier(attacker, event);
            value *= VillageProgressionSystem.smithyDamageMultiplier(attacker);
            value *= VillageProgressionSystem.learnedSkillDamageMultiplier(attacker);
            value *= VillageSkillTreeSystem.outgoingDamageMultiplier(attacker);
            event.setAmount(event.getAmount() * value);
        }
        if (event.getEntity() instanceof ServerPlayer defender) {
            float value = incomingDamageMultiplier(VillageCouncilState.levelOf(defender.getUUID()));
            value *= roleIncomingMultiplier(defender);
            value *= VillageSkillTreeSystem.incomingDamageMultiplier(defender);
            if (VillageCouncilState.isInsideVillage(defender)) {
                value *= VillageProgressionSystem.wallDamageMultiplier();
            }
            event.setAmount(event.getAmount() * value);
        }
        VillageCombatTechniqueSystem.handleIncomingDamage(event);
    }

    public static void handleDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster defeated)
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        int base = Math.min(90, 7 + Math.round(defeated.getMaxHealth() * 0.48f));
        int reward = VillageCouncilState.isInsideVillage(killer)
                ? Math.round(base * 1.18f)
                : base;
        VillageCouncilState.ExperienceResult result = VillageCouncilState.grantExperience(killer, reward);
        int baseCoins = Math.max(1, Math.round(defeated.getMaxHealth() / 12.0f));
        int coins = Math.max(1, Math.round(baseCoins * VillageSkillTreeSystem.coinRewardMultiplier(killer)));
        VillageProgressionSystem.addCoins(killer, coins, "적 처치");
        killer.sendSystemMessage(Component.literal("§d+" + result.awardedExperience() + " XP"));
        if (result.levelsGained() > 0) {
            refreshPlayerPassive(killer);
            killer.heal(Math.min(killer.getMaxHealth(), 4.0f + result.levelsGained() * 2.0f));
        }
    }

    public static String useRoleSkill(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            return "먼저 역할을 선택해야 합니다.";
        }
        int learned = VillageProgressionSystem.skillRank(player);
        if (learned <= 0) {
            return "기술 연구소에서 첫 능력을 배워야 합니다.";
        }
        long now = System.currentTimeMillis();
        long ready = NEXT_SKILL_USE.getOrDefault(player.getUUID(), 0L);
        if (ready > now) {
            return "스킬 재사용까지 " + Math.max(1, (ready - now + 999) / 1000) + "초 남았습니다.";
        }

        int level = VillageCouncilState.levelOf(player.getUUID());
        int tier = Math.min(3, (level - 1) / 10 + (learned - 1) / 2);
        int duration = 180 + level * 8 + VillageProgressionSystem.skillDurationBonusTicks(player);
        List<ServerPlayer> allies = allies(player, 12 + learned * 1.5);
        String name = role.displayName() + " 전술";
        for (ServerPlayer ally : allies) {
            apply(role, ally, duration, tier, level, learned);
        }
        VillageCombatTechniqueSystem.castRoleTechnique(player, role, learned);
        int cooldown = Math.max(
                12,
                40 - level / 3
                        - VillageProgressionSystem.skillCooldownReductionSeconds(player)
                        - VillageSkillTreeSystem.cooldownReductionSeconds(player));
        NEXT_SKILL_USE.put(player.getUUID(), now + cooldown * 1000L);
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§b[역할 스킬] §f" + player.getGameProfile().name() + " - " + name),
                    false);
        }
        return name + " 사용 완료. 재사용 " + cooldown + "초";
    }

    private static void apply(
            VillageRole role,
            ServerPlayer ally,
            int duration,
            int tier,
            int level,
            int learned) {
        switch (role) {
            case GUARD_CAPTAIN -> {
                ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, tier));
                ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, Math.min(2, tier)));
            }
            case RANGER -> {
                ally.addEffect(new MobEffectInstance(MobEffects.SPEED, duration + 120, Math.min(3, tier + 1)));
                ally.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration + 400, 0));
                RANGED_FOCUS_UNTIL.put(ally.getUUID(), System.currentTimeMillis() + duration * 50L);
            }
            case ENGINEER -> {
                ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration + 100, Math.min(3, tier + 1)));
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration + 100, Math.min(4, tier + 1)));
            }
            case MEDIC -> {
                ally.heal(7 + level * 0.75f + learned * 2);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration + 100, Math.min(4, tier + 1)));
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.min(4, tier + 1)));
            }
        }
    }

    private static float roleOutgoingMultiplier(ServerPlayer player, LivingIncomingDamageEvent event) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            return 1.0f;
        }
        boolean projectile = event.getSource().getDirectEntity() instanceof AbstractArrow;
        return switch (role) {
            case GUARD_CAPTAIN -> !projectile && player.getMainHandItem().is(ItemTags.SWORDS) ? 1.18f : 1.04f;
            case RANGER -> projectile
                    ? (isOnWallTop(player) ? 1.60f : 1.28f)
                    * (RANGED_FOCUS_UNTIL.getOrDefault(player.getUUID(), 0L) > System.currentTimeMillis() ? 1.22f : 1.0f)
                    : 0.94f;
            case ENGINEER -> 1.0f;
            case MEDIC -> 0.96f;
        };
    }

    private static float roleIncomingMultiplier(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            return 1.0f;
        }
        return switch (role) {
            case GUARD_CAPTAIN -> 0.86f;
            case RANGER -> 1.02f;
            case ENGINEER -> VillageCouncilState.isInsideVillage(player) ? 0.90f : 0.97f;
            case MEDIC -> 0.95f;
        };
    }

    private static boolean isOnWallTop(ServerPlayer player) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || !VillageCouncilState.isInsideVillage(player)) {
            return false;
        }
        BlockPos pos = player.blockPosition();
        int dx = Math.abs(pos.getX() - center.getX());
        int dz = Math.abs(pos.getZ() - center.getZ());
        int relativeY = pos.getY() - center.getY();
        boolean nearWallLine = Math.abs(dx - VillageWorldSystem.FORTRESS_RADIUS) <= 7
                || Math.abs(dz - VillageWorldSystem.FORTRESS_RADIUS) <= 7;
        return nearWallLine && relativeY >= 7 && relativeY <= 17;
    }

    public static float outgoingDamageMultiplier(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return 1.0f + (value - 1) * 0.035f + ((value - 1) / 5) * 0.08f;
    }

    public static float incomingDamageMultiplier(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return Math.max(0.58f, 1.0f - (value - 1) * 0.009f - ((value - 1) / 5) * 0.025f);
    }

    public static int bonusHealthPoints(int level) {
        int value = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        return ((value - 1) / 5) * 4;
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
}

package kr.moonseungjun.survivalascension.combat;

import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CombatProgression {
    private static final Set<UUID> CLEAVE_GUARD = new HashSet<>();

    private CombatProgression() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player || event.getAmount() <= 0.0F) return;
        if (CLEAVE_GUARD.contains(player.getUUID())) return;

        int level = SkillProgressData.get(player).level(player, SkillType.COMBAT);
        float scaledDamage = (float) (event.getAmount() * SkillTuning.combatDamageMultiplier(level));
        event.setAmount(scaledDamage);

        if (event.getSource().getDirectEntity() != player || !(event.getEntity() instanceof Enemy)) return;
        double radius = SkillTuning.combatCleaveRadius(level);
        int targetLimit = SkillTuning.combatCleaveTargetLimit(level);
        double fraction = SkillTuning.combatCleaveFraction(level);
        if (radius <= 0.0D || targetLimit <= 0 || fraction <= 0.0D) return;
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) return;

        LivingEntity primary = event.getEntity();
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                primary.getBoundingBox().inflate(radius),
                candidate -> candidate != primary
                        && candidate != player
                        && candidate.isAlive()
                        && candidate instanceof Enemy
                        && !player.isAlliedTo(candidate));
        nearby.sort(Comparator.comparingDouble(primary::distanceToSqr));

        float cleaveDamage = Math.max(1.0F, (float) (scaledDamage * fraction));
        CLEAVE_GUARD.add(player.getUUID());
        try {
            int hit = 0;
            for (LivingEntity candidate : nearby) {
                if (hit >= targetLimit) break;
                if (candidate.hurtServer(serverLevel, event.getSource(), cleaveDamage)) hit++;
            }
        } finally {
            CLEAVE_GUARD.remove(player.getUUID());
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity victim = event.getEntity();
        if (victim == player || victim instanceof Player) return;

        int xp = xpForKill(victim);
        announceMilestones(player, SkillProgressionService.award(player, SkillType.COMBAT, xp));
    }

    private static int xpForKill(LivingEntity victim) {
        double health = Math.max(1.0D, victim.getMaxHealth());
        double weight = victim instanceof Enemy ? 1.5D : 0.35D;
        return Math.max(2, Math.min(200, (int) Math.ceil(health * weight)));
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel();
        int newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) {
            player.sendSystemMessage(Component.literal("§c[전투 해금] §f전투 숙련 피해 성장이 본격적으로 시작됩니다."));
        }
        if (oldLevel < 30 && newLevel >= 30) {
            player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접 공격 파급 I · 주변 적 2체까지 연쇄 타격"));
        }
        if (oldLevel < 60 && newLevel >= 60) {
            player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접 공격 파급 II · 반경/대상이 크게 확장됩니다."));
        }
        if (oldLevel < 90 && newLevel >= 90) {
            player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접 공격 파급 III · 주변 적 최대 8체까지 타격"));
        }
    }
}

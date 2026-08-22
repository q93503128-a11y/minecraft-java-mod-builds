package kr.moonseungjun.survivalascension.combat;

import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CombatProgression {
    private static final Set<UUID> CLEAVE_GUARD = new HashSet<>();
    private static final Set<UUID> SHOCKWAVE_GUARD = new HashSet<>();
    private static final String SHOCKWAVE_READY_KEY = "survivalascension_combat_shockwave_ready";
    private static final double SHOCKWAVE_RADIUS = 5.5D;
    private static final int SHOCKWAVE_TARGETS = 12;
    private static final double SHOCKWAVE_FRACTION = 0.45D;
    private static final int SHOCKWAVE_COOLDOWN_TICKS = 60;

    private CombatProgression() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player || event.getAmount() <= 0.0F) return;
        UUID uuid = player.getUUID();
        if (CLEAVE_GUARD.contains(uuid) || SHOCKWAVE_GUARD.contains(uuid)) return;

        ItemStack weapon = player.getMainHandItem();
        int level = SkillProgressData.get(player).level(player, SkillType.COMBAT);
        float scaledDamage = (float) (event.getAmount() * SkillTuning.combatDamageMultiplier(level) * AscensionAffixes.damageMultiplier(weapon));
        event.setAmount(scaledDamage);

        if (event.getSource().getDirectEntity() != player || !(event.getEntity() instanceof Enemy)) return;
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) return;

        LivingEntity primary = event.getEntity();
        if (tryShockwave(player, serverLevel, primary, event, scaledDamage, level)) return;

        double radius = SkillTuning.combatCleaveRadius(level);
        int baseTargets = SkillTuning.combatCleaveTargetLimit(level);
        double baseFraction = SkillTuning.combatCleaveFraction(level);
        int targetLimit = AscensionAffixes.adjustCleaveTargets(weapon, baseTargets);
        double fraction = AscensionAffixes.adjustCleaveFraction(weapon, baseFraction);
        if (radius <= 0.0D || targetLimit <= 0 || fraction <= 0.0D) return;

        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                primary.getBoundingBox().inflate(radius),
                candidate -> candidate != primary && candidate != player && candidate.isAlive() && candidate instanceof Enemy && !player.isAlliedTo(candidate));
        nearby.sort(Comparator.comparingDouble(primary::distanceToSqr));

        float cleaveDamage = Math.max(1.0F, (float) (scaledDamage * fraction));
        CLEAVE_GUARD.add(uuid);
        try {
            int hit = 0;
            for (LivingEntity candidate : nearby) {
                if (hit >= targetLimit) break;
                if (candidate.hurtServer(serverLevel, event.getSource(), cleaveDamage)) hit++;
            }
        } finally {
            CLEAVE_GUARD.remove(uuid);
        }
    }

    private static boolean tryShockwave(ServerPlayer player, ServerLevel level, LivingEntity primary,
                                        LivingIncomingDamageEvent event, float scaledDamage, int combatLevel) {
        if (combatLevel < 90 || !player.isSprinting()) return false;
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.COMBAT_ACADEMY)) return false;
        long now = level.getGameTime();
        if (now < player.getPersistentData().getLongOr(SHOCKWAVE_READY_KEY, 0L)) return false;

        player.getPersistentData().putLong(SHOCKWAVE_READY_KEY, now + SHOCKWAVE_COOLDOWN_TICKS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(SHOCKWAVE_RADIUS),
                candidate -> candidate != primary && candidate != player && candidate.isAlive() && candidate instanceof Enemy && !player.isAlliedTo(candidate));
        nearby.sort(Comparator.comparingDouble(player::distanceToSqr));

        float shockDamage = Math.max(1.0F, (float) (scaledDamage * SHOCKWAVE_FRACTION));
        UUID uuid = player.getUUID();
        SHOCKWAVE_GUARD.add(uuid);
        try {
            int hit = 0;
            for (LivingEntity candidate : nearby) {
                if (hit >= SHOCKWAVE_TARGETS) break;
                if (!candidate.hurtServer(level, event.getSource(), shockDamage)) continue;
                Vec3 push = candidate.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-5D) {
                    push = push.normalize();
                    candidate.setDeltaMovement(candidate.getDeltaMovement().add(push.x * 0.70D, 0.18D, push.z * 0.70D));
                    candidate.hurtMarked = true;
                }
                hit++;
            }
        } finally {
            SHOCKWAVE_GUARD.remove(uuid);
        }
        return true;
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity victim = event.getEntity();
        if (victim == player || victim instanceof Player) return;
        int xp = Math.max(1, (int) Math.ceil(xpForKill(victim) * AscensionAffixes.xpMultiplier(player.getMainHandItem())));
        announceMilestones(player, SkillProgressionService.award(player, SkillType.COMBAT, xp));
    }

    private static int xpForKill(LivingEntity victim) {
        double health = Math.max(1.0D, victim.getMaxHealth());
        double weight = victim instanceof Enemy ? 1.5D : 0.35D;
        return Math.max(2, Math.min(200, (int) Math.ceil(health * weight)));
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§c[전투 해금] §f전투 숙련 피해 성장이 본격적으로 시작됩니다."));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접 공격 파급 I · 주변 적 2체까지 연쇄 타격"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접 공격 파급 II · 반경/대상이 크게 확장됩니다."));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접 파급 III · 전투 훈련장 완공 시 질주 공격이 360° 충격파로 승격"));
    }
}

package kr.moonseungjun.survivalascension.combat;

import kr.moonseungjun.survivalascension.compat.ContentPackCompatibility;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.expedition.ExpeditionProgression;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CombatProgression {
    private static final Set<UUID> CLEAVE_GUARD = new HashSet<>();
    private static final Set<UUID> SHOCKWAVE_GUARD = new HashSet<>();
    private static final String SHOCKWAVE_READY_KEY = "survivalascension_combat_shockwave_ready";
    private static final String RANGED_BURST_USED_KEY = "survivalascension_ranged_burst_used";
    private static final String SHIELD_WAVE_READY_KEY = "survivalascension_shield_wave_ready";
    private static final double VANILLA_MACE_KNOCKBACK_RADIUS_SQR = 12.25D;
    private static final int MAJOR_TARGET_EXPEDITION_BONUS = 3;

    private CombatProgression() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof Projectile projectile)) return;
        if (AscensionAffixes.isRangedProjectile(projectile)) return;
        if (!(projectile.getOwner() instanceof ServerPlayer player)) return;
        ItemStack weapon = player.getMainHandItem();
        if (!AscensionAffixes.isRangedWeapon(weapon)) weapon = player.getOffhandItem();
        if (!AscensionAffixes.isRangedWeapon(weapon)) return;
        AscensionAffixes.snapshotRangedProjectile(projectile, weapon, player.isShiftKeyDown());
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F) return;

        if (event.getEntity() instanceof ServerPlayer defender) {
            boolean environmental = event.getSource().getEntity() == null;
            double armorMultiplier = AscensionAffixes.armorDamageMultiplier(defender, event.getAmount(), environmental);
            if (armorMultiplier < 1.0D) event.setAmount((float) (event.getAmount() * armorMultiplier));
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player || event.getAmount() <= 0.0F) return;
        UUID uuid = player.getUUID();
        if (CLEAVE_GUARD.contains(uuid) || SHOCKWAVE_GUARD.contains(uuid)) return;

        Entity direct = event.getSource().getDirectEntity();
        boolean rangedShot = AscensionAffixes.isRangedProjectile(direct);
        ItemStack weapon = player.getMainHandItem();
        double equipmentDamage = direct == player ? AscensionAffixes.damageMultiplier(weapon)
                : rangedShot ? AscensionAffixes.projectileDamageMultiplier(direct) : 1.0D;
        int level = SkillProgressData.get(player).level(player, SkillType.COMBAT);
        float scaledDamage = (float) (event.getAmount() * SkillTuning.combatDamageMultiplier(level) * equipmentDamage);
        event.setAmount(scaledDamage);

        if (!ContentPackCompatibility.isCombatTarget(event.getEntity())) return;
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) return;
        LivingEntity primary = event.getEntity();

        if (rangedShot) {
            tryRangedBurst(player, serverLevel, primary, event, direct, scaledDamage, level);
            return;
        }
        if (direct != player) return;

        if (event.getSource().is(DamageTypeTags.IS_MACE_SMASH) && AscensionAffixes.isMace(weapon)) {
            tryMaceImpact(player, serverLevel, primary, weapon, level);
            return;
        }

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
                candidate -> candidate != primary && candidate != player && candidate.isAlive()
                        && ContentPackCompatibility.isCombatTarget(candidate) && !player.isAlliedTo(candidate));
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

    public static void onShieldBlock(LivingShieldBlockEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player) || !event.getBlocked() || event.getBlockedDamage() <= 0.0F) return;
    ItemStack shield = player.getUseItem();
    if (!AscensionAffixes.isShield(shield)) return;
    int combatLevel = SkillProgressData.get(player).level(player, SkillType.COMBAT);
    if (combatLevel < 30 || player.isShiftKeyDown()) return;
    if (!(player.level() instanceof ServerLevel level)) return;

    long now = level.getGameTime();
    boolean fieldMastery = combatLevel >= 100 && ExpeditionProgression.hasFieldMastery(player);
    int baseCooldown = fieldMastery ? 10 : combatLevel >= 100 ? 12 : combatLevel >= 90 ? 14 : combatLevel >= 60 ? 16 : 20;
    int cooldown = Math.max(6, baseCooldown - AscensionAffixes.shieldWaveCooldownReduction(shield));
    if (now < player.getPersistentData().getLongOr(SHIELD_WAVE_READY_KEY, 0L)) return;

    double radius = fieldMastery ? 6.5D : combatLevel >= 100 ? 5.5D : combatLevel >= 90 ? 4.5D : combatLevel >= 60 ? 3.5D : 2.5D;
    int targetLimit = fieldMastery ? 10 : combatLevel >= 100 ? 8 : combatLevel >= 90 ? 6 : combatLevel >= 60 ? 4 : 2;
    double knockback = fieldMastery ? 1.00D : combatLevel >= 100 ? 0.90D : combatLevel >= 90 ? 0.75D : combatLevel >= 60 ? 0.60D : 0.45D;
    double lift = fieldMastery ? 0.16D : combatLevel >= 100 ? 0.14D : combatLevel >= 90 ? 0.12D : combatLevel >= 60 ? 0.10D : 0.08D;
    radius = Math.min(8.0D, radius + AscensionAffixes.shieldWaveRadiusBonus(shield));
    targetLimit = Math.min(14, targetLimit + AscensionAffixes.shieldWaveTargetBonus(shield));
    knockback = Math.min(1.30D, knockback + AscensionAffixes.shieldWaveKnockbackBonus(shield));
    lift = Math.min(0.28D, lift + AscensionAffixes.shieldWaveLiftBonus(shield));
    final double areaRadius = radius;

    List<LivingEntity> nearby = level.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(areaRadius),
            candidate -> candidate != player && candidate.isAlive()
                    && ContentPackCompatibility.isCombatTarget(candidate) && !player.isAlliedTo(candidate)
                    && player.distanceToSqr(candidate) <= areaRadius * areaRadius);
    nearby.sort(Comparator.comparingDouble(player::distanceToSqr));
    if (nearby.isEmpty()) return;

    player.getPersistentData().putLong(SHIELD_WAVE_READY_KEY, now + cooldown);
    int pushed = 0;
    for (LivingEntity candidate : nearby) {
        if (pushed >= targetLimit) break;
        Vec3 push = candidate.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
        if (push.lengthSqr() <= 1.0E-5D) continue;
        push = push.normalize();
        candidate.setDeltaMovement(candidate.getDeltaMovement().add(push.x * knockback, lift, push.z * knockback));
        candidate.hurtMarked = true;
        pushed++;
    }
}

    private static void tryMaceImpact(ServerPlayer player, ServerLevel level, LivingEntity primary,
                            ItemStack mace, int combatLevel) {
        if (combatLevel < 30 || player.isShiftKeyDown()) return;
        boolean fieldMastery = combatLevel >= 100 && ExpeditionProgression.hasFieldMastery(player);
        double radius = fieldMastery ? 9.0D : combatLevel >= 100 ? 7.5D : combatLevel >= 90 ? 6.5D : combatLevel >= 60 ? 5.5D : 4.5D;
        int targetLimit = fieldMastery ? 20 : combatLevel >= 100 ? 14 : combatLevel >= 90 ? 10 : combatLevel >= 60 ? 6 : 3;
        double knockback = fieldMastery ? 1.00D : combatLevel >= 100 ? 0.85D : combatLevel >= 90 ? 0.70D : combatLevel >= 60 ? 0.55D : 0.45D;
        double lift = fieldMastery ? 0.16D : combatLevel >= 100 ? 0.14D : combatLevel >= 90 ? 0.12D : combatLevel >= 60 ? 0.10D : 0.08D;
        radius = Math.min(10.5D, radius + AscensionAffixes.maceImpactRadiusBonus(mace));
        targetLimit = Math.min(26, targetLimit + AscensionAffixes.maceImpactTargetBonus(mace));
        knockback = Math.min(1.30D, knockback + AscensionAffixes.maceImpactKnockbackBonus(mace));
        lift = Math.min(0.28D, lift + AscensionAffixes.maceImpactLiftBonus(mace));
        final double outerRadius = radius;

        List<LivingEntity> nearby = level.getEntitiesOfClass(
      LivingEntity.class,
      primary.getBoundingBox().inflate(outerRadius),
      candidate -> candidate != primary && candidate != player && candidate.isAlive()
              && ContentPackCompatibility.isCombatTarget(candidate) && !player.isAlliedTo(candidate)
              && primary.distanceToSqr(candidate) > VANILLA_MACE_KNOCKBACK_RADIUS_SQR
              && primary.distanceToSqr(candidate) <= outerRadius * outerRadius);
        nearby.sort(Comparator.comparingDouble(primary::distanceToSqr));

        int pushed = 0;
        for (LivingEntity candidate : nearby) {
  if (pushed >= targetLimit) break;
  Vec3 push = candidate.position().subtract(primary.position()).multiply(1.0D, 0.0D, 1.0D);
  if (push.lengthSqr() <= 1.0E-5D) continue;
  double resistance = Math.max(0.0D, Math.min(1.0D, candidate.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
  double strength = knockback * (1.0D - resistance);
  if (strength <= 0.0D) continue;
  push = push.normalize();
  candidate.setDeltaMovement(candidate.getDeltaMovement().add(push.x * strength, lift * (1.0D - resistance), push.z * strength));
  candidate.hurtMarked = true;
  pushed++;
        }
    }

    private static void tryRangedBurst(ServerPlayer player, ServerLevel level, LivingEntity primary,
                                       LivingIncomingDamageEvent event, Entity direct, float scaledDamage, int combatLevel) {
        if (combatLevel < 30 || AscensionAffixes.isPrecisionRangedProjectile(direct)
                || direct.getPersistentData().getBooleanOr(RANGED_BURST_USED_KEY, false)) return;
        boolean fieldMastery = combatLevel >= 100 && ExpeditionProgression.hasFieldMastery(player);
        double radius = fieldMastery ? 6.0D : combatLevel >= 100 ? 5.0D : combatLevel >= 90 ? 4.25D : combatLevel >= 60 ? 3.5D : 2.5D;
        int targetLimit = fieldMastery ? 10 : combatLevel >= 100 ? 8 : combatLevel >= 90 ? 6 : combatLevel >= 60 ? 4 : 2;
        double fraction = combatLevel >= 100 ? 0.40D : combatLevel >= 90 ? 0.35D : combatLevel >= 60 ? 0.30D : 0.25D;
        radius += AscensionAffixes.projectileBurstRadiusBonus(direct);
        targetLimit += AscensionAffixes.projectileBurstTargetBonus(direct);
        fraction = Math.min(0.65D, fraction + AscensionAffixes.projectileBurstFractionBonus(direct));

        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                primary.getBoundingBox().inflate(radius),
                candidate -> candidate != primary && candidate != player && candidate.isAlive()
                        && ContentPackCompatibility.isCombatTarget(candidate) && !player.isAlliedTo(candidate));
        nearby.sort(Comparator.comparingDouble(primary::distanceToSqr));
        if (nearby.isEmpty()) return;

        // A physical projectile creates at most one real burst. Piercing may still damage later targets directly,
        // but cannot multiply the same shot into several overlapping area bursts.
        direct.getPersistentData().putBoolean(RANGED_BURST_USED_KEY, true);
        float burstDamage = Math.max(1.0F, (float) (scaledDamage * fraction));
        UUID uuid = player.getUUID();
        CLEAVE_GUARD.add(uuid);
        try {
            int hit = 0;
            for (LivingEntity candidate : nearby) {
                if (hit >= targetLimit) break;
                if (candidate.hurtServer(level, event.getSource(), burstDamage)) hit++;
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
        int cooldown = combatLevel >= 100 ? 50 : 60;
        if (now < player.getPersistentData().getLongOr(SHOCKWAVE_READY_KEY, 0L)) return false;

        boolean fieldMastery = combatLevel >= 100 && ExpeditionProgression.hasFieldMastery(player);
        double radius = fieldMastery ? 7.5D : (combatLevel >= 100 ? 6.5D : 5.5D);
        int targetLimit = fieldMastery ? 20 : (combatLevel >= 100 ? 16 : 12);
        double fraction = combatLevel >= 100 ? 0.55D : 0.45D;
        player.getPersistentData().putLong(SHOCKWAVE_READY_KEY, now + cooldown);
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate != primary && candidate != player && candidate.isAlive()
                        && ContentPackCompatibility.isCombatTarget(candidate) && !player.isAlliedTo(candidate));
        nearby.sort(Comparator.comparingDouble(player::distanceToSqr));

        float shockDamage = Math.max(1.0F, (float) (scaledDamage * fraction));
        UUID uuid = player.getUUID();
        SHOCKWAVE_GUARD.add(uuid);
        try {
            int hit = 0;
            for (LivingEntity candidate : nearby) {
                if (hit >= targetLimit) break;
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
        if (victim == player || victim instanceof Player || !ContentPackCompatibility.isCombatTarget(victim)) return;

        boolean majorTarget = ContentPackCompatibility.isMajorExpeditionTarget(victim);
        ExpeditionProgression.recordSkillAction(player, SkillType.COMBAT, 1);
        if (majorTarget) ExpeditionProgression.grantMajorTargetBonus(player, MAJOR_TARGET_EXPEDITION_BONUS);

        Entity direct = event.getSource().getDirectEntity();
        double equipmentXp = AscensionAffixes.isRangedProjectile(direct)
                ? AscensionAffixes.projectileXpMultiplier(direct)
                : AscensionAffixes.xpMultiplier(player.getMainHandItem());
        int xp = Math.max(1, (int) Math.ceil(xpForKill(victim, majorTarget)
                * equipmentXp
                * AscensionAffixes.armorXpMultiplier(player)));
        announceMilestones(player, SkillProgressionService.award(player, SkillType.COMBAT, xp));
    }

    private static int xpForKill(LivingEntity victim, boolean majorTarget) {
        double health = Math.max(1.0D, victim.getMaxHealth());
        int cap = majorTarget ? 600 : 200;
        double healthScale = majorTarget ? 2.5D : 1.5D;
        return Math.max(2, Math.min(cap, (int) Math.ceil(health * healthScale)));
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§c[전투 해금] §f전투 숙련 피해 성장이 본격적으로 시작됩니다."));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접 파급 I + 원거리 충돌 파급 I · Shift 발사는 단일 정밀 타격"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접/원거리 파급 II · 반경과 연쇄 대상이 크게 확장됩니다."));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§c[전투 해금] §f근접 파급 III · 전투 훈련장 질주 충격파 + 원거리 충돌 파급 III"));
        if (oldLevel < 100 && newLevel >= 100) {
            String shockwave = ExpeditionProgression.hasFieldMastery(player) ? "7.5블록/20체" : "6.5블록/16체";
            String ranged = ExpeditionProgression.hasFieldMastery(player) ? "6블록/10체" : "5블록/8체";
            player.sendSystemMessage(Component.literal("§c[전투 숙련 VI] §f근접 파급 10체/5블록 · 훈련장 충격파 " + shockwave + " · 원거리 파급 " + ranged));
        }
    }
}

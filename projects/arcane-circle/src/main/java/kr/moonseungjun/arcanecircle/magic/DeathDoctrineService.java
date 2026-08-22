package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Set;

/**
 * Alpha.62 death-school hierarchy.
 *
 * 6C Circle of Death is broad life erosion, never an execution spell.
 * 7C Finger of Death is a locked single-target soul rupture, never an instant-kill threshold.
 * 9C Power Word Kill is the only true execution authority; even a target above its law threshold
 * still suffers catastrophic direct damage so the ninth-circle spell never degrades into a debuff.
 */
public final class DeathDoctrineService {
    private static final Set<String> HANDLED = Set.of("circle_of_death", "finger_of_death", "power_word_kill");

    private DeathDoctrineService() {}

    public static boolean handles(String spellId) {
        return HANDLED.contains(spellId);
    }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        return execute((ServerLevel) caster.level(), caster, null, spellId, range, power, snapshot);
    }

    public static boolean executeNpc(ServerLevel level, LivingEntity caster, LivingEntity fallback,
                                     String spellId, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        return execute(level, caster, fallback, spellId, range, power, snapshot);
    }

    private static boolean execute(ServerLevel level, LivingEntity caster, LivingEntity fallback,
                                   String spellId, double range, double power,
                                   CastTargetSnapshot snapshot) {
        return switch (spellId) {
            case "circle_of_death" -> circleOfDeath(level, caster, snapshot.target(), range, power);
            case "finger_of_death" -> fingerOfDeath(level, caster,
                    lockedTarget(level, caster, fallback, snapshot), power);
            case "power_word_kill" -> powerWordKill(level, caster,
                    lockedTarget(level, caster, fallback, snapshot), power);
            default -> false;
        };
    }

    /** Sixth circle: army-clearing life erosion. It deliberately contains no execution branch. */
    private static boolean circleOfDeath(ServerLevel level, LivingEntity caster, Vec3 center,
                                         double range, double power) {
        double radius = Math.max(11.0, Math.min(18.0,
                SpellMetrics.effectRadius("circle_of_death", range, 6) * 1.10));
        AABB box = new AABB(center, center).inflate(radius, Math.max(7.0, radius * .72), radius);
        boolean hit = false;
        int affected = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value) && center.distanceToSqr(value.position()) <= radius * radius)) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            double falloff = Math.max(.58, 1.0 - distance / Math.max(1.0, radius) * .38);
            double vitalityTax = Math.min(power * .38, target.getMaxHealth() * .11);
            float damage = (float) Math.max(1.0, (power * .58 + vitalityTax) * falloff);
            if (ArcaneDamage.hurt(level, caster, target, damage)) hit = true;
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 140, 1, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 2, true, false));
            affected++;
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_SPAWN,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .78F, .72F);
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§5[죽음의 원] §f광역 생명 침식 · "
                    + affected + "체의 생명력과 전투력을 깎았습니다. §7처형 권능은 9써클에만 존재합니다."), 74);
        }
        // Ground-authority casts remain valid even if the formation is empty at impact time.
        return hit || center != null;
    }

    /** Seventh circle: one soul, one overwhelming rupture. No instant-kill gate is present. */
    private static boolean fingerOfDeath(ServerLevel level, LivingEntity caster,
                                         LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        double soulPressure = Math.min(power * .62, target.getMaxHealth() * .16);
        float damage = (float) Math.max(1.0, power * 1.92 + soulPressure);
        boolean hit = ArcaneDamage.hurt(level, caster, target, damage);
        if (target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 420, 4, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 420, 4, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, true, false));
        }
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.05F, .56F);
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§5[죽음의 손가락] §f단일 영혼 파열 · 즉사 판정 대신 7써클급 초고화력과 장기 생명 붕괴를 가합니다."), 72);
        }
        return hit;
    }

    /** Ninth circle: the sole death-law execution. Targets outside the law gate still take a 9C catastrophe hit. */
    private static boolean powerWordKill(ServerLevel level, LivingEntity caster,
                                         LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        double threshold = Math.max(180.0, power * 1.24);
        double pool = target.getHealth() + target.getAbsorptionAmount();
        boolean executed = pool <= threshold;
        if (executed) {
            float fatal = Math.max(4096.0F,
                    target.getHealth() + target.getAbsorptionAmount() + target.getMaxHealth() * 12.0F);
            ArcaneDamage.hurt(level, caster, target, fatal);
        } else {
            double lawDamage = power * 1.08 + Math.min(power * .80, target.getMaxHealth() * .20);
            ArcaneDamage.hurt(level, caster, target, (float) Math.max(1.0, lawDamage));
            if (target.isAlive()) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 360, 5, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 4, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 180, 1, true, false));
            }
        }
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_DEATH,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                executed ? 1.18F : 1.02F, executed ? .42F : .52F);
        if (caster instanceof ServerPlayer player) {
            String text = executed
                    ? "§0[죽음의 권능어] §f생명 법칙을 직접 끊었습니다."
                    : "§5[죽음의 권능어] §f처형 역치 " + whole(threshold)
                    + "을 넘긴 존재가 법칙을 버텼지만, 9써클 생명 붕괴는 그대로 관통했습니다.";
            ArcaneNoticeService.push(player, Component.literal(text), 84);
        }
        return true;
    }

    private static LivingEntity lockedTarget(ServerLevel level, LivingEntity caster,
                                             LivingEntity fallback, CastTargetSnapshot snapshot) {
        if (snapshot.targetEntityId() != null) {
            Entity raw = level.getEntity(snapshot.targetEntityId());
            if (raw instanceof LivingEntity living && enemy(caster, living)) return living;
            return null; // A vanished locked target never retargets another creature.
        }
        if (fallback != null && enemy(caster, fallback)) return fallback;
        Vec3 point = snapshot.target();
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(2.4),
                        value -> enemy(caster, value)).stream()
                .min(Comparator.comparingDouble(value -> value.getEyePosition().distanceToSqr(point)))
                .orElse(null);
    }

    private static boolean enemy(LivingEntity caster, LivingEntity target) {
        return caster != null && target != null && target != caster && target.isAlive() && !target.isRemoved()
                && caster.level() == target.level() && !caster.isAlliedTo(target);
    }

    private static String whole(double value) {
        return Long.toString(Math.round(value));
    }
}

package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lv.90+ veteran mastery uses the existing two equipped role skills as the build decision.
 * Progression is level-derived; only short combat-chain state is transient.
 */
public final class VillageRoleMasterySystem {
    public static final int FIRST_MASTERY_LEVEL = 90;
    public static final int SECOND_MASTERY_LEVEL = 150;
    public static final int THIRD_MASTERY_LEVEL = 210;
    public static final int FOURTH_MASTERY_LEVEL = 270;
    public static final int FINAL_MASTERY_LEVEL = 300;

    private static final Map<UUID, VillageRoleSkillSystem.ActiveSkill> LAST_SKILL = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_CAST_AT = new LinkedHashMap<>();
    private static final Map<UUID, RangerFocus> RANGER_FOCUS = new LinkedHashMap<>();

    private VillageRoleMasterySystem() {}

    public static void reset() {
        LAST_SKILL.clear();
        LAST_CAST_AT.clear();
        RANGER_FOCUS.clear();
    }

    public static int rank(ServerPlayer player) {
        if (player == null) return 0;
        return rankForLevel(VillageCouncilState.levelOf(player.getUUID()));
    }

    public static int rankForLevel(int level) {
        if (level >= FINAL_MASTERY_LEVEL) return 5;
        if (level >= FOURTH_MASTERY_LEVEL) return 4;
        if (level >= THIRD_MASTERY_LEVEL) return 3;
        if (level >= SECOND_MASTERY_LEVEL) return 2;
        if (level >= FIRST_MASTERY_LEVEL) return 1;
        return 0;
    }

    public static int nextMilestoneLevel(int level) {
        if (level < FIRST_MASTERY_LEVEL) return FIRST_MASTERY_LEVEL;
        if (level < SECOND_MASTERY_LEVEL) return SECOND_MASTERY_LEVEL;
        if (level < THIRD_MASTERY_LEVEL) return THIRD_MASTERY_LEVEL;
        if (level < FOURTH_MASTERY_LEVEL) return FOURTH_MASTERY_LEVEL;
        if (level < FINAL_MASTERY_LEVEL) return FINAL_MASTERY_LEVEL;
        return 0;
    }

    public static String summary(ServerPlayer player, VillageRole role) {
        int level = player == null ? 1 : VillageCouncilState.levelOf(player.getUUID());
        int rank = rankForLevel(level);
        int next = nextMilestoneLevel(level);
        if (rank <= 0) return "전투 숙련 Lv." + FIRST_MASTERY_LEVEL + " 해금";
        PairStyle style = pairStyle(player, role);
        String pair = switch (style) {
            case FOCUSED -> "집중 조합";
            case MIXED -> "혼합 조합";
            case INCOMPLETE -> "기술 2개 장착 필요";
        };
        return "전투 숙련 " + roman(rank) + " · " + pair
                + (next > 0 ? " · 다음 Lv." + next : " · 완성");
    }

    public static MasteryProc prepareCast(
            ServerPlayer player,
            VillageRole role,
            VillageRoleSkillSystem.ActiveSkill skill) {
        int rank = rank(player);
        if (player == null || role == null || skill == null || rank <= 0) return MasteryProc.none();

        UUID id = player.getUUID();
        long now = System.currentTimeMillis();
        PairStyle style = pairStyle(player, role);
        long windowMillis = (8L + rank + (style == PairStyle.MIXED ? 2L : 0L)) * 1000L;
        VillageRoleSkillSystem.ActiveSkill previous = LAST_SKILL.get(id);
        boolean alternating = previous != null
                && previous.role() == role
                && previous != skill
                && now - LAST_CAST_AT.getOrDefault(id, 0L) <= windowMillis;

        if (!alternating) {
            return new MasteryProc(false, rank, style, 1.0f, 1.0f, 0, 1.0f);
        }

        float effectScale = style == PairStyle.FOCUSED ? 1.25f : 1.0f;
        float power = 1.0f + (0.04f + rank * 0.02f) * effectScale;
        float duration = role == VillageRole.WARDEN || role == VillageRole.LUMINAR
                ? 1.0f + (0.03f + rank * 0.01f) * effectScale : 1.0f;
        int refund = rank >= 5 ? 3 : rank >= 4 ? 2 : rank >= 2 ? 1 : 0;
        if (style == PairStyle.MIXED) refund++;
        return new MasteryProc(true, rank, style, power, duration, refund, effectScale);
    }

    public static void finishCast(
            ServerLevel level,
            ServerPlayer player,
            VillageRole role,
            VillageRoleSkillSystem.ActiveSkill skill,
            MasteryProc proc,
            float resolvedPower,
            float resolvedDuration,
            int specialRank) {
        if (player == null || skill == null) return;
        UUID id = player.getUUID();
        LAST_SKILL.put(id, skill);
        LAST_CAST_AT.put(id, System.currentTimeMillis());
        if (level == null || role == null || proc == null || !proc.triggered()) return;

        int rank = proc.rank();
        switch (role) {
            case VANGUARD -> triggerVanguard(level, player, rank, proc.effectScale());
            case RANGER -> armRangerFocus(player, rank, proc.effectScale());
            case ARCANIST -> {
                if (rank >= 3) {
                    float echoPower = resolvedPower * (0.26f + rank * 0.045f)
                            * (proc.style() == PairStyle.FOCUSED ? 1.10f : 1.0f);
                    VillageRoleAbilitySystem.scheduleMasteryEcho(
                            level, player, skill, echoPower, resolvedDuration, specialRank,
                            Math.max(5, 11 - rank));
                }
            }
            case LUMINAR -> triggerLuminar(level, player, rank, proc.effectScale());
            case WARDEN -> triggerWarden(level, player, rank, proc.effectScale());
        }
    }

    public static float consumeRangerProjectileMultiplier(ServerPlayer player, Mob target) {
        if (player == null || target == null) return 1.0f;
        RangerFocus focus = RANGER_FOCUS.get(player.getUUID());
        if (focus == null) return 1.0f;
        if (System.currentTimeMillis() > focus.untilMillis()) {
            RANGER_FOCUS.remove(player.getUUID());
            return 1.0f;
        }
        RANGER_FOCUS.remove(player.getUUID());
        target.addEffect(new MobEffectInstance(
                MobEffects.GLOWING, 60 + focus.rank() * 12, 0, false, false, true));
        if (focus.rank() >= 3) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 50 + focus.rank() * 10,
                    focus.rank() >= 5 ? 1 : 0, false, false, true));
        }
        return focus.multiplier();
    }

    private static void triggerVanguard(ServerLevel level, ServerPlayer player, int rank, float effectScale) {
        int duration = Math.round((45 + rank * 11) * effectScale);
        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED, duration, rank >= 4 ? 1 : 0, false, false, true));
        player.heal((0.6f + rank * 0.35f) * effectScale);

        double radius = (4.0 + rank * 0.65) * effectScale;
        int weaknessDuration = Math.round((35 + rank * 10) * effectScale);
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class, player.getBoundingBox().inflate(radius), VillageRaidSystem::isRaidEnemy)) {
            mob.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, weaknessDuration, rank >= 5 ? 1 : 0,
                    false, false, true));
        }
    }

    private static void armRangerFocus(ServerPlayer player, int rank, float effectScale) {
        float multiplier = 1.0f + (0.07f + rank * 0.025f) * effectScale;
        long until = System.currentTimeMillis() + (6_000L + rank * 800L);
        RANGER_FOCUS.put(player.getUUID(), new RangerFocus(until, multiplier, rank));
    }

    private static void triggerLuminar(ServerLevel level, ServerPlayer player, int rank, float effectScale) {
        double radius = (8.0 + rank * 1.2) * effectScale;
        int duration = Math.round((70 + rank * 18) * effectScale);
        int amplifier = rank >= 5 ? 2 : rank >= 3 ? 1 : 0;
        float smallHeal = (0.8f + rank * 0.35f) * effectScale;
        double radiusSq = radius * radius;
        for (ServerPlayer ally : level.players()) {
            if (!ally.isAlive() || ally.distanceToSqr(player) > radiusSq) continue;
            ally.heal(smallHeal);
            ally.addEffect(new MobEffectInstance(
                    MobEffects.ABSORPTION, duration, amplifier, false, false, true));
        }
    }

    private static void triggerWarden(ServerLevel level, ServerPlayer player, int rank, float effectScale) {
        double radius = (8.5 + rank * 1.1) * effectScale;
        int tauntTicks = Math.round((75 + rank * 20) * effectScale);
        VillageRaidSystem.tauntEnemies(level, player, player.position(), radius, tauntTicks, 160);
        if (rank < 3) return;

        int duration = Math.round((45 + rank * 12) * effectScale);
        double radiusSq = radius * radius;
        for (ServerPlayer ally : level.players()) {
            if (!ally.isAlive() || ally.distanceToSqr(player) > radiusSq) continue;
            ally.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE, duration, rank >= 5 ? 1 : 0, false, false, true));
        }
    }

    private static PairStyle pairStyle(ServerPlayer player, VillageRole role) {
        if (player == null || role == null) return PairStyle.INCOMPLETE;
        VillageRoleSkillSystem.ActiveSkill first =
                VillageRoleSkillSystem.equippedSkill(player, 0).orElse(null);
        VillageRoleSkillSystem.ActiveSkill second =
                VillageRoleSkillSystem.equippedSkill(player, 1).orElse(null);
        if (first == null || second == null || first.role() != role || second.role() != role) {
            return PairStyle.INCOMPLETE;
        }
        return first.promotionTier() == second.promotionTier()
                ? PairStyle.FOCUSED : PairStyle.MIXED;
    }

    private static String roman(int rank) {
        return switch (rank) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }

    public enum PairStyle {
        FOCUSED,
        MIXED,
        INCOMPLETE
    }

    public record MasteryProc(
            boolean triggered,
            int rank,
            PairStyle style,
            float powerMultiplier,
            float durationMultiplier,
            int cooldownRefundSeconds,
            float effectScale) {
        public static MasteryProc none() {
            return new MasteryProc(false, 0, PairStyle.INCOMPLETE, 1.0f, 1.0f, 0, 1.0f);
        }
    }

    private record RangerFocus(long untilMillis, float multiplier, int rank) {}
}

package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-owned shared Ultimate Gauge authority and class event publishers.
 */
public final class ProjectUltimateChargeRuntime {
    public static final double CLERIC_DAMAGING_ACTIVE_CHARGE = 2.0;
    public static final double CLERIC_SUPPORT_STEP_CHARGE = 2.0;
    public static final double CLERIC_CLEANSE_CHARGE = 3.0;
    public static final double WARRIOR_BASIC_CYCLE_CHARGE = 2.0;
    public static final double WARRIOR_ACTIVE_HIT_CHARGE = 3.0;
    public static final double WARRIOR_PERFECT_GUARD_CHARGE = 5.0;
    public static final double WARRIOR_POISE_BREAK_CHARGE = 10.0;
    public static final double HUNTER_RANGED_QUARRY_HIT_CHARGE = 2.0;
    public static final double HUNTER_LONG_RANGE_BONUS_CHARGE = 1.0;
    public static final double HUNTER_WEAK_POINT_HIT_CHARGE = 3.0;
    public static final double HUNTER_RANGED_POISE_BREAK_CHARGE = 6.0;
    public static final double HUNTER_ACTIVE_QUARRY_HIT_CHARGE = 3.0;
    public static final double MAGE_PRIMARY_ACTIVE_HIT_CHARGE = 3.0;
    public static final double MAGE_ADDITIONAL_ACTIVE_HIT_CHARGE = 0.5;
    public static final double MAGE_WEAVE_COMPLETION_CHARGE = 6.0;
    public static final double MAGE_MEANINGFUL_CONTROL_CHARGE = 2.0;
    public static final long MAGE_CONTROL_ICD_TICKS = 80L;
    public static final double GUARDIAN_GUARDED_HIT_CHARGE = 2.0;
    public static final double GUARDIAN_PERFECT_GUARD_CHARGE = 6.0;
    public static final double GUARDIAN_BARRIER_STEP_CHARGE = 2.0;
    public static final double GUARDIAN_PROVOKED_HIT_CHARGE = 2.0;
    public static final double GUARDIAN_GUARDED_HIT_STAMINA_THRESHOLD = 12.0;
    public static final long GUARDIAN_PROVOKED_HIT_ICD_TICKS = 40L;
    public static final int MAX_GUARDIAN_BARRIER_STEPS_PER_SOURCE_RECIPIENT = 3;
    public static final double SUPPORT_STEP_MAX_HP_FRACTION = 0.05;
    public static final int MAX_SUPPORT_STEPS_PER_SOURCE_RECIPIENT = 3;

    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Long>>
            MAGE_CONTROL_READY_AT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Long>>
            GUARDIAN_PROVOKED_HIT_READY_AT = new ConcurrentHashMap<>();

    private ProjectUltimateChargeRuntime() {
    }

    public static GainApplication recordClassEvent(
            ServerPlayer player,
            double authoredCharge,
            int encounterLevel
    ) {
        Objects.requireNonNull(player, "player");
        if (!Double.isFinite(authoredCharge)
                || authoredCharge <= 0.0
                || encounterLevel < 1) {
            return GainApplication.rejected();
        }

        long nowTick = player.level().getGameTime();
        int playerLevel = PlayerProgressionService.state(player)
                .combatLevel();
        double levelMultiplier =
                ProjectProgressionRules
                        .combatRewardLevelMultiplier(
                                encounterLevel,
                                playerLevel
                        );
        double gearBonus = PlayerEquipmentService.state(player)
                .aggregateUltimateChargeGainBonus();

        var result = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .gainUltimateCharge(
                        authoredCharge,
                        gearBonus,
                        levelMultiplier,
                        nowTick
                );
        return new GainApplication(
                true,
                authoredCharge,
                encounterLevel,
                levelMultiplier,
                gearBonus,
                result.requestedCharge(),
                result.grantedCharge(),
                result.chargeAfter()
        );
    }

    public static GainApplication recordClericDamagingActive(
            ServerPlayer caster,
            LivingEntity primaryHostile
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(primaryHostile, "primaryHostile");
        if (!isCleric(caster)) {
            return GainApplication.rejected();
        }

        var profile = ExternalActorBindingRuntime
                .combatProfile(primaryHostile)
                .orElse(null);
        if (profile == null) {
            return GainApplication.rejected();
        }

        return recordClassEvent(
                caster,
                CLERIC_DAMAGING_ACTIVE_CHARGE,
                profile.contentLevel()
        );
    }

    public static GainApplication recordClericEffectiveHealing(
            ServerPlayer caster,
            ServerPlayer recipient,
            double effectiveHealing
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(recipient, "recipient");
        if (!isCleric(caster)
                || !Double.isFinite(effectiveHealing)
                || effectiveHealing <= 0.0
                || caster.level() != recipient.level()) {
            return GainApplication.rejected();
        }

        int steps = supportSteps(
                effectiveHealing,
                recipient.getMaxHealth()
        );
        if (steps <= 0) {
            return GainApplication.rejected();
        }

        OptionalInt encounterLevel =
                ProjectActiveEncounterRuntime.supportEncounterLevel(
                        caster,
                        recipient
                );
        if (encounterLevel.isEmpty()) {
            return GainApplication.rejected();
        }

        return recordClassEvent(
                caster,
                steps * CLERIC_SUPPORT_STEP_CHARGE,
                encounterLevel.getAsInt()
        );
    }

    public static GainApplication recordClericBarrierConsumption(
            ServerPlayer sourcePlayer,
            ServerPlayer recipient,
            int newlyQualifiedFivePercentSteps,
            LivingEntity hostileSource
    ) {
        Objects.requireNonNull(sourcePlayer, "sourcePlayer");
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(hostileSource, "hostileSource");
        if (!isCleric(sourcePlayer)
                || newlyQualifiedFivePercentSteps <= 0
                || sourcePlayer.level() != recipient.level()
                || hostileSource.level() != recipient.level()) {
            return GainApplication.rejected();
        }

        var profile = ExternalActorBindingRuntime
                .combatProfile(hostileSource)
                .orElse(null);
        if (profile == null) {
            return GainApplication.rejected();
        }

        int steps = Math.min(
                MAX_SUPPORT_STEPS_PER_SOURCE_RECIPIENT,
                newlyQualifiedFivePercentSteps
        );
        return recordClassEvent(
                sourcePlayer,
                steps * CLERIC_SUPPORT_STEP_CHARGE,
                profile.contentLevel()
        );
    }

    public static GainApplication recordClericCleanse(
            ServerPlayer caster,
            ServerPlayer recipient,
            int cleansedMeaningfulStatuses
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(recipient, "recipient");
        if (!isCleric(caster)
                || cleansedMeaningfulStatuses <= 0
                || caster.level() != recipient.level()) {
            return GainApplication.rejected();
        }

        OptionalInt encounterLevel =
                ProjectActiveEncounterRuntime.supportEncounterLevel(
                        caster,
                        recipient
                );
        if (encounterLevel.isEmpty()) {
            return GainApplication.rejected();
        }

        return recordClassEvent(
                caster,
                CLERIC_CLEANSE_CHARGE
                        * cleansedMeaningfulStatuses,
                encounterLevel.getAsInt()
        );
    }

    public static GainApplication recordWarriorBasicCycle(
            ServerPlayer warrior,
            LivingEntity hostile
    ) {
        return recordWarriorHostileEvent(
                warrior,
                hostile,
                WARRIOR_BASIC_CYCLE_CHARGE
        );
    }

    public static GainApplication recordWarriorActiveHit(
            ServerPlayer warrior,
            LivingEntity hostile
    ) {
        return recordWarriorHostileEvent(
                warrior,
                hostile,
                WARRIOR_ACTIVE_HIT_CHARGE
        );
    }

    public static GainApplication recordWarriorPerfectGuard(
            ServerPlayer warrior,
            LivingEntity hostile
    ) {
        return recordWarriorHostileEvent(
                warrior,
                hostile,
                WARRIOR_PERFECT_GUARD_CHARGE
        );
    }

    public static GainApplication recordWarriorPoiseBreak(
            ServerPlayer warrior,
            LivingEntity hostile
    ) {
        return recordWarriorHostileEvent(
                warrior,
                hostile,
                WARRIOR_POISE_BREAK_CHARGE
        );
    }

    public static GainApplication recordHunterRangedQuarryHit(
            ServerPlayer hunter,
            LivingEntity hostile
    ) {
        return recordHunterHostileEvent(
                hunter,
                hostile,
                HUNTER_RANGED_QUARRY_HIT_CHARGE
        );
    }

    public static GainApplication recordHunterLongRangeBonus(
            ServerPlayer hunter,
            LivingEntity hostile
    ) {
        return recordHunterHostileEvent(
                hunter,
                hostile,
                HUNTER_LONG_RANGE_BONUS_CHARGE
        );
    }

    public static GainApplication recordHunterWeakPointHit(
            ServerPlayer hunter,
            LivingEntity hostile
    ) {
        return recordHunterHostileEvent(
                hunter,
                hostile,
                HUNTER_WEAK_POINT_HIT_CHARGE
        );
    }

    public static GainApplication recordHunterRangedPoiseBreak(
            ServerPlayer hunter,
            LivingEntity hostile
    ) {
        return recordHunterHostileEvent(
                hunter,
                hostile,
                HUNTER_RANGED_POISE_BREAK_CHARGE
        );
    }

    public static GainApplication recordHunterActiveQuarryHit(
            ServerPlayer hunter,
            LivingEntity hostile
    ) {
        return recordHunterHostileEvent(
                hunter,
                hostile,
                HUNTER_ACTIVE_QUARRY_HIT_CHARGE
        );
    }

    public static GainApplication recordMagePrimaryActiveHit(
            ServerPlayer mage,
            LivingEntity hostile
    ) {
        return recordMageHostileEvent(mage, hostile, MAGE_PRIMARY_ACTIVE_HIT_CHARGE);
    }

    public static GainApplication recordMageAdditionalActiveHit(
            ServerPlayer mage,
            LivingEntity hostile
    ) {
        return recordMageHostileEvent(mage, hostile, MAGE_ADDITIONAL_ACTIVE_HIT_CHARGE);
    }

    public static GainApplication recordMageWeaveCompletion(ServerPlayer mage) {
        Objects.requireNonNull(mage, "mage");
        if (!isMage(mage)) {
            return GainApplication.rejected();
        }
        int encounterLevel = ProjectActiveEncounterRuntime
                .supportEncounterLevel(mage, mage)
                .orElse(PlayerProgressionService.state(mage).combatLevel());
        return recordClassEvent(mage, MAGE_WEAVE_COMPLETION_CHARGE, encounterLevel);
    }

    public static GainApplication recordMageMeaningfulControl(
            ServerPlayer mage,
            LivingEntity target,
            long nowTick
    ) {
        Objects.requireNonNull(mage, "mage");
        Objects.requireNonNull(target, "target");
        if (nowTick < 0L || !isMage(mage) || mage.level() != target.level()) {
            return GainApplication.rejected();
        }
        var profile = ExternalActorBindingRuntime.combatProfile(target).orElse(null);
        if (profile == null || !profile.meaningfulControlRewardEligible()) {
            return GainApplication.rejected();
        }
        var readyByTarget = MAGE_CONTROL_READY_AT.computeIfAbsent(
                mage.getUUID(), ignored -> new ConcurrentHashMap<>()
        );
        Long readyAt = readyByTarget.get(target.getUUID());
        if (readyAt != null && nowTick < readyAt) {
            return GainApplication.rejected();
        }
        readyByTarget.put(target.getUUID(), Math.addExact(nowTick, MAGE_CONTROL_ICD_TICKS));
        return recordClassEvent(mage, MAGE_MEANINGFUL_CONTROL_CHARGE, profile.contentLevel());
    }

    public static GainApplication recordGuardianOrdinaryGuardedHit(
            ServerPlayer guardian,
            LivingEntity hostile,
            double finalStaminaCost
    ) {
        Objects.requireNonNull(guardian, "guardian");
        Objects.requireNonNull(hostile, "hostile");
        if (!isGuardian(guardian)
                || guardian.level() != hostile.level()
                || !Double.isFinite(finalStaminaCost)
                || finalStaminaCost + 1.0e-9
                        < GUARDIAN_GUARDED_HIT_STAMINA_THRESHOLD) {
            return GainApplication.rejected();
        }
        return recordGuardianHostileEvent(
                guardian,
                hostile,
                GUARDIAN_GUARDED_HIT_CHARGE
        );
    }

    public static GainApplication recordGuardianPerfectGuard(
            ServerPlayer guardian,
            LivingEntity hostile
    ) {
        return recordGuardianHostileEvent(
                guardian,
                hostile,
                GUARDIAN_PERFECT_GUARD_CHARGE
        );
    }

    public static GainApplication recordGuardianBarrierConsumption(
            ServerPlayer guardian,
            ServerPlayer recipient,
            int newlyQualifiedFivePercentSteps,
            LivingEntity hostileSource
    ) {
        Objects.requireNonNull(guardian, "guardian");
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(hostileSource, "hostileSource");
        if (!isGuardian(guardian)
                || guardian.level() != recipient.level()
                || hostileSource.level() != recipient.level()
                || newlyQualifiedFivePercentSteps <= 0) {
            return GainApplication.rejected();
        }
        var profile = ExternalActorBindingRuntime
                .combatProfile(hostileSource)
                .orElse(null);
        if (profile == null) {
            return GainApplication.rejected();
        }
        int steps = Math.min(
                MAX_GUARDIAN_BARRIER_STEPS_PER_SOURCE_RECIPIENT,
                newlyQualifiedFivePercentSteps
        );
        return recordClassEvent(
                guardian,
                steps * GUARDIAN_BARRIER_STEP_CHARGE,
                profile.contentLevel()
        );
    }

    public static GainApplication recordGuardianProvokedHit(
            ServerPlayer guardian,
            LivingEntity hostile,
            long nowTick
    ) {
        Objects.requireNonNull(guardian, "guardian");
        Objects.requireNonNull(hostile, "hostile");
        if (nowTick < 0L
                || !isGuardian(guardian)
                || guardian.level() != hostile.level()
                || !GuardianProvokedRuntime.isProvokedToward(
                        hostile,
                        guardian,
                        nowTick
                )) {
            return GainApplication.rejected();
        }
        var profile = ExternalActorBindingRuntime
                .combatProfile(hostile)
                .orElse(null);
        if (profile == null) {
            return GainApplication.rejected();
        }
        var readyByTarget = GUARDIAN_PROVOKED_HIT_READY_AT
                .computeIfAbsent(
                        guardian.getUUID(),
                        ignored -> new ConcurrentHashMap<>()
                );
        Long readyAt = readyByTarget.get(hostile.getUUID());
        if (readyAt != null && nowTick < readyAt) {
            return GainApplication.rejected();
        }
        readyByTarget.put(
                hostile.getUUID(),
                Math.addExact(
                        nowTick,
                        GUARDIAN_PROVOKED_HIT_ICD_TICKS
                )
        );
        return recordClassEvent(
                guardian,
                GUARDIAN_PROVOKED_HIT_CHARGE,
                profile.contentLevel()
        );
    }

    public static void resetGuardianTransient(UUID guardianId) {
        if (guardianId != null) {
            GUARDIAN_PROVOKED_HIT_READY_AT.remove(guardianId);
        }
    }

    public static void resetMageTransient(UUID mageId) {
        if (mageId != null) {
            MAGE_CONTROL_READY_AT.remove(mageId);
        }
    }

    private static GainApplication recordMageHostileEvent(
            ServerPlayer mage,
            LivingEntity hostile,
            double authoredCharge
    ) {
        Objects.requireNonNull(mage, "mage");
        Objects.requireNonNull(hostile, "hostile");
        if (!isMage(mage) || mage.level() != hostile.level()) {
            return GainApplication.rejected();
        }
        var profile = ExternalActorBindingRuntime.combatProfile(hostile).orElse(null);
        if (profile == null) {
            return GainApplication.rejected();
        }
        return recordClassEvent(mage, authoredCharge, profile.contentLevel());
    }

    private static GainApplication recordGuardianHostileEvent(
            ServerPlayer guardian,
            LivingEntity hostile,
            double authoredCharge
    ) {
        Objects.requireNonNull(guardian, "guardian");
        Objects.requireNonNull(hostile, "hostile");
        if (!isGuardian(guardian)
                || guardian.level() != hostile.level()) {
            return GainApplication.rejected();
        }
        var profile = ExternalActorBindingRuntime
                .combatProfile(hostile)
                .orElse(null);
        if (profile == null) {
            return GainApplication.rejected();
        }
        return recordClassEvent(
                guardian,
                authoredCharge,
                profile.contentLevel()
        );
    }

    private static GainApplication recordWarriorHostileEvent(
            ServerPlayer warrior,
            LivingEntity hostile,
            double authoredCharge
    ) {
        Objects.requireNonNull(warrior, "warrior");
        Objects.requireNonNull(hostile, "hostile");
        if (!isWarrior(warrior)) {
            return GainApplication.rejected();
        }
        var profile = ExternalActorBindingRuntime
                .combatProfile(hostile)
                .orElse(null);
        if (profile == null) {
            return GainApplication.rejected();
        }
        return recordClassEvent(
                warrior,
                authoredCharge,
                profile.contentLevel()
        );
    }

    private static GainApplication recordHunterHostileEvent(
            ServerPlayer hunter,
            LivingEntity hostile,
            double authoredCharge
    ) {
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(hostile, "hostile");
        if (!isHunter(hunter)) {
            return GainApplication.rejected();
        }
        var profile = ExternalActorBindingRuntime
                .combatProfile(hostile)
                .orElse(null);
        if (profile == null) {
            return GainApplication.rejected();
        }
        return recordClassEvent(
                hunter,
                authoredCharge,
                profile.contentLevel()
        );
    }

    public static double charge(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long nowTick = player.level().getGameTime();
        return CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .ultimateCharge(nowTick);
    }

    public static long lockoutRemainingTicks(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long nowTick = player.level().getGameTime();
        return CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .ultimateLockoutRemainingTicks(nowTick);
    }

    public static boolean canActivateUltimate(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long nowTick = player.level().getGameTime();
        return CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .canActivateUltimate(nowTick);
    }

    public static boolean tryActivateUltimate(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long nowTick = player.level().getGameTime();
        return CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .spendUltimate(nowTick);
    }

    public static void resetForRest(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long nowTick = player.level().getGameTime();
        CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .resetUltimateCharge(nowTick);
    }

    public static int supportSteps(
            double effectiveAmount,
            double recipientMaxHp
    ) {
        if (!Double.isFinite(effectiveAmount)
                || effectiveAmount < 0.0
                || !Double.isFinite(recipientMaxHp)
                || recipientMaxHp <= 0.0) {
            throw new IllegalArgumentException(
                    "Ultimate support-step inputs must be finite and valid."
            );
        }
        double stepAmount = recipientMaxHp
                * SUPPORT_STEP_MAX_HP_FRACTION;
        int steps = (int) Math.floor(
                (effectiveAmount + 1.0e-9) / stepAmount
        );
        return Math.min(
                MAX_SUPPORT_STEPS_PER_SOURCE_RECIPIENT,
                Math.max(0, steps)
        );
    }

    private static boolean isWarrior(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.WARRIOR::equals)
                .isPresent();
    }

    private static boolean isHunter(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.HUNTER::equals)
                .isPresent();
    }

    private static boolean isCleric(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.CLERIC::equals)
                .isPresent();
    }

    private static boolean isGuardian(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.GUARDIAN::equals)
                .isPresent();
    }

    private static boolean isMage(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.MAGE::equals)
                .isPresent();
    }

    public record GainApplication(
            boolean accepted,
            double authoredCharge,
            int encounterLevel,
            double levelMultiplier,
            double gearBonus,
            double requestedCharge,
            double grantedCharge,
            double chargeAfter
    ) {
        public GainApplication {
            if (!Double.isFinite(authoredCharge)
                    || authoredCharge < 0.0
                    || encounterLevel < 0
                    || !Double.isFinite(levelMultiplier)
                    || levelMultiplier < 0.0
                    || !Double.isFinite(gearBonus)
                    || gearBonus < 0.0
                    || !Double.isFinite(requestedCharge)
                    || requestedCharge < 0.0
                    || !Double.isFinite(grantedCharge)
                    || grantedCharge < 0.0
                    || !Double.isFinite(chargeAfter)
                    || chargeAfter < 0.0
                    || chargeAfter > 100.0 + 1.0e-9) {
                throw new IllegalArgumentException(
                        "Invalid Ultimate Gauge gain result."
                );
            }
            if (!accepted
                    && (authoredCharge != 0.0
                    || encounterLevel != 0
                    || levelMultiplier != 0.0
                    || gearBonus != 0.0
                    || requestedCharge != 0.0
                    || grantedCharge != 0.0
                    || chargeAfter != 0.0)) {
                throw new IllegalArgumentException(
                        "Rejected Ultimate gain cannot carry state."
                );
            }
        }

        public static GainApplication rejected() {
            return new GainApplication(
                    false,
                    0.0,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }
}

package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-owned shared Ultimate Gauge authority plus current Cleric event publishers.
 */
public final class ProjectUltimateChargeRuntime {
    public static final double CLERIC_DAMAGING_ACTIVE_CHARGE = 2.0;
    public static final double CLERIC_SUPPORT_STEP_CHARGE = 2.0;
    public static final double CLERIC_CLEANSE_CHARGE = 3.0;
    public static final double SUPPORT_STEP_MAX_HP_FRACTION = 0.05;
    public static final int MAX_SUPPORT_STEPS_PER_SOURCE_RECIPIENT = 3;

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

    private static boolean isCleric(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.CLERIC::equals)
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

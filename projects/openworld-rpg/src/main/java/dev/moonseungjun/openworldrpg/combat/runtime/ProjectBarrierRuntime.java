package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalEncounterRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatAttribute;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerBarrierRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterService;
import dev.moonseungjun.openworldrpg.progression.r01.R01NatureSpiritRewardService;
import dev.moonseungjun.openworldrpg.progression.r01.R01RegalhartRewardService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Server-owned application boundary for all project player barriers. */
public final class ProjectBarrierRuntime {
    private ProjectBarrierRuntime() {
    }

    public static GrantApplication applySkillBarrier(
            ServerPlayer caster,
            ServerPlayer target,
            String sourceId,
            double barrierCoefficient,
            double applicableOutputBonus,
            int durationTicks,
            boolean clericGraceSource
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(target, "target");
        if (caster.level() != target.level()
                || !target.isAlive()) {
            return GrantApplication.rejected();
        }

        var progression = PlayerProgressionService.state(caster);
        RootClass activeClass = progression.activeClass()
                .orElse(null);
        if (activeClass == null) {
            return GrantApplication.rejected();
        }

        var casterLoadout = PlayerEquipmentService.state(caster);
        var gearAttributes =
                casterLoadout.aggregateFlatAttributeBonuses();
        var allocation = progression.allocation(activeClass);
        double effectiveWill =
                allocation.value(CombatAttribute.WIL)
                        + gearAttributes.wil();
        double effectiveEndurance =
                allocation.value(CombatAttribute.END)
                        + gearAttributes.end();
        double barrierReference =
                PlayerBarrierAuthority.barrierReference(
                        progression.combatLevel(),
                        effectiveWill,
                        effectiveEndurance
                );
        double requested =
                PlayerBarrierAuthority.skillBarrierAmount(
                        barrierReference,
                        barrierCoefficient,
                        applicableOutputBonus
                                + ClericRootPassiveEffects
                                        .barrierOutputBonus(caster)
                                + ClericSaintEffects
                                        .barrierOutputBonus(caster)
                                + GuardianRootPassiveEffects
                                        .barrierOutputBonus(caster)
                );

        return applyFixedBarrier(
                caster,
                target,
                sourceId,
                requested,
                durationTicks,
                clericGraceSource,
                activeClass == RootClass.GUARDIAN
        );
    }

    /**
     * Encounter-linked Earthloong barrier application. The caller must pass the active Earthloong
     * whose engaged ally is receiving the barrier.
     */
    public static GrantApplication applyEarthloongSkillBarrier(
            ServerPlayer caster,
            ServerPlayer target,
            LivingEntity earthloong,
            String sourceId,
            double barrierCoefficient,
            double applicableOutputBonus,
            int durationTicks,
            boolean clericGraceSource
    ) {
        Objects.requireNonNull(earthloong, "earthloong");
        return applyEncounterSkillBarrier(
                caster,
                target,
                earthloong,
                sourceId,
                barrierCoefficient,
                applicableOutputBonus,
                durationTicks,
                clericGraceSource
        );
    }

    /**
     * Encounter-linked Nature Spirit barrier application. A positive effective grant to another
     * actively engaged player is one valid support contribution; proximity and self-barriers do not
     * qualify.
     */
    public static GrantApplication applyNatureSpiritSkillBarrier(
            ServerPlayer caster,
            ServerPlayer target,
            LivingEntity natureSpirit,
            String sourceId,
            double barrierCoefficient,
            double applicableOutputBonus,
            int durationTicks,
            boolean clericGraceSource
    ) {
        Objects.requireNonNull(natureSpirit, "natureSpirit");
        return applyEncounterSkillBarrier(
                caster,
                target,
                natureSpirit,
                sourceId,
                barrierCoefficient,
                applicableOutputBonus,
                durationTicks,
                clericGraceSource
        );
    }

    /**
     * Encounter-linked Regalhart barrier participation. A positive effective grant to another
     * actively engaged player may establish that caster's personal boss-reward eligibility.
     */
    public static GrantApplication applyRegalhartSkillBarrier(
            ServerPlayer caster,
            ServerPlayer target,
            LivingEntity regalhart,
            String sourceId,
            double barrierCoefficient,
            double applicableOutputBonus,
            int durationTicks,
            boolean clericGraceSource
    ) {
        Objects.requireNonNull(regalhart, "regalhart");
        return applyEncounterSkillBarrier(
                caster,
                target,
                regalhart,
                sourceId,
                barrierCoefficient,
                applicableOutputBonus,
                durationTicks,
                clericGraceSource
        );
    }

    private static GrantApplication applyEncounterSkillBarrier(
            ServerPlayer caster,
            ServerPlayer target,
            LivingEntity encounterActor,
            String sourceId,
            double barrierCoefficient,
            double applicableOutputBonus,
            int durationTicks,
            boolean clericGraceSource
    ) {
        GrantApplication application = applySkillBarrier(
                caster,
                target,
                sourceId,
                barrierCoefficient,
                applicableOutputBonus,
                durationTicks,
                clericGraceSource
        );
        if (!application.accepted()
                || application.effectiveGranted() <= 0.0) {
            return application;
        }

        long gameTick = caster.level().getGameTime();
        R01EarthloongPhysicalEncounterRuntime.recordEffectiveBarrierThreat(
                encounterActor,
                caster,
                target,
                application.effectiveGranted(),
                gameTick
        );
        if (caster == target) {
            return application;
        }

        R01EarthloongEncounterService.recordValidatedSupportContribution(
                encounterActor,
                caster
        );
        R01NatureSpiritRewardService.recordValidatedSupportContribution(
                encounterActor,
                caster
        );
        R01RegalhartRewardService.recordValidatedSupportContribution(
                encounterActor,
                caster
        );
        CombatStateServices.markCombatActivity(
                caster.getUUID(),
                caster.level().getGameTime()
        );
        return application;
    }

    public static GrantApplication applyFixedBarrier(
            ServerPlayer sourcePlayer,
            ServerPlayer target,
            String sourceId,
            double requestedAmount,
            int durationTicks,
            boolean clericGraceSource
    ) {
        return applyFixedBarrier(
                sourcePlayer,
                target,
                sourceId,
                requestedAmount,
                durationTicks,
                clericGraceSource,
                false
        );
    }

    public static GrantApplication applyFixedBarrier(
            ServerPlayer sourcePlayer,
            ServerPlayer target,
            String sourceId,
            double requestedAmount,
            int durationTicks,
            boolean clericGraceSource,
            boolean guardianResolveSource
    ) {
        Objects.requireNonNull(sourcePlayer, "sourcePlayer");
        Objects.requireNonNull(target, "target");
        if (sourcePlayer.level() != target.level()
                || !target.isAlive()) {
            return GrantApplication.rejected();
        }

        long nowTick = target.level().getGameTime();
        PlayerBarrierRuntimeState.GrantResult result =
                CombatStateServices.barrierStates()
                        .getOrCreate(target.getUUID())
                        .grant(
                                sourceId,
                                sourcePlayer.getUUID(),
                                requestedAmount,
                                target.getMaxHealth(),
                                durationTicks,
                                clericGraceSource,
                                guardianResolveSource,
                                nowTick
                        );
        return new GrantApplication(
                true,
                result.requestedAmount(),
                result.appliedAmount(),
                result.effectiveGranted(),
                result.totalBarrierAfter(),
                result.expiresAtTick()
        );
    }

    public static AbsorptionApplication absorbHostileDamage(
            ServerPlayer target,
            double incomingDamage,
            long nowTick
    ) {
        return absorbHostileDamage(
                null,
                target,
                incomingDamage,
                nowTick
        );
    }

    public static AbsorptionApplication absorbHostileDamage(
            LivingEntity hostileSource,
            ServerPlayer target,
            double incomingDamage,
            long nowTick
    ) {
        Objects.requireNonNull(target, "target");
        PlayerBarrierRuntimeState.Absorption result =
                CombatStateServices.barrierStates()
                        .getOrCreate(target.getUUID())
                        .absorbHostileDamage(
                                incomingDamage,
                                target.getMaxHealth(),
                                nowTick
                        );

        int gracePipsGranted = 0;
        for (var consumption : result.sourceConsumptions()) {
            ServerPlayer sourcePlayer = target.level()
                    .getServer()
                    .getPlayerList()
                    .getPlayer(
                            consumption.sourcePlayerId()
                    );
            if (sourcePlayer == null) {
                continue;
            }

            long sourceTick = sourcePlayer.level()
                    .getGameTime();
            if (consumption.guardianResolveSource()) {
                GuardianResolveRuntime.onBarrierAbsorbed(
                        sourcePlayer,
                        target,
                        consumption.absorbedDamage(),
                        sourceTick
                );
                if (hostileSource != null
                        && consumption
                                .guardianUltimateChargeStepsReached() > 0) {
                    ProjectUltimateChargeRuntime
                            .recordGuardianBarrierConsumption(
                                    sourcePlayer,
                                    target,
                                    consumption
                                            .guardianUltimateChargeStepsReached(),
                                    hostileSource
                            );
                }
            }

            boolean graceQualified =
                    consumption.clericGraceThresholdReached();
            boolean ultimateQualified =
                    consumption.clericUltimateChargeStepsReached() > 0;
            if ((!graceQualified && !ultimateQualified)
                    || PlayerProgressionService.state(sourcePlayer)
                            .activeClass()
                            .filter(RootClass.CLERIC::equals)
                            .isEmpty()) {
                continue;
            }

            if (graceQualified) {
                ClericRootPassiveRuntime.synchronize(sourcePlayer);
                var combat = CombatStateServices.states()
                        .getOrCreate(
                                sourcePlayer.getUUID(),
                                sourceTick
                        );
                var gain = CombatStateServices.clericGraceStates()
                        .getOrCreate(sourcePlayer.getUUID())
                        .recordConsumedBarrier(
                                target.getUUID(),
                                consumption.consumedSinceGrant(),
                                target.getMaxHealth(),
                                sourceTick,
                                combat.lastCombatActivityTick()
                        );
                ClericRootPassiveRuntime.onGraceGain(
                        sourcePlayer,
                        gain,
                        sourceTick
                );
                if (gain.pipAdded()) {
                    gracePipsGranted++;
                }
            }

            if (ultimateQualified && hostileSource != null) {
                ProjectUltimateChargeRuntime
                        .recordClericBarrierConsumption(
                                sourcePlayer,
                                target,
                                consumption
                                        .clericUltimateChargeStepsReached(),
                                hostileSource
                        );
            }
        }

        return new AbsorptionApplication(
                result.remainingDamage(),
                result.absorbedDamage(),
                result.remainingBarrier(),
                gracePipsGranted
        );
    }

    public record GrantApplication(
            boolean accepted,
            double requestedAmount,
            double appliedAmount,
            double effectiveGranted,
            double totalBarrierAfter,
            long expiresAtTick
    ) {
        public GrantApplication {
            if (requestedAmount < 0.0
                    || appliedAmount < 0.0
                    || effectiveGranted < 0.0
                    || totalBarrierAfter < 0.0
                    || expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid barrier grant result."
                );
            }
            if (!accepted
                    && (requestedAmount != 0.0
                    || appliedAmount != 0.0
                    || effectiveGranted != 0.0
                    || totalBarrierAfter != 0.0
                    || expiresAtTick != 0L)) {
                throw new IllegalArgumentException(
                        "Rejected barrier cannot carry applied state."
                );
            }
        }

        public static GrantApplication rejected() {
            return new GrantApplication(
                    false,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0L
            );
        }
    }

    public record AbsorptionApplication(
            double remainingDamage,
            double absorbedDamage,
            double remainingBarrier,
            int gracePipsGranted
    ) {
        public AbsorptionApplication {
            if (!Double.isFinite(remainingDamage)
                    || remainingDamage < 0.0
                    || !Double.isFinite(absorbedDamage)
                    || absorbedDamage < 0.0
                    || !Double.isFinite(remainingBarrier)
                    || remainingBarrier < 0.0
                    || gracePipsGranted < 0) {
                throw new IllegalArgumentException(
                        "Invalid barrier absorption result."
                );
            }
        }
    }
}

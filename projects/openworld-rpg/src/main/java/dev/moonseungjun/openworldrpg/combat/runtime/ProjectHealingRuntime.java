package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.PlayerHealingAuthority;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalEncounterRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatAttribute;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterService;
import dev.moonseungjun.openworldrpg.progression.r01.R01NatureSpiritRewardService;
import dev.moonseungjun.openworldrpg.progression.r01.R01RegalhartRewardService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-owned application boundary for authored player skill healing.
 *
 * <p>Recovery Belt consumables do not use Healing Done. This path is for project skills whose
 * authored HealCoefficient resolves against the caster's canonical HealingReference. Effective
 * healing is measured after Minecraft MaxHP clamping so zero-effect full-HP casts cannot qualify
 * as encounter support participation.</p>
 */
public final class ProjectHealingRuntime {
    private static final String SAINT_OVERHEAL_BARRIER_SOURCE =
            "openworld_rpg:saint/overflowing_grace/";

    private ProjectHealingRuntime() {
    }

    public static Application applySkillHeal(
            ServerPlayer caster,
            ServerPlayer target,
            double healCoefficient
    ) {
        return applySkillHeal(
                caster,
                target,
                healCoefficient,
                1.0
        );
    }

    public static Application applySkillHeal(
            ServerPlayer caster,
            ServerPlayer target,
            double healCoefficient,
            double outputMultiplier
    ) {
        return apply(
                caster,
                target,
                healCoefficient,
                outputMultiplier,
                null
        );
    }

    /**
     * Applies a real skill heal and, when it restores HP to another player, records one validated
     * Earthloong support contribution through the existing encounter authority.
     *
     * <p>The skill/encounter caller remains responsible for passing the actual active Earthloong.
     * Invalid/non-Earthloong entities simply fail the participation bridge without undoing a valid
     * heal.</p>
     */
    public static Application applyEarthloongSkillHeal(
            ServerPlayer caster,
            ServerPlayer target,
            LivingEntity earthloong,
            double healCoefficient
    ) {
        Objects.requireNonNull(earthloong, "earthloong");
        return apply(caster, target, healCoefficient, 1.0, earthloong);
    }

    /**
     * Applies a real skill heal and, when it restores HP to another player, records one validated
     * Nature Spirit support contribution through the encounter reward authority.
     *
     * <p>The caller must pass the actual authored Nature Spirit whose encounter the healed ally is
     * actively engaged in. Merely being nearby never qualifies.</p>
     */
    public static Application applyNatureSpiritSkillHeal(
            ServerPlayer caster,
            ServerPlayer target,
            LivingEntity natureSpirit,
            double healCoefficient
    ) {
        Objects.requireNonNull(natureSpirit, "natureSpirit");
        return apply(caster, target, healCoefficient, 1.0, natureSpirit);
    }

    /**
     * Encounter-linked Regalhart heal participation. The caller must pass the actual authored
     * Regalhart whose engaged ally is being healed.
     */
    public static Application applyRegalhartSkillHeal(
            ServerPlayer caster,
            ServerPlayer target,
            LivingEntity regalhart,
            double healCoefficient
    ) {
        Objects.requireNonNull(regalhart, "regalhart");
        return apply(caster, target, healCoefficient, 1.0, regalhart);
    }

    private static Application apply(
            ServerPlayer caster,
            ServerPlayer target,
            double healCoefficient,
            double outputMultiplier,
            LivingEntity encounterActor
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(healCoefficient) || healCoefficient <= 0.0) {
            throw new IllegalArgumentException(
                    "healCoefficient must be finite and positive."
            );
        }
        if (!Double.isFinite(outputMultiplier)
                || outputMultiplier < 1.0) {
            throw new IllegalArgumentException(
                    "outputMultiplier must be finite and >= 1."
            );
        }
        if (caster.level() != target.level() || !target.isAlive()) {
            return Application.rejected();
        }

        var progression = PlayerProgressionService.state(caster);
        var activeClass = progression.activeClass().orElse(null);
        if (activeClass == null) {
            return Application.rejected();
        }

        var casterLoadout = PlayerEquipmentService.state(caster);
        var targetLoadout = PlayerEquipmentService.state(target);
        var gearAttributes = casterLoadout.aggregateFlatAttributeBonuses();
        var allocation = progression.allocation(activeClass);

        double effectiveWill = allocation.value(CombatAttribute.WIL)
                + gearAttributes.wil();
        double effectiveIntelligence = allocation.value(CombatAttribute.INT)
                + gearAttributes.intel();
        double healingReference = PlayerHealingAuthority.healingReference(
                progression.combatLevel(),
                effectiveWill,
                effectiveIntelligence
        );
        double requestedHealing =
                PlayerHealingAuthority.skillHealingAmount(
                        healingReference,
                        healCoefficient,
                        casterLoadout.aggregateHealingDoneBonus(),
                        targetLoadout.aggregateHealingReceivedBonus()
                )
                * outputMultiplier
                * ClericRootPassiveEffects
                        .healingOutputMultiplier(caster)
                * ClericSaintEffects
                        .healingOutputMultiplier(caster);

        float before = target.getHealth();
        target.heal((float) requestedHealing);
        double effectiveHealing = Math.max(
                0.0,
                target.getHealth() - before
        );
        ClericRootPassiveRuntime.recordEffectiveHealing(
                caster,
                effectiveHealing,
                caster.level().getGameTime()
        );

        double otherwiseWastedOverheal = Math.max(
                0.0,
                requestedHealing - effectiveHealing
        );
        double overflowBarrierRequested =
                ClericSaintEffects.overhealBarrierAmount(
                        caster,
                        otherwiseWastedOverheal,
                        target.getMaxHealth()
                );
        double overflowBarrierGranted = 0.0;
        if (overflowBarrierRequested > 0.0) {
            var overflow = ProjectBarrierRuntime.applyFixedBarrier(
                    caster,
                    target,
                    SAINT_OVERHEAL_BARRIER_SOURCE + caster.getUUID(),
                    overflowBarrierRequested,
                    PlayerBarrierAuthority.DEFAULT_BARRIER_DURATION_TICKS,
                    true
            );
            if (overflow.accepted()) {
                overflowBarrierGranted = overflow.effectiveGranted();
            }
        }

        boolean newEarthloongParticipation = false;
        boolean newNatureSpiritParticipation = false;
        boolean newRegalhartParticipation = false;
        if (encounterActor != null
                && caster != target
                && (effectiveHealing > 0.0
                        || overflowBarrierGranted > 0.0)) {
            long gameTick = caster.level().getGameTime();
            if (effectiveHealing > 0.0) {
                R01EarthloongPhysicalEncounterRuntime.recordEffectiveHealingThreat(
                        encounterActor,
                        caster,
                        target,
                        effectiveHealing,
                        gameTick
                );
            }
            if (overflowBarrierGranted > 0.0) {
                R01EarthloongPhysicalEncounterRuntime.recordEffectiveBarrierThreat(
                        encounterActor,
                        caster,
                        target,
                        overflowBarrierGranted,
                        gameTick
                );
            }
            newEarthloongParticipation =
                    R01EarthloongEncounterService.recordValidatedSupportContribution(
                            encounterActor,
                            caster
                    );
            newNatureSpiritParticipation =
                    R01NatureSpiritRewardService.recordValidatedSupportContribution(
                            encounterActor,
                            caster
                    );
            newRegalhartParticipation =
                    R01RegalhartRewardService.recordValidatedSupportContribution(
                            encounterActor,
                            caster
                    );
            CombatStateServices.markCombatActivity(
                    caster.getUUID(),
                    gameTick
            );
        }

        return new Application(
                true,
                requestedHealing,
                effectiveHealing,
                newEarthloongParticipation,
                newNatureSpiritParticipation,
                newRegalhartParticipation,
                overflowBarrierGranted
        );
    }

    public record Application(
            boolean accepted,
            double requestedHealing,
            double effectiveHealing,
            boolean newEarthloongParticipation,
            boolean newNatureSpiritParticipation,
            boolean newRegalhartParticipation,
            double overflowBarrierGranted
    ) {
        public Application(
                boolean accepted,
                double requestedHealing,
                double effectiveHealing,
                boolean newEarthloongParticipation,
                boolean newNatureSpiritParticipation,
                boolean newRegalhartParticipation
        ) {
            this(
                    accepted,
                    requestedHealing,
                    effectiveHealing,
                    newEarthloongParticipation,
                    newNatureSpiritParticipation,
                    newRegalhartParticipation,
                    0.0
            );
        }
        public Application(
                boolean accepted,
                double requestedHealing,
                double effectiveHealing,
                boolean newEarthloongParticipation,
                boolean newNatureSpiritParticipation
        ) {
            this(
                    accepted,
                    requestedHealing,
                    effectiveHealing,
                    newEarthloongParticipation,
                    newNatureSpiritParticipation,
                    false
            );
        }

        public Application {
            if (!Double.isFinite(requestedHealing)
                    || requestedHealing < 0.0
                    || !Double.isFinite(effectiveHealing)
                    || effectiveHealing < 0.0
                    || effectiveHealing > requestedHealing + 0.001
                    || !Double.isFinite(overflowBarrierGranted)
                    || overflowBarrierGranted < 0.0) {
                throw new IllegalArgumentException(
                        "Healing application amounts are invalid."
                );
            }
            if (!accepted
                    && (requestedHealing != 0.0
                    || effectiveHealing != 0.0
                    || newEarthloongParticipation
                    || newNatureSpiritParticipation
                    || newRegalhartParticipation
                    || overflowBarrierGranted != 0.0)) {
                throw new IllegalArgumentException(
                        "Rejected healing cannot carry applied state."
                );
            }
            if ((newEarthloongParticipation
                    || newNatureSpiritParticipation
                    || newRegalhartParticipation)
                    && effectiveHealing <= 0.0
                    && overflowBarrierGranted <= 0.0) {
                throw new IllegalArgumentException(
                        "Support participation requires effective healing or barrier."
                );
            }
            int participationCount =
                    (newEarthloongParticipation ? 1 : 0)
                            + (newNatureSpiritParticipation ? 1 : 0)
                            + (newRegalhartParticipation ? 1 : 0);
            if (participationCount > 1) {
                throw new IllegalArgumentException(
                        "One heal cannot qualify for two encounter actors."
                );
            }
        }

        public static Application rejected() {
            return new Application(false, 0.0, 0.0, false, false, false, 0.0);
        }
    }
}

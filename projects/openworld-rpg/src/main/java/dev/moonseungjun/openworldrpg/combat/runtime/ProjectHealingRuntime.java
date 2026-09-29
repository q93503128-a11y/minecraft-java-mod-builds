package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerHealingAuthority;
import dev.moonseungjun.openworldrpg.combat.state.CombatAttribute;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterService;
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
    private ProjectHealingRuntime() {
    }

    public static Application applySkillHeal(
            ServerPlayer caster,
            ServerPlayer target,
            double healCoefficient
    ) {
        return apply(caster, target, healCoefficient, null);
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
        return apply(caster, target, healCoefficient, earthloong);
    }

    private static Application apply(
            ServerPlayer caster,
            ServerPlayer target,
            double healCoefficient,
            LivingEntity earthloong
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(healCoefficient) || healCoefficient <= 0.0) {
            throw new IllegalArgumentException(
                    "healCoefficient must be finite and positive."
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
        double requestedHealing = PlayerHealingAuthority.skillHealingAmount(
                healingReference,
                healCoefficient,
                casterLoadout.aggregateHealingDoneBonus(),
                targetLoadout.aggregateHealingReceivedBonus()
        );

        float before = target.getHealth();
        target.heal((float) requestedHealing);
        double effectiveHealing = Math.max(0.0, target.getHealth() - before);

        boolean newEarthloongParticipation = false;
        if (earthloong != null
                && caster != target
                && effectiveHealing > 0.0) {
            newEarthloongParticipation =
                    R01EarthloongEncounterService.recordValidatedSupportContribution(
                            earthloong,
                            caster
                    );
            CombatStateServices.markCombatActivity(
                    caster.getUUID(),
                    caster.level().getGameTime()
            );
        }

        return new Application(
                true,
                requestedHealing,
                effectiveHealing,
                newEarthloongParticipation
        );
    }

    public record Application(
            boolean accepted,
            double requestedHealing,
            double effectiveHealing,
            boolean newEarthloongParticipation
    ) {
        public Application {
            if (!Double.isFinite(requestedHealing)
                    || requestedHealing < 0.0
                    || !Double.isFinite(effectiveHealing)
                    || effectiveHealing < 0.0
                    || effectiveHealing > requestedHealing + 0.001) {
                throw new IllegalArgumentException(
                        "Healing application amounts are invalid."
                );
            }
            if (!accepted
                    && (requestedHealing != 0.0
                    || effectiveHealing != 0.0
                    || newEarthloongParticipation)) {
                throw new IllegalArgumentException(
                        "Rejected healing cannot carry applied state."
                );
            }
            if (newEarthloongParticipation && effectiveHealing <= 0.0) {
                throw new IllegalArgumentException(
                        "Support participation requires effective healing."
                );
            }
        }

        public static Application rejected() {
            return new Application(false, 0.0, 0.0, false);
        }
    }
}

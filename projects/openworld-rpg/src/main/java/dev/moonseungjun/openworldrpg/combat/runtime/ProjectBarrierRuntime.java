package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.state.CombatAttribute;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerBarrierRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
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
                );

        return applyFixedBarrier(
                caster,
                target,
                sourceId,
                requested,
                durationTicks,
                clericGraceSource
        );
    }

    public static GrantApplication applyFixedBarrier(
            ServerPlayer sourcePlayer,
            ServerPlayer target,
            String sourceId,
            double requestedAmount,
            int durationTicks,
            boolean clericGraceSource
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
            boolean graceQualified =
                    consumption.clericGraceThresholdReached();
            boolean ultimateQualified =
                    consumption.clericUltimateChargeStepsReached() > 0;
            if (!graceQualified && !ultimateQualified) {
                continue;
            }

            ServerPlayer sourcePlayer = target.level()
                    .getServer()
                    .getPlayerList()
                    .getPlayer(
                            consumption.sourcePlayerId()
                    );
            if (sourcePlayer == null
                    || PlayerProgressionService.state(sourcePlayer)
                            .activeClass()
                            .filter(RootClass.CLERIC::equals)
                            .isEmpty()) {
                continue;
            }

            long sourceTick = sourcePlayer.level()
                    .getGameTime();
            if (graceQualified) {
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

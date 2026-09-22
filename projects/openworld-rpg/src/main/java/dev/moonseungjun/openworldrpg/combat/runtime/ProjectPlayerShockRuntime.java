package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.CombatAttribute;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Project-owned player Shock buildup/proc bridge for authored enemy attacks. */
public final class ProjectPlayerShockRuntime {
    private static final String EARTHLOONG_ID = "threateningly_mobs:the_earthloong";
    private static final double EARTHLOONG_PROC_BENCHMARK_SHARE = 0.08;
    private static final double EARTHLOONG_PROC_POISE_PRESSURE = 16.0;

    private ProjectPlayerShockRuntime() {
    }

    public static Application applyEarthloongBuildup(
            LivingEntity earthloong,
            ServerPlayer target,
            double buildup
    ) {
        Objects.requireNonNull(earthloong, "earthloong");
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(buildup) || buildup < 0.0) {
            throw new IllegalArgumentException("Shock buildup must be finite and non-negative.");
        }

        var profile = ExternalActorBindingRuntime.combatProfile(earthloong).orElse(null);
        if (profile == null
                || !EARTHLOONG_ID.equals(profile.entityId())
                || earthloong.level() != target.level()) {
            return Application.rejected();
        }

        var progression = PlayerProgressionService.state(target);
        var loadout = PlayerEquipmentService.state(target);
        AttributeAllocation allocation = progression.activeClass()
                .map(progression::allocation)
                .orElseGet(AttributeAllocation::unspent);
        double effectiveWill = allocation.value(CombatAttribute.WIL)
                + loadout.aggregateFlatAttributeBonuses().wil();
        int threshold = ProjectCombatRules.playerAilmentThreshold(effectiveWill);
        long gameTick = target.level().getGameTime();

        var shockState = CombatStateServices.shockStates().synchronize(
                target.getUUID(),
                threshold,
                gameTick
        );
        var buildupResult = shockState.apply(buildup, gameTick);
        if (!buildupResult.procced()) {
            return new Application(
                    true,
                    buildup,
                    buildupResult.remainingBuildup(),
                    threshold,
                    false,
                    false,
                    false
            );
        }

        double procRawDamage =
                ProjectCombatRules.benchmarkPlayerHealth(profile.contentLevel())
                        * EARTHLOONG_PROC_BENCHMARK_SHARE;
        var procDamage = ProjectPlayerIncomingDamageRuntime.applyProjectOwnedActorHit(
                earthloong,
                target,
                PlayerDefenseAuthority.IncomingHit.unguardable(
                        procRawDamage,
                        ProjectImpactTransaction.DamageSchool.MAGIC,
                        profile.contentLevel(),
                        false
                )
        );
        var poise = ProjectPlayerPoisePressureRuntime.applyAuthoredPressure(
                target,
                EARTHLOONG_PROC_POISE_PRESSURE
        );

        return new Application(
                true,
                buildup,
                0.0,
                threshold,
                true,
                procDamage.minecraftDamageApplied(),
                poise.applied()
        );
    }

    public record Application(
            boolean accepted,
            double acceptedBuildup,
            double remainingBuildup,
            double threshold,
            boolean procced,
            boolean procDamageApplied,
            boolean procPoiseApplied
    ) {
        private static Application rejected() {
            return new Application(false, 0.0, 0.0, 0.0, false, false, false);
        }
    }
}

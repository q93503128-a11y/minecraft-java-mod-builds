package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import java.util.Objects;

/**
 * Server-owned decision point for melee damage that arrives through external presentation backends.
 *
 * <p>Better Combat is a presentation/hit-candidate source only. For project-owned actors the donor's
 * proposed vanilla damage is validated as a positive hit candidate and then discarded; canonical
 * WeaponPower, family cadence, player attributes, target mitigation and poise determine the result.</p>
 */
public final class CombatDamageAuthority {
    private CombatDamageAuthority() {
    }

    public static MeleeDamageDecision authorizeBetterCombatMelee(
            float donorProposedDamage,
            int comboCount,
            PlayerCombatBuildState build,
            ProjectImpactTransaction.DamageTargetSnapshot target
    ) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(target, "target");

        if (comboCount < 0
                || !Float.isFinite(donorProposedDamage)
                || donorProposedDamage <= 0.0F
                || !ProjectBasicAttackRules.supportsBetterCombatMelee(build.equipment().weaponFamily())) {
            return MeleeDamageDecision.rejected();
        }

        ProjectBasicAttackRules.BasicHitProfile hit =
                ProjectBasicAttackRules.hitProfile(build.equipment().weaponFamily(), comboCount);
        ProjectImpactTransaction.DamageSourceSnapshot source =
                build.damageSource(ProjectImpactTransaction.DamageSchool.PHYSICAL);

        ProjectImpactTransaction.DirectDamageResult damage =
                ProjectImpactTransaction.resolveDirectDamage(
                        new ProjectImpactTransaction.DirectDamageRequest(
                                source,
                                target,
                                ProjectImpactTransaction.DamageSchool.PHYSICAL,
                                hit.damageActionCoefficient(),
                                1.0,
                                1.0
                        )
                );

        ProjectImpactTransaction.PoiseResult poise =
                ProjectImpactTransaction.resolvePoise(
                        new ProjectImpactTransaction.PoiseRequest(
                                target.poiseMax(),
                                target.poiseMax(),
                                source.poiseOutputMultiplier(),
                                hit.poiseActionCoefficient(),
                                1.0,
                                1.0
                        )
                );

        if (damage.finalDamage() <= 0.0 || poise.poiseDamage() < 0.0) {
            return MeleeDamageDecision.rejected();
        }

        return new MeleeDamageDecision(
                true,
                damage.finalDamage(),
                poise.poiseDamage(),
                hit.damageActionCoefficient(),
                hit.poiseActionCoefficient(),
                hit.cycleFinisher()
        );
    }

    public static RangedDamageDecision authorizeProjectileBasic(
            float donorProposedDamage,
            PlayerCombatBuildState build,
            ProjectImpactTransaction.DamageTargetSnapshot target
    ) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(target, "target");

        if (!Float.isFinite(donorProposedDamage)
                || donorProposedDamage <= 0.0F
                || !ProjectBasicAttackRules.supportsAuthoritativeProjectileBasic(
                        build.equipment().weaponFamily()
                )) {
            return RangedDamageDecision.rejected();
        }

        ProjectBasicAttackRules.BasicHitProfile hit =
                ProjectBasicAttackRules.projectileFullShotProfile(
                        build.equipment().weaponFamily()
                );
        ProjectImpactTransaction.DamageSourceSnapshot source =
                build.damageSource(ProjectImpactTransaction.DamageSchool.PHYSICAL);

        ProjectImpactTransaction.DirectDamageResult resolvedDamage =
                ProjectImpactTransaction.resolveDirectDamage(
                        new ProjectImpactTransaction.DirectDamageRequest(
                                source,
                                target,
                                ProjectImpactTransaction.DamageSchool.PHYSICAL,
                                hit.damageActionCoefficient(),
                                1.0,
                                1.0
                        )
                );

        ProjectImpactTransaction.PoiseResult poise =
                ProjectImpactTransaction.resolvePoise(
                        new ProjectImpactTransaction.PoiseRequest(
                                target.poiseMax(),
                                target.poiseMax(),
                                source.poiseOutputMultiplier(),
                                hit.poiseActionCoefficient(),
                                1.0,
                                1.0
                        )
                );

        if (resolvedDamage.finalDamage() <= 0.0 || poise.poiseDamage() < 0.0) {
            return RangedDamageDecision.rejected();
        }

        return new RangedDamageDecision(
                true,
                resolvedDamage.finalDamage(),
                poise.poiseDamage(),
                hit.damageActionCoefficient(),
                hit.poiseActionCoefficient()
        );
    }

    public static ProjectImpactTransaction.DirectDamageResult resolveProjectDirectDamage(
            ProjectImpactTransaction.DirectDamageRequest request
    ) {
        return ProjectImpactTransaction.resolveDirectDamage(request);
    }

    public record MeleeDamageDecision(
            boolean accepted,
            double finalDamage,
            double poiseDamage,
            double damageActionCoefficient,
            double poiseActionCoefficient,
            boolean cycleFinisher
    ) {
        public static MeleeDamageDecision rejected() {
            return new MeleeDamageDecision(false, 0.0, 0.0, 0.0, 0.0, false);
        }
    }

    public record RangedDamageDecision(
            boolean accepted,
            double finalDamage,
            double poiseDamage,
            double damageActionCoefficient,
            double poiseActionCoefficient
    ) {
        public static RangedDamageDecision rejected() {
            return new RangedDamageDecision(false, 0.0, 0.0, 0.0, 0.0);
        }
    }
}

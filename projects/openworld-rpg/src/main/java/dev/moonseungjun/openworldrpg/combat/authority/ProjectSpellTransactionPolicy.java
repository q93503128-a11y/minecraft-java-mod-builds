package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatStateStore;

/**
 * Atomic server policy for one canonical project spell.
 *
 * <p>PRE checks are side-effect free. POST is used as the accepted-cast commit point because Spell
 * Engine reaches POST only after its own cooldown/ammo gates. All project spells share the accepted
 * cast lock inside {@link PlayerCombatState}, so simultaneous inputs cannot double-spend through
 * separate per-spell policies.</p>
 */
public final class ProjectSpellTransactionPolicy implements SpellCastAuthority.Policy {
    private final ProjectSpellSpec spec;
    private final PlayerCombatStateStore states;
    private final SpellImpactPort impactPort;

    public ProjectSpellTransactionPolicy(
            ProjectSpellSpec spec,
            PlayerCombatStateStore states,
            SpellImpactPort impactPort
    ) {
        this.spec = spec;
        this.states = states;
        this.impactPort = impactPort;
    }

    @Override
    public boolean preflight(SpellCastAuthority.CastContext context) {
        PlayerCombatState state = states.getOrCreate(context.playerId(), context.gameTick());
        synchronized (state) {
            if (state.isAcceptedCastReentry(context.spellId(), context.gameTick())) {
                return true;
            }
            if (context.engineContinuation() && state.isAcceptedCastContinuation(context.spellId())) {
                return true;
            }
            if (state.hasCompetingAcceptedCast(context.spellId(), context.gameTick())) {
                return false;
            }
            return !state.isCoolingDown(spec.id(), context.gameTick())
                    && state.canSpendMana(spec.manaCost(), context.gameTick());
        }
    }

    @Override
    public boolean commitAcceptedCast(SpellCastAuthority.CastContext context) {
        PlayerCombatState state = states.getOrCreate(context.playerId(), context.gameTick());
        synchronized (state) {
            if (state.isAcceptedCastReentry(context.spellId(), context.gameTick())) {
                return true;
            }
            if (context.engineContinuation() && state.isAcceptedCastContinuation(context.spellId())) {
                return true;
            }
            if (state.hasCompetingAcceptedCast(context.spellId(), context.gameTick())) {
                return false;
            }
            if (state.isCoolingDown(spec.id(), context.gameTick())
                    || !state.spendMana(spec.manaCost(), context.gameTick())) {
                return false;
            }

            state.startCooldown(spec.id(), spec.cooldownTicks(), context.gameTick());
            state.beginAcceptedCast(
                    spec.id(),
                    context.gameTick() + spec.reentryWindowTicks(),
                    context.gameTick()
            );
            return true;
        }
    }

    @Override
    public void onEngineCostConsumed(SpellCastAuthority.CastContext context) {
        PlayerCombatState state = states.getOrCreate(context.playerId(), context.gameTick());
        synchronized (state) {
            state.requireAcceptedCast(context.spellId(), context.gameTick());
        }
    }

    @Override
    public void onEngineCastCompleted(SpellCastAuthority.CastCompletion context) {
        PlayerCombatState state = states.getOrCreate(context.playerId(), context.gameTick());
        synchronized (state) {
            state.requireAcceptedCast(context.spellId(), context.gameTick());
            state.completeAcceptedCast(context.spellId());
        }
    }

    @Override
    public SpellCastAuthority.ImpactDecision onImpact(SpellCastAuthority.ImpactContext context) {
        /*
         * PROJECTILE/METEOR delivery completes at launch in Spell Engine. The later impact therefore
         * cannot depend on the resource transaction that ended when the cast completed.
         */
        return impactPort.apply(spec, context);
    }

    public ProjectSpellSpec spec() {
        return spec;
    }

    @FunctionalInterface
    public interface SpellImpactPort {
        SpellCastAuthority.ImpactDecision apply(
                ProjectSpellSpec spec,
                SpellCastAuthority.ImpactContext context
        );

        static SpellImpactPort failClosed() {
            return (spec, context) -> SpellCastAuthority.ImpactDecision.rejected();
        }

        /**
         * Canonical direct-magic resolver for project-owned spells.
         *
         * <p>Spell Engine's enginePower and impact-total values are deliberately not damage authority.
         * The project source/target snapshots plus the project spell coefficient are the only inputs
         * to canonical direct damage. Runtime application may still fail closed before reaching this
         * port if either authoritative snapshot is unavailable.</p>
         */
        static SpellImpactPort directMagic() {
            return (spec, context) -> {
                ProjectImpactTransaction.DirectDamageResult damage =
                        ProjectImpactTransaction.resolveDirectDamage(
                                new ProjectImpactTransaction.DirectDamageRequest(
                                        context.sourceSnapshot(),
                                        context.targetSnapshot(),
                                        ProjectImpactTransaction.DamageSchool.MAGIC,
                                        spec.actionCoefficient(),
                                        1.0,
                                        1.0
                                )
                        );

                if (damage.finalDamage() <= 0.0) {
                    return SpellCastAuthority.ImpactDecision.rejected();
                }

                double poiseDamage = 0.0;
                if (context.targetSnapshot().poiseMax() > 0.0) {
                    poiseDamage = ProjectImpactTransaction.resolvePoise(
                            new ProjectImpactTransaction.PoiseRequest(
                                    context.targetSnapshot().poiseMax(),
                                    context.targetSnapshot().poiseMax(),
                                    context.sourceSnapshot().poiseOutputMultiplier(),
                                    spec.poiseCoefficient(),
                                    1.0,
                                    1.0
                            )
                    ).poiseDamage();
                }

                return SpellCastAuthority.ImpactDecision.accepted(
                        false,
                        damage.finalDamage(),
                        poiseDamage
                );
            };
        }
    }
}

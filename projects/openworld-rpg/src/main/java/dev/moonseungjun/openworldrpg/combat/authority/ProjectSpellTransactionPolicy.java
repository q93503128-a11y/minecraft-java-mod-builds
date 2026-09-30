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
    private final ManaCostAdjustment manaCostAdjustment;

    public ProjectSpellTransactionPolicy(
            ProjectSpellSpec spec,
            PlayerCombatStateStore states,
            SpellImpactPort impactPort
    ) {
        this(
                spec,
                states,
                impactPort,
                ManaCostAdjustment.none()
        );
    }

    public ProjectSpellTransactionPolicy(
            ProjectSpellSpec spec,
            PlayerCombatStateStore states,
            SpellImpactPort impactPort,
            ManaCostAdjustment manaCostAdjustment
    ) {
        this.spec = spec;
        this.states = states;
        this.impactPort = impactPort;
        this.manaCostAdjustment = manaCostAdjustment;
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
            double manaCost = adjustedManaCost(context);
            return !state.isCoolingDown(spec.id(), context.gameTick())
                    && state.canSpendMana(manaCost, context.gameTick())
                    && state.canSpendStamina(
                            spec.staminaCost(),
                            context.gameTick()
                    );
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
            double manaCost = adjustedManaCost(context);
            if (state.isCoolingDown(spec.id(), context.gameTick())
                    || !state.canSpendMana(
                            manaCost,
                            context.gameTick()
                    )
                    || !state.canSpendStamina(
                            spec.staminaCost(),
                            context.gameTick()
                    )) {
                return false;
            }

            if (manaCost > 0.0
                    && !state.spendMana(
                            manaCost,
                            context.gameTick()
                    )) {
                throw new IllegalStateException(
                        "Mana changed inside synchronized spell transaction."
                );
            }
            if (spec.staminaCost() > 0.0
                    && !state.spendStamina(
                            spec.staminaCost(),
                            0L,
                            context.gameTick()
                    )) {
                throw new IllegalStateException(
                        "Stamina changed inside synchronized spell transaction."
                );
            }

            if (spec.manaCost() > 0.0) {
                manaCostAdjustment.commit(context);
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

    private double adjustedManaCost(
            SpellCastAuthority.CastContext context
    ) {
        double multiplier =
                manaCostAdjustment.previewMultiplier(context);
        if (!Double.isFinite(multiplier)
                || multiplier <= 0.0
                || multiplier > 1.0) {
            throw new IllegalStateException(
                    "Mana-cost adjustment must be inside (0, 1]."
            );
        }
        return spec.manaCost() * multiplier;
    }

    public ProjectSpellSpec spec() {
        return spec;
    }

    public interface ManaCostAdjustment {
        double previewMultiplier(
                SpellCastAuthority.CastContext context
        );

        void commit(SpellCastAuthority.CastContext context);

        static ManaCostAdjustment none() {
            return new ManaCostAdjustment() {
                @Override
                public double previewMultiplier(
                        SpellCastAuthority.CastContext context
                ) {
                    return 1.0;
                }

                @Override
                public void commit(
                        SpellCastAuthority.CastContext context
                ) {
                }
            };
        }
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

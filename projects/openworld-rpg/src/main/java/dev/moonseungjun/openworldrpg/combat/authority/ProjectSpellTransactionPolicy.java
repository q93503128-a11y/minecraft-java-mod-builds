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
    }
}

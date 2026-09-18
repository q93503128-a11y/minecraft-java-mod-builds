package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatStateStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atomic server policy for one canonical project spell.
 *
 * <p>PRE checks are side-effect free. POST is used as the accepted-cast commit point because Spell
 * Engine reaches POST only after its own cooldown/ammo gates. Re-entry by the same instant cast in
 * the same server-tick window is allowed without spending a second time.</p>
 */
public final class ProjectSpellTransactionPolicy implements SpellCastAuthority.Policy {
    private final ProjectSpellSpec spec;
    private final PlayerCombatStateStore states;
    private final SpellImpactPort impactPort;
    private final Map<UUID, ActiveCast> activeCasts = new ConcurrentHashMap<>();

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
        ActiveCast active = activeCasts.get(context.playerId());
        if (isReentry(active, context)) {
            return true;
        }
        expireOldCast(context.playerId(), active, context.gameTick());

        return !state.isCoolingDown(spec.id(), context.gameTick())
                && state.canSpendMana(spec.manaCost(), context.gameTick());
    }

    @Override
    public boolean commitAcceptedCast(SpellCastAuthority.CastContext context) {
        PlayerCombatState state = states.getOrCreate(context.playerId(), context.gameTick());
        ActiveCast active = activeCasts.get(context.playerId());
        if (isReentry(active, context)) {
            return true;
        }
        expireOldCast(context.playerId(), active, context.gameTick());

        synchronized (state) {
            if (state.isCoolingDown(spec.id(), context.gameTick())
                    || !state.spendMana(spec.manaCost(), context.gameTick())) {
                return false;
            }
            state.startCooldown(spec.id(), spec.cooldownTicks(), context.gameTick());
            activeCasts.put(
                    context.playerId(),
                    new ActiveCast(spec.id(), context.gameTick(), context.gameTick() + spec.reentryWindowTicks())
            );
            return true;
        }
    }

    @Override
    public void onEngineCostConsumed(SpellCastAuthority.CastContext context) {
        requireCommittedCast(context.playerId(), context.spellId());
    }

    @Override
    public void onEngineCastCompleted(SpellCastAuthority.CastCompletion context) {
        ActiveCast active = requireCommittedCast(context.playerId(), context.spellId());
        activeCasts.remove(context.playerId(), active);
    }

    @Override
    public SpellCastAuthority.ImpactDecision onImpact(SpellCastAuthority.ImpactContext context) {
        requireCommittedCast(context.playerId(), context.spellId());
        return impactPort.apply(spec, context);
    }

    public ProjectSpellSpec spec() {
        return spec;
    }

    private boolean isReentry(ActiveCast active, SpellCastAuthority.CastContext context) {
        return active != null
                && active.spellId().equals(context.spellId())
                && context.gameTick() <= active.reentryUntilTick();
    }

    private void expireOldCast(UUID playerId, ActiveCast active, long nowTick) {
        if (active != null && nowTick > active.reentryUntilTick()) {
            activeCasts.remove(playerId, active);
        }
    }

    private ActiveCast requireCommittedCast(UUID playerId, String spellId) {
        ActiveCast active = activeCasts.get(playerId);
        if (active == null || !active.spellId().equals(spellId)) {
            throw new IllegalStateException(
                    "Spell Engine reached a committed project-spell stage without an active project transaction: "
                            + spellId
            );
        }
        return active;
    }

    private record ActiveCast(String spellId, long acceptedTick, long reentryUntilTick) {
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

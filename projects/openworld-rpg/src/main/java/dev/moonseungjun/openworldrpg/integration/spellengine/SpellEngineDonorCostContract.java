package dev.moonseungjun.openworldrpg.integration.spellengine;

/**
 * Safety contract for project-owned spells executed through Spell Engine.
 *
 * <p>Openworld RPG owns resource/cooldown decisions. Therefore project spell data must neutralize
 * Spell Engine's donor-side exhaustion, durability, ammo/item and cooldown costs. Batching is also
 * forbidden because it defers COST_CONSUME until after the project's accepted-cast token may have
 * completed.</p>
 */
public record SpellEngineDonorCostContract(
        boolean batching,
        double exhaust,
        int durability,
        boolean hasEffectCost,
        boolean hasItemCost,
        boolean hasCooldownGroup,
        double attemptCooldownSeconds,
        double cooldownSeconds
) {
    private static final double EPSILON = 1.0e-6;

    public boolean isNeutralForProjectAuthority() {
        return !batching
                && Math.abs(exhaust) <= EPSILON
                && durability == 0
                && !hasEffectCost
                && !hasItemCost
                && !hasCooldownGroup
                && Math.abs(attemptCooldownSeconds) <= EPSILON
                && Math.abs(cooldownSeconds) <= EPSILON;
    }
}

package dev.moonseungjun.openworldrpg.integration.spellengine;

/**
 * Project-owned input semantics layered over the pinned Spell Engine client hotbar.
 */
public final class ProjectSpellInputRules {
    public static final String ARC_BOLT_ID = "openworld_rpg:arc_bolt";

    private ProjectSpellInputRules() {
    }

    /**
     * Arc Bolt is one physical press -> one cast. Holding the same key must not auto-repeat it.
     *
     * @param spellId spell option id
     * @param freshPress true only when the Spell Engine START debounce has been released since
     *                   the previous accepted key-down
     */
    public static boolean suppressHeldRepeat(String spellId, boolean freshPress) {
        return ARC_BOLT_ID.equals(spellId) && !freshPress;
    }
}

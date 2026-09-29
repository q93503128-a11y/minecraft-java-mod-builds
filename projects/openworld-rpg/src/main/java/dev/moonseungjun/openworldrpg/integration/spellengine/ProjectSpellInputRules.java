package dev.moonseungjun.openworldrpg.integration.spellengine;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;

/**
 * Project-owned input semantics layered over the pinned Spell Engine client hotbar.
 */
public final class ProjectSpellInputRules {
    private ProjectSpellInputRules() {
    }

    /**
     * Project tap-cast skills require one physical press per cast. Holding the same key must not
     * auto-repeat after a completed cast.
     *
     * @param spellId spell option id
     * @param freshPress true only when the Spell Engine START debounce has been released since
     *                   the previous accepted key-down
     */
    public static boolean suppressHeldRepeat(String spellId, boolean freshPress) {
        return (ProjectSpellSpec.ARC_BOLT_ID.equals(spellId)
                || ProjectSpellSpec.RADIANT_LANCE_ID.equals(spellId)
                || ProjectSpellSpec.MEND_ID.equals(spellId)
                || ProjectSpellSpec.REBUKE_ID.equals(spellId)
                || ProjectSpellSpec.SANCTUARY_ID.equals(spellId))
                && !freshPress;
    }
}

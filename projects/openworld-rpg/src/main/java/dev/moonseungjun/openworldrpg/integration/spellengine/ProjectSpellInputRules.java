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
                || ProjectSpellSpec.CONSECRATED_GROUND_ID.equals(spellId)
                || ProjectSpellSpec.REBUKE_ID.equals(spellId)
                || ProjectSpellSpec.SANCTUARY_ID.equals(spellId)
                || ProjectSpellSpec.WARRIOR_DRIVING_SLASH_ID.equals(spellId)
                || ProjectSpellSpec.WARRIOR_IRON_COUNTER_ID.equals(spellId)
                || ProjectSpellSpec.WARRIOR_CYCLONE_CUT_ID.equals(spellId)
                || ProjectSpellSpec.WARRIOR_BREAKER_SLAM_ID.equals(spellId)
                || ProjectSpellSpec.WARRIOR_EARTHSHATTER_ID.equals(spellId)
                || ProjectSpellSpec.HUNTER_QUICKSTEP_VOLLEY_ID.equals(spellId)
                || ProjectSpellSpec.HUNTER_PINNING_SHOT_ID.equals(spellId)
                || ProjectSpellSpec.HUNTER_FAN_OF_ARROWS_ID.equals(spellId)
                || ProjectSpellSpec.HUNTER_POWER_SHOT_ID.equals(spellId)
                || ProjectSpellSpec.HUNTER_SKYFALL_ID.equals(spellId))
                && !freshPress;
    }
}

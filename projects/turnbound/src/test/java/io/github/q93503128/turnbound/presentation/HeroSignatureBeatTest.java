package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.combat.BattleEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeroSignatureBeatTest {
    @Test
    void reactionDetailsResolveToTheirAuthoredPayoffFamilies() {
        assertEquals(HeroSignatureBeat.Kind.KYREN_FOLLOWUP,
                HeroSignatureBeat.resolve(new BattleEvent("REACTION_DAMAGE", "k", "e", 10, "P01_FOCUS_FOLLOWUP")));
        assertEquals(HeroSignatureBeat.Kind.BRAM_REDIRECT_COUNTER,
                HeroSignatureBeat.resolve(new BattleEvent("REACTION_DAMAGE", "b", "e", 10, "P03_REDIRECT_COUNTER")));
        assertEquals(HeroSignatureBeat.Kind.LYNETTE_CROSS_SHOT,
                HeroSignatureBeat.resolve(new BattleEvent("REACTION_DAMAGE", "l", "e", 10, "P05_CROSS_SHOT")));
        assertEquals(HeroSignatureBeat.Kind.MARION_JOINT_STRIKE,
                HeroSignatureBeat.resolve(new BattleEvent("REACTION_DAMAGE", "toto", "e", 10, "P07_JOINT")));
        assertEquals(HeroSignatureBeat.Kind.RAZE_HIGH_FURY,
                HeroSignatureBeat.resolve(new BattleEvent("REACTION_DAMAGE", "r", "e", 10, "P08_HIGH_FURY")));
    }

    @Test
    void nonDamagePayoffsUseAuthoritativeEventTypesToo() {
        assertEquals(HeroSignatureBeat.Kind.ELYSIA_SANCTUARY,
                HeroSignatureBeat.resolve(new BattleEvent("REACTION_HEAL", "e", "a", 20, "P04_SANCTUARY")));
        assertEquals(HeroSignatureBeat.Kind.MORWEN_RECORD_SPEND,
                HeroSignatureBeat.resolve(new BattleEvent("RESOURCE", "m", "m", -3, "P06_RECORD_SPEND")));
        assertEquals(HeroSignatureBeat.Kind.MORWEN_LAST_PAGE,
                HeroSignatureBeat.resolve(new BattleEvent("SELF_REVIVE", "m", "m", 100, "P06_LAST_PAGE")));
        assertEquals(HeroSignatureBeat.Kind.RAZE_OVERHEAT,
                HeroSignatureBeat.resolve(new BattleEvent("RESOURCE", "r", "r", -60, "P08_OVERHEAT")));
        assertNull(HeroSignatureBeat.resolve(new BattleEvent("DAMAGE", "x", "y", 5, "ordinary")));
    }
}

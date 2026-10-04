package dev.moonseungjun.openworldrpg.progression.r01;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01ClassInsightRulesTest {
    private static final UUID REGALHART = UUID.fromString(
            "11111111-1111-1111-1111-111111111111"
    );
    private static final UUID OTHER = UUID.fromString(
            "22222222-2222-2222-2222-222222222222"
    );

    @Test
    void crownedMarkRequiresFullFocusWeakPointOnTheCastTimeQuarry() {
        assertTrue(R01ClassInsightRules.hunterCrownedMarkQualifies(
                R01ExternalActorCatalog.REGALHART,
                true,
                REGALHART,
                REGALHART,
                true,
                true
        ));

        assertFalse(R01ClassInsightRules.hunterCrownedMarkQualifies(
                R01ExternalActorCatalog.REGALHART,
                true,
                REGALHART,
                null,
                true,
                true
        ));
        assertFalse(R01ClassInsightRules.hunterCrownedMarkQualifies(
                R01ExternalActorCatalog.REGALHART,
                true,
                REGALHART,
                OTHER,
                true,
                true
        ));
        assertFalse(R01ClassInsightRules.hunterCrownedMarkQualifies(
                R01ExternalActorCatalog.REGALHART,
                true,
                REGALHART,
                REGALHART,
                false,
                true
        ));
        assertFalse(R01ClassInsightRules.hunterCrownedMarkQualifies(
                R01ExternalActorCatalog.REGALHART,
                true,
                REGALHART,
                REGALHART,
                true,
                false
        ));
    }

    @Test
    void crownedMarkRejectsDonorOrWrongActorEvenWithMatchingCombatFlags() {
        assertFalse(R01ClassInsightRules.hunterCrownedMarkQualifies(
                R01ExternalActorCatalog.REGALHART,
                false,
                REGALHART,
                REGALHART,
                true,
                true
        ));
        assertFalse(R01ClassInsightRules.hunterCrownedMarkQualifies(
                R01ExternalActorCatalog.STEELBOAR,
                true,
                REGALHART,
                REGALHART,
                true,
                true
        ));
    }
}

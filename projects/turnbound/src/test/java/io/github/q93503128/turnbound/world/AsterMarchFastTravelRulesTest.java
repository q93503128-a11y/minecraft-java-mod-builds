package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class AsterMarchFastTravelRulesTest {
    private static AsterMarchFastTravelRules.Progress progress(Set<String> quests, Set<String> flags, Set<String> clears) {
        return new AsterMarchFastTravelRules.Progress(quests, flags, clears);
    }

    @Test
    void radiaIsAlwaysAvailableAndUnknownIdsAreNotCanonical() {
        var empty = progress(Set.of(), Set.of(), Set.of());
        assertTrue(AsterMarchFastTravelRules.canonicalDestination(AsterMarchRegionCatalog.FT_RADIA));
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_RADIA, empty));
        assertFalse(AsterMarchFastTravelRules.canonicalDestination("START_VILLAGE"));
        assertFalse(AsterMarchFastTravelRules.unlocked("START_VILLAGE", empty));
    }

    @Test
    void meadowActivatesFromAuthoredPatrolMilestoneOrEquivalentPersistedState() {
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_MEADOW,
                progress(Set.of("MQ_C01_01_patrol"), Set.of(), Set.of())));
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_MEADOW,
                progress(Set.of(), Set.of("FT_MEADOW"), Set.of())));
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_MEADOW,
                progress(Set.of(), Set.of(), Set.of("ENC_M01", "ENC_M02"))));
        assertFalse(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_MEADOW,
                progress(Set.of(), Set.of(), Set.of("ENC_M01"))));
    }

    @Test
    void regionalRelayActivationMirrorsExistingQuestAndLocalGateMilestones() {
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_GLOAM,
                progress(Set.of("MQ_C02_01_spores"), Set.of(), Set.of())));
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_GLOAM,
                progress(Set.of(), Set.of("GLOAM_DEEP_PATH"), Set.of())));

        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_AQUEDUCT,
                progress(Set.of("MQ_C03_01_dry_channel"), Set.of(), Set.of())));
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_AQUEDUCT,
                progress(Set.of(), Set.of("AQUEDUCT_LOWER"), Set.of())));

        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_QUARRY,
                progress(Set.of("MQ_C04_01_ash_route"), Set.of(), Set.of())));
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_QUARRY,
                progress(Set.of(), Set.of("FT_QUARRY"), Set.of())));

        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_RELAY,
                progress(Set.of("MQ_C05_01_relay_key"), Set.of(), Set.of())));
        assertTrue(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_RELAY,
                progress(Set.of(), Set.of("OLD_RELAY_ENTRANCE"), Set.of())));
    }

    @Test
    void futureRegionalRelaysRemainLockedWithoutTheirActivationMilestone() {
        var empty = progress(Set.of(), Set.of(), Set.of());
        assertFalse(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_MEADOW, empty));
        assertFalse(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_GLOAM, empty));
        assertFalse(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_AQUEDUCT, empty));
        assertFalse(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_QUARRY, empty));
        assertFalse(AsterMarchFastTravelRules.unlocked(AsterMarchRegionCatalog.FT_RELAY, empty));
    }
}

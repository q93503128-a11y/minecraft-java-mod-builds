package dev.moonseungjun.openworldrpg.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class R01AlderfordNpcPresenceRulesTest {
    @Test
    void minecraftDayTicksMapToClosedWorldTimeBands() {
        assertEquals(
                R01AlderfordNpcPresenceRules.TimeBand.MORNING,
                R01AlderfordNpcPresenceRules.timeBand(0)
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.TimeBand.DAY,
                R01AlderfordNpcPresenceRules.timeBand(4000)
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.TimeBand.EVENING,
                R01AlderfordNpcPresenceRules.timeBand(12000)
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.TimeBand.NIGHT,
                R01AlderfordNpcPresenceRules.timeBand(16000)
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.TimeBand.MORNING,
                R01AlderfordNpcPresenceRules.timeBand(24000)
        );
    }

    @Test
    void primaryServicesExistOnlyWhereCanonActuallyDefinesThem() {
        assertEquals(
                "openworld_rpg:service/alderford/holt_forge",
                R01AlderfordNpcPresenceRules.primaryServiceId(
                        R01AlderfordNpcPresenceRules.DAREN_HOLT
                ).orElseThrow()
        );
        assertEquals(
                "openworld_rpg:service/alderford/route_board",
                R01AlderfordNpcPresenceRules.primaryServiceId(
                        R01AlderfordNpcPresenceRules.SERA_WREN
                ).orElseThrow()
        );
        assertTrue(
                R01AlderfordNpcPresenceRules.primaryServiceId(
                        R01AlderfordNpcPresenceRules.MARA_VENN
                ).isEmpty()
        );
        assertTrue(
                R01AlderfordNpcPresenceRules.primaryServiceId(
                        R01AlderfordNpcPresenceRules.ILYAN_VOSS
                ).isEmpty()
        );
        assertTrue(
                R01AlderfordNpcPresenceRules.primaryServiceId(
                        R01AlderfordNpcPresenceRules.KEST_ARDEN
                ).isEmpty()
        );
    }

    @Test
    void ambiguousAmbientCanonStaysSemanticInsteadOfInventingCoordinates() {
        assertEquals(
                R01AlderfordNpcPresenceRules.AmbientSlot
                        .DAREN_EVENING_FORGE_OR_KETTLE,
                R01AlderfordNpcPresenceRules.ambientSlot(
                        R01AlderfordNpcPresenceRules.DAREN_HOLT,
                        R01AlderfordNpcPresenceRules.TimeBand.EVENING,
                        false
                )
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.AmbientSlot
                        .MARA_EVENING_NIGHT_INTERIOR,
                R01AlderfordNpcPresenceRules.ambientSlot(
                        R01AlderfordNpcPresenceRules.MARA_VENN,
                        R01AlderfordNpcPresenceRules.TimeBand.NIGHT,
                        false
                )
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.AmbientSlot
                        .UNSPECIFIED_POST_CLEAR_AMBIENT,
                R01AlderfordNpcPresenceRules.ambientSlot(
                        R01AlderfordNpcPresenceRules.ILYAN_VOSS,
                        R01AlderfordNpcPresenceRules.TimeBand.MORNING,
                        true
                )
        );
    }

    @Test
    void serviceRelevanceLatchesForExactlyTwentySecondsAfterPlayersLeave() {
        var initial = R01AlderfordNpcPresenceRules.PresenceLatch.initial();
        var active = R01AlderfordNpcPresenceRules.updateNamedPresence(
                initial,
                context(
                        R01AlderfordNpcPresenceRules.DAREN_HOLT,
                        1000L,
                        true,
                        false,
                        false
                )
        );

        assertEquals(
                R01AlderfordNpcPresenceRules.PresenceMode.PRIMARY_SERVICE,
                active.mode()
        );
        assertEquals(1400L, active.state().releaseAtTick());

        var grace = R01AlderfordNpcPresenceRules.updateNamedPresence(
                active.state(),
                context(
                        R01AlderfordNpcPresenceRules.DAREN_HOLT,
                        1399L,
                        false,
                        false,
                        false
                )
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.PresenceMode.PRIMARY_SERVICE,
                grace.mode()
        );

        var released = R01AlderfordNpcPresenceRules.updateNamedPresence(
                grace.state(),
                context(
                        R01AlderfordNpcPresenceRules.DAREN_HOLT,
                        1400L,
                        false,
                        false,
                        false
                )
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.PresenceMode.AMBIENT_SCHEDULE,
                released.mode()
        );
    }

    @Test
    void questTurnInForcesSeraToServiceAndStorySceneTemporarilyWins() {
        var quest = R01AlderfordNpcPresenceRules.updateNamedPresence(
                R01AlderfordNpcPresenceRules.PresenceLatch.initial(),
                context(
                        R01AlderfordNpcPresenceRules.SERA_WREN,
                        2000L,
                        false,
                        true,
                        false
                )
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.PresenceMode.PRIMARY_SERVICE,
                quest.mode()
        );

        var scene = R01AlderfordNpcPresenceRules.updateNamedPresence(
                quest.state(),
                context(
                        R01AlderfordNpcPresenceRules.SERA_WREN,
                        2010L,
                        true,
                        true,
                        true
                )
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.PresenceMode.STORY_OR_EVENT_SCENE,
                scene.mode()
        );

        var resume = R01AlderfordNpcPresenceRules.updateNamedPresence(
                scene.state(),
                context(
                        R01AlderfordNpcPresenceRules.SERA_WREN,
                        2011L,
                        true,
                        true,
                        false
                )
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.PresenceMode.PRIMARY_SERVICE,
                resume.mode()
        );
    }

    @Test
    void distanceBoundaryIsTwelveBlocksInclusive() {
        assertTrue(
                R01AlderfordNpcPresenceRules
                        .insideServiceRelevanceRadius(144.0)
        );
        assertFalse(
                R01AlderfordNpcPresenceRules
                        .insideServiceRelevanceRadius(144.0001)
        );
    }

    @Test
    void corePathfinderCapProtectsRequiredNamedThenGuardsThenAmbient() {
        List<R01AlderfordNpcPresenceRules.PathfinderCandidate> candidates =
                new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            candidates.add(new R01AlderfordNpcPresenceRules.PathfinderCandidate(
                    "required_" + i,
                    R01AlderfordNpcPresenceRules.ActivationPriority.REQUIRED_NAMED
            ));
        }
        for (int i = 0; i < 2; i++) {
            candidates.add(new R01AlderfordNpcPresenceRules.PathfinderCandidate(
                    "guard_" + i,
                    R01AlderfordNpcPresenceRules.ActivationPriority.WATCH_GUARD
            ));
        }
        for (int i = 0; i < 8; i++) {
            candidates.add(new R01AlderfordNpcPresenceRules.PathfinderCandidate(
                    "named_" + i,
                    R01AlderfordNpcPresenceRules.ActivationPriority.NAMED_AMBIENT
            ));
        }
        for (int i = 0; i < 4; i++) {
            candidates.add(new R01AlderfordNpcPresenceRules.PathfinderCandidate(
                    "town_" + i,
                    R01AlderfordNpcPresenceRules.ActivationPriority.UNNAMED_TOWNSFOLK
            ));
        }

        var selected =
                R01AlderfordNpcPresenceRules.selectCorePathfinders(candidates);

        assertEquals(15, selected.size());
        assertTrue(
                selected.stream().anyMatch(value ->
                        value.actorId().equals("required_0"))
        );
        assertTrue(
                selected.stream().anyMatch(value ->
                        value.actorId().equals("guard_1"))
        );
        assertTrue(
                selected.stream().anyMatch(value ->
                        value.actorId().equals("named_7"))
        );
        assertTrue(
                selected.stream().anyMatch(value ->
                        value.actorId().equals("town_0"))
        );
        assertTrue(
                selected.stream().anyMatch(value ->
                        value.actorId().equals("town_1"))
        );
        assertFalse(
                selected.stream().anyMatch(value ->
                        value.actorId().equals("town_2"))
        );
        assertFalse(
                selected.stream().anyMatch(value ->
                        value.actorId().equals("town_3"))
        );
    }

    @Test
    void kestNeverBecomesGenericAlderfordAmbientNpc() {
        var update = R01AlderfordNpcPresenceRules.updateNamedPresence(
                R01AlderfordNpcPresenceRules.PresenceLatch.initial(),
                context(
                        R01AlderfordNpcPresenceRules.KEST_ARDEN,
                        10L,
                        false,
                        false,
                        false
                )
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.PresenceMode.EVENT_OWNED_ONLY,
                update.mode()
        );
        assertEquals(
                R01AlderfordNpcPresenceRules.ActivationPriority.INACTIVE,
                update.priority()
        );
    }

    private static R01AlderfordNpcPresenceRules.PresenceContext context(
            String npcId,
            long tick,
            boolean insideRadius,
            boolean questTurnIn,
            boolean scene
    ) {
        return new R01AlderfordNpcPresenceRules.PresenceContext(
                npcId,
                tick,
                R01AlderfordNpcPresenceRules.TimeBand.DAY,
                false,
                true,
                insideRadius,
                questTurnIn,
                scene
        );
    }
}

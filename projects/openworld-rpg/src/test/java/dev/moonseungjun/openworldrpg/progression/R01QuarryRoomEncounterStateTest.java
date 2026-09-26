package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterState;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRunContribution;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01QuarryRoomEncounterStateTest {
    private static final String ENCOUNTER_ID =
            "openworld_rpg:r01/quarry/test_run";
    private static final String PLAYER_ID =
            UUID.fromString("11111111-2222-3333-4444-555555555555").toString();

    @Test
    void upperGalleryStaggersSecondWaveAndCapsFourPlayerRoomAtSeven() {
        var state = R01QuarryRoomEncounterState.initial()
                .beginRoom(
                        ENCOUNTER_ID,
                        7L,
                        R01QuarryRoomEncounterRules.RoomId.UPPER_GALLERY,
                        4
                );

        var active = state.run(ENCOUNTER_ID).orElseThrow()
                .activeRoom().orElseThrow();
        assertEquals(2, active.spawnedEnemies());
        assertEquals(2, active.remainingEnemies());
        assertFalse(active.secondWaveActivated());

        state = state.recordEnemyDefeat(ENCOUNTER_ID, 2);
        assertTrue(state.run(ENCOUNTER_ID).orElseThrow()
                .activeRoom().isPresent());

        state = state.activateUpperGallerySecondWave(
                ENCOUNTER_ID,
                4
        );
        active = state.run(ENCOUNTER_ID).orElseThrow()
                .activeRoom().orElseThrow();
        assertEquals(7, active.spawnedEnemies());
        assertEquals(5, active.remainingEnemies());

        state = state.recordEnemyDefeat(ENCOUNTER_ID, 5);
        var run = state.run(ENCOUNTER_ID).orElseThrow();
        assertTrue(run.activeRoom().isEmpty());
        assertTrue(run.clearedRooms().contains(
                R01QuarryRoomEncounterRules.RoomId.UPPER_GALLERY
        ));
    }

    @Test
    void canonicalRoomSequenceAndCoopCountsAreEnforced() {
        var initial = R01QuarryRoomEncounterState.initial();
        assertThrows(
                IllegalStateException.class,
                () -> initial.beginRoom(
                        ENCOUNTER_ID,
                        1L,
                        R01QuarryRoomEncounterRules.RoomId.COLLAPSED_HOIST,
                        1
                )
        );

        var state = initial.beginRoom(
                ENCOUNTER_ID,
                1L,
                R01QuarryRoomEncounterRules.RoomId.UPPER_GALLERY,
                1
        );
        state = state.activateUpperGallerySecondWave(ENCOUNTER_ID, 1);
        state = state.recordEnemyDefeat(ENCOUNTER_ID, 4);

        state = state.beginRoom(
                ENCOUNTER_ID,
                1L,
                R01QuarryRoomEncounterRules.RoomId.COLLAPSED_HOIST,
                4
        );
        var hoist = state.run(ENCOUNTER_ID).orElseThrow()
                .activeRoom().orElseThrow();
        assertEquals(6, hoist.spawnedEnemies());

        state = state.recordEnemyDefeat(ENCOUNTER_ID, 6);
        state = state.beginRoom(
                ENCOUNTER_ID,
                1L,
                R01QuarryRoomEncounterRules.RoomId.ROOT_BREACHED,
                4
        );
        var rootPlan = R01QuarryRoomEncounterRules.initialPlan(
                R01QuarryRoomEncounterRules.RoomId.ROOT_BREACHED,
                4
        );
        assertEquals(1, rootPlan.actorCount());
        assertEquals(2.95, rootPlan.hpScale(), 0.0001);
        assertEquals(2.20, rootPlan.poiseScale(), 0.0001);
    }

    @Test
    void roomCompletionQueuesOneCapturedClassVoteAndCodecPersistsIt() {
        var state = R01QuarryRoomEncounterState.initial()
                .beginRoom(
                        ENCOUNTER_ID,
                        9L,
                        R01QuarryRoomEncounterRules.RoomId.UPPER_GALLERY,
                        1
                )
                .recordParticipation(
                        ENCOUNTER_ID,
                        PLAYER_ID,
                        RootClass.HUNTER
                )
                .recordParticipation(
                        ENCOUNTER_ID,
                        PLAYER_ID,
                        RootClass.MAGE
                )
                .activateUpperGallerySecondWave(ENCOUNTER_ID, 1)
                .recordEnemyDefeat(ENCOUNTER_ID, 4);

        var pending = state.pendingFor(PLAYER_ID);
        assertEquals(1, pending.size());
        assertEquals(
                R01QuarryRunContribution.UPPER_GALLERY_COMBAT,
                pending.getFirst().contribution()
        );
        assertEquals(RootClass.HUNTER, pending.getFirst().owner());

        var encoded = R01QuarryRoomEncounterState.CODEC
                .encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow();
        var decoded = R01QuarryRoomEncounterState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(state, decoded);
    }

    @Test
    void resetOnlyDropsCurrentUnclearedRoomAndPreservesEarlierClear() {
        var state = R01QuarryRoomEncounterState.initial()
                .beginRoom(
                        ENCOUNTER_ID,
                        3L,
                        R01QuarryRoomEncounterRules.RoomId.UPPER_GALLERY,
                        1
                )
                .activateUpperGallerySecondWave(ENCOUNTER_ID, 1)
                .recordEnemyDefeat(ENCOUNTER_ID, 4)
                .beginRoom(
                        ENCOUNTER_ID,
                        3L,
                        R01QuarryRoomEncounterRules.RoomId.COLLAPSED_HOIST,
                        1
                );

        state = state.resetActiveRoom(ENCOUNTER_ID);
        var run = state.run(ENCOUNTER_ID).orElseThrow();
        assertTrue(run.activeRoom().isEmpty());
        assertTrue(run.clearedRooms().contains(
                R01QuarryRoomEncounterRules.RoomId.UPPER_GALLERY
        ));
        assertFalse(run.clearedRooms().contains(
                R01QuarryRoomEncounterRules.RoomId.COLLAPSED_HOIST
        ));
    }
}

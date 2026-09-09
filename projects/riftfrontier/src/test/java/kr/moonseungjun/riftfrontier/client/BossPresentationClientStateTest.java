package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossPresentationClientStateTest {
    private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @AfterEach
    void clear() {
        BossPresentationClientState.clearAll();
    }

    @Test
    void sameActorRejectsOlderServerTick() {
        assertTrue(BossPresentationClientState.accept(active(7, FIRST, 20L, 0.75D)));
        assertFalse(BossPresentationClientState.accept(active(7, FIRST, 19L, 0.25D)));
        assertEquals(0.75D, BossPresentationClientState.current(7, FIRST).orElseThrow().phaseProgress());
    }

    @Test
    void reusedNumericIdCannotExposePreviousActorPresentation() {
        assertTrue(BossPresentationClientState.accept(active(7, FIRST, 20L, 0.75D)));

        assertTrue(BossPresentationClientState.current(7, SECOND).isEmpty());
        assertEquals(FIRST, BossPresentationClientState.current(7, FIRST).orElseThrow().entityUuid());

        assertTrue(BossPresentationClientState.accept(active(7, SECOND, 3L, 0.25D)));
        assertTrue(BossPresentationClientState.current(7, FIRST).isEmpty());
        assertEquals(SECOND, BossPresentationClientState.current(7, SECOND).orElseThrow().entityUuid());
        assertEquals(0.25D, BossPresentationClientState.current(7, SECOND).orElseThrow().phaseProgress());
    }

    @Test
    void clearWatermarkOnlyAppliesToTheSameActorUuid() {
        assertTrue(BossPresentationClientState.accept(active(4, FIRST, 30L, 0.5D)));
        assertTrue(BossPresentationClientState.accept(BossPresentationSemanticState.clear(4, FIRST, 31L)));
        assertFalse(BossPresentationClientState.accept(active(4, FIRST, 30L, 0.8D)));
        assertTrue(BossPresentationClientState.current(4, FIRST).isEmpty());

        assertTrue(BossPresentationClientState.accept(active(4, SECOND, 1L, 0.2D)));
        assertEquals(SECOND, BossPresentationClientState.current(4, SECOND).orElseThrow().entityUuid());
    }

    private static BossPresentationSemanticState active(int entityId, UUID uuid, long tick, double progress) {
        return new BossPresentationSemanticState(
            entityId,
            uuid,
            tick,
            true,
            1,
            "riftfrontier:attack/test",
            AttackTimeline.Phase.TELEGRAPH.name(),
            progress,
            "test_cue",
            "melee",
            List.of("dodge"),
            false
        );
    }
}

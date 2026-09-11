package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
    void sameTickCannotRewriteAuthoritativePresentation() {
        BossPresentationSemanticState first = active(7, FIRST, 20L, 0.25D);
        assertTrue(BossPresentationClientState.accept(first));

        assertFalse(BossPresentationClientState.accept(first));
        assertFalse(BossPresentationClientState.accept(active(7, FIRST, 20L, 0.75D)));
        assertFalse(BossPresentationClientState.accept(BossPresentationSemanticState.clear(7, FIRST, 20L)));

        BossPresentationSemanticState retained = BossPresentationClientState.current(7, FIRST).orElseThrow();
        assertEquals(20L, retained.serverGameTick());
        assertEquals(0.25D, retained.phaseProgress());
        assertTrue(retained.active());
    }

    @Test
    void sameTickCannotReactivateAnAuthoritativeClear() {
        assertTrue(BossPresentationClientState.accept(active(4, FIRST, 30L, 0.5D)));
        assertTrue(BossPresentationClientState.accept(BossPresentationSemanticState.clear(4, FIRST, 31L)));

        assertFalse(BossPresentationClientState.accept(active(4, FIRST, 31L, 0.8D)));
        assertTrue(BossPresentationClientState.current(4, FIRST).isEmpty());
    }

    @Test
    void reusedNumericIdCannotExposePreviousActorPresentation() {
        assertTrue(BossPresentationClientState.accept(active(7, FIRST, 20L, 0.75D)));

        assertTrue(BossPresentationClientState.current(7, SECOND).isEmpty());
        assertEquals(FIRST, BossPresentationClientState.current(7, FIRST).orElseThrow().entityUuid());

        // A different UUID is a different logical actor and may come from a different level-time epoch.
        assertTrue(BossPresentationClientState.accept(active(7, SECOND, 3L, 0.25D)));
        assertTrue(BossPresentationClientState.current(7, FIRST).isEmpty());
        assertEquals(SECOND, BossPresentationClientState.current(7, SECOND).orElseThrow().entityUuid());
        assertEquals(0.25D, BossPresentationClientState.current(7, SECOND).orElseThrow().phaseProgress());
    }

    @Test
    void numericOnlyLookupIsNotExposedByClientCache() {
        assertFalse(Arrays.stream(BossPresentationClientState.class.getDeclaredMethods()).anyMatch(method ->
            method.getName().equals("current")
                && Arrays.equals(method.getParameterTypes(), new Class<?>[]{int.class})
        ));
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

    @Test
    void forgettingActorRetainsWatermarkAndRejectsDelayedPreLeavePackets() {
        assertTrue(BossPresentationClientState.accept(active(7, FIRST, 50L, 0.6D)));
        assertTrue(BossPresentationClientState.forgetActor(7, FIRST));
        assertTrue(BossPresentationClientState.current(7, FIRST).isEmpty());
        assertFalse(BossPresentationClientState.forgetActor(7, FIRST));

        // A packet sampled before or at the leave watermark cannot resurrect presentation afterward.
        assertFalse(BossPresentationClientState.accept(active(7, FIRST, 49L, 0.1D)));
        assertFalse(BossPresentationClientState.accept(active(7, FIRST, 50L, 0.1D)));
        assertTrue(BossPresentationClientState.current(7, FIRST).isEmpty());

        // The same UUID may become present again only when authoritative server time advances.
        assertTrue(BossPresentationClientState.accept(active(7, FIRST, 51L, 0.2D)));
        assertEquals(51L, BossPresentationClientState.current(7, FIRST).orElseThrow().serverGameTick());
    }

    @Test
    void staleLeaveForOldUuidCannotEraseReusedNumericId() {
        assertTrue(BossPresentationClientState.accept(active(7, FIRST, 50L, 0.6D)));
        assertTrue(BossPresentationClientState.accept(active(7, SECOND, 3L, 0.2D)));

        assertFalse(BossPresentationClientState.forgetActor(7, FIRST));
        BossPresentationSemanticState retained = BossPresentationClientState.current(7, SECOND).orElseThrow();
        assertEquals(SECOND, retained.entityUuid());
        assertEquals(3L, retained.serverGameTick());
    }

    @Test
    void forgettingActorPreservesExistingClearWatermark() {
        assertTrue(BossPresentationClientState.accept(active(4, FIRST, 30L, 0.5D)));
        assertTrue(BossPresentationClientState.accept(BossPresentationSemanticState.clear(4, FIRST, 31L)));
        assertTrue(BossPresentationClientState.current(4, FIRST).isEmpty());

        // The actor is already semantically retired, so a duplicate lifecycle callback is a no-op.
        assertFalse(BossPresentationClientState.forgetActor(4, FIRST));
        assertFalse(BossPresentationClientState.accept(active(4, FIRST, 31L, 0.2D)));
        assertTrue(BossPresentationClientState.accept(active(4, FIRST, 32L, 0.2D)));
        assertEquals(32L, BossPresentationClientState.current(4, FIRST).orElseThrow().serverGameTick());
    }

    @Test
    void clearAllRetiresLeaveTombstonesForANewConnectionEpoch() {
        assertTrue(BossPresentationClientState.accept(active(7, FIRST, 50L, 0.6D)));
        assertTrue(BossPresentationClientState.forgetActor(7, FIRST));
        assertFalse(BossPresentationClientState.accept(active(7, FIRST, 2L, 0.1D)));

        BossPresentationClientState.clearAll();
        assertTrue(BossPresentationClientState.accept(active(7, FIRST, 2L, 0.1D)));
        assertEquals(2L, BossPresentationClientState.current(7, FIRST).orElseThrow().serverGameTick());
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

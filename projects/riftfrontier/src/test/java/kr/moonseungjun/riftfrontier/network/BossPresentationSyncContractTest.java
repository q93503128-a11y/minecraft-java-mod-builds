package kr.moonseungjun.riftfrontier.network;

import kr.moonseungjun.riftfrontier.client.BossPresentationClientState;
import kr.moonseungjun.riftfrontier.combat.AttackExecution;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class BossPresentationSyncContractTest {
    @AfterEach
    void clearClientState() {
        BossPresentationClientState.clearAll();
    }

    @Test
    void payloadCopiesOnlyAuthoritativePresentationSemantics() {
        var pattern = new CoreDefinition.AttackPattern(
            ContentId.parse("riftfrontier:sync_contract"),
            "line_charge",
            4,
            3,
            5,
            Set.of("interrupt", "sidestep"),
            "lowered_charge"
        );
        var frame = MinecraftBossCombatAdapter.PresentationFrame.from(2, AttackExecution.start(pattern, 100L).sample(105L));
        var payload = BossPresentationPayload.fromFrame(77, 105L, frame);

        assertTrue(payload.active());
        assertEquals(77, payload.entityId());
        assertEquals(105L, payload.serverGameTick());
        assertEquals(frame.bossPhase(), payload.bossPhase());
        assertEquals(frame.patternId().toString(), payload.patternId());
        assertEquals(frame.attackPhase().name(), payload.attackPhase());
        assertEquals(frame.phaseProgress(), payload.phaseProgress(), 0.000001D);
        assertEquals(frame.presentationCue(), payload.presentationCue());
        assertEquals(frame.delivery(), payload.delivery());
        assertEquals(frame.counterplay().stream().sorted().toList(), payload.counterplay());
        assertEquals(frame.hitWindowOpen(), payload.hitWindowOpen());
    }

    @Test
    void delayedPayloadCannotRewindClientPresentation() {
        var newest = active(12, 220L, "ACTIVE", true);
        var older = active(12, 219L, "TELEGRAPH", false);

        assertTrue(BossPresentationClientState.accept(newest));
        assertFalse(BossPresentationClientState.accept(older));
        assertEquals(newest, BossPresentationClientState.current(12).orElseThrow());
    }

    @Test
    void authoritativeClearRejectsOlderReactivation() {
        var active = active(8, 300L, "ACTIVE", true);
        var clear = BossPresentationPayload.clear(8, 301L);
        var delayed = active(8, 300L, "RECOVERY", false);

        assertTrue(BossPresentationClientState.accept(active));
        assertTrue(BossPresentationClientState.accept(clear));
        assertTrue(BossPresentationClientState.current(8).isEmpty());
        assertFalse(BossPresentationClientState.accept(delayed));
        assertTrue(BossPresentationClientState.current(8).isEmpty());
    }

    @Test
    void clearingConnectionStateAllowsNewServerTickEpoch() {
        var oldSession = active(5, 900L, "ACTIVE", true);
        var newSession = active(5, 12L, "TELEGRAPH", false);
        assertTrue(BossPresentationClientState.accept(oldSession));
        BossPresentationClientState.clearAll();
        assertTrue(BossPresentationClientState.accept(newSession));
        assertEquals(newSession, BossPresentationClientState.current(5).orElseThrow());
    }

    @Test
    void payloadRejectsImpossibleHitWindowSemantics() {
        assertThrows(IllegalArgumentException.class, () -> active(4, 10L, "TELEGRAPH", true));
        assertThrows(IllegalArgumentException.class, () -> active(4, 10L, "ACTIVE", false));
    }

    private static BossPresentationPayload active(int entityId, long tick, String phase, boolean hitWindowOpen) {
        return new BossPresentationPayload(
            entityId,
            tick,
            true,
            1,
            "riftfrontier:sync_contract",
            phase,
            0.5D,
            "cue",
            "line_charge",
            java.util.List.of("sidestep"),
            hitWindowOpen
        );
    }
}

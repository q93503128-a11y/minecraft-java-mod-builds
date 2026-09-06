package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.StatusDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class M1StatusRuntimeTest {
    @Test void sharedRuntimeTracksCanonicalSingleStatuses() {
        var runtime = new StatusRuntime();

        StatusService.applySingle(runtime, StatusService.GUARD);
        assertTrue(runtime.has(StatusService.GUARD));
        assertEquals(1, runtime.stacks(StatusService.GUARD));

        assertTrue(StatusService.remove(runtime, StatusService.GUARD));
        assertFalse(runtime.has(StatusService.GUARD));
        assertFalse(StatusService.remove(runtime, StatusService.GUARD));
    }

    @Test void dataDefinedStackCountRespectsDefinitionMaximumWithoutInventingRefreshRules() {
        var runtime = new StatusRuntime();
        var burn = new StatusDefinition("BURN", "NEGATIVE", "TURN", 3, "REFRESH", List.of("DEBUFF"), List.of("TURN_END"));

        StatusService.apply(runtime, burn, 2);
        assertEquals(2, runtime.stacks("BURN"));
        assertThrows(IllegalArgumentException.class, () -> StatusService.apply(runtime, burn, 4));
    }

    @Test void participantLegacyAccessorsAreBackedBySharedRuntime() {
        var participant = new BattleParticipant("p", BattleTeam.PLAYER, 0, 10, 100, 100, 100, 10);
        var state = new ParticipantCombatState(participant);

        state.setGuard(true);
        assertTrue(state.guard());
        assertTrue(state.statuses().has(StatusService.GUARD));

        assertTrue(state.applyPoiseDamage(10));
        assertTrue(state.exposed());
        assertTrue(state.statuses().has(StatusService.EXPOSED));

        assertTrue(state.recoverAtTurnStartIfExposed());
        assertFalse(state.exposed());
        assertTrue(state.poiseGuard());
        assertTrue(state.statuses().has(StatusService.POISE_GUARD));

        assertTrue(state.expirePoiseGuardAtTurnStart());
        assertFalse(state.poiseGuard());
    }
}

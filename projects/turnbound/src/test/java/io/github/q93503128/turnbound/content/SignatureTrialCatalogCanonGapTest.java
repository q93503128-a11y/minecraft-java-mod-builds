package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SignatureTrialCatalogCanonGapTest {
    @Test
    void identityDependentTrialsStayDistinctFromRosterOnlyTrials() {
        assertEquals(SignatureTrialCatalog.GapKind.SPECIAL_ELITE_IDENTITY,
                SignatureTrialCatalog.forCharacter("P01").gapKind());
        assertEquals(SignatureTrialCatalog.GapKind.TRIAL_BOSS_IDENTITY,
                SignatureTrialCatalog.forCharacter("P02").gapKind());
        assertEquals(SignatureTrialCatalog.GapKind.PROTECTED_NPC_IDENTITY,
                SignatureTrialCatalog.forCharacter("P03").gapKind());

        assertFalse(SignatureTrialCatalog.forCharacter("P01").objectiveEvaluatorReady());
        assertFalse(SignatureTrialCatalog.forCharacter("P02").objectiveEvaluatorReady());
        assertFalse(SignatureTrialCatalog.forCharacter("P03").objectiveEvaluatorReady());
    }

    @Test
    void p04ThroughP07HaveCompleteObjectiveTelemetryButStillNeedEncounterRosters() {
        for (String id : new String[]{"P04", "P05", "P06", "P07"}) {
            var spec = SignatureTrialCatalog.forCharacter(id);
            assertEquals(SignatureTrialCatalog.GapKind.ENCOUNTER_ROSTER, spec.gapKind(), id);
            assertTrue(spec.objectiveEvaluatorReady(), id);
            assertEquals(SignatureTrialCatalog.CanonState.RULES_READY_ROSTER_GAP, spec.canonState(), id);
            assertTrue(spec.authoringBlockReason().contains("목표 판정 로직 준비 완료"), id);
        }
    }

    @Test
    void p08IsARealPrerequisiteContradictionNotAnEncounterAuthoringGap() {
        var spec = SignatureTrialCatalog.forCharacter("P08");
        assertEquals(SignatureTrialCatalog.CanonState.CANON_CONTRADICTION, spec.canonState());
        assertEquals(SignatureTrialCatalog.GapKind.PREREQUISITE_CONTRADICTION, spec.gapKind());
        assertFalse(spec.objectiveEvaluatorReady());
        assertTrue(spec.authoringBlockReason().contains("선행조건"));
    }
}

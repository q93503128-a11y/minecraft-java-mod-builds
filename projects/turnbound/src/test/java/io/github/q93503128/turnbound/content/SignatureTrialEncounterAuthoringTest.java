package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SignatureTrialEncounterAuthoringTest {
    @Test
    void currentV04CanonKeepsEverySignatureTrialBlocked() {
        for (SignatureTrialCatalog.Spec trial : SignatureTrialCatalog.all()) {
            var readiness = SignatureTrialEncounterAuthoring.readiness(trial.characterId());
            assertFalse(readiness.ready(), trial.characterId());
            assertFalse(readiness.blockReason().isBlank(), trial.characterId());
        }
    }

    @Test
    void rosterOnlyTrialNeedsEncounterIdAndEnemyRoster() {
        var missing = SignatureTrialEncounterAuthoring.validate("P04",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P04", "", List.of(), "", "", ""));
        assertFalse(missing.ready());
        assertTrue(missing.blockReason().contains("encounterId"));
        assertTrue(missing.blockReason().contains("enemyIds"));

        var complete = SignatureTrialEncounterAuthoring.validate("P04",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P04", "SIG_P04", List.of("E_A", "E_B"), "", "", ""));
        assertTrue(complete.ready());
    }

    @Test
    void p01RequiresSpecialEliteInsideEnemyRoster() {
        var missingIdentity = SignatureTrialEncounterAuthoring.validate("P01",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P01", "SIG_P01", List.of("E_A"), "", "", ""));
        assertFalse(missingIdentity.ready());
        assertTrue(missingIdentity.blockReason().contains("specialEliteId"));

        var outsideRoster = SignatureTrialEncounterAuthoring.validate("P01",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P01", "SIG_P01", List.of("E_A"), "ELITE_X", "", ""));
        assertFalse(outsideRoster.ready());
        assertTrue(outsideRoster.blockReason().contains("enemyIds"));

        var complete = SignatureTrialEncounterAuthoring.validate("P01",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P01", "SIG_P01", List.of("ELITE_X"), "ELITE_X", "", ""));
        assertTrue(complete.ready());
    }

    @Test
    void p02RequiresTrialBossInsideEnemyRoster() {
        var complete = SignatureTrialEncounterAuthoring.validate("P02",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P02", "SIG_P02", List.of("BOSS_X"), "", "BOSS_X", ""));
        assertTrue(complete.ready());
    }

    @Test
    void p03RequiresProtectedNpcOutsideEnemyRoster() {
        var invalid = SignatureTrialEncounterAuthoring.validate("P03",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P03", "SIG_P03", List.of("NPC_X"), "", "", "NPC_X"));
        assertFalse(invalid.ready());
        assertTrue(invalid.blockReason().contains("enemyIds"));

        var complete = SignatureTrialEncounterAuthoring.validate("P03",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P03", "SIG_P03", List.of("E_A"), "", "", "NPC_X"));
        assertTrue(complete.ready());
    }

    @Test
    void p08RemainsBlockedEvenWithStructurallyFilledDraft() {
        var result = SignatureTrialEncounterAuthoring.validate("P08",
                new SignatureTrialEncounterAuthoring.EncounterSpec("P08", "SIG_P08", List.of("E_A"), "", "", ""));
        assertFalse(result.ready());
        assertTrue(result.blockReason().contains("PREREQUISITE_CONTRADICTION"));
    }
}

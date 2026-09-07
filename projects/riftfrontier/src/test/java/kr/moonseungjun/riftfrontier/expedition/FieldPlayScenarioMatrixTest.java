package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldPlayScenarioMatrixTest {
    @Test
    void matrixCoversEveryRequiredM2ManualBoundaryExactlyOnce() {
        var scenarios = FieldPlayScenarioMatrix.scenarios();
        assertEquals(6, scenarios.size());

        Set<FieldPlayScenarioMatrix.ScenarioId> ids = scenarios.stream()
            .map(FieldPlayScenarioMatrix.Scenario::id)
            .collect(Collectors.toSet());

        assertEquals(Set.of(FieldPlayScenarioMatrix.ScenarioId.values()), ids);
        assertTrue(scenarios.stream().allMatch(scenario -> !scenario.checkpoints().isEmpty()));
        assertTrue(scenarios.stream().allMatch(scenario -> !scenario.passCriterion().isBlank()));
    }

    @Test
    void renderedChecklistIsStableAndExplicitlyContainsEvidenceBoundaries() {
        var lines = FieldPlayScenarioMatrix.reportLines();
        assertEquals(6, lines.size());
        assertTrue(lines.stream().anyMatch(line -> line.contains("scenario=fast-extraction") && line.contains("pre-extraction")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("scenario=patrol-clear") && line.contains("+1 retained salvage")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("scenario=failure-boundaries") && line.contains("endReason")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("scenario=restart-reentry") && line.contains("SERVER_RESTART")));
        assertFalse(lines.stream().anyMatch(String::isBlank));
    }
}

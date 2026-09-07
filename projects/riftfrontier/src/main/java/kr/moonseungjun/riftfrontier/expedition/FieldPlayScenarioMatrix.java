package kr.moonseungjun.riftfrontier.expedition;

import java.util.List;

/**
 * Canonical manual-play matrix for the M2 Region 01 quality gate.
 *
 * This is deliberately pure data: it does not mutate world state, fake a verdict, or replace
 * actual Minecraft play. The command surface renders these scenarios so every reviewer runs the
 * same behavioural cases and captures the existing persisted review/evidence trail at comparable
 * checkpoints.
 */
public final class FieldPlayScenarioMatrix {
    private FieldPlayScenarioMatrix() {}

    public enum ScenarioId {
        LOW_PRESSURE_BASELINE("low-pressure"),
        HIGH_PRESSURE_SCALING("high-pressure"),
        FAST_EXTRACTION("fast-extraction"),
        PATROL_CLEAR("patrol-clear"),
        FAILURE_BOUNDARIES("failure-boundaries"),
        RESTART_REENTRY("restart-reentry");

        private final String serializedName;

        ScenarioId(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }
    }

    public record Scenario(
        ScenarioId id,
        String purpose,
        List<String> checkpoints,
        String passCriterion
    ) {
        public Scenario {
            if (id == null) throw new IllegalArgumentException("id is required");
            if (purpose == null || purpose.isBlank()) throw new IllegalArgumentException("purpose is required");
            checkpoints = List.copyOf(checkpoints);
            if (checkpoints.isEmpty()) throw new IllegalArgumentException("at least one checkpoint is required");
            if (passCriterion == null || passCriterion.isBlank()) throw new IllegalArgumentException("passCriterion is required");
        }

        public String reportLine() {
            return "scenario=" + id.serializedName()
                + " | purpose=" + purpose
                + " | checkpoints=" + String.join(" -> ", checkpoints)
                + " | pass=" + passCriterion;
        }
    }

    private static final List<Scenario> SCENARIOS = List.of(
        new Scenario(
            ScenarioId.LOW_PRESSURE_BASELINE,
            "baseline spawn spacing, aggro rhythm, salvage hazard readability",
            List.of("start at low pressure", "review before first salvage", "review after each salvage", "review immediately before extraction"),
            "encounter remains readable and salvage interaction does not create unavoidable damage"
        ),
        new Scenario(
            ScenarioId.HIGH_PRESSURE_SCALING,
            "verify pressure increases difficulty without collapsing spacing or counterplay",
            List.of("reach a higher persisted pressure", "start expedition", "compare persisted startContext", "repeat salvage checkpoints"),
            "extra hunter/scout pressure is perceptible while extraction remains a deliberate viable choice"
        ),
        new Scenario(
            ScenarioId.FAST_EXTRACTION,
            "validate the risk/reward path that ignores remaining patrol threats",
            List.of("recover contract minimum", "leave patrol alive", "capture pre-extraction review", "extract", "inspect trail"),
            "extraction succeeds without patrol bonus and terminal evidence preserves live threats before cleanup"
        ),
        new Scenario(
            ScenarioId.PATROL_CLEAR,
            "validate combat-resource coupling and patrol suppression reward",
            List.of("recover contract minimum", "clear all run-owned threats", "confirm liveThreats=0", "extract", "inspect retained salvage"),
            "patrol-clear route grants exactly the intended +1 retained salvage and does not double-award"
        ),
        new Scenario(
            ScenarioId.FAILURE_BOUNDARIES,
            "exercise owner-bound abort/death/logout failure semantics",
            List.of("run explicit abort", "run player death", "run player logout", "rejoin and inspect latest review/trail after each"),
            "each path ends FAILED with its own endReason, no supply refund, no foreign-player lifecycle mutation"
        ),
        new Scenario(
            ScenarioId.RESTART_REENTRY,
            "verify restart reconciliation and stranded-player re-entry UX in an actual client",
            List.of("deploy expedition", "capture active review", "restart server", "rejoin", "inspect review/trail and hub return"),
            "run is SERVER_RESTART failure, technical proxies do not survive as owned threats, player is not stranded in the field"
        )
    );

    public static List<Scenario> scenarios() {
        return SCENARIOS;
    }

    public static List<String> reportLines() {
        return SCENARIOS.stream().map(Scenario::reportLine).toList();
    }
}

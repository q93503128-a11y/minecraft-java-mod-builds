package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record R01EarthloongEncounterData(
        String id,
        int contentLevel,
        int decisionDelayTicks,
        double phaseTwoHealthThreshold,
        LightningFurrowPattern lightningFurrow,
        List<ActionRule> actions
) {
    public R01EarthloongEncounterData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(lightningFurrow, "lightningFurrow");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }

    public Map<ActionId, ActionRule> rulesById() {
        EnumMap<ActionId, ActionRule> result = new EnumMap<>(ActionId.class);
        for (ActionRule action : actions) {
            ActionRule previous = result.put(action.id(), action);
            if (previous != null) {
                throw new IllegalStateException("Duplicate Earthloong action rule: " + action.id());
            }
        }
        return Map.copyOf(result);
    }

    public enum ActionId {
        CLAW_SWEEP,
        TAIL_SCYTHE,
        QUARRY_RUSH,
        LIGHTNING_FURROW,
        ROOT_BREAKER,
        FORKED_HEAVEN,
        EARTHLINE_SURGE
    }

    public enum Phase {
        ONE(1),
        TWO(2);

        private final int number;

        Phase(int number) {
            this.number = number;
        }

        public int number() {
            return number;
        }
    }

    public record LightningFurrowPattern(
            int phaseOneLaneCount,
            int phaseTwoFirstLaneCount,
            List<Integer> phaseTwoAlternatingLaneCounts
    ) {
        public LightningFurrowPattern {
            phaseTwoAlternatingLaneCounts = List.copyOf(
                    Objects.requireNonNull(
                            phaseTwoAlternatingLaneCounts,
                            "phaseTwoAlternatingLaneCounts"
                    )
            );
        }
    }

    public record ActionRule(
            ActionId id,
            int minimumPhase,
            int weight,
            int cooldownTicks,
            boolean spaceControl
    ) {
        public ActionRule {
            Objects.requireNonNull(id, "id");
        }
    }
}

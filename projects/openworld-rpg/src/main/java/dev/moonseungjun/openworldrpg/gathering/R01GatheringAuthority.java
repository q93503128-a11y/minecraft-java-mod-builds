package dev.moonseungjun.openworldrpg.gathering;

import java.util.Objects;

/** Pure validation/yield authority for an authored personal R01 gathering interaction. */
public final class R01GatheringAuthority {
    private R01GatheringAuthority() {
    }

    public static GatherDecision evaluate(
            R01GatheringState state,
            String nodeId,
            String resourceId,
            long activeTicks,
            GatherContext context,
            int baseYieldRollOffset,
            int bonusRollPercent
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(context, "context");

        var definition = R01GatheringRules.resource(resourceId);
        if (definition.isEmpty()) {
            return new GatherDecision(Status.INVALID_RESOURCE, 0);
        }
        var resource = definition.orElseThrow();

        if (!context.eligible()) {
            return new GatherDecision(Status.ACTION_BLOCKED, 0);
        }
        if (!state.isNodeAvailable(nodeId, activeTicks)) {
            return new GatherDecision(Status.NODE_COOLDOWN, 0);
        }
        if (state.toolTier(resource.toolFamily()).level()
                < resource.requiredToolTier().level()) {
            return new GatherDecision(Status.TOOL_TIER_TOO_LOW, 0);
        }
        if (baseYieldRollOffset < 0
                || baseYieldRollOffset >= resource.yieldRangeSize()) {
            throw new IllegalArgumentException("baseYieldRollOffset outside resource range.");
        }
        if (bonusRollPercent < 0 || bonusRollPercent >= 100) {
            throw new IllegalArgumentException("bonusRollPercent must be inside 0..99.");
        }

        int quantity = resource.minBaseYield() + baseYieldRollOffset;
        if (!resource.rareOrDense()) {
            int rank = state.masteryRank(resource.discipline());
            int chance = R01GatheringRules.ordinaryBonusChancePercent(rank);
            if (bonusRollPercent < chance) {
                quantity++;
            }
        }
        return new GatherDecision(Status.ALLOWED, quantity);
    }

    public enum Status {
        ALLOWED,
        INVALID_RESOURCE,
        ACTION_BLOCKED,
        NODE_COOLDOWN,
        TOOL_TIER_TOO_LOW
    }

    public record GatherContext(
            boolean inCombat,
            boolean mounted,
            boolean downed,
            boolean climbing,
            boolean incompatibleCommittedAction
    ) {
        public boolean eligible() {
            return !inCombat
                    && !mounted
                    && !downed
                    && !climbing
                    && !incompatibleCommittedAction;
        }

        public static GatherContext clear() {
            return new GatherContext(false, false, false, false, false);
        }
    }

    public record GatherDecision(
            Status status,
            int quantity
    ) {
        public GatherDecision {
            Objects.requireNonNull(status, "status");
            if (quantity < 0) {
                throw new IllegalArgumentException("quantity must be non-negative.");
            }
            if (status != Status.ALLOWED && quantity != 0) {
                throw new IllegalArgumentException(
                        "Rejected gathering decision cannot yield material."
                );
            }
        }

        public boolean allowed() {
            return status == Status.ALLOWED;
        }
    }
}

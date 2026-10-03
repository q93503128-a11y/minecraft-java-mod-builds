package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure server-owned R01 Regalhart action selector and deterministic internal combo-counter authority.
 */
public final class R01RegalhartActionController {
    private final R01RegalhartEncounterData data;
    private final String encounterInstanceId;
    private final UUID actorId;
    private final Map<R01RegalhartEncounterData.ActionId, Long> cooldownEndTicks =
            new EnumMap<>(R01RegalhartEncounterData.ActionId.class);

    private long actionCounter;
    private R01RegalhartEncounterData.ActionId lastCommittedAction;
    private int consecutiveOrdinaryCount;
    private int antlerSweepCounter;
    private int sovereignCrownChargeCounter;
    private boolean sovereignEntered;
    private long sovereignTransitionEndTick = Long.MIN_VALUE;

    public R01RegalhartActionController(
            R01RegalhartEncounterData data,
            String encounterInstanceId,
            UUID actorId
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01RegalhartEncounterDataLoader.validate(data);
        if (encounterInstanceId == null || encounterInstanceId.isBlank()) {
            throw new IllegalArgumentException("encounterInstanceId must be non-blank.");
        }
        this.encounterInstanceId = encounterInstanceId;
        this.actorId = Objects.requireNonNull(actorId, "actorId");
    }

    public Decision select(Context context, long nowTick) {
        Objects.requireNonNull(context, "context");
        requireTick(nowTick);

        if (!sovereignEntered
                && context.healthFraction()
                        <= data.sovereign().healthFractionInclusive()) {
            sovereignEntered = true;
            sovereignTransitionEndTick = Math.addExact(
                    nowTick,
                    data.sovereign().transitionTicks()
            );
            consecutiveOrdinaryCount = 0;
            lastCommittedAction = null;
            return Decision.sovereignTransitionStart(
                    actionCounter,
                    sovereignTransitionEndTick
            );
        }
        if (sovereignEntered && nowTick < sovereignTransitionEndTick) {
            return Decision.sovereignTransitionHold(
                    actionCounter,
                    sovereignTransitionEndTick
            );
        }

        var rearKick = rule(R01RegalhartEncounterData.ActionId.REAR_KICK);
        if (context.rearArcLegal()
                && context.targetDistance() <= rearKick.maximumRange()
                && ready(rearKick.id(), nowTick)) {
            return commit(rearKick, nowTick, false, false);
        }

        var sweep = rule(R01RegalhartEncounterData.ActionId.ANTLER_SWEEP);
        if (context.targetDistance() <= sweep.maximumRange()) {
            return commit(
                    sweep,
                    nowTick,
                    context.mirroredSweepFollowUpArcLegal(),
                    false
            );
        }

        if (context.targetDistance() > 16.0
                || exactUnresolvedWeightBoundary(context.targetDistance())) {
            return Decision.reposition(actionCounter);
        }

        List<WeightedAction> candidates = new ArrayList<>(2);
        var charge = rule(R01RegalhartEncounterData.ActionId.CROWN_CHARGE);
        var bound = rule(R01RegalhartEncounterData.ActionId.ROYAL_BOUND);
        int[] weights = distanceWeights(context.targetDistance());

        if (context.crownChargeLegal() && ready(charge.id(), nowTick)) {
            candidates.add(new WeightedAction(charge, weights[0]));
        }
        if (context.royalBoundLegal() && ready(bound.id(), nowTick)) {
            candidates.add(new WeightedAction(bound, weights[1]));
        }

        if (candidates.isEmpty()) {
            return Decision.reposition(actionCounter);
        }

        // Crown Charge / Royal Bound are signature movement/space-control actions. When both are
        // legal, the immediately repeated signature is removed. If only one is legal, canon permits
        // repetition.
        if (candidates.size() > 1
                && isSignature(lastCommittedAction)) {
            candidates.removeIf(candidate ->
                    candidate.rule().id() == lastCommittedAction
            );
        } else if (candidates.size() > 1
                && consecutiveOrdinaryCount >= 2
                && lastCommittedAction != null) {
            candidates.removeIf(candidate ->
                    candidate.rule().id() == lastCommittedAction
            );
        }

        WeightedAction chosen;
        if (candidates.size() == 1) {
            chosen = candidates.getFirst();
        } else {
            int total = candidates.stream()
                    .mapToInt(WeightedAction::weight)
                    .sum();
            int roll = deterministicRoll(total);
            int cursor = 0;
            chosen = null;
            for (WeightedAction candidate : candidates) {
                cursor += candidate.weight();
                if (roll < cursor) {
                    chosen = candidate;
                    break;
                }
            }
            if (chosen == null) {
                throw new IllegalStateException(
                        "Regalhart weighted selection produced no action."
                );
            }
        }

        boolean secondChargeDue = false;
        if (chosen.rule().id()
                == R01RegalhartEncounterData.ActionId.CROWN_CHARGE
                && sovereignEntered) {
            sovereignCrownChargeCounter++;
            secondChargeDue =
                    sovereignCrownChargeCounter
                            % data.sovereign().crownChargeComboEvery()
                            == 0;
        }

        return commit(chosen.rule(), nowTick, false, secondChargeDue);
    }

    public long cooldownRemainingTicks(
            R01RegalhartEncounterData.ActionId action,
            long nowTick
    ) {
        requireTick(nowTick);
        return Math.max(
                0L,
                cooldownEndTicks.getOrDefault(
                        Objects.requireNonNull(action, "action"),
                        Long.MIN_VALUE
                ) - nowTick
        );
    }

    public long sovereignTransitionRemainingTicks(long nowTick) {
        requireTick(nowTick);
        return sovereignEntered
                ? Math.max(0L, sovereignTransitionEndTick - nowTick)
                : 0L;
    }

    public boolean sovereignEntered() {
        return sovereignEntered;
    }

    public int antlerSweepCounter() {
        return antlerSweepCounter;
    }

    public int sovereignCrownChargeCounter() {
        return sovereignCrownChargeCounter;
    }

    private Decision commit(
            R01RegalhartEncounterData.AttackRule rule,
            long nowTick,
            boolean mirroredSweepFollowUpArcLegal,
            boolean secondChargeDue
    ) {
        long selectedCounter = actionCounter++;
        if (rule.cooldownTicks() > 0) {
            cooldownEndTicks.put(
                    rule.id(),
                    nowTick + rule.cooldownTicks()
            );
        }

        boolean mirroredSweepFollowUp = false;
        if (rule.id()
                == R01RegalhartEncounterData.ActionId.ANTLER_SWEEP) {
            antlerSweepCounter++;
            mirroredSweepFollowUp =
                    antlerFollowUpDueAfterIncrement()
                            && mirroredSweepFollowUpArcLegal;
        }

        if (rule.id() == lastCommittedAction) {
            consecutiveOrdinaryCount++;
        } else {
            consecutiveOrdinaryCount = 1;
        }
        lastCommittedAction = rule.id();

        return Decision.attack(
                rule.id(),
                selectedCounter,
                mirroredSweepFollowUp,
                secondChargeDue
        );
    }

    private boolean antlerFollowUpDueAfterIncrement() {
        int every = sovereignEntered
                ? data.sovereign().sovereignSweepComboEvery()
                : data.sovereign().normalSweepComboEvery();
        return antlerSweepCounter % every == 0;
    }

    private static boolean isSignature(
            R01RegalhartEncounterData.ActionId action
    ) {
        return action == R01RegalhartEncounterData.ActionId.CROWN_CHARGE
                || action == R01RegalhartEncounterData.ActionId.ROYAL_BOUND;
    }

    private boolean ready(
            R01RegalhartEncounterData.ActionId action,
            long nowTick
    ) {
        return nowTick >= cooldownEndTicks.getOrDefault(
                action,
                Long.MIN_VALUE
        );
    }

    private R01RegalhartEncounterData.AttackRule rule(
            R01RegalhartEncounterData.ActionId action
    ) {
        var rule = data.rulesById().get(action);
        if (rule == null) {
            throw new IllegalStateException(
                    "Missing Regalhart action rule: " + action
            );
        }
        return rule;
    }

    /**
     * The design table currently labels both adjacent rows with the exact 7.0 and 12.0 endpoints.
     * Until canon explicitly assigns those two shared endpoints, selection fails closed there rather
     * than silently changing the authored 45/55, 60/40 or 75/25 probability.
     */
    static boolean exactUnresolvedWeightBoundary(double distance) {
        return Double.compare(distance, 7.0) == 0
                || Double.compare(distance, 12.0) == 0;
    }

    static int[] distanceWeights(double distance) {
        if (!Double.isFinite(distance)
                || distance <= 4.5
                || distance > 16.0
                || exactUnresolvedWeightBoundary(distance)) {
            throw new IllegalArgumentException(
                    "Regalhart weighted distance must be in a canon-resolved mid/far interval."
            );
        }
        if (distance < 7.0) {
            return new int[] {45, 55};
        }
        if (distance < 12.0) {
            return new int[] {60, 40};
        }
        return new int[] {75, 25};
    }

    private int deterministicRoll(int bound) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(encounterInstanceId.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(actorId.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Long.toString(actionCounter).getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest.digest()).getLong();
            return (int) Math.floorMod(value, (long) bound);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable for deterministic Regalhart RNG.",
                    exception
            );
        }
    }

    private static void requireTick(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
    }

    private record WeightedAction(
            R01RegalhartEncounterData.AttackRule rule,
            int weight
    ) {
        private WeightedAction {
            Objects.requireNonNull(rule, "rule");
            if (weight <= 0) {
                throw new IllegalArgumentException("Regalhart action weight must be positive.");
            }
        }
    }

    public record Context(
            double targetDistance,
            boolean rearArcLegal,
            boolean mirroredSweepFollowUpArcLegal,
            boolean crownChargeLegal,
            boolean royalBoundLegal,
            double healthFraction
    ) {
        public Context {
            if (!Double.isFinite(targetDistance)
                    || targetDistance < 0.0
                    || !Double.isFinite(healthFraction)
                    || healthFraction < 0.0
                    || healthFraction > 1.0) {
                throw new IllegalArgumentException(
                        "Invalid Regalhart decision context."
                );
            }
        }
    }

    public enum Mode {
        ATTACK,
        SOVEREIGN_TRANSITION_START,
        SOVEREIGN_TRANSITION_HOLD,
        REPOSITION
    }

    public record Decision(
            Mode mode,
            Optional<R01RegalhartEncounterData.ActionId> action,
            long actionCounter,
            boolean mirroredSweepFollowUp,
            boolean sovereignSecondChargeDue,
            long sovereignTransitionEndTick
    ) {
        public Decision {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(action, "action");
            if ((mode == Mode.ATTACK) != action.isPresent()) {
                throw new IllegalArgumentException(
                        "Only Regalhart ATTACK decisions carry an action."
                );
            }
            if (mirroredSweepFollowUp
                    && action.orElse(null)
                            != R01RegalhartEncounterData.ActionId.ANTLER_SWEEP) {
                throw new IllegalArgumentException(
                        "Mirrored follow-up must belong to Antler Sweep."
                );
            }
            if (sovereignSecondChargeDue
                    && action.orElse(null)
                            != R01RegalhartEncounterData.ActionId.CROWN_CHARGE) {
                throw new IllegalArgumentException(
                        "Second-charge due marker must belong to Crown Charge."
                );
            }
        }

        public boolean sovereignTransition() {
            return mode == Mode.SOVEREIGN_TRANSITION_START
                    || mode == Mode.SOVEREIGN_TRANSITION_HOLD;
        }

        public boolean reposition() {
            return mode == Mode.REPOSITION;
        }

        public static Decision attack(
                R01RegalhartEncounterData.ActionId action,
                long actionCounter,
                boolean mirroredSweepFollowUp,
                boolean sovereignSecondChargeDue
        ) {
            return new Decision(
                    Mode.ATTACK,
                    Optional.of(action),
                    actionCounter,
                    mirroredSweepFollowUp,
                    sovereignSecondChargeDue,
                    0L
            );
        }

        public static Decision sovereignTransitionStart(
                long actionCounter,
                long transitionEndTick
        ) {
            return new Decision(
                    Mode.SOVEREIGN_TRANSITION_START,
                    Optional.empty(),
                    actionCounter,
                    false,
                    false,
                    transitionEndTick
            );
        }

        public static Decision sovereignTransitionHold(
                long actionCounter,
                long transitionEndTick
        ) {
            return new Decision(
                    Mode.SOVEREIGN_TRANSITION_HOLD,
                    Optional.empty(),
                    actionCounter,
                    false,
                    false,
                    transitionEndTick
            );
        }

        public static Decision reposition(long actionCounter) {
            return new Decision(
                    Mode.REPOSITION,
                    Optional.empty(),
                    actionCounter,
                    false,
                    false,
                    0L
            );
        }
    }
}

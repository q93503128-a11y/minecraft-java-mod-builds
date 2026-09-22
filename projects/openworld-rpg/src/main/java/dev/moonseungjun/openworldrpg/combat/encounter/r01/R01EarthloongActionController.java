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
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Pure server-authoritative action-selection state for the R01 Earthloong encounter.
 *
 * <p>Spatial/anatomy validation is deliberately outside this class. The eventual actor binder
 * reports which authored actions are legal after validating real model orientation, hit geometry,
 * terrain and current target. This controller then owns canonical weights, cooldowns, phase gates,
 * anti-spam history and Lightning Furrow lane sequencing.</p>
 */
public final class R01EarthloongActionController {
    private final R01EarthloongEncounterData data;
    private final String encounterInstanceId;
    private final UUID actorId;
    private final Map<R01EarthloongEncounterData.ActionId, Long> cooldownEndTicks =
            new EnumMap<>(R01EarthloongEncounterData.ActionId.class);

    private long actionCounter;
    private int consecutiveSpaceControlActions;
    private int phaseTwoFurrowCastCount;
    private R01EarthloongEncounterData.ActionId lastCommittedAction;
    private int consecutiveSameActionCount;

    public R01EarthloongActionController(
            R01EarthloongEncounterData data,
            String encounterInstanceId,
            UUID actorId
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01EarthloongEncounterDataLoader.validate(data);
        this.encounterInstanceId = requireIdentity(encounterInstanceId, "encounterInstanceId");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
    }

    public Decision select(
            R01EarthloongEncounterData.Phase phase,
            Legality legality,
            long nowTick
    ) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(legality, "legality");
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }

        List<R01EarthloongEncounterData.ActionRule> candidates = new ArrayList<>();
        for (var rule : data.actions()) {
            if (rule.minimumPhase() > phase.number()
                    || !legality.isLegal(rule.id())
                    || nowTick < cooldownEndTicks.getOrDefault(rule.id(), Long.MIN_VALUE)) {
                continue;
            }
            candidates.add(rule);
        }

        if (consecutiveSpaceControlActions >= 2) {
            candidates.removeIf(R01EarthloongEncounterData.ActionRule::spaceControl);
            if (candidates.isEmpty()) {
                return Decision.reposition(actionCounter);
            }
        }

        if (lastCommittedAction != null && candidates.size() > 1) {
            int repeatLimit = repeatLimit(lastCommittedAction);
            if (consecutiveSameActionCount >= repeatLimit
                    && candidates.stream().anyMatch(rule -> rule.id() != lastCommittedAction)) {
                candidates.removeIf(rule -> rule.id() == lastCommittedAction);
            }
        }

        if (candidates.isEmpty()) {
            return Decision.reposition(actionCounter);
        }

        int totalWeight = candidates.stream()
                .mapToInt(R01EarthloongEncounterData.ActionRule::weight)
                .sum();
        int roll = deterministicRoll(totalWeight);
        R01EarthloongEncounterData.ActionRule selected = null;
        int cursor = 0;
        for (var candidate : candidates) {
            cursor += candidate.weight();
            if (roll < cursor) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            throw new IllegalStateException("Earthloong weighted selection produced no action.");
        }

        long selectedCounter = actionCounter;
        OptionalInt laneCount = lightningFurrowLaneCount(selected.id(), phase);

        if (selected.cooldownTicks() > 0) {
            cooldownEndTicks.put(
                    selected.id(),
                    nowTick + selected.cooldownTicks()
            );
        }
        if (selected.spaceControl()) {
            consecutiveSpaceControlActions++;
        } else {
            consecutiveSpaceControlActions = 0;
        }

        if (selected.id() == lastCommittedAction) {
            consecutiveSameActionCount++;
        } else {
            lastCommittedAction = selected.id();
            consecutiveSameActionCount = 1;
        }
        actionCounter++;

        return Decision.attack(
                selected.id(),
                selectedCounter,
                cooldownEndTicks.getOrDefault(selected.id(), nowTick),
                laneCount
        );
    }

    public long cooldownRemainingTicks(
            R01EarthloongEncounterData.ActionId action,
            long nowTick
    ) {
        Objects.requireNonNull(action, "action");
        return Math.max(
                0L,
                cooldownEndTicks.getOrDefault(action, Long.MIN_VALUE) - nowTick
        );
    }

    public long actionCounter() {
        return actionCounter;
    }

    public int consecutiveSpaceControlActions() {
        return consecutiveSpaceControlActions;
    }

    public int phaseTwoFurrowCastCount() {
        return phaseTwoFurrowCastCount;
    }

    public Optional<R01EarthloongEncounterData.ActionId> lastCommittedAction() {
        return Optional.ofNullable(lastCommittedAction);
    }

    public int consecutiveSameActionCount() {
        return consecutiveSameActionCount;
    }

    private static int repeatLimit(R01EarthloongEncounterData.ActionId action) {
        return switch (Objects.requireNonNull(action, "action")) {
            case QUARRY_RUSH, LIGHTNING_FURROW, ROOT_BREAKER, FORKED_HEAVEN, EARTHLINE_SURGE -> 1;
            case CLAW_SWEEP, TAIL_SCYTHE -> 2;
        };
    }

    private OptionalInt lightningFurrowLaneCount(
            R01EarthloongEncounterData.ActionId action,
            R01EarthloongEncounterData.Phase phase
    ) {
        if (action != R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW) {
            return OptionalInt.empty();
        }

        var pattern = data.lightningFurrow();
        if (phase == R01EarthloongEncounterData.Phase.ONE) {
            return OptionalInt.of(pattern.phaseOneLaneCount());
        }

        int result;
        if (phaseTwoFurrowCastCount == 0) {
            result = pattern.phaseTwoFirstLaneCount();
        } else {
            List<Integer> alternating = pattern.phaseTwoAlternatingLaneCounts();
            result = alternating.get((phaseTwoFurrowCastCount - 1) % alternating.size());
        }
        phaseTwoFurrowCastCount++;
        return OptionalInt.of(result);
    }

    private int deterministicRoll(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive.");
        }
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
            throw new IllegalStateException("SHA-256 unavailable for deterministic encounter RNG.", exception);
        }
    }

    private static String requireIdentity(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must be non-blank.");
        }
        return value;
    }

    public record Legality(
            boolean clawSweep,
            boolean tailScythe,
            boolean quarryRush,
            boolean lightningFurrow,
            boolean rootBreaker,
            boolean forkedHeaven,
            boolean earthlineSurge
    ) {
        public boolean isLegal(R01EarthloongEncounterData.ActionId id) {
            return switch (Objects.requireNonNull(id, "id")) {
                case CLAW_SWEEP -> clawSweep;
                case TAIL_SCYTHE -> tailScythe;
                case QUARRY_RUSH -> quarryRush;
                case LIGHTNING_FURROW -> lightningFurrow;
                case ROOT_BREAKER -> rootBreaker;
                case FORKED_HEAVEN -> forkedHeaven;
                case EARTHLINE_SURGE -> earthlineSurge;
            };
        }

        public static Legality only(R01EarthloongEncounterData.ActionId id) {
            Objects.requireNonNull(id, "id");
            return new Legality(
                    id == R01EarthloongEncounterData.ActionId.CLAW_SWEEP,
                    id == R01EarthloongEncounterData.ActionId.TAIL_SCYTHE,
                    id == R01EarthloongEncounterData.ActionId.QUARRY_RUSH,
                    id == R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW,
                    id == R01EarthloongEncounterData.ActionId.ROOT_BREAKER,
                    id == R01EarthloongEncounterData.ActionId.FORKED_HEAVEN,
                    id == R01EarthloongEncounterData.ActionId.EARTHLINE_SURGE
            );
        }
    }

    public record Decision(
            Optional<R01EarthloongEncounterData.ActionId> action,
            boolean reposition,
            long actionCounter,
            long cooldownEndTick,
            OptionalInt lightningFurrowLaneCount
    ) {
        public Decision {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(lightningFurrowLaneCount, "lightningFurrowLaneCount");
            if (reposition == action.isPresent()) {
                throw new IllegalArgumentException(
                        "Decision must be exactly one of attack or reposition."
                );
            }
            if (reposition && lightningFurrowLaneCount.isPresent()) {
                throw new IllegalArgumentException(
                        "Reposition decision cannot carry a Lightning Furrow lane count."
                );
            }
        }

        public static Decision attack(
                R01EarthloongEncounterData.ActionId action,
                long actionCounter,
                long cooldownEndTick,
                OptionalInt lightningFurrowLaneCount
        ) {
            return new Decision(
                    Optional.of(Objects.requireNonNull(action, "action")),
                    false,
                    actionCounter,
                    cooldownEndTick,
                    lightningFurrowLaneCount
            );
        }

        public static Decision reposition(long actionCounter) {
            return new Decision(
                    Optional.empty(),
                    true,
                    actionCounter,
                    0L,
                    OptionalInt.empty()
            );
        }
    }
}

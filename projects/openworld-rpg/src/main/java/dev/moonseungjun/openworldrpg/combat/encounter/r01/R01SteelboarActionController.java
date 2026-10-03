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
 * Pure server-owned action selector for the R01 Steelboar.
 *
 * <p>Iron Rush owns the authored mid/far priority. Below 35% HP, a ready Furious Route deterministically
 * replaces the next otherwise-legal 6-12 block Iron Rush opportunity; it is never a random roll.</p>
 */
public final class R01SteelboarActionController {
    private final R01SteelboarEncounterData data;
    private final String encounterInstanceId;
    private final UUID actorId;
    private final Map<R01SteelboarEncounterData.ActionId, Long> cooldownEndTicks =
            new EnumMap<>(R01SteelboarEncounterData.ActionId.class);

    private long actionCounter;
    private long furiousRouteCooldownEndTick = Long.MIN_VALUE;
    private R01SteelboarEncounterData.ActionId lastCommittedAction;
    private int consecutiveOrdinaryCount;

    public R01SteelboarActionController(
            R01SteelboarEncounterData data,
            String encounterInstanceId,
            UUID actorId
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01SteelboarEncounterDataLoader.validate(data);
        if (encounterInstanceId == null || encounterInstanceId.isBlank()) {
            throw new IllegalArgumentException("encounterInstanceId must be non-blank.");
        }
        this.encounterInstanceId = encounterInstanceId;
        this.actorId = Objects.requireNonNull(actorId, "actorId");
    }

    public Decision select(Context context, long nowTick) {
        Objects.requireNonNull(context, "context");
        requireTick(nowTick);

        var rush = rule(R01SteelboarEncounterData.ActionId.IRON_RUSH);
        boolean rushLegal = context.clearCommittedLine()
                && context.targetDistance() >= rush.minimumRange()
                && context.targetDistance() <= rush.maximumRange()
                && nowTick >= cooldownEndTicks.getOrDefault(
                        rush.id(),
                        Long.MIN_VALUE
                );

        if (rushLegal) {
            var furious = data.furiousRoute();
            boolean furiousRange = context.targetDistance() >= furious.minimumRange()
                    && context.targetDistance() <= furious.maximumRange();
            if (context.healthFraction() < furious.healthFractionExclusive()
                    && furiousRange
                    && nowTick >= furiousRouteCooldownEndTick) {
                return commitRush(nowTick, true);
            }
            return commitRush(nowTick, false);
        }

        if (context.targetDistance() > 3.0) {
            return Decision.reposition(actionCounter);
        }

        List<R01SteelboarEncounterData.AttackRule> candidates = new ArrayList<>();
        var tusk = rule(R01SteelboarEncounterData.ActionId.IRON_TUSK);
        candidates.add(tusk);

        var hook = rule(R01SteelboarEncounterData.ActionId.SHOULDER_HOOK);
        if (nowTick >= cooldownEndTicks.getOrDefault(
                hook.id(),
                Long.MIN_VALUE
        )) {
            candidates.add(hook);
        }

        if (candidates.size() > 1
                && consecutiveOrdinaryCount >= 2
                && lastCommittedAction != null) {
            candidates.removeIf(rule -> rule.id() == lastCommittedAction);
        }

        int totalWeight = candidates.stream()
                .mapToInt(R01SteelboarEncounterData.AttackRule::weight)
                .sum();
        int roll = deterministicRoll(totalWeight);
        int cursor = 0;
        for (var candidate : candidates) {
            cursor += candidate.weight();
            if (roll < cursor) {
                return commitOrdinary(candidate, nowTick);
            }
        }
        throw new IllegalStateException("Steelboar weighted selection produced no action.");
    }

    public long cooldownRemainingTicks(
            R01SteelboarEncounterData.ActionId action,
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

    public long furiousRouteCooldownRemainingTicks(long nowTick) {
        requireTick(nowTick);
        return Math.max(0L, furiousRouteCooldownEndTick - nowTick);
    }

    public long actionCounter() {
        return actionCounter;
    }

    public Optional<R01SteelboarEncounterData.ActionId> lastCommittedAction() {
        return Optional.ofNullable(lastCommittedAction);
    }

    private Decision commitRush(long nowTick, boolean furiousRoute) {
        var rush = rule(R01SteelboarEncounterData.ActionId.IRON_RUSH);
        long selectedCounter = actionCounter++;
        cooldownEndTicks.put(
                rush.id(),
                nowTick + rush.cooldownTicks()
        );
        if (furiousRoute) {
            furiousRouteCooldownEndTick =
                    nowTick + data.furiousRoute().cooldownTicks();
        }
        lastCommittedAction = rush.id();
        consecutiveOrdinaryCount = 0;
        return Decision.attack(
                rush.id(),
                selectedCounter,
                furiousRoute,
                cooldownEndTicks.get(rush.id()),
                furiousRouteCooldownEndTick
        );
    }

    private Decision commitOrdinary(
            R01SteelboarEncounterData.AttackRule rule,
            long nowTick
    ) {
        long selectedCounter = actionCounter++;
        if (rule.cooldownTicks() > 0) {
            cooldownEndTicks.put(
                    rule.id(),
                    nowTick + rule.cooldownTicks()
            );
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
                false,
                cooldownEndTicks.getOrDefault(rule.id(), nowTick),
                furiousRouteCooldownEndTick
        );
    }

    private R01SteelboarEncounterData.AttackRule rule(
            R01SteelboarEncounterData.ActionId action
    ) {
        var result = data.rulesById().get(action);
        if (result == null) {
            throw new IllegalStateException("Missing Steelboar action rule: " + action);
        }
        return result;
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
                    "SHA-256 unavailable for deterministic Steelboar RNG.",
                    exception
            );
        }
    }

    private static void requireTick(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
    }

    public record Context(
            double targetDistance,
            boolean clearCommittedLine,
            double healthFraction
    ) {
        public Context {
            if (!Double.isFinite(targetDistance)
                    || targetDistance < 0.0
                    || !Double.isFinite(healthFraction)
                    || healthFraction < 0.0
                    || healthFraction > 1.0) {
                throw new IllegalArgumentException("Invalid Steelboar decision context.");
            }
        }
    }

    public record Decision(
            Optional<R01SteelboarEncounterData.ActionId> action,
            long actionCounter,
            boolean furiousRoute,
            boolean reposition,
            long actionCooldownEndTick,
            long furiousRouteCooldownEndTick
    ) {
        public Decision {
            Objects.requireNonNull(action, "action");
            if (reposition == action.isPresent()) {
                throw new IllegalArgumentException(
                        "Steelboar decision must be either an action or reposition."
                );
            }
            if (furiousRoute
                    && action.orElse(null)
                            != R01SteelboarEncounterData.ActionId.IRON_RUSH) {
                throw new IllegalArgumentException(
                        "Furious Route must wrap Iron Rush."
                );
            }
        }

        public static Decision attack(
                R01SteelboarEncounterData.ActionId action,
                long actionCounter,
                boolean furiousRoute,
                long actionCooldownEndTick,
                long furiousRouteCooldownEndTick
        ) {
            return new Decision(
                    Optional.of(action),
                    actionCounter,
                    furiousRoute,
                    false,
                    actionCooldownEndTick,
                    furiousRouteCooldownEndTick
            );
        }

        public static Decision reposition(long actionCounter) {
            return new Decision(
                    Optional.empty(),
                    actionCounter,
                    false,
                    true,
                    0L,
                    0L
            );
        }
    }
}

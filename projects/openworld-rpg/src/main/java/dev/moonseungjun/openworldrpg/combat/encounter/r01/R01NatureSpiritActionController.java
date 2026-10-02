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
 * Pure server-owned action/defense state for the R01 Nature Spirit.
 *
 * <p>Living Shell is an authored stance, not a projectile mode. The controller therefore exposes
 * its damage/poise multipliers and forced natural-exit Bloom Quake without allowing donor defense
 * mode to invent a ranged attack.</p>
 */
public final class R01NatureSpiritActionController {
    private final R01SecondaryCreatureEncounterData data;
    private final String encounterInstanceId;
    private final UUID actorId;
    private final Map<R01SecondaryCreatureEncounterData.ActionId, Long> cooldownEndTicks =
            new EnumMap<>(R01SecondaryCreatureEncounterData.ActionId.class);

    private long actionCounter;
    private long livingShellReuseEndTick = Long.MIN_VALUE;
    private long livingShellEndTick = Long.MIN_VALUE;
    private boolean livingShellActive;
    private boolean naturalShellExitPending;
    private R01SecondaryCreatureEncounterData.ActionId lastCommittedAction;

    public R01NatureSpiritActionController(
            R01SecondaryCreatureEncounterData data,
            String encounterInstanceId,
            UUID actorId
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01SecondaryCreatureEncounterDataLoader.validate(data);
        if (data.kind() != R01SecondaryCreatureEncounterData.CreatureKind.NATURE_SPIRIT) {
            throw new IllegalArgumentException("Expected Nature Spirit encounter data.");
        }
        if (encounterInstanceId == null || encounterInstanceId.isBlank()) {
            throw new IllegalArgumentException("encounterInstanceId must be non-blank.");
        }
        this.encounterInstanceId = encounterInstanceId;
        this.actorId = Objects.requireNonNull(actorId, "actorId");
    }

    public Decision select(Context context, long nowTick) {
        Objects.requireNonNull(context, "context");
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
        refreshLivingShell(nowTick);

        if (livingShellActive) {
            return Decision.holdLivingShell(actionCounter, livingShellEndTick);
        }

        var shell = data.livingShell();
        if (naturalShellExitPending) {
            naturalShellExitPending = false;
            if (context.bloomQuake()
                    && context.targetDistance() <= shell.forcedBloomQuakeRange()) {
                return commitAttack(
                        rule(R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE),
                        nowTick,
                        true
                );
            }
        }

        if (nowTick >= livingShellReuseEndTick
                && (context.recentHostileDamageFraction() >= shell.recentDamageTriggerFraction()
                        || context.currentPoiseFraction() <= shell.poiseTriggerFraction())) {
            livingShellActive = true;
            livingShellEndTick = nowTick + shell.durationTicks();
            livingShellReuseEndTick = nowTick + shell.reuseTicks();
            actionCounter++;
            return Decision.enterLivingShell(
                    actionCounter - 1,
                    livingShellEndTick,
                    livingShellReuseEndTick
            );
        }

        List<R01SecondaryCreatureEncounterData.ActionRule> candidates = new ArrayList<>();
        for (var rule : data.actions()) {
            if (!context.isLegal(rule.id())
                    || nowTick < cooldownEndTicks.getOrDefault(
                            rule.id(),
                            Long.MIN_VALUE
                    )
                    || (rule.id() == R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE
                            && lastCommittedAction
                            == R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE)) {
                continue;
            }
            candidates.add(rule);
        }

        if (candidates.isEmpty()) {
            return Decision.reposition(actionCounter);
        }

        int totalWeight = candidates.stream()
                .mapToInt(R01SecondaryCreatureEncounterData.ActionRule::weight)
                .sum();
        int roll = deterministicRoll(totalWeight);
        int cursor = 0;
        for (var candidate : candidates) {
            cursor += candidate.weight();
            if (roll < cursor) {
                return commitAttack(candidate, nowTick, false);
            }
        }
        throw new IllegalStateException("Nature Spirit weighted selection produced no action.");
    }

    public boolean onPoiseBroken(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }
        refreshLivingShell(nowTick);
        if (!livingShellActive) {
            return false;
        }
        livingShellActive = false;
        livingShellEndTick = nowTick;
        naturalShellExitPending = false;
        return true;
    }

    public double directDamageTakenMultiplier(long nowTick) {
        refreshLivingShell(nowTick);
        return livingShellActive
                ? data.livingShell().directDamageTakenMultiplier()
                : 1.0;
    }

    public double poiseDamageTakenMultiplier(long nowTick) {
        refreshLivingShell(nowTick);
        return livingShellActive
                ? data.livingShell().poiseDamageTakenMultiplier()
                : 1.0;
    }

    public long livingShellReuseRemainingTicks(long nowTick) {
        return Math.max(0L, livingShellReuseEndTick - nowTick);
    }

    public long cooldownRemainingTicks(
            R01SecondaryCreatureEncounterData.ActionId action,
            long nowTick
    ) {
        return Math.max(
                0L,
                cooldownEndTicks.getOrDefault(
                        Objects.requireNonNull(action, "action"),
                        Long.MIN_VALUE
                ) - nowTick
        );
    }

    public long actionCounter() {
        return actionCounter;
    }

    public Optional<R01SecondaryCreatureEncounterData.ActionId> lastCommittedAction() {
        return Optional.ofNullable(lastCommittedAction);
    }

    private Decision commitAttack(
            R01SecondaryCreatureEncounterData.ActionRule rule,
            long nowTick,
            boolean forcedAfterShell
    ) {
        long selectedCounter = actionCounter++;
        if (rule.cooldownTicks() > 0) {
            cooldownEndTicks.put(rule.id(), nowTick + rule.cooldownTicks());
        }
        lastCommittedAction = rule.id();
        return Decision.attack(
                rule.id(),
                selectedCounter,
                cooldownEndTicks.getOrDefault(rule.id(), nowTick),
                forcedAfterShell
        );
    }

    private void refreshLivingShell(long nowTick) {
        if (livingShellActive && nowTick >= livingShellEndTick) {
            livingShellActive = false;
            naturalShellExitPending = true;
        }
    }

    private R01SecondaryCreatureEncounterData.ActionRule rule(
            R01SecondaryCreatureEncounterData.ActionId action
    ) {
        var result = data.rulesById().get(action);
        if (result == null) {
            throw new IllegalStateException("Missing Nature Spirit action rule: " + action);
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
            throw new IllegalStateException("SHA-256 unavailable for deterministic encounter RNG.", exception);
        }
    }

    public record Context(
            boolean rootedSwipe,
            boolean earthenRam,
            boolean bloomQuake,
            double targetDistance,
            double recentHostileDamageFraction,
            double currentPoiseFraction
    ) {
        public Context {
            if (!Double.isFinite(targetDistance)
                    || targetDistance < 0.0
                    || !Double.isFinite(recentHostileDamageFraction)
                    || recentHostileDamageFraction < 0.0
                    || !Double.isFinite(currentPoiseFraction)
                    || currentPoiseFraction < 0.0
                    || currentPoiseFraction > 1.0) {
                throw new IllegalArgumentException("Invalid Nature Spirit decision context.");
            }
        }

        public boolean isLegal(R01SecondaryCreatureEncounterData.ActionId action) {
            return switch (Objects.requireNonNull(action, "action")) {
                case ROOTED_SWIPE -> rootedSwipe;
                case EARTHEN_RAM -> earthenRam;
                case BLOOM_QUAKE -> bloomQuake;
                default -> false;
            };
        }
    }

    public enum Mode {
        ATTACK,
        ENTER_LIVING_SHELL,
        HOLD_LIVING_SHELL,
        REPOSITION
    }

    public record Decision(
            Mode mode,
            Optional<R01SecondaryCreatureEncounterData.ActionId> action,
            long actionCounter,
            long cooldownEndTick,
            long livingShellEndTick,
            long livingShellReuseEndTick,
            boolean forcedAfterShell
    ) {
        public Decision {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(action, "action");
            if (mode == Mode.ATTACK && action.isEmpty()) {
                throw new IllegalArgumentException("Nature Spirit attack decision requires an action.");
            }
            if (mode != Mode.ATTACK && action.isPresent()) {
                throw new IllegalArgumentException("Only attack decisions may carry an action.");
            }
        }

        public static Decision attack(
                R01SecondaryCreatureEncounterData.ActionId action,
                long actionCounter,
                long cooldownEndTick,
                boolean forcedAfterShell
        ) {
            return new Decision(
                    Mode.ATTACK,
                    Optional.of(action),
                    actionCounter,
                    cooldownEndTick,
                    0L,
                    0L,
                    forcedAfterShell
            );
        }

        public static Decision enterLivingShell(
                long actionCounter,
                long shellEndTick,
                long shellReuseEndTick
        ) {
            return new Decision(
                    Mode.ENTER_LIVING_SHELL,
                    Optional.empty(),
                    actionCounter,
                    0L,
                    shellEndTick,
                    shellReuseEndTick,
                    false
            );
        }

        public static Decision holdLivingShell(long actionCounter, long shellEndTick) {
            return new Decision(
                    Mode.HOLD_LIVING_SHELL,
                    Optional.empty(),
                    actionCounter,
                    0L,
                    shellEndTick,
                    0L,
                    false
            );
        }

        public static Decision reposition(long actionCounter) {
            return new Decision(
                    Mode.REPOSITION,
                    Optional.empty(),
                    actionCounter,
                    0L,
                    0L,
                    0L,
                    false
            );
        }
    }
}

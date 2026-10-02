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

/** Pure server-owned action selection for the R01 Cave Centipede. */
public final class R01CaveCentipedeActionController {
    private final R01SecondaryCreatureEncounterData data;
    private final String encounterInstanceId;
    private final UUID actorId;
    private final Map<R01SecondaryCreatureEncounterData.ActionId, Long> cooldownEndTicks =
            new EnumMap<>(R01SecondaryCreatureEncounterData.ActionId.class);
    private long actionCounter;

    public R01CaveCentipedeActionController(
            R01SecondaryCreatureEncounterData data,
            String encounterInstanceId,
            UUID actorId
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01SecondaryCreatureEncounterDataLoader.validate(data);
        if (data.kind() != R01SecondaryCreatureEncounterData.CreatureKind.CAVE_CENTIPEDE) {
            throw new IllegalArgumentException("Expected Cave Centipede encounter data.");
        }
        if (encounterInstanceId == null || encounterInstanceId.isBlank()) {
            throw new IllegalArgumentException("encounterInstanceId must be non-blank.");
        }
        this.encounterInstanceId = encounterInstanceId;
        this.actorId = Objects.requireNonNull(actorId, "actorId");
    }

    public Decision select(Legality legality, long nowTick) {
        Objects.requireNonNull(legality, "legality");
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }

        List<R01SecondaryCreatureEncounterData.ActionRule> candidates = new ArrayList<>();
        for (var rule : data.actions()) {
            if (!legality.isLegal(rule.id())
                    || nowTick < cooldownEndTicks.getOrDefault(
                            rule.id(),
                            Long.MIN_VALUE
                    )) {
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
        R01SecondaryCreatureEncounterData.ActionRule selected = null;
        for (var candidate : candidates) {
            cursor += candidate.weight();
            if (roll < cursor) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            throw new IllegalStateException("Cave Centipede weighted selection produced no action.");
        }

        long selectedCounter = actionCounter++;
        if (selected.cooldownTicks() > 0) {
            cooldownEndTicks.put(
                    selected.id(),
                    nowTick + selected.cooldownTicks()
            );
        }
        return Decision.attack(
                selected.id(),
                selectedCounter,
                cooldownEndTicks.getOrDefault(selected.id(), nowTick)
        );
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

    public record Legality(
            boolean scuttleBite,
            boolean bodyRake,
            boolean ceilingDrop
    ) {
        public boolean isLegal(R01SecondaryCreatureEncounterData.ActionId action) {
            return switch (Objects.requireNonNull(action, "action")) {
                case SCUTTLE_BITE -> scuttleBite;
                case BODY_RAKE -> bodyRake;
                case CEILING_DROP -> ceilingDrop;
                default -> false;
            };
        }

        public static Legality only(R01SecondaryCreatureEncounterData.ActionId action) {
            return new Legality(
                    action == R01SecondaryCreatureEncounterData.ActionId.SCUTTLE_BITE,
                    action == R01SecondaryCreatureEncounterData.ActionId.BODY_RAKE,
                    action == R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP
            );
        }
    }

    public record Decision(
            Optional<R01SecondaryCreatureEncounterData.ActionId> action,
            boolean reposition,
            long actionCounter,
            long cooldownEndTick
    ) {
        public Decision {
            Objects.requireNonNull(action, "action");
            if (reposition == action.isPresent()) {
                throw new IllegalArgumentException(
                        "Cave Centipede decision must be exactly attack or reposition."
                );
            }
        }

        public static Decision attack(
                R01SecondaryCreatureEncounterData.ActionId action,
                long actionCounter,
                long cooldownEndTick
        ) {
            return new Decision(Optional.of(action), false, actionCounter, cooldownEndTick);
        }

        public static Decision reposition(long actionCounter) {
            return new Decision(Optional.empty(), true, actionCounter, 0L);
        }
    }
}

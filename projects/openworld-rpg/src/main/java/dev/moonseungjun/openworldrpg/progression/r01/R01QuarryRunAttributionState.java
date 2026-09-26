package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded personal class-attribution evidence for one R01 Quarry run.
 *
 * <p>Repeated hits or repeated interaction cannot inflate ownership because every canonical
 * combat/objective unit is recorded at most once. If the highest contribution count ties, the
 * earliest canonical dungeon contribution owned by a tied class resolves the tie. Completion-time
 * class switching therefore has no ownership value.</p>
 */
public record R01QuarryRunAttributionState(
        int schemaVersion,
        long runId,
        Map<R01QuarryRunContribution, RootClass> contributionOwners
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01QuarryRunAttributionState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("quarry_run_attribution_schema_version")
                            .forGetter(R01QuarryRunAttributionState::schemaVersion),
                    Codec.LONG.fieldOf("run_id")
                            .forGetter(R01QuarryRunAttributionState::runId),
                    Codec.unboundedMap(
                                    R01QuarryRunContribution.CODEC,
                                    RootClass.CODEC
                            )
                            .fieldOf("contribution_owners")
                            .forGetter(R01QuarryRunAttributionState::contributionOwners)
            ).apply(instance, R01QuarryRunAttributionState::new));

    public R01QuarryRunAttributionState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Quarry attribution schema version: " + schemaVersion
            );
        }
        if (runId < 0L) {
            throw new IllegalArgumentException("runId must be non-negative.");
        }
        contributionOwners = Map.copyOf(
                Objects.requireNonNull(contributionOwners, "contributionOwners")
        );
        contributionOwners.forEach((contribution, owner) -> {
            Objects.requireNonNull(contribution, "contribution");
            Objects.requireNonNull(owner, "owner");
        });
        if (runId == 0L && !contributionOwners.isEmpty()) {
            throw new IllegalArgumentException(
                    "Quarry attribution without an active historical run id is invalid."
            );
        }
    }

    public static R01QuarryRunAttributionState initial() {
        return new R01QuarryRunAttributionState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Map.of()
        );
    }

    public R01QuarryRunAttributionState beginRun(long nextRunId) {
        if (nextRunId <= 0L) {
            throw new IllegalArgumentException("nextRunId must be positive.");
        }
        if (runId == nextRunId) {
            return this;
        }
        return new R01QuarryRunAttributionState(
                schemaVersion,
                nextRunId,
                Map.of()
        );
    }

    public R01QuarryRunAttributionState record(
            long expectedRunId,
            R01QuarryRunContribution contribution,
            RootClass owner
    ) {
        if (expectedRunId <= 0L || runId != expectedRunId) {
            throw new IllegalStateException(
                    "Quarry contribution does not belong to the current attributed run."
            );
        }
        Objects.requireNonNull(contribution, "contribution");
        Objects.requireNonNull(owner, "owner");
        if (contributionOwners.containsKey(contribution)) {
            return this;
        }

        Map<R01QuarryRunContribution, RootClass> next =
                new HashMap<>(contributionOwners);
        next.put(contribution, owner);
        return new R01QuarryRunAttributionState(
                schemaVersion,
                runId,
                Map.copyOf(next)
        );
    }

    public Optional<RootClass> completionClass() {
        if (contributionOwners.isEmpty()) {
            return Optional.empty();
        }

        EnumMap<RootClass, Integer> counts = new EnumMap<>(RootClass.class);
        for (RootClass owner : contributionOwners.values()) {
            counts.merge(owner, 1, Integer::sum);
        }

        int highest = counts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        EnumMap<RootClass, Boolean> tied = new EnumMap<>(RootClass.class);
        for (Map.Entry<RootClass, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == highest) {
                tied.put(entry.getKey(), Boolean.TRUE);
            }
        }
        if (tied.size() == 1) {
            return Optional.of(tied.keySet().iterator().next());
        }

        for (R01QuarryRunContribution contribution
                : R01QuarryRunContribution.values()) {
            RootClass owner = contributionOwners.get(contribution);
            if (owner != null && tied.containsKey(owner)) {
                return Optional.of(owner);
            }
        }
        throw new IllegalStateException(
                "Quarry attribution tie could not resolve from recorded contributions."
        );
    }
}

package dev.moonseungjun.openworldrpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record PlayerPassiveProgressState(
        int schemaVersion,
        Map<String, Integer> allocationRanks,
        Set<String> completedInsightIds,
        Set<String> pendingInsightIds,
        long respecSequence,
        Optional<PendingRespec> pendingRespec
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Codec<Map<String, Integer>> ALLOCATION_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 3));
    private static final Codec<Set<String>> STRING_SET_CODEC =
            Codec.STRING.listOf().xmap(Set::copyOf, set -> set.stream().sorted().toList());

    public static final Codec<PlayerPassiveProgressState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .optionalFieldOf("passive_progress_schema_version", CURRENT_SCHEMA_VERSION)
                            .forGetter(PlayerPassiveProgressState::schemaVersion),
                    ALLOCATION_CODEC.optionalFieldOf("allocation_ranks", Map.of())
                            .forGetter(PlayerPassiveProgressState::allocationRanks),
                    STRING_SET_CODEC.optionalFieldOf("completed_insight_ids", Set.of())
                            .forGetter(PlayerPassiveProgressState::completedInsightIds),
                    STRING_SET_CODEC.optionalFieldOf("pending_insight_ids", Set.of())
                            .forGetter(PlayerPassiveProgressState::pendingInsightIds),
                    Codec.LONG.optionalFieldOf("respec_sequence", 0L)
                            .forGetter(PlayerPassiveProgressState::respecSequence),
                    PendingRespec.CODEC.optionalFieldOf("pending_respec")
                            .forGetter(PlayerPassiveProgressState::pendingRespec)
            ).apply(instance, PlayerPassiveProgressState::new));

    public PlayerPassiveProgressState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported passive progress schema version: " + schemaVersion);
        }
        allocationRanks = Map.copyOf(Objects.requireNonNull(allocationRanks, "allocationRanks"));
        completedInsightIds = Set.copyOf(Objects.requireNonNull(completedInsightIds, "completedInsightIds"));
        pendingInsightIds = Set.copyOf(Objects.requireNonNull(pendingInsightIds, "pendingInsightIds"));
        if (respecSequence < 0L) {
            throw new IllegalArgumentException("Passive respec sequence cannot be negative.");
        }
        pendingRespec = Objects.requireNonNull(pendingRespec, "pendingRespec");
        Set<String> overlap = new HashSet<>(completedInsightIds);
        overlap.retainAll(pendingInsightIds);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("Completed insights cannot remain pending: " + overlap);
        }
        if (pendingRespec.isPresent() && pendingRespec.orElseThrow().sequence() != respecSequence) {
            throw new IllegalArgumentException("Pending passive respec sequence mismatch.");
        }
    }

    public static PlayerPassiveProgressState initial() {
        return new PlayerPassiveProgressState(
                CURRENT_SCHEMA_VERSION, Map.of(), Set.of(), Set.of(), 0L, Optional.empty()
        );
    }

    public int allocationRank(String nodeId) {
        return allocationRanks.getOrDefault(nodeId, 0);
    }

    public int completedInsightCount(RootClass rootClass) {
        Objects.requireNonNull(rootClass, "rootClass");
        int count = 0;
        for (String id : completedInsightIds) {
            ClassInsight insight = ClassInsight.byId(id).orElseThrow(
                    () -> new IllegalStateException("Unknown persisted Class Insight: " + id)
            );
            if (insight.rootClass() == rootClass) {
                count++;
            }
        }
        return count;
    }

    public PlayerPassiveProgressState withAllocationRank(String nodeId, int rank) {
        if (nodeId == null || nodeId.isBlank() || rank < 1) {
            throw new IllegalArgumentException("Passive allocation requires node id and positive rank.");
        }
        Map<String, Integer> next = new HashMap<>(allocationRanks);
        next.put(nodeId, rank);
        return new PlayerPassiveProgressState(
                schemaVersion, Map.copyOf(next), completedInsightIds, pendingInsightIds,
                respecSequence, pendingRespec
        );
    }

    public PlayerPassiveProgressState beginInsight(ClassInsight insight) {
        Objects.requireNonNull(insight, "insight");
        if (completedInsightIds.contains(insight.id()) || pendingInsightIds.contains(insight.id())) {
            return this;
        }
        Set<String> next = new HashSet<>(pendingInsightIds);
        next.add(insight.id());
        return new PlayerPassiveProgressState(
                schemaVersion, allocationRanks, completedInsightIds, Set.copyOf(next),
                respecSequence, pendingRespec
        );
    }

    public PlayerPassiveProgressState commitInsight(ClassInsight insight) {
        Objects.requireNonNull(insight, "insight");
        if (completedInsightIds.contains(insight.id())) {
            return this;
        }
        if (!pendingInsightIds.contains(insight.id())) {
            throw new IllegalStateException("Cannot commit a Class Insight that was not pending.");
        }
        Set<String> completed = new HashSet<>(completedInsightIds);
        completed.add(insight.id());
        Set<String> pending = new HashSet<>(pendingInsightIds);
        pending.remove(insight.id());
        return new PlayerPassiveProgressState(
                schemaVersion, allocationRanks, Set.copyOf(completed), Set.copyOf(pending),
                respecSequence, pendingRespec
        );
    }

    public PlayerPassiveProgressState prepareRespec(
            RootClass rootClass,
            Optional<ClassSpecialization> activeBranch,
            long goldCost
    ) {
        Objects.requireNonNull(rootClass, "rootClass");
        activeBranch = Objects.requireNonNull(activeBranch, "activeBranch");
        if (activeBranch.isPresent() && activeBranch.orElseThrow().rootClass() != rootClass) {
            throw new IllegalArgumentException("Passive respec branch/root mismatch.");
        }
        if (goldCost <= 0L) {
            throw new IllegalArgumentException("Passive respec Gold cost must be positive.");
        }
        if (pendingRespec.isPresent()) {
            throw new IllegalStateException("A passive respec is already pending.");
        }
        long sequence = Math.addExact(respecSequence, 1L);
        PendingRespec pending = new PendingRespec(
                "openworld_rpg:passive_respec/" + sequence,
                sequence,
                rootClass,
                activeBranch,
                goldCost
        );
        return new PlayerPassiveProgressState(
                schemaVersion, allocationRanks, completedInsightIds, pendingInsightIds,
                sequence, Optional.of(pending)
        );
    }

    public PlayerPassiveProgressState commitRespec(
            String transactionId,
            Map<String, Integer> nextAllocations
    ) {
        PendingRespec pending = requirePendingRespec(transactionId);
        return new PlayerPassiveProgressState(
                schemaVersion, Map.copyOf(nextAllocations), completedInsightIds, pendingInsightIds,
                pending.sequence(), Optional.empty()
        );
    }

    public PlayerPassiveProgressState cancelRespec(String transactionId) {
        PendingRespec pending = requirePendingRespec(transactionId);
        return new PlayerPassiveProgressState(
                schemaVersion, allocationRanks, completedInsightIds, pendingInsightIds,
                pending.sequence(), Optional.empty()
        );
    }

    public void validateAgainst(ClassPassiveCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        for (var entry : allocationRanks.entrySet()) {
            ClassPassiveNodeSpec node = catalog.require(entry.getKey());
            if (entry.getValue() < 1 || entry.getValue() > node.maxRank()) {
                throw new IllegalStateException("Persisted passive rank exceeds node max: " + entry);
            }
        }
        for (String id : completedInsightIds) {
            if (ClassInsight.byId(id).isEmpty()) {
                throw new IllegalStateException("Unknown completed Class Insight: " + id);
            }
        }
        for (String id : pendingInsightIds) {
            if (ClassInsight.byId(id).isEmpty()) {
                throw new IllegalStateException("Unknown pending Class Insight: " + id);
            }
        }
    }

    private PendingRespec requirePendingRespec(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Passive respec transaction id cannot be blank.");
        }
        PendingRespec pending = pendingRespec.orElseThrow(
                () -> new IllegalStateException("No passive respec is pending.")
        );
        if (!pending.transactionId().equals(transactionId)) {
            throw new IllegalStateException("Cannot mutate a different passive respec transaction.");
        }
        return pending;
    }

    public record PendingRespec(
            String transactionId,
            long sequence,
            RootClass rootClass,
            Optional<ClassSpecialization> activeBranch,
            long goldCost
    ) {
        public static final Codec<PendingRespec> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id").forGetter(PendingRespec::transactionId),
                        Codec.LONG.fieldOf("sequence").forGetter(PendingRespec::sequence),
                        RootClass.CODEC.fieldOf("root_class").forGetter(PendingRespec::rootClass),
                        ClassSpecialization.CODEC.optionalFieldOf("active_branch").forGetter(PendingRespec::activeBranch),
                        Codec.LONG.fieldOf("gold_cost").forGetter(PendingRespec::goldCost)
                ).apply(instance, PendingRespec::new));

        public PendingRespec {
            if (transactionId == null
                    || !transactionId.startsWith("openworld_rpg:passive_respec/")
                    || sequence <= 0L
                    || goldCost <= 0L) {
                throw new IllegalArgumentException("Invalid pending passive respec.");
            }
            Objects.requireNonNull(rootClass, "rootClass");
            activeBranch = Objects.requireNonNull(activeBranch, "activeBranch");
            if (activeBranch.isPresent() && activeBranch.orElseThrow().rootClass() != rootClass) {
                throw new IllegalArgumentException("Pending passive respec branch/root mismatch.");
            }
        }

        public String debitTransactionId() {
            return transactionId + "/gold";
        }
    }
}

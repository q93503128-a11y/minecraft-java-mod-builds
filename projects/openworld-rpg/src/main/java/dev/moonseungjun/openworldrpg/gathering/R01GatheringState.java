package dev.moonseungjun.openworldrpg.gathering;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Persistent server-owned personal gathering state for authored R01 resource nodes. */
public record R01GatheringState(
        int schemaVersion,
        Map<String, Integer> toolTiers,
        Map<String, Integer> masteryXp,
        Set<String> discoveryFlags,
        Map<String, NodeCycle> nodeCycles,
        Map<String, PendingHarvest> pendingHarvests
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<R01GatheringState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("gathering_schema_version")
                            .forGetter(R01GatheringState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("tool_tiers")
                            .forGetter(R01GatheringState::toolTiers),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("mastery_xp")
                            .forGetter(R01GatheringState::masteryXp),
                    STRING_SET_CODEC.fieldOf("discovery_flags")
                            .forGetter(R01GatheringState::discoveryFlags),
                    Codec.unboundedMap(Codec.STRING, NodeCycle.CODEC)
                            .fieldOf("node_cycles")
                            .forGetter(R01GatheringState::nodeCycles),
                    Codec.unboundedMap(Codec.STRING, PendingHarvest.CODEC)
                            .fieldOf("pending_harvests")
                            .forGetter(R01GatheringState::pendingHarvests)
            ).apply(instance, R01GatheringState::new)
    );

    public R01GatheringState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported gathering schema version: " + schemaVersion
            );
        }
        toolTiers = Map.copyOf(Objects.requireNonNull(toolTiers, "toolTiers"));
        masteryXp = Map.copyOf(Objects.requireNonNull(masteryXp, "masteryXp"));
        discoveryFlags = Set.copyOf(
                Objects.requireNonNull(discoveryFlags, "discoveryFlags")
        );
        nodeCycles = Map.copyOf(Objects.requireNonNull(nodeCycles, "nodeCycles"));
        pendingHarvests = Map.copyOf(
                Objects.requireNonNull(pendingHarvests, "pendingHarvests")
        );

        for (R01GatheringRules.ToolFamily family : R01GatheringRules.ToolFamily.values()) {
            int level = toolTiers.getOrDefault(
                    family.id(),
                    R01GatheringRules.ToolTier.FIELD.level()
            );
            R01GatheringRules.ToolTier.fromLevel(level);
        }
        for (Map.Entry<String, Integer> entry : masteryXp.entrySet()) {
            requireStableId(entry.getKey());
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Mastery XP must be non-negative.");
            }
        }
        discoveryFlags.forEach(R01GatheringState::requireStableId);
        for (Map.Entry<String, NodeCycle> entry : nodeCycles.entrySet()) {
            requireStableId(entry.getKey());
            Objects.requireNonNull(entry.getValue(), "node cycle");
        }

        Set<String> pendingNodes = new HashSet<>();
        for (Map.Entry<String, PendingHarvest> entry : pendingHarvests.entrySet()) {
            requireStableId(entry.getKey());
            PendingHarvest pending = Objects.requireNonNull(
                    entry.getValue(),
                    "pending harvest"
            );
            if (!entry.getKey().equals(pending.transactionId())) {
                throw new IllegalArgumentException(
                        "Pending-harvest map key must match transaction id."
                );
            }
            if (!pendingNodes.add(pending.nodeId())) {
                throw new IllegalArgumentException(
                        "A personal node cannot have multiple pending harvests."
                );
            }
        }
    }

    public static R01GatheringState initial() {
        Map<String, Integer> tools = new HashMap<>();
        for (R01GatheringRules.ToolFamily family : R01GatheringRules.ToolFamily.values()) {
            tools.put(family.id(), R01GatheringRules.ToolTier.FIELD.level());
        }
        return new R01GatheringState(
                CURRENT_SCHEMA_VERSION,
                Map.copyOf(tools),
                Map.of(),
                Set.of(),
                Map.of(),
                Map.of()
        );
    }

    public R01GatheringRules.ToolTier toolTier(R01GatheringRules.ToolFamily family) {
        Objects.requireNonNull(family, "family");
        return R01GatheringRules.ToolTier.fromLevel(
                toolTiers.getOrDefault(
                        family.id(),
                        R01GatheringRules.ToolTier.FIELD.level()
                )
        );
    }

    public R01GatheringState withToolTier(
            R01GatheringRules.ToolFamily family,
            R01GatheringRules.ToolTier tier
    ) {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(tier, "tier");
        R01GatheringRules.ToolTier current = toolTier(family);
        if (tier.level() < current.level()) {
            throw new IllegalArgumentException("Permanent gathering tools cannot downgrade.");
        }
        if (tier == current) {
            return this;
        }
        Map<String, Integer> next = new HashMap<>(toolTiers);
        next.put(family.id(), tier.level());
        return copy(
                Map.copyOf(next),
                masteryXp,
                discoveryFlags,
                nodeCycles,
                pendingHarvests
        );
    }

    public int masteryXp(R01GatheringRules.GatheringDiscipline discipline) {
        Objects.requireNonNull(discipline, "discipline");
        return masteryXp.getOrDefault(discipline.id(), 0);
    }

    public int masteryRank(R01GatheringRules.GatheringDiscipline discipline) {
        return R01GatheringRules.masteryRankForXp(masteryXp(discipline));
    }

    public boolean isNodeAvailable(String nodeId, long activeTicks) {
        requireStableId(nodeId);
        if (activeTicks < 0L) {
            throw new IllegalArgumentException("activeTicks must be non-negative.");
        }
        boolean pending = pendingHarvests.values().stream()
                .anyMatch(value -> value.nodeId().equals(nodeId));
        if (pending) {
            return false;
        }
        NodeCycle cycle = nodeCycles.get(nodeId);
        return cycle == null || activeTicks >= cycle.availableAfterActiveTick();
    }

    public Optional<PendingHarvest> pendingHarvest(String transactionId) {
        requireStableId(transactionId);
        return Optional.ofNullable(pendingHarvests.get(transactionId));
    }

    public BeginHarvestResult beginHarvest(
            String nodeId,
            String resourceId,
            int quantity,
            long activeTicks
    ) {
        requireStableId(nodeId);
        R01GatheringRules.ResourceDefinition definition =
                R01GatheringRules.requireResource(resourceId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Gather quantity must be positive.");
        }
        if (!isNodeAvailable(nodeId, activeTicks)) {
            throw new IllegalStateException("Personal gathering node is not available.");
        }

        NodeCycle previous = nodeCycles.getOrDefault(nodeId, NodeCycle.initial());
        long generation = Math.addExact(previous.generation(), 1L);
        long availableAfter = Math.addExact(
                activeTicks,
                definition.respawnActiveTicks()
        );
        String transactionId =
                R01GatheringRules.deliveryTransactionId(nodeId, generation);
        String discoveryFlag = R01GatheringRules.discoveryFlag(resourceId);
        int masteryAward = definition.baseMasteryXp()
                + (discoveryFlags.contains(discoveryFlag) ? 0 : 10);

        PendingHarvest pending = new PendingHarvest(
                transactionId,
                nodeId,
                resourceId,
                quantity,
                definition.discipline().id(),
                masteryAward,
                discoveryFlag,
                generation
        );

        Map<String, NodeCycle> nextCycles = new HashMap<>(nodeCycles);
        nextCycles.put(nodeId, new NodeCycle(generation, availableAfter));
        Map<String, PendingHarvest> nextPending = new HashMap<>(pendingHarvests);
        nextPending.put(transactionId, pending);

        return new BeginHarvestResult(
                copy(
                        toolTiers,
                        masteryXp,
                        discoveryFlags,
                        Map.copyOf(nextCycles),
                        Map.copyOf(nextPending)
                ),
                pending
        );
    }

    public R01GatheringState finalizeHarvest(String transactionId) {
        requireStableId(transactionId);
        PendingHarvest pending = pendingHarvests.get(transactionId);
        if (pending == null) {
            return this;
        }

        Map<String, Integer> nextMastery = new HashMap<>(masteryXp);
        int current = nextMastery.getOrDefault(pending.disciplineId(), 0);
        nextMastery.put(
                pending.disciplineId(),
                Math.addExact(current, pending.masteryXpAward())
        );

        Set<String> nextDiscoveries = new HashSet<>(discoveryFlags);
        nextDiscoveries.add(pending.discoveryFlag());

        Map<String, PendingHarvest> nextPending = new HashMap<>(pendingHarvests);
        nextPending.remove(transactionId);

        return copy(
                toolTiers,
                Map.copyOf(nextMastery),
                Set.copyOf(nextDiscoveries),
                nodeCycles,
                Map.copyOf(nextPending)
        );
    }

    private R01GatheringState copy(
            Map<String, Integer> nextToolTiers,
            Map<String, Integer> nextMasteryXp,
            Set<String> nextDiscoveries,
            Map<String, NodeCycle> nextNodeCycles,
            Map<String, PendingHarvest> nextPending
    ) {
        return new R01GatheringState(
                schemaVersion,
                nextToolTiers,
                nextMasteryXp,
                nextDiscoveries,
                nextNodeCycles,
                nextPending
        );
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable namespaced id.");
        }
    }

    public record NodeCycle(
            long generation,
            long availableAfterActiveTick
    ) {
        public static final Codec<NodeCycle> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("generation").forGetter(NodeCycle::generation),
                        Codec.LONG.fieldOf("available_after_active_tick")
                                .forGetter(NodeCycle::availableAfterActiveTick)
                ).apply(instance, NodeCycle::new)
        );

        public NodeCycle {
            if (generation < 0L || availableAfterActiveTick < 0L) {
                throw new IllegalArgumentException(
                        "Node generation/cooldown must be non-negative."
                );
            }
        }

        public static NodeCycle initial() {
            return new NodeCycle(0L, 0L);
        }
    }

    public record PendingHarvest(
            String transactionId,
            String nodeId,
            String resourceId,
            int quantity,
            String disciplineId,
            int masteryXpAward,
            String discoveryFlag,
            long generation
    ) {
        public static final Codec<PendingHarvest> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                Codec.STRING.fieldOf("transaction_id")
                                        .forGetter(PendingHarvest::transactionId),
                                Codec.STRING.fieldOf("node_id")
                                        .forGetter(PendingHarvest::nodeId),
                                Codec.STRING.fieldOf("resource_id")
                                        .forGetter(PendingHarvest::resourceId),
                                Codec.INT.fieldOf("quantity")
                                        .forGetter(PendingHarvest::quantity),
                                Codec.STRING.fieldOf("discipline_id")
                                        .forGetter(PendingHarvest::disciplineId),
                                Codec.INT.fieldOf("mastery_xp_award")
                                        .forGetter(PendingHarvest::masteryXpAward),
                                Codec.STRING.fieldOf("discovery_flag")
                                        .forGetter(PendingHarvest::discoveryFlag),
                                Codec.LONG.fieldOf("generation")
                                        .forGetter(PendingHarvest::generation)
                        ).apply(instance, PendingHarvest::new)
                );

        public PendingHarvest {
            requireStableId(transactionId);
            requireStableId(nodeId);
            R01GatheringRules.requireResource(resourceId);
            requireStableId(disciplineId);
            requireStableId(discoveryFlag);
            if (quantity <= 0 || masteryXpAward <= 0 || generation <= 0L) {
                throw new IllegalArgumentException(
                        "Pending harvest values must be positive."
                );
            }
        }
    }

    public record BeginHarvestResult(
            R01GatheringState state,
            PendingHarvest pending
    ) {
        public BeginHarvestResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(pending, "pending");
        }
    }
}

package dev.moonseungjun.openworldrpg.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent personal R01 fishing state.
 *
 * <p>Candidate identity survives miss/relog; a successful Hook consumes the personal spot charge
 * immediately; an interrupted hooked attempt is finalized as a failed catch without refund.</p>
 */
public record R01FishingState(
        int schemaVersion,
        Map<String, SpotCycle> spotCycles,
        Map<String, PendingCandidate> pendingCandidates,
        Map<String, PendingReward> pendingRewards
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<R01FishingRules.Rarity> RARITY_CODEC =
            Codec.STRING.xmap(
                    R01FishingRules.Rarity::parse,
                    value -> value.name().toLowerCase(java.util.Locale.ROOT)
            );

    private static final Codec<R01FishingRules.CatchCandidate> CANDIDATE_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("fish_slot_id")
                            .forGetter(R01FishingRules.CatchCandidate::fishSlotId),
                    RARITY_CODEC.fieldOf("rarity")
                            .forGetter(R01FishingRules.CatchCandidate::rarity),
                    Codec.DOUBLE.fieldOf("primary_size_percentile")
                            .forGetter(R01FishingRules.CatchCandidate::primarySizePercentile),
                    Codec.DOUBLE.fieldOf("secondary_size_percentile")
                            .forGetter(R01FishingRules.CatchCandidate::secondarySizePercentile),
                    Codec.DOUBLE.fieldOf("size_cm")
                            .forGetter(R01FishingRules.CatchCandidate::sizeCm),
                    Codec.BOOL.fieldOf("trophy")
                            .forGetter(R01FishingRules.CatchCandidate::trophy),
                    Codec.INT.fieldOf("sale_value_gold")
                            .forGetter(R01FishingRules.CatchCandidate::saleValueGold),
                    Codec.DOUBLE.fieldOf("bite_delay_seconds")
                            .forGetter(R01FishingRules.CatchCandidate::biteDelaySeconds),
                    Codec.DOUBLE.fieldOf("hook_window_seconds")
                            .forGetter(R01FishingRules.CatchCandidate::hookWindowSeconds)
            ).apply(instance, R01FishingRules.CatchCandidate::new));

    public static final Codec<R01FishingState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("fishing_schema_version")
                            .forGetter(R01FishingState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, SpotCycle.CODEC)
                            .fieldOf("spot_cycles")
                            .forGetter(R01FishingState::spotCycles),
                    Codec.unboundedMap(Codec.STRING, PendingCandidate.CODEC)
                            .fieldOf("pending_candidates")
                            .forGetter(R01FishingState::pendingCandidates),
                    Codec.unboundedMap(Codec.STRING, PendingReward.CODEC)
                            .fieldOf("pending_rewards")
                            .forGetter(R01FishingState::pendingRewards)
            ).apply(instance, R01FishingState::new));

    public R01FishingState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported fishing schema version: " + schemaVersion
            );
        }
        spotCycles = Map.copyOf(Objects.requireNonNull(spotCycles, "spotCycles"));
        pendingCandidates = Map.copyOf(
                Objects.requireNonNull(pendingCandidates, "pendingCandidates")
        );
        pendingRewards = Map.copyOf(
                Objects.requireNonNull(pendingRewards, "pendingRewards")
        );

        spotCycles.forEach((spotId, cycle) -> {
            requireStableId(spotId);
            Objects.requireNonNull(cycle, "spot cycle");
        });
        pendingCandidates.forEach((spotId, pending) -> {
            requireStableId(spotId);
            Objects.requireNonNull(pending, "pending candidate");
            if (!spotId.equals(pending.spotId())) {
                throw new IllegalArgumentException(
                        "Pending fishing candidate key must match spot id."
                );
            }
        });
        pendingRewards.forEach((transactionId, reward) -> {
            requireStableId(transactionId);
            Objects.requireNonNull(reward, "pending fishing reward");
            if (!transactionId.equals(reward.transactionId())) {
                throw new IllegalArgumentException(
                        "Pending fishing reward key must match transaction id."
                );
            }
        });
    }

    public static R01FishingState initial() {
        return new R01FishingState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public Optional<PendingCandidate> pendingCandidate(String spotId) {
        requireStableId(spotId);
        return Optional.ofNullable(pendingCandidates.get(spotId));
    }

    public Optional<PendingReward> pendingReward(String transactionId) {
        requireStableId(transactionId);
        return Optional.ofNullable(pendingRewards.get(transactionId));
    }

    public boolean isSpotAvailable(
            String spotId,
            R01FishingRules.SpotTier tier,
            long activeTicks
    ) {
        requireStableId(spotId);
        Objects.requireNonNull(tier, "tier");
        requireActiveTicks(activeTicks);

        PendingCandidate pending = pendingCandidates.get(spotId);
        if (pending != null) {
            return !pending.hooked();
        }

        SpotCycle cycle = spotCycles.get(spotId);
        if (cycle == null) {
            return true;
        }
        int capacity = R01FishingRules.personalCatchCapacity(tier);
        if (cycle.consumedCharges() < capacity) {
            return true;
        }
        return activeTicks >= cycle.availableAfterActiveTick();
    }

    public PrepareResult prepareCandidate(
            long worldSeed,
            String playerUuid,
            String spotId,
            R01FishingRules.SpotTier tier,
            long activeTicks,
            int masteryRank
    ) {
        requireStableId(spotId);
        Objects.requireNonNull(tier, "tier");
        requireActiveTicks(activeTicks);

        PendingCandidate existing = pendingCandidates.get(spotId);
        if (existing != null) {
            if (existing.hooked()) {
                throw new IllegalStateException(
                        "Fishing Hook is already committed for this spot."
                );
            }
            return new PrepareResult(this, existing);
        }

        SpotCycle cycle = refreshedCycle(spotId, tier, activeTicks);
        if (cycle.consumedCharges()
                >= R01FishingRules.personalCatchCapacity(tier)) {
            throw new IllegalStateException(
                    "Personal fishing spot is still depleted."
            );
        }

        R01FishingRules.CatchCandidate candidate =
                R01FishingRules.generateCandidate(
                        worldSeed,
                        playerUuid,
                        spotId,
                        tier,
                        cycle.cycleIndex(),
                        cycle.catchOrdinal(),
                        masteryRank
                );
        PendingCandidate pending = new PendingCandidate(
                spotId,
                cycle.cycleIndex(),
                cycle.catchOrdinal(),
                false,
                candidate
        );

        Map<String, SpotCycle> nextCycles = new HashMap<>(spotCycles);
        nextCycles.put(spotId, cycle);
        Map<String, PendingCandidate> nextCandidates =
                new HashMap<>(pendingCandidates);
        nextCandidates.put(spotId, pending);

        return new PrepareResult(
                copy(
                        Map.copyOf(nextCycles),
                        Map.copyOf(nextCandidates),
                        pendingRewards
                ),
                pending
        );
    }

    public R01FishingState missHook(String spotId) {
        requireStableId(spotId);
        PendingCandidate pending = pendingCandidates.get(spotId);
        if (pending == null || pending.hooked()) {
            return this;
        }
        return this;
    }

    public R01FishingState commitHook(
            String spotId,
            R01FishingRules.SpotTier tier,
            long activeTicks
    ) {
        requireStableId(spotId);
        Objects.requireNonNull(tier, "tier");
        requireActiveTicks(activeTicks);

        PendingCandidate pending = pendingCandidates.get(spotId);
        if (pending == null) {
            throw new IllegalStateException(
                    "Cannot commit a fishing Hook without a pending candidate."
            );
        }
        if (pending.hooked()) {
            return this;
        }

        SpotCycle cycle = spotCycles.get(spotId);
        if (cycle == null
                || cycle.cycleIndex() != pending.cycleIndex()
                || cycle.catchOrdinal() != pending.catchOrdinal()) {
            throw new IllegalStateException(
                    "Fishing candidate no longer matches its spot cycle."
            );
        }

        int capacity = R01FishingRules.personalCatchCapacity(tier);
        int consumed = Math.addExact(cycle.consumedCharges(), 1);
        if (consumed > capacity) {
            throw new IllegalStateException(
                    "Fishing Hook would over-consume the personal spot."
            );
        }
        long availableAfter = consumed == capacity
                ? Math.addExact(
                        activeTicks,
                        R01FishingRules.respawnActiveTicks(tier)
                )
                : cycle.availableAfterActiveTick();

        Map<String, SpotCycle> nextCycles = new HashMap<>(spotCycles);
        nextCycles.put(
                spotId,
                new SpotCycle(
                        cycle.cycleIndex(),
                        cycle.catchOrdinal(),
                        consumed,
                        availableAfter
                )
        );
        Map<String, PendingCandidate> nextCandidates =
                new HashMap<>(pendingCandidates);
        nextCandidates.put(spotId, pending.withHooked(true));

        return copy(
                Map.copyOf(nextCycles),
                Map.copyOf(nextCandidates),
                pendingRewards
        );
    }

    public R01FishingState resolveHookFailure(String spotId) {
        return resolveHookedAttempt(spotId, false).state();
    }

    public CatchResolution resolveCatchSuccess(String spotId) {
        return resolveHookedAttempt(spotId, true);
    }

    public R01FishingState reconcileInterruptedHooks() {
        Map<String, SpotCycle> nextCycles = new HashMap<>(spotCycles);
        Map<String, PendingCandidate> nextCandidates =
                new HashMap<>(pendingCandidates);
        boolean changed = false;

        for (PendingCandidate pending : pendingCandidates.values()) {
            if (!pending.hooked()) {
                continue;
            }
            SpotCycle cycle = nextCycles.get(pending.spotId());
            if (cycle == null
                    || cycle.cycleIndex() != pending.cycleIndex()
                    || cycle.catchOrdinal() != pending.catchOrdinal()) {
                throw new IllegalStateException(
                        "Interrupted fishing Hook lost its spot cycle."
                );
            }
            nextCycles.put(
                    pending.spotId(),
                    cycle.withCatchOrdinal(
                            Math.addExact(cycle.catchOrdinal(), 1)
                    )
            );
            nextCandidates.remove(pending.spotId());
            changed = true;
        }

        return changed
                ? copy(
                        Map.copyOf(nextCycles),
                        Map.copyOf(nextCandidates),
                        pendingRewards
                )
                : this;
    }

    public R01FishingState clearPendingReward(String transactionId) {
        requireStableId(transactionId);
        if (!pendingRewards.containsKey(transactionId)) {
            return this;
        }
        Map<String, PendingReward> next = new HashMap<>(pendingRewards);
        next.remove(transactionId);
        return copy(
                spotCycles,
                pendingCandidates,
                Map.copyOf(next)
        );
    }

    private CatchResolution resolveHookedAttempt(
            String spotId,
            boolean successfulCatch
    ) {
        requireStableId(spotId);
        PendingCandidate pending = pendingCandidates.get(spotId);
        if (pending == null || !pending.hooked()) {
            throw new IllegalStateException(
                    "Fishing resolution requires a committed Hook."
            );
        }
        SpotCycle cycle = spotCycles.get(spotId);
        if (cycle == null
                || cycle.cycleIndex() != pending.cycleIndex()
                || cycle.catchOrdinal() != pending.catchOrdinal()) {
            throw new IllegalStateException(
                    "Fishing resolution no longer matches its spot cycle."
            );
        }

        Map<String, SpotCycle> nextCycles = new HashMap<>(spotCycles);
        nextCycles.put(
                spotId,
                cycle.withCatchOrdinal(
                        Math.addExact(cycle.catchOrdinal(), 1)
                )
        );

        Map<String, PendingCandidate> nextCandidates =
                new HashMap<>(pendingCandidates);
        nextCandidates.remove(spotId);

        PendingReward reward = null;
        Map<String, PendingReward> nextRewards =
                new HashMap<>(pendingRewards);
        if (successfulCatch) {
            String transactionId = rewardTransactionId(
                    spotId,
                    pending.cycleIndex(),
                    pending.catchOrdinal()
            );
            reward = new PendingReward(
                    transactionId,
                    spotId,
                    pending.cycleIndex(),
                    pending.catchOrdinal(),
                    pending.candidate()
            );
            PendingReward previous = nextRewards.putIfAbsent(
                    transactionId,
                    reward
            );
            if (previous != null && !previous.equals(reward)) {
                throw new IllegalStateException(
                        "Fishing reward transaction id collision."
                );
            }
        }

        R01FishingState next = copy(
                Map.copyOf(nextCycles),
                Map.copyOf(nextCandidates),
                Map.copyOf(nextRewards)
        );
        return new CatchResolution(next, Optional.ofNullable(reward));
    }

    private SpotCycle refreshedCycle(
            String spotId,
            R01FishingRules.SpotTier tier,
            long activeTicks
    ) {
        SpotCycle cycle = spotCycles.getOrDefault(
                spotId,
                SpotCycle.initial()
        );
        int capacity = R01FishingRules.personalCatchCapacity(tier);
        if (cycle.consumedCharges() < capacity) {
            return cycle;
        }
        if (activeTicks < cycle.availableAfterActiveTick()) {
            return cycle;
        }
        return new SpotCycle(
                Math.addExact(cycle.cycleIndex(), 1L),
                0,
                0,
                0L
        );
    }

    private R01FishingState copy(
            Map<String, SpotCycle> cycles,
            Map<String, PendingCandidate> candidates,
            Map<String, PendingReward> rewards
    ) {
        return new R01FishingState(
                schemaVersion,
                cycles,
                candidates,
                rewards
        );
    }

    private static String rewardTransactionId(
            String spotId,
            long cycleIndex,
            int catchOrdinal
    ) {
        return "openworld_rpg:r01/fishing_reward/"
                + spotId.substring(spotId.indexOf(':') + 1)
                + "/" + cycleIndex
                + "/" + catchOrdinal;
    }

    private static void requireActiveTicks(long activeTicks) {
        if (activeTicks < 0L) {
            throw new IllegalArgumentException(
                    "Fishing active-world time must be non-negative."
            );
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced fishing id."
            );
        }
    }

    public record SpotCycle(
            long cycleIndex,
            int catchOrdinal,
            int consumedCharges,
            long availableAfterActiveTick
    ) {
        public static final Codec<SpotCycle> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("cycle_index")
                                .forGetter(SpotCycle::cycleIndex),
                        Codec.INT.fieldOf("catch_ordinal_in_cycle")
                                .forGetter(SpotCycle::catchOrdinal),
                        Codec.INT.fieldOf("consumed_charges")
                                .forGetter(SpotCycle::consumedCharges),
                        Codec.LONG.fieldOf("available_after_active_tick")
                                .forGetter(SpotCycle::availableAfterActiveTick)
                ).apply(instance, SpotCycle::new));

        public SpotCycle {
            if (cycleIndex < 0L
                    || catchOrdinal < 0
                    || consumedCharges < 0
                    || availableAfterActiveTick < 0L) {
                throw new IllegalArgumentException(
                        "Fishing spot-cycle values must be non-negative."
                );
            }
        }

        public static SpotCycle initial() {
            return new SpotCycle(0L, 0, 0, 0L);
        }

        public SpotCycle withCatchOrdinal(int nextOrdinal) {
            return new SpotCycle(
                    cycleIndex,
                    nextOrdinal,
                    consumedCharges,
                    availableAfterActiveTick
            );
        }
    }

    public record PendingCandidate(
            String spotId,
            long cycleIndex,
            int catchOrdinal,
            boolean hooked,
            R01FishingRules.CatchCandidate candidate
    ) {
        public static final Codec<PendingCandidate> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("spot_id")
                                .forGetter(PendingCandidate::spotId),
                        Codec.LONG.fieldOf("cycle_index")
                                .forGetter(PendingCandidate::cycleIndex),
                        Codec.INT.fieldOf("catch_ordinal")
                                .forGetter(PendingCandidate::catchOrdinal),
                        Codec.BOOL.fieldOf("hooked")
                                .forGetter(PendingCandidate::hooked),
                        CANDIDATE_CODEC.fieldOf("candidate")
                                .forGetter(PendingCandidate::candidate)
                ).apply(instance, PendingCandidate::new));

        public PendingCandidate {
            requireStableId(spotId);
            if (cycleIndex < 0L || catchOrdinal < 0) {
                throw new IllegalArgumentException(
                        "Fishing pending candidate cycle/ordinal must be non-negative."
                );
            }
            Objects.requireNonNull(candidate, "candidate");
        }

        public PendingCandidate withHooked(boolean nextHooked) {
            return new PendingCandidate(
                    spotId,
                    cycleIndex,
                    catchOrdinal,
                    nextHooked,
                    candidate
            );
        }
    }

    public record PendingReward(
            String transactionId,
            String spotId,
            long cycleIndex,
            int catchOrdinal,
            R01FishingRules.CatchCandidate candidate
    ) {
        public static final Codec<PendingReward> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingReward::transactionId),
                        Codec.STRING.fieldOf("spot_id")
                                .forGetter(PendingReward::spotId),
                        Codec.LONG.fieldOf("cycle_index")
                                .forGetter(PendingReward::cycleIndex),
                        Codec.INT.fieldOf("catch_ordinal")
                                .forGetter(PendingReward::catchOrdinal),
                        CANDIDATE_CODEC.fieldOf("candidate")
                                .forGetter(PendingReward::candidate)
                ).apply(instance, PendingReward::new));

        public PendingReward {
            requireStableId(transactionId);
            requireStableId(spotId);
            if (cycleIndex < 0L || catchOrdinal < 0) {
                throw new IllegalArgumentException(
                        "Fishing reward cycle/ordinal must be non-negative."
                );
            }
            Objects.requireNonNull(candidate, "candidate");
        }
    }

    public record PrepareResult(
            R01FishingState state,
            PendingCandidate pending
    ) {
        public PrepareResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(pending, "pending");
        }
    }

    public record CatchResolution(
            R01FishingState state,
            Optional<PendingReward> reward
    ) {
        public CatchResolution {
            Objects.requireNonNull(state, "state");
            reward = Objects.requireNonNull(reward, "reward");
        }
    }
}

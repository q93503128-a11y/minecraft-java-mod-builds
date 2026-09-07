package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.RewardTableDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/** Deterministic server-side reward rolling and immutable application to PlayerProgress. */
public final class RewardService {
    public record RewardGrant(long coin, long essence, Map<String, Integer> shards) {
        public RewardGrant {
            if (coin < 0 || essence < 0) throw new IllegalArgumentException("reward currencies must be >= 0");
            shards = shards == null ? Map.of() : Map.copyOf(shards);
            for (Map.Entry<String, Integer> entry : shards.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 0) {
                    throw new IllegalArgumentException("invalid shard grant");
                }
            }
        }
    }

    public record Applied(RewardGrant grant, PlayerProgress state) {}

    private final DefinitionRegistry definitions;

    public RewardService(DefinitionRegistry definitions) {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        this.definitions = definitions;
    }

    /**
     * Reward semantics:
     * - COIN/ESSENCE rows are independent chance rolls.
     * - successful CHARACTER_SHARD rows form one candidate pool; exactly one candidate is selected by weight.
     * This gives weight a concrete meaning and avoids awarding several character shard types from one encounter roll.
     */
    public RewardGrant roll(String rewardTableId, long seed) {
        RewardTableDefinition table = definitions.rewards().get(rewardTableId);
        if (table == null) throw new IllegalArgumentException("unknown reward table " + rewardTableId);
        SplittableRandom random = new SplittableRandom(seed);
        long coin = 0;
        long essence = 0;
        List<RewardTableDefinition.Roll> shardCandidates = new ArrayList<>();

        for (RewardTableDefinition.Roll roll : table.rolls()) {
            boolean passed = random.nextDouble() < roll.chance();
            if (!passed) continue;
            switch (roll.type()) {
                case "COIN" -> coin = Math.addExact(coin, amount(random, roll));
                case "ESSENCE" -> essence = Math.addExact(essence, amount(random, roll));
                case "CHARACTER_SHARD" -> shardCandidates.add(roll);
                default -> throw new IllegalStateException("validated reward type became unsupported: " + roll.type());
            }
        }

        Map<String, Integer> shards = new LinkedHashMap<>();
        if (!shardCandidates.isEmpty()) {
            int totalWeight = shardCandidates.stream().mapToInt(RewardTableDefinition.Roll::weight).sum();
            if (totalWeight <= 0) throw new IllegalStateException("validated shard candidate pool has no positive weight");
            int ticket = random.nextInt(totalWeight);
            RewardTableDefinition.Roll selected = shardCandidates.getLast();
            int cursor = 0;
            for (RewardTableDefinition.Roll candidate : shardCandidates) {
                cursor += candidate.weight();
                if (ticket < cursor) { selected = candidate; break; }
            }
            shards.put(selected.character(), amount(random, selected));
        }
        return new RewardGrant(coin, essence, shards);
    }

    public Applied rollAndApply(PlayerProgress state, String rewardTableId, long seed) {
        RewardGrant grant = roll(rewardTableId, seed);
        return new Applied(grant, apply(state, grant));
    }

    public PlayerProgress apply(PlayerProgress state, RewardGrant grant) {
        if (state == null || grant == null) throw new IllegalArgumentException("state/grant required");
        Map<String, Integer> shards = new LinkedHashMap<>(state.shards());
        for (Map.Entry<String, Integer> entry : grant.shards().entrySet()) {
            if (!definitions.characters().containsKey(entry.getKey())) {
                throw new IllegalArgumentException("reward references unknown character " + entry.getKey());
            }
            shards.put(entry.getKey(), Math.addExact(shards.getOrDefault(entry.getKey(), 0), entry.getValue()));
        }
        return new PlayerProgress(
                state.schemaVersion(),
                Math.addExact(state.coin(), grant.coin()),
                Math.addExact(state.essence(), grant.essence()),
                shards, state.characters(), state.party(), state.partyCapacity());
    }

    private static int amount(SplittableRandom random, RewardTableDefinition.Roll roll) {
        if (roll.min() == roll.max()) return roll.min();
        return (int) random.nextLong(roll.min(), (long) roll.max() + 1L);
    }
}

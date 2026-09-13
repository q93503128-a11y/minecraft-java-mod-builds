package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;
import kr.moonseungjun.turnboundre.data.RewardTableDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Thin server persistence facade. Every operation captures one immutable definition snapshot before reading/writing. */
public final class PlayerProgressStore {
    public static final String DEFAULT_PROGRESSION_ID = "turnbound_re:default_progression";

    private final DefinitionRepository definitions;

    public PlayerProgressStore(DefinitionRepository definitions) {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        this.definitions = definitions;
    }

    public PlayerProgress getOrCreate(MinecraftServer server, UUID playerId) {
        Context context = context();
        TurnboundProgressSavedData data = data(server);
        return data.get(playerId).orElseGet(() -> {
            PlayerProgress created = freshWithStarterParty(context);
            data.put(playerId, created);
            return created;
        });
    }

    public PlayerProgress grantCurrency(MinecraftServer server, UUID playerId, long coin, long essence) {
        Context context = context();
        TurnboundProgressSavedData data = data(server);
        PlayerProgress current = currentOrFresh(data, playerId, context);
        PlayerProgress next = new ProgressionService(context.registry(), context.tuning()).grantCurrency(current, coin, essence);
        data.put(playerId, next);
        return next;
    }

    public PlayerProgress grantShards(MinecraftServer server, UUID playerId, String characterId, int amount) {
        Context context = context();
        TurnboundProgressSavedData data = data(server);
        PlayerProgress current = currentOrFresh(data, playerId, context);
        PlayerProgress next = new ProgressionService(context.registry(), context.tuning()).grantShards(current, characterId, amount);
        data.put(playerId, next);
        return next;
    }

    public ProgressionService.Result unlock(MinecraftServer server, UUID playerId, String characterId) {
        return transact(server, playerId, service -> state -> service.unlock(state, characterId));
    }

    public ProgressionService.Result levelUp(MinecraftServer server, UUID playerId, String characterId) {
        return transact(server, playerId, service -> state -> service.levelUp(state, characterId));
    }

    public ProgressionService.Result ascend(MinecraftServer server, UUID playerId, String characterId) {
        return transact(server, playerId, service -> state -> service.ascend(state, characterId));
    }

    public ProgressionService.Result setParty(MinecraftServer server, UUID playerId, List<String> characterIds) {
        return transact(server, playerId, service -> state -> service.setParty(state, characterIds));
    }

    public RewardService.Applied applyReward(MinecraftServer server, UUID playerId, String rewardTableId, long seed) {
        Context context = context();
        RewardTableDefinition table = context.registry().rewards().get(rewardTableId);
        if (table == null) throw new IllegalArgumentException("unknown reward table " + rewardTableId);
        return applyReward(server, playerId, table, seed);
    }

    /** Persists a reward using the immutable table captured at encounter open, not a table reloaded later. */
    public RewardService.Applied applyReward(
            MinecraftServer server,
            UUID playerId,
            RewardTableDefinition rewardTable,
            long seed
    ) {
        return applyRewardInternal(server, playerId, rewardTable, seed, "");
    }

    /**
     * Persists the captured reward and one non-repeatable world encounter completion in a single SavedData replacement.
     * No reward-only intermediate state is written, so claim retry cannot observe half of the transaction.
     */
    public RewardService.Applied applyRewardAndCompleteEncounter(
            MinecraftServer server,
            UUID playerId,
            RewardTableDefinition rewardTable,
            long seed,
            String locator
    ) {
        if (locator == null || locator.isBlank()) throw new IllegalArgumentException("locator must not be blank");
        return applyRewardInternal(server, playerId, rewardTable, seed, locator);
    }

    private RewardService.Applied applyRewardInternal(
            MinecraftServer server,
            UUID playerId,
            RewardTableDefinition rewardTable,
            long seed,
            String completedLocator
    ) {
        if (rewardTable == null) throw new IllegalArgumentException("rewardTable must not be null");
        Context context = context();
        TurnboundProgressSavedData data = data(server);
        PlayerProgress current = currentOrFresh(data, playerId, context);
        RewardService.Applied applied = applyRewardToState(
                context.registry(), current, rewardTable, seed, completedLocator);
        data.put(playerId, applied.state());
        return applied;
    }

    /** Pure transaction composer used by persistence and focused tests. */
    static RewardService.Applied applyRewardToState(
            DefinitionRegistry registry,
            PlayerProgress current,
            RewardTableDefinition rewardTable,
            long seed,
            String completedLocator
    ) {
        if (registry == null || current == null || rewardTable == null) {
            throw new IllegalArgumentException("registry/current/rewardTable required");
        }
        RewardService.Applied applied = new RewardService(registry).rollAndApply(current, rewardTable, seed);
        if (completedLocator == null || completedLocator.isBlank()) return applied;
        PlayerProgress completed = applied.state().completeEncounterLocator(completedLocator);
        return new RewardService.Applied(applied.grant(), completed);
    }

    private ProgressionService.Result transact(
            MinecraftServer server,
            UUID playerId,
            java.util.function.Function<ProgressionService, java.util.function.Function<PlayerProgress, ProgressionService.Result>> operation
    ) {
        Context context = context();
        TurnboundProgressSavedData data = data(server);
        PlayerProgress current = currentOrFresh(data, playerId, context);
        ProgressionService service = new ProgressionService(context.registry(), context.tuning());
        ProgressionService.Result result = operation.apply(service).apply(current);
        if (result.accepted()) data.put(playerId, result.state());
        return result;
    }

    private static PlayerProgress currentOrFresh(TurnboundProgressSavedData data, UUID playerId, Context context) {
        return data.get(playerId).orElseGet(() -> {
            PlayerProgress created = freshWithStarterParty(context);
            data.put(playerId, created);
            return created;
        });
    }

    private static PlayerProgress freshWithStarterParty(Context context) {
        Map<String, CharacterProgress> characters = new LinkedHashMap<>();
        for (String characterId : context.tuning().starterParty()) {
            CharacterDefinition definition = context.registry().characters().get(characterId);
            if (definition == null) {
                throw new IllegalStateException("validated starter character disappeared: " + characterId);
            }
            characters.put(characterId, new CharacterProgress(
                    characterId, definition.originStar(), definition.originStar(), 1));
        }
        return new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA,
                0L,
                0L,
                Map.of(),
                characters,
                context.tuning().starterParty(),
                context.tuning().partyCapacity());
    }

    private Context context() {
        DefinitionRepository.Snapshot snapshot = definitions.snapshot();
        DefinitionRegistry registry = snapshot.registry();
        ProgressionDefinition tuning = registry.progressions().get(DEFAULT_PROGRESSION_ID);
        if (tuning == null) throw new IllegalStateException("progression definitions are not loaded: " + DEFAULT_PROGRESSION_ID);
        return new Context(registry, tuning);
    }

    private static TurnboundProgressSavedData data(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server must not be null");
        return server.getDataStorage().computeIfAbsent(TurnboundProgressSavedData.TYPE);
    }

    private record Context(DefinitionRegistry registry, ProgressionDefinition tuning) {}
}

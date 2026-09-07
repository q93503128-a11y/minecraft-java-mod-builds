package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.List;
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
        return data(server).getOrCreate(playerId, context.tuning());
    }

    public PlayerProgress grantCurrency(MinecraftServer server, UUID playerId, long coin, long essence) {
        Context context = context();
        TurnboundProgressSavedData data = data(server);
        PlayerProgress current = data.getOrCreate(playerId, context.tuning());
        PlayerProgress next = new ProgressionService(context.registry(), context.tuning()).grantCurrency(current, coin, essence);
        data.put(playerId, next);
        return next;
    }

    public PlayerProgress grantShards(MinecraftServer server, UUID playerId, String characterId, int amount) {
        Context context = context();
        TurnboundProgressSavedData data = data(server);
        PlayerProgress current = data.getOrCreate(playerId, context.tuning());
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
        TurnboundProgressSavedData data = data(server);
        PlayerProgress current = data.getOrCreate(playerId, context.tuning());
        RewardService.Applied applied = new RewardService(context.registry()).rollAndApply(current, rewardTableId, seed);
        data.put(playerId, applied.state());
        return applied;
    }

    private ProgressionService.Result transact(
            MinecraftServer server,
            UUID playerId,
            java.util.function.Function<ProgressionService, java.util.function.Function<PlayerProgress, ProgressionService.Result>> operation
    ) {
        Context context = context();
        TurnboundProgressSavedData data = data(server);
        PlayerProgress current = data.getOrCreate(playerId, context.tuning());
        ProgressionService service = new ProgressionService(context.registry(), context.tuning());
        ProgressionService.Result result = operation.apply(service).apply(current);
        if (result.accepted()) data.put(playerId, result.state());
        return result;
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

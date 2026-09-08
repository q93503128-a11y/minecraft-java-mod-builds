package kr.moonseungjun.turnboundre.battle;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Server-side active battle registry.
 * World actor bindings and player command ownership are intentionally separate: one player may control several
 * party participants while every participant still owns a distinct world presentation entity.
 */
public final class BattleManager {
    private final Map<UUID, BattleInstance> activeBattles = new HashMap<>();
    private final Map<UUID, UUID> entityToBattle = new HashMap<>();
    private final Map<UUID, Map<String, EntityParticipantBinding>> bindingsByBattle = new HashMap<>();
    private final Map<UUID, Map<String, UUID>> controllersByBattle = new HashMap<>();
    private final Map<UUID, BattleCommandService> commandServices = new HashMap<>();
    private final Map<UUID, BattleDefinitionContext> definitionContexts = new HashMap<>();
    private final Map<UUID, BattleRewardContext> rewardContexts = new HashMap<>();
    private final Set<UUID> claimedRewards = new HashSet<>();

    /** Adapter-only registration for tests/flows that do not submit player commands. */
    public void register(BattleInstance battle, List<EntityParticipantBinding> bindings) {
        registerInternal(battle, bindings, null, null, null, null, Map.of());
    }

    /** Universal/debug command registration. Player control defaults to the matching bound entity. */
    public void register(BattleInstance battle, List<EntityParticipantBinding> bindings, List<BattleParticipant> participants) {
        register(battle, bindings, participants, defaultControllers(participants, bindings));
    }

    /** Universal/debug command registration with explicit player controllers. */
    public void register(
            BattleInstance battle,
            List<EntityParticipantBinding> bindings,
            List<BattleParticipant> participants,
            Map<String, UUID> controllers
    ) {
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        registerInternal(battle, bindings, participants, new BattleCommandService(battle, participants), null, null, controllers);
    }

    /** Production/data-driven registration without a reward-bearing authored Encounter. */
    public void register(
            BattleInstance battle,
            List<EntityParticipantBinding> bindings,
            List<BattleParticipant> participants,
            BattleDefinitionContext definitionContext
    ) {
        register(battle, bindings, participants, definitionContext, null);
    }

    /** Production registration preserving the old one-entity-per-player ownership behavior for compatibility. */
    public void register(
            BattleInstance battle,
            List<EntityParticipantBinding> bindings,
            List<BattleParticipant> participants,
            BattleDefinitionContext definitionContext,
            BattleRewardContext rewardContext
    ) {
        register(battle, bindings, participants, definitionContext, rewardContext, defaultControllers(participants, bindings));
    }

    /**
     * Production/data-driven registration with explicit controller ownership. Definition/reward metadata and controller
     * ownership are snapshotted for the entire battle.
     */
    public void register(
            BattleInstance battle,
            List<EntityParticipantBinding> bindings,
            List<BattleParticipant> participants,
            BattleDefinitionContext definitionContext,
            BattleRewardContext rewardContext,
            Map<String, UUID> controllers
    ) {
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        if (definitionContext == null) throw new IllegalArgumentException("definitionContext must not be null");
        registerInternal(
                battle,
                bindings,
                participants,
                new BattleCommandService(battle, participants),
                definitionContext,
                rewardContext,
                controllers);
    }

    private void registerInternal(
            BattleInstance battle,
            List<EntityParticipantBinding> bindings,
            List<BattleParticipant> participants,
            BattleCommandService commandService,
            BattleDefinitionContext definitionContext,
            BattleRewardContext rewardContext,
            Map<String, UUID> controllers
    ) {
        if (battle == null) throw new IllegalArgumentException("battle must not be null");
        if (bindings == null || bindings.isEmpty()) throw new IllegalArgumentException("bindings must not be empty");
        if (controllers == null) throw new IllegalArgumentException("controllers must not be null");
        UUID battleId = battle.battleId();
        if (activeBattles.containsKey(battleId)) throw new IllegalStateException("battle already registered: " + battleId);

        if (definitionContext != null) {
            Set<String> participantIds = participants.stream().map(BattleParticipant::id).collect(Collectors.toUnmodifiableSet());
            if (!definitionContext.characterIdsByParticipant().keySet().equals(participantIds)) {
                throw new IllegalArgumentException("definition context must map every participant exactly once");
            }
        }
        if (rewardContext != null) {
            if (definitionContext == null) {
                throw new IllegalArgumentException("reward context requires a data-driven definition context");
            }
            var capturedTable = definitionContext.definitions().rewards().get(rewardContext.rewardTableId());
            if (!Objects.equals(capturedTable, rewardContext.rewardTable())) {
                throw new IllegalArgumentException("reward context must use the reward table from the captured definition snapshot: "
                        + rewardContext.rewardTableId());
            }
        }

        Map<String, EntityParticipantBinding> byParticipant = new HashMap<>();
        Set<UUID> localEntityIds = new HashSet<>();
        for (EntityParticipantBinding binding : bindings) {
            if (binding == null) throw new IllegalArgumentException("binding must not be null");
            battle.combatState(binding.participantId());
            if (byParticipant.putIfAbsent(binding.participantId(), binding) != null) {
                throw new IllegalArgumentException("duplicate participant binding: " + binding.participantId());
            }
            if (!localEntityIds.add(binding.entityId())) {
                throw new IllegalArgumentException("same entity bound twice in battle: " + binding.entityId());
            }
            UUID existingBattle = entityToBattle.get(binding.entityId());
            if (existingBattle != null) {
                throw new IllegalStateException("entity already participates in battle " + existingBattle + ": " + binding.entityId());
            }
        }

        Map<String, UUID> controllerSnapshot = Map.copyOf(controllers);
        if (commandService != null) {
            Set<String> expectedPlayerActors = participants.stream()
                    .filter(participant -> participant.team() == BattleTeam.PLAYER)
                    .map(BattleParticipant::id)
                    .collect(Collectors.toUnmodifiableSet());
            if (!controllerSnapshot.keySet().equals(expectedPlayerActors)) {
                throw new IllegalArgumentException("controllers must map every PLAYER participant exactly once");
            }
            for (Map.Entry<String, UUID> entry : controllerSnapshot.entrySet()) {
                if (entry.getValue() == null) throw new IllegalArgumentException("controller UUID must not be null: " + entry.getKey());
                battle.combatState(entry.getKey());
            }
        } else if (!controllerSnapshot.isEmpty()) {
            throw new IllegalArgumentException("controller ownership requires a command service");
        }

        activeBattles.put(battleId, battle);
        bindingsByBattle.put(battleId, Map.copyOf(byParticipant));
        controllersByBattle.put(battleId, controllerSnapshot);
        if (commandService != null) commandServices.put(battleId, commandService);
        if (definitionContext != null) definitionContexts.put(battleId, definitionContext);
        if (rewardContext != null) rewardContexts.put(battleId, rewardContext);
        for (EntityParticipantBinding binding : bindings) entityToBattle.put(binding.entityId(), battleId);
    }

    private static Map<String, UUID> defaultControllers(
            List<BattleParticipant> participants,
            List<EntityParticipantBinding> bindings
    ) {
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        if (bindings == null || bindings.isEmpty()) throw new IllegalArgumentException("bindings must not be empty");
        Map<String, UUID> bound = new HashMap<>();
        for (EntityParticipantBinding binding : bindings) {
            if (binding != null) bound.put(binding.participantId(), binding.entityId());
        }
        Map<String, UUID> controllers = new HashMap<>();
        for (BattleParticipant participant : participants) {
            if (participant.team() != BattleTeam.PLAYER) continue;
            UUID controller = bound.get(participant.id());
            if (controller == null) {
                throw new IllegalArgumentException("PLAYER participant has no binding for default controller: " + participant.id());
            }
            controllers.put(participant.id(), controller);
        }
        return Map.copyOf(controllers);
    }

    public Optional<BattleInstance> battle(UUID battleId) {
        return Optional.ofNullable(activeBattles.get(battleId));
    }

    public Optional<BattleInstance> battleForEntity(UUID entityId) {
        UUID battleId = entityToBattle.get(entityId);
        return battleId == null ? Optional.empty() : battle(battleId);
    }

    public Optional<BattleInstance> battleForController(UUID controllerId) {
        if (controllerId == null) return Optional.empty();
        return controllersByBattle.entrySet().stream()
                .filter(entry -> entry.getValue().containsValue(controllerId))
                .map(Map.Entry::getKey)
                .sorted()
                .map(activeBattles::get)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public Optional<EntityParticipantBinding> binding(UUID battleId, String participantId) {
        Map<String, EntityParticipantBinding> bindings = bindingsByBattle.get(battleId);
        return bindings == null ? Optional.empty() : Optional.ofNullable(bindings.get(participantId));
    }

    /** Stable read-only snapshot used by server presentation ownership resolution. */
    public List<EntityParticipantBinding> bindings(UUID battleId) {
        Map<String, EntityParticipantBinding> bindings = bindingsByBattle.get(battleId);
        if (bindings == null) return List.of();
        return bindings.values().stream()
                .sorted(Comparator.comparing(EntityParticipantBinding::participantId))
                .toList();
    }

    public Optional<UUID> controller(UUID battleId, String participantId) {
        Map<String, UUID> controllers = controllersByBattle.get(battleId);
        return controllers == null ? Optional.empty() : Optional.ofNullable(controllers.get(participantId));
    }

    public List<UUID> controllers(UUID battleId) {
        Map<String, UUID> controllers = controllersByBattle.get(battleId);
        if (controllers == null) return List.of();
        return controllers.values().stream().distinct().sorted().toList();
    }

    public List<UUID> activeBattleIds() {
        return activeBattles.keySet().stream().sorted().toList();
    }

    public Optional<BattleCommandService> commandService(UUID battleId) {
        return Optional.ofNullable(commandServices.get(battleId));
    }

    public Optional<BattleDefinitionContext> definitionContext(UUID battleId) {
        return Optional.ofNullable(definitionContexts.get(battleId));
    }

    public Optional<BattleRewardContext> rewardContext(UUID battleId) {
        return Optional.ofNullable(rewardContexts.get(battleId));
    }

    public Optional<String> characterId(UUID battleId, String participantId) {
        BattleDefinitionContext context = definitionContexts.get(battleId);
        return context == null ? Optional.empty() : Optional.ofNullable(context.characterId(participantId));
    }

    public boolean rewardClaimed(UUID battleId) {
        return claimedRewards.contains(battleId);
    }

    /** Snapshot of every terminal battle waiting for player-facing result acknowledgement. */
    public List<UUID> terminalRewardStateBattleIds() {
        return activeBattles.entrySet().stream()
                .filter(entry -> entry.getValue().state() == BattleState.REWARD)
                .filter(entry -> entry.getValue().outcome() != BattleInstance.Outcome.ONGOING)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    /** Snapshot of unclaimed authored victories currently waiting for persistence. */
    public List<UUID> rewardReadyBattleIds() {
        return activeBattles.entrySet().stream()
                .filter(entry -> entry.getValue().outcome() == BattleInstance.Outcome.VICTORY)
                .filter(entry -> entry.getValue().state() == BattleState.REWARD)
                .map(Map.Entry::getKey)
                .filter(rewardContexts::containsKey)
                .filter(id -> !claimedRewards.contains(id))
                .toList();
    }

    /** Executes a reward claim at most once for a data-driven VICTORY in REWARD. */
    public <T> Optional<T> claimVictoryReward(UUID battleId, Function<BattleRewardContext, T> claim) {
        if (battleId == null || claim == null) throw new IllegalArgumentException("battleId/claim required");
        BattleInstance battle = activeBattles.get(battleId);
        BattleRewardContext context = rewardContexts.get(battleId);
        if (battle == null || context == null || claimedRewards.contains(battleId)) return Optional.empty();
        if (battle.outcome() != BattleInstance.Outcome.VICTORY || battle.state() != BattleState.REWARD) {
            return Optional.empty();
        }
        T result = Objects.requireNonNull(claim.apply(context), "reward claim callback must not return null");
        claimedRewards.add(battleId);
        return Optional.of(result);
    }

    public int activeBattleCount() { return activeBattles.size(); }
    public int boundEntityCount() { return entityToBattle.size(); }

    /** Idempotent cleanup used by normal battle completion and exceptional lifecycle guards. */
    public Optional<BattleInstance> cleanup(UUID battleId) {
        BattleInstance battle = activeBattles.remove(battleId);
        commandServices.remove(battleId);
        definitionContexts.remove(battleId);
        rewardContexts.remove(battleId);
        controllersByBattle.remove(battleId);
        claimedRewards.remove(battleId);
        Map<String, EntityParticipantBinding> bindings = bindingsByBattle.remove(battleId);
        if (bindings != null) {
            for (EntityParticipantBinding binding : bindings.values()) entityToBattle.remove(binding.entityId(), battleId);
        }
        return Optional.ofNullable(battle);
    }
}

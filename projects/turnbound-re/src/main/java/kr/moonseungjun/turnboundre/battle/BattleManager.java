package kr.moonseungjun.turnboundre.battle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side active battle registry and entity-to-battle ownership gate.
 * A Minecraft entity may participate in at most one live battle.
 */
public final class BattleManager {
    private final Map<UUID, BattleInstance> activeBattles = new HashMap<>();
    private final Map<UUID, UUID> entityToBattle = new HashMap<>();
    private final Map<UUID, Map<String, EntityParticipantBinding>> bindingsByBattle = new HashMap<>();

    public void register(BattleInstance battle, List<EntityParticipantBinding> bindings) {
        if (battle == null) throw new IllegalArgumentException("battle must not be null");
        if (bindings == null || bindings.isEmpty()) throw new IllegalArgumentException("bindings must not be empty");
        UUID battleId = battle.battleId();
        if (activeBattles.containsKey(battleId)) {
            throw new IllegalStateException("battle already registered: " + battleId);
        }

        Map<String, EntityParticipantBinding> byParticipant = new HashMap<>();
        Set<UUID> localEntityIds = new HashSet<>();
        for (EntityParticipantBinding binding : bindings) {
            if (binding == null) throw new IllegalArgumentException("binding must not be null");
            // Proves that the binding points at a participant actually owned by the core battle.
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

        activeBattles.put(battleId, battle);
        bindingsByBattle.put(battleId, Map.copyOf(byParticipant));
        for (EntityParticipantBinding binding : bindings) {
            entityToBattle.put(binding.entityId(), battleId);
        }
    }

    public Optional<BattleInstance> battle(UUID battleId) {
        return Optional.ofNullable(activeBattles.get(battleId));
    }

    public Optional<BattleInstance> battleForEntity(UUID entityId) {
        UUID battleId = entityToBattle.get(entityId);
        return battleId == null ? Optional.empty() : battle(battleId);
    }

    public Optional<EntityParticipantBinding> binding(UUID battleId, String participantId) {
        Map<String, EntityParticipantBinding> bindings = bindingsByBattle.get(battleId);
        return bindings == null ? Optional.empty() : Optional.ofNullable(bindings.get(participantId));
    }

    public int activeBattleCount() {
        return activeBattles.size();
    }

    public int boundEntityCount() {
        return entityToBattle.size();
    }

    /**
     * Idempotent cleanup used by normal battle completion and exceptional lifecycle guards.
     */
    public Optional<BattleInstance> cleanup(UUID battleId) {
        BattleInstance battle = activeBattles.remove(battleId);
        Map<String, EntityParticipantBinding> bindings = bindingsByBattle.remove(battleId);
        if (bindings != null) {
            for (EntityParticipantBinding binding : bindings.values()) {
                entityToBattle.remove(binding.entityId(), battleId);
            }
        }
        return Optional.ofNullable(battle);
    }
}

package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.EquipmentDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pure server-side equipment progression. Minecraft inventory removal is an outer adapter concern. */
public final class EquipmentProgressionService {
    public enum ResultCode {
        ACCEPTED,
        UNKNOWN_EQUIPMENT,
        ALREADY_OWNED,
        EQUIPMENT_NOT_OWNED,
        CHARACTER_NOT_OWNED,
        EQUIPMENT_IN_USE,
        INSUFFICIENT_COIN,
        INSUFFICIENT_MATERIAL,
        MAX_LEVEL,
        NOT_EQUIPPED
    }

    public record MaterialCost(String itemId, int count) {
        public static final MaterialCost NONE = new MaterialCost("", 0);

        public MaterialCost {
            itemId = itemId == null ? "" : itemId;
            if (count < 0) throw new IllegalArgumentException("material count must be >= 0");
            if (count > 0 && itemId.isBlank()) throw new IllegalArgumentException("material item required when count > 0");
        }
    }

    public record Result(ResultCode code, PlayerProgress state, long coinCost, MaterialCost materialCost, String detail) {
        public Result {
            if (code == null || state == null || materialCost == null) throw new IllegalArgumentException("result fields required");
            if (coinCost < 0) throw new IllegalArgumentException("coinCost must be >= 0");
            detail = detail == null ? "" : detail;
        }

        public boolean accepted() { return code == ResultCode.ACCEPTED; }
    }

    private final DefinitionRegistry definitions;

    public EquipmentProgressionService(DefinitionRegistry definitions) {
        if (definitions == null) throw new IllegalArgumentException("definitions required");
        this.definitions = definitions;
    }

    /** Crafts one unique equipment piece at tier 1. */
    public Result craft(PlayerProgress state, String equipmentId, int availableMaterialCount) {
        requireState(state);
        EquipmentDefinition definition = definitions.equipment().get(equipmentId);
        if (definition == null) return reject(ResultCode.UNKNOWN_EQUIPMENT, state, equipmentId);
        if (state.equipment().containsKey(equipmentId)) return reject(ResultCode.ALREADY_OWNED, state, equipmentId);
        EquipmentDefinition.Tier tier = definition.tier(1);
        if (tier == null) throw new IllegalStateException("validated equipment lost tier 1: " + equipmentId);
        Result blocked = affordability(state, definition, tier, availableMaterialCount);
        if (blocked != null) return blocked;

        Map<String, EquipmentProgress> equipment = new LinkedHashMap<>(state.equipment());
        equipment.put(equipmentId, new EquipmentProgress(equipmentId, 1));
        PlayerProgress next = copy(state, state.coin() - tier.coinCost(), equipment, state.equippedEquipment());
        return accepted(next, definition, tier, "crafted=" + equipmentId);
    }

    /** Upgrades an owned piece exactly one authored tier. */
    public Result upgrade(PlayerProgress state, String equipmentId, int availableMaterialCount) {
        requireState(state);
        EquipmentDefinition definition = definitions.equipment().get(equipmentId);
        if (definition == null) return reject(ResultCode.UNKNOWN_EQUIPMENT, state, equipmentId);
        EquipmentProgress current = state.equipment().get(equipmentId);
        if (current == null) return reject(ResultCode.EQUIPMENT_NOT_OWNED, state, equipmentId);
        if (!EquipmentRules.valid(definitions, current)) {
            throw new IllegalArgumentException("persisted equipment does not match current definitions: " + current);
        }
        EquipmentDefinition.Tier nextTier = definition.tier(current.level() + 1);
        if (nextTier == null) return reject(ResultCode.MAX_LEVEL, state, equipmentId + "@" + current.level());
        Result blocked = affordability(state, definition, nextTier, availableMaterialCount);
        if (blocked != null) return blocked;

        Map<String, EquipmentProgress> equipment = new LinkedHashMap<>(state.equipment());
        equipment.put(equipmentId, new EquipmentProgress(equipmentId, nextTier.level()));
        PlayerProgress next = copy(state, state.coin() - nextTier.coinCost(), equipment, state.equippedEquipment());
        return accepted(next, definition, nextTier, "level=" + nextTier.level());
    }

    /** Equipping is free. A unique crafted piece cannot be assigned to two characters at once. */
    public Result equip(PlayerProgress state, String characterId, String equipmentId) {
        requireState(state);
        if (!state.characters().containsKey(characterId)) {
            return reject(ResultCode.CHARACTER_NOT_OWNED, state, characterId);
        }
        EquipmentProgress equipment = state.equipment().get(equipmentId);
        if (equipment == null) return reject(ResultCode.EQUIPMENT_NOT_OWNED, state, equipmentId);
        if (!EquipmentRules.valid(definitions, equipment)) {
            throw new IllegalArgumentException("persisted equipment does not match current definitions: " + equipment);
        }
        for (Map.Entry<String, String> entry : state.equippedEquipment().entrySet()) {
            if (!entry.getKey().equals(characterId) && entry.getValue().equals(equipmentId)) {
                return reject(ResultCode.EQUIPMENT_IN_USE, state, entry.getKey());
            }
        }
        Map<String, String> equipped = new LinkedHashMap<>(state.equippedEquipment());
        equipped.put(characterId, equipmentId);
        return new Result(ResultCode.ACCEPTED, copy(state, state.coin(), state.equipment(), equipped),
                0L, MaterialCost.NONE, "equipped=" + equipmentId);
    }

    public Result unequip(PlayerProgress state, String characterId) {
        requireState(state);
        if (!state.characters().containsKey(characterId)) {
            return reject(ResultCode.CHARACTER_NOT_OWNED, state, characterId);
        }
        if (!state.equippedEquipment().containsKey(characterId)) {
            return reject(ResultCode.NOT_EQUIPPED, state, characterId);
        }
        Map<String, String> equipped = new LinkedHashMap<>(state.equippedEquipment());
        equipped.remove(characterId);
        return new Result(ResultCode.ACCEPTED, copy(state, state.coin(), state.equipment(), equipped),
                0L, MaterialCost.NONE, "unequipped=" + characterId);
    }

    private Result affordability(
            PlayerProgress state,
            EquipmentDefinition definition,
            EquipmentDefinition.Tier tier,
            int availableMaterialCount
    ) {
        MaterialCost material = new MaterialCost(definition.ingredientItem(), tier.materialCount());
        if (availableMaterialCount < 0) throw new IllegalArgumentException("availableMaterialCount must be >= 0");
        if (state.coin() < tier.coinCost()) {
            return new Result(ResultCode.INSUFFICIENT_COIN, state, tier.coinCost(), material,
                    state.coin() + "/" + tier.coinCost());
        }
        if (availableMaterialCount < tier.materialCount()) {
            return new Result(ResultCode.INSUFFICIENT_MATERIAL, state, tier.coinCost(), material,
                    availableMaterialCount + "/" + tier.materialCount());
        }
        return null;
    }

    private static Result accepted(
            PlayerProgress next,
            EquipmentDefinition definition,
            EquipmentDefinition.Tier tier,
            String detail
    ) {
        return new Result(ResultCode.ACCEPTED, next, tier.coinCost(),
                new MaterialCost(definition.ingredientItem(), tier.materialCount()), detail);
    }

    private static Result reject(ResultCode code, PlayerProgress state, String detail) {
        return new Result(code, state, 0L, MaterialCost.NONE, detail);
    }

    private static PlayerProgress copy(
            PlayerProgress state,
            long coin,
            Map<String, EquipmentProgress> equipment,
            Map<String, String> equipped
    ) {
        return new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA,
                coin,
                state.essence(),
                state.shards(),
                state.characters(),
                state.party(),
                state.partyCapacity(),
                state.completedEncounterLocators(),
                equipment,
                equipped);
    }

    private static void requireState(PlayerProgress state) {
        if (state == null) throw new IllegalArgumentException("state required");
    }
}

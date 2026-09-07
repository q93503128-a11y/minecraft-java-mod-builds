package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure transactional progression operations. Rejections always return the exact unchanged input state instance. */
public final class ProgressionService {
    public enum ResultCode {
        ACCEPTED,
        UNKNOWN_CHARACTER,
        ALREADY_OWNED,
        NOT_OWNED,
        INSUFFICIENT_SHARDS,
        INSUFFICIENT_COIN,
        INSUFFICIENT_ESSENCE,
        LEVEL_CAP,
        NOT_AT_LEVEL_CAP,
        MAX_STAR,
        INVALID_PARTY,
        PARTY_COST_EXCEEDED
    }

    public record Result(ResultCode code, PlayerProgress state, String detail) {
        public boolean accepted() { return code == ResultCode.ACCEPTED; }
    }

    private final DefinitionRegistry definitions;
    private final ProgressionDefinition tuning;

    public ProgressionService(DefinitionRegistry definitions, ProgressionDefinition tuning) {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        if (tuning == null) throw new IllegalArgumentException("tuning must not be null");
        this.definitions = definitions;
        this.tuning = tuning;
    }

    public PlayerProgress grantCurrency(PlayerProgress state, long coin, long essence) {
        requireState(state);
        if (coin < 0 || essence < 0) throw new IllegalArgumentException("grants must be >= 0");
        return copy(state, Math.addExact(state.coin(), coin), Math.addExact(state.essence(), essence),
                state.shards(), state.characters(), state.party());
    }

    public PlayerProgress grantShards(PlayerProgress state, String characterId, int amount) {
        requireState(state);
        character(characterId); // Unknown shard IDs are rejected instead of silently creating dead balances.
        if (amount < 0) throw new IllegalArgumentException("shard grant must be >= 0");
        Map<String, Integer> shards = new LinkedHashMap<>(state.shards());
        shards.put(characterId, Math.addExact(shards.getOrDefault(characterId, 0), amount));
        return copy(state, state.coin(), state.essence(), shards, state.characters(), state.party());
    }

    public Result unlock(PlayerProgress state, String characterId) {
        requireState(state);
        CharacterDefinition definition = definitions.characters().get(characterId);
        if (definition == null) return reject(ResultCode.UNKNOWN_CHARACTER, state, characterId);
        if (state.characters().containsKey(characterId)) return reject(ResultCode.ALREADY_OWNED, state, characterId);
        int cost = ProgressionRules.unlockShardCost(tuning, definition.originStar());
        int balance = state.shards().getOrDefault(characterId, 0);
        if (balance < cost) return reject(ResultCode.INSUFFICIENT_SHARDS, state, balance + "/" + cost);

        Map<String, Integer> shards = new LinkedHashMap<>(state.shards());
        shards.put(characterId, balance - cost);
        Map<String, CharacterProgress> characters = new LinkedHashMap<>(state.characters());
        characters.put(characterId, new CharacterProgress(characterId, definition.originStar(), definition.originStar(), 1));
        return accept(copy(state, state.coin(), state.essence(), shards, characters, state.party()), "unlocked=" + characterId);
    }

    public Result levelUp(PlayerProgress state, String characterId) {
        requireState(state);
        CharacterProgress current = state.characters().get(characterId);
        if (current == null) return reject(ResultCode.NOT_OWNED, state, characterId);
        CharacterDefinition definition = character(characterId);
        ProgressionRules.requireMatches(definition, current);
        if (current.level() >= ProgressionRules.levelCap(current.currentStar())) {
            return reject(ResultCode.LEVEL_CAP, state, "level=" + current.level());
        }
        ProgressionRules.Cost cost = ProgressionRules.levelUpCost(tuning, current.currentStar(), current.level());
        ResultCode affordability = affordability(state, cost);
        if (affordability != null) return reject(affordability, state, cost.toString());

        Map<String, CharacterProgress> characters = new LinkedHashMap<>(state.characters());
        characters.put(characterId, new CharacterProgress(
                current.characterId(), current.originStar(), current.currentStar(), current.level() + 1));
        PlayerProgress next = copy(state, state.coin() - cost.coin(), state.essence() - cost.essence(),
                state.shards(), characters, state.party());
        return accept(next, "level=" + (current.level() + 1));
    }

    public Result ascend(PlayerProgress state, String characterId) {
        requireState(state);
        CharacterProgress current = state.characters().get(characterId);
        if (current == null) return reject(ResultCode.NOT_OWNED, state, characterId);
        CharacterDefinition definition = character(characterId);
        ProgressionRules.requireMatches(definition, current);
        if (current.currentStar() >= 6) return reject(ResultCode.MAX_STAR, state, characterId);
        if (current.level() != ProgressionRules.levelCap(current.currentStar())) {
            return reject(ResultCode.NOT_AT_LEVEL_CAP, state,
                    "level=" + current.level() + " cap=" + ProgressionRules.levelCap(current.currentStar()));
        }

        int targetStar = current.currentStar() + 1;
        ProgressionRules.Cost cost = ProgressionRules.ascensionCost(tuning, targetStar);
        ResultCode affordability = affordability(state, cost);
        if (affordability != null) return reject(affordability, state, cost.toString());
        int shardBalance = state.shards().getOrDefault(characterId, 0);
        if (shardBalance < cost.shards()) {
            return reject(ResultCode.INSUFFICIENT_SHARDS, state, shardBalance + "/" + cost.shards());
        }

        Map<String, Integer> shards = new LinkedHashMap<>(state.shards());
        shards.put(characterId, shardBalance - cost.shards());
        Map<String, CharacterProgress> characters = new LinkedHashMap<>(state.characters());
        characters.put(characterId, new CharacterProgress(
                current.characterId(), current.originStar(), targetStar, current.level()));
        PlayerProgress next = copy(state, state.coin() - cost.coin(), state.essence() - cost.essence(),
                shards, characters, state.party());
        return accept(next, "star=" + targetStar);
    }

    public Result setParty(PlayerProgress state, List<String> requestedParty) {
        requireState(state);
        if (requestedParty == null || requestedParty.size() > 4 || requestedParty.stream().anyMatch(id -> id == null || id.isBlank())) {
            return reject(ResultCode.INVALID_PARTY, state, "party must contain 0..4 valid ids");
        }
        if (requestedParty.stream().distinct().count() != requestedParty.size()) {
            return reject(ResultCode.INVALID_PARTY, state, "party contains duplicate character");
        }
        int cost = 0;
        for (String characterId : requestedParty) {
            if (!state.characters().containsKey(characterId)) return reject(ResultCode.NOT_OWNED, state, characterId);
            cost = Math.addExact(cost, character(characterId).squadCost());
        }
        if (cost > state.partyCapacity()) {
            return reject(ResultCode.PARTY_COST_EXCEEDED, state, cost + "/" + state.partyCapacity());
        }
        return accept(copy(state, state.coin(), state.essence(), state.shards(), state.characters(), List.copyOf(requestedParty)),
                "partyCost=" + cost);
    }

    private ResultCode affordability(PlayerProgress state, ProgressionRules.Cost cost) {
        if (state.coin() < cost.coin()) return ResultCode.INSUFFICIENT_COIN;
        if (state.essence() < cost.essence()) return ResultCode.INSUFFICIENT_ESSENCE;
        return null;
    }

    private CharacterDefinition character(String characterId) {
        CharacterDefinition definition = definitions.characters().get(characterId);
        if (definition == null) throw new IllegalArgumentException("unknown CharacterDefinition " + characterId);
        return definition;
    }

    private static PlayerProgress copy(
            PlayerProgress state,
            long coin,
            long essence,
            Map<String, Integer> shards,
            Map<String, CharacterProgress> characters,
            List<String> party
    ) {
        return new PlayerProgress(state.schemaVersion(), coin, essence, shards, characters, party, state.partyCapacity());
    }

    private static Result accept(PlayerProgress state, String detail) {
        return new Result(ResultCode.ACCEPTED, state, detail);
    }

    private static Result reject(ResultCode code, PlayerProgress original, String detail) {
        return new Result(code, original, detail);
    }

    private static void requireState(PlayerProgress state) {
        if (state == null) throw new IllegalArgumentException("state must not be null");
    }
}

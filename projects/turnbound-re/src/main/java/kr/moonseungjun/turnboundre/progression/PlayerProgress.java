package kr.moonseungjun.turnboundre.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Immutable server-owned progression root. Invalid operations create no partially-mutated state. */
public record PlayerProgress(
        int schemaVersion,
        long coin,
        long essence,
        Map<String, Integer> shards,
        Map<String, CharacterProgress> characters,
        List<String> party,
        int partyCapacity
) {
    public static final int CURRENT_SCHEMA = 1;

    public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schemaVersion").forGetter(PlayerProgress::schemaVersion),
            Codec.LONG.fieldOf("coin").forGetter(PlayerProgress::coin),
            Codec.LONG.fieldOf("essence").forGetter(PlayerProgress::essence),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("shards", Map.of()).forGetter(PlayerProgress::shards),
            Codec.unboundedMap(Codec.STRING, CharacterProgress.CODEC).optionalFieldOf("characters", Map.of()).forGetter(PlayerProgress::characters),
            Codec.STRING.listOf().optionalFieldOf("party", List.of()).forGetter(PlayerProgress::party),
            Codec.INT.fieldOf("partyCapacity").forGetter(PlayerProgress::partyCapacity)
    ).apply(instance, PlayerProgress::new));

    public PlayerProgress {
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA) {
            throw new IllegalArgumentException("unsupported schemaVersion " + schemaVersion);
        }
        if (coin < 0 || essence < 0) throw new IllegalArgumentException("currencies must be >= 0");
        if (partyCapacity < 1) throw new IllegalArgumentException("partyCapacity must be >= 1");
        shards = shards == null ? Map.of() : Map.copyOf(shards);
        characters = characters == null ? Map.of() : Map.copyOf(characters);
        party = party == null ? List.of() : List.copyOf(party);
        for (Map.Entry<String, Integer> entry : shards.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("invalid shard balance");
            }
        }
        for (Map.Entry<String, CharacterProgress> entry : characters.entrySet()) {
            if (entry.getValue() == null || !entry.getKey().equals(entry.getValue().characterId())) {
                throw new IllegalArgumentException("character map key must equal characterId");
            }
        }
        if (party.size() > 4 || new HashSet<>(party).size() != party.size()) {
            throw new IllegalArgumentException("party must contain at most four unique characters");
        }
        for (String characterId : party) {
            if (!characters.containsKey(characterId)) throw new IllegalArgumentException("party contains unowned character " + characterId);
        }
    }

    public static PlayerProgress fresh(ProgressionDefinition tuning) {
        if (tuning == null) throw new IllegalArgumentException("tuning must not be null");
        return new PlayerProgress(CURRENT_SCHEMA, 0L, 0L, Map.of(), Map.of(), List.of(), tuning.partyCapacity());
    }
}

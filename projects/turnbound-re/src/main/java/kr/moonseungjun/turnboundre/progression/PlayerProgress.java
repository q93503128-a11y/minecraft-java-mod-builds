package kr.moonseungjun.turnboundre.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable server-owned progression root. Invalid operations create no partially-mutated state. */
public record PlayerProgress(
        int schemaVersion,
        long coin,
        long essence,
        Map<String, Integer> shards,
        Map<String, CharacterProgress> characters,
        List<String> party,
        int partyCapacity,
        Set<String> completedEncounterLocators
) {
    public static final int CURRENT_SCHEMA = 2;

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            values -> Set.copyOf(values),
            values -> values.stream().sorted().toList());

    public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schemaVersion").forGetter(PlayerProgress::schemaVersion),
            Codec.LONG.fieldOf("coin").forGetter(PlayerProgress::coin),
            Codec.LONG.fieldOf("essence").forGetter(PlayerProgress::essence),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("shards", Map.of()).forGetter(PlayerProgress::shards),
            Codec.unboundedMap(Codec.STRING, CharacterProgress.CODEC).optionalFieldOf("characters", Map.of()).forGetter(PlayerProgress::characters),
            Codec.STRING.listOf().optionalFieldOf("party", List.of()).forGetter(PlayerProgress::party),
            Codec.INT.fieldOf("partyCapacity").forGetter(PlayerProgress::partyCapacity),
            STRING_SET_CODEC.optionalFieldOf("completedEncounterLocators", Set.of()).forGetter(PlayerProgress::completedEncounterLocators)
    ).apply(instance, PlayerProgress::new));

    /** Source-compatible constructor for schema-1 callers; decoded schema-1 saves also default the new field to empty. */
    public PlayerProgress(
            int schemaVersion,
            long coin,
            long essence,
            Map<String, Integer> shards,
            Map<String, CharacterProgress> characters,
            List<String> party,
            int partyCapacity
    ) {
        this(schemaVersion, coin, essence, shards, characters, party, partyCapacity, Set.of());
    }

    public PlayerProgress {
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA) {
            throw new IllegalArgumentException("unsupported schemaVersion " + schemaVersion);
        }
        if (coin < 0 || essence < 0) throw new IllegalArgumentException("currencies must be >= 0");
        if (partyCapacity < 1) throw new IllegalArgumentException("partyCapacity must be >= 1");
        shards = shards == null ? Map.of() : Map.copyOf(shards);
        characters = characters == null ? Map.of() : Map.copyOf(characters);
        party = party == null ? List.of() : List.copyOf(party);
        completedEncounterLocators = completedEncounterLocators == null ? Set.of() : Set.copyOf(completedEncounterLocators);
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
        for (String locator : completedEncounterLocators) {
            if (locator == null || locator.isBlank()) throw new IllegalArgumentException("completed encounter locator must not be blank");
        }
    }

    public boolean hasCompletedEncounterLocator(String locator) {
        return locator != null && !locator.isBlank() && completedEncounterLocators.contains(locator);
    }

    /** Returns one upgraded immutable save root; repeated completion of the same locator is idempotent. */
    public PlayerProgress completeEncounterLocator(String locator) {
        if (locator == null || locator.isBlank()) throw new IllegalArgumentException("locator must not be blank");
        if (schemaVersion == CURRENT_SCHEMA && completedEncounterLocators.contains(locator)) return this;
        Set<String> completed = new LinkedHashSet<>(completedEncounterLocators);
        completed.add(locator);
        return new PlayerProgress(
                CURRENT_SCHEMA, coin, essence, shards, characters, party, partyCapacity, completed);
    }

    public static PlayerProgress fresh(ProgressionDefinition tuning) {
        if (tuning == null) throw new IllegalArgumentException("tuning must not be null");
        return new PlayerProgress(
                CURRENT_SCHEMA, 0L, 0L, Map.of(), Map.of(), List.of(), tuning.partyCapacity(), Set.of());
    }
}

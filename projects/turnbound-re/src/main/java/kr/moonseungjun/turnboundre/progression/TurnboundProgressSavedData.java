package kr.moonseungjun.turnboundre.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-global persisted player progression. One file stores all TURNBOUND player roots for the world. */
public final class TurnboundProgressSavedData extends SavedData {
    public static final Codec<TurnboundProgressSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PlayerProgress.CODEC)
                    .optionalFieldOf("players", Map.of())
                    .forGetter(TurnboundProgressSavedData::snapshot)
    ).apply(instance, TurnboundProgressSavedData::new));

    public static final SavedDataType<TurnboundProgressSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "progression/players"),
            TurnboundProgressSavedData::new,
            CODEC
    );

    private final Map<String, PlayerProgress> players;

    public TurnboundProgressSavedData() {
        this(Map.of());
    }

    TurnboundProgressSavedData(Map<String, PlayerProgress> players) {
        this.players = new LinkedHashMap<>();
        if (players != null) {
            for (Map.Entry<String, PlayerProgress> entry : players.entrySet()) {
                UUID.fromString(entry.getKey());
                if (entry.getValue() == null) throw new IllegalArgumentException("null PlayerProgress for " + entry.getKey());
                this.players.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Optional<PlayerProgress> get(UUID playerId) {
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        return Optional.ofNullable(players.get(playerId.toString()));
    }

    public PlayerProgress getOrCreate(UUID playerId, ProgressionDefinition tuning) {
        if (playerId == null || tuning == null) throw new IllegalArgumentException("playerId/tuning required");
        String key = playerId.toString();
        PlayerProgress existing = players.get(key);
        if (existing != null) return existing;
        PlayerProgress created = PlayerProgress.fresh(tuning);
        players.put(key, created);
        setDirty();
        return created;
    }

    /** Replaces one immutable root and marks disk state dirty only when the value actually changed. */
    public void put(UUID playerId, PlayerProgress state) {
        if (playerId == null || state == null) throw new IllegalArgumentException("playerId/state required");
        String key = playerId.toString();
        PlayerProgress previous = players.put(key, state);
        if (!state.equals(previous)) setDirty();
    }

    public Map<String, PlayerProgress> snapshot() {
        return Map.copyOf(players);
    }
}

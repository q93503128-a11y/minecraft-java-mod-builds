package dev.moonseungjun.augmentationsurvival.augment.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Save-wide domination occupancy keyed by owner UUID so offline deaths release slots correctly. */
public record DominationWorldState(Map<String, Integer> occupiedByOwner) {
    public static final Codec<DominationWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("occupied_by_owner", Map.of())
                    .forGetter(DominationWorldState::occupiedByOwner)
    ).apply(instance, DominationWorldState::new));

    public DominationWorldState {
        Map<String, Integer> sanitized = new HashMap<>();
        for (Map.Entry<String, Integer> entry : occupiedByOwner.entrySet()) {
            int count = Math.max(0, entry.getValue());
            if (count > 0) {
                sanitized.put(entry.getKey(), count);
            }
        }
        occupiedByOwner = Map.copyOf(sanitized);
    }

    public static DominationWorldState initial() {
        return new DominationWorldState(Map.of());
    }

    public int occupied(UUID owner) {
        return Math.max(0, occupiedByOwner.getOrDefault(owner.toString(), 0));
    }

    public DominationWorldState withDelta(UUID owner, int delta) {
        Map<String, Integer> next = new HashMap<>(occupiedByOwner);
        String key = owner.toString();
        int value = Math.max(0, next.getOrDefault(key, 0) + delta);
        if (value == 0) {
            next.remove(key);
        } else {
            next.put(key, value);
        }
        return new DominationWorldState(next);
    }
}

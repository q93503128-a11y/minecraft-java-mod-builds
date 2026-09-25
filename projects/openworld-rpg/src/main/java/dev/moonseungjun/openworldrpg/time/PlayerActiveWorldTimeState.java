package dev.moonseungjun.openworldrpg.time;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Persistent personal active-world time.
 *
 * <p>It advances only while the player is legitimately loaded on the server. It is independent of
 * Minecraft day time, sleep and relog, so merchant/event epochs cannot be rerolled by changing the
 * world clock.</p>
 */
public record PlayerActiveWorldTimeState(
        int schemaVersion,
        long activeTicks,
        Map<String, Long> epochs
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<PlayerActiveWorldTimeState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("active_world_time_schema_version")
                            .forGetter(PlayerActiveWorldTimeState::schemaVersion),
                    Codec.LONG.fieldOf("active_ticks")
                            .forGetter(PlayerActiveWorldTimeState::activeTicks),
                    Codec.unboundedMap(Codec.STRING, Codec.LONG)
                            .fieldOf("epochs")
                            .forGetter(PlayerActiveWorldTimeState::epochs)
            ).apply(instance, PlayerActiveWorldTimeState::new));

    public PlayerActiveWorldTimeState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported active-world-time schema version: " + schemaVersion
            );
        }
        if (activeTicks < 0L) {
            throw new IllegalArgumentException("activeTicks must be non-negative.");
        }
        epochs = Map.copyOf(Objects.requireNonNull(epochs, "epochs"));
        for (Map.Entry<String, Long> entry : epochs.entrySet()) {
            requireStableId(entry.getKey());
            if (entry.getValue() == null
                    || entry.getValue() < 0L
                    || entry.getValue() > activeTicks) {
                throw new IllegalArgumentException(
                        "Active-time epoch must be inside [0, activeTicks]: " + entry.getKey()
                );
            }
        }
    }

    public static PlayerActiveWorldTimeState initial() {
        return new PlayerActiveWorldTimeState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Map.of()
        );
    }

    public PlayerActiveWorldTimeState advance(long ticks) {
        if (ticks < 0L) {
            throw new IllegalArgumentException("ticks must be non-negative.");
        }
        if (ticks == 0L) {
            return this;
        }
        return new PlayerActiveWorldTimeState(
                schemaVersion,
                Math.addExact(activeTicks, ticks),
                epochs
        );
    }

    public PlayerActiveWorldTimeState markEpochOnce(String epochId) {
        requireStableId(epochId);
        if (epochs.containsKey(epochId)) {
            return this;
        }
        Map<String, Long> next = new HashMap<>(epochs);
        next.put(epochId, activeTicks);
        return new PlayerActiveWorldTimeState(
                schemaVersion,
                activeTicks,
                Map.copyOf(next)
        );
    }

    public OptionalLong epoch(String epochId) {
        requireStableId(epochId);
        Long value = epochs.get(epochId);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    public long elapsedSinceEpoch(String epochId) {
        long epoch = epoch(epochId).orElseThrow(
                () -> new IllegalStateException("Missing active-time epoch: " + epochId)
        );
        return activeTicks - epoch;
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable namespaced epoch id.");
        }
    }
}

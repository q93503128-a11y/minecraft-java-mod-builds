package dev.moonseungjun.augmentationsurvival.augment.active;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Persistent expiry and owner records for system-created temporary platform blocks. */
public record TemporaryPlatformWorldState(Map<String, PlatformRecord> platforms) {
    public static final Codec<TemporaryPlatformWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PlatformRecord.CODEC)
                    .optionalFieldOf("platforms", Map.of())
                    .forGetter(TemporaryPlatformWorldState::platforms)
    ).apply(instance, TemporaryPlatformWorldState::new));

    public TemporaryPlatformWorldState {
        platforms = Map.copyOf(platforms);
    }

    public static TemporaryPlatformWorldState initial() {
        return new TemporaryPlatformWorldState(Map.of());
    }

    public static String key(ResourceKey<Level> dimension, long packedPosition) {
        return dimension.identifier() + "|" + packedPosition;
    }

    public TemporaryPlatformWorldState withPlatform(String key, PlatformRecord record) {
        Map<String, PlatformRecord> next = new HashMap<>(platforms);
        next.put(key, record);
        return new TemporaryPlatformWorldState(next);
    }

    public TemporaryPlatformWorldState withoutPlatform(String key) {
        if (!platforms.containsKey(key)) return this;
        Map<String, PlatformRecord> next = new HashMap<>(platforms);
        next.remove(key);
        return new TemporaryPlatformWorldState(next);
    }

    public record PlatformRecord(String dimension, long packedPosition, String ownerId, long expiresAtTick, long maxExpiresAtTick) {
        public static final Codec<PlatformRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(PlatformRecord::dimension),
                Codec.LONG.fieldOf("packed_position").forGetter(PlatformRecord::packedPosition),
                Codec.STRING.fieldOf("owner").forGetter(PlatformRecord::ownerId),
                Codec.LONG.fieldOf("expires_at").forGetter(PlatformRecord::expiresAtTick),
                Codec.LONG.fieldOf("max_expires_at").forGetter(PlatformRecord::maxExpiresAtTick)
        ).apply(instance, PlatformRecord::new));

        public PlatformRecord withExpiry(long expiry) {
            return new PlatformRecord(dimension, packedPosition, ownerId, expiry, maxExpiresAtTick);
        }
    }
}

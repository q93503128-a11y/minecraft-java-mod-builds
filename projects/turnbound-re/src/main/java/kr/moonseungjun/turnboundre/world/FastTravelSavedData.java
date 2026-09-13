package kr.moonseungjun.turnboundre.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-owned fast-travel state.
 * World-shared registered anchor positions and per-player discovery are deliberately stored in separate maps.
 */
public final class FastTravelSavedData extends SavedData {
    public record AnchorLocation(String dimension, int x, int y, int z) {
        public static final Codec<AnchorLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(AnchorLocation::dimension),
                Codec.INT.fieldOf("x").forGetter(AnchorLocation::x),
                Codec.INT.fieldOf("y").forGetter(AnchorLocation::y),
                Codec.INT.fieldOf("z").forGetter(AnchorLocation::z)
        ).apply(instance, AnchorLocation::new));

        public AnchorLocation {
            if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("dimension required");
        }

        public BlockPos blockPos() { return new BlockPos(x, y, z); }
    }

    public static final Codec<FastTravelSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, AnchorLocation.CODEC)
                    .optionalFieldOf("anchors", Map.of())
                    .forGetter(FastTravelSavedData::anchorsSnapshot),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
                    .optionalFieldOf("discoveries", Map.of())
                    .forGetter(FastTravelSavedData::discoveriesSnapshot)
    ).apply(instance, FastTravelSavedData::new));

    public static final SavedDataType<FastTravelSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "world/fast_travel"),
            FastTravelSavedData::new,
            CODEC
    );

    private final Map<String, AnchorLocation> anchors = new LinkedHashMap<>();
    private final Map<String, List<String>> discoveries = new LinkedHashMap<>();

    public FastTravelSavedData() {}

    FastTravelSavedData(Map<String, AnchorLocation> anchors, Map<String, List<String>> discoveries) {
        if (anchors != null) {
            for (Map.Entry<String, AnchorLocation> entry : anchors.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                    throw new IllegalArgumentException("invalid fast-travel anchor save entry");
                }
                this.anchors.put(entry.getKey(), entry.getValue());
            }
        }
        if (discoveries != null) {
            for (Map.Entry<String, List<String>> entry : discoveries.entrySet()) {
                UUID.fromString(entry.getKey());
                this.discoveries.put(entry.getKey(), normalize(entry.getValue()));
            }
        }
    }

    public static FastTravelSavedData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server required");
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public void registerAnchor(String locator, String dimension, BlockPos arrival) {
        if (locator == null || locator.isBlank() || dimension == null || dimension.isBlank() || arrival == null) {
            throw new IllegalArgumentException("locator/dimension/arrival required");
        }
        AnchorLocation next = new AnchorLocation(dimension, arrival.getX(), arrival.getY(), arrival.getZ());
        AnchorLocation previous = anchors.put(locator, next);
        if (!next.equals(previous)) setDirty();
    }

    public Optional<AnchorLocation> anchor(String locator) {
        if (locator == null || locator.isBlank()) return Optional.empty();
        return Optional.ofNullable(anchors.get(locator));
    }

    public boolean discover(UUID playerId, String locator) {
        if (playerId == null || locator == null || locator.isBlank()) throw new IllegalArgumentException("playerId/locator required");
        String key = playerId.toString();
        LinkedHashSet<String> updated = new LinkedHashSet<>(discoveries.getOrDefault(key, List.of()));
        if (!updated.add(locator)) return false;
        discoveries.put(key, updated.stream().sorted().toList());
        setDirty();
        return true;
    }

    public boolean discovered(UUID playerId, String locator) {
        return playerId != null && locator != null && !locator.isBlank()
                && discoveries.getOrDefault(playerId.toString(), List.of()).contains(locator);
    }

    public Set<String> discovered(UUID playerId) {
        if (playerId == null) return Set.of();
        return Set.copyOf(discoveries.getOrDefault(playerId.toString(), List.of()));
    }

    public Map<String, AnchorLocation> anchorsSnapshot() { return Map.copyOf(anchors); }
    public Map<String, List<String>> discoveriesSnapshot() { return Map.copyOf(discoveries); }

    private static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException("blank discovered fast-travel locator");
            unique.add(value);
        }
        ArrayList<String> sorted = new ArrayList<>(unique);
        sorted.sort(String::compareTo);
        return List.copyOf(sorted);
    }
}

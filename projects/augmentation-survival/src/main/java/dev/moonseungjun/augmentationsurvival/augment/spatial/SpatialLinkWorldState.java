package dev.moonseungjun.augmentationsurvival.augment.spatial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Save-wide spatial-link graph. All mutation is performed by the server service. */
public record SpatialLinkWorldState(
        Map<String, GateRecord> gates,
        Map<String, String> defaultGateByOwner,
        Map<String, List<String>> favoritesByOwner
) {
    public static final Codec<SpatialLinkWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, GateRecord.CODEC)
                    .optionalFieldOf("gates", Map.of()).forGetter(SpatialLinkWorldState::gates),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("default_gate_by_owner", Map.of()).forGetter(SpatialLinkWorldState::defaultGateByOwner),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
                    .optionalFieldOf("favorites_by_owner", Map.of()).forGetter(SpatialLinkWorldState::favoritesByOwner)
    ).apply(instance, SpatialLinkWorldState::new));

    public SpatialLinkWorldState {
        gates = Map.copyOf(gates);
        defaultGateByOwner = Map.copyOf(defaultGateByOwner);
        Map<String, List<String>> normalized = new HashMap<>();
        favoritesByOwner.forEach((owner, ids) -> normalized.put(owner, List.copyOf(new LinkedHashSet<>(ids))));
        favoritesByOwner = Map.copyOf(normalized);
    }

    public static SpatialLinkWorldState initial() {
        return new SpatialLinkWorldState(Map.of(), Map.of(), Map.of());
    }

    public List<GateRecord> visibleGates(UUID viewer) {
        String viewerId = viewer.toString();
        return gates.values().stream()
                .filter(gate -> gate.owner().equals(viewerId) || gate.sharedWith().contains(viewerId))
                .sorted((a, b) -> {
                    boolean af = isFavorite(viewer, a.id());
                    boolean bf = isFavorite(viewer, b.id());
                    if (af != bf) return af ? -1 : 1;
                    int byName = a.name().compareToIgnoreCase(b.name());
                    return byName != 0 ? byName : a.id().compareTo(b.id());
                })
                .toList();
    }

    public boolean isFavorite(UUID owner, String gateId) {
        return favoritesByOwner.getOrDefault(owner.toString(), List.of()).contains(gateId);
    }

    public boolean isDefault(UUID owner, String gateId) {
        return gateId.equals(defaultGateByOwner.get(owner.toString()));
    }

    public SpatialLinkWorldState withGate(GateRecord gate) {
        Map<String, GateRecord> next = new HashMap<>(gates);
        next.put(gate.id(), gate);
        return new SpatialLinkWorldState(next, defaultGateByOwner, favoritesByOwner);
    }

    public SpatialLinkWorldState withoutGate(UUID owner, String gateId) {
        GateRecord gate = gates.get(gateId);
        if (gate == null || !gate.owner().equals(owner.toString())) return this;
        Map<String, GateRecord> nextGates = new HashMap<>(gates);
        nextGates.remove(gateId);
        Map<String, String> nextDefaults = new HashMap<>(defaultGateByOwner);
        nextDefaults.entrySet().removeIf(entry -> gateId.equals(entry.getValue()));
        Map<String, List<String>> nextFavorites = new HashMap<>();
        favoritesByOwner.forEach((key, value) -> nextFavorites.put(key, value.stream().filter(id -> !gateId.equals(id)).toList()));
        return new SpatialLinkWorldState(nextGates, nextDefaults, nextFavorites);
    }

    public SpatialLinkWorldState withDefault(UUID owner, String gateId) {
        if (!isVisible(owner, gateId)) return this;
        Map<String, String> next = new HashMap<>(defaultGateByOwner);
        next.put(owner.toString(), gateId);
        return new SpatialLinkWorldState(gates, next, favoritesByOwner);
    }

    public SpatialLinkWorldState toggleFavorite(UUID owner, String gateId) {
        if (!isVisible(owner, gateId)) return this;
        Map<String, List<String>> next = new HashMap<>(favoritesByOwner);
        Set<String> ids = new LinkedHashSet<>(next.getOrDefault(owner.toString(), List.of()));
        if (!ids.remove(gateId)) ids.add(gateId);
        next.put(owner.toString(), List.copyOf(ids));
        return new SpatialLinkWorldState(gates, defaultGateByOwner, next);
    }

    public SpatialLinkWorldState rename(UUID owner, String gateId, String name) {
        GateRecord gate = gates.get(gateId);
        if (gate == null || !gate.owner().equals(owner.toString())) return this;
        return withGate(gate.withName(name));
    }

    public SpatialLinkWorldState share(UUID owner, String gateId, UUID target, boolean shared) {
        GateRecord gate = gates.get(gateId);
        if (gate == null || !gate.owner().equals(owner.toString()) || owner.equals(target)) return this;
        LinkedHashSet<String> viewers = new LinkedHashSet<>(gate.sharedWith());
        if (shared) viewers.add(target.toString()); else viewers.remove(target.toString());
        return withGate(gate.withSharedWith(List.copyOf(viewers)));
    }

    public boolean isVisible(UUID viewer, String gateId) {
        GateRecord gate = gates.get(gateId);
        return gate != null && (gate.owner().equals(viewer.toString()) || gate.sharedWith().contains(viewer.toString()));
    }

    public record GateRecord(
            String id,
            String owner,
            String name,
            String dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            List<String> sharedWith
    ) {
        public static final Codec<GateRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(GateRecord::id),
                Codec.STRING.fieldOf("owner").forGetter(GateRecord::owner),
                Codec.STRING.fieldOf("name").forGetter(GateRecord::name),
                Codec.STRING.fieldOf("dimension").forGetter(GateRecord::dimension),
                Codec.DOUBLE.fieldOf("x").forGetter(GateRecord::x),
                Codec.DOUBLE.fieldOf("y").forGetter(GateRecord::y),
                Codec.DOUBLE.fieldOf("z").forGetter(GateRecord::z),
                Codec.FLOAT.fieldOf("yaw").forGetter(GateRecord::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(GateRecord::pitch),
                Codec.STRING.listOf().optionalFieldOf("shared_with", List.of()).forGetter(GateRecord::sharedWith)
        ).apply(instance, GateRecord::new));

        public GateRecord {
            sharedWith = List.copyOf(new ArrayList<>(new LinkedHashSet<>(sharedWith)));
        }

        public GateRecord withName(String value) {
            return new GateRecord(id, owner, value, dimension, x, y, z, yaw, pitch, sharedWith);
        }

        public GateRecord withSharedWith(List<String> value) {
            return new GateRecord(id, owner, name, dimension, x, y, z, yaw, pitch, value);
        }
    }
}

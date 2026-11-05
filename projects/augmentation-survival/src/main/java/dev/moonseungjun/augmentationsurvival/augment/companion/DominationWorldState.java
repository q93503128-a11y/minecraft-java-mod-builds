package dev.moonseungjun.augmentationsurvival.augment.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Save-wide ownership and companion identity index. Old saves with only occupied_by_owner
 * decode unchanged; their existing loaded entities are indexed again when their chunks load.
 */
public record DominationWorldState(
        Map<String, Integer> occupiedByOwner,
        Map<String, List<CompanionRecord>> companionsByOwner,
        List<String> pendingReleaseIds
) {
    public record CompanionRecord(String uuid, String entityId, String name, String dimension) {
        public static final Codec<CompanionRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(CompanionRecord::uuid),
                Codec.STRING.fieldOf("entity_id").forGetter(CompanionRecord::entityId),
                Codec.STRING.fieldOf("name").forGetter(CompanionRecord::name),
                Codec.STRING.fieldOf("dimension").forGetter(CompanionRecord::dimension)
        ).apply(instance, CompanionRecord::new));
    }

    public static final Codec<DominationWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("occupied_by_owner", Map.of())
                    .forGetter(DominationWorldState::occupiedByOwner),
            Codec.unboundedMap(Codec.STRING, CompanionRecord.CODEC.listOf())
                    .optionalFieldOf("companions_by_owner", Map.of())
                    .forGetter(DominationWorldState::companionsByOwner),
            Codec.STRING.listOf()
                    .optionalFieldOf("pending_release_ids", List.of())
                    .forGetter(DominationWorldState::pendingReleaseIds)
    ).apply(instance, DominationWorldState::new));

    public DominationWorldState {
        Map<String, Integer> normalized = new HashMap<>();
        occupiedByOwner.forEach((key, value) -> {
            if (key != null && value != null && value > 0) normalized.put(key, value);
        });
        occupiedByOwner = Map.copyOf(normalized);
        Map<String, List<CompanionRecord>> roster = new HashMap<>();
        companionsByOwner.forEach((key, records) -> roster.put(key, List.copyOf(records)));
        companionsByOwner = Map.copyOf(roster);
        pendingReleaseIds = List.copyOf(pendingReleaseIds);
    }

    public static DominationWorldState initial() {
        return new DominationWorldState(Map.of(), Map.of(), List.of());
    }

    public int occupied(UUID owner) {
        return Math.max(0, occupiedByOwner.getOrDefault(owner.toString(), 0));
    }

    public List<CompanionRecord> companions(UUID owner) {
        return companionsByOwner.getOrDefault(owner.toString(), List.of());
    }

    public boolean pending(UUID entityId) {
        return pendingReleaseIds.contains(entityId.toString());
    }

    public DominationWorldState withDelta(UUID owner, int delta) {
        Map<String, Integer> next = new HashMap<>(occupiedByOwner);
        String key = owner.toString();
        int count = Math.max(0, next.getOrDefault(key, 0) + delta);
        if (count == 0) next.remove(key); else next.put(key, count);
        return new DominationWorldState(next, companionsByOwner, pendingReleaseIds);
    }

    public DominationWorldState withCompanion(UUID owner, CompanionRecord record) {
        Map<String, List<CompanionRecord>> next = new HashMap<>(companionsByOwner);
        ArrayList<CompanionRecord> list = new ArrayList<>(companions(owner));
        list.removeIf(existing -> existing.uuid().equals(record.uuid()));
        list.add(record);
        next.put(owner.toString(), List.copyOf(list));
        return new DominationWorldState(occupiedByOwner, next, pendingReleaseIds);
    }

    public DominationWorldState withoutCompanion(UUID owner, UUID entityId, boolean pendingRelease) {
        Map<String, List<CompanionRecord>> next = new HashMap<>(companionsByOwner);
        ArrayList<CompanionRecord> list = new ArrayList<>(companions(owner));
        list.removeIf(record -> record.uuid().equals(entityId.toString()));
        if (list.isEmpty()) next.remove(owner.toString()); else next.put(owner.toString(), List.copyOf(list));
        ArrayList<String> pending = new ArrayList<>(pendingReleaseIds);
        if (pendingRelease && !pending.contains(entityId.toString())) pending.add(entityId.toString());
        return new DominationWorldState(occupiedByOwner, next, pending);
    }

    public DominationWorldState consumePending(UUID entityId) {
        if (!pending(entityId)) return this;
        ArrayList<String> pending = new ArrayList<>(pendingReleaseIds);
        pending.remove(entityId.toString());
        return new DominationWorldState(occupiedByOwner, companionsByOwner, pending);
    }
}

package dev.moonseungjun.augmentationsurvival.augment.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Save-wide authoritative occupancy, companion directory and deferred release intents. */
public record DominationWorldState(
        Map<String, Integer> occupiedByOwner,
        Map<String, Member> members,
        List<String> pendingReleases
) {
    public static final Codec<DominationWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("occupied_by_owner", Map.of())
                    .forGetter(DominationWorldState::occupiedByOwner),
            Codec.unboundedMap(Codec.STRING, Member.CODEC).optionalFieldOf("members", Map.of())
                    .forGetter(DominationWorldState::members),
            Codec.STRING.listOf().optionalFieldOf("pending_releases", List.of())
                    .forGetter(DominationWorldState::pendingReleases)
    ).apply(instance, DominationWorldState::new));

    public DominationWorldState {
        Map<String, Integer> cleanCounts = new HashMap<>();
        for (var entry : occupiedByOwner.entrySet()) {
            int count = Math.max(0, entry.getValue());
            if (count > 0) cleanCounts.put(entry.getKey(), count);
        }
        occupiedByOwner = Map.copyOf(cleanCounts);
        members = Map.copyOf(members);
        pendingReleases = List.copyOf(pendingReleases);
    }

    public static DominationWorldState initial() {
        return new DominationWorldState(Map.of(), Map.of(), List.of());
    }

    public int occupied(UUID owner) {
        return Math.max(0, occupiedByOwner.getOrDefault(owner.toString(), 0));
    }

    public Member member(UUID entityId) {
        return members.get(entityId.toString());
    }

    public List<Map.Entry<String, Member>> membersOf(UUID owner) {
        return members.entrySet().stream().filter(e -> e.getValue().owner().equals(owner.toString()))
                .sorted(Map.Entry.comparingByKey()).toList();
    }

    public boolean pendingRelease(UUID id) {
        return pendingReleases.contains(id.toString());
    }

    public DominationWorldState withLoaded(UUID id, Member member) {
        if (members.containsKey(id.toString())) return this;
        Map<String, Member> next = new HashMap<>(members);
        next.put(id.toString(), member);
        // A legacy companion was counted before the persistent roster was introduced.
        return new DominationWorldState(occupiedByOwner, next, pendingReleases);
    }

    public DominationWorldState withAcquired(UUID id, Member member) {
        Map<String, Member> next = new HashMap<>(members);
        Member previous = next.put(id.toString(), member);
        return new DominationWorldState(
                updatedCount(member.owner(), previous == null ? 1 : 0), next, pendingReleases
        );
    }

    /** A grave reserves the occupied slot until revival, release or expiry. */
    public DominationWorldState withDeath(UUID id, Member fallback, boolean reserved) {
        Map<String, Member> next = new HashMap<>(members);
        Member previous = next.getOrDefault(id.toString(), fallback);
        if (reserved) next.put(id.toString(), previous.asReserved());
        else next.remove(id.toString());
        return new DominationWorldState(
                reserved ? occupiedByOwner : updatedCount(previous.owner(), -1),
                next, pendingReleases
        );
    }

    public DominationWorldState withRevived(UUID id, Member member) {
        Map<String, Member> next = new HashMap<>(members);
        Member old = next.put(id.toString(), member);
        return new DominationWorldState(
                old == null ? updatedCount(member.owner(), 1) : occupiedByOwner,
                next, pendingReleases
        );
    }

    /** Returns one slot exactly once; unloaded mobs are cleared on their next load. */
    public DominationWorldState withReleased(UUID owner, UUID id, boolean entityAlreadyCleared) {
        Member old = members.get(id.toString());
        if (old == null || !old.owner().equals(owner.toString())) return this;
        Map<String, Member> next = new HashMap<>(members);
        next.remove(id.toString());
        List<String> pending = new ArrayList<>(pendingReleases);
        if (!entityAlreadyCleared && !old.reserved() && !pending.contains(id.toString())) {
            pending.add(id.toString());
        }
        return new DominationWorldState(updatedCount(old.owner(), -1), next, pending);
    }

    public DominationWorldState withClearedPending(UUID id) {
        if (!pendingRelease(id)) return this;
        List<String> next = new ArrayList<>(pendingReleases);
        next.remove(id.toString());
        return new DominationWorldState(occupiedByOwner, members, next);
    }

    /** Compatibility with historical occupancy callers. */
    public DominationWorldState withDelta(UUID owner, int delta) {
        return new DominationWorldState(updatedCount(owner.toString(), delta), members, pendingReleases);
    }

    private Map<String, Integer> updatedCount(String owner, int delta) {
        Map<String, Integer> next = new HashMap<>(occupiedByOwner);
        int value = Math.max(0, next.getOrDefault(owner, 0) + delta);
        if (value == 0) next.remove(owner);
        else next.put(owner, value);
        return next;
    }

    public record Member(String owner, String type, String dimension, boolean reserved) {
        public static final Codec<Member> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("owner").forGetter(Member::owner),
                Codec.STRING.fieldOf("type").forGetter(Member::type),
                Codec.STRING.fieldOf("dimension").forGetter(Member::dimension),
                Codec.BOOL.optionalFieldOf("reserved", false).forGetter(Member::reserved)
        ).apply(instance, Member::new));

        public Member asReserved() { return new Member(owner, type, dimension, true); }
    }
}

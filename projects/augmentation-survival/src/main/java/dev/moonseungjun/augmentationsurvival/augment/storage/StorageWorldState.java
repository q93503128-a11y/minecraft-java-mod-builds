package dev.moonseungjun.augmentationsurvival.augment.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/** Save-wide, server-authoritative storage state for Pockets and Remote Storage. */
public record StorageWorldState(
        Map<String, List<ItemStack>> pocketSegments,
        Map<String, List<ItemStack>> remoteSegments,
        Map<String, String> remoteGroupByOwner
) {
    public static final int POCKET_SIZE = 9;
    public static final int REMOTE_SEGMENT_SIZE = 27;

    private static final Codec<List<ItemStack>> STACK_LIST_CODEC = ItemStack.OPTIONAL_CODEC.listOf();

    public static final Codec<StorageWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, STACK_LIST_CODEC)
                    .optionalFieldOf("pocket_segments", Map.of())
                    .forGetter(StorageWorldState::pocketSegments),
            Codec.unboundedMap(Codec.STRING, STACK_LIST_CODEC)
                    .optionalFieldOf("remote_segments", Map.of())
                    .forGetter(StorageWorldState::remoteSegments),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("remote_group_by_owner", Map.of())
                    .forGetter(StorageWorldState::remoteGroupByOwner)
    ).apply(instance, StorageWorldState::new));

    public StorageWorldState {
        pocketSegments = sanitizeSegments(pocketSegments, POCKET_SIZE);
        remoteSegments = sanitizeSegments(remoteSegments, REMOTE_SEGMENT_SIZE);
        Map<String, String> groups = new HashMap<>();
        for (Map.Entry<String, String> entry : remoteGroupByOwner.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank()
                    && entry.getValue() != null && !entry.getValue().isBlank()) {
                groups.put(entry.getKey(), entry.getValue());
            }
        }
        remoteGroupByOwner = Map.copyOf(groups);
    }

    public static StorageWorldState initial() {
        return new StorageWorldState(Map.of(), Map.of(), Map.of());
    }

    public List<ItemStack> pocketSegment(UUID owner) {
        return segment(pocketSegments, owner, POCKET_SIZE);
    }

    public List<ItemStack> remoteSegment(UUID owner) {
        return segment(remoteSegments, owner, REMOTE_SEGMENT_SIZE);
    }

    public StorageWorldState withPocketSegment(UUID owner, List<ItemStack> items) {
        Map<String, List<ItemStack>> next = new HashMap<>(pocketSegments);
        putOrRemove(next, owner, items, POCKET_SIZE);
        return new StorageWorldState(next, remoteSegments, remoteGroupByOwner);
    }

    public StorageWorldState withRemoteSegment(UUID owner, List<ItemStack> items) {
        Map<String, List<ItemStack>> next = new HashMap<>(remoteSegments);
        putOrRemove(next, owner, items, REMOTE_SEGMENT_SIZE);
        return new StorageWorldState(pocketSegments, next, remoteGroupByOwner);
    }

    public Optional<String> groupId(UUID owner) {
        return Optional.ofNullable(remoteGroupByOwner.get(owner.toString()));
    }

    public List<UUID> members(UUID owner) {
        String group = remoteGroupByOwner.get(owner.toString());
        if (group == null) {
            return List.of(owner);
        }
        LinkedHashSet<UUID> members = new LinkedHashSet<>();
        members.add(owner);
        remoteGroupByOwner.forEach((rawOwner, rawGroup) -> {
            if (!group.equals(rawGroup)) return;
            try {
                members.add(UUID.fromString(rawOwner));
            } catch (IllegalArgumentException ignored) {
                // Corrupt legacy key is ignored; constructor keeps the raw entry so data is not destroyed silently.
            }
        });
        ArrayList<UUID> ordered = new ArrayList<>(members);
        ordered.sort((a, b) -> {
            if (a.equals(owner)) return b.equals(owner) ? 0 : -1;
            if (b.equals(owner)) return 1;
            return a.toString().compareTo(b.toString());
        });
        return List.copyOf(ordered);
    }

    public StorageWorldState mergeGroups(UUID first, UUID second, String groupId) {
        Set<UUID> members = new LinkedHashSet<>(members(first));
        members.addAll(members(second));
        Map<String, String> groups = new HashMap<>(remoteGroupByOwner);
        for (UUID member : members) {
            groups.put(member.toString(), groupId);
        }
        return new StorageWorldState(pocketSegments, remoteSegments, groups);
    }

    public StorageWorldState leaveGroup(UUID owner) {
        String group = remoteGroupByOwner.get(owner.toString());
        if (group == null) return this;
        Map<String, String> groups = new HashMap<>(remoteGroupByOwner);
        groups.remove(owner.toString());
        List<String> remaining = groups.entrySet().stream()
                .filter(entry -> group.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        if (remaining.size() <= 1) {
            for (String key : remaining) groups.remove(key);
        }
        return new StorageWorldState(pocketSegments, remoteSegments, groups);
    }

    private static Map<String, List<ItemStack>> sanitizeSegments(
            Map<String, List<ItemStack>> source,
            int size
    ) {
        Map<String, List<ItemStack>> sanitized = new HashMap<>();
        for (Map.Entry<String, List<ItemStack>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            List<ItemStack> normalized = normalize(entry.getValue(), size);
            if (!allEmpty(normalized)) sanitized.put(entry.getKey(), normalized);
        }
        return Map.copyOf(sanitized);
    }

    private static List<ItemStack> segment(Map<String, List<ItemStack>> source, UUID owner, int size) {
        return normalize(source.getOrDefault(owner.toString(), List.of()), size);
    }

    private static void putOrRemove(
            Map<String, List<ItemStack>> target,
            UUID owner,
            List<ItemStack> items,
            int size
    ) {
        List<ItemStack> normalized = normalize(items, size);
        if (allEmpty(normalized)) target.remove(owner.toString());
        else target.put(owner.toString(), normalized);
    }

    private static List<ItemStack> normalize(List<ItemStack> source, int size) {
        ArrayList<ItemStack> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            ItemStack stack = index < source.size() ? source.get(index) : ItemStack.EMPTY;
            result.add(stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return List.copyOf(result);
    }

    private static boolean allEmpty(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }
}

package kr.moonseungjun.survivalascension.production;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FieldDepotData extends SavedData {
    public static final int MAX_DEPOTS_PER_PLAYER = 3;
    public static final int MAX_LINKED_BARRELS_PER_DEPOT = 8;
    public static final int MAX_LINK_RADIUS = 6;

    public record DepotEntry(String dimension, int x, int y, int z) {
        private static final Codec<DepotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(DepotEntry::dimension),
                Codec.INT.fieldOf("x").forGetter(DepotEntry::x),
                Codec.INT.fieldOf("y").forGetter(DepotEntry::y),
                Codec.INT.fieldOf("z").forGetter(DepotEntry::z)
        ).apply(instance, DepotEntry::new));

        public BlockPos pos() { return new BlockPos(x, y, z); }
        private String key() { return positionKey(dimension, pos()); }
    }

    public record LinkedBarrel(String owner, String dimension, int anchorX, int anchorY, int anchorZ, int x, int y, int z) {
        private static final Codec<LinkedBarrel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("owner").forGetter(LinkedBarrel::owner),
                Codec.STRING.fieldOf("dimension").forGetter(LinkedBarrel::dimension),
                Codec.INT.fieldOf("anchor_x").forGetter(LinkedBarrel::anchorX),
                Codec.INT.fieldOf("anchor_y").forGetter(LinkedBarrel::anchorY),
                Codec.INT.fieldOf("anchor_z").forGetter(LinkedBarrel::anchorZ),
                Codec.INT.fieldOf("x").forGetter(LinkedBarrel::x),
                Codec.INT.fieldOf("y").forGetter(LinkedBarrel::y),
                Codec.INT.fieldOf("z").forGetter(LinkedBarrel::z)
        ).apply(instance, LinkedBarrel::new));

        public BlockPos anchorPos() { return new BlockPos(anchorX, anchorY, anchorZ); }
        public BlockPos pos() { return new BlockPos(x, y, z); }
        private String anchorKey() { return positionKey(dimension, anchorPos()); }
        private String key() { return positionKey(dimension, pos()); }
    }

    private record PlayerEntry(String uuid, List<DepotEntry> depots) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                DepotEntry.CODEC.listOf().optionalFieldOf("depots", List.of()).forGetter(PlayerEntry::depots)
        ).apply(instance, PlayerEntry::new));
    }

    public enum AddResult { ADDED, ALREADY_OWNED, CLAIMED_BY_OTHER, LIMIT_REACHED }
    public enum LinkResult { ADDED, ALREADY_LINKED, CLAIMED, LIMIT_REACHED, INVALID_ANCHOR, TOO_FAR }

    public static final SavedDataType<FieldDepotData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "field_depots_v1"),
            FieldDepotData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(FieldDepotData::entries),
                    LinkedBarrel.CODEC.listOf().optionalFieldOf("warehouse_links", List.of()).forGetter(FieldDepotData::linkedEntries)
            ).apply(instance, FieldDepotData::new))
    );

    private final Map<String, List<DepotEntry>> players = new HashMap<>();
    private final Map<String, List<LinkedBarrel>> warehouseLinks = new HashMap<>();

    public FieldDepotData() {}

    private FieldDepotData(List<PlayerEntry> entries, List<LinkedBarrel> links) {
        for (PlayerEntry entry : entries) {
            List<DepotEntry> sanitized = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (DepotEntry depot : entry.depots()) {
                if (sanitized.size() >= MAX_DEPOTS_PER_PLAYER) break;
                if (seen.add(depot.key())) sanitized.add(depot);
            }
            if (!sanitized.isEmpty()) players.put(entry.uuid(), sanitized);
        }

        Set<String> claimed = new HashSet<>();
        for (List<DepotEntry> depots : players.values()) {
            for (DepotEntry depot : depots) claimed.add(depot.key());
        }
        Map<String, Integer> perAnchor = new HashMap<>();
        for (LinkedBarrel link : links) {
            List<DepotEntry> ownerDepots = players.get(link.owner());
            if (ownerDepots == null) continue;
            boolean anchorExists = ownerDepots.stream().anyMatch(depot -> depot.key().equals(link.anchorKey()));
            if (!anchorExists || link.key().equals(link.anchorKey())) continue;
            if (link.anchorPos().distSqr(link.pos()) > MAX_LINK_RADIUS * MAX_LINK_RADIUS) continue;
            String anchorOwnerKey = link.owner() + "|" + link.anchorKey();
            if (perAnchor.getOrDefault(anchorOwnerKey, 0) >= MAX_LINKED_BARRELS_PER_DEPOT) continue;
            if (!claimed.add(link.key())) continue;
            warehouseLinks.computeIfAbsent(link.owner(), ignored -> new ArrayList<>()).add(link);
            perAnchor.merge(anchorOwnerKey, 1, Integer::sum);
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, depots) -> out.add(new PlayerEntry(uuid, List.copyOf(depots))));
        return out;
    }

    private List<LinkedBarrel> linkedEntries() {
        List<LinkedBarrel> out = new ArrayList<>();
        warehouseLinks.values().forEach(out::addAll);
        return out;
    }

    public static FieldDepotData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static FieldDepotData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private List<DepotEntry> state(ServerPlayer player) {
        return players.computeIfAbsent(player.getUUID().toString(), ignored -> new ArrayList<>());
    }

    private List<LinkedBarrel> linkState(ServerPlayer player) {
        return warehouseLinks.computeIfAbsent(player.getUUID().toString(), ignored -> new ArrayList<>());
    }

    public List<DepotEntry> depots(ServerPlayer player) { return List.copyOf(state(player)); }
    public int count(ServerPlayer player) { return state(player).size(); }

    public List<LinkedBarrel> linkedBarrels(ServerPlayer player, DepotEntry depot) {
        String anchorKey = depot.key();
        return linkState(player).stream().filter(link -> link.anchorKey().equals(anchorKey)).toList();
    }

    public int linkedCount(ServerPlayer player, DepotEntry depot) { return linkedBarrels(player, depot).size(); }
    public int totalLinkedCount(ServerPlayer player) { return linkState(player).size(); }

    public boolean owns(ServerPlayer player, String dimension, BlockPos pos) {
        String key = positionKey(dimension, pos);
        for (DepotEntry depot : state(player)) if (depot.key().equals(key)) return true;
        return false;
    }

    public boolean isRegisteredAnchor(String dimension, BlockPos pos) {
        String key = positionKey(dimension, pos);
        for (List<DepotEntry> depots : players.values()) {
            for (DepotEntry depot : depots) if (depot.key().equals(key)) return true;
        }
        return false;
    }

    public boolean isLinkedByOwner(ServerPlayer player, String dimension, BlockPos pos) {
        String key = positionKey(dimension, pos);
        for (LinkedBarrel link : linkState(player)) if (link.key().equals(key)) return true;
        return false;
    }

    public boolean isLinkedByAny(String dimension, BlockPos pos) {
        String key = positionKey(dimension, pos);
        for (List<LinkedBarrel> links : warehouseLinks.values()) {
            for (LinkedBarrel link : links) if (link.key().equals(key)) return true;
        }
        return false;
    }

    public boolean isPositionClaimed(String dimension, BlockPos pos) {
        return isRegisteredAnchor(dimension, pos) || isLinkedByAny(dimension, pos);
    }

    public AddResult add(ServerPlayer player, String dimension, BlockPos pos) {
        DepotEntry candidate = new DepotEntry(dimension, pos.getX(), pos.getY(), pos.getZ());
        String playerId = player.getUUID().toString();
        List<DepotEntry> own = state(player);
        for (DepotEntry depot : own) if (depot.key().equals(candidate.key())) return AddResult.ALREADY_OWNED;
        if (isLinkedByAny(dimension, pos)) return AddResult.CLAIMED_BY_OTHER;
        for (Map.Entry<String, List<DepotEntry>> entry : players.entrySet()) {
            if (entry.getKey().equals(playerId)) continue;
            for (DepotEntry depot : entry.getValue()) if (depot.key().equals(candidate.key())) return AddResult.CLAIMED_BY_OTHER;
        }
        if (own.size() >= MAX_DEPOTS_PER_PLAYER) return AddResult.LIMIT_REACHED;
        own.add(candidate);
        setDirty();
        return AddResult.ADDED;
    }

    public LinkResult addLink(ServerPlayer player, DepotEntry depot, BlockPos pos) {
        if (!owns(player, depot.dimension(), depot.pos())) return LinkResult.INVALID_ANCHOR;
        if (depot.pos().distSqr(pos) > MAX_LINK_RADIUS * MAX_LINK_RADIUS) return LinkResult.TOO_FAR;
        if (depot.pos().equals(pos)) return LinkResult.INVALID_ANCHOR;
        if (isLinkedByOwner(player, depot.dimension(), pos)) return LinkResult.ALREADY_LINKED;
        if (isPositionClaimed(depot.dimension(), pos)) return LinkResult.CLAIMED;
        if (linkedCount(player, depot) >= MAX_LINKED_BARRELS_PER_DEPOT) return LinkResult.LIMIT_REACHED;
        linkState(player).add(new LinkedBarrel(player.getUUID().toString(), depot.dimension(), depot.x(), depot.y(), depot.z(), pos.getX(), pos.getY(), pos.getZ()));
        setDirty();
        return LinkResult.ADDED;
    }

    public boolean removeLink(ServerPlayer player, String dimension, BlockPos pos) {
        String key = positionKey(dimension, pos);
        List<LinkedBarrel> links = linkState(player);
        boolean removed = links.removeIf(link -> link.key().equals(key));
        if (removed) {
            if (links.isEmpty()) warehouseLinks.remove(player.getUUID().toString());
            setDirty();
        }
        return removed;
    }

    public boolean remove(ServerPlayer player, DepotEntry depot) {
        List<DepotEntry> own = state(player);
        String anchorKey = depot.key();
        boolean removed = own.removeIf(value -> value.key().equals(anchorKey));
        if (removed) {
            List<LinkedBarrel> links = linkState(player);
            links.removeIf(link -> link.anchorKey().equals(anchorKey));
            if (links.isEmpty()) warehouseLinks.remove(player.getUUID().toString());
            if (own.isEmpty()) players.remove(player.getUUID().toString());
            setDirty();
        }
        return removed;
    }

    public boolean remove(ServerPlayer player, String dimension, BlockPos pos) {
        return remove(player, new DepotEntry(dimension, pos.getX(), pos.getY(), pos.getZ()));
    }

    private static String positionKey(String dimension, BlockPos pos) {
        return dimension + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }
}

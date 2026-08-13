package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persistent construction and physical-vessel state for the Silver River port. */
public final class ErdenRiverPortSavedData extends SavedData {
    private static final Codec<ErdenRiverPortSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("built_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.builtChunks)),
            Codec.STRING.optionalFieldOf("active_shipment", "").forGetter(data -> data.activeShipment),
            Codec.STRING.optionalFieldOf("boat_uuid", "").forGetter(data -> data.boatUuid),
            Codec.INT.optionalFieldOf("waypoint", 0).forGetter(data -> data.waypoint),
            Codec.LONG.optionalFieldOf("vessels_spawned", 0L).forGetter(data -> data.vesselsSpawned),
            Codec.LONG.optionalFieldOf("vessels_docked", 0L).forGetter(data -> data.vesselsDocked)
    ).apply(instance, ErdenRiverPortSavedData::new));

    public static final SavedDataType<ErdenRiverPortSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_river_port"),
            level -> new ErdenRiverPortSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> builtChunks;
    private String activeShipment;
    private String boatUuid;
    private int waypoint;
    private long vesselsSpawned;
    private long vesselsDocked;

    public ErdenRiverPortSavedData() {
        this(0, List.of(), "", "", 0, 0L, 0L);
    }

    private ErdenRiverPortSavedData(
            int revision,
            List<Long> builtChunks,
            String activeShipment,
            String boatUuid,
            int waypoint,
            long vesselsSpawned,
            long vesselsDocked) {
        this.revision = Math.max(0, revision);
        this.builtChunks = new HashSet<>(builtChunks);
        this.activeShipment = activeShipment == null ? "" : activeShipment;
        this.boatUuid = boatUuid == null ? "" : boatUuid;
        this.waypoint = Math.max(0, waypoint);
        this.vesselsSpawned = Math.max(0L, vesselsSpawned);
        this.vesselsDocked = Math.max(0L, vesselsDocked);
    }

    public boolean needsChunk(long packed, int expectedRevision) {
        return revision != expectedRevision || !builtChunks.contains(packed);
    }

    public boolean builtChunk(long packed, int expectedRevision) {
        return revision == expectedRevision && builtChunks.contains(packed);
    }

    public void markChunk(long packed, int expectedRevision) {
        if (revision != expectedRevision) {
            revision = expectedRevision;
            builtChunks.clear();
        }
        if (builtChunks.add(packed)) setDirty();
    }

    public int builtChunkCount(int expectedRevision) {
        return revision == expectedRevision ? builtChunks.size() : 0;
    }

    public String activeShipment() {
        return activeShipment;
    }

    public String boatUuid() {
        return boatUuid;
    }

    public int waypoint() {
        return waypoint;
    }

    public long vesselsSpawned() {
        return vesselsSpawned;
    }

    public long vesselsDocked() {
        return vesselsDocked;
    }

    public void assignVessel(String shipmentId, String uuid) {
        activeShipment = shipmentId == null ? "" : shipmentId;
        boatUuid = uuid == null ? "" : uuid;
        waypoint = 0;
        vesselsSpawned++;
        setDirty();
    }

    public void setVesselIdentity(String uuid) {
        String replacement = uuid == null ? "" : uuid;
        if (replacement.equals(boatUuid)) return;
        boatUuid = replacement;
        setDirty();
    }

    public void setWaypoint(int next) {
        int safe = Math.max(0, next);
        if (safe == waypoint) return;
        waypoint = safe;
        setDirty();
    }

    public void markDocked() {
        vesselsDocked++;
        setDirty();
    }

    public void clearVessel() {
        if (activeShipment.isEmpty() && boatUuid.isEmpty() && waypoint == 0) return;
        activeShipment = "";
        boatUuid = "";
        waypoint = 0;
        setDirty();
    }
}

package kr.moonseungjun.earthtostars.ship.persistence.minecraft;

import com.mojang.serialization.Codec;
import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.interior.InteriorAssignmentTable;
import kr.moonseungjun.earthtostars.ship.interior.InteriorRef;
import kr.moonseungjun.earthtostars.ship.interior.InteriorSlotLayout;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InteriorSavedData extends SavedData {
    private static final Codec<Map<String, Long>> ASSIGNMENTS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG);
    public static final Codec<InteriorSavedData> CODEC = ASSIGNMENTS_CODEC.xmap(
            InteriorSavedData::new,
            InteriorSavedData::encodeAssignments
    );
    public static final SavedDataType<InteriorSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EarthToStars.MOD_ID, "ship_interiors"),
            InteriorSavedData::new,
            CODEC
    );

    private final InteriorAssignmentTable assignments;

    public InteriorSavedData() {
        this.assignments = InteriorAssignmentTable.empty();
    }

    private InteriorSavedData(Map<String, Long> encoded) {
        Map<ShipId, Long> decoded = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : encoded.entrySet()) {
            try {
                ShipId shipId = new ShipId(UUID.fromString(entry.getKey()));
                if (decoded.putIfAbsent(shipId, entry.getValue()) != null) {
                    throw new IllegalStateException("duplicate persisted interior ship id: " + shipId);
                }
            } catch (RuntimeException invalid) {
                throw new IllegalStateException("invalid persisted interior assignment for " + entry.getKey(), invalid);
            }
        }
        this.assignments = InteriorAssignmentTable.restore(decoded);
    }

    public static InteriorSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public synchronized InteriorRef getOrAllocate(ShipId shipId) {
        boolean existed = assignments.find(shipId).isPresent();
        InteriorRef ref = assignments.getOrAllocate(shipId);
        if (!existed) {
            setDirty();
        }
        return ref;
    }

    public synchronized Optional<InteriorRef> find(ShipId shipId) {
        return assignments.find(shipId);
    }

    public synchronized Optional<ShipId> findShipAt(double x, double z) {
        try {
            return assignments.findShip(InteriorSlotLayout.slotAt(x, z));
        } catch (IllegalArgumentException outsideGrid) {
            return Optional.empty();
        }
    }

    private synchronized Map<String, Long> encodeAssignments() {
        Map<String, Long> encoded = new LinkedHashMap<>();
        assignments.snapshot().forEach((shipId, slot) -> encoded.put(shipId.toString(), slot));
        return Map.copyOf(encoded);
    }
}

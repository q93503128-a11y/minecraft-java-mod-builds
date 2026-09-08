package kr.moonseungjun.earthtostars.ship.persistence.minecraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.systems.ShipSystemsSnapshot;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShipSystemsSavedData extends SavedData {
    private record PersistedSystems(double powerStored, Map<String, Integer> ammoAmounts) {
        private static final Codec<PersistedSystems> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("power_stored").forGetter(PersistedSystems::powerStored),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ammo").forGetter(PersistedSystems::ammoAmounts)
        ).apply(instance, PersistedSystems::new));
    }

    private static final Codec<Map<String, PersistedSystems>> ENTRIES_CODEC = Codec.unboundedMap(Codec.STRING, PersistedSystems.CODEC);
    public static final Codec<ShipSystemsSavedData> CODEC = ENTRIES_CODEC.xmap(
            ShipSystemsSavedData::new,
            data -> Map.copyOf(data.entries)
    );
    public static final SavedDataType<ShipSystemsSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EarthToStars.MOD_ID, "ship_systems"),
            ShipSystemsSavedData::new,
            CODEC
    );

    private final Map<String, PersistedSystems> entries;

    public ShipSystemsSavedData() {
        this(Map.of());
    }

    private ShipSystemsSavedData(Map<String, PersistedSystems> entries) {
        this.entries = new LinkedHashMap<>(entries);
        decodeAll();
    }

    public static ShipSystemsSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public synchronized void put(ShipSystemsSnapshot snapshot) {
        PersistedSystems next = new PersistedSystems(snapshot.powerStored(), snapshot.ammoAmounts());
        PersistedSystems previous = entries.put(snapshot.shipId().toString(), next);
        if (!next.equals(previous)) {
            setDirty();
        }
    }

    public synchronized Optional<ShipSystemsSnapshot> find(ShipId shipId) {
        PersistedSystems persisted = entries.get(shipId.toString());
        return persisted == null ? Optional.empty() : Optional.of(decode(shipId.toString(), persisted));
    }

    public synchronized Map<ShipId, ShipSystemsSnapshot> decodeAll() {
        Map<ShipId, ShipSystemsSnapshot> decoded = new LinkedHashMap<>();
        for (Map.Entry<String, PersistedSystems> entry : entries.entrySet()) {
            ShipSystemsSnapshot snapshot = decode(entry.getKey(), entry.getValue());
            if (decoded.putIfAbsent(snapshot.shipId(), snapshot) != null) {
                throw new IllegalStateException("duplicate persisted ship systems id: " + snapshot.shipId());
            }
        }
        return Map.copyOf(decoded);
    }

    public synchronized int size() {
        return entries.size();
    }

    private static ShipSystemsSnapshot decode(String encodedShipId, PersistedSystems persisted) {
        try {
            ShipId shipId = new ShipId(UUID.fromString(encodedShipId));
            return new ShipSystemsSnapshot(shipId, persisted.powerStored(), persisted.ammoAmounts());
        } catch (RuntimeException corrupt) {
            throw new IllegalStateException("invalid persisted ship systems for " + encodedShipId, corrupt);
        }
    }
}

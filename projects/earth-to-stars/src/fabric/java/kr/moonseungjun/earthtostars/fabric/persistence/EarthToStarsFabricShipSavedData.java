package kr.moonseungjun.earthtostars.fabric.persistence;

import com.mojang.serialization.Codec;
import kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric;
import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.persistence.ShipStateCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EarthToStarsFabricShipSavedData extends SavedData {
    private static final Codec<Map<String, String>> PAYLOADS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);
    public static final Codec<EarthToStarsFabricShipSavedData> CODEC = PAYLOADS_CODEC.xmap(
            EarthToStarsFabricShipSavedData::new,
            data -> Map.copyOf(data.encodedShips)
    );
    public static final SavedDataType<EarthToStarsFabricShipSavedData> TYPE = new SavedDataType<>(
            EarthToStarsFabric.id("ships"),
            EarthToStarsFabricShipSavedData::new,
            CODEC,
            null
    );

    private final Map<String, String> encodedShips;

    public EarthToStarsFabricShipSavedData() {
        this(Map.of());
    }

    private EarthToStarsFabricShipSavedData(Map<String, String> encodedShips) {
        this.encodedShips = new LinkedHashMap<>(encodedShips);
    }

    public static EarthToStarsFabricShipSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public synchronized void put(ShipState state) {
        encodedShips.put(
                state.shipId().toString(),
                Base64.getEncoder().encodeToString(ShipStateCodec.encode(state))
        );
        setDirty();
    }

    public synchronized boolean remove(ShipId shipId) {
        if (encodedShips.remove(shipId.toString()) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public synchronized List<ShipState> decodeAll(ModuleCatalog catalog) {
        List<ShipState> states = new ArrayList<>(encodedShips.size());
        for (Map.Entry<String, String> entry : encodedShips.entrySet()) {
            try {
                ShipState state = ShipStateCodec.decode(Base64.getDecoder().decode(entry.getValue()), catalog);
                if (!state.shipId().toString().equals(entry.getKey())) {
                    throw new IllegalStateException("persisted ship key/id mismatch for " + entry.getKey());
                }
                states.add(state);
            } catch (RuntimeException corrupt) {
                throw new IllegalStateException("failed to decode persisted ship " + entry.getKey(), corrupt);
            }
        }
        return List.copyOf(states);
    }

    public synchronized int size() {
        return encodedShips.size();
    }
}

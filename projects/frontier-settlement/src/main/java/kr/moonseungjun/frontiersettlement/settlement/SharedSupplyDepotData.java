package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SharedSupplyDepotData extends SavedData {
    private static final int MAX_DEPOTS = 32;

    public static final SavedDataType<SharedSupplyDepotData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "shared_supply_depots"),
            SharedSupplyDepotData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(SharedSupplyDepotData::positionLongs)
            ).apply(instance, SharedSupplyDepotData::new))
    );

    private final List<Long> positions = new ArrayList<>();

    public SharedSupplyDepotData() {}

    private SharedSupplyDepotData(List<Long> positions) {
        Set<Long> unique = new LinkedHashSet<>(positions);
        for (long value : unique) {
            if (this.positions.size() >= MAX_DEPOTS) break;
            this.positions.add(value);
        }
    }

    public static SharedSupplyDepotData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Long> positionLongs() {
        return List.copyOf(positions);
    }

    public List<BlockPos> positions() {
        return positions.stream().map(BlockPos::of).toList();
    }

    public boolean add(BlockPos pos) {
        long packed = pos.asLong();
        if (positions.contains(packed) || positions.size() >= MAX_DEPOTS) return false;
        positions.add(packed);
        setDirty();
        return true;
    }

    public boolean remove(BlockPos pos) {
        boolean removed = positions.remove(pos.asLong());
        if (removed) setDirty();
        return removed;
    }
}

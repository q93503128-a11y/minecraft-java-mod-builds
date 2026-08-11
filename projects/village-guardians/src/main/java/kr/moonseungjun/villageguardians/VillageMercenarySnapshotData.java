package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VillageMercenarySnapshotData extends SavedData {
    private static final Codec<VillageMercenarySnapshotData> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .xmap(VillageMercenarySnapshotData::new, VillageMercenarySnapshotData::entries);

    public static final SavedDataType<VillageMercenarySnapshotData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_mercenary_night_snapshot"),
            level -> new VillageMercenarySnapshotData(),
            level -> CODEC);

    private Map<String, String> entries;

    public VillageMercenarySnapshotData() {
        this(Map.of());
    }

    private VillageMercenarySnapshotData(Map<String, String> entries) {
        this.entries = new LinkedHashMap<>(entries);
    }

    public Map<String, String> entries() {
        return new LinkedHashMap<>(entries);
    }

    public void replace(Map<String, String> updated) {
        entries = new LinkedHashMap<>(updated);
        setDirty();
    }
}

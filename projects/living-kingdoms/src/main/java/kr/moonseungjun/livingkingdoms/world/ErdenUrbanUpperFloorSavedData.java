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

/** Remembers which streamed Erden urban buildings received their vertical interior pass. */
public final class ErdenUrbanUpperFloorSavedData extends SavedData {
    private static final Codec<ErdenUrbanUpperFloorSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("completed_entrances", List.of())
                    .forGetter(data -> List.copyOf(data.completedEntrances))
    ).apply(instance, ErdenUrbanUpperFloorSavedData::new));

    public static final SavedDataType<ErdenUrbanUpperFloorSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_urban_upper_floors"),
            level -> new ErdenUrbanUpperFloorSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> completedEntrances;

    public ErdenUrbanUpperFloorSavedData() {
        this(0, List.of());
    }

    private ErdenUrbanUpperFloorSavedData(int revision, List<Long> completedEntrances) {
        this.revision = Math.max(0, revision);
        this.completedEntrances = new HashSet<>(completedEntrances);
    }

    public boolean isComplete(long entranceKey, int currentRevision) {
        return revision == currentRevision && completedEntrances.contains(entranceKey);
    }

    public void markComplete(long entranceKey, int currentRevision) {
        if (revision != currentRevision) {
            revision = currentRevision;
            completedEntrances.clear();
        }
        if (completedEntrances.add(entranceKey)) setDirty();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completedEntrances.size() : 0;
    }
}

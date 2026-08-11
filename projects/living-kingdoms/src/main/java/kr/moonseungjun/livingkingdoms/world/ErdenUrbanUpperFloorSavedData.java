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
                    .forGetter(data -> List.copyOf(data.completedEntrances)),
            Codec.LONG.listOf().optionalFieldOf("built_entrances", List.of())
                    .forGetter(data -> List.copyOf(data.builtEntrances))
    ).apply(instance, ErdenUrbanUpperFloorSavedData::new));

    public static final SavedDataType<ErdenUrbanUpperFloorSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_urban_upper_floors"),
            level -> new ErdenUrbanUpperFloorSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> completedEntrances;
    private final Set<Long> builtEntrances;

    public ErdenUrbanUpperFloorSavedData() {
        this(0, List.of(), List.of());
    }

    private ErdenUrbanUpperFloorSavedData(
            int revision,
            List<Long> completedEntrances,
            List<Long> builtEntrances) {
        this.revision = Math.max(0, revision);
        this.completedEntrances = new HashSet<>(completedEntrances);
        this.builtEntrances = new HashSet<>(builtEntrances);
    }

    public boolean isComplete(long entranceKey, int currentRevision) {
        return revision == currentRevision && completedEntrances.contains(entranceKey);
    }

    public void markComplete(long entranceKey, int currentRevision, boolean built) {
        if (revision != currentRevision) {
            revision = currentRevision;
            completedEntrances.clear();
            builtEntrances.clear();
        }
        boolean changed = completedEntrances.add(entranceKey);
        if (built) changed |= builtEntrances.add(entranceKey);
        if (changed) setDirty();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completedEntrances.size() : 0;
    }

    public int builtCount(int currentRevision) {
        return revision == currentRevision ? builtEntrances.size() : 0;
    }
}

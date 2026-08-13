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

/** Persists source-safe authored floors created inside structurally approved upper voids. */
public final class ErdenUrbanAuthoredNewFloorSavedData extends SavedData {
    private static final Codec<ErdenUrbanAuthoredNewFloorSavedData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
                    Codec.LONG.listOf().optionalFieldOf("prepared_entrances", List.of())
                            .forGetter(data -> List.copyOf(data.preparedEntrances)),
                    Codec.LONG.listOf().optionalFieldOf("completed_entrances", List.of())
                            .forGetter(data -> List.copyOf(data.completedEntrances))
            ).apply(instance, ErdenUrbanAuthoredNewFloorSavedData::new));

    public static final SavedDataType<ErdenUrbanAuthoredNewFloorSavedData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            LivingKingdoms.MOD_ID, "erden_urban_authored_new_floors"),
                    level -> new ErdenUrbanAuthoredNewFloorSavedData(),
                    level -> CODEC
            );

    private int revision;
    private final Set<Long> preparedEntrances;
    private final Set<Long> completedEntrances;

    public ErdenUrbanAuthoredNewFloorSavedData() {
        this(0, List.of(), List.of());
    }

    private ErdenUrbanAuthoredNewFloorSavedData(
            int revision,
            List<Long> preparedEntrances,
            List<Long> completedEntrances) {
        this.revision = Math.max(0, revision);
        this.preparedEntrances = new HashSet<>(preparedEntrances);
        this.completedEntrances = new HashSet<>(completedEntrances);
    }

    public boolean isPrepared(long entranceKey, int currentRevision) {
        return revision == currentRevision
                && (preparedEntrances.contains(entranceKey)
                || completedEntrances.contains(entranceKey));
    }

    public boolean isCompleted(long entranceKey, int currentRevision) {
        return revision == currentRevision && completedEntrances.contains(entranceKey);
    }

    public void markPrepared(long entranceKey, int currentRevision) {
        ensureRevision(currentRevision);
        if (preparedEntrances.add(entranceKey)) setDirty();
    }

    public void markCompleted(long entranceKey, int currentRevision) {
        ensureRevision(currentRevision);
        boolean changed = preparedEntrances.add(entranceKey);
        changed |= completedEntrances.add(entranceKey);
        if (changed) setDirty();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completedEntrances.size() : 0;
    }

    private void ensureRevision(int currentRevision) {
        if (revision == currentRevision) return;
        revision = currentRevision;
        preparedEntrances.clear();
        completedEntrances.clear();
        setDirty();
    }
}

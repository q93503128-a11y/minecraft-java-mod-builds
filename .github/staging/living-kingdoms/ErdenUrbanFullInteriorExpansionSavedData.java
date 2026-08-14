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

/** Persists physically materialized extra Erden interior levels beyond the legacy first upper floor. */
public final class ErdenUrbanFullInteriorExpansionSavedData extends SavedData {
    private static final Codec<ErdenUrbanFullInteriorExpansionSavedData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
                    Codec.LONG.listOf().optionalFieldOf("completed_entrances", List.of())
                            .forGetter(data -> List.copyOf(data.completedEntrances))
            ).apply(instance, ErdenUrbanFullInteriorExpansionSavedData::new));

    public static final SavedDataType<ErdenUrbanFullInteriorExpansionSavedData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            LivingKingdoms.MOD_ID, "erden_urban_full_interior_expansion"),
                    level -> new ErdenUrbanFullInteriorExpansionSavedData(),
                    level -> CODEC
            );

    private int revision;
    private final Set<Long> completedEntrances;

    public ErdenUrbanFullInteriorExpansionSavedData() {
        this(0, List.of());
    }

    private ErdenUrbanFullInteriorExpansionSavedData(int revision, List<Long> completedEntrances) {
        this.revision = Math.max(0, revision);
        this.completedEntrances = new HashSet<>(completedEntrances);
    }

    public boolean isCompleted(long entranceKey, int currentRevision) {
        return revision == currentRevision && completedEntrances.contains(entranceKey);
    }

    public void markCompleted(long entranceKey, int currentRevision) {
        ensureRevision(currentRevision);
        if (completedEntrances.add(entranceKey)) setDirty();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completedEntrances.size() : 0;
    }

    private void ensureRevision(int currentRevision) {
        if (revision == currentRevision) return;
        revision = currentRevision;
        completedEntrances.clear();
        setDirty();
    }
}

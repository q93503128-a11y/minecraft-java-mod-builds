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

/** Remembers which capital entrances already have a terrain-matched walkable threshold. */
public final class ErdenEntranceThresholdSavedData extends SavedData {
    private static final Codec<ErdenEntranceThresholdSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("completed", List.of())
                    .forGetter(data -> List.copyOf(data.completed))
    ).apply(instance, ErdenEntranceThresholdSavedData::new));

    public static final SavedDataType<ErdenEntranceThresholdSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_entrance_thresholds"),
            level -> new ErdenEntranceThresholdSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> completed;

    public ErdenEntranceThresholdSavedData() {
        this(0, List.of());
    }

    private ErdenEntranceThresholdSavedData(int revision, List<Long> completed) {
        this.revision = Math.max(0, revision);
        this.completed = new HashSet<>(completed);
    }

    public boolean isComplete(long key, int currentRevision) {
        return revision == currentRevision && completed.contains(key);
    }

    public void markComplete(long key, int currentRevision) {
        if (revision != currentRevision) {
            revision = currentRevision;
            completed.clear();
        }
        if (completed.add(key)) setDirty();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completed.size() : 0;
    }
}

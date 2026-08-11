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

/** Persists which attributed Erden landmark entrances received their functional interior layer. */
public final class ErdenLandmarkInteriorSavedData extends SavedData {
    private static final Codec<ErdenLandmarkInteriorSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("completed_landmarks", List.of())
                    .forGetter(data -> List.copyOf(data.completedLandmarks))
    ).apply(instance, ErdenLandmarkInteriorSavedData::new));

    public static final SavedDataType<ErdenLandmarkInteriorSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_landmark_interiors"),
            level -> new ErdenLandmarkInteriorSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> completedLandmarks;

    public ErdenLandmarkInteriorSavedData() {
        this(0, List.of());
    }

    private ErdenLandmarkInteriorSavedData(int revision, List<Long> completedLandmarks) {
        this.revision = Math.max(0, revision);
        this.completedLandmarks = new HashSet<>(completedLandmarks);
    }

    public boolean isComplete(long key, int currentRevision) {
        return revision == currentRevision && completedLandmarks.contains(key);
    }

    public void markComplete(long key, int currentRevision) {
        if (revision != currentRevision) {
            revision = currentRevision;
            completedLandmarks.clear();
        }
        if (completedLandmarks.add(key)) setDirty();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completedLandmarks.size() : 0;
    }
}

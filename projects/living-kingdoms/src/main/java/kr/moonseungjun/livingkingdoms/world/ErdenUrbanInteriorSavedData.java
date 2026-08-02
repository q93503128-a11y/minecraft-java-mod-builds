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

/** Remembers which streamed urban entrances already received the current functional interior. */
public final class ErdenUrbanInteriorSavedData extends SavedData {
    private static final Codec<ErdenUrbanInteriorSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("completed_entrances", List.of())
                    .forGetter(data -> List.copyOf(data.completedEntrances))
    ).apply(instance, ErdenUrbanInteriorSavedData::new));

    public static final SavedDataType<ErdenUrbanInteriorSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_urban_interiors"),
            level -> new ErdenUrbanInteriorSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> completedEntrances;

    public ErdenUrbanInteriorSavedData() {
        this(0, List.of());
    }

    private ErdenUrbanInteriorSavedData(int revision, List<Long> completedEntrances) {
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

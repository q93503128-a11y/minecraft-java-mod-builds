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

/**
 * Tracks capital chunks that already received the non-functional micro-infill pass.
 *
 * This intentionally lives outside {@link ErdenCapitalChunkSavedData}: decorative courtyards,
 * rear-yard clutter and alley corners must never change the canonical functional plot IDs,
 * household assignments or economy/workplace counts.
 */
public final class ErdenUrbanMicroInfillSavedData extends SavedData {
    private static final Codec<ErdenUrbanMicroInfillSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("completed_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.completedChunks))
    ).apply(instance, ErdenUrbanMicroInfillSavedData::new));

    public static final SavedDataType<ErdenUrbanMicroInfillSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_urban_micro_infill"),
            level -> new ErdenUrbanMicroInfillSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> completedChunks;

    public ErdenUrbanMicroInfillSavedData() {
        this(0, List.of());
    }

    private ErdenUrbanMicroInfillSavedData(int revision, List<Long> completedChunks) {
        this.revision = Math.max(0, revision);
        this.completedChunks = new HashSet<>(completedChunks);
    }

    public boolean needs(long chunkPos, int currentRevision) {
        return !isCompleted(chunkPos, currentRevision);
    }

    public boolean isCompleted(long chunkPos, int currentRevision) {
        return revision == currentRevision && completedChunks.contains(chunkPos);
    }

    public void mark(long chunkPos, int currentRevision) {
        if (revision != currentRevision) {
            revision = currentRevision;
            completedChunks.clear();
        }
        if (completedChunks.add(chunkPos)) setDirty();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completedChunks.size() : 0;
    }
}

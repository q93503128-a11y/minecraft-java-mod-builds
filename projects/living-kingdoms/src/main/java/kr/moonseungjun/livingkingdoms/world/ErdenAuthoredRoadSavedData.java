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

/** Tracks capital cells normalized to authored terrain heights independently of older capital saves. */
public final class ErdenAuthoredRoadSavedData extends SavedData {
    public static final int REVISION = 1;

    private static final Codec<ErdenAuthoredRoadSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("normalized_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.normalizedChunks)),
            Codec.LONG.optionalFieldOf("road_columns", 0L).forGetter(data -> data.roadColumns),
            Codec.LONG.optionalFieldOf("culvert_cells", 0L).forGetter(data -> data.culvertCells),
            Codec.LONG.optionalFieldOf("canopy_blocks_removed", 0L).forGetter(data -> data.canopyBlocksRemoved)
    ).apply(instance, ErdenAuthoredRoadSavedData::new));

    public static final SavedDataType<ErdenAuthoredRoadSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_authored_roads"),
            level -> new ErdenAuthoredRoadSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> normalizedChunks;
    private long roadColumns;
    private long culvertCells;
    private long canopyBlocksRemoved;

    public ErdenAuthoredRoadSavedData() {
        this(REVISION, List.of(), 0L, 0L, 0L);
    }

    private ErdenAuthoredRoadSavedData(
            int revision,
            List<Long> normalizedChunks,
            long roadColumns,
            long culvertCells,
            long canopyBlocksRemoved) {
        this.revision = Math.max(0, revision);
        this.normalizedChunks = new HashSet<>(normalizedChunks);
        this.roadColumns = Math.max(0L, roadColumns);
        this.culvertCells = Math.max(0L, culvertCells);
        this.canopyBlocksRemoved = Math.max(0L, canopyBlocksRemoved);
        ensureRevision();
    }

    public boolean needs(long chunkPos) {
        ensureRevision();
        return !normalizedChunks.contains(chunkPos);
    }

    public boolean isNormalized(long chunkPos) {
        ensureRevision();
        return normalizedChunks.contains(chunkPos);
    }

    public void markNormalized(long chunkPos, long roads, long culverts, long removed) {
        ensureRevision();
        if (!normalizedChunks.add(chunkPos)) return;
        roadColumns += Math.max(0L, roads);
        culvertCells += Math.max(0L, culverts);
        canopyBlocksRemoved += Math.max(0L, removed);
        setDirty();
    }

    public int normalizedChunkCount() {
        ensureRevision();
        return normalizedChunks.size();
    }

    public long roadColumns() {
        return roadColumns;
    }

    public long culvertCells() {
        return culvertCells;
    }

    public long canopyBlocksRemoved() {
        return canopyBlocksRemoved;
    }

    private void ensureRevision() {
        if (revision == REVISION) return;
        revision = REVISION;
        normalizedChunks.clear();
        roadColumns = 0L;
        culvertCells = 0L;
        canopyBlocksRemoved = 0L;
        setDirty();
    }
}

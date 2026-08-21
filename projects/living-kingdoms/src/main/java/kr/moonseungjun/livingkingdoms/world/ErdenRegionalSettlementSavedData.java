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

/** Persists streamed second-ring village construction without pinning remote chunks in memory. */
public final class ErdenRegionalSettlementSavedData extends SavedData {
    private static final Codec<ErdenRegionalSettlementSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("built_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.builtChunks)),
            Codec.STRING.listOf().optionalFieldOf("completed_centres", List.of())
                    .forGetter(data -> List.copyOf(data.completedCentres)),
            Codec.LONG.optionalFieldOf("total_writes", 0L).forGetter(data -> data.totalWrites)
    ).apply(instance, ErdenRegionalSettlementSavedData::new));

    public static final SavedDataType<ErdenRegionalSettlementSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_regional_settlements"),
            level -> new ErdenRegionalSettlementSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> builtChunks;
    private final Set<String> completedCentres;
    private long totalWrites;

    public ErdenRegionalSettlementSavedData() {
        this(0, List.of(), List.of(), 0L);
    }

    private ErdenRegionalSettlementSavedData(
            int revision,
            List<Long> builtChunks,
            List<String> completedCentres,
            long totalWrites) {
        this.revision = Math.max(0, revision);
        this.builtChunks = new HashSet<>(builtChunks);
        this.completedCentres = new HashSet<>(completedCentres);
        this.totalWrites = Math.max(0L, totalWrites);
    }

    public boolean needs(long chunkKey, int currentRevision) {
        return revision != currentRevision || !builtChunks.contains(chunkKey);
    }

    public boolean isBuilt(long chunkKey, int currentRevision) {
        return revision == currentRevision && builtChunks.contains(chunkKey);
    }

    public void markChunk(long chunkKey, int currentRevision, long writes) {
        ensureRevision(currentRevision);
        if (builtChunks.add(chunkKey)) {
            totalWrites += Math.max(0L, writes);
            setDirty();
        }
    }

    public void markCentre(String settlementId, int currentRevision) {
        ensureRevision(currentRevision);
        if (completedCentres.add(settlementId)) setDirty();
    }

    public boolean centreComplete(String settlementId, int currentRevision) {
        return revision == currentRevision && completedCentres.contains(settlementId);
    }

    public int builtChunkCount(int currentRevision) {
        return revision == currentRevision ? builtChunks.size() : 0;
    }

    public int completedCentreCount(int currentRevision) {
        return revision == currentRevision ? completedCentres.size() : 0;
    }

    public long totalWrites(int currentRevision) {
        return revision == currentRevision ? totalWrites : 0L;
    }

    private void ensureRevision(int currentRevision) {
        if (revision == currentRevision) return;
        revision = currentRevision;
        builtChunks.clear();
        completedCentres.clear();
        totalWrites = 0L;
        setDirty();
    }
}

package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Remembers streamed exterior construction cells and completed supply-site anchors. */
public final class ErdenKingdomExteriorSavedData extends SavedData {
    private static final Codec<ErdenKingdomExteriorSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("built_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.builtChunks)),
            Codec.STRING.listOf().optionalFieldOf("completed_nodes", List.of())
                    .forGetter(data -> List.copyOf(data.completedNodes)),
            Codec.LONG.optionalFieldOf("total_writes", 0L).forGetter(data -> data.totalWrites)
    ).apply(instance, ErdenKingdomExteriorSavedData::new));

    public static final SavedDataType<ErdenKingdomExteriorSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_kingdom_exterior"),
            level -> new ErdenKingdomExteriorSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> builtChunks;
    private final Set<String> completedNodes;
    private long totalWrites;

    public ErdenKingdomExteriorSavedData() {
        this(0, List.of(), List.of(), 0L);
    }

    private ErdenKingdomExteriorSavedData(
            int revision,
            List<Long> builtChunks,
            List<String> completedNodes,
            long totalWrites) {
        this.revision = Math.max(0, revision);
        this.builtChunks = new HashSet<>(builtChunks);
        this.completedNodes = new HashSet<>(completedNodes);
        this.totalWrites = Math.max(0L, totalWrites);
    }

    public boolean needs(long chunkPos, int currentRevision) {
        return revision != currentRevision || !builtChunks.contains(chunkPos);
    }

    public boolean isBuilt(long chunkPos, int currentRevision) {
        return revision == currentRevision && builtChunks.contains(chunkPos);
    }

    public void markChunk(long chunkPos, int currentRevision, long writes) {
        ensureRevision(currentRevision);
        if (builtChunks.add(chunkPos)) {
            totalWrites += Math.max(0L, writes);
            setDirty();
        }
    }

    public void markNode(String nodeId, int currentRevision) {
        ensureRevision(currentRevision);
        if (completedNodes.add(nodeId)) setDirty();
    }

    public boolean nodeComplete(String nodeId, int currentRevision) {
        return revision == currentRevision && completedNodes.contains(nodeId);
    }

    public int builtChunkCount(int currentRevision) {
        return revision == currentRevision ? builtChunks.size() : 0;
    }

    public int completedNodeCount(int currentRevision) {
        return revision == currentRevision ? completedNodes.size() : 0;
    }

    public long totalWrites(int currentRevision) {
        return revision == currentRevision ? totalWrites : 0L;
    }

    private void ensureRevision(int currentRevision) {
        if (revision == currentRevision) return;
        revision = currentRevision;
        builtChunks.clear();
        completedNodes.clear();
        totalWrites = 0L;
        setDirty();
    }
}

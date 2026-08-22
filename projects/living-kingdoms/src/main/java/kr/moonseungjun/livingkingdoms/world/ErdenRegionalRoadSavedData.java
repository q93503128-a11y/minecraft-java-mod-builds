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

/** Persistent revision ledger for streamed national-road chunks. */
public final class ErdenRegionalRoadSavedData extends SavedData {
    private static final Codec<ErdenRegionalRoadSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("road_revision", 0).forGetter(data -> data.roadRevision),
            Codec.LONG.listOf().optionalFieldOf("built_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.builtChunks)),
            Codec.LONG.optionalFieldOf("total_writes", 0L).forGetter(data -> data.totalWrites)
    ).apply(instance, ErdenRegionalRoadSavedData::new));

    public static final SavedDataType<ErdenRegionalRoadSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_regional_roads"),
            level -> new ErdenRegionalRoadSavedData(),
            level -> CODEC
    );

    private int roadRevision;
    private final Set<Long> builtChunks;
    private long totalWrites;

    public ErdenRegionalRoadSavedData() {
        this(0, List.of(), 0L);
    }

    private ErdenRegionalRoadSavedData(int roadRevision, List<Long> builtChunks, long totalWrites) {
        this.roadRevision = Math.max(0, roadRevision);
        this.builtChunks = new HashSet<>(builtChunks);
        this.totalWrites = Math.max(0L, totalWrites);
    }

    public boolean needs(long chunkKey, int revision) {
        return roadRevision != revision || !builtChunks.contains(chunkKey);
    }

    public boolean isBuilt(long chunkKey, int revision) {
        return roadRevision == revision && builtChunks.contains(chunkKey);
    }

    public void markBuilt(long chunkKey, int revision, long writes) {
        if (roadRevision != revision) {
            roadRevision = revision;
            builtChunks.clear();
            totalWrites = 0L;
        }
        if (builtChunks.add(chunkKey)) {
            totalWrites += Math.max(0L, writes);
            setDirty();
        }
    }

    public int builtChunkCount(int revision) {
        return roadRevision == revision ? builtChunks.size() : 0;
    }

    public long totalWrites(int revision) {
        return roadRevision == revision ? totalWrites : 0L;
    }
}

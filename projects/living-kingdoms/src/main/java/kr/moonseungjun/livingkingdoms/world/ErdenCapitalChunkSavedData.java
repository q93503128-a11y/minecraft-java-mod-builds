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

/** Remembers which 16 x 16 metre capital cells already received the current authored layout. */
public final class ErdenCapitalChunkSavedData extends SavedData {
    private static final Codec<ErdenCapitalChunkSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.LONG.listOf().optionalFieldOf("built_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.builtChunks))
    ).apply(instance, ErdenCapitalChunkSavedData::new));

    public static final SavedDataType<ErdenCapitalChunkSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_capital_chunks"),
            level -> new ErdenCapitalChunkSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<Long> builtChunks;

    public ErdenCapitalChunkSavedData() {
        this(0, List.of());
    }

    private ErdenCapitalChunkSavedData(int revision, List<Long> builtChunks) {
        this.revision = Math.max(0, revision);
        this.builtChunks = new HashSet<>(builtChunks);
    }

    public boolean needs(long chunkPos, int currentRevision) {
        return !isBuilt(chunkPos, currentRevision);
    }

    public boolean isBuilt(long chunkPos, int currentRevision) {
        return revision == currentRevision && builtChunks.contains(chunkPos);
    }

    public void mark(long chunkPos, int currentRevision) {
        if (revision != currentRevision) {
            revision = currentRevision;
            builtChunks.clear();
        }
        if (builtChunks.add(chunkPos)) setDirty();
    }

    public int builtCount(int currentRevision) {
        return revision == currentRevision ? builtChunks.size() : 0;
    }
}

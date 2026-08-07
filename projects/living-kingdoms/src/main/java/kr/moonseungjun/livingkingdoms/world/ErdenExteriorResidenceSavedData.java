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

/** Separate construction ledger so old exterior saves receive homes without rebuilding production sites. */
public final class ErdenExteriorResidenceSavedData extends SavedData {
    private static final Codec<ErdenExteriorResidenceSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("residence_revision", 0)
                    .forGetter(data -> data.residenceRevision),
            Codec.LONG.listOf().optionalFieldOf("built_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.builtChunks)),
            Codec.STRING.listOf().optionalFieldOf("built_households", List.of())
                    .forGetter(data -> List.copyOf(data.builtHouseholds)),
            Codec.LONG.optionalFieldOf("total_writes", 0L)
                    .forGetter(data -> data.totalWrites)
    ).apply(instance, ErdenExteriorResidenceSavedData::new));

    public static final SavedDataType<ErdenExteriorResidenceSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_exterior_residences"),
            level -> new ErdenExteriorResidenceSavedData(),
            level -> CODEC
    );

    private int residenceRevision;
    private final Set<Long> builtChunks;
    private final Set<String> builtHouseholds;
    private long totalWrites;

    public ErdenExteriorResidenceSavedData() {
        this(0, List.of(), List.of(), 0L);
    }

    private ErdenExteriorResidenceSavedData(
            int residenceRevision,
            List<Long> builtChunks,
            List<String> builtHouseholds,
            long totalWrites) {
        this.residenceRevision = Math.max(0, residenceRevision);
        this.builtChunks = new HashSet<>(builtChunks);
        this.builtHouseholds = new HashSet<>(builtHouseholds);
        this.totalWrites = Math.max(0L, totalWrites);
    }

    public boolean needsChunk(int chunkX, int chunkZ, int revision) {
        if (!ErdenExteriorResidenceCatalog.residenceChunk(chunkX, chunkZ)) return false;
        return residenceRevision != revision || !builtChunks.contains(pack(chunkX, chunkZ));
    }

    public boolean householdBuilt(String householdId, int revision) {
        return residenceRevision == revision && builtHouseholds.contains(householdId);
    }

    public void markChunk(
            int chunkX,
            int chunkZ,
            int revision,
            List<ErdenExteriorResidenceCatalog.ResidencePlot> plots,
            long writes) {
        if (residenceRevision != revision) {
            residenceRevision = revision;
            builtChunks.clear();
            builtHouseholds.clear();
            totalWrites = 0L;
        }
        boolean changed = builtChunks.add(pack(chunkX, chunkZ));
        for (ErdenExteriorResidenceCatalog.ResidencePlot plot : plots) {
            changed |= builtHouseholds.add(plot.householdId());
        }
        if (writes > 0L) {
            totalWrites += writes;
            changed = true;
        }
        if (changed) setDirty();
    }

    public int builtChunkCount(int revision) {
        return residenceRevision == revision ? builtChunks.size() : 0;
    }

    public int builtHouseholdCount(int revision) {
        return residenceRevision == revision ? builtHouseholds.size() : 0;
    }

    public long totalWrites(int revision) {
        return residenceRevision == revision ? totalWrites : 0L;
    }

    public List<String> missingHouseholds(int revision) {
        if (residenceRevision != revision) {
            List<String> all = new ArrayList<>();
            for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                    ErdenExteriorResidenceCatalog.plots()) all.add(plot.householdId());
            return all;
        }
        List<String> missing = new ArrayList<>();
        for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                ErdenExteriorResidenceCatalog.plots()) {
            if (!builtHouseholds.contains(plot.householdId())) missing.add(plot.householdId());
        }
        return missing;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}

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
 * Persists the two-stage authored upper-route migration for Erden's small urban buildings.
 *
 * <p>A prepared route has had only source-air staircase/support cells materialized. A completed
 * route has subsequently passed runtime traversal geometry checks after the authored-interior
 * restoration pass had a chance to put retained source floors and partitions back in place.</p>
 */
public final class ErdenUrbanAuthoredUpperRouteSavedData extends SavedData {
    private static final Codec<ErdenUrbanAuthoredUpperRouteSavedData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
                    Codec.LONG.listOf().optionalFieldOf("prepared_entrances", List.of())
                            .forGetter(data -> List.copyOf(data.preparedEntrances)),
                    Codec.LONG.listOf().optionalFieldOf("completed_entrances", List.of())
                            .forGetter(data -> List.copyOf(data.completedEntrances))
            ).apply(instance, ErdenUrbanAuthoredUpperRouteSavedData::new));

    public static final SavedDataType<ErdenUrbanAuthoredUpperRouteSavedData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            LivingKingdoms.MOD_ID, "erden_urban_authored_upper_routes"),
                    level -> new ErdenUrbanAuthoredUpperRouteSavedData(),
                    level -> CODEC
            );

    private int revision;
    private final Set<Long> preparedEntrances;
    private final Set<Long> completedEntrances;

    public ErdenUrbanAuthoredUpperRouteSavedData() {
        this(0, List.of(), List.of());
    }

    private ErdenUrbanAuthoredUpperRouteSavedData(
            int revision,
            List<Long> preparedEntrances,
            List<Long> completedEntrances) {
        this.revision = Math.max(0, revision);
        this.preparedEntrances = new HashSet<>(preparedEntrances);
        this.completedEntrances = new HashSet<>(completedEntrances);
    }

    public boolean isPrepared(long entranceKey, int currentRevision) {
        return revision == currentRevision
                && (preparedEntrances.contains(entranceKey)
                || completedEntrances.contains(entranceKey));
    }

    public boolean isCompleted(long entranceKey, int currentRevision) {
        return revision == currentRevision && completedEntrances.contains(entranceKey);
    }

    public void markPrepared(long entranceKey, int currentRevision) {
        ensureRevision(currentRevision);
        if (preparedEntrances.add(entranceKey)) setDirty();
    }

    public void markCompleted(long entranceKey, int currentRevision) {
        ensureRevision(currentRevision);
        boolean changed = preparedEntrances.add(entranceKey);
        changed |= completedEntrances.add(entranceKey);
        if (changed) setDirty();
    }

    public int preparedCount(int currentRevision) {
        if (revision != currentRevision) return 0;
        Set<Long> all = new HashSet<>(preparedEntrances);
        all.addAll(completedEntrances);
        return all.size();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completedEntrances.size() : 0;
    }

    private void ensureRevision(int currentRevision) {
        if (revision == currentRevision) return;
        revision = currentRevision;
        preparedEntrances.clear();
        completedEntrances.clear();
        setDirty();
    }
}

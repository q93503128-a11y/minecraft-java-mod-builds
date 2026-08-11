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

/** Persists the non-destructive functional zoning layered into the imported Erden citadel. */
public final class ErdenCitadelInteriorSavedData extends SavedData {
    private static final Codec<ErdenCitadelInteriorSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.STRING.listOf().optionalFieldOf("completed_zones", List.of())
                    .forGetter(data -> List.copyOf(data.completedZones))
    ).apply(instance, ErdenCitadelInteriorSavedData::new));

    public static final SavedDataType<ErdenCitadelInteriorSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_citadel_interiors"),
            level -> new ErdenCitadelInteriorSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<String> completedZones;

    public ErdenCitadelInteriorSavedData() {
        this(0, List.of());
    }

    private ErdenCitadelInteriorSavedData(int revision, List<String> completedZones) {
        this.revision = Math.max(0, revision);
        this.completedZones = new HashSet<>(completedZones);
    }

    public boolean isComplete(String zoneId, int currentRevision) {
        return revision == currentRevision && completedZones.contains(zoneId);
    }

    public void markComplete(String zoneId, int currentRevision) {
        if (revision != currentRevision) {
            revision = currentRevision;
            completedZones.clear();
        }
        if (completedZones.add(zoneId)) setDirty();
    }

    public int completedCount(int currentRevision) {
        return revision == currentRevision ? completedZones.size() : 0;
    }
}

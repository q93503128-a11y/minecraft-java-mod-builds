package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VillageMercenaryData extends SavedData {
    private static final Codec<VillageMercenaryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("classes", Map.of())
                    .forGetter(data -> data.classes),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("levels", Map.of())
                    .forGetter(data -> data.levels),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("kills", Map.of())
                    .forGetter(data -> data.kills)
    ).apply(instance, VillageMercenaryData::new));

    public static final SavedDataType<VillageMercenaryData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_mercenaries"),
            level -> new VillageMercenaryData(),
            level -> CODEC);

    private Map<String, String> classes;
    private Map<String, Integer> levels;
    private Map<String, Integer> kills;

    public VillageMercenaryData() { this(Map.of(), Map.of(), Map.of()); }

    private VillageMercenaryData(Map<String, String> classes,
                                 Map<String, Integer> levels,
                                 Map<String, Integer> kills) {
        this.classes = new LinkedHashMap<>(classes);
        this.levels = new LinkedHashMap<>(levels);
        this.kills = new LinkedHashMap<>(kills);
    }

    public Map<String, String> classes() { return new LinkedHashMap<>(classes); }
    public Map<String, Integer> levels() { return new LinkedHashMap<>(levels); }
    public Map<String, Integer> kills() { return new LinkedHashMap<>(kills); }

    public void replace(Map<String, String> updatedClasses,
                        Map<String, Integer> updatedLevels,
                        Map<String, Integer> updatedKills) {
        classes = new LinkedHashMap<>(updatedClasses);
        levels = new LinkedHashMap<>(updatedLevels);
        kills = new LinkedHashMap<>(updatedKills);
        setDirty();
    }
}

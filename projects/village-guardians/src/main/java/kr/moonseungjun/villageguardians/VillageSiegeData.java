package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent state for siege wall segments, deployable turrets and deployment doctrine. */
public final class VillageSiegeData extends SavedData {
    private static final Codec<VillageSiegeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("integers", Map.of()).forGetter(data -> data.integers),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("strings", Map.of()).forGetter(data -> data.strings)
    ).apply(instance, VillageSiegeData::new));

    public static final SavedDataType<VillageSiegeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_siege_phase2"),
            level -> new VillageSiegeData(), level -> CODEC);

    private Map<String, Integer> integers;
    private Map<String, String> strings;

    public VillageSiegeData() { this(Map.of(), Map.of()); }

    private VillageSiegeData(Map<String, Integer> integers, Map<String, String> strings) {
        this.integers = new LinkedHashMap<>(integers);
        this.strings = new LinkedHashMap<>(strings);
    }

    public Map<String, Integer> integers() { return new LinkedHashMap<>(integers); }
    public Map<String, String> strings() { return new LinkedHashMap<>(strings); }

    public void replace(Map<String, Integer> newIntegers, Map<String, String> newStrings) {
        integers = new LinkedHashMap<>(newIntegers);
        strings = new LinkedHashMap<>(newStrings);
        setDirty();
    }
}

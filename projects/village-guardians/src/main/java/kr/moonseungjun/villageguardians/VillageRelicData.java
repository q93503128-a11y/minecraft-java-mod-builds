package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VillageRelicData extends SavedData {
    private static final Codec<VillageRelicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("owned", Map.of())
                    .forGetter(data -> data.owned),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("pending", Map.of())
                    .forGetter(data -> data.pending)
    ).apply(instance, VillageRelicData::new));

    public static final SavedDataType<VillageRelicData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_relics"),
            level -> new VillageRelicData(),
            level -> CODEC);

    private Map<String, Integer> owned;
    private Map<String, String> pending;

    public VillageRelicData() { this(Map.of(), Map.of()); }

    private VillageRelicData(Map<String, Integer> owned, Map<String, String> pending) {
        this.owned = new LinkedHashMap<>(owned);
        this.pending = new LinkedHashMap<>(pending);
    }

    public Map<String, Integer> owned() { return new LinkedHashMap<>(owned); }
    public Map<String, String> pending() { return new LinkedHashMap<>(pending); }

    public void replace(Map<String, Integer> updatedOwned, Map<String, String> updatedPending) {
        owned = new LinkedHashMap<>(updatedOwned);
        pending = new LinkedHashMap<>(updatedPending);
        setDirty();
    }
}

package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VillageDefenseResearchData extends SavedData {
    private static final Codec<VillageDefenseResearchData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("levels", Map.of())
                    .forGetter(data -> data.levels)
    ).apply(instance, VillageDefenseResearchData::new));

    public static final SavedDataType<VillageDefenseResearchData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_defense_research"),
            level -> new VillageDefenseResearchData(),
            level -> CODEC);

    private Map<String, Integer> levels;

    public VillageDefenseResearchData() { this(Map.of()); }

    private VillageDefenseResearchData(Map<String, Integer> levels) {
        this.levels = sanitize(levels);
    }

    public Map<String, Integer> levels() { return new LinkedHashMap<>(levels); }

    public void replace(Map<String, Integer> updated) {
        levels = sanitize(updated);
        setDirty();
    }

    private static Map<String, Integer> sanitize(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            VillageDefenseResearchSystem.Branch branch = VillageDefenseResearchSystem.Branch.fromId(key);
            if (branch != null && value != null) result.put(branch.id(), Math.max(0, Math.min(VillageDefenseResearchSystem.MAX_LEVEL, value)));
        });
        return result;
    }
}

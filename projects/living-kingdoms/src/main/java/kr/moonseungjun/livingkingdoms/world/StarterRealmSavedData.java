package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StarterRealmSavedData extends SavedData {
    private static final Codec<StarterRealmSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf()
                    .optionalFieldOf("generated_regions", List.of())
                    .forGetter(data -> List.copyOf(data.generatedRegions))
    ).apply(instance, StarterRealmSavedData::new));

    public static final SavedDataType<StarterRealmSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "starter_realm"),
            level -> new StarterRealmSavedData(),
            level -> CODEC
    );

    private final Set<String> generatedRegions;

    public StarterRealmSavedData() {
        this(List.of());
    }

    private StarterRealmSavedData(List<String> generatedRegions) {
        this.generatedRegions = new LinkedHashSet<>(generatedRegions);
    }

    public boolean isGenerated(String regionId) {
        return generatedRegions.contains(regionId);
    }

    public void markGenerated(String regionId) {
        if (generatedRegions.add(regionId)) {
            setDirty();
        }
    }

    public int generatedRegionCount() {
        return generatedRegions.size();
    }
}

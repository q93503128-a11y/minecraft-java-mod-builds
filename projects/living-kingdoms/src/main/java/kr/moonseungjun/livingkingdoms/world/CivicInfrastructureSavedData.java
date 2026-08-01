package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

/** Prevents expensive infrastructure finishing passes from repeating after each server restart. */
public final class CivicInfrastructureSavedData extends SavedData {
    private static final Codec<CivicInfrastructureSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("revisions", Map.of())
                    .forGetter(data -> Map.copyOf(data.revisions))
    ).apply(instance, CivicInfrastructureSavedData::new));

    public static final SavedDataType<CivicInfrastructureSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "civic_infrastructure"),
            level -> new CivicInfrastructureSavedData(),
            level -> CODEC
    );

    private final Map<String, Integer> revisions;

    public CivicInfrastructureSavedData() {
        this(Map.of());
    }

    private CivicInfrastructureSavedData(Map<String, Integer> revisions) {
        this.revisions = new LinkedHashMap<>(revisions);
    }

    public boolean needs(String homelandId, int revision) {
        return revisions.getOrDefault(homelandId, 0) < revision;
    }

    public void mark(String homelandId, int revision) {
        revisions.put(homelandId, Math.max(0, revision));
        setDirty();
    }
}

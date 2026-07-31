package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

/** Stores the last authored-world repair revision applied to each homeland. */
public final class StarterRealmUpgradeSavedData extends SavedData {
    private static final Codec<StarterRealmUpgradeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("region_revisions", Map.of())
                    .forGetter(data -> Map.copyOf(data.regionRevisions))
    ).apply(instance, StarterRealmUpgradeSavedData::new));

    public static final SavedDataType<StarterRealmUpgradeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "starter_realm_upgrades"),
            level -> new StarterRealmUpgradeSavedData(),
            level -> CODEC
    );

    private final Map<String, Integer> regionRevisions;

    public StarterRealmUpgradeSavedData() {
        this(Map.of());
    }

    private StarterRealmUpgradeSavedData(Map<String, Integer> regionRevisions) {
        this.regionRevisions = new LinkedHashMap<>(regionRevisions);
    }

    public int revision(String regionId) {
        return regionRevisions.getOrDefault(regionId, 0);
    }

    public void setRevision(String regionId, int revision) {
        if (regionRevisions.getOrDefault(regionId, 0) != revision) {
            regionRevisions.put(regionId, revision);
            setDirty();
        }
    }
}

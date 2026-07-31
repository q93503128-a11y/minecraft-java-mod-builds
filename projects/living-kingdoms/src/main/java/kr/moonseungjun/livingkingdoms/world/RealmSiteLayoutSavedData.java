package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Stores the terrain-surveyed anchor and build revision of each homeland. */
public final class RealmSiteLayoutSavedData extends SavedData {
    private static final Codec<RealmSite> SITE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("center_x").forGetter(RealmSite::centerX),
            Codec.INT.fieldOf("center_z").forGetter(RealmSite::centerZ),
            Codec.INT.fieldOf("base_y").forGetter(RealmSite::baseY),
            Codec.INT.optionalFieldOf("revision", 0).forGetter(RealmSite::revision),
            Codec.BOOL.optionalFieldOf("built", false).forGetter(RealmSite::built)
    ).apply(instance, RealmSite::new));

    private static final Codec<RealmSiteLayoutSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, SITE_CODEC)
                    .optionalFieldOf("homelands", Map.of())
                    .forGetter(data -> Map.copyOf(data.sites))
    ).apply(instance, RealmSiteLayoutSavedData::new));

    public static final SavedDataType<RealmSiteLayoutSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "realm_site_layout"),
            level -> new RealmSiteLayoutSavedData(),
            level -> CODEC
    );

    private final Map<String, RealmSite> sites;

    public RealmSiteLayoutSavedData() {
        this(Map.of());
    }

    private RealmSiteLayoutSavedData(Map<String, RealmSite> sites) {
        this.sites = new LinkedHashMap<>(sites);
    }

    public Optional<RealmSite> site(String homelandId) {
        return Optional.ofNullable(sites.get(homelandId));
    }

    public void put(String homelandId, RealmSite site) {
        sites.put(homelandId, site);
        setDirty();
    }

    public void markBuilt(String homelandId, int revision) {
        RealmSite current = sites.get(homelandId);
        if (current == null) return;
        sites.put(homelandId, new RealmSite(current.centerX(), current.centerZ(), current.baseY(), revision, true));
        setDirty();
    }

    public record RealmSite(int centerX, int centerZ, int baseY, int revision, boolean built) {
    }
}

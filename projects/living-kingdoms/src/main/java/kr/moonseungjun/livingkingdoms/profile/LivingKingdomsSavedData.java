package kr.moonseungjun.livingkingdoms.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class LivingKingdomsSavedData extends SavedData {
    private static final Codec<LivingKingdomsSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, OriginProfile.CODEC)
                    .optionalFieldOf("origin_profiles", Map.of())
                    .forGetter(data -> data.originProfiles)
    ).apply(instance, LivingKingdomsSavedData::new));

    public static final SavedDataType<LivingKingdomsSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "player_profiles"),
            level -> new LivingKingdomsSavedData(),
            level -> CODEC
    );

    private Map<String, OriginProfile> originProfiles;

    public LivingKingdomsSavedData() {
        this(Map.of());
    }

    private LivingKingdomsSavedData(Map<String, OriginProfile> originProfiles) {
        this.originProfiles = new LinkedHashMap<>(originProfiles);
    }

    public Optional<OriginProfile> profile(UUID playerId) {
        return Optional.ofNullable(originProfiles.get(playerId.toString()));
    }

    public void putProfile(UUID playerId, OriginProfile profile) {
        originProfiles.put(playerId.toString(), profile);
        setDirty();
    }

    public int profileCount() {
        return originProfiles.size();
    }
}

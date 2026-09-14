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
                    .forGetter(data -> data.originProfiles),
            Codec.unboundedMap(Codec.STRING, ResidenceAssignment.CODEC)
                    .optionalFieldOf("residence_assignments", Map.of())
                    .forGetter(data -> data.residenceAssignments)
    ).apply(instance, LivingKingdomsSavedData::new));

    public static final SavedDataType<LivingKingdomsSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "player_profiles"),
            level -> new LivingKingdomsSavedData(),
            level -> CODEC
    );

    private final Map<String, OriginProfile> originProfiles;
    private final Map<String, ResidenceAssignment> residenceAssignments;

    public LivingKingdomsSavedData() {
        this(Map.of(), Map.of());
    }

    private LivingKingdomsSavedData(Map<String, OriginProfile> originProfiles,
                                    Map<String, ResidenceAssignment> residenceAssignments) {
        this.originProfiles = new LinkedHashMap<>(originProfiles);
        this.residenceAssignments = new LinkedHashMap<>(residenceAssignments);
    }

    public Optional<OriginProfile> profile(UUID playerId) {
        return Optional.ofNullable(originProfiles.get(playerId.toString()));
    }

    public void putProfile(UUID playerId, OriginProfile profile) {
        originProfiles.put(playerId.toString(), profile);
        setDirty();
    }

    public Optional<ResidenceAssignment> residenceAssignment(UUID playerId) {
        return Optional.ofNullable(residenceAssignments.get(playerId.toString()));
    }

    public void putResidenceAssignment(UUID playerId, ResidenceAssignment assignment) {
        residenceAssignments.put(playerId.toString(), assignment);
        setDirty();
    }

    public int profileCount() {
        return originProfiles.size();
    }

    public int residenceAssignmentCount() {
        return residenceAssignments.size();
    }
}

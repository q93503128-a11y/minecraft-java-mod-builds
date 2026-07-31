package kr.moonseungjun.livingkingdoms.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;

import java.util.Objects;

public record OriginProfile(
        String speciesId,
        String homelandId,
        String backgroundId,
        String residenceId,
        long createdAtGameTime
) {
    public static final Codec<OriginProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("species").forGetter(OriginProfile::speciesId),
            Codec.STRING.fieldOf("homeland").forGetter(OriginProfile::homelandId),
            Codec.STRING.fieldOf("background").forGetter(OriginProfile::backgroundId),
            Codec.STRING.fieldOf("residence").forGetter(OriginProfile::residenceId),
            Codec.LONG.optionalFieldOf("created_at_game_time", 0L).forGetter(OriginProfile::createdAtGameTime)
    ).apply(instance, OriginProfile::new));

    public OriginProfile {
        speciesId = requireId(speciesId, "speciesId");
        homelandId = requireId(homelandId, "homelandId");
        backgroundId = requireId(backgroundId, "backgroundId");
        residenceId = requireId(residenceId, "residenceId");
        createdAtGameTime = Math.max(0L, createdAtGameTime);
    }

    public FoundationCatalog.OriginSelection selection() {
        return new FoundationCatalog.OriginSelection(speciesId, homelandId, backgroundId, residenceId);
    }

    private static String requireId(String value, String field) {
        String id = Objects.requireNonNull(value, field).trim();
        if (!id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid " + field + ": " + id);
        }
        return id;
    }
}

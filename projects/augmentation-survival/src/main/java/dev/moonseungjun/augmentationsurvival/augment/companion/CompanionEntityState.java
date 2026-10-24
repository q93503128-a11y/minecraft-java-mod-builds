package dev.moonseungjun.augmentationsurvival.augment.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;

/** Persistent ownership metadata for permanent dominated companions. */
public record CompanionEntityState(String ownerUuid) {
    public static final Codec<CompanionEntityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("owner_uuid", "").forGetter(CompanionEntityState::ownerUuid)
    ).apply(instance, CompanionEntityState::new));

    public static CompanionEntityState initial() {
        return new CompanionEntityState("");
    }

    public static CompanionEntityState dominatedBy(UUID owner) {
        return new CompanionEntityState(owner.toString());
    }

    public Optional<UUID> ownerId() {
        if (ownerUuid == null || ownerUuid.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(ownerUuid));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}

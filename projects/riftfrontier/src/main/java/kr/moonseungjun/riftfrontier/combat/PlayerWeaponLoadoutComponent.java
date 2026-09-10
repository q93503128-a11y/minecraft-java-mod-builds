package kr.moonseungjun.riftfrontier.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable ItemStack data-component value that identifies a Riftfrontier weapon loadout.
 *
 * <p>The component stores stable content ids only. It contains no damage, reach, timing or hit
 * authority; those remain server-owned content/runtime policy.</p>
 */
public record PlayerWeaponLoadoutComponent(String familyId, Optional<String> moduleId) {
    public static final Codec<PlayerWeaponLoadoutComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("family").forGetter(PlayerWeaponLoadoutComponent::familyId),
        Codec.STRING.optionalFieldOf("module").forGetter(PlayerWeaponLoadoutComponent::moduleId)
    ).apply(instance, PlayerWeaponLoadoutComponent::new));

    public PlayerWeaponLoadoutComponent {
        familyId = Objects.requireNonNull(familyId, "familyId");
        moduleId = Objects.requireNonNull(moduleId, "moduleId");
        if (familyId.isBlank()) throw new IllegalArgumentException("familyId must not be blank");
        if (moduleId.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("moduleId must not be blank when present");
        }
    }
}

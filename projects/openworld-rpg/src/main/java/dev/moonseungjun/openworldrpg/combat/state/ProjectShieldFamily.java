package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import java.util.Locale;

/**
 * Persistent shield-family identity.
 *
 * <p>Weapon guard is deliberately not represented here because the design canon does not yet give
 * weapon families a canonical GuardRating. Only the three authored shield families may publish an
 * equipment-derived guard snapshot.</p>
 */
public enum ProjectShieldFamily {
    BUCKLER,
    STANDARD,
    HEAVY;

    public static final Codec<ProjectShieldFamily> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            ProjectShieldFamily.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown Openworld RPG shield family: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );

    public PlayerDefenseAuthority.GuardType guardType() {
        return switch (this) {
            case BUCKLER -> PlayerDefenseAuthority.GuardType.BUCKLER;
            case STANDARD -> PlayerDefenseAuthority.GuardType.STANDARD_SHIELD;
            case HEAVY -> PlayerDefenseAuthority.GuardType.HEAVY_SHIELD;
        };
    }
}

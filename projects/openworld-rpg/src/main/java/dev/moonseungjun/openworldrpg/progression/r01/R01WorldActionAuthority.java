package dev.moonseungjun.openworldrpg.progression.r01;

import java.util.Objects;
import java.util.Set;

/** Pure semantic validation for R01 world actions before personal quest credit is committed. */
public final class R01WorldActionAuthority {
    public static final String IRON_ORE = "openworld_rpg:iron_ore";
    public static final String HARDWOOD = "openworld_rpg:hardwood";
    public static final String HEALING_HERB = "openworld_rpg:healing_herb";

    private static final Set<String> VALID_DUST_GATHER_RESOURCES =
            Set.of(IRON_ORE, HARDWOOD, HEALING_HERB);

    private R01WorldActionAuthority() {
    }

    public static R01PlayerState.QuarryRoadAction personalInteraction(
            PersonalInteraction interaction
    ) {
        Objects.requireNonNull(interaction, "interaction");
        return switch (interaction) {
            case LOST_CARGO -> R01PlayerState.QuarryRoadAction.LOST_CARGO;
            case BROKEN_ROAD_MARKER -> R01PlayerState.QuarryRoadAction.BROKEN_ROAD_MARKER;
        };
    }

    public static boolean isValidDustGatherResource(String resourceId) {
        return resourceId != null && VALID_DUST_GATHER_RESOURCES.contains(resourceId);
    }

    public static R01PlayerState.QuarryRoadAction validGather(String resourceId) {
        if (!isValidDustGatherResource(resourceId)) {
            throw new IllegalArgumentException(
                    "Dust gather credit requires Iron Ore, Hardwood or Healing Herb."
            );
        }
        return R01PlayerState.QuarryRoadAction.R01_GATHERING_NODE;
    }

    public static R01PlayerState.QuarryRoadAction meadowViperContribution(
            CombatContribution contribution
    ) {
        Objects.requireNonNull(contribution, "contribution");
        if (contribution == CombatContribution.PROXIMITY_ONLY) {
            throw new IllegalArgumentException(
                    "Zero-action proximity cannot qualify as Meadow Viper participation."
            );
        }
        return R01PlayerState.QuarryRoadAction.MEADOW_VIPER;
    }

    public enum PersonalInteraction {
        LOST_CARGO,
        BROKEN_ROAD_MARKER
    }

    public enum CombatContribution {
        DAMAGE,
        VALID_SUPPORT,
        PROXIMITY_ONLY
    }
}

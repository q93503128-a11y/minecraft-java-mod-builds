package dev.moonseungjun.openworldrpg.fishing;

import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingData;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Exact 5+1+1 R01 fishing-spot binding with production-only live access. */
public final class R01FishingSpatialRegistry {
    public static final List<String> ORDINARY_SPOTS = List.of(
            "openworld_rpg:r01/fishing/ordinary_greenwater_ford",
            "openworld_rpg:r01/fishing/ordinary_greenwater_lower",
            "openworld_rpg:r01/fishing/ordinary_twin_willow_bend",
            "openworld_rpg:r01/fishing/ordinary_mosswheel_branch",
            "openworld_rpg:r01/fishing/ordinary_riverwood_bank"
    );
    public static final String UNCOMMON_SPOT =
            "openworld_rpg:r01/fishing/uncommon_riverwood_pool";
    public static final String RARE_SPOT =
            "openworld_rpg:r01/fishing/rare_deepwater_spot";

    private static final Set<String> ALL_IDS = Set.of(
            ORDINARY_SPOTS.get(0),
            ORDINARY_SPOTS.get(1),
            ORDINARY_SPOTS.get(2),
            ORDINARY_SPOTS.get(3),
            ORDINARY_SPOTS.get(4),
            UNCOMMON_SPOT,
            RARE_SPOT
    );

    private R01FishingSpatialRegistry() {
    }

    public static List<BoundSpot> allAuthoredSpots() {
        List<BoundSpot> result = new ArrayList<>();
        for (String id : ALL_IDS) {
            R01SpatialBindingData.Anchor anchor =
                    R01SpatialBindingRegistry.data().anchor(id)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Missing authored R01 fishing anchor: " + id
                            ));
            result.add(new BoundSpot(id, tier(id), anchor));
        }
        result.sort(Comparator.comparing(BoundSpot::spotId));
        return List.copyOf(result);
    }

    public static Optional<BoundSpot> spot(String spotId) {
        Objects.requireNonNull(spotId, "spotId");
        if (!ALL_IDS.contains(spotId)) {
            return Optional.empty();
        }
        return R01SpatialBindingRegistry.data()
                .anchor(spotId)
                .map(anchor -> new BoundSpot(
                        spotId,
                        tier(spotId),
                        anchor
                ));
    }

    public static Optional<BoundSpot> productionSpot(String spotId) {
        Objects.requireNonNull(spotId, "spotId");
        if (!ALL_IDS.contains(spotId)) {
            return Optional.empty();
        }
        return R01SpatialBindingRegistry.productionAnchor(spotId)
                .map(anchor -> new BoundSpot(
                        spotId,
                        tier(spotId),
                        anchor
                ));
    }

    public static boolean productionReady() {
        for (String id : ALL_IDS) {
            if (productionSpot(id).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static R01FishingRules.SpotTier tier(String spotId) {
        Objects.requireNonNull(spotId, "spotId");
        if (ORDINARY_SPOTS.contains(spotId)) {
            return R01FishingRules.SpotTier.ORDINARY;
        }
        if (UNCOMMON_SPOT.equals(spotId)) {
            return R01FishingRules.SpotTier.UNCOMMON;
        }
        if (RARE_SPOT.equals(spotId)) {
            return R01FishingRules.SpotTier.RARE;
        }
        throw new IllegalArgumentException(
                "Unknown R01 fishing spot: " + spotId
        );
    }

    public record BoundSpot(
            String spotId,
            R01FishingRules.SpotTier tier,
            R01SpatialBindingData.Anchor anchor
    ) {
        public BoundSpot {
            Objects.requireNonNull(spotId, "spotId");
            Objects.requireNonNull(tier, "tier");
            Objects.requireNonNull(anchor, "anchor");
            if (!spotId.equals(anchor.id())) {
                throw new IllegalArgumentException(
                        "Fishing spot id must match its spatial anchor."
                );
            }
        }

        public boolean production() {
            return anchor.production();
        }
    }
}

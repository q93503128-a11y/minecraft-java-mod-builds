package dev.moonseungjun.openworldrpg.gathering;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Canon-locked non-spatial R01 gathering definitions. */
public final class R01GatheringRules {
    public static final String IRON_ORE = "openworld_rpg:iron_ore";
    public static final String HARDWOOD = "openworld_rpg:hardwood";
    public static final String HEALING_HERB = "openworld_rpg:healing_herb";
    public static final String VERDANT_CRYSTAL = "openworld_rpg:verdant_crystal";

    public static final String DELIVERY_RECEIPT_PREFIX =
            "openworld_rpg:gather_delivery/";

    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINUTE = 60 * TICKS_PER_SECOND;

    private static final Map<String, ResourceDefinition> R01_RESOURCES = Map.of(
            IRON_ORE,
            new ResourceDefinition(
                    IRON_ORE,
                    GatheringDiscipline.MINING,
                    ToolFamily.MINING_PICK,
                    ToolTier.FIELD,
                    2,
                    4,
                    7L * TICKS_PER_MINUTE,
                    31,
                    false,
                    true
            ),
            HARDWOOD,
            new ResourceDefinition(
                    HARDWOOD,
                    GatheringDiscipline.FORESTRY,
                    ToolFamily.WOODCUTTER_AXE,
                    ToolTier.FIELD,
                    2,
                    3,
                    5L * TICKS_PER_MINUTE,
                    27,
                    false,
                    true
            ),
            HEALING_HERB,
            new ResourceDefinition(
                    HEALING_HERB,
                    GatheringDiscipline.HERBALISM,
                    ToolFamily.HARVEST_KNIFE,
                    ToolTier.FIELD,
                    1,
                    2,
                    4L * TICKS_PER_MINUTE,
                    13,
                    false,
                    true
            ),
            VERDANT_CRYSTAL,
            new ResourceDefinition(
                    VERDANT_CRYSTAL,
                    GatheringDiscipline.MINING,
                    ToolFamily.MINING_PICK,
                    ToolTier.FIELD,
                    1,
                    1,
                    18L * TICKS_PER_MINUTE,
                    36,
                    true,
                    false
            )
    );

    private R01GatheringRules() {
    }

    public static Optional<ResourceDefinition> resource(String resourceId) {
        return Optional.ofNullable(R01_RESOURCES.get(resourceId));
    }

    public static ResourceDefinition requireResource(String resourceId) {
        return resource(resourceId).orElseThrow(
                () -> new IllegalArgumentException("Unknown R01 gathering resource: " + resourceId)
        );
    }

    public static int masteryRankForXp(int xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("Gathering mastery XP must be non-negative.");
        }
        if (xp >= 240) return 5;
        if (xp >= 130) return 4;
        if (xp >= 60) return 3;
        if (xp >= 20) return 2;
        return 1;
    }

    public static int ordinaryBonusChancePercent(int masteryRank) {
        if (masteryRank < 1 || masteryRank > 5) {
            throw new IllegalArgumentException("Gathering mastery rank must be inside 1..5.");
        }
        if (masteryRank >= 5) return 10;
        if (masteryRank >= 3) return 5;
        return 0;
    }

    public static String discoveryFlag(String resourceId) {
        ResourceDefinition definition = requireResource(resourceId);
        return "openworld_rpg:r01/discovery/"
                + definition.resourceId().substring(definition.resourceId().indexOf(':') + 1);
    }

    public static String deliveryTransactionId(String nodeId, long generation) {
        if (generation <= 0L) {
            throw new IllegalArgumentException("Gather generation must be positive.");
        }
        requireStableId(nodeId);
        return DELIVERY_RECEIPT_PREFIX
                + nodeId.replace(':', '/')
                + "/"
                + generation;
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable namespaced id.");
        }
    }

    public enum GatheringDiscipline {
        MINING("openworld_rpg:mining"),
        FORESTRY("openworld_rpg:forestry"),
        HERBALISM("openworld_rpg:herbalism"),
        FISHING("openworld_rpg:fishing");

        private final String id;

        GatheringDiscipline(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum ToolFamily {
        MINING_PICK("openworld_rpg:mining_pick"),
        WOODCUTTER_AXE("openworld_rpg:woodcutter_axe"),
        HARVEST_KNIFE("openworld_rpg:harvest_knife"),
        FISHING_ROD("openworld_rpg:fishing_rod");

        private final String id;

        ToolFamily(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum ToolTier {
        FIELD(0, 100),
        REFINED(1, 85),
        MASTERWORK(2, 75);

        private final int level;
        private final int actionTimePercent;

        ToolTier(int level, int actionTimePercent) {
            this.level = level;
            this.actionTimePercent = actionTimePercent;
        }

        public int level() {
            return level;
        }

        public int actionTimePercent() {
            return actionTimePercent;
        }

        public static ToolTier fromLevel(int level) {
            for (ToolTier tier : values()) {
                if (tier.level == level) {
                    return tier;
                }
            }
            throw new IllegalArgumentException("Unknown gathering tool tier level: " + level);
        }
    }

    public record ResourceDefinition(
            String resourceId,
            GatheringDiscipline discipline,
            ToolFamily toolFamily,
            ToolTier requiredToolTier,
            int minBaseYield,
            int maxBaseYield,
            long respawnActiveTicks,
            int baseInteractionTicks,
            boolean rareOrDense,
            boolean dustQuestCredit
    ) {
        public ResourceDefinition {
            requireStableId(resourceId);
            Objects.requireNonNull(discipline, "discipline");
            Objects.requireNonNull(toolFamily, "toolFamily");
            Objects.requireNonNull(requiredToolTier, "requiredToolTier");
            if (minBaseYield <= 0 || maxBaseYield < minBaseYield) {
                throw new IllegalArgumentException("Invalid R01 gathering yield range.");
            }
            if (respawnActiveTicks <= 0L || baseInteractionTicks <= 0) {
                throw new IllegalArgumentException(
                        "Gather respawn and interaction time must be positive."
                );
            }
        }

        public int baseMasteryXp() {
            return rareOrDense ? 3 : 1;
        }

        public int yieldRangeSize() {
            return maxBaseYield - minBaseYield + 1;
        }
    }
}

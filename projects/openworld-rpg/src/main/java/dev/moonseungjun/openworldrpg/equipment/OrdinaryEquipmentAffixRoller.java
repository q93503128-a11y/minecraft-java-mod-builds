package dev.moonseungjun.openworldrpg.equipment;

import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical data-driven ordinary-equipment affix roll procedure.
 *
 * <p>Runtime owns selection/roll validation. Item data owns the eligible affix identities and
 * ranges. The roller never invents an item-specific pool.</p>
 */
public final class OrdinaryEquipmentAffixRoller {
    private static final int MAX_SAME_CATEGORY = 2;

    private OrdinaryEquipmentAffixRoller() {
    }

    public static List<GeneratedAffix> roll(
            RollRequest request
    ) {
        Objects.requireNonNull(request, "request");
        if (request.grade() == ProjectItemGrade.MYTHIC) {
            throw new IllegalArgumentException(
                    "Mythic affixes are authored and do not use the ordinary roller."
            );
        }
        int count = affixCount(request.grade());
        if (request.eligibleAffixes().size() < count) {
            throw new IllegalArgumentException(
                    "Eligible affix pool is too small for grade "
                            + request.grade()
            );
        }

        List<GeneratedAffix> result = new ArrayList<>(count);
        Set<String> usedIds = new HashSet<>();
        Map<AffixCategory, Integer> categoryCounts =
                new EnumMap<>(AffixCategory.class);

        for (int index = 0; index < count; index++) {
            List<AffixDefinition> available =
                    request.eligibleAffixes().stream()
                            .filter(value -> !usedIds.contains(value.id()))
                            .filter(value ->
                                    categoryCounts.getOrDefault(
                                            value.category(),
                                            0
                                    ) < MAX_SAME_CATEGORY
                            )
                            .toList();
            if (available.isEmpty()) {
                throw new IllegalStateException(
                        "Affix constraints exhausted before grade count was satisfied."
                );
            }

            Map<AffixCategory, List<AffixDefinition>> byCategory =
                    new EnumMap<>(AffixCategory.class);
            for (AffixDefinition definition : available) {
                byCategory.computeIfAbsent(
                        definition.category(),
                        ignored -> new ArrayList<>()
                ).add(definition);
            }

            AffixCategory category = chooseCategory(
                    request.family(),
                    byCategory.keySet(),
                    request.seed(),
                    index
            );
            List<AffixDefinition> candidates =
                    byCategory.getOrDefault(category, List.of());
            if (candidates.isEmpty()) {
                throw new IllegalStateException(
                        "Chosen affix category has no eligible identity."
                );
            }

            AffixDefinition chosen = chooseEqualWeight(
                    candidates,
                    request.seed(),
                    index,
                    "identity"
            );
            double value = rollValue(
                    chosen,
                    request.itemLevel(),
                    percentileFloor(request.grade()),
                    request.seed(),
                    index
            );

            result.add(new GeneratedAffix(
                    chosen.id(),
                    chosen.category(),
                    value,
                    chosen.runtimePayload()
            ));
            usedIds.add(chosen.id());
            categoryCounts.merge(category, 1, Integer::sum);
        }

        return List.copyOf(result);
    }

    public static int affixCount(ProjectItemGrade grade) {
        Objects.requireNonNull(grade, "grade");
        return switch (grade) {
            case STANDARD -> 1;
            case REFINED -> 2;
            case SUPERIOR -> 3;
            case EXALTED -> 4;
            case MYTHIC -> throw new IllegalArgumentException(
                    "Mythic uses authored affix identities."
            );
        };
    }

    public static double percentileFloor(ProjectItemGrade grade) {
        Objects.requireNonNull(grade, "grade");
        return switch (grade) {
            case STANDARD -> 0.60;
            case REFINED -> 0.65;
            case SUPERIOR -> 0.70;
            case EXALTED -> 0.75;
            case MYTHIC -> throw new IllegalArgumentException(
                    "Mythic uses authored affix identities."
            );
        };
    }

    public static CategoryWeights categoryWeights(ItemFamily family) {
        Objects.requireNonNull(family, "family");
        return switch (family) {
            case NORMAL_WEAPON -> new CategoryWeights(20, 50, 0, 20, 10);
            case ARMOR_GENERIC -> new CategoryWeights(25, 20, 35, 10, 10);
            case ARMOR_LIGHT -> new CategoryWeights(25, 20, 15, 30, 10);
            case ARMOR_MEDIUM -> new CategoryWeights(25, 25, 25, 10, 15);
            case ARMOR_HEAVY -> new CategoryWeights(25, 10, 45, 5, 15);
            case SHIELD_DEFENSIVE_OFFHAND ->
                    new CategoryWeights(15, 10, 35, 20, 20);
            case ACCESSORY -> new CategoryWeights(20, 25, 10, 25, 20);
            case MAGICAL_FOCUS -> new CategoryWeights(20, 35, 5, 30, 10);
        };
    }

    private static AffixCategory chooseCategory(
            ItemFamily family,
            Set<AffixCategory> available,
            long seed,
            int affixIndex
    ) {
        CategoryWeights weights = categoryWeights(family);
        Map<AffixCategory, Integer> filtered = new EnumMap<>(
                AffixCategory.class
        );
        int total = 0;
        for (AffixCategory category : AffixCategory.values()) {
            if (!available.contains(category)) {
                continue;
            }
            int weight = weights.weight(category);
            if (weight <= 0) {
                continue;
            }
            filtered.put(category, weight);
            total += weight;
        }
        if (total <= 0) {
            throw new IllegalArgumentException(
                    "No positively weighted eligible affix category for " + family
            );
        }

        double target = unit(seed, affixIndex, "category") * total;
        double cursor = 0.0;
        for (AffixCategory category : AffixCategory.values()) {
            int weight = filtered.getOrDefault(category, 0);
            if (weight <= 0) {
                continue;
            }
            cursor += weight;
            if (target < cursor) {
                return category;
            }
        }
        throw new IllegalStateException("Affix category roll did not resolve.");
    }

    private static AffixDefinition chooseEqualWeight(
            List<AffixDefinition> values,
            long seed,
            int affixIndex,
            String domain
    ) {
        int index = Math.min(
                values.size() - 1,
                (int) Math.floor(
                        unit(seed, affixIndex, domain) * values.size()
                )
        );
        return values.get(index);
    }

    private static double rollValue(
            AffixDefinition definition,
            int itemLevel,
            double floor,
            long seed,
            int affixIndex
    ) {
        requireItemLevel(itemLevel);
        RawRange range = definition.valueRule() == ValueRule.PRIMARY_FLAT
                ? definition.primaryCurve().orElseThrow().range(itemLevel)
                : definition.rawRange();

        double percentile = floor
                + (1.0 - floor)
                * unit(seed, affixIndex, "value");
        double raw = range.min()
                + (range.max() - range.min()) * percentile;

        return switch (definition.valueRule()) {
            case PRIMARY_FLAT -> Math.rint(raw);
            case PERCENT_TENTH -> Math.round(raw * 10.0) / 10.0;
            case PERCENT_HALF -> Math.round(raw * 2.0) / 2.0;
        };
    }

    private static double unit(
            long seed,
            int affixIndex,
            String domain
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ByteBuffer.allocate(Long.BYTES).putLong(seed).array());
            digest.update((byte) 0);
            digest.update(
                    Integer.toString(affixIndex)
                            .getBytes(StandardCharsets.UTF_8)
            );
            digest.update((byte) 0);
            digest.update(domain.getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest.digest()).getLong();
            return (value >>> 11) * 0x1.0p-53;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable for equipment affix RNG.",
                    exception
            );
        }
    }

    private static void requireItemLevel(int itemLevel) {
        if (itemLevel < 1 || itemLevel > 80) {
            throw new IllegalArgumentException(
                    "Ordinary equipment Item Lv must be inside 1..80."
            );
        }
    }

    public enum AffixCategory {
        PRIMARY,
        OFFENSE,
        DEFENSE,
        RESOURCE,
        UTILITY
    }

    public enum ItemFamily {
        NORMAL_WEAPON,
        ARMOR_GENERIC,
        ARMOR_LIGHT,
        ARMOR_MEDIUM,
        ARMOR_HEAVY,
        SHIELD_DEFENSIVE_OFFHAND,
        ACCESSORY,
        MAGICAL_FOCUS
    }

    public enum ValueRule {
        PRIMARY_FLAT,
        PERCENT_TENTH,
        PERCENT_HALF
    }

    public record RawRange(
            double min,
            double max
    ) {
        public RawRange {
            if (!Double.isFinite(min)
                    || !Double.isFinite(max)
                    || min < 0.0
                    || max < min) {
                throw new IllegalArgumentException(
                        "Invalid ordinary-equipment affix raw range."
                );
            }
        }
    }

    /**
     * Data-owned affix entry. RuntimePayload is a stable runtime mapping key, not player text.
     */
    public record AffixDefinition(
            String id,
            AffixCategory category,
            ValueRule valueRule,
            RawRange rawRange,
            String runtimePayload,
            java.util.Optional<PrimaryCurve> primaryCurve
    ) {
        public AffixDefinition {
            requireStableId(id);
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(valueRule, "valueRule");
            Objects.requireNonNull(rawRange, "rawRange");
            requireStableId(runtimePayload);
            primaryCurve = Objects.requireNonNull(
                    primaryCurve,
                    "primaryCurve"
            );
            if (valueRule == ValueRule.PRIMARY_FLAT) {
                if (primaryCurve.isEmpty()
                        || rawRange.min() != 0.0
                        || rawRange.max() != 0.0) {
                    throw new IllegalArgumentException(
                            "Primary affix requires a data-owned primary curve and 0/0 raw-range sentinel."
                    );
                }
            } else {
                if (primaryCurve.isPresent()
                        || rawRange.max() <= rawRange.min()) {
                    throw new IllegalArgumentException(
                            "Percentage affix requires only a positive raw range."
                    );
                }
            }
        }

        public static AffixDefinition primary(
                String id,
                String runtimePayload,
                PrimaryCurve primaryCurve
        ) {
            return new AffixDefinition(
                    id,
                    AffixCategory.PRIMARY,
                    ValueRule.PRIMARY_FLAT,
                    new RawRange(0.0, 0.0),
                    runtimePayload,
                    java.util.Optional.of(
                            Objects.requireNonNull(
                                    primaryCurve,
                                    "primaryCurve"
                            )
                    )
            );
        }

        public static AffixDefinition percent(
                String id,
                AffixCategory category,
                double min,
                double max,
                boolean tenthRounding,
                String runtimePayload
        ) {
            return new AffixDefinition(
                    id,
                    category,
                    tenthRounding
                            ? ValueRule.PERCENT_TENTH
                            : ValueRule.PERCENT_HALF,
                    new RawRange(min, max),
                    runtimePayload,
                    java.util.Optional.empty()
            );
        }
    }

    public record PrimaryCurve(
            double baseScale,
            double perLevelScale,
            double minMultiplier,
            double maxMultiplier
    ) {
        public PrimaryCurve {
            if (!Double.isFinite(baseScale)
                    || !Double.isFinite(perLevelScale)
                    || !Double.isFinite(minMultiplier)
                    || !Double.isFinite(maxMultiplier)
                    || baseScale <= 0.0
                    || perLevelScale < 0.0
                    || minMultiplier <= 0.0
                    || maxMultiplier <= minMultiplier) {
                throw new IllegalArgumentException(
                        "Invalid data-owned primary-affix curve."
                );
            }
        }

        public RawRange range(int itemLevel) {
            requireItemLevel(itemLevel);
            double scale = baseScale
                    + perLevelScale * (itemLevel - 1);
            double min = Math.max(
                    1.0,
                    Math.round(minMultiplier * scale)
            );
            double max = Math.max(
                    min + 1.0,
                    Math.round(maxMultiplier * scale)
            );
            return new RawRange(min, max);
        }
    }

    public record RollRequest(
            ProjectItemGrade grade,
            int itemLevel,
            ItemFamily family,
            List<AffixDefinition> eligibleAffixes,
            long seed
    ) {
        public RollRequest {
            Objects.requireNonNull(grade, "grade");
            requireItemLevel(itemLevel);
            Objects.requireNonNull(family, "family");
            eligibleAffixes = List.copyOf(
                    Objects.requireNonNull(
                            eligibleAffixes,
                            "eligibleAffixes"
                    )
            );
            Set<String> ids = new HashSet<>();
            for (AffixDefinition definition : eligibleAffixes) {
                if (!ids.add(definition.id())) {
                    throw new IllegalArgumentException(
                            "Duplicate eligible affix id: " + definition.id()
                    );
                }
            }
        }
    }

    public record GeneratedAffix(
            String id,
            AffixCategory category,
            double value,
            String runtimePayload
    ) {
        public GeneratedAffix {
            requireStableId(id);
            Objects.requireNonNull(category, "category");
            if (!Double.isFinite(value) || value < 0.0) {
                throw new IllegalArgumentException(
                        "Generated affix value must be finite and non-negative."
                );
            }
            requireStableId(runtimePayload);
        }
    }

    public record CategoryWeights(
            int primary,
            int offense,
            int defense,
            int resource,
            int utility
    ) {
        public CategoryWeights {
            if (primary < 0
                    || offense < 0
                    || defense < 0
                    || resource < 0
                    || utility < 0) {
                throw new IllegalArgumentException(
                        "Affix category weights must be non-negative."
                );
            }
        }

        public int weight(AffixCategory category) {
            return switch (category) {
                case PRIMARY -> primary;
                case OFFENSE -> offense;
                case DEFENSE -> defense;
                case RESOURCE -> resource;
                case UTILITY -> utility;
            };
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced affix id."
            );
        }
    }
}

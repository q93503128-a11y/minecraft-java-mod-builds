package dev.moonseungjun.openworldrpg.fishing;

import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Canon-locked non-visual R01 fishing mechanics and deterministic candidate generation. */
public final class R01FishingRules {
    public static final String COMMON_A = "r01_river_common_a";
    public static final String COMMON_B = "r01_river_common_b";
    public static final String POOL_UNCOMMON = "r01_pool_uncommon";
    public static final String FORD_RARE = "r01_ford_rare";

    public static final List<FishDefinition> FISH = List.of(
            new FishDefinition(COMMON_A, Rarity.COMMON, 16.0, 24.0, 34.0, 42.0, 4),
            new FishDefinition(COMMON_B, Rarity.COMMON, 12.0, 18.0, 28.0, 34.0, 5),
            new FishDefinition(POOL_UNCOMMON, Rarity.UNCOMMON, 24.0, 32.0, 46.0, 54.0, 7),
            new FishDefinition(FORD_RARE, Rarity.RARE, 32.0, 44.0, 64.0, 76.0, 14)
    );

    private R01FishingRules() {
    }

    public static FishDefinition fish(String slotId) {
        Objects.requireNonNull(slotId, "slotId");
        return FISH.stream()
                .filter(value -> value.slotId().equals(slotId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown R01 fish slot: " + slotId
                ));
    }

    public static int masteryRankForXp(int xp) {
        return R01GatheringRules.masteryRankForXp(xp);
    }

    public static int personalCatchCapacity(SpotTier tier) {
        Objects.requireNonNull(tier, "tier");
        return tier == SpotTier.RARE ? 1 : 2;
    }

    public static long respawnActiveTicks(SpotTier tier) {
        Objects.requireNonNull(tier, "tier");
        return switch (tier) {
            case ORDINARY -> 6L * 60L * 20L;
            case UNCOMMON -> 8L * 60L * 20L;
            case RARE -> 18L * 60L * 20L;
        };
    }

    public static String speciesForRoll(SpotTier tier, int roll0To99) {
        Objects.requireNonNull(tier, "tier");
        if (roll0To99 < 0 || roll0To99 >= 100) {
            throw new IllegalArgumentException("Species roll must be inside 0..99.");
        }
        return switch (tier) {
            case ORDINARY -> {
                if (roll0To99 < 55) yield COMMON_A;
                if (roll0To99 < 90) yield COMMON_B;
                yield POOL_UNCOMMON;
            }
            case UNCOMMON -> {
                if (roll0To99 < 25) yield COMMON_A;
                if (roll0To99 < 50) yield COMMON_B;
                if (roll0To99 < 95) yield POOL_UNCOMMON;
                yield FORD_RARE;
            }
            case RARE -> {
                if (roll0To99 < 10) yield COMMON_A;
                if (roll0To99 < 20) yield COMMON_B;
                if (roll0To99 < 65) yield POOL_UNCOMMON;
                yield FORD_RARE;
            }
        };
    }

    public static CatchCandidate generateCandidate(
            long worldSeed,
            String playerUuid,
            String spotId,
            SpotTier tier,
            long cycleIndex,
            int catchOrdinal,
            int masteryRank
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        UUID.fromString(playerUuid);
        requireStableId(spotId);
        Objects.requireNonNull(tier, "tier");
        if (cycleIndex < 0L || catchOrdinal < 0) {
            throw new IllegalArgumentException(
                    "Fishing cycle/ordinal must be non-negative."
            );
        }
        requireMasteryRank(masteryRank);

        String seedMaterial = worldSeed
                + "|" + playerUuid
                + "|" + spotId
                + "|" + cycleIndex
                + "|" + catchOrdinal;

        int speciesRoll = Math.min(99, (int) Math.floor(unit(seedMaterial, 0) * 100.0));
        String species = speciesForRoll(tier, speciesRoll);
        FishDefinition definition = fish(species);

        double primaryPercentile = unit(seedMaterial, 1);
        double secondaryPercentile =
                masteryRank >= 5 ? unit(seedMaterial, 2) : -1.0;
        double primarySize = sizeFromPercentile(definition, primaryPercentile);
        double resolvedSize = primarySize;
        if (secondaryPercentile >= 0.0) {
            resolvedSize = Math.max(
                    primarySize,
                    sizeFromPercentile(definition, secondaryPercentile)
            );
        }

        boolean trophy = isTrophy(definition, resolvedSize);
        double biteRoll = unit(seedMaterial, 3);
        double biteDelaySeconds = 1.50
                + biteRoll * (biteDelayMaximumSeconds(masteryRank) - 1.50);

        return new CatchCandidate(
                species,
                definition.rarity(),
                primaryPercentile,
                secondaryPercentile,
                resolvedSize,
                trophy,
                saleValue(definition, resolvedSize),
                biteDelaySeconds,
                hookWindowSeconds(definition.rarity(), trophy, masteryRank)
        );
    }

    public static double sizeFromPercentile(
            FishDefinition definition,
            double percentile
    ) {
        Objects.requireNonNull(definition, "definition");
        requireUnitPercentile(percentile);

        if (percentile < 0.10) {
            return lerp(
                    definition.minCm(),
                    definition.normalLowCm(),
                    percentile / 0.10
            );
        }
        if (percentile < 0.90) {
            return lerp(
                    definition.normalLowCm(),
                    definition.normalHighCm(),
                    (percentile - 0.10) / 0.80
            );
        }
        return lerp(
                definition.normalHighCm(),
                definition.maxCm(),
                (percentile - 0.90) / 0.10
        );
    }

    public static double trophyThresholdCm(FishDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return definition.maxCm()
                - 0.05 * (definition.maxCm() - definition.minCm());
    }

    public static boolean isTrophy(
            FishDefinition definition,
            double sizeCm
    ) {
        Objects.requireNonNull(definition, "definition");
        requireSizeInsideDefinition(definition, sizeCm);
        return sizeCm >= trophyThresholdCm(definition);
    }

    public static int saleValue(
            FishDefinition definition,
            double sizeCm
    ) {
        Objects.requireNonNull(definition, "definition");
        requireSizeInsideDefinition(definition, sizeCm);

        double multiplier;
        if (isTrophy(definition, sizeCm)) {
            multiplier = 1.50;
        } else {
            double midpoint =
                    (definition.normalLowCm() + definition.normalHighCm()) / 2.0;
            double trophyThreshold = trophyThresholdCm(definition);
            if (sizeCm <= definition.normalLowCm()) {
                multiplier = interpolateAnchors(
                        sizeCm,
                        definition.minCm(), 0.85,
                        definition.normalLowCm(), 0.95
                );
            } else if (sizeCm <= midpoint) {
                multiplier = interpolateAnchors(
                        sizeCm,
                        definition.normalLowCm(), 0.95,
                        midpoint, 1.00
                );
            } else if (sizeCm <= definition.normalHighCm()) {
                multiplier = interpolateAnchors(
                        sizeCm,
                        midpoint, 1.00,
                        definition.normalHighCm(), 1.10
                );
            } else {
                multiplier = interpolateAnchors(
                        sizeCm,
                        definition.normalHighCm(), 1.10,
                        trophyThreshold, 1.25
                );
            }
        }
        return Math.max(1, (int) Math.round(definition.baseSellGold() * multiplier));
    }

    public static double biteDelayMaximumSeconds(int masteryRank) {
        requireMasteryRank(masteryRank);
        if (masteryRank >= 4) return 4.25;
        if (masteryRank >= 2) return 4.60;
        return 5.00;
    }

    public static double hookWindowSeconds(
            Rarity rarity,
            boolean trophy,
            int masteryRank
    ) {
        Objects.requireNonNull(rarity, "rarity");
        requireMasteryRank(masteryRank);
        double base = trophy ? 0.55 : switch (rarity) {
            case COMMON -> 0.90;
            case UNCOMMON -> 0.75;
            case RARE -> 0.65;
        };
        return base + (masteryRank >= 3 ? 0.08 : 0.0);
    }

    public static boolean requiresTension(CatchCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        return candidate.trophy() || candidate.rarity() != Rarity.COMMON;
    }

    public static double requiredProgressSeconds(CatchCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.trophy()) return 5.8;
        return switch (candidate.rarity()) {
            case COMMON -> 0.0;
            case UNCOMMON -> 3.2;
            case RARE -> 4.4;
        };
    }

    public static double fishPullPerSecond(CatchCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.trophy()) return 0.16;
        return switch (candidate.rarity()) {
            case COMMON -> 0.0;
            case UNCOMMON -> 0.08;
            case RARE -> 0.12;
        };
    }

    public static TensionStep advanceTension(
            TensionState state,
            CatchCandidate candidate,
            boolean hold,
            int masteryRank,
            double deltaSeconds
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(candidate, "candidate");
        requireMasteryRank(masteryRank);
        if (!requiresTension(candidate)) {
            throw new IllegalArgumentException(
                    "Common non-Trophy catches do not use tension."
            );
        }
        if (!Double.isFinite(deltaSeconds) || deltaSeconds <= 0.0) {
            throw new IllegalArgumentException(
                    "Fishing tension delta must be finite and positive."
            );
        }

        double input = hold ? 0.28 : -0.32;
        double nextTension = clamp(
                state.tension()
                        + (fishPullPerSecond(candidate) + input) * deltaSeconds,
                0.0,
                1.0
        );
        double low = masteryRank >= 5 ? 0.225 : 0.25;
        double high = masteryRank >= 5 ? 0.775 : 0.75;

        double nextProgress = state.progressSeconds();
        if (hold && nextTension >= low && nextTension <= high) {
            nextProgress += deltaSeconds;
        }

        double nextHighDuration =
                nextTension >= 0.95
                        ? state.highFailureSeconds() + deltaSeconds
                        : 0.0;
        double nextLowDuration =
                nextTension <= 0.05
                        ? state.lowFailureSeconds() + deltaSeconds
                        : 0.0;

        TensionOutcome outcome = TensionOutcome.ACTIVE;
        if (nextHighDuration >= 0.40 || nextLowDuration >= 0.60) {
            outcome = TensionOutcome.FAILED;
        } else if (nextProgress >= requiredProgressSeconds(candidate)) {
            outcome = TensionOutcome.CAUGHT;
        }

        return new TensionStep(
                new TensionState(
                        nextTension,
                        nextProgress,
                        nextHighDuration,
                        nextLowDuration
                ),
                outcome
        );
    }

    public static TensionState initialTension() {
        return new TensionState(0.45, 0.0, 0.0, 0.0);
    }

    private static double unit(String material, int subRoll) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(material.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Integer.toString(subRoll).getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest.digest()).getLong();
            return (value >>> 11) * 0x1.0p-53;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable for deterministic fishing RNG.",
                    exception
            );
        }
    }

    private static double interpolateAnchors(
            double x,
            double x0,
            double y0,
            double x1,
            double y1
    ) {
        if (x1 <= x0) return y1;
        return lerp(y0, y1, clamp((x - x0) / (x1 - x0), 0.0, 1.0));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void requireUnitPercentile(double percentile) {
        if (!Double.isFinite(percentile)
                || percentile < 0.0
                || percentile >= 1.0) {
            throw new IllegalArgumentException(
                    "Fishing percentile must be inside [0, 1)."
            );
        }
    }

    private static void requireSizeInsideDefinition(
            FishDefinition definition,
            double sizeCm
    ) {
        if (!Double.isFinite(sizeCm)
                || sizeCm < definition.minCm() - 1.0e-9
                || sizeCm > definition.maxCm() + 1.0e-9) {
            throw new IllegalArgumentException(
                    "Fish size must remain inside its authored bounds."
            );
        }
    }

    private static void requireMasteryRank(int rank) {
        if (rank < 1 || rank > 5) {
            throw new IllegalArgumentException(
                    "Fishing mastery rank must be inside 1..5."
            );
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced fishing spot id."
            );
        }
    }

    public enum SpotTier {
        ORDINARY,
        UNCOMMON,
        RARE;

        public static SpotTier parse(String value) {
            Objects.requireNonNull(value, "value");
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum Rarity {
        COMMON,
        UNCOMMON,
        RARE;

        public static Rarity parse(String value) {
            Objects.requireNonNull(value, "value");
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public record FishDefinition(
            String slotId,
            Rarity rarity,
            double minCm,
            double normalLowCm,
            double normalHighCm,
            double maxCm,
            int baseSellGold
    ) {
        public FishDefinition {
            Objects.requireNonNull(slotId, "slotId");
            Objects.requireNonNull(rarity, "rarity");
            if (!(minCm > 0.0
                    && minCm < normalLowCm
                    && normalLowCm < normalHighCm
                    && normalHighCm < maxCm)) {
                throw new IllegalArgumentException(
                        "R01 fish size anchors must be strictly increasing."
                );
            }
            if (baseSellGold <= 0) {
                throw new IllegalArgumentException(
                        "R01 fish base sell value must be positive."
                );
            }
        }
    }

    public record CatchCandidate(
            String fishSlotId,
            Rarity rarity,
            double primarySizePercentile,
            double secondarySizePercentile,
            double sizeCm,
            boolean trophy,
            int saleValueGold,
            double biteDelaySeconds,
            double hookWindowSeconds
    ) {
        public CatchCandidate {
            Objects.requireNonNull(fishSlotId, "fishSlotId");
            Objects.requireNonNull(rarity, "rarity");
            FishDefinition definition = fish(fishSlotId);
            if (definition.rarity() != rarity) {
                throw new IllegalArgumentException(
                        "Fishing candidate rarity does not match its slot."
                );
            }
            requireUnitPercentile(primarySizePercentile);
            if (secondarySizePercentile != -1.0) {
                requireUnitPercentile(secondarySizePercentile);
            }
            requireSizeInsideDefinition(definition, sizeCm);
            if (trophy != isTrophy(definition, sizeCm)) {
                throw new IllegalArgumentException(
                        "Fishing Trophy flag does not match resolved size."
                );
            }
            if (saleValueGold != saleValue(definition, sizeCm)) {
                throw new IllegalArgumentException(
                        "Fishing sale value does not match resolved size."
                );
            }
            if (!Double.isFinite(biteDelaySeconds)
                    || biteDelaySeconds < 1.50
                    || biteDelaySeconds > 5.00
                    || !Double.isFinite(hookWindowSeconds)
                    || hookWindowSeconds <= 0.0) {
                throw new IllegalArgumentException(
                        "Fishing candidate timing is outside canonical bounds."
                );
            }
        }
    }

    public record TensionState(
            double tension,
            double progressSeconds,
            double highFailureSeconds,
            double lowFailureSeconds
    ) {
        public TensionState {
            if (!Double.isFinite(tension)
                    || tension < 0.0
                    || tension > 1.0
                    || !Double.isFinite(progressSeconds)
                    || progressSeconds < 0.0
                    || !Double.isFinite(highFailureSeconds)
                    || highFailureSeconds < 0.0
                    || !Double.isFinite(lowFailureSeconds)
                    || lowFailureSeconds < 0.0) {
                throw new IllegalArgumentException(
                        "Invalid fishing tension state."
                );
            }
        }
    }

    public enum TensionOutcome {
        ACTIVE,
        CAUGHT,
        FAILED
    }

    public record TensionStep(
            TensionState state,
            TensionOutcome outcome
    ) {
        public TensionStep {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(outcome, "outcome");
        }
    }
}

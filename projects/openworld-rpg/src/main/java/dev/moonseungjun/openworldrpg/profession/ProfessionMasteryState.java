package dev.moonseungjun.openworldrpg.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Persistent server-owned Smithing/Alchemy/Cooking mastery. */
public record ProfessionMasteryState(
        int schemaVersion,
        Map<String, Integer> insights,
        Set<String> insightFlags
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<ProfessionMasteryState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("profession_mastery_schema_version")
                            .forGetter(ProfessionMasteryState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("insights")
                            .forGetter(ProfessionMasteryState::insights),
                    STRING_SET_CODEC
                            .fieldOf("insight_flags")
                            .forGetter(ProfessionMasteryState::insightFlags)
            ).apply(instance, ProfessionMasteryState::new));

    public ProfessionMasteryState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported profession mastery schema.");
        }
        insights = Map.copyOf(Objects.requireNonNull(insights, "insights"));
        insightFlags = Set.copyOf(Objects.requireNonNull(insightFlags, "insightFlags"));
        for (Map.Entry<String, Integer> entry : insights.entrySet()) {
            requireStableId(entry.getKey());
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Profession Insight must be non-negative.");
            }
        }
        insightFlags.forEach(ProfessionMasteryState::requireStableId);
    }

    public static ProfessionMasteryState initial() {
        return new ProfessionMasteryState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Set.of()
        );
    }

    public int insights(Profession profession) {
        Objects.requireNonNull(profession, "profession");
        return insights.getOrDefault(profession.id(), 0);
    }

    public int rank(Profession profession) {
        return rankForInsights(insights(profession));
    }

    public ProfessionMasteryState awardInsightOnce(
            Profession profession,
            String insightFlag
    ) {
        Objects.requireNonNull(profession, "profession");
        requireStableId(insightFlag);
        if (insightFlags.contains(insightFlag)) {
            return this;
        }

        Map<String, Integer> nextInsights = new HashMap<>(insights);
        nextInsights.put(
                profession.id(),
                Math.addExact(nextInsights.getOrDefault(profession.id(), 0), 1)
        );
        Set<String> nextFlags = new HashSet<>(insightFlags);
        nextFlags.add(insightFlag);
        return new ProfessionMasteryState(
                schemaVersion,
                Map.copyOf(nextInsights),
                Set.copyOf(nextFlags)
        );
    }

    public static int rankForInsights(int insightCount) {
        if (insightCount < 0) {
            throw new IllegalArgumentException("Insight count must be non-negative.");
        }
        if (insightCount >= 22) return 5;
        if (insightCount >= 15) return 4;
        if (insightCount >= 9) return 3;
        if (insightCount >= 4) return 2;
        return 1;
    }

    public static int feeReductionPercent(int rank) {
        return switch (rank) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 4;
            case 4 -> 6;
            case 5 -> 8;
            default -> throw new IllegalArgumentException(
                    "Profession Mastery rank must be inside 1..5."
            );
        };
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable namespaced id.");
        }
    }

    public enum Profession {
        SMITHING("openworld_rpg:smithing"),
        ALCHEMY("openworld_rpg:alchemy"),
        COOKING("openworld_rpg:cooking");

        private final String id;

        Profession(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }
}

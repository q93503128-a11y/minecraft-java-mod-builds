package kr.moonseungjun.riftfrontier.content;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public sealed interface CoreDefinition permits CoreDefinition.CombatArchetype, CoreDefinition.Region, CoreDefinition.LootProfile, CoreDefinition.Creature, CoreDefinition.Encounter, CoreDefinition.ExpeditionResource, CoreDefinition.Contract, CoreDefinition.ExtractionResultProfile {
    ContentId id();
    Kind kind();

    enum Kind {
        COMBAT_ARCHETYPE,
        REGION,
        LOOT_PROFILE,
        CREATURE,
        ENCOUNTER,
        EXPEDITION_RESOURCE,
        CONTRACT,
        EXTRACTION_RESULT
    }

    record CombatArchetype(ContentId id, Set<String> behaviours) implements CoreDefinition {
        public CombatArchetype { behaviours = Set.copyOf(Objects.requireNonNull(behaviours)); }
        @Override public Kind kind() { return Kind.COMBAT_ARCHETYPE; }
    }

    record Region(
        ContentId id,
        String gameplayRule,
        Set<ContentId> archetypes,
        Set<ContentId> resources,
        Set<ContentId> contracts
    ) implements CoreDefinition {
        public Region {
            gameplayRule = Objects.requireNonNull(gameplayRule).trim();
            archetypes = Set.copyOf(Objects.requireNonNull(archetypes));
            resources = Set.copyOf(Objects.requireNonNull(resources));
            contracts = Set.copyOf(Objects.requireNonNull(contracts));
        }

        /** Compatibility constructor for M1 fixtures and tests while M2 packs adopt explicit links. */
        public Region(ContentId id, String gameplayRule, Set<ContentId> archetypes) {
            this(id, gameplayRule, archetypes, Set.of(), Set.of());
        }

        @Override public Kind kind() { return Kind.REGION; }
    }

    record LootProfile(ContentId id, List<String> pools) implements CoreDefinition {
        public LootProfile { pools = List.copyOf(Objects.requireNonNull(pools)); }
        @Override public Kind kind() { return Kind.LOOT_PROFILE; }
    }

    record Creature(ContentId id, ContentId region, ContentId archetype, ContentId lootProfile, Set<String> behaviours) implements CoreDefinition {
        public Creature { behaviours = Set.copyOf(Objects.requireNonNull(behaviours)); }
        @Override public Kind kind() { return Kind.CREATURE; }
    }

    record Encounter(ContentId id, ContentId region, List<ContentId> participants, String objective, String worldConsequence) implements CoreDefinition {
        public Encounter {
            participants = List.copyOf(Objects.requireNonNull(participants));
            objective = Objects.requireNonNull(objective).trim();
            worldConsequence = Objects.requireNonNull(worldConsequence).trim();
        }
        @Override public Kind kind() { return Kind.ENCOUNTER; }
    }

    /** A resource whose meaning exists in the expedition loop, not merely as an item stack. */
    record ExpeditionResource(
        ContentId id,
        ContentId region,
        String category,
        int carryWeight,
        int fieldValue
    ) implements CoreDefinition {
        public ExpeditionResource {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(region, "region");
            category = Objects.requireNonNull(category, "category").trim();
            if (carryWeight <= 0) throw new IllegalArgumentException("carryWeight must be > 0");
            if (fieldValue < 0) throw new IllegalArgumentException("fieldValue must be >= 0");
        }
        @Override public Kind kind() { return Kind.EXPEDITION_RESOURCE; }
    }

    /** Data-authored objective and reward contract. Runtime acceptance/progress lives outside content definitions. */
    record Contract(
        ContentId id,
        ContentId region,
        String objective,
        Map<ContentId, Integer> requiredResources,
        ContentId rewardLootProfile,
        ContentId extractionResult,
        String worldConsequence
    ) implements CoreDefinition {
        public Contract {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(region, "region");
            objective = Objects.requireNonNull(objective, "objective").trim();
            requiredResources = Map.copyOf(Objects.requireNonNull(requiredResources));
            Objects.requireNonNull(rewardLootProfile, "rewardLootProfile");
            Objects.requireNonNull(extractionResult, "extractionResult");
            worldConsequence = Objects.requireNonNull(worldConsequence, "worldConsequence").trim();
            requiredResources.forEach((resource, amount) -> {
                Objects.requireNonNull(resource, "required resource id");
                if (amount == null || amount <= 0) throw new IllegalArgumentException("required resource amounts must be > 0");
            });
        }
        @Override public Kind kind() { return Kind.CONTRACT; }
    }

    /** Policy describing what a terminal extraction outcome does; actual run results are authoritative runtime state. */
    record ExtractionResultProfile(
        ContentId id,
        String outcome,
        int retainedPercent,
        int threatDelta,
        String worldConsequence
    ) implements CoreDefinition {
        public ExtractionResultProfile {
            Objects.requireNonNull(id, "id");
            outcome = Objects.requireNonNull(outcome, "outcome").trim();
            if (retainedPercent < 0 || retainedPercent > 100) throw new IllegalArgumentException("retainedPercent must be between 0 and 100");
            worldConsequence = Objects.requireNonNull(worldConsequence, "worldConsequence").trim();
        }
        @Override public Kind kind() { return Kind.EXTRACTION_RESULT; }
    }
}

package kr.moonseungjun.riftfrontier.content;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public sealed interface CoreDefinition permits CoreDefinition.CombatArchetype, CoreDefinition.Region, CoreDefinition.LootProfile, CoreDefinition.Creature, CoreDefinition.Encounter {
    ContentId id();
    Kind kind();

    enum Kind { COMBAT_ARCHETYPE, REGION, LOOT_PROFILE, CREATURE, ENCOUNTER }

    record CombatArchetype(ContentId id, Set<String> behaviours) implements CoreDefinition {
        public CombatArchetype { behaviours = Set.copyOf(Objects.requireNonNull(behaviours)); }
        @Override public Kind kind() { return Kind.COMBAT_ARCHETYPE; }
    }

    record Region(ContentId id, String gameplayRule, Set<ContentId> archetypes) implements CoreDefinition {
        public Region {
            gameplayRule = Objects.requireNonNull(gameplayRule).trim();
            archetypes = Set.copyOf(Objects.requireNonNull(archetypes));
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
}

package kr.moonseungjun.riftfrontier.content;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public sealed interface CoreDefinition permits CoreDefinition.CombatArchetype, CoreDefinition.Region, CoreDefinition.LootProfile, CoreDefinition.Creature, CoreDefinition.Encounter, CoreDefinition.ExpeditionResource, CoreDefinition.Contract, CoreDefinition.ExtractionResultProfile, CoreDefinition.AttackPattern, CoreDefinition.BossProfile, CoreDefinition.WeaponFamily, CoreDefinition.WeaponModule {
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
        EXTRACTION_RESULT,
        ATTACK_PATTERN,
        BOSS_PROFILE,
        WEAPON_FAMILY,
        WEAPON_MODULE
    }

    record CombatArchetype(ContentId id, Set<String> behaviours) implements CoreDefinition {
        public CombatArchetype { behaviours = Set.copyOf(Objects.requireNonNull(behaviours)); }
        @Override public Kind kind() { return Kind.COMBAT_ARCHETYPE; }
    }

    record Region(ContentId id, String gameplayRule, Set<ContentId> archetypes, Set<ContentId> resources, Set<ContentId> contracts) implements CoreDefinition {
        public Region {
            gameplayRule = Objects.requireNonNull(gameplayRule).trim();
            archetypes = Set.copyOf(Objects.requireNonNull(archetypes));
            resources = Set.copyOf(Objects.requireNonNull(resources));
            contracts = Set.copyOf(Objects.requireNonNull(contracts));
        }
        public Region(ContentId id, String gameplayRule, Set<ContentId> archetypes) { this(id, gameplayRule, archetypes, Set.of(), Set.of()); }
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

    record ExpeditionResource(ContentId id, ContentId region, String category, int carryWeight, int fieldValue) implements CoreDefinition {
        public ExpeditionResource {
            Objects.requireNonNull(id, "id"); Objects.requireNonNull(region, "region");
            category = Objects.requireNonNull(category, "category").trim();
            if (carryWeight <= 0) throw new IllegalArgumentException("carryWeight must be > 0");
            if (fieldValue < 0) throw new IllegalArgumentException("fieldValue must be >= 0");
        }
        @Override public Kind kind() { return Kind.EXPEDITION_RESOURCE; }
    }

    record Contract(ContentId id, ContentId region, String objective, Map<ContentId, Integer> requiredResources, ContentId rewardLootProfile, ContentId extractionResult, String worldConsequence) implements CoreDefinition {
        public Contract {
            Objects.requireNonNull(id, "id"); Objects.requireNonNull(region, "region");
            objective = Objects.requireNonNull(objective, "objective").trim();
            requiredResources = Map.copyOf(Objects.requireNonNull(requiredResources));
            Objects.requireNonNull(rewardLootProfile, "rewardLootProfile"); Objects.requireNonNull(extractionResult, "extractionResult");
            worldConsequence = Objects.requireNonNull(worldConsequence, "worldConsequence").trim();
            requiredResources.forEach((resource, amount) -> { Objects.requireNonNull(resource, "required resource id"); if (amount == null || amount <= 0) throw new IllegalArgumentException("required resource amounts must be > 0"); });
        }
        @Override public Kind kind() { return Kind.CONTRACT; }
    }

    record ExtractionResultProfile(ContentId id, String outcome, int retainedPercent, int threatDelta, String worldConsequence) implements CoreDefinition {
        public ExtractionResultProfile {
            Objects.requireNonNull(id, "id"); outcome = Objects.requireNonNull(outcome, "outcome").trim();
            if (retainedPercent < 0 || retainedPercent > 100) throw new IllegalArgumentException("retainedPercent must be between 0 and 100");
            worldConsequence = Objects.requireNonNull(worldConsequence, "worldConsequence").trim();
        }
        @Override public Kind kind() { return Kind.EXTRACTION_RESULT; }
    }

    /** Data-authored combat cadence shared by server attack runtimes. Presentation resolves separately. */
    record AttackPattern(ContentId id, String delivery, int telegraphTicks, int activeTicks, int recoveryTicks, Set<String> counterplay, String presentationCue) implements CoreDefinition {
        public AttackPattern {
            Objects.requireNonNull(id, "id"); delivery = Objects.requireNonNull(delivery, "delivery").trim();
            counterplay = Set.copyOf(Objects.requireNonNull(counterplay, "counterplay"));
            presentationCue = Objects.requireNonNull(presentationCue, "presentationCue").trim();
            if (telegraphTicks <= 0) throw new IllegalArgumentException("telegraphTicks must be > 0");
            if (activeTicks <= 0) throw new IllegalArgumentException("activeTicks must be > 0");
            if (recoveryTicks < 0) throw new IllegalArgumentException("recoveryTicks must be >= 0");
        }
        public int totalTicks() { return Math.addExact(Math.addExact(telegraphTicks, activeTicks), recoveryTicks); }
        @Override public Kind kind() { return Kind.ATTACK_PATTERN; }
    }

    /** Boss policy links reusable attack patterns without committing to a final model or animation set. */
    record BossProfile(ContentId id, int phaseCount, Set<ContentId> attackPatterns, String arenaRule) implements CoreDefinition {
        public BossProfile {
            Objects.requireNonNull(id, "id");
            attackPatterns = Set.copyOf(Objects.requireNonNull(attackPatterns, "attackPatterns"));
            arenaRule = Objects.requireNonNull(arenaRule, "arenaRule").trim();
            if (phaseCount <= 0) throw new IllegalArgumentException("phaseCount must be > 0");
        }
        @Override public Kind kind() { return Kind.BOSS_PROFILE; }
    }

    /**
     * Player weapon-family semantics. Moves reuse authoritative attack patterns; roles and sockets are
     * composition tokens, not balance values or presentation assets.
     */
    record WeaponFamily(ContentId id, Set<ContentId> moves, Set<String> combatRoles, Set<String> moduleSockets) implements CoreDefinition {
        public WeaponFamily {
            Objects.requireNonNull(id, "id");
            moves = Set.copyOf(Objects.requireNonNull(moves, "moves"));
            combatRoles = Set.copyOf(Objects.requireNonNull(combatRoles, "combatRoles"));
            moduleSockets = Set.copyOf(Objects.requireNonNull(moduleSockets, "moduleSockets"));
        }
        @Override public Kind kind() { return Kind.WEAPON_FAMILY; }
    }

    /** Behaviour-changing module composition. Exact stat deltas remain outside this semantic gate. */
    record WeaponModule(ContentId id, Set<ContentId> compatibleFamilies, String socket, Set<String> behaviourChanges) implements CoreDefinition {
        public WeaponModule {
            Objects.requireNonNull(id, "id");
            compatibleFamilies = Set.copyOf(Objects.requireNonNull(compatibleFamilies, "compatibleFamilies"));
            socket = Objects.requireNonNull(socket, "socket").trim();
            behaviourChanges = Set.copyOf(Objects.requireNonNull(behaviourChanges, "behaviourChanges"));
        }
        @Override public Kind kind() { return Kind.WEAPON_MODULE; }
    }
}

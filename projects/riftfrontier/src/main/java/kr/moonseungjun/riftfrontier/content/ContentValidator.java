package kr.moonseungjun.riftfrontier.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ContentValidator {
    public enum Severity { ERROR, WARN }

    /** Stable machine-readable codes for CI, tooling, and future in-game diagnostics. */
    public enum Code {
        EMPTY_BEHAVIOUR_SET,
        MISSING_UNIQUE_GAMEPLAY_RULE,
        NO_REGION_ARCHETYPES,
        NO_REGION_RESOURCES,
        NO_REGION_CONTRACTS,
        EMPTY_LOOT_POOLS,
        MISSING_REFERENCE,
        EMPTY_ENCOUNTER_PARTICIPANTS,
        EMPTY_ENCOUNTER_OBJECTIVE,
        EMPTY_CONTRACT_OBJECTIVE,
        EMPTY_CONTRACT_REQUIREMENTS,
        INVALID_RESOURCE_CATEGORY,
        INVALID_EXTRACTION_OUTCOME,
        NO_WORLD_CONSEQUENCE
    }

    public record Issue(Code code, Severity severity, ContentId source, String message) { }

    public record Report(int definitionCount, List<Issue> issues) {
        public Report { issues = List.copyOf(issues); }
        public boolean hasErrors() { return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR); }
        public List<Issue> warnings() { return issues.stream().filter(i -> i.severity() == Severity.WARN).toList(); }
        public List<Issue> byCode(Code code) { return issues.stream().filter(i -> i.code() == code).toList(); }
        public String format() {
            return issues.stream()
                .map(i -> i.severity() + " [" + i.code() + "] " + i.source() + " - " + i.message())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("OK");
        }
    }

    public Report validate(ContentRegistry registry) {
        List<Issue> issues = new ArrayList<>();
        for (CoreDefinition definition : registry.all()) {
            switch (definition) {
                case CoreDefinition.CombatArchetype archetype -> {
                    if (archetype.behaviours().isEmpty()) error(issues, Code.EMPTY_BEHAVIOUR_SET, archetype.id(), "combat archetype has no behaviours");
                }
                case CoreDefinition.Region region -> {
                    if (region.gameplayRule().isBlank()) error(issues, Code.MISSING_UNIQUE_GAMEPLAY_RULE, region.id(), "region has no unique gameplay rule");
                    if (region.archetypes().isEmpty()) warn(issues, Code.NO_REGION_ARCHETYPES, region.id(), "region has no combat archetypes yet");
                    if (region.resources().isEmpty()) warn(issues, Code.NO_REGION_RESOURCES, region.id(), "region has no expedition resources yet");
                    if (region.contracts().isEmpty()) warn(issues, Code.NO_REGION_CONTRACTS, region.id(), "region has no contracts yet");
                    for (ContentId id : region.archetypes()) require(registry, issues, region.id(), CoreDefinition.Kind.COMBAT_ARCHETYPE, id);
                    for (ContentId id : region.resources()) require(registry, issues, region.id(), CoreDefinition.Kind.EXPEDITION_RESOURCE, id);
                    for (ContentId id : region.contracts()) require(registry, issues, region.id(), CoreDefinition.Kind.CONTRACT, id);
                }
                case CoreDefinition.LootProfile loot -> {
                    if (loot.pools().isEmpty()) warn(issues, Code.EMPTY_LOOT_POOLS, loot.id(), "loot profile has no pools");
                }
                case CoreDefinition.Creature creature -> {
                    require(registry, issues, creature.id(), CoreDefinition.Kind.REGION, creature.region());
                    require(registry, issues, creature.id(), CoreDefinition.Kind.COMBAT_ARCHETYPE, creature.archetype());
                    require(registry, issues, creature.id(), CoreDefinition.Kind.LOOT_PROFILE, creature.lootProfile());
                    if (creature.behaviours().isEmpty()) error(issues, Code.EMPTY_BEHAVIOUR_SET, creature.id(), "creature has no behaviours");
                }
                case CoreDefinition.Encounter encounter -> {
                    require(registry, issues, encounter.id(), CoreDefinition.Kind.REGION, encounter.region());
                    if (encounter.participants().isEmpty()) error(issues, Code.EMPTY_ENCOUNTER_PARTICIPANTS, encounter.id(), "encounter has no participants");
                    for (ContentId id : encounter.participants()) require(registry, issues, encounter.id(), CoreDefinition.Kind.CREATURE, id);
                    if (encounter.objective().isBlank()) error(issues, Code.EMPTY_ENCOUNTER_OBJECTIVE, encounter.id(), "encounter has no objective");
                    if (encounter.worldConsequence().isBlank()) warn(issues, Code.NO_WORLD_CONSEQUENCE, encounter.id(), "encounter has no world consequence");
                }
                case CoreDefinition.ExpeditionResource resource -> {
                    require(registry, issues, resource.id(), CoreDefinition.Kind.REGION, resource.region());
                    if (resource.category().isBlank()) error(issues, Code.INVALID_RESOURCE_CATEGORY, resource.id(), "expedition resource category is blank");
                }
                case CoreDefinition.Contract contract -> {
                    require(registry, issues, contract.id(), CoreDefinition.Kind.REGION, contract.region());
                    require(registry, issues, contract.id(), CoreDefinition.Kind.LOOT_PROFILE, contract.rewardLootProfile());
                    require(registry, issues, contract.id(), CoreDefinition.Kind.EXTRACTION_RESULT, contract.extractionResult());
                    if (contract.objective().isBlank()) error(issues, Code.EMPTY_CONTRACT_OBJECTIVE, contract.id(), "contract has no objective");
                    if (contract.requiredResources().isEmpty()) warn(issues, Code.EMPTY_CONTRACT_REQUIREMENTS, contract.id(), "contract has no resource requirements");
                    for (ContentId id : contract.requiredResources().keySet()) {
                        require(registry, issues, contract.id(), CoreDefinition.Kind.EXPEDITION_RESOURCE, id);
                    }
                    if (contract.worldConsequence().isBlank()) warn(issues, Code.NO_WORLD_CONSEQUENCE, contract.id(), "contract has no world consequence");
                }
                case CoreDefinition.ExtractionResultProfile extraction -> {
                    if (extraction.outcome().isBlank()) error(issues, Code.INVALID_EXTRACTION_OUTCOME, extraction.id(), "extraction result outcome is blank");
                    if (extraction.worldConsequence().isBlank()) warn(issues, Code.NO_WORLD_CONSEQUENCE, extraction.id(), "extraction result has no world consequence");
                }
            }
        }
        issues.sort(
            Comparator.comparing((Issue i) -> i.severity().ordinal())
                .thenComparing(i -> i.source().toString())
                .thenComparing(i -> i.code().name())
                .thenComparing(Issue::message)
        );
        return new Report(registry.size(), issues);
    }

    private static void require(ContentRegistry registry, List<Issue> issues, ContentId source, CoreDefinition.Kind kind, ContentId target) {
        if (!registry.contains(kind, target)) {
            error(issues, Code.MISSING_REFERENCE, source, "missing " + kind + " reference: " + target);
        }
    }

    private static void error(List<Issue> issues, Code code, ContentId id, String message) {
        issues.add(new Issue(code, Severity.ERROR, id, message));
    }

    private static void warn(List<Issue> issues, Code code, ContentId id, String message) {
        issues.add(new Issue(code, Severity.WARN, id, message));
    }
}

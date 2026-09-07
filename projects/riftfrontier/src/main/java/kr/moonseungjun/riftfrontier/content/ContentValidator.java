package kr.moonseungjun.riftfrontier.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ContentValidator {
    public enum Severity { ERROR, WARN }
    public record Issue(Severity severity, ContentId source, String message) { }
    public record Report(int definitionCount, List<Issue> issues) {
        public Report { issues = List.copyOf(issues); }
        public boolean hasErrors() { return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR); }
        public List<Issue> warnings() { return issues.stream().filter(i -> i.severity() == Severity.WARN).toList(); }
        public String format() { return issues.stream().map(i -> i.severity() + " " + i.source() + " - " + i.message()).reduce((a,b) -> a + "\n" + b).orElse("OK"); }
    }

    public Report validate(ContentRegistry registry) {
        List<Issue> issues = new ArrayList<>();
        for (CoreDefinition definition : registry.all()) {
            switch (definition) {
                case CoreDefinition.CombatArchetype archetype -> {
                    if (archetype.behaviours().isEmpty()) error(issues, archetype.id(), "combat archetype has no behaviours");
                }
                case CoreDefinition.Region region -> {
                    if (region.gameplayRule().isBlank()) error(issues, region.id(), "region has no unique gameplay rule");
                    if (region.archetypes().isEmpty()) warn(issues, region.id(), "region has no combat archetypes yet");
                    for (ContentId id : region.archetypes()) require(registry, issues, region.id(), CoreDefinition.Kind.COMBAT_ARCHETYPE, id);
                }
                case CoreDefinition.LootProfile loot -> {
                    if (loot.pools().isEmpty()) warn(issues, loot.id(), "loot profile has no pools");
                }
                case CoreDefinition.Creature creature -> {
                    require(registry, issues, creature.id(), CoreDefinition.Kind.REGION, creature.region());
                    require(registry, issues, creature.id(), CoreDefinition.Kind.COMBAT_ARCHETYPE, creature.archetype());
                    require(registry, issues, creature.id(), CoreDefinition.Kind.LOOT_PROFILE, creature.lootProfile());
                    if (creature.behaviours().isEmpty()) error(issues, creature.id(), "creature has no behaviours");
                }
                case CoreDefinition.Encounter encounter -> {
                    require(registry, issues, encounter.id(), CoreDefinition.Kind.REGION, encounter.region());
                    if (encounter.participants().isEmpty()) error(issues, encounter.id(), "encounter has no participants");
                    for (ContentId id : encounter.participants()) require(registry, issues, encounter.id(), CoreDefinition.Kind.CREATURE, id);
                    if (encounter.objective().isBlank()) error(issues, encounter.id(), "encounter has no objective");
                    if (encounter.worldConsequence().isBlank()) warn(issues, encounter.id(), "encounter has no world consequence");
                }
            }
        }
        issues.sort(Comparator.comparing((Issue i) -> i.severity().ordinal()).thenComparing(i -> i.source().toString()).thenComparing(Issue::message));
        return new Report(registry.size(), issues);
    }

    private static void require(ContentRegistry registry, List<Issue> issues, ContentId source, CoreDefinition.Kind kind, ContentId target) {
        if (!registry.contains(kind, target)) error(issues, source, "missing " + kind + " reference: " + target);
    }
    private static void error(List<Issue> issues, ContentId id, String message) { issues.add(new Issue(Severity.ERROR, id, message)); }
    private static void warn(List<Issue> issues, ContentId id, String message) { issues.add(new Issue(Severity.WARN, id, message)); }
}

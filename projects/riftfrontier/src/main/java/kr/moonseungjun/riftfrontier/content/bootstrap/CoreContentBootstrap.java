package kr.moonseungjun.riftfrontier.content.bootstrap;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.ContentValidator;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.List;
import java.util.Set;

/** M1 fixture expressed only through shared definitions. Names here are technical fixtures, not locked production lore/art. */
public final class CoreContentBootstrap {
    private CoreContentBootstrap() { }

    public static ContentRegistry createFixtureRegistry() {
        ContentRegistry registry = new ContentRegistry();
        ContentId pursuer = ContentId.rift("archetype/pursuer");
        ContentId skirmisher = ContentId.rift("archetype/skirmisher");
        ContentId region = ContentId.rift("region/vertical_slice_01");
        ContentId loot = ContentId.rift("loot/salvage_basic");
        ContentId scout = ContentId.rift("creature/fixture_scout");
        ContentId hunter = ContentId.rift("creature/fixture_hunter");

        registry.register(new CoreDefinition.CombatArchetype(pursuer, Set.of("pursue", "melee_combo")));
        registry.register(new CoreDefinition.CombatArchetype(skirmisher, Set.of("orbit", "retreat", "ranged_pressure")));
        registry.register(new CoreDefinition.Region(region, "expedition pressure changes with unresolved local threat", Set.of(pursuer, skirmisher)));
        registry.register(new CoreDefinition.LootProfile(loot, List.of("field_salvage", "regional_material")));
        registry.register(new CoreDefinition.Creature(scout, region, skirmisher, loot, Set.of("orbit", "retreat", "ranged_pressure")));
        registry.register(new CoreDefinition.Creature(hunter, region, pursuer, loot, Set.of("pursue", "melee_combo")));
        registry.register(new CoreDefinition.Encounter(
            ContentId.rift("encounter/fixture_patrol"), region, List.of(scout, hunter),
            "recover expedition salvage while breaking contact or defeating the patrol",
            "local threat decreases and recovered supply becomes available to the return loop"
        ));
        return registry;
    }

    public static ContentValidator.Report bootstrapAndValidate() {
        return new ContentValidator().validate(createFixtureRegistry());
    }
}

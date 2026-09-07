package kr.moonseungjun.turnboundre.data;

import kr.moonseungjun.turnboundre.network.DataActionResolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class M3BundledContentTest {
    private static final String ROOT = "data/turnbound_re/turnbound_definitions/";
    private static final String ZOMBIE = "turnbound_re:zombie";

    @Test
    void productionVerticalSliceBundlesDecodeAndValidateAsOneRegistry() throws IOException {
        DefinitionBundleParser.Parsed parsed = loadBundledDefinitions();
        DefinitionRegistry registry = parsed.registry();

        assertEquals(40, registry.actions().size());
        assertEquals(8, registry.characters().size());
        assertEquals(12, registry.statuses().size());
        assertEquals(2, registry.encounters().size());
        assertEquals(2, registry.rewards().size());
        assertEquals(4, parsed.resourceIds().size());

        assertTrue(registry.characters().keySet().containsAll(Set.of(
                "turnbound_re:zombie", "turnbound_re:skeleton", "turnbound_re:spider", "turnbound_re:creeper",
                "turnbound_re:blaze", "turnbound_re:witch", "turnbound_re:enderman", "turnbound_re:iron_golem")));

        Set<String> roles = registry.characters().values().stream()
                .flatMap(character -> character.roles().stream())
                .collect(Collectors.toSet());
        assertEquals(Set.of("VANGUARD", "BREAKER", "STRIKER", "SUPPORT", "CONTROLLER"), roles);

        Set<String> damageTags = registry.actions().values().stream()
                .map(ActionDefinition::damageTag)
                .collect(Collectors.toSet());
        assertEquals(Set.of("MELEE", "PROJECTILE", "FIRE", "BLAST", "ARCANE", "VOID"), damageTags);

        Set<String> affinityGrades = registry.characters().values().stream()
                .flatMap(character -> character.affinities().values().stream())
                .collect(Collectors.toSet());
        assertTrue(affinityGrades.containsAll(Set.of("WEAK", "NORMAL", "RESIST", "IMMUNE")));

        registry.characters().values().forEach(character -> {
            assertEquals(5, character.actions().size(), character.id());
            assertEquals(2, character.skills().size(), character.id());
        });

        assertEquals(3, registry.encounters().get("turnbound_re:debug_overworld_patrol").enemies().size());
        assertEquals(4, registry.encounters().get("turnbound_re:debug_rift_elite").enemies().size());
    }

    @Test
    void dataDrivenBasicIsARealOwnedCommandWhilePassiveIsNeverSubmittedAsACommand() throws IOException {
        DefinitionRegistry registry = loadBundledDefinitions().registry();
        DataActionResolver resolver = new DataActionResolver(
                registry,
                (battleId, participantId) -> "p1".equals(participantId) ? ZOMBIE : null,
                DataActionResolver.ALLOW_ALL_RUNTIME);

        UUID battleId = UUID.randomUUID();
        var basic = resolver.resolve(battleId, "p1", "turnbound_re:zombie_rotten_swing");
        assertTrue(basic.isPresent());
        assertEquals("BASIC", basic.orElseThrow().definition().kind());
        assertTrue(basic.orElseThrow().policy().owned());

        assertTrue(resolver.resolve(battleId, "p1", "turnbound_re:zombie_undead_endurance").isEmpty());
    }

    @Test
    void bundledStatusReferencesCoverCoreAndRepresentativeControlStates() throws IOException {
        Set<String> ids = loadBundledDefinitions().registry().statuses().keySet();
        assertTrue(ids.containsAll(Set.of(
                "turnbound_re:guard", "turnbound_re:exposed", "turnbound_re:poise_guard",
                "turnbound_re:burn", "turnbound_re:slow", "turnbound_re:atk_up", "turnbound_re:def_down",
                "turnbound_re:venom", "turnbound_re:webbed", "turnbound_re:evasion",
                "turnbound_re:ward", "turnbound_re:volatile")));
    }

    private static DefinitionBundleParser.Parsed loadBundledDefinitions() throws IOException {
        Map<String, String> resources = new LinkedHashMap<>();
        for (String file : new String[]{
                "core_statuses.json", "vertical_actions.json", "vertical_characters.json", "vertical_encounters.json"}) {
            String classpath = ROOT + file;
            try (InputStream stream = M3BundledContentTest.class.getClassLoader().getResourceAsStream(classpath)) {
                assertNotNull(stream, "missing production resource " + classpath);
                resources.put("turnbound_re:turnbound_definitions/" + file,
                        new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return DefinitionBundleParser.parse(resources);
    }
}

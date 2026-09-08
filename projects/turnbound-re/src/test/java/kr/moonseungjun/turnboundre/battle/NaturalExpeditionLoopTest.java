package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionBundleParser;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgressStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalExpeditionLoopTest {
    @Test
    void productionStarterPartyLaunchesWithoutMinecraftEntityBindingsAndSharesOneController() throws IOException {
        DefinitionBundleParser.Parsed parsed = ProductionDefinitionFixture.load();
        DefinitionRegistry registry = parsed.registry();
        DefinitionRepository repository = new DefinitionRepository();
        repository.install(registry, parsed.hash());

        List<String> starterIds = registry.progressions()
                .get(PlayerProgressStore.DEFAULT_PROGRESSION_ID)
                .starterParty();
        assertEquals(List.of(
                "turnbound_re:zombie",
                "turnbound_re:skeleton",
                "turnbound_re:spider",
                "turnbound_re:creeper"), starterIds);

        List<CharacterProgress> party = starterIds.stream()
                .map(id -> levelOne(registry.characters().get(id)))
                .toList();
        UUID owner = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        BattleManager battles = new BattleManager();
        AuthoredEncounterLauncher launcher = new AuthoredEncounterLauncher(battles, repository);

        AuthoredEncounterLauncher.Launch launch = launcher.openVirtual(
                "turnbound_re:debug_overworld_patrol",
                owner,
                party,
                battleId,
                12345L);

        assertEquals(0, battles.boundEntityCount(), "virtual encounter must not create or require Minecraft entity bindings");
        assertTrue(battles.bindings(battleId).isEmpty());
        assertEquals(List.of(owner), battles.controllers(battleId));
        for (int index = 0; index < party.size(); index++) {
            assertEquals(owner, battles.controller(battleId, "player_" + index).orElseThrow());
        }
        assertEquals(party.size() + launch.encounter().enemies().size(), launch.battle().actorOrder().size());
    }

    @Test
    void enemyAutoTurnStopsAtPlayerDecisionInsteadOfRequiringDebugAdvance() throws IOException {
        DefinitionBundleParser.Parsed parsed = ProductionDefinitionFixture.load();
        DefinitionRegistry registry = parsed.registry();
        DefinitionRepository repository = new DefinitionRepository();
        repository.install(registry, parsed.hash());

        CharacterDefinition slowStarter = registry.characters().get("turnbound_re:zombie");
        CharacterProgress solo = levelOne(slowStarter);
        BattleManager battles = new BattleManager();
        AuthoredEncounterLauncher launcher = new AuthoredEncounterLauncher(battles, repository);
        UUID owner = UUID.randomUUID();

        AuthoredEncounterLauncher.Launch launch = launcher.openVirtual(
                "turnbound_re:debug_rift_elite",
                owner,
                List.of(solo),
                UUID.randomUUID(),
                777L);

        EnemyTurnService.resolveUntilPlayerOrTerminal(battles, launch.battle());

        if (launch.battle().state() == BattleState.AWAIT_COMMAND) {
            assertEquals(BattleTeam.PLAYER, launch.battle().participant(launch.battle().currentActorId()).team());
        } else {
            assertTrue(launch.battle().state() == BattleState.REWARD,
                    "automatic enemy resolution may only stop for player input or a terminal result");
        }
    }

    private static CharacterProgress levelOne(CharacterDefinition definition) {
        return new CharacterProgress(definition.id(), definition.originStar(), definition.originStar(), 1);
    }
}

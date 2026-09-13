package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.EquipmentProgress;
import kr.moonseungjun.turnboundre.progression.ProgressionRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M6EquipmentBattleIntegrationTest {
    private static final String HERO = "turnbound_re:iron_golem";
    private static final String EDGE = "turnbound_re:copper_edge";
    private static final String ENCOUNTER = "turnbound_re:debug_overworld_patrol";

    @Test
    void serverOwnedEquipmentChangesOnlyPlayerBattleParticipant() throws IOException {
        var parsed = ProductionDefinitionFixture.load();
        DefinitionRepository repository = new DefinitionRepository();
        repository.install(parsed.registry(), parsed.hash());
        BattleManager battles = new BattleManager();
        AuthoredEncounterLauncher launcher = new AuthoredEncounterLauncher(battles, repository);
        var heroDefinition = parsed.registry().characters().get(HERO);
        CharacterProgress hero = new CharacterProgress(
                HERO, heroDefinition.originStar(), heroDefinition.originStar(), 1);
        var heroBase = ProgressionRules.stats(heroDefinition, hero);

        var encounter = parsed.registry().encounters().get(ENCOUNTER);
        var firstEnemySlot = encounter.enemies().getFirst();
        var firstEnemyDefinition = parsed.registry().characters().get(firstEnemySlot.character());
        var enemyBase = ProgressionRules.stats(firstEnemyDefinition, new CharacterProgress(
                firstEnemyDefinition.id(), firstEnemyDefinition.originStar(),
                firstEnemySlot.currentStar(), firstEnemySlot.level()));

        AuthoredEncounterLauncher.Launch launch = launcher.openVirtualFromAnchor(
                ENCOUNTER,
                UUID.randomUUID(),
                List.of(hero),
                UUID.randomUUID(),
                20260913L,
                "turnbound_re:test/equipment_battle",
                true,
                BattlePreparationBonus.NONE,
                Map.of(HERO, new EquipmentProgress(EDGE, 3)));

        BattleParticipant player = launch.battle().participant("party_0");
        assertEquals(heroBase.atk() + heroBase.atk() * 8 / 100, player.attack());
        assertEquals(heroBase.spd(), player.speed());
        assertEquals(heroBase.hp(), player.maxHp());
        assertEquals(heroBase.def(), player.defense());
        assertEquals(heroBase.poise(), player.poiseMax());

        BattleParticipant enemy = launch.battle().participant("enemy_0");
        assertEquals(enemyBase.atk(), enemy.attack());
        assertEquals(enemyBase.spd(), enemy.speed());
        assertEquals(enemyBase.hp(), enemy.maxHp());
        assertEquals(enemyBase.def(), enemy.defense());
        assertEquals(enemyBase.poise(), enemy.poiseMax());
    }
}

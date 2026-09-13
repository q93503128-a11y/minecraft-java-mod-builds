package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionBundleParser;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.ProgressionRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class M6BattlePreparationLauncherTest {
    @Test
    void authoredLaunchAppliesPreparationOnlyToPlayerParticipants() throws IOException {
        DefinitionBundleParser.Parsed parsed = ProductionDefinitionFixture.load();
        DefinitionRepository repository = new DefinitionRepository();
        repository.install(parsed.registry(), parsed.hash());
        BattleManager battles = new BattleManager();
        AuthoredEncounterLauncher launcher = new AuthoredEncounterLauncher(battles, repository);

        CharacterDefinition zombie = parsed.registry().characters().get("turnbound_re:zombie");
        CharacterProgress playerProgress = new CharacterProgress(zombie.id(), zombie.originStar(), 2, 8);
        CharacterDefinition.Stats playerBase = ProgressionRules.stats(zombie, playerProgress);
        BattlePreparationBonus preparation = new BattlePreparationBonus("iron", 0, 0, 8, 8);

        AuthoredEncounterLauncher.Launch launch = launcher.openVirtualFromAnchor(
                "turnbound_re:debug_overworld_patrol",
                UUID.fromString("00000000-0000-0000-0000-000000006101"),
                List.of(playerProgress),
                UUID.fromString("00000000-0000-0000-0000-000000006102"),
                6102L,
                "turnbound_re:region_01/overworld_patrol",
                true,
                preparation);

        BattleParticipant player = launch.battle().participant("party_0");
        CharacterDefinition.Stats prepared = preparation.apply(playerBase);
        assertEquals(prepared.hp(), player.maxHp());
        assertEquals(prepared.atk(), player.attack());
        assertEquals(prepared.def(), player.defense());
        assertEquals(prepared.spd(), player.speed());
        assertEquals(prepared.poise(), player.poiseMax());

        var enemySlot = parsed.registry().encounters().get("turnbound_re:debug_overworld_patrol").enemies().getFirst();
        CharacterDefinition enemyDefinition = parsed.registry().characters().get(enemySlot.character());
        CharacterProgress enemyProgress = new CharacterProgress(
                enemyDefinition.id(), enemyDefinition.originStar(), enemySlot.currentStar(), enemySlot.level());
        CharacterDefinition.Stats enemyBase = ProgressionRules.stats(enemyDefinition, enemyProgress);
        BattleParticipant enemy = launch.battle().participant("enemy_0");
        assertEquals(enemyBase.hp(), enemy.maxHp());
        assertEquals(enemyBase.atk(), enemy.attack());
        assertEquals(enemyBase.def(), enemy.defense());
        assertEquals(enemyBase.spd(), enemy.speed());
        assertEquals(enemyBase.poise(), enemy.poiseMax());
    }
}

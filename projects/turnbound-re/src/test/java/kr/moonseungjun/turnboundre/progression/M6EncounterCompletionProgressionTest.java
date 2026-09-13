package kr.moonseungjun.turnboundre.progression;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import kr.moonseungjun.turnboundre.battle.AuthoredEncounterLauncher;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.data.EncounterDefinition;
import kr.moonseungjun.turnboundre.data.RegionDefinition;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.world.WorldEncounterAnchorAccessPolicy;
import kr.moonseungjun.turnboundre.world.WorldEncounterAnchorResolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M6EncounterCompletionProgressionTest {
    private static final String LOCATOR = "turnbound_re:test/one_time";
    private static final String SECOND_LOCATOR = "turnbound_re:test/second_one_time";
    private static final String ENCOUNTER = "turnbound_re:debug_overworld_patrol";
    private static final String REWARD_TABLE = "turnbound_re:debug_overworld_patrol";
    private static final String HERO = "turnbound_re:iron_golem";

    @Test
    void schemaOneSaveWithoutCompletionFieldStillLoads() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        var tuning = registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID);
        JsonObject legacy = new JsonObject();
        legacy.addProperty("schemaVersion", 1);
        legacy.addProperty("coin", 123L);
        legacy.addProperty("essence", 45L);
        legacy.addProperty("partyCapacity", tuning.partyCapacity());

        PlayerProgress decoded = PlayerProgress.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();

        assertEquals(1, decoded.schemaVersion());
        assertEquals(123L, decoded.coin());
        assertTrue(decoded.completedEncounterLocators().isEmpty());
    }

    @Test
    void completionRoundTripsAndSurvivesProgressionAndRewardMutations() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        var tuning = registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID);
        PlayerProgress completed = PlayerProgress.fresh(tuning).completeEncounterLocator(LOCATOR);

        assertEquals(PlayerProgress.CURRENT_SCHEMA, completed.schemaVersion());
        assertTrue(completed.hasCompletedEncounterLocator(LOCATOR));
        assertSame(completed, completed.completeEncounterLocator(LOCATOR));

        PlayerProgress decoded = PlayerProgress.CODEC.parse(
                JsonOps.INSTANCE,
                PlayerProgress.CODEC.encodeStart(JsonOps.INSTANCE, completed).getOrThrow()).getOrThrow();
        assertEquals(completed, decoded);

        ProgressionService progression = new ProgressionService(registry, tuning);
        PlayerProgress funded = progression.grantCurrency(decoded, 500, 100);
        assertTrue(funded.hasCompletedEncounterLocator(LOCATOR));

        var table = registry.rewards().get(REWARD_TABLE);
        assertNotNull(table);
        RewardService.Applied applied = PlayerProgressStore.applyRewardToState(
                registry, funded, table, 20260913L, SECOND_LOCATOR);
        assertTrue(applied.state().hasCompletedEncounterLocator(LOCATOR));
        assertTrue(applied.state().hasCompletedEncounterLocator(SECOND_LOCATOR));
        assertEquals(PlayerProgress.CURRENT_SCHEMA, applied.state().schemaVersion());
    }

    @Test
    void anchorAndEncounterRepeatabilityBothParticipateInClearPolicy() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        var tuning = registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID);
        PlayerProgress completed = PlayerProgress.fresh(tuning).completeEncounterLocator(LOCATOR);

        assertTrue(WorldEncounterAnchorAccessPolicy.cleared(completed, resolved(false, true)));
        assertTrue(WorldEncounterAnchorAccessPolicy.cleared(completed, resolved(true, false)));
        assertFalse(WorldEncounterAnchorAccessPolicy.cleared(completed, resolved(true, true)));
        assertFalse(WorldEncounterAnchorAccessPolicy.repeatable(resolved(false, true)));
        assertTrue(WorldEncounterAnchorAccessPolicy.repeatable(resolved(true, true)));
    }

    @Test
    void authoredAnchorLaunchCapturesSourceMetadataForLaterSettlement() throws IOException {
        var parsed = ProductionDefinitionFixture.load();
        DefinitionRepository definitions = new DefinitionRepository();
        definitions.install(parsed.registry(), parsed.hash());
        BattleManager battles = new BattleManager();
        AuthoredEncounterLauncher launcher = new AuthoredEncounterLauncher(battles, definitions);
        var hero = parsed.registry().characters().get(HERO);
        CharacterProgress progress = new CharacterProgress(HERO, hero.originStar(), 6, 70);

        AuthoredEncounterLauncher.Launch launch = launcher.openVirtualFromAnchor(
                ENCOUNTER,
                UUID.randomUUID(),
                List.of(progress),
                UUID.randomUUID(),
                99173L,
                LOCATOR,
                false);

        assertTrue(launch.rewardContext().fromWorldAnchor());
        assertEquals(LOCATOR, launch.rewardContext().worldAnchorLocator());
        assertFalse(launch.rewardContext().worldAnchorRepeatable());
        assertEquals(ENCOUNTER, launch.encounter().id());
    }

    private static WorldEncounterAnchorResolver.Resolved resolved(boolean anchorRepeatable, boolean encounterRepeatable) {
        EncounterDefinition encounter = new EncounterDefinition(
                "turnbound_re:test_encounter",
                1,
                List.of(new EncounterDefinition.EnemySlot("turnbound_re:zombie", 1, 2)),
                REWARD_TABLE,
                "turnbound_re:test_scene",
                encounterRepeatable);
        RegionDefinition.EncounterAnchor anchor = new RegionDefinition.EncounterAnchor(
                "turnbound_re:test/anchor",
                encounter.id(),
                LOCATOR,
                anchorRepeatable);
        RegionDefinition region = new RegionDefinition(
                "turnbound_re:test_region",
                "REGION",
                "minecraft:overworld",
                List.of(),
                List.of(anchor));
        return new WorldEncounterAnchorResolver.Resolved(region, anchor, encounter);
    }
}

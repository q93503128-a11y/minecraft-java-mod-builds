package kr.moonseungjun.turnboundre.progression;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class M6EquipmentProgressionTest {
    private static final String ZOMBIE = "turnbound_re:zombie";
    private static final String SKELETON = "turnbound_re:skeleton";
    private static final String BULWARK = "turnbound_re:iron_bulwark";
    private static final String EDGE = "turnbound_re:copper_edge";

    @Test
    void forgeUpgradeAndEquipAreTransactionalAndPreserveProgression() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        EquipmentProgressionService service = new EquipmentProgressionService(registry);
        PlayerProgress state = fundedState(registry, 1000L, true);

        EquipmentProgressionService.Result forged = service.craft(state, BULWARK, 6);
        assertTrue(forged.accepted());
        assertEquals(940L, forged.state().coin());
        assertEquals(1, forged.state().equipment().get(BULWARK).level());
        assertEquals(new EquipmentProgressionService.MaterialCost("minecraft:iron_ingot", 6), forged.materialCost());
        assertEquals(state.essence(), forged.state().essence());
        assertEquals(state.shards(), forged.state().shards());
        assertEquals(state.completedEncounterLocators(), forged.state().completedEncounterLocators());

        EquipmentProgressionService.Result upgraded = service.upgrade(forged.state(), BULWARK, 10);
        assertTrue(upgraded.accepted());
        assertEquals(840L, upgraded.state().coin());
        assertEquals(2, upgraded.state().equipment().get(BULWARK).level());

        EquipmentProgressionService.Result equipped = service.equip(upgraded.state(), ZOMBIE, BULWARK);
        assertTrue(equipped.accepted());
        assertEquals(BULWARK, equipped.state().equippedEquipment().get(ZOMBIE));
        assertEquals(840L, equipped.state().coin());
    }

    @Test
    void rejectedForgeNeverMutatesStateAndReportsRequiredMaterial() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        EquipmentProgressionService service = new EquipmentProgressionService(registry);
        PlayerProgress state = fundedState(registry, 1000L, false);

        EquipmentProgressionService.Result result = service.craft(state, BULWARK, 5);

        assertEquals(EquipmentProgressionService.ResultCode.INSUFFICIENT_MATERIAL, result.code());
        assertSame(state, result.state());
        assertEquals(60L, result.coinCost());
        assertEquals(new EquipmentProgressionService.MaterialCost("minecraft:iron_ingot", 6), result.materialCost());
        assertTrue(state.equipment().isEmpty());
    }

    @Test
    void oneUniquePieceCannotBeEquippedByTwoCharacters() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        EquipmentProgressionService service = new EquipmentProgressionService(registry);
        PlayerProgress state = fundedState(registry, 1000L, true);
        state = service.craft(state, BULWARK, 6).state();
        state = service.equip(state, ZOMBIE, BULWARK).state();

        EquipmentProgressionService.Result collision = service.equip(state, SKELETON, BULWARK);
        assertEquals(EquipmentProgressionService.ResultCode.EQUIPMENT_IN_USE, collision.code());
        assertSame(state, collision.state());
    }

    @Test
    void switchingACharactersSlotIsFreeAndLeavesOldPieceOwned() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        EquipmentProgressionService service = new EquipmentProgressionService(registry);
        PlayerProgress state = fundedState(registry, 1000L, false);
        state = service.craft(state, BULWARK, 6).state();
        state = service.craft(state, EDGE, 6).state();
        state = service.equip(state, ZOMBIE, BULWARK).state();

        EquipmentProgressionService.Result switched = service.equip(state, ZOMBIE, EDGE);
        assertTrue(switched.accepted());
        assertEquals(EDGE, switched.state().equippedEquipment().get(ZOMBIE));
        assertTrue(switched.state().equipment().containsKey(BULWARK));
        assertTrue(switched.state().equipment().containsKey(EDGE));
        assertEquals(state.coin(), switched.state().coin());
    }

    @Test
    void equipmentBonusPreservesSpeedAndUsesDeterministicIntegerScaling() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        var base = new kr.moonseungjun.turnboundre.data.CharacterDefinition.Stats(101, 101, 101, 37, 101);
        var equipped = EquipmentRules.apply(registry, new EquipmentProgress(EDGE, 3), base);

        assertEquals(109, equipped.atk());
        assertEquals(101, equipped.hp());
        assertEquals(101, equipped.def());
        assertEquals(101, equipped.poise());
        assertEquals(37, equipped.spd());
    }

    @Test
    void schemaTwoLoadsWithoutEquipmentAndSchemaThreeRoundTripsEquipment() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        int capacity = registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID).partyCapacity();
        JsonObject legacy = new JsonObject();
        legacy.addProperty("schemaVersion", 2);
        legacy.addProperty("coin", 12L);
        legacy.addProperty("essence", 3L);
        legacy.addProperty("partyCapacity", capacity);

        PlayerProgress decodedLegacy = PlayerProgress.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        assertEquals(2, decodedLegacy.schemaVersion());
        assertTrue(decodedLegacy.equipment().isEmpty());
        assertTrue(decodedLegacy.equippedEquipment().isEmpty());

        PlayerProgress equipped = fundedState(registry, 1000L, false);
        EquipmentProgressionService service = new EquipmentProgressionService(registry);
        equipped = service.craft(equipped, BULWARK, 6).state();
        equipped = service.equip(equipped, ZOMBIE, BULWARK).state();
        PlayerProgress roundTrip = PlayerProgress.CODEC.parse(
                JsonOps.INSTANCE,
                PlayerProgress.CODEC.encodeStart(JsonOps.INSTANCE, equipped).getOrThrow()).getOrThrow();
        assertEquals(equipped, roundTrip);
        assertEquals(PlayerProgress.CURRENT_SCHEMA, roundTrip.schemaVersion());
    }

    private static PlayerProgress fundedState(DefinitionRegistry registry, long coin, boolean twoCharacters) {
        var zombie = registry.characters().get(ZOMBIE);
        var skeleton = registry.characters().get(SKELETON);
        Map<String, CharacterProgress> characters = twoCharacters
                ? Map.of(
                        ZOMBIE, new CharacterProgress(ZOMBIE, zombie.originStar(), zombie.originStar(), 1),
                        SKELETON, new CharacterProgress(SKELETON, skeleton.originStar(), skeleton.originStar(), 1))
                : Map.of(ZOMBIE, new CharacterProgress(ZOMBIE, zombie.originStar(), zombie.originStar(), 1));
        return new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA,
                coin,
                77L,
                Map.of(ZOMBIE, 9),
                characters,
                List.of(ZOMBIE),
                registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID).partyCapacity(),
                Set.of("turnbound_re:test/already_cleared"),
                Map.of(),
                Map.of());
    }
}

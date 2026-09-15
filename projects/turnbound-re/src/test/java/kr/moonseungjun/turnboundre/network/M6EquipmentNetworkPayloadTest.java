package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.EquipmentProgressionService;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgressStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class M6EquipmentNetworkPayloadTest {
    private static final String ZOMBIE = "turnbound_re:zombie";
    private static final String BULWARK = "turnbound_re:iron_bulwark";

    @Test
    void actionRoundTripPreservesServerRevalidationTokens() {
        EquipmentNetworkPayloads.ActionC2S payload = EquipmentNetworkPayloads.ActionC2S.of(
                "UPGRADE", ZOMBIE, BULWARK, 2, BULWARK);
        EquipmentNetworkPayloads.DecodedAction decoded = payload.decode();

        assertEquals("UPGRADE", decoded.operation());
        assertEquals(ZOMBIE, decoded.characterId());
        assertEquals(BULWARK, decoded.equipmentId());
        assertEquals(2, decoded.expectedLevel());
        assertEquals(BULWARK, decoded.expectedEquippedId());
    }

    @Test
    void serverProjectionCarriesActualMaterialBalanceForgeGateAndVisualItem() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        var zombie = registry.characters().get(ZOMBIE);
        PlayerProgress state = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA,
                500L,
                0L,
                Map.of(),
                Map.of(ZOMBIE, new CharacterProgress(ZOMBIE, zombie.originStar(), zombie.originStar(), 1)),
                List.of(ZOMBIE),
                registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID).partyCapacity(),
                Set.of(), Map.of(), Map.of());
        state = new EquipmentProgressionService(registry).craft(state, BULWARK, 6).state();
        state = new EquipmentProgressionService(registry).equip(state, ZOMBIE, BULWARK).state();

        EquipmentNetworkPayloads.SnapshotS2C payload = EquipmentNetworkPayloads.SnapshotS2C.from(
                state, registry, Map.of("minecraft:iron_ingot", 9), true, "ACCEPTED", "level=1");
        EquipmentNetworkPayloads.Snapshot decoded = payload.decode();
        EquipmentNetworkPayloads.EquipmentView view = decoded.equipment(BULWARK).orElseThrow();

        assertTrue(decoded.forgeAvailable());
        assertEquals("minecraft:shield", view.visualItem());
        assertEquals(9, view.materialOwned());
        assertEquals(1, view.level());
        assertEquals(ZOMBIE, view.equippedCharacterId());
        assertEquals("UPGRADE", view.action());
        assertEquals(100L, view.nextCoinCost());
        assertEquals(10, view.nextMaterialCount());
        assertEquals("INSUFFICIENT_MATERIAL", view.blockCode());
        assertEquals(4, view.currentBonus().defPercent());
        assertEquals(6, view.nextBonus().defPercent());
        assertEquals("ACCEPTED", decoded.resultCode());
    }

    @Test
    void legacyTwelveFieldEquipmentViewWireFallsBackToIngredientVisual() {
        var legacyView = new EquipmentNetworkPayloads.EquipmentView(
                BULWARK, "minecraft:iron_ingot", 1, 3, ZOMBIE, 6,
                new EquipmentNetworkPayloads.BonusView(0, 0, 4, 4),
                100L, 10, new EquipmentNetworkPayloads.BonusView(0, 0, 6, 6),
                "UPGRADE", "");
        assertEquals("minecraft:iron_ingot", legacyView.visualItem());
    }

    @Test
    void noPhysicalForgeBlocksCraftEvenWhenCoinAndMaterialAreEnough() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        PlayerProgress state = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA, 500L, 0L, Map.of(), Map.of(), List.of(),
                registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID).partyCapacity(),
                Set.of(), Map.of(), Map.of());

        EquipmentNetworkPayloads.Snapshot snapshot = EquipmentNetworkPayloads.SnapshotS2C.from(
                state, registry, Map.of(
                        "minecraft:iron_ingot", 64,
                        "minecraft:copper_ingot", 64,
                        "minecraft:gold_ingot", 64),
                false, "", "").decode();

        assertTrue(snapshot.equipment().stream().allMatch(view -> "FORGE_UNAVAILABLE".equals(view.blockCode())));
        assertTrue(snapshot.equipment().stream().noneMatch(EquipmentNetworkPayloads.EquipmentView::canForge));
        assertEquals("minecraft:copper_sword", snapshot.equipment("turnbound_re:copper_edge").orElseThrow().visualItem());
        assertEquals("minecraft:golden_apple", snapshot.equipment("turnbound_re:golden_heart").orElseThrow().visualItem());
    }
}

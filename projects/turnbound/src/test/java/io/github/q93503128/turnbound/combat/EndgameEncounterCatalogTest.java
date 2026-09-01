package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndgameEncounterCatalogTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() { CampaignProgressStore.removeRuntime(playerId); }

    @Test
    void hardBossUsesCanonicalMultipliersAndStoryClearGate() {
        CampaignProgressStore.ensureNewGame(playerId);
        assertFalse(EndgameEncounterCatalog.unlocked(playerId, "HARD_B01"));
        addClear("BATTLE_B01");
        assertTrue(EndgameEncounterCatalog.unlocked(playerId, "HARD_B01"));

        CombatantState boss = EndgameEncounterCatalog.createBattle(playerId, "HARD_B01").living(CombatantSide.ENEMY).getFirst();
        BattleStats normal = CanonicalData.definition("B01", 6, 0, false).stats();
        assertEquals((int)Math.floor(normal.maxHp() * 1.65), boss.maxHp());
        assertEquals((int)Math.floor(normal.attack() * 1.25), boss.definition().stats().attack());
        assertEquals((int)Math.floor(normal.defense() * 1.15), boss.definition().stats().defense());
        assertEquals(normal.speed() + 8, boss.definition().stats().speed());
        assertEquals(11, boss.definition().intParam("level", -1));
    }

    @Test
    void riftUsesExactFloorDataAndB05Unlock() {
        CampaignProgressStore.ensureNewGame(playerId);
        assertFalse(EndgameEncounterCatalog.unlocked(playerId, "RIFT_F01"));
        addClear("BATTLE_B05");
        assertTrue(EndgameEncounterCatalog.unlocked(playerId, "RIFT_F01"));

        BattleState floor1 = EndgameEncounterCatalog.createBattle(playerId, "RIFT_F01");
        assertEquals(V04Catalogs.riftFloor(1).enemies(), floor1.living(CombatantSide.ENEMY).stream()
                .map(unit -> unit.definition().id()).toList());
        assertEquals(20.0, floor1.living(CombatantSide.ENEMY).getFirst().definition().param("level", -1), 0.0);
    }

    @Test
    void riftBossFloorAppliesHardPatternThenFloorHpAdjustment() {
        CampaignProgressStore.ensureNewGame(playerId);
        addClear("BATTLE_B05");
        CombatantState boss = EndgameEncounterCatalog.createBattle(playerId, "RIFT_F30").living(CombatantSide.ENEMY).getFirst();
        BattleStats normal = CanonicalData.definition("B05", 20, 0, false).stats();
        double floorAdjustment = 1.0 + 0.02 * (60 - 20);
        assertEquals((int)Math.floor(normal.maxHp() * 1.65 * floorAdjustment), boss.maxHp());
        // Canon §132: the Rift table level overrides the enemy level; HardPattern does not add Hard-rematch +5.
        assertEquals(60, boss.definition().intParam("level", -1));
        assertTrue(boss.definition().param("hardBoss", 0.0) > 0.0);
    }

    @Test
    void endgameControlPermissionsAreServerCanonical() {
        for (int boss = 1; boss <= 5; boss++) {
            String id = EndgameEncounterCatalog.hardId("B0" + boss);
            assertFalse(EndgameEncounterCatalog.autoAllowed(id), id + " must reject Auto");
            assertFalse(EndgameEncounterCatalog.speedAllowed(id), id + " must reject x2");
            assertFalse(EndgameEncounterCatalog.fleeAllowed(id), id + " must reject flee");
        }
        for (int floor = 1; floor <= 30; floor++) {
            String id = EndgameEncounterCatalog.riftId(floor);
            assertTrue(EndgameEncounterCatalog.autoAllowed(id), id + " must allow Auto");
            assertTrue(EndgameEncounterCatalog.speedAllowed(id), id + " must allow x2");
            assertFalse(EndgameEncounterCatalog.fleeAllowed(id), id + " must reject flee");
        }
    }

    private void addClear(String clearId) {
        CampaignProgressStore.Snapshot old = CampaignProgressStore.snapshot(playerId);
        Set<String> clears = new LinkedHashSet<>(old.clearedEncounters());
        clears.add(clearId);
        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                old.profile(), old.characters(), old.growth(), old.equipment(), old.quests(), clears,
                old.orphanedCharacterIds(), old.orphanedEquipmentIds()));
    }
}

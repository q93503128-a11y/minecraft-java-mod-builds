package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class PlayerProgressionStateTest {
    @Test
    void initialProgressionHasNoInventedClassOrWeaponBuild() {
        var state = PlayerProgressionState.initial();

        assertEquals(1, state.combatLevel());
        assertEquals(0L, state.combatXp());
        assertFalse(state.activeClass().isPresent());
        assertEquals(1, state.classProgress(RootClass.MAGE).rank());
        assertEquals(0L, state.classProgress(RootClass.MAGE).classXp());
        assertEquals(0, state.allocation(RootClass.MAGE).spentPoints());
        assertFalse(state.buildWith(
                EquipmentCombatState.weaponOnly(ProjectWeaponFamily.STAFF, 1)
        ).isPresent());
    }

    @Test
    void perClassAllocationAndProgressSurviveCodecRoundTrip() {
        var original = PlayerProgressionState.initial()
                .withCombatLevel(8)
                .withAllocation(
                        RootClass.MAGE,
                        new AttributeAllocation(0, 0, 0, 0, 7, 0)
                )
                .withActiveClass(RootClass.MAGE)
                .grantCombatXpOnce("openworld_rpg:test/combat", 300)
                .grantClassXpOnce("openworld_rpg:test/class", RootClass.MAGE, 300);

        var encoded = PlayerProgressionState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerProgressionState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
        assertEquals(7, decoded.allocation(RootClass.MAGE).intel());
        assertEquals(0, decoded.allocation(RootClass.WARRIOR).spentPoints());
        assertEquals(3, decoded.classProgress(RootClass.MAGE).rank());
        assertEquals(30L, decoded.classProgress(RootClass.MAGE).classXp());
    }

    @Test
    void oldCombatProgressionPayloadDefaultsNewXpFieldsWithoutInventingProgress() {
        var oldPayload = JsonParser.parseString("""
                {
                  "combat_level": 8,
                  "active_class": "mage",
                  "warrior": {"vit":0,"end":0,"str":0,"dex":0,"int":0,"wil":0},
                  "hunter": {"vit":0,"end":0,"str":0,"dex":0,"int":0,"wil":0},
                  "cleric": {"vit":0,"end":0,"str":0,"dex":0,"int":0,"wil":0},
                  "mage": {"vit":0,"end":0,"str":0,"dex":0,"int":7,"wil":0},
                  "guardian": {"vit":0,"end":0,"str":0,"dex":0,"int":0,"wil":0}
                }
                """);

        var decoded = PlayerProgressionState.CODEC.parse(JsonOps.INSTANCE, oldPayload)
                .getOrThrow();

        assertEquals(8, decoded.combatLevel());
        assertEquals(0L, decoded.combatXp());
        assertEquals(1, decoded.classProgress(RootClass.MAGE).rank());
        assertEquals(0L, decoded.classProgress(RootClass.MAGE).classXp());
        assertTrue(decoded.appliedCombatXpTransactionIds().isEmpty());
        assertTrue(decoded.appliedClassXpTransactionIds().isEmpty());
    }

    @Test
    void combatXpLevelsAcrossMultipleThresholdsAndStopsAtCap() {
        var state = PlayerProgressionState.initial()
                .grantCombatXpOnce("openworld_rpg:test/multi_level", 500);

        assertEquals(3, state.combatLevel());
        assertEquals(130L, state.combatXp());

        var capped = PlayerProgressionState.initial()
                .grantCombatXpOnce("openworld_rpg:test/cap", 10_000_000L);

        assertEquals(80, capped.combatLevel());
        assertEquals(0L, capped.combatXp());
    }

    @Test
    void oneTimeXpTransactionsAreIdempotentPerRewardDomain() {
        var combat = PlayerProgressionState.initial()
                .grantCombatXpOnce("openworld_rpg:r01/dust/combat_xp", 105);
        var combatRetry = combat.grantCombatXpOnce(
                "openworld_rpg:r01/dust/combat_xp",
                105
        );

        var clazz = combat
                .grantClassXpOnce(
                        "openworld_rpg:r01/dust/class_xp",
                        RootClass.HUNTER,
                        60
                );
        var classRetry = clazz.grantClassXpOnce(
                "openworld_rpg:r01/dust/class_xp",
                RootClass.HUNTER,
                60
        );

        assertSame(combat, combatRetry);
        assertSame(clazz, classRetry);
        assertEquals(105L, clazz.combatXp());
        assertEquals(60L, clazz.classProgress(RootClass.HUNTER).classXp());
    }

    @Test
    void repeatableReceiptCleanupKeepsEarnedProgress() {
        var combat = PlayerProgressionState.initial()
                .grantCombatXpOnce(
                        "openworld_rpg:r01/roadside_trouble/reward/2/combat_xp",
                        50
                )
                .grantClassXpOnce(
                        "openworld_rpg:r01/roadside_trouble/reward/2/class_xp",
                        RootClass.WARRIOR,
                        40
                );

        var cleaned = combat
                .forgetCombatXpTransaction(
                        "openworld_rpg:r01/roadside_trouble/reward/2/combat_xp"
                )
                .forgetClassXpTransaction(
                        "openworld_rpg:r01/roadside_trouble/reward/2/class_xp"
                );

        assertEquals(50L, cleaned.combatXp());
        assertEquals(40L, cleaned.classProgress(RootClass.WARRIOR).classXp());
        assertTrue(cleaned.appliedCombatXpTransactionIds().isEmpty());
        assertTrue(cleaned.appliedClassXpTransactionIds().isEmpty());
    }

    @Test
    void loweringLevelBelowExistingAllocationFailsClosed() {
        var state = PlayerProgressionState.initial()
                .withCombatLevel(8)
                .withAllocation(
                        RootClass.MAGE,
                        new AttributeAllocation(0, 0, 0, 0, 7, 0)
                );

        assertThrows(IllegalArgumentException.class, () -> state.withCombatLevel(7));
    }
}

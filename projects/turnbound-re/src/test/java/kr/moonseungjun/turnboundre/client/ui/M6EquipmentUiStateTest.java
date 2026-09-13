package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.network.EquipmentNetworkPayloads;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class M6EquipmentUiStateTest {
    private static final EquipmentNetworkPayloads.BonusView ZERO = EquipmentNetworkPayloads.BonusView.ZERO;

    @Test
    void equippedPieceWinsSelectionForSelectedCharacter() {
        var snapshot = snapshot(
                view("turnbound_re:copper_edge", 2, "turnbound_re:char_a"),
                view("turnbound_re:golden_heart", 1, ""));

        assertEquals("turnbound_re:copper_edge",
                EquipmentUiState.selectionForCharacter(snapshot, "turnbound_re:golden_heart", "turnbound_re:char_a"));
        assertEquals("turnbound_re:copper_edge",
                EquipmentUiState.equippedToCharacter(snapshot, "turnbound_re:char_a"));
    }

    @Test
    void freeOwnedPieceCanEquipButPieceUsedByAnotherCharacterCannot() {
        var free = view("turnbound_re:golden_heart", 1, "");
        var used = view("turnbound_re:copper_edge", 1, "turnbound_re:char_b");

        assertTrue(EquipmentUiState.canEquip(free, "turnbound_re:char_a", true));
        assertFalse(EquipmentUiState.canEquip(used, "turnbound_re:char_a", true));
        assertFalse(EquipmentUiState.canEquip(free, "turnbound_re:char_a", false));
    }

    @Test
    void selectedCharactersOwnPieceProducesUnequipIntent() {
        var own = view("turnbound_re:iron_bulwark", 3, "turnbound_re:char_a");
        var free = view("turnbound_re:golden_heart", 1, "");

        assertEquals("UNEQUIP", EquipmentUiState.equipOperation(own, "turnbound_re:char_a"));
        assertEquals("EQUIP", EquipmentUiState.equipOperation(free, "turnbound_re:char_a"));
    }

    private static EquipmentNetworkPayloads.Snapshot snapshot(EquipmentNetworkPayloads.EquipmentView... equipment) {
        return new EquipmentNetworkPayloads.Snapshot(true, List.of(equipment), "", "");
    }

    private static EquipmentNetworkPayloads.EquipmentView view(String id, int level, String equippedCharacter) {
        return new EquipmentNetworkPayloads.EquipmentView(
                id, "minecraft:iron_ingot", level, 3, equippedCharacter, 64,
                ZERO, 100, 8, ZERO, level == 0 ? "CRAFT" : level >= 3 ? "MAX" : "UPGRADE", "");
    }
}

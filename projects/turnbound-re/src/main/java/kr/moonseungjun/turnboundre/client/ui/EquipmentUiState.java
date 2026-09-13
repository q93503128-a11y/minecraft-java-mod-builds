package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.network.EquipmentNetworkPayloads;

/** Pure selection/action rules for the one-slot equipment panel. */
public final class EquipmentUiState {
    private EquipmentUiState() {}

    public static String equippedToCharacter(EquipmentNetworkPayloads.Snapshot snapshot, String characterId) {
        if (snapshot == null || characterId == null || characterId.isBlank()) return "";
        return snapshot.equipment().stream()
                .filter(view -> characterId.equals(view.equippedCharacterId()))
                .map(EquipmentNetworkPayloads.EquipmentView::id)
                .findFirst()
                .orElse("");
    }

    public static String selectionForCharacter(
            EquipmentNetworkPayloads.Snapshot snapshot,
            String currentSelection,
            String characterId
    ) {
        if (snapshot == null || snapshot.equipment().isEmpty()) return "";
        String equipped = equippedToCharacter(snapshot, characterId);
        if (!equipped.isBlank()) return equipped;
        if (currentSelection != null && snapshot.equipment(currentSelection).isPresent()) return currentSelection;
        return snapshot.equipment().getFirst().id();
    }

    public static boolean canEquip(EquipmentNetworkPayloads.EquipmentView view, String characterId, boolean characterOwned) {
        if (view == null || !view.owned() || !characterOwned || characterId == null || characterId.isBlank()) return false;
        return view.equippedCharacterId().isBlank() || characterId.equals(view.equippedCharacterId());
    }

    public static String equipOperation(EquipmentNetworkPayloads.EquipmentView view, String characterId) {
        if (view == null || characterId == null || characterId.isBlank()) return "";
        return characterId.equals(view.equippedCharacterId()) ? "UNEQUIP" : "EQUIP";
    }
}

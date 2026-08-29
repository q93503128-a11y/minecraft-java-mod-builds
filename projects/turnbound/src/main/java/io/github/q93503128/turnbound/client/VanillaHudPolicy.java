package io.github.q93503128.turnbound.client;

import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Removes the vanilla survival/player shell HUD and keeps battle presentation free of hotbar/crosshair overlap. */
public final class VanillaHudPolicy {
    private VanillaHudPolicy() {}

    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        var name = event.getName();
        boolean survivalShell = name.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || name.equals(VanillaGuiLayers.FOOD_LEVEL)
                || name.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || name.equals(VanillaGuiLayers.AIR_LEVEL)
                || name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || name.equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND)
                || name.equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR)
                || name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME);
        boolean battleOnly = ClientBattleState.snapshot().active()
                && (name.equals(VanillaGuiLayers.HOTBAR) || name.equals(VanillaGuiLayers.CROSSHAIR));
        if (survivalShell || battleOnly) event.setCanceled(true);
    }
}
package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Removes the vanilla survival/player shell so TURNBOUND reads as its own RPG rather than survival Minecraft. */
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

        boolean rpgSession = ClientFieldState.snapshot().active() || ClientBattleState.snapshot().active();
        boolean hotbar = rpgSession && name.equals(VanillaGuiLayers.HOTBAR);
        // Field exploration keeps a crosshair for entity/facility interaction. Battle targeting owns its own cursor.
        boolean battleCrosshair = ClientBattleState.snapshot().active() && name.equals(VanillaGuiLayers.CROSSHAIR);
        if (survivalShell || hotbar || battleCrosshair) event.setCanceled(true);
    }
}

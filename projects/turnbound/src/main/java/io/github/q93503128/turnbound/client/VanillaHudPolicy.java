package io.github.q93503128.turnbound.client;

import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Hides survival-only HUD elements that are not part of TURNBOUND's RPG rules. */
public final class VanillaHudPolicy {
    private VanillaHudPolicy() {
    }

    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
            event.setCanceled(true);
        }
    }
}

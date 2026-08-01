package kr.moonseungjun.livingkingdoms.client;

import net.neoforged.neoforge.client.event.ScreenEvent;

/** Mouse input bridge for the draggable atlas without relying on unstable Screen method mappings. */
public final class RealmCodexInteractionClient {
    private RealmCodexInteractionClient() {
    }

    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof RealmCodexScreenV3 screen)) return;
        if (screen.handleMapDrag(event.getMouseX(), event.getMouseY(), event.getMouseButton(),
                event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof RealmCodexScreenV3 screen)) return;
        if (screen.handleMapScroll(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }
}

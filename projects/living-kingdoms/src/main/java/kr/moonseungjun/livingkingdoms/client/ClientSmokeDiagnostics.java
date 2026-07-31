package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * CI-only graphical smoke test. It is completely dormant in normal launches.
 */
final class ClientSmokeDiagnostics {
    private static final boolean ENABLED = "1".equals(System.getenv("LIVING_KINGDOMS_CI_CLIENT_TEST"));
    private static final int OPEN_AFTER_TICKS = 20;
    private static final int PASS_AFTER_TICKS = 100;

    private static int ticks;
    private static OriginSelectionScreen diagnosticScreen;

    private ClientSmokeDiagnostics() {
    }

    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }

        ticks++;
        Minecraft minecraft = Minecraft.getInstance();

        if (diagnosticScreen == null && ticks >= OPEN_AFTER_TICKS) {
            diagnosticScreen = new OriginSelectionScreen(1);
            minecraft.gui.setScreen(diagnosticScreen);
            LivingKingdoms.LOGGER.info("LK_CLIENT_DIAGNOSTIC_SCREEN_OPENED");
            return;
        }

        if (diagnosticScreen != null && ticks >= PASS_AFTER_TICKS) {
            LivingKingdoms.LOGGER.info("LK_CLIENT_DIAGNOSTIC_PASS screen=origin_selection rendered_window=true");
            System.exit(0);
        }
    }
}

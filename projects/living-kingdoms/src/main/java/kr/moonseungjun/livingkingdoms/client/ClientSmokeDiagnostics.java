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
    private static ResponsiveOriginSelectionScreen diagnosticScreen;

    private ClientSmokeDiagnostics() {
    }

    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }

        ticks++;
        Minecraft minecraft = Minecraft.getInstance();

        if (diagnosticScreen == null && ticks >= OPEN_AFTER_TICKS) {
            diagnosticScreen = new ResponsiveOriginSelectionScreen(1);
            minecraft.gui.setScreen(diagnosticScreen);
            LivingKingdoms.LOGGER.info("LK_CLIENT_DIAGNOSTIC_SCREEN_OPENED responsive=true");
            return;
        }

        if (diagnosticScreen != null && ticks >= PASS_AFTER_TICKS) {
            if (!diagnosticScreen.allRequiredControlsFit()) {
                throw new IllegalStateException("Responsive origin controls extend outside the current client viewport");
            }
            LivingKingdoms.LOGGER.info(
                    "LK_CLIENT_DIAGNOSTIC_PASS screen=responsive_origin_selection rendered_window=true viewport={}x{} controls_fit=true",
                    diagnosticScreen.width,
                    diagnosticScreen.height
            );
            System.exit(0);
        }
    }
}

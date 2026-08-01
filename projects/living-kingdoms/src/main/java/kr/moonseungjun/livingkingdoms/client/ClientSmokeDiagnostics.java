package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** CI-only graphical smoke test. It is completely dormant in normal launches. */
final class ClientSmokeDiagnostics {
    private static final boolean ENABLED = "1".equals(System.getenv("LIVING_KINGDOMS_CI_CLIENT_TEST"));
    private static final int OPEN_AFTER_TICKS = 20;
    private static final int PASS_AFTER_TICKS = 100;

    private static int ticks;
    private static ResponsiveOriginSelectionScreen diagnosticScreen;
    private static RealmLoadingScreen loadingScreen;

    private ClientSmokeDiagnostics() {
    }

    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;

        ticks++;
        Minecraft minecraft = Minecraft.getInstance();

        if (diagnosticScreen == null && ticks >= OPEN_AFTER_TICKS) {
            diagnosticScreen = new ResponsiveOriginSelectionScreen(1);
            minecraft.gui.setScreen(diagnosticScreen);
            LivingKingdoms.LOGGER.info("LK_CLIENT_DIAGNOSTIC_SCREEN_OPENED responsive=true");
            return;
        }

        if (ticks == 97) {
            loadingScreen = new RealmLoadingScreen("도로와 건물을 구역별로 배치하고 있습니다.");
            loadingScreen.update(new RealmBuildProgressPayload(
                    "erden_kingdom", "building", 64,
                    "도로와 건물을 구역별로 배치하고 있습니다.", false, false
            ));
            minecraft.gui.setScreen(loadingScreen);
        }
        if (ticks == 99) {
            if (loadingScreen == null || !loadingScreen.allRequiredControlsFit()) {
                throw new IllegalStateException("Realm loading screen extends outside the current client viewport");
            }
            LivingKingdoms.LOGGER.info(
                    "LK_CLIENT_LOADING_DIAGNOSTIC_PASS screen=realm_loading non_pausing=true viewport={}x{} controls_fit=true",
                    loadingScreen.width, loadingScreen.height
            );
        }

        if (diagnosticScreen != null && ticks >= PASS_AFTER_TICKS) {
            if (!diagnosticScreen.allRequiredControlsFit()) {
                throw new IllegalStateException("Responsive origin controls extend outside the current client viewport");
            }
            LivingKingdoms.LOGGER.info(
                    "LK_CLIENT_DIAGNOSTIC_PASS screen=origin_selection rendered_window=true responsive=true viewport={}x{} controls_fit=true",
                    diagnosticScreen.width,
                    diagnosticScreen.height
            );
            System.exit(0);
        }
    }
}

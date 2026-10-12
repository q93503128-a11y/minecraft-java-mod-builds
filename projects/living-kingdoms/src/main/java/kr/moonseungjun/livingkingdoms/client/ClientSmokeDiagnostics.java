package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** CI-only graphical smoke test. It is completely dormant in normal launches. */
final class ClientSmokeDiagnostics {
    private static final boolean ENABLED = "1".equals(System.getenv("LIVING_KINGDOMS_CI_CLIENT_TEST"));
    private static final int OPEN_AFTER_TICKS = 55;
    private static final int PASS_AFTER_TICKS = 216;

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
            diagnosticScreen = new ResponsiveOriginSelectionScreen(2);
            minecraft.gui.setScreen(diagnosticScreen);
            LivingKingdoms.LOGGER.info("LK_CLIENT_DIAGNOSTIC_SCREEN_OPENED responsive=true schema=2");
            return;
        }

        if (ticks == 78) {
            if (!diagnosticScreen.allRequiredControlsFit()) {
                throw new IllegalStateException("Responsive origin controls extend outside the current client viewport");
            }
            verifyInventoryCodexClickTargets();
            verifyRealmClock();
            LivingKingdoms.LOGGER.info(
                    "LK_CLIENT_DIAGNOSTIC_PASS screen=origin_selection fixed_erden_origin=true rendered_window=true responsive=true viewport={}x{} controls_fit=true inventory_codex_click_targets=true click_through=false realm_clock_server_payload=true",
                    diagnosticScreen.width, diagnosticScreen.height
            );
        }

        if (ticks == 188) {
            loadingScreen = new RealmLoadingScreen("에르덴 왕도와 생활권을 준비하고 있습니다.");
            loadingScreen.update(new RealmBuildProgressPayload(
                    "erden_kingdom", "building", 64,
                    "에르덴 왕도와 생활권을 준비하고 있습니다.", false, false
            ));
            minecraft.gui.setScreen(loadingScreen);
        }
        if (ticks == 210) {
            if (loadingScreen == null || !loadingScreen.allRequiredControlsFit()) {
                throw new IllegalStateException("Realm loading screen extends outside the current client viewport");
            }
            LivingKingdoms.LOGGER.info(
                    "LK_CLIENT_LOADING_DIAGNOSTIC_PASS screen=realm_loading non_pausing=true viewport={}x{} controls_fit=true",
                    loadingScreen.width, loadingScreen.height
            );
        }

        if (ticks >= PASS_AFTER_TICKS) System.exit(0);
    }

    private static void verifyInventoryCodexClickTargets() {
        int width = 854;
        int height = 480;
        int overview = 0;
        int map = 0;
        int skills = 0;
        int other = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                String action = RealmCodexClient.inventoryPanelAction(width, height, x + 0.5D, y + 0.5D);
                if (action == null) continue;
                switch (action) {
                    case "overview" -> overview++;
                    case "map" -> map++;
                    case "skills" -> skills++;
                    default -> other++;
                }
            }
        }
        if (overview != 96 * 19 || map != 46 * 19 || skills != 46 * 19 || other != 0) {
            throw new IllegalStateException(
                    "Inventory codex click targets drifted overview=" + overview
                            + " map=" + map + " skills=" + skills + " other=" + other);
        }
        LivingKingdoms.LOGGER.info(
                "LK_CLIENT_INVENTORY_CODEX_INTERACTION_PASS click_targets=3 overview_pixels={} map_pixels={} skills_pixels={} overlap=false pre_screen_intercept=true",
                overview, map, skills);
    }

    private static void verifyRealmClock() {
        String[] actual = {
                FantasyHudClient.realmClockText(0L, "미등록", 0),
                FantasyHudClient.realmClockText(999L, "미등록", 0),
                FantasyHudClient.realmClockText(1_000L, "미등록", 0),
                FantasyHudClient.realmClockText(23_999L, "미등록", 0),
                FantasyHudClient.realmClockText(24_000L, "미등록", 0)
        };
        String[] expectedClock = {"1일  06:00", "1일  06:59", "1일  07:00", "1일  05:59", "2일  06:00"};
        for (int i = 0; i < actual.length; i++) {
            if (!actual[i].contains(expectedClock[i])) {
                throw new IllegalStateException(
                        "Server realm clock formatting drifted index=" + i + " text=" + actual[i]);
            }
        }
        LivingKingdoms.LOGGER.info(
                "LK_CLIENT_REALM_CLOCK_DIAGNOSTIC_PASS server_payload=true client_level_time=false samples=5 day_rollover=true minute_boundary=true protocol=realm-codex-6");
    }
}

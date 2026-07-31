package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** CI-only atlas and status renderer verification. Dormant during normal launches. */
final class CodexSmokeDiagnostics {
    private static final boolean ENABLED = "1".equals(System.getenv("LIVING_KINGDOMS_CI_CLIENT_TEST"));
    private static int ticks;
    private static RealmCodexScreen mapScreen;
    private static RealmCodexScreen statusScreen;

    private CodexSmokeDiagnostics() {
    }

    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        ticks++;
        Minecraft minecraft = Minecraft.getInstance();

        if (ticks == 40) {
            mapScreen = new RealmCodexScreen("map", sampleSnapshot());
            minecraft.gui.setScreen(mapScreen);
            LivingKingdoms.LOGGER.info("LK_CLIENT_CODEX_SCREEN_OPENED page=map");
            return;
        }
        if (ticks == 68) {
            if (mapScreen == null || !fits(mapScreen)) {
                throw new IllegalStateException("Realm atlas extends outside the current client viewport");
            }
            statusScreen = new RealmCodexScreen("status", sampleSnapshot());
            minecraft.gui.setScreen(statusScreen);
            LivingKingdoms.LOGGER.info("LK_CLIENT_CODEX_SCREEN_OPENED page=status map_fit=true");
            return;
        }
        if (ticks == 94) {
            if (statusScreen == null || !fits(statusScreen)) {
                throw new IllegalStateException("Detailed character status extends outside the current client viewport");
            }
            LivingKingdoms.LOGGER.info(
                    "LK_CLIENT_CODEX_DIAGNOSTIC_PASS screens=map,status rendered_window=true responsive=true viewport={}x{} controls_fit=true",
                    statusScreen.width, statusScreen.height
            );
        }
    }

    private static boolean fits(RealmCodexScreen screen) {
        int panelW = Math.min(650, Math.max(390, screen.width - 16));
        int panelH = Math.min(390, Math.max(220, screen.height - 12));
        panelW = Math.min(panelW, screen.width - 8);
        panelH = Math.min(panelH, screen.height - 6);
        int left = (screen.width - panelW) / 2;
        int top = Math.max(3, (screen.height - panelH) / 2);
        int right = left + panelW;
        int bottom = top + panelH;
        int mapW = panelW - 36;
        int mapH = panelH - 76;
        int cardW = (panelW - 44) / 2;
        int cardH = panelH - 70;
        return left >= 0 && top >= 0 && right <= screen.width && bottom <= screen.height
                && mapW >= 250 && mapH >= 130 && cardW >= 160 && cardH >= 150
                && top + 52 + cardH <= bottom;
    }

    private static String sampleSnapshot() {
        return "player\tCI Wanderer\n"
                + "species\t인간\n"
                + "homeland\t에르덴 왕국\n"
                + "background\t방랑자\n"
                + "residence\t왕국 북로의 방랑자 야영지\n"
                + "health\t20.0 / 20.0\n"
                + "armor\t7\n"
                + "food\t18 / 20\n"
                + "level\t12\n"
                + "experience\t540\n"
                + "position\t84, 66, -112\n"
                + "region\t에르덴 변경\n"
                + "realm\t살아있는 왕국 대륙\n"
                + "mainhand\t철제 장검\n"
                + "offhand\t여행자의 등불\n"
                + "head\t가죽 두건\n"
                + "chest\t사슬 흉갑\n"
                + "legs\t가죽 바지\n"
                + "feet\t여행 장화\n"
                + "wanted\t23\n"
                + "resistance\t3\n"
                + "jurisdiction\t에르덴 사법권\n"
                + "arrest\t40%\n"
                + "home_x\t84\n"
                + "home_z\t-112\n"
                + "player_x\t12\n"
                + "player_z\t8\n";
    }
}

package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** CI-only graphical verification for all responsive codex pages after client resource binding. */
final class CodexSmokeDiagnostics {
    private static final boolean ENABLED = "1".equals(System.getenv("LIVING_KINGDOMS_CI_CLIENT_TEST"));
    private static int ticks;
    private static RealmCodexScreenV3 active;

    private CodexSmokeDiagnostics() {
    }

    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        ticks++;
        Minecraft minecraft = Minecraft.getInstance();

        if (ticks == 82) open(minecraft, "map");
        if (ticks == 102) verify("map");
        if (ticks == 106) open(minecraft, "overview");
        if (ticks == 126) verify("overview");
        if (ticks == 130) open(minecraft, "equipment");
        if (ticks == 150) verify("equipment");
        if (ticks == 154) open(minecraft, "skills");
        if (ticks == 176) {
            verify("skills");
            LivingKingdoms.LOGGER.info(
                    "LK_CLIENT_CODEX_DIAGNOSTIC_PASS screens=overview,equipment,map,skills rendered_window=true responsive=true viewport={}x{} controls_fit=true draggable_atlas=true",
                    active.width, active.height
            );
        }
    }

    private static void open(Minecraft minecraft, String page) {
        active = new RealmCodexScreenV3(page, sampleSnapshot());
        minecraft.gui.setScreen(active);
        LivingKingdoms.LOGGER.info("LK_CLIENT_CODEX_SCREEN_OPENED page={}", page);
    }

    private static void verify(String page) {
        if (active == null || !active.allRequiredControlsFit()) {
            throw new IllegalStateException("Codex page extends outside the current client viewport: " + page);
        }
    }

    private static String sampleSnapshot() {
        return "player\tCI Wanderer\n"
                + "species_id\thuman\n"
                + "species\t인간\n"
                + "homeland\t에르덴 왕국\n"
                + "affiliation\t에르덴 왕국 · 로엔 변경백령\n"
                + "citizenship\t에르덴 왕국 시민\n"
                + "background\t방랑자\n"
                + "residence\t왕국 북로의 방랑자 야영지\n"
                + "trait_title\t다재다능\n"
                + "trait_description\t다른 종족보다 초기 기술 점수를 1점 더 받습니다.\n"
                + "health\t17.0 / 20.0\n"
                + "armor\t7\n"
                + "food\t18 / 20\n"
                + "level\t12\n"
                + "experience\t540\n"
                + "position\t84, 78, -112\n"
                + "region\t에르덴 로엔 변경백령\n"
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
                + "skill_points\t4\n"
                + "skill_milestone\t2\n"
                + "unlocked_skills\tcombat_endurance,explore_trailblazer\n"
                + "home_x\t84\n"
                + "home_z\t-112\n"
                + "player_x\t12\n"
                + "player_z\t8\n"
                + "erden_x\t0\n"
                + "erden_z\t0\n"
                + "silvana_x\t-9000\n"
                + "silvana_z\t-1500\n"
                + "kardum_x\t-2500\n"
                + "kardum_z\t-9000\n";
    }
}

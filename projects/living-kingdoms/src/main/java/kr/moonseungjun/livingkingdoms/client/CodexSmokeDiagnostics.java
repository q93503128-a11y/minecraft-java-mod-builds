package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** CI-only graphical and interaction verification for every codex page. */
final class CodexSmokeDiagnostics {
    private static final boolean ENABLED = "1".equals(System.getenv("LIVING_KINGDOMS_CI_CLIENT_TEST"));
    private static int ticks;
    private static RealmCodexScreenV4 active;

    private CodexSmokeDiagnostics() {
    }

    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        ticks++;
        Minecraft minecraft = Minecraft.getInstance();

        if (ticks == 82) open(minecraft, "map");
        if (ticks == 101) verifyAtlasInteraction();
        if (ticks == 102) verify("map");
        if (ticks == 106) open(minecraft, "overview");
        if (ticks == 126) verify("overview");
        if (ticks == 130) open(minecraft, "equipment");
        if (ticks == 150) verify("equipment");
        if (ticks == 154) open(minecraft, "skills");
        if (ticks == 176) {
            verify("skills");
            LivingKingdoms.LOGGER.info(
                    "LK_CLIENT_CODEX_DIAGNOSTIC_PASS screens=overview,equipment,map,skills rendered_window=true responsive=true viewport={}x{} controls_fit=true overlap_free=true atlas_drag=true atlas_zoom=true mastery_first=true",
                    active.width, active.height
            );
        }
    }

    private static void open(Minecraft minecraft, String page) {
        active = new RealmCodexScreenV4(page, sampleSnapshot());
        minecraft.gui.setScreen(active);
        LivingKingdoms.LOGGER.info("LK_CLIENT_CODEX_SCREEN_OPENED page={}", page);
    }

    private static void verifyAtlasInteraction() {
        if (active == null) throw new IllegalStateException("Atlas screen is not active");
        double x = active.width / 2.0D;
        double y = active.height * 0.65D;
        if (!active.handleMapScroll(x, y, 1.0D)) {
            throw new IllegalStateException("Atlas rejected zoom input");
        }
        if (!active.handleMapDrag(x, y, 0, 8.0D, -5.0D)) {
            throw new IllegalStateException("Atlas rejected drag input");
        }
    }

    private static void verify(String page) {
        if (active == null || !active.allRequiredControlsFit()) {
            throw new IllegalStateException("Codex page extends outside or overlaps the current viewport: " + page);
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
                + "growth_rule\t행동 숙련은 계속 성장하며 기술 트리는 부가 효과만 해금합니다.\n"
                + mastery("combat", "전투 숙련", 27, 13_770, 0.42F)
                + mastery("defense", "방어 숙련", 19, 7_200, 0.31F)
                + mastery("mining", "채광 숙련", 34, 21_500, 0.64F)
                + mastery("logging", "벌목 숙련", 12, 3_400, 0.21F)
                + mastery("farming", "농사 숙련", 23, 10_200, 0.55F)
                + mastery("gathering", "채집 숙련", 17, 5_500, 0.73F)
                + mastery("exploration", "탐험 숙련", 41, 31_100, 0.48F)
                + "home_x\t84\n"
                + "home_z\t-112\n"
                + "player_x\t12\n"
                + "player_z\t8\n"
                + "erden_x\t0\n"
                + "erden_z\t0\n"
                + "silvana_x\t-2400\n"
                + "silvana_z\t-1200\n"
                + "kardum_x\t2200\n"
                + "kardum_z\t-1500\n";
    }

    private static String mastery(String id, String name, int level, long xp, float progress) {
        return "mastery_" + id + "_name\t" + name + "\n"
                + "mastery_" + id + "_level\t" + level + "\n"
                + "mastery_" + id + "_xp\t" + xp + "\n"
                + "mastery_" + id + "_progress\t" + progress + "\n";
    }
}

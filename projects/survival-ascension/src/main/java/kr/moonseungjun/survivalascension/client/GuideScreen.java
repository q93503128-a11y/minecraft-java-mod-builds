package kr.moonseungjun.survivalascension.client;

/* Page navigation and skill-help information architecture follow Skill Proficiencies MIT. */

import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GuideScreen extends Screen {
    public enum Page { OVERVIEW, UNLOCKS, STATS, CONTROLS }
    private final Screen parent;
    private final Page page;
    public GuideScreen(Screen parent, Page page) { super(Component.literal(titleFor(page))); this.parent = parent; this.page = page; }

    @Override protected void init() {
        int totalWidth = 4 * 76 + 3 * 4, x = (this.width - totalWidth) / 2, y = 32;
        addRenderableWidget(Button.builder(Component.literal("가이드"), b -> open(Page.OVERVIEW)).bounds(x, y, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("해금표"), b -> open(Page.UNLOCKS)).bounds(x + 80, y, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("통계"), b -> open(Page.STATS)).bounds(x + 160, y, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("조작"), b -> open(Page.CONTROLS)).bounds(x + 240, y, 76, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(this.width / 2 - 60, this.height - 30, 120, 20).build());
    }
    private void open(Page target) { if (target != this.page) this.minecraft.gui.setScreen(new GuideScreen(this.parent, target)); }
    @Override public void onClose() { this.minecraft.gui.setScreen(this.parent); }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 14, 0xFFFFFFFF, true);
        int left = Math.max(18, this.width / 2 - 190), y = 64;
        for (Line line : lines()) { graphics.text(this.font, line.text(), left + line.indent(), y, line.color(), false); y += line.gapAfter(); }
    }

    private List<Line> lines() { return switch (this.page) { case OVERVIEW -> overviewLines(); case UNLOCKS -> unlockLines(); case STATS -> statsLines(); case CONTROLS -> controlLines(); }; }
    private static List<Line> overviewLines() {
        return List.of(
                h("SURVIVAL ASCENSION"), p("행동 숙련이 오를수록 작업 규모가 커지고, 보스 진행에 따라 세계 자체도 다음 난도 단계로 승천합니다."),
                h("숙련 VI"), p("Lv.100은 최종 숙련 VI. 여섯 숙련 모두 Lv.90보다 한 단계 큰 실제 행동 체급을 해금합니다."),
                h("월드 승천"), p("각성 → 위더 격파: 전설 → 엔더 드래곤 격파: 종말. M→인프라→진행도에서 현재 단계 확인."),
                h("종말 변이"), p("종말 단계에서는 자연 생성 좀비/스켈레톤 일부가 위상·역병·위더 변이로 등장. 엘리트/전술 분대와 중첩 가능."),
                h("채굴"), p("광역 굴착 + 광맥 + 추출. 채석장 네트워크 뒤에는 Lv.90 5×5×8, Lv.100 7×7×10 터널."),
                h("벌목"), p("잎이 붙은 자연 나무만 연쇄 벌목. Lv.100 최대 384로그, 서버 틱 분산."),
                h("농사"), p("광역 수확. Lv.100 11×11, 관개 시설 완공 뒤 실제 씨앗을 소비해 자동 재파종."),
                h("전투"), p("근접 파급·질주 충격파. Lv.100은 파급/충격파 반경과 대상 수가 한 단계 더 확장."),
                h("건축"), p("선/벽/바닥 + 건축 공방. Lv.100에서 면 11×11, 선49, 입체 7×7×7."),
                h("기동"), p("R 돌진·공중 돌진. 종말 단계 승천 중추 완공 + Lv.100이면 착지 전 공중 돌진 3회."),
                h("공동 인프라"), p("서버 월드 공동 자원을 투입해 대형 행동을 해금. 일부 최종 프로젝트는 월드 승천 단계도 요구."),
                p("Shift는 광역 작업을 단일 정밀 작업으로 되돌립니다.")
        );
    }
    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100"),
                h("월드 승천"), p("0 각성 · 1 전설(위더) · 2 종말(엔더 드래곤). 단계 상승 시 엘리트/전술 분대 체급 상승."),
                h("종말 변이"), p("Stage 2 자연 생성 대상: 위더 스켈레톤형 공격은 위더 효과 · 위상 좀비는 피격 회피 · 역병 좀비는 공격 시 독."),
                h("채굴"), p("Lv.10 3×3 · Lv.30 5×5+광맥24 · Lv.60 7×7+광맥64 · Lv.90 9×9+광맥128+추출 · Lv.100 11×11+광맥192/채석장 터널7×7×10"),
                h("벌목"), p("Lv.10 16 · Lv.30 48 · Lv.60 128 · Lv.90 256 · Lv.100 384로그. 자연 나무 잎 검증."),
                h("농사"), p("Lv.10 3×3 · Lv.30 5×5 · Lv.60 7×7 · Lv.90 9×9 · Lv.100 11×11 · 관개 완공 시 Lv.30 재파종"),
                h("전투"), p("Lv.30 파급2 · Lv.60 파급4 · Lv.90 파급8 · Lv.100 파급10/5블록 · 훈련장 충격파 Lv.100 6.5블록/16체"),
                h("건축"), p("Lv.10 선5 · Lv.30 선9+3×3 · Lv.60 선17+5×5 · Lv.90 선33+9×9/입체5³ · Lv.100 선49+11×11/입체7³"),
                h("기동"), p("Lv.30 지상 R · Lv.60 공중 R 1회 · Lv.90 극한 돌진 · Lv.100 2블록 단차/16블록 안전낙하/0.8초 대시"),
                p("종말 단계 + 승천 중추 완공: 기동 Lv.90 공중 R 2회, Lv.100 숙련 VI 공중 R 3회."),
                h("승천 중추 비용"), p("네더의 별4 · 드래곤의 숨결64 · 흑요석512 · 자수정512 · 메아리 조각64")
        );
    }
    private static List<Line> statsLines() {
        List<Line> out = new ArrayList<>(); out.add(h("현재 캐릭터 숙련"));
        for (SkillType skill : SkillType.values()) {
            int level = ClientSkillState.level(skill); long totalXp = ClientSkillState.xp(skill);
            long into = SkillTuning.xpIntoLevel(totalXp), next = SkillTuning.xpForNextLevel(level);
            String xp = level >= SkillTuning.MAX_LEVEL ? "MAX" : into + " / " + next + " XP";
            out.add(p(skill.koreanName() + "  Lv." + level + " · " + xp + " · " + effect(skill, level)));
        }
        out.add(p("월드 승천/공동 인프라 정본은 M → 인프라 → 진행도에서 확인."));
        return out;
    }
    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("M → 인프라"), p("채석장 / 관개 / 건축 공방 / 전투 훈련장 / 승천 중추 / 진행도."),
                h("승천 중추"), p("종말 단계에서만 건설 가능. 기동 Lv.90은 공중 돌진 2회, Lv.100은 3회."),
                h("전술 분대"), p("각성 3~6체 · 전설 4~7체 · 종말 5~8체. 전단장을 먼저 쓰러뜨리면 8초 후퇴."),
                h("종말 변이"), p("스포너 출신은 변이 대상에서 제외. 처치 시 추가 경험치와 확률적 메아리 조각 보상."),
                h("R · 기동 액션"), p("지상/공중 돌진. 공중 사용 횟수는 착지 시 초기화되고 모든 사용은 기존 대시 쿨을 공유."),
                h("Shift · 정밀 모드"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선."),
                h("경고"), p("Lv.100 작업은 매우 넓습니다. 건축물 주변에서는 Shift 정밀 모드를 사용하세요.")
        );
    }

    private static String effect(SkillType skill, int level) {
        return switch (skill) {
            case MINING -> SkillTuning.miningAreaSize(level) + "×" + SkillTuning.miningAreaSize(level) + " / 광맥 " + (SkillTuning.miningVeinLimit(level) <= 1 ? "잠김" : SkillTuning.miningVeinLimit(level));
            case WOODCUTTING -> "연쇄 " + SkillTuning.woodcuttingLogLimit(level) + "로그";
            case HARVESTING -> SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level) + " 수확";
            case COMBAT -> String.format(Locale.ROOT, "피해 %.2f× / 파급 %d체", SkillTuning.combatDamageMultiplier(level), SkillTuning.combatCleaveTargetLimit(level));
            case CONSTRUCTION -> "선 " + SkillTuning.constructionLineLength(level) + " / 면 " + SkillTuning.constructionPlaneSize(level) + "×" + SkillTuning.constructionPlaneSize(level);
            case MOBILITY -> String.format(Locale.ROOT, "이속 %.2f× / 단차 %.2f / 안전낙하 %.0f / R %s", SkillTuning.mobilitySpeedMultiplier(level), SkillTuning.mobilityStepHeight(level), SkillTuning.mobilitySafeFallDistance(level), level < 30 ? "잠김" : "돌진");
        };
    }
    private static String titleFor(Page page) { return switch (page) { case OVERVIEW -> "Survival Ascension · 가이드"; case UNLOCKS -> "Survival Ascension · 해금표"; case STATS -> "Survival Ascension · 통계"; case CONTROLS -> "Survival Ascension · 조작"; }; }
    private static Line h(String text) { return new Line(text, 0, 0xFFFFD37A, 15); }
    private static Line p(String text) { return new Line(text, 10, 0xFFE0E0E0, 13); }
    private record Line(String text, int indent, int color, int gapAfter) {}
}

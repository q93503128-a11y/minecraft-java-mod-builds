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
                h("SURVIVAL ASCENSION"), p("행동 숙련이 오를수록 작업 규모가 커지고, 보스 진행과 대원정 완주에 따라 세계와 행동 체급도 승천합니다."),
                h("숙련 VI + 현장 숙련"), p("Lv.100이 기본 숙련 VI. 종말 단계에서 9개 원정권을 모두 조사하면 Lv.100 행동이 한 단계 더 커지는 현장 숙련을 해방합니다."),
                h("월드 승천 / 대원정"), p("각성 → 위더: 전설 → 엔더 드래곤: 종말. 9개 원정권 진행은 /ascension stats에서 확인합니다."),
                h("승천 시련"), p("종말 단계에서 승천 중추를 완공한 뒤 다시 선택하면 4웨이브 반복 시련. 매 판 쇄도/추격/봉쇄 전술 교리와 중반 증원이 달라집니다."),
                h("각성 신화 장비"), p("신화 III는 M→장비→신화 각성에서 대량 자원을 소모해 기존 3 affix를 보존한 채 4번째 affix를 추가할 수 있습니다."),
                h("종말 변이"), p("종말 단계 자연 생성 좀비/스켈레톤 일부가 위상·역병·위더 변이. 엘리트/전술 분대와 중첩 가능."),
                h("채굴"), p("Lv.100 채석장 터널 7×7×10. 현장 숙련이면 7×7×12이며 처리량은 계속 서버 틱 분산."),
                h("벌목"), p("잎이 붙은 자연 나무만 연쇄 벌목. Lv.100 384로그, 현장 숙련 448로그. 서버 틱 분산."),
                h("농사"), p("Lv.100 11×11, 현장 숙련 13×13. 대량 수확은 틱 분산되고 관개 재파종은 실제 씨앗을 소비."),
                h("전투"), p("Lv.100 훈련장 충격파 6.5블록/16체, 현장 숙련 7.5블록/20체. 피해비율/쿨은 유지."),
                h("건축"), p("Lv.100 선49/면11×11/입체7³. 현장 숙련 선65/면13×13, 입체와 재료·보호 규칙은 유지."),
                h("기동"), p("종말+승천 중추 Lv.100 공중 돌진3회, 현장 숙련이면 착지 전4회. 모든 사용은 기존 쿨 공유."),
                h("공동 인프라"), p("서버 월드 공동 자원을 투입해 대형 행동을 해금. 일부 최종 프로젝트는 월드 승천 단계도 요구."),
                p("Shift는 광역 작업을 단일 정밀 작업으로 되돌립니다.")
        );
    }
    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 완주 시 Lv.100 현장 숙련"),
                h("월드 승천"), p("0 각성 · 1 전설(위더) · 2 종말(엔더 드래곤). 단계 상승 시 엘리트/분대 체급과 원정권이 추가됩니다."),
                h("원정권"), p("Stage0 삼림/건조/습지/고산/대양 · Stage1 심층/빙설/네더 · Stage2 엔드. 최초 발견은 플레이어별 1회."),
                h("승천 시련"), p("Stage2 + 승천 중추. 입장 메아리32 · 자수정64 · 드래곤 숨결8. 4웨이브/60초/교리별 증원, 완료 신화 III 보장."),
                h("신화 각성"), p("신화 III 전용 1회. 자수정256 · 다이아24 · 파편8 · 메아리64 · 드래곤 숨결16 → 기존3 affix + 4번째 affix."),
                h("채굴"), p("Lv.10 3×3 · Lv.30 5×5+광맥24 · Lv.60 7×7+64 · Lv.90 9×9+128+추출 · Lv.100 11×11+192/터널7×7×10 · 현장7×7×12"),
                h("벌목"), p("Lv.10 16 · Lv.30 48 · Lv.60 128 · Lv.90 256 · Lv.100 384 · 현장448로그. 자연 나무 잎 검증."),
                h("농사"), p("Lv.10 3×3 · Lv.30 5×5 · Lv.60 7×7 · Lv.90 9×9 · Lv.100 11×11 · 현장13×13 · 관개 재파종"),
                h("전투"), p("Lv.30 파급2 · Lv.60 파급4 · Lv.90 파급8 · Lv.100 파급10/5블록 · 훈련장6.5/16 · 현장7.5/20"),
                h("건축"), p("Lv.10 선5 · Lv.30 선9+3×3 · Lv.60 선17+5×5 · Lv.90 선33+9×9/입체5³ · Lv.100 선49+11×11/7³ · 현장 선65+13×13"),
                h("기동"), p("Lv.30 지상R · Lv.60 공중1회 · Stage2+중추 Lv.90 2회 · Lv.100 3회 · 현장 숙련4회."),
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
        out.add(p("월드 승천/인프라/원정 정본은 M→인프라 진행도와 /ascension stats에서 확인."));
        return out;
    }
    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("/ascension stats"), p("현재 숙련과 9개 원정권 발견 상태, 현장 숙련 해방 여부를 확인합니다."),
                h("M → 장비"), p("재련 / 신화 각성 / 분해 / 장비 정보. 각성 신화 재련은 4 affix 상태를 유지하지만 비용이 큽니다."),
                h("M → 인프라"), p("채석장 / 관개 / 건축 공방 / 전투 훈련장 / 승천 중추 / 진행도."),
                h("승천 시련"), p("열린 지형에서 시작. 보스바에 교리/남은 적/시간 표시. 절반 부근 증원, 사망·64블록 이탈 10초 또는 시간초과 시 실패."),
                h("전술 분대"), p("각성3~6체 · 전설4~7체 · 종말5~8체. 전단장을 먼저 쓰러뜨리면 8초 후퇴."),
                h("R · 기동 액션"), p("지상/공중 돌진. 공중 횟수는 착지 시 초기화되고 모든 사용은 기존 대시 쿨을 공유."),
                h("Shift · 정밀 모드"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선."),
                h("경고"), p("현장 숙련 작업은 매우 넓습니다. 건축물 주변에서는 Shift 정밀 모드를 사용하세요.")
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

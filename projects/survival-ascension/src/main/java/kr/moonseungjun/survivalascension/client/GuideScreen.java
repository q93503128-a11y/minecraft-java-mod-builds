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
                h("SURVIVAL ASCENSION"), p("숙련이 오를수록 작업 규모가 커지고, 보스 진행·인프라·원정 완수가 더 큰 행동 체급을 엽니다."),
                h("대원정 현장 목표"), p("지역에 들어가는 것은 '발견'뿐. 그 지역에서 벌목·건축·수확·횡단·항해·채굴·전투 목표를 직접 채워야 '완수'됩니다."),
                h("현장 숙련"), p("종말 단계에서 9개 원정권을 모두 완수하면 Lv.100 행동이 한 단계 더 커집니다. 저레벨 행동을 조기 해금하지 않습니다."),
                h("승천 시련 / 장비"), p("승천 중추의 4웨이브 시련은 쇄도/추격/봉쇄 교리로 변주되고, 신화 III는 대량 자원으로 4-affix 각성 신화까지 성장합니다."),
                h("최종 작업 체급"), p("현장 숙련: 터널7×7×12 · 벌목448 · 수확13×13 · 충격파7.5/20 · 건축 선65/면13×13 · 공중돌진4회."),
                h("안전 계약"), p("Shift 정밀 작업, 정상 destroyBlock, 실제 재료 소비/보호 훅, 대형 채굴·벌목·농사·건축 틱 분산을 유지합니다.")
        );
    }
    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 완주 시 현장 숙련"),
                h("Stage 0 원정"), p("삼림: 자연 나무 일괄벌목96 · 건조: 대량배치128 · 습지: 성숙작물96 · 고산: 도보횡단600 · 대양: 수영/항해800"),
                h("Stage 1~2 원정"), p("심층: 곡괭이채굴192 · 빙설: 도보횡단600 · 네더: 적대몹24 · 엔드: 적대몹32"),
                h("대원정 보상"), p("초기권4완수 → 자원 보상 · 7완수+심층/네더 → 전설 보상 · 9/9 → 신화 III+종말 자원+현장 숙련"),
                h("승천 시련"), p("Stage2 + 승천 중추. 메아리32 · 자수정64 · 드래곤숨결8. 4웨이브/60초/교리별 증원, 완료 시 신화 III 보장."),
                h("신화 각성"), p("자수정256 · 다이아24 · 파편8 · 메아리64 · 드래곤숨결16 → 정상 신화 III의 기존3 affix 유지 + 4번째 affix."),
                h("숙련 VI"), p("채굴11×11+192 · 벌목384 · 농사11×11 · 전투 파급10 · 건축 선49/면11×11/입체7³ · 중추 공중돌진3회"),
                h("현장 숙련"), p("Lv.100에서만 터널12깊이 · 벌목448 · 농사13×13 · 충격파7.5/20 · 건축65/13×13 · 공중돌진4회")
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
        out.add(p("/ascension stats에서 원정 발견/완수 수와 진행 중인 현장 목표 수치를 확인할 수 있습니다."));
        return out;
    }
    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("/ascension stats"), p("숙련 + 원정 발견/완수 + 진행 중 목표 수치 + 현장 숙련 해방 여부 확인."),
                h("원정 판정"), p("삼림은 자연나무 일괄벌목, 건조는 대량배치, 습지는 성숙작물, 심층은 곡괭이 채굴, 네더/엔드는 적대몹 처치만 기록."),
                h("이동 원정"), p("고산/빙설은 정상 도보 질주만 기록. 텔레포트·비행·수영·탑승은 제외. 대양만 수영/항해를 별도 기록."),
                h("R · 기동"), p("지상/공중 돌진. 착지 시 공중횟수 초기화. Stage2+중추 Lv.100 3회, 현장 숙련 4회."),
                h("Shift · 정밀"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선."),
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

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
                h("SURVIVAL ASCENSION"), p("숙련이 오를수록 작업 규모가 커지고, 세계 진행·원정·인프라·강적·생산망이 그 체급을 다시 소비합니다."),
                h("원정 / 현장 사건"), p("9지역은 실제 행동 지령과 희귀 사건으로 진행됩니다. 전부 완수하면 Lv.100 현장 숙련이 열립니다."),
                h("산업 생산망"), p("Stage1 산업 가공소는 광물·구조재·농산물·정밀재료를 각각 대량 배치로 소비합니다. 네 계통을 모두 돌려야 하나의 산업 사이클이 완성됩니다."),
                h("현장 보급"), p("산업 사이클은 보급권1을 만들며 최대3개까지 비축합니다. 출고 시 금32·자수정16·메아리2를 받아 사냥·시련·장비에 다시 투입할 수 있습니다."),
                h("정점 사냥"), p("완수한 원정권에서는 지역별 행동 패턴을 가진 정점 강적9종을 반복 추적할 수 있습니다. 9종 최초 격파는 별도 완주 보상을 줍니다."),
                h("최종 작업 체급"), p("현장 숙련: 터널7×7×12 · 벌목448 · 수확13×13 · 충격파7.5/20 · 건축 선65/면13×13 · 공중돌진4회.")
        );
    }
    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 완주 시 현장 숙련"),
                h("Stage 0~2 원정"), p("삼림/건조/습지/고산/대양 → 심층/빙설/네더 → 엔드. 지역마다 표준/혼합 지령과 희귀 사건이 있습니다."),
                h("산업 가공소"), p("Stage1 · 석재벽돌1024 · 철512 · 구리512 · 레드스톤256 · 자수정128."),
                h("4계통 생산"), p("제련: 철원석96+구리원석96+석탄64 · 구조재: 통나무192+조약돌384+철32 · 식량: 밀128+당근64+감자64+비트32 · 정밀: 레드128+자수정64+금32+석영64."),
                h("생산 규칙"), p("계통별 버퍼 최대3. 네 계통이 모두1 이상일 때만 1세트를 소비해 보급권1 생성. 보급권도 최대3이며 대기 세트는 출고 후 자동 조립됩니다."),
                h("정점 / 승천 시련"), p("정점: Stage1 추적소+완수 원정권, 90초. 시련: Stage2 승천 중추, 4웨이브. 둘은 현장 사건과 중첩되지 않습니다."),
                h("숙련 VI / 현장"), p("기본: 채굴11×11+192 · 벌목384 · 농사11×11 · 건축49/11×11 · 공중3회. 현장: 터널12깊이 · 448 · 13×13 · 건축65/13×13 · 공중4회")
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
        out.add(p("/ascension stats에서 원정/사건, 정점 최초격파·총승리, 산업 누적 사이클·보급권을 함께 확인할 수 있습니다."));
        return out;
    }
    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("산업 생산"), p("M → 인프라 → 산업 가공소. 시설 투자 후 네 생산 배치·보급 출고·생산 현황을 같은 방사형 하위 메뉴에서 처리합니다."),
                h("재료 안전"), p("생산 배치는 필요한 모든 재료와 버퍼 여유를 서버가 먼저 확인한 뒤 한 번에 소비합니다. 부족하면 일부 재료만 빠지지 않습니다."),
                h("/ascension stats"), p("숙련 + 원정/사건 + 정점 사냥 + 산업 사이클/보급권 + 현장 숙련 여부 확인."),
                h("정점 사냥"), p("M → 인프라 → 정점 추적소. 완공 후 완수한 원정권 안에서 다시 선택해 시작합니다."),
                h("R · 기동"), p("지상/공중 돌진. 성공한 R 사용만 돌진 지령과 해당 긴급 사건에 기록됩니다."),
                h("Shift · 정밀"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선.")
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

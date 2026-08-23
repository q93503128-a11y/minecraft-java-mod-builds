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
                h("대원정 현장 지령"), p("지역은 발견 후 플레이어별 표준/혼합 지령을 배정받으며, 모든 task를 실제 행동으로 채워야 완수됩니다."),
                h("희귀 현장 사건"), p("원정 지역에서는 드물게 45~60초 사건이 발생합니다. 실패해도 지령은 유지되고 지역당 최초 해결 보상만 지급됩니다."),
                h("정점 사냥"), p("전설 단계의 정점 추적소를 완공하면 완수한 원정권에서 지역 고유 정점 강적을 반복 추적할 수 있습니다. 9종은 돌진·증원·독기·끌어당김·도약·냉기·위더·중력 등 서로 다른 패턴을 씁니다."),
                h("정점 장기 목표"), p("사냥마다 메아리8·자수정32·금32를 소비합니다. 최초 격파 9/9는 별도 신화 III+종말 자원 보상을 주며 반복 사냥은 승천 II 중심이라 승천 시련을 대체하지 않습니다."),
                h("현장 숙련"), p("종말 단계에서 9개 원정권을 모두 완수하면 Lv.100 행동이 한 단계 더 커집니다. 저레벨 행동을 조기 해금하지 않습니다."),
                h("최종 작업 체급"), p("현장 숙련: 터널7×7×12 · 벌목448 · 수확13×13 · 충격파7.5/20 · 건축 선65/면13×13 · 공중돌진4회.")
        );
    }
    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 완주 시 현장 숙련"),
                h("Stage 0 표준 지령"), p("삼림 벌목96 · 건조 대량배치128 · 습지 수확96 · 고산 횡단600 · 대양 항해800. 새 발견은 표준/혼합 중 무작위."),
                h("Stage 1~2 표준 지령"), p("심층 채굴192 · 빙설 횡단600 · 네더 적대몹24 · 엔드 적대몹32. 혼합형은 두 task를 모두 요구."),
                h("희귀 사건"), p("지역별 습격형1 + 긴급작업형1. 30초 간격 희귀 체크, 해결 보상은 각 원정권당 1회."),
                h("정점 추적소"), p("Stage1 · 철512 · 금256 · 자수정256 · 메아리32 · 네더별1. 완공 후 완수한 원정권 현지에서 다시 선택하면 정점 사냥."),
                h("정점 사냥"), p("메아리8 · 자수정32 · 금32 · 90초. 지역별 정점 9종 최초 격파를 모아 9/9 완주 보상을 노립니다."),
                h("승천 시련"), p("Stage2 + 승천 중추. 메아리32 · 자수정64 · 드래곤숨결8. 4웨이브/60초/교리별 증원, 완료 시 신화 III 보장."),
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
        out.add(p("/ascension stats에서 원정 발견/완수/사건 해결과 정점 최초격파0~9, 총 사냥 승리를 함께 확인할 수 있습니다."));
        return out;
    }
    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("/ascension stats"), p("숙련 + 원정 발견/완수/사건 + 정점 최초격파/총승리 + 현장 숙련 여부 확인."),
                h("정점 사냥 시작"), p("M → 인프라 → 정점 추적소. 완공 후에는 이미 완수한 원정권 안에서 다시 선택해야 해당 지역 정점을 추적합니다."),
                h("정점 전조"), p("정점 보스는 보스바와 액션바 전조를 사용합니다. 돌진 준비·증원·범위 상태이상·끌어당김 등을 보고 위치를 바꾸세요."),
                h("전투 이벤트 겹침 방지"), p("정점 사냥 중에는 승천 시련 시작을 막고 현장 사건 재개방도 뒤로 밀어 세 이벤트가 한 플레이어에게 동시에 겹치지 않게 합니다."),
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

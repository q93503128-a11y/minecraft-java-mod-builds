package kr.moonseungjun.survivalascension.client;

/*
 * Page navigation and skill-help information architecture follow the MIT-licensed
 * Skill Proficiencies skills screen: Copyright (c) 2026 balovich-matje.
 */

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

    public GuideScreen(Screen parent, Page page) {
        super(Component.literal(titleFor(page)));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        int totalWidth = 4 * 76 + 3 * 4;
        int x = (this.width - totalWidth) / 2;
        int y = 32;
        addRenderableWidget(Button.builder(Component.literal("가이드"), b -> open(Page.OVERVIEW)).bounds(x, y, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("해금표"), b -> open(Page.UNLOCKS)).bounds(x + 80, y, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("통계"), b -> open(Page.STATS)).bounds(x + 160, y, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("조작"), b -> open(Page.CONTROLS)).bounds(x + 240, y, 76, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(this.width / 2 - 60, this.height - 30, 120, 20).build());
    }

    private void open(Page target) {
        if (target != this.page) this.minecraft.gui.setScreen(new GuideScreen(this.parent, target));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 14, 0xFFFFFFFF, true);

        int left = Math.max(18, this.width / 2 - 190);
        int y = 64;
        for (Line line : lines()) {
            graphics.text(this.font, line.text(), left + line.indent(), y, line.color(), false);
            y += line.gapAfter();
        }
    }

    private List<Line> lines() {
        return switch (this.page) {
            case OVERVIEW -> overviewLines();
            case UNLOCKS -> unlockLines();
            case STATS -> statsLines();
            case CONTROLS -> controlLines();
        };
    }

    private static List<Line> overviewLines() {
        return List.of(
                h("SURVIVAL ASCENSION"),
                p("행동을 반복할수록 단순 수치가 아니라 한 번에 처리하는 작업 규모가 커집니다."),
                h("채굴"), p("곡괭이 채굴로 성장. 일반 지형은 광역 굴착, 가치 광석은 고레벨에서 연결 광맥 추적."),
                h("벌목"), p("도끼로 통나무를 베며 성장. 레벨에 따라 연결된 통나무를 한 번에 처리."),
                h("농사"), p("완전히 익은 작물만 경험치. 괭이를 들면 고레벨에서 넓은 밭을 한 번에 수확."),
                h("전투"), p("직접 처치로 성장. 피해가 오르고 고레벨 근접 공격은 주변 적대몹에 파급."),
                p("파괴형 능력이 부담스러우면 웅크린 채 작업하면 정밀 모드가 우선됩니다.")
        );
    }

    private static List<Line> unlockLines() {
        return List.of(
                h("채굴"), p("Lv.10 3×3  ·  Lv.30 5×5 + 광맥24  ·  Lv.60 7×7 + 광맥64  ·  Lv.90 9×9 + 광맥128"),
                h("벌목"), p("Lv.10 16로그  ·  Lv.30 48로그  ·  Lv.60 128로그  ·  Lv.90 256로그"),
                h("농사"), p("Lv.10 3×3  ·  Lv.30 5×5  ·  Lv.60 7×7  ·  Lv.90 9×9"),
                h("전투"), p("Lv.30 파급2체  ·  Lv.60 파급4체  ·  Lv.90 파급8체"),
                p("숙련 등급 I / II / III / IV / V는 Lv.0 / 10 / 30 / 60 / 90 구간과 대응합니다."),
                p("건축과 기동은 다음 성장 계통으로 확장됩니다.")
        );
    }

    private static List<Line> statsLines() {
        List<Line> out = new ArrayList<>();
        out.add(h("현재 캐릭터 숙련"));
        for (SkillType skill : SkillType.values()) {
            int level = ClientSkillState.level(skill);
            long totalXp = ClientSkillState.xp(skill);
            long into = SkillTuning.xpIntoLevel(totalXp);
            long next = SkillTuning.xpForNextLevel(level);
            String xp = level >= SkillTuning.MAX_LEVEL ? "MAX" : into + " / " + next + " XP";
            out.add(p(skill.koreanName() + "  Lv." + level + "  ·  " + xp + "  ·  " + effect(skill, level)));
        }
        return out;
    }

    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 가이드 / 해금표 / 통계 / 조작을 한 곳에서 엽니다."),
                h("마우스 · 라디얼 선택"), p("M 메뉴에서 원하는 방향을 가리킨 뒤 좌클릭하여 선택합니다."),
                h("Shift · 정밀 모드"), p("채굴·벌목·농사의 광역/연쇄 효과를 끄고 한 블록만 처리합니다."),
                h("경고"), p("고레벨 채굴은 매우 넓은 범위를 파괴합니다. 건축물 주변에서는 Shift 정밀 모드를 사용하세요."),
                p("K 직접 숙련 단축키는 제거되고 M 통합 메뉴로 합쳐집니다.")
        );
    }

    private static String effect(SkillType skill, int level) {
        return switch (skill) {
            case MINING -> SkillTuning.miningAreaSize(level) + "×" + SkillTuning.miningAreaSize(level) + " / 광맥 " + (SkillTuning.miningVeinLimit(level) <= 1 ? "잠김" : SkillTuning.miningVeinLimit(level));
            case WOODCUTTING -> "연쇄 " + SkillTuning.woodcuttingLogLimit(level) + "로그";
            case HARVESTING -> SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level) + " 수확";
            case COMBAT -> String.format(Locale.ROOT, "피해 %.2f× / 파급 %d체", SkillTuning.combatDamageMultiplier(level), SkillTuning.combatCleaveTargetLimit(level));
            case CONSTRUCTION -> "개발 중";
            case MOBILITY -> "개발 중";
        };
    }

    private static String titleFor(Page page) {
        return switch (page) {
            case OVERVIEW -> "Survival Ascension · 가이드";
            case UNLOCKS -> "Survival Ascension · 해금표";
            case STATS -> "Survival Ascension · 통계";
            case CONTROLS -> "Survival Ascension · 조작";
        };
    }

    private static Line h(String text) { return new Line(text, 0, 0xFFFFD37A, 15); }
    private static Line p(String text) { return new Line(text, 10, 0xFFE0E0E0, 13); }
    private record Line(String text, int indent, int color, int gapAfter) {}
}

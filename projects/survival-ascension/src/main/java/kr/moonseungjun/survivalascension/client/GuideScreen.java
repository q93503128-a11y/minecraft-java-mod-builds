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

    private static final int MAX_CONTENT_WIDTH = 760;
    private static final int CONTENT_MARGIN = 18;
    private static final int TEXT_LINE_HEIGHT = 10;

    private final Screen parent;
    private final Page page;

    public GuideScreen(Screen parent, Page page) {
        super(Component.literal(titleFor(page)));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(76, Math.max(62, (this.width - 36) / 4 - 4));
        int totalWidth = 4 * buttonWidth + 3 * 4;
        int x = (this.width - totalWidth) / 2;
        int y = 32;
        addRenderableWidget(Button.builder(Component.literal("가이드"), b -> open(Page.OVERVIEW)).bounds(x, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("해금표"), b -> open(Page.UNLOCKS)).bounds(x + buttonWidth + 4, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("통계"), b -> open(Page.STATS)).bounds(x + (buttonWidth + 4) * 2, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("조작"), b -> open(Page.CONTROLS)).bounds(x + (buttonWidth + 4) * 3, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(this.width / 2 - 60, this.height - 30, 120, 20).build());
    }

    private void open(Page target) {
        if (target != this.page) this.minecraft.gui.setScreen(new GuideScreen(this.parent, target));
    }

    @Override public void onClose() { this.minecraft.gui.setScreen(this.parent); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 14, 0xFFFFFFFF, true);

        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(120, this.width - CONTENT_MARGIN * 2));
        int left = (this.width - contentWidth) / 2;
        int bottom = this.height - 44;
        int y = 64;

        for (Line line : lines()) {
            int lineLeft = left + line.indent();
            int availableWidth = Math.max(80, contentWidth - line.indent());
            List<String> wrapped = wrapToWidth(line.text(), availableWidth);
            for (String segment : wrapped) {
                if (y > bottom) return;
                graphics.text(this.font, segment, lineLeft, y, line.color(), false);
                y += TEXT_LINE_HEIGHT;
            }
            y += Math.max(3, line.gapAfter() - TEXT_LINE_HEIGHT);
        }
    }

    private List<String> wrapToWidth(String text, int maxWidth) {
        if (text.isEmpty() || this.font.width(text) <= maxWidth) return List.of(text);
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            while (start < text.length() && text.charAt(start) == ' ') start++;
            if (start >= text.length()) break;
            int end = start;
            int lastSpace = -1;
            while (end < text.length()) {
                if (text.charAt(end) == ' ') lastSpace = end;
                String candidate = text.substring(start, end + 1);
                if (this.font.width(candidate) > maxWidth) break;
                end++;
            }
            if (end >= text.length()) {
                out.add(text.substring(start).trim());
                break;
            }
            int split = lastSpace >= start ? lastSpace : Math.max(start + 1, end);
            String segment = text.substring(start, split).trim();
            if (!segment.isEmpty()) out.add(segment);
            start = split;
        }
        return out.isEmpty() ? List.of(text) : out;
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
                h("SURVIVAL ASCENSION"), p("숙련이 오를수록 작업 규모가 커지고, 세계 진행·원정·인프라·강적·생산망이 그 체급을 다시 소비합니다."),
                h("물리 화물 중계"), p("0.42부터 산업 가공소+토목 공사소를 완공하면 활성 전초의 실제 창고 자원을 레일 위 빈 상자 광산수레에 싣고, 같은 수레를 다른 활성 전초까지 실제 운반해 그곳 창고군으로 하역할 수 있습니다."),
                h("토목 공사소 / 도로·교량"), p("전설 단계 토목 공사소를 실제 물류 배럴 기반 준공 현장과 함께 완공하면 건축 Lv.60부터 바라보는 방향으로 3폭 장거리 도로/교량 바닥을 실제 블록으로 시공합니다."),
                h("물리 요새 방어"), p("전초 반경6~12의 실제 방어진지는 길막 자체가 방어력이며, 요새 최종 공세의 Ravager/Vindicator는 적격 방어진을 실제로 파괴할 수 있습니다."),
                h("물리 창고군"), p("등록 배럴 1개를 앵커로 두고 반경6 실제 배럴을 최대8개 연결합니다. 가상 슬롯이 아니라 각 배럴의 실제 인벤토리를 그대로 씁니다."),
                h("최종 작업 체급"), p("현장 숙련: 터널7×7×12 · 벌목448 · 수확13×13 · 충격파7.5/20 · 건축 선65/도로3×65/면13×13 · 공중돌진4회.")
        );
    }

    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 완주 시 현장 숙련"),
                h("물리 화물"), p("산업 가공소+토목 공사소 필요. 활성 전초4블록 안에서 레일 위 빈 상자 광산수레4블록 → 적재. 같은 차원의 다른 활성 전초까지 실제 운반 → 같은 메뉴로 하역."),
                h("화물 대상"), p("현장 일괄 적재와 같은 대량 자원만 이동합니다. 출발 전초 앵커+연결 창고에서 실제 스택을 빼 수레 슬롯에 넣고, 도착 창고 용량만큼만 하역합니다."),
                h("토목 공사소"), p("월드 승천1단계. 석재벽돌2048 · 조약돌1536 · 자갈1536 · 철256 · 구리256. 최종 투입은 자기 등록 배럴4블록 안의 물리 토목 현장을 검증합니다."),
                h("도로/교량 모드"), p("건축 Lv.60 + 토목 공사소. 3폭 × 17칸, Lv.90 33, Lv.100 49, 현장 숙련65. 첫 블록과 같은 블록을 수평 앞으로 시공합니다."),
                h("요새 방어전"), p("실제 방어진지 + 보급권2. 총4공세·5분. 최종 공세는 공성 파괴자가 방어진을 열 수 있으며 일반 3공세 방어는 비파괴입니다.")
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
            out.add(p(skill.koreanName() + "  Lv." + level + " · " + xp + " · " + effect(skill, level)));
        }
        out.add(p("산업 생산 현황에서 실제 배럴/전초와 근처 상자 광산수레의 물리 화물 상태도 함께 확인합니다."));
        return out;
    }

    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("물리 화물 적재"), p("M → 인프라 → 산업 가공소 → 물리 화물 수레. 활성 출발 전초4블록 안에서 레일 위 빈 상자 광산수레를 두고 선택하면 그 전초의 실제 창고 대량자원을 수레 용량만큼 적재합니다."),
                h("물리 화물 하역"), p("적재한 바로 그 수레를 같은 차원의 다른 자신의 활성 전초까지 레일로 운반한 뒤 다시 선택합니다. 도착 배럴 용량 부족 시 가능한 만큼만 옮기고 잔여 화물은 수레에 남습니다."),
                h("화물 안전 경계"), p("수레/재고 순간이동, 자동 운전, 가상 노선, 강제 청크 로딩, 새 보급권 비용은 없습니다. 실제 레일·실제 수레·실제 배럴 재고가 전부입니다."),
                h("도로/교량 시공"), p("M → 건축 → 도로/교량. 토목 공사소 완공 후 블록 하나를 놓으면 같은 높이의 3폭 바닥을 앞으로 대량 시공합니다."),
                h("Shift · 정밀"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선합니다."),
                h("공성 파괴 대응"), p("요새 4공세의 Ravager/Vindicator가 성벽에 붙으면 적격 방어진을 파괴합니다. 드롭된 자재를 건축 선/벽 배치로 다시 메울 수 있습니다.")
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

    private static String titleFor(Page page) {
        return switch (page) {
            case OVERVIEW -> "Survival Ascension · 가이드";
            case UNLOCKS -> "Survival Ascension · 해금표";
            case STATS -> "Survival Ascension · 통계";
            case CONTROLS -> "Survival Ascension · 조작";
        };
    }

    private static Line h(String text) { return new Line(text, 0, 0xFFFFD37A, 15); }
    private static Line p(String text) { return new Line(text, 8, 0xFFD0D0D0, 13); }
    private record Line(String text, int indent, int color, int gapAfter) {}
}

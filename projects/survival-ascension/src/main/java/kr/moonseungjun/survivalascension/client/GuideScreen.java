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
                h("SURVIVAL ASCENSION"), p("행동을 반복할수록 단순 수치가 아니라 한 번에 처리하는 작업 규모와 행동 자체가 커집니다."),
                h("채굴"), p("광역 굴착 + 연결 광맥. M→채굴에서 자동/굴착/광맥/추출/터널을 선택."),
                h("벌목"), p("레벨에 따라 연결된 통나무를 한 번에 처리."),
                h("농사"), p("완전히 익은 작물을 넓은 범위로 수확. 관개 시설 완공 후 Lv.30부터 씨앗 소비 자동 재파종."),
                h("전투"), p("피해 성장 + 고레벨 근접 파급 타격."),
                h("건축"), p("M→건축에서 선/벽/바닥을 선택해 실제 재료로 대량 배치."),
                h("기동"), p("실제 지상 질주로 성장. 단차 자동 넘기기, 안전 낙하, R 돌진, 고레벨 공중 돌진을 해금."),
                h("희귀 장비"), p("M→장비에서 affix 확인/재련/분해. 재련은 자수정·다이아·네더라이트를 대량 소비."),
                h("공동 인프라"), p("M→인프라에서 서버 월드 공동 자원을 투입. 완공 효과는 모든 플레이어가 함께 사용."),
                p("Shift는 광역 작업을 단일 정밀 작업으로 되돌립니다.")
        );
    }

    private static List<Line> unlockLines() {
        return List.of(
                h("채굴"), p("Lv.10 3×3+굴착 · Lv.30 5×5+광맥24 · Lv.60 7×7+광맥64 · Lv.90 9×9+광맥128+추출"),
                p("채석장 네트워크 완공 + 채굴 Lv.90: 터널 5×5×8. 서버 틱에 분산 처리."),
                h("벌목"), p("Lv.10 16로그 · Lv.30 48로그 · Lv.60 128로그 · Lv.90 256로그"),
                h("농사"), p("Lv.10 3×3 · Lv.30 5×5 · Lv.60 7×7 · Lv.90 9×9"),
                p("관개 시설 완공 + 농사 Lv.30: 밀/당근/감자/비트/네더와트 자동 재파종. 씨앗/작물을 실제 1개 소비."),
                h("전투"), p("Lv.30 파급2체 · Lv.60 파급4체 · Lv.90 파급8체"),
                h("건축"), p("Lv.10 선5 · Lv.30 선9+3×3 · Lv.60 선17+5×5 · Lv.90 선33+9×9"),
                h("기동"), p("Lv.10 1블록 단차+안전낙하 · Lv.30 R 지상돌진 · Lv.60 공중 R 1회 · Lv.90 극한돌진/1.2초 쿨"),
                p("숙련 등급 I / II / III / IV / V = Lv.0 / 10 / 30 / 60 / 90")
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
        return out;
    }

    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기. 해금표·통계·조작은 가이드 내부 탭."),
                h("M → 채굴"), p("자동 / 굴착 / 광맥 / 추출 / 터널. 터널은 채석장 네트워크 완공 후 Lv.90 사용."),
                h("M → 인프라"), p("프로젝트 항목 클릭 시 현재 인벤토리의 필요한 재료만큼 공동 진행도에 자동 투입. 진행도에서 전체 현황 확인."),
                h("M → 장비"), p("주 손 희귀 장비의 affix를 확인하고 재련/분해. 모든 재료와 장비 변경은 서버가 재검증."),
                h("R · 기동 액션"), p("기동 Lv.30 지상 돌진. Lv.60부터 공중에서도 착지 전 1회 사용 가능."),
                h("Shift · 정밀 모드"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선."),
                h("관개 재파종"), p("보호 배치 훅을 통과하고 현재 인벤토리에 씨앗/작물이 있을 때만 재파종. 공짜 씨앗 복제 없음."),
                h("경고"), p("고레벨 작업은 넓습니다. 건축물 주변에서는 Shift 정밀 모드를 사용하세요.")
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

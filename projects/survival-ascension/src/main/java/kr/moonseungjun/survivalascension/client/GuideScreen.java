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
                h("월드 승천"), p("각성 단계 → 위더 격파: 전설 단계 → 엔더 드래곤 격파: 종말 단계. M→인프라→진행도에서 현재 단계 확인."),
                h("채굴"), p("광역 굴착 + 연결 광맥. M→채굴에서 자동/굴착/광맥/추출/터널을 선택."),
                h("벌목"), p("잎이 붙은 자연 나무만 연쇄 벌목. 대규모 벌목은 서버 틱에 나눠 처리."),
                h("농사"), p("완전히 익은 작물을 넓은 범위로 수확. 관개 시설 완공 후 Lv.30부터 씨앗 소비 자동 재파종."),
                h("전투"), p("피해 성장 + 근접 파급. 고성장 지역에서는 적이 전단장/돌격/추적/지원 역할의 전술 분대를 구성."),
                h("건축"), p("M→건축에서 선/벽/바닥. 건축 공방 완공 + Lv.90이면 실제 재료를 쓰는 5×5×5 입체 채우기."),
                h("기동"), p("실제 지상 질주로 성장. 단차 자동 넘기기, 안전 낙하, R 돌진, 고레벨 공중 돌진을 해금."),
                h("공동 인프라"), p("M→인프라에서 서버 월드 공동 자원을 투입. 완공 효과는 모든 플레이어가 함께 사용."),
                p("Shift는 광역 작업을 단일 정밀 작업으로 되돌립니다.")
        );
    }

    private static List<Line> unlockLines() {
        return List.of(
                h("월드 승천"), p("0 각성: 기본 세계 · 1 전설: 위더 격파 · 2 종말: 엔더 드래곤 격파"),
                p("전설/종말 단계에서는 엘리트 출현률·상위 랭크 비율과 전술 분대의 출현률·최대 규모가 함께 상승."),
                h("채굴"), p("Lv.10 3×3+굴착 · Lv.30 5×5+광맥24 · Lv.60 7×7+광맥64 · Lv.90 9×9+광맥128+추출"),
                p("채석장 네트워크 완공 + 채굴 Lv.90: 터널 5×5×8. 서버 틱에 분산 처리."),
                h("벌목"), p("Lv.10 16로그 · Lv.30 48로그 · Lv.60 128로그 · Lv.90 256로그. 잎 근거 없는 목조 구조물은 단일 파괴."),
                h("농사"), p("Lv.10 3×3 · Lv.30 5×5 · Lv.60 7×7 · Lv.90 9×9"),
                p("관개 시설 완공 + 농사 Lv.30: 밀/당근/감자/비트/네더와트 자동 재파종. 씨앗/작물을 실제 1개 소비."),
                h("전투"), p("Lv.30 파급2체 · Lv.60 파급4체 · Lv.90 파급8체"),
                p("전투 훈련장 완공 + 전투 Lv.90: 질주 근접 공격이 3초마다 반경 5.5블록 / 최대 12체 360° 충격파로 승격."),
                h("건축"), p("Lv.10 선5 · Lv.30 선9+3×3 · Lv.60 선17+5×5 · Lv.90 선33+9×9"),
                p("건축 공방 완공 + 건축 Lv.90: 5×5×5 입체 채우기."),
                h("기동"), p("Lv.10 1블록 단차+안전낙하 · Lv.30 R 지상돌진 · Lv.60 공중 R 1회 · Lv.90 극한돌진/1.2초 쿨")
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
        out.add(p("월드 승천 단계는 M → 인프라 → 진행도에서 서버 정본을 확인하세요."));
        return out;
    }

    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기. 해금표·통계·조작은 가이드 내부 탭."),
                h("M → 인프라 → 진행도"), p("현재 월드 승천 단계와 모든 공동 프로젝트 진행도를 서버에서 확인."),
                h("전술 분대"), p("평균 숙련 30+ 자연 적이 3~6체 분대로 시작하며, 전설 단계는 최대 7체·종말 단계는 최대 8체."),
                h("전단장"), p("전단장을 먼저 쓰러뜨리면 8초간 분대가 후퇴하며 메아리 조각 보상."),
                h("질주 충격파"), p("전투 훈련장 완공 + 전투 Lv.90 상태에서 질주하며 직접 근접 공격하면 준비 시 360° 충격파."),
                h("R · 기동 액션"), p("기동 Lv.30 지상 돌진. Lv.60부터 공중에서도 착지 전 1회 사용 가능."),
                h("Shift · 정밀 모드"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선."),
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

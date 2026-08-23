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
                h("통합 물류 백본"), p("산업 가공소 완공 뒤에는 가까운 등록 배럴/활성 전초의 실재 재고가 산업 배치·대형 인프라 투자·장비 재련/각성에 연결됩니다. 인벤토리를 먼저 쓰고, 가까운 물류 배럴부터 인출합니다."),
                h("원정 / 현장 사건"), p("9지역은 실제 행동 지령과 희귀 사건으로 진행됩니다. 전부 완수하면 Lv.100 현장 숙련이 열립니다."),
                h("반복 원정 작전"), p("완수한 지역의 활성 전초에서 보급권1로 출발합니다. 전진선을 넘고 현지 목표를 수행한 뒤 같은 전초로 귀환해야 보상이 확정됩니다."),
                h("원정 작전 변수"), p("새 작전마다 전선 고착 / 전선 재전개 / 긴급 철수 중 하나가 붙어 현장 위치나 귀환 압박이 달라집니다. 기존 0.32 진행 작전은 변수 없음으로 유지됩니다."),
                h("현장 물류 / 전초기지"), p("실제 배럴과 침대·모닥불·작업대·화로를 묶어 전초기지를 만들며, 활성 시 물류64·NATURAL 적대몹 안전권24가 작동합니다."),
                h("현장 복귀"), p("활성 전초기지에서 보급권1로 1회 복귀 계약을 지정합니다. 같은 차원96블록 안의 일반 사망만 예약됩니다."),
                h("정점 사냥"), p("완수한 원정권에서는 지역별 행동 패턴을 가진 정점 강적9종을 반복 추적할 수 있습니다."),
                h("최종 작업 체급"), p("현장 숙련: 터널7×7×12 · 벌목448 · 수확13×13 · 충격파7.5/20 · 건축 선65/면13×13 · 공중돌진4회.")
        );
    }
    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 완주 시 현장 숙련"),
                h("Stage 0~2 원정"), p("삼림/건조/습지/고산/대양 → 심층/빙설/네더 → 엔드. 지역마다 표준/혼합 지령과 희귀 사건이 있습니다."),
                h("산업 / 물류"), p("Stage1 산업 가공소 → 4계통 생산 → 보급권 → 배럴 거점 → 물리 전초기지. 등록 배럴은 일반32블록, 활성 전초는64블록 안에서 산업/인프라/재련 투입 재고로도 사용됩니다."),
                h("원정 작전 해금"), p("해당 지역 지령 완수 + 그 지역의 활성 전초기지 필요. 보급권1. 전진선 Stage0 96 · Stage1 128 · Stage2 160블록."),
                h("작전 현장 규칙"), p("전진선 돌파 후 전초48블록 밖의 같은 원정권에서만 실제 행동이 기록됩니다. 목표 후 같은 전초8블록 안 귀환. 사망/차원이탈/시간초과는 실패."),
                h("0.33 작전 변수"), p("전선 고착=원래 전진선 밖에서만 작업 · 전선 재전개=첫 목표 뒤 +48블록 2차선 돌파 · 긴급 철수=목표 후 Stage0 4:00 / Stage1 3:00 / Stage2 2:30 귀환."),
                h("전초 승격"), p("배럴 반경5 안에 침대·모닥불·작업대·화로가 필요. 비용 보급권2+철32+금8+석탄32."),
                h("현장 복귀 계약"), p("활성 전초4블록 안에서 보급권1 선결제. 일반 사망96블록만 대상이며 사건/Apex/시련 사망은 계약을 소비하지 않습니다."),
                h("정점 / 승천 시련"), p("정점: Stage1 추적소+완수 원정권, 90초. 시련: Stage2 승천 중추, 4웨이브. 전투 입장 재료는 현장에서 직접 들고 있어야 하며 원정 작전과 중첩 시작 불가."),
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
        out.add(p("/ascension stats에서 원정/사건/반복 작전, 정점, 산업 사이클·보급권·물류·전초·현장 복귀 상태를 함께 확인합니다."));
        return out;
    }
    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("산업 생산"), p("M → 인프라 → 산업 가공소. 생산·출고·배럴 연결·전초 승격·원정 작전·현장 복귀를 같은 방사형 메뉴에서 처리합니다."),
                h("통합 물류 투입"), p("산업 배치·완공 전 인프라 투자·재련/각성은 인벤토리를 먼저 쓰고, 현재 범위 안의 사용 가능한 등록 배럴을 가까운 순서로 이어서 사용합니다. 청크 강제로드나 원격 차원 인출은 없습니다."),
                h("원정 작전 시작"), p("완수한 원정권의 활성 전초4블록 안에서 망원경 아이콘 선택. 새 출발 때 작전 변수1개가 서버에서 정해지며, 진행 중 다시 선택하면 변수/전진선/목표/남은 시간을 표시합니다."),
                h("원정 작전 변수 대응"), p("전선 고착은 전진선 밖 작업 유지, 재전개는 첫 목표 뒤 추가 전진, 긴급 철수는 두 목표 뒤 표시되는 별도 귀환 타이머를 지킵니다."),
                h("원정 작전 귀환"), p("전진선과 두 현장 목표를 끝낸 뒤 출발한 바로 그 전초8블록 안으로 돌아옵니다. 전초 구조가 실제로 유지되어야 귀환 완료됩니다."),
                h("물류 / 전초"), p("배럴 거점은 일반32블록, 활성 전초는64블록까지 실제 재고를 대량 건축/관개/산업 투입에 공급합니다. 청크 강제로드 없음."),
                h("현장 복귀 설정"), p("활성 전초4블록 안에서 나침반 아이콘 선택. 첫 지정 보급권1, 재지정 무료. 원정 작전 사망은 작전 실패지만 일반 복귀 계약은 별도 조건으로 판정됩니다."),
                h("R · 기동"), p("지상/공중 돌진. 성공한 R만 지령·사건·원정 작전의 돌진/이동 조건에 기록됩니다."),
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

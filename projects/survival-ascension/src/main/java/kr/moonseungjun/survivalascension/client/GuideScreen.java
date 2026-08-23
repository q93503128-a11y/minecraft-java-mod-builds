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
                h("물리 창고군"), p("0.37부터 등록 배럴 1개를 앵커로 두고 반경6의 실제 배럴을 최대8개까지 같은 거점에 연결합니다. 가상 슬롯이 아니라 각 배럴의 실제 인벤토리를 그대로 씁니다."),
                h("통합 물류 / 현장 일괄 적재"), p("인벤토리 우선 → 가까운 실제 앵커/창고 배럴 순으로 산업·인프라·재련 재료를 쓰며, 일괄 적재는 주 인벤토리 슬롯9~35만 반대 방향으로 저장합니다."),
                h("물리 준공 현장"), p("산업 가공소·정점 추적소·승천 중추는 마지막 자원 투입 전에 실제 배럴 중심 건축 현장을 확인합니다. 기존 완공 월드는 그대로 인정됩니다."),
                h("반복 원정 작전"), p("완수 지역의 활성 전초에서 보급권1로 출발해 전진선을 넘고 실제 현장 목표를 수행한 뒤 같은 전초로 귀환합니다."),
                h("원정 작전 변수"), p("전선 고착 / 전선 재전개 / 긴급 철수 중 하나가 새 작전에 붙습니다. 긴급 철수는 Stage0 4:00 / Stage1 3:00 / Stage2 2:30입니다."),
                h("현장 물류 / 전초기지"), p("등록 거점은 물류32, 활성 전초는64. 전초는 실제 침대·모닥불·작업대·화로를 요구하고 NATURAL 적대몹만 반경24에서 억제합니다."),
                h("최종 작업 체급"), p("현장 숙련: 터널7×7×12 · 벌목448 · 수확13×13 · 충격파7.5/20 · 건축 선65/면13×13 · 공중돌진4회.")
        );
    }

    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 완주 시 현장 숙련"),
                h("산업 가공소 준공"), p("마지막 투입: 4블록 내 실제 배럴 + 반경6 석재벽돌48 · 철블록4 · 용광로2 · 석재절단기1 · 호퍼2."),
                h("정점 추적소 준공"), p("마지막 투입: 4블록 내 자기 등록 배럴 + 반경6 석재벽돌32 · 금블록4 · 자석석1 · 지도제작대1 · 과녁4."),
                h("승천 중추 준공"), p("마지막 투입: 4블록 내 자기 등록 배럴 + 반경6 흑요석32 · 우는흑요석8 · 신호기1 · 마법부여대1 · 엔더상자1."),
                h("물류 창고군"), p("산업 가공소 완공 후 대상 실제 배럴4블록 내에서 창고 배럴 연결. 자신의 등록 앵커6블록 이내, 앵커당 최대8, 별도 보급권 없음, 다른 거점/플레이어와 중복 점유 불가."),
                h("창고군 물류 범위"), p("앵커가 일반 거점이면32, 활성 전초면64 안에서 그 앵커와 연결 배럴 모두 사용. 링크 배럴 청크가 미로딩이면 건너뛰고, 로딩된 상태에서 사라졌으면 링크만 정리합니다."),
                h("원정 작전"), p("해당 지역 지령 완수 + 활성 전초 필요. 전진선 Stage0 96 · Stage1 128 · Nether/End 160. 두 목표 뒤 같은 전초8블록 안 귀환."),
                h("0.33 작전 변수"), p("전선 고착=전진선 밖 작업 유지 · 전선 재전개=첫 목표 뒤 +48 2차선 · 긴급 철수=Stage0 4:00 / Stage1 3:00 / Stage2 2:30."),
                h("정점 / 승천 시련"), p("정점은 90초 지역별 패턴9종, 시련은4웨이브. 입장 재료는 계속 플레이어가 직접 들고 있어야 하며 원정 작전과 중첩 시작 불가."),
                h("숙련 VI / 현장"), p("기본: 채굴11×11+192 · 벌목384 · 농사11×11 · 건축49/11×11 · 공중3회. 현장: 터널12깊이 · 448 · 13×13 · 건축65/13×13 · 공중4회")
        );
    }

    private static List<Line> statsLines() {
        List<Line> out = new ArrayList<>();
        out.add(h("현재 캐릭터 숙련"));
        for (SkillType skill : SkillType.values()) {
            int level = ClientSkillState.level(skill);
            long totalXp = ClientSkillState.xp(skill);
            long into = SkillTuning.xpIntoLevel(totalXp), next = SkillTuning.xpForNextLevel(level);
            String xp = level >= SkillTuning.MAX_LEVEL ? "MAX" : into + " / " + next + " XP";
            out.add(p(skill.koreanName() + "  Lv." + level + " · " + xp + " · " + effect(skill, level)));
        }
        out.add(p("/ascension stats와 산업 생산 현황에서 원정·정점·생산·보급권·거점/창고군·전초·현장 복귀 상태를 확인합니다."));
        return out;
    }

    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("창고 배럴 연결"), p("M → 인프라 → 산업 가공소 → 창고 배럴 연결. 대상 배럴4블록 내에서 사용하며 서버가 반경6의 자신의 거점 앵커를 찾습니다. 같은 버튼으로 링크 해제."),
                h("창고군 안전 규칙"), p("실제 Barrel+Container와 mayInteract를 매 사용 때 확인합니다. 다른 사람 앵커/창고 배럴은 점유하지 못하며 청크 강제로드·원격 차원 접근·가상 저장공간은 없습니다."),
                h("현장 일괄 적재"), p("주 인벤토리 슬롯9~35의 authored 대량 자원만 가장 가까운 사용 가능 실제 배럴부터 채웁니다. 핫바0~8·장비·보조손은 유지됩니다."),
                h("통합 물류 투입"), p("산업 배치·미완공 인프라·재련/각성은 인벤토리를 먼저 쓰고 현재 범위의 실제 앵커/창고 배럴을 거리순으로 이어서 사용합니다."),
                h("시설 최종 준공"), p("산업/정점/승천의 남은 재료를 완납 가능한 순간에는 먼저 물리 준공검사. 현장이 부족하면 그 호출은 마지막 프로젝트 재료를 소비하지 않습니다."),
                h("원정 작전"), p("완수 지역 활성 전초에서 시작 → 전진선 → 두 실제 행동 목표 → 같은 전초 귀환. 사망/차원이탈/시간초과 실패, 보급권 환불 없음."),
                h("현장 복귀"), p("활성 전초4블록 안에서 보급권1 선결제. 같은 차원96블록 안 일반 사망만 대상이며 사건/Apex/시련 사망은 계약을 쓰지 않습니다."),
                h("R · 기동"), p("지상/공중 돌진. 성공한 R만 지령·사건·원정 작전의 돌진/이동 조건에 기록됩니다."),
                h("Shift · 정밀"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선합니다.")
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

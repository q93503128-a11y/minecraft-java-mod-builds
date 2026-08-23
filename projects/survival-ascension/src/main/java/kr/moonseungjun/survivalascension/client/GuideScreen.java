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
                h("SURVIVAL ASCENSION"), p("숙련이 오를수록 작업 규모가 커지고, 세계 진행·원정·인프라·강적·생산망이 그 체급을 다시 소비합니다."),
                h("전초 방어전"), p("0.38부터 활성 전초는 물류 반경만 주는 시설이 아니라 직접 지켜야 하는 선택형 전장이 됩니다. 보급권1로 3개 공세를 열고, 앵커6블록 안 적 점유를 끊어 돌파 압력을 막습니다."),
                h("물리 창고군"), p("등록 배럴 1개를 앵커로 두고 반경6 실제 배럴을 최대8개까지 같은 거점에 연결합니다. 가상 슬롯이 아니라 각 배럴의 실제 인벤토리를 그대로 씁니다."),
                h("통합 물류 / 현장 일괄 적재"), p("인벤토리 우선 → 가까운 실제 앵커/창고 배럴 순으로 산업·인프라·재련 재료를 쓰며, 일괄 적재는 주 인벤토리 슬롯9~35만 반대 방향으로 저장합니다."),
                h("물리 준공 현장"), p("산업 가공소·정점 추적소·승천 중추는 마지막 자원 투입 전에 실제 배럴 중심 건축 현장을 확인합니다. 기존 완공 월드는 그대로 인정됩니다."),
                h("반복 원정 작전"), p("완수 지역 활성 전초에서 보급권1로 출발해 전진선을 넘고 실제 현장 목표를 수행한 뒤 같은 전초로 귀환합니다."),
                h("최종 작업 체급"), p("현장 숙련: 터널7×7×12 · 벌목448 · 수확13×13 · 충격파7.5/20 · 건축 선65/면13×13 · 공중돌진4회.")
        );
    }

    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 완주 시 현장 숙련"),
                h("전초 방어전 해금"), p("Stage1 산업 가공소 + 활성 전초 필요. 앵커4블록 안에서 보급권1. 총3공세·전체4분·공세 사이3초 정비."),
                h("방어 실패 조건"), p("앵커6블록 안 적 점유가 누적되어 돌파압력200 도달, 소유자64블록 이탈/사망/게임모드 변경, 실제 전초 구조 손상, 전체4분 초과."),
                h("방어 공세"), p("Stage1은 해골/거미 → 좀비/약탈자/도끼병 → 약탈수+혼성 습격. Stage2는 수와 역할 조합이 강해지며 체력 배율을 추가하지 않습니다."),
                h("산업 가공소 준공"), p("마지막 투입: 4블록 내 실제 배럴 + 반경6 석재벽돌48 · 철블록4 · 용광로2 · 석재절단기1 · 호퍼2."),
                h("물류 창고군"), p("대상 실제 배럴4블록 내에서 창고 배럴 연결. 자신의 등록 앵커6블록 이내, 앵커당 최대8, 별도 보급권 없음, 중복 점유 불가."),
                h("원정 작전"), p("해당 지역 지령 완수 + 활성 전초 필요. 전진선 Stage0 96 · Stage1 128 · Nether/End 160. 두 목표 뒤 같은 전초8블록 안 귀환."),
                h("정점 / 승천 시련"), p("정점은90초 지역 패턴9종, 시련은4웨이브. 방어전·원정 작전과 중첩 시작하지 않습니다."),
                h("숙련 VI / 현장"), p("기본: 채굴11×11+192 · 벌목384 · 농사11×11 · 건축49/11×11 · 공중3회. 현장: 터널12깊이 · 448 · 13×13 · 건축65/13×13 · 공중4회")
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
        out.add(p("산업 생산 현황에서 보급권·거점/창고군·전초·전초 방어전·현장 복귀·원정 작전 상태를 함께 확인합니다."));
        return out;
    }

    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("전초 방어전 시작"), p("M → 인프라 → 산업 가공소 → 전초 방어전. 활성 전초 앵커4블록 안에서 사용하며 보급권1을 소비합니다. 진행 중 다시 선택하면 현재 공세/적/돌파압력/시간을 표시합니다."),
                h("전초 방어 핵심"), p("적을 멀리 쫓아가 죽이는 것보다 앵커6블록 안에 오래 머물지 못하게 하는 것이 중요합니다. 돌파압력은 침투 중 오르고 비면 빠르게 회복됩니다."),
                h("방어전 상호배제"), p("방어 중 자동 현장 사건과 정점/시련 재개방을 잠그고 새 원정 작전도 시작할 수 없습니다. 방어전 사망은 현장 복귀 계약을 소비하지 않습니다."),
                h("창고 배럴 연결"), p("대상 배럴4블록 내에서 사용하며 서버가 반경6의 자신의 거점 앵커를 찾습니다. 같은 버튼으로 링크 해제."),
                h("현장 일괄 적재"), p("주 인벤토리 슬롯9~35의 대량 자원만 가장 가까운 사용 가능 실제 배럴부터 채웁니다. 핫바0~8·장비·보조손은 유지됩니다."),
                h("시설 최종 준공"), p("산업/정점/승천의 남은 재료를 완납 가능한 순간에는 먼저 물리 준공검사. 현장이 부족하면 그 호출은 마지막 프로젝트 재료를 소비하지 않습니다."),
                h("현장 복귀"), p("활성 전초에서 보급권1 선결제. 같은 차원96블록 안 일반 사망만 대상이며 사건/방어전/Apex/시련 사망은 계약을 쓰지 않습니다."),
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

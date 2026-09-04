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
    private static final int CONTENT_TOP = 64;
    private static final int CONTENT_BOTTOM_MARGIN = 44;
    private static final int TEXT_LINE_HEIGHT = 10;
    private static final int SCROLL_STEP = 30;

    private final Screen parent;
    private final Page page;
    private double scrollOffset;
    private int maxScroll;

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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0D || maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        scrollOffset = Math.max(0.0D, Math.min(maxScroll, scrollOffset - scrollY * SCROLL_STEP));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 14, 0xFFFFFFFF, true);

        int contentWidth = Math.min(MAX_CONTENT_WIDTH, Math.max(120, this.width - CONTENT_MARGIN * 2));
        int left = (this.width - contentWidth) / 2;
        int bottom = this.height - CONTENT_BOTTOM_MARGIN;
        if (bottom <= CONTENT_TOP) {
            maxScroll = 0;
            scrollOffset = 0.0D;
            return;
        }

        List<Line> content = lines();
        int contentHeight = contentHeight(content, contentWidth);
        int viewportHeight = bottom - CONTENT_TOP;
        maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.max(0.0D, Math.min(maxScroll, scrollOffset));

        graphics.enableScissor(left - 2, CONTENT_TOP, left + contentWidth + 2, bottom);
        int y = CONTENT_TOP - (int) Math.round(scrollOffset);
        for (Line line : content) {
            int lineLeft = left + line.indent();
            int availableWidth = Math.max(80, contentWidth - line.indent() - (maxScroll > 0 ? 7 : 0));
            List<String> wrapped = wrapToWidth(line.text(), availableWidth);
            for (String segment : wrapped) {
                if (y + TEXT_LINE_HEIGHT >= CONTENT_TOP && y <= bottom) {
                    graphics.text(this.font, segment, lineLeft, y, line.color(), false);
                }
                y += TEXT_LINE_HEIGHT;
            }
            y += Math.max(3, line.gapAfter() - TEXT_LINE_HEIGHT);
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackX = left + contentWidth - 3;
            int barHeight = Math.max(18, viewportHeight * viewportHeight / Math.max(viewportHeight, contentHeight));
            int travel = Math.max(1, viewportHeight - barHeight);
            int barTop = CONTENT_TOP + (int) Math.round(scrollOffset * travel / maxScroll);
            graphics.fill(trackX, CONTENT_TOP, trackX + 2, bottom, 0x55303030);
            graphics.fill(trackX, barTop, trackX + 2, Math.min(bottom, barTop + barHeight), 0xFFB8B8B8);
        }
    }

    private int contentHeight(List<Line> content, int contentWidth) {
        int height = 0;
        for (Line line : content) {
            int availableWidth = Math.max(80, contentWidth - line.indent() - 7);
            height += wrapToWidth(line.text(), availableWidth).size() * TEXT_LINE_HEIGHT;
            height += Math.max(3, line.gapAfter() - TEXT_LINE_HEIGHT);
        }
        return height;
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
                h("SURVIVAL ASCENSION"), p("숙련이 오를수록 채굴·벌목·농사·건축·전투·기동의 실제 작업 규모가 커지고, 세계 진행·원정·인프라·강적·생산망이 그 체급을 다시 소비합니다."),
                h("월드 승천"), p("세계는 각성 단계에서 시작합니다. 위더를 처음 격파하면 전설 단계(1), 엔더 드래곤을 처음 격파하면 종말 단계(2)로 올라가며 내려가지 않습니다."),
                p("전설 단계부터 상위 인프라와 더 큰 엘리트·전술 분대가 본격적으로 열리고, 종말 단계에서는 종말 변이·승천 중추·승천 시련 등 최종 진행이 활성화됩니다."),
                h("숙련과 현장 숙련"), p("기본 숙련은 Lv.10 / 30 / 60 / 90 / 100에서 작업 규모가 크게 확장됩니다. 9개 지역 원정을 모두 처음 완수하면 Lv.100 작업의 최종 현장 숙련 체급이 열립니다."),
                h("외부 장비와 승천 각인"), p("콘텐츠 팩이나 다른 모드의 장비가 표준 검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방패 또는 머리/가슴/다리/발 방어구 태그를 사용하면 장비 메뉴의 승천 각인으로 Survival Ascension 승천 옵션 체계에 편입할 수 있습니다. 각성 단계는 정예, 전설 단계는 승천, 종말 단계는 신화 등급으로 각인합니다."),
                h("방어구 승천 옵션"), p("각인 방어구는 착용 중에만 작동합니다. 신화 한 부위 기준 수호=상시 피해 -10%, 불굴=체력 절반 이하 추가 -8%, 숙련=전투 숙련 XP +20%, 완강=8 이상 큰 피해 추가 -9%, 보호=환경 피해 추가 -12%입니다. 네 부위 합산 승천 옵션 피해 감소 상한은 70%, 숙련 XP는 최대 2배까지 적용됩니다."),
                h("원거리 전투 파급"), p("표준 활/쇠뇌의 발사체는 발사 순간 장비 옵션·Shift 정밀 상태와 발사자 귀속을 함께 기록합니다. 전투 Lv30=2.5블록/2체, Lv60=3.5/4, Lv90=4.25/6, Lv100=5/8, 현장 숙련=6블록/10체의 기본 충돌 파급이 열립니다. Shift 발사는 파급 없는 단일 정밀 타격이며 산개·연쇄·충격 옵션이 반경·대상·파급 피해를 제한적으로 확장합니다. 충돌 시 공격자 참조가 끊겨도 발사자가 온라인이면 전투 성장·처치 보상·특수 적 보상이 같은 발사자에게 유지되며, 오프라인 보상 큐는 만들지 않습니다."),
                h("방패 방어 파동"), p("표준 방패로 실제 차단에 성공하면 전투 Lv30부터 주변 적을 피해 없이 밀어내는 방어 파동이 발생합니다. Lv30=2.5블록/2체, Lv60=3.5/4, Lv90=4.5/6, Lv100=5.5/8, 현장 숙련=6.5블록/10체입니다. Shift 차단은 파동 없는 정밀 방어이며, 방패 옵션 압력·파동·대응·진압·반동은 밀침·반경·재사용·대상·수직 반동만 제한적으로 확장합니다."),
                h("메이스 충격권"), p("표준 메이스의 실제 스매시 공격은 전투 Lv30부터 바닐라 3.5블록 충격 바깥으로 승천 충격권이 확장됩니다. Lv30=4.5블록/3체, Lv60=5.5/6, Lv90=6.5/10, Lv100=7.5/14, 현장 숙련=9블록/20체입니다. 추가 충격권은 피해나 숙련 XP 없이 적·보스만 밀어내고 넉백 저항을 존중합니다. Shift 스매시는 추가 승천 충격권만 끄며 바닐라 메이스 스매시는 그대로 유지됩니다."),
                h("스피어 돌파선"), p("표준 minecraft:spears 장비는 승천 각인에 합류합니다. 전투 Lv30부터 실제 전진 속도가 있는 스피어 직접 타격은 주 대상 뒤의 좁은 돌파선에 있는 적을 피해·숙련 XP 없이 밀어냅니다. Shift 공격은 추가 돌파선 없는 정밀 타격이며 바닐라 Jab/Charge 피해·사거리·속도 계산은 그대로 유지됩니다."),
                h("삽 토공"), p("표준 삽 태그 장비는 채굴 숙련의 속도·숙련 보정을 받고, 흙·모래·자갈처럼 삽으로 캐는 블록은 숙련 면적에 맞춰 평면 토공합니다. 웅크리면 단일 블록이며 광맥/추출/터널 규칙은 삽에 적용하지 않습니다."),
                h("외부 바이옴 원정"), p("외부 월드젠 바이옴은 Survival Ascension 원정 태그를 통해 기존 9지역 판정에 합류합니다. Biomes O’ Plenty의 Glowing Grotto와 Spider Nest는 심층권이며, 외부 모드가 없어도 바닐라 원정 판정은 그대로 작동합니다."),
                h("물류 통과 창고군"), p("공용 보급고는 가까이 있을 때 별도 등록 없이 공용 재고에 합류합니다. 산업 가공소 완공 후 통 또는 공용 보급고를 물류 거점으로 등록하면 원격 물류가 열리고, 거점 주변 실제 통을 창고군으로 연결할 수 있습니다. 같은 차원에서 현재 로딩된 등록 창고는 플레이어와 거리가 멀어도 제작·건축·인프라·재련의 공용 재고로 사용됩니다. 단, 청크를 강제로 로드하지 않으므로 미로딩 창고는 건드리지 않습니다. 원정/방어전의 전초 현지 보급만은 해당 전초의 실제 재고를 요구합니다."),
                h("물류 재료 사용"), p("Survival Ascension이 자동으로 소비하는 건축·인프라·생산·재파종·장비 자재는 사용 가능한 물류 통을 먼저 쓰고, 부족한 수량만 플레이어 인벤토리에서 사용합니다."),
                h("도로·교량과 물리 화물"), p("전설 단계 토목 공사소와 건축 Lv.60을 갖추면 3폭 장거리 도로/교량을 실제 블록으로 시공합니다. 물리 화물은 출발·도착 전초의 통 앵커 반경6에 레일6개 이상, 동력레일, 호퍼, 레버 또는 레드스톤 블록이 있는 소형 하역장을 요구하며 실제 상자 광산수레로 재고를 운반합니다."),
                h("전선 화물 묶음"), p("빈 상자 광산수레에서 Shift를 누른 채 물리 화물 수레를 선택하면 일반 잡자재 대신 원정1회+전초 방어1회+요새 방어1회분만 선별합니다. 식량(밀/당근/감자/비트) 60 + 철 주괴 16 + 연료(석탄 또는 숯) 3 + 아무 종류의 통나무 12 + 석재 벽돌 32이며 출발 전초 통에서 실제로 빠지고 같은 수레로 운송됩니다. Shift 없이 선택하면 기존 일반 대량화물 적재입니다."),
                h("전선 현지 보급"), p("원정 작전·전초 방어·요새 방어는 현장 보급권과 별도로 출발 전초의 등록 통+연결 창고 통에 실물 재고가 있어야 시작합니다. 원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3, 전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12, 요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32을 씁니다. 인벤토리나 다른 거점 재고로 대체하지 않으므로 먼 전초는 직접 적재하거나 물리 화물로 보급해야 합니다."),
                h("전초와 요새 방어"), p("전초의 실제 침대·모닥불·작업대·화로와 실제 방어진이 기능의 근거입니다. 요새 최종 공세에서는 Ravager/Vindicator가 적격 방어진을 실제로 파괴할 수 있습니다."),
                h("최종 작업 체급"), p("현장 숙련: 터널7×7×12 · 벌목448 · 수확13×13 · 충격파7.5/20 · 건축 선65/도로3×65/면13×13 · 공중돌진4회. 최후의 승천 완료 후 Lv.100은 건축 선/도로65 · 벽/바닥15×15 · 공중돌진4회를 최종 권한으로 사용하며, 현장 숙련까지 갖추면 선/도로81 · 공중돌진5회로 확장됩니다.")
        );
    }

    private static List<Line> unlockLines() {
        return List.of(
                h("숙련 등급"), p("I Lv.0 · II Lv.10 · III Lv.30 · IV Lv.60 · V Lv.90 · VI Lv.100 · 9지역 원정 최초 완주 시 현장 숙련"),
                h("월드 승천 0 · 각성"), p("기본 세계 단계. 숙련과 초기 인프라를 키우며 위더 격파를 준비합니다."),
                h("월드 승천 1 · 전설"), p("위더 최초 격파로 진입합니다. 전설 단계 인프라와 상위 엘리트/전술 분대가 열리며 토목 공사소·Apex 추적소 같은 후반 기반을 사용할 수 있습니다."),
                h("월드 승천 2 · 종말"), p("엔더 드래곤 최초 격파로 진입합니다. 종말 변이와 승천 중추/승천 시련 등 최종 콘텐츠가 활성화됩니다."),
                h("최후의 승천"), p("종말 단계 · 9지역 원정 완수 · 9정점 최초 격파 · 승천 중추 완공 후 개방됩니다. 세계의 시험 → 9지역 잔향 → 붕괴 봉쇄 → 세계의 경계자 최종 관문을 연속 돌파합니다. 월드 최초 완료 후 Lv.100 최종 건축/기동 권한이 영구 개방됩니다."),
                h("승천 각인"), p("M → 장비 → 승천 각인. 승천 옵션이 없는 표준 검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비를 현재 월드 승천 단계의 정예/승천/신화 등급으로 편입합니다. 외부 모드 장비도 같은 태그를 사용하면 별도 의존성 없이 지원됩니다."),
                h("산업 가공소"), p("전설 단계 대량 생산·현장 보급·물류 거점의 기반입니다. 등록 물류 거점은 기본 통을 실제 앵커로 사용합니다."),
                h("물류 통 / 창고군"), p("산업 가공소 완공 후 4블록 내 기본 통을 거점으로 등록합니다. 거점 한도는 산업 단계 3개 → 토목 공사소 완공 후 6개 → 승천 중추 완공 후 9개이며, 거점당 반경6 실제 통 최대8개를 추가 연결할 수 있습니다."),
                h("물리 화물 / 하역장"), p("산업 가공소+토목 공사소 필요. 양쪽 활성 전초 통 앵커 반경6에 레일6+ · 동력레일1+ · 호퍼1+ · 레버/레드스톤 블록1+을 두고, 수레 주변에 동력레일·호퍼·제어가 실제로 연결된 소형 하역장을 만듭니다. 그 레일 위 상자 광산수레를 같은 차원의 다른 활성 전초까지 실제 운반합니다."),
                h("전선 화물 적재"), p("빈 수레에서 물리 화물 수레를 일반 선택하면 기존 대량자원을 적재하고, Shift+선택하면 식량176·철56·연료8·통나무32·석재벽돌128의 전선 묶음만 선별 적재합니다. 묶음은 세 전선 작전을 각각 1회 준비하는 양이며 부족하면 일부만 빼가지 않습니다."),
                h("전선 작전 재고"), p("원정=보급권1+식량32+철8+연료8, 전초 방어=보급권1+식량48+철16+통나무32, 요새 방어=보급권2+식량96+철32+석재벽돌128. 실물 비용은 정확히 출발 전초의 통 창고군에서만 차감합니다."),
                h("토목 공사소"), p("월드 승천1단계. 석재벽돌2048 · 조약돌1536 · 자갈1536 · 철256 · 구리256. 최종 투입은 자기 등록 통4블록 안의 실제 토목 준공 현장을 검증합니다."),
                h("도로/교량 모드"), p("건축 Lv.60 + 토목 공사소. 3폭 × 17칸, Lv.90 33, Lv.100 49, 현장 숙련65. 최후의 승천 완료와 현장 숙련을 모두 갖추면 81까지 열립니다. 첫 블록과 같은 블록을 수평 앞으로 시공합니다."),
                h("요새 방어전"), p("실제 방어진지 + 보급권2 + 출발 전초 현지재고. 총4공세·5분. 최종 공세는 공성 파괴자가 방어진을 열 수 있으며 일반 3공세 방어는 비파괴입니다.")
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
        out.add(p("산업 생산 현황에서 실제 물류 통/전초, 현재 전초 현지재고, 하역장 완성 여부와 근처 상자 광산수레의 일반/전선 화물 상태도 함께 확인합니다."));
        return out;
    }

    private static List<Line> controlLines() {
        return List.of(
                h("M · 통합 메뉴"), p("숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기."),
                h("휠 · 가이드 스크롤"), p("가이드 내용이 화면 높이를 넘으면 마우스 휠로 위아래를 스크롤합니다."),
                h("Shift · 정밀"), p("채굴·벌목·농사 광역 효과를 끄고 건축은 단일 배치를 우선합니다. 빈 화물 수레 적재에서는 Shift가 전선 보급 묶음 선택으로 동작합니다."),
                h("승천 각인"), p("주 손의 승천 옵션 없는 검/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비를 M → 장비 → 승천 각인으로 편입합니다. 각인 재료도 가까운 사용 가능 물류 통을 먼저 쓰고 부족분만 인벤토리에서 사용합니다."),
                h("방패 정밀/파동"), p("표준 방패로 실제 차단에 성공하면 전투 Lv30부터 주변 적을 밀어냅니다. Shift를 누른 채 막으면 광역 파동을 끄고 바닐라 차단만 유지합니다. 방패 파동 자체는 피해나 전투 숙련 XP를 주지 않습니다."),
                h("메이스 스매시"), p("메이스 스매시에서 Shift를 누르면 Survival Ascension의 추가 외곽 충격권만 끕니다. 바닐라 3.5블록 스매시 넉백과 직접 타격은 바뀌지 않습니다."),
                h("물류 재료 우선순위"), p("모드가 자동 소비하는 일반 재료는 가까운 사용 가능 물류 통 → 플레이어 인벤토리 순서입니다. 일반 바닐라 작업대 조합은 조합칸/인벤토리 규칙을 그대로 따릅니다. 단, 전선 현지 보급은 예외로 출발 전초 통 창고군만 사용합니다."),
                h("지역 물류 한도"), p("등록 거점과 전초는 산업 가공소 단계 3개, 토목 공사소 완공 후 6개, 승천 중추 완공 후 9개까지 유지할 수 있습니다. 한도는 새 거점/전초 추가에만 적용되며 실제 통·전초 구조와 로딩 조건은 그대로입니다."),
                h("전선 현지 보급"), p("원정/방어를 누르기 전에 출발 전초 통에 필요한 식량·철·연료/통나무/석재벽돌을 적치합니다. 생산 현황에서 근처 전초의 현재 수량을 확인할 수 있습니다. 부족하면 직접 넣거나 화물 수레로 옮깁니다."),
                h("화물 하역장"), p("각 화물 전초의 등록 통 앵커 반경6에 레일6개 이상, 동력레일1개 이상, 호퍼1개 이상, 레버 또는 레드스톤 블록1개 이상을 둡니다. 실제 적재·하역 수레에서 3블록 안에 동력레일과 호퍼, 4블록 안에 제어 블록이 있어야 합니다."),
                h("일반 화물 적재"), p("M → 인프라 → 산업 가공소 → 물리 화물 수레. 완성된 출발 하역장 레일 위 빈 상자 광산수레에서 Shift 없이 선택하면 그 전초의 기존 대량자원을 수레 용량만큼 적재합니다."),
                h("전선 화물 적재"), p("같은 빈 수레에서 Shift를 누른 채 물리 화물 수레를 선택하면 식량176·철56·연료8·통나무32·석재벽돌128만 선별합니다. 원정1+전초 방어1+요새 방어1회분이며 전체 묶음이 출발 전초에 없으면 아무것도 적재하지 않습니다."),
                h("물리 화물 하역"), p("적재한 바로 그 수레를 같은 차원의 다른 자신의 활성 전초 하역장까지 레일로 운반한 뒤 다시 선택합니다. 도착 통 용량 부족 시 가능한 만큼만 옮기고 잔여 화물은 수레에 남습니다."),
                h("화물 안전 경계"), p("수레/재고 순간이동, 자동 운전, 가상 노선, 강제 청크 로딩은 없습니다. 전선 묶음 표식도 수레 자체 NBT에만 있으며 실제 레일·실제 하역장·실제 수레·실제 통 재고가 전부입니다."),
                h("도로/교량 시공"), p("M → 건축 → 도로/교량. 토목 공사소 완공 후 블록 하나를 놓으면 같은 높이의 3폭 바닥을 앞으로 대량 시공합니다."),
                h("공성 파괴 대응"), p("요새 4공세의 Ravager/Vindicator가 성벽에 붙으면 적격 방어진을 파괴합니다. 드롭된 자재를 건축 선/벽 배치로 다시 메울 수 있습니다.")
        );
    }

    private static String effect(SkillType skill, int level) {
        return switch (skill) {
            case MINING -> SkillTuning.miningAreaSize(level) + "×" + SkillTuning.miningAreaSize(level) + " / 광맥 " + (SkillTuning.miningVeinLimit(level) <= 1 ? "잠김" : SkillTuning.miningVeinLimit(level));
            case WOODCUTTING -> "연쇄 " + SkillTuning.woodcuttingLogLimit(level) + "로그";
            case HARVESTING -> SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level) + " 수확";
            case FISHING -> "낚싯대 마모 방지 " + Math.round(SkillTuning.fishingRodPreservationChance(level) * 100.0D) + "%";
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

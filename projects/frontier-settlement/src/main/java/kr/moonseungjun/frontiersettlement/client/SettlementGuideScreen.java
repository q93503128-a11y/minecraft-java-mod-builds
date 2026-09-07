package kr.moonseungjun.frontiersettlement.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lightweight in-game guide: enough to play without turning Frontier into a quest checklist. */
public final class SettlementGuideScreen extends Screen {
    private static final int PAGE_COUNT = 5;
    private final Screen parent;
    private final int page;
    private int panelX, panelY, panelWidth, panelHeight;

    public SettlementGuideScreen(Screen parent, int page) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Frontier 가이드"));
        this.parent = parent;
        this.page = Math.max(0, Math.min(PAGE_COUNT - 1, page));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(580, Math.max(300, this.width - FrontierUiTheme.L));
        panelHeight = Math.min(294, Math.max(220, this.height - FrontierUiTheme.L));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(FrontierUiTheme.S, (this.height - panelHeight) / 2);
        int y = panelY + panelHeight - 30;
        if (page > 0) addRenderableWidget(Button.builder(Component.literal("이전"),
                b -> this.minecraft.gui.setScreen(new SettlementGuideScreen(parent, page - 1)))
                .bounds(panelX + FrontierUiTheme.M, y, 58, 20).build());
        if (page < PAGE_COUNT - 1) addRenderableWidget(Button.builder(Component.literal("다음"),
                b -> this.minecraft.gui.setScreen(new SettlementGuideScreen(parent, page + 1)))
                .bounds(panelX + FrontierUiTheme.M + 64, y, 58, 20).build());
        addRenderableWidget(Button.builder(Component.literal("돌아가기"), b -> this.minecraft.gui.setScreen(parent))
                .bounds(panelX + panelWidth - 82, y, 68, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float p) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
        FrontierUiTheme.panel(g, panelX, panelY, panelWidth, panelHeight);
        int x = panelX + FrontierUiTheme.M;
        int y = panelY + FrontierUiTheme.M;
        g.text(this.font, Component.literal("FRONTIER GUIDE"), x, y, FrontierUiTheme.ACCENT, true);
        String pageText = (page + 1) + " / " + PAGE_COUNT;
        g.text(this.font, Component.literal(pageText), panelX + panelWidth - FrontierUiTheme.M - this.font.width(pageText),
                y, FrontierUiTheme.TEXT_MUTED, false);
        drawPageProgress(g, x, y + 14, panelWidth - FrontierUiTheme.M * 2);

        switch (page) {
            case 0 -> draw(g, x, y, "1. 개척지 시작",
                    "M → ‘현재 위치에 개척지 세우기’를 누릅니다.",
                    "표식과 54칸 공동 보급고가 실제 월드에 생성됩니다.",
                    "실패하면 평평하고 빈 오버월드 지면으로 이동해 다시 시도하세요.",
                    "명령어는 필요 없습니다.");
            case 1 -> draw(g, x, y, "2. 자원과 건설",
                    "공동 보급고와 연결된 저장소에 목재·돌·금속·음식을 넣습니다.",
                    "HUD 숫자는 실제 저장 아이템을 집계한 값입니다.",
                    "M → 건물 선택 / R 회전 / Enter 확정.",
                    "주민이 실제 재료를 운반해 실제 블록으로 건설합니다.");
            case 2 -> draw(g, x, y, "3. 초반 성장",
                    "권장 순서: 주택 → 벌목소 → 농장 → 채석장 → 창고.",
                    "마을 단계는 생산시설 II~IV 개량 한도를 열며, 시설별 자원 투자가 필요합니다.",
                    "빈손으로 웅크린 채 생산시설 현장 저장통을 우클릭하면 다음 개량을 진행합니다.",
                    "다음 성장 조건은 M 화면의 마을 등급 바로 아래에서 확인합니다.");
            case 3 -> draw(g, x, y, "4. 영토와 물류",
                    "M → 인프라 → 거점 위치에서 본진·전초 좌표와 방향을 확인합니다.",
                    "창고·수레 정거장은 빈손 웅크리기+저장통 우클릭으로 물류 II~III를 확장합니다.",
                    "벌목·농업·채석·광업 전초를 다양화하면 영지망이 최대 III까지 성장합니다.",
                    "영지망은 시장·수리·제작·생산 화물을 강화하며 언로드 지역은 강제 로드하지 않습니다.");
            default -> draw(g, x, y, "5. 영지와 개척 수도",
                    "개척 도시부터 시민회관, 영지부터 교역회관·성채가 열립니다.",
                    "시민회관 강단·교역회관 종을 빈손 웅크려 우클릭하면 도시 투자를 진행합니다.",
                    "도시 투자는 실물 목재·돌·구리/철을 크게 소비해 행정·교역 효율을 높입니다.",
                    "등급 승격과 도시는 별개입니다. 조건을 채우면 개척 수도가 완성됩니다.");
        }
        super.extractRenderState(g, mx, my, p);
    }

    private void drawPageProgress(GuiGraphicsExtractor g, int x, int y, int width) {
        int gap = FrontierUiTheme.XS;
        int segment = Math.max(12, (width - gap * (PAGE_COUNT - 1)) / PAGE_COUNT);
        for (int i = 0; i < PAGE_COUNT; i++) {
            int sx = x + i * (segment + gap);
            g.fill(sx, y, sx + segment, y + 3, i <= page ? FrontierUiTheme.PRIMARY : FrontierUiTheme.TRACK);
        }
    }

    private void draw(GuiGraphicsExtractor g, int x, int y, String title, String a, String b, String c, String d) {
        int titleY = y + 29;
        g.text(this.font, Component.literal(title), x, titleY, FrontierUiTheme.TEXT_PRIMARY, true);
        int surfaceY = titleY + 20;
        FrontierUiTheme.surface(g, x, surfaceY, panelWidth - FrontierUiTheme.M * 2, 104);
        int tx = x + FrontierUiTheme.M;
        g.text(this.font, Component.literal(a), tx, surfaceY + 12, FrontierUiTheme.TEXT_PRIMARY, false);
        g.text(this.font, Component.literal(b), tx, surfaceY + 31, FrontierUiTheme.TEXT_SECONDARY, false);
        g.text(this.font, Component.literal(c), tx, surfaceY + 50, FrontierUiTheme.TEXT_SECONDARY, false);
        FrontierUiTheme.divider(g, tx, surfaceY + 69, panelWidth - FrontierUiTheme.M * 4);
        g.text(this.font, Component.literal(d), tx, surfaceY + 80, FrontierUiTheme.WARNING, false);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}

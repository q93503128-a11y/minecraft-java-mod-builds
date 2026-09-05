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
        panelWidth = Math.min(560, Math.max(300, this.width - 16));
        panelHeight = Math.min(286, Math.max(220, this.height - 16));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(8, (this.height - panelHeight) / 2);
        int y = panelY + panelHeight - 30;
        if (page > 0) addRenderableWidget(Button.builder(Component.literal("이전"),
                b -> this.minecraft.gui.setScreen(new SettlementGuideScreen(parent, page - 1)))
                .bounds(panelX + 14, y, 58, 20).build());
        if (page < PAGE_COUNT - 1) addRenderableWidget(Button.builder(Component.literal("다음"),
                b -> this.minecraft.gui.setScreen(new SettlementGuideScreen(parent, page + 1)))
                .bounds(panelX + 78, y, 58, 20).build());
        addRenderableWidget(Button.builder(Component.literal("돌아가기"), b -> this.minecraft.gui.setScreen(parent))
                .bounds(panelX + panelWidth - 82, y, 68, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float p) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0121418);
        g.fill(panelX, panelY, panelX + 4, panelY + panelHeight, 0xFFD0A45C);
        int x = panelX + 16, y = panelY + 14;
        g.text(this.font, Component.literal("FRONTIER GUIDE   " + (page + 1) + " / " + PAGE_COUNT), x, y, 0xFFD0A45C, true);
        switch (page) {
            case 0 -> draw(g, x, y, "1. 개척지 시작",
                    "M → ‘현재 위치에 개척지 세우기’를 누릅니다.",
                    "표식과 공동 창고가 실제 월드에 생성됩니다.",
                    "실패하면 평평하고 빈 지면으로 이동해 다시 시도하세요.",
                    "명령어는 필요 없습니다.");
            case 1 -> draw(g, x, y, "2. 자원과 건설",
                    "공동 창고에 목재·돌·금속·음식을 넣습니다.",
                    "HUD 숫자는 창고의 실제 아이템을 집계한 값입니다.",
                    "M → 건물 선택 / R 회전 / Enter 확정.",
                    "주민이 재료를 운반해 실제 블록으로 건설합니다.");
            case 2 -> draw(g, x, y, "3. 초반 성장",
                    "권장 순서: 주택 → 벌목소 → 농장 → 채석장 → 창고.",
                    "생산시설은 반복 건설만 강요하지 않고 마을 단계에 따라 기존 시설이 자동 개량됩니다.",
                    "농장 주민은 작물을 직접 관리해 바닐라 랜덤 성장만 기다리지 않습니다.",
                    "HUD의 노란 ‘다음 목표’를 따라가면 해금 흐름이 이어집니다.");
            case 3 -> draw(g, x, y, "4. 영토와 물류",
                    "M → 인프라 → 거점 위치에서 본진·전초 좌표와 방향을 확인합니다.",
                    "도로 끝에 전초기지를 세워 영토·생산 거점을 넓힙니다.",
                    "체크포인트를 바꿔도 거점 저장 좌표는 사라지지 않습니다.",
                    "언로드 지역은 강제로 로드하지 않으며 운송도 멈춥니다.");
            default -> draw(g, x, y, "5. 영지와 개척 수도",
                    "개척 도시부터 시민회관, 영지부터 교역회관·성채가 열립니다.",
                    "교역회관은 기존 유물 교역 가치를 높이고 성채는 감시망을 넓힙니다.",
                    "인구 20 · 전초 5 · 도로 4 · 탐험 7과 랜드마크 3종을 완성하세요.",
                    "조건을 모두 채우면 최종 단계 ‘개척 수도’가 완성됩니다.");
        }
        super.extractRenderState(g, mx, my, p);
    }

    private void draw(GuiGraphicsExtractor g, int x, int y, String title, String a, String b, String c, String d) {
        g.text(this.font, Component.literal(title), x, y + 25, 0xFFFFFFFF, true);
        g.fill(x - 4, y + 43, panelX + panelWidth - 14, y + 130, 0x701F2328);
        g.text(this.font, Component.literal(a), x + 4, y + 52, 0xFFE7E0D3, false);
        g.text(this.font, Component.literal(b), x + 4, y + 70, 0xFFE7E0D3, false);
        g.text(this.font, Component.literal(c), x + 4, y + 88, 0xFFE7E0D3, false);
        g.text(this.font, Component.literal(d), x + 4, y + 106, 0xFFFFD58A, false);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}

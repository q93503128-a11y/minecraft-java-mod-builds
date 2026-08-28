package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Category-first construction palette; layout/assets are Frontier-owned. */
public final class BuildingPaletteScreen extends Screen {
    private enum Category {
        FOUNDATION("기반", "주거와 저장 기반", List.of(BuildingType.HOUSE, BuildingType.WAREHOUSE)),
        PRODUCTION("생산", "목재·식량·석재·광물", List.of(BuildingType.LUMBER_CAMP, BuildingType.FARM, BuildingType.QUARRY, BuildingType.MINE)),
        SERVICES("제작·서비스", "제작, 건설 지원, 교역과 운송", List.of(
                BuildingType.CONSTRUCTION_OFFICE, BuildingType.BLACKSMITH, BuildingType.WORKSHOP,
                BuildingType.ADVANCED_WORKSHOP, BuildingType.MARKET, BuildingType.CART_STATION)),
        DEFENSE("방어", "경계 감시와 주둔 병력", List.of(BuildingType.GUARD_POST, BuildingType.WATCHTOWER, BuildingType.BARRACKS)),
        LANDMARKS("랜드마크", "중후반 도시 기능과 최종 목표", List.of(BuildingType.CIVIC_HALL, BuildingType.TRADE_HALL, BuildingType.CITADEL)),
        INFRA("인프라", "도로, 전초기지, 영지 토목", List.of());

        final String label;
        final String description;
        final List<BuildingType> buildings;
        Category(String label, String description, List<BuildingType> buildings) {
            this.label = label; this.description = description; this.buildings = buildings;
        }
    }

    private final Category category;
    private int panelX, panelY, panelWidth, panelHeight, sidebarWidth;

    public BuildingPaletteScreen() { this(Category.FOUNDATION); }
    private BuildingPaletteScreen(Category category) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("마을 건설"));
        this.category = category;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(620, Math.max(320, this.width - 16));
        panelHeight = Math.min(330, Math.max(220, this.height - 16));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(8, (this.height - panelHeight) / 2);
        sidebarWidth = panelWidth < 430 ? 88 : 118;

        int sidebarX = panelX + 10;
        int y = panelY + 46;
        int sidebarButtonWidth = sidebarWidth - 12;
        for (Category value : Category.values()) {
            String prefix = value == category ? "◆ " : "";
            addRenderableWidget(Button.builder(Component.literal(prefix + value.label),
                    b -> this.minecraft.gui.setScreen(new BuildingPaletteScreen(value)))
                    .bounds(sidebarX, y, sidebarButtonWidth, 18).build());
            y += 22;
        }

        int contentX = panelX + sidebarWidth + 12;
        int contentWidth = panelWidth - sidebarWidth - 24;
        int contentY = panelY + 72;
        if (category == Category.INFRA) addInfrastructure(contentX, contentY, contentWidth);
        else {
            int cardHeight = panelHeight < 290 ? 19 : 24;
            int step = cardHeight + 4;
            int cardY = contentY;
            for (BuildingType type : category.buildings) {
                addBuilding(type, contentX, cardY, contentWidth, cardHeight);
                cardY += step;
            }
        }

        addRenderableWidget(Button.builder(Component.literal("가이드"),
                b -> this.minecraft.gui.setScreen(new SettlementGuideScreen(this, 0)))
                .bounds(panelX + panelWidth - 126, panelY + 10, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose())
                .bounds(panelX + panelWidth - 60, panelY + 10, 46, 20).build());
    }

    private void addBuilding(BuildingType type, int x, int y, int width, int height) {
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        boolean unlocked = (data.buildingUnlockMask() & (1 << type.ordinal())) != 0;
        boolean affordable = data.wood() >= type.woodCost() && data.stone() >= type.stoneCost();
        String state = unlocked ? (affordable ? "건설 가능" : "자원 부족") : "잠김";
        String label = type.displayName() + "  목 " + type.woodCost() + " · 석 " + type.stoneCost() + "  [" + state + "]";
        Button button = Button.builder(Component.literal(label), c -> {
            BuildingPlacementClient.beginPlacement(type);
            this.minecraft.gui.setScreen(null);
        }).bounds(x, y, width, height).build();
        button.active = unlocked;
        addRenderableWidget(button);
    }

    private void addInfrastructure(int x, int y, int width) {
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        addRenderableWidget(Button.builder(Component.literal("도로 계획 · 마을-거점 연결"),
                b -> { RoadPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y, width, 22).build());
        addRenderableWidget(Button.builder(Component.literal("전초기지 · 영토/생산 확장"),
                b -> { OutpostPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y + 29, width, 22).build());
        boolean civilUnlocked = data.tier().equals("영지") || data.tier().equals("개척 수도");
        Button civil = Button.builder(Component.literal(
                        civilUnlocked ? "토목 평탄화 · 절토/성토" : "토목 평탄화 [영지 잠김]"),
                b -> { CivilWorkPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y + 58, width, 22).build();
        civil.active = civilUnlocked;
        addRenderableWidget(civil);
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float p) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0121418);
        g.fill(panelX, panelY, panelX + 4, panelY + panelHeight, 0xFFD0A45C);
        g.fill(panelX + sidebarWidth + 2, panelY + 40, panelX + sidebarWidth + 3, panelY + panelHeight - 10, 0x505A5144);
        g.text(this.font, Component.literal("마을 건설"), panelX + 12, panelY + 14, 0xFFFFFFFF, true);
        String resources = data.tier() + "  목 " + data.wood() + " 석 " + data.stone() + " 금 " + data.metal()
                + " 식 " + data.food() + " 인구 " + data.population();
        g.text(this.font, Component.literal(resources), panelX + 12, panelY + 29, 0xFFBEB7AA, false);

        int contentX = panelX + sidebarWidth + 12;
        g.text(this.font, Component.literal(category.label), contentX, panelY + 44, 0xFFFFD58A, true);
        g.text(this.font, Component.literal(category.description), contentX, panelY + 57, 0xFFAFA99E, false);
        if (panelHeight >= 245) {
            g.text(this.font, Component.literal("M 닫기/다시 열기 · 배치 후 R 회전 / Enter 확정"),
                    contentX, panelY + panelHeight - 16, 0xFF8F8A82, false);
        }
        super.extractRenderState(g, mx, my, p);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}

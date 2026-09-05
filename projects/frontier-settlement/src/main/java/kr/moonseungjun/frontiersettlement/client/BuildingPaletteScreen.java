package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import kr.moonseungjun.frontiersettlement.network.SettlementContextTarget;
import net.minecraft.world.level.Level;
import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Category-first construction palette; layout/assets are Frontier-owned. */
public final class BuildingPaletteScreen extends Screen {
    private enum Category {
        FOUNDATION("기반", "주거와 저장 기반", List.of(BuildingType.HOUSE, BuildingType.WAREHOUSE)),
        PRODUCTION("생산", "목재·식량·석재·광물 · 마을 단계에 따라 기존 시설 자동 개량", List.of(BuildingType.LUMBER_CAMP, BuildingType.FARM, BuildingType.QUARRY, BuildingType.MINE)),
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
            this.label = label;
            this.description = description;
            this.buildings = buildings;
        }
    }

    private record BuildingRow(BuildingType type, int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private static final int PANEL_BG = 0xF0121418;
    private static final int PANEL_EDGE = 0xFFD0A45C;
    private static final int DETAIL_BG = 0xA81A1D21;
    private static final int DIVIDER = 0x705A5144;
    private static final int TEXT_PRIMARY = 0xFFF4F1EA;
    private static final int TEXT_SECONDARY = 0xFFBEB7AA;
    private static final int TEXT_MUTED = 0xFF918B82;
    private static final int TEXT_ACCENT = 0xFFFFD58A;
    private static final int TEXT_GOOD = 0xFFAEDC9A;
    private static final int TEXT_WARN = 0xFFFFC878;

    private final Category category;
    private final List<BuildingRow> buildingRows = new ArrayList<>();
    private int panelX, panelY, panelWidth, panelHeight, sidebarWidth;
    private int contentX, contentWidth, contentY;
    private boolean splitDetails;

    public BuildingPaletteScreen() {
        this(Category.FOUNDATION);
    }

    private BuildingPaletteScreen(Category category) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("마을 건설"));
        this.category = category;
    }

    @Override
    protected void init() {
        buildingRows.clear();
        panelWidth = Math.min(620, Math.max(320, this.width - 16));
        panelHeight = Math.min(330, Math.max(220, this.height - 16));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(8, (this.height - panelHeight) / 2);
        sidebarWidth = panelWidth < 430 ? 88 : 118;

        int sidebarX = panelX + 10;
        int y = panelY + 48;
        int sidebarButtonWidth = sidebarWidth - 12;
        for (Category value : Category.values()) {
            String prefix = value == category ? "◆ " : "";
            addRenderableWidget(Button.builder(Component.literal(prefix + value.label),
                    b -> this.minecraft.gui.setScreen(new BuildingPaletteScreen(value)))
                    .bounds(sidebarX, y, sidebarButtonWidth, 18).build());
            y += 22;
        }

        contentX = panelX + sidebarWidth + 13;
        contentWidth = panelWidth - sidebarWidth - 25;
        contentY = panelY + 75;
        splitDetails = panelWidth >= 520 && category != Category.INFRA;

        if (category == Category.INFRA) {
            addInfrastructure(contentX, contentY, contentWidth);
        } else {
            int listWidth = splitDetails ? Math.min(216, Math.max(178, contentWidth * 46 / 100)) : contentWidth;
            int cardHeight = panelHeight < 290 ? 20 : 24;
            int step = cardHeight + 4;
            int cardY = contentY;
            for (BuildingType type : category.buildings) {
                addBuilding(type, contentX, cardY, listWidth, cardHeight);
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
        boolean unlocked = isUnlocked(data, type);
        boolean affordable = isAffordable(data, type);

        String suffix;
        if (!unlocked) suffix = "  · 잠김";
        else if (!affordable) suffix = "  · 자원 부족";
        else suffix = "";

        String label = splitDetails
                ? type.displayName() + suffix
                : type.displayName() + "   목 " + type.woodCost() + " · 석 " + type.stoneCost() + suffix;

        Button button = Button.builder(Component.literal(label), c -> {
            BuildingPlacementClient.beginPlacement(type);
            this.minecraft.gui.setScreen(null);
        }).bounds(x, y, width, height).build();
        button.active = unlocked;
        addRenderableWidget(button);
        buildingRows.add(new BuildingRow(type, x, y, width, height));
    }

    private void addInfrastructure(int x, int y, int width) {
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        addRenderableWidget(Button.builder(Component.literal("도로 계획   · 마을과 거점 연결"),
                b -> { RoadPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y, width, 23).build());
        addRenderableWidget(Button.builder(Component.literal("전초기지   · 영토와 생산권 확장"),
                b -> { OutpostPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y + 31, width, 23).build());
        addRenderableWidget(Button.builder(Component.literal("토목 평탄화   · 절토/성토"),
                b -> { CivilWorkPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y + 62, width, 23).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int x, int y, float p) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
        g.fill(panelX, panelY, panelX + 4, panelY + panelHeight, PANEL_EDGE);
        g.fill(panelX + sidebarWidth + 2, panelY + 41, panelX + sidebarWidth + 3, panelY + panelHeight - 10, DIVIDER);

        g.text(this.font, Component.literal("마을 건설"), panelX + 12, panelY + 13, TEXT_PRIMARY, true);
        String resources = data.tier() + "   목재 " + data.wood() + "   석재 " + data.stone()
                + "   금속 " + data.metal() + "   식량 " + data.food() + "   인구 " + data.population();
        g.text(this.font, Component.literal(resources), panelX + 12, panelY + 29, TEXT_SECONDARY, false);

        g.text(this.font, Component.literal(category.label), contentX, panelY + 46, TEXT_ACCENT, true);
        g.text(this.font, Component.literal(category.description), contentX, panelY + 59, TEXT_SECONDARY, false);

        if (splitDetails && !category.buildings.isEmpty()) {
            BuildingType detail = hoveredBuilding(mx, my);
            if (detail == null) detail = category.buildings.get(0);
            drawBuildingDetail(g, data, detail);
        } else if (category == Category.INFRA && panelHeight >= 270) {
            int infoY = contentY + 102;
            g.fill(contentX, infoY, contentX + contentWidth, infoY + 1, DIVIDER);
            g.text(this.font, Component.literal("인프라는 건물보다 월드의 연결 관계가 중요합니다."),
                    contentX, infoY + 9, TEXT_SECONDARY, false);
            g.text(this.font, Component.literal("메뉴에서 작업을 고른 뒤 월드 프리뷰로 위치와 범위를 확인하세요."),
                    contentX, infoY + 22, TEXT_MUTED, false);
            drawSettlementLocations(g, infoY + 39);
        }

        if (panelHeight >= 245) {
            g.text(this.font, Component.literal("M 닫기/다시 열기   ·   배치: R 회전 / Enter 확정"),
                    contentX, panelY + panelHeight - 16, TEXT_MUTED, false);
        }
        super.extractRenderState(g, mx, my, p);
    }

    private void drawSettlementLocations(GuiGraphicsExtractor g, int startY) {
        List<SettlementContextTarget> bases = new ArrayList<>();
        for (SettlementContextTarget target : ClientSettlementState.context().targets()) {
            if ("settlement".equals(target.kind()) || "outpost".equals(target.kind())) bases.add(target);
        }
        if (bases.isEmpty()) {
            g.text(this.font, Component.literal("거점 위치 동기화 대기 중…"), contentX, startY, TEXT_MUTED, false);
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        boolean overworld = minecraft.level != null && minecraft.level.dimension().equals(Level.OVERWORLD);
        if (player != null && overworld) {
            bases.sort((a, b) -> Long.compare(distanceSq(player.getX(), player.getZ(), a), distanceSq(player.getX(), player.getZ(), b)));
        } else {
            bases.sort((a, b) -> {
                if ("settlement".equals(a.kind())) return -1;
                if ("settlement".equals(b.kind())) return 1;
                return Integer.compare(a.markerX(), b.markerX());
            });
        }

        int bottom = panelY + panelHeight - 27;
        int maxRows = Math.max(1, (bottom - startY) / 11);
        int visible = Math.min(bases.size(), maxRows);
        for (int i = 0; i < visible; i++) {
            SettlementContextTarget target = bases.get(i);
            String label = "settlement".equals(target.kind()) ? "본진" : target.title();
            String line = label + " · X " + target.markerX() + " Y " + target.markerY() + " Z " + target.markerZ();
            if (player != null && overworld) {
                long dx = Math.round(target.markerX() + 0.5D - player.getX());
                long dz = Math.round(target.markerZ() + 0.5D - player.getZ());
                long distance = Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
                line += " · " + distance + "블록 " + directionName(dx, dz);
            } else {
                line += " · 오버월드";
            }
            if (i == visible - 1 && bases.size() > visible) {
                line += " · 외 " + (bases.size() - visible) + "곳";
            }
            line = trimToWidth(line, contentWidth);
            int color = "settlement".equals(target.kind()) ? TEXT_ACCENT : TEXT_SECONDARY;
            g.text(this.font, Component.literal(line), contentX, startY + i * 11, color, false);
        }
    }

    private static long distanceSq(double x, double z, SettlementContextTarget target) {
        long dx = Math.round(target.markerX() + 0.5D - x);
        long dz = Math.round(target.markerZ() + 0.5D - z);
        return dx * dx + dz * dz;
    }

    private static String directionName(long dx, long dz) {
        if (dx == 0L && dz == 0L) return "현재 위치";
        String[] names = {"북", "북동", "동", "남동", "남", "남서", "서", "북서"};
        double angle = Math.atan2((double) dx, (double) -dz);
        int index = (int) Math.round(angle / (Math.PI / 4.0D));
        index = Math.floorMod(index, 8);
        return names[index];
    }

    private String trimToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "…";
        String out = text;
        while (!out.isEmpty() && this.font.width(out + suffix) > maxWidth) out = out.substring(0, out.length() - 1);
        return out + suffix;
    }

    private void drawBuildingDetail(GuiGraphicsExtractor g, SettlementSnapshotPayload data, BuildingType type) {
        int listWidth = Math.min(216, Math.max(178, contentWidth * 46 / 100));
        int detailX = contentX + listWidth + 12;
        int detailWidth = contentX + contentWidth - detailX;
        int detailY = contentY;
        int detailBottom = panelY + panelHeight - 30;

        g.fill(detailX, detailY, detailX + detailWidth, detailBottom, DETAIL_BG);
        g.fill(detailX, detailY, detailX + 2, detailBottom, 0xFF806B4D);

        boolean unlocked = isUnlocked(data, type);
        boolean affordable = isAffordable(data, type);
        String status = !unlocked ? "잠김" : affordable ? "건설 가능" : "자원 부족";
        int statusColor = !unlocked ? TEXT_MUTED : affordable ? TEXT_GOOD : TEXT_WARN;

        int x = detailX + 10;
        int y = detailY + 10;
        g.text(this.font, Component.literal(type.displayName()), x, y, TEXT_PRIMARY, true);
        int statusX = detailX + detailWidth - 10 - this.font.width(status);
        g.text(this.font, Component.literal(status), statusX, y, statusColor, false);

        y += 19;
        g.fill(x, y - 4, detailX + detailWidth - 10, y - 3, DIVIDER);
        g.text(this.font, Component.literal("부지"), x, y, TEXT_MUTED, false);
        g.text(this.font, Component.literal(type.width() + " × " + type.depth() + "   높이 " + type.clearHeight()), x + 37, y, TEXT_PRIMARY, false);

        y += 15;
        g.text(this.font, Component.literal("비용"), x, y, TEXT_MUTED, false);
        g.text(this.font, Component.literal("목재 " + type.woodCost() + "   석재 " + type.stoneCost()), x + 37, y,
                affordable ? TEXT_PRIMARY : TEXT_WARN, false);

        if (type.housingGain() > 0) {
            y += 15;
            g.text(this.font, Component.literal("주거"), x, y, TEXT_MUTED, false);
            g.text(this.font, Component.literal("인구 수용 +" + type.housingGain()), x + 37, y, TEXT_PRIMARY, false);
        }

        if (!type.unlockHint().isBlank()) {
            y += 18;
            g.fill(x, y - 4, detailX + detailWidth - 10, y - 3, DIVIDER);
            g.text(this.font, Component.literal("해금 조건"), x, y, TEXT_ACCENT, false);
            y += 13;
            g.text(this.font, Component.literal(type.unlockHint()), x, y, unlocked ? TEXT_SECONDARY : TEXT_WARN, false);
        }

        int footerY = detailBottom - 24;
        g.text(this.font, Component.literal(unlocked ? "클릭하면 월드 배치 모드로 전환" : "조건을 달성하면 배치 가능"),
                x, footerY, unlocked ? TEXT_SECONDARY : TEXT_MUTED, false);
    }

    private BuildingType hoveredBuilding(int mouseX, int mouseY) {
        for (BuildingRow row : buildingRows) {
            if (row.contains(mouseX, mouseY)) return row.type();
        }
        return null;
    }

    private static boolean isUnlocked(SettlementSnapshotPayload data, BuildingType type) {
        return (data.buildingUnlockMask() & (1 << type.ordinal())) != 0;
    }

    private static boolean isAffordable(SettlementSnapshotPayload data, BuildingType type) {
        return data.wood() >= type.woodCost() && data.stone() >= type.stoneCost();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean isInGameUi() { return true; }
}

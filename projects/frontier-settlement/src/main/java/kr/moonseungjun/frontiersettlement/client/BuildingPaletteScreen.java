package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Settlement command palette.
 *
 * Alpha.117 follows the repository UI quality standard: settlement status first, compact resource
 * authority second, one construction-family navigation row, then a choice/detail workspace. The
 * screen deliberately remains a vanilla Screen because the interaction is bounded and does not
 * justify a new UI framework dependency.
 */
public final class BuildingPaletteScreen extends Screen {
    private enum Category {
        FOUNDATION("기반", "기반", "주거와 저장 기반", List.of(BuildingType.HOUSE, BuildingType.WAREHOUSE)),
        PRODUCTION("생산", "생산", "목재·식량·석재·광물 생산", List.of(
                BuildingType.LUMBER_CAMP, BuildingType.FARM, BuildingType.QUARRY, BuildingType.MINE)),
        SERVICES("제작·서비스", "서비스", "건설 지원, 제작, 교역과 운송", List.of(
                BuildingType.CONSTRUCTION_OFFICE, BuildingType.BLACKSMITH, BuildingType.WORKSHOP,
                BuildingType.ADVANCED_WORKSHOP, BuildingType.MARKET, BuildingType.CART_STATION)),
        DEFENSE("방어", "방어", "경계 감시와 주둔 병력", List.of(
                BuildingType.GUARD_POST, BuildingType.WATCHTOWER, BuildingType.BARRACKS)),
        LANDMARKS("랜드마크", "명소", "중후반 도시 기능과 최종 목표", List.of(
                BuildingType.CIVIC_HALL, BuildingType.TRADE_HALL, BuildingType.CITADEL)),
        INFRA("인프라", "인프라", "도로, 전초기지, 영지 토목", List.of());

        final String label;
        final String compactLabel;
        final String description;
        final List<BuildingType> buildings;

        Category(String label, String compactLabel, String description, List<BuildingType> buildings) {
            this.label = label;
            this.compactLabel = compactLabel;
            this.description = description;
            this.buildings = buildings;
        }
    }

    private record BuildingRow(BuildingType type, int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record CategoryTab(Category category, int x, int y, int width, int height) {}

    private static final int SETTLEMENT_TIER_COUNT = 6;

    private final Category category;
    private final List<BuildingRow> buildingRows = new ArrayList<>();
    private final List<CategoryTab> categoryTabs = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int innerX;
    private int innerWidth;
    private int contentY;
    private int contentBottom;
    private int listWidth;
    private boolean splitDetails;
    private boolean compact;

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
        categoryTabs.clear();

        panelWidth = Math.min(700, Math.max(300, this.width - FrontierUiTheme.M));
        panelHeight = Math.min(370, Math.max(180, this.height - FrontierUiTheme.M));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        innerX = panelX + FrontierUiTheme.M;
        innerWidth = panelWidth - FrontierUiTheme.M * 2;
        compact = panelWidth < 520 || panelHeight < 290;
        splitDetails = panelWidth >= 560 && panelHeight >= 300 && category != Category.INFRA;

        addHeaderActions();
        int tabsY = panelY + (compact ? 78 : 86);
        addCategoryTabs(tabsY);
        contentY = tabsY + 20 + FrontierUiTheme.S;
        contentBottom = panelY + panelHeight - (compact ? 24 : 30);

        if (category == Category.INFRA) {
            addInfrastructure();
        } else {
            addBuildingList();
        }
    }

    private void addHeaderActions() {
        int closeWidth = compact ? 42 : 48;
        int guideWidth = compact ? 46 : 58;
        int operationsWidth = compact ? 42 : 52;
        int y = panelY + FrontierUiTheme.S;
        int closeX = panelX + panelWidth - FrontierUiTheme.S - closeWidth;
        int guideX = closeX - FrontierUiTheme.XS - guideWidth;
        int operationsX = guideX - FrontierUiTheme.XS - operationsWidth;
        addRenderableWidget(Button.builder(Component.literal("운영"),
                b -> this.minecraft.gui.setScreen(new SettlementOperationsScreen(this)))
                .bounds(operationsX, y, operationsWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal(compact ? "도움" : "가이드"),
                b -> this.minecraft.gui.setScreen(new SettlementGuideScreen(this, 0)))
                .bounds(guideX, y, guideWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose())
                .bounds(closeX, y, closeWidth, 18).build());
    }

    private void addCategoryTabs(int tabsY) {
        int gap = FrontierUiTheme.XS;
        int count = Category.values().length;
        int tabWidth = (innerWidth - gap * (count - 1)) / count;
        int x = innerX;
        for (Category value : Category.values()) {
            String label = compact ? value.compactLabel : value.label;
            Button tab = Button.builder(Component.literal(label),
                    b -> this.minecraft.gui.setScreen(new BuildingPaletteScreen(value)))
                    .bounds(x, tabsY, tabWidth, 20).build();
            addRenderableWidget(tab);
            categoryTabs.add(new CategoryTab(value, x, tabsY, tabWidth, 20));
            x += tabWidth + gap;
        }
    }

    private void addBuildingList() {
        int gap = FrontierUiTheme.XS;
        listWidth = splitDetails ? Math.max(210, innerWidth * 43 / 100) : innerWidth;
        int available = Math.max(1, contentBottom - contentY);
        int count = Math.max(1, category.buildings.size());
        int cardHeight = Math.max(17, Math.min(24, (available - gap * (count - 1)) / count));
        int y = contentY;

        for (BuildingType type : category.buildings) {
            addBuilding(type, innerX, y, listWidth, cardHeight);
            y += cardHeight + gap;
        }
    }

    private void addBuilding(BuildingType type, int x, int y, int width, int height) {
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        boolean unlocked = isUnlocked(data, type);
        boolean affordable = isAffordable(data, type);
        String state = !unlocked ? "잠김" : affordable ? "건설 가능" : "자원 부족";
        String label;
        if (splitDetails) {
            label = type.displayName() + "  ·  " + state;
        } else {
            label = type.displayName() + "  ·  목 " + type.woodCost() + "  석 " + type.stoneCost() + "  ·  " + state;
        }

        Button button = Button.builder(Component.literal(trimToWidth(label, width - FrontierUiTheme.S)), b -> {
            BuildingPlacementClient.beginPlacement(type);
            this.minecraft.gui.setScreen(null);
        }).bounds(x, y, width, height).build();
        button.active = unlocked && affordable;
        addRenderableWidget(button);
        buildingRows.add(new BuildingRow(type, x, y, width, height));
    }

    private void addInfrastructure() {
        int gap = FrontierUiTheme.S;
        int buttonHeight = compact ? 20 : 24;
        int y = contentY;
        addRenderableWidget(Button.builder(Component.literal("도로 계획  ·  마을과 거점 연결"),
                b -> { RoadPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(innerX, y, innerWidth, buttonHeight).build());
        y += buttonHeight + gap;
        addRenderableWidget(Button.builder(Component.literal("전초기지  ·  영토와 생산권 확장"),
                b -> { OutpostPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(innerX, y, innerWidth, buttonHeight).build());
        y += buttonHeight + gap;
        addRenderableWidget(Button.builder(Component.literal("토목 평탄화  ·  절토 / 성토"),
                b -> { CivilWorkPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(innerX, y, innerWidth, buttonHeight).build());
        y += buttonHeight + gap;
        addRenderableWidget(Button.builder(Component.literal("거점 위치  ·  본진 / 전초 좌표와 방향"),
                b -> this.minecraft.gui.setScreen(new SettlementLocationScreen(this)))
                .bounds(innerX, y, innerWidth, buttonHeight).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        FrontierUiTheme.panel(graphics, panelX, panelY, panelWidth, panelHeight);
        drawHeader(graphics, data);
        drawCategorySelection(graphics);
        drawBuildingStateMarkers(graphics, data, mouseX, mouseY);

        if (splitDetails && !category.buildings.isEmpty()) {
            BuildingType detail = hoveredBuilding(mouseX, mouseY);
            if (detail == null) detail = defaultDetail(data);
            drawBuildingDetail(graphics, data, detail);
        } else if (category == Category.INFRA && !compact) {
            int infoY = Math.min(contentBottom - 28, contentY + 4 * 32 + FrontierUiTheme.S);
            if (infoY > contentY + 80) {
                FrontierUiTheme.divider(graphics, innerX, infoY, innerWidth);
                graphics.text(this.font, Component.literal("도로·전초·토목은 월드에서 위치를 정하고 서버가 최종 검증합니다."),
                        innerX, infoY + FrontierUiTheme.S, FrontierUiTheme.TEXT_SECONDARY, false);
            }
        }

        drawFooter(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, SettlementSnapshotPayload data) {
        int titleY = panelY + FrontierUiTheme.S + 4;
        graphics.text(this.font, Component.literal(compact ? "FRONTIER" : "FRONTIER SETTLEMENT"),
                innerX, titleY, FrontierUiTheme.TEXT_PRIMARY, true);

        String tier = data.tier() == null || data.tier().isBlank() ? "동기화 대기" : data.tier();
        int rank = tierRank(tier);
        int tierY = panelY + 31;
        String tierText = "마을 등급  " + tier + (rank > 0 ? "   " + rank + " / " + SETTLEMENT_TIER_COUNT : "");
        graphics.text(this.font, Component.literal(trimToWidth(tierText, innerWidth)),
                innerX, tierY, FrontierUiTheme.ACCENT, true);

        int progressY = tierY + 12;
        drawTierSegments(graphics, innerX, progressY, compact ? Math.min(120, innerWidth) : Math.min(168, innerWidth), rank);

        String goal = data.nextGoal() == null || data.nextGoal().isBlank()
                ? "다음 성장  ·  자유 건설과 영토 확장"
                : "다음 성장  ·  " + data.nextGoal();
        int goalY = progressY + 8;
        graphics.text(this.font, Component.literal(trimToWidth(goal, innerWidth)),
                innerX, goalY, FrontierUiTheme.TEXT_SECONDARY, false);

        String resources = "목재 " + data.wood() + "   석재 " + data.stone()
                + "   금속 " + data.metal() + "   식량 " + data.food() + "   인구 " + data.population();
        int resourceY = goalY + 14;
        graphics.text(this.font, Component.literal(trimToWidth(resources, innerWidth)),
                innerX, resourceY, FrontierUiTheme.TEXT_PRIMARY, false);
        FrontierUiTheme.divider(graphics, innerX, resourceY + 13, innerWidth);
    }

    private void drawTierSegments(GuiGraphicsExtractor graphics, int x, int y, int width, int rank) {
        int gap = 2;
        int segmentWidth = Math.max(5, (width - gap * (SETTLEMENT_TIER_COUNT - 1)) / SETTLEMENT_TIER_COUNT);
        for (int i = 0; i < SETTLEMENT_TIER_COUNT; i++) {
            int sx = x + i * (segmentWidth + gap);
            int color = i < rank ? FrontierUiTheme.PRIMARY : FrontierUiTheme.TRACK;
            graphics.fill(sx, y, sx + segmentWidth, y + 4, color);
        }
    }

    private void drawCategorySelection(GuiGraphicsExtractor graphics) {
        for (CategoryTab tab : categoryTabs) {
            if (tab.category() != category) continue;
            graphics.fill(tab.x(), tab.y() + tab.height() + 1,
                    tab.x() + tab.width(), tab.y() + tab.height() + 3, FrontierUiTheme.PRIMARY);
        }
        if (!compact) {
            graphics.text(this.font, Component.literal(category.description),
                    innerX, contentY - FrontierUiTheme.XS - 9, FrontierUiTheme.TEXT_MUTED, false);
        }
    }

    private void drawBuildingStateMarkers(GuiGraphicsExtractor graphics, SettlementSnapshotPayload data,
                                          int mouseX, int mouseY) {
        for (BuildingRow row : buildingRows) {
            boolean unlocked = isUnlocked(data, row.type());
            boolean affordable = isAffordable(data, row.type());
            int color = !unlocked ? FrontierUiTheme.DISABLED
                    : affordable ? FrontierUiTheme.SUCCESS : FrontierUiTheme.WARNING;
            graphics.fill(row.x() - 3, row.y(), row.x() - 1, row.y() + row.height(), color);
            if (row.contains(mouseX, mouseY)) {
                graphics.fill(row.x() - 5, row.y(), row.x() - 3, row.y() + row.height(), FrontierUiTheme.ACCENT);
            }
        }
    }

    private void drawBuildingDetail(GuiGraphicsExtractor graphics, SettlementSnapshotPayload data, BuildingType type) {
        int detailX = innerX + listWidth + FrontierUiTheme.M;
        int detailWidth = panelX + panelWidth - FrontierUiTheme.M - detailX;
        int detailHeight = Math.max(1, contentBottom - contentY);
        FrontierUiTheme.surface(graphics, detailX, contentY, detailWidth, detailHeight);

        boolean unlocked = isUnlocked(data, type);
        boolean affordable = isAffordable(data, type);
        String status = !unlocked ? "잠김" : affordable ? "건설 가능" : "자원 부족";
        int statusColor = !unlocked ? FrontierUiTheme.DISABLED
                : affordable ? FrontierUiTheme.SUCCESS : FrontierUiTheme.WARNING;

        int x = detailX + FrontierUiTheme.M;
        int right = detailX + detailWidth - FrontierUiTheme.M;
        int y = contentY + FrontierUiTheme.M;
        graphics.text(this.font, Component.literal(type.displayName()), x, y, FrontierUiTheme.TEXT_PRIMARY, true);
        graphics.text(this.font, Component.literal(status),
                Math.max(x, right - this.font.width(status)), y, statusColor, true);

        y += 18;
        FrontierUiTheme.divider(graphics, x, y, Math.max(1, right - x));
        y += FrontierUiTheme.S;
        graphics.text(this.font, Component.literal("부지"), x, y, FrontierUiTheme.TEXT_MUTED, false);
        graphics.text(this.font, Component.literal(type.width() + " × " + type.depth() + "   높이 " + type.clearHeight()),
                x + 42, y, FrontierUiTheme.TEXT_PRIMARY, false);

        y += 15;
        graphics.text(this.font, Component.literal("비용"), x, y, FrontierUiTheme.TEXT_MUTED, false);
        graphics.text(this.font, Component.literal("목재 " + type.woodCost() + "   석재 " + type.stoneCost()),
                x + 42, y, affordable ? FrontierUiTheme.TEXT_PRIMARY : FrontierUiTheme.WARNING, false);

        if (type.housingGain() > 0) {
            y += 15;
            graphics.text(this.font, Component.literal("주거"), x, y, FrontierUiTheme.TEXT_MUTED, false);
            graphics.text(this.font, Component.literal("인구 수용 +" + type.housingGain()),
                    x + 42, y, FrontierUiTheme.TEXT_PRIMARY, false);
        }

        String effect = specialEffect(data, type);
        if (!effect.isBlank()) {
            y += 15;
            graphics.text(this.font, Component.literal("효과"), x, y, FrontierUiTheme.TEXT_MUTED, false);
            graphics.text(this.font, Component.literal(trimToWidth(effect, Math.max(30, right - (x + 42)))),
                    x + 42, y, FrontierUiTheme.TEXT_PRIMARY, false);
        }

        if (!type.unlockHint().isBlank()) {
            y += 20;
            FrontierUiTheme.divider(graphics, x, y, Math.max(1, right - x));
            y += FrontierUiTheme.S;
            graphics.text(this.font, Component.literal("해금 조건"), x, y, FrontierUiTheme.ACCENT, false);
            y += 13;
            int maxWidth = Math.max(30, right - x);
            List<String> lines = wrapToWidth(type.unlockHint(), maxWidth, 2);
            for (String line : lines) {
                graphics.text(this.font, Component.literal(line), x, y,
                        unlocked ? FrontierUiTheme.TEXT_SECONDARY : FrontierUiTheme.WARNING, false);
                y += 12;
            }
        }

        String action = unlocked && affordable
                ? "클릭하면 월드 배치 모드로 전환"
                : !unlocked ? "조건 달성 후 건설 가능" : "공동 저장소에 자원을 보충하세요";
        graphics.text(this.font, Component.literal(trimToWidth(action, Math.max(30, right - x))),
                x, contentBottom - 13, unlocked && affordable
                        ? FrontierUiTheme.TEXT_SECONDARY : FrontierUiTheme.TEXT_MUTED, false);
    }

    private void drawFooter(GuiGraphicsExtractor graphics) {
        int y = panelY + panelHeight - 16;
        String controls = compact ? "M 메뉴 · R 회전 · Enter 확정" : "M 메뉴   ·   R 회전   ·   Enter 확정   ·   Esc 닫기";
        graphics.text(this.font, Component.literal(trimToWidth(controls, innerWidth)),
                innerX, y, FrontierUiTheme.TEXT_MUTED, false);
    }

    private BuildingType hoveredBuilding(int mouseX, int mouseY) {
        for (BuildingRow row : buildingRows) {
            if (row.contains(mouseX, mouseY)) return row.type();
        }
        return null;
    }

    private BuildingType defaultDetail(SettlementSnapshotPayload data) {
        for (BuildingType type : category.buildings) {
            if (isUnlocked(data, type) && isAffordable(data, type)) return type;
        }
        for (BuildingType type : category.buildings) {
            if (isUnlocked(data, type)) return type;
        }
        return category.buildings.getFirst();
    }

    private static boolean isUnlocked(SettlementSnapshotPayload data, BuildingType type) {
        return (data.buildingUnlockMask() & (1 << type.ordinal())) != 0;
    }

    private static boolean isAffordable(SettlementSnapshotPayload data, BuildingType type) {
        return data.wood() >= type.woodCost() && data.stone() >= type.stoneCost();
    }

    private static String specialEffect(SettlementSnapshotPayload data, BuildingType type) {
        if (type == BuildingType.CIVIC_HALL) return "주민 유입 20초 · 건설 인력 +2";
        if (type == BuildingType.LUMBER_CAMP || type == BuildingType.FARM
                || type == BuildingType.QUARRY || type == BuildingType.MINE) {
            int ceiling = productionCeiling(data.tier());
            return "신규 개량 I · 현장 버퍼 1통 · 현재 상한 " + gradeLabel(ceiling) + " · 완공 후 현장 저장통에서 수동 개량";
        }
        if (type == BuildingType.WAREHOUSE || type == BuildingType.CART_STATION) {
            int ceiling = logisticsCeiling(data.tier());
            return "신규 물류 I · 현재 상한 " + gradeLabel(ceiling) + " · 완공 후 저장통에서 수동 확장";
        }
        if (type == BuildingType.GUARD_POST || type == BuildingType.WATCHTOWER
                || type == BuildingType.BARRACKS || type == BuildingType.CITADEL) {
            int ceiling = militaryCeiling(data.tier());
            return "신규 군사 I · 현재 상한 " + gradeLabel(ceiling) + " · 완공 후 지휘 지점에서 수동 개량";
        }
        return "";
    }

    private static int productionCeiling(String tier) {
        return switch (tier) {
            case "마을" -> 2;
            case "개척 도시" -> 3;
            case "영지", "개척 수도" -> 4;
            default -> 1;
        };
    }

    private static int logisticsCeiling(String tier) {
        return switch (tier) {
            case "개척 도시" -> 2;
            case "영지", "개척 수도" -> 3;
            default -> 1;
        };
    }

    private static int militaryCeiling(String tier) {
        return switch (tier) {
            case "개척 도시" -> 2;
            case "영지" -> 3;
            case "개척 수도" -> 4;
            default -> 1;
        };
    }

    private static String gradeLabel(int grade) {
        return switch (Math.max(1, Math.min(4, grade))) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }

    private static int tierRank(String tier) {
        return switch (tier) {
            case "개척 캠프" -> 1;
            case "촌락" -> 2;
            case "마을" -> 3;
            case "개척 도시" -> 4;
            case "영지" -> 5;
            case "개척 수도" -> 6;
            default -> 0;
        };
    }

    private String trimToWidth(String text, int maxWidth) {
        if (maxWidth <= 0 || text == null) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "…";
        String out = text;
        while (!out.isEmpty() && this.font.width(out + suffix) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + suffix;
    }

    private List<String> wrapToWidth(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (this.font.width(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            int cut = 1;
            while (cut < remaining.length() && this.font.width(remaining.substring(0, cut + 1)) <= maxWidth) cut++;
            int space = remaining.lastIndexOf(' ', cut);
            if (space > 0) cut = space;
            String line = remaining.substring(0, cut).trim();
            remaining = remaining.substring(cut).trim();
            if (lines.size() == maxLines - 1 && !remaining.isEmpty()) line = trimToWidth(line + " " + remaining, maxWidth);
            lines.add(line);
        }
        return lines;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean isInGameUi() { return true; }
}

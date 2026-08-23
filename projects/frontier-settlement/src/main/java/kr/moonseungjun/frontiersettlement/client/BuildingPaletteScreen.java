package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BuildingPaletteScreen extends Screen {
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public BuildingPaletteScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("마을 건설"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(400, Math.max(300, this.width - 24));
        panelHeight = Math.min(194, Math.max(170, this.height - 24));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(12, (this.height - panelHeight) / 2);

        int gap = 10;
        int innerX = panelX + 12;
        int innerY = panelY + 32;
        int columnWidth = (panelWidth - 34) / 2;
        int rightX = innerX + columnWidth + gap;

        addBuilding(BuildingType.HOUSE, innerX, innerY + 14, columnWidth);
        addBuilding(BuildingType.WAREHOUSE, innerX, innerY + 60, columnWidth);
        addBuilding(BuildingType.GUARD_POST, innerX, innerY + 106, columnWidth);

        addBuilding(BuildingType.LUMBER_CAMP, rightX, innerY + 14, columnWidth);
        addBuilding(BuildingType.FARM, rightX, innerY + 38, columnWidth);
        addBuilding(BuildingType.QUARRY, rightX, innerY + 62, columnWidth);
        addBuilding(BuildingType.MINE, rightX, innerY + 86, columnWidth);
        addBuilding(BuildingType.BLACKSMITH, rightX, innerY + 110, columnWidth);

        addRenderableWidget(Button.builder(Component.literal("닫기"), button -> this.onClose())
                .bounds(panelX + panelWidth - 58, panelY + 7, 46, 20)
                .build());
    }

    private void addBuilding(BuildingType type, int x, int y, int width) {
        Component label = Component.literal(type.displayName() + "   목 " + type.woodCost() + " · 석 " + type.stoneCost());
        addRenderableWidget(Button.builder(label, button -> {
            BuildingPlacementClient.beginPlacement(type);
            this.minecraft.gui.setScreen(null);
        }).bounds(x, y, width, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally no blur or full-screen dim: the build palette is an in-world planning tool.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xD0101114);
        graphics.fill(panelX, panelY, panelX + 3, panelY + panelHeight, 0xFFD1A85A);
        graphics.text(this.font, Component.literal("마을 건설"), panelX + 12, panelY + 10, 0xFFFFFFFF, true);

        int innerX = panelX + 12;
        int innerY = panelY + 32;
        int gap = 10;
        int columnWidth = (panelWidth - 34) / 2;
        int rightX = innerX + columnWidth + gap;

        graphics.text(this.font, Component.literal("기반"), innerX, innerY, 0xFFFFD58A, true);
        graphics.text(this.font, Component.literal("물류"), innerX, innerY + 46, 0xFFFFD58A, true);
        graphics.text(this.font, Component.literal("방어"), innerX, innerY + 92, 0xFFFFD58A, true);
        graphics.text(this.font, Component.literal("생산"), rightX, innerY, 0xFFFFD58A, true);

        String hint = "건물 선택 → 월드에서 R 회전 · Enter 건설   |   J 도로 · K 전초기지";
        graphics.text(this.font, Component.literal(hint), panelX + 12, panelY + panelHeight - 15, 0xFFD0D0D0, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}

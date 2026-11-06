package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.device.AugmentDeviceBlocks;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compact recipe-only guide. The actual crafting recipes remain authoritative in data resources. */
public final class AugmentGuidebookScreen extends Screen {
    private static final int SCRIM = 0x94000000;
    private static final int BG = 0xF40B1013;
    private static final int TOP = 0xFF171E22;
    private static final int SURFACE = 0xFF11171B;
    private static final int SURFACE_HI = 0xFF1A2328;
    private static final int LINE = 0xFF3E4A50;
    private static final int LINE_SOFT = 0xFF293238;
    private static final int TEXT = 0xFFF2F3F1;
    private static final int MUTED = 0xFFA5AEB1;
    private static final int ACCENT = 0xFF91B9B3;
    private static final int GOLD = 0xFFE2C679;

    private static final List<RecipePage> PAGES = List.of(
            new RecipePage(
                    "증강 제작 안내서",
                    new ItemStack[] {new ItemStack(Items.BOOK), new ItemStack(Items.COPPER_INGOT)},
                    new ItemStack(dev.moonseungjun.augmentationsurvival.guide.GuidebookItems.GUIDEBOOK),
                    true
            ),
            new RecipePage(
                    "증강 장치",
                    grid(
                            Items.IRON_INGOT, Items.COPPER_INGOT, Items.IRON_INGOT,
                            Items.COPPER_INGOT, Items.FURNACE, Items.COPPER_INGOT,
                            Items.IRON_INGOT, Items.COPPER_INGOT, Items.IRON_INGOT
                    ),
                    new ItemStack(AugmentDeviceBlocks.AUGMENT_DEVICE.asItem()), false
            ),
            new RecipePage(
                    "강화 증강 장치",
                    grid(
                            Items.IRON_INGOT, Items.DIAMOND, Items.IRON_INGOT,
                            Items.GOLD_INGOT, AugmentDeviceBlocks.AUGMENT_DEVICE.asItem(), Items.GOLD_INGOT,
                            Items.IRON_INGOT, Items.DIAMOND, Items.IRON_INGOT
                    ),
                    new ItemStack(AugmentDeviceBlocks.ADVANCED_AUGMENT_DEVICE.asItem()), false
            ),
            new RecipePage(
                    "일반 증강 코어",
                    grid(
                            Items.REDSTONE, Items.AMETHYST_SHARD, Items.REDSTONE,
                            Items.AMETHYST_SHARD, Items.COPPER_INGOT, Items.AMETHYST_SHARD,
                            Items.REDSTONE, Items.AMETHYST_SHARD, Items.REDSTONE
                    ),
                    new ItemStack(AugmentCoreItems.COMMON_CORE), false
            )
    );

    private int page;
    private String hoveredName = "";

    public AugmentGuidebookScreen() {
        super(Component.translatable("screen.augmentation_survival.guidebook"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Layout l = layout();
        hoveredName = "";

        g.fill(0, 0, width, height, SCRIM);
        g.fill(l.x(), l.y(), l.x() + l.w(), l.y() + l.h(), BG);
        g.outline(l.x(), l.y(), l.w(), l.h(), 0xFF526067);
        g.fill(l.x() + 1, l.y() + 1, l.x() + l.w() - 1, l.y() + 31, TOP);
        g.fill(l.x() + 2, l.y() + 2, l.x() + l.w() - 2, l.y() + 4, ACCENT);
        drawCorner(g, l.x() + 7, l.y() + 7, 1, 1);
        drawCorner(g, l.x() + l.w() - 8, l.y() + 7, -1, 1);
        drawCorner(g, l.x() + 7, l.y() + l.h() - 8, 1, -1);
        drawCorner(g, l.x() + l.w() - 8, l.y() + l.h() - 8, -1, -1);

        g.text(font, "증강 제작 안내서", l.x() + 13, l.y() + 11, TEXT, false);
        String pageState = (page + 1) + " / " + PAGES.size();
        g.text(font, pageState, l.x() + l.w() - 13 - font.width(pageState), l.y() + 11, MUTED, false);

        RecipePage recipe = PAGES.get(page);
        centered(g, recipe.name(), l.centerX(), l.recipeTitleY(), TEXT);
        if (recipe.shapeless() && !l.compact()) {
            centered(g, "조합 위치 무관", l.centerX(), l.recipeTitleY() + (l.compact() ? 10 : 14), MUTED);
        } else if (!l.compact()) {
            centered(g, "3×3 제작대", l.centerX(), l.recipeTitleY() + 14, MUTED);
        }

        int slot = l.slotSize();
        int gap = l.slotGap();
        if (recipe.shapeless()) {
            int pairGap = gap + (l.compact() ? 4 : 8);
            int pairWidth = slot * 2 + pairGap;
            int startX = l.gridX() + (l.gridWidth() - pairWidth) / 2;
            int y = l.gridY() + (l.gridHeight() - slot) / 2;
            drawIngredientSlot(g, startX, y, slot, recipe.ingredients()[0], mouseX, mouseY);
            drawIngredientSlot(g, startX + slot + pairGap, y, slot, recipe.ingredients()[1], mouseX, mouseY);
        } else {
            for (int index = 0; index < 9; index++) {
                int col = index % 3;
                int row = index / 3;
                int x = l.gridX() + col * (slot + gap);
                int y = l.gridY() + row * (slot + gap);
                drawIngredientSlot(g, x, y, slot, recipe.ingredients()[index], mouseX, mouseY);
            }
        }

        centered(g, "→", l.arrowX(), l.arrowY(), GOLD);
        drawResultSlot(g, l.resultX(), l.resultY(), l.resultSize(), recipe.result(), mouseX, mouseY);

        if (l.showFooter()) {
            g.fill(l.x() + 13, l.footerLineY(), l.x() + l.w() - 13, l.footerLineY() + 1, LINE_SOFT);
            String hover = hoveredName.isBlank() ? "재료 위에 마우스를 올리세요." : hoveredName;
            centered(g, clip(hover, l.w() - 100), l.centerX(), l.footerTextY(), hoveredName.isBlank() ? MUTED : TEXT);
        }

        drawPageControl(g, l.leftX(), l.controlY(), "‹", page > 0, mouseX, mouseY);
        drawPageControl(g, l.rightX(), l.controlY(), "›", page + 1 < PAGES.size(), mouseX, mouseY);
    }

    private void drawIngredientSlot(GuiGraphicsExtractor g, int x, int y, int size, ItemStack stack, int mx, int my) {
        boolean hover = inside(mx, my, x, y, size, size);
        g.fill(x, y, x + size, y + size, hover ? SURFACE_HI : SURFACE);
        g.outline(x, y, size, size, hover ? ACCENT : LINE_SOFT);
        g.fill(x + 3, y + 3, x + size - 3, y + 4, hover ? 0x4DFFFFFF : 0x24FFFFFF);
        if (!stack.isEmpty()) {
            g.item(stack, x + size / 2 - 8, y + size / 2 - 8);
            if (hover) hoveredName = stack.getHoverName().getString();
        }
    }

    private void drawResultSlot(GuiGraphicsExtractor g, int x, int y, int size, ItemStack stack, int mx, int my) {
        boolean hover = inside(mx, my, x, y, size, size);
        g.fill(x - 4, y - 4, x + size + 4, y + size + 4, 0x1891B9B3);
        g.outline(x - 3, y - 3, size + 6, size + 6, 0x6091B9B3);
        g.fill(x, y, x + size, y + size, hover ? SURFACE_HI : SURFACE);
        g.outline(x, y, size, size, hover ? 0xFFD0E7E3 : ACCENT);
        g.outline(x + 3, y + 3, size - 6, size - 6, LINE_SOFT);
        g.item(stack, x + size / 2 - 8, y + size / 2 - 8);
        if (hover) hoveredName = stack.getHoverName().getString();
    }

    private void drawPageControl(GuiGraphicsExtractor g, int x, int y, String label, boolean enabled, int mx, int my) {
        boolean hover = enabled && inside(mx, my, x, y, 30, 18);
        g.fill(x, y, x + 30, y + 18, hover ? SURFACE_HI : SURFACE);
        g.outline(x, y, 30, 18, enabled ? (hover ? ACCENT : LINE) : LINE_SOFT);
        centered(g, label, x + 15, y + 4, enabled ? (hover ? TEXT : MUTED) : 0xFF596268);
    }

    private void drawCorner(GuiGraphicsExtractor g, int x, int y, int dx, int dy) {
        int hx = x + dx * 9;
        int vy = y + dy * 9;
        g.fill(Math.min(x, hx), Math.min(y, y + dy), Math.max(x, hx), Math.max(y, y + dy), 0xFF75878A);
        g.fill(Math.min(x, x + dx), Math.min(y, vy), Math.max(x, x + dx), Math.max(y, vy), 0xFF75878A);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout l = layout();
        if (page > 0 && inside(event.x(), event.y(), l.leftX(), l.controlY(), 30, 18)) {
            page--;
            return true;
        }
        if (page + 1 < PAGES.size() && inside(event.x(), event.y(), l.rightX(), l.controlY(), 30, 18)) {
            page++;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Layout layout() {
        int margin = width >= 320 && height >= 220 ? 12 : 4;
        int w = Math.max(1, Math.min(470, width - margin * 2));
        int h = Math.max(1, Math.min(310, height - margin * 2));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        boolean compact = w < 330 || h < 235;

        int slotGap = compact ? 2 : 5;
        int controlY = y + h - 19;
        boolean showFooter = !compact && h >= 235;
        int footerLineY = showFooter ? controlY - 26 : -1;
        int footerTextY = showFooter ? footerLineY + 8 : -1;
        int recipeTitleY = y + (compact ? 29 : 42);
        int gridY = y + (compact ? 41 : 74);
        int gridH = Math.max(12, (showFooter ? footerLineY : controlY - 6) - gridY);
        int maxSlot = compact ? 22 : 36;
        int slotSize = Math.max(compact ? 16 : 24, Math.min(maxSlot, (gridH - slotGap * 2) / 3));
        int gridHActual = slotSize * 3 + slotGap * 2;
        gridY = Math.min(gridY, controlY - gridHActual - (showFooter ? 32 : 1));
        gridH = gridHActual;
        int gridW = slotSize * 3 + slotGap * 2;
        int resultSize = compact ? 32 : 42;
        int resultDrawSize = Math.min(resultSize, Math.max(16, gridH));
        int flowW = gridW + (compact ? 28 : 52) + resultDrawSize;
        int gridX = x + (w - flowW) / 2;
        int arrowX = gridX + gridW + (compact ? 12 : 24);
        int resultX = gridX + gridW + (compact ? 28 : 52);
        int resultY = gridY + (gridH - resultDrawSize) / 2;
        int arrowY = gridY + gridH / 2 - 4;

        int leftX = x + 14;
        int rightX = x + w - 44;
        int availableGridW = Math.max(1, w - resultDrawSize - (compact ? 28 : 52));
        gridX = x + 12 + Math.max(0, (availableGridW - gridW) / 2);
        arrowX = gridX + gridW + (compact ? 12 : 24);
        resultX = Math.min(x + w - resultDrawSize - 14, arrowX + (compact ? 16 : 28));
        return new Layout(x, y, w, h, x + w / 2, compact, showFooter, recipeTitleY,
                gridX, gridY, gridW, gridH, slotSize, slotGap, arrowX, arrowY,
                resultX, resultY, resultDrawSize, footerLineY, footerTextY, controlY, leftX, rightX);
    }

    private String clip(String value, int maxWidth) {
        if (maxWidth <= 0 || value.isEmpty()) return "";
        if (font.width(value) <= maxWidth) return value;
        String result = value;
        while (!result.isEmpty() && font.width(result + "…") > maxWidth) {
            int end = result.offsetByCodePoints(result.length(), -1);
            result = result.substring(0, end);
        }
        return result.isEmpty() ? "…" : result + "…";
    }

    private void centered(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, value, x - font.width(value) / 2, y, color, false);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static ItemStack[] grid(net.minecraft.world.level.ItemLike... items) {
        ItemStack[] stacks = new ItemStack[9];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = i < items.length && items[i] != null ? new ItemStack(items[i]) : ItemStack.EMPTY;
        }
        return stacks;
    }

    private record RecipePage(String name, ItemStack[] ingredients, ItemStack result, boolean shapeless) {
    }

    private record Layout(
            int x, int y, int w, int h, int centerX, boolean compact, boolean showFooter, int recipeTitleY,
            int gridX, int gridY, int gridWidth, int gridHeight, int slotSize, int slotGap, int arrowX, int arrowY,
            int resultX, int resultY, int resultSize,
            int footerLineY, int footerTextY, int controlY, int leftX, int rightX
    ) {
    }
}

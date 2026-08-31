package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ImplantVaultScreen extends Screen {
    private int selected;

    public ImplantVaultScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("screen.titanbreak.vault"));
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        int y = layout.bottom() - 28;
        addRenderableWidget(TitanButton.create(Component.literal("‹"), button -> cycle(-1),
                layout.left() + 12, y, 30, 19));
        addRenderableWidget(TitanButton.create(Component.literal("›"), button -> cycle(1),
                layout.left() + 47, y, 30, 19));
        addRenderableWidget(TitanButton.create(Component.translatable("gui.done"), button -> onClose(),
                layout.right() - 70, y, 58, 19));
    }

    private void cycle(int direction) {
        List<TitanClientState.AugmentMeta> entries = TitanClientState.vaultMetadata();
        if (entries.isEmpty()) return;
        selected = Math.floorMod(selected + direction, entries.size());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            List<TitanClientState.AugmentMeta> entries = TitanClientState.vaultMetadata();
            Layout layout = layout();
            PanelGrid grid = panelGrid(layout);
            int rowHeight = 29;
            int visible = Math.max(1, (grid.left().bottom() - grid.left().top() - 33) / rowHeight);
            int start = windowStart(entries.size(), visible);
            for (int row = 0; row < visible && start + row < entries.size(); row++) {
                int y = grid.left().top() + 25 + row * rowHeight;
                if (event.x() >= grid.left().left() + 7 && event.x() <= grid.left().right() - 7
                        && event.y() >= y && event.y() <= y + 24) {
                    selected = start + row;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TitanInterfaceTheme.backdrop(graphics, width, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        TitanInterfaceTheme.shell(graphics, font, title, layout.left(), layout.top(), layout.right(), layout.bottom());
        PanelGrid grid = panelGrid(layout);
        TitanInterfaceTheme.panel(graphics, grid.left().left(), grid.left().top(), grid.left().right(), grid.left().bottom());
        TitanInterfaceTheme.panel(graphics, grid.right().left(), grid.right().top(), grid.right().right(), grid.right().bottom());
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.vault_inventory"),
                grid.left().left(), grid.left().top(), grid.left().right());
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.vault_metadata"),
                grid.right().left(), grid.right().top(), grid.right().right());

        List<TitanClientState.AugmentMeta> entries = TitanClientState.vaultMetadata();
        if (entries.isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.titanbreak.vault_empty"),
                    grid.left().centerX(), grid.left().top() + 54, TitanInterfaceTheme.TEXT_MUTED);
            TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.vault_hint"),
                    grid.right().left() + 10, grid.right().top() + 31, grid.right().width() - 20,
                    TitanInterfaceTheme.TEXT_MUTED, 5);
        } else {
            selected = Math.max(0, Math.min(selected, entries.size() - 1));
            drawEntries(graphics, entries, grid.left());
            drawDetails(graphics, entries.get(selected), grid.right());
        }

        graphics.text(font, Component.translatable("screen.titanbreak.vault_count", entries.size()),
                layout.left() + 86, layout.bottom() - 23, TitanInterfaceTheme.CYAN, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawEntries(GuiGraphicsExtractor graphics, List<TitanClientState.AugmentMeta> entries, Panel panel) {
        int rowHeight = 29;
        int visible = Math.max(1, (panel.bottom() - panel.top() - 33) / rowHeight);
        int start = windowStart(entries.size(), visible);
        for (int row = 0; row < visible && start + row < entries.size(); row++) {
            int index = start + row;
            TitanClientState.AugmentMeta meta = entries.get(index);
            AugmentationCatalog.Definition definition = AugmentationCatalog.byId(meta.id());
            int y = panel.top() + 25 + row * rowHeight;
            int left = panel.left() + 7;
            int right = panel.right() - 7;
            boolean current = index == selected;
            graphics.fill(left, y, right, y + 24, current ? 0xE61A1B15 : TitanInterfaceTheme.PANEL_SOFT);
            graphics.fill(left, y, left + 2, y + 24, current ? TitanInterfaceTheme.ACCENT : TitanInterfaceTheme.LINE);
            if (current) TitanInterfaceTheme.selectionFrame(graphics, left, y, right - left, 24, TitanInterfaceTheme.ACCENT);

            ItemStack stack = stackFor(definition);
            if (!stack.isEmpty()) graphics.item(stack, left + 6, y + 4);
            int textX = left + 28;
            Component name = definition == null ? Component.literal(meta.id()) : Component.translatable(definition.nameKey());
            TitanInterfaceTheme.wrapped(graphics, font, name, textX, y + 3,
                    Math.max(40, right - textX - 44), TitanInterfaceTheme.TEXT, 1);
            graphics.text(font, Component.literal("M" + meta.mk() + "  +" + meta.enhancement()),
                    Math.max(textX, right - 39), y + 13, TitanInterfaceTheme.CYAN, false);
        }
    }

    private int windowStart(int count, int visible) {
        if (count <= visible) return 0;
        int half = visible / 2;
        return Math.max(0, Math.min(selected - half, count - visible));
    }

    private void drawDetails(GuiGraphicsExtractor graphics, TitanClientState.AugmentMeta meta, Panel panel) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(meta.id());
        int x = panel.left() + 11;
        int y = panel.top() + 29;
        int maxWidth = panel.width() - 22;
        ItemStack stack = stackFor(definition);
        graphics.fill(x, y, panel.right() - 11, y + 44, 0xD90C1215);
        graphics.outline(x, y, panel.width() - 22, 44, TitanInterfaceTheme.LINE_SOFT);
        if (!stack.isEmpty()) graphics.item(stack, x + 8, y + 9);
        Component name = definition == null ? Component.literal(meta.id()) : Component.translatable(definition.nameKey());
        TitanInterfaceTheme.wrapped(graphics, font, name, x + 34, y + 7,
                Math.max(50, maxWidth - 38), TitanInterfaceTheme.TEXT, 2);
        y += 53;

        graphics.text(font, Component.translatable("screen.titanbreak.generation", meta.mk()), x, y,
                TitanInterfaceTheme.ACCENT, false);
        y += font.lineHeight + 4;
        graphics.text(font, Component.translatable("screen.titanbreak.enhancement", meta.enhancement()), x, y,
                TitanInterfaceTheme.CYAN, false);
        y += font.lineHeight + 4;
        graphics.text(font, Component.translatable("screen.titanbreak.mastery", TitanClientState.masteryLevel(meta.id())), x, y,
                TitanInterfaceTheme.VIOLET, false);
        y += font.lineHeight + 7;
        graphics.horizontalLine(x, panel.right() - 11, y, TitanInterfaceTheme.LINE_SOFT);
        y += 7;

        if (definition != null) {
            y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable(definition.effectKey()), x, y,
                    maxWidth, TitanInterfaceTheme.TEXT_MUTED, 4) + 7;
        }
        if (y < panel.bottom() - 34) {
            TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.vault_protected"),
                    x, y, maxWidth, TitanInterfaceTheme.GOOD,
                    Math.max(1, (panel.bottom() - y - 12) / (font.lineHeight + 2)));
        }
    }

    private ItemStack stackFor(AugmentationCatalog.Definition definition) {
        if (definition == null) return ItemStack.EMPTY;
        Item item = ModItems.augmentationByPath(definition.itemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private Layout layout() {
        int panelWidth = Math.min(700, Math.max(300, width - 20));
        int panelHeight = Math.min(330, Math.max(190, height - 18));
        int left = width / 2 - panelWidth / 2;
        int top = Math.max(6, height / 2 - panelHeight / 2);
        return new Layout(left, left + panelWidth, top, top + panelHeight);
    }

    private PanelGrid panelGrid(Layout layout) {
        int left = layout.left() + 10;
        int right = layout.right() - 10;
        int top = layout.top() + 39;
        int bottom = layout.bottom() - 38;
        int gap = 7;
        int available = right - left - gap;
        int leftWidth = Math.max(125, Math.min(280, available * 44 / 100));
        Panel a = new Panel(left, left + leftWidth, top, bottom);
        Panel b = new Panel(a.right() + gap, right, top, bottom);
        return new PanelGrid(a, b);
    }

    private record Layout(int left, int right, int top, int bottom) {}
    private record Panel(int left, int right, int top, int bottom) {
        int width() { return right - left; }
        int centerX() { return (left + right) / 2; }
    }
    private record PanelGrid(Panel left, Panel right) {}
}

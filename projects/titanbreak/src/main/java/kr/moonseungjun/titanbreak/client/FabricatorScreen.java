package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.network.StationActionPayload;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.Map;

public final class FabricatorScreen extends Screen {
    private final String stationSpec;
    private final List<AugmentationCatalog.Definition> definitions = AugmentationCatalog.fabricatorOneDefinitions();
    private int selected;
    private TitanButton fabricate;

    public FabricatorScreen(String stationSpec) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("screen.titanbreak.fabricator"));
        this.stationSpec = stationSpec;
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        int y = layout.bottom() - 28;
        int nav = 28;
        int gap = 5;
        int fabricateWidth = Math.min(112, Math.max(88, layout.width() / 7));
        int x = layout.left() + 12;

        addRenderableWidget(TitanButton.create(Component.literal("‹"), button -> cycle(-1), x, y, nav, 19));
        x += nav + gap;
        addRenderableWidget(TitanButton.create(Component.literal("›"), button -> cycle(1), x, y, nav, 19));
        x += nav + gap + 4;
        fabricate = addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.fabricate"), button -> fabricate(),
                x, y, fabricateWidth, 19));

        int doneWidth = 58;
        int assembleWidth = Math.min(126, Math.max(98, layout.width() / 6));
        int doneX = layout.right() - 12 - doneWidth;
        int assembleX = doneX - gap - assembleWidth;
        addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.assemble_surgery"),
                button -> send("assemble_surgery", ""), assembleX, y, assembleWidth, 19));
        addRenderableWidget(TitanButton.create(Component.translatable("gui.done"), button -> onClose(),
                doneX, y, doneWidth, 19));
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (fabricate != null && !definitions.isEmpty()) fabricate.active = recipeReady(definitions.get(selected));
    }

    private void cycle(int direction) {
        if (definitions.isEmpty()) return;
        selected = Math.floorMod(selected + direction, definitions.size());
        refreshButtons();
    }

    private void fabricate() {
        if (definitions.isEmpty()) return;
        send("fabricate", definitions.get(selected).id());
    }

    private void send(String action, String argument) {
        ClientPacketDistributor.sendToServer(new StationActionPayload(stationSpec + "|" + action + "|" + argument));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TitanInterfaceTheme.backdrop(graphics, width, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        TitanInterfaceTheme.shell(graphics, font, title, layout.left(), layout.top(), layout.right(), layout.bottom());
        if (definitions.isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.titanbreak.empty"), width / 2, height / 2,
                    TitanInterfaceTheme.TEXT_MUTED);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        PanelGrid grid = panelGrid(layout);
        TitanInterfaceTheme.panel(graphics, grid.left().left(), grid.left().top(), grid.left().right(), grid.left().bottom());
        TitanInterfaceTheme.panel(graphics, grid.middle().left(), grid.middle().top(), grid.middle().right(), grid.middle().bottom());
        TitanInterfaceTheme.panel(graphics, grid.right().left(), grid.right().top(), grid.right().right(), grid.right().bottom());

        AugmentationCatalog.Definition definition = definitions.get(selected);
        drawModuleCard(graphics, definition, grid.left());
        drawRecipe(graphics, definition, grid.middle());
        drawOutput(graphics, definition, grid.right());

        graphics.horizontalLine(layout.left() + 10, layout.right() - 10, layout.bottom() - 34, TitanInterfaceTheme.LINE_SOFT);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawModuleCard(GuiGraphicsExtractor graphics, AugmentationCatalog.Definition definition, Panel panel) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.module"),
                panel.left(), panel.top(), panel.right());
        ItemStack stack = outputStack(definition);
        int x = panel.left() + 10;
        int maxWidth = Math.max(70, panel.width() - 20);
        int y = panel.top() + 28;

        graphics.fill(x, y, panel.right() - 10, y + 43, TitanInterfaceTheme.PANEL_SOFT);
        graphics.outline(x, y, panel.width() - 20, 43, TitanInterfaceTheme.LINE_SOFT);
        if (!stack.isEmpty()) {
            graphics.item(stack, x + 7, y + 8);
            graphics.itemDecorations(font, stack, x + 7, y + 8);
        }
        int textX = x + 32;
        int textW = Math.max(46, panel.right() - 10 - textX);
        int nameBottom = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable(definition.nameKey()),
                textX, y + 6, textW, TitanInterfaceTheme.TEXT, 2);
        graphics.text(font, Component.translatable("screen.titanbreak.tier", definition.tier()), textX,
                Math.min(y + 29, nameBottom + 1), TitanInterfaceTheme.ACCENT, false);

        y += 54;
        y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable(definition.effectKey()),
                x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED, 4) + 6;
        graphics.horizontalLine(x, panel.right() - 10, y, TitanInterfaceTheme.LINE_SOFT);
        y += 7;
        TitanInterfaceTheme.wrapped(graphics, font, compatibleText(definition), x, y, maxWidth,
                TitanInterfaceTheme.CYAN, Math.max(1, (panel.bottom() - y - 24) / (font.lineHeight + 2)));

        String index = (selected + 1) + " / " + definitions.size();
        graphics.text(font, Component.literal(index), x, panel.bottom() - 15, TitanInterfaceTheme.TEXT_MUTED, false);
        graphics.horizontalLine(x + font.width(index) + 7, panel.right() - 10, panel.bottom() - 11, 0x6643D7E8);
    }

    private void drawRecipe(GuiGraphicsExtractor graphics, AugmentationCatalog.Definition definition, Panel panel) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.requirements"),
                panel.left(), panel.top(), panel.right());
        int y = panel.top() + 27;
        int count = Math.max(1, definition.recipe().size());
        int available = panel.bottom() - y - 9;
        int rowHeight = Math.max(31, Math.min(42, available / count - 5));

        for (Map.Entry<String, Integer> requirement : definition.recipe().entrySet()) {
            if (y + 27 > panel.bottom() - 5) break;
            Item item = ModItems.byPath(requirement.getKey());
            ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
            int owned = owned(requirement.getKey());
            boolean ready = owned >= requirement.getValue();
            int rowBottom = Math.min(panel.bottom() - 7, y + rowHeight);
            int rowColor = ready ? 0xA5102019 : 0xA5220F13;
            int edge = ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD;
            graphics.fill(panel.left() + 8, y, panel.right() - 8, rowBottom, rowColor);
            graphics.fill(panel.left() + 8, y, panel.left() + 10, rowBottom, edge);
            graphics.horizontalLine(panel.left() + 10, panel.right() - 8, y, 0x66404B50);
            if (!stack.isEmpty()) {
                graphics.item(stack, panel.left() + 15, y + 7);
                graphics.itemDecorations(font, stack, panel.left() + 15, y + 7);
            }
            int textX = panel.left() + 38;
            int textW = Math.max(48, panel.right() - 16 - textX);
            TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("item.titanbreak." + requirement.getKey()),
                    textX, y + 5, textW, TitanInterfaceTheme.TEXT, 1);
            String countText = owned + " / " + requirement.getValue();
            graphics.text(font, Component.literal(countText), textX, rowBottom - 12,
                    ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD, false);
            y = rowBottom + 5;
        }

        if (definition.recipe().isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.titanbreak.empty"), panel.centerX(),
                    panel.top() + 52, TitanInterfaceTheme.TEXT_MUTED);
        }
    }

    private void drawOutput(GuiGraphicsExtractor graphics, AugmentationCatalog.Definition definition, Panel panel) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.fabricate"),
                panel.left(), panel.top(), panel.right());
        ItemStack stack = outputStack(definition);
        boolean ready = recipeReady(definition);
        int x = panel.left() + 10;
        int maxWidth = Math.max(70, panel.width() - 20);
        int y = panel.top() + 31;

        graphics.fill(x, y, panel.right() - 10, y + 54, 0xC80B1114);
        TitanInterfaceTheme.selectionFrame(graphics, panel.centerX() - 10, y + 10, 20, 20,
                ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD);
        if (!stack.isEmpty()) graphics.item(stack, panel.centerX() - 8, y + 12);
        y += 60;

        y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable(definition.nameKey()),
                x, y, maxWidth, TitanInterfaceTheme.TEXT, 2) + 4;
        Component status = ready ? Component.translatable("screen.titanbreak.fabricate")
                : Component.translatable("message.titanbreak.materials_missing");
        y = TitanInterfaceTheme.wrapped(graphics, font, status, x, y, maxWidth,
                ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD, 2) + 7;
        graphics.horizontalLine(x, panel.right() - 10, y, TitanInterfaceTheme.LINE_SOFT);
        y += 8;
        y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.fabricator_hint"),
                x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED, 3) + 8;

        if (y < panel.bottom() - 31) {
            graphics.fill(x, y, panel.right() - 10, y + 1, 0x665BA7F0);
            y += 7;
            TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.surgery_cost_short"),
                    x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED,
                    Math.max(1, (panel.bottom() - y - 5) / (font.lineHeight + 2)));
        }
    }

    private boolean recipeReady(AugmentationCatalog.Definition definition) {
        for (Map.Entry<String, Integer> requirement : definition.recipe().entrySet()) {
            if (owned(requirement.getKey()) < requirement.getValue()) return false;
        }
        return true;
    }

    private Component compatibleText(AugmentationCatalog.Definition definition) {
        MutableComponent text = Component.translatable("screen.titanbreak.compatible_slots").append(" · ");
        for (int i = 0; i < definition.placements().size(); i++) {
            if (i > 0) text.append(" / ");
            AugmentationCatalog.Placement placement = definition.placements().get(i);
            for (int j = 0; j < placement.slots().size(); j++) {
                if (j > 0) text.append(" + ");
                text.append(Component.translatable(placement.slots().get(j).translationKey()));
            }
        }
        return text;
    }

    private ItemStack outputStack(AugmentationCatalog.Definition definition) {
        Item item = ModItems.augmentationByPath(definition.itemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private int owned(String itemPath) {
        if (minecraft.player == null) return 0;
        Item item = ModItems.byPath(itemPath);
        if (item == null) return 0;
        int total = 0;
        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private Layout layout() {
        int panelWidth = Math.min(780, Math.max(300, width - 20));
        int panelHeight = Math.min(348, Math.max(190, height - 18));
        int left = width / 2 - panelWidth / 2;
        int top = Math.max(6, height / 2 - panelHeight / 2);
        return new Layout(left, left + panelWidth, top, top + panelHeight);
    }

    private PanelGrid panelGrid(Layout layout) {
        int gap = 6;
        int left = layout.left() + 10;
        int right = layout.right() - 10;
        int top = layout.top() + 39;
        int bottom = layout.bottom() - 38;
        int available = right - left - gap * 2;
        int leftWidth = Math.max(145, Math.min(215, (int) Math.round(available * 0.30D)));
        int middleWidth = Math.max(145, Math.min(230, (int) Math.round(available * 0.32D)));
        if (leftWidth + middleWidth + 130 > available) {
            leftWidth = Math.max(120, available * 30 / 100);
            middleWidth = Math.max(120, available * 31 / 100);
        }
        int rightWidth = available - leftWidth - middleWidth;
        Panel a = new Panel(left, left + leftWidth, top, bottom);
        Panel b = new Panel(a.right() + gap, a.right() + gap + middleWidth, top, bottom);
        Panel c = new Panel(b.right() + gap, right, top, bottom);
        return new PanelGrid(a, b, c);
    }

    private record Layout(int left, int right, int top, int bottom) {
        int width() { return right - left; }
    }

    private record Panel(int left, int right, int top, int bottom) {
        int width() { return right - left; }
        int centerX() { return (left + right) / 2; }
    }

    private record PanelGrid(Panel left, Panel middle, Panel right) {}
}

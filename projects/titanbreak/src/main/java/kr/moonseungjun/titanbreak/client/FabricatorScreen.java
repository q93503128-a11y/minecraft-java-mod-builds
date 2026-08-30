package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.network.StationActionPayload;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
    private Button fabricate;

    public FabricatorScreen(String stationSpec) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("screen.titanbreak.fabricator"));
        this.stationSpec = stationSpec;
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        int footerY = layout.bottom() - 30;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycle(-1))
                .bounds(layout.left() + 14, footerY, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycle(1))
                .bounds(layout.left() + 50, footerY, 32, 20).build());
        fabricate = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.fabricate"), button -> fabricate())
                .bounds(layout.left() + 92, footerY, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.assemble_surgery"), button -> send("assemble_surgery", ""))
                .bounds(layout.right() - 234, footerY, 140, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(layout.right() - 84, footerY, 70, 20).build());
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (fabricate != null) fabricate.active = recipeReady(definitions.get(selected));
    }

    private void cycle(int direction) {
        selected = Math.floorMod(selected + direction, definitions.size());
        refreshButtons();
    }

    private void fabricate() {
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

        int contentTop = layout.top() + 42;
        int contentBottom = layout.bottom() - 40;
        int sideWidth = sideWidth(layout.width());
        int gap = 8;
        int leftPanelRight = layout.left() + sideWidth;
        int rightPanelLeft = layout.right() - sideWidth;

        TitanInterfaceTheme.panel(graphics, layout.left() + 10, contentTop, leftPanelRight, contentBottom);
        TitanInterfaceTheme.panel(graphics, leftPanelRight + gap, contentTop, rightPanelLeft - gap, contentBottom);
        TitanInterfaceTheme.panel(graphics, rightPanelLeft, contentTop, layout.right() - 10, contentBottom);

        AugmentationCatalog.Definition definition = definitions.get(selected);
        drawModuleCard(graphics, definition, layout.left() + 10, contentTop, leftPanelRight, contentBottom);
        drawRecipe(graphics, definition, leftPanelRight + gap, contentTop, rightPanelLeft - gap, contentBottom);
        drawOutput(graphics, definition, rightPanelLeft, contentTop, layout.right() - 10, contentBottom);

        graphics.horizontalLine(layout.left() + 10, layout.right() - 10, layout.bottom() - 36, TitanInterfaceTheme.LINE_SOFT);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawModuleCard(GuiGraphicsExtractor graphics, AugmentationCatalog.Definition definition,
                                int left, int top, int right, int bottom) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.module"), left, top, right);
        ItemStack stack = outputStack(definition);
        int cx = (left + right) / 2;
        int cardTop = top + 30;

        graphics.fill(left + 12, cardTop, right - 12, cardTop + 50, TitanInterfaceTheme.PANEL_SOFT);
        graphics.outline(left + 12, cardTop, right - left - 24, 50, TitanInterfaceTheme.LINE_SOFT);
        if (!stack.isEmpty()) {
            graphics.item(stack, left + 20, cardTop + 10);
            graphics.itemDecorations(font, stack, left + 20, cardTop + 10);
        }
        graphics.textWithWordWrap(font, Component.translatable(definition.nameKey()), left + 45, cardTop + 8,
                Math.max(58, right - left - 58), TitanInterfaceTheme.TEXT, false);
        graphics.text(font, Component.translatable("screen.titanbreak.tier", definition.tier()), left + 45, cardTop + 31,
                TitanInterfaceTheme.ACCENT, false);

        graphics.textWithWordWrap(font, Component.translatable(definition.effectKey()), left + 12, cardTop + 66,
                Math.max(70, right - left - 24), TitanInterfaceTheme.TEXT_MUTED, false);
        graphics.textWithWordWrap(font, compatibleText(definition), left + 12, cardTop + 113,
                Math.max(70, right - left - 24), TitanInterfaceTheme.CYAN, false);

        String index = (selected + 1) + " / " + definitions.size();
        graphics.centeredText(font, index, cx, bottom - 19, TitanInterfaceTheme.TEXT_MUTED);
    }

    private void drawRecipe(GuiGraphicsExtractor graphics, AugmentationCatalog.Definition definition,
                            int left, int top, int right, int bottom) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.requirements"), left, top, right);
        int y = top + 30;
        for (Map.Entry<String, Integer> requirement : definition.recipe().entrySet()) {
            Item item = ModItems.byPath(requirement.getKey());
            ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
            int owned = owned(requirement.getKey());
            boolean ready = owned >= requirement.getValue();
            int rowBottom = y + 38;
            graphics.fill(left + 9, y, right - 9, rowBottom, ready ? 0xB5223B30 : 0xB53B2528);
            graphics.outline(left + 9, y, right - left - 18, 38,
                    ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD);
            if (!stack.isEmpty()) {
                graphics.item(stack, left + 16, y + 10);
                graphics.itemDecorations(font, stack, left + 16, y + 10, Integer.toString(owned));
            }
            graphics.textWithWordWrap(font, Component.translatable("item.titanbreak." + requirement.getKey()),
                    left + 40, y + 7, Math.max(52, right - left - 52), TitanInterfaceTheme.TEXT, false);
            graphics.text(font, owned + " / " + requirement.getValue(), left + 40, y + 23,
                    ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD, false);
            y += 44;
        }

        if (definition.recipe().isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.titanbreak.empty"), (left + right) / 2,
                    top + 62, TitanInterfaceTheme.TEXT_MUTED);
        }
    }

    private void drawOutput(GuiGraphicsExtractor graphics, AugmentationCatalog.Definition definition,
                            int left, int top, int right, int bottom) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.fabricate"), left, top, right);
        ItemStack stack = outputStack(definition);
        int cx = (left + right) / 2;
        boolean ready = recipeReady(definition);

        graphics.fill(left + 12, top + 31, right - 12, top + 88, TitanInterfaceTheme.PANEL_SOFT);
        TitanInterfaceTheme.selectionFrame(graphics, cx - 10, top + 45, 20, 20,
                ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD);
        if (!stack.isEmpty()) graphics.item(stack, cx - 8, top + 47);

        graphics.centeredText(font, Component.translatable(definition.nameKey()), cx, top + 99, TitanInterfaceTheme.TEXT);
        Component status = ready ? Component.translatable("screen.titanbreak.fabricate")
                : Component.translatable("message.titanbreak.materials_missing");
        graphics.centeredText(font, status, cx, top + 118, ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD);

        graphics.horizontalLine(left + 12, right - 12, top + 142, TitanInterfaceTheme.LINE_SOFT);
        graphics.textWithWordWrap(font, Component.translatable("screen.titanbreak.fabricator_hint"), left + 12, top + 153,
                Math.max(70, right - left - 24), TitanInterfaceTheme.TEXT_MUTED, false);
        graphics.textWithWordWrap(font, Component.translatable("screen.titanbreak.surgery_cost_short"), left + 12, bottom - 50,
                Math.max(70, right - left - 24), TitanInterfaceTheme.TEXT_MUTED, false);
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

    private static int sideWidth(int totalWidth) {
        return Math.max(90, Math.min(220, (totalWidth - 166) / 2));
    }

    private Layout layout() {
        int panelWidth = Math.min(790, Math.max(320, width - 16));
        int panelHeight = Math.min(294, Math.max(250, height - 16));
        int left = width / 2 - panelWidth / 2;
        int top = Math.max(8, height / 2 - panelHeight / 2);
        return new Layout(left, left + panelWidth, top, top + panelHeight);
    }

    private record Layout(int left, int right, int top, int bottom) {
        int width() { return right - left; }
    }
}

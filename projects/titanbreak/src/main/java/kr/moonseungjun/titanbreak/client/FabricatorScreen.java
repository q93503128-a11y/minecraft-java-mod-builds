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
    private final int fabricatorTier;
    private final List<AugmentationCatalog.Definition> definitions;
    private int selected;
    private TitanButton fabricate;
    private TitanButton enhance;
    private TitanButton mkUpgrade;
    private TitanButton upgrade;

    public FabricatorScreen(String stationSpec) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.titanbreak.fabricator_tier", parseTier(stationSpec)));
        this.stationSpec = stationSpec;
        this.fabricatorTier = parseTier(stationSpec);
        this.definitions = AugmentationCatalog.fabricatorDefinitions(fabricatorTier);
    }

    private static int parseTier(String spec) {
        if (spec == null) return 1;
        int split = spec.indexOf('|');
        String station = split >= 0 ? spec.substring(0, split) : spec;
        if (!station.startsWith("fabricator_")) return 1;
        try {
            return Math.max(1, Math.min(3, Integer.parseInt(station.substring("fabricator_".length()))));
        } catch (RuntimeException ignored) {
            return 1;
        }
    }

    private int enhancementCap() {
        return switch (fabricatorTier) {
            case 1 -> 3;
            case 2 -> 7;
            default -> 10;
        };
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        int gap = 5;
        int coreY = layout.bottom() - 50;
        int facilityY = layout.bottom() - 27;
        int nav = 27;
        int left = layout.left() + 11;
        int doneW = 56;
        int doneX = layout.right() - 11 - doneW;
        int available = Math.max(162, doneX - left - nav * 2 - gap * 5);
        int actionW = Math.max(48, Math.min(78, available / 3));

        addRenderableWidget(TitanButton.create(Component.literal("‹"), button -> cycle(-1), left, coreY, nav, 19));
        left += nav + gap;
        addRenderableWidget(TitanButton.create(Component.literal("›"), button -> cycle(1), left, coreY, nav, 19));
        left += nav + gap + 2;

        fabricate = addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.fabricate"),
                button -> fabricate(), left, coreY, actionW, 19));
        left += actionW + gap;
        enhance = addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.enhance"),
                button -> progressSelected("enhance"), left, coreY, actionW, 19));
        left += actionW + gap;
        mkUpgrade = addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.upgrade_mk"),
                button -> progressSelected("upgrade_mk"), left, coreY, actionW, 19));

        addRenderableWidget(TitanButton.create(Component.translatable("gui.done"), button -> onClose(),
                doneX, coreY, doneW, 19));

        int facilityLeft = layout.left() + 11;
        int facilityRight = layout.right() - 11;
        int facilityGap = 5;
        int facilityButtons = layout.width() >= 610 ? 3 : layout.width() >= 445 ? 2 : 1;
        int facilityW = Math.max(70, Math.min(128,
                (facilityRight - facilityLeft - facilityGap * (facilityButtons - 1)) / facilityButtons));
        if (fabricatorTier < 3) {
            upgrade = addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.upgrade_fabricator"),
                    button -> send("upgrade_fabricator", ""), facilityLeft, facilityY, facilityW, 19));
            facilityLeft += facilityW + facilityGap;
        }
        if (layout.width() >= 445 && facilityLeft + facilityW <= facilityRight) {
            addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.assemble_surgery"),
                    button -> send("assemble_surgery", ""), facilityLeft, facilityY, facilityW, 19));
            facilityLeft += facilityW + facilityGap;
        }
        if (layout.width() >= 610 && facilityLeft + facilityW <= facilityRight) {
            addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.assemble_vault"),
                    button -> send("assemble_vault", ""), facilityLeft, facilityY, facilityW, 19));
        }
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (definitions.isEmpty()) return;
        AugmentationCatalog.Definition definition = definitions.get(selected);
        TitanClientState.AugmentMeta meta = TitanClientState.augmentMeta(definition.id());
        if (fabricate != null) fabricate.active = recipeReady(definition);
        if (enhance != null) enhance.active = meta != null && meta.enhancement() < enhancementCap();
        if (mkUpgrade != null) {
            boolean stationUnlocked = fabricatorTier >= 2;
            boolean nextAllowed = meta != null && meta.mk() < 5 && (meta.mk() < 4 || fabricatorTier >= 3);
            mkUpgrade.active = stationUnlocked && nextAllowed;
        }
        if (upgrade != null) upgrade.active = fabricatorTier < 3;
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

    private void progressSelected(String action) {
        if (definitions.isEmpty()) return;
        send(action, definitions.get(selected).id());
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

        graphics.horizontalLine(layout.left() + 10, layout.right() - 10, layout.bottom() - 57, TitanInterfaceTheme.LINE_SOFT);
        graphics.horizontalLine(layout.left() + 10, layout.right() - 10, layout.bottom() - 34, 0x5543D7E8);
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
                x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED, 4) + 5;
        Component load = Component.translatable("screen.titanbreak.system_load",
                definition.powerLoad(), definition.heatLoad(), definition.neuralLoad());
        y = TitanInterfaceTheme.wrapped(graphics, font, load, x, y, maxWidth,
                TitanInterfaceTheme.ACCENT, 2) + 5;
        graphics.horizontalLine(x, panel.right() - 10, y, TitanInterfaceTheme.LINE_SOFT);
        y += 7;
        TitanInterfaceTheme.wrapped(graphics, font, compatibleText(definition), x, y, maxWidth,
                TitanInterfaceTheme.CYAN, Math.max(1, (panel.bottom() - y - 24) / (font.lineHeight + 2)));

        String index = (selected + 1) + " / " + definitions.size();
        graphics.text(font, Component.literal(index), x, panel.bottom() - 15, TitanInterfaceTheme.TEXT_MUTED, false);
    }

    private void drawRecipe(GuiGraphicsExtractor graphics, AugmentationCatalog.Definition definition, Panel panel) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.requirements"),
                panel.left(), panel.top(), panel.right());
        int y = panel.top() + 27;
        int count = Math.max(1, definition.recipe().size());
        int available = panel.bottom() - y - 9;
        int rowHeight = Math.max(29, Math.min(40, available / count - 5));

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
            if (!stack.isEmpty()) {
                graphics.item(stack, panel.left() + 15, y + 6);
                graphics.itemDecorations(font, stack, panel.left() + 15, y + 6);
            }
            int textX = panel.left() + 38;
            int textW = Math.max(48, panel.right() - 16 - textX);
            TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("item.titanbreak." + requirement.getKey()),
                    textX, y + 4, textW, TitanInterfaceTheme.TEXT, 1);
            graphics.text(font, Component.literal(owned + " / " + requirement.getValue()), textX, rowBottom - 11,
                    ready ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.BAD, false);
            y = rowBottom + 5;
        }
    }

    private void drawOutput(GuiGraphicsExtractor graphics, AugmentationCatalog.Definition definition, Panel panel) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.fabrication_status"),
                panel.left(), panel.top(), panel.right());
        ItemStack stack = outputStack(definition);
        TitanClientState.AugmentMeta meta = TitanClientState.augmentMeta(definition.id());
        int x = panel.left() + 10;
        int maxWidth = Math.max(70, panel.width() - 20);
        int y = panel.top() + 29;

        graphics.fill(x, y, panel.right() - 10, y + 48, 0xC80B1114);
        TitanInterfaceTheme.selectionFrame(graphics, panel.centerX() - 10, y + 8, 20, 20,
                meta == null ? TitanInterfaceTheme.CYAN : TitanInterfaceTheme.ACCENT);
        if (!stack.isEmpty()) graphics.item(stack, panel.centerX() - 8, y + 10);
        y += 54;

        y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable(definition.nameKey()),
                x, y, maxWidth, TitanInterfaceTheme.TEXT, 2) + 4;
        if (meta == null) {
            y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.no_owned_instance"),
                    x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED, 2) + 5;
        } else {
            y = TitanInterfaceTheme.wrapped(graphics, font,
                    Component.translatable("screen.titanbreak.instance_progress", meta.mk(), meta.enhancement(),
                            TitanClientState.masteryLevel(definition.id())),
                    x, y, maxWidth, TitanInterfaceTheme.ACCENT, 2) + 5;
            Component location = Component.translatable(meta.installed()
                    ? "screen.titanbreak.instance_installed" : "screen.titanbreak.instance_vault");
            y = TitanInterfaceTheme.wrapped(graphics, font, location, x, y, maxWidth,
                    meta.installed() ? TitanInterfaceTheme.GOOD : TitanInterfaceTheme.CYAN, 1) + 5;
        }
        y = TitanInterfaceTheme.wrapped(graphics, font,
                Component.translatable("screen.titanbreak.fabricator_access", fabricatorTier, enhancementCap()),
                x, y, maxWidth, TitanInterfaceTheme.CYAN, 2) + 6;
        if (fabricatorTier == 1) {
            y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.mk_requires_two"),
                    x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED, 2) + 5;
        }
        if (y < panel.bottom() - 15) {
            graphics.horizontalLine(x, panel.right() - 10, y, TitanInterfaceTheme.LINE_SOFT);
            y += 7;
            TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.fabricator_hint"),
                    x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED,
                    Math.max(1, (panel.bottom() - y - 6) / (font.lineHeight + 2)));
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
        int panelHeight = Math.min(360, Math.max(210, height - 18));
        int left = width / 2 - panelWidth / 2;
        int top = Math.max(6, height / 2 - panelHeight / 2);
        return new Layout(left, left + panelWidth, top, top + panelHeight);
    }

    private PanelGrid panelGrid(Layout layout) {
        int gap = 6;
        int left = layout.left() + 10;
        int right = layout.right() - 10;
        int top = layout.top() + 39;
        int bottom = layout.bottom() - 61;
        int available = right - left - gap * 2;
        int leftWidth = Math.max(112, Math.min(215, (int) Math.round(available * 0.30D)));
        int middleWidth = Math.max(112, Math.min(230, (int) Math.round(available * 0.32D)));
        if (leftWidth + middleWidth + 100 > available) {
            leftWidth = Math.max(88, available * 30 / 100);
            middleWidth = Math.max(88, available * 31 / 100);
        }
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

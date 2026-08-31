package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.network.StationActionPayload;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.Locale;

public final class SurgeryScreen extends Screen {
    private static final int NODE_W = 19;
    private static final int NODE_H = 14;

    /* Normalized body-map positions. Values are percentages of the map's center-width/height. */
    private static final SlotNode[] BODY_NODES = {
            new SlotNode(AugmentationCatalog.Slot.BRAIN_1, -24, 7),
            new SlotNode(AugmentationCatalog.Slot.BRAIN_2, 24, 7),
            new SlotNode(AugmentationCatalog.Slot.EYE_1, -18, 18),
            new SlotNode(AugmentationCatalog.Slot.EYE_2, 18, 18),
            new SlotNode(AugmentationCatalog.Slot.NERVES_1, -34, 31),
            new SlotNode(AugmentationCatalog.Slot.NERVES_2, 34, 31),
            new SlotNode(AugmentationCatalog.Slot.HEART_1, -19, 43),
            new SlotNode(AugmentationCatalog.Slot.HEART_2, 19, 43),
            new SlotNode(AugmentationCatalog.Slot.SPINE_MAIN, -10, 54),
            new SlotNode(AugmentationCatalog.Slot.SPINE_AUX, 10, 54),
            new SlotNode(AugmentationCatalog.Slot.SKELETON_1, -26, 64),
            new SlotNode(AugmentationCatalog.Slot.SKELETON_2, 26, 64),
            new SlotNode(AugmentationCatalog.Slot.SKIN_1, -46, 70),
            new SlotNode(AugmentationCatalog.Slot.SKIN_2, 46, 70),
            new SlotNode(AugmentationCatalog.Slot.AUX_ORGAN_1, -17, 73),
            new SlotNode(AugmentationCatalog.Slot.AUX_ORGAN_2, 17, 73),
            new SlotNode(AugmentationCatalog.Slot.LEFT_ARM_MAIN, -83, 39),
            new SlotNode(AugmentationCatalog.Slot.LEFT_ARM_AUX, -83, 53),
            new SlotNode(AugmentationCatalog.Slot.RIGHT_ARM_MAIN, 83, 39),
            new SlotNode(AugmentationCatalog.Slot.RIGHT_ARM_AUX, 83, 53),
            new SlotNode(AugmentationCatalog.Slot.LEFT_LEG_MAIN, -40, 84),
            new SlotNode(AugmentationCatalog.Slot.LEFT_LEG_AUX, -40, 95),
            new SlotNode(AugmentationCatalog.Slot.RIGHT_LEG_MAIN, 40, 84),
            new SlotNode(AugmentationCatalog.Slot.RIGHT_LEG_AUX, 40, 95)
    };

    private final String stationSpec;
    private final List<AugmentationCatalog.Definition> definitions = AugmentationCatalog.DEFINITIONS;

    private int selectedAugment;
    private AugmentationCatalog.Slot selectedSlot = AugmentationCatalog.Slot.EYE_1;
    private TitanButton install;
    private TitanButton remove;

    public SurgeryScreen(String stationSpec) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("screen.titanbreak.surgery"));
        this.stationSpec = stationSpec;
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        int y = layout.bottom() - 28;
        int nav = 28;
        int gap = 5;
        int x = layout.left() + 12;

        addRenderableWidget(TitanButton.create(Component.literal("‹"), button -> cycleAugment(-1), x, y, nav, 19));
        x += nav + gap;
        addRenderableWidget(TitanButton.create(Component.literal("›"), button -> cycleAugment(1), x, y, nav, 19));
        x += nav + gap + 4;
        int installWidth = Math.min(132, Math.max(102, layout.width() / 6));
        install = addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.install_selected"),
                button -> installSelected(), x, y, installWidth, 19));

        int doneWidth = 58;
        int removeWidth = Math.min(118, Math.max(96, layout.width() / 7));
        int doneX = layout.right() - 12 - doneWidth;
        int removeX = doneX - gap - removeWidth;
        remove = addRenderableWidget(TitanButton.create(Component.translatable("screen.titanbreak.remove_selected"),
                button -> removeSelected(), removeX, y, removeWidth, 19));
        addRenderableWidget(TitanButton.create(Component.translatable("gui.done"), button -> onClose(),
                doneX, y, doneWidth, 19));

        selectCompatibleSlotIfNeeded();
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (install == null || remove == null || definitions.isEmpty()) return;
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        AugmentationCatalog.Placement placement = definition.placementFor(selectedSlot);
        boolean surgeryRunning = TitanClientState.liveCountdownSeconds("surgeryTicks") > 0.0D;
        boolean placementFree = placement != null;
        if (placement != null) {
            for (AugmentationCatalog.Slot slot : placement.slots()) {
                if (!TitanClientState.installedIn(slot.name()).isEmpty()) {
                    placementFree = false;
                    break;
                }
            }
        }
        install.active = !surgeryRunning && placementFree;
        remove.active = !surgeryRunning && !TitanClientState.installedIn(selectedSlot.name()).isEmpty();
    }

    private void cycleAugment(int direction) {
        if (definitions.isEmpty()) return;
        selectedAugment = Math.floorMod(selectedAugment + direction, definitions.size());
        selectCompatibleSlotIfNeeded();
        refreshButtons();
    }

    private void selectCompatibleSlotIfNeeded() {
        if (definitions.isEmpty()) return;
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        if (definition.canInstallAt(selectedSlot)) return;
        selectedSlot = definition.placements().getFirst().anchor();
    }

    private void installSelected() {
        if (definitions.isEmpty()) return;
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        if (!definition.canInstallAt(selectedSlot)) return;
        send("install", definition.id() + ":" + selectedSlot.name());
    }

    private void removeSelected() {
        send("remove", selectedSlot.name());
    }

    private void send(String action, String argument) {
        ClientPacketDistributor.sendToServer(new StationActionPayload(stationSpec + "|" + action + "|" + argument));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && !definitions.isEmpty()) {
            BodyLayout body = bodyLayout(panelGrid(layout()).middle());
            for (SlotNode node : BODY_NODES) {
                int x = nodeX(body, node);
                int y = nodeY(body, node);
                if (inside(event.x(), event.y(), x, y, NODE_W, NODE_H)) {
                    selectedSlot = node.slot();
                    refreshButtons();
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

        drawModulePanel(graphics, grid.left());
        drawBodyPanel(graphics, bodyLayout(grid.middle()));
        drawSlotInfo(graphics, grid.right());

        double surgerySeconds = TitanClientState.liveCountdownSeconds("surgeryTicks");
        if (surgerySeconds > 0.0D) {
            Component progress = Component.translatable("screen.titanbreak.surgery_progress",
                    String.format(Locale.ROOT, "%.1f", surgerySeconds));
            int progressWidth = font.width(progress);
            int x = layout.right() - 14 - progressWidth;
            graphics.text(font, progress, x, layout.top() + 10, TitanInterfaceTheme.ACCENT, false);
            int meterLeft = Math.max(layout.left() + 150, x - 92);
            if (x - meterLeft > 24) {
                TitanInterfaceTheme.meter(graphics, meterLeft, layout.top() + 15,
                        x - meterLeft - 6, Math.min(1.0D, surgerySeconds / 6.0D), TitanInterfaceTheme.ACCENT);
            }
        }

        graphics.horizontalLine(layout.left() + 10, layout.right() - 10, layout.bottom() - 34, TitanInterfaceTheme.LINE_SOFT);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawModulePanel(GuiGraphicsExtractor graphics, Panel panel) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.module"),
                panel.left(), panel.top(), panel.right());
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        ItemStack stack = stackFor(definition);
        int x = panel.left() + 10;
        int maxWidth = Math.max(70, panel.width() - 20);
        int y = panel.top() + 29;

        graphics.fill(x, y, panel.right() - 10, y + 43, TitanInterfaceTheme.PANEL_SOFT);
        graphics.outline(x, y, panel.width() - 20, 43, TitanInterfaceTheme.LINE_SOFT);
        if (!stack.isEmpty()) {
            graphics.item(stack, x + 7, y + 8);
            graphics.itemDecorations(font, stack, x + 7, y + 8);
        }
        int textX = x + 32;
        int textW = Math.max(44, panel.right() - 10 - textX);
        int afterName = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable(definition.nameKey()),
                textX, y + 6, textW, TitanInterfaceTheme.TEXT, 2);
        graphics.text(font, Component.translatable("screen.titanbreak.tier", definition.tier()), textX,
                Math.min(y + 29, afterName + 1), TitanInterfaceTheme.ACCENT, false);

        y += 54;
        y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable(definition.effectKey()),
                x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED, 4) + 6;
        graphics.horizontalLine(x, panel.right() - 10, y, TitanInterfaceTheme.LINE_SOFT);
        y += 7;
        y = TitanInterfaceTheme.wrapped(graphics, font, compatibleText(definition), x, y, maxWidth,
                TitanInterfaceTheme.CYAN, 4) + 6;
        if (y < panel.bottom() - 30) {
            TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.body_map_hint"),
                    x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED,
                    Math.max(1, (panel.bottom() - y - 24) / (font.lineHeight + 2)));
        }

        String index = (selectedAugment + 1) + " / " + definitions.size();
        graphics.text(font, Component.literal(index), x, panel.bottom() - 15, TitanInterfaceTheme.TEXT_MUTED, false);
    }

    private void drawBodyPanel(GuiGraphicsExtractor graphics, BodyLayout body) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.selected_body_slot"),
                body.panel().left(), body.panel().top(), body.panel().right());
        drawBodySilhouette(graphics, body);

        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        for (SlotNode node : BODY_NODES) {
            int x = nodeX(body, node);
            int y = nodeY(body, node);
            boolean selected = node.slot() == selectedSlot;
            boolean compatible = definition.canInstallAt(node.slot());
            boolean occupied = !TitanClientState.installedIn(node.slot().name()).isEmpty();
            int regionColor = TitanInterfaceTheme.regionColor(node.slot().region());
            Point anchor = regionAnchor(body, node.slot().region());
            TitanInterfaceTheme.connector(graphics, x + NODE_W / 2, y + NODE_H / 2, anchor.x(), anchor.y(),
                    compatible ? regionColor : 0x55374348);

            int fill = occupied ? 0xE5162B24 : compatible ? 0xE5111D20 : 0xE50C1114;
            graphics.fill(x, y, x + NODE_W, y + NODE_H, fill);
            graphics.outline(x, y, NODE_W, NODE_H, occupied ? TitanInterfaceTheme.OCCUPIED : 0xAA435159);
            graphics.centeredText(font, nodeCode(node.slot()), x + NODE_W / 2, y + 3,
                    compatible ? regionColor : 0xFF687377);
            if (selected) TitanInterfaceTheme.selectionFrame(graphics, x, y, NODE_W, NODE_H, TitanInterfaceTheme.ACCENT);
            if (occupied) graphics.fill(x + NODE_W - 4, y + 2, x + NODE_W - 2, y + NODE_H - 2, TitanInterfaceTheme.GOOD);
        }

        Component selected = Component.translatable(selectedSlot.translationKey());
        graphics.centeredText(font, selected, body.panel().centerX(), body.panel().bottom() - 15,
                TitanInterfaceTheme.regionColor(selectedSlot.region()));
    }

    private void drawBodySilhouette(GuiGraphicsExtractor graphics, BodyLayout body) {
        int cx = body.centerX();
        int top = body.mapTop() + 12;
        int h = Math.max(118, body.mapHeight() - 30);
        int shoulder = Math.max(18, body.mapWidth() / 9);
        int torsoHalf = Math.max(14, body.mapWidth() / 12);
        int armW = Math.max(7, body.mapWidth() / 26);
        int legW = Math.max(8, body.mapWidth() / 22);
        int headH = Math.max(20, h * 17 / 100);
        int torsoTop = top + headH + 5;
        int torsoBottom = top + h * 64 / 100;
        int legBottom = top + h;
        int edge = 0xFF415158;
        int bodyFill = 0xE70D1418;

        graphics.fill(cx - torsoHalf, top, cx + torsoHalf, top + headH, edge);
        graphics.fill(cx - torsoHalf + 2, top + 2, cx + torsoHalf - 2, top + headH, bodyFill);
        graphics.fill(cx - shoulder, torsoTop, cx + shoulder, torsoBottom, edge);
        graphics.fill(cx - shoulder + 2, torsoTop + 2, cx + shoulder - 2, torsoBottom, bodyFill);
        graphics.fill(cx - shoulder - armW - 4, torsoTop + 7, cx - shoulder - 2, torsoBottom - 4, edge);
        graphics.fill(cx - shoulder - armW - 2, torsoTop + 9, cx - shoulder - 3, torsoBottom - 6, bodyFill);
        graphics.fill(cx + shoulder + 2, torsoTop + 7, cx + shoulder + armW + 4, torsoBottom - 4, edge);
        graphics.fill(cx + shoulder + 3, torsoTop + 9, cx + shoulder + armW + 2, torsoBottom - 6, bodyFill);
        graphics.fill(cx - legW - 3, torsoBottom + 2, cx - 3, legBottom, edge);
        graphics.fill(cx - legW - 1, torsoBottom + 2, cx - 5, legBottom - 2, bodyFill);
        graphics.fill(cx + 3, torsoBottom + 2, cx + legW + 3, legBottom, edge);
        graphics.fill(cx + 5, torsoBottom + 2, cx + legW + 1, legBottom - 2, bodyFill);

        /* Cybernetic traces instead of a generic filled mannequin. */
        graphics.fill(cx - 1, torsoTop + 4, cx + 2, torsoBottom - 4, 0xAA43D7E8);
        for (int y = torsoTop + 14; y < torsoBottom - 7; y += 11) {
            graphics.horizontalLine(cx - torsoHalf + 5, cx + torsoHalf - 5, y, 0x5543D7E8);
        }
        graphics.fill(cx - 7, top + headH / 2, cx + 7, top + headH / 2 + 2, 0xCC43D7E8);
        graphics.fill(cx - 9, torsoTop + 20, cx - 2, torsoTop + 29, 0xCCE94A57);
        graphics.fill(cx + 3, torsoTop + 34, cx + 10, torsoTop + 43, 0xAAB46EEA);
    }

    private void drawSlotInfo(GuiGraphicsExtractor graphics, Panel panel) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.installed_module"),
                panel.left(), panel.top(), panel.right());
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        int x = panel.left() + 10;
        int maxWidth = Math.max(70, panel.width() - 20);
        int y = panel.top() + 30;
        int regionColor = TitanInterfaceTheme.regionColor(selectedSlot.region());

        y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable(selectedSlot.translationKey()),
                x, y, maxWidth, regionColor, 2) + 7;
        String installed = TitanClientState.installedIn(selectedSlot.name());
        Component installedName = installed.isEmpty() ? Component.translatable("screen.titanbreak.empty") : nameOf(installed);
        y = TitanInterfaceTheme.wrapped(graphics, font, installedName, x, y, maxWidth,
                installed.isEmpty() ? TitanInterfaceTheme.TEXT_MUTED : TitanInterfaceTheme.GOOD, 2) + 9;
        graphics.horizontalLine(x, panel.right() - 10, y, TitanInterfaceTheme.LINE_SOFT);
        y += 8;

        AugmentationCatalog.Placement placement = definition.placementFor(selectedSlot);
        Component compatibility = Component.translatable(placement == null
                ? "screen.titanbreak.slot_not_compatible"
                : "screen.titanbreak.slot_compatible");
        y = TitanInterfaceTheme.wrapped(graphics, font, compatibility, x, y, maxWidth,
                placement == null ? TitanInterfaceTheme.BAD : TitanInterfaceTheme.GOOD, 3) + 7;
        if (placement != null && placement.slots().size() > 1) {
            y = TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.paired_install"),
                    x, y, maxWidth, TitanInterfaceTheme.ACCENT, 2) + 7;
        }

        if (y < panel.bottom() - 42) {
            graphics.horizontalLine(x, panel.right() - 10, y, TitanInterfaceTheme.LINE_SOFT);
            y += 8;
            TitanInterfaceTheme.wrapped(graphics, font, Component.translatable("screen.titanbreak.surgery_hint"),
                    x, y, maxWidth, TitanInterfaceTheme.TEXT_MUTED,
                    Math.max(1, (panel.bottom() - y - 8) / (font.lineHeight + 2)));
        }
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

    private Component nameOf(String id) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(id);
        return definition == null ? Component.literal(id) : Component.translatable(definition.nameKey());
    }

    private ItemStack stackFor(AugmentationCatalog.Definition definition) {
        Item item = ModItems.augmentationByPath(definition.itemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static int nodeX(BodyLayout body, SlotNode node) {
        return body.centerX() + node.xPercent() * body.mapWidth() / 200 - NODE_W / 2;
    }

    private static int nodeY(BodyLayout body, SlotNode node) {
        return body.mapTop() + node.yPercent() * body.mapHeight() / 100 - NODE_H / 2;
    }

    private static Point regionAnchor(BodyLayout body, AugmentationCatalog.Region region) {
        int cx = body.centerX();
        int top = body.mapTop();
        int h = body.mapHeight();
        int w = body.mapWidth();
        return switch (region) {
            case BRAIN -> new Point(cx, top + h * 11 / 100);
            case EYE -> new Point(cx, top + h * 20 / 100);
            case NERVES -> new Point(cx, top + h * 34 / 100);
            case HEART -> new Point(cx - w / 30, top + h * 43 / 100);
            case SPINE -> new Point(cx, top + h * 54 / 100);
            case SKELETON -> new Point(cx, top + h * 63 / 100);
            case SKIN -> new Point(cx + w / 12, top + h * 68 / 100);
            case AUX_ORGAN -> new Point(cx + w / 28, top + h * 73 / 100);
            case LEFT_ARM -> new Point(cx - w / 7, top + h * 47 / 100);
            case RIGHT_ARM -> new Point(cx + w / 7, top + h * 47 / 100);
            case LEFT_LEG -> new Point(cx - w / 18, top + h * 84 / 100);
            case RIGHT_LEG -> new Point(cx + w / 18, top + h * 84 / 100);
        };
    }

    private static String nodeCode(AugmentationCatalog.Slot slot) {
        return switch (slot) {
            case EYE_1 -> "E1";
            case EYE_2 -> "E2";
            case BRAIN_1 -> "B1";
            case BRAIN_2 -> "B2";
            case NERVES_1 -> "N1";
            case NERVES_2 -> "N2";
            case SPINE_MAIN -> "SM";
            case SPINE_AUX -> "SA";
            case HEART_1 -> "H1";
            case HEART_2 -> "H2";
            case SKELETON_1 -> "K1";
            case SKELETON_2 -> "K2";
            case SKIN_1 -> "D1";
            case SKIN_2 -> "D2";
            case LEFT_ARM_MAIN -> "LM";
            case LEFT_ARM_AUX -> "LA";
            case RIGHT_ARM_MAIN -> "RM";
            case RIGHT_ARM_AUX -> "RA";
            case LEFT_LEG_MAIN -> "L1";
            case LEFT_LEG_AUX -> "L2";
            case RIGHT_LEG_MAIN -> "R1";
            case RIGHT_LEG_AUX -> "R2";
            case AUX_ORGAN_1 -> "A1";
            case AUX_ORGAN_2 -> "A2";
        };
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
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
        int side = Math.max(140, Math.min(202, available * 27 / 100));
        int middle = available - side * 2;
        if (middle < 190) {
            side = Math.max(118, (available - 190) / 2);
            middle = available - side * 2;
        }
        Panel a = new Panel(left, left + side, top, bottom);
        Panel b = new Panel(a.right() + gap, a.right() + gap + middle, top, bottom);
        Panel c = new Panel(b.right() + gap, right, top, bottom);
        return new PanelGrid(a, b, c);
    }

    private BodyLayout bodyLayout(Panel panel) {
        int mapTop = panel.top() + 23;
        int mapBottom = panel.bottom() - 23;
        int mapWidth = Math.max(130, panel.width() - 20);
        return new BodyLayout(panel, panel.centerX(), mapTop, mapWidth, Math.max(118, mapBottom - mapTop));
    }

    private record Layout(int left, int right, int top, int bottom) {
        int width() { return right - left; }
    }

    private record Panel(int left, int right, int top, int bottom) {
        int width() { return right - left; }
        int centerX() { return (left + right) / 2; }
    }

    private record PanelGrid(Panel left, Panel middle, Panel right) {}
    private record BodyLayout(Panel panel, int centerX, int mapTop, int mapWidth, int mapHeight) {}
    private record SlotNode(AugmentationCatalog.Slot slot, int xPercent, int yPercent) {}
    private record Point(int x, int y) {}
}

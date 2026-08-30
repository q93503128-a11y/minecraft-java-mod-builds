package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.network.StationActionPayload;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
    private static final int NODE_W = 20;
    private static final int NODE_H = 16;

    private static final SlotNode[] BODY_NODES = {
            new SlotNode(AugmentationCatalog.Slot.EYE_1, -23, 24),
            new SlotNode(AugmentationCatalog.Slot.EYE_2, 3, 24),
            new SlotNode(AugmentationCatalog.Slot.BRAIN_1, -23, 43),
            new SlotNode(AugmentationCatalog.Slot.BRAIN_2, 3, 43),
            new SlotNode(AugmentationCatalog.Slot.NERVES_1, -23, 64),
            new SlotNode(AugmentationCatalog.Slot.NERVES_2, 3, 64),
            new SlotNode(AugmentationCatalog.Slot.SPINE_MAIN, -23, 84),
            new SlotNode(AugmentationCatalog.Slot.SPINE_AUX, 3, 84),
            new SlotNode(AugmentationCatalog.Slot.HEART_1, -23, 104),
            new SlotNode(AugmentationCatalog.Slot.HEART_2, 3, 104),
            new SlotNode(AugmentationCatalog.Slot.SKELETON_1, -23, 124),
            new SlotNode(AugmentationCatalog.Slot.SKELETON_2, 3, 124),
            new SlotNode(AugmentationCatalog.Slot.SKIN_1, -23, 144),
            new SlotNode(AugmentationCatalog.Slot.SKIN_2, 3, 144),
            new SlotNode(AugmentationCatalog.Slot.AUX_ORGAN_1, -23, 164),
            new SlotNode(AugmentationCatalog.Slot.AUX_ORGAN_2, 3, 164),
            new SlotNode(AugmentationCatalog.Slot.LEFT_ARM_MAIN, -78, 80),
            new SlotNode(AugmentationCatalog.Slot.LEFT_ARM_AUX, -78, 101),
            new SlotNode(AugmentationCatalog.Slot.RIGHT_ARM_MAIN, 58, 80),
            new SlotNode(AugmentationCatalog.Slot.RIGHT_ARM_AUX, 58, 101),
            new SlotNode(AugmentationCatalog.Slot.LEFT_LEG_MAIN, -48, 178),
            new SlotNode(AugmentationCatalog.Slot.LEFT_LEG_AUX, -48, 198),
            new SlotNode(AugmentationCatalog.Slot.RIGHT_LEG_MAIN, 28, 178),
            new SlotNode(AugmentationCatalog.Slot.RIGHT_LEG_AUX, 28, 198)
    };

    private final String stationSpec;
    private final List<AugmentationCatalog.Definition> definitions = AugmentationCatalog.DEFINITIONS;

    private int selectedAugment;
    private AugmentationCatalog.Slot selectedSlot = AugmentationCatalog.Slot.EYE_1;
    private Button install;
    private Button remove;

    public SurgeryScreen(String stationSpec) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("screen.titanbreak.surgery"));
        this.stationSpec = stationSpec;
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        int footerY = layout.bottom() - 30;

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleAugment(-1))
                .bounds(layout.left() + 14, footerY, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleAugment(1))
                .bounds(layout.left() + 48, footerY, 30, 20).build());
        install = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.install_selected"), button -> installSelected())
                .bounds(layout.left() + 88, footerY, 140, 20).build());
        remove = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.remove_selected"), button -> removeSelected())
                .bounds(layout.right() - 218, footerY, 126, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(layout.right() - 84, footerY, 70, 20).build());

        selectCompatibleSlotIfNeeded();
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (install == null || remove == null) return;
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
        selectedAugment = Math.floorMod(selectedAugment + direction, definitions.size());
        selectCompatibleSlotIfNeeded();
        refreshButtons();
    }

    private void selectCompatibleSlotIfNeeded() {
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        if (definition.canInstallAt(selectedSlot)) return;
        selectedSlot = definition.placements().getFirst().anchor();
    }

    private void installSelected() {
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
        if (event.button() == 0) {
            BodyLayout body = bodyLayout(layout());
            for (SlotNode node : BODY_NODES) {
                int x = body.centerX() + node.xOffset();
                int y = body.top() + node.yOffset();
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

        int contentTop = layout.top() + 42;
        int contentBottom = layout.bottom() - 40;
        int sideWidth = sideWidth(layout.width());
        int gap = 8;
        int leftPanelRight = layout.left() + sideWidth;
        int rightPanelLeft = layout.right() - sideWidth;

        TitanInterfaceTheme.panel(graphics, layout.left() + 10, contentTop, leftPanelRight, contentBottom);
        TitanInterfaceTheme.panel(graphics, leftPanelRight + gap, contentTop, rightPanelLeft - gap, contentBottom);
        TitanInterfaceTheme.panel(graphics, rightPanelLeft, contentTop, layout.right() - 10, contentBottom);

        drawModulePanel(graphics, layout.left() + 10, contentTop, leftPanelRight, contentBottom);
        drawBodyPanel(graphics, bodyLayout(layout()));
        drawSlotInfo(graphics, rightPanelLeft, contentTop, layout.right() - 10, contentBottom);

        double surgerySeconds = TitanClientState.liveCountdownSeconds("surgeryTicks");
        if (surgerySeconds > 0.0D) {
            Component progress = Component.translatable("screen.titanbreak.surgery_progress",
                    String.format(Locale.ROOT, "%.1f", surgerySeconds));
            int x = layout.right() - 16 - font.width(progress);
            graphics.text(font, progress, x, layout.top() + 11, TitanInterfaceTheme.ACCENT);
            TitanInterfaceTheme.meter(graphics, layout.left() + 130, layout.top() + 14,
                    Math.max(70, x - layout.left() - 146), Math.min(1.0D, surgerySeconds / 6.0D), TitanInterfaceTheme.ACCENT);
        }

        graphics.horizontalLine(layout.left() + 10, layout.right() - 10, layout.bottom() - 36, TitanInterfaceTheme.LINE_SOFT);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawModulePanel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.module"), left, top, right);
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        ItemStack stack = stackFor(definition);

        int cardTop = top + 28;
        graphics.fill(left + 8, cardTop, right - 8, cardTop + 54, TitanInterfaceTheme.PANEL_SOFT);
        graphics.outline(left + 8, cardTop, right - left - 16, 54, TitanInterfaceTheme.LINE_SOFT);
        if (!stack.isEmpty()) {
            graphics.item(stack, left + 15, cardTop + 9);
            graphics.itemDecorations(font, stack, left + 15, cardTop + 9);
        }
        graphics.textWithWordWrap(font, Component.translatable(definition.nameKey()), left + 39, cardTop + 8,
                Math.max(64, right - left - 48), TitanInterfaceTheme.TEXT, false);
        graphics.text(font, Component.translatable("screen.titanbreak.tier", definition.tier()), left + 39, cardTop + 35,
                TitanInterfaceTheme.ACCENT, false);
        graphics.textWithWordWrap(font, Component.translatable(definition.effectKey()), left + 10, cardTop + 64,
                Math.max(70, right - left - 20), TitanInterfaceTheme.TEXT_MUTED, false);

        int listTop = cardTop + 115;
        graphics.textWithWordWrap(font, compatibleText(definition), left + 10, listTop,
                Math.max(70, right - left - 20), TitanInterfaceTheme.CYAN, false);
        graphics.textWithWordWrap(font, Component.translatable("screen.titanbreak.body_map_hint"), left + 10, listTop + 42,
                Math.max(70, right - left - 20), TitanInterfaceTheme.TEXT_MUTED, false);

        int indexY = bottom - 18;
        String index = (selectedAugment + 1) + " / " + definitions.size();
        graphics.centeredText(font, index, (left + right) / 2, indexY, TitanInterfaceTheme.TEXT_MUTED);
    }

    private void drawBodyPanel(GuiGraphicsExtractor graphics, BodyLayout body) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.selected_body_slot"),
                body.left(), body.panelTop(), body.right());

        drawBodySilhouette(graphics, body.centerX(), body.top());
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        for (SlotNode node : BODY_NODES) {
            int x = body.centerX() + node.xOffset();
            int y = body.top() + node.yOffset();
            boolean selected = node.slot() == selectedSlot;
            boolean compatible = definition.canInstallAt(node.slot());
            String installed = TitanClientState.installedIn(node.slot().name());
            boolean occupied = !installed.isEmpty();

            int fill = occupied ? 0xDD335546 : compatible ? 0xD52A4650 : 0xC51C272D;
            graphics.fill(x, y, x + NODE_W, y + NODE_H, fill);
            graphics.outline(x, y, NODE_W, NODE_H, occupied ? TitanInterfaceTheme.OCCUPIED : TitanInterfaceTheme.LINE);
            graphics.centeredText(font, shortLabel(node.slot()), x + NODE_W / 2, y + 4,
                    compatible ? TitanInterfaceTheme.TEXT : TitanInterfaceTheme.TEXT_MUTED);
            if (selected) TitanInterfaceTheme.selectionFrame(graphics, x, y, NODE_W, NODE_H, TitanInterfaceTheme.ACCENT);
        }

        int labelY = body.panelBottom() - 18;
        graphics.centeredText(font, Component.translatable(selectedSlot.translationKey()), body.centerX(), labelY,
                TitanInterfaceTheme.ACCENT);
        drawRegionTrace(graphics, body, selectedSlot.region());
    }

    private void drawBodySilhouette(GuiGraphicsExtractor graphics, int cx, int top) {
        int body = 0xFF1C272D;
        int bodyEdge = 0xFF53656E;
        int nerve = 0xAA4D8996;
        int bone = 0x664E6874;

        graphics.fill(cx - 17, top + 14, cx + 17, top + 54, bodyEdge);
        graphics.fill(cx - 15, top + 16, cx + 15, top + 54, body);
        graphics.fill(cx - 27, top + 56, cx + 27, top + 151, bodyEdge);
        graphics.fill(cx - 25, top + 58, cx + 25, top + 151, body);
        graphics.fill(cx - 53, top + 65, cx - 28, top + 136, bodyEdge);
        graphics.fill(cx - 51, top + 67, cx - 28, top + 134, body);
        graphics.fill(cx + 28, top + 65, cx + 53, top + 136, bodyEdge);
        graphics.fill(cx + 28, top + 67, cx + 51, top + 134, body);
        graphics.fill(cx - 23, top + 153, cx - 3, top + 216, bodyEdge);
        graphics.fill(cx - 21, top + 153, cx - 5, top + 214, body);
        graphics.fill(cx + 3, top + 153, cx + 23, top + 216, bodyEdge);
        graphics.fill(cx + 5, top + 153, cx + 21, top + 214, body);

        graphics.fill(cx - 11, top + 22, cx - 3, top + 28, nerve);
        graphics.fill(cx + 3, top + 22, cx + 11, top + 28, nerve);
        graphics.fill(cx - 11, top + 38, cx + 11, top + 50, bone);
        graphics.fill(cx - 2, top + 56, cx + 2, top + 149, nerve);
        for (int y = top + 63; y <= top + 143; y += 10) {
            graphics.horizontalLine(cx - 11, cx + 11, y, bone);
        }
        graphics.fill(cx - 10, top + 91, cx - 1, top + 102, 0xAA9B5B61);
        graphics.fill(cx + 2, top + 104, cx + 12, top + 116, 0xAA6E566E);
    }

    private void drawRegionTrace(GuiGraphicsExtractor graphics, BodyLayout body, AugmentationCatalog.Region region) {
        SlotNode anchor = null;
        for (SlotNode node : BODY_NODES) {
            if (node.slot().region() == region) {
                anchor = node;
                break;
            }
        }
        if (anchor == null) return;
        int x = body.centerX() + anchor.xOffset() + NODE_W / 2;
        int y = body.top() + anchor.yOffset() + NODE_H / 2;
        int targetX = region == AugmentationCatalog.Region.LEFT_ARM || region == AugmentationCatalog.Region.LEFT_LEG
                ? body.left() + 8 : body.right() - 8;
        TitanInterfaceTheme.connector(graphics, x, y, targetX, y, TitanInterfaceTheme.CYAN);
    }

    private void drawSlotInfo(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        TitanInterfaceTheme.panelHeader(graphics, font, Component.translatable("screen.titanbreak.installed_module"), left, top, right);
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        String installed = TitanClientState.installedIn(selectedSlot.name());
        Component installedName = installed.isEmpty() ? Component.translatable("screen.titanbreak.empty") : nameOf(installed);

        graphics.textWithWordWrap(font, Component.translatable(selectedSlot.translationKey()), left + 10, top + 30,
                Math.max(70, right - left - 20), TitanInterfaceTheme.ACCENT, false);
        if (!installed.isEmpty()) {
            ItemStack installedStack = stackForId(installed);
            if (!installedStack.isEmpty()) graphics.item(installedStack, left + 10, top + 52);
            graphics.textWithWordWrap(font, installedName, left + 34, top + 54,
                    Math.max(60, right - left - 44), TitanInterfaceTheme.GOOD, false);
        } else {
            graphics.text(font, installedName, left + 10, top + 56, TitanInterfaceTheme.TEXT_MUTED);
        }

        AugmentationCatalog.Placement placement = definition.placementFor(selectedSlot);
        Component compatibility = Component.translatable(placement == null
                ? "screen.titanbreak.slot_not_compatible"
                : "screen.titanbreak.slot_compatible");
        graphics.textWithWordWrap(font, compatibility, left + 10, top + 88,
                Math.max(70, right - left - 20), placement == null ? TitanInterfaceTheme.BAD : TitanInterfaceTheme.GOOD, false);
        if (placement != null && placement.slots().size() > 1) {
            graphics.textWithWordWrap(font, Component.translatable("screen.titanbreak.paired_install"), left + 10, top + 116,
                    Math.max(70, right - left - 20), TitanInterfaceTheme.ACCENT, false);
        }

        graphics.horizontalLine(left + 10, right - 10, top + 145, TitanInterfaceTheme.LINE_SOFT);
        graphics.text(font, Component.translatable("screen.titanbreak.module"), left + 10, top + 155, TitanInterfaceTheme.TEXT_MUTED);
        ItemStack candidate = stackFor(definition);
        if (!candidate.isEmpty()) graphics.item(candidate, left + 10, top + 173);
        graphics.textWithWordWrap(font, Component.translatable(definition.nameKey()), left + 34, top + 175,
                Math.max(60, right - left - 44), TitanInterfaceTheme.TEXT, false);

        graphics.textWithWordWrap(font, Component.translatable("screen.titanbreak.surgery_hint"), left + 10, bottom - 39,
                Math.max(70, right - left - 20), TitanInterfaceTheme.TEXT_MUTED, false);
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

    private ItemStack stackForId(String id) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(id);
        return definition == null ? ItemStack.EMPTY : stackFor(definition);
    }

    private static String shortLabel(AugmentationCatalog.Slot slot) {
        if (slot.kind() == AugmentationCatalog.SlotKind.MAIN) return "M";
        if (slot.kind() == AugmentationCatalog.SlotKind.AUXILIARY) return "A";
        return slot.name().endsWith("_1") ? "1" : "2";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int sideWidth(int totalWidth) {
        return Math.max(84, Math.min(210, (totalWidth - 166) / 2));
    }

    private Layout layout() {
        int panelWidth = Math.min(820, Math.max(320, width - 16));
        int panelHeight = Math.min(326, Math.max(286, height - 16));
        int left = width / 2 - panelWidth / 2;
        int top = Math.max(8, height / 2 - panelHeight / 2);
        return new Layout(left, left + panelWidth, top, top + panelHeight);
    }

    private BodyLayout bodyLayout(Layout layout) {
        int contentTop = layout.top() + 42;
        int contentBottom = layout.bottom() - 40;
        int sideWidth = sideWidth(layout.width());
        int left = layout.left() + sideWidth + 8;
        int right = layout.right() - sideWidth - 8;
        return new BodyLayout(left, right, contentTop, contentBottom, (left + right) / 2, contentTop + 20);
    }

    private record SlotNode(AugmentationCatalog.Slot slot, int xOffset, int yOffset) {}
    private record Layout(int left, int right, int top, int bottom) {
        int width() { return right - left; }
    }
    private record BodyLayout(int left, int right, int panelTop, int panelBottom, int centerX, int top) {}
}

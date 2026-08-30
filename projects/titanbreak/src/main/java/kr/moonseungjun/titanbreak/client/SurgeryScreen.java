package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.network.StationActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SurgeryScreen extends Screen {
    private static final int SLOT_W = 24;
    private static final int SLOT_H = 16;

    private final String stationSpec;
    private final List<AugmentationCatalog.Definition> definitions = AugmentationCatalog.DEFINITIONS;
    private final Map<AugmentationCatalog.Slot, Button> slotButtons = new EnumMap<>(AugmentationCatalog.Slot.class);
    private final Map<AugmentationCatalog.Slot, SlotVisual> slotVisuals = new EnumMap<>(AugmentationCatalog.Slot.class);

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
        slotButtons.clear();
        slotVisuals.clear();

        Layout layout = layout();
        int left = layout.left();
        int top = layout.top();
        int cx = layout.centerX();

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleAugment(-1))
                .bounds(left + 16, top + 254, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleAugment(1))
                .bounds(left + 52, top + 254, 32, 20).build());

        install = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.install_selected"), button -> installSelected())
                .bounds(left + 90, top + 254, 126, 20).build());
        remove = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.remove_selected"), button -> removeSelected())
                .bounds(layout.right() - 216, top + 254, 126, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(layout.right() - 84, top + 254, 68, 20).build());

        addSlotButton(AugmentationCatalog.Slot.EYE_1, cx - 27, top + 53);
        addSlotButton(AugmentationCatalog.Slot.EYE_2, cx + 3, top + 53);
        addSlotButton(AugmentationCatalog.Slot.BRAIN_1, cx - 27, top + 74);
        addSlotButton(AugmentationCatalog.Slot.BRAIN_2, cx + 3, top + 74);
        addSlotButton(AugmentationCatalog.Slot.NERVES_1, cx - 27, top + 99);
        addSlotButton(AugmentationCatalog.Slot.NERVES_2, cx + 3, top + 99);
        addSlotButton(AugmentationCatalog.Slot.SPINE_MAIN, cx - 27, top + 120);
        addSlotButton(AugmentationCatalog.Slot.SPINE_AUX, cx + 3, top + 120);
        addSlotButton(AugmentationCatalog.Slot.HEART_1, cx - 27, top + 141);
        addSlotButton(AugmentationCatalog.Slot.HEART_2, cx + 3, top + 141);
        addSlotButton(AugmentationCatalog.Slot.SKELETON_1, cx - 27, top + 162);
        addSlotButton(AugmentationCatalog.Slot.SKELETON_2, cx + 3, top + 162);
        addSlotButton(AugmentationCatalog.Slot.SKIN_1, cx - 27, top + 183);
        addSlotButton(AugmentationCatalog.Slot.SKIN_2, cx + 3, top + 183);
        addSlotButton(AugmentationCatalog.Slot.AUX_ORGAN_1, cx - 27, top + 204);
        addSlotButton(AugmentationCatalog.Slot.AUX_ORGAN_2, cx + 3, top + 204);

        addSlotButton(AugmentationCatalog.Slot.LEFT_ARM_MAIN, cx - 83, top + 110);
        addSlotButton(AugmentationCatalog.Slot.LEFT_ARM_AUX, cx - 83, top + 133);
        addSlotButton(AugmentationCatalog.Slot.RIGHT_ARM_MAIN, cx + 59, top + 110);
        addSlotButton(AugmentationCatalog.Slot.RIGHT_ARM_AUX, cx + 59, top + 133);
        addSlotButton(AugmentationCatalog.Slot.LEFT_LEG_MAIN, cx - 49, top + 225);
        addSlotButton(AugmentationCatalog.Slot.LEFT_LEG_AUX, cx - 49, top + 246);
        addSlotButton(AugmentationCatalog.Slot.RIGHT_LEG_MAIN, cx + 25, top + 225);
        addSlotButton(AugmentationCatalog.Slot.RIGHT_LEG_AUX, cx + 25, top + 246);

        selectCompatibleSlotIfNeeded();
        refreshButtons();
    }

    private void addSlotButton(AugmentationCatalog.Slot slot, int x, int y) {
        Button button = addRenderableWidget(Button.builder(Component.literal(shortLabel(slot)), ignored -> {
                    selectedSlot = slot;
                    refreshButtons();
                })
                .bounds(x, y, SLOT_W, SLOT_H).build());
        slotButtons.put(slot, button);
        slotVisuals.put(slot, new SlotVisual(x, y));
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        int left = layout.left();
        int right = layout.right();
        int top = layout.top();
        int cx = layout.centerX();

        graphics.fill(left, top, right, top + 282, 0xEB11171C);
        graphics.fill(left + 2, top + 2, right - 2, top + 32, 0xFF263038);
        graphics.text(font, title, left + 16, top + 12, 0xFFE7EEF2);

        double surgerySeconds = TitanClientState.liveCountdownSeconds("surgeryTicks");
        if (surgerySeconds > 0.0D) {
            Component progress = Component.translatable("screen.titanbreak.surgery_progress",
                    String.format(Locale.ROOT, "%.1f", surgerySeconds));
            graphics.text(font, progress, right - 16 - font.width(progress), top + 12, 0xFFE2A95B);
        }

        drawBodySilhouette(graphics, cx, top);
        drawSlotBackplates(graphics);

        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        graphics.text(font, Component.translatable("screen.titanbreak.module"), left + 16, top + 48, 0xFF9FB3BF);
        graphics.text(font, Component.translatable(definition.nameKey()), left + 16, top + 68, 0xFFF3D7A2);
        graphics.text(font, Component.translatable("screen.titanbreak.tier", definition.tier()), left + 16, top + 86, 0xFF8599A5);
        graphics.text(font, Component.translatable(definition.effectKey()), left + 16, top + 108, 0xFFD4E0E5);
        graphics.text(font, compatibleText(definition), left + 16, top + 142, 0xFF8FC3D4);
        graphics.text(font, Component.translatable("screen.titanbreak.body_map_hint"), left + 16, top + 176, 0xFF7E929D);

        int infoX = right - 202;
        graphics.text(font, Component.translatable("screen.titanbreak.selected_body_slot"), infoX, top + 48, 0xFF9FB3BF);
        graphics.text(font, Component.translatable(selectedSlot.translationKey()), infoX, top + 68, 0xFFE6D19C);

        String installed = TitanClientState.installedIn(selectedSlot.name());
        Component installedName = installed.isEmpty()
                ? Component.translatable("screen.titanbreak.empty")
                : nameOf(installed);
        graphics.text(font, Component.translatable("screen.titanbreak.installed_module"), infoX, top + 96, 0xFF9FB3BF);
        graphics.text(font, installedName, infoX, top + 116, installed.isEmpty() ? 0xFF71818A : 0xFF8ED0A2);

        AugmentationCatalog.Placement selectedPlacement = definition.placementFor(selectedSlot);
        Component compatibility = Component.translatable(selectedPlacement == null
                ? "screen.titanbreak.slot_not_compatible"
                : "screen.titanbreak.slot_compatible");
        graphics.text(font, compatibility, infoX, top + 148,
                selectedPlacement == null ? 0xFFD87676 : 0xFF7FC4A0);
        if (selectedPlacement != null && selectedPlacement.slots().size() > 1) {
            graphics.text(font, Component.translatable("screen.titanbreak.paired_install"), infoX, top + 168, 0xFFBCA46D);
        }

        graphics.text(font, Component.translatable("screen.titanbreak.surgery_hint"), infoX, top + 202, 0xFF94A7B0);
        graphics.fill(left + 14, top + 244, right - 14, top + 245, 0x553A464D);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBodySilhouette(GuiGraphicsExtractor graphics, int cx, int top) {
        int body = 0xFF202A31;
        int edge = 0xFF4B5A63;
        graphics.fill(cx - 22, top + 43, cx + 22, top + 91, edge);
        graphics.fill(cx - 20, top + 45, cx + 20, top + 91, body);
        graphics.fill(cx - 34, top + 92, cx + 34, top + 214, edge);
        graphics.fill(cx - 32, top + 94, cx + 32, top + 214, body);
        graphics.fill(cx - 66, top + 98, cx - 35, top + 195, edge);
        graphics.fill(cx - 64, top + 100, cx - 35, top + 193, body);
        graphics.fill(cx + 35, top + 98, cx + 66, top + 195, edge);
        graphics.fill(cx + 35, top + 100, cx + 64, top + 193, body);
        graphics.fill(cx - 31, top + 215, cx - 3, top + 267, edge);
        graphics.fill(cx - 29, top + 215, cx - 5, top + 267, body);
        graphics.fill(cx + 3, top + 215, cx + 31, top + 267, edge);
        graphics.fill(cx + 5, top + 215, cx + 29, top + 267, body);
    }

    private void drawSlotBackplates(GuiGraphicsExtractor graphics) {
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        for (Map.Entry<AugmentationCatalog.Slot, SlotVisual> entry : slotVisuals.entrySet()) {
            AugmentationCatalog.Slot slot = entry.getKey();
            SlotVisual visual = entry.getValue();
            boolean compatible = definition.canInstallAt(slot);
            boolean occupied = !TitanClientState.installedIn(slot.name()).isEmpty();
            int color = occupied ? 0xAA467458 : compatible ? 0xAA356C82 : 0x77343C42;
            graphics.fill(visual.x() - 2, visual.y() - 2,
                    visual.x() + SLOT_W + 2, visual.y() + SLOT_H + 2, color);
            if (slot == selectedSlot) {
                graphics.fill(visual.x() - 4, visual.y() - 4,
                        visual.x() + SLOT_W + 4, visual.y() - 2, 0xFFE3B863);
                graphics.fill(visual.x() - 4, visual.y() + SLOT_H + 2,
                        visual.x() + SLOT_W + 4, visual.y() + SLOT_H + 4, 0xFFE3B863);
                graphics.fill(visual.x() - 4, visual.y() - 2,
                        visual.x() - 2, visual.y() + SLOT_H + 2, 0xFFE3B863);
                graphics.fill(visual.x() + SLOT_W + 2, visual.y() - 2,
                        visual.x() + SLOT_W + 4, visual.y() + SLOT_H + 2, 0xFFE3B863);
            }
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

    private static String shortLabel(AugmentationCatalog.Slot slot) {
        if (slot.kind() == AugmentationCatalog.SlotKind.MAIN) return "M";
        if (slot.kind() == AugmentationCatalog.SlotKind.AUXILIARY) return "A";
        return slot.name().endsWith("_1") ? "1" : "2";
    }

    private Layout layout() {
        int panelWidth = Math.min(760, Math.max(300, width - 16));
        int left = width / 2 - panelWidth / 2;
        int top = Math.max(8, height / 2 - 146);
        return new Layout(left, left + panelWidth, top, width / 2);
    }

    private record Layout(int left, int right, int top, int centerX) {}
    private record SlotVisual(int x, int y) {}
}

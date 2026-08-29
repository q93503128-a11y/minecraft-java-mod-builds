package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.network.StationActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

public final class SurgeryScreen extends Screen {
    private final String stationSpec;
    private final List<AugmentationCatalog.Definition> definitions = AugmentationCatalog.DEFINITIONS;
    private final AugmentationCatalog.Slot[] slots = AugmentationCatalog.Slot.values();
    private int selectedAugment;
    private int selectedSlot;
    private Button install;
    private Button installLeft;
    private Button installRight;

    public SurgeryScreen(String stationSpec) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("screen.titanbreak.surgery"));
        this.stationSpec = stationSpec;
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int bottom = height / 2 + 106;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleAugment(-1)).bounds(cx - 170, bottom - 54, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleAugment(1)).bounds(cx - 132, bottom - 54, 34, 20).build());
        install = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.install"), button -> installDefault())
                .bounds(cx - 88, bottom - 54, 104, 20).build());
        installLeft = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.install_left"), button -> installArm(AugmentationCatalog.Slot.LEFT_ARM))
                .bounds(cx - 88, bottom - 54, 104, 20).build());
        installRight = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.install_right"), button -> installArm(AugmentationCatalog.Slot.RIGHT_ARM))
                .bounds(cx + 20, bottom - 54, 104, 20).build());

        addRenderableWidget(Button.builder(Component.literal("<"), button -> selectedSlot = Math.floorMod(selectedSlot - 1, slots.length))
                .bounds(cx - 170, bottom - 26, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> selectedSlot = Math.floorMod(selectedSlot + 1, slots.length))
                .bounds(cx - 132, bottom - 26, 34, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.remove"), button -> removeSelected())
                .bounds(cx - 88, bottom - 26, 104, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(cx + 20, bottom - 26, 104, 20).build());
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (install == null) return;
        boolean arm = definitions.get(selectedAugment).armModule();
        install.visible = !arm;
        install.active = !arm;
        installLeft.visible = arm;
        installLeft.active = arm;
        installRight.visible = arm;
        installRight.active = arm;
    }

    private void cycleAugment(int direction) {
        selectedAugment = Math.floorMod(selectedAugment + direction, definitions.size());
        refreshButtons();
    }

    private void installDefault() {
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        send("install", definition.id());
    }

    private void installArm(AugmentationCatalog.Slot slot) {
        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        send("install", definition.id() + ":" + slot.name());
    }

    private void removeSelected() {
        send("remove", slots[selectedSlot].name());
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
        int cx = width / 2;
        int top = height / 2 - 118;
        graphics.fill(cx - 190, top, cx + 190, top + 204, 0xE511171C);
        graphics.fill(cx - 188, top + 2, cx + 188, top + 32, 0xFF263038);
        graphics.text(font, title, cx - 174, top + 12, 0xFFE7EEF2);

        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        graphics.text(font, Component.translatable("screen.titanbreak.module"), cx - 174, top + 48, 0xFF9FB3BF);
        graphics.text(font, Component.translatable(definition.nameKey()), cx - 174, top + 66, 0xFFF3D7A2);
        graphics.text(font, Component.translatable(definition.effectKey()), cx - 174, top + 86, 0xFFD4E0E5);

        AugmentationCatalog.Slot slot = slots[selectedSlot];
        String installed = TitanClientState.installedIn(slot.name());
        Component installedName = installed.isEmpty()
                ? Component.translatable("screen.titanbreak.empty")
                : nameOf(installed);
        graphics.text(font, Component.translatable("screen.titanbreak.slot", Component.translatable("slot.titanbreak." + slot.name().toLowerCase())),
                cx - 174, top + 120, 0xFF9FB3BF);
        graphics.text(font, installedName, cx - 174, top + 138, installed.isEmpty() ? 0xFF71818A : 0xFF8ED0A2);

        int surgeryTicks = TitanClientState.integer("surgeryTicks", 0);
        if (surgeryTicks > 0) {
            graphics.text(font, Component.translatable("screen.titanbreak.surgery_progress", String.format(java.util.Locale.ROOT, "%.1f", surgeryTicks / 20.0D)),
                    cx + 30, top + 138, 0xFFE2A95B);
        }
        graphics.text(font, Component.translatable("screen.titanbreak.surgery_hint"), cx - 174, top + 166, 0xFF94A7B0);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private Component nameOf(String id) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(id);
        return definition == null ? Component.literal(id) : Component.translatable(definition.nameKey());
    }
}

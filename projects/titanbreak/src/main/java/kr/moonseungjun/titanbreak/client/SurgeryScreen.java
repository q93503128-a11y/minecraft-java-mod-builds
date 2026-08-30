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

import java.util.List;
import java.util.Locale;

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
        int panelWidth = Math.min(420, Math.max(296, width - 24));
        int panelLeft = width / 2 - panelWidth / 2;
        int innerLeft = panelLeft + 16;
        int actionLeft = innerLeft + 80;
        int actionWidth = panelWidth - 112;
        int actionHalf = Math.max(72, (actionWidth - 6) / 2);
        int top = Math.max(12, height / 2 - 142);
        int installY = top + 202;
        int removeY = top + 230;

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleAugment(-1))
                .bounds(innerLeft, installY, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleAugment(1))
                .bounds(innerLeft + 40, installY, 34, 20).build());

        install = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.install"), button -> installDefault())
                .bounds(actionLeft, installY, actionWidth, 20).build());
        installLeft = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.install_left"), button -> installArm(AugmentationCatalog.Slot.LEFT_ARM))
                .bounds(actionLeft, installY, actionHalf, 20).build());
        installRight = addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.install_right"), button -> installArm(AugmentationCatalog.Slot.RIGHT_ARM))
                .bounds(actionLeft + actionHalf + 6, installY, Math.max(72, actionWidth - actionHalf - 6), 20).build());

        addRenderableWidget(Button.builder(Component.literal("<"), button -> selectedSlot = Math.floorMod(selectedSlot - 1, slots.length))
                .bounds(innerLeft, removeY, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> selectedSlot = Math.floorMod(selectedSlot + 1, slots.length))
                .bounds(innerLeft + 40, removeY, 34, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.remove"), button -> removeSelected())
                .bounds(actionLeft, removeY, actionHalf, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(actionLeft + actionHalf + 6, removeY, Math.max(72, actionWidth - actionHalf - 6), 20).build());
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
        int panelWidth = Math.min(420, Math.max(296, width - 24));
        int left = width / 2 - panelWidth / 2;
        int right = left + panelWidth;
        int top = Math.max(12, height / 2 - 142);

        graphics.fill(left, top, right, top + 258, 0xEB11171C);
        graphics.fill(left + 2, top + 2, right - 2, top + 32, 0xFF263038);
        graphics.text(font, title, left + 16, top + 12, 0xFFE7EEF2);

        AugmentationCatalog.Definition definition = definitions.get(selectedAugment);
        graphics.text(font, Component.translatable("screen.titanbreak.module"), left + 16, top + 48, 0xFF9FB3BF);
        graphics.text(font, Component.translatable(definition.nameKey()), left + 16, top + 66, 0xFFF3D7A2);
        graphics.text(font, Component.translatable(definition.effectKey()), left + 16, top + 86, 0xFFD4E0E5);

        MutableComponent compatible = Component.translatable("screen.titanbreak.install").append(" · ");
        if (definition.armModule()) {
            compatible.append(Component.translatable("slot.titanbreak.left_arm"))
                    .append(" / ")
                    .append(Component.translatable("slot.titanbreak.right_arm"));
        } else {
            compatible.append(Component.translatable("slot.titanbreak." + definition.slot().name().toLowerCase(Locale.ROOT)));
        }
        graphics.text(font, compatible, left + 16, top + 112, 0xFF8FC3D4);

        AugmentationCatalog.Slot slot = slots[selectedSlot];
        String installed = TitanClientState.installedIn(slot.name());
        Component installedName = installed.isEmpty()
                ? Component.translatable("screen.titanbreak.empty")
                : nameOf(installed);
        graphics.text(font,
                Component.translatable("screen.titanbreak.slot", Component.translatable("slot.titanbreak." + slot.name().toLowerCase(Locale.ROOT))),
                left + 16, top + 136, 0xFF9FB3BF);
        graphics.text(font, installedName, left + 16, top + 154, installed.isEmpty() ? 0xFF71818A : 0xFF8ED0A2);

        double surgerySeconds = TitanClientState.liveCountdownSeconds("surgeryTicks");
        if (surgerySeconds > 0.0D) {
            Component progress = Component.translatable("screen.titanbreak.surgery_progress",
                    String.format(Locale.ROOT, "%.1f", surgerySeconds));
            int progressX = Math.max(left + 16, right - 16 - font.width(progress));
            graphics.text(font, progress, progressX, top + 154, 0xFFE2A95B);
        }

        graphics.text(font, Component.translatable("screen.titanbreak.surgery_hint"), left + 16, top + 178, 0xFF94A7B0);
        graphics.fill(left + 14, top + 194, right - 14, top + 195, 0x553A464D);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private Component nameOf(String id) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(id);
        return definition == null ? Component.literal(id) : Component.translatable(definition.nameKey());
    }
}

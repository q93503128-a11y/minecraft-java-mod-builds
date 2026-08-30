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

    public FabricatorScreen(String stationSpec) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("screen.titanbreak.fabricator"));
        this.stationSpec = stationSpec;
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int bottom = height / 2 + 92;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycle(-1)).bounds(cx - 150, bottom - 26, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycle(1)).bounds(cx - 112, bottom - 26, 34, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.fabricate"), button -> fabricate())
                .bounds(cx - 70, bottom - 26, 140, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.titanbreak.assemble_surgery"), button -> send("assemble_surgery", ""))
                .bounds(cx + 78, bottom - 26, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(cx - 50, bottom + 2, 100, 20).build());
    }

    private void cycle(int direction) {
        selected = Math.floorMod(selected + direction, definitions.size());
    }

    private void fabricate() {
        send("fabricate", definitions.get(selected).id());
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
        int top = height / 2 - 108;
        graphics.fill(cx - 176, top, cx + 246, top + 196, 0xE512181D);
        graphics.fill(cx - 174, top + 2, cx + 244, top + 32, 0xFF1D2B33);
        graphics.text(font, title, cx - 160, top + 12, 0xFFE7EEF2);

        AugmentationCatalog.Definition definition = definitions.get(selected);
        graphics.text(font, Component.translatable(definition.nameKey()), cx - 160, top + 48, 0xFFF3D7A2);
        graphics.text(font, Component.translatable("screen.titanbreak.tier", definition.tier()), cx - 160, top + 64, 0xFF9FB3BF);
        graphics.text(font, Component.translatable(definition.effectKey()), cx - 160, top + 84, 0xFFD4E0E5);
        graphics.text(font, compatibleText(definition), cx - 160, top + 108, 0xFF8FC3D4);
        graphics.text(font, Component.translatable("screen.titanbreak.requirements"), cx + 54, top + 48, 0xFFE7EEF2);

        int y = top + 68;
        for (Map.Entry<String, Integer> requirement : definition.recipe().entrySet()) {
            int owned = owned(requirement.getKey());
            int color = owned >= requirement.getValue() ? 0xFF88D59C : 0xFFE17B78;
            graphics.text(font, Component.translatable("screen.titanbreak.requirement_line",
                    Component.translatable("item.titanbreak." + requirement.getKey()), owned, requirement.getValue()),
                    cx + 54, y, color);
            y += 16;
        }
        graphics.text(font, Component.translatable("screen.titanbreak.fabricator_hint"), cx - 160, top + 140, 0xFF94A7B0);
        graphics.text(font, Component.translatable("screen.titanbreak.surgery_cost_short"), cx + 54, top + 140, 0xFF94A7B0);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
}

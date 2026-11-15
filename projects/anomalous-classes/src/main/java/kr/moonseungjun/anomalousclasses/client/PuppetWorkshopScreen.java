package kr.moonseungjun.anomalousclasses.client;

import java.util.Map;
import kr.moonseungjun.anomalousclasses.item.PuppetItems;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetWorkshopActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetWorkshopStatePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.PuppetBlueprint;
import kr.moonseungjun.anomalousclasses.state.PuppetEconomy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Uses the existing project menu art; costs and action enablement originate on the server. */
public final class PuppetWorkshopScreen extends Screen {
    private PuppetWorkshopStatePayload state;
    private MenuUi.Box outer, ingredients;
    private MenuUi.Button assembleButton, closeButton;
    private boolean pending;

    public PuppetWorkshopScreen(PuppetWorkshopStatePayload state) {
        super(Component.translatable("screen.anomalousclasses.puppet_workbench.title"));
        this.state = state;
    }

    public boolean matches(PuppetWorkshopStatePayload updated) {
        return state.station().equals(updated.station());
    }

    public void applyServerState(PuppetWorkshopStatePayload updated) {
        state = updated;
        pending = false;
        if (assembleButton != null) assembleButton.active = updated.craftable();
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(width - 16, 430);
        int panelHeight = Math.min(height - 16, 250);
        outer = new MenuUi.Box((width - panelWidth) / 2, (height - panelHeight) / 2,
                panelWidth, panelHeight);
        ingredients = new MenuUi.Box(outer.x()+12, outer.y()+53,
                outer.width()-24, Math.max(72, outer.height()-115));
        int controls = Math.min(210, outer.width()-30);
        int buttonY = outer.bottom()-39;
        assembleButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(
                outer.x()+outer.width()/2-controls/2, buttonY, controls*2/3, 22),
                Component.translatable("screen.anomalousclasses.puppet_workbench.assemble"),
                button -> assemble()));
        closeButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(
                assembleButton.getX()+assembleButton.getWidth()+5, buttonY,
                Math.max(26, controls/3-5), 22),
                Component.translatable("screen.anomalousclasses.puppet_workbench.close"),
                button -> onClose()));
        assembleButton.active = state.craftable();
    }

    private void assemble() {
        if (pending || !state.craftable()) return;
        pending = true;
        assembleButton.active = false;
        PacketDistributor.sendToServer(new PuppetWorkshopActionPayload(state.station()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x681A1825);
        MenuUi.drawPanel(graphics, outer);
        MenuUi.drawTitle(graphics, font, title, outer, width, height);
        MenuUi.drawInsetSurface(graphics, ingredients);
        graphics.fill(ingredients.x()+6, ingredients.y()+3,
                ingredients.right()-6, ingredients.y()+5, MenuUi.accent(ClassId.PUPPETEER));
        MenuUi.drawFit(graphics, font,
                Component.translatable("screen.anomalousclasses.puppet_workbench.intro"),
                ingredients.x()+10, ingredients.y()+11, ingredients.width()-20, 0xFFE6C788);

        Map<String, Integer> bill = PuppetEconomy.starterBlueprintLedger(PuppetBlueprint.Size.NORMAL);
        int index = 0;
        for (var entry : bill.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            int cols = ingredients.width() >= 270 ? 4 : 2;
            int itemWidth = ingredients.width() / cols;
            int row = index / cols, column = index % cols;
            int x = ingredients.x()+10+column*itemWidth;
            int y = ingredients.y()+29+row*37;
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
            graphics.renderItem(new ItemStack(item), x, y);
            MenuUi.drawFit(graphics, font, Component.literal("×"+entry.getValue()),
                    x+18, y+4, Math.max(8, itemWidth-28), 0xFFE2D6C5);
            MenuUi.drawFit(graphics, font, item.getDescription(),
                    x, y+19, Math.max(8, itemWidth-14), 0xFFB7AC9D);
            index++;
        }

        String info = state.result() == PuppetWorkshopStatePayload.CRAFTED
                ? "screen.anomalousclasses.puppet_workbench.done"
                : state.result() == PuppetWorkshopStatePayload.REJECTED
                ? "screen.anomalousclasses.puppet_workbench.rejected"
                : state.craftable()
                ? "screen.anomalousclasses.puppet_workbench.ready"
                : "screen.anomalousclasses.puppet_workbench.missing";
        MenuUi.drawFit(graphics, font, Component.translatable(info),
                outer.x()+16, outer.bottom()-55, outer.width()-32,
                state.craftable() ? 0xFFACD8B4 : 0xFFE6B18C);
        for (Renderable widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
    }
}

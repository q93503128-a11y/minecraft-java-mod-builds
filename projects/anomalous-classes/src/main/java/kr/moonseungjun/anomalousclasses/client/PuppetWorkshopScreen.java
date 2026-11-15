package kr.moonseungjun.anomalousclasses.client;

import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetWorkshopActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetWorkshopStatePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.PuppetBlueprint;
import kr.moonseungjun.anomalousclasses.state.PuppetEconomy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One familiar workbench screen: assemble, expand, repair and permanent dismantle.
 * All material quotes and button enabled states are returned by the game server.
 */
public final class PuppetWorkshopScreen extends Screen {
    private PuppetWorkshopStatePayload state;
    private MenuUi.Box outer;
    private MenuUi.Button[] tabs;
    private MenuUi.Button prevButton, nextButton, confirmButton, closeButton;
    private int mode = PuppetWorkshopActionPayload.CRAFT;
    private int selectedIndex;
    private int selectedDevice;
    private UUID selectedId;
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
        if (selectedId != null) {
            for (int i = 0; i < updated.puppets().size(); i++) {
                if (updated.puppets().get(i).id().equals(selectedId)) {
                    selectedIndex = i;
                    break;
                }
                if (i == updated.puppets().size() - 1) {
                    selectedIndex = 0;
                    selectedId = null;
                }
            }
        }
        if (!updated.puppets().isEmpty()) {
            selectedIndex = Math.min(selectedIndex, updated.puppets().size() - 1);
            selectedId = updated.puppets().get(selectedIndex).id();
        } else {
            selectedIndex = 0;
            selectedId = null;
        }
        refresh();
    }

    @Override
    protected void init() {
        int width = Math.max(160, Math.min(this.width - 12, 466));
        int height = Math.max(140, Math.min(this.height - 12, 274));
        outer = new MenuUi.Box((this.width - width) / 2, (this.height - height) / 2, width, height);
        int tabWidth = Math.max(22, (outer.width() - 26) / 4);
        int tabX = outer.x() + 10, tabY = outer.y() + 36;
        String[] keys = {"assemble", "repair", "expand", "dismantle"};
        tabs = new MenuUi.Button[4];
        for (int i = 0; i < tabs.length; i++) {
            final int action = i;
            tabs[i] = addRenderableWidget(new MenuUi.Button(
                    new MenuUi.Box(tabX + i * tabWidth, tabY, tabWidth - 2, 22),
                    Component.translatable("screen.anomalousclasses.puppet_workbench." + keys[i]),
                    button -> { mode = action; refresh(); }));
        }
        int navigationY = tabY + 29;
        prevButton = addRenderableWidget(new MenuUi.Button(
                new MenuUi.Box(outer.x()+11, navigationY, 34, 19),
                Component.literal("◀"), b -> select(-1)));
        nextButton = addRenderableWidget(new MenuUi.Button(
                new MenuUi.Box(outer.right()-45, navigationY, 34, 19),
                Component.literal("▶"), b -> select(1)));
        int bottom = outer.bottom()-32;
        confirmButton = addRenderableWidget(new MenuUi.Button(
                new MenuUi.Box(outer.x()+12, bottom, Math.max(50, outer.width()-111), 22),
                Component.translatable("screen.anomalousclasses.puppet_workbench.confirm"),
                b -> confirm()));
        closeButton = addRenderableWidget(new MenuUi.Button(
                new MenuUi.Box(outer.right()-92, bottom, 80, 22),
                Component.translatable("screen.anomalousclasses.puppet_workbench.close"),
                b -> onClose()));
        if (selectedId == null && !state.puppets().isEmpty())
            selectedId = state.puppets().get(0).id();
        refresh();
    }

    private PuppetWorkshopStatePayload.PuppetView selected() {
        if (state.puppets().isEmpty()) return null;
        return state.puppets().get(Math.min(selectedIndex, state.puppets().size()-1));
    }

    private void select(int delta) {
        if (pending) return;
        if (mode == PuppetWorkshopActionPayload.CRAFT) {
            selectedDevice = Math.floorMod(selectedDevice + delta, state.craftChoices().size());
        } else {
            if (state.puppets().isEmpty()) return;
            selectedIndex = Math.floorMod(selectedIndex + delta, state.puppets().size());
            selectedId = selected().id();
        }
        refresh();
    }

    private PuppetWorkshopStatePayload.CraftChoice selectedRecipe() {
        return state.craftChoices().get(selectedDevice);
    }

    private boolean available() {
        if (pending) return false;
        var puppet = selected();
        return switch (mode) {
            case PuppetWorkshopActionPayload.CRAFT -> selectedRecipe().craftable();
            case PuppetWorkshopActionPayload.REPAIR -> puppet != null && puppet.canRepair();
            case PuppetWorkshopActionPayload.EXPAND -> puppet != null && puppet.canExpand();
            case PuppetWorkshopActionPayload.DISMANTLE -> puppet != null && puppet.canDismantle();
            default -> false;
        };
    }

    private void refresh() {
        if (tabs == null) return;
        for (MenuUi.Button tab : tabs) tab.active = !pending;
        prevButton.active = nextButton.active = !pending &&
                (mode == PuppetWorkshopActionPayload.CRAFT || !state.puppets().isEmpty());
        confirmButton.active = available();
        confirmButton.setMessage(Component.translatable(
                mode == PuppetWorkshopActionPayload.CRAFT ? "screen.anomalousclasses.puppet_workbench.assemble"
                : mode == PuppetWorkshopActionPayload.REPAIR ? "screen.anomalousclasses.puppet_workbench.repair"
                : mode == PuppetWorkshopActionPayload.EXPAND ? "screen.anomalousclasses.puppet_workbench.expand"
                : "screen.anomalousclasses.puppet_workbench.dismantle"));
    }

    private void confirm() {
        if (!available()) return;
        PuppetWorkshopStatePayload.PuppetView puppet = selected();
        if (mode != PuppetWorkshopActionPayload.CRAFT && puppet == null) return;
        int slot = puppet == null ? -1 : puppet.slot();
        int revision = puppet == null ? 0 : puppet.revision();
        UUID id = puppet == null ? new UUID(0,0) : puppet.id();
        pending = true;
        refresh();
        PacketDistributor.sendToServer(new PuppetWorkshopActionPayload(
                state.station(), mode, slot, id, revision, selectedDevice));
    }

    private Map<String,Integer> quote() {
        var puppet = selected();
        if (mode == PuppetWorkshopActionPayload.CRAFT)
            return selectedRecipe().bill();
        if (puppet == null) return Map.of();
        return switch (mode) {
            case PuppetWorkshopActionPayload.REPAIR -> puppet.repairCost();
            case PuppetWorkshopActionPayload.EXPAND -> puppet.expandCost();
            case PuppetWorkshopActionPayload.DISMANTLE -> puppet.refund();
            default -> Map.of();
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x681A1825);
        MenuUi.drawPanel(graphics, outer);
        MenuUi.drawTitle(graphics, font, title, outer, width, height);
        graphics.fill(outer.x()+13, outer.y()+32, outer.right()-13, outer.y()+33,
                MenuUi.accent(ClassId.PUPPETEER));
        var puppet = selected();
        int naviY = outer.y()+69;
        Component detail;
        if (mode == PuppetWorkshopActionPayload.CRAFT) {
            detail = Component.translatable("screen.anomalousclasses.puppet_workbench.assemble_choice",
                    Component.translatable("puppet.anomalousclasses.device." +
                            PuppetBlueprint.Device.values()[selectedDevice].name().toLowerCase()),
                    selectedDevice + 1, state.craftChoices().size());
        } else if (puppet == null) detail = Component.translatable(
                "screen.anomalousclasses.puppet_workbench.no_puppet");
        else {
            double size = PuppetBlueprint.Size.values()[Math.max(0, Math.min(4, puppet.size()))].height();
            detail = Component.translatable("screen.anomalousclasses.puppet_workbench.selected",
                    selectedIndex+1, state.puppets().size(), size,
                    puppet.condition());
        }
        MenuUi.drawFit(graphics, font, detail, outer.x()+52,naviY+5,outer.width()-104,0xFFF0DEBC);

        int qY = Math.min(outer.y()+107, outer.bottom()-109);
        String type = mode == PuppetWorkshopActionPayload.DISMANTLE
                ? "screen.anomalousclasses.puppet_workbench.refund"
                : "screen.anomalousclasses.puppet_workbench.cost";
        MenuUi.drawFit(graphics,font,Component.translatable(type),outer.x()+16,qY-13,
                outer.width()-32,0xFFE2BD84);
        MenuUi.drawInsetSurface(graphics, new MenuUi.Box(
                outer.x()+12,qY,outer.width()-24,Math.max(30,outer.bottom()-qY-62)));
        var entries = quote().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            int column = index % 4, row = index / 4;
            int cell = (outer.width()-30)/4;
            int x = outer.x()+16+column*cell;
            int y = qY+6+row*22;
            if (y+18 > outer.bottom()-62) break;
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
            ItemStack itemStack = new ItemStack(item);
            graphics.renderItem(itemStack,x,y);
            MenuUi.drawFit(graphics,font,Component.literal("×"+entry.getValue()),
                    x+18,y+5,Math.max(10,cell-20),0xFFE9E3D9);
            if(mouseX>=x&&mouseX<x+18&&mouseY>=y&&mouseY<y+18)
                graphics.renderTooltip(font,itemStack,mouseX,mouseY);
        }

        String info = switch (state.result()) {
            case PuppetWorkshopStatePayload.CRAFTED -> "screen.anomalousclasses.puppet_workbench.done";
            case PuppetWorkshopStatePayload.REPAIRED -> "screen.anomalousclasses.puppet_workbench.repaired";
            case PuppetWorkshopStatePayload.EXPANDED -> "screen.anomalousclasses.puppet_workbench.expanded";
            case PuppetWorkshopStatePayload.DISMANTLED -> "screen.anomalousclasses.puppet_workbench.dismantled";
            case PuppetWorkshopStatePayload.REJECTED -> "screen.anomalousclasses.puppet_workbench.rejected";
            default -> available() ? "screen.anomalousclasses.puppet_workbench.ready"
                    : "screen.anomalousclasses.puppet_workbench.missing";
        };
        MenuUi.drawFit(graphics,font,Component.translatable(info),outer.x()+16,
                outer.bottom()-54,outer.width()-32,
                available() ? 0xFFB6E2C0 : 0xFFE6B18C);
        for (int i=0;i<tabs.length;i++){
            MenuUi.Box tabBox=new MenuUi.Box(tabs[i].getX(),tabs[i].getY(),
                    tabs[i].getWidth(),tabs[i].getHeight());
            if(mode==i)graphics.fill(tabBox.x()+3,tabBox.bottom()-2,
                    tabBox.right()-3,tabBox.bottom(),MenuUi.accent(ClassId.PUPPETEER));
        }
        for (Renderable renderable : renderables) renderable.render(graphics,mouseX,mouseY,partialTick);
    }
}

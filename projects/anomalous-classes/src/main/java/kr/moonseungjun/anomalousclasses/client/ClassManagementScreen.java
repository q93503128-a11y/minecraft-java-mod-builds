package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassMenuPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClassManagementScreen extends Screen {
    private final ProfileStatePayload profile;
    private final ClassId classId;
    private MenuUi.Box outer, central;
    private MenuUi.Button profileButton, changeButton;
    private boolean pending;

    public ClassManagementScreen(ProfileStatePayload profile) {
        super(Component.translatable("screen.anomalousclasses.class_management.title"));
        this.profile = profile;
        this.classId = ClassId.fromNetworkIndex(profile.classIndex()).orElse(ClassId.MENTAL_DOMINATOR);
    }

    @Override
    protected void init() {
        int margin = width < 360 ? 5 : 10;
        int top = height < 250 ? 4 : 10;
        outer = new MenuUi.Box(margin, top, Math.max(1, width - margin * 2), Math.max(1, height - top * 2));
        int footerHeight = Math.max(19, Math.min(27, height / 13));
        int footerY = outer.bottom() - footerHeight - 5;
        int buttonWidth = Math.max(1, Math.min(132, (outer.width() - 28) / 2));
        int profileX = outer.x() + 10;
        int changeWidth = Math.max(1, Math.min(112, (outer.width() - 28) / 2));
        int changeX = outer.right() - changeWidth - 10;
        profileButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(profileX, footerY, buttonWidth, footerHeight),
                Component.translatable("screen.anomalousclasses.class_management.profile"), MenuUi.accent(classId),
                button -> openProfile()));
        changeButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(changeX, footerY,
                changeWidth, footerHeight), Component.translatable("screen.anomalousclasses.class_management.change_class"),
                MenuUi.accent(classId), button -> openClassSelection()));
        central = new MenuUi.Box(outer.x() + 8, outer.y() + 63, Math.max(1, outer.width() - 16),
                Math.max(1, footerY - (outer.y() + 72)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, outer, MenuUi.accent(classId));
        MenuUi.drawCentered(graphics, font, title, width / 2, outer.y() + 5, outer.width() - 24, 0xFFF1E7D7);
        MenuUi.drawClassIcon(graphics, classId, outer.x() + 16, outer.y() + 31, 25);
        MenuUi.drawFit(graphics, font, Component.translatable("class.anomalousclasses." + classId.key()),
                outer.x() + 48, outer.y() + 38, Math.max(1, outer.width() - 64), 0xFFF0E6D8);
        MenuUi.drawPanel(graphics, central, MenuUi.accent(classId));
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }

    private void openClassSelection() {
        if (pending) return;
        pending = true;
        profileButton.active = false;
        changeButton.active = false;
        PacketDistributor.sendToServer(RequestClassMenuPayload.INSTANCE);
    }

    private void openProfile() {
        if (pending) return;
        pending = true;
        profileButton.active = false;
        changeButton.active = false;
        PacketDistributor.sendToServer(RequestProfilePayload.INSTANCE);
    }
}

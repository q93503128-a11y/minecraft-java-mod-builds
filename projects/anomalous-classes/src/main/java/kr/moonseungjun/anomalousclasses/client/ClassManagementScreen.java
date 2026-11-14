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

/** Honest fallback for classes whose server management state is not available yet. */
public final class ClassManagementScreen extends Screen {
    private final ProfileStatePayload profile;
    private final ClassId classId;
    private ClassManagementLayout layout;
    private MenuUi.Button backButton;
    private MenuUi.Button changeButton;
    private boolean pending;

    public ClassManagementScreen(ProfileStatePayload profile) {
        super(Component.translatable("screen.anomalousclasses.class_management.title"));
        this.profile = profile;
        this.classId = ClassId.fromNetworkIndex(profile.classIndex()).orElse(ClassId.MENTAL_DOMINATOR);
    }

    @Override
    protected void init() {
        boolean returnToProfile = ClassManagementNavigation.shouldReturnToProfile();
        layout = ClassManagementLayout.calculate(width, height, returnToProfile);
        if (returnToProfile) {
            backButton = addRenderableWidget(new MenuUi.Button(layout.backButton(),
                    Component.translatable("screen.anomalousclasses.class_management.back_to_profile"), button -> {
                        if (pending) return;
                        pending = true;
                        ClassManagementNavigation.clearReturnToProfile();
                        refreshButtons();
                        PacketDistributor.sendToServer(RequestProfilePayload.INSTANCE);
                    }));
        }
        changeButton = addRenderableWidget(new MenuUi.Button(layout.changeButton(),
                Component.translatable("screen.anomalousclasses.class_management.change_class"), button -> {
                    if (pending) return;
                    pending = true;
                    refreshButtons();
                    PacketDistributor.sendToServer(RequestClassMenuPayload.INSTANCE);
                }));
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, layout.outer());
        MenuUi.drawTitle(graphics, font, title, layout.outer(), width, height);

        MenuUi.Box content = layout.content();
        int iconSize = Math.min(34, Math.max(22, content.height() / 5));
        Component className = Component.translatable("class.anomalousclasses." + classId.key());
        int nameWidth = Math.max(1, Math.min(font.width(className), content.width() - iconSize - 8));
        int identityWidth = iconSize + 7 + nameWidth;
        int identityX = content.x() + (content.width() - identityWidth) / 2;
        int identityY = content.y() + Math.max(0, (content.height() - iconSize - font.lineHeight - 14) / 2);
        MenuUi.drawClassIcon(graphics, classId, identityX, identityY, iconSize);
        MenuUi.drawFit(graphics, font, className, identityX + iconSize + 7,
                identityY + Math.max(0, (iconSize - font.lineHeight) / 2), nameWidth, 0xFFF1E7D7);
        int dividerY = identityY + iconSize + 9;
        int horizontalInset = Math.min(40, Math.max(12, content.width() / 6));
        graphics.fill(content.x() + horizontalInset, dividerY, content.right() - horizontalInset,
                dividerY + 1, MenuUi.accent(classId));
        MenuUi.drawCentered(graphics, font,
                Component.translatable("screen.anomalousclasses.class_management.unavailable"),
                content.x() + content.width() / 2, dividerY + 9, content.width() - 12, 0xFFD8D0C5);

        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    private void refreshButtons() {
        if (backButton != null) backButton.active = !pending;
        if (changeButton != null) changeButton.active = !pending;
    }

    @Override public boolean isPauseScreen() { return false; }
}

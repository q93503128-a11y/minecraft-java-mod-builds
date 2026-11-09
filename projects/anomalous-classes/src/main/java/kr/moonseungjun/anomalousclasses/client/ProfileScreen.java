package kr.moonseungjun.anomalousclasses.client;

import java.util.Locale;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestSkillTreePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ProfileScreen extends Screen {
    private ProfileStatePayload state;
    private ClassId classId;
    private MenuUi.Box outer, artwork, information, stats;
    private MenuUi.Button skillButton, managementButton;
    private boolean pending;

    public ProfileScreen(ProfileStatePayload state) {
        super(Component.translatable("screen.anomalousclasses.profile.title"));
        this.state = state;
        this.classId = ClassId.fromNetworkIndex(state.classIndex()).orElse(ClassId.MENTAL_DOMINATOR);
    }

    @Override
    protected void init() {
        classId = ClassId.fromNetworkIndex(state.classIndex()).orElse(ClassId.MENTAL_DOMINATOR);
        int margin = width < 360 ? 5 : 10;
        int top = height < 250 ? 4 : 10;
        outer = new MenuUi.Box(margin, top, Math.max(1, width - margin * 2), Math.max(1, height - top * 2));
        boolean compact = width < 540 || height < 310;
        int header = height < 250 ? 17 : 23;
        int bodyY = outer.y() + header + 5;
        int navHeight = Math.max(19, Math.min(23, height / 15));
        int navY = outer.bottom() - navHeight - 5;
        int bodyBottom = navY - 5;
        int bodyHeight = Math.max(1, bodyBottom - bodyY);
        int gap = 7;
        if (compact) {
            int leftWidth = Math.min((outer.width() - gap) / 2, Math.max(90, Math.round(outer.width() * 0.44f)));
            artwork = new MenuUi.Box(outer.x() + 7, bodyY + Math.max(0, (bodyHeight - Math.min(bodyHeight, leftWidth * 9 / 16)) / 2),
                    Math.max(1, leftWidth), Math.max(1, Math.min(bodyHeight, leftWidth * 9 / 16)));
            information = new MenuUi.Box(artwork.right() + gap, bodyY, Math.max(1, outer.right() - artwork.right() - gap - 7), bodyHeight);
        } else {
            int leftWidth = Math.round((outer.width() - 21) * 0.44f);
            artwork = new MenuUi.Box(outer.x() + 8, bodyY, Math.max(1, leftWidth), bodyHeight);
            information = new MenuUi.Box(artwork.right() + gap, bodyY, Math.max(1, outer.right() - artwork.right() - 9), bodyHeight);
        }
        stats = new MenuUi.Box(information.x(), information.y() + (compact ? 68 : 112), information.width(),
                Math.max(1, information.bottom() - (information.y() + (compact ? 68 : 112)) - 2));

        int buttonGap = 6;
        int buttonWidth = Math.max(1, Math.min(156, (outer.width() - 20 - buttonGap) / 2));
        int buttonX = outer.x() + (outer.width() - (buttonWidth * 2 + buttonGap)) / 2;
        skillButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(buttonX, navY, buttonWidth, navHeight),
                Component.translatable("screen.anomalousclasses.profile.skill_tree"), button -> openSkillTree()));
        managementButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(buttonX + buttonWidth + buttonGap, navY,
                buttonWidth, navHeight), Component.translatable("screen.anomalousclasses.profile.class_management"),
                button -> minecraft.setScreen(new ClassManagementScreen(state))));
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, outer);
        MenuUi.drawTitle(graphics, font, title, outer, width, height);

        MenuUi.drawArtCover(graphics, classId, artwork);
        MenuUi.drawInsetSurface(graphics, information);
        MenuUi.drawInsetSurface(graphics, stats);
        MenuUi.drawClassIcon(graphics, classId, information.x(), information.y() + 2, Math.min(26, font.lineHeight + 8));
        int nameX = information.x() + Math.min(26, font.lineHeight + 8) + 5;
        MenuUi.drawFit(graphics, font, Component.translatable("class.anomalousclasses." + classId.key()), nameX,
                information.y() + 6, Math.max(1, information.right() - nameX), 0xFFF4E8D7);
        int compact = width < 540 || height < 310 ? 1 : 0;
        int line = compact == 1 ? Math.max(10, font.lineHeight + 1) : font.lineHeight + 4;
        int rowY = information.y() + (compact == 1 ? 38 : 44);
        drawPair(graphics, "screen.anomalousclasses.profile.level", Integer.toString(state.level()), rowY, information.width());
        drawPair(graphics, "screen.anomalousclasses.profile.experience", Long.toString(state.experience()), rowY + line, information.width());
        drawPair(graphics, "screen.anomalousclasses.profile.skill_points", Integer.toString(state.skillPoints()), rowY + line * 2, information.width());

        int statsTitleY = stats.y() + 2;
        MenuUi.drawCentered(graphics, font, Component.translatable("screen.anomalousclasses.profile.attributes"),
                stats.x() + stats.width() / 2, statsTitleY, stats.width() - 8, 0xFFE6C788);
        int statY = statsTitleY + line;
        int statLine = Math.max(9, font.lineHeight + (compact == 1 ? 0 : 2));
        drawPair(graphics, "screen.anomalousclasses.profile.max_health", format(state.maxHealth()), statY, stats.width());
        drawPair(graphics, "screen.anomalousclasses.profile.attack_damage", format(state.attackDamage()), statY + statLine, stats.width());
        drawPair(graphics, "screen.anomalousclasses.profile.movement_speed", format(state.movementSpeed()), statY + statLine * 2, stats.width());
        drawPair(graphics, "screen.anomalousclasses.profile.armor", format(state.armor()), statY + statLine * 3, stats.width());

        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    public void applyServerState(ProfileStatePayload state) {
        this.state = state;
        this.classId = ClassId.fromNetworkIndex(state.classIndex()).orElse(this.classId);
        pending = false;
        refreshButtons();
    }

    @Override public boolean isPauseScreen() { return false; }

    private void drawPair(GuiGraphics graphics, String key, String value, int y, int availableWidth) {
        Component label = Component.translatable(key);
        int valueWidth = Math.min(Math.max(1, availableWidth / 2), Math.max(1, font.width(value)));
        int labelWidth = Math.max(1, availableWidth - valueWidth - 8);
        MenuUi.drawFit(graphics, font, label, information.x() + 3, y, labelWidth, 0xFFD8D0C5);
        MenuUi.drawFit(graphics, font, Component.literal(value), information.right() - valueWidth, y,
                Math.max(1, valueWidth), 0xFFF1E7D7);
    }

    private static String format(double value) { return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", ""); }

    private void openSkillTree() {
        if (pending) return;
        pending = true;
        refreshButtons();
        PacketDistributor.sendToServer(RequestSkillTreePayload.INSTANCE);
    }

    private void refreshButtons() {
        if (skillButton != null) skillButton.active = !pending;
        if (managementButton != null) managementButton.active = !pending;
    }
}

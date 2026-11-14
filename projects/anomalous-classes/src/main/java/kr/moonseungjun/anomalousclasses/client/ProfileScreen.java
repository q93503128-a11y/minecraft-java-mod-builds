package kr.moonseungjun.anomalousclasses.client;

import java.util.Locale;
import java.util.List;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassManagementPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestSkillTreePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ProfileScreen extends Screen {
    private record MetricRow(Component label, Component value) {}

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
        int frameInset = Math.min(30, Math.max(12, Math.min(outer.width(), outer.height()) / 10));
        int header = Math.max(height < 250 ? 17 : 23, frameInset + 7);
        int bodyY = outer.y() + header + 5;
        int navHeight = Math.max(19, Math.min(23, height / 15));
        int navY = outer.bottom() - navHeight - Math.max(7, frameInset / 2);
        int bodyBottom = navY - 7;
        int bodyHeight = Math.max(1, bodyBottom - bodyY);
        int gap = 7;
        int contentX = outer.x() + frameInset;
        int contentRight = outer.right() - frameInset;
        int innerWidth = Math.max(1, contentRight - contentX);
        if (compact) {
            int leftWidth = Math.min((innerWidth - gap) / 2, Math.max(90, Math.round(innerWidth * 0.44f)));
            artwork = new MenuUi.Box(contentX, bodyY + Math.max(0, (bodyHeight - Math.min(bodyHeight, leftWidth * 9 / 16)) / 2),
                    Math.max(1, leftWidth), Math.max(1, Math.min(bodyHeight, leftWidth * 9 / 16)));
            information = new MenuUi.Box(artwork.right() + gap, bodyY,
                    Math.max(1, contentRight - artwork.right() - gap), bodyHeight);
        } else {
            int leftWidth = Math.round((innerWidth - gap) * 0.44f);
            artwork = new MenuUi.Box(contentX, bodyY, Math.max(1, leftWidth), bodyHeight);
            information = new MenuUi.Box(artwork.right() + gap, bodyY,
                    Math.max(1, contentRight - artwork.right() - gap), bodyHeight);
        }
        stats = new MenuUi.Box(information.x(), information.y() + (compact ? 68 : 112), information.width(),
                Math.max(1, information.bottom() - (information.y() + (compact ? 68 : 112)) - 2));

        int buttonGap = 6;
        int buttonWidth = Math.max(1, Math.min(156, (innerWidth - buttonGap) / 2));
        int buttonX = outer.x() + (outer.width() - (buttonWidth * 2 + buttonGap)) / 2;
        skillButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(buttonX, navY, buttonWidth, navHeight),
                shortcutLabel("screen.anomalousclasses.profile.skill_tree", ClassSelectionKeyMappings.OPEN_SKILL_TREE),
                button -> openSkillTree()));
        managementButton = addRenderableWidget(new MenuUi.Button(new MenuUi.Box(buttonX + buttonWidth + buttonGap, navY,
                buttonWidth, navHeight), shortcutLabel("screen.anomalousclasses.profile.class_management",
                ClassSelectionKeyMappings.OPEN_CLASS_MANAGEMENT),
                button -> openManagement()));
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, outer);
        MenuUi.drawTitle(graphics, font, title, outer, width, height);

        graphics.enableScissor(artwork.x(), artwork.y(), artwork.right(), artwork.bottom());
        MenuUi.drawArtCover(graphics, classId, artwork);
        graphics.disableScissor();
        MenuUi.drawInsetSurface(graphics, information);
        MenuUi.drawInsetSurface(graphics, stats);
        int classIconSize = Math.min(26, font.lineHeight + 8);
        int classIconY = information.y() + 2;
        MenuUi.drawClassIcon(graphics, classId, information.x(), classIconY, classIconSize);
        int nameX = information.x() + classIconSize + 5;
        MenuUi.drawFit(graphics, font, Component.translatable("class.anomalousclasses." + classId.key()), nameX,
                classIconY + Math.max(0, (classIconSize - font.lineHeight) / 2),
                Math.max(1, information.right() - nameX), 0xFFF4E8D7);
        int compact = width < 540 || height < 310 ? 1 : 0;
        int line = compact == 1 ? Math.max(10, font.lineHeight + 1) : font.lineHeight + 4;
        int rowY = information.y() + (compact == 1 ? 38 : 44);
        drawMetricRows(graphics, information, List.of(
                metric("screen.anomalousclasses.profile.level", Integer.toString(state.level())),
                metric("screen.anomalousclasses.profile.experience", Long.toString(state.experience())),
                metric("screen.anomalousclasses.profile.skill_points", Integer.toString(state.skillPoints()))
        ), rowY, line);

        int statsTitleY = stats.y() + 2;
        MenuUi.drawCentered(graphics, font, Component.translatable("screen.anomalousclasses.profile.attributes"),
                stats.x() + stats.width() / 2, statsTitleY, stats.width() - 8, 0xFFE6C788);
        int statY = statsTitleY + line;
        int statLine = Math.max(9, font.lineHeight + (compact == 1 ? 0 : 2));
        drawMetricRows(graphics, stats, List.of(
                metric("screen.anomalousclasses.profile.max_health", format(state.maxHealth())),
                metric("screen.anomalousclasses.profile.attack_damage", format(state.attackDamage())),
                metric("screen.anomalousclasses.profile.movement_speed", format(state.movementSpeed())),
                metric("screen.anomalousclasses.profile.armor", format(state.armor()))
        ), statY, statLine);

        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    public void applyServerState(ProfileStatePayload state) {
        this.state = state;
        this.classId = ClassId.fromNetworkIndex(state.classIndex()).orElse(this.classId);
        pending = false;
        refreshButtons();
    }

    @Override public boolean isPauseScreen() { return false; }

    private static Component shortcutLabel(String labelKey, net.minecraft.client.KeyMapping shortcut) {
        return Component.translatable("screen.anomalousclasses.profile.shortcut",
                Component.translatable(labelKey), shortcut.getTranslatedKeyMessage());
    }

    private MetricRow metric(String key, String value) {
        return new MetricRow(Component.translatable(key), Component.literal(value));
    }

    private void drawMetricRows(GuiGraphics graphics, MenuUi.Box bounds, List<MetricRow> rows, int y, int rowHeight) {
        int inset = Math.min(5, Math.max(2, bounds.width() / 24));
        int availableWidth = Math.max(1, bounds.width() - inset * 2);
        int labelWidth = rows.stream().mapToInt(row -> font.width(row.label())).max().orElse(1);
        int valueWidth = rows.stream().mapToInt(row -> font.width(row.value())).max().orElse(1);
        int gap = Math.min(12, Math.max(5, bounds.width() / 36));
        if (labelWidth + gap + valueWidth > availableWidth) {
            gap = Math.min(gap, Math.max(2, availableWidth / 16));
            valueWidth = Math.min(valueWidth, Math.max(1, availableWidth / 3));
            labelWidth = Math.max(1, availableWidth - gap - valueWidth);
        }
        int groupWidth = Math.min(availableWidth, labelWidth + gap + valueWidth);
        int groupX = bounds.x() + (bounds.width() - groupWidth) / 2;
        int valueRight = groupX + labelWidth + gap + valueWidth;
        for (int index = 0; index < rows.size(); index++) {
            MetricRow row = rows.get(index);
            int rowY = y + index * rowHeight;
            MenuUi.drawFit(graphics, font, row.label(), groupX, rowY, labelWidth, 0xFFD8D0C5);
            MenuUi.drawFitRight(graphics, font, row.value(), valueRight, rowY, valueWidth, 0xFFF1E7D7);
        }
    }

    private static String format(double value) { return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", ""); }

    private void openSkillTree() {
        if (pending) return;
        SkillTreeNavigation.begin(SkillTreeNavigation.ReturnTarget.PROFILE);
        pending = true;
        refreshButtons();
        PacketDistributor.sendToServer(RequestSkillTreePayload.INSTANCE);
    }

    @Override
    public void removed() {
        SkillTreeNavigation.cancel(SkillTreeNavigation.ReturnTarget.PROFILE);
        super.removed();
    }

    private void openManagement() {
        if (pending) return;
        ClassManagementNavigation.enterFromProfile();
        pending = true;
        refreshButtons();
        PacketDistributor.sendToServer(RequestClassManagementPayload.INSTANCE);
    }

    private void refreshButtons() {
        if (skillButton != null) skillButton.active = !pending;
        if (managementButton != null) managementButton.active = !pending;
    }
}

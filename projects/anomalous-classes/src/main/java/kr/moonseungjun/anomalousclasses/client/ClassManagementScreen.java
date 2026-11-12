package kr.moonseungjun.anomalousclasses.client;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassMenuPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestSkillTreePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** A server-backed overview and navigation hub for the currently selected class. */
public final class ClassManagementScreen extends Screen {
    private record Metric(Component label, Component value) {}

    private final ProfileStatePayload profile;
    private final ClassId classId;
    private ClassManagementLayout layout;
    private MenuUi.Button profileButton;
    private MenuUi.Button skillTreeButton;
    private MenuUi.Button changeButton;
    private boolean pending;

    public ClassManagementScreen(ProfileStatePayload profile) {
        super(Component.translatable("screen.anomalousclasses.class_management.title"));
        this.profile = profile;
        this.classId = ClassId.fromNetworkIndex(profile.classIndex()).orElse(ClassId.MENTAL_DOMINATOR);
    }

    @Override
    protected void init() {
        layout = ClassManagementLayout.calculate(width, height);
        profileButton = addRenderableWidget(new MenuUi.Button(layout.profileButton(),
                shortcutLabel("screen.anomalousclasses.class_management.profile", ClassSelectionKeyMappings.OPEN_PROFILE),
                button -> openProfile()));
        skillTreeButton = addRenderableWidget(new MenuUi.Button(layout.skillTreeButton(),
                shortcutLabel("screen.anomalousclasses.class_management.skill_tree", ClassSelectionKeyMappings.OPEN_SKILL_TREE),
                button -> openSkillTree()));
        changeButton = addRenderableWidget(new MenuUi.Button(layout.changeButton(),
                Component.translatable("screen.anomalousclasses.class_management.change_class"),
                button -> openClassSelection()));
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, layout.outer());
        MenuUi.drawTitle(graphics, font, title, layout.outer(), width, height);

        drawArtwork(graphics);
        drawOverview(graphics);

        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawArtwork(GuiGraphics graphics) {
        MenuUi.Box artwork = layout.artwork();
        MenuUi.drawInsetSurface(graphics, artwork);
        int inset = Math.min(3, Math.max(1, Math.min(artwork.width(), artwork.height()) / 24));
        int left = artwork.x() + inset;
        int top = artwork.y() + inset;
        int right = Math.max(left + 1, artwork.right() - inset);
        int bottom = Math.max(top + 1, artwork.bottom() - inset);
        graphics.enableScissor(left, top, right, bottom);
        MenuUi.drawArtCover(graphics, classId, new MenuUi.Box(left, top, right - left, bottom - top));
        graphics.disableScissor();
    }

    private void drawOverview(GuiGraphics graphics) {
        MenuUi.Box panel = layout.overview();
        MenuUi.drawInsetSurface(graphics, panel);

        int pad = Math.min(12, Math.max(7, panel.width() / 22));
        int contentX = panel.x() + pad;
        int contentRight = panel.right() - pad;
        int contentWidth = Math.max(1, contentRight - contentX);
        int iconSize = Math.min(28, Math.max(20, font.lineHeight + 16));
        int iconY = panel.y() + 8;
        MenuUi.drawClassIcon(graphics, classId, contentX, iconY, iconSize);

        int nameX = contentX + iconSize + Math.min(8, Math.max(4, panel.width() / 35));
        int nameWidth = Math.max(1, contentRight - nameX);
        int nameY = iconY + Math.max(0, (iconSize - font.lineHeight) / 2);
        MenuUi.drawFit(graphics, font, Component.translatable("class.anomalousclasses." + classId.key()),
                nameX, nameY, nameWidth, 0xFFF2E7D8);

        int sectionY = iconY + iconSize + 5;
        MenuUi.drawFit(graphics, font,
                Component.translatable("screen.anomalousclasses.class_management.growth"),
                contentX, sectionY, contentWidth, 0xFFE6C788);

        List<Metric> metrics = List.of(
                metric("screen.anomalousclasses.profile.level", Integer.toString(profile.level())),
                metric("screen.anomalousclasses.profile.experience", grouped(profile.experience())),
                metric("screen.anomalousclasses.profile.skill_points", Integer.toString(profile.skillPoints())));
        int rowY = sectionY + font.lineHeight + 3;
        int rowGap = 2;
        for (Metric metric : metrics) {
            int rowHeight = drawMetric(graphics, metric, contentX, rowY, contentWidth);
            rowY += rowHeight + rowGap;
        }

        int dividerY = rowY + 1;
        graphics.fill(contentX, dividerY, contentRight, dividerY + 1, MenuUi.accent(classId));
        int attributesY = dividerY + 6;
        MenuUi.drawFit(graphics, font,
                Component.translatable("screen.anomalousclasses.profile.attributes"),
                contentX, attributesY, contentWidth, 0xFFE6C788);
        List<Metric> attributes = List.of(
                metric("screen.anomalousclasses.profile.max_health", format(profile.maxHealth())),
                metric("screen.anomalousclasses.profile.attack_damage", format(profile.attackDamage())),
                metric("screen.anomalousclasses.profile.movement_speed", format(profile.movementSpeed())),
                metric("screen.anomalousclasses.profile.armor", format(profile.armor())));
        int attributeY = attributesY + font.lineHeight + 2;
        for (Metric metric : attributes) {
            int rowHeight = drawMetric(graphics, metric, contentX, attributeY, contentWidth);
            attributeY += rowHeight + 1;
        }
    }

    private int drawMetric(GuiGraphics graphics, Metric metric, int x, int y, int availableWidth) {
        int labelWidth = font.width(metric.label());
        int valueWidth = font.width(metric.value());
        int gap = Math.min(8, Math.max(4, availableWidth / 28));
        int lineHeight = font.lineHeight;
        if (labelWidth + gap + valueWidth <= availableWidth) {
            int groupWidth = labelWidth + gap + valueWidth;
            int groupX = x + (availableWidth - groupWidth) / 2;
            MenuUi.drawFit(graphics, font, metric.label(), groupX, y, labelWidth, 0xFFD8D0C5);
            MenuUi.drawFitRight(graphics, font, metric.value(), groupX + groupWidth, y,
                    valueWidth, 0xFFF1E7D7);
            return lineHeight;
        }

        MenuUi.drawFit(graphics, font, metric.label(), x, y, availableWidth, 0xFFD8D0C5);
        MenuUi.drawFitRight(graphics, font, metric.value(), x + availableWidth, y + lineHeight,
                availableWidth, 0xFFF1E7D7);
        return lineHeight * 2;
    }

    private Metric metric(String key, String value) {
        return new Metric(Component.translatable(key), Component.literal(value));
    }

    private static String grouped(long value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(Math.max(0L, value));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", "");
    }

    private static Component shortcutLabel(String labelKey, net.minecraft.client.KeyMapping shortcut) {
        return Component.translatable("screen.anomalousclasses.profile.shortcut",
                Component.translatable(labelKey), shortcut.getTranslatedKeyMessage());
    }

    private void openProfile() {
        request(RequestProfilePayload.INSTANCE);
    }

    private void openSkillTree() {
        request(RequestSkillTreePayload.INSTANCE);
    }

    private void openClassSelection() {
        request(RequestClassMenuPayload.INSTANCE);
    }

    private void request(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (pending) return;
        pending = true;
        refreshButtons();
        PacketDistributor.sendToServer(payload);
    }

    private void refreshButtons() {
        if (profileButton != null) profileButton.active = !pending;
        if (skillTreeButton != null) skillTreeButton.active = !pending;
        if (changeButton != null) changeButton.active = !pending;
    }

    @Override public boolean isPauseScreen() { return false; }
}

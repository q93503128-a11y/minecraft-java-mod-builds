package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.SubmitOriginPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Fixed introductory record for the first complete Erden kingdom slice. */
public final class ResponsiveOriginSelectionScreen extends Screen {
    private final int schemaVersion;
    private final OriginChoiceState choice = new OriginChoiceState();
    private boolean submitting;
    private String status = "첫 완성 지역인 에르덴 왕국의 시민으로 시작합니다.";
    private Button confirmButton;

    public ResponsiveOriginSelectionScreen(int schemaVersion) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("livingkingdoms.origin.title"));
        this.schemaVersion = schemaVersion;
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        confirmButton = addRenderableWidget(Button.builder(Component.empty(), ignored -> submit())
                .bounds(layout.cardX(), layout.confirmY(), layout.cardW(), layout.confirmH()).build());
        confirmButton.setAlpha(0.0F);
        confirmButton.active = !submitting;
    }

    private void submit() {
        if (submitting || schemaVersion != 2) return;
        submitting = true;
        status = "왕국 시민 기록을 작성하고 있습니다...";
        if (confirmButton != null) confirmButton.active = false;
        ClientPacketDistributor.sendToServer(new SubmitOriginPayload(
                choice.speciesId(), choice.homelandId(), choice.backgroundId(), choice.residenceId()
        ));
    }

    public void handleServerResult(boolean accepted, String message) {
        status = message;
        if (!accepted) {
            submitting = false;
            if (confirmButton != null) confirmButton.active = true;
        }
    }

    boolean allRequiredControlsFit() {
        Layout layout = layout();
        return layout.top() >= 0
                && layout.cardX() >= 0
                && layout.cardX() + layout.cardW() <= width
                && layout.residenceY() + layout.cardH() < layout.confirmY()
                && layout.confirmY() + layout.confirmH() <= height
                && layout.hintY() < layout.statusY()
                && layout.statusY() < height;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        ExternalRpgUi.dimWorld(graphics, width, height);
        ExternalRpgUi.window(graphics, layout.left(), layout.top(), layout.panelW(), layout.panelH());
        ExternalRpgUi.title(graphics, font, "LIVING KINGDOMS", "에르덴 왕국 시민 기록",
                layout.left() + 28, layout.top() + 20);
        ExternalRpgUi.divider(graphics, layout.left() + 24, layout.top() + 53, layout.panelW() - 48);

        fixedRow(graphics, layout, layout.speciesY(), Items.PLAYER_HEAD, "종족", choice.speciesName());
        fixedRow(graphics, layout, layout.homelandY(), Items.GOLDEN_HELMET, "소속", choice.homelandName());
        fixedRow(graphics, layout, layout.backgroundY(), Items.EMERALD, "신분", choice.backgroundName());
        fixedRow(graphics, layout, layout.residenceY(), Items.CHEST,
                "첫 거주지", choice.residenceName(layout.compact()));

        ExternalRpgUi.button(graphics, font, layout.cardX(), layout.confirmY(),
                layout.cardW(), layout.confirmH(),
                submitting ? "시민 기록 작성 중" : "에르덴에서 삶을 시작하기",
                true, inside(mouseX, mouseY, layout.cardX(), layout.confirmY(), layout.cardW(), layout.confirmH()),
                !submitting);

        graphics.centeredText(font, Component.literal("다른 종족과 왕국은 해당 지역이 완성된 뒤 개방됩니다."),
                width / 2, layout.hintY(), 0xFF5A4636);
        graphics.centeredText(font, Component.literal(status), width / 2, layout.statusY(),
                submitting ? 0xFF775339 : 0xFF3F342A);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void fixedRow(GuiGraphicsExtractor graphics, Layout layout, int y, Item icon,
                          String category, String value) {
        ExternalRpgUi.button(graphics, font, layout.cardX(), y, layout.cardW(), layout.cardH(),
                "", false, false, false);
        ExternalRpgUi.itemIcon(graphics, icon, layout.cardX() + 12,
                y + Math.max(4, (layout.cardH() - 16) / 2));
        int textY = y + Math.max(6, (layout.cardH() - 9) / 2);
        graphics.text(font, Component.literal(category), layout.cardX() + 39, textY,
                0xFF806143, false);
        graphics.text(font, Component.literal(value), layout.cardX() + 126, textY,
                0xFF352A21, false);
        graphics.text(font, Component.literal("확정"), layout.cardX() + layout.cardW() - 39,
                textY, 0xFF6B5038, false);
    }

    private Layout layout() {
        boolean compact = height < 325 || width < 560;
        int panelWidth = Math.min(520, Math.max(300, width - 24));
        int panelHeight = Math.min(compact ? 258 : 322, Math.max(218, height - 16));
        int left = (width - panelWidth) / 2;
        int top = Math.max(5, (height - panelHeight) / 2);
        int side = compact ? 24 : 38;
        int cardX = left + side;
        int cardWidth = panelWidth - side * 2;

        if (compact) {
            int cardHeight = 24;
            int gap = 3;
            int speciesY = top + 56;
            int homelandY = speciesY + cardHeight + gap;
            int backgroundY = homelandY + cardHeight + gap;
            int residenceY = backgroundY + cardHeight + gap;
            int confirmHeight = 28;
            int confirmY = residenceY + cardHeight + 5;
            int hintY = confirmY + confirmHeight + 3;
            int statusY = top + panelHeight - 17;
            return new Layout(true, panelWidth, panelHeight, left, top, cardX, cardWidth,
                    cardHeight, confirmHeight, speciesY, homelandY, backgroundY, residenceY,
                    confirmY, hintY, statusY);
        }

        int cardHeight = 34;
        int gap = 8;
        int speciesY = top + 76;
        int homelandY = speciesY + cardHeight + gap;
        int backgroundY = homelandY + cardHeight + gap;
        int residenceY = backgroundY + cardHeight + gap;
        int confirmHeight = 36;
        int confirmY = top + panelHeight - 76;
        return new Layout(false, panelWidth, panelHeight, left, top, cardX, cardWidth,
                cardHeight, confirmHeight, speciesY, homelandY, backgroundY, residenceY,
                confirmY, residenceY + cardHeight + 7, top + panelHeight - 22);
    }

    private static boolean inside(int x, int y, int boxX, int boxY, int boxWidth, int boxHeight) {
        return x >= boxX && y >= boxY && x < boxX + boxWidth && y < boxY + boxHeight;
    }

    private record Layout(boolean compact, int panelW, int panelH, int left, int top,
                          int cardX, int cardW, int cardH, int confirmH,
                          int speciesY, int homelandY, int backgroundY, int residenceY,
                          int confirmY, int hintY, int statusY) {
    }
}

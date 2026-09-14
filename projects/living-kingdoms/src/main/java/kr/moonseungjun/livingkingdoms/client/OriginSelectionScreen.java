package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.network.SubmitOriginPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Fixed Erden citizen registration while Erden is the only playable kingdom. */
public final class OriginSelectionScreen extends Screen {
    private final int schemaVersion;
    private boolean submitting;
    private String statusMessage = "현재 플레이 가능한 출신은 에르덴 왕국 시민 한 가지입니다.";
    private Button confirmButton;

    public OriginSelectionScreen(int schemaVersion) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("livingkingdoms.origin.title"));
        this.schemaVersion = schemaVersion;
        PlayableOriginCatalog.residences();
    }

    @Override
    protected void init() {
        super.init();
        int panelWidth = Math.min(460, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int buttonLeft = left + 38;
        int buttonWidth = panelWidth - 76;
        int top = Math.max(22, (this.height - 330) / 2);

        this.confirmButton = addRenderableWidget(Button.builder(
                Component.empty(), ignored -> submit()
        ).bounds(buttonLeft, top + 270, buttonWidth, 32).build());
        this.confirmButton.setAlpha(0.0F);
        refreshButtonState();
    }

    private void refreshButtonState() {
        if (confirmButton != null) confirmButton.active = !submitting && schemaVersion == 1;
    }

    private void submit() {
        if (submitting || schemaVersion != 1) return;
        submitting = true;
        statusMessage = "왕국 기록부에 시민 정보를 등록하고 있습니다...";
        refreshButtonState();
        ClientPacketDistributor.sendToServer(new SubmitOriginPayload(
                PlayableOriginCatalog.DEFAULT_SPECIES,
                PlayableOriginCatalog.DEFAULT_HOMELAND,
                PlayableOriginCatalog.DEFAULT_BACKGROUND,
                PlayableOriginCatalog.DEFAULT_RESIDENCE
        ));
    }

    public void handleServerResult(boolean accepted, String message) {
        this.statusMessage = message;
        if (accepted) {
            Minecraft.getInstance().gui.setScreen(null);
        } else {
            submitting = false;
            refreshButtonState();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        // Citizen registration cannot be bypassed by closing the screen.
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(460, this.width - 32);
        int panelHeight = 330;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(22, (this.height - panelHeight) / 2);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int cardLeft = left + 38;
        int cardWidth = panelWidth - 76;

        graphics.fill(0, 0, this.width, this.height, 0xD00A0C12);
        graphics.fill(left - 7, top - 7, right + 7, bottom + 7, 0xFF18100D);
        graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xFF80542D);
        graphics.fill(left, top, right, bottom, 0xFFF0DFC0);
        graphics.fill(left, top, left + 7, bottom, 0xFF70431F);
        graphics.fill(right - 7, top, right, bottom, 0xFF70431F);
        graphics.fill(left + 18, top + 48, right - 18, top + 50, 0xFFB48A50);
        graphics.fill(left + 18, bottom - 15, right - 18, bottom - 13, 0xFFD1AE6B);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        drawCard(graphics, cardLeft, top + 72, cardWidth, 28, "종족  ·  인간");
        drawCard(graphics, cardLeft, top + 122, cardWidth, 28, "국가  ·  에르덴 왕국");
        drawCard(graphics, cardLeft, top + 172, cardWidth, 28, "신분  ·  평범한 주민");
        drawCard(graphics, cardLeft, top + 222, cardWidth, 28, "거주지  ·  왕도 시민구의 임대방");
        drawConfirm(graphics, cardLeft, top + 270, cardWidth, 32,
                submitting ? "왕국 기록부에 등록 중..." : "에르덴에서 삶을 시작하기",
                isInside(mouseX, mouseY, cardLeft, top + 270, cardWidth, 32), !submitting && schemaVersion == 1);

        graphics.centeredText(this.font, Component.translatable("livingkingdoms.origin.title"),
                this.width / 2, top + 18, 0xFF3A2418);
        graphics.centeredText(this.font, Component.literal("에르덴 왕국 시민등록"),
                this.width / 2, top + 36, 0xFF6B4B35);
        graphics.centeredText(this.font, Component.literal("다른 왕국과 종족은 실제 지역 완성 뒤에만 개방됩니다."),
                this.width / 2, top + 254, 0xFF5A402D);
        graphics.centeredText(this.font, Component.literal(statusMessage),
                this.width / 2, top + 310, submitting ? 0xFF7D5526 : 0xFF4A3528);
    }

    private void drawCard(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String label) {
        graphics.fill(x, y, x + width, y + height, 0xFF59361F);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFC89B52);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xFF744726);
        graphics.fill(x + 8, y + height - 6, x + width - 8, y + height - 4, 0xFFB17E43);
        graphics.centeredText(this.font, Component.literal(label), x + width / 2, y + (height - 8) / 2, 0xFFFFE8B0);
    }

    private void drawConfirm(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                             String label, boolean hovered, boolean enabled) {
        int outer = enabled ? 0xFF2C4937 : 0xFF6C6358;
        int border = enabled ? 0xFFD5B66D : 0xFF958C7E;
        int inner = enabled ? (hovered ? 0xFF476F50 : 0xFF365941) : 0xFF82786B;
        int text = enabled ? 0xFFFFE8B0 : 0xFFD2C9B9;
        graphics.fill(x, y, x + width, y + height, outer);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, border);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, inner);
        graphics.fill(x + 8, y + height - 6, x + width - 8, y + height - 4, 0xFFBFA15A);
        graphics.centeredText(this.font, Component.literal(label), x + width / 2, y + (height - 8) / 2, text);
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}

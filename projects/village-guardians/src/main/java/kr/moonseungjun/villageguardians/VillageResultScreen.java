package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.Locale;

/** Compact result modal using the same dark command-surface language as the current town UI. */
public final class VillageResultScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x68040709;
    private static final int PANEL = 0xF00B1217;
    private static final int PANEL_2 = 0xEC152229;
    private static final int BORDER = 0xB34F6873;
    private static final int TEXT = 0xFFF2F5F5;
    private static final int MUTED = 0xFFA8B4B9;
    private static final int CYAN = 0xFF50D9C1;
    private static final int GOLD = 0xFFF1C25B;
    private static final int RED = 0xFFE36A63;

    private final String heading;
    private final String body;
    private final String returnAction;

    public VillageResultScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        heading = plain(payload.title());
        body = plain(payload.body());
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        returnAction = actions.length == 0 ? "" : actions[0];
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Bounds b = bounds();
        int accent = accent();
        graphics.fill(b.left() - 2, b.top() - 2, b.right() + 2, b.bottom() + 2, BORDER);
        graphics.fill(b.left(), b.top(), b.right(), b.bottom(), PANEL);
        graphics.fill(b.left(), b.top(), b.left() + 4, b.bottom(), accent);
        graphics.text(font, fit(heading, b.width() - 38), b.left() + 16, b.top() + 12, accent, false);
        graphics.fill(b.left() + 15, b.top() + 31, b.right() - 15, b.top() + 32, BORDER);

        int buttonTop = b.bottom() - 31;
        List<FormattedCharSequence> lines = font.split(Component.literal(body), Math.max(80, b.width() - 32));
        int y = b.top() + 42;
        for (FormattedCharSequence line : lines) {
            if (y > buttonTop - 14) break;
            graphics.text(font, line, b.left() + 16, y, y == b.top() + 42 ? TEXT : MUTED, false);
            y += 12;
        }

        Button button = button(b);
        boolean hovered = inside(mouseX, mouseY, button.x(), button.y(), button.w(), button.h());
        graphics.fill(button.x() - 1, button.y() - 1, button.x() + button.w() + 1,
                button.y() + button.h() + 1, hovered ? TEXT : accent);
        graphics.fill(button.x(), button.y(), button.x() + button.w(), button.y() + button.h(),
                hovered ? 0xEF203039 : PANEL_2);
        graphics.centeredText(font, "확인", button.x() + button.w() / 2, button.y() + 6, TEXT);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Button button = button(bounds());
        if (!inside(click.x(), click.y(), button.x(), button.y(), button.w(), button.h())) {
            return super.mouseClicked(click, doubled);
        }
        if (!returnAction.isBlank()) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(returnAction));
        }
        onClose();
        return true;
    }

    private Bounds bounds() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int panelWidth = Math.min(360, Math.max(210, safe.width() - 34));
        int panelHeight = Math.min(164, Math.max(112, safe.height() - 24));
        panelWidth = Math.min(panelWidth, safe.width());
        panelHeight = Math.min(panelHeight, safe.height());
        int left = safe.centerX() - panelWidth / 2;
        int top = safe.centerY() - panelHeight / 2;
        return new Bounds(left, top, left + panelWidth, top + panelHeight);
    }

    private Button button(Bounds b) {
        int w = Math.min(92, Math.max(68, b.width() / 4));
        return new Button(b.right() - w - 14, b.bottom() - 29, w, 20);
    }

    private int accent() {
        String text = (heading + " " + body).toLowerCase(Locale.ROOT);
        if (text.contains("실패") || text.contains("부족") || text.contains("파괴") || text.contains("불가")) return RED;
        if (text.contains("강화") || text.contains("구매") || text.contains("보급")) return GOLD;
        return CYAN;
    }

    private String fit(String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + "…") > maxWidth) end--;
        return normalized.substring(0, end) + "…";
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Button(int x, int y, int w, int h) {}
    private record Bounds(int left, int top, int right, int bottom) {
        int width() { return Math.max(1, right - left); }
    }
}

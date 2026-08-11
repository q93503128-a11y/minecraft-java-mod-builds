package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Compact post-raid report; replaces the oversized generic action screen. */
public final class VillageVictoryScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x6204080A;
    private static final int PANEL = 0xF00C1518;
    private static final int PANEL_2 = 0xE818272C;
    private static final int LINE = 0xB34F6D70;
    private static final int TEXT = 0xFFF2F6F3;
    private static final int MUTED = 0xFFAAB9B3;
    private static final int GREEN = 0xFF64D99A;
    private static final int GOLD = 0xFFF0C35A;

    private final String body;
    private final List<Option> options = new ArrayList<>();

    public VillageVictoryScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        for (int i = 0; i < Math.min(actions.length, labels.length); i++) {
            String[] p = labels[i].split("\\|", 2);
            options.add(new Option(actions[i], plain(p[0])));
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Bounds b = bounds();
        graphics.fill(b.x() - 2, b.y() - 2, b.right() + 2, b.bottom() + 2, LINE);
        graphics.fill(b.x(), b.y(), b.right(), b.bottom(), PANEL);
        graphics.fill(b.x(), b.y(), b.x() + 4, b.bottom(), GREEN);
        graphics.centeredText(font, "◆  방어 성공  ◆", b.centerX(), b.y() + 12, GREEN);
        graphics.fill(b.x() + 18, b.y() + 31, b.right() - 18, b.y() + 32, 0x8067A47F);

        int textLeft = b.x() + 22;
        int textRight = b.right() - 22;
        int y = b.y() + 43;
        List<FormattedCharSequence> lines = font.split(Component.literal(body), Math.max(100, textRight - textLeft));
        int limitY = b.bottom() - (options.isEmpty() ? 18 : 48);
        for (FormattedCharSequence line : lines) {
            if (y > limitY - 10) break;
            graphics.text(font, line, textLeft, y, y == b.y() + 43 ? TEXT : MUTED, false);
            y += 12;
        }

        if (!options.isEmpty()) {
            int gap = 7;
            int total = Math.min(b.width() - 36, options.size() * 118 + Math.max(0, options.size() - 1) * gap);
            int w = Math.max(76, (total - gap * Math.max(0, options.size() - 1)) / options.size());
            int x = b.centerX() - total / 2;
            int by = b.bottom() - 34;
            for (Option option : options) {
                boolean hover = inside(mouseX, mouseY, x, by, w, 21);
                graphics.fill(x - 1, by - 1, x + w + 1, by + 22, hover ? GOLD : GREEN);
                graphics.fill(x, by, x + w, by + 21, hover ? PANEL_2 : PANEL);
                graphics.centeredText(font, option.label(), x + w / 2, by + 6, TEXT);
                x += w + gap;
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (options.isEmpty()) return super.mouseClicked(click, doubled);
        Bounds b = bounds();
        int gap = 7;
        int total = Math.min(b.width() - 36, options.size() * 118 + Math.max(0, options.size() - 1) * gap);
        int w = Math.max(76, (total - gap * Math.max(0, options.size() - 1)) / options.size());
        int x = b.centerX() - total / 2;
        int by = b.bottom() - 34;
        for (Option option : options) {
            if (inside(click.x(), click.y(), x, by, w, 21)) {
                ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(option.action()));
                return true;
            }
            x += w + gap;
        }
        return super.mouseClicked(click, doubled);
    }

    private Bounds bounds() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int w = Math.min(470, Math.max(260, safe.width() - 36));
        int h = Math.min(270, Math.max(170, safe.height() - 26));
        w = Math.min(w, safe.width());
        h = Math.min(h, safe.height());
        return new Bounds(safe.centerX() - w / 2, safe.centerY() - h / 2, w, h);
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Option(String action, String label) {}
    private record Bounds(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        int centerX() { return x + width / 2; }
    }
}

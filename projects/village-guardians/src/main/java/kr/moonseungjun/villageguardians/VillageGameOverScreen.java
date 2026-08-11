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

/** Dedicated failure presentation. Destructive restart actions retain confirmation. */
public final class VillageGameOverScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x880A0506;
    private static final int TEXT = 0xFFF3E9E5;
    private static final int MUTED = 0xFFC5B3AF;
    private static final int RED = 0xFFDF594E;
    private static final int GOLD = 0xFFD6A45B;
    private static final int DARK = 0xE5151011;

    private final String body;
    private final String[] actions;
    private final List<Option> options = new ArrayList<>();

    public VillageGameOverScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
        actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        for (int index = 0; index < Math.min(actions.length, labels.length); index++) {
            String[] p = labels[index].split("\\|", 2);
            options.add(new Option(plain(p[0]), p.length > 1 ? plain(p[1]) : ""));
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
        int band = Math.max(22, height / 8);
        graphics.fill(0, height / 2 - band, width, height / 2 + band, 0x481D0809);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2;
        int top = Math.max(18, height / 10);
        graphics.centeredText(font, "—  방어선 붕괴  —", cx, top, RED);
        graphics.centeredText(font, plain(title.getString()), cx, top + 20, TEXT);
        graphics.fill(Math.max(28, cx - 160), top + 38, Math.min(width - 28, cx + 160), top + 40, RED);

        List<FormattedCharSequence> lines = font.split(Component.literal(body), Math.max(120, Math.min(660, width - 54)));
        int y = top + 55;
        for (int index = 0; index < Math.min(8, lines.size()); index++) {
            int lineWidth = font.width(lines.get(index));
            graphics.text(font, lines.get(index), cx - lineWidth / 2, y, index == 0 ? TEXT : MUTED, false);
            y += 12;
        }

        int optionTop = Math.max(y + 25, height - 112);
        for (int index = 0; index < options.size(); index++) {
            Bounds b = optionBounds(index, options.size(), optionTop);
            boolean hovered = inside(mouseX, mouseY, b);
            drawOption(graphics, b, options.get(index), hovered, index);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawOption(GuiGraphicsExtractor graphics, Bounds b, Option option, boolean hovered, int index) {
        int accent = index == 0 ? GOLD : RED;
        int midY = b.y() + b.height() / 2;
        graphics.fill(b.x() + 12, b.y(), b.x() + b.width() - 12, b.y() + b.height(), hovered ? 0xEE251A19 : DARK);
        for (int i = 0; i < 12; i++) {
            graphics.fill(b.x() + i, midY - i - 1, b.x() + 12, midY + i + 1, hovered ? accent : 0xFF6C4B47);
            graphics.fill(b.x() + b.width() - 12, midY - i - 1,
                    b.x() + b.width() - i, midY + i + 1, hovered ? accent : 0xFF6C4B47);
        }
        graphics.fill(b.x() + 14, b.y() + 3, b.x() + 17, b.y() + b.height() - 3, accent);
        graphics.text(font, fit(option.title(), b.width() - 44), b.x() + 26, b.y() + 9,
                hovered ? accent : TEXT, false);
        graphics.text(font, fit(option.description(), b.width() - 44), b.x() + 26, b.y() + 25, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int top = Math.max(18, height / 10);
        List<FormattedCharSequence> lines = font.split(Component.literal(body), Math.max(120, Math.min(660, width - 54)));
        int y = top + 55 + Math.min(8, lines.size()) * 12;
        int optionTop = Math.max(y + 25, height - 112);
        for (int index = 0; index < options.size(); index++) {
            Bounds b = optionBounds(index, options.size(), optionTop);
            if (!inside(click.x(), click.y(), b)) continue;
            String action = actions[index];
            Option option = options.get(index);
            String description = VillageActionDescriptions.describe(action, option.title());
            if (VillageActionDescriptions.requiresConfirmation(action) && minecraft != null) {
                minecraft.gui.setScreen(new VillageConfirmScreen(this, action, option.title(), description));
            } else {
                ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private Bounds optionBounds(int index, int count, int top) {
        int gap = 10;
        int usable = Math.max(160, width - 36 - gap * Math.max(0, count - 1));
        int w = Math.min(330, usable / Math.max(1, count));
        int total = count * w + Math.max(0, count - 1) * gap;
        int x = (width - total) / 2 + index * (w + gap);
        return new Bounds(x, top, w, 48);
    }

    private String fit(String value, int maxWidth) {
        if (maxWidth <= 0 || font.width(value) <= maxWidth) return maxWidth <= 0 ? "" : value;
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + "…") > maxWidth) end--;
        return value.substring(0, end) + "…";
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static boolean inside(double x, double y, Bounds b) {
        return x >= b.x() && x < b.x() + b.width() && y >= b.y() && y < b.y() + b.height();
    }

    private record Option(String title, String description) {}
    private record Bounds(int x, int y, int width, int height) {}
}

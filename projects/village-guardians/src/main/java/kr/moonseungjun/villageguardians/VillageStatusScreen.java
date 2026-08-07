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

/** Compact read-only page with dark text and no legacy-format colour leakage. */
public final class VillageStatusScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x65000000;
    private static final int PANEL = 0xFFF1E6CF;
    private static final int SURFACE = 0xFFFFFAEE;
    private static final int SURFACE_ALT = 0xFFE9DCC1;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int ACCENT = 0xFF267E73;
    private static final int GOLD = 0xFFB87B20;
    private static final int RED = 0xFFAA4545;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String action;
    private final String actionLabel;
    private Layout lastLayout;

    public VillageStatusScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        action = actions.length == 0 ? "" : actions[0];
        actionLabel = labels.length == 0 ? "획득 유물 보기" : labels[0].split("\\|", 2)[0];
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(760, Math.max(300, width - 20));
        int bodyWidth = panelWidth - 54;
        List<Row> rows = rows(Math.max(120, bodyWidth));
        int actionSpace = action.isBlank() ? 0 : 34;
        int contentHeight = contentHeight(rows);
        int panelHeight = Math.min(height - 12, Math.max(150, 88 + contentHeight + actionSpace));
        panelWidth = Math.min(width - 12, panelWidth);
        Layout layout = new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
        lastLayout = layout;

        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), ACCENT);

        int titleX = layout.left() + 18;
        int closeX = layout.right() - 36;
        graphics.text(font, plain(payload.title()), titleX, layout.top() + 10, TEXT, false);
        graphics.text(font, payload.screenId().equals("wave_intel") ? "다음 웨이브 정찰" : "현재 수호자 정보",
                titleX, layout.top() + 27, MUTED, false);
        boolean hovered = inside(mouseX, mouseY, closeX, layout.top() + 7, 27, 27);
        graphics.fill(closeX, layout.top() + 7, closeX + 27, layout.top() + 34,
                hovered ? 0xFFE2AAAA : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 13, layout.top() + 16, hovered ? RED : TEXT);

        int bodyLeft = layout.left() + 15;
        int bodyTop = layout.top() + 47;
        int bodyRight = layout.right() - 15;
        int bodyBottom = layout.bottom() - (action.isBlank() ? 13 : 46);
        graphics.fill(bodyLeft - 1, bodyTop - 1, bodyRight + 1, bodyBottom + 1, BORDER);
        graphics.fill(bodyLeft, bodyTop, bodyRight, bodyBottom, SURFACE);

        int y = bodyTop + 12;
        int labelWidth = payload.screenId().equals("wave_intel") ? 118 : 86;
        for (Row row : rows) {
            y += row.gap();
            if (row.spacer()) {
                y += 5;
                continue;
            }
            if (!row.label().isBlank() && row.value().isBlank()) {
                graphics.text(font, row.label(), bodyLeft + 14, y, row.color(), false);
                y += 15;
                continue;
            }
            if (!row.label().isBlank()) {
                graphics.text(font, row.label(), bodyLeft + 14, y, row.color(), false);
                int valueX = bodyLeft + 14 + labelWidth;
                List<FormattedCharSequence> wrapped = font.split(Component.literal(row.value()),
                        Math.max(80, bodyRight - valueX - 12));
                for (FormattedCharSequence line : wrapped) {
                    graphics.text(font, line, valueX, y, TEXT, false);
                    y += 14;
                }
                if (wrapped.isEmpty()) y += 14;
            } else {
                List<FormattedCharSequence> wrapped = font.split(Component.literal(row.value()),
                        Math.max(100, bodyRight - bodyLeft - 28));
                for (FormattedCharSequence line : wrapped) {
                    graphics.text(font, line, bodyLeft + 14, y, row.color(), false);
                    y += 14;
                }
                if (wrapped.isEmpty()) y += 14;
            }
        }

        if (!action.isBlank()) {
            int buttonWidth = Math.min(220, Math.max(138, layout.width() / 3));
            int buttonX = layout.left() + (layout.width() - buttonWidth) / 2;
            int buttonY = layout.bottom() - 34;
            boolean buttonHover = inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 22);
            graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY + 23,
                    buttonHover ? ACCENT : BORDER);
            graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 22,
                    buttonHover ? 0xFFD7F1E9 : SURFACE_ALT);
            graphics.centeredText(font, fit(actionLabel, buttonWidth - 12), buttonX + buttonWidth / 2,
                    buttonY + 6, buttonHover ? TEXT : MUTED);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private List<Row> rows(int width) {
        List<Row> result = new ArrayList<>();
        for (String raw : payload.body().split("\n", -1)) {
            if (raw.isBlank()) {
                result.add(new Row("", "", MUTED, 0, true));
                continue;
            }
            String text = plain(raw);
            int divider = text.indexOf("  ");
            int color = raw.startsWith("§6") ? GOLD : raw.startsWith("§b") ? ACCENT
                    : raw.startsWith("§7") ? MUTED : TEXT;
            if (divider > 0) {
                String label = text.substring(0, divider).trim();
                String value = text.substring(divider).trim();
                result.add(new Row(label, value, color, result.isEmpty() ? 0 : 3, false));
            } else if ((raw.startsWith("§b") || raw.startsWith("§6")) && text.length() <= 16) {
                result.add(new Row(text, "", color, result.isEmpty() ? 0 : 5, false));
            } else {
                result.add(new Row("", text, color, result.isEmpty() ? 0 : 2, false));
            }
        }
        while (!result.isEmpty() && result.getLast().spacer()) result.removeLast();
        return result;
    }

    private int contentHeight(List<Row> rows) {
        int bodyWidth = Math.max(120, Math.min(706, width - 74));
        int labelWidth = payload.screenId().equals("wave_intel") ? 118 : 86;
        int total = 24;
        for (Row row : rows) {
            total += row.gap();
            if (row.spacer()) {
                total += 5;
            } else if (!row.label().isBlank() && row.value().isBlank()) {
                total += 15;
            } else {
                int wrapWidth = row.label().isBlank() ? bodyWidth - 28 : bodyWidth - labelWidth - 28;
                int lines = Math.max(1, font.split(Component.literal(row.value()), Math.max(80, wrapWidth)).size());
                total += lines * 14;
            }
        }
        return total;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0 && lastLayout != null) {
            if (inside(click.x(), click.y(), lastLayout.right() - 36, lastLayout.top() + 7, 27, 27)) {
                onClose();
                return true;
            }
            if (!action.isBlank()) {
                int buttonWidth = Math.min(220, Math.max(138, lastLayout.width() / 3));
                int buttonX = lastLayout.left() + (lastLayout.width() - buttonWidth) / 2;
                int buttonY = lastLayout.bottom() - 34;
                if (inside(click.x(), click.y(), buttonX, buttonY, buttonWidth, 22)) {
                    ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private String fit(String value, int maximumWidth) {
        String safe = plain(value);
        if (font.width(safe) <= maximumWidth) return safe;
        int end = safe.length();
        while (end > 0 && font.width(safe.substring(0, end) + "…") > maximumWidth) end--;
        return safe.substring(0, end) + "…";
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Row(String label, String value, int color, int gap, boolean spacer) {}
    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }
}

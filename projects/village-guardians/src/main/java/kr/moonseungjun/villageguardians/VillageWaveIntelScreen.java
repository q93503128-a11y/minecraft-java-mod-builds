package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Tactical wave briefing laid out as an attack timeline instead of a facility menu. */
public final class VillageWaveIntelScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x68070D12;
    private static final int TEXT = 0xFFF1F5F6;
    private static final int MUTED = 0xFFA6B2B8;
    private static final int ACCENT = 0xFFFFB84E;
    private static final int ALERT = 0xFFE7584D;
    private static final int LINE = 0xAA60737E;

    private final String body;
    private final String[] actions;
    private final List<Entry> entries = new ArrayList<>();
    private int selected;

    public VillageWaveIntelScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
        actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        for (int index = 0; index < Math.min(actions.length, labels.length); index++) {
            String[] p = labels[index].split("\\|", 2);
            entries.add(new Entry(plain(p[0]), p.length > 1 ? plain(p[1]) : ""));
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2;
        int top = Math.max(12, height / 15);
        graphics.centeredText(font, "전술 브리핑  //  " + plain(title.getString()), cx, top, ACCENT);
        graphics.fill(Math.max(18, cx - 190), top + 18, Math.min(width - 18, cx + 190), top + 19, 0x88CB8A34);

        int reportTop = top + 31;
        List<FormattedCharSequence> report = font.split(Component.literal(body), Math.max(100, width - 46));
        int reportLines = Math.min(entries.isEmpty() ? 12 : 6, report.size());
        int y = reportTop;
        for (int index = 0; index < reportLines; index++) {
            int lineWidth = font.width(report.get(index));
            graphics.text(font, report.get(index), cx - lineWidth / 2, y,
                    index == 0 ? TEXT : MUTED, false);
            y += 11;
        }

        if (!entries.isEmpty()) {
            int timelineY = Math.max(y + 30, height / 2);
            int left = Math.max(28, width / 10);
            int right = Math.min(width - 28, width - width / 10);
            if (entries.size() == 1) left = right = cx;
            if (right > left) graphics.fill(left, timelineY, right, timelineY + 2, LINE);

            for (int index = 0; index < entries.size(); index++) {
                int x = entries.size() == 1 ? cx
                        : left + Math.round((right - left) * (index / (float) (entries.size() - 1)));
                boolean hovered = insideDiamond(mouseX, mouseY, x, timelineY, 25);
                boolean active = index == selected;
                VillageQuickChatScreen.drawDiamond(graphics, x, timelineY, active || hovered ? 19 : 15,
                        0xE5162026);
                VillageQuickChatScreen.drawDiamondOutline(graphics, x, timelineY, active || hovered ? 19 : 15,
                        active ? ACCENT : hovered ? TEXT : 0xFF778993);
                VillageQuickChatScreen.drawDiamond(graphics, x, timelineY, 5,
                        active ? ACCENT : hovered ? TEXT : 0xFF83949D);
                int labelY = timelineY + (index % 2 == 0 ? -34 : 25);
                graphics.centeredText(font, fit(entries.get(index).title(), 126), x, labelY,
                        active ? ACCENT : TEXT);
            }

            Entry current = entries.get(clamp(selected, 0, entries.size() - 1));
            int detailY = Math.min(height - 55, timelineY + 58);
            graphics.centeredText(font, current.title(), cx, detailY, ACCENT);
            List<FormattedCharSequence> detail = font.split(Component.literal(current.detail()),
                    Math.max(100, Math.min(600, width - 54)));
            int lineY = detailY + 15;
            for (int index = 0; index < Math.min(5, detail.size()); index++) {
                int lineWidth = font.width(detail.get(index));
                graphics.text(font, detail.get(index), cx - lineWidth / 2, lineY, MUTED, false);
                lineY += 11;
            }
        } else {
            graphics.centeredText(font, "현재 추가 정찰 경로 없음", cx, Math.min(height - 42, y + 35), ALERT);
        }
        graphics.text(font, "ESC  브리핑 닫기", 10, height - 16, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0 || entries.isEmpty()) return super.mouseClicked(click, doubled);
        int top = Math.max(12, height / 15);
        List<FormattedCharSequence> report = font.split(Component.literal(body), Math.max(100, width - 46));
        int y = top + 31 + Math.min(6, report.size()) * 11;
        int timelineY = Math.max(y + 30, height / 2);
        int left = Math.max(28, width / 10);
        int right = Math.min(width - 28, width - width / 10);
        if (entries.size() == 1) left = right = width / 2;
        for (int index = 0; index < entries.size(); index++) {
            int x = entries.size() == 1 ? width / 2
                    : left + Math.round((right - left) * (index / (float) (entries.size() - 1)));
            if (insideDiamond(click.x(), click.y(), x, timelineY, 28)) {
                selected = index;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
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

    private static boolean insideDiamond(double x, double y, int cx, int cy, int radius) {
        return Math.abs(x - cx) + Math.abs(y - cy) <= radius;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Entry(String title, String detail) {}
}

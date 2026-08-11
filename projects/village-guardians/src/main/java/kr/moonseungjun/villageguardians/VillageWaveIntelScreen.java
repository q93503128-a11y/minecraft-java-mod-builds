package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Tactical wave briefing laid out as an attack timeline inside the shared safe viewport. */
public final class VillageWaveIntelScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x68070D12;
    private static final int TEXT = 0xFFF1F5F6;
    private static final int MUTED = 0xFFA6B2B8;
    private static final int ACCENT = 0xFFFFB84E;
    private static final int ALERT = 0xFFE7584D;
    private static final int LINE = 0xAA60737E;

    private final String body;
    private final List<Entry> entries = new ArrayList<>();
    private int selected;

    public VillageWaveIntelScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        for (int index = 0; index < Math.min(actions.length, labels.length); index++) {
            String[] p = labels[index].split("\\|", 2);
            entries.add(new Entry(plain(p[0]), p.length > 1 ? plain(p[1]) : ""));
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        VillageUiSafeArea.Rect safe = layout.safe();
        int cx = safe.centerX();
        graphics.centeredText(font, "전술 브리핑  //  " + plain(title.getString()), cx, layout.top(), ACCENT);
        graphics.fill(Math.max(safe.left() + 8, cx - 190), layout.top() + 18,
                Math.min(safe.right() - 8, cx + 190), layout.top() + 19, 0x88CB8A34);

        List<FormattedCharSequence> report = font.split(Component.literal(body), Math.max(100, safe.width() - 28));
        int y = layout.top() + 31;
        int reportLines = Math.min(entries.isEmpty() ? 10 : 5, report.size());
        for (int index = 0; index < reportLines; index++) {
            int lineWidth = font.width(report.get(index));
            graphics.text(font, report.get(index), cx - lineWidth / 2, y, index == 0 ? TEXT : MUTED, false);
            y += 11;
        }

        if (!entries.isEmpty()) {
            if (layout.timelineRight() > layout.timelineLeft()) {
                graphics.fill(layout.timelineLeft(), layout.timelineY(), layout.timelineRight(), layout.timelineY() + 2, LINE);
            }
            for (int index = 0; index < entries.size(); index++) {
                int x = pointX(index, layout);
                boolean hovered = insideDiamond(mouseX, mouseY, x, layout.timelineY(), 25);
                boolean active = index == selected;
                VillageQuickChatSafeScreen.drawDiamond(graphics, x, layout.timelineY(), active || hovered ? 19 : 15,
                        0xE5162026);
                VillageQuickChatSafeScreen.drawDiamondOutline(graphics, x, layout.timelineY(), active || hovered ? 19 : 15,
                        active ? ACCENT : hovered ? TEXT : 0xFF778993);
                VillageQuickChatSafeScreen.drawDiamond(graphics, x, layout.timelineY(), 5,
                        active ? ACCENT : hovered ? TEXT : 0xFF83949D);
                int labelY = layout.timelineY() + (index % 2 == 0 ? -34 : 25);
                graphics.centeredText(font, fit(entries.get(index).title(), 126), x, labelY,
                        active ? ACCENT : TEXT);
            }

            Entry current = entries.get(clamp(selected, 0, entries.size() - 1));
            int detailY = layout.detailY();
            graphics.centeredText(font, current.title(), cx, detailY, ACCENT);
            List<FormattedCharSequence> detail = font.split(Component.literal(current.detail()),
                    Math.max(100, Math.min(600, safe.width() - 28)));
            int lineY = detailY + 15;
            int maxLines = Math.max(1, Math.min(5, (safe.bottom() - 28 - lineY) / 11));
            for (int index = 0; index < Math.min(maxLines, detail.size()); index++) {
                int lineWidth = font.width(detail.get(index));
                graphics.text(font, detail.get(index), cx - lineWidth / 2, lineY, MUTED, false);
                lineY += 11;
            }
        } else {
            graphics.centeredText(font, "현재 추가 정찰 경로 없음", cx,
                    Math.min(safe.bottom() - 32, y + 35), ALERT);
        }
        graphics.text(font, "ESC 브리핑 닫기", safe.left() + 4, safe.bottom() - 11, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0 || entries.isEmpty()) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        for (int index = 0; index < entries.size(); index++) {
            if (insideDiamond(click.x(), click.y(), pointX(index, layout), layout.timelineY(), 28)) {
                selected = index;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int top = safe.top() + 4;
        List<FormattedCharSequence> report = font.split(Component.literal(body), Math.max(100, safe.width() - 28));
        int reportBottom = top + 31 + Math.min(entries.isEmpty() ? 10 : 5, report.size()) * 11;
        int timelineY = clamp(Math.max(reportBottom + 26, safe.top() + safe.height() / 2 - 10),
                safe.top() + 92, safe.bottom() - 93);
        int left = safe.left() + Math.max(20, safe.width() / 12);
        int right = safe.right() - Math.max(20, safe.width() / 12);
        if (entries.size() == 1) left = right = safe.centerX();
        int detailY = Math.min(safe.bottom() - 55, timelineY + 55);
        return new Layout(safe, top, timelineY, left, right, detailY);
    }

    private int pointX(int index, Layout layout) {
        if (entries.size() <= 1) return layout.safe().centerX();
        return layout.timelineLeft() + Math.round((layout.timelineRight() - layout.timelineLeft())
                * (index / (float) (entries.size() - 1)));
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
    private record Layout(VillageUiSafeArea.Rect safe, int top, int timelineY,
                          int timelineLeft, int timelineRight, int detailY) {}
}

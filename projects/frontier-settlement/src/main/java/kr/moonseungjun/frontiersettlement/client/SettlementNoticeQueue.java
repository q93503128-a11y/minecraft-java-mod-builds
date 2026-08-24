package kr.moonseungjun.frontiersettlement.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.Deque;

/** Against-the-Storm-style compact side notices; no modal popup and no new key. */
public final class SettlementNoticeQueue {
    private static final long LIFETIME_MS = 6_000L;
    private static final int MAX_VISIBLE = 3;
    private static final Deque<Notice> NOTICES = new ArrayDeque<>();

    private record Notice(String text, long expiresAt) {}
    private SettlementNoticeQueue() {}

    public static synchronized void push(String text) {
        if (text == null || text.isBlank()) return;
        long now = System.currentTimeMillis();
        NOTICES.removeIf(notice -> notice.expiresAt() <= now || notice.text().equals(text));
        NOTICES.addLast(new Notice(text, now + LIFETIME_MS));
        while (NOTICES.size() > MAX_VISIBLE) NOTICES.removeFirst();
    }

    public static synchronized void clear() {
        NOTICES.clear();
    }

    public static synchronized void render(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        long now = System.currentTimeMillis();
        NOTICES.removeIf(notice -> notice.expiresAt() <= now);
        if (NOTICES.isEmpty()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int y = 48;
        for (Notice notice : NOTICES) {
            int width = minecraft.font.width(notice.text()) + 14;
            int x = Math.max(8, screenWidth - width - 8);
            graphics.fill(x, y, x + width, y + 18, 0xB0000000);
            graphics.text(minecraft.font, notice.text(), x + 7, y + 5, 0xFFFFD58A, true);
            y += 21;
        }
    }
}

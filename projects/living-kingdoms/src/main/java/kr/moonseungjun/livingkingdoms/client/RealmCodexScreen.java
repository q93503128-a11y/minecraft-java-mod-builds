package kr.moonseungjun.livingkingdoms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RealmCodexScreen extends Screen {
    private static final int WORLD_MIN_X = -24_000;
    private static final int WORLD_MAX_X = 24_000;
    private static final int WORLD_MIN_Z = -20_000;
    private static final int WORLD_MAX_Z = 20_000;

    private final Map<String, String> data;
    private String page;

    public RealmCodexScreen(String page, String snapshot) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("왕국 수첩"));
        this.page = "status".equals(page) ? "status" : "map";
        this.data = parse(snapshot);
    }

    @Override
    protected void init() {
        super.init();
        Layout l = layout();
        invisible(l.left() + 18, l.top() + 13, 74, 22, () -> page = "map");
        invisible(l.left() + 96, l.top() + 13, 74, 22, () -> page = "status");
        invisible(l.right() - 44, l.top() + 13, 26, 22, this::onClose);
    }

    private void invisible(int x, int y, int w, int h, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> action.run())
                .bounds(x, y, w, h).build());
        button.setAlpha(0.0F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0xB80A0C12);
        g.fill(l.left() - 6, l.top() - 6, l.right() + 6, l.bottom() + 6, 0xFF17100C);
        g.fill(l.left() - 3, l.top() - 3, l.right() + 3, l.bottom() + 3, 0xFF79502D);
        g.fill(l.left(), l.top(), l.right(), l.bottom(), 0xFFF1DFC0);
        g.fill(l.left(), l.top(), l.left() + 7, l.bottom(), 0xFF68401F);
        g.fill(l.right() - 7, l.top(), l.right(), l.bottom(), 0xFF68401F);
        g.fill(l.left() + 14, l.top() + 42, l.right() - 14, l.top() + 44, 0xFFB68B4E);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
        tab(g, l.left() + 18, l.top() + 13, 74, 22, "에르덴 지도", "map".equals(page));
        tab(g, l.left() + 96, l.top() + 13, 74, 22, "상태 기록", "status".equals(page));
        tab(g, l.right() - 44, l.top() + 13, 26, 22, "×", false);
        g.centeredText(font, Component.literal("Living Kingdoms · 왕국 수첩"), l.centerX(), l.top() + 18, 0xFF3B2718);

        if ("status".equals(page)) drawStatus(g, l);
        else drawMap(g, l);
    }

    private void drawMap(GuiGraphicsExtractor g, Layout l) {
        int x = l.left() + 18;
        int y = l.top() + 52;
        int w = l.panelW() - 36;
        int h = l.panelH() - 76;
        g.fill(x, y, x + w, y + h, 0xFFDDC99F);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFEAD9B4);

        // Current playable world: the complete 48 x 40 km Erden kingdom only.
        irregularRegion(g, x + w / 2, y + (h - 16) / 2,
                Math.max(30, (w - 52) / 2), Math.max(24, (h - 58) / 2), 0xFF9EB36B);

        int capitalX = parseInt("erden_x");
        int capitalZ = parseInt("erden_z");
        landmark(g, mapX(capitalX, x, w), mapZ(capitalZ, y, h), "에르덴 왕도", 0xFF38552F);

        int homeX = parseInt("home_x");
        int homeZ = parseInt("home_z");
        int playerX = parseInt("player_x");
        int playerZ = parseInt("player_z");
        marker(g, mapX(homeX, x, w), mapZ(homeZ, y, h), 0xFF2F5E86, 4);
        marker(g, mapX(playerX, x, w), mapZ(playerZ, y, h), 0xFFD94C32, 5);

        int textY = y + h - 21;
        g.fill(x + 7, textY - 3, x + w - 7, y + h - 6, 0xC0F3E6C7);
        g.text(font, Component.literal("● 현재 위치   ◆ 등록 거주지   에르덴 왕국 48×40 km"), x + 12, textY, 0xFF4A3524);
        g.text(font, Component.literal(shortText(value("region") + " · " + value("position"), 48)), x + 12, textY + 10, 0xFF5E432B);
    }

    private void drawStatus(GuiGraphicsExtractor g, Layout l) {
        int contentTop = l.top() + 52;
        int gap = 8;
        int cardW = (l.panelW() - 36 - gap) / 2;
        int leftX = l.left() + 18;
        int rightX = leftX + cardW + gap;
        int cardH = l.panelH() - 70;

        card(g, leftX, contentTop, cardW, cardH, "인물과 신분");
        card(g, rightX, contentTop, cardW, cardH, "장비와 법적 상태");

        int y = contentTop + 22;
        row(g, leftX + 9, y, cardW - 18, "이름", value("player")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "종족", value("species")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "출신", value("homeland")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "배경", value("background")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "거주지", value("residence")); y += 14;
        separator(g, leftX + 9, y, cardW - 18); y += 6;
        row(g, leftX + 9, y, cardW - 18, "체력", value("health")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "방어", value("armor")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "허기", value("food")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "레벨", value("level")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "총 경험", value("experience")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "현재 지역", value("region")); y += 11;
        row(g, leftX + 9, y, cardW - 18, "좌표", value("position"));

        y = contentTop + 22;
        row(g, rightX + 9, y, cardW - 18, "주무기", value("mainhand")); y += 11;
        row(g, rightX + 9, y, cardW - 18, "보조", value("offhand")); y += 11;
        row(g, rightX + 9, y, cardW - 18, "머리", value("head")); y += 11;
        row(g, rightX + 9, y, cardW - 18, "몸통", value("chest")); y += 11;
        row(g, rightX + 9, y, cardW - 18, "다리", value("legs")); y += 11;
        row(g, rightX + 9, y, cardW - 18, "발", value("feet")); y += 14;
        separator(g, rightX + 9, y, cardW - 18); y += 6;
        row(g, rightX + 9, y, cardW - 18, "수배도", value("wanted") + " / 100"); y += 11;
        row(g, rightX + 9, y, cardW - 18, "저항 단계", value("resistance")); y += 11;
        row(g, rightX + 9, y, cardW - 18, "관할", value("jurisdiction")); y += 11;
        row(g, rightX + 9, y, cardW - 18, "체포 진행", value("arrest")); y += 11;
        row(g, rightX + 9, y, cardW - 18, "세계", value("realm"));
    }

    private void tab(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, boolean active) {
        g.fill(x, y, x + w, y + h, active ? 0xFF4C6A4B : 0xFF654225);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, active ? 0xFFCDBA7A : 0xFFB88A4B);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, active ? 0xFF668260 : 0xFF81552E);
        g.centeredText(font, Component.literal(text), x + w / 2, y + 7, 0xFFFFEDBD);
    }

    private void card(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
        g.fill(x, y, x + w, y + h, 0xFF6E492B);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFE9D5AE);
        g.fill(x + 5, y + 18, x + w - 5, y + 20, 0xFFBE965B);
        g.centeredText(font, Component.literal(title), x + w / 2, y + 6, 0xFF4A3020);
    }

    private void row(GuiGraphicsExtractor g, int x, int y, int width, String label, String value) {
        String compact = shortText(value, Math.max(10, width / 6));
        g.text(font, Component.literal(label), x, y, 0xFF735238);
        int valueX = x + Math.min(58, Math.max(40, width / 3));
        g.text(font, Component.literal(compact), valueX, y, 0xFF30271E);
    }

    private void separator(GuiGraphicsExtractor g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 1, 0xFFC49C62);
    }

    private void irregularRegion(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int color) {
        for (int dy = -ry; dy <= ry; dy += 3) {
            double normalized = 1.0 - (double) (dy * dy) / (double) (ry * ry);
            if (normalized < 0.0) continue;
            int half = Math.max(3, (int) Math.round(rx * Math.sqrt(normalized)));
            int wobble = (int) Math.round(Math.sin((cy + dy) * 0.31) * 3.0);
            g.fill(cx - half + wobble, cy + dy, cx + half + wobble, cy + dy + 3, color);
        }
    }

    private void landmark(GuiGraphicsExtractor g, int x, int y, String text, int color) {
        marker(g, x, y, color, 4);
        g.centeredText(font, Component.literal(text), x, y - 13, 0xFF3C2B20);
    }

    private static void marker(GuiGraphicsExtractor g, int x, int y, int color, int radius) {
        g.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, 0xFFF5E6BE);
        g.fill(x - radius + 2, y - radius + 2, x + radius - 1, y + radius - 1, color);
    }

    private static int mapX(int worldX, int x, int w) {
        double t = (worldX - WORLD_MIN_X) / (double) (WORLD_MAX_X - WORLD_MIN_X);
        return x + 16 + (int) Math.round(Math.max(0.0, Math.min(1.0, t)) * (w - 32));
    }

    private static int mapZ(int worldZ, int y, int h) {
        double t = (worldZ - WORLD_MIN_Z) / (double) (WORLD_MAX_Z - WORLD_MIN_Z);
        return y + 14 + (int) Math.round(Math.max(0.0, Math.min(1.0, t)) * (h - 42));
    }

    private Layout layout() {
        int panelW = Math.min(650, Math.max(390, width - 16));
        int panelH = Math.min(390, Math.max(220, height - 12));
        panelW = Math.min(panelW, width - 8);
        panelH = Math.min(panelH, height - 6);
        int left = (width - panelW) / 2;
        int top = Math.max(3, (height - panelH) / 2);
        return new Layout(left, top, panelW, panelH);
    }

    private String value(String key) {
        return data.getOrDefault(key, "-");
    }

    private int parseInt(String key) {
        try {
            return Integer.parseInt(value(key));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String shortText(String value, int maxChars) {
        if (value == null) return "-";
        if (value.length() <= maxChars) return value;
        return value.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    private static Map<String, String> parse(String snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        if (snapshot == null) return values;
        for (String line : snapshot.split("\\n")) {
            int tab = line.indexOf('\t');
            if (tab <= 0) continue;
            values.put(line.substring(0, tab), line.substring(tab + 1));
        }
        return values;
    }

    private record Layout(int left, int top, int panelW, int panelH) {
        int right() { return left + panelW; }
        int bottom() { return top + panelH; }
        int centerX() { return left + panelW / 2; }
    }
}

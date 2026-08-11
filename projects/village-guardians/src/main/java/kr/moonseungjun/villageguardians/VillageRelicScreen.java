package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Relic collection presented as a reliquary of sigils instead of a scrolling card list. */
public final class VillageRelicScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x56000000;
    private static final int TEXT = 0xFFF7F0DD;
    private static final int MUTED = 0xFFAAA499;
    private static final int GOLD = 0xFFD8A84C;
    private static final int TEAL = 0xFF55CDB8;
    private static final int LOCKED = 0xFF606469;
    private static final int SHADOW = 0xCC12171B;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<Relic> relics = new ArrayList<>();
    private int selected;
    private int scroll;

    public VillageRelicScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        parse();
        for (int index = 0; index < relics.size(); index++) {
            if (relics.get(index).owned()) { selected = index; break; }
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
        int centerX = width / 2;
        int titleY = Math.max(12, height / 14);
        graphics.centeredText(font, plain(payload.title()), centerX, titleY, GOLD);
        graphics.centeredText(font, fit(summaryLine(), Math.max(100, width - 40)), centerX, titleY + 16, MUTED);
        graphics.fill(Math.max(16, centerX - 160), titleY + 30,
                Math.min(width - 16, centerX + 160), titleY + 31, 0x88A77B35);

        Grid grid = grid(titleY + 43);
        int rows = relics.isEmpty() ? 0 : (relics.size() + grid.columns() - 1) / grid.columns();
        int contentHeight = rows * grid.cell();
        int maximum = Math.max(0, contentHeight - grid.height());
        scroll = clamp(scroll, 0, maximum);
        graphics.enableScissor(grid.left() - 4, grid.top(), grid.right() + 4, grid.bottom());
        for (int index = 0; index < relics.size(); index++) {
            int row = index / grid.columns();
            int column = index % grid.columns();
            int cx = grid.left() + column * grid.cell() + grid.cell() / 2;
            int cy = grid.top() + row * grid.cell() + 25 - scroll;
            if (cy < grid.top() - 30 || cy > grid.bottom() + 30) continue;
            Relic relic = relics.get(index);
            boolean hovered = insideDiamond(mouseX, mouseY, cx, cy, 25);
            boolean active = index == selected;
            int outer = relic.owned() ? (active || hovered ? GOLD : TEAL) : LOCKED;
            VillageQuickChatScreen.drawDiamond(graphics, cx, cy, active ? 24 : 21, SHADOW);
            VillageQuickChatScreen.drawDiamondOutline(graphics, cx, cy, active ? 24 : 21, outer);
            VillageQuickChatScreen.drawDiamond(graphics, cx, cy, 13,
                    relic.owned() ? 0xDD20363A : 0xDD2B2D30);
            String rune = relic.owned() && !relic.name().isBlank() ? relic.name().substring(0, 1) : "?";
            graphics.centeredText(font, rune, cx, cy - 4, relic.owned() ? TEXT : MUTED);
            String name = relic.owned() ? relic.name() : "미획득";
            graphics.centeredText(font, fit(name, grid.cell() - 8), cx, cy + 29,
                    relic.owned() ? (active ? GOLD : TEXT) : MUTED);
        }
        graphics.disableScissor();

        if (!relics.isEmpty()) renderSelected(graphics, relics.get(clamp(selected, 0, relics.size() - 1)), grid.bottom() + 8);
        graphics.text(font, "ESC  닫기", 10, height - 16, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSelected(GuiGraphicsExtractor graphics, Relic relic, int top) {
        int centerX = width / 2;
        int available = Math.max(100, width - 44);
        graphics.centeredText(font, relic.owned() ? relic.name() : "미획득 유물", centerX, top,
                relic.owned() ? GOLD : MUTED);
        String description = relic.owned() ? relic.description() : "보스를 처치하고 제시된 유물 중 하나를 선택하면 공개됩니다.";
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(description),
                Math.min(560, available));
        int y = top + 14;
        for (int index = 0; index < Math.min(3, lines.size()); index++) {
            int lineWidth = font.width(lines.get(index));
            graphics.text(font, lines.get(index), centerX - lineWidth / 2, y, relic.owned() ? TEXT : MUTED, false);
            y += 11;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Grid grid = grid(Math.max(12, height / 14) + 43);
        for (int index = 0; index < relics.size(); index++) {
            int row = index / grid.columns();
            int column = index % grid.columns();
            int cx = grid.left() + column * grid.cell() + grid.cell() / 2;
            int cy = grid.top() + row * grid.cell() + 25 - scroll;
            if (insideDiamond(click.x(), click.y(), cx, cy, 27)) {
                selected = index;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) Math.round(vertical * 34));
        return true;
    }

    private Grid grid(int top) {
        int cell = width < 430 ? 62 : 72;
        int columns = clamp((width - 36) / cell, 3, 7);
        int gridWidth = columns * cell;
        int left = (width - gridWidth) / 2;
        int detailReserve = 78;
        int bottom = Math.max(top + 72, height - detailReserve);
        return new Grid(left, top, left + gridWidth, bottom, columns, cell);
    }

    private String summaryLine() {
        String body = plain(payload.body()).replace('\n', ' ');
        int dot = body.indexOf('·');
        return dot > 0 ? body.substring(0, dot).trim() : body;
    }

    private void parse() {
        if (payload.labels().isBlank()) return;
        for (String raw : payload.labels().split(SEP, -1)) {
            String[] p = raw.split("\\|", 5);
            if (p.length < 5 || !"relic".equals(p[0])) continue;
            relics.add(new Relic(p[1], "owned".equals(p[2]), plain(p[3]), plain(p[4])));
        }
    }

    private String fit(String value, int maxWidth) {
        String safe = plain(value);
        if (maxWidth <= 0 || font.width(safe) <= maxWidth) return maxWidth <= 0 ? "" : safe;
        int end = safe.length();
        while (end > 0 && font.width(safe.substring(0, end) + "…") > maxWidth) end--;
        return safe.substring(0, end) + "…";
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

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Relic(String id, boolean owned, String name, String description) {}
    private record Grid(int left, int top, int right, int bottom, int columns, int cell) {
        int height() { return bottom - top; }
    }
}

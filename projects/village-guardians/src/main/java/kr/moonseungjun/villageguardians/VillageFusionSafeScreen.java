package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Smithy fusion altar constrained to the common safe viewport. */
public final class VillageFusionSafeScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x72090B0D;
    private static final int TEXT = 0xFFF2ECE1;
    private static final int MUTED = 0xFFAAA49A;
    private static final int TEAL = 0xFF51CDB7;
    private static final int GOLD = 0xFFE1A74A;
    private static final int EMBER = 0xFFE86B3D;
    private static final int SURFACE = 0xC8141B1E;
    private static final int LINE = 0x99708186;

    private final List<Candidate> candidates = new ArrayList<>();
    private final List<Integer> selectedSlots = new ArrayList<>();
    private int scroll;

    public VillageFusionSafeScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        parse(payload);
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
        graphics.centeredText(font, "대장간  //  장비 융합", safe.centerX(), safe.top() + 5, GOLD);
        graphics.centeredText(font, instruction(), safe.centerX(), safe.top() + 20,
                selectedSlots.size() == 3 ? TEAL : MUTED);
        graphics.fill(safe.left() + 12, safe.top() + 34, safe.right() - 12, safe.top() + 35, 0x88785C35);

        for (int i = 0; i < 3; i++) {
            Socket socket = layout.sockets()[i];
            Candidate candidate = selectedCandidate(i);
            drawSocket(graphics, socket, candidate, mouseX, mouseY, i);
        }
        drawCombine(graphics, layout, mouseX, mouseY);
        drawCandidates(graphics, layout, mouseX, mouseY);
        graphics.text(font, "ESC 닫기", safe.left() + 4, safe.bottom() - 11, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private String instruction() {
        if (selectedSlots.isEmpty()) return "첫 재료를 고르면 같은 종류·등급만 선택할 수 있습니다.";
        if (selectedSlots.size() < 3) return "융합 재료 " + selectedSlots.size() + " / 3";
        return "재료 준비 완료 · 중앙의 융합 문양을 누르세요.";
    }

    private void drawSocket(GuiGraphicsExtractor graphics, Socket socket, Candidate candidate,
                            int mouseX, int mouseY, int index) {
        boolean hovered = insideDiamond(mouseX, mouseY, socket.x(), socket.y(), socket.radius() + 4);
        boolean filled = candidate != null;
        VillageQuickChatSafeScreen.drawDiamond(graphics, socket.x(), socket.y(), socket.radius(), SURFACE);
        VillageQuickChatSafeScreen.drawDiamondOutline(graphics, socket.x(), socket.y(), socket.radius(),
                filled ? GOLD : hovered ? TEAL : 0xFF718087);
        VillageQuickChatSafeScreen.drawDiamond(graphics, socket.x(), socket.y(), 8,
                filled ? 0xDD51371C : 0xDD263034);
        graphics.centeredText(font, Integer.toString(index + 1), socket.x(), socket.y() - 4,
                filled ? GOLD : MUTED);
        String label = candidate == null ? "빈 소켓" : candidate.name();
        graphics.centeredText(font, fit(font, label, 118), socket.x(), socket.labelY(),
                filled ? TEXT : MUTED);
        if (candidate != null) {
            graphics.centeredText(font, fit(font, candidate.rarity(), 92), socket.x(), socket.labelY() + 11,
                    rarityColor(candidate.rarity()));
        }
    }

    private void drawCombine(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int cx = layout.safe().centerX();
        int cy = layout.combineY();
        boolean ready = selectedSlots.size() == 3;
        boolean hovered = ready && insideDiamond(mouseX, mouseY, cx, cy, 29);
        VillageQuickChatSafeScreen.drawDiamond(graphics, cx, cy, 27, 0xED16100E);
        VillageQuickChatSafeScreen.drawDiamondOutline(graphics, cx, cy, 27,
                ready ? (hovered ? GOLD : EMBER) : 0xFF657176);
        VillageQuickChatSafeScreen.drawDiamondOutline(graphics, cx, cy, 16,
                ready ? GOLD : 0xFF4B565B);
        graphics.centeredText(font, ready ? "융합" : selectedSlots.size() + "/3", cx, cy - 4,
                ready ? TEXT : MUTED);
    }

    private void drawCandidates(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Grid grid = layout.grid();
        graphics.text(font, "보유 융합 재료", grid.left(), grid.top() - 15, MUTED, false);
        graphics.fill(grid.left(), grid.top() - 5, grid.right(), grid.top() - 4, LINE);
        int rows = candidates.isEmpty() ? 0 : (candidates.size() + grid.columns() - 1) / grid.columns();
        int content = rows * grid.rowHeight();
        int maximum = Math.max(0, content - grid.height());
        scroll = VillageUiSafeArea.clamp(scroll, 0, maximum);

        graphics.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        for (int index = 0; index < candidates.size(); index++) {
            Candidate candidate = candidates.get(index);
            int row = index / grid.columns();
            int col = index % grid.columns();
            int x = grid.left() + col * grid.cellWidth();
            int y = grid.top() + row * grid.rowHeight() - scroll;
            int w = grid.cellWidth() - 6;
            int h = grid.rowHeight() - 5;
            if (y + h < grid.top() || y >= grid.bottom()) continue;
            boolean selected = selectedSlots.contains(candidate.slot());
            boolean compatible = selectedSlots.isEmpty() || selected || selectedGroup().equals(candidate.group());
            boolean hovered = inside(mouseX, mouseY, x, y, w, h);
            if (hovered && compatible) graphics.fill(x, y, x + w, y + h, 0x501F3940);
            int dx = x + 10;
            int dy = y + h / 2;
            VillageQuickChatSafeScreen.drawDiamond(graphics, dx, dy, selected ? 7 : 5, 0xCC192529);
            VillageQuickChatSafeScreen.drawDiamondOutline(graphics, dx, dy, selected ? 7 : 5,
                    selected ? GOLD : compatible ? TEAL : 0xFF555A5D);
            int color = compatible ? (selected ? GOLD : TEXT) : 0xFF666A6C;
            graphics.text(font, fit(font, candidate.name(), w - 26), x + 21, y + 4, color, false);
            graphics.text(font, fit(font, candidate.rarity() + " · 슬롯 " + (candidate.slot() + 1), w - 26),
                    x + 21, y + 16, compatible ? rarityColor(candidate.rarity()) : 0xFF666A6C, false);
        }
        graphics.disableScissor();

        if (candidates.isEmpty()) {
            graphics.centeredText(font, "융합 가능한 장비가 없습니다.", grid.centerX(),
                    grid.top() + Math.max(8, grid.height() / 2), MUTED);
        }
        if (maximum > 0) {
            int thumb = Math.max(12, grid.height() * grid.height() / Math.max(grid.height(), content));
            int y = grid.top() + (grid.height() - thumb) * scroll / maximum;
            graphics.fill(grid.right() - 2, grid.top(), grid.right(), grid.bottom(), 0x555C686D);
            graphics.fill(grid.right() - 2, y, grid.right(), y + thumb, TEAL);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (selectedSlots.size() == 3
                && insideDiamond(click.x(), click.y(), layout.safe().centerX(), layout.combineY(), 31)) {
            String action = "fusion_combine:" + selectedSlots.get(0) + ","
                    + selectedSlots.get(1) + "," + selectedSlots.get(2);
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
            return true;
        }
        for (int i = 0; i < 3; i++) {
            Candidate candidate = selectedCandidate(i);
            Socket socket = layout.sockets()[i];
            if (candidate != null && insideDiamond(click.x(), click.y(), socket.x(), socket.y(), socket.radius() + 3)) {
                selectedSlots.remove(Integer.valueOf(candidate.slot()));
                return true;
            }
        }
        Grid grid = layout.grid();
        for (int index = 0; index < candidates.size(); index++) {
            int row = index / grid.columns();
            int col = index % grid.columns();
            int x = grid.left() + col * grid.cellWidth();
            int y = grid.top() + row * grid.rowHeight() - scroll;
            if (inside(click.x(), click.y(), x, y, grid.cellWidth() - 6, grid.rowHeight() - 5)) {
                toggle(candidates.get(index));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Grid grid = layout().grid();
        if (inside(mouseX, mouseY, grid.left(), grid.top(), grid.width(), grid.height())) {
            scroll = Math.max(0, scroll - (int) Math.round(vertical * 31));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void toggle(Candidate candidate) {
        if (selectedSlots.contains(candidate.slot())) {
            selectedSlots.remove(Integer.valueOf(candidate.slot()));
            return;
        }
        if (selectedSlots.size() >= 3) return;
        if (!selectedSlots.isEmpty() && !selectedGroup().equals(candidate.group())) return;
        selectedSlots.add(candidate.slot());
    }

    private Candidate selectedCandidate(int index) {
        if (index < 0 || index >= selectedSlots.size()) return null;
        return candidateBySlot(selectedSlots.get(index)).orElse(null);
    }

    private String selectedGroup() {
        if (selectedSlots.isEmpty()) return "";
        return candidateBySlot(selectedSlots.getFirst()).map(Candidate::group).orElse("");
    }

    private Optional<Candidate> candidateBySlot(int slot) {
        return candidates.stream().filter(candidate -> candidate.slot() == slot).findFirst();
    }

    private void parse(VillageNetwork.OpenVillageUiPayload payload) {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String[] p = labels[i].split("\\|", -1);
            if (p.length >= 6 && "fusion".equals(p[0])) {
                candidates.add(new Candidate(parseInt(p[1]), p[2], p[3], p[4], p[5]));
            }
        }
    }

    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int top = safe.top() + 45;
        int socketRadius = VillageUiSafeArea.clamp(safe.height() / 14, 20, 27);
        int spread = VillageUiSafeArea.clamp(safe.width() / 5, 70, 132);
        int socketY = top + socketRadius + 8;
        int labelY = socketY + socketRadius + 8;
        Socket[] sockets = new Socket[]{
                new Socket(safe.centerX() - spread, socketY, socketRadius, labelY),
                new Socket(safe.centerX(), socketY, socketRadius, labelY),
                new Socket(safe.centerX() + spread, socketY, socketRadius, labelY)
        };
        int combineY = labelY + 42;
        int gridTop = combineY + 45;
        int gridBottom = safe.bottom() - 20;
        if (gridBottom < gridTop + 42) gridTop = Math.max(labelY + 35, gridBottom - 42);
        int gridLeft = safe.left() + 18;
        int gridRight = safe.right() - 18;
        int columns = VillageUiSafeArea.clamp((gridRight - gridLeft) / 142, 2, 6);
        int cellWidth = Math.max(70, (gridRight - gridLeft) / columns);
        return new Layout(safe, sockets, combineY,
                new Grid(gridLeft, gridTop, gridRight, gridBottom, columns, cellWidth, 34));
    }

    private static int rarityColor(String rarity) {
        String value = rarity == null ? "" : rarity.toLowerCase(Locale.ROOT);
        if (value.contains("전설") || value.contains("legend")) return 0xFFFFB347;
        if (value.contains("영웅") || value.contains("epic")) return 0xFFD674FF;
        if (value.contains("희귀") || value.contains("rare")) return 0xFF71A8FF;
        if (value.contains("고급") || value.contains("uncommon")) return 0xFF75D98D;
        return MUTED;
    }

    private static String fit(Font font, String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + "…") > maxWidth) end--;
        return normalized.substring(0, end) + "…";
    }

    private static int parseInt(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return -1; }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean insideDiamond(double x, double y, int cx, int cy, int radius) {
        return Math.abs(x - cx) + Math.abs(y - cy) <= radius;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Candidate(int slot, String group, String name, String rarity, String itemId) {}
    private record Socket(int x, int y, int radius, int labelY) {}
    private record Layout(VillageUiSafeArea.Rect safe, Socket[] sockets, int combineY, Grid grid) {}
    private record Grid(int left, int top, int right, int bottom, int columns, int cellWidth, int rowHeight) {
        int width() { return right - left; }
        int height() { return bottom - top; }
        int centerX() { return (left + right) / 2; }
    }
}

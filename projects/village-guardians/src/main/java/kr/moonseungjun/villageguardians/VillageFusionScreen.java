package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Three-item smithy fusion presented as a forge altar with three equipment sockets. */
public final class VillageFusionScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x6C090B0D;
    private static final int TEXT = 0xFFF2ECE1;
    private static final int MUTED = 0xFFAAA49A;
    private static final int TEAL = 0xFF51CDB7;
    private static final int GOLD = 0xFFE1A74A;
    private static final int EMBER = 0xFFE86B3D;
    private static final int LINE = 0xAA5E747A;
    private static final int SOCKET = 0xE5162023;

    private final List<Candidate> candidates = new ArrayList<>();
    private final List<Integer> selectedSlots = new ArrayList<>();
    private int scroll;

    public VillageFusionScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        parse(payload);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ForgeLayout layout = layout();
        int cx = width / 2;
        graphics.centeredText(font, "대장간  //  장비 융합", cx, layout.titleY(), GOLD);
        graphics.centeredText(font,
                selectedSlots.isEmpty() ? "첫 장비를 고르면 같은 종류·등급만 활성화됩니다."
                        : selectedSlots.size() < 3 ? "융합 재료 " + selectedSlots.size() + " / 3"
                        : "재료 준비 완료 · 중앙의 융합 문양을 선택하세요.",
                cx, layout.titleY() + 16, selectedSlots.size() == 3 ? TEAL : MUTED);
        graphics.fill(Math.max(18, cx - 170), layout.titleY() + 31,
                Math.min(width - 18, cx + 170), layout.titleY() + 32, 0x88785C35);

        Socket[] sockets = sockets(layout);
        for (Socket socket : sockets) {
            VillageQuickChatScreen.drawLine(graphics, socket.x(), socket.y(), layout.coreX(), layout.coreY(),
                    socket.candidate() == null ? 0x66515E63 : 0xCCB68542);
        }
        for (Socket socket : sockets) drawSocket(graphics, socket, mouseX, mouseY);
        drawCore(graphics, layout, mouseX, mouseY);

        renderCandidates(graphics, layout, mouseX, mouseY);
        graphics.text(font, "ESC  닫기", 10, height - 16, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSocket(GuiGraphicsExtractor graphics, Socket socket, int mouseX, int mouseY) {
        boolean hovered = insideDiamond(mouseX, mouseY, socket.x(), socket.y(), 29);
        boolean filled = socket.candidate() != null;
        VillageQuickChatScreen.drawDiamond(graphics, socket.x(), socket.y(), hovered ? 27 : 23, SOCKET);
        VillageQuickChatScreen.drawDiamondOutline(graphics, socket.x(), socket.y(), hovered ? 27 : 23,
                filled ? GOLD : hovered ? TEXT : 0xFF708087);
        VillageQuickChatScreen.drawDiamond(graphics, socket.x(), socket.y(), 10,
                filled ? 0xDD4A331B : 0xDD242B2E);
        graphics.centeredText(font, Integer.toString(socket.index() + 1), socket.x(), socket.y() - 4,
                filled ? GOLD : MUTED);

        String name = filled ? socket.candidate().name() : "빈 재료 소켓";
        int labelY = socket.y() + (socket.index() == 1 ? -39 : 31);
        graphics.centeredText(font, fit(name, Math.min(132, Math.max(68, width / 4))),
                socket.x(), labelY, filled ? TEXT : MUTED);
        if (filled) {
            graphics.centeredText(font, fit(socket.candidate().rarity(), 92), socket.x(), labelY + 11,
                    rarityColor(socket.candidate().rarity()));
        }
    }

    private void drawCore(GuiGraphicsExtractor graphics, ForgeLayout layout, int mouseX, int mouseY) {
        boolean ready = selectedSlots.size() == 3;
        boolean hovered = ready && insideDiamond(mouseX, mouseY, layout.coreX(), layout.coreY(), 31);
        int outer = ready ? (hovered ? GOLD : EMBER) : 0xFF657176;
        VillageQuickChatScreen.drawDiamond(graphics, layout.coreX(), layout.coreY(), hovered ? 30 : 27, 0xED16100E);
        VillageQuickChatScreen.drawDiamondOutline(graphics, layout.coreX(), layout.coreY(), hovered ? 30 : 27, outer);
        VillageQuickChatScreen.drawDiamondOutline(graphics, layout.coreX(), layout.coreY(), 18,
                ready ? GOLD : 0xFF4B565B);
        graphics.centeredText(font, ready ? "융합" : "3/3", layout.coreX(), layout.coreY() - 4,
                ready ? TEXT : MUTED);
    }

    private void renderCandidates(GuiGraphicsExtractor graphics, ForgeLayout layout, int mouseX, int mouseY) {
        CandidateGrid grid = candidateGrid(layout);
        int columns = grid.columns();
        int rows = candidates.isEmpty() ? 0 : (candidates.size() + columns - 1) / columns;
        int content = rows * grid.rowHeight();
        int maximum = Math.max(0, content - grid.height());
        scroll = clamp(scroll, 0, maximum);

        graphics.centeredText(font, "보유 융합 재료", width / 2, grid.top() - 18, MUTED);
        graphics.fill(grid.left(), grid.top() - 5, grid.right(), grid.top() - 4, 0x66708186);
        graphics.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        for (int index = 0; index < candidates.size(); index++) {
            Candidate candidate = candidates.get(index);
            int row = index / columns;
            int column = index % columns;
            int x = grid.left() + column * grid.cellWidth();
            int y = grid.top() + row * grid.rowHeight() - scroll;
            if (y < grid.top() - grid.rowHeight() || y > grid.bottom()) continue;
            int w = grid.cellWidth() - 7;
            boolean selected = selectedSlots.contains(candidate.slot());
            boolean compatible = selectedSlots.isEmpty() || selected || selectedGroup().equals(candidate.group());
            boolean hovered = inside(mouseX, mouseY, x, y, w, grid.rowHeight() - 5);

            int markerX = x + 10;
            int markerY = y + 14;
            VillageQuickChatScreen.drawDiamond(graphics, markerX, markerY, selected ? 8 : 6,
                    selected ? 0xDD54391A : 0xDD1D282C);
            VillageQuickChatScreen.drawDiamondOutline(graphics, markerX, markerY, selected ? 8 : 6,
                    selected ? GOLD : compatible ? TEAL : 0xFF555A5D);
            graphics.text(font, fit(candidate.name(), w - 27), x + 22, y + 4,
                    compatible ? (selected ? GOLD : TEXT) : 0xFF6F7375, false);
            graphics.text(font, fit(candidate.rarity() + " · 슬롯 " + (candidate.slot() + 1), w - 27),
                    x + 22, y + 17, compatible ? rarityColor(candidate.rarity()) : 0xFF666A6C, false);
            graphics.fill(x + 22, y + 29, x + w, y + 30,
                    selected ? GOLD : hovered && compatible ? TEAL : 0x555C686D);
        }
        graphics.disableScissor();

        if (candidates.isEmpty()) {
            graphics.centeredText(font, "융합 가능한 습격 장비가 없습니다.", width / 2,
                    grid.top() + Math.max(8, grid.height() / 2), MUTED);
        }
        if (maximum > 0) {
            int track = Math.max(1, grid.height());
            int thumb = Math.max(12, track * grid.height() / Math.max(grid.height(), content));
            int y = grid.top() + (track - thumb) * scroll / maximum;
            graphics.fill(grid.right() - 2, grid.top(), grid.right(), grid.bottom(), 0x555C686D);
            graphics.fill(grid.right() - 2, y, grid.right(), y + thumb, TEAL);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        ForgeLayout layout = layout();
        if (selectedSlots.size() == 3
                && insideDiamond(click.x(), click.y(), layout.coreX(), layout.coreY(), 33)) {
            String action = "fusion_combine:" + selectedSlots.get(0) + ","
                    + selectedSlots.get(1) + "," + selectedSlots.get(2);
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
            return true;
        }

        Socket[] sockets = sockets(layout);
        for (Socket socket : sockets) {
            if (socket.candidate() != null && insideDiamond(click.x(), click.y(), socket.x(), socket.y(), 30)) {
                selectedSlots.remove(Integer.valueOf(socket.candidate().slot()));
                return true;
            }
        }

        CandidateGrid grid = candidateGrid(layout);
        for (int index = 0; index < candidates.size(); index++) {
            int row = index / grid.columns();
            int column = index % grid.columns();
            int x = grid.left() + column * grid.cellWidth();
            int y = grid.top() + row * grid.rowHeight() - scroll;
            int w = grid.cellWidth() - 7;
            if (inside(click.x(), click.y(), x, y, w, grid.rowHeight() - 5)) {
                toggle(candidates.get(index));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        CandidateGrid grid = candidateGrid(layout());
        if (inside(mouseX, mouseY, grid.left(), grid.top(), grid.width(), grid.height())) {
            scroll = Math.max(0, scroll - (int) Math.round(vertical * 34));
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

    private Socket[] sockets(ForgeLayout layout) {
        int spread = Math.min(128, Math.max(70, width / 4));
        int upper = Math.min(44, Math.max(30, height / 9));
        return new Socket[]{
                new Socket(0, layout.coreX() - spread, layout.coreY() - 16, selectedCandidate(0)),
                new Socket(1, layout.coreX(), layout.coreY() - upper - 29, selectedCandidate(1)),
                new Socket(2, layout.coreX() + spread, layout.coreY() - 16, selectedCandidate(2))
        };
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
        for (int index = 0; index < count; index++) {
            String[] p = labels[index].split("\\|", -1);
            if (p.length >= 6 && "fusion".equals(p[0])) {
                candidates.add(new Candidate(parseInt(p[1]), p[2], p[3], p[4], p[5]));
            }
        }
    }

    private ForgeLayout layout() {
        int titleY = Math.max(10, height / 18);
        int coreY = clamp(height / 3 + 24, titleY + 106, Math.max(titleY + 106, height - 150));
        return new ForgeLayout(titleY, width / 2, coreY);
    }

    private CandidateGrid candidateGrid(ForgeLayout layout) {
        int top = Math.min(height - 58, layout.coreY() + 74);
        int bottom = Math.max(top + 32, height - 24);
        int left = Math.max(10, width / 24);
        int right = Math.min(width - 10, width - width / 24);
        int columns = clamp(Math.max(1, (right - left) / 126), 2, 7);
        int cellWidth = Math.max(58, (right - left) / columns);
        return new CandidateGrid(left, top, right, bottom, columns, cellWidth, 36);
    }

    private int rarityColor(String rarity) {
        String value = rarity == null ? "" : rarity.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("전설") || value.contains("legend")) return 0xFFFFB347;
        if (value.contains("영웅") || value.contains("epic")) return 0xFFD674FF;
        if (value.contains("희귀") || value.contains("rare")) return 0xFF71A8FF;
        if (value.contains("고급") || value.contains("uncommon")) return 0xFF75D98D;
        return MUTED;
    }

    private String fit(String value, int maxWidth) {
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean insideDiamond(double x, double y, int cx, int cy, int radius) {
        return Math.abs(x - cx) + Math.abs(y - cy) <= radius;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Candidate(int slot, String group, String name, String rarity, String itemId) {}
    private record Socket(int index, int x, int y, Candidate candidate) {}
    private record ForgeLayout(int titleY, int coreX, int coreY) {}
    private record CandidateGrid(int left, int top, int right, int bottom, int columns, int cellWidth, int rowHeight) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
}

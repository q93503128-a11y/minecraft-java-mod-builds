package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.network.DominationManagementRequestPayload;
import dev.moonseungjun.augmentationsurvival.network.DominationManagementSnapshotPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Compact management roster: read, inspect, release loaded or unloaded dominated companions. */
public final class DominationManagementScreen extends Screen {
    private static final int BACKDROP = 0xE6252F32;
    private static final int SURFACE = 0xFFE3DED2;
    private static final int LINE = 0xFF6A726B;
    private static final int TEXT = 0xFF2D3431;
    private static final int MUTED = 0xFF696D67;
    private static final int ACCENT = 0xFF466E61;

    private DominationManagementSnapshotPayload snapshot;
    private int page;
    private String selected = "";

    public DominationManagementScreen(DominationManagementSnapshotPayload snapshot) {
        super(Component.literal("지배 관리"));
        this.snapshot = snapshot;
        if (!snapshot.companions().isEmpty()) selected = snapshot.companions().getFirst().uuid();
    }

    public void applySnapshot(DominationManagementSnapshotPayload next) {
        snapshot = next;
        if (next.companions().stream().noneMatch(view -> view.uuid().equals(selected))) {
            selected = next.companions().isEmpty() ? "" : next.companions().getFirst().uuid();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Layout l = layout();
        g.fill(0, 0, width, height, 0x95000000);
        g.fill(l.x(), l.y(), l.x() + l.w(), l.y() + l.h(), SURFACE);
        g.outline(l.x(), l.y(), l.w(), l.h(), LINE);
        g.fill(l.x(), l.y(), l.x() + l.w(), l.y() + 30, BACKDROP);
        g.text(font, "지배 관리", l.x() + 13, l.y() + 10, 0xFFF5F2EA, false);
        String count = snapshot.occupied() + " / " + snapshot.capacity() + " 점유";
        g.text(font, count, l.x() + l.w() - font.width(count) - 13, l.y() + 10, 0xFFF5F2EA, false);

        int start = page * l.rows();
        if (start >= snapshot.companions().size()) {
            page = Math.max(0, (snapshot.companions().size() - 1) / l.rows());
            start = page * l.rows();
        }
        List<DominationManagementSnapshotPayload.CompanionView> companions = snapshot.companions();
        if (companions.isEmpty()) {
            g.text(font, "지배한 동료가 없습니다.", l.x() + 17, l.listY() + 20, MUTED, false);
        }
        for (int row = 0; row < l.rows() && start + row < companions.size(); row++) {
            var companion = companions.get(start + row);
            int y = l.listY() + row * 26;
            boolean isSelected = selected.equals(companion.uuid());
            g.fill(l.listX(), y, l.listX() + l.listW(), y + 23,
                    isSelected ? 0xFFBFCFC3 : 0xFFD3D4CB);
            g.fill(l.listX(), y, l.listX() + 3, y + 23, isSelected ? ACCENT : LINE);
            String state = companion.loaded() ? "활동 중" : "멀리 있음";
            g.text(font, clip(companion.name(), l.listW() - 90), l.listX() + 10, y + 7, TEXT, false);
            g.text(font, state, l.listX() + l.listW() - font.width(state) - 8,
                    y + 7, companion.loaded() ? ACCENT : MUTED, false);
        }
        int navY = l.listY() + l.rows() * 26 + 4;
        g.text(font, "‹ 이전", l.listX() + 4, navY, page > 0 ? TEXT : MUTED, false);
        String pageInfo = (page + 1) + " / " + Math.max(1, (companions.size() + l.rows() - 1) / l.rows());
        g.text(font, pageInfo, l.listX() + (l.listW() - font.width(pageInfo)) / 2, navY, MUTED, false);
        g.text(font, "다음 ›", l.listX() + l.listW() - font.width("다음 ›") - 4,
                navY, (page + 1) * l.rows() < companions.size() ? TEXT : MUTED, false);

        var chosen = companions.stream().filter(view -> view.uuid().equals(selected)).findFirst().orElse(null);
        g.fill(l.detailX(), l.listY(), l.detailX() + l.detailW(), l.listY() + l.rows() * 26, 0xFFD5D7D0);
        if (chosen != null) {
            int x = l.detailX() + 11;
            int y = l.listY() + 10;
            g.text(font, clip(chosen.name(), l.detailW() - 22), x, y, TEXT, false);
            g.text(font, clip(chosen.entityId(), l.detailW() - 22), x, y + 17, MUTED, false);
            g.text(font, chosen.loaded() ? "현재 서버에서 활동 중" : "현재 개체가 로드되지 않음", x, y + 39, MUTED, false);
            g.text(font, clip(chosen.dimension(), l.detailW() - 22), x, y + 57, MUTED, false);
            int buttonY = l.listY() + l.rows() * 26 - 33;
            g.fill(x, buttonY, l.detailX() + l.detailW() - 11, buttonY + 24, 0xFFC5A29B);
            g.outline(x, buttonY, l.detailW() - 22, 24, 0xFF90483F);
            String action = "지배 해제";
            g.text(font, action, x + (l.detailW() - 22 - font.width(action)) / 2,
                    buttonY + 8, 0xFF54231D, false);
        }
        int footerY = l.y() + l.h() - 18;
        g.text(font, clip("Shift + 지배 슬롯: 관리 · 멀리 있는 동료도 해제 가능 · Esc: 닫기", l.w() - 20),
                l.x() + 10, footerY, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout l = layout();
        int listBottom = l.listY() + l.rows() * 26;
        if (inside(event.x(), event.y(), l.listX(), l.listY(), l.listW(), l.rows() * 26)) {
            int index = page * l.rows() + (int) (event.y() - l.listY()) / 26;
            if (index < snapshot.companions().size()) {
                selected = snapshot.companions().get(index).uuid();
                return true;
            }
        }
        if (inside(event.x(), event.y(), l.listX(), listBottom, l.listW() / 3, 19)) {
            page = Math.max(0, page - 1); return true;
        }
        if (inside(event.x(), event.y(), l.listX() + l.listW() * 2 / 3, listBottom,
                l.listW() / 3, 19)) {
            page = Math.min(Math.max(0, (snapshot.companions().size() - 1) / l.rows()), page + 1);
            return true;
        }
        int detailButtonX = l.detailX() + 11;
        int buttonY = l.listY() + l.rows() * 26 - 33;
        if (!selected.isBlank() && inside(event.x(), event.y(), detailButtonX, buttonY,
                l.detailW() - 22, 24)) {
            ClientPlayNetworking.send(new DominationManagementRequestPayload(false, selected));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Layout layout() {
        int w = Math.max(1, Math.min(620, width - 20));
        int h = Math.max(1, Math.min(330, height - 20));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int listX = x + 12, listY = y + 42;
        int listW = Math.max(120, w * 50 / 100);
        int detailX = listX + listW + 10;
        int detailW = w - (detailX - x) - 12;
        int rows = Math.max(1, Math.min(9, (h - 83) / 26));
        return new Layout(x, y, w, h, listX, listY, listW, detailX, detailW, rows);
    }

    private String clip(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String text = value;
        while (!text.isEmpty() && font.width(text + "…") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public boolean isPauseScreen() { return false; }
    private record Layout(int x, int y, int w, int h, int listX, int listY,
                          int listW, int detailX, int detailW, int rows) { }
}

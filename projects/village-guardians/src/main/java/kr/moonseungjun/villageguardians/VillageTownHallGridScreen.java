package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Wide town-hall dashboard: navigation is one click, permanent role changes require confirmation. */
public final class VillageTownHallGridScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x70070A0D;
    private static final int TEXT = 0xFFF1F4F5;
    private static final int MUTED = 0xFFAAB5BA;
    private static final int CYAN = 0xFF52D9C2;
    private static final int GOLD = 0xFFFFC65C;
    private static final int RED = 0xFFE06E64;
    private static final int SURFACE = 0xD1131B1F;
    private static final int SURFACE_HOVER = 0xE51D2A30;
    private static final int LINE = 0xA34B6873;

    private final String body;
    private final List<RoleCard> roles = new ArrayList<>();
    private final List<FacilityCard> facilities = new ArrayList<>();

    public VillageTownHallGridScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
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
        graphics.text(font, "지휘 회관", safe.left() + 7, safe.top() + 4, GOLD, false);
        List<FormattedCharSequence> summary = font.split(Component.literal(body), Math.max(80, safe.width() - 18));
        if (!summary.isEmpty()) graphics.text(font, summary.getFirst(), safe.left() + 7, safe.top() + 20, MUTED, false);
        graphics.fill(safe.left() + 7, safe.top() + 35, safe.right() - 7, safe.top() + 36, LINE);

        graphics.text(font, "직업 배치", safe.left() + 7, layout.roleTitleY(), CYAN, false);
        for (int i = 0; i < roles.size(); i++) {
            Cell c = roleCell(layout, i);
            RoleCard role = roles.get(i);
            boolean hovered = inside(mouseX, mouseY, c.x(), c.y(), c.w(), c.h());
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hovered ? SURFACE_HOVER : SURFACE);
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + 2, role.current() ? GOLD : CYAN);
            VillageQuickChatSafeScreen.drawDiamond(graphics, c.x() + 13, c.y() + 16, 6, 0xCC203036);
            VillageQuickChatSafeScreen.drawDiamondOutline(graphics, c.x() + 13, c.y() + 16, 6,
                    role.current() ? GOLD : CYAN);
            graphics.text(font, fit(font, role.name(), c.w() - 34), c.x() + 27, c.y() + 8,
                    role.current() ? GOLD : TEXT, false);
            graphics.text(font, role.current() ? "현재 직업" : "선택 후 확인", c.x() + 8, c.y() + c.h() - 14,
                    role.current() ? GOLD : CYAN, false);
        }

        graphics.text(font, "시설 지휘", safe.left() + 7, layout.facilityTitleY(), CYAN, false);
        for (int i = 0; i < facilities.size(); i++) {
            Cell c = facilityCell(layout, i);
            FacilityCard facility = facilities.get(i);
            boolean hovered = inside(mouseX, mouseY, c.x(), c.y(), c.w(), c.h());
            graphics.fill(c.x(), c.y(), c.x() + c.w(), c.y() + c.h(), hovered ? SURFACE_HOVER : SURFACE);
            graphics.fill(c.x(), c.y(), c.x() + 4, c.y() + c.h(), hovered ? GOLD : CYAN);
            graphics.text(font, fit(font, facility.name(), c.w() - 18), c.x() + 11, c.y() + 6,
                    hovered ? GOLD : TEXT, false);
            graphics.text(font, fit(font, facility.meta(), c.w() - 18), c.x() + 11, c.y() + 20, CYAN, false);
            if (c.h() >= 53) {
                graphics.text(font, fit(font, facility.effect(), c.w() - 18), c.x() + 11, c.y() + 35, MUTED, false);
            }
            if (facility.maximum() > 0) {
                int left = c.x() + 11;
                int right = c.x() + c.w() - 10;
                int barY = c.y() + c.h() - 9;
                int fill = left + (right - left) * Math.max(0, Math.min(facility.current(), facility.maximum()))
                        / Math.max(1, facility.maximum());
                graphics.fill(left, barY, right, barY + 3, 0xFF39464B);
                graphics.fill(left, barY, fill, barY + 3,
                        facility.current() * 3 < facility.maximum() ? RED : CYAN);
            }
        }
        graphics.text(font, "시설 카드를 누르면 해당 기능 화면으로 이동합니다.  ·  ESC 닫기",
                safe.left() + 4, safe.bottom() - 11, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        for (int i = 0; i < roles.size(); i++) {
            Cell c = roleCell(layout, i);
            if (!inside(click.x(), click.y(), c.x(), c.y(), c.w(), c.h())) continue;
            RoleCard role = roles.get(i);
            if (role.current()) return true;
            String detail = role.overview()
                    + "\n상시 효과: " + role.passive()
                    + "\n전투 방식: " + role.active()
                    + "\n추천 위치: " + role.recommended();
            if (minecraft != null) {
                minecraft.gui.setScreen(new VillageConfirmScreen(this, role.action(), role.name() + " 배치", detail));
            }
            return true;
        }
        for (int i = 0; i < facilities.size(); i++) {
            Cell c = facilityCell(layout, i);
            if (!inside(click.x(), click.y(), c.x(), c.y(), c.w(), c.h())) continue;
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(facilities.get(i).action()));
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int roleTitleY = safe.top() + 43;
        int roleTop = roleTitleY + 13;
        int roleHeight = safe.height() < 260 ? 39 : 51;
        int roleGap = 5;
        int roleCount = Math.max(1, roles.size());
        int roleWidth = Math.max(52, (safe.width() - 14 - roleGap * (roleCount - 1)) / roleCount);
        roleWidth = Math.min(150, roleWidth);
        int roleTotal = roleCount * roleWidth + roleGap * Math.max(0, roleCount - 1);
        int roleLeft = safe.centerX() - roleTotal / 2;

        int facilityTitleY = roleTop + roleHeight + 9;
        int facilityTop = facilityTitleY + 14;
        int facilityBottom = safe.bottom() - 18;
        int columns = facilityColumns(safe.width(), facilities.size());
        int rows = Math.max(1, rows(facilities.size(), columns));
        int gap = 6;
        int cellWidth = Math.max(60, (safe.width() - 14 - gap * (columns - 1)) / columns);
        int available = Math.max(1, facilityBottom - facilityTop);
        int rowHeight = Math.max(35, Math.min(96, (available - gap * (rows - 1)) / rows));
        return new Layout(safe, roleTitleY, roleLeft, roleTop, roleWidth, roleHeight, roleGap,
                facilityTitleY, facilityTop, columns, cellWidth, rowHeight, gap);
    }

    private int facilityColumns(int availableWidth, int count) {
        if (count <= 0) return 1;
        if (count <= 4) return count;
        if (availableWidth >= 520) return 4;
        return 3;
    }

    private Cell roleCell(Layout layout, int index) {
        return new Cell(layout.roleLeft() + index * (layout.roleWidth() + layout.roleGap()),
                layout.roleTop(), layout.roleWidth(), layout.roleHeight());
    }

    private Cell facilityCell(Layout layout, int index) {
        int row = index / layout.facilityColumns();
        int col = index % layout.facilityColumns();
        int x = layout.safe().left() + 7 + col * (layout.facilityWidth() + layout.facilityGap());
        int y = layout.facilityTop() + row * (layout.facilityHeight() + layout.facilityGap());
        return new Cell(x, y, layout.facilityWidth(), layout.facilityHeight());
    }

    private void parse(VillageNetwork.OpenVillageUiPayload payload) {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String[] p = labels[i].split("\\|", -1);
            if (p.length >= 8 && "role".equals(p[0])) {
                roles.add(new RoleCard(actions[i], plain(p[2]), plain(p[3]), plain(p[4]), plain(p[5]),
                        plain(p[6]), "current".equals(p[7])));
            } else if (p.length >= 7 && "facility".equals(p[0])) {
                facilities.add(new FacilityCard(actions[i], plain(p[2]), plain(p[3]), plain(p[6]),
                        parseInt(p[4]), parseInt(p[5])));
            }
        }
    }

    private static int rows(int count, int columns) {
        return count <= 0 ? 1 : (count + columns - 1) / columns;
    }

    private static int parseInt(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static String fit(Font font, String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + "…") > maxWidth) end--;
        return normalized.substring(0, end) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record RoleCard(String action, String name, String overview, String passive,
                            String active, String recommended, boolean current) {}
    private record FacilityCard(String action, String name, String meta, String effect,
                                int current, int maximum) {}
    private record Cell(int x, int y, int w, int h) {}
    private record Layout(VillageUiSafeArea.Rect safe, int roleTitleY, int roleLeft, int roleTop,
                          int roleWidth, int roleHeight, int roleGap, int facilityTitleY, int facilityTop,
                          int facilityColumns, int facilityWidth, int facilityHeight, int facilityGap) {}
}

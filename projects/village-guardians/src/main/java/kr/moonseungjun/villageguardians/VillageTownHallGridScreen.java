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

/**
 * Compact command-table town hall. Nothing executes from the list itself: the left rail selects,
 * the right dossier explains, and the bottom actions expose facility use, repair and upgrade separately.
 */
public final class VillageTownHallGridScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x7805090C;
    private static final int PANEL = 0xF00B1217;
    private static final int PANEL_2 = 0xE9142027;
    private static final int PANEL_3 = 0xE91B2A32;
    private static final int LINE = 0xB04F6873;
    private static final int TEXT = 0xFFF3F5F5;
    private static final int MUTED = 0xFFA8B4B9;
    private static final int CYAN = 0xFF50D9C1;
    private static final int GOLD = 0xFFF2C35D;
    private static final int RED = 0xFFE56A64;
    private static final int GREEN = 0xFF76D39A;
    private static final int BLUE = 0xFF7AA9E8;

    private final String body;
    private final List<RoleCard> roles = new ArrayList<>();
    private final List<FacilityCard> facilities = new ArrayList<>();
    private Tab tab = Tab.FACILITIES;
    private int selectedRole;
    private int selectedFacility;
    private int listScroll;

    public VillageTownHallGridScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        body = plain(payload.body());
        parse(payload);
        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i).current()) {
                selectedRole = i;
                break;
            }
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
        drawFrame(graphics, layout);
        drawHeader(graphics, layout, mouseX, mouseY);
        drawList(graphics, layout.list(), mouseX, mouseY);
        drawDetail(graphics, layout.detail(), mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFrame(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, LINE);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 4, layout.bottom(), GOLD);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int x = layout.left() + 17;
        int closeX = layout.right() - 34;
        graphics.text(font, "지휘 회관 · 시설 유지보수", x, layout.top() + 10, GOLD, false);
        int bodyWidth = Math.max(80, closeX - x - 10);
        List<FormattedCharSequence> lines = font.split(Component.literal(body.replace('\n', ' ')), bodyWidth);
        int bodyY = layout.top() + 27;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.text(font, lines.get(i), x, bodyY, MUTED, false);
            bodyY += 11;
        }
        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 8, 24, 24);
        graphics.fill(closeX, layout.top() + 8, closeX + 24, layout.top() + 32, close ? 0xFF71353A : PANEL_3);
        graphics.centeredText(font, "×", closeX + 12, layout.top() + 15, close ? TEXT : MUTED);
        graphics.fill(layout.left() + 14, layout.top() + 52, layout.right() - 14, layout.top() + 53, LINE);
    }

    private void drawTabs(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int x = layout.left() + 14;
        int y = layout.top() + 50;
        int gap = 5;
        int w = Math.min(144, Math.max(60, (layout.width() - 33) / 2));
        drawTab(graphics, x, y, w, Tab.FACILITIES, mouseX, mouseY);
        drawTab(graphics, x + w + gap, y, w, Tab.ROLES, mouseX, mouseY);
    }

    private void drawTab(GuiGraphicsExtractor graphics, int x, int y, int w, Tab value, int mouseX, int mouseY) {
        boolean active = tab == value;
        boolean hover = inside(mouseX, mouseY, x, y, w, 20);
        int accent = value == Tab.FACILITIES ? GOLD : CYAN;
        graphics.fill(x, y, x + w, y + 20, active ? PANEL_3 : hover ? PANEL_2 : PANEL);
        graphics.fill(x, y + 18, x + w, y + 20, active ? accent : LINE);
        graphics.centeredText(font, value.label(), x + w / 2, y + 6, active ? TEXT : MUTED);
    }

    private void drawList(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), PANEL_2);
        int count = facilities.size();
        int rowHeight = 50;
        int gap = 4;
        int content = count <= 0 ? 0 : count * rowHeight + Math.max(0, count - 1) * gap;
        int maxScroll = Math.max(0, content - Math.max(1, pane.height() - 12));
        listScroll = clamp(listScroll, 0, maxScroll);
        graphics.enableScissor(pane.left() + 1, pane.top() + 1, pane.right() - 1, pane.bottom() - 1);
        int y = pane.top() + 6 - listScroll;
        for (int i = 0; i < count; i++) {
            FacilityCard f = facilities.get(i);
            int x = pane.left() + 6;
            int w = pane.width() - 14;
            boolean hover = inside(mouseX, mouseY, x, y, w, rowHeight);
            boolean selected = selectedFacility == i;
            int accent = facilityColor(f);
            graphics.fill(x, y, x + w, y + rowHeight, selected ? PANEL_3 : hover ? 0xE522333B : 0xD9111B21);
            graphics.fill(x, y, x + 3, y + rowHeight, accent);
            graphics.text(font, fit(font, f.name(), w - 18), x + 10, y + 6, selected ? TEXT : MUTED, false);
            graphics.text(font, fit(font, f.meta(), w - 18), x + 10, y + 20, MUTED, false);
            String durability = f.current() <= 0 ? "파괴됨" : "내구도 " + f.current() + " / " + f.maximum();
            graphics.text(font, fit(font, durability, w - 18), x + 10, y + 34, accent, false);
            y += rowHeight + gap;
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int track = pane.height() - 12;
            int thumb = Math.max(14, track * Math.max(1, pane.height() - 12) / Math.max(1, content));
            int sy = pane.top() + 6 + (track - thumb) * listScroll / maxScroll;
            graphics.fill(pane.right() - 4, pane.top() + 6, pane.right() - 2, pane.bottom() - 6, 0x55607178);
            graphics.fill(pane.right() - 4, sy, pane.right() - 2, sy + thumb, GOLD);
        }
    }

    private void drawDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), 0xD90D171D);
        drawFacilityDetail(graphics, pane, mouseX, mouseY);
    }

    private void drawRoleDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        if (roles.isEmpty()) return;
        RoleCard role = roles.get(clamp(selectedRole, 0, roles.size() - 1));
        int x = pane.left() + 16;
        int right = pane.right() - 16;
        int y = pane.top() + 14;
        graphics.text(font, fit(font, role.name(), Math.max(40, right - x)), x, y, role.current() ? GOLD : CYAN, false);
        graphics.text(font, role.current() ? "현재 직업" : "배치 변경 후보", x, y + 16, role.current() ? GOLD : MUTED, false);
        y += 38;
        Button action = roleButton(pane);
        int clipBottom = role.current() ? pane.bottom() - 8 : action.y() - 6;
        graphics.enableScissor(pane.left() + 1, pane.top() + 1, pane.right() - 1, Math.max(pane.top() + 2, clipBottom));
        y = section(graphics, "역할", role.overview(), x, right, y, TEXT);
        y = section(graphics, "상시 효과", role.passive(), x, right, y, CYAN);
        y = section(graphics, "전투 방식", role.active(), x, right, y, GOLD);
        section(graphics, "추천 위치", role.recommended(), x, right, y, BLUE);
        graphics.disableScissor();
        if (!role.current()) {
            drawButton(graphics, action, "이 직업으로 배치", true, CYAN, mouseX, mouseY);
        }
    }

    private void drawFacilityDetail(GuiGraphicsExtractor graphics, Pane pane, int mouseX, int mouseY) {
        if (facilities.isEmpty()) return;
        FacilityCard f = facilities.get(clamp(selectedFacility, 0, facilities.size() - 1));
        List<ButtonSpec> specs = facilityButtons(pane, f);
        int actionTop = specs.stream().mapToInt(spec -> spec.bounds().y()).min().orElse(pane.bottom()) - 6;
        int accent = facilityColor(f);
        int x = pane.left() + 16;
        int right = pane.right() - 16;
        int y = pane.top() + 13;

        graphics.enableScissor(pane.left() + 1, pane.top() + 1, pane.right() - 1,
                Math.max(pane.top() + 2, actionTop));
        graphics.text(font, fit(font, f.name(), Math.max(40, right - x)), x, y, accent, false);
        graphics.text(font, fit(font, f.meta(), Math.max(40, right - x)), x, y + 16, MUTED, false);

        int barTop = y + 34;
        graphics.fill(x, barTop, right, barTop + 5, 0xFF334047);
        int fill = f.maximum() <= 0 ? 0 : Math.round((right - x) * clamp(f.current(), 0, f.maximum()) / (float) f.maximum());
        graphics.fill(x, barTop, x + Math.max(0, fill), barTop + 5, accent);
        graphics.text(font, "내구도 " + f.current() + " / " + f.maximum(), x, barTop + 10, TEXT, false);
        y = barTop + 30;

        if (f.current() <= 0) {
            y = section(graphics, "시설 상태", "파괴됨 · 기능 정지", x, right, y, RED);
            section(graphics, "복구", "회관에서 완전 수리하면 건물과 기능이 즉시 복구됩니다.", x, right, y, GOLD);
        } else {
            y = section(graphics, "현재 효과", f.effect(), x, right, y, CYAN);
            if (!f.nextEffect().isBlank() && f.upgradeCost() > 0) {
                y = section(graphics, "다음 단계", f.nextEffect(), x, right, y, GREEN);
                section(graphics, "강화 비용", "공동 보급품 " + f.upgradeCost(), x, right, y, GOLD);
            }
        }
        graphics.disableScissor();

        for (ButtonSpec spec : specs) {
            drawButton(graphics, spec.bounds(), spec.label(), spec.enabled(), spec.accent(), mouseX, mouseY);
        }
    }

    private int section(GuiGraphicsExtractor graphics, String title, String value,
                        int left, int right, int y, int color) {
        if (value == null || value.isBlank()) return y;
        graphics.text(font, title, left, y, color, false);
        int lineY = y + 13;
        List<FormattedCharSequence> lines = font.split(Component.literal(value), Math.max(70, right - left));
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            graphics.text(font, lines.get(i), left, lineY, MUTED, false);
            lineY += 11;
        }
        return lineY + 8;
    }

    private List<ButtonSpec> facilityButtons(Pane pane, FacilityCard f) {
        List<ButtonSpec> result = new ArrayList<>();
        boolean usable = f.current() > 0;
        boolean repair = f.current() < f.maximum() && f.repairCost() > 0;
        boolean upgrade = usable && f.upgradeCost() > 0 && !f.nextEffect().isBlank();
        String repairLabel = repair ? "건물 수리 · " + f.repairCost() : "수리 불필요";
        String upgradeLabel = upgrade ? "건물 강화 · " + f.upgradeCost() : "강화 완료";

        int gap = 7;
        int left = pane.left() + 14;
        int innerWidth = Math.max(1, pane.width() - 28);
        int h = 27;
        if (pane.width() < 260) {
            int y = pane.bottom() - 12 - (h * 2 + gap);
            result.add(new ButtonSpec(new Button(left, y, innerWidth, h), repairLabel, repair, GOLD, "repair:" + f.id()));
            y += h + gap;
            result.add(new ButtonSpec(new Button(left, y, innerWidth, h), upgradeLabel, upgrade, GREEN, "upgrade:" + f.id()));
            return result;
        }

        int available = Math.max(2, innerWidth - gap);
        int firstWidth = available / 2;
        int secondWidth = available - firstWidth;
        int y = pane.bottom() - 39;
        result.add(new ButtonSpec(new Button(left, y, firstWidth, h), repairLabel, repair, GOLD, "repair:" + f.id()));
        result.add(new ButtonSpec(new Button(left + firstWidth + gap, y, secondWidth, h),
                upgradeLabel, upgrade, GREEN, "upgrade:" + f.id()));
        return result;
    }

    private String functionAction(FacilityCard f) {
        if (f.id().equals("town_hall")) return "open_funding";
        if (f.id().equals("walls")) return "open_tower_control";
        return f.action();
    }

    private Button roleButton(Pane pane) {
        int w = Math.min(178, Math.max(76, pane.width() / 3));
        w = Math.min(w, Math.max(1, pane.width() - 28));
        return new Button(pane.right() - w - 14, pane.bottom() - 34, w, 23);
    }

    private void drawButton(GuiGraphicsExtractor graphics, Button b, String label, boolean enabled,
                            int accent, int mouseX, int mouseY) {
        boolean hover = enabled && inside(mouseX, mouseY, b.x(), b.y(), b.w(), b.h());
        int edge = enabled ? (hover ? TEXT : accent) : 0xFF536068;
        graphics.fill(b.x() - 1, b.y() - 1, b.x() + b.w() + 1, b.y() + b.h() + 1, edge);
        graphics.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), hover ? PANEL_3 : PANEL_2);
        graphics.centeredText(font, fit(font, label, b.w() - 8), b.x() + b.w() / 2, b.y() + 7,
                enabled ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 34, layout.top() + 8, 24, 24)) {
            onClose();
            return true;
        }

        Pane list = layout.list();
        int y = list.top() + 6 - listScroll;
        for (int i = 0; i < facilities.size(); i++) {
            if (inside(click.x(), click.y(), list.left() + 6, y, list.width() - 14, 50)) {
                selectedFacility = i;
                return true;
            }
            y += 54;
        }

        if (!facilities.isEmpty()) {
            FacilityCard facility = facilities.get(clamp(selectedFacility, 0, facilities.size() - 1));
            for (ButtonSpec spec : facilityButtons(layout.detail(), facility)) {
                if (!spec.enabled() || !inside(click.x(), click.y(), spec.bounds().x(), spec.bounds().y(),
                        spec.bounds().w(), spec.bounds().h())) continue;
                if (VillageActionDescriptions.requiresConfirmation(spec.action()) && minecraft != null) {
                    minecraft.gui.setScreen(new VillageConfirmScreen(this, spec.action(), facility.name(),
                            VillageActionDescriptions.describe(spec.action(), facility.name())));
                } else {
                    ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(spec.action()));
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Pane list = layout().list();
        if (inside(mouseX, mouseY, list.left(), list.top(), list.width(), list.height())) {
            listScroll = Math.max(0, listScroll - (int) Math.round(vertical * 31));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private Layout layout() {
        VillageUiSafeArea.Rect safe = VillageUiSafeArea.screen(width, height);
        int panelWidth = Math.min(940, Math.max(1, safe.width()));
        int panelHeight = Math.min(500, Math.max(1, safe.height()));
        int left = safe.centerX() - panelWidth / 2;
        int top = safe.top() + Math.max(0, (safe.height() - panelHeight) / 2);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int contentTop = Math.min(bottom - 1, top + 60);
        int contentBottom = Math.max(contentTop + 1, bottom - 12);
        int gap = 10;
        int contentWidth = Math.max(1, panelWidth - 28 - gap);
        int listWidth = clamp(panelWidth * 31 / 100, 150, 280);
        listWidth = Math.min(listWidth, Math.max(90, contentWidth - 170));
        Pane list = new Pane(left + 14, contentTop, left + 14 + listWidth, contentBottom);
        Pane detail = new Pane(Math.min(right - 15, list.right() + gap), contentTop, right - 14, contentBottom);
        return new Layout(left, top, right, bottom, list, detail);
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
                facilities.add(new FacilityCard(actions[i], plain(p[1]), plain(p[2]), plain(p[3]),
                        parseInt(p[4]), parseInt(p[5]), plain(p[6]), p.length > 7 ? plain(p[7]) : "",
                        p.length > 8 ? parseInt(p[8]) : 0, p.length > 9 ? parseInt(p[9]) : 0));
            }
        }
    }

    private static int facilityColor(FacilityCard f) {
        if (f.current() <= 0) return RED;
        if (f.maximum() > 0 && f.current() * 3 < f.maximum()) return RED;
        if (f.maximum() > 0 && f.current() < f.maximum()) return GOLD;
        return CYAN;
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

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Tab {
        FACILITIES("시설 관리"), ROLES("직업 배치");
        private final String label;
        Tab(String label) { this.label = label; }
        String label() { return label; }
    }

    private record RoleCard(String action, String name, String overview, String passive,
                            String active, String recommended, boolean current) {}
    private record FacilityCard(String action, String id, String name, String meta,
                                int current, int maximum, String effect, String nextEffect,
                                int upgradeCost, int repairCost) {}
    private record Button(int x, int y, int w, int h) {}
    private record ButtonSpec(Button bounds, String label, boolean enabled, int accent, String action) {}
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return Math.max(1, right - left); }
        int height() { return Math.max(1, bottom - top); }
    }
    private record Layout(int left, int top, int right, int bottom, Pane list, Pane detail) {
        int width() { return right - left; }
    }
}

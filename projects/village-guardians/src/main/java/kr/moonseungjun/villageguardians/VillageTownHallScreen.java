package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VillageTownHallScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0xCC05080D;
    private static final int PANEL = 0xFF0B1118;
    private static final int SURFACE = 0xFF121C26;
    private static final int SURFACE_2 = 0xFF192633;
    private static final int BORDER = 0xFF3E5365;
    private static final int TEXT = 0xFFF3F7FA;
    private static final int MUTED = 0xFFA7B4BE;
    private static final int ACCENT = 0xFF43D6BC;
    private static final int GOLD = 0xFFF2C25B;
    private static final int RED = 0xFFE36E76;
    private static final int BLUE = 0xFF78A7ED;
    private static final int PURPLE = 0xFFB38AE8;
    private static final int GREEN = 0xFF55D49B;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<RoleCard> roles = new ArrayList<>();
    private final List<FacilityCard> facilities = new ArrayList<>();
    private Tab tab = Tab.ROLES;
    private int selectedRole;
    private int selectedFacility;
    private int listScroll;

    public VillageTownHallScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        parsePayload();
        for (int i = 0; i < roles.size(); i++) if (roles.get(i).current()) selectedRole = i;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 4, layout.bottom(), ACCENT);
        renderHeader(graphics, mouseX, mouseY, layout);
        if (tab == Tab.ROLES) renderRoles(graphics, mouseX, mouseY, layout);
        else renderFacilities(graphics, mouseX, mouseY, layout);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        graphics.text(font, "마을 회관", layout.left() + 16, layout.top() + 12, TEXT, false);
        graphics.text(font, compact(payload.body(), Math.max(30, layout.width() / 8)),
                layout.left() + 16, layout.top() + 29, MUTED, false);
        int closeX = layout.right() - 34;
        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 10, 24, 24);
        graphics.fill(closeX, layout.top() + 10, closeX + 24, layout.top() + 34, close ? 0xFF6C3038 : SURFACE_2);
        graphics.centeredText(font, "×", closeX + 12, layout.top() + 18, close ? TEXT : MUTED);
        int tabY = layout.top() + 49;
        int tabWidth = Math.min(130, Math.max(92, (layout.width() - 40) / 3));
        drawTab(graphics, mouseX, mouseY, layout.left() + 16, tabY, tabWidth, "직업 배치", tab == Tab.ROLES);
        drawTab(graphics, mouseX, mouseY, layout.left() + 22 + tabWidth, tabY, tabWidth, "시설 관리", tab == Tab.FACILITIES);
    }

    private void renderRoles(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        Content content = content(layout);
        int listWidth = Math.max(112, Math.min(170, content.width() * 36 / 100));
        int listRight = content.left() + listWidth;
        graphics.fill(content.left(), content.top(), listRight, content.bottom(), 0xFF0A1017);
        graphics.fill(listRight + 6, content.top(), content.right(), content.bottom(), SURFACE);

        int gap = 6;
        int available = content.height() - 16;
        int cardHeight = Math.max(36, Math.min(52, (available - gap * Math.max(0, roles.size() - 1)) / Math.max(1, roles.size())));
        int y = content.top() + 8;
        for (int i = 0; i < roles.size(); i++) {
            RoleCard role = roles.get(i);
            int x = content.left() + 8;
            int w = listWidth - 16;
            boolean hovered = inside(mouseX, mouseY, x, y, w, cardHeight);
            boolean selected = selectedRole == i;
            int color = roleColor(role.id());
            graphics.fill(x - 1, y - 1, x + w + 1, y + cardHeight + 1,
                    selected ? color : hovered ? BORDER : 0xFF263440);
            graphics.fill(x, y, x + w, y + cardHeight, selected || hovered ? SURFACE_2 : SURFACE);
            graphics.fill(x, y, x + 4, y + cardHeight, color);
            graphics.text(font, compact(role.name(), Math.max(8, w / 7)), x + 12, y + 9, TEXT, false);
            graphics.text(font, role.current() ? "현재 직업" : "배치 가능", x + 12, y + cardHeight - 16,
                    role.current() ? ACCENT : MUTED, false);
            y += cardHeight + gap;
        }
        renderRoleDetail(graphics, mouseX, mouseY, content, listRight + 6);
    }

    private void renderRoleDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Content content, int detailLeft) {
        if (roles.isEmpty()) return;
        RoleCard role = roles.get(Math.max(0, Math.min(selectedRole, roles.size() - 1)));
        int left = detailLeft + 14;
        int right = content.right() - 14;
        int top = content.top() + 13;
        int color = roleColor(role.id());
        graphics.text(font, role.name(), left, top, color, false);
        graphics.text(font, role.current() ? "현재 배치" : "새 직업", right - font.width(role.current() ? "현재 배치" : "새 직업"), top,
                role.current() ? ACCENT : GOLD, false);
        int lineY = top + 22;
        lineY = drawSection(graphics, "역할", role.overview(), left, lineY, right - left, color, content.bottom() - 62);
        lineY = drawSection(graphics, "상시 효과", role.passive(), left, lineY + 4, right - left, color, content.bottom() - 62);
        lineY = drawSection(graphics, "기술 방향", role.active(), left, lineY + 4, right - left, color, content.bottom() - 62);
        if (content.bottom() - lineY > 34) {
            graphics.text(font, compact(role.recommended(), Math.max(16, (right - left) / 6)), left, lineY + 4, MUTED, false);
        }

        int buttonY = content.bottom() - 39;
        int buttonW = Math.min(190, Math.max(112, right - left));
        int buttonX = right - buttonW;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonW, 25);
        String label = role.current() ? "직업 성장·기술 장착" : role.name() + " 배치";
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonW + 1, buttonY + 26, hovered ? GOLD : color);
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + 25, hovered ? 0xFF342D1B : SURFACE_2);
        graphics.centeredText(font, compact(label, Math.max(12, buttonW / 7)), buttonX + buttonW / 2, buttonY + 8, TEXT);
        if (role.current()) {
            graphics.text(font, compact(role.loadout(), Math.max(16, (buttonX - left - 8) / 6)), left, buttonY + 8, ACCENT, false);
        }
    }

    private void renderFacilities(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        Content content = content(layout);
        int listWidth = Math.max(128, Math.min(190, content.width() * 40 / 100));
        int listRight = content.left() + listWidth;
        graphics.fill(content.left(), content.top(), listRight, content.bottom(), 0xFF0A1017);
        graphics.fill(listRight + 6, content.top(), content.right(), content.bottom(), SURFACE);

        int cardHeight = 48;
        int gap = 6;
        int total = facilities.size() * (cardHeight + gap) - gap;
        int visible = content.height() - 16;
        int maxScroll = Math.max(0, total - visible);
        listScroll = Math.max(0, Math.min(maxScroll, listScroll));
        graphics.enableScissor(content.left(), content.top(), listRight, content.bottom());
        int y = content.top() + 8 - listScroll;
        for (int i = 0; i < facilities.size(); i++) {
            FacilityCard facility = facilities.get(i);
            int x = content.left() + 8;
            int w = listWidth - 16;
            boolean hovered = inside(mouseX, mouseY, x, y, w, cardHeight);
            boolean selected = selectedFacility == i;
            int hpColor = facility.hp() <= 0 ? RED : facility.hp() < facility.maxHp() / 2 ? GOLD : ACCENT;
            graphics.fill(x - 1, y - 1, x + w + 1, y + cardHeight + 1,
                    selected ? hpColor : hovered ? BORDER : 0xFF263440);
            graphics.fill(x, y, x + w, y + cardHeight, selected || hovered ? SURFACE_2 : SURFACE);
            graphics.text(font, compact(facility.name(), Math.max(8, w / 7)), x + 9, y + 7, TEXT, false);
            graphics.text(font, compact(facility.level(), Math.max(7, w / 8)), x + 9, y + 21, MUTED, false);
            int barLeft = x + 9;
            int barRight = x + w - 9;
            int barY = y + 36;
            graphics.fill(barLeft, barY, barRight, barY + 5, 0xFF03070A);
            int fill = facility.maxHp() <= 0 ? 0 : Math.round((barRight - barLeft - 2) * facility.hp() / (float) facility.maxHp());
            graphics.fill(barLeft + 1, barY + 1, barLeft + 1 + fill, barY + 4, hpColor);
            y += cardHeight + gap;
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int trackX = listRight - 4;
            int thumbH = Math.max(18, visible * visible / Math.max(visible, total));
            int thumbY = content.top() + (visible - thumbH) * listScroll / maxScroll;
            graphics.fill(trackX, content.top(), trackX + 3, content.bottom(), 0xFF05080B);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ACCENT);
        }
        renderFacilityDetail(graphics, mouseX, mouseY, content, listRight + 6);
    }

    private void renderFacilityDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Content content, int detailLeft) {
        if (facilities.isEmpty()) return;
        FacilityCard facility = facilities.get(Math.max(0, Math.min(selectedFacility, facilities.size() - 1)));
        int left = detailLeft + 14;
        int right = content.right() - 14;
        int top = content.top() + 14;
        int hpColor = facility.hp() <= 0 ? RED : facility.hp() < facility.maxHp() / 2 ? GOLD : ACCENT;
        graphics.text(font, facility.name(), left, top, TEXT, false);
        graphics.text(font, facility.level(), right - font.width(facility.level()), top, MUTED, false);
        graphics.text(font, "내구도 " + facility.hp() + " / " + facility.maxHp(), left, top + 24, hpColor, false);
        int barTop = top + 42;
        graphics.fill(left, barTop, right, barTop + 7, 0xFF03070A);
        int fill = facility.maxHp() <= 0 ? 0 : Math.round((right - left - 2) * facility.hp() / (float) facility.maxHp());
        graphics.fill(left + 1, barTop + 1, left + 1 + fill, barTop + 6, hpColor);
        graphics.text(font, "현재 효과", left, barTop + 19, ACCENT, false);
        List<FormattedCharSequence> lines = font.split(Component.literal(facility.effect()), Math.max(80, right - left));
        int y = barTop + 36;
        for (FormattedCharSequence line : lines) {
            if (y > content.bottom() - 55) break;
            graphics.text(font, line, left, y, MUTED, false);
            y += 12;
        }
        int buttonW = Math.min(170, Math.max(110, right - left));
        int buttonX = right - buttonW;
        int buttonY = content.bottom() - 39;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonW, 25);
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonW + 1, buttonY + 26, hovered ? GOLD : ACCENT);
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + 25, hovered ? 0xFF342D1B : SURFACE_2);
        graphics.centeredText(font, "수리·강화 관리", buttonX + buttonW / 2, buttonY + 8, TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 34, layout.top() + 10, 24, 24)) { onClose(); return true; }
        int tabY = layout.top() + 49;
        int tabWidth = Math.min(130, Math.max(92, (layout.width() - 40) / 3));
        if (inside(click.x(), click.y(), layout.left() + 16, tabY, tabWidth, 24)) { tab = Tab.ROLES; listScroll = 0; return true; }
        if (inside(click.x(), click.y(), layout.left() + 22 + tabWidth, tabY, tabWidth, 24)) { tab = Tab.FACILITIES; listScroll = 0; return true; }
        Content content = content(layout);
        if (tab == Tab.ROLES) return clickRoles(click, content) || super.mouseClicked(click, doubled);
        return clickFacilities(click, content) || super.mouseClicked(click, doubled);
    }

    private boolean clickRoles(MouseButtonEvent click, Content content) {
        int listWidth = Math.max(112, Math.min(170, content.width() * 36 / 100));
        int gap = 6;
        int cardHeight = Math.max(36, Math.min(52,
                (content.height() - 16 - gap * Math.max(0, roles.size() - 1)) / Math.max(1, roles.size())));
        int y = content.top() + 8;
        for (int i = 0; i < roles.size(); i++) {
            if (inside(click.x(), click.y(), content.left() + 8, y, listWidth - 16, cardHeight)) {
                selectedRole = i;
                return true;
            }
            y += cardHeight + gap;
        }
        if (roles.isEmpty()) return false;
        RoleCard role = roles.get(Math.max(0, Math.min(selectedRole, roles.size() - 1)));
        int detailLeft = content.left() + listWidth + 20;
        int right = content.right() - 14;
        int buttonW = Math.min(190, Math.max(112, right - detailLeft));
        int buttonX = right - buttonW;
        int buttonY = content.bottom() - 39;
        if (inside(click.x(), click.y(), buttonX, buttonY, buttonW, 25)) {
            send(role.current() ? "open_role_progress:" + role.id() : role.action());
            return true;
        }
        return false;
    }

    private boolean clickFacilities(MouseButtonEvent click, Content content) {
        int listWidth = Math.max(128, Math.min(190, content.width() * 40 / 100));
        int cardHeight = 48;
        int y = content.top() + 8 - listScroll;
        for (int i = 0; i < facilities.size(); i++) {
            if (inside(click.x(), click.y(), content.left() + 8, y, listWidth - 16, cardHeight)) {
                selectedFacility = i;
                return true;
            }
            y += cardHeight + 6;
        }
        if (facilities.isEmpty()) return false;
        int detailLeft = content.left() + listWidth + 20;
        int right = content.right() - 14;
        int buttonW = Math.min(170, Math.max(110, right - detailLeft));
        int buttonX = right - buttonW;
        int buttonY = content.bottom() - 39;
        if (inside(click.x(), click.y(), buttonX, buttonY, buttonW, 25)) {
            send(facilities.get(Math.max(0, Math.min(selectedFacility, facilities.size() - 1))).action());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (tab == Tab.FACILITIES) {
            Content content = content(layout());
            int listWidth = Math.max(128, Math.min(190, content.width() * 40 / 100));
            if (inside(mouseX, mouseY, content.left(), content.top(), listWidth, content.height())) {
                listScroll = Math.max(0, listScroll - (int) Math.round(vertical * 32));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private int drawSection(GuiGraphicsExtractor graphics, String label, String text,
                            int x, int y, int width, int color, int limit) {
        if (y > limit) return y;
        graphics.text(font, label, x, y, color, false);
        int lineY = y + 14;
        for (FormattedCharSequence line : font.split(Component.literal(text), Math.max(80, width))) {
            if (lineY > limit) break;
            graphics.text(font, line, x, lineY, MUTED, false);
            lineY += 11;
        }
        return lineY;
    }

    private void parsePayload() {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String[] p = labels[i].split("\\|", -1);
            if (p.length >= 8 && "role".equals(p[0])) {
                roles.add(new RoleCard(actions[i], p[1], p[2], p[3], p[4], p[5], p[6], "current".equals(p[7]), p.length > 8 ? p[8] : ""));
            } else if (p.length >= 7 && "facility".equals(p[0])) {
                facilities.add(new FacilityCard(actions[i], p[1], p[2], p[3], parseInt(p[4]), parseInt(p[5]), p[6]));
            }
        }
    }

    private int roleColor(String id) {
        return switch (id) {
            case "vanguard" -> RED;
            case "ranger" -> GOLD;
            case "arcanist" -> PURPLE;
            case "luminar" -> GREEN;
            case "warden" -> BLUE;
            default -> ACCENT;
        };
    }

    private void drawTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, String text, boolean active) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, 24);
        graphics.fill(x, y, x + width, y + 24, active ? ACCENT : hovered ? SURFACE_2 : SURFACE);
        graphics.centeredText(font, text, x + width / 2, y + 8, active ? 0xFF07100F : TEXT);
    }

    private Layout layout() {
        int panelWidth = Math.max(330, Math.min(760, width - 12));
        int panelHeight = Math.max(250, Math.min(520, height - 10));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Content content(Layout layout) {
        return new Content(layout.left() + 14, layout.top() + 82, layout.right() - 14, layout.bottom() - 14);
    }

    private int parseInt(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return 0; } }
    private String compact(String value, int max) { String n = value.replace('\n', ' '); return n.length() <= max ? n : n.substring(0, Math.max(1, max - 1)) + "…"; }
    private void send(String action) { ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Tab { ROLES, FACILITIES }
    private record Layout(int left, int top, int width, int height) { int right() { return left + width; } int bottom() { return top + height; } }
    private record Content(int left, int top, int right, int bottom) { int width() { return right - left; } int height() { return bottom - top; } }
    private record RoleCard(String action, String id, String name, String overview, String passive, String active, String recommended, boolean current, String loadout) {}
    private record FacilityCard(String action, String id, String name, String level, int hp, int maxHp, String effect) {}
}

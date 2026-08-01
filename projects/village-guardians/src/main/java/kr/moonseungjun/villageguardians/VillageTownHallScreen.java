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
    private static final int OVERLAY = 0xD005080D;
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
    private static final int CARD_HEIGHT = 58;
    private static final int CARD_GAP = 7;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<RoleCard> roles = new ArrayList<>();
    private final List<FacilityCard> facilities = new ArrayList<>();
    private Tab tab = Tab.ROLES;
    private int selectedRole;
    private int selectedFacility;
    private int roleListScroll;
    private int roleDetailScroll;
    private int facilityListScroll;
    private int facilityDetailScroll;

    public VillageTownHallScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        parsePayload();
        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i).current()) {
                selectedRole = i;
                break;
            }
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
        int left = layout.left() + 18;
        int closeX = layout.right() - 37;
        graphics.text(font, "마을 회관", left, layout.top() + 12, TEXT, false);
        int summaryRight = closeX - 12;
        List<FormattedCharSequence> summary = font.split(Component.literal(payload.body()),
                Math.max(120, summaryRight - left));
        int y = layout.top() + 30;
        for (int i = 0; i < Math.min(2, summary.size()); i++) {
            graphics.text(font, summary.get(i), left, y, MUTED, false);
            y += 11;
        }

        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 10, 27, 27);
        graphics.fill(closeX, layout.top() + 10, closeX + 27, layout.top() + 37,
                close ? 0xFF6C3038 : SURFACE_2);
        graphics.centeredText(font, "×", closeX + 13, layout.top() + 19, close ? TEXT : MUTED);

        int tabY = layout.top() + 58;
        int tabWidth = Math.min(158, Math.max(112, (layout.width() - 58) / 3));
        drawTab(graphics, mouseX, mouseY, layout.left() + 18, tabY, tabWidth,
                "직업 배치", tab == Tab.ROLES);
        drawTab(graphics, mouseX, mouseY, layout.left() + 26 + tabWidth, tabY, tabWidth,
                "시설·포탑 관리", tab == Tab.FACILITIES);
    }

    private void renderRoles(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        Split split = split(layout);
        drawPaneBackgrounds(graphics, split);
        renderRoleList(graphics, mouseX, mouseY, split.list());
        renderRoleDetail(graphics, mouseX, mouseY, split.detail());
    }

    private void renderRoleList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        int total = listContentHeight(roles.size());
        int visible = pane.height() - 16;
        int maxScroll = Math.max(0, total - visible);
        roleListScroll = clamp(roleListScroll, 0, maxScroll);
        graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom());
        int y = pane.top() + 8 - roleListScroll;
        for (int i = 0; i < roles.size(); i++) {
            RoleCard role = roles.get(i);
            int x = pane.left() + 9;
            int w = pane.width() - 19;
            boolean hovered = inside(mouseX, mouseY, x, y, w, CARD_HEIGHT);
            boolean selected = selectedRole == i;
            int color = roleColor(role.id());
            graphics.fill(x - 1, y - 1, x + w + 1, y + CARD_HEIGHT + 1,
                    selected ? color : hovered ? BORDER : 0xFF263440);
            graphics.fill(x, y, x + w, y + CARD_HEIGHT,
                    selected || hovered ? SURFACE_2 : SURFACE);
            graphics.fill(x, y, x + 5, y + CARD_HEIGHT, color);
            graphics.text(font, compact(role.name(), Math.max(10, w / 7)), x + 15, y + 12, TEXT, false);
            graphics.text(font, role.current() ? "현재 직업" : "배치 가능",
                    x + 15, y + 34, role.current() ? ACCENT : MUTED, false);
            y += CARD_HEIGHT + CARD_GAP;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, pane.right() - 5, pane.top() + 5, pane.bottom() - 5,
                roleListScroll, maxScroll, visible, total, ACCENT);
    }

    private void renderRoleDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        if (roles.isEmpty()) return;
        RoleCard role = roles.get(clamp(selectedRole, 0, roles.size() - 1));
        int color = roleColor(role.id());
        int actionHeight = 54;
        int scrollBottom = pane.bottom() - actionHeight;
        graphics.fill(pane.left(), pane.top(), pane.left() + 4, pane.bottom(), color);
        graphics.fill(pane.left() + 4, scrollBottom, pane.right(), pane.bottom(), 0xFF0E171F);
        graphics.fill(pane.left() + 4, scrollBottom, pane.right(), scrollBottom + 1, BORDER);

        int textLeft = pane.left() + 18;
        int textRight = pane.right() - 16;
        int textWidth = Math.max(100, textRight - textLeft);
        List<DetailLine> lines = roleDetailLines(role, textWidth, color);
        int contentHeight = detailHeight(lines);
        int visible = Math.max(1, scrollBottom - pane.top() - 18);
        int maxScroll = Math.max(0, contentHeight - visible);
        roleDetailScroll = clamp(roleDetailScroll, 0, maxScroll);

        graphics.enableScissor(pane.left() + 4, pane.top(), pane.right(), scrollBottom);
        int y = pane.top() + 14 - roleDetailScroll;
        for (DetailLine line : lines) {
            if (line.gapBefore() > 0) y += line.gapBefore();
            if (y + 10 >= pane.top() && y <= scrollBottom) {
                graphics.text(font, line.text(), textLeft, y, line.color(), false);
            }
            y += line.height();
        }
        graphics.disableScissor();
        drawScrollbar(graphics, pane.right() - 5, pane.top() + 5, scrollBottom - 5,
                roleDetailScroll, maxScroll, visible, contentHeight, color);

        String state = role.current() ? "현재 배치" : "새 직업";
        graphics.text(font, state, textLeft, scrollBottom + 12,
                role.current() ? ACCENT : GOLD, false);
        int buttonX = Math.max(textLeft + 110, pane.right() - Math.min(260, pane.width() / 2) - 14);
        int buttonW = pane.right() - buttonX - 14;
        int buttonY = scrollBottom + 10;
        boolean active = !role.current();
        boolean hovered = active && inside(mouseX, mouseY, buttonX, buttonY, buttonW, 31);
        int border = active ? (hovered ? GOLD : color) : BORDER;
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonW + 1, buttonY + 32, border);
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + 31,
                hovered ? 0xFF342D1B : active ? SURFACE_2 : 0xFF151C23);
        String label = active ? role.name() + " 배치" : "성장·기술은 연구소에서 관리";
        graphics.centeredText(font, compact(label, Math.max(14, buttonW / 7)),
                buttonX + buttonW / 2, buttonY + 10, active ? TEXT : MUTED);
    }

    private List<DetailLine> roleDetailLines(RoleCard role, int width, int color) {
        List<DetailLine> result = new ArrayList<>();
        result.add(new DetailLine(FormattedCharSequence.forward(role.name(), net.minecraft.network.chat.Style.EMPTY), color, 15, 0));
        addSection(result, "역할", role.overview(), width, color);
        addSection(result, "상시 효과", role.passive(), width, color);
        addSection(result, "기술 성향", role.active(), width, color);
        addSection(result, "추천 위치", role.recommended(), width, BLUE);
        if (role.current() && !role.loadout().isBlank()) {
            addSection(result, "현재 장착", role.loadout(), width, ACCENT);
        }
        return result;
    }

    private void renderFacilities(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        Split split = split(layout);
        drawPaneBackgrounds(graphics, split);
        renderFacilityList(graphics, mouseX, mouseY, split.list());
        renderFacilityDetail(graphics, mouseX, mouseY, split.detail());
    }

    private void renderFacilityList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        int total = listContentHeight(facilities.size());
        int visible = pane.height() - 16;
        int maxScroll = Math.max(0, total - visible);
        facilityListScroll = clamp(facilityListScroll, 0, maxScroll);
        graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom());
        int y = pane.top() + 8 - facilityListScroll;
        for (int i = 0; i < facilities.size(); i++) {
            FacilityCard facility = facilities.get(i);
            int x = pane.left() + 9;
            int w = pane.width() - 19;
            boolean hovered = inside(mouseX, mouseY, x, y, w, CARD_HEIGHT);
            boolean selected = selectedFacility == i;
            int stateColor = facility.hp() <= 0 ? RED : facility.hp() * 2 < facility.maxHp() ? GOLD : ACCENT;
            graphics.fill(x - 1, y - 1, x + w + 1, y + CARD_HEIGHT + 1,
                    selected ? stateColor : hovered ? BORDER : 0xFF263440);
            graphics.fill(x, y, x + w, y + CARD_HEIGHT,
                    selected || hovered ? SURFACE_2 : SURFACE);
            graphics.fill(x, y, x + 5, y + CARD_HEIGHT, stateColor);
            graphics.text(font, compact(facility.name(), Math.max(10, w / 7)), x + 15, y + 9, TEXT, false);
            graphics.text(font, compact(facility.level(), Math.max(9, w / 8)), x + 15, y + 25, MUTED, false);
            int barLeft = x + 15;
            int barRight = x + w - 12;
            int barY = y + 43;
            graphics.fill(barLeft, barY, barRight, barY + 5, 0xFF03070A);
            int fill = facility.maxHp() <= 0 ? 0
                    : Math.round((barRight - barLeft - 2) * facility.hp() / (float) facility.maxHp());
            graphics.fill(barLeft + 1, barY + 1, barLeft + 1 + Math.max(0, fill), barY + 4, stateColor);
            y += CARD_HEIGHT + CARD_GAP;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, pane.right() - 5, pane.top() + 5, pane.bottom() - 5,
                facilityListScroll, maxScroll, visible, total, ACCENT);
    }

    private void renderFacilityDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        if (facilities.isEmpty()) return;
        FacilityCard facility = facilities.get(clamp(selectedFacility, 0, facilities.size() - 1));
        int stateColor = facility.hp() <= 0 ? RED : facility.hp() * 2 < facility.maxHp() ? GOLD : ACCENT;
        int actionHeight = 54;
        int scrollBottom = pane.bottom() - actionHeight;
        graphics.fill(pane.left(), pane.top(), pane.left() + 4, pane.bottom(), stateColor);
        graphics.fill(pane.left() + 4, scrollBottom, pane.right(), pane.bottom(), 0xFF0E171F);
        graphics.fill(pane.left() + 4, scrollBottom, pane.right(), scrollBottom + 1, BORDER);

        int textLeft = pane.left() + 18;
        int textRight = pane.right() - 16;
        int textWidth = Math.max(100, textRight - textLeft);
        List<DetailLine> lines = facilityDetailLines(facility, textWidth, stateColor);
        int contentHeight = detailHeight(lines);
        int visible = Math.max(1, scrollBottom - pane.top() - 18);
        int maxScroll = Math.max(0, contentHeight - visible);
        facilityDetailScroll = clamp(facilityDetailScroll, 0, maxScroll);

        graphics.enableScissor(pane.left() + 4, pane.top(), pane.right(), scrollBottom);
        int y = pane.top() + 14 - facilityDetailScroll;
        for (DetailLine line : lines) {
            if (line.gapBefore() > 0) y += line.gapBefore();
            if (y + 10 >= pane.top() && y <= scrollBottom) {
                graphics.text(font, line.text(), textLeft, y, line.color(), false);
            }
            y += line.height();
        }
        graphics.disableScissor();
        drawScrollbar(graphics, pane.right() - 5, pane.top() + 5, scrollBottom - 5,
                facilityDetailScroll, maxScroll, visible, contentHeight, stateColor);

        int buttonX = textLeft;
        int buttonY = scrollBottom + 10;
        int buttonW = textRight - textLeft;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonW, 31);
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonW + 1, buttonY + 32,
                hovered ? GOLD : stateColor);
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + 31,
                hovered ? 0xFF342D1B : SURFACE_2);
        graphics.centeredText(font, facilityActionLabel(facility),
                buttonX + buttonW / 2, buttonY + 10, TEXT);
    }

    private List<DetailLine> facilityDetailLines(FacilityCard facility, int width, int color) {
        List<DetailLine> result = new ArrayList<>();
        result.add(new DetailLine(FormattedCharSequence.forward(facility.name(), net.minecraft.network.chat.Style.EMPTY), TEXT, 15, 0));
        result.add(new DetailLine(FormattedCharSequence.forward(facility.level(), net.minecraft.network.chat.Style.EMPTY), MUTED, 13, 2));
        String health = "내구도 " + facility.hp() + " / " + facility.maxHp();
        result.add(new DetailLine(FormattedCharSequence.forward(health, net.minecraft.network.chat.Style.EMPTY), color, 14, 4));
        addSection(result, "현재 효과", facility.effect(), width, ACCENT);
        String guide = switch (facility.id()) {
            case "town_hall" -> "개인 수호 주화를 공동 보급품으로 바꿔 시설 수리와 강화에 사용합니다.";
            case "walls" -> "회관에서 포탑 해금 상태와 성벽 수리·강화를 함께 지휘합니다.";
            default -> "공동 보급품으로 수리·강화하며, 보급품은 회관에서 개인 주화로 조달할 수 있습니다.";
        };
        addSection(result, "관리 안내", guide, width, GOLD);
        return result;
    }

    private void addSection(List<DetailLine> target, String heading, String body, int width, int headingColor) {
        target.add(new DetailLine(FormattedCharSequence.forward(heading, net.minecraft.network.chat.Style.EMPTY), headingColor, 14, 8));
        for (FormattedCharSequence line : font.split(Component.literal(body), width)) {
            target.add(new DetailLine(line, MUTED, 12, 0));
        }
    }

    private int detailHeight(List<DetailLine> lines) {
        int height = 0;
        for (DetailLine line : lines) height += line.gapBefore() + line.height();
        return height + 8;
    }

    private String facilityActionLabel(FacilityCard facility) {
        return switch (facility.id()) {
            case "town_hall" -> "공동 보급품 조달";
            case "walls" -> "포탑 지휘·성벽 관리";
            default -> "수리·강화 관리";
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 37, layout.top() + 10, 27, 27)) {
            onClose();
            return true;
        }
        int tabY = layout.top() + 58;
        int tabWidth = Math.min(158, Math.max(112, (layout.width() - 58) / 3));
        if (inside(click.x(), click.y(), layout.left() + 18, tabY, tabWidth, 25)) {
            tab = Tab.ROLES;
            return true;
        }
        if (inside(click.x(), click.y(), layout.left() + 26 + tabWidth, tabY, tabWidth, 25)) {
            tab = Tab.FACILITIES;
            return true;
        }

        Split split = split(layout);
        if (tab == Tab.ROLES) return clickRoles(click, split) || super.mouseClicked(click, doubled);
        return clickFacilities(click, split) || super.mouseClicked(click, doubled);
    }

    private boolean clickRoles(MouseButtonEvent click, Split split) {
        Pane list = split.list();
        int y = list.top() + 8 - roleListScroll;
        for (int i = 0; i < roles.size(); i++) {
            if (inside(click.x(), click.y(), list.left() + 9, y, list.width() - 19, CARD_HEIGHT)) {
                selectedRole = i;
                roleDetailScroll = 0;
                return true;
            }
            y += CARD_HEIGHT + CARD_GAP;
        }
        if (roles.isEmpty()) return false;
        RoleCard role = roles.get(clamp(selectedRole, 0, roles.size() - 1));
        Pane detail = split.detail();
        int scrollBottom = detail.bottom() - 54;
        int textLeft = detail.left() + 18;
        int textRight = detail.right() - 16;
        int buttonX = Math.max(textLeft + 110, detail.right() - Math.min(260, detail.width() / 2) - 14);
        int buttonW = detail.right() - buttonX - 14;
        if (!role.current() && inside(click.x(), click.y(), buttonX, scrollBottom + 10, buttonW, 31)) {
            send(role.action());
            return true;
        }
        return false;
    }

    private boolean clickFacilities(MouseButtonEvent click, Split split) {
        Pane list = split.list();
        int y = list.top() + 8 - facilityListScroll;
        for (int i = 0; i < facilities.size(); i++) {
            if (inside(click.x(), click.y(), list.left() + 9, y, list.width() - 19, CARD_HEIGHT)) {
                selectedFacility = i;
                facilityDetailScroll = 0;
                return true;
            }
            y += CARD_HEIGHT + CARD_GAP;
        }
        if (facilities.isEmpty()) return false;
        Pane detail = split.detail();
        int buttonY = detail.bottom() - 44;
        if (inside(click.x(), click.y(), detail.left() + 18, buttonY,
                detail.width() - 34, 31)) {
            send(facilities.get(clamp(selectedFacility, 0, facilities.size() - 1)).action());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Split split = split(layout());
        int amount = (int) Math.round(vertical * 38);
        if (inside(mouseX, mouseY, split.list().left(), split.list().top(),
                split.list().width(), split.list().height())) {
            if (tab == Tab.ROLES) roleListScroll = Math.max(0, roleListScroll - amount);
            else facilityListScroll = Math.max(0, facilityListScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, split.detail().left(), split.detail().top(),
                split.detail().width(), split.detail().height() - 54)) {
            if (tab == Tab.ROLES) roleDetailScroll = Math.max(0, roleDetailScroll - amount);
            else facilityDetailScroll = Math.max(0, facilityDetailScroll - amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void drawPaneBackgrounds(GuiGraphicsExtractor graphics, Split split) {
        graphics.fill(split.list().left(), split.list().top(), split.list().right(), split.list().bottom(), 0xFF0A1017);
        graphics.fill(split.detail().left(), split.detail().top(), split.detail().right(), split.detail().bottom(), SURFACE);
    }

    private void drawTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                         int x, int y, int width, String text, boolean active) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, 25);
        graphics.fill(x, y, x + width, y + 25, active ? ACCENT : hovered ? SURFACE_2 : SURFACE);
        graphics.centeredText(font, text, x + width / 2, y + 8, active ? 0xFF07100F : TEXT);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int scroll, int maxScroll, int visible, int content, int color) {
        if (maxScroll <= 0 || content <= visible) return;
        int trackHeight = Math.max(1, bottom - top);
        int thumb = Math.max(18, trackHeight * visible / Math.max(visible, content));
        int y = top + (trackHeight - thumb) * clamp(scroll, 0, maxScroll) / maxScroll;
        graphics.fill(x, top, x + 3, bottom, 0xFF05080B);
        graphics.fill(x, y, x + 3, y + thumb, color);
    }

    private int listContentHeight(int count) {
        return count <= 0 ? 0 : count * CARD_HEIGHT + Math.max(0, count - 1) * CARD_GAP;
    }

    private void parsePayload() {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String[] p = labels[i].split("\\|", -1);
            if (p.length >= 8 && "role".equals(p[0])) {
                roles.add(new RoleCard(actions[i], p[1], p[2], p[3], p[4], p[5], p[6],
                        "current".equals(p[7]), p.length > 8 ? p[8] : ""));
            } else if (p.length >= 7 && "facility".equals(p[0])) {
                facilities.add(new FacilityCard(actions[i], p[1], p[2], p[3],
                        parseInt(p[4]), parseInt(p[5]), p[6]));
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

    private Layout layout() {
        int margin = 5;
        int panelWidth = Math.max(360, Math.min(980, width - margin * 2));
        int panelHeight = Math.max(280, Math.min(640, height - margin * 2));
        panelWidth = Math.min(panelWidth, width - 2);
        panelHeight = Math.min(panelHeight, height - 2);
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Split split(Layout layout) {
        int contentLeft = layout.left() + 14;
        int contentTop = layout.top() + 91;
        int contentRight = layout.right() - 14;
        int contentBottom = layout.bottom() - 14;
        int totalWidth = contentRight - contentLeft;
        int listWidth = Math.max(155, Math.min(250, totalWidth * 32 / 100));
        if (totalWidth < 520) listWidth = Math.max(130, totalWidth * 40 / 100);
        Pane list = new Pane(contentLeft, contentTop, contentLeft + listWidth, contentBottom);
        Pane detail = new Pane(contentLeft + listWidth + 8, contentTop, contentRight, contentBottom);
        return new Split(list, detail);
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String compact(String value, int max) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        return normalized.length() <= max ? normalized
                : normalized.substring(0, Math.max(1, max - 1)) + "…";
    }

    private void send(String action) {
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Tab { ROLES, FACILITIES }
    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    private record Split(Pane list, Pane detail) {}
    private record DetailLine(FormattedCharSequence text, int color, int height, int gapBefore) {}
    private record RoleCard(String action, String id, String name, String overview, String passive,
                            String active, String recommended, boolean current, String loadout) {}
    private record FacilityCard(String action, String id, String name, String level,
                                int hp, int maxHp, String effect) {}
}

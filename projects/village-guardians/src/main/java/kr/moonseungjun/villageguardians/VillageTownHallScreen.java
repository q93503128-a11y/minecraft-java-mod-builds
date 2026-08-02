package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Dense town hall interface with a narrow selector and a wide explanation pane. */
public final class VillageTownHallScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x65000000;
    private static final int PANEL = 0xFFE4D8BF;
    private static final int SURFACE = 0xFFF1E9D7;
    private static final int SURFACE_ALT = 0xFFD8CBB1;
    private static final int SELECTED = 0xFFE1C98F;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int TEAL = 0xFF267E73;
    private static final int GOLD = 0xFFB87B20;
    private static final int RED = 0xFFAA4545;
    private static final int BLUE = 0xFF466FA8;
    private static final int PURPLE = 0xFF74509A;
    private static final int GREEN = 0xFF39764F;
    private static final int CARD_HEIGHT = 30;
    private static final int CARD_GAP = 3;
    private static final int ACTION_HEIGHT = 20;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<RoleCard> roles = new ArrayList<>();
    private final List<FacilityCard> facilities = new ArrayList<>();
    private Tab tab = Tab.ROLES;
    private int selectedRole;
    private int selectedFacility;
    private int listScroll;
    private int detailScroll;

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
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), tab.accent());
        renderHeader(graphics, mouseX, mouseY, layout);

        Split split = split(layout);
        panel(graphics, split.list(), SURFACE_ALT);
        panel(graphics, split.detail(), SURFACE);
        if (tab == Tab.ROLES) {
            renderRoleList(graphics, mouseX, mouseY, split.list());
            renderRoleDetail(graphics, mouseX, mouseY, split.detail());
        } else {
            renderFacilityList(graphics, mouseX, mouseY, split.list());
            renderFacilityDetail(graphics, mouseX, mouseY, split.detail());
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 18;
        int closeX = layout.right() - 36;
        graphics.text(font, "마을 회관", left, layout.top() + 10, TEXT, false);
        List<FormattedCharSequence> summary = font.split(Component.literal(plain(payload.body())),
                Math.max(150, closeX - left - 8));
        if (!summary.isEmpty()) graphics.text(font, summary.getFirst(), left, layout.top() + 27, MUTED, false);

        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 7, 27, 27);
        graphics.fill(closeX, layout.top() + 7, closeX + 27, layout.top() + 34,
                close ? 0xFFE2AAAA : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 13, layout.top() + 16, close ? RED : TEXT);

        int tabY = layout.top() + 40;
        int gap = 5;
        int total = layout.width() - 32;
        int tabWidth = (total - gap * 2) / 3;
        int x = layout.left() + 16;
        for (Tab value : Tab.values()) {
            drawTab(graphics, mouseX, mouseY, x, tabY, tabWidth, value);
            x += tabWidth + gap;
        }
    }

    private void drawTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                         int x, int y, int width, Tab value) {
        boolean active = tab == value;
        boolean hovered = inside(mouseX, mouseY, x, y, width, 18);
        graphics.fill(x - 1, y - 1, x + width + 1, y + 19, active ? value.accent() : BORDER);
        graphics.fill(x, y, x + width, y + 18, active ? SELECTED : hovered ? SURFACE : SURFACE_ALT);
        graphics.centeredText(font, value.displayName(), x + width / 2, y + 5, active ? TEXT : MUTED);
    }

    private void renderRoleList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        int content = listHeight(roles.size());
        int viewport = Math.max(1, pane.height() - 12);
        int maximum = Math.max(0, content - viewport);
        listScroll = clamp(listScroll, 0, maximum);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, pane.bottom() - 2);
        int y = pane.top() + 6 - listScroll;
        for (int i = 0; i < roles.size(); i++) {
            RoleCard role = roles.get(i);
            int x = pane.left() + 6;
            int w = pane.width() - 15;
            boolean hovered = inside(mouseX, mouseY, x, y, w, CARD_HEIGHT);
            boolean selected = selectedRole == i;
            int accent = roleColor(role.id());
            card(graphics, x, y, w, selected, hovered, accent);
            graphics.text(font, compact(role.name(), w - 22), x + 11, y + 7, TEXT, false);
            graphics.text(font, role.current() ? "현재" : "선택",
                    x + 11, y + 18, role.current() ? TEAL : MUTED, false);
            y += CARD_HEIGHT + CARD_GAP;
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 5, pane.top() + 5, pane.bottom() - 5,
                listScroll, maximum, viewport, content, TEAL);
    }

    private void renderFacilityList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        int content = listHeight(facilities.size());
        int viewport = Math.max(1, pane.height() - 12);
        int maximum = Math.max(0, content - viewport);
        listScroll = clamp(listScroll, 0, maximum);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, pane.bottom() - 2);
        int y = pane.top() + 6 - listScroll;
        for (int i = 0; i < facilities.size(); i++) {
            FacilityCard facility = facilities.get(i);
            int x = pane.left() + 6;
            int w = pane.width() - 15;
            boolean hovered = inside(mouseX, mouseY, x, y, w, CARD_HEIGHT);
            boolean selected = selectedFacility == i;
            int accent = facility.hp() <= 0 ? RED : facility.hp() < facility.maxHp() ? GOLD : TEAL;
            card(graphics, x, y, w, selected, hovered, accent);
            graphics.text(font, compact(facility.name(), w - 22), x + 11, y + 6, TEXT, false);
            String state = tab == Tab.REPAIR
                    ? facility.hp() + "/" + facility.maxHp()
                    : facility.level();
            graphics.text(font, compact(state, w - 22), x + 11, y + 17, MUTED, false);
            int barLeft = x + 11;
            int barRight = x + w - 8;
            graphics.fill(barLeft, y + 25, barRight, y + 28, 0xFFC6B79D);
            int fill = facility.maxHp() <= 0 ? 0
                    : Math.round((barRight - barLeft) * facility.hp() / (float) facility.maxHp());
            graphics.fill(barLeft, y + 25, barLeft + Math.max(0, fill), y + 28, accent);
            y += CARD_HEIGHT + CARD_GAP;
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 5, pane.top() + 5, pane.bottom() - 5,
                listScroll, maximum, viewport, content, tab.accent());
    }

    private void renderRoleDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        if (roles.isEmpty()) return;
        RoleCard role = roles.get(clamp(selectedRole, 0, roles.size() - 1));
        List<Section> sections = new ArrayList<>();
        sections.add(new Section(role.name(), "", roleColor(role.id()), true));
        sections.add(new Section("역할", role.overview(), TEXT, false));
        sections.add(new Section("상시 효과", role.passive(), TEAL, false));
        sections.add(new Section("전투 방식", role.active(), GOLD, false));
        sections.add(new Section("추천 위치", role.recommended(), BLUE, false));
        if (role.current() && !role.loadout().isBlank()) {
            sections.add(new Section("현재 장착 기술", role.loadout(), GREEN, false));
        }
        List<ActionButton> buttons = role.current()
                ? List.of()
                : List.of(new ActionButton(role.action(), role.name() + " 배치", role.name()));
        renderDetail(graphics, mouseX, mouseY, pane, sections, buttons, roleColor(role.id()));
    }

    private void renderFacilityDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        if (facilities.isEmpty()) return;
        FacilityCard facility = facilities.get(clamp(selectedFacility, 0, facilities.size() - 1));
        int stateColor = facility.hp() <= 0 ? RED : facility.hp() < facility.maxHp() ? GOLD : TEAL;
        List<Section> sections = new ArrayList<>();
        sections.add(new Section(facility.name(), "", stateColor, true));
        sections.add(new Section("시설 상태",
                facility.level() + " · 내구도 " + facility.hp() + " / " + facility.maxHp(), TEXT, false));

        if (tab == Tab.REPAIR) {
            sections.add(new Section("수리 결과",
                    facility.hp() >= facility.maxHp()
                            ? "현재 내구도가 최대입니다."
                            : facility.maxHp() + " / " + facility.maxHp() + "로 완전히 복구", TEAL, false));
            sections.add(new Section("수리 비용",
                    facility.repairCost() <= 0 ? "소모 없음" : "공동 보급품 " + facility.repairCost(), GOLD, false));
        } else {
            sections.add(new Section("현재 단계", facility.effect(), TEAL, false));
            if (facility.canUpgrade()) {
                sections.add(new Section("다음 단계 변화", facility.nextEffect(), BLUE, false));
                sections.add(new Section("강화 비용", "공동 보급품 " + facility.upgradeCost(), GOLD, false));
            } else if (facility.hp() <= 0) {
                sections.add(new Section("강화 불가", "시설 수리 탭에서 먼저 복구해야 합니다.", RED, false));
            } else if (!facility.id().equals("town_hall")) {
                sections.add(new Section("강화 완료", "최고 강화 단계에 도달했습니다.", GREEN, false));
            }
            if (facility.id().equals("walls")) {
                sections.add(new Section("방어망 관리", "포탑 설치와 전문화는 별도 버튼에서 관리합니다.", GOLD, false));
            } else if (facility.id().equals("town_hall")) {
                sections.add(new Section("공동 보급", "개인 주화를 시설 수리·강화용 공동 보급품으로 전환합니다.", GOLD, false));
            }
        }
        renderDetail(graphics, mouseX, mouseY, pane, sections, facilityButtons(facility), stateColor);
    }

    private List<ActionButton> facilityButtons(FacilityCard facility) {
        List<ActionButton> result = new ArrayList<>();
        if (tab == Tab.REPAIR) {
            if (facility.hp() < facility.maxHp()) {
                result.add(new ActionButton("repair:" + facility.id(), "완전 수리", facility.name()));
            }
            return result;
        }
        if (facility.canUpgrade()) {
            result.add(new ActionButton("upgrade:" + facility.id(), "시설 강화", facility.name()));
        }
        if (facility.id().equals("walls")) {
            result.add(new ActionButton("open_tower_control", "포탑 관리", facility.name()));
        } else if (facility.id().equals("town_hall")) {
            result.add(new ActionButton("open_funding", "보급 조달", facility.name()));
        }
        return result;
    }

    private void renderDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane,
                              List<Section> sections, List<ActionButton> buttons, int accent) {
        int actionTop = buttons.isEmpty() ? pane.bottom() - 10 : pane.bottom() - ACTION_HEIGHT - 12;
        int textLeft = pane.left() + 16;
        int textRight = pane.right() - 16;
        int textTop = pane.top() + 14;
        int textBottom = buttons.isEmpty() ? pane.bottom() - 12 : actionTop - 9;
        List<DetailLine> lines = sectionLines(sections, Math.max(100, textRight - textLeft));
        int content = 2;
        for (DetailLine line : lines) content += line.height() + line.gap();
        int viewport = Math.max(1, textBottom - textTop);
        int maximum = Math.max(0, content - viewport);
        detailScroll = clamp(detailScroll, 0, maximum);

        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, Math.max(pane.top() + 3, textBottom));
        int y = textTop - detailScroll;
        for (DetailLine line : lines) {
            y += line.gap();
            if (y + line.height() >= textTop && y <= textBottom) {
                graphics.text(font, line.text(), textLeft, y, line.color(), false);
            }
            y += line.height();
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 6, textTop, textBottom,
                detailScroll, maximum, viewport, content, accent);
        renderActionButtons(graphics, mouseX, mouseY, pane, buttons, actionTop, accent);
    }

    private List<DetailLine> sectionLines(List<Section> sections, int width) {
        List<DetailLine> lines = new ArrayList<>();
        for (Section section : sections) {
            int gap = lines.isEmpty() ? 0 : 5;
            List<FormattedCharSequence> title = font.split(Component.literal(plain(section.title())), width);
            boolean first = true;
            for (FormattedCharSequence line : title) {
                lines.add(new DetailLine(line, section.color(), section.major() ? 13 : 11, first ? gap : 0));
                first = false;
            }
            if (!section.body().isBlank()) {
                List<FormattedCharSequence> body = font.split(Component.literal(plain(section.body())), width);
                first = true;
                for (FormattedCharSequence line : body) {
                    lines.add(new DetailLine(line, TEXT, 11, first ? 2 : 0));
                    first = false;
                }
            }
        }
        return lines;
    }

    private void renderActionButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane,
                                     List<ActionButton> buttons, int y, int accent) {
        List<ButtonBounds> bounds = actionBounds(pane, buttons, y);
        for (int i = 0; i < bounds.size(); i++) {
            ButtonBounds bound = bounds.get(i);
            ActionButton button = buttons.get(i);
            boolean hovered = inside(mouseX, mouseY, bound.x(), bound.y(), bound.width(), ACTION_HEIGHT);
            graphics.fill(bound.x() - 1, bound.y() - 1, bound.x() + bound.width() + 1,
                    bound.y() + ACTION_HEIGHT + 1, hovered ? GOLD : accent);
            graphics.fill(bound.x(), bound.y(), bound.x() + bound.width(), bound.y() + ACTION_HEIGHT,
                    hovered ? 0xFFFFE8B5 : SELECTED);
            graphics.centeredText(font, compact(button.label(), bound.width() - 10),
                    bound.x() + bound.width() / 2, bound.y() + 5, TEXT);
        }
    }

    private List<ButtonBounds> actionBounds(Pane pane, List<ActionButton> buttons, int y) {
        if (buttons.isEmpty()) return List.of();
        int gap = 6;
        int available = pane.width() - 32;
        int width = Math.min(112, Math.max(72, (available - gap * (buttons.size() - 1)) / buttons.size()));
        int total = width * buttons.size() + gap * (buttons.size() - 1);
        int x = pane.right() - 16 - total;
        List<ButtonBounds> result = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i++) {
            result.add(new ButtonBounds(x, y, width));
            x += width + gap;
        }
        return result;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 36, layout.top() + 7, 27, 27)) {
            onClose();
            return true;
        }
        int tabY = layout.top() + 40;
        int gap = 5;
        int tabWidth = (layout.width() - 32 - gap * 2) / 3;
        int x = layout.left() + 16;
        for (Tab value : Tab.values()) {
            if (inside(click.x(), click.y(), x, tabY, tabWidth, 18)) {
                tab = value;
                listScroll = 0;
                detailScroll = 0;
                return true;
            }
            x += tabWidth + gap;
        }

        Split split = split(layout);
        int y = split.list().top() + 6 - listScroll;
        int count = tab == Tab.ROLES ? roles.size() : facilities.size();
        for (int i = 0; i < count; i++) {
            if (inside(click.x(), click.y(), split.list().left() + 6, y,
                    split.list().width() - 15, CARD_HEIGHT)) {
                if (tab == Tab.ROLES) selectedRole = i;
                else selectedFacility = i;
                detailScroll = 0;
                return true;
            }
            y += CARD_HEIGHT + CARD_GAP;
        }

        List<ActionButton> buttons = currentButtons();
        int actionTop = split.detail().bottom() - ACTION_HEIGHT - 12;
        List<ButtonBounds> bounds = actionBounds(split.detail(), buttons, actionTop);
        for (int i = 0; i < bounds.size(); i++) {
            ButtonBounds bound = bounds.get(i);
            if (inside(click.x(), click.y(), bound.x(), bound.y(), bound.width(), ACTION_HEIGHT)) {
                ActionButton button = buttons.get(i);
                confirmOrSend(button.action(), button.title());
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private List<ActionButton> currentButtons() {
        if (tab == Tab.ROLES) {
            if (roles.isEmpty()) return List.of();
            RoleCard role = roles.get(clamp(selectedRole, 0, roles.size() - 1));
            return role.current() ? List.of()
                    : List.of(new ActionButton(role.action(), role.name() + " 배치", role.name()));
        }
        if (facilities.isEmpty()) return List.of();
        return facilityButtons(facilities.get(clamp(selectedFacility, 0, facilities.size() - 1)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Split split = split(layout());
        int amount = (int) Math.round(vertical * 32);
        if (inside(mouseX, mouseY, split.list().left(), split.list().top(), split.list().width(), split.list().height())) {
            listScroll = Math.max(0, listScroll - amount);
            return true;
        }
        if (inside(mouseX, mouseY, split.detail().left(), split.detail().top(), split.detail().width(), split.detail().height())) {
            detailScroll = Math.max(0, detailScroll - amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void confirmOrSend(String action, String title) {
        String description = VillageActionDescriptions.describe(action, title);
        if (VillageActionDescriptions.requiresConfirmation(action) && minecraft != null) {
            minecraft.gui.setScreen(new VillageConfirmScreen(this, action, title, description));
        } else {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
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
                facilities.add(new FacilityCard(p[1], p[2], p[3], parseInt(p[4]), parseInt(p[5]), p[6],
                        p.length > 7 ? p[7] : "", p.length > 8 ? parseInt(p[8]) : 0,
                        p.length > 9 ? parseInt(p[9]) : 0));
            }
        }
    }

    private void card(GuiGraphicsExtractor graphics, int x, int y, int width,
                      boolean selected, boolean hovered, int accent) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + CARD_HEIGHT + 1,
                selected ? accent : hovered ? GOLD : BORDER);
        graphics.fill(x, y, x + width, y + CARD_HEIGHT, selected ? SELECTED : SURFACE);
        graphics.fill(x, y, x + 4, y + CARD_HEIGHT, selected ? accent : BORDER);
    }

    private void panel(GuiGraphicsExtractor graphics, Pane pane, int color) {
        graphics.fill(pane.left() - 1, pane.top() - 1, pane.right() + 1, pane.bottom() + 1, BORDER);
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), color);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                           int value, int maximum, int visible, int content, int color) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(14, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 3, bottom, 0xFFB9AA91);
        graphics.fill(x, y, x + 3, y + thumb, color);
    }

    private int roleColor(String id) {
        return switch (id) {
            case "vanguard" -> RED;
            case "ranger" -> GOLD;
            case "arcanist" -> PURPLE;
            case "luminar" -> GREEN;
            case "warden" -> BLUE;
            default -> TEAL;
        };
    }

    private int listHeight(int count) {
        return count <= 0 ? 0 : count * CARD_HEIGHT + Math.max(0, count - 1) * CARD_GAP;
    }

    private Layout layout() {
        int margin = 8;
        int panelWidth = Math.min(900, Math.max(380, width - margin * 2));
        int panelHeight = Math.min(560, Math.max(285, height - margin * 2));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Split split(Layout layout) {
        int left = layout.left() + 15;
        int right = layout.right() - 15;
        int top = layout.top() + 64;
        int bottom = layout.bottom() - 13;
        int contentWidth = right - left;
        if (contentWidth < 260) {
            int availableHeight = Math.max(1, bottom - top);
            int listHeight = clamp(availableHeight * 36 / 100, 72,
                    Math.max(72, availableHeight - 96));
            return new Split(new Pane(left, top, right, top + listHeight),
                    new Pane(left, top + listHeight + 7, right, bottom));
        }
        int listWidth = clamp(contentWidth * 24 / 100, 125, 205);
        return new Split(new Pane(left, top, left + listWidth, bottom),
                new Pane(left + listWidth + 8, top, right, bottom));
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String compact(String value, int maxWidth) {
        String normalized = plain(value).replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        if (font.width(suffix) > maxWidth) return "";
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, end) + suffix;
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Tab {
        ROLES("직업 배치", TEAL), REPAIR("시설 수리", RED), MANAGEMENT("시설 강화", GOLD);
        private final String displayName;
        private final int accent;
        Tab(String displayName, int accent) { this.displayName = displayName; this.accent = accent; }
        String displayName() { return displayName; }
        int accent() { return accent; }
    }

    private record RoleCard(String action, String id, String name, String overview, String passive,
                            String active, String recommended, boolean current, String loadout) {}

    private record FacilityCard(String id, String name, String level, int hp, int maxHp, String effect,
                                String nextEffect, int upgradeCost, int repairCost) {
        int levelValue() {
            String digits = level == null ? "" : level.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            try { return Integer.parseInt(digits.substring(0, 1)); }
            catch (NumberFormatException ignored) { return 0; }
        }
        boolean canUpgrade() {
            return hp > 0 && !id.equals("town_hall") && levelValue() < 5
                    && upgradeCost > 0 && nextEffect != null && !nextEffect.isBlank();
        }
    }

    private record Section(String title, String body, int color, boolean major) {}
    private record DetailLine(FormattedCharSequence text, int color, int height, int gap) {}
    private record ActionButton(String action, String label, String title) {}
    private record ButtonBounds(int x, int y, int width) {}
    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    private record Split(Pane list, Pane detail) {}
}

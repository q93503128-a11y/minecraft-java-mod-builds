package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Town hall command interface: role assignment, repairs and management are separate jobs. */
public final class VillageTownHallScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0x65000000;
    private static final int PANEL = 0xFFF0E5CC;
    private static final int SURFACE = 0xFFFFF8E8;
    private static final int SURFACE_ALT = 0xFFE6D9BE;
    private static final int SELECTED = 0xFFFFE2A8;
    private static final int BORDER = 0xFF75634C;
    private static final int TEXT = 0xFF241D17;
    private static final int MUTED = 0xFF6D6256;
    private static final int TEAL = 0xFF2E8E80;
    private static final int GOLD = 0xFFC78B2D;
    private static final int RED = 0xFFB95050;
    private static final int BLUE = 0xFF4F79B8;
    private static final int PURPLE = 0xFF8055A8;
    private static final int GREEN = 0xFF43835C;
    private static final int CARD_HEIGHT = 57;
    private static final int CARD_GAP = 6;

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
        graphics.fill(split.list().left() - 1, split.list().top() - 1,
                split.list().right() + 1, split.list().bottom() + 1, BORDER);
        graphics.fill(split.list().left(), split.list().top(), split.list().right(), split.list().bottom(), SURFACE_ALT);
        graphics.fill(split.detail().left() - 1, split.detail().top() - 1,
                split.detail().right() + 1, split.detail().bottom() + 1, BORDER);
        graphics.fill(split.detail().left(), split.detail().top(), split.detail().right(), split.detail().bottom(), SURFACE);
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
        int left = layout.left() + 20;
        int closeX = layout.right() - 39;
        graphics.text(font, "마을 회관", left, layout.top() + 11, TEXT, false);
        List<FormattedCharSequence> summary = font.split(Component.literal(payload.body()),
                Math.max(150, closeX - left - 12));
        if (!summary.isEmpty()) graphics.text(font, summary.getFirst(), left, layout.top() + 29, MUTED, false);
        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 8, 29, 29);
        graphics.fill(closeX, layout.top() + 8, closeX + 29, layout.top() + 37,
                close ? 0xFFE6A6A6 : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 14, layout.top() + 18, close ? RED : TEXT);

        int tabY = layout.top() + 48;
        int gap = 7;
        int total = layout.width() - 36;
        int tabWidth = (total - gap * 2) / 3;
        int x = layout.left() + 18;
        for (Tab value : Tab.values()) {
            drawTab(graphics, mouseX, mouseY, x, tabY, tabWidth, value);
            x += tabWidth + gap;
        }
    }

    private void drawTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                         int x, int y, int width, Tab value) {
        boolean active = tab == value;
        boolean hovered = inside(mouseX, mouseY, x, y, width, 27);
        graphics.fill(x - 1, y - 1, x + width + 1, y + 28, active ? value.accent() : BORDER);
        graphics.fill(x, y, x + width, y + 27, active ? SELECTED : hovered ? SURFACE : SURFACE_ALT);
        graphics.centeredText(font, value.displayName(), x + width / 2, y + 9, active ? TEXT : MUTED);
    }

    private void renderRoleList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        int content = listHeight(roles.size());
        int viewport = Math.max(1, pane.height() - 18);
        int maximum = Math.max(0, content - viewport);
        listScroll = clamp(listScroll, 0, maximum);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, pane.bottom() - 2);
        int y = pane.top() + 9 - listScroll;
        for (int i = 0; i < roles.size(); i++) {
            RoleCard role = roles.get(i);
            int x = pane.left() + 9;
            int w = pane.width() - 20;
            boolean hovered = inside(mouseX, mouseY, x, y, w, CARD_HEIGHT);
            boolean selected = selectedRole == i;
            int color = roleColor(role.id());
            card(graphics, x, y, w, selected, hovered, color);
            graphics.text(font, compact(role.name(), Math.max(12, w / 7)), x + 15, y + 11, TEXT, false);
            graphics.text(font, role.current() ? "현재 직업" : "배치 가능",
                    x + 15, y + 35, role.current() ? TEAL : MUTED, false);
            y += CARD_HEIGHT + CARD_GAP;
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 7, pane.top() + 7, pane.bottom() - 7,
                listScroll, maximum, viewport, content, TEAL);
    }

    private void renderRoleDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        if (roles.isEmpty()) return;
        RoleCard role = roles.get(clamp(selectedRole, 0, roles.size() - 1));
        String action = role.current() ? "" : role.action();
        String buttonLabel = role.current() ? "현재 배치된 직업" : role.name() + " 배치";
        List<Section> sections = new ArrayList<>();
        sections.add(new Section(role.name(), roleColor(role.id()), true));
        sections.add(new Section("역할\n" + role.overview(), TEXT, false));
        sections.add(new Section("상시 효과\n" + role.passive(), TEAL, false));
        sections.add(new Section("전투 방식\n" + role.active(), GOLD, false));
        sections.add(new Section("추천 위치\n" + role.recommended(), BLUE, false));
        if (role.current() && !role.loadout().isBlank()) {
            sections.add(new Section("현재 장착 기술\n" + role.loadout(), GREEN, false));
        }
        renderSections(graphics, mouseX, mouseY, pane, sections, action, buttonLabel, roleColor(role.id()));
    }

    private void renderFacilityList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        int content = listHeight(facilities.size());
        int viewport = Math.max(1, pane.height() - 18);
        int maximum = Math.max(0, content - viewport);
        listScroll = clamp(listScroll, 0, maximum);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, pane.bottom() - 2);
        int y = pane.top() + 9 - listScroll;
        for (int i = 0; i < facilities.size(); i++) {
            FacilityCard facility = facilities.get(i);
            int x = pane.left() + 9;
            int w = pane.width() - 20;
            boolean hovered = inside(mouseX, mouseY, x, y, w, CARD_HEIGHT);
            boolean selected = selectedFacility == i;
            int stateColor = facility.hp() <= 0 ? RED : facility.hp() < facility.maxHp() ? GOLD : TEAL;
            card(graphics, x, y, w, selected, hovered, stateColor);
            graphics.text(font, compact(facility.name(), Math.max(12, w / 7)), x + 15, y + 8, TEXT, false);
            String state = tab == Tab.REPAIR
                    ? facility.hp() + " / " + facility.maxHp()
                    : facility.level();
            graphics.text(font, compact(state, Math.max(11, w / 7)), x + 15, y + 27, MUTED, false);
            int barLeft = x + 15;
            int barRight = x + w - 12;
            graphics.fill(barLeft, y + 45, barRight, y + 50, 0xFFC1B39B);
            int fill = facility.maxHp() <= 0 ? 0
                    : Math.round((barRight - barLeft) * facility.hp() / (float) facility.maxHp());
            graphics.fill(barLeft, y + 45, barLeft + Math.max(0, fill), y + 50, stateColor);
            y += CARD_HEIGHT + CARD_GAP;
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 7, pane.top() + 7, pane.bottom() - 7,
                listScroll, maximum, viewport, content, tab.accent());
    }

    private void renderFacilityDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        if (facilities.isEmpty()) return;
        FacilityCard facility = facilities.get(clamp(selectedFacility, 0, facilities.size() - 1));
        String action;
        String button;
        if (tab == Tab.REPAIR) {
            action = facility.hp() < facility.maxHp() ? "repair:" + facility.id() : "";
            button = action.isBlank() ? "수리할 손상이 없습니다" : "완전 수리";
        } else {
            action = managementAction(facility);
            button = managementLabel(facility, action);
        }
        int stateColor = facility.hp() <= 0 ? RED : facility.hp() < facility.maxHp() ? GOLD : TEAL;
        List<Section> sections = new ArrayList<>();
        sections.add(new Section(facility.name(), stateColor, true));
        sections.add(new Section("시설 상태\n" + facility.level() + " · 내구도 "
                + facility.hp() + " / " + facility.maxHp(), TEXT, false));
        sections.add(new Section("현재 효과\n" + facility.effect(), TEAL, false));
        if (tab == Tab.REPAIR) {
            sections.add(new Section(facility.hp() < facility.maxHp()
                    ? "회관 수리반이 공동 보급품을 사용해 내구도를 완전히 복구합니다."
                    : "현재 손상이 없습니다.", GOLD, false));
        } else {
            sections.add(new Section(managementGuide(facility), GOLD, false));
        }
        renderSections(graphics, mouseX, mouseY, pane, sections, action, button, stateColor);
    }

    private void renderSections(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane,
                                List<Section> sections, String action, String buttonLabel, int accent) {
        int buttonHeight = 34;
        int buttonTop = pane.bottom() - 49;
        int textLeft = pane.left() + 20;
        int textRight = pane.right() - 20;
        int textTop = pane.top() + 18;
        int textBottom = buttonTop - 12;
        List<DetailLine> lines = new ArrayList<>();
        for (Section section : sections) {
            boolean first = true;
            for (String paragraph : section.text().split("\n", -1)) {
                for (FormattedCharSequence line : font.split(Component.literal(paragraph), Math.max(100, textRight - textLeft))) {
                    lines.add(new DetailLine(line, section.color(), section.title() && first ? 16 : 14,
                            first ? (lines.isEmpty() ? 0 : 9) : 0));
                    first = false;
                }
            }
        }
        int content = 4;
        for (DetailLine line : lines) content += line.height() + line.gap();
        int viewport = Math.max(1, textBottom - textTop);
        int maximum = Math.max(0, content - viewport);
        detailScroll = clamp(detailScroll, 0, maximum);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, textBottom);
        int y = textTop - detailScroll;
        for (DetailLine line : lines) {
            y += line.gap();
            if (y + line.height() >= textTop && y <= textBottom) {
                graphics.text(font, line.text(), textLeft, y, line.color(), false);
            }
            y += line.height();
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 8, textTop, textBottom,
                detailScroll, maximum, viewport, content, accent);

        boolean active = !action.isBlank();
        boolean hovered = active && inside(mouseX, mouseY, textLeft, buttonTop, textRight - textLeft, buttonHeight);
        graphics.fill(textLeft - 1, buttonTop - 1, textRight + 1, buttonTop + buttonHeight + 1,
                active ? (hovered ? GOLD : accent) : BORDER);
        graphics.fill(textLeft, buttonTop, textRight, buttonTop + buttonHeight,
                active ? (hovered ? 0xFFFFE9B9 : SELECTED) : SURFACE_ALT);
        graphics.centeredText(font, compact(buttonLabel, Math.max(16, (textRight - textLeft) / 7)),
                (textLeft + textRight) / 2, buttonTop + 12, active ? TEXT : MUTED);
    }

    private String managementAction(FacilityCard facility) {
        if (facility.id().equals("town_hall")) return "open_funding";
        if (facility.id().equals("walls")) return "open_tower_control";
        if (facility.hp() <= 0 || facility.levelValue() >= 5) return "";
        return "upgrade:" + facility.id();
    }

    private String managementLabel(FacilityCard facility, String action) {
        if (action.equals("open_funding")) return "공동 보급품 조달";
        if (action.equals("open_tower_control")) return "성벽·포탑 관리";
        if (action.startsWith("upgrade:")) return "시설 강화";
        if (facility.hp() <= 0) return "먼저 수리해야 합니다";
        return "최고 강화 단계";
    }

    private String managementGuide(FacilityCard facility) {
        if (facility.id().equals("town_hall")) return "개인 주화를 공동 보급품으로 조달합니다.";
        if (facility.id().equals("walls")) return "성벽 강화, 포탑 설치와 전문화를 관리합니다.";
        if (facility.hp() <= 0) return "파괴된 시설은 수리 탭에서 먼저 복구해야 합니다.";
        if (facility.levelValue() >= 5) return "시설이 최고 강화 단계에 도달했습니다.";
        return "공동 보급품을 사용해 다음 단계로 강화합니다.";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 39, layout.top() + 8, 29, 29)) {
            onClose();
            return true;
        }
        int tabY = layout.top() + 48;
        int gap = 7;
        int tabWidth = (layout.width() - 36 - gap * 2) / 3;
        int x = layout.left() + 18;
        for (Tab value : Tab.values()) {
            if (inside(click.x(), click.y(), x, tabY, tabWidth, 27)) {
                tab = value;
                listScroll = 0;
                detailScroll = 0;
                return true;
            }
            x += tabWidth + gap;
        }
        Split split = split(layout);
        int y = split.list().top() + 9 - listScroll;
        int count = tab == Tab.ROLES ? roles.size() : facilities.size();
        for (int i = 0; i < count; i++) {
            if (inside(click.x(), click.y(), split.list().left() + 9, y,
                    split.list().width() - 20, CARD_HEIGHT)) {
                if (tab == Tab.ROLES) selectedRole = i;
                else selectedFacility = i;
                detailScroll = 0;
                return true;
            }
            y += CARD_HEIGHT + CARD_GAP;
        }
        String action = currentAction();
        if (!action.isBlank() && inside(click.x(), click.y(), split.detail().left() + 20,
                split.detail().bottom() - 49, split.detail().width() - 40, 34)) {
            confirmOrSend(action, currentTitle());
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Split split = split(layout());
        int amount = (int) Math.round(vertical * 40);
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

    private String currentAction() {
        if (tab == Tab.ROLES) {
            if (roles.isEmpty()) return "";
            RoleCard role = roles.get(clamp(selectedRole, 0, roles.size() - 1));
            return role.current() ? "" : role.action();
        }
        if (facilities.isEmpty()) return "";
        FacilityCard facility = facilities.get(clamp(selectedFacility, 0, facilities.size() - 1));
        return tab == Tab.REPAIR
                ? facility.hp() < facility.maxHp() ? "repair:" + facility.id() : ""
                : managementAction(facility);
    }

    private String currentTitle() {
        if (tab == Tab.ROLES && !roles.isEmpty()) return roles.get(clamp(selectedRole, 0, roles.size() - 1)).name();
        if (!facilities.isEmpty()) return facilities.get(clamp(selectedFacility, 0, facilities.size() - 1)).name();
        return "마을 회관";
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
                facilities.add(new FacilityCard(p[1], p[2], p[3], parseInt(p[4]), parseInt(p[5]), p[6]));
            }
        }
    }

    private void card(GuiGraphicsExtractor graphics, int x, int y, int width,
                      boolean selected, boolean hovered, int accent) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + CARD_HEIGHT + 1,
                selected ? accent : hovered ? GOLD : BORDER);
        graphics.fill(x, y, x + width, y + CARD_HEIGHT, selected ? SELECTED : SURFACE);
        graphics.fill(x, y, x + 5, y + CARD_HEIGHT, selected ? accent : BORDER);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                           int value, int maximum, int visible, int content, int color) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(18, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 4, bottom, 0xFFB9AA91);
        graphics.fill(x, y, x + 4, y + thumb, color);
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
        int panelWidth = Math.min(980, Math.max(410, width - margin * 2));
        int panelHeight = Math.min(650, Math.max(300, height - margin * 2));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Split split(Layout layout) {
        int left = layout.left() + 18;
        int right = layout.right() - 18;
        int top = layout.top() + 88;
        int bottom = layout.bottom() - 16;
        int listWidth = clamp((right - left) * 35 / 100, 190, 310);
        return new Split(new Pane(left, top, left + listWidth, bottom),
                new Pane(left + listWidth + 10, top, right, bottom));
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

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Tab {
        ROLES("직업 배치", TEAL), REPAIR("시설 수리", RED), MANAGEMENT("관리·건설", GOLD);
        private final String displayName;
        private final int accent;
        Tab(String displayName, int accent) { this.displayName = displayName; this.accent = accent; }
        String displayName() { return displayName; }
        int accent() { return accent; }
    }

    private record RoleCard(String action, String id, String name, String overview, String passive,
                            String active, String recommended, boolean current, String loadout) {}
    private record FacilityCard(String id, String name, String level, int hp, int maxHp, String effect) {
        int levelValue() {
            String digits = level.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            try { return Integer.parseInt(digits.substring(0, 1)); }
            catch (NumberFormatException ignored) { return 0; }
        }
    }
    private record Section(String text, int color, boolean title) {}
    private record DetailLine(FormattedCharSequence text, int color, int height, int gap) {}
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

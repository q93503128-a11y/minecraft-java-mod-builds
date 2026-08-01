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
    private static final int MUTED = 0xFFA7B5C1;
    private static final int GOLD = 0xFFF1C35E;
    private static final int ACCENT = 0xFF45D7BE;
    private static final int RED = 0xFFE06A72;
    private static final int BLUE = 0xFF78A9EE;
    private static final int GREEN = 0xFF58D39B;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final List<RoleEntry> roles = new ArrayList<>();
    private final List<FacilityEntry> facilities = new ArrayList<>();
    private Tab tab = Tab.ROLES;
    private int selectedRole = -1;

    public VillageTownHallScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        parseEntries(payload);
        for (int index = 0; index < roles.size(); index++) {
            if (roles.get(index).selected()) {
                selectedRole = index;
                break;
            }
        }
        if (selectedRole < 0 && !roles.isEmpty()) selectedRole = 0;
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
        int right = layout.left() + layout.width();
        int bottom = layout.top() + layout.height();
        graphics.fill(layout.left() - 2, layout.top() - 2, right + 2, bottom + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), right, bottom, PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, bottom, ACCENT);
        graphics.text(font, "마을 회관", layout.left() + 18, layout.top() + 14, TEXT, false);
        graphics.text(font, compact(payload.body(), Math.max(44, layout.width() / 7)), layout.left() + 18, layout.top() + 30, MUTED, false);
        int closeX = right - 34;
        int closeY = layout.top() + 11;
        boolean closeHovered = inside(mouseX, mouseY, closeX, closeY, 22, 22);
        graphics.fill(closeX, closeY, closeX + 22, closeY + 22, closeHovered ? 0xFF6E3038 : SURFACE_2);
        graphics.centeredText(font, "×", closeX + 11, closeY + 7, closeHovered ? 0xFFFFFFFF : MUTED);
        int tabY = layout.top() + 49;
        drawTab(graphics, mouseX, mouseY, layout.left() + 16, tabY, 106, 24, "역할 배치", tab == Tab.ROLES);
        drawTab(graphics, mouseX, mouseY, layout.left() + 128, tabY, 106, 24, "시설 관리", tab == Tab.FACILITIES);
        int contentLeft = layout.left() + 15;
        int contentTop = tabY + 31;
        int contentRight = right - 15;
        int contentBottom = bottom - 15;
        graphics.fill(contentLeft, contentTop, contentRight, contentBottom, SURFACE);
        if (tab == Tab.ROLES) renderRoles(graphics, mouseX, mouseY, contentLeft, contentTop, contentRight, contentBottom);
        else renderFacilities(graphics, mouseX, mouseY, contentLeft, contentTop, contentRight, contentBottom);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRoles(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        boolean wide = right - left >= 540;
        if (!wide) {
            renderRolesCompact(graphics, mouseX, mouseY, left, top, right, bottom);
            return;
        }
        int divider = left + (right - left) * 54 / 100;
        graphics.fill(divider, top + 10, divider + 1, bottom - 10, BORDER);
        int gridLeft = left + 12;
        int gridTop = top + 12;
        int gridRight = divider - 12;
        int gap = 8;
        int cardWidth = Math.max(120, (gridRight - gridLeft - gap) / 2);
        int cardHeight = Math.max(76, Math.min(104, (bottom - gridTop - 20 - gap) / 2));
        for (int index = 0; index < roles.size(); index++) {
            int x = gridLeft + (index % 2) * (cardWidth + gap);
            int y = gridTop + (index / 2) * (cardHeight + gap);
            drawRoleCard(graphics, mouseX, mouseY, index, x, y, cardWidth, cardHeight);
        }
        drawRoleDetail(graphics, mouseX, mouseY, divider + 14, top + 12, right - 12, bottom - 12);
    }

    private void renderRolesCompact(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        int gap = 6;
        int gridLeft = left + 9;
        int gridRight = right - 9;
        int cardWidth = Math.max(92, (gridRight - gridLeft - gap) / 2);
        int cardHeight = 56;
        for (int index = 0; index < roles.size(); index++) {
            int x = gridLeft + (index % 2) * (cardWidth + gap);
            int y = top + 9 + (index / 2) * (cardHeight + gap);
            drawRoleCard(graphics, mouseX, mouseY, index, x, y, cardWidth, cardHeight);
        }
        int detailTop = top + 9 + 2 * (cardHeight + gap) + 4;
        drawRoleDetail(graphics, mouseX, mouseY, gridLeft, detailTop, gridRight, bottom - 9);
    }

    private void drawRoleCard(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int index, int x, int y, int width, int height) {
        if (index >= roles.size()) return;
        RoleEntry role = roles.get(index);
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        boolean selected = selectedRole == index;
        int color = roleColor(role.id());
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, selected ? color : hovered ? BORDER : 0xFF263745);
        graphics.fill(x, y, x + width, y + height, hovered || selected ? SURFACE_2 : 0xFF101923);
        graphics.fill(x, y, x + 4, y + height, color);
        int iconSize = Math.min(34, height - 18);
        int iconX = x + 12;
        int iconY = y + (height - iconSize) / 2;
        drawRoleIcon(graphics, role.id(), iconX, iconY, iconSize, color);
        int textX = iconX + iconSize + 10;
        graphics.text(font, role.name(), textX, y + 13, TEXT, false);
        graphics.text(font, role.selected() ? "현재 역할" : "선택 가능", textX, y + 29, role.selected() ? ACCENT : MUTED, false);
        if (height >= 82) {
            List<FormattedCharSequence> lines = font.split(Component.literal(role.overview()), Math.max(70, x + width - textX - 10));
            int lineY = y + 46;
            for (FormattedCharSequence line : lines) {
                if (lineY > y + height - 11) break;
                graphics.text(font, line, textX, lineY, MUTED, false);
                lineY += 11;
            }
        }
    }

    private void drawRoleDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, 0xFF0E171F);
        if (selectedRole < 0 || selectedRole >= roles.size()) {
            graphics.text(font, "역할을 선택하세요.", left + 12, top + 12, MUTED, false);
            return;
        }
        RoleEntry role = roles.get(selectedRole);
        int color = roleColor(role.id());
        graphics.fill(left, top, left + 4, bottom, color);
        graphics.text(font, role.name(), left + 14, top + 12, TEXT, false);
        String state = role.selected() ? "현재 배치됨" : "새 역할";
        graphics.text(font, state, right - 12 - font.width(state), top + 12, role.selected() ? ACCENT : GOLD, false);
        int y = top + 34;
        y = drawSection(graphics, "역할", role.overview(), left + 14, y, right - 14, bottom - 45, TEXT);
        y = drawSection(graphics, "상시 효과", role.passive(), left + 14, y, right - 14, bottom - 45, ACCENT);
        y = drawSection(graphics, "R키 전술", role.active(), left + 14, y, right - 14, bottom - 45, GOLD);
        drawSection(graphics, "추천 위치", role.recommended(), left + 14, y, right - 14, bottom - 45, BLUE);
        if (!role.selected()) {
            int buttonX = left + 12;
            int buttonY = bottom - 34;
            int buttonWidth = right - left - 24;
            boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 23);
            graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY + 24, hovered ? GOLD : color);
            graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + 23, hovered ? 0xFF3C3320 : SURFACE_2);
            graphics.centeredText(font, role.name() + "으로 배치", buttonX + buttonWidth / 2, buttonY + 7, TEXT);
        }
    }

    private int drawSection(GuiGraphicsExtractor graphics, String heading, String value, int left, int y, int right, int limit, int headingColor) {
        if (y > limit) return y;
        graphics.text(font, heading, left, y, headingColor, false);
        y += 13;
        List<FormattedCharSequence> lines = font.split(Component.literal(value), Math.max(80, right - left));
        for (FormattedCharSequence line : lines) {
            if (y > limit) break;
            graphics.text(font, line, left, y, MUTED, false);
            y += 11;
        }
        return y + 7;
    }

    private void renderFacilities(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        int columns = right - left >= 620 ? 3 : 2;
        int gap = 8;
        int padding = 12;
        int cardWidth = Math.max(120, (right - left - padding * 2 - gap * (columns - 1)) / columns);
        int rows = Math.max(1, (facilities.size() + columns - 1) / columns);
        int cardHeight = Math.max(60, Math.min(88, (bottom - top - padding * 2 - gap * (rows - 1)) / rows));
        for (int index = 0; index < facilities.size(); index++) {
            int x = left + padding + (index % columns) * (cardWidth + gap);
            int y = top + padding + (index / columns) * (cardHeight + gap);
            drawFacilityCard(graphics, mouseX, mouseY, facilities.get(index), x, y, cardWidth, cardHeight);
        }
    }

    private void drawFacilityCard(GuiGraphicsExtractor graphics, int mouseX, int mouseY, FacilityEntry facility, int x, int y, int width, int height) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        int condition = facility.maximum() <= 0 ? 0 : Math.round(100.0f * facility.current() / facility.maximum());
        int stateColor = condition <= 0 ? RED : condition < 45 ? GOLD : ACCENT;
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, hovered ? stateColor : BORDER);
        graphics.fill(x, y, x + width, y + height, hovered ? SURFACE_2 : 0xFF101923);
        int iconSize = Math.min(30, height - 20);
        drawFacilityIcon(graphics, x + 11, y + 11, iconSize, stateColor);
        int textX = x + 20 + iconSize;
        graphics.text(font, facility.name(), textX, y + 11, TEXT, false);
        graphics.text(font, facility.levelText(), textX, y + 27, MUTED, false);
        int barLeft = x + 11;
        int barRight = x + width - 11;
        int barY = y + height - 16;
        graphics.fill(barLeft, barY, barRight, barY + 5, 0xFF05080C);
        int fill = Math.max(0, Math.min(barRight - barLeft, Math.round((barRight - barLeft) * condition / 100.0f)));
        graphics.fill(barLeft, barY, barLeft + fill, barY + 5, stateColor);
        graphics.text(font, facility.current() + " / " + facility.maximum(), barLeft, barY - 11, condition <= 0 ? RED : MUTED, false);
        graphics.text(font, "›", x + width - 18, y + 12, hovered ? GOLD : MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        Layout layout = layout();
        int right = layout.left() + layout.width();
        if (click.button() == 0 && inside(click.x(), click.y(), right - 34, layout.top() + 11, 22, 22)) {
            onClose();
            return true;
        }
        int tabY = layout.top() + 49;
        if (click.button() == 0 && inside(click.x(), click.y(), layout.left() + 16, tabY, 106, 24)) {
            tab = Tab.ROLES;
            return true;
        }
        if (click.button() == 0 && inside(click.x(), click.y(), layout.left() + 128, tabY, 106, 24)) {
            tab = Tab.FACILITIES;
            return true;
        }
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int contentLeft = layout.left() + 15;
        int contentTop = tabY + 31;
        int contentRight = right - 15;
        int contentBottom = layout.top() + layout.height() - 15;
        if (tab == Tab.ROLES) return clickRole(click.x(), click.y(), contentLeft, contentTop, contentRight, contentBottom) || super.mouseClicked(click, doubled);
        return clickFacility(click.x(), click.y(), contentLeft, contentTop, contentRight, contentBottom) || super.mouseClicked(click, doubled);
    }

    private boolean clickRole(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        boolean wide = right - left >= 540;
        if (wide) {
            int divider = left + (right - left) * 54 / 100;
            int gridLeft = left + 12;
            int gridTop = top + 12;
            int gridRight = divider - 12;
            int gap = 8;
            int cardWidth = Math.max(120, (gridRight - gridLeft - gap) / 2);
            int cardHeight = Math.max(76, Math.min(104, (bottom - gridTop - 20 - gap) / 2));
            for (int index = 0; index < roles.size(); index++) {
                int x = gridLeft + (index % 2) * (cardWidth + gap);
                int y = gridTop + (index / 2) * (cardHeight + gap);
                if (inside(mouseX, mouseY, x, y, cardWidth, cardHeight)) {
                    selectedRole = index;
                    return true;
                }
            }
            if (selectedRole >= 0 && selectedRole < roles.size() && !roles.get(selectedRole).selected()) {
                int detailLeft = divider + 14;
                int detailRight = right - 12;
                int detailBottom = bottom - 12;
                if (inside(mouseX, mouseY, detailLeft + 12, detailBottom - 34, detailRight - detailLeft - 24, 23)) {
                    send(roles.get(selectedRole).action());
                    return true;
                }
            }
            return false;
        }
        int gap = 6;
        int gridLeft = left + 9;
        int gridRight = right - 9;
        int cardWidth = Math.max(92, (gridRight - gridLeft - gap) / 2);
        int cardHeight = 56;
        for (int index = 0; index < roles.size(); index++) {
            int x = gridLeft + (index % 2) * (cardWidth + gap);
            int y = top + 9 + (index / 2) * (cardHeight + gap);
            if (inside(mouseX, mouseY, x, y, cardWidth, cardHeight)) {
                selectedRole = index;
                return true;
            }
        }
        int detailTop = top + 9 + 2 * (cardHeight + gap) + 4;
        if (selectedRole >= 0 && selectedRole < roles.size() && !roles.get(selectedRole).selected()
                && inside(mouseX, mouseY, gridLeft + 12, bottom - 43, gridRight - gridLeft - 24, 23)
                && bottom - 43 >= detailTop) {
            send(roles.get(selectedRole).action());
            return true;
        }
        return false;
    }

    private boolean clickFacility(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        int columns = right - left >= 620 ? 3 : 2;
        int gap = 8;
        int padding = 12;
        int cardWidth = Math.max(120, (right - left - padding * 2 - gap * (columns - 1)) / columns);
        int rows = Math.max(1, (facilities.size() + columns - 1) / columns);
        int cardHeight = Math.max(60, Math.min(88, (bottom - top - padding * 2 - gap * (rows - 1)) / rows));
        for (int index = 0; index < facilities.size(); index++) {
            int x = left + padding + (index % columns) * (cardWidth + gap);
            int y = top + padding + (index / columns) * (cardHeight + gap);
            if (inside(mouseX, mouseY, x, y, cardWidth, cardHeight)) {
                send(facilities.get(index).action());
                return true;
            }
        }
        return false;
    }

    private void drawTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, int height, String label, boolean active) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        graphics.fill(x, y, x + width, y + height, active ? ACCENT : hovered ? SURFACE_2 : SURFACE);
        graphics.centeredText(font, label, x + width / 2, y + 8, active ? 0xFF07100F : TEXT);
    }

    private void drawRoleIcon(GuiGraphicsExtractor graphics, String id, int x, int y, int size, int color) {
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        graphics.fill(x, y, x + size, y + size, 0xFF0A1118);
        switch (id) {
            case "guard_captain" -> {
                graphics.fill(centerX - 7, y + 6, centerX + 7, y + 10, color);
                graphics.fill(centerX - 9, y + 10, centerX + 9, y + 18, color);
                graphics.fill(centerX - 6, y + 18, centerX + 6, y + 25, color);
                graphics.fill(centerX - 3, y + 25, centerX + 3, y + 29, color);
            }
            case "ranger" -> {
                for (int i = 0; i < size - 12; i++) graphics.fill(x + 7 + i / 2, y + 6 + i, x + 9 + i / 2, y + 8 + i, color);
                graphics.fill(x + 8, centerY - 1, x + size - 7, centerY + 1, color);
                graphics.fill(x + size - 11, centerY - 5, x + size - 7, centerY + 5, color);
            }
            case "engineer" -> {
                graphics.fill(centerX - 3, y + 8, centerX + 3, y + size - 6, color);
                graphics.fill(centerX - 10, y + 7, centerX + 10, y + 14, color);
                graphics.fill(centerX + 3, y + 14, centerX + 8, y + 20, color);
            }
            case "medic" -> {
                graphics.fill(centerX - 4, y + 7, centerX + 4, y + size - 7, color);
                graphics.fill(x + 7, centerY - 4, x + size - 7, centerY + 4, color);
            }
            default -> graphics.fill(centerX - 5, centerY - 5, centerX + 5, centerY + 5, color);
        }
    }

    private void drawFacilityIcon(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        graphics.fill(x, y + size / 3, x + size, y + size, 0xFF0A1118);
        graphics.fill(x + 3, y + size / 3 - 3, x + size - 3, y + size / 3 + 2, color);
        graphics.fill(x + 5, y + size / 3 + 3, x + 9, y + size - 3, color);
        graphics.fill(x + size - 9, y + size / 3 + 3, x + size - 5, y + size - 3, color);
        graphics.fill(x + size / 2 - 2, y + size / 2, x + size / 2 + 2, y + size - 2, color);
    }

    private int roleColor(String id) {
        return switch (id) {
            case "guard_captain" -> RED;
            case "ranger" -> GOLD;
            case "engineer" -> BLUE;
            case "medic" -> GREEN;
            default -> ACCENT;
        };
    }

    private void send(String action) {
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
    }

    private void parseEntries(VillageNetwork.OpenVillageUiPayload payload) {
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int index = 0; index < count; index++) {
            String[] parts = labels[index].split("\\|", -1);
            if (parts.length >= 8 && "role".equals(parts[0])) {
                roles.add(new RoleEntry(actions[index], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], "current".equals(parts[7]) || "현재".equals(parts[7])));
            } else if (parts.length >= 7 && "facility".equals(parts[0])) {
                facilities.add(new FacilityEntry(actions[index], parts[1], parts[2], parts[3], parseInt(parts[4]), parseInt(parts[5]), parts[6]));
            }
        }
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String compact(String value, int maxCharacters) {
        String normalized = value.replace('\n', ' ');
        if (normalized.length() <= maxCharacters) return normalized;
        return normalized.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private Layout layout() {
        int panelWidth = Math.max(310, Math.min(820, width - 18));
        int panelHeight = Math.max(250, Math.min(500, height - 14));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private enum Tab { ROLES, FACILITIES }
    private record Layout(int left, int top, int width, int height) {}
    private record RoleEntry(String action, String id, String name, String overview, String passive, String active, String recommended, boolean selected) {}
    private record FacilityEntry(String action, String id, String name, String levelText, int current, int maximum, String effect) {}
}

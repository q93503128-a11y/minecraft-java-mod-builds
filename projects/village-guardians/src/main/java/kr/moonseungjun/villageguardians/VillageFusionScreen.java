package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Explicit three-item smithy fusion picker. */
public final class VillageFusionScreen extends Screen {
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
    private static final int CARD_HEIGHT = 32;
    private static final int CARD_GAP = 3;
    private static final int ACTION_HEIGHT = 20;

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
        Layout layout = layout();
        graphics.fill(layout.left() - 2, layout.top() - 2, layout.right() + 2, layout.bottom() + 2, BORDER);
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.fill(layout.left(), layout.top(), layout.left() + 5, layout.bottom(), TEAL);
        renderHeader(graphics, mouseX, mouseY, layout);
        Panes panes = panes(layout);
        panel(graphics, panes.list(), SURFACE_ALT);
        panel(graphics, panes.detail(), SURFACE);
        renderCandidates(graphics, mouseX, mouseY, panes.list());
        renderSelection(graphics, mouseX, mouseY, panes.detail());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        int left = layout.left() + 18;
        int closeX = layout.right() - 36;
        int textWidth = Math.max(20, closeX - left - 8);
        graphics.text(font, fit("장비 3개 합성", textWidth), left, layout.top() + 9, TEXT, false);
        graphics.text(font, fit("같은 종류·같은 등급 장비 세 개를 직접 선택합니다.", textWidth),
                left, layout.top() + 25, MUTED, false);
        boolean close = inside(mouseX, mouseY, closeX, layout.top() + 7, 27, 27);
        graphics.fill(closeX, layout.top() + 7, closeX + 27, layout.top() + 34,
                close ? 0xFFE2AAAA : SURFACE_ALT);
        graphics.centeredText(font, "×", closeX + 13, layout.top() + 16, close ? RED : TEXT);
    }

    private void renderCandidates(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        int content = candidates.isEmpty() ? 0
                : candidates.size() * CARD_HEIGHT + Math.max(0, candidates.size() - 1) * CARD_GAP;
        int viewport = Math.max(1, pane.height() - 14);
        int maximum = Math.max(0, content - viewport);
        scroll = clamp(scroll, 0, maximum);
        int x = pane.left() + 7;
        int cardWidth = Math.max(30, pane.width() - 16);
        graphics.enableScissor(pane.left() + 2, pane.top() + 2, pane.right() - 2, pane.bottom() - 2);
        int y = pane.top() + 7 - scroll;
        for (Candidate candidate : candidates) {
            boolean selected = selectedSlots.contains(candidate.slot());
            boolean compatible = selectedSlots.isEmpty() || selected || selectedGroup().equals(candidate.group());
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, CARD_HEIGHT);
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + CARD_HEIGHT + 1,
                    selected ? GOLD : hovered && compatible ? TEAL : BORDER);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT,
                    selected ? SELECTED : compatible ? SURFACE : SURFACE_ALT);
            graphics.fill(x, y, x + 4, y + CARD_HEIGHT, selected ? GOLD : compatible ? TEAL : BORDER);
            graphics.text(font, fit(candidate.name(), cardWidth - 18), x + 10, y + 5,
                    compatible ? TEXT : MUTED, false);
            graphics.text(font, fit(candidate.rarity() + " · 인벤토리 " + (candidate.slot() + 1) + "번",
                    cardWidth - 18), x + 10, y + 18, selected ? GOLD : MUTED, false);
            y += CARD_HEIGHT + CARD_GAP;
        }
        if (candidates.isEmpty()) {
            y = pane.top() + 10;
            for (FormattedCharSequence line : font.split(Component.literal(
                    "합성 가능한 습격 장비가 없습니다. 전설 등급은 더 합성할 수 없습니다."),
                    Math.max(40, pane.width() - 20))) {
                graphics.text(font, line, pane.left() + 10, y, MUTED, false);
                y += 11;
            }
        }
        graphics.disableScissor();
        scrollbar(graphics, pane.right() - 5, pane.top() + 5, pane.bottom() - 5,
                scroll, maximum, viewport, content);
    }

    private void renderSelection(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Pane pane) {
        int left = pane.left() + 13;
        int right = pane.right() - 13;
        graphics.text(font, fit("선택 " + selectedSlots.size() + " / 3", right - left),
                left, pane.top() + 12, TEAL, false);
        int y = pane.top() + 32;
        for (int index = 0; index < 3; index++) {
            String value = index < selectedSlots.size()
                    ? candidateBySlot(selectedSlots.get(index)).map(Candidate::name).orElse("선택 해제됨")
                    : "장비를 선택하세요";
            graphics.fill(left - 1, y - 1, right + 1, y + 23, BORDER);
            graphics.fill(left, y, right, y + 22, index < selectedSlots.size() ? SELECTED : SURFACE_ALT);
            graphics.text(font, fit((index + 1) + ". " + value, right - left - 12),
                    left + 6, y + 6, index < selectedSlots.size() ? TEXT : MUTED, false);
            y += 29;
        }
        y += 5;
        String guide = selectedSlots.isEmpty()
                ? "첫 장비를 고르면 같은 종류·등급만 선택할 수 있습니다."
                : selectedSlots.size() < 3
                ? "같은 종류·같은 등급 장비를 " + (3 - selectedSlots.size()) + "개 더 선택하세요."
                : "선택한 세 장비를 다음 등급 하나로 합성합니다.";
        for (FormattedCharSequence line : font.split(Component.literal(guide), Math.max(40, right - left))) {
            graphics.text(font, line, left, y, MUTED, false);
            y += 11;
        }

        int buttonWidth = Math.min(108, Math.max(70, pane.width() / 3));
        int buttonX = right - buttonWidth;
        int buttonY = pane.bottom() - ACTION_HEIGHT - 11;
        boolean active = selectedSlots.size() == 3;
        boolean hovered = active && inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, ACTION_HEIGHT);
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1,
                buttonY + ACTION_HEIGHT + 1, active ? hovered ? GOLD : TEAL : BORDER);
        graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + ACTION_HEIGHT,
                active ? hovered ? 0xFFFFE8B5 : SELECTED : SURFACE_ALT);
        graphics.centeredText(font, fit(active ? "세 장비 합성" : "3개 선택 필요", buttonWidth - 8),
                buttonX + buttonWidth / 2, buttonY + 5, active ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Layout layout = layout();
        if (inside(click.x(), click.y(), layout.right() - 36, layout.top() + 7, 27, 27)) {
            onClose();
            return true;
        }
        Panes panes = panes(layout);
        int x = panes.list().left() + 7;
        int y = panes.list().top() + 7 - scroll;
        int cardWidth = Math.max(30, panes.list().width() - 16);
        for (Candidate candidate : candidates) {
            if (inside(click.x(), click.y(), x, y, cardWidth, CARD_HEIGHT)) {
                toggle(candidate);
                return true;
            }
            y += CARD_HEIGHT + CARD_GAP;
        }
        int buttonWidth = Math.min(108, Math.max(70, panes.detail().width() / 3));
        int buttonX = panes.detail().right() - 13 - buttonWidth;
        int buttonY = panes.detail().bottom() - ACTION_HEIGHT - 11;
        if (selectedSlots.size() == 3 && inside(click.x(), click.y(), buttonX, buttonY,
                buttonWidth, ACTION_HEIGHT)) {
            String action = "fusion_combine:" + selectedSlots.get(0) + ","
                    + selectedSlots.get(1) + "," + selectedSlots.get(2);
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Pane list = panes(layout()).list();
        if (inside(mouseX, mouseY, list.left(), list.top(), list.width(), list.height())) {
            scroll = Math.max(0, scroll - (int) Math.round(vertical * 32));
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

    private Layout layout() {
        int margin = 7;
        int panelWidth = Math.min(820, Math.max(300, width - margin * 2));
        int panelHeight = Math.min(530, Math.max(230, height - margin * 2));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 2));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 2));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private Panes panes(Layout layout) {
        int left = layout.left() + 14;
        int right = layout.right() - 14;
        int top = layout.top() + 44;
        int bottom = layout.bottom() - 12;
        int contentWidth = right - left;
        if (contentWidth < 340) {
            int listHeight = clamp((bottom - top) * 45 / 100, 82,
                    Math.max(82, bottom - top - 105));
            return new Panes(new Pane(left, top, right, top + listHeight),
                    new Pane(left, top + listHeight + 7, right, bottom));
        }
        int listWidth = clamp(contentWidth * 44 / 100, 150, 300);
        return new Panes(new Pane(left, top, left + listWidth, bottom),
                new Pane(left + listWidth + 8, top, right, bottom));
    }

    private void panel(GuiGraphicsExtractor graphics, Pane pane, int color) {
        graphics.fill(pane.left() - 1, pane.top() - 1, pane.right() + 1, pane.bottom() + 1, BORDER);
        graphics.fill(pane.left(), pane.top(), pane.right(), pane.bottom(), color);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                           int value, int maximum, int visible, int content) {
        if (maximum <= 0 || content <= visible || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(14, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(value, 0, maximum) / maximum;
        graphics.fill(x, top, x + 3, bottom, 0xFFB9AA91);
        graphics.fill(x, y, x + 3, y + thumb, TEAL);
    }

    private String fit(String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, end) + suffix;
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

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Candidate(int slot, String group, String name, String rarity, String itemId) {}
    private record Layout(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }
    }
    private record Pane(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    private record Panes(Pane list, Pane detail) {}
}

package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.client.AugmentIconClient;
import dev.moonseungjun.augmentationsurvival.client.ClientActiveLoadoutState;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.network.ActiveLoadoutEditPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Compact server-backed active augment equipment/order screen. */
public final class ActiveLoadoutScreen extends Screen {
    private static final int BG = 0xF20A0E12;
    private static final int SURFACE = 0xFF111820;
    private static final int SURFACE_2 = 0xFF172027;
    private static final int LINE = 0xFF34414A;
    private static final int LINE_HI = 0xFF71828D;
    private static final int TEXT = 0xFFF1F5F7;
    private static final int MUTED = 0xFF8B989F;
    private static final int ACCENT = 0xFF72C7D7;
    private int selectedSlot = -1;
    private String hoveredId = "";

    public ActiveLoadoutScreen() { super(Component.literal("전투 증강 편성")); }

    @Override
    protected void init() {
        ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.REQUEST, 0, 0, ""));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int w = Math.min(704, width - 30);
        int h = Math.min(382, height - 26);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        hoveredId = "";

        g.fill(0, 0, width, height, 0x8D000000);
        g.fill(x, y, x + w, y + h, BG);
        g.outline(x, y, w, h, 0xFF4B5963);
        g.fill(x + 1, y + 1, x + w - 1, y + 31, 0xFF151D24);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0xFF7B8B95);
        g.text(font, "전투 증강 편성", x + 14, y + 11, TEXT, false);
        String help = "클릭 장착 · 슬롯 두 개 클릭 교환 · 우클릭 해제";
        g.text(font, help, x + w - 14 - font.width(help), y + 11, MUTED, false);

        int leftX = x + 14;
        int leftW = Math.min(306, (w - 42) / 2);
        int rightX = leftX + leftW + 14;
        int rightW = x + w - 14 - rightX;
        int bodyY = y + 43;

        g.text(font, "빠른 사용  ALT + 1~0", leftX, bodyY, 0xFFC7D3D9, false);
        drawSlots(g, leftX, bodyY + 16, leftW, mouseX, mouseY);
        drawSelectedDetail(g, leftX, bodyY + 127, leftW);

        g.text(font, "보유 액티브  " + ClientActiveLoadoutState.available().size(), rightX, bodyY, 0xFFC7D3D9, false);
        drawAvailable(g, rightX, bodyY + 16, rightW, mouseX, mouseY);

        String inspect = !hoveredId.isBlank() ? hoveredId
                : selectedSlot >= 0 ? ClientActiveLoadoutState.slot(selectedSlot) : "";
        if (!inspect.isBlank()) drawDescriptionStrip(g, x + 14, y + h - 58, w - 28, inspect);
        else g.text(font, "액티브를 가리키면 효과 설명을 확인할 수 있습니다.", x + 14, y + h - 35, MUTED, false);
    }

    private void drawSlots(GuiGraphicsExtractor g, int x, int y, int width, int mouseX, int mouseY) {
        int gap = 5;
        int slotW = (width - gap * 4) / 5;
        int slotH = 48;
        for (int i = 0; i < 10; i++) {
            int col = i % 5, row = i / 5;
            int sx = x + col * (slotW + gap), sy = y + row * (slotH + 5);
            String id = ClientActiveLoadoutState.slot(i);
            boolean hover = inside(mouseX, mouseY, sx, sy, slotW, slotH);
            boolean selected = selectedSlot == i;
            g.fill(sx, sy, sx + slotW, sy + slotH, selected ? 0xFF152831 : SURFACE);
            g.outline(sx, sy, slotW, slotH, selected ? ACCENT : hover ? LINE_HI : LINE);
            g.text(font, i == 9 ? "0" : Integer.toString(i + 1), sx + 4, sy + 4, selected ? TEXT : MUTED, false);
            if (id.isBlank()) {
                centered(g, "—", sx + slotW / 2, sy + 18, 0xFF4E5960);
                continue;
            }
            AugmentDefinition d = safe(id);
            if (d != null) g.item(AugmentIconClient.stackFor(id, d.tier()), sx + slotW / 2 - 8, sy + 8);
            centered(g, clip(displayName(id), slotW - 6), sx + slotW / 2, sy + 34, TEXT);
            if (hover) hoveredId = id;
        }
    }

    private void drawSelectedDetail(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 62, 0xB20E1419);
        g.fill(x, y, x + 3, y + 62, selectedSlot >= 0 ? ACCENT : 0xFF303940);
        if (selectedSlot < 0) {
            g.text(font, "슬롯을 선택하세요", x + 12, y + 13, TEXT, false);
            g.text(font, "두 슬롯을 차례로 누르면 위치가 바뀝니다.", x + 12, y + 32, MUTED, false);
            return;
        }
        String id = ClientActiveLoadoutState.slot(selectedSlot);
        g.text(font, "슬롯 " + (selectedSlot == 9 ? "0" : selectedSlot + 1), x + 12, y + 9, ACCENT, false);
        g.text(font, id.isBlank() ? "비어 있음" : displayName(id), x + 12, y + 27, TEXT, false);
        g.text(font, "오른쪽 스킬을 누르면 이 슬롯에 장착됩니다.", x + 12, y + 44, MUTED, false);
    }

    private void drawAvailable(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
        List<String> available = ClientActiveLoadoutState.available();
        if (available.isEmpty()) {
            g.fill(x, y, x + w, y + 76, 0x7A0E1419);
            g.text(font, "아직 획득한 액티브 증강이 없습니다.", x + 12, y + 22, TEXT, false);
            g.text(font, "액티브 증강을 획득하면 여기서 사용 순서를 정할 수 있습니다.", x + 12, y + 43, MUTED, false);
            return;
        }
        int cols = 2, gapX = 6, gapY = 3;
        int cellW = (w - gapX) / 2;
        int cellH = 20;
        for (int i = 0; i < available.size(); i++) {
            int col = i % 2, row = i / 2;
            int cx = x + col * (cellW + gapX), cy = y + row * (cellH + gapY);
            drawAvailableCell(g, cx, cy, cellW, cellH, available.get(i), mouseX, mouseY);
        }
    }

    private void drawAvailableCell(GuiGraphicsExtractor g, int x, int y, int w, int h, String id, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        boolean equipped = ClientActiveLoadoutState.slots().contains(id);
        g.fill(x, y, x + w, y + h, hover ? SURFACE_2 : SURFACE);
        g.fill(x, y, x + 2, y + h, equipped ? ACCENT : hover ? LINE_HI : LINE);
        AugmentDefinition d = safe(id);
        if (d != null) g.item(AugmentIconClient.stackFor(id, d.tier()), x + 4, y + 2);
        g.text(font, clip(displayName(id), w - 50), x + 24, y + 6, hover ? TEXT : 0xFFD2DADF, false);
        if (equipped) {
            String mark = "장착";
            g.text(font, mark, x + w - 6 - font.width(mark), y + 6, ACCENT, false);
        }
        if (hover) hoveredId = id;
    }

    private void drawDescriptionStrip(GuiGraphicsExtractor g, int x, int y, int w, String id) {
        AugmentDefinition d = safe(id);
        if (d == null) return;
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int level = Math.max(1, ClientAugmentProgressState.level(id));
        g.fill(x, y, x + w, y + 44, 0xD60D1217);
        g.fill(x, y, x + 3, y + 44, tierColor(d));
        g.text(font, p.displayName(), x + 12, y + 6, TEXT, false);
        String meta = p.kind().label() + " · Lv." + level + "/" + d.maxLevel();
        g.text(font, meta, x + w - 12 - font.width(meta), y + 6, MUTED, false);
        List<String> lines = wrap(p.summary(), w - 24);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.text(font, lines.get(i), x + 12, y + 20 + i * 10, 0xFFC7D0D5, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int w = Math.min(704, width - 30), h = Math.min(382, height - 26);
        int x = (width - w) / 2, y = (height - h) / 2;
        int leftX = x + 14, leftW = Math.min(306, (w - 42) / 2), bodyY = y + 43;
        int slotY = bodyY + 16, gap = 5, slotW = (leftW - gap * 4) / 5, slotH = 48;
        for (int i = 0; i < 10; i++) {
            int col = i % 5, row = i / 5;
            int sx = leftX + col * (slotW + gap), sy = slotY + row * (slotH + 5);
            if (!inside(event.x(), event.y(), sx, sy, slotW, slotH)) continue;
            if (event.button() == 1) {
                ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.CLEAR, i, 0, ""));
                selectedSlot = -1;
                return true;
            }
            if (event.button() == 0) {
                if (selectedSlot >= 0 && selectedSlot != i) {
                    ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.SWAP, selectedSlot, i, ""));
                    selectedSlot = -1;
                } else selectedSlot = selectedSlot == i ? -1 : i;
                return true;
            }
        }

        if (event.button() == 0 && selectedSlot >= 0) {
            int rightX = leftX + leftW + 14, rightW = x + w - 14 - rightX;
            int listY = bodyY + 16, cellW = (rightW - 6) / 2, cellH = 20;
            List<String> available = ClientActiveLoadoutState.available();
            for (int i = 0; i < available.size(); i++) {
                int col = i % 2, row = i / 2;
                int cx = rightX + col * (cellW + 6), cy = listY + row * 23;
                if (inside(event.x(), event.y(), cx, cy, cellW, cellH)) {
                    ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                            ActiveLoadoutEditPayload.ASSIGN, selectedSlot, 0, available.get(i)
                    ));
                    selectedSlot = -1;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean isPauseScreen() { return false; }

    private AugmentDefinition safe(String id) {
        try { return AugmentCatalog.require(id); } catch (RuntimeException ignored) { return null; }
    }
    private String displayName(String id) {
        AugmentDefinition d = safe(id);
        return d == null ? id.substring(Math.max(0, id.indexOf(':') + 1)) : AugmentPresentationCatalog.get(d).displayName();
    }
    private int tierColor(AugmentDefinition d) {
        return switch (d.tier()) {
            case COMMON -> 0xFFB8BEC5; case UNCOMMON -> 0xFF51C982; case RARE -> 0xFF4596E4;
            case EPIC -> 0xFFAA58D4; case LEGENDARY -> 0xFFE5AA36; case MYTHIC -> 0xFFE54C76;
        };
    }
    private List<String> wrap(String value, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) <= maxWidth) line = new StringBuilder(candidate);
            else {
                if (line.length() > 0) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }
    private String clip(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String result = value;
        while (result.length() > 1 && font.width(result + "…") > maxWidth) result = result.substring(0, result.length() - 1);
        return result + "…";
    }
    private void centered(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(font, s, x - font.width(s) / 2, y, color, false);
    }
    private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
}

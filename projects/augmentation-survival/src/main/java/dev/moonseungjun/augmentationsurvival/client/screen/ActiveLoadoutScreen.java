package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.client.ClientActiveLoadoutState;
import dev.moonseungjun.augmentationsurvival.client.CombatHudClient;
import dev.moonseungjun.augmentationsurvival.network.ActiveLoadoutEditPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Server-backed active augment equipment/order screen. */
public final class ActiveLoadoutScreen extends Screen {
    private static final int PANEL_W = 566, PANEL_H = 300;
    private static final int BG = 0xF20B1015, PANEL = 0xFF111820, METAL = 0xFF44515C;
    private static final int LIGHT = 0xFF9AAAB6, TEXT = 0xFFF0F5F8, MUTED = 0xFF8C99A3;
    private static final int ACCENT = 0xFF7FC9DA, SELECTED = 0xFF1E3540;
    private int selectedSlot = -1;

    public ActiveLoadoutScreen() { super(Component.literal("전투 증강 편성")); }

    @Override
    protected void init() {
        ClientPlayNetworking.send(new ActiveLoadoutEditPayload(ActiveLoadoutEditPayload.REQUEST, 0, 0, ""));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = (width - PANEL_W) / 2, y = (height - PANEL_H) / 2;
        g.fill(x - 4, y - 4, x + PANEL_W + 4, y + PANEL_H + 4, 0xB0000000);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, BG);
        g.outline(x, y, PANEL_W, PANEL_H, METAL);
        g.fill(x + 2, y + 2, x + PANEL_W - 2, y + 31, 0xFF17222B);
        g.fill(x + 3, y + 3, x + PANEL_W - 3, y + 4, LIGHT);
        g.text(font, "전투 증강 편성", x + 16, y + 11, TEXT, false);
        g.text(font, "ALT + 1~0 사용", x + PANEL_W - 105, y + 11, MUTED, false);

        int slotY = y + 45;
        for (int i = 0; i < 10; i++) drawSlot(g, x + 17 + i * 53, slotY, i, mouseX, mouseY);

        g.text(font, "보유 액티브", x + 18, y + 108, TEXT, false);
        g.text(font, "슬롯 선택 → 아래 증강 선택 · 슬롯 두 개를 차례로 누르면 위치 교환 · 우클릭으로 해제",
                x + 102, y + 108, MUTED, false);

        var available = ClientActiveLoadoutState.available();
        for (int i = 0; i < available.size(); i++) {
            int col = i % 3, row = i / 3;
            int ax = x + 18 + col * 178, ay = y + 126 + row * 25;
            drawAvailable(g, ax, ay, 168, available.get(i), mouseX, mouseY);
        }
        if (available.isEmpty()) {
            g.text(font, "아직 획득한 액티브 증강이 없습니다.", x + 18, y + 140, MUTED, false);
        }
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y, int index, int mouseX, int mouseY) {
        String id = ClientActiveLoadoutState.slot(index);
        boolean hover = inside(mouseX, mouseY, x, y, 47, 49);
        boolean selected = selectedSlot == index;
        g.fill(x, y, x + 47, y + 49, 0xFF080C10);
        g.outline(x, y, 47, 49, selected ? ACCENT : hover ? LIGHT : METAL);
        g.fill(x + 2, y + 2, x + 45, y + 47, selected ? SELECTED : PANEL);
        g.text(font, index == 9 ? "0" : Integer.toString(index + 1), x + 4, y + 3, selected ? TEXT : MUTED, false);
        if (!id.isBlank()) {
            CombatHudClient.drawSigil(g, id, x + 14, y + 12, 18, selected ? 0xFFBCEEF4 : 0xFF86C9D8);
            String name = clip(CombatHudClient.shortName(id), 42);
            g.text(font, name, x + 23 - font.width(name) / 2, y + 36, TEXT, false);
        } else {
            g.text(font, "—", x + 20, y + 18, 0xFF46515A, false);
        }
    }

    private void drawAvailable(GuiGraphicsExtractor g, int x, int y, int w, String id, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 20);
        g.fill(x, y, x + w, y + 20, 0xFF0B1015);
        g.outline(x, y, w, 20, hover ? LIGHT : 0xFF303B44);
        if (hover) g.fill(x + 1, y + 1, x + w - 1, y + 19, 0xFF19242C);
        CombatHudClient.drawSigil(g, id, x + 6, y + 5, 10, hover ? 0xFFBCEEF4 : 0xFF75BCCA);
        String name = clip(CombatHudClient.shortName(id), w - 32);
        g.text(font, name, x + 23, y + 6, hover ? TEXT : 0xFFD0D9DE, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int px = (int) event.x(), py = (int) event.y();
        int x = (width - PANEL_W) / 2, y = (height - PANEL_H) / 2;
        int slotY = y + 45;
        for (int i = 0; i < 10; i++) {
            int sx = x + 17 + i * 53;
            if (!inside(px, py, sx, slotY, 47, 49)) continue;
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
            var available = ClientActiveLoadoutState.available();
            for (int i = 0; i < available.size(); i++) {
                int col = i % 3, row = i / 3;
                int ax = x + 18 + col * 178, ay = y + 126 + row * 25;
                if (inside(px, py, ax, ay, 168, 20)) {
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

    private String clip(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String result = value;
        while (result.length() > 1 && font.width(result + "…") > maxWidth) result = result.substring(0, result.length() - 1);
        return result + "…";
    }
    private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
}

package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Compact combat HUD: readable health bar plus the ten server-owned active slots. */
public final class CombatHudClient {
    private static final int DARK = 0xD90A0E12;
    private static final int METAL_LIGHT = 0xFF98A7B2;
    private static final int TEXT = 0xFFEAF1F5;
    private static final int MUTED = 0xFF86939C;
    private static final int HP = 0xFFE34F5F;
    private static final int HP_DARK = 0xFF6E242D;

    private CombatHudClient() { }

    public static void initialize() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "combat_hud"),
                CombatHudClient::extract
        );
    }

    private static void extract(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        drawHealth(g, client, sw / 2 - 112, sh - 94);
        drawSkills(g, client, sw / 2 - 119, sh - 78);
    }

    private static void drawHealth(GuiGraphicsExtractor g, Minecraft client, int x, int y) {
        int w = 224, h = 11;
        float max = Math.max(1.0F, client.player.getMaxHealth());
        float hp = Math.max(0.0F, client.player.getHealth());
        int fill = Math.max(0, Math.min(w - 4, Math.round((w - 4) * hp / max)));
        g.fill(x, y, x + w, y + h, 0xD9080B0E);
        g.outline(x, y, w, h, 0xFF27313A);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, HP_DARK);
        if (fill > 0) g.fill(x + 2, y + 2, x + 2 + fill, y + h - 2, HP);
        if (fill > 2) g.fill(x + 3, y + 2, x + 2 + fill, y + 3, 0xFFFF9AA3);
        String value = Math.round(hp) + " / " + Math.round(max);
        g.text(client.font, value, x + w / 2 - client.font.width(value) / 2, y + 2, TEXT, true);
    }

    private static void drawSkills(GuiGraphicsExtractor g, Minecraft client, int x, int y) {
        int slot = 22, gap = 2;
        for (int i = 0; i < 10; i++) {
            int sx = x + i * (slot + gap);
            String id = ClientActiveLoadoutState.slot(i);
            int cd = ClientActiveLoadoutState.cooldownTicks(i);
            g.fill(sx, y, sx + slot, y + slot, DARK);
            g.outline(sx, y, slot, slot, id.isBlank() ? 0xFF303942 : METAL_LIGHT);
            g.fill(sx + 2, y + 2, sx + slot - 2, y + slot - 2, id.isBlank() ? 0xC90B0F13 : 0xE30E151B);
            g.text(client.font, i == 9 ? "0" : Integer.toString(i + 1), sx + 3, y + 2, id.isBlank() ? 0xFF59636B : TEXT, false);
            if (!id.isBlank()) {
                drawSigil(g, id, sx + 7, y + 8, 9, 0xFF9FD8E7);
                if (cd > 0) {
                    g.fill(sx + 1, y + 1, sx + slot - 1, y + slot - 1, 0xA9000000);
                    String sec = Integer.toString((cd + 19) / 20);
                    g.text(client.font, sec, sx + slot / 2 - client.font.width(sec) / 2, y + 8, TEXT, true);
                }
            }
        }
        g.text(client.font, "ALT", x - 25, y + 7, MUTED, false);
        g.text(client.font, "K 편성", x + 245, y + 7, MUTED, false);
    }

    public static void drawSigil(GuiGraphicsExtractor g, String id, int x, int y, int size, int color) {
        int hash = id.hashCode();
        int cell = Math.max(1, size / 5);
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                int bit = row * 3 + col;
                if (((hash >>> bit) & 1) == 0 && !(row == 2 && col == 2)) continue;
                int px = x + col * cell;
                int py = y + row * cell;
                g.fill(px, py, px + cell, py + cell, color);
                int mirror = 4 - col;
                if (mirror != col) {
                    int mx = x + mirror * cell;
                    g.fill(mx, py, mx + cell, py + cell, color);
                }
            }
        }
    }

    public static String shortName(String id) {
        if (id == null || id.isBlank()) return "빈 슬롯";
        try {
            AugmentDefinition d = AugmentCatalog.require(id);
            return d.canonicalNameKo().replace(" (액티브 · 토글)", "").replace(" (액티브)", "");
        } catch (RuntimeException ignored) {
            int colon = id.indexOf(':');
            return colon >= 0 ? id.substring(colon + 1) : id;
        }
    }
}

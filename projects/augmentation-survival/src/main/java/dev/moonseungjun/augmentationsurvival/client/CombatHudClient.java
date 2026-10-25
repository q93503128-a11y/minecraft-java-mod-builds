package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Combat HUD following the canonical HUD standard: hearts are replaced by a compact numeric liquid
 * health gauge; the ten active slots only expand while the augment-skill modifier is held.
 */
public final class CombatHudClient {
    private static final int TEXT = 0xFFF4F7F9;
    private static final int MUTED = 0xFF89939A;
    private static final int TRACK = 0xD9090C0F;
    private static final int RED_DARK = 0xFF6C2029;
    private static final int RED = 0xFFE04A5A;
    private static final int RED_HI = 0xFFFF8E98;
    private static final int ABSORB = 0xFFF3C84B;
    private static final int ABSORB_HI = 0xFFFFE79A;

    private CombatHudClient() { }

    public static void initialize() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "combat_hud"),
                CombatHudClient::extract
        );
    }

    private static void extract(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null || client.gui.hud.isHidden()) return;
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        drawHealth(g, client, sw / 2 - 91, sh - 40);
        if (ActiveAbilityInputClient.isModeDown()) drawSkills(g, client, sw / 2 - 91, sh - 67);
    }

    private static void drawHealth(GuiGraphicsExtractor g, Minecraft client, int x, int y) {
        int w = 81, h = 9, inner = w - 4;
        float max = Math.max(1.0F, client.player.getMaxHealth());
        float hp = Math.max(0.0F, Math.min(max, client.player.getHealth()));
        float absorption = Math.max(0.0F, client.player.getAbsorptionAmount());
        int hpFill = Math.max(0, Math.min(inner, Math.round(inner * hp / max)));
        int absorbFill = Math.max(0, Math.min(inner, Math.round(inner * absorption / max)));

        g.fill(x, y, x + w, y + h, TRACK);
        g.outline(x, y, w, h, 0xFF313940);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, RED_DARK);
        if (hpFill > 0) {
            g.fill(x + 2, y + 2, x + 2 + hpFill, y + h - 2, RED);
            drawLiquidCrest(g, x + 2, y + 2, hpFill, RED_HI);
        }
        if (absorbFill > 0) {
            int start = Math.max(x + 2, x + 2 + hpFill - absorbFill);
            int end = Math.min(x + w - 2, start + absorbFill);
            g.fill(start, y + 2, end, y + 4, ABSORB);
            if (end > start + 2) g.fill(start + 1, y + 2, end - 1, y + 3, ABSORB_HI);
        }
        String value = compact(hp) + " / " + compact(max);
        g.text(client.font, value, x + w / 2 - client.font.width(value) / 2, y + 1, TEXT, true);
    }

    private static void drawLiquidCrest(GuiGraphicsExtractor g, int x, int y, int width, int color) {
        if (width < 3) return;
        int phase = (int) ((System.currentTimeMillis() / 170L) % 6L);
        for (int px = 1; px < width - 1; px += 3) {
            int wave = ((px + phase) % 6) < 3 ? 0 : 1;
            g.fill(x + px, y + wave, x + Math.min(width, px + 2), y + wave + 1, color);
        }
    }

    private static void drawSkills(GuiGraphicsExtractor g, Minecraft client, int x, int y) {
        int slot = 17, gap = 1;
        g.fill(x - 3, y - 12, x + 10 * (slot + gap) + 2, y + slot + 3, 0xA9080B0E);
        g.text(client.font, "증강 스킬", x, y - 10, 0xFFDCE5EA, false);
        for (int i = 0; i < 10; i++) {
            int sx = x + i * (slot + gap);
            String id = ClientActiveLoadoutState.slot(i);
            int cd = ClientActiveLoadoutState.cooldownTicks(i);
            boolean empty = id.isBlank();
            g.fill(sx, y, sx + slot, y + slot, empty ? 0xD90A0D10 : 0xE312181D);
            g.outline(sx, y, slot, slot, empty ? 0xFF30363B : 0xFF778993);
            if (!empty) {
                var definition = safeDefinition(id);
                if (definition != null) g.item(AugmentIconClient.stackFor(id, definition.tier()), sx + 1, y + 1);
                if (cd > 0) {
                    g.fill(sx, y, sx + slot, y + slot, 0xA9000000);
                    String sec = Integer.toString((cd + 19) / 20);
                    g.text(client.font, sec, sx + slot / 2 - client.font.width(sec) / 2, y + 5, TEXT, true);
                }
            }
            String key = i == 9 ? "0" : Integer.toString(i + 1);
            g.text(client.font, key, sx + 1, y - 8, empty ? 0xFF596168 : MUTED, false);
        }
    }

    private static dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition safeDefinition(String id) {
        try { return dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog.require(id); }
        catch (RuntimeException ignored) { return null; }
    }

    public static String shortName(String id) {
        if (id == null || id.isBlank()) return "빈 슬롯";
        var definition = safeDefinition(id);
        if (definition == null) return id.substring(Math.max(0, id.indexOf(':') + 1));
        return dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog.get(definition).displayName();
    }

    private static String compact(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05F) return Integer.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}

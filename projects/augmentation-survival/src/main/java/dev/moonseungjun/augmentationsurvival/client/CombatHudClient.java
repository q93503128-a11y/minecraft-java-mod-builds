package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Canonical combat HUD: health, server-confirmed active feedback and the ten-slot skill strip. */
public final class CombatHudClient {
    private static final int TEXT = 0xFFF4F7F9;
    private static final int MUTED = 0xFF89939A;
    private static final int TRACK = 0xD9090C0F;
    private static final int RED_DARK = 0xFF6C2029;
    private static final int RED = 0xFFE04A5A;
    private static final int RED_HI = 0xFFFF8E98;
    private static final int ABSORB = 0xFFF3C84B;
    private static final int ABSORB_HI = 0xFFFFE79A;
    private static final int SHIELD_DARK = 0xFF174B66;
    private static final int SHIELD = 0xFF3DA9DA;
    private static final int SHIELD_HI = 0xFF9BE7FF;
    private static final int ACTIVE_BORDER = 0xDD66747D;
    private static final int ACTIVE_FILL = 0xFF5FB7D6;
    private static final int CHANNEL_FILL = 0xFFE8C56C;
    private static final int TIME_EDGE = 0xB09BE7FF;
    private static final int STATUS_BACK = 0xC80A0E12;
    private static final String TIME_STOP_ID = AugmentationSurvival.MOD_ID + ":time_stop";
    private static final String THREAT_SUPPRESSION_ID = AugmentationSurvival.MOD_ID + ":threat_suppression";
    private static final String TIME_ACCELERATION_ID = AugmentationSurvival.MOD_ID + ":time_acceleration";
    private static final String AREA_ACCELERATION_ID = AugmentationSurvival.MOD_ID + ":area_acceleration";
    private static final List<String> CORNER_STATUS_IDS = List.of(
            THREAT_SUPPRESSION_ID, TIME_ACCELERATION_ID, AREA_ACCELERATION_ID
    );

    private CombatHudClient() { }

    public static void initialize() {
        HudElementRegistry.replaceElement(
                VanillaHudElements.HEALTH_BAR,
                original -> CombatHudClient::extractHealth
        );
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "active_slots"),
                CombatHudClient::extractSkills
        );
    }

    private static void extractHealth(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (!canRender(client)) return;
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        drawHealth(g, client, sw / 2 - 91, sh - 39);
    }

    private static void extractSkills(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (!canRender(client)) return;
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        boolean mode = ActiveAbilityInputClient.isModeDown();
        if (mode) drawSkills(g, client, sw / 2 - 89, sh - 67);
        drawActiveFeedback(g, client, sw, sh, mode);
    }

    private static boolean canRender(Minecraft client) {
        return client.player != null && client.gui.screen() == null && !client.gui.hud.isHidden();
    }

    private static void drawHealth(GuiGraphicsExtractor g, Minecraft client, int x, int y) {
        int w = 81, h = 9, inner = w - 4;
        float max = Math.max(1.0F, client.player.getMaxHealth());
        float hp = Math.max(0.0F, Math.min(max, client.player.getHealth()));
        float vanillaAbsorption = Math.max(0.0F, client.player.getAbsorptionAmount());
        float augmentAbsorption = Math.max(0.0F, ClientProtectionHudState.augmentAbsorption());
        float absorption = vanillaAbsorption + augmentAbsorption;
        int hpFill = Math.max(0, Math.min(inner, Math.round(inner * hp / max)));
        int absorbFill = Math.max(0, Math.min(inner, Math.round(inner * absorption / max)));

        drawShield(g, x, y - 5, w);

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
            if (end > start) {
                g.fill(start, y + 2, end, y + 4, ABSORB);
                if (end > start + 2) g.fill(start + 1, y + 2, end - 1, y + 3, ABSORB_HI);
            }
        }
        String value = compact(hp) + " / " + compact(max);
        g.text(client.font, value, x + w / 2 - client.font.width(value) / 2, y + 1, TEXT, true);
    }

    private static void drawShield(GuiGraphicsExtractor g, int x, int y, int w) {
        float shield = ClientProtectionHudState.shield();
        float cap = ClientProtectionHudState.shieldCapacity();
        if (!(shield > 0.0F) || !(cap > 0.0F)) return;
        int inner = w - 4;
        int fill = Math.max(1, Math.min(inner, Math.round(inner * shield / cap)));
        g.fill(x, y, x + w, y + 4, TRACK);
        g.outline(x, y, w, 4, 0xFF28566B);
        g.fill(x + 2, y + 1, x + w - 2, y + 3, SHIELD_DARK);
        g.fill(x + 2, y + 1, x + 2 + fill, y + 3, SHIELD);
        if (fill > 3) g.fill(x + 3, y + 1, x + 1 + fill, y + 2, SHIELD_HI);
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
        int slot = 16, gap = 2;
        for (int i = 0; i < 10; i++) {
            int sx = x + i * (slot + gap);
            String id = ClientActiveLoadoutState.slot(i);
            int cd = ClientActiveLoadoutState.cooldownTicks(i);
            int maxCharges = ClientActiveLoadoutState.maxCharges(i);
            int readyCharges = ClientActiveLoadoutState.readyCharges(i);
            int nextRecharge = ClientActiveLoadoutState.nextRechargeTicks(i);
            boolean empty = id.isBlank();
            g.fill(sx, y, sx + slot, y + slot, empty ? 0xB2080B0E : 0xD9101519);
            g.outline(sx, y, slot, slot, empty ? 0xAA313940 : 0xDD7A8992);
            if (!empty) {
                var definition = safeDefinition(id);
                if (definition != null) g.item(AugmentIconClient.stackFor(id, definition.tier()), sx, y);

                if (maxCharges > 1) {
                    if (readyCharges <= 0) {
                        g.fill(sx, y, sx + slot, y + slot, 0xA8000000);
                        if (nextRecharge > 0) {
                            String sec = Integer.toString((nextRecharge + 19) / 20);
                            g.text(client.font, sec,
                                    sx + slot / 2 - client.font.width(sec) / 2, y + 4, TEXT, true);
                        }
                    } else if (readyCharges < maxCharges && nextRecharge > 0) {
                        String sec = Integer.toString((nextRecharge + 19) / 20);
                        g.text(client.font, sec, sx + slot - client.font.width(sec) - 1, y + 1, TEXT, true);
                    }
                    String charges = readyCharges + "/" + maxCharges;
                    g.text(client.font, charges, sx + slot - client.font.width(charges) - 1,
                            y + slot - 7, readyCharges > 0 ? TEXT : MUTED, true);
                } else if (cd > 0) {
                    g.fill(sx, y, sx + slot, y + slot, 0xA8000000);
                    String sec = Integer.toString((cd + 19) / 20);
                    g.text(client.font, sec, sx + slot / 2 - client.font.width(sec) / 2, y + 4, TEXT, true);
                }
            }
            String key = i == 9 ? "0" : Integer.toString(i + 1);
            g.text(client.font, key, sx + 1, y + slot - 7, empty ? 0xFF586168 : MUTED, true);
        }
    }

    private static void drawActiveFeedback(
            GuiGraphicsExtractor g,
            Minecraft client,
            int screenWidth,
            int screenHeight,
            boolean skillStripVisible
    ) {
        if (ClientActivePresentationState.isActive(TIME_STOP_ID)) {
            drawTimeStopEdge(g, screenWidth, screenHeight);
        }

        List<ClientActivePresentationState.Entry> allEntries = ClientActivePresentationState.entries();
        drawCornerStatuses(g, client, screenWidth, allEntries);

        List<ClientActivePresentationState.Entry> entries = allEntries.stream()
                .filter(entry -> !isCornerStatus(entry.abilityId()))
                .toList();
        int shown = Math.min(3, entries.size());
        int y = screenHeight - (skillStripVisible ? 82 : 62) - shown * 12;
        for (int i = Math.max(0, entries.size() - shown); i < entries.size(); i++) {
            ClientActivePresentationState.Entry entry = entries.get(i);
            drawActiveEntry(g, client, screenWidth, y, entry);
            y += 12;
        }

        ClientActivePresentationState.Banner banner = ClientActivePresentationState.banner();
        if (banner.visible() && !isCornerStatus(banner.abilityId())) {
            String text = shortName(banner.abilityId()) + phaseSuffix(banner.phase());
            int width = client.font.width(text) + 12;
            int bx = screenWidth / 2 - width / 2;
            int by = screenHeight - (skillStripVisible ? 112 : 92) - shown * 12;
            g.fill(bx, by, bx + width, by + 11, 0xD90A0E12);
            g.outline(bx, by, width, 11, ACTIVE_BORDER);
            g.text(client.font, text, bx + 6, by + 2, TEXT, true);
        }
    }

    private static void drawCornerStatuses(
            GuiGraphicsExtractor g,
            Minecraft client,
            int screenWidth,
            List<ClientActivePresentationState.Entry> entries
    ) {
        int y = 8;
        for (String id : CORNER_STATUS_IDS) {
            ClientActivePresentationState.Entry entry = entries.stream()
                    .filter(candidate -> id.equals(candidate.abilityId()))
                    .findFirst()
                    .orElse(null);
            if (entry == null) continue;
            var definition = safeDefinition(id);
            if (definition == null) continue;

            int ticks = entry.remainingTicks();
            String suffix = ticks >= 0 ? " " + Math.max(1, (ticks + 19) / 20) + "s" : " ON";
            String label = shortName(id) + suffix;
            int textWidth = client.font.width(label);
            int width = Math.min(116, 23 + textWidth);
            int x = screenWidth - width - 8;

            g.fill(x, y, x + width, y + 18, STATUS_BACK);
            g.fill(x, y, x + 2, y + 18, ACTIVE_FILL);
            g.item(AugmentIconClient.stackFor(id, definition.tier()), x + 3, y + 1);
            g.text(client.font, label, x + 21, y + 5, TEXT, true);
            y += 21;
        }
    }

    private static boolean isCornerStatus(String abilityId) {
        return CORNER_STATUS_IDS.contains(abilityId);
    }

    private static void drawActiveEntry(
            GuiGraphicsExtractor g,
            Minecraft client,
            int screenWidth,
            int y,
            ClientActivePresentationState.Entry entry
    ) {
        int ticks = entry.remainingTicks();
        String state = entry.phase() == ActivePresentationPayload.CHANNEL ? "집중" : "활성";
        String time = ticks >= 0 ? " " + String.format(Locale.ROOT, "%.1fs", ticks / 20.0D) : "";
        String radius = entry.radius() > 0.0F ? " · " + Math.round(entry.radius()) + "m" : "";
        String text = shortName(entry.abilityId()) + "  " + state + time + radius;
        int width = Math.max(88, Math.min(172, client.font.width(text) + 12));
        int x = screenWidth / 2 - width / 2;
        g.fill(x, y, x + width, y + 10, 0xD90A0E12);
        g.outline(x, y, width, 10, ACTIVE_BORDER);
        g.text(client.font, text, x + 5, y + 1, TEXT, true);
        if (entry.durationMillis() > 0L && entry.expiresAtMillis() != Long.MAX_VALUE) {
            int inner = width - 2;
            int fill = Math.max(0, Math.min(inner, Math.round(inner * entry.remainingFraction())));
            int color = entry.phase() == ActivePresentationPayload.CHANNEL ? CHANNEL_FILL : ACTIVE_FILL;
            if (fill > 0) g.fill(x + 1, y + 8, x + 1 + fill, y + 9, color);
        }
    }

    private static void drawTimeStopEdge(GuiGraphicsExtractor g, int width, int height) {
        int thickness = 2;
        g.fill(0, 0, width, thickness, TIME_EDGE);
        g.fill(0, height - thickness, width, height, TIME_EDGE);
        g.fill(0, thickness, thickness, height - thickness, TIME_EDGE);
        g.fill(width - thickness, thickness, width, height - thickness, TIME_EDGE);
    }

    private static String phaseSuffix(int phase) {
        return switch (phase) {
            case ActivePresentationPayload.CHANNEL -> " 집중";
            case ActivePresentationPayload.SUSTAINED -> " 활성";
            case ActivePresentationPayload.TOGGLE_ON -> " ON";
            case ActivePresentationPayload.TOGGLE_OFF -> " OFF";
            case ActivePresentationPayload.END -> " 종료";
            case ActivePresentationPayload.CANCEL -> " 취소";
            default -> " 발동";
        };
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
        return String.format(Locale.ROOT, "%.1f", value);
    }
}

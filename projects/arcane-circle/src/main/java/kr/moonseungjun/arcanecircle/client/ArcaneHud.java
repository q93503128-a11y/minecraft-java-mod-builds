package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

public final class ArcaneHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "spell_hotbar");

    private ArcaneHud() {}

    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ArcaneHud::renderWorldHud);
    }

    private static void renderWorldHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || !ArcaneClientState.ready()) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        int gap = width >= 520 ? 6 : 5;
        int slotSize = width >= 520 ? 25 : width >= 360 ? 23 : 21;
        int total = slotSize * 5 + gap * 4;
        int startX = Math.max(4, (width - total) / 2);
        int y = Math.max(8, height - slotSize - 54);
        drawMana(g, font, startX, y);
        for (int slot = 0; slot < 5; slot++) {
            drawSlot(g, font, startX + slot * (slotSize + gap), y, slotSize, slot);
        }
        drawHealth(g, font, width, height);
        drawFusionQueue(g, font, width, y - 15);
        drawRaisedNotice(g, font, width, y - 42);
    }


    public static void onVanillaLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && ArcaneClientState.ready()
                && VanillaGuiLayers.PLAYER_HEALTH.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    private static void drawHealth(GuiGraphicsExtractor g, Font font, int width, int height) {
        int health = Math.max(0, ArcaneClientState.integer("health", 0));
        int maximum = Math.max(1, ArcaneClientState.integer("health_max", 100));
        int absorption = Math.max(0, ArcaneClientState.integer("absorption", 0));
        double ratio = Math.max(0.0, Math.min(1.0, health / (double) maximum));
        double absorptionRatio = Math.max(0.0, Math.min(1.0, absorption / (double) maximum));

        // Vanilla hearts occupied the left half above the hotbar. Keep the replacement there.
        int barW = 81;
        int barH = 7;
        int x = width / 2 - 91;
        int y = height - 39;
        int innerW = barW - 4;
        int redFill = (int) Math.round(innerW * ratio);
        int goldFill = (int) Math.round(innerW * absorptionRatio);

        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xF4000000);
        g.fill(x, y, x + barW, y + barH, 0xF00B0205);
        g.fill(x + 2, y + 2, x + 2 + redFill, y + barH - 1,
                ratio <= 0.22 ? 0xFF8E0715 : ratio <= 0.48 ? 0xFFAA0A1D : 0xFFBB0D22);
        if (redFill > 0) {
            g.fill(x + 2, y + 2, x + 2 + redFill, y + 3, 0xFFD52A39);
            int phase = (int) ((System.nanoTime() / 70_000_000L) % 12L);
            for (int stripe = phase - 12; stripe < redFill; stripe += 12) {
                int sx = x + 2 + stripe;
                if (sx >= x + 2 && sx < x + 2 + redFill) {
                    g.fill(sx, y + 3, Math.min(sx + 2, x + 2 + redFill), y + barH - 1, 0xFFCF1830);
                }
            }
        }

        if (goldFill > 0) {
            int goldEnd = Math.min(innerW, goldFill);
            g.fill(x + 2, y + 2, x + 2 + goldEnd, y + barH - 1, 0xFFE0A512);
            g.fill(x + 2, y + 2, x + 2 + goldEnd, y + 3, 0xFFFFE16B);
        }

        String label = absorption > 0
                ? health + "/" + maximum + " +" + absorption
                : health + "/" + maximum;
        tinyText(g, font, label, x + barW / 2, y - 6, 0xFFF5E9EC, 0.52F, true);
    }

    private static void drawMana(GuiGraphicsExtractor g, Font font, int startX, int y) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int w = Math.min(62, Math.max(42, startX - 10));
        int x = Math.max(4, startX - w - 7);
        int fill = (int) Math.round((w - 2) * Math.min(1.0, mana / (double) max));
        tinyText(g, font, ArcaneClientState.integer("circle", 1) + "C " + mana + "/" + max,
                x, y + 3, 0xFFE7DDF7, 0.62F, false);
        g.fill(x, y + 13, x + w, y + 17, 0xD9050912);
        g.fill(x + 1, y + 14, x + 1 + fill, y + 16, 0xEF5E8EEB);
    }

    private static void drawSlot(GuiGraphicsExtractor g, Font font, int x, int y, int size, int slot) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        int color = spell == null ? 0xFF606475 : ArcaneRenderUtil.schoolColor(spell.school());
        int dark = spell == null ? 0xFF121620 : ArcaneRenderUtil.schoolDark(spell.school());
        int remaining = ArcaneClientState.cooldownRemainingTicks(slot);
        boolean charging = ArcaneClientState.isChargingSlot(slot);
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, charging ? 0xFFFFD36B : 0xD9040610);
        g.fill(x, y, x + size, y + size, remaining > 0 ? dark : 0xEB101827);
        g.fill(x, y + size - 2, x + size, y + size, color);
        tinyText(g, font, Integer.toString(slot + 1), x + 2, y + 1, 0xFF98A3B7, 0.55F, false);
        if (spell == null) return;
        String archetype = kr.moonseungjun.arcanecircle.magic.SpellArchetype.mode(spell.id()).badge();
        tinyText(g, font, archetype, x + size - 3, y + 1, 0xFFC8B7DA, 0.48F, true);
        int iconY = y + 10;
        ArcaneRenderUtil.ring(g, x + size / 2, iconY, Math.max(4, size / 5), remaining > 0 ? 0xFF686A74 : color);
        ArcaneRenderUtil.spellRune(g, x + size / 2, iconY, spell, Math.max(3, size / 7),
                remaining > 0 ? 0xFF777A84 : 0xFFF8F2FF);
        String name = fitName(font, spell.name(), (size - 2) * 2);
        tinyText(g, font, name, x + size / 2, y + size - 7,
                remaining > 0 ? 0xFF7E7F88 : charging ? 0xFFFFE0A2 : 0xFFE7DFEC, 0.50F, true);
        if (remaining > 0) {
            String seconds = remaining >= 200 ? Integer.toString((int) Math.ceil(remaining / 20.0))
                    : String.format("%.1f", remaining / 20.0);
            tinyText(g, font, seconds, x + size / 2, iconY - 2, 0xFFFFFFFF, 0.55F, true);
            int fill = (int) Math.round((size - 2) * ArcaneClientState.cooldownFraction(slot));
            g.fill(x + 1, y + size - 3, x + 1 + fill, y + size - 1, 0xFFE46D78);
        } else if (charging) {
            int fill = (int) Math.round((size - 2) * ArcaneClientState.chargingFraction());
            g.fill(x + 1, y + size - 3, x + 1 + fill, y + size - 1,
                    ArcaneClientState.chargingReady() ? 0xFFFFD36B : color);
        }
    }

    private static void drawRaisedNotice(GuiGraphicsExtractor g, Font font, int width, int y) {
        if (!ArcaneClientState.noticeVisible()) return;
        String raw = ArcaneClientState.noticeText();
        int split = raw.length() > 46 ? raw.lastIndexOf(' ', 46) : -1;
        if (split < 18 && raw.length() > 46) split = 46;
        String first = split > 0 ? raw.substring(0, split).trim() : raw;
        String second = split > 0 ? raw.substring(split).trim() : "";
        first = compactName(first, 58);
        second = compactName(second, 58);
        int textW = Math.max(font.width(first), font.width(second));
        int boxW = Math.min(width - 16, Math.max(120, textW + 18));
        int boxH = second.isBlank() ? 17 : 28;
        int x = (width - boxW) / 2;
        g.fill(x, y, x + boxW, y + boxH, 0xE6080B16);
        g.fill(x, y, x + boxW, y + 2, 0xFFB67ADE);
        g.centeredText(font, Component.literal(first), width / 2, y + 5, 0xFFF4E9FA);
        if (!second.isBlank()) g.centeredText(font, Component.literal(second), width / 2, y + 16, 0xFFD6C8E3);
    }

    private static void drawFusionQueue(GuiGraphicsExtractor g, Font font, int width, int y) {
        List<String> queue = ArcaneClientState.queue();
        if (queue.isEmpty()) return;
        String result = ArcaneClientState.queueResult();
        int boxWidth = Math.min(width - 12, 190);
        int x = (width - boxWidth) / 2;
        g.fill(x, y, x + boxWidth, y + 11, 0xED080B16);
        double progress = ArcaneClientState.fusionChargingFraction();
        int fill = (int) Math.round(boxWidth * progress);
        g.fill(x, y, x + fill, y + 2, result.isBlank() ? 0xFF7E67AD : 0xFFFFC861);
        String chain = queue.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .reduce((a, b) -> a + "+" + b).orElse("");
        String suffix = result.isBlank() ? "" : "→" + SpellCatalog.spell(result).map(SpellDefinition::name).orElse(result);
        tinyText(g, font, compactName(chain + suffix, 34), width / 2, y + 3,
                result.isBlank() ? 0xFFD4B8F1 : ArcaneClientState.fusionChargingReady() ? 0xFFFFE5A1 : 0xFFFFD889,
                0.58F, true);
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) || !ArcaneClientState.ready()) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = minecraft.font;
        int inventoryRight = width / 2 + 88;
        int inventoryLeft = width / 2 - 88;
        int sideSpace = Math.max(inventoryLeft, width - inventoryRight);
        if (sideSpace < 142) return;
        int panelW = Math.min(154, sideSpace - 12);
        int x = inventoryLeft - panelW - 7;
        if (x < 5) x = inventoryRight + 7;
        int y = Math.max(5, (height - 104) / 2);
        panel(g, x, y, panelW, 104, "마력핵");
        int lineY = y + 27;
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C  MP "
                + ArcaneClientState.integer("mana", 0) + "/" + ArcaneClientState.integer("max", 100)),
                x + 7, lineY, 0xFFC9D8F2);
        g.text(font, Component.literal("회복 " + String.format("%.1f", ArcaneClientState.regenPerSecond()) + "/초"),
                x + 7, lineY + 14, 0xFF8ED6C0);
        g.text(font, Component.literal(compactName(ArcaneClientState.text("staff", "맨손"), 18)),
                x + 7, lineY + 30, 0xFFFFD58D);
        g.text(font, Component.literal("C 마도서 · 1~5 시전 · 숫자키 조합 융합"), x + 7, y + 85, 0xFF81778F);
    }

    private static void tinyText(GuiGraphicsExtractor g, Font font, String text, int x, int y,
                                 int color, float scale, boolean centered) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        if (centered) g.centeredText(font, Component.literal(text), 0, 0, color);
        else g.text(font, Component.literal(text), 0, 0, color);
        g.pose().popMatrix();
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF604779);
        g.fill(x, y, x + w, y + h, 0xF20A0F1D);
        g.fill(x + 3, y + 3, x + w - 3, y + 21, 0xD1241A38);
        g.centeredText(Minecraft.getInstance().font, Component.literal(title), x + w / 2, y + 8, 0xFFEAD9FF);
    }

    private static String fitName(Font font, String value, int pixels) {
        if (value == null || pixels <= 0) return "";
        if (font.width(value) <= pixels) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > pixels) end--;
        return end <= 0 ? suffix : value.substring(0, end) + suffix;
    }

    private static String compactName(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }
}

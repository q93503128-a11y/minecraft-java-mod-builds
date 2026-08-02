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
import net.neoforged.neoforge.client.event.ScreenEvent;

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

        drawCastingSigil(g, width, height);

        int gap = 2;
        int slotSize = width >= 520 ? 36 : width >= 360 ? 32 : 28;
        int total = slotSize * 5 + gap * 4;
        int startX = Math.max(4, (width - total) / 2);
        int y = Math.max(8, height - slotSize - 58);
        drawMana(g, font, startX, y, slotSize);
        for (int slot = 0; slot < 5; slot++) {
            drawSlot(g, font, startX + slot * (slotSize + gap), y, slotSize, slot);
        }
        drawFusionQueue(g, font, width, y - 21);
    }

    private static void drawMana(GuiGraphicsExtractor g, Font font, int startX, int y, int size) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int w = Math.min(72, Math.max(48, startX - 10));
        int x = Math.max(4, startX - w - 6);
        int fill = (int) Math.round((w - 2) * Math.min(1.0, mana / (double) max));
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C " + mana + "/" + max),
                x, y + 5, 0xFFE7DDF7);
        g.fill(x, y + 19, x + w, y + 24, 0xD9050912);
        g.fill(x + 1, y + 20, x + 1 + fill, y + 23, 0xEF5E8EEB);
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
        g.text(font, Component.literal(Integer.toString(slot + 1)), x + 2, y + 1, 0xFF98A3B7);

        if (spell == null) return;
        int iconY = y + 13;
        ArcaneRenderUtil.ring(g, x + size / 2, iconY, Math.max(6, size / 5), remaining > 0 ? 0xFF686A74 : color);
        ArcaneRenderUtil.spellRune(g, x + size / 2, iconY, spell, Math.max(4, size / 7),
                remaining > 0 ? 0xFF777A84 : 0xFFF8F2FF);
        String name = fitName(font, spell.name(), size - 3);
        g.centeredText(font, Component.literal(name), x + size / 2, y + size - 10,
                remaining > 0 ? 0xFF7E7F88 : charging ? 0xFFFFE0A2 : 0xFFD8D1E1);

        if (remaining > 0) {
            String seconds = remaining >= 200 ? Integer.toString((int) Math.ceil(remaining / 20.0))
                    : String.format("%.1f", remaining / 20.0);
            g.centeredText(font, Component.literal(seconds), x + size / 2, iconY - 4, 0xFFFFFFFF);
            int fill = (int) Math.round((size - 2) * ArcaneClientState.cooldownFraction(slot));
            g.fill(x + 1, y + size - 3, x + 1 + fill, y + size - 1, 0xFFE46D78);
        } else if (charging) {
            int fill = (int) Math.round((size - 2) * ArcaneClientState.chargingFraction());
            g.fill(x + 1, y + size - 3, x + 1 + fill, y + size - 1,
                    ArcaneClientState.chargingReady() ? 0xFFFFD36B : color);
        }
    }

    /** Screen-space vector seal: exact circle count, progressive line construction, zero particles. */
    private static void drawCastingSigil(GuiGraphicsExtractor g, int width, int height) {
        String id = ArcaneClientState.chargingSpell();
        SpellDefinition spell = SpellCatalog.spell(id).orElse(null);
        if (spell == null) return;
        double progress = Math.max(0.0, Math.min(1.0, ArcaneClientState.chargingFraction()));
        int cx = width / 2;
        int cy = height / 2 + 18;
        int radius = Math.min(34, 13 + spell.circle() * 2);
        int color = ArcaneRenderUtil.schoolColor(spell.school());
        int faint = (color & 0x00FFFFFF) | 0x88000000;

        // 1C = one concentric boundary, 2C = two, and so on through 9C.
        for (int ring = 0; ring < spell.circle(); ring++) {
            int r = Math.max(5, radius - ring * Math.max(2, radius / 12));
            double local = Math.max(0.0, Math.min(1.0, progress * spell.circle() - ring));
            partialRing(g, cx, cy, r, local, ring == 0 ? color : faint);
        }
        if (progress < 0.18) return;

        int inner = Math.max(5, radius / 2);
        ArcaneRenderUtil.spellRune(g, cx, cy, spell, inner, progress >= 1.0 ? 0xFFFFFFFF : color);
        int spokes = Math.min(12, 3 + spell.circle());
        int completed = (int) Math.floor(spokes * Math.min(1.0, Math.max(0.0, (progress - 0.22) / 0.58)));
        for (int i = 0; i < completed; i++) {
            double a = Math.PI * 2.0 * i / spokes - Math.PI / 2.0;
            int x1 = cx + (int) Math.round(Math.cos(a) * (inner + 2));
            int y1 = cy + (int) Math.round(Math.sin(a) * (inner + 2));
            int x2 = cx + (int) Math.round(Math.cos(a) * (radius - 1));
            int y2 = cy + (int) Math.round(Math.sin(a) * (radius - 1));
            ArcaneRenderUtil.line(g, x1, y1, x2, y2, faint);
        }

        if (spell.circle() >= 3 && progress >= 0.62) {
            int satellites = Math.min(6, spell.circle() - 1);
            for (int i = 0; i < satellites; i++) {
                double a = Math.PI * 2.0 * i / satellites - Math.PI / 2.0;
                int sx = cx + (int) Math.round(Math.cos(a) * (radius + 6));
                int sy = cy + (int) Math.round(Math.sin(a) * (radius + 6));
                ArcaneRenderUtil.ring(g, sx, sy, 3 + spell.circle() / 4, faint);
                ArcaneRenderUtil.diamond(g, sx, sy, 2, color);
            }
        }
        if (progress >= 1.0) ArcaneRenderUtil.ring(g, cx, cy, radius + 2, 0xFFFFE7A3);
    }

    private static void partialRing(GuiGraphicsExtractor g, int cx, int cy, int radius, double progress, int color) {
        int points = Math.max(32, radius * 7);
        int shown = (int) Math.ceil(points * progress);
        for (int i = 0; i < shown; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / points;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawFusionQueue(GuiGraphicsExtractor g, Font font, int width, int y) {
        List<String> queue = ArcaneClientState.queue();
        if (queue.isEmpty()) return;
        String result = ArcaneClientState.queueResult();
        int boxWidth = Math.min(width - 12, 230);
        int x = (width - boxWidth) / 2;
        g.fill(x, y, x + boxWidth, y + 16, 0xED080B16);
        g.fill(x, y, x + boxWidth, y + 2, result.isBlank() ? 0xFF7E67AD : 0xFFFFC861);
        String chain = queue.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .reduce((a, b) -> a + "+" + b).orElse("");
        String suffix = result.isBlank() ? "" : "→" + SpellCatalog.spell(result).map(SpellDefinition::name).orElse(result);
        g.centeredText(font, Component.literal(compactName("X " + chain + suffix, 28)), width / 2, y + 4,
                result.isBlank() ? 0xFFD4B8F1 : 0xFFFFD889);
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
        int x = inventoryRight + 7;
        if (x + panelW > width - 5) x = inventoryLeft - panelW - 7;
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
        g.text(font, Component.literal("C 마도서 · 1~5 시전 · X 융합"), x + 7, y + 85, 0xFF81778F);
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

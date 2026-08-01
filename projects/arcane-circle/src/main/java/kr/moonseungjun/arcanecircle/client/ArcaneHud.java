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

        int gap = width < 360 ? 2 : 4;
        int slotSize = Math.max(24, Math.min(38, (width - 16 - gap * 4) / 5));
        int total = slotSize * 5 + gap * 4;
        int startX = Math.max(4, (width - total) / 2);
        int y = Math.max(8, height - slotSize - 29);

        if (width >= 500) drawManaSide(g, font, startX, y, slotSize);
        else drawManaTop(g, font, width, y - 13);

        for (int slot = 0; slot < 5; slot++) {
            drawSlot(g, font, startX + slot * (slotSize + gap), y, slotSize, slot);
        }
        drawFusionQueue(g, font, width, y - (width >= 500 ? 25 : 39));
    }

    private static void drawManaSide(GuiGraphicsExtractor g, Font font, int startX, int y, int slotSize) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int barWidth = Math.min(110, Math.max(68, startX - 14));
        int x = Math.max(6, startX - barWidth - 8);
        int fill = (int) Math.round((barWidth - 2) * Math.min(1.0, mana / (double) max));
        g.fill(x, y + slotSize - 9, x + barWidth, y + slotSize - 2, 0xDC050912);
        g.fill(x + 1, y + slotSize - 8, x + 1 + fill, y + slotSize - 3, 0xEF5E8EEB);
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C  " + mana + "/" + max),
                x, y + slotSize - 21, 0xFFE7DDF7);
        g.text(font, Component.literal(compactName(ArcaneClientState.text("staff", "맨손"), 12)),
                x, y + 1, 0xFFFFD489);
    }

    private static void drawManaTop(GuiGraphicsExtractor g, Font font, int width, int y) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int barWidth = Math.min(170, Math.max(100, width - 70));
        int x = (width - barWidth) / 2;
        int fill = (int) Math.round((barWidth - 2) * Math.min(1.0, mana / (double) max));
        g.fill(x, y, x + barWidth, y + 7, 0xDC050912);
        g.fill(x + 1, y + 1, x + 1 + fill, y + 6, 0xEF5E8EEB);
        String label = ArcaneClientState.integer("circle", 1) + "C  " + mana + "/" + max;
        g.centeredText(font, Component.literal(label), width / 2, y - 10, 0xFFE7DDF7);
    }

    private static void drawSlot(GuiGraphicsExtractor g, Font font, int x, int y, int size, int slot) {
        String spellId = ArcaneClientState.slot(slot);
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        int color = spell == null ? 0xFF606475 : ArcaneRenderUtil.schoolColor(spell.school());
        int dark = spell == null ? 0xFF171A22 : ArcaneRenderUtil.schoolDark(spell.school());
        double cooldown = ArcaneClientState.cooldownFraction(slot);
        int remaining = ArcaneClientState.cooldownRemainingTicks(slot);

        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xE0040610);
        g.fill(x, y, x + size, y + size, 0xEC0A0E1A);
        g.fill(x + 2, y + 2, x + size - 2, y + size - 2, remaining > 0 ? dark : 0xE312192A);
        if (remaining > 0) g.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0x66101018);

        ArcaneRenderUtil.cooldownArc(g, x, y, size - 1, cooldown,
                remaining > 0 ? 0xFFF17777 : color, 0xFF34394A);
        g.text(font, Component.literal(Integer.toString(slot + 1)), x + 3, y + 2, 0xFFF8F2FF);

        if (spell != null) {
            ArcaneRenderUtil.spellRune(g, x + size / 2, y + size / 2 - 2, spell,
                    Math.max(5, size / 5), remaining > 0 ? 0xFF827B89 : 0xFFF8F2FF);
            if (size >= 30) {
                String name = compactName(spell.name(), size < 35 ? 3 : 5);
                g.centeredText(font, Component.literal(name), x + size / 2, y + size - 10,
                        remaining > 0 ? 0xFF8B8492 : 0xFFDCD4E9);
            }
        }
        if (remaining > 0) {
            String seconds = remaining >= 200 ? Integer.toString((int) Math.ceil(remaining / 20.0))
                    : String.format("%.1f", remaining / 20.0);
            g.centeredText(font, Component.literal(seconds), x + size / 2, y + size / 2 - 5, 0xFFFFFFFF);
        }
    }

    private static void drawFusionQueue(GuiGraphicsExtractor g, Font font, int width, int y) {
        List<String> queue = ArcaneClientState.queue();
        if (queue.isEmpty()) return;
        String result = ArcaneClientState.queueResult();
        List<String> candidates = ArcaneClientState.queueCandidates();
        int boxWidth = Math.min(width - 12, width < 400 ? 240 : 310);
        int x = (width - boxWidth) / 2;
        g.fill(x, y, x + boxWidth, y + 20, 0xED080B16);
        g.fill(x, y, x + boxWidth, y + 2, result.isBlank() ? 0xFF7E67AD : 0xFFFFC861);
        String chain = queue.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .reduce((a, b) -> a + " + " + b).orElse("");
        String suffix;
        int color;
        if (!result.isBlank()) {
            suffix = " → " + SpellCatalog.spell(result).map(SpellDefinition::name).orElse(result)
                    + (ArcaneClientState.queueCanExtend() ? " (+1 가능)" : "");
            color = 0xFFFFD889;
        } else if (!candidates.isEmpty()) {
            suffix = " · 후보 " + candidates.size() + "개";
            color = 0xFFD4B8F1;
        } else {
            suffix = " · 조합 불가";
            color = 0xFFE1828D;
        }
        g.centeredText(font, Component.literal(compactName("X  " + chain + suffix, Math.max(24, boxWidth / 5))),
                width / 2, y + 6, color);
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) || !ArcaneClientState.ready()) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = minecraft.font;

        int inventoryLeft = width / 2 - 88;
        int inventoryRight = width / 2 + 88;
        int sideSpace = Math.max(inventoryLeft, width - inventoryRight);
        if (sideSpace >= 142) {
            int panelW = Math.min(164, sideSpace - 12);
            int x = inventoryRight + 7;
            if (x + panelW > width - 5) x = inventoryLeft - panelW - 7;
            int y = Math.max(5, (height - 128) / 2);
            drawInventorySide(g, font, x, y, panelW, 128);
        } else {
            int panelW = Math.min(width - 10, 232);
            int x = (width - panelW) / 2;
            int inventoryTop = (height - 166) / 2;
            int y = Math.max(4, inventoryTop - 57);
            drawInventoryCompact(g, font, x, y, panelW, 50);
        }
    }

    private static void drawInventorySide(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h) {
        panel(g, x, y, w, h, "마력핵 상태");
        int mana = ArcaneClientState.integer("mana", 0);
        int max = ArcaneClientState.integer("max", 100);
        int lineY = y + 29;
        g.text(font, Component.literal("써클  " + ArcaneClientState.integer("circle", 1) + "C"), x + 8, lineY, 0xFFDCC7F5);
        g.text(font, Component.literal("마력  " + mana + "/" + max), x + 8, lineY + 13, 0xFF8DB6F1);
        g.text(font, Component.literal("회복  " + String.format("%.1f", ArcaneClientState.regenPerSecond()) + "/초"),
                x + 8, lineY + 26, 0xFF8ED6C0);
        g.text(font, Component.literal("통찰  " + ArcaneClientState.integer("insight", 0)), x + 8, lineY + 39, 0xFFBBA6D5);
        g.text(font, Component.literal("지팡이  " + compactName(ArcaneClientState.text("staff", "맨손"), 12)),
                x + 8, lineY + 55, 0xFFFFD58D);
        g.text(font, Component.literal(compactName(modifierSummary(), 23)), x + 8, lineY + 68, 0xFF9EA9C1);
        g.text(font, Component.literal("C 마도서 · 1~5 주문 · X 융합"), x + 8, y + h - 14, 0xFF81778F);
    }

    private static void drawInventoryCompact(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h) {
        panel(g, x, y, w, h, "마력핵");
        String first = ArcaneClientState.integer("circle", 1) + "C  ·  MP "
                + ArcaneClientState.integer("mana", 0) + "/" + ArcaneClientState.integer("max", 100)
                + "  ·  회복 " + String.format("%.1f", ArcaneClientState.regenPerSecond()) + "/초";
        String second = compactName(ArcaneClientState.text("staff", "맨손") + "  ·  " + modifierSummary(), Math.max(30, w / 5));
        g.centeredText(font, Component.literal(first), x + w / 2, y + 25, 0xFFC9D8F2);
        g.centeredText(font, Component.literal(second), x + w / 2, y + 37, 0xFFFFD58D);
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF604779);
        g.fill(x, y, x + w, y + h, 0xF20A0F1D);
        g.fill(x + 3, y + 3, x + w - 3, y + 21, 0xD1241A38);
        Minecraft minecraft = Minecraft.getInstance();
        g.centeredText(minecraft.font, Component.literal(title), x + w / 2, y + 8, 0xFFEAD9FF);
    }

    private static String modifierSummary() {
        int mana = ArcaneClientState.integer("staff_mana", 0);
        int cost = (int) Math.round((ArcaneClientState.staffMultiplier("staff_cost") - 1.0) * 100.0);
        int power = (int) Math.round((ArcaneClientState.staffMultiplier("staff_power") - 1.0) * 100.0);
        int range = (int) Math.round((ArcaneClientState.staffMultiplier("staff_range") - 1.0) * 100.0);
        int cooldown = (int) Math.round((ArcaneClientState.staffMultiplier("staff_cooldown") - 1.0) * 100.0);
        int regen = (int) Math.round((ArcaneClientState.staffMultiplier("staff_regen") - 1.0) * 100.0);
        return "MP" + signed(mana) + " 소모" + signed(cost) + "% 위력" + signed(power)
                + "% 범위" + signed(range) + "% 쿨" + signed(cooldown) + "% 회복" + signed(regen) + "%";
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static String compactName(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }
}

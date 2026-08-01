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
        if (minecraft.player == null || minecraft.gui.screen() != null) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        int slotSize = width < 430 ? 30 : 38;
        int gap = width < 430 ? 2 : 4;
        int total = slotSize * 5 + gap * 4;
        int startX = (width - total) / 2;
        int y = Math.max(8, height - slotSize - 29);

        drawMana(g, font, width, startX, y, slotSize);
        for (int slot = 0; slot < 5; slot++) drawSlot(g, font, startX + slot * (slotSize + gap), y, slotSize, slot);
        drawFusionQueue(g, font, width, y - 24);
    }

    private static void drawMana(GuiGraphicsExtractor g, Font font, int width, int startX, int y, int slotSize) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int barWidth = Math.min(104, Math.max(58, startX - 12));
        int x = Math.max(6, startX - barWidth - 8);
        int fill = (int) Math.round((barWidth - 2) * Math.min(1.0, mana / (double) max));
        g.fill(x, y + slotSize - 9, x + barWidth, y + slotSize - 2, 0xD9060A14);
        g.fill(x + 1, y + slotSize - 8, x + 1 + fill, y + slotSize - 3, 0xE85783E6);
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C  " + mana + "/" + max),
                x, y + slotSize - 21, 0xFFE0D7F4);
        if (width >= 520) {
            g.text(font, Component.literal(ArcaneClientState.text("staff", "맨손")), x, y + 1, 0xFFB59AD2);
        }
    }

    private static void drawSlot(GuiGraphicsExtractor g, Font font, int x, int y, int size, int slot) {
        String spellId = ArcaneClientState.slot(slot);
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        int color = spell == null ? 0xFF606475 : ArcaneRenderUtil.schoolColor(spell.school());
        double cooldown = ArcaneClientState.cooldownFraction(slot);
        g.fill(x, y, x + size, y + size, 0xDC080B16);
        g.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xE3111828);
        ArcaneRenderUtil.cooldownArc(g, x, y, size - 1, cooldown,
                cooldown > 0.0 ? 0xFFE86D6D : color, 0xFF34394A);
        g.text(font, Component.literal(Integer.toString(slot + 1)), x + 3, y + 2, 0xFFF5ECFF);
        if (spell != null) {
            ArcaneRenderUtil.spellRune(g, x + size / 2, y + size / 2 - 2, spell,
                    Math.max(6, size / 5), cooldown > 0.0 ? 0xFF706B78 : 0xFFF8F2FF);
            String name = compactName(spell.name(), size < 34 ? 3 : 5);
            g.centeredText(font, Component.literal(name), x + size / 2, y + size - 10,
                    cooldown > 0.0 ? 0xFF777481 : 0xFFD9D2E7);
        }
        int remaining = ArcaneClientState.cooldownRemainingTicks(slot);
        if (remaining > 0) {
            g.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0x55101018);
            String seconds = String.format("%.1f", remaining / 20.0);
            g.centeredText(font, Component.literal(seconds), x + size / 2, y + size / 2 - 5, 0xFFFFFFFF);
        }
    }

    private static void drawFusionQueue(GuiGraphicsExtractor g, Font font, int width, int y) {
        List<String> queue = ArcaneClientState.queue();
        if (queue.isEmpty()) return;
        String result = ArcaneClientState.queueResult();
        int boxWidth = Math.min(width - 16, 220);
        int x = (width - boxWidth) / 2;
        g.fill(x, y, x + boxWidth, y + 20, 0xE20A0C18);
        g.fill(x, y, x + boxWidth, y + 1, 0xFFB878ED);
        String chain = queue.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .reduce((a, b) -> a + " + " + b).orElse("");
        String suffix = result.isBlank() ? "  → 불안정" : "  → " + SpellCatalog.spell(result).map(SpellDefinition::name).orElse(result);
        g.centeredText(font, Component.literal("X 융합 " + chain + suffix), width / 2, y + 6,
                result.isBlank() ? 0xFFCB7C8A : 0xFFFFD889);
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = minecraft.font;
        int panelW = Math.min(154, Math.max(118, width / 3));
        int panelH = 122;
        int inventoryRight = width / 2 + 88;
        int x = inventoryRight + 8;
        if (x + panelW > width - 6) x = Math.max(6, width / 2 - 88 - panelW - 8);
        int y = Math.max(6, (height - panelH) / 2);
        drawInventoryStatus(g, font, x, y, panelW, panelH);
    }

    private static void drawInventoryStatus(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF685184);
        g.fill(x, y, x + w, y + h, 0xEE0B1120);
        g.fill(x + 4, y + 4, x + w - 4, y + 22, 0xCC241A38);
        g.centeredText(font, Component.literal("마력핵 상태"), x + w / 2, y + 9, 0xFFEAD9FF);
        int mana = ArcaneClientState.integer("mana", 0);
        int max = ArcaneClientState.integer("max", 100);
        int lineY = y + 29;
        g.text(font, Component.literal("써클  " + ArcaneClientState.integer("circle", 1) + "C"), x + 8, lineY, 0xFFDCC7F5);
        g.text(font, Component.literal("마력  " + mana + "/" + max), x + 8, lineY + 13, 0xFF8DB6F1);
        g.text(font, Component.literal("회복  " + String.format("%.1f", ArcaneClientState.regenPerSecond()) + "/초"),
                x + 8, lineY + 26, 0xFF8ED6C0);
        g.text(font, Component.literal("통찰  " + ArcaneClientState.integer("insight", 0)), x + 8, lineY + 39, 0xFFBBA6D5);
        g.text(font, Component.literal("지팡이  " + compactName(ArcaneClientState.text("staff", "맨손"), 10)),
                x + 8, lineY + 55, 0xFFFFD58D);
        g.text(font, Component.literal(modifierSummary()), x + 8, lineY + 68, 0xFF9EA9C1);
        g.text(font, Component.literal("C: 마도서 · 1~5: 주문 · X: 융합"), x + 8, y + h - 14, 0xFF81778F);
    }

    private static String modifierSummary() {
        int mana = ArcaneClientState.integer("staff_mana", 0);
        int cost = (int) Math.round((ArcaneClientState.staffMultiplier("staff_cost") - 1.0) * 100.0);
        int power = (int) Math.round((ArcaneClientState.staffMultiplier("staff_power") - 1.0) * 100.0);
        int range = (int) Math.round((ArcaneClientState.staffMultiplier("staff_range") - 1.0) * 100.0);
        int cooldown = (int) Math.round((ArcaneClientState.staffMultiplier("staff_cooldown") - 1.0) * 100.0);
        return "MP" + signed(mana) + " 소모" + signed(cost) + "% 위력" + signed(power)
                + "% 범위" + signed(range) + "% 쿨" + signed(cooldown) + "%";
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static String compactName(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }
}

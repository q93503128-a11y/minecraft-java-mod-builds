package kr.moonseungjun.villageguardians;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Mobile-defense inspired combat ribbon: wave pressure first, economy and progression second,
 * with the weakest defense and four live approach lanes visible without opening a menu.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageMainHudOverlay {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            VillageGuardians.MOD_ID, "village_main_hud");
    private static VillageDefenseHudFrame frame = VillageDefenseHudFrame.empty();
    private static long lastUpdate;

    private VillageMainHudOverlay() {}

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.OVERLAY_MESSAGE, LAYER_ID, VillageMainHudOverlay::render);
    }

    public static void accept(VillageNetwork.MainHudPayload payload) {
        frame = VillageDefenseHudFrame.decode(payload == null ? "" : payload.text());
        lastUpdate = System.currentTimeMillis();
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || !frame.valid()) return;
        if (System.currentTimeMillis() - lastUpdate > 3_000L) return;

        Font font = minecraft.font;
        int screenWidth = graphics.guiWidth();
        boolean compact = screenWidth < 480;
        int outer = 8;
        int totalWidth = Math.min(compact ? screenWidth - 16 : 600, Math.max(260, screenWidth - 16));
        int left = Math.max(outer, (screenWidth - totalWidth) / 2);
        int right = left + totalWidth;
        int top = 8;
        int mainHeight = compact ? 44 : 38;

        VillageDefenseUiTheme.panel(graphics, left, top, right, top + mainHeight);
        if (compact) renderCompactTop(graphics, font, left, right, top);
        else renderWideTop(graphics, font, left, right, top);

        int lowerTop = top + mainHeight + 4;
        int defenseWidth = compact ? Math.min(176, totalWidth * 46 / 100) : Math.min(210, totalWidth * 34 / 100);
        renderDefenseCard(graphics, font, left, lowerTop, defenseWidth);
        renderFrontPressure(graphics, font, left + defenseWidth + 5, right, lowerTop);

        if (!frame.alert().isBlank()) {
            int alertWidth = Math.min(totalWidth, font.width(frame.alert()) + 24);
            int alertLeft = (screenWidth - alertWidth) / 2;
            int y = lowerTop + 30;
            int accent = frame.alert().contains("위험") ? VillageDefenseUiTheme.RED : VillageDefenseUiTheme.AMBER;
            graphics.fill(alertLeft, y, alertLeft + alertWidth, y + 15, 0xD8141112);
            graphics.fill(alertLeft, y, alertLeft + 3, y + 15, accent);
            graphics.centeredText(font, fit(font, frame.alert(), alertWidth - 12),
                    alertLeft + alertWidth / 2, y + 4, accent);
        }
    }

    private static void renderWideTop(
            GuiGraphicsExtractor graphics, Font font, int left, int right, int top) {
        int width = right - left;
        int leftWidth = Math.max(128, width * 27 / 100);
        int rightWidth = Math.max(116, width * 23 / 100);
        int centerLeft = left + leftWidth;
        int centerRight = right - rightWidth;

        graphics.fill(centerLeft, top + 1, centerRight, top + 37, VillageDefenseUiTheme.PANEL_ACTIVE);
        graphics.fill(centerLeft, top + 1, centerLeft + 2, top + 37, VillageDefenseUiTheme.GOLD);
        graphics.fill(centerRight - 2, top + 1, centerRight, top + 37, VillageDefenseUiTheme.GOLD);

        graphics.text(font, frame.day() + "일 · " + frame.phase(), left + 10, top + 7,
                VillageDefenseUiTheme.GOLD, true);
        graphics.text(font, fit(font, "Lv." + frame.level() + "  " + frame.role(), leftWidth - 20),
                left + 10, top + 22, VillageDefenseUiTheme.MUTED, false);

        String raid = raidTitle();
        graphics.centeredText(font, fit(font, raid, centerRight - centerLeft - 16),
                (centerLeft + centerRight) / 2, top + 6, raidColor());
        String sub = raidSubline();
        graphics.centeredText(font, fit(font, sub, centerRight - centerLeft - 18),
                (centerLeft + centerRight) / 2, top + 22, VillageDefenseUiTheme.MUTED);

        String coins = frame.coins() + " 주화";
        String supplies = frame.supplies() + " 보급";
        graphics.text(font, fit(font, coins, rightWidth - 20), centerRight + 10, top + 7,
                VillageDefenseUiTheme.GOLD, true);
        graphics.text(font, fit(font, supplies, rightWidth - 20), centerRight + 10, top + 22,
                VillageDefenseUiTheme.CYAN, false);
    }

    private static void renderCompactTop(
            GuiGraphicsExtractor graphics, Font font, int left, int right, int top) {
        int center = (left + right) / 2;
        graphics.fill(center - 1, top + 4, center + 1, top + 40, VillageDefenseUiTheme.EDGE_SOFT);
        graphics.text(font, frame.day() + "일 · " + frame.phase(), left + 9, top + 5,
                VillageDefenseUiTheme.GOLD, true);
        graphics.text(font, fit(font, "Lv." + frame.level() + " " + frame.role(), center - left - 18),
                left + 9, top + 19, VillageDefenseUiTheme.MUTED, false);
        graphics.text(font, fit(font, frame.coins() + "주화 · " + frame.supplies() + "보급",
                        center - left - 18), left + 9, top + 32, VillageDefenseUiTheme.CYAN, false);

        graphics.centeredText(font, fit(font, raidTitle(), right - center - 18),
                center + (right - center) / 2, top + 9, raidColor());
        graphics.centeredText(font, fit(font, raidSubline(), right - center - 18),
                center + (right - center) / 2, top + 27, VillageDefenseUiTheme.MUTED);
    }

    private static void renderDefenseCard(
            GuiGraphicsExtractor graphics, Font font, int left, int top, int width) {
        int current = frame.defenseCurrent();
        int maximum = Math.max(1, frame.defenseMaximum());
        int color = VillageDefenseUiTheme.integrityColor(current, maximum);
        int height = 26;
        VillageDefenseUiTheme.card(graphics, left, top, left + width, top + height, color, current * 2 < maximum);
        graphics.text(font, fit(font, frame.defenseName(), width - 58), left + 9, top + 5,
                VillageDefenseUiTheme.TEXT, false);
        String percent = Math.round(current * 100.0f / maximum) + "%";
        graphics.text(font, percent, left + width - font.width(percent) - 8, top + 5, color, true);
        VillageDefenseUiTheme.progressBar(graphics, left + 9, top + 18, left + width - 8, top + 22,
                current, maximum, color);
    }

    private static void renderFrontPressure(
            GuiGraphicsExtractor graphics, Font font, int left, int right, int top) {
        int width = Math.max(72, right - left);
        VillageDefenseUiTheme.card(graphics, left, top, right, top + 26,
                VillageDefenseUiTheme.CYAN, frame.enemyCount() > 0);
        int gap = 3;
        int cell = Math.max(28, (width - 14 - gap * 3) / 4);
        int x = left + 7;
        x = front(graphics, font, x, top, cell, "북문", frame.northPressure(), gap);
        x = front(graphics, font, x, top, cell, "서측", frame.westPressure(), gap);
        x = front(graphics, font, x, top, cell, "동측", frame.eastPressure(), gap);
        front(graphics, font, x, top, cell, "후방", frame.rearPressure(), gap);
    }

    private static int front(
            GuiGraphicsExtractor graphics, Font font, int left, int top, int width,
            String label, int count, int gap) {
        int color = VillageDefenseUiTheme.pressureColor(count);
        String text = count <= 0 ? label : label + " " + count;
        graphics.centeredText(font, fit(font, text, width - 2), left + width / 2, top + 5, color);
        VillageDefenseUiTheme.pip(graphics, left + 2, top + 18, Math.max(8, width - 4), count);
        return left + width + gap;
    }

    private static String raidTitle() {
        return switch (frame.raidMode()) {
            case "ACTIVE" -> "WAVE " + frame.wave() + "/" + Math.max(1, frame.maxWaves())
                    + " · 적 " + frame.enemyCount();
            case "COUNTDOWN" -> "습격 접근 · " + Math.max(0, frame.nextSeconds()) + "초";
            case "GAME_OVER" -> "방어선 붕괴";
            default -> "정비 단계";
        };
    }

    private static String raidSubline() {
        if ("ACTIVE".equals(frame.raidMode())) {
            String trait = frame.trait().isBlank() ? "전투 진행 중" : frame.trait();
            return frame.nextSeconds() > 0 ? trait + " · 다음 " + frame.nextSeconds() + "초" : trait;
        }
        if ("COUNTDOWN".equals(frame.raidMode())) return "포탑·용병·성벽 최종 점검";
        if ("GAME_OVER".equals(frame.raidMode())) return "마을 회관 복구 필요";
        return "시설 수리 · 성장 · 배치 준비";
    }

    private static int raidColor() {
        return switch (frame.raidMode()) {
            case "ACTIVE" -> frame.enemyCount() >= 16 ? VillageDefenseUiTheme.RED : VillageDefenseUiTheme.GOLD;
            case "COUNTDOWN" -> VillageDefenseUiTheme.AMBER;
            case "GAME_OVER" -> VillageDefenseUiTheme.RED;
            default -> VillageDefenseUiTheme.GREEN;
        };
    }

    private static String fit(Font font, String value, int maximumWidth) {
        if (maximumWidth <= 0 || value == null) return "";
        if (font.width(value) <= maximumWidth) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > maximumWidth) end--;
        return value.substring(0, end) + suffix;
    }
}

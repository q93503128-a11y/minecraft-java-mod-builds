package kr.moonseungjun.arcanecircle.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Arcane Circle's procedural codex skin. Inspired by the spatial discipline of physical-book UIs,
 * but built only from Arcane Circle primitives and colors: no external textures, models or code.
 */
final class CodexVisualLanguage {
    private static final int SHADOW = 0xE905070B;
    private static final int LEATHER = 0xFF17101D;
    private static final int LEATHER_EDGE = 0xFF382440;
    private static final int PAGE = 0xFF171A25;
    private static final int PAGE_DEEP = 0xFF11141D;
    private static final int INK_EDGE = 0xFF393343;
    private static final int GOLD = 0xFFB99454;
    private static final int GOLD_HI = 0xFFE0C47A;
    private static final int PLUM = 0xFF392A49;
    private static final int PLUM_HI = 0xFF604574;

    private CodexVisualLanguage() {}

    static void bookFrame(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        g.fill(left - 5, top + 4, right + 6, bottom + 7, SHADOW);
        g.fill(left - 3, top - 3, right + 3, bottom + 3, LEATHER_EDGE);
        g.fill(left, top, right, bottom, LEATHER);

        int mid = (left + right) / 2;
        page(g, left + 7, top + 8, mid - 3, bottom - 7, false);
        page(g, mid + 3, top + 8, right - 7, bottom - 7, true);

        g.fill(mid - 3, top + 8, mid + 3, bottom - 7, 0xFF0B0A10);
        g.fill(mid - 2, top + 11, mid - 1, bottom - 10, 0xFF72556E);
        g.fill(mid + 1, top + 11, mid + 2, bottom - 10, 0xFF2A2030);

        ornament(g, left + 12, top + 13, 1);
        ornament(g, right - 12, top + 13, -1);
        ornament(g, left + 12, bottom - 13, -1);
        ornament(g, right - 12, bottom - 13, 1);

        ArcaneRenderUtil.ring(g, left + 23, top + 22, 8, 0x667C5D91);
        ArcaneRenderUtil.diamond(g, left + 23, top + 22, 4, 0x99C9A760);
        ArcaneRenderUtil.ring(g, right - 23, bottom - 22, 8, 0x667C5D91);
        ArcaneRenderUtil.diamond(g, right - 23, bottom - 22, 4, 0x99C9A760);
    }

    private static void page(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, boolean right) {
        g.fill(x0, y0, x1, y1, PAGE_DEEP);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, PAGE);
        int inner = right ? x0 + 2 : x1 - 3;
        g.fill(inner, y0 + 7, inner + 1, y1 - 7, 0xFF292231);
        g.fill(x0 + 7, y0 + 7, x1 - 7, y0 + 8, INK_EDGE);
        g.fill(x0 + 7, y1 - 8, x1 - 7, y1 - 7, 0xFF282431);
    }

    static void bookmark(GuiGraphicsExtractor g, int x, int y, int w, int h,
                         boolean active, boolean hover, int accent) {
        int body = active ? PLUM_HI : hover ? PLUM : 0xFF201927;
        g.fill(x, y, x + w, y + h - 3, body);
        g.fill(x + 2, y + h - 3, x + w / 2, y + h, body);
        g.fill(x + w / 2, y + h - 3, x + w - 2, y + h, body);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, active ? GOLD_HI : 0xFF574761);
        if (active) g.fill(x + 3, y, x + w - 3, y + 1, accent);
    }

    static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent,
                     boolean hover, boolean selected, boolean enabled) {
        int edge = enabled ? accent : 0xFF4B4851;
        int body = selected ? 0xFF342943 : hover ? 0xFF292536 : 0xE9191B27;
        g.fill(x, y, x + w, y + h, 0xFF0C0D13);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, body);
        g.fill(x + 1, y + 1, x + 3, y + h - 1, edge);
        g.fill(x + 5, y + 3, x + w - 5, y + 4, selected ? GOLD_HI : 0xFF3C3445);
        g.fill(x + w - 5, y + h - 5, x + w - 2, y + h - 2, enabled ? edge : 0xFF4A4750);
    }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent) {
        g.fill(x, y, x + w, y + h, 0xFF0C0D13);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xE9181A25);
        g.fill(x + 5, y + 3, x + w - 5, y + 4, 0xFF3C3445);
        g.fill(x + 2, y + 2, x + 3, y + h - 2, accent);
        ArcaneRenderUtil.diamond(g, x + w - 10, y + 10, 4, 0xAA8F6D9B);
    }

    static void action(GuiGraphicsExtractor g, int x, int y, int w, int h,
                       boolean hover, boolean emphasized) {
        int accent = emphasized ? GOLD : 0xFF765989;
        g.fill(x, y, x + w, y + h, 0xFF0C0D13);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xFF332942 : 0xFF211B2B);
        g.fill(x + 2, y + 2, x + 3, y + h - 2, accent);
        if (emphasized) g.fill(x + 5, y + h - 3, x + w - 5, y + h - 2, GOLD_HI);
    }

    static void seal(GuiGraphicsExtractor g, int cx, int cy, int radius, int accent, int seed) {
        ArcaneRenderUtil.ring(g, cx, cy, radius, 0xCC000000 | (accent & 0x00FFFFFF));
        ArcaneRenderUtil.polygon(g, cx, cy, Math.max(3, radius - 3), 4 + Math.floorMod(seed, 4),
                0xBB000000 | (accent & 0x00FFFFFF));
        ArcaneRenderUtil.diamond(g, cx, cy, Math.max(2, radius / 3), GOLD_HI);
    }

    private static void ornament(GuiGraphicsExtractor g, int x, int y, int direction) {
        ArcaneRenderUtil.line(g, x, y, x + 12 * direction, y, GOLD);
        ArcaneRenderUtil.line(g, x, y, x, y + 8 * direction, GOLD);
        ArcaneRenderUtil.diamond(g, x, y, 3, GOLD_HI);
    }
}

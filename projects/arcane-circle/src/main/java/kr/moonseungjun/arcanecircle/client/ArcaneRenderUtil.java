package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ArcaneRenderUtil {
    private ArcaneRenderUtil() {}

    public static int schoolColor(SpellDefinition.School school) {
        return switch (school) {
            case FIRE -> 0xFFE36C45;
            case FROST -> 0xFF63C8E8;
            case WIND -> 0xFF72D4B0;
            case WARD -> 0xFFB38BE8;
            case LIFE -> 0xFF71D487;
            case SPACE -> 0xFF8669E5;
            default -> 0xFF6F91E7;
        };
    }

    public static void spellRune(GuiGraphicsExtractor g, int x, int y, SpellDefinition spell, int size, int color) {
        int seed = spell.id().hashCode() & 0x7fffffff;
        int sides = 3 + seed % 5;
        polygon(g, x, y, size, sides, color);
        int inner = Math.max(2, size / 2);
        if ((seed & 1) == 0) diamond(g, x, y, inner, color);
        else ring(g, x, y, inner, color);
        int spokes = 2 + seed % 4;
        for (int i = 0; i < spokes; i++) {
            double angle = Math.PI * 2.0 * i / spokes + (seed % 17) * 0.03;
            int dx = (int) Math.round(Math.cos(angle) * size);
            int dy = (int) Math.round(Math.sin(angle) * size);
            line(g, x, y, x + dx, y + dy, color);
        }
    }

    public static void cooldownArc(GuiGraphicsExtractor g, int x, int y, int size, double remainingFraction,
                                   int activeColor, int emptyColor) {
        int segments = 32;
        int active = (int) Math.ceil(Math.max(0.0, Math.min(1.0, remainingFraction)) * segments);
        for (int i = 0; i < segments; i++) {
            double progress = i / (double) segments;
            double angle = -Math.PI / 2.0 + progress * Math.PI * 2.0;
            double scale = 1.0 / Math.max(Math.abs(Math.cos(angle)), Math.abs(Math.sin(angle)));
            int px = x + size / 2 + (int) Math.round(Math.cos(angle) * (size / 2.0) * scale);
            int py = y + size / 2 + (int) Math.round(Math.sin(angle) * (size / 2.0) * scale);
            int color = i < active ? activeColor : emptyColor;
            g.fill(px - 1, py - 1, px + 2, py + 2, color);
        }
    }

    public static void fillCircle(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    public static void ring(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        int points = Math.max(24, radius * 5);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    public static void diamond(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        line(g, cx, cy - radius, cx + radius, cy, color);
        line(g, cx + radius, cy, cx, cy + radius, color);
        line(g, cx, cy + radius, cx - radius, cy, color);
        line(g, cx - radius, cy, cx, cy - radius, color);
    }

    public static void polygon(GuiGraphicsExtractor g, int x, int y, int radius, int sides, int color) {
        Point first = null;
        Point previous = null;
        for (int i = 0; i < sides; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides;
            Point p = new Point(x + (int) Math.round(Math.cos(angle) * radius),
                    y + (int) Math.round(Math.sin(angle) * radius));
            if (first == null) first = p;
            if (previous != null) line(g, previous.x(), previous.y(), p.x(), p.y(), color);
            previous = p;
        }
        if (first != null && previous != null) line(g, previous.x(), previous.y(), first.x(), first.y(), color);
    }

    public static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            double p = i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * p);
            int y = (int) Math.round(y1 + (y2 - y1) * p);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private record Point(int x, int y) {}
}

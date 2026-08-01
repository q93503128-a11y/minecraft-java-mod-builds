package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ArcaneRenderUtil {
    private ArcaneRenderUtil() {}

    public static int schoolColor(SpellDefinition.School school) {
        return switch (school) {
            case FIRE -> 0xFFE9784F;
            case FROST -> 0xFF67D2F0;
            case WIND -> 0xFF72D9B6;
            case WARD -> 0xFFB990EE;
            case LIFE -> 0xFF79D98D;
            case SPACE -> 0xFF9175EC;
            default -> 0xFF7599F2;
        };
    }

    public static int schoolDark(SpellDefinition.School school) {
        return switch (school) {
            case FIRE -> 0xFF4A221D;
            case FROST -> 0xFF183A4A;
            case WIND -> 0xFF183D35;
            case WARD -> 0xFF38264B;
            case LIFE -> 0xFF203E29;
            case SPACE -> 0xFF2B244F;
            default -> 0xFF202D54;
        };
    }

    public static void spellRune(GuiGraphicsExtractor g, int x, int y, SpellDefinition spell, int size, int color) {
        int seed = spell.id().hashCode() & 0x7fffffff;
        int outer = Math.max(4, size);
        polygon(g, x, y, outer, 5 + seed % 3, color);
        int inner = Math.max(3, outer * 2 / 3);
        switch (spell.school()) {
            case ARCANE -> {
                diamond(g, x, y, inner, color);
                line(g, x - inner, y, x + inner, y, color);
                line(g, x, y - inner, x, y + inner, color);
            }
            case FIRE -> {
                triangle(g, x, y, inner, color);
                line(g, x, y - inner, x, y + inner, color);
                diamond(g, x, y + inner / 3, Math.max(2, inner / 3), color);
            }
            case FROST -> {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.PI * i / 3.0;
                    int dx = (int) Math.round(Math.cos(angle) * inner);
                    int dy = (int) Math.round(Math.sin(angle) * inner);
                    line(g, x, y, x + dx, y + dy, color);
                }
                ring(g, x, y, Math.max(2, inner / 3), color);
            }
            case WIND -> {
                ring(g, x - inner / 3, y, Math.max(2, inner / 2), color);
                ring(g, x + inner / 3, y - inner / 4, Math.max(2, inner / 2), color);
                line(g, x - inner, y + inner / 2, x + inner, y + inner / 2, color);
            }
            case WARD -> {
                diamond(g, x, y, inner, color);
                diamond(g, x, y, Math.max(2, inner / 2), color);
                line(g, x - inner, y, x + inner, y, color);
            }
            case LIFE -> {
                line(g, x - inner, y, x + inner, y, color);
                line(g, x, y - inner, x, y + inner, color);
                diamond(g, x, y, Math.max(2, inner / 2), color);
            }
            case SPACE -> {
                ring(g, x - inner / 3, y, Math.max(2, inner * 2 / 3), color);
                ring(g, x + inner / 3, y, Math.max(2, inner * 2 / 3), color);
                diamond(g, x, y, Math.max(2, inner / 3), color);
            }
        }
    }

    public static void cooldownArc(GuiGraphicsExtractor g, int x, int y, int size, double remainingFraction,
                                   int activeColor, int emptyColor) {
        int segments = Math.max(40, size * 2);
        int active = (int) Math.ceil(Math.max(0.0, Math.min(1.0, remainingFraction)) * segments);
        for (int i = 0; i < segments; i++) {
            double p = i / (double) segments;
            Point point = squarePerimeterPoint(x, y, size, p);
            int color = i < active ? activeColor : emptyColor;
            g.fill(point.x(), point.y(), point.x() + 2, point.y() + 2, color);
        }
    }

    private static Point squarePerimeterPoint(int x, int y, int size, double progress) {
        double edge = (progress * 4.0 + 0.5) % 4.0;
        if (edge < 1.0) return new Point(x + size, y + (int) Math.round(edge * size));
        if (edge < 2.0) return new Point(x + size - (int) Math.round((edge - 1.0) * size), y + size);
        if (edge < 3.0) return new Point(x, y + size - (int) Math.round((edge - 2.0) * size));
        return new Point(x + (int) Math.round((edge - 3.0) * size), y);
    }

    public static void fillCircle(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    public static void ring(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        int points = Math.max(24, radius * 6);
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

    public static void triangle(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        int topY = cy - radius;
        int bottomY = cy + radius;
        line(g, cx, topY, cx + radius, bottomY, color);
        line(g, cx + radius, bottomY, cx - radius, bottomY, color);
        line(g, cx - radius, bottomY, cx, topY, color);
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

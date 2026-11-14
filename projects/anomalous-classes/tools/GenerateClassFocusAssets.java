import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Builds tier-shaped 16px focus icons from the project's existing class glyphs. */
public final class GenerateClassFocusAssets {
    private static final String[] CLASSES = {
            "mental_dominator", "puppeteer", "parasite_morpher",
            "barrier_caster", "pact_caster", "swarm_caster"
    };
    private static final String[] TIERS = {
            "standard", "altered", "anomalous", "eldritch", "incomprehensible"
    };
    private static final String[] TIER_LABELS = {
            "STANDARD / REGULATED", "ALTERED / BRACED", "ANOMALOUS / FACETED",
            "ELDRITCH / SEALED", "INCOMPREHENSIBLE / FRACTURED"
    };
    private static final String[] CLASS_LABELS = {
            "Mental Dominator", "Puppeteer", "Parasite Morpher",
            "Barrier Caster", "Pact Caster", "Swarm Caster"
    };
    // shadow, body, highlight. The class glyph remains the dominant identity color.
    private static final int[][] MATERIALS = {
            {0xFF302638, 0xFF786649, 0xFFD8BD78}, // worn bronze socket
            {0xFF293039, 0xFF657B78, 0xFFB6D1B4}, // braced alloy
            {0xFF302340, 0xFF7951A2, 0xFFE1B8FF}, // faceted anomaly crystal
            {0xFF201B32, 0xFF69478D, 0xFFD8B9F4}, // sealed rune-metal
            {0xFF19182E, 0xFF9384B1, 0xFFFFD98A}  // fractured pale-gold void matter
    };

    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/anomalousclasses");
    private static final Path REVIEW_ROOT = Path.of("ui/reviews/2026-11-14");

    private GenerateClassFocusAssets() {}

    public static void main(String[] args) throws IOException {
        Path iconRoot = ASSET_ROOT.resolve("textures/gui/class_icons_v2");
        Path textureRoot = ASSET_ROOT.resolve("textures/item");
        Files.createDirectories(textureRoot);
        int[][] hashes = new int[CLASSES.length][TIERS.length];
        int generated = 0;
        for (int classIndex = 0; classIndex < CLASSES.length; classIndex++) {
            Path classIcon = iconRoot.resolve(CLASSES[classIndex] + ".png");
            BufferedImage source = ImageIO.read(classIcon.toFile());
            if (source == null) throw new IOException("Could not read class icon for " + CLASSES[classIndex]);
            for (int tier = 0; tier < TIERS.length; tier++) {
                String itemId = "focus_" + CLASSES[classIndex] + "_" + TIERS[tier];
                BufferedImage icon = icon(source, tier);
                verifyIcon(icon, itemId);
                hashes[classIndex][tier] = Arrays.hashCode(icon.getRGB(0, 0, 16, 16, null, 0, 16));
                ImageIO.write(icon, "png", textureRoot.resolve(itemId + ".png").toFile());
                generated++;
            }
        }
        verifyTierVariation(hashes);
        writeContactSheet(textureRoot);
        System.out.println("Generated " + generated + " 16x16 focus textures; item models and IDs were not changed.");
    }

    private static BufferedImage icon(BufferedImage source, int tier) {
        BufferedImage target = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int shadow = MATERIALS[tier][0];
        int body = MATERIALS[tier][1];
        int highlight = MATERIALS[tier][2];
        drawSilhouette(target, tier, shadow, body, highlight);

        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        // Keep the original job glyph and its colors; the grade changes the physical casing around it.
        graphics.drawImage(source, 2, 2, 14, 14, 0, 0, source.getWidth(), source.getHeight(), null);
        graphics.dispose();
        drawTopDetails(target, tier, shadow, body, highlight);
        return target;
    }

    private static void drawSilhouette(BufferedImage image, int tier, int shadow, int body, int light) {
        switch (tier) {
            case 0 -> {
                line(image, 4, 14, 11, 14, shadow);
                line(image, 5, 15, 10, 15, body);
                pixel(image, 7, 14, light);
                pixel(image, 8, 14, light);
            }
            case 1 -> {
                line(image, 3, 3, 5, 1, shadow);
                line(image, 5, 1, 7, 1, light);
                line(image, 10, 1, 12, 3, shadow);
                line(image, 10, 1, 11, 1, light);
                line(image, 1, 5, 1, 10, shadow);
                line(image, 2, 5, 2, 10, body);
                line(image, 13, 5, 13, 10, shadow);
                line(image, 14, 5, 14, 10, body);
                pixel(image, 1, 4, light);
                pixel(image, 14, 4, light);
                line(image, 4, 14, 11, 14, shadow);
                line(image, 5, 15, 10, 15, body);
            }
            case 2 -> {
                line(image, 7, 0, 8, 0, light);
                line(image, 5, 2, 6, 1, body);
                line(image, 9, 1, 10, 2, body);
                line(image, 0, 7, 2, 5, shadow);
                line(image, 0, 8, 2, 10, light);
                line(image, 13, 5, 15, 7, shadow);
                line(image, 13, 10, 15, 8, light);
                line(image, 5, 13, 6, 14, body);
                line(image, 9, 14, 10, 13, body);
                line(image, 7, 15, 8, 15, light);
            }
            case 3 -> {
                line(image, 5, 0, 6, 0, shadow);
                line(image, 9, 0, 10, 0, shadow);
                line(image, 4, 1, 4, 3, body);
                line(image, 11, 1, 11, 3, body);
                line(image, 2, 4, 3, 4, light);
                line(image, 12, 4, 13, 4, light);
                line(image, 1, 6, 1, 9, shadow);
                line(image, 14, 6, 14, 9, shadow);
                line(image, 3, 11, 4, 12, body);
                line(image, 11, 12, 12, 11, body);
                line(image, 5, 14, 6, 15, light);
                line(image, 9, 15, 10, 14, light);
                pixel(image, 7, 0, light);
                pixel(image, 8, 0, light);
                pixel(image, 0, 7, body);
                pixel(image, 15, 8, body);
            }
            case 4 -> {
                // A broken orbit and four unequal shards give this grade an unmistakably larger silhouette.
                line(image, 2, 3, 4, 1, shadow);
                pixel(image, 2, 2, light);
                line(image, 5, 0, 6, 0, body);
                line(image, 10, 0, 11, 1, light);
                line(image, 12, 2, 13, 4, shadow);
                line(image, 13, 5, 13, 6, body);
                line(image, 15, 8, 14, 10, light);
                line(image, 13, 12, 11, 13, shadow);
                line(image, 9, 15, 7, 14, body);
                line(image, 4, 14, 2, 12, light);
                line(image, 1, 10, 1, 8, shadow);
                line(image, 0, 6, 1, 4, body);
                pixel(image, 0, 2, light);
                line(image, 14, 14, 15, 13, body);
            }
            default -> throw new IllegalArgumentException("Unknown focus tier " + tier);
        }
    }

    private static void drawTopDetails(BufferedImage image, int tier, int shadow, int body, int light) {
        switch (tier) {
            case 0 -> {
                pixel(image, 5, 13, shadow);
                pixel(image, 10, 13, shadow);
                line(image, 6, 15, 9, 15, light);
            }
            case 1 -> {
                pixel(image, 3, 4, body);
                pixel(image, 12, 4, body);
                pixel(image, 3, 11, light);
                pixel(image, 12, 11, light);
            }
            case 2 -> {
                pixel(image, 7, 1, light);
                pixel(image, 8, 1, light);
                pixel(image, 1, 7, light);
                pixel(image, 14, 8, light);
                pixel(image, 7, 14, shadow);
                pixel(image, 8, 14, shadow);
            }
            case 3 -> {
                pixel(image, 3, 3, shadow);
                pixel(image, 12, 3, shadow);
                pixel(image, 2, 5, body);
                pixel(image, 13, 10, body);
                pixel(image, 4, 12, light);
                pixel(image, 11, 12, light);
                pixel(image, 7, 1, light);
                pixel(image, 8, 1, light);
            }
            case 4 -> {
                pixel(image, 3, 4, light);
                pixel(image, 12, 5, body);
                pixel(image, 2, 11, shadow);
                pixel(image, 10, 14, light);
                pixel(image, 7, 1, light);
                pixel(image, 15, 11, shadow);
            }
            default -> throw new IllegalArgumentException("Unknown focus tier " + tier);
        }
    }

    private static void writeContactSheet(Path textureRoot) throws IOException {
        int left = 178, top = 58, cellWidth = 166, cellHeight = 82;
        BufferedImage sheet = new BufferedImage(left + TIERS.length * cellWidth + 18,
                top + CLASSES.length * cellHeight + 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(24, 18, 32));
        graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        graphics.setColor(new Color(238, 215, 173));
        for (int tier = 0; tier < TIERS.length; tier++) {
            graphics.drawString(TIER_LABELS[tier], left + tier * cellWidth + 5, 34);
        }
        for (int classIndex = 0; classIndex < CLASSES.length; classIndex++) {
            int y = top + classIndex * cellHeight;
            graphics.setColor(new Color(48, 35, 57));
            graphics.fillRoundRect(8, y + 3, sheet.getWidth() - 16, cellHeight - 5, 10, 10);
            graphics.setColor(new Color(238, 215, 173));
            graphics.drawString(CLASS_LABELS[classIndex], 18, y + 39);
            for (int tier = 0; tier < TIERS.length; tier++) {
                Path iconPath = textureRoot.resolve("focus_" + CLASSES[classIndex] + "_" + TIERS[tier] + ".png");
                BufferedImage icon = ImageIO.read(iconPath.toFile());
                if (icon == null) throw new IOException("Could not read focus texture " + iconPath);
                graphics.drawImage(icon, left + tier * cellWidth + 47, y + 8, 64, 64, null);
            }
        }
        graphics.dispose();
        Files.createDirectories(REVIEW_ROOT);
        ImageIO.write(sheet, "png", REVIEW_ROOT.resolve("class_focus_tiers_comparison.png").toFile());
    }

    private static void verifyIcon(BufferedImage icon, String itemId) throws IOException {
        if (icon.getWidth() != 16 || icon.getHeight() != 16) throw new IOException("Unexpected dimensions for " + itemId);
        boolean visible = false;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            if ((icon.getRGB(x, y) >>> 24) != 0) visible = true;
        }
        if (!visible) throw new IOException("Empty focus texture " + itemId);
    }

    private static void verifyTierVariation(int[][] hashes) throws IOException {
        for (int classIndex = 0; classIndex < CLASSES.length; classIndex++) {
            for (int first = 0; first < TIERS.length; first++) {
                for (int second = first + 1; second < TIERS.length; second++) {
                    if (hashes[classIndex][first] == hashes[classIndex][second]) {
                        throw new IOException("Tier artwork is identical for " + CLASSES[classIndex]
                                + ": " + TIERS[first] + " and " + TIERS[second]);
                    }
                }
            }
        }
    }

    private static void line(BufferedImage image, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
        int dy = -Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            pixel(image, x1, y1, color);
            if (x1 == x2 && y1 == y2) break;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x1 += sx; }
            if (twice <= dx) { error += dx; y1 += sy; }
        }
    }

    private static void pixel(BufferedImage image, int x, int y, int color) {
        if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) image.setRGB(x, y, color);
    }
}

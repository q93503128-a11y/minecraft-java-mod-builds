import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Builds 16px focus item icons from the existing class glyphs, without adding external artwork. */
public final class GenerateClassFocusAssets {
    private static final String[] CLASSES = {
            "mental_dominator", "puppeteer", "parasite_morpher",
            "barrier_caster", "pact_caster", "swarm_caster"
    };
    private static final String[] TIERS = {
            "standard", "altered", "anomalous", "eldritch", "incomprehensible"
    };
    private static final int[] TIER_COLORS = {
            0xFFB2A78E, 0xFF71C990, 0xFF73A9F5, 0xFFC57AF4, 0xFFFFCE67
    };
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/anomalousclasses");
    private static final Path REVIEW_ROOT = Path.of("ui/reviews/2026-11-12");

    private GenerateClassFocusAssets() {}

    public static void main(String[] args) throws IOException {
        Path iconRoot = ASSET_ROOT.resolve("textures/gui/class_icons_v2");
        Path textureRoot = ASSET_ROOT.resolve("textures/item");
        Path modelRoot = ASSET_ROOT.resolve("models/item");
        Files.createDirectories(textureRoot);
        Files.createDirectories(modelRoot);

        int generated = 0;
        for (String classId : CLASSES) {
            BufferedImage source = ImageIO.read(iconRoot.resolve(classId + ".png").toFile());
            if (source == null) throw new IOException("Could not read class icon for " + classId);
            for (int tier = 0; tier < TIERS.length; tier++) {
                String itemId = "focus_" + classId + "_" + TIERS[tier];
                BufferedImage icon = icon(source, TIER_COLORS[tier], tier + 1);
                ImageIO.write(icon, "png", textureRoot.resolve(itemId + ".png").toFile());
                String model = "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n"
                        + "    \"layer0\": \"anomalousclasses:item/" + itemId + "\"\n  }\n}\n";
                Files.writeString(modelRoot.resolve(itemId + ".json"), model);
                generated++;
            }
        }
        writeContactSheet(textureRoot);
        System.out.println("Generated " + generated + " class-focus textures and item models.");
    }

    private static void writeContactSheet(Path textureRoot) throws IOException {
        int left = 160;
        int top = 54;
        int cellWidth = 146;
        int cellHeight = 76;
        BufferedImage sheet = new BufferedImage(left + TIERS.length * cellWidth + 18,
                top + CLASSES.length * cellHeight + 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(27, 20, 35));
        graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        graphics.setColor(new Color(238, 215, 173));
        for (int tier = 0; tier < TIERS.length; tier++) {
            graphics.drawString(TIERS[tier], left + tier * cellWidth + 8, 30);
        }
        for (int classIndex = 0; classIndex < CLASSES.length; classIndex++) {
            int y = top + classIndex * cellHeight;
            graphics.setColor(new Color(54, 39, 63));
            graphics.fillRoundRect(8, y + 3, sheet.getWidth() - 16, cellHeight - 5, 10, 10);
            graphics.setColor(new Color(238, 215, 173));
            graphics.drawString(CLASSES[classIndex], 18, y + 35);
            for (int tier = 0; tier < TIERS.length; tier++) {
                Path iconPath = textureRoot.resolve("focus_" + CLASSES[classIndex] + "_" + TIERS[tier] + ".png");
                BufferedImage icon = ImageIO.read(iconPath.toFile());
                if (icon == null) throw new IOException("Could not read generated icon " + iconPath);
                int x = left + tier * cellWidth + 34;
                graphics.drawImage(icon, x, y + 5, 64, 64, null);
            }
        }
        graphics.dispose();
        Files.createDirectories(REVIEW_ROOT);
        ImageIO.write(sheet, "png", REVIEW_ROOT.resolve("class_focus_item_icons_contact_sheet.png").toFile());
    }

    private static BufferedImage icon(BufferedImage source, int accent, int pips) {
        BufferedImage target = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 14; y++) {
            for (int x = 0; x < 14; x++) {
                int sx = Math.min(source.getWidth() - 1, (x * source.getWidth()) / 14);
                int sy = Math.min(source.getHeight() - 1, (y * source.getHeight()) / 14);
                target.setRGB(x + 1, y, source.getRGB(sx, sy));
            }
        }

        // Five small grade pips share the visual language of the existing class glyphs.
        int start = 8 - (pips * 2 - 1) / 2;
        for (int index = 0; index < pips; index++) {
            int x = start + index * 2;
            target.setRGB(x, 14, 0xFF261B2C);
            target.setRGB(x, 15, accent);
        }
        return target;
    }
}

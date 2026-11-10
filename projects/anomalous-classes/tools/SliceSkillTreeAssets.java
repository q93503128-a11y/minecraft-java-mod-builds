import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Crops the generated transparent atlases into runtime sprites; no artwork is painted by this utility. */
public final class SliceSkillTreeAssets {
    private SliceSkillTreeAssets() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException("Usage: SliceSkillTreeAssets <atlas> <output-dir> <columns> <rows> <name...>");
        }
        BufferedImage atlas = ImageIO.read(new File(args[0]));
        File output = new File(args[1]);
        int columns = Integer.parseInt(args[2]);
        int rows = Integer.parseInt(args[3]);
        if (atlas == null || !output.isDirectory() && !output.mkdirs()) throw new IllegalArgumentException("Invalid atlas or output directory");
        int count = Math.min(args.length - 4, columns * rows);
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            int left = column * atlas.getWidth() / columns;
            int right = (column + 1) * atlas.getWidth() / columns;
            int top = row * atlas.getHeight() / rows;
            int bottom = (row + 1) * atlas.getHeight() / rows;
            BufferedImage cell = atlas.getSubimage(left, top, right - left, bottom - top);
            BufferedImage sprite = normalize(cell);
            String name = args[index + 4].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
            if (!ImageIO.write(sprite, "png", new File(output, name + ".png"))) {
                throw new IllegalStateException("PNG writer unavailable");
            }
        }
    }

    private static BufferedImage normalize(BufferedImage cell) {
        int minX = cell.getWidth(), minY = cell.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < cell.getHeight(); y++) {
            for (int x = 0; x < cell.getWidth(); x++) {
                if ((cell.getRGB(x, y) >>> 24) > 8) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
        }
        BufferedImage result = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        if (maxX < minX || maxY < minY) return result;
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int side = Math.max(width, height);
        BufferedImage square = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D squareGraphics = square.createGraphics();
        squareGraphics.setComposite(AlphaComposite.Src);
        squareGraphics.drawImage(cell, (side - width) / 2, (side - height) / 2,
                (side - width) / 2 + width, (side - height) / 2 + height,
                minX, minY, maxX + 1, maxY + 1, null);
        squareGraphics.dispose();
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.drawImage(square, 12, 12, 244, 244, null);
        graphics.dispose();
        return result;
    }
}

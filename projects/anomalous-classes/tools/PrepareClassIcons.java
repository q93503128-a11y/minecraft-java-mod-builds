import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayDeque;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Normalizes transparent generated sprite art to one shared 96x96 icon canvas. */
public final class PrepareClassIcons {
    private static final int CANVAS_SIZE = 96;
    private static final int ICON_SIZE = 80;

    private PrepareClassIcons() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length % 2 != 0) {
            throw new IllegalArgumentException("Pass source and destination paths in pairs");
        }
        for (int index = 0; index < args.length; index += 2) {
            File sourceFile = new File(args[index]);
            File targetFile = new File(args[index + 1]);
            BufferedImage original = ImageIO.read(sourceFile);
            if (original == null) throw new IllegalArgumentException("Unsupported image: " + sourceFile);
            CleanedImage cleaned = removeEdgeCheckerboard(original);
            BufferedImage source = cleaned.image();
            long removedBackgroundPixels = cleaned.removedPixels();

            int minX = source.getWidth(), minY = source.getHeight(), maxX = -1, maxY = -1;
            long transparentPixels = 0;
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    if ((source.getRGB(x, y) >>> 24) < 16) {
                        transparentPixels++;
                    } else {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < minX || maxY < minY) throw new IllegalArgumentException("Icon has no visible pixels: " + sourceFile);

            int cropWidth = maxX - minX + 1;
            int cropHeight = maxY - minY + 1;
            double scale = Math.min(ICON_SIZE / (double) cropWidth, ICON_SIZE / (double) cropHeight);
            int width = Math.max(1, (int) Math.round(cropWidth * scale));
            int height = Math.max(1, (int) Math.round(cropHeight * scale));
            int x = (CANVAS_SIZE - width) / 2;
            int y = (CANVAS_SIZE - height) / 2;
            BufferedImage output = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, x, y, x + width, y + height,
                    minX, minY, maxX + 1, maxY + 1, null);
            graphics.dispose();

            File parent = targetFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IllegalStateException("Cannot create directory: " + parent);
            }
            if (!ImageIO.write(output, "png", targetFile)) throw new IllegalStateException("PNG writer unavailable");
            System.out.printf("%s -> %s (%dx%d, transparent=%d, checker removed=%d)%n",
                    sourceFile, targetFile, CANVAS_SIZE, CANVAS_SIZE, transparentPixels, removedBackgroundPixels);
        }
    }

    private record CleanedImage(BufferedImage image, long removedPixels) {}

    private static CleanedImage removeEdgeCheckerboard(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] visited = new boolean[width * height];
        boolean[] background = new boolean[width * height];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            enqueueBackground(image, x, 0, visited, queue);
            enqueueBackground(image, x, height - 1, visited, queue);
        }
        for (int y = 1; y < height - 1; y++) {
            enqueueBackground(image, 0, y, visited, queue);
            enqueueBackground(image, width - 1, y, visited, queue);
        }
        long removed = 0;
        while (!queue.isEmpty()) {
            int pixel = queue.removeFirst();
            int x = pixel % width;
            int y = pixel / width;
            background[pixel] = true;
            removed++;
            if (x > 0) enqueueBackground(image, x - 1, y, visited, queue);
            if (x + 1 < width) enqueueBackground(image, x + 1, y, visited, queue);
            if (y > 0) enqueueBackground(image, x, y - 1, visited, queue);
            if (y + 1 < height) enqueueBackground(image, x, y + 1, visited, queue);
        }
        BufferedImage cleaned = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = y * width + x;
                int argb = image.getRGB(x, y);
                cleaned.setRGB(x, y, background[pixel] ? 0x00000000 : argb);
            }
        }
        return new CleanedImage(cleaned, removed);
    }

    private static void enqueueBackground(BufferedImage image, int x, int y, boolean[] visited, ArrayDeque<Integer> queue) {
        int width = image.getWidth();
        int index = y * width + x;
        if (visited[index]) return;
        visited[index] = true;
        int argb = image.getRGB(x, y);
        int alpha = argb >>> 24;
        int red = argb >>> 16 & 0xFF;
        int green = argb >>> 8 & 0xFF;
        int blue = argb & 0xFF;
        int spread = Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
        boolean transparent = alpha < 16;
        boolean checker = alpha > 0 && spread <= 18 && red >= 200 && green >= 200 && blue >= 200;
        if (transparent || checker) queue.addLast(index);
    }
}

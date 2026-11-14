import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Re-extracts frame sprites from the original menu_v1 atlas without alpha compositing. */
public final class extract_menu_frame_parts {
    private record Part(String name, int x, int y, int width, int height) {}

    private static final Part[] PARTS = {
            new Part("corner_tl", 32, 32, 160, 160),
            new Part("corner_tr", 576, 32, 160, 160),
            new Part("corner_bl", 32, 576, 160, 160),
            new Part("corner_br", 576, 576, 160, 160),
            new Part("rail_top_left", 318, 64, 24, 64),
            new Part("rail_top_right", 426, 64, 24, 64),
            new Part("rail_bottom_left", 319, 640, 24, 64),
            new Part("rail_bottom_right", 426, 640, 24, 64),
            new Part("rail_side_left", 64, 434, 64, 24),
            new Part("rail_side_right", 640, 428, 64, 24),
            new Part("jewel_side_left", 84, 344, 24, 64),
            new Part("jewel_side_right", 660, 344, 24, 64),
            new Part("jewel_top", 368, 48, 32, 88),
            new Part("jewel_bottom", 368, 628, 32, 88)
    };

    private extract_menu_frame_parts() {}

    public static void main(String[] args) throws IOException {
        Path project = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        Path atlasPath = project.resolve("src/main/resources/assets/anomalousclasses/textures/gui/menu_v1/frame_atlas.png");
        Path output = project.resolve("src/main/resources/assets/anomalousclasses/textures/gui/menu_v1/frame_parts");
        BufferedImage atlas = ImageIO.read(atlasPath.toFile());
        if (atlas == null || atlas.getWidth() != 768 || atlas.getHeight() != 768) {
            throw new IOException("Expected the original 768x768 menu_v1/frame_atlas.png: " + atlasPath);
        }
        Files.createDirectories(output);
        for (Part part : PARTS) {
            BufferedImage image = new BufferedImage(part.width(), part.height(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < part.height(); y++) {
                for (int x = 0; x < part.width(); x++) {
                    image.setRGB(x, y, atlas.getRGB(part.x() + x, part.y() + y));
                }
            }
            if (part.name().startsWith("corner_")) {
                cleanCornerAlphaNoise(image);
            }
            Path file = output.resolve(part.name() + ".png");
            if (!ImageIO.write(image, "png", file.toFile())) throw new IOException("PNG writer unavailable");
            image.flush();
            System.out.printf("%s <- (%d,%d %dx%d)%n", file.getFileName(), part.x(), part.y(), part.width(), part.height());
        }
        atlas.flush();
    }

    /**
     * The source atlas contains low-alpha speckle outside each corner ornament. Keep its authored edge
     * antialiasing and shadow close to the visible art, but clear detached alpha noise and hidden RGB.
     */
    private static void cleanCornerAlphaNoise(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] visibleEdge = new boolean[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                visibleEdge[y * width + x] = (image.getRGB(x, y) >>> 24) >= 33;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int alpha = pixel >>> 24;
                if (alpha == 0) {
                    image.setRGB(x, y, 0);
                } else if (alpha <= 32 && !hasNearbyVisibleEdge(visibleEdge, width, height, x, y)) {
                    image.setRGB(x, y, 0);
                }
            }
        }
    }

    private static boolean hasNearbyVisibleEdge(boolean[] visibleEdge, int width, int height, int x, int y) {
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                if (dx * dx + dy * dy > 9) continue;
                int neighborX = x + dx;
                int neighborY = y + dy;
                if (neighborX >= 0 && neighborX < width && neighborY >= 0 && neighborY < height
                        && visibleEdge[neighborY * width + neighborX]) return true;
            }
        }
        return false;
    }
}

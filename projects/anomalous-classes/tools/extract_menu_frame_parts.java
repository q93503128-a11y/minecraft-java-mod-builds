import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Re-extracts verified, isolated frame sprites from the original menu_v1 atlas. */
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
            new Part("jewel_side_left", 72, 344, 48, 64),
            new Part("jewel_side_right", 648, 344, 48, 64),
            new Part("jewel_top", 352, 48, 64, 88),
            new Part("jewel_bottom", 352, 628, 64, 88)
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
            var graphics = image.createGraphics();
            graphics.drawImage(atlas,
                    0, 0, part.width(), part.height(),
                    part.x(), part.y(), part.x() + part.width(), part.y() + part.height(), null);
            graphics.dispose();
            Path file = output.resolve(part.name() + ".png");
            if (!ImageIO.write(image, "png", file.toFile())) throw new IOException("PNG writer unavailable");
            image.flush();
            System.out.printf("%s <- (%d,%d %dx%d)%n", file.getFileName(), part.x(), part.y(), part.width(), part.height());
        }
        atlas.flush();
    }
}

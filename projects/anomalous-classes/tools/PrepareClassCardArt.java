import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Crops generated 16:9 card scenes to the in-game texture dimensions. */
public final class PrepareClassCardArt {
    private static final int TARGET_WIDTH = 512;
    private static final int TARGET_HEIGHT = 288;

    private PrepareClassCardArt() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length % 2 != 0) {
            throw new IllegalArgumentException("Pass source and destination paths in pairs");
        }
        for (int index = 0; index < args.length; index += 2) {
            File sourceFile = new File(args[index]);
            File targetFile = new File(args[index + 1]);
            BufferedImage source = ImageIO.read(sourceFile);
            if (source == null) throw new IllegalArgumentException("Unsupported image: " + sourceFile);

            double sourceRatio = source.getWidth() / (double) source.getHeight();
            double targetRatio = TARGET_WIDTH / (double) TARGET_HEIGHT;
            int cropWidth = source.getWidth();
            int cropHeight = source.getHeight();
            if (sourceRatio > targetRatio) {
                cropWidth = (int) Math.round(source.getHeight() * targetRatio);
            } else if (sourceRatio < targetRatio) {
                cropHeight = (int) Math.round(source.getWidth() / targetRatio);
            }
            int cropX = (source.getWidth() - cropWidth) / 2;
            int cropY = (source.getHeight() - cropHeight) / 2;
            BufferedImage output = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, TARGET_WIDTH, TARGET_HEIGHT,
                    cropX, cropY, cropX + cropWidth, cropY + cropHeight, null);
            graphics.dispose();
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IllegalStateException("Cannot create directory: " + parent);
            }
            if (!ImageIO.write(output, "png", targetFile)) throw new IllegalStateException("PNG writer unavailable");
            System.out.printf("%s -> %s (%dx%d)%n", sourceFile, targetFile, TARGET_WIDTH, TARGET_HEIGHT);
        }
    }
}

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

/** Creates the original, text-free pixel illustrations and emblems shipped with the mod. */
public final class GenerateClassCardArt {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 144;
    private static final String[] IDS = {
            "mental_dominator", "puppeteer", "parasite_morpher", "barrier_caster", "pact_caster", "swarm_caster"
    };
    private static final int[] ACCENTS = {0xFFB880F5, 0xFFFFB56A, 0xFF9FC47D, 0xFF75C7E8, 0xFFE4C47B, 0xFFB9CB70};

    private GenerateClassCardArt() {}

    public static void main(String[] args) throws IOException {
        File assetRoot = args.length > 0 ? new File(args[0]) : new File("src/main/resources/assets/anomalousclasses");
        File cardDirectory = new File(assetRoot, "textures/gui/class_cards");
        File iconDirectory = new File(assetRoot, "textures/gui/class_icons");
        if (!cardDirectory.mkdirs() && !cardDirectory.isDirectory()) throw new IOException("Cannot create " + cardDirectory);
        if (!iconDirectory.mkdirs() && !iconDirectory.isDirectory()) throw new IOException("Cannot create " + iconDirectory);

        for (int index = 0; index < IDS.length; index++) {
            BufferedImage art = createCard(index);
            BufferedImage scaled = new BufferedImage(WIDTH * 2, HEIGHT * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(art, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
            g.dispose();
            ImageIO.write(scaled, "png", new File(cardDirectory, IDS[index] + ".png"));
            ImageIO.write(createIcon(index), "png", new File(iconDirectory, IDS[index] + ".png"));
        }
        System.out.println("Generated six original class card illustrations and six emblems at " + assetRoot.getAbsolutePath());
    }

    private static BufferedImage createCard(int index) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int[][] sky = {
                {0xFF201934, 0xFF45315B, 0xFF785998}, {0xFF30251F, 0xFF644932, 0xFF9A7950},
                {0xFF152B29, 0xFF33544A, 0xFF71917A}, {0xFF172B38, 0xFF2D5367, 0xFF6CA4B9},
                {0xFF29231E, 0xFF5B4D36, 0xFFB38C4A}, {0xFF1C2C1C, 0xFF3A5935, 0xFF789250}
        };
        drawSky(g, sky[index][0], sky[index][1], sky[index][2]);
        drawArchitecture(g, index);
        drawFloor(g, index);
        switch (index) {
            case 0 -> drawMentalScene(g);
            case 1 -> drawPuppetScene(g);
            case 2 -> drawParasiteScene(g);
            case 3 -> drawBarrierScene(g);
            case 4 -> drawPactScene(g);
            case 5 -> drawSwarmScene(g);
            default -> throw new IllegalArgumentException("Unknown class art index " + index);
        }
        drawForeground(g, index);
        drawVignette(g, index);
        g.dispose();
        return image;
    }

    private static void drawSky(Graphics2D g, int top, int middle, int bottom) {
        for (int y = 0; y < 118; y += 4) {
            float t = y / 118.0f;
            int upper = mix(top, middle, Math.min(1.0f, t * 1.8f));
            int lower = mix(middle, bottom, Math.max(0.0f, (t - 0.45f) * 1.8f));
            int color = t < 0.45f ? upper : lower;
            fill(g, 0, y, WIDTH, 4, color);
        }
        Random random = new Random(490 + top);
        for (int i = 0; i < 80; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(92);
            int size = random.nextBoolean() ? 2 : 3;
            int alpha = 22 + random.nextInt(28);
            fill(g, x, y, size, size, (alpha << 24) | 0x00D9D6D2);
        }
    }

    private static void drawArchitecture(Graphics2D g, int theme) {
        int stone = switch (theme) {
            case 1 -> 0xFF4B3D34;
            case 2, 5 -> 0xFF263B35;
            case 3 -> 0xFF2F4A55;
            case 4 -> 0xFF514738;
            default -> 0xFF352D40;
        };
        int stoneLight = lighter(stone, 18);
        int stoneDark = darker(stone, 24);
        drawColumn(g, 8, 22, 27, 94, stoneLight, stoneDark, theme);
        drawColumn(g, 219, 12, 28, 101, stoneLight, stoneDark, theme);
        drawArch(g, 38, 14, 180, 70, stone, stoneLight, theme);
        if (theme == 1 || theme == 4) {
            drawBanner(g, 50, 25, theme == 1 ? 0xFF5A3557 : 0xFF5E4A2B, theme == 1 ? 0xFFB986E8 : 0xFFE4C47B);
        }
        if (theme == 2 || theme == 5) {
            drawVine(g, 20, 35, theme == 2 ? 0xFF819D62 : 0xFF8CAA54);
            drawVine(g, 233, 36, theme == 2 ? 0xFF75955B : 0xFF9BAC51);
        }
        if (theme == 3) {
            for (int i = 0; i < 4; i++) {
                fill(g, 52 + i * 7, 48 - i * 5, 5, 4, 0x5548B8D6);
            }
        }
    }

    private static void drawColumn(Graphics2D g, int x, int y, int w, int h, int light, int dark, int theme) {
        fill(g, x + 3, y, w - 6, 8, lighter(light, 18));
        fill(g, x, y + 7, w, 7, light);
        fill(g, x + 4, y + 14, w - 8, h - 24, dark);
        fill(g, x + 7, y + 16, w - 14, h - 27, light);
        fill(g, x + 10, y + 18, w - 18, h - 30, stoneShade(light, theme));
        for (int tile = 25; tile < h - 8; tile += 18) {
            fill(g, x + 3, y + tile, w - 6, 2, lighter(light, 5));
            fill(g, x + 8 + (tile % 3), y + tile + 5, 3, 4, darker(light, 18));
        }
        fill(g, x - 2, y + h - 10, w + 4, 7, light);
        fill(g, x + 2, y + h - 3, w - 4, 3, darker(light, 12));
    }

    private static int stoneShade(int base, int theme) {
        return switch (theme) {
            case 0 -> mix(base, 0xFF463753, 0.55f);
            case 1 -> mix(base, 0xFF73502C, 0.50f);
            case 2, 5 -> mix(base, 0xFF435F3C, 0.48f);
            case 3 -> mix(base, 0xFF3A6C78, 0.42f);
            default -> mix(base, 0xFF75603A, 0.46f);
        };
    }

    private static void drawArch(Graphics2D g, int x, int y, int w, int h, int stone, int light, int theme) {
        g.setColor(new Color(light, true));
        g.setStroke(new BasicStroke(9, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.draw(new Arc2D.Float(x, y, w, h * 2.1f, 0, 180, Arc2D.OPEN));
        g.setColor(new Color(darker(stone, 5), true));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.draw(new Arc2D.Float(x + 10, y + 5, w - 20, h * 1.7f, 0, 180, Arc2D.OPEN));
        for (int i = 0; i < 7; i++) {
            int bx = x + 22 + i * ((w - 44) / 6);
            fill(g, bx, y + 6 + Math.abs(3 - i) * 2, 7, 5, lighter(light, 10));
        }
        if (theme == 0 || theme == 4) {
            int accent = theme == 0 ? 0xFF8F5BBB : 0xFFB68B41;
            fill(g, WIDTH / 2 - 2, y + 2, 4, 13, accent);
        }
    }

    private static void drawBanner(Graphics2D g, int x, int y, int cloth, int trim) {
        fill(g, x, y, 32, 5, trim);
        fill(g, x + 4, y + 5, 24, 38, cloth);
        fill(g, x + 6, y + 7, 20, 33, darker(cloth, 16));
        fill(g, x + 11, y + 12, 10, 3, trim);
        fill(g, x + 13, y + 15, 6, 12, trim);
        fill(g, x + 8, y + 40, 16, 4, cloth);
        fill(g, x + 4, y + 43, 7, 9, trim);
        fill(g, x + 21, y + 43, 7, 9, trim);
    }

    private static void drawVine(Graphics2D g, int x, int y, int green) {
        fill(g, x, y, 3, 24, green);
        fill(g, x - 4, y + 10, 7, 3, green);
        fill(g, x + 1, y + 24, 3, 18, darker(green, 12));
        fill(g, x - 6, y + 29, 8, 3, green);
        fill(g, x + 3, y + 42, 3, 16, green);
        fill(g, x + 4, y + 13, 4, 3, lighter(green, 12));
        fill(g, x - 8, y + 31, 4, 3, lighter(green, 10));
    }

    private static void drawFloor(Graphics2D g, int theme) {
        int base = switch (theme) {
            case 1 -> 0xFF594A3B;
            case 2, 5 -> 0xFF33453B;
            case 3 -> 0xFF38515A;
            case 4 -> 0xFF554B3A;
            default -> 0xFF3A3345;
        };
        fill(g, 0, 108, WIDTH, 36, darker(base, 21));
        for (int y = 110; y < HEIGHT; y += 8) {
            int offset = ((y / 8) % 2) * 8;
            for (int x = -16 + offset; x < WIDTH; x += 16) {
                int color = mix(base, 0xFFB9AA8B, ((x + y) % 4 == 0) ? 0.20f : 0.06f);
                fill(g, x + 1, y + 1, 14, 6, color);
                fill(g, x + 1, y + 1, 13, 1, lighter(color, 7));
                fill(g, x + 13, y + 2, 1, 4, darker(color, 13));
            }
        }
    }

    private static void drawMentalScene(Graphics2D g) {
        drawRing(g, 127, 55, 54, 38, 0xAA9C53D5, 2);
        drawRing(g, 127, 55, 39, 26, 0xAA72459D, 1);
        drawEye(g, 127, 54, 15, 0xFFCE8FFF);
        drawHumanoid(g, 124, 125, 0, 0xFF3A3047, 0xFF171823, 0xFFB880F5);
        drawZombie(g, 71, 124, 0xFF668266, 0xFF394C43, 0xFFAE7AE9);
        drawZombie(g, 183, 125, 0xFF5B725F, 0xFF313F3A, 0xFF9F6CE0);
        drawTendril(g, 100, 87, 72, 108, 0xFFCE80FF);
        drawTendril(g, 145, 83, 182, 104, 0xFFB96BE9);
        fill(g, 94, 79, 4, 4, 0xFFFFD4FF);
        fill(g, 159, 85, 4, 4, 0xFFFFD4FF);
        for (int i = 0; i < 8; i++) fill(g, 87 + i * 11, 104 - (i % 3) * 4, 2, 2, 0xFFDBA0FF);
    }

    private static void drawPuppetScene(Graphics2D g) {
        fill(g, 52, 21, 150, 4, 0xFF8A6641);
        fill(g, 64, 24, 4, 7, 0xFFD6A45F);
        fill(g, 187, 24, 4, 7, 0xFFD6A45F);
        drawHumanoid(g, 91, 125, 1, 0xFF815334, 0xFF382A26, 0xFFFFB56A);
        drawMarionette(g, 168, 123);
        drawThread(g, 66, 30, 168, 39, 0xFFD5B178);
        drawThread(g, 132, 25, 168, 40, 0xFFD5B178);
        drawThread(g, 190, 31, 168, 39, 0xFFD5B178);
        drawThread(g, 93, 85, 168, 58, 0xFFC89A59);
        fill(g, 67, 30, 3, 3, 0xFFFFD58A);
        fill(g, 130, 25, 3, 3, 0xFFFFD58A);
        fill(g, 188, 31, 3, 3, 0xFFFFD58A);
        fill(g, 76, 112, 8, 7, 0xFFB28253);
        fill(g, 199, 106, 9, 11, 0xFFB28253);
        fill(g, 202, 103, 4, 4, 0xFFFFCF7D);
    }

    private static void drawParasiteScene(Graphics2D g) {
        fill(g, 33, 91, 34, 13, 0xFF273D35);
        fill(g, 196, 79, 39, 16, 0xFF213A35);
        drawHumanoid(g, 128, 126, 2, 0xFF5C7056, 0xFF24312D, 0xFFB36E6C);
        drawZombie(g, 74, 125, 0xFF78845B, 0xFF354336, 0xFF83B77B);
        drawZombie(g, 182, 124, 0xFF647D5B, 0xFF293A32, 0xFF91BC72);
        drawOrganicGrowth(g, 126, 68, 0xFFB05E65, 0xFF70904D);
        drawTendril(g, 110, 93, 85, 74, 0xFF9C625F);
        drawTendril(g, 146, 94, 174, 70, 0xFF79924F);
        for (int i = 0; i < 11; i++) {
            int x = 44 + (i * 37) % 174;
            int y = 48 + (i * 17) % 48;
            fill(g, x, y, 3, 3, i % 2 == 0 ? 0xFF9ABA76 : 0xFFB67473);
        }
        fill(g, 119, 48, 17, 6, 0xFF8A5A54);
        fill(g, 123, 46, 9, 2, 0xFFD27C70);
    }

    private static void drawBarrierScene(Graphics2D g) {
        drawHumanoid(g, 127, 126, 3, 0xFF557786, 0xFF283440, 0xFF75C7E8);
        drawHexRing(g, 128, 82, 69, 0x7A85D8EC, 3);
        drawHexRing(g, 128, 82, 54, 0x6672BED9, 2);
        drawHexRing(g, 128, 82, 39, 0x5063AFC7, 1);
        for (int i = 0; i < 6; i++) {
            int x = 62 + i * 22;
            fill(g, x, 53 + (i % 2) * 14, 10, 2, 0xFFA6E9F7);
            fill(g, x + 4, 48 + (i % 2) * 14, 2, 12, 0xFF5CB4CD);
        }
        fill(g, 48, 112, 34, 5, 0xFF657A82);
        fill(g, 178, 111, 39, 6, 0xFF526B75);
        fill(g, 80, 102, 5, 4, 0xFFFFE7AB);
        fill(g, 174, 92, 4, 4, 0xFFFFE7AB);
    }

    private static void drawPactScene(Graphics2D g) {
        drawPortal(g, 177, 68, 0xFFE4C47B);
        drawHumanoid(g, 105, 125, 4, 0xFF6E5339, 0xFF2C2623, 0xFFE4C47B);
        drawSpirit(g, 180, 122, 0xFFD8C393, 0xFF605645, 0xFFFFDA88);
        drawRing(g, 105, 75, 44, 29, 0x88E3C37B, 2);
        drawContractSeal(g, 106, 79, 15, 0xFFFFD37C);
        fill(g, 61, 112, 38, 12, 0xFF574B3B);
        fill(g, 68, 108, 29, 6, 0xFF8A7147);
        fill(g, 73, 105, 20, 4, 0xFFE3C070);
        for (int i = 0; i < 7; i++) {
            int x = 144 + i * 7;
            fill(g, x, 48 + (i % 3) * 6, 3, 3, i % 2 == 0 ? 0xFFFFD98A : 0xFFD7B66D);
        }
        drawTendril(g, 129, 79, 165, 72, 0xFFE3C37B);
    }

    private static void drawSwarmScene(Graphics2D g) {
        fill(g, 33, 96, 24, 21, 0xFF365A31);
        fill(g, 31, 83, 28, 13, 0xFF466A3A);
        fill(g, 195, 90, 29, 25, 0xFF3A6034);
        fill(g, 190, 78, 35, 12, 0xFF567744);
        drawHumanoid(g, 126, 126, 5, 0xFF627343, 0xFF2A3327, 0xFFB9CB70);
        drawBee(g, 68, 58, 0xFFEDC85D);
        drawBee(g, 89, 44, 0xFFE8BF54);
        drawBee(g, 107, 73, 0xFFF2D167);
        drawBee(g, 157, 48, 0xFFE4BD4F);
        drawBee(g, 181, 69, 0xFFF0CB60);
        drawBee(g, 201, 51, 0xFFE3B84A);
        drawBee(g, 79, 95, 0xFFEBC55A);
        drawBee(g, 175, 99, 0xFFE6BD52);
        for (int i = 0; i < 15; i++) {
            int x = 61 + (i * 29) % 141;
            int y = 43 + (i * 19) % 62;
            fill(g, x, y, 2, 2, 0xAAE8E99D);
        }
        drawVine(g, 23, 40, 0xFF87A052);
        drawVine(g, 226, 38, 0xFF789248);
    }

    private static void drawHumanoid(Graphics2D g, int center, int baseY, int theme, int coat, int dark, int accent) {
        int skin = switch (theme) {
            case 1 -> 0xFFD7A879;
            case 2 -> 0xFF879377;
            case 3 -> 0xFFD4B08A;
            case 4 -> 0xFFC9AB86;
            case 5 -> 0xFFC8A875;
            default -> 0xFFD5B28D;
        };
        fill(g, center - 18, baseY - 58, 36, 47, 0x55302A2D);
        drawVoxelCube(g, center - 13, baseY - 91, 25, 27, 5, skin, lighter(skin, 14), darker(skin, 22));
        fill(g, center - 13, baseY - 92, 25, 6, dark);
        fill(g, center - 16, baseY - 87, 5, 19, dark);
        fill(g, center - 10, baseY - 75, 5, 3, theme == 0 ? accent : 0xFF3A2925);
        fill(g, center + 4, baseY - 75, 5, 3, theme == 0 ? accent : 0xFF3A2925);
        drawVoxelCube(g, center - 13, baseY - 61, 27, 37, 5, coat, lighter(coat, 16), darker(coat, 22));
        fill(g, center - 14, baseY - 57, 29, 8, accent);
        fill(g, center - 12, baseY - 46, 24, 13, dark);
        fill(g, center - 15, baseY - 28, 11, 28, dark);
        fill(g, center + 4, baseY - 28, 11, 28, darker(dark, 8));
        fill(g, center - 17, baseY - 4, 14, 5, 0xFF312820);
        fill(g, center + 3, baseY - 4, 15, 5, 0xFF2A241F);
        fill(g, center - 21, baseY - 59, 9, 31, coat);
        fill(g, center + 12, baseY - 57, 10, 25, coat);
        fill(g, center - 22, baseY - 29, 10, 7, skin);
        fill(g, center + 12, baseY - 34, 10, 7, skin);
        if (theme == 0) {
            fill(g, center - 14, baseY - 71, 29, 9, darker(dark, 3));
            fill(g, center - 4, baseY - 69, 9, 5, accent);
        } else if (theme == 2) {
            fill(g, center - 15, baseY - 67, 30, 5, 0xFF9D625D);
            fill(g, center - 1, baseY - 66, 3, 3, 0xFFFFD8B2);
        } else if (theme == 4) {
            fill(g, center - 8, baseY - 42, 5, 10, accent);
            fill(g, center + 3, baseY - 39, 4, 8, accent);
        }
    }

    private static void drawZombie(Graphics2D g, int center, int baseY, int skin, int clothes, int eye) {
        drawVoxelCube(g, center - 10, baseY - 47, 20, 20, 4, skin, lighter(skin, 12), darker(skin, 17));
        fill(g, center - 10, baseY - 48, 20, 5, darker(skin, 13));
        fill(g, center - 7, baseY - 38, 4, 3, eye);
        fill(g, center + 3, baseY - 38, 4, 3, eye);
        fill(g, center - 12, baseY - 26, 24, 24, clothes);
        fill(g, center - 14, baseY - 24, 6, 18, skin);
        fill(g, center + 8, baseY - 24, 6, 18, skin);
        fill(g, center - 10, baseY - 3, 8, 12, darker(clothes, 12));
        fill(g, center + 2, baseY - 3, 8, 12, darker(clothes, 8));
    }

    private static void drawMarionette(Graphics2D g, int center, int baseY) {
        drawVoxelCube(g, center - 10, baseY - 70, 20, 19, 4, 0xFFB8804E, 0xFFD29B60, 0xFF76543A);
        fill(g, center - 6, baseY - 63, 4, 3, 0xFF34231B);
        fill(g, center + 3, baseY - 63, 4, 3, 0xFF34231B);
        fill(g, center - 8, baseY - 51, 16, 23, 0xFF9C7047);
        fill(g, center - 6, baseY - 49, 12, 19, 0xFFBD8C56);
        fill(g, center - 19, baseY - 49, 11, 6, 0xFF80583B);
        fill(g, center + 8, baseY - 49, 11, 6, 0xFF80583B);
        fill(g, center - 21, baseY - 45, 6, 18, 0xFF9E7045);
        fill(g, center + 15, baseY - 45, 6, 18, 0xFF9E7045);
        fill(g, center - 7, baseY - 29, 6, 20, 0xFF8D643E);
        fill(g, center + 2, baseY - 29, 6, 20, 0xFF8D643E);
        fill(g, center - 9, baseY - 10, 10, 4, 0xFF62452F);
        fill(g, center + 1, baseY - 10, 10, 4, 0xFF62452F);
        fill(g, center - 22, baseY - 73, 44, 2, 0xFFD2A765);
        fill(g, center - 1, baseY - 86, 2, 14, 0xFFD9B273);
        fill(g, center - 20, baseY - 73, 2, 21, 0xFFD9B273);
        fill(g, center + 18, baseY - 73, 2, 21, 0xFFD9B273);
    }

    private static void drawOrganicGrowth(Graphics2D g, int center, int y, int red, int green) {
        fill(g, center - 12, y, 24, 7, darker(red, 16));
        fill(g, center - 8, y - 7, 16, 10, red);
        fill(g, center - 2, y - 12, 7, 11, lighter(red, 8));
        fill(g, center - 15, y + 4, 6, 19, green);
        fill(g, center + 10, y + 2, 6, 20, green);
        fill(g, center - 20, y + 16, 10, 4, lighter(green, 8));
        fill(g, center + 9, y + 15, 11, 4, lighter(green, 8));
        fill(g, center - 4, y + 2, 8, 3, 0xFFDAA47A);
    }

    private static void drawSpirit(Graphics2D g, int center, int baseY, int body, int dark, int accent) {
        fill(g, center - 14, baseY - 72, 28, 57, 0x5531271D);
        drawVoxelCube(g, center - 11, baseY - 75, 22, 22, 5, body, lighter(body, 16), darker(body, 18));
        fill(g, center - 7, baseY - 68, 5, 4, accent);
        fill(g, center + 2, baseY - 68, 5, 4, accent);
        fill(g, center - 15, baseY - 53, 30, 31, dark);
        fill(g, center - 10, baseY - 50, 20, 25, body);
        fill(g, center - 23, baseY - 47, 8, 22, dark);
        fill(g, center + 15, baseY - 47, 8, 22, dark);
        fill(g, center - 11, baseY - 20, 8, 20, dark);
        fill(g, center + 4, baseY - 20, 8, 20, dark);
        for (int i = 0; i < 4; i++) fill(g, center - 20 + i * 12, baseY - 77 - (i % 2) * 5, 3, 3, accent);
    }

    private static void drawBee(Graphics2D g, int x, int y, int yellow) {
        fill(g, x, y + 3, 11, 7, 0xFF211F19);
        fill(g, x + 2, y + 3, 7, 7, yellow);
        fill(g, x + 4, y + 3, 2, 7, 0xFF333026);
        fill(g, x + 8, y + 4, 3, 3, 0xFF1D1917);
        fill(g, x + 3, y, 4, 3, 0xA8DFF3E4);
        fill(g, x + 7, y + 1, 4, 3, 0x88DFF3E4);
        fill(g, x + 1, y + 6, 2, 2, 0xFFE6BD4F);
    }

    private static void drawPortal(Graphics2D g, int center, int midY, int gold) {
        drawRing(g, center, midY, 35, 51, 0xAADDAC65, 4);
        drawRing(g, center, midY, 25, 39, 0xCCF2D285, 2);
        fill(g, center - 10, midY - 32, 20, 63, 0x4F7D6E54);
        fill(g, center - 5, midY - 27, 10, 52, 0x5586B3A4);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            int x = center + (int) (Math.cos(angle) * 37);
            int y = midY + (int) (Math.sin(angle) * 52);
            fill(g, x - 2, y - 2, 4, 4, gold);
        }
    }

    private static void drawContractSeal(Graphics2D g, int center, int y, int size, int gold) {
        fill(g, center - size, y - size, size * 2, 2, gold);
        fill(g, center - size, y + size - 2, size * 2, 2, gold);
        fill(g, center - size, y - size, 2, size * 2, gold);
        fill(g, center + size - 2, y - size, 2, size * 2, gold);
        fill(g, center - 5, y - 9, 10, 17, 0xFF554630);
        fill(g, center - 3, y - 7, 6, 2, gold);
        fill(g, center - 3, y - 2, 6, 2, gold);
        fill(g, center - 3, y + 3, 4, 2, gold);
        fill(g, center + 6, y + 5, 2, 4, gold);
    }

    private static void drawEye(Graphics2D g, int centerX, int centerY, int radius, int color) {
        fill(g, centerX - radius, centerY - radius / 2, radius * 2, radius, color);
        fill(g, centerX - radius + 4, centerY - radius / 2 - 3, radius * 2 - 8, radius + 6, color);
        fill(g, centerX - radius / 3, centerY - radius / 3, radius / 1, radius / 1, 0xFF21162D);
        fill(g, centerX - 3, centerY - 5, 6, 10, 0xFFFFE6FF);
        fill(g, centerX - 1, centerY - 3, 3, 6, 0xFF7B3FA0);
    }

    private static void drawRing(Graphics2D g, int cx, int cy, int rx, int ry, int color, int thickness) {
        g.setColor(new Color(color, true));
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.drawOval(cx - rx, cy - ry, rx * 2, ry * 2);
        for (int i = 0; i < 8; i++) {
            int x = cx - 2 + i * 4;
            fill(g, x, cy - ry - 1, 3, 2, lighter(color, 18));
        }
    }

    private static void drawHexRing(Graphics2D g, int cx, int cy, int radius, int color, int thickness) {
        Polygon hex = regularHex(cx, cy, radius);
        g.setColor(new Color(color, true));
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.drawPolygon(hex);
        for (int i = 0; i < 6; i++) {
            fill(g, hex.xpoints[i] - 2, hex.ypoints[i] - 2, 4, 4, 0xFFB9F2FF);
        }
    }

    private static Polygon regularHex(int cx, int cy, int radius) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            polygon.addPoint(cx + (int) (radius * Math.cos(angle)), cy + (int) (radius * Math.sin(angle)));
        }
        return polygon;
    }

    private static void drawTendril(Graphics2D g, int x1, int y1, int x2, int y2, int color) {
        g.setColor(new Color(color, true));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.drawLine(x1, y1, x1 + (x2 - x1) / 3, y1 - 5);
        g.drawLine(x1 + (x2 - x1) / 3, y1 - 5, x1 + (x2 - x1) * 2 / 3, y2 + 5);
        g.drawLine(x1 + (x2 - x1) * 2 / 3, y2 + 5, x2, y2);
        fill(g, x2 - 2, y2 - 2, 4, 4, lighter(color, 36));
    }

    private static void drawThread(Graphics2D g, int x1, int y1, int x2, int y2, int color) {
        g.setColor(new Color(color, true));
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.drawLine(x1, y1, x2, y2);
        fill(g, x2 - 1, y2 - 1, 3, 3, lighter(color, 16));
    }

    private static void drawVoxelCube(Graphics2D g, int x, int y, int w, int h, int depth, int front, int top, int side) {
        Polygon topFace = new Polygon(new int[]{x, x + depth, x + w + depth, x + w}, new int[]{y, y - depth, y - depth, y}, 4);
        Polygon sideFace = new Polygon(new int[]{x + w, x + w + depth, x + w + depth, x + w}, new int[]{y, y - depth, y + h - depth, y + h}, 4);
        g.setColor(new Color(side, true)); g.fillPolygon(sideFace);
        g.setColor(new Color(top, true)); g.fillPolygon(topFace);
        fill(g, x, y, w, h, front);
        fill(g, x + 1, y + 1, Math.max(1, w - 2), 2, lighter(front, 12));
        fill(g, x + w - 2, y + 3, 1, Math.max(1, h - 5), darker(front, 12));
    }

    private static void drawForeground(Graphics2D g, int theme) {
        int color = switch (theme) {
            case 0 -> 0xFF463653;
            case 1 -> 0xFF6A5039;
            case 2, 5 -> 0xFF45603C;
            case 3 -> 0xFF4A6D77;
            default -> 0xFF70603D;
        };
        fill(g, 0, 132, WIDTH, 12, 0xFF18171B);
        fill(g, 0, 132, WIDTH, 2, darker(color, 6));
        for (int i = 0; i < 8; i++) {
            int x = i * 35 - 8;
            fill(g, x, 133 + (i % 3), 20, 2, darker(color, 15));
            fill(g, x + 8, 136 + (i % 2), 10, 2, color);
        }
    }

    private static void drawVignette(Graphics2D g, int theme) {
        for (int i = 0; i < 6; i++) {
            int alpha = 8 + i * 7;
            fill(g, i * 4, 0, 4, HEIGHT, (alpha << 24) | 0x000A0910);
            fill(g, WIDTH - 4 - i * 4, 0, 4, HEIGHT, (alpha << 24) | 0x000A0910);
        }
        for (int i = 0; i < 4; i++) {
            int alpha = 8 + i * 8;
            fill(g, 0, HEIGHT - 5 - i * 5, WIDTH, 5, (alpha << 24) | 0x000A0910);
        }
        int accent = ACCENTS[theme];
        fill(g, 18, 18, 2, 2, 0x55FFFFFF);
        fill(g, 232, 28, 2, 2, 0x66FFFFFF);
        fill(g, 42, 96, 2, 2, accent);
    }

    private static BufferedImage createIcon(int index) {
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        int accent = ACCENTS[index];
        fill(g, 6, 6, 52, 52, 0xC6151319);
        fill(g, 13, 5, 38, 2, 0xFFB89A6A);
        fill(g, 13, 57, 38, 2, 0xFF806C4D);
        fill(g, 5, 13, 2, 38, 0xFFB89A6A);
        fill(g, 57, 13, 2, 38, 0xFF806C4D);
        fill(g, 9, 9, 46, 1, accent);
        fill(g, 9, 54, 46, 1, 0xFF4D4542);
        switch (index) {
            case 0 -> drawEye(g, 32, 32, 17, accent);
            case 1 -> {
                fill(g, 13, 17, 38, 3, accent);
                fill(g, 30, 17, 4, 30, accent);
                fill(g, 19, 24, 4, 15, 0xFFD4A068);
                fill(g, 41, 24, 4, 15, 0xFFD4A068);
                fill(g, 22, 37, 20, 3, 0xFFD4A068);
                fill(g, 23, 40, 5, 13, 0xFFAA7B4C);
                fill(g, 36, 40, 5, 13, 0xFFAA7B4C);
                fill(g, 18, 13, 2, 16, 0xFFE5C078);
                fill(g, 44, 13, 2, 16, 0xFFE5C078);
            }
            case 2 -> {
                fill(g, 16, 27, 32, 20, 0xFF45634A);
                fill(g, 20, 17, 24, 19, accent);
                fill(g, 23, 22, 6, 4, 0xFF2E3A2C);
                fill(g, 35, 22, 6, 4, 0xFF2E3A2C);
                fill(g, 29, 30, 7, 3, 0xFFFFD3A8);
                fill(g, 13, 39, 10, 3, 0xFF9B6262);
                fill(g, 42, 36, 10, 3, 0xFF9B6262);
                fill(g, 17, 49, 3, 7, 0xFF86A464);
                fill(g, 44, 46, 3, 8, 0xFF86A464);
            }
            case 3 -> drawHexRing(g, 32, 32, 20, accent, 4);
            case 4 -> {
                g.setColor(new Color(accent, true));
                g.setStroke(new BasicStroke(4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
                g.drawOval(12, 20, 25, 20);
                g.drawOval(27, 20, 25, 20);
                fill(g, 29, 24, 6, 16, 0xFF17151B);
                fill(g, 30, 27, 4, 2, accent);
                fill(g, 30, 32, 4, 2, accent);
                fill(g, 30, 37, 3, 2, accent);
            }
            case 5 -> {
                int[][] points = {{24, 21}, {35, 17}, {44, 25}, {40, 38}, {28, 43}, {17, 35}, {32, 31}};
                for (int i = 0; i < points.length; i++) {
                    int x = points[i][0], y = points[i][1];
                    fill(g, x - 4, y - 3, 8, 6, 0xFF2B2A20);
                    fill(g, x - 2, y - 2, 4, 4, i % 2 == 0 ? accent : 0xFFE2C04F);
                    fill(g, x - 1, y - 5, 3, 2, 0xFFD9F0DA);
                }
            }
            default -> throw new IllegalArgumentException("Unknown icon index " + index);
        }
        g.dispose();
        return icon;
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int alpha = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
        int red = (int) (((a >>> 16) & 255) * (1 - t) + ((b >>> 16) & 255) * t);
        int green = (int) (((a >>> 8) & 255) * (1 - t) + ((b >>> 8) & 255) * t);
        int blue = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int lighter(int color, int amount) {
        return alter(color, amount);
    }

    private static int darker(int color, int amount) {
        return alter(color, -amount);
    }

    private static int alter(int color, int amount) {
        int alpha = color >>> 24 & 255;
        int red = Math.max(0, Math.min(255, (color >>> 16 & 255) + amount));
        int green = Math.max(0, Math.min(255, (color >>> 8 & 255) + amount));
        int blue = Math.max(0, Math.min(255, (color & 255) + amount));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static void fill(Graphics2D g, int x, int y, int w, int h, int argb) {
        g.setColor(new Color(argb, true));
        g.fillRect(x, y, w, h);
    }
}

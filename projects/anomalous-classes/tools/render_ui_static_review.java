import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Creates reproducible, off-screen review images from the exact shipped textures and current UI geometry.
 * This is a Java2D static reference, not Minecraft's renderer or a client screenshot.
 */
public final class render_ui_static_review {
    private static final int W = 512, H = 288, SCALE = 4;
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path RES = ROOT.resolve("src/main/resources/assets/anomalousclasses");
    private static final Path OUT = ROOT.resolve("ui/reviews/2026-11-11");
    private static BufferedImage atlas, surface, titlePlate, buttons, swarmArt, swarmIcon, swarmBackground, nodeFrames;

    private record Box(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }
    private record Metric(String label, String value) {}

    private render_ui_static_review() {}

    public static void main(String[] args) throws Exception {
        if (args.length > 0) throw new IllegalArgumentException("Run from the anomalous-classes project directory.");
        Files.createDirectories(OUT);
        atlas = load("textures/gui/menu_v1/frame_atlas.png");
        surface = load("textures/gui/menu_v1/surface_tile.png");
        titlePlate = load("textures/gui/menu_v1/title_plate.png");
        buttons = load("textures/gui/menu_v1/button_states.png");
        swarmArt = load("textures/gui/class_cards_v3/swarm_caster.png");
        swarmIcon = load("textures/gui/class_icons_v2/swarm_caster.png");
        swarmBackground = load("textures/gui/skilltree_v1/backgrounds/swarm_caster.png");
        nodeFrames = load("textures/gui/menu_v1/node_states.png");

        write(OUT.resolve("frame_before_after.png"), frameComparison());
        write(OUT.resolve("profile_layout_ko.png"), profile("ko_kr"));
        write(OUT.resolve("profile_layout_en.png"), profile("en_us"));
        write(OUT.resolve("tooltip_ko.png"), tooltip("ko_kr"));
        write(OUT.resolve("tooltip_en.png"), tooltip("en_us"));
        System.out.println("STATIC REVIEW IMAGES: " + OUT);
    }

    private static BufferedImage frameComparison() {
        int logicalW = W * 2, logicalH = H;
        BufferedImage result = canvas(logicalW, logicalH, 2, new Color(18, 13, 23));
        Graphics2D g = graphics(result, 2);
        panel(g, new Box(10, 10, 492, 268), true);
        panel(g, new Box(W + 10, 10, 492, 268), false);
        text(g, "BEFORE | original guessed atlas UV", 22, 34, 0xFFFFFFFF, "en_us");
        text(g, "AFTER | isolated source parts, fixed-scale tiled rails", W + 22, 34, 0xFFFFE4B4, "en_us");
        g.dispose();
        return result;
    }

    private static BufferedImage profile(String locale) throws IOException {
        BufferedImage result = canvas(W, H, SCALE, new Color(22, 15, 27));
        Graphics2D g = graphics(result, SCALE);
        g.setColor(new Color(13, 9, 19));
        g.fillRect(0, 0, W, H);

        int margin = W < 360 ? 5 : 10;
        int top = H < 250 ? 4 : 10;
        Box outer = new Box(margin, top, Math.max(1, W - margin * 2), Math.max(1, H - top * 2));
        boolean compact = W < 540 || H < 310;
        int frameInset = Math.min(30, Math.max(12, Math.min(outer.w(), outer.h()) / 10));
        int header = Math.max(H < 250 ? 17 : 23, frameInset + 7);
        int bodyY = outer.y() + header + 5;
        int navHeight = Math.max(19, Math.min(23, H / 15));
        int navY = outer.bottom() - navHeight - Math.max(7, frameInset / 2);
        int bodyBottom = navY - 7;
        int bodyHeight = Math.max(1, bodyBottom - bodyY);
        int gap = 7;
        int contentX = outer.x() + frameInset;
        int contentRight = outer.right() - frameInset;
        int innerWidth = Math.max(1, contentRight - contentX);
        int leftWidth = Math.min((innerWidth - gap) / 2, Math.max(90, Math.round(innerWidth * 0.44f)));
        int artHeight = Math.max(1, Math.min(bodyHeight, leftWidth * 9 / 16));
        Box art = new Box(contentX, bodyY + Math.max(0, (bodyHeight - artHeight) / 2), Math.max(1, leftWidth), artHeight);
        Box info = new Box(art.right() + gap, bodyY, Math.max(1, contentRight - art.right() - gap), bodyHeight);
        Box stats = new Box(info.x(), info.y() + 68, info.w(), Math.max(1, info.bottom() - (info.y() + 68) - 2));

        panel(g, outer, false);
        drawTitle(g, locale.equals("ko_kr") ? "프로필" : "Profile", outer, locale);
        drawArtCover(g, swarmArt, art);
        insetSurface(g, info);
        insetSurface(g, stats);
        int iconSize = Math.min(26, 9 + 8);
        drawImage(g, swarmIcon, info.x(), info.y() + 2, iconSize, iconSize, 0, 0, 96, 96);
        String className = localized(locale, "class.anomalousclasses.swarm_caster");
        drawFit(g, className, info.x() + iconSize + 5, info.y() + 2 + Math.max(0, (iconSize - 9) / 2),
                Math.max(1, info.right() - (info.x() + iconSize + 5)), 0xFFF4E8D7, locale);

        int line = 10;
        int rowY = info.y() + 38;
        drawMetricRows(g, info, List.of(
                new Metric(localized(locale, "screen.anomalousclasses.profile.level"), "1"),
                new Metric(localized(locale, "screen.anomalousclasses.profile.experience"), "0"),
                new Metric(localized(locale, "screen.anomalousclasses.profile.skill_points"), "0")
        ), rowY, line, locale);
        int statsTitleY = stats.y() + 2;
        drawCentered(g, localized(locale, "screen.anomalousclasses.profile.attributes"),
                stats.x() + stats.w() / 2, statsTitleY, stats.w() - 8, 0xFFE6C788, locale);
        int statY = statsTitleY + line;
        drawMetricRows(g, stats, List.of(
                new Metric(localized(locale, "screen.anomalousclasses.profile.max_health"), "20"),
                new Metric(localized(locale, "screen.anomalousclasses.profile.attack_damage"), "1"),
                new Metric(localized(locale, "screen.anomalousclasses.profile.movement_speed"), "0.1"),
                new Metric(localized(locale, "screen.anomalousclasses.profile.armor"), "0")
        ), statY, 9, locale);

        int buttonGap = 6;
        int buttonWidth = Math.max(1, Math.min(156, (innerWidth - buttonGap) / 2));
        int buttonX = outer.x() + (outer.w() - (buttonWidth * 2 + buttonGap)) / 2;
        drawButton(g, new Box(buttonX, navY, buttonWidth, navHeight),
                localized(locale, "screen.anomalousclasses.profile.skill_tree"), locale);
        drawButton(g, new Box(buttonX + buttonWidth + buttonGap, navY, buttonWidth, navHeight),
                localized(locale, "screen.anomalousclasses.profile.class_management"), locale);
        g.dispose();
        return result;
    }

    private static BufferedImage tooltip(String locale) throws IOException {
        BufferedImage result = canvas(W, H, SCALE, new Color(13, 10, 19));
        Graphics2D g = graphics(result, SCALE);
        double cover = Math.max(W / 1672.0, H / 941.0);
        int drawW = (int) Math.ceil(1672 * cover), drawH = (int) Math.ceil(941 * cover);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        drawImage(g, swarmBackground, (W - drawW) / 2, (H - drawH) / 2, drawW, drawH,
                0, 0, 1672, 941);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(8, 7, 18, 38));
        g.fillRect(0, 25, W, H - 25);

        int margin = W < 420 ? 5 : 9;
        int backWidth = Math.min(76, Math.max(54, W / 7));
        int resetWidth = Math.min(78, Math.max(54, W / 7));
        int spWidth = Math.min(112, Math.max(78, W / 4));
        int resetX = W - margin - resetWidth;
        int spX = resetX - 4 - spWidth;
        int profileX = margin;
        Box back = new Box(profileX, 7, backWidth, 21);
        Box reset = new Box(resetX, 7, resetWidth, 21);
        drawButton(g, back, localized(locale, "screen.anomalousclasses.skill_tree.back"), locale);
        drawButton(g, reset, localized(locale, "screen.anomalousclasses.skill_tree.reset"), locale);
        panel(g, new Box(spX, 6, spWidth, 23), false);
        int leftX = profileX + backWidth + 4;
        int classWidth = Math.max(leftX, spX - 6) - leftX;
        drawImage(g, swarmIcon, leftX + 1, 8, 18, 18, 0, 0, 96, 96);
        drawFit(g, localized(locale, "class.anomalousclasses.swarm_caster"), leftX + 23, 13,
                Math.max(1, classWidth - 24), 0xFFF4E8D4, locale);
        drawFit(g, format(locale, "screen.anomalousclasses.skill_tree.points", "0"), spX + 6, 8,
                spWidth - 12, 0xFFF2E7DB, locale);
        drawFit(g, format(locale, "screen.anomalousclasses.skill_tree.spent", "0"), spX + 6, 17,
                spWidth - 12, 0xFFCDBDD6, locale);

        int nodeX = 195, nodeY = 148, nodeSize = 28;
        g.setColor(new Color(227, 188, 240, 125));
        g.setStroke(new java.awt.BasicStroke(1.4f));
        g.drawLine(128, 195, nodeX, nodeY);
        g.drawLine(nodeX, nodeY, 248, 104);
        drawImage(g, nodeFrames, nodeX - nodeSize / 2, nodeY - nodeSize / 2, nodeSize, nodeSize,
                512, 0, 512, 512);
        BufferedImage skillIcon = load("textures/gui/skilltree_v1/icons/swarm_caster/natural_fission.png");
        drawImage(g, skillIcon, nodeX - 8, nodeY - 8, 16, 16, 0, 0, 256, 256);

        String name = localized(locale, "skill.anomalousclasses.swarm_caster.natural_fission.name");
        String effect = localized(locale, "skill.anomalousclasses.swarm_caster.natural_fission.effect");
        String cost = format(locale, "screen.anomalousclasses.skill_tree.node_cost", "44");
        String status = localized(locale, "screen.anomalousclasses.skill_tree.requirement_missing");
        int maxWidth = Math.min(160, W - 12);
        int measured = Math.max(Math.max(fontWidth(name, locale), fontWidth(effect, locale)),
                Math.max(fontWidth(cost, locale), fontWidth(status, locale)));
        int cardW = Math.max(Math.min(116, maxWidth), Math.min(maxWidth, measured + 20));
        int textW = Math.max(1, cardW - 16);
        List<String> lines = new ArrayList<>();
        for (String line : List.of(name, effect, cost, status)) lines.addAll(wrap(line, textW, locale));
        int cardH = 10 + lines.size() * 11;
        int nodeRight = nodeX + nodeSize / 2;
        int cardX = nodeRight + cardW + 12 <= W ? nodeRight + 9 : nodeX - nodeSize / 2 - cardW - 9;
        cardX = Math.max(6, Math.min(Math.max(6, W - cardW - 6), cardX));
        int mouseY = nodeY;
        int cardY = Math.max(28, Math.min(H - cardH - 5, mouseY + 8));
        if (cardY < 36) cardY = Math.min(H - cardH - 5, 37);
        panel(g, new Box(cardX, cardY, cardW, cardH), false);
        int y = cardY + 6;
        int[] colors = {0xFFFFE8BF, 0xFFE7DFD4, 0xFFF3D48D, 0xFFF1D18A};
        for (int index = 0; index < lines.size(); index++) {
            text(g, lines.get(index), cardX + 8, y + 9, colors[Math.min(index, colors.length - 1)], locale);
            y += 11;
        }
        g.dispose();
        return result;
    }

    private static BufferedImage canvas(int width, int height, int scale, Color color) {
        BufferedImage image = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
        return image;
    }

    private static Graphics2D graphics(BufferedImage image, int scale) {
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        return g;
    }

    private static void panel(Graphics2D g, Box b, boolean legacy) throws RuntimeException {
        if (legacy) {
            legacyPanel(g, b);
            return;
        }
        insetSurface(g, b);
        int corner = Math.max(6, Math.min(40, Math.min(b.w(), b.h()) / 10));
        corner = Math.min(corner, Math.min(b.w() / 2, b.h() / 2));
        int offset = scale(corner, 32, 160), thickness = Math.max(1, scale(corner, 64, 160));
        int centerX = b.x() + b.w() / 2;
        boolean ornaments = corner >= 14 && b.w() >= 120 && b.h() >= 48;
        int jewelW = ornaments ? scale(corner, 64, 160) : 0;
        int jewelH = ornaments ? scale(corner, 88, 160) : 0;
        int jewelLeft = centerX - jewelW / 2, jewelRight = centerX + (jewelW + 1) / 2;
        int edgeStart = b.x() + corner - 1, edgeEnd = b.right() - corner + 1;
        int topY = b.y() + offset, bottomY = b.bottom() - corner + offset;
        int tile = scale(corner, 24, 160);
        if (ornaments) {
            hTiles(g, part("rail_top_left"), edgeStart, topY, jewelLeft, thickness, tile, 24, 64);
            hTiles(g, part("rail_top_right"), jewelRight, topY, edgeEnd, thickness, tile, 24, 64);
            hTiles(g, part("rail_bottom_left"), edgeStart, bottomY, jewelLeft, thickness, tile, 24, 64);
            hTiles(g, part("rail_bottom_right"), jewelRight, bottomY, edgeEnd, thickness, tile, 24, 64);
        } else {
            hTiles(g, part("rail_top_left"), edgeStart, topY, edgeEnd, thickness, tile, 24, 64);
            hTiles(g, part("rail_bottom_left"), edgeStart, bottomY, edgeEnd, thickness, tile, 24, 64);
        }
        int centerY = b.y() + b.h() / 2;
        int sideW = ornaments ? scale(corner, 48, 160) : 0;
        int sideH = ornaments ? scale(corner, 64, 160) : 0;
        int sideY = centerY - sideH / 2, sideLX = b.x() + offset, sideRX = b.right() - corner + offset;
        int sideTile = scale(corner, 24, 160), sideStart = b.y() + corner - 1, sideEnd = b.bottom() - corner + 1;
        if (ornaments) {
            vTiles(g, part("rail_side_left"), sideLX, sideStart, sideY, thickness, sideTile, 64, 24);
            vTiles(g, part("rail_side_left"), sideLX, sideY + sideH, sideEnd, thickness, sideTile, 64, 24);
            vTiles(g, part("rail_side_right"), sideRX, sideStart, sideY, thickness, sideTile, 64, 24);
            vTiles(g, part("rail_side_right"), sideRX, sideY + sideH, sideEnd, thickness, sideTile, 64, 24);
        } else {
            vTiles(g, part("rail_side_left"), sideLX, sideStart, sideEnd, thickness, sideTile, 64, 24);
            vTiles(g, part("rail_side_right"), sideRX, sideStart, sideEnd, thickness, sideTile, 64, 24);
        }
        drawSprite(g, "corner_tl", b.x(), b.y(), corner, corner, 160, 160);
        drawSprite(g, "corner_tr", b.right() - corner, b.y(), corner, corner, 160, 160);
        drawSprite(g, "corner_bl", b.x(), b.bottom() - corner, corner, corner, 160, 160);
        drawSprite(g, "corner_br", b.right() - corner, b.bottom() - corner, corner, corner, 160, 160);
        if (ornaments) {
            int sideJewelLeft = sideLX + (thickness - sideW) / 2;
            int sideJewelRight = sideRX + (thickness - sideW) / 2;
            drawSprite(g, "jewel_side_left", sideJewelLeft, sideY, sideW, sideH, 48, 64);
            drawSprite(g, "jewel_side_right", sideJewelRight, sideY, sideW, sideH, 48, 64);
            int jewelTopY = topY + (thickness - jewelH) / 2;
            int jewelBottomY = bottomY + (thickness - jewelH) / 2;
            drawSprite(g, "jewel_top", jewelLeft, jewelTopY, jewelW, jewelH, 64, 88);
            drawSprite(g, "jewel_bottom", jewelLeft, jewelBottomY, jewelW, jewelH, 64, 88);
        }
    }

    private static void hTiles(Graphics2D g, BufferedImage texture, int x, int y, int end, int height,
                               int tileWidth, int sw, int sh) {
        while (x < end) {
            int dw = Math.min(tileWidth, end - x);
            int cropped = Math.min(sw, (int) Math.ceil(dw * sw / (double) tileWidth));
            drawImage(g, texture, x, y, dw, height, 0, 0, cropped, sh);
            x += dw;
        }
    }

    private static void vTiles(Graphics2D g, BufferedImage texture, int x, int y, int end, int width,
                               int tileHeight, int sw, int sh) {
        while (y < end) {
            int dh = Math.min(tileHeight, end - y);
            int cropped = Math.min(sh, (int) Math.ceil(dh * sh / (double) tileHeight));
            drawImage(g, texture, x, y, width, dh, 0, 0, sw, cropped);
            y += dh;
        }
    }

    private static void drawSprite(Graphics2D g, String name, int x, int y, int w, int h, int sw, int sh) {
        BufferedImage texture = readPart(name);
        drawImage(g, texture, x, y, w, h, 0, 0, sw, sh);
    }

    private static BufferedImage part(String name) { return readPart(name); }
    private static BufferedImage readPart(String name) {
        try { return ImageIO.read(RES.resolve("textures/gui/menu_v1/frame_parts/" + name + ".png").toFile()); }
        catch (IOException error) { throw new RuntimeException(error); }
    }

    private static void insetSurface(Graphics2D g, Box b) {
        for (int y = b.y(); y < b.bottom(); y += 64) {
            int h = Math.min(64, b.bottom() - y);
            for (int x = b.x(); x < b.right(); x += 64) {
                int w = Math.min(64, b.right() - x);
                drawImage(g, surface, x, y, w, h, 0, 0, Math.max(1, w * 4), Math.max(1, h * 4));
            }
        }
    }

    private static void drawTitle(Graphics2D g, String title, Box outer, String locale) {
        int plateH = H < 280 ? 24 : 32;
        int plateW = Math.min(340, Math.max(150, outer.w() * 36 / 100));
        plateW = Math.min(plateW, Math.max(1, outer.w() - 12));
        int cap = Math.max(1, (int) Math.round(plateH * 132.0 / 342.0));
        int jewel = Math.max(1, (int) Math.round(plateH * 112.0 / 342.0));
        int middle = Math.max(2, plateW - cap * 2 - jewel), leftMiddle = middle / 2, rightMiddle = middle - leftMiddle;
        int x = (W - plateW) / 2, y = outer.y() - 3;
        drawImage(g, titlePlate, x, y, cap, plateH, 0, 0, 132, 342);
        drawImage(g, titlePlate, x + cap, y, leftMiddle, plateH, 132, 0, 324, 342);
        drawImage(g, titlePlate, x + cap + leftMiddle, y, jewel, plateH, 456, 0, 112, 342);
        drawImage(g, titlePlate, x + cap + leftMiddle + jewel, y, rightMiddle, plateH, 568, 0, 324, 342);
        drawImage(g, titlePlate, x + plateW - cap, y, cap, plateH, 892, 0, 132, 342);
        drawCentered(g, title, W / 2, y + Math.max(1, (plateH - 9) / 2), plateW - 40, 0xFFF1E7D7, locale);
    }

    private static void drawButton(Graphics2D g, Box b, String label, String locale) {
        int cap = Math.min(b.h(), Math.max(1, b.w() / 2));
        int center = Math.max(0, b.w() - cap * 2);
        int rowY = 0;
        drawImage(g, buttons, b.x(), b.y(), cap, b.h(), 0, rowY, 256, 256);
        drawImage(g, buttons, b.x() + cap, b.y(), center, b.h(), 256, rowY, 512, 256);
        drawImage(g, buttons, b.x() + b.w() - cap, b.y(), cap, b.h(), 768, rowY, 256, 256);
        drawCentered(g, label, b.x() + b.w() / 2, b.y() + Math.max(1, (b.h() - 9) / 2),
                Math.max(1, b.w() - Math.max(8, b.w() / 12)), 0xFFF1E9DB, locale);
    }

    private static void drawMetricRows(Graphics2D g, Box bounds, List<Metric> rows, int y, int rowHeight, String locale) {
        int inset = Math.min(5, Math.max(2, bounds.w() / 24));
        int available = Math.max(1, bounds.w() - inset * 2);
        int labelW = rows.stream().mapToInt(row -> fontWidth(row.label(), locale)).max().orElse(1);
        int valueW = rows.stream().mapToInt(row -> fontWidth(row.value(), locale)).max().orElse(1);
        int gap = Math.min(12, Math.max(5, bounds.w() / 36));
        if (labelW + gap + valueW > available) {
            gap = Math.min(gap, Math.max(2, available / 16));
            valueW = Math.min(valueW, Math.max(1, available / 3));
            labelW = Math.max(1, available - gap - valueW);
        }
        int groupW = Math.min(available, labelW + gap + valueW);
        int x = bounds.x() + (bounds.w() - groupW) / 2;
        int right = x + labelW + gap + valueW;
        for (int i = 0; i < rows.size(); i++) {
            Metric row = rows.get(i);
            drawFit(g, row.label(), x, y + i * rowHeight, labelW, 0xFFD8D0C5, locale);
            drawFitRight(g, row.value(), right, y + i * rowHeight, valueW, 0xFFF1E7D7, locale);
        }
    }

    private static void drawArtCover(Graphics2D g, BufferedImage image, Box b) {
        float sourceAspect = 16f / 9f, targetAspect = b.w() / (float) b.h();
        int sw = 512, sh = 288;
        int u = 0, v = 0;
        if (targetAspect < sourceAspect) { sw = Math.max(1, Math.round(288 * targetAspect)); u = (512 - sw) / 2; }
        else { sh = Math.max(1, Math.round(512 / targetAspect)); v = (288 - sh) / 2; }
        Graphics2D clip = (Graphics2D) g.create();
        clip.clipRect(b.x(), b.y(), b.w(), b.h());
        clip.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        drawImage(clip, image, b.x(), b.y(), b.w(), b.h(), u, v, sw, sh);
        clip.dispose();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    private static void drawImage(Graphics2D g, BufferedImage image, int x, int y, int w, int h,
                                  int u, int v, int sw, int sh) {
        if (w <= 0 || h <= 0 || sw <= 0 || sh <= 0) return;
        int tw = image.getWidth(), th = image.getHeight();
        g.drawImage(image, x, y, x + w, y + h, u, v, Math.min(tw, u + sw), Math.min(th, v + sh), null);
    }

    private static void drawFit(Graphics2D g, String value, int x, int y, int maxWidth, int color, String locale) {
        text(g, fit(value, maxWidth, locale), x, y + 9, color, locale);
    }

    private static void drawFitRight(Graphics2D g, String value, int right, int y, int maxWidth, int color, String locale) {
        String fitted = fit(value, maxWidth, locale);
        text(g, fitted, right - fontWidth(fitted, locale), y + 9, color, locale);
    }

    private static void drawCentered(Graphics2D g, String value, int center, int y, int maxWidth, int color, String locale) {
        String fitted = fit(value, maxWidth, locale);
        text(g, fitted, center - fontWidth(fitted, locale) / 2, y + 9, color, locale);
    }

    private static void text(Graphics2D g, String value, int x, int baseline, int color, String locale) {
        g.setFont(font(locale));
        g.setColor(new Color(color, true));
        g.drawString(value, x, baseline);
    }

    private static Font font(String locale) {
        return new Font(locale.equals("ko_kr") ? "Malgun Gothic" : "Monospaced", Font.PLAIN, 9);
    }

    private static int fontWidth(String value, String locale) {
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = measure.createGraphics();
        g.setFont(font(locale));
        int width = g.getFontMetrics().stringWidth(value);
        g.dispose();
        return width;
    }

    private static String fit(String value, int maxWidth, String locale) {
        if (maxWidth <= 0) return "";
        if (fontWidth(value, locale) <= maxWidth) return value;
        String suffix = "…";
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int cp = value.codePointAt(offset);
            String candidate = result.toString() + new String(Character.toChars(cp)) + suffix;
            if (fontWidth(candidate, locale) > maxWidth) break;
            result.appendCodePoint(cp);
            offset += Character.charCount(cp);
        }
        return result + suffix;
    }

    private static List<String> wrap(String value, int maxWidth, String locale) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int cp = value.codePointAt(offset);
            String glyph = new String(Character.toChars(cp));
            if (fontWidth(current + glyph, locale) > maxWidth && current.length() > 0) {
                lines.add(current.toString().stripTrailing());
                current.setLength(0);
                if (!Character.isWhitespace(cp)) current.append(glyph);
            } else current.append(glyph);
            offset += Character.charCount(cp);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private static String format(String locale, String key, String value) throws IOException {
        return localized(locale, key).replace("%s", value);
    }

    private static String localized(String locale, String key) throws IOException {
        String json = Files.readString(RES.resolve("lang/" + locale + ".json"));
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
                .matcher(json);
        if (!matcher.find()) throw new IllegalStateException("Missing " + locale + " translation " + key);
        return matcher.group(1).replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
    }

    private static int scale(int value, int numerator, int denominator) {
        return Math.max(1, Math.round(value * (float) numerator / denominator));
    }

    private static BufferedImage load(String relative) throws IOException {
        return ImageIO.read(RES.resolve(relative).toFile());
    }

    private static void write(Path path, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", path.toFile());
        System.out.printf("%s (%d x %d)%n", path, image.getWidth(), image.getHeight());
    }

    // Replays the previous atlas UV and stretched-rail code for an apples-to-apples source comparison.
    private static void legacyPanel(Graphics2D g, Box b) {
        insetSurface(g, b);
        int cell = 256;
        int corner = Math.max(6, Math.min(40, Math.min(b.w(), b.h()) / 10));
        corner = Math.min(corner, Math.min(b.w() / 2, b.h() / 2));
        int edge = Math.max(4, Math.min(22, corner * 2 / 3));
        int jewelWidth = Math.max(4, corner / 2);
        jewelWidth = Math.min(jewelWidth, Math.max(1, b.w() - corner * 2));
        int sideJewelWidth = Math.min(corner, Math.max(4, corner * 5 / 6));
        int middleX = b.x() + b.w() / 2;
        int topRailY = b.y() + corner / 2 - edge / 2;
        int bottomRailY = b.bottom() - corner / 2 - edge / 2;
        int topJewelY = b.y(), bottomJewelY = b.bottom() - corner;
        int sourceH = Math.min(50, Math.max(12, edge * 3));
        hLegacy(g, b, corner, edge, middleX, jewelWidth, topRailY, 0, 0, 83 - sourceH / 2, 84, 24, sourceH);
        hLegacy(g, b, corner, edge, middleX, jewelWidth, bottomRailY, 0, 512, 170 - sourceH / 2, 84, 24, sourceH);
        vLegacy(g, b, corner, edge, 0, 256, 54, 0, 106, 150, 106, 52, 104, 60, 50, sideJewelWidth);
        vLegacy(g, b, corner, edge, 512, 256, 148, 0, 106, 150, 106, 148, 104, 60, 50, sideJewelWidth);
        int jewelSourceX = cell + 104;
        drawImage(g, atlas, middleX - jewelWidth / 2, topJewelY, jewelWidth, corner, jewelSourceX, 16, 48, 128);
        drawImage(g, atlas, middleX - jewelWidth / 2, bottomJewelY, jewelWidth, corner, jewelSourceX, cell * 2 + 112, 48, 128);
        drawImage(g, atlas, b.x(), b.y(), corner, corner, 0, 0, 144, 144);
        drawImage(g, atlas, b.right() - corner, b.y(), corner, corner, cell * 2 + 112, 0, 144, 144);
        drawImage(g, atlas, b.x(), b.bottom() - corner, corner, corner, 0, cell * 2 + 112, 144, 144);
        drawImage(g, atlas, b.right() - corner, b.bottom() - corner, corner, corner,
                cell * 2 + 112, cell * 2 + 112, 144, 144);
    }

    private static void hLegacy(Graphics2D g, Box b, int corner, int edge, int middleX, int jewelWidth,
                                int y, int sourceCellX, int sourceCellY, int sourceY,
                                int sourceX, int sourceWidth, int sourceHeight) {
        int left = b.x() + corner * 3 / 4;
        int right = b.right() - corner * 3 / 4;
        int jewelLeft = middleX - jewelWidth / 2;
        int jewelRight = middleX + (jewelWidth + 1) / 2;
        drawImage(g, atlas, left, y, Math.max(0, jewelLeft - left), edge,
                sourceCellX + sourceX, sourceCellY + sourceY, sourceWidth, sourceHeight);
        drawImage(g, atlas, jewelRight, y, Math.max(0, right - jewelRight), edge,
                sourceCellX + 150, sourceCellY + sourceY, sourceWidth, sourceHeight);
    }

    private static void vLegacy(Graphics2D g, Box b, int corner, int edge, int cellX, int cellY,
                                int railX, int upperStart, int upperHeight, int lowerStart, int lowerHeight,
                                int jewelX, int jewelY, int jewelW, int jewelH, int jewelWidth) {
        int centerY = b.y() + b.h() / 2;
        int jewelHeight = Math.max(4, corner * 3 / 4);
        int top = b.y() + corner * 3 / 4, bottom = b.bottom() - corner * 3 / 4;
        int jewelTop = centerY - jewelHeight / 2, jewelBottom = centerY + (jewelHeight + 1) / 2;
        boolean left = cellX == 0;
        int destX = left ? b.x() + corner / 2 - edge / 2 : b.right() - corner / 2 - edge / 2;
        int upperDest = Math.max(0, jewelTop - top), lowerDest = Math.max(0, bottom - jewelBottom);
        int railWidth = Math.min(58, Math.max(16, edge * 3));
        int srcRailX = railX + (58 - railWidth) / 2;
        int upperSrcH = Math.min(upperHeight, Math.max(12, upperDest * 4));
        int lowerSrcH = Math.min(lowerHeight, Math.max(12, lowerDest * 4));
        int upperSrcY = upperStart + (upperHeight - upperSrcH) / 2;
        int lowerSrcY = lowerStart + (lowerHeight - lowerSrcH) / 2;
        drawImage(g, atlas, destX, top, edge, upperDest, cellX + srcRailX, cellY + upperSrcY, railWidth, upperSrcH);
        drawImage(g, atlas, destX, jewelBottom, edge, lowerDest, cellX + srcRailX, cellY + lowerSrcY, railWidth, lowerSrcH);
        int jewelDestX = left ? b.x() + corner / 2 - jewelWidth / 2 : b.right() - corner / 2 - jewelWidth / 2;
        drawImage(g, atlas, jewelDestX, jewelTop, jewelWidth, jewelHeight,
                cellX + jewelX, cellY + jewelY, jewelW, jewelH);
    }
}

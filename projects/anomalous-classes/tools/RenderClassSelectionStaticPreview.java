import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import kr.moonseungjun.anomalousclasses.client.ClassSelectionLayout;
import kr.moonseungjun.anomalousclasses.client.ClassSelectionLayout.Box;
import kr.moonseungjun.anomalousclasses.client.ClassSelectionLayout.Layout;

/** Creates a source-backed Java2D layout preview; this is not a Minecraft screenshot. */
public final class RenderClassSelectionStaticPreview {
    private static final String[] KEYS = {
            "mental_dominator", "puppeteer", "parasite_morpher",
            "barrier_caster", "pact_caster", "swarm_caster"
    };
    private static final String[] KO_NAMES = {"정신 지배자", "인형술사", "기생술사", "결계술사", "계약술사", "군체술사"};
    private static final String[] EN_NAMES = {"Mental Dominator", "Puppeteer", "Parasite Morpher", "Barrier Caster", "Pact Caster", "Swarm Caster"};
    private static final String[] KO_DESCRIPTIONS = {
            "지배한 몹을 지휘합니다. 지배를 유지하려면 정신력이 필요합니다.",
            "직접 만든 인형을 꺼내 실로 움직입니다.",
            "숙주의 몸을 차지해 싸우며 생체 데이터를 쌓습니다.",
            "방어·압축·격리 결계를 배치합니다.",
            "계약한 존재의 가호와 능력을 빌립니다.",
            "군체를 공격·엄호·이동에 나눠 씁니다."
    };
    private static final String[] EN_DESCRIPTIONS = {
            "Command creatures you dominate. Maintaining control costs focus.",
            "Deploy puppets you craft and move them by thread.",
            "Take over a host, then fight and build biodata.",
            "Place barriers to defend, crush, or isolate.",
            "Borrow protection and abilities from your pact.",
            "Use your swarm to attack, cover, or travel."
    };
    private static final int[] COLORS = {
            0xFFC69AE9, 0xFFD6A36D, 0xFF8CBF82, 0xFF70BFD5, 0xFFD0B47C, 0xFFB4C875
    };
    private static boolean english;

    private RenderClassSelectionStaticPreview() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 6) {
            throw new IllegalArgumentException("Usage: <assets/anomalousclasses directory> <output.png> [logicalWidth] [logicalHeight] [guiScale] [ko|en]");
        }
        Path assets = Path.of(args[0]);
        File output = new File(args[1]);
        int logicalWidth = args.length > 2 ? Integer.parseInt(args[2]) : 960;
        int logicalHeight = args.length > 3 ? Integer.parseInt(args[3]) : 540;
        int outputScale = args.length > 4 ? Integer.parseInt(args[4]) : 2;
        english = args.length > 5 && args[5].equalsIgnoreCase("en");
        Layout layout = ClassSelectionLayout.calculate(logicalWidth, logicalHeight);
        BufferedImage image = new BufferedImage(layout.screenWidth() * outputScale,
                layout.screenHeight() * outputScale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(outputScale, outputScale);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        drawBackground(graphics, layout.screenWidth(), layout.screenHeight());
        drawHeader(graphics, layout);
        drawSideCards(graphics, assets, layout);
        drawMainCard(graphics, assets, layout, 0);
        drawSideLabels(graphics, layout);
        drawFooter(graphics, assets, layout);
        drawButtons(graphics, layout);
        graphics.dispose();
        File parent = output.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IllegalStateException("Cannot create " + parent);
        ImageIO.write(image, "png", output);
        System.out.printf("STATIC PREVIEW (not a Minecraft screenshot): %s (%dx%d output, %dx%d logical, GUI scale %d, %s)%n",
                output, image.getWidth(), image.getHeight(), logicalWidth, logicalHeight, outputScale, english ? "English" : "Korean");
    }

    private static String[] names() { return english ? EN_NAMES : KO_NAMES; }
    private static String[] descriptions() { return english ? EN_DESCRIPTIONS : KO_DESCRIPTIONS; }

    private static void drawBackground(Graphics2D g, int width, int height) {
        g.setPaint(new LinearGradientPaint(new Point2D.Float(0, 0), new Point2D.Float(width, height),
                new float[] {0.0f, 0.48f, 1.0f}, new Color[] {new Color(44, 38, 52), new Color(22, 21, 29), new Color(37, 31, 43)}));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(114, 86, 132, 30));
        for (int index = 0; index < 12; index++) {
            int x = (index * 113 + 31) % width;
            int y = 44 + (index * 71 + 13) % Math.max(1, height - 100);
            g.fillRect(x, y, 2 + index % 3, 2 + index % 3);
        }
        g.setColor(new Color(8, 8, 12, 110));
        g.fillRect(0, height - 62, width, 62);
        g.setColor(new Color(177, 143, 104, 90));
        g.setStroke(new BasicStroke(1.0f));
        for (int x = 0; x < width; x += 32) g.drawLine(x, height - 28, x + 18, height - 28);
    }

    private static void drawHeader(Graphics2D g, Layout layout) {
        setFont(g, Font.BOLD, 10);
        drawCentered(g, english ? "Choose a Class" : "직업 선택", layout.screenWidth() / 2, 18, 300, new Color(244, 232, 214));
        setFont(g, Font.PLAIN, 7);
        drawCentered(g, english ? "Choose a class before entering the world." : "세계로 나가기 전에 직업을 고르세요.",
                layout.screenWidth() / 2, 32, 500, new Color(211, 201, 188));
    }

    private static void drawSideCards(Graphics2D g, Path assets, Layout layout) throws Exception {
        int previous = 5;
        int next = 1;
        drawSideCard(g, assets, layout.previousPreview(), layout.previousVisible(), previous);
        drawSideCard(g, assets, layout.nextPreview(), layout.nextVisible(), next);
    }

    private static void drawSideCard(Graphics2D g, Path assets, Box preview, Box visible, int index) throws Exception {
        BufferedImage art = ImageIO.read(assets.resolve("textures/gui/class_cards_v3/" + KEYS[index] + ".png").toFile());
        g.setColor(new Color(24, 22, 29));
        g.fillRect(preview.x(), preview.y(), preview.width(), preview.height());
        drawFrame(g, preview, new Color(154, 139, 119, 180), new Color(17, 15, 20, 140));
        int thumbnailWidth = Math.max(1, visible.width() - 6);
        int thumbnailHeight = Math.min(visible.height() - 24, Math.max(1, Math.round(thumbnailWidth * 9.0f / 16.0f)));
        thumbnailWidth = Math.min(thumbnailWidth, Math.max(1, Math.round(thumbnailHeight * 16.0f / 9.0f)));
        Box thumbnail = new Box(visible.x() + (visible.width() - thumbnailWidth) / 2,
                visible.y() + Math.max(2, (visible.height() - thumbnailHeight) / 2), thumbnailWidth, thumbnailHeight);
        g.setColor(new Color(92, 82, 96));
        g.fillRect(thumbnail.x() - 1, thumbnail.y() - 1, thumbnail.width() + 2, thumbnail.height() + 2);
        drawCover(g, art, thumbnail);
    }

    private static void drawMainCard(Graphics2D g, Path assets, Layout layout, int offset) throws Exception {
        Box panel = shift(layout.card(), offset);
        Box artBox = shift(layout.art(), offset);
        Box details = shift(layout.details(), offset);
        Color accent = color(COLORS[0]);
        g.setColor(new Color(10, 10, 14));
        g.fillRoundRect(panel.x(), panel.y(), panel.width(), panel.height(), 10, 10);
        g.setColor(new Color(206, 191, 168));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(panel.x() + 1, panel.y() + 1, panel.width() - 2, panel.height() - 2, 9, 9);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150));
        g.setStroke(new BasicStroke(1));
        g.drawLine(panel.x() + 14, panel.y() + 3, panel.right() - 14, panel.y() + 3);
        g.setColor(new Color(27, 24, 33));
        g.fillRect(panel.x() + 5, panel.y() + 5, panel.width() - 10, panel.height() - 10);

        BufferedImage art = ImageIO.read(assets.resolve("textures/gui/class_cards_v3/" + KEYS[0] + ".png").toFile());
        g.setColor(new Color(62, 53, 68));
        g.fillRect(artBox.x() - 1, artBox.y() - 1, artBox.width() + 2, artBox.height() + 2);
        drawCover(g, art, artBox);

        if (layout.compact()) {
            g.setColor(new Color(27, 24, 33, 233));
            g.fillRect(details.x(), details.y(), details.width(), details.height());
            g.setColor(accent);
            if (layout.compactPortrait()) g.fillRect(details.x(), details.y(), details.width(), 1);
            else g.fillRect(details.x(), details.y(), 2, details.height());
        } else {
            g.setColor(new Color(29, 27, 34));
            g.fillRect(details.x(), details.y(), details.width(), details.height());
            g.setColor(accent);
            if (layout.stacked()) g.fillRect(details.x(), details.y(), details.width(), 1);
            else g.fillRect(details.x(), details.y(), 2, details.height());
        }

        if (layout.compactPortrait()) {
            drawCompactPortraitInfo(g, assets, layout, offset);
            g.setColor(new Color(196, 156, 233));
            g.fillOval(panel.x() + 8, panel.y() + 8, 4, 4);
            return;
        }

        int pad = layout.compact() ? 6 : 12;
        int textX = details.x() + pad;
        int textWidth = Math.max(1, details.width() - pad * 2);
        BufferedImage icon = ImageIO.read(assets.resolve("textures/gui/class_icons_v2/" + KEYS[0] + ".png").toFile());
        int iconSize = layout.compact() ? 17 : 21;
        setFont(g, Font.PLAIN, 7);
        List<String> lines = wrap(descriptions()[0], textWidth, g.getFontMetrics());
        FontMetrics textMetrics = g.getFontMetrics();
        int mcLineHeight = 9;
        int headerHeight = iconSize + 4 + mcLineHeight + 4;
        Box action = layout.actionForContent(headerHeight, lines.size(), 0, mcLineHeight);
        int blockTop = action.y() - 10 - lines.size() * mcLineHeight - headerHeight;
        int titleY = blockTop;
        drawPixelIcon(g, icon, textX, titleY - 1, iconSize, iconSize);
        setFont(g, Font.BOLD, 8);
        FontMetrics titleMetrics = g.getFontMetrics();
        drawFit(g, names()[0], textX + iconSize + 5,
                titleY + Math.max(0, (iconSize - titleMetrics.getHeight()) / 2) + titleMetrics.getAscent(),
                textWidth - iconSize - 5, new Color(243, 233, 217));
        setFont(g, Font.PLAIN, 7);
        int stateY = titleY + iconSize + 4;
        drawFit(g, english ? "New class · Lv 1 · SP 0" : "새 직업 · Lv 1 · SP 0",
                textX, stateY + g.getFontMetrics().getAscent(), textWidth, new Color(195, 186, 172));
        int descriptionTop = stateY + mcLineHeight + 4;
        for (int index = 0; index < lines.size(); index++) {
            drawFit(g, lines.get(index), textX,
                    descriptionTop + textMetrics.getAscent() + index * textMetrics.getHeight(),
                    textWidth, new Color(226, 218, 207));
        }
        g.setColor(new Color(196, 156, 233));
        g.fillOval(panel.x() + 8, panel.y() + 8, 4, 4);
    }

    private static void drawCompactPortraitInfo(Graphics2D g, Path assets, Layout layout, int offset) throws Exception {
        Box meta = shift(layout.compactMetaColumn(), offset);
        Box copy = shift(layout.compactCopyColumn(), offset);
        BufferedImage icon = ImageIO.read(assets.resolve("textures/gui/class_icons_v2/" + KEYS[0] + ".png").toFile());
        int iconSize = 17;
        setFont(g, Font.BOLD, 7);
        List<String> titleLines = wrap(names()[0], Math.max(1, meta.width() - iconSize - 4), g.getFontMetrics());
        int titleHeight = Math.max(iconSize, titleLines.size() * 9);
        int titleY = meta.y();
        drawPixelIcon(g, icon, meta.x(), titleY, iconSize, iconSize);
        for (int index = 0; index < titleLines.size(); index++) {
            drawFit(g, titleLines.get(index), meta.x() + iconSize + 4,
                    titleY + g.getFontMetrics().getAscent() + index * 9,
                    Math.max(1, meta.right() - meta.x() - iconSize - 4), new Color(243, 233, 217));
        }
        setFont(g, Font.PLAIN, 7);
        String state = english ? "New class · Lv 1 · SP 0" : "새 직업 · Lv 1 · SP 0";
        List<String> stateLines = wrap(state, meta.width(), g.getFontMetrics());
        int stateY = titleY + titleHeight + 3;
        for (int index = 0; index < stateLines.size(); index++) {
            drawFit(g, stateLines.get(index), meta.x(), stateY + g.getFontMetrics().getAscent() + index * 9,
                    meta.width(), new Color(195, 186, 172));
        }
        List<String> descriptionLines = wrap(descriptions()[0], copy.width(), g.getFontMetrics());
        for (int index = 0; index < descriptionLines.size(); index++) {
            drawFit(g, descriptionLines.get(index), copy.x(), copy.y() + g.getFontMetrics().getAscent() + index * 9,
                    copy.width(), new Color(226, 218, 207));
        }
    }

    private static void drawSideLabels(Graphics2D g, Layout layout) {
        drawSideLabel(g, layout.previousVisible(), 5);
        drawSideLabel(g, layout.nextVisible(), 1);
    }

    private static void drawSideLabel(Graphics2D g, Box visible, int index) {
        g.setClip(visible.x(), visible.y(), visible.width(), visible.height());
        setFont(g, Font.BOLD, 7);
        List<String> lines = wrap(names()[index], Math.max(1, visible.width() - 4), g.getFontMetrics());
        int lineCount = Math.min(2, Math.max(1, lines.size()));
        int bandHeight = lineCount * 9 + 4;
        int y = visible.bottom() - bandHeight;
        g.setColor(new Color(17, 15, 21, 220));
        g.fillRect(visible.x(), y, visible.width(), bandHeight);
        for (int line = 0; line < lineCount; line++) {
            drawCenteredFit(g, lines.get(line), visible.x() + visible.width() / 2,
                    y + 2 + g.getFontMetrics().getAscent() + line * 9,
                    Math.max(1, visible.width() - 4), new Color(231, 222, 208));
        }
        g.setClip(null);
    }

    private static void drawFooter(Graphics2D g, Path assets, Layout layout) throws Exception {
        for (int index = 0; index < KEYS.length; index++) {
            Box box = layout.classTabs().get(index);
            Color accent = color(COLORS[index]);
            g.setColor(new Color(25, 23, 30, 226));
            g.fillRoundRect(box.x(), box.y(), box.width(), box.height(), 8, 8);
            g.setColor(index == 0 ? accent : new Color(105, 98, 91));
            g.setStroke(new BasicStroke(index == 0 ? 2 : 1));
            g.drawRoundRect(box.x(), box.y(), box.width(), box.height(), 8, 8);
            BufferedImage icon = ImageIO.read(assets.resolve("textures/gui/class_icons_v2/" + KEYS[index] + ".png").toFile());
            setFont(g, Font.BOLD, 7);
            List<String> lines = wrap(names()[index], Math.max(1, box.width() - 8), g.getFontMetrics());
            int lineCount = Math.max(1, Math.min(2, lines.size()));
            int iconSize = Math.min(26, Math.max(15, box.height() - lineCount * 9 - 7));
            drawPixelIcon(g, icon, box.x() + (box.width() - iconSize) / 2, box.y() + 3, iconSize, iconSize);
            int labelY = box.bottom() - lineCount * 9 - 3;
            for (int line = 0; line < lineCount; line++) {
                drawCenteredFit(g, lines.get(line), box.x() + box.width() / 2,
                        labelY + g.getFontMetrics().getAscent() + line * 9, box.width() - 6,
                        index == 0 ? new Color(246, 237, 222) : new Color(212, 203, 188));
            }
            if (index == 0) {
                g.setColor(accent);
                g.fillRect(box.x() + box.width() / 2 - 2, box.bottom() - 2, 4, 2);
            }
        }
        if (!layout.compact()) {
            Box held = layout.heldPanel();
            g.setColor(new Color(25, 23, 29, 210));
            g.fillRoundRect(held.x(), held.y(), held.width(), held.height(), 8, 8);
            g.setColor(new Color(104, 99, 105));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(held.x(), held.y(), held.width(), held.height(), 8, 8);
            setFont(g, Font.PLAIN, 7);
            drawFit(g, english ? "Unavailable" : "선택 불가", held.x() + 7, held.y() + 6, held.width() - 14, new Color(197, 188, 174));
            String gravity = english ? "Gravity Caster" : "중력술사";
            String resonance = english ? "Resonance Caster" : "공명술사";
            for (int index = 0; index < layout.heldNames().size(); index++) {
                Box nameBox = layout.heldNames().get(index);
                String name = index == 0 ? gravity : resonance;
                List<String> lines = wrap(name, nameBox.width(), g.getFontMetrics());
                for (int line = 0; line < Math.min(2, lines.size()); line++) {
                    drawCenteredFit(g, lines.get(line), nameBox.x() + nameBox.width() / 2,
                            nameBox.y() + g.getFontMetrics().getAscent() + line * 9,
                            nameBox.width(), new Color(154, 148, 148));
                }
            }
        } else {
            setFont(g, Font.PLAIN, 7);
            String held = english ? "Unavailable · Gravity · Resonance" : "선택 불가 · 중력술사 · 공명술사";
            drawCenteredFit(g, held,
                    layout.heldInline().x() + layout.heldInline().width() / 2,
                    layout.heldInline().y() + g.getFontMetrics().getAscent(),
                    layout.heldInline().width(), new Color(174, 164, 161));
        }
    }

    private static void drawButtons(Graphics2D g, Layout layout) {
        drawArrow(g, layout.previousButton(), false, color(COLORS[0]));
        drawArrow(g, layout.nextButton(), true, color(COLORS[0]));
        Box action = previewAction(g, layout);
        g.setColor(new Color(51, 42, 60));
        g.fillRoundRect(action.x(), action.y(), action.width(), action.height(), 8, 8);
        g.setColor(color(COLORS[0]));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(action.x(), action.y(), action.width(), action.height(), 8, 8);
        setFont(g, Font.BOLD, 8);
        FontMetrics actionMetrics = g.getFontMetrics();
        drawCenteredFit(g, english ? "Choose" : "선택", action.x() + action.width() / 2,
                action.y() + Math.max(0, (action.height() - actionMetrics.getHeight()) / 2) + actionMetrics.getAscent(),
                action.width() - 10, new Color(246, 236, 219));
    }

    private static Box previewAction(Graphics2D g, Layout layout) {
        if (layout.compactPortrait()) return layout.action();
        setFont(g, Font.PLAIN, 7);
        int pad = layout.compact() ? 6 : 12;
        int textWidth = Math.max(1, layout.details().width() - pad * 2);
        int descriptionLines = wrap(descriptions()[0], textWidth, g.getFontMetrics()).size();
        int headerHeight = 21 + 4 + 9 + 4;
        return layout.actionForContent(headerHeight, descriptionLines, 0, 9);
    }

    private static void drawArrow(Graphics2D g, Box box, boolean right, Color accent) {
        g.setColor(new Color(27, 24, 31, 225));
        g.fillRoundRect(box.x(), box.y(), box.width(), box.height(), 6, 6);
        g.setColor(new Color(185, 169, 145));
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(box.x(), box.y(), box.width(), box.height(), 6, 6);
        g.setColor(accent);
        int direction = right ? 1 : -1;
        int centerX = box.x() + box.width() / 2;
        int centerY = box.y() + box.height() / 2;
        for (int row = 0; row < Math.max(5, box.height() / 3); row++) {
            int x = centerX + direction * (row - 2);
            g.fillRect(x, centerY - 3 + row, 2, 2);
        }
    }

    private static void drawFrame(Graphics2D g, Box box, Color edge, Color fill) {
        g.setColor(fill);
        g.fillRect(box.x(), box.y(), box.width(), box.height());
        g.setColor(edge);
        g.setStroke(new BasicStroke(1));
        g.drawRect(box.x(), box.y(), box.width() - 1, box.height() - 1);
    }

    private static void drawPixelIcon(Graphics2D g, BufferedImage icon, int x, int y, int width, int height) {
        Graphics2D pixel = (Graphics2D) g.create();
        pixel.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        pixel.drawImage(icon, x, y, width, height, null);
        pixel.dispose();
    }

    private static void drawCover(Graphics2D g, BufferedImage source, Box target) {
        float scale = Math.max(target.width() / (float) source.getWidth(), target.height() / (float) source.getHeight());
        int drawWidth = Math.round(source.getWidth() * scale);
        int drawHeight = Math.round(source.getHeight() * scale);
        int drawX = target.x() + (target.width() - drawWidth) / 2;
        int drawY = target.y() + (target.height() - drawHeight) / 2;
        Graphics2D child = (Graphics2D) g.create();
        child.clipRect(target.x(), target.y(), target.width(), target.height());
        child.drawImage(source, drawX, drawY, drawWidth, drawHeight, null);
        child.dispose();
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int y, int maxWidth, Color color) {
        drawCenteredFit(g, text, centerX, y, maxWidth, color);
    }

    private static void drawCenteredFit(Graphics2D g, String text, int centerX, int y, int maxWidth, Color color) {
        FontMetrics metrics = g.getFontMetrics();
        float scale = Math.min(1.0f, maxWidth / (float) Math.max(1, metrics.stringWidth(text)));
        drawFit(g, text, Math.round(centerX - metrics.stringWidth(text) * scale / 2.0f), y,
                maxWidth, color, scale);
    }

    private static void drawFit(Graphics2D g, String text, int x, int baselineY, int maxWidth, Color color) {
        FontMetrics metrics = g.getFontMetrics();
        float scale = Math.min(1.0f, maxWidth / (float) Math.max(1, metrics.stringWidth(text)));
        drawFit(g, text, x, baselineY, maxWidth, color, scale);
    }

    private static void drawFit(Graphics2D g, String text, int x, int baselineY, int maxWidth, Color color, float scale) {
        Graphics2D child = (Graphics2D) g.create();
        child.clipRect(x, baselineY - 14, Math.max(0, maxWidth), 20);
        child.translate(x, baselineY);
        child.scale(scale, scale);
        child.setColor(color);
        child.drawString(text, 0, 0);
        child.dispose();
    }

    private static List<String> wrap(String text, int maxWidth, FontMetrics metrics) {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            if (metrics.stringWidth(word) <= maxWidth) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && metrics.stringWidth(candidate) > maxWidth) {
                    result.add(line.toString());
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
                continue;
            }

            if (!line.isEmpty()) {
                result.add(line.toString());
                line.setLength(0);
            }
            for (int offset = 0; offset < word.length();) {
                int codePoint = word.codePointAt(offset);
                String next = new String(Character.toChars(codePoint));
                if (!line.isEmpty() && metrics.stringWidth(line + next) > maxWidth) {
                    result.add(line.toString());
                    line.setLength(0);
                }
                line.append(next);
                offset += Character.charCount(codePoint);
            }
            if (!line.isEmpty()) {
                result.add(line.toString());
                line.setLength(0);
            }
        }
        if (!line.isEmpty()) result.add(line.toString());
        return result;
    }

    private static void setFont(Graphics2D g, int style, int size) {
        String family = "Malgun Gothic";
        g.setFont(new Font(family, style, size));
    }

    private static Color color(int argb) {
        return new Color(argb, true);
    }

    private static Box shift(Box box, int offset) {
        return new Box(box.x() + offset, box.y(), box.width(), box.height());
    }
}

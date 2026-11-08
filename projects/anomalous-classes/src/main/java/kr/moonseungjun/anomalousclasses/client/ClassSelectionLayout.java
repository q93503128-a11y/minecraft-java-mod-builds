package kr.moonseungjun.anomalousclasses.client;

import java.util.ArrayList;
import java.util.List;

/** Screen-space geometry shared by rendering, hitboxes, and static layout review. */
public final class ClassSelectionLayout {
    public record Box(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean inside(int screenWidth, int screenHeight) {
            return x >= 0 && y >= 0 && width >= 0 && height >= 0 && right() <= screenWidth && bottom() <= screenHeight;
        }
        public boolean intersects(Box other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }

    public record Layout(
            int screenWidth,
            int screenHeight,
            boolean compact,
            int margin,
            Box header,
            Box card,
            Box art,
            Box details,
            Box action,
            Box previousPreview,
            Box nextPreview,
            Box previousVisible,
            Box nextVisible,
            Box previousButton,
            Box nextButton,
            List<Box> classTabs,
            Box heldPanel,
            List<Box> heldNames,
            Box heldInline) {
        public Box actionForContent(int headerHeight, int descriptionLineCount, int statusLineCount, int lineHeight) {
            int statusHeight = statusLineCount <= 0 ? 0 : statusLineCount * Math.max(1, lineHeight) + 3;
            int descriptionHeight = Math.max(0, descriptionLineCount) * Math.max(1, lineHeight);
            int blockHeight = headerHeight + descriptionHeight + statusHeight + 10 + action.height();
            int blockTop = details.y() + Math.max(5, (details.height() - blockHeight) / 2);
            int actionY = blockTop + headerHeight + descriptionHeight + statusHeight + 10;
            actionY = Math.min(actionY, details.bottom() - action.height() - 4);
            actionY = Math.max(details.y() + 4, actionY);
            return new Box(action.x(), actionY, action.width(), action.height());
        }
    }

    private ClassSelectionLayout() {}

    public static Layout calculate(int screenWidth, int screenHeight) {
        if (screenWidth < 1 || screenHeight < 1) throw new IllegalArgumentException("Screen dimensions must be positive");

        boolean compact = screenWidth < 700 || screenHeight < 400;
        int margin = clamp(screenWidth / 40, 8, 20);
        int navigationSize = clamp(Math.min(screenWidth, screenHeight) / 14, 16, 28);
        int navigationGap = clamp(screenWidth / 120, 3, 8);
        int bottomMargin = clamp(screenHeight / 36, 5, 10);
        int tabHeight = 48;
        int heldLineHeight = 13;
        int heldGap = 8;
        int heldWidth = compact ? 0 : clamp(screenWidth / 5, 140, 188);
        int footerY = screenHeight - bottomMargin - tabHeight - (compact ? heldLineHeight + 4 : 0);

        int footerWidth = Math.max(1, screenWidth - margin * 2);
        int tabsWidth = compact ? footerWidth : Math.max(1, footerWidth - heldWidth - heldGap);
        int tabGap = Math.min(clamp(screenWidth / 120, 2, 8), Math.max(1, (tabsWidth - 6 * 20) / 5));
        int tabWidth = Math.max(1, (tabsWidth - tabGap * 5) / 6);
        int tabsUsedWidth = tabWidth * 6 + tabGap * 5;
        int tabsX = margin + Math.max(0, (tabsWidth - tabsUsedWidth) / 2);
        List<Box> classTabs = new ArrayList<>(6);
        for (int index = 0; index < 6; index++) {
            classTabs.add(new Box(tabsX + index * (tabWidth + tabGap), footerY, tabWidth, tabHeight));
        }

        Box heldPanel = new Box(margin + tabsWidth + heldGap, footerY, heldWidth, tabHeight);
        List<Box> heldNames = new ArrayList<>(2);
        if (!compact) {
            int nameAreaWidth = Math.max(0, heldWidth - 12);
            int nameGap = 4;
            int nameWidth = Math.max(0, (nameAreaWidth - nameGap) / 2);
            int nameY = footerY + 20;
            int nameHeight = Math.max(0, tabHeight - 23);
            heldNames.add(new Box(heldPanel.x() + 6, nameY, nameWidth, nameHeight));
            heldNames.add(new Box(heldPanel.x() + 6 + nameWidth + nameGap, nameY, nameWidth, nameHeight));
        }
        Box heldInline = compact
                ? new Box(margin, footerY + tabHeight + 3, footerWidth, heldLineHeight)
                : new Box(heldPanel.x() + 4, footerY + 4, heldWidth - 8, 12);

        int sideMinimum = compact ? 28 : 42;
        int sideReserve = margin + navigationSize + navigationGap + sideMinimum;
        int maxCardWidth = Math.max(1, screenWidth - sideReserve * 2);
        int desiredCardWidth = Math.round(screenWidth * (compact ? 0.70f : 0.77f));
        int cardWidth = Math.min(840, Math.min(maxCardWidth, desiredCardWidth));
        int cardX = (screenWidth - cardWidth) / 2;

        int headerHeight = 28;
        int topBound = clamp(screenHeight / 7, 30, 54);
        int bottomBound = Math.max(topBound + 1, footerY - 10);
        int availableCardHeight = Math.max(1, bottomBound - topBound);
        int targetCardHeight = Math.round(screenHeight * (compact ? 0.58f : 0.52f));
        int cardHeight = Math.min(availableCardHeight, targetCardHeight);
        int cardY = topBound + Math.max(0, (availableCardHeight - cardHeight) / 2);
        Box card = new Box(cardX, cardY, cardWidth, cardHeight);

        int frameInset = clamp(cardHeight / 58, 2, 4);
        int innerX = card.x() + frameInset + 1;
        int innerY = card.y() + frameInset + 1;
        int innerWidth = Math.max(1, card.width() - (frameInset + 1) * 2);
        int innerHeight = Math.max(1, card.height() - (frameInset + 1) * 2);
        int minimumDetailWidth = compact
                ? Math.min(118, Math.max(1, innerWidth / 2))
                : Math.min(260, Math.max(170, Math.round(innerWidth * 0.34f)));
        int maximumArtPaneWidth = Math.max(1, innerWidth - minimumDetailWidth);
        int artPaneWidth = compact
                ? Math.min(Math.round(innerWidth * 0.48f), maximumArtPaneWidth)
                : maximumArtPaneWidth;
        int artWidth = Math.min(artPaneWidth, Math.max(1, Math.round(innerHeight * 16.0f / 9.0f)));
        int artHeight = Math.min(innerHeight, Math.max(1, Math.round(artWidth * 9.0f / 16.0f)));
        Box art = new Box(innerX + Math.max(0, (artPaneWidth - artWidth) / 2),
                innerY + Math.max(0, (innerHeight - artHeight) / 2), artWidth, artHeight);
        int detailsX = innerX + artPaneWidth;
        Box details = new Box(detailsX, innerY, Math.max(1, innerX + innerWidth - detailsX), innerHeight);

        int detailPadding = compact ? 8 : 12;
        int actionHeight = clamp(details.height() / 6, 22, 28);
        int actionWidth = Math.max(1, details.width() - detailPadding * 2);
        Box action = new Box(details.x() + detailPadding,
                Math.max(details.y(), details.bottom() - actionHeight - (compact ? 6 : 9)),
                actionWidth,
                Math.min(actionHeight, details.height()));

        int previewY = card.y() + 4;
        int previewHeight = Math.max(1, card.height() - 8);
        int previewOverlap = Math.min(10, Math.max(4, card.width() / 42));
        int previewStart = margin + navigationSize + navigationGap;
        int previewEnd = screenWidth - margin - navigationSize - navigationGap;
        Box previousPreview = new Box(previewStart, previewY,
                Math.max(1, card.x() - previewStart + previewOverlap), previewHeight);
        Box nextPreview = new Box(card.right() - previewOverlap, previewY,
                Math.max(1, previewEnd - card.right() + previewOverlap), previewHeight);
        Box previousVisible = new Box(previousPreview.x(), previewY,
                Math.max(0, card.x() - previousPreview.x()), previewHeight);
        Box nextVisible = new Box(card.right(), previewY,
                Math.max(0, nextPreview.right() - card.right()), previewHeight);
        int navigationY = card.y() + (card.height() - navigationSize) / 2;
        Box previousButton = new Box(margin, navigationY, navigationSize, navigationSize);
        Box nextButton = new Box(screenWidth - margin - navigationSize, navigationY, navigationSize, navigationSize);

        return new Layout(screenWidth, screenHeight, compact, margin,
                new Box(0, 0, screenWidth, headerHeight), card, art, details, action,
                previousPreview, nextPreview, previousVisible, nextVisible,
                previousButton, nextButton, List.copyOf(classTabs), heldPanel,
                List.copyOf(heldNames), heldInline);
    }

    public static float fitScale(float measuredWidth, float availableWidth) {
        if (measuredWidth <= 0 || availableWidth <= 0) return 0;
        return Math.min(1.0f, availableWidth / measuredWidth);
    }

    private static int clamp(int value, int minimum, int maximum) {
        if (maximum < minimum) return maximum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}

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
            boolean stacked,
            boolean compactPortrait,
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
            List<Box> heldNames) {
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

        /** Text columns used by the compact 4:3 card's bottom information band. */
        public Box compactMetaColumn() {
            int pad = 6;
            int availableWidth = Math.max(1, details.width() - pad * 2);
            int metaWidth = Math.min(availableWidth, clamp(Math.round(availableWidth * 0.38f), 76, 144));
            int y = details.y() + 4;
            int bottom = Math.max(y + 1, action.y() - 3);
            return new Box(details.x() + pad, y, metaWidth, bottom - y);
        }

        public Box compactCopyColumn() {
            int pad = 6;
            Box meta = compactMetaColumn();
            int x = meta.right() + 6;
            int y = details.y() + 4;
            int right = details.right() - pad;
            int bottom = Math.max(y + 1, action.y() - 3);
            return new Box(x, y, Math.max(1, right - x), bottom - y);
        }
    }

    private ClassSelectionLayout() {}

    public static Layout calculate(int screenWidth, int screenHeight) {
        if (screenWidth < 1 || screenHeight < 1) throw new IllegalArgumentException("Screen dimensions must be positive");

        float aspect = screenWidth / (float) screenHeight;
        boolean compact = screenWidth < 700 || screenHeight < 400;
        boolean stacked = !compact && aspect < 1.5f;
        boolean compactPortrait = compact && aspect < 1.5f;
        int margin = clamp(screenWidth / 40, 8, 20);
        int navigationSize = clamp(Math.min(screenWidth, screenHeight) / 14, 16, 28);
        int navigationGap = clamp(screenWidth / 120, 3, 8);
        int bottomMargin = clamp(screenHeight / 36, 5, 10);
        int tabHeight = compact ? 40 : 44;
        int heldWidth = compact ? 0 : clamp(screenWidth / 5, 136, 156);
        int heldGap = compact ? 0 : 8;
        int compactHotbarClearance = compact ? 16 : 0;
        int footerY = screenHeight - bottomMargin - tabHeight - compactHotbarClearance;

        int sideMinimum = compact ? 28 : 60;
        int sideReserve = margin + navigationSize + navigationGap + sideMinimum;
        int maxCardWidth = Math.max(1, screenWidth - sideReserve * 2);
        int desiredCardWidth = Math.round(screenWidth * (compact ? 0.72f : 0.74f));
        int cardWidth = Math.min(840, Math.min(maxCardWidth, desiredCardWidth));
        int cardX = (screenWidth - cardWidth) / 2;

        int headerHeight = 28;
        int topBound = headerHeight + 8;
        int bottomBound = Math.max(topBound + 1, footerY - 8);
        int availableCardHeight = Math.max(1, bottomBound - topBound);
        int frameInset = 4;
        int innerWidthForCard = Math.max(1, cardWidth - (frameInset + 1) * 2);
        int cardHeight;

        if (stacked) {
            int detailsHeight = 140;
            int maximumArtHeight = Math.max(1, availableCardHeight - detailsHeight - 12);
            int maximumInnerWidth = Math.max(1, (int) Math.floor(maximumArtHeight * 16.0 / 9.0));
            cardWidth = Math.min(cardWidth, maximumInnerWidth + (frameInset + 1) * 2);
            cardX = (screenWidth - cardWidth) / 2;
            innerWidthForCard = Math.max(1, cardWidth - (frameInset + 1) * 2);
            int artHeight = Math.max(1, Math.round(innerWidthForCard * 9.0f / 16.0f));
            cardHeight = artHeight + detailsHeight + 12;
        } else {
            float heightRatio = compact ? 0.64f : (aspect >= 1.7f ? 0.60f : 0.56f);
            cardHeight = Math.min(availableCardHeight, Math.max(1, Math.round(screenHeight * heightRatio)));
            frameInset = clamp(cardHeight / 58, 2, 4);
            innerWidthForCard = Math.max(1, cardWidth - (frameInset + 1) * 2);
        }

        int cardY = topBound + Math.max(0, (availableCardHeight - cardHeight) / 2);
        Box card = new Box(cardX, cardY, cardWidth, cardHeight);
        int contentInset = frameInset + 1;
        int innerX = card.x() + contentInset;
        int innerY = card.y() + contentInset;
        int innerWidth = Math.max(1, card.width() - contentInset * 2);
        int innerHeight = Math.max(1, card.height() - contentInset * 2);

        Box art;
        Box details;
        if (compact) {
            art = new Box(innerX, innerY, innerWidth, innerHeight);
            if (compactPortrait) {
                int detailsHeight = Math.min(innerHeight, clamp(screenHeight / 4 + 18, 90, 112));
                details = new Box(innerX, innerY + innerHeight - detailsHeight, innerWidth, detailsHeight);
            } else {
                int detailsWidth = Math.min(innerWidth, clamp(Math.round(innerWidth * 0.40f), 96, 184));
                details = new Box(innerX + innerWidth - detailsWidth, innerY, detailsWidth, innerHeight);
            }
        } else if (stacked) {
            int artHeight = Math.max(1, Math.round(innerWidth * 9.0f / 16.0f));
            art = new Box(innerX, innerY, innerWidth, artHeight);
            details = new Box(innerX, art.bottom() + 2, innerWidth,
                    Math.max(1, card.bottom() - contentInset - (art.bottom() + 2)));
        } else {
            int minimumDetailWidth = Math.min(260, Math.max(170, Math.round(innerWidth * 0.29f)));
            int artPaneWidth = Math.max(1, innerWidth - minimumDetailWidth);
            art = new Box(innerX, innerY, artPaneWidth, innerHeight);
            details = new Box(innerX + artPaneWidth, innerY, Math.max(1, innerWidth - artPaneWidth), innerHeight);
        }

        int detailPadding = compact ? 6 : 12;
        int actionHeight = clamp(details.height() / 6, 22, 28);
        int actionWidth;
        int actionX;
        int actionY;
        if (compactPortrait) {
            actionWidth = Math.min(Math.max(1, details.width() - detailPadding * 2),
                    Math.max(104, Math.round(details.width() * 0.52f)));
            actionX = details.x() + (details.width() - actionWidth) / 2;
            actionY = details.bottom() - 4 - actionHeight;
        } else {
            actionWidth = Math.max(1, details.width() - detailPadding * 2);
            actionX = details.x() + detailPadding;
            actionY = Math.max(details.y(), details.bottom() - actionHeight - (compact ? 6 : 9));
        }
        Box action = new Box(actionX, actionY, actionWidth, Math.min(actionHeight, details.height()));

        int previewHeight = Math.min(Math.max(1, card.height() - 8),
                clamp(card.height() / 3, compact ? 52 : 72, 112));
        int previewCenterY = art.y() + art.height() / 2;
        int previewY = clamp(previewCenterY - previewHeight / 2,
                card.y() + 4, Math.max(card.y() + 4, card.bottom() - 4 - previewHeight));
        int previewOverlap = Math.min(10, Math.max(4, card.width() / 64));
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
        int navigationY = clamp(previewCenterY - navigationSize / 2, 0, screenHeight - navigationSize);
        Box previousButton = new Box(margin, navigationY, navigationSize, navigationSize);
        Box nextButton = new Box(screenWidth - margin - navigationSize, navigationY, navigationSize, navigationSize);

        int footerWidth = Math.max(1, screenWidth - margin * 2);
        int tabsWidth;
        int heldX = screenWidth - margin - heldWidth;
        if (compact) {
            tabsWidth = Math.max(1, Math.min(footerWidth, Math.round(screenWidth * 0.92f)));
        } else {
            int maximumCenteredWidth = Math.max(1, 2 * (heldX - screenWidth / 2) - heldGap);
            tabsWidth = Math.max(1, Math.min(Math.round(card.width() * 0.90f),
                    Math.min(maximumCenteredWidth, footerWidth)));
        }
        int tabGap = Math.min(clamp(screenWidth / 120, 2, 8), Math.max(1, (tabsWidth - 6 * 20) / 5));
        int tabWidth = Math.max(1, (tabsWidth - tabGap * 5) / 6);
        int tabsUsedWidth = tabWidth * 6 + tabGap * 5;
        int tabsX = screenWidth / 2 - tabsUsedWidth / 2;
        List<Box> classTabs = new ArrayList<>(6);
        for (int index = 0; index < 6; index++) {
            classTabs.add(new Box(tabsX + index * (tabWidth + tabGap), footerY, tabWidth, tabHeight));
        }

        Box heldPanel = new Box(heldX, footerY, heldWidth, tabHeight);
        List<Box> heldNames = new ArrayList<>(2);
        if (!compact) {
            int nameAreaWidth = Math.max(0, heldWidth - 12);
            int nameGap = 4;
            int nameWidth = Math.max(0, (nameAreaWidth - nameGap) / 2);
            int nameY = footerY + 18;
            int nameHeight = Math.max(0, tabHeight - 24);
            heldNames.add(new Box(heldPanel.x() + 6, nameY, nameWidth, nameHeight));
            heldNames.add(new Box(heldPanel.x() + 6 + nameWidth + nameGap, nameY, nameWidth, nameHeight));
        }
        return new Layout(screenWidth, screenHeight, compact, stacked, compactPortrait, margin,
                new Box(0, 0, screenWidth, headerHeight), card, art, details, action,
                previousPreview, nextPreview, previousVisible, nextVisible,
                previousButton, nextButton, List.copyOf(classTabs), heldPanel,
                List.copyOf(heldNames));
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

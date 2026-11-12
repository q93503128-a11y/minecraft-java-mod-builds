package kr.moonseungjun.anomalousclasses.client;

/** Logical GUI-space layout shared by the class-management screen and its viewport verifier. */
record ClassManagementLayout(MenuUi.Box outer, MenuUi.Box artwork, MenuUi.Box overview,
                             MenuUi.Box profileButton, MenuUi.Box skillTreeButton, MenuUi.Box changeButton) {
    static ClassManagementLayout calculate(int screenWidth, int screenHeight) {
        int width = Math.max(1, screenWidth);
        int height = Math.max(1, screenHeight);
        int margin = width < 360 ? 5 : 10;
        int top = height < 250 ? 4 : 10;
        MenuUi.Box outer = new MenuUi.Box(margin, top, Math.max(1, width - margin * 2),
                Math.max(1, height - top * 2));

        int frameInset = Math.min(30, Math.max(12, Math.min(outer.width(), outer.height()) / 10));
        int contentX = outer.x() + frameInset;
        int contentRight = outer.right() - frameInset;
        int contentWidth = Math.max(1, contentRight - contentX);
        int contentGap = Math.min(12, Math.max(6, contentWidth / 60));

        int buttonHeight = Math.max(17, Math.min(24, height / 15));
        int buttonGap = Math.min(8, Math.max(4, contentWidth / 80));
        int switchWidth = Math.min(100, Math.max(58, contentWidth / 5));
        int primaryWidth = Math.max(1, Math.min(160,
                (contentWidth - switchWidth - buttonGap * 2) / 2));
        int rowWidth = primaryWidth * 2 + switchWidth + buttonGap * 2;
        int buttonX = outer.x() + (outer.width() - rowWidth) / 2;
        int buttonY = outer.bottom() - buttonHeight - Math.max(6, frameInset / 2);
        MenuUi.Box profileButton = new MenuUi.Box(buttonX, buttonY, primaryWidth, buttonHeight);
        MenuUi.Box skillTreeButton = new MenuUi.Box(profileButton.right() + buttonGap, buttonY,
                primaryWidth, buttonHeight);
        MenuUi.Box changeButton = new MenuUi.Box(skillTreeButton.right() + buttonGap, buttonY,
                switchWidth, buttonHeight);

        int bodyTop = outer.y() + Math.max(39, frameInset + 12);
        int bodyBottom = Math.max(bodyTop + 1, buttonY - 8);
        int bodyHeight = Math.max(1, bodyBottom - bodyTop);
        int availableForArtwork = Math.max(1, contentWidth - contentGap);
        int artworkWidth = Math.max(1, Math.round(availableForArtwork * 0.40f));
        int artworkHeight = Math.max(1, Math.min(bodyHeight, Math.round(artworkWidth * 9.0f / 16.0f)));
        int artworkY = bodyTop + (bodyHeight - artworkHeight) / 2;
        MenuUi.Box artwork = new MenuUi.Box(contentX, artworkY, artworkWidth, artworkHeight);
        MenuUi.Box overview = new MenuUi.Box(artwork.right() + contentGap, bodyTop,
                Math.max(1, contentRight - artwork.right() - contentGap), bodyHeight);

        return new ClassManagementLayout(outer, artwork, overview,
                profileButton, skillTreeButton, changeButton);
    }
}

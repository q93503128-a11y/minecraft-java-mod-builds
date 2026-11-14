package kr.moonseungjun.anomalousclasses.client;

/** Logical-pixel layout for the honest empty state used when a class has no management data contract. */
record ClassManagementLayout(MenuUi.Box outer, MenuUi.Box content, MenuUi.Box backButton,
                             MenuUi.Box changeButton) {
    static ClassManagementLayout calculate(int screenWidth, int screenHeight) {
        return calculate(screenWidth, screenHeight, false);
    }

    static ClassManagementLayout calculate(int screenWidth, int screenHeight, boolean returnToProfile) {
        int width = Math.max(1, screenWidth);
        int height = Math.max(1, screenHeight);
        int margin = width < 360 ? 5 : 10;
        int top = height < 250 ? 4 : 10;
        MenuUi.Box outer = new MenuUi.Box(margin, top, Math.max(1, width - margin * 2),
                Math.max(1, height - top * 2));
        int corner = Math.max(6, Math.min(40, Math.min(outer.width(), outer.height()) / 10));
        corner = Math.min(corner, Math.min(outer.width() / 2, outer.height() / 2));
        int buttonHeight = Math.max(16, Math.min(21, height / 20));
        int footerY = outer.bottom() - corner - buttonHeight - 4;
        int contentX = outer.x() + corner + 5;
        int contentRight = outer.right() - corner - 5;
        int contentTop = outer.y() + Math.max(40, corner + 5);
        MenuUi.Box content = new MenuUi.Box(contentX, contentTop,
                Math.max(1, contentRight - contentX), Math.max(1, footerY - contentTop - 8));

        int buttonWidth = Math.min(98, Math.max(72, outer.width() / 9));
        MenuUi.Box back = new MenuUi.Box(contentX, footerY, returnToProfile ? buttonWidth : 1, buttonHeight);
        MenuUi.Box change = new MenuUi.Box(contentRight - buttonWidth, footerY, buttonWidth, buttonHeight);
        return new ClassManagementLayout(outer, content, back, change);
    }
}

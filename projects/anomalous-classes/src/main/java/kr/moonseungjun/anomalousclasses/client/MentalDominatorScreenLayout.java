package kr.moonseungjun.anomalousclasses.client;

/** Logical-pixel layout for the Mental Dominator management screen. */
record MentalDominatorScreenLayout(MenuUi.Box outer, MenuUi.Box list, MenuUi.Box detail,
                                   MenuUi.Box profile, MenuUi.Box focus, MenuUi.Box changeClass,
                                   MenuUi.Box codex, MenuUi.Box allFollow, MenuUi.Box allStay,
                                   MenuUi.Box follow, MenuUi.Box stay, MenuUi.Box damage,
                                   MenuUi.Box release, int buttonHeight) {
    static MentalDominatorScreenLayout calculate(int width, int height) {
        int margin = width < 360 ? 5 : 10;
        int top = height < 260 ? 4 : 9;
        MenuUi.Box outer = new MenuUi.Box(margin, top, Math.max(1, width - margin * 2),
                Math.max(1, height - top * 2));
        int inset = Math.min(28, Math.max(11, Math.min(outer.width(), outer.height()) / 10));
        int left = outer.x() + inset;
        int right = outer.right() - inset;
        int bodyTop = outer.y() + 46;
        int footerY = outer.bottom() - 34;
        int bodyBottom = Math.max(bodyTop + 1, footerY - 7);
        int gap = Math.min(10, Math.max(5, outer.width() / 70));
        int bodyWidth = Math.max(1, right - left);
        int listWidth = Math.max(92, (bodyWidth - gap) * 38 / 100);
        listWidth = Math.min(listWidth, Math.max(1, bodyWidth - gap - 105));
        MenuUi.Box list = new MenuUi.Box(left, bodyTop, listWidth, Math.max(1, bodyBottom - bodyTop));
        MenuUi.Box detail = new MenuUi.Box(list.right() + gap, bodyTop,
                Math.max(1, right - list.right() - gap), Math.max(1, bodyBottom - bodyTop));

        int buttonHeight = Math.max(16, Math.min(22, height / 20));
        int topButtonY = outer.y() + 4;
        int navWidth = Math.max(42, Math.min(86, outer.width() / 7));
        MenuUi.Box profile = new MenuUi.Box(left, topButtonY, navWidth, buttonHeight);
        MenuUi.Box focus = new MenuUi.Box(profile.x() + navWidth + 3, topButtonY, navWidth, buttonHeight);
        MenuUi.Box changeClass = new MenuUi.Box(right - navWidth, topButtonY, navWidth, buttonHeight);
        int codexWidth = Math.max(48, Math.min(92, outer.width() / 6));
        MenuUi.Box codex = new MenuUi.Box(changeClass.x() - codexWidth - 4, topButtonY, codexWidth, buttonHeight);

        int footerWidth = Math.min(122, Math.max(74, (bodyWidth - gap) / 2));
        int footerX = left + (bodyWidth - footerWidth * 2 - gap) / 2;
        MenuUi.Box allFollow = new MenuUi.Box(footerX, footerY, footerWidth, buttonHeight);
        MenuUi.Box allStay = new MenuUi.Box(footerX + footerWidth + gap, footerY, footerWidth, buttonHeight);

        int detailInset = Math.max(6, detail.width() / 20);
        int actionGap = 4;
        int actionWidth = Math.max(1, (detail.width() - detailInset * 2 - actionGap) / 2);
        int actionY = detail.bottom() - buttonHeight * 2 - actionGap - 6;
        MenuUi.Box follow = new MenuUi.Box(detail.x() + detailInset, actionY, actionWidth, buttonHeight);
        MenuUi.Box stay = new MenuUi.Box(follow.right() + actionGap, actionY, actionWidth, buttonHeight);
        MenuUi.Box damage = new MenuUi.Box(detail.x() + detailInset, actionY + buttonHeight + actionGap,
                actionWidth, buttonHeight);
        MenuUi.Box release = new MenuUi.Box(damage.right() + actionGap, damage.y(), actionWidth, buttonHeight);
        return new MentalDominatorScreenLayout(outer, list, detail, profile, focus, changeClass, codex,
                allFollow, allStay, follow, stay, damage, release, buttonHeight);
    }
}

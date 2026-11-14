package kr.moonseungjun.anomalousclasses.client;

/** Static regression check for the class-management fallback at supported viewports. */
public final class ClassManagementLayoutVerifier {
    private static final int[][] PHYSICAL_VIEWPORTS = {
            {1920, 1080}, // 16:9
            {1920, 1200}, // 16:10
            {1600, 1200}  // 4:3
    };

    private ClassManagementLayoutVerifier() {}

    public static void main(String[] args) {
        int checked = 0;
        for (int[] viewport : PHYSICAL_VIEWPORTS) {
            for (int guiScale = 2; guiScale <= 4; guiScale++) {
                int width = viewport[0] / guiScale;
                int height = viewport[1] / guiScale;
                verify(width, height, false);
                verify(width, height, true);
                checked += 2;
            }
        }
        System.out.println("Class management layout: " + checked
                + " context/viewports passed (16:9, 16:10, 4:3 × GUI 2–4).");
    }

    private static void verify(int width, int height, boolean returnToProfile) {
        ClassManagementLayout layout = ClassManagementLayout.calculate(width, height, returnToProfile);
        MenuUi.Box outer = layout.outer();
        MenuUi.Box content = layout.content();
        MenuUi.Box back = layout.backButton();
        MenuUi.Box change = layout.changeButton();
        require(contains(outer, content), "content outside outer frame", width, height);
        require(contains(outer, change), "class-change button outside outer frame", width, height);
        require(!overlaps(content, change), "content overlaps footer action", width, height);
        require(!overlaps(back, change), "back overlaps class-change button", width, height);
        require(change.y() == back.y(), "footer buttons are misaligned", width, height);
        require(change.width() > 0 && change.height() > 0 && content.width() > 0 && content.height() > 0,
                "content or class-change action collapsed", width, height);
        require(returnToProfile ? back.width() > 1 : back.width() == 1,
                "back-to-profile visibility does not match navigation context", width, height);
        if (returnToProfile) require(contains(outer, back), "back button outside outer frame", width, height);
    }

    private static boolean contains(MenuUi.Box parent, MenuUi.Box child) {
        return child.x() >= parent.x() && child.y() >= parent.y()
                && child.right() <= parent.right() && child.bottom() <= parent.bottom();
    }

    private static boolean overlaps(MenuUi.Box left, MenuUi.Box right) {
        return left.x() < right.right() && left.right() > right.x()
                && left.y() < right.bottom() && left.bottom() > right.y();
    }

    private static void require(boolean condition, String message, int width, int height) {
        if (!condition) throw new IllegalStateException(message + " at logical viewport " + width + "x" + height);
    }
}

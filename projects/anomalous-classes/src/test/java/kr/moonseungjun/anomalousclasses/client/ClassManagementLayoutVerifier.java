package kr.moonseungjun.anomalousclasses.client;

/** Static regression check for the manager layout at the supported screen ratios and GUI scales. */
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
                verify(width, height);
                checked++;
            }
        }
        System.out.println("Class management layout: " + checked
                + " viewports passed (16:9, 16:10, 4:3 × GUI 2–4).");
    }

    private static void verify(int width, int height) {
        ClassManagementLayout layout = ClassManagementLayout.calculate(width, height);
        MenuUi.Box outer = layout.outer();
        require(contains(outer, layout.artwork()), "artwork outside outer frame", width, height);
        require(contains(outer, layout.overview()), "overview outside outer frame", width, height);
        require(contains(outer, layout.profileButton()), "profile button outside outer frame", width, height);
        require(contains(outer, layout.skillTreeButton()), "skill-tree button outside outer frame", width, height);
        require(contains(outer, layout.changeButton()), "class-change button outside outer frame", width, height);
        require(!overlaps(layout.artwork(), layout.overview()), "artwork overlaps overview", width, height);
        require(!overlaps(layout.profileButton(), layout.skillTreeButton()),
                "profile overlaps skill-tree button", width, height);
        require(!overlaps(layout.skillTreeButton(), layout.changeButton()),
                "skill-tree overlaps class-change button", width, height);
        require(layout.profileButton().y() == layout.skillTreeButton().y()
                        && layout.profileButton().y() == layout.changeButton().y(),
                "footer buttons are misaligned", width, height);
        require(layout.artwork().width() > 0 && layout.artwork().height() > 0
                        && layout.overview().width() > 0 && layout.overview().height() > 0,
                "content area collapsed", width, height);
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

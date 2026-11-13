package kr.moonseungjun.anomalousclasses.client;

/** Static regression checks for the real management widget geometry. */
public final class MentalDominatorScreenLayoutVerifier {
    private static final int[][] VIEWPORTS = {{1920, 1080}, {1920, 1200}, {1600, 1200}};

    private MentalDominatorScreenLayoutVerifier() {}

    public static void main(String[] args) {
        int checked = 0;
        for (int[] viewport : VIEWPORTS) {
            for (int scale = 2; scale <= 4; scale++) {
                int width = viewport[0] / scale;
                int height = viewport[1] / scale;
                verify(width, height);
                checked++;
            }
        }
        System.out.println("Mental Dominator layout: " + checked
                + " viewports passed (16:9, 16:10, 4:3 × GUI 2–4).");
    }

    private static void verify(int width, int height) {
        MentalDominatorScreenLayout layout = MentalDominatorScreenLayout.calculate(width, height);
        MenuUi.Box outer = layout.outer();
        MenuUi.Box[] inside = {layout.list(), layout.detail(), layout.profile(), layout.focus(),
                layout.changeClass(), layout.codex(), layout.allFollow(), layout.allStay()};
        for (MenuUi.Box box : inside) require(contains(outer, box), "element outside outer frame", width, height);
        require(!overlaps(layout.list(), layout.detail()), "list overlaps detail", width, height);
        require(!overlaps(layout.profile(), layout.focus()), "profile overlaps focus", width, height);
        require(!overlaps(layout.focus(), layout.codex()), "focus overlaps codex", width, height);
        require(!overlaps(layout.codex(), layout.changeClass()), "codex overlaps class change", width, height);
        require(!overlaps(layout.list(), layout.allFollow()) && !overlaps(layout.list(), layout.allStay()),
                "footer overlaps list", width, height);
        require(!overlaps(layout.detail(), layout.allFollow()) && !overlaps(layout.detail(), layout.allStay()),
                "footer overlaps details", width, height);
        MenuUi.Box[] actions = {layout.follow(), layout.stay(), layout.damage(), layout.release()};
        for (MenuUi.Box action : actions) require(contains(layout.detail(), action), "action outside detail", width, height);
        require(!overlaps(layout.follow(), layout.stay()) && !overlaps(layout.damage(), layout.release()),
                "paired action buttons overlap", width, height);
        require(!overlaps(layout.follow(), layout.damage()) && !overlaps(layout.stay(), layout.release()),
                "action rows overlap", width, height);
        require(layout.list().width() >= 92 && layout.detail().width() >= 105,
                "content columns too narrow", width, height);
    }

    private static boolean contains(MenuUi.Box parent, MenuUi.Box child) {
        return child.x() >= parent.x() && child.y() >= parent.y()
                && child.right() <= parent.right() && child.bottom() <= parent.bottom();
    }

    private static boolean overlaps(MenuUi.Box a, MenuUi.Box b) {
        return a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y();
    }

    private static void require(boolean condition, String message, int width, int height) {
        if (!condition) throw new IllegalStateException(message + " at logical viewport " + width + "×" + height);
    }
}

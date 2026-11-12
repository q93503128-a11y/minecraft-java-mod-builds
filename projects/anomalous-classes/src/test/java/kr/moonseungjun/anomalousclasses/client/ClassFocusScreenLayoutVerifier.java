package kr.moonseungjun.anomalousclasses.client;

/** Static viewport regression checks for the focus recipe viewer's hit regions and panels. */
public final class ClassFocusScreenLayoutVerifier {
    private static final int[][] PHYSICAL_VIEWPORTS = {
            {1920, 1080}, {1920, 1200}, {1600, 1200}, {1280, 720}
    };

    private ClassFocusScreenLayoutVerifier() {}

    public static void main(String[] args) {
        int checked = 0;
        for (int[] viewport : PHYSICAL_VIEWPORTS) {
            for (int guiScale = 2; guiScale <= 4; guiScale++) {
                verify(viewport[0] / guiScale, viewport[1] / guiScale);
                checked++;
            }
        }
        System.out.println("Class focus layout: " + checked
                + " viewports passed (16:9, 16:10, 4:3, compact 16:9 × GUI 2–4).");
    }

    private static void verify(int width, int height) {
        ClassFocusScreenLayout layout = ClassFocusScreenLayout.calculate(width, height);
        MenuUi.Box outer = layout.outer();
        require(contains(outer, layout.recipePanel()), "recipe panel outside frame", width, height);
        require(contains(outer, layout.infoPanel()), "info panel outside frame", width, height);
        require(contains(layout.recipePanel(), layout.resultSlot()), "result slot outside recipe panel", width, height);
        require(contains(layout.recipePanel(), layout.arrowMarker()), "arrow outside recipe panel", width, height);
        require(!overlaps(layout.arrowMarker(), layout.resultSlot()), "recipe arrow overlaps result", width, height);
        require(contains(outer, layout.backButton()), "back button outside frame", width, height);
        require(contains(outer, layout.equipButton()), "equip button outside frame", width, height);
        require(contains(outer, layout.unequipButton()), "unequip button outside frame", width, height);
        require(contains(layout.infoPanel(), layout.alternativeButton()), "alternative selector outside info panel", width, height);
        require(layout.infoPanel().height() >= 86, "focus information rows do not fit vertically", width, height);
        require(!overlaps(layout.recipePanel(), layout.infoPanel()), "recipe and info panels overlap", width, height);
        require(!overlaps(layout.backButton(), layout.equipButton())
                        && !overlaps(layout.backButton(), layout.unequipButton()),
                "footer actions overlap", width, height);
        require(layout.tierButtons().size() == 5, "expected five grade selectors", width, height);
        require(layout.recipeCells().size() == 9, "expected a three-by-three recipe grid", width, height);
        for (MenuUi.Box box : layout.tierButtons()) require(contains(outer, box), "tier selector outside frame", width, height);
        for (MenuUi.Box box : layout.recipeCells()) require(contains(layout.recipePanel(), box), "recipe cell outside panel", width, height);
        for (int left = 0; left < layout.recipeCells().size(); left++) {
            for (int right = left + 1; right < layout.recipeCells().size(); right++) {
                require(!overlaps(layout.recipeCells().get(left), layout.recipeCells().get(right)),
                        "recipe cells overlap", width, height);
            }
            require(!overlaps(layout.recipeCells().get(left), layout.resultSlot()),
                    "result slot overlaps an input cell", width, height);
            require(!overlaps(layout.recipeCells().get(left), layout.arrowMarker()),
                    "recipe arrow overlaps an input cell", width, height);
        }
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

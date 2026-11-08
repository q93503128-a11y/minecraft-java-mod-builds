import java.util.List;
import kr.moonseungjun.anomalousclasses.client.ClassSelectionLayout;
import kr.moonseungjun.anomalousclasses.client.ClassSelectionLayout.Box;
import kr.moonseungjun.anomalousclasses.client.ClassSelectionLayout.Layout;

/** Static geometry checks for supported aspect ratios and GUI scales. */
public final class VerifyClassSelectionLayout {
    private record Viewport(String aspect, int physicalWidth, int physicalHeight) {}

    private static final List<Viewport> VIEWPORTS = List.of(
            new Viewport("16:9", 1920, 1080),
            new Viewport("16:10", 1920, 1200),
            new Viewport("4:3", 1600, 1200));

    private VerifyClassSelectionLayout() {}

    public static void main(String[] args) {
        System.out.println("| Aspect | GUI scale | Logical size | Mode | Card | Art | Details | Action | Side previews | Tabs | Result |");
        System.out.println("|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|");
        for (Viewport viewport : VIEWPORTS) {
            for (int scale : new int[] {2, 3, 4}) {
                int width = viewport.physicalWidth() / scale;
                int height = viewport.physicalHeight() / scale;
                Layout layout = ClassSelectionLayout.calculate(width, height);
                verify(layout);
                System.out.printf("| %s | %d | %dx%d | %s | %s | %s | %s | %s | %d / %d px | %d | PASS |%n",
                        viewport.aspect(), scale, width, height, layout.compact() ? "compact" : "wide",
                        size(layout.card()), size(layout.art()), size(layout.details()), size(layout.action()),
                        layout.previousVisible().width(), layout.nextVisible().width(), layout.classTabs().get(0).width());
            }
        }
        System.out.println("All static geometry checks passed.");
    }

    private static void verify(Layout layout) {
        int width = layout.screenWidth();
        int height = layout.screenHeight();
        require(layout.card().inside(width, height), "card outside screen");
        require(layout.art().inside(width, height) && within(layout.art(), layout.card()), "art outside card");
        require(layout.details().inside(width, height) && within(layout.details(), layout.card()), "details outside card");
        require(layout.action().inside(width, height) && within(layout.action(), layout.details()), "action outside details");
        require(layout.header().inside(width, height) && !layout.header().intersects(layout.card()), "header/card overlap");
        require(layout.previousButton().inside(width, height) && layout.nextButton().inside(width, height), "navigation button outside screen");
        require(!layout.previousButton().intersects(layout.card()) && !layout.nextButton().intersects(layout.card()), "navigation overlaps card");
        require(layout.previousVisible().right() <= layout.card().x(), "left preview overlaps main card");
        require(layout.nextVisible().x() >= layout.card().right(), "right preview overlaps main card");
        require(layout.previousVisible().width() >= 20 && layout.nextVisible().width() >= 20, "side preview too narrow");
        require(layout.previousVisible().inside(width, height) && layout.nextVisible().inside(width, height), "side preview outside screen");

        for (int descriptionLines = 1; descriptionLines <= 4; descriptionLines++) {
            for (int statusLines = 0; statusLines <= 2; statusLines++) {
                Box action = layout.actionForContent(38, descriptionLines, statusLines, 9);
                require(action.inside(width, height) && within(action, layout.details()), "content-fitted action outside details");
                require(!action.intersects(layout.classTabs().get(0)), "content-fitted action overlaps footer");
                require(within(shift(action, -Math.min(18, layout.card().width() / 32)),
                        shift(layout.card(), -Math.min(18, layout.card().width() / 32))), "animated action hitbox leaves card");
            }
        }

        Box firstTab = layout.classTabs().get(0);
        for (int index = 0; index < layout.classTabs().size(); index++) {
            Box tab = layout.classTabs().get(index);
            require(tab.inside(width, height), "class tab outside screen");
            if (index > 0) require(!tab.intersects(layout.classTabs().get(index - 1)), "class tabs overlap");
        }
        require(!layout.card().intersects(firstTab), "card overlaps footer tabs");

        if (layout.compact()) {
            require(layout.heldInline().inside(width, height), "compact held-class line outside screen");
        } else {
            require(layout.heldPanel().inside(width, height), "held-class panel outside screen");
            require(!layout.classTabs().get(layout.classTabs().size() - 1).intersects(layout.heldPanel()), "held-class panel overlaps tabs");
            for (Box name : layout.heldNames()) require(name.inside(width, height) && within(name, layout.heldPanel()), "held class name outside panel");
        }

        int transitionDistance = Math.min(18, layout.card().width() / 32);
        Box leftShift = shift(layout.card(), -transitionDistance);
        Box rightShift = shift(layout.card(), transitionDistance);
        Box leftAction = shift(layout.action(), -transitionDistance);
        Box rightAction = shift(layout.action(), transitionDistance);
        require(leftShift.inside(width, height) && rightShift.inside(width, height), "transition moves card offscreen");
        require(within(leftAction, leftShift) && within(rightAction, rightShift), "transition separates action hitbox from card");
    }

    private static boolean within(Box inner, Box outer) {
        return inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    private static Box shift(Box box, int offset) {
        return new Box(box.x() + offset, box.y(), box.width(), box.height());
    }

    private static String size(Box box) {
        return box.width() + "x" + box.height();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

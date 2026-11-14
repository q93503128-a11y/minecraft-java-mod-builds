package kr.moonseungjun.anomalousclasses.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kr.moonseungjun.anomalousclasses.item.ClassFocusItems;
import kr.moonseungjun.anomalousclasses.network.payload.ClassFocusStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassManagementPayload;
import kr.moonseungjun.anomalousclasses.state.ClassFocus;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.FocusTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Read-only guide to the existing class-focus items and their shaped recipes. */
public final class ClassFocusScreen extends Screen {
    private static final int[] MAX_PSYCHE_PERCENT = {15, 25, 40, 60, 100};
    private static final int[] UPKEEP_REDUCTION_PERCENT = {4, 10, 17, 25, 40};
    private static final int[] INTRUSION_PROGRESS_PERCENT = {0, 4, 8, 13, 28};

    private ClassId classId;
    private final List<ClassFocusRecipeCatalog.Recipe> recipes;
    private ClassFocusRecipeCatalog.Recipe recipe;
    private ClassFocusScreenLayout layout;
    private final List<MenuUi.Button> tierButtons = new ArrayList<>();
    private MenuUi.Button backButton;
    private MenuUi.Button alternativeButton;
    private FocusTier selectedTier;
    private int alternative;
    private boolean pending;

    public ClassFocusScreen(ClassFocusStatePayload state) {
        super(Component.translatable("screen.anomalousclasses.class_focus.title"));
        this.classId = ClassId.fromNetworkIndex(state.profile().classIndex()).orElse(ClassId.MENTAL_DOMINATOR);
        this.recipes = ClassFocusRecipeCatalog.load(Minecraft.getInstance().getResourceManager());
        this.selectedTier = FocusTier.STANDARD;
        updateRecipe();
    }

    @Override
    protected void init() {
        layout = ClassFocusScreenLayout.calculate(width, height);
        tierButtons.clear();
        for (int index = 0; index < FocusTier.values().length; index++) {
            FocusTier tier = FocusTier.values()[index];
            MenuUi.Button button = addRenderableWidget(new MenuUi.Button(layout.tierButtons().get(index),
                    Component.literal(Integer.toString(index + 1)), ignored -> selectTier(tier)));
            tierButtons.add(button);
        }
        backButton = addRenderableWidget(new MenuUi.Button(layout.backButton(),
                Component.translatable("screen.anomalousclasses.class_focus.back"),
                ignored -> returnToManagement()));
        if (recipe != null && hasAlternative(recipe)) {
            alternativeButton = addRenderableWidget(new MenuUi.Button(layout.alternativeButton(),
                    alternativeLabel(), ignored -> {
                        alternative = (alternative + 1) % 2;
                    }));
        } else {
            alternativeButton = null;
        }
        refreshButtons();
    }

    private void selectTier(FocusTier tier) {
        selectedTier = tier;
        alternative = 0;
        updateRecipe();
        rebuildWidgets();
    }

    private void updateRecipe() {
        recipe = ClassFocusRecipeCatalog.find(recipes, classId, selectedTier).orElse(null);
    }

    public void applyServerState(ClassFocusStatePayload state) {
        ClassId updatedClass = ClassId.fromNetworkIndex(state.profile().classIndex())
                .orElse(ClassId.MENTAL_DOMINATOR);
        if (updatedClass == classId) return;
        classId = updatedClass;
        selectedTier = FocusTier.STANDARD;
        alternative = 0;
        updateRecipe();
        rebuildWidgets();
    }

    private void returnToManagement() {
        if (pending) return;
        pending = true;
        refreshButtons();
        PacketDistributor.sendToServer(RequestClassManagementPayload.INSTANCE);
    }

    private void refreshButtons() {
        if (backButton != null) backButton.active = !pending;
        for (MenuUi.Button button : tierButtons) button.active = !pending;
        if (alternativeButton != null) alternativeButton.active = !pending;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x70201D25);
        MenuUi.drawPanel(graphics, layout.outer());
        MenuUi.drawTitle(graphics, font, title, layout.outer(), width, height);
        drawRecipePanel(graphics);
        drawInfoPanel(graphics);
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
        for (int index = 0; index < tierButtons.size(); index++) {
            MenuUi.Box box = layout.tierButtons().get(index);
            if (FocusTier.values()[index] == selectedTier) {
                graphics.fill(box.x() + 4, box.bottom() - 2, box.right() - 4, box.bottom(), MenuUi.accent(classId));
            }
        }
        renderHoverTooltip(graphics, mouseX, mouseY);
    }

    private void drawRecipePanel(GuiGraphics graphics) {
        MenuUi.drawInsetSurface(graphics, layout.recipePanel());
        graphics.fill(layout.recipePanel().x() + 6, layout.recipePanel().y() + 3,
                layout.recipePanel().right() - 6, layout.recipePanel().y() + 5, MenuUi.accent(classId));
        int pad = Math.min(8, Math.max(4, layout.recipePanel().width() / 18));
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.class_focus.recipe"),
                layout.recipePanel().x() + pad, layout.recipePanel().y() + 5,
                layout.recipePanel().width() - pad * 2, 0xFFE6C788);
        if (recipe == null) return;

        for (int index = 0; index < layout.recipeCells().size(); index++) {
            MenuUi.Box cell = layout.recipeCells().get(index);
            int row = index / 3;
            int column = index % 3;
            graphics.fill(cell.x(), cell.y(), cell.right(), cell.bottom(), 0xA8271C31);
            graphics.fill(cell.x(), cell.y(), cell.right(), cell.y() + 1, 0xA8B18A63);
            graphics.fill(cell.x(), cell.y(), cell.x() + 1, cell.bottom(), 0xA8B18A63);
            ClassFocusRecipeCatalog.Ingredient ingredient = recipe.ingredientAt(row, column);
            if (ingredient != null) {
                ItemStack stack = ingredient.stack(alternativeFor(recipe, row, column));
                graphics.renderItem(stack, cell.x() + (cell.width() - 16) / 2, cell.y() + (cell.height() - 16) / 2);
            }
        }

        MenuUi.Box result = layout.resultSlot();
        graphics.fill(result.x(), result.y(), result.right(), result.bottom(), 0xC8271C31);
        graphics.fill(result.x(), result.y(), result.right(), result.y() + 1, 0xFFE6C788);
        graphics.fill(result.x(), result.y(), result.x() + 1, result.bottom(), 0xFFE6C788);
        MenuUi.Box arrow = layout.arrowMarker();
        graphics.drawCenteredString(font, Component.literal("›"), arrow.x() + arrow.width() / 2,
                arrow.y() + Math.max(0, (arrow.height() - font.lineHeight) / 2), 0xFFE6C788);
        graphics.renderItem(ClassFocusItems.stack(new ClassFocus(classId, selectedTier)),
                result.x() + (result.width() - 16) / 2, result.y() + (result.height() - 16) / 2);
    }

    private void drawInfoPanel(GuiGraphics graphics) {
        MenuUi.drawInsetSurface(graphics, layout.infoPanel());
        graphics.fill(layout.infoPanel().x() + 6, layout.infoPanel().y() + 3,
                layout.infoPanel().right() - 6, layout.infoPanel().y() + 5, MenuUi.accent(classId));
        int pad = Math.min(9, Math.max(5, layout.infoPanel().width() / 18));
        int x = layout.infoPanel().x() + pad;
        int width = Math.max(1, layout.infoPanel().width() - pad * 2);
        ItemStack selected = ClassFocusItems.stack(new ClassFocus(classId, selectedTier));
        boolean compact = layout.infoPanel().height() < 130;
        int iconSize = compact ? 16 : 20;
        int infoY = layout.infoPanel().y();
        graphics.renderItem(selected, x, infoY + (compact ? 7 : 8));
        int nameX = x + iconSize + 5;
        int nameWidth = Math.max(1, layout.infoPanel().right() - pad - nameX);
        MenuUi.drawFit(graphics, font, selected.getHoverName(), nameX, infoY + (compact ? 7 : 9),
                nameWidth, 0xFFF2E7D8);
        MenuUi.drawFit(graphics, font, tierName(selectedTier), x, infoY + (compact ? 23 : 29),
                width, MenuUi.accent(classId));
        int dividerY = infoY + (compact ? 34 : 42);
        graphics.fill(x, dividerY, layout.infoPanel().right() - pad, dividerY + 1, 0x806D4D7D);
        List<Component> effects = effectLines();
        int effectY = layout.effectArea().y();
        for (Component effect : effects) {
            MenuUi.drawFit(graphics, font, effect, layout.effectArea().x(), effectY,
                    layout.effectArea().width(), 0xFFD8D0C5);
            effectY += font.lineHeight;
        }
        if (alternativeButton != null) alternativeButton.setMessage(alternativeLabel());
    }

    private void renderHoverTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int index = 0; index < layout.tierButtons().size(); index++) {
            if (contains(layout.tierButtons().get(index), mouseX, mouseY)) {
                renderTooltip(graphics, tierName(FocusTier.values()[index]), mouseX, mouseY);
                return;
            }
        }
        if (recipe == null) return;
        for (int index = 0; index < layout.recipeCells().size(); index++) {
            MenuUi.Box cell = layout.recipeCells().get(index);
            if (!contains(cell, mouseX, mouseY)) continue;
            int row = index / 3;
            int column = index % 3;
            ClassFocusRecipeCatalog.Ingredient ingredient = recipe.ingredientAt(row, column);
            if (ingredient == null) return;
            char symbol = recipe.pattern().get(row).charAt(column);
            ItemStack stack = ingredient.stack(alternativeFor(recipe, row, column));
            int count = recipe.requiredSlots(symbol);
            graphics.renderTooltip(font, List.of(stack.getHoverName(),
                            Component.translatable("screen.anomalousclasses.class_focus.required_count", count)),
                    Optional.<TooltipComponent>empty(), mouseX, mouseY);
            return;
        }
        MenuUi.Box result = layout.resultSlot();
        if (contains(result, mouseX, mouseY)) {
            graphics.renderTooltip(font, ClassFocusItems.stack(new ClassFocus(classId, selectedTier)), mouseX, mouseY);
        }
    }

    private void renderTooltip(GuiGraphics graphics, Component text, int mouseX, int mouseY) {
        graphics.renderTooltip(font, List.of(text), Optional.<TooltipComponent>empty(), mouseX, mouseY);
    }

    private int alternativeFor(ClassFocusRecipeCatalog.Recipe recipe, int row, int column) {
        char symbol = recipe.pattern().get(row).charAt(column);
        ClassFocusRecipeCatalog.Ingredient ingredient = recipe.key().get(symbol);
        return ingredient == null || ingredient.items().size() < 2 ? 0 : alternative;
    }

    private static boolean hasAlternative(ClassFocusRecipeCatalog.Recipe recipe) {
        return recipe.key().values().stream().anyMatch(ingredient -> ingredient.items().size() > 1);
    }

    private Component alternativeLabel() {
        ClassFocusRecipeCatalog.Ingredient ingredient = recipe.key().values().stream()
                .filter(value -> value.items().size() > 1).findFirst().orElseThrow();
        ItemStack selected = ingredient.stack(alternative);
        return Component.translatable("screen.anomalousclasses.class_focus.alternative", selected.getHoverName());
    }

    private Component tierName(FocusTier tier) {
        return Component.translatable("screen.anomalousclasses.class_focus.tier." + tier.key());
    }

    private List<Component> effectLines() {
        if (classId != ClassId.MENTAL_DOMINATOR) return List.of();
        int tier = selectedTier.index();
        return List.of(
                Component.translatable("screen.anomalousclasses.class_focus.effect.max_psyche",
                        MAX_PSYCHE_PERCENT[tier]),
                Component.translatable("screen.anomalousclasses.class_focus.effect.upkeep",
                        UPKEEP_REDUCTION_PERCENT[tier]),
                Component.translatable("screen.anomalousclasses.class_focus.effect.intrusion",
                        INTRUSION_PROGRESS_PERCENT[tier]));
    }

    private static boolean contains(MenuUi.Box box, int x, int y) {
        return x >= box.x() && x < box.right() && y >= box.y() && y < box.bottom();
    }

    @Override public void onClose() { returnToManagement(); }

    @Override public boolean isPauseScreen() { return false; }
}

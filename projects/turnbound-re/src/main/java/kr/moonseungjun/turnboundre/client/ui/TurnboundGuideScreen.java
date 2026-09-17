package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Persistent player-facing guide, styled with the same adopted Kenney-backed production frame family. */
public final class TurnboundGuideScreen extends Screen {
    private record Page(String titleKey, String bodyKey, ItemStack icon) {}

    private static final List<Page> PAGES = List.of(
            new Page("screen.turnbound_re.guide.page.menu.title", "screen.turnbound_re.guide.page.menu.body", new ItemStack(Items.COMPASS)),
            new Page("screen.turnbound_re.guide.page.party.title", "screen.turnbound_re.guide.page.party.body", new ItemStack(Items.PLAYER_HEAD)),
            new Page("screen.turnbound_re.guide.page.expedition.title", "screen.turnbound_re.guide.page.expedition.body", new ItemStack(Items.FILLED_MAP)),
            new Page("screen.turnbound_re.guide.page.world.title", "screen.turnbound_re.guide.page.world.body", new ItemStack(Items.SPYGLASS)),
            new Page("screen.turnbound_re.guide.page.combat.title", "screen.turnbound_re.guide.page.combat.body", new ItemStack(Items.IRON_SWORD)),
            new Page("screen.turnbound_re.guide.page.growth.title", "screen.turnbound_re.guide.page.growth.body", new ItemStack(Items.EXPERIENCE_BOTTLE)),
            new Page("screen.turnbound_re.guide.page.equipment.title", "screen.turnbound_re.guide.page.equipment.body", new ItemStack(Items.SMITHING_TABLE))
    );

    private final Screen parent;
    private int page;

    public TurnboundGuideScreen(Screen parent) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.guide.title"));
        this.parent = parent;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the authored Minecraft world visible behind the guide.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiLayoutMetrics.Rect root = root();
        UiVisualLanguage.titleBand(graphics, this.font,
                root.x(), root.y(), root.width(), 22,
                Component.translatable("screen.turnbound_re.guide.title"), UiVisualLanguage.TEXT_FOCUS, true);

        Page current = PAGES.get(page);
        UiLayoutMetrics.Rect card = card();
        UiVisualLanguage.frame(graphics, card.x(), card.y(), card.width(), card.height(), UiVisualLanguage.FrameState.IDLE);
        if (!current.icon().isEmpty()) graphics.item(current.icon(), card.x() + UiLayoutMetrics.SPACE_8, card.y() + UiLayoutMetrics.SPACE_8);
        graphics.text(this.font, Component.translatable(current.titleKey()),
                card.x() + 32, card.y() + UiLayoutMetrics.SPACE_8 + 2, UiVisualLanguage.TEXT_FOCUS, true);

        int bodyX = card.x() + UiLayoutMetrics.SPACE_8;
        int bodyY = card.y() + 34;
        int bodyWidth = card.width() - UiLayoutMetrics.SPACE_16;
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(
                Component.translatable(current.bodyKey()), bodyWidth);
        int maxLines = Math.max(1, (card.bottom() - UiLayoutMetrics.SPACE_8 - bodyY) / (this.font.lineHeight + 2));
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.text(this.font, lines.get(i), bodyX, bodyY + i * (this.font.lineHeight + 2), UiVisualLanguage.TEXT_PRIMARY, true);
        }

        String counter = (page + 1) + " / " + PAGES.size();
        graphics.text(this.font, Component.literal(counter),
                root.right() - UiLayoutMetrics.SPACE_8 - this.font.width(counter), root.y() + 7,
                UiVisualLanguage.TEXT_SECONDARY, true);

        renderButton(graphics, previous(), Component.literal("<"), page > 0, mouseX, mouseY);
        renderButton(graphics, next(), Component.literal(">"), page + 1 < PAGES.size(), mouseX, mouseY);
        renderButton(graphics, back(), Component.translatable(parent == null ? "gui.done" : "gui.back"), true, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int x = (int) Math.floor(event.x());
            int y = (int) Math.floor(event.y());
            if (page > 0 && TurnboundMenuScreen.contains(previous(), x, y)) {
                page--;
                return true;
            }
            if (page + 1 < PAGES.size() && TurnboundMenuScreen.contains(next(), x, y)) {
                page++;
                return true;
            }
            if (TurnboundMenuScreen.contains(back(), x, y)) {
                this.minecraft.gui.setScreen(parent);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void renderButton(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect bounds, Component label,
                              boolean active, int mouseX, int mouseY) {
        UiVisualLanguage.FrameState state = !active ? UiVisualLanguage.FrameState.DISABLED
                : TurnboundMenuScreen.contains(bounds, mouseX, mouseY)
                ? UiVisualLanguage.FrameState.FOCUS : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), state);
        int x = bounds.x() + Math.max(UiLayoutMetrics.SPACE_4, (bounds.width() - this.font.width(label)) / 2);
        int y = bounds.y() + Math.max(UiLayoutMetrics.SPACE_2, (bounds.height() - this.font.lineHeight) / 2);
        graphics.text(this.font, label, x, y, UiVisualLanguage.textColor(state), true);
    }

    private UiLayoutMetrics.Rect root() {
        int width = Math.min(440, Math.max(280, this.width - UiLayoutMetrics.SPACE_16 * 2));
        int height = Math.min(244, Math.max(190, this.height - UiLayoutMetrics.SPACE_16 * 2));
        return new UiLayoutMetrics.Rect(Math.max(0, (this.width - width) / 2), Math.max(UiLayoutMetrics.SPACE_8, (this.height - height) / 2), width, height);
    }

    private UiLayoutMetrics.Rect card() {
        UiLayoutMetrics.Rect root = root();
        return new UiLayoutMetrics.Rect(root.x() + UiLayoutMetrics.SPACE_8, root.y() + 30,
                root.width() - UiLayoutMetrics.SPACE_16, root.height() - 66);
    }

    private UiLayoutMetrics.Rect previous() {
        UiLayoutMetrics.Rect root = root();
        return new UiLayoutMetrics.Rect(root.x() + UiLayoutMetrics.SPACE_8, root.bottom() - 28, 44, 20);
    }

    private UiLayoutMetrics.Rect next() {
        UiLayoutMetrics.Rect previous = previous();
        return new UiLayoutMetrics.Rect(previous.right() + UiLayoutMetrics.SPACE_4, previous.y(), 44, 20);
    }

    private UiLayoutMetrics.Rect back() {
        UiLayoutMetrics.Rect root = root();
        return new UiLayoutMetrics.Rect(root.right() - 92, root.bottom() - 28, 84, 20);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}

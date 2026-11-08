package kr.moonseungjun.anomalousclasses.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.network.payload.ClassSelectionStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassStatusPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SelectClassPayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClassSelectionScreen extends Screen {
    private record MainContent(int titleY, int stateY, int descriptionY, int statusY,
                               int descriptionHeight, int statusHeight,
                               List<FormattedCharSequence> titleLines,
                               List<FormattedCharSequence> stateLines,
                               List<FormattedCharSequence> descriptionLines,
                               List<FormattedCharSequence> statusLines,
                               ClassSelectionLayout.Box action) {}

    private static final int ART_WIDTH = 512;
    private static final int ART_HEIGHT = 288;
    private static final ResourceLocation[] CARD_ART = new ResourceLocation[ClassId.values().length];
    private static final ResourceLocation[] CLASS_ICONS = new ResourceLocation[ClassId.values().length];
    private static final int[] ACCENTS = {
            0xFFC69AE9, 0xFFD6A36D, 0xFF8CBF82, 0xFF70BFD5, 0xFFD0B47C, 0xFFB4C875
    };

    static {
        for (ClassId id : ClassId.values()) {
            CARD_ART[id.networkIndex()] = ResourceLocation.fromNamespaceAndPath(
                    AnomalousClasses.MOD_ID, "textures/gui/class_cards_v3/" + id.key() + ".png");
            CLASS_ICONS[id.networkIndex()] = ResourceLocation.fromNamespaceAndPath(
                    AnomalousClasses.MOD_ID, "textures/gui/class_icons_v2/" + id.key() + ".png");
        }
    }

    private final List<StyledButton> tabButtons = new ArrayList<>();
    private ClassSelectionStatePayload serverState;
    private ClassSelectionLayout.Layout layout;
    private int selectedIndex;
    private boolean pending;
    private int localStatus = -1;
    private int statusPollTicks;
    private long transitionStarted;
    private int transitionDirection;
    private StyledButton previousButton;
    private StyledButton nextButton;
    private StyledButton selectButton;

    public ClassSelectionScreen(ClassSelectionStatePayload serverState) {
        super(Component.translatable("screen.anomalousclasses.class_selection.title"));
        this.serverState = serverState;
        this.selectedIndex = serverState.currentClassIndex() >= 0 ? serverState.currentClassIndex() : 0;
    }

    @Override
    protected void init() {
        layout = ClassSelectionLayout.calculate(width, height);
        tabButtons.clear();

        ClassSelectionLayout.Box previous = layout.previousButton();
        ClassSelectionLayout.Box next = layout.nextButton();
        previousButton = addRenderableWidget(new StyledButton(previous, -1,
                Component.translatable("screen.anomalousclasses.class_selection.previous"), Style.PREVIOUS,
                button -> moveSelection(-1)));
        nextButton = addRenderableWidget(new StyledButton(next, -1,
                Component.translatable("screen.anomalousclasses.class_selection.next"), Style.NEXT,
                button -> moveSelection(1)));

        for (ClassId id : ClassId.values()) {
            int index = id.networkIndex();
            tabButtons.add(addRenderableWidget(new StyledButton(layout.classTabs().get(index), index,
                    Component.translatable("class.anomalousclasses." + id.key()), Style.TAB,
                    button -> setSelection(index))));
        }

        selectButton = addRenderableWidget(new StyledButton(layout.action(), -1,
                actionMessage(), Style.CONFIRM, button -> submitSelection()));
        refreshWidgetStates();
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null) return;
        if (++statusPollTicks >= 20) {
            statusPollTicks = 0;
            PacketDistributor.sendToServer(RequestClassStatusPayload.INSTANCE);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int actionOffset = transitionOffset();
        MainContent content = calculateMainContent();
        if (selectButton != null) {
            selectButton.setX(content.action().x() + actionOffset);
            selectButton.setY(content.action().y() + actionOffset);
        }

        graphics.fill(0, 0, width, height, 0x70201D25);
        renderHeader(graphics);
        renderSideCards(graphics);
        renderMainCard(graphics, actionOffset, content);
        renderSideLabels(graphics);
        renderFooter(graphics);
        // Screen.render() also invokes renderBackground(), which would blur the custom UI just drawn above.
        // Keep widgets registered for input and narration, but render that list after the screen content here.
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    public void applyServerState(ClassSelectionStatePayload state) {
        ClassSelectionStatePayload.Result priorResult = serverState.result();
        this.serverState = state;
        this.pending = false;
        this.localStatus = -1;
        if (state.result() == ClassSelectionStatePayload.Result.SAVED) {
            selectedIndex = state.currentClassIndex();
            transitionStarted = System.currentTimeMillis();
        } else if (state.result() == ClassSelectionStatePayload.Result.COMBAT_LOCKED
                || state.result() == ClassSelectionStatePayload.Result.STALE_REQUEST
                || state.result() == ClassSelectionStatePayload.Result.INVALID_CLASS) {
            if (state.currentClassIndex() >= 0 && selectedIndex == state.currentClassIndex()
                    && priorResult == ClassSelectionStatePayload.Result.SAVED) {
                selectedIndex = state.currentClassIndex();
            }
        }
        refreshWidgetStates();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            moveSelection(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveSelection(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && serverState.currentClassIndex() < 0) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) moveSelection(-1);
        else if (scrollY < 0) moveSelection(1);
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return serverState.currentClassIndex() >= 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderHeader(GuiGraphics graphics) {
        int centerX = width / 2;
        int titleY = Math.max(2, (layout.header().height() - font.lineHeight * 2) / 2 - 1);
        drawFittedCentered(graphics, title, centerX, titleY, width - layout.margin() * 2, 0xFFF1E8D8);
        Component subtitle = serverState.currentClassIndex() < 0
                ? Component.translatable("screen.anomalousclasses.class_selection.first_visit")
                : Component.translatable("screen.anomalousclasses.class_selection.change_hint");
        drawFittedCentered(graphics, subtitle, centerX, titleY + font.lineHeight + 2,
                width - layout.margin() * 2, 0xFFD0C8BC);
    }

    private void renderSideCards(GuiGraphics graphics) {
        int previousIndex = Math.floorMod(selectedIndex - 1, ClassId.values().length);
        int nextIndex = Math.floorMod(selectedIndex + 1, ClassId.values().length);
        drawSideCard(graphics, layout.previousPreview(), layout.previousVisible(), previousIndex);
        drawSideCard(graphics, layout.nextPreview(), layout.nextVisible(), nextIndex);
    }

    private void drawSideCard(GuiGraphics graphics, ClassSelectionLayout.Box preview,
                              ClassSelectionLayout.Box visible, int classIndex) {
        if (preview.width() <= 0 || preview.height() <= 0) return;
        graphics.fill(preview.x(), preview.y(), preview.right(), preview.bottom(), 0xFF19171E);
        drawThinFrame(graphics, preview, 0xA59B8D7C, 0x50131118);
        if (visible.width() <= 0 || visible.height() <= 0) return;
        int thumbnailWidth = Math.max(1, visible.width() - 6);
        int thumbnailHeight = Math.min(visible.height() - 24, Math.max(1, Math.round(thumbnailWidth * 9.0f / 16.0f)));
        thumbnailWidth = Math.min(thumbnailWidth, Math.max(1, Math.round(thumbnailHeight * 16.0f / 9.0f)));
        ClassSelectionLayout.Box thumbnail = new ClassSelectionLayout.Box(
                visible.x() + (visible.width() - thumbnailWidth) / 2,
                visible.y() + Math.max(2, (visible.height() - thumbnailHeight) / 2),
                thumbnailWidth, thumbnailHeight);
        graphics.fill(thumbnail.x() - 1, thumbnail.y() - 1, thumbnail.right() + 1, thumbnail.bottom() + 1, 0xFF5C5260);
        drawArtCover(graphics, CARD_ART[classIndex], thumbnail, 0.94f, 0.5f);
    }

    private void renderSideLabels(GuiGraphics graphics) {
        drawSideLabel(graphics, layout.previousVisible(), Math.floorMod(selectedIndex - 1, ClassId.values().length));
        drawSideLabel(graphics, layout.nextVisible(), Math.floorMod(selectedIndex + 1, ClassId.values().length));
    }

    private void drawSideLabel(GuiGraphics graphics, ClassSelectionLayout.Box visible, int classIndex) {
        if (visible.width() < 16 || visible.height() < font.lineHeight + 2) return;
        Component name = Component.translatable("class.anomalousclasses." + ClassId.values()[classIndex].key());
        List<FormattedCharSequence> lines = sideNameLines(name, Math.max(1, visible.width() - 6));
        if (lines.isEmpty()) return;
        int lineCount = Math.min(lines.size(), Math.max(1, (visible.height() - 4) / font.lineHeight));
        int bandHeight = Math.min(visible.height(), lineCount * font.lineHeight + 4);
        int bandY = visible.bottom() - bandHeight;
        graphics.enableScissor(visible.x(), visible.y(), visible.right(), visible.bottom());
        graphics.fill(visible.x(), bandY, visible.right(), visible.bottom(), 0xD5141219);
        for (int index = 0; index < lineCount; index++) {
            drawFittedCentered(graphics, lines.get(index), visible.x() + visible.width() / 2,
                    bandY + 2 + index * font.lineHeight, Math.max(1, visible.width() - 4), 0xFFE7DFD2);
        }
        graphics.disableScissor();
    }

    private List<FormattedCharSequence> sideNameLines(Component name, int maxWidth) {
        String[] words = name.getString().trim().split("\\s+");
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String word : words) {
            if (font.width(Component.literal(word)) <= maxWidth) {
                lines.add(Component.literal(word).getVisualOrderText());
                continue;
            }
            StringBuilder part = new StringBuilder();
            for (int offset = 0; offset < word.length();) {
                int codePoint = word.codePointAt(offset);
                String next = new String(Character.toChars(codePoint));
                if (!part.isEmpty() && font.width(Component.literal(part + next)) > maxWidth) {
                    lines.add(Component.literal(part.toString()).getVisualOrderText());
                    part.setLength(0);
                }
                part.append(next);
                offset += Character.charCount(codePoint);
            }
            if (!part.isEmpty()) lines.add(Component.literal(part.toString()).getVisualOrderText());
        }
        return lines;
    }

    private void renderMainCard(GuiGraphics graphics, int offset, MainContent content) {
        ClassId id = ClassId.values()[selectedIndex];
        int accent = ACCENTS[selectedIndex];
        ClassSelectionLayout.Box panel = shifted(layout.card(), offset);
        ClassSelectionLayout.Box art = shifted(layout.art(), offset);
        ClassSelectionLayout.Box details = shifted(layout.details(), offset);
        int inset = Math.max(2, Math.min(4, panel.height() / 58));

        drawMainFrame(graphics, panel, accent);
        graphics.fill(panel.x() + inset + 1, panel.y() + inset + 1,
                panel.right() - inset - 1, panel.bottom() - inset - 1, 0xFF17151B);
        graphics.fill(art.x() - 1, art.y() - 1, art.right() + 1, art.bottom() + 1, 0xFF332D38);
        drawArtCover(graphics, CARD_ART[selectedIndex], art, 1.0f, selectedIndex == 2 ? 0.36f : 0.43f);

        if (layout.compact()) {
            graphics.fill(details.x(), details.y(), details.right(), details.bottom(), 0xE91B1821);
            if (layout.compactPortrait()) {
                graphics.fill(details.x(), details.y(), details.right(), details.y() + 1, accent);
            } else {
                graphics.fill(details.x(), details.y(), details.x() + 1, details.bottom(), accent);
            }
        } else if (layout.stacked()) {
            graphics.fill(details.x(), details.y(), details.right(), details.bottom(), 0xFF211E26);
            graphics.fill(details.x(), details.y(), details.right(), details.y() + 1, accent);
        } else {
            graphics.fill(details.x(), details.y(), details.right(), details.bottom(), 0xFF211E26);
            graphics.fill(details.x(), details.y(), details.x() + 1, details.bottom(), accent);
        }

        int pad = layout.compact() ? 6 : 12;
        int textX = details.x() + pad;
        int textWidth = Math.max(1, details.width() - pad * 2);
        int iconSize = Math.min(21, Math.max(15, font.lineHeight + 8));
        int titleY = content.titleY() + offset;
        int current = serverState.currentClassIndex();
        Component name = Component.translatable("class.anomalousclasses." + id.key());
        if (layout.compactPortrait()) {
            ClassSelectionLayout.Box meta = shifted(layout.compactMetaColumn(), offset);
            ClassSelectionLayout.Box copy = shifted(layout.compactCopyColumn(), offset);
            graphics.blit(CLASS_ICONS[selectedIndex], meta.x(), titleY, iconSize, iconSize,
                    0, 0, 96, 96, 96, 96);
            int titleX = meta.x() + iconSize + 4;
            drawFittedLines(graphics, content.titleLines(), titleX, titleY,
                    Math.max(1, meta.right() - titleX), content.titleLines().size() * font.lineHeight, 0xFFF3E9D9);
            drawFittedLines(graphics, content.stateLines(), meta.x(), content.stateY() + offset,
                    meta.width(), content.stateLines().size() * font.lineHeight, 0xFFBEB5AA);
            drawFittedLines(graphics, content.descriptionLines(), copy.x(), content.descriptionY() + offset,
                    copy.width(), content.descriptionHeight(), 0xFFE1DAD0);
            if (!content.statusLines().isEmpty()) {
                drawFittedLines(graphics, content.statusLines(), copy.x(), content.statusY() + offset,
                        copy.width(), content.statusHeight(),
                        serverState.combatLocked() ? 0xFFFFC0AA : localStatusColor());
            }
        } else {
            int titleTextX = textX + iconSize + 5;
            int titleTextWidth = Math.max(1, textWidth - iconSize - 5);
            graphics.blit(CLASS_ICONS[selectedIndex], textX, titleY - 1, iconSize, iconSize,
                    0, 0, 96, 96, 96, 96);
            drawFittedString(graphics, name, titleTextX,
                    titleY + Math.max(0, (iconSize - font.lineHeight) / 2), titleTextWidth, 0xFFF3E9D9);
            drawFittedString(graphics, classStateText(current), textX, content.stateY() + offset, textWidth,
                    current == selectedIndex ? accent : 0xFFBEB5AA);
            drawFittedLines(graphics, content.descriptionLines(), textX, content.descriptionY() + offset,
                    textWidth, content.descriptionHeight(), 0xFFE1DAD0);

            if (!content.statusLines().isEmpty()) {
                drawFittedLines(graphics, content.statusLines(), textX, content.statusY() + offset,
                        textWidth, content.statusHeight(),
                        serverState.combatLocked() ? 0xFFFFC0AA : localStatusColor());
            }
        }
        if (current == selectedIndex) {
            graphics.fill(panel.x() + 7, panel.y() + 7, panel.x() + 10, panel.y() + 10, accent);
        }
    }

    private Component classStateText(int current) {
        if (current == selectedIndex) {
            return Component.translatable("screen.anomalousclasses.class_selection.current");
        }
        if (hasPlayed(selectedIndex)) {
            return Component.translatable("screen.anomalousclasses.class_selection.progress",
                    level(selectedIndex), skillPoints(selectedIndex));
        }
        return Component.translatable("screen.anomalousclasses.class_selection.new_class");
    }

    private MainContent calculateMainContent() {
        ClassId id = ClassId.values()[selectedIndex];
        ClassSelectionLayout.Box details = layout.details();
        int iconSize = Math.min(21, Math.max(15, font.lineHeight + 8));
        if (layout.compactPortrait()) {
            ClassSelectionLayout.Box meta = layout.compactMetaColumn();
            ClassSelectionLayout.Box copy = layout.compactCopyColumn();
            Component name = Component.translatable("class.anomalousclasses." + id.key());
            Component state = classStateText(serverState.currentClassIndex());
            List<FormattedCharSequence> titleLines = font.split(name,
                    Math.max(1, meta.width() - iconSize - 4));
            List<FormattedCharSequence> stateLines = font.split(state, Math.max(1, meta.width()));
            List<FormattedCharSequence> descriptionLines = font.split(
                    Component.translatable("class.anomalousclasses." + id.key() + ".description"), copy.width());
            List<FormattedCharSequence> statusLines = currentStatusLines(copy.width());
            int titleBlockHeight = Math.max(iconSize, titleLines.size() * font.lineHeight);
            int titleY = meta.y();
            int stateY = titleY + titleBlockHeight + 3;
            int descriptionY = copy.y();
            int descriptionHeight = descriptionLines.size() * font.lineHeight;
            int statusY = descriptionY + descriptionHeight + (statusLines.isEmpty() ? 0 : 3);
            int statusHeight = statusLines.isEmpty() ? 0 : statusLines.size() * font.lineHeight;
            return new MainContent(titleY, stateY, descriptionY, statusY,
                    descriptionHeight, statusHeight, titleLines, stateLines,
                    descriptionLines, statusLines, layout.action());
        }

        int pad = layout.compact() ? 6 : 12;
        int textWidth = Math.max(1, details.width() - pad * 2);
        List<FormattedCharSequence> descriptionLines = font.split(
                Component.translatable("class.anomalousclasses." + id.key() + ".description"), textWidth);
        List<FormattedCharSequence> statusLines = currentStatusLines(textWidth);
        int headerHeight = iconSize + 4 + font.lineHeight + 4;
        ClassSelectionLayout.Box action = layout.actionForContent(headerHeight,
                descriptionLines.size(), statusLines.size(), font.lineHeight);
        int descriptionHeight = descriptionLines.size() * font.lineHeight;
        int statusHeight = statusLines.isEmpty() ? 0 : statusLines.size() * font.lineHeight;
        int blockTop = action.y() - 10 - (statusLines.isEmpty() ? 0 : statusHeight + 3)
                - descriptionHeight - headerHeight;
        int stateY = blockTop + iconSize + 4;
        int descriptionY = stateY + font.lineHeight + 4;
        int statusY = descriptionY + descriptionHeight + (statusLines.isEmpty() ? 0 : 3);
        return new MainContent(blockTop, stateY, descriptionY, statusY,
                descriptionHeight, statusHeight, List.of(), List.of(), descriptionLines, statusLines, action);
    }

    private List<FormattedCharSequence> currentStatusLines(int textWidth) {
        if (serverState.combatLocked()) {
            return font.split(Component.translatable("screen.anomalousclasses.class_selection.combat_lock"), textWidth);
        }
        if (localStatus >= 0) {
            return font.split(localStatusComponent(), textWidth);
        }
        return List.of();
    }

    private void renderFooter(GuiGraphics graphics) {
        for (int index = 0; index < tabButtons.size(); index++) {
            StyledButton tab = tabButtons.get(index);
            tab.setSelected(index == selectedIndex);
            tab.setCurrent(index == serverState.currentClassIndex());
        }

        if (layout.compact()) {
            drawFittedCentered(graphics, Component.translatable("screen.anomalousclasses.class_selection.on_hold_compact"),
                    layout.heldInline().x() + layout.heldInline().width() / 2, layout.heldInline().y(),
                    layout.heldInline().width(), 0xFFAAA2A0);
            return;
        }

        ClassSelectionLayout.Box held = layout.heldPanel();
        graphics.fill(held.x() + 3, held.y(), held.right() - 3, held.bottom(), 0xC019171D);
        graphics.fill(held.x(), held.y() + 3, held.right(), held.bottom() - 3, 0xC019171D);
        drawThinFrame(graphics, held, 0xFF68636A, 0x99141218);
        drawFittedString(graphics, Component.translatable("screen.anomalousclasses.class_selection.on_hold"),
                held.x() + 7, held.y() + 5, held.width() - 14, 0xFFBEB6AA);
        for (int index = 0; index < layout.heldNames().size(); index++) {
            ClassSelectionLayout.Box nameBox = layout.heldNames().get(index);
            Component name = Component.translatable(index == 0
                    ? "class.anomalousclasses.gravity_caster"
                    : "class.anomalousclasses.resonance_caster");
            drawWrappedCentered(graphics, name, nameBox, 0xFF969096);
        }
    }

    private void drawArtCover(GuiGraphics graphics, ResourceLocation texture, ClassSelectionLayout.Box target,
                              float brightness, float horizontalFocus) {
        if (target.width() <= 0 || target.height() <= 0) return;
        float scale = Math.max(target.width() / (float) ART_WIDTH, target.height() / (float) ART_HEIGHT);
        int drawWidth = Math.round(ART_WIDTH * scale);
        int drawHeight = Math.round(ART_HEIGHT * scale);
        int overflowX = Math.max(0, drawWidth - target.width());
        int drawX = target.x() - Math.round(overflowX * Math.max(0.0f, Math.min(1.0f, horizontalFocus)));
        int drawY = target.y() + (target.height() - drawHeight) / 2;
        graphics.enableScissor(target.x(), target.y(), target.right(), target.bottom());
        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(texture, 0, 0, 0, 0, ART_WIDTH, ART_HEIGHT, ART_WIDTH, ART_HEIGHT);
        graphics.pose().popPose();
        graphics.disableScissor();
        if (brightness < 1.0f) {
            int alpha = Math.max(0, Math.min(255, Math.round((1.0f - brightness) * 120)));
            graphics.fill(target.x(), target.y(), target.right(), target.bottom(), alpha << 24 | 0x0008090D);
        }
    }

    private void drawMainFrame(GuiGraphics graphics, ClassSelectionLayout.Box box, int accent) {
        graphics.fill(box.x() + 4, box.y(), box.right() - 4, box.bottom(), 0xFF111015);
        graphics.fill(box.x(), box.y() + 4, box.right(), box.bottom() - 4, 0xFF111015);
        graphics.fill(box.x() + 2, box.y() + 2, box.right() - 2, box.bottom() - 2, 0xFFD2C3AC);
        graphics.fill(box.x() + 3, box.y() + 3, box.right() - 3, box.bottom() - 3, 0xFF27242A);
        graphics.fill(box.x() + 5, box.y() + 5, box.right() - 5, box.bottom() - 5, 0xFF16141A);
        graphics.fill(box.x() + 8, box.y() + 2, box.right() - 8, box.y() + 3, accent);
        graphics.fill(box.x() + 8, box.bottom() - 3, box.right() - 8, box.bottom() - 2, 0xFFAA987E);
    }

    private void drawThinFrame(GuiGraphics graphics, ClassSelectionLayout.Box box, int edge, int fill) {
        graphics.fill(box.x() + 3, box.y(), box.right() - 3, box.bottom(), fill);
        graphics.fill(box.x(), box.y() + 3, box.right(), box.bottom() - 3, fill);
        graphics.fill(box.x() + 3, box.y(), box.right() - 3, box.y() + 1, edge);
        graphics.fill(box.x() + 3, box.bottom() - 1, box.right() - 3, box.bottom(), edge);
        graphics.fill(box.x(), box.y() + 3, box.x() + 1, box.bottom() - 3, edge);
        graphics.fill(box.right() - 1, box.y() + 3, box.right(), box.bottom() - 3, edge);
    }

    private void drawChevron(GuiGraphics graphics, int centerX, int centerY, int direction, int color, int size) {
        for (int row = 0; row < size; row++) {
            int x = centerX + direction * (row - size / 2);
            int y = centerY + row - size / 2;
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private void drawFittedString(GuiGraphics graphics, Component text, int x, int y, int availableWidth, int color) {
        drawFittedString(graphics, text.getVisualOrderText(), x, y, availableWidth, color);
    }

    private void drawFittedString(GuiGraphics graphics, FormattedCharSequence text, int x, int y,
                                  int availableWidth, int color) {
        float scale = ClassSelectionLayout.fitScale(font.width(text), availableWidth);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawFittedCentered(GuiGraphics graphics, Component text, int centerX, int y,
                                    int availableWidth, int color) {
        drawFittedCentered(graphics, text.getVisualOrderText(), centerX, y, availableWidth, color);
    }

    private void drawFittedCentered(GuiGraphics graphics, FormattedCharSequence text, int centerX, int y,
                                    int availableWidth, int color) {
        float scale = ClassSelectionLayout.fitScale(font.width(text), availableWidth);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - font.width(text) * scale / 2.0f, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawFittedLines(GuiGraphics graphics, List<FormattedCharSequence> lines, int x, int y,
                                 int availableWidth, int availableHeight, int color) {
        if (lines.isEmpty() || availableWidth <= 0 || availableHeight <= 0) return;
        float widest = 1.0f;
        for (FormattedCharSequence line : lines) widest = Math.max(widest, font.width(line));
        float widthScale = ClassSelectionLayout.fitScale(widest, availableWidth);
        float heightScale = Math.min(1.0f, availableHeight / (float) (lines.size() * font.lineHeight));
        float scale = Math.min(widthScale, heightScale);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), 0, index * font.lineHeight, color, false);
        }
        graphics.pose().popPose();
    }

    private void drawWrappedCentered(GuiGraphics graphics, Component text, ClassSelectionLayout.Box box, int color) {
        if (box.width() <= 0 || box.height() <= 0) return;
        List<FormattedCharSequence> lines = font.split(text, box.width());
        int maxLines = Math.max(1, box.height() / font.lineHeight);
        int lineCount = Math.min(lines.size(), maxLines);
        int y = box.y() + Math.max(0, (box.height() - lineCount * font.lineHeight) / 2);
        for (int index = 0; index < lineCount; index++) {
            drawFittedCentered(graphics, lines.get(index), box.x() + box.width() / 2,
                    y + index * font.lineHeight, box.width(), color);
        }
    }

    private ClassSelectionLayout.Box shifted(ClassSelectionLayout.Box box, int offset) {
        return new ClassSelectionLayout.Box(box.x() + offset, box.y(), box.width(), box.height());
    }

    private int transitionOffset() {
        if (transitionDirection == 0) return 0;
        double elapsed = Math.min(1.0, (System.currentTimeMillis() - transitionStarted) / 190.0);
        if (elapsed >= 1.0) {
            transitionDirection = 0;
            return 0;
        }
        double eased = elapsed * elapsed * (3.0 - 2.0 * elapsed);
        int distance = Math.min(18, layout.card().width() / 32);
        return (int) Math.round((1.0 - eased) * transitionDirection * distance);
    }

    private void moveSelection(int direction) {
        if (pending) return;
        transitionDirection = direction;
        transitionStarted = System.currentTimeMillis();
        selectedIndex = Math.floorMod(selectedIndex + direction, ClassId.values().length);
        localStatus = -1;
        refreshWidgetStates();
    }

    private void setSelection(int index) {
        if (pending || index < 0 || index >= ClassId.values().length || index == selectedIndex) return;
        int delta = index - selectedIndex;
        int count = ClassId.values().length;
        if (delta > count / 2) delta -= count;
        if (delta < -count / 2) delta += count;
        transitionDirection = Integer.signum(delta);
        transitionStarted = System.currentTimeMillis();
        selectedIndex = index;
        localStatus = -1;
        refreshWidgetStates();
    }

    private void submitSelection() {
        if (pending || serverState.combatLocked() || selectedIndex == serverState.currentClassIndex()) return;
        pending = true;
        localStatus = 0;
        refreshWidgetStates();
        PacketDistributor.sendToServer(new SelectClassPayload(selectedIndex, serverState.revision()));
    }

    private void refreshWidgetStates() {
        if (previousButton != null) previousButton.active = !pending;
        if (nextButton != null) nextButton.active = !pending;
        for (StyledButton button : tabButtons) button.active = !pending;
        if (selectButton != null) {
            selectButton.active = !pending && !serverState.combatLocked()
                    && selectedIndex != serverState.currentClassIndex();
            selectButton.setMessage(actionMessage());
        }
    }

    private Component actionMessage() {
        if (pending) return Component.translatable("screen.anomalousclasses.class_selection.saving");
        if (selectedIndex == serverState.currentClassIndex()) return Component.translatable("screen.anomalousclasses.class_selection.selected");
        return serverState.currentClassIndex() < 0
                ? Component.translatable("screen.anomalousclasses.class_selection.choose")
                : Component.translatable("screen.anomalousclasses.class_selection.change");
    }

    private Component localStatusComponent() {
        if (localStatus == 0) return Component.translatable("screen.anomalousclasses.class_selection.saving");
        return switch (serverState.result()) {
            case SAVED -> Component.translatable("screen.anomalousclasses.class_selection.saved");
            case ALREADY_CURRENT -> Component.translatable("screen.anomalousclasses.class_selection.already_current");
            case COMBAT_LOCKED -> Component.translatable("screen.anomalousclasses.class_selection.combat_lock");
            case STALE_REQUEST -> Component.translatable("screen.anomalousclasses.class_selection.stale");
            case INVALID_CLASS -> Component.translatable("screen.anomalousclasses.class_selection.invalid");
            default -> Component.translatable("screen.anomalousclasses.class_selection.status");
        };
    }

    private int localStatusColor() {
        return switch (serverState.result()) {
            case SAVED -> 0xFFB8D99A;
            case COMBAT_LOCKED, INVALID_CLASS -> 0xFFFFB5A1;
            default -> 0xFFD7CEC3;
        };
    }

    private boolean hasPlayed(int index) {
        return serverState.played()[index];
    }

    private int level(int index) {
        return serverState.levels()[index];
    }

    private int skillPoints(int index) {
        return serverState.skillPoints()[index];
    }

    private enum Style { PREVIOUS, NEXT, TAB, CONFIRM }

    private final class StyledButton extends AbstractButton {
        private final Style style;
        private final int classIndex;
        private final Consumer<StyledButton> action;
        private boolean selected;
        private boolean current;

        private StyledButton(ClassSelectionLayout.Box box, int classIndex, Component label,
                             Style style, Consumer<StyledButton> action) {
            super(box.x(), box.y(), box.width(), box.height(), label);
            this.classIndex = classIndex;
            this.style = style;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.accept(this);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            if (style == Style.TAB) {
                renderClassTab(graphics, hovered);
                return;
            }
            if (style == Style.PREVIOUS || style == Style.NEXT) {
                int edge = active ? 0xFFBDAF9A : 0xFF756D65;
                int accent = hovered ? ACCENTS[selectedIndex] : 0xFF605B58;
                drawSmallFrame(graphics, getX(), getY(), width, height, edge, active ? 0xD31C1921 : 0xA6131117, accent);
                drawChevron(graphics, getX() + width / 2, getY() + height / 2,
                        style == Style.PREVIOUS ? -1 : 1, active ? 0xFFE7D9C3 : 0xFF79716A,
                        Math.max(5, height / 3));
                return;
            }

            int accent = ACCENTS[selectedIndex];
            int background = !active ? 0xFF343038 : (hovered ? 0xFF493A55 : 0xFF332A3B);
            drawSmallFrame(graphics, getX(), getY(), width, height,
                    active ? accent : 0xFF746B62, background, active ? accent : 0xFF746B62);
            drawFittedCentered(graphics, getMessage(), getX() + width / 2,
                    getY() + Math.max(0, (height - font.lineHeight) / 2), width - 12,
                    active ? 0xFFF2E8D8 : 0xFFB0AAA2);
        }

        private void renderClassTab(GuiGraphics graphics, boolean hovered) {
            int accent = ACCENTS[classIndex];
            int fill = active ? 0xD31A1820 : 0xA4121218;
            graphics.fill(getX() + 3, getY(), getX() + width - 3, getY() + height, fill);
            graphics.fill(getX(), getY() + 3, getX() + width, getY() + height - 3, fill);
            int edge = selected ? accent : (hovered ? 0xFFD0C5B5 : 0xFF615B57);
            int thickness = selected ? 2 : 1;
            graphics.fill(getX() + 3, getY(), getX() + width - 3, getY() + thickness, edge);
            graphics.fill(getX() + 3, getY() + height - thickness, getX() + width - 3, getY() + height, edge);
            graphics.fill(getX(), getY() + 3, getX() + thickness, getY() + height - 3, edge);
            graphics.fill(getX() + width - thickness, getY() + 3, getX() + width, getY() + height - 3, edge);
            if (selected) graphics.fill(getX() + width / 2 - 2, getY(), getX() + width / 2 + 2, getY() + 3, accent);

            List<FormattedCharSequence> lines = font.split(getMessage(), Math.max(1, width - 8));
            int lineCount = Math.max(1, Math.min(2, lines.size()));
            int iconSize = Math.min(26, Math.max(15, height - lineCount * font.lineHeight - 7));
            int iconX = getX() + (width - iconSize) / 2;
            int iconY = getY() + 3;
            graphics.blit(CLASS_ICONS[classIndex], iconX, iconY, iconSize, iconSize,
                    0, 0, 96, 96, 96, 96);
            int labelY = getY() + height - lineCount * font.lineHeight - 3;
            int labelColor = active ? (selected ? 0xFFF2E8DA : 0xFFD1C9BE) : 0xFF8D8781;
            for (int index = 0; index < lineCount; index++) {
                drawFittedCentered(graphics, lines.get(index), getX() + width / 2,
                        labelY + index * font.lineHeight, width - 6, labelColor);
            }
            if (current) {
                int center = getX() + width / 2;
                graphics.fill(center - 2, getY() + height - 1, center + 2, getY() + height, 0xFFF0D7AD);
            }
        }

        private void setSelected(boolean selected) { this.selected = selected; }
        private void setCurrent(boolean current) { this.current = current; }
    }

    private void drawSmallFrame(GuiGraphics graphics, int x, int y, int buttonWidth, int buttonHeight,
                                int edge, int background, int accent) {
        graphics.fill(x + 3, y, x + buttonWidth - 3, y + buttonHeight, background);
        graphics.fill(x, y + 3, x + buttonWidth, y + buttonHeight - 3, background);
        graphics.fill(x + 3, y, x + buttonWidth - 3, y + 1, edge);
        graphics.fill(x + 3, y + buttonHeight - 1, x + buttonWidth - 3, y + buttonHeight, edge);
        graphics.fill(x, y + 3, x + 1, y + buttonHeight - 3, edge);
        graphics.fill(x + buttonWidth - 1, y + 3, x + buttonWidth, y + buttonHeight - 3, edge);
        graphics.fill(x + 4, y + 2, x + 6, y + 3, accent);
        graphics.fill(x + buttonWidth - 6, y + 2, x + buttonWidth - 4, y + 3, accent);
    }
}

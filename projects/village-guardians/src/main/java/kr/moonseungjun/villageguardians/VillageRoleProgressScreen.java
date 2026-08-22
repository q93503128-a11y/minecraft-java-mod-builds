package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VillageRoleProgressScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int BG = 0xFF090E13;
    private static final int PANEL = 0xF40D151D;
    private static final int SURFACE = 0xFF17222B;
    private static final int SURFACE_2 = 0xFF1D2B35;
    private static final int BORDER = 0xFF364754;
    private static final int TEXT = 0xFFDCE5EA;
    private static final int MUTED = 0xFF8998A2;
    private static final int ACCENT = 0xFF4A9188;
    private static final int GOLD = 0xFFC3A45D;
    private static final int RED = 0xFFAA6068;
    private static final int BLUE = 0xFF5D7FA2;
    private static final int PURPLE = 0xFF7D6C99;
    private static final int HEADER_HEIGHT = 52;
    private static final int SKILL_GAP = 7;

    private static double savedZoom = 0.74;
    private static double savedPanX;
    private static double savedPanY;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final boolean skillsOnly;
    private final List<TreeEntry> nodes = new ArrayList<>();
    private final List<SkillEntry> skills = new ArrayList<>();
    private String roleId = "";
    private String roleName = "직업";
    private String summary = "";
    private Tab tab = Tab.TREE;
    private int selectedNode = -1;
    private int selectedSkill = -1;
    private int skillScroll;
    private boolean dragging;

    public VillageRoleProgressScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        this.skillsOnly = "role_skills".equals(payload.screenId());
        this.tab = skillsOnly ? Tab.SKILLS : Tab.TREE;
        parsePayload();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BG);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BG);
        if (tab == Tab.TREE) renderTree(graphics, mouseX, mouseY);
        else renderSkills(graphics, mouseX, mouseY);
        renderHeader(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, HEADER_HEIGHT, PANEL);
        graphics.fill(0, HEADER_HEIGHT - 1, width, HEADER_HEIGHT, BORDER);
        int closeX = width - 28;
        int controlsLeft = tab == Tab.TREE && !skillsOnly ? closeX - 134 : closeX;
        graphics.text(font, fit(skillsOnly ? roleName + " 기술 연구" : roleName + " 성장",
                        Math.max(80, controlsLeft - 18)), 10, 6, TEXT, false);
        graphics.text(font, fit(summary, Math.max(80, controlsLeft - 18)),
                10, 19, MUTED, false);
        if (!skillsOnly) {
            drawTab(graphics, mouseX, mouseY, 10, 33, 84, "성장 경로", tab == Tab.TREE);
            drawTab(graphics, mouseX, mouseY, 100, 33, 84, "기술 관리", tab == Tab.SKILLS);
        } else {
            graphics.text(font, fit("기술 습득과 " + VillageClientKeys.skillOneKeyName()
                            + "/" + VillageClientKeys.skillTwoKeyName() + " 장착",
                    Math.max(80, closeX - 18)),
                    10, 36, ACCENT, false);
        }

        drawSmall(graphics, mouseX, mouseY, closeX, 7, 20, "×");
        if (tab == Tab.TREE && !skillsOnly) {
            int centerX = closeX - 42;
            int plusX = centerX - 26;
            int percentX = plusX - 36;
            int minusX = percentX - 26;
            drawSmall(graphics, mouseX, mouseY, minusX, 7, 21, "−");
            graphics.centeredText(font, Math.round(savedZoom * 100) + "%",
                    percentX + 16, 13, MUTED);
            drawSmall(graphics, mouseX, mouseY, plusX, 7, 21, "+");
            drawSmall(graphics, mouseX, mouseY, centerX, 7, 37, "중앙");
        }
    }

    private void renderTree(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Viewport view = contentViewport();
        graphics.fill(view.left(), view.top(), view.right(), view.bottom(), BG);
        renderTreeGrid(graphics, view);
        renderTreeConnections(graphics, view);
        renderTreeRoot(graphics, view);
        renderTreeNodes(graphics, mouseX, mouseY, view);
        renderTreeBubble(graphics, mouseX, mouseY, view);
    }

    private void renderTreeGrid(GuiGraphicsExtractor graphics, Viewport view) {
        int spacing = Math.max(16, (int) Math.round(28 * savedZoom));
        int startX = view.left() + Math.floorMod((int) Math.round(savedPanX), spacing);
        int startY = view.top() + Math.floorMod((int) Math.round(savedPanY), spacing);
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        for (int x = startX; x < view.right(); x += spacing) {
            graphics.fill(x, view.top(), x + 1, view.bottom(), 0xFF141C24);
        }
        for (int y = startY; y < view.bottom(); y += spacing) {
            graphics.fill(view.left(), y, view.right(), y + 1, 0xFF141C24);
        }
        graphics.disableScissor();
    }

    private void renderTreeConnections(GuiGraphicsExtractor graphics, Viewport view) {
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        for (TreeEntry node : nodes) {
            TreeEntry previous = previous(node);
            double x0 = previous == null ? 0 : previous.worldX();
            double y0 = previous == null ? 0 : previous.worldY();
            int color = previous == null || "습득".equals(previous.status())
                    ? branchColor(node.branch()) : 0xFF2C3944;
            drawLine(graphics, screenX(view, x0), screenY(view, y0),
                    screenX(view, node.worldX()), screenY(view, node.worldY()), color);
        }
        graphics.disableScissor();
    }

    private void renderTreeRoot(GuiGraphicsExtractor graphics, Viewport view) {
        int size = Math.max(20, scaledNodeSize() - 6);
        int cx = screenX(view, 0);
        int cy = screenY(view, 0);
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        graphics.fill(cx - size / 2 - 2, cy - size / 2 - 2,
                cx + size / 2 + 2, cy + size / 2 + 2, GOLD);
        graphics.fill(cx - size / 2, cy - size / 2,
                cx + size / 2, cy + size / 2, SURFACE);
        graphics.centeredText(font, roleName.isBlank() ? "직" : roleName.substring(0, 1),
                cx, cy - 4, GOLD);
        graphics.disableScissor();
    }

    private void renderTreeNodes(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Viewport view) {
        int size = scaledNodeSize();
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        for (int i = 0; i < nodes.size(); i++) {
            TreeEntry node = nodes.get(i);
            int cx = screenX(view, node.worldX());
            int cy = screenY(view, node.worldY());
            int x = cx - size / 2;
            int y = cy - size / 2;
            if (x + size < view.left() || x > view.right() || y + size < view.top() || y > view.bottom()) continue;
            boolean hovered = inside(mouseX, mouseY, x, y, size, size);
            boolean selected = selectedNode == i;
            int branch = branchColor(node.branch());
            int border = switch (node.status()) {
                case "습득" -> branch;
                case "습득 가능" -> GOLD;
                default -> BORDER;
            };
            graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2,
                    selected ? GOLD : hovered ? border : 0xFF121B22);
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, border);
            graphics.fill(x, y, x + size, y + size, hovered || selected ? SURFACE_2 : SURFACE);
            drawBranchIcon(graphics, node.branch(), x + 5, y + 5, size - 10, border);
            if (hovered || selected || savedZoom >= 0.88) {
                graphics.centeredText(font, fit(node.title(), 92), cx, y + size + 4,
                        hovered || selected ? TEXT : MUTED);
            }
        }
        graphics.disableScissor();
    }

    private void renderTreeBubble(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Viewport view) {
        TreeBubble bubble = treeBubble(view);
        if (bubble == null) return;
        TreeEntry node = bubble.node();
        int edgeX = bubble.x() > bubble.nodeX() ? bubble.x() : bubble.x() + bubble.width();
        int edgeY = clamp(bubble.nodeY(), bubble.y() + 12, bubble.y() + bubble.height() - 12);
        drawLine(graphics, bubble.nodeX(), bubble.nodeY(), edgeX, edgeY, branchColor(node.branch()));
        drawPopoverPanel(graphics, bubble.x(), bubble.y(), bubble.width(), bubble.height(),
                branchColor(node.branch()));
        graphics.text(font, fit(node.branch().displayName() + " " + node.tier() + "단계 · " + node.title(),
                        bubble.width() - 16), bubble.x() + 8, bubble.y() + 7, TEXT, false);
        graphics.text(font, fit("요구 Lv." + node.level() + " · 주화 " + node.cost()
                        + " · " + node.status(), bubble.width() - 16),
                bubble.x() + 8, bubble.y() + 20, statusColor(node.status()), false);
        int y = bubble.y() + 34;
        for (int i = 0; i < bubble.lineCount(); i++) {
            graphics.text(font, bubble.lines().get(i), bubble.x() + 8, y, MUTED, false);
            y += 11;
        }
        if (bubble.purchasable()) {
            drawInlineButton(graphics, mouseX, mouseY, bubble.buttonX(), bubble.buttonY(),
                    bubble.buttonWidth(), bubble.buttonHeight(), "노드 습득", true, ACCENT);
        }
    }

    private TreeBubble treeBubble(Viewport view) {
        if (selectedNode < 0 || selectedNode >= nodes.size()) return null;
        TreeEntry node = nodes.get(selectedNode);
        int nodeX = screenX(view, node.worldX());
        int nodeY = screenY(view, node.worldY());
        int nodeHalf = scaledNodeSize() / 2;
        if (nodeX + nodeHalf < view.left() || nodeX - nodeHalf > view.right()
                || nodeY + nodeHalf < view.top() || nodeY - nodeHalf > view.bottom()) {
            return null;
        }
        int bubbleWidth = fitPopoverWidth(view.width(), 176, 264);
        boolean purchasable = "습득 가능".equals(node.status());
        List<FormattedCharSequence> lines = font.split(Component.literal(node.description()),
                Math.max(40, bubbleWidth - 16));
        int baseHeight = 45 + (purchasable ? 24 : 8);
        int lineCount = Math.min(lines.size(), Math.max(0, (view.height() - baseHeight - 10) / 11));
        int bubbleHeight = baseHeight + lineCount * 11;
        int size = scaledNodeSize();
        int x = nodeX + size / 2 + 9;
        if (x + bubbleWidth > view.right() - 5) x = nodeX - size / 2 - bubbleWidth - 9;
        x = clamp(x, view.left() + 5, Math.max(view.left() + 5, view.right() - bubbleWidth - 5));
        int y = clamp(nodeY - 25, view.top() + 5,
                Math.max(view.top() + 5, view.bottom() - bubbleHeight - 5));
        int buttonWidth = 76;
        int buttonHeight = 18;
        int buttonX = x + bubbleWidth - buttonWidth - 7;
        int buttonY = y + bubbleHeight - buttonHeight - 6;
        return new TreeBubble(x, y, bubbleWidth, bubbleHeight, buttonX, buttonY,
                buttonWidth, buttonHeight, purchasable, lines, lineCount, node, nodeX, nodeY);
    }

    private void renderSkills(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Viewport view = contentViewport();
        graphics.fill(view.left(), view.top(), view.right(), view.bottom(), BG);
        SkillGrid grid = skillGrid(view);
        skillScroll = clamp(skillScroll, 0, grid.maxScroll());
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        for (int i = 0; i < skills.size(); i++) {
            CardBounds card = skillCardBounds(view, grid, i);
            if (card.y() + card.size() < view.top() || card.y() > view.bottom()) continue;
            SkillEntry skill = skills.get(i);
            boolean hovered = inside(mouseX, mouseY, card.x(), card.y(), card.size(), card.size());
            boolean selected = selectedSkill == i;
            int accent = skillColor(i);
            int border = selected ? GOLD : hovered ? accent : BORDER;
            graphics.fill(card.x() - 1, card.y() - 1,
                    card.x() + card.size() + 1, card.y() + card.size() + 1, border);
            graphics.fill(card.x(), card.y(), card.x() + card.size(), card.y() + card.size(),
                    selected || hovered ? SURFACE_2 : SURFACE);
            graphics.fill(card.x(), card.y(), card.x() + 4, card.y() + card.size(), accent);
            int icon = Math.max(8, card.size() / 7);
            graphics.fill(card.x() + card.size() - icon - 7, card.y() + 7,
                    card.x() + card.size() - 7, card.y() + 7 + icon, accent);
            graphics.text(font, fit(skill.name(), card.size() - 28),
                    card.x() + 10, card.y() + 9, TEXT, false);
            graphics.text(font, fit("Lv." + skill.level() + " · " + skill.cost() + "주화", card.size() - 18),
                    card.x() + 10, card.y() + 27, MUTED, false);
            graphics.text(font, fit(shortStatus(skill), card.size() - 18),
                    card.x() + 10, card.y() + card.size() - 17,
                    skill.status().startsWith("장착") ? ACCENT
                            : "습득 가능".equals(skill.status()) ? GOLD : MUTED, false);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, view.right() - 4, view.top() + 5, view.bottom() - 5,
                skillScroll, grid.maxScroll(), view.height(), grid.contentHeight());
        renderSkillBubble(graphics, mouseX, mouseY, view, grid);
    }

    private void renderSkillBubble(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   Viewport view, SkillGrid grid) {
        SkillBubble bubble = skillBubble(view, grid);
        if (bubble == null) return;
        SkillEntry skill = bubble.skill();
        int edgeX = bubble.x() > bubble.cardX() ? bubble.x() : bubble.x() + bubble.width();
        int edgeY = clamp(bubble.cardY(), bubble.y() + 12, bubble.y() + bubble.height() - 12);
        drawLine(graphics, bubble.cardX(), bubble.cardY(), edgeX, edgeY, skillColor(selectedSkill));
        drawPopoverPanel(graphics, bubble.x(), bubble.y(), bubble.width(), bubble.height(),
                skillColor(selectedSkill));
        graphics.text(font, fit(skill.name(), bubble.width() - 16),
                bubble.x() + 8, bubble.y() + 7, TEXT, false);
        graphics.text(font, fit("요구 Lv." + skill.level() + " · 주화 " + skill.cost()
                        + " · " + skill.status(), bubble.width() - 16),
                bubble.x() + 8, bubble.y() + 20, statusColor(skill.status()), false);
        int y = bubble.y() + 34;
        for (int i = 0; i < bubble.lineCount(); i++) {
            graphics.text(font, bubble.lines().get(i), bubble.x() + 8, y, MUTED, false);
            y += 11;
        }
        if (bubble.learned()) {
            drawInlineButton(graphics, mouseX, mouseY, bubble.firstX(), bubble.buttonY(),
                    bubble.buttonWidth(), bubble.buttonHeight(),
                    skill.slot() == 0 ? VillageClientKeys.skillOneKeyName() + " 슬롯 ✓"
                            : VillageClientKeys.skillOneKeyName() + " 슬롯", true, ACCENT);
            drawInlineButton(graphics, mouseX, mouseY, bubble.secondX(), bubble.buttonY(),
                    bubble.buttonWidth(), bubble.buttonHeight(),
                    skill.slot() == 1 ? VillageClientKeys.skillTwoKeyName() + " 슬롯 ✓"
                            : VillageClientKeys.skillTwoKeyName() + " 슬롯", true, ACCENT);
        } else if (bubble.unlockable()) {
            drawInlineButton(graphics, mouseX, mouseY, bubble.firstX(), bubble.buttonY(),
                    bubble.unlockWidth(), bubble.buttonHeight(), "연구소에서 습득", true, ACCENT);
        }
    }

    private SkillBubble skillBubble(Viewport view, SkillGrid grid) {
        if (selectedSkill < 0 || selectedSkill >= skills.size()) return null;
        SkillEntry skill = skills.get(selectedSkill);
        CardBounds card = skillCardBounds(view, grid, selectedSkill);
        if (card.y() + card.size() < view.top() || card.y() > view.bottom()) return null;
        int cardX = card.x() + card.size() / 2;
        int cardY = card.y() + card.size() / 2;
        int maximumWidth = Math.max(1, view.width() - 10);
        int preferredWidth = Math.min(maximumWidth, Math.min(270, Math.max(180, view.width() / 3)));
        int rightSpace = view.right() - 5 - (card.x() + card.size() + 9);
        int leftSpace = card.x() - 9 - (view.left() + 5);
        boolean placeRight = rightSpace >= leftSpace;
        int sideSpace = Math.max(rightSpace, leftSpace);
        boolean horizontalPlacement = sideSpace >= Math.min(160, preferredWidth);
        int bubbleWidth = horizontalPlacement ? Math.min(preferredWidth, sideSpace) : preferredWidth;
        boolean learned = isLearned(skill);
        boolean unlockable = "습득 가능".equals(skill.status());
        List<FormattedCharSequence> lines = font.split(Component.literal(skill.description()),
                Math.max(40, bubbleWidth - 16));
        int baseHeight = 45 + ((learned || unlockable) ? 24 : 8);
        int lineCount = Math.min(lines.size(), Math.max(0, (view.height() - baseHeight - 10) / 11));
        int bubbleHeight = baseHeight + lineCount * 11;

        int x;
        int y;
        if (horizontalPlacement) {
            x = placeRight ? card.x() + card.size() + 9 : card.x() - bubbleWidth - 9;
            y = clamp(card.y() + 4, view.top() + 5,
                    Math.max(view.top() + 5, view.bottom() - bubbleHeight - 5));
        } else {
            x = clamp(cardX - bubbleWidth / 2, view.left() + 5,
                    Math.max(view.left() + 5, view.right() - bubbleWidth - 5));
            int below = card.y() + card.size() + 8;
            int above = card.y() - bubbleHeight - 8;
            if (below + bubbleHeight <= view.bottom() - 5) y = below;
            else if (above >= view.top() + 5) y = above;
            else y = clamp(card.y() + 4, view.top() + 5,
                        Math.max(view.top() + 5, view.bottom() - bubbleHeight - 5));
        }

        int buttonHeight = 18;
        int buttonGap = 6;
        int buttonWidth = learned
                ? Math.max(1, Math.min(66, (bubbleWidth - 20 - buttonGap) / 2))
                : Math.max(1, Math.min(66, bubbleWidth - 14));
        int unlockWidth = Math.max(1, Math.min(104, bubbleWidth - 14));
        int buttonY = y + bubbleHeight - buttonHeight - 6;
        int secondX = x + bubbleWidth - buttonWidth - 7;
        int firstX = learned ? secondX - buttonWidth - buttonGap : x + bubbleWidth - unlockWidth - 7;
        return new SkillBubble(x, y, bubbleWidth, bubbleHeight, firstX, secondX,
                buttonY, buttonWidth, unlockWidth, buttonHeight, learned, unlockable,
                lines, lineCount, skill, cardX, cardY);
    }

    private static int fitPopoverWidth(int viewportWidth, int preferredMinimum, int preferredMaximum) {
        int maximum = Math.max(1, viewportWidth - 10);
        int minimum = Math.min(preferredMinimum, maximum);
        int preferred = Math.max(minimum, Math.min(maximum, viewportWidth / 3));
        return Math.min(maximum, Math.min(preferredMaximum, preferred));
    }

    private SkillGrid skillGrid(Viewport view) {
        int columns = Math.min(4, Math.max(2, Math.max(1, view.width() - 20) / 105));
        int size = clamp((view.width() - 20 - SKILL_GAP * (columns - 1)) / columns, 70, 92);
        int rows = Math.max(1, (skills.size() + columns - 1) / columns);
        int contentHeight = rows * size + Math.max(0, rows - 1) * SKILL_GAP + 18;
        int maxScroll = Math.max(0, contentHeight - view.height());
        int totalWidth = columns * size + Math.max(0, columns - 1) * SKILL_GAP;
        int left = view.left() + Math.max(10, (view.width() - totalWidth) / 2);
        return new SkillGrid(columns, size, left, contentHeight, maxScroll);
    }

    private CardBounds skillCardBounds(Viewport view, SkillGrid grid, int index) {
        int x = grid.left() + (index % grid.columns()) * (grid.size() + SKILL_GAP);
        int y = view.top() + 9 + (index / grid.columns()) * (grid.size() + SKILL_GAP) - skillScroll;
        return new CardBounds(x, y, grid.size());
    }

    private String shortStatus(SkillEntry skill) {
        if (skill.slot() == 0) return VillageClientKeys.skillOneKeyName() + " 슬롯 장착";
        if (skill.slot() == 1) return VillageClientKeys.skillTwoKeyName() + " 슬롯 장착";
        if (isLearned(skill)) return "습득 완료";
        return skill.status();
    }

    private boolean isLearned(SkillEntry skill) {
        return "습득".equals(skill.status()) || skill.status().startsWith("장착");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (inside(click.x(), click.y(), width - 28, 7, 20, 20)) {
            onClose();
            return true;
        }
        if (!skillsOnly && inside(click.x(), click.y(), 10, 33, 84, 17)) {
            tab = Tab.TREE;
            selectedSkill = -1;
            return true;
        }
        if (!skillsOnly && inside(click.x(), click.y(), 100, 33, 84, 17)) {
            tab = Tab.SKILLS;
            selectedNode = -1;
            return true;
        }
        if (tab == Tab.TREE && !skillsOnly) return clickTree(click) || super.mouseClicked(click, doubled);
        return clickSkills(click) || super.mouseClicked(click, doubled);
    }

    private boolean clickTree(MouseButtonEvent click) {
        int closeX = width - 28;
        int centerX = closeX - 42;
        int plusX = centerX - 26;
        int percentX = plusX - 36;
        int minusX = percentX - 26;
        if (inside(click.x(), click.y(), centerX, 7, 37, 20)) {
            savedPanX = 0; savedPanY = 0; savedZoom = 0.74; return true;
        }
        if (inside(click.x(), click.y(), minusX, 7, 21, 20)) {
            setZoom(savedZoom - 0.12, width / 2.0, contentViewport().top() + contentViewport().height() / 2.0);
            return true;
        }
        if (inside(click.x(), click.y(), plusX, 7, 21, 20)) {
            setZoom(savedZoom + 0.12, width / 2.0, contentViewport().top() + contentViewport().height() / 2.0);
            return true;
        }
        Viewport view = contentViewport();
        TreeBubble bubble = treeBubble(view);
        if (bubble != null) {
            if (bubble.purchasable() && inside(click.x(), click.y(), bubble.buttonX(), bubble.buttonY(),
                    bubble.buttonWidth(), bubble.buttonHeight())) {
                send(bubble.node().action());
                return true;
            }
            if (inside(click.x(), click.y(), bubble.x(), bubble.y(), bubble.width(), bubble.height())) return true;
        }
        if (!inside(click.x(), click.y(), view.left(), view.top(), view.width(), view.height())) return false;
        int size = scaledNodeSize();
        for (int i = 0; i < nodes.size(); i++) {
            TreeEntry candidate = nodes.get(i);
            int x = screenX(view, candidate.worldX()) - size / 2;
            int y = screenY(view, candidate.worldY()) - size / 2;
            if (inside(click.x(), click.y(), x, y, size, size)) {
                selectedNode = selectedNode == i ? -1 : i;
                return true;
            }
        }
        selectedNode = -1;
        dragging = true;
        return true;
    }

    private boolean clickSkills(MouseButtonEvent click) {
        Viewport view = contentViewport();
        SkillGrid grid = skillGrid(view);
        SkillBubble bubble = skillBubble(view, grid);
        if (bubble != null) {
            if (bubble.learned()) {
                if (inside(click.x(), click.y(), bubble.firstX(), bubble.buttonY(),
                        bubble.buttonWidth(), bubble.buttonHeight())) {
                    send(equipAction(bubble.skill().id(), 0));
                    return true;
                }
                if (inside(click.x(), click.y(), bubble.secondX(), bubble.buttonY(),
                        bubble.buttonWidth(), bubble.buttonHeight())) {
                    send(equipAction(bubble.skill().id(), 1));
                    return true;
                }
            } else if (bubble.unlockable()
                    && inside(click.x(), click.y(), bubble.firstX(), bubble.buttonY(),
                    bubble.unlockWidth(), bubble.buttonHeight())) {
                send(bubble.skill().unlockAction());
                return true;
            }
            if (inside(click.x(), click.y(), bubble.x(), bubble.y(), bubble.width(), bubble.height())) return true;
        }
        for (int i = 0; i < skills.size(); i++) {
            CardBounds card = skillCardBounds(view, grid, i);
            if (inside(click.x(), click.y(), card.x(), card.y(), card.size(), card.size())) {
                selectedSkill = selectedSkill == i ? -1 : i;
                return true;
            }
        }
        if (inside(click.x(), click.y(), view.left(), view.top(), view.width(), view.height())) {
            selectedSkill = -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (tab == Tab.TREE && dragging && event.button() == 0) {
            savedPanX = clamp(savedPanX + dragX, -1000, 1000);
            savedPanY = clamp(savedPanY + dragY, -1000, 1000);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Viewport view = contentViewport();
        if (!inside(mouseX, mouseY, view.left(), view.top(), view.width(), view.height())) {
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        if (tab == Tab.TREE) {
            setZoom(savedZoom + vertical * 0.09, mouseX, mouseY);
        } else {
            skillScroll = Math.max(0, skillScroll - (int) Math.round(vertical * 34));
        }
        return true;
    }

    private void parsePayload() {
        String[] header = payload.body().split("\\|", -1);
        if (header.length >= 2) {
            roleId = header[0];
            roleName = header[1];
        }
        summary = header.length >= 3 ? header[2] : payload.body();
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String[] p = labels[i].split("\\|", -1);
            if (p.length >= 9 && "node".equals(p[0])) {
                Branch branch = Branch.parse(p[2]);
                int tier = parseInt(p[3], 1);
                double x = switch (branch) {
                    case DURATION -> -145;
                    case POWER -> 0;
                    case SPECIAL -> 145;
                };
                double y = -tier * 76.0;
                nodes.add(new TreeEntry(actions[i], p[1], branch, tier, p[4], p[5],
                        parseInt(p[6], 1), parseInt(p[7], 0), p[8], x, y));
            } else if (p.length >= 8 && "skill".equals(p[0])) {
                skills.add(new SkillEntry(actions[i], p[1], p[2], p[3],
                        parseInt(p[4], 1), parseInt(p[5], 0), p[6], parseInt(p[7], -1)));
            }
        }
    }

    private TreeEntry previous(TreeEntry node) {
        if (node.tier() <= 1) return null;
        for (TreeEntry candidate : nodes) {
            if (candidate.branch() == node.branch() && candidate.tier() == node.tier() - 1) return candidate;
        }
        return null;
    }

    private void setZoom(double requested, double mouseX, double mouseY) {
        Viewport view = contentViewport();
        double old = savedZoom;
        double next = clamp(requested, 0.50, 1.55);
        double anchorX = view.left() + view.width() / 2.0;
        double anchorY = view.bottom() - 38.0;
        double worldX = (mouseX - anchorX - savedPanX) / old;
        double worldY = (mouseY - anchorY - savedPanY) / old;
        savedZoom = next;
        savedPanX = clamp(mouseX - anchorX - worldX * next, -1000, 1000);
        savedPanY = clamp(mouseY - anchorY - worldY * next, -1000, 1000);
    }

    private int screenX(Viewport view, double x) {
        return (int) Math.round(view.left() + view.width() / 2.0 + savedPanX + x * savedZoom);
    }

    private int screenY(Viewport view, double y) {
        return (int) Math.round(view.bottom() - 38.0 + savedPanY + y * savedZoom);
    }

    private int scaledNodeSize() {
        return (int) Math.round(clamp(38 * savedZoom, 24, 52));
    }

    private Viewport contentViewport() {
        return new Viewport(5, HEADER_HEIGHT + 4, width - 5, Math.max(HEADER_HEIGHT + 5, height - 5));
    }

    private void drawTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                         int x, int y, int w, String text, boolean active) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, 17);
        graphics.fill(x, y, x + w, y + 17, active ? ACCENT : hovered ? SURFACE_2 : SURFACE);
        graphics.centeredText(font, fit(text, w - 6), x + w / 2, y + 4,
                active ? 0xFF07100F : TEXT);
    }

    private void drawSmall(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                           int x, int y, int w, String text) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, 20);
        graphics.fill(x, y, x + w, y + 20, hovered ? SURFACE_2 : SURFACE);
        graphics.centeredText(font, fit(text, w - 4), x + w / 2, y + 6,
                hovered ? TEXT : MUTED);
    }

    private void drawInlineButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                  int x, int y, int w, int h, String text,
                                  boolean active, int accent) {
        boolean hovered = active && inside(mouseX, mouseY, x, y, w, h);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1,
                hovered ? GOLD : active ? accent : BORDER);
        graphics.fill(x, y, x + w, y + h, hovered ? SURFACE_2 : SURFACE);
        graphics.centeredText(font, fit(text, w - 6), x + w / 2, y + 5,
                active ? TEXT : MUTED);
    }

    private void drawPopoverPanel(GuiGraphicsExtractor graphics, int x, int y,
                                  int w, int h, int accent) {
        graphics.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x78000000);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, accent);
        graphics.fill(x, y, x + w, y + h, PANEL);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int scroll, int maxScroll, int visible, int content) {
        if (maxScroll <= 0 || content <= visible) return;
        int track = Math.max(1, bottom - top);
        int thumb = Math.max(14, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(scroll, 0, maxScroll) / maxScroll;
        graphics.fill(x, top, x + 3, bottom, 0xFF05080B);
        graphics.fill(x, y, x + 3, y + thumb, ACCENT);
    }

    private void drawBranchIcon(GuiGraphicsExtractor graphics, Branch branch,
                                int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (branch) {
            case DURATION -> {
                graphics.fill(cx - 2, y + 2, cx + 2, y + size - 2, color);
                graphics.fill(cx - 6, y + 2, cx + 6, y + 5, color);
            }
            case POWER -> {
                graphics.fill(x + 3, cy - 2, x + size - 3, cy + 2, color);
                graphics.fill(cx - 2, y + 3, cx + 2, y + size - 3, color);
            }
            case SPECIAL -> {
                graphics.fill(cx - 2, y + 2, cx + 2, y + size - 2, color);
                graphics.fill(x + 3, cy - 2, x + size - 3, cy + 2, color);
                graphics.fill(x + 4, y + 4, x + 7, y + 7, GOLD);
            }
        }
    }

    private void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            graphics.fill(x0 - 1, y0 - 1, x0 + 2, y0 + 2, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private int branchColor(Branch branch) {
        return switch (branch) {
            case DURATION -> BLUE;
            case POWER -> RED;
            case SPECIAL -> PURPLE;
        };
    }

    private int skillColor(int index) {
        return switch (index % 4) {
            case 0 -> ACCENT;
            case 1 -> BLUE;
            case 2 -> PURPLE;
            default -> GOLD;
        };
    }

    private int statusColor(String status) {
        if (status == null) return MUTED;
        if (status.startsWith("장착")) return ACCENT;
        return switch (status) {
            case "습득" -> ACCENT;
            case "습득 가능" -> GOLD;
            default -> MUTED;
        };
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private String fit(String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (maxWidth <= 0 || font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        int end = normalized.length();
        while (end > 1 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, Math.max(1, end)) + suffix;
    }

    private String equipAction(String skillId, int slot) {
        return (skillsOnly ? "research_skill_equip:" : "role_skill_equip:") + skillId + ":" + slot;
    }

    private void send(String action) {
        ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }

    private enum Tab { TREE, SKILLS }

    private enum Branch {
        DURATION, POWER, SPECIAL;

        String displayName() {
            return switch (this) {
                case DURATION -> "지속";
                case POWER -> "위력";
                case SPECIAL -> "특수";
            };
        }

        static Branch parse(String value) {
            if (value == null) return POWER;
            return switch (value.toLowerCase()) {
                case "duration" -> DURATION;
                case "special" -> SPECIAL;
                default -> POWER;
            };
        }
    }

    private record Viewport(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }

    private record TreeEntry(String action, String id, Branch branch, int tier, String title,
                             String description, int level, int cost, String status,
                             double worldX, double worldY) {}

    private record SkillEntry(String unlockAction, String id, String name, String description,
                              int level, int cost, String status, int slot) {}

    private record TreeBubble(int x, int y, int width, int height,
                              int buttonX, int buttonY, int buttonWidth, int buttonHeight,
                              boolean purchasable, List<FormattedCharSequence> lines, int lineCount,
                              TreeEntry node, int nodeX, int nodeY) {}

    private record SkillBubble(int x, int y, int width, int height,
                               int firstX, int secondX, int buttonY,
                               int buttonWidth, int unlockWidth, int buttonHeight,
                               boolean learned, boolean unlockable,
                               List<FormattedCharSequence> lines, int lineCount,
                               SkillEntry skill, int cardX, int cardY) {}

    private record SkillGrid(int columns, int size, int left,
                             int contentHeight, int maxScroll) {}

    private record CardBounds(int x, int y, int size) {}
}

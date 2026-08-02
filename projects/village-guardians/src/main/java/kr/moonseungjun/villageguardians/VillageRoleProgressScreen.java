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
    private static final int BG = 0xFF070B10;
    private static final int PANEL = 0xF40C131B;
    private static final int SURFACE = 0xFF14202A;
    private static final int SURFACE_2 = 0xFF1B2A36;
    private static final int BORDER = 0xFF405568;
    private static final int TEXT = 0xFFF4F7FA;
    private static final int MUTED = 0xFFA5B3BF;
    private static final int ACCENT = 0xFF43D6BC;
    private static final int GOLD = 0xFFF2C25B;
    private static final int RED = 0xFFE36E76;
    private static final int BLUE = 0xFF78A7ED;
    private static final int PURPLE = 0xFFB38AE8;
    private static final int HEADER_HEIGHT = 76;
    private static final int FOOTER_HEIGHT = 126;
    private static final int SKILL_CARD_HEIGHT = 70;
    private static final int SKILL_GAP = 8;

    private static double savedZoom = 0.86;
    private static double savedPanX;
    private static double savedPanY;

    private final VillageNetwork.OpenVillageUiPayload payload;
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
        renderHeader(graphics, mouseX, mouseY);
        if (tab == Tab.TREE) renderTree(graphics, mouseX, mouseY);
        else renderSkills(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, HEADER_HEIGHT, PANEL);
        graphics.fill(0, HEADER_HEIGHT - 2, width, HEADER_HEIGHT, BORDER);
        graphics.text(font, roleName + " 성장", 18, 12, TEXT, false);
        graphics.text(font, compact(summary, Math.max(26, (width - 190) / 7)), 18, 30, MUTED, false);
        drawTab(graphics, mouseX, mouseY, 18, 48, 112, "성장 경로", tab == Tab.TREE);
        drawTab(graphics, mouseX, mouseY, 138, 48, 112, "기술 습득·장착", tab == Tab.SKILLS);

        int closeX = width - 39;
        boolean hovered = inside(mouseX, mouseY, closeX, 10, 28, 28);
        graphics.fill(closeX, 10, closeX + 28, 38, hovered ? 0xFF6E3038 : SURFACE);
        graphics.centeredText(font, "×", closeX + 14, 19, hovered ? TEXT : MUTED);

        if (tab == Tab.TREE) {
            int centerX = closeX - 57;
            int plusX = centerX - 34;
            int percentX = plusX - 51;
            int minusX = percentX - 34;
            drawSmall(graphics, mouseX, mouseY, minusX, 10, 28, "−");
            graphics.centeredText(font, Math.round(savedZoom * 100) + "%", percentX + 24, 19, MUTED);
            drawSmall(graphics, mouseX, mouseY, plusX, 10, 28, "+");
            drawSmall(graphics, mouseX, mouseY, centerX, 10, 49, "중앙");
        }
    }

    private void renderTree(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Viewport view = contentViewport();
        graphics.fill(view.left(), view.top(), view.right(), view.bottom(), BG);
        renderTreeGrid(graphics, view);
        renderTreeConnections(graphics, view);
        renderTreeRoot(graphics, view);
        renderTreeNodes(graphics, mouseX, mouseY, view);
        renderTreeFooter(graphics, mouseX, mouseY);
    }

    private void renderTreeGrid(GuiGraphicsExtractor graphics, Viewport view) {
        int spacing = Math.max(18, (int) Math.round(30 * savedZoom));
        int startX = view.left() + Math.floorMod((int) Math.round(savedPanX), spacing);
        int startY = view.top() + Math.floorMod((int) Math.round(savedPanY), spacing);
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        for (int x = startX; x < view.right(); x += spacing) {
            graphics.fill(x, view.top(), x + 1, view.bottom(), 0xFF101923);
        }
        for (int y = startY; y < view.bottom(); y += spacing) {
            graphics.fill(view.left(), y, view.right(), y + 1, 0xFF101923);
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
                    ? branchColor(node.branch()) : 0xFF34434F;
            drawLine(graphics, screenX(view, x0), screenY(view, y0),
                    screenX(view, node.worldX()), screenY(view, node.worldY()), color);
        }
        graphics.disableScissor();
    }

    private void renderTreeRoot(GuiGraphicsExtractor graphics, Viewport view) {
        int size = scaledNodeSize() - 6;
        int cx = screenX(view, 0);
        int cy = screenY(view, 0);
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        graphics.fill(cx - size / 2 - 2, cy - size / 2 - 2,
                cx + size / 2 + 2, cy + size / 2 + 2, GOLD);
        graphics.fill(cx - size / 2, cy - size / 2,
                cx + size / 2, cy + size / 2, SURFACE_2);
        graphics.centeredText(font, roleName.isBlank() ? "직" : roleName.substring(0, 1), cx, cy - 4, GOLD);
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
            boolean hovered = inside(mouseX, mouseY, x, y, size, size);
            boolean selected = selectedNode == i;
            int branch = branchColor(node.branch());
            int border = switch (node.status()) {
                case "습득" -> branch;
                case "습득 가능" -> GOLD;
                default -> 0xFF384854;
            };
            graphics.fill(x - 3, y - 3, x + size + 3, y + size + 3,
                    selected ? GOLD : hovered ? border : 0xFF18242E);
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, border);
            graphics.fill(x, y, x + size, y + size, hovered || selected ? SURFACE_2 : SURFACE);
            drawBranchIcon(graphics, node.branch(), x + 7, y + 7, size - 14, border);
            if (savedZoom >= 0.78) {
                graphics.centeredText(font, compact(node.title(), 14), cx, y + size + 5,
                        selected ? TEXT : MUTED);
            }
        }
        graphics.disableScissor();
    }

    private void renderTreeFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int top = footerTop();
        graphics.fill(0, top, width, height, PANEL);
        graphics.fill(0, top, width, top + 2, BORDER);
        TreeEntry node = selectedNode >= 0 && selectedNode < nodes.size() ? nodes.get(selectedNode) : null;
        String title = node == null ? "성장 노드를 선택하세요"
                : node.branch().displayName() + " " + node.tier() + "단계 · " + node.title()
                + " · 요구 Lv." + node.level() + " · 주화 " + node.cost();
        String desc = node == null
                ? "지속·위력·특수 세 갈래 효과는 장착한 모든 직업 기술에 공통 적용됩니다."
                : node.description();
        String status = node == null ? "" : node.status();
        graphics.text(font, compact(title, Math.max(24, width / 7)), 18, top + 12, TEXT, false);
        if (!status.isBlank()) {
            graphics.text(font, status, 18, top + 29,
                    "습득 가능".equals(status) ? GOLD : MUTED, false);
        }

        int buttonW = Math.min(170, Math.max(110, width / 5));
        int buttonX = width - buttonW - 18;
        int buttonY = height - 40;
        int descWidth = Math.max(100, buttonX - 36);
        int y = top + 49;
        for (FormattedCharSequence line : font.split(Component.literal(desc), descWidth)) {
            if (y > height - 12) break;
            graphics.text(font, line, 18, y, MUTED, false);
            y += 11;
        }
        boolean active = node != null && "습득 가능".equals(node.status());
        boolean hovered = active && inside(mouseX, mouseY, buttonX, buttonY, buttonW, 28);
        drawActionButton(graphics, buttonX, buttonY, buttonW, 28,
                active, hovered, active ? "노드 습득" : node == null ? "선택 필요" : node.status());
    }

    private void renderSkills(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Viewport view = contentViewport();
        graphics.fill(view.left(), view.top(), view.right(), view.bottom(), 0xFF0B1219);
        int columns = view.width() >= 500 ? 2 : 1;
        int cardWidth = Math.max(150,
                (view.width() - 24 - SKILL_GAP * (columns - 1)) / columns);
        int rows = Math.max(1, (skills.size() + columns - 1) / columns);
        int contentHeight = rows * SKILL_CARD_HEIGHT + Math.max(0, rows - 1) * SKILL_GAP;
        int visible = Math.max(1, view.height() - 18);
        int maxScroll = Math.max(0, contentHeight - visible);
        skillScroll = clamp(skillScroll, 0, maxScroll);

        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        for (int i = 0; i < skills.size(); i++) {
            SkillEntry skill = skills.get(i);
            int x = view.left() + 10 + (i % columns) * (cardWidth + SKILL_GAP);
            int y = view.top() + 9 + (i / columns) * (SKILL_CARD_HEIGHT + SKILL_GAP) - skillScroll;
            boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, SKILL_CARD_HEIGHT);
            boolean selected = selectedSkill == i;
            int border = selected ? GOLD : hovered ? ACCENT : BORDER;
            graphics.fill(x - 1, y - 1, x + cardWidth + 1, y + SKILL_CARD_HEIGHT + 1, border);
            graphics.fill(x, y, x + cardWidth, y + SKILL_CARD_HEIGHT,
                    selected || hovered ? SURFACE_2 : SURFACE);
            graphics.fill(x, y, x + 5, y + SKILL_CARD_HEIGHT, skillColor(i));
            graphics.text(font, compact(skill.name(), Math.max(14, cardWidth / 8)),
                    x + 15, y + 10, TEXT, false);
            graphics.text(font, "요구 Lv." + skill.level() + " · 주화 " + skill.cost(),
                    x + 15, y + 27, MUTED, false);
            List<FormattedCharSequence> preview = font.split(Component.literal(skill.description()),
                    Math.max(80, cardWidth - 30));
            if (!preview.isEmpty()) {
                graphics.text(font, preview.getFirst(), x + 15, y + 43, MUTED, false);
            }
            graphics.text(font, skill.status(), x + 15, y + 57,
                    skill.status().startsWith("장착") ? ACCENT
                            : "습득 가능".equals(skill.status()) ? GOLD : MUTED, false);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, view.right() - 5, view.top() + 5, view.bottom() - 5,
                skillScroll, maxScroll, visible, contentHeight);
        renderSkillFooter(graphics, mouseX, mouseY);
    }

    private void renderSkillFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int top = footerTop();
        graphics.fill(0, top, width, height, PANEL);
        graphics.fill(0, top, width, top + 2, BORDER);
        SkillEntry skill = selectedSkill >= 0 && selectedSkill < skills.size() ? skills.get(selectedSkill) : null;
        String title = skill == null ? "기술을 선택하세요"
                : skill.name() + " · 요구 Lv." + skill.level() + " · 주화 " + skill.cost();
        String desc = skill == null
                ? "직업 기술은 최대 두 개까지 장착하며 기본 단축키는 R과 G입니다."
                : skill.description();
        graphics.text(font, compact(title, Math.max(24, width / 7)), 18, top + 12, TEXT, false);
        int y = top + 31;
        for (FormattedCharSequence line : font.split(Component.literal(desc), Math.max(100, width - 36))) {
            if (y > top + 61) break;
            graphics.text(font, line, 18, y, MUTED, false);
            y += 11;
        }

        int gap = 8;
        int totalWidth = Math.min(width - 36, 430);
        int buttonW = (totalWidth - gap * 2) / 3;
        int startX = width - totalWidth - 18;
        int buttonY = height - 39;
        boolean learned = skill != null
                && (skill.status().equals("습득") || skill.status().startsWith("장착"));
        boolean unlockable = skill != null && "습득 가능".equals(skill.status());
        drawActionButton(graphics, startX, buttonY, buttonW, 28,
                unlockable, unlockable && inside(mouseX, mouseY, startX, buttonY, buttonW, 28),
                unlockable ? "기술 습득" : skill == null ? "선택 필요" : skill.status());
        drawActionButton(graphics, startX + buttonW + gap, buttonY, buttonW, 28,
                learned, learned && inside(mouseX, mouseY, startX + buttonW + gap, buttonY, buttonW, 28),
                "R에 장착");
        drawActionButton(graphics, startX + (buttonW + gap) * 2, buttonY, buttonW, 28,
                learned, learned && inside(mouseX, mouseY, startX + (buttonW + gap) * 2, buttonY, buttonW, 28),
                "G에 장착");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (inside(click.x(), click.y(), width - 39, 10, 28, 28)) {
            onClose();
            return true;
        }
        if (inside(click.x(), click.y(), 18, 48, 112, 25)) {
            tab = Tab.TREE;
            return true;
        }
        if (inside(click.x(), click.y(), 138, 48, 112, 25)) {
            tab = Tab.SKILLS;
            return true;
        }
        if (tab == Tab.TREE) return clickTree(click) || super.mouseClicked(click, doubled);
        return clickSkills(click) || super.mouseClicked(click, doubled);
    }

    private boolean clickTree(MouseButtonEvent click) {
        int closeX = width - 39;
        int centerX = closeX - 57;
        int plusX = centerX - 34;
        int percentX = plusX - 51;
        int minusX = percentX - 34;
        if (inside(click.x(), click.y(), centerX, 10, 49, 28)) {
            savedPanX = 0; savedPanY = 0; savedZoom = 0.86; return true;
        }
        if (inside(click.x(), click.y(), minusX, 10, 28, 28)) {
            setZoom(savedZoom - 0.15, width / 2.0, contentViewport().top() + contentViewport().height() / 2.0);
            return true;
        }
        if (inside(click.x(), click.y(), plusX, 10, 28, 28)) {
            setZoom(savedZoom + 0.15, width / 2.0, contentViewport().top() + contentViewport().height() / 2.0);
            return true;
        }

        TreeEntry node = selectedNode >= 0 && selectedNode < nodes.size() ? nodes.get(selectedNode) : null;
        int buttonW = Math.min(170, Math.max(110, width / 5));
        int buttonX = width - buttonW - 18;
        int buttonY = height - 40;
        if (node != null && "습득 가능".equals(node.status())
                && inside(click.x(), click.y(), buttonX, buttonY, buttonW, 28)) {
            send(node.action());
            return true;
        }

        Viewport view = contentViewport();
        if (!inside(click.x(), click.y(), view.left(), view.top(), view.width(), view.height())) return false;
        int size = scaledNodeSize();
        for (int i = 0; i < nodes.size(); i++) {
            TreeEntry candidate = nodes.get(i);
            int x = screenX(view, candidate.worldX()) - size / 2;
            int y = screenY(view, candidate.worldY()) - size / 2;
            if (inside(click.x(), click.y(), x, y, size, size)) {
                selectedNode = i;
                return true;
            }
        }
        dragging = true;
        return true;
    }

    private boolean clickSkills(MouseButtonEvent click) {
        Viewport view = contentViewport();
        int columns = view.width() >= 500 ? 2 : 1;
        int cardWidth = Math.max(150,
                (view.width() - 24 - SKILL_GAP * (columns - 1)) / columns);
        for (int i = 0; i < skills.size(); i++) {
            int x = view.left() + 10 + (i % columns) * (cardWidth + SKILL_GAP);
            int y = view.top() + 9 + (i / columns) * (SKILL_CARD_HEIGHT + SKILL_GAP) - skillScroll;
            if (inside(click.x(), click.y(), x, y, cardWidth, SKILL_CARD_HEIGHT)) {
                selectedSkill = i;
                return true;
            }
        }
        if (selectedSkill < 0 || selectedSkill >= skills.size()) return false;
        SkillEntry skill = skills.get(selectedSkill);
        int gap = 8;
        int totalWidth = Math.min(width - 36, 430);
        int buttonW = (totalWidth - gap * 2) / 3;
        int startX = width - totalWidth - 18;
        int buttonY = height - 39;
        boolean learned = skill.status().equals("습득") || skill.status().startsWith("장착");
        if ("습득 가능".equals(skill.status())
                && inside(click.x(), click.y(), startX, buttonY, buttonW, 28)) {
            send(skill.unlockAction());
            return true;
        }
        if (learned && inside(click.x(), click.y(), startX + buttonW + gap, buttonY, buttonW, 28)) {
            send("role_skill_equip:" + skill.id() + ":0");
            return true;
        }
        if (learned && inside(click.x(), click.y(), startX + (buttonW + gap) * 2, buttonY, buttonW, 28)) {
            send("role_skill_equip:" + skill.id() + ":1");
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
            setZoom(savedZoom + vertical * 0.10, mouseX, mouseY);
        } else {
            skillScroll = Math.max(0, skillScroll - (int) Math.round(vertical * 42));
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
                    case DURATION -> -165;
                    case POWER -> 0;
                    case SPECIAL -> 165;
                };
                double y = -tier * 88.0;
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
        double next = clamp(requested, 0.55, 1.80);
        double anchorX = view.left() + view.width() / 2.0;
        double anchorY = view.bottom() - 52.0;
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
        return (int) Math.round(view.bottom() - 52.0 + savedPanY + y * savedZoom);
    }

    private int scaledNodeSize() { return (int) Math.round(clamp(46 * savedZoom, 28, 66)); }
    private int footerTop() { return Math.max(HEADER_HEIGHT + 100, height - FOOTER_HEIGHT); }
    private Viewport contentViewport() {
        return new Viewport(8, HEADER_HEIGHT + 6, width - 8, Math.max(HEADER_HEIGHT + 7, footerTop() - 6));
    }

    private void drawTab(GuiGraphicsExtractor g, int mx, int my,
                         int x, int y, int w, String text, boolean active) {
        boolean hovered = inside(mx, my, x, y, w, 25);
        g.fill(x, y, x + w, y + 25, active ? ACCENT : hovered ? SURFACE_2 : SURFACE);
        g.centeredText(font, text, x + w / 2, y + 8, active ? 0xFF07100F : TEXT);
    }

    private void drawSmall(GuiGraphicsExtractor g, int mx, int my,
                           int x, int y, int w, String text) {
        boolean hovered = inside(mx, my, x, y, w, 28);
        g.fill(x, y, x + w, y + 28, hovered ? SURFACE_2 : SURFACE);
        g.centeredText(font, text, x + w / 2, y + 9, hovered ? GOLD : MUTED);
    }

    private void drawActionButton(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                  boolean active, boolean hovered, String text) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1,
                hovered ? GOLD : active ? ACCENT : 0xFF35424D);
        g.fill(x, y, x + w, y + h,
                hovered ? 0xFF3C3420 : active ? SURFACE_2 : 0xFF171E25);
        g.centeredText(font, compact(text, Math.max(10, w / 7)),
                x + w / 2, y + (h - 9) / 2, active ? TEXT : MUTED);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int scroll, int maxScroll, int visible, int content) {
        if (maxScroll <= 0 || content <= visible) return;
        int track = Math.max(1, bottom - top);
        int thumb = Math.max(18, track * visible / Math.max(visible, content));
        int y = top + (track - thumb) * clamp(scroll, 0, maxScroll) / maxScroll;
        graphics.fill(x, top, x + 3, bottom, 0xFF05080B);
        graphics.fill(x, y, x + 3, y + thumb, ACCENT);
    }

    private void drawBranchIcon(GuiGraphicsExtractor g, Branch branch,
                                int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (branch) {
            case DURATION -> {
                g.fill(cx - 2, y + 2, cx + 2, y + size - 2, color);
                g.fill(cx - 7, y + 2, cx + 7, y + 6, color);
            }
            case POWER -> {
                g.fill(x + 3, cy - 3, x + size - 3, cy + 3, color);
                g.fill(cx - 3, y + 3, cx + 3, y + size - 3, color);
            }
            case SPECIAL -> {
                g.fill(cx - 3, y + 2, cx + 3, y + size - 2, color);
                g.fill(x + 3, cy - 3, x + size - 3, cy + 3, color);
                g.fill(x + 5, y + 5, x + 9, y + 9, GOLD);
            }
        }
    }

    private void drawLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            g.fill(x0 - 1, y0 - 1, x0 + 2, y0 + 2, color);
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

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private String compact(String value, int max) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        return normalized.length() <= max ? normalized
                : normalized.substring(0, Math.max(1, max - 1)) + "…";
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
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

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
}

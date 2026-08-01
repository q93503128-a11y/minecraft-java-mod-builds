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

    private static double savedZoom = 1.0;
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
        graphics.fill(0, 0, width, 72, PANEL);
        graphics.fill(0, 70, width, 72, BORDER);
        graphics.text(font, roleName + " 성장", 16, 11, TEXT, false);
        graphics.text(font, compact(summary, Math.max(24, width / 8)), 16, 28, MUTED, false);
        drawTab(graphics, mouseX, mouseY, 16, 44, 96, "성장 경로", tab == Tab.TREE);
        drawTab(graphics, mouseX, mouseY, 118, 44, 96, "기술 장착", tab == Tab.SKILLS);
        int closeX = width - 36;
        boolean hovered = inside(mouseX, mouseY, closeX, 10, 26, 25);
        graphics.fill(closeX, 10, closeX + 26, 35, hovered ? 0xFF6E3038 : SURFACE);
        graphics.centeredText(font, "×", closeX + 13, 18, hovered ? TEXT : MUTED);
        if (tab == Tab.TREE) {
            int centerX = closeX - 53;
            int plusX = centerX - 32;
            int percentX = plusX - 46;
            int minusX = percentX - 32;
            drawSmall(graphics, mouseX, mouseY, minusX, 10, 26, "−");
            graphics.centeredText(font, Math.round(savedZoom * 100) + "%", percentX + 21, 18, MUTED);
            drawSmall(graphics, mouseX, mouseY, plusX, 10, 26, "+");
            drawSmall(graphics, mouseX, mouseY, centerX, 10, 45, "중앙");
        }
    }

    private void renderTree(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Viewport view = treeViewport();
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
        for (int x = startX; x < view.right(); x += spacing) graphics.fill(x, view.top(), x + 1, view.bottom(), 0xFF101923);
        for (int y = startY; y < view.bottom(); y += spacing) graphics.fill(view.left(), y, view.right(), y + 1, 0xFF101923);
    }

    private void renderTreeConnections(GuiGraphicsExtractor graphics, Viewport view) {
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        for (TreeEntry node : nodes) {
            TreeEntry previous = previous(node);
            double x0 = previous == null ? 0 : previous.worldX();
            double y0 = previous == null ? 0 : previous.worldY();
            int color = previous == null || "습득".equals(previous.status()) ? branchColor(node.branch()) : 0xFF34434F;
            drawLine(graphics, screenX(view, x0), screenY(view, y0), screenX(view, node.worldX()), screenY(view, node.worldY()), color);
        }
        graphics.disableScissor();
    }

    private void renderTreeRoot(GuiGraphicsExtractor graphics, Viewport view) {
        int size = scaledNodeSize() - 6;
        int cx = screenX(view, 0);
        int cy = screenY(view, 0);
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        graphics.fill(cx - size / 2 - 2, cy - size / 2 - 2, cx + size / 2 + 2, cy + size / 2 + 2, GOLD);
        graphics.fill(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, SURFACE_2);
        graphics.centeredText(font, roleName.substring(0, 1), cx, cy - 4, GOLD);
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
            graphics.fill(x - 3, y - 3, x + size + 3, y + size + 3, selected ? GOLD : hovered ? border : 0xFF18242E);
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, border);
            graphics.fill(x, y, x + size, y + size, hovered || selected ? SURFACE_2 : SURFACE);
            drawBranchIcon(graphics, node.branch(), x + 7, y + 7, size - 14, border);
            if (savedZoom >= 0.82) graphics.centeredText(font, compact(node.title(), 12), cx, y + size + 5, selected ? TEXT : MUTED);
        }
        graphics.disableScissor();
    }

    private void renderTreeFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int top = Math.max(120, height - 94);
        graphics.fill(0, top, width, height, PANEL);
        graphics.fill(0, top, width, top + 2, BORDER);
        String title = "성장 노드를 선택하세요";
        String desc = "세 갈래의 효과는 장착한 모든 직업 기술에 공통 적용됩니다.";
        String status = "";
        if (selectedNode >= 0 && selectedNode < nodes.size()) {
            TreeEntry node = nodes.get(selectedNode);
            title = node.title() + " · Lv." + node.level() + " · 주화 " + node.cost();
            desc = node.description();
            status = node.status();
        }
        graphics.text(font, compact(title, Math.max(22, width / 9)), 16, top + 12, TEXT, false);
        if (!status.isBlank()) graphics.text(font, status, 16, top + 29, "습득 가능".equals(status) ? GOLD : MUTED, false);
        int buttonW = Math.min(128, Math.max(94, width / 5));
        int buttonX = width - buttonW - 16;
        int buttonY = top + 31;
        List<FormattedCharSequence> lines = font.split(Component.literal(desc), Math.max(80, buttonX - 28));
        int y = top + 49;
        for (FormattedCharSequence line : lines) {
            if (y > height - 10) break;
            graphics.text(font, line, 16, y, MUTED, false);
            y += 11;
        }
        boolean active = selectedNode >= 0 && "습득 가능".equals(nodes.get(selectedNode).status());
        boolean hovered = active && inside(mouseX, mouseY, buttonX, buttonY, buttonW, 25);
        drawActionButton(graphics, buttonX, buttonY, buttonW, active, hovered, active ? "습득" : selectedNode < 0 ? "선택 필요" : nodes.get(selectedNode).status());
    }

    private void renderSkills(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = 16;
        int right = width - 16;
        int top = 82;
        int bottom = Math.max(top + 120, height - 96);
        graphics.fill(left, top, right, bottom, 0xFF0B1219);
        int columns = width >= 520 ? 2 : 1;
        int gap = 8;
        int cardW = Math.max(150, (right - left - 20 - gap * (columns - 1)) / columns);
        int rows = Math.max(1, (skills.size() + columns - 1) / columns);
        int cardH = Math.max(55, Math.min(78, (bottom - top - 20 - gap * Math.max(0, rows - 1)) / rows));
        graphics.enableScissor(left, top, right, bottom);
        for (int i = 0; i < skills.size(); i++) {
            SkillEntry skill = skills.get(i);
            int x = left + 10 + (i % columns) * (cardW + gap);
            int y = top + 10 + (i / columns) * (cardH + gap);
            boolean hovered = inside(mouseX, mouseY, x, y, cardW, cardH);
            boolean selected = selectedSkill == i;
            int border = selected ? GOLD : hovered ? ACCENT : BORDER;
            graphics.fill(x - 1, y - 1, x + cardW + 1, y + cardH + 1, border);
            graphics.fill(x, y, x + cardW, y + cardH, selected || hovered ? SURFACE_2 : SURFACE);
            graphics.fill(x, y, x + 4, y + cardH, skillColor(i));
            graphics.text(font, compact(skill.name(), Math.max(12, cardW / 9)), x + 13, y + 10, TEXT, false);
            graphics.text(font, "Lv." + skill.level() + " · " + skill.cost() + " 주화", x + 13, y + 25, MUTED, false);
            graphics.text(font, skill.status(), x + 13, y + cardH - 17,
                    skill.status().startsWith("장착") ? ACCENT : "습득 가능".equals(skill.status()) ? GOLD : MUTED, false);
        }
        graphics.disableScissor();
        renderSkillFooter(graphics, mouseX, mouseY);
    }

    private void renderSkillFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int top = Math.max(120, height - 88);
        graphics.fill(0, top, width, height, PANEL);
        graphics.fill(0, top, width, top + 2, BORDER);
        SkillEntry skill = selectedSkill >= 0 && selectedSkill < skills.size() ? skills.get(selectedSkill) : null;
        String title = skill == null ? "기술을 선택하세요" : skill.name();
        String desc = skill == null ? "기술은 최대 두 개까지 장착하며 기본 단축키는 R과 G입니다." : skill.description();
        graphics.text(font, compact(title, Math.max(18, width / 10)), 16, top + 12, TEXT, false);
        List<FormattedCharSequence> lines = font.split(Component.literal(desc), Math.max(80, width - 360));
        int y = top + 31;
        for (FormattedCharSequence line : lines) {
            if (y > height - 10) break;
            graphics.text(font, line, 16, y, MUTED, false);
            y += 11;
        }
        int w = Math.min(104, Math.max(82, width / 7));
        int gap = 8;
        int x3 = width - 16 - w;
        int x2 = x3 - gap - w;
        int x1 = x2 - gap - w;
        boolean learned = skill != null && (skill.status().equals("습득") || skill.status().startsWith("장착"));
        boolean unlockable = skill != null && "습득 가능".equals(skill.status());
        drawActionButton(graphics, x1, top + 30, w, unlockable, unlockable && inside(mouseX, mouseY, x1, top + 30, w, 25), unlockable ? "기술 습득" : skill == null ? "선택 필요" : skill.status());
        drawActionButton(graphics, x2, top + 30, w, learned, learned && inside(mouseX, mouseY, x2, top + 30, w, 25), "R에 장착");
        drawActionButton(graphics, x3, top + 30, w, learned, learned && inside(mouseX, mouseY, x3, top + 30, w, 25), "G에 장착");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (inside(click.x(), click.y(), width - 36, 10, 26, 25)) { onClose(); return true; }
        if (inside(click.x(), click.y(), 16, 44, 96, 24)) { tab = Tab.TREE; return true; }
        if (inside(click.x(), click.y(), 118, 44, 96, 24)) { tab = Tab.SKILLS; return true; }
        if (tab == Tab.TREE) return clickTree(click) || super.mouseClicked(click, doubled);
        return clickSkills(click) || super.mouseClicked(click, doubled);
    }

    private boolean clickTree(MouseButtonEvent click) {
        int closeX = width - 36;
        int centerX = closeX - 53;
        int plusX = centerX - 32;
        int percentX = plusX - 46;
        int minusX = percentX - 32;
        if (inside(click.x(), click.y(), centerX, 10, 45, 25)) { savedZoom = 1.0; savedPanX = 0; savedPanY = 0; return true; }
        if (inside(click.x(), click.y(), minusX, 10, 26, 25)) { setZoom(savedZoom - 0.15, width / 2.0, height / 2.0); return true; }
        if (inside(click.x(), click.y(), plusX, 10, 26, 25)) { setZoom(savedZoom + 0.15, width / 2.0, height / 2.0); return true; }
        int top = Math.max(120, height - 94);
        int buttonW = Math.min(128, Math.max(94, width / 5));
        int buttonX = width - buttonW - 16;
        if (selectedNode >= 0 && "습득 가능".equals(nodes.get(selectedNode).status())
                && inside(click.x(), click.y(), buttonX, top + 31, buttonW, 25)) {
            send(nodes.get(selectedNode).action()); return true;
        }
        Viewport view = treeViewport();
        if (!inside(click.x(), click.y(), view.left(), view.top(), view.width(), view.height())) return false;
        int size = scaledNodeSize();
        for (int i = 0; i < nodes.size(); i++) {
            TreeEntry node = nodes.get(i);
            int x = screenX(view, node.worldX()) - size / 2;
            int y = screenY(view, node.worldY()) - size / 2;
            if (inside(click.x(), click.y(), x, y, size, size)) { selectedNode = i; return true; }
        }
        dragging = true;
        return true;
    }

    private boolean clickSkills(MouseButtonEvent click) {
        int left = 16, right = width - 16, top = 82, bottom = Math.max(top + 120, height - 96);
        int columns = width >= 520 ? 2 : 1;
        int gap = 8;
        int cardW = Math.max(150, (right - left - 20 - gap * (columns - 1)) / columns);
        int rows = Math.max(1, (skills.size() + columns - 1) / columns);
        int cardH = Math.max(55, Math.min(78, (bottom - top - 20 - gap * Math.max(0, rows - 1)) / rows));
        for (int i = 0; i < skills.size(); i++) {
            int x = left + 10 + (i % columns) * (cardW + gap);
            int y = top + 10 + (i / columns) * (cardH + gap);
            if (inside(click.x(), click.y(), x, y, cardW, cardH)) { selectedSkill = i; return true; }
        }
        if (selectedSkill < 0) return false;
        SkillEntry skill = skills.get(selectedSkill);
        int footerTop = Math.max(120, height - 88);
        int w = Math.min(104, Math.max(82, width / 7));
        int x3 = width - 16 - w;
        int x2 = x3 - 8 - w;
        int x1 = x2 - 8 - w;
        boolean learned = skill.status().equals("습득") || skill.status().startsWith("장착");
        if ("습득 가능".equals(skill.status()) && inside(click.x(), click.y(), x1, footerTop + 30, w, 25)) { send(skill.unlockAction()); return true; }
        if (learned && inside(click.x(), click.y(), x2, footerTop + 30, w, 25)) { send("role_skill_equip:" + skill.id() + ":0"); return true; }
        if (learned && inside(click.x(), click.y(), x3, footerTop + 30, w, 25)) { send("role_skill_equip:" + skill.id() + ":1"); return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (tab == Tab.TREE && dragging && event.button() == 0) {
            savedPanX = clamp(savedPanX + dragX, -900, 900);
            savedPanY = clamp(savedPanY + dragY, -900, 900);
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
        Viewport view = treeViewport();
        if (tab == Tab.TREE && inside(mouseX, mouseY, view.left(), view.top(), view.width(), view.height())) {
            setZoom(savedZoom + vertical * 0.10, mouseX, mouseY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void parsePayload() {
        String[] header = payload.body().split("\\|", -1);
        if (header.length >= 2) { roleId = header[0]; roleName = header[1]; }
        summary = header.length >= 3 ? header[2] : payload.body();
        String[] actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        String[] labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
        int count = Math.min(actions.length, labels.length);
        for (int i = 0; i < count; i++) {
            String[] p = labels[i].split("\\|", -1);
            if (p.length >= 9 && "node".equals(p[0])) {
                Branch branch = Branch.parse(p[2]);
                int tier = parseInt(p[3], 1);
                double x = switch (branch) { case DURATION -> -150; case POWER -> 0; case SPECIAL -> 150; };
                double y = -tier * 115.0;
                nodes.add(new TreeEntry(actions[i], p[1], branch, tier, p[4], p[5], parseInt(p[6], 1), parseInt(p[7], 0), p[8], x, y));
            } else if (p.length >= 8 && "skill".equals(p[0])) {
                skills.add(new SkillEntry(actions[i], p[1], p[2], p[3], parseInt(p[4], 1), parseInt(p[5], 0), p[6], parseInt(p[7], -1)));
            }
        }
    }

    private TreeEntry previous(TreeEntry node) {
        if (node.tier() <= 1) return null;
        for (TreeEntry candidate : nodes) if (candidate.branch() == node.branch() && candidate.tier() == node.tier() - 1) return candidate;
        return null;
    }

    private void setZoom(double requested, double mouseX, double mouseY) {
        Viewport view = treeViewport();
        double old = savedZoom;
        double next = clamp(requested, 0.60, 1.70);
        double anchorX = view.left() + view.width() / 2.0;
        double anchorY = view.bottom() - 58.0;
        double worldX = (mouseX - anchorX - savedPanX) / old;
        double worldY = (mouseY - anchorY - savedPanY) / old;
        savedZoom = next;
        savedPanX = clamp(mouseX - anchorX - worldX * next, -900, 900);
        savedPanY = clamp(mouseY - anchorY - worldY * next, -900, 900);
    }

    private int screenX(Viewport view, double x) { return (int) Math.round(view.left() + view.width() / 2.0 + savedPanX + x * savedZoom); }
    private int screenY(Viewport view, double y) { return (int) Math.round(view.bottom() - 58.0 + savedPanY + y * savedZoom); }
    private int scaledNodeSize() { return (int) Math.round(clamp(44 * savedZoom, 28, 62)); }
    private Viewport treeViewport() { return new Viewport(0, 72, width, Math.max(73, height - 94)); }

    private void drawTab(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String text, boolean active) {
        boolean h = inside(mx, my, x, y, w, 24);
        g.fill(x, y, x + w, y + 24, active ? ACCENT : h ? SURFACE_2 : SURFACE);
        g.centeredText(font, text, x + w / 2, y + 8, active ? 0xFF07100F : TEXT);
    }

    private void drawSmall(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, String text) {
        boolean h = inside(mx, my, x, y, w, 25);
        g.fill(x, y, x + w, y + 25, h ? SURFACE_2 : SURFACE);
        g.centeredText(font, text, x + w / 2, y + 8, h ? GOLD : MUTED);
    }

    private void drawActionButton(GuiGraphicsExtractor g, int x, int y, int w, boolean active, boolean hovered, String text) {
        g.fill(x - 1, y - 1, x + w + 1, y + 26, hovered ? GOLD : active ? ACCENT : 0xFF35424D);
        g.fill(x, y, x + w, y + 25, hovered ? 0xFF3C3420 : active ? SURFACE_2 : 0xFF171E25);
        g.centeredText(font, compact(text, Math.max(9, w / 7)), x + w / 2, y + 8, active ? TEXT : MUTED);
    }

    private void drawBranchIcon(GuiGraphicsExtractor g, Branch branch, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (branch) {
            case DURATION -> { g.fill(cx - 2, y + 2, cx + 2, y + size - 2, color); g.fill(cx - 7, y + 2, cx + 7, y + 6, color); }
            case POWER -> { g.fill(x + 3, cy - 3, x + size - 3, cy + 3, color); g.fill(cx - 3, y + 3, cx + 3, y + size - 3, color); }
            case SPECIAL -> { g.fill(cx - 3, y + 2, cx + 3, y + size - 2, color); g.fill(x + 3, cy - 3, x + size - 3, cy + 3, color); g.fill(x + 5, y + 5, x + 9, y + 9, GOLD); }
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

    private int branchColor(Branch branch) { return switch (branch) { case DURATION -> BLUE; case POWER -> RED; case SPECIAL -> PURPLE; }; }
    private int skillColor(int index) { return switch (index % 4) { case 0 -> ACCENT; case 1 -> BLUE; case 2 -> PURPLE; default -> GOLD; }; }
    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; } }
    private String compact(String value, int max) { String n = value.replace('\n', ' '); return n.length() <= max ? n : n.substring(0, Math.max(1, max - 1)) + "…"; }
    private void send(String action) { ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action)); }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private enum Tab { TREE, SKILLS }
    private enum Branch {
        DURATION, POWER, SPECIAL;
        static Branch parse(String value) {
            if (value == null) return POWER;
            return switch (value.toLowerCase()) { case "duration" -> DURATION; case "special" -> SPECIAL; default -> POWER; };
        }
    }
    private record Viewport(int left, int top, int right, int bottom) { int width() { return right - left; } int height() { return bottom - top; } }
    private record TreeEntry(String action, String id, Branch branch, int tier, String title, String description, int level, int cost, String status, double worldX, double worldY) {}
    private record SkillEntry(String unlockAction, String id, String name, String description, int level, int cost, String status, int slot) {}
}

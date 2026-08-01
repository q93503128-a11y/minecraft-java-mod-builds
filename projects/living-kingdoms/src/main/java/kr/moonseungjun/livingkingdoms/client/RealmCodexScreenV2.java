package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.network.UnlockSkillPayload;
import kr.moonseungjun.livingkingdoms.skill.SkillTreeCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Four-page RPG codex using a verified CC0 Kenney UI sheet and server-authoritative data. */
public final class RealmCodexScreenV2 extends Screen {
    private static final Identifier UI_SHEET = Identifier.fromNamespaceAndPath(
            LivingKingdoms.MOD_ID, "textures/gui/kenney/uipack_rpg_sheet.png"
    );
    private static final int SHEET_SIZE = 512;
    private static final List<PageTab> PAGES = List.of(
            new PageTab("overview", "인물"),
            new PageTab("equipment", "장비·법률"),
            new PageTab("map", "세계 지도"),
            new PageTab("skills", "기술 성장")
    );
    private static final List<PageTab> BRANCHES = List.of(
            new PageTab("combat", "전투"),
            new PageTab("exploration", "탐험"),
            new PageTab("livelihood", "생활"),
            new PageTab("society", "사회"),
            new PageTab("arcana", "마법")
    );

    private final Map<String, String> data;
    private String page;
    private String skillBranch = "combat";

    public RealmCodexScreenV2(String requestedPage, String snapshot) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("왕국 수첩"));
        page = normalizePage(requestedPage);
        data = parse(snapshot);
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            invisible(layout.pageTab(i), () -> page = tab.id());
        }
        invisible(layout.closeButton(), this::onClose);
        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            invisible(layout.branchTab(i), () -> {
                if ("skills".equals(page)) skillBranch = branch.id();
            });
        }
        Map<String, Integer> branchIndexes = new LinkedHashMap<>();
        for (SkillTreeCatalog.SkillNode node : SkillTreeCatalog.nodes().values()) {
            int index = branchIndexes.getOrDefault(node.branch(), 0);
            branchIndexes.put(node.branch(), index + 1);
            invisible(layout.skillNode(index), () -> {
                if ("skills".equals(page) && skillBranch.equals(node.branch())) {
                    ClientPacketDistributor.sendToServer(new UnlockSkillPayload(node.id()));
                }
            });
        }
    }

    private void invisible(Rect rect, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> action.run())
                .bounds(rect.x(), rect.y(), rect.w(), rect.h()).build());
        button.setAlpha(0.0F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    boolean allRequiredControlsFit() {
        Layout l = layout();
        return l.left() >= 2 && l.top() >= 2 && l.right() <= width - 2 && l.bottom() <= height - 2
                && l.closeButton().right() <= l.right() && l.content().bottom() <= l.bottom();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        graphics.fill(0, 0, width, height, 0xC80A0D12);
        sprite(graphics, l.left() - 5, l.top() - 5, l.panelW() + 10, l.panelH() + 10,
                100, 376, 100, 100);
        graphics.fill(l.left() + 8, l.top() + 8, l.right() - 8, l.top() + 39, 0xE536281F);
        graphics.fill(l.left() + 10, l.top() + 39, l.right() - 10, l.top() + 41, 0xFFCDAA67);
        graphics.text(font, Component.literal("LIVING KINGDOMS"), l.left() + 16, l.top() + 10, 0xFFD9B870);
        graphics.text(font, Component.literal("왕국 수첩"), l.left() + 16, l.top() + 23, 0xFFFFF1CB);
        badge(graphics, l.left() + 108, l.top() + 14, shortText(value("affiliation"), 27), 0xFF46614D);
        squareButton(graphics, l.closeButton(), "×", inside(mouseX, mouseY, l.closeButton()));

        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            texturedButton(graphics, l.pageTab(i), tab.label(), page.equals(tab.id()));
        }

        switch (page) {
            case "equipment" -> drawEquipment(graphics, l);
            case "map" -> drawMap(graphics, l);
            case "skills" -> drawSkills(graphics, l, mouseX, mouseY);
            default -> drawOverview(graphics, l);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawOverview(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        int gap = 8;
        int leftWidth = Math.max(150, c.w() * 55 / 100);
        Rect identity = new Rect(c.x(), c.y(), leftWidth, c.h());
        Rect state = new Rect(c.x() + leftWidth + gap, c.y(), c.w() - leftWidth - gap, c.h());
        card(g, identity, "신분과 소속");
        card(g, state, "종족 특성과 현재 상태");

        int y = identity.y() + 27;
        g.text(font, Component.literal(shortText(value("player"), 28)), identity.x() + 10, y, 0xFF332319); y += 18;
        y = row(g, identity, y, "종족", value("species"));
        y = row(g, identity, y, "기본 소속", value("affiliation"));
        y = row(g, identity, y, "신분", value("citizenship"));
        y = row(g, identity, y, "배경", value("background"));
        y = row(g, identity, y, "거주지", value("residence"));
        y = row(g, identity, y, "현재 지역", value("region"));
        row(g, identity, y, "좌표", value("position"));

        y = state.y() + 27;
        g.text(font, Component.literal(shortText(value("trait_title"), 28)), state.x() + 10, y, 0xFF3F664D); y += 14;
        for (String line : wrap(value("trait_description"), Math.max(15, state.w() / 7))) {
            g.text(font, Component.literal(line), state.x() + 10, y, 0xFF5C4635);
            y += 11;
            if (y > state.y() + 62) break;
        }
        y = Math.max(y + 4, state.y() + 72);
        progress(g, state.x() + 10, y, state.w() - 20, "체력", value("health"), ratio(value("health"))); y += 27;
        progress(g, state.x() + 10, y, state.w() - 20, "허기", value("food"), ratio(value("food"))); y += 27;
        progress(g, state.x() + 10, y, state.w() - 20, "수배", value("wanted") + " / 100", parseInt("wanted") / 100.0F); y += 28;
        badge(g, state.x() + 10, y, "LV " + value("level"), 0xFF694A2E);
        badge(g, state.x() + 63, y, "기술 점수 " + value("skill_points"), 0xFF315A68);
    }

    private void drawEquipment(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        int upperHeight = Math.max(86, c.h() / 2);
        Rect equipment = new Rect(c.x(), c.y(), c.w(), upperHeight);
        Rect law = new Rect(c.x(), c.y() + upperHeight + 8, c.w(), c.h() - upperHeight - 8);
        card(g, equipment, "장비 기록");
        card(g, law, "법적 상태와 관할");
        String[][] slots = {
                {"주무기", value("mainhand")}, {"보조", value("offhand")},
                {"머리", value("head")}, {"몸통", value("chest")},
                {"다리", value("legs")}, {"발", value("feet")}
        };
        int columnWidth = (equipment.w() - 28) / 3;
        for (int i = 0; i < slots.length; i++) {
            int column = i % 3;
            int row = i / 3;
            Rect slot = new Rect(equipment.x() + 10 + column * (columnWidth + 4),
                    equipment.y() + 27 + row * 34, columnWidth, 29);
            inset(g, slot);
            g.text(font, Component.literal(slots[i][0]), slot.x() + 6, slot.y() + 4, 0xFF816246);
            g.text(font, Component.literal(shortText(slots[i][1], Math.max(8, slot.w() / 6))),
                    slot.x() + 6, slot.y() + 16, 0xFF30271F);
        }
        int y = law.y() + 27;
        y = dualRow(g, law, y, "수배도", value("wanted") + " / 100", "저항 단계", value("resistance"));
        y = dualRow(g, law, y, "관할", value("jurisdiction"), "체포 진행", value("arrest"));
        for (String line : wrap("범죄는 즉시 순간이동 처벌이 아니라 경비의 추격·제압·호송 절차로 처리됩니다.",
                Math.max(25, law.w() / 7))) {
            g.text(font, Component.literal(line), law.x() + 10, y, 0xFF5D4633);
            y += 11;
        }
    }

    private void drawMap(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        inset(g, c);
        int mapLeft = c.x() + 11;
        int mapTop = c.y() + 11;
        int mapWidth = c.w() - 22;
        int mapHeight = c.h() - 39;
        g.fill(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight, 0xFFD8C99F);
        terrain(g, mapLeft, mapTop, mapWidth, mapHeight);

        int[] xs = {parseInt("erden_x"), parseInt("silvana_x"), parseInt("kardum_x"),
                parseInt("player_x"), parseInt("home_x")};
        int[] zs = {parseInt("erden_z"), parseInt("silvana_z"), parseInt("kardum_z"),
                parseInt("player_z"), parseInt("home_z")};
        Bounds bounds = bounds(xs, zs);
        int erdenX = project(parseInt("erden_x"), bounds.minX(), bounds.maxX(), mapLeft, mapWidth);
        int erdenY = project(parseInt("erden_z"), bounds.minZ(), bounds.maxZ(), mapTop, mapHeight);
        int silvanaX = project(parseInt("silvana_x"), bounds.minX(), bounds.maxX(), mapLeft, mapWidth);
        int silvanaY = project(parseInt("silvana_z"), bounds.minZ(), bounds.maxZ(), mapTop, mapHeight);
        int kardumX = project(parseInt("kardum_x"), bounds.minX(), bounds.maxX(), mapLeft, mapWidth);
        int kardumY = project(parseInt("kardum_z"), bounds.minZ(), bounds.maxZ(), mapTop, mapHeight);
        route(g, kardumX, kardumY, erdenX, erdenY);
        route(g, erdenX, erdenY, silvanaX, silvanaY);
        settlement(g, kardumX, kardumY, "카르둠 연맹", 0xFF57514B);
        settlement(g, erdenX, erdenY, "에르덴 왕국", 0xFF536D3A);
        settlement(g, silvanaX, silvanaY, "실바나 수림", 0xFF2E6A47);

        int homeX = project(parseInt("home_x"), bounds.minX(), bounds.maxX(), mapLeft, mapWidth);
        int homeY = project(parseInt("home_z"), bounds.minZ(), bounds.maxZ(), mapTop, mapHeight);
        int playerX = project(parseInt("player_x"), bounds.minX(), bounds.maxX(), mapLeft, mapWidth);
        int playerY = project(parseInt("player_z"), bounds.minZ(), bounds.maxZ(), mapTop, mapHeight);
        marker(g, homeX, homeY, 0xFF2F6588, 4);
        marker(g, playerX, playerY, 0xFFD45138, 5);

        int legendY = c.bottom() - 22;
        g.fill(c.x() + 8, legendY - 3, c.right() - 8, c.bottom() - 6, 0xDDF7EACB);
        g.text(font, Component.literal("빨강 현재 위치  ·  파랑 거주지  ·  갈색 교역로"),
                c.x() + 14, legendY, 0xFF473529);
        g.text(font, Component.literal(shortText(value("region") + " · " + value("position"), 58)),
                c.x() + 14, legendY + 11, 0xFF624A34);
    }

    private void drawSkills(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c = l.content();
        badge(g, c.x(), c.y(), "남은 기술 점수 " + value("skill_points"), 0xFF315A68);
        g.text(font, Component.literal(shortText(value("trait_title") + " · " + value("trait_description"), 68)),
                c.x() + 128, c.y() + 5, 0xFF5C4635);
        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            texturedButton(g, l.branchTab(i), branch.label(), skillBranch.equals(branch.id()));
        }

        List<SkillTreeCatalog.SkillNode> nodes = SkillTreeCatalog.nodes().values().stream()
                .filter(node -> node.branch().equals(skillBranch)).toList();
        Set<String> unlocked = unlockedSkills();
        String species = value("species_id");
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            Rect r = l.skillNode(i);
            boolean owned = unlocked.contains(node.id());
            boolean prerequisite = node.prerequisites().stream().allMatch(unlocked::contains);
            boolean affordable = parseInt("skill_points") >= SkillTreeCatalog.effectiveCost(node, species);
            sprite(g, r.x(), r.y(), r.w(), r.h(), owned ? 0 : 0, owned ? 188 : 282, 190, 49);
            if (!owned && (!prerequisite || !affordable)) g.fill(r.x() + 4, r.y() + 4, r.right() - 4, r.bottom() - 4, 0x884B4945);
            int cost = SkillTreeCatalog.effectiveCost(node, species);
            g.text(font, Component.literal((owned ? "◆ " : "◇ ") + node.title()), r.x() + 8, r.y() + 7,
                    owned ? 0xFFF4EDC8 : 0xFF493326);
            badge(g, r.right() - 45, r.y() + 5, owned ? "해금" : cost + "점", owned ? 0xFF4B7252 : 0xFF6E4B2E);
            List<String> lines = wrap(node.description(), Math.max(14, (r.w() - 16) / 7));
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                g.text(font, Component.literal(lines.get(line)), r.x() + 8, r.y() + 22 + line * 11,
                        owned ? 0xFFFFF1D0 : 0xFF66503D);
            }
            if (inside(mouseX, mouseY, r) && !owned) {
                g.fill(r.x() + 5, r.bottom() - 5, r.right() - 5, r.bottom() - 3,
                        prerequisite && affordable ? 0xFFC99545 : 0xFF8B8175);
            }
        }
        g.text(font, Component.literal("경험치 레벨 5마다 기술 점수 1점을 추가로 얻습니다."),
                c.x(), c.bottom() - 10, 0xFF6A5039);
    }

    private void sprite(GuiGraphicsExtractor g, int x, int y, int w, int h,
                        int u, int v, int sourceW, int sourceH) {
        g.blit(RenderPipelines.GUI_TEXTURED, UI_SHEET, x, y, u, v, w, h,
                sourceW, sourceH, SHEET_SIZE, SHEET_SIZE);
    }

    private void texturedButton(GuiGraphicsExtractor g, Rect r, String text, boolean active) {
        sprite(g, r.x(), r.y(), r.w(), r.h(), 0, active ? 188 : 49, 190, 49);
        g.centeredText(font, Component.literal(text), r.x() + r.w() / 2,
                r.y() + Math.max(5, (r.h() - 8) / 2), 0xFFFFEBC0);
    }

    private void squareButton(GuiGraphicsExtractor g, Rect r, String text, boolean hovered) {
        sprite(g, r.x(), r.y(), r.w(), r.h(), 293, hovered ? 392 : 343, 45, hovered ? 45 : 49);
        g.centeredText(font, Component.literal(text), r.x() + r.w() / 2, r.y() + 7, 0xFFFFEBC0);
    }

    private void card(GuiGraphicsExtractor g, Rect r, String title) {
        sprite(g, r.x(), r.y(), r.w(), r.h(), 190, 200, 93, 94);
        g.fill(r.x() + 8, r.y() + 20, r.right() - 8, r.y() + 22, 0xFFC7A064);
        g.text(font, Component.literal(title), r.x() + 10, r.y() + 8, 0xFF493020);
    }

    private void inset(GuiGraphicsExtractor g, Rect r) {
        sprite(g, r.x(), r.y(), r.w(), r.h(), 200, 294, 93, 94);
    }

    private int row(GuiGraphicsExtractor g, Rect card, int y, String label, String value) {
        g.text(font, Component.literal(label), card.x() + 10, y, 0xFF806247);
        g.text(font, Component.literal(shortText(value, Math.max(8, (card.w() - 95) / 6))),
                card.x() + 82, y, 0xFF332A22);
        return y + 15;
    }

    private int dualRow(GuiGraphicsExtractor g, Rect card, int y,
                        String leftLabel, String leftValue, String rightLabel, String rightValue) {
        int half = card.w() / 2;
        g.text(font, Component.literal(leftLabel), card.x() + 10, y, 0xFF806247);
        g.text(font, Component.literal(shortText(leftValue, 16)), card.x() + 73, y, 0xFF332A22);
        g.text(font, Component.literal(rightLabel), card.x() + half, y, 0xFF806247);
        g.text(font, Component.literal(shortText(rightValue, 16)), card.x() + half + 70, y, 0xFF332A22);
        return y + 17;
    }

    private void badge(GuiGraphicsExtractor g, int x, int y, String text, int color) {
        int w = Math.min(150, font.width(text) + 12);
        g.fill(x, y, x + w, y + 17, 0xFF2A211B);
        g.fill(x + 2, y + 2, x + w - 2, y + 15, color);
        g.text(font, Component.literal(shortText(text, Math.max(8, w / 6))), x + 6, y + 5, 0xFFFFEDC5);
    }

    private void progress(GuiGraphicsExtractor g, int x, int y, int width,
                          String label, String text, float ratio) {
        g.text(font, Component.literal(label), x, y, 0xFF73543B);
        g.text(font, Component.literal(text), x + width - Math.min(width / 2, font.width(text)), y, 0xFF3A2C23);
        int barY = y + 12;
        sprite(g, x, barY - 5, width, 18, 372, 330, 9, 18);
        g.fill(x + 3, barY + 1, x + 3 + Math.round((width - 6) * clamp(ratio)), barY + 7, 0xFF5B8A63);
    }

    private void terrain(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        for (int dy = 0; dy < h; dy += 4) {
            int wobble = (int) Math.round(Math.sin(dy * 0.15) * 7.0);
            int margin = 8 + Math.abs(h / 2 - dy) / 10;
            int color = dy < h / 3 ? 0xFFA2B878 : dy > h * 2 / 3 ? 0xFFB8AF72 : 0xFFABB97A;
            g.fill(x + margin + wobble, y + dy, x + w - margin + wobble, y + dy + 4, color);
        }
        for (int i = 0; i < 7; i++) {
            int mx = x + 20 + i * 11;
            int my = y + h / 2 - 20 + (i % 3) * 12;
            g.fill(mx - 4, my + 4, mx + 5, my + 8, 0xFF776F64);
            g.fill(mx - 1, my, mx + 2, my + 4, 0xFFE1D9C8);
        }
        for (int i = 0; i < 9; i++) {
            int fx = x + w - 76 + (i % 3) * 13;
            int fy = y + h / 2 - 23 + (i / 3) * 14;
            g.fill(fx - 3, fy - 4, fx + 4, fy + 3, 0xFF42734B);
            g.fill(fx - 1, fy + 3, fx + 2, fy + 7, 0xFF6D5237);
        }
    }

    private void route(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            g.fill(x - 1, y - 1, x + 2, y + 2, 0xFF865E3A);
        }
    }

    private void settlement(GuiGraphicsExtractor g, int x, int y, String title, int color) {
        marker(g, x, y, color, 5);
        g.centeredText(font, Component.literal(title), x, y - 15, 0xFF35291F);
    }

    private void marker(GuiGraphicsExtractor g, int x, int y, int color, int radius) {
        g.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, 0xFFF6E9C8);
        g.fill(x - radius + 2, y - radius + 2, x + radius - 1, y + radius - 1, color);
    }

    private Layout layout() {
        int panelWidth = Math.min(760, Math.max(300, width - 12));
        int panelHeight = Math.min(430, Math.max(220, height - 10));
        panelWidth = Math.min(panelWidth, width - 6);
        panelHeight = Math.min(panelHeight, height - 6);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        boolean compact = panelWidth < 540;
        int navigationWidth = compact ? 0 : 104;
        int contentX = compact ? left + 10 : left + navigationWidth + 14;
        int contentY = compact ? top + 72 : top + 48;
        int contentWidth = compact ? panelWidth - 20 : panelWidth - navigationWidth - 24;
        int contentHeight = panelHeight - (contentY - top) - 10;
        return new Layout(left, top, panelWidth, panelHeight, compact, navigationWidth,
                new Rect(contentX, contentY, contentWidth, contentHeight));
    }

    private Set<String> unlockedSkills() {
        Set<String> result = new LinkedHashSet<>();
        String raw = value("unlocked_skills");
        if (raw.isBlank() || "-".equals(raw)) return result;
        for (String id : raw.split(",")) if (!id.isBlank()) result.add(id);
        return result;
    }

    private String value(String key) {
        return data.getOrDefault(key, "-");
    }

    private int parseInt(String key) {
        try {
            return Integer.parseInt(value(key));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private float ratio(String raw) {
        String[] parts = raw.split("/");
        if (parts.length != 2) return 0.0F;
        try {
            return clamp(Float.parseFloat(parts[0].trim()) / Math.max(1.0F, Float.parseFloat(parts[1].trim())));
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    private static Bounds bounds(int[] xs, int[] zs) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int value : xs) { minX = Math.min(minX, value); maxX = Math.max(maxX, value); }
        for (int value : zs) { minZ = Math.min(minZ, value); maxZ = Math.max(maxZ, value); }
        if (maxX - minX < 600) { minX -= 300; maxX += 300; }
        else { minX -= 180; maxX += 180; }
        if (maxZ - minZ < 600) { minZ -= 300; maxZ += 300; }
        else { minZ -= 180; maxZ += 180; }
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    private static int project(int value, int min, int max, int start, int size) {
        double ratio = (value - min) / (double) Math.max(1, max - min);
        return start + 12 + (int) Math.round(clamp((float) ratio) * (size - 24));
    }

    private static String normalizePage(String requested) {
        return switch (requested) {
            case "equipment", "map", "skills" -> requested;
            default -> "overview";
        };
    }

    private static boolean inside(int x, int y, Rect r) {
        return x >= r.x() && y >= r.y() && x < r.right() && y < r.bottom();
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String shortText(String value, int maximum) {
        if (value == null || value.isBlank()) return "-";
        if (value.length() <= maximum) return value;
        return value.substring(0, Math.max(1, maximum - 1)) + "…";
    }

    private static List<String> wrap(String value, int maximum) {
        List<String> lines = new ArrayList<>();
        String remaining = value == null ? "-" : value.trim();
        while (!remaining.isEmpty()) {
            int end = Math.min(maximum, remaining.length());
            if (end < remaining.length()) {
                int space = remaining.lastIndexOf(' ', end);
                if (space > maximum / 2) end = space;
            }
            lines.add(remaining.substring(0, end).trim());
            remaining = remaining.substring(end).trim();
        }
        if (lines.isEmpty()) lines.add("-");
        return lines;
    }

    private static Map<String, String> parse(String snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        if (snapshot == null) return values;
        for (String line : snapshot.split("\\n")) {
            int tab = line.indexOf('\t');
            if (tab > 0) values.put(line.substring(0, tab), line.substring(tab + 1));
        }
        return values;
    }

    private record PageTab(String id, String label) {}
    private record Bounds(int minX, int maxX, int minZ, int maxZ) {}
    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }

    private record Layout(int left, int top, int panelW, int panelH,
                          boolean compact, int navW, Rect content) {
        int right() { return left + panelW; }
        int bottom() { return top + panelH; }
        Rect closeButton() { return new Rect(right() - 35, top + 9, 25, 23); }
        Rect pageTab(int index) {
            if (compact) {
                int available = panelW - 20;
                int w = available / PAGES.size();
                return new Rect(left + 10 + index * w, top + 44, w - 3, 22);
            }
            return new Rect(left + 9, top + 49 + index * 31, navW - 18, 25);
        }
        Rect branchTab(int index) {
            int gap = 4;
            int w = (content.w() - gap * (BRANCHES.size() - 1)) / BRANCHES.size();
            return new Rect(content.x() + index * (w + gap), content.y() + 23, w, 22);
        }
        Rect skillNode(int index) {
            int y = content.y() + 54;
            int availableHeight = content.h() - 70;
            if (content.w() >= 500) {
                int gap = 14;
                int w = (content.w() - gap * 2) / 3;
                return new Rect(content.x() + index * (w + gap), y, w, Math.min(80, availableHeight));
            }
            int gap = 6;
            int h = Math.max(43, (availableHeight - gap * 2) / 3);
            return new Rect(content.x(), y + index * (h + gap), content.w(), h);
        }
    }
}

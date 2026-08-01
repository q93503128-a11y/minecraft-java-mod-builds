package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.UnlockSkillPayload;
import kr.moonseungjun.livingkingdoms.skill.MasteryProgressionSavedData;
import kr.moonseungjun.livingkingdoms.skill.SkillTreeCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compact, bounded codex with an interactive atlas and separated mastery/tree regions. */
public final class RealmCodexScreenV4 extends Screen {
    private static final int BG = 0xF0141820;
    private static final int PANEL = 0xE91D232C;
    private static final int PANEL_ALT = 0xE9252C36;
    private static final int EDGE = 0xFF806B47;
    private static final int GOLD = 0xFFE0BF72;
    private static final int TEXT = 0xFFF0E7D3;
    private static final int MUTED = 0xFFBFB49D;
    private static final int WORLD_MIN = -16_000;
    private static final int WORLD_MAX = 16_000;

    private static final List<PageTab> PAGES = List.of(
            new PageTab("overview", "인물", Items.PLAYER_HEAD),
            new PageTab("equipment", "장비·법률", Items.IRON_CHESTPLATE),
            new PageTab("map", "지도", Items.FILLED_MAP),
            new PageTab("skills", "성장", Items.ENCHANTED_BOOK)
    );
    private static final List<PageTab> BRANCHES = List.of(
            new PageTab("combat", "전투", Items.IRON_SWORD),
            new PageTab("exploration", "탐험", Items.COMPASS),
            new PageTab("livelihood", "생활", Items.IRON_HOE),
            new PageTab("society", "사회", Items.EMERALD),
            new PageTab("arcana", "마법", Items.AMETHYST_SHARD)
    );
    private static final String[] MASTERY = {
            MasteryProgressionSavedData.COMBAT,
            MasteryProgressionSavedData.DEFENSE,
            MasteryProgressionSavedData.MINING,
            MasteryProgressionSavedData.LOGGING,
            MasteryProgressionSavedData.FARMING,
            MasteryProgressionSavedData.GATHERING,
            MasteryProgressionSavedData.EXPLORATION
    };
    private static final List<Region> REGIONS = List.of(
            new Region("에르덴", "erden_x", "erden_z", 0, 0, 0xFFD8B86A),
            new Region("실바나", "silvana_x", "silvana_z", -9_000, -1_500, 0xFF70A674),
            new Region("카르둠", "kardum_x", "kardum_z", -2_500, -9_000, 0xFF9AA2AA),
            new Region("붉은 초원", null, null, 9_500, -1_000, 0xFFC87955),
            new Region("벨라스", null, null, 1_500, 7_500, 0xFF72A7C7),
            new Region("사하르", null, null, 9_000, 9_000, 0xFFD8C16F)
    );
    private static final int[][] LINKS = {{0, 1}, {0, 2}, {0, 3}, {0, 4}, {3, 5}};

    private final Map<String, String> data;
    private String page;
    private String branch = "combat";
    private float mapZoom = 1.0F;
    private float mapPanX;
    private float mapPanY;
    private Rect mapViewport = Rect.EMPTY;

    public RealmCodexScreenV4(String requestedPage, String snapshot) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("왕국 기록부"));
        page = normalizePage(requestedPage);
        data = parse(snapshot);
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        invisible(layout.close(), this::onClose);
        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            invisible(layout.tab(i), () -> {
                page = tab.id();
                rebuild();
            });
        }
        if (!"skills".equals(page)) return;
        GrowthLayout growth = growthLayout(layout.content());
        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab tab = BRANCHES.get(i);
            invisible(growth.branch(i), () -> {
                branch = tab.id();
                rebuild();
            });
        }
        List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            invisible(growth.node(i, nodes.size()), () ->
                    ClientPacketDistributor.sendToServer(new UnlockSkillPayload(node.id())));
        }
    }

    private void invisible(Rect rect, Runnable action) {
        if (rect.w() < 2 || rect.h() < 2) return;
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> action.run())
                .bounds(rect.x(), rect.y(), rect.w(), rect.h()).build());
        button.setAlpha(0.0F);
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    boolean handleMapDrag(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!"map".equals(page) || button != 0 || !mapViewport.contains(mouseX, mouseY)) return false;
        mapPanX += (float) dx;
        mapPanY += (float) dy;
        clampPan();
        return true;
    }

    boolean handleMapScroll(double mouseX, double mouseY, double delta) {
        if (!"map".equals(page) || delta == 0.0D || !mapViewport.contains(mouseX, mouseY)) return false;
        mapZoom = clamp(mapZoom * (float) Math.pow(1.14D, delta), 0.7F, 3.5F);
        clampPan();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(0, 0, width, height, 0xD6090C11);
        panel(graphics, layout.window(), BG);
        header(graphics, layout, mouseX, mouseY);
        tabs(graphics, layout, mouseX, mouseY);
        switch (page) {
            case "equipment" -> equipment(graphics, layout.content());
            case "map" -> map(graphics, layout.content());
            case "skills" -> growth(graphics, layout.content(), mouseX, mouseY);
            default -> overview(graphics, layout.content());
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void header(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Rect window = layout.window();
        graphics.text(font, Component.literal("LIVING KINGDOMS"), window.x() + 16, window.y() + 12, GOLD, true);
        graphics.text(font, Component.literal("왕국 기록부"), window.x() + 16, window.y() + 25, MUTED, false);
        Rect close = layout.close();
        tab(graphics, close, "×", close.contains(mouseX, mouseY), false);
        graphics.fill(window.x() + 12, window.y() + 42, window.right() - 12, window.y() + 43, EDGE);
    }

    private void tabs(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        for (int i = 0; i < PAGES.size(); i++) {
            PageTab pageTab = PAGES.get(i);
            Rect rect = layout.tab(i);
            boolean selected = pageTab.id().equals(page);
            tab(graphics, rect, pageTab.label(), rect.contains(mouseX, mouseY), selected);
            ExternalRpgUi.itemIcon(graphics, pageTab.icon(), rect.x() + 7, rect.y() + 6);
        }
    }

    private void overview(GuiGraphicsExtractor graphics, Rect content) {
        int gap = 8;
        boolean wide = content.w() >= 500;
        Rect identity = wide
                ? new Rect(content.x(), content.y(), (content.w() - gap) * 56 / 100, content.h())
                : new Rect(content.x(), content.y(), content.w(), (content.h() - gap) / 2);
        Rect status = wide
                ? new Rect(identity.right() + gap, content.y(), content.right() - identity.right() - gap, content.h())
                : new Rect(content.x(), identity.bottom() + gap, content.w(), content.bottom() - identity.bottom() - gap);
        panel(graphics, identity, PANEL);
        panel(graphics, status, PANEL_ALT);
        ExternalRpgUi.itemIcon(graphics, Items.PLAYER_HEAD, identity.x() + 12, identity.y() + 12);
        graphics.text(font, Component.literal(shortText(value("player"), 28)), identity.x() + 37, identity.y() + 11, TEXT, true);
        graphics.text(font, Component.literal(shortText(value("citizenship"), 34)), identity.x() + 37, identity.y() + 24, MUTED, false);
        int y = identity.y() + 47;
        y = row(graphics, identity, y, "종족", value("species"));
        y = row(graphics, identity, y, "소속", value("affiliation"));
        y = row(graphics, identity, y, "배경", value("background"));
        if (y + 10 < identity.bottom()) y = row(graphics, identity, y, "거주", value("residence"));
        if (y + 10 < identity.bottom()) row(graphics, identity, y, "지역", value("region"));
        graphics.text(font, Component.literal(shortText(value("trait_title"), 30)), status.x() + 10, status.y() + 10, GOLD, true);
        graphics.text(font, Component.literal(shortText(value("trait_description"), Math.max(12, (status.w() - 20) / 6))), status.x() + 10, status.y() + 25, MUTED, false);
        meter(graphics, status.x() + 10, status.y() + 49, status.w() - 20, "생명", value("health"), ratio(value("health")), 0xFFD35B51);
        meter(graphics, status.x() + 10, status.y() + 73, status.w() - 20, "방호", value("armor"), Math.min(1.0F, parseInt("armor") / 20.0F), 0xFF718DA6);
        if (status.h() > 118) graphics.text(font, Component.literal("숙련은 행동으로 계속 성장합니다"), status.x() + 10, status.bottom() - 18, 0xFFA8C3A2, false);
    }

    private void equipment(GuiGraphicsExtractor graphics, Rect content) {
        panel(graphics, content, PANEL);
        title(graphics, content, Items.IRON_CHESTPLATE, "장비와 법적 상태");
        int lawHeight = 46;
        Rect grid = new Rect(content.x() + 10, content.y() + 38, content.w() - 20, Math.max(1, content.h() - 38 - lawHeight - 14));
        Rect law = new Rect(content.x() + 10, content.bottom() - lawHeight - 6, content.w() - 20, lawHeight);
        Slot[] slots = {
                new Slot("주무기", "mainhand", Items.IRON_SWORD), new Slot("보조", "offhand", Items.SHIELD),
                new Slot("머리", "head", Items.IRON_HELMET), new Slot("몸통", "chest", Items.IRON_CHESTPLATE),
                new Slot("다리", "legs", Items.IRON_LEGGINGS), new Slot("발", "feet", Items.IRON_BOOTS)
        };
        int columns = grid.w() >= 470 ? 3 : 2;
        int rows = (slots.length + columns - 1) / columns;
        int gap = 5;
        int slotW = (grid.w() - gap * (columns - 1)) / columns;
        int slotH = (grid.h() - gap * (rows - 1)) / rows;
        for (int i = 0; i < slots.length; i++) {
            Rect rect = new Rect(grid.x() + (i % columns) * (slotW + gap), grid.y() + (i / columns) * (slotH + gap), slotW, slotH);
            panel(graphics, rect, PANEL_ALT);
            ExternalRpgUi.itemIcon(graphics, slots[i].icon(), rect.x() + 7, rect.y() + Math.max(3, (rect.h() - 16) / 2));
            String text = slots[i].label() + "  " + shortText(value(slots[i].key()), Math.max(8, (rect.w() - 44) / 6));
            graphics.text(font, Component.literal(text), rect.x() + 30, rect.y() + Math.max(5, (rect.h() - 8) / 2), TEXT, false);
        }
        panel(graphics, law, 0xE9181C23);
        graphics.text(font, Component.literal("관할  " + shortText(value("jurisdiction"), 24)), law.x() + 9, law.y() + 8, TEXT, false);
        graphics.text(font, Component.literal("수배 " + value("wanted") + "   저항 " + value("resistance") + "   체포 " + value("arrest")), law.x() + 9, law.y() + 24, parseInt("wanted") > 0 ? 0xFFFF8172 : MUTED, false);
    }

    private void map(GuiGraphicsExtractor graphics, Rect content) {
        panel(graphics, content, PANEL);
        title(graphics, content, Items.FILLED_MAP, "아르데시아 대륙 지도");
        mapViewport = new Rect(content.x() + 10, content.y() + 38, content.w() - 20, content.h() - 48);
        graphics.fill(mapViewport.x(), mapViewport.y(), mapViewport.right(), mapViewport.bottom(), 0xFF101820);
        graphics.fill(mapViewport.x() + 2, mapViewport.y() + 2, mapViewport.right() - 2, mapViewport.bottom() - 2, 0xFF263A38);
        grid(graphics);
        for (int[] link : LINKS) dotted(graphics, mapPoint(REGIONS.get(link[0])), mapPoint(REGIONS.get(link[1])), 0xAAAC9562);
        for (Region region : REGIONS) region(graphics, region);
        marker(graphics, mapPoint(parseInt("home_x"), parseInt("home_z")), 0xFFE5C66E, 3);
        marker(graphics, mapPoint(parseInt("player_x"), parseInt("player_z")), 0xFFFFFFFF, 4);
        graphics.text(font, Component.literal("드래그 이동 · 휠 확대/축소 · 흰색 현재 위치 · 금색 거주지"), mapViewport.x() + 7, mapViewport.bottom() - 13, 0xFFD7CFBB, false);
        graphics.text(font, Component.literal(Math.round(mapZoom * 100) + "%"), mapViewport.right() - 35, mapViewport.y() + 6, GOLD, false);
    }

    private void growth(GuiGraphicsExtractor graphics, Rect content, int mouseX, int mouseY) {
        GrowthLayout layout = growthLayout(content);
        panel(graphics, content, PANEL);
        title(graphics, content, Items.EXPERIENCE_BOTTLE, "행동 숙련 · 최대 레벨 없음");
        for (int i = 0; i < MASTERY.length; i++) {
            String id = MASTERY[i];
            Rect rect = layout.mastery(i);
            String name = value("mastery_" + id + "_name");
            if (name.isBlank()) name = MasteryProgressionSavedData.displayName(id);
            meter(graphics, rect.x(), rect.y(), rect.w(), name, "Lv." + value("mastery_" + id + "_level"), parseFloat("mastery_" + id + "_progress"), masteryColor(i));
        }
        Rect tree = layout.tree();
        panel(graphics, tree, PANEL_ALT);
        graphics.text(font, Component.literal("보조 효과 트리"), tree.x() + 8, tree.y() + 7, GOLD, true);
        graphics.text(font, Component.literal("숙련 수치를 대신하지 않고 특수 규칙만 해금"), tree.x() + 89, tree.y() + 7, MUTED, false);
        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branchTab = BRANCHES.get(i);
            Rect rect = layout.branch(i);
            tab(graphics, rect, branchTab.label(), rect.contains(mouseX, mouseY), branchTab.id().equals(branch));
        }
        Set<String> unlocked = unlocked();
        List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            Rect rect = layout.node(i, nodes.size());
            panel(graphics, rect, 0xE9191E25);
            boolean owned = unlocked.contains(node.id());
            graphics.text(font, Component.literal(shortText(node.title(), Math.max(8, (rect.w() - 12) / 6))), rect.x() + 6, rect.y() + 5, owned ? 0xFF8FD29B : TEXT, true);
            if (rect.h() >= 36) graphics.text(font, Component.literal(shortText(node.description(), Math.max(8, (rect.w() - 12) / 6))), rect.x() + 6, rect.y() + 18, MUTED, false);
            graphics.text(font, Component.literal(owned ? "해금됨" : node.cost() + "점"), rect.x() + 6, rect.bottom() - 11, owned ? 0xFF8FD29B : GOLD, false);
        }
    }

    private void title(GuiGraphicsExtractor graphics, Rect content, Item icon, String text) {
        ExternalRpgUi.itemIcon(graphics, icon, content.x() + 10, content.y() + 10);
        graphics.text(font, Component.literal(text), content.x() + 34, content.y() + 13, GOLD, true);
        graphics.fill(content.x() + 34, content.y() + 29, content.right() - 10, content.y() + 30, EDGE);
    }

    private int row(GuiGraphicsExtractor graphics, Rect panel, int y, String label, String value) {
        if (y + 9 >= panel.bottom()) return y;
        graphics.text(font, Component.literal(label), panel.x() + 10, y, 0xFFB59A6B, false);
        graphics.text(font, Component.literal(shortText(value, Math.max(10, (panel.w() - 78) / 6))), panel.x() + 65, y, TEXT, false);
        return y + 15;
    }

    private void meter(GuiGraphicsExtractor graphics, int x, int y, int width, String label, String value, float ratio, int color) {
        int valueWidth = font.width(value);
        graphics.text(font, Component.literal(label), x, y, TEXT, false);
        graphics.text(font, Component.literal(value), x + width - valueWidth, y, MUTED, false);
        graphics.fill(x, y + 11, x + width, y + 17, 0xC3090C10);
        int filled = Math.max(0, Math.min(width, Math.round(width * clamp(ratio, 0.0F, 1.0F))));
        if (filled > 0) graphics.fill(x, y + 11, x + filled, y + 17, color);
    }

    private void tab(GuiGraphicsExtractor graphics, Rect rect, String label, boolean hovered, boolean selected) {
        int fill = selected ? 0xFF344553 : hovered ? 0xFF2A343F : 0xE91A2028;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        int edge = selected ? GOLD : EDGE;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, edge);
        graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), edge);
        graphics.centeredText(font, Component.literal(label), rect.x() + rect.w() / 2, rect.y() + Math.max(4, (rect.h() - 8) / 2), selected ? 0xFFFFFFFF : TEXT);
    }

    private void panel(GuiGraphicsExtractor graphics, Rect rect, int fill) {
        graphics.fill(rect.x() + 3, rect.y() + 4, rect.right() + 3, rect.bottom() + 4, 0x55000000);
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, EDGE);
    }

    private void grid(GuiGraphicsExtractor graphics) {
        for (int coordinate = WORLD_MIN; coordinate <= WORLD_MAX; coordinate += 4_000) {
            dotted(graphics, mapPoint(coordinate, WORLD_MIN), mapPoint(coordinate, WORLD_MAX), 0x443E5A58);
            dotted(graphics, mapPoint(WORLD_MIN, coordinate), mapPoint(WORLD_MAX, coordinate), 0x443E5A58);
        }
    }

    private void region(GuiGraphicsExtractor graphics, Region region) {
        Point point = mapPoint(region);
        if (!mapViewport.inset(5).contains(point.x(), point.y())) return;
        marker(graphics, point, region.color(), 4);
        int x = clamp(point.x() + 6, mapViewport.x() + 3, mapViewport.right() - font.width(region.name()) - 3);
        int y = clamp(point.y() - 4, mapViewport.y() + 3, mapViewport.bottom() - 24);
        graphics.text(font, Component.literal(region.name()), x, y, TEXT, true);
    }

    private Point mapPoint(Region region) {
        int x = region.xKey() == null ? region.defaultX() : parseInt(region.xKey(), region.defaultX());
        int z = region.zKey() == null ? region.defaultZ() : parseInt(region.zKey(), region.defaultZ());
        return mapPoint(x, z);
    }

    private Point mapPoint(int worldX, int worldZ) {
        float nx = (worldX - WORLD_MIN) / (float) (WORLD_MAX - WORLD_MIN) - 0.5F;
        float nz = (worldZ - WORLD_MIN) / (float) (WORLD_MAX - WORLD_MIN) - 0.5F;
        return new Point(Math.round(mapViewport.x() + mapViewport.w() / 2.0F + nx * mapViewport.w() * mapZoom + mapPanX), Math.round(mapViewport.y() + mapViewport.h() / 2.0F + nz * mapViewport.h() * mapZoom + mapPanY));
    }

    private void dotted(GuiGraphicsExtractor graphics, Point a, Point b, int color) {
        int dx = b.x() - a.x();
        int dy = b.y() - a.y();
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) return;
        for (int i = 0; i <= steps; i += 4) {
            int x = a.x() + dx * i / steps;
            int y = a.y() + dy * i / steps;
            if (mapViewport.inset(2).contains(x, y)) graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private void marker(GuiGraphicsExtractor graphics, Point p, int color, int radius) {
        if (!mapViewport.inset(radius).contains(p.x(), p.y())) return;
        graphics.fill(p.x() - radius, p.y() - 1, p.x() + radius + 1, p.y() + 2, color);
        graphics.fill(p.x() - 1, p.y() - radius, p.x() + 2, p.y() + radius + 1, color);
    }

    private void clampPan() {
        float xLimit = Math.max(50.0F, mapViewport.w() * mapZoom * 0.55F);
        float yLimit = Math.max(40.0F, mapViewport.h() * mapZoom * 0.55F);
        mapPanX = clamp(mapPanX, -xLimit, xLimit);
        mapPanY = clamp(mapPanY, -yLimit, yLimit);
    }

    boolean allRequiredControlsFit() {
        Layout layout = layout();
        Rect screen = new Rect(0, 0, width, height);
        if (!inside(layout.window(), screen) || !inside(layout.content(), layout.window()) || !inside(layout.close(), layout.window())) return false;
        for (int i = 0; i < PAGES.size(); i++) if (!inside(layout.tab(i), layout.window())) return false;
        if ("skills".equals(page)) {
            GrowthLayout growth = growthLayout(layout.content());
            if (!inside(growth.tree(), layout.content())) return false;
            for (int i = 0; i < MASTERY.length; i++) if (!inside(growth.mastery(i), layout.content())) return false;
            List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
            for (int i = 0; i < nodes.size(); i++) if (!inside(growth.node(i, nodes.size()), growth.tree())) return false;
        }
        return true;
    }

    private Layout layout() {
        int windowW = Math.min(740, Math.max(300, width - 10));
        int windowH = Math.min(430, Math.max(190, height - 10));
        windowW = Math.min(windowW, Math.max(1, width - 4));
        windowH = Math.min(windowH, Math.max(1, height - 4));
        Rect window = new Rect((width - windowW) / 2, (height - windowH) / 2, windowW, windowH);
        int tabY = window.y() + 48;
        int gap = 5;
        int tabW = (window.w() - 24 - gap * 3) / 4;
        Rect content = new Rect(window.x() + 12, tabY + 35, window.w() - 24, window.bottom() - 12 - (tabY + 35));
        return new Layout(window, content, new Rect(window.right() - 43, window.y() + 10, 30, 28), tabY, tabW, gap);
    }

    private GrowthLayout growthLayout(Rect content) {
        Rect area = new Rect(content.x() + 10, content.y() + 38, content.w() - 20, content.h() - 48);
        int masteryHeight = Math.max(72, area.h() * 48 / 100);
        Rect mastery = new Rect(area.x(), area.y(), area.w(), masteryHeight);
        Rect tree = new Rect(area.x(), mastery.bottom() + 6, area.w(), area.bottom() - mastery.bottom() - 6);
        return new GrowthLayout(mastery, tree);
    }

    private List<SkillTreeCatalog.SkillNode> branchNodes() { return SkillTreeCatalog.nodes().values().stream().filter(node -> branch.equals(node.branch())).toList(); }

    private Set<String> unlocked() {
        Set<String> result = new LinkedHashSet<>();
        for (String id : value("unlocked_skills").split(",")) if (!id.isBlank()) result.add(id.trim());
        return result;
    }

    private String value(String key) { return data.getOrDefault(key, ""); }
    private int parseInt(String key) { return parseInt(key, 0); }
    private int parseInt(String key, int fallback) { try { return Integer.parseInt(value(key)); } catch (NumberFormatException ignored) { return fallback; } }
    private float parseFloat(String key) { try { return clamp(Float.parseFloat(value(key)), 0.0F, 1.0F); } catch (NumberFormatException ignored) { return 0.0F; } }
    private float ratio(String raw) {
        String[] parts = raw.split("/");
        if (parts.length != 2) return 0.0F;
        try { return clamp(Float.parseFloat(parts[0].trim()) / Math.max(1.0F, Float.parseFloat(parts[1].trim())), 0.0F, 1.0F); }
        catch (NumberFormatException ignored) { return 0.0F; }
    }

    private int masteryColor(int index) {
        return switch (index) {
            case 0 -> 0xFFB75C52; case 1 -> 0xFF6687A4; case 2 -> 0xFF8D9299;
            case 3 -> 0xFF8B6846; case 4 -> 0xFF729353; case 5 -> 0xFF559078;
            default -> 0xFF547E9A;
        };
    }

    private static String normalizePage(String page) { return switch (page) { case "equipment", "map", "skills" -> page; default -> "overview"; }; }
    private static Map<String, String> parse(String snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        if (snapshot == null) return values;
        for (String line : snapshot.split("\\n")) { int tab = line.indexOf('\t'); if (tab > 0) values.put(line.substring(0, tab), line.substring(tab + 1)); }
        return values;
    }
    private static String shortText(String text, int max) { if (text == null || text.isBlank()) return "없음"; if (text.length() <= max) return text; return text.substring(0, Math.max(1, max - 1)) + "…"; }
    private static boolean inside(Rect child, Rect parent) { return child.x() >= parent.x() && child.y() >= parent.y() && child.right() <= parent.right() && child.bottom() <= parent.bottom(); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private record PageTab(String id, String label, Item icon) {}
    private record Slot(String label, String key, Item icon) {}
    private record Region(String name, String xKey, String zKey, int defaultX, int defaultZ, int color) {}
    private record Point(int x, int y) {}
    record Rect(int x, int y, int w, int h) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);
        int right() { return x + w; }
        int bottom() { return y + h; }
        boolean contains(double px, double py) { return px >= x && py >= y && px < right() && py < bottom(); }
        Rect inset(int value) { return new Rect(x + value, y + value, Math.max(0, w - value * 2), Math.max(0, h - value * 2)); }
    }
    private record Layout(Rect window, Rect content, Rect close, int tabY, int tabW, int gap) { Rect tab(int index) { return new Rect(window.x() + 12 + index * (tabW + gap), tabY, tabW, 29); } }
    private record GrowthLayout(Rect masteryArea, Rect tree) {
        Rect mastery(int index) {
            int columns = masteryArea.w() >= 470 ? 2 : 1;
            int rows = (MASTERY.length + columns - 1) / columns;
            int gapX = 12, gapY = 3;
            int width = (masteryArea.w() - gapX * (columns - 1)) / columns;
            int height = Math.max(18, (masteryArea.h() - gapY * (rows - 1)) / rows);
            return new Rect(masteryArea.x() + (index % columns) * (width + gapX), masteryArea.y() + (index / columns) * (height + gapY), width, height);
        }
        Rect branch(int index) {
            int gap = 3;
            int width = (tree.w() - 16 - gap * (BRANCHES.size() - 1)) / BRANCHES.size();
            return new Rect(tree.x() + 8 + index * (width + gap), tree.y() + 22, width, 22);
        }
        Rect node(int index, int count) {
            int gap = 5, top = tree.y() + 49;
            int available = Math.max(1, tree.bottom() - top - 5);
            int width = (tree.w() - 16 - gap * Math.max(0, count - 1)) / Math.max(1, count);
            return new Rect(tree.x() + 8 + index * (width + gap), top, width, available);
        }
    }
}

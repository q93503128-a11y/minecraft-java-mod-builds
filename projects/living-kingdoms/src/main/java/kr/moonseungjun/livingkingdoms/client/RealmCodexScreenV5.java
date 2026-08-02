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

/**
 * Fifth-generation codex designed around the actual small GUI viewport used by Minecraft.
 * Masteries and the auxiliary tree are separate views instead of competing for the same pixels.
 */
public final class RealmCodexScreenV5 extends Screen {
    private static final int WINDOW = 0xF213171E;
    private static final int PANEL = 0xED1C222B;
    private static final int PANEL_ALT = 0xED242C36;
    private static final int SHADOW = 0x66000000;
    private static final int EDGE = 0xFF786444;
    private static final int GOLD = 0xFFE1BE70;
    private static final int TEXT = 0xFFF1E8D3;
    private static final int MUTED = 0xFFB9AE98;
    private static final int WORLD_MIN = -6_000;
    private static final int WORLD_MAX = 6_000;

    private static final List<Tab> PAGES = List.of(
            new Tab("overview", "인물", Items.PLAYER_HEAD),
            new Tab("equipment", "장비·법률", Items.IRON_CHESTPLATE),
            new Tab("map", "지도", Items.FILLED_MAP),
            new Tab("growth", "성장", Items.ENCHANTED_BOOK)
    );
    private static final List<Tab> BRANCHES = List.of(
            new Tab("combat", "전투", Items.IRON_SWORD),
            new Tab("exploration", "탐험", Items.COMPASS),
            new Tab("livelihood", "생활", Items.IRON_HOE),
            new Tab("society", "사회", Items.EMERALD),
            new Tab("arcana", "마법", Items.AMETHYST_SHARD)
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
            new Region("실바나", "silvana_x", "silvana_z", -2_400, -1_200, 0xFF70A674),
            new Region("카르둠", "kardum_x", "kardum_z", 2_200, -1_500, 0xFF9AA2AA),
            new Region("붉은 초원", null, null, 3_400, 300, 0xFFC87955),
            new Region("벨라스", null, null, 600, 2_500, 0xFF72A7C7),
            new Region("사하르", null, null, 3_200, 2_600, 0xFFD8C16F),
            new Region("서부 군도", null, null, -4_200, 1_800, 0xFF658DAA)
    );
    private static final int[][] LINKS = {{0, 1}, {0, 2}, {0, 3}, {0, 4}, {3, 5}, {1, 6}};

    private final Map<String, String> data;
    private String page;
    private String growthView = "mastery";
    private String branch = "combat";
    private float mapZoom = 1.0F;
    private float mapPanX;
    private float mapPanY;
    private Rect mapViewport = Rect.EMPTY;

    public RealmCodexScreenV5(String requestedPage, String snapshot) {
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
            Tab tab = PAGES.get(i);
            invisible(layout.pageTab(i), () -> {
                page = tab.id();
                rebuild();
            });
        }
        if (!"growth".equals(page)) return;
        GrowthLayout growth = growthLayout(layout.content());
        invisible(growth.masteryButton(), () -> {
            growthView = "mastery";
            rebuild();
        });
        invisible(growth.treeButton(), () -> {
            growthView = "tree";
            rebuild();
        });
        if (!"tree".equals(growthView)) return;
        for (int i = 0; i < BRANCHES.size(); i++) {
            Tab tab = BRANCHES.get(i);
            invisible(growth.branchTab(i), () -> {
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
        mapZoom = clamp(mapZoom * (float) Math.pow(1.14D, delta), 0.72F, 3.5F);
        clampPan();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(0, 0, width, height, 0xD9080B0F);
        panel(graphics, layout.window(), WINDOW);
        drawHeader(graphics, layout, mouseX, mouseY);
        drawPageTabs(graphics, layout, mouseX, mouseY);
        switch (page) {
            case "equipment" -> drawEquipment(graphics, layout.content());
            case "map" -> drawMap(graphics, layout.content());
            case "growth" -> drawGrowth(graphics, layout.content(), mouseX, mouseY);
            default -> drawOverview(graphics, layout.content());
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Rect window = layout.window();
        graphics.text(font, Component.literal("LIVING KINGDOMS"), window.x() + 14, window.y() + 10, GOLD, true);
        graphics.text(font, Component.literal("왕국 기록부"), window.x() + 14, window.y() + 23, MUTED, false);
        Rect close = layout.close();
        button(graphics, close, "×", close.contains(mouseX, mouseY), false);
        graphics.fill(window.x() + 10, window.y() + 39, window.right() - 10, window.y() + 40, EDGE);
    }

    private void drawPageTabs(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        for (int i = 0; i < PAGES.size(); i++) {
            Tab tab = PAGES.get(i);
            Rect rect = layout.pageTab(i);
            button(graphics, rect, tab.label(), rect.contains(mouseX, mouseY), tab.id().equals(page));
            if (rect.w() >= 70) ExternalRpgUi.itemIcon(graphics, tab.icon(), rect.x() + 5, rect.y() + 5);
        }
    }

    private void drawOverview(GuiGraphicsExtractor graphics, Rect content) {
        int gap = 6;
        boolean wide = content.w() >= 470;
        Rect identity = wide
                ? new Rect(content.x(), content.y(), (content.w() - gap) * 55 / 100, content.h())
                : new Rect(content.x(), content.y(), content.w(), (content.h() - gap) / 2);
        Rect status = wide
                ? new Rect(identity.right() + gap, content.y(), content.right() - identity.right() - gap, content.h())
                : new Rect(content.x(), identity.bottom() + gap, content.w(), content.bottom() - identity.bottom() - gap);
        panel(graphics, identity, PANEL);
        panel(graphics, status, PANEL_ALT);
        ExternalRpgUi.itemIcon(graphics, Items.PLAYER_HEAD, identity.x() + 9, identity.y() + 9);
        graphics.text(font, Component.literal(shortText(value("player"), 26)), identity.x() + 32, identity.y() + 8, TEXT, true);
        graphics.text(font, Component.literal(shortText(value("citizenship"), 30)), identity.x() + 32, identity.y() + 21, MUTED, false);
        int y = identity.y() + 41;
        y = row(graphics, identity, y, "종족", value("species"));
        y = row(graphics, identity, y, "소속", value("affiliation"));
        y = row(graphics, identity, y, "배경", value("background"));
        if (y + 9 < identity.bottom()) y = row(graphics, identity, y, "거주", value("residence"));
        if (y + 9 < identity.bottom()) row(graphics, identity, y, "지역", value("region"));

        graphics.text(font, Component.literal(shortText(value("trait_title"), 24)), status.x() + 8, status.y() + 8, GOLD, true);
        if (status.h() >= 48) graphics.text(font, Component.literal(shortText(value("trait_description"), Math.max(10, (status.w() - 16) / 6))), status.x() + 8, status.y() + 21, MUTED, false);
        int meterY = status.h() >= 100 ? status.y() + 43 : status.y() + 29;
        meter(graphics, status.x() + 8, meterY, status.w() - 16, "생명", value("health"), ratio(value("health")), 0xFFD15A50);
        if (meterY + 42 <= status.bottom()) meter(graphics, status.x() + 8, meterY + 22, status.w() - 16, "방호", value("armor"), Math.min(1.0F, parseInt("armor") / 20.0F), 0xFF718DA6);
    }

    private void drawEquipment(GuiGraphicsExtractor graphics, Rect content) {
        panel(graphics, content, PANEL);
        title(graphics, content, Items.IRON_CHESTPLATE, "장비와 법적 상태");
        int lawHeight = Math.min(42, Math.max(34, content.h() / 4));
        Rect grid = new Rect(content.x() + 8, content.y() + 34, content.w() - 16,
                Math.max(1, content.h() - 34 - lawHeight - 10));
        Rect law = new Rect(content.x() + 8, content.bottom() - lawHeight - 4, content.w() - 16, lawHeight);
        Slot[] slots = {
                new Slot("주무기", "mainhand", Items.IRON_SWORD), new Slot("보조", "offhand", Items.SHIELD),
                new Slot("머리", "head", Items.IRON_HELMET), new Slot("몸통", "chest", Items.IRON_CHESTPLATE),
                new Slot("다리", "legs", Items.IRON_LEGGINGS), new Slot("발", "feet", Items.IRON_BOOTS)
        };
        int columns = grid.w() >= 470 ? 3 : 2;
        int rows = (slots.length + columns - 1) / columns;
        int gap = 4;
        int slotW = (grid.w() - gap * (columns - 1)) / columns;
        int slotH = Math.max(1, (grid.h() - gap * (rows - 1)) / rows);
        for (int i = 0; i < slots.length; i++) {
            Rect rect = new Rect(grid.x() + (i % columns) * (slotW + gap),
                    grid.y() + (i / columns) * (slotH + gap), slotW, slotH);
            panel(graphics, rect, PANEL_ALT);
            ExternalRpgUi.itemIcon(graphics, slots[i].icon(), rect.x() + 5, rect.y() + Math.max(2, (rect.h() - 16) / 2));
            String text = slots[i].label() + "  " + shortText(value(slots[i].key()), Math.max(6, (rect.w() - 36) / 6));
            graphics.text(font, Component.literal(text), rect.x() + 26, rect.y() + Math.max(4, (rect.h() - 8) / 2), TEXT, false);
        }
        panel(graphics, law, 0xEF171B21);
        graphics.text(font, Component.literal("관할  " + shortText(value("jurisdiction"), 22)), law.x() + 7, law.y() + 6, TEXT, false);
        if (law.h() >= 25) graphics.text(font, Component.literal("수배 " + value("wanted") + " · 저항 " + value("resistance") + " · 체포 " + value("arrest")), law.x() + 7, law.y() + 19, parseInt("wanted") > 0 ? 0xFFFF8172 : MUTED, false);
    }

    private void drawMap(GuiGraphicsExtractor graphics, Rect content) {
        panel(graphics, content, PANEL);
        title(graphics, content, Items.FILLED_MAP, "아르데시아 대륙 지도");
        mapViewport = new Rect(content.x() + 8, content.y() + 34, content.w() - 16, content.h() - 42);
        graphics.fill(mapViewport.x(), mapViewport.y(), mapViewport.right(), mapViewport.bottom(), 0xFF0F171D);
        graphics.fill(mapViewport.x() + 2, mapViewport.y() + 2, mapViewport.right() - 2, mapViewport.bottom() - 2, 0xFF263B38);
        for (int coordinate = WORLD_MIN; coordinate <= WORLD_MAX; coordinate += 2_000) {
            dotted(graphics, mapPoint(coordinate, WORLD_MIN), mapPoint(coordinate, WORLD_MAX), 0x423E5A58);
            dotted(graphics, mapPoint(WORLD_MIN, coordinate), mapPoint(WORLD_MAX, coordinate), 0x423E5A58);
        }
        for (int[] link : LINKS) dotted(graphics, mapPoint(REGIONS.get(link[0])), mapPoint(REGIONS.get(link[1])), 0xA5AE9764);
        for (Region region : REGIONS) drawRegion(graphics, region);
        marker(graphics, mapPoint(parseInt("home_x"), parseInt("home_z")), 0xFFE7C66C, 3);
        marker(graphics, mapPoint(parseInt("player_x"), parseInt("player_z")), 0xFFFFFFFF, 4);
        if (mapViewport.h() >= 35) graphics.text(font, Component.literal("드래그 이동 · 휠 확대 · 흰색 현재 · 금색 거주지"), mapViewport.x() + 6, mapViewport.bottom() - 12, 0xFFD7CFBB, false);
        graphics.text(font, Component.literal(Math.round(mapZoom * 100) + "%"), mapViewport.right() - 34, mapViewport.y() + 5, GOLD, false);
    }

    private void drawGrowth(GuiGraphicsExtractor graphics, Rect content, int mouseX, int mouseY) {
        panel(graphics, content, PANEL);
        title(graphics, content, Items.EXPERIENCE_BOTTLE, "성장");
        GrowthLayout layout = growthLayout(content);
        button(graphics, layout.masteryButton(), "행동 숙련", layout.masteryButton().contains(mouseX, mouseY), "mastery".equals(growthView));
        button(graphics, layout.treeButton(), "보조 효과", layout.treeButton().contains(mouseX, mouseY), "tree".equals(growthView));
        if ("mastery".equals(growthView)) drawMasteries(graphics, layout);
        else drawTree(graphics, layout, mouseX, mouseY);
    }

    private void drawMasteries(GuiGraphicsExtractor graphics, GrowthLayout layout) {
        for (int i = 0; i < MASTERY.length; i++) {
            String id = MASTERY[i];
            Rect rect = layout.mastery(i);
            String name = value("mastery_" + id + "_name");
            if (name.isBlank()) name = MasteryProgressionSavedData.displayName(id);
            meter(graphics, rect.x(), rect.y(), rect.w(), name,
                    "Lv." + value("mastery_" + id + "_level"),
                    parseFloat("mastery_" + id + "_progress"), masteryColor(i));
        }
        Rect note = layout.note();
        if (note.h() >= 9) graphics.text(font, Component.literal("전투·생활 행동이 직접 숙련을 올리며 최대 레벨은 없습니다."), note.x(), note.y(), 0xFFA8C5A4, false);
    }

    private void drawTree(GuiGraphicsExtractor graphics, GrowthLayout layout, int mouseX, int mouseY) {
        for (int i = 0; i < BRANCHES.size(); i++) {
            Tab tab = BRANCHES.get(i);
            Rect rect = layout.branchTab(i);
            button(graphics, rect, tab.label(), rect.contains(mouseX, mouseY), tab.id().equals(branch));
        }
        Set<String> unlocked = unlocked();
        List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            Rect rect = layout.node(i, nodes.size());
            panel(graphics, rect, 0xED181D24);
            boolean owned = unlocked.contains(node.id());
            graphics.text(font, Component.literal(shortText(node.title(), Math.max(6, (rect.w() - 10) / 6))), rect.x() + 5, rect.y() + 4, owned ? 0xFF8FD29B : TEXT, true);
            if (rect.h() >= 31) graphics.text(font, Component.literal(shortText(node.description(), Math.max(6, (rect.w() - 10) / 6))), rect.x() + 5, rect.y() + 16, MUTED, false);
            graphics.text(font, Component.literal(owned ? "해금됨" : node.cost() + "점"), rect.x() + 5, rect.bottom() - 10, owned ? 0xFF8FD29B : GOLD, false);
        }
    }

    private void title(GuiGraphicsExtractor graphics, Rect content, Item icon, String text) {
        ExternalRpgUi.itemIcon(graphics, icon, content.x() + 8, content.y() + 8);
        graphics.text(font, Component.literal(text), content.x() + 31, content.y() + 11, GOLD, true);
        graphics.fill(content.x() + 31, content.y() + 27, content.right() - 8, content.y() + 28, EDGE);
    }

    private int row(GuiGraphicsExtractor graphics, Rect box, int y, String label, String text) {
        if (y + 8 >= box.bottom()) return y;
        graphics.text(font, Component.literal(label), box.x() + 8, y, 0xFFB69B6C, false);
        graphics.text(font, Component.literal(shortText(text, Math.max(8, (box.w() - 66) / 6))), box.x() + 56, y, TEXT, false);
        return y + 14;
    }

    private void meter(GuiGraphicsExtractor graphics, int x, int y, int width,
                       String label, String value, float ratio, int color) {
        graphics.text(font, Component.literal(shortText(label, Math.max(6, width / 12))), x, y, TEXT, false);
        int valueWidth = font.width(value);
        graphics.text(font, Component.literal(value), x + width - valueWidth, y, MUTED, false);
        graphics.fill(x, y + 10, x + width, y + 15, 0xD0080B0F);
        int filled = Math.max(0, Math.min(width, Math.round(width * clamp(ratio, 0.0F, 1.0F))));
        if (filled > 0) graphics.fill(x, y + 10, x + filled, y + 15, color);
    }

    private void button(GuiGraphicsExtractor graphics, Rect rect, String label, boolean hovered, boolean selected) {
        int fill = selected ? 0xFF344653 : hovered ? 0xFF2A343D : 0xED191F27;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        int edge = selected ? GOLD : EDGE;
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, edge);
        graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), edge);
        graphics.centeredText(font, Component.literal(label), rect.x() + rect.w() / 2,
                rect.y() + Math.max(3, (rect.h() - 8) / 2), selected ? 0xFFFFFFFF : TEXT);
    }

    private void panel(GuiGraphicsExtractor graphics, Rect rect, int fill) {
        graphics.fill(rect.x() + 2, rect.y() + 3, rect.right() + 2, rect.bottom() + 3, SHADOW);
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, EDGE);
    }

    private void drawRegion(GuiGraphicsExtractor graphics, Region region) {
        Point point = mapPoint(region);
        if (!mapViewport.inset(4).contains(point.x(), point.y())) return;
        marker(graphics, point, region.color(), 3);
        String label = region.name();
        int x = clamp(point.x() + 5, mapViewport.x() + 2, mapViewport.right() - font.width(label) - 2);
        int y = clamp(point.y() - 4, mapViewport.y() + 2, mapViewport.bottom() - 20);
        graphics.text(font, Component.literal(label), x, y, TEXT, true);
    }

    private Point mapPoint(Region region) {
        int x = region.xKey() == null ? region.defaultX() : parseInt(region.xKey(), region.defaultX());
        int z = region.zKey() == null ? region.defaultZ() : parseInt(region.zKey(), region.defaultZ());
        return mapPoint(x, z);
    }

    private Point mapPoint(int worldX, int worldZ) {
        float nx = (worldX - WORLD_MIN) / (float) (WORLD_MAX - WORLD_MIN) - 0.5F;
        float nz = (worldZ - WORLD_MIN) / (float) (WORLD_MAX - WORLD_MIN) - 0.5F;
        return new Point(
                Math.round(mapViewport.x() + mapViewport.w() / 2.0F + nx * mapViewport.w() * mapZoom + mapPanX),
                Math.round(mapViewport.y() + mapViewport.h() / 2.0F + nz * mapViewport.h() * mapZoom + mapPanY)
        );
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

    private void marker(GuiGraphicsExtractor graphics, Point point, int color, int radius) {
        if (!mapViewport.inset(radius).contains(point.x(), point.y())) return;
        graphics.fill(point.x() - radius, point.y() - 1, point.x() + radius + 1, point.y() + 2, color);
        graphics.fill(point.x() - 1, point.y() - radius, point.x() + 2, point.y() + radius + 1, color);
    }

    private void clampPan() {
        float xLimit = Math.max(45.0F, mapViewport.w() * mapZoom * 0.55F);
        float yLimit = Math.max(35.0F, mapViewport.h() * mapZoom * 0.55F);
        mapPanX = clamp(mapPanX, -xLimit, xLimit);
        mapPanY = clamp(mapPanY, -yLimit, yLimit);
    }

    boolean allRequiredControlsFit() {
        Layout layout = layout();
        Rect screen = new Rect(0, 0, width, height);
        if (!inside(layout.window(), screen) || !inside(layout.content(), layout.window()) || !inside(layout.close(), layout.window())) return false;
        for (int i = 0; i < PAGES.size(); i++) if (!inside(layout.pageTab(i), layout.window())) return false;
        if ("growth".equals(page)) {
            GrowthLayout growth = growthLayout(layout.content());
            if (!inside(growth.masteryButton(), layout.content()) || !inside(growth.treeButton(), layout.content())) return false;
            if ("mastery".equals(growthView)) {
                for (int i = 0; i < MASTERY.length; i++) if (!inside(growth.mastery(i), layout.content())) return false;
            } else {
                for (int i = 0; i < BRANCHES.size(); i++) if (!inside(growth.branchTab(i), layout.content())) return false;
                List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
                for (int i = 0; i < nodes.size(); i++) if (!inside(growth.node(i, nodes.size()), layout.content())) return false;
            }
        }
        return true;
    }

    boolean selectGrowthViewForTest(String view) {
        if (!"mastery".equals(view) && !"tree".equals(view)) return false;
        growthView = view;
        rebuild();
        return true;
    }

    private Layout layout() {
        int windowW = Math.min(720, Math.max(296, width - 8));
        int windowH = Math.min(420, Math.max(186, height - 8));
        windowW = Math.min(windowW, Math.max(1, width - 4));
        windowH = Math.min(windowH, Math.max(1, height - 4));
        Rect window = new Rect((width - windowW) / 2, (height - windowH) / 2, windowW, windowH);
        int tabY = window.y() + 44;
        int gap = 4;
        int tabW = (window.w() - 20 - gap * 3) / 4;
        Rect content = new Rect(window.x() + 10, tabY + 31, window.w() - 20,
                window.bottom() - 10 - (tabY + 31));
        return new Layout(window, content, new Rect(window.right() - 39, window.y() + 8, 27, 26), tabY, tabW, gap);
    }

    private GrowthLayout growthLayout(Rect content) {
        int top = content.y() + 34;
        int modeGap = 4;
        int modeW = Math.min(110, (content.w() - 20) / 2);
        Rect masteryButton = new Rect(content.x() + 8, top, modeW, 21);
        Rect treeButton = new Rect(masteryButton.right() + modeGap, top, modeW, 21);
        Rect body = new Rect(content.x() + 8, top + 26, content.w() - 16,
                Math.max(1, content.bottom() - 7 - (top + 26)));
        return new GrowthLayout(masteryButton, treeButton, body);
    }

    private List<SkillTreeCatalog.SkillNode> branchNodes() {
        return SkillTreeCatalog.nodes().values().stream()
                .filter(node -> branch.equals(node.branch())).toList();
    }

    private Set<String> unlocked() {
        Set<String> result = new LinkedHashSet<>();
        for (String id : value("unlocked_skills").split(",")) if (!id.isBlank()) result.add(id.trim());
        return result;
    }

    private String value(String key) { return data.getOrDefault(key, ""); }
    private int parseInt(String key) { return parseInt(key, 0); }
    private int parseInt(String key, int fallback) {
        try { return Integer.parseInt(value(key)); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private float parseFloat(String key) {
        try { return clamp(Float.parseFloat(value(key)), 0.0F, 1.0F); }
        catch (NumberFormatException ignored) { return 0.0F; }
    }
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

    private static String normalizePage(String page) {
        return switch (page) {
            case "equipment", "map" -> page;
            case "skills", "growth" -> "growth";
            default -> "overview";
        };
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
    private static String shortText(String text, int max) {
        if (text == null || text.isBlank()) return "없음";
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(1, max - 1)) + "…";
    }
    private static boolean inside(Rect child, Rect parent) {
        return child.x() >= parent.x() && child.y() >= parent.y()
                && child.right() <= parent.right() && child.bottom() <= parent.bottom();
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private record Tab(String id, String label, Item icon) {}
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
    private record Layout(Rect window, Rect content, Rect close, int tabY, int tabW, int gap) {
        Rect pageTab(int index) { return new Rect(window.x() + 10 + index * (tabW + gap), tabY, tabW, 26); }
    }
    private record GrowthLayout(Rect masteryButton, Rect treeButton, Rect body) {
        Rect mastery(int index) {
            int columns = body.w() >= 330 ? 2 : 1;
            int rows = (MASTERY.length + columns - 1) / columns;
            int gapX = 10;
            int gapY = 2;
            int width = (body.w() - gapX * (columns - 1)) / columns;
            int noteH = body.h() >= 80 ? 11 : 0;
            int available = Math.max(1, body.h() - noteH - 2);
            int height = Math.max(15, (available - gapY * (rows - 1)) / rows);
            return new Rect(body.x() + (index % columns) * (width + gapX),
                    body.y() + (index / columns) * (height + gapY), width, height);
        }
        Rect note() {
            return new Rect(body.x(), body.bottom() - 9, body.w(), 9);
        }
        Rect branchTab(int index) {
            int gap = 3;
            int width = (body.w() - gap * (BRANCHES.size() - 1)) / BRANCHES.size();
            return new Rect(body.x() + index * (width + gap), body.y(), width, 20);
        }
        Rect node(int index, int count) {
            int gap = 4;
            int top = body.y() + 24;
            int height = Math.max(1, body.bottom() - top);
            int width = (body.w() - gap * Math.max(0, count - 1)) / Math.max(1, count);
            return new Rect(body.x() + index * (width + gap), top, width, height);
        }
    }
}

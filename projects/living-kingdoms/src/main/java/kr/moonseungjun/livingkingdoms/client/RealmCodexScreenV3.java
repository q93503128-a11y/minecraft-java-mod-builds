package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.UnlockSkillPayload;
import kr.moonseungjun.livingkingdoms.skill.SkillTreeCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded RPG codex with a pannable and zoomable authored-world atlas. */
public final class RealmCodexScreenV3 extends Screen {
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
    private static final List<AtlasRegion> REGIONS = List.of(
            new AtlasRegion("에르덴 왕국", "erden_x", "erden_z", 0, 0, 0xFFD6B45C),
            new AtlasRegion("실바나 수림 의회", "silvana_x", "silvana_z", -9_000, -1_500, 0xFF68A66B),
            new AtlasRegion("카르둠 산악 연맹", "kardum_x", "kardum_z", -2_500, -9_000, 0xFF9AA1A8),
            new AtlasRegion("붉은 초원 연맹", null, null, 9_500, -1_000, 0xFFC9784E),
            new AtlasRegion("벨라스 자유도시", null, null, 1_500, 7_500, 0xFF72A9C8),
            new AtlasRegion("사하르 신정국", null, null, 9_000, 9_000, 0xFFE0C36D),
            new AtlasRegion("회색 왕관 폐허", null, null, 8_500, -7_500, 0xFF817A86),
            new AtlasRegion("북부 용의 변경", null, null, 0, -15_000, 0xFF8D6E9E),
            new AtlasRegion("서부 군도", null, null, -14_000, 7_000, 0xFF5E91B5)
    );
    private static final int[][] ROAD_LINKS = {
            {0, 1}, {0, 2}, {0, 3}, {0, 4}, {3, 5}, {3, 6}, {2, 7}, {1, 8}, {4, 8}
    };

    private final Map<String, String> data;
    private String page;
    private String skillBranch = "combat";
    private float mapZoom = 1.0F;
    private float mapPanX;
    private float mapPanY;
    private Rect mapViewport = Rect.EMPTY;

    public RealmCodexScreenV3(String requestedPage, String snapshot) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("왕국 기록부"));
        page = normalizePage(requestedPage);
        data = parse(snapshot);
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            invisible(layout.pageTab(i), () -> {
                page = tab.id();
                rebuildWidgets();
            });
        }
        invisible(layout.closeButton(), this::onClose);
        if (!"skills".equals(page)) return;

        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            invisible(layout.branchTab(i), () -> {
                skillBranch = branch.id();
                rebuildWidgets();
            });
        }
        List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            invisible(layout.skillNode(i, nodes.size()), () ->
                    ClientPacketDistributor.sendToServer(new UnlockSkillPayload(node.id())));
        }
    }

    private void invisible(Rect rect, Runnable action) {
        if (rect.w() <= 0 || rect.h() <= 0) return;
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
        Layout layout = layout();
        if (!inside(layout.window(), new Rect(2, 2, Math.max(0, width - 4), Math.max(0, height - 4)))) return false;
        if (!inside(layout.content(), layout.window()) || !inside(layout.closeButton(), layout.window())) return false;
        if ("skills".equals(page)) {
            List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
            for (int i = 0; i < nodes.size(); i++) {
                if (!inside(layout.skillNode(i, nodes.size()), layout.content())) return false;
            }
        }
        return true;
    }

    boolean handleMapDrag(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        if (!"map".equals(page) || mouseButton != 0 || !mapViewport.contains(mouseX, mouseY)) return false;
        mapPanX += (float) dragX;
        mapPanY += (float) dragY;
        clampMapPan();
        return true;
    }

    boolean handleMapScroll(double mouseX, double mouseY, double scrollDeltaY) {
        if (!"map".equals(page) || !mapViewport.contains(mouseX, mouseY) || scrollDeltaY == 0.0) return false;
        mapZoom = clamp(mapZoom * (float) Math.pow(1.13, scrollDeltaY), 0.65F, 3.5F);
        clampMapPan();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        ExternalRpgUi.dimWorld(graphics, width, height);
        ExternalRpgUi.window(graphics, layout.left(), layout.top(), layout.panelW(), layout.panelH());
        drawHeader(graphics, layout, mouseX, mouseY);
        drawNavigation(graphics, layout, mouseX, mouseY);
        switch (page) {
            case "equipment" -> drawEquipment(graphics, layout);
            case "map" -> drawMap(graphics, layout);
            case "skills" -> drawSkills(graphics, layout, mouseX, mouseY);
            default -> drawOverview(graphics, layout);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        ExternalRpgUi.title(graphics, font, "LIVING KINGDOMS", "왕국 기록부",
                layout.left() + 20, layout.top() + 16);
        if (!layout.compact()) {
            ExternalRpgUi.badge(graphics, font, layout.left() + 164, layout.top() + 19,
                    shortText(value("affiliation"), 28), 0xFF49624E);
        }
        Rect close = layout.closeButton();
        ExternalRpgUi.button(graphics, font, close.x(), close.y(), close.w(), close.h(), "×",
                false, close.contains(mouseX, mouseY), true);
        ExternalRpgUi.divider(graphics, layout.left() + 17, layout.top() + 46, layout.panelW() - 34);
    }

    private void drawNavigation(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            Rect rect = layout.pageTab(i);
            ExternalRpgUi.iconButton(graphics, font, tab.icon(), rect.x(), rect.y(), rect.w(), rect.h(),
                    tab.label(), page.equals(tab.id()), rect.contains(mouseX, mouseY));
        }
    }

    private void drawOverview(GuiGraphicsExtractor graphics, Layout layout) {
        Rect content = layout.content();
        int gap = 7;
        Rect identity;
        Rect status;
        if (layout.compact()) {
            int firstHeight = Math.max(78, content.h() * 48 / 100);
            identity = new Rect(content.x(), content.y(), content.w(), firstHeight);
            status = new Rect(content.x(), identity.bottom() + gap, content.w(),
                    Math.max(0, content.bottom() - identity.bottom() - gap));
        } else {
            int identityWidth = Math.max(215, content.w() * 56 / 100);
            identity = new Rect(content.x(), content.y(), identityWidth, content.h());
            status = new Rect(identity.right() + gap, content.y(),
                    Math.max(0, content.right() - identity.right() - gap), content.h());
        }
        ExternalRpgUi.card(graphics, identity.x(), identity.y(), identity.w(), identity.h());
        ExternalRpgUi.card(graphics, status.x(), status.y(), status.w(), status.h());

        ExternalRpgUi.iconFrame(graphics, Items.PLAYER_HEAD, identity.x() + 10, identity.y() + 10, 34);
        graphics.text(font, Component.literal(shortText(value("player"), 28)), identity.x() + 53,
                identity.y() + 13, 0xFF34281F, false);
        graphics.text(font, Component.literal(shortText(value("citizenship"), 34)), identity.x() + 53,
                identity.y() + 27, 0xFF7A5C3C, false);
        int y = identity.y() + 52;
        int step = layout.compact() ? 13 : 17;
        y = row(graphics, identity, y, "종족", value("species"), step);
        y = row(graphics, identity, y, "소속", value("affiliation"), step);
        y = row(graphics, identity, y, "배경", value("background"), step);
        if (y + 9 < identity.bottom()) y = row(graphics, identity, y, "거주지", value("residence"), step);
        if (!layout.compact() && y + 9 < identity.bottom()) {
            y = row(graphics, identity, y, "현재 지역", value("region"), step);
            row(graphics, identity, y, "좌표", value("position"), step);
        }

        graphics.text(font, Component.literal(shortText(value("trait_title"), 24)), status.x() + 10,
                status.y() + 10, 0xFF3F6248, false);
        int barY = status.y() + (layout.compact() ? 28 : 43);
        if (!layout.compact()) {
            List<String> lines = wrap(value("trait_description"), Math.max(14, (status.w() - 20) / 6));
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.text(font, Component.literal(lines.get(i)), status.x() + 10,
                        status.y() + 25 + i * 11, 0xFF594536, false);
            }
            barY = status.y() + 51;
        }
        int stepY = Math.max(27, Math.min(34, Math.max(27, (status.h() - 73) / 3)));
        drawProgressIfFits(graphics, status, barY, "체력", value("health"), ratio(value("health")), 0xFF55845A);
        drawProgressIfFits(graphics, status, barY + stepY, "허기", value("food"), ratio(value("food")), 0xFFB58C43);
        drawProgressIfFits(graphics, status, barY + stepY * 2, "수배", value("wanted") + " / 100",
                parseInt("wanted") / 100.0F, 0xFF93483F);
        if (status.h() >= 26) {
            ExternalRpgUi.badge(graphics, font, status.x() + 10, status.bottom() - 23,
                    "LV " + value("level") + " · 기술 " + value("skill_points"), 0xFF3D6475);
        }
    }

    private void drawProgressIfFits(GuiGraphicsExtractor graphics, Rect card, int y,
                                    String label, String text, float ratio, int color) {
        if (y + 28 <= card.bottom() - 22) {
            ExternalRpgUi.progress(graphics, font, card.x() + 10, y, card.w() - 20,
                    label, text, ratio, color);
        }
    }

    private void drawEquipment(GuiGraphicsExtractor graphics, Layout layout) {
        Rect content = layout.content();
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, content, Items.IRON_CHESTPLATE, "장비와 법적 상태");
        Slot[] slots = {
                new Slot("주무기", "mainhand", Items.IRON_SWORD), new Slot("보조", "offhand", Items.SHIELD),
                new Slot("머리", "head", Items.IRON_HELMET), new Slot("몸통", "chest", Items.IRON_CHESTPLATE),
                new Slot("다리", "legs", Items.IRON_LEGGINGS), new Slot("발", "feet", Items.IRON_BOOTS)
        };
        int columns = content.w() >= 430 ? 3 : 2;
        int rows = (slots.length + columns - 1) / columns;
        int gap = 5;
        int lawHeight = Math.min(60, Math.max(42, content.h() / 4));
        int gridTop = content.y() + 39;
        int gridBottom = content.bottom() - lawHeight - 8;
        int slotWidth = (content.w() - 20 - gap * (columns - 1)) / columns;
        int slotHeight = Math.max(23, (gridBottom - gridTop - gap * (rows - 1)) / rows);
        for (int i = 0; i < slots.length; i++) {
            int column = i % columns;
            int row = i / columns;
            Rect slot = new Rect(content.x() + 10 + column * (slotWidth + gap),
                    gridTop + row * (slotHeight + gap), slotWidth, slotHeight);
            ExternalRpgUi.card(graphics, slot.x(), slot.y(), slot.w(), slot.h());
            ExternalRpgUi.itemIcon(graphics, slots[i].icon(), slot.x() + 5, slot.y() + Math.max(3, (slot.h() - 16) / 2));
            graphics.text(font, Component.literal(slots[i].label()), slot.x() + 26, slot.y() + 4, 0xFF806143, false);
            if (slot.h() >= 27) {
                graphics.text(font, Component.literal(shortText(value(slots[i].key()), Math.max(7, (slot.w() - 32) / 6))),
                        slot.x() + 26, slot.y() + 15, 0xFF32281F, false);
            }
        }
        Rect law = new Rect(content.x() + 10, content.bottom() - lawHeight, content.w() - 20, lawHeight - 7);
        ExternalRpgUi.card(graphics, law.x(), law.y(), law.w(), law.h());
        graphics.text(font, Component.literal("관할  " + shortText(value("jurisdiction"), 26)),
                law.x() + 8, law.y() + 8, 0xFF4D3B2C, false);
        graphics.text(font, Component.literal("수배 " + value("wanted") + " · 저항 " + value("resistance")
                        + " · 체포 " + value("arrest")), law.x() + 8, law.y() + 22, 0xFF6D4B38, false);
    }

    private void drawMap(GuiGraphicsExtractor graphics, Layout layout) {
        Rect content = layout.content();
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, content, Items.FILLED_MAP, "아르데시아 대륙 지도");
        mapViewport = new Rect(content.x() + 9, content.y() + 39, content.w() - 18, content.h() - 48);
        graphics.fill(mapViewport.x(), mapViewport.y(), mapViewport.right(), mapViewport.bottom(), 0xFF17232A);
        graphics.fill(mapViewport.x() + 2, mapViewport.y() + 2,
                mapViewport.right() - 2, mapViewport.bottom() - 2, 0xFF2C3D3A);

        for (int coordinate = WORLD_MIN; coordinate <= WORLD_MAX; coordinate += 4_000) {
            Point a = mapPoint(coordinate, WORLD_MIN);
            Point b = mapPoint(coordinate, WORLD_MAX);
            dottedLine(graphics, a, b, 0x553F5A58);
            a = mapPoint(WORLD_MIN, coordinate);
            b = mapPoint(WORLD_MAX, coordinate);
            dottedLine(graphics, a, b, 0x553F5A58);
        }
        for (int[] link : ROAD_LINKS) dottedLine(graphics,
                mapPoint(REGIONS.get(link[0])), mapPoint(REGIONS.get(link[1])), 0xFFB29A63);
        for (AtlasRegion region : REGIONS) drawRegion(graphics, region);

        marker(graphics, mapPoint(parseCoordinate("home_x", 0), parseCoordinate("home_z", 0)), 0xFFE7D078, 3);
        marker(graphics, mapPoint(currentCoordinate("player_x", 0), currentCoordinate("player_z", 0)), 0xFFFFFFFF, 4);
        graphics.text(font, Component.literal("드래그 이동 · 휠 확대 · 흰색 현재 위치 · 금색 거주지"),
                mapViewport.x() + 7, mapViewport.bottom() - 12, 0xFFE4D7B7, false);
        graphics.text(font, Component.literal(Math.round(mapZoom * 100) + "%"),
                mapViewport.right() - 33, mapViewport.y() + 6, 0xFFEBD9A8, false);
        graphics.text(font, Component.literal("N"), mapViewport.x() + 10, mapViewport.y() + 6, 0xFFF1DFB0, false);
    }

    private void drawRegion(GuiGraphicsExtractor graphics, AtlasRegion region) {
        Point point = mapPoint(region);
        if (!mapViewport.inset(4).contains(point.x(), point.y())) return;
        marker(graphics, point, region.color(), 4);
        String label = shortText(region.name(), 16);
        int x = clamp(point.x() + 6, mapViewport.x() + 3, mapViewport.right() - font.width(label) - 3);
        int y = clamp(point.y() - 4, mapViewport.y() + 3, mapViewport.bottom() - 22);
        graphics.text(font, Component.literal(label), x, y, 0xFFF2E7CA, true);
    }

    private void drawSkills(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Rect content = layout.content();
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, content, Items.ENCHANTED_BOOK,
                "기술 성장 · 남은 점수 " + value("skill_points"));
        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            Rect rect = layout.branchTab(i);
            ExternalRpgUi.button(graphics, font, rect.x(), rect.y(), rect.w(), rect.h(),
                    branch.label(), skillBranch.equals(branch.id()), rect.contains(mouseX, mouseY), true);
        }

        List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
        Set<String> unlocked = unlockedSkills();
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            Rect rect = layout.skillNode(i, nodes.size());
            boolean owned = unlocked.contains(node.id());
            boolean prerequisite = node.prerequisites().stream().allMatch(unlocked::contains);
            int cost = SkillTreeCatalog.effectiveCost(node, value("species_id"));
            boolean enabled = owned || prerequisite && parseInt("skill_points") >= cost;
            ExternalRpgUi.button(graphics, font, rect.x(), rect.y(), rect.w(), rect.h(), "",
                    owned, rect.contains(mouseX, mouseY), enabled);
            ExternalRpgUi.itemIcon(graphics, branchIcon(skillBranch), rect.x() + 7, rect.y() + 7);
            graphics.text(font, Component.literal(shortText(node.title(), Math.max(7, (rect.w() - 80) / 6))),
                    rect.x() + 29, rect.y() + 7, owned ? 0xFFF4E6BC : 0xFF3D3025, false);
            ExternalRpgUi.badge(graphics, font, rect.right() - 54, rect.y() + 3,
                    owned ? "해금" : cost + "점", owned ? 0xFF4B7252 : 0xFF775339);
            if (rect.h() >= 40) {
                List<String> lines = wrap(node.description(), Math.max(12, (rect.w() - 16) / 6));
                int maximumLines = Math.max(1, (rect.h() - 28) / 11);
                for (int line = 0; line < Math.min(maximumLines, lines.size()); line++) {
                    graphics.text(font, Component.literal(lines.get(line)), rect.x() + 8,
                            rect.y() + 25 + line * 11, owned ? 0xFFF4E8C9 : 0xFF66503C, false);
                }
            }
        }
    }

    private void sectionTitle(GuiGraphicsExtractor graphics, Rect card, Item icon, String title) {
        ExternalRpgUi.itemIcon(graphics, icon, card.x() + 10, card.y() + 9);
        graphics.text(font, Component.literal(shortText(title, Math.max(14, (card.w() - 42) / 6))),
                card.x() + 33, card.y() + 13, 0xFF443226, false);
        ExternalRpgUi.divider(graphics, card.x() + 33, card.y() + 29, card.w() - 44);
    }

    private int row(GuiGraphicsExtractor graphics, Rect card, int y, String label, String text, int step) {
        if (y + 9 >= card.bottom()) return y;
        graphics.text(font, Component.literal(label), card.x() + 10, y, 0xFF806143, false);
        graphics.text(font, Component.literal(shortText(text, Math.max(9, (card.w() - 88) / 6))),
                card.x() + 72, y, 0xFF342A21, false);
        return y + step;
    }

    private List<SkillTreeCatalog.SkillNode> branchNodes() {
        return SkillTreeCatalog.nodes().values().stream()
                .filter(node -> node.branch().equals(skillBranch)).toList();
    }

    private Set<String> unlockedSkills() {
        Set<String> result = new LinkedHashSet<>();
        String raw = value("unlocked_skills");
        if (raw.isBlank() || "-".equals(raw)) return result;
        for (String id : raw.split(",")) if (!id.isBlank()) result.add(id);
        return result;
    }

    private Item branchIcon(String branch) {
        return switch (branch) {
            case "exploration" -> Items.COMPASS;
            case "livelihood" -> Items.IRON_HOE;
            case "society" -> Items.EMERALD;
            case "arcana" -> Items.AMETHYST_SHARD;
            default -> Items.IRON_SWORD;
        };
    }

    private Point mapPoint(AtlasRegion region) {
        int x = region.xKey() == null ? region.defaultX() : parseCoordinate(region.xKey(), region.defaultX());
        int z = region.zKey() == null ? region.defaultZ() : parseCoordinate(region.zKey(), region.defaultZ());
        return mapPoint(x, z);
    }

    private Point mapPoint(int worldX, int worldZ) {
        float normalizedX = (worldX - WORLD_MIN) / (float) (WORLD_MAX - WORLD_MIN) - 0.5F;
        float normalizedZ = (worldZ - WORLD_MIN) / (float) (WORLD_MAX - WORLD_MIN) - 0.5F;
        return new Point(
                Math.round(mapViewport.x() + mapViewport.w() / 2.0F + normalizedX * mapViewport.w() * mapZoom + mapPanX),
                Math.round(mapViewport.y() + mapViewport.h() / 2.0F + normalizedZ * mapViewport.h() * mapZoom + mapPanY)
        );
    }

    private void dottedLine(GuiGraphicsExtractor graphics, Point a, Point b, int color) {
        int dx = b.x() - a.x();
        int dy = b.y() - a.y();
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
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

    private void clampMapPan() {
        float limitX = Math.max(70.0F, mapViewport.w() * Math.max(0.3F, mapZoom * 0.6F));
        float limitY = Math.max(50.0F, mapViewport.h() * Math.max(0.3F, mapZoom * 0.6F));
        mapPanX = clamp(mapPanX, -limitX, limitX);
        mapPanY = clamp(mapPanY, -limitY, limitY);
    }

    private Layout layout() {
        int panelWidth = Math.min(720, Math.max(276, width - 8));
        int panelHeight = Math.min(430, Math.max(164, height - 8));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 4));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 4));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        boolean compact = panelWidth < 560 || panelHeight < 315;
        int navigationWidth = compact ? 0 : 116;
        int contentX = compact ? left + 10 : left + navigationWidth + 6;
        int contentY = compact ? top + 77 : top + 54;
        int contentWidth = compact ? panelWidth - 20 : panelWidth - navigationWidth - 16;
        int contentHeight = Math.max(45, panelHeight - (contentY - top) - 10);
        return new Layout(left, top, panelWidth, panelHeight, compact, navigationWidth,
                new Rect(contentX, contentY, contentWidth, contentHeight));
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

    private int parseCoordinate(String key, int fallback) {
        try {
            return Integer.parseInt(value(key));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int currentCoordinate(String key, int fallback) {
        int direct = parseCoordinate(key, Integer.MIN_VALUE);
        if (direct != Integer.MIN_VALUE) return direct;
        String[] parts = value("position").split(",");
        if (parts.length < 3) return fallback;
        try {
            return Integer.parseInt(("player_x".equals(key) ? parts[0] : parts[2]).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private float ratio(String raw) {
        String[] parts = raw.split("/");
        if (parts.length != 2) return 0.0F;
        try {
            return clamp(Float.parseFloat(parts[0].trim()) /
                    Math.max(1.0F, Float.parseFloat(parts[1].trim())));
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    private static String normalizePage(String requested) {
        return switch (requested) {
            case "equipment", "map", "skills" -> requested;
            default -> "overview";
        };
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
            int end = Math.min(Math.max(1, maximum), remaining.length());
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

    private static boolean inside(Rect inner, Rect outer) {
        return inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record PageTab(String id, String label, Item icon) {
    }

    private record Slot(String label, String key, Item icon) {
    }

    private record AtlasRegion(String name, String xKey, String zKey, int defaultX, int defaultZ, int color) {
    }

    private record Point(int x, int y) {
    }

    record Rect(int x, int y, int w, int h) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);

        int right() {
            return x + w;
        }

        int bottom() {
            return y + h;
        }

        boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }

        Rect inset(int amount) {
            return new Rect(x + amount, y + amount,
                    Math.max(0, w - amount * 2), Math.max(0, h - amount * 2));
        }
    }

    private record Layout(int left, int top, int panelW, int panelH, boolean compact,
                          int navigationWidth, Rect content) {
        int right() {
            return left + panelW;
        }

        int bottom() {
            return top + panelH;
        }

        Rect window() {
            return new Rect(left, top, panelW, panelH);
        }

        Rect closeButton() {
            return new Rect(right() - 47, top + 11, 32, 29);
        }

        Rect pageTab(int index) {
            if (compact) {
                int gap = 4;
                int width = (panelW - 20 - gap * 3) / 4;
                return new Rect(left + 10 + index * (width + gap), top + 50, width, 24);
            }
            return new Rect(left + 10, top + 56 + index * 46, navigationWidth - 17, 38);
        }

        Rect branchTab(int index) {
            int gap = 4;
            int width = (content.w() - 18 - gap * 4) / 5;
            return new Rect(content.x() + 9 + index * (width + gap), content.y() + 35, width, 26);
        }

        Rect skillNode(int index, int count) {
            int nodeTop = content.y() + 66;
            int available = Math.max(25, content.bottom() - nodeTop - 8);
            int gap = 5;
            if (!compact && content.w() >= 520) {
                int width = (content.w() - 18 - gap * Math.max(0, count - 1)) / Math.max(1, count);
                return new Rect(content.x() + 9 + index * (width + gap), nodeTop, width, available);
            }
            int height = Math.max(20,
                    (available - gap * Math.max(0, count - 1)) / Math.max(1, count));
            return new Rect(content.x() + 9, nodeTop + index * (height + gap), content.w() - 18, height);
        }
    }
}

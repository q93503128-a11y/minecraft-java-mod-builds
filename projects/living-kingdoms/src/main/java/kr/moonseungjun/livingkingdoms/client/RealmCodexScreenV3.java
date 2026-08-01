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

/** Responsive codex with a bounded skill layout and a pannable, zoomable world atlas. */
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
            new AtlasRegion("에르덴 왕국", "erden_x", "erden_z", 0, 0, 0xFFD6B45C, Items.GOLDEN_HELMET),
            new AtlasRegion("실바나 수림 의회", "silvana_x", "silvana_z", -9_000, -1_500, 0xFF68A66B, Items.OAK_SAPLING),
            new AtlasRegion("카르둠 산악 연맹", "kardum_x", "kardum_z", -2_500, -9_000, 0xFF9AA1A8, Items.IRON_PICKAXE),
            new AtlasRegion("붉은 초원 연맹", null, null, 9_500, -1_000, 0xFFC9784E, Items.LEATHER),
            new AtlasRegion("벨라스 자유도시", null, null, 1_500, 7_500, 0xFF72A9C8, Items.EMERALD),
            new AtlasRegion("사하르 신정국", null, null, 9_000, 9_000, 0xFFE0C36D, Items.SANDSTONE),
            new AtlasRegion("회색 왕관 폐허", null, null, 8_500, -7_500, 0xFF817A86, Items.CRACKED_STONE_BRICKS),
            new AtlasRegion("북부 용의 변경", null, null, 0, -15_000, 0xFF8D6E9E, Items.DRAGON_BREATH),
            new AtlasRegion("서부 군도", null, null, -14_000, 7_000, 0xFF5E91B5, Items.OAK_BOAT)
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
    private Rect lastMapViewport = Rect.EMPTY;

    public RealmCodexScreenV3(String requestedPage, String snapshot) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("왕국 기록부"));
        this.page = normalizePage(requestedPage);
        this.data = parse(snapshot);
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
        if ("skills".equals(page)) {
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
    }

    private void rebuildWidgets() {
        clearWidgets();
        init();
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
        if (layout.left() < 2 || layout.top() < 2 || layout.right() > width - 2 || layout.bottom() > height - 2) {
            return false;
        }
        if (!inside(layout.closeButton(), layout.window()) || !inside(layout.content(), layout.window())) return false;
        if ("skills".equals(page)) {
            List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
            for (int i = 0; i < nodes.size(); i++) {
                if (!inside(layout.skillNode(i, nodes.size()), layout.content())) return false;
            }
        }
        return true;
    }

    boolean handleMapDrag(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        if (!"map".equals(page) || mouseButton != 0 || !lastMapViewport.contains(mouseX, mouseY)) return false;
        mapPanX += (float) dragX;
        mapPanY += (float) dragY;
        clampMapPan();
        return true;
    }

    boolean handleMapScroll(double mouseX, double mouseY, double scrollDeltaY) {
        if (!"map".equals(page) || !lastMapViewport.contains(mouseX, mouseY) || scrollDeltaY == 0.0) return false;
        float previous = mapZoom;
        mapZoom = clamp(mapZoom * (float) Math.pow(1.13, scrollDeltaY), 0.65F, 3.5F);
        if (previous != 0.0F) {
            float ratio = mapZoom / previous;
            mapPanX *= ratio;
            mapPanY *= ratio;
        }
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
                layout.left() + 22, layout.top() + 17);
        if (!layout.compact()) {
            ExternalRpgUi.badge(graphics, font, layout.left() + 164, layout.top() + 20,
                    shortText(value("affiliation"), 28), 0xFF49624E);
        }
        Rect close = layout.closeButton();
        ExternalRpgUi.button(graphics, font, close.x(), close.y(), close.w(), close.h(), "×",
                false, close.contains(mouseX, mouseY), true);
        ExternalRpgUi.divider(graphics, layout.left() + 18, layout.top() + 47, layout.panelW() - 36);
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
        int gap = 8;
        int leftWidth = layout.compact() ? content.w() : Math.max(210, content.w() * 56 / 100);
        Rect identity = layout.compact()
                ? new Rect(content.x(), content.y(), content.w(), Math.max(92, content.h() * 48 / 100))
                : new Rect(content.x(), content.y(), leftWidth, content.h());
        Rect status = layout.compact()
                ? new Rect(content.x(), identity.bottom() + gap, content.w(), content.bottom() - identity.bottom() - gap)
                : new Rect(identity.right() + gap, content.y(), content.right() - identity.right() - gap, content.h());
        ExternalRpgUi.card(graphics, identity.x(), identity.y(), identity.w(), identity.h());
        ExternalRpgUi.card(graphics, status.x(), status.y(), status.w(), status.h());

        ExternalRpgUi.iconFrame(graphics, Items.PLAYER_HEAD, identity.x() + 11, identity.y() + 11, 36);
        graphics.text(font, Component.literal(shortText(value("player"), 28)), identity.x() + 56,
                identity.y() + 14, 0xFF34281F, false);
        graphics.text(font, Component.literal(shortText(value("citizenship"), 34)), identity.x() + 56,
                identity.y() + 29, 0xFF7A5C3C, false);
        int y = identity.y() + 57;
        int rowStep = layout.compact() ? 14 : 18;
        y = row(graphics, identity, y, "종족", value("species"), rowStep);
        y = row(graphics, identity, y, "소속", value("affiliation"), rowStep);
        y = row(graphics, identity, y, "배경", value("background"), rowStep);
        if (!layout.compact() || y + rowStep < identity.bottom()) {
            y = row(graphics, identity, y, "거주지", value("residence"), rowStep);
        }
        if (!layout.compact() && y + rowStep < identity.bottom()) {
            y = row(graphics, identity, y, "현재 지역", value("region"), rowStep);
            row(graphics, identity, y, "좌표", value("position"), rowStep);
        }

        graphics.text(font, Component.literal(shortText(value("trait_title"), 26)), status.x() + 12,
                status.y() + 12, 0xFF3F6248, false);
        List<String> trait = wrap(value("trait_description"), Math.max(16, (status.w() - 24) / 6));
        int traitY = status.y() + 28;
        for (int i = 0; i < Math.min(layout.compact() ? 1 : 2, trait.size()); i++) {
            graphics.text(font, Component.literal(trait.get(i)), status.x() + 12, traitY, 0xFF594536, false);
            traitY += 11;
        }
        int barsTop = Math.max(status.y() + 44, traitY + 3);
        int available = Math.max(30, status.bottom() - barsTop - 28);
        int barStep = Math.max(27, Math.min(35, available / 3));
        ExternalRpgUi.progress(graphics, font, status.x() + 12, barsTop, status.w() - 24,
                "체력", value("health"), ratio(value("health")), 0xFF55845A);
        if (barsTop + barStep + 28 <= status.bottom()) {
            ExternalRpgUi.progress(graphics, font, status.x() + 12, barsTop + barStep, status.w() - 24,
                    "허기", value("food"), ratio(value("food")), 0xFFB58C43);
        }
        if (barsTop + barStep * 2 + 28 <= status.bottom()) {
            ExternalRpgUi.progress(graphics, font, status.x() + 12, barsTop + barStep * 2, status.w() - 24,
                    "수배", value("wanted") + " / 100", parseInt("wanted") / 100.0F, 0xFF93483F);
        }
        ExternalRpgUi.badge(graphics, font, status.x() + 12, status.bottom() - 24,
                "LV " + value("level") + " · 기술 " + value("skill_points"), 0xFF3D6475);
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
        int gridTop = content.y() + 43;
        int lawHeight = Math.min(64, Math.max(47, content.h() / 4));
        int gridHeight = Math.max(40, content.h() - 50 - lawHeight);
        int slotWidth = (content.w() - 24 - gap * (columns - 1)) / columns;
        int slotHeight = Math.max(28, (gridHeight - gap * (rows - 1)) / rows);
        for (int i = 0; i < slots.length; i++) {
            int column = i % columns;
            int row = i / columns;
            Rect slot = new Rect(content.x() + 12 + column * (slotWidth + gap),
                    gridTop + row * (slotHeight + gap), slotWidth, slotHeight);
            ExternalRpgUi.card(graphics, slot.x(), slot.y(), slot.w(), slot.h());
            ExternalRpgUi.itemIcon(graphics, slots[i].icon(), slot.x() + 6, slot.y() + Math.max(4, (slot.h() - 16) / 2));
            graphics.text(font, Component.literal(slots[i].label()), slot.x() + 27, slot.y() + 5, 0xFF806143, false);
            if (slot.h() >= 29) {
                graphics.text(font, Component.literal(shortText(value(slots[i].key()), Math.max(8, (slot.w() - 34) / 6))),
                        slot.x() + 27, slot.y() + 17, 0xFF32281F, false);
            }
        }
        Rect law = new Rect(content.x() + 12, content.bottom() - lawHeight - 9, content.w() - 24, lawHeight);
        ExternalRpgUi.card(graphics, law.x(), law.y(), law.w(), law.h());
        graphics.text(font, Component.literal("관할 " + shortText(value("jurisdiction"), 24)), law.x() + 9, law.y() + 9,
                0xFF4D3B2C, false);
        graphics.text(font, Component.literal("수배 " + value("wanted") + " / 100   저항 " + value("resistance")
                        + "   체포 " + value("arrest")), law.x() + 9, law.y() + 24, 0xFF6D4B38, false);
    }

    private void drawMap(GuiGraphicsExtractor graphics, Layout layout) {
        Rect content = layout.content();
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, content, Items.FILLED_MAP, "아르데시아 대륙 지도");
        Rect viewport = new Rect(content.x() + 10, content.y() + 42, content.w() - 20, content.h() - 54);
        lastMapViewport = viewport;
        graphics.fill(viewport.x(), viewport.y(), viewport.right(), viewport.bottom(), 0xFF17232A);
        graphics.fill(viewport.x() + 2, viewport.y() + 2, viewport.right() - 2, viewport.bottom() - 2, 0xFF2C3D3A);
        drawMapGrid(graphics, viewport);

        for (int[] link : ROAD_LINKS) {
            Point a = mapPoint(viewport, REGIONS.get(link[0]));
            Point b = mapPoint(viewport, REGIONS.get(link[1]));
            drawDottedLine(graphics, viewport, a.x(), a.y(), b.x(), b.y(), 0xFFB29A63);
        }
        for (AtlasRegion region : REGIONS) drawRegionMarker(graphics, viewport, region);

        Point home = mapPoint(viewport, parseCoordinate("home_x", 0), parseCoordinate("home_z", 0));
        marker(graphics, viewport, home.x(), home.y(), 0xFFE7D078, 3);
        Point player = mapPoint(viewport, currentCoordinate("player_x", 0), currentCoordinate("player_z", 0));
        marker(graphics, viewport, player.x(), player.y(), 0xFFFFFFFF, 4);

        graphics.text(font, Component.literal("드래그: 이동  ·  휠: 확대/축소  ·  흰색: 현재 위치  ·  금색: 거주지"),
                viewport.x() + 8, viewport.bottom() - 13, 0xFFE4D7B7, false);
        graphics.text(font, Component.literal(Math.round(mapZoom * 100) + "%"),
                viewport.right() - 34, viewport.y() + 7, 0xFFEBD9A8, false);
        drawCompass(graphics, viewport);
    }

    private void drawMapGrid(GuiGraphicsExtractor graphics, Rect viewport) {
        for (int coordinate = WORLD_MIN; coordinate <= WORLD_MAX; coordinate += 4_000) {
            Point verticalA = mapPoint(viewport, coordinate, WORLD_MIN);
            Point verticalB = mapPoint(viewport, coordinate, WORLD_MAX);
            drawDottedLine(graphics, viewport, verticalA.x(), verticalA.y(), verticalB.x(), verticalB.y(), 0x553F5A58);
            Point horizontalA = mapPoint(viewport, WORLD_MIN, coordinate);
            Point horizontalB = mapPoint(viewport, WORLD_MAX, coordinate);
            drawDottedLine(graphics, viewport, horizontalA.x(), horizontalA.y(), horizontalB.x(), horizontalB.y(), 0x553F5A58);
        }
    }

    private void drawCompass(GuiGraphicsExtractor graphics, Rect viewport) {
        int x = viewport.x() + 14;
        int y = viewport.y() + 17;
        graphics.fill(x - 1, y - 9, x + 1, y + 9, 0xFFD9C79B);
        graphics.fill(x - 9, y - 1, x + 9, y + 1, 0xFFD9C79B);
        graphics.text(font, Component.literal("N"), x - 3, y - 18, 0xFFF1DFB0, false);
    }

    private void drawRegionMarker(GuiGraphicsExtractor graphics, Rect viewport, AtlasRegion region) {
        Point point = mapPoint(viewport, region);
        if (!viewport.inset(4).contains(point.x(), point.y())) return;
        marker(graphics, viewport, point.x(), point.y(), region.color(), 4);
        String label = shortText(region.name(), 16);
        int labelX = clamp(point.x() + 6, viewport.x() + 3, viewport.right() - font.width(label) - 3);
        int labelY = clamp(point.y() - 4, viewport.y() + 3, viewport.bottom() - 25);
        graphics.text(font, Component.literal(label), labelX, labelY, 0xFFF2E7CA, true);
    }

    private void drawSkills(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Rect content = layout.content();
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, content, Items.ENCHANTED_BOOK, "기술 성장 · 남은 점수 " + value("skill_points"));
        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            Rect rect = layout.branchTab(i);
            ExternalRpgUi.button(graphics, font, rect.x(), rect.y(), rect.w(), rect.h(),
                    branch.label(), skillBranch.equals(branch.id()), rect.contains(mouseX, mouseY), true);
        }

        List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
        Set<String> unlocked = unlockedSkills();
        String species = value("species_id");
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            Rect rect = layout.skillNode(i, nodes.size());
            boolean owned = unlocked.contains(node.id());
            boolean prerequisite = node.prerequisites().stream().allMatch(unlocked::contains);
            int cost = SkillTreeCatalog.effectiveCost(node, species);
            boolean affordable = parseInt("skill_points") >= cost;
            ExternalRpgUi.button(graphics, font, rect.x(), rect.y(), rect.w(), rect.h(), "",
                    owned, rect.contains(mouseX, mouseY), owned || prerequisite && affordable);
            ExternalRpgUi.itemIcon(graphics, branchIcon(skillBranch), rect.x() + 8, rect.y() + 8);
            graphics.text(font, Component.literal(shortText(node.title(), Math.max(8, (rect.w() - 82) / 6))),
                    rect.x() + 31, rect.y() + 8, owned ? 0xFFF4E6BC : 0xFF3D3025, false);
            ExternalRpgUi.badge(graphics, font, rect.right() - 55, rect.y() + 4,
                    owned ? "해금" : cost + "점", owned ? 0xFF4B7252 : 0xFF775339);
            if (rect.h() >= 43) {
                List<String> lines = wrap(node.description(), Math.max(14, (rect.w() - 18) / 6));
                int maxLines = Math.max(1, (rect.h() - 31) / 11);
                for (int line = 0; line < Math.min(maxLines, lines.size()); line++) {
                    graphics.text(font, Component.literal(lines.get(line)), rect.x() + 9, rect.y() + 27 + line * 11,
                            owned ? 0xFFF4E8C9 : 0xFF66503C, false);
                }
            }
        }
    }

    private void sectionTitle(GuiGraphicsExtractor graphics, Rect card, Item icon, String title) {
        ExternalRpgUi.itemIcon(graphics, icon, card.x() + 12, card.y() + 10);
        graphics.text(font, Component.literal(shortText(title, Math.max(16, (card.w() - 45) / 6))),
                card.x() + 35, card.y() + 14, 0xFF443226, false);
        ExternalRpgUi.divider(graphics, card.x() + 35, card.y() + 31, card.w() - 48);
    }

    private int row(GuiGraphicsExtractor graphics, Rect card, int y, String label, String text, int step) {
        if (y + 9 >= card.bottom()) return y;
        graphics.text(font, Component.literal(label), card.x() + 12, y, 0xFF806143, false);
        graphics.text(font, Component.literal(shortText(text, Math.max(10, (card.w() - 94) / 6))),
                card.x() + 77, y, 0xFF342A21, false);
        return y + step;
    }

    private List<SkillTreeCatalog.SkillNode> branchNodes() {
        return SkillTreeCatalog.nodes().values().stream().filter(node -> node.branch().equals(skillBranch)).toList();
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

    private Point mapPoint(Rect viewport, AtlasRegion region) {
        int x = region.xKey() == null ? region.defaultX() : parseCoordinate(region.xKey(), region.defaultX());
        int z = region.zKey() == null ? region.defaultZ() : parseCoordinate(region.zKey(), region.defaultZ());
        return mapPoint(viewport, x, z);
    }

    private Point mapPoint(Rect viewport, int worldX, int worldZ) {
        float normalizedX = (worldX - WORLD_MIN) / (float) (WORLD_MAX - WORLD_MIN) - 0.5F;
        float normalizedZ = (worldZ - WORLD_MIN) / (float) (WORLD_MAX - WORLD_MIN) - 0.5F;
        int screenX = Math.round(viewport.x() + viewport.w() / 2.0F + normalizedX * viewport.w() * mapZoom + mapPanX);
        int screenY = Math.round(viewport.y() + viewport.h() / 2.0F + normalizedZ * viewport.h() * mapZoom + mapPanY);
        return new Point(screenX, screenY);
    }

    private void drawDottedLine(GuiGraphicsExtractor graphics, Rect clip,
                                int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i += 4) {
            int x = x1 + dx * i / steps;
            int y = y1 + dy * i / steps;
            if (clip.inset(2).contains(x, y)) graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private void marker(GuiGraphicsExtractor graphics, Rect clip, int x, int y, int color, int radius) {
        if (!clip.inset(radius).contains(x, y)) return;
        graphics.fill(x - radius, y - 1, x + radius + 1, y + 2, color);
        graphics.fill(x - 1, y - radius, x + 2, y + radius + 1, color);
    }

    private void clampMapPan() {
        int width = Math.max(120, lastMapViewport.w());
        int height = Math.max(80, lastMapViewport.h());
        float limitX = width * Math.max(0.25F, mapZoom * 0.6F);
        float limitY = height * Math.max(0.25F, mapZoom * 0.6F);
        mapPanX = clamp(mapPanX, -limitX, limitX);
        mapPanY = clamp(mapPanY, -limitY, limitY);
    }

    private Layout layout() {
        int panelWidth = Math.max(280, Math.min(720, width - 10));
        int panelHeight = Math.max(170, Math.min(430, height - 10));
        panelWidth = Math.min(panelWidth, Math.max(1, width - 4));
        panelHeight = Math.min(panelHeight, Math.max(1, height - 4));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        boolean compact = panelWidth < 560 || panelHeight < 315;
        int navWidth = compact ? 0 : 118;
        int contentX = compact ? left + 11 : left + navWidth + 7;
        int contentY = compact ? top + 79 : top + 56;
        int contentWidth = compact ? panelWidth - 22 : panelWidth - navWidth - 18;
        int contentHeight = Math.max(50, panelHeight - (contentY - top) - 11);
        return new Layout(left, top, panelWidth, panelHeight, compact, navWidth,
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
        String[] position = value("position").split(",");
        if (position.length >= 3) {
            try {
                return Integer.parseInt(("player_x".equals(key) ? position[0] : position[2]).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
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

    private record AtlasRegion(String name, String xKey, String zKey, int defaultX, int defaultZ,
                               int color, Item icon) {
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
            return new Rect(x + amount, y + amount, Math.max(0, w - amount * 2), Math.max(0, h - amount * 2));
        }
    }

    private record Layout(int left, int top, int panelW, int panelH, boolean compact, int navWidth,
                          Rect content) {
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
            return new Rect(right() - 49, top + 12, 34, 30);
        }

        Rect pageTab(int index) {
            if (compact) {
                int gap = 4;
                int width = (panelW - 22 - gap * 3) / 4;
                return new Rect(left + 11 + index * (width + gap), top + 52, width, 24);
            }
            return new Rect(left + 11, top + 58 + index * 47, navWidth - 18, 39);
        }

        Rect branchTab(int index) {
            int gap = 4;
            int width = (content.w() - 20 - gap * 4) / 5;
            return new Rect(content.x() + 10 + index * (width + gap), content.y() + 38, width, 27);
        }

        Rect skillNode(int index, int count) {
            int top = content.y() + 71;
            int bottomPadding = 9;
            int available = Math.max(30, content.bottom() - bottomPadding - top);
            int gap = 6;
            if (!compact && content.w() >= 520) {
                int width = (content.w() - 20 - gap * Math.max(0, count - 1)) / Math.max(1, count);
                return new Rect(content.x() + 10 + index * (width + gap), top, width, available);
            }
            int height = Math.max(24, (available - gap * Math.max(0, count - 1)) / Math.max(1, count));
            return new Rect(content.x() + 10, top + index * (height + gap), content.w() - 20, height);
        }
    }
}

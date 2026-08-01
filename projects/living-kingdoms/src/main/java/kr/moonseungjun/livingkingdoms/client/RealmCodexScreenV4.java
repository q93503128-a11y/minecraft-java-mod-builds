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

/** Responsive codex whose content regions never overlap, even when the GUI scale changes. */
public final class RealmCodexScreenV4 extends Screen {
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
    private static final String[] MASTERY_TRACKS = {
            MasteryProgressionSavedData.COMBAT,
            MasteryProgressionSavedData.DEFENSE,
            MasteryProgressionSavedData.MINING,
            MasteryProgressionSavedData.LOGGING,
            MasteryProgressionSavedData.FARMING,
            MasteryProgressionSavedData.GATHERING,
            MasteryProgressionSavedData.EXPLORATION
    };

    private final Map<String, String> data;
    private String page;
    private String skillBranch = "combat";

    public RealmCodexScreenV4(String requestedPage, String snapshot) {
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
        GrowthLayout growth = growthLayout(layout.content());
        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            invisible(growth.branchTab(i), () -> {
                skillBranch = branch.id();
                rebuildWidgets();
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
        if (rect.w() <= 2 || rect.h() <= 2) return;
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        ExternalRpgUi.dimWorld(graphics, width, height);
        ExternalRpgUi.window(graphics, layout.window().x(), layout.window().y(),
                layout.window().w(), layout.window().h());
        drawHeader(graphics, layout, mouseX, mouseY);
        drawNavigation(graphics, layout, mouseX, mouseY);
        switch (page) {
            case "equipment" -> drawEquipment(graphics, layout.content());
            case "map" -> drawMap(graphics, layout.content());
            case "skills" -> drawGrowth(graphics, layout.content(), mouseX, mouseY);
            default -> drawOverview(graphics, layout.content());
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Rect window = layout.window();
        ExternalRpgUi.title(graphics, font, "LIVING KINGDOMS", "왕국 기록부", window.x() + 20, window.y() + 14);
        Rect close = layout.closeButton();
        ExternalRpgUi.button(graphics, font, close.x(), close.y(), close.w(), close.h(), "×",
                false, close.contains(mouseX, mouseY), true);
        ExternalRpgUi.divider(graphics, window.x() + 18, window.y() + 46, window.w() - 36);
    }

    private void drawNavigation(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            Rect rect = layout.pageTab(i);
            ExternalRpgUi.iconButton(graphics, font, tab.icon(), rect.x(), rect.y(), rect.w(), rect.h(),
                    tab.label(), page.equals(tab.id()), rect.contains(mouseX, mouseY));
        }
    }

    private void drawOverview(GuiGraphicsExtractor graphics, Rect content) {
        int gap = 8;
        Rect identity;
        Rect status;
        if (content.w() >= 500) {
            int leftWidth = (content.w() - gap) * 56 / 100;
            identity = new Rect(content.x(), content.y(), leftWidth, content.h());
            status = new Rect(identity.right() + gap, content.y(), content.right() - identity.right() - gap, content.h());
        } else {
            int topHeight = (content.h() - gap) / 2;
            identity = new Rect(content.x(), content.y(), content.w(), topHeight);
            status = new Rect(content.x(), identity.bottom() + gap, content.w(), content.bottom() - identity.bottom() - gap);
        }
        ExternalRpgUi.card(graphics, identity.x(), identity.y(), identity.w(), identity.h());
        ExternalRpgUi.card(graphics, status.x(), status.y(), status.w(), status.h());

        ExternalRpgUi.iconFrame(graphics, Items.PLAYER_HEAD, identity.x() + 10, identity.y() + 10, 34);
        graphics.text(font, Component.literal(shortText(value("player"), 28)), identity.x() + 52,
                identity.y() + 12, 0xFF34281F, false);
        graphics.text(font, Component.literal(shortText(value("citizenship"), 34)), identity.x() + 52,
                identity.y() + 26, 0xFF7A5C3C, false);
        int y = identity.y() + 51;
        y = row(graphics, identity, y, "종족", value("species"));
        y = row(graphics, identity, y, "소속", value("affiliation"));
        y = row(graphics, identity, y, "배경", value("background"));
        if (y + 12 < identity.bottom()) y = row(graphics, identity, y, "거주지", value("residence"));
        if (y + 12 < identity.bottom()) row(graphics, identity, y, "현재 지역", value("region"));

        graphics.text(font, Component.literal(shortText(value("trait_title"), 28)), status.x() + 10,
                status.y() + 10, 0xFF3F6248, false);
        int textY = status.y() + 25;
        for (String line : wrap(value("trait_description"), Math.max(12, (status.w() - 20) / 6))) {
            if (textY + 10 >= status.bottom() - 83) break;
            graphics.text(font, Component.literal(line), status.x() + 10, textY, 0xFF594536, false);
            textY += 11;
        }
        int barWidth = status.w() - 20;
        int firstBar = Math.max(textY + 3, status.bottom() - 82);
        if (firstBar + 28 <= status.bottom()) {
            ExternalRpgUi.progress(graphics, font, status.x() + 10, firstBar, barWidth,
                    "체력", value("health"), ratio(value("health")), 0xFF55845A);
        }
        if (firstBar + 58 <= status.bottom()) {
            ExternalRpgUi.progress(graphics, font, status.x() + 10, firstBar + 30, barWidth,
                    "허기", value("food"), ratio(value("food")), 0xFFB58C43);
        }
        if (status.h() >= 24) {
            ExternalRpgUi.badge(graphics, font, status.x() + 10, status.bottom() - 23,
                    "숙련 성장 · 트리 점수 " + value("skill_points"), 0xFF3D6475);
        }
    }

    private void drawEquipment(GuiGraphicsExtractor graphics, Rect content) {
        EquipmentLayout layout = equipmentLayout(content);
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, layout.title(), Items.IRON_CHESTPLATE, "장비와 법적 상태");

        Slot[] slots = {
                new Slot("주무기", "mainhand", Items.IRON_SWORD),
                new Slot("보조", "offhand", Items.SHIELD),
                new Slot("머리", "head", Items.IRON_HELMET),
                new Slot("몸통", "chest", Items.IRON_CHESTPLATE),
                new Slot("다리", "legs", Items.IRON_LEGGINGS),
                new Slot("발", "feet", Items.IRON_BOOTS)
        };
        for (int i = 0; i < slots.length; i++) {
            Rect rect = layout.slot(i);
            if (rect.h() < 18) continue;
            ExternalRpgUi.card(graphics, rect.x(), rect.y(), rect.w(), rect.h());
            ExternalRpgUi.itemIcon(graphics, slots[i].icon(), rect.x() + 5,
                    rect.y() + Math.max(2, (rect.h() - 16) / 2));
            graphics.text(font, Component.literal(slots[i].label()), rect.x() + 27, rect.y() + 4,
                    0xFF7B5A39, false);
            if (rect.h() >= 31) {
                graphics.text(font, Component.literal(shortText(value(slots[i].key()), Math.max(8, (rect.w() - 34) / 6))),
                        rect.x() + 27, rect.y() + 17, 0xFF34281F, false);
            } else {
                String combined = slots[i].label() + " · " + shortText(value(slots[i].key()), 18);
                graphics.text(font, Component.literal(shortText(combined, Math.max(8, (rect.w() - 34) / 6))),
                        rect.x() + 27, rect.y() + 5, 0xFF34281F, false);
            }
        }

        Rect law = layout.law();
        ExternalRpgUi.card(graphics, law.x(), law.y(), law.w(), law.h());
        graphics.text(font, Component.literal("관할 · " + shortText(value("jurisdiction"), 28)),
                law.x() + 10, law.y() + 8, 0xFF5B4633, false);
        graphics.text(font, Component.literal("수배 " + value("wanted") + " · 저항 " + value("resistance")
                        + " · 체포 " + value("arrest")), law.x() + 10,
                law.y() + Math.min(24, Math.max(18, law.h() - 15)), 0xFF8A4D3E, false);
    }

    private void drawMap(GuiGraphicsExtractor graphics, Rect content) {
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, new Rect(content.x() + 9, content.y() + 8, content.w() - 18, 26),
                Items.FILLED_MAP, "대륙 지도");
        Rect map = new Rect(content.x() + 12, content.y() + 40, content.w() - 24, content.h() - 52);
        graphics.fill(map.x(), map.y(), map.right(), map.bottom(), 0xFFB8A77E);
        graphics.fill(map.x() + 4, map.y() + 4, map.right() - 4, map.bottom() - 4, 0xFF82996B);
        drawMapPoint(graphics, map, parseInt("erden_x"), parseInt("erden_z"), "에르덴", 0xFFD6B45C);
        drawMapPoint(graphics, map, parseInt("silvana_x"), parseInt("silvana_z"), "실바나", 0xFF68A66B);
        drawMapPoint(graphics, map, parseInt("kardum_x"), parseInt("kardum_z"), "카르둠", 0xFF9AA1A8);
        drawMapPoint(graphics, map, parseInt("home_x"), parseInt("home_z"), "집", 0xFFF2E4B5);
        drawMapPoint(graphics, map, parseInt("player_x"), parseInt("player_z"), "현재", 0xFFCA4E45);
        graphics.text(font, Component.literal("좌표 " + value("position") + " · " + shortText(value("region"), 28)),
                map.x() + 8, map.bottom() - 16, 0xFF2E2A20, false);
    }

    private void drawGrowth(GuiGraphicsExtractor graphics, Rect content, int mouseX, int mouseY) {
        GrowthLayout layout = growthLayout(content);
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, layout.masteryTitle(), Items.EXPERIENCE_BOTTLE, "행동 숙련 · 최대 레벨 없음");
        graphics.text(font, Component.literal(shortText(value("growth_rule"), Math.max(24, content.w() / 6))),
                layout.masteryTitle().x() + 154, layout.masteryTitle().y() + 8, 0xFF6D543A, false);

        for (int i = 0; i < MASTERY_TRACKS.length; i++) {
            String track = MASTERY_TRACKS[i];
            Rect rect = layout.mastery(i);
            if (rect.h() < 26) continue;
            String name = value("mastery_" + track + "_name");
            if (name.isBlank()) name = MasteryProgressionSavedData.displayName(track);
            String level = value("mastery_" + track + "_level");
            String xp = value("mastery_" + track + "_xp");
            ExternalRpgUi.progress(graphics, font, rect.x(), rect.y(), rect.w(),
                    name, "Lv." + level + " · " + xp + " XP",
                    parseFloat("mastery_" + track + "_progress"), masteryColor(i));
        }

        Rect tree = layout.tree();
        ExternalRpgUi.card(graphics, tree.x(), tree.y(), tree.w(), tree.h());
        graphics.text(font, Component.literal("보조 효과 트리"), tree.x() + 10, tree.y() + 8, 0xFF5B4633, false);
        graphics.text(font, Component.literal("숙련을 대체하지 않으며 특수 효과만 해금합니다."),
                tree.x() + 92, tree.y() + 8, 0xFF80664A, false);
        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            Rect tab = layout.branchTab(i);
            ExternalRpgUi.button(graphics, font, tab.x(), tab.y(), tab.w(), tab.h(), branch.label(),
                    skillBranch.equals(branch.id()), tab.contains(mouseX, mouseY), true);
        }

        Set<String> unlocked = unlocked();
        List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            Rect rect = layout.node(i, nodes.size());
            if (rect.w() <= 0 || rect.h() <= 0) continue;
            boolean owned = unlocked.contains(node.id());
            boolean available = prerequisitesMet(node, unlocked);
            ExternalRpgUi.card(graphics, rect.x(), rect.y(), rect.w(), rect.h());
            graphics.text(font, Component.literal(shortText(node.title(), 22)), rect.x() + 8, rect.y() + 6,
                    owned ? 0xFF3F7448 : available ? 0xFF4A3928 : 0xFF8A8177, false);
            if (rect.h() >= 34) {
                graphics.text(font, Component.literal(shortText(node.description(), Math.max(10, (rect.w() - 16) / 6))),
                        rect.x() + 8, rect.y() + 19, 0xFF5D4B39, false);
            }
            String state = owned ? "해금됨" : available ? "점수 " + node.cost() : "선행 필요";
            graphics.text(font, Component.literal(state), rect.x() + 8, rect.bottom() - 12,
                    owned ? 0xFF4C7C55 : available ? 0xFF8B6B37 : 0xFF8A4D43, false);
        }
    }

    private void drawMapPoint(GuiGraphicsExtractor graphics, Rect map, int worldX, int worldZ,
                              String label, int color) {
        int px = map.x() + 12 + Math.round((worldX + 16_000) / 32_000.0F * Math.max(1, map.w() - 24));
        int py = map.y() + 12 + Math.round((worldZ + 16_000) / 32_000.0F * Math.max(1, map.h() - 36));
        px = Math.max(map.x() + 5, Math.min(map.right() - 6, px));
        py = Math.max(map.y() + 5, Math.min(map.bottom() - 18, py));
        graphics.fill(px - 3, py - 3, px + 4, py + 4, 0xFF2B241C);
        graphics.fill(px - 2, py - 2, px + 3, py + 3, color);
        graphics.text(font, Component.literal(label), px + 6, py - 4, 0xFF2E281E, false);
    }

    private void sectionTitle(GuiGraphicsExtractor graphics, Rect rect, Item icon, String title) {
        ExternalRpgUi.itemIcon(graphics, icon, rect.x(), rect.y() + Math.max(0, (rect.h() - 16) / 2));
        graphics.text(font, Component.literal(title), rect.x() + 23, rect.y() + Math.max(3, (rect.h() - 8) / 2),
                0xFF5B4633, false);
        ExternalRpgUi.divider(graphics, rect.x() + 22, rect.bottom() - 2, Math.max(0, rect.w() - 22));
    }

    private int row(GuiGraphicsExtractor graphics, Rect card, int y, String label, String text) {
        if (y + 10 >= card.bottom()) return y;
        graphics.text(font, Component.literal(label), card.x() + 10, y, 0xFF806044, false);
        graphics.text(font, Component.literal(shortText(text, Math.max(8, (card.w() - 82) / 6))),
                card.x() + 70, y, 0xFF34281F, false);
        return y + 15;
    }

    boolean allRequiredControlsFit() {
        Layout layout = layout();
        Rect screen = new Rect(0, 0, width, height);
        if (!inside(layout.window(), screen) || !inside(layout.content(), layout.window())
                || !inside(layout.closeButton(), layout.window())) return false;
        for (int i = 0; i < PAGES.size(); i++) if (!inside(layout.pageTab(i), layout.window())) return false;

        if ("equipment".equals(page)) {
            EquipmentLayout equipment = equipmentLayout(layout.content());
            if (!inside(equipment.title(), layout.content()) || !inside(equipment.law(), layout.content())) return false;
            for (int i = 0; i < 6; i++) {
                Rect slot = equipment.slot(i);
                if (!inside(slot, layout.content()) || slot.intersects(equipment.law())) return false;
            }
        }
        if ("skills".equals(page)) {
            GrowthLayout growth = growthLayout(layout.content());
            if (!inside(growth.tree(), layout.content())) return false;
            for (int i = 0; i < BRANCHES.size(); i++) if (!inside(growth.branchTab(i), growth.tree())) return false;
            List<SkillTreeCatalog.SkillNode> nodes = branchNodes();
            for (int i = 0; i < nodes.size(); i++) if (!inside(growth.node(i, nodes.size()), growth.tree())) return false;
        }
        return true;
    }

    private Layout layout() {
        int panelW = Math.min(760, Math.max(300, width - 12));
        int panelH = Math.min(440, Math.max(190, height - 12));
        panelW = Math.min(panelW, Math.max(1, width - 4));
        panelH = Math.min(panelH, Math.max(1, height - 4));
        Rect window = new Rect((width - panelW) / 2, (height - panelH) / 2, panelW, panelH);
        int tabY = window.y() + 54;
        int tabGap = 5;
        int tabW = Math.max(1, (window.w() - 24 - tabGap * 3) / 4);
        Rect content = new Rect(window.x() + 12, tabY + 39,
                Math.max(0, window.w() - 24), Math.max(0, window.bottom() - 12 - (tabY + 39)));
        return new Layout(window, content, new Rect(window.right() - 52, window.y() + 11, 38, 31),
                tabY, tabW, tabGap);
    }

    private EquipmentLayout equipmentLayout(Rect content) {
        Rect inner = content.inset(9);
        int titleH = Math.min(30, Math.max(22, inner.h() / 8));
        Rect title = new Rect(inner.x(), inner.y(), inner.w(), titleH);
        int lawH = Math.min(58, Math.max(42, inner.h() / 5));
        Rect law = new Rect(inner.x(), inner.bottom() - lawH, inner.w(), lawH);
        int gridTop = title.bottom() + 5;
        int gridBottom = Math.max(gridTop, law.y() - 7);
        Rect grid = new Rect(inner.x(), gridTop, inner.w(), Math.max(0, gridBottom - gridTop));
        int columns = grid.w() >= 520 ? 3 : 2;
        int rows = (6 + columns - 1) / columns;
        int gap = 5;
        int slotW = Math.max(1, (grid.w() - gap * (columns - 1)) / columns);
        int slotH = Math.max(1, (grid.h() - gap * (rows - 1)) / rows);
        return new EquipmentLayout(title, grid, law, columns, rows, gap, slotW, slotH);
    }

    private GrowthLayout growthLayout(Rect content) {
        Rect inner = content.inset(9);
        int titleH = 27;
        Rect masteryTitle = new Rect(inner.x(), inner.y(), inner.w(), titleH);
        int treeH = Math.min(148, Math.max(88, inner.h() * 43 / 100));
        Rect tree = new Rect(inner.x(), inner.bottom() - treeH, inner.w(), treeH);
        Rect mastery = new Rect(inner.x(), masteryTitle.bottom() + 4, inner.w(),
                Math.max(0, tree.y() - 7 - (masteryTitle.bottom() + 4)));
        return new GrowthLayout(masteryTitle, mastery, tree);
    }

    private List<SkillTreeCatalog.SkillNode> branchNodes() {
        return SkillTreeCatalog.nodes().values().stream()
                .filter(node -> skillBranch.equals(node.branch()))
                .toList();
    }

    private boolean prerequisitesMet(SkillTreeCatalog.SkillNode node, Set<String> unlocked) {
        return unlocked.containsAll(node.prerequisites());
    }

    private Set<String> unlocked() {
        Set<String> result = new LinkedHashSet<>();
        for (String value : value("unlocked_skills").split(",")) {
            if (!value.isBlank()) result.add(value.trim());
        }
        return result;
    }

    private String value(String key) {
        return data.getOrDefault(key, "");
    }

    private int parseInt(String key) {
        try {
            return Integer.parseInt(value(key));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private float parseFloat(String key) {
        try {
            return Math.max(0.0F, Math.min(1.0F, Float.parseFloat(value(key))));
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    private float ratio(String value) {
        String first = value.split("/")[0].trim();
        String second = value.contains("/") ? value.substring(value.indexOf('/') + 1).trim() : "100";
        try {
            float max = Float.parseFloat(second);
            return max <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, Float.parseFloat(first) / max));
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    private int masteryColor(int index) {
        return switch (index) {
            case 0 -> 0xFF9A4D43;
            case 1 -> 0xFF526E8B;
            case 2 -> 0xFF747A83;
            case 3 -> 0xFF7A5A3D;
            case 4 -> 0xFF6D8A4B;
            case 5 -> 0xFF4E8063;
            default -> 0xFF4B758D;
        };
    }

    private static String normalizePage(String page) {
        return switch (page) {
            case "equipment", "map", "skills" -> page;
            default -> "overview";
        };
    }

    private static Map<String, String> parse(String snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        if (snapshot == null) return values;
        for (String line : snapshot.split("\\n")) {
            int separator = line.indexOf('\t');
            if (separator > 0) values.put(line.substring(0, separator), line.substring(separator + 1));
        }
        return values;
    }

    private static String shortText(String value, int maxCharacters) {
        if (value == null || value.isBlank()) return "없음";
        if (maxCharacters <= 1 || value.length() <= maxCharacters) return value;
        return value.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private static List<String> wrap(String value, int width) {
        if (value == null || value.isBlank()) return List.of("없음");
        int safe = Math.max(6, width);
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (int start = 0; start < value.length(); start += safe) {
            lines.add(value.substring(start, Math.min(value.length(), start + safe)));
        }
        return lines;
    }

    private static boolean inside(Rect child, Rect parent) {
        return child.x() >= parent.x() && child.y() >= parent.y()
                && child.right() <= parent.right() && child.bottom() <= parent.bottom();
    }

    private record PageTab(String id, String label, Item icon) {
    }

    private record Slot(String label, String key, Item icon) {
    }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
        boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }
        boolean intersects(Rect other) {
            return x < other.right() && right() > other.x() && y < other.bottom() && bottom() > other.y();
        }
        Rect inset(int amount) {
            return new Rect(x + amount, y + amount, Math.max(0, w - amount * 2), Math.max(0, h - amount * 2));
        }
    }

    private record Layout(Rect window, Rect content, Rect closeButton, int tabY, int tabW, int tabGap) {
        Rect pageTab(int index) {
            return new Rect(window.x() + 12 + index * (tabW + tabGap), tabY, tabW, 34);
        }
    }

    private record EquipmentLayout(Rect title, Rect grid, Rect law, int columns, int rows,
                                   int gap, int slotW, int slotH) {
        Rect slot(int index) {
            int column = index % columns;
            int row = index / columns;
            return new Rect(grid.x() + column * (slotW + gap), grid.y() + row * (slotH + gap), slotW, slotH);
        }
    }

    private record GrowthLayout(Rect masteryTitle, Rect masteryArea, Rect tree) {
        Rect mastery(int index) {
            int columns = masteryArea.w() >= 500 ? 2 : 1;
            int rows = (MASTERY_TRACKS.length + columns - 1) / columns;
            int gapX = 12;
            int gapY = 3;
            int width = Math.max(1, (masteryArea.w() - gapX * (columns - 1)) / columns);
            int height = Math.max(1, (masteryArea.h() - gapY * (rows - 1)) / rows);
            int column = index % columns;
            int row = index / columns;
            return new Rect(masteryArea.x() + column * (width + gapX),
                    masteryArea.y() + row * (height + gapY), width, height);
        }

        Rect branchTab(int index) {
            int gap = 3;
            int width = Math.max(1, (tree.w() - 20 - gap * (BRANCHES.size() - 1)) / BRANCHES.size());
            return new Rect(tree.x() + 10 + index * (width + gap), tree.y() + 23, width, 24);
        }

        Rect node(int index, int count) {
            int top = tree.y() + 52;
            int availableH = Math.max(0, tree.bottom() - 7 - top);
            int gap = 5;
            if (tree.w() >= 500) {
                int width = Math.max(1, (tree.w() - 20 - gap * (count - 1)) / Math.max(1, count));
                return new Rect(tree.x() + 10 + index * (width + gap), top, width, availableH);
            }
            int height = Math.max(1, (availableH - gap * (count - 1)) / Math.max(1, count));
            return new Rect(tree.x() + 10, top + index * (height + gap), tree.w() - 20, height);
        }
    }
}

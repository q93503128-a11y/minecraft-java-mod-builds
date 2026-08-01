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

/** Compact server-backed RPG codex composed from verified external UI components. */
public final class RealmCodexScreenV2 extends Screen {
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

    private final Map<String, String> data;
    private String page;
    private String skillBranch = "combat";

    public RealmCodexScreenV2(String requestedPage, String snapshot) {
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
        Layout layout = layout();
        return layout.left() >= 2 && layout.top() >= 2
                && layout.right() <= width - 2 && layout.bottom() <= height - 2
                && layout.closeButton().right() <= layout.right()
                && layout.content().bottom() <= layout.bottom();
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
                layout.left() + 24, layout.top() + 19);
        ExternalRpgUi.badge(graphics, font, layout.left() + 164, layout.top() + 22,
                shortText(value("affiliation"), 27), 0xFF49624E);
        Rect close = layout.closeButton();
        ExternalRpgUi.button(graphics, font, close.x(), close.y(), close.w(), close.h(), "×",
                false, inside(mouseX, mouseY, close), true);
        ExternalRpgUi.divider(graphics, layout.left() + 20, layout.top() + 48, layout.panelW() - 40);
    }

    private void drawNavigation(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            Rect rect = layout.pageTab(i);
            if (layout.compact()) {
                ExternalRpgUi.button(graphics, font, rect.x(), rect.y(), rect.w(), rect.h(),
                        tab.label(), page.equals(tab.id()), inside(mouseX, mouseY, rect), true);
                graphics.fakeItem(new net.minecraft.world.item.ItemStack(tab.icon()), rect.x() + 7, rect.y() + 5);
            } else {
                ExternalRpgUi.iconButton(graphics, font, tab.icon(), rect.x(), rect.y(), rect.w(), rect.h(),
                        tab.label(), page.equals(tab.id()), inside(mouseX, mouseY, rect));
            }
        }
    }

    private void drawOverview(GuiGraphicsExtractor graphics, Layout layout) {
        Rect content = layout.content();
        int gap = 9;
        int identityWidth = Math.max(180, content.w() * 56 / 100);
        Rect identity = new Rect(content.x(), content.y(), identityWidth, content.h());
        Rect status = new Rect(content.x() + identityWidth + gap, content.y(),
                content.w() - identityWidth - gap, content.h());
        ExternalRpgUi.card(graphics, identity.x(), identity.y(), identity.w(), identity.h());
        ExternalRpgUi.card(graphics, status.x(), status.y(), status.w(), status.h());

        ExternalRpgUi.iconFrame(graphics, Items.PLAYER_HEAD, identity.x() + 13, identity.y() + 14, 42);
        graphics.text(font, Component.literal(shortText(value("player"), 28)), identity.x() + 65,
                identity.y() + 17, 0xFF34281F, false);
        graphics.text(font, Component.literal(shortText(value("citizenship"), 31)), identity.x() + 65,
                identity.y() + 32, 0xFF7A5C3C, false);
        ExternalRpgUi.divider(graphics, identity.x() + 13, identity.y() + 64, identity.w() - 26);

        int y = identity.y() + 76;
        y = row(graphics, identity, y, "종족", value("species"));
        y = row(graphics, identity, y, "소속", value("affiliation"));
        y = row(graphics, identity, y, "배경", value("background"));
        y = row(graphics, identity, y, "거주지", value("residence"));
        y = row(graphics, identity, y, "현재 지역", value("region"));
        row(graphics, identity, y, "좌표", value("position"));

        ExternalRpgUi.iconFrame(graphics, speciesIcon(), status.x() + 13, status.y() + 14, 36);
        graphics.text(font, Component.literal(shortText(value("trait_title"), 25)), status.x() + 58,
                status.y() + 18, 0xFF3F6248, false);
        int descriptionY = status.y() + 45;
        for (String line : wrap(value("trait_description"), Math.max(14, (status.w() - 25) / 6))) {
            graphics.text(font, Component.literal(line), status.x() + 13, descriptionY, 0xFF594536, false);
            descriptionY += 11;
            if (descriptionY > status.y() + 76) break;
        }
        int barY = Math.max(status.y() + 87, descriptionY + 5);
        ExternalRpgUi.progress(graphics, font, status.x() + 13, barY, status.w() - 26,
                "체력", value("health"), ratio(value("health")), 0xFF55845A);
        ExternalRpgUi.progress(graphics, font, status.x() + 13, barY + 35, status.w() - 26,
                "허기", value("food"), ratio(value("food")), 0xFFB58C43);
        ExternalRpgUi.progress(graphics, font, status.x() + 13, barY + 70, status.w() - 26,
                "수배", value("wanted") + " / 100", parseInt("wanted") / 100.0F, 0xFF93483F);
        ExternalRpgUi.badge(graphics, font, status.x() + 13, status.bottom() - 31,
                "LV " + value("level"), 0xFF70523A);
        ExternalRpgUi.badge(graphics, font, status.x() + 76, status.bottom() - 31,
                "기술 " + value("skill_points"), 0xFF3D6475);
    }

    private void drawEquipment(GuiGraphicsExtractor graphics, Layout layout) {
        Rect content = layout.content();
        int upperHeight = Math.max(125, content.h() * 58 / 100);
        Rect equipment = new Rect(content.x(), content.y(), content.w(), upperHeight);
        Rect law = new Rect(content.x(), content.y() + upperHeight + 8,
                content.w(), content.h() - upperHeight - 8);
        ExternalRpgUi.card(graphics, equipment.x(), equipment.y(), equipment.w(), equipment.h());
        ExternalRpgUi.card(graphics, law.x(), law.y(), law.w(), law.h());
        sectionTitle(graphics, equipment, Items.IRON_CHESTPLATE, "장비 기록");
        sectionTitle(graphics, law, Items.WRITABLE_BOOK, "법적 상태와 관할");

        Slot[] slots = {
                new Slot("주무기", "mainhand", Items.IRON_SWORD),
                new Slot("보조", "offhand", Items.SHIELD),
                new Slot("머리", "head", Items.IRON_HELMET),
                new Slot("몸통", "chest", Items.IRON_CHESTPLATE),
                new Slot("다리", "legs", Items.IRON_LEGGINGS),
                new Slot("발", "feet", Items.IRON_BOOTS)
        };
        int slotGap = 6;
        int slotWidth = (equipment.w() - 26 - slotGap * 2) / 3;
        int slotHeight = Math.max(37, (equipment.h() - 57) / 2);
        for (int i = 0; i < slots.length; i++) {
            int column = i % 3;
            int row = i / 3;
            Rect slotRect = new Rect(equipment.x() + 13 + column * (slotWidth + slotGap),
                    equipment.y() + 45 + row * (slotHeight + 5), slotWidth, slotHeight);
            ExternalRpgUi.card(graphics, slotRect.x(), slotRect.y(), slotRect.w(), slotRect.h());
            graphics.fakeItem(new net.minecraft.world.item.ItemStack(slots[i].icon()),
                    slotRect.x() + 7, slotRect.y() + Math.max(5, (slotRect.h() - 16) / 2));
            graphics.text(font, Component.literal(slots[i].label()), slotRect.x() + 29, slotRect.y() + 7,
                    0xFF806143, false);
            graphics.text(font, Component.literal(shortText(value(slots[i].key()), Math.max(8, (slotRect.w() - 35) / 6))),
                    slotRect.x() + 29, slotRect.y() + 20, 0xFF32281F, false);
        }

        int y = law.y() + 44;
        y = dualRow(graphics, law, y, "수배도", value("wanted") + " / 100", "저항 단계", value("resistance"));
        y = dualRow(graphics, law, y, "관할", value("jurisdiction"), "체포 진행", value("arrest"));
        graphics.text(font, Component.literal("경비의 추격·제압·호송 절차가 적용됩니다."),
                law.x() + 13, Math.min(law.bottom() - 16, y + 2), 0xFF66503C, false);
    }

    private void drawMap(GuiGraphicsExtractor graphics, Layout layout) {
        Rect content = layout.content();
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, content, Items.FILLED_MAP, "대륙 지도 기록");
        graphics.text(font, Component.literal("서버가 조사한 실제 수도 좌표와 현재 위치를 표시합니다."),
                content.x() + 51, content.y() + 28, 0xFF6E563F, false);

        RegionCard[] regions = {
                new RegionCard("카르둠 산악 연맹", "kardum_x", "kardum_z", Items.IRON_PICKAXE),
                new RegionCard("에르덴 왕국", "erden_x", "erden_z", Items.GOLDEN_HELMET),
                new RegionCard("실바나 수림 의회", "silvana_x", "silvana_z", Items.OAK_SAPLING)
        };
        int gap = 7;
        boolean horizontal = content.w() >= 430;
        for (int i = 0; i < regions.length; i++) {
            Rect card;
            if (horizontal) {
                int cardWidth = (content.w() - 26 - gap * 2) / 3;
                card = new Rect(content.x() + 13 + i * (cardWidth + gap), content.y() + 58,
                        cardWidth, Math.max(72, content.h() - 126));
            } else {
                int cardHeight = Math.max(48, (content.h() - 111 - gap * 2) / 3);
                card = new Rect(content.x() + 13, content.y() + 55 + i * (cardHeight + gap),
                        content.w() - 26, cardHeight);
            }
            drawRegionCard(graphics, card, regions[i]);
        }

        int footerY = content.bottom() - 56;
        ExternalRpgUi.divider(graphics, content.x() + 14, footerY, content.w() - 28);
        graphics.fakeItem(new net.minecraft.world.item.ItemStack(Items.COMPASS), content.x() + 18, footerY + 9);
        graphics.text(font, Component.literal("현재  " + shortText(value("region") + " · " + value("position"), 45)),
                content.x() + 41, footerY + 11, 0xFF3F3026, false);
        graphics.fakeItem(new net.minecraft.world.item.ItemStack(Items.CHEST), content.x() + 18, footerY + 29);
        graphics.text(font, Component.literal("거주지  " + value("home_x") + ", " + value("home_z")),
                content.x() + 41, footerY + 31, 0xFF3F3026, false);
    }

    private void drawRegionCard(GuiGraphicsExtractor graphics, Rect card, RegionCard region) {
        ExternalRpgUi.card(graphics, card.x(), card.y(), card.w(), card.h());
        ExternalRpgUi.iconFrame(graphics, region.icon(), card.x() + 8, card.y() + 8, 34);
        int textX = card.x() + 49;
        graphics.text(font, Component.literal(shortText(region.name(), Math.max(10, (card.w() - 56) / 6))),
                textX, card.y() + 11, 0xFF3E3025, false);
        graphics.text(font, Component.literal("X " + value(region.xKey()) + "  Z " + value(region.zKey())),
                textX, card.y() + 27, 0xFF75583C, false);
        if (card.h() >= 67) {
            graphics.text(font, Component.literal(regionDescription(region.xKey())),
                    card.x() + 10, card.bottom() - 18, 0xFF6A513A, false);
        }
    }

    private void drawSkills(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        Rect content = layout.content();
        ExternalRpgUi.card(graphics, content.x(), content.y(), content.w(), content.h());
        sectionTitle(graphics, content, Items.ENCHANTED_BOOK, "기술 성장");
        ExternalRpgUi.badge(graphics, font, content.right() - 132, content.y() + 13,
                "남은 점수 " + value("skill_points"), 0xFF3D6475);

        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            Rect rect = layout.branchTab(i);
            ExternalRpgUi.button(graphics, font, rect.x(), rect.y(), rect.w(), rect.h(),
                    branch.label(), skillBranch.equals(branch.id()), inside(mouseX, mouseY, rect), true);
        }

        List<SkillTreeCatalog.SkillNode> nodes = SkillTreeCatalog.nodes().values().stream()
                .filter(node -> node.branch().equals(skillBranch)).toList();
        Set<String> unlocked = unlockedSkills();
        String species = value("species_id");
        for (int i = 0; i < nodes.size(); i++) {
            SkillTreeCatalog.SkillNode node = nodes.get(i);
            Rect rect = layout.skillNode(i);
            boolean owned = unlocked.contains(node.id());
            boolean prerequisite = node.prerequisites().stream().allMatch(unlocked::contains);
            int cost = SkillTreeCatalog.effectiveCost(node, species);
            boolean affordable = parseInt("skill_points") >= cost;
            ExternalRpgUi.button(graphics, font, rect.x(), rect.y(), rect.w(), rect.h(), "",
                    owned, inside(mouseX, mouseY, rect), owned || prerequisite && affordable);
            graphics.fakeItem(new net.minecraft.world.item.ItemStack(branchIcon(skillBranch)),
                    rect.x() + 9, rect.y() + 9);
            graphics.text(font, Component.literal(shortText(node.title(), Math.max(10, (rect.w() - 78) / 6))),
                    rect.x() + 34, rect.y() + 9, owned ? 0xFFF4E6BC : 0xFF3D3025, false);
            ExternalRpgUi.badge(graphics, font, rect.right() - 56, rect.y() + 6,
                    owned ? "해금" : cost + "점", owned ? 0xFF4B7252 : 0xFF775339);
            int textY = rect.y() + 27;
            for (String line : wrap(node.description(), Math.max(15, (rect.w() - 18) / 6))) {
                graphics.text(font, Component.literal(line), rect.x() + 10, textY,
                        owned ? 0xFFF4E8C9 : 0xFF66503C, false);
                textY += 11;
                if (textY > rect.bottom() - 10) break;
            }
        }
        graphics.text(font, Component.literal("경험치 레벨 5마다 기술 점수 1점을 얻습니다."),
                content.x() + 13, content.bottom() - 15, 0xFF6B513A, false);
    }

    private void sectionTitle(GuiGraphicsExtractor graphics, Rect card, Item icon, String title) {
        ExternalRpgUi.iconFrame(graphics, icon, card.x() + 11, card.y() + 10, 30);
        graphics.text(font, Component.literal(title), card.x() + 49, card.y() + 17, 0xFF443226, false);
        ExternalRpgUi.divider(graphics, card.x() + 49, card.y() + 33, card.w() - 62);
    }

    private int row(GuiGraphicsExtractor graphics, Rect card, int y, String label, String text) {
        graphics.text(font, Component.literal(label), card.x() + 14, y, 0xFF806143, false);
        graphics.text(font, Component.literal(shortText(text, Math.max(10, (card.w() - 104) / 6))),
                card.x() + 91, y, 0xFF342A21, false);
        return y + 18;
    }

    private int dualRow(GuiGraphicsExtractor graphics, Rect card, int y,
                        String leftLabel, String leftValue, String rightLabel, String rightValue) {
        int half = card.w() / 2;
        graphics.text(font, Component.literal(leftLabel), card.x() + 13, y, 0xFF806143, false);
        graphics.text(font, Component.literal(shortText(leftValue, 16)), card.x() + 74, y, 0xFF342A21, false);
        graphics.text(font, Component.literal(rightLabel), card.x() + half, y, 0xFF806143, false);
        graphics.text(font, Component.literal(shortText(rightValue, 16)), card.x() + half + 68, y, 0xFF342A21, false);
        return y + 18;
    }

    private Item speciesIcon() {
        return switch (value("species_id")) {
            case "elf" -> Items.AMETHYST_SHARD;
            case "dwarf" -> Items.IRON_PICKAXE;
            default -> Items.PLAYER_HEAD;
        };
    }

    private static Item branchIcon(String branch) {
        return switch (branch) {
            case "exploration" -> Items.COMPASS;
            case "livelihood" -> Items.IRON_HOE;
            case "society" -> Items.EMERALD;
            case "arcana" -> Items.AMETHYST_SHARD;
            default -> Items.IRON_SWORD;
        };
    }

    private static String regionDescription(String xKey) {
        return switch (xKey) {
            case "kardum_x" -> "산악·광업권";
            case "silvana_x" -> "삼림·마력권";
            default -> "도시·농경권";
        };
    }

    private Layout layout() {
        int panelWidth = Math.min(640, Math.max(310, width - 28));
        int panelHeight = Math.min(350, Math.max(235, height - 24));
        panelWidth = Math.min(panelWidth, width - 8);
        panelHeight = Math.min(panelHeight, height - 8);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        boolean compact = panelWidth < 520 || panelHeight < 300;
        int navigationWidth = compact ? 0 : 108;
        int contentX = compact ? left + 13 : left + navigationWidth + 7;
        int contentY = compact ? top + 82 : top + 58;
        int contentWidth = compact ? panelWidth - 26 : panelWidth - navigationWidth - 20;
        int contentHeight = panelHeight - (contentY - top) - 13;
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

    private static boolean inside(int x, int y, Rect rect) {
        return x >= rect.x() && y >= rect.y() && x < rect.right() && y < rect.bottom();
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

    private record PageTab(String id, String label, Item icon) {}
    private record Slot(String label, String key, Item icon) {}
    private record RegionCard(String name, String xKey, String zKey, Item icon) {}
    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }

    private record Layout(int left, int top, int panelW, int panelH,
                          boolean compact, int navW, Rect content) {
        int right() { return left + panelW; }
        int bottom() { return top + panelH; }
        Rect closeButton() { return new Rect(right() - 47, top + 16, 28, 26); }
        Rect pageTab(int index) {
            if (compact) {
                int available = panelW - 28;
                int gap = 4;
                int tabWidth = (available - gap * 3) / PAGES.size();
                return new Rect(left + 14 + index * (tabWidth + gap), top + 52, tabWidth, 25);
            }
            return new Rect(left + 14, top + 62 + index * 49, navW - 27, 40);
        }
        Rect branchTab(int index) {
            int gap = 4;
            int tabWidth = (content.w() - 26 - gap * 4) / BRANCHES.size();
            return new Rect(content.x() + 13 + index * (tabWidth + gap), content.y() + 43, tabWidth, 25);
        }
        Rect skillNode(int index) {
            int nodeTop = content.y() + 76;
            int availableHeight = content.h() - 100;
            if (content.w() >= 430) {
                int gap = 8;
                int nodeWidth = (content.w() - 26 - gap * 2) / 3;
                return new Rect(content.x() + 13 + index * (nodeWidth + gap), nodeTop,
                        nodeWidth, Math.max(70, availableHeight));
            }
            int gap = 5;
            int nodeHeight = Math.max(45, (availableHeight - gap * 2) / 3);
            return new Rect(content.x() + 13, nodeTop + index * (nodeHeight + gap),
                    content.w() - 26, nodeHeight);
        }
    }
}

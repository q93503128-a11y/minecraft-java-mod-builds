package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.UnlockSkillPayload;
import kr.moonseungjun.livingkingdoms.skill.SkillTreeCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Readable four-page RPG codex for identity, equipment, atlas and skills. */
public final class RealmCodexScreenV2 extends Screen {
    private static final List<PageTab> PAGES = List.of(
            new PageTab("overview", "인물 개요"),
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
        this.page = normalizePage(requestedPage);
        this.data = parse(snapshot);
    }

    @Override
    protected void init() {
        super.init();
        Layout l = layout();
        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            Rect r = l.pageTab(i);
            invisible(r, () -> page = tab.id());
        }
        invisible(l.closeButton(), this::onClose);

        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            Rect r = l.branchTab(i);
            invisible(r, () -> {
                if ("skills".equals(page)) skillBranch = branch.id();
            });
        }

        Map<String, Integer> branchIndex = new LinkedHashMap<>();
        for (SkillTreeCatalog.SkillNode node : SkillTreeCatalog.nodes().values()) {
            int index = branchIndex.getOrDefault(node.branch(), 0);
            branchIndex.put(node.branch(), index + 1);
            Rect r = l.skillNode(index);
            invisible(r, () -> {
                if ("skills".equals(page) && skillBranch.equals(node.branch())) {
                    ClientPacketDistributor.sendToServer(new UnlockSkillPayload(node.id()));
                }
            });
        }
    }

    private void invisible(Rect r, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> action.run())
                .bounds(r.x(), r.y(), r.w(), r.h()).build());
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
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0xC40A0D12);
        panel(g, l.left() - 5, l.top() - 5, l.panelW() + 10, l.panelH() + 10, 0xFF20150E, 0xFF81552F);
        g.fill(l.left(), l.top(), l.right(), l.bottom(), 0xFFF0DFC0);
        g.fill(l.left(), l.top(), l.right(), l.top() + 38, 0xFF39271D);
        g.fill(l.left(), l.top() + 38, l.right(), l.top() + 40, 0xFFC9A45F);

        g.text(font, Component.literal("LIVING KINGDOMS"), l.left() + 14, l.top() + 8, 0xFFD5B56D);
        g.text(font, Component.literal("왕국 수첩"), l.left() + 14, l.top() + 20, 0xFFFFF0C8);
        badge(g, l.left() + 108, l.top() + 13, shortText(value("affiliation"), 27), 0xFF45604C);
        customButton(g, l.closeButton(), "×", inside(mouseX, mouseY, l.closeButton()), false);

        for (int i = 0; i < PAGES.size(); i++) {
            PageTab tab = PAGES.get(i);
            Rect r = l.pageTab(i);
            customButton(g, r, tab.label(), page.equals(tab.id()), true);
        }

        switch (page) {
            case "equipment" -> drawEquipment(g, l);
            case "map" -> drawMap(g, l);
            case "skills" -> drawSkills(g, l, mouseX, mouseY);
            default -> drawOverview(g, l);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawOverview(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        int gap = 8;
        int leftW = Math.max(150, (c.w() * 55) / 100);
        Rect identity = new Rect(c.x(), c.y(), leftW, c.h());
        Rect right = new Rect(c.x() + leftW + gap, c.y(), c.w() - leftW - gap, c.h());
        card(g, identity, "신분과 소속");
        card(g, right, "종족 특성과 현재 상태");

        int y = identity.y() + 25;
        largeValue(g, identity.x() + 10, y, value("player"), 0xFF332319); y += 17;
        infoRow(g, identity.x() + 10, y, identity.w() - 20, "종족", value("species")); y += 15;
        infoRow(g, identity.x() + 10, y, identity.w() - 20, "기본 소속", value("affiliation")); y += 15;
        infoRow(g, identity.x() + 10, y, identity.w() - 20, "신분", value("citizenship")); y += 15;
        infoRow(g, identity.x() + 10, y, identity.w() - 20, "배경", value("background")); y += 15;
        infoRow(g, identity.x() + 10, y, identity.w() - 20, "거주지", value("residence")); y += 15;
        infoRow(g, identity.x() + 10, y, identity.w() - 20, "현재 지역", value("region")); y += 15;
        infoRow(g, identity.x() + 10, y, identity.w() - 20, "좌표", value("position"));

        y = right.y() + 25;
        g.text(font, Component.literal(value("trait_title")), right.x() + 10, y, 0xFF3F664D); y += 14;
        for (String line : wrap(value("trait_description"), Math.max(16, right.w() / 7))) {
            g.text(font, Component.literal(line), right.x() + 10, y, 0xFF5C4635); y += 11;
        }
        y += 5;
        progressBar(g, right.x() + 10, y, right.w() - 20, "체력", value("health"), ratio(value("health"))); y += 27;
        progressBar(g, right.x() + 10, y, right.w() - 20, "허기", value("food"), ratio(value("food"))); y += 27;
        progressBar(g, right.x() + 10, y, right.w() - 20, "수배", value("wanted") + " / 100", parseInt("wanted") / 100.0F); y += 29;
        badge(g, right.x() + 10, y, "LV " + value("level"), 0xFF6A4B2D);
        badge(g, right.x() + 64, y, "기술 점수 " + value("skill_points"), 0xFF315A68);
    }

    private void drawEquipment(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        int gap = 8;
        int topH = Math.max(86, c.h() / 2);
        Rect equipment = new Rect(c.x(), c.y(), c.w(), topH);
        Rect law = new Rect(c.x(), c.y() + topH + gap, c.w(), c.h() - topH - gap);
        card(g, equipment, "장비 기록");
        card(g, law, "법적 상태와 관할");

        String[][] slots = {
                {"주무기", value("mainhand")}, {"보조", value("offhand")},
                {"머리", value("head")}, {"몸통", value("chest")},
                {"다리", value("legs")}, {"발", value("feet")}
        };
        int colW = (equipment.w() - 28) / 3;
        for (int i = 0; i < slots.length; i++) {
            int col = i % 3;
            int row = i / 3;
            Rect slot = new Rect(equipment.x() + 10 + col * (colW + 4), equipment.y() + 25 + row * 34,
                    colW, 29);
            g.fill(slot.x(), slot.y(), slot.right(), slot.bottom(), 0xFFD5BD91);
            g.fill(slot.x() + 2, slot.y() + 2, slot.right() - 2, slot.bottom() - 2, 0xFFF3E6C8);
            g.text(font, Component.literal(slots[i][0]), slot.x() + 5, slot.y() + 4, 0xFF816246);
            g.text(font, Component.literal(shortText(slots[i][1], Math.max(8, slot.w() / 6))),
                    slot.x() + 5, slot.y() + 16, 0xFF2F2922);
        }

        int y = law.y() + 25;
        infoRow(g, law.x() + 10, y, law.w() / 2 - 14, "수배도", value("wanted") + " / 100");
        infoRow(g, law.x() + law.w() / 2, y, law.w() / 2 - 10, "저항 단계", value("resistance")); y += 16;
        infoRow(g, law.x() + 10, y, law.w() / 2 - 14, "관할", value("jurisdiction"));
        infoRow(g, law.x() + law.w() / 2, y, law.w() / 2 - 10, "체포 진행", value("arrest")); y += 20;
        g.text(font, Component.literal("범죄는 즉시 순간이동 처벌이 아니라 경비 추격·제압·호송 절차를 거칩니다."),
                law.x() + 10, y, 0xFF5D4633);
    }

    private void drawMap(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        g.fill(c.x(), c.y(), c.right(), c.bottom(), 0xFFB8C7B6);
        g.fill(c.x() + 3, c.y() + 3, c.right() - 3, c.bottom() - 3, 0xFFE7D7AE);

        int mapLeft = c.x() + 12;
        int mapTop = c.y() + 12;
        int mapW = c.w() - 24;
        int mapH = c.h() - 42;
        g.fill(mapLeft, mapTop, mapLeft + mapW, mapTop + mapH, 0xFFD7C79D);

        terrainMass(g, mapLeft + mapW / 2, mapTop + mapH / 2, mapW / 2 - 12, mapH / 2 - 8);
        river(g, mapLeft + mapW / 2, mapTop + 8, mapLeft + mapW / 2 - 20, mapTop + mapH - 8);
        mountains(g, mapLeft + 35, mapTop + mapH / 2, 6);
        forests(g, mapLeft + mapW - 65, mapTop + mapH / 2, 8);

        int erdenX = mapLeft + mapW / 2;
        int erdenY = mapTop + mapH / 2;
        int kardumX = mapLeft + 55;
        int silvanaX = mapLeft + mapW - 65;
        roadLine(g, kardumX, erdenY, erdenX, erdenY, 0xFF865E3A);
        roadLine(g, erdenX, erdenY, silvanaX, erdenY, 0xFF865E3A);
        settlement(g, kardumX, erdenY, "카르둠 연맹", 0xFF57514B);
        settlement(g, erdenX, erdenY, "에르덴 왕국", 0xFF536D3A);
        settlement(g, silvanaX, erdenY, "실바나 수림", 0xFF2E6A47);

        district(g, erdenX - 24, erdenY + 27, "로엔 변경도시");
        district(g, erdenX + 32, erdenY + 16, "농경지");
        district(g, erdenX - 35, erdenY - 20, "강항구");

        int px = mapWorldX(parseInt("player_x"), mapLeft, mapW);
        int pz = mapWorldZ(parseInt("player_z"), mapTop, mapH);
        int hx = mapWorldX(parseInt("home_x"), mapLeft, mapW);
        int hz = mapWorldZ(parseInt("home_z"), mapTop, mapH);
        marker(g, hx, hz, 0xFF2F6588, 4);
        marker(g, px, pz, 0xFFD45138, 5);

        int legendY = c.bottom() - 23;
        g.fill(c.x() + 8, legendY - 3, c.right() - 8, c.bottom() - 7, 0xDDF7EACB);
        g.text(font, Component.literal("빨강: 현재 위치   파랑: 거주지   갈색: 주요 교역로"),
                c.x() + 14, legendY, 0xFF473529);
        g.text(font, Component.literal(shortText(value("region") + " · " + value("position"), 55)),
                c.x() + 14, legendY + 11, 0xFF624A34);
    }

    private void drawSkills(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c = l.content();
        badge(g, c.x(), c.y(), "남은 기술 점수  " + value("skill_points"), 0xFF315A68);
        g.text(font, Component.literal(shortText(value("trait_title") + " · " + value("trait_description"), 70)),
                c.x() + 135, c.y() + 6, 0xFF5C4635);

        for (int i = 0; i < BRANCHES.size(); i++) {
            PageTab branch = BRANCHES.get(i);
            Rect r = l.branchTab(i);
            customButton(g, r, branch.label(), skillBranch.equals(branch.id()), true);
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
            int border = owned ? 0xFF4C7653 : prerequisite && affordable ? 0xFF8A6437 : 0xFF766B5E;
            int fill = owned ? 0xFFDCE9CF : prerequisite ? 0xFFF0E0BD : 0xFFD7CCB8;
            g.fill(r.x(), r.y(), r.right(), r.bottom(), border);
            g.fill(r.x() + 3, r.y() + 3, r.right() - 3, r.bottom() - 3, fill);
            if (i > 0) {
                Rect previous = l.skillNode(i - 1);
                int cy = r.y() + r.h() / 2;
                g.fill(previous.right(), cy - 2, r.x(), cy + 2, owned ? 0xFF5B855C : 0xFF9B7B4D);
            }
            int cost = SkillTreeCatalog.effectiveCost(node, species);
            g.text(font, Component.literal((owned ? "◆ " : "◇ ") + node.title()), r.x() + 8, r.y() + 7,
                    owned ? 0xFF31583B : 0xFF463326);
            badge(g, r.right() - 44, r.y() + 5, owned ? "해금" : cost + "점",
                    owned ? 0xFF4B7252 : 0xFF6E4B2E);
            List<String> lines = wrap(node.description(), Math.max(14, (r.w() - 16) / 7));
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                g.text(font, Component.literal(lines.get(line)), r.x() + 8, r.y() + 22 + line * 11, 0xFF66503D);
            }
            if (inside(mouseX, mouseY, r) && !owned) {
                g.fill(r.x() + 3, r.bottom() - 4, r.right() - 3, r.bottom() - 2,
                        prerequisite && affordable ? 0xFFC99545 : 0xFF8B8175);
            }
        }
        g.text(font, Component.literal("기술 점수는 처음 지급되며, 경험치 레벨 5마다 1점씩 추가됩니다."),
                c.x(), c.bottom() - 10, 0xFF6A5039);
    }

    private void terrainMass(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry) {
        for (int dy = -ry; dy <= ry; dy += 3) {
            double n = 1.0 - (double) (dy * dy) / Math.max(1.0, ry * (double) ry);
            int half = Math.max(8, (int) Math.round(rx * Math.sqrt(Math.max(0.0, n))));
            int wobble = (int) Math.round(Math.sin((cy + dy) * 0.17) * 7.0 + Math.cos(dy * 0.11) * 4.0);
            int color = dy < -ry / 3 ? 0xFF9DB06D : dy > ry / 3 ? 0xFFB6B26D : 0xFFA8B778;
            g.fill(cx - half + wobble, cy + dy, cx + half + wobble, cy + dy + 3, color);
        }
    }

    private void river(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t + Math.sin(i * 0.18) * 8.0);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            g.fill(x - 2, y - 1, x + 3, y + 2, 0xFF6E9CB1);
        }
    }

    private void mountains(GuiGraphicsExtractor g, int cx, int cy, int count) {
        for (int i = 0; i < count; i++) {
            int x = cx + (i % 3) * 14 - 14;
            int y = cy + (i / 3) * 16 - 12;
            g.fill(x - 5, y + 5, x + 6, y + 8, 0xFF776F64);
            g.fill(x - 3, y + 2, x + 4, y + 5, 0xFF8A8175);
            g.fill(x - 1, y, x + 2, y + 2, 0xFFE1D9C8);
        }
    }

    private void forests(GuiGraphicsExtractor g, int cx, int cy, int count) {
        for (int i = 0; i < count; i++) {
            int x = cx + (i % 4) * 11 - 16;
            int y = cy + (i / 4) * 15 - 10;
            g.fill(x - 3, y - 4, x + 4, y + 2, 0xFF42734B);
            g.fill(x - 1, y + 2, x + 2, y + 6, 0xFF6D5237);
        }
    }

    private void settlement(GuiGraphicsExtractor g, int x, int y, String title, int color) {
        marker(g, x, y, color, 5);
        g.centeredText(font, Component.literal(title), x, y - 15, 0xFF35291F);
    }

    private void district(GuiGraphicsExtractor g, int x, int y, String title) {
        g.fill(x - 3, y - 3, x + 4, y + 4, 0xFF775333);
        g.text(font, Component.literal(title), x + 6, y - 4, 0xFF4B382A);
    }

    private void roadLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            g.fill(x - 1, y - 1, x + 2, y + 2, color);
        }
    }

    private void marker(GuiGraphicsExtractor g, int x, int y, int color, int radius) {
        g.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, 0xFFF6E9C8);
        g.fill(x - radius + 2, y - radius + 2, x + radius - 1, y + radius - 1, color);
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int outer, int border) {
        g.fill(x, y, x + w, y + h, outer);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, border);
    }

    private void card(GuiGraphicsExtractor g, Rect r, String title) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xFF6E4A2D);
        g.fill(r.x() + 2, r.y() + 2, r.right() - 2, r.bottom() - 2, 0xFFF1E2C4);
        g.fill(r.x() + 7, r.y() + 19, r.right() - 7, r.y() + 21, 0xFFC7A064);
        g.text(font, Component.literal(title), r.x() + 9, r.y() + 7, 0xFF493020);
    }

    private void customButton(GuiGraphicsExtractor g, Rect r, String text, boolean active, boolean tab) {
        int outer = active ? 0xFF315144 : 0xFF5E3E27;
        int border = active ? 0xFFD2BD78 : 0xFFB88B4C;
        int fill = active ? 0xFF617B61 : 0xFF80542F;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), outer);
        g.fill(r.x() + 2, r.y() + 2, r.right() - 2, r.bottom() - 2, border);
        g.fill(r.x() + 4, r.y() + 4, r.right() - 4, r.bottom() - 4, fill);
        g.centeredText(font, Component.literal(text), r.x() + r.w() / 2, r.y() + Math.max(5, (r.h() - 8) / 2), 0xFFFFEBC0);
        if (tab && active) g.fill(r.right() - 4, r.y() + 5, r.right() - 2, r.bottom() - 5, 0xFFFFD978);
    }

    private void badge(GuiGraphicsExtractor g, int x, int y, String text, int color) {
        int w = Math.min(150, font.width(text) + 12);
        g.fill(x, y, x + w, y + 17, 0xFF2A211B);
        g.fill(x + 2, y + 2, x + w - 2, y + 15, color);
        g.text(font, Component.literal(shortText(text, Math.max(8, w / 6))), x + 6, y + 5, 0xFFFFEDC5);
    }

    private void largeValue(GuiGraphicsExtractor g, int x, int y, String text, int color) {
        g.text(font, Component.literal(shortText(text, 28)), x, y, color);
    }

    private void infoRow(GuiGraphicsExtractor g, int x, int y, int width, String label, String value) {
        g.text(font, Component.literal(label), x, y, 0xFF806247);
        int valueX = x + Math.min(70, Math.max(48, width / 3));
        g.text(font, Component.literal(shortText(value, Math.max(8, (width - valueX + x) / 6))), valueX, y, 0xFF332A22);
    }

    private void progressBar(GuiGraphicsExtractor g, int x, int y, int width, String label, String text, float ratio) {
        g.text(font, Component.literal(label), x, y, 0xFF73543B);
        g.text(font, Component.literal(text), x + width - Math.min(width / 2, font.width(text)), y, 0xFF3A2C23);
        int barY = y + 12;
        g.fill(x, barY, x + width, barY + 8, 0xFF3B3028);
        g.fill(x + 2, barY + 2, x + 2 + Math.round((width - 4) * clamp(ratio)), barY + 6, 0xFF5B8A63);
    }

    private Layout layout() {
        int panelW = Math.min(760, Math.max(360, width - 12));
        int panelH = Math.min(430, Math.max(230, height - 10));
        panelW = Math.min(panelW, width - 6);
        panelH = Math.min(panelH, height - 6);
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        boolean compact = panelW < 520;
        int navW = compact ? 0 : 104;
        int contentX = compact ? left + 10 : left + navW + 14;
        int contentY = compact ? top + 72 : top + 48;
        int contentW = compact ? panelW - 20 : panelW - navW - 24;
        int contentH = panelH - (contentY - top) - 10;
        return new Layout(left, top, panelW, panelH, compact, navW,
                new Rect(contentX, contentY, contentW, contentH));
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

    private float ratio(String value) {
        String[] parts = value.split("/");
        if (parts.length != 2) return 0.0F;
        try {
            return clamp(Float.parseFloat(parts[0].trim()) / Math.max(1.0F, Float.parseFloat(parts[1].trim())));
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    private static int mapWorldX(int worldX, int x, int w) {
        double t = (worldX + 1700.0) / 3400.0;
        return x + 12 + (int) Math.round(clamp((float) t) * (w - 24));
    }

    private static int mapWorldZ(int worldZ, int y, int h) {
        double t = (worldZ + 500.0) / 1000.0;
        return y + 10 + (int) Math.round(clamp((float) t) * (h - 20));
    }

    private static String normalizePage(String page) {
        return switch (page) {
            case "equipment", "map", "skills" -> page;
            default -> "overview";
        };
    }

    private static boolean inside(int x, int y, Rect r) {
        return x >= r.x() && y >= r.y() && x < r.right() && y < r.bottom();
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String shortText(String value, int maxChars) {
        if (value == null || value.isBlank()) return "-";
        if (value.length() <= maxChars) return value;
        return value.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    private static List<String> wrap(String value, int maxChars) {
        List<String> lines = new ArrayList<>();
        String remaining = value == null ? "-" : value.trim();
        while (!remaining.isEmpty()) {
            int end = Math.min(maxChars, remaining.length());
            if (end < remaining.length()) {
                int space = remaining.lastIndexOf(' ', end);
                if (space > maxChars / 2) end = space;
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

    private record PageTab(String id, String label) {
    }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }

    private record Layout(int left, int top, int panelW, int panelH, boolean compact, int navW, Rect content) {
        int right() { return left + panelW; }
        int bottom() { return top + panelH; }

        Rect closeButton() { return new Rect(right() - 34, top + 8, 24, 22); }

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
            int availableH = content.h() - 70;
            if (content.w() >= 500) {
                int gap = 14;
                int w = (content.w() - gap * 2) / 3;
                return new Rect(content.x() + index * (w + gap), y, w, Math.min(80, availableH));
            }
            int gap = 6;
            int h = Math.max(43, (availableH - gap * 2) / 3);
            return new Rect(content.x(), y + index * (h + gap), content.w(), h);
        }
    }
}

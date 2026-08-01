package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.EquipSpellPayload;
import kr.moonseungjun.arcanecircle.network.FuseSpellPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import kr.moonseungjun.arcanecircle.network.SelectSlotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GrimoireScreen extends Screen {
    private static final List<Tab> TABS = List.of(
            new Tab("spells", "주문서"), new Tab("fusion", "융합 연구"), new Tab("circle", "써클·마력"));
    private String page;

    public GrimoireScreen(String page) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("구중 마도서"));
        this.page = normalize(page);
    }

    @Override
    protected void init() {
        super.init();
        Layout l = layout();
        for (int index = 0; index < TABS.size(); index++) {
            Tab tab = TABS.get(index);
            invisible(l.tab(index), () -> request(tab.id()));
        }
        invisible(l.close(), this::onClose);
        for (int slot = 0; slot < 5; slot++) {
            int selectedSlot = slot;
            invisible(l.slot(slot), () -> ClientPacketDistributor.sendToServer(new SelectSlotPayload(selectedSlot)));
        }
        if ("spells".equals(page)) {
            List<SpellDefinition> spells = new ArrayList<>(SpellCatalog.spells().values());
            for (int index = 0; index < spells.size(); index++) {
                SpellDefinition spell = spells.get(index);
                int spellIndex = index;
                invisible(l.spellCard(spellIndex), () -> ClientPacketDistributor.sendToServer(
                        new EquipSpellPayload(spell.id(), ArcaneClientState.selected())));
            }
        } else if ("fusion".equals(page)) {
            List<SpellCatalog.FusionFormula> formulas = SpellCatalog.fusions();
            for (int index = 0; index < formulas.size(); index++) {
                SpellCatalog.FusionFormula formula = formulas.get(index);
                invisible(l.fusionCard(index), () -> ClientPacketDistributor.sendToServer(new FuseSpellPayload(formula.result())));
            }
        }
    }

    private void invisible(Rect rect, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> action.run())
                .bounds(rect.x(), rect.y(), rect.w(), rect.h()).build());
        button.setAlpha(0.0F);
    }

    private void request(String requestedPage) {
        ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(requestedPage));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0xD0060710);
        g.fill(l.left() - 5, l.top() - 5, l.right() + 5, l.bottom() + 5, 0xFF120B22);
        g.fill(l.left(), l.top(), l.right(), l.bottom(), 0xFF211333);
        g.fill(l.left() + 3, l.top() + 3, l.right() - 3, l.bottom() - 3, 0xFF0E1729);
        g.fill(l.left() + 8, l.top() + 8, l.right() - 8, l.top() + 38, 0xFF35205A);
        g.text(font, Component.literal("NINEFOLD ARCANA"), l.left() + 16, l.top() + 12, 0xFFBFA7FF);
        g.text(font, Component.literal("구중 마도서 · 1~3써클 시험본"), l.left() + 16, l.top() + 25, 0xFFF0E8FF);
        square(g, l.close(), "×", inside(mouseX, mouseY, l.close()));

        for (int i = 0; i < TABS.size(); i++) tab(g, l.tab(i), TABS.get(i).label(), page.equals(TABS.get(i).id()));
        statusBar(g, l);

        switch (page) {
            case "fusion" -> drawFusion(g, l, mouseX, mouseY);
            case "circle" -> drawCircle(g, l);
            default -> drawSpells(g, l, mouseX, mouseY);
        }
        drawSlots(g, l, mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void statusBar(GuiGraphicsExtractor g, Layout l) {
        int circle = ArcaneClientState.integer("circle", 1);
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int insight = ArcaneClientState.integer("insight", 0);
        int next = ArcaneClientState.integer("next", 8);
        Rect bar = l.status();
        g.fill(bar.x(), bar.y(), bar.right(), bar.bottom(), 0xFF171E35);
        g.fill(bar.x() + 2, bar.y() + 2, bar.right() - 2, bar.bottom() - 2, 0xFF26375D);
        int manaWidth = (int) ((bar.w() - 126) * Math.min(1.0, mana / (double) max));
        g.fill(bar.x() + 118, bar.y() + 6, bar.x() + 118 + manaWidth, bar.y() + 15, 0xFF4679DA);
        g.text(font, Component.literal(circle + "써클"), bar.x() + 9, bar.y() + 6, 0xFFDCCAFF);
        g.text(font, Component.literal("마력 " + mana + "/" + max), bar.x() + 118, bar.y() + 18, 0xFFBFD7FF);
        g.text(font, Component.literal(circle >= 3 ? "통찰 완성" : "통찰 " + insight + "/" + next),
                bar.right() - 92, bar.y() + 6, 0xFFE1D4FF);
    }

    private void drawSpells(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        Set<String> known = ArcaneClientState.known();
        List<SpellDefinition> spells = new ArrayList<>(SpellCatalog.spells().values());
        for (int index = 0; index < spells.size(); index++) {
            SpellDefinition spell = spells.get(index);
            Rect card = l.spellCard(index);
            boolean learned = known.contains(spell.id());
            boolean usable = learned && spell.circle() <= ArcaneClientState.integer("circle", 1);
            int fill = inside(mx, my, card) ? 0xFF3D315B : 0xFF242B43;
            g.fill(card.x(), card.y(), card.right(), card.bottom(), 0xFF0A0D18);
            g.fill(card.x() + 2, card.y() + 2, card.right() - 2, card.bottom() - 2, fill);
            g.fill(card.x() + 2, card.y() + 2, card.x() + 7, card.bottom() - 2, schoolColor(spell.school()));
            g.text(font, Component.literal(spell.circle() + "C  " + spell.name()), card.x() + 12, card.y() + 5,
                    usable ? 0xFFFFFFFF : learned ? 0xFFB8A8C8 : 0xFF756D7E);
            g.text(font, Component.literal(spell.school().displayName() + " · 마력 " + spell.manaCost()
                    + " · " + spell.acquisition().displayName()), card.x() + 12, card.y() + 17,
                    learned ? 0xFFB9C9EB : 0xFF6F7380);
            g.text(font, Component.literal(learned ? "클릭: 선택 슬롯 장착" : "미습득"),
                    card.right() - 104, card.y() + 17, usable ? 0xFF9EE6C0 : 0xFF8D8394);
        }
    }

    private void drawFusion(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        Set<String> known = ArcaneClientState.known();
        List<SpellCatalog.FusionFormula> formulas = SpellCatalog.fusions();
        for (int index = 0; index < formulas.size(); index++) {
            SpellCatalog.FusionFormula formula = formulas.get(index);
            SpellDefinition result = SpellCatalog.spell(formula.result()).orElseThrow();
            SpellDefinition first = SpellCatalog.spell(formula.first()).orElseThrow();
            SpellDefinition second = SpellCatalog.spell(formula.second()).orElseThrow();
            Rect card = l.fusionCard(index);
            boolean ready = known.contains(first.id()) && known.contains(second.id()) && !known.contains(result.id());
            boolean complete = known.contains(result.id());
            g.fill(card.x(), card.y(), card.right(), card.bottom(), 0xFF090B15);
            g.fill(card.x() + 2, card.y() + 2, card.right() - 2, card.bottom() - 2,
                    inside(mx, my, card) ? 0xFF43355C : 0xFF24253E);
            g.text(font, Component.literal(first.name() + "  +  " + second.name()), card.x() + 10, card.y() + 6, 0xFFB8B9D7);
            g.centeredText(font, Component.literal("↓"), card.x() + card.w() / 2, card.y() + 18, 0xFF9A7BE8);
            g.text(font, Component.literal(result.circle() + "써클 · " + result.name()), card.x() + 10, card.y() + 31, 0xFFF3E9FF);
            g.text(font, Component.literal(complete ? "연구 완료" : ready ? "클릭하여 융합" : "선행 주문 필요"),
                    card.right() - 96, card.y() + 31, complete ? 0xFF79D7A0 : ready ? 0xFFE0B8FF : 0xFF82798B);
            g.text(font, Component.literal(shorten(result.description(), 44)), card.x() + 10, card.y() + 44, 0xFFAEB6CF);
        }
    }

    private void drawCircle(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        panel(g, new Rect(c.x(), c.y(), c.w(), 56), "구중 마법학의 원리",
                List.of("마력은 세계를 흐르는 에테르를 마력핵이 개인 회로로 변환한 힘이다.",
                        "써클은 주문의 등급이면서 마력핵을 안정화하는 동심 회로의 수다.",
                        "상위 주문은 융합 연구 외에도 전승·주문서·스승에게 직접 얻을 수 있다."));
        int top = c.y() + 64;
        int gap = 7;
        int width = (c.w() - gap * 2) / 3;
        circleCard(g, new Rect(c.x(), top, width, 100), 1, "시동환", "최대 마력 100 · 초당 회복 2",
                "단일 원소, 단일 표적, 이동과 소형 방벽의 기초.");
        circleCard(g, new Rect(c.x() + width + gap, top, width, 100), 2, "교직환", "최대 마력 170 · 초당 회복 4",
                "두 원리를 엮어 관통·구속·치유·공간 전이를 만든다.");
        circleCard(g, new Rect(c.x() + (width + gap) * 2, top, width, 100), 3, "영역환", "최대 마력 260 · 초당 회복 6",
                "폭발·연쇄·광역 제압·중형 방벽으로 전장을 지배한다.");
        Rect mastery = new Rect(c.x(), top + 108, c.w(), 57);
        panel(g, mastery, "저써클 숙련 보정",
                List.of("현재 써클보다 낮은 주문은 써클 차이마다 마력 소모 -10%, 쿨타임 -18%.",
                        "같은 차이마다 위력 +12%, 사거리 +10%. 8/24 통찰에서 2/3써클이 개방된다."));
    }

    private void drawSlots(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        List<String> slots = ArcaneClientState.slots();
        int selected = ArcaneClientState.selected();
        for (int i = 0; i < 5; i++) {
            Rect slot = l.slot(i);
            SpellDefinition spell = SpellCatalog.spell(slots.get(i)).orElse(null);
            boolean active = i == selected;
            g.fill(slot.x(), slot.y(), slot.right(), slot.bottom(), active ? 0xFF9B6EE8 : 0xFF15192A);
            g.fill(slot.x() + 2, slot.y() + 2, slot.right() - 2, slot.bottom() - 2,
                    inside(mx, my, slot) ? 0xFF4A3F69 : active ? 0xFF4B306E : 0xFF283149);
            g.text(font, Component.literal(Integer.toString(i + 1)), slot.x() + 6, slot.y() + 6, 0xFFD6C2FF);
            g.centeredText(font, Component.literal(spell == null ? "빈 슬롯" : shorten(spell.name(), 10)),
                    slot.x() + slot.w() / 2 + 5, slot.y() + 6, 0xFFFFFFFF);
        }
        g.text(font, Component.literal("C 마도서 · R 시전 · Z/X 슬롯 변경"), l.left() + 12, l.bottom() - 13, 0xFF9BA9C9);
    }

    private void panel(GuiGraphicsExtractor g, Rect rect, String title, List<String> lines) {
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF141A2C);
        g.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, 0xFF28324D);
        g.text(font, Component.literal(title), rect.x() + 9, rect.y() + 7, 0xFFE6D7FF);
        int y = rect.y() + 22;
        for (String line : lines) {
            g.text(font, Component.literal(line), rect.x() + 9, y, 0xFFB9C5DE);
            y += 11;
        }
    }

    private void circleCard(GuiGraphicsExtractor g, Rect rect, int circle, String name, String stats, String description) {
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0xFF111625);
        g.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2,
                circle <= ArcaneClientState.integer("circle", 1) ? 0xFF34466E : 0xFF282B3B);
        g.centeredText(font, Component.literal(circle + "써클"), rect.x() + rect.w() / 2, rect.y() + 9, 0xFFE8DBFF);
        g.centeredText(font, Component.literal(name), rect.x() + rect.w() / 2, rect.y() + 23, 0xFFC3A8FF);
        g.centeredText(font, Component.literal(stats), rect.x() + rect.w() / 2, rect.y() + 40, 0xFFB2C8ED);
        List<String> wrapped = wrap(description, Math.max(12, rect.w() / 7));
        int y = rect.y() + 59;
        for (String line : wrapped) {
            g.centeredText(font, Component.literal(line), rect.x() + rect.w() / 2, y, 0xFFAAB2C6);
            y += 11;
        }
    }

    private void tab(GuiGraphicsExtractor g, Rect rect, String text, boolean active) {
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), active ? 0xFF6D4AA0 : 0xFF242D45);
        g.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2,
                active ? 0xFF4C3475 : 0xFF303A56);
        g.centeredText(font, Component.literal(text), rect.x() + rect.w() / 2, rect.y() + 6, 0xFFF1E9FF);
    }

    private void square(GuiGraphicsExtractor g, Rect rect, String text, boolean hover) {
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), hover ? 0xFF8A3F5A : 0xFF4B2843);
        g.centeredText(font, Component.literal(text), rect.x() + rect.w() / 2, rect.y() + 5, 0xFFFFFFFF);
    }

    private Layout layout() {
        int panelW = Math.min(690, Math.max(520, width - 24));
        int panelH = Math.min(430, Math.max(330, height - 20));
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;
        return new Layout(left, top, panelW, panelH);
    }

    private static int schoolColor(SpellDefinition.School school) {
        return switch (school) {
            case FIRE -> 0xFFE46845;
            case FROST -> 0xFF65C7E8;
            case WIND -> 0xFF78D7B1;
            case WARD -> 0xFFB894ED;
            case LIFE -> 0xFF74D58A;
            case SPACE -> 0xFF8A6BE8;
            default -> 0xFF6C8DE0;
        };
    }

    private static boolean inside(int x, int y, Rect rect) {
        return x >= rect.x() && y >= rect.y() && x < rect.right() && y < rect.bottom();
    }

    private static String normalize(String page) {
        return "fusion".equals(page) || "circle".equals(page) ? page : "spells";
    }

    private static String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static List<String> wrap(String value, int max) {
        List<String> lines = new ArrayList<>();
        String remaining = value;
        while (remaining.length() > max) {
            int split = remaining.lastIndexOf(' ', max);
            if (split < 1) split = max;
            lines.add(remaining.substring(0, split));
            remaining = remaining.substring(split).stripLeading();
        }
        if (!remaining.isEmpty()) lines.add(remaining);
        return lines;
    }

    private record Tab(String id, String label) {}
    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }

    private record Layout(int left, int top, int panelW, int panelH) {
        int right() { return left + panelW; }
        int bottom() { return top + panelH; }
        Rect close() { return new Rect(right() - 31, top + 11, 20, 20); }
        Rect tab(int index) { return new Rect(left + 12 + index * 104, top + 45, 98, 24); }
        Rect status() { return new Rect(left + 12, top + 74, panelW - 24, 32); }
        Rect content() { return new Rect(left + 12, top + 112, panelW - 24, panelH - 176); }
        Rect slot(int index) {
            int gap = 6;
            int width = (panelW - 24 - gap * 4) / 5;
            return new Rect(left + 12 + index * (width + gap), bottom() - 53, width, 26);
        }
        Rect spellCard(int index) {
            Rect c = content();
            int columns = 3;
            int gap = 6;
            int width = (c.w() - gap * (columns - 1)) / columns;
            int row = index / columns;
            int column = index % columns;
            return new Rect(c.x() + column * (width + gap), c.y() + row * 35, width, 31);
        }
        Rect fusionCard(int index) {
            Rect c = content();
            int columns = 2;
            int gap = 8;
            int width = (c.w() - gap) / 2;
            int row = index / columns;
            int column = index % columns;
            return new Rect(c.x() + column * (width + gap), c.y() + row * 65, width, 58);
        }
    }
}

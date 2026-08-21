package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.ChooseTraditionPayload;
import kr.moonseungjun.arcanecircle.network.EquipSpellPayload;
import kr.moonseungjun.arcanecircle.network.PurchaseAcademyItemPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import kr.moonseungjun.arcanecircle.world.AcademyOfferCatalog;
import kr.moonseungjun.arcanecircle.world.MagicTradition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Alpha.62 readability-first atlas/academy.
 *
 * Every text block owns an explicit rectangle. Long text is wrapped or fitted before drawing,
 * action controls reserve their own columns, and the five equipped spells are full cards with
 * visible names instead of icon-only circles. The academy shop uses tall rows and the affiliation
 * summary never shares horizontal space with the join action.
 */
public final class ReadableGrimoireScreen extends Screen {
    private static final List<Tab> TABS = List.of(
            new Tab("atlas", "주문", "Ⅰ"), new Tab("recipes", "융합", "Ⅱ"),
            new Tab("staffs", "지팡이", "Ⅲ"), new Tab("academy", "마도회", "Ⅳ"),
            new Tab("quests", "의뢰", "Ⅴ"), new Tab("core", "마력핵", "Ⅵ"));

    private static int atlasCircle = 1;
    private static int academyCircle = 1;
    private static int activeSlot = -1;
    private static String inspectedSpellId = "";
    private static MagicTradition inspectedTradition = MagicTradition.ARCANE;
    private static boolean effects;

    private final String page;
    private int scroll;
    private String notice = "";
    private long noticeUntil;

    public ReadableGrimoireScreen(String page) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("구중 마도서"));
        this.page = "academy".equals(page) ? "academy" : "atlas";
    }

    @Override
    protected void init() {
        super.init();
        atlasCircle = clamp(atlasCircle, 1, 9);
        academyCircle = clamp(academyCircle, 1, 9);
        ensureInspectedSpell();
        scroll = clamp(scroll, 0, maxScroll(layout()));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout l = layout();
        if (inside(event.x(), event.y(), l.close())) {
            onClose();
            return true;
        }
        for (int i = 0; i < TABS.size(); i++) {
            if (!inside(event.x(), event.y(), l.tab(i))) continue;
            request(TABS.get(i).id());
            return true;
        }
        return "academy".equals(page) ? clickAcademy(event, l) : clickAtlas(event, l, doubleClick);
    }

    private boolean clickAtlas(MouseButtonEvent event, Layout l, boolean doubleClick) {
        if (inside(event.x(), event.y(), l.mode(0))) {
            effects = false;
            scroll = 0;
            return true;
        }
        if (inside(event.x(), event.y(), l.mode(1))) {
            effects = true;
            activeSlot = -1;
            scroll = 0;
            return true;
        }
        for (int circle = 1; circle <= 9; circle++) {
            if (!inside(event.x(), event.y(), l.circle(circle))) continue;
            atlasCircle = circle;
            scroll = 0;
            activeSlot = -1;
            ensureInspectedSpell();
            return true;
        }
        if (!effects) {
            for (int slot = 0; slot < 5; slot++) {
                if (!inside(event.x(), event.y(), l.slot(slot))) continue;
                activeSlot = activeSlot == slot ? -1 : slot;
                String current = ArcaneClientState.slot(slot);
                if (activeSlot >= 0 && !current.isBlank()) inspectedSpellId = current;
                notice(activeSlot < 0 ? "슬롯 선택 취소" : (slot + 1) + "번 슬롯 선택 · 주문을 클릭해 교체");
                return true;
            }
        }

        List<SpellDefinition> spells = SpellCatalog.spellsInCircle(atlasCircle);
        for (int i = 0; i < spells.size(); i++) {
            Rect row = effects ? l.effectRow(i, scroll) : l.spellRow(i, scroll);
            if (!inside(event.x(), event.y(), row)) continue;
            SpellDefinition spell = spells.get(i);
            inspectedSpellId = spell.id();
            if (!effects && activeSlot >= 0 && usable(spell)) {
                equip(spell, activeSlot);
                activeSlot = -1;
            } else if (!effects && doubleClick && usable(spell)) {
                int empty = firstEmptySlot();
                if (empty >= 0) equip(spell, empty);
                else notice("빈 슬롯 없음 · 교체할 슬롯을 먼저 선택하세요");
            }
            return true;
        }

        if (!effects && inside(event.x(), event.y(), l.equipAction())) {
            SpellDefinition spell = inspectedSpell();
            if (spell == null || !usable(spell)) {
                notice("아직 사용할 수 없는 주문입니다");
                return true;
            }
            int slot = activeSlot >= 0 ? activeSlot : firstEmptySlot();
            if (slot < 0) notice("빈 슬롯 없음 · 교체할 슬롯을 먼저 선택하세요");
            else {
                equip(spell, slot);
                activeSlot = -1;
            }
            return true;
        }
        return false;
    }

    private boolean clickAcademy(MouseButtonEvent event, Layout l) {
        MagicTradition[] all = traditions();
        for (int i = 0; i < all.length; i++) {
            if (!inside(event.x(), event.y(), l.tradition(i))) continue;
            inspectedTradition = all[i];
            return true;
        }

        if (inside(event.x(), event.y(), l.join())) {
            MagicTradition current = MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND"));
            if (current != inspectedTradition) {
                ClientPacketDistributor.sendToServer(new ChooseTraditionPayload(inspectedTradition.name()));
                notice(inspectedTradition.displayName() + " 소속 등록 요청");
            }
            return true;
        }

        for (int circle = 1; circle <= 9; circle++) {
            if (!inside(event.x(), event.y(), l.circle(circle))) continue;
            academyCircle = circle;
            scroll = 0;
            return true;
        }

        List<AcademyOfferCatalog.Offer> offers = AcademyOfferCatalog.forCircle(academyCircle);
        for (int i = 0; i < offers.size(); i++) {
            if (!inside(event.x(), event.y(), l.offerRow(i, scroll))) continue;
            ClientPacketDistributor.sendToServer(new PurchaseAcademyItemPayload(offers.get(i).id()));
            notice(offers.get(i).displayName() + " 구매 요청");
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0) return false;
        Layout l = layout();
        Rect viewport = "academy".equals(page) ? l.shopViewport()
                : (effects ? l.effectViewport() : l.listViewport());
        if (!inside(mouseX, mouseY, viewport)) return false;
        scroll = clamp(scroll + (scrollY < 0.0 ? 36 : -36), 0, maxScroll(l));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0xDC07080A);
        drawBook(g, l);
        drawTabs(g, l, mouseX, mouseY);
        if ("academy".equals(page)) drawAcademy(g, l, mouseX, mouseY);
        else drawAtlas(g, l, mouseX, mouseY);
        drawNotice(g, l);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawBook(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.left() - 5, l.top() + 5, l.right() + 5, l.bottom() + 7, 0xB8000000);
        g.fill(l.left(), l.top(), l.right(), l.bottom(), 0xFF171513);
        g.fill(l.left() + 1, l.top() + 1, l.left() + 56, l.bottom() - 1, 0xFF0D0E11);
        g.fill(l.left() + 56, l.top() + 1, l.left() + 58, l.bottom() - 1, 0xFF6C573C);
        g.fill(l.left() + 59, l.top() + 1, l.right() - 1, l.bottom() - 1, 0xFF241F1A);
        g.fill(l.left() + 64, l.top() + 24, l.right() - 9, l.top() + 25, 0xFF62513B);
        g.fill(l.right() - 3, l.top() + 8, l.right() - 1, l.bottom() - 8, 0xFFB18C58);
        g.fill(l.left() + 62, l.bottom() - 3, l.right() - 4, l.bottom() - 1, 0xFF7F623E);
        Rect close = l.close();
        g.text(font, Component.literal("×"), close.x() + 6, close.y() + 4, 0xFFE6D8C3);
    }

    private void drawTabs(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        g.centeredText(font, Component.literal("九"), l.left() + 28, l.top() + 10, 0xFFFFD27A);
        for (int i = 0; i < TABS.size(); i++) {
            Tab tab = TABS.get(i);
            Rect r = l.tab(i);
            boolean selected = tab.id().equals(page);
            boolean hover = inside(mouseX, mouseY, r);
            if (selected) {
                g.fill(r.x() + 1, r.y() + 2, r.x() + 5, r.bottom() - 2, 0xFFFFCB69);
                ArcaneRenderUtil.diamond(g, r.x() + 12, r.y() + r.h() / 2, 3, 0xFFFFDF8C);
            }
            int roman = selected ? 0xFFFFDE98 : hover ? 0xFFE5D4BB : 0xFF8B8378;
            int label = selected ? 0xFFF5EBDD : hover ? 0xFFD9CBBB : 0xFF90877C;
            g.text(font, Component.literal(tab.roman()), r.x() + 20, r.y() + 4, roman);
            tiny(g, tab.label(), r.x() + 20, r.y() + 16, label, .60F, false);
        }
    }

    private void drawAtlas(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        ensureInspectedSpell();
        Rect b = l.body();
        String sub = atlasCircle + "써클 · " + prestigeLabel(atlasCircle);
        title(g, b, effects ? "마법 효과 도감" : "주문 목록", sub);

        action(g, l.mode(0), "주문 목록", inside(mouseX, mouseY, l.mode(0)), true,
                effects ? 0xFF766B5D : 0xFFE0B76A);
        action(g, l.mode(1), "효과 도감", inside(mouseX, mouseY, l.mode(1)), true,
                effects ? 0xFFE0B76A : 0xFF766B5D);

        drawCircleRail(g, l, atlasCircle, mouseX, mouseY);

        List<SpellDefinition> spells = SpellCatalog.spellsInCircle(atlasCircle);
        Set<String> known = ArcaneClientState.known();
        Rect viewport = effects ? l.effectViewport() : l.listViewport();
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        for (int i = 0; i < spells.size(); i++) {
            SpellDefinition spell = spells.get(i);
            if (effects) drawEffectRow(g, l.effectRow(i, scroll), spell, known.contains(spell.id()), mouseX, mouseY);
            else drawSpellRow(g, l.spellRow(i, scroll), spell, known.contains(spell.id()), mouseX, mouseY);
        }
        g.disableScissor();

        drawDetail(g, l, inspectedSpell(), mouseX, mouseY);
        if (!effects) {
            for (int slot = 0; slot < 5; slot++) drawSlot(g, l.slot(slot), slot, mouseX, mouseY);
        }
    }

    private void drawCircleRail(GuiGraphicsExtractor g, Layout l, int selected, int mouseX, int mouseY) {
        int unlockedCircle = ArcaneClientState.integer("circle", 1);
        for (int circle = 1; circle <= 9; circle++) {
            Rect r = l.circle(circle);
            boolean active = circle == selected;
            boolean unlocked = circle <= unlockedCircle;
            boolean hover = inside(mouseX, mouseY, r);
            int accent = unlocked ? circleColor(circle) : 0xFF57524D;
            if (active) {
                g.fill(r.x(), r.y() + 2, r.x() + 3, r.bottom() - 2, accent);
                g.fill(r.x() + 4, r.y() + 2, r.right(), r.bottom() - 2, 0x2CFFFFFF);
            }
            int text = active ? 0xFFFFE2A8 : hover ? 0xFFE5D3B9 : unlocked ? 0xFFB8AE9F : 0xFF6D6862;
            g.centeredText(font, Component.literal(Integer.toString(circle)), r.x() + r.w() / 2,
                    r.y() + Math.max(2, r.h() / 2 - 4), text);
            if (circle >= 7 && (active || hover)) {
                ArcaneRenderUtil.ring(g, r.x() + r.w() / 2, r.y() + r.h() / 2,
                        Math.max(6, r.h() / 2 - 2), accent);
            }
        }
    }

    private void drawSpellRow(GuiGraphicsExtractor g, Rect r, SpellDefinition spell, boolean known,
                              int mouseX, int mouseY) {
        if (r.bottom() < 0 || r.y() > height) return;
        boolean selected = spell.id().equals(inspectedSpellId);
        boolean hover = inside(mouseX, mouseY, r);
        boolean usable = known && spell.circle() <= ArcaneClientState.integer("circle", 1);
        int accent = ArcaneRenderUtil.schoolColor(spell.school());

        if (selected) g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x384F4638);
        else if (hover) g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x243D3731);
        if (selected) g.fill(r.x(), r.y() + 3, r.x() + 3, r.bottom() - 3, accent);

        int cx = r.x() + 20, cy = r.y() + r.h() / 2;
        ArcaneRenderUtil.ring(g, cx, cy, 11, usable ? accent : 0xFF605B55);
        if (usable) ArcaneRenderUtil.spellRune(g, cx, cy, spell, 6, 0xFFFFF2DE);
        else ArcaneRenderUtil.diamond(g, cx, cy, 4, 0xFF706961);

        int rightReserve = 82;
        g.text(font, Component.literal(fit(spell.name(), Math.max(40, r.w() - 50 - rightReserve))),
                r.x() + 40, r.y() + 7, usable ? 0xFFF5E9D7 : 0xFF8E857A);
        tiny(g, spell.school().displayName() + " · " + spell.sigilAnchor().displayName(),
                r.x() + 40, r.y() + 24, usable ? 0xFFB9AD9D : 0xFF716A63, .56F, false);

        int equipped = equippedSlot(spell.id());
        if (equipped >= 0) {
            tiny(g, "장착 " + (equipped + 1), r.right() - 7, r.y() + 8, 0xFFFFD27E, .58F, true);
        } else if (!usable) {
            tiny(g, known ? "써클 부족" : "미습득", r.right() - 7, r.y() + 8, 0xFFCA8B88, .54F, true);
        }
        tiny(g, "MP " + spell.manaCost(), r.right() - 7, r.y() + 25, 0xFFC1B5A4, .52F, true);
        rule(g, r.bottom() - 1, r.x() + 38, r.right() - 4, 0xFF4B4237);
    }

    private void drawEffectRow(GuiGraphicsExtractor g, Rect r, SpellDefinition spell, boolean known,
                               int mouseX, int mouseY) {
        if (r.bottom() < 0 || r.y() > height) return;
        boolean selected = spell.id().equals(inspectedSpellId);
        boolean hover = inside(mouseX, mouseY, r);
        int accent = ArcaneRenderUtil.schoolColor(spell.school());
        if (selected) g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x36493F35);
        else if (hover) g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x203A342E);
        if (selected) g.fill(r.x(), r.y() + 4, r.x() + 3, r.bottom() - 4, accent);

        g.text(font, Component.literal(fit(spell.name(), r.w() - 110)), r.x() + 8, r.y() + 6,
                known ? 0xFFF2E5D2 : 0xFF92887D);
        tiny(g, spell.circle() + "C · " + spell.school().displayName(), r.right() - 7, r.y() + 8,
                accent, .56F, true);

        int y = r.y() + 23;
        for (String line : wrap(spell.effectSummary(), r.w() - 16, 4)) {
            tiny(g, line, r.x() + 8, y, known ? 0xFFC9BBA8 : 0xFF7C746B, .58F, false);
            y += 10;
        }
        rule(g, r.bottom() - 1, r.x() + 8, r.right() - 4, 0xFF4A4035);
    }

    private void drawDetail(GuiGraphicsExtractor g, Layout l, SpellDefinition spell, int mouseX, int mouseY) {
        Rect d = l.detail();
        if (spell == null || d.w() < 120 || d.h() < 120) return;
        g.enableScissor(d.x(), d.y(), d.right(), d.bottom());
        g.fill(d.x(), d.y(), d.right(), d.bottom(), 0x662D2721);

        int accent = ArcaneRenderUtil.schoolColor(spell.school());
        int cx = d.x() + d.w() / 2;
        int iconY = d.y() + 33;
        ArcaneRenderUtil.ring(g, cx, iconY, 22, accent);
        ArcaneRenderUtil.ring(g, cx, iconY, 16, 0xFF806A4C);
        ArcaneRenderUtil.spellRune(g, cx, iconY, spell, 10, 0xFFFFF1DC);

        g.centeredText(font, Component.literal(fit(spell.name(), d.w() - 18)), cx, d.y() + 64, 0xFFFFEBD0);
        tiny(g, spell.circle() + "C · " + prestigeLabel(spell.circle()) + " · " + spell.school().displayName(),
                cx, d.y() + 80, spell.circle() >= 7 ? circleColor(spell.circle()) : accent, .60F, true);

        int y = d.y() + 96;
        String primaryText = effects ? spell.effectSummary() : spell.description();
        for (String line : wrap(primaryText, d.w() - 20, effects ? 7 : 4)) {
            tiny(g, line, d.x() + 10, y, 0xFFD0C2AF, .60F, false);
            y += 10;
        }

        y += 4;
        rule(g, y, d.x() + 9, d.right() - 9, 0xFF62513F);
        y += 9;
        tiny(g, "마력  " + spell.manaCost(), d.x() + 10, y, 0xFFE7D2B2, .58F, false);
        tiny(g, "재사용  " + one(spell.cooldownTicks() / 20.0) + "초", d.right() - 10, y,
                0xFFE7D2B2, .58F, true);
        y += 12;
        tiny(g, "사거리  " + one(spell.range()), d.x() + 10, y, 0xFFBFB19D, .56F, false);
        tiny(g, "숙련  " + ArcaneClientState.mastery(spell.id()), d.right() - 10, y, 0xFFBFB19D, .56F, true);

        if (spell.circle() >= 7 && y + 28 < d.bottom()) {
            y += 18;
            g.fill(d.x() + 9, y, d.right() - 9, y + 20, 0x2AFFFFFF);
            String tier = spell.circle() == 9 ? "세계법칙급 · 동일 역할의 하위 써클보다 명확히 상위"
                    : spell.circle() == 8 ? "지역·현실 지배급 · 전장 자체를 재편"
                    : "요새·차원 전술급 · 일반 상급마법과 체급 분리";
            tiny(g, fit(tier, (d.w() - 24) * 2), d.x() + 12, y + 6,
                    circleColor(spell.circle()), .55F, false);
        }

        if (!effects) {
            Rect action = l.equipAction();
            boolean canUse = usable(spell);
            int empty = firstEmptySlot();
            String label = !canUse ? "습득 필요"
                    : activeSlot >= 0 ? (activeSlot + 1) + "번 슬롯에 장착"
                    : empty >= 0 ? "빈 슬롯에 빠른 장착"
                    : "교체할 슬롯을 먼저 선택";
            action(g, action, label, inside(mouseX, mouseY, action),
                    canUse && (activeSlot >= 0 || empty >= 0), accent);
        }
        g.disableScissor();
    }

    private void drawSlot(GuiGraphicsExtractor g, Rect r, int slot, int mouseX, int mouseY) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        boolean selected = activeSlot == slot;
        boolean hover = inside(mouseX, mouseY, r);
        int accent = spell == null ? 0xFF766E64 : ArcaneRenderUtil.schoolColor(spell.school());

        g.fill(r.x(), r.y(), r.right(), r.bottom(), selected ? 0x4CFFE0A0 : hover ? 0x323C352E : 0x292A2622);
        g.fill(r.x(), r.y(), r.right(), r.y() + 1, selected ? 0xFFFFD274 : accent);
        if (selected) {
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), 0xFFFFD274);
            g.fill(r.right() - 2, r.y(), r.right(), r.bottom(), 0xFFFFD274);
        }

        int iconX = r.x() + 22, iconY = r.y() + r.h() / 2;
        ArcaneRenderUtil.ring(g, iconX, iconY, 11, selected ? 0xFFFFD274 : accent);
        if (spell != null) ArcaneRenderUtil.spellRune(g, iconX, iconY, spell, 6,
                selected ? 0xFFFFF0C0 : 0xFFF4E8D8);
        else ArcaneRenderUtil.diamond(g, iconX, iconY, 4, 0xFF776F66);

        tiny(g, Integer.toString(slot + 1), r.x() + 4, r.y() + 3,
                selected ? 0xFFFFE0A0 : 0xFFB3A99B, .54F, false);

        int textX = r.x() + 40;
        int textW = Math.max(18, r.right() - textX - 5);
        if (spell == null) {
            tiny(g, "빈 슬롯", textX, r.y() + 12, 0xFF948A7E, .60F, false);
            tiny(g, "장착 대상 지정", textX, r.y() + 29, 0xFF716A62, .50F, false);
        } else {
            g.text(font, Component.literal(fit(spell.name(), textW)), textX, r.y() + 9,
                    selected ? 0xFFFFE8B2 : 0xFFF0E4D3);
            tiny(g, spell.circle() + "C · " + spell.school().displayName(), textX, r.y() + 27,
                    spell.circle() >= 7 ? circleColor(spell.circle()) : 0xFFB2A696, .52F, false);
            int remaining = ArcaneClientState.cooldownRemainingTicks(slot);
            if (remaining > 0) {
                double fraction = ArcaneClientState.cooldownFraction(slot);
                g.fill(r.x() + 3, r.bottom() - 4, r.right() - 3, r.bottom() - 2, 0xFF402E30);
                g.fill(r.x() + 3, r.bottom() - 4,
                        r.x() + 3 + (int) ((r.w() - 6) * fraction), r.bottom() - 2, 0xFFD15A61);
                tiny(g, one(remaining / 20.0) + "초", r.right() - 5, r.y() + 29, 0xFFE18488, .48F, true);
            }
        }
    }

    private void drawAcademy(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect b = l.body();
        title(g, b, "마도회 기록부", "아르카나 " + ArcaneClientState.longInteger("marks", 0));
        drawCircleRail(g, l, academyCircle, mouseX, mouseY);

        MagicTradition current = MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND"));
        MagicTradition[] all = traditions();
        for (int i = 0; i < all.length; i++) {
            Rect r = l.tradition(i);
            boolean selected = all[i] == inspectedTradition;
            boolean joined = all[i] == current;
            boolean hover = inside(mouseX, mouseY, r);
            if (selected) {
                g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x303F382F);
                g.fill(r.x() + 3, r.bottom() - 2, r.right() - 3, r.bottom(), 0xFFE1B86B);
            } else if (hover) {
                g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x20372F29);
            }
            if (joined) ArcaneRenderUtil.diamond(g, r.x() + 9, r.y() + r.h() / 2, 3, 0xFFFFD984);
            g.centeredText(font, Component.literal(fit(all[i].displayName(), r.w() - 22)),
                    r.x() + r.w() / 2 + 4, r.y() + 6,
                    joined ? 0xFFFFD984 : selected ? 0xFFF3E5D0 : hover ? 0xFFD8C9B6 : 0xFFA1988D);
        }

        Rect info = l.academyInfo();
        g.fill(info.x(), info.y(), info.right(), info.bottom(), 0x422D2721);
        Rect textArea = l.academyText();
        int x = textArea.x() + 6, y = textArea.y() + 5;
        g.text(font, Component.literal(inspectedTradition.displayName()), x, y, 0xFFFFE9C7);
        y += 15;
        for (String line : wrap(inspectedTradition.description(), textArea.w() - 12, 3)) {
            tiny(g, line, x, y, 0xFFD7C8B3, .60F, false);
            y += 10;
        }
        y += 3;
        tiny(g, "강점 · " + fit(inspectedTradition.strength(), Math.max(20, textArea.w() * 2 - 48)),
                x, y, 0xFF8DE0B5, .58F, false);
        y += 11;
        tiny(g, "약점 · " + fit(inspectedTradition.weakness(), Math.max(20, textArea.w() * 2 - 48)),
                x, y, 0xFFE29A98, .58F, false);
        y += 11;
        String prefix = "faction_" + inspectedTradition.name().toLowerCase(Locale.ROOT);
        tiny(g, "본거지 · " + fit(ArcaneClientState.text(prefix + "_headquarters", "기록 없음"),
                        Math.max(20, textArea.w() * 2 - 56)),
                x, y, 0xFFC5B7A4, .56F, false);

        Rect join = l.join();
        g.fill(join.x(), join.y(), join.right(), join.bottom(), 0x30231F1B);
        tiny(g, current == inspectedTradition ? "현재 소속" : "선택 마도회",
                join.x() + join.w() / 2, join.y() + 8,
                current == inspectedTradition ? 0xFFFFD884 : 0xFFB8AA97, .56F, true);
        tiny(g, fit(inspectedTradition.displayName(), Math.max(24, join.w() * 2 - 12)),
                join.x() + join.w() / 2, join.y() + 25, 0xFFF1E3CF, .60F, true);
        action(g, l.joinAction(), current == inspectedTradition ? "가입 완료" : "소속 등록",
                inside(mouseX, mouseY, l.joinAction()), current != inspectedTradition, 0xFFE0B76A);

        Rect shopHead = l.shopHeader();
        g.text(font, Component.literal("주문서 상점"), shopHead.x() + 2, shopHead.y() + 3, 0xFFFFE4C0);
        tiny(g, academyCircle + "써클 · " + AcademyOfferCatalog.forCircle(academyCircle).size() + "종",
                shopHead.right() - 2, shopHead.y() + 5, 0xFFC3B6A4, .56F, true);
        rule(g, shopHead.bottom() - 1, shopHead.x(), shopHead.right(), 0xFF6B5740);

        Rect shop = l.shopViewport();
        g.fill(shop.x(), shop.y(), shop.right(), shop.bottom(), 0x24231E1A);
        List<AcademyOfferCatalog.Offer> offers = AcademyOfferCatalog.forCircle(academyCircle);
        long marks = ArcaneClientState.longInteger("marks", 0);
        g.enableScissor(shop.x(), shop.y(), shop.right(), shop.bottom());
        if (offers.isEmpty()) {
            g.text(font, Component.literal("이 써클의 주문서가 아직 등록되지 않았습니다."),
                    shop.x() + 10, shop.y() + 12, 0xFFB5AA9B);
        }
        for (int i = 0; i < offers.size(); i++) {
            Rect r = l.offerRow(i, scroll);
            AcademyOfferCatalog.Offer offer = offers.get(i);
            boolean enough = marks >= offer.basePrice();
            boolean hover = inside(mouseX, mouseY, r);
            if (hover) g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x303A332C);
            ArcaneRenderUtil.diamond(g, r.x() + 16, r.y() + r.h() / 2, 5,
                    enough ? circleColor(academyCircle) : 0xFF805F5F);
            g.text(font, Component.literal(fit(offer.displayName(), Math.max(50, r.w() - 136))),
                    r.x() + 32, r.y() + 7, enough ? 0xFFF4E7D4 : 0xFFAC9188);
            List<String> desc = wrap(offer.description(), Math.max(40, r.w() - 142), 2);
            int dy = r.y() + 24;
            for (String line : desc) {
                tiny(g, line, r.x() + 32, dy, enough ? 0xFFBFB2A2 : 0xFF83776F, .54F, false);
                dy += 9;
            }
            tiny(g, offer.basePrice() + " A", r.right() - 8, r.y() + 14,
                    enough ? 0xFFFFD17A : 0xFFE18183, .62F, true);
            tiny(g, enough ? "클릭 구매" : "아르카나 부족", r.right() - 8, r.y() + 31,
                    enough ? 0xFF8DD8AE : 0xFFC77E80, .50F, true);
            rule(g, r.bottom() - 1, r.x() + 28, r.right() - 4, 0xFF4E4337);
        }
        g.disableScissor();
    }

    private void title(GuiGraphicsExtractor g, Rect body, String title, String sub) {
        g.text(font, Component.literal(title), body.x() + 2, body.y() + 2, 0xFFFFECD4);
        g.text(font, Component.literal(fit(sub, Math.max(60, body.w() - 180))), body.x() + 2, body.y() + 17, 0xFFB6AA9A);
    }

    private void action(GuiGraphicsExtractor g, Rect r, String label, boolean hover, boolean enabled, int accent) {
        if (r.w() <= 0 || r.h() <= 0) return;
        int line = enabled ? (hover ? 0xFFFFE0A0 : accent) : 0xFF6C645B;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), enabled ? (hover ? 0x324C4034 : 0x242D2822) : 0x18262220);
        g.fill(r.x(), r.bottom() - 2, r.right(), r.bottom(), line);
        if (hover && enabled) g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), line);
        g.centeredText(font, Component.literal(fit(label, r.w() - 10)), r.x() + r.w() / 2,
                r.y() + Math.max(4, r.h() / 2 - 4),
                enabled ? (hover ? 0xFFFFF0D4 : 0xFFEEDFC9) : 0xFF8B8379);
    }

    private void drawNotice(GuiGraphicsExtractor g, Layout l) {
        String server = ArcaneClientState.noticeText();
        String shown = !server.isBlank() ? server
                : (!notice.isBlank() && System.currentTimeMillis() <= noticeUntil ? notice : "");
        if (shown.isBlank()) return;
        int w = Math.min(l.body().w() - 20, Math.max(140, font.width(shown) + 20));
        int x = l.body().x() + l.body().w() / 2 - w / 2;
        int y = l.top() + 7;
        g.fill(x, y, x + w, y + 18, 0xF20A0A0C);
        g.fill(x, y, x + 3, y + 18, 0xFFFFC96A);
        g.centeredText(font, Component.literal(fit(shown, w - 12)), x + w / 2, y + 5, 0xFFFFEAD0);
    }

    private void tiny(GuiGraphicsExtractor g, String text, int x, int y, int color, float scale, boolean centered) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        if (centered) g.centeredText(font, Component.literal(text), 0, 0, color);
        else g.text(font, Component.literal(text), 0, 0, color);
        g.pose().popMatrix();
    }

    private void rule(GuiGraphicsExtractor g, int y, int x0, int x1, int color) {
        g.fill(x0, y, x1, y + 1, color);
    }

    private void request(String target) {
        ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(target));
    }

    private void notice(String text) {
        notice = text;
        noticeUntil = System.currentTimeMillis() + 2200L;
    }

    private boolean usable(SpellDefinition spell) {
        return spell != null
                && ArcaneClientState.known().contains(spell.id())
                && spell.circle() <= ArcaneClientState.integer("circle", 1);
    }

    private void equip(SpellDefinition spell, int slot) {
        ClientPacketDistributor.sendToServer(new EquipSpellPayload(spell.id(), slot));
        notice((slot + 1) + "번 슬롯 · " + spell.name());
    }

    private int firstEmptySlot() {
        for (int i = 0; i < 5; i++) if (ArcaneClientState.slot(i).isBlank()) return i;
        return -1;
    }

    private int equippedSlot(String spellId) {
        for (int i = 0; i < 5; i++) if (spellId.equals(ArcaneClientState.slot(i))) return i;
        return -1;
    }

    private void ensureInspectedSpell() {
        List<SpellDefinition> list = SpellCatalog.spellsInCircle(atlasCircle);
        if (list.isEmpty()) {
            inspectedSpellId = "";
            return;
        }
        if (list.stream().noneMatch(spell -> spell.id().equals(inspectedSpellId))) {
            inspectedSpellId = list.getFirst().id();
        }
    }

    private SpellDefinition inspectedSpell() {
        return SpellCatalog.spell(inspectedSpellId).orElse(null);
    }

    private int maxScroll(Layout l) {
        if ("academy".equals(page)) {
            return Math.max(0, AcademyOfferCatalog.forCircle(academyCircle).size() * Layout.OFFER_ROW - l.shopViewport().h());
        }
        int count = SpellCatalog.spellsInCircle(atlasCircle).size();
        int row = effects ? Layout.EFFECT_ROW : Layout.SPELL_ROW;
        Rect viewport = effects ? l.effectViewport() : l.listViewport();
        return Math.max(0, count * row - viewport.h());
    }

    private Layout layout() {
        int w = Math.min(960, Math.max(620, width - 24));
        int h = Math.min(570, Math.max(360, height - 18));
        w = Math.min(w, Math.max(1, width - 8));
        h = Math.min(h, Math.max(1, height - 8));
        return new Layout((width - w) / 2, (height - h) / 2, w, h);
    }

    private List<String> wrap(String value, int pixels, int maxLines) {
        List<String> out = new ArrayList<>();
        if (value == null || value.isBlank() || pixels <= 0 || maxLines <= 0) return out;
        String remain = value.trim();
        for (int line = 0; line < maxLines && !remain.isEmpty(); line++) {
            if (font.width(remain) <= pixels) {
                out.add(remain);
                break;
            }
            int cut = remain.length();
            while (cut > 1 && font.width(remain.substring(0, cut)) > pixels) cut--;
            int space = remain.lastIndexOf(' ', cut);
            if (space > Math.max(1, cut / 2)) cut = space;
            String part = remain.substring(0, Math.max(1, cut)).trim();
            remain = remain.substring(Math.max(1, cut)).trim();
            if (line == maxLines - 1 && !remain.isEmpty()) part = fit(part + "…", pixels);
            out.add(part);
        }
        return out;
    }

    private String fit(String value, int pixels) {
        if (value == null || pixels <= 0) return "";
        if (font.width(value) <= pixels) return value;
        String suffix = "…";
        int allowed = Math.max(0, pixels - font.width(suffix));
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end)) > allowed) end--;
        return end <= 0 ? suffix : value.substring(0, end) + suffix;
    }

    private static MagicTradition[] traditions() {
        return new MagicTradition[]{
                MagicTradition.ARCANE, MagicTradition.DIVINE, MagicTradition.OCCULT, MagicTradition.PRIMAL
        };
    }

    private static String prestigeLabel(int circle) {
        return switch (circle) {
            case 1 -> "기초 회로";
            case 2 -> "전투 입문";
            case 3 -> "정규 마도";
            case 4 -> "상급 전술";
            case 5 -> "전장 지배";
            case 6 -> "대마법사";
            case 7 -> "요새·차원 권능";
            case 8 -> "지역·현실 지배";
            default -> "세계법칙·재앙";
        };
    }

    private static int circleColor(int circle) {
        return switch (circle) {
            case 1 -> 0xFF86B2E9;
            case 2 -> 0xFF79C7D8;
            case 3 -> 0xFF83C8A3;
            case 4 -> 0xFFE1BD70;
            case 5 -> 0xFFE49471;
            case 6 -> 0xFFC39ADD;
            case 7 -> 0xFFB28BEA;
            case 8 -> 0xFF9B85E8;
            default -> 0xFFFFD66F;
        };
    }

    private static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean inside(double x, double y, Rect r) {
        return x >= r.x() && y >= r.y() && x < r.right() && y < r.bottom();
    }

    private record Tab(String id, String label, String roman) {}
    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }

    private record Layout(int left, int top, int panelW, int panelH) {
        static final int SPELL_ROW = 46;
        static final int EFFECT_ROW = 74;
        static final int OFFER_ROW = 52;

        int right() { return left + panelW; }
        int bottom() { return top + panelH; }
        Rect close() { return new Rect(right() - 25, top + 5, 18, 18); }

        Rect tab(int i) {
            int step = Math.max(34, Math.min(50, (panelH - 36) / TABS.size()));
            return new Rect(left + 4, top + 28 + i * step, 50, Math.min(38, step - 3));
        }

        Rect body() {
            return new Rect(left + 68, top + 29, Math.max(300, panelW - 82), Math.max(260, panelH - 42));
        }

        Rect mode(int i) {
            Rect b = body();
            int w = 84;
            return new Rect(b.right() - (2 - i) * (w + 8), b.y() + 1, w, 22);
        }

        int contentTop() { return body().y() + 38; }

        Rect circle(int c) {
            Rect b = body();
            int available = Math.max(216, b.h() - 52);
            int step = Math.max(23, available / 9);
            return new Rect(b.x(), contentTop() + (c - 1) * step, 32, Math.min(24, step));
        }

        Rect content() {
            Rect b = body();
            return new Rect(b.x() + 42, contentTop(), Math.max(220, b.w() - 44), Math.max(190, b.bottom() - contentTop() - 4));
        }

        int slotHeight() { return 62; }

        Rect listViewport() {
            Rect c = content();
            int detailW = Math.max(230, Math.min(300, c.w() / 3));
            int h = Math.max(80, c.h() - slotHeight() - 8);
            return new Rect(c.x(), c.y(), Math.max(160, c.w() - detailW - 8), h);
        }

        Rect effectViewport() {
            Rect c = content();
            int detailW = Math.max(230, Math.min(300, c.w() / 3));
            return new Rect(c.x(), c.y(), Math.max(160, c.w() - detailW - 8), c.h());
        }

        Rect detail() {
            Rect c = content();
            int w = Math.max(230, Math.min(300, c.w() / 3));
            int x = c.right() - w;
            return new Rect(x, c.y(), w, c.h());
        }

        Rect spellRow(int i, int scroll) {
            Rect v = listViewport();
            return new Rect(v.x(), v.y() + i * SPELL_ROW - scroll, v.w(), SPELL_ROW - 2);
        }

        Rect effectRow(int i, int scroll) {
            Rect v = effectViewport();
            return new Rect(v.x(), v.y() + i * EFFECT_ROW - scroll, v.w(), EFFECT_ROW - 2);
        }

        Rect slot(int i) {
            Rect c = content();
            Rect list = listViewport();
            int gap = 5;
            int totalW = list.w();
            int w = Math.max(54, (totalW - gap * 4) / 5);
            return new Rect(list.x() + i * (w + gap), c.bottom() - slotHeight(), w, slotHeight());
        }

        Rect equipAction() {
            Rect d = detail();
            return new Rect(d.x() + 10, d.bottom() - 28, d.w() - 20, 23);
        }

        Rect tradition(int i) {
            Rect c = content();
            int gap = 6;
            int w = Math.max(72, (c.w() - gap * 3) / 4);
            return new Rect(c.x() + i * (w + gap), c.y(), w, 24);
        }

        Rect academyInfo() {
            Rect c = content();
            return new Rect(c.x(), c.y() + 31, c.w(), Math.max(118, Math.min(138, c.h() / 3)));
        }

        Rect academyText() {
            Rect info = academyInfo();
            int joinW = Math.max(110, Math.min(145, info.w() / 4));
            return new Rect(info.x(), info.y(), Math.max(100, info.w() - joinW - 10), info.h());
        }

        Rect join() {
            Rect info = academyInfo(), text = academyText();
            return new Rect(text.right() + 10, info.y(), info.right() - text.right() - 10, info.h());
        }

        Rect joinAction() {
            Rect j = join();
            return new Rect(j.x() + 8, j.bottom() - 32, Math.max(40, j.w() - 16), 24);
        }

        Rect shopHeader() {
            Rect c = content(), info = academyInfo();
            int y = info.bottom() + 10;
            return new Rect(c.x(), y, c.w(), 24);
        }

        Rect shopViewport() {
            Rect c = content(), head = shopHeader();
            int y = head.bottom() + 4;
            return new Rect(c.x(), y, c.w(), Math.max(52, c.bottom() - y));
        }

        Rect offerRow(int i, int scroll) {
            Rect v = shopViewport();
            return new Rect(v.x() + 2, v.y() + i * OFFER_ROW - scroll, Math.max(20, v.w() - 4), OFFER_ROW - 2);
        }
    }
}

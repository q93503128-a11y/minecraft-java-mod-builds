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
 * Readability-first front pages for the grimoire. The ordinary spell list keeps only identity,
 * loadout and core numbers; long mechanical rules live in a dedicated effect compendium. Academy
 * affiliation identity is given a full readable block instead of being compressed below the shop.
 */
public final class PrimaryGrimoireScreen extends Screen {
    private static final List<Tab> TABS = List.of(
            new Tab("atlas", "주문", "Ⅰ"), new Tab("recipes", "융합", "Ⅱ"), new Tab("staffs", "지팡이", "Ⅲ"),
            new Tab("academy", "마도회", "Ⅳ"), new Tab("quests", "의뢰", "Ⅴ"), new Tab("core", "마력핵", "Ⅵ"));
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

    public PrimaryGrimoireScreen(String page) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("구중 마도서"));
        this.page = "academy".equals(page) ? "academy" : "atlas";
    }

    @Override protected void init() {
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
        if (inside(event.x(), event.y(), l.close())) { onClose(); return true; }
        for (int i = 0; i < TABS.size(); i++) {
            if (!inside(event.x(), event.y(), l.tab(i))) continue;
            request(TABS.get(i).id());
            return true;
        }
        return "academy".equals(page) ? clickAcademy(event, l) : clickAtlas(event, l, doubleClick);
    }

    private boolean clickAtlas(MouseButtonEvent e, Layout l, boolean doubleClick) {
        if (inside(e.x(), e.y(), l.mode(0))) { effects = false; scroll = 0; return true; }
        if (inside(e.x(), e.y(), l.mode(1))) { effects = true; activeSlot = -1; scroll = 0; return true; }
        for (int circle = 1; circle <= 9; circle++) if (inside(e.x(), e.y(), l.circle(circle))) {
            atlasCircle = circle; scroll = 0; activeSlot = -1; ensureInspectedSpell(); return true;
        }
        if (effects) return false;
        for (int slot = 0; slot < 5; slot++) if (inside(e.x(), e.y(), l.slot(slot))) {
            activeSlot = activeSlot == slot ? -1 : slot;
            String current = ArcaneClientState.slot(slot);
            if (activeSlot >= 0 && !current.isBlank()) inspectedSpellId = current;
            notice(activeSlot < 0 ? "슬롯 선택 취소" : (slot + 1) + "번 슬롯 선택 · 주문을 클릭해 교체");
            return true;
        }
        List<SpellDefinition> spells = SpellCatalog.spellsInCircle(atlasCircle);
        for (int i = 0; i < spells.size(); i++) if (inside(e.x(), e.y(), l.spellRow(i, scroll))) {
            SpellDefinition spell = spells.get(i);
            inspectedSpellId = spell.id();
            if (activeSlot >= 0 && usable(spell)) {
                equip(spell, activeSlot); activeSlot = -1;
            } else if (doubleClick && usable(spell)) {
                int empty = firstEmptySlot();
                if (empty >= 0) equip(spell, empty); else notice("빈 슬롯 없음 · 교체할 슬롯을 먼저 선택하세요");
            }
            return true;
        }
        if (inside(e.x(), e.y(), l.equipAction())) {
            SpellDefinition spell = inspectedSpell();
            if (spell == null || !usable(spell)) { notice("아직 사용할 수 없는 주문입니다"); return true; }
            int slot = activeSlot >= 0 ? activeSlot : firstEmptySlot();
            if (slot < 0) notice("빈 슬롯 없음 · 교체할 슬롯을 먼저 선택하세요");
            else { equip(spell, slot); activeSlot = -1; }
            return true;
        }
        return false;
    }

    private boolean clickAcademy(MouseButtonEvent e, Layout l) {
        MagicTradition[] traditions = traditions();
        for (int i = 0; i < traditions.length; i++) if (inside(e.x(), e.y(), l.tradition(i))) {
            inspectedTradition = traditions[i]; return true;
        }
        if (inside(e.x(), e.y(), l.join())) {
            MagicTradition current = MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND"));
            if (current != inspectedTradition) {
                ClientPacketDistributor.sendToServer(new ChooseTraditionPayload(inspectedTradition.name()));
                notice(inspectedTradition.displayName() + " 소속 등록 요청");
            }
            return true;
        }
        for (int circle = 1; circle <= 9; circle++) if (inside(e.x(), e.y(), l.circle(circle))) {
            academyCircle = circle; scroll = 0; return true;
        }
        List<AcademyOfferCatalog.Offer> offers = AcademyOfferCatalog.forCircle(academyCircle);
        for (int i = 0; i < offers.size(); i++) if (inside(e.x(), e.y(), l.offerRow(i, scroll))) {
            ClientPacketDistributor.sendToServer(new PurchaseAcademyItemPayload(offers.get(i).id()));
            notice(offers.get(i).displayName() + " 구매 요청");
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        Layout l = layout();
        Rect viewport = "academy".equals(page) ? l.shopViewport() : l.listViewport();
        if (!inside(mouseX, mouseY, viewport)) return false;
        scroll = clamp(scroll + (scrollY < 0 ? 28 : -28), 0, maxScroll(l));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0xD708080A);
        drawBook(g, l);
        drawTabs(g, l, mouseX, mouseY);
        if ("academy".equals(page)) drawAcademy(g, l, mouseX, mouseY);
        else drawAtlas(g, l, mouseX, mouseY);
        drawNotice(g, l);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawBook(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.left - 5, l.top + 5, l.right() + 5, l.bottom() + 7, 0xB0000000);
        g.fill(l.left, l.top, l.right(), l.bottom(), 0xF3151413);
        g.fill(l.left + 1, l.top + 1, l.left + 54, l.bottom() - 1, 0xFA0C0D10);
        g.fill(l.left + 54, l.top + 1, l.left + 56, l.bottom() - 1, 0xFF5B4935);
        g.fill(l.left + 57, l.top + 1, l.right() - 1, l.bottom() - 1, 0xF21B1814);
        g.fill(l.left + 62, l.top + 22, l.right() - 8, l.top + 23, 0xFF4B4033);
        g.fill(l.right() - 3, l.top + 8, l.right() - 1, l.bottom() - 8, 0xFF8A704A);
        g.text(font, Component.literal("×"), l.close().x + 6, l.close().y + 4, 0xFFC9BDAA);
    }

    private void drawTabs(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        g.centeredText(font, Component.literal("九"), l.left + 27, l.top + 10, 0xFFD8B875);
        for (int i = 0; i < TABS.size(); i++) {
            Tab tab = TABS.get(i); Rect r = l.tab(i);
            boolean selected = tab.id.equals(page), hover = inside(mouseX, mouseY, r);
            if (selected) { g.fill(r.x + 1, r.y + 2, r.x + 4, r.bottom() - 2, 0xFFE0B96B); ArcaneRenderUtil.diamond(g, r.x + 11, r.y + r.h / 2, 3, 0xFFFFD786); }
            g.text(font, Component.literal(tab.roman), r.x + 18, r.y + 4, selected ? 0xFFFFD990 : hover ? 0xFFD7C7AE : 0xFF766F66);
            tiny(g, tab.label, r.x + 18, r.y + 15, selected ? 0xFFE8DDCB : hover ? 0xFFBDB1A0 : 0xFF706A62, .56F, false);
        }
    }

    private void drawAtlas(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        ensureInspectedSpell(); Rect b = l.body();
        title(g, b, effects ? "마법 효과 도감" : "주문 목록",
                effects ? atlasCircle + "써클 · 실제 판정/지속/대상 규칙" : atlasCircle + "써클 · 핵심 정보와 장착");
        action(g, l.mode(0), "주문 목록", inside(mouseX, mouseY, l.mode(0)), !effects, 0xFFD2AE6B);
        action(g, l.mode(1), "효과 도감", inside(mouseX, mouseY, l.mode(1)), effects, 0xFF7FB3D2);
        drawCircleRail(g, l, atlasCircle, mouseX, mouseY);
        if (effects) drawEffectCompendium(g, l);
        else drawSpellBrowser(g, l, mouseX, mouseY);
    }

    private void drawSpellBrowser(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect list = l.listViewport(); Rect detail = l.detail();
        Set<String> known = ArcaneClientState.known(); List<SpellDefinition> spells = SpellCatalog.spellsInCircle(atlasCircle);
        g.enableScissor(list.x, list.y, list.right(), list.bottom());
        for (int i = 0; i < spells.size(); i++) {
            SpellDefinition s = spells.get(i); Rect r = l.spellRow(i, scroll);
            if (r.bottom() < list.y || r.y > list.bottom()) continue;
            boolean selected = s.id().equals(inspectedSpellId), usable = known.contains(s.id()) && s.circle() <= ArcaneClientState.integer("circle", 1);
            int accent = ArcaneRenderUtil.schoolColor(s.school());
            if (selected) g.fill(r.x, r.y, r.x + 3, r.bottom(), accent);
            ArcaneRenderUtil.ring(g, r.x + 16, r.y + 15, 8, usable ? accent : 0xFF55524E);
            ArcaneRenderUtil.spellRune(g, r.x + 16, r.y + 15, s, 4, usable ? 0xFFF1E6D3 : 0xFF676159);
            g.text(font, Component.literal(fit(s.name(), r.w - 42)), r.x + 31, r.y + 5, usable ? 0xFFE5D9C7 : 0xFF77716A);
            tiny(g, s.school().displayName() + " · " + s.sigilAnchor().displayName(), r.x + 31, r.y + 18, 0xFF8E857A, .52F, false);
            rule(g, r.bottom() - 1, r.x + 29, r.right(), 0xFF3D362E);
        }
        g.disableScissor();

        SpellDefinition s = inspectedSpell(); if (s == null) return;
        g.enableScissor(detail.x, detail.y, detail.right(), detail.bottom());
        int accent = ArcaneRenderUtil.schoolColor(s.school()), cx = detail.x + detail.w / 2;
        ArcaneRenderUtil.ring(g, cx, detail.y + 31, 19, accent);
        ArcaneRenderUtil.spellRune(g, cx, detail.y + 31, s, 9, 0xFFFFEFDA);
        g.centeredText(font, Component.literal(fit(s.name(), detail.w - 12)), cx, detail.y + 56, 0xFFF0E2CD);
        g.centeredText(font, Component.literal(s.circle() + "C · " + s.school().displayName()), cx, detail.y + 69, accent);
        int y = detail.y + 88;
        for (String line : wrap(s.description(), detail.w - 16, 3)) { g.text(font, Component.literal(line), detail.x + 8, y, 0xFFC5B8A5); y += 11; }
        y += 4; rule(g, y, detail.x + 8, detail.right() - 8, 0xFF514536); y += 9;
        g.text(font, Component.literal("MP  " + s.manaCost()), detail.x + 8, y, 0xFFE2D1B5); y += 12;
        g.text(font, Component.literal("쿨  " + one(s.cooldownTicks() / 20.0) + "초"), detail.x + 8, y, 0xFFE2D1B5); y += 12;
        g.text(font, Component.literal("범위  " + one(s.range()) + "m"), detail.x + 8, y, 0xFFE2D1B5); y += 12;
        g.text(font, Component.literal("숙련  " + ArcaneClientState.mastery(s.id()) + " / " + SpellCatalog.masteryRequired(s.id())), detail.x + 8, y, 0xFFE2D1B5); y += 15;
        g.text(font, Component.literal("세부 판정은 효과 도감에서 확인"), detail.x + 8, y, 0xFF88B8D1);
        g.disableScissor();

        for (int slot = 0; slot < 5; slot++) drawSlot(g, l.slot(slot), slot, mouseX, mouseY);
        boolean can = usable(s) && (activeSlot >= 0 || firstEmptySlot() >= 0);
        String label = activeSlot >= 0 ? (activeSlot + 1) + "번 슬롯에 장착" : firstEmptySlot() >= 0 ? "빈 슬롯에 장착" : "교체 슬롯 선택 필요";
        action(g, l.equipAction(), label, inside(mouseX, mouseY, l.equipAction()), can, accent);
    }

    private void drawEffectCompendium(GuiGraphicsExtractor g, Layout l) {
        Rect v = l.listViewport(); List<SpellDefinition> spells = SpellCatalog.spellsInCircle(atlasCircle);
        g.enableScissor(v.x, v.y, v.right(), v.bottom());
        for (int i = 0; i < spells.size(); i++) {
            SpellDefinition s = spells.get(i); Rect r = l.effectRow(i, scroll); if (r.bottom() < v.y || r.y > v.bottom()) continue;
            int accent = ArcaneRenderUtil.schoolColor(s.school());
            g.text(font, Component.literal(s.name()), r.x + 4, r.y + 4, 0xFFF0E1CB);
            tiny(g, s.circle() + "C · " + s.school().displayName() + " · " + s.sigilAnchor().displayName(), r.right() - 4, r.y + 5, accent, .52F, true);
            int y = r.y + 18;
            for (String line : wrap(s.effectSummary(), r.w - 8, 2)) { tiny(g, line, r.x + 4, y, 0xFFC7BAA8, .58F, false); y += 9; }
            tiny(g, "MP " + s.manaCost() + " · 쿨 " + one(s.cooldownTicks() / 20.0) + "초 · 범위 " + one(s.range()) + "m", r.x + 4, r.bottom() - 10, 0xFF8F867A, .52F, false);
            rule(g, r.bottom() - 1, r.x, r.right(), 0xFF493F34);
        }
        g.disableScissor();
    }

    private void drawAcademy(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect b = l.body(); title(g, b, "마도회 기록부", "아르카나 " + ArcaneClientState.longInteger("marks", 0));
        MagicTradition current = MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND"));
        MagicTradition[] all = traditions();
        for (int i = 0; i < all.length; i++) {
            Rect r = l.tradition(i); boolean selected = all[i] == inspectedTradition, joined = all[i] == current;
            if (selected) g.fill(r.x + 3, r.bottom() - 2, r.right() - 3, r.bottom(), 0xFFD2AE6B);
            if (joined) ArcaneRenderUtil.diamond(g, r.x + 7, r.y + r.h / 2, 3, 0xFFFFD984);
            g.centeredText(font, Component.literal(fit(all[i].displayName(), r.w - 12)), r.x + r.w / 2 + 3, r.y + 5,
                    joined ? 0xFFFFD984 : selected ? 0xFFF0E0C7 : 0xFF91877A);
        }

        Rect info = l.academyInfo(); int x = info.x + 5, y = info.y + 4;
        g.text(font, Component.literal(inspectedTradition.displayName()), x, y, 0xFFF5E5CA); y += 14;
        for (String line : wrap(inspectedTradition.description(), info.w - 10, 2)) { g.text(font, Component.literal(line), x, y, 0xFFC9BCA9); y += 11; }
        y += 2;
        g.text(font, Component.literal("강점 · " + fit(inspectedTradition.strength(), info.w - 48)), x, y, 0xFF89D5AD); y += 12;
        g.text(font, Component.literal("약점 · " + fit(inspectedTradition.weakness(), info.w - 48)), x, y, 0xFFD98E8E); y += 12;
        String prefix = "faction_" + inspectedTradition.name().toLowerCase(Locale.ROOT);
        g.text(font, Component.literal("본거지 · " + fit(ArcaneClientState.text(prefix + "_headquarters", "기록 없음"), info.w - 50)), x, y, 0xFFB9AD9D);
        action(g, l.join(), current == inspectedTradition ? "현재 소속" : "소속 등록", inside(mouseX, mouseY, l.join()), current != inspectedTradition, 0xFFD2AE6B);

        Rect shopHead = l.shopHeader();
        g.text(font, Component.literal("주문서 상점"), shopHead.x, shopHead.y + 2, 0xFFE5D4BA);
        tiny(g, academyCircle + "써클 · " + AcademyOfferCatalog.forCircle(academyCircle).size() + "종", shopHead.right(), shopHead.y + 4, 0xFF8F867A, .54F, true);
        drawCircleRail(g, l, academyCircle, mouseX, mouseY);

        Rect shop = l.shopViewport(); List<AcademyOfferCatalog.Offer> offers = AcademyOfferCatalog.forCircle(academyCircle); long marks = ArcaneClientState.longInteger("marks", 0);
        g.enableScissor(shop.x, shop.y, shop.right(), shop.bottom());
        for (int i = 0; i < offers.size(); i++) {
            Rect r = l.offerRow(i, scroll); AcademyOfferCatalog.Offer o = offers.get(i); boolean enough = marks >= o.basePrice();
            ArcaneRenderUtil.diamond(g, r.x + 11, r.y + 14, 4, enough ? circleColor(academyCircle) : 0xFF6A5552);
            g.text(font, Component.literal(fit(o.displayName(), r.w - 90)), r.x + 24, r.y + 5, enough ? 0xFFE1D4C0 : 0xFF8A7E74);
            tiny(g, fit(o.description(), Math.max(30, (r.w - 90) * 2)), r.x + 24, r.y + 18, 0xFF7E756B, .50F, false);
            tiny(g, o.basePrice() + " A", r.right() - 5, r.y + 10, enough ? 0xFFFFD179 : 0xFFB46F72, .58F, true);
            rule(g, r.bottom() - 1, r.x + 22, r.right(), 0xFF3E362D);
        }
        g.disableScissor();
    }

    private void drawCircleRail(GuiGraphicsExtractor g, Layout l, int selected, int mouseX, int mouseY) {
        for (int c = 1; c <= 9; c++) {
            Rect r = l.circle(c); boolean active = c == selected, unlocked = c <= ArcaneClientState.integer("circle", 1), hover = inside(mouseX, mouseY, r);
            int color = unlocked ? circleColor(c) : 0xFF4D4A47;
            if (active) g.fill(r.x, r.y + 2, r.x + 2, r.bottom() - 2, color);
            g.centeredText(font, Component.literal(Integer.toString(c)), r.x + r.w / 2, r.y + Math.max(2, r.h / 2 - 4), active ? 0xFFF5E2BC : hover ? 0xFFD9C7A8 : unlocked ? 0xFFA39A8C : 0xFF5C5852);
        }
    }

    private void drawSlot(GuiGraphicsExtractor g, Rect r, int slot, int mouseX, int mouseY) {
        SpellDefinition s = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null); boolean selected = activeSlot == slot;
        int accent = s == null ? 0xFF625D55 : ArcaneRenderUtil.schoolColor(s.school()), cx = r.x + r.w / 2, cy = r.y + r.h / 2;
        ArcaneRenderUtil.ring(g, cx, cy, Math.max(6, Math.min(10, r.h / 2 - 2)), selected ? 0xFFFFD275 : inside(mouseX, mouseY, r) ? accent : 0xFF514C46);
        if (s != null) ArcaneRenderUtil.spellRune(g, cx, cy, s, 4, selected ? 0xFFFFE6AA : 0xFFEAE0D1);
        tiny(g, Integer.toString(slot + 1), r.x + 3, r.y + 2, selected ? 0xFFFFD584 : 0xFF8B8378, .50F, false);
    }

    private void title(GuiGraphicsExtractor g, Rect body, String title, String sub) {
        g.text(font, Component.literal(title), body.x + 2, body.y + 2, 0xFFF0E2CD);
        g.text(font, Component.literal(sub), body.x + 2, body.y + 16, 0xFF93897C);
    }
    private void action(GuiGraphicsExtractor g, Rect r, String label, boolean hover, boolean enabled, int accent) {
        int color = enabled ? (hover ? 0xFFFFDE9A : accent) : 0xFF5A554F;
        g.fill(r.x, r.bottom() - 1, r.right(), r.bottom(), color);
        if (hover && enabled) g.fill(r.x, r.y, r.x + 2, r.bottom(), color);
        g.centeredText(font, Component.literal(fit(label, r.w - 8)), r.x + r.w / 2, r.y + 5,
                enabled ? (hover ? 0xFFFFF0D0 : 0xFFD8C9B3) : 0xFF6C665E);
    }
    private void drawNotice(GuiGraphicsExtractor g, Layout l) {
        String server = ArcaneClientState.noticeText(); String shown = !server.isBlank() ? server : (System.currentTimeMillis() <= noticeUntil ? notice : "");
        if (shown.isBlank()) return; int w = Math.min(l.body().w - 20, Math.max(120, font.width(shown) + 18)); int x = l.body().x + l.body().w / 2 - w / 2;
        g.fill(x, l.top + 6, x + w, l.top + 23, 0xEE0B0B0D); g.fill(x, l.top + 6, x + 2, l.top + 23, 0xFFD2AE6B);
        g.centeredText(font, Component.literal(fit(shown, w - 10)), x + w / 2, l.top + 10, 0xFFEADDC9);
    }
    private void tiny(GuiGraphicsExtractor g, String text, int x, int y, int color, float scale, boolean centered) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale, scale);
        if (centered) g.centeredText(font, Component.literal(text), 0, 0, color); else g.text(font, Component.literal(text), 0, 0, color);
        g.pose().popMatrix();
    }
    private void rule(GuiGraphicsExtractor g, int y, int x0, int x1, int color) { g.fill(x0, y, x1, y + 1, color); }

    private void request(String target) { ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(target)); }
    private void notice(String text) { notice = text; noticeUntil = System.currentTimeMillis() + 1800L; }
    private boolean usable(SpellDefinition s) { return s != null && ArcaneClientState.known().contains(s.id()) && s.circle() <= ArcaneClientState.integer("circle", 1); }
    private void equip(SpellDefinition s, int slot) { ClientPacketDistributor.sendToServer(new EquipSpellPayload(s.id(), slot)); notice((slot + 1) + "번 슬롯 · " + s.name()); }
    private int firstEmptySlot() { for (int i = 0; i < 5; i++) if (ArcaneClientState.slot(i).isBlank()) return i; return -1; }
    private void ensureInspectedSpell() { List<SpellDefinition> list = SpellCatalog.spellsInCircle(atlasCircle); if (list.isEmpty()) { inspectedSpellId = ""; return; } if (list.stream().noneMatch(s -> s.id().equals(inspectedSpellId))) inspectedSpellId = list.getFirst().id(); }
    private SpellDefinition inspectedSpell() { return SpellCatalog.spell(inspectedSpellId).orElse(null); }
    private int maxScroll(Layout l) {
        if ("academy".equals(page)) return Math.max(0, AcademyOfferCatalog.forCircle(academyCircle).size() * 31 - l.shopViewport().h);
        int count = SpellCatalog.spellsInCircle(atlasCircle).size();
        return effects ? Math.max(0, count * 68 - l.listViewport().h) : Math.max(0, count * 32 - l.listViewport().h);
    }
    private Layout layout() { int w = Math.min(840, Math.max(500, width - 40)), h = Math.min(500, Math.max(300, height - 32)); w = Math.min(w, width - 8); h = Math.min(h, height - 8); return new Layout((width - w) / 2, (height - h) / 2, w, h); }
    private List<String> wrap(String value, int pixels, int maxLines) {
        List<String> out = new ArrayList<>(); if (value == null || value.isBlank() || maxLines <= 0) return out; String remain = value.trim();
        for (int line = 0; line < maxLines && !remain.isEmpty(); line++) { if (font.width(remain) <= pixels) { out.add(remain); break; } int cut = remain.length(); while (cut > 1 && font.width(remain.substring(0, cut)) > pixels) cut--; int space = remain.lastIndexOf(' ', cut); if (space > Math.max(1, cut / 2)) cut = space; String part = remain.substring(0, cut).trim(); out.add(line == maxLines - 1 ? fit(part + "…", pixels) : part); remain = remain.substring(cut).trim(); }
        return out;
    }
    private String fit(String value, int pixels) { if (value == null || pixels <= 0) return ""; if (font.width(value) <= pixels) return value; String suffix = "…"; int allowed = Math.max(0, pixels - font.width(suffix)), end = value.length(); while (end > 0 && font.width(value.substring(0, end)) > allowed) end--; return end <= 0 ? suffix : value.substring(0, end) + suffix; }
    private static MagicTradition[] traditions() { return new MagicTradition[]{MagicTradition.ARCANE, MagicTradition.DIVINE, MagicTradition.OCCULT, MagicTradition.PRIMAL}; }
    private static boolean inside(double x, double y, Rect r) { return x >= r.x && y >= r.y && x < r.right() && y < r.bottom(); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static String one(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    private static int circleColor(int c) { return switch (c) { case 1 -> 0xFF82A9D6; case 2 -> 0xFF78B9C9; case 3 -> 0xFF80B99A; case 4 -> 0xFFD0B06D; case 5 -> 0xFFD18B6D; case 6 -> 0xFFB18AC5; case 7 -> 0xFF9B79D0; case 8 -> 0xFF806FC6; default -> 0xFFE0C56F; }; }

    private record Tab(String id, String label, String roman) {}
    private record Rect(int x, int y, int w, int h) { int right() { return x + w; } int bottom() { return y + h; } }
    private record Layout(int left, int top, int panelW, int panelH) {
        int right() { return left + panelW; } int bottom() { return top + panelH; }
        Rect close() { return new Rect(right() - 24, top + 5, 18, 18); }
        Rect tab(int i) { int step = Math.max(30, Math.min(42, (panelH - 30) / TABS.size())); return new Rect(left + 4, top + 26 + i * step, 48, Math.min(34, step - 2)); }
        Rect body() { return new Rect(left + 66, top + 28, panelW - 79, panelH - 40); }
        Rect mode(int i) { Rect b = body(); int w = 70; return new Rect(b.right() - 146 + i * 74, b.y + 1, w, 19); }
        Rect circle(int c) { Rect b = body(); int y = b.y + 35 + (c - 1) * Math.max(24, (b.h - 45) / 9); return new Rect(b.x, y, 31, 23); }
        Rect content() { Rect b = body(); return new Rect(b.x + 39, b.y + 34, b.w - 42, b.h - 38); }
        Rect listViewport() { Rect c = content(); if (panelW < 680) return c; return new Rect(c.x, c.y, Math.max(180, c.w - 245), c.h - 37); }
        Rect detail() { Rect c = content(); if (panelW < 680) return new Rect(c.right(), c.y, 0, 0); return new Rect(c.right() - 235, c.y, 235, c.h - 37); }
        Rect spellRow(int i, int scroll) { Rect v = listViewport(); return new Rect(v.x, v.y + i * 32 - scroll, v.w, 31); }
        Rect effectRow(int i, int scroll) { Rect v = listViewport(); return new Rect(v.x, v.y + i * 68 - scroll, v.w, 67); }
        Rect slot(int i) { Rect c = content(); int gap = 4, w = Math.max(38, (c.w - gap * 4) / 5); return new Rect(c.x + i * (w + gap), c.bottom() - 31, w, 29); }
        Rect equipAction() { Rect d = detail(); return d.w <= 0 ? new Rect(d.x, d.y, 0, 0) : new Rect(d.x + 8, d.bottom() - 24, d.w - 16, 20); }
        Rect tradition(int i) { Rect b = body(); int gap = 5, w = (b.w - 39 - gap * 3) / 4; return new Rect(b.x + 39 + i * (w + gap), b.y + 34, w, 20); }
        Rect academyInfo() { Rect b = body(); return new Rect(b.x + 39, b.y + 59, b.w - 42, Math.min(112, Math.max(90, b.h / 3))); }
        Rect join() { Rect r = academyInfo(); return new Rect(r.right() - 88, r.y + 2, 84, 20); }
        Rect shopHeader() { Rect info = academyInfo(); return new Rect(info.x, info.bottom() + 6, info.w, 18); }
        Rect shopViewport() { Rect b = body(), head = shopHeader(); return new Rect(b.x + 39, head.bottom() + 4, b.w - 42, Math.max(1, b.bottom() - head.bottom() - 8)); }
        Rect offerRow(int i, int scroll) { Rect v = shopViewport(); return new Rect(v.x, v.y + i * 31 - scroll, v.w, 29); }
    }
}

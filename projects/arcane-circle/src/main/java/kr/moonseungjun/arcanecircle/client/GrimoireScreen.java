package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellWorldLore;
import kr.moonseungjun.arcanecircle.network.ChooseTraditionPayload;
import kr.moonseungjun.arcanecircle.network.EquipSpellPayload;
import kr.moonseungjun.arcanecircle.network.PurchaseAcademyItemPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import kr.moonseungjun.arcanecircle.world.AcademyOfferCatalog;
import kr.moonseungjun.arcanecircle.world.MagicTradition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fixed, hierarchical grimoire UI. Circle categories and item lists never compete for space. */
public final class GrimoireScreen extends Screen {
    private static final List<Tab> TABS = List.of(
            new Tab("atlas", "주문"), new Tab("recipes", "융합"), new Tab("staffs", "지팡이"),
            new Tab("academy", "학원"), new Tab("core", "마력핵"));
    private static final Map<String, Integer> SAVED_SCROLL = new HashMap<>();
    private static int activeSlot;
    private static int atlasCircle;
    private static int academyCircle;

    private final String page;
    private int scroll;
    private String notice = "";
    private long noticeUntil;

    public GrimoireScreen(String page) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("구중 마도서"));
        this.page = normalize(page);
        this.scroll = SAVED_SCROLL.getOrDefault(scrollKey(), 0);
    }

    @Override protected void init() {
        super.init();
        scroll = clamp(scroll, 0, maxScroll(layout()));
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public void onClose() { SAVED_SCROLL.put(scrollKey(), scroll); super.onClose(); }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout l = layout();
        if (inside(event.x(), event.y(), l.close())) { onClose(); return true; }
        for (int i = 0; i < TABS.size(); i++) {
            if (inside(event.x(), event.y(), l.tab(i))) { request(TABS.get(i).id()); return true; }
        }
        if ("atlas".equals(page)) return clickAtlas(event, l) || super.mouseClicked(event, doubleClick);
        if ("academy".equals(page)) return clickAcademy(event, l) || super.mouseClicked(event, doubleClick);
        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickAtlas(MouseButtonEvent event, Layout l) {
        if (atlasCircle == 0) {
            for (int circle = 1; circle <= 9; circle++) {
                if (inside(event.x(), event.y(), l.circleCard(circle))) {
                    atlasCircle = circle; scroll = 0; saveScroll(); return true;
                }
            }
            return false;
        }
        if (inside(event.x(), event.y(), l.back())) { atlasCircle = 0; scroll = 0; saveScroll(); return true; }
        for (int i = 0; i < 5; i++) {
            if (inside(event.x(), event.y(), l.loadout(i))) { activeSlot = i; notice("슬롯 " + (i + 1) + " 선택"); return true; }
        }
        List<SpellDefinition> spells = SpellCatalog.spellsInCircle(atlasCircle);
        for (int i = 0; i < spells.size(); i++) {
            if (inside(event.x(), event.y(), l.spellCard(i, scroll))) { select(spells.get(i)); return true; }
        }
        return false;
    }

    private boolean clickAcademy(MouseButtonEvent event, Layout l) {
        MagicTradition[] traditions = traditions();
        for (int i = 0; i < traditions.length; i++) {
            if (inside(event.x(), event.y(), l.tradition(i))) {
                ClientPacketDistributor.sendToServer(new ChooseTraditionPayload(traditions[i].name()));
                notice(traditions[i].displayName() + " 조율 요청"); return true;
            }
        }
        if (academyCircle == 0) {
            for (int circle = 1; circle <= 9; circle++) {
                if (inside(event.x(), event.y(), l.academyCircleCard(circle))) {
                    academyCircle = circle; scroll = 0; saveScroll(); return true;
                }
            }
            return false;
        }
        if (inside(event.x(), event.y(), l.academyBack())) { academyCircle = 0; scroll = 0; saveScroll(); return true; }
        List<AcademyOfferCatalog.Offer> offers = AcademyOfferCatalog.forCircle(academyCircle);
        for (int i = 0; i < offers.size(); i++) {
            if (inside(event.x(), event.y(), l.offerCard(i, scroll))) {
                ClientPacketDistributor.sendToServer(new PurchaseAcademyItemPayload(offers.get(i).id()));
                notice(offers.get(i).displayName() + " 구매 요청"); return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout l = layout();
        if (scrollY == 0.0 || !inside(mouseX, mouseY, l.content())) return false;
        scroll = clamp(scroll + (scrollY < 0 ? 28 : -28), 0, maxScroll(l));
        saveScroll();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0xB7080B12);
        frame(g, l);
        header(g, l, mouseX, mouseY);
        switch (page) {
            case "recipes" -> recipes(g, l, mouseX, mouseY);
            case "staffs" -> staffs(g, l, mouseX, mouseY);
            case "academy" -> academy(g, l, mouseX, mouseY);
            case "core" -> core(g, l);
            default -> atlas(g, l, mouseX, mouseY);
        }
        drawNotice(g, l);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void atlas(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        if (atlasCircle == 0) {
            sectionTitle(g, l, "주문 써클", "");
            for (int circle = 1; circle <= 9; circle++) {
                drawCircleCard(g, l.circleCard(circle), circle, mouseX, mouseY, false);
            }
            return;
        }

        Rect back = l.back();
        button(g, back, "‹ 써클", inside(mouseX, mouseY, back), true);
        g.text(font, Component.literal(atlasCircle + "C 주문"), back.right() + 8, back.y() + 5, 0xFFF1E8FA);
        drawMana(g, l, back.y());
        for (int i = 0; i < 5; i++) drawLoadout(g, l.loadout(i), i, mouseX, mouseY);

        Rect viewport = l.atlasViewport();
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        List<SpellDefinition> spells = SpellCatalog.spellsInCircle(atlasCircle);
        Set<String> known = ArcaneClientState.known();
        for (int i = 0; i < spells.size(); i++) {
            drawSpell(g, l.spellCard(i, scroll), spells.get(i), known.contains(spells.get(i).id()), mouseX, mouseY);
        }
        g.disableScissor();
    }

    private void drawCircleCard(GuiGraphicsExtractor g, Rect r, int circle, int mouseX, int mouseY, boolean shop) {
        boolean unlocked = circle <= ArcaneClientState.integer("circle", 1);
        boolean hover = inside(mouseX, mouseY, r);
        int accent = unlocked ? circleColor(circle) : 0xFF4D4F59;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), hover ? 0xFF26344A : 0xFF121A29);
        g.fill(r.x(), r.bottom() - 2, r.right(), r.bottom(), accent);
        ArcaneRenderUtil.ring(g, r.x() + 15, r.y() + r.h() / 2, 8, accent);
        ArcaneRenderUtil.diamond(g, r.x() + 15, r.y() + r.h() / 2, 4, unlocked ? 0xFFF5ECFF : 0xFF686A72);
        g.text(font, Component.literal(circle + "C"), r.x() + 28, r.y() + 6, unlocked ? 0xFFF5EDFF : 0xFF85848C);
        int count = shop ? AcademyOfferCatalog.forCircle(circle).size() : SpellCatalog.spellsInCircle(circle).size();
        g.text(font, Component.literal(Integer.toString(count)), r.x() + 29, r.y() + 18, unlocked ? accent : 0xFF666872);
    }

    private void drawLoadout(GuiGraphicsExtractor g, Rect r, int slot, int mouseX, int mouseY) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        boolean selected = activeSlot == slot;
        boolean hover = inside(mouseX, mouseY, r);
        int accent = spell == null ? 0xFF596171 : ArcaneRenderUtil.schoolColor(spell.school());
        g.fill(r.x(), r.y(), r.right(), r.bottom(), selected ? 0xFF2B2940 : hover ? 0xFF202B3D : 0xFF111827);
        g.fill(r.x(), r.bottom() - 2, r.right(), r.bottom(), selected ? 0xFFFFD36B : accent);
        String name = spell == null ? (slot + 1) + "  -" : (slot + 1) + "  " + spell.name();
        g.text(font, Component.literal(fit(name, r.w() - 8)), r.x() + 4, r.y() + 5,
                selected ? 0xFFFFE3A2 : 0xFFE7E0ED);
        if (ArcaneClientState.cooldownRemainingTicks(slot) > 0) {
            int fill = (int) Math.round((r.w() - 2) * ArcaneClientState.cooldownFraction(slot));
            g.fill(r.x() + 1, r.bottom() - 3, r.x() + 1 + fill, r.bottom() - 1, 0xFFE46D78);
        }
    }

    private void drawSpell(GuiGraphicsExtractor g, Rect r, SpellDefinition spell, boolean known, int mouseX, int mouseY) {
        if (r.bottom() < 0 || r.y() > height) return;
        boolean usable = known && spell.circle() <= ArcaneClientState.integer("circle", 1);
        boolean hover = inside(mouseX, mouseY, r);
        boolean equipped = ArcaneClientState.slots().contains(spell.id());
        int accent = ArcaneRenderUtil.schoolColor(spell.school());
        g.fill(r.x(), r.y(), r.right(), r.bottom(), hover ? 0xFF25334A : 0xFF111927);
        g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), usable ? accent : 0xFF484A52);
        ArcaneRenderUtil.ring(g, r.x() + 16, r.y() + r.h() / 2, 9, usable ? accent : 0xFF555760);
        if (usable) ArcaneRenderUtil.spellRune(g, r.x() + 16, r.y() + r.h() / 2, spell, 5, 0xFFF8F1FF);
        else ArcaneRenderUtil.diamond(g, r.x() + 16, r.y() + r.h() / 2, 4, 0xFF696A72);
        int tx = r.x() + 30;
        g.text(font, Component.literal(fit(spell.name(), r.w() - 34)), tx, r.y() + 7,
                usable ? (equipped ? 0xFFFFD98A : 0xFFF0E8F8) : 0xFF777881);
        String meta = "MP " + spell.manaCost() + " · " + String.format("%.1fs", spell.cooldownTicks() / 20.0)
                + (equipped ? " · 장착" : known ? "" : " · 미습득");
        g.text(font, Component.literal(fit(meta, r.w() - 34)), tx, r.y() + 21,
                usable ? 0xFF9FB6D2 : 0xFF6F7078);
    }

    private void recipes(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        sectionTitle(g, l, "융합", "");
        Rect viewport = l.listViewport(24);
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        List<SpellCatalog.FusionFormula> formulas = SpellCatalog.fusions();
        for (int i = 0; i < formulas.size(); i++) {
            Rect r = l.wideCard(i, scroll, 40, 24);
            SpellCatalog.FusionFormula formula = formulas.get(i);
            SpellDefinition result = SpellCatalog.spell(formula.result()).orElseThrow();
            int accent = ArcaneRenderUtil.schoolColor(result.school());
            g.fill(r.x(), r.y(), r.right(), r.bottom(), inside(mouseX, mouseY, r) ? 0xFF25344B : 0xFF111927);
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), accent);
            g.text(font, Component.literal(fit(result.circle() + "C  " + result.name(), r.w() - 10)), r.x() + 6, r.y() + 6, 0xFFF0E7FA);
            String chain = formula.ingredients().stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                    .reduce((a, b) -> a + " + " + b).orElse("");
            g.text(font, Component.literal(fit(chain, r.w() - 10)), r.x() + 6, r.y() + 20, 0xFF93A2B8);
        }
        g.disableScissor();
    }

    private void staffs(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        sectionTitle(g, l, "지팡이", "");
        Rect viewport = l.listViewport(24);
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        List<StaffProfile> profiles = ModItems.profiles();
        for (int i = 0; i < profiles.size(); i++) {
            Rect r = l.staffCard(i, scroll);
            StaffProfile p = profiles.get(i);
            boolean equipped = p.id().equals(ArcaneClientState.text("staff_id", "none"));
            int accent = p.favoredSchool() == null ? 0xFFFFC866 : ArcaneRenderUtil.schoolColor(p.favoredSchool());
            g.fill(r.x(), r.y(), r.right(), r.bottom(), inside(mouseX, mouseY, r) ? 0xFF26354B : 0xFF111927);
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), equipped ? 0xFFFFD36B : accent);
            g.text(font, Component.literal(fit(p.displayName() + (equipped ? " · 장착" : ""), r.w() - 12)), r.x() + 6, r.y() + 6,
                    equipped ? 0xFFFFDFA0 : 0xFFF0E8FA);
            g.text(font, Component.literal(fit(staffStats(p), r.w() - 12)), r.x() + 6, r.y() + 21, 0xFF9EADC2);
        }
        g.disableScissor();
    }

    private void academy(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c = l.content();
        long marks = ArcaneClientState.longInteger("marks", 0L);
        MagicTradition current = MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND"));
        g.text(font, Component.literal("아르카나 " + marks), c.x() + 2, c.y() + 4, 0xFFFFD66F);
        g.text(font, Component.literal(current.displayName()), c.right() - font.width(current.displayName()) - 2, c.y() + 4, 0xFFD9C8ED);

        MagicTradition[] traditions = traditions();
        for (int i = 0; i < traditions.length; i++) {
            Rect r = l.tradition(i);
            MagicTradition t = traditions[i];
            boolean selected = current == t;
            g.fill(r.x(), r.y(), r.right(), r.bottom(), selected ? 0xFF3D3157 : inside(mouseX, mouseY, r) ? 0xFF253249 : 0xFF111827);
            g.fill(r.x(), r.bottom() - 2, r.right(), r.bottom(), selected ? 0xFFFFD36B : 0xFF6F5A8E);
            g.centeredText(font, Component.literal(t.displayName()), r.x() + r.w() / 2, r.y() + 5,
                    selected ? 0xFFFFE4A7 : 0xFFE9E0F1);
        }

        if (academyCircle == 0) {
            for (int circle = 1; circle <= 9; circle++) {
                drawCircleCard(g, l.academyCircleCard(circle), circle, mouseX, mouseY, true);
            }
            return;
        }

        Rect back = l.academyBack();
        button(g, back, "‹ 써클", inside(mouseX, mouseY, back), true);
        g.text(font, Component.literal(academyCircle + "C 상점"), back.right() + 8, back.y() + 5, 0xFFF1E8FA);
        Rect viewport = l.academyViewport();
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        List<AcademyOfferCatalog.Offer> offers = AcademyOfferCatalog.forCircle(academyCircle);
        for (int i = 0; i < offers.size(); i++) {
            Rect r = l.offerCard(i, scroll);
            AcademyOfferCatalog.Offer offer = offers.get(i);
            long price = offer.basePrice();
            if (offer.kind() == AcademyOfferCatalog.Kind.SPELLBOOK && current != MagicTradition.UNBOUND
                    && SpellWorldLore.tradition(offer.targetId()) == current) {
                price = Math.max(1L, Math.round(price * 0.82));
            }
            boolean enough = marks >= price;
            g.fill(r.x(), r.y(), r.right(), r.bottom(), inside(mouseX, mouseY, r) ? 0xFF25344B : 0xFF111927);
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), enough ? 0xFF70C69D : 0xFFB75B68);
            g.text(font, Component.literal(fit(offer.displayName(), r.w() - 10)), r.x() + 6, r.y() + 6,
                    enough ? 0xFFF0E8FA : 0xFF988A91);
            g.text(font, Component.literal(price + " A"), r.x() + 6, r.y() + 21,
                    enough ? 0xFFFFD66F : 0xFFE0717C);
        }
        g.disableScissor();
    }

    private void core(GuiGraphicsExtractor g, Layout l) {
        sectionTitle(g, l, "마력핵", "");
        Rect c = l.content();
        int circle = ArcaneClientState.integer("circle", 1);
        int iconX = c.x() + 42;
        int iconY = c.y() + 68;
        ArcaneRenderUtil.ring(g, iconX, iconY, 22, 0xFF9C6ED0);
        ArcaneRenderUtil.diamond(g, iconX, iconY, 10, 0xFFEAD9FF);
        g.centeredText(font, Component.literal(circle + "C"), iconX, iconY - 4, 0xFFFFFFFF);
        infoPanel(g, c.x() + 80, c.y() + 28, 180, "상태", List.of(
                "MP " + ArcaneClientState.integer("mana", 0) + "/" + ArcaneClientState.integer("max", 100),
                "회복 " + String.format("%.1f", ArcaneClientState.regenPerSecond()) + "/초",
                "통찰 " + ArcaneClientState.integer("insight", 0),
                "아르카나 " + ArcaneClientState.longInteger("marks", 0L)));
        infoPanel(g, c.x() + 268, c.y() + 28, Math.max(150, c.w() - 268), "장비", List.of(
                ArcaneClientState.text("staff", "맨손"),
                "학부 " + MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND")).displayName(),
                "저단계 주문 자동 단축", "건축 허용"));
    }

    private void infoPanel(GuiGraphicsExtractor g, int x, int y, int w, String title, List<String> lines) {
        g.fill(x, y, x + w, y + 82, 0xFF101827); g.fill(x, y, x + w, y + 2, 0xFF745797);
        g.text(font, Component.literal(title), x + 8, y + 8, 0xFFE8D9F5);
        for (int i = 0; i < lines.size(); i++) g.text(font, Component.literal(fit(lines.get(i), w - 16)), x + 8, y + 25 + i * 13, 0xFF9EABC0);
    }

    private void frame(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.left() - 2, l.top() - 2, l.right() + 2, l.bottom() + 2, 0xFF05070D);
        g.fill(l.left(), l.top(), l.right(), l.bottom(), 0xFA0D1422);
        g.fill(l.left(), l.top(), l.left() + 3, l.bottom(), 0xFF8B63B4);
        g.fill(l.left(), l.top(), l.right(), l.top() + 2, 0xFFB78BDD);
    }

    private void header(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        g.text(font, Component.literal("구중 마도서"), l.left() + 14, l.top() + 13, 0xFFF2E8FA);
        for (int i = 0; i < TABS.size(); i++) {
            Rect r = l.tab(i); boolean active = TABS.get(i).id().equals(page); boolean hover = inside(mouseX, mouseY, r);
            g.centeredText(font, Component.literal(TABS.get(i).label()), r.x() + r.w() / 2, r.y() + 6,
                    active ? 0xFFFFE3A4 : hover ? 0xFFD8C3E9 : 0xFF8B8296);
            if (active) g.fill(r.x() + 8, r.bottom() - 2, r.right() - 8, r.bottom(), 0xFFB57ADC);
        }
        Rect close = l.close(); button(g, close, "×", inside(mouseX, mouseY, close), false);
    }

    private void sectionTitle(GuiGraphicsExtractor g, Layout l, String title, String subtitle) {
        Rect c = l.content();
        g.text(font, Component.literal(title), c.x() + 2, c.y() + 5, 0xFFF0E7F8);
    }
    private void drawMana(GuiGraphicsExtractor g, Layout l, int y) {
        int mana = ArcaneClientState.integer("mana", 0), max = Math.max(1, ArcaneClientState.integer("max", 100));
        String s = ArcaneClientState.integer("circle", 1) + "C  MP " + mana + "/" + max;
        g.text(font, Component.literal(s), l.content().right() - font.width(s) - 4, y + 5, 0xFF9EC1F2);
    }
    private void button(GuiGraphicsExtractor g, Rect r, String text, boolean hover, boolean wide) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), hover ? 0xFF2C3A51 : 0xFF141D2B);
        if (wide) g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), 0xFF9E72C8);
        g.centeredText(font, Component.literal(text), r.x() + r.w() / 2, r.y() + 5, hover ? 0xFFFFFFFF : 0xFFD8CFDF);
    }
    private void footer(GuiGraphicsExtractor g, Layout l, String text) {
        // v0.10 intentionally leaves the bottom edge empty: no tutorial sentence competes with content.
    }
    private void drawNotice(GuiGraphicsExtractor g, Layout l) {
        if (notice.isBlank() || System.currentTimeMillis() > noticeUntil) return;
        int w = Math.min(l.panelW() - 30, Math.max(110, font.width(notice) + 20)); int x = l.cx() - w / 2;
        g.fill(x, l.top() + 48, x + w, l.top() + 67, 0xF0181324); g.fill(x, l.top() + 48, x + w, l.top() + 50, 0xFFFFD36B);
        g.centeredText(font, Component.literal(notice), l.cx(), l.top() + 54, 0xFFFFE8B4);
    }
    private void notice(String text) { notice = text; noticeUntil = System.currentTimeMillis() + 1800L; }

    private void select(SpellDefinition spell) {
        int circle = ArcaneClientState.integer("circle", 1);
        if (!ArcaneClientState.known().contains(spell.id())) { notice("아직 습득하지 않은 주문입니다"); return; }
        if (spell.circle() > circle) { notice(spell.circle() + "써클 마력핵이 필요합니다"); return; }
        ClientPacketDistributor.sendToServer(new EquipSpellPayload(spell.id(), activeSlot));
        notice((activeSlot + 1) + "번 슬롯 · " + spell.name());
    }
    private void request(String next) { saveScroll(); ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(next)); }
    private void saveScroll() { SAVED_SCROLL.put(scrollKey(), scroll); }
    private String scrollKey() { return page + ":" + ("atlas".equals(page) ? atlasCircle : "academy".equals(page) ? academyCircle : 0); }

    private int maxScroll(Layout l) {
        return switch (page) {
            case "recipes" -> l.maxWideScroll(SpellCatalog.fusions().size(), 52, 36);
            case "staffs" -> l.maxStaffScroll(ModItems.profiles().size());
            case "academy" -> academyCircle == 0 ? 0 : l.maxOfferScroll(AcademyOfferCatalog.forCircle(academyCircle).size());
            case "atlas" -> atlasCircle == 0 ? 0 : l.maxSpellScroll(SpellCatalog.spellsInCircle(atlasCircle).size());
            default -> 0;
        };
    }

    private Layout layout() {
        int panelW = Math.min(720, Math.max(360, width - 40));
        int panelH = Math.min(410, Math.max(260, height - 36));
        panelW = Math.min(panelW, Math.max(1, width - 12));
        panelH = Math.min(panelH, Math.max(1, height - 12));
        return new Layout((width - panelW) / 2, (height - panelH) / 2, panelW, panelH);
    }

    private String fit(String value, int pixels) {
        if (value == null || pixels <= 0) return "";
        if (font.width(value) <= pixels) return value;
        String suffix = "…"; int allowed = Math.max(0, pixels - font.width(suffix)); int end = value.length();
        while (end > 0 && font.width(value.substring(0, end)) > allowed) end--;
        return end <= 0 ? suffix : value.substring(0, end) + suffix;
    }
    private static String staffStats(StaffProfile p) {
        return "MP " + signed(p.maxManaBonus()) + " · 위력 " + signed((int)Math.round((p.powerMultiplier()-1)*100))
                + "% · 범위 " + signed((int)Math.round((p.rangeMultiplier()-1)*100)) + "% · 쿨 "
                + signed((int)Math.round((p.cooldownMultiplier()-1)*100)) + "%";
    }
    private static String signed(int v) { return v >= 0 ? "+" + v : Integer.toString(v); }
    private static MagicTradition[] traditions() { return new MagicTradition[]{MagicTradition.ARCANE, MagicTradition.DIVINE, MagicTradition.OCCULT, MagicTradition.PRIMAL}; }
    private static String normalize(String p) { return "recipes".equals(p)||"staffs".equals(p)||"academy".equals(p)||"core".equals(p) ? p : "atlas"; }
    private static boolean inside(double x, double y, Rect r) { return x >= r.x() && y >= r.y() && x < r.right() && y < r.bottom(); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static int circleColor(int c) { return switch(c){case 1->0xFF7DA7E8;case 2->0xFF78B7D8;case 3->0xFF75C6B0;case 4->0xFFE0B86C;case 5->0xFFE18A72;case 6->0xFFC783D9;case 7->0xFFA879E7;case 8->0xFF8A6BE0;default->0xFFE4C56A;}; }
    private static String circleSubtitle(int c) { return switch(c){case 1->"기초 회로";case 2->"전투 입문";case 3->"정규 마도";case 4->"상급 전술";case 5->"전장 지배";case 6->"대마법";case 7->"차원 마법";case 8->"현실 간섭";default->"궁극 마법";}; }

    private record Tab(String id, String label) {}
    private record Rect(int x, int y, int w, int h) { int right(){return x+w;} int bottom(){return y+h;} }
    private record Layout(int left, int top, int panelW, int panelH) {
        int right(){return left+panelW;} int bottom(){return top+panelH;} int cx(){return left+panelW/2;}
        Rect close(){return new Rect(right()-27,top+7,20,20);}
        Rect tab(int i){int w=58;return new Rect(cx()-TABS.size()*w/2+i*w,top+5,w,25);}
        Rect content(){return new Rect(left+12,top+36,panelW-24,panelH-44);}
        Rect back(){Rect c=content();return new Rect(c.x(),c.y(),66,19);}

        Rect circleCard(int circle){
            Rect c=content(); int cols=c.w()>=420?9:3; int gap=4; int col=(circle-1)%cols,row=(circle-1)/cols;
            int w=(c.w()-gap*(cols-1))/cols; int h=cols==9?36:34;
            return new Rect(c.x()+col*(w+gap),c.y()+28+row*(h+gap),w,h);
        }
        Rect loadout(int i){Rect c=content();int gap=4;int w=(c.w()-gap*4)/5;return new Rect(c.x()+i*(w+gap),c.y()+25,w,18);}
        Rect atlasViewport(){Rect c=content();return new Rect(c.x(),c.y()+49,c.w(),c.h()-50);}
        Rect spellCard(int i,int scroll){
            Rect v=atlasViewport(); int cols=v.w()>=540?5:v.w()>=330?3:2; int gap=5;
            int w=(v.w()-gap*(cols-1))/cols; int row=i/cols,col=i%cols;
            return new Rect(v.x()+col*(w+gap),v.y()+row*48-scroll,w,43);
        }
        int maxSpellScroll(int count){Rect v=atlasViewport();int cols=v.w()>=540?5:v.w()>=330?3:2;return Math.max(0,((count+cols-1)/cols)*48-v.h());}

        Rect listViewport(int topOffset){Rect c=content();return new Rect(c.x(),c.y()+topOffset,c.w(),c.h()-topOffset);}
        Rect wideCard(int i,int scroll,int h,int topOffset){
            Rect v=listViewport(topOffset);int cols=v.w()>=520?3:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;
            int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*(h+5)-scroll,w,h);
        }
        int maxWideScroll(int count,int h,int topOffset){Rect v=listViewport(topOffset);int cols=v.w()>=520?3:2;return Math.max(0,((count+cols-1)/cols)*(h+5)-v.h());}
        Rect staffCard(int i,int scroll){Rect v=listViewport(24);int cols=v.w()>=520?3:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*44-scroll,w,39);}
        int maxStaffScroll(int count){Rect v=listViewport(24);int cols=v.w()>=520?3:2;return Math.max(0,((count+cols-1)/cols)*44-v.h());}

        Rect tradition(int i){Rect c=content();int gap=4;int w=(c.w()-gap*3)/4;return new Rect(c.x()+i*(w+gap),c.y()+20,w,20);}
        Rect academyCircleCard(int circle){
            Rect c=content();int cols=c.w()>=420?9:3,gap=4;int w=(c.w()-gap*(cols-1))/cols;int col=(circle-1)%cols,row=(circle-1)/cols;int h=34;
            return new Rect(c.x()+col*(w+gap),c.y()+48+row*(h+gap),w,h);
        }
        Rect academyBack(){Rect c=content();return new Rect(c.x(),c.y()+45,66,19);}
        Rect academyViewport(){Rect c=content();return new Rect(c.x(),c.y()+70,c.w(),c.h()-71);}
        Rect offerCard(int i,int scroll){Rect v=academyViewport();int cols=v.w()>=540?4:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*43-scroll,w,38);}
        int maxOfferScroll(int count){Rect v=academyViewport();int cols=v.w()>=540?4:2;return Math.max(0,((count+cols-1)/cols)*43-v.h());}
    }
}

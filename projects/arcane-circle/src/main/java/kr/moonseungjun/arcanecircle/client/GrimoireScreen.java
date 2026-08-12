package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellWorldLore;
import kr.moonseungjun.arcanecircle.network.ChooseTraditionPayload;
import kr.moonseungjun.arcanecircle.network.EquipSpellPayload;
import kr.moonseungjun.arcanecircle.network.PurchaseAcademyItemPayload;
import kr.moonseungjun.arcanecircle.network.QuestActionPayload;
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
            new Tab("academy", "마도회"), new Tab("quests", "의뢰"), new Tab("core", "마력핵"));
    private static final Map<String, Integer> SAVED_SCROLL = new HashMap<>();
    private static int activeSlot = -1;
    private static String selectedStaffId = "";
    private static int atlasCircle;
    private static int fusionCircle;
    private static int academyCircle;
    private static MagicTradition inspectedTradition = MagicTradition.ARCANE;

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
        if ("recipes".equals(page)) return clickRecipes(event, l) || super.mouseClicked(event, doubleClick);
        if ("academy".equals(page)) return clickAcademy(event, l) || super.mouseClicked(event, doubleClick);
        if ("quests".equals(page)) return clickQuests(event, l) || super.mouseClicked(event, doubleClick);
        if ("staffs".equals(page)) return clickStaffs(event, l) || super.mouseClicked(event, doubleClick);
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
            if (inside(event.x(), event.y(), l.loadout(i))) {
                if (activeSlot == i) { activeSlot = -1; notice("주문 슬롯 선택 취소"); }
                else { activeSlot = i; notice("슬롯 " + (i + 1) + " 선택"); }
                return true;
            }
        }
        List<SpellDefinition> spells = SpellCatalog.spellsInCircle(atlasCircle);
        for (int i = 0; i < spells.size(); i++) {
            if (inside(event.x(), event.y(), l.spellCard(i, scroll))) { select(spells.get(i)); return true; }
        }
        return false;
    }

    private boolean clickRecipes(MouseButtonEvent event, Layout l) {
        if (fusionCircle == 0) {
            for (int circle = 1; circle <= 9; circle++) {
                if (inside(event.x(), event.y(), l.circleCard(circle))) {
                    fusionCircle = circle;
                    scroll = 0;
                    saveScroll();
                    return true;
                }
            }
            return false;
        }
        if (inside(event.x(), event.y(), l.back())) {
            fusionCircle = 0;
            scroll = 0;
            saveScroll();
            return true;
        }
        return false;
    }

    private boolean clickAcademy(MouseButtonEvent event, Layout l) {
        MagicTradition[] traditions = traditions();
        for (int i = 0; i < traditions.length; i++) {
            if (inside(event.x(), event.y(), l.tradition(i))) {
                inspectedTradition = traditions[i];
                academyCircle = 0;
                scroll = 0;
                saveScroll();
                notice(inspectedTradition.displayName() + " 상세 정보");
                return true;
            }
        }
        if (inside(event.x(), event.y(), l.traditionJoin())) {
            ClientPacketDistributor.sendToServer(new ChooseTraditionPayload(inspectedTradition.name()));
            notice(inspectedTradition.displayName() + " 소속 등록 요청");
            return true;
        }
        if (academyCircle == 0) {
            for (int circle = 1; circle <= 9; circle++) {
                if (inside(event.x(), event.y(), l.academyCircleCard(circle, scroll))) {
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

    private boolean clickQuests(MouseButtonEvent event, Layout l) {
        String offered = ArcaneClientState.text("quest_offer_id", "");
        if (!offered.isBlank()) {
            if (inside(event.x(), event.y(), l.questAccept(scroll))) {
                ClientPacketDistributor.sendToServer(new QuestActionPayload("accept"));
                notice("의뢰 수락 요청");
                return true;
            }
            if (inside(event.x(), event.y(), l.questReject(scroll))) {
                ClientPacketDistributor.sendToServer(new QuestActionPayload("reject"));
                notice("의뢰 거절 요청");
                return true;
            }
        }
        int count = Math.min(3, ArcaneClientState.integer("quest_count", 0));
        for (int i = 0; i < count; i++) {
            int progress = ArcaneClientState.integer("quest_" + i + "_progress", 0);
            int target = ArcaneClientState.integer("quest_" + i + "_target", 0);
            if (target > 0 && progress >= target && inside(event.x(), event.y(), l.questClaim(i, scroll))) {
                ClientPacketDistributor.sendToServer(new QuestActionPayload("claim:" + i));
                notice((i + 1) + "번 의뢰 보상 수령 요청");
                return true;
            }
        }
        return false;
    }

    private boolean clickStaffs(MouseButtonEvent event, Layout l) {
        List<StaffProfile> profiles = ModItems.profiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (!inside(event.x(), event.y(), l.staffCard(i, scroll))) continue;
            String id = profiles.get(i).id();
            selectedStaffId = id.equals(selectedStaffId) ? "" : id;
            notice(selectedStaffId.isBlank() ? "지팡이 조합법 닫기" : profiles.get(i).displayName() + " 조합법");
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout l = layout();
        if (scrollY == 0.0 || !inside(mouseX, mouseY, l.content())) return false;
        int step = "academy".equals(page) && academyCircle == 0 ? 38
                : "quests".equals(page) ? 50 : 28;
        scroll = clamp(scroll + (scrollY < 0 ? step : -step), 0, maxScroll(l));
        saveScroll();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        g.fill(0, 0, width, height, 0xD1080710);
        frame(g, l);
        header(g, l, mouseX, mouseY);
        switch (page) {
            case "recipes" -> recipes(g, l, mouseX, mouseY);
            case "staffs" -> staffs(g, l, mouseX, mouseY);
            case "academy" -> academy(g, l, mouseX, mouseY);
            case "quests" -> quests(g, l, mouseX, mouseY);
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
        CodexVisualLanguage.card(g, r.x(), r.y(), r.w(), r.h(), accent, hover, false, unlocked);
        CodexVisualLanguage.seal(g, r.x() + 15, r.y() + r.h() / 2, 8, accent, circle);
        g.text(font, Component.literal(circle + "C"), r.x() + 28, r.y() + 6, unlocked ? 0xFFF5EDFF : 0xFF85848C);
        int count = shop ? AcademyOfferCatalog.forCircle(circle).size() : SpellCatalog.spellsInCircle(circle).size();
        g.text(font, Component.literal(Integer.toString(count)), r.x() + 29, r.y() + 18, unlocked ? accent : 0xFF666872);
    }

    private void drawLoadout(GuiGraphicsExtractor g, Rect r, int slot, int mouseX, int mouseY) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        boolean selected = activeSlot == slot;
        boolean hover = inside(mouseX, mouseY, r);
        int accent = spell == null ? 0xFF596171 : ArcaneRenderUtil.schoolColor(spell.school());
        CodexVisualLanguage.card(g, r.x(), r.y(), r.w(), r.h(), selected ? 0xFFFFD36B : accent, hover, selected, true);
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
        CodexVisualLanguage.card(g, r.x(), r.y(), r.w(), r.h(), accent, hover, equipped, usable);
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
        if (fusionCircle == 0) {
            sectionTitle(g, l, "융합 써클", "결과 주문의 써클별로 분류합니다");
            for (int circle = 1; circle <= 9; circle++) drawFusionCircleCard(g, l.circleCard(circle), circle, mouseX, mouseY);
            return;
        }

        Rect back = l.back();
        button(g, back, "‹ 써클", inside(mouseX, mouseY, back), true);
        g.text(font, Component.literal(fusionCircle + "C 융합"), back.right() + 8, back.y() + 5, 0xFFF1E8FA);
        Rect viewport = l.listViewport(49);
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        List<SpellCatalog.FusionFormula> formulas = fusionFormulasInCircle(fusionCircle);
        for (int i = 0; i < formulas.size(); i++) {
            Rect r = l.wideCard(i, scroll, 54, 49);
            SpellCatalog.FusionFormula formula = formulas.get(i);
            SpellDefinition result = SpellCatalog.spell(formula.result()).orElseThrow();
            int accent = ArcaneRenderUtil.schoolColor(result.school());
            int playerCircle = ArcaneClientState.integer("circle", 1);
            boolean circleReady = result.circle() <= playerCircle;
            boolean learned = formula.ingredients().stream().allMatch(id -> ArcaneClientState.known().contains(id));
            boolean cooldownReady = formula.ingredients().stream()
                    .allMatch(id -> ArcaneClientState.cooldownRemainingTicks(id) <= 0);
            boolean ready = circleReady && learned && cooldownReady;
            CodexVisualLanguage.card(g, r.x(), r.y(), r.w(), r.h(), ready ? accent : 0xFFB75B68,
                    inside(mouseX, mouseY, r), ready, ready);
            g.text(font, Component.literal(fit(result.circle() + "C  " + result.name(), r.w() - 10)), r.x() + 6, r.y() + 5, 0xFFF0E7FA);
            String chain = formula.ingredients().stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                    .reduce((a, b) -> a + " + " + b).orElse("");
            g.text(font, Component.literal(fit(chain, r.w() - 10)), r.x() + 6, r.y() + 18, 0xFF93A2B8);
            String meta = "MP " + result.manaCost() + " · 쿨 " + String.format("%.1fs", result.cooldownTicks() / 20.0)
                    + " · 숙련 " + ArcaneClientState.mastery(result.id()) + "/" + SpellCatalog.masteryRequired(result.id());
            g.text(font, Component.literal(fit(meta, r.w() - 10)), r.x() + 6, r.y() + 31, 0xFF9FB6D2);
            String readiness = !circleReady
                    ? "융합 불가 · 필요 " + result.circle() + "써클 / 현재 " + playerCircle + "써클"
                    : !learned ? "융합 불가 · 재료 주문 미습득"
                    : !cooldownReady ? "융합 불가 · 재료 주문 쿨타임 대기 중"
                    : "융합 가능 · 재료 주문 준비 완료";
            g.text(font, Component.literal(fit(readiness, r.w() - 10)), r.x() + 6, r.y() + 43,
                    ready ? 0xFF76D5A5 : 0xFFE07882);
        }
        g.disableScissor();
    }

    private void drawFusionCircleCard(GuiGraphicsExtractor g, Rect r, int circle, int mouseX, int mouseY) {
        int count = fusionFormulasInCircle(circle).size();
        boolean unlocked = circle <= ArcaneClientState.integer("circle", 1);
        boolean hover = inside(mouseX, mouseY, r);
        int accent = count == 0 ? 0xFF4D4F59 : unlocked ? circleColor(circle) : 0xFF5B5364;
        CodexVisualLanguage.card(g, r.x(), r.y(), r.w(), r.h(), accent, hover, false, count > 0 && unlocked);
        ArcaneRenderUtil.ring(g, r.x() + 15, r.y() + r.h() / 2, 8, accent);
        g.text(font, Component.literal(circle + "C"), r.x() + 28, r.y() + 6, count > 0 ? 0xFFF5EDFF : 0xFF777881);
        g.text(font, Component.literal(Integer.toString(count)), r.x() + 29, r.y() + 18, accent);
    }

    private void staffs(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        sectionTitle(g, l, "지팡이", "클릭하면 조합법과 획득 경로를 확인합니다");
        Rect viewport = l.staffViewport();
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        List<StaffProfile> profiles = ModItems.profiles();
        for (int i = 0; i < profiles.size(); i++) {
            Rect r = l.staffCard(i, scroll);
            StaffProfile p = profiles.get(i);
            boolean equipped = p.id().equals(ArcaneClientState.text("staff_id", "none"));
            boolean selected = p.id().equals(selectedStaffId);
            int accent = p.favoredSchool() == null ? 0xFFFFC866 : ArcaneRenderUtil.schoolColor(p.favoredSchool());
            CodexVisualLanguage.card(g, r.x(), r.y(), r.w(), r.h(), equipped ? 0xFFFFD36B : accent,
                    inside(mouseX, mouseY, r), selected || equipped, true);
            g.text(font, Component.literal(fit(p.displayName() + (equipped ? " · 장착" : ""), r.w() - 12)), r.x() + 6, r.y() + 6,
                    equipped ? 0xFFFFDFA0 : 0xFFF0E8FA);
            g.text(font, Component.literal(fit(staffStats(p), r.w() - 12)), r.x() + 6, r.y() + 21, 0xFF9EADC2);
        }
        g.disableScissor();
        drawStaffRecipe(g, l);
    }

    private void drawStaffRecipe(GuiGraphicsExtractor g, Layout l) {
        if (selectedStaffId.isBlank()) return;
        StaffProfile p = ModItems.profile(selectedStaffId);
        if (p == StaffProfile.NONE) return;
        Rect r = l.staffRecipe();
        CodexVisualLanguage.panel(g, r.x(), r.y(), r.w(), r.h(), 0xFFFFC866);
        g.text(font, Component.literal(fit(p.displayName() + " · 조합법", r.w() - 16)), r.x() + 8, r.y() + 6, 0xFFFFDFA0);
        g.text(font, Component.literal(fit(p.recipeHint(), r.w() - 16)), r.x() + 8, r.y() + 20, 0xFFE9E0F1);
        g.text(font, Component.literal(fit(p.summary(), r.w() - 16)), r.x() + 8, r.y() + 34, 0xFF9EADC2);
        g.text(font, Component.literal("마법사 주민 상점에서는 아르카나로 완제품 구매 가능"), r.x() + 8, r.y() + 48, 0xFFFFD66F);
    }

    private void academy(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c = l.content();
        long marks = ArcaneClientState.longInteger("marks", 0L);
        MagicTradition current = MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND"));
        if (inspectedTradition == MagicTradition.UNBOUND) inspectedTradition = MagicTradition.ARCANE;
        g.text(font, Component.literal("아르카나 " + marks), c.x() + 2, c.y() + 4, 0xFFFFD66F);
        g.text(font, Component.literal("현재 " + current.displayName()), c.right() - font.width("현재 " + current.displayName()) - 2, c.y() + 4, 0xFFD9C8ED);
        MagicTradition[] traditions = traditions();
        for (int i = 0; i < traditions.length; i++) {
            Rect r = l.tradition(i); MagicTradition t = traditions[i];
            boolean inspected = inspectedTradition == t, joined = current == t;
            CodexVisualLanguage.card(g, r.x(), r.y(), r.w(), r.h(),
                    joined ? 0xFFFFD36B : inspected ? 0xFFB57ADC : 0xFF59687C,
                    inside(mouseX, mouseY, r), inspected || joined, true);
            g.centeredText(font, Component.literal(t.displayName()), r.x() + r.w()/2, r.y()+5, inspected ? 0xFFFFE4A7 : 0xFFE9E0F1);
        }
        Rect detail = l.traditionDetail();
        CodexVisualLanguage.panel(g, detail.x(), detail.y(), detail.w(), detail.h(), 0xFF8A65B1);
        String prefix = "faction_" + inspectedTradition.name().toLowerCase();
        g.text(font, Component.literal(fit(inspectedTradition.description(), detail.w()-122)), detail.x()+8, detail.y()+6, 0xFFD9E0EC);
        g.text(font, Component.literal(fit("강점 · " + inspectedTradition.strength(), detail.w()-122)), detail.x()+8, detail.y()+20, 0xFF7BD4A4);
        g.text(font, Component.literal(fit("약점 · " + inspectedTradition.weakness(), detail.w()-122)), detail.x()+8, detail.y()+34, 0xFFE08891);
        g.text(font, Component.literal(fit("우호 " + ArcaneClientState.text(prefix+"_friendly","없음") + " · 적대 " + ArcaneClientState.text(prefix+"_hostile","없음"), detail.w()-16)), detail.x()+8, detail.y()+49, 0xFFAFC2DB);
        g.text(font, Component.literal(fit("중립 " + ArcaneClientState.text(prefix+"_neutral","없음"), detail.w()-16)), detail.x()+8, detail.y()+63, 0xFF909EAF);
        String rep = ArcaneClientState.text(prefix+"_representative","없음");
        int repCircle = ArcaneClientState.integer(prefix+"_representative_circle",0);
        String champion = ArcaneClientState.text(prefix+"_champion","공석");
        int championCircle = ArcaneClientState.integer(prefix+"_champion_circle",0);
        g.text(font, Component.literal(fit("대표 " + rep + " " + repCircle + "C", 108)), detail.right()-112, detail.y()+8, 0xFFFFD891);
        g.text(font, Component.literal(fit("최강 " + champion + " " + championCircle + "C", 108)), detail.right()-112, detail.y()+23, 0xFFE9B7FF);
        g.text(font, Component.literal(fit(ArcaneClientState.text(prefix+"_headquarters",""),108)), detail.right()-112, detail.y()+38, 0xFF9BA8B9);
        button(g, l.traditionJoin(), current==inspectedTradition?"현재 소속":"소속 등록", inside(mouseX,mouseY,l.traditionJoin()), true);
        if (academyCircle == 0) {
            Rect circles=l.academyCircleViewport();
            g.enableScissor(circles.x(),circles.y(),circles.right(),circles.bottom());
            for(int circle=1;circle<=9;circle++)drawCircleCard(g,l.academyCircleCard(circle,scroll),circle,mouseX,mouseY,true);
            g.disableScissor();
            return;
        }
        Rect back=l.academyBack();button(g,back,"‹ 써클",inside(mouseX,mouseY,back),true);g.text(font,Component.literal(academyCircle+"C 상점"),back.right()+8,back.y()+5,0xFFF1E8FA);
        Rect viewport=l.academyViewport();g.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());List<AcademyOfferCatalog.Offer> offers=AcademyOfferCatalog.forCircle(academyCircle);
        for(int i=0;i<offers.size();i++){Rect r=l.offerCard(i,scroll);AcademyOfferCatalog.Offer offer=offers.get(i);long price=offer.basePrice();boolean enough=marks>=price;g.fill(r.x(),r.y(),r.right(),r.bottom(),inside(mouseX,mouseY,r)?0xFF25344B:0xFF111927);g.fill(r.x(),r.y(),r.x()+2,r.bottom(),enough?0xFF70C69D:0xFFB75B68);g.text(font,Component.literal(fit(offer.displayName(),r.w()-10)),r.x()+6,r.y()+6,enough?0xFFF0E8FA:0xFF988A91);g.text(font,Component.literal(price+" A"),r.x()+6,r.y()+21,enough?0xFFFFD66F:0xFFE0717C);}g.disableScissor();
    }

    private void quests(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c=l.content();
        int count=Math.min(3,ArcaneClientState.integer("quest_count",0));
        sectionTitle(g,l,"의뢰 게시판","고정 난이도·고정 보상");
        Rect viewport=l.questViewport();
        g.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
        String offered=ArcaneClientState.text("quest_offer_id","");
        Rect offer=l.questOffer(scroll);
        g.fill(offer.x(),offer.y(),offer.right(),offer.bottom(),0xFF111A2A);
        g.fill(offer.x(),offer.y(),offer.x()+3,offer.bottom(),offered.isBlank()?0xFF4E5563:0xFFFFC65D);
        if(offered.isBlank()){
            g.text(font,Component.literal("새 제안 없음"),offer.x()+9,offer.y()+8,0xFF8F98A8);
            g.text(font,Component.literal(fit("마도사 주민과 대화하면 난이도와 보상을 먼저 확인합니다.",offer.w()-18)),offer.x()+9,offer.y()+24,0xFF9EABC0);
        }else{
            String difficulty=ArcaneClientState.text("quest_offer_difficulty_name","견습");
            int target=ArcaneClientState.integer("quest_offer_target",0);
            long reward=ArcaneClientState.longInteger("quest_offer_reward",0);
            String desc=ArcaneClientState.text("quest_offer_desc","마도 의뢰");
            MagicTradition issuer=MagicTradition.parse(ArcaneClientState.text("quest_offer_affiliation","UNBOUND"));
            g.text(font,Component.literal(fit("["+difficulty+"] "+desc,offer.w()-190)),offer.x()+9,offer.y()+7,0xFFFFE0A0);
            g.text(font,Component.literal(fit("목표 "+target+" · "+reward+" A · "+issuer.displayName(),offer.w()-190)),offer.x()+9,offer.y()+24,0xFFFFC967);
            button(g,l.questAccept(scroll),"수락",inside(mouseX,mouseY,l.questAccept(scroll)),true);
            button(g,l.questReject(scroll),"거절",inside(mouseX,mouseY,l.questReject(scroll)),true);
        }
        int startY=l.questListY(scroll);
        if(count==0){
            Rect empty=new Rect(c.x(),startY,c.w(),34);
            g.fill(empty.x(),empty.y(),empty.right(),empty.bottom(),0xFF0F1724);
            g.fill(empty.x(),empty.y(),empty.x()+3,empty.bottom(),0xFF343945);
            g.text(font,Component.literal("진행 중인 의뢰 없음 · 최대 3개"),empty.x()+9,empty.y()+12,0xFF697483);
            g.disableScissor();
            return;
        }
        for(int i=0;i<count;i++){
            Rect card=l.questCard(i,scroll);
            int progress=ArcaneClientState.integer("quest_"+i+"_progress",0);
            int target=Math.max(1,ArcaneClientState.integer("quest_"+i+"_target",1));
            long reward=ArcaneClientState.longInteger("quest_"+i+"_reward",0);
            String diff=ArcaneClientState.text("quest_"+i+"_difficulty_name","견습");
            String desc=ArcaneClientState.text("quest_"+i+"_desc","마도 의뢰");
            boolean complete=progress>=target;
            g.fill(card.x(),card.y(),card.right(),card.bottom(),0xFF101827);
            g.fill(card.x(),card.y(),card.x()+3,card.bottom(),complete?0xFFFFC65D:0xFF7560A2);
            int textW=card.w()-105;
            g.text(font,Component.literal(fit((i+1)+". ["+diff+"] "+desc,textW)),card.x()+9,card.y()+6,complete?0xFFFFD36B:0xFFE9E0F1);
            g.text(font,Component.literal(fit(progress+"/"+target+" · "+reward+" A",textW)),card.x()+9,card.y()+21,complete?0xFFFFC65D:0xFF9FC6E8);
            g.fill(card.x()+9,card.y()+36,card.x()+9+Math.max(1,textW-8),card.y()+40,0xFF293244);
            int fill=(int)Math.round(Math.max(0,textW-8)*Math.min(1.0,progress/(double)target));
            g.fill(card.x()+9,card.y()+36,card.x()+9+fill,card.y()+40,complete?0xFFFFC65D:0xFF7569C2);
            if(complete)button(g,l.questClaim(i,scroll),"보상 수령",inside(mouseX,mouseY,l.questClaim(i,scroll)),true);
        }
        g.disableScissor();
    }

    private void core(GuiGraphicsExtractor g, Layout l) {
        sectionTitle(g,l,"마력핵","");
        Rect c=l.content();
        Rect viewport=l.coreViewport();
        g.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
        int circle=ArcaneClientState.integer("circle",1);
        int y=viewport.y()-scroll;
        int iconX=c.x()+28;
        int iconY=y+42;
        ArcaneRenderUtil.ring(g,iconX,iconY,18,0xFF9C6ED0);
        ArcaneRenderUtil.diamond(g,iconX,iconY,8,0xFFEAD9FF);
        g.centeredText(font,Component.literal(circle+"C"),iconX,iconY-4,0xFFFFFFFF);
        List<String> status=List.of(
                "MP "+ArcaneClientState.integer("mana",0)+"/"+ArcaneClientState.integer("max",100),
                "회복 "+String.format("%.1f",ArcaneClientState.regenPerSecond())+"/초",
                "통찰 "+ArcaneClientState.integer("insight",0),
                "아르카나 "+ArcaneClientState.longInteger("marks",0L));
        MagicTradition tradition=MagicTradition.parse(ArcaneClientState.text("tradition","UNBOUND"));
        List<String> gear=List.of(
                ArcaneClientState.text("staff","맨손"),
                ArcaneClientState.text("gear_hat","모자 없음"),
                ArcaneClientState.text("gear_robe","로브 없음"),
                ArcaneClientState.text("gear_boots","마도화 없음"),
                "소속 "+tradition.displayName(),
                "강점 "+tradition.strength(),
                "약점 "+tradition.weakness(),
                "위험지대 "+ArcaneClientState.text("zones","미탐지").replace("|"," · "));
        if(c.w()>=520){
            int firstX=c.x()+58;
            int firstW=Math.min(190,(c.w()-66)/2);
            infoPanel(g,firstX,y,firstW,"상태",status);
            int secondX=firstX+firstW+8;
            infoPanel(g,secondX,y,Math.max(120,c.right()-secondX),"장비 / 소속",gear);
        }else{
            infoPanel(g,c.x()+58,y,Math.max(120,c.w()-58),"상태",status);
            infoPanel(g,c.x(),y+100,c.w(),"장비 / 소속",gear);
        }
        g.disableScissor();
    }

    private void infoPanel(GuiGraphicsExtractor g, int x, int y, int w, String title, List<String> lines) {
        int panelHeight = Math.max(82, 34 + lines.size() * 13);
        CodexVisualLanguage.panel(g, x, y, w, panelHeight, 0xFF8D6BA1);
        g.text(font, Component.literal(title), x + 8, y + 8, 0xFFE8D9F5);
        for (int i = 0; i < lines.size(); i++) g.text(font, Component.literal(fit(lines.get(i), w - 16)), x + 8, y + 25 + i * 13, 0xFF9EABC0);
    }

    private void frame(GuiGraphicsExtractor g, Layout l) {
        CodexVisualLanguage.bookFrame(g, l.left(), l.top(), l.right(), l.bottom());
    }

    private void header(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        if(l.panelW()>=520)g.text(font, Component.literal("구중 마도서"), l.left() + 14, l.top() + 13, 0xFFF2E8FA);
        for (int i = 0; i < TABS.size(); i++) {
            Rect r = l.tab(i); boolean active = TABS.get(i).id().equals(page); boolean hover = inside(mouseX, mouseY, r);
            CodexVisualLanguage.bookmark(g, r.x(), r.y(), r.w(), r.h(), active, hover, active ? 0xFFFFD36B : 0xFF84649B);
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
        CodexVisualLanguage.action(g, r.x(), r.y(), r.w(), r.h(), hover, wide);
        g.centeredText(font, Component.literal(text), r.x() + r.w() / 2, r.y() + 5, hover ? 0xFFFFFFFF : 0xFFD8CFDF);
    }
    private void footer(GuiGraphicsExtractor g, Layout l, String text) {
        // v0.10 intentionally leaves the bottom edge empty: no tutorial sentence competes with content.
    }
    private void drawNotice(GuiGraphicsExtractor g, Layout l) {
        String serverNotice = ArcaneClientState.noticeText();
        String shown = !serverNotice.isBlank() ? serverNotice
                : (!notice.isBlank() && System.currentTimeMillis() <= noticeUntil ? notice : "");
        if (shown.isBlank()) return;
        int w = Math.min(l.panelW() - 30, Math.max(110, font.width(shown) + 20)); int x = l.cx() - w / 2;
        g.fill(x, l.top() + 48, x + w, l.top() + 67, 0xF0181324); g.fill(x, l.top() + 48, x + w, l.top() + 50, 0xFFFFD36B);
        g.centeredText(font, Component.literal(fit(shown, w - 12)), l.cx(), l.top() + 54, 0xFFFFE8B4);
    }
    private void notice(String text) { notice = text; noticeUntil = System.currentTimeMillis() + 1800L; }

    private void select(SpellDefinition spell) {
        int circle = ArcaneClientState.integer("circle", 1);
        if (activeSlot < 0) { notice("먼저 위의 1~5 주문 슬롯을 선택하세요"); return; }
        if (!ArcaneClientState.known().contains(spell.id())) { notice("아직 습득하지 않은 주문입니다"); return; }
        if (spell.circle() > circle) { notice(spell.circle() + "써클 마력핵이 필요합니다"); return; }
        ClientPacketDistributor.sendToServer(new EquipSpellPayload(spell.id(), activeSlot));
        notice((activeSlot + 1) + "번 슬롯 · " + spell.name());
    }
    private void request(String next) { saveScroll(); ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(next)); }
    private void saveScroll() { SAVED_SCROLL.put(scrollKey(), scroll); }
    private String scrollKey() { return page + ":" + ("atlas".equals(page) ? atlasCircle : "recipes".equals(page) ? fusionCircle : "academy".equals(page) ? academyCircle : 0); }

    private int maxScroll(Layout l) {
        return switch (page) {
            case "recipes" -> fusionCircle == 0 ? 0 : l.maxWideScroll(fusionFormulasInCircle(fusionCircle).size(), 54, 49);
            case "staffs" -> l.maxStaffScroll(ModItems.profiles().size());
            case "academy" -> academyCircle == 0 ? l.maxAcademyCircleScroll(9)
                    : l.maxOfferScroll(AcademyOfferCatalog.forCircle(academyCircle).size());
            case "quests" -> l.maxQuestScroll(Math.min(3, ArcaneClientState.integer("quest_count", 0)));
            case "atlas" -> atlasCircle == 0 ? 0 : l.maxSpellScroll(SpellCatalog.spellsInCircle(atlasCircle).size());
            case "core" -> l.maxCoreScroll();
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
    private static List<SpellCatalog.FusionFormula> fusionFormulasInCircle(int circle) {
        return SpellCatalog.fusions().stream().filter(formula -> SpellCatalog.spell(formula.result())
                .map(spell -> spell.circle() == circle).orElse(false)).toList();
    }
    private static MagicTradition[] traditions() { return new MagicTradition[]{MagicTradition.ARCANE, MagicTradition.DIVINE, MagicTradition.OCCULT, MagicTradition.PRIMAL}; }
    private static String normalize(String p) { return "recipes".equals(p)||"staffs".equals(p)||"academy".equals(p)||"quests".equals(p)||"core".equals(p) ? p : "atlas"; }
    private static boolean inside(double x, double y, Rect r) { return x >= r.x() && y >= r.y() && x < r.right() && y < r.bottom(); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static int circleColor(int c) { return switch(c){case 1->0xFF7DA7E8;case 2->0xFF78B7D8;case 3->0xFF75C6B0;case 4->0xFFE0B86C;case 5->0xFFE18A72;case 6->0xFFC783D9;case 7->0xFFA879E7;case 8->0xFF8A6BE0;default->0xFFE4C56A;}; }
    private static String circleSubtitle(int c) { return switch(c){case 1->"기초 회로";case 2->"전투 입문";case 3->"정규 마도";case 4->"상급 전술";case 5->"전장 지배";case 6->"대마법사";case 7->"초월 마법";case 8->"신화 마법";default->"세계급 의식";}; }

    private record Tab(String id, String label) {}
    private record Rect(int x, int y, int w, int h) { int right(){return x+w;} int bottom(){return y+h;} }
    private record Layout(int left, int top, int panelW, int panelH) {
        int right(){return left+panelW;} int bottom(){return top+panelH;} int cx(){return left+panelW/2;}
        Rect close(){return new Rect(right()-27,top+7,20,20);}
        Rect tab(int i){
            int start=panelW>=520?left+120:left+6;
            int end=right()-34;
            int w=Math.max(36,Math.min(70,(end-start)/TABS.size()));
            int total=w*TABS.size();
            int x=start+Math.max(0,(end-start-total)/2)+i*w;
            return new Rect(x,top+5,w,25);
        }
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
        Rect staffViewport(){Rect c=content();int bottom=selectedStaffId.isBlank()?c.bottom():c.bottom()-68;return new Rect(c.x(),c.y()+24,c.w(),Math.max(30,bottom-(c.y()+24)));}
        Rect staffCard(int i,int scroll){Rect v=staffViewport();int cols=v.w()>=520?3:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*44-scroll,w,39);}
        int maxStaffScroll(int count){Rect v=staffViewport();int cols=v.w()>=520?3:2;return Math.max(0,((count+cols-1)/cols)*44-v.h());}
        Rect staffRecipe(){Rect c=content();return new Rect(c.x(),c.bottom()-62,c.w(),60);}

        Rect tradition(int i){Rect c=content();int gap=4;int w=(c.w()-gap*3)/4;return new Rect(c.x()+i*(w+gap),c.y()+20,w,20);}
        Rect traditionDetail(){Rect c=content();return new Rect(c.x(),c.y()+44,c.w(),78);}
        Rect traditionJoin(){Rect d=traditionDetail();return new Rect(d.right()-108,d.bottom()-27,100,21);}
        Rect academyCircleViewport(){Rect c=content();Rect d=traditionDetail();int y=d.bottom()+6;return new Rect(c.x(),y,c.w(),Math.max(18,c.bottom()-y));}
        Rect academyCircleCard(int circle,int scroll){Rect v=academyCircleViewport();int cols=v.w()>=620?9:v.w()>=430?6:3,gap=4;int w=(v.w()-gap*(cols-1))/cols;int col=(circle-1)%cols,row=(circle-1)/cols;int h=34;return new Rect(v.x()+col*(w+gap),v.y()+row*(h+4)-scroll,w,h);}
        int maxAcademyCircleScroll(int count){Rect v=academyCircleViewport();int cols=v.w()>=620?9:v.w()>=430?6:3;int rows=(count+cols-1)/cols;return Math.max(0,rows*38-v.h());}
        Rect academyBack(){Rect c=content();Rect d=traditionDetail();return new Rect(c.x(),d.bottom()+6,66,19);}
        Rect academyViewport(){Rect c=content();Rect b=academyBack();int y=b.bottom()+6;return new Rect(c.x(),y,c.w(),Math.max(18,c.bottom()-y));}
        Rect offerCard(int i,int scroll){Rect v=academyViewport();int cols=v.w()>=540?4:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*43-scroll,w,38);}
        int maxOfferScroll(int count){Rect v=academyViewport();int cols=v.w()>=540?4:2;return Math.max(0,((count+cols-1)/cols)*43-v.h());}

        Rect questViewport(){Rect c=content();return new Rect(c.x(),c.y()+24,c.w(),Math.max(18,c.h()-24));}
        Rect questOffer(int scroll){Rect c=content();return new Rect(c.x(),c.y()+28-scroll,c.w(),48);}
        Rect questAccept(int scroll){Rect r=questOffer(scroll);return new Rect(r.right()-166,r.y()+13,78,21);}
        Rect questReject(int scroll){Rect r=questOffer(scroll);return new Rect(r.right()-82,r.y()+13,78,21);}
        int questListY(int scroll){return content().y()+84-scroll;}
        Rect questCard(int i,int scroll){Rect c=content();return new Rect(c.x(),questListY(scroll)+i*50,c.w(),44);}
        Rect questClaim(int i,int scroll){Rect r=questCard(i,scroll);return new Rect(r.right()-92,r.y()+10,82,23);}
        int maxQuestScroll(int count){Rect v=questViewport();return Math.max(0,84+Math.max(1,count)*50-v.h());}

        Rect coreViewport(){Rect c=content();return new Rect(c.x(),c.y()+24,c.w(),Math.max(18,c.h()-24));}
        int maxCoreScroll(){Rect c=content();Rect v=coreViewport();int contentHeight=c.w()>=520?150:242;return Math.max(0,contentHeight-v.h());}
    }
}

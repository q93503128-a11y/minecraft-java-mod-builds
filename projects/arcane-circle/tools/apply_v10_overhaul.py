#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"missing anchor in {path}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_method(path: Path, signature: str, replacement: str) -> None:
    text = path.read_text(encoding="utf-8")
    start = text.find(signature)
    if start < 0:
        if replacement.strip() in text:
            return
        raise RuntimeError(f"missing method in {path.name}: {signature}")
    brace = text.find("{", start)
    if brace < 0:
        raise RuntimeError(f"missing method body in {path.name}: {signature}")
    depth = 0
    end = -1
    for i in range(brace, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end < 0:
        raise RuntimeError(f"unclosed method in {path.name}: {signature}")
    path.write_text(text[:start] + replacement.rstrip() + text[end:], encoding="utf-8")


def replace_tail(path: Path, marker: str, replacement: str) -> None:
    text = path.read_text(encoding="utf-8")
    start = text.find(marker)
    if start < 0:
        if replacement.strip() in text:
            return
        raise RuntimeError(f"missing tail marker in {path}: {marker}")
    path.write_text(text[:start] + replacement, encoding="utf-8")


# Version and protocol
props = ROOT / "gradle.properties"
replace_once(props, "mod_version=0.9.0-alpha.1", "mod_version=0.10.0-alpha.1")
main = JAVA / "ArcaneCircle.java"
replace_once(main, 'VERSION = "0.9.0-alpha.1"', 'VERSION = "0.10.0-alpha.1"')
network = JAVA / "network/ArcaneNetwork.java"
replace_once(network, 'PROTOCOL_VERSION = "ninefold-arcana-9"', 'PROTOCOL_VERSION = "ninefold-arcana-10"')
index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
index["version"] = "0.10.0-alpha.1"
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Dense grimoire UI
screen = JAVA / "client/GrimoireScreen.java"
replace_method(screen, "    private void atlas(", r'''    private void atlas(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
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
    }''')

replace_method(screen, "    private void drawCircleCard(", r'''    private void drawCircleCard(GuiGraphicsExtractor g, Rect r, int circle, int mouseX, int mouseY, boolean shop) {
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
    }''')

replace_method(screen, "    private void drawLoadout(", r'''    private void drawLoadout(GuiGraphicsExtractor g, Rect r, int slot, int mouseX, int mouseY) {
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
    }''')

replace_method(screen, "    private void drawSpell(", r'''    private void drawSpell(GuiGraphicsExtractor g, Rect r, SpellDefinition spell, boolean known, int mouseX, int mouseY) {
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
    }''')

replace_method(screen, "    private void recipes(", r'''    private void recipes(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
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
    }''')

replace_method(screen, "    private void staffs(", r'''    private void staffs(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
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
    }''')

replace_method(screen, "    private void academy(", r'''    private void academy(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
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
    }''')

replace_method(screen, "    private void core(", r'''    private void core(GuiGraphicsExtractor g, Layout l) {
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
    }''')

replace_method(screen, "    private void sectionTitle(", r'''    private void sectionTitle(GuiGraphicsExtractor g, Layout l, String title, String subtitle) {
        Rect c = l.content();
        g.text(font, Component.literal(title), c.x() + 2, c.y() + 5, 0xFFF0E7F8);
    }''')
replace_method(screen, "    private void footer(", r'''    private void footer(GuiGraphicsExtractor g, Layout l, String text) {
        // v0.10 intentionally leaves the bottom edge empty: no tutorial sentence competes with content.
    }''')
replace_method(screen, "    private Layout layout(", r'''    private Layout layout() {
        int targetH = switch (page) {
            case "atlas" -> atlasCircle == 0 ? 150 : 238;
            case "academy" -> academyCircle == 0 ? 188 : 278;
            case "core" -> 210;
            default -> 300;
        };
        int panelW = Math.min(620, Math.max(300, width - 28));
        int panelH = Math.min(targetH, Math.max(180, height - 24));
        panelW = Math.min(panelW, Math.max(1, width - 10));
        panelH = Math.min(panelH, Math.max(1, height - 10));
        return new Layout((width - panelW) / 2, (height - panelH) / 2, panelW, panelH);
    }''')

replace_tail(screen, "    private record Layout", r'''    private record Layout(int left, int top, int panelW, int panelH) {
        int right(){return left+panelW;} int bottom(){return top+panelH;} int cx(){return left+panelW/2;}
        Rect close(){return new Rect(right()-27,top+7,20,20);}
        Rect tab(int i){int w=58;return new Rect(cx()-TABS.size()*w/2+i*w,top+5,w,25);}
        Rect content(){return new Rect(left+12,top+36,panelW-24,panelH-44);}
        Rect back(){Rect c=content();return new Rect(c.x(),c.y(),66,19);}

        Rect circleCard(int circle){
            Rect c=content(); int cols=c.w()>=520?9:3; int gap=4; int col=(circle-1)%cols,row=(circle-1)/cols;
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
            Rect c=content();int cols=c.w()>=520?9:3,gap=4;int w=(c.w()-gap*(cols-1))/cols;int col=(circle-1)%cols,row=(circle-1)/cols;int h=34;
            return new Rect(c.x()+col*(w+gap),c.y()+48+row*(h+gap),w,h);
        }
        Rect academyBack(){Rect c=content();return new Rect(c.x(),c.y()+45,66,19);}
        Rect academyViewport(){Rect c=content();return new Rect(c.x(),c.y()+70,c.w(),c.h()-71);}
        Rect offerCard(int i,int scroll){Rect v=academyViewport();int cols=v.w()>=540?4:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*43-scroll,w,38);}
        int maxOfferScroll(int count){Rect v=academyViewport();int cols=v.w()>=540?4:2;return Math.max(0,((count+cols-1)/cols)*43-v.h());}
    }
}
''')

# Compact rectangular HUD
hud = JAVA / "client/ArcaneHud.java"
replace_method(hud, "    private static void renderWorldHud(", r'''    private static void renderWorldHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || !ArcaneClientState.ready()) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;

        int gap = 3;
        int slotW = width >= 520 ? 82 : width >= 390 ? 68 : Math.max(48, (width - 20 - gap * 4) / 5);
        int slotH = 28;
        int total = slotW * 5 + gap * 4;
        int startX = Math.max(4, (width - total) / 2);
        int y = Math.max(8, height - slotH - 58);

        if (width >= 500) drawManaSide(g, font, startX, y, slotH);
        else drawManaTop(g, font, width, y - 12);
        for (int slot = 0; slot < 5; slot++) {
            drawSlot(g, font, startX + slot * (slotW + gap), y, slotW, slotH, slot);
        }
        drawFusionQueue(g, font, width, y - 22);
    }''')
replace_method(hud, "    private static void drawManaSide(", r'''    private static void drawManaSide(GuiGraphicsExtractor g, Font font, int startX, int y, int slotHeight) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int barWidth = Math.min(94, Math.max(58, startX - 12));
        int x = Math.max(5, startX - barWidth - 7);
        int fill = (int) Math.round((barWidth - 2) * Math.min(1.0, mana / (double) max));
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C  " + mana + "/" + max), x, y + 2, 0xFFE7DDF7);
        g.fill(x, y + 16, x + barWidth, y + 22, 0xDC050912);
        g.fill(x + 1, y + 17, x + 1 + fill, y + 21, 0xEF5E8EEB);
    }''')
replace_method(hud, "    private static void drawManaTop(", r'''    private static void drawManaTop(GuiGraphicsExtractor g, Font font, int width, int y) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int barWidth = Math.min(150, Math.max(90, width - 80));
        int x = (width - barWidth) / 2;
        int fill = (int) Math.round((barWidth - 2) * Math.min(1.0, mana / (double) max));
        g.fill(x, y, x + barWidth, y + 6, 0xDC050912);
        g.fill(x + 1, y + 1, x + 1 + fill, y + 5, 0xEF5E8EEB);
        g.centeredText(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C " + mana + "/" + max), width / 2, y - 10, 0xFFE7DDF7);
    }''')
replace_method(hud, "    private static void drawSlot(", r'''    private static void drawSlot(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int slot) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        int color = spell == null ? 0xFF606475 : ArcaneRenderUtil.schoolColor(spell.school());
        int dark = spell == null ? 0xFF171A22 : ArcaneRenderUtil.schoolDark(spell.school());
        int remaining = ArcaneClientState.cooldownRemainingTicks(slot);
        boolean charging = ArcaneClientState.isChargingSlot(slot);

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, charging ? 0xFFFFD36B : 0xD9040610);
        g.fill(x, y, x + w, y + h, remaining > 0 ? dark : 0xEB101827);
        g.fill(x, y + h - 2, x + w, y + h, color);
        g.text(font, Component.literal(Integer.toString(slot + 1)), x + 3, y + 3, 0xFFB8C2D4);

        if (spell == null) {
            g.text(font, Component.literal("-"), x + 16, y + 9, 0xFF666B78);
            return;
        }

        ArcaneRenderUtil.ring(g, x + 17, y + 14, 7, remaining > 0 ? 0xFF706D78 : color);
        ArcaneRenderUtil.spellRune(g, x + 17, y + 14, spell, 4, remaining > 0 ? 0xFF827B89 : 0xFFF8F2FF);
        g.text(font, Component.literal(fitName(font, spell.name(), w - 29)), x + 28, y + 4,
                remaining > 0 ? 0xFF8B8492 : charging ? 0xFFFFE0A2 : 0xFFE6DFED);
        String meta = remaining > 0 ? String.format("%.1fs", remaining / 20.0) : "MP " + spell.manaCost();
        g.text(font, Component.literal(meta), x + 28, y + 15, remaining > 0 ? 0xFFF18A8A : 0xFF91A4BF);

        if (remaining > 0) {
            int fill = (int) Math.round((w - 2) * ArcaneClientState.cooldownFraction(slot));
            g.fill(x + 1, y + h - 3, x + 1 + fill, y + h - 1, 0xFFE46D78);
        } else if (charging) {
            int progress = (int) Math.round((w - 2) * ArcaneClientState.chargingFraction());
            g.fill(x + 1, y + h - 3, x + 1 + progress, y + h - 1, 0xFFFFD36B);
        }
    }''')

# Single-pass, compact, non-falling magic circle
sigil = JAVA / "magic/SpellSigilService.java"
sigil.write_text(r'''package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Compact single-pass seal. Structural particles never use gravity-driven school particles. */
public final class SpellSigilService {
    public static final int CHARGE_STAGES = 5;
    private static final ParticleOptions INK = ParticleTypes.END_ROD;
    private SpellSigilService() {}

    public static void renderChargeStep(ServerPlayer player, SpellDefinition spell, double effectiveRange, int step) {
        Seal seal = seal(player, spell, effectiveRange, false);
        drawStep((ServerLevel) player.level(), seal, spell, Math.max(0, Math.min(CHARGE_STAGES - 1, step)));
    }

    public static void renderRelease(ServerPlayer player, SpellDefinition spell, double effectiveRange) {
        ServerLevel level = (ServerLevel) player.level();
        Seal seal = seal(player, spell, effectiveRange, true);
        ring(level, seal, seal.radius() * 1.04, INK, 14);
        nodeMarks(level, seal, spell, INK);
    }

    private static Seal seal(ServerPlayer player, SpellDefinition spell, double range, boolean release) {
        double ratio = spell.range() <= 0.0 ? 1.0 : Math.max(0.85, Math.min(1.45, range / spell.range()));
        double radius = Math.min(0.78, (0.30 + spell.circle() * 0.043) * Math.sqrt(ratio));
        if (release) radius *= 1.03;
        Anchor anchor = anchor(player, spell, range);
        return new Seal(anchor.center(), anchor.right(), anchor.up(), radius);
    }

    private static Anchor anchor(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 upReference = Math.abs(look.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(upReference).normalize();
        Vec3 up = right.cross(look).normalize();
        return switch (spell.sigilAnchor()) {
            case FRONT -> new Anchor(player.getEyePosition().add(look.scale(1.28)).add(up.scale(-0.46)), right, up);
            case FEET, GROUND_SELF -> horizontal(player.position().add(0, 0.08, 0));
            case BODY -> horizontal(player.position().add(0, 0.12, 0));
            case GROUND_TARGET -> horizontal(aimGround(player, Math.max(5.0, range)).add(0, 0.06, 0));
            case TARGET -> horizontal(target(player, Math.max(7.0, range)).map(Mob::position)
                    .orElse(player.getEyePosition().add(look.scale(1.8))).add(0, 0.06, 0));
        };
    }

    private static Anchor horizontal(Vec3 center) {
        return new Anchor(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1));
    }

    private static void drawStep(ServerLevel level, Seal seal, SpellDefinition spell, int step) {
        int signature = Math.floorMod(spell.id().hashCode(), 4093);
        double rotation = (signature % 24) * Math.PI / 12.0;
        switch (step) {
            case 0 -> ring(level, seal, seal.radius(), INK, 20);
            case 1 -> {
                ring(level, seal, seal.radius() * 0.82, INK, 16);
                radialCompartments(level, seal, spell.circle() >= 6 ? 8 : 6, INK, rotation);
            }
            case 2 -> centralSeal(level, seal, spell, INK, rotation);
            case 3 -> signatureLines(level, seal, signature, INK, rotation);
            case 4 -> runeTicks(level, seal, spell, INK);
            default -> { }
        }
    }

    private static void centralSeal(ServerLevel level, Seal s, SpellDefinition spell, ParticleOptions p, double rotation) {
        SpellWorldLore.SigilFamily family = SpellWorldLore.sigilFamily(spell.id());
        switch (family) {
            case LANCE -> {
                polygon(level, s, s.radius() * 0.55, 3, rotation, p, 4);
                line(level, point(s, 0, -s.radius() * 0.55), point(s, 0, s.radius() * 0.55), p, 8);
            }
            case STAR, STORM, CROWN -> star(level, s, s.radius() * 0.58, s.radius() * 0.26,
                    family == SpellWorldLore.SigilFamily.CROWN ? 9 : family == SpellWorldLore.SigilFamily.STORM ? 8 : 6,
                    rotation, p, 4);
            case HEX, SEAL -> polygon(level, s, s.radius() * 0.56,
                    family == SpellWorldLore.SigilFamily.HEX ? 6 : 5, rotation, p, 4);
            case PORTAL -> {
                polygon(level, s, s.radius() * 0.56, 4, rotation, p, 4);
                polygon(level, s, s.radius() * 0.30, 4, rotation + Math.PI / 4.0, p, 3);
            }
            case EYE -> {
                arc(level, s, s.radius() * 0.56, 0, Math.PI, rotation, p, 10);
                arc(level, s, s.radius() * 0.56, Math.PI, Math.PI * 2, rotation, p, 10);
                ring(level, s, s.radius() * 0.18, p, 10);
            }
            case CLOCK -> {
                ring(level, s, s.radius() * 0.52, p, 14);
                line(level, s.center(), point(s, Math.cos(rotation) * s.radius() * 0.42,
                        Math.sin(rotation) * s.radius() * 0.42), p, 6);
            }
            case SPIRAL -> spiral(level, s, s.radius() * 0.56, rotation, p, 16);
        }
    }

    private static void radialCompartments(ServerLevel level, Seal s, int divisions, ParticleOptions p, double rotation) {
        for (int i = 0; i < divisions; i++) {
            double angle = rotation + Math.PI * 2.0 * i / divisions;
            line(level, polar(s, s.radius() * 0.70, angle), polar(s, s.radius() * 0.96, angle), p, 3);
        }
    }

    private static void nodeMarks(ServerLevel level, Seal s, SpellDefinition spell, ParticleOptions p) {
        int nodes = Math.min(10, 4 + spell.circle());
        double offset = Math.floorMod(spell.id().hashCode(), 16) * Math.PI / 8.0;
        for (int i = 0; i < nodes; i++) {
            Vec3 point = polar(s, s.radius() * 0.76, offset + Math.PI * 2.0 * i / nodes);
            level.sendParticles(p, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static void signatureLines(ServerLevel level, Seal s, int signature, ParticleOptions p, double rotation) {
        int spokes = 3 + signature % 4;
        for (int i = 0; i < spokes; i++) {
            double a = rotation + Math.PI * 2.0 * i / spokes;
            double b = a + Math.PI * (2 + signature % 3) / spokes;
            line(level, polar(s, s.radius() * 0.17, a), polar(s, s.radius() * 0.48, b), p, 4);
        }
    }

    private static void runeTicks(ServerLevel level, Seal s, SpellDefinition spell, ParticleOptions p) {
        int ticks = Math.min(14, 7 + spell.circle());
        double offset = Math.floorMod(spell.id().hashCode(), 36) * Math.PI / 18.0;
        for (int i = 0; i < ticks; i++) {
            double a = offset + Math.PI * 2.0 * i / ticks;
            double length = (i + spell.circle()) % 3 == 0 ? 0.08 : 0.045;
            line(level, polar(s, s.radius() * (1.0 - length), a), polar(s, s.radius(), a), p, 2);
        }
    }

    private static void ring(ServerLevel level, Seal s, double radius, ParticleOptions p, int points) {
        for (int i = 0; i < points; i++) {
            Vec3 v = polar(s, radius, Math.PI * 2.0 * i / points);
            level.sendParticles(p, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    private static void polygon(ServerLevel level, Seal s, double radius, int sides, double rotation,
                                ParticleOptions p, int edgePoints) {
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) vertices.add(polar(s, radius, rotation - Math.PI / 2.0 + Math.PI * 2.0 * i / sides));
        for (int i = 0; i < sides; i++) line(level, vertices.get(i), vertices.get((i + 1) % sides), p, edgePoints);
    }

    private static void star(ServerLevel level, Seal s, double outer, double inner, int points, double rotation,
                             ParticleOptions p, int edgePoints) {
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < points * 2; i++) {
            double radius = i % 2 == 0 ? outer : inner;
            vertices.add(polar(s, radius, rotation - Math.PI / 2.0 + Math.PI * i / points));
        }
        for (int i = 0; i < vertices.size(); i++) line(level, vertices.get(i), vertices.get((i + 1) % vertices.size()), p, edgePoints);
    }

    private static void spiral(ServerLevel level, Seal s, double radius, double rotation, ParticleOptions p, int points) {
        Vec3 previous = s.center();
        for (int i = 1; i <= points; i++) {
            double t = i / (double) points;
            Vec3 next = polar(s, radius * t, rotation + t * Math.PI * 4.0);
            line(level, previous, next, p, 2);
            previous = next;
        }
    }

    private static void arc(ServerLevel level, Seal s, double radius, double start, double end,
                            double rotation, ParticleOptions p, int points) {
        for (int i = 0; i <= points; i++) {
            Vec3 v = polar(s, radius, rotation + start + (end - start) * i / points);
            level.sendParticles(p, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    private static void line(ServerLevel level, Vec3 a, Vec3 b, ParticleOptions p, int points) {
        int safe = Math.max(2, points);
        for (int i = 0; i <= safe; i++) {
            Vec3 v = a.lerp(b, i / (double) safe);
            level.sendParticles(p, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    private static Vec3 polar(Seal s, double radius, double angle) {
        return s.center().add(s.right().scale(Math.cos(angle) * radius)).add(s.up().scale(Math.sin(angle) * radius));
    }
    private static Vec3 point(Seal s, double x, double y) { return s.center().add(s.right().scale(x)).add(s.up().scale(y)); }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        Vec3 end = player.getEyePosition().add(player.getLookAngle().normalize().scale(range));
        return new Vec3(end.x, Math.max(player.level().getMinY() + 1, end.y), end.z);
    }

    private static Optional<Mob> target(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(Mob.class, new AABB(eye, eye.add(look.scale(range))).inflate(2.0),
                mob -> mob.isAlive() && (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !tame.isOwnedBy(player))).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.2, mob.getBbWidth() + 0.8);
                }).min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
    }

    private record Anchor(Vec3 center, Vec3 right, Vec3 up) {}
    private record Seal(Vec3 center, Vec3 right, Vec3 up, double radius) {}
}
''', encoding="utf-8")

# Automatic cast on completion: no ready-loop and no repeated circle generation.
casting = JAVA / "magic/SpellCastingService.java"
replace_method(casting, "    public static void tickCharge(", r'''    public static void tickCharge(ServerPlayer player) {
        ChargeState charge = CHARGES.get(player.getUUID());
        if (charge == null) return;
        long now = serverClock(player);
        long elapsed = now - charge.startedAt;
        if (!player.isAlive() || player.isSpectator() || elapsed > CHARGE_TIMEOUT_TICKS) {
            CHARGES.remove(player.getUUID());
            return;
        }
        SpellDefinition spell = SpellCatalog.spell(charge.spellId).orElse(null);
        if (spell == null || !data(player).state(player).known().contains(spell.id())) {
            CHARGES.remove(player.getUUID());
            return;
        }
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareSlot(player, charge.slot);
        if (!cast.accepted() || !charge.spellId.equals(cast.spell().id())) {
            CHARGES.remove(player.getUUID());
            return;
        }

        int stage = Math.min(SpellSigilService.CHARGE_STAGES - 1,
                (int) (elapsed * SpellSigilService.CHARGE_STAGES / Math.max(1, charge.requiredTicks)));
        if (stage > charge.lastStage) {
            for (int next = charge.lastStage + 1; next <= stage; next++) {
                SpellSigilService.renderChargeStep(player, spell, cast.range(), next);
            }
            charge.lastStage = stage;
        }

        if (elapsed >= charge.requiredTicks) {
            CHARGES.remove(player.getUUID());
            castPrepared(player, data, cast);
        }
    }''')
replace_method(casting, "    public static int requiredCastTicks(", r'''    public static int requiredCastTicks(ServerPlayer player, SpellDefinition spell) {
        MagicPlayerData.MageState state = data(player).state(player);
        int base = 4 + spell.circle() * 4;
        int circleGap = Math.max(0, state.circle() - spell.circle());
        int circleGapReduction = circleGap * 4;
        int masteryReduction = SpellCatalog.masteryTier(state.mastery(spell.id())) * 2;
        return Math.max(0, base - circleGapReduction - masteryReduction);
    }''')
# Remove obsolete ready pulse call/token if an earlier transformed source still contains it.
text = casting.read_text(encoding="utf-8")
text = text.replace("        if (elapsed >= charge.requiredTicks && now - charge.lastReadyPulse >= 16L) {\n            SpellSigilService.renderReadyPulse(player, spell, cast.range());\n            charge.lastReadyPulse = now;\n        }\n", "")
casting.write_text(text, encoding="utf-8")

# Stop generating and teleporting to the placeholder academy.
world = JAVA / "world/MagicWorldService.java"
world.write_text(r'''package kr.moonseungjun.arcanecircle.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameType;

/** Magic-world rules without the deprecated generated test academy. */
public final class MagicWorldService {
    private MagicWorldService() {}

    public static void onLogin(ServerPlayer player, boolean firstAwakening) {
        ArcaneWorldData data = ArcaneWorldData.get(((ServerLevel) player.level()).getServer());
        if (data.claimFirstArrival(player)) {
            if (!player.isCreative() && !player.isSpectator()) player.setGameMode(GameType.SURVIVAL);
            data.addMarks(player, firstAwakening ? 120L : 40L);
            player.sendSystemMessage(Component.literal("§5[마력핵 각성] §f주문과 마도서를 사용할 수 있습니다."));
        }
    }

    public static void onRespawn(ServerPlayer player) {
        if (!player.isCreative() && !player.isSpectator()) player.setGameMode(GameType.SURVIVAL);
    }

    public static void tick(ServerPlayer player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        if (player.tickCount % 80 == 0) awakenNearbyEnemies(player);
    }

    public static BlockPos academy(ServerPlayer player) {
        return player.blockPosition();
    }

    public static void teleportToAcademy(ServerPlayer player) {
        player.sendOverlayMessage(Component.literal("§7물리 학원 귀환은 비활성화되어 있습니다."));
    }

    private static void awakenNearbyEnemies(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(42.0),
                value -> value instanceof Enemy && value.isAlive())) {
            if (mob.getCustomName() != null) continue;
            int tier = Math.max(1, Math.min(5, 1 + (int) (level.getGameTime() / 24000L / 3L)));
            if (level.getRandom().nextInt(100) >= 8 + tier * 2) continue;
            mob.setCustomName(Component.literal("§5마력 변이체 " + tier + "환"));
            mob.setCustomNameVisible(false);
            mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, tier));
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, Math.max(0, tier - 1)));
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, Math.max(0, tier / 2 - 1)));
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, Math.max(0, tier / 2)));
            mob.setHealth(mob.getMaxHealth());
        }
    }
}
''', encoding="utf-8")

# Bring the broad contract forward to v0.10 and remove requirements for the deleted placeholder academy/ready loop.
contract = ROOT / "tools/test_magic_contract.py"
text = contract.read_text(encoding="utf-8")
text = text.replace("mod_version=0.9.0-alpha.1", "mod_version=0.10.0-alpha.1")
text = text.replace('VERSION = "0.9.0-alpha.1"', 'VERSION = "0.10.0-alpha.1"')
text = text.replace('["0.9.0-alpha.1", "apply_v09_usability.py", "staged casting"]',
                    '["0.10.0-alpha.1", "apply_v10_overhaul.py", "single-pass casting"]')
text = text.replace('"renderChargeStep", "renderReadyPulse", "renderRelease", "CHARGE_STAGES",',
                    '"renderChargeStep", "renderRelease", "CHARGE_STAGES",')
text = text.replace('"SpellSigilService.renderChargeStep", "SpellSigilService.renderReadyPulse",\n    "SpellSigilService.renderRelease",',
                    '"SpellSigilService.renderChargeStep", "SpellSigilService.renderRelease",')
text = text.replace('need(academy, ["61x61", "central rotunda", "Four faculty halls", "return origin"], "academy generation")\n', '')
text = text.replace('need(world, [\n    "ArcaneAcademyBuilder.build(level, player.blockPosition())", "setFoodLevel(20)",\n    "GameType.SURVIVAL", "teleportToAcademy", "level.getGameTime()"\n], "Minecraft 26.2 builder magic-world shell")',
                    'need(world, ["setFoodLevel(20)", "GameType.SURVIVAL", "teleportToAcademy", "level.getGameTime()"], "Minecraft 26.2 magic-world shell")\nif "ArcaneAcademyBuilder.build" in world:\n    raise SystemExit("placeholder academy generation remains")')
text = text.replace('if index.get("version") != "0.9.0-alpha.1"', 'if index.get("version") != "0.10.0-alpha.1"')
text = text.replace('raise SystemExit("v0.9 spell catalogue index mismatch")', 'raise SystemExit("v0.10 spell catalogue index mismatch")')
text = text.replace('print("Arcane Circle v0.9 fixed grimoire and staged casting contract: PASS")',
                    'print("Arcane Circle v0.10 dense UI and single-pass casting contract: PASS")')
contract.write_text(text, encoding="utf-8")

print("Arcane Circle v0.10 dense UI, compact HUD, single-pass casting and placeholder-world removal: PASS")

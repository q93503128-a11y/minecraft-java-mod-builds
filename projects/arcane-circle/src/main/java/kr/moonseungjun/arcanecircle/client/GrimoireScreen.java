package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.EquipSpellPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
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
            new Tab("atlas", "주문 성좌"), new Tab("mastery", "융합 각인"), new Tab("core", "마력핵"));
    private final String page;
    private int activeSocket;

    public GrimoireScreen(String page) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("구중 마도서"));
        this.page = normalize(page);
    }

    @Override
    protected void init() {
        super.init();
        Layout l = layout();
        for (int i = 0; i < TABS.size(); i++) {
            int index = i;
            invisible(l.tab(i), () -> request(TABS.get(index).id()));
        }
        invisible(l.close(), this::onClose);
        if ("atlas".equals(page)) {
            invisible(l.focusHit(), () -> activeSocket = 0);
            invisible(l.weaveHit(), () -> activeSocket = 1);
            List<SpellDefinition> spells = new ArrayList<>(SpellCatalog.spells().values());
            for (int i = 0; i < spells.size(); i++) {
                SpellDefinition spell = spells.get(i);
                invisible(l.nodeHit(i), () -> select(spell));
            }
        }
    }

    private void invisible(Rect rect, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> action.run())
                .bounds(rect.x(), rect.y(), rect.w(), rect.h()).build());
        button.setAlpha(0.0F);
    }

    private void request(String next) {
        ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(next));
    }

    private void select(SpellDefinition spell) {
        int circle = ArcaneClientState.integer("circle", 1);
        if (!ArcaneClientState.known().contains(spell.id()) || spell.circle() > circle) return;
        ClientPacketDistributor.sendToServer(new EquipSpellPayload(spell.id(), activeSocket));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        long time = System.currentTimeMillis();
        g.fill(0, 0, width, height, 0xE8050610);
        stars(g, time);
        frame(g, l);
        header(g, l, mouseX, mouseY);
        mana(g, l);
        switch (page) {
            case "mastery" -> mastery(g, l, mouseX, mouseY, time);
            case "core" -> core(g, l, time);
            default -> atlas(g, l, mouseX, mouseY, time);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void stars(GuiGraphicsExtractor g, long time) {
        for (int i = 0; i < 64; i++) {
            int x = Math.floorMod(i * 97 + 31, Math.max(1, width));
            int y = Math.floorMod(i * 53 + 17, Math.max(1, height));
            int phase = (int) ((time / 170 + i * 7) % 14);
            int color = phase < 3 ? 0x99D8C8FF : phase < 8 ? 0x554D70B7 : 0x33433B5C;
            g.fill(x, y, x + (phase == 0 ? 2 : 1), y + 1, color);
        }
    }

    private void frame(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.left() - 5, l.top() - 5, l.right() + 5, l.bottom() + 5, 0xFF050611);
        g.fill(l.left() - 2, l.top() - 2, l.right() + 2, l.bottom() + 2, 0xFF8060B3);
        g.fill(l.left(), l.top(), l.right(), l.bottom(), 0xF20B1120);
        g.fill(l.left() + 3, l.top() + 3, l.right() - 3, l.bottom() - 3, 0xF2121A30);
        line(g, l.left() + 12, l.top() + 11, l.right() - 12, l.top() + 11, 0xFFCAA6F1);
        line(g, l.left() + 12, l.bottom() - 12, l.right() - 12, l.bottom() - 12, 0xFF544368);
        corner(g, l.left() + 12, l.top() + 12, 1, 1);
        corner(g, l.right() - 13, l.top() + 12, -1, 1);
        corner(g, l.left() + 12, l.bottom() - 13, 1, -1);
        corner(g, l.right() - 13, l.bottom() - 13, -1, -1);
    }

    private void header(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        g.centeredText(font, Component.literal("NINEFOLD ARCANA"), l.cx(), l.top() + 16, 0xFFF1DEFF);
        g.centeredText(font, Component.literal("천구의 마도서 · 실전 융합 회로"), l.cx(), l.top() + 29, 0xFF8F7AA9);
        for (int i = 0; i < TABS.size(); i++) {
            Rect tab = l.tab(i);
            boolean active = TABS.get(i).id().equals(page);
            boolean hover = inside(mouseX, mouseY, tab);
            g.centeredText(font, Component.literal(TABS.get(i).label()), tab.x() + tab.w() / 2, tab.y() + 5,
                    active ? 0xFFE7C9FF : hover ? 0xFFC7A9E8 : 0xFF746982);
            if (active) {
                line(g, tab.x() + 14, tab.bottom() - 2, tab.right() - 14, tab.bottom() - 2, 0xFFB16EEE);
                diamond(g, tab.x() + tab.w() / 2, tab.bottom() - 2, 3, 0xFFF0D8FF);
            }
        }
        Rect close = l.close();
        diamond(g, close.x() + 10, close.y() + 10, 8,
                inside(mouseX, mouseY, close) ? 0xFFD86382 : 0xFF67384F);
        g.centeredText(font, Component.literal("×"), close.x() + 10, close.y() + 4, 0xFFFFFFFF);
    }

    private void mana(GuiGraphicsExtractor g, Layout l) {
        int circle = ArcaneClientState.integer("circle", 1);
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int insight = ArcaneClientState.integer("insight", 0);
        int next = ArcaneClientState.integer("next", 8);
        int x = l.left() + 18;
        int y = l.top() + 18;
        g.text(font, Component.literal(circle + " CIRCLE"), x, y, 0xFFE2CCFA);
        g.fill(x, y + 14, x + 118, y + 20, 0xFF1B243B);
        int fill = (int) Math.round(118 * Math.min(1.0, mana / (double) max));
        g.fill(x, y + 14, x + fill, y + 20, 0xFF547FE3);
        g.text(font, Component.literal("MANA " + mana + "/" + max), x, y + 23, 0xFFA8C5F7);
        g.text(font, Component.literal(circle >= 3 ? "INSIGHT COMPLETE" : "INSIGHT " + insight + "/" + next),
                l.right() - 132, y + 1, 0xFFB8A3D1);
    }

    private void atlas(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY, long time) {
        Rect c = l.content();
        sockets(g, l, mouseX, mouseY, time);
        List<SpellDefinition> spells = new ArrayList<>(SpellCatalog.spells().values());
        Set<String> known = ArcaneClientState.known();
        int circle = ArcaneClientState.integer("circle", 1);
        SpellDefinition hovered = null;

        for (int row = 0; row < 3; row++) {
            Point first = l.node(row * 5);
            Point last = l.node(row * 5 + 4);
            line(g, first.x(), first.y(), last.x(), last.y(), row < circle ? 0x776E89BC : 0x33343A4A);
            g.text(font, Component.literal((row + 1) + "C"), c.x() + 7, first.y() - 4,
                    row < circle ? 0xFFD6B9F4 : 0xFF5D5967);
        }

        for (int i = 0; i < spells.size(); i++) {
            SpellDefinition spell = spells.get(i);
            Point p = l.node(i);
            boolean learned = known.contains(spell.id());
            boolean usable = learned && spell.circle() <= circle;
            boolean focus = spell.id().equals(ArcaneClientState.focus());
            boolean weave = spell.id().equals(ArcaneClientState.weave());
            boolean hover = inside(mouseX, mouseY, l.nodeHit(i));
            spellNode(g, p, spell, learned, usable, focus, weave, hover, time + i * 83L);
            if (hover) hovered = spell;
        }

        if (hovered != null) {
            int x = c.right() - 246;
            g.text(font, Component.literal(hovered.circle() + "C · " + hovered.school().displayName()
                    + " · 마력 " + hovered.manaCost()), x, c.y() + 3, 0xFFD9C6F0);
            g.text(font, Component.literal(shorten(hovered.description(), 38)), x, c.y() + 15, 0xFFAAB5CC);
        }
        g.text(font, Component.literal("회로 선택 → 주문 문양 선택 · R 단독 시전 · G 두 회로 즉석 융합"),
                c.x() + 10, c.bottom() - 13, 0xFF98A6C2);
    }

    private void sockets(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY, long time) {
        Point focus = l.focus();
        Point weave = l.weave();
        Point center = new Point(l.cx(), focus.y());
        line(g, focus.x() + 25, focus.y(), center.x() - 18, center.y(), 0xFF9A62DC);
        line(g, center.x() + 18, center.y(), weave.x() - 25, weave.y(), 0xFF55B6D8);
        socket(g, focus, ArcaneClientState.focus(), "주력 회로", activeSocket == 0,
                inside(mouseX, mouseY, l.focusHit()), time);
        socket(g, weave, ArcaneClientState.weave(), "직조 회로", activeSocket == 1,
                inside(mouseX, mouseY, l.weaveHit()), time + 500);

        SpellDefinition fusion = SpellCatalog.spell(ArcaneClientState.fusion()).orElse(null);
        int color = fusion == null ? 0xFF424657 : schoolColor(fusion.school());
        fillCircle(g, center.x(), center.y(), 19, 0xFF070913);
        ring(g, center.x(), center.y(), 19, color);
        diamond(g, center.x(), center.y(), 8, color);
        if (fusion == null) {
            g.centeredText(font, Component.literal("불안정"), center.x(), center.y() + 24, 0xFF6C6875);
        } else {
            boolean registered = ArcaneClientState.known().contains(fusion.id());
            int mastery = ArcaneClientState.mastery(fusion.id());
            int required = SpellCatalog.masteryRequired(fusion.id());
            g.centeredText(font, Component.literal(fusion.name()), center.x(), center.y() + 24,
                    registered ? 0xFFFFD98A : 0xFFE8DCFA);
            g.centeredText(font, Component.literal(registered ? "단독 시전 각인" : "실전 숙련 " + mastery + "/" + required),
                    center.x(), center.y() + 36, registered ? 0xFFE5B96A : 0xFF9689A8);
        }
    }

    private void socket(GuiGraphicsExtractor g, Point p, String id, String label, boolean active,
                        boolean hover, long time) {
        SpellDefinition spell = SpellCatalog.spell(id).orElse(null);
        int color = spell == null ? 0xFF555B70 : schoolColor(spell.school());
        fillCircle(g, p.x(), p.y(), 25, 0xFF070913);
        ring(g, p.x(), p.y(), 25, active ? 0xFFFFD98A : hover ? 0xFFE0C3FA : color);
        ring(g, p.x(), p.y(), 18, color);
        if (spell != null) rune(g, p.x(), p.y(), spell.school(), 9, 0xFFF8F3FF);
        int ox = p.x() + (int) Math.round(Math.cos(time / 420.0) * 29.0);
        int oy = p.y() + (int) Math.round(Math.sin(time / 420.0) * 29.0);
        fillCircle(g, ox, oy, 2, active ? 0xFFFFE1A0 : color);
        g.centeredText(font, Component.literal(label), p.x(), p.y() - 38, active ? 0xFFFFDE91 : 0xFF9B8DB2);
        g.centeredText(font, Component.literal(spell == null ? "비어 있음" : spell.name()), p.x(), p.y() + 32,
                0xFFF0EAFE);
    }

    private void spellNode(GuiGraphicsExtractor g, Point p, SpellDefinition spell, boolean learned,
                           boolean usable, boolean focus, boolean weave, boolean hover, long time) {
        int school = schoolColor(spell.school());
        fillCircle(g, p.x(), p.y(), 15, 0xFF070913);
        ring(g, p.x(), p.y(), 15, focus ? 0xFFFFD36B : weave ? 0xFF65D6F2
                : !learned ? 0xFF383942 : hover ? 0xFFF1D9FF : school);
        ring(g, p.x(), p.y(), 11, learned ? school : 0xFF30313A);
        if (learned) rune(g, p.x(), p.y(), spell.school(), 7, usable ? 0xFFF8F4FF : 0xFF746F7E);
        else diamond(g, p.x(), p.y(), 5, 0xFF4B4851);
        if (focus || weave) {
            int ox = p.x() + (int) Math.round(Math.cos(time / 350.0) * 19.0);
            int oy = p.y() + (int) Math.round(Math.sin(time / 350.0) * 19.0);
            fillCircle(g, ox, oy, 2, focus ? 0xFFFFD36B : 0xFF65D6F2);
        }
        g.centeredText(font, Component.literal(shorten(spell.name(), 9)), p.x(), p.y() + 20,
                usable ? 0xFFEFE8FA : learned ? 0xFF817A89 : 0xFF504D58);
    }

    private void mastery(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY, long time) {
        Rect c = l.content();
        Point center = new Point(l.cx(), c.y() + c.h() / 2 - 4);
        fillCircle(g, center.x(), center.y(), 38, 0xFF070914);
        ring(g, center.x(), center.y(), 38, 0xFF8E63D5);
        ring(g, center.x(), center.y(), 29, 0xFF4C82C9);
        rune(g, center.x(), center.y() - 3, SpellDefinition.School.ARCANE, 13, 0xFFF2E7FF);
        g.centeredText(font, Component.literal("융합 각인"), center.x(), center.y() + 18, 0xFFE8D5FF);

        SpellCatalog.FusionFormula hovered = null;
        List<SpellCatalog.FusionFormula> formulas = SpellCatalog.fusions();
        int rx = Math.max(170, c.w() / 2 - 88);
        int ry = Math.max(86, c.h() / 2 - 55);
        for (int i = 0; i < formulas.size(); i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / formulas.size();
            int x = center.x() + (int) Math.round(Math.cos(angle) * rx);
            int y = center.y() + (int) Math.round(Math.sin(angle) * ry);
            SpellCatalog.FusionFormula formula = formulas.get(i);
            SpellDefinition result = SpellCatalog.spell(formula.result()).orElseThrow();
            boolean registered = ArcaneClientState.known().contains(result.id());
            int casts = ArcaneClientState.mastery(result.id());
            int required = SpellCatalog.masteryRequired(result.id());
            line(g, center.x(), center.y(), x, y, registered ? 0x887F6A37 : 0x554B4E70);
            masteryNode(g, x, y, result, casts, required, registered, time + i * 160L);
            if (distance(mouseX, mouseY, x, y) <= 26 * 26) hovered = formula;
        }

        if (hovered != null) {
            SpellDefinition a = SpellCatalog.spell(hovered.first()).orElseThrow();
            SpellDefinition b = SpellCatalog.spell(hovered.second()).orElseThrow();
            SpellDefinition result = SpellCatalog.spell(hovered.result()).orElseThrow();
            g.centeredText(font, Component.literal(a.name() + "  ×  " + b.name() + "  →  " + result.name()),
                    center.x(), c.bottom() - 26, 0xFFF0E7FF);
            g.centeredText(font, Component.literal(shorten(result.description(), 72)), center.x(), c.bottom() - 14,
                    0xFF9EABC5);
        } else {
            g.centeredText(font, Component.literal("G로 융합 마법을 실제 성공시킬 때마다 완성 회로가 새겨진다"),
                    center.x(), c.bottom() - 19, 0xFF98A4BF);
        }
    }

    private void masteryNode(GuiGraphicsExtractor g, int x, int y, SpellDefinition spell, int casts,
                             int required, boolean registered, long time) {
        int color = schoolColor(spell.school());
        fillCircle(g, x, y, 18, 0xFF070913);
        ring(g, x, y, 18, registered ? 0xFFFFD36B : color);
        rune(g, x, y, spell.school(), 8, registered ? 0xFFFFF2C8 : 0xFFF5EDFF);
        int filled = (int) Math.round(12 * Math.min(1.0, casts / (double) required));
        for (int i = 0; i < 12; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / 12.0;
            int sx = x + (int) Math.round(Math.cos(angle) * 24.0);
            int sy = y + (int) Math.round(Math.sin(angle) * 24.0);
            fillCircle(g, sx, sy, i < filled ? 2 : 1, i < filled ? color : 0xFF3E4050);
        }
        if (registered) {
            int ox = x + (int) Math.round(Math.cos(time / 520.0) * 29.0);
            int oy = y + (int) Math.round(Math.sin(time / 520.0) * 29.0);
            fillCircle(g, ox, oy, 2, 0xFFFFD36B);
        }
        g.centeredText(font, Component.literal(shorten(spell.name(), 9)), x, y + 31,
                registered ? 0xFFFFE7A8 : 0xFFE4DCF0);
        g.centeredText(font, Component.literal(registered ? "직접 시전 등록" : casts + "/" + required), x, y + 42,
                registered ? 0xFFD2A95B : 0xFF8C819B);
    }

    private void core(GuiGraphicsExtractor g, Layout l, long time) {
        Rect c = l.content();
        int circle = ArcaneClientState.integer("circle", 1);
        Point center = new Point(l.cx(), c.y() + c.h() / 2);
        for (int r = 9; r >= 1; r--) {
            int radius = 18 + r * 10;
            ring(g, center.x(), center.y(), radius,
                    r <= circle ? 0xFFB17BE8 : r <= 3 ? 0xFF4C4961 : 0xFF272A35);
        }
        for (int i = 0; i < 9; i++) {
            double angle = time / 850.0 + i * Math.PI * 2.0 / 9.0;
            int radius = 28 + i * 9;
            fillCircle(g, center.x() + (int) Math.round(Math.cos(angle) * radius),
                    center.y() + (int) Math.round(Math.sin(angle) * radius), i < circle ? 2 : 1,
                    i < circle ? 0xFFEBD6FF : 0xFF4F505A);
        }
        fillCircle(g, center.x(), center.y(), 14, 0xFF080A15);
        diamond(g, center.x(), center.y(), 11, 0xFFB67CF0);
        g.centeredText(font, Component.literal(circle + "C"), center.x(), center.y() - 4, 0xFFFFFFFF);

        int top = c.y() + 38;
        g.text(font, Component.literal("마력핵 상태"), c.x() + 18, top, 0xFFE2CBFF);
        g.text(font, Component.literal("최대 마력  " + ArcaneClientState.integer("max", 100)), c.x() + 18, top + 18, 0xFFADC7F2);
        g.text(font, Component.literal("현재 마력  " + ArcaneClientState.integer("mana", 0)), c.x() + 18, top + 31, 0xFFADC7F2);
        g.text(font, Component.literal("통찰  " + ArcaneClientState.integer("insight", 0)), c.x() + 18, top + 44, 0xFFB9A5D4);
        int right = c.right() - 180;
        g.text(font, Component.literal("회로 원리"), right, top, 0xFFE2CBFF);
        g.text(font, Component.literal("1C  시동환"), right, top + 18, circle >= 1 ? 0xFFE9E3F4 : 0xFF66616D);
        g.text(font, Component.literal("2C  교직환"), right, top + 31, circle >= 2 ? 0xFFE9E3F4 : 0xFF66616D);
        g.text(font, Component.literal("3C  영역환"), right, top + 44, circle >= 3 ? 0xFFE9E3F4 : 0xFF66616D);
        g.text(font, Component.literal("4~9C  미구현 영역"), right, top + 61, 0xFF615D68);
        g.text(font, Component.literal("저써클 주문은 써클 차이마다 마력·쿨타임 감소, 위력·사거리 증가"),
                c.x() + 18, c.bottom() - 20, 0xFF98A4BF);
    }

    private void rune(GuiGraphicsExtractor g, int x, int y, SpellDefinition.School school, int size, int color) {
        switch (school) {
            case FIRE -> {
                line(g, x, y - size, x - size, y + size, color);
                line(g, x - size, y + size, x + size, y + size, color);
                line(g, x + size, y + size, x, y - size, color);
                line(g, x, y - size / 2, x, y + size, color);
            }
            case FROST -> {
                line(g, x - size, y, x + size, y, color);
                line(g, x - size / 2, y - size, x + size / 2, y + size, color);
                line(g, x + size / 2, y - size, x - size / 2, y + size, color);
            }
            case WIND -> {
                line(g, x - size, y - size / 2, x + size, y - size / 2, color);
                line(g, x - size / 2, y, x + size, y, color);
                line(g, x - size, y + size / 2, x + size / 2, y + size / 2, color);
            }
            case WARD -> polygon(g, x, y, size, 6, color);
            case LIFE -> {
                line(g, x, y - size, x, y + size, color);
                line(g, x - size, y, x + size, y, color);
                diamond(g, x, y - size / 2, Math.max(2, size / 3), color);
            }
            case SPACE -> {
                diamond(g, x, y, size, color);
                diamond(g, x, y, Math.max(2, size / 2), color);
            }
            default -> {
                diamond(g, x, y, size, color);
                line(g, x - size, y, x + size, y, color);
                line(g, x, y - size, x, y + size, color);
            }
        }
    }

    private void polygon(GuiGraphicsExtractor g, int x, int y, int radius, int sides, int color) {
        Point first = null;
        Point previous = null;
        for (int i = 0; i < sides; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides;
            Point p = new Point(x + (int) Math.round(Math.cos(angle) * radius),
                    y + (int) Math.round(Math.sin(angle) * radius));
            if (first == null) first = p;
            if (previous != null) line(g, previous.x(), previous.y(), p.x(), p.y(), color);
            previous = p;
        }
        if (first != null && previous != null) line(g, previous.x(), previous.y(), first.x(), first.y(), color);
    }

    private void corner(GuiGraphicsExtractor g, int x, int y, int horizontal, int vertical) {
        line(g, x, y, x + horizontal * 18, y, 0xFF8665B0);
        line(g, x, y, x, y + vertical * 18, 0xFF8665B0);
        diamond(g, x, y, 3, 0xFFD8B7FF);
    }

    private static void fillCircle(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static void ring(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        int points = Math.max(24, radius * 5);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void diamond(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        line(g, cx, cy - radius, cx + radius, cy, color);
        line(g, cx + radius, cy, cx, cy + radius, color);
        line(g, cx, cy + radius, cx - radius, cy, color);
        line(g, cx - radius, cy, cx, cy - radius, color);
    }

    private static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            double p = i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * p);
            int y = (int) Math.round(y1 + (y2 - y1) * p);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private Layout layout() {
        int panelW = Math.min(820, Math.max(600, width - 24));
        int panelH = Math.min(470, Math.max(360, height - 20));
        return new Layout((width - panelW) / 2, (height - panelH) / 2, panelW, panelH);
    }

    private static int schoolColor(SpellDefinition.School school) {
        return switch (school) {
            case FIRE -> 0xFFE36C45;
            case FROST -> 0xFF63C8E8;
            case WIND -> 0xFF72D4B0;
            case WARD -> 0xFFB38BE8;
            case LIFE -> 0xFF71D487;
            case SPACE -> 0xFF8669E5;
            default -> 0xFF6F91E7;
        };
    }

    private static boolean inside(int x, int y, Rect r) {
        return x >= r.x() && y >= r.y() && x < r.right() && y < r.bottom();
    }

    private static int distance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private static String normalize(String page) {
        return "mastery".equals(page) || "core".equals(page) ? page : "atlas";
    }

    private static String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private record Tab(String id, String label) {}
    private record Point(int x, int y) {}
    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }

    private record Layout(int left, int top, int panelW, int panelH) {
        int right() { return left + panelW; }
        int bottom() { return top + panelH; }
        int cx() { return left + panelW / 2; }
        Rect close() { return new Rect(right() - 33, top + 16, 20, 20); }
        Rect tab(int i) { return new Rect(cx() - 162 + i * 108, top + 43, 108, 22); }
        Rect content() { return new Rect(left + 14, top + 72, panelW - 28, panelH - 91); }
        Point focus() { return new Point(cx() - 94, content().y() + 48); }
        Point weave() { return new Point(cx() + 94, content().y() + 48); }
        Rect focusHit() { return new Rect(focus().x() - 31, focus().y() - 31, 62, 76); }
        Rect weaveHit() { return new Rect(weave().x() - 31, weave().y() - 31, 62, 76); }
        Point node(int index) {
            Rect c = content();
            int row = index / 5;
            int column = index % 5;
            int start = c.x() + 62;
            int end = c.right() - 42;
            int x = start + (end - start) * column / 4;
            int gap = Math.max(44, Math.min(58, Math.max(88, c.h() - 162) / 2));
            return new Point(x, c.y() + 116 + row * gap);
        }
        Rect nodeHit(int index) {
            Point p = node(index);
            return new Rect(p.x() - 24, p.y() - 19, 48, 49);
        }
    }
}

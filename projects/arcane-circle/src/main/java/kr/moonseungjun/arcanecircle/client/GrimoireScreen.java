package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.EquipSpellPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GrimoireScreen extends Screen {
    private static final List<Tab> TABS = List.of(
            new Tab("atlas", "주문"), new Tab("recipes", "융합식"), new Tab("core", "마력핵"));
    private static int savedOffsetX;
    private static int savedOffsetY;

    private final String page;
    private int activeSlot;
    private int contentScroll;
    private boolean dragging;
    private double dragAnchorX;
    private double dragAnchorY;
    private int dragOriginX;
    private int dragOriginY;

    public GrimoireScreen(String page) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("구중 마도서"));
        this.page = normalize(page);
    }

    @Override
    protected void init() {
        super.init();
        clampSavedOffset();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout l = layout();
        if (event.button() == 0 && inside(event.x(), event.y(), l.dragBar())) {
            dragging = true;
            dragAnchorX = event.x();
            dragAnchorY = event.y();
            dragOriginX = savedOffsetX;
            dragOriginY = savedOffsetY;
            return true;
        }
        if (inside(event.x(), event.y(), l.close())) {
            onClose();
            return true;
        }
        for (int i = 0; i < TABS.size(); i++) {
            if (inside(event.x(), event.y(), l.tab(i))) {
                request(TABS.get(i).id());
                return true;
            }
        }
        if ("atlas".equals(page)) {
            for (int i = 0; i < 5; i++) {
                if (inside(event.x(), event.y(), l.slot(i))) {
                    activeSlot = i;
                    return true;
                }
            }
            List<SpellDefinition> spells = new ArrayList<>(SpellCatalog.spells().values());
            for (int i = 0; i < spells.size(); i++) {
                if (inside(event.x(), event.y(), l.spellHit(i, contentScroll))) {
                    select(spells.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!dragging) return super.mouseDragged(event, deltaX, deltaY);
        savedOffsetX = dragOriginX + (int) Math.round(event.x() - dragAnchorX);
        savedOffsetY = dragOriginY + (int) Math.round(event.y() - dragAnchorY);
        clampSavedOffset();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = dragging;
        dragging = false;
        return handled || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0) return false;
        Layout l = layout();
        if (!inside(mouseX, mouseY, l.content())) return false;
        int max = "atlas".equals(page) ? l.maxAtlasScroll(SpellCatalog.spells().size())
                : "recipes".equals(page) ? l.maxRecipeScroll(SpellCatalog.fusions().size()) : 0;
        contentScroll = clamp(contentScroll + (scrollY < 0 ? 28 : -28), 0, max);
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        long time = System.currentTimeMillis();
        g.fill(0, 0, width, height, 0xC7040610);
        stars(g, time);
        frame(g, l);
        header(g, l, mouseX, mouseY);
        switch (page) {
            case "recipes" -> recipes(g, l, mouseX, mouseY);
            case "core" -> core(g, l, time);
            default -> atlas(g, l, mouseX, mouseY, time);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void atlas(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY, long time) {
        Rect c = l.content();
        drawManaStrip(g, l);
        for (int i = 0; i < 5; i++) drawLoadoutSlot(g, l.slot(i), i, activeSlot == i, time + i * 140L);

        int gridTop = l.gridTop();
        g.enableScissor(c.x(), gridTop, c.right(), c.bottom());
        List<SpellDefinition> spells = new ArrayList<>(SpellCatalog.spells().values());
        Set<String> known = ArcaneClientState.known();
        int circle = ArcaneClientState.integer("circle", 1);
        SpellDefinition hovered = null;
        for (int i = 0; i < spells.size(); i++) {
            SpellDefinition spell = spells.get(i);
            Rect card = l.spellHit(i, contentScroll);
            boolean learned = known.contains(spell.id());
            boolean usable = learned && spell.circle() <= circle;
            boolean equipped = ArcaneClientState.slots().contains(spell.id());
            boolean hover = inside(mouseX, mouseY, card);
            drawSpellCard(g, card, spell, usable, equipped, hover, time + i * 91L);
            if (hover) hovered = spell;
        }
        g.disableScissor();

        if (hovered != null) drawSpellTooltip(g, l, hovered);
        else g.centeredText(font, Component.literal("1~5 슬롯을 고른 뒤 주문 문양을 누르세요 · 마우스 휠로 목록 이동"),
                l.cx(), c.bottom() - 12, 0xFF8E9AB4);
    }

    private void drawManaStrip(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int barX = c.x() + 10;
        int barY = c.y() + 5;
        int barW = Math.max(72, Math.min(160, c.w() / 4));
        g.fill(barX, barY + 12, barX + barW, barY + 18, 0xFF192037);
        g.fill(barX + 1, barY + 13, barX + 1 + (int) ((barW - 2) * Math.min(1.0, mana / (double) max)),
                barY + 17, 0xFF5684E6);
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C  MANA " + mana + "/" + max),
                barX, barY, 0xFFC9D8F2);
        g.text(font, Component.literal("장착: " + ArcaneClientState.text("staff", "맨손")),
                c.right() - Math.min(170, c.w() / 3), barY, 0xFFFFD58A);
    }

    private void drawLoadoutSlot(GuiGraphicsExtractor g, Rect r, int slot, boolean active, long time) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        int color = spell == null ? 0xFF555A6A : ArcaneRenderUtil.schoolColor(spell.school());
        g.fill(r.x(), r.y(), r.right(), r.bottom(), active ? 0xFF6C4B8F : 0xFF080C18);
        g.fill(r.x() + 2, r.y() + 2, r.right() - 2, r.bottom() - 2, 0xFF121A2C);
        ArcaneRenderUtil.cooldownArc(g, r.x(), r.y(), r.w() - 1, ArcaneClientState.cooldownFraction(slot),
                0xFFE46B72, active ? 0xFFFFD36B : color);
        g.text(font, Component.literal(Integer.toString(slot + 1)), r.x() + 4, r.y() + 3, 0xFFFFFFFF);
        if (spell != null) {
            ArcaneRenderUtil.spellRune(g, r.x() + r.w() / 2, r.y() + r.h() / 2 - 3, spell,
                    Math.max(7, r.w() / 6), 0xFFF8F3FF);
            g.centeredText(font, Component.literal(shorten(spell.name(), 7)), r.x() + r.w() / 2,
                    r.bottom() - 11, active ? 0xFFFFE0A2 : 0xFFD8D0E6);
        }
        if (active) {
            int ox = r.x() + r.w() / 2 + (int) Math.round(Math.cos(time / 420.0) * (r.w() / 2 + 4));
            int oy = r.y() + r.h() / 2 + (int) Math.round(Math.sin(time / 420.0) * (r.h() / 2 + 4));
            ArcaneRenderUtil.fillCircle(g, ox, oy, 2, 0xFFFFD36B);
        }
    }

    private void drawSpellCard(GuiGraphicsExtractor g, Rect r, SpellDefinition spell, boolean usable,
                               boolean equipped, boolean hover, long time) {
        if (r.bottom() < 0 || r.y() > height) return;
        int school = ArcaneRenderUtil.schoolColor(spell.school());
        g.fill(r.x(), r.y(), r.right(), r.bottom(), hover ? 0xFF293451 : 0xFF111827);
        g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), usable ? school : 0xFF3B3D46);
        int iconX = r.x() + 22;
        int iconY = r.y() + r.h() / 2;
        ArcaneRenderUtil.fillCircle(g, iconX, iconY, 15, 0xFF070A13);
        ArcaneRenderUtil.ring(g, iconX, iconY, 15, usable ? school : 0xFF474852);
        if (usable) ArcaneRenderUtil.spellRune(g, iconX, iconY, spell, 8, 0xFFF7F0FF);
        else ArcaneRenderUtil.diamond(g, iconX, iconY, 6, 0xFF5B5963);
        g.text(font, Component.literal(spell.circle() + "C " + spell.name()), r.x() + 44, r.y() + 8,
                usable ? 0xFFF0E8FA : 0xFF77727D);
        g.text(font, Component.literal(spell.school().displayName() + " · MP " + spell.manaCost()),
                r.x() + 44, r.y() + 21, usable ? 0xFF9CB1CE : 0xFF5E5B64);
        if (equipped) {
            ArcaneRenderUtil.ring(g, iconX, iconY, 19, 0xFFFFD36B);
            int ox = iconX + (int) Math.round(Math.cos(time / 390.0) * 20.0);
            int oy = iconY + (int) Math.round(Math.sin(time / 390.0) * 20.0);
            ArcaneRenderUtil.fillCircle(g, ox, oy, 2, 0xFFFFD36B);
        }
    }

    private void drawSpellTooltip(GuiGraphicsExtractor g, Layout l, SpellDefinition spell) {
        Rect c = l.content();
        String line = spell.description() + "  [위력 " + trim(spell.power()) + " · 사거리 " + trim(spell.range())
                + " · 쿨 " + String.format("%.1f", spell.cooldownTicks() / 20.0) + "초]";
        g.centeredText(font, Component.literal(shorten(line, Math.max(34, c.w() / 6))), l.cx(),
                c.bottom() - 12, 0xFFD5C7E6);
    }

    private void recipes(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c = l.content();
        drawManaStrip(g, l);
        int top = c.y() + 29;
        g.enableScissor(c.x(), top, c.right(), c.bottom());
        List<SpellCatalog.FusionFormula> formulas = SpellCatalog.fusions();
        for (int i = 0; i < formulas.size(); i++) {
            Rect row = l.recipeRow(i, contentScroll);
            drawRecipeRow(g, row, formulas.get(i), inside(mouseX, mouseY, row));
        }
        g.disableScissor();
        g.centeredText(font, Component.literal("X를 누른 채 재료 주문 2~3개를 숫자키로 고르고 X를 놓으면 시전"),
                l.cx(), c.bottom() - 12, 0xFF9BA7BF);
    }

    private void drawRecipeRow(GuiGraphicsExtractor g, Rect r, SpellCatalog.FusionFormula formula, boolean hover) {
        SpellDefinition result = SpellCatalog.spell(formula.result()).orElseThrow();
        int color = ArcaneRenderUtil.schoolColor(result.school());
        boolean registered = ArcaneClientState.known().contains(result.id());
        int mastery = ArcaneClientState.mastery(result.id());
        int required = SpellCatalog.masteryRequired(result.id());
        g.fill(r.x(), r.y(), r.right(), r.bottom(), hover ? 0xFF26324B : 0xFF111827);
        g.fill(r.x(), r.y(), r.x() + 3, r.bottom(), registered ? 0xFFFFD36B : color);
        int x = r.x() + 14;
        for (int i = 0; i < formula.ingredients().size(); i++) {
            SpellDefinition source = SpellCatalog.spell(formula.ingredients().get(i)).orElseThrow();
            ArcaneRenderUtil.fillCircle(g, x + 15, r.y() + 22, 13, 0xFF070A13);
            ArcaneRenderUtil.ring(g, x + 15, r.y() + 22, 13, ArcaneRenderUtil.schoolColor(source.school()));
            ArcaneRenderUtil.spellRune(g, x + 15, r.y() + 22, source, 7, 0xFFF5EDFF);
            x += 34;
            if (i < formula.ingredients().size() - 1) {
                g.text(font, Component.literal("+"), x - 5, r.y() + 17, 0xFFBBA7D0);
                x += 10;
            }
        }
        g.text(font, Component.literal("→"), x + 2, r.y() + 17, 0xFFEBD9FF);
        x += 22;
        ArcaneRenderUtil.fillCircle(g, x + 15, r.y() + 22, 15, 0xFF070A13);
        ArcaneRenderUtil.ring(g, x + 15, r.y() + 22, 15, registered ? 0xFFFFD36B : color);
        ArcaneRenderUtil.spellRune(g, x + 15, r.y() + 22, result, 8, 0xFFFFFFFF);
        int textX = x + 37;
        g.text(font, Component.literal(result.circle() + "C " + result.name()), textX, r.y() + 7,
                registered ? 0xFFFFE2A5 : 0xFFEADDF8);
        g.text(font, Component.literal(registered ? "직접 시전 등록 완료" : "실전 숙련 " + mastery + "/" + required),
                textX, r.y() + 21, registered ? 0xFFD8B565 : 0xFF9D8BB1);
        int barW = Math.max(34, r.right() - textX - 12);
        g.fill(textX, r.y() + 34, textX + barW, r.y() + 38, 0xFF272C3B);
        g.fill(textX, r.y() + 34, textX + (int) (barW * Math.min(1.0, mastery / (double) required)),
                r.y() + 38, registered ? 0xFFFFD36B : color);
    }

    private void core(GuiGraphicsExtractor g, Layout l, long time) {
        Rect c = l.content();
        int circle = ArcaneClientState.integer("circle", 1);
        boolean compact = c.w() < 560;
        int centerX = compact ? l.cx() : l.cx() - 12;
        int centerY = c.y() + c.h() / 2 + 8;
        int maxRadius = Math.max(42, Math.min(compact ? c.w() / 4 : c.w() / 5, c.h() / 2 - 22));
        for (int r = 9; r >= 1; r--) {
            int radius = Math.max(8, maxRadius * r / 9);
            ArcaneRenderUtil.ring(g, centerX, centerY, radius,
                    r <= circle ? 0xFFB17BE8 : r <= 3 ? 0xFF4C4961 : 0xFF272A35);
        }
        for (int i = 0; i < 9; i++) {
            double angle = time / 850.0 + i * Math.PI * 2.0 / 9.0;
            int radius = Math.max(12, maxRadius * (i + 1) / 9);
            ArcaneRenderUtil.fillCircle(g, centerX + (int) Math.round(Math.cos(angle) * radius),
                    centerY + (int) Math.round(Math.sin(angle) * radius), i < circle ? 2 : 1,
                    i < circle ? 0xFFEBD6FF : 0xFF4F505A);
        }
        ArcaneRenderUtil.fillCircle(g, centerX, centerY, 14, 0xFF080A15);
        ArcaneRenderUtil.diamond(g, centerX, centerY, 11, 0xFFB67CF0);
        g.centeredText(font, Component.literal(circle + "C"), centerX, centerY - 4, 0xFFFFFFFF);

        int left = c.x() + 12;
        int right = c.right() - 176;
        int top = c.y() + 38;
        if (compact) {
            top = c.y() + 8;
            left = c.x() + 8;
            right = c.right() - 150;
        }
        drawCorePanel(g, left, top, 148, "마력핵 상태", List.of(
                "최대 마력  " + ArcaneClientState.integer("max", 100),
                "현재 마력  " + ArcaneClientState.integer("mana", 0),
                "초당 회복  " + String.format("%.1f", ArcaneClientState.regenPerSecond()),
                "통찰  " + ArcaneClientState.integer("insight", 0)));
        drawCorePanel(g, right, top, 164, "지팡이 조율", List.of(
                ArcaneClientState.text("staff", "맨손"),
                shorten(ArcaneClientState.text("staff_summary", "효과 없음"), 22),
                "1C 시동환 · 2C 교직환",
                "3C 영역환 · 4~9C 미구현"));
        g.centeredText(font, Component.literal("저써클 주문은 써클 차이마다 소모·쿨 감소, 위력·범위 증가"),
                l.cx(), c.bottom() - 13, 0xFF8F9BB3);
    }

    private void drawCorePanel(GuiGraphicsExtractor g, int x, int y, int w, String title, List<String> lines) {
        g.fill(x, y, x + w, y + 72, 0xB90A0E1B);
        g.fill(x, y, x + w, y + 1, 0xFF7F60AB);
        g.text(font, Component.literal(title), x + 7, y + 7, 0xFFE1CBF8);
        for (int i = 0; i < lines.size(); i++) {
            g.text(font, Component.literal(lines.get(i)), x + 7, y + 22 + i * 12, 0xFFAEB9CF);
        }
    }

    private void frame(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.left() - 3, l.top() - 3, l.right() + 3, l.bottom() + 3, 0xFF050611);
        g.fill(l.left() - 1, l.top() - 1, l.right() + 1, l.bottom() + 1, 0xFF8060B3);
        g.fill(l.left(), l.top(), l.right(), l.bottom(), 0xF20B1120);
        g.fill(l.left() + 3, l.top() + 3, l.right() - 3, l.bottom() - 3, 0xF2121A30);
        ArcaneRenderUtil.line(g, l.left() + 10, l.top() + 10, l.right() - 10, l.top() + 10, 0xFFCAA6F1);
        ArcaneRenderUtil.line(g, l.left() + 10, l.bottom() - 10, l.right() - 10, l.bottom() - 10, 0xFF544368);
    }

    private void header(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        g.centeredText(font, Component.literal("NINEFOLD ARCANA"), l.cx(), l.top() + 12, 0xFFF1DEFF);
        g.centeredText(font, Component.literal(dragging ? "화면 이동 중" : "상단을 드래그해 이동"),
                l.cx(), l.top() + 24, dragging ? 0xFFFFD36B : 0xFF71667F);
        for (int i = 0; i < TABS.size(); i++) {
            Rect tab = l.tab(i);
            boolean active = TABS.get(i).id().equals(page);
            boolean hover = inside(mouseX, mouseY, tab);
            g.centeredText(font, Component.literal(TABS.get(i).label()), tab.x() + tab.w() / 2, tab.y() + 5,
                    active ? 0xFFE7C9FF : hover ? 0xFFC7A9E8 : 0xFF746982);
            if (active) ArcaneRenderUtil.line(g, tab.x() + 8, tab.bottom() - 2, tab.right() - 8, tab.bottom() - 2, 0xFFB16EEE);
        }
        Rect close = l.close();
        ArcaneRenderUtil.diamond(g, close.x() + 9, close.y() + 9, 7,
                inside(mouseX, mouseY, close) ? 0xFFD86382 : 0xFF67384F);
        g.centeredText(font, Component.literal("×"), close.x() + 9, close.y() + 3, 0xFFFFFFFF);
    }

    private void stars(GuiGraphicsExtractor g, long time) {
        for (int i = 0; i < 48; i++) {
            int x = Math.floorMod(i * 97 + 31, Math.max(1, width));
            int y = Math.floorMod(i * 53 + 17, Math.max(1, height));
            int phase = (int) ((time / 170 + i * 7) % 14);
            int color = phase < 3 ? 0x88D8C8FF : phase < 8 ? 0x444D70B7 : 0x22433B5C;
            g.fill(x, y, x + (phase == 0 ? 2 : 1), y + 1, color);
        }
    }

    private void select(SpellDefinition spell) {
        int circle = ArcaneClientState.integer("circle", 1);
        if (!ArcaneClientState.known().contains(spell.id()) || spell.circle() > circle) return;
        ClientPacketDistributor.sendToServer(new EquipSpellPayload(spell.id(), activeSlot));
    }

    private void request(String next) {
        ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(next));
    }

    private void clampSavedOffset() {
        int panelW = Math.max(0, Math.min(760, width - 10));
        int panelH = Math.max(0, Math.min(430, height - 10));
        int baseLeft = (width - panelW) / 2;
        int baseTop = (height - panelH) / 2;
        savedOffsetX = clamp(savedOffsetX, 4 - baseLeft, width - 4 - panelW - baseLeft);
        savedOffsetY = clamp(savedOffsetY, 4 - baseTop, height - 4 - panelH - baseTop);
    }

    private Layout layout() {
        int panelW = Math.max(0, Math.min(760, width - 10));
        int panelH = Math.max(0, Math.min(430, height - 10));
        int left = clamp((width - panelW) / 2 + savedOffsetX, 4, Math.max(4, width - panelW - 4));
        int top = clamp((height - panelH) / 2 + savedOffsetY, 4, Math.max(4, height - panelH - 4));
        return new Layout(left, top, panelW, panelH);
    }

    private static boolean inside(double x, double y, Rect r) {
        return x >= r.x() && y >= r.y() && x < r.right() && y < r.bottom();
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String page) {
        return "recipes".equals(page) || "core".equals(page) ? page : "atlas";
    }

    private static String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : String.format("%.1f", value);
    }

    private record Tab(String id, String label) {}
    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }

    private record Layout(int left, int top, int panelW, int panelH) {
        int right() { return left + panelW; }
        int bottom() { return top + panelH; }
        int cx() { return left + panelW / 2; }
        Rect dragBar() { return new Rect(left + 4, top + 4, Math.max(0, panelW - 36), 29); }
        Rect close() { return new Rect(right() - 27, top + 10, 18, 18); }
        Rect tab(int i) {
            int tabW = Math.max(58, Math.min(104, (panelW - 24) / 3));
            int total = tabW * 3;
            return new Rect(cx() - total / 2 + i * tabW, top + 34, tabW, 24);
        }
        Rect content() { return new Rect(left + 10, top + 62, Math.max(0, panelW - 20), Math.max(0, panelH - 75)); }
        int gridTop() { return content().y() + 78; }
        Rect slot(int i) {
            Rect c = content();
            int gap = c.w() < 500 ? 3 : 6;
            int maxSize = c.w() < 500 ? 45 : 54;
            int size = Math.max(28, Math.min(maxSize, (c.w() - 16 - gap * 4) / 5));
            int total = size * 5 + gap * 4;
            return new Rect(cx() - total / 2 + i * (size + gap), c.y() + 24, size, size);
        }
        int columns() { return content().w() >= 650 ? 4 : content().w() >= 440 ? 3 : 2; }
        Rect spellHit(int index, int scroll) {
            Rect c = content();
            int cols = columns();
            int gap = 6;
            int cardW = Math.max(92, (c.w() - gap * (cols - 1)) / cols);
            int cardH = 44;
            int row = index / cols;
            int col = index % cols;
            return new Rect(c.x() + col * (cardW + gap), gridTop() + row * (cardH + 6) - scroll, cardW, cardH);
        }
        int maxAtlasScroll(int count) {
            int rows = (count + columns() - 1) / columns();
            int totalH = rows * 50;
            return Math.max(0, totalH - Math.max(40, content().bottom() - gridTop() - 18));
        }
        Rect recipeRow(int index, int scroll) {
            Rect c = content();
            return new Rect(c.x() + 4, c.y() + 30 + index * 50 - scroll, Math.max(0, c.w() - 8), 44);
        }
        int maxRecipeScroll(int count) {
            return Math.max(0, count * 50 - Math.max(40, content().h() - 54));
        }
    }
}

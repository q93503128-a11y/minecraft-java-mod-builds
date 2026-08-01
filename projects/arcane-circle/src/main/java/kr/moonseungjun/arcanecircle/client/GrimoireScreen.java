package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.EquipSpellPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GrimoireScreen extends Screen {
    private static final List<Tab> TABS = List.of(
            new Tab("atlas", "주문"),
            new Tab("recipes", "융합식"),
            new Tab("staffs", "지팡이"),
            new Tab("core", "마력핵"));
    private static final Map<String, Integer> SAVED_SCROLL = new HashMap<>();
    private static int savedOffsetX;
    private static int savedOffsetY;
    private static int savedActiveSlot;
    private static int savedCircleFilter;

    private final String page;
    private int contentScroll;
    private boolean dragging;
    private double dragAnchorX;
    private double dragAnchorY;
    private int dragOriginX;
    private int dragOriginY;
    private String notice = "";
    private long noticeUntil;

    public GrimoireScreen(String page) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("구중 마도서"));
        this.page = normalize(page);
        this.contentScroll = SAVED_SCROLL.getOrDefault(this.page, 0);
    }

    @Override
    protected void init() {
        super.init();
        clampSavedOffset();
        contentScroll = Math.min(contentScroll, maxScroll(layout()));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void onClose() {
        SAVED_SCROLL.put(page, contentScroll);
        super.onClose();
    }

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
            for (int circle = 0; circle <= SpellCatalog.IMPLEMENTED_MAX_CIRCLE; circle++) {
                if (inside(event.x(), event.y(), l.circleFilter(circle))) {
                    savedCircleFilter = circle;
                    contentScroll = 0;
                    SAVED_SCROLL.put(page, 0);
                    notice(circle == 0 ? "전체 주문 표시" : circle + "써클 주문만 표시");
                    return true;
                }
            }
            for (int i = 0; i < 5; i++) {
                if (inside(event.x(), event.y(), l.slot(i))) {
                    savedActiveSlot = i;
                    notice("슬롯 " + (i + 1) + " 선택");
                    return true;
                }
            }
            List<SpellDefinition> spells = visibleSpells();
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
        int max = maxScroll(l);
        contentScroll = clamp(contentScroll + (scrollY < 0 ? 30 : -30), 0, max);
        SAVED_SCROLL.put(page, contentScroll);
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        long time = System.currentTimeMillis();
        g.fill(0, 0, width, height, 0xC8050711);
        stars(g, time);
        frame(g, l);
        header(g, l, mouseX, mouseY);
        switch (page) {
            case "recipes" -> recipes(g, l, mouseX, mouseY);
            case "staffs" -> staffs(g, l, mouseX, mouseY);
            case "core" -> core(g, l, time);
            default -> atlas(g, l, mouseX, mouseY, time);
        }
        drawNotice(g, l);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void atlas(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY, long time) {
        Rect c = l.content();
        drawManaStrip(g, l);
        for (int i = 0; i < 5; i++) drawLoadoutSlot(g, l.slot(i), i, savedActiveSlot == i, time + i * 140L);
        drawCircleFilters(g, l, mouseX, mouseY);

        int gridTop = l.gridTop();
        g.enableScissor(c.x(), gridTop, c.right(), c.bottom() - 17);
        List<SpellDefinition> spells = visibleSpells();
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
        else footer(g, l, "슬롯 선택 → 주문 선택 · 1~5 누름: 전개 · 놓기: 시전 · 휠 이동");
    }

    private List<SpellDefinition> visibleSpells() {
        return SpellCatalog.spells().values().stream()
                .filter(spell -> savedCircleFilter == 0 || spell.circle() == savedCircleFilter)
                .toList();
    }

    private void drawCircleFilters(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        for (int circle = 0; circle <= SpellCatalog.IMPLEMENTED_MAX_CIRCLE; circle++) {
            Rect tab = l.circleFilter(circle);
            boolean active = savedCircleFilter == circle;
            boolean hover = inside(mouseX, mouseY, tab);
            int background = active ? 0xFF5B367C : hover ? 0xFF293754 : 0xFF111827;
            int border = active ? 0xFFE4B8FF : circle <= ArcaneClientState.integer("circle", 1)
                    ? 0xFF6E8FC7 : 0xFF4A4652;
            g.fill(tab.x(), tab.y(), tab.right(), tab.bottom(), background);
            g.fill(tab.x(), tab.bottom() - 2, tab.right(), tab.bottom(), border);
            g.centeredText(font, Component.literal(circle == 0 ? "전체" : circle + "C"),
                    tab.x() + tab.w() / 2, tab.y() + 5, active ? 0xFFFFFFFF : 0xFFD0C8DC);
        }
    }

    private void drawManaStrip(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int barW = Math.max(88, Math.min(176, c.w() / 3));
        int barX = c.x() + 8;
        int barY = c.y() + 4;
        g.fill(barX, barY + 12, barX + barW, barY + 19, 0xFF151D31);
        g.fill(barX + 1, barY + 13,
                barX + 1 + (int) ((barW - 2) * Math.min(1.0, mana / (double) max)),
                barY + 18, 0xFF5E8DEB);
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C  MANA " + mana + "/" + max),
                barX, barY, 0xFFD3E0F5);
        String staff = "장착  " + ArcaneClientState.text("staff", "맨손");
        if (c.w() >= 430) {
            g.text(font, Component.literal(shorten(staff, 22)), c.right() - Math.min(168, c.w() / 3), barY, 0xFFFFD58A);
        } else {
            g.centeredText(font, Component.literal(shorten(staff, 24)), l.cx(), barY + 22, 0xFFFFD58A);
        }
    }

    private void drawLoadoutSlot(GuiGraphicsExtractor g, Rect r, int slot, boolean active, long time) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        int color = spell == null ? 0xFF555A6A : ArcaneRenderUtil.schoolColor(spell.school());
        int dark = spell == null ? 0xFF151824 : ArcaneRenderUtil.schoolDark(spell.school());
        int remaining = ArcaneClientState.cooldownRemainingTicks(slot);
        g.fill(r.x() - 1, r.y() - 1, r.right() + 1, r.bottom() + 1, active ? 0xFFFFD36B : 0xFF060914);
        g.fill(r.x(), r.y(), r.right(), r.bottom(), remaining > 0 ? dark : 0xFF101829);
        if (remaining > 0) g.fill(r.x() + 2, r.y() + 2, r.right() - 2, r.bottom() - 2, 0x66101018);
        ArcaneRenderUtil.cooldownArc(g, r.x(), r.y(), r.w() - 1, ArcaneClientState.cooldownFraction(slot),
                remaining > 0 ? 0xFFE66E78 : color, active ? 0xFFFFD36B : 0xFF353B4B);
        g.text(font, Component.literal(Integer.toString(slot + 1)), r.x() + 4, r.y() + 3, 0xFFFFFFFF);
        if (spell != null) {
            ArcaneRenderUtil.spellRune(g, r.x() + r.w() / 2, r.y() + r.h() / 2 - 3, spell,
                    Math.max(6, r.w() / 6), remaining > 0 ? 0xFF827C89 : 0xFFF8F3FF);
            if (r.w() >= 34) g.centeredText(font, Component.literal(shorten(spell.name(), 6)), r.x() + r.w() / 2,
                    r.bottom() - 11, active ? 0xFFFFE0A2 : 0xFFD8D0E6);
        }
        if (remaining > 0) {
            g.centeredText(font, Component.literal(String.format("%.1f", remaining / 20.0)),
                    r.x() + r.w() / 2, r.y() + r.h() / 2 - 5, 0xFFFFFFFF);
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
        int bg = hover ? 0xFF293753 : usable ? 0xFF111A2B : 0xFF11151E;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), bg);
        g.fill(r.x(), r.y(), r.x() + 3, r.bottom(), usable ? school : 0xFF3B3D46);
        int iconX = r.x() + 22;
        int iconY = r.y() + r.h() / 2;
        ArcaneRenderUtil.fillCircle(g, iconX, iconY, 15, 0xFF070A13);
        ArcaneRenderUtil.ring(g, iconX, iconY, 15, usable ? school : 0xFF474852);
        if (usable) ArcaneRenderUtil.spellRune(g, iconX, iconY, spell, 8, 0xFFF7F0FF);
        else ArcaneRenderUtil.diamond(g, iconX, iconY, 6, 0xFF5B5963);
        int textX = r.x() + 44;
        g.text(font, Component.literal(spell.circle() + "C " + shorten(spell.name(), Math.max(5, r.w() / 10))),
                textX, r.y() + 8, usable ? 0xFFF0E8FA : 0xFF77727D);
        int points = ArcaneClientState.mastery(spell.id());
        int tier = SpellCatalog.masteryTier(points);
        String status = !ArcaneClientState.known().contains(spell.id()) ? "미습득"
                : spell.circle() > ArcaneClientState.integer("circle", 1) ? "써클 부족"
                : equipped ? "장착 · 숙련 " + tier : "사용 가능 · 숙련 " + tier;
        g.text(font, Component.literal(spell.school().displayName() + " · MP " + spell.manaCost() + " · " + status),
                textX, r.y() + 21, usable ? (equipped ? 0xFFFFD36B : 0xFFB8D4F4) : 0xFF77727D);
        if (equipped) {
            ArcaneRenderUtil.ring(g, iconX, iconY, 19, 0xFFFFD36B);
            int ox = iconX + (int) Math.round(Math.cos(time / 390.0) * 20.0);
            int oy = iconY + (int) Math.round(Math.sin(time / 390.0) * 20.0);
            ArcaneRenderUtil.fillCircle(g, ox, oy, 2, 0xFFFFD36B);
        }
    }

    private void drawSpellTooltip(GuiGraphicsExtractor g, Layout l, SpellDefinition spell) {
        Rect c = l.content();
        String state = ArcaneClientState.known().contains(spell.id()) ? "" : " · 미각인";
        String line = spell.acquisition().displayName() + " · " + spell.sigilAnchor().displayName() + " · " + spell.description() + "  [위력 " + trim(spell.power()) + " · 사거리 " + trim(spell.range())
                + " · 쿨 " + String.format("%.1f", spell.cooldownTicks() / 20.0) + "초" + state + "]";
        g.centeredText(font, Component.literal(shorten(line, Math.max(34, c.w() / 6))), l.cx(),
                c.bottom() - 12, 0xFFD5C7E6);
    }

    private void recipes(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c = l.content();
        drawManaStrip(g, l);
        int top = l.listTop();
        g.enableScissor(c.x(), top, c.right(), c.bottom() - 17);
        List<SpellCatalog.FusionFormula> formulas = SpellCatalog.fusions();
        for (int i = 0; i < formulas.size(); i++) {
            Rect row = l.recipeRow(i, contentScroll);
            drawRecipeRow(g, row, formulas.get(i), inside(mouseX, mouseY, row));
        }
        g.disableScissor();
        footer(g, l, "X 누르기 → 재료 주문 2~3개 → X 놓기 · 순서는 무관");
    }

    private void drawRecipeRow(GuiGraphicsExtractor g, Rect r, SpellCatalog.FusionFormula formula, boolean hover) {
        SpellDefinition result = SpellCatalog.spell(formula.result()).orElseThrow();
        int color = ArcaneRenderUtil.schoolColor(result.school());
        boolean registered = ArcaneClientState.known().contains(result.id());
        int mastery = ArcaneClientState.mastery(result.id());
        int required = SpellCatalog.masteryRequired(result.id());
        g.fill(r.x(), r.y(), r.right(), r.bottom(), hover ? 0xFF26344F : 0xFF111827);
        g.fill(r.x(), r.y(), r.x() + 3, r.bottom(), registered ? 0xFFFFD36B : color);

        int iconX = r.x() + 24;
        int iconY = r.y() + r.h() / 2;
        ArcaneRenderUtil.fillCircle(g, iconX, iconY, 16, 0xFF070A13);
        ArcaneRenderUtil.ring(g, iconX, iconY, 16, registered ? 0xFFFFD36B : color);
        ArcaneRenderUtil.spellRune(g, iconX, iconY, result, 9, 0xFFFFFFFF);

        String chain = formula.ingredients().stream()
                .map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .reduce((a, b) -> a + " + " + b).orElse("");
        int textX = r.x() + 48;
        g.text(font, Component.literal(result.circle() + "C " + result.name()), textX, r.y() + 6,
                registered ? 0xFFFFE2A5 : 0xFFEADDF8);
        g.text(font, Component.literal(shorten(chain + " → " + result.name(), Math.max(18, r.w() / 6))),
                textX, r.y() + 19, 0xFF9FB2CD);
        String progress = registered ? "직접 시전 등록 완료" : "실전 숙련 " + mastery + "/" + required;
        g.text(font, Component.literal(progress), textX, r.y() + 32,
                registered ? 0xFFD8B565 : 0xFFAA93BD);
        int barX = Math.max(textX + 100, r.right() - 98);
        int barW = Math.max(36, r.right() - barX - 9);
        g.fill(barX, r.y() + 34, barX + barW, r.y() + 39, 0xFF272C3B);
        g.fill(barX, r.y() + 34, barX + (int) (barW * Math.min(1.0, mastery / (double) required)),
                r.y() + 39, registered ? 0xFFFFD36B : color);
    }

    private void staffs(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c = l.content();
        drawManaStrip(g, l);
        int top = l.listTop();
        g.enableScissor(c.x(), top, c.right(), c.bottom() - 17);
        List<StaffProfile> profiles = ModItems.profiles();
        for (int i = 0; i < profiles.size(); i++) {
            Rect card = l.staffCard(i, contentScroll);
            drawStaffCard(g, card, profiles.get(i), inside(mouseX, mouseY, card));
        }
        g.disableScissor();
        footer(g, l, "지팡이는 주 손 우선, 없으면 보조 손의 효과가 적용됩니다");
    }

    private void drawStaffCard(GuiGraphicsExtractor g, Rect r, StaffProfile profile, boolean hover) {
        boolean equipped = profile.id().equals(ArcaneClientState.text("staff_id", "none"));
        int accent = staffColor(profile);
        g.fill(r.x(), r.y(), r.right(), r.bottom(), hover ? 0xFF29344B : 0xFF111827);
        g.fill(r.x(), r.y(), r.x() + 3, r.bottom(), equipped ? 0xFFFFD36B : accent);
        int iconX = r.x() + 24;
        int iconY = r.y() + r.h() / 2;
        ArcaneRenderUtil.fillCircle(g, iconX, iconY, 17, 0xFF070A13);
        ArcaneRenderUtil.ring(g, iconX, iconY, 17, equipped ? 0xFFFFD36B : accent);
        ArcaneRenderUtil.diamond(g, iconX, iconY - 4, 6, 0xFFF8EFFF);
        ArcaneRenderUtil.line(g, iconX, iconY + 2, iconX, iconY + 15, 0xFFF8EFFF);
        int tx = r.x() + 49;
        g.text(font, Component.literal(profile.displayName() + (equipped ? "  [장착]" : "")), tx, r.y() + 6,
                equipped ? 0xFFFFE1A0 : 0xFFEBDDF7);
        g.text(font, Component.literal(shorten(profile.summary(), Math.max(20, r.w() / 6))), tx, r.y() + 19, 0xFF9FB0C8);
        g.text(font, Component.literal(shorten(staffStats(profile), Math.max(20, r.w() / 6))), tx, r.y() + 32, 0xFFBFA6D6);
        g.text(font, Component.literal(shorten("제작: " + profile.recipeHint(), Math.max(20, r.w() / 6))), tx, r.y() + 45, 0xFF8C93A7);
    }

    private void core(GuiGraphicsExtractor g, Layout l, long time) {
        Rect c = l.content();
        int circle = ArcaneClientState.integer("circle", 1);
        boolean compact = c.w() < 540 || c.h() < 310;
        int centerX = compact ? l.cx() : l.cx();
        int centerY = compact ? c.y() + Math.min(88, c.h() / 3) : c.y() + c.h() / 2 + 5;
        int maxRadius = compact ? Math.max(36, Math.min(68, c.h() / 4))
                : Math.max(48, Math.min(c.w() / 6, c.h() / 2 - 26));

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

        List<String> coreLines = List.of(
                "최대 마력  " + ArcaneClientState.integer("max", 100),
                "현재 마력  " + ArcaneClientState.integer("mana", 0),
                "초당 회복  " + String.format("%.1f", ArcaneClientState.regenPerSecond()),
                "통찰  " + ArcaneClientState.integer("insight", 0)
                        + (ArcaneClientState.integer("next", 0) > 0 ? "/" + ArcaneClientState.integer("next", 0) : " (최대)"));
        List<String> staffLines = List.of(
                ArcaneClientState.text("staff", "맨손"),
                shorten(ArcaneClientState.text("staff_summary", "효과 없음"), 25),
                shorten(currentStaffStats(), 25),
                "1~5C 구현 · 6~9C 연구 중");

        if (!compact) {
            drawCorePanel(g, c.x() + 10, c.y() + 30, 170, "마력핵 상태", coreLines);
            drawCorePanel(g, c.right() - 180, c.y() + 30, 170, "지팡이 조율", staffLines);
        } else {
            int gap = 6;
            int panelW = Math.max(120, (c.w() - 18 - gap) / 2);
            int y = Math.min(c.bottom() - 90, centerY + maxRadius + 10);
            drawCorePanel(g, c.x() + 6, y, panelW, "마력핵 상태", coreLines);
            drawCorePanel(g, c.x() + 6 + panelW + gap, y, panelW, "지팡이 조율", staffLines);
        }
        footer(g, l, "저써클 숙련: 소모·쿨 감소 / 위력·범위 증가");
    }

    private void drawCorePanel(GuiGraphicsExtractor g, int x, int y, int w, String title, List<String> lines) {
        g.fill(x, y, x + w, y + 76, 0xD20A0E1B);
        g.fill(x, y, x + w, y + 2, 0xFF7F60AB);
        g.text(font, Component.literal(title), x + 7, y + 7, 0xFFE1CBF8);
        for (int i = 0; i < lines.size(); i++) {
            g.text(font, Component.literal(shorten(lines.get(i), Math.max(12, w / 6))),
                    x + 7, y + 23 + i * 12, 0xFFAEB9CF);
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
        g.centeredText(font, Component.literal("NINEFOLD ARCANA"), l.cx(), l.top() + 11, 0xFFF1DEFF);
        g.centeredText(font, Component.literal(dragging ? "화면 이동 중" : "상단을 드래그해 이동"),
                l.cx(), l.top() + 23, dragging ? 0xFFFFD36B : 0xFF71667F);
        for (int i = 0; i < TABS.size(); i++) {
            Rect tab = l.tab(i);
            boolean active = TABS.get(i).id().equals(page);
            boolean hover = inside(mouseX, mouseY, tab);
            g.centeredText(font, Component.literal(TABS.get(i).label()), tab.x() + tab.w() / 2, tab.y() + 5,
                    active ? 0xFFE7C9FF : hover ? 0xFFC7A9E8 : 0xFF746982);
            if (active) ArcaneRenderUtil.line(g, tab.x() + 7, tab.bottom() - 2, tab.right() - 7, tab.bottom() - 2, 0xFFB16EEE);
        }
        Rect close = l.close();
        ArcaneRenderUtil.diamond(g, close.x() + 9, close.y() + 9, 7,
                inside(mouseX, mouseY, close) ? 0xFFD86382 : 0xFF67384F);
        g.centeredText(font, Component.literal("×"), close.x() + 9, close.y() + 3, 0xFFFFFFFF);
    }

    private void footer(GuiGraphicsExtractor g, Layout l, String text) {
        g.centeredText(font, Component.literal(shorten(text, Math.max(30, l.panelW() / 6))),
                l.cx(), l.content().bottom() - 12, 0xFF929EB6);
    }

    private void drawNotice(GuiGraphicsExtractor g, Layout l) {
        if (notice.isBlank() || System.currentTimeMillis() > noticeUntil) return;
        int w = Math.min(l.panelW() - 28, Math.max(92, font.width(notice) + 20));
        int x = l.cx() - w / 2;
        int y = l.top() + 58;
        g.fill(x, y, x + w, y + 18, 0xEE120E20);
        g.fill(x, y, x + w, y + 1, 0xFFFFD36B);
        g.centeredText(font, Component.literal(notice), l.cx(), y + 5, 0xFFFFE9B8);
    }

    private void notice(String text) {
        notice = text;
        noticeUntil = System.currentTimeMillis() + 1800L;
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
        if (!ArcaneClientState.known().contains(spell.id())) {
            notice("아직 각인되지 않은 주문입니다");
            return;
        }
        if (spell.circle() > circle) {
            notice(spell.circle() + "써클 마력핵이 필요합니다");
            return;
        }
        ClientPacketDistributor.sendToServer(new EquipSpellPayload(spell.id(), savedActiveSlot));
        notice((savedActiveSlot + 1) + "번 슬롯에 " + spell.name() + " 장착");
    }

    private void request(String next) {
        SAVED_SCROLL.put(page, contentScroll);
        ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(next));
    }

    private int maxScroll(Layout l) {
        return switch (page) {
            case "recipes" -> l.maxRecipeScroll(SpellCatalog.fusions().size());
            case "staffs" -> l.maxStaffScroll(ModItems.profiles().size());
            case "atlas" -> l.maxAtlasScroll(visibleSpells().size());
            default -> 0;
        };
    }

    private void clampSavedOffset() {
        int panelW = Math.max(240, Math.min(820, width - 10));
        int panelH = Math.max(190, Math.min(470, height - 10));
        panelW = Math.min(panelW, Math.max(1, width - 8));
        panelH = Math.min(panelH, Math.max(1, height - 8));
        int baseLeft = (width - panelW) / 2;
        int baseTop = (height - panelH) / 2;
        savedOffsetX = clamp(savedOffsetX, 4 - baseLeft, width - 4 - panelW - baseLeft);
        savedOffsetY = clamp(savedOffsetY, 4 - baseTop, height - 4 - panelH - baseTop);
    }

    private Layout layout() {
        int panelW = Math.max(240, Math.min(820, width - 10));
        int panelH = Math.max(190, Math.min(470, height - 10));
        panelW = Math.min(panelW, Math.max(1, width - 8));
        panelH = Math.min(panelH, Math.max(1, height - 8));
        int left = clamp((width - panelW) / 2 + savedOffsetX, 4, Math.max(4, width - panelW - 4));
        int top = clamp((height - panelH) / 2 + savedOffsetY, 4, Math.max(4, height - panelH - 4));
        return new Layout(left, top, panelW, panelH);
    }

    private static int staffColor(StaffProfile profile) {
        return profile.favoredSchool() == null ? 0xFFFFC85C : ArcaneRenderUtil.schoolColor(profile.favoredSchool());
    }

    private static String staffStats(StaffProfile profile) {
        int cost = (int) Math.round((profile.manaCostMultiplier() - 1.0) * 100.0);
        int power = (int) Math.round((profile.powerMultiplier() - 1.0) * 100.0);
        int range = (int) Math.round((profile.rangeMultiplier() - 1.0) * 100.0);
        int cool = (int) Math.round((profile.cooldownMultiplier() - 1.0) * 100.0);
        return "MP" + signed(profile.maxManaBonus()) + " · 소모" + signed(cost) + "% · 위력"
                + signed(power) + "% · 범위" + signed(range) + "% · 쿨" + signed(cool) + "%";
    }

    private static String currentStaffStats() {
        int mana = ArcaneClientState.integer("staff_mana", 0);
        int cost = (int) Math.round((ArcaneClientState.staffMultiplier("staff_cost") - 1.0) * 100.0);
        int power = (int) Math.round((ArcaneClientState.staffMultiplier("staff_power") - 1.0) * 100.0);
        int range = (int) Math.round((ArcaneClientState.staffMultiplier("staff_range") - 1.0) * 100.0);
        return "MP" + signed(mana) + " · 소모" + signed(cost) + "% · 위력" + signed(power) + "% · 범위" + signed(range) + "%";
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static boolean inside(double x, double y, Rect r) {
        return x >= r.x() && y >= r.y() && x < r.right() && y < r.bottom();
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String page) {
        return "recipes".equals(page) || "staffs".equals(page) || "core".equals(page) ? page : "atlas";
    }

    private static String shorten(String value, int max) {
        if (value == null) return "";
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
            int tabW = Math.max(48, Math.min(96, (panelW - 20) / TABS.size()));
            int total = tabW * TABS.size();
            return new Rect(cx() - total / 2 + i * tabW, top + 33, tabW, 25);
        }
        Rect content() { return new Rect(left + 10, top + 62, Math.max(0, panelW - 20), Math.max(0, panelH - 75)); }
        int manaExtra() { return content().w() < 430 ? 16 : 0; }
        int slotTop() { return content().y() + 25 + manaExtra(); }
        int filterTop() { return slotTop() + slotSize() + 7; }
        int gridTop() { return filterTop() + 25; }
        Rect circleFilter(int circle) {
            Rect c = content();
            int count = SpellCatalog.IMPLEMENTED_MAX_CIRCLE + 1;
            int gap = c.w() < 420 ? 2 : 5;
            int width = Math.max(34, Math.min(62, (c.w() - gap * (count - 1)) / count));
            int total = width * count + gap * (count - 1);
            return new Rect(cx() - total / 2 + circle * (width + gap), filterTop(), width, 20);
        }
        int listTop() { return content().y() + 30 + manaExtra(); }
        int slotSize() {
            Rect c = content();
            int gap = c.w() < 500 ? 3 : 6;
            return Math.max(24, Math.min(c.w() < 500 ? 44 : 54, (c.w() - 12 - gap * 4) / 5));
        }
        Rect slot(int i) {
            Rect c = content();
            int gap = c.w() < 500 ? 3 : 6;
            int size = slotSize();
            int total = size * 5 + gap * 4;
            return new Rect(cx() - total / 2 + i * (size + gap), slotTop(), size, size);
        }
        int columns() { return content().w() >= 680 ? 4 : content().w() >= 450 ? 3 : content().w() >= 260 ? 2 : 1; }
        Rect spellHit(int index, int scroll) {
            Rect c = content();
            int cols = columns();
            int gap = 6;
            int cardW = Math.max(90, (c.w() - gap * (cols - 1)) / cols);
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
            return new Rect(c.x() + 3, listTop() + index * 50 - scroll, Math.max(0, c.w() - 6), 44);
        }
        int maxRecipeScroll(int count) {
            return Math.max(0, count * 50 - Math.max(40, content().bottom() - listTop() - 18));
        }
        int staffColumns() { return content().w() >= 560 ? 2 : 1; }
        Rect staffCard(int index, int scroll) {
            Rect c = content();
            int cols = staffColumns();
            int gap = 6;
            int cardW = Math.max(150, (c.w() - gap * (cols - 1)) / cols);
            int cardH = 58;
            int row = index / cols;
            int col = index % cols;
            return new Rect(c.x() + col * (cardW + gap), listTop() + row * (cardH + 6) - scroll, cardW, cardH);
        }
        int maxStaffScroll(int count) {
            int rows = (count + staffColumns() - 1) / staffColumns();
            int totalH = rows * 64;
            return Math.max(0, totalH - Math.max(40, content().bottom() - listTop() - 18));
        }
    }
}

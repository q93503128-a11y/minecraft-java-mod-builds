package kr.moonseungjun.senbonzakura.client;

import kr.moonseungjun.senbonzakura.ability.ShowcaseAbility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A dedicated ability loadout screen.
 *
 * Design language borrows the useful structure of spellbook/special-inventory mods:
 * a readable library, a persistent detail pane and a separate equipped belt.
 * It deliberately avoids the vanilla grey inventory-grid look.
 */
public final class SkillInventoryScreen extends Screen {
    private static final int OVERLAY = 0xB406080C;
    private static final int PANEL = 0xED10151D;
    private static final int PANEL_2 = 0xF018202A;
    private static final int CARD = 0xD918212C;
    private static final int CARD_HOVER = 0xED222E3C;
    private static final int CARD_SELECTED = 0xF12A3542;
    private static final int SLOT = 0xE10C1219;
    private static final int SLOT_HOVER = 0xF0202B37;
    private static final int LINE = 0xB74E6072;
    private static final int TEXT = 0xFFF3F6F8;
    private static final int MUTED = 0xFFA8B1BA;
    private static final int DIM = 0xFF68737F;
    private static final int GOLD = 0xFFFFC66D;

    private ShowcaseAbility selected;
    private int selectedSlot = -1;

    public SkillInventoryScreen() {
        super(Component.literal("스킬 인벤토리"));
        SkillLoadout.snapshot();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        drawFrame(graphics, layout);
        drawLibrary(graphics, layout, mouseX, mouseY);
        drawDetail(graphics, layout);
        drawSlots(graphics, layout, mouseX, mouseY);
        drawFooter(graphics, layout);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFrame(GuiGraphicsExtractor graphics, Layout l) {
        graphics.fill(l.left, l.top, l.right, l.bottom, PANEL);
        graphics.fill(l.left, l.top, l.right, l.top + 2, 0xFF6F8298);
        graphics.fill(l.left, l.top + 2, l.left + 2, l.bottom, 0xFF384654);
        graphics.fill(l.right - 2, l.top + 2, l.right, l.bottom, 0xFF27313C);
        graphics.fill(l.left, l.bottom - 2, l.right, l.bottom, 0xFF27313C);

        graphics.text(font, "ABILITY LOADOUT", l.left + 10, l.top + 8, GOLD, false);
        String help = "기술 선택 → 슬롯 지정   ·   우클릭 슬롯 해제   ·   Shift + 1~0 사용";
        graphics.text(font, fit(font, help, l.width() - 20), l.left + 10, l.top + 20, MUTED, false);
        graphics.fill(l.left + 10, l.top + 33, l.right - 10, l.top + 34, LINE);
    }

    private void drawLibrary(GuiGraphicsExtractor graphics, Layout l, int mouseX, int mouseY) {
        graphics.text(font, "SKILL LIBRARY", l.libraryLeft, l.libraryTop - 12, 0xFF8ED7FF, false);
        ShowcaseAbility[] abilities = ShowcaseAbility.values();
        for (int i = 0; i < abilities.length; i++) {
            ShowcaseAbility ability = abilities[i];
            Rect r = abilityRect(l, i);
            boolean hover = inside(mouseX, mouseY, r);
            boolean active = selected == ability;
            int accent = accent(ability);
            graphics.fill(r.x, r.y, r.right(), r.bottom(), active ? CARD_SELECTED : hover ? CARD_HOVER : CARD);
            graphics.fill(r.x, r.y, r.x + 3, r.bottom(), accent);
            if (active) {
                graphics.fill(r.x + 3, r.y, r.right(), r.y + 2, accent);
                graphics.fill(r.x + 3, r.bottom() - 2, r.right(), r.bottom(), accent);
            }

            drawGlyph(graphics, ability, r.x + 15, r.y + r.h / 2, accent);
            graphics.text(font, fit(font, ability.displayName(), r.w - 36), r.x + 29, r.y + 7, TEXT, false);
            String sub = typeLabel(ability);
            graphics.text(font, fit(font, sub, r.w - 36), r.x + 29, r.y + 19, hover || active ? accent : MUTED, false);

            int equipped = SkillLoadout.slotOf(ability);
            if (equipped >= 0) {
                String key = slotLabel(equipped);
                int badgeW = font.width(key) + 8;
                graphics.fill(r.right() - badgeW - 4, r.y + 4, r.right() - 4, r.y + 15, 0xD70B1016);
                graphics.centeredText(font, key, r.right() - 4 - badgeW / 2, r.y + 6, GOLD);
            }
        }
    }

    private void drawDetail(GuiGraphicsExtractor graphics, Layout l) {
        Rect r = l.detail;
        graphics.fill(r.x, r.y, r.right(), r.bottom(), PANEL_2);
        int accent = selected == null ? 0xFF6D7B88 : accent(selected);
        graphics.fill(r.x, r.y, r.x + 3, r.bottom(), accent);

        if (selected == null) {
            graphics.text(font, "기술을 선택하세요", r.x + 10, r.y + 8, TEXT, false);
            graphics.text(font, fit(font,
                    "왼쪽 카드에서 기술을 고른 뒤 아래 10개 슬롯 중 하나를 누르면 장착됩니다.", r.w - 20),
                    r.x + 10, r.y + 21, MUTED, false);
            return;
        }

        graphics.text(font, selected.displayName(), r.x + 10, r.y + 7, accent, false);
        String meta = typeLabel(selected) + "  ·  재사용 " + formatSeconds(selected.cooldownTicks()) + "초";
        graphics.text(font, fit(font, meta, r.w - 20), r.x + 10, r.y + 19, MUTED, false);
        graphics.text(font, fit(font, description(selected), r.w - 20), r.x + 10, r.y + 32, TEXT, false);

        int current = SkillLoadout.slotOf(selected);
        String state = current >= 0
                ? "현재 " + slotLabel(current) + " 슬롯에 장착됨"
                : "아래 슬롯을 클릭해 장착";
        graphics.text(font, fit(font, state, r.w - 20), r.x + 10, r.bottom() - 12,
                current >= 0 ? GOLD : DIM, false);
    }

    private void drawSlots(GuiGraphicsExtractor graphics, Layout l, int mouseX, int mouseY) {
        graphics.text(font, "EQUIPPED · SHIFT + 1~0", l.slotLeft, l.slotTop - 12, GOLD, false);
        ShowcaseAbility[] equipped = SkillLoadout.snapshot();
        for (int i = 0; i < SkillLoadout.SLOT_COUNT; i++) {
            Rect r = slotRect(l, i);
            boolean hover = inside(mouseX, mouseY, r);
            ShowcaseAbility ability = equipped[i];
            int accent = ability == null ? LINE : accent(ability);

            graphics.fill(r.x, r.y, r.right(), r.bottom(), hover ? SLOT_HOVER : SLOT);
            graphics.fill(r.x, r.y, r.right(), r.y + 2, accent);
            if (selectedSlot == i) {
                graphics.fill(r.x, r.bottom() - 2, r.right(), r.bottom(), GOLD);
                graphics.fill(r.x, r.y + 2, r.x + 2, r.bottom() - 2, GOLD);
                graphics.fill(r.right() - 2, r.y + 2, r.right(), r.bottom() - 2, GOLD);
            }

            String key = slotLabel(i);
            graphics.text(font, key, r.x + 4, r.y + 4, ability == null ? DIM : GOLD, false);

            if (ability != null) {
                drawGlyph(graphics, ability, r.x + r.w / 2, r.y + 22, accent);
                String shortName = shortName(ability);
                graphics.centeredText(font, fit(font, shortName, r.w - 6), r.x + r.w / 2, r.bottom() - 12, TEXT);
            } else {
                graphics.centeredText(font, "—", r.x + r.w / 2, r.y + 20, DIM);
                graphics.centeredText(font, "EMPTY", r.x + r.w / 2, r.bottom() - 12, DIM);
            }
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, Layout l) {
        String left = selected == null
                ? "카드를 클릭하면 기술 정보가 표시됩니다."
                : selected.displayName() + " 선택됨";
        graphics.text(font, fit(font, left, l.width() - 120), l.left + 10, l.bottom - 12, MUTED, false);
        String close = "ESC 닫기";
        graphics.text(font, close, l.right - font.width(close) - 10, l.bottom - 12, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        Layout l = layout();
        int button = click.button();

        for (int i = 0; i < ShowcaseAbility.values().length; i++) {
            Rect r = abilityRect(l, i);
            if (!inside(click.x(), click.y(), r)) continue;
            ShowcaseAbility ability = ShowcaseAbility.values()[i];
            selected = ability;
            selectedSlot = SkillLoadout.slotOf(ability);
            if (button == 0 && doubled) {
                int current = SkillLoadout.slotOf(ability);
                int target = current >= 0 ? current : SkillLoadout.firstEmpty();
                if (target < 0) target = 0;
                SkillLoadout.set(target, ability);
                selectedSlot = target;
            }
            return true;
        }

        for (int i = 0; i < SkillLoadout.SLOT_COUNT; i++) {
            Rect r = slotRect(l, i);
            if (!inside(click.x(), click.y(), r)) continue;
            if (button == 1) {
                SkillLoadout.clear(i);
                if (selectedSlot == i) selectedSlot = -1;
                return true;
            }
            if (button == 0) {
                if (selected != null) {
                    SkillLoadout.set(i, selected);
                    selectedSlot = i;
                } else {
                    selected = SkillLoadout.get(i);
                    selectedSlot = i;
                }
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private Layout layout() {
        int margin = Math.max(8, Math.min(18, width / 28));
        int w = Math.min(620, Math.max(280, width - margin * 2));
        int h = Math.min(330, Math.max(176, height - margin * 2));
        int left = (width - w) / 2;
        int top = (height - h) / 2;
        int right = left + w;
        int bottom = top + h;

        int contentTop = top + 49;
        int footerH = 18;
        int slotsH = Math.max(46, Math.min(58, h / 5));
        int slotTop = bottom - footerH - slotsH - 15;
        int detailH = Math.max(48, Math.min(62, h / 4));
        int detailTop = slotTop - detailH - 18;
        int libraryBottom = detailTop - 7;
        int libraryH = Math.max(58, libraryBottom - contentTop);

        int libraryLeft = left + 10;
        int libraryRight = right - 10;
        int gap = 5;
        int cols = w < 370 ? 2 : 4;
        int rows = (ShowcaseAbility.values().length + cols - 1) / cols;
        int cardW = Math.max(74, (libraryRight - libraryLeft - gap * (cols - 1)) / cols);
        int cardH = Math.max(30, Math.min(46, (libraryH - gap * Math.max(0, rows - 1)) / Math.max(1, rows)));

        Rect detail = new Rect(left + 10, detailTop, w - 20, detailH);
        int slotLeft = left + 10;
        int slotRight = right - 10;
        int slotGap = 3;
        int slotW = Math.max(22, (slotRight - slotLeft - slotGap * 9) / 10);

        return new Layout(left, top, right, bottom, libraryLeft, contentTop,
                cols, cardW, cardH, gap, detail, slotLeft, slotTop, slotW, slotsH, slotGap);
    }

    private Rect abilityRect(Layout l, int index) {
        int col = index % l.libraryColumns;
        int row = index / l.libraryColumns;
        return new Rect(
                l.libraryLeft + col * (l.cardW + l.cardGap),
                l.libraryTop + row * (l.cardH + l.cardGap),
                l.cardW,
                l.cardH);
    }

    private Rect slotRect(Layout l, int slot) {
        return new Rect(l.slotLeft + slot * (l.slotW + l.slotGap), l.slotTop, l.slotW, l.slotH);
    }

    private static int accent(ShowcaseAbility ability) {
        return switch (ability) {
            case SKYFALL -> 0xFF90C7FF;
            case WORLD_DIVIDE -> 0xFFE6ECFF;
            case BLACK_SUN -> 0xFFB685F5;
            case SWORD_GRAVE -> 0xFFCAD2DB;
            case GRAVITY_REVERSAL -> 0xFF79E0D1;
            case LAST_SECOND -> 0xFFFFD27A;
            case HEAVEN_JUDGMENT -> 0xFF9FE9FF;
            case STELLAR_LANCE -> 0xFFFFA76D;
        };
    }

    private static String typeLabel(ShowcaseAbility ability) {
        return switch (ability) {
            case SKYFALL -> "강하 · 범위";
            case WORLD_DIVIDE -> "절단 · 직선";
            case BLACK_SUN -> "흡인 · 폭발";
            case SWORD_GRAVE -> "소환 · 광역";
            case GRAVITY_REVERSAL -> "제어 · 낙하";
            case LAST_SECOND -> "정지 · 지연";
            case HEAVEN_JUDGMENT -> "낙뢰 · 광역";
            case STELLAR_LANCE -> "관통 · 후폭발";
        };
    }

    private static String description(ShowcaseAbility ability) {
        return switch (ability) {
            case SKYFALL -> "하늘의 균열에서 초거대 무기가 강하해 전방 지면을 붕괴시킨다.";
            case WORLD_DIVIDE -> "베인 공간이 잠시 정지한 뒤 어긋나며 긴 절단 영역 전체가 한꺼번에 파쇄된다.";
            case BLACK_SUN -> "상공의 암흑핵이 주변을 끌어당기고 극도로 압축된 뒤 구형 충격으로 붕괴한다.";
            case SWORD_GRAVE -> "전장을 거대 검의 묘지로 바꾸고 떠오른 검들을 광역으로 동시 낙하시킨다.";
            case GRAVITY_REVERSAL -> "적과 잔해를 천천히 들어 올린 뒤 중력을 되돌려 지면으로 강제 추락시킨다.";
            case LAST_SECOND -> "주변 시간을 묶어 참격을 축적하고 시간이 재개되는 순간 모든 판정을 동시에 터뜨린다.";
            case HEAVEN_JUDGMENT -> "상공에 응축된 전격이 굵은 본류와 가지 번개로 지면을 관통한다.";
            case STELLAR_LANCE -> "거대한 에너지를 작은 창으로 압축해 관통시키고 지나간 경로를 늦게 연쇄 폭발시킨다.";
        };
    }

    private static String shortName(ShowcaseAbility ability) {
        return switch (ability) {
            case SKYFALL -> "천락";
            case WORLD_DIVIDE -> "공간";
            case BLACK_SUN -> "흑일";
            case SWORD_GRAVE -> "검묘";
            case GRAVITY_REVERSAL -> "역천";
            case LAST_SECOND -> "시간";
            case HEAVEN_JUDGMENT -> "백뢰";
            case STELLAR_LANCE -> "성창";
        };
    }

    private static String slotLabel(int slot) {
        return slot == 9 ? "0" : Integer.toString(slot + 1);
    }

    private static String formatSeconds(int ticks) {
        if (ticks % 20 == 0) return Integer.toString(ticks / 20);
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private static void drawGlyph(GuiGraphicsExtractor g, ShowcaseAbility ability, int cx, int cy, int color) {
        int dark = 0xD20A0F15;
        g.fill(cx - 7, cy - 7, cx + 8, cy + 8, dark);
        switch (ability) {
            case SKYFALL -> {
                g.fill(cx - 1, cy - 6, cx + 2, cy + 4, color);
                g.fill(cx - 4, cy + 1, cx + 5, cy + 3, color);
                g.fill(cx - 2, cy + 4, cx + 3, cy + 7, color);
            }
            case WORLD_DIVIDE -> {
                for (int i = -5; i <= 5; i++) g.fill(cx + i, cy - i - 1, cx + i + 2, cy - i + 1, color);
            }
            case BLACK_SUN -> {
                g.fill(cx - 5, cy - 3, cx + 6, cy + 4, color);
                g.fill(cx - 3, cy - 5, cx + 4, cy + 6, color);
                g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF080A0E);
            }
            case SWORD_GRAVE -> {
                for (int x : new int[]{-5, 0, 5}) {
                    g.fill(cx + x - 1, cy - 5, cx + x + 1, cy + 5, color);
                    g.fill(cx + x - 2, cy + 3, cx + x + 2, cy + 4, color);
                }
            }
            case GRAVITY_REVERSAL -> {
                g.fill(cx - 1, cy - 6, cx + 2, cy + 6, color);
                g.fill(cx - 4, cy - 4, cx + 5, cy - 2, color);
                g.fill(cx - 4, cy + 2, cx + 5, cy + 4, color);
            }
            case LAST_SECOND -> {
                g.fill(cx - 5, cy - 5, cx + 6, cy - 4, color);
                g.fill(cx - 5, cy + 4, cx + 6, cy + 5, color);
                g.fill(cx - 5, cy - 5, cx - 4, cy + 5, color);
                g.fill(cx + 5, cy - 5, cx + 6, cy + 5, color);
                g.fill(cx, cy - 3, cx + 1, cy + 1, color);
                g.fill(cx, cy, cx + 4, cy + 1, color);
            }
            case HEAVEN_JUDGMENT -> {
                g.fill(cx, cy - 6, cx + 3, cy - 1, color);
                g.fill(cx - 3, cy - 1, cx + 2, cy + 2, color);
                g.fill(cx - 1, cy + 1, cx + 2, cy + 6, color);
            }
            case STELLAR_LANCE -> {
                g.fill(cx - 6, cy - 1, cx + 5, cy + 2, color);
                g.fill(cx + 3, cy - 3, cx + 7, cy + 4, color);
                g.fill(cx - 5, cy - 4, cx - 4, cy + 5, color);
            }
        }
    }

    private static String fit(Font font, String value, int maxWidth) {
        if (value == null || maxWidth <= 0) return "";
        String normalized = value.replace('\n', ' ');
        if (font.width(normalized) <= maxWidth) return normalized;
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + "…") > maxWidth) end--;
        return normalized.substring(0, end) + "…";
    }

    private static boolean inside(double x, double y, Rect r) {
        return x >= r.x && x < r.right() && y >= r.y && y < r.bottom();
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; }
        int bottom() { return y + h; }
    }

    private record Layout(
            int left, int top, int right, int bottom,
            int libraryLeft, int libraryTop, int libraryColumns,
            int cardW, int cardH, int cardGap,
            Rect detail,
            int slotLeft, int slotTop, int slotW, int slotH, int slotGap) {
        int width() { return right - left; }
    }
}

package dev.moonseungjun.augmentationsurvival.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.client.AugmentIconClient;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Focused augment selection/collection/synthesis UI. Gameplay state remains server-authoritative. */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 660, H = 338;
    private static final int BG = 0xF20A0E12, SURFACE = 0xFF11171C, SURFACE_HI = 0xFF172027;
    private static final int LINE = 0xFF37434B, LINE_HI = 0xFF8797A1;
    private static final int TEXT = 0xFFF3F6F8, MUTED = 0xFF909CA3, FAINT = 0xFF616B71;
    private static final int GOOD = 0xFF79D496, BAD = 0xFFE67F86;
    private static final int CARD_W = 194, CARD_H = 226, CARD_GAP = 15;
    private static final int OWNED_PER_PAGE = 18;

    private View view = View.SELECT;
    private int ownedPage;
    private int synthesisTier;
    private long revealStarted = Long.MIN_VALUE;
    private int observedCandidateKey = Integer.MIN_VALUE;
    private String hoveredOwnedId = "";

    public AugmentDeviceScreen(AugmentDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int l = leftPos, t = topPos;
        g.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), 0x8B000000);
        g.fill(l, t, l + W, t + H, BG);
        g.outline(l, t, W, H, 0xFF4A5760);
        g.fill(l + 1, t + 1, l + W - 1, t + 31, 0xFF151D23);
        g.fill(l + 2, t + 2, l + W - 2, t + 3, 0xFF83939D);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        syncReveal();
        int mx = mouseX - leftPos, my = mouseY - topPos;
        boolean choosing = menu.status() == AugmentDeviceMenu.STATUS_READY;
        g.text(font, choosing ? "증강 동조" : title.getString(), 14, 11, TEXT, false);
        if (choosing) {
            drawOffer(g, mx, my);
            return;
        }
        drawTabs(g, mx, my);
        switch (view) {
            case SELECT -> drawTierSelect(g, mx, my);
            case OWNED -> drawOwned(g, mx, my);
            case SYNTHESIS -> drawSynthesis(g, mx, my);
        }
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my) {
        int x = 226;
        drawTab(g, x, 9, 72, "동조", view == View.SELECT, mx, my);
        drawTab(g, x + 76, 9, 88, "보유 증강", view == View.OWNED, mx, my);
        if (menu.advanced()) drawTab(g, x + 168, 9, 88, "코어 합성", view == View.SYNTHESIS, mx, my);
    }

    private void drawTierSelect(GuiGraphicsExtractor g, int mx, int my) {
        centered(g, "동조할 코어 등급을 선택하세요", W / 2, 53, TEXT);
        centered(g, "코어 하나가 세 후보를 고정합니다 · 화면을 닫아도 후보는 바뀌지 않습니다", W / 2, 70, MUTED);
        int startX = 25, y = 103, cellW = 94, gap = 10;
        for (AugmentTier tier : AugmentTier.values()) {
            int x = startX + tier.ordinal() * (cellW + gap);
            drawTierChoice(g, x, y, cellW, 112, tier, mx, my);
        }
        centered(g, "획득한 증강과 현재 레벨은 ‘보유 증강’에서 확인할 수 있습니다.", W / 2, 240, FAINT);
        String state = switch (menu.status()) {
            case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다.";
            case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 증강을 모두 완성했습니다.";
            case AugmentDeviceMenu.STATUS_ACQUIRED -> "동조 완료 · 획득 정보가 보유 증강에 반영되었습니다.";
            case AugmentDeviceMenu.STATUS_INVALID -> "진행 중인 동조를 먼저 완료하세요.";
            default -> "";
        };
        if (!state.isBlank()) centered(g, state, W / 2, 266,
                menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED ? GOOD : BAD);
    }

    private void drawTierChoice(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentTier tier, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int color = tierColor(tier);
        g.fill(x, y, x + w, y + h, hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + w, y + 2, color);
        if (hover) g.outline(x, y, w, h, brighten(color, 36));
        g.item(new ItemStack(AugmentCoreItems.item(tier)), x + w / 2 - 8, y + 21);
        centered(g, tierLabel(tier), x + w / 2, y + 48, hover ? TEXT : 0xFFD3DADF);
        int count = coreCount(tier);
        centered(g, "보유 " + count, x + w / 2, y + 66, count > 0 ? MUTED : BAD);
        centered(g, count > 0 ? "동조 시작" : "코어 필요", x + w / 2, y + 89, count > 0 ? color : FAINT);
        if (hover && count > 0) g.fill(x + 17, y + h - 6, x + w - 17, y + h - 4, color);
    }

    private void drawOffer(GuiGraphicsExtractor g, int mx, int my) {
        AugmentTier selectedTier = menu.tierOrdinal() >= 0 ? AugmentTier.values()[menu.tierOrdinal()] : AugmentTier.COMMON;
        g.text(font, tierLabel(selectedTier) + " 코어", 14, 37, tierColor(selectedTier), false);
        String hint = menu.selectionCount() == 2 ? "서로 다른 증강 두 개를 선택" : "한 가지 증강을 선택";
        g.text(font, hint, W - 14 - font.width(hint), 37, MUTED, false);
        int y = 60;
        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition d = menu.candidate(slot);
            if (d == null) continue;
            int x = 24 + slot * (CARD_W + CARD_GAP);
            drawAugmentCard(g, x, y, CARD_W, CARD_H, slot, d, mx, my);
        }
        AugmentDefinition hover = hoveredCandidate(mx, my, y);
        if (hover != null && shiftDown()) {
            drawDetailedStrip(g, hover, 16, H - 45, W - 32, menu.currentLevel(candidateSlotFor(hover)));
        } else {
            centered(g, hover != null ? "Shift: 정확한 수치 보기" : statusLine(), W / 2, H - 22, MUTED);
        }
    }

    private void drawAugmentCard(GuiGraphicsExtractor g, int x, int y, int w, int h, int slot,
                                 AugmentDefinition d, int mx, int my) {
        double reveal = revealProgress(slot);
        y += (int) Math.round((1.0 - reveal) * 12.0);
        boolean hover = inside(mx, my, x, y, w, h);
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        int color = tierColor(d.tier());
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);

        g.fill(x, y, x + w, y + h, 0xFF080C10);
        g.outline(x, y, w, h, selected ? brighten(color, 54) : hover ? color : 0xFF3A444A);
        g.fill(x + 1, y + 1, x + w - 1, y + 5, color);
        g.fill(x + 1, y + 6, x + w - 1, y + 31, tint(0xFF11171C, color, 10));
        g.text(font, tierLabel(d.tier()), x + 10, y + 13, color, false);
        String kind = p.kind().label();
        g.text(font, kind, x + w - 10 - font.width(kind), y + 13,
                p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? MUTED : 0xFFF0D47A, false);

        g.fill(x + w / 2 - 25, y + 43, x + w / 2 + 25, y + 93, 0xFF0D1318);
        if (hover) g.outline(x + w / 2 - 25, y + 43, 50, 50, shade(color, 0.75));
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()), x + w / 2 - 8, y + 60);
        centered(g, p.displayName(), x + w / 2, y + 104, TEXT);

        List<String> lines = wrap(p.summary(), w - 26);
        for (int i = 0; i < Math.min(5, lines.size()); i++) {
            centered(g, lines.get(i), x + w / 2, y + 128 + i * 11, 0xFFC5CDD2);
        }
        int now = menu.currentLevel(slot), next = Math.min(now + 1, d.maxLevel());
        g.fill(x + 12, y + h - 37, x + w - 12, y + h - 36, 0xFF2B343A);
        String level = now == 0 ? "획득 Lv." + next : "Lv." + now + " → " + next;
        g.text(font, level, x + 12, y + h - 26, MUTED, false);
        String max = "최대 " + d.maxLevel();
        g.text(font, max, x + w - 12 - font.width(max), y + h - 26, FAINT, false);
        if (selected) centered(g, "선택됨", x + w / 2, y + h - 12, brighten(color, 65));
        else if (hover) centered(g, "클릭하여 선택", x + w / 2, y + h - 12, color);
    }

    private void drawOwned(GuiGraphicsExtractor g, int mx, int my) {
        List<AugmentDefinition> owned = ownedDefinitions();
        int pages = Math.max(1, (owned.size() + OWNED_PER_PAGE - 1) / OWNED_PER_PAGE);
        ownedPage = Math.max(0, Math.min(ownedPage, pages - 1));
        g.text(font, "보유 증강 " + owned.size() + " / 146", 18, 48, TEXT, false);
        String help = "마우스: 설명 · Shift: 정확한 수치";
        g.text(font, help, W - 18 - font.width(help), 48, MUTED, false);
        hoveredOwnedId = "";

        int start = ownedPage * OWNED_PER_PAGE;
        int end = Math.min(owned.size(), start + OWNED_PER_PAGE);
        int startX = 18, startY = 68, cellW = 202, cellH = 31, gapX = 8, gapY = 5;
        for (int i = start; i < end; i++) {
            int local = i - start, col = local % 3, row = local / 3;
            AugmentDefinition d = owned.get(i);
            drawOwnedCell(g, startX + col * (cellW + gapX), startY + row * (cellH + gapY), cellW, cellH, d, mx, my);
        }
        if (owned.isEmpty()) centered(g, "아직 획득한 증강이 없습니다.", W / 2, 150, MUTED);
        if (pages > 1) drawPager(g, W / 2, 289, ownedPage, pages, mx, my);
        if (!hoveredOwnedId.isBlank()) {
            AugmentDefinition d = AugmentCatalog.require(hoveredOwnedId);
            int level = ClientAugmentProgressState.level(d.id());
            if (shiftDown()) drawDetailedStrip(g, d, 18, H - 45, W - 36, level);
            else drawSummaryStrip(g, d, 18, H - 45, W - 36, level);
        }
    }

    private void drawOwnedCell(GuiGraphicsExtractor g, int x, int y, int w, int h, AugmentDefinition d, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int color = tierColor(d.tier());
        g.fill(x, y, x + w, y + h, hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + 2, y + h, color);
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()), x + 7, y + 8);
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        g.text(font, clip(p.displayName(), w - 72), x + 29, y + 6, hover ? TEXT : 0xFFD3DADE, false);
        int level = ClientAugmentProgressState.level(d.id());
        g.text(font, "Lv." + level + "/" + d.maxLevel(), x + 29, y + 18, MUTED, false);
        String kind = p.kind().label();
        g.text(font, kind, x + w - 8 - font.width(kind), y + 11,
                p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? FAINT : 0xFFE8CF78, false);
        if (hover) hoveredOwnedId = d.id();
    }

    private void drawPager(GuiGraphicsExtractor g, int centerX, int y, int page, int pages, int mx, int my) {
        boolean left = page > 0 && inside(mx, my, centerX - 58, y - 5, 28, 20);
        boolean right = page + 1 < pages && inside(mx, my, centerX + 30, y - 5, 28, 20);
        g.text(font, "‹", centerX - 50, y, left ? TEXT : FAINT, false);
        String state = (page + 1) + " / " + pages;
        centered(g, state, centerX, y, MUTED);
        g.text(font, "›", centerX + 42, y, right ? TEXT : FAINT, false);
    }

    private void drawSynthesis(GuiGraphicsExtractor g, int mx, int my) {
        centered(g, "같은 등급 코어 3개를 다음 등급 코어 1개로 정제합니다.", W / 2, 54, MUTED);
        int y = 77;
        for (int i = 0; i < AugmentTier.values().length - 1; i++) {
            AugmentTier tier = AugmentTier.values()[i];
            int x = 109 + i * 89;
            boolean selected = synthesisTier == i, hover = inside(mx, my, x, y, 78, 28);
            g.fill(x, y, x + 78, y + 28, selected || hover ? SURFACE_HI : SURFACE);
            g.fill(x, y + 26, x + 78, y + 28, selected ? tierColor(tier) : 0xFF343D43);
            centered(g, tierLabel(tier), x + 39, y + 9, selected ? TEXT : MUTED);
        }
        AugmentTier from = AugmentTier.values()[synthesisTier];
        AugmentTier to = from.next().orElse(AugmentTier.MYTHIC);
        int cy = 163;
        for (int i = 0; i < 3; i++) g.item(new ItemStack(AugmentCoreItems.item(from)), 174 + i * 23, cy);
        g.text(font, "×3", 241, cy + 5, MUTED, false);
        centered(g, "→", W / 2, cy + 5, tierColor(to));
        g.item(new ItemStack(AugmentCoreItems.item(to)), 411, cy);
        g.text(font, "×1", 433, cy + 5, MUTED, false);
        centered(g, tierLabel(from) + "  →  " + tierLabel(to), W / 2, 196, TEXT);
        centered(g, "보유 " + coreCount(from), W / 2, 214, coreCount(from) >= 3 ? MUTED : BAD);
        drawButton(g, 237, 242, 82, 26, "1회 정제", mx, my);
        drawButton(g, 341, 242, 82, 26, "모두 정제", mx, my);
        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED) centered(g, "정제 완료 ×" + Math.max(1, menu.lastOutputCount()), W / 2, 289, GOOD);
        else if (menu.status() == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES) centered(g, "코어가 부족합니다.", W / 2, 289, BAD);
    }

    private void drawSummaryStrip(GuiGraphicsExtractor g, AugmentDefinition d, int x, int y, int w, int level) {
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        g.fill(x, y, x + w, y + 34, 0xE30D1216);
        g.fill(x, y, x + 3, y + 34, tierColor(d.tier()));
        g.text(font, p.displayName() + "  Lv." + level, x + 11, y + 6, TEXT, false);
        g.text(font, clip(p.summary(), w - 22), x + 11, y + 19, 0xFFC7D0D5, false);
    }

    private void drawDetailedStrip(GuiGraphicsExtractor g, AugmentDefinition d, int x, int y, int w, int level) {
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        g.fill(x, y - 16, x + w, y + 36, 0xF20B1014);
        g.fill(x, y - 16, x + 3, y + 36, tierColor(d.tier()));
        g.text(font, p.displayName() + "  Lv." + level + "/" + d.maxLevel(), x + 11, y - 10, TEXT, false);
        List<String> lines = wrap(p.detail(), w - 22);
        for (int i = 0; i < Math.min(3, lines.size()); i++) g.text(font, lines.get(i), x + 11, y + 3 + i * 10, 0xFFD2D9DD, false);
    }

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean active, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 20);
        int color = active ? 0xFFC7D3D9 : hover ? LINE_HI : MUTED;
        g.text(font, text, x + (w - font.width(text)) / 2, y + 5, color, false);
        if (active) g.fill(x + 10, y + 18, x + w - 10, y + 20, 0xFF8FA3AE);
    }

    private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? SURFACE_HI : SURFACE);
        g.outline(x, y, w, h, hover ? LINE_HI : LINE);
        centered(g, text, x + w / 2, y + 9, hover ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double x = event.x() - leftPos, y = event.y() - topPos;
        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            for (int slot = 0; slot < 3; slot++) {
                int cx = 24 + slot * (CARD_W + CARD_GAP);
                if (menu.candidate(slot) != null && inside(x, y, cx, 60, CARD_W, CARD_H)) {
                    playClick(1.05F + slot * 0.04F);
                    return send(AugmentDeviceMenu.candidateButtonId(slot));
                }
            }
            return true;
        }
        if (inside(x, y, 226, 9, 72, 20)) { view = View.SELECT; playClick(1.0F); return true; }
        if (inside(x, y, 302, 9, 88, 20)) { view = View.OWNED; playClick(1.04F); return true; }
        if (menu.advanced() && inside(x, y, 394, 9, 88, 20)) { view = View.SYNTHESIS; playClick(1.08F); return true; }

        if (view == View.SELECT) {
            int startX = 25, yy = 103, cellW = 94, gap = 10;
            for (AugmentTier tier : AugmentTier.values()) {
                int xx = startX + tier.ordinal() * (cellW + gap);
                if (inside(x, y, xx, yy, cellW, 112) && coreCount(tier) > 0) {
                    playClick(0.92F + tier.ordinal() * 0.04F);
                    return send(AugmentDeviceMenu.beginButtonId(tier));
                }
            }
        } else if (view == View.OWNED) {
            List<AugmentDefinition> owned = ownedDefinitions();
            int pages = Math.max(1, (owned.size() + OWNED_PER_PAGE - 1) / OWNED_PER_PAGE);
            if (inside(x, y, W / 2 - 58, 284, 28, 20) && ownedPage > 0) {
                ownedPage--;
                playClick(0.96F);
                return true;
            }
            if (inside(x, y, W / 2 + 30, 284, 28, 20) && ownedPage + 1 < pages) {
                ownedPage++;
                playClick(1.04F);
                return true;
            }
        } else if (view == View.SYNTHESIS && menu.advanced()) {
            for (int i = 0; i < AugmentTier.values().length - 1; i++) {
                int xx = 109 + i * 89;
                if (inside(x, y, xx, 77, 78, 28)) { synthesisTier = i; playClick(0.95F + i * 0.05F); return true; }
            }
            AugmentTier tier = AugmentTier.values()[synthesisTier];
            if (inside(x, y, 237, 242, 82, 26)) return send(AugmentDeviceMenu.upgradeOneButtonId(tier));
            if (inside(x, y, 341, 242, 82, 26)) return send(AugmentDeviceMenu.upgradeAllButtonId(tier));
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean send(int id) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!menu.clickMenuButton(minecraft.player, id)) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }

    private void syncReveal() {
        int key = 17;
        for (int i = 0; i < 3; i++) {
            AugmentDefinition d = menu.candidate(i);
            key = 31 * key + (d == null ? 0 : d.id().hashCode());
        }
        if (key != observedCandidateKey && menu.status() == AugmentDeviceMenu.STATUS_READY) revealStarted = System.currentTimeMillis();
        observedCandidateKey = key;
    }

    private double revealProgress(int slot) {
        if (revealStarted == Long.MIN_VALUE) return 1.0;
        long age = System.currentTimeMillis() - revealStarted - slot * 90L;
        if (age <= 0) return 0.0;
        double p = Math.max(0.0, Math.min(1.0, age / 260.0));
        double inv = 1.0 - p;
        return 1.0 - inv * inv * inv;
    }

    private List<AugmentDefinition> ownedDefinitions() {
        List<AugmentDefinition> owned = new ArrayList<>();
        for (String id : ClientAugmentProgressState.levels().keySet()) {
            try { owned.add(AugmentCatalog.require(id)); } catch (RuntimeException ignored) { }
        }
        owned.sort(Comparator.comparing((AugmentDefinition d) -> d.tier().ordinal())
                .thenComparing(d -> AugmentPresentationCatalog.get(d).displayName()));
        return owned;
    }
    private AugmentDefinition hoveredCandidate(int mx, int my, int y) {
        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition d = menu.candidate(slot);
            if (d != null && inside(mx, my, 24 + slot * (CARD_W + CARD_GAP), y, CARD_W, CARD_H)) return d;
        }
        return null;
    }
    private int candidateSlotFor(AugmentDefinition definition) {
        for (int i = 0; i < 3; i++) if (definition.equals(menu.candidate(i))) return i;
        return 0;
    }
    private String statusLine() {
        return menu.selectionCount() == 2 ? "서로 다른 증강 두 개를 선택하세요" : "원하는 증강을 선택하세요";
    }
    private int coreCount(AugmentTier tier) {
        if (minecraft == null || minecraft.player == null) return 0;
        int count = 0;
        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(AugmentCoreItems.item(tier))) count += stack.getCount();
        }
        return count;
    }
    private boolean shiftDown() {
        return minecraft != null && (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
    }
    private void playClick(float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch));
    }
    private List<String> wrap(String value, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) <= maxWidth) line = new StringBuilder(candidate);
            else {
                if (line.length() > 0) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }
    private String clip(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String result = value;
        while (result.length() > 1 && font.width(result + "…") > maxWidth) result = result.substring(0, result.length() - 1);
        return result + "…";
    }
    private void centered(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, value, x - font.width(value) / 2, y, color, false);
    }
    private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
    private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 255) + amount), gg = Math.min(255, ((color >> 8) & 255) + amount), b = Math.min(255, (color & 255) + amount);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    private static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor), gg = (int) (((color >> 8) & 255) * factor), b = (int) ((color & 255) * factor);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    private static int tint(int base, int color, int percent) {
        int inv = 100 - percent;
        int r = (((base >> 16) & 255) * inv + ((color >> 16) & 255) * percent) / 100;
        int gg = (((base >> 8) & 255) * inv + ((color >> 8) & 255) * percent) / 100;
        int b = ((base & 255) * inv + (color & 255) * percent) / 100;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    private static int tierColor(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> 0xFFB8BEC5; case UNCOMMON -> 0xFF4FC77F; case RARE -> 0xFF4394E4;
            case EPIC -> 0xFFAA58D4; case LEGENDARY -> 0xFFE4A934; case MYTHIC -> 0xFFE54C76;
        };
    }
    private static String tierLabel(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> "일반"; case UNCOMMON -> "고급"; case RARE -> "희귀";
            case EPIC -> "영웅"; case LEGENDARY -> "전설"; case MYTHIC -> "신화";
        };
    }
    private enum View { SELECT, OWNED, SYNTHESIS }
}

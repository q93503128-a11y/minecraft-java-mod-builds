package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Portable augment device presentation.
 * Server menu state remains authoritative; reveal, selection and synthesis motion are client-only.
 */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 380, H = 236;
    private static final int BG = 0xFF0E1319, PANEL = 0xFF141B22, PANEL_TOP = 0xFF1E2832;
    private static final int HOVER = 0xFF2B3945, SELECTED = 0xFF33424E;
    private static final int METAL_DARK = 0xFF26313B, METAL = 0xFF566674, METAL_LIGHT = 0xFFA9BAC8;
    private static final int GLASS = 0xFF080D12, GLASS_TOP = 0xFF111A22;
    private static final int TEXT = 0xFFF3F7FA, MUTED = 0xFFAAB5BE, FAINT = 0xFF72808B;
    private static final int GOOD = 0xFF8ED9A0, BAD = 0xFFE58C8C, ACTIVE = 0xFFF0D58A;
    private static final int CARD_X = 13, CARD_GAP = 9, CARD_W = 112, CARD_H = 112;

    private boolean upgradeMode;
    private int upgradeTierCursor;
    private int observedStatus = Integer.MIN_VALUE;
    private int observedMask = -1;
    private int observedTier = Integer.MIN_VALUE;
    private int observedCandidateKey = Integer.MIN_VALUE;
    private long revealStarted = Long.MIN_VALUE;
    private long acquireStarted = Long.MIN_VALUE;
    private long selectPulseStarted = Long.MIN_VALUE;
    private int selectPulseSlot = -1;
    private long synthesisStarted = Long.MIN_VALUE;
    private AugmentTier synthesisFrom = AugmentTier.COMMON;
    private boolean synthesisAll;

    public AugmentDeviceScreen(AugmentDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int l = leftPos, t = topPos;
        g.fill(l + 5, t + 6, l + imageWidth + 5, t + imageHeight + 6, 0x76000000);
        g.fill(l - 2, t - 2, l + imageWidth + 2, t + imageHeight + 2, 0xFF090D12);
        g.fill(l, t, l + imageWidth, t + imageHeight, METAL_DARK);
        g.outline(l, t, imageWidth, imageHeight, METAL);
        g.fill(l + 2, t + 2, l + imageWidth - 2, t + imageHeight - 2, BG);
        g.fill(l + 3, t + 3, l + imageWidth - 3, t + 4, 0xFF7C8C99);
        g.fill(l + 4, t + 4, l + imageWidth - 4, t + 14, PANEL_TOP);
        g.fill(l + 4, t + 14, l + imageWidth - 4, t + 29, 0xFF182129);
        g.fill(l + 4, t + 29, l + imageWidth - 4, t + 33, 0xFF10161C);
        g.fill(l + 4, t + 32, l + imageWidth - 4, t + 33, 0xFF52616D);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        syncPresentationState();
        int mx = mouseX - leftPos, my = mouseY - topPos;
        g.text(font, title.getString(), 14, 11, TEXT, false);
        String mode = upgradeMode ? "코어 정제" : "증강 동조";
        g.text(font, mode, imageWidth - 14 - font.width(mode), 11, 0xFFD4DEE6, false);
        if (menu.advanced()) drawTabs(g, mx, my);
        if (upgradeMode && menu.advanced()) drawUpgrade(g, mx, my);
        else drawSelection(g, mx, my);
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my) {
        drawTab(g, 14, 37, 104, "증강 선택", !upgradeMode, mx, my);
        drawTab(g, 124, 37, 104, "코어 합성", upgradeMode, mx, my);
    }

    private void drawSelection(GuiGraphicsExtractor g, int mx, int my) {
        int tierY = menu.advanced() ? 61 : 40;
        for (AugmentTier tier : AugmentTier.values()) {
            int x = 16 + tier.ordinal() * 58;
            boolean selected = menu.tierOrdinal() == tier.ordinal();
            boolean hover = inside(mx, my, x, tierY, 54, 19);
            drawTierChip(g, x, tierY, 54, 19, tier, selected, hover);
            centered(g, tierLabel(tier), x + 27, tierY + 6, selected ? TEXT : MUTED);
        }

        int cardY = menu.advanced() ? 88 : 67;
        boolean ready = menu.status() == AugmentDeviceMenu.STATUS_READY
                || menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED;
        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition candidate = ready ? menu.candidate(slot) : null;
            if (candidate == null) {
                drawEmptyCard(g, slot, cardY);
                continue;
            }
            double reveal = revealProgress(slot);
            if (reveal <= 0.03) {
                drawChargingCard(g, slot, cardY, candidate.tier());
                continue;
            }
            int offsetY = (int) Math.round((1.0 - easeOut(reveal)) * 15.0);
            drawCard(g, slot, cardY + offsetY, mx, my, reveal);
            if (reveal < 1.0) drawRevealSweep(g, slot, cardY + offsetY, candidate.tier(), reveal);
        }

        if (!ready) {
            centered(g, statusText(menu.status()), imageWidth / 2, cardY + 47, statusColor(menu.status()));
            if (menu.status() == AugmentDeviceMenu.STATUS_IDLE) {
                centered(g, "코어 등급을 고르면 세 가지 증강이 나타납니다.", imageWidth / 2, cardY + 64, MUTED);
            }
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            centered(g, menu.selectionCount() == 2 ? "서로 다른 증강 2개를 선택" : "원하는 증강을 선택",
                    imageWidth / 2, H - 18, MUTED);
        } else if (menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED) {
            drawAcquisitionBurst(g, cardY);
            centered(g, "동조 완료", imageWidth / 2, H - 18, GOOD);
        } else if (menu.status() != AugmentDeviceMenu.STATUS_IDLE) {
            centered(g, statusText(menu.status()), imageWidth / 2, H - 18, statusColor(menu.status()));
        }
    }

    private void drawTierChip(
            GuiGraphicsExtractor g, int x, int y, int w, int h,
            AugmentTier tier, boolean selected, boolean hover
    ) {
        int c = tierColor(tier);
        if (selected) g.outline(x - 1, y - 1, w + 2, h + 2, alpha(c, 72));
        g.fill(x, y, x + w, y + h, METAL_DARK);
        g.outline(x, y, w, h, selected ? brighten(c, 42) : hover ? brighten(c, 14) : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, selected ? tint(SELECTED, c, 24) : hover ? HOVER : PANEL);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, alpha(brighten(c, 60), selected ? 185 : 60));
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, shade(c, 0.48));
        if (selected) g.fill(x + 7, y + h - 3, x + w - 7, y + h - 2, c);
    }

    private void drawCard(GuiGraphicsExtractor g, int slot, int y, int mx, int my, double reveal) {
        AugmentDefinition d = menu.candidate(slot);
        int x = cardX(slot), tier = tierColor(d.tier());
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        boolean hover = inside(mx, my, x, y, CARD_W, CARD_H);
        int pulse = selectPulseSlot == slot ? (int) Math.round((1.0 - progress(selectPulseStarted, 320)) * 80.0) : 0;
        drawAugmentFrame(g, x, y, CARD_W, CARD_H, d.tier(), selected, hover, pulse);

        int wellX = x + 40, wellY = y + 10;
        g.fill(wellX, wellY, wellX + 32, wellY + 30, 0xFF05090D);
        g.outline(wellX, wellY, 32, 30, shade(tier, 0.58));
        g.fill(wellX + 2, wellY + 2, wellX + 30, wellY + 4, alpha(brighten(tier, 55), 90));
        if (reveal > 0.20) g.item(new ItemStack(AugmentCoreItems.item(d.tier())), x + 48, y + 16);

        if (reveal > 0.42) {
            String raw = d.canonicalNameKo();
            boolean active = raw.contains("(액티브");
            String name = raw.replace(" (액티브 · 토글)", "").replace(" (액티브)", "");
            centered(g, name, x + CARD_W / 2, y + 42, TEXT);
            drawBadge(g, x + 30, y + 55, 52, 13, tierLabel(d.tier()), tier, true);
            drawBadge(g, x + 29, y + 72, 54, 12, active ? "액티브" : "상시 효과", active ? ACTIVE : MUTED, false);
            int now = menu.currentLevel(slot), next = Math.min(now + 1, d.maxLevel());
            centered(g, now == 0 ? "획득  Lv." + next : "Lv." + now + "  →  " + next,
                    x + CARD_W / 2, y + 88, MUTED);
            centered(g, selected ? "선택됨" : "최대 Lv." + d.maxLevel(),
                    x + CARD_W / 2, y + 101, selected ? brighten(tier, 80) : FAINT);
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED) {
            double p = progress(acquireStarted, 720);
            if (!selected) {
                g.fill(x + 4, y + 4, x + CARD_W - 4, y + CARD_H - 4,
                        alpha(0xFF000000, (int) (165 * (1.0 - p * 0.35))));
            } else if (p < 1.0) {
                int spread = 2 + (int) Math.round(p * 10);
                int a = (int) Math.round((1.0 - p) * 190);
                g.outline(x - spread, y - spread, CARD_W + spread * 2, CARD_H + spread * 2,
                        alpha(brighten(tier, 90), a));
            }
        }
    }

    private void drawAugmentFrame(
            GuiGraphicsExtractor g, int x, int y, int w, int h,
            AugmentTier tier, boolean selected, boolean hover, int pulse
    ) {
        int c = tierColor(tier);
        int hi = brighten(c, tier.ordinal() >= AugmentTier.EPIC.ordinal() ? 90 : 56);
        int lo = shade(c, 0.46);
        int surface = selected ? tint(SELECTED, c, 18) : hover ? tint(HOVER, c, 10) : GLASS;
        int glow = Math.min(220, (selected ? 118 : hover ? 72 : 36 + tier.ordinal() * 4) + pulse);
        g.outline(x - 3, y - 3, w + 6, h + 6, alpha(c, Math.max(18, glow / 3)));
        g.outline(x - 2, y - 2, w + 4, h + 4, alpha(c, Math.max(24, glow / 2)));
        g.outline(x - 1, y - 1, w + 2, h + 2, alpha(hi, glow));
        g.fill(x, y, x + w, y + h, 0xFF080B0F);
        g.outline(x, y, w, h, lo);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, c);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, METAL_DARK);
        g.outline(x + 3, y + 3, w - 6, h - 6, hi);
        g.fill(x + 5, y + 5, x + w - 5, y + h - 5, surface);
        g.fill(x + 6, y + 6, x + w - 6, y + 12, GLASS_TOP);
        g.fill(x + 7, y + 6, x + w - 7, y + 7, alpha(hi, selected ? 108 : hover ? 76 : 42));
        g.fill(x + 7, y + h - 7, x + w - 7, y + h - 6, alpha(lo, 190));
        cutCorners(g, x, y, w, h);
        drawCornerBrackets(g, x, y, w, h, hi, c, tier.ordinal());
        if (selected) g.outline(x + 7, y + 7, w - 14, h - 14, alpha(brighten(c, 96), 150));
    }

    private void drawRevealSweep(GuiGraphicsExtractor g, int slot, int y, AugmentTier tier, double p) {
        int x = cardX(slot);
        int beamY = y + 7 + (int) Math.round((CARD_H - 14) * p);
        int a = (int) Math.round(160 * (1.0 - Math.abs(0.5 - p)));
        g.fill(x + 7, beamY, x + CARD_W - 7, beamY + 1, alpha(brighten(tierColor(tier), 90), a));
        g.fill(x + 13, beamY + 1, x + CARD_W - 13, beamY + 2, alpha(tierColor(tier), a / 2));
    }

    private void drawAcquisitionBurst(GuiGraphicsExtractor g, int cardY) {
        double p = progress(acquireStarted, 720);
        if (p >= 1.0) return;
        int width = CARD_W * 3 + CARD_GAP * 2;
        int spread = (int) Math.round(18 * p);
        int a = (int) Math.round(150 * (1.0 - p));
        int c = selectedTierColor();
        g.outline(CARD_X - spread, cardY + 34 - spread / 3, width + spread * 2, 46 + spread * 2 / 3,
                alpha(brighten(c, 90), a));
        int center = imageWidth / 2;
        int ray = 18 + (int) Math.round(38 * p);
        g.fill(center - ray, cardY + 54, center + ray, cardY + 55, alpha(brighten(c, 100), a));
    }

    private void drawChargingCard(GuiGraphicsExtractor g, int slot, int y, AugmentTier tier) {
        int x = cardX(slot), c = tierColor(tier);
        g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, 0xFF111820);
        g.outline(x - 1, y - 1, CARD_W + 2, CARD_H + 2, alpha(c, 80));
        g.fill(x + 3, y + 3, x + CARD_W - 3, y + CARD_H - 3, GLASS);
        int wave = (int) ((now() / 18L + slot * 19L) % (CARD_H - 16));
        g.fill(x + 8, y + 8 + wave, x + CARD_W - 8, y + 9 + wave, alpha(brighten(c, 70), 70));
        centered(g, "동조 중", x + CARD_W / 2, y + 56, alpha(brighten(c, 60), 180));
    }

    private void cutCorners(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + 3, y + 1, BG); g.fill(x, y, x + 1, y + 3, BG);
        g.fill(x + w - 3, y, x + w, y + 1, BG); g.fill(x + w - 1, y, x + w, y + 3, BG);
        g.fill(x, y + h - 1, x + 3, y + h, BG); g.fill(x, y + h - 3, x + 1, y + h, BG);
        g.fill(x + w - 3, y + h - 1, x + w, y + h, BG); g.fill(x + w - 1, y + h - 3, x + w, y + h, BG);
    }

    private void drawCornerBrackets(GuiGraphicsExtractor g, int x, int y, int w, int h, int hi, int c, int tierIndex) {
        int len = tierIndex >= 4 ? 9 : 7;
        int x0 = x + 4, x1 = x + w - 5, y0 = y + 4, y1 = y + h - 5;
        g.fill(x0, y0, x0 + len, y0 + 1, hi); g.fill(x0, y0, x0 + 1, y0 + len, hi);
        g.fill(x1 - len + 1, y0, x1 + 1, y0 + 1, hi); g.fill(x1, y0, x1 + 1, y0 + len, hi);
        int low = shade(c, 0.72);
        g.fill(x0, y1, x0 + len, y1 + 1, low); g.fill(x0, y1 - len + 1, x0 + 1, y1 + 1, low);
        g.fill(x1 - len + 1, y1, x1 + 1, y1 + 1, low); g.fill(x1, y1 - len + 1, x1 + 1, y1 + 1, low);
    }

    private void drawBadge(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, int accent, boolean luminous) {
        g.fill(x, y, x + w, y + h, 0xFF080B0F);
        g.outline(x, y, w, h, luminous ? shade(accent, 0.70) : 0xFF39444D);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, tint(0xFF11171D, accent, luminous ? 15 : 7));
        if (luminous) g.fill(x + 2, y + 2, x + w - 2, y + 3, alpha(brighten(accent, 48), 74));
        centered(g, text, x + w / 2, y + 3, luminous ? brighten(accent, 55) : accent);
    }

    private void drawEmptyCard(GuiGraphicsExtractor g, int slot, int y) {
        int x = cardX(slot);
        g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, 0xFF111820);
        g.outline(x - 1, y - 1, CARD_W + 2, CARD_H + 2, 0xFF27313A);
        g.fill(x + 2, y + 2, x + CARD_W - 2, y + CARD_H - 2, GLASS);
        g.outline(x + 3, y + 3, CARD_W - 6, CARD_H - 6, 0xFF303A43);
        g.fill(x + 5, y + 5, x + CARD_W - 5, y + 11, GLASS_TOP);
        centered(g, "◆", x + CARD_W / 2, y + 43, 0xFF34404A);
        centered(g, "동조 대기", x + CARD_W / 2, y + 61, 0xFF4B5862);
    }

    private void drawUpgrade(GuiGraphicsExtractor g, int mx, int my) {
        centered(g, "정제할 코어 등급을 고르세요.", imageWidth / 2, 61, MUTED);
        for (int i = 0; i < AugmentTier.values().length - 1; i++) {
            AugmentTier tier = AugmentTier.values()[i];
            int x = 20 + i * 68;
            boolean selected = upgradeTierCursor == i;
            boolean hover = inside(mx, my, x, 71, 62, 18);
            drawUpgradeTierChip(g, x, 71, 62, tier, selected, hover);
            centered(g, tierLabel(tier), x + 31, 76, selected ? TEXT : MUTED);
        }

        AugmentTier from = AugmentTier.values()[Math.max(0, Math.min(upgradeTierCursor, AugmentTier.values().length - 2))];
        AugmentTier to = from.next().orElse(AugmentTier.MYTHIC);
        drawSynthesisChamber(g, from, to, mx, my);
        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED) {
            centered(g, "코어 정제 완료 ×" + Math.max(1, menu.lastOutputCount()), imageWidth / 2, H - 14, GOOD);
        } else if (menu.status() == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES) {
            centered(g, "합성할 코어가 부족합니다.", imageWidth / 2, H - 14, BAD);
        } else {
            centered(g, "같은 등급 코어 3개 → 다음 등급 코어 1개", imageWidth / 2, H - 14, FAINT);
        }
        drawSynthesisBurst(g);
    }

    private void drawUpgradeTierChip(
            GuiGraphicsExtractor g, int x, int y, int w,
            AugmentTier tier, boolean selected, boolean hover
    ) {
        int c = tierColor(tier);
        g.fill(x, y, x + w, y + 18, METAL_DARK);
        g.outline(x, y, w, 18, selected ? brighten(c, 62) : hover ? brighten(c, 22) : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + 16, selected ? tint(SELECTED, c, 24) : hover ? HOVER : PANEL);
        g.fill(x + 4, y + 3, x + w - 4, y + 4, alpha(brighten(c, 70), selected ? 150 : 55));
        if (selected) g.fill(x + 8, y + 15, x + w - 8, y + 17, c);
    }

    private void drawSynthesisChamber(GuiGraphicsExtractor g, AugmentTier from, AugmentTier to, int mx, int my) {
        int chamberX = 22, chamberY = 96, chamberW = 336, chamberH = 78;
        int toColor = tierColor(to);
        g.fill(chamberX, chamberY, chamberX + chamberW, chamberY + chamberH, METAL_DARK);
        g.outline(chamberX, chamberY, chamberW, chamberH, METAL);
        g.fill(chamberX + 2, chamberY + 2, chamberX + chamberW - 2, chamberY + chamberH - 2, GLASS);
        g.fill(chamberX + 3, chamberY + 3, chamberX + chamberW - 3, chamberY + 9, GLASS_TOP);
        drawCoreWell(g, 39, 105, 108, 58, tierColor(from), "입력", from, true);
        drawCoreWell(g, 233, 105, 108, 58, toColor, "결과", to, false);

        int cy = 134;
        int wave = (int) ((now() / 24L) % 54L);
        g.fill(151, cy, 229, cy + 2, 0xFF1C2831);
        g.fill(157 + wave, cy, Math.min(229, 163 + wave), cy + 2, alpha(brighten(toColor, 70), 130));
        g.outline(178, 119, 24, 30, alpha(toColor, 78));
        g.outline(181, 122, 18, 24, alpha(brighten(toColor, 55), 108));
        centered(g, "◆", 190, 130, brighten(toColor, 70));
        drawAction(g, 106, 183, 78, "1회 정제", mx, my);
        drawAction(g, 196, 183, 78, "모두 정제", mx, my);
    }

    private void drawCoreWell(
            GuiGraphicsExtractor g, int x, int y, int w, int h,
            int accent, String label, AugmentTier tier, boolean input
    ) {
        g.fill(x, y, x + w, y + h, 0xFF070B0F);
        g.outline(x, y, w, h, shade(accent, 0.68));
        g.fill(x + 2, y + 2, x + w - 2, y + 8, tint(GLASS_TOP, accent, 14));
        g.fill(x + 3, y + 3, x + w - 3, y + 4, alpha(brighten(accent, 70), 74));
        centered(g, label, x + w / 2, y + 6, brighten(accent, 55));
        if (input) {
            g.item(new ItemStack(AugmentCoreItems.item(tier)), x + 27, y + 25);
            g.item(new ItemStack(AugmentCoreItems.item(tier)), x + 47, y + 19);
            g.item(new ItemStack(AugmentCoreItems.item(tier)), x + 67, y + 25);
            centered(g, tierLabel(tier) + " ×3", x + w / 2, y + 45, MUTED);
        } else {
            g.item(new ItemStack(AugmentCoreItems.item(tier)), x + 46, y + 21);
            centered(g, tierLabel(tier) + " ×1", x + w / 2, y + 45, MUTED);
        }
    }

    private void drawSynthesisBurst(GuiGraphicsExtractor g) {
        double p = progress(synthesisStarted, 880);
        if (p >= 1.0) return;

        int fromColor = tierColor(synthesisFrom);
        AugmentTier toTier = synthesisFrom.next().orElse(AugmentTier.MYTHIC);
        int toColor = tierColor(toTier);
        g.fill(24, 98, 356, 174, alpha(0xFF05080C, (int) (130 * (1.0 - Math.min(1.0, p * 1.7)))));

        boolean confirmed = menu.status() == AugmentDeviceMenu.STATUS_UPGRADED;
        if (!confirmed || p < 0.52) {
            double q = easeInOut(Math.min(1.0, p / 0.52));
            int[][] start = {{66, 130}, {86, 122}, {106, 130}};
            for (int[] s : start) {
                int x = lerp(s[0], 182, q), y = lerp(s[1], 126, q);
                g.item(new ItemStack(AugmentCoreItems.item(synthesisFrom)), x, y);
            }
            int radius = 8 + (int) Math.round(q * 15);
            g.outline(190 - radius, 134 - radius, radius * 2, radius * 2,
                    alpha(brighten(fromColor, 80), (int) (90 + 120 * q)));
            return;
        }

        double q = easeOut((p - 0.52) / 0.48);
        int x = lerp(182, 279, q);
        int radius = 22 - (int) Math.round(q * 11);
        g.outline(190 - radius, 134 - radius, radius * 2, radius * 2,
                alpha(brighten(toColor, 100), (int) (210 * (1.0 - q))));
        g.item(new ItemStack(AugmentCoreItems.item(toTier)), x, 126);
        if (q > 0.62) centered(g, synthesisAll ? "연속 정제 완료" : "정제 완료", 287, 151, brighten(toColor, 70));
    }

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean active, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 18);
        g.fill(x, y, x + w, y + 18, METAL_DARK);
        g.outline(x, y, w, 18, active ? 0xFFCAD7E1 : hover ? 0xFF7F909E : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + 16, active ? SELECTED : hover ? HOVER : PANEL);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, active ? 0xFF7E93A3 : 0xFF36434E);
        if (active) g.fill(x + 8, y + 15, x + w - 8, y + 17, 0xFFB8CAD7);
        centered(g, text, x + w / 2, y + 5, active ? TEXT : MUTED);
    }

    private void drawAction(GuiGraphicsExtractor g, int x, int y, int w, String text, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 17);
        g.fill(x, y, x + w, y + 17, METAL_DARK);
        g.outline(x, y, w, 17, hover ? METAL_LIGHT : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + 15, hover ? HOVER : PANEL);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, hover ? 0xFF8195A4 : 0xFF34414C);
        centered(g, text, x + w / 2, y + 5, hover ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double x = event.x() - leftPos, y = event.y() - topPos;

        if (menu.advanced()) {
            if (inside(x, y, 14, 37, 104, 18)) {
                upgradeMode = false;
                playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
                return true;
            }
            if (inside(x, y, 124, 37, 104, 18)) {
                upgradeMode = true;
                playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.08F);
                return true;
            }
        }

        if (upgradeMode && menu.advanced()) {
            for (int i = 0; i < AugmentTier.values().length - 1; i++) {
                int tx = 20 + i * 68;
                if (inside(x, y, tx, 71, 62, 18)) {
                    upgradeTierCursor = i;
                    playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.96F + i * 0.05F);
                    return true;
                }
            }
            AugmentTier tier = AugmentTier.values()[upgradeTierCursor];
            if (inside(x, y, 106, 183, 78, 17)) {
                beginSynthesisFeedback(tier, false);
                return send(AugmentDeviceMenu.upgradeOneButtonId(tier));
            }
            if (inside(x, y, 196, 183, 78, 17)) {
                beginSynthesisFeedback(tier, true);
                return send(AugmentDeviceMenu.upgradeAllButtonId(tier));
            }
            return super.mouseClicked(event, doubleClick);
        }

        int tierY = menu.advanced() ? 61 : 40;
        for (AugmentTier tier : AugmentTier.values()) {
            int tx = 16 + tier.ordinal() * 58;
            if (inside(x, y, tx, tierY, 54, 19)) {
                playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.90F + tier.ordinal() * 0.05F);
                return send(AugmentDeviceMenu.beginButtonId(tier));
            }
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            int cardY = menu.advanced() ? 88 : 67;
            for (int slot = 0; slot < 3; slot++) {
                if (menu.candidate(slot) != null && inside(x, y, cardX(slot), cardY, CARD_W, CARD_H)) {
                    selectPulseSlot = slot;
                    selectPulseStarted = now();
                    playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.18F + slot * 0.05F);
                    return send(AugmentDeviceMenu.candidateButtonId(slot));
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void beginSynthesisFeedback(AugmentTier tier, boolean all) {
        synthesisFrom = tier;
        synthesisAll = all;
        synthesisStarted = now();
        playUi(SoundEvents.AMETHYST_BLOCK_CHIME, all ? 0.88F : 1.02F);
    }

    private boolean send(int id) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!menu.clickMenuButton(minecraft.player, id)) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }

    private void syncPresentationState() {
        int status = menu.status(), mask = menu.selectedMask(), tier = menu.tierOrdinal(), candidateKey = candidateKey();
        if (observedStatus == Integer.MIN_VALUE) {
            observedStatus = status;
            observedMask = mask;
            observedTier = tier;
            observedCandidateKey = candidateKey;
            return;
        }

        if (status == AugmentDeviceMenu.STATUS_READY
                && (observedStatus != status || observedTier != tier || observedCandidateKey != candidateKey)) {
            revealStarted = now();
            playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.10F + Math.max(0, tier) * 0.04F);
        }
        if (status == AugmentDeviceMenu.STATUS_ACQUIRED && observedStatus != status) {
            acquireStarted = now();
            playUi(SoundEvents.PLAYER_LEVELUP, 1.18F);
        } else if (status == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES && observedStatus != status) {
            synthesisStarted = Long.MIN_VALUE;
            playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.58F);
        }
        if (mask != observedMask && status == AugmentDeviceMenu.STATUS_READY) {
            int added = mask & ~observedMask;
            if (added != 0) {
                selectPulseSlot = Integer.numberOfTrailingZeros(added);
                selectPulseStarted = now();
            }
        }
        if (status == AugmentDeviceMenu.STATUS_UPGRADED && observedStatus != status) {
            if (tier >= 0 && tier < AugmentTier.values().length - 1) {
                synthesisFrom = AugmentTier.values()[tier];
                upgradeTierCursor = tier;
            }
            synthesisStarted = now();
            playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.34F);
        }
        observedStatus = status;
        observedMask = mask;
        observedTier = tier;
        observedCandidateKey = candidateKey;
    }

    private int candidateKey() {
        int key = 17;
        for (int i = 0; i < 3; i++) {
            AugmentDefinition d = menu.candidate(i);
            key = 31 * key + (d == null ? 0 : d.id().hashCode());
        }
        return key;
    }

    private void playUi(SoundEvent sound, float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    private double revealProgress(int slot) {
        if (revealStarted == Long.MIN_VALUE) return 1.0;
        long age = now() - revealStarted - slot * 85L;
        if (age <= 0) return 0.0;
        return clamp01(age / 240.0);
    }

    private int selectedTierColor() {
        int tier = menu.tierOrdinal();
        if (tier < 0 || tier >= AugmentTier.values().length) return METAL_LIGHT;
        return tierColor(AugmentTier.values()[tier]);
    }

    private static long now() { return System.currentTimeMillis(); }
    private static double progress(long start, long duration) {
        if (start == Long.MIN_VALUE) return 1.0;
        return clamp01((now() - start) / (double) duration);
    }
    private static double easeOut(double p) {
        p = clamp01(p);
        double inv = 1.0 - p;
        return 1.0 - inv * inv * inv;
    }
    private static double easeInOut(double p) {
        p = clamp01(p);
        return p < 0.5 ? 2.0 * p * p : 1.0 - Math.pow(-2.0 * p + 2.0, 2.0) / 2.0;
    }
    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    private static int lerp(int a, int b, double p) { return (int) Math.round(a + (b - a) * clamp01(p)); }
    private static int cardX(int slot) { return CARD_X + slot * (CARD_W + CARD_GAP); }
    private void centered(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(font, s, x - font.width(s) / 2, y, color, false);
    }
    private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
    private static int alpha(int argb, int a) { return (clamp(a) << 24) | (argb & 0x00FFFFFF); }
    private static int brighten(int argb, int amount) {
        int r = clamp(((argb >> 16) & 0xFF) + amount);
        int g = clamp(((argb >> 8) & 0xFF) + amount);
        int b = clamp((argb & 0xFF) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static int shade(int argb, double f) {
        int r = clamp((int) (((argb >> 16) & 0xFF) * f));
        int g = clamp((int) (((argb >> 8) & 0xFF) * f));
        int b = clamp((int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static int tint(int base, int tint, int p) {
        int inv = 100 - p;
        int r = (((base >> 16) & 0xFF) * inv + ((tint >> 16) & 0xFF) * p) / 100;
        int g = (((base >> 8) & 0xFF) * inv + ((tint >> 8) & 0xFF) * p) / 100;
        int b = ((base & 0xFF) * inv + (tint & 0xFF) * p) / 100;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static String tierLabel(AugmentTier t) {
        return switch (t) {
            case COMMON -> "일반";
            case UNCOMMON -> "고급";
            case RARE -> "희귀";
            case EPIC -> "영웅";
            case LEGENDARY -> "전설";
            case MYTHIC -> "신화";
        };
    }

    private static int tierColor(AugmentTier t) {
        return switch (t) {
            case COMMON -> 0xFFC6CCD3;
            case UNCOMMON -> 0xFF55D28B;
            case RARE -> 0xFF4A9FF2;
            case EPIC -> 0xFFB35FE3;
            case LEGENDARY -> 0xFFF0B33F;
            case MYTHIC -> 0xFFEC4E78;
        };
    }

    private static String statusText(int s) {
        return switch (s) {
            case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다.";
            case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 모든 증강을 완성했습니다.";
            case AugmentDeviceMenu.STATUS_ACQUIRED -> "증강을 획득했습니다.";
            case AugmentDeviceMenu.STATUS_INVALID -> "선택을 완료할 수 없습니다. 다시 시도하세요.";
            case AugmentDeviceMenu.STATUS_UPGRADED -> "코어 정제가 완료되었습니다.";
            case AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> "합성할 코어가 부족합니다.";
            default -> "증강 코어를 선택하세요.";
        };
    }

    private static int statusColor(int s) {
        return switch (s) {
            case AugmentDeviceMenu.STATUS_ACQUIRED, AugmentDeviceMenu.STATUS_UPGRADED -> GOOD;
            case AugmentDeviceMenu.STATUS_NO_CORE, AugmentDeviceMenu.STATUS_INVALID,
                 AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> BAD;
            default -> MUTED;
        };
    }
}

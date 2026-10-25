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

/** Portable augment device with a tier lobby, dedicated draw page and synthesis chamber. */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 560, H = 326;
    private static final int BG = 0xFF090E13, PANEL = 0xFF111820, PANEL_TOP = 0xFF1A252E;
    private static final int HOVER = 0xFF263640, SELECTED = 0xFF1F3844;
    private static final int METAL_DARK = 0xFF202A32, METAL = 0xFF51616E, METAL_LIGHT = 0xFFA9BBC7;
    private static final int GLASS = 0xFF070B0F, GLASS_TOP = 0xFF101820;
    private static final int TEXT = 0xFFF3F7FA, MUTED = 0xFFA5B0B8, FAINT = 0xFF707C85;
    private static final int GOOD = 0xFF8ED9A0, BAD = 0xFFE58C8C, ACTIVE = 0xFFF0D58A;
    private static final int DRAW_X = 16, DRAW_GAP = 16, CARD_W = 165, CARD_H = 218, CARD_Y = 67;

    private final Inventory playerInventory;
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
        this.playerInventory = inventory;
    }

    private boolean drawPhase() {
        return menu.status() == AugmentDeviceMenu.STATUS_READY || menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int l = leftPos, t = topPos;
        g.fill(l + 7, t + 8, l + imageWidth + 7, t + imageHeight + 8, 0x78000000);
        g.fill(l - 3, t - 3, l + imageWidth + 3, t + imageHeight + 3, 0xFF05090C);
        g.outline(l - 2, t - 2, imageWidth + 4, imageHeight + 4, 0xFF31404B);
        g.fill(l, t, l + imageWidth, t + imageHeight, BG);
        g.outline(l, t, imageWidth, imageHeight, METAL);
        g.fill(l + 2, t + 2, l + imageWidth - 2, t + 31, 0xFF111B23);
        g.fill(l + 3, t + 3, l + imageWidth - 3, t + 4, 0xFF93A6B3);
        g.fill(l + 3, t + 30, l + imageWidth - 3, t + 31, 0xFF3B4A55);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        syncPresentationState();
        int mx = mouseX - leftPos, my = mouseY - topPos;
        String page = drawPhase() ? "증강 동조" : upgradeMode ? "코어 정제" : "코어 선택";
        g.text(font, page, 16, 11, TEXT, false);
        if (drawPhase()) {
            AugmentTier tier = selectedTier();
            String tierName = tier == null ? "" : tierLabel(tier) + " 코어";
            g.text(font, tierName, imageWidth - 16 - font.width(tierName), 11,
                    tier == null ? MUTED : brighten(tierColor(tier), 55), false);
            drawDrawPage(g, mx, my);
            return;
        }
        if (menu.advanced()) drawTabs(g, mx, my);
        if (upgradeMode && menu.advanced()) drawUpgrade(g, mx, my);
        else drawTierLobby(g, mx, my);
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my) {
        drawTab(g, 16, 40, 112, "증강 동조", !upgradeMode, mx, my);
        drawTab(g, 136, 40, 112, "코어 정제", upgradeMode, mx, my);
    }

    private void drawTierLobby(GuiGraphicsExtractor g, int mx, int my) {
        int top = menu.advanced() ? 72 : 48;
        centered(g, "동조할 코어의 등급을 선택하세요", imageWidth / 2, top, TEXT);
        centered(g, "선택하면 코어 하나에 고정된 세 가지 증강이 공개됩니다.", imageWidth / 2, top + 15, MUTED);
        centered(g, "공개된 후보는 화면을 닫아도 다시 굴러가지 않습니다.", imageWidth / 2, top + 29, FAINT);

        int gridY = top + 51;
        for (AugmentTier tier : AugmentTier.values()) {
            int index = tier.ordinal();
            int col = index % 3, row = index / 3;
            int x = 18 + col * 178, y = gridY + row * 83;
            drawTierPedestal(g, x, y, 166, 69, tier, mx, my);
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_NO_CORE
                || menu.status() == AugmentDeviceMenu.STATUS_FULLY_MAXED
                || menu.status() == AugmentDeviceMenu.STATUS_INVALID) {
            centered(g, statusText(menu.status()), imageWidth / 2, H - 19, statusColor(menu.status()));
        } else {
            centered(g, "코어는 증강을 확정할 때 1개 소모됩니다.", imageWidth / 2, H - 19, FAINT);
        }
    }

    private void drawTierPedestal(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                  AugmentTier tier, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, h);
        int c = tierColor(tier), hi = brighten(c, 68);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, alpha(c, hover ? 58 : 24));
        g.fill(x, y, x + w, y + h, METAL_DARK);
        g.outline(x, y, w, h, hover ? hi : shade(c, 0.72));
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, hover ? tint(PANEL, c, 12) : GLASS);
        g.fill(x + 4, y + 4, x + w - 4, y + 10, tint(GLASS_TOP, c, 16));
        g.fill(x + 5, y + 4, x + w - 5, y + 5, alpha(hi, hover ? 130 : 58));
        g.fill(x + 6, y + h - 7, x + w - 6, y + h - 6, alpha(shade(c, 0.55), 190));

        int wellX = x + 13, wellY = y + 15;
        g.fill(wellX, wellY, wellX + 42, wellY + 42, 0xFF05090D);
        g.outline(wellX, wellY, 42, 42, hover ? hi : shade(c, 0.62));
        g.fill(wellX + 2, wellY + 2, wellX + 40, wellY + 6, alpha(hi, 70));
        g.item(new ItemStack(AugmentCoreItems.item(tier)), wellX + 13, wellY + 13);

        g.text(font, tierLabel(tier), x + 68, y + 15, hover ? TEXT : brighten(c, 42), false);
        int count = countCore(tier);
        String owned = "보유  " + count;
        g.text(font, owned, x + 68, y + 33, count > 0 ? MUTED : BAD, false);
        String action = count > 0 ? "동조 시작  ›" : "코어 필요";
        g.text(font, action, x + w - 12 - font.width(action), y + 49, hover && count > 0 ? hi : FAINT, false);
        drawCornerBrackets(g, x, y, w, h, hi, c, tier.ordinal());
    }

    private void drawDrawPage(GuiGraphicsExtractor g, int mx, int my) {
        if (menu.status() != AugmentDeviceMenu.STATUS_READY && menu.status() != AugmentDeviceMenu.STATUS_ACQUIRED) {
            centered(g, statusText(menu.status()), imageWidth / 2, H / 2, statusColor(menu.status()));
            return;
        }

        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition d = menu.candidate(slot);
            if (d == null) continue;
            double reveal = revealProgress(slot);
            if (reveal <= 0.03) {
                drawChargingCard(g, slot, d.tier());
                continue;
            }
            int offsetY = (int) Math.round((1.0 - easeOut(reveal)) * 24.0);
            drawCard(g, slot, CARD_Y + offsetY, mx, my, reveal);
            if (reveal < 1.0) drawRevealSweep(g, slot, CARD_Y + offsetY, d.tier(), reveal);
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
            String instruction = menu.selectionCount() == 2
                    ? "두 개를 선택하세요 · 후보는 이미 고정되었습니다"
                    : "하나를 선택하세요 · 후보는 이미 고정되었습니다";
            centered(g, instruction, imageWidth / 2, H - 20, MUTED);
        } else {
            drawAcquisitionBurst(g);
            centered(g, "동조 완료", imageWidth / 2, H - 20, GOOD);
        }
    }

    private void drawCard(GuiGraphicsExtractor g, int slot, int y, int mx, int my, double reveal) {
        AugmentDefinition d = menu.candidate(slot);
        int x = cardX(slot), c = tierColor(d.tier());
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        boolean hover = inside(mx, my, x, y, CARD_W, CARD_H);
        int pulse = selectPulseSlot == slot ? (int) Math.round((1.0 - progress(selectPulseStarted, 340)) * 92.0) : 0;
        drawAugmentFrame(g, x, y, CARD_W, CARD_H, d.tier(), selected, hover, pulse);

        String raw = d.canonicalNameKo();
        boolean active = raw.contains("(액티브");
        String name = cleanName(raw);
        if (reveal > 0.18) {
            drawTinyTag(g, x + 10, y + 10, tierLabel(d.tier()), c);
            String type = active ? "액티브" : "패시브";
            int tagW = font.width(type) + 12;
            drawTinyTag(g, x + CARD_W - tagW - 10, y + 10, type, active ? ACTIVE : MUTED);
        }

        if (reveal > 0.28) {
            int cx = x + CARD_W / 2, cy = y + 80;
            drawAugmentSigil(g, d.id(), cx, cy, 54, brighten(c, 80), selected || hover);
        }

        if (reveal > 0.48) {
            centered(g, name, x + CARD_W / 2, y + 126, TEXT);
            g.fill(x + 22, y + 145, x + CARD_W - 22, y + 146, alpha(c, 84));
            int nowLevel = menu.currentLevel(slot), next = Math.min(nowLevel + 1, d.maxLevel());
            String level = nowLevel <= 0 ? "획득 시 Lv." + next : "Lv." + nowLevel + "  →  Lv." + next;
            centered(g, level, x + CARD_W / 2, y + 158, MUTED);
            centered(g, "최대 Lv." + d.maxLevel(), x + CARD_W / 2, y + 175, FAINT);
            String select = selected ? "선택됨" : hover ? "선택하기" : "";
            if (!select.isEmpty()) centered(g, select, x + CARD_W / 2, y + 197, selected ? brighten(c, 95) : brighten(c, 65));
        }

        if (menu.status() == AugmentDeviceMenu.STATUS_ACQUIRED) {
            double p = progress(acquireStarted, 760);
            if (!selected) {
                g.fill(x + 5, y + 5, x + CARD_W - 5, y + CARD_H - 5,
                        alpha(0xFF000000, (int) (174 * (1.0 - p * 0.28))));
            } else if (p < 1.0) {
                int spread = 2 + (int) Math.round(p * 15);
                int a = (int) Math.round((1.0 - p) * 210);
                g.outline(x - spread, y - spread, CARD_W + spread * 2, CARD_H + spread * 2,
                        alpha(brighten(c, 105), a));
            }
        }
    }

    private void drawAugmentFrame(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                  AugmentTier tier, boolean selected, boolean hover, int pulse) {
        int c = tierColor(tier), hi = brighten(c, tier.ordinal() >= 3 ? 98 : 66), lo = shade(c, 0.42);
        int surface = selected ? tint(SELECTED, c, 18) : hover ? tint(HOVER, c, 9) : GLASS;
        int glow = Math.min(225, (selected ? 130 : hover ? 82 : 42 + tier.ordinal() * 6) + pulse);
        g.outline(x - 4, y - 4, w + 8, h + 8, alpha(c, Math.max(18, glow / 4)));
        g.outline(x - 3, y - 3, w + 6, h + 6, alpha(c, Math.max(24, glow / 3)));
        g.outline(x - 2, y - 2, w + 4, h + 4, alpha(hi, glow / 2));
        g.fill(x, y, x + w, y + h, 0xFF05080B);
        g.outline(x, y, w, h, lo);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, c);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, METAL_DARK);
        g.outline(x + 4, y + 4, w - 8, h - 8, hi);
        g.fill(x + 7, y + 7, x + w - 7, y + h - 7, surface);
        g.fill(x + 8, y + 8, x + w - 8, y + 20, tint(GLASS_TOP, c, 10));
        g.fill(x + 10, y + 8, x + w - 10, y + 9, alpha(hi, selected ? 145 : hover ? 105 : 54));
        g.fill(x + 10, y + h - 11, x + w - 10, y + h - 9, alpha(lo, 210));
        cutCorners(g, x, y, w, h);
        drawCornerBrackets(g, x, y, w, h, hi, c, tier.ordinal());
        if (tier.ordinal() >= AugmentTier.LEGENDARY.ordinal()) {
            g.outline(x + 9, y + 9, w - 18, h - 18, alpha(brighten(c, 100), hover || selected ? 98 : 52));
        }
    }

    private void drawTinyTag(GuiGraphicsExtractor g, int x, int y, String text, int accent) {
        int w = font.width(text) + 12, h = 13;
        g.fill(x, y, x + w, y + h, 0xD905090D);
        g.outline(x, y, w, h, shade(accent, 0.78));
        g.fill(x + 2, y + 2, x + w - 2, y + 4, alpha(brighten(accent, 60), 66));
        g.text(font, text, x + 6, y + 3, brighten(accent, 46), false);
    }

    private void drawAugmentSigil(GuiGraphicsExtractor g, String id, int cx, int cy, int size, int color, boolean bright) {
        int hash = id.hashCode();
        int half = size / 2;
        int dark = shade(color, 0.32);
        g.fill(cx - half, cy - half, cx + half, cy + half, 0xFF05090D);
        g.outline(cx - half, cy - half, size, size, alpha(color, bright ? 150 : 86));
        g.outline(cx - half + 5, cy - half + 5, size - 10, size - 10, alpha(color, bright ? 102 : 54));
        g.fill(cx - 1, cy - half + 8, cx + 1, cy + half - 8, alpha(dark, 190));
        g.fill(cx - half + 8, cy - 1, cx + half - 8, cy + 1, alpha(dark, 190));
        int cell = 6;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                int bit = row * 3 + col;
                if (((hash >>> bit) & 1) == 0 && !(row == 2 && col == 2)) continue;
                int px = cx - 15 + col * cell;
                int py = cy - 15 + row * cell;
                g.fill(px, py, px + cell - 1, py + cell - 1, color);
                int mirrorX = cx + 15 - (col + 1) * cell;
                g.fill(mirrorX, py, mirrorX + cell - 1, py + cell - 1, color);
            }
        }
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, brighten(color, 90));
        if (bright) {
            g.outline(cx - half - 3, cy - half - 3, size + 6, size + 6, alpha(brighten(color, 90), 62));
        }
    }

    private void drawChargingCard(GuiGraphicsExtractor g, int slot, AugmentTier tier) {
        int x = cardX(slot), y = CARD_Y, c = tierColor(tier);
        g.fill(x - 2, y - 2, x + CARD_W + 2, y + CARD_H + 2, 0xFF101820);
        g.outline(x - 2, y - 2, CARD_W + 4, CARD_H + 4, alpha(c, 88));
        g.fill(x + 4, y + 4, x + CARD_W - 4, y + CARD_H - 4, GLASS);
        int wave = (int) ((now() / 16L + slot * 31L) % (CARD_H - 30));
        g.fill(x + 12, y + 15 + wave, x + CARD_W - 12, y + 17 + wave, alpha(brighten(c, 80), 80));
        centered(g, "동조 중", x + CARD_W / 2, y + CARD_H / 2, brighten(c, 55));
    }

    private void drawRevealSweep(GuiGraphicsExtractor g, int slot, int y, AugmentTier tier, double p) {
        int x = cardX(slot), beamY = y + 10 + (int) Math.round((CARD_H - 20) * p);
        int a = (int) Math.round(175 * (1.0 - Math.abs(0.5 - p)));
        g.fill(x + 10, beamY, x + CARD_W - 10, beamY + 2, alpha(brighten(tierColor(tier), 100), a));
        g.fill(x + 22, beamY + 2, x + CARD_W - 22, beamY + 3, alpha(tierColor(tier), a / 2));
    }

    private void drawAcquisitionBurst(GuiGraphicsExtractor g) {
        double p = progress(acquireStarted, 760);
        if (p >= 1.0) return;
        int c = selectedTierColor(), center = imageWidth / 2;
        int spread = (int) Math.round(28 * p), a = (int) Math.round(170 * (1.0 - p));
        g.outline(DRAW_X - spread, CARD_Y + 52 - spread / 3,
                CARD_W * 3 + DRAW_GAP * 2 + spread * 2, 104 + spread * 2 / 3,
                alpha(brighten(c, 100), a));
        int ray = 28 + (int) Math.round(78 * p);
        g.fill(center - ray, CARD_Y + 103, center + ray, CARD_Y + 105, alpha(brighten(c, 110), a));
    }

    private void drawUpgrade(GuiGraphicsExtractor g, int mx, int my) {
        centered(g, "정제할 코어 등급을 고르세요", imageWidth / 2, 76, MUTED);
        for (int i = 0; i < AugmentTier.values().length - 1; i++) {
            AugmentTier tier = AugmentTier.values()[i];
            int x = 72 + i * 84;
            boolean selected = upgradeTierCursor == i, hover = inside(mx, my, x, 89, 76, 19);
            drawUpgradeTierChip(g, x, 89, 76, tier, selected, hover);
            centered(g, tierLabel(tier), x + 38, 95, selected ? TEXT : MUTED);
        }
        AugmentTier from = AugmentTier.values()[Math.max(0, Math.min(upgradeTierCursor, AugmentTier.values().length - 2))];
        AugmentTier to = from.next().orElse(AugmentTier.MYTHIC);
        drawSynthesisChamber(g, from, to, mx, my);
        if (menu.status() == AugmentDeviceMenu.STATUS_UPGRADED) {
            centered(g, "코어 정제 완료 ×" + Math.max(1, menu.lastOutputCount()), imageWidth / 2, H - 18, GOOD);
        } else if (menu.status() == AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES) {
            centered(g, "합성할 코어가 부족합니다", imageWidth / 2, H - 18, BAD);
        } else if (menu.status() == AugmentDeviceMenu.STATUS_INVALID) {
            centered(g, "진행 중인 증강 선택을 먼저 완료하세요", imageWidth / 2, H - 18, BAD);
        } else {
            centered(g, "같은 등급 코어 3개 → 다음 등급 코어 1개", imageWidth / 2, H - 18, FAINT);
        }
        drawSynthesisBurst(g);
    }

    private void drawUpgradeTierChip(GuiGraphicsExtractor g, int x, int y, int w, AugmentTier tier,
                                     boolean selected, boolean hover) {
        int c = tierColor(tier);
        g.fill(x, y, x + w, y + 19, METAL_DARK);
        g.outline(x, y, w, 19, selected ? brighten(c, 62) : hover ? brighten(c, 22) : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + 17, selected ? tint(SELECTED, c, 24) : hover ? HOVER : PANEL);
        g.fill(x + 4, y + 3, x + w - 4, y + 4, alpha(brighten(c, 70), selected ? 150 : 55));
        if (selected) g.fill(x + 9, y + 16, x + w - 9, y + 18, c);
    }

    private void drawSynthesisChamber(GuiGraphicsExtractor g, AugmentTier from, AugmentTier to, int mx, int my) {
        int chamberX = 70, chamberY = 126, chamberW = 420, chamberH = 108, toColor = tierColor(to);
        g.fill(chamberX, chamberY, chamberX + chamberW, chamberY + chamberH, METAL_DARK);
        g.outline(chamberX, chamberY, chamberW, chamberH, METAL);
        g.fill(chamberX + 3, chamberY + 3, chamberX + chamberW - 3, chamberY + chamberH - 3, GLASS);
        g.fill(chamberX + 4, chamberY + 4, chamberX + chamberW - 4, chamberY + 13, GLASS_TOP);
        drawCoreWell(g, 89, 142, 133, 73, tierColor(from), "입력", from, true);
        drawCoreWell(g, 338, 142, 133, 73, toColor, "결과", to, false);
        int cy = 178, wave = (int) ((now() / 24L) % 82L);
        g.fill(228, cy, 332, cy + 2, 0xFF1C2831);
        g.fill(234 + wave, cy, Math.min(332, 241 + wave), cy + 2, alpha(brighten(toColor, 70), 130));
        g.outline(267, 157, 27, 40, alpha(toColor, 78));
        g.outline(271, 161, 19, 32, alpha(brighten(toColor, 55), 108));
        centered(g, "◆", 281, 173, brighten(toColor, 70));
        drawAction(g, 188, 250, 86, "1회 정제", mx, my);
        drawAction(g, 286, 250, 86, "모두 정제", mx, my);
    }

    private void drawCoreWell(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent,
                              String label, AugmentTier tier, boolean input) {
        g.fill(x, y, x + w, y + h, 0xFF070B0F);
        g.outline(x, y, w, h, shade(accent, 0.68));
        g.fill(x + 2, y + 2, x + w - 2, y + 9, tint(GLASS_TOP, accent, 14));
        g.fill(x + 3, y + 3, x + w - 3, y + 4, alpha(brighten(accent, 70), 74));
        centered(g, label, x + w / 2, y + 7, brighten(accent, 55));
        if (input) {
            g.item(new ItemStack(AugmentCoreItems.item(tier)), x + 33, y + 31);
            g.item(new ItemStack(AugmentCoreItems.item(tier)), x + 57, y + 24);
            g.item(new ItemStack(AugmentCoreItems.item(tier)), x + 81, y + 31);
            centered(g, tierLabel(tier) + " ×3", x + w / 2, y + 57, MUTED);
        } else {
            g.item(new ItemStack(AugmentCoreItems.item(tier)), x + 58, y + 29);
            centered(g, tierLabel(tier) + " ×1", x + w / 2, y + 57, MUTED);
        }
    }

    private void drawSynthesisBurst(GuiGraphicsExtractor g) {
        double p = progress(synthesisStarted, 880);
        if (p >= 1.0) return;
        int fromColor = tierColor(synthesisFrom);
        AugmentTier toTier = synthesisFrom.next().orElse(AugmentTier.MYTHIC);
        int toColor = tierColor(toTier);
        boolean confirmed = menu.status() == AugmentDeviceMenu.STATUS_UPGRADED;
        if (!confirmed || p < 0.52) {
            double q = easeInOut(Math.min(1.0, p / 0.52));
            int[][] start = {{126, 177}, {150, 169}, {174, 177}};
            for (int[] s : start) {
                int x = lerp(s[0], 273, q), y = lerp(s[1], 169, q);
                g.item(new ItemStack(AugmentCoreItems.item(synthesisFrom)), x, y);
            }
            int radius = 8 + (int) Math.round(q * 18);
            g.outline(281 - radius, 177 - radius, radius * 2, radius * 2,
                    alpha(brighten(fromColor, 80), (int) (90 + 120 * q)));
            return;
        }
        double q = easeOut((p - 0.52) / 0.48);
        int x = lerp(273, 390, q), radius = 25 - (int) Math.round(q * 13);
        g.outline(281 - radius, 177 - radius, radius * 2, radius * 2,
                alpha(brighten(toColor, 100), (int) (210 * (1.0 - q))));
        g.item(new ItemStack(AugmentCoreItems.item(toTier)), x, 169);
        if (q > 0.62) centered(g, synthesisAll ? "연속 정제 완료" : "정제 완료", 407, 197, brighten(toColor, 70));
    }

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean active, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 19);
        g.fill(x, y, x + w, y + 19, METAL_DARK);
        g.outline(x, y, w, 19, active ? 0xFFCAD7E1 : hover ? 0xFF7F909E : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + 17, active ? SELECTED : hover ? HOVER : PANEL);
        if (active) g.fill(x + 10, y + 16, x + w - 10, y + 18, 0xFFB8CAD7);
        centered(g, text, x + w / 2, y + 6, active ? TEXT : MUTED);
    }

    private void drawAction(GuiGraphicsExtractor g, int x, int y, int w, String text, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 19);
        g.fill(x, y, x + w, y + 19, METAL_DARK);
        g.outline(x, y, w, 19, hover ? METAL_LIGHT : METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + 17, hover ? HOVER : PANEL);
        centered(g, text, x + w / 2, y + 6, hover ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double x = event.x() - leftPos, y = event.y() - topPos;

        if (drawPhase()) {
            if (menu.status() == AugmentDeviceMenu.STATUS_READY) {
                for (int slot = 0; slot < 3; slot++) {
                    if (menu.candidate(slot) != null && inside(x, y, cardX(slot), CARD_Y, CARD_W, CARD_H)) {
                        selectPulseSlot = slot;
                        selectPulseStarted = now();
                        playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.16F + slot * 0.05F);
                        return send(AugmentDeviceMenu.candidateButtonId(slot));
                    }
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        if (menu.advanced()) {
            if (inside(x, y, 16, 40, 112, 19)) {
                upgradeMode = false;
                playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
                return true;
            }
            if (inside(x, y, 136, 40, 112, 19)) {
                upgradeMode = true;
                playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.08F);
                return true;
            }
        }

        if (upgradeMode && menu.advanced()) {
            for (int i = 0; i < AugmentTier.values().length - 1; i++) {
                int tx = 72 + i * 84;
                if (inside(x, y, tx, 89, 76, 19)) {
                    upgradeTierCursor = i;
                    playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.96F + i * 0.05F);
                    return true;
                }
            }
            AugmentTier tier = AugmentTier.values()[upgradeTierCursor];
            if (inside(x, y, 188, 250, 86, 19)) {
                beginSynthesisFeedback(tier, false);
                return send(AugmentDeviceMenu.upgradeOneButtonId(tier));
            }
            if (inside(x, y, 286, 250, 86, 19)) {
                beginSynthesisFeedback(tier, true);
                return send(AugmentDeviceMenu.upgradeAllButtonId(tier));
            }
            return super.mouseClicked(event, doubleClick);
        }

        int top = menu.advanced() ? 72 : 48, gridY = top + 51;
        for (AugmentTier tier : AugmentTier.values()) {
            int index = tier.ordinal(), tx = 18 + (index % 3) * 178, ty = gridY + (index / 3) * 83;
            if (inside(x, y, tx, ty, 166, 69)) {
                playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.92F + tier.ordinal() * 0.04F);
                return send(AugmentDeviceMenu.beginButtonId(tier));
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

    private int countCore(AugmentTier tier) {
        int count = 0;
        for (int i = 0; i < playerInventory.getContainerSize(); i++) {
            ItemStack stack = playerInventory.getItem(i);
            if (!stack.isEmpty() && stack.is(AugmentCoreItems.item(tier))) count += stack.getCount();
        }
        return count;
    }

    private void playUi(SoundEvent sound, float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    private double revealProgress(int slot) {
        if (revealStarted == Long.MIN_VALUE) return 1.0;
        long age = now() - revealStarted - slot * 105L;
        if (age <= 0) return 0.0;
        return clamp01(age / 330.0);
    }

    private AugmentTier selectedTier() {
        int tier = menu.tierOrdinal();
        return tier < 0 || tier >= AugmentTier.values().length ? null : AugmentTier.values()[tier];
    }
    private int selectedTierColor() {
        AugmentTier tier = selectedTier();
        return tier == null ? METAL_LIGHT : tierColor(tier);
    }
    private static String cleanName(String raw) {
        return raw.replace(" (액티브 · 토글)", "").replace(" (액티브)", "");
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
    private static int cardX(int slot) { return DRAW_X + slot * (CARD_W + DRAW_GAP); }
    private void centered(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(font, s, x - font.width(s) / 2, y, color, false);
    }
    private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
    private static int alpha(int argb, int a) { return (clamp(a) << 24) | (argb & 0x00FFFFFF); }
    private static int brighten(int argb, int amount) {
        int r = clamp(((argb >> 16) & 0xFF) + amount), g = clamp(((argb >> 8) & 0xFF) + amount), b = clamp((argb & 0xFF) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static int shade(int argb, double f) {
        int r = clamp((int) (((argb >> 16) & 0xFF) * f)), g = clamp((int) (((argb >> 8) & 0xFF) * f)), b = clamp((int) ((argb & 0xFF) * f));
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
            case AugmentDeviceMenu.STATUS_NO_CORE -> "선택한 등급의 코어가 없습니다";
            case AugmentDeviceMenu.STATUS_FULLY_MAXED -> "이 등급의 모든 증강을 완성했습니다";
            case AugmentDeviceMenu.STATUS_ACQUIRED -> "증강을 획득했습니다";
            case AugmentDeviceMenu.STATUS_INVALID -> "진행 중인 선택을 먼저 완료하거나 다시 시도하세요";
            case AugmentDeviceMenu.STATUS_UPGRADED -> "코어 정제가 완료되었습니다";
            case AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES -> "합성할 코어가 부족합니다";
            default -> "증강 코어를 선택하세요";
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

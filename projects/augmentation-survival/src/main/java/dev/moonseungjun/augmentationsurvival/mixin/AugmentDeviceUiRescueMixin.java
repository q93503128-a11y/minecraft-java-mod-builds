package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.client.AugmentIconClient;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.client.screen.AugmentDeviceScreen;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Playtest-driven presentation pass for the augment device.
 * Keeps all acquisition state/server authority in AugmentDeviceMenu and only replaces client presentation/hit geometry.
 */
@Mixin(value = AugmentDeviceScreen.class, priority = 1300)
public abstract class AugmentDeviceUiRescueMixin extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W = 704;
    private static final int H = 360;
    private static final int TEXT = 0xFFF2F3F1;
    private static final int MUTED = 0xFFA2AAAD;
    private static final int FAINT = 0xFF667075;
    private static final int LINE = 0xFF354047;
    private static final int LINE_SOFT = 0xFF273137;
    private static final int SURFACE = 0xFF111619;
    private static final int SURFACE_HI = 0xFF192126;
    private static final int DEEP = 0xFF080C0F;
    private static final int GOLD = 0xFFE2C679;

    @Shadow private int ownedPage;
    @Shadow private String hoveredOwnedId;
    @Shadow private double revealProgress(int slot) { throw new AssertionError(); }
    @Shadow private boolean shiftDown() { throw new AssertionError(); }
    @Shadow private List<AugmentDefinition> ownedDefinitions() { throw new AssertionError(); }

    @Unique private boolean augmentationSurvival$ownedVisible;
    @Unique private String augmentationSurvival$pendingHover = "";
    @Unique private long augmentationSurvival$hoverSince = Long.MIN_VALUE;

    protected AugmentDeviceUiRescueMixin(
            AugmentDeviceMenu menu,
            Inventory inventory,
            Component title,
            int imageWidth,
            int imageHeight
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    @Inject(method = "extractLabels", at = @At("HEAD"))
    private void augmentationSurvival$beginFrame(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
        augmentationSurvival$ownedVisible = false;
    }

    @Inject(method = "drawAugmentCard", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$drawCard(
            GuiGraphicsExtractor g,
            int ignoredX,
            int ignoredY,
            int ignoredW,
            int ignoredH,
            int slot,
            AugmentDefinition definition,
            int mouseX,
            int mouseY,
            boolean ignoredCompact,
            CallbackInfo ci
    ) {
        ci.cancel();
        OfferGeometry geo = augmentationSurvival$offerGeometry();
        int x = geo.cardLeft() + slot * (geo.cardW() + geo.gap());
        int y = geo.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 10.0);
        int w = geo.cardW();
        int h = geo.cardH();
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(definition);
        int color = tierColor(definition.tier());

        augmentationSurvival$cardAura(g, x, y, w, h, definition.tier(), color, hover, selected);
        augmentationSurvival$cardMaterial(g, x, y, w, h, definition.tier(), color, hover);

        g.text(font, p.kind().label(), x + 10, y + 10,
                p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? MUTED : GOLD, false);
        String control = augmentationSurvival$controlBadge(definition, p);
        if (!control.isBlank() && w >= 88) {
            int bw = font.width(control) + 10;
            int bx = x + w - bw - 9;
            g.fill(bx, y + 7, bx + bw, y + 21, withAlpha(color, 0x1E));
            g.outline(bx, y + 7, bw, 14, withAlpha(brighten(color, 18), 0x70));
            centered(g, control, bx + bw / 2, y + 10, brighten(color, 30));
        }

        boolean tight = w < 118 || h < 176;
        int iconSize = tight ? 30 : 40;
        int iconY = y + (tight ? 29 : 32);
        int iconX = x + (w - iconSize) / 2;
        augmentationSurvival$socket(g, iconX, iconY, iconSize, definition.tier(), color, hover);
        g.item(AugmentIconClient.stackFor(definition.id(), definition.tier()),
                iconX + iconSize / 2 - 8, iconY + iconSize / 2 - 8);

        int nameY = iconY + iconSize + (tight ? 5 : 7);
        centered(g, fit(p.displayName(), w - 18), x + w / 2, nameY, TEXT);
        int now = menu.currentLevel(slot);
        int next = Math.min(now + 1, definition.maxLevel());
        String level = now == 0
                ? "신규 · Lv." + next + "/" + definition.maxLevel()
                : "Lv." + now + " → " + next + "/" + definition.maxLevel();
        centered(g, fit(level, w - 18), x + w / 2, nameY + 12, now == 0 ? color : MUTED);

        int dividerY = nameY + 27;
        g.fill(x + 10, dividerY, x + w - 10, dividerY + 1, withAlpha(brighten(color, 18), 0x60));

        boolean reroll = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;
        int actionY = y + h - 20;
        int bodyTop = dividerY + 7;
        int bodyBottom = reroll ? actionY - 5 : y + h - 9;
        List<String> lines = wrap(p.summary(), Math.max(34, w - 20));
        int bodyHeight = Math.max(8, bodyBottom - bodyTop);
        int lineHeight = lines.isEmpty() ? 10 : Math.min(10, Math.max(8, bodyHeight / lines.size()));
        for (int i = 0; i < lines.size(); i++) {
            int yy = bodyTop + i * lineHeight;
            if (yy + 8 > bodyBottom) break;
            g.text(font, lines.get(i), x + 10, yy, 0xFFD6DCDD, false);
        }

        if (reroll) {
            boolean can = menu.rerollsRemaining() > 0 && !selected;
            boolean buttonHover = inside(mouseX, mouseY, x + 8, actionY, w - 16, 14);
            int fill = can && buttonHover ? tint(SURFACE_HI, color, 18) : 0xEC10161B;
            int edge = can ? (buttonHover ? brighten(color, 25) : shade(color, 0.75)) : LINE_SOFT;
            g.fill(x + 8, actionY, x + w - 8, actionY + 14, fill);
            g.outline(x + 8, actionY, w - 16, 14, edge);
            String label = selected ? "선택 해제" : menu.rerollsRemaining() > 0 ? "↻ 재추첨" : "재추첨 소진";
            centered(g, fit(label, w - 24), x + w / 2, actionY + 3,
                    can ? (buttonHover ? TEXT : color) : FAINT);
        } else if (selected) {
            g.fill(x + 10, y + h - 7, x + w - 10, y + h - 4, brighten(color, 30));
        }
    }

    @Inject(method = "drawOffer", at = @At("TAIL"))
    private void augmentationSurvival$drawShiftDetail(
            GuiGraphicsExtractor g,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        if (!shiftDown()) return;
        OfferGeometry geo = augmentationSurvival$offerGeometry();
        AugmentDefinition hovered = null;
        for (int slot = 0; slot < 3; slot++) {
            AugmentDefinition d = menu.candidate(slot);
            if (d == null) continue;
            int x = geo.cardLeft() + slot * (geo.cardW() + geo.gap());
            int y = geo.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 10.0);
            if (inside(mouseX, mouseY, x, y, geo.cardW(), geo.cardH())) {
                hovered = d;
                break;
            }
        }
        if (hovered == null) return;
        augmentationSurvival$popover(g, hovered, ClientAugmentProgressState.level(hovered.id()),
                hovered == null ? "" : AugmentPresentationCatalog.get(hovered).detail(),
                mouseX, mouseY, true, augmentationSurvival$bounds());
    }

    @Inject(method = "drawOwned", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$drawOwned(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
        ci.cancel();
        augmentationSurvival$ownedVisible = true;
        Bounds b = augmentationSurvival$bounds();
        List<AugmentDefinition> owned = ownedDefinitions();
        OwnedGeometry geo = augmentationSurvival$ownedGeometry(b);
        int pages = Math.max(1, (owned.size() + geo.pageSize() - 1) / geo.pageSize());
        ownedPage = Math.max(0, Math.min(ownedPage, pages - 1));
        hoveredOwnedId = "";

        g.text(font, "보유 증강", geo.left(), geo.headerY(), TEXT, false);
        String count = owned.size() + " / " + AugmentCatalog.allDefinitions().size();
        g.text(font, count, geo.left() + font.width("보유 증강") + 8, geo.headerY(), 0xFF91B9B3, false);
        String help = geo.width() >= 330 ? "마우스: 카드 정보 · Shift: 정확한 수치" : "마우스 정보 · Shift 상세";
        g.text(font, help, geo.right() - font.width(help), geo.headerY(), MUTED, false);

        int start = ownedPage * geo.pageSize();
        int end = Math.min(owned.size(), start + geo.pageSize());
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % geo.columns();
            int row = local / geo.columns();
            int x = geo.gridLeft() + col * (geo.cellW() + geo.gapX());
            int y = geo.gridTop() + row * (geo.cellH() + geo.gapY());
            augmentationSurvival$ownedCell(g, x, y, geo.cellW(), geo.cellH(), owned.get(i), mouseX, mouseY);
        }
        if (owned.isEmpty()) {
            centered(g, "아직 획득한 증강이 없습니다.", (geo.left() + geo.right()) / 2, geo.gridTop() + 25, MUTED);
        }
        if (pages > 1) {
            boolean leftHover = ownedPage > 0 && inside(mouseX, mouseY, geo.leftPagerX(), geo.pagerY(), 28, 16);
            boolean rightHover = ownedPage + 1 < pages && inside(mouseX, mouseY, geo.rightPagerX(), geo.pagerY(), 28, 16);
            g.text(font, "‹", geo.leftPagerX() + 10, geo.pagerY() + 3, leftHover ? TEXT : FAINT, false);
            centered(g, (ownedPage + 1) + " / " + pages, (geo.left() + geo.right()) / 2, geo.pagerY() + 3, MUTED);
            g.text(font, "›", geo.rightPagerX() + 10, geo.pagerY() + 3, rightHover ? TEXT : FAINT, false);
        }

        long now = System.currentTimeMillis();
        if (!hoveredOwnedId.equals(augmentationSurvival$pendingHover)) {
            augmentationSurvival$pendingHover = hoveredOwnedId;
            augmentationSurvival$hoverSince = now;
        }
        if (!hoveredOwnedId.isBlank() && now - augmentationSurvival$hoverSince >= 90L) {
            AugmentDefinition d = AugmentCatalog.require(hoveredOwnedId);
            AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
            augmentationSurvival$popover(g, d, ClientAugmentProgressState.level(d.id()),
                    shiftDown() ? p.detail() : p.summary(), mouseX, mouseY, shiftDown(), b);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        double mx = event.x() - leftPos;
        double my = event.y() - topPos;
        if (menu.status() == AugmentDeviceMenu.STATUS_READY && event.button() == 0) {
            OfferGeometry geo = augmentationSurvival$offerGeometry();
            boolean reroll = ClientAugmentProgressState.level(AugmentCatalog.REROLL_ID) > 0;
            for (int slot = 0; slot < 3; slot++) {
                if (menu.candidate(slot) == null) continue;
                int x = geo.cardLeft() + slot * (geo.cardW() + geo.gap());
                int y = geo.cardTop() + (int) Math.round((1.0 - revealProgress(slot)) * 10.0);
                boolean selected = (menu.selectedMask() & (1 << slot)) != 0;
                if (reroll) {
                    int actionY = y + geo.cardH() - 20;
                    if (inside(mx, my, x + 8, actionY, geo.cardW() - 16, 14)) {
                        if (selected) augmentationSurvival$send(AugmentDeviceMenu.candidateButtonId(slot));
                        else if (menu.rerollsRemaining() > 0) augmentationSurvival$send(AugmentDeviceMenu.rerollButtonId(slot));
                        cir.setReturnValue(true);
                        return;
                    }
                }
                if (inside(mx, my, x, y, geo.cardW(), geo.cardH())) {
                    augmentationSurvival$send(AugmentDeviceMenu.candidateButtonId(slot));
                    cir.setReturnValue(true);
                    return;
                }
            }
            // Prevent the old compact hit boxes from firing behind the new cards.
            cir.setReturnValue(true);
            return;
        }

        if (augmentationSurvival$ownedVisible && event.button() == 0) {
            List<AugmentDefinition> owned = ownedDefinitions();
            OwnedGeometry geo = augmentationSurvival$ownedGeometry(augmentationSurvival$bounds());
            int pages = Math.max(1, (owned.size() + geo.pageSize() - 1) / geo.pageSize());
            if (pages > 1 && inside(mx, my, geo.leftPagerX(), geo.pagerY(), 28, 16) && ownedPage > 0) {
                ownedPage--;
                cir.setReturnValue(true);
                return;
            }
            if (pages > 1 && inside(mx, my, geo.rightPagerX(), geo.pagerY(), 28, 16) && ownedPage + 1 < pages) {
                ownedPage++;
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private void augmentationSurvival$ownedCell(
            GuiGraphicsExtractor g, int x, int y, int w, int h,
            AugmentDefinition d, int mouseX, int mouseY
    ) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        int color = tierColor(d.tier());
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int level = ClientAugmentProgressState.level(d.id());
        g.fill(x, y, x + w, y + h, hover ? SURFACE_HI : SURFACE);
        g.fill(x, y, x + w, y + 2, color);
        g.outline(x, y, w, h, hover ? brighten(color, 10) : LINE_SOFT);
        int textX = x + 7;
        if (w >= 60) {
            g.fill(x + 6, y + 5, x + 28, y + h - 4, DEEP);
            g.item(AugmentIconClient.stackFor(d.id(), d.tier()), x + 9, y + Math.max(6, (h - 16) / 2));
            textX = x + 34;
        }
        g.text(font, fit(p.displayName(), Math.max(24, x + w - textX - 7)), textX, y + 5,
                hover ? TEXT : 0xFFD5DCDE, false);
        if (h >= 27) {
            g.text(font, "Lv." + level + "/" + d.maxLevel(), textX, y + 17, MUTED, false);
            if (w >= 92) {
                String kind = p.kind().label();
                g.text(font, kind, x + w - 7 - font.width(kind), y + 17,
                        p.kind() == AugmentPresentationCatalog.Kind.PASSIVE ? FAINT : GOLD, false);
            }
        }
        if (hover) hoveredOwnedId = d.id();
    }

    @Unique
    private void augmentationSurvival$popover(
            GuiGraphicsExtractor g,
            AugmentDefinition d,
            int level,
            String body,
            int mouseX,
            int mouseY,
            boolean detailed,
            Bounds b
    ) {
        AugmentPresentationCatalog.Entry p = AugmentPresentationCatalog.get(d);
        int color = tierColor(d.tier());
        int maxW = Math.max(176, b.width() - 18);
        int panelW = Math.min(350, Math.max(220, maxW * 4 / 5));
        panelW = Math.min(panelW, maxW);
        List<String> lines = wrap(body, Math.max(40, panelW - 24));
        int lineH = 10;
        int maxH = Math.max(92, b.height() - 18);
        int panelH = 74 + lines.size() * lineH;
        if (panelH > maxH && panelW < maxW) {
            panelW = maxW;
            lines = wrap(body, Math.max(40, panelW - 24));
            panelH = 74 + lines.size() * lineH;
        }
        if (panelH > maxH && !lines.isEmpty()) {
            lineH = Math.max(8, (maxH - 74) / lines.size());
            panelH = Math.min(maxH, 74 + lines.size() * lineH);
        }
        int px = mouseX + 14;
        if (px + panelW > b.right() - 4) px = mouseX - panelW - 14;
        px = Math.max(b.left() + 4, Math.min(px, b.right() - panelW - 4));
        int py = mouseY + 12;
        if (py + panelH > b.bottom() - 4) py = mouseY - panelH - 12;
        py = Math.max(b.top() + 4, Math.min(py, b.bottom() - panelH - 4));

        augmentationSurvival$softAura(g, px, py, panelW, panelH, d.tier(), color, false);
        g.fill(px - 3, py - 3, px + panelW + 3, py + panelH + 3, 0xB0000000);
        g.fill(px, py, px + panelW, py + panelH, 0xFA090E11);
        g.outline(px, py, panelW, panelH, brighten(color, 8));
        g.fill(px, py, px + panelW, py + 3, color);
        g.fill(px + 10, py + 11, px + 42, py + 43, DEEP);
        g.outline(px + 10, py + 11, 32, 32, shade(color, 0.84));
        g.item(AugmentIconClient.stackFor(d.id(), d.tier()), px + 18, py + 19);
        int tx = px + 50;
        g.text(font, fit(p.displayName(), panelW - 62), tx, py + 12, TEXT, false);
        String meta = tierLabel(d.tier()) + " · " + p.kind().label() + " · Lv." + level + "/" + d.maxLevel();
        g.text(font, fit(meta, panelW - 62), tx, py + 26, color, false);
        g.fill(px + 10, py + 48, px + panelW - 10, py + 49, LINE_SOFT);
        for (int i = 0; i < lines.size(); i++) {
            int yy = py + 54 + i * lineH;
            if (yy + 8 > py + panelH - 17) break;
            g.text(font, lines.get(i), px + 12, yy, detailed ? 0xFFE4E8E8 : 0xFFD1D8DA, false);
        }
        String footer = detailed ? "Shift · 정확한 수치 표시 중" : "Shift · 정확한 수치";
        g.text(font, footer, px + 12, py + panelH - 14, detailed ? color : FAINT, false);
    }

    @Unique
    private void augmentationSurvival$cardAura(
            GuiGraphicsExtractor g, int x, int y, int w, int h,
            AugmentTier tier, int color, boolean hover, boolean selected
    ) {
        augmentationSurvival$softAura(g, x, y, w, h, tier, color, hover);
        g.fill(x - 3, y - 3, x + w + 3, y + h + 4, 0x56000000);
        g.fill(x, y, x + w, y + h, DEEP);
        int outer = selected ? brighten(color, 60)
                : hover ? brighten(color, 22)
                : tier.ordinal() >= AugmentTier.RARE.ordinal() ? shade(color, 0.80) : 0xFF596268;
        g.outline(x, y, w, h, outer);
        g.outline(x + 3, y + 3, w - 6, h - 6,
                tier.ordinal() >= AugmentTier.EPIC.ordinal() ? withAlpha(brighten(color, 18), 0xA0) : LINE_SOFT);
    }

    @Unique
    private void augmentationSurvival$softAura(
            GuiGraphicsExtractor g, int x, int y, int w, int h,
            AugmentTier tier, int color, boolean hover
    ) {
        double wave = (Math.sin(System.currentTimeMillis() / 230.0) + 1.0) * 0.5;
        switch (tier) {
            case COMMON -> { }
            case UNCOMMON -> g.fill(x - 3, y - 3, x + w + 3, y + h + 3, withAlpha(color, hover ? 0x18 : 0x0D));
            case RARE -> {
                g.fill(x - 5, y - 5, x + w + 5, y + h + 5, withAlpha(color, hover ? 0x20 : 0x12));
                g.fill(x + 8, y - 2, x + w - 8, y, withAlpha(brighten(color, 40), 0x78));
            }
            case EPIC -> {
                g.fill(x - 8, y - 8, x + w + 8, y + h + 8, withAlpha(color, 0x12));
                g.fill(x - 4, y - 4, x + w + 4, y + h + 4, withAlpha(color, hover ? 0x2A : 0x20));
            }
            case LEGENDARY -> {
                int pulse = 0x12 + (int) Math.round(wave * 0x0E);
                g.fill(x - 12, y - 12, x + w + 12, y + h + 12, withAlpha(color, pulse));
                g.fill(x - 7, y - 7, x + w + 7, y + h + 7, withAlpha(color, 0x24));
                g.fill(x + 10, y - 4, x + w - 10, y - 1, withAlpha(brighten(color, 62), 0xD2));
                g.fill(x + 16, y + h + 1, x + w - 16, y + h + 3, withAlpha(color, 0x8A));
            }
            case MYTHIC -> {
                int outer = 0x14 + (int) Math.round(wave * 0x12);
                int mid = 0x28 + (int) Math.round(wave * 0x18);
                g.fill(x - 14, y - 14, x + w + 14, y + h + 14, withAlpha(color, outer));
                g.fill(x - 9, y - 9, x + w + 9, y + h + 9, withAlpha(color, mid));
                g.fill(x - 5, y - 5, x + w + 5, y + h + 5, withAlpha(brighten(color, 24), 0x42));
                g.fill(x + 8, y - 5, x + w - 8, y - 1, withAlpha(brighten(color, 72), 0xDE));
                g.fill(x + 8, y + h + 1, x + w - 8, y + h + 5, withAlpha(color, 0xA4));
            }
        }
    }

    @Unique
    private void augmentationSurvival$cardMaterial(
            GuiGraphicsExtractor g, int x, int y, int w, int h,
            AugmentTier tier, int color, boolean hover
    ) {
        int inset = 4;
        g.fill(x + inset, y + inset, x + w - inset, y + h - inset, 0xFF080C0F);
        int upper = y + Math.max(60, h * 44 / 100);
        int tintAmount = switch (tier) {
            case COMMON -> 2;
            case UNCOMMON -> 4;
            case RARE -> 6;
            case EPIC -> 9;
            case LEGENDARY -> 12;
            case MYTHIC -> 14;
        };
        g.fill(x + 5, y + 5, x + w - 5, upper, tint(0xFF0A1014, color, tintAmount));
        g.fill(x + 7, y + 7, x + w - 7, y + 8, withAlpha(0xFFFFFFFF, hover ? 0x24 : 0x16));
        g.fill(x + 10, y + 11, x + Math.max(11, w * 3 / 5), y + 12, withAlpha(brighten(color, 55), hover ? 0x24 : 0x14));
        int texture = withAlpha(brighten(color, 42), hover ? 0x14 : 0x0B);
        for (int i = 0; i < 4; i++) {
            int yy = y + 18 + i * 6;
            if (yy >= upper - 4) break;
            int xx = x + 9 + i * 5;
            g.fill(xx, yy, Math.min(x + w - 9, xx + Math.max(18, w / 3)), yy + 1, texture);
        }
        int lower = y + h - Math.max(17, h / 8);
        g.fill(x + 5, lower, x + w - 5, y + h - 5, withAlpha(color, 0x0D));
        g.fill(x + 10, lower, x + w - 10, lower + 1, withAlpha(color, 0x34));
    }

    @Unique
    private void augmentationSurvival$socket(
            GuiGraphicsExtractor g, int x, int y, int size,
            AugmentTier tier, int color, boolean hover
    ) {
        if (tier == AugmentTier.EPIC) {
            g.fill(x - 3, y - 3, x + size + 3, y + size + 3, withAlpha(color, 0x20));
        } else if (tier == AugmentTier.LEGENDARY) {
            g.fill(x - 6, y - 6, x + size + 6, y + size + 6, withAlpha(color, 0x1D));
            g.fill(x - 3, y - 3, x + size + 3, y + size + 3, withAlpha(brighten(color, 28), 0x36));
        } else if (tier == AugmentTier.MYTHIC) {
            double wave = (Math.sin(System.currentTimeMillis() / 220.0) + 1.0) * 0.5;
            int alpha = 0x24 + (int) Math.round(wave * 0x18);
            g.fill(x - 7, y - 7, x + size + 7, y + size + 7, withAlpha(color, 0x18));
            g.fill(x - 4, y - 4, x + size + 4, y + size + 4, withAlpha(brighten(color, 30), alpha));
        }
        g.fill(x, y, x + size, y + size, tint(DEEP, color, tier.ordinal() >= AugmentTier.EPIC.ordinal() ? 13 : 5));
        g.outline(x, y, size, size, hover ? brighten(color, 30) : shade(color, 0.84));
        g.outline(x + 3, y + 3, size - 6, size - 6, withAlpha(brighten(color, 20), 0x8A));
        g.fill(x + 4, y + 4, x + size - 4, y + 5, withAlpha(0xFFFFFFFF, 0x28));
    }

    @Unique
    private OfferGeometry augmentationSurvival$offerGeometry() {
        Bounds b = augmentationSurvival$bounds();
        int usableW = b.width();
        int usableH = b.height();
        int gap = usableW >= 620 ? 9 : usableW >= 480 ? 7 : 4;
        int cardW = Math.min(220, Math.max(42, (usableW - gap * 2) / 3));
        int groupW = cardW * 3 + gap * 2;
        int cardLeft = b.left() + Math.max(0, (usableW - groupW) / 2);
        boolean compact = cardW < 150 || usableH < 282;
        int headerTop = b.top() + 4;
        int cardTop = headerTop + (compact ? 29 : 39);
        int footerY = b.bottom() - 7;
        int cardH = Math.min(264, Math.max(98, footerY - cardTop - 3));
        if (cardTop + cardH > footerY - 2) cardH = Math.max(94, footerY - cardTop - 2);
        return new OfferGeometry(cardLeft, cardTop, cardW, cardH, gap);
    }

    @Unique
    private OwnedGeometry augmentationSurvival$ownedGeometry(Bounds b) {
        int left = b.left() + 10;
        int right = b.right() - 10;
        int width = Math.max(40, right - left);
        int headerY = b.top() + 31;
        int gridTop = headerY + 20;
        int pagerY = b.bottom() - 20;
        int gridBottom = pagerY - 4;
        int columns = width >= 360 ? 3 : width >= 240 ? 2 : 1;
        int gapX = width >= 430 ? 7 : 5;
        int gapY = 4;
        int cellH = gridBottom - gridTop >= 110 ? 30 : 27;
        int cellW = Math.max(54, (width - gapX * (columns - 1)) / columns);
        int rows = Math.max(1, (gridBottom - gridTop + gapY) / (cellH + gapY));
        int pageSize = Math.max(1, columns * rows);
        int gridW = cellW * columns + gapX * (columns - 1);
        int gridLeft = left + Math.max(0, (width - gridW) / 2);
        int center = (left + right) / 2;
        return new OwnedGeometry(left, right, width, headerY, gridLeft, gridTop, cellW, cellH,
                gapX, gapY, columns, rows, pageSize, pagerY, center - 48, center + 20);
    }

    @Unique
    private Bounds augmentationSurvival$bounds() {
        int scaledW = minecraft == null ? W : minecraft.getWindow().getGuiScaledWidth();
        int scaledH = minecraft == null ? H : minecraft.getWindow().getGuiScaledHeight();
        int left = Math.max(8, -leftPos + 8);
        int right = Math.min(W - 8, scaledW - leftPos - 8);
        int top = Math.max(8, -topPos + 8);
        int bottom = Math.min(H - 8, scaledH - topPos - 8);
        if (right <= left) { left = 8; right = W - 8; }
        if (bottom <= top) { top = 8; bottom = H - 8; }
        return new Bounds(left, right, top, bottom);
    }

    @Unique
    private boolean augmentationSurvival$send(int button) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!menu.clickMenuButton(minecraft.player, button)) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        return true;
    }

    @Unique
    private String augmentationSurvival$controlBadge(AugmentDefinition d, AugmentPresentationCatalog.Entry p) {
        String id = d.id();
        if (id.endsWith(":pockets") || id.endsWith(":remote_storage")) return "B";
        if (id.endsWith(":spatial_link")) return "G";
        if (id.endsWith(":authority_theft")) return "J";
        if (p.kind() != AugmentPresentationCatalog.Kind.PASSIVE) return "K·Alt";
        return "";
    }

    @Unique
    private List<String> wrap(String value, int maxWidth) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            if (font.width(word) > maxWidth) {
                if (line.length() > 0) { out.add(line.toString()); line.setLength(0); }
                StringBuilder part = new StringBuilder();
                for (int offset = 0; offset < word.length();) {
                    int cp = word.codePointAt(offset);
                    String next = part.toString() + new String(Character.toChars(cp));
                    if (part.length() > 0 && font.width(next) > maxWidth) { out.add(part.toString()); part.setLength(0); }
                    part.appendCodePoint(cp);
                    offset += Character.charCount(cp);
                }
                line.append(part);
                continue;
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) <= maxWidth) line = new StringBuilder(candidate);
            else { if (line.length() > 0) out.add(line.toString()); line = new StringBuilder(word); }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    @Unique
    private String fit(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        String result = value;
        while (!result.isEmpty() && font.width(result + "…") > maxWidth) {
            int end = result.offsetByCodePoints(result.length(), -1);
            result = result.substring(0, end);
        }
        return result.isEmpty() ? "…" : result + "…";
    }

    @Unique private void centered(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(font, s, x - font.width(s) / 2, y, color, false);
    }
    @Unique private static boolean inside(double x, double y, int l, int t, int w, int h) {
        return x >= l && x < l + w && y >= t && y < t + h;
    }
    @Unique private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 255) + amount);
        int gg = Math.min(255, ((color >> 8) & 255) + amount);
        int b = Math.min(255, (color & 255) + amount);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    @Unique private static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor);
        int gg = (int) (((color >> 8) & 255) * factor);
        int b = (int) ((color & 255) * factor);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    @Unique private static int tint(int base, int color, int percent) {
        int inv = 100 - percent;
        int r = (((base >> 16) & 255) * inv + ((color >> 16) & 255) * percent) / 100;
        int gg = (((base >> 8) & 255) * inv + ((color >> 8) & 255) * percent) / 100;
        int b = ((base & 255) * inv + (color & 255) * percent) / 100;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
    @Unique private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
    @Unique private static int tierColor(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> 0xFFB8BEC5;
            case UNCOMMON -> 0xFF4FC77F;
            case RARE -> 0xFF4394E4;
            case EPIC -> 0xFFAA58D4;
            case LEGENDARY -> 0xFFE4A934;
            case MYTHIC -> 0xFFE44B4B;
        };
    }
    @Unique private static String tierLabel(AugmentTier tier) {
        return switch (tier) {
            case COMMON -> "일반";
            case UNCOMMON -> "고급";
            case RARE -> "희귀";
            case EPIC -> "영웅";
            case LEGENDARY -> "전설";
            case MYTHIC -> "신화";
        };
    }

    @Unique private record Bounds(int left, int right, int top, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    @Unique private record OfferGeometry(int cardLeft, int cardTop, int cardW, int cardH, int gap) { }
    @Unique private record OwnedGeometry(
            int left, int right, int width, int headerY,
            int gridLeft, int gridTop, int cellW, int cellH, int gapX, int gapY,
            int columns, int rows, int pageSize, int pagerY, int leftPagerX, int rightPagerX
    ) { }
}

package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.client.ClientActiveLoadoutState;
import dev.moonseungjun.augmentationsurvival.client.screen.ActiveLoadoutScreen;
import dev.moonseungjun.augmentationsurvival.network.ActiveLoadoutEditPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Double-click fast path resolved from the actual click geometry, never stale hover state. */
@Mixin(value = ActiveLoadoutScreen.class, priority = 1400)
abstract class ActiveLoadoutQuickClickMixin extends Screen {
    @Shadow private int selectedSlot;
    @Shadow private String selectedAugmentId;
    @Shadow private int page;

    protected ActiveLoadoutQuickClickMixin() {
        super(Component.empty());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$doubleClickQuickEquip(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!doubleClick || event.button() != 0) return;

        Geometry layout = augmentationSurvival$layout();
        List<String> slots = ClientActiveLoadoutState.slots();

        // Double-clicking an equipped quick slot is always an immediate unequip.
        for (int slot = 0; slot < Math.min(10, slots.size()); slot++) {
            int sx = layout.slotStartX + slot * (layout.slotW + layout.slotGap);
            if (!inside(event.x(), event.y(), sx, layout.slotY, layout.slotW, layout.slotH)) continue;
            if (!slots.get(slot).isBlank()) {
                sendClear(slot);
                selectedSlot = -1;
                selectedAugmentId = "";
            }
            cir.setReturnValue(true);
            return;
        }

        List<String> available = ClientActiveLoadoutState.available();
        Grid grid = augmentationSurvival$grid(layout);
        int pages = Math.max(1, (available.size() + grid.pageSize - 1) / grid.pageSize);
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * grid.pageSize;
        int end = Math.min(available.size(), start + grid.pageSize);

        for (int index = start; index < end; index++) {
            int local = index - start;
            int col = local % grid.columns;
            int row = local / grid.columns;
            int x = grid.left + col * (grid.cellW + grid.gapX);
            int y = grid.top + row * (grid.cellH + grid.gapY);
            if (!inside(event.x(), event.y(), x, y, grid.cellW, grid.cellH)) continue;

            String id = available.get(index);
            int equippedSlot = slots.indexOf(id);
            if (equippedSlot >= 0) {
                sendClear(equippedSlot);
            } else {
                int emptySlot = slots.indexOf("");
                if (emptySlot >= 0 && emptySlot < 10) {
                    ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                            ActiveLoadoutEditPayload.ASSIGN, emptySlot, 0, id
                    ));
                } else {
                    // A full bar requires an explicit replacement choice rather than silently evicting one.
                    selectedAugmentId = id;
                    selectedSlot = -1;
                    cir.setReturnValue(true);
                    return;
                }
            }
            selectedSlot = -1;
            selectedAugmentId = "";
            cir.setReturnValue(true);
            return;
        }
    }

    private void sendClear(int slot) {
        ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                ActiveLoadoutEditPayload.CLEAR, slot, 0, ""
        ));
    }

    private Geometry augmentationSurvival$layout() {
        int margin = width >= 300 && height >= 220 ? 8 : 4;
        int w = Math.max(1, Math.min(720, width - margin * 2));
        int h = Math.max(1, Math.min(390, height - margin * 2));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        boolean compact = w < 540 || h < 300;
        int gap = w >= 620 ? 5 : w >= 430 ? 4 : w >= 300 ? 2 : 1;
        int budget = Math.max(1, w - 28);
        int rawW = Math.max(1, (budget - gap * 9) / 10);
        int slotW = Math.min(49, Math.max(10, rawW));
        if (slotW * 10 + gap * 9 > budget) slotW = rawW;
        int slotH = compact ? Math.max(28, Math.min(34, h / 7)) : 48;
        int total = slotW * 10 + gap * 9;
        int slotStartX = x + (w - total) / 2;
        int slotY = y + (compact ? 47 : 53);
        int listY = slotY + slotH + (compact ? 7 : 11);
        int footerY = y + h - (compact ? 20 : 24);
        int listH = Math.max(48, footerY - listY);
        int listX = x + 11;
        int listW = Math.max(40, w - 22);
        return new Geometry(slotStartX, slotY, slotW, slotH, gap, listX, listY, listW, listH);
    }

    private Grid augmentationSurvival$grid(Geometry layout) {
        int columns = layout.listW >= 520 ? 5 : layout.listW >= 340 ? 4 : layout.listW >= 240 ? 3 : 2;
        int gapX = 5;
        int gapY = 3;
        int cellH = 27;
        int left = layout.listX + 9;
        int top = layout.listY + 32;
        int usableW = Math.max(20, layout.listW - 18);
        int cellW = Math.max(36, (usableW - gapX * (columns - 1)) / columns);
        int pagerReserve = 17;
        int rows = Math.max(1, (layout.listH - 32 - pagerReserve + gapY) / (cellH + gapY));
        return new Grid(left, top, cellW, cellH, gapX, gapY, columns, Math.max(1, columns * rows));
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    private record Geometry(
            int slotStartX,
            int slotY,
            int slotW,
            int slotH,
            int slotGap,
            int listX,
            int listY,
            int listW,
            int listH
    ) { }

    private record Grid(
            int left,
            int top,
            int cellW,
            int cellH,
            int gapX,
            int gapY,
            int columns,
            int pageSize
    ) { }
}

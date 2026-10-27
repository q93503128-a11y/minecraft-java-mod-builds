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

/** Double-click is the fast path; the existing single-click select/swap flow remains unchanged. */
@Mixin(value = ActiveLoadoutScreen.class, priority = 1400)
abstract class ActiveLoadoutQuickClickMixin extends Screen {
    @Shadow private String hoveredId;
    @Shadow private int selectedSlot;
    @Shadow private String selectedAugmentId;

    protected ActiveLoadoutQuickClickMixin() {
        super(Component.empty());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$doubleClickQuickEquip(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!doubleClick || event.button() != 0 || hoveredId == null || hoveredId.isBlank()) return;

        List<String> slots = ClientActiveLoadoutState.slots();
        for (int slot = 0; slot < slots.size(); slot++) {
            if (!hoveredId.equals(slots.get(slot))) continue;
            ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                    ActiveLoadoutEditPayload.CLEAR, slot, 0, ""
            ));
            selectedSlot = -1;
            selectedAugmentId = "";
            cir.setReturnValue(true);
            return;
        }

        for (int slot = 0; slot < Math.min(10, slots.size()); slot++) {
            if (!slots.get(slot).isBlank()) continue;
            ClientPlayNetworking.send(new ActiveLoadoutEditPayload(
                    ActiveLoadoutEditPayload.ASSIGN, slot, 0, hoveredId
            ));
            selectedSlot = -1;
            selectedAugmentId = "";
            cir.setReturnValue(true);
            return;
        }

        // No free slot: keep the item selected so a following single click can replace/swap deliberately.
        selectedAugmentId = hoveredId;
        selectedSlot = -1;
        cir.setReturnValue(true);
    }
}

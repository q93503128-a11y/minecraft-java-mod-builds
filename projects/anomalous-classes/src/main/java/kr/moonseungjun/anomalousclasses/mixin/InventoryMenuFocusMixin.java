package kr.moonseungjun.anomalousclasses.mixin;

import kr.moonseungjun.anomalousclasses.item.ClassFocusItems;
import kr.moonseungjun.anomalousclasses.state.ClassFocusEquipmentContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds exactly one real synchronized equipment slot to the existing vanilla
 * inventory menu. Index 46 follows vanilla's 0-45 slots without renumbering
 * crafting, armor, inventory, hotbar or offhand slots.
 *
 * Its position is a minimal logical anchor. UI styling/visual integration is
 * intentionally left to the UI owner and must not change the server slot rules.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuFocusMixin extends AbstractContainerMenu {
    @Unique private static final int anomalousclasses$focusIndex = 46;
    @Unique private static final int anomalousclasses$focusX = 77;
    @Unique private static final int anomalousclasses$focusY = 35;

    protected InventoryMenuFocusMixin(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void anomalousclasses$addFocusSlot(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        // The server-side backing store resolves the profile lazily because
        // inventoryMenu is constructed before ServerPlayer finishes initializing.
        Container backing = owner instanceof ServerPlayer serverPlayer
                ? new ClassFocusEquipmentContainer(serverPlayer)
                : new SimpleContainer(1);
        this.addSlot(new Slot(backing, ClassFocusEquipmentContainer.FOCUS_SLOT,
                anomalousclasses$focusX, anomalousclasses$focusY) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.isEmpty() || stack.getCount() != 1 || ClassFocusItems.identity(stack).isEmpty()) {
                    return false;
                }
                // Client data is only a prediction. The server makes the real
                // class/ownership check in ClassFocusEquipmentContainer.
                return !(owner instanceof ServerPlayer) || backing.canPlaceItem(0, stack);
            }
        });
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void anomalousclasses$quickMoveFocus(Player player, int index,
                                                   CallbackInfoReturnable<ItemStack> cir) {
        if (this.slots.size() <= anomalousclasses$focusIndex) return;
        Slot focus = this.slots.get(anomalousclasses$focusIndex);
        if (index == anomalousclasses$focusIndex) {
            ItemStack original = focus.getItem();
            if (original.isEmpty()) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            ItemStack remaining = original.copy();
            // Only the 36 vanilla player-inventory/hotbar slots, not crafting
            // inputs, armor or offhand. A full inventory leaves the focus intact.
            if (!this.moveItemStackTo(remaining, 9, 45, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            focus.set(remaining);
            focus.setChanged();
            cir.setReturnValue(original);
            return;
        }
        if (index < 9 || index >= 45) return;
        Slot source = this.slots.get(index);
        ItemStack original = source.getItem();
        if (original.isEmpty() || focus.hasItem() || !focus.mayPlace(original)) return;
        ItemStack extracted = source.remove(1);
        if (extracted.isEmpty()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        focus.set(extracted);
        source.setChanged();
        focus.setChanged();
        cir.setReturnValue(extracted.copy());
    }
}

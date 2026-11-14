package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.item.ClassFocusItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The single class-focus equipment slot, backed by the existing player profile.
 *
 * <p>Only a server-side menu should create this container. Client menus should use a
 * one-slot dummy container and let vanilla slot synchronization supply the stack.
 * There is no separate item store: the old saved focus remains authoritative, so
 * previously equipped items are preserved across this UI migration.</p>
 */
public final class ClassFocusEquipmentContainer implements Container {
    public static final int FOCUS_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    private final ServerPlayer owner;
    private final ClassProfileSavedData profiles;
    private final PlayerClassProfile profile;

    public ClassFocusEquipmentContainer(ServerPlayer owner) {
        if (owner == null || owner.getServer() == null) {
            throw new IllegalArgumentException("The equipment container requires a server player");
        }
        this.owner = owner;
        this.profiles = ClassProfileSavedData.get(owner.getServer());
        this.profile = profiles.profile(owner.getUUID());
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return profile.equippedFocus().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != FOCUS_SLOT) return ItemStack.EMPTY;
        // This is a read-only snapshot; all mutations must go through Container methods.
        return profile.equippedFocus().map(ClassFocusItems::stack).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != FOCUS_SLOT || amount <= 0) return ItemStack.EMPTY;
        ItemStack removed = getItem(slot);
        if (!removed.isEmpty()) setFocus(null);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != FOCUS_SLOT || stack == null) return;
        if (stack.isEmpty()) {
            setFocus(null);
            return;
        }
        // An item can be inserted only if the server currently approves its class.
        if (!canPlaceItem(slot, stack)) return;
        setFocus(ClassFocusItems.identity(stack).orElseThrow());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != FOCUS_SLOT || stack == null || stack.isEmpty() || stack.getCount() != 1) return false;
        if (!stillValid(owner)) return false;
        ClassFocus focus = ClassFocusItems.identity(stack).orElse(null);
        return focus != null && profile.currentClass().filter(focus.classId()::equals).isPresent();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        // Preserve vanilla Container notification semantics for menu observers.
        profiles.setDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive() && !owner.isRemoved() && owner.getServer() != null;
    }

    @Override
    public void clearContent() {
        setFocus(null);
    }

    private void setFocus(ClassFocus next) {
        if (profile.equipFocus(next)) profiles.setDirty();
    }
}

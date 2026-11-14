package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.item.ClassFocusItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A profile-backed server inventory slot. The backing profile is resolved lazily:
 * vanilla constructs Player.inventoryMenu in the Player superclass constructor,
 * before ServerPlayer has finished initializing its server reference.
 *
 * There is deliberately no second stored ItemStack. The established persisted
 * equippedFocus field owns the item, including equipment saved by older builds.
 */
public final class ClassFocusEquipmentContainer implements Container {
    public static final int FOCUS_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    private final ServerPlayer owner;

    public ClassFocusEquipmentContainer(ServerPlayer owner) {
        if (owner == null) throw new IllegalArgumentException("A server player is required");
        this.owner = owner;
    }

    private ClassProfileSavedData saved() {
        MinecraftServer server = owner.getServer();
        return server == null ? null : ClassProfileSavedData.get(server);
    }

    private PlayerClassProfile profile() {
        ClassProfileSavedData data = saved();
        return data == null ? null : data.profile(owner.getUUID());
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return getItem(FOCUS_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != FOCUS_SLOT) return ItemStack.EMPTY;
        PlayerClassProfile playerProfile = profile();
        if (playerProfile == null) return ItemStack.EMPTY;
        // A snapshot, not a mutable reference to another inventory.
        return playerProfile.equippedFocus().map(ClassFocusItems::stack).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != FOCUS_SLOT || amount <= 0) return ItemStack.EMPTY;
        ItemStack previous = getItem(slot);
        if (!previous.isEmpty()) change(null);
        return previous;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != FOCUS_SLOT || stack == null) return;
        if (stack.isEmpty()) {
            change(null);
            return;
        }
        if (!canPlaceItem(slot, stack)) return;
        change(ClassFocusItems.identity(stack).orElseThrow());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != FOCUS_SLOT || stack == null || stack.isEmpty() || stack.getCount() != 1) return false;
        PlayerClassProfile playerProfile = profile();
        if (playerProfile == null || !stillValid(owner)) return false;
        ClassFocus focus = ClassFocusItems.identity(stack).orElse(null);
        return focus != null && playerProfile.currentClass().filter(focus.classId()::equals).isPresent();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        ClassProfileSavedData data = saved();
        if (data != null) data.setDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive() && !owner.isRemoved() && owner.getServer() != null;
    }

    @Override
    public void clearContent() {
        change(null);
    }

    private void change(ClassFocus focus) {
        ClassProfileSavedData data = saved();
        if (data != null && data.profile(owner.getUUID()).equipFocus(focus)) {
            data.setDirty();
        }
    }
}

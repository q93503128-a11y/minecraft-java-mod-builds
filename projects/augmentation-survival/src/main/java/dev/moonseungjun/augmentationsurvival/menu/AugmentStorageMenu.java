package dev.moonseungjun.augmentationsurvival.menu;

import dev.moonseungjun.augmentationsurvival.augment.storage.StorageAugmentService;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative portable inventory for Pockets and the paged shared Remote Storage view. */
public final class AugmentStorageMenu extends AbstractContainerMenu {
    public static final int POCKET_SLOTS = 9;
    public static final int REMOTE_VIEW_SLOTS = 54;
    public static final int STORAGE_SLOT_COUNT = POCKET_SLOTS + REMOTE_VIEW_SLOTS;

    /** Shared compact layout contract used by the menu hit boxes and its client screen. */
    public static final int GRID_STEP = 17;
    public static final int STORAGE_X = 6;
    public static final int POCKET_Y = 20;
    public static final int REMOTE_Y = 54;
    public static final int PLAYER_X = 162;
    public static final int PLAYER_Y = 20;
    public static final int HOTBAR_Y = 78;

    private static final int DATA_POCKET_UNLOCKED = 0;
    private static final int DATA_HAS_REMOTE = 1;
    private static final int DATA_REMOTE_MEMBERS = 2;
    private static final int DATA_REMOTE_PAGE = 3;
    private static final int DATA_REMOTE_PAGES = 4;
    private static final int DATA_COUNT = 5;
    private static final int BUTTON_PREVIOUS_PAGE = 0;
    private static final int BUTTON_NEXT_PAGE = 1;

    private final Container pocket;
    private final Container remote;
    private final ContainerData data;
    private final ServerPlayer serverOwner;
    private final UUID ownerId;

    public AugmentStorageMenu(int containerId, Inventory inventory, ServerPlayer owner) {
        this(
                containerId,
                inventory,
                StorageAugmentService.pocketContainer(owner),
                StorageAugmentService.remoteView(owner),
                new SimpleContainerData(DATA_COUNT),
                owner
        );
        StorageAugmentService.registerOpenMenu(this);
        refreshServerData();
    }

    private AugmentStorageMenu(
            int containerId,
            Inventory inventory,
            Container pocket,
            Container remote,
            ContainerData data,
            ServerPlayer serverOwner
    ) {
        super(AugmentStorageMenus.STORAGE, containerId);
        this.pocket = pocket;
        this.remote = remote;
        this.data = data;
        this.serverOwner = serverOwner;
        this.ownerId = serverOwner == null ? inventory.player.getUUID() : serverOwner.getUUID();
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
        addStorageSlots();
        addPlayerSlots(inventory);
    }

    public static AugmentStorageMenu client(int containerId, Inventory inventory) {
        return new AugmentStorageMenu(
                containerId,
                inventory,
                new SimpleContainer(POCKET_SLOTS),
                new SimpleContainer(REMOTE_VIEW_SLOTS),
                new SimpleContainerData(DATA_COUNT),
                null
        );
    }

    private void addStorageSlots() {
        for (int slot = 0; slot < POCKET_SLOTS; slot++) {
            final int index = slot;
            addSlot(new ActiveStorageSlot(
                    pocket,
                    slot,
                    STORAGE_X + slot * GRID_STEP,
                    POCKET_Y,
                    () -> index < pocketUnlocked()
            ));
        }
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = row * 9 + column;
                final int index = slot;
                addSlot(new ActiveStorageSlot(
                        remote,
                        slot,
                        STORAGE_X + column * GRID_STEP,
                        REMOTE_Y + row * GRID_STEP,
                        () -> remoteSlotActive(index)
                ));
            }
        }
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        PLAYER_X + column * GRID_STEP,
                        PLAYER_Y + row * GRID_STEP
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_X + column * GRID_STEP, HOTBAR_Y));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != BUTTON_PREVIOUS_PAGE && buttonId != BUTTON_NEXT_PAGE) return false;
        if (!(remote instanceof StorageAugmentService.RemoteView view)) return true;
        view.setPage(view.page() + (buttonId == BUTTON_NEXT_PAGE ? 1 : -1));
        refreshServerData();
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        refreshServerData();
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (slotIndex < STORAGE_SLOT_COUNT) {
            moved = moveItemStackTo(stack, STORAGE_SLOT_COUNT, slots.size(), true);
        } else {
            moved = moveItemStackTo(stack, 0, STORAGE_SLOT_COUNT, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return serverOwner == null || (player == serverOwner && StorageAugmentService.hasAnyStorage(serverOwner));
    }

    @Override
    public void removed(Player player) {
        if (serverOwner != null) StorageAugmentService.unregisterOpenMenu(this);
        super.removed(player);
    }

    public int pocketUnlocked() { return data.get(DATA_POCKET_UNLOCKED); }
    public boolean hasRemote() { return data.get(DATA_HAS_REMOTE) != 0; }
    public int remoteMemberCount() { return data.get(DATA_REMOTE_MEMBERS); }
    public int remotePage() { return data.get(DATA_REMOTE_PAGE); }
    public int remotePageCount() { return data.get(DATA_REMOTE_PAGES); }
    public UUID ownerId() { return ownerId; }
    public ServerPlayer serverOwner() { return serverOwner; }

    public boolean remoteSlotActive(int localSlot) {
        if (!hasRemote()) return false;
        int global = remotePage() * REMOTE_VIEW_SLOTS + localSlot;
        return global >= 0 && global < remoteMemberCount() * 27;
    }

    public boolean referencesSegment(StorageAugmentService.SegmentKind kind, UUID owner) {
        if (kind == StorageAugmentService.SegmentKind.POCKET) return ownerId.equals(owner);
        return remote instanceof StorageAugmentService.RemoteView view && view.references(owner);
    }

    public static int previousPageButtonId() { return BUTTON_PREVIOUS_PAGE; }
    public static int nextPageButtonId() { return BUTTON_NEXT_PAGE; }

    private void refreshServerData() {
        if (serverOwner == null) return;
        data.set(DATA_POCKET_UNLOCKED, StorageAugmentService.unlockedPocketSlots(serverOwner));
        data.set(DATA_HAS_REMOTE, StorageAugmentService.hasRemoteStorage(serverOwner) ? 1 : 0);
        if (remote instanceof StorageAugmentService.RemoteView view) {
            view.setPage(view.page());
            data.set(DATA_REMOTE_MEMBERS, view.memberCount());
            data.set(DATA_REMOTE_PAGE, view.page());
            data.set(DATA_REMOTE_PAGES, view.pageCount());
        }
    }

    private static final class ActiveStorageSlot extends Slot {
        private final BooleanSupplier active;

        private ActiveStorageSlot(Container container, int slot, int x, int y, BooleanSupplier active) {
            super(container, slot, x, y);
            this.active = active;
        }

        @Override public boolean isActive() { return active.getAsBoolean(); }
        @Override public boolean mayPlace(ItemStack stack) { return isActive(); }
        @Override public boolean mayPickup(Player player) { return isActive(); }
    }
}

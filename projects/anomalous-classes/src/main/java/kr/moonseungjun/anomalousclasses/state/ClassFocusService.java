package kr.moonseungjun.anomalousclasses.state;

import java.util.Optional;
import kr.moonseungjun.anomalousclasses.item.ClassFocusItems;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.ClassFocusActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.ClassFocusStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

/** Server-authoritative storage and inventory transactions for the one shared class-focus slot. */
public final class ClassFocusService {
    private ClassFocusService() {}

    public enum Result {
        OPEN(0), EQUIPPED(1), UNEQUIPPED(2), NO_ITEM(3), WRONG_CLASS(4),
        INVENTORY_FULL(5), STALE_STATE(6), INVALID_ACTION(7);

        private final int code;
        Result(int code) { this.code = code; }
        public int code() { return code; }
    }

    public static void open(ServerPlayer player) {
        ClassProfileSavedData saved = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = saved.profile(player.getUUID());
        ClassId active = profile.currentClass().orElse(null);
        if (active == null) {
            ClassSelectionService.openMenu(player);
            return;
        }
        send(player, profile, active, Result.OPEN);
    }

    public static void handle(ServerPlayer player, ClassFocusActionPayload payload) {
        ClassProfileSavedData saved = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = saved.profile(player.getUUID());
        ClassId active = profile.currentClass().orElse(null);
        if (active == null) {
            ClassSelectionService.openMenu(player);
            return;
        }

        Result result;
        if (payload.expectedRevision() < 0 || payload.expectedRevision() != profile.revision()) {
            result = Result.STALE_STATE;
        } else if (payload.action() == ClassFocusActionPayload.EQUIP) {
            result = equip(player, saved, profile, active, payload.tierIndex());
        } else if (payload.action() == ClassFocusActionPayload.UNEQUIP) {
            result = unequip(player, saved, profile);
        } else {
            result = Result.INVALID_ACTION;
        }
        send(player, profile, active, result);
    }

    private static Result equip(ServerPlayer player, ClassProfileSavedData saved, PlayerClassProfile profile,
                                ClassId active, int tierIndex) {
        FocusTier tier = FocusTier.fromIndex(tierIndex).orElse(null);
        if (tier == null) return Result.INVALID_ACTION;
        ClassFocus requested = new ClassFocus(active, tier);
        Optional<ClassFocus> existing = profile.equippedFocus();
        if (existing.filter(requested::equals).isPresent()) return Result.EQUIPPED;

        Inventory inventory = player.getInventory();
        int sourceSlot = -1;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (inventory.getItem(slot).is(ClassFocusItems.item(requested))) {
                sourceSlot = slot;
                break;
            }
        }
        if (sourceSlot < 0) return Result.NO_ITEM;

        ItemStack source = inventory.getItem(sourceSlot);
        if (source.getCount() < 1 || source.getCount() > 1) return Result.INVALID_ACTION;

        ItemStack returned = existing.map(ClassFocusItems::stack).orElse(ItemStack.EMPTY);
        inventory.setItem(sourceSlot, returned);
        inventory.setChanged();
        profile.equipFocus(requested);
        saved.setDirty();
        return Result.EQUIPPED;
    }

    private static Result unequip(ServerPlayer player, ClassProfileSavedData saved, PlayerClassProfile profile) {
        ClassFocus existing = profile.equippedFocus().orElse(null);
        if (existing == null) return Result.UNEQUIPPED;

        Inventory inventory = player.getInventory();
        int emptySlot = -1;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                emptySlot = slot;
                break;
            }
        }
        if (emptySlot < 0) return Result.INVENTORY_FULL;

        inventory.setItem(emptySlot, ClassFocusItems.stack(existing));
        inventory.setChanged();
        profile.equipFocus(null);
        saved.setDirty();
        return Result.UNEQUIPPED;
    }

    /** The dedicated slot follows the world keep-inventory rule on death. */
    public static void onPlayerRespawn(ServerPlayer player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        ClassProfileSavedData saved = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = saved.profile(player.getUUID());
        ClassFocus focus = profile.equippedFocus().orElse(null);
        if (focus == null) return;
        profile.equipFocus(null);
        saved.setDirty();
        player.drop(ClassFocusItems.stack(focus), true);
    }

    private static void send(ServerPlayer player, PlayerClassProfile profile, ClassId active, Result result) {
        ClassFocus equipped = profile.equippedFocus().orElse(null);
        ClassFocusStatePayload state = new ClassFocusStatePayload(
                ProfileStatePayload.from(player, profile, active),
                equipped == null ? -1 : equipped.classId().networkIndex(),
                equipped == null ? -1 : equipped.tier().index(), result.code());
        ClassNetwork.sendToPlayer(player, state);
    }
}

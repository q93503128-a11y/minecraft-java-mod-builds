package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Reads companion content from the same physical settlement storage used by every other system.
 * This does not create an abstract inventory and does not hard-depend on any companion mod class.
 */
public final class SettlementExternalContentService {
    private static final Set<String> EXTERNAL_WEAPON_NAMESPACES = Set.of("weaponsexpanded");

    private SettlementExternalContentService() {}

    public record Snapshot(boolean storageLoaded, long expeditionRelics, long externalWeapons) {}

    public static Snapshot snapshot(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return new Snapshot(false, 0L, 0L);

        long relics = 0L;
        long weapons = 0L;
        for (BlockPos pos : SettlementStorageService.storagePositions(data)) {
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) continue;
                if (stack.is(ExternalContentTags.EXPEDITION_RELICS)) relics += stack.getCount();
                if (isExternalWeapon(stack)) weapons += stack.getCount();
            }
        }
        return new Snapshot(true, relics, weapons);
    }

    public static boolean isExternalWeapon(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;
        String namespace = id.getNamespace();
        if (FrontierSettlement.MOD_ID.equals(namespace) || "minecraft".equals(namespace)) return false;
        return EXTERNAL_WEAPON_NAMESPACES.contains(namespace);
    }
}

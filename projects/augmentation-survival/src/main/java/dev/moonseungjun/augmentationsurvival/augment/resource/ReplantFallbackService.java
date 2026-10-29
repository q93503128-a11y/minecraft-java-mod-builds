package dev.moonseungjun.augmentationsurvival.augment.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Tracks the harvesting player so delayed replant can consume a seed that was picked up immediately. */
public final class ReplantFallbackService {
    private static final int OWNER_CONTEXT_LIFETIME_TICKS = 20 * 10;
    private static final Map<Key, OwnerContext> OWNERS = new HashMap<>();

    private ReplantFallbackService() { }

    public static void record(ServerLevel level, BlockPos pos, ServerPlayer player) {
        int expiresAtTick = level.getServer().getTickCount() + OWNER_CONTEXT_LIFETIME_TICKS;
        OWNERS.put(
                new Key(level.getServer(), level.dimension(), pos.immutable()),
                new OwnerContext(player.getUUID(), expiresAtTick)
        );
    }

    public static void discard(ServerLevel level, BlockPos pos) {
        OWNERS.remove(new Key(level.getServer(), level.dimension(), pos.immutable()));
    }

    public static boolean consumeOwnerInventory(ServerLevel level, BlockPos pos, Item plantingItem) {
        Key key = new Key(level.getServer(), level.dimension(), pos.immutable());
        OwnerContext context = OWNERS.remove(key);
        if (context == null || isExpired(context, level.getServer().getTickCount())) return false;
        UUID ownerId = context.ownerId();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return false;

        Inventory inventory = owner.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.is(plantingItem)) continue;
            stack.shrink(1);
            inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            owner.inventoryMenu.broadcastChanges();
            return true;
        }
        return false;
    }

    public static void clear(MinecraftServer server) {
        OWNERS.keySet().removeIf(key -> key.server == server);
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<Key, OwnerContext>> iterator = OWNERS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Key, OwnerContext> entry = iterator.next();
                if (entry.getKey().server == server && isExpired(entry.getValue(), server.getTickCount())) {
                    iterator.remove();
                }
            }
        });
    }

    private static boolean isExpired(OwnerContext context, int currentTick) {
        return currentTick > context.expiresAtTick();
    }

    private record Key(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) { }
    private record OwnerContext(UUID ownerId, int expiresAtTick) { }
}

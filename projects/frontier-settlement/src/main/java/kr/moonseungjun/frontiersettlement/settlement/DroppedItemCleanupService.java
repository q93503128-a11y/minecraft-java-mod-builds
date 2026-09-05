package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Bounded lag-pressure relief for large mining and civil-work drop fields.
 *
 * Normal gameplay is untouched: the scan runs every five seconds, only activates when the local
 * ItemEntity count is already excessive, gives ordinary rock drops a grace period for pickup, and
 * deletes only an explicit low-value vanilla rock whitelist. Ores, coal, gems, equipment, food,
 * wood, relics and modded drops are never selected by this automatic path.
 */
public final class DroppedItemCleanupService {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final double HORIZONTAL_RADIUS = 128.0D;
    private static final double VERTICAL_RADIUS = 64.0D;
    private static final int PRESSURE_TRIGGER_ENTITIES = 160;
    private static final int PRESSURE_TARGET_ENTITIES = 96;
    private static final int NORMAL_GRACE_TICKS = 400;
    private static final int EMERGENCY_TRIGGER_ENTITIES = 400;
    private static final int EMERGENCY_GRACE_TICKS = 100;

    private DroppedItemCleanupService() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                trimAround(level, player);
            }
        }
    }

    private static void trimAround(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(HORIZONTAL_RADIUS, VERTICAL_RADIUS, HORIZONTAL_RADIUS);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
        if (drops.size() < PRESSURE_TRIGGER_ENTITIES) return;

        int grace = drops.size() >= EMERGENCY_TRIGGER_ENTITIES ? EMERGENCY_GRACE_TICKS : NORMAL_GRACE_TICKS;
        int needToRemove = drops.size() - PRESSURE_TARGET_ENTITIES;
        if (needToRemove <= 0) return;

        for (ItemEntity drop : drops) {
            if (needToRemove <= 0) break;
            if (!drop.isAlive() || drop.getAge() < grace || !isDisposableRock(drop.getItem())) continue;
            drop.discard();
            needToRemove--;
        }
    }

    public static boolean isDisposableRock(ItemStack stack) {
        return stack.is(Items.STONE)
                || stack.is(Items.COBBLESTONE)
                || stack.is(Items.DEEPSLATE)
                || stack.is(Items.COBBLED_DEEPSLATE)
                || stack.is(Items.ANDESITE)
                || stack.is(Items.DIORITE)
                || stack.is(Items.GRANITE)
                || stack.is(Items.TUFF)
                || stack.is(Items.CALCITE)
                || stack.is(Items.DRIPSTONE_BLOCK)
                || stack.is(Items.BLACKSTONE)
                || stack.is(Items.BASALT)
                || stack.is(Items.NETHERRACK);
    }
}

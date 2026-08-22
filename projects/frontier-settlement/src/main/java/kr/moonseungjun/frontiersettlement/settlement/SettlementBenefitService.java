package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class SettlementBenefitService {
    private static final int REPAIR_INTERVAL_TICKS = 100;
    private static final int GUARD_CHECK_INTERVAL_TICKS = 200;
    private static final double BLACKSMITH_RADIUS_SQR = 10.0D * 10.0D;
    private static final int REPAIR_PER_METAL = 16;
    private static final EquipmentSlot[] REPAIR_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private SettlementBenefitService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        int tick = server.getTickCount();
        if (tick % REPAIR_INTERVAL_TICKS == 0) repairNearbyEquipment(server, data);
        if (tick % GUARD_CHECK_INTERVAL_TICKS == 0) maintainGuards(server.overworld(), data);
    }

    private static void repairNearbyEquipment(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        boolean changed = false;
        for (BuildingRecord blacksmith : buildings(data, BuildingType.BLACKSMITH)) {
            BlockPos work = blacksmith.workCenter();
            for (ServerPlayer player : level.players()) {
                if (player.blockPosition().distSqr(work) > BLACKSMITH_RADIUS_SQR) continue;
                ItemStack damaged = mostDamagedEquippedItem(player);
                if (damaged.isEmpty()) continue;
                if (!SettlementStorageService.consumeMetal(level, data, 1L)) return;
                damaged.setDamageValue(Math.max(0, damaged.getDamageValue() - REPAIR_PER_METAL));
                changed = true;
            }
        }
        if (changed) {
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
    }

    private static ItemStack mostDamagedEquippedItem(ServerPlayer player) {
        ItemStack best = ItemStack.EMPTY;
        int bestDamage = 0;
        for (EquipmentSlot slot : REPAIR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;
            int damage = stack.getDamageValue();
            if (damage > bestDamage) {
                best = stack;
                bestDamage = damage;
            }
        }
        return best;
    }

    private static void maintainGuards(ServerLevel level, SettlementData data) {
        for (BuildingRecord post : buildings(data, BuildingType.GUARD_POST)) {
            BlockPos center = post.workCenter();
            String identity = guardIdentity(post);
            AABB search = new AABB(center).inflate(16.0D, 8.0D, 16.0D);
            List<IronGolem> existing = level.getEntitiesOfClass(IronGolem.class, search,
                    guard -> guard.getCustomName() != null && identity.equals(guard.getCustomName().getString()));
            if (!existing.isEmpty()) continue;

            IronGolem guard = new IronGolem(EntityTypes.IRON_GOLEM, level);
            guard.setPos(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
            guard.setCustomName(Component.literal(identity));
            guard.setCustomNameVisible(false);
            guard.setPersistenceRequired();
            guard.setPlayerCreated(true);
            level.addFreshEntity(guard);
        }
    }

    private static String guardIdentity(BuildingRecord post) {
        return "개척 경비대 [" + post.originX() + "," + post.originZ() + "]";
    }

    private static List<BuildingRecord> buildings(SettlementData data, BuildingType type) {
        return data.buildings().stream().filter(building -> building.buildingType() == type).toList();
    }
}

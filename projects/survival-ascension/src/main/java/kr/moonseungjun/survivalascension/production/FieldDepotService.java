package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FieldDepotService {
    public static final int REGISTER_RADIUS = 4;
    public static final int SUPPLY_RADIUS = 32;

    private FieldDepotService() {}

    public static void toggleNearest(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f크리에이티브/관전자 상태에서는 거점을 등록할 수 없습니다."));
            return;
        }
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.INDUSTRIAL_WORKS)) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f먼저 §b산업 가공소§f를 완공해야 합니다."));
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos barrel = findNearestBarrel(level, player.blockPosition());
        if (barrel == null) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f4블록 이내에 등록할 §6배럴§f이 없습니다."));
            return;
        }
        if (!level.mayInteract(player, barrel)) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f이 배럴에는 상호작용할 권한이 없습니다."));
            return;
        }

        String dimension = level.dimension().toString();
        FieldDepotData depots = FieldDepotData.get(player);
        if (depots.owns(player, dimension, barrel)) {
            depots.remove(player, dimension, barrel);
            OutpostService.onDepotRemoved(player, dimension, barrel);
            player.sendSystemMessage(Component.literal("§3[현장 물류 해제] §f배럴 거점 연결을 해제했습니다. §7전초기지 승격도 함께 해제되며 보급권/승격 재료는 반환되지 않습니다."));
            return;
        }
        if (depots.count(player) >= FieldDepotData.MAX_DEPOTS_PER_PLAYER) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f등록 가능한 거점은 플레이어당 최대 §e"
                    + FieldDepotData.MAX_DEPOTS_PER_PLAYER + "개§f입니다."));
            return;
        }

        ProductionData production = ProductionData.get(player);
        if (production.supplyCharges(player) <= 0) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f거점 등록에는 §e현장 보급권 1개§f가 필요합니다."));
            return;
        }

        FieldDepotData.AddResult result = depots.add(player, dimension, barrel);
        if (result == FieldDepotData.AddResult.CLAIMED_BY_OTHER) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f이 배럴은 다른 플레이어의 물류 거점으로 이미 등록되어 있습니다."));
            return;
        }
        if (result != FieldDepotData.AddResult.ADDED) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f거점을 등록하지 못했습니다. §7(" + result.name() + ")"));
            return;
        }
        if (!production.consumeSupplyCharge(player)) {
            depots.remove(player, dimension, barrel);
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f보급권 상태가 바뀌어 등록을 취소했습니다."));
            return;
        }
        player.sendSystemMessage(Component.literal("§b[현장 물류 등록] §f배럴 §e" + barrel.getX() + ", " + barrel.getY() + ", " + barrel.getZ()
                + "§f을 거점으로 연결했습니다. §7같은 차원 반경 " + SUPPLY_RADIUS + "블록에서 대량 건축/관개가 재료를 인출합니다."));
    }

    public static void sendStatus(ServerPlayer player) {
        FieldDepotData data = FieldDepotData.get(player);
        List<FieldDepotData.DepotEntry> depots = data.depots(player);
        int active = activeDepotCount(player);
        player.sendSystemMessage(Component.literal("§3[현장 물류] §f등록 §e" + depots.size() + "/" + FieldDepotData.MAX_DEPOTS_PER_PLAYER
                + " §7· 현재 사용 가능 §a" + active + " §7· 일반 반경 " + SUPPLY_RADIUS + " / 전초 " + OutpostService.EXTENDED_SUPPLY_RADIUS));
        if (depots.isEmpty()) {
            player.sendSystemMessage(Component.literal("  §7- 4블록 내 배럴에서 '물류 거점 연결'을 선택하면 보급권1로 등록합니다."));
            return;
        }
        String currentDimension = player.level().dimension().toString();
        for (FieldDepotData.DepotEntry depot : depots) {
            BlockPos pos = depot.pos();
            boolean sameDimension = depot.dimension().equals(currentDimension);
            boolean outpost = OutpostService.isOutpost(player, depot);
            String state = sameDimension && isUsableBarrel(player, depot) ? "§a사용 가능" : "§8비활성/미로딩";
            player.sendSystemMessage(Component.literal("  §7- §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                    + " §7· " + (outpost ? "§2전초기지 §7· " : "") + state + (sameDimension ? "" : " §7· 다른 차원")));
        }
    }

    public static boolean hasMaterial(ServerPlayer player, Item item) { return countMaterial(player, item) > 0; }

    public static int countMaterial(ServerPlayer player, Item item) {
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) found += stack.getCount();
        }
        for (Container container : usableContainers(player)) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.is(item)) found += stack.getCount();
            }
        }
        return found;
    }

    public static boolean consumeOne(ServerPlayer player, Item item) { return consume(player, item, 1); }

    public static boolean consume(ServerPlayer player, Item item, int amount) {
        if (amount <= 0 || countMaterial(player, item) < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        player.getInventory().setChanged();
        for (Container container : usableContainers(player)) {
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.is(item)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
            container.setChanged();
            if (remaining <= 0) break;
        }
        return remaining == 0;
    }

    public static int activeDepotCount(ServerPlayer player) { return usableContainers(player).size(); }

    private static List<Container> usableContainers(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        String dimension = level.dimension().toString();
        FieldDepotData data = FieldDepotData.get(player);
        List<FieldDepotData.DepotEntry> depots = new ArrayList<>(data.depots(player));
        depots.sort(Comparator.comparingDouble(depot -> depot.pos().distSqr(player.blockPosition())));
        List<Container> out = new ArrayList<>();
        for (FieldDepotData.DepotEntry depot : depots) {
            if (!depot.dimension().equals(dimension)) continue;
            int radius = OutpostService.isActiveForLogistics(player, depot) ? OutpostService.EXTENDED_SUPPLY_RADIUS : SUPPLY_RADIUS;
            BlockPos pos = depot.pos();
            if (pos.distSqr(player.blockPosition()) > radius * radius) continue;
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).is(Blocks.BARREL)) {
                data.remove(player, depot);
                OutpostService.onDepotRemoved(player, depot.dimension(), pos);
                continue;
            }
            if (!level.mayInteract(player, pos)) continue;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) out.add(container);
        }
        return out;
    }

    private static boolean isUsableBarrel(ServerPlayer player, FieldDepotData.DepotEntry depot) {
        ServerLevel level = (ServerLevel) player.level();
        if (!depot.dimension().equals(level.dimension().toString())) return false;
        int radius = OutpostService.isActiveForLogistics(player, depot) ? OutpostService.EXTENDED_SUPPLY_RADIUS : SUPPLY_RADIUS;
        BlockPos pos = depot.pos();
        if (pos.distSqr(player.blockPosition()) > radius * radius) return false;
        if (!level.hasChunkAt(pos)) return false;
        if (!level.getBlockState(pos).is(Blocks.BARREL)) {
            FieldDepotData.get(player).remove(player, depot);
            OutpostService.onDepotRemoved(player, depot.dimension(), pos);
            return false;
        }
        return level.mayInteract(player, pos) && level.getBlockEntity(pos) instanceof Container;
    }

    private static BlockPos findNearestBarrel(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -REGISTER_RADIUS; dx <= REGISTER_RADIUS; dx++) {
            for (int dy = -REGISTER_RADIUS; dy <= REGISTER_RADIUS; dy++) {
                for (int dz = -REGISTER_RADIUS; dz <= REGISTER_RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(Blocks.BARREL)) continue;
                    double distance = pos.distSqr(origin);
                    if (distance > REGISTER_RADIUS * REGISTER_RADIUS || distance >= bestDistance) continue;
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (!(blockEntity instanceof Container)) continue;
                    best = pos.immutable();
                    bestDistance = distance;
                }
            }
        }
        return best;
    }
}

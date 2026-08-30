package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.Comparator;

public final class OutpostService {
    public static final int UPGRADE_RADIUS = 4;
    public static final int STRUCTURE_RADIUS = 5;
    public static final int ACTIVE_OWNER_RADIUS = 64;
    public static final int EXTENDED_SUPPLY_RADIUS = 64;
    public static final int SAFE_RADIUS = 24;
    public static final int SUPPLY_CHARGE_COST = 2;
    public static final int IRON_COST = 32;
    public static final int GOLD_COST = 8;
    public static final int COAL_COST = 32;

    private OutpostService() {}

    public static void upgradeNearest(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f크리에이티브/관전자 상태에서는 전초기지를 승격할 수 없습니다."));
            return;
        }
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.INDUSTRIAL_WORKS)) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f먼저 §b산업 가공소§f를 완공해야 합니다."));
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        FieldDepotData.DepotEntry depot = nearestOwnedDepot(player, UPGRADE_RADIUS);
        if (depot == null) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f4블록 이내에 자신의 등록된 §6물류 통 거점§f이 없습니다."));
            return;
        }
        BlockPos anchor = depot.pos();
        if (!level.mayInteract(player, anchor)) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f이 거점에는 상호작용할 권한이 없습니다."));
            return;
        }
        String dimension = level.dimension().toString();
        OutpostData outposts = OutpostData.get(player);
        if (outposts.isOutpost(player, dimension, anchor)) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f이 물류 거점은 이미 전초기지로 승격되어 있습니다."));
            sendStatus(player);
            return;
        }
        int outpostLimit = FieldDepotData.registrationLimit(player);
        if (outposts.count(player) >= outpostLimit) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f현재 인프라의 전초 한도는 §e" + outpostLimit
                    + "개§f입니다. §7산업 3 · 토목 6 · 승천 중추 9"));
            return;
        }

        StructureCheck check = inspectStructure(player, level, anchor);
        if (!check.complete()) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f배럴 반경 " + STRUCTURE_RADIUS
                    + "블록 안에 §e침대 · 모닥불 · 작업대 · 화로 계열§f이 각각 1개 이상 필요하며 모두 상호작용 가능한 위치여야 합니다."));
            player.sendSystemMessage(Component.literal("  §7현재: 침대 " + yes(check.bed()) + " · 모닥불 " + yes(check.campfire())
                    + " · 작업대 " + yes(check.crafting()) + " · 화로 " + yes(check.furnace())));
            return;
        }

        ProductionData production = ProductionData.get(player);
        if (production.supplyCharges(player) < SUPPLY_CHARGE_COST) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f승격에는 §e현장 보급권 " + SUPPLY_CHARGE_COST + "개§f가 필요합니다."));
            return;
        }
        if (FieldDepotService.countMaterial(player, Items.IRON_INGOT) < IRON_COST
                || FieldDepotService.countMaterial(player, Items.GOLD_INGOT) < GOLD_COST
                || FieldDepotService.countMaterial(player, Items.COAL) < COAL_COST) {
            player.sendSystemMessage(Component.literal("§2[전초기지] §f승격 재료 부족: §7철 주괴 32 · 금 주괴 8 · 석탄 32. 인벤토리와 활성 물류 배럴 재고를 합산합니다."));
            return;
        }

        if (!FieldDepotService.consume(player, Items.IRON_INGOT, IRON_COST)
                || !FieldDepotService.consume(player, Items.GOLD_INGOT, GOLD_COST)
                || !FieldDepotService.consume(player, Items.COAL, COAL_COST)) {
            player.sendSystemMessage(Component.literal("§c[전초기지] §f재료 상태가 바뀌어 승격을 중단했습니다."));
            return;
        }
        if (!production.consumeSupplyCharges(player, SUPPLY_CHARGE_COST)) {
            player.sendSystemMessage(Component.literal("§c[전초기지] §f보급권 상태가 바뀌어 승격을 중단했습니다."));
            return;
        }
        if (!outposts.upgrade(player, dimension, anchor, outpostLimit)) {
            player.sendSystemMessage(Component.literal("§c[전초기지] §f승격 상태가 바뀌어 저장하지 못했습니다."));
            return;
        }

        player.sendSystemMessage(Component.literal("§a[전초기지 완성] §f" + anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ()
                + " §7· 현재 " + outposts.count(player) + "/" + outpostLimit
                + " · 소유자가 64블록 안에 있을 때 물류64 / 자연 적대몹 억제24가 활성화됩니다."));
    }

    public static void sendStatus(ServerPlayer player) {
        OutpostData data = OutpostData.get(player);
        int active = activeCount(player);
        int outpostLimit = FieldDepotData.registrationLimit(player);
        player.sendSystemMessage(Component.literal("§2[전초기지] §f승격 §e" + data.count(player) + "/" + outpostLimit
                + " §7· 현재 활성 §a" + active + " §7· 물류 " + EXTENDED_SUPPLY_RADIUS + " / 안전권 " + SAFE_RADIUS));
        player.sendSystemMessage(Component.literal("  §7- 지역 한도: 산업 3 · 토목 6 · 승천 중추 9"));
        for (OutpostData.OutpostEntry outpost : data.outposts(player)) {
            String state = isActive(player, outpost.dimension(), outpost.pos()) ? "§a활성" : "§8비활성";
            player.sendSystemMessage(Component.literal("  §7- §f" + outpost.pos().getX() + ", " + outpost.pos().getY() + ", " + outpost.pos().getZ()
                    + " §7· " + state + (outpost.dimension().equals(player.level().dimension().toString()) ? "" : " §7· 다른 차원")));
        }
    }

    public static boolean isOutpost(ServerPlayer player, FieldDepotData.DepotEntry depot) {
        return OutpostData.get(player).isOutpost(player, depot.dimension(), depot.pos());
    }

    public static boolean isActiveForLogistics(ServerPlayer player, FieldDepotData.DepotEntry depot) {
        return isOutpost(player, depot) && isActive(player, depot.dimension(), depot.pos());
    }

    public static OutpostData.OutpostEntry nearestActiveOutpost(ServerPlayer player, int radius) {
        String dimension = player.level().dimension().toString();
        BlockPos origin = player.blockPosition();
        double max = radius * radius;
        return OutpostData.get(player).outposts(player).stream()
                .filter(outpost -> outpost.dimension().equals(dimension))
                .filter(outpost -> outpost.pos().distSqr(origin) <= max)
                .filter(outpost -> isActive(player, outpost.dimension(), outpost.pos()))
                .min(Comparator.comparingDouble(outpost -> outpost.pos().distSqr(origin)))
                .orElse(null);
    }

    public static boolean isRecoveryOperational(ServerPlayer player, ServerLevel level, String dimension, BlockPos anchor) {
        if (!dimension.equals(level.dimension().toString())) return false;
        if (!OutpostData.get(player).isOutpost(player, dimension, anchor)) return false;
        if (!level.hasChunkAt(anchor) || !level.getBlockState(anchor).is(Blocks.BARREL)) return false;
        if (!level.mayInteract(player, anchor)) return false;
        return inspectStructure(player, level, anchor).complete();
    }

    public static void onDepotRemoved(ServerPlayer player, String dimension, BlockPos pos) {
        OutpostData.get(player).remove(player, dimension, pos);
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || !(mob.level() instanceof ServerLevel level)) return;
        if (!"NATURAL".equals(event.getSpawnType().name())) return;
        double safeRadiusSq = SAFE_RADIUS * SAFE_RADIUS;
        for (ServerPlayer owner : level.getServer().getPlayerList().getPlayers()) {
            if (owner.isSpectator() || owner.level() != level) continue;
            OutpostData data = OutpostData.get(owner);
            for (OutpostData.OutpostEntry outpost : data.outposts(owner)) {
                if (!outpost.dimension().equals(level.dimension().toString())) continue;
                if (mob.blockPosition().distSqr(outpost.pos()) > safeRadiusSq) continue;
                if (!isActive(owner, outpost.dimension(), outpost.pos())) continue;
                event.setCanceled(true);
                return;
            }
        }
    }

    public static int activeCount(ServerPlayer player) {
        int active = 0;
        for (OutpostData.OutpostEntry outpost : OutpostData.get(player).outposts(player)) {
            if (isActive(player, outpost.dimension(), outpost.pos())) active++;
        }
        return active;
    }

    private static boolean isActive(ServerPlayer player, String dimension, BlockPos anchor) {
        if (!dimension.equals(player.level().dimension().toString())) return false;
        if (anchor.distSqr(player.blockPosition()) > ACTIVE_OWNER_RADIUS * ACTIVE_OWNER_RADIUS) return false;
        return isRecoveryOperational(player, (ServerLevel) player.level(), dimension, anchor);
    }

    private static FieldDepotData.DepotEntry nearestOwnedDepot(ServerPlayer player, int radius) {
        String dimension = player.level().dimension().toString();
        BlockPos origin = player.blockPosition();
        double max = radius * radius;
        return FieldDepotData.get(player).depots(player).stream()
                .filter(depot -> depot.dimension().equals(dimension))
                .filter(depot -> depot.pos().distSqr(origin) <= max)
                .filter(depot -> ((ServerLevel) player.level()).hasChunkAt(depot.pos()))
                .filter(depot -> ((ServerLevel) player.level()).getBlockState(depot.pos()).is(Blocks.BARREL))
                .min(Comparator.comparingDouble(depot -> depot.pos().distSqr(origin)))
                .orElse(null);
    }

    private static StructureCheck inspectStructure(ServerPlayer player, ServerLevel level, BlockPos anchor) {
        boolean bed = false, campfire = false, crafting = false, furnace = false;
        for (int dx = -STRUCTURE_RADIUS; dx <= STRUCTURE_RADIUS; dx++) {
            for (int dy = -STRUCTURE_RADIUS; dy <= STRUCTURE_RADIUS; dy++) {
                for (int dz = -STRUCTURE_RADIUS; dz <= STRUCTURE_RADIUS; dz++) {
                    BlockPos pos = anchor.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof BedBlock) bed = true;
                    if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) campfire = true;
                    if (state.is(Blocks.CRAFTING_TABLE)) crafting = true;
                    if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) furnace = true;
                    if (bed && campfire && crafting && furnace) return new StructureCheck(true, true, true, true);
                }
            }
        }
        return new StructureCheck(bed, campfire, crafting, furnace);
    }

    private static String yes(boolean value) { return value ? "§aO" : "§cX"; }
    private record StructureCheck(boolean bed, boolean campfire, boolean crafting, boolean furnace) {
        boolean complete() { return bed && campfire && crafting && furnace; }
    }
}

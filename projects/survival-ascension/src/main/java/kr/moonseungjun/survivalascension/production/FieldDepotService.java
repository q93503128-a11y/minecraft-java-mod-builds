package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.compat.SharedEconomyCompat;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class FieldDepotService {
    public static final int REGISTER_RADIUS = 4;
    public static final int SUPPLY_RADIUS = 32;
    public static final int MAIN_INVENTORY_FIRST_SLOT = 9;
    public static final int MAIN_INVENTORY_END_EXCLUSIVE = 36;

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
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f4블록 이내에 등록할 §6통§f이 없습니다."));
            return;
        }
        if (!level.mayInteract(player, barrel)) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f이 통에는 상호작용할 권한이 없습니다."));
            return;
        }

        String dimension = level.dimension().toString();
        FieldDepotData depots = FieldDepotData.get(player);
        if (depots.owns(player, dimension, barrel)) {
            depots.remove(player, dimension, barrel);
            OutpostService.onDepotRemoved(player, dimension, barrel);
            player.sendSystemMessage(Component.literal("§3[현장 물류 해제] §f통 거점 연결을 해제했습니다. §7이 앵커의 창고 통 링크와 전초기지 승격도 함께 해제되며 재료는 반환되지 않습니다."));
            return;
        }
        if (depots.isLinkedByOwner(player, dimension, barrel)) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f이 통은 자신의 창고 확장 통입니다. §7먼저 '창고 통 연결'에서 해제하세요."));
            return;
        }
        if (depots.isLinkedByAny(dimension, barrel)) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f이 통은 다른 물류 창고군에 이미 연결되어 있습니다."));
            return;
        }
        int depotLimit = FieldDepotData.registrationLimit(player);
        if (depots.count(player) >= depotLimit) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f현재 인프라의 거점 한도는 §e" + depotLimit
                    + "개§f입니다. §7산업 3 · 토목 6 · 승천 중추 9"));
            return;
        }

        ProductionData production = ProductionData.get(player);
        if (production.supplyCharges(player) <= 0) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f거점 등록에는 §e현장 보급권 1개§f가 필요합니다."));
            return;
        }

        FieldDepotData.AddResult result = depots.add(player, dimension, barrel, depotLimit);
        if (result == FieldDepotData.AddResult.CLAIMED_BY_OTHER) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f이 통은 다른 물류 거점/창고군에 이미 연결되어 있습니다."));
            return;
        }
        if (result == FieldDepotData.AddResult.LIMIT_REACHED) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f현재 인프라의 거점 한도는 §e" + depotLimit
                    + "개§f입니다. §7산업 3 · 토목 6 · 승천 중추 9"));
            return;
        }
        if (result != FieldDepotData.AddResult.ADDED) {
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f거점을 등록하지 못했습니다. §7현재 다른 연결 상태나 거점 한도를 확인하세요."));
            return;
        }
        if (!production.consumeSupplyCharge(player)) {
            depots.remove(player, dimension, barrel);
            player.sendSystemMessage(Component.literal("§3[현장 물류] §f보급권 상태가 바뀌어 등록을 취소했습니다."));
            return;
        }
        player.sendSystemMessage(Component.literal("§b[현장 물류 등록] §f통 §e" + barrel.getX() + ", " + barrel.getY() + ", " + barrel.getZ()
                + "§f을 거점 앵커로 연결했습니다. §7현재 " + depots.count(player) + "/" + depotLimit
                + " · 같은 차원에서 로딩 중이면 거리 제한 없이 사용 · 주변 창고 통 최대 "
                + FieldDepotData.MAX_LINKED_BARRELS_PER_DEPOT + "개 확장 가능"));
    }

    public static void toggleWarehouseNearest(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f크리에이티브/관전자 상태에서는 창고 통을 연결할 수 없습니다."));
            return;
        }
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.INDUSTRIAL_WORKS)) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f먼저 §b산업 가공소§f를 완공해야 합니다."));
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos target = findNearestBarrel(level, player.blockPosition());
        if (target == null) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f4블록 이내에 대상 §6통§f이 없습니다."));
            return;
        }
        if (!level.mayInteract(player, target)) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f이 통에는 상호작용할 권한이 없습니다."));
            return;
        }

        String dimension = level.dimension().toString();
        FieldDepotData data = FieldDepotData.get(player);
        if (data.isRegisteredAnchor(dimension, target)) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f등록 거점 앵커 자체는 확장 통 대상이 아닙니다. §7연결할 통 쪽에 더 가까이 서서 다시 선택하세요."));
            return;
        }
        if (data.isLinkedByOwner(player, dimension, target)) {
            data.removeLink(player, dimension, target);
            player.sendSystemMessage(Component.literal("§3[창고 통 해제] §f통 §e" + coords(target) + "§f을 현재 창고군에서 해제했습니다. §7내용물은 실제 통 안에 그대로 남습니다."));
            return;
        }
        if (data.isLinkedByAny(dimension, target)) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f이 통은 다른 플레이어의 창고군에 이미 연결되어 있습니다."));
            return;
        }

        FieldDepotData.DepotEntry depot = nearestOwnedDepotForTarget(player, level, target);
        if (depot == null) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f이 통에서 §e" + FieldDepotData.MAX_LINK_RADIUS
                    + "블록§f 안에 로딩·상호작용 가능한 자신의 등록 거점 앵커가 없습니다."));
            return;
        }
        FieldDepotData.LinkResult result = data.addLink(player, depot, target);
        if (result == FieldDepotData.LinkResult.LIMIT_REACHED) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f이 거점은 이미 확장 통 §e"
                    + FieldDepotData.MAX_LINKED_BARRELS_PER_DEPOT + "개§f를 사용 중입니다."));
            return;
        }
        if (result != FieldDepotData.LinkResult.ADDED) {
            player.sendSystemMessage(Component.literal("§3[물류 창고군] §f창고 통을 연결하지 못했습니다. §7거점 거리·창고 한도·기존 연결 상태를 확인하세요."));
            return;
        }
        player.sendSystemMessage(Component.literal("§b[창고 통 연결] §f통 §e" + coords(target) + "§f → 거점 §e" + coords(depot.pos())
                + " §7· " + data.linkedCount(player, depot) + "/" + FieldDepotData.MAX_LINKED_BARRELS_PER_DEPOT
                + " · 별도 보급권 없음 · 실제 통 용량 그대로 사용"));
    }

    public static void sendStatus(ServerPlayer player) {
        FieldDepotData data = FieldDepotData.get(player);
        List<FieldDepotData.DepotEntry> depots = data.depots(player);
        int activeDepots = activeDepotCount(player);
        int activeBarrels = activeStorageBarrelCount(player);
        int depotLimit = FieldDepotData.registrationLimit(player);
        player.sendSystemMessage(Component.literal("§3[현장 물류] §f등록 거점 §e" + depots.size() + "/" + depotLimit
                + " §7· 사용 가능 거점 §a" + activeDepots + " §7· 사용 가능 저장 통 §b" + activeBarrels
                + " §7· 같은 차원 로딩 창고는 거리 제한 없음"));
        player.sendSystemMessage(Component.literal("  §7- 지역 한도: 산업 3 · 토목 6 · 승천 중추 9"));
        player.sendSystemMessage(Component.literal("  §7- 등록 방법: 산업 가공소 완공 → 등록할 통에서 4블록 이내 → M→인프라→산업 가공소→물류 거점 연결 · 최초 등록 보급권 1"));
        player.sendSystemMessage(Component.literal("  §7- 확장 방법: 거점 6블록 안의 다른 통에서 창고 통 연결 · 거점당 최대 8개 · 추가 보급권 없음"));
        player.sendSystemMessage(Component.literal("  §7- 이동 방법: 등록된 일반 거점/창고 통을 파괴하면 내용물이 바닥에 쏟아지지 않고 포장된 물류 통 1개로 보존됩니다. 다시 설치하면 연결을 자동 복구합니다."));
        player.sendSystemMessage(Component.literal("  §7- 창고군: 거점 앵커 반경 " + FieldDepotData.MAX_LINK_RADIUS + " 안 실제 통 최대 "
                + FieldDepotData.MAX_LINKED_BARRELS_PER_DEPOT + "개 연결 · 전체 링크 " + data.totalLinkedCount(player)));
        player.sendSystemMessage(Component.literal("  §7- 재료 소비: 같은 차원에서 현재 로딩된 등록 창고 전체를 공용 재고로 사용하고, 부족분만 플레이어 인벤토리에서 사용"));
        player.sendSystemMessage(Component.literal("  §7- 현장 일괄 적재: 주 인벤토리 슬롯9~35의 대량 자원만 가까운 사용 가능 통부터 적재 · 핫바/장비 유지"));
        if (depots.isEmpty()) {
            player.sendSystemMessage(Component.literal("  §7- 4블록 내 통에서 '물류 거점 연결'을 선택하면 보급권1로 등록합니다."));
            return;
        }
        String currentDimension = player.level().dimension().toString();
        for (FieldDepotData.DepotEntry depot : depots) {
            BlockPos pos = depot.pos();
            boolean sameDimension = depot.dimension().equals(currentDimension);
            boolean outpost = OutpostService.isOutpost(player, depot);
            String state = sameDimension && isUsableAnchor(player, depot) ? "§a사용 가능" : "§8비활성/미로딩";
            player.sendSystemMessage(Component.literal("  §7- §f" + coords(pos) + " §7· " + (outpost ? "§2전초기지 §7· " : "")
                    + state + " §7· 창고 §b" + data.linkedCount(player, depot) + "/" + FieldDepotData.MAX_LINKED_BARRELS_PER_DEPOT
                    + (sameDimension ? "" : " §7· 다른 차원")));
        }
    }

    public static boolean hasMaterial(ServerPlayer player, Item item) { return countMaterial(player, item) > 0; }

    public static int countMaterial(ServerPlayer player, Item item) {
        return countMatching(player, stack -> stack.is(item));
    }

    public static int countMatching(ServerPlayer player, Predicate<ItemStack> matcher) {
        if (matcher == null) return 0;
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (matcher.test(stack)) found += stack.getCount();
        }
        for (Container container : usableContainers(player)) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (matcher.test(stack)) found += stack.getCount();
            }
        }
        return found;
    }

    public static boolean consumeOne(ServerPlayer player, Item item) { return consume(player, item, 1); }

    public static boolean consume(ServerPlayer player, Item item, int amount) {
        return consumeMatching(player, stack -> stack.is(item), amount);
    }

    public static boolean consumeMatching(ServerPlayer player, Predicate<ItemStack> matcher, int amount) {
        if (matcher == null || amount <= 0 || countMatching(player, matcher) < amount) return false;
        int remaining = amount;
        List<Container> containers = usableContainers(player);
        for (Container container : containers) {
            boolean changed = false;
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = container.getItem(slot);
                if (!matcher.test(stack)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                changed = true;
            }
            if (changed) container.setChanged();
            if (remaining <= 0) break;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!matcher.test(stack)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return remaining == 0;
    }

    public static int countOffloadableMainInventory(ServerPlayer player) {
        int found = 0;
        int end = Math.min(MAIN_INVENTORY_END_EXCLUSIVE, player.getInventory().getContainerSize());
        for (int slot = MAIN_INVENTORY_FIRST_SLOT; slot < end; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isBulkMaterial(stack)) found += stack.getCount();
        }
        return found;
    }

    public static int offloadBulkMaterials(ServerPlayer player) {
        List<Container> containers = usableContainers(player);
        if (containers.isEmpty()) return 0;
        int moved = 0;
        int end = Math.min(MAIN_INVENTORY_END_EXCLUSIVE, player.getInventory().getContainerSize());
        for (int slot = MAIN_INVENTORY_FIRST_SLOT; slot < end; slot++) {
            ItemStack source = player.getInventory().getItem(slot);
            if (!isBulkMaterial(source)) continue;
            moved += insertIntoContainers(source, containers);
        }
        if (moved > 0) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return moved;
    }

    public static boolean isBulkMaterial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ItemTags.LOGS)
                || stack.is(Items.RAW_IRON) || stack.is(Items.RAW_COPPER) || stack.is(Items.RAW_GOLD)
                || stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT) || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.REDSTONE)
                || stack.is(Items.LAPIS_LAZULI) || stack.is(Items.DIAMOND) || stack.is(Items.EMERALD)
                || stack.is(Items.AMETHYST_SHARD) || stack.is(Items.QUARTZ) || stack.is(Items.ECHO_SHARD)
                || stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.NETHER_STAR) || stack.is(Items.DRAGON_BREATH)
                || stack.is(Items.COBBLESTONE) || stack.is(Items.COBBLED_DEEPSLATE) || stack.is(Items.STONE)
                || stack.is(Items.DEEPSLATE) || stack.is(Items.STONE_BRICKS) || stack.is(Items.NETHERRACK)
                || stack.is(Items.END_STONE) || stack.is(Items.OBSIDIAN) || stack.is(Items.SAND)
                || stack.is(Items.RED_SAND) || stack.is(Items.GRAVEL) || stack.is(Items.DIRT)
                || stack.is(Items.GLASS) || stack.is(Items.SLIME_BALL)
                || stack.is(Items.WHEAT) || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)
                || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.MELON_SEEDS) || stack.is(Items.PUMPKIN_SEEDS);
    }

    public static int activeDepotCount(ServerPlayer player) {
        int active = 0;
        for (FieldDepotData.DepotEntry depot : new ArrayList<>(FieldDepotData.get(player).depots(player))) {
            if (isUsableAnchor(player, depot)) active++;
        }
        return active;
    }

    public static int activeStorageBarrelCount(ServerPlayer player) { return usableContainers(player).size(); }

    private static int insertIntoContainers(ItemStack source, List<Container> containers) {
        int before = source.getCount();
        for (Container container : containers) {
            boolean changed = false;
            for (int slot = 0; slot < container.getContainerSize() && !source.isEmpty(); slot++) {
                ItemStack existing = container.getItem(slot);
                if (existing.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(source, existing)) continue;
                if (!container.canPlaceItem(slot, source)) continue;
                int limit = Math.min(container.getMaxStackSize(source), existing.getMaxStackSize());
                int space = limit - existing.getCount();
                if (space <= 0) continue;
                int move = Math.min(space, source.getCount());
                existing.grow(move);
                source.shrink(move);
                changed = true;
            }
            for (int slot = 0; slot < container.getContainerSize() && !source.isEmpty(); slot++) {
                ItemStack existing = container.getItem(slot);
                if (!existing.isEmpty() || !container.canPlaceItem(slot, source)) continue;
                int limit = Math.min(container.getMaxStackSize(source), source.getMaxStackSize());
                if (limit <= 0) continue;
                int move = Math.min(limit, source.getCount());
                container.setItem(slot, source.copyWithCount(move));
                source.shrink(move);
                changed = true;
            }
            if (changed) container.setChanged();
            if (source.isEmpty()) break;
        }
        return before - source.getCount();
    }

    private static List<Container> usableContainers(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        String dimension = level.dimension().toString();
        FieldDepotData data = FieldDepotData.get(player);
        List<ResolvedContainer> resolved = new ArrayList<>();
        for (FieldDepotData.DepotEntry depot : new ArrayList<>(data.depots(player))) {
            if (!depot.dimension().equals(dimension)) continue;
            BlockPos anchor = depot.pos();
            // Registered logistics is a same-dimension network. Distance is not a gameplay tax;
            // unloaded chunks are still skipped so this never force-loads the world.
            if (!level.hasChunkAt(anchor)) continue;
            if (!SharedEconomyCompat.isLogisticsContainerBlock(level.getBlockState(anchor))) {
                data.remove(player, depot);
                OutpostService.onDepotRemoved(player, depot.dimension(), anchor);
                continue;
            }
            if (!level.mayInteract(player, anchor)) continue;
            BlockEntity anchorEntity = level.getBlockEntity(anchor);
            if (!(anchorEntity instanceof Container anchorContainer)) {
                data.remove(player, depot);
                OutpostService.onDepotRemoved(player, depot.dimension(), anchor);
                continue;
            }
            resolved.add(new ResolvedContainer(anchor, anchorContainer));

            for (FieldDepotData.LinkedBarrel link : new ArrayList<>(data.linkedBarrels(player, depot))) {
                BlockPos pos = link.pos();
                if (!level.hasChunkAt(pos)) continue;
                if (!SharedEconomyCompat.isLogisticsContainerBlock(level.getBlockState(pos))) {
                    data.removeLink(player, dimension, pos);
                    continue;
                }
                if (!level.mayInteract(player, pos)) continue;
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof Container container) {
                    resolved.add(new ResolvedContainer(pos, container));
                } else {
                    data.removeLink(player, dimension, pos);
                }
            }
        }
        appendNearbySharedSupplyDepots(player, level, resolved);
        resolved.sort(Comparator.comparingDouble(value -> value.pos().distSqr(player.blockPosition())));
        return resolved.stream().map(ResolvedContainer::container).toList();
    }

    private static void appendNearbySharedSupplyDepots(ServerPlayer player, ServerLevel level, List<ResolvedContainer> resolved) {
        final int horizontalRadius = 24;
        final int verticalRadius = 8;
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (ResolvedContainer value : resolved) seen.add(value.pos());
        BlockPos origin = player.blockPosition();
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                if (dx * dx + dz * dz > horizontalRadius * horizontalRadius) continue;
                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (seen.contains(pos) || !level.hasChunkAt(pos)) continue;
                    if (!SharedEconomyCompat.isSharedSupplyDepot(level.getBlockState(pos))) continue;
                    if (!level.mayInteract(player, pos)) continue;
                    if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
                    BlockPos immutable = pos.immutable();
                    seen.add(immutable);
                    resolved.add(new ResolvedContainer(immutable, container));
                }
            }
        }
    }

    private static boolean isUsableAnchor(ServerPlayer player, FieldDepotData.DepotEntry depot) {
        ServerLevel level = (ServerLevel) player.level();
        if (!depot.dimension().equals(level.dimension().toString())) return false;
        BlockPos pos = depot.pos();
        // Same-dimension loaded depots are usable at any distance; no chunk tickets are created.
        if (!level.hasChunkAt(pos)) return false;
        if (!SharedEconomyCompat.isLogisticsContainerBlock(level.getBlockState(pos))) {
            FieldDepotData.get(player).remove(player, depot);
            OutpostService.onDepotRemoved(player, depot.dimension(), pos);
            return false;
        }
        if (!level.mayInteract(player, pos)) return false;
        if (!(level.getBlockEntity(pos) instanceof Container)) {
            FieldDepotData.get(player).remove(player, depot);
            OutpostService.onDepotRemoved(player, depot.dimension(), pos);
            return false;
        }
        return true;
    }

    private static FieldDepotData.DepotEntry nearestOwnedDepotForTarget(ServerPlayer player, ServerLevel level, BlockPos target) {
        String dimension = level.dimension().toString();
        double max = FieldDepotData.MAX_LINK_RADIUS * FieldDepotData.MAX_LINK_RADIUS;
        return FieldDepotData.get(player).depots(player).stream()
                .filter(depot -> depot.dimension().equals(dimension))
                .filter(depot -> depot.pos().distSqr(target) <= max)
                .filter(depot -> isPhysicalAnchor(player, level, depot.pos()))
                .min(Comparator.comparingDouble(depot -> depot.pos().distSqr(target)))
                .orElse(null);
    }

    private static boolean isPhysicalAnchor(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        if (!SharedEconomyCompat.isLogisticsContainerBlock(level.getBlockState(pos))) return false;
        if (!level.mayInteract(player, pos)) return false;
        return level.getBlockEntity(pos) instanceof Container;
    }

    private static BlockPos findNearestBarrel(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -REGISTER_RADIUS; dx <= REGISTER_RADIUS; dx++) {
            for (int dy = -REGISTER_RADIUS; dy <= REGISTER_RADIUS; dy++) {
                for (int dz = -REGISTER_RADIUS; dz <= REGISTER_RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos) || !SharedEconomyCompat.isLogisticsContainerBlock(level.getBlockState(pos))) continue;
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

    private static String coords(BlockPos pos) { return pos.getX() + ", " + pos.getY() + ", " + pos.getZ(); }
    private record ResolvedContainer(BlockPos pos, Container container) {}
}

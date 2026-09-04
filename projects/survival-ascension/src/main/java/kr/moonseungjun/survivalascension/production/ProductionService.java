package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.expedition.ExpeditionOperationSystem;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ProductionService {
    public static final String ACTION_PREFIX = "produce:";
    public static final String ACTION_STATUS = "production_status";
    public static final String ACTION_DISPATCH = "dispatch_supply";
    public static final String ACTION_DEPOT_TOGGLE = "toggle_field_depot";
    public static final String ACTION_WAREHOUSE_TOGGLE = "toggle_warehouse_barrel";
    public static final String ACTION_BULK_OFFLOAD = "bulk_offload";
    public static final String ACTION_FREIGHT = "physical_freight";
    public static final String ACTION_OUTPOST_UPGRADE = "upgrade_outpost";
    public static final String ACTION_OUTPOST_SIEGE = "outpost_siege";
    public static final String ACTION_BASTION_SIEGE = "bastion_siege";
    public static final String ACTION_FIELD_RECOVERY = "field_recovery";
    public static final String ACTION_FIELD_OPERATION = "field_operation";

    private ProductionService() {}

    public static void perform(ServerPlayer player, String action) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§3[산업 생산망] §f크리에이티브/관전자 상태에서는 생산/물류 작업을 처리할 수 없습니다."));
            return;
        }
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.INDUSTRIAL_WORKS)) {
            player.sendSystemMessage(Component.literal("§3[산업 생산망] §f먼저 §b산업 가공소§f를 완공해야 합니다."));
            return;
        }
        if (ACTION_STATUS.equals(action)) { sendStatus(player); return; }
        if (ACTION_DISPATCH.equals(action)) { dispatchSupply(player); return; }
        if (ACTION_DEPOT_TOGGLE.equals(action)) { FieldDepotService.toggleNearest(player); return; }
        if (ACTION_WAREHOUSE_TOGGLE.equals(action)) { FieldDepotService.toggleWarehouseNearest(player); return; }
        if (ACTION_BULK_OFFLOAD.equals(action)) { bulkOffload(player); return; }
        if (ACTION_FREIGHT.equals(action)) { FreightService.transferNearest(player); return; }
        if (ACTION_OUTPOST_UPGRADE.equals(action)) { OutpostService.upgradeNearest(player); return; }
        if (ACTION_OUTPOST_SIEGE.equals(action)) { startSiegeWithLocalSupply(player, false); return; }
        if (ACTION_BASTION_SIEGE.equals(action)) { startSiegeWithLocalSupply(player, true); return; }
        if (ACTION_FIELD_RECOVERY.equals(action)) { FieldRecoveryService.configure(player); return; }
        if (ACTION_FIELD_OPERATION.equals(action)) {
            if (OutpostSiegeSystem.isActive(player)) {
                player.sendSystemMessage(Component.literal("§6[원정 작전] §f진행 중인 §c전초/요새 방어§f를 먼저 끝내세요."));
                return;
            }
            startOperationWithLocalSupply(player);
            return;
        }
        if (!action.startsWith(ACTION_PREFIX)) {
            player.sendSystemMessage(Component.literal("§c[산업 생산망] §f알 수 없는 생산 작업입니다."));
            return;
        }
        ProductionProgram program = ProductionProgram.fromId(action.substring(ACTION_PREFIX.length()));
        if (program == null) {
            player.sendSystemMessage(Component.literal("§c[산업 생산망] §f알 수 없는 생산 배치입니다."));
            return;
        }
        ProductionData data = ProductionData.get(player);
        if (!data.canAccept(player, program)) {
            player.sendSystemMessage(Component.literal("§3[산업 생산망] §f" + program.koreanName() + " 버퍼가 가득 찼습니다. §7(최대 " + ProductionData.MAX_BUFFER + ")"));
            return;
        }
        if (!hasAll(player, program)) {
            player.sendSystemMessage(Component.literal("§3[산업 생산망] §f" + program.koreanName() + " 재료가 부족합니다."));
            for (ProductionProgram.Input input : program.inputs()) {
                int have = count(player, input);
                player.sendSystemMessage(Component.literal("  §7- §f" + input.label() + " §e" + have + "§7/§f" + input.amount()));
            }
            player.sendSystemMessage(Component.literal("  §7가까운 사용 가능 물류 통을 먼저 합산하고, 부족분은 인벤토리에서 사용합니다."));
            return;
        }
        for (ProductionProgram.Input input : program.inputs()) {
            if (!consume(player, input, input.amount())) {
                player.sendSystemMessage(Component.literal("§c[산업 생산망] §f투입 재고 상태가 바뀌어 생산을 중단했습니다."));
                return;
            }
        }
        ProductionData.BatchResult result = data.addBatch(player, program);
        player.sendSystemMessage(Component.literal("§3[산업 생산] §b" + program.koreanName() + " §f1배치 완료. §7가까운 실제 물류 통 우선 → 부족분 인벤토리"));
        if (result.cycleCompleted()) {
            player.sendSystemMessage(Component.literal("§b[산업 사이클 완성] §f4계통 배치를 결합해 §e현장 보급권 +1§f을 확보했습니다. §7보유 "
                    + result.supplyCharges() + "/" + ProductionData.MAX_SUPPLY_CHARGES));
        }
        sendStatus(player);
    }

    public static void sendStatus(ServerPlayer player) {
        ProductionData data = ProductionData.get(player);
        player.sendSystemMessage(Component.literal("§3[산업 생산망] §f누적 사이클 §b" + data.cycles(player)
                + " §7· 현장 보급권 §e" + data.supplyCharges(player) + "/" + ProductionData.MAX_SUPPLY_CHARGES));
        for (ProductionProgram program : ProductionProgram.values()) {
            player.sendSystemMessage(Component.literal("  §7- §f" + program.koreanName() + " §b" + data.buffer(player, program)
                    + "§7/§f" + ProductionData.MAX_BUFFER));
        }
        player.sendSystemMessage(Component.literal("§7산업 투입: 같은 차원에서 현재 로딩된 등록 거점/창고 통 전체 + 부족분 인벤토리. 보급권: 실물 출고1 / 거점1 / 전초2 / 방어1 / 요새방어2 / 복귀1 / 원정1."));
        player.sendSystemMessage(Component.literal("§7전선 작전은 보급권과 별도로 출발 전초의 실제 통 재고를 소비합니다. 원정=식량(밀/당근/감자/비트)12+철 주괴3+연료(석탄/숯)3 / 방어=식량16+철 주괴5+아무 종류의 통나무12 / 요새=식량32+철 주괴8+석재 벽돌32."));
        player.sendSystemMessage(Component.literal("§7창고군: 등록 앵커 하나당 반경6 실제 통 최대8개를 연결하며, 같은 차원에서 로딩된 등록 창고는 플레이어와 거리 제한 없이 공용 재고로 사용합니다."));
        player.sendSystemMessage(Component.literal("§7일괄 적재: 핫바/장비를 보존하고 주 인벤토리의 대량 자원을 가까운 사용 가능 실제 통부터 채웁니다."));
        player.sendSystemMessage(Component.literal("§7물리 화물: 산업+토목 완공 후 양쪽 활성 전초마다 반경6 소형 하역장(레일6+·동력레일·호퍼·제어)을 만들고 실제 상자 광산수레로 창고 재고를 운반합니다."));
        player.sendSystemMessage(Component.literal("§7실물 출고 1회는 금 주괴 32 + 자수정 조각 16 + 메아리 조각 2이며 플레이어에게 직접 지급됩니다."));
        FieldDepotService.sendStatus(player);
        OutpostService.sendStatus(player);
        sendLocalSupplyStatus(player);
        FreightService.sendStatus(player);
        OutpostSiegeSystem.sendStatus(player);
        OutpostFortificationService.sendStatus(player);
        FieldRecoveryService.sendStatus(player);
        ExpeditionOperationSystem.sendStatus(player);
    }

    private static void startSiegeWithLocalSupply(ServerPlayer player, boolean bastion) {
        if (OutpostSiegeSystem.isActive(player)) {
            if (bastion) OutpostSiegeSystem.startBastionOrStatus(player);
            else OutpostSiegeSystem.startOrStatus(player);
            return;
        }

        int chargeCost = bastion ? OutpostSiegeSystem.BASTION_SUPPLY_CHARGE_COST : OutpostSiegeSystem.SUPPLY_CHARGE_COST;
        if (ProductionData.get(player).supplyCharges(player) < chargeCost) {
            if (bastion) OutpostSiegeSystem.startBastionOrStatus(player);
            else OutpostSiegeSystem.startOrStatus(player);
            return;
        }

        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, OutpostSiegeSystem.START_RADIUS);
        if (outpost == null) {
            if (bastion) OutpostSiegeSystem.startBastionOrStatus(player);
            else OutpostSiegeSystem.startOrStatus(player);
            return;
        }
        if (bastion && !OutpostFortificationService.validateForBastion(player, outpost, true)) return;

        LocalLoadout loadout = bastion ? LocalLoadout.BASTION_DEFENSE : LocalLoadout.OUTPOST_DEFENSE;
        PreparedLocalSupply prepared = prepareLocalOutpostSupply(player, outpost, loadout);
        if (prepared == null) return;

        if (bastion) OutpostSiegeSystem.startBastionOrStatus(player);
        else OutpostSiegeSystem.startOrStatus(player);
        if (OutpostSiegeSystem.isActive(player) && !consumeLocalOutpostSupply(player, prepared)) {
            player.sendSystemMessage(Component.literal("§c[전선 현지 보급] §f방어전 시작 직후 전초 재고가 바뀌어 시작을 취소했습니다. §7재고를 확인한 뒤 다시 시도하세요."));
        }
    }

    private static void startOperationWithLocalSupply(ServerPlayer player) {
        if (ExpeditionOperationSystem.isActive(player)) {
            ExpeditionOperationSystem.startOrStatus(player);
            return;
        }
        if (ProductionData.get(player).supplyCharges(player) < ExpeditionOperationSystem.SUPPLY_CHARGE_COST) {
            ExpeditionOperationSystem.startOrStatus(player);
            return;
        }

        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, ExpeditionOperationSystem.START_RADIUS);
        if (outpost == null) {
            ExpeditionOperationSystem.startOrStatus(player);
            return;
        }
        PreparedLocalSupply prepared = prepareLocalOutpostSupply(player, outpost, LocalLoadout.EXPEDITION);
        if (prepared == null) return;

        ExpeditionOperationSystem.startOrStatus(player);
        if (ExpeditionOperationSystem.isActive(player) && !consumeLocalOutpostSupply(player, prepared)) {
            player.sendSystemMessage(Component.literal("§c[전선 현지 보급] §f원정 출발 직후 전초 재고가 바뀌어 시작을 취소했습니다. §7재고를 확인한 뒤 다시 시도하세요."));
        }
    }

    private static PreparedLocalSupply prepareLocalOutpostSupply(ServerPlayer player, OutpostData.OutpostEntry outpost, LocalLoadout loadout) {
        List<Container> containers = exactOutpostContainers(player, outpost);
        if (containers.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[전선 현지 보급] §f출발 전초의 등록 통/연결 창고 통을 현재 사용할 수 없습니다. §7실제 통이 로딩되어 있고 상호작용 가능해야 합니다."));
            return null;
        }

        boolean ready = true;
        for (LocalRequirement requirement : requirements(loadout)) {
            int have = countInContainers(containers, requirement.matcher());
            if (have < requirement.amount()) {
                ready = false;
                player.sendSystemMessage(Component.literal("§c[전선 현지 보급] §f" + loadout.koreanName + " 준비 부족 · "
                        + requirement.label() + " §e" + have + "§7/§f" + requirement.amount()));
            }
        }
        if (!ready) {
            player.sendSystemMessage(Component.literal("§7이 비용은 플레이어 인벤토리나 다른 근처 거점으로 대체하지 않습니다. 직접 전초 통에 넣거나 물리 화물 수레로 운송하세요."));
            return null;
        }
        return new PreparedLocalSupply(outpost, loadout);
    }

    private static boolean consumeLocalOutpostSupply(ServerPlayer player, PreparedLocalSupply prepared) {
        List<Container> containers = exactOutpostContainers(player, prepared.outpost());
        if (containers.isEmpty()) return false;
        List<LocalRequirement> requirements = requirements(prepared.loadout());
        for (LocalRequirement requirement : requirements) {
            if (countInContainers(containers, requirement.matcher()) < requirement.amount()) return false;
        }
        for (LocalRequirement requirement : requirements) {
            if (!consumeFromContainers(containers, requirement.matcher(), requirement.amount())) return false;
        }
        player.sendSystemMessage(Component.literal("§b[전선 현지 보급] §f" + prepared.loadout().koreanName
                + " 출발 전초 실물 재고 소비 완료. §7플레이어 인벤토리는 사용하지 않았습니다."));
        return true;
    }

    private static void sendLocalSupplyStatus(ServerPlayer player) {
        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, OutpostService.ACTIVE_OWNER_RADIUS);
        if (outpost == null) {
            player.sendSystemMessage(Component.literal("§3[전선 현지 보급] §f64블록 안 활성 전초 없음 §7· 작전 시작 시 출발 전초의 실제 통 재고만 사용"));
            return;
        }
        List<Container> containers = exactOutpostContainers(player, outpost);
        if (containers.isEmpty()) {
            player.sendSystemMessage(Component.literal("§3[전선 현지 보급] §f근처 활성 전초의 통 창고군이 현재 미로딩/사용 불가 상태입니다."));
            return;
        }
        int food = countInContainers(containers, ProductionService::isFieldFood);
        int iron = countInContainers(containers, stack -> stack.is(Items.IRON_INGOT));
        int fuel = countInContainers(containers, ProductionService::isFieldFuel);
        int logs = countInContainers(containers, stack -> stack.is(ItemTags.LOGS));
        int bricks = countInContainers(containers, stack -> stack.is(Items.STONE_BRICKS));
        player.sendSystemMessage(Component.literal("§3[전선 현지 보급] §f전초 " + coords(outpost.pos()) + " §7· 식량(밀/당근/감자/비트) §e" + food
                + " §7· 철 주괴 §e" + iron + " §7· 연료(석탄/숯) §e" + fuel + " §7· 아무 종류의 통나무 §e" + logs + " §7· 석재 벽돌 §e" + bricks));
    }

    private static List<LocalRequirement> requirements(LocalLoadout loadout) {
        return switch (loadout) {
            case EXPEDITION -> List.of(
                    new LocalRequirement("식량(밀/당근/감자/비트)", 12, ProductionService::isFieldFood),
                    new LocalRequirement("철 주괴", 3, stack -> stack.is(Items.IRON_INGOT)),
                    new LocalRequirement("연료(석탄 또는 숯)", 3, ProductionService::isFieldFuel));
            case OUTPOST_DEFENSE -> List.of(
                    new LocalRequirement("식량(밀/당근/감자/비트)", 16, ProductionService::isFieldFood),
                    new LocalRequirement("철 주괴", 5, stack -> stack.is(Items.IRON_INGOT)),
                    new LocalRequirement("아무 종류의 통나무", 12, stack -> stack.is(ItemTags.LOGS)));
            case BASTION_DEFENSE -> List.of(
                    new LocalRequirement("식량(밀/당근/감자/비트)", 32, ProductionService::isFieldFood),
                    new LocalRequirement("철 주괴", 8, stack -> stack.is(Items.IRON_INGOT)),
                    new LocalRequirement("석재 벽돌", 32, stack -> stack.is(Items.STONE_BRICKS)));
        };
    }

    private static boolean isFieldFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT);
    }

    private static boolean isFieldFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    private static List<Container> exactOutpostContainers(ServerPlayer player, OutpostData.OutpostEntry outpost) {
        if (!(player.level() instanceof ServerLevel level)) return List.of();
        if (!outpost.dimension().equals(level.dimension().toString())) return List.of();
        FieldDepotData data = FieldDepotData.get(player);
        FieldDepotData.DepotEntry depot = data.depots(player).stream()
                .filter(entry -> entry.dimension().equals(outpost.dimension()) && entry.pos().equals(outpost.pos()))
                .findFirst().orElse(null);
        if (depot == null) return List.of();

        List<Container> containers = new ArrayList<>();
        addExactBarrel(player, level, depot.pos(), containers);
        for (FieldDepotData.LinkedBarrel link : data.linkedBarrels(player, depot)) {
            if (!link.dimension().equals(outpost.dimension())) continue;
            addExactBarrel(player, level, link.pos(), containers);
        }
        return containers;
    }

    private static void addExactBarrel(ServerPlayer player, ServerLevel level, BlockPos pos, List<Container> containers) {
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) return;
        if (!level.getBlockState(pos).is(Blocks.BARREL)) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) containers.add(container);
    }

    private static int countInContainers(List<Container> containers, Predicate<ItemStack> matcher) {
        int found = 0;
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (matcher.test(stack)) found += stack.getCount();
            }
        }
        return found;
    }

    private static boolean consumeFromContainers(List<Container> containers, Predicate<ItemStack> matcher, int amount) {
        if (amount <= 0 || countInContainers(containers, matcher) < amount) return false;
        int remaining = amount;
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
        return remaining == 0;
    }

    private static void bulkOffload(ServerPlayer player) {
        int storageBarrels = FieldDepotService.activeStorageBarrelCount(player);
        if (storageBarrels <= 0) {
            player.sendSystemMessage(Component.literal("§3[현장 일괄 적재] §f같은 차원에서 현재 로딩된 사용할 수 있는 등록 물류 통이 없습니다."));
            return;
        }
        int eligible = FieldDepotService.countOffloadableMainInventory(player);
        if (eligible <= 0) {
            player.sendSystemMessage(Component.literal("§3[현장 일괄 적재] §f주 인벤토리에 적재할 대량 자원이 없습니다. §7핫바/장비는 대상에서 제외됩니다."));
            return;
        }
        int moved = FieldDepotService.offloadBulkMaterials(player);
        if (moved <= 0) {
            player.sendSystemMessage(Component.literal("§3[현장 일괄 적재] §f사용 가능한 실제 통들에 남은 적재 공간이 없습니다."));
            return;
        }
        if (moved < eligible) {
            player.sendSystemMessage(Component.literal("§b[현장 일괄 적재] §f대량 자원 §e" + moved + "§f개를 사용할 수 있는 등록 물류 통부터 적재했습니다. §7대상 "
                    + eligible + "개 중 일부만 수용됨 · 사용 통 " + storageBarrels + " · 핫바/장비 유지"));
        } else {
            player.sendSystemMessage(Component.literal("§b[현장 일괄 적재] §f대량 자원 §e" + moved
                    + "§f개를 사용할 수 있는 등록 물류 통부터 적재했습니다. §7사용 통 " + storageBarrels + " · 핫바/장비 유지"));
        }
    }

    private static void dispatchSupply(ServerPlayer player) {
        ProductionData data = ProductionData.get(player);
        if (!data.consumeSupplyCharge(player)) {
            player.sendSystemMessage(Component.literal("§3[산업 출고] §f사용 가능한 현장 보급권이 없습니다."));
            return;
        }
        giveOrDrop(player, new ItemStack(Items.GOLD_INGOT, 32));
        giveOrDrop(player, new ItemStack(Items.AMETHYST_SHARD, 16));
        giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 2));
        player.sendSystemMessage(Component.literal("§b[산업 출고] §f현장 보급 물자 지급: §6금 주괴 32 §7· §d자수정 조각 16 §7· §b메아리 조각 2"
                + " §7· 남은 보급권 " + data.supplyCharges(player) + "/" + ProductionData.MAX_SUPPLY_CHARGES));
    }

    private static boolean hasAll(ServerPlayer player, ProductionProgram program) {
        for (ProductionProgram.Input input : program.inputs()) if (count(player, input) < input.amount()) return false;
        return true;
    }

    private static int count(ServerPlayer player, ProductionProgram.Input input) {
        return FieldDepotService.countValue(player, input::value);
    }

    private static boolean consume(ServerPlayer player, ProductionProgram.Input input, int amount) {
        return FieldDepotService.consumeValue(player, input::value, amount);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static String coords(BlockPos pos) { return pos.getX() + ", " + pos.getY() + ", " + pos.getZ(); }

    private enum LocalLoadout {
        EXPEDITION("원정 작전"), OUTPOST_DEFENSE("전초 방어"), BASTION_DEFENSE("요새 방어");
        private final String koreanName;
        LocalLoadout(String koreanName) { this.koreanName = koreanName; }
    }

    private record LocalRequirement(String label, int amount, Predicate<ItemStack> matcher) {}
    private record PreparedLocalSupply(OutpostData.OutpostEntry outpost, LocalLoadout loadout) {}
}

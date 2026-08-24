package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.expedition.ExpeditionOperationSystem;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        if (ACTION_OUTPOST_SIEGE.equals(action)) { OutpostSiegeSystem.startOrStatus(player); return; }
        if (ACTION_BASTION_SIEGE.equals(action)) { OutpostSiegeSystem.startBastionOrStatus(player); return; }
        if (ACTION_FIELD_RECOVERY.equals(action)) { FieldRecoveryService.configure(player); return; }
        if (ACTION_FIELD_OPERATION.equals(action)) {
            if (OutpostSiegeSystem.isActive(player)) {
                player.sendSystemMessage(Component.literal("§6[원정 작전] §f진행 중인 §c전초/요새 방어§f를 먼저 끝내세요."));
                return;
            }
            ExpeditionOperationSystem.startOrStatus(player);
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
        player.sendSystemMessage(Component.literal("§7산업 투입: 가까운 사용 가능 거점/창고 통 우선 + 부족분 인벤토리. 보급권: 실물 출고1 / 거점1 / 전초2 / 방어1 / 요새방어2 / 복귀1 / 원정1."));
        player.sendSystemMessage(Component.literal("§7창고군: 등록 앵커 하나당 반경6 실제 통 최대8개를 별도 보급권 없이 연결해 같은32/64 물류권에서 사용합니다."));
        player.sendSystemMessage(Component.literal("§7일괄 적재: 핫바/장비를 보존하고 주 인벤토리의 대량 자원을 가까운 사용 가능 실제 통부터 채웁니다."));
        player.sendSystemMessage(Component.literal("§7물리 화물: 산업+토목 완공 후 양쪽 활성 전초마다 반경6 소형 하역장(레일6+·동력레일·호퍼·제어)을 만들고 실제 상자 광산수레로 창고 재고를 운반합니다."));
        player.sendSystemMessage(Component.literal("§7실물 출고1회는 금32+자수정16+메아리2이며 플레이어에게 직접 지급됩니다."));
        FieldDepotService.sendStatus(player);
        OutpostService.sendStatus(player);
        FreightService.sendStatus(player);
        OutpostSiegeSystem.sendStatus(player);
        OutpostFortificationService.sendStatus(player);
        FieldRecoveryService.sendStatus(player);
        ExpeditionOperationSystem.sendStatus(player);
    }

    private static void bulkOffload(ServerPlayer player) {
        int storageBarrels = FieldDepotService.activeStorageBarrelCount(player);
        if (storageBarrels <= 0) {
            player.sendSystemMessage(Component.literal("§3[현장 일괄 적재] §f현재 범위 안에 사용할 수 있는 거점/창고 통이 없습니다."));
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
            player.sendSystemMessage(Component.literal("§b[현장 일괄 적재] §f대량 자원 §e" + moved + "§f개를 가까운 실제 통부터 적재했습니다. §7대상 "
                    + eligible + "개 중 일부만 수용됨 · 사용 통 " + storageBarrels + " · 핫바/장비 유지"));
        } else {
            player.sendSystemMessage(Component.literal("§b[현장 일괄 적재] §f대량 자원 §e" + moved
                    + "§f개를 가까운 실제 통부터 적재했습니다. §7사용 통 " + storageBarrels + " · 핫바/장비 유지"));
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
        player.sendSystemMessage(Component.literal("§b[산업 출고] §f현장 보급 물자 지급: §6금32 §7· §d자수정16 §7· §b메아리2"
                + " §7· 남은 보급권 " + data.supplyCharges(player) + "/" + ProductionData.MAX_SUPPLY_CHARGES));
    }

    private static boolean hasAll(ServerPlayer player, ProductionProgram program) {
        for (ProductionProgram.Input input : program.inputs()) if (count(player, input) < input.amount()) return false;
        return true;
    }

    private static int count(ServerPlayer player, ProductionProgram.Input input) {
        return FieldDepotService.countMatching(player, input::matches);
    }

    private static boolean consume(ServerPlayer player, ProductionProgram.Input input, int amount) {
        return FieldDepotService.consumeMatching(player, input::matches, amount);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}

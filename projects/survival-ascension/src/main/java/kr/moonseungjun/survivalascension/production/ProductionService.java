package kr.moonseungjun.survivalascension.production;

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
    public static final String ACTION_OUTPOST_UPGRADE = "upgrade_outpost";

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
        if (ACTION_OUTPOST_UPGRADE.equals(action)) { OutpostService.upgradeNearest(player); return; }
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
            return;
        }
        for (ProductionProgram.Input input : program.inputs()) consume(player, input, input.amount());
        ProductionData.BatchResult result = data.addBatch(player, program);
        player.sendSystemMessage(Component.literal("§3[산업 생산] §b" + program.koreanName() + " §f1배치 완료."));
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
        player.sendSystemMessage(Component.literal("§7보급권: 실물 출고1 / 배럴 거점 등록1 / 전초기지 승격2. 출고1회는 금32+자수정16+메아리2."));
        FieldDepotService.sendStatus(player);
        OutpostService.sendStatus(player);
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
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (input.matches(stack)) found += stack.getCount();
        }
        return found;
    }

    private static void consume(ServerPlayer player, ProductionProgram.Input input, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!input.matches(stack)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        player.getInventory().setChanged();
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}

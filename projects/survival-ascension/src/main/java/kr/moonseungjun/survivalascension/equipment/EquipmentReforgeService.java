package kr.moonseungjun.survivalascension.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EquipmentReforgeService {
    public static final int ACTION_REFORGE = 0;
    public static final int ACTION_SALVAGE = 1;
    public static final int ACTION_AWAKEN = 2;

    private EquipmentReforgeService() {}

    public static void perform(ServerPlayer player, int action) {
        if (action == ACTION_REFORGE) reforge(player);
        else if (action == ACTION_SALVAGE) salvage(player);
        else if (action == ACTION_AWAKEN) awaken(player);
    }

    private static void reforge(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        int rarity = AscensionAffixes.rarity(held);
        if (rarity <= 0) {
            player.sendSystemMessage(Component.literal("§c[장비] §f주 손에 정예/승천/신화 장비를 들어야 합니다."));
            return;
        }
        MaterialCost[] costs = reforgeCosts(rarity, AscensionAffixes.isAwakened(held));
        if (!player.isCreative() && !hasAll(player, costs)) {
            player.sendSystemMessage(Component.literal("§c[재련] §f재료 부족 · " + join(costs)));
            return;
        }
        if (!player.isCreative()) consumeAll(player, costs);
        if (!AscensionAffixes.reroll(held, player.level().getRandom())) return;
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§d[재련 완료] §f" + AscensionAffixes.rarityName(held) + " · §e" + AscensionAffixes.affixSummary(held)));
    }

    private static void awaken(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (AscensionAffixes.rarity(held) != 3) {
            player.sendSystemMessage(Component.literal("§c[신화 각성] §f신화 III 장비만 각성할 수 있습니다."));
            return;
        }
        if (AscensionAffixes.isAwakened(held)) {
            player.sendSystemMessage(Component.literal("§5[신화 각성] §f이미 각성한 장비입니다."));
            return;
        }
        if (!AscensionAffixes.canAwaken(held)) {
            player.sendSystemMessage(Component.literal("§c[신화 각성] §faffix 데이터가 정상적인 3-affix 신화 장비가 아닙니다. 재료는 소비하지 않았습니다."));
            return;
        }
        MaterialCost[] costs = awakeningCosts();
        if (!player.isCreative() && !hasAll(player, costs)) {
            player.sendSystemMessage(Component.literal("§c[신화 각성] §f재료 부족 · " + join(costs)));
            return;
        }
        if (!player.isCreative()) consumeAll(player, costs);
        if (!AscensionAffixes.awaken(held, player.level().getRandom())) {
            player.sendSystemMessage(Component.literal("§c[신화 각성] §f검증 이후 장비 상태가 바뀌어 각성을 중단했습니다."));
            return;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§5[신화 각성 완료] §f4번째 affix가 개방되었습니다. §e" + AscensionAffixes.affixSummary(held)));
    }

    private static void salvage(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        int rarity = AscensionAffixes.rarity(held);
        if (rarity <= 0) {
            player.sendSystemMessage(Component.literal("§c[장비] §f주 손에 정예/승천/신화 장비를 들어야 합니다."));
            return;
        }
        if (player.isCreative()) {
            player.sendSystemMessage(Component.literal("§e[분해] §f크리에이티브에서는 재료 복제를 막기 위해 분해 보상을 지급하지 않습니다."));
            return;
        }
        String oldName = held.getHoverName().getString();
        held.shrink(1);
        for (MaterialCost reward : salvageRewards(rarity)) give(player, new ItemStack(reward.item(), reward.count()));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§b[분해 완료] §f" + oldName + " §7→ §f" + salvageText(rarity)));
    }

    public static String costText(int rarity) {
        return join(reforgeCosts(rarity, false));
    }

    public static String costText(ItemStack stack) {
        return join(reforgeCosts(AscensionAffixes.rarity(stack), AscensionAffixes.isAwakened(stack)));
    }

    public static String awakeningCostText() {
        return join(awakeningCosts());
    }

    public static String salvageText(int rarity) {
        return join(salvageRewards(rarity));
    }

    private static MaterialCost[] reforgeCosts(int rarity, boolean awakened) {
        if (rarity == 3 && awakened) {
            return new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 128, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 16, "다이아몬드"),
                    new MaterialCost(Items.NETHERITE_SCRAP, 4, "네더라이트 파편"),
                    new MaterialCost(Items.ECHO_SHARD, 16, "메아리 조각")
            };
        }
        return switch (rarity) {
            case 1 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 16, "자수정 조각"), new MaterialCost(Items.IRON_INGOT, 8, "철 주괴") };
            case 2 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 32, "자수정 조각"), new MaterialCost(Items.DIAMOND, 6, "다이아몬드") };
            case 3 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 64, "자수정 조각"), new MaterialCost(Items.DIAMOND, 12, "다이아몬드"), new MaterialCost(Items.NETHERITE_SCRAP, 2, "네더라이트 파편") };
            default -> new MaterialCost[0];
        };
    }

    private static MaterialCost[] awakeningCosts() {
        return new MaterialCost[] {
                new MaterialCost(Items.AMETHYST_SHARD, 256, "자수정 조각"),
                new MaterialCost(Items.DIAMOND, 24, "다이아몬드"),
                new MaterialCost(Items.NETHERITE_SCRAP, 8, "네더라이트 파편"),
                new MaterialCost(Items.ECHO_SHARD, 64, "메아리 조각"),
                new MaterialCost(Items.DRAGON_BREATH, 16, "드래곤의 숨결")
        };
    }

    private static MaterialCost[] salvageRewards(int rarity) {
        return switch (rarity) {
            case 1 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 8, "자수정 조각"), new MaterialCost(Items.IRON_INGOT, 4, "철 주괴") };
            case 2 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 20, "자수정 조각"), new MaterialCost(Items.DIAMOND, 2, "다이아몬드") };
            case 3 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 32, "자수정 조각"), new MaterialCost(Items.DIAMOND, 4, "다이아몬드"), new MaterialCost(Items.NETHERITE_SCRAP, 1, "네더라이트 파편") };
            default -> new MaterialCost[0];
        };
    }

    private static boolean hasAll(ServerPlayer player, MaterialCost[] costs) {
        for (MaterialCost cost : costs) if (count(player, cost.item()) < cost.count()) return false;
        return true;
    }

    private static int count(ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void consumeAll(ServerPlayer player, MaterialCost[] costs) {
        for (MaterialCost cost : costs) {
            int remaining = cost.count();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.is(cost.item())) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        player.getInventory().setChanged();
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static String join(MaterialCost[] costs) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < costs.length; i++) {
            if (i > 0) out.append(" · ");
            out.append(costs[i].label()).append(" ").append(costs[i].count());
        }
        return out.toString();
    }

    private record MaterialCost(Item item, int count, String label) {}
}

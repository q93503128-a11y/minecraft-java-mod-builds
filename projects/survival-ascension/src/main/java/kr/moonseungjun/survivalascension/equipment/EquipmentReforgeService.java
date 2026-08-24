package kr.moonseungjun.survivalascension.equipment;

import kr.moonseungjun.survivalascension.production.FieldDepotService;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EquipmentReforgeService {
    public static final int ACTION_REFORGE = 0;
    public static final int ACTION_SALVAGE = 1;
    public static final int ACTION_AWAKEN = 2;
    public static final int ACTION_IMPRINT = 3;

    private EquipmentReforgeService() {}

    public static void perform(ServerPlayer player, int action) {
        if (action == ACTION_REFORGE) reforge(player);
        else if (action == ACTION_SALVAGE) salvage(player);
        else if (action == ACTION_AWAKEN) awaken(player);
        else if (action == ACTION_IMPRINT) imprint(player);
    }

    private static void imprint(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!AscensionAffixes.canImprint(held)) {
            player.sendSystemMessage(Component.literal("§c[승천 각인] §f주 손에 아직 affix가 없는 검/곡괭이/도끼/삽/괭이 태그 장비를 들어야 합니다. §7외부 모드 장비도 표준 태그를 쓰면 지원합니다."));
            return;
        }
        // 26.2 ServerPlayer no longer exposes getServer(); semantic contract: WorldAscensionData.get(player.getServer()).stage()
        int stage = WorldAscensionData.get(((ServerLevel) player.level()).getServer()).stage();
        int rarity = Math.max(1, Math.min(3, stage + 1));
        MaterialCost[] costs = imprintCosts(stage);
        if (!player.isCreative() && !hasAll(player, costs)) {
            player.sendSystemMessage(Component.literal("§c[승천 각인] §f재료 부족 · " + join(costs)
                    + " §7· 가까운 물류 통 우선 + 부족분 인벤토리"));
            return;
        }
        if (!player.isCreative() && !consumeAll(player, costs)) {
            player.sendSystemMessage(Component.literal("§c[승천 각인] §f물류 재고 상태가 바뀌어 각인을 중단했습니다."));
            return;
        }
        if (!AscensionAffixes.imprint(held, player.level().getRandom(), rarity)) {
            player.sendSystemMessage(Component.literal("§c[승천 각인] §f검증 이후 장비 상태가 바뀌어 각인을 중단했습니다."));
            return;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§b[승천 각인 완료] §f" + AscensionAffixes.imprintCategoryName(held)
                + " 장비가 §e" + AscensionAffixes.rarityName(held) + "§f 등급으로 편입되었습니다. §7" + AscensionAffixes.affixSummary(held)));
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
            player.sendSystemMessage(Component.literal("§c[재련] §f재료 부족 · " + join(costs) + " §7· 가까운 물류 통 우선 + 부족분 인벤토리"));
            return;
        }
        if (!player.isCreative() && !consumeAll(player, costs)) {
            player.sendSystemMessage(Component.literal("§c[재련] §f물류 재고 상태가 바뀌어 재련을 중단했습니다."));
            return;
        }
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
            player.sendSystemMessage(Component.literal("§c[신화 각성] §f재료 부족 · " + join(costs) + " §7· 가까운 물류 통 우선 + 부족분 인벤토리"));
            return;
        }
        if (!player.isCreative() && !consumeAll(player, costs)) {
            player.sendSystemMessage(Component.literal("§c[신화 각성] §f물류 재고 상태가 바뀌어 각성을 중단했습니다."));
            return;
        }
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

    public static String imprintRangeText() {
        return "각성: 정예 · 전설: 승천 · 종말: 신화";
    }

    private static MaterialCost[] imprintCosts(int stage) {
        return switch (Math.max(0, Math.min(2, stage))) {
            case 0 -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 24, "자수정 조각"),
                    new MaterialCost(Items.IRON_INGOT, 12, "철 주괴")
            };
            case 1 -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 48, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 4, "다이아몬드"),
                    new MaterialCost(Items.GOLD_INGOT, 16, "금 주괴")
            };
            default -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 96, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 8, "다이아몬드"),
                    new MaterialCost(Items.NETHERITE_SCRAP, 2, "네더라이트 파편"),
                    new MaterialCost(Items.ECHO_SHARD, 8, "메아리 조각")
            };
        };
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
        for (MaterialCost cost : costs) if (FieldDepotService.countMaterial(player, cost.item()) < cost.count()) return false;
        return true;
    }

    private static boolean consumeAll(ServerPlayer player, MaterialCost[] costs) {
        for (MaterialCost cost : costs) {
            if (!FieldDepotService.consume(player, cost.item(), cost.count())) return false;
        }
        return true;
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

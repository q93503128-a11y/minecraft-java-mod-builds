package kr.moonseungjun.survivalascension.equipment;

import kr.moonseungjun.survivalascension.production.FieldDepotService;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.LinkedHashMap;
import java.util.Map;

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
            player.sendSystemMessage(Component.literal("§c[승천 각인] §f주 손에 아직 승천 옵션이 없는 검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비를 들어야 합니다. §7외부 모드 장비도 표준 태그를 쓰면 지원합니다."));
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
        player.sendSystemMessage(Component.literal("§7실제 효과: §f" + AscensionAffixes.effectSummary(held)));
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
        player.sendSystemMessage(Component.literal("§7실제 효과: §f" + AscensionAffixes.effectSummary(held)));
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
            player.sendSystemMessage(Component.literal("§c[신화 각성] §f승천 옵션 3개가 정상적으로 붙은 신화 장비가 아닙니다. 재료는 소비하지 않았습니다."));
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
        player.sendSystemMessage(Component.literal("§5[신화 각성 완료] §f4번째 승천 옵션이 개방되었습니다. §e" + AscensionAffixes.affixSummary(held)));
        player.sendSystemMessage(Component.literal("§7실제 효과: §f" + AscensionAffixes.effectSummary(held)));
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
        MaterialCost[] rewards = salvageRewards(held);
        held.shrink(1);
        for (MaterialCost reward : rewards) give(player, new ItemStack(reward.item(), reward.count()));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§b[분해 완료] §f" + oldName + " §7→ §f" + join(rewards)
                + " §8· 장비 부위/재질 체급/남은 내구도 반영"));
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

    public static String salvageText(ItemStack stack) {
        return join(salvageRewards(stack));
    }

    public static String imprintRangeText() {
        return "각성: 정예 · 전설: 승천 · 종말: 신화";
    }

    private static MaterialCost[] imprintCosts(int stage) {
        return switch (Math.max(0, Math.min(2, stage))) {
            case 0 -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 8, "자수정 조각"),
                    new MaterialCost(Items.IRON_INGOT, 4, "철 주괴")
            };
            case 1 -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 12, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 1, "다이아몬드"),
                    new MaterialCost(Items.GOLD_INGOT, 4, "금 주괴")
            };
            default -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 24, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 2, "다이아몬드"),
                    new MaterialCost(Items.ECHO_SHARD, 2, "메아리 조각")
            };
        };
    }

    private static MaterialCost[] reforgeCosts(int rarity, boolean awakened) {
        if (rarity == 3 && awakened) {
            return new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 24, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 3, "다이아몬드"),
                    new MaterialCost(Items.ECHO_SHARD, 2, "메아리 조각")
            };
        }
        return switch (rarity) {
            case 1 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 4, "자수정 조각"), new MaterialCost(Items.IRON_INGOT, 2, "철 주괴") };
            case 2 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 8, "자수정 조각"), new MaterialCost(Items.DIAMOND, 1, "다이아몬드") };
            case 3 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 16, "자수정 조각"), new MaterialCost(Items.DIAMOND, 2, "다이아몬드") };
            default -> new MaterialCost[0];
        };
    }

    private static MaterialCost[] awakeningCosts() {
        return new MaterialCost[] {
                new MaterialCost(Items.AMETHYST_SHARD, 32, "자수정 조각"),
                new MaterialCost(Items.DIAMOND, 4, "다이아몬드"),
                new MaterialCost(Items.NETHERITE_SCRAP, 1, "네더라이트 파편"),
                new MaterialCost(Items.ECHO_SHARD, 8, "메아리 조각"),
                new MaterialCost(Items.DRAGON_BREATH, 4, "드래곤의 숨결")
        };
    }

    private static MaterialCost[] salvageRewards(int rarity) {
        return switch (rarity) {
            case 1 -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 4, "자수정 조각"),
                    new MaterialCost(Items.IRON_INGOT, 2, "철 주괴")
            };
            case 2 -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 7, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 1, "다이아몬드"),
                    new MaterialCost(Items.GOLD_INGOT, 2, "금 주괴")
            };
            case 3 -> new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 12, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 2, "다이아몬드"),
                    new MaterialCost(Items.ECHO_SHARD, 1, "메아리 조각")
            };
            default -> new MaterialCost[0];
        };
    }

    private static MaterialCost[] salvageRewards(ItemStack stack) {
        int rarity = AscensionAffixes.rarity(stack);
        if (rarity <= 0) return new MaterialCost[0];

        Map<Item, Integer> amounts = new LinkedHashMap<>();
        Map<Item, String> labels = new LinkedHashMap<>();
        for (MaterialCost base : salvageRewards(rarity)) {
            addReward(amounts, labels, base.item(), base.count(), base.label());
        }

        int body = salvageBodyValue(stack);
        if (rarity == 1) {
            addReward(amounts, labels, Items.AMETHYST_SHARD, body, "자수정 조각");
            addReward(amounts, labels, Items.IRON_INGOT, body / 2, "철 주괴");
        } else if (rarity == 2) {
            addReward(amounts, labels, Items.AMETHYST_SHARD, body, "자수정 조각");
            addReward(amounts, labels, Items.DIAMOND, body / 3, "다이아몬드");
            addReward(amounts, labels, Items.GOLD_INGOT, body / 3, "금 주괴");
        } else {
            addReward(amounts, labels, Items.AMETHYST_SHARD, body * 2, "자수정 조각");
            addReward(amounts, labels, Items.DIAMOND, body / 2, "다이아몬드");
            addReward(amounts, labels, Items.ECHO_SHARD, body / 4, "메아리 조각");
        }

        if (AscensionAffixes.isAwakened(stack)) {
            // Awakening is expensive enough that destroying the finished item should return a meaningful,
            // but still clearly partial, share of the one-time awakening investment.
            addReward(amounts, labels, Items.AMETHYST_SHARD, 12, "자수정 조각");
            addReward(amounts, labels, Items.DIAMOND, 2, "다이아몬드");
            addReward(amounts, labels, Items.ECHO_SHARD, 3, "메아리 조각");
            addReward(amounts, labels, Items.NETHERITE_SCRAP, 1, "네더라이트 파편");
            addReward(amounts, labels, Items.DRAGON_BREATH, 2, "드래곤의 숨결");
        }

        MaterialCost[] result = new MaterialCost[amounts.size()];
        int index = 0;
        for (Map.Entry<Item, Integer> entry : amounts.entrySet()) {
            result[index++] = new MaterialCost(entry.getKey(), entry.getValue(), labels.get(entry.getKey()));
        }
        return result;
    }

    /**
     * One rarity is not one salvage value. A chestplate contains more recoverable work than boots,
     * a netherite-grade body is worth more than iron, and a nearly destroyed item returns less than
     * a fresh one. The value is intentionally bounded to 1..6 so external high-durability gear cannot
     * turn salvage into an unbounded material printer.
     */
    private static int salvageBodyValue(ItemStack stack) {
        int weight = salvageEquipmentWeight(stack);
        int quality = salvageMaterialQuality(stack);
        double condition = salvageConditionFactor(stack);
        int value = (int) Math.round(weight * (0.75D + 0.25D * quality) * condition);
        return Math.max(1, Math.min(6, value));
    }

    private static int salvageEquipmentWeight(ItemStack stack) {
        if (stack.is(ItemTags.CHEST_ARMOR)) return 4;
        if (stack.is(ItemTags.LEG_ARMOR)) return 3;
        if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.FOOT_ARMOR)) return 2;
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(Tags.Items.TOOLS_MACE)) return 3;
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.SPEARS) || stack.is(ItemTags.HOES)
                || stack.is(Tags.Items.TOOLS_BOW) || stack.is(Tags.Items.TOOLS_CROSSBOW)
                || stack.is(Tags.Items.TOOLS_SHIELD)) return 2;
        if (stack.is(ItemTags.SHOVELS)) return 1;
        return 2;
    }

    private static int salvageMaterialQuality(ItemStack stack) {
        if (isAny(stack,
                Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS)) return 3;
        if (isAny(stack,
                Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)) return 2;
        if (isAny(stack,
                Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
                Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS)) return 1;

        // External tagged gear has no reliable material recipe API. Durability is only a bounded fallback
        // quality signal; it can improve body value but cannot exceed quality III or body value 6.
        int maxDamage = stack.getMaxDamage();
        if (maxDamage >= 1500) return 3;
        if (maxDamage >= 500) return 2;
        if (maxDamage >= 200) return 1;
        return 0;
    }

    private static double salvageConditionFactor(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 1.0D;
        int max = stack.getMaxDamage();
        int remaining = Math.max(0, max - stack.getDamageValue());
        double ratio = remaining / (double) max;
        return 0.50D + 0.50D * ratio;
    }

    private static boolean isAny(ItemStack stack, Item... items) {
        for (Item item : items) if (stack.is(item)) return true;
        return false;
    }

    private static void addReward(Map<Item, Integer> amounts, Map<Item, String> labels,
                                  Item item, int count, String label) {
        if (count <= 0) return;
        amounts.merge(item, count, Integer::sum);
        labels.putIfAbsent(item, label);
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

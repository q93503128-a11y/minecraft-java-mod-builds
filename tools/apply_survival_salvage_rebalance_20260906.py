from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SURVIVAL = ROOT / "projects/survival-ascension"
JAVA = SURVIVAL / "src/main/java/kr/moonseungjun/survivalascension"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one replacement anchor, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


props = SURVIVAL / "gradle.properties"
main = JAVA / "SurvivalAscension.java"
equipment = JAVA / "equipment/EquipmentReforgeService.java"
equipment_ui = JAVA / "client/EquipmentRadialMenuScreen.java"
guide = JAVA / "client/GuideScreen.java"
check = SURVIVAL / "tools/test_current_source.py"

replace_once(props, "mod_version=0.61.18-alpha.1", "mod_version=0.61.19-alpha.1")
replace_once(main, 'VERSION = "0.61.18-alpha.1"', 'VERSION = "0.61.19-alpha.1"')

replace_once(
    equipment,
'''import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
''',
'''import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.LinkedHashMap;
import java.util.Map;
''')

replace_once(
    equipment,
'''        String oldName = held.getHoverName().getString();
        held.shrink(1);
        for (MaterialCost reward : salvageRewards(rarity)) give(player, new ItemStack(reward.item(), reward.count()));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§b[분해 완료] §f" + oldName + " §7→ §f" + salvageText(rarity)));
''',
'''        String oldName = held.getHoverName().getString();
        MaterialCost[] rewards = salvageRewards(held);
        held.shrink(1);
        for (MaterialCost reward : rewards) give(player, new ItemStack(reward.item(), reward.count()));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§b[분해 완료] §f" + oldName + " §7→ §f" + join(rewards)
                + " §8· 장비 부위/재질 체급/남은 내구도 반영"));
''')

replace_once(
    equipment,
'''    public static String salvageText(int rarity) {
        return join(salvageRewards(rarity));
    }
''',
'''    public static String salvageText(int rarity) {
        return join(salvageRewards(rarity));
    }

    public static String salvageText(ItemStack stack) {
        return join(salvageRewards(stack));
    }
''')

replace_once(
    equipment,
'''    private static MaterialCost[] salvageRewards(int rarity) {
        return switch (rarity) {
            case 1 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 2, "자수정 조각"), new MaterialCost(Items.IRON_INGOT, 1, "철 주괴") };
            case 2 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 4, "자수정 조각"), new MaterialCost(Items.DIAMOND, 1, "다이아몬드") };
            case 3 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 8, "자수정 조각"), new MaterialCost(Items.DIAMOND, 1, "다이아몬드") };
            default -> new MaterialCost[0];
        };
    }
''',
'''    private static MaterialCost[] salvageRewards(int rarity) {
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
''')

replace_once(
    equipment_ui,
    'case SALVAGE->"환급 · "+EquipmentReforgeService.salvageText(rarity);',
    'case SALVAGE->"환급 · "+EquipmentReforgeService.salvageText(held);')

replace_once(
    guide,
'''import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
''',
'''import kr.moonseungjun.survivalascension.production.FreightService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
''')

replace_once(
    guide,
'''                h("전선 화물 적재"), p("빈 수레에서 물리 화물 수레를 일반 선택하면 기존 대량자원을 적재하고, Shift+선택하면 식량176·철56·연료8·통나무32·석재벽돌128의 전선 묶음만 선별 적재합니다. 묶음은 세 전선 작전을 각각 1회 준비하는 양이며 부족하면 일부만 빼가지 않습니다."),
                h("전선 작전 재고"), p("원정=보급권1+식량32+철8+연료8, 전초 방어=보급권1+식량48+철16+통나무32, 요새 방어=보급권2+식량96+철32+석재벽돌128. 실물 비용은 정확히 출발 전초의 통 창고군에서만 차감합니다."),
''',
'''                h("전선 화물 적재"), p("빈 수레에서 물리 화물 수레를 일반 선택하면 기존 대량자원을 적재하고, Shift+선택하면 식량" + FreightService.FRONTLINE_FOOD
                        + "·철" + FreightService.FRONTLINE_IRON + "·연료" + FreightService.FRONTLINE_FUEL
                        + "·통나무" + FreightService.FRONTLINE_LOGS + "·석재벽돌" + FreightService.FRONTLINE_STONE_BRICKS
                        + "의 전선 묶음만 선별 적재합니다. 묶음은 세 전선 작전을 각각 1회 준비하는 양이며 부족하면 일부만 빼가지 않습니다."),
                h("전선 작전 재고"), p("원정·전초 방어·요새 방어의 보급권과 현지 실물 재고는 각 작전 시작 시 서버가 직접 검증합니다. 전선 화물 묶음은 그 세 작전을 각각 1회 수행하는 실제 합계만 운송합니다."),
''')

replace_once(
    guide,
'''                h("승천 각인"), p("주 손의 승천 옵션 없는 검/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비를 K → 장비 → 승천 각인으로 편입합니다. 각인 재료도 가까운 사용 가능 물류 통을 먼저 쓰고 부족분만 인벤토리에서 사용합니다."),
''',
'''                h("승천 각인"), p("주 손의 승천 옵션 없는 검/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 태그 장비를 K → 장비 → 승천 각인으로 편입합니다. 각인 재료도 가까운 사용 가능 물류 통을 먼저 쓰고 부족분만 인벤토리에서 사용합니다."),
                h("장비 분해"), p("분해는 등급만 보지 않습니다. 같은 정예/승천/신화라도 방어구 부위·도구 체급·바닐라 재질 등급·남은 내구도에 따라 환급량이 달라집니다. 각성 신화는 각성에 들어간 희귀 재료도 일부 회수합니다."),
''')

replace_once(
    guide,
'''                h("전선 화물 적재"), p("같은 빈 수레에서 Shift를 누른 채 물리 화물 수레를 선택하면 식량176·철56·연료8·통나무32·석재벽돌128만 선별합니다. 원정1+전초 방어1+요새 방어1회분이며 전체 묶음이 출발 전초에 없으면 아무것도 적재하지 않습니다."),
''',
'''                h("전선 화물 적재"), p("같은 빈 수레에서 Shift를 누른 채 물리 화물 수레를 선택하면 식량" + FreightService.FRONTLINE_FOOD
                        + "·철" + FreightService.FRONTLINE_IRON + "·연료" + FreightService.FRONTLINE_FUEL
                        + "·통나무" + FreightService.FRONTLINE_LOGS + "·석재벽돌" + FreightService.FRONTLINE_STONE_BRICKS
                        + "만 선별합니다. 원정1+전초 방어1+요새 방어1회분이며 전체 묶음이 출발 전초에 없으면 아무것도 적재하지 않습니다."),
''')

replace_once(check, 'require("mod_version=0.61.18-alpha.1" in props, "Survival Ascension version drift")',
             'require("mod_version=0.61.19-alpha.1" in props, "Survival Ascension version drift")')
replace_once(check, 'require(\'VERSION = "0.61.18-alpha.1"\' in main, "source version drift")',
             'require(\'VERSION = "0.61.19-alpha.1"\' in main, "source version drift")')

replace_once(
    check,
'''field = text(JAVA / "production/FieldDepotService.java")
for forbidden in ("setChunkForced", "addRegionTicket"):
    require(forbidden not in field, f"physical depot policy regressed: {forbidden}")

print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.18 full skill command/control regression + adaptive bore budget/profiling + protocol15 + prior runtime invariants")
''',
'''field = text(JAVA / "production/FieldDepotService.java")
for forbidden in ("setChunkForced", "addRegionTicket"):
    require(forbidden not in field, f"physical depot policy regressed: {forbidden}")

equipment = text(JAVA / "equipment/EquipmentReforgeService.java")
equipment_ui = text(JAVA / "client/EquipmentRadialMenuScreen.java")
guide = text(JAVA / "client/GuideScreen.java")
require("salvageRewards(ItemStack stack)" in equipment and "salvageBodyValue" in equipment,
        "equipment salvage returned to rarity-only fixed rewards")
require("salvageEquipmentWeight" in equipment and "salvageMaterialQuality" in equipment
        and "salvageConditionFactor" in equipment,
        "equipment type/material/condition salvage scaling missing")
require("AscensionAffixes.isAwakened(stack)" in equipment and "Items.NETHERITE_SCRAP" in equipment
        and "Items.DRAGON_BREATH" in equipment,
        "awakened salvage does not recover a bounded share of awakening materials")
require("salvageText(held)" in equipment_ui, "salvage UI still previews rarity-only rewards")
require("FreightService.FRONTLINE_FOOD" in guide and "FreightService.FRONTLINE_STONE_BRICKS" in guide,
        "guide no longer derives frontline manifest from freight authority")
require("식량176" not in guide and "철56" not in guide and "석재벽돌128만 선별" not in guide,
        "stale frontline manifest values returned to guide")
require("장비 분해" in guide and "남은 내구도" in guide,
        "dynamic salvage rules are hidden from player guidance")

print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.19 dynamic equipment salvage + canonical freight guide + full skill/runtime invariants")
''')

print("Applied Survival Ascension 0.61.19 salvage rebalance and guide authority fixes")

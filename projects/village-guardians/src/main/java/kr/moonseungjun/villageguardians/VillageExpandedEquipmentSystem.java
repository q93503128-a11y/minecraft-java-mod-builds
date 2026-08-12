package kr.moonseungjun.villageguardians;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Broader raid equipment pool; rarity remains the universal main-power layer. */
public final class VillageExpandedEquipmentSystem {
    private VillageExpandedEquipmentSystem() {}

    public static ItemStack createRaidDrop(int day, boolean boss,
                                           VillageEnemyArchetypeSystem.Archetype archetype,
                                           RandomSource random) {
        int safeDay = Math.max(1, day);
        List<Item> pool = safeDay < 5 ? List.of(
                Items.IRON_SWORD, Items.IRON_AXE, Items.BOW, Items.CROSSBOW, Items.SHIELD,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS)
                : safeDay < 10 ? List.of(
                Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.TRIDENT, Items.MACE, Items.BOW, Items.CROSSBOW,
                Items.SHIELD, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)
                : List.of(
                Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.TRIDENT, Items.MACE, Items.BOW, Items.CROSSBOW,
                Items.SHIELD, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
                Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.BLAZE_ROD);
        if (archetype == VillageEnemyArchetypeSystem.Archetype.MARKSMAN
                || archetype == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER) {
            pool = List.of(Items.BOW, Items.CROSSBOW,
                    safeDay >= 10 ? Items.NETHERITE_HELMET : Items.DIAMOND_HELMET,
                    safeDay >= 10 ? Items.NETHERITE_BOOTS : Items.DIAMOND_BOOTS);
        } else if (archetype == VillageEnemyArchetypeSystem.Archetype.SHIELDBREAKER
                || archetype == VillageEnemyArchetypeSystem.Archetype.SIEGE_BEAST
                || archetype == VillageEnemyArchetypeSystem.Archetype.IRON_WARLORD) {
            pool = List.of(safeDay >= 8 ? Items.NETHERITE_AXE : Items.DIAMOND_AXE,
                    Items.MACE, Items.SHIELD,
                    safeDay >= 10 ? Items.NETHERITE_CHESTPLATE : Items.DIAMOND_CHESTPLATE);
        }
        Item item = pool.get(random.nextInt(pool.size()));
        VillageEquipmentRaritySystem.Rarity rarity = rollRarity(safeDay, boss, random);
        return VillageEquipmentRaritySystem.createNamed(item, rarity, displayName(item, random));
    }

    private static VillageEquipmentRaritySystem.Rarity rollRarity(int day, boolean boss, RandomSource random) {
        int roll = random.nextInt(1000) + Math.min(280, (day - 1) * 19) + (boss ? 280 : 0);
        if (roll >= 1080) return VillageEquipmentRaritySystem.Rarity.LEGENDARY;
        if (roll >= 880) return VillageEquipmentRaritySystem.Rarity.EPIC;
        if (roll >= 610) return VillageEquipmentRaritySystem.Rarity.RARE;
        if (roll >= 330) return VillageEquipmentRaritySystem.Rarity.UNCOMMON;
        return VillageEquipmentRaritySystem.Rarity.COMMON;
    }

    private static String displayName(Item item, RandomSource random) {
        if (item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD) {
            return random.nextBoolean() ? "수호 장검" : "결투 검";
        }
        if (item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE) return "전열 대형도끼";
        if (item == Items.TRIDENT) return "성벽 장창";
        if (item == Items.MACE) return "공성 전투망치";
        if (item == Items.BOW) return random.nextBoolean() ? "성루 장궁" : "기동 단궁";
        if (item == Items.CROSSBOW) return "수호 석궁";
        if (item == Items.SHIELD) return "성벽 수호 방패";
        if (item == Items.BLAZE_ROD) return "비전 전투 집중봉";
        return VillageEquipmentRaritySystem.displayName(item);
    }
}

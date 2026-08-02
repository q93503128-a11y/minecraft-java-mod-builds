package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;

/** Raid enemies never leak vanilla food loops; rewards use curated sale loot and graded equipment. */
public final class VillageRaidLootSystem {
    private static final List<Item> COMMON_SALE_LOOT = List.of(
            Items.BONE, Items.STRING, Items.SPIDER_EYE, Items.GUNPOWDER);

    private VillageRaidLootSystem() {}

    public static void handleDrops(LivingDropsEvent event) {
        if (!VillageRaidSystem.isRaidEnemy(event.getEntity())) return;
        event.getDrops().clear();
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        RandomSource random = event.getEntity().getRandom();
        boolean boss = event.getEntity() instanceof Mob mob && isBoss(mob);
        float logistics = VillageDefenseResearchSystem.equipmentDropBonus();

        if (random.nextFloat() < (boss ? 1.0f : 0.28f)) {
            Item material = boss
                    ? (random.nextBoolean() ? Items.ENDER_PEARL : Items.BLAZE_ROD)
                    : COMMON_SALE_LOOT.get(random.nextInt(COMMON_SALE_LOOT.size()));
            ItemStack stack = material.getDefaultInstance();
            stack.setCount(boss ? 2 + random.nextInt(3) : 1 + random.nextInt(2));
            giveOrDrop(killer, stack);
        }

        float equipmentChance = boss ? 1.0f : 0.045f + logistics;
        if (random.nextFloat() < equipmentChance) {
            giveOrDrop(killer, VillageEquipmentRaritySystem.createRaidDrop(
                    VillageCouncilState.currentDay(), boss, random));
        }
    }

    private static boolean isBoss(Mob mob) {
        if (VillageRaidSystem.isBossEnemy(mob)) return true;
        String name = mob.getCustomName() == null ? "" : mob.getCustomName().getString();
        for (VillageEnemyArchetypeSystem.Archetype archetype : VillageEnemyArchetypeSystem.Archetype.values()) {
            if (VillageEnemyArchetypeSystem.isBoss(archetype) && name.contains(archetype.displayName())) return true;
        }
        return false;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
}

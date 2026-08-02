package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;

/** Raid rewards use game-specific sale loot names and graded equipment. */
public final class VillageRaidLootSystem {
    private static final List<SaleLoot> COMMON_SALE_LOOT = List.of(
            new SaleLoot(Items.BONE, "금 간 오크 송곳니", ChatFormatting.GRAY),
            new SaleLoot(Items.STRING, "찢긴 전투 끈", ChatFormatting.GRAY),
            new SaleLoot(Items.SPIDER_EYE, "응고된 마력낭", ChatFormatting.DARK_PURPLE),
            new SaleLoot(Items.GUNPOWDER, "폭파병 화약 주머니", ChatFormatting.GOLD));
    private static final List<SaleLoot> BOSS_SALE_LOOT = List.of(
            new SaleLoot(Items.ENDER_PEARL, "뒤틀린 지휘핵", ChatFormatting.LIGHT_PURPLE),
            new SaleLoot(Items.BLAZE_ROD, "전쟁 주술봉 파편", ChatFormatting.GOLD));

    private VillageRaidLootSystem() {}

    public static void handleDrops(LivingDropsEvent event) {
        if (!VillageRaidSystem.isRaidEnemy(event.getEntity())) return;
        event.getDrops().clear();
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        RandomSource random = event.getEntity().getRandom();
        boolean boss = event.getEntity() instanceof Mob mob && isBoss(mob);
        float logistics = VillageDefenseResearchSystem.equipmentDropBonus();

        if (random.nextFloat() < (boss ? 1.0f : 0.28f)) {
            SaleLoot loot = boss
                    ? BOSS_SALE_LOOT.get(random.nextInt(BOSS_SALE_LOOT.size()))
                    : COMMON_SALE_LOOT.get(random.nextInt(COMMON_SALE_LOOT.size()));
            ItemStack stack = namedSaleLoot(loot);
            stack.setCount(boss ? 2 + random.nextInt(3) : 1 + random.nextInt(2));
            giveOrDrop(killer, stack);
        }

        float equipmentChance = boss ? 1.0f : 0.045f + logistics;
        if (random.nextFloat() < equipmentChance) {
            giveOrDrop(killer, VillageEquipmentRaritySystem.createRaidDrop(
                    VillageCouncilState.currentDay(), boss, random));
        }
    }

    private static ItemStack namedSaleLoot(SaleLoot loot) {
        ItemStack stack = loot.item().getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("[판매용] " + loot.name()).withStyle(loot.color()));
        return stack;
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

    private record SaleLoot(Item item, String name, ChatFormatting color) {}
}

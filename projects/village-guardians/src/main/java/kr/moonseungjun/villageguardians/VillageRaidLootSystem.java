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

import java.util.EnumMap;
import java.util.Map;

public final class VillageRaidLootSystem {
    private static final Map<VillageEnemyArchetypeSystem.Archetype, SaleLoot> SALE_LOOT =
            new EnumMap<>(VillageEnemyArchetypeSystem.Archetype.class);
    static {
        put(VillageEnemyArchetypeSystem.Archetype.GRUNT, Items.BONE, "금 간 전열병 송곳니", ChatFormatting.GRAY, 3);
        put(VillageEnemyArchetypeSystem.Archetype.RUSHER, Items.FLINT, "척후병의 닳은 단검 조각", ChatFormatting.GRAY, 4);
        put(VillageEnemyArchetypeSystem.Archetype.BULWARK, Items.IRON_NUGGET, "찌그러진 방패 고리", ChatFormatting.WHITE, 6);
        put(VillageEnemyArchetypeSystem.Archetype.SAPPER, Items.GUNPOWDER, "폭파병 화약 주머니", ChatFormatting.GOLD, 7);
        put(VillageEnemyArchetypeSystem.Archetype.MARKSMAN, Items.FEATHER, "사수의 찢긴 깃", ChatFormatting.WHITE, 6);
        put(VillageEnemyArchetypeSystem.Archetype.SHIELDBREAKER, Items.IRON_NUGGET, "파쇄병 도끼날 파편", ChatFormatting.DARK_GRAY, 8);
        put(VillageEnemyArchetypeSystem.Archetype.HEXER, Items.SPIDER_EYE, "응고된 저주 마력낭", ChatFormatting.DARK_PURPLE, 9);
        put(VillageEnemyArchetypeSystem.Archetype.WAR_CHANTER, Items.GOAT_HORN, "전쟁 고수의 갈라진 뿔", ChatFormatting.GOLD, 10);
        put(VillageEnemyArchetypeSystem.Archetype.NECROMANCER, Items.COAL, "사령술사의 검은 뼛가루", ChatFormatting.DARK_PURPLE, 12);
        put(VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER, Items.AMETHYST_SHARD, "탑 사냥꾼의 조준 렌즈", ChatFormatting.AQUA, 14);
        put(VillageEnemyArchetypeSystem.Archetype.SIEGE_BEAST, Items.HEAVY_CORE, "공성 야수의 파쇄핵", ChatFormatting.LIGHT_PURPLE, 28);
        put(VillageEnemyArchetypeSystem.Archetype.IRON_WARLORD, Items.NETHERITE_SCRAP, "철의 전쟁군주 휘장", ChatFormatting.GOLD, 34);
        put(VillageEnemyArchetypeSystem.Archetype.PLAGUE_ARCHON, Items.ENDER_PEARL, "역병 대주교의 뒤틀린 심장", ChatFormatting.DARK_PURPLE, 38);
        put(VillageEnemyArchetypeSystem.Archetype.DREAD_KNIGHT, Items.ECHO_SHARD, "공포 기사의 암흑 갑편", ChatFormatting.DARK_AQUA, 42);
    }

    private VillageRaidLootSystem() {}
    private static void put(VillageEnemyArchetypeSystem.Archetype type, Item item,
                            String name, ChatFormatting color, int value) {
        SALE_LOOT.put(type, new SaleLoot(item, name, color, Math.max(1, value)));
    }

    public static int saleValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom == null) return 0;
        String plain = ChatFormatting.stripFormatting(custom.getString());
        for (SaleLoot loot : SALE_LOOT.values()) {
            if (("[판매용] " + loot.name()).equals(plain)) return loot.value();
        }
        return 0;
    }

    public static void handleDrops(LivingDropsEvent event) {
        if (VillageSkillTestSystem.isTestDummy(event.getEntity())) { event.getDrops().clear(); return; }
        if (!VillageRaidSystem.isRaidEnemy(event.getEntity())) return;
        event.getDrops().clear();
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)
                || !(event.getEntity() instanceof Mob mob)) return;
        RandomSource random = mob.getRandom();
        VillageEnemyArchetypeSystem.Archetype type = VillageRaidSystem.archetypeOf(mob);
        boolean boss = type != null && VillageEnemyArchetypeSystem.isBoss(type);
        SaleLoot loot = SALE_LOOT.get(type);
        float chance = boss ? 1.0f : switch (type == null
                ? VillageEnemyArchetypeSystem.Archetype.GRUNT : type) {
            case GRUNT, RUSHER -> 0.28f;
            case BULWARK, MARKSMAN -> 0.36f;
            case SAPPER, SHIELDBREAKER, HEXER, WAR_CHANTER -> 0.48f;
            case NECROMANCER, TOWER_HUNTER -> 0.62f;
            default -> 1.0f;
        };
        if (loot != null && random.nextFloat() < chance) {
            ItemStack stack = named(loot);
            stack.setCount(boss ? 2 + random.nextInt(3) : 1 + random.nextInt(2));
            give(killer, stack);
        }
        float equipmentChance = boss ? 1.0f : 0.045f
                + VillageDefenseResearchSystem.equipmentDropBonus();
        if (random.nextFloat() < equipmentChance) {
            give(killer, VillageExpandedEquipmentSystem.createRaidDrop(
                    VillageCouncilState.currentDay(), boss, type, random));
        }
    }

    private static ItemStack named(SaleLoot loot) {
        ItemStack stack = loot.item().getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("[판매용] " + loot.name()).withStyle(loot.color()));
        return stack;
    }
    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
    private record SaleLoot(Item item, String name, ChatFormatting color, int value) {}
}

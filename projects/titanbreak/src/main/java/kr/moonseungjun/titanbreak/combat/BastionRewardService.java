package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.entity.BastionWalkerEntity;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class BastionRewardService {
    private static final int FIRST_KILL_RD = 400;

    private BastionRewardService() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof BastionWalkerEntity victim)
                || !(victim.level() instanceof ServerLevel level)) return;

        drop(level, victim, ModItems.BASTION_ARMOR_CORE.get(), 1);
        drop(level, victim, ModItems.COMPOSITE_ARMOR_PLATE.get(), 6 + victim.getRandom().nextInt(5));
        drop(level, victim, ModItems.DENSE_BONE_LATTICE.get(), 4 + victim.getRandom().nextInt(3));

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        int levels = data.addAdaptationXp(player, 700);
        data.addMasteryXpToInstalled(player, 95);
        if (levels > 0) {
            TitanPlayerData.State state = data.state(player);
            player.sendSystemMessage(Component.translatable("message.titanbreak.adaptation_level",
                    state.adaptationLevel(), state.adaptationPoints()), true);
        }

        boolean first = data.recordBossFirstKill(player, "bastion_walker", FIRST_KILL_RD, 4);
        if (first) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.first_hunt_rd", FIRST_KILL_RD), true);
            player.sendSystemMessage(Component.translatable("message.titanbreak.bastion_defeated"));
        }
        TitanbreakNetwork.sync(player);
    }

    private static void drop(ServerLevel level, BastionWalkerEntity victim, Item item, int count) {
        if (count <= 0) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 2.0D, victim.getZ(),
                new ItemStack(item, count));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}

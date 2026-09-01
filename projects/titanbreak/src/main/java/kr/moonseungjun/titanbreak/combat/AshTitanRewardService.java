package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.entity.AshTitanEntity;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AshTitanRewardService {
    private static final int FIRST_KILL_RD = 940;

    private AshTitanRewardService() {}

    public static void award(AshTitanEntity victim, ServerLevel level, DamageSource source) {
        drop(level, victim, ModItems.ASH_RADIANT_HEART.get(), 1);
        drop(level, victim, ModItems.RADIATION_CORE.get(), 2);
        drop(level, victim, ModItems.HEAT_SINK.get(), 4 + victim.getRandom().nextInt(3));

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        int levels = data.addAdaptationXp(player, 1380);
        data.addMasteryXpToInstalled(player, 185);
        if (levels > 0) {
            TitanPlayerData.State state = data.state(player);
            player.sendSystemMessage(Component.translatable("message.titanbreak.adaptation_level",
                    state.adaptationLevel(), state.adaptationPoints()), true);
        }

        boolean first = data.recordBossFirstKill(player, "ash_titan", FIRST_KILL_RD, 7);
        if (first) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.first_hunt_rd", FIRST_KILL_RD), true);
            player.sendSystemMessage(Component.translatable("message.titanbreak.ash_titan_defeated"));
        }
        TitanbreakNetwork.sync(player);
    }

    private static void drop(ServerLevel level, AshTitanEntity victim, Item item, int count) {
        if (count <= 0) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 1.5D, victim.getZ(),
                new ItemStack(item, count));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}

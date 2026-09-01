package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.entity.RegnantFleshEntity;
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

public final class RegnantRewardService {
    private static final int FIRST_KILL_RD = 400;

    private RegnantRewardService() {}

    public static void award(RegnantFleshEntity victim, ServerLevel level, DamageSource source) {
        drop(level, victim, ModItems.REGNANT_REGENERATION_CORE.get(), 1);
        drop(level, victim, ModItems.REGENERATIVE_TISSUE.get(), 5 + victim.getRandom().nextInt(4));
        drop(level, victim, ModItems.CIRCULATION_CORE.get(), 2 + victim.getRandom().nextInt(2));
        drop(level, victim, ModItems.NANO_MEDIUM.get(), 1);

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        int levels = data.addAdaptationXp(player, 600);
        data.addMasteryXpToInstalled(player, 90);
        if (levels > 0) {
            TitanPlayerData.State state = data.state(player);
            player.sendSystemMessage(Component.translatable("message.titanbreak.adaptation_level",
                    state.adaptationLevel(), state.adaptationPoints()), true);
        }

        boolean first = data.recordBossFirstKill(player, "regnant_flesh", FIRST_KILL_RD, 3);
        if (first) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.first_hunt_rd", FIRST_KILL_RD), true);
            player.sendSystemMessage(Component.translatable("message.titanbreak.regnant_defeated"));
        }
        TitanbreakNetwork.sync(player);
    }

    private static void drop(ServerLevel level, RegnantFleshEntity victim, Item item, int count) {
        if (count <= 0) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 1.5D, victim.getZ(),
                new ItemStack(item, count));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}

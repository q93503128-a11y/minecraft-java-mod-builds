package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.entity.NullSeraphEntity;
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

public final class NullSeraphRewardService {
    private static final int FIRST_KILL_RD = 1120;

    private NullSeraphRewardService() {}

    public static void award(NullSeraphEntity victim, ServerLevel level, DamageSource source) {
        drop(level, victim, ModItems.NULL_SUPPRESSION_CORE.get(), 1);
        drop(level, victim, ModItems.RESONANT_NEURAL_GANGLION.get(), 3);
        drop(level, victim, ModItems.CALCULATION_CORE.get(), 4);

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        int levels = data.addAdaptationXp(player, 1620);
        data.addMasteryXpToInstalled(player, 215);
        if (levels > 0) {
            TitanPlayerData.State state = data.state(player);
            player.sendSystemMessage(Component.translatable("message.titanbreak.adaptation_level",
                    state.adaptationLevel(), state.adaptationPoints()), true);
        }

        boolean first = data.recordBossFirstKill(player, "null_seraph", FIRST_KILL_RD, 8);
        if (first) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.first_hunt_rd", FIRST_KILL_RD), true);
            player.sendSystemMessage(Component.translatable("message.titanbreak.null_seraph_defeated"));
        }
        NullSuppressionService.clear(player.getUUID());
        AnalysisJammingService.clear(player.getUUID());
        TitanbreakNetwork.sync(player);
    }

    private static void drop(ServerLevel level, NullSeraphEntity victim, Item item, int count) {
        if (count <= 0) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 1.5D, victim.getZ(),
                new ItemStack(item, count));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}

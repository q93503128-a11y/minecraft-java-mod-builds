package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.entity.WorldbreakerEntity;
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

public final class WorldbreakerRewardService {
    private static final int FIRST_KILL_RD = 1_600;

    private WorldbreakerRewardService() {}

    public static void award(WorldbreakerEntity victim, ServerLevel level, DamageSource source) {
        drop(level, victim, ModItems.WORLDBREAKER_CORE.get(), 1);
        drop(level, victim, ModItems.TEMPORAL_NEURAL_BUNDLE.get(), 4 + victim.getRandom().nextInt(3));
        drop(level, victim, ModItems.PREDICTIVE_OPTIC_CORE.get(), 4 + victim.getRandom().nextInt(3));
        drop(level, victim, ModItems.IMPACT_CORE.get(), 5 + victim.getRandom().nextInt(4));
        drop(level, victim, ModItems.DENSE_BONE_LATTICE.get(), 4 + victim.getRandom().nextInt(3));
        drop(level, victim, ModItems.RADIATION_CORE.get(), 4 + victim.getRandom().nextInt(3));
        drop(level, victim, ModItems.NANO_MEDIUM.get(), 4 + victim.getRandom().nextInt(3));
        drop(level, victim, ModItems.TEMPORAL_ORGAN.get(), 3 + victim.getRandom().nextInt(3));

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        int levels = data.addAdaptationXp(player, 2_200);
        data.addMasteryXpToInstalled(player, 280);
        if (levels > 0) {
            TitanPlayerData.State state = data.state(player);
            player.sendSystemMessage(Component.translatable("message.titanbreak.adaptation_level",
                    state.adaptationLevel(), state.adaptationPoints()), true);
        }

        boolean first = data.recordBossFirstKill(player, "worldbreaker", FIRST_KILL_RD, 10);
        if (first) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.first_hunt_rd", FIRST_KILL_RD), true);
            player.sendSystemMessage(Component.translatable("message.titanbreak.worldbreaker_defeated"));
        }
        TitanbreakNetwork.sync(player);
    }

    private static void drop(ServerLevel level, WorldbreakerEntity victim, Item item, int count) {
        if (count <= 0) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 2.0D, victim.getZ(),
                new ItemStack(item, count));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}

package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.entity.RipperEntity;
import kr.moonseungjun.titanbreak.entity.SkitterEntity;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class HuntRewardService {
    private static final int NORMAL_FIRST_KILL_RD = 10;

    private HuntRewardService() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;

        String speciesKey;
        if (victim instanceof RipperEntity) {
            speciesKey = "ripper";
            drop(level, victim, ModItems.HIGH_DENSITY_MUSCLE_FIBER.get(), 1 + victim.getRandom().nextInt(2));
            if (victim.getRandom().nextFloat() < 0.20F) {
                drop(level, victim, ModItems.HIGH_DENSITY_NEURAL_FIBER.get(), 1);
            }
        } else if (victim instanceof SkitterEntity) {
            speciesKey = "skitter";
            drop(level, victim, ModItems.SERVO_BUNDLE.get(), 1);
            drop(level, victim, ModItems.SYNTHETIC_TENDON.get(), 1 + victim.getRandom().nextInt(2));
        } else {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        if (data.recordNormalFirstKill(player, speciesKey, NORMAL_FIRST_KILL_RD)) {
            player.displayClientMessage(Component.translatable("message.titanbreak.first_hunt_rd", NORMAL_FIRST_KILL_RD), true);
            TitanbreakNetwork.sync(player);
        }
    }

    private static void drop(ServerLevel level, LivingEntity victim, Item item, int count) {
        if (count <= 0) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.35D, victim.getZ(),
                new ItemStack(item, count));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}

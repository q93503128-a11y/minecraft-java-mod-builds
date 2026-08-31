package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.entity.BulwarkEntity;
import kr.moonseungjun.titanbreak.entity.ChronoHoundEntity;
import kr.moonseungjun.titanbreak.entity.HowlerEntity;
import kr.moonseungjun.titanbreak.entity.NeedlerEntity;
import kr.moonseungjun.titanbreak.entity.NullEyeEntity;
import kr.moonseungjun.titanbreak.entity.PursuerEntity;
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
    private static final int ELITE_FIRST_KILL_RD = 40;
    private static final int BOSS_FIRST_KILL_RD = 150;

    private HuntRewardService() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;

        HuntClass huntClass;
        String speciesKey;
        int adaptationXp;

        if (victim instanceof RipperEntity) {
            huntClass = HuntClass.NORMAL;
            speciesKey = "ripper";
            adaptationXp = 20;
            drop(level, victim, ModItems.HIGH_DENSITY_MUSCLE_FIBER.get(), 1 + victim.getRandom().nextInt(2));
            chanceDrop(level, victim, ModItems.HIGH_DENSITY_NEURAL_FIBER.get(), 1, 0.20F);
        } else if (victim instanceof SkitterEntity) {
            huntClass = HuntClass.NORMAL;
            speciesKey = "skitter";
            adaptationXp = 20;
            drop(level, victim, ModItems.SERVO_BUNDLE.get(), 1);
            drop(level, victim, ModItems.SYNTHETIC_TENDON.get(), 1 + victim.getRandom().nextInt(2));
        } else if (victim instanceof BulwarkEntity) {
            huntClass = HuntClass.NORMAL;
            speciesKey = "bulwark";
            adaptationXp = 25;
            drop(level, victim, ModItems.COMPOSITE_ARMOR_PLATE.get(), 1 + victim.getRandom().nextInt(3));
            chanceDrop(level, victim, ModItems.DENSE_BONE_LATTICE.get(), 1, 0.30F);
        } else if (victim instanceof NeedlerEntity) {
            huntClass = HuntClass.NORMAL;
            speciesKey = "needler";
            adaptationXp = 25;
            drop(level, victim, ModItems.OPTIC_SENSOR_CLUSTER.get(), 1 + victim.getRandom().nextInt(2));
            drop(level, victim, ModItems.CALCULATION_CORE.get(), 1);
        } else if (victim instanceof HowlerEntity) {
            huntClass = HuntClass.NORMAL;
            speciesKey = "howler";
            adaptationXp = 25;
            drop(level, victim, ModItems.RESONANT_NEURAL_GANGLION.get(), 1 + victim.getRandom().nextInt(2));
            chanceDrop(level, victim, ModItems.THERMAL_OPTIC_CLUSTER.get(), 1, 0.25F);
        } else if (victim instanceof ChronoHoundEntity) {
            huntClass = HuntClass.ELITE;
            speciesKey = "chrono_hound";
            adaptationXp = 90;
            chanceDrop(level, victim, ModItems.TEMPORAL_NEURAL_BUNDLE.get(), 1, 0.35F);
            drop(level, victim, ModItems.REACTION_TEMPORAL_MATRIX.get(), 1 + victim.getRandom().nextInt(2));
        } else if (victim instanceof NullEyeEntity) {
            huntClass = HuntClass.ELITE;
            speciesKey = "null_eye";
            adaptationXp = 90;
            chanceDrop(level, victim, ModItems.PREDICTIVE_OPTIC_CORE.get(), 1, 0.25F);
            drop(level, victim, ModItems.THERMAL_OPTIC_CLUSTER.get(), 2);
        } else if (victim instanceof PursuerEntity) {
            huntClass = HuntClass.BOSS;
            speciesKey = "the_pursuer";
            adaptationXp = 350;
            drop(level, victim, ModItems.PURSUER_REACTION_ORGAN.get(), 1);
            drop(level, victim, ModItems.TEMPORAL_NEURAL_BUNDLE.get(), 1 + victim.getRandom().nextInt(2));
            drop(level, victim, ModItems.REACTION_TEMPORAL_MATRIX.get(), 2 + victim.getRandom().nextInt(3));
        } else {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        int levels = data.addAdaptationXp(player, adaptationXp);
        int masteryReward = switch (huntClass) {
            case NORMAL -> 6;
            case ELITE -> 18;
            case BOSS -> 60;
        };
        data.addMasteryXpToInstalled(player, masteryReward);

        if (levels > 0) {
            TitanPlayerData.State state = data.state(player);
            player.sendSystemMessage(Component.translatable("message.titanbreak.adaptation_level",
                    state.adaptationLevel(), state.adaptationPoints()), true);
        }

        boolean first = switch (huntClass) {
            case NORMAL -> data.recordNormalFirstKill(player, speciesKey, NORMAL_FIRST_KILL_RD);
            case ELITE -> data.recordEliteFirstKill(player, speciesKey, ELITE_FIRST_KILL_RD);
            case BOSS -> data.recordBossFirstKill(player, speciesKey, BOSS_FIRST_KILL_RD, 3);
        };
        if (first) {
            int reward = switch (huntClass) {
                case NORMAL -> NORMAL_FIRST_KILL_RD;
                case ELITE -> ELITE_FIRST_KILL_RD;
                case BOSS -> BOSS_FIRST_KILL_RD;
            };
            player.sendSystemMessage(Component.translatable("message.titanbreak.first_hunt_rd", reward), true);
            TitanPlayerData.State state = data.state(player);
            if (huntClass == HuntClass.NORMAL && state.normalFirstKillCount() == 5) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.normal_catalog_complete"));
            } else if (huntClass == HuntClass.ELITE && state.eliteFirstKillCount() == 2) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.elite_catalog_complete"));
            } else if (huntClass == HuntClass.BOSS) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.pursuer_defeated"));
            }
        }
        TitanbreakNetwork.sync(player);
    }

    private static void chanceDrop(ServerLevel level, LivingEntity victim, Item item, int count, float chance) {
        if (victim.getRandom().nextFloat() < chance) drop(level, victim, item, count);
    }

    private static void drop(ServerLevel level, LivingEntity victim, Item item, int count) {
        if (count <= 0) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.35D, victim.getZ(), new ItemStack(item, count));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }

    private enum HuntClass { NORMAL, ELITE, BOSS }
}

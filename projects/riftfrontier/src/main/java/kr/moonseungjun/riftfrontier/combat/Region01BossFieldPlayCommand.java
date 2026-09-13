package kr.moonseungjun.riftfrontier.combat;

import com.mojang.brigadier.Command;
import kr.moonseungjun.riftfrontier.entity.Region01BossEntity;
import kr.moonseungjun.riftfrontier.entity.RiftfrontierEntityTypes;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;

/** Explicit development/field-play surface for the not-yet-production Region 01 boss encounter. */
public final class Region01BossFieldPlayCommand {
    private Region01BossFieldPlayCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("riftfrontier")
                .then(Commands.literal("boss")
                    .then(Commands.literal("fieldtest")
                        .then(Commands.literal("spawn").executes(context -> spawn(
                            context.getSource().getPlayerOrException()
                        )))
                        .then(Commands.literal("phase1").executes(context -> setPhase(
                            context.getSource().getPlayerOrException(), 1
                        )))
                        .then(Commands.literal("phase2").executes(context -> setPhase(
                            context.getSource().getPlayerOrException(), 2
                        )))
                    )
                )
        );
    }

    private static int spawn(ServerPlayer player) {
        ServerLevel level = player.level();
        Region01BossEntity boss = new Region01BossEntity(RiftfrontierEntityTypes.REGION_01_BOSS.get(), level);
        Vec3 position = player.position().add(player.getLookAngle().normalize().scale(6.0D));
        boss.setPos(position.x, position.y, position.z);
        boss.setYRot(player.getYRot() + 180.0F);
        boss.enableFieldTestCombat();
        if (!level.addFreshEntity(boss)) {
            player.sendSystemMessage(Component.literal("Riftfrontier Region 01 boss field-test spawn failed."));
            return 0;
        }
        player.sendSystemMessage(Component.literal(
            "Region 01 boss field-test actor spawned 6 blocks ahead. Diagnostic damage/reach are provisional; use '/riftfrontier boss fieldtest phase2' to exercise the phase-2 attack pool."
        ));
        return Command.SINGLE_SUCCESS;
    }

    private static int setPhase(ServerPlayer player, int phase) {
        Region01BossEntity boss = nearestFieldTestBoss(player, 64.0D);
        if (boss == null) {
            player.sendSystemMessage(Component.literal("No enabled Region 01 boss field-test actor found within 64 blocks."));
            return 0;
        }
        boss.setFieldTestPhase(phase);
        player.sendSystemMessage(Component.literal("Region 01 boss field-test phase set to " + phase + "."));
        return Command.SINGLE_SUCCESS;
    }

    private static Region01BossEntity nearestFieldTestBoss(ServerPlayer player, double radius) {
        AABB search = player.getBoundingBox().inflate(radius);
        return player.level().getEntitiesOfClass(
                Region01BossEntity.class,
                search,
                Region01BossEntity::fieldTestCombatEnabled
            ).stream()
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
    }
}
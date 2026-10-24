package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** One successful last-death teleport is granted by each actual death. */
public final class DeathReturnAugmentService {
    private static final String DEATH_RETURN_ID = id("death_return");
    private static final String AVAILABLE_KEY = "death_return/available";

    private DeathReturnAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(DEATH_RETURN_ID);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                return;
            }
            AugmentationPlayerState state = AugmentationPlayerStateService.state(newPlayer);
            if (boundedLevel(state) <= 0 || newPlayer.getLastDeathLocation().isEmpty()) {
                return;
            }
            AugmentationPlayerStateService.replace(
                    newPlayer,
                    state.withPersistentInt(AVAILABLE_KEY, 1)
            );
        });
    }

    public static boolean tryUse(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state) <= 0 || state.persistentInt(AVAILABLE_KEY) <= 0) {
            return false;
        }
        Optional<GlobalPos> lastDeath = player.getLastDeathLocation();
        if (lastDeath.isEmpty()) {
            return false;
        }
        GlobalPos target = lastDeath.get();
        ServerLevel targetLevel = player.level().getServer().getLevel(target.dimension());
        if (targetLevel == null) {
            return false;
        }
        Vec3 destination = Vec3.atBottomCenterOf(target.pos());
        ServerPlayer teleported = player.teleport(new TeleportTransition(
                targetLevel,
                destination,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                TeleportTransition.DO_NOTHING
        ));
        if (teleported == null) {
            return false;
        }
        teleported.resetFallDistance();
        AugmentationPlayerStateService.replace(
                teleported,
                AugmentationPlayerStateService.state(teleported).withPersistentInt(AVAILABLE_KEY, 0)
        );
        return true;
    }

    private static int boundedLevel(AugmentationPlayerState state) {
        int level = state.level(DEATH_RETURN_ID);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(DEATH_RETURN_ID).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}

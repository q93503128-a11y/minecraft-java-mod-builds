package dev.moonseungjun.augmentationsurvival.state;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.MobilityUtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.stat.PlayerStatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.threat.ThreatScalingService;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;

/** Server-only mutation boundary for persistent augmentation player state. */
public final class AugmentationPlayerStateService {
    private AugmentationPlayerStateService() {
    }

    public static AugmentationPlayerState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                AugmentationAttachments.PLAYER_STATE,
                AugmentationPlayerState.initial()
        );
    }

    public static boolean hasBossFirstKillReward(ServerPlayer player, String bossId) {
        return state(player).hasBossFirstKillReward(bossId);
    }

    public static boolean markBossFirstKillRewardClaimed(ServerPlayer player, String bossId) {
        AugmentationPlayerState before = state(player);
        AugmentationPlayerState next = before.withBossFirstKillReward(bossId);
        if (next.equals(before)) {
            return false;
        }
        replace(player, next);
        return true;
    }

    public static AugmentationPlayerState update(
            ServerPlayer player,
            UnaryOperator<AugmentationPlayerState> operation
    ) {
        Objects.requireNonNull(operation, "operation");
        return replace(player, operation.apply(state(player)));
    }

    public static AugmentationPlayerState replace(
            ServerPlayer player,
            AugmentationPlayerState next
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(next, "next");
        AugmentationPlayerState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(AugmentationAttachments.PLAYER_STATE, next);
        PlayerStatAugmentService.reconcile(player, next);
        MovementAugmentService.reconcile(player, next);
        MobilityUtilityAugmentService.reconcile(player, next);
        ReactiveCombatAugmentService.reconcile(player, next);
        SurvivalAugmentService.reconcile(player, next);
        AdvancedUtilityAugmentService.reconcile(player, next);
        ConsumptionAugmentService.syncClient(player, next);
        if (!current.augmentLevels().equals(next.augmentLevels())) {
            ThreatScalingService.observePlayerPower(player, next);
        }
        return next;
    }
}

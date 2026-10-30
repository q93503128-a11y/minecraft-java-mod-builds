package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Last Stand owns the whole three-second survival window: no delayed debt may survive or reappear
 * while that window is active. This is deliberately enforced at the protection runtime itself in
 * addition to the trigger-time clear so later compatibility hooks cannot requeue stale debt.
 */
@Mixin(value = ProtectionAugmentService.class, remap = false)
abstract class ProtectionLastStandDebtGuardMixin {
    private static final String AUGMENTATION_SURVIVAL$LAST_STAND_ID =
            AugmentationSurvival.MOD_ID + ":last_stand";

    @Inject(method = "tickRuntime", at = @At("HEAD"))
    private static void augmentationSurvival$purgeDebtDuringLastStand(MinecraftServer server, CallbackInfo ci) {
        Map<UUID, Object> runtimes = ProtectionRuntimeAccessor.augmentationSurvival$getRuntime();
        if (runtimes.isEmpty()) return;

        CombatTuning.Snapshot tuning = CombatTuning.current();
        for (Map.Entry<UUID, Object> entry : runtimes.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !augmentationSurvival$isLastStandWindow(player, tuning)) continue;
            if (entry.getValue() instanceof ProtectionRuntimeStateAccessor accessor) {
                accessor.augmentationSurvival$getDelayed().clear();
            }
        }
    }

    private static boolean augmentationSurvival$isLastStandWindow(
            ServerPlayer player,
            CombatTuning.Snapshot tuning
    ) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.level(AUGMENTATION_SURVIVAL$LAST_STAND_ID) <= 0) return false;
        long now = player.level().getGameTime();
        long readyTick = state.cooldownReadyTick(AUGMENTATION_SURVIVAL$LAST_STAND_ID);
        if (readyTick <= now) return false;
        long triggeredAt = readyTick - tuning.lastStandCooldownTicks();
        return triggeredAt <= now && now < triggeredAt + tuning.lastStandInvulnerableTicks();
    }
}

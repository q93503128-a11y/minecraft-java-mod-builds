package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.ClientTemporalState;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalTuning;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import dev.moonseungjun.augmentationsurvival.client.ClientInstantMiningState;
import dev.moonseungjun.augmentationsurvival.client.ClientMiningPredictionState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Mirrors the shared additive mining-speed axis on the local player for break-progress prediction. */
@Mixin(Player.class)
abstract class ClientMiningAugmentMixin {
    private static final String MINERS_HAND_ID = id("miners_hand");
    private static final String LOGGING_KNACK_ID = id("logging_knack");
    private static final String NOCTURNAL_ID = id("nocturnal");
    private static final String INSTANT_MINING_ID = id("instant_mining");

    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float augmentationSurvival$predictMiningSpeed(float original, BlockState state) {
        if (!((Object) this instanceof LocalPlayer player) || !(original > 0.0F) || !Float.isFinite(original)) {
            return original;
        }

        if (ClientInstantMiningState.enabled()
                && ClientAugmentProgressState.level(INSTANT_MINING_ID) > 0
                && player.hasCorrectToolForDrops(state)) {
            return Float.MAX_VALUE;
        }

        double bonus = 0.04D * ClientAugmentProgressState.level(MINERS_HAND_ID);
        int logging = ClientAugmentProgressState.level(LOGGING_KNACK_ID);
        if (logging > 0 && state.is(BlockTags.LOGS)) {
            bonus += SurvivalTuning.current().loggingBreakSpeedPerLevel() * logging;
        }
        int nocturnal = ClientAugmentProgressState.level(NOCTURNAL_ID);
        if (nocturnal > 0 && !isDaytime(player)) {
            bonus += SurvivalTuning.current().nocturnalMiningSpeedPerLevel() * nocturnal;
        }
        bonus += ClientMiningPredictionState.miningSpeedBonus();
        if (ClientTemporalState.timeAccelerationActive()) {
            bonus += AdvancedActiveTuning.current().timeAccelerationMiningBonus();
        }

        double scaled = original * (1.0D + bonus);
        return Double.isFinite(scaled) ? (float) Math.min(Float.MAX_VALUE, scaled) : original;
    }

    private static boolean isDaytime(LocalPlayer player) {
        long dayTime = Math.floorMod(player.level().getOverworldClockTime(), 24_000L);
        return dayTime < 13_000L;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}

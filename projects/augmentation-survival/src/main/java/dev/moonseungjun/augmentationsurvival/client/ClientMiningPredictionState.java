package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalTuning;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;

/** Client prediction mirror for the ROOT-only Continuous Mining stack. Server gameplay remains authoritative. */
public final class ClientMiningPredictionState {
    private static final String CONTINUOUS_MINING_ID = AugmentationSurvival.MOD_ID + ":continuous_mining";
    private static int miningStacks;
    private static long miningUntilTick;

    private ClientMiningPredictionState() { }

    public static void initialize() {
        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> {
            int level = ClientAugmentProgressState.level(CONTINUOUS_MINING_ID);
            if (level <= 0) {
                clear();
                return;
            }
            long now = world.getGameTime();
            SurvivalTuning.Snapshot tuning = SurvivalTuning.current();
            if (miningUntilTick >= now) {
                miningStacks = Math.min(tuning.continuousMiningMaxStacks(), miningStacks + 1);
            } else {
                miningStacks = 1;
            }
            miningUntilTick = now + tuning.continuousMiningWindowTicks();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null
                    || ClientAugmentProgressState.level(CONTINUOUS_MINING_ID) <= 0) {
                clear();
                return;
            }
            if (miningUntilTick < client.level.getGameTime()) {
                miningStacks = 0;
            }
        });
    }

    public static double miningSpeedBonus() {
        int level = ClientAugmentProgressState.level(CONTINUOUS_MINING_ID);
        if (level <= 0 || miningStacks <= 0) return 0.0D;
        return SurvivalTuning.current().continuousMiningSpeedPerStackPerLevel() * level * miningStacks;
    }

    private static void clear() {
        miningStacks = 0;
        miningUntilTick = 0L;
    }
}

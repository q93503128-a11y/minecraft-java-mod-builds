package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Server-side physiological effects for ocular augmentations that need real gameplay state.
 * Analysis overlays themselves stay client-side; this service only supplies low-light vision.
 */
public final class OcularAugmentationService {
    private OcularAugmentationService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance multispectrum = state.firstInstalledInstance("multispectrum_eye");
        if (multispectrum == null) return;

        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || current.getDuration() < 50) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 90, 0, true, false, false));
        }
    }
}

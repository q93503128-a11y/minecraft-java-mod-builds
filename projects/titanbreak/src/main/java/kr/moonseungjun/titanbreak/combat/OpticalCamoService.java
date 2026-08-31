package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Automatic visual concealment rules for Optical Camo Skin. */
public final class OpticalCamoService {
    private static final int EFFECT_REFRESH_TICKS = 8;

    private OpticalCamoService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance camo = state.firstInstalledInstance("optical_camo_skin");
        if (camo == null || player.isSpectator()) return;

        Vec3 motion = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        boolean stationaryConcealment = player.isShiftKeyDown() && horizontalSpeed <= 0.045D;
        boolean movingConcealment = camo.enhancement() >= 5 && !player.isSprinting() && horizontalSpeed <= 0.30D;
        boolean concealed = stationaryConcealment || movingConcealment;
        if (!concealed) return;

        // Camo must be powered; evaluate on a short cadence so P12 does not become a per-tick 12-point drain.
        if (player.tickCount % 5 == 0) {
            if (!AugmentationResourceService.trySpendContinuousPower(player, state, "optical_camo_skin")) return;
            if (player.level() instanceof ServerLevel level) {
                TitanPlayerData data = TitanPlayerData.get(level.getServer());
                AugmentationCatalog.Definition definition = AugmentationCatalog.byId("optical_camo_skin");
                if (definition != null && definition.heatLoad() > 0) {
                    double thermalEfficiency = camo.enhancement() >= 7 ? 0.65D : 1.0D;
                    double rawHeat = definition.heatLoad() * 0.07D * thermalEfficiency
                            * state.heatLoadMultiplier("optical_camo_skin");
                    data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
                }
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_REFRESH_TICKS, 0, true, false, false));
    }
}

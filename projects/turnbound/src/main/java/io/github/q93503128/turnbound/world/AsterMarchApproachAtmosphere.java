package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Low-density field atmosphere around elite and boss approaches.
 *
 * This deliberately does not own encounter state or collision. It reads the existing campaign clear set and only
 * adds regional visual language while an authored threat is still alive: spores in Gloamwood, pressure/electricity
 * in the Aqueduct, ash/heat in the Quarry and unstable Relay particles in the final station.
 */
public final class AsterMarchApproachAtmosphere {
    private static final Vec3 GLOAM_ELITE = new Vec3(-75, 71, -365);
    private static final Vec3 AQUEDUCT_ELITE = new Vec3(-380, 65, 15);
    private static final Vec3 QUARRY_ELITE = new Vec3(-5, 65, 445);
    private static final Vec3 RELAY_ELITE_A = new Vec3(395, 67, -285);
    private static final Vec3 RELAY_ELITE_B = new Vec3(410, 66, -330);

    private AsterMarchApproachAtmosphere() {}

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();

        if (FieldSessionManager.active(player)) {
            boss(level, player, clears, "BATTLE_B01", AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01).position(),
                    ParticleTypes.CLOUD, ParticleTypes.CRIT);
            return;
        }
        if (GloamwoodSessionManager.active(player)) {
            elite(level, player, clears, "ENC_G04", GLOAM_ELITE, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.SOUL);
            boss(level, player, clears, "BATTLE_B02", AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B02).position(),
                    ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.ENCHANT);
            return;
        }
        if (BrokenAqueductSessionManager.active(player)) {
            elite(level, player, clears, "ENC_A04", AQUEDUCT_ELITE, ParticleTypes.DRIPPING_WATER, ParticleTypes.ELECTRIC_SPARK);
            boss(level, player, clears, "BATTLE_B03", AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B03).position(),
                    ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CLOUD);
            return;
        }
        if (EmberQuarrySessionManager.active(player)) {
            elite(level, player, clears, "ENC_Q04", QUARRY_ELITE, ParticleTypes.ASH, ParticleTypes.SMALL_FLAME);
            boss(level, player, clears, "BATTLE_B04", AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B04).position(),
                    ParticleTypes.ASH, ParticleTypes.FLAME);
            return;
        }
        if (OldRelayStationSessionManager.active(player)) {
            elite(level, player, clears, "ENC_R03", RELAY_ELITE_A, ParticleTypes.PORTAL, ParticleTypes.ELECTRIC_SPARK);
            elite(level, player, clears, "ENC_R04", RELAY_ELITE_B, ParticleTypes.SOUL, ParticleTypes.PORTAL);
            boss(level, player, clears, "BATTLE_B05", AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B05).position(),
                    ParticleTypes.PORTAL, ParticleTypes.SOUL_FIRE_FLAME);
        }
    }

    private static void elite(ServerLevel level, ServerPlayer player, Set<String> clears, String encounterId,
                              Vec3 center, ParticleOptions ambient, ParticleOptions accent) {
        if (clears.contains(encounterId)) return;
        double distanceSq = player.position().distanceToSqr(center);
        if (distanceSq > 20.0 * 20.0) return;
        pulse(level, player, center, ambient, 5, 3.2, 1.2, 3.2, 0.015);
        if (distanceSq <= 12.0 * 12.0) pulse(level, player, center.add(0, 1.2, 0), accent, 3, 1.8, 1.4, 1.8, 0.02);
    }

    private static void boss(ServerLevel level, ServerPlayer player, Set<String> clears, String encounterId,
                             Vec3 center, ParticleOptions ambient, ParticleOptions accent) {
        if (clears.contains(encounterId)) return;
        double distanceSq = player.position().distanceToSqr(center);
        if (distanceSq > 34.0 * 34.0) return;
        pulse(level, player, center, ambient, 7, 5.5, 1.5, 5.5, 0.018);
        if (distanceSq <= 24.0 * 24.0) pulse(level, player, center.add(0, 1.5, 0), accent, 5, 3.0, 2.0, 3.0, 0.025);
    }

    private static void pulse(ServerLevel level, ServerPlayer player, Vec3 center, ParticleOptions particle, int count,
                              double spreadX, double spreadY, double spreadZ, double speed) {
        PersonalPresentationIsolation.particles(level, player, particle,
                center.x, center.y, center.z, count, spreadX, spreadY, spreadZ, speed);
    }
}

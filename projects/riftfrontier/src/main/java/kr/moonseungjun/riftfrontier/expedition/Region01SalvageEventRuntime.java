package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * First Region 01 dynamic event adapter.
 *
 * The event is derived from authoritative contract progress rather than owning a second lifecycle flag:
 * crossing exactly two recovered salvage units can happen once because each field node is consumed on use.
 * Presentation intentionally borrows Minecraft's established sculk warning language while final Riftfrontier
 * VFX/audio direction remains under human review.
 */
public final class Region01SalvageEventRuntime {
    public static final int BLACKOUT_TICKS = 80;

    private Region01SalvageEventRuntime() {}

    public static boolean triggerIfDue(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        ExpeditionRun run = ExpeditionGameplayService.activeFor(player, world).orElse(null);
        if (run == null || run.status() != ExpeditionRun.Status.DEPLOYED) return false;
        int recovered = run.recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0);
        if (!Region01SalvageEventRules.isBlackoutDue(recovered)) return false;

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, BLACKOUT_TICKS, 0));
        level.sendParticles(
            ParticleTypes.SCULK_SOUL,
            player.getX(), player.getY(0.6D), player.getZ(),
            18, 0.8D, 0.5D, 0.8D, 0.025D
        );
        level.playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            SoundEvents.SCULK_SHRIEKER_SHRIEK,
            SoundSource.PLAYERS,
            0.55F,
            0.8F
        );
        return true;
    }
}

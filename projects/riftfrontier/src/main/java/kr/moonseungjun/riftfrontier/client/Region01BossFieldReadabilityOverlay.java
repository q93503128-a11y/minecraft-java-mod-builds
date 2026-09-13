package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.client.render.Region01BossClientRenderRuntime;
import kr.moonseungjun.riftfrontier.combat.Region01BossFieldImpactProfile;
import kr.moonseungjun.riftfrontier.combat.Region01BossFieldReadabilityProjection;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.entity.Region01BossEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Development readability overlay for the Region 01 boss field harness.
 *
 * <p>This is deliberately not final VFX art. It projects the exact provisional hit geometry already owned by
 * {@link Region01BossFieldImpactProfile} into lightweight vanilla particles so human field play can compare what the
 * server says is hittable with what the player can actually read on screen. It does not change damage, timing,
 * target admission, attack selection or presentation semantics.</p>
 *
 * <p>The overlay automatically retires once a reviewed production boss render binding is published. That keeps this
 * diagnostic layer from becoming accidental final presentation when the real material/animation/VFX stack lands.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = Riftfrontier.MOD_ID)
public final class Region01BossFieldReadabilityOverlay {
    private static final double SEARCH_RADIUS = 96.0D;
    private static final long SAMPLE_INTERVAL_TICKS = 2L;

    private Region01BossFieldReadabilityOverlay() {}

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        if (Region01BossClientRenderRuntime.current().isPresent()) return;
        if (minecraft.level.getGameTime() % SAMPLE_INTERVAL_TICKS != 0L) return;

        var search = minecraft.player.getBoundingBox().inflate(SEARCH_RADIUS);
        for (Region01BossEntity boss : minecraft.level.getEntitiesOfClass(
            Region01BossEntity.class,
            search,
            Region01BossEntity::isAlive
        )) {
            BossPresentationClientState.current(boss.getId(), boss.getUUID()).ifPresent(state -> {
                if (!state.active()) return;
                Region01BossFieldImpactProfile.Profile profile;
                try {
                    profile = Region01BossFieldImpactProfile.find(ContentId.parse(state.patternId())).orElse(null);
                } catch (IllegalArgumentException malformed) {
                    return;
                }
                if (profile == null) return;

                Vec3 look = boss.getLookAngle();
                double horizontal = Math.hypot(look.x, look.z);
                double forwardX = horizontal < 1.0E-6D ? 0.0D : look.x / horizontal;
                double forwardZ = horizontal < 1.0E-6D ? 1.0D : look.z / horizontal;
                ParticleOptions particle = particleForPhase(state.attackPhase());
                double y = boss.getY() + 0.12D;

                for (Region01BossFieldReadabilityProjection.LocalSample sample
                    : Region01BossFieldReadabilityProjection.samples(profile)) {
                    double x = boss.getX() + sample.forward() * forwardX - sample.lateral() * forwardZ;
                    double z = boss.getZ() + sample.forward() * forwardZ + sample.lateral() * forwardX;
                    minecraft.level.addParticle(particle, x, y, z, 0.0D, 0.012D, 0.0D);
                }
            });
        }
    }

    private static ParticleOptions particleForPhase(String phase) {
        return switch (phase) {
            case "TELEGRAPH" -> ParticleTypes.CLOUD;
            case "ACTIVE" -> ParticleTypes.CRIT;
            case "RECOVERY" -> ParticleTypes.SMOKE;
            default -> ParticleTypes.CLOUD;
        };
    }
}
